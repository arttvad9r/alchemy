#!/usr/bin/env python3
"""Build the flat portrait studio intro from its supplied storyboard (Pillow + ffmpeg).

Run: python3 tools/build_studio_intro.py
The original lettering is retained; its texture is replaced by a white silhouette.
"""

import math
import subprocess
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
WIDTH, HEIGHT = 720, 1280
FPS, SECONDS = 30, 3


def silhouette(source, box, size):
    crop = source.crop(box).convert("RGB")
    # The red channel separates the warm letters from the dark background.
    mask = crop.getchannel("R").point(lambda value: 255 if value >= 55 else 0)
    mask = mask.filter(ImageFilter.MedianFilter(3)) if size[1] > 100 else mask
    # Close tiny texture pits without filling the counters in A and R.
    mask = mask.filter(ImageFilter.MaxFilter(3)).filter(ImageFilter.MinFilter(3)) if size[1] > 100 else mask
    # Smooth the raster silhouette at four times delivery resolution, then antialias.
    large = (size[0] * 4, size[1] * 4)
    mask = mask.resize(large, Image.Resampling.LANCZOS).filter(ImageFilter.GaussianBlur(8))
    mask = mask.point(lambda value: 255 if value >= 128 else 0).resize(size, Image.Resampling.LANCZOS)
    result = Image.new("RGBA", size, "white")
    result.putalpha(mask)
    return result


def smooth(value):
    value = max(0.0, min(1.0, value))
    return value * value * (3 - 2 * value)


def main():
    source = Image.open(ROOT / "assets/branding/artt_storyboard.png")
    logo = silhouette(source, (118, 999, 396, 1084), (540, 165))
    # Use the project's existing licensed font for a clean, untextured subtitle.
    studio = Image.new("RGBA", (350, 52))
    text = ImageDraw.Draw(studio)
    font = ImageFont.truetype(str(ROOT / "app/src/main/res/font/alegreya.ttf"), 42)
    glyphs = "STUDIO"
    widths = [font.getlength(glyph) for glyph in glyphs]
    spacing = (320 - sum(widths)) / (len(glyphs) - 1)
    x = 15
    for glyph, width in zip(glyphs, widths):
        text.text((round(x), 0), glyph, font=font, fill="white")
        x += width + spacing
    # Bounds between letters in the supplied ARTT wordmark.
    boundaries = [0, 86, 158, 216, 278]
    letters = []
    for left, right in zip(boundaries, boundaries[1:]):
        left, right = round(left * 540 / 278), round(right * 540 / 278)
        letters.append((logo.crop((left, 0, right, logo.height)), left))
    raw = ROOT / "app/src/main/res/raw/artt_intro.mp4"
    poster = ROOT / "app/src/main/res/drawable-nodpi/artt_intro_poster.png"
    raw.parent.mkdir(parents=True, exist_ok=True)
    command = [
        "ffmpeg", "-v", "error", "-y", "-f", "rawvideo", "-pix_fmt", "rgb24",
        "-s", f"{WIDTH}x{HEIGHT}", "-r", str(FPS), "-i", "-", "-an",
        "-c:v", "libx264", "-preset", "medium", "-crf", "18",
        "-pix_fmt", "yuv420p", "-movflags", "+faststart", str(raw),
    ]
    encoder = subprocess.Popen(command, stdin=subprocess.PIPE)
    try:
        for frame in range(FPS * SECONDS):
            time = frame / FPS
            canvas = Image.new("RGBA", (WIDTH, HEIGHT), "black")
            for index, (letter, left) in enumerate(letters):
                elapsed = time - (0.2 + index * 0.07)
                if elapsed < 0:
                    continue
                progress = smooth(elapsed / 0.24)
                # A small damped rebound makes the flat letters feel playful.
                bounce = 0 if elapsed < 0.24 else 0.065 * math.exp(-8 * (elapsed - 0.24)) * math.sin(19 * (elapsed - 0.24))
                scale = 0.78 + 0.22 * progress + bounce
                tile = letter.resize((round(letter.width * scale), round(letter.height * scale)), Image.Resampling.LANCZOS)
                tile.putalpha(tile.getchannel("A").point(lambda value: round(value * progress)))
                x = 90 + left + (letter.width - tile.width) // 2
                y = 515 + (letter.height - tile.height) // 2 + round(34 * (1 - progress))
                canvas.alpha_composite(tile, (x, y))
            reveal = smooth((time - 0.75) / 0.25)
            subtitle = studio.copy()
            subtitle.putalpha(subtitle.getchannel("A").point(lambda value: round(value * reveal)))
            canvas.alpha_composite(subtitle, (185, 716 + round(10 * (1 - reveal))))
            if frame == 45:
                canvas.convert("RGB").save(poster, optimize=True)
            rgb = canvas.convert("RGB")
            fade = smooth((SECONDS - 1 / FPS - time) / 0.18)
            if fade < 1:
                rgb = Image.blend(Image.new("RGB", rgb.size, "black"), rgb, fade)
            encoder.stdin.write(rgb.tobytes())
    finally:
        encoder.stdin.close()
    if encoder.wait():
        raise RuntimeError("ffmpeg could not encode the studio intro")
    print(f"Built {raw.relative_to(ROOT)}: {WIDTH}x{HEIGHT}, {SECONDS}s, {FPS}fps")


if __name__ == "__main__":
    main()
