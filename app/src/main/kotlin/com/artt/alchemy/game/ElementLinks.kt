package com.artt.alchemy.game

/** Which recipes make an element and which use it, worked out once from the catalog. */
object ElementLinks {
    val recipesByResult: Map<String, List<Recipe>> = AlchemyCatalog.recipes.groupBy(Recipe::resultId)

    val recipesByIngredient: Map<String, List<Recipe>> = AlchemyCatalog.recipes
        .flatMap { recipe -> setOf(recipe.firstId, recipe.secondId).map { it to recipe } }
        .groupBy({ it.first }, { it.second })

    /** Elements that take part in no recipe: nothing more can be made from them. */
    val finalElementIds: Set<String> = AlchemyCatalog.elements.map(ElementDefinition::id).filterNot(recipesByIngredient::containsKey).toSet()
}

/** The ingredient that goes with [elementId] in this recipe; the element itself for a pair of the same. */
fun Recipe.partnerOf(elementId: String): String = if (firstId == elementId) secondId else firstId
