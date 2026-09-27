package com.artt.alchemy.ui.achievements

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.artt.alchemy.R
import com.artt.alchemy.data.PlayerProgress
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.ElementGroup

data class AchievementDefinition(
    val id: String,
    val title: Int,
    val target: Int,
    val current: (PlayerProgress) -> Int
)

private val achievements = listOf(
    AchievementDefinition("first_unlocks", R.string.achievement_first_unlocks, 10) { it.unlockedIds.size },
    AchievementDefinition("mixes", R.string.achievement_mixes, 25) { it.successfulMixCount },
    AchievementDefinition("experiments", R.string.achievement_experiments, 50) { it.mixAttemptCount }
) + ElementGroup.entries.map { group ->
    AchievementDefinition(
        id = "group_${group.name.lowercase()}",
        title = group.achievementTitleRes,
        target = AlchemyCatalog.elements.count { it.group == group }
    ) { progress -> progress.unlockedIds.count { AlchemyCatalog.elementsById.getValue(it).group == group } }
}

@Composable
fun AchievementsScreen(progress: PlayerProgress, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize().testTag("screen_achievements")) {
        items(achievements, key = AchievementDefinition::id) { achievement ->
            val current = achievement.current(progress).coerceAtMost(achievement.target)
            Card(modifier = Modifier.padding(12.dp).testTag("achievement_${achievement.id}")) {
                Text(
                    text = stringResource(achievement.title) + ": $current / ${achievement.target}",
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
                )
                if (current == achievement.target) {
                    Text(
                        text = stringResource(R.string.achievement_completed),
                        modifier = Modifier.padding(start = 16.dp, bottom = 16.dp, end = 16.dp)
                    )
                }
            }
        }
    }
}

private val ElementGroup.achievementTitleRes: Int
    get() = when (this) {
        ElementGroup.NATURE -> R.string.achievement_nature
        ElementGroup.MATERIAL -> R.string.achievement_material
        ElementGroup.LIFE -> R.string.achievement_life
        ElementGroup.CIVILIZATION -> R.string.achievement_civilization
        ElementGroup.COSMOS -> R.string.achievement_cosmos
    }
