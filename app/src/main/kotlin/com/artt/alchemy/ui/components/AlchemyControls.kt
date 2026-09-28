package com.artt.alchemy.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.artt.alchemy.R
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
private val WholeHeight = listOf(Segment(0f, 1f, true))

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

/** Single-line Material text field drawn over the field art, with the search icon in front. */
@Composable
fun AlchemySearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val art = ImageBitmap.imageResource(R.drawable.field)
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Image(painterResource(R.drawable.ic_search), contentDescription = null, modifier = Modifier.size(24.dp)) },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = modifier.drawBehind { drawSliced(art, CapSegments, WholeHeight, size.height / art.height) }
    )
}

@Composable
fun AlchemyDialog(
    onDismissRequest: () -> Unit,
    @DrawableRes panelRes: Int,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        val art = ImageBitmap.imageResource(panelRes)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .drawBehind { drawSliced(art, PanelColumns, PanelRows, minOf(size.width / art.width, size.height / art.height)) }
                // Clears the ornaments on the top and bottom edges of the panel art.
                .padding(start = 32.dp, top = 52.dp, end = 32.dp, bottom = 44.dp),
            content = content
        )
    }
}

private fun DrawScope.drawSliced(image: ImageBitmap, columns: List<Segment>, rows: List<Segment>, fixedScale: Float) {
    val xs = layout(columns, image.width, size.width, fixedScale)
    val ys = layout(rows, image.height, size.height, fixedScale)
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
