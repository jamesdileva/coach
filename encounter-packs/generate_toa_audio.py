"""Generate TTS audio for the ToA encounter pack (rule 11)."""
import asyncio
import subprocess
import sys
from pathlib import Path

import edge_tts

VOICE = "en-US-GuyNeural"
PACK_AUDIO_DIR = Path(__file__).parent / "toa.pack" / "audio"

CALLOUTS = {
    "zebak_start": "Zebak! Watch for waves and jugs!",
    "zebak_special": "Special incoming! Get ready!",
    "zebak_enrage": "Zebak enraged! Attacks much faster!",
    "akkha_start": "Akkha! Match prayers to his style.",
    "akkha_shadows": "Shadows! Break a corner and lure him in!",
    "baba_start": "Ba Ba! Melee distance, dodge boulders!",
    "baba_knockback": "Knockback soon! Stand on the sides!",
    "kephri_start": "Kephri! Clear swarms and break the shield!",
    "wardens_start": "Wardens! Attack the obelisk, pick your trough!",
    "warden_enrage": "Enrage! He heals twenty percent, finish it fast!",
}


async def tts_to_mp3(text: str, mp3_path: Path) -> None:
    communicate = edge_tts.Communicate(text, VOICE, rate="+15%")
    await communicate.save(str(mp3_path))


def mp3_to_ogg(mp3_path: Path, ogg_path: Path) -> None:
    subprocess.run(
        ["ffmpeg", "-y", "-loglevel", "error", "-i", str(mp3_path),
         "-acodec", "libvorbis", "-ar", "44100", "-ac", "1", "-qscale:a", "4",
         str(ogg_path)],
        check=True,
    )


async def main() -> int:
    PACK_AUDIO_DIR.mkdir(parents=True, exist_ok=True)
    tmp_dir = Path("_tmp_tts")
    tmp_dir.mkdir(exist_ok=True)

    failures = []
    for callout_id, text in CALLOUTS.items():
        mp3 = tmp_dir / f"{callout_id}.mp3"
        ogg = PACK_AUDIO_DIR / f"{callout_id}.ogg"
        try:
            await tts_to_mp3(text, mp3)
            mp3_to_ogg(mp3, ogg)
            print(f"[tts] {callout_id} -> {ogg.name} ({ogg.stat().st_size} bytes)")
        except Exception as exc:  # noqa: BLE001
            failures.append((callout_id, str(exc)))
            print(f"FAILED {callout_id}: {exc}", file=sys.stderr)

    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(asyncio.run(main()))
