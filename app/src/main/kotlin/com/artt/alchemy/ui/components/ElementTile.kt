package com.artt.alchemy.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artt.alchemy.R
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.ElementDefinition
import com.artt.alchemy.game.ElementRarity
import com.artt.alchemy.ui.theme.color

// Share of the frame taken by its ornamental border on each side.
private const val FRAME_INSET = 0.16f

val ElementDefinition.rarity: ElementRarity
    get() = AlchemyCatalog.rarityById.getValue(id)

@Composable
fun ElementIcon(element: ElementDefinition, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(elementIconRes(element.id)),
        contentDescription = null,
        modifier = modifier.aspectRatio(1f)
    )
}

/** Square frame in the colors of [rarity]. */
@Composable
fun ElementFrame(rarity: ElementRarity, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.aspectRatio(1f)) {
        Image(
            painter = painterResource(rarity.frameRes),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize(1f - 2 * FRAME_INSET), content = content)
    }
}

/** The element in its rarity frame, or the question mark panel while it is [locked], which gives nothing away. */
@Composable
fun FramedElementIcon(element: ElementDefinition, modifier: Modifier = Modifier, locked: Boolean = false) {
    if (locked) {
        Image(
            painter = painterResource(R.drawable.element_unknown),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = modifier.aspectRatio(1f)
        )
    } else {
        ElementFrame(rarity = element.rarity, modifier = modifier) {
            ElementIcon(element, Modifier.fillMaxSize())
        }
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

@Composable
fun ElementTile(element: ElementDefinition, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .semantics { contentDescription = element.name }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        FramedElementIcon(element, Modifier.fillMaxWidth())
        Text(
            text = element.name,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private val ElementRarity.frameRes: Int
    get() = when (this) {
        ElementRarity.BASE -> R.drawable.frame_base
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
