# Материалы оформления

Это исходные материалы для будущего визуального оформления. Они **не подключены к APK**: игра остаётся primitive-first, пока не будет отдельно подтверждён переход на финальные ассеты.

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
