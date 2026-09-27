package com.artt.alchemy.ui.elements

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.artt.alchemy.R
import com.artt.alchemy.data.PlayerProgress
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.ElementGroup

@Composable
fun ElementsScreen(progress: PlayerProgress, modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf<ElementGroup?>(null) }
    val entries = AlchemyCatalog.elements.filter { element ->
        (selectedGroup == null || element.group == selectedGroup) &&
            (query.isBlank() || (element.id in progress.unlockedIds && element.name.contains(query, ignoreCase = true)))
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize().testTag("screen_elements"),
        contentPadding = PaddingValues(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.element_search)) },
                singleLine = true,
                modifier = Modifier.testTag("elements_search")
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            LazyRow(modifier = Modifier.padding(vertical = 8.dp), contentPadding = PaddingValues(end = 8.dp)) {
                item {
                    FilterChip(
                        selected = selectedGroup == null,
                        onClick = { selectedGroup = null },
                        label = { Text(stringResource(R.string.group_all)) },
                        modifier = Modifier.testTag("elements_group_all")
                    )
                }
                lazyRowItems(ElementGroup.entries) { group ->
                    FilterChip(
                        selected = selectedGroup == group,
                        onClick = { selectedGroup = group },
                        label = { Text(stringResource(group.labelRes)) },
                        modifier = Modifier.padding(start = 8.dp).testTag("elements_group_${group.name.lowercase()}")
                    )
                }
            }
        }
        items(entries, key = { it.id }) { element ->
            Card(
                modifier = Modifier
                    .padding(6.dp)
                    .testTag(if (element.id in progress.unlockedIds) "element_${element.id}" else "element_locked_${element.id}")
            ) {
                Text(
                    text = if (element.id in progress.unlockedIds) element.name else stringResource(R.string.locked_element),
                    modifier = Modifier.padding(16.dp)
                )
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
