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
)

@Composable
fun AchievementsScreen(progress: PlayerProgress, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize().testTag("screen_achievements")) {
        items(achievements, key = AchievementDefinition::id) { achievement ->
            Card(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(achievement.title) + ": ${achievement.current(progress)} / ${achievement.target}",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
