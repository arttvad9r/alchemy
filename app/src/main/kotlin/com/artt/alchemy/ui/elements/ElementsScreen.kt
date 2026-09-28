package com.artt.alchemy.ui.elements

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.artt.alchemy.R
import com.artt.alchemy.data.PlayerProgress
import com.artt.alchemy.game.AlchemyCatalog
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

    LazyVerticalGrid(
        // Three columns on a phone keep long names whole even with large text; wider screens get more.
        columns = GridCells.Adaptive(CARD_MIN_WIDTH),
        modifier = modifier.fillMaxSize().testTag("screen_elements"),
        contentPadding = PaddingValues(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ScreenBanner(stringResource(R.string.tab_elements), Modifier.padding(bottom = 8.dp))
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            AlchemySearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.element_search),
                modifier = Modifier.fillMaxWidth().testTag("elements_search")
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
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
        items(entries, key = { it.id }) { element ->
            val unlocked = element.id in progress.unlockedIds
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(3.dp)
                    .testTag(if (unlocked) "element_${element.id}" else "element_locked_${element.id}")
                    .panelBackground(R.drawable.card_base)
                    // Clears the ornament on the top edge of the card art.
                    .padding(start = 6.dp, top = 10.dp, end = 6.dp, bottom = 7.dp)
            ) {
                FramedElementIcon(element, Modifier.fillMaxWidth(), locked = !unlocked)
                // An invisible two-line name with a badge sizes every card alike, so rows line up, while
                // the real name and badge sit together in the middle of that room.
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(top = 4.dp)) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.alpha(0f).clearAndSetSemantics {}
                    ) {
                        Text("", style = MaterialTheme.typography.labelMedium, minLines = 2)
                        RarityBadge(element.rarity, Modifier.padding(top = 2.dp))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
