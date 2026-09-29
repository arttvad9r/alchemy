package com.artt.alchemy.ui.recipes

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.artt.alchemy.R
import com.artt.alchemy.data.PlayerProgress
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.nextHintRecipe
import com.artt.alchemy.game.recipeForKey
import com.artt.alchemy.ui.components.ElementFrame
import com.artt.alchemy.ui.components.FramedElementIcon
import com.artt.alchemy.ui.components.rowPanel
import com.artt.alchemy.ui.theme.Gold

/** One row above the recipes: a button that asks for a hint, then the hint itself with what it has revealed so far. */
@Composable
fun HintCard(progress: PlayerProgress, onRequestHint: () -> Unit, onPlaceHint: () -> Unit, modifier: Modifier = Modifier) {
    val hint = progress.activeHint
    val recipe = hint?.let { recipeForKey(it.recipeKey) }
    if (hint == null || recipe == null) {
        if (nextHintRecipe(progress.unlockedIds) != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                modifier = modifier
                    .fillMaxWidth()
                    .rowPanel()
                    .clip(RoundedCornerShape(ROW_CORNER))
                    .clickable(role = Role.Button, onClick = onRequestHint)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("hint_request")
            ) {
                Image(painterResource(R.drawable.ic_hint), contentDescription = null, modifier = Modifier.size(BULB_SIZE))
                Text(stringResource(R.string.hint_request), style = MaterialTheme.typography.titleMedium, color = Gold)
            }
        }
        return
    }

    val first = AlchemyCatalog.elementsById.getValue(recipe.firstId)
    val second = AlchemyCatalog.elementsById.getValue(recipe.secondId)
    val result = AlchemyCatalog.elementsById.getValue(recipe.resultId)
    val secondRevealed = hint.step >= 2
    val description = stringResource(R.string.hint_place, first.name, if (secondRevealed) second.name else "?")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .rowPanel()
            .clip(RoundedCornerShape(ROW_CORNER))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("hint_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ICON_GAP),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(ROW_CORNER))
                .clickable(role = Role.Button, onClick = onPlaceHint)
                .testTag("hint_place")
                .semantics(mergeDescendants = true) { contentDescription = description }
        ) {
            FramedElementIcon(first, Modifier.size(HINT_ICON_SIZE))
            Image(painterResource(R.drawable.ic_plus), contentDescription = null, modifier = Modifier.size(OPERATOR_SIZE))
            if (secondRevealed) {
                FramedElementIcon(second, Modifier.size(HINT_ICON_SIZE))
            } else {
                UnknownSlot()
            }
            Image(painterResource(R.drawable.ic_forward), contentDescription = null, modifier = Modifier.size(OPERATOR_SIZE))
            FramedElementIcon(result, Modifier.size(HINT_ICON_SIZE), locked = true)
        }
        if (!secondRevealed) {
            Image(
                painterResource(R.drawable.ic_hint),
                contentDescription = stringResource(R.string.hint_more),
                modifier = Modifier
                    .padding(start = ICON_GAP)
                    .clip(RoundedCornerShape(ROW_CORNER))
                    .clickable(role = Role.Button, onClick = onRequestHint)
                    .padding(4.dp)
                    .size(BULB_SIZE)
                    .testTag("hint_more")
            )
        }
    }
}

@Composable
private fun UnknownSlot() {
    ElementFrame(rarity = null, modifier = Modifier.size(HINT_ICON_SIZE)) {
        Text("?", style = MaterialTheme.typography.titleLarge, color = Gold)
    }
}

private val ROW_CORNER = 14.dp
private val HINT_ICON_SIZE = 44.dp
private val OPERATOR_SIZE = 22.dp
private val BULB_SIZE = 32.dp
private val ICON_GAP = 6.dp
