package com.artt.alchemy.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.artt.alchemy.R
import com.artt.alchemy.ui.theme.Gold
import kotlin.math.roundToInt

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
private val WholeHeight = listOf(Segment(0f, 1f, true))

// Where the fill sits inside the track art, in track pixels (see tools/build_ui_assets.py).
private const val PROGRESS_INSET_X = 6f
private const val PROGRESS_INSET_Y = 7f

private val SEARCH_ICON_SPACE = 60.dp

private const val DIALOG_WIDTH_FRACTION = 0.9f
private const val DIALOG_ENTRANCE_MILLIS = 200
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
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .widthIn(min = 96.dp)
            .drawBehind { drawSliced(art, CapSegments, WholeHeight, size.height / art.height) }
            .clickable(role = Role.Button, onClick = onClick)
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
    Image(
        painter = painterResource(if (checked) R.drawable.toggle_on else R.drawable.toggle_off),
        contentDescription = null,
        modifier = modifier
            .size(width = 64.dp, height = 34.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
    )
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

@Composable
fun AlchemyDialog(
    onDismissRequest: () -> Unit,
    @DrawableRes panelRes: Int,
    content: @Composable ColumnScope.() -> Unit
) {
    // A set width instead of the platform's narrow default, so reading text gets long enough lines.
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val art = ImageBitmap.imageResource(panelRes)
        val entrance = remember { Animatable(0f) }
        LaunchedEffect(Unit) { entrance.animateTo(1f, tween(DIALOG_ENTRANCE_MILLIS, easing = FastOutSlowInEasing)) }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer {
                    val value = entrance.value
                    alpha = value
                    scaleX = DIALOG_START_SCALE + (1f - DIALOG_START_SCALE) * value
                    scaleY = scaleX
                }
                .fillMaxWidth(DIALOG_WIDTH_FRACTION)
                .widthIn(max = DIALOG_MAX_WIDTH)
                .drawBehind { drawSliced(art, PanelColumns, PanelRows, minOf(size.width / art.width, size.height / art.height)) }
                // Clears the ornaments on the top and bottom edges of the panel art.
                .padding(start = 32.dp, top = 52.dp, end = 32.dp, bottom = 44.dp),
            content = content
        )
    }
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
    val shown by animateFloatAsState(target, tween(PROGRESS_FILL_MILLIS), label = "progressFill")
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
