#!/usr/bin/env python3
"""Convert the UI art used by the app from assets/ into WebP drawables.

Each entry maps an asset to a drawable name and the longest side it is scaled down to.
Transparent margins are trimmed so artwork fills the bounds it is drawn into.

Usage: python3 tools/build_ui_assets.py  (requires pillow)
"""

import colorsys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "assets"
RES_DIR = ROOT / "app/src/main/res/drawable-nodpi"

VISIBLE_ALPHA = 16
WEBP_QUALITY = 86

# Ordinary controls should read as surfaces, not as rewards. Their transparent bloom and brightest
# blue highlights are restrained here so selected, rare and discovery states keep the strongest light.
RESTRAINED_UI_ART = {
    "btn_blue",
    "dialog_blue",
    "tab_active",
    "tab_inactive",
    "toggle_on",
    "toggle_off",
    "field_search",
    "field_dropdown",
    "field_row",
    "frame_base",
    "frame_common",
    "banner_wide",
}

# Each tab has its own source room background; screen code only selects the matching drawable.
ROOM_BACKGROUNDS = (
    ("backgrounds/bg_home.png", "bg_home"),
    ("backgrounds/bg_elements.png", "bg_elements"),
    ("backgrounds/bg_recipes.png", "bg_recipes"),
    ("backgrounds/bg_achievements.png", "bg_achievements"),
    ("backgrounds/bg_settings.png", "bg_settings"),
)

UI_ASSETS = {
    "backgrounds/bg_home.png": ("bg_aether", 1672),
    "ui/icons/home.png": ("nav_home", 128),
    "ui/icons/elements_leaf.png": ("nav_elements", 128),
    "ui/icons/recipes_book.png": ("nav_recipes", 128),
    "ui/icons/achievements_trophy.png": ("nav_achievements", 128),
    "ui/icons/settings_gear.png": ("nav_settings", 128),
    "ui/icons/audio.png": ("ic_audio", 96),
    "ui/icons/haptics.png": ("ic_haptics", 96),
    "ui/icons/music.png": ("ic_music", 96),
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
    "ui/buttons/pill_badge_green.png": ("pill_badge", 256),
    "ui/buttons/tooltip_bubble.png": ("tooltip_bubble", 512),
    "ui/buttons/progress_track.png": ("progress_track", 512),
    "ui/buttons/progress_fill.png": ("progress_fill", 512),
    "ui/buttons/slider_knob.png": ("slider_knob", 96),
    "ui/icons/close.png": ("ic_close", 96),
    "ui/panels_states/discovery_burst_gold.png": ("fx_discovery_burst_gold", 256),
    # Dedicated achievement symbols generated in the same visual language as the rest of the redesign.
    "achievements/achievement_experiments.png": ("achievement_experiments", 128),
    "achievements/achievement_discovered_50.png": ("achievement_discovered_50", 128),
    "achievements/achievement_discovered_100.png": ("achievement_discovered_100", 128),
    "achievements/achievement_first_epic.png": ("achievement_first_epic", 128),
    "achievements/achievement_first_legendary.png": ("achievement_first_legendary", 128),
    "achievements/achievement_all_final.png": ("achievement_all_final", 128),
    "achievements/achievement_nature.png": ("achievement_nature", 128),
    "achievements/achievement_material.png": ("achievement_material", 128),
    "achievements/achievement_life.png": ("achievement_life", 128),
    "achievements/achievement_civilization.png": ("achievement_civilization", 128),
    "achievements/achievement_cosmos.png": ("achievement_cosmos", 128),
}


CARD = "ui/panels_states/card_base.png"
# The card's silver trim, recoloured per rarity: (drawable, hue, saturation).
CARD_RARITIES = (
    ("card_common", 0.61, 0.52),
    ("card_rare", 0.37, 0.6),
    ("card_epic", 0.75, 0.75),
    ("card_legendary", 0.115, 0.75),
)
# The trim is light and nearly grey; the card's blue face is saturated and stays as it is.
CARD_TRIM_MAX_SATURATION = 0.35
CARD_TRIM_MIN_LIGHTNESS = 0.35
CARD_TRIM_LIGHTNESS = 0.9


BLUE_HUE_RANGE = (190, 262)
BLUE_MIN_SATURATION = 0.1


def trimmed(source: Path) -> Image.Image:
    image = Image.open(source).convert("RGBA")
    visible = image.getchannel("A").point(lambda value: 255 if value > VISIBLE_ALPHA else 0)
    return image.crop(visible.getbbox())


def restrained_ui_art(image: Image.Image, strength: float = 1.0) -> Image.Image:
    """Reduce baked-in bloom and cyan glare while preserving the shape and opaque painted details."""
    image = image.copy()
    pixels = image.load()
    for y in range(image.height):
        for x in range(image.width):
            red, green, blue, alpha = pixels[x, y]
            if alpha == 0:
                continue

            # Bloom lives mostly in the translucent fringe around the painted control.
            if alpha < 220:
                fringe = alpha / 220
                alpha = round(alpha * (0.68 + 0.32 * fringe) ** strength)

            hue, lightness, saturation = colorsys.rgb_to_hls(red / 255, green / 255, blue / 255)
            if BLUE_HUE_RANGE[0] < hue * 360 < BLUE_HUE_RANGE[1] and saturation > BLUE_MIN_SATURATION:
                saturation *= 1 - 0.16 * strength
                bright_share = min(1.0, max(0.0, (lightness - 0.45) / 0.55))
                lightness *= 1 - 0.07 * strength * bright_share
                red_f, green_f, blue_f = colorsys.hls_to_rgb(hue, lightness, saturation)
                red, green, blue = (round(channel * 255) for channel in (red_f, green_f, blue_f))

            pixels[x, y] = (red, green, blue, alpha)
    return image


def convert(source: Path, name: str, max_side: int) -> None:
    image = trimmed(source)
    scale = min(1.0, max_side / max(image.size))
    if scale < 1.0:
        image = image.resize((round(image.width * scale), round(image.height * scale)), Image.LANCZOS)
    if name in RESTRAINED_UI_ART:
        image = restrained_ui_art(image)
    image.save(RES_DIR / f"{name}.webp", "WEBP", quality=WEBP_QUALITY, method=6)



def convert_card_rarities() -> None:
    """One copy of the element card per rarity, with only its silver trim recoloured."""
    source = trimmed(ASSETS / CARD)
    source.thumbnail((512, 512), Image.LANCZOS)
    for name, hue, saturation in CARD_RARITIES:
        image = source.copy()
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
        restrained_ui_art(image, 0.65).save(RES_DIR / f"{name}.webp", "WEBP", quality=WEBP_QUALITY, method=6)




def main() -> None:
    RES_DIR.mkdir(parents=True, exist_ok=True)
    for source, name in ROOM_BACKGROUNDS:
        convert(ASSETS / source, name, 1672)
    for source, (name, max_side) in UI_ASSETS.items():
        convert(ASSETS / source, name, max_side)
    convert_card_rarities()


if __name__ == "__main__":
    main()
