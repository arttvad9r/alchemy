package com.artt.alchemy.ui.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.lerp
import com.artt.alchemy.game.ElementRarity
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private const val TAU = 2f * PI.toFloat()

// Sparks: how quickly they slow down, how many can live at once, how thick the drag trail is.
private const val SPARK_DRAG = 2.6f
private const val MAX_SPARKS = 260
private const val SPARK_GLOW_SHARE = 3.2f
private const val SPARK_GLOW_ALPHA = 0.28f
private const val TRAIL_SPACING = 0.32f
private const val MAX_TRAIL_PER_FRAME = 2
private const val TRAIL_LIFE = 0.3f

// The ambience rises out of stillness when the workspace comes into view.
private const val MOTES_FADE_IN_SECONDS = 1.5f

// Ambient motes drifting up through the workspace.
private const val MOTE_COUNT = 26
private const val MOTE_TRAVEL = 1.3f
private const val MOTE_EDGE = 0.15f
private const val MOTE_GLOW_SHARE = 5f
private const val MOTE_GLOW_ALPHA = 0.55f
private const val MOTE_CORE_ALPHA = 0.8f
private const val MOTE_CORE_SHARE = 0.55f

// Rays are this share of the gap between them.
private const val RAY_WIDTH_SHARE = 0.34f

private val SparkWhite = Color(0xFFFFFFFF)
private val MoteGold = Color(0xFFFFD98A)
private val MoteViolet = Color(0xFFC9A4FF)

/** The glowing colour of a rarity in effects: brighter than its badge, so it reads as light on the dark workspace. */
internal val ElementRarity.glowColor: Color
    get() = when (this) {
        ElementRarity.BASE -> Color(0xFFBFD0FF)
        ElementRarity.COMMON -> Color(0xFF8FB8FF)
        ElementRarity.RARE -> Color(0xFF7DF5A0)
        ElementRarity.EPIC -> Color(0xFFC79BFF)
        ElementRarity.LEGENDARY -> Color(0xFFFFD66B)
    }

/** The sparks a mix or discovery of this rarity throws: its own colour, lit with white and a second tint. */
internal val ElementRarity.sparkColors: List<Color>
    get() = when (this) {
        ElementRarity.BASE, ElementRarity.COMMON -> listOf(glowColor, SparkWhite, Color(0xFF7FE3FF))
        ElementRarity.RARE -> listOf(glowColor, SparkWhite, Color(0xFFFFE08A))
        ElementRarity.EPIC -> listOf(glowColor, SparkWhite, Color(0xFFFF9BF0))
        ElementRarity.LEGENDARY -> listOf(glowColor, SparkWhite, Color(0xFFFFB347), Color(0xFFFFF3C4))
    }

private class Spark(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val life: Float,
    val size: Float,
    val color: Color,
    val gravity: Float
) {
    var age = 0f
}

/**
 * Sparks thrown by mixes, refusals and a dragged element, in canvas pixels. They slow down, fall or rise, and fade over
 * their life. [step] advances them with the frame clock; [draw] paints them with additive light.
 */
class Sparks {
    private val live = ArrayList<Spark>()
    private val random = Random(SEED)
    private var trailFrom: Offset? = null

    val isEmpty: Boolean get() = live.isEmpty()

    fun step(seconds: Float) {
        val slow = (1f - SPARK_DRAG * seconds).coerceIn(0f, 1f)
        val iterator = live.iterator()
        while (iterator.hasNext()) {
            val spark = iterator.next()
            spark.age += seconds
            if (spark.age >= spark.life) {
                iterator.remove()
            } else {
                spark.vx *= slow
                spark.vy = spark.vy * slow + spark.gravity * seconds
                spark.x += spark.vx * seconds
                spark.y += spark.vy * seconds
            }
        }
    }

    /**
     * A ring of sparks thrown out from [center]. Distances are in [unit]s, the item radius, so a burst looks the same
     * on every screen; [gravity] pulls them down (or up, when negative) in units per second squared.
     */
    fun burst(center: Offset, unit: Float, count: Int, speed: Float, colors: List<Color>, life: Float, gravity: Float = 0f) {
        repeat(count) {
            val angle = random.nextFloat() * TAU
            val velocity = unit * speed * (0.35f + 0.65f * random.nextFloat())
            add(
                Spark(
                    x = center.x,
                    y = center.y,
                    vx = cos(angle) * velocity,
                    vy = sin(angle) * velocity,
                    life = life * (0.55f + 0.45f * random.nextFloat()),
                    size = unit * (0.025f + 0.035f * random.nextFloat()),
                    color = colors[random.nextInt(colors.size)],
                    gravity = gravity * unit
                )
            )
        }
    }

    /** Leaves a thin trail of rising motes behind an element in hand; call it every frame with its position, or null when nothing is held. */
    fun trail(position: Offset?, unit: Float, color: Color) {
        val from = trailFrom
        trailFrom = position
        if (position == null || from == null) return
        val count = ((position - from).getDistance() / (unit * TRAIL_SPACING)).toInt().coerceAtMost(MAX_TRAIL_PER_FRAME)
        repeat(count) { index ->
            val along = (index + random.nextFloat()) / count
            add(
                Spark(
                    x = lerp(from.x, position.x, along) + (random.nextFloat() - 0.5f) * unit * 0.3f,
                    y = lerp(from.y, position.y, along) + (random.nextFloat() - 0.5f) * unit * 0.3f,
                    vx = (random.nextFloat() - 0.5f) * unit * 0.3f,
                    vy = -unit * (0.2f + 0.3f * random.nextFloat()),
                    life = TRAIL_LIFE * (0.6f + 0.4f * random.nextFloat()),
                    size = unit * (0.012f + 0.014f * random.nextFloat()),
                    color = if (random.nextInt(3) == 0) SparkWhite else color,
                    gravity = 0f
                )
            )
        }
    }

    fun clear() {
        live.clear()
        trailFrom = null
    }

    fun draw(scope: DrawScope) {
        live.forEach { spark ->
            val left = 1f - spark.age / spark.life
            val fade = left * (2f - left)
            val center = Offset(spark.x, spark.y)
            scope.drawCircle(spark.color.copy(alpha = SPARK_GLOW_ALPHA * fade), spark.size * SPARK_GLOW_SHARE, center, blendMode = BlendMode.Plus)
            scope.drawCircle(spark.color.copy(alpha = fade), spark.size * (0.5f + 0.5f * left), center, blendMode = BlendMode.Plus)
        }
    }

    private fun add(spark: Spark) {
        if (live.size < MAX_SPARKS) live += spark
    }

    private companion object {
        const val SEED = 7
    }
}

private class Mote(val x: Float, val start: Float, val speed: Float, val sway: Float, val phase: Float, val size: Float, val color: Color)

/** Specks of light drifting slowly up through the workspace and twinkling, always the same for the same time; [tint] is the theme's own speck. */
class Motes(tint: Color) {
    private val colors = listOf(MoteGold, MoteGold, tint, tint, MoteViolet)
    private val motes = Random(SEED).let { random ->
        List(MOTE_COUNT) {
            Mote(
                x = random.nextFloat(),
                start = random.nextFloat() * MOTE_TRAVEL,
                speed = 0.012f + 0.022f * random.nextFloat(),
                sway = 0.01f + 0.03f * random.nextFloat(),
                phase = random.nextFloat() * TAU,
                size = 0.02f + 0.03f * random.nextFloat(),
                color = colors[random.nextInt(colors.size)]
            )
        }
    }

    /** Draws the motes at [seconds] into their drift; [unit] is the item radius. */
    fun draw(scope: DrawScope, seconds: Float, unit: Float) {
        val width = scope.size.width
        val height = scope.size.height
        val arrival = (seconds / MOTES_FADE_IN_SECONDS).coerceIn(0f, 1f)
        motes.forEach { mote ->
            // From just below the bottom edge up past the top, then round again.
            val travelled = (mote.start + seconds * mote.speed) % MOTE_TRAVEL
            val y = 1f + MOTE_EDGE - travelled
            val x = mote.x + mote.sway * sin(seconds * 0.6f + mote.phase)
            val edgeFade = minOf(1f, (y + MOTE_EDGE) / (2 * MOTE_EDGE), (1f + MOTE_EDGE - y) / (2 * MOTE_EDGE)).coerceIn(0f, 1f)
            val twinkle = 0.55f + 0.45f * sin(seconds * (1.1f + mote.speed * 20f) + mote.phase * 3f)
            val alpha = edgeFade * twinkle * arrival
            if (alpha <= 0f) return@forEach
            val center = Offset(x * width, y * height)
            val radius = unit * mote.size
            scope.drawGlow(center, radius * MOTE_GLOW_SHARE, mote.color, MOTE_GLOW_ALPHA * alpha)
            scope.drawCircle(mote.color.copy(alpha = MOTE_CORE_ALPHA * alpha), radius * MOTE_CORE_SHARE, center, blendMode = BlendMode.Plus)
        }
    }

    private companion object {
        const val SEED = 42
    }
}

/**
 * A fan of [count] light rays around [center], turned by [turn] radians and fading out towards [length].
 * [path] is reused between frames so drawing allocates no geometry.
 */
internal fun DrawScope.drawRays(center: Offset, length: Float, count: Int, turn: Float, color: Color, alpha: Float, path: Path) {
    if (alpha <= 0f || length <= 0f) return
    val half = TAU / count * RAY_WIDTH_SHARE / 2f
    path.reset()
    repeat(count) { index ->
        val angle = turn + index * TAU / count
        path.moveTo(center.x, center.y)
        path.lineTo(center.x + cos(angle - half) * length, center.y + sin(angle - half) * length)
        path.lineTo(center.x + cos(angle + half) * length, center.y + sin(angle + half) * length)
        path.close()
    }
    val brush = Brush.radialGradient(
        0f to color.copy(alpha = alpha.coerceIn(0f, 1f)),
        0.35f to color.copy(alpha = alpha.coerceIn(0f, 1f) * 0.45f),
        1f to color.copy(alpha = 0f),
        center = center,
        radius = length
    )
    drawPath(path, brush, blendMode = BlendMode.Plus)
}

/** A soft round glow of [color] around [center], brightest in the middle. */
internal fun DrawScope.drawGlow(center: Offset, radius: Float, color: Color, alpha: Float) {
    if (alpha <= 0f || radius <= 0f) return
    // One white glow drawn once and tinted per use: a gradient shader per glow per frame was the costliest thing here.
    val side = (radius * 2).roundToInt()
    drawImage(
        image = GlowSprite,
        dstOffset = IntOffset((center.x - radius).roundToInt(), (center.y - radius).roundToInt()),
        dstSize = IntSize(side, side),
        alpha = alpha.coerceIn(0f, 1f),
        colorFilter = ColorFilter.tint(color),
        blendMode = BlendMode.Plus
    )
}

private const val GLOW_SPRITE_SIZE = 128
private const val GLOW_MIDDLE_ALPHA = 0.3f

// Brightest in the middle, a third of that halfway out, nothing at the edge.
private val GlowSprite: ImageBitmap by lazy {
    val bitmap = ImageBitmap(GLOW_SPRITE_SIZE, GLOW_SPRITE_SIZE)
    val middle = Offset(GLOW_SPRITE_SIZE / 2f, GLOW_SPRITE_SIZE / 2f)
    val paint = Paint().apply {
        shader = RadialGradientShader(
            center = middle,
            radius = GLOW_SPRITE_SIZE / 2f,
            colors = listOf(Color.White, Color.White.copy(alpha = GLOW_MIDDLE_ALPHA), Color.Transparent),
            colorStops = listOf(0f, 0.5f, 1f)
        )
    }
    Canvas(bitmap).drawCircle(middle, GLOW_SPRITE_SIZE / 2f, paint)
    bitmap
}
