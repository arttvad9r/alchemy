# Выпускная проверка primitive-сборки

Дата проверки: 27 сентября 2026 года  
Кандидат: `6e791fa` (`fix: polish primitive collection layout`)  
Пакет: `com.artt.alchemy`

## Среда

- Ручная проверка: AVD `android-phone`, портрет `1080×2400`.
- Инструментальные Compose-тесты: AVD `qa-api36-ime`.
- Сборка: Kotlin + Jetpack Compose, debug-вариант.

## Детерминированный gate

Выполнены успешно:

```bash
./gradlew formatCode
./gradlew qualityCheck
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew installDebug installDebugAndroidTest
adb -s emulator-5556 shell am instrument -w \
  -e class com.artt.alchemy.ui.NavigationTest,com.artt.alchemy.ui.WorkspaceJourneyTest,com.artt.alchemy.ui.CollectionScreensTest,com.artt.alchemy.ui.SettingsScreenTest \
  com.artt.alchemy.test/androidx.test.runner.AndroidJUnitRunner
```

Результат инструментального прогона: `OK (7 tests)`.

## Пройденный пользовательский путь

Приложение запущено на `android-phone` командой:

```bash
android run --apks app/build/outputs/apk/debug/app-debug.apk \
  --activity com.artt.alchemy.MainActivity --device emulator-5554
```

Проверено вручную:

1. На Home показаны четыре базовых элемента.
2. Нажатия на палитру создают копии в рабочей области.
3. Перетаскивание `Вода` на `Огонь` открыло `Пар`: счётчик изменился с `4 / 120` на `5 / 120`.
4. После принудительного завершения и повторного запуска прогресс сохранился: `5 / 120`.
5. Открываются пять разделов: Дом, Элементы, Рецепты, Достижения, Настройки.
6. Экран сброса показывает блокирующее подтверждение с действиями «Отмена» и «Сбросить»; отмена не меняет прогресс.
7. Поведение некорректной пары, удаления за границей и переходов дополнительно покрыто Compose-тестами `WorkspaceJourneyTest` и `NavigationTest`.

## Артефакты

Для каждого верхнеуровневого раздела сохранены screenshot и layout dump:

- `artifacts/home.png`, `artifacts/home.json`
- `artifacts/elements.png`, `artifacts/elements.json`
- `artifacts/recipes.png`, `artifacts/recipes.json`
- `artifacts/achievements.png`, `artifacts/achievements.json`
- `artifacts/settings.png`, `artifacts/settings.json`

Дополнительные доказательства:

- успешное открытие «Пара»: `artifacts/home-combination.png`, `artifacts/home-combination.json`;
- сохранение после перезапуска: `artifacts/home-relaunch.json`;
- подтверждение сброса: `artifacts/reset-confirmation.png`, `artifacts/reset-confirmation.json`;
- масштаб шрифта `1.5`: `artifacts/home-font-150.png`, `artifacts/home-font-150.json`, `artifacts/settings-font-150.png`, `artifacts/settings-font-150.json`.

## Доступность и адаптация

Проверка при `font_scale=1.5` показала: заголовки, счётчик, базовые элементы, настройки, инструкция, кнопка сброса и навигация остаются видимыми. Длинные подписи палитры и нижней навигации при этом могут переноситься на несколько строк, но не обрезаются и не перекрываются.

`ui-geometry-check.py` был запущен по layout dump Home. Он подтвердил размер hit-target, системные зоны, вложенность, равномерность рядов и отсутствие text ellipsis. Проверка sibling-overlap сообщила шесть ложных срабатываний на безымянные перекрывающиеся Compose semantic wrapper-узлы (`View/-`); визуальные screenshot и плоские layout dump не показывают перекрытия интерактивного UI. Это ограничение инструмента для данного Compose semantic tree, а не известный дефект экрана.

## Граница этапа

Это завершённая primitive-сборка механик: данные, 120 элементов, 180 симметричных рецептов, локальный прогресс, drag-and-drop, коллекции, достижения и настройки работают без сети. Готовые PNG-ассеты намеренно не подключены до явного подтверждения механик пользователем; следующий этап — замена primitive-визуала на предоставленные ассеты и финальная художественная полировка.
