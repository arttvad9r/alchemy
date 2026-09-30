#!/usr/bin/env python3
"""Render the game's name for the bottom of the launch splash, as on the home screen: the display face in a gold
gradient with a soft dark shadow. One image per language; the platform shows it in a 200x80dp slot.

Usage: python3 tools/build_splash_branding.py  (requires pillow, only for development)
"""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
FONT = RES / "font/underdog.ttf"

# The 200x80dp slot at xxhdpi.
WIDTH, HEIGHT = 600, 240
NAMES = {"drawable-xxhdpi": "Alchemy", "drawable-ru-xxhdpi": "Алхимия"}

# The title gradient of the home screen, top to bottom.
TOP, MIDDLE, BOTTOM = (0xFF, 0xEF, 0xB8), (0xF3, 0xC3, 0x5B), (0xD0, 0x8A, 0x26)
SHADOW = (0x1A, 0x0E, 0x3D)


def gradient(height: int) -> Image.Image:
    column = Image.new("RGB", (1, height))
    for y in range(height):
        t = y / max(1, height - 1)
        a, b, u = (TOP, MIDDLE, t * 2) if t < 0.5 else (MIDDLE, BOTTOM, t * 2 - 1)
        column.putpixel((0, y), tuple(round(a[i] + (b[i] - a[i]) * u) for i in range(3)))
    return column.resize((WIDTH, height))


def render(name: str) -> Image.Image:
    size = 150
    while True:
        font = ImageFont.truetype(str(FONT), size)
        left, top, right, bottom = font.getbbox(name)
        if right - left <= WIDTH * 0.92 and bottom - top <= HEIGHT * 0.7:
            break
        size -= 2
    x = (WIDTH - (right - left)) / 2 - left
    y = (HEIGHT - (bottom - top)) / 2 - top
    mask = Image.new("L", (WIDTH, HEIGHT))
    ImageDraw.Draw(mask).text((x, y), name, font=font, fill=255)
    shadow = Image.new("RGBA", (WIDTH, HEIGHT), SHADOW + (0,))
    shadow.putalpha(mask.transform(mask.size, Image.AFFINE, (1, 0, 0, 0, 1, -8)).filter(ImageFilter.GaussianBlur(10)).point(lambda a: a * 0.9))
    text = gradient(HEIGHT).convert("RGBA")
    text.putalpha(mask)
    return Image.alpha_composite(shadow, text)


def main() -> None:
    for folder, name in NAMES.items():
        out = RES / folder
        out.mkdir(parents=True, exist_ok=True)
        render(name).save(out / "splash_branding.webp", "WEBP", quality=92, method=6)


if __name__ == "__main__":
    main()
