package com.artt.alchemy.ui.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.artt.alchemy.R
import com.artt.alchemy.data.PlayerProgress
import com.artt.alchemy.game.AlchemyCatalog
import com.artt.alchemy.game.recipeKey

@Composable
fun RecipesScreen(progress: PlayerProgress, modifier: Modifier = Modifier) {
    val recipes = AlchemyCatalog.recipes.filter { recipeKey(it.firstId, it.secondId) in progress.knownRecipeKeys }
    if (recipes.isEmpty()) {
        Column(modifier = modifier.fillMaxSize().testTag("screen_recipes"), verticalArrangement = Arrangement.Center) {
            Text(text = stringResource(R.string.no_known_recipes), modifier = Modifier.padding(24.dp))
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxSize().testTag("screen_recipes")) {
            items(recipes, key = { recipeKey(it.firstId, it.secondId) }) { recipe ->
                val first = AlchemyCatalog.elementsById.getValue(recipe.firstId).name
                val second = AlchemyCatalog.elementsById.getValue(recipe.secondId).name
                val result = AlchemyCatalog.elementsById.getValue(recipe.resultId).name
                Text(text = "$first + $second = $result", modifier = Modifier.padding(16.dp))
            }
        }
    }
}
