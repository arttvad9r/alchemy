#!/usr/bin/env python3
"""Build element icon drawables from the imported art in assets/.

Sources:
- assets/elements/<id>.png for the 42 ids listed in assets/asset_manifest_catalog.csv
  (these files are named correctly);
- assets/raw_sheets/sheet_01..09.png for the remaining 138 elements. The sliced files
  from asset_manifest_remaining_elements.* are mislabeled (six cells of sheet_06 were
  skipped and the names were shuffled), so icons are re-sliced from the raw sheets.
  Sheet icons appear in row-major order and follow REMAINING_IDS below.

Output: app/src/main/res/drawable-nodpi/element_<id>.webp and the Kotlin lookup table
ElementIcons.kt. Requires numpy, scipy and pillow (dev-only, not an app dependency).

Usage: python3 tools/build_element_icons.py [--preview out.png]
"""

import argparse
import csv
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage as ndi

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "assets"
RES_DIR = ROOT / "app/src/main/res/drawable-nodpi"
KOTLIN_FILE = ROOT / "app/src/main/kotlin/com/artt/alchemy/ui/components/ElementIcons.kt"

ICON_SIZE = 256
# Share of the canvas the icon's longer side may occupy.
ICON_FILL = 0.94
WEBP_QUALITY = 88

CORE_ALPHA = 128
BODY_ALPHA = 24
MIN_CORE_PIXELS = 1500
VISIBLE_ALPHA = 16

REMAINING_IDS = """
wind clay forest moss sky sun moon salt wave algae wood field farm wheat bread cattle
milk cheese meat leather wool cloth rope paper ink book knowledge wheel cart road village settlement
city bridge boat sail ship port net fisherman feather egg nest bee honey fruit vegetable herbs
medicine poison mushroom bone skull blood corpse ghost cemetery grave wolf horse rider string bow hunter
arrow spear sword shield armor warrior campfire tent camp forge hammer plow mine gold silver gem
crown king queen castle tower temple statue garden fountain park market barrel juice wine beer tavern
bakery mill farmer baker blacksmith miner merchant scholar priest sailor captain pirate hurricane blizzard volcano desert
oasis jungle island beach coral shark mermaid dragon phoenix griffin unicorn golem wizard alchemy rune potion
elixir portal spellbook enchantment clock hourglass star observatory astronomer philosopher_stone
""".split()


def catalog_ids() -> list[str]:
    with open(ASSETS / "asset_manifest_catalog.csv", newline="") as manifest:
        return [row["name"] for row in csv.DictReader(manifest) if row["category"] == "elements"]


def slice_sheet(path: Path) -> list[Image.Image]:
    rgba = np.array(Image.open(path).convert("RGBA"))
    alpha = rgba[..., 3]
    structure = np.ones((3, 3))

    cores, count = ndi.label(alpha > CORE_ALPHA, structure=structure)
    sizes = ndi.sum(np.ones_like(alpha), cores, range(1, count + 1))
    seed_labels = [int(label) for label in np.nonzero(sizes >= MIN_CORE_PIXELS)[0] + 1]
    centers = ndi.center_of_mass(np.ones_like(alpha), cores, seed_labels)

    # Row-major order: sheets have four icons per row, so chunk by y and sort each row by x.
    by_y = sorted(zip(seed_labels, centers), key=lambda item: item[1][0])
    ordered = [item for row in range(0, len(by_y), 4) for item in sorted(by_y[row : row + 4], key=lambda item: item[1][1])]
    seeds = np.zeros_like(cores)
    for index, (label, _) in enumerate(ordered, start=1):
        seeds[cores == label] = index

    # Opaque-ish blobs with exactly one seed belong to it; others go to the nearest seed pixel.
    owner = np.zeros_like(cores)
    _, (near_y, near_x) = ndi.distance_transform_edt(seeds == 0, return_indices=True)
    nearest = seeds[near_y, near_x]
    blobs, blob_count = ndi.label(alpha > BODY_ALPHA, structure=structure)
    for blob in range(1, blob_count + 1):
        mask = blobs == blob
        inside = np.unique(seeds[mask])
        inside = inside[inside != 0]
        owner[mask] = inside[0] if len(inside) == 1 else nearest[mask]
    # Faint glow pixels follow the closest assigned pixel.
    _, (near_y, near_x) = ndi.distance_transform_edt(owner == 0, return_indices=True)
    owner = np.where(alpha > 0, owner[near_y, near_x], 0)

    icons = []
    for index in range(1, len(ordered) + 1):
        layer = rgba.copy()
        layer[..., 3] = np.where(owner == index, alpha, 0)
        icons.append(Image.fromarray(layer))
    return icons


def normalize(icon: Image.Image) -> Image.Image:
    # Near-invisible matte noise can sit far from the icon; it must not widen the crop.
    visible = icon.getchannel("A").point(lambda value: 255 if value > VISIBLE_ALPHA else 0)
    icon = icon.crop(visible.getbbox())
    scale = ICON_SIZE * ICON_FILL / max(icon.size)
    icon = icon.resize((max(1, round(icon.width * scale)), max(1, round(icon.height * scale))), Image.LANCZOS)
    canvas = Image.new("RGBA", (ICON_SIZE, ICON_SIZE), (0, 0, 0, 0))
    canvas.paste(icon, ((ICON_SIZE - icon.width) // 2, (ICON_SIZE - icon.height) // 2), icon)
    return canvas


def build_icons() -> dict[str, Image.Image]:
    icons = {element_id: Image.open(ASSETS / "elements" / f"{element_id}.png").convert("RGBA") for element_id in catalog_ids()}
    sliced = [icon for sheet in range(1, 10) for icon in slice_sheet(ASSETS / "raw_sheets" / f"sheet_{sheet:02d}.png")]
    if len(sliced) != len(REMAINING_IDS):
        raise SystemExit(f"Expected {len(REMAINING_IDS)} sheet icons, found {len(sliced)}")
    icons.update(zip(REMAINING_IDS, sliced))
    return {element_id: normalize(icon) for element_id, icon in icons.items()}


def write_preview(icons: dict[str, Image.Image], path: Path) -> None:
    cell, columns = 128, 12
    rows = (len(icons) + columns - 1) // columns
    sheet = Image.new("RGB", (columns * cell, rows * (cell + 14)), (90, 90, 90))
    draw = ImageDraw.Draw(sheet)
    for index, (element_id, icon) in enumerate(sorted(icons.items())):
        x, y = index % columns * cell, index // columns * (cell + 14)
        thumbnail = icon.resize((cell, cell), Image.LANCZOS)
        sheet.paste(thumbnail, (x, y), thumbnail)
        draw.text((x + 2, y + cell), element_id, fill="white")
    sheet.save(path)


def write_resources(icons: dict[str, Image.Image]) -> None:
    RES_DIR.mkdir(parents=True, exist_ok=True)
    for stale in RES_DIR.glob("element_*.webp"):
        stale.unlink()
    for element_id, icon in icons.items():
        icon.save(RES_DIR / f"element_{element_id}.webp", "WEBP", quality=WEBP_QUALITY, method=6)

    entries = "\n".join(f'    "{element_id}" to R.drawable.element_{element_id},' for element_id in sorted(icons))
    KOTLIN_FILE.write_text(
        "// Generated by tools/build_element_icons.py. Do not edit by hand.\n"
        "package com.artt.alchemy.ui.components\n\n"
        "import androidx.annotation.DrawableRes\n"
        "import com.artt.alchemy.R\n\n"
        "@DrawableRes\n"
        "fun elementIconRes(elementId: String): Int = elementIcons.getValue(elementId)\n\n"
        f"internal val elementIcons: Map<String, Int> = mapOf(\n{entries.rstrip(',')}\n)\n"
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--preview", type=Path, help="write a labeled contact sheet instead of resources")
    args = parser.parse_args()
    icons = build_icons()
    if args.preview:
        write_preview(icons, args.preview)
    else:
        write_resources(icons)


if __name__ == "__main__":
    main()
