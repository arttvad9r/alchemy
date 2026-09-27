# Выпускная проверка primitive-сборки

Дата проверки: 27 сентября 2026 года  
Кодовый кандидат: `30cad1a` (`fix: use AGP 9 Kotlin source sets`)

`57244be` заменил алгоритмический filler на явный курируемый граф рецептов и усилил runtime-тесты. `30cad1a` дополнительно исправил скрытую проблему AGP 9 built-in Kotlin: все Kotlin sources/tests перенесены из `src/*/java` в штатные `src/*/kotlin`, после чего выполнена полная перекомпиляция без опоры на старые build outputs.
Пакет: `com.artt.alchemy`

## Среда

- Свежая ручная проверка hardening-кандидата: AVD `qa-api36-ime` (`emulator-5554`).
- Инструментальные Compose-тесты: тот же AVD `qa-api36-ime`.
- Сборка: Kotlin + Jetpack Compose, debug-вариант.

## Детерминированный gate

Выполнены успешно:

```bash
./gradlew formatCode
./gradlew testDebugUnitTest qualityCheck assembleDebug --rerun-tasks
./gradlew assembleDebugAndroidTest --rerun-tasks
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w \
  -e class com.artt.alchemy.ui.NavigationTest,com.artt.alchemy.ui.WorkspaceJourneyTest,com.artt.alchemy.ui.CollectionScreensTest,com.artt.alchemy.ui.SettingsScreenTest \
  com.artt.alchemy.test/androidx.test.runner.AndroidJUnitRunner
```

Результат host unit-прогона: `15 tests, 0 failures, 0 errors`.

Результат инструментального прогона: `OK (10 tests)`. После полного rerun реально созданы built-in Kotlin class outputs, включая `AlchemyCatalog.class` и `AlchemyEngine.class`; package-specific crash scan после instrumentation и ручного сценария пуст.

## Пройденный пользовательский путь

После свежей сборки APK был установлен, данные приложения очищены и Activity запущена на `qa-api36-ime`:

```bash
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell pm clear com.artt.alchemy
adb -s emulator-5554 shell am start -W -n com.artt.alchemy/.MainActivity
```

Для проверки механики использовались настоящие `adb shell input tap/swipe`, а не Compose callbacks.

Проверено вручную:

1. На Home показаны четыре базовых элемента.
2. Нажатия на палитру создают копии в рабочей области.
3. Перетаскивание `Вода` на `Огонь` открыло `Пар`: счётчик изменился с `4 / 120` на `5 / 120`.
4. После принудительного завершения и повторного запуска прогресс сохранился: `5 / 120`.
5. Открываются пять разделов: Дом, Элементы, Рецепты, Достижения, Настройки.
6. Экран сброса показывает блокирующее подтверждение с действиями «Отмена» и «Сбросить»; отмена не меняет прогресс.
7. Compose runtime-тесты дополнительно проверяют, что неверная пара оставляет оба экземпляра на поле, перенос за границу удаляет только временный экземпляр и не убирает элемент из палитры, а переход в другой раздел и обратно сохраняет фактическое содержимое рабочей области.
8. Настройки звука и вибрации проверены через recreation Activity: изменённые значения сохраняются и загружаются повторно.

## Артефакты

Свежие runtime-артефакты hardening-кандидата, снятые после установки текущего APK:

- чистый старт: `artifacts/hardened-home-fresh.png`;
- настоящий tap/tap/swipe и результат «Пар» (`5 / 120`): `artifacts/hardened-home-combination.png`;
- состояние после force-stop/relaunch: `artifacts/hardened-home-relaunch.png`;
- неверная пара `Огонь + Огонь` оставляет оба экземпляра: `artifacts/hardened-invalid-pair.png`;
- перенос за границу удаляет временный экземпляр, палитра сохраняется: `artifacts/hardened-boundary-delete.png`.

Пять `hardened-*.png` выше пересняты после полной AGP 9 перекомпиляции exact-candidate `30cad1a`. Старые `home/elements/recipes/achievements/settings*.png|json`, `reset-confirmation*` и `*-font-150*` были сняты на более раннем `6e791fa`; они остаются только исторической визуальной документацией.

## Доступность и адаптация

Проверка при `font_scale=1.5` показала: заголовки, счётчик, базовые элементы, настройки, инструкция, кнопка сброса и навигация остаются видимыми. Длинные подписи палитры и нижней навигации при этом могут переноситься на несколько строк, но не обрезаются и не перекрываются.

`ui-geometry-check.py` был запущен по layout dump Home. Он подтвердил размер hit-target, системные зоны, вложенность, равномерность рядов и отсутствие text ellipsis. Проверка sibling-overlap сообщила шесть ложных срабатываний на безымянные перекрывающиеся Compose semantic wrapper-узлы (`View/-`); визуальные screenshot и плоские layout dump не показывают перекрытия интерактивного UI. Это ограничение инструмента для данного Compose semantic tree, а не известный дефект экрана.

## Граница этапа

Это завершённая primitive-сборка механик: данные, 120 элементов, 180 уникальных симметричных пар рецептов, локальный прогресс, drag-and-drop, коллекции, достижения и настройки работают без сети. Рецепты теперь заданы явным курируемым графом; алгоритмический filler, который обеспечивал только нужное количество и достижимость, удалён. Готовые PNG-ассеты намеренно не подключены до явного подтверждения механик пользователем; следующий этап — замена primitive-визуала на предоставленные ассеты и финальная художественная полировка.
