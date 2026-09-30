package com.artt.alchemy.ui.home

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.artt.alchemy.ui.TIP_COUNT
import com.artt.alchemy.ui.components.AlchemyIconButton
import com.artt.alchemy.ui.components.motion
import com.artt.alchemy.ui.components.tooltipBackground
import com.artt.alchemy.ui.theme.Gold

private const val TIP_FADE_MILLIS = 250
private const val HINT_FADE_MILLIS = 250
private val TIP_MAX_WIDTH = 320.dp
private val TIP_ICON_SIZE = 36.dp

// The bubble art's tail hangs below its body.
private val TIP_TAIL_ROOM = 26.dp

@StringRes
fun tipText(step: Int): Int = when (step) {
    0 -> R.string.tip_tap
    1 -> R.string.tip_combine
    else -> R.string.tip_remove
}

/** One first-run tip: tap anywhere on it for the next, or skip them all with the cross. */
@Composable
fun FirstRunTip(
    step: Int,
    count: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(1f, motion(tween(TIP_FADE_MILLIS)), label = "tipAlpha")
    val isLast = step >= count - 1
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .alpha(alpha)
            .widthIn(max = TIP_MAX_WIDTH)
            .fillMaxWidth()
            .tooltipBackground()
            .clickable(onClick = onNext)
            .padding(start = 24.dp, top = 14.dp, end = 24.dp, bottom = 14.dp + TIP_TAIL_ROOM)
            .testTag("first_run_tip")
    ) {
        Text(
            text = stringResource(tipText(step)),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            AlchemyIconButton(R.drawable.ic_close, stringResource(R.string.tips_skip), onSkip, Modifier.testTag("tip_skip"), TIP_ICON_SIZE)
            Text(text = "${step + 1} / $count", style = MaterialTheme.typography.labelLarge, color = Gold)
            AlchemyIconButton(
                icon = if (isLast) R.drawable.ic_check else R.drawable.ic_forward,
                contentDescription = stringResource(if (isLast) R.string.ok else R.string.tip_next),
                onClick = onNext,
                modifier = Modifier.testTag("tip_next"),
                size = TIP_ICON_SIZE
            )
        }
    }
}

/** The empty-plate hint, or the first-run tips in its place while they are due. */
@Composable
fun BoxScope.WorkspaceGuidance(isEmpty: Boolean, tipsVisible: Boolean, tipStep: Int, onNextTip: () -> Unit, onSkipTips: () -> Unit) {
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
        // The first tip points down at the palette; the others sit over the middle of the plate.
        FirstRunTip(
            step = tipStep,
            count = TIP_COUNT,
            onNext = onNextTip,
            onSkip = onSkipTips,
            modifier = Modifier.align(if (tipStep == 0) Alignment.BottomCenter else Alignment.Center)
        )
    }
}
