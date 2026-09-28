#!/usr/bin/env python3
"""Convert the sounds used by the app from assets/audio into Ogg Vorbis raw resources.

Effects become mono; the music keeps its stereo and loses the silence the MP3 encoder put in front,
so it loops without a gap. Sources and licenses are listed in assets/audio/README.md.

Usage: python3 tools/build_audio.py  (requires ffmpeg)
"""

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
AUDIO = ROOT / "assets/audio"
RAW_DIR = ROOT / "app/src/main/res/raw"

EFFECTS = {
    "kenney/impactSoft_medium_003.ogg": "sfx_place",
    "kenney/maximize_006.ogg": "sfx_combine",
    "kenney/jingles_STEEL02.ogg": "sfx_discover",
    "kenney/bong_001.ogg": "sfx_no_match",
    "kenney/minimize_002.ogg": "sfx_remove",
    "kenney/minimize_004.ogg": "sfx_clear",
    "kenney/click_001.ogg": "sfx_click",
    "kenney/toggle_001.ogg": "sfx_toggle_on",
    "kenney/toggle_002.ogg": "sfx_toggle_off",
    "kenney/bookFlip3.ogg": "sfx_page",
}

# The most frequent sounds get a soft onset and no bright top, so they never click.
SOFTENED = {"sfx_place", "sfx_combine"}
SOFTEN_FILTER = "afade=t=in:d=0.015,lowpass=f=3500"

MUSIC = "music/crystal_cave_song18.mp3"
# Encoder delay at the start of the MP3: where its first sample above silence is.
MUSIC_LEADING_SILENCE = 0.0145


def ffmpeg(*args: str) -> None:
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", *args], check=True)


def main() -> None:
    RAW_DIR.mkdir(parents=True, exist_ok=True)
    for source, name in EFFECTS.items():
        soften = ["-af", SOFTEN_FILTER] if name in SOFTENED else []
        ffmpeg("-i", str(AUDIO / source), "-ac", "1", *soften, "-c:a", "libvorbis", "-q:a", "4", str(RAW_DIR / f"{name}.ogg"))
    ffmpeg(
        "-ss", str(MUSIC_LEADING_SILENCE), "-i", str(AUDIO / MUSIC),
        "-c:a", "libvorbis", "-q:a", "3", str(RAW_DIR / "music_crystal_cave.ogg"),
    )


if __name__ == "__main__":
    main()
