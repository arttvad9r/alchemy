package com.artt.alchemy.ui.elements

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Card
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

@Composable
fun ElementsScreen(progress: PlayerProgress, modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    val entries = AlchemyCatalog.elements.filter {
        query.isBlank() || (it.id in progress.unlockedIds && it.name.contains(query, ignoreCase = true))
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize().testTag("screen_elements"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            BasicTextField(value = query, onValueChange = { query = it }, modifier = Modifier.testTag("elements_search"))
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
