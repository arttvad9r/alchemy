package com.artt.alchemy.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.artt.alchemy.R
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.WorkspaceItem
import com.artt.alchemy.ui.CombinationEffect
import com.artt.alchemy.ui.ItemTransition
import com.artt.alchemy.ui.TransitionKind
import com.artt.alchemy.ui.components.elementIconRes
import kotlin.math.roundToInt

private const val ITEM_RADIUS_FRACTION = 0.11f
private const val LABEL_SIZE_FRACTION = 0.042f
private const val ICON_SHARE = 0.66f
private const val ICON_TOP_SHARE = 0.85f
private const val LABEL_SHADOW_RADIUS = 6f
private const val WATERMARK_SIZE_FRACTION = 0.85f
private const val WATERMARK_ALPHA = 0.28f
private const val SMOKE_ALPHA = 0.9f
private const val HELD_RING_SHARE = 1.45f

@Composable
fun WorkspaceCanvas(
    items: List<WorkspaceItem>,
    onMove: (instanceId: Long, position: Offset) -> Unit,
    onResolve: (instanceId: Long, position: Offset) -> Unit,
    onBoundsChanged: (Rect) -> Unit,
    effect: CombinationEffect?,
    effectProgress: () -> Float,
    transitions: () -> List<Pair<ItemTransition, Float>>,
    modifier: Modifier = Modifier
) {
    val currentItems by rememberUpdatedState(items)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnResolve by rememberUpdatedState(onResolve)
    var heldId by remember { mutableStateOf<Long?>(null) }
    val icons = items.map(WorkspaceItem::elementId).distinct().associateWith { elementId ->
        key(elementId) { ImageBitmap.imageResource(elementIconRes(elementId)) }
    }
    val magicCircle = ImageBitmap.imageResource(R.drawable.scene_magic_circle)
    val flash = ImageBitmap.imageResource(R.drawable.fx_combine_flash)
    val burst = ImageBitmap.imageResource(R.drawable.fx_success_burst)
    val sparkles = ImageBitmap.imageResource(R.drawable.fx_sparkles_gold)
    val appearSparkles = ImageBitmap.imageResource(R.drawable.fx_sparkles_blue)
    val smoke = ImageBitmap.imageResource(R.drawable.fx_smoke_puff)
    val heldRing = ImageBitmap.imageResource(R.drawable.fx_selected_ring)
    val energyRing = ImageBitmap.imageResource(R.drawable.fx_energy_ring)

    val labelColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val labelPaint = remember(labelColor) {
        android.graphics.Paint().apply {
            color = labelColor
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
            // Keeps labels readable over the bright scene background.
            setShadowLayer(LABEL_SHADOW_RADIUS, 0f, 2f, android.graphics.Color.BLACK)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("workspace_canvas")
            .semantics { contentDescription = currentItems.joinToString { AlchemyCatalog.elementsById.getValue(it.elementId).name } }
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
        drawWatermark(magicCircle)
        val radius = minOf(size.width, size.height) * ITEM_RADIUS_FRACTION
        labelPaint.textSize = minOf(size.width, size.height) * LABEL_SIZE_FRACTION
        currentItems.forEach { item ->
            val element = AlchemyCatalog.elementsById.getValue(item.elementId)
            val center = Offset(item.xFraction * size.width, item.yFraction * size.height)
            val icon = icons[item.elementId] ?: return@forEach
            // Icon and label share the item's square so the label stays inside the workspace like the icon.
            val iconSize = radius * 2 * ICON_SHARE
            if (item.instanceId == heldId) {
                val iconCenter = Offset(center.x, center.y - radius * ICON_TOP_SHARE + iconSize / 2)
                drawCentered(heldRing, iconCenter, iconSize * HELD_RING_SHARE, 1f)
            }
            drawImage(
                image = icon,
                dstOffset = IntOffset((center.x - iconSize / 2).roundToInt(), (center.y - radius * ICON_TOP_SHARE).roundToInt()),
                dstSize = IntSize(iconSize.roundToInt(), iconSize.roundToInt()),
                filterQuality = FilterQuality.Medium
            )
            val labelBaseline = center.y - radius * ICON_TOP_SHARE + iconSize - labelPaint.ascent()
            drawContext.canvas.nativeCanvas.drawText(element.name, center.x, labelBaseline, labelPaint)
        }
        transitions().forEach { (transition, progress) ->
            val center = Offset(transition.xFraction * size.width, transition.yFraction * size.height)
            val fade = 1f - progress
            when (transition.kind) {
                TransitionKind.APPEAR -> drawCentered(appearSparkles, center, radius * (1.8f + 1.2f * progress), fade)
                TransitionKind.VANISH -> drawCentered(smoke, center, radius * (1.4f + 1.4f * progress), fade * SMOKE_ALPHA)
            }
        }
        effect?.let { current ->
            val progress = effectProgress()
            val center = Offset(current.xFraction * size.width, current.yFraction * size.height)
            val fade = 1f - progress
            if (current.isDiscovery) {
                drawCentered(burst, center, radius * (2.4f + 1.6f * progress), fade)
                drawCentered(sparkles, center, radius * 3.2f, fade)
            } else {
                drawCentered(energyRing, center, radius * (1.4f + 2.2f * progress), fade)
            }
            drawCentered(flash, center, radius * (1.6f + 2f * progress), fade)
        }
    }
}

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
