package com.artt.alchemy.data

import android.content.Context
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.Combination
import com.artt.alchemy.game.recipeKey

data class PlayerProgress(
    val unlockedIds: Set<String>,
    val knownRecipeKeys: Set<String>,
    val successfulMixCount: Int,
    val mixAttemptCount: Int,
    val soundEnabled: Boolean,
    val vibrationEnabled: Boolean
)

fun initialPlayerProgress(): PlayerProgress = PlayerProgress(
    unlockedIds = AlchemyCatalog.baseElementIds,
    knownRecipeKeys = emptySet(),
    successfulMixCount = 0,
    mixAttemptCount = 0,
    soundEnabled = true,
    vibrationEnabled = true
)

fun PlayerProgress.recordAttempt(combination: Combination?): PlayerProgress = if (combination == null) {
    copy(mixAttemptCount = mixAttemptCount + 1)
} else {
    copy(
        unlockedIds = unlockedIds + combination.resultId,
        knownRecipeKeys = knownRecipeKeys + recipeKey(combination.firstId, combination.secondId),
        successfulMixCount = successfulMixCount + 1,
        mixAttemptCount = mixAttemptCount + 1
    )
}

fun PlayerProgress.reset(): PlayerProgress = initialPlayerProgress()

class ProgressStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): PlayerProgress = PlayerProgress(
        unlockedIds = preferences.getStringSet(KEY_UNLOCKED_IDS, null)?.toSet() ?: AlchemyCatalog.baseElementIds,
        knownRecipeKeys = preferences.getStringSet(KEY_KNOWN_RECIPES, null)?.toSet() ?: emptySet(),
        successfulMixCount = preferences.getInt(KEY_SUCCESSFUL_MIX_COUNT, 0),
        mixAttemptCount = preferences.getInt(KEY_MIX_ATTEMPT_COUNT, 0),
        soundEnabled = preferences.getBoolean(KEY_SOUND_ENABLED, true),
        vibrationEnabled = preferences.getBoolean(KEY_VIBRATION_ENABLED, true)
    )

    fun save(progress: PlayerProgress) {
        preferences.edit()
            .putStringSet(KEY_UNLOCKED_IDS, progress.unlockedIds)
            .putStringSet(KEY_KNOWN_RECIPES, progress.knownRecipeKeys)
            .putInt(KEY_SUCCESSFUL_MIX_COUNT, progress.successfulMixCount)
            .putInt(KEY_MIX_ATTEMPT_COUNT, progress.mixAttemptCount)
            .putBoolean(KEY_SOUND_ENABLED, progress.soundEnabled)
            .putBoolean(KEY_VIBRATION_ENABLED, progress.vibrationEnabled)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "alchemy_progress"
        const val KEY_UNLOCKED_IDS = "unlocked_ids"
        const val KEY_KNOWN_RECIPES = "known_recipe_keys"
        const val KEY_SUCCESSFUL_MIX_COUNT = "successful_mix_count"
        const val KEY_MIX_ATTEMPT_COUNT = "mix_attempt_count"
        const val KEY_SOUND_ENABLED = "sound_enabled"
        const val KEY_VIBRATION_ENABLED = "vibration_enabled"
    }
}
