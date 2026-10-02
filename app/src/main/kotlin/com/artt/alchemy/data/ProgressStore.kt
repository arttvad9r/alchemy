package com.artt.alchemy.data

import android.content.Context
import androidx.core.content.edit
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.Combination
import com.artt.alchemy.game.ElementSort
import com.artt.alchemy.game.WorkspaceItem
import com.artt.alchemy.game.WorkspaceState
import com.artt.alchemy.game.nextHintRecipe
import com.artt.alchemy.game.recipeForKey
import com.artt.alchemy.game.recipeKey

enum class AppTheme {
    AETHER,
    EMBER,
    VERDANT
}

/** A hint in progress: [step] 1 shows the result and one ingredient, 2 the second ingredient. */
data class ActiveHint(
    val recipeKey: String,
    val step: Int
)

data class PlayerProgress(
    val unlockedIds: Set<String>,
    // Every unlocked id exactly once, oldest first.
    val discoveryOrder: List<String>,
    val knownRecipeKeys: Set<String>,
    val successfulMixCount: Int,
    val mixAttemptCount: Int,
    val soundEnabled: Boolean,
    val vibrationEnabled: Boolean,
    val musicEnabled: Boolean,
    val musicVolume: Float = 1f,
    val effectsVolume: Float = 1f,
    // Null follows the system animation scale.
    val reducedMotion: Boolean? = null,
    val theme: AppTheme = AppTheme.AETHER,
    val onboardingSeen: Boolean = false,
    val activeHint: ActiveHint? = null,
    val paletteSort: ElementSort = ElementSort.RECENT
)

/** Every element of the catalog is open. */
val PlayerProgress.isComplete: Boolean
    get() = unlockedIds.size == AlchemyCatalog.elements.size

fun initialPlayerProgress(): PlayerProgress = PlayerProgress(
    unlockedIds = AlchemyCatalog.baseElementIds,
    discoveryOrder = AlchemyCatalog.baseElementIds.toList(),
    knownRecipeKeys = emptySet(),
    successfulMixCount = 0,
    mixAttemptCount = 0,
    soundEnabled = true,
    vibrationEnabled = true,
    musicEnabled = true
)

/**
 * Drops ids and recipes the catalog does not know or the progress cannot have, clamps numbers and rebuilds
 * [PlayerProgress.discoveryOrder]: the base elements come first, then open elements the order does not list (saves from
 * before it was kept), in catalog order, then the ones it lists, as found.
 */
fun PlayerProgress.sanitized(): PlayerProgress {
    val unlocked = (unlockedIds.filter { it in AlchemyCatalog.elementsById } + AlchemyCatalog.baseElementIds).toSet()
    val listed = discoveryOrder.filter { it in unlocked && it !in AlchemyCatalog.baseElementIds }.distinct()
    val unlisted = AlchemyCatalog.elements.map { it.id }.filter { it in unlocked && it !in listed && it !in AlchemyCatalog.baseElementIds }
    val successful = successfulMixCount.coerceAtLeast(0)
    return copy(
        unlockedIds = unlocked,
        discoveryOrder = AlchemyCatalog.elements.map { it.id }.filter(AlchemyCatalog.baseElementIds::contains) + unlisted + listed,
        // A recipe is known only once it has been made, so its ingredients and result are all open.
        knownRecipeKeys = knownRecipeKeys.filter { key ->
            recipeForKey(key)?.let { it.firstId in unlocked && it.secondId in unlocked && it.resultId in unlocked } == true
        }.toSet(),
        successfulMixCount = successful,
        // Every successful mix was an attempt too.
        mixAttemptCount = mixAttemptCount.coerceAtLeast(successful),
        musicVolume = musicVolume.toVolume(),
        effectsVolume = effectsVolume.toVolume(),
        activeHint = activeHint?.takeIf { it.recipeKey in AlchemyCatalog.recipeResultsByKey && it.step in 1..2 }
    ).withoutSolvedHint()
}

private fun Float.toVolume(): Float = if (isFinite()) coerceIn(0f, 1f) else 1f

private fun PlayerProgress.withoutSolvedHint(): PlayerProgress {
    val hinted = activeHint?.let { recipeForKey(it.recipeKey) } ?: return this
    return if (hinted.resultId in unlockedIds) copy(activeHint = null) else this
}

/** Starts a hint, or moves the running one to its second step; the same hint stays until its element is found. */
fun PlayerProgress.requestHint(): PlayerProgress {
    val hint = activeHint
    val next = when {
        hint == null -> nextHintRecipe(unlockedIds)?.let { ActiveHint(recipeKey(it.firstId, it.secondId), step = 1) }
        hint.step == 1 -> hint.copy(step = 2)
        else -> hint
    }
    return copy(activeHint = next)
}

fun PlayerProgress.recordAttempt(combination: Combination?): PlayerProgress = if (combination == null) {
    copy(mixAttemptCount = mixAttemptCount + 1)
} else {
    copy(
        unlockedIds = unlockedIds + combination.resultId,
        discoveryOrder = if (combination.resultId in unlockedIds) discoveryOrder else discoveryOrder + combination.resultId,
        knownRecipeKeys = knownRecipeKeys + recipeKey(combination.firstId, combination.secondId),
        successfulMixCount = successfulMixCount + 1,
        mixAttemptCount = mixAttemptCount + 1
    ).withoutSolvedHint()
}

/** Back to the four base elements with the first-run tips due again; the player's settings stay as they are. */
fun PlayerProgress.reset(): PlayerProgress = withSettingsOf(this, initialPlayerProgress()).copy(onboardingSeen = false)

/** [progress] with the settings of [settings]: what the player chose for this device, not what they have found. */
fun withSettingsOf(settings: PlayerProgress, progress: PlayerProgress): PlayerProgress = progress.copy(
    soundEnabled = settings.soundEnabled,
    vibrationEnabled = settings.vibrationEnabled,
    musicEnabled = settings.musicEnabled,
    musicVolume = settings.musicVolume,
    effectsVolume = settings.effectsVolume,
    reducedMotion = settings.reducedMotion,
    theme = settings.theme,
    onboardingSeen = settings.onboardingSeen,
    paletteSort = settings.paletteSort
)

class ProgressStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): PlayerProgress = loadRaw().let { saved ->
        // Saves from before the first-run tips exist have no flag; a player who already mixed needs no tips.
        if (preferences.contains(KEY_ONBOARDING_SEEN)) saved else saved.copy(onboardingSeen = saved.mixAttemptCount > 0)
    }

    private fun loadRaw(): PlayerProgress = PlayerProgress(
        unlockedIds = preferences.getStringSet(KEY_UNLOCKED_IDS, null)?.toSet() ?: AlchemyCatalog.baseElementIds,
        discoveryOrder = preferences.getString(KEY_DISCOVERY_ORDER, null)?.split(SEPARATOR)?.filter(String::isNotEmpty).orEmpty(),
        knownRecipeKeys = preferences.getStringSet(KEY_KNOWN_RECIPES, null)?.toSet() ?: emptySet(),
        successfulMixCount = preferences.getInt(KEY_SUCCESSFUL_MIX_COUNT, 0),
        mixAttemptCount = preferences.getInt(KEY_MIX_ATTEMPT_COUNT, 0),
        soundEnabled = preferences.getBoolean(KEY_SOUND_ENABLED, true),
        vibrationEnabled = preferences.getBoolean(KEY_VIBRATION_ENABLED, true),
        musicEnabled = preferences.getBoolean(KEY_MUSIC_ENABLED, true),
        musicVolume = preferences.getFloat(KEY_MUSIC_VOLUME, 1f),
        effectsVolume = preferences.getFloat(KEY_EFFECTS_VOLUME, 1f),
        reducedMotion = if (preferences.contains(KEY_REDUCED_MOTION)) preferences.getBoolean(KEY_REDUCED_MOTION, false) else null,
        theme = AppTheme.entries.firstOrNull { it.name == preferences.getString(KEY_THEME, null) } ?: AppTheme.AETHER,
        onboardingSeen = preferences.getBoolean(KEY_ONBOARDING_SEEN, false),
        activeHint = preferences.getString(KEY_ACTIVE_HINT, null)?.let(::decodeHint),
        paletteSort = ElementSort.entries.firstOrNull { it.name == preferences.getString(KEY_PALETTE_SORT, null) } ?: ElementSort.RECENT
    ).sanitized()

    fun save(progress: PlayerProgress) {
        preferences.edit {
            putStringSet(KEY_UNLOCKED_IDS, progress.unlockedIds)
            putString(KEY_DISCOVERY_ORDER, progress.discoveryOrder.joinToString(SEPARATOR))
            putStringSet(KEY_KNOWN_RECIPES, progress.knownRecipeKeys)
            putInt(KEY_SUCCESSFUL_MIX_COUNT, progress.successfulMixCount)
            putInt(KEY_MIX_ATTEMPT_COUNT, progress.mixAttemptCount)
            putBoolean(KEY_SOUND_ENABLED, progress.soundEnabled)
            putBoolean(KEY_VIBRATION_ENABLED, progress.vibrationEnabled)
            putBoolean(KEY_MUSIC_ENABLED, progress.musicEnabled)
            putFloat(KEY_MUSIC_VOLUME, progress.musicVolume)
            putFloat(KEY_EFFECTS_VOLUME, progress.effectsVolume)
            progress.reducedMotion?.let { putBoolean(KEY_REDUCED_MOTION, it) } ?: remove(KEY_REDUCED_MOTION)
            progress.activeHint?.let { putString(KEY_ACTIVE_HINT, "${it.step}$SEPARATOR${it.recipeKey}") } ?: remove(KEY_ACTIVE_HINT)
            putString(KEY_THEME, progress.theme.name)
            putBoolean(KEY_ONBOARDING_SEEN, progress.onboardingSeen)
            putString(KEY_PALETTE_SORT, progress.paletteSort.name)
        }
    }

    /** The elements lying on the workspace, kept apart from the progress so they survive the app being closed. */
    fun saveWorkspace(workspace: WorkspaceState) {
        val items = workspace.items.joinToString(ITEM_SEPARATOR) { "${it.elementId}$SEPARATOR${it.xFraction}$SEPARATOR${it.yFraction}" }
        preferences.edit { putString(KEY_WORKSPACE, items) }
    }

    /** The first-run tip on screen, kept so the tips go on where they were left after the app is closed. */
    fun saveTipStep(step: Int) {
        preferences.edit { putInt(KEY_TIP_STEP, step) }
    }

    /** The saved tip, or null if none was kept (saves from before tips were kept). */
    fun loadTipStep(): Int? = if (preferences.contains(KEY_TIP_STEP)) preferences.getInt(KEY_TIP_STEP, 0).coerceAtLeast(0) else null

    /** The saved workspace, without elements that are unknown or not open in [unlockedIds] and with positions on the board. */
    fun loadWorkspace(unlockedIds: Set<String>): WorkspaceState {
        val items = preferences.getString(KEY_WORKSPACE, null).orEmpty().split(ITEM_SEPARATOR).mapNotNull { entry ->
            val parts = entry.split(SEPARATOR)
            val elementId = parts.getOrNull(0)?.takeIf { it in unlockedIds && it in AlchemyCatalog.elementsById } ?: return@mapNotNull null
            val x = parts.getOrNull(1)?.toFloatOrNull()?.takeIf(Float::isFinite) ?: return@mapNotNull null
            val y = parts.getOrNull(2)?.toFloatOrNull()?.takeIf(Float::isFinite) ?: return@mapNotNull null
            elementId to Pair(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
        }
        return WorkspaceState(
            items = items.mapIndexed { index, (elementId, at) -> WorkspaceItem(index + 1L, elementId, at.first, at.second) },
            nextInstanceId = items.size + 1L
        )
    }

    private fun decodeHint(value: String): ActiveHint? {
        val step = value.substringBefore(SEPARATOR).toIntOrNull() ?: return null
        return ActiveHint(recipeKey = value.substringAfter(SEPARATOR), step = step)
    }

    private companion object {
        const val SEPARATOR = ","
        const val ITEM_SEPARATOR = ";"
        const val KEY_WORKSPACE = "workspace"
        const val KEY_TIP_STEP = "tip_step"
        const val PREFERENCES_NAME = "alchemy_progress"
        const val KEY_UNLOCKED_IDS = "unlocked_ids"
        const val KEY_DISCOVERY_ORDER = "discovery_order"
        const val KEY_KNOWN_RECIPES = "known_recipe_keys"
        const val KEY_SUCCESSFUL_MIX_COUNT = "successful_mix_count"
        const val KEY_MIX_ATTEMPT_COUNT = "mix_attempt_count"
        const val KEY_SOUND_ENABLED = "sound_enabled"
        const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        const val KEY_MUSIC_ENABLED = "music_enabled"
        const val KEY_MUSIC_VOLUME = "music_volume"
        const val KEY_EFFECTS_VOLUME = "effects_volume"
        const val KEY_REDUCED_MOTION = "reduced_motion"
        const val KEY_THEME = "theme"
        const val KEY_ONBOARDING_SEEN = "onboarding_seen"
        const val KEY_ACTIVE_HINT = "active_hint"
        const val KEY_PALETTE_SORT = "palette_sort"
    }
}
