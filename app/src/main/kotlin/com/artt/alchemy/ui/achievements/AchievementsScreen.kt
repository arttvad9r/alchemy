package com.artt.alchemy.ui.achievements

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.artt.alchemy.R
import com.artt.alchemy.data.PlayerProgress
import com.artt.alchemy.ui.components.AlchemyProgressBar
import com.artt.alchemy.ui.components.ScreenBanner
import com.artt.alchemy.ui.components.rowPanel
import com.artt.alchemy.ui.theme.Gold

@Composable
fun AchievementsScreen(progress: PlayerProgress, modifier: Modifier = Modifier) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(12.dp),
        modifier = modifier.fillMaxSize().testTag("screen_achievements")
    ) {
        item { ScreenBanner(stringResource(R.string.tab_achievements)) }
        items(achievements, key = AchievementDefinition::id) { achievement ->
            val current = achievement.progressOf(progress)
            val completed = achievement.isCompleted(progress)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .rowPanel()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .testTag("achievement_${achievement.id}")
            ) {
                AchievementBadge(completed)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(achievement.title) + ": $current / ${achievement.target}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    AlchemyProgressBar(
                        progress = current.toFloat() / achievement.target,
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

/** A trophy in the gold wreath once earned, a chained lock until then. */
@Composable
fun AchievementBadge(completed: Boolean, modifier: Modifier = Modifier) {
    if (completed) {
        Box(contentAlignment = Alignment.Center, modifier = modifier.size(BADGE_SIZE)) {
            Image(painter = painterResource(R.drawable.achievement_wreath), contentDescription = null, modifier = Modifier.fillMaxSize())
            Image(painter = painterResource(R.drawable.nav_achievements), contentDescription = null, modifier = Modifier.size(BADGE_SIZE * 0.5f))
        }
    } else {
        Image(painter = painterResource(R.drawable.achievement_locked), contentDescription = null, modifier = modifier.size(BADGE_SIZE))
    }
}

private val BADGE_SIZE = 56.dp
