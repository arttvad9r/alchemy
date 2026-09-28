package com.artt.alchemy.ui.home

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.artt.alchemy.R
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.ElementDefinition
import com.artt.alchemy.game.WorkspaceEvent
import com.artt.alchemy.ui.AlchemyUiState
import com.artt.alchemy.ui.components.ElementIcon
import com.artt.alchemy.ui.components.ElementTile
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    state: AlchemyUiState,
    onEvent: (WorkspaceEvent) -> Unit,
    onDismissNewElement: () -> Unit,
    onFeedbackHandled: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unlocked = AlchemyCatalog.elements.filter { it.id in state.progress.unlockedIds }
    var workspaceBounds by remember { mutableStateOf<Rect?>(null) }
    var homeBounds by remember { mutableStateOf<Rect?>(null) }
    var draggedElement by remember { mutableStateOf<ElementDefinition?>(null) }
    var dragPosition by remember { mutableStateOf<Offset?>(null) }
    val previewHalfSize = with(androidx.compose.ui.platform.LocalDensity.current) { 36.dp.roundToPx() }
    val hapticFeedback = LocalHapticFeedback.current
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 40) }

    DisposableEffect(toneGenerator) {
        onDispose(toneGenerator::release)
    }
    LaunchedEffect(state.feedbackEventId) {
        if (state.feedbackEventId != 0L) {
            if (state.progress.vibrationEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            if (state.progress.soundEnabled) toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 100)
            onFeedbackHandled()
        }
    }

    Box(modifier = modifier.fillMaxSize().onGloballyPositioned { homeBounds = it.boundsInRoot() }) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.weight(1f))
                Text(text = stringResource(R.string.progress, state.progress.unlockedIds.size, AlchemyCatalog.elements.size))
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.workspace_title), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { onEvent(WorkspaceEvent.Clear) }, modifier = Modifier.testTag("clear_workspace")) {
                    Text(stringResource(R.string.clear_workspace))
                }
            }
            OutlinedCard(
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("home_workspace")
            ) {
                WorkspaceCanvas(
                    items = state.workspace.items,
                    onMove = { id, position -> onEvent(WorkspaceEvent.Move(id, position.x, position.y)) },
                    onResolve = { id, position -> onEvent(WorkspaceEvent.ResolveOverlap(id, position.x, position.y)) },
                    onBoundsChanged = { workspaceBounds = it },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = stringResource(R.string.palette_title), style = MaterialTheme.typography.titleMedium)
            Text(text = stringResource(R.string.palette_hint), style = MaterialTheme.typography.bodySmall)
            val paletteState = rememberLazyGridState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(152.dp)
                    .testTag("palette_grid")
            ) {
                LazyVerticalGrid(
                    state = paletteState,
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 12.dp, end = 16.dp, bottom = 12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(unlocked, key = { it.id }) { element ->
                        DraggablePaletteElement(
                            element = element,
                            modifier = Modifier.fillMaxWidth().testTag("palette_${element.id}"),
                            onDragPosition = { position ->
                                draggedElement = position?.let { element }
                                dragPosition = position
                            },
                            onDrop = { drop ->
                                workspaceBounds
                                    ?.takeIf { it.contains(drop) }
                                    ?.let { bounds ->
                                        onEvent(
                                            WorkspaceEvent.Spawn(
                                                element.id,
                                                (drop.x - bounds.left) / bounds.width,
                                                (drop.y - bounds.top) / bounds.height
                                            )
                                        )
                                    }
                            },
                            onTap = { onEvent(WorkspaceEvent.SpawnAutomatically(element.id)) }
                        )
                    }
                }
                PaletteScrollbar(state = paletteState, modifier = Modifier.align(Alignment.CenterEnd))
            }
        }

        val element = draggedElement
        val position = dragPosition
        val bounds = homeBounds
        if (element != null && position != null && bounds != null) {
            ElementTile(
                element = element,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        IntOffset(
                            (position.x - bounds.left - previewHalfSize).roundToInt(),
                            (position.y - bounds.top - previewHalfSize).roundToInt()
                        )
                    }
                    .width(72.dp)
                    .zIndex(1f)
                    .testTag("drag_preview")
            )
        }
    }

    state.newlyUnlockedId?.let { elementId ->
        AlertDialog(
            onDismissRequest = onDismissNewElement,
            title = { Text(stringResource(R.string.new_element_title)) },
            text = {
                val element = AlchemyCatalog.elementsById.getValue(elementId)
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    ElementIcon(element, Modifier.width(120.dp))
                    Text(stringResource(R.string.new_element_message, element.name))
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissNewElement) { Text(stringResource(R.string.ok)) }
            }
        )
    }
}

@Composable
private fun PaletteScrollbar(state: LazyGridState, modifier: Modifier = Modifier) {
    val totalItems = state.layoutInfo.totalItemsCount
    val visibleItems = state.layoutInfo.visibleItemsInfo.map { it.index }.distinct().size.coerceAtLeast(1)
    val thumbFraction = (visibleItems.toFloat() / totalItems.coerceAtLeast(visibleItems)).coerceIn(0.18f, 1f)
    val maxFirstVisibleIndex = (totalItems - visibleItems).coerceAtLeast(1)
    val scrollFraction = (state.firstVisibleItemIndex.toFloat() / maxFirstVisibleIndex).coerceIn(0f, 1f)
    val dragState = rememberDraggableState { delta ->
        state.dispatchRawDelta(delta * (totalItems.toFloat() / visibleItems).coerceAtLeast(1f))
    }

    BoxWithConstraints(
        modifier = modifier
            .width(12.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
            .testTag("palette_scrollbar")
    ) {
        val thumbHeight = maxHeight * thumbFraction
        val thumbOffset = (maxHeight - thumbHeight) * scrollFraction
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = thumbOffset)
                .width(8.dp)
                .height(thumbHeight)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                .draggable(state = dragState, orientation = Orientation.Vertical)
                .testTag("palette_scroll_thumb")
        )
    }
}

@Composable
private fun DraggablePaletteElement(
    element: ElementDefinition,
    modifier: Modifier,
    onDragPosition: (Offset?) -> Unit,
    onDrop: (Offset) -> Unit,
    onTap: () -> Unit
) {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val currentOnDragPosition by rememberUpdatedState(onDragPosition)
    val currentOnDrop by rememberUpdatedState(onDrop)

    ElementTile(
        element = element,
        modifier = modifier.onGloballyPositioned { coordinates = it }
            .pointerInput(element.id) {
                var lastPosition: Offset? = null
                detectDragGestures(
                    onDragStart = { position ->
                        lastPosition = coordinates?.localToRoot(position)
                        currentOnDragPosition(lastPosition)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        lastPosition = coordinates?.localToRoot(change.position)
                        currentOnDragPosition(lastPosition)
                    },
                    onDragEnd = {
                        lastPosition?.let(currentOnDrop)
                        lastPosition = null
                        currentOnDragPosition(null)
                    },
                    onDragCancel = {
                        lastPosition = null
                        currentOnDragPosition(null)
                    }
                )
            },
        onClick = onTap
    )
}
