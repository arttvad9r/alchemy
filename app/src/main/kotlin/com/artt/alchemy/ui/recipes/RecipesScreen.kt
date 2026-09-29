package com.artt.alchemy.ui.recipes

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artt.alchemy.R
import com.artt.alchemy.data.PlayerProgress
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.Recipe
import com.artt.alchemy.game.recipeKey
import com.artt.alchemy.ui.components.AlchemySearchField
import com.artt.alchemy.ui.components.FramedElementIcon
import com.artt.alchemy.ui.components.ScreenBanner
import com.artt.alchemy.ui.components.ScreenPadding
import com.artt.alchemy.ui.components.WholeWordsAutoSize
import com.artt.alchemy.ui.components.rowPanel

@Composable
fun RecipesScreen(progress: PlayerProgress, modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    val recipes = AlchemyCatalog.recipes
        .filter { recipeKey(it.firstId, it.secondId) in progress.knownRecipeKeys }
        .filter { recipe ->
            query.isBlank() ||
                listOf(recipe.firstId, recipe.secondId, recipe.resultId)
                    .map { AlchemyCatalog.elementsById.getValue(it).name }
                    .any { it.contains(query, ignoreCase = true) }
        }

    Column(modifier = modifier.fillMaxSize().testTag("screen_recipes").padding(ScreenPadding)) {
        ScreenBanner(stringResource(R.string.tab_recipes), Modifier.padding(bottom = 8.dp))
        AlchemySearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(R.string.recipe_filter),
            modifier = Modifier.fillMaxWidth().testTag("recipes_filter")
        )
        if (recipes.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = stringResource(R.string.no_known_recipes), modifier = Modifier.padding(24.dp))
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(recipes, key = { recipeKey(it.firstId, it.secondId) }) { recipe -> RecipeRow(recipe) }
            }
        }
    }
}

@Composable
private fun RecipeRow(recipe: Recipe) {
    val first = AlchemyCatalog.elementsById.getValue(recipe.firstId)
    val second = AlchemyCatalog.elementsById.getValue(recipe.secondId)
    val result = AlchemyCatalog.elementsById.getValue(recipe.resultId)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .rowPanel()
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) { contentDescription = "${first.name} + ${second.name} = ${result.name}" }
    ) {
        // Icons give up size before the names do, so a narrow screen keeps the names readable.
        val iconSize = ((maxWidth - OPERATOR_ICON_SIZE * 2 - ROW_GAP * 5 - NAMES_MIN_WIDTH) / 3).coerceIn(RECIPE_ICON_MIN_SIZE, RECIPE_ICON_SIZE)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ROW_GAP)) {
            FramedElementIcon(first, Modifier.size(iconSize))
            Image(painterResource(R.drawable.ic_plus), contentDescription = null, modifier = Modifier.size(OPERATOR_ICON_SIZE))
            FramedElementIcon(second, Modifier.size(iconSize))
            Image(painterResource(R.drawable.ic_forward), contentDescription = null, modifier = Modifier.size(OPERATOR_ICON_SIZE))
            FramedElementIcon(result, Modifier.size(iconSize))
            Column(modifier = Modifier.weight(1f)) {
                val nameStyle = MaterialTheme.typography.titleMedium
                val ingredientsStyle = MaterialTheme.typography.bodySmall
                Text(
                    result.name,
                    style = nameStyle,
                    maxLines = 2,
                    autoSize = WholeWordsAutoSize(min = NAME_MIN_SIZE, max = nameStyle.fontSize)
                )
                Text(
                    "${first.name} + ${second.name}",
                    style = ingredientsStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    autoSize = WholeWordsAutoSize(min = NAME_MIN_SIZE, max = ingredientsStyle.fontSize)
                )
            }
        }
    }
}

private val RECIPE_ICON_SIZE = 52.dp
private val OPERATOR_ICON_SIZE = 22.dp
private val RECIPE_ICON_MIN_SIZE = 38.dp
private val ROW_GAP = 6.dp
private val NAMES_MIN_WIDTH = 116.dp
private val NAME_MIN_SIZE = 10.sp
