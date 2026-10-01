#!/usr/bin/env python3
"""Put the sounds used by the app from assets/audio into app/src/main/res/raw.

The Stable Audio files are already cut, looped and encoded as picked, and the procedural ones come
out of tools/build_procedural_audio.py ready to use, so both are copied as is;
the Kenney effect becomes mono Ogg Vorbis with a soft onset. Sources and licenses are listed in
assets/audio/README.md.

Usage: python3 tools/build_audio.py  (requires ffmpeg)
"""

import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
AUDIO = ROOT / "assets/audio"
RAW_DIR = ROOT / "app/src/main/res/raw"

COPIED = {
    "stable-audio/place.ogg": "sfx_place",
    "stable-audio/discover.ogg": "sfx_discover",
    "stable-audio/no_match.ogg": "sfx_no_match",
    "stable-audio/remove.ogg": "sfx_remove",
    "stable-audio/click.ogg": "sfx_click",
    "stable-audio/page.ogg": "sfx_page",
    "stable-audio/music.ogg": "music_background",
    "procedural/pickup.ogg": "sfx_pickup",
    "procedural/whoosh.ogg": "sfx_whoosh",
    "procedural/hint.ogg": "sfx_hint",
    "procedural/achievement.ogg": "sfx_achievement",
    "procedural/discover_grand.ogg": "sfx_discover_grand",
}

# Heard on every repeat combination, so it gets a soft onset and no bright top and never clicks.
SOFTENED = {"kenney/maximize_006.ogg": "sfx_combine"}
SOFTEN_FILTER = "afade=t=in:d=0.015,lowpass=f=3500"


def main() -> None:
    RAW_DIR.mkdir(parents=True, exist_ok=True)
    for source, name in COPIED.items():
        shutil.copyfile(AUDIO / source, RAW_DIR / f"{name}.ogg")
    for source, name in SOFTENED.items():
        subprocess.run(
            [
                "ffmpeg", "-loglevel", "error", "-y", "-i", str(AUDIO / source),
                "-ac", "1", "-af", SOFTEN_FILTER, "-c:a", "libvorbis", "-q:a", "4", str(RAW_DIR / f"{name}.ogg"),
            ],
            check=True,
        )


if __name__ == "__main__":
    main()
