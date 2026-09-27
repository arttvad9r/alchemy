package com.artt.alchemy.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.WorkspaceItem

private const val ITEM_RADIUS_FRACTION = 0.11f

@Composable
fun WorkspaceCanvas(
    items: List<WorkspaceItem>,
    onMove: (instanceId: Long, position: Offset) -> Unit,
    onResolve: (instanceId: Long, position: Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentItems by rememberUpdatedState(items)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnResolve by rememberUpdatedState(onResolve)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("workspace_canvas")
            .semantics { contentDescription = currentItems.joinToString { AlchemyCatalog.elementsById.getValue(it.elementId).name } }
            .pointerInput(Unit) {
                var activeItemId: Long? = null
                var lastPosition = Offset.Zero

                detectDragGestures(
                    onDragStart = { position ->
                        activeItemId = currentItems
                            .minByOrNull { item -> distanceSquared(item, position, size.width.toFloat(), size.height.toFloat()) }
                            ?.takeIf { item ->
                                distanceSquared(item, position, size.width.toFloat(), size.height.toFloat()) <= itemRadiusSquared(size.width, size.height)
                            }
                            ?.instanceId
                        lastPosition = position
                    },
                    onDrag = { change, _ ->
                        activeItemId?.let { id ->
                            change.consume()
                            lastPosition = change.position
                            currentOnMove(id, normalize(lastPosition, size.width, size.height))
                        }
                    },
                    onDragEnd = {
                        activeItemId?.let { id ->
                            val position = normalize(lastPosition, size.width, size.height)
                            currentOnMove(id, position)
                            if (position.x in 0f..1f && position.y in 0f..1f) currentOnResolve(id, position)
                        }
                        activeItemId = null
                    },
                    onDragCancel = { activeItemId = null }
                )
            }
    ) {
        currentItems.forEach { item ->
            val element = AlchemyCatalog.elementsById.getValue(item.elementId)
            val center = Offset(item.xFraction * size.width, item.yFraction * size.height)
            drawCircle(color = Color(element.color), radius = minOf(size.width, size.height) * ITEM_RADIUS_FRACTION, center = center)
            drawContext.canvas.nativeCanvas.drawText(
                element.name,
                center.x,
                center.y,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = minOf(size.width, size.height) * 0.09f
                    isFakeBoldText = true
                }
            )
        }
    }
}

private fun normalize(position: Offset, width: Int, height: Int): Offset = Offset(position.x / width, position.y / height)

private fun distanceSquared(item: WorkspaceItem, position: Offset, width: Float, height: Float): Float {
    val dx = item.xFraction * width - position.x
    val dy = item.yFraction * height - position.y
    return dx * dx + dy * dy
}

private fun itemRadiusSquared(width: Int, height: Int): Float = minOf(width, height).let { it * ITEM_RADIUS_FRACTION }.let { it * it }
