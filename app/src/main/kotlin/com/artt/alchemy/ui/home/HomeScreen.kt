package com.artt.alchemy.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.artt.alchemy.R
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.ElementDefinition
import com.artt.alchemy.game.WorkspaceEvent
import com.artt.alchemy.ui.AlchemyUiState
import com.artt.alchemy.ui.CombinationEffect
import com.artt.alchemy.ui.ItemTransition
import com.artt.alchemy.ui.TransitionFrame
import com.artt.alchemy.ui.TransitionKind
import com.artt.alchemy.ui.components.AlchemyButton
import com.artt.alchemy.ui.components.ButtonStyle
import com.artt.alchemy.ui.components.ElementTile
import com.artt.alchemy.ui.components.panelBackground
import com.artt.alchemy.ui.components.rowPanel
import com.artt.alchemy.ui.theme.Gold
import com.artt.alchemy.ui.theme.PanelBorderColor
import com.artt.alchemy.ui.theme.PanelColor
import kotlin.math.roundToInt
import kotlinx.coroutines.currentCoroutineContext

private const val EFFECT_DURATION_MILLIS = 700
private const val DISCOVERY_CARD_AFTER_EFFECT = 0.3f
private const val TRANSITION_DURATION_MILLIS = 450L
private const val WORKSPACE_PANEL_ALPHA = 0.88f
private const val HINT_FADE_MILLIS = 250
private const val DRAGGED_TILE_ALPHA = 0.4f
private const val DRAGGED_TILE_FADE_MILLIS = 120

// Keeps the frame border thin; unscaled corners would eat into the item area.
private const val WORKSPACE_FRAME_SCALE = 1.3f
private val WORKSPACE_FRAME_INSET = 12.dp

@Composable
fun HomeScreen(
    state: AlchemyUiState,
    onEvent: (WorkspaceEvent) -> Unit,
    onDismissNewElement: () -> Unit,
    onPickUp: () -> Unit,
    onClick: () -> Unit,
    onEffectConsumed: () -> Unit,
    onTransitionsConsumed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unlockedIds = state.progress.unlockedIds
    val unlocked = remember(unlockedIds) { AlchemyCatalog.elements.filter { it.id in unlockedIds } }
    var workspaceBounds by remember { mutableStateOf<Rect?>(null) }
    var homeBounds by remember { mutableStateOf<Rect?>(null) }
    var draggedElement by remember { mutableStateOf<ElementDefinition?>(null) }
    var dragPosition by remember { mutableStateOf<Offset?>(null) }
    val previewHalfSize = with(androidx.compose.ui.platform.LocalDensity.current) { 36.dp.roundToPx() }

    // The effect is taken out of the UI state at once so it does not replay when Home is shown again.
    var playingEffect by remember { mutableStateOf<CombinationEffect?>(null) }
    // Linear time of the effect; each part of it applies its own easing.
    val effectTime = remember { Animatable(0f) }
    LaunchedEffect(state.combinationEffect) {
        state.combinationEffect?.let { effect ->
            playingEffect = effect
            onEffectConsumed()
        }
    }
    LaunchedEffect(playingEffect) {
        if (playingEffect != null) {
            effectTime.snapTo(0f)
            effectTime.animateTo(1f, tween(EFFECT_DURATION_MILLIS, easing = LinearEasing))
            playingEffect = null
            // Back to the start, so the next effect never begins looking already half played.
            effectTime.snapTo(0f)
        }
    }

    // Appear and vanish effects run side by side, each timed from the frame it started on.
    val playingTransitions = remember { mutableStateListOf<PlayingTransition>() }
    // Where the tile last tapped sits, so the item it adds flies in from there.
    var tapOrigin by remember { mutableStateOf<Offset?>(null) }
    val originFraction = tapOrigin?.let { origin ->
        workspaceBounds?.let { bounds -> Offset((origin.x - bounds.left) / bounds.width, (origin.y - bounds.top) / bounds.height) }
    }
    var transitionClock by remember { mutableLongStateOf(0L) }
    var transitionDuration by remember { mutableLongStateOf(TRANSITION_DURATION_MILLIS) }
    LaunchedEffect(state.itemTransitions) {
        if (state.itemTransitions.isNotEmpty()) {
            val now = withFrameMillis { it }
            playingTransitions += state.itemTransitions.map { PlayingTransition(it, now, originFraction.takeIf { _ -> it.kind == TransitionKind.APPEAR }) }
            tapOrigin = null
            onTransitionsConsumed()
        }
    }
    TransitionClock(playingTransitions) { now, duration ->
        transitionClock = now
        transitionDuration = duration
    }

    Box(modifier = modifier.fillMaxSize().onGloballyPositioned { homeBounds = it.boundsInRoot() }) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                GameTitle(modifier = Modifier.weight(1f))
                AlchemyButton(
                    text = stringResource(R.string.clear_workspace),
                    style = ButtonStyle.BLUE,
                    onClick = { onEvent(WorkspaceEvent.Clear) },
                    modifier = Modifier.testTag("clear_workspace")
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // A distinct slab over the scene: the background only faintly shows through.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .panelBackground(R.drawable.dialog_blue, alpha = WORKSPACE_PANEL_ALPHA, maxScale = WORKSPACE_FRAME_SCALE)
                    .testTag("home_workspace")
                    // Items live inside the frame, so ones near an edge never cover its border.
                    .padding(WORKSPACE_FRAME_INSET)
            ) {
                WorkspaceCanvas(
                    items = state.workspace.items,
                    onMove = { id, position -> onEvent(WorkspaceEvent.Move(id, position.x, position.y)) },
                    onResolve = { id, position -> onEvent(WorkspaceEvent.ResolveOverlap(id, position.x, position.y)) },
                    onPickUp = onPickUp,
                    onBoundsChanged = { workspaceBounds = it },
                    // A new effect is drawn from its first frame, before it is taken to play.
                    effect = playingEffect ?: state.combinationEffect,
                    effectTime = { if (playingEffect == null) 0f else effectTime.value },
                    // Transitions not yet taken to play are drawn from their first frame too.
                    transitions = {
                        state.itemTransitions.map { TransitionFrame(it, 0f, originFraction.takeIf { _ -> it.kind == TransitionKind.APPEAR }) } +
                            playingTransitions.map {
                                TransitionFrame(
                                    it.transition,
                                    ((transitionClock - it.startMillis).toFloat() / transitionDuration).coerceIn(0f, 1f),
                                    it.origin
                                )
                            }
                    }
                )
                val hintAlpha by animateFloatAsState(if (state.workspace.items.isEmpty()) 1f else 0f, tween(HINT_FADE_MILLIS), label = "hintAlpha")
                if (hintAlpha > 0f) {
                    Text(
                        text = stringResource(R.string.workspace_hint),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp).alpha(hintAlpha)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PanelColor, RoundedCornerShape(20.dp))
                    .border(1.dp, PanelBorderColor, RoundedCornerShape(20.dp))
                    .padding(start = 12.dp, top = 10.dp, end = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(end = 4.dp)) {
                    Text(text = stringResource(R.string.palette_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.weight(1f))
                    ProgressCounter(unlocked = unlockedIds.size)
                }
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
                                dimmed = draggedElement?.id == element.id,
                                modifier = Modifier.fillMaxWidth().testTag("palette_${element.id}"),
                                onDragPosition = { position ->
                                    draggedElement = position?.let { element }
                                    dragPosition = position
                                },
                                onDrop = { drop ->
                                    tapOrigin = null
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
                                onTap = { center ->
                                    tapOrigin = center
                                    onEvent(WorkspaceEvent.SpawnAutomatically(element.id))
                                },
                                onPickUp = onPickUp
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

    // The discovery card comes up once the flash has shown, while the burst plays on beneath it. The effect is
    // still in the UI state for the first frame, before it is taken to play, so that is checked too.
    val effectShown by remember { derivedStateOf { LinearOutSlowInEasing.transform(effectTime.value) >= DISCOVERY_CARD_AFTER_EFFECT } }
    state.newlyUnlockedId?.takeIf { state.combinationEffect == null && (playingEffect == null || effectShown) }?.let { elementId ->
        val element = AlchemyCatalog.elementsById.getValue(elementId)
        NewElementDialog(element, onDismiss = onDismissNewElement, onClick = onClick)
    }
}

/** How many elements are open out of the whole catalog, on a small panel with a book. */
@Composable
private fun ProgressCounter(unlocked: Int) {
    val bounce = remember { Animatable(1f) }
    var previous by remember { mutableIntStateOf(unlocked) }
    LaunchedEffect(unlocked) {
        if (unlocked > previous) {
            bounce.snapTo(COUNTER_BOUNCE_SCALE)
            bounce.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
        previous = unlocked
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .graphicsLayer {
                scaleX = bounce.value
                scaleY = bounce.value
            }
            .rowPanel()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Image(painter = painterResource(R.drawable.nav_recipes), contentDescription = null, modifier = Modifier.size(20.dp))
        Text(
            text = stringResource(R.string.progress, unlocked, AlchemyCatalog.elements.size),
            style = MaterialTheme.typography.titleSmall,
            color = Gold
        )
    }
}

/** Ticks every frame while transitions play, reporting the frame time and how long a transition lasts. */
@Composable
private fun TransitionClock(playing: MutableList<PlayingTransition>, onTick: (now: Long, duration: Long) -> Unit) {
    val hasPlaying = playing.isNotEmpty()
    LaunchedEffect(hasPlaying) {
        // Follows the system's animation scale, like the animations Compose times itself.
        val scale = currentCoroutineContext()[MotionDurationScale]?.scaleFactor ?: 1f
        val duration = (TRANSITION_DURATION_MILLIS * scale).toLong().coerceAtLeast(1L)
        while (playing.isNotEmpty()) {
            withFrameMillis { now ->
                onTick(now, duration)
                playing.removeAll { now - it.startMillis >= duration }
            }
        }
    }
}

private const val COUNTER_BOUNCE_SCALE = 1.25f

private data class PlayingTransition(val transition: ItemTransition, val startMillis: Long, val origin: Offset?)

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
    dimmed: Boolean,
    modifier: Modifier,
    onDragPosition: (Offset?) -> Unit,
    onDrop: (Offset) -> Unit,
    onTap: (center: Offset?) -> Unit,
    onPickUp: () -> Unit
) {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val currentOnDragPosition by rememberUpdatedState(onDragPosition)
    val currentOnDrop by rememberUpdatedState(onDrop)
    val currentOnPickUp by rememberUpdatedState(onPickUp)

    val alpha by animateFloatAsState(if (dimmed) DRAGGED_TILE_ALPHA else 1f, tween(DRAGGED_TILE_FADE_MILLIS), label = "tileAlpha")
    ElementTile(
        element = element,
        modifier = modifier.alpha(alpha).onGloballyPositioned { coordinates = it }
            .pointerInput(element.id) {
                var lastPosition: Offset? = null
                detectDragGestures(
                    onDragStart = { position ->
                        currentOnPickUp()
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
        onClick = { onTap(coordinates?.boundsInRoot()?.center) }
    )
}
