package com.artt.alchemy.game

private val recipesByKey: Map<String, Recipe> = AlchemyCatalog.recipes.associateBy { recipeKey(it.firstId, it.secondId) }

fun recipeForKey(key: String): Recipe? = recipesByKey[key]

/**
 * The recipe to hint at: both ingredients open, result not, shallowest result first; ties keep catalog order.
 * Null only once every element is open.
 */
fun nextHintRecipe(unlockedIds: Set<String>): Recipe? = AlchemyCatalog.recipes
    .filter { it.firstId in unlockedIds && it.secondId in unlockedIds && it.resultId !in unlockedIds }
    .minByOrNull { AlchemyCatalog.depthById.getValue(it.resultId) }
