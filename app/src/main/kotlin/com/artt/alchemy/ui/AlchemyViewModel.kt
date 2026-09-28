package com.artt.alchemy.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.artt.alchemy.data.PlayerProgress
import com.artt.alchemy.data.ProgressStore
import com.artt.alchemy.data.initialPlayerProgress
import com.artt.alchemy.data.recordAttempt
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.AlchemyEngine
import com.artt.alchemy.game.WorkspaceEvent
import com.artt.alchemy.game.WorkspaceState
import com.artt.alchemy.game.reduce

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
    val isDiscovery: Boolean
)

data class AlchemyUiState(
    val progress: PlayerProgress,
    val workspace: WorkspaceState = WorkspaceState(),
    val selectedTab: AppTab = AppTab.HOME,
    val newlyUnlockedId: String? = null,
    val feedbackEventId: Long = 0,
    val combinationEffect: CombinationEffect? = null,
    val isResetConfirmationVisible: Boolean = false
)

class AlchemyViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = AlchemyEngine(AlchemyCatalog)
    private val store = ProgressStore(application)

    var state by mutableStateOf(AlchemyUiState(progress = store.load()))
        private set

    private var combinationEffectCount = 0L

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
                isDiscovery = newlyUnlockedId != null
            )
        }

        if (progress != state.progress) store.save(progress)
        state = state.copy(
            combinationEffect = effect ?: state.combinationEffect,
            progress = progress,
            workspace = result.workspace,
            newlyUnlockedId = newlyUnlockedId ?: state.newlyUnlockedId,
            feedbackEventId = if (result.combination != null) state.feedbackEventId + 1 else state.feedbackEventId
        )
    }

    fun consumeCombinationEffect() {
        state = state.copy(combinationEffect = null)
    }

    fun selectTab(tab: AppTab) {
        state = state.copy(selectedTab = tab)
    }

    fun dismissNewElement() {
        state = state.copy(newlyUnlockedId = null)
    }

    fun consumeCombinationFeedback() {
        state = state.copy(feedbackEventId = 0)
    }

    fun requestReset() {
        state = state.copy(isResetConfirmationVisible = true)
    }

    fun dismissReset() {
        state = state.copy(isResetConfirmationVisible = false)
    }

    fun confirmReset() {
        store.clear()
        state = AlchemyUiState(progress = initialPlayerProgress(), selectedTab = AppTab.SETTINGS)
    }

    fun setSoundEnabled(enabled: Boolean) {
        updateProgress { copy(soundEnabled = enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        updateProgress { copy(vibrationEnabled = enabled) }
    }

    private fun updateProgress(transform: PlayerProgress.() -> PlayerProgress) {
        val progress = state.progress.transform()
        store.save(progress)
        state = state.copy(progress = progress)
    }
}
