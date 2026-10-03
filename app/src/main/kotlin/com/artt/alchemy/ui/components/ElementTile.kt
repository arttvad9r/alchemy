package com.artt.alchemy.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artt.alchemy.R
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.ElementDefinition
import com.artt.alchemy.game.ElementRarity
import com.artt.alchemy.ui.theme.Gold
import com.artt.alchemy.ui.theme.color

private val FinalMarkEdge = Color(0xFF3B2A06)

// Share of the frame taken by its ornamental border on each side.
private const val FRAME_INSET = 0.16f

val ElementDefinition.rarity: ElementRarity
    get() = AlchemyCatalog.rarityById.getValue(id)

@Composable
fun ElementIcon(element: ElementDefinition, modifier: Modifier = Modifier, silhouette: Boolean = false) {
    Image(
        painter = painterResource(elementIconRes(element.id)),
        contentDescription = null,
        // Locked elements take the theme's outline colour, so they match the card they sit on.
        colorFilter = if (silhouette) ColorFilter.tint(MaterialTheme.colorScheme.outline) else null,
        modifier = modifier.aspectRatio(1f)
    )
}

/** Square rarity frame; [rarity] null draws the neutral frame used for locked elements. */
@Composable
fun ElementFrame(rarity: ElementRarity?, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.aspectRatio(1f)) {
        Image(
            painter = painterResource(rarity.frameRes),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize().then(if (rarity == null) Modifier.alpha(0.7f) else Modifier)
        )
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize(1f - 2 * FRAME_INSET), content = content)
    }
}

@Composable
fun FramedElementIcon(element: ElementDefinition, modifier: Modifier = Modifier, locked: Boolean = false) {
    ElementFrame(rarity = if (locked) null else element.rarity, modifier = modifier) {
        ElementIcon(element, Modifier.fillMaxSize(), silhouette = locked)
    }
}

@Composable
fun RarityBadge(rarity: ElementRarity, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(rarity.labelRes),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        maxLines = 1,
        modifier = modifier
            .background(rarity.color.copy(alpha = 0.55f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 1.dp)
    )
}

/** Marks an open element that no recipe uses: the same pill as the rarity, led by the final mark. */
@Composable
fun FinalBadge(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 1.dp)
    ) {
        FinalMark(Modifier.size(8.dp))
        Text(text = stringResource(R.string.element_final), style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1)
    }
}

/** A small gold diamond: the sign of a final element wherever there is no room for the word. */
@Composable
fun FinalMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .rotate(45f)
            .background(Gold, RoundedCornerShape(2.dp))
            .border(1.dp, FinalMarkEdge, RoundedCornerShape(2.dp))
    )
}

@Composable
fun ElementTile(element: ElementDefinition, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val name = elementName(element.id)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ElementTextGap),
        modifier = modifier
            .semantics { contentDescription = name }
            .then(if (onClick != null) Modifier.pressClickable(onClick = onClick) else Modifier)
    ) {
        FramedElementIcon(element, Modifier.fillMaxWidth())
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = if (LocalDensity.current.fontScale >= 1.5f) 2 else 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Space between an element's icon and its name, in every tile and card. */
val ElementTextGap = 4.dp

private val ElementRarity?.frameRes: Int
    get() = when (this) {
        null, ElementRarity.BASE -> R.drawable.frame_base
        ElementRarity.COMMON -> R.drawable.frame_common
        ElementRarity.RARE -> R.drawable.frame_rare
        ElementRarity.EPIC -> R.drawable.frame_epic
        ElementRarity.LEGENDARY -> R.drawable.frame_legendary
    }

val ElementRarity.labelRes: Int
    get() = when (this) {
        ElementRarity.BASE -> R.string.rarity_base
        ElementRarity.COMMON -> R.string.rarity_common
        ElementRarity.RARE -> R.string.rarity_rare
        ElementRarity.EPIC -> R.string.rarity_epic
        ElementRarity.LEGENDARY -> R.string.rarity_legendary
    }

/**
 * An element's fact set as a short piece of reading text: body size, medium weight, single line spacing,
 * no hyphenation.
 */
@Composable
fun FactText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = remember(text) { withTypographicBinding(text) },
        style = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.Medium,
            // Single spacing: the font's own line height.
            lineHeight = TextUnit.Unspecified,
            lineBreak = LineBreak.Paragraph
        ),
        fontSize = FACT_FONT_SIZE,
        color = MaterialTheme.colorScheme.onSurface,
        // Centred like the rest of the cards; justifying is ignored on some phones and leaves wide gaps on narrow ones.
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

private val FACT_FONT_SIZE = 15.sp

private const val NO_BREAK_SPACE = '\u00A0'
private const val WORD_JOINER = '\u2060'
private val NUMBER_BEFORE_WORD = Regex("""(\d) """)

/**
 * Russian typesetting rules for line breaks: a number stays with the word after it ("408 км/ч"),
 * a dash never starts a line, and a unit like "км/ч" is not split at its slash.
 */
internal fun withTypographicBinding(text: String): String = text
    .replace(NUMBER_BEFORE_WORD, "$1$NO_BREAK_SPACE")
    .replace(" —", "$NO_BREAK_SPACE—")
    .replace("/", "/$WORD_JOINER")
