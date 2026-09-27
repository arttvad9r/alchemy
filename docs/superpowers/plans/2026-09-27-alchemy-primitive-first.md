# Алхимия: primitive-first Android implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Создать самостоятельную портретную Android-игру «Алхимия» с рабочей областью перетаскивания, 120 элементами, около 180 рецептами, локальным прогрессом и пятью играбельными экранами, сначала на примитивах.

**Architecture:** Один Compose-модуль `app` без DI, доменных модулей и игровых движков. Чистые Kotlin-модели и reducer рабочей области принимают события и возвращают новое состояние; `AlchemyViewModel` владеет сессионным UI-состоянием и записывает устойчивый прогресс через `SharedPreferences`. Compose-экраны только отображают `AlchemyUiState` и отправляют события.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Canvas/pointer input Compose, AndroidX Lifecycle ViewModel, `SharedPreferences`, Compose UI tests, JUnit4, Android SDK; applicationId `com.artt.alchemy`, minSdk 31.

**Spec:** `docs/superpowers/specs/2026-09-27-alchemy-design.md`

## Global Constraints

- Создавать и изменять только `/home/artt/Projects/Alchemy-From-Scratch`; `/home/artt/Projects/Alchemy` не читать как исходник и не изменять.
- Имя приложения и видимый заголовок: «Алхимия»; основной язык интерфейса — русский.
- Портретная ориентация, `applicationId = "com.artt.alchemy"`, `minSdk = 31` (Android 12).
- Только офлайн, без аккаунтов, рекламы, покупок, аналитики, облака, сети, физического или стороннего игрового движка.
- Первый этап использует лишь примитивы: круги, текст, простые панели и Canvas; PNG из архива не копировать до пользовательской проверки механик.
- Базовые элементы: Огонь, Вода, Земля, Воздух. Каталог содержит ровно 120 элементов и 180 неупорядоченных двухэлементных рецептов.
- Контролы, используемые в сценариях, получают стабильный `Modifier.testTag`; `testTagsAsResourceId` включается на корне семантики.
- Проект закрепляет `android-engineering-v1`, поддерживает `./gradlew qualityCheck` и `./gradlew formatCode`.
- Каждый законченный task имеет отдельный Git-коммит. Не добавлять Hilt, Room, DataStore, Navigation, игровой движок, звукозагрузчик или стороннюю библиотеку без новой потребности.

## Review Focus

1. Перестановка ингредиентов должна давать тот же рецепт; тест владельца — Task 2.
2. Каждый из 120 элементов должен быть достижим из четырёх стартовых через каталог рецептов; тест владельца — Task 2.
3. Ошибочная операция с полем не должна затронуть коллекцию: неверная пара сохраняет оба экземпляра, а вывод за границу удаляет лишь экземпляр; тест владельца — Task 3.
4. Повторный рецепт не должен повторно увеличивать число уникальных открытий или повторно показывать «новый элемент»; тест владельца — Task 4.
5. Сброс в настройках должен требовать подтверждения и после подтверждения восстановить четыре стартовых элемента; тест владельца — Task 8.

---

## File Structure

```text
app/
  build.gradle.kts                         # приложение, Compose и тестовые зависимости
  src/main/AndroidManifest.xml             # launcher и portrait orientation
  src/main/java/com/artt/alchemy/
    AlchemyApplication.kt                  # test-tags root policy
    MainActivity.kt                        # activity и root composition
    data/ProgressStore.kt                  # SharedPreferences persistent snapshot
    game/AlchemyCatalog.kt                 # 120 elements, 180 recipes, achievements
    game/AlchemyEngine.kt                  # symmetric recipe lookup and discovery result
    game/GameModels.kt                     # immutable catalog/progress/workspace models
    game/WorkspaceReducer.kt               # pure events: spawn, drag, merge, clear
    ui/AlchemyViewModel.kt                 # sole app state owner and event coordinator
    ui/AlchemyApp.kt                       # root tabs and screen routing
    ui/components/PrimitiveElement.kt      # labelled circle and static primitive panels
    ui/home/HomeScreen.kt                  # palette, header, workspace and new-item dialog
    ui/home/WorkspaceCanvas.kt             # Canvas positions and pointer drag handling
    ui/elements/ElementsScreen.kt          # collection grid and filter/search
    ui/recipes/RecipesScreen.kt            # discovered recipe list/filter
    ui/achievements/AchievementsScreen.kt  # derived achievement progress
    ui/settings/SettingsScreen.kt          # feedback toggles, help, reset confirmation
    ui/theme/…                             # template theme adjusted for primitive game UI
  src/main/res/values/strings.xml          # fixed Russian labels
  src/test/java/com/artt/alchemy/game/…    # JUnit tests for pure rules
  src/androidTest/java/com/artt/alchemy/ui/… # Compose behavior tests
project-engineering.yaml
.android-engineering/…
docs/testing.md
README_RU.md
```

### Task 1: Создать воспроизводимую Android-основу

**Files:**
- Create: Android template files under `app/`, Gradle wrapper and settings files.
- Create: `project-engineering.yaml`, `.android-engineering/bootstrap-verification.json`, `docs/testing.md`, `README_RU.md`.
- Modify: `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/strings.xml`.

**Interfaces:**
- Produces: Android project with namespace `com.artt.alchemy`, Compose activity, `minSdk = 31`, stable Gradle wrapper and green `qualityCheck`.

- [ ] **Step 1: Generate the official `empty-activity` template in a scratch directory**

Run:
```bash
android create empty-activity --name='Алхимия' --minSdk=31 --output=/home/artt/.hermes/cache/scratch/alchemy-template
```

The installed CLI does not expose namespace/applicationId creation flags. In Step 2, set `namespace = "com.artt.alchemy"` and `applicationId = "com.artt.alchemy"` in the generated Gradle configuration and move Kotlin sources to that package before compiling.

- [ ] **Step 2: Configure the template for this project**

Set the launcher label to `Алхимия`, lock the activity to portrait, remove template demo UI, and retain only official dependencies generated by the template plus its standard Compose test dependencies. Do not add Navigation: top-level tabs are a closed in-memory set.

- [ ] **Step 3: Bootstrap Android Engineering v1**

Run:
```bash
~/.hermes/scripts/android-bootstrap.py /home/artt/Projects/Alchemy-From-Scratch
```

Create `docs/testing.md` with exact local/unit, instrumented, quality, formatting, and emulator commands. Create `README_RU.md` with run instructions and the primitive-first boundary.

- [ ] **Step 4: Verify the baseline**

Run: `./gradlew qualityCheck`

Expected: PASS; `.android-engineering/bootstrap-verification.json` exists and identifies the candidate.

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "chore: bootstrap Android Compose project"
```

### Task 2: Определить каталог и симметричный рецептный движок

**Files:**
- Create: `app/src/main/java/com/artt/alchemy/game/GameModels.kt`
- Create: `app/src/main/java/com/artt/alchemy/game/AlchemyCatalog.kt`
- Create: `app/src/main/java/com/artt/alchemy/game/AlchemyEngine.kt`
- Test: `app/src/test/java/com/artt/alchemy/game/AlchemyEngineTest.kt`

**Interfaces:**
- Produces: `data class ElementDefinition(val id: String, val name: String, val group: ElementGroup, val color: Long)`; `data class Recipe(val firstId: String, val secondId: String, val resultId: String)`; `object AlchemyCatalog`; `class AlchemyEngine(catalog: AlchemyCatalog)`.
- Produces: `fun recipeKey(firstId: String, secondId: String): String` and `fun combine(firstId: String, secondId: String): String?`.

- [ ] **Step 1: Write failing catalog and engine tests**

```kotlin
@Test fun catalog_has_120_unique_elements_and_180_unique_pair_keys()
@Test fun every_catalog_element_is_reachable_from_base_elements()
@Test fun combine_is_independent_of_ingredient_order()
@Test fun unknown_pair_returns_null()
@Test fun four_base_elements_are_present()
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests '*AlchemyEngineTest'`

Expected: FAIL because the game package does not yet exist.

- [ ] **Step 3: Implement immutable catalog definitions and lookup**

Put all 120 Russian-named elements and exactly 180 recipes in `AlchemyCatalog`. Use `recipeKey` that sorts IDs before joining them, reject a duplicate pair while constructing the lookup, and expose only immutable lists/maps. Use the four agreed base IDs as the initial unlock set.

- [ ] **Step 4: Run the unit test**

Run: `./gradlew testDebugUnitTest --tests '*AlchemyEngineTest'`

Expected: PASS with all four tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/artt/alchemy/game app/src/test/java/com/artt/alchemy/game
git commit -m "feat: add alchemy catalog and recipe engine"
```

### Task 3: Реализовать чистое состояние рабочей области

**Files:**
- Modify: `app/src/main/java/com/artt/alchemy/game/GameModels.kt`
- Create: `app/src/main/java/com/artt/alchemy/game/WorkspaceReducer.kt`
- Test: `app/src/test/java/com/artt/alchemy/game/WorkspaceReducerTest.kt`

**Interfaces:**
- Produces: `data class WorkspaceItem(val instanceId: Long, val elementId: String, val xFraction: Float, val yFraction: Float)`.
- Produces: `data class WorkspaceState(val items: List<WorkspaceItem>, val nextInstanceId: Long)`.
- Produces: `sealed interface WorkspaceEvent` with `Spawn`, `Move`, `Remove`, `Clear`, `ResolveOverlap`.
- Produces: `fun reduce(state: WorkspaceState, event: WorkspaceEvent, engine: AlchemyEngine): WorkspaceResult` where the result contains new workspace and optional successful `Combination`.

- [ ] **Step 1: Write failing reducer tests**

```kotlin
@Test fun spawn_creates_a_temporary_instance_without_consuming_unlock()
@Test fun invalid_overlap_keeps_both_workspace_items()
@Test fun successful_overlap_removes_exactly_two_items_and_spawns_result_at_contact()
@Test fun moving_outside_bounds_removes_only_that_workspace_item()
@Test fun clear_removes_all_workspace_items()
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests '*WorkspaceReducerTest'`

Expected: FAIL because `WorkspaceReducer` is missing.

- [ ] **Step 3: Implement `WorkspaceReducer` as a pure reducer**

Use normalized `xFraction`/`yFraction` values. `ResolveOverlap` checks only the dragged item and the first intersecting other item; it must never create triple recipes. For a valid pair, remove only those two instances and add the result at the contact position. An invalid pair is a no-op. `Move` outside `[0f, 1f]` removes the moved instance.

- [ ] **Step 4: Run the reducer test**

Run: `./gradlew testDebugUnitTest --tests '*WorkspaceReducerTest'`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/artt/alchemy/game app/src/test/java/com/artt/alchemy/game
git commit -m "feat: add workspace reducer"
```

### Task 4: Сохранить прогресс и собрать единый UI state

**Files:**
- Create: `app/src/main/java/com/artt/alchemy/data/ProgressStore.kt`
- Create: `app/src/main/java/com/artt/alchemy/ui/AlchemyViewModel.kt`
- Test: `app/src/test/java/com/artt/alchemy/game/ProgressRulesTest.kt`

**Interfaces:**
- Produces: `data class PlayerProgress(val unlockedIds: Set<String>, val knownRecipeKeys: Set<String>, val successfulMixCount: Int, val mixAttemptCount: Int, val soundEnabled: Boolean, val vibrationEnabled: Boolean)`.
- Produces: `class ProgressStore(context: Context)` with `fun load(): PlayerProgress`, `fun save(progress: PlayerProgress)`, `fun clear()`.
- Produces: `data class AlchemyUiState(val progress: PlayerProgress, val workspace: WorkspaceState, val selectedTab: AppTab, val newlyUnlockedId: String?)`.
- Produces: `class AlchemyViewModel(application: Application)` with `fun onWorkspaceEvent(event: WorkspaceEvent)`, `fun selectTab(tab: AppTab)`, `fun confirmReset()`, `fun setSoundEnabled(enabled: Boolean)`, `fun setVibrationEnabled(enabled: Boolean)`.

- [ ] **Step 1: Write failing progress tests**

```kotlin
@Test fun initial_progress_contains_only_the_four_base_elements()
@Test fun first_successful_recipe_unlocks_result_and_marks_recipe_known()
@Test fun repeated_successful_recipe_does_not_add_a_second_unique_unlock()
@Test fun reset_restores_initial_progress()
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests '*ProgressRulesTest'`

Expected: FAIL because progress transition functions are missing.

- [ ] **Step 3: Implement progress transitions and persistence**

Keep progress transformations as pure internal functions exercised by the unit tests. Persist scalar booleans/counters and string sets using `SharedPreferences`. Keep workspace in the ViewModel only so it survives top-level tab changes but is not incorrectly advertised as a permanent board. Increment `mixAttemptCount` when two objects overlap; increment `successfulMixCount` only for a valid recipe. Set `newlyUnlockedId` only for a first-time discovery.

- [ ] **Step 4: Run progress and prior game tests**

Run: `./gradlew testDebugUnitTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/artt/alchemy/data app/src/main/java/com/artt/alchemy/ui app/src/test/java/com/artt/alchemy/game
git commit -m "feat: persist alchemy progress"
```

### Task 5: Построить корневую навигацию и примитивный Home

**Files:**
- Create: `app/src/main/java/com/artt/alchemy/AlchemyApplication.kt`
- Modify: `app/src/main/java/com/artt/alchemy/MainActivity.kt`
- Create: `app/src/main/java/com/artt/alchemy/ui/AlchemyApp.kt`
- Create: `app/src/main/java/com/artt/alchemy/ui/components/PrimitiveElement.kt`
- Create: `app/src/main/java/com/artt/alchemy/ui/home/HomeScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/androidTest/java/com/artt/alchemy/ui/NavigationTest.kt`

**Interfaces:**
- Produces: `enum class AppTab { HOME, ELEMENTS, RECIPES, ACHIEVEMENTS, SETTINGS }`.
- Produces: `@Composable fun AlchemyApp(viewModel: AlchemyViewModel)` and `@Composable fun HomeScreen(state: AlchemyUiState, onEvent: (WorkspaceEvent) -> Unit, …)`.

- [ ] **Step 1: Write failing Compose navigation tests**

```kotlin
@Test fun bottom_navigation_reaches_all_five_screens()
@Test fun switching_tabs_keeps_home_workspace_state()
```

Find navigation targets by test tags, not displayed positions.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew connectedDebugAndroidTest --tests '*NavigationTest'`

Expected: FAIL because the root UI does not expose the tabs.

- [ ] **Step 3: Implement root composition and five top-level routes**

Use one `Scaffold` with a `NavigationBar`; store the selected `AppTab` in `AlchemyViewModel`. Add test tags `nav_home`, `nav_elements`, `nav_recipes`, `nav_achievements`, `nav_settings`, `home_workspace`, and `clear_workspace`. Implement Home header title/progress, dashed working-area placeholder, horizontally scrollable unlocked palette and a clear action. `PrimitiveElement` must display both a color and the Russian name.

- [ ] **Step 4: Run the navigation test**

Run: `./gradlew connectedDebugAndroidTest --tests '*NavigationTest'`

Expected: PASS on `android-phone`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main app/src/androidTest app/build.gradle.kts
git commit -m "feat: add primitive home and top-level navigation"
```

### Task 6: Сделать рабочую область с реальным перетаскиванием

**Files:**
- Create: `app/src/main/java/com/artt/alchemy/ui/home/WorkspaceCanvas.kt`
- Modify: `app/src/main/java/com/artt/alchemy/ui/home/HomeScreen.kt`
- Test: `app/src/androidTest/java/com/artt/alchemy/ui/WorkspaceJourneyTest.kt`

**Interfaces:**
- Produces: `@Composable fun WorkspaceCanvas(items: List<WorkspaceItem>, onMove: (instanceId: Long, Offset) -> Unit, modifier: Modifier = Modifier)`.
- Consumes: `WorkspaceReducer` and `AlchemyViewModel.onWorkspaceEvent`.

- [ ] **Step 1: Write failing workspace journey tests**

```kotlin
@Test fun dragging_two_known_ingredients_together_shows_the_result_on_workspace()
@Test fun dragging_an_item_beyond_workspace_boundary_removes_only_the_instance()
@Test fun invalid_recipe_leaves_both_items_visible()
```

Use the tagged canvas and semantic item labels; coordinates are permitted only inside the tagged canvas gesture after identity was established.

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew connectedDebugAndroidTest --tests '*WorkspaceJourneyTest'`

Expected: FAIL because Canvas does not process drag events.

- [ ] **Step 3: Implement `WorkspaceCanvas` and palette spawning**

Use Compose `Canvas` plus `pointerInput` drag handling. A palette tap/drag adds a temporary item into the workspace; dragged circles render labels and use their stable instance IDs in semantics. Convert pixel offsets to normalized fractions before sending `Move`; dispatch overlap resolution after move; send `Remove` when released outside the canvas. Render only circles/text/panels and a dashed border. Do not use native drag-and-drop, a physics library, or an asset.

- [ ] **Step 4: Add success feedback**

When `newlyUnlockedId` is non-null, show a dismissible dialog/card «Новый элемент» and clear it only on dismissal. Apply `LocalHapticFeedback` and a short native `ToneGenerator` feedback on a successful combination only when their respective settings are enabled; release the generator with composition lifecycle.

- [ ] **Step 5: Run interaction checks**

Run: `./gradlew connectedDebugAndroidTest --tests '*WorkspaceJourneyTest'`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/artt/alchemy/ui/home app/src/main/java/com/artt/alchemy/ui app/src/androidTest/java/com/artt/alchemy/ui
git commit -m "feat: add draggable alchemy workspace"
```

### Task 7: Реализовать экраны коллекции, рецептов и достижений

**Files:**
- Create: `app/src/main/java/com/artt/alchemy/ui/elements/ElementsScreen.kt`
- Create: `app/src/main/java/com/artt/alchemy/ui/recipes/RecipesScreen.kt`
- Create: `app/src/main/java/com/artt/alchemy/ui/achievements/AchievementsScreen.kt`
- Modify: `app/src/main/java/com/artt/alchemy/game/AlchemyCatalog.kt`
- Test: `app/src/androidTest/java/com/artt/alchemy/ui/CollectionScreensTest.kt`

**Interfaces:**
- Produces: `data class AchievementDefinition(val id: String, val title: String, val target: Int, val current: (PlayerProgress) -> Int)`.
- Produces: three state-only Compose screens driven by `AlchemyUiState.progress`.

- [ ] **Step 1: Write failing screen behavior tests**

```kotlin
@Test fun elements_screen_shows_open_and_locked_cards_without_revealing_locked_names()
@Test fun recipe_screen_shows_only_known_recipe_pairs()
@Test fun element_search_filters_collection_by_visible_name()
@Test fun achievements_show_derived_progress()
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew connectedDebugAndroidTest --tests '*CollectionScreensTest'`

Expected: FAIL because the three screens are placeholders.

- [ ] **Step 3: Implement collection, recipes and achievements**

Create a scrollable 120-card grid with visible names for unlocked entries and a generic «Не открыт» silhouette for locked ones. Add local in-memory text search and group filter only to Elements. Show only known pair/result rows in Recipes. Define a small fixed achievement list derived from unique unlocks, successful mixes, and attempts; do not add a separate achievement persistence model.

- [ ] **Step 4: Run the screen tests**

Run: `./gradlew connectedDebugAndroidTest --tests '*CollectionScreensTest'`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/artt/alchemy/game/AlchemyCatalog.kt app/src/main/java/com/artt/alchemy/ui/elements app/src/main/java/com/artt/alchemy/ui/recipes app/src/main/java/com/artt/alchemy/ui/achievements app/src/androidTest/java/com/artt/alchemy/ui
git commit -m "feat: add collection recipes and achievements"
```

### Task 8: Реализовать настройки, сброс и доступность

**Files:**
- Create: `app/src/main/java/com/artt/alchemy/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/artt/alchemy/ui/AlchemyViewModel.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/androidTest/java/com/artt/alchemy/ui/SettingsScreenTest.kt`

**Interfaces:**
- Produces: `@Composable fun SettingsScreen(state: AlchemyUiState, onSoundChanged: (Boolean) -> Unit, onVibrationChanged: (Boolean) -> Unit, onRequestReset: () -> Unit, onConfirmReset: () -> Unit, onDismissReset: () -> Unit)`.

- [ ] **Step 1: Write failing settings tests**

```kotlin
@Test fun sound_and_vibration_switches_change_persisted_state()
@Test fun reset_requires_confirmation_before_progress_changes()
@Test fun confirmed_reset_restores_the_four_base_elements()
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew connectedDebugAndroidTest --tests '*SettingsScreenTest'`

Expected: FAIL because settings UI is absent.

- [ ] **Step 3: Implement settings and confirmation dialog**

Use Material switches with Russian labels and state descriptions. Implement Help as a static explanation of dragging, overlap, invalid pairs, and delete-by-boundary. Present a blocking confirmation dialog before calling `ProgressStore.clear()` and replacing UI state with base progress. Ensure all targets have content descriptions/test tags and text wraps at a 1.5 font scale.

- [ ] **Step 4: Run the settings test**

Run: `./gradlew connectedDebugAndroidTest --tests '*SettingsScreenTest'`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/artt/alchemy/ui/settings app/src/main/java/com/artt/alchemy/ui/AlchemyViewModel.kt app/src/main/res/values/strings.xml app/src/androidTest/java/com/artt/alchemy/ui
 git commit -m "feat: add settings and progress reset"
```

### Task 9: Выполнить выпускную проверку primitive-сборки

**Files:**
- Create: `docs/verification/primitive-build-acceptance.md`
- Modify: `README_RU.md`

**Interfaces:**
- Produces: repeatable evidence for the primitive-first acceptance boundary; no production API changes.

- [ ] **Step 1: Run full deterministic gate**

Run:
```bash
./gradlew formatCode
./gradlew qualityCheck
./gradlew assembleDebug
```

Expected: all commands PASS; no formatter or static-analysis debt is hidden by a baseline.

- [ ] **Step 2: Run the app on `android-phone` and exercise the acceptance journey**

Use `android run`, then verify: four base elements shown; palette spawns field copies; a valid pair discovers a result; invalid overlap changes nothing; boundary deletion preserves collection; five tabs work; progress survives relaunch; reset needs confirmation. Capture a screenshot and a layout dump for each top-level screen, then run the project geometry checker required by `android-engineering-v1`.

- [ ] **Step 3: Verify accessibility and visual resilience**

Run Compose behavior tests at font scale `1.5`; confirm text labels remain visible and test hooks resolve. Do not add screenshot-test infrastructure unless this run exposes a regression that behavior tests cannot cover.

- [ ] **Step 4: Write the acceptance record**

Record exact commands, candidate SHA, emulator/device, outputs, the exercised journey, screenshot/layout artifact paths, and any known intentional limitations. Update `README_RU.md` with the verified debug-install command and explicitly state that assets are deferred until user mechanic approval.

- [ ] **Step 5: Commit**

```bash
git add docs/verification README_RU.md
git commit -m "docs: record primitive build verification"
```

## Self-Review

- **Spec coverage:** Tasks 2–4 implement 120/180 data, symmetric recipes, first discovery and local persistence. Tasks 5–6 implement portrait Home, bottom palette, retained session workspace, real drag, boundary deletion, invalid no-op, primitive feedback and accessibility. Tasks 7–8 implement the remaining four product areas, achievements, settings and confirmed reset. Task 9 verifies offline primitive acceptance before any asset integration.
- **Step scan:** Each task names exact files, emitted interfaces, test behavior, execution command and a small focused commit. The only intentionally deferred work is packaged assets and final polish, as required by the spec.
- **Type consistency:** `AlchemyCatalog`/`AlchemyEngine` feed `WorkspaceReducer`; reducer emits `Combination` into `AlchemyViewModel`; screens consume `AlchemyUiState`; only `ProgressStore` persists `PlayerProgress`.
- **Review Focus:** Each listed risk is assigned to Task 2, 3, 4 or 8 and has an explicit test name.
- **Proportion:** The plan locks requirements, test points and interfaces without prescribing method bodies or adding a premature domain layer, DI, navigation framework, persistence library or asset pipeline.
