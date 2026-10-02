#!/usr/bin/env python3
"""Convert the UI art used by the app from assets/ into WebP drawables.

Each entry maps an asset to a drawable name and the longest side it is scaled down to.
Transparent margins are trimmed so artwork fills the bounds it is drawn into.

Usage: python3 tools/build_ui_assets.py  (requires pillow)
"""

import colorsys
import math
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageEnhance, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "assets"
RES_DIR = ROOT / "app/src/main/res/drawable-nodpi"

VISIBLE_ALPHA = 16
WEBP_QUALITY = 86

UI_ASSETS = {
    "backgrounds/bg_aether.png": ("bg_aether", 1672),
    "backgrounds/bg_ember.png": ("bg_ember", 1672),
    "backgrounds/bg_verdant.png": ("bg_verdant", 1672),
    "ui/icons/home.png": ("nav_home", 128),
    "ui/icons/elements_leaf.png": ("nav_elements", 128),
    "ui/icons/recipes_book.png": ("nav_recipes", 128),
    "ui/icons/achievements_trophy.png": ("nav_achievements", 128),
    "ui/icons/settings_gear.png": ("nav_settings", 128),
    "ui/icons/audio.png": ("ic_audio", 96),
    "ui/icons/haptics.png": ("ic_haptics", 96),
    "ui/icons/music.png": ("ic_music", 96),
    "ui/icons/help.png": ("ic_help", 96),
    "ui/panels_states/frame_base_silver.png": ("frame_base", 256),
    "ui/panels_states/frame_common_blue.png": ("frame_common", 256),
    "ui/panels_states/frame_rare_green.png": ("frame_rare", 256),
    "ui/panels_states/frame_epic_purple.png": ("frame_epic", 256),
    "ui/panels_states/frame_legendary_gold.png": ("frame_legendary", 256),
    "combine_scene/magic_circle_glyph.png": ("scene_magic_circle", 512),
    "effects/combine_flash.png": ("fx_combine_flash", 512),
    "effects/success_burst.png": ("fx_success_burst", 512),
    "effects/sparkles_gold.png": ("fx_sparkles_gold", 512),
    "ui/buttons/button_small_blue.png": ("btn_blue", 512),
    "ui/buttons/button_small_gold.png": ("btn_gold", 512),
    "ui/buttons/button_danger_red.png": ("btn_red", 512),
    "ui/buttons/button_secondary_dark.png": ("btn_dark", 512),
    "ui/buttons/toggle_on.png": ("toggle_on", 512),
    "ui/buttons/toggle_off.png": ("toggle_off", 512),
    "ui/buttons/tab_active.png": ("tab_active", 512),
    "ui/buttons/tab_inactive.png": ("tab_inactive", 512),
    "ui/panels_states/dialog_panel_gold.png": ("dialog_gold", 512),
    "ui/panels_states/dialog_panel_blue.png": ("dialog_blue", 512),
    "ui/panels_states/achievement_wreath_gold.png": ("achievement_wreath", 256),
    "ui/panels_states/locked_chain_panel.png": ("achievement_locked", 256),
    "ui/panels_states/banner_wide.png": ("banner_wide", 512),
    "effects/sparkles_blue.png": ("fx_sparkles_blue", 512),
    "effects/smoke_puff.png": ("fx_smoke_puff", 512),
    "ui/buttons/search_field.png": ("field_search", 512),
    "ui/panels_states/card_base.png": ("card_base", 512),
    "ui/panels_states/selected_ring_blue.png": ("fx_selected_ring", 256),
    "effects/energy_ring.png": ("fx_energy_ring", 512),
    "effects/shockwave_ring.png": ("fx_shockwave_ring", 512),
    "effects/sparkles_purple.png": ("fx_sparkles_purple", 512),
    "effects/glow_purple_orb.png": ("fx_glow_purple_orb", 512),
    "effects/glow_gold_orb.png": ("fx_glow_gold_orb", 512),
    "effects/stars_cluster.png": ("fx_stars_cluster", 512),
    "ui/buttons/input_field.png": ("field_row", 512),
    "ui/icons/plus.png": ("ic_plus", 96),
    "ui/icons/forward.png": ("ic_forward", 96),
    "ui/buttons/dropdown_field.png": ("field_dropdown", 512),
    "ui/icons/recent_clock.png": ("ic_recent", 96),
    "ui/icons/hint_bulb.png": ("ic_hint", 96),
    "ui/icons/back.png": ("ic_back", 96),
    "ui/icons/check.png": ("ic_check", 96),
    "ui/buttons/radio_on.png": ("radio_on", 96),
    "ui/buttons/radio_off.png": ("radio_off", 96),
    "ui/icons/info.png": ("ic_info", 96),
    "ui/buttons/pill_badge_green.png": ("pill_badge", 256),
    "ui/buttons/tooltip_bubble.png": ("tooltip_bubble", 512),
    "ui/panels_states/discovery_burst_gold.png": ("fx_discovery_burst_gold", 256),
}

CLOSE_ICON = "ui/icons/close.png"
# The close icon is drawn orange-red; the dialogs want the blue of the button art.
CLOSE_HUE = 0.6
# Strokes lighter than this (the cross itself) lose most of their colour and read as white.
CLOSE_WHITE_LIGHTNESS = 0.6
CLOSE_WHITE_SATURATION = 0.35

CARD = "ui/panels_states/card_base.png"
# The card's silver trim, recoloured per rarity: (drawable, hue, saturation).
CARD_RARITIES = (
    ("card_common", 0.61, 0.75),
    ("card_rare", 0.37, 0.6),
    ("card_epic", 0.75, 0.75),
    ("card_legendary", 0.115, 0.75),
)
# The trim is light and nearly grey; the card's blue face is saturated and stays as it is.
CARD_TRIM_MAX_SATURATION = 0.35
CARD_TRIM_MIN_LIGHTNESS = 0.35
CARD_TRIM_LIGHTNESS = 0.9

PROGRESS_BAR = "ui/buttons/progress_bar.png"
# The bar art is drawn half full; its empty right end becomes both ends of the track.
PROGRESS_TRACK_CAP = 60
PROGRESS_TRACK_MIDDLE_COLUMN = 230
# Bounds of the fill in the trimmed art, matched by PROGRESS_* insets in AlchemyControls.kt.
PROGRESS_FILL_BOX = (6, 7, 155, 45)
PROGRESS_FILL_RADIUS = 11

SLIDER = "ui/buttons/slider.png"
SLIDER_KNOB_CENTER = (173, 52)
SLIDER_KNOB_RADIUS = 42
SLIDER_KNOB_FEATHER = 6
SLIDER_KNOB_SIZE = 96

# Blue art that each theme recolours: written next to the original as <name>_<theme>.webp.
# Only the saturated blues move; gold trim, silver and the green/red accents keep their colour.
# The rarity cards are recoloured in convert_card_rarities, from the themed card_base.
THEMED_ART = (
    "btn_blue", "dialog_blue", "tab_active", "tab_inactive", "toggle_on", "field_search", "field_dropdown",
    "field_row", "progress_track", "progress_fill", "slider_knob", "banner_wide", "scene_magic_circle",
    "radio_on", "card_base", "ic_plus", "ic_forward", "ic_close",
)
# Theme -> (OKLCH hue every blue becomes, chroma factor, lightness factor). The shift keeps perceived
# lightness, so the new hue is no brighter than the blue it replaces; one hue keeps toggles and panels alike.
THEME_TINTS = {"ember": (48, 0.75, 1.1), "verdant": (180, 0.7, 1.0)}
BLUE_HUE_RANGE = (190, 262)
BLUE_MIN_SATURATION = 0.35
# Dark navy fills turn muddy in a warm or green hue, so the darkest pixels keep only this share of
# their chroma and read as tinted charcoal; full chroma returns between these OKLCH lightnesses.
DARK_CHROMA = 0.4
DARK_CHROMA_RAMP = (0.25, 0.55)

# Backgrounds graded towards the dusk of bg_aether: drawable -> (colour factor, brightness factor).
BACKGROUND_GRADES = {"bg_ember": (0.75, 0.85), "bg_verdant": (0.7, 0.6)}


def srgb_to_linear(c: float) -> float:
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def linear_to_srgb(c: float) -> float:
    c = min(1.0, max(0.0, c))
    return 12.92 * c if c <= 0.0031308 else 1.055 * c ** (1 / 2.4) - 0.055


def to_oklch(red: float, green: float, blue: float) -> tuple[float, float, float]:
    """sRGB in 0..1 to OKLCH (https://bottosson.github.io/posts/oklab/)."""
    r, g, b = srgb_to_linear(red), srgb_to_linear(green), srgb_to_linear(blue)
    l = (0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b) ** (1 / 3)
    m = (0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b) ** (1 / 3)
    s = (0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b) ** (1 / 3)
    lightness = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
    a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
    b_ = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
    return lightness, math.hypot(a, b_), math.degrees(math.atan2(b_, a)) % 360


def from_oklch(lightness: float, chroma: float, hue: float) -> tuple[float, float, float]:
    a, b = chroma * math.cos(math.radians(hue)), chroma * math.sin(math.radians(hue))
    l = (lightness + 0.3963377774 * a + 0.2158037573 * b) ** 3
    m = (lightness - 0.1055613458 * a - 0.0638541728 * b) ** 3
    s = (lightness - 0.0894841775 * a - 1.2914855480 * b) ** 3
    return (
        linear_to_srgb(4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s),
        linear_to_srgb(-1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s),
        linear_to_srgb(-0.0041960771 * l - 0.7034186147 * m + 1.7076127572 * s),
    )


def tinted(source: Image.Image, theme: str) -> Image.Image:
    """The image with its saturated blues moved to the theme's hue."""
    target, chroma_factor, lightness_factor = THEME_TINTS[theme]
    low, high = DARK_CHROMA_RAMP
    image = source.copy()
    pixels = image.load()
    for y in range(image.height):
        for x in range(image.width):
            red, green, blue, alpha = pixels[x, y]
            if alpha == 0:
                continue
            hue, _, saturation = colorsys.rgb_to_hls(red / 255, green / 255, blue / 255)
            if BLUE_HUE_RANGE[0] < hue * 360 < BLUE_HUE_RANGE[1] and saturation > BLUE_MIN_SATURATION:
                lightness, chroma, _ = to_oklch(red / 255, green / 255, blue / 255)
                share = DARK_CHROMA + (1 - DARK_CHROMA) * min(1.0, max(0.0, (lightness - low) / (high - low)))
                rgb = from_oklch(lightness * lightness_factor, chroma * chroma_factor * share, target)
                pixels[x, y] = (*(round(c * 255) for c in rgb), alpha)
    return image


def convert_themed_art() -> None:
    """Hue-shift the blue pixels of THEMED_ART into each theme's colour."""
    for name in THEMED_ART:
        source = Image.open(RES_DIR / f"{name}.webp").convert("RGBA")
        for theme in THEME_TINTS:
            tinted(source, theme).save(RES_DIR / f"{name}_{theme}.webp", "WEBP", quality=WEBP_QUALITY, method=6)


def trimmed(source: Path) -> Image.Image:
    image = Image.open(source).convert("RGBA")
    visible = image.getchannel("A").point(lambda value: 255 if value > VISIBLE_ALPHA else 0)
    return image.crop(visible.getbbox())


def convert(source: Path, name: str, max_side: int) -> None:
    image = trimmed(source)
    scale = min(1.0, max_side / max(image.size))
    if scale < 1.0:
        image = image.resize((round(image.width * scale), round(image.height * scale)), Image.LANCZOS)
    if name in BACKGROUND_GRADES:
        colour, brightness = BACKGROUND_GRADES[name]
        image = ImageEnhance.Brightness(ImageEnhance.Color(image).enhance(colour)).enhance(brightness)
    image.save(RES_DIR / f"{name}.webp", "WEBP", quality=WEBP_QUALITY, method=6)


def convert_close_icon() -> None:
    """Recolour the close icon to the blue buttons: one blue hue throughout, the cross nearly white."""
    image = trimmed(ASSETS / CLOSE_ICON)
    pixels = image.load()
    for y in range(image.height):
        for x in range(image.width):
            red, green, blue, alpha = pixels[x, y]
            if alpha == 0:
                continue
            _, lightness, saturation = colorsys.rgb_to_hls(red / 255, green / 255, blue / 255)
            if lightness > CLOSE_WHITE_LIGHTNESS:
                saturation *= CLOSE_WHITE_SATURATION
            r, g, b = colorsys.hls_to_rgb(CLOSE_HUE, lightness, saturation)
            pixels[x, y] = (round(r * 255), round(g * 255), round(b * 255), alpha)
    image.thumbnail((96, 96), Image.LANCZOS)
    image.save(RES_DIR / "ic_close.webp", "WEBP", quality=WEBP_QUALITY, method=6)


def convert_card_rarities() -> None:
    """One copy of the element card per rarity and theme, with only its silver trim recoloured."""
    source = trimmed(ASSETS / CARD)
    source.thumbnail((512, 512), Image.LANCZOS)
    faces = {"": source} | {f"_{theme}": tinted(source, theme) for theme in THEME_TINTS}
    for suffix, face in faces.items():
        for name, hue, saturation in CARD_RARITIES:
            image = face.copy()
            pixels = image.load()
            for y in range(image.height):
                for x in range(image.width):
                    red, green, blue, alpha = pixels[x, y]
                    if alpha == 0:
                        continue
                    _, lightness, trim_saturation = colorsys.rgb_to_hls(red / 255, green / 255, blue / 255)
                    if trim_saturation < CARD_TRIM_MAX_SATURATION and lightness > CARD_TRIM_MIN_LIGHTNESS:
                        r, g, b = colorsys.hls_to_rgb(hue, lightness * CARD_TRIM_LIGHTNESS, saturation)
                        pixels[x, y] = (round(r * 255), round(g * 255), round(b * 255), alpha)
            image.save(RES_DIR / f"{name}{suffix}.webp", "WEBP", quality=WEBP_QUALITY, method=6)


def convert_progress_bar() -> None:
    """Split the half-filled bar art into an empty track and a pill-shaped fill."""
    bar = trimmed(ASSETS / PROGRESS_BAR)
    width, height = bar.size
    cap = bar.crop((width - PROGRESS_TRACK_CAP, 0, width, height))
    middle = bar.crop((PROGRESS_TRACK_MIDDLE_COLUMN, 0, PROGRESS_TRACK_MIDDLE_COLUMN + 1, height))
    track = Image.new("RGBA", bar.size)
    track.paste(cap.transpose(Image.FLIP_LEFT_RIGHT), (0, 0))
    track.paste(middle.resize((width - 2 * PROGRESS_TRACK_CAP, height)), (PROGRESS_TRACK_CAP, 0))
    track.paste(cap, (width - PROGRESS_TRACK_CAP, 0))
    track.save(RES_DIR / "progress_track.webp", "WEBP", quality=WEBP_QUALITY, method=6)

    fill = bar.crop(PROGRESS_FILL_BOX)
    mask = Image.new("L", fill.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, fill.width - 1, fill.height - 1), radius=PROGRESS_FILL_RADIUS, fill=255)
    fill.putalpha(ImageChops.multiply(fill.getchannel("A"), mask))
    fill.save(RES_DIR / "progress_fill.webp", "WEBP", quality=WEBP_QUALITY, method=6)


def convert_slider_knob() -> None:
    """Cut the glowing knob out of the composite slider art with a softly feathered round edge."""
    source = Image.open(ASSETS / SLIDER).convert("RGBA")
    cx, cy = SLIDER_KNOB_CENTER
    box = (cx - SLIDER_KNOB_RADIUS, cy - SLIDER_KNOB_RADIUS, cx + SLIDER_KNOB_RADIUS, cy + SLIDER_KNOB_RADIUS)
    knob = source.crop(box)
    mask = Image.new("L", knob.size, 0)
    inset = SLIDER_KNOB_FEATHER
    ImageDraw.Draw(mask).ellipse((inset, inset, knob.width - inset, knob.height - inset), fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(SLIDER_KNOB_FEATHER / 2))
    knob.putalpha(ImageChops.multiply(knob.getchannel("A"), mask))
    knob = knob.resize((SLIDER_KNOB_SIZE, SLIDER_KNOB_SIZE), Image.LANCZOS)
    knob.save(RES_DIR / "slider_knob.webp", "WEBP", quality=WEBP_QUALITY, method=6)


def main() -> None:
    RES_DIR.mkdir(parents=True, exist_ok=True)
    for source, (name, max_side) in UI_ASSETS.items():
        convert(ASSETS / source, name, max_side)
    convert_progress_bar()
    convert_slider_knob()
    convert_close_icon()
    convert_card_rarities()
    convert_themed_art()


if __name__ == "__main__":
    main()
