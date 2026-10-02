package com.artt.alchemy.ui.achievements

import androidx.annotation.DrawableRes
import com.artt.alchemy.R
import com.artt.alchemy.data.PlayerProgress
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.ElementGroup
import com.artt.alchemy.game.ElementLinks
import com.artt.alchemy.game.ElementRarity

data class AchievementDefinition(
    val id: String,
    val title: Int,
    @param:DrawableRes val iconRes: Int,
    val target: Int,
    val current: (PlayerProgress) -> Int
) {
    fun progressOf(progress: PlayerProgress): Int = current(progress).coerceAtMost(target)

    fun isCompleted(progress: PlayerProgress): Boolean = progressOf(progress) == target
}

// The base elements are given at the start, so only elements found by mixing count.
private val PlayerProgress.discoveredIds: Set<String>
    get() = unlockedIds - AlchemyCatalog.baseElementIds

private fun PlayerProgress.discoveredWith(rarity: ElementRarity): Int = discoveredIds.count { AlchemyCatalog.rarityById.getValue(it) == rarity }

private val discoverableCount = AlchemyCatalog.elements.size - AlchemyCatalog.baseElementIds.size

val achievements = listOf(
    AchievementDefinition("first_unlocks", R.string.achievement_first_unlocks, R.drawable.element_book, 10) { it.discoveredIds.size },
    AchievementDefinition("mixes", R.string.achievement_mixes, R.drawable.element_hammer, 25) { it.successfulMixCount },
    AchievementDefinition("experiments", R.string.achievement_experiments, R.drawable.element_potion, 50) { it.mixAttemptCount },
    AchievementDefinition("discovered_50", R.string.achievement_discovered_50, R.drawable.element_star, 50) { it.discoveredIds.size },
    AchievementDefinition("discovered_100", R.string.achievement_discovered_100, R.drawable.element_sun, 100) { it.discoveredIds.size },
    AchievementDefinition("first_epic", R.string.achievement_first_epic, R.drawable.element_crystal, 1) { it.discoveredWith(ElementRarity.EPIC) },
    AchievementDefinition("first_legendary", R.string.achievement_first_legendary, R.drawable.element_crown, 1) { it.discoveredWith(ElementRarity.LEGENDARY) },
    AchievementDefinition("all_final", R.string.achievement_all_final, R.drawable.element_philosopher_stone, ElementLinks.finalElementIds.size) { progress ->
        progress.discoveredIds.count { it in ElementLinks.finalElementIds }
    },
    AchievementDefinition("discovered_all", R.string.achievement_discovered_all, R.drawable.element_alchemy, discoverableCount) { it.discoveredIds.size }
) + ElementGroup.entries.map { group ->
    AchievementDefinition(
        id = "group_${group.name.lowercase()}",
        title = group.achievementTitleRes,
        iconRes = group.achievementIconRes,
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

private val ElementGroup.achievementIconRes: Int
    get() = when (this) {
        ElementGroup.NATURE -> R.drawable.element_tree
        ElementGroup.MATERIAL -> R.drawable.element_metal
        ElementGroup.LIFE -> R.drawable.element_human
        ElementGroup.CIVILIZATION -> R.drawable.element_city
        ElementGroup.COSMOS -> R.drawable.element_star
    }
