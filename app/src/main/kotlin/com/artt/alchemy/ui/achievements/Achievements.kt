package com.artt.alchemy.ui.achievements

import com.artt.alchemy.R
import com.artt.alchemy.data.PlayerProgress
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.ElementGroup

data class AchievementDefinition(
    val id: String,
    val title: Int,
    val target: Int,
    val current: (PlayerProgress) -> Int
) {
    fun progressOf(progress: PlayerProgress): Int = current(progress).coerceAtMost(target)

    fun isCompleted(progress: PlayerProgress): Boolean = progressOf(progress) == target
}

// The base elements are given at the start, so only elements found by mixing count.
private val PlayerProgress.discoveredIds: Set<String>
    get() = unlockedIds - AlchemyCatalog.baseElementIds

val achievements = listOf(
    AchievementDefinition("first_unlocks", R.string.achievement_first_unlocks, 10) { it.discoveredIds.size },
    AchievementDefinition("mixes", R.string.achievement_mixes, 25) { it.successfulMixCount },
    AchievementDefinition("experiments", R.string.achievement_experiments, 50) { it.mixAttemptCount }
) + ElementGroup.entries.map { group ->
    AchievementDefinition(
        id = "group_${group.name.lowercase()}",
        title = group.achievementTitleRes,
        target = AlchemyCatalog.elements.count { it.group == group && it.id !in AlchemyCatalog.baseElementIds }
    ) { progress -> progress.discoveredIds.count { AlchemyCatalog.elementsById.getValue(it).group == group } }
}

val achievementsById: Map<String, AchievementDefinition> = achievements.associateBy(AchievementDefinition::id)

/** Ids of the achievements that [after] completes and [before] did not, in list order. */
fun newlyCompletedAchievements(before: PlayerProgress, after: PlayerProgress): List<String> = achievements.filter { it.isCompleted(after) && !it.isCompleted(before) }.map(AchievementDefinition::id)

private val ElementGroup.achievementTitleRes: Int
    get() = when (this) {
        ElementGroup.NATURE -> R.string.achievement_nature
        ElementGroup.MATERIAL -> R.string.achievement_material
        ElementGroup.LIFE -> R.string.achievement_life
        ElementGroup.CIVILIZATION -> R.string.achievement_civilization
        ElementGroup.COSMOS -> R.string.achievement_cosmos
    }
