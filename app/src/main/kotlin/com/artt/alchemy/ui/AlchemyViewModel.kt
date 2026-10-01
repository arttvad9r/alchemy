package com.artt.alchemy.ui

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.artt.alchemy.audio.BackgroundMusic
import com.artt.alchemy.audio.ComboStreak
import com.artt.alchemy.audio.Haptic
import com.artt.alchemy.audio.Haptics
import com.artt.alchemy.audio.Sound
import com.artt.alchemy.audio.SoundEffects
import com.artt.alchemy.audio.panAt
import com.artt.alchemy.data.PlayerProgress
import com.artt.alchemy.data.ProgressStore
import com.artt.alchemy.data.initialPlayerProgress
import com.artt.alchemy.data.isComplete
import com.artt.alchemy.data.parsePlayerProgress
import com.artt.alchemy.data.recordAttempt
import com.artt.alchemy.data.requestHint
import com.artt.alchemy.data.toJson
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.AlchemyEngine
import com.artt.alchemy.game.ElementRarity
import com.artt.alchemy.game.ElementSort
import com.artt.alchemy.game.Recipe
import com.artt.alchemy.game.WorkspaceEvent
import com.artt.alchemy.game.WorkspaceState
import com.artt.alchemy.game.recipeForKey
import com.artt.alchemy.game.reduce
import com.artt.alchemy.ui.achievements.newlyCompletedAchievements

private const val CENTER = 0.5f

// How long the music stays lowered under each big moment.
private const val DISCOVER_DUCK_MILLIS = 1300L
private const val GRAND_DUCK_MILLIS = 2600L
private const val ACHIEVEMENT_DUCK_MILLIS = 2000L

enum class AppTab {
    HOME,
    ELEMENTS,
    RECIPES,
    ACHIEVEMENTS,
    SETTINGS
}

/** A successful combination to celebrate at the result's workspace position. */
data class CombinationEffect(
    val id: Long,
    val xFraction: Float,
    val yFraction: Float,
    val isDiscovery: Boolean,
    val rarity: ElementRarity,
    val resultInstanceId: Long,
    val sources: List<EffectSource>
)

data class AlchemyUiState(
    val progress: PlayerProgress,
    val workspace: WorkspaceState = WorkspaceState(),
    val selectedTab: AppTab = AppTab.HOME,
    val combinationEffect: CombinationEffect? = null,
    val itemTransitions: List<ItemTransition> = emptyList(),
    val isResetConfirmationVisible: Boolean = false,
    // Discovery cards, the finished collection and achievement banners waiting their turn; the first one is on stage.
    val reveals: List<Reveal> = emptyList(),
    // Elements found this session that the catalog has not shown yet.
    val freshElementIds: Set<String> = emptySet(),
    // A save read from a file, waiting for the player to agree to replace the current progress.
    val pendingImport: PlayerProgress? = null,
    val transferResult: TransferResult? = null,
    // The first-run tip on screen while the progress has not marked them seen.
    val tipStep: Int = 0
) {
    /** The reveal on stage now, if any. */
    val reveal: Reveal? get() = reveals.firstOrNull()
}

/** How saving progress to a file or loading it from one ended, shown to the player once. */
enum class TransferResult {
    EXPORTED,
    EXPORT_FAILED,
    IMPORTED,
    IMPORT_INVALID
}

class AlchemyViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = AlchemyEngine(AlchemyCatalog)
    private val store = ProgressStore(application)

    var state by mutableStateOf(AlchemyUiState(progress = store.load()))
        private set

    private var combinationEffectCount = 0L

    private val sounds = SoundEffects(application)
    private val haptics = Haptics(application)
    private val music = BackgroundMusic(application)
    private val streak = ComboStreak()

    init {
        applyVolumes()
    }

    fun onWorkspaceEvent(requested: WorkspaceEvent) {
        // While a tip sits on the workspace, elements placed automatically land clear of it.
        val event = if (requested is WorkspaceEvent.SpawnAutomatically && !state.progress.onboardingSeen && state.tipStep > 0) {
            requested.copy(keepClear = tipHalf(state.workspace.items))
        } else {
            requested
        }
        val result = reduce(state.workspace, event, engine)
        val progress = if (result.attemptedMix) state.progress.recordAttempt(result.combination) else state.progress
        val newlyUnlockedId = result.combination?.resultId?.takeUnless(state.progress.unlockedIds::contains)

        // The reducer appends the combination result as the last workspace item.
        val effect = result.combination?.let {
            val item = result.workspace.items.last()
            CombinationEffect(
                id = ++combinationEffectCount,
                xFraction = item.xFraction,
                yFraction = item.yFraction,
                isDiscovery = newlyUnlockedId != null,
                rarity = AlchemyCatalog.rarityById.getValue(it.resultId),
                resultInstanceId = item.instanceId,
                sources = effectSources(state.workspace, result)
            )
        }

        val transitions = itemTransitions(state.workspace, result, event)
        // Heard from where it happened: the mix, or the item that came, went or was refused.
        val where = effect?.xFraction ?: transitions.firstOrNull()?.xFraction ?: CENTER
        workspaceFeedback(event, state.workspace, result, discovered = newlyUnlockedId?.let(AlchemyCatalog.rarityById::getValue))?.let { play(it, where) }

        // The tips follow what the player does; after the last one they are done for good.
        val tipStep = if (progress.onboardingSeen) state.tipStep else tipAfter(state.tipStep, event, state.workspace, result)
        val onboarded = if (tipStep >= TIP_COUNT) progress.copy(onboardingSeen = true) else progress
        val reveals = listOfNotNull(newlyUnlockedId?.let(Reveal::Discovery)) +
            listOfNotNull(Reveal.Completion.takeIf { progress.isComplete && !state.progress.isComplete }) +
            newlyCompletedAchievements(state.progress, progress).map(Reveal::Achievement)

        if (onboarded != state.progress) store.save(onboarded)
        state = state.copy(
            combinationEffect = effect ?: state.combinationEffect,
            // Transitions pile up until Home takes them, so none is lost between frames.
            itemTransitions = if (transitions.isEmpty()) state.itemTransitions else state.itemTransitions + transitions,
            progress = onboarded,
            workspace = result.workspace,
            freshElementIds = newlyUnlockedId?.let { state.freshElementIds + it } ?: state.freshElementIds,
            reveals = state.reveals.enqueue(reveals),
            tipStep = tipStep.coerceAtMost(TIP_COUNT - 1)
        )
    }

    /** Puts both ingredients of a known recipe on the workspace and shows it, as if each had been tapped in the palette. */
    fun placeRecipe(recipe: Recipe) {
        placeElements(listOf(recipe.firstId, recipe.secondId))
    }

    /** Asks for a hint, or for the next step of the one already shown. */
    fun requestHint() {
        playSound(Sound.HINT)
        updateProgress { requestHint() }
    }

    /** Puts the ingredients the hint has revealed so far on the workspace. */
    fun placeHint() {
        val hint = state.progress.activeHint ?: return
        val recipe = recipeForKey(hint.recipeKey) ?: return
        placeElements(if (hint.step == 1) listOf(recipe.firstId) else listOf(recipe.firstId, recipe.secondId))
    }

    private fun placeElements(elementIds: List<String>) {
        elementIds.forEach { onWorkspaceEvent(WorkspaceEvent.SpawnAutomatically(it)) }
        selectTab(AppTab.HOME)
    }

    fun setPaletteSort(sort: ElementSort) {
        playSound(Sound.CLICK)
        updateProgress { copy(paletteSort = sort) }
    }

    fun consumeCombinationEffect() {
        state = state.copy(combinationEffect = null)
    }

    fun consumeItemTransitions() {
        state = state.copy(itemTransitions = emptyList())
    }

    fun selectTab(tab: AppTab) {
        if (tab != state.selectedTab) playSound(if (tab == AppTab.RECIPES) Sound.PAGE else Sound.CLICK)
        state = state.copy(selectedTab = tab)
    }

    /** An element was taken in hand, from the palette or on the workspace. */
    fun onPickUp() {
        playSound(Sound.PICKUP)
        vibrate(Haptic.TICK)
    }

    fun onButtonClick() {
        playSound(Sound.CLICK)
    }

    /** The achievement banner came on screen: the reward chime and a tap. */
    fun onAchievementShown() {
        playSound(Sound.ACHIEVEMENT)
        vibrate(Haptic.ACHIEVEMENT)
        music.duck(ACHIEVEMENT_DUCK_MILLIS)
    }

    fun dismissAchievement(id: String) {
        dismiss(Reveal.Achievement(id))
    }

    fun showCompletion() {
        playSound(Sound.CLICK)
        state = state.copy(reveals = state.reveals.enqueue(listOf(Reveal.Completion)))
    }

    fun dismissCompletion() {
        dismiss(Reveal.Completion)
    }

    fun skipTips() {
        playSound(Sound.CLICK)
        updateProgress { copy(onboardingSeen = true) }
    }

    fun showTips() {
        playSound(Sound.CLICK)
        updateProgress { copy(onboardingSeen = false) }
        state = state.copy(tipStep = 0)
    }

    fun markElementsSeen() {
        if (state.freshElementIds.isNotEmpty()) state = state.copy(freshElementIds = emptySet())
    }

    fun dismissNewElement(elementId: String) {
        dismiss(Reveal.Discovery(elementId))
    }

    private fun dismiss(reveal: Reveal) {
        state = state.copy(reveals = state.reveals - reveal)
    }

    fun requestReset() {
        playSound(Sound.CLICK)
        state = state.copy(isResetConfirmationVisible = true)
    }

    fun dismissReset() {
        playSound(Sound.CLICK)
        state = state.copy(isResetConfirmationVisible = false)
    }

    fun confirmReset() {
        store.clear()
        replaceProgress(initialPlayerProgress())
    }

    /** Writes the progress as JSON to the document the player picked. */
    fun exportProgress(uri: Uri) {
        val written = runCatching {
            getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use { it.write(state.progress.toJson().toByteArray()) } != null
        }.getOrDefault(false)
        state = state.copy(transferResult = if (written) TransferResult.EXPORTED else TransferResult.EXPORT_FAILED)
    }

    /** Reads a save from the picked document; a file that is not a save changes nothing, a good one waits for confirmation. */
    fun readImport(uri: Uri) {
        val progress = runCatching {
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
        }.getOrNull()?.let(::parsePlayerProgress)
        state = if (progress == null) state.copy(transferResult = TransferResult.IMPORT_INVALID) else state.copy(pendingImport = progress)
    }

    fun dismissImport() {
        playSound(Sound.CLICK)
        state = state.copy(pendingImport = null)
    }

    fun confirmImport() {
        val progress = state.pendingImport ?: return
        store.save(progress)
        replaceProgress(progress)
        state = state.copy(transferResult = TransferResult.IMPORTED)
    }

    fun dismissTransferResult() {
        state = state.copy(transferResult = null)
    }

    private fun replaceProgress(progress: PlayerProgress) {
        state = AlchemyUiState(progress = progress, selectedTab = AppTab.SETTINGS)
        applyVolumes()
        if (progress.musicEnabled) resumeMusic() else music.pause()
    }

    private fun applyVolumes() {
        sounds.level = state.progress.effectsVolume
        music.level = state.progress.musicVolume
    }

    fun setSoundEnabled(enabled: Boolean) {
        updateProgress { copy(soundEnabled = enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        updateProgress { copy(vibrationEnabled = enabled) }
        vibrate(Haptic.CLICK)
    }

    fun setMusicVolume(volume: Float) {
        updateProgress { copy(musicVolume = volume.coerceIn(0f, 1f)) }
        music.level = state.progress.musicVolume
    }

    fun setEffectsVolume(volume: Float) {
        updateProgress { copy(effectsVolume = volume.coerceIn(0f, 1f)) }
        sounds.level = state.progress.effectsVolume
    }

    /** The player has let go of the effects slider: a click at the new volume shows what it sounds like. */
    fun previewEffectsVolume() {
        playSound(Sound.CLICK)
    }

    fun setReducedMotion(reduced: Boolean) {
        updateProgress { copy(reducedMotion = reduced) }
    }

    fun setMusicEnabled(enabled: Boolean) {
        updateProgress { copy(musicEnabled = enabled) }
        if (enabled) resumeMusic() else music.pause()
    }

    /** Plays the music while the app is on screen, if the player wants it. */
    fun resumeMusic() {
        if (state.progress.musicEnabled) music.start()
    }

    fun pauseMusic() {
        music.pause()
    }

    override fun onCleared() {
        sounds.release()
        music.release()
    }

    private fun play(feedback: GameFeedback, xFraction: Float) {
        val pan = panAt(xFraction)
        when (feedback) {
            GameFeedback.PLACE -> feedback(Sound.PLACE, Haptic.TICK, pan)

            // Mixes in quick succession climb in pitch, so a good run sounds like one.
            GameFeedback.COMBINE -> feedback(Sound.COMBINE, Haptic.CLICK, pan, streak.hit(SystemClock.uptimeMillis()))

            GameFeedback.DISCOVER -> {
                streak.hit(SystemClock.uptimeMillis())
                feedback(Sound.DISCOVER, Haptic.DISCOVER, pan)
                music.duck(DISCOVER_DUCK_MILLIS)
            }

            GameFeedback.DISCOVER_GRAND -> {
                streak.hit(SystemClock.uptimeMillis())
                feedback(Sound.DISCOVER_GRAND, Haptic.DISCOVER_GRAND, pan)
                music.duck(GRAND_DUCK_MILLIS)
            }

            GameFeedback.NO_MATCH -> {
                streak.reset()
                feedback(Sound.NO_MATCH, Haptic.TICK, pan)
            }

            GameFeedback.REMOVE -> feedback(Sound.REMOVE, Haptic.TICK, pan)

            GameFeedback.CLEAR -> feedback(Sound.WHOOSH, Haptic.CLICK, 0f)
        }
    }

    private fun feedback(sound: Sound, haptic: Haptic, pan: Float, semitones: Int = 0) {
        if (state.progress.soundEnabled) sounds.play(sound, pan, semitones.toFloat())
        vibrate(haptic)
    }

    private fun playSound(sound: Sound) {
        if (state.progress.soundEnabled) sounds.play(sound)
    }

    private fun vibrate(haptic: Haptic) {
        if (state.progress.vibrationEnabled) haptics.perform(haptic)
    }

    private fun updateProgress(transform: PlayerProgress.() -> PlayerProgress) {
        val progress = state.progress.transform()
        store.save(progress)
        state = state.copy(progress = progress)
    }
}
