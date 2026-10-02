package com.artt.alchemy.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.artt.alchemy.R
import com.artt.alchemy.data.PlayerProgress
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.ElementRarity
import com.artt.alchemy.ui.achievements.AchievementBadge
import com.artt.alchemy.ui.components.AlchemyButton
import com.artt.alchemy.ui.components.AlchemyDialog
import com.artt.alchemy.ui.components.ButtonStyle
import com.artt.alchemy.ui.components.motion
import com.artt.alchemy.ui.components.shimmer
import com.artt.alchemy.ui.theme.Gold

private const val BURST_SIZE = 240
private const val TROPHY_SIZE = 110
private const val BURST_START_SCALE = 0.4f
private const val COMPLETION_RAYS = 20

/** The card for a finished collection: a trophy over a golden burst, and how many tries and mixes it took. */
@Composable
fun CompletionDialog(progress: PlayerProgress, onDismiss: () -> Unit, onClick: () -> Unit) {
    val entrance = remember { Animatable(0f) }
    val entranceSpec = motion(spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, entranceSpec)
    }
    AlchemyDialog(onDismissRequest = onDismiss, panelRes = R.drawable.dialog_gold) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).testTag("completion_dialog")) {
            Text(
                stringResource(R.string.completion_title),
                style = MaterialTheme.typography.headlineSmall,
                color = Gold,
                textAlign = TextAlign.Center,
                modifier = Modifier.shimmer()
            )
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()) {
                RadiantBackdrop(
                    color = ElementRarity.LEGENDARY.glowColor,
                    intensity = { entrance.value },
                    rays = COMPLETION_RAYS,
                    modifier = Modifier.matchParentSize()
                )
                Image(
                    painter = painterResource(R.drawable.fx_discovery_burst_gold),
                    contentDescription = null,
                    modifier = Modifier
                        .size(BURST_SIZE.dp)
                        .scale(BURST_START_SCALE + (1f - BURST_START_SCALE) * entrance.value)
                        .graphicsLayer { alpha = entrance.value.coerceIn(0f, 1f) }
                )
                AchievementBadge(R.drawable.nav_achievements, completed = true, modifier = Modifier.size(TROPHY_SIZE.dp).scale(entrance.value))
            }
            Text(stringResource(R.string.completion_message, AlchemyCatalog.elements.size), textAlign = TextAlign.Center)
            Text(
                stringResource(R.string.completion_attempts, progress.mixAttemptCount),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                stringResource(R.string.completion_mixes, progress.successfulMixCount),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
        }
        AlchemyButton(stringResource(R.string.ok), ButtonStyle.GOLD, onClick = {
            onClick()
            onDismiss()
        })
    }
}
