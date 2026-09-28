package com.artt.alchemy.ui.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.artt.alchemy.R
import com.artt.alchemy.data.PlayerProgress
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.Recipe
import com.artt.alchemy.game.recipeKey
import com.artt.alchemy.ui.components.AlchemySearchField
import com.artt.alchemy.ui.components.FramedElementIcon
import com.artt.alchemy.ui.theme.PanelBorderColor
import com.artt.alchemy.ui.theme.PanelColor

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

    Column(modifier = modifier.fillMaxSize().testTag("screen_recipes").padding(12.dp)) {
        AlchemySearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(R.string.recipe_filter),
            modifier = Modifier.fillMaxWidth().testTag("recipes_filter")
        )
        if (recipes.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelColor, RoundedCornerShape(16.dp))
            .border(1.dp, PanelBorderColor, RoundedCornerShape(16.dp))
            .padding(10.dp)
            .semantics(mergeDescendants = true) { contentDescription = "${first.name} + ${second.name} = ${result.name}" }
    ) {
        FramedElementIcon(first, Modifier.size(RECIPE_ICON_SIZE))
        Text("+", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        FramedElementIcon(second, Modifier.size(RECIPE_ICON_SIZE))
        Text("→", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        FramedElementIcon(result, Modifier.size(RECIPE_ICON_SIZE))
        Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
            Text(result.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
            Text(
                "${first.name} + ${second.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

private val RECIPE_ICON_SIZE = 52.dp
