#!/usr/bin/env python3
"""Convert the UI art used by the app from assets/ into WebP drawables.

Each entry maps an asset to a drawable name and the longest side it is scaled down to.
Transparent margins are trimmed so artwork fills the bounds it is drawn into.

Usage: python3 tools/build_ui_assets.py  (requires pillow)
"""

from pathlib import Path

from PIL import Image, ImageChops, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "assets"
RES_DIR = ROOT / "app/src/main/res/drawable-nodpi"

VISIBLE_ALPHA = 16
WEBP_QUALITY = 86

UI_ASSETS = {
    "backgrounds/bg_aether.png": ("bg_aether", 1672),
    "ui/icons/home.png": ("nav_home", 128),
    "ui/icons/elements_leaf.png": ("nav_elements", 128),
    "ui/icons/recipes_book.png": ("nav_recipes", 128),
    "ui/icons/achievements_trophy.png": ("nav_achievements", 128),
    "ui/icons/settings_gear.png": ("nav_settings", 128),
    "ui/icons/audio.png": ("ic_audio", 96),
    "ui/icons/haptics.png": ("ic_haptics", 96),
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
}

PROGRESS_BAR = "ui/buttons/progress_bar.png"
# The bar art is drawn half full; its empty right end becomes both ends of the track.
PROGRESS_TRACK_CAP = 60
PROGRESS_TRACK_MIDDLE_COLUMN = 230
# Bounds of the fill in the trimmed art, matched by PROGRESS_* insets in AlchemyControls.kt.
PROGRESS_FILL_BOX = (6, 7, 155, 45)
PROGRESS_FILL_RADIUS = 11


def trimmed(source: Path) -> Image.Image:
    image = Image.open(source).convert("RGBA")
    visible = image.getchannel("A").point(lambda value: 255 if value > VISIBLE_ALPHA else 0)
    return image.crop(visible.getbbox())


def convert(source: Path, name: str, max_side: int) -> None:
    image = trimmed(source)
    scale = min(1.0, max_side / max(image.size))
    if scale < 1.0:
        image = image.resize((round(image.width * scale), round(image.height * scale)), Image.LANCZOS)
    image.save(RES_DIR / f"{name}.webp", "WEBP", quality=WEBP_QUALITY, method=6)


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


def main() -> None:
    RES_DIR.mkdir(parents=True, exist_ok=True)
    for source, (name, max_side) in UI_ASSETS.items():
        convert(ASSETS / source, name, max_side)
    convert_progress_bar()


if __name__ == "__main__":
    main()
