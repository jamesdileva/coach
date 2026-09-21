"""Assemble validated drafts + generated audio into distributable packs.

Pipeline stage: approved draft JSON + audio/ directory -> pack .zip +
manifest with SHA-256 checksums.

Layout inside the zip (exactly what EncounterLoader expects):
    encounter.json
    audio/<callout>.ogg
    manifest.json          # files + sha256 + sizes + pack metadata
"""
from __future__ import annotations

import hashlib
import json
import sys
import zipfile
from pathlib import Path
from typing import Dict, List, Optional

HERE = Path(__file__).resolve().parent
SRC = HERE.parent
if str(SRC) not in sys.path:
    sys.path.insert(0, str(SRC))

import logic_validator  # noqa: E402
import schema_validator  # noqa: E402


class PackBuildError(RuntimeError):
    """Raised when a pack cannot be assembled (never ships half a pack)."""


def collect_referenced_audio(draft: dict) -> Dict[str, str]:
    """audioFile -> calloutId for every callout in the draft."""
    referenced: Dict[str, str] = {}

    def walk_mechanics(mechanics):
        for mechanic in mechanics or []:
            for callout in mechanic.get("callouts") or []:
                audio = callout.get("audioFile")
                if audio and audio not in referenced:
                    referenced[audio] = callout.get("calloutId", "?")

    for boss in draft.get("bosses") or []:
        walk_mechanics(boss.get("mechanics"))
        for phase in boss.get("phases") or []:
            walk_mechanics(phase.get("mechanics"))
    return referenced


def sha256_of(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(65536), b""):
            digest.update(chunk)
    return digest.hexdigest()


def build_pack(draft_path: str | Path,
               audio_dir: str | Path,
               output_zip: str | Path,
               version: Optional[str] = None) -> Dict:
    """Validate, verify audio, assemble. Returns the manifest dict."""
    draft_file = Path(draft_path)
    audio_path = Path(audio_dir)
    out_zip = Path(output_zip)

    draft = json.loads(draft_file.read_text(encoding="utf-8"))

    if version:
        draft.setdefault("metadata", {})["version"] = version

    schema_report = schema_validator.validate(draft)
    if not schema_report.passed:
        raise PackBuildError(
            "schema validation failed:\n" + "\n".join(schema_report.lines()))

    referenced = collect_referenced_audio(draft)
    available = {p.name for p in audio_path.glob("*.ogg")} | \
                {p.name for p in audio_path.glob("*.wav")}
    missing = sorted(set(referenced) - available)
    if missing:
        raise PackBuildError(
            "audio files referenced but missing from "
            f"{audio_path}: {', '.join(missing)}")

    logic_report = logic_validator.check(draft, available_audio_files=available)
    blocking = [i for i in logic_report.issues
                if i.severity == "critical" and i.status == "open"]
    if blocking:
        raise PackBuildError(
            "logic validation failed:\n" +
            "\n".join(f"! {i.code} @ {i.path}: {i.message}" for i in blocking))

    metadata = draft.get("metadata", {})
    manifest_files: List[Dict] = []
    out_zip.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(out_zip, "w", zipfile.ZIP_DEFLATED) as archive:
        encounter_json = json.dumps(draft, indent=1)
        archive.writestr("encounter.json", encounter_json)
        manifest_files.append({
            "name": "encounter.json",
            "sha256": hashlib.sha256(encounter_json.encode("utf-8")).hexdigest(),
            "bytes": len(encounter_json.encode("utf-8")),
        })
        for audio_name in sorted(referenced):
            source = audio_path / audio_name
            arcname = f"audio/{audio_name}"
            archive.write(source, arcname)
            manifest_files.append({
                "name": arcname,
                "sha256": sha256_of(source),
                "bytes": source.stat().st_size,
            })

        manifest = {
            "packId": metadata.get("packId"),
            "version": metadata.get("version"),
            "schemaVersion": draft.get("schemaVersion"),
            "reviewStatus": metadata.get("review_status"),
            "files": manifest_files,
            "totalBytes": out_zip.stat().st_size if out_zip.exists() else None,
        }
        archive.writestr("manifest.json", json.dumps(manifest, indent=1))

    manifest["totalBytes"] = out_zip.stat().st_size
    return manifest


def main(argv: list[str] | None = None) -> int:
    import argparse

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("draft", help="approved draft encounter JSON")
    parser.add_argument("audio_dir", help="directory with .ogg/.wav files")
    parser.add_argument("output_zip", help="pack .zip to write")
    parser.add_argument("--version", default=None,
                        help="override metadata.version")
    args = parser.parse_args(argv)

    try:
        manifest = build_pack(args.draft, args.audio_dir,
                              args.output_zip, args.version)
    except PackBuildError as exc:
        print(f"pack build failed: {exc}")
        return 1
    print(f"pack: {args.output_zip} "
          f"({manifest['totalBytes']} bytes, {len(manifest['files'])} files)")
    for entry in manifest["files"]:
        print(f"  {entry['name']} ({entry['bytes']} bytes)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
