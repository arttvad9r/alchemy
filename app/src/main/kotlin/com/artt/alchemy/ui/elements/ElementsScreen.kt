package com.artt.alchemy.ui.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.artt.alchemy.R
import com.artt.alchemy.data.PlayerProgress
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.ElementDefinition
import com.artt.alchemy.game.ElementGroup
import com.artt.alchemy.ui.components.AlchemySearchField
import com.artt.alchemy.ui.components.AlchemyTab
import com.artt.alchemy.ui.components.FramedElementIcon
import com.artt.alchemy.ui.components.RarityBadge
import com.artt.alchemy.ui.components.ScreenBanner
import com.artt.alchemy.ui.components.panelBackground
import com.artt.alchemy.ui.components.rarity

@Composable
fun ElementsScreen(progress: PlayerProgress, modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf<ElementGroup?>(null) }
    val entries = AlchemyCatalog.elements.filter { element ->
        (selectedGroup == null || element.group == selectedGroup) &&
            (query.isBlank() || (element.id in progress.unlockedIds && element.name.contains(query, ignoreCase = true)))
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // Three columns on a phone keep long names whole even with large text; wider screens get more.
        val columns = maxOf(1, ((maxWidth - SCREEN_PADDING * 2) / CARD_MIN_WIDTH).toInt())
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("screen_elements"),
            contentPadding = PaddingValues(SCREEN_PADDING)
        ) {
            item {
                ScreenBanner(stringResource(R.string.tab_elements), Modifier.padding(bottom = 8.dp))
            }
            item {
                AlchemySearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = stringResource(R.string.element_search),
                    modifier = Modifier.fillMaxWidth().testTag("elements_search")
                )
            }
            item {
                LazyRow(modifier = Modifier.padding(vertical = 8.dp), contentPadding = PaddingValues(end = 8.dp)) {
                    item {
                        AlchemyTab(
                            text = stringResource(R.string.group_all),
                            selected = selectedGroup == null,
                            onClick = { selectedGroup = null },
                            modifier = Modifier.testTag("elements_group_all")
                        )
                    }
                    lazyRowItems(ElementGroup.entries) { group ->
                        AlchemyTab(
                            text = stringResource(group.labelRes),
                            selected = selectedGroup == group,
                            onClick = { selectedGroup = group },
                            modifier = Modifier.padding(start = 8.dp).testTag("elements_group_${group.name.lowercase()}")
                        )
                    }
                }
            }
            // Each row is as tall as its tallest card and every card in it stretches to match, so rows
            // line up without reserving room for names that fit on one line.
            items(entries.chunked(columns), key = { row -> row.first().id }) { row ->
                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    row.forEach { element ->
                        ElementCard(element, unlocked = element.id in progress.unlockedIds, Modifier.weight(1f).fillMaxHeight())
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun ElementCard(element: ElementDefinition, unlocked: Boolean, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(3.dp)
            .testTag(if (unlocked) "element_${element.id}" else "element_locked_${element.id}")
            .panelBackground(R.drawable.card_base)
            // Clears the ornament on the top edge of the card art.
            .padding(start = 6.dp, top = 10.dp, end = 6.dp, bottom = CARD_TEXT_GAP + CARD_BOTTOM_BORDER)
    ) {
        FramedElementIcon(element, Modifier.fillMaxWidth(), locked = !unlocked)
        // The name and badge sit in the middle of what is left when a neighbour's name takes two lines.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f).padding(top = CARD_TEXT_GAP)
        ) {
            Text(
                text = if (unlocked) element.name else stringResource(R.string.locked_element),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (unlocked) RarityBadge(element.rarity, Modifier.padding(top = 2.dp))
        }
    }
}

private val ElementGroup.labelRes: Int
    get() = when (this) {
        ElementGroup.NATURE -> R.string.group_nature
        ElementGroup.MATERIAL -> R.string.group_material
        ElementGroup.LIFE -> R.string.group_life
        ElementGroup.CIVILIZATION -> R.string.group_civilization
        ElementGroup.COSMOS -> R.string.group_cosmos
    }

private val CARD_MIN_WIDTH = 104.dp
private val SCREEN_PADDING = 12.dp

// Space above the name and below the badge, kept equal so the text sits evenly in the card.
private val CARD_TEXT_GAP = 6.dp

// The card art's bottom border, which the gap under the badge is measured from.
private val CARD_BOTTOM_BORDER = 4.dp
