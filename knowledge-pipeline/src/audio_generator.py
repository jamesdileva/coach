"""TTS audio generation for approved drafts (roadmap Sprint 27).

Pipeline stage: approved encounter JSON -> per-callout .ogg files.
Engines: edge (network, default) or kokoro (offline). Output always goes
through ffmpeg to mono Vorbis .ogg, matching what encounter packs ship.

The review gate is load-bearing: drafts whose metadata.review_status is not
"approved" are refused unless the caller explicitly overrides — only approved
packs proceed to audio generation.
"""
from __future__ import annotations

import asyncio
import subprocess
import sys
import tempfile
from dataclasses import dataclass, field
from pathlib import Path
from typing import Awaitable, Callable, Dict, List, Optional

HERE = Path(__file__).resolve().parent
SRC = HERE.parent
if str(SRC) not in sys.path:
    sys.path.insert(0, str(SRC))

try:
    import yaml  # type: ignore
except ImportError:  # pragma: no cover - exercised only without pyyaml
    yaml = None

DEFAULT_CONFIG = HERE / "audio_config.yaml"
MAX_BYTES_DEFAULT = 512000

# calloutId -> (spoken text, category, audio filename)
CalloutWork = Dict[str, Dict[str, str]]


class ReviewGateError(RuntimeError):
    """Raised when a draft without approved review reaches audio generation."""


@dataclass
class AudioManifestEntry:
    callout_id: str
    text: str
    category: str
    file: str
    bytes: int


@dataclass
class AudioResult:
    entries: List[AudioManifestEntry] = field(default_factory=list)

    def by_id(self, callout_id: str) -> Optional[AudioManifestEntry]:
        return next((e for e in self.entries if e.callout_id == callout_id), None)

    def total_bytes(self) -> int:
        return sum(e.bytes for e in self.entries)


def load_audio_config(path: Optional[Path | str] = None) -> dict:
    config_path = Path(path) if path else DEFAULT_CONFIG
    if yaml is None:
        raise RuntimeError("pyyaml is required for audio config (pip install pyyaml)")
    return yaml.safe_load(config_path.read_text(encoding="utf-8"))


def check_review_gate(draft: dict) -> None:
    metadata = draft.get("metadata") or {}
    if metadata.get("review_status") != "approved":
        raise ReviewGateError(
            f"pack '{metadata.get('packId', '?')}' review_status is "
            f"'{metadata.get('review_status')}' (need 'approved'); "
            f"approve it in the review interface first")


def collect_callout_texts(draft: dict) -> CalloutWork:
    """Every callout across all bosses: id -> text/category/audio filename."""
    work: CalloutWork = {}
    for boss in draft.get("bosses") or []:
        for phase in boss.get("phases") or []:
            mechanics = list(phase.get("mechanics") or []) + \
                list((boss.get("mechanics") or []))
            for mechanic in mechanics:
                for callout in mechanic.get("callouts") or []:
                    callout_id = callout.get("calloutId")
                    if not callout_id or callout_id in work:
                        continue
                    work[callout_id] = {
                        "text": callout.get("text", ""),
                        "category": callout.get("category", "info"),
                        "file": callout.get("audioFile") or f"{callout_id}.ogg",
                    }
    return work


# ---- synthesis backends (provider-injectable for offline tests) ----

Synthesizer = Callable[[str, str, str, Path], Awaitable[None]]
# (text, voice, rate, mp3_output_path) -> None


async def edge_synthesize(text: str, voice: str, rate: str, mp3_path: Path) -> None:
    import edge_tts

    communicate = edge_tts.Communicate(text, voice, rate=rate)
    await communicate.save(str(mp3_path))


async def kokoro_synthesize(text: str, voice: str, rate: str, mp3_path: Path) -> None:
    raise RuntimeError(
        "kokoro engine requested but the kokoro package is not wired up; "
        "install it and implement kokoro_synthesize, or use engine: edge")


def mp3_to_ogg(mp3_path: Path, ogg_path: Path, config: dict) -> None:
    ogg = config.get("ogg", {})
    subprocess.run(
        ["ffmpeg", "-y", "-loglevel", "error",
         "-i", str(mp3_path),
         "-acodec", "libvorbis",
         "-ar", str(ogg.get("sample_rate", 44100)),
         "-ac", str(ogg.get("channels", 1)),
         "-qscale:a", str(ogg.get("quality", 4)),
         str(ogg_path)],
        check=True,
    )


def voice_for_category(config: dict, category: str) -> str:
    voices = config.get("voices", {})
    return voices.get(category) or voices.get("info") or "en-US-GuyNeural"


async def _synthesize_one(text: str, voice: str, rate: str,
                          synthesize: Synthesizer,
                          ogg_path: Path, config: dict) -> None:
    with tempfile.TemporaryDirectory() as tmp:
        mp3_path = Path(tmp) / "clip.mp3"
        await synthesize(text, voice, rate, mp3_path)
        mp3_to_ogg(mp3_path, ogg_path, config)


async def generate_audio_async(
        draft: dict,
        output_dir: str | Path,
        config: Optional[dict] = None,
        synthesize: Optional[Synthesizer] = None,
        allow_unreviewed: bool = False,
        skip_existing: bool = True) -> AudioResult:
    """Generate one .ogg per callout. Returns the manifest of what was written."""
    config = config or load_audio_config()
    if not allow_unreviewed and config.get("require_approved_review", True):
        check_review_gate(draft)

    work = collect_callout_texts(draft)
    if not work:
        raise ValueError("draft contains no callouts to vocalise")

    engine = (config.get("engine") or "edge").lower()
    synthesize = synthesize or (
        edge_synthesize if engine == "edge" else kokoro_synthesize)
    rate = config.get("rate", "+15%")
    max_bytes = int(config.get("ogg", {}).get("max_bytes", MAX_BYTES_DEFAULT))

    out_dir = Path(output_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    result = AudioResult()
    for callout_id, item in sorted(work.items()):
        if not item["text"].strip():
            raise ValueError(f"callout '{callout_id}' has empty text")
        ogg_path = out_dir / item["file"]
        if skip_existing and ogg_path.exists():
            size = ogg_path.stat().st_size
        else:
            voice = voice_for_category(config, item["category"])
            await _synthesize_one(item["text"], voice, rate,
                                  synthesize, ogg_path, config)
            size = ogg_path.stat().st_size
        if size > max_bytes:
            raise ValueError(
                f"{ogg_path.name} is {size} bytes (limit {max_bytes})")
        result.entries.append(AudioManifestEntry(
            callout_id=callout_id, text=item["text"],
            category=item["category"], file=item["file"], bytes=size))
    return result


def generate_audio(draft: dict, output_dir: str | Path, **kwargs) -> AudioResult:
    """Synchronous wrapper around generate_audio_async."""
    return asyncio.run(generate_audio_async(draft, output_dir, **kwargs))


def main(argv: list[str] | None = None) -> int:
    import argparse

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("draft", help="approved draft encounter JSON")
    parser.add_argument("output_dir", help="directory for .ogg files")
    parser.add_argument("--config", default=None)
    parser.add_argument("--allow-unreviewed", action="store_true")
    parser.add_argument("--regen", action="store_true",
                        help="regenerate even if .ogg files exist")
    args = parser.parse_args(argv)

    import json
    draft = json.loads(Path(args.draft).read_text(encoding="utf-8"))
    config = load_audio_config(args.config)
    result = generate_audio(
        draft, args.output_dir, config=config,
        allow_unreviewed=args.allow_unreviewed,
        skip_existing=not args.regen)
    for entry in result.entries:
        print(f"[audio] {entry.callout_id} -> {entry.file} ({entry.bytes} bytes)")
    print(f"{len(result.entries)} files, {result.total_bytes()} bytes total")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
