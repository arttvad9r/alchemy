package com.artt.alchemy.ui.elements

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artt.alchemy.R
import com.artt.alchemy.data.PlayerProgress
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.ElementDefinition
import com.artt.alchemy.game.ElementLinks
import com.artt.alchemy.game.Recipe
import com.artt.alchemy.game.elementFacts
import com.artt.alchemy.game.partnerOf
import com.artt.alchemy.game.recipeKey
import com.artt.alchemy.ui.components.AlchemyDialog
import com.artt.alchemy.ui.components.ElementTile
import com.artt.alchemy.ui.components.FactText
import com.artt.alchemy.ui.components.FinalBadge
import com.artt.alchemy.ui.components.FramedElementIcon
import com.artt.alchemy.ui.components.RarityBadge
import com.artt.alchemy.ui.components.WholeWordsAutoSize
import com.artt.alchemy.ui.components.rarity
import com.artt.alchemy.ui.theme.Gold

/** An open element up close: its fact, the recipe it came from and the known recipes it is part of. */
@Composable
fun ElementDetailsDialog(
    element: ElementDefinition,
    progress: PlayerProgress,
    onOpenElement: (ElementDefinition) -> Unit,
    onDismiss: () -> Unit
) {
    val madeFrom = ElementLinks.recipesByResult[element.id].orEmpty().filter { it.isKnown(progress) }
    val allUses = ElementLinks.recipesByIngredient[element.id].orEmpty()
    val knownUses = allUses.filter { it.isKnown(progress) }
    // A link swaps the element in place, so each one starts from the top.
    val scroll = remember(element.id) { ScrollState(0) }
    AlchemyDialog(
        onDismissRequest = onDismiss,
        panelRes = R.drawable.dialog_blue,
        sidePadding = CARD_SIDE_PADDING,
        widthFraction = CARD_WIDTH_FRACTION,
        height = CARD_HEIGHT,
        onClose = onDismiss
    ) {
        Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).testTag("element_details")) {
            // Centred, with equal room on both sides so the close cross never covers a long name.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = HEADER_SIDE_GAP)
            ) {
                FramedElementIcon(element, Modifier.size(HEADER_ICON_SIZE))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f, fill = false).padding(start = 14.dp)) {
                    val nameStyle = MaterialTheme.typography.headlineSmall
                    Text(
                        element.name,
                        style = nameStyle,
                        color = Gold,
                        maxLines = 2,
                        autoSize = WholeWordsAutoSize(min = NAME_MIN_SIZE, max = nameStyle.fontSize)
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        RarityBadge(element.rarity)
                        if (element.id in ElementLinks.finalElementIds) FinalBadge()
                    }
                }
            }
            FactText(elementFacts.getValue(element.id), Modifier.padding(top = 8.dp))
            if (madeFrom.isNotEmpty()) {
                LinkSection(R.string.element_made_from) {
                    madeFrom.forEach { recipe ->
                        LinkRow {
                            LinkTile(recipe.firstId, onOpenElement)
                            OperatorIcon(R.drawable.ic_plus)
                            LinkTile(recipe.secondId, onOpenElement)
                        }
                    }
                }
            }
            if (knownUses.isNotEmpty()) {
                LinkSection(R.string.element_used_in) {
                    knownUses.forEach { recipe ->
                        LinkRow {
                            OperatorIcon(R.drawable.ic_plus)
                            LinkTile(recipe.partnerOf(element.id), onOpenElement)
                            OperatorIcon(R.drawable.ic_forward)
                            LinkTile(recipe.resultId, onOpenElement)
                        }
                    }
                }
            }
            val note = when {
                allUses.isEmpty() -> stringResource(R.string.element_final_note)
                allUses.size > knownUses.size -> pluralStringResource(R.plurals.element_unknown_combinations, allUses.size - knownUses.size, allUses.size - knownUses.size)
                else -> null
            }
            note?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("element_links_note")
                )
            }
        }
    }
}

private fun Recipe.isKnown(progress: PlayerProgress): Boolean = recipeKey(firstId, secondId) in progress.knownRecipeKeys

@Composable
private fun LinkSection(titleRes: Int, content: @Composable () -> Unit) {
    Text(
        stringResource(titleRes),
        style = MaterialTheme.typography.titleSmall,
        color = Gold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp)
    )
    content()
}

@Composable
private fun LinkRow(content: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
    ) {
        content()
    }
}

@Composable
private fun LinkTile(elementId: String, onOpen: (ElementDefinition) -> Unit) {
    val element = AlchemyCatalog.elementsById.getValue(elementId)
    ElementTile(element, Modifier.width(LINK_TILE_WIDTH).testTag("link_$elementId"), onClick = { onOpen(element) })
}

@Composable
private fun OperatorIcon(res: Int) {
    Image(painterResource(res), contentDescription = null, modifier = Modifier.size(18.dp))
}

private val LINK_TILE_WIDTH = 60.dp
private val HEADER_ICON_SIZE = 84.dp
private val HEADER_SIDE_GAP = 36.dp
private val CARD_SIDE_PADDING = 20.dp
private const val CARD_WIDTH_FRACTION = 0.96f

// Fits a fact, one recipe and the note on a compact phone; longer cards scroll.
private val CARD_HEIGHT = 470.dp
private val NAME_MIN_SIZE = 18.sp
