"""Generate TTS audio for the Nex encounter pack (rule 11: offline pack creation).

Usage: python generate_audio.py
Reads callout text from a fixed map, writes .ogg files into ../nex.pack/audio/.
Requires: edge-tts (pip), ffmpeg on PATH.
"""
import asyncio
import subprocess
import sys
from pathlib import Path

import edge_tts

VOICE = "en-US-GuyNeural"
PACK_AUDIO_DIR = Path(__file__).parent / "nex.pack" / "audio"

# calloutId -> spoken text (short, urgent phrasing; may differ slightly from overlay text)
CALLOUTS = {
    "smoke_start": "Smoke phase. Melee distance. Flick protect melee and magic.",
    "choke": "Choke. Spread out.",
    "smoke_dash": "Smoke dash! Move off the path!",
    "shadow_start": "Shadow phase! Pray missiles now!",
    "shadow_smash": "Shadow smash! Move!",
    "darkness": "Darkness! Run away from Nex!",
    "blood_start": "Blood phase! Pray magic, spread out.",
    "siphon": "Siphon! Stop attacking! Kill reavers.",
    "sacrifice": "Sacrifice marked! Run seven tiles!",
    "ice_start": "Ice phase! Pray magic to avoid freeze.",
    "containment": "Containment! Move out!",
    "ice_prison": "Ice prison! Break him out fast!",
    "zaros_start": "Zaros phase! Pray magic.",
    "wrath": "Wrath! Run away from the body!",
}


async def tts_to_mp3(text: str, mp3_path: Path) -> None:
    communicate = edge_tts.Communicate(text, VOICE, rate="+15%")
    await communicate.save(str(mp3_path))


def mp3_to_ogg(mp3_path: Path, ogg_path: Path) -> None:
    subprocess.run(
        [
            "ffmpeg", "-y", "-loglevel", "error",
            "-i", str(mp3_path),
            "-acodec", "libvorbis", "-ar", "44100", "-ac", "1", "-qscale:a", "4",
            str(ogg_path),
        ],
        check=True,
    )


async def main() -> int:
    PACK_AUDIO_DIR.mkdir(parents=True, exist_ok=True)
    tmp_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("_tmp_tts")
    tmp_dir.mkdir(exist_ok=True)

    failures = []
    for callout_id, text in CALLOUTS.items():
        mp3 = tmp_dir / f"{callout_id}.mp3"
        ogg = PACK_AUDIO_DIR / f"{callout_id}.ogg"
        try:
            print(f"[tts] {callout_id}: {text!r}")
            await tts_to_mp3(text, mp3)
            mp3_to_ogg(mp3, ogg)
            size = ogg.stat().st_size
            print(f"       -> {ogg.name} ({size} bytes)")
        except Exception as exc:  # noqa: BLE001 - report all and continue
            failures.append((callout_id, str(exc)))
            print(f"       FAILED: {exc}", file=sys.stderr)

    if failures:
        print(f"\n{len(failures)} callout(s) failed:", file=sys.stderr)
        for callout_id, error in failures:
            print(f"  {callout_id}: {error}", file=sys.stderr)
        return 1
    print("\nAll Nex callouts generated.")
    return 0


if __name__ == "__main__":
    raise SystemExit(asyncio.run(main()))
