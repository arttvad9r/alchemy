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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.artt.alchemy.R
import com.artt.alchemy.data.PlayerProgress
import com.artt.alchemy.data.isComplete
import com.artt.alchemy.ui.components.AlchemyProgressBar
import com.artt.alchemy.ui.components.ScreenBanner
import com.artt.alchemy.ui.components.ScreenPadding
import com.artt.alchemy.ui.components.cascadeIn
import com.artt.alchemy.ui.components.pressClickable
import com.artt.alchemy.ui.components.rowPanel
import com.artt.alchemy.ui.theme.Gold
import com.artt.alchemy.ui.theme.themedArt

@Composable
fun AchievementsScreen(progress: PlayerProgress, onOpenCompletion: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(ScreenPadding),
        modifier = modifier.fillMaxSize().testTag("screen_achievements")
    ) {
        item { ScreenBanner(stringResource(R.string.tab_achievements)) }
        if (progress.isComplete) item { CompletionCard(onOpenCompletion) }
        itemsIndexed(achievements, key = { _, achievement -> achievement.id }) { index, achievement ->
            val current = achievement.progressOf(progress)
            val completed = achievement.isCompleted(progress)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .cascadeIn(index)
                    .fillMaxWidth()
                    .rowPanel()
                    .padding(horizontal = 16.dp, vertical = 9.dp)
                    .testTag("achievement_${achievement.id}")
            ) {
                AchievementBadge(achievement.iconRes, completed)
                val title = stringResource(achievement.title)
                val count = "$current / ${achievement.target}"
                val status = if (completed) stringResource(R.string.achievement_completed) else count
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clearAndSetSemantics { contentDescription = "$title: $status" }
                ) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AlchemyProgressBar(progress = current.toFloat() / achievement.target, modifier = Modifier.weight(1f))
                        Text(
                            text = count,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (completed) Gold else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletionCard(onClick: () -> Unit) {
    val description = stringResource(R.string.completion_card)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            // Before the panel, so the whole card sinks under the finger, art and all.
            .pressClickable(role = Role.Button, pressedScale = ROW_PRESSED_SCALE, onClick = onClick)
            .rowPanel()
            .padding(horizontal = 16.dp, vertical = 9.dp)
            .testTag("achievement_completion")
            .clearAndSetSemantics { contentDescription = description }
    ) {
        AchievementBadge(R.drawable.nav_achievements, completed = true)
        Text(
            text = stringResource(R.string.completion_title),
            style = MaterialTheme.typography.titleMedium,
            color = Gold,
            maxLines = 2,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Each achievement keeps its own symbol; completion upgrades the neutral frame to the gold wreath. */
@Composable
fun AchievementBadge(iconRes: Int, completed: Boolean, modifier: Modifier = Modifier) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(BADGE_SIZE)) {
        Image(
            painter = painterResource(if (completed) R.drawable.achievement_wreath else themedArt(R.drawable.frame_base)),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(if (completed) 1f else 0.78f)
        )
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(BADGE_SIZE * if (completed) 0.5f else 0.56f).alpha(if (completed) 1f else 0.52f)
        )
    }
}

private val BADGE_SIZE = 44.dp
private const val ROW_PRESSED_SCALE = 0.97f
