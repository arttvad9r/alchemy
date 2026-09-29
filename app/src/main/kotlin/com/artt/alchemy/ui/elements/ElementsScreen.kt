package com.artt.alchemy.ui.elements

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artt.alchemy.R
import com.artt.alchemy.data.PlayerProgress
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.ElementDefinition
import com.artt.alchemy.game.ElementGroup
import com.artt.alchemy.game.ElementLinks
import com.artt.alchemy.ui.components.AlchemySearchField
import com.artt.alchemy.ui.components.AlchemyTab
import com.artt.alchemy.ui.components.ElementIcon
import com.artt.alchemy.ui.components.FinalMark
import com.artt.alchemy.ui.components.ScreenBanner
import com.artt.alchemy.ui.components.WholeWordsAutoSize
import com.artt.alchemy.ui.components.panelBackground
import com.artt.alchemy.ui.theme.Gold
import kotlinx.coroutines.delay

@Composable
fun ElementsScreen(
    progress: PlayerProgress,
    freshIds: Set<String>,
    onSeen: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // What was new when the catalog opened stays marked until it is left.
    val fresh = remember { freshIds }
    DisposableEffect(Unit) { onDispose(onSeen) }
    var query by remember { mutableStateOf("") }
    var openedElement by remember { mutableStateOf<ElementDefinition?>(null) }
    var selectedGroup by remember { mutableStateOf<ElementGroup?>(null) }
    // Open elements come first; each part keeps the catalog order, as sortedBy is stable.
    val entries = AlchemyCatalog.elements.filter { element ->
        (selectedGroup == null || element.group == selectedGroup) &&
            (query.isBlank() || (element.id in progress.unlockedIds && element.name.contains(query, ignoreCase = true)))
    }.sortedBy { it.id !in progress.unlockedIds }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // Four columns on a compact phone; long names shrink to whole words, wider screens get more.
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
                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).animateItem()) {
                    row.forEach { element ->
                        val unlocked = element.id in progress.unlockedIds
                        ElementCard(
                            element,
                            unlocked = unlocked,
                            fresh = element.id in fresh,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onOpen = {
                                onClick()
                                openedElement = element
                            }.takeIf { unlocked }
                        )
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
    openedElement?.let { element ->
        ElementDetailsDialog(element, progress, onOpenElement = { openedElement = it }, onDismiss = { openedElement = null })
    }
}

@Composable
private fun ElementCard(
    element: ElementDefinition,
    unlocked: Boolean,
    fresh: Boolean,
    modifier: Modifier = Modifier,
    onOpen: (() -> Unit)? = null
) {
    // A fresh element shows as a silhouette for a moment, then its icon appears.
    var revealed by remember { mutableStateOf(!fresh) }
    LaunchedEffect(Unit) {
        delay(REVEAL_DELAY_MILLIS)
        revealed = true
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(3.dp)
            .testTag(if (unlocked) "element_${element.id}" else "element_locked_${element.id}")
            .then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier)
            .panelBackground(R.drawable.card_base)
            // Clears the ornament on the top edge of the card art.
            .padding(start = 6.dp, top = 8.dp, end = 6.dp, bottom = CARD_TEXT_GAP + CARD_BOTTOM_BORDER)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            // The card art is the only frame; the icon sits straight on it.
            Crossfade(targetState = unlocked && revealed, label = "reveal") { shown ->
                ElementIcon(element, Modifier.fillMaxWidth().padding(ICON_INSET), silhouette = !shown)
            }
            if (unlocked && element.id in ElementLinks.finalElementIds) {
                val finalLabel = stringResource(R.string.element_final)
                FinalMark(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(FINAL_MARK_INSET)
                        .size(FINAL_MARK_SIZE)
                        .semantics { contentDescription = finalLabel }
                )
            }
            if (fresh) {
                Text(
                    text = stringResource(R.string.element_new_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.background(Gold.copy(alpha = 0.85f), RoundedCornerShape(50)).padding(horizontal = 6.dp)
                )
            }
        }
        // The name sits in the middle of what is left when a neighbour's name takes two lines.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f).padding(top = CARD_TEXT_GAP)
        ) {
            val nameStyle = MaterialTheme.typography.labelMedium
            Text(
                text = if (unlocked) element.name else stringResource(R.string.locked_element),
                style = nameStyle,
                textAlign = TextAlign.Center,
                maxLines = 2,
                autoSize = WholeWordsAutoSize(min = NAME_MIN_SIZE, max = nameStyle.fontSize),
                color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
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

private const val REVEAL_DELAY_MILLIS = 250L
private val FINAL_MARK_SIZE = 11.dp

// In the icon's corner, clear of the art.
private val FINAL_MARK_INSET = 2.dp
private val CARD_MIN_WIDTH = 80.dp
private val ICON_INSET = 4.dp
private val NAME_MIN_SIZE = 10.sp
private val SCREEN_PADDING = 12.dp

// Space above and below the name, kept equal so the text sits evenly in the card.
private val CARD_TEXT_GAP = 6.dp

// The card art's bottom border, which the gap under the name is measured from.
private val CARD_BOTTOM_BORDER = 4.dp
