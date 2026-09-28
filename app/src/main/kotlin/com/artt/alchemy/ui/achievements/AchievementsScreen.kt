package com.artt.alchemy.ui.achievements

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.artt.alchemy.R
import com.artt.alchemy.data.PlayerProgress
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.ElementGroup
import com.artt.alchemy.ui.theme.Gold
import com.artt.alchemy.ui.theme.PanelBorderColor
import com.artt.alchemy.ui.theme.PanelColor

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
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(12.dp),
        modifier = modifier.fillMaxSize().testTag("screen_achievements")
    ) {
        items(achievements, key = AchievementDefinition::id) { achievement ->
            val current = achievement.current(progress).coerceAtMost(achievement.target)
            val completed = current == achievement.target
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PanelColor, RoundedCornerShape(16.dp))
                    .border(1.dp, if (completed) Gold else PanelBorderColor, RoundedCornerShape(16.dp))
                    .padding(14.dp)
                    .testTag("achievement_${achievement.id}")
            ) {
                Image(
                    painter = painterResource(R.drawable.nav_achievements),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).alpha(if (completed) 1f else 0.45f)
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(achievement.title) + ": $current / ${achievement.target}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    LinearProgressIndicator(
                        progress = { current.toFloat() / achievement.target },
                        color = if (completed) Gold else MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (completed) {
                        Text(text = stringResource(R.string.achievement_completed), color = Gold)
                    }
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
