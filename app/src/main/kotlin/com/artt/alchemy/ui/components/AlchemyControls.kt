package com.artt.alchemy.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.artt.alchemy.R
import com.artt.alchemy.ui.theme.Gold
import kotlin.math.roundToInt

private const val PRESS_MILLIS = 90
private const val PRESSED_SCALE = 0.96f
private const val PRESSED_ALPHA = 0.85f

/** A source range of the art; fixed segments keep their proportions, stretched ones absorb the rest. */
private data class Segment(val start: Float, val end: Float, val stretch: Boolean)

// Rounded ends stay intact and the middle stretches, for buttons, tabs and fields.
private val CapSegments = listOf(Segment(0f, 0.25f, false), Segment(0.25f, 0.75f, true), Segment(0.75f, 1f, false))

// Panels keep their corners and the ornament in the middle of the top and bottom edges.
private val PanelColumns = listOf(
    Segment(0f, 0.3f, false),
    Segment(0.3f, 0.4f, true),
    Segment(0.4f, 0.6f, false),
    Segment(0.6f, 0.7f, true),
    Segment(0.7f, 1f, false)
)
private val PanelRows = listOf(Segment(0f, 0.3f, false), Segment(0.3f, 0.7f, true), Segment(0.7f, 1f, false))

// The search field art has the magnifier and a divider drawn into its left end, so that end stays whole.
private val SearchFieldSegments = listOf(Segment(0f, 0.33f, false), Segment(0.33f, 0.75f, true), Segment(0.75f, 1f, false))
private val RowSegments = listOf(Segment(0f, 0.15f, false), Segment(0.15f, 0.85f, true), Segment(0.85f, 1f, false))

// The chevron is drawn into the right end of the dropdown art, so that end stays whole.
private val DropdownSegments = listOf(Segment(0f, 0.15f, false), Segment(0.15f, 0.72f, true), Segment(0.72f, 1f, false))
private val WholeHeight = listOf(Segment(0f, 1f, true))

// The bubble's tail sits in the middle of the bottom edge and stays whole with the lower corners.
private val BubbleColumns = listOf(
    Segment(0f, 0.2f, false),
    Segment(0.2f, 0.42f, true),
    Segment(0.42f, 0.58f, false),
    Segment(0.58f, 0.8f, true),
    Segment(0.8f, 1f, false)
)
private val BubbleRows = listOf(Segment(0f, 0.25f, false), Segment(0.25f, 0.6f, true), Segment(0.6f, 1f, false))

// The bubble art is about three times as large as it should be drawn, so its corners and tail stay small.
private const val BUBBLE_ART_DENSITY = 3f

// Where the fill sits inside the track art, in track pixels (see tools/build_ui_assets.py).
private const val PROGRESS_INSET_X = 6f
private const val PROGRESS_INSET_Y = 7f

private val SEARCH_ICON_SPACE = 60.dp
private val DROPDOWN_HEIGHT = 40.dp
private val SLIDER_HEIGHT = 36.dp
private const val SLIDER_TRACK_SHARE = 0.5f
private val DROPDOWN_CHEVRON_SPACE = 34.dp

private const val DIALOG_WIDTH_FRACTION = 0.9f
private const val DIALOG_DAMPING = 0.68f
private const val PROGRESS_FILL_MILLIS = 600
private const val DIALOG_START_SCALE = 0.9f
private val DIALOG_MAX_WIDTH = 480.dp

enum class ButtonStyle(@param:DrawableRes val res: Int, val textColor: Color) {
    BLUE(R.drawable.btn_blue, Color.White),
    GOLD(R.drawable.btn_gold, Color.White),
    RED(R.drawable.btn_red, Color.White),
    DARK(R.drawable.btn_dark, Color.White)
}

@Composable
fun AlchemyButton(text: String, style: ButtonStyle, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val art = ImageBitmap.imageResource(style.res)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, motion(tween(PRESS_MILLIS)), label = "buttonPress")
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .widthIn(min = 96.dp)
            // The press follows the art's own shape; the platform ripple would be a plain rectangle.
            .graphicsLayer {
                val scale = 1f - (1f - PRESSED_SCALE) * press
                scaleX = scale
                scaleY = scale
                alpha = 1f - (1f - PRESSED_ALPHA) * press
            }
            .drawBehind { drawSliced(art, CapSegments, WholeHeight, size.height / art.height) }
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = style.textColor)
    }
}

@Composable
fun AlchemyTab(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val art = ImageBitmap.imageResource(if (selected) R.drawable.tab_active else R.drawable.tab_inactive)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .drawBehind { drawSliced(art, CapSegments, WholeHeight, size.height / art.height) }
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AlchemyToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 64.dp, height = 34.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
    ) {
        Crossfade(targetState = checked, animationSpec = motion(tween(PRESS_MILLIS * 2)), label = "toggle") { on ->
            Image(
                painter = painterResource(if (on) R.drawable.toggle_on else R.drawable.toggle_off),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** Single-line Material text field drawn over the search field art, which has the magnifier drawn in. */
@Composable
fun AlchemySearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val art = ImageBitmap.imageResource(R.drawable.field_search)
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        // Keeps the text clear of the drawn magnifier.
        leadingIcon = { Spacer(Modifier.width(SEARCH_ICON_SPACE)) },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = modifier.drawBehind { drawSliced(art, SearchFieldSegments, WholeHeight, size.height / art.height) }
    )
}

/** A closed field showing the [selected] option that opens a menu of all [options]; tags are `<tag>` and `<tag>_<option name>`. */
@Composable
fun <T : Enum<T>> AlchemyDropdown(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null
) {
    val art = ImageBitmap.imageResource(R.drawable.field_dropdown)
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(DROPDOWN_HEIGHT)
                .drawBehind { drawSliced(art, DropdownSegments, WholeHeight, size.height / art.height) }
                .clickable(role = Role.DropdownList) { expanded = true }
                .padding(start = 10.dp, end = DROPDOWN_CHEVRON_SPACE)
                .testTag(tag)
        ) {
            iconRes?.let {
                Image(painterResource(it), contentDescription = null, modifier = Modifier.size(20.dp).padding(end = 4.dp))
            }
            Text(label(selected), style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(label(option), color = if (option == selected) Gold else Color.Unspecified) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                    modifier = Modifier.testTag("${tag}_${option.name.lowercase()}")
                )
            }
        }
    }
}

@Composable
fun AlchemyDialog(
    onDismissRequest: () -> Unit,
    @DrawableRes panelRes: Int,
    sidePadding: Dp = DIALOG_SIDE_PADDING,
    widthFraction: Float = DIALOG_WIDTH_FRACTION,
    // A set height keeps every card the same size; its content scrolls inside.
    height: Dp? = null,
    // Shows a close cross in the top right corner.
    onClose: (() -> Unit)? = null,
    // Shows a back arrow in the top left corner.
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    // A set width instead of the platform's narrow default, so reading text gets long enough lines.
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val art = ImageBitmap.imageResource(panelRes)
        val entrance = remember { Animatable(0f) }
        // A quick pop with a touch of overshoot, like a card laid down with a flourish.
        val entranceSpec = motion(spring<Float>(dampingRatio = DIALOG_DAMPING, stiffness = Spring.StiffnessMediumLow))
        LaunchedEffect(Unit) { entrance.animateTo(1f, entranceSpec) }
        Box(
            modifier = Modifier
                .graphicsLayer {
                    val value = entrance.value
                    alpha = value.coerceIn(0f, 1f)
                    scaleX = DIALOG_START_SCALE + (1f - DIALOG_START_SCALE) * value
                    scaleY = scaleX
                }
                .fillMaxWidth(widthFraction)
                .widthIn(max = DIALOG_MAX_WIDTH)
                .then(if (height != null) Modifier.height(height) else Modifier)
                .drawBehind { drawSliced(art, PanelColumns, PanelRows, minOf(size.width / art.width, size.height / art.height)) }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                // Clears the ornaments on the top and bottom edges of the panel art.
                modifier = Modifier.padding(start = sidePadding, top = 52.dp, end = sidePadding, bottom = 44.dp),
                content = content
            )
            if (onClose != null) {
                AlchemyIconButton(
                    icon = R.drawable.ic_close,
                    contentDescription = stringResource(R.string.close),
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = CLOSE_TOP_INSET, end = CLOSE_END_INSET).testTag("dialog_close"),
                    size = CLOSE_SIZE
                )
            }
            if (onBack != null) {
                AlchemyIconButton(
                    icon = R.drawable.ic_back,
                    contentDescription = stringResource(R.string.back),
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.TopStart).padding(top = CLOSE_TOP_INSET, start = CLOSE_END_INSET).testTag("dialog_back"),
                    size = CLOSE_SIZE
                )
            }
        }
    }
}

/** A round icon standing in for a word: close, back, check, info. It always carries a spoken description. */
@Composable
fun AlchemyIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = ICON_BUTTON_SIZE
) {
    Image(
        painter = painterResource(icon),
        contentDescription = contentDescription,
        modifier = modifier.size(size).clip(CircleShape).clickable(role = Role.Button, onClick = onClick)
    )
}

/** The bar art: an empty track with a glowing fill that grows with [progress] from 0 to 1. */
@Composable
fun AlchemyProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val track = ImageBitmap.imageResource(R.drawable.progress_track)
    val fill = ImageBitmap.imageResource(R.drawable.progress_fill)
    val fraction = progress.coerceIn(0f, 1f)
    // Starts empty so the fill grows to its value when the bar first appears, then follows changes.
    var target by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(fraction) { target = fraction }
    val shown by animateFloatAsState(target, motion(tween(PROGRESS_FILL_MILLIS)), label = "progressFill")
    Box(
        modifier = modifier
            .height(16.dp)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f) }
            .drawBehind {
                val scale = size.height / track.height
                drawSliced(track, CapSegments, WholeHeight, scale)
                val insetX = PROGRESS_INSET_X * scale
                val insetY = PROGRESS_INSET_Y * scale
                val fillWidth = (size.width - 2 * insetX) * shown
                if (fillWidth >= 1f) {
                    val fillHeight = size.height - 2 * insetY
                    drawSliced(
                        fill,
                        CapSegments,
                        WholeHeight,
                        fillHeight / fill.height,
                        topLeft = Offset(insetX, insetY),
                        target = Size(fillWidth, fillHeight)
                    )
                }
            }
    )
}

/** A volume-style slider on the bar art: [value] from 0 to 1, set by tapping or dragging, or by an accessibility service. */
@Composable
fun AlchemySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: () -> Unit = {}
) {
    val track = ImageBitmap.imageResource(R.drawable.progress_track)
    val fill = ImageBitmap.imageResource(R.drawable.progress_fill)
    val knob = ImageBitmap.imageResource(R.drawable.slider_knob)
    val fraction = value.coerceIn(0f, 1f)
    val currentChange by rememberUpdatedState(onValueChange)
    val currentFinished by rememberUpdatedState(onValueChangeFinished)
    Box(
        modifier = modifier
            .height(SLIDER_HEIGHT)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                setProgress {
                    currentChange(it.coerceIn(0f, 1f))
                    currentFinished()
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures {
                    currentChange(sliderValueAt(it.x, size.width.toFloat(), size.height / 2f))
                    currentFinished()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(onDragEnd = { currentFinished() }, onDragCancel = { currentFinished() }) { change, _ ->
                    change.consume()
                    currentChange(sliderValueAt(change.position.x, size.width.toFloat(), size.height / 2f))
                }
            }
            .drawBehind {
                val knobRadius = size.height / 2f
                val trackHeight = size.height * SLIDER_TRACK_SHARE
                val trackTop = (size.height - trackHeight) / 2f
                val scale = trackHeight / track.height
                drawSliced(track, CapSegments, WholeHeight, scale, topLeft = Offset(0f, trackTop), target = Size(size.width, trackHeight))
                val knobCenterX = knobRadius + (size.width - 2 * knobRadius) * fraction
                val insetX = PROGRESS_INSET_X * scale
                val insetY = PROGRESS_INSET_Y * scale
                val fillWidth = knobCenterX - insetX
                if (fillWidth >= 1f) {
                    val fillHeight = trackHeight - 2 * insetY
                    drawSliced(
                        fill,
                        CapSegments,
                        WholeHeight,
                        fillHeight / fill.height,
                        topLeft = Offset(insetX, trackTop + insetY),
                        target = Size(fillWidth, fillHeight)
                    )
                }
                val knobSize = (knobRadius * 2).roundToInt()
                drawImage(
                    image = knob,
                    dstOffset = IntOffset((knobCenterX - knobRadius).roundToInt(), 0),
                    dstSize = IntSize(knobSize, knobSize),
                    filterQuality = FilterQuality.Medium
                )
            }
    )
}

private fun sliderValueAt(x: Float, width: Float, knobRadius: Float): Float {
    val travel = width - 2 * knobRadius
    return if (travel <= 0f) 0f else ((x - knobRadius) / travel).coerceIn(0f, 1f)
}

/** Screen title on the ribbon banner, centered at the top of a screen. */
@Composable
fun ScreenBanner(title: String, modifier: Modifier = Modifier) {
    val art = ImageBitmap.imageResource(R.drawable.banner_wide)
    Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxWidth()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .defaultMinSize(minWidth = 220.dp, minHeight = 60.dp)
                .drawBehind { drawSliced(art, PanelColumns, WholeHeight, size.height / art.height) }
                .semantics(mergeDescendants = true) { heading() }
                // Keeps the text on the ribbon body, clear of the tails and the lower fold.
                .padding(start = 44.dp, top = 10.dp, end = 44.dp, bottom = 16.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = Gold, maxLines = 1)
        }
    }
}

/** Panel art stretched to the modifier's bounds, keeping corners and edge ornaments intact. */
@Composable
fun Modifier.panelBackground(@DrawableRes res: Int, alpha: Float = 1f, maxScale: Float = Float.MAX_VALUE): Modifier {
    val art = ImageBitmap.imageResource(res)
    return drawBehind {
        drawSliced(art, PanelColumns, PanelRows, minOf(size.width / art.width, size.height / art.height, maxScale), alpha)
    }
}

/** A speech bubble with its tail at the bottom centre; leave room for the tail below the content. */
@Composable
fun Modifier.tooltipBackground(): Modifier {
    val art = ImageBitmap.imageResource(R.drawable.tooltip_bubble)
    return drawBehind { drawSliced(art, BubbleColumns, BubbleRows, density / BUBBLE_ART_DENSITY) }
}

/** The green pill behind a counter; its height follows the content so the rim keeps its thickness. */
@Composable
fun Modifier.pillBadge(): Modifier {
    val art = ImageBitmap.imageResource(R.drawable.pill_badge)
    return drawBehind { drawSliced(art, CapSegments, WholeHeight, size.height / art.height) }
}

/** A single-line row panel: the art's height follows the row, so the trim keeps its thickness. */
@Composable
fun Modifier.rowPanel(): Modifier {
    val art = ImageBitmap.imageResource(R.drawable.field_row)
    return drawBehind { drawSliced(art, RowSegments, WholeHeight, size.height / art.height) }
}

private fun DrawScope.drawSliced(
    image: ImageBitmap,
    columns: List<Segment>,
    rows: List<Segment>,
    fixedScale: Float,
    alpha: Float = 1f,
    topLeft: Offset = Offset.Zero,
    target: Size = size
) {
    val xs = layout(columns, image.width, target.width, fixedScale).map { it + topLeft.x }
    val ys = layout(rows, image.height, target.height, fixedScale).map { it + topLeft.y }
    columns.forEachIndexed { column, horizontal ->
        rows.forEachIndexed { row, vertical ->
            val srcX = (horizontal.start * image.width).roundToInt()
            val srcY = (vertical.start * image.height).roundToInt()
            val srcWidth = (horizontal.end * image.width).roundToInt() - srcX
            val srcHeight = (vertical.end * image.height).roundToInt() - srcY
            val dstX = xs[column].roundToInt()
            val dstY = ys[row].roundToInt()
            drawImage(
                image = image,
                srcOffset = IntOffset(srcX, srcY),
                srcSize = IntSize(srcWidth, srcHeight),
                dstOffset = IntOffset(dstX, dstY),
                dstSize = IntSize(xs[column + 1].roundToInt() - dstX, ys[row + 1].roundToInt() - dstY),
                alpha = alpha,
                filterQuality = FilterQuality.Medium
            )
        }
    }
}

/** Destination edges of each segment; fixed parts shrink uniformly when the target is too small. */
private fun layout(segments: List<Segment>, sourceLength: Int, targetLength: Float, fixedScale: Float): List<Float> {
    val fixed = segments.filterNot(Segment::stretch).sumOf { (it.end - it.start).toDouble() }.toFloat() * sourceLength
    val scale = minOf(fixedScale, if (fixed > 0f) targetLength / fixed else fixedScale)
    val stretchSource = segments.filter(Segment::stretch).sumOf { (it.end - it.start).toDouble() }.toFloat()
    val stretchTarget = targetLength - fixed * scale
    return segments.runningFold(0f) { position, segment ->
        val length = segment.end - segment.start
        position + if (segment.stretch) stretchTarget * length / stretchSource else length * sourceLength * scale
    }
}

private val DIALOG_SIDE_PADDING = 32.dp

/** Distance from the screen edge (and from the top of the content area) to a screen's content, the same everywhere. */
val ScreenPadding = 4.dp
private val CLOSE_SIZE = 36.dp
private val ICON_BUTTON_SIZE = 44.dp

// Below the corner ornament and level with the top of the card's content.
private val CLOSE_TOP_INSET = 40.dp
private val CLOSE_END_INSET = 14.dp
