"""End-to-end knowledge pipeline CLI (roadmap Sprint 28).

Stages: fetch -> parse -> generate -> validate -> review -> audio -> pack.
Human review stays in the loop: without a --review decision the run stops
after validation (exit 2) with artifacts retained; rerun with
--resume <run-dir> --review approved after using the review interface.

Usage:
    python -m pipeline run --boss "Nex" --out-dir dist/nex \\
        --version 1.0.0 --review approved --review-notes "verified in-game"
    python -m pipeline run --boss "Nex" --out-dir dist/nex
    python -m pipeline run --resume dist/nex --review approved

Exit codes: 0 success, 1 stage failure, 2 awaiting human review.
"""
from __future__ import annotations

import argparse
import copy
import json
import re
import sys
import traceback
from dataclasses import dataclass, field
from pathlib import Path
from typing import Callable, Dict, List, Optional

HERE = Path(__file__).resolve().parent
if str(HERE) not in sys.path:
    sys.path.insert(0, str(HERE))

from ai_validator_agent import AiValidatorAgent, apply_review  # noqa: E402
from audio_generator import generate_audio  # noqa: E402
from changelog_generator import generate_changelog  # noqa: E402
from pack_builder import build_pack  # noqa: E402
import logic_validator  # noqa: E402
import schema_validator  # noqa: E402
import wiki_parser  # noqa: E402
from wiki_fetcher import WikiFetcher  # noqa: E402

EXIT_OK = 0
EXIT_FAILED = 1
EXIT_NEEDS_REVIEW = 2

SEMVER = re.compile(r"^\d+\.\d+\.\d+$")


@dataclass
class RunConfig:
    boss: str = ""
    pages: List[str] = field(default_factory=list)
    cache_dir: Path = Path(".wiki_cache")
    out_dir: Path = Path("dist")
    version: Optional[str] = None
    review: Optional[str] = None
    review_notes: str = ""
    allow_unreviewed: bool = False
    resume_dir: Optional[Path] = None
    previous_draft: Optional[Path] = None
    log_file: Optional[Path] = None
    force_refresh: bool = False
    synthesize: Optional[Callable] = None  # injected TTS (tests)
    audio_config: Optional[dict] = None


class PipelineLog:
    """Progress reporting to console plus an optional log file."""

    def __init__(self, log_file: Optional[Path] = None):
        self.log_file = log_file
        self.lines: List[str] = []

    def stage(self, index: int, total: int, name: str) -> None:
        self.emit(f"==> [{index}/{total}] {name}...")

    def ok(self, message: str) -> None:
        self.emit(f"    ok: {message}")

    def fail(self, message: str) -> None:
        self.emit(f"    XX FAILED: {message}")

    def emit(self, message: str) -> None:
        print(message, flush=True)
        self.lines.append(message)
        if self.log_file is not None:
            self.log_file.parent.mkdir(parents=True, exist_ok=True)
            with self.log_file.open("a", encoding="utf-8") as handle:
                handle.write(message + "\n")


def _write_json(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=1) + "\n", encoding="utf-8")


def run_pipeline(config: RunConfig) -> int:
    log = PipelineLog(config.log_file)
    out_dir = Path(config.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    if config.resume_dir is not None:
        return _run_resume(config, log)

    if not config.boss:
        log.fail("--boss is required (or use --resume)")
        return EXIT_FAILED
    if config.version is not None and not SEMVER.match(config.version):
        log.fail(f"version '{config.version}' is not semver (x.y.z)")
        return EXIT_FAILED

    pages = config.pages or [config.boss, f"{config.boss}/Strategies"]
    total_stages = 7

    # 1. fetch
    log.stage(1, total_stages, f"fetching {len(pages)} wiki page(s)")
    fetcher = WikiFetcher(config.cache_dir)
    html_by_page: Dict[str, str] = {}
    try:
        for page in pages:
            html_by_page[page] = fetcher.fetch(
                page, force_refresh=config.force_refresh)
            log.ok(f"{page} ({len(html_by_page[page])} chars)")
    except Exception as exc:
        log.fail(f"wiki fetch failed: {exc}")
        return EXIT_FAILED

    # 2. parse
    log.stage(2, total_stages, "parsing wiki content")
    try:
        main_html = html_by_page[pages[0]]
        strategies_html = html_by_page[pages[1]] if len(pages) > 1 else main_html
        boss = wiki_parser.parse_boss(
            main_html, strategies_html, source_pages=list(pages))
        if not boss.phases:
            raise ValueError("no phases extracted — wrong pages?")
        log.ok(f"{boss.name}: {len(boss.phases)} phases, "
               f"{sum(len(p.mechanics) for p in boss.phases)} mechanics")
    except Exception as exc:
        log.fail(f"parse failed: {exc}")
        return EXIT_FAILED

    # 3. generate
    log.stage(3, total_stages, "generating draft encounter JSON")
    try:
        from llm_prompter import generate_draft
        draft = generate_draft(boss)
        _write_json(out_dir / "draft.json", draft)
        log.ok("draft.json written")
    except Exception as exc:
        log.fail(f"generation failed: {exc}")
        log.emit(traceback.format_exc(limit=3))
        return EXIT_FAILED

    # 4. validate
    log.stage(4, total_stages, "validating draft")
    try:
        agent = AiValidatorAgent()
        result = agent.run(copy.deepcopy(draft))
        draft = result.fixed_draft
        _write_json(out_dir / "draft.json", draft)
        (out_dir / "validation_report.json").write_text(
            result.report.to_json() + "\n", encoding="utf-8")
        log.ok(f"report: passed={result.report.passed}, "
               f"auto-fixes={result.fixed_count}")
        for line in result.report.lines():
            log.emit(f"    {line}")
        if not result.report.passed:
            log.fail("draft has open critical issues — fix or review manually")
            return EXIT_FAILED
    except Exception as exc:
        log.fail(f"validation failed: {exc}")
        return EXIT_FAILED

    # 5. review gate
    log.stage(5, total_stages, "human review gate")
    if config.review:
        try:
            apply_review(draft, config.review, config.review_notes)
        except ValueError as exc:
            log.fail(str(exc))
            return EXIT_FAILED
        _write_json(out_dir / "draft.json", draft)
        log.ok(f"review recorded: {config.review}")
    elif config.allow_unreviewed:
        log.ok("proceeding without review (--allow-unreviewed)")
    else:
        log.emit("    !! no --review decision: draft + report retained, "
                 "open the review interface, then rerun with "
                 f"--resume {out_dir} --review approved")
        return EXIT_NEEDS_REVIEW

    # 6. audio
    log.stage(6, total_stages, "generating callout audio")
    try:
        if config.version:
            draft.setdefault("metadata", {})["version"] = config.version
            _write_json(out_dir / "draft.json", draft)
        audio_result = generate_audio(
            draft, out_dir / "audio",
            config=config.audio_config,
            synthesize=config.synthesize,
            allow_unreviewed=True)
        log.ok(f"{len(audio_result.entries)} files, "
               f"{audio_result.total_bytes()} bytes")
    except Exception as exc:
        log.fail(f"audio generation failed (draft + report retained): {exc}")
        return EXIT_FAILED

    # 7. pack + changelog
    log.stage(7, total_stages, "assembling pack")
    try:
        manifest = build_pack(out_dir / "draft.json", out_dir / "audio",
                              out_dir / "pack.zip", version=config.version)
        log.ok(f"pack.zip ({manifest['totalBytes']} bytes, "
               f"{len(manifest['files'])} files)")
        previous = None
        if config.previous_draft is not None:
            previous = json.loads(config.previous_draft.read_text(
                encoding="utf-8"))
        changelog = generate_changelog(
            draft, previous,
            version=manifest.get("version") or config.version or "0.1.0")
        (out_dir / "changelog.md").write_text(changelog, encoding="utf-8")
        log.ok("changelog.md written")
    except Exception as exc:
        log.fail(f"pack assembly failed: {exc}")
        return EXIT_FAILED

    log.emit("pipeline complete.")
    return EXIT_OK


def _run_resume(config: RunConfig, log: PipelineLog) -> int:
    resume = Path(config.resume_dir)
    draft_file = resume / "draft.json"
    if not draft_file.exists():
        log.fail(f"nothing to resume: {draft_file} missing")
        return EXIT_FAILED
    if not config.review and not config.allow_unreviewed:
        log.fail("resume needs --review <status> (or --allow-unreviewed)")
        return EXIT_FAILED

    draft = json.loads(draft_file.read_text(encoding="utf-8"))
    total_stages = 7
    log.stage(5, total_stages, "human review gate (resumed)")
    if config.review:
        try:
            apply_review(draft, config.review, config.review_notes)
        except ValueError as exc:
            log.fail(str(exc))
            return EXIT_FAILED
        _write_json(draft_file, draft)
        log.ok(f"review recorded: {config.review}")
    else:
        log.ok("proceeding without review (--allow-unreviewed)")

    continued = RunConfig(
        boss=config.boss, out_dir=resume, version=config.version,
        review=config.review, review_notes=config.review_notes,
        allow_unreviewed=True, previous_draft=config.previous_draft,
        log_file=config.log_file, synthesize=config.synthesize,
        audio_config=config.audio_config)
    # stages 6-7 with the already-reviewed draft on disk
    log.stage(6, total_stages, "generating callout audio")
    try:
        if config.version:
            draft.setdefault("metadata", {})["version"] = config.version
            _write_json(draft_file, draft)
        audio_result = generate_audio(
            draft, resume / "audio", config=continued.audio_config,
            synthesize=continued.synthesize, allow_unreviewed=True)
        log.ok(f"{len(audio_result.entries)} files")
    except Exception as exc:
        log.fail(f"audio generation failed: {exc}")
        return EXIT_FAILED

    log.stage(7, total_stages, "assembling pack")
    try:
        manifest = build_pack(draft_file, resume / "audio",
                              resume / "pack.zip", version=config.version)
        log.ok(f"pack.zip ({manifest['totalBytes']} bytes)")
        previous = None
        if config.previous_draft is not None:
            previous = json.loads(config.previous_draft.read_text(
                encoding="utf-8"))
        from changelog_generator import generate_changelog as changelog
        (resume / "changelog.md").write_text(
            changelog(draft, previous,
                      version=manifest.get("version") or "0.1.0"),
            encoding="utf-8")
        log.ok("changelog.md written")
    except Exception as exc:
        log.fail(f"pack assembly failed: {exc}")
        return EXIT_FAILED

    log.emit("pipeline complete.")
    return EXIT_OK


def build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Project Coach knowledge pipeline: wiki to pack.")
    parser.add_argument("command", choices=["run"], help="only 'run' for now")
    parser.add_argument("--boss", default="",
                        help='boss name, e.g. "Nex"')
    parser.add_argument("--pages", nargs="*", default=None,
                        help="wiki pages (default: <boss> and <boss>/Strategies)")
    parser.add_argument("--out-dir", default="dist",
                        help="run directory for artifacts")
    parser.add_argument("--cache-dir", default=".wiki_cache")
    parser.add_argument("--version", default=None, help="pack version (semver)")
    parser.add_argument("--review", default=None,
                        choices=["approved", "needs_work", "rejected"])
    parser.add_argument("--review-notes", default="")
    parser.add_argument("--allow-unreviewed", action="store_true")
    parser.add_argument("--resume", default=None,
                        help="resume a previous run directory after review")
    parser.add_argument("--previous-draft", default=None,
                        help="previous version draft for changelog diffing")
    parser.add_argument("--log-file", default=None)
    parser.add_argument("--force-refresh", action="store_true",
                        help="re-fetch wiki pages ignoring cache")
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_arg_parser().parse_args(argv)
    config = RunConfig(
        boss=args.boss,
        pages=args.pages or [],
        cache_dir=Path(args.cache_dir),
        out_dir=Path(args.out_dir),
        version=args.version,
        review=args.review,
        review_notes=args.review_notes,
        allow_unreviewed=args.allow_unreviewed,
        resume_dir=Path(args.resume) if args.resume else None,
        previous_draft=Path(args.previous_draft) if args.previous_draft else None,
        log_file=Path(args.log_file) if args.log_file else None,
        force_refresh=args.force_refresh,
    )
    return run_pipeline(config)


if __name__ == "__main__":
    raise SystemExit(main())
