package com.artt.alchemy.game

enum class ElementRarity {
    BASE,
    COMMON,
    RARE,
    EPIC,
    LEGENDARY
}

/** Fewest combination steps needed to reach each element from the base set. */
fun recipeDepths(baseIds: Set<String>, recipes: List<Recipe>): Map<String, Int> {
    val depths = baseIds.associateWith { 0 }.toMutableMap()
    var changed = true
    while (changed) {
        changed = false
        recipes.forEach { recipe ->
            val first = depths[recipe.firstId] ?: return@forEach
            val second = depths[recipe.secondId] ?: return@forEach
            val depth = maxOf(first, second) + 1
            if (depth < (depths[recipe.resultId] ?: Int.MAX_VALUE)) {
                depths[recipe.resultId] = depth
                changed = true
            }
        }
    }
    return depths
}

fun rarityForDepth(depth: Int): ElementRarity = when {
    depth == 0 -> ElementRarity.BASE
    depth <= 3 -> ElementRarity.COMMON
    depth <= 7 -> ElementRarity.RARE
    depth <= 11 -> ElementRarity.EPIC
    else -> ElementRarity.LEGENDARY
}
