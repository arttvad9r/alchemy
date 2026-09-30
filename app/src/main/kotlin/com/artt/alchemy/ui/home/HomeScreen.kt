package com.artt.alchemy.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.runtime.key
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.artt.alchemy.R
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.ElementDefinition
import com.artt.alchemy.game.ElementRarity
import com.artt.alchemy.game.ElementSort
import com.artt.alchemy.game.WorkspaceEvent
import com.artt.alchemy.game.sortElements
import com.artt.alchemy.ui.AlchemyUiState
import com.artt.alchemy.ui.CombinationEffect
import com.artt.alchemy.ui.ItemTransition
import com.artt.alchemy.ui.Reveal
import com.artt.alchemy.ui.TransitionFrame
import com.artt.alchemy.ui.TransitionKind
import com.artt.alchemy.ui.components.AlchemyButton
import com.artt.alchemy.ui.components.AlchemyDropdown
import com.artt.alchemy.ui.components.AlchemyIconButton
import com.artt.alchemy.ui.components.ButtonStyle
import com.artt.alchemy.ui.components.ElementIcon
import com.artt.alchemy.ui.components.ElementTextGap
import com.artt.alchemy.ui.components.ElementTile
import com.artt.alchemy.ui.components.LocalReducedMotion
import com.artt.alchemy.ui.components.ScreenPadding
import com.artt.alchemy.ui.components.WholeWordsAutoSize
import com.artt.alchemy.ui.components.elementName
import com.artt.alchemy.ui.components.motion
import com.artt.alchemy.ui.components.panelBackground
import com.artt.alchemy.ui.components.pillBadge
import com.artt.alchemy.ui.components.rarity
import com.artt.alchemy.ui.theme.Gold
import com.artt.alchemy.ui.theme.PanelBorderColor
import com.artt.alchemy.ui.theme.PanelColor
import com.artt.alchemy.ui.theme.TitleFontFamily
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.currentCoroutineContext

private const val EFFECT_DURATION_MILLIS = 700
private const val DISCOVERY_CARD_AFTER_EFFECT = 0.3f

// An epic or legendary find holds the stage a little longer before its card comes up.
private const val GRAND_CARD_AFTER_EFFECT = 0.6f
private const val TRANSITION_DURATION_MILLIS = 450L
private const val WORKSPACE_PANEL_ALPHA = 0.88f
private const val DRAGGED_TILE_ALPHA = 0.4f
private const val DRAGGED_TILE_FADE_MILLIS = 120

// Keeps the frame border thin; unscaled corners would eat into the item area.
private const val WORKSPACE_FRAME_SCALE = 1.3f
private val WORKSPACE_FRAME_INSET = 12.dp

@Composable
fun HomeScreen(
    state: AlchemyUiState,
    onEvent: (WorkspaceEvent) -> Unit,
    onDismissNewElement: (elementId: String) -> Unit,
    onPickUp: () -> Unit,
    onClick: () -> Unit,
    onPaletteSort: (ElementSort) -> Unit,
    onEffectConsumed: () -> Unit,
    onTransitionsConsumed: () -> Unit,
    onSkipTips: () -> Unit,
    onShowTips: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unlockedIds = state.progress.unlockedIds
    val paletteSort = state.progress.paletteSort
    val discoveryOrder = state.progress.discoveryOrder
    val resources = LocalContext.current.resources
    val unlocked = remember(unlockedIds, discoveryOrder, paletteSort, resources) {
        sortElements(AlchemyCatalog.elements.filter { it.id in unlockedIds }, discoveryOrder, paletteSort, Locale.getDefault(), resources::elementName)
    }
    var workspaceBounds by remember { mutableStateOf<Rect?>(null) }
    var homeBounds by remember { mutableStateOf<Rect?>(null) }
    var draggedElement by remember { mutableStateOf<ElementDefinition?>(null) }
    var dragPosition by remember { mutableStateOf<Offset?>(null) }

    // The effect is taken out of the UI state at once so it does not replay when Home is shown again.
    var playingEffect by remember { mutableStateOf<CombinationEffect?>(null) }
    // Linear time of the effect; each part of it applies its own easing.
    val effectTime = remember { Animatable(0f) }
    val reducedMotion = LocalReducedMotion.current
    LaunchedEffect(state.combinationEffect) {
        state.combinationEffect?.let { effect ->
            // With reduced motion the result simply appears, and the discovery card follows at once.
            playingEffect = effect.takeUnless { reducedMotion }
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
            playingTransitions += state.itemTransitions.takeUnless { reducedMotion }.orEmpty().map { PlayingTransition(it, now, originFraction.takeIf { _ -> it.kind == TransitionKind.APPEAR }) }
            tapOrigin = null
            onTransitionsConsumed()
        }
    }
    TransitionClock(playingTransitions) { now, duration ->
        transitionClock = now
        transitionDuration = duration
    }

    Box(modifier = modifier.fillMaxSize().onGloballyPositioned { homeBounds = it.boundsInRoot() }) {
        Column(modifier = Modifier.fillMaxSize().padding(ScreenPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                GameTitle(modifier = Modifier.weight(1f))
                AlchemyIconButton(
                    icon = R.drawable.ic_info,
                    contentDescription = stringResource(R.string.tips_show),
                    onClick = onShowTips,
                    modifier = Modifier.padding(end = 8.dp).testTag("show_tips")
                )
                AlchemyButton(
                    text = stringResource(R.string.clear_workspace),
                    style = ButtonStyle.BLUE,
                    onClick = { onEvent(WorkspaceEvent.Clear) },
                    modifier = Modifier.testTag("clear_workspace")
                )
            }
            Spacer(modifier = Modifier.height(ScreenPadding))
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
                WorkspaceGuidance(
                    items = state.workspace.items,
                    tipsVisible = !state.progress.onboardingSeen,
                    tipStep = state.tipStep,
                    onSkipTips = onSkipTips
                )
            }
            Spacer(modifier = Modifier.height(ScreenPadding))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PanelColor, RoundedCornerShape(20.dp))
                    .border(1.dp, PanelBorderColor, RoundedCornerShape(20.dp))
                    .padding(start = 12.dp, top = 10.dp, end = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(end = 4.dp)) {
                    val titleStyle = MaterialTheme.typography.titleMedium
                    Text(
                        text = stringResource(R.string.palette_title),
                        style = titleStyle,
                        maxLines = 1,
                        autoSize = WholeWordsAutoSize(min = PALETTE_TITLE_MIN_SIZE, max = titleStyle.fontSize),
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    AlchemyDropdown(
                        options = ElementSort.entries,
                        selected = paletteSort,
                        label = { stringResource(it.labelRes) },
                        onSelect = onPaletteSort,
                        tag = "palette_sort",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    ProgressCounter(unlocked = unlockedIds.size)
                }
                val paletteState = rememberLazyGridState()
                val labelHeight = with(LocalDensity.current) { MaterialTheme.typography.labelSmall.lineHeight.toDp() }
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    // Exactly two whole rows, labels included, so no row is ever cut through its names.
                    val tileWidth = (maxWidth - PALETTE_END_PADDING - PALETTE_GAP * (PALETTE_COLUMNS - 1)) / PALETTE_COLUMNS
                    val rowHeight = tileWidth + ElementTextGap + labelHeight
                    val paletteHeight = rowHeight * PALETTE_ROWS + PALETTE_GAP * (PALETTE_ROWS - 1) + PALETTE_VERTICAL_PADDING * 2
                    Box(modifier = Modifier.fillMaxWidth().height(paletteHeight).testTag("palette_grid")) {
                        LazyVerticalGrid(
                            state = paletteState,
                            columns = GridCells.Fixed(PALETTE_COLUMNS),
                            horizontalArrangement = Arrangement.spacedBy(PALETTE_GAP),
                            verticalArrangement = Arrangement.spacedBy(PALETTE_GAP),
                            contentPadding = PaddingValues(top = PALETTE_VERTICAL_PADDING, end = PALETTE_END_PADDING, bottom = PALETTE_VERTICAL_PADDING),
                            flingBehavior = rememberSnapFlingBehavior(paletteState, SnapPosition.Start),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(unlocked, key = { it.id }) { element ->
                                DraggablePaletteElement(
                                    element = element,
                                    dimmed = draggedElement?.id == element.id,
                                    fresh = element.id in state.freshElementIds,
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
                                                // The element lands where its icon was under the finger, clear of the edges.
                                                val at = dropPosition(Offset(drop.x - bounds.left, drop.y - bounds.top), bounds.width, bounds.height)
                                                onEvent(WorkspaceEvent.Spawn(element.id, at.x, at.y))
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
        }

        val element = draggedElement
        val position = dragPosition
        val bounds = homeBounds
        if (element != null && position != null && bounds != null) {
            DragPreview(element, position - bounds.topLeft, workspaceBounds)
        }
    }

    // The discovery card comes up once the flash has shown, while the burst plays on beneath it. The effect is
    // still in the UI state for the first frame, before it is taken to play, so that is checked too.
    val effectShown by remember {
        derivedStateOf {
            val grand = playingEffect?.let { it.rarity >= ElementRarity.EPIC } == true
            LinearOutSlowInEasing.transform(effectTime.value) >= if (grand) GRAND_CARD_AFTER_EFFECT else DISCOVERY_CARD_AFTER_EFFECT
        }
    }
    (state.reveal as? Reveal.Discovery)?.elementId?.takeIf { state.combinationEffect == null && (playingEffect == null || effectShown) }?.let { elementId ->
        val element = AlchemyCatalog.elementsById.getValue(elementId)
        // Keyed, so a card that follows another plays its entrance afresh.
        key(elementId) { NewElementDialog(element, onDismiss = { onDismissNewElement(elementId) }, onClick = onClick) }
    }
}

/**
 * The element in hand while it is dragged out of the palette, drawn as it will look on the workspace: its bare icon and
 * name at the workspace's size, centred under the finger at [at] and glowing in its rarity's colour. It rises a little
 * as it leaves the palette and lands without changing shape.
 */
@Composable
private fun BoxScope.DragPreview(element: ElementDefinition, at: Offset, workspace: Rect?) {
    val density = LocalDensity.current
    val iconPx = workspace?.let { workspaceIconSize(it.width, it.height) } ?: with(density) { PREVIEW_FALLBACK_ICON.toPx() }
    val labelSize = workspace?.let { with(density) { workspaceLabelSize(it.width, it.height).toSp() } } ?: MaterialTheme.typography.labelLarge.fontSize
    val lift = remember(element.id) { Animatable(1f) }
    val liftSpec = motion(spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    LaunchedEffect(element.id) { lift.animateTo(PREVIEW_LIFT_SCALE, liftSpec) }
    val glow = element.rarity.glowColor
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .align(Alignment.TopStart)
            .layout { measurable, constraints ->
                // Centres the icon, not the whole column, under the finger.
                val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                layout(placeable.width, placeable.height) {
                    placeable.place((at.x - placeable.width / 2f).roundToInt(), (at.y - iconPx / 2f).roundToInt())
                }
            }
            .zIndex(1f)
            .graphicsLayer {
                scaleX = lift.value
                scaleY = lift.value
                transformOrigin = TransformOrigin(0.5f, iconPx / 2f / size.height.coerceAtLeast(1f))
            }
            .testTag("drag_preview")
    ) {
        ElementIcon(
            element,
            Modifier
                .size(with(density) { iconPx.toDp() })
                .drawBehind { drawGlow(center, size.width * PREVIEW_GLOW_SHARE, glow, PREVIEW_GLOW_ALPHA) }
        )
        Text(
            text = elementName(element.id),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = labelSize, shadow = PreviewLabelShadow),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false
        )
    }
}

/** How many elements are open out of the whole catalog, on a small panel with a book. */
@Composable
private fun ProgressCounter(unlocked: Int) {
    val bounce = remember { Animatable(1f) }
    val bounceSpec = motion(spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    var previous by remember { mutableIntStateOf(unlocked) }
    LaunchedEffect(unlocked) {
        if (unlocked > previous) {
            bounce.snapTo(COUNTER_BOUNCE_SCALE)
            bounce.animateTo(1f, bounceSpec)
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
            .pillBadge()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Image(painter = painterResource(R.drawable.nav_recipes), contentDescription = null, modifier = Modifier.size(20.dp))
        // The count rolls over like a mechanical counter: up for a find, down after a reset.
        AnimatedContent(targetState = unlocked, transitionSpec = counterRoll(LocalReducedMotion.current), label = "counter") { count ->
            Text(
                text = stringResource(R.string.progress, count, AlchemyCatalog.elements.size),
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = TitleFontFamily, fontWeight = FontWeight.Normal),
                color = Gold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

private fun counterRoll(reducedMotion: Boolean): AnimatedContentTransitionScope<Int>.() -> ContentTransform = {
    if (reducedMotion) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        val up = if (targetState > initialState) 1 else -1
        (slideInVertically(tween(COUNTER_ROLL_MILLIS)) { height -> up * height } + fadeIn(tween(COUNTER_ROLL_MILLIS))) togetherWith
            (slideOutVertically(tween(COUNTER_ROLL_MILLIS)) { height -> -up * height } + fadeOut(tween(COUNTER_ROLL_MILLIS)))
    }
}

/** A soft glow of [color] behind a tile's icon, which is the square at the top of the tile. */
private fun DrawScope.drawTileGlow(color: Color, alpha: Float) {
    drawGlow(Offset(size.width / 2, size.width / 2), size.width * TILE_GLOW_SHARE, color, alpha)
}

/** A slow golden pulse behind a tile, marking it as new. */
@Composable
private fun Modifier.freshGlow(): Modifier {
    val reducedMotion = LocalReducedMotion.current
    val pulse = rememberInfiniteTransition(label = "fresh").animateFloat(
        initialValue = FRESH_GLOW_LOW,
        targetValue = FRESH_GLOW_HIGH,
        animationSpec = infiniteRepeatable(tween(FRESH_PULSE_MILLIS), RepeatMode.Reverse),
        label = "freshPulse"
    )
    return drawBehind { drawTileGlow(Gold, if (reducedMotion) FRESH_GLOW_HIGH else pulse.value) }
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
private const val COUNTER_ROLL_MILLIS = 260
private const val PREVIEW_LIFT_SCALE = 1.12f
private const val PREVIEW_GLOW_ALPHA = 0.7f
private const val PREVIEW_GLOW_SHARE = 0.85f
private val PREVIEW_FALLBACK_ICON = 56.dp
private val PreviewLabelShadow = Shadow(color = Color.Black, offset = Offset(0f, 2f), blurRadius = 6f)
private const val TILE_GLOW_SHARE = 0.75f
private const val FRESH_GLOW_LOW = 0.25f
private const val FRESH_GLOW_HIGH = 0.75f
private const val FRESH_PULSE_MILLIS = 1100

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
    fresh: Boolean,
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

    val alpha by animateFloatAsState(if (dimmed) DRAGGED_TILE_ALPHA else 1f, motion(tween(DRAGGED_TILE_FADE_MILLIS)), label = "tileAlpha")
    ElementTile(
        element = element,
        // An element found this session glows until the catalog has shown it.
        modifier = modifier.alpha(alpha).then(if (fresh) Modifier.freshGlow() else Modifier).onGloballyPositioned { coordinates = it }
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

private val ElementSort.labelRes: Int
    get() = when (this) {
        ElementSort.RECENT -> R.string.sort_recent
        ElementSort.ALPHABET -> R.string.sort_alphabet
        ElementSort.GROUP -> R.string.sort_group
    }

private val PALETTE_TITLE_MIN_SIZE = 12.sp
private const val PALETTE_COLUMNS = 5
private const val PALETTE_ROWS = 2
private val PALETTE_GAP = 10.dp
private val PALETTE_END_PADDING = 16.dp
private val PALETTE_VERTICAL_PADDING = 12.dp
