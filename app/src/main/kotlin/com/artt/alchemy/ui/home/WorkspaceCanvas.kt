package com.artt.alchemy.ui.home

import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.artt.alchemy.ui.components.AlchemyButton
import com.artt.alchemy.ui.components.AlchemyDialog
import com.artt.alchemy.ui.components.ButtonStyle
import com.artt.alchemy.ui.components.LocalReducedMotion
import com.artt.alchemy.ui.components.elementIconRes
import com.artt.alchemy.ui.components.elementName
import com.artt.alchemy.ui.components.motion
import com.artt.alchemy.ui.theme.LocalAppTheme
import com.artt.alchemy.ui.theme.moteTint
import com.artt.alchemy.ui.theme.themedArt
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay

private const val ITEM_RADIUS_FRACTION = 0.11f
private const val LABEL_SIZE_FRACTION = 0.042f
private const val ICON_SHARE = 0.66f
private const val ICON_TOP_SHARE = 0.85f
private const val LABEL_SHADOW_RADIUS = 6f
private const val WATERMARK_SIZE_FRACTION = 0.85f
private const val WATERMARK_ALPHA = 0.2f
private const val SMOKE_ALPHA = 0.9f
private const val HELD_RING_SHARE = 1.45f

// The ring around the element in hand stays faint, so the target it would mix with is what stands out.
private const val HELD_RING_ALPHA = 0.4f
private const val LEAVING_ALPHA = 0.35f
private const val TARGET_PULSE_MILLIS = 550
private const val LIFT_SCALE = 0.12f
private const val LIFT_SHADOW_ALPHA = 0.35f
private const val TARGET_RING_SHARE = 1.7f
private const val TARGET_GLOW_SHARE = 1.4f
private const val TARGET_GLOW_ALPHA = 0.9f
private const val TARGET_SCALE = 0.08f
private val TargetGlow = Color(0xFFFFD98A)

// Recolours the ring by its brightness, so it keeps its shape (a flat tint would turn it into a blob).
private val TargetRingTint = ColorFilter.colorMatrix(goldFromLuminance(red = 1f, green = 0.8f, blue = 0.36f, boost = 1.7f))

// A tapped element flies in from its palette tile at about the tile's size; a dropped one only settles.
internal const val FLY_START_SCALE = 0.8f
private const val DROP_START_SCALE = 0.92f
private const val SWEEP_ALPHA = 0.35f

// How far inside the edges an element comes to rest, in item radii: its icon and name stay clear of the frame.
private const val REST_SIDE = 0.7f
private const val REST_TOP = 0.9f
private const val REST_BOTTOM = 0.95f
private const val LABEL_EDGE_GAP = 0.1f
private const val FLY_IN_SPEED = 1.4f
private const val LANDED_AT = 1f / FLY_IN_SPEED
private const val SHAKE_WAVES = 3f
private const val SHAKE_AMPLITUDE_SHARE = 0.22f
private const val AURA_ALPHA = 0.9f
private const val STARS_TURN_DEGREES = 40f

// Where the merge plays inside the effect's time: the sources close in first, then the result pops out.
private const val MERGE_SPAN = 0.25f
private const val RESULT_APPEAR_START = 0.2f
private const val RESULT_APPEAR_SPAN = 0.45f

// The scene breathes: the circle turns slowly, items float on the spot, and a discovery wakes the circle up.
private const val CIRCLE_TURN_DEGREES_PER_SECOND = 2.4f
private const val CIRCLE_BREATH_SPEED = 0.7f
private const val CIRCLE_BREATH_ALPHA = 0.025f
private const val CIRCLE_FLARE_ALPHA = 0.45f
private const val BOB_SPEED = 1.7f
private const val BOB_SHARE = 0.045f
private const val HALO_SHARE = 0.95f
private const val HALO_ALPHA = 0.6f
private const val HALO_PULSE = 0.25f

// Sparks thrown when the two sources meet, a little after the effect starts.
private const val BURST_DELAY_MILLIS = 130L
private const val SHAKE_MILLIS = 480
private const val SHAKE_FREQUENCY = 46f
private const val SHAKE_Y_RATIO = 0.8f
private const val SHAKE_Y_SHARE = 0.6f
private val GRAND_SHAKE = 7.dp
private val EPIC_SHAKE = 4.dp

// Discovery rays and the flash that fills the workspace for an epic or legendary find.
private const val RAY_COUNT = 14
private const val RAY_TURN = 0.6f
private const val RAY_LENGTH_SHARE = 5f
private const val RAY_ALPHA = 0.55f
private const val GRAND_FLASH_SPAN = 0.3f
private const val GRAND_FLASH_ALPHA = 0.55f

// Dull and a little rosy: plainly not the light of a mix.
private val NoMatchColors = listOf(Color(0xFFFF5C7C), Color(0xFFE0507A), Color(0xFFB8509A))
private val EmberColors = listOf(Color(0xFFB9BEDD), Color(0xFFFFB27A), Color(0xFF8A90B8))
private val SparkBlue = Color(0xFF8FB8FF)

@Composable
fun WorkspaceCanvas(
    items: List<WorkspaceItem>,
    onMove: (instanceId: Long, position: Offset) -> Unit,
    onResolve: (instanceId: Long, position: Offset) -> Unit,
    onRemove: (instanceId: Long) -> Unit,
    onPickUp: () -> Unit,
    onBoundsChanged: (Rect) -> Unit,
    effect: CombinationEffect?,
    effectTime: () -> Float,
    transitions: () -> List<TransitionFrame>,
    modifier: Modifier = Modifier,
    // The frame around the canvas: still the board, so an element let go on it stays and settles inside.
    frameWidth: Dp = 0.dp
) {
    val currentItems by rememberUpdatedState(items)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnResolve by rememberUpdatedState(onResolve)
    val currentOnPickUp by rememberUpdatedState(onPickUp)
    var heldId by remember { mutableStateOf<Long?>(null) }
    // Where the element in hand is while it is dragged, in workspace fractions on the board. The game hears of the move
    // only when it is let go, so a drag redraws the canvas without recomposing the screen around it.
    var heldAt by remember { mutableStateOf<Offset?>(null) }
    // The finger has left the board: let go now, the element is taken away; brought back, it stays.
    var heldOff by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    val iconIds = (items.map(WorkspaceItem::elementId) + effect?.sources.orEmpty().map { it.elementId }).distinct()
    val icons = iconIds.associateWith { elementId ->
        key(elementId) { ImageBitmap.imageResource(elementIconRes(elementId)) }
    }
    val art = WorkspaceArt(
        icons = icons,
        magicCircle = ImageBitmap.imageResource(themedArt(R.drawable.scene_magic_circle)),
        flash = ImageBitmap.imageResource(themedArt(R.drawable.fx_combine_flash)),
        burst = ImageBitmap.imageResource(R.drawable.fx_success_burst),
        sparkles = ImageBitmap.imageResource(R.drawable.fx_sparkles_gold),
        appearSparkles = ImageBitmap.imageResource(themedArt(R.drawable.fx_sparkles_blue)),
        smoke = ImageBitmap.imageResource(R.drawable.fx_smoke_puff),
        heldRing = ImageBitmap.imageResource(themedArt(R.drawable.fx_selected_ring)),
        energyRing = ImageBitmap.imageResource(themedArt(R.drawable.fx_energy_ring)),
        shockwave = ImageBitmap.imageResource(themedArt(R.drawable.fx_shockwave_ring)),
        purpleSparkles = ImageBitmap.imageResource(R.drawable.fx_sparkles_purple),
        purpleOrb = ImageBitmap.imageResource(R.drawable.fx_glow_purple_orb),
        goldOrb = ImageBitmap.imageResource(R.drawable.fx_glow_gold_orb),
        stars = ImageBitmap.imageResource(R.drawable.fx_stars_cluster)
    )

    // Only one item is in hand at a time; it stays the lifted one while it settles back after being let go.
    val lift = remember { Animatable(0f) }
    var liftedId by remember { mutableStateOf<Long?>(null) }
    val liftSpec = motion(spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    LaunchedEffect(heldId) {
        if (heldId != null) {
            liftedId = heldId
            lift.animateTo(1f, liftSpec)
        } else {
            lift.animateTo(0f, liftSpec)
            liftedId = null
        }
    }
    val animatedPulse by rememberInfiniteTransition(label = "target").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(TARGET_PULSE_MILLIS), RepeatMode.Reverse),
        label = "targetPulse"
    )
    val reducedMotion = LocalReducedMotion.current
    val targetPulse = if (reducedMotion) 1f else animatedPulse

    val moteTint = LocalAppTheme.current.moteTint
    val fx = remember(moteTint) { WorkspaceFx(moteTint) }
    val clock = rememberWorkspaceClock(fx, reducedMotion)
    val shake = remember { Animatable(0f) }

    val labelColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val context = LocalContext.current
    val density = LocalDensity.current
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
        BurstOnMix(effect, fx, shake, Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat()), reducedMotion)
        val shakeAmplitude = with(LocalDensity.current) { (if (effect?.rarity == ElementRarity.LEGENDARY) GRAND_SHAKE else EPIC_SHAKE).toPx() }
        val framePx = with(LocalDensity.current) { frameWidth.toPx() }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("workspace_canvas")
                .onGloballyPositioned { onBoundsChanged(it.boundsInRoot()) }
                // After the bounds are taken, so a jolt never reports the workspace as moved.
                .shaking(shake, shakeAmplitude)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val item = currentItems.asReversed()
                            .minByOrNull { candidate -> distanceSquared(candidate, down.position, size.width.toFloat(), size.height.toFloat()) }
                            ?.takeIf { candidate ->
                                distanceSquared(candidate, down.position, size.width.toFloat(), size.height.toFloat()) <= itemRadiusSquared(size.width, size.height)
                            }
                            ?: return@awaitEachGesture
                        heldId = item.instanceId
                        currentOnPickUp()
                        val width = size.width.toFloat()
                        val height = size.height.toFloat()
                        // Over the frame the element is still on the board; only past it is it taken away.
                        val board = Rect(-framePx / width, -framePx / height, 1f + framePx / width, 1f + framePx / height)
                        val onBoard = { position: Offset -> Offset(position.x.coerceIn(0f, 1f), position.y.coerceIn(0f, 1f)) }
                        val follow = { position: Offset ->
                            val at = normalize(position, size.width, size.height)
                            heldAt = onBoard(at)
                            heldOff = !board.contains(at)
                        }
                        try {
                            var lastPosition = down.position
                            val dragStart = awaitTouchSlopOrCancellation(down.id) { change, _ ->
                                change.consume()
                                lastPosition = change.position
                                follow(lastPosition)
                            } ?: return@awaitEachGesture

                            drag(dragStart.id) { change ->
                                change.consume()
                                lastPosition = change.position
                                follow(lastPosition)
                            }
                            val position = normalize(lastPosition, size.width, size.height)
                            if (board.contains(position)) {
                                // Let go on the board: it settles in from the edge, name and all, instead of hanging over the frame.
                                val labelHalf = labelPaint.measureText(context.resources.elementName(item.elementId)) / 2
                                val resting = restingPosition(onBoard(position), width, height, labelHalf, labelPaint.fontSpacing)
                                currentOnMove(item.instanceId, resting)
                                currentOnResolve(item.instanceId, resting)
                            } else {
                                // Taken away from the edge where it left the board, so it vanishes where it was last seen.
                                currentOnMove(item.instanceId, onBoard(position))
                                currentOnMove(item.instanceId, position)
                            }
                        } finally {
                            heldId = null
                            heldAt = null
                            heldOff = false
                        }
                    }
                }
        ) {
            val seconds = clock.floatValue
            val time = if (effect == null) 0f else effectTime()
            val flare = if (effect?.isDiscovery == true) sin(PI.toFloat() * time) * CIRCLE_FLARE_ALPHA else 0f
            drawWatermark(art.magicCircle, seconds, flare)
            val radius = minOf(size.width, size.height) * ITEM_RADIUS_FRACTION
            val frames = transitions()
            val shown = withHeldAt(currentItems, heldId, heldAt)
            val held = shown.find { it.instanceId == heldId }
            if (!reducedMotion) fx.drawAmbience(this, frames, seconds, radius)
            labelPaint.textSize = workspaceLabelSize(size.width, size.height, density)
            val target = held?.takeUnless { heldOff }?.let { mixTarget(shown, it, labelPaint, context.resources.elementName(it.elementId)) }
            target?.let { target ->
                val iconSize = radius * 2 * ICON_SHARE
                val center = Offset(target.xFraction * size.width, target.yFraction * size.height)
                val iconCenter = iconCenterOf(center, radius, iconSize)
                drawGlow(iconCenter, iconSize * TARGET_GLOW_SHARE, TargetGlow, TARGET_GLOW_ALPHA * targetPulse)
                // The same ring art as the one in hand, but gold, so the two never read as one.
                drawCentered(art.energyRing, iconCenter, iconSize * TARGET_RING_SHARE, targetPulse, TargetRingTint)
            }
            val motion = ItemMotion(
                appearing = frames.filter { it.transition.kind == TransitionKind.APPEAR }.associateBy { it.transition.instanceId },
                shaking = frames.filter { it.transition.kind == TransitionKind.SHAKE }.associate { it.transition.instanceId to it.progress },
                resultInstanceId = effect?.resultInstanceId,
                effectTime = time,
                heldId = heldId,
                liftedId = liftedId,
                lift = lift.value,
                seconds = seconds,
                targetId = target?.instanceId,
                leavingId = heldId.takeIf { heldOff }
            )
            // The item in hand is drawn last, so it never slides under the others.
            shown.sortedBy { it.instanceId == heldId }.forEach { drawItem(it, art, labelPaint, radius, motion, context.resources.elementName(it.elementId)) }
            drawTransitions(frames, art, radius)
            if (!reducedMotion) fx.drawSparks(this, held, radius)
            effect?.let { drawEffect(it, time, art, radius, fx.rayPath) }
        }
        ItemAccessibilityNodes(items, constraints.maxWidth, constraints.maxHeight, onSelect = { selectedId = it }, onRemove = onRemove)
    }
    WorkspaceCombinationDialog(
        selected = items.find { it.instanceId == selectedId },
        items = items,
        onMix = { selected, position ->
            selectedId = null
            // Resolve at the partner's position without moving the source: an invalid pair keeps both as they were.
            onResolve(selected.instanceId, Offset(position.xFraction, position.yFraction))
        },
        onRemove = { id ->
            selectedId = null
            onRemove(id)
        },
        onDismiss = { selectedId = null }
    )
}

/** [items] with the one in hand where the finger has taken it. */
private fun withHeldAt(items: List<WorkspaceItem>, heldId: Long?, at: Offset?): List<WorkspaceItem> = items.map {
    if (it.instanceId == heldId && at != null) it.copy(xFraction = at.x, yFraction = at.y) else it
}

/** What [held] would mix with if let go now: judged where it would come to rest, as the mix itself is. */
private fun DrawScope.mixTarget(items: List<WorkspaceItem>, held: WorkspaceItem, labelPaint: android.graphics.Paint, name: String): WorkspaceItem? {
    val resting = restingPosition(Offset(held.xFraction, held.yFraction), size.width, size.height, labelPaint.measureText(name) / 2, labelPaint.fontSpacing)
    return overlapTarget(items, held.instanceId, resting.x, resting.y)
}

/**
 * One clock for everything alive on the workspace: drifting motes, floating items and sparks, in seconds. The canvas
 * reads it only while drawing, so a tick redraws the workspace without recomposing it. Reduced motion stops it, and so
 * do UI tests, which never run infinite animations.
 */
@Composable
private fun rememberWorkspaceClock(fx: WorkspaceFx, reducedMotion: Boolean): MutableFloatState {
    val clock = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            fx.sparks.clear()
            clock.floatValue = 0f
            return@LaunchedEffect
        }
        var last = withInfiniteAnimationFrameNanos { it }
        while (true) {
            withInfiniteAnimationFrameNanos { now ->
                val step = ((now - last) / NANOS_PER_SECOND).coerceIn(0f, MAX_FRAME_STEP)
                last = now
                fx.sparks.step(step)
                clock.floatValue += step
            }
        }
    }
    return clock
}

/** Throws a mix's sparks the moment its two sources meet, and jolts the workspace for an epic or legendary find. */
@Composable
private fun BurstOnMix(effect: CombinationEffect?, fx: WorkspaceFx, shake: Animatable<Float, AnimationVector1D>, size: Size, reducedMotion: Boolean) {
    LaunchedEffect(effect?.id) {
        val current = effect?.takeUnless { reducedMotion } ?: return@LaunchedEffect
        delay(BURST_DELAY_MILLIS)
        val radius = minOf(size.width, size.height) * ITEM_RADIUS_FRACTION
        val center = iconCenterOf(Offset(current.xFraction * size.width, current.yFraction * size.height), radius, radius * 2 * ICON_SHARE)
        fx.burst(current, center, radius)
        if (current.isDiscovery && current.rarity >= ElementRarity.EPIC) {
            shake.snapTo(1f)
            shake.animateTo(0f, tween(SHAKE_MILLIS, easing = LinearEasing))
        }
    }
}

/** Moves the content in a quick decaying tremor while [shake] runs from 1 down to 0, up to [amplitude] pixels. */
private fun Modifier.shaking(shake: Animatable<Float, AnimationVector1D>, amplitude: Float): Modifier = graphicsLayer {
    val amount = shake.value
    if (amount > 0f) {
        val phase = (1f - amount) * SHAKE_FREQUENCY
        translationX = sin(phase) * amount * amount * amplitude
        translationY = cos(phase * SHAKE_Y_RATIO) * amount * amount * amplitude * SHAKE_Y_SHARE
    }
}

/** One invisible node per item over the canvas, so a screen reader can reach each element and hear its name. */
@Composable
private fun ItemAccessibilityNodes(items: List<WorkspaceItem>, width: Int, height: Int, onSelect: (Long) -> Unit, onRemove: (Long) -> Unit) {
    val mixLabel = stringResource(R.string.workspace_mix)
    val removeLabel = stringResource(R.string.workspace_remove)
    val radius = minOf(width, height) * ITEM_RADIUS_FRACTION
    val side = with(LocalDensity.current) { (radius * 2).toDp() }
    items.forEach { item ->
        key(item.instanceId) {
            val name = elementName(item.elementId)
            Box(
                modifier = Modifier
                    .offset { IntOffset((item.xFraction * width - radius).roundToInt(), (item.yFraction * height - radius).roundToInt()) }
                    .size(side)
                    .testTag("workspace_item")
                    // Semantics actions leave ordinary pointer gestures to the canvas underneath.
                    .semantics {
                        contentDescription = name
                        role = Role.Button
                        onClick(label = mixLabel) {
                            onSelect(item.instanceId)
                            true
                        }
                        customActions = listOf(
                            CustomAccessibilityAction(mixLabel) {
                                onSelect(item.instanceId)
                                true
                            },
                            CustomAccessibilityAction(removeLabel) {
                                onRemove(item.instanceId)
                                true
                            }
                        )
                    }
            )
        }
    }
}

/** A partner picker reachable by screen readers without having to drag an element. */
@Composable
private fun WorkspaceCombinationDialog(
    selected: WorkspaceItem?,
    items: List<WorkspaceItem>,
    onMix: (WorkspaceItem, WorkspaceItem) -> Unit,
    onRemove: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    if (selected == null) return
    // When items overlap, offer the actual target at each position, so a button never mixes a different ingredient.
    val partners = items.filter { it.instanceId != selected.instanceId }
        .mapNotNull { position -> overlapTarget(items, selected.instanceId, position.xFraction, position.yFraction)?.let { it to position } }
        .distinctBy { it.first.instanceId }
    AlchemyDialog(onDismissRequest = onDismiss, panelRes = R.drawable.dialog_blue) {
        Text(stringResource(R.string.workspace_choose_partner, elementName(selected.elementId)), style = MaterialTheme.typography.titleLarge)
        Column(modifier = Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState()).testTag("workspace_combine_dialog")) {
            if (partners.isEmpty()) Text(stringResource(R.string.workspace_no_partner))
            partners.forEach { (partner, position) ->
                AlchemyButton(
                    text = elementName(partner.elementId),
                    style = ButtonStyle.BLUE,
                    onClick = { onMix(selected, position) },
                    modifier = Modifier.fillMaxWidth().testTag("workspace_partner_${partner.instanceId}")
                )
            }
        }
        AlchemyButton(stringResource(R.string.workspace_remove), ButtonStyle.RED, onClick = { onRemove(selected.instanceId) })
        AlchemyButton(stringResource(R.string.cancel), ButtonStyle.DARK, onDismiss)
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
    val lift: Float,
    val seconds: Float,
    // The element the one in hand would mix with if let go now.
    val targetId: Long?,
    // The element in hand while the finger is off the board, fading to show it would be taken away.
    val leavingId: Long?
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

private fun DrawScope.drawItem(item: WorkspaceItem, art: WorkspaceArt, labelPaint: android.graphics.Paint, radius: Float, motion: ItemMotion, name: String) {
    val icon = art.icons[item.elementId] ?: return
    val iconSize = radius * 2 * ICON_SHARE
    var center = Offset(item.xFraction * size.width, item.yFraction * size.height)
    var scale = 1f
    var alpha = 1f
    motion.appearing[item.instanceId]?.let { frame ->
        // A flying element is drawn over the whole screen until it lands (see flightOf), then it is simply here.
        if (frame.origin != null) {
            if (flightOf(frame.progress) != null) return
        } else {
            // A dropped element takes over from the tile in hand at once and only settles.
            scale = lerp(DROP_START_SCALE, 1f, easeOutBack(frame.progress))
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
    // The target rises a little to meet the element in hand.
    if (item.instanceId == motion.targetId) scale *= 1f + TARGET_SCALE
    if (item.instanceId == motion.leavingId) alpha *= LEAVING_ALPHA
    if (alpha <= 0f || scale <= 0f) return
    // Each item floats on its own beat; one in hand is held still.
    val beat = motion.seconds * BOB_SPEED + (item.instanceId % BEAT_SPREAD) * BEAT_STEP
    center += Offset(0f, sin(beat) * radius * BOB_SHARE * (1f - liftAmount))
    val iconCenter = iconCenterOf(center, radius, iconSize)
    // Rare and finer elements glow in their colour, so the board shows what is precious at a glance.
    val rarity = AlchemyCatalog.rarityById.getValue(item.elementId)
    if (rarity >= ElementRarity.RARE) {
        val pulse = 1f - HALO_PULSE + HALO_PULSE * sin(beat * 0.5f)
        drawGlow(iconCenter, iconSize * HALO_SHARE * scale, rarity.glowColor, HALO_ALPHA * alpha * pulse)
    }
    withTransform({ scale(scale, scale, pivot = iconCenter) }) {
        if (liftAmount > 0f) {
            drawOval(
                color = Color.Black.copy(alpha = LIFT_SHADOW_ALPHA * liftAmount),
                topLeft = Offset(iconCenter.x - iconSize * 0.4f, iconCenter.y + iconSize * 0.5f),
                size = Size(iconSize * 0.8f, iconSize * 0.2f)
            )
        }
        if (item.instanceId == motion.heldId) drawCentered(art.heldRing, iconCenter, iconSize * HELD_RING_SHARE, HELD_RING_ALPHA)
        drawImage(
            image = icon,
            dstOffset = IntOffset((center.x - iconSize / 2).roundToInt(), (center.y - radius * ICON_TOP_SHARE).roundToInt()),
            dstSize = IntSize(iconSize.roundToInt(), iconSize.roundToInt()),
            alpha = alpha,
            filterQuality = FilterQuality.Medium
        )
        val halfWidth = labelPaint.measureText(name) / 2f
        val labelBounds = Rect(-halfWidth, labelPaint.ascent(), halfWidth, labelPaint.descent())
        val iconBounds = Rect(center.x - iconSize / 2f, center.y - radius * ICON_TOP_SHARE, center.x + iconSize / 2f, center.y - radius * ICON_TOP_SHARE + iconSize)
        val labelPosition = workspaceLabelPosition(iconBounds, labelBounds, size, scale, radius * LABEL_EDGE_GAP)
        labelPaint.alpha = (alpha * 255).roundToInt()
        drawContext.canvas.nativeCanvas.drawText(name, labelPosition.x, labelPosition.y, labelPaint)
        labelPaint.alpha = 255
    }
}

private fun DrawScope.drawTransitions(frames: List<TransitionFrame>, art: WorkspaceArt, radius: Float) {
    frames.forEach { frame ->
        val (transition, progress) = frame
        val center = Offset(transition.xFraction * size.width, transition.yFraction * size.height)
        val fade = 1f - progress
        when (transition.kind) {
            // A flying element sparkles where it lands, once it has.
            TransitionKind.APPEAR -> if (frame.origin == null || flightOf(progress) == null) {
                val local = if (frame.origin == null) progress else ((progress - LANDED_AT) / (1f - LANDED_AT)).coerceIn(0f, 1f)
                drawCentered(art.appearSparkles, center, radius * (1.8f + 1.2f * local), 1f - local)
            }

            TransitionKind.VANISH -> drawCentered(art.smoke, center, radius * (1.4f + 1.4f * progress), fade * SMOKE_ALPHA)

            TransitionKind.SWEEP -> drawCentered(art.smoke, center, radius * (1f + 0.6f * progress), fade * SWEEP_ALPHA)

            TransitionKind.SHAKE -> Unit
        }
    }
}

private fun DrawScope.drawEffect(effect: CombinationEffect, time: Float, art: WorkspaceArt, radius: Float, rayPath: Path) {
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
        drawDiscovery(effect.rarity, center, time, art, radius, rayPath)
    } else {
        drawCentered(art.energyRing, center, radius * (1.4f + 2.2f * progress), fade)
    }
    drawCentered(art.flash, center, radius * (1.6f + 2f * progress), fade)
}

// A first discovery grows with its rarity: rare adds a shockwave, epic a purple aura, legendary a golden one with turning stars.
private fun DrawScope.drawDiscovery(rarity: ElementRarity, center: Offset, time: Float, art: WorkspaceArt, radius: Float, rayPath: Path) {
    val progress = LinearOutSlowInEasing.transform(time)
    val fade = 1f - progress
    val grand = rarity == ElementRarity.EPIC || rarity == ElementRarity.LEGENDARY
    // The whole workspace lights up for a moment, then the light gathers into rays around the find.
    if (grand) {
        val flash = (1f - time / GRAND_FLASH_SPAN).coerceIn(0f, 1f)
        drawGlow(center, maxOf(size.width, size.height) * 1.2f, rarity.glowColor, GRAND_FLASH_ALPHA * flash * flash)
    }
    val raysIn = (time / MERGE_SPAN).coerceIn(0f, 1f)
    drawRays(center, radius * RAY_LENGTH_SHARE * (0.5f + 0.5f * raysIn), RAY_COUNT, RAY_TURN * time, rarity.glowColor, RAY_ALPHA * raysIn * fade, rayPath)
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

/** A colour matrix that turns every colour into [red], [green], [blue] scaled by its brightness and [boost], keeping alpha. */
private fun goldFromLuminance(red: Float, green: Float, blue: Float, boost: Float): ColorMatrix {
    val lr = 0.3f * boost
    val lg = 0.59f * boost
    val lb = 0.11f * boost
    return ColorMatrix(
        floatArrayOf(
            lr * red, lg * red, lb * red, 0f, 0f,
            lr * green, lg * green, lb * green, 0f, 0f,
            lr * blue, lg * blue, lb * blue, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
}

// Starts gently and arrives gently, so the icon is seen leaving its tile instead of already being half way.
private fun easeInOut(t: Float): Float = t.coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }

private fun easeOutCubic(t: Float): Float = 1f - (1f - t.coerceIn(0f, 1f)).let { it * it * it }

private fun easeInCubic(t: Float): Float = t * t * t

/** The magic circle as a faint watermark in the middle of the workspace, slowly turning and breathing; [flare] brightens it. */
private fun DrawScope.drawWatermark(magicCircle: ImageBitmap, seconds: Float, flare: Float) {
    val alpha = WATERMARK_ALPHA + CIRCLE_BREATH_ALPHA * sin(seconds * CIRCLE_BREATH_SPEED) + flare
    withTransform({ rotate(seconds * CIRCLE_TURN_DEGREES_PER_SECOND, center) }) {
        drawCentered(magicCircle, center, minOf(size.width, size.height) * WATERMARK_SIZE_FRACTION, alpha)
    }
}

private fun DrawScope.drawCentered(image: ImageBitmap, center: Offset, width: Float, alpha: Float, colorFilter: ColorFilter? = null) {
    val height = width * image.height / image.width
    drawImage(
        image = image,
        dstOffset = IntOffset((center.x - width / 2).roundToInt(), (center.y - height / 2).roundToInt()),
        dstSize = IntSize(width.roundToInt(), height.roundToInt()),
        alpha = alpha.coerceIn(0f, 1f),
        colorFilter = colorFilter,
        filterQuality = FilterQuality.Medium
    )
}

private fun normalize(position: Offset, width: Int, height: Int): Offset = Offset(position.x / width, position.y / height)

/**
 * Where an element let go at [position] (workspace fractions) comes to rest, far enough in that its icon and its name,
 * [labelHalfWidth] pixels either side of the centre, clear the edges.
 */
internal fun restingPosition(position: Offset, width: Float, height: Float, labelHalfWidth: Float = 0f, labelHeight: Float = 0f): Offset {
    val radius = minOf(width, height) * ITEM_RADIUS_FRACTION
    val side = (maxOf(radius * REST_SIDE, labelHalfWidth + radius * LABEL_EDGE_GAP) / width).coerceAtMost(HALF)
    val top = (radius * REST_TOP / height).coerceAtMost(HALF)
    val bottom = (maxOf(radius * REST_BOTTOM, radius * (2 * ICON_SHARE - ICON_TOP_SHARE) + labelHeight) / height).coerceAtMost(HALF)
    return Offset(position.x.coerceIn(side, 1f - side), position.y.coerceIn(top, 1f - bottom))
}

/**
 * Where an element comes to rest when dropped with its icon centred on [iconCenter] (pixels in the workspace, possibly
 * over the frame), in workspace fractions; [labelHalfWidth] is half its name's width.
 */
internal fun dropPosition(iconCenter: Offset, width: Float, height: Float, labelHalfWidth: Float, labelHeight: Float = 0f): Offset {
    val radius = minOf(width, height) * ITEM_RADIUS_FRACTION
    val center = Offset(iconCenter.x, iconCenter.y + radius * ICON_TOP_SHARE - radius * ICON_SHARE)
    return restingPosition(Offset((center.x / width).coerceIn(0f, 1f), (center.y / height).coerceIn(0f, 1f)), width, height, labelHalfWidth, labelHeight)
}

/**
 * How far along its flight from the palette an element tapped in is, eased, at this point of its appear transition;
 * null once it has landed.
 */
internal fun flightOf(progress: Float): Float? = (progress * FLY_IN_SPEED).takeIf { it < 1f }?.let(::easeInOut)

/** The centre of an element's icon at these workspace fractions, in workspace pixels. */
internal fun iconCenterAt(xFraction: Float, yFraction: Float, width: Float, height: Float): Offset {
    val radius = minOf(width, height) * ITEM_RADIUS_FRACTION
    return iconCenterOf(Offset(xFraction * width, yFraction * height), radius, radius * 2 * ICON_SHARE)
}

/** The side of an element's icon on a workspace of this size, in pixels. */
internal fun workspaceIconSize(width: Float, height: Float): Float = minOf(width, height) * ITEM_RADIUS_FRACTION * 2 * ICON_SHARE

/** The size of an element's name on a workspace of this size, in pixels. */
internal fun workspaceLabelSize(width: Float, height: Float, density: Density = Density(1f)): Float = with(density) {
    maxOf(12f, minOf(width, height) * LABEL_SIZE_FRACTION / density.density).sp.toPx()
}

/** Coordinates of a label baseline before the item's visual scale is applied. */
internal fun workspaceLabelPosition(icon: Rect, label: Rect, board: Size, scale: Float = 1f, gap: Float = 0f): Offset {
    val pivot = icon.center
    // Text is drawn inside the same scale transform as the icon; keep its transformed bounds on the board.
    val bounds = Rect(
        pivot.x - pivot.x / scale,
        pivot.y - pivot.y / scale,
        pivot.x + (board.width - pivot.x) / scale,
        pivot.y + (board.height - pivot.y) / scale
    )
    val below = icon.bottom - label.top
    val baseline = if (below + label.bottom <= bounds.bottom) below else icon.top - label.bottom - gap
    val minX = (bounds.left - label.left).coerceAtMost(bounds.center.x)
    val maxX = (bounds.right - label.right).coerceAtLeast(bounds.center.x)
    val minY = (bounds.top - label.top).coerceAtMost(bounds.center.y)
    val maxY = (bounds.bottom - label.bottom).coerceAtLeast(bounds.center.y)
    return Offset(pivot.x.coerceIn(minX, maxX), baseline.coerceIn(minY, maxY))
}

private fun distanceSquared(item: WorkspaceItem, position: Offset, width: Float, height: Float): Float {
    val dx = item.xFraction * width - position.x
    val dy = item.yFraction * height - position.y
    return dx * dx + dy * dy
}

private fun itemRadiusSquared(width: Int, height: Int): Float = minOf(width, height).let { it * ITEM_RADIUS_FRACTION }.let { it * it }

private const val LABEL_WEIGHT = 700
private const val HALF = 0.5f
private const val NANOS_PER_SECOND = 1_000_000_000f

// A long pause (the app in the background, a dropped frame) moves sparks at most this far in one step.
private const val MAX_FRAME_STEP = 0.05f

// Neighbouring items float out of step with each other.
private const val BEAT_SPREAD = 7L
private const val BEAT_STEP = 0.9f

/** What lives on the workspace between the game's events: its sparks, motes and the reusable ray geometry. */
private class WorkspaceFx(moteTint: Color) {
    val sparks = Sparks()
    val motes = Motes(moteTint)
    val rayPath = Path()

    // Refusals and removals already given their sparks, so a transition throws them once.
    private var sparked = emptySet<Pair<Long, TransitionKind>>()

    /** The sparks of a mix: more, faster and longer-lived the rarer its result, and always more for a discovery. */
    fun burst(effect: CombinationEffect, center: Offset, radius: Float) {
        val colors = effect.rarity.sparkColors
        if (!effect.isDiscovery) {
            sparks.burst(center, radius, count = 14, speed = 5f, colors = colors, life = 0.55f)
            return
        }
        val tier = effect.rarity.ordinal
        sparks.burst(center, radius, count = 22 + tier * 8, speed = 6.5f + tier, colors = colors, life = 0.8f + tier * 0.1f, gravity = 1.2f)
        if (effect.rarity >= ElementRarity.EPIC) {
            // A second, slower cloud that hangs in the air like dust.
            sparks.burst(center, radius, count = 30, speed = 2.5f, colors = colors, life = 1.4f, gravity = -0.3f)
        }
    }

    /** The motes behind the items, and the sparks for any refusal or removal that has just begun. */
    fun drawAmbience(scope: DrawScope, frames: List<TransitionFrame>, seconds: Float, radius: Float) {
        motes.draw(scope, seconds, radius)
        emitFor(frames, scope.size, radius)
    }

    /** The sparks over the items, fed by a trail behind the element in hand, which glows in its rarity's colour. */
    fun drawSparks(scope: DrawScope, held: WorkspaceItem?, radius: Float) {
        val trail = held?.let { Offset(it.xFraction * scope.size.width, it.yFraction * scope.size.height) }
        val color = held?.let { AlchemyCatalog.rarityById.getValue(it.elementId).glowColor } ?: SparkBlue
        sparks.trail(trail?.let { iconCenterOf(it, radius, radius * 2 * ICON_SHARE) }, radius, color)
        sparks.draw(scope)
    }

    /** Throws a fizzle of dull sparks when a mix is refused and a puff of embers when an item is swept away. */
    private fun emitFor(frames: List<TransitionFrame>, size: Size, radius: Float) {
        val current = frames.mapTo(HashSet()) { it.transition.instanceId to it.transition.kind }
        frames.forEach { frame ->
            val transition = frame.transition
            if ((transition.instanceId to transition.kind) in sparked) return@forEach
            val at = Offset(transition.xFraction * size.width, transition.yFraction * size.height)
            when (transition.kind) {
                TransitionKind.SHAKE -> sparks.burst(iconCenterOf(at, radius, radius * 2 * ICON_SHARE), radius, count = 12, speed = 4f, colors = NoMatchColors, life = 0.55f, gravity = 3f)
                TransitionKind.VANISH -> sparks.burst(at, radius, count = 9, speed = 2.2f, colors = EmberColors, life = 0.7f, gravity = -1.5f)
                TransitionKind.APPEAR, TransitionKind.SWEEP -> Unit
            }
        }
        sparked = current
    }
}
