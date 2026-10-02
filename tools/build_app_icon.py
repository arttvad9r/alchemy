#!/usr/bin/env python3
"""Build the foreground and monochrome layers of the adaptive launcher icon from the spellbook element icon.

The layer is the 108dp adaptive canvas at xxxhdpi. Launchers show its middle 72dp through a mask
of their own shape and some zoom in further, so the book, glow included, is scaled to stay inside
the 66dp safe circle that no mask or zoom may cut.

Usage: python3 tools/build_app_icon.py  (requires pillow; run after tools/build_element_icons.py)
"""

from pathlib import Path

from PIL import Image, ImageChops, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
SOURCE = RES / "drawable-nodpi/element_spellbook.webp"

CANVAS = 432
CANVAS_DP = 108
# Launchers may zoom or reshape the icon, but never cut into this central circle.
SAFE_RADIUS_DP = 33
# Keeps the glow a little clear of the safe circle's edge.
SAFE_MARGIN_DP = 2
VISIBLE_ALPHA = 40
# The themed (monochrome) icon keeps only the book's solid body, as its soft glow would read as a blurred disc, with
# the bright moon and star of the cover cut out of it so the shape still reads as the spellbook.
SOLID_ALPHA = 200
CUT_LUMINANCE = 165
SMOOTHING = 7


def reach(image: Image.Image) -> float:
    """How far visible pixels reach from the center, as a share of half the image side."""
    alpha = image.getchannel("A")
    center_x, center_y = (image.width - 1) / 2, (image.height - 1) / 2
    farthest = max(
        ((x - center_x) ** 2 + (y - center_y) ** 2) ** 0.5
        for y in range(image.height)
        for x in range(image.width)
        if alpha.getpixel((x, y)) > VISIBLE_ALPHA
    )
    return farthest / (image.width / 2)


def main() -> None:
    book = Image.open(SOURCE).convert("RGBA")
    radius_px = (SAFE_RADIUS_DP - SAFE_MARGIN_DP) * CANVAS / CANVAS_DP
    side = round(2 * radius_px / reach(book))
    book = book.resize((side, side), Image.LANCZOS)
    layer = Image.new("RGBA", (CANVAS, CANVAS))
    layer.paste(book, ((CANVAS - side) // 2, (CANVAS - side) // 2), book)
    layer.save(RES / "drawable-nodpi/ic_launcher_foreground.webp", "WEBP", quality=90, method=6)
    # Android 13+ themed icons tint this layer by its alpha alone.
    solid = layer.getchannel("A").point(lambda alpha: 255 if alpha >= SOLID_ALPHA else 0)
    dark = layer.convert("L").point(lambda luminance: 255 if luminance < CUT_LUMINANCE else 0).filter(ImageFilter.MedianFilter(SMOOTHING))
    monochrome = Image.new("RGBA", layer.size, (255, 255, 255, 0))
    monochrome.putalpha(ImageChops.multiply(solid, dark))
    monochrome.save(RES / "drawable-nodpi/ic_launcher_monochrome.webp", "WEBP", lossless=True)


if __name__ == "__main__":
    main()
