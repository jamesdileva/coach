"""Generate TTS audio for the CoX encounter pack (rule 11)."""
import asyncio
import subprocess
import sys
from pathlib import Path

import edge_tts

VOICE = "en-US-GuyNeural"
PACK_AUDIO_DIR = Path(__file__).parent / "cox.pack" / "audio"

CALLOUTS = {
    "vanguard_start": "Vanguards! Kill order melee ranged magic. Don't let them stack!",
    "tekton_start": "Tekton! Hard hitter, keep prayers flicking!",
    "vasa_start": "Vasa! Watch for boulders and heal portals!",
    "olm_start": "Great Olm! Disable both hands, watch for specials!",
    "olm_final": "Final phase! All powers live, disable hands fast!",
}


async def tts_to_mp3(text: str, mp3_path: Path) -> None:
    communicate = edge_tts.Communicate(text, VOICE, rate="+15%")
    await communicate.save(str(mp3_path))


def mp3_to_wav(mp3_path: Path, wav_path: Path) -> None:
    subprocess.run(
        ["ffmpeg", "-y", "-loglevel", "error", "-i", str(mp3_path),
         "-acodec", "pcm_s16le", "-ar", "44100", "-ac", "1",
         str(wav_path)],
        check=True,
    )


async def main() -> int:
    PACK_AUDIO_DIR.mkdir(parents=True, exist_ok=True)
    tmp_dir = Path("_tmp_tts")
    tmp_dir.mkdir(exist_ok=True)

    failures = []
    for callout_id, text in CALLOUTS.items():
        mp3 = tmp_dir / f"{callout_id}.mp3"
        wav = PACK_AUDIO_DIR / f"{callout_id}.wav"
        try:
            await tts_to_mp3(text, mp3)
            mp3_to_wav(mp3, wav)
            print(f"[tts] {callout_id} -> {wav.name} ({wav.stat().st_size} bytes)")
        except Exception as exc:  # noqa: BLE001
            failures.append((callout_id, str(exc)))
            print(f"FAILED {callout_id}: {exc}", file=sys.stderr)

    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(asyncio.run(main()))
