# Звуки и музыка

`tools/build_audio.py` (нужен ffmpeg) кладёт эти файлы в `app/src/main/res/raw`.

## Эффекты и музыка — Stable Audio 3

Сгенерированы локально моделями Stable Audio 3 (`medium` и `small-sfx`) от Stability AI и выбраны на слух из нескольких вариантов. Файлы уже обрезаны, выровнены по громкости, а музыка склеена в петлю, поэтому скрипт копирует их без перекодирования. Модели распространяются под [Stability AI Community License](https://stability.ai/community-license-agreement): права на результат генерации остаются у автора, коммерческое использование разрешено.

| Файл | Где звучит |
| --- | --- |
| `stable-audio/place.ogg` | элемент поставлен на поле |
| `stable-audio/discover.ogg` | открыт новый элемент |
| `stable-audio/no_match.ogg` | пара не сочетается |
| `stable-audio/remove.ogg` | элемент убран за край поля |
| `stable-audio/click.ogg` | вкладки и кнопки |
| `stable-audio/page.ogg` | переход на экран рецептов |
| `stable-audio/music.ogg` | фоновая музыка, по кругу |

Очистка поля, сброс прогресса и переключатели в настройках звука не издают — только вибрацию, где она была.

## Известное сочетание — Kenney, CC0

`kenney/maximize_006.ogg` из пака [Interface Sounds](https://kenney.nl/assets/interface-sounds). Лицензия CC0 (`kenney/License.txt`), указание автора не требуется.
