package com.artt.alchemy.ui.home

import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.lerp
import androidx.core.content.res.ResourcesCompat
import com.artt.alchemy.R
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.ElementRarity
import com.artt.alchemy.game.WorkspaceItem
import com.artt.alchemy.game.overlapTarget
import com.artt.alchemy.ui.CombinationEffect
import com.artt.alchemy.ui.TransitionFrame
import com.artt.alchemy.ui.TransitionKind
import com.artt.alchemy.ui.components.elementIconRes
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private const val ITEM_RADIUS_FRACTION = 0.11f
private const val LABEL_SIZE_FRACTION = 0.042f
private const val ICON_SHARE = 0.66f
private const val ICON_TOP_SHARE = 0.85f
private const val LABEL_SHADOW_RADIUS = 6f
private const val WATERMARK_SIZE_FRACTION = 0.85f
private const val WATERMARK_ALPHA = 0.28f
private const val SMOKE_ALPHA = 0.9f
private const val HELD_RING_SHARE = 1.45f
private const val TARGET_PULSE_MILLIS = 550
private const val LIFT_SCALE = 0.12f
private const val LIFT_SHADOW_ALPHA = 0.35f
private const val TARGET_RING_SHARE = 1.7f
private const val APPEAR_START_SCALE = 0.5f
private const val FLY_IN_SPEED = 1.5f
private const val SHAKE_WAVES = 2.5f
private const val SHAKE_AMPLITUDE_SHARE = 0.1f
private const val AURA_ALPHA = 0.9f
private const val STARS_TURN_DEGREES = 40f

// Where the merge plays inside the effect's time: the sources close in first, then the result pops out.
private const val MERGE_SPAN = 0.25f
private const val RESULT_APPEAR_START = 0.2f
private const val RESULT_APPEAR_SPAN = 0.45f

@Composable
fun WorkspaceCanvas(
    items: List<WorkspaceItem>,
    onMove: (instanceId: Long, position: Offset) -> Unit,
    onResolve: (instanceId: Long, position: Offset) -> Unit,
    onPickUp: () -> Unit,
    onBoundsChanged: (Rect) -> Unit,
    effect: CombinationEffect?,
    effectTime: () -> Float,
    transitions: () -> List<TransitionFrame>,
    modifier: Modifier = Modifier
) {
    val currentItems by rememberUpdatedState(items)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnResolve by rememberUpdatedState(onResolve)
    val currentOnPickUp by rememberUpdatedState(onPickUp)
    var heldId by remember { mutableStateOf<Long?>(null) }
    val iconIds = (items.map(WorkspaceItem::elementId) + effect?.sources.orEmpty().map { it.elementId }).distinct()
    val icons = iconIds.associateWith { elementId ->
        key(elementId) { ImageBitmap.imageResource(elementIconRes(elementId)) }
    }
    val art = WorkspaceArt(
        icons = icons,
        magicCircle = ImageBitmap.imageResource(R.drawable.scene_magic_circle),
        flash = ImageBitmap.imageResource(R.drawable.fx_combine_flash),
        burst = ImageBitmap.imageResource(R.drawable.fx_success_burst),
        sparkles = ImageBitmap.imageResource(R.drawable.fx_sparkles_gold),
        appearSparkles = ImageBitmap.imageResource(R.drawable.fx_sparkles_blue),
        smoke = ImageBitmap.imageResource(R.drawable.fx_smoke_puff),
        heldRing = ImageBitmap.imageResource(R.drawable.fx_selected_ring),
        energyRing = ImageBitmap.imageResource(R.drawable.fx_energy_ring),
        shockwave = ImageBitmap.imageResource(R.drawable.fx_shockwave_ring),
        purpleSparkles = ImageBitmap.imageResource(R.drawable.fx_sparkles_purple),
        purpleOrb = ImageBitmap.imageResource(R.drawable.fx_glow_purple_orb),
        goldOrb = ImageBitmap.imageResource(R.drawable.fx_glow_gold_orb),
        stars = ImageBitmap.imageResource(R.drawable.fx_stars_cluster)
    )

    // Only one item is in hand at a time; it stays the lifted one while it settles back after being let go.
    val lift = remember { Animatable(0f) }
    var liftedId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(heldId) {
        if (heldId != null) {
            liftedId = heldId
            lift.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        } else {
            lift.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
            liftedId = null
        }
    }
    val targetPulse by rememberInfiniteTransition(label = "target").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(TARGET_PULSE_MILLIS), RepeatMode.Reverse),
        label = "targetPulse"
    )

    val labelColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val context = LocalContext.current
    val labelPaint = remember(labelColor) {
        android.graphics.Paint().apply {
            color = labelColor
            textAlign = android.graphics.Paint.Align.CENTER
            // The app's reading face at bold weight, like every other label.
            typeface = ResourcesCompat.getFont(context, R.font.alegreya)?.let { Typeface.create(it, LABEL_WEIGHT, false) }
            isAntiAlias = true
            // Keeps labels readable over the bright scene background.
            setShadowLayer(LABEL_SHADOW_RADIUS, 0f, 2f, android.graphics.Color.BLACK)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("workspace_canvas")
                .onGloballyPositioned { onBoundsChanged(it.boundsInRoot()) }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val item = currentItems
                            .minByOrNull { candidate -> distanceSquared(candidate, down.position, size.width.toFloat(), size.height.toFloat()) }
                            ?.takeIf { candidate ->
                                distanceSquared(candidate, down.position, size.width.toFloat(), size.height.toFloat()) <= itemRadiusSquared(size.width, size.height)
                            }
                            ?: return@awaitEachGesture
                        heldId = item.instanceId
                        currentOnPickUp()
                        try {
                            var lastPosition = down.position
                            val dragStart = awaitTouchSlopOrCancellation(down.id) { change, _ ->
                                change.consume()
                                lastPosition = change.position
                                currentOnMove(item.instanceId, normalize(lastPosition, size.width, size.height))
                            } ?: return@awaitEachGesture

                            drag(dragStart.id) { change ->
                                change.consume()
                                lastPosition = change.position
                                currentOnMove(item.instanceId, normalize(lastPosition, size.width, size.height))
                            }
                            val position = normalize(lastPosition, size.width, size.height)
                            currentOnMove(item.instanceId, position)
                            if (position.x in 0f..1f && position.y in 0f..1f) currentOnResolve(item.instanceId, position)
                        } finally {
                            heldId = null
                        }
                    }
                }
        ) {
            drawWatermark(art.magicCircle)
            val radius = minOf(size.width, size.height) * ITEM_RADIUS_FRACTION
            labelPaint.textSize = minOf(size.width, size.height) * LABEL_SIZE_FRACTION
            val frames = transitions()
            val time = if (effect == null) 0f else effectTime()
            val held = currentItems.find { it.instanceId == heldId }
            held?.let { current -> overlapTarget(currentItems, current.instanceId, current.xFraction, current.yFraction) }?.let { target ->
                val iconSize = radius * 2 * ICON_SHARE
                val center = Offset(target.xFraction * size.width, target.yFraction * size.height)
                drawCentered(art.energyRing, iconCenterOf(center, radius, iconSize), iconSize * TARGET_RING_SHARE, targetPulse)
            }
            val motion = ItemMotion(
                appearing = frames.filter { it.transition.kind == TransitionKind.APPEAR }.associateBy { it.transition.instanceId },
                shaking = frames.filter { it.transition.kind == TransitionKind.SHAKE }.associate { it.transition.instanceId to it.progress },
                resultInstanceId = effect?.resultInstanceId,
                effectTime = time,
                heldId = heldId,
                liftedId = liftedId,
                lift = lift.value
            )
            // The item in hand is drawn last, so it never slides under the others.
            currentItems.sortedBy { it.instanceId == heldId }.forEach { drawItem(it, art, labelPaint, radius, motion) }
            drawTransitions(frames, art, radius)
            effect?.let { drawEffect(it, time, art, radius) }
        }
        ItemAccessibilityNodes(items, constraints.maxWidth, constraints.maxHeight)
    }
}

/** One invisible node per item over the canvas, so a screen reader can reach each element and hear its name. */
@Composable
private fun ItemAccessibilityNodes(items: List<WorkspaceItem>, width: Int, height: Int) {
    val radius = minOf(width, height) * ITEM_RADIUS_FRACTION
    val side = with(LocalDensity.current) { (radius * 2).toDp() }
    items.forEach { item ->
        key(item.instanceId) {
            Box(
                modifier = Modifier
                    .offset { IntOffset((item.xFraction * width - radius).roundToInt(), (item.yFraction * height - radius).roundToInt()) }
                    .size(side)
                    .testTag("workspace_item")
                    .semantics { contentDescription = AlchemyCatalog.elementsById.getValue(item.elementId).name }
            )
        }
    }
}

/** Where every item is in its short movements this frame: appearing, refused, lifted, or a merge's result. */
private class ItemMotion(
    val appearing: Map<Long, TransitionFrame>,
    val shaking: Map<Long, Float>,
    val resultInstanceId: Long?,
    val effectTime: Float,
    val heldId: Long?,
    val liftedId: Long?,
    val lift: Float
)

private class WorkspaceArt(
    val icons: Map<String, ImageBitmap>,
    val magicCircle: ImageBitmap,
    val flash: ImageBitmap,
    val burst: ImageBitmap,
    val sparkles: ImageBitmap,
    val appearSparkles: ImageBitmap,
    val smoke: ImageBitmap,
    val heldRing: ImageBitmap,
    val energyRing: ImageBitmap,
    val shockwave: ImageBitmap,
    val purpleSparkles: ImageBitmap,
    val purpleOrb: ImageBitmap,
    val goldOrb: ImageBitmap,
    val stars: ImageBitmap
)

private fun DrawScope.drawItem(item: WorkspaceItem, art: WorkspaceArt, labelPaint: android.graphics.Paint, radius: Float, motion: ItemMotion) {
    val icon = art.icons[item.elementId] ?: return
    val iconSize = radius * 2 * ICON_SHARE
    var center = Offset(item.xFraction * size.width, item.yFraction * size.height)
    var scale = 1f
    var alpha = 1f
    motion.appearing[item.instanceId]?.let { frame ->
        scale = lerp(APPEAR_START_SCALE, 1f, easeOutBack(frame.progress))
        alpha = (frame.progress * 4f).coerceAtMost(1f)
        frame.origin?.let { origin ->
            val fly = easeOutCubic(frame.progress * FLY_IN_SPEED)
            center = Offset(lerp(origin.x * size.width, center.x, fly), lerp(origin.y * size.height, center.y, fly))
        }
    }
    if (item.instanceId == motion.resultInstanceId) {
        val progress = ((motion.effectTime - RESULT_APPEAR_START) / RESULT_APPEAR_SPAN).coerceIn(0f, 1f)
        scale = easeOutBack(progress)
        alpha = (progress * 4f).coerceAtMost(1f)
    }
    motion.shaking[item.instanceId]?.let { progress ->
        center += Offset(sin(progress * SHAKE_WAVES * 2f * PI.toFloat()) * (1f - progress) * radius * SHAKE_AMPLITUDE_SHARE, 0f)
    }
    val liftAmount = if (item.instanceId == motion.liftedId) motion.lift else 0f
    scale *= 1f + LIFT_SCALE * liftAmount
    if (alpha <= 0f || scale <= 0f) return
    val iconCenter = iconCenterOf(center, radius, iconSize)
    withTransform({ scale(scale, scale, pivot = iconCenter) }) {
        if (liftAmount > 0f) {
            drawOval(
                color = Color.Black.copy(alpha = LIFT_SHADOW_ALPHA * liftAmount),
                topLeft = Offset(iconCenter.x - iconSize * 0.4f, iconCenter.y + iconSize * 0.5f),
                size = Size(iconSize * 0.8f, iconSize * 0.2f)
            )
        }
        if (item.instanceId == motion.heldId) drawCentered(art.heldRing, iconCenter, iconSize * HELD_RING_SHARE, 1f)
        drawImage(
            image = icon,
            dstOffset = IntOffset((center.x - iconSize / 2).roundToInt(), (center.y - radius * ICON_TOP_SHARE).roundToInt()),
            dstSize = IntSize(iconSize.roundToInt(), iconSize.roundToInt()),
            alpha = alpha,
            filterQuality = FilterQuality.Medium
        )
        val labelBaseline = center.y - radius * ICON_TOP_SHARE + iconSize - labelPaint.ascent()
        labelPaint.alpha = (alpha * 255).roundToInt()
        drawContext.canvas.nativeCanvas.drawText(AlchemyCatalog.elementsById.getValue(item.elementId).name, center.x, labelBaseline, labelPaint)
        labelPaint.alpha = 255
    }
}

private fun DrawScope.drawTransitions(frames: List<TransitionFrame>, art: WorkspaceArt, radius: Float) {
    frames.forEach { (transition, progress) ->
        val center = Offset(transition.xFraction * size.width, transition.yFraction * size.height)
        val fade = 1f - progress
        when (transition.kind) {
            TransitionKind.APPEAR -> drawCentered(art.appearSparkles, center, radius * (1.8f + 1.2f * progress), fade)
            TransitionKind.VANISH -> drawCentered(art.smoke, center, radius * (1.4f + 1.4f * progress), fade * SMOKE_ALPHA)
            TransitionKind.SHAKE -> Unit
        }
    }
}

private fun DrawScope.drawEffect(effect: CombinationEffect, time: Float, art: WorkspaceArt, radius: Float) {
    val progress = LinearOutSlowInEasing.transform(time)
    val center = Offset(effect.xFraction * size.width, effect.yFraction * size.height)
    val fade = 1f - progress
    val iconSize = radius * 2 * ICON_SHARE
    val mergeProgress = (time / MERGE_SPAN).coerceIn(0f, 1f)
    val closing = easeInCubic(mergeProgress)
    effect.sources.forEach { source ->
        val icon = art.icons[source.elementId] ?: return@forEach
        val from = Offset(source.xFraction * size.width, source.yFraction * size.height)
        val ghostCenter = iconCenterOf(Offset(lerp(from.x, center.x, closing), lerp(from.y, center.y, closing)), radius, iconSize)
        val ghostSize = iconSize * lerp(1f, 0.5f, mergeProgress)
        drawImage(
            image = icon,
            dstOffset = IntOffset((ghostCenter.x - ghostSize / 2).roundToInt(), (ghostCenter.y - ghostSize / 2).roundToInt()),
            dstSize = IntSize(ghostSize.roundToInt(), ghostSize.roundToInt()),
            alpha = 1f - mergeProgress,
            filterQuality = FilterQuality.Medium
        )
    }
    if (effect.isDiscovery) {
        drawDiscovery(effect.rarity, center, time, art, radius)
    } else {
        drawCentered(art.energyRing, center, radius * (1.4f + 2.2f * progress), fade)
    }
    drawCentered(art.flash, center, radius * (1.6f + 2f * progress), fade)
}

// A first discovery grows with its rarity: rare adds a shockwave, epic a purple aura, legendary a golden one with turning stars.
private fun DrawScope.drawDiscovery(rarity: ElementRarity, center: Offset, time: Float, art: WorkspaceArt, radius: Float) {
    val progress = LinearOutSlowInEasing.transform(time)
    val fade = 1f - progress
    val grand = rarity == ElementRarity.EPIC || rarity == ElementRarity.LEGENDARY
    if (grand) {
        val orb = if (rarity == ElementRarity.EPIC) art.purpleOrb else art.goldOrb
        drawCentered(orb, center, radius * (2.2f + progress), fade * AURA_ALPHA)
    }
    drawCentered(art.burst, center, radius * (2.4f + 1.6f * progress), fade)
    if (rarity >= ElementRarity.RARE) drawCentered(art.shockwave, center, radius * (1.5f + 5f * progress), fade)
    drawCentered(if (rarity == ElementRarity.EPIC) art.purpleSparkles else art.sparkles, center, radius * 3.2f, fade)
    if (rarity == ElementRarity.LEGENDARY) {
        withTransform({ rotate(STARS_TURN_DEGREES * time, center) }) {
            drawCentered(art.stars, center, radius * (4f + 1.5f * progress), fade)
        }
    }
}

// The icon sits at the top of the item's square, with its label beneath.
private fun iconCenterOf(center: Offset, radius: Float, iconSize: Float) = Offset(center.x, center.y - radius * ICON_TOP_SHARE + iconSize / 2)

private fun easeOutBack(t: Float): Float {
    val c1 = 1.70158f
    val x = t - 1f
    return 1f + (c1 + 1f) * x * x * x + c1 * x * x
}

private fun easeOutCubic(t: Float): Float = 1f - (1f - t.coerceIn(0f, 1f)).let { it * it * it }

private fun easeInCubic(t: Float): Float = t * t * t

/** The magic circle as a faint watermark in the middle of the workspace. */
private fun DrawScope.drawWatermark(magicCircle: ImageBitmap) {
    drawCentered(magicCircle, center, minOf(size.width, size.height) * WATERMARK_SIZE_FRACTION, WATERMARK_ALPHA)
}

private fun DrawScope.drawCentered(image: ImageBitmap, center: Offset, width: Float, alpha: Float) {
    val height = width * image.height / image.width
    drawImage(
        image = image,
        dstOffset = IntOffset((center.x - width / 2).roundToInt(), (center.y - height / 2).roundToInt()),
        dstSize = IntSize(width.roundToInt(), height.roundToInt()),
        alpha = alpha.coerceIn(0f, 1f),
        filterQuality = FilterQuality.Medium
    )
}

private fun normalize(position: Offset, width: Int, height: Int): Offset = Offset(position.x / width, position.y / height)

private fun distanceSquared(item: WorkspaceItem, position: Offset, width: Float, height: Float): Float {
    val dx = item.xFraction * width - position.x
    val dy = item.yFraction * height - position.y
    return dx * dx + dy * dy
}

private fun itemRadiusSquared(width: Int, height: Int): Float = minOf(width, height).let { it * ITEM_RADIUS_FRACTION }.let { it * it }

private const val LABEL_WEIGHT = 700
