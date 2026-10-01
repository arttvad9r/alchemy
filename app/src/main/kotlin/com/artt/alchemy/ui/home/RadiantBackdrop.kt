package com.artt.alchemy.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.artt.alchemy.ui.components.LocalReducedMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val TURN_MILLIS = 24_000
private const val GLOW_ALPHA = 0.55f
private const val RAYS_ALPHA = 0.8f
private const val INNER_RAYS_ALPHA = 0.45f
private const val INNER_RAYS_LENGTH = 0.75f
private const val INNER_RAYS_SPEED = -1.6f
private const val TWINKLE_SPEED = 9f
private const val TWINKLE_SIZE = 0.045f

// Where the twinkling stars sit around the centre: angle in turns and distance as a share of the radius.
private val Twinkles = listOf(0.05f to 0.72f, 0.19f to 0.9f, 0.33f to 0.62f, 0.47f to 0.85f, 0.61f to 0.7f, 0.76f to 0.92f, 0.9f to 0.6f)

/**
 * Slowly turning rays of [color] over a soft glow, with stars twinkling around them: the light behind a revealed element
 * or trophy. [intensity] from 0 to 1 is read while drawing, so the backdrop can fade in with the reveal. With reduced
 * motion the rays stand still.
 */
@Composable
fun RadiantBackdrop(color: Color, intensity: () -> Float, modifier: Modifier = Modifier, rays: Int = 12) {
    val reducedMotion = LocalReducedMotion.current
    val turn = rememberInfiniteTransition(label = "radiance").animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(TURN_MILLIS, easing = LinearEasing)),
        label = "radianceTurn"
    )
    val path = remember { Path() }
    Canvas(modifier) {
        val shown = intensity().coerceIn(0f, 1f)
        if (shown <= 0f) return@Canvas
        val angle = if (reducedMotion) 0f else turn.value
        val radius = size.minDimension / 2f
        drawGlow(center, radius, color, GLOW_ALPHA * shown)
        drawRays(center, radius, rays, angle, color, RAYS_ALPHA * shown, path)
        drawRays(center, radius * INNER_RAYS_LENGTH, rays, angle * INNER_RAYS_SPEED + PI.toFloat() / rays, color, INNER_RAYS_ALPHA * shown, path)
        Twinkles.forEachIndexed { index, (turns, distance) ->
            val at = 2f * PI.toFloat() * turns
            val twinkle = if (reducedMotion) 1f else (0.5f + 0.5f * sin(angle * TWINKLE_SPEED + index * 1.9f)).let { it * it }
            drawTwinkle(Offset(center.x + cos(at) * radius * distance, center.y + sin(at) * radius * distance), radius * TWINKLE_SIZE, color, twinkle * shown)
        }
    }
}

/** A four-pointed glint: two crossing strokes of white light over a small glow of [color]. */
private fun DrawScope.drawTwinkle(at: Offset, size: Float, color: Color, alpha: Float) {
    if (alpha <= 0f) return
    drawGlow(at, size * 2.5f, color, alpha * 0.7f)
    val white = Color.White.copy(alpha = alpha.coerceIn(0f, 1f))
    val stroke = size * 0.22f
    drawLine(white, Offset(at.x - size, at.y), Offset(at.x + size, at.y), strokeWidth = stroke, cap = StrokeCap.Round, blendMode = BlendMode.Plus)
    drawLine(white, Offset(at.x, at.y - size * 1.4f), Offset(at.x, at.y + size * 1.4f), strokeWidth = stroke, cap = StrokeCap.Round, blendMode = BlendMode.Plus)
}
