package com.artt.alchemy.ui.elements

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artt.alchemy.R
import com.artt.alchemy.data.PlayerProgress
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.ElementDefinition
import com.artt.alchemy.game.ElementGroup
import com.artt.alchemy.game.ElementLinks
import com.artt.alchemy.game.ElementRarity
import com.artt.alchemy.ui.components.AlchemySearchField
import com.artt.alchemy.ui.components.ElementIcon
import com.artt.alchemy.ui.components.ElementTextGap
import com.artt.alchemy.ui.components.FinalMark
import com.artt.alchemy.ui.components.GroupTabs
import com.artt.alchemy.ui.components.ScreenBanner
import com.artt.alchemy.ui.components.ScreenPadding
import com.artt.alchemy.ui.components.motion
import com.artt.alchemy.ui.components.panelBackground
import com.artt.alchemy.ui.components.rarity
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
    // Cards opened one from another, so back returns to the previous one.
    var openedCards by remember { mutableStateOf(emptyList<ElementDefinition>()) }
    var selectedGroup by remember { mutableStateOf<ElementGroup?>(null) }
    // Open elements come first; each part keeps the catalog order, as sortedBy is stable.
    val entries = AlchemyCatalog.elements.filter { element ->
        (selectedGroup == null || element.group == selectedGroup) &&
            (query.isBlank() || (element.id in progress.unlockedIds && element.name.contains(query, ignoreCase = true)))
    }.sortedBy { it.id !in progress.unlockedIds }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // Four columns on a compact phone; long names shrink to whole words, wider screens get more.
        val columns = maxOf(1, ((maxWidth - ROW_INSET * 2) / CARD_MIN_WIDTH).toInt())
        val nameStyle = catalogNameStyle((maxWidth - ROW_INSET * 2) / columns - CARD_GAP - CARD_SIDE_PADDING * 2)
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("screen_elements"),
            contentPadding = PaddingValues(top = ScreenPadding, bottom = ROW_INSET)
        ) {
            item {
                ScreenBanner(stringResource(R.string.tab_elements), Modifier.padding(start = ScreenPadding, end = ScreenPadding, bottom = 8.dp))
            }
            item {
                AlchemySearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = stringResource(R.string.element_search),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenPadding).testTag("elements_search")
                )
            }
            item {
                GroupTabs(
                    selected = selectedGroup,
                    onSelect = { selectedGroup = it },
                    tagPrefix = "elements_group",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            // Each row is as tall as its tallest card and every card in it stretches to match, so rows
            // line up without reserving room for names that fit on one line.
            items(entries.chunked(columns), key = { row -> row.first().id }) { row ->
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = ROW_INSET).height(IntrinsicSize.Min).animateItem()) {
                    row.forEach { element ->
                        val unlocked = element.id in progress.unlockedIds
                        ElementCard(
                            element,
                            unlocked = unlocked,
                            fresh = element.id in fresh,
                            nameStyle = nameStyle,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onOpen = {
                                onClick()
                                openedCards = listOf(element)
                            }.takeIf { unlocked }
                        )
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
    openedCards.lastOrNull()?.let { element ->
        ElementDetailsDialog(
            element = element,
            progress = progress,
            onOpenElement = { openedCards = if (it == element) openedCards else openedCards + it },
            onBack = { openedCards = openedCards.dropLast(1) }.takeIf { openedCards.size > 1 },
            onDismiss = { openedCards = emptyList() }
        )
    }
}

@Composable
private fun ElementCard(
    element: ElementDefinition,
    unlocked: Boolean,
    fresh: Boolean,
    nameStyle: TextStyle,
    modifier: Modifier = Modifier,
    onOpen: (() -> Unit)? = null
) {
    // A fresh element shows as a silhouette for a moment, then its icon appears.
    var revealed by remember { mutableStateOf(!fresh) }
    LaunchedEffect(Unit) {
        delay(REVEAL_DELAY_MILLIS)
        revealed = true
    }
    Box(
        modifier = modifier
            .padding(CARD_GAP / 2)
            .testTag(if (unlocked) "element_${element.id}" else "element_locked_${element.id}")
            .then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier)
            // The trim takes the rarity colour once the element is open.
            .panelBackground(if (unlocked) element.rarity.cardRes else R.drawable.card_base)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            // Clears the ornament on the top edge of the card art.
            modifier = Modifier.fillMaxSize().padding(start = CARD_SIDE_PADDING, top = 8.dp, end = CARD_SIDE_PADDING, bottom = ElementTextGap + CARD_BOTTOM_BORDER)
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                // The card art is the only frame; the icon sits straight on it.
                Crossfade(targetState = unlocked && revealed, animationSpec = motion(tween()), label = "reveal") { shown ->
                    ElementIcon(element, Modifier.fillMaxWidth().padding(ICON_INSET), silhouette = !shown)
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
            // The name stays right under the icon; a taller neighbour only adds room below it.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier.weight(1f).padding(top = ElementTextGap)
            ) {
                Text(
                    text = if (unlocked) element.name else stringResource(R.string.locked_element),
                    style = nameStyle,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (unlocked && element.id in ElementLinks.finalElementIds) {
            val finalLabel = stringResource(R.string.element_final)
            // Set into the bottom edge of the trim, like the ornament on the top edge.
            FinalMark(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = FINAL_MARK_BOTTOM)
                    .size(FINAL_MARK_SIZE)
                    .semantics { contentDescription = finalLabel }
            )
        }
    }
}

/**
 * One name size for the whole catalog: the largest that keeps every name in two lines of whole words
 * and "Не открыт" on one line, so neighbouring cards never differ.
 */
@Composable
private fun catalogNameStyle(contentWidth: Dp): TextStyle {
    // Tight leading, so a two-word name reads as one label.
    val base = MaterialTheme.typography.labelMedium.let { it.copy(lineHeight = it.fontSize * NAME_LINE_HEIGHT) }
    val locked = stringResource(R.string.locked_element)
    val measurer = rememberTextMeasurer()
    val widthPx = with(LocalDensity.current) { contentWidth.roundToPx() }
    return remember(base, locked, widthPx) {
        val constraints = Constraints(maxWidth = widthPx.coerceAtLeast(1))
        val words = (AlchemyCatalog.elements.flatMap { it.name.split(' ') } + locked).distinct()
        val fits = { size: Float ->
            val style = base.copy(fontSize = size.sp)
            words.all { measurer.measure(it, style, softWrap = false).size.width <= widthPx } &&
                measurer.measure(locked, style, softWrap = false).size.width <= widthPx &&
                AlchemyCatalog.elements.none { measurer.measure(it.name, style, constraints = constraints, maxLines = 2).hasVisualOverflow }
        }
        val size = generateSequence(base.fontSize.value) { it - NAME_SIZE_STEP }.takeWhile { it > NAME_MIN_SIZE.value }.firstOrNull(fits)
        val fontSize = (size ?: NAME_MIN_SIZE.value).sp
        base.copy(fontSize = fontSize, lineHeight = fontSize * NAME_LINE_HEIGHT)
    }
}

private val ElementRarity.cardRes: Int
    get() = when (this) {
        ElementRarity.BASE -> R.drawable.card_base
        ElementRarity.COMMON -> R.drawable.card_common
        ElementRarity.RARE -> R.drawable.card_rare
        ElementRarity.EPIC -> R.drawable.card_epic
        ElementRarity.LEGENDARY -> R.drawable.card_legendary
    }

private const val REVEAL_DELAY_MILLIS = 250L
private val FINAL_MARK_SIZE = 8.dp

// Puts the mark's centre on the trim line along the bottom edge of the card art.
private val FINAL_MARK_BOTTOM = 0.dp
private val CARD_MIN_WIDTH = 80.dp
private val ICON_INSET = 2.dp
private val NAME_MIN_SIZE = 10.sp
private const val NAME_SIZE_STEP = 0.5f
private const val NAME_LINE_HEIGHT = 1.1f
private val CARD_SIDE_PADDING = 4.dp

// Between neighbouring cards: enough that their trims never touch.
private val CARD_GAP = 4.dp

// Cards carry half a gap each, so their rows sit that much closer to the edge to keep the cards themselves ScreenPadding away.
private val ROW_INSET = ScreenPadding - CARD_GAP / 2

// The card art's bottom border, which the gap under the name is measured from.
private val CARD_BOTTOM_BORDER = 4.dp
