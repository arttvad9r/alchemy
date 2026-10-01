package com.artt.alchemy.ui.home

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.artt.alchemy.R
import com.artt.alchemy.game.BoardHalf
import com.artt.alchemy.game.WorkspaceItem
import com.artt.alchemy.ui.TIP_COUNT
import com.artt.alchemy.ui.components.AlchemyIconButton
import com.artt.alchemy.ui.components.LocalReducedMotion
import com.artt.alchemy.ui.components.motion
import com.artt.alchemy.ui.components.tooltipBackground
import com.artt.alchemy.ui.theme.Gold
import com.artt.alchemy.ui.tipHalf

private const val TIP_FADE_MILLIS = 300
private const val TIP_START_SCALE = 0.85f
private const val HINT_FADE_MILLIS = 250
private val TIP_MAX_WIDTH = 280.dp
private val TIP_ICON_SIZE = 30.dp

// The bubble art's tail hangs below its body.
private val TIP_TAIL_ROOM = 26.dp

@StringRes
fun tipText(step: Int): Int = when (step) {
    0 -> R.string.tip_tap
    1 -> R.string.tip_combine
    else -> R.string.tip_remove
}

/**
 * One first-run tip. It gives way by itself once the player has done what it asks, so it has nothing to press but the
 * cross that skips them all; touches elsewhere on it pass through to the workspace beneath.
 */
@Composable
fun FirstRunTip(step: Int, count: Int, onSkip: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .widthIn(max = TIP_MAX_WIDTH)
            .fillMaxWidth()
            .tooltipBackground()
            .padding(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 8.dp + TIP_TAIL_ROOM)
            .testTag("first_run_tip")
    ) {
        Text(
            text = stringResource(tipText(step)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            AlchemyIconButton(
                R.drawable.ic_close,
                stringResource(R.string.tips_skip),
                onSkip,
                Modifier.align(Alignment.CenterStart).testTag("tip_skip"),
                TIP_ICON_SIZE
            )
            Text(text = "${step + 1} / $count", style = MaterialTheme.typography.labelLarge, color = Gold)
        }
    }
}

/**
 * The empty-plate hint, or the first-run tips in its place while they are due. [items] are where the elements lie,
 * so a tip can keep out of the way of the ones the player is about to handle.
 */
@Composable
fun BoxScope.WorkspaceGuidance(items: List<WorkspaceItem>, tipsVisible: Boolean, tipStep: Int, onSkipTips: () -> Unit) {
    val isEmpty = items.isEmpty()
    val hintAlpha by animateFloatAsState(if (isEmpty && !tipsVisible) 1f else 0f, motion(tween(HINT_FADE_MILLIS)), label = "hintAlpha")
    if (hintAlpha > 0f) {
        Text(
            text = stringResource(R.string.workspace_hint),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp).alpha(hintAlpha)
        )
    }
    if (tipsVisible) {
        // The first tip points down at the palette; the others take the half of the plate with fewer elements in it,
        // which elements placed automatically then keep clear of.
        val alignment = when {
            tipStep == 0 -> Alignment.BottomCenter
            tipHalf(items) == BoardHalf.UPPER -> Alignment.TopCenter
            else -> Alignment.BottomCenter
        }
        AnimatedContent(
            targetState = tipStep,
            transitionSpec = tipTransition(LocalReducedMotion.current),
            contentAlignment = Alignment.Center,
            label = "tip",
            modifier = Modifier.align(alignment)
        ) { step ->
            FirstRunTip(step = step, count = TIP_COUNT, onSkip = onSkipTips)
        }
    }
}

// A tip done shrinks away and the next one pops up in its place.
private fun tipTransition(reducedMotion: Boolean): AnimatedContentTransitionScope<Int>.() -> ContentTransform = {
    if (reducedMotion) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        (fadeIn(tween(TIP_FADE_MILLIS, delayMillis = TIP_FADE_MILLIS / 2)) + scaleIn(tween(TIP_FADE_MILLIS, delayMillis = TIP_FADE_MILLIS / 2), initialScale = TIP_START_SCALE)) togetherWith
            (fadeOut(tween(TIP_FADE_MILLIS / 2)) + scaleOut(tween(TIP_FADE_MILLIS / 2), targetScale = TIP_START_SCALE))
    }
}
