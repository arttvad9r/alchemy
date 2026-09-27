# Проверка проекта «Алхимия»

Все команды запускаются из корня репозитория.

## Локальные unit-тесты

```bash
./gradlew testDebugUnitTest
```

## Инструментальные Compose-тесты

Подключите запущенный эмулятор или устройство, затем:

```bash
adb devices
./gradlew connectedDebugAndroidTest
```

## Эмулятор

```bash
emulator -list-avds
emulator @<имя_из_списка>
adb wait-for-device
./gradlew connectedDebugAndroidTest
```

## Полная проверка качества

```bash
./gradlew qualityCheck
```

`qualityCheck` запускает ktlint, detekt, Android Lint и локальные unit-тесты.

## Форматирование

```bash
./gradlew formatCode
./gradlew qualityCheck
```
