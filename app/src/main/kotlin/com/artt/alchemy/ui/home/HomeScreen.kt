package com.artt.alchemy.ui.home

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.artt.alchemy.R
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.ElementDefinition
import com.artt.alchemy.game.WorkspaceEvent
import com.artt.alchemy.ui.AlchemyUiState
import com.artt.alchemy.ui.CombinationEffect
import com.artt.alchemy.ui.components.AlchemyButton
import com.artt.alchemy.ui.components.AlchemyDialog
import com.artt.alchemy.ui.components.ButtonStyle
import com.artt.alchemy.ui.components.ElementTile
import com.artt.alchemy.ui.components.FramedElementIcon
import com.artt.alchemy.ui.components.RarityBadge
import com.artt.alchemy.ui.components.panelBackground
import com.artt.alchemy.ui.components.rarity
import com.artt.alchemy.ui.theme.Gold
import com.artt.alchemy.ui.theme.PanelBorderColor
import com.artt.alchemy.ui.theme.PanelColor
import kotlin.math.roundToInt

private const val EFFECT_DURATION_MILLIS = 700
private const val WORKSPACE_PANEL_ALPHA = 0.88f

// Keeps the frame border thin; unscaled corners would eat into the item area.
private const val WORKSPACE_FRAME_SCALE = 1.3f

@Composable
fun HomeScreen(
    state: AlchemyUiState,
    onEvent: (WorkspaceEvent) -> Unit,
    onDismissNewElement: () -> Unit,
    onFeedbackHandled: () -> Unit,
    onEffectConsumed: () -> Unit,
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

    // The effect is taken out of the UI state at once so it does not replay when Home is shown again.
    var playingEffect by remember { mutableStateOf<CombinationEffect?>(null) }
    val effectProgress = remember { Animatable(0f) }
    LaunchedEffect(state.combinationEffect) {
        state.combinationEffect?.let { effect ->
            playingEffect = effect
            onEffectConsumed()
        }
    }
    LaunchedEffect(playingEffect) {
        if (playingEffect != null) {
            effectProgress.snapTo(0f)
            effectProgress.animateTo(1f, tween(EFFECT_DURATION_MILLIS, easing = LinearOutSlowInEasing))
            playingEffect = null
        }
    }

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
                Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, color = Gold)
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .background(PanelColor, RoundedCornerShape(14.dp))
                        .border(1.dp, PanelBorderColor, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Image(painter = painterResource(R.drawable.nav_recipes), contentDescription = null, modifier = Modifier.size(22.dp))
                    Text(
                        text = stringResource(R.string.progress, state.progress.unlockedIds.size, AlchemyCatalog.elements.size),
                        style = MaterialTheme.typography.titleMedium,
                        color = Gold
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.workspace_title), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.weight(1f))
                AlchemyButton(
                    text = stringResource(R.string.clear_workspace),
                    style = ButtonStyle.BLUE,
                    onClick = { onEvent(WorkspaceEvent.Clear) },
                    modifier = Modifier.testTag("clear_workspace")
                )
            }
            // A distinct slab over the scene: the background only faintly shows through.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .panelBackground(R.drawable.dialog_blue, alpha = WORKSPACE_PANEL_ALPHA, maxScale = WORKSPACE_FRAME_SCALE)
                    .testTag("home_workspace")
            ) {
                WorkspaceCanvas(
                    items = state.workspace.items,
                    onMove = { id, position -> onEvent(WorkspaceEvent.Move(id, position.x, position.y)) },
                    onResolve = { id, position -> onEvent(WorkspaceEvent.ResolveOverlap(id, position.x, position.y)) },
                    onBoundsChanged = { workspaceBounds = it },
                    effect = playingEffect,
                    effectProgress = { effectProgress.value }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PanelColor, RoundedCornerShape(20.dp))
                    .border(1.dp, PanelBorderColor, RoundedCornerShape(20.dp))
                    .padding(start = 12.dp, top = 10.dp, end = 8.dp)
            ) {
                Text(text = stringResource(R.string.palette_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.palette_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                    PaletteScrollbar(state = paletteState, modifier = Modifier.align(Alignment.CenterEnd).padding(vertical = 12.dp))
                }
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

    // The discovery card waits until the combination effect has played.
    state.newlyUnlockedId?.takeIf { playingEffect == null }?.let { elementId ->
        val element = AlchemyCatalog.elementsById.getValue(elementId)
        AlchemyDialog(onDismissRequest = onDismissNewElement, panelRes = R.drawable.dialog_gold) {
            Text(stringResource(R.string.new_element_title), style = MaterialTheme.typography.headlineSmall, color = Gold)
            FramedElementIcon(element, Modifier.padding(top = 16.dp).width(140.dp))
            RarityBadge(element.rarity, Modifier.padding(vertical = 8.dp))
            Text(stringResource(R.string.new_element_message, element.name), modifier = Modifier.padding(bottom = 16.dp))
            AlchemyButton(stringResource(R.string.ok), ButtonStyle.GOLD, onDismissNewElement)
        }
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
