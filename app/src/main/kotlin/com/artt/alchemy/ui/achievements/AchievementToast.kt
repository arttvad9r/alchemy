package com.artt.alchemy.ui.achievements

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.artt.alchemy.R
import com.artt.alchemy.ui.components.motion
import com.artt.alchemy.ui.theme.Gold
import com.artt.alchemy.ui.theme.PanelColor
import com.artt.alchemy.ui.theme.TitleFontFamily
import kotlinx.coroutines.delay

private const val SHOWN_MILLIS = 3000L
private const val LEAVE_MILLIS = 220
private const val SWIPE_UP_THRESHOLD = 12f
private const val OFFSCREEN_MARGIN = 100f

/**
 * The banner for an earned achievement. It slides in from the top, stays a few seconds, and leaves by itself,
 * on a tap, or on a swipe up; [onDismiss] runs once it is gone. Show one banner at a time with a fresh `key`.
 */
@Composable
fun AchievementToast(achievement: AchievementDefinition, onShown: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val shown = remember { Animatable(0f) }
    var leaving by remember { mutableStateOf(false) }
    val enterSpec = motion(spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
    val leaveSpec = motion(tween<Float>(LEAVE_MILLIS))
    LaunchedEffect(Unit) {
        onShown()
        shown.animateTo(1f, enterSpec)
        delay(SHOWN_MILLIS)
        leaving = true
    }
    LaunchedEffect(leaving) {
        if (leaving) {
            shown.animateTo(0f, leaveSpec)
            onDismiss()
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .graphicsLayer {
                translationY = (shown.value - 1f) * (size.height + OFFSCREEN_MARGIN)
                alpha = shown.value.coerceIn(0f, 1f)
            }
            .fillMaxWidth()
            .background(PanelColor.copy(alpha = 1f), RoundedCornerShape(16.dp))
            .border(2.dp, Gold, RoundedCornerShape(16.dp))
            .pointerInput(Unit) { detectTapGestures { leaving = true } }
            .pointerInput(Unit) { detectVerticalDragGestures { _, dragAmount -> if (dragAmount < -SWIPE_UP_THRESHOLD) leaving = true } }
            .padding(12.dp)
            .testTag("achievement_toast")
    ) {
        AchievementBadge(completed = true)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.achievement_unlocked), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(achievement.title), style = MaterialTheme.typography.titleMedium.copy(fontFamily = TitleFontFamily, fontWeight = FontWeight.Normal), color = Gold)
        }
    }
}
