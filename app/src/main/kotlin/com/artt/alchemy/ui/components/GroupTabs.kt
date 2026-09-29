package com.artt.alchemy.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artt.alchemy.R
import com.artt.alchemy.game.ElementGroup

/** "All" plus one tab per group; a null [selected] means all. [leading] sits before the tabs and scrolls with them. */
@Composable
fun GroupTabs(
    selected: ElementGroup?,
    onSelect: (ElementGroup?) -> Unit,
    tagPrefix: String,
    modifier: Modifier = Modifier,
    edgePadding: Dp = ScreenPadding,
    leading: (@Composable () -> Unit)? = null
) {
    LazyRow(modifier = modifier, contentPadding = PaddingValues(start = edgePadding, end = edgePadding)) {
        leading?.let { item { it() } }
        item {
            AlchemyTab(
                text = stringResource(R.string.group_all),
                selected = selected == null,
                onClick = { onSelect(null) },
                modifier = Modifier.padding(start = if (leading == null) 0.dp else 8.dp).testTag("${tagPrefix}_all")
            )
        }
        items(ElementGroup.entries) { group ->
            AlchemyTab(
                text = stringResource(group.labelRes),
                selected = selected == group,
                onClick = { onSelect(group) },
                modifier = Modifier.padding(start = 8.dp).testTag("${tagPrefix}_${group.name.lowercase()}")
            )
        }
    }
}

val ElementGroup.labelRes: Int
    get() = when (this) {
        ElementGroup.NATURE -> R.string.group_nature
        ElementGroup.MATERIAL -> R.string.group_material
        ElementGroup.LIFE -> R.string.group_life
        ElementGroup.CIVILIZATION -> R.string.group_civilization
        ElementGroup.COSMOS -> R.string.group_cosmos
    }
