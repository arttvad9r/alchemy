package com.artt.alchemy.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.artt.alchemy.audio.BackgroundMusic
import com.artt.alchemy.audio.Haptic
import com.artt.alchemy.audio.Haptics
import com.artt.alchemy.audio.Sound
import com.artt.alchemy.audio.SoundEffects
import com.artt.alchemy.data.PlayerProgress
import com.artt.alchemy.data.ProgressStore
import com.artt.alchemy.data.initialPlayerProgress
import com.artt.alchemy.data.recordAttempt
import com.artt.alchemy.data.requestHint
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
    val newlyUnlockedId: String? = null,
    val combinationEffect: CombinationEffect? = null,
    val itemTransitions: List<ItemTransition> = emptyList(),
    val isResetConfirmationVisible: Boolean = false,
    // Ids of achievements earned and not yet announced; the first one is on screen.
    val achievementQueue: List<String> = emptyList(),
    // Elements found this session that the catalog has not shown yet.
    val freshElementIds: Set<String> = emptySet()
)

class AlchemyViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = AlchemyEngine(AlchemyCatalog)
    private val store = ProgressStore(application)

    var state by mutableStateOf(AlchemyUiState(progress = store.load()))
        private set

    private var combinationEffectCount = 0L

    private val sounds = SoundEffects(application)
    private val haptics = Haptics(application)
    private val music = BackgroundMusic(application)

    fun onWorkspaceEvent(event: WorkspaceEvent) {
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
        workspaceFeedback(event, state.workspace, result, discovered = newlyUnlockedId?.let(AlchemyCatalog.rarityById::getValue))?.let(::play)

        if (progress != state.progress) store.save(progress)
        state = state.copy(
            combinationEffect = effect ?: state.combinationEffect,
            // Transitions pile up until Home takes them, so none is lost between frames.
            itemTransitions = if (transitions.isEmpty()) state.itemTransitions else state.itemTransitions + transitions,
            progress = progress,
            workspace = result.workspace,
            newlyUnlockedId = newlyUnlockedId ?: state.newlyUnlockedId,
            freshElementIds = newlyUnlockedId?.let { state.freshElementIds + it } ?: state.freshElementIds,
            achievementQueue = state.achievementQueue + newlyCompletedAchievements(state.progress, progress)
        )
    }

    /** Puts both ingredients of a known recipe on the workspace and shows it, as if each had been tapped in the palette. */
    fun placeRecipe(recipe: Recipe) {
        placeElements(listOf(recipe.firstId, recipe.secondId))
    }

    /** Asks for a hint, or for the next step of the one already shown. */
    fun requestHint() {
        playSound(Sound.CLICK)
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
        vibrate(Haptic.TICK)
    }

    fun onButtonClick() {
        playSound(Sound.CLICK)
    }

    /** The achievement banner came on screen: the reward chime and a tap. */
    fun onAchievementShown() {
        playSound(Sound.DISCOVER)
        vibrate(Haptic.CLICK)
    }

    fun dismissAchievement() {
        state = state.copy(achievementQueue = state.achievementQueue.drop(1))
    }

    fun markElementsSeen() {
        if (state.freshElementIds.isNotEmpty()) state = state.copy(freshElementIds = emptySet())
    }

    fun dismissNewElement() {
        state = state.copy(newlyUnlockedId = null)
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
        state = AlchemyUiState(progress = initialPlayerProgress(), selectedTab = AppTab.SETTINGS)
        resumeMusic()
    }

    fun setSoundEnabled(enabled: Boolean) {
        updateProgress { copy(soundEnabled = enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        updateProgress { copy(vibrationEnabled = enabled) }
        vibrate(Haptic.CLICK)
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

    private fun play(feedback: GameFeedback) {
        val (sound, haptic) = when (feedback) {
            GameFeedback.PLACE -> Sound.PLACE to Haptic.TICK
            GameFeedback.COMBINE -> Sound.COMBINE to Haptic.CLICK
            GameFeedback.DISCOVER -> Sound.DISCOVER to Haptic.DOUBLE
            GameFeedback.DISCOVER_GRAND -> Sound.DISCOVER to Haptic.HEAVY
            GameFeedback.NO_MATCH -> Sound.NO_MATCH to Haptic.TICK
            GameFeedback.REMOVE -> Sound.REMOVE to Haptic.TICK
            GameFeedback.CLEAR -> Sound.REMOVE to Haptic.CLICK
        }
        sound?.let(::playSound)
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
