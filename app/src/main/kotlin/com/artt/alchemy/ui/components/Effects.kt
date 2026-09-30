package com.artt.alchemy.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val PRESSED_SCALE = 0.93f

// The band of light is this share of the content's width, and crosses in this share of a shimmer's period.
private const val SHINE_BAND_SHARE = 0.28f
private const val SHIMMER_CROSSING_SHARE = 0.28f
private const val SHIMMER_PERIOD_MILLIS = 5600

// Light enough to read as a gleam over the art, never so bright that it hides it.
private val ShineColor = Color(0x5CFFFFFF)

// A cascade: items composed in a screen's first moments arrive one after another, rising a little as they fade in.
private const val CASCADE_WINDOW_MILLIS = 450L
private const val CASCADE_STEP_MILLIS = 35L
private const val CASCADE_MAX_STEPS = 12
private const val CASCADE_MILLIS = 320
private val CASCADE_RISE = 18.dp

/** Whether the screen on display is still opening; lists composed while it is arrive in a cascade. */
class ScreenOpening {
    var isOpening = true
        internal set
}

/** The screen on display; outside any screen nothing cascades. */
val LocalScreenOpening = staticCompositionLocalOf { ScreenOpening().apply { isOpening = false } }

/**
 * A screen stays opening from its first composition until a moment after its first frame is drawn, so a slow first
 * layout never eats the cascade, and items scrolled into view later simply appear.
 */
@Composable
fun rememberScreenOpening(): ScreenOpening {
    val opening = remember { ScreenOpening() }
    LaunchedEffect(opening) {
        withFrameMillis { }
        delay(CASCADE_WINDOW_MILLIS)
        opening.isOpening = false
    }
    return opening
}

/**
 * Clickable without the rectangular ripple: the content sinks under the finger and springs back when let go,
 * following its own drawn shape. [pressedScale] is how far it sinks; wide rows want less than tiles.
 */
@Composable
fun Modifier.pressClickable(role: Role? = null, pressedScale: Float = PRESSED_SCALE, onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = motion(spring(dampingRatio = if (pressed) Spring.DampingRatioNoBouncy else Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)),
        label = "press"
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.clickable(interactionSource = interactionSource, indication = null, role = role, onClick = onClick)
}

/**
 * A diagonal band of light across the content, only where the content itself is drawn. [position] runs from 0 (the band
 * just left of the content) to 1 (just past its right edge); outside that range nothing is drawn. It is read while
 * drawing, so an animated position redraws without recomposing.
 */
fun Modifier.shineSweep(position: () -> Float, color: Color = ShineColor, bandShare: Float = SHINE_BAND_SHARE): Modifier = graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}.drawWithContent {
    drawContent()
    val at = position()
    if (at <= 0f || at >= 1f) return@drawWithContent
    val band = size.width * bandShare
    val x = -band + (size.width + 2 * band) * at
    val brush = Brush.linearGradient(
        0f to Color.Transparent,
        0.5f to color,
        1f to Color.Transparent,
        start = Offset(x - band / 2, 0f),
        end = Offset(x + band / 2, size.height)
    )
    drawRect(brush, blendMode = BlendMode.SrcAtop)
}

/** A gleam that crosses the content every few seconds, as on polished gold; still when motion is reduced. */
@Composable
fun Modifier.shimmer(periodMillis: Int = SHIMMER_PERIOD_MILLIS, color: Color = ShineColor): Modifier {
    if (LocalReducedMotion.current) return this
    val cycle = rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMillis, easing = LinearEasing)),
        label = "shimmerCycle"
    )
    return shineSweep(position = { cycle.value / SHIMMER_CROSSING_SHARE }, color = color)
}

/** One gleam across the content, [delayMillis] after it first appears; wide content wants a narrower, softer [bandShare] and [color]. */
@Composable
fun Modifier.shineOnce(delayMillis: Long = 0L, durationMillis: Int = 700, color: Color = ShineColor, bandShare: Float = SHINE_BAND_SHARE): Modifier {
    if (LocalReducedMotion.current) return this
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMillis)
        sweep.animateTo(1f, tween(durationMillis, easing = FastOutSlowInEasing))
    }
    return shineSweep(position = { sweep.value }, color = color, bandShare = bandShare)
}

/** Lets the [index]th item of a list arrive in a cascade when it is composed while its screen is opening (see [LocalScreenOpening]). */
@Composable
fun Modifier.cascadeIn(index: Int): Modifier {
    val opening = LocalScreenOpening.current
    val reducedMotion = LocalReducedMotion.current
    val arrival = remember { Animatable(if (!opening.isOpening || reducedMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (arrival.value < 1f) {
            delay(index.coerceIn(0, CASCADE_MAX_STEPS) * CASCADE_STEP_MILLIS)
            arrival.animateTo(1f, tween(CASCADE_MILLIS, easing = FastOutSlowInEasing))
        }
    }
    return graphicsLayer {
        val shown = arrival.value
        alpha = shown
        translationY = (1f - shown) * CASCADE_RISE.toPx()
    }
}
