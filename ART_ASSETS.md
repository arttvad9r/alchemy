# Материалы оформления

Это исходные материалы для визуального оформления. В APK подключены иконки элементов и ассеты оболочки, перечисленные в `UI_ASSETS` скрипта `tools/build_ui_assets.py` (фон, иконки навигации и настроек, рамки редкости, магический круг, эффекты смешивания, кнопки, переключатели, вкладки, поле поиска, панели диалогов, карточки каталога, панель неоткрытого элемента); остальные пока не используются.

## Иконки элементов

Иконки собирает `tools/build_element_icons.py` (нужны numpy, scipy и pillow, только для разработки) в `app/src/main/res/drawable-nodpi/element_<id>.webp` (256×256) и генерирует `ElementIcons.kt`:

- 42 иконки из `asset_manifest_catalog.*` берутся из `assets/elements/<id>.png`, имена у них верные;
- остальные 138 **нарезаются заново из `raw_sheets/sheet_01…09.png`**. Готовые файлы `assets/elements/<id>.png` и `assets/elements/trimmed/<id>.png` для этих 138 id подписаны неверно: при нарезке пропущены 6 ячеек `sheet_06` (market, barrel, juice, wine, beer, tavern), а имена перемешаны. Не используйте эти файлы по имени. Порядок иконок в листах (по строкам) задан списком `REMAINING_IDS` в скрипте.

## `assets/`

Все ассеты лежат в одной структуре, сгруппированной по назначению:

- `elements/` — все иконки элементов; `trimmed/` содержит их обрезанные варианты.
- `backgrounds/`, `combine_scene/`, `effects/`, `props/`, `ui/` — фоны, сцена объединения, эффекты, реквизит и интерфейс.
- `previews/` и `raw_sheets/` — листы предпросмотра и исходные листы.

Исходные описания и манифесты сохранены в корне `assets/` с понятными именами: `README_CATALOG_RU.md`, `README_REMAINING_ELEMENTS_RU.md`, `asset_manifest_catalog.*` и `asset_manifest_remaining_elements.*`.

## `references/`

Экранные референсы переименованы по содержимому:

1. `01-combine-workspace.png` — рабочее поле комбинации.
2. `02-recipes-screen.png` — экран рецептов.
3. `03-elements-catalog.png` — каталог элементов.
4. `04-achievements-screen.png` — достижения и квесты.
5. `05-settings-screen.png` — настройки.

Исходные ZIP-архивы не хранятся в репозитории: их содержимое распаковано в `assets/`, а оригиналы остаются в `/home/artt/Downloads/Alchemy`.
