package com.artt.alchemy.game

import java.text.Collator
import java.util.Locale

enum class ElementSort { RECENT, ALPHABET, GROUP }

enum class RecipeSort { RECENT, ALPHABET }

/** [elements] ordered for display; ties keep their incoming (catalog) order. */
/** [name] gives an element's display name; alphabetical order follows [locale]. */
fun sortElements(
    elements: List<ElementDefinition>,
    discoveryOrder: List<String>,
    sort: ElementSort,
    locale: Locale,
    name: (String) -> String
): List<ElementDefinition> = when (sort) {
    ElementSort.RECENT -> {
        val position = discoveryOrder.withIndex().associate { it.value to it.index }
        elements.sortedByDescending { position[it.id] ?: -1 }
    }
    ElementSort.ALPHABET -> {
        val collator = Collator.getInstance(locale)
        elements.sortedWith { first, second -> collator.compare(name(first.id), name(second.id)) }
    }
    ElementSort.GROUP -> elements.sortedBy { it.group.ordinal }
}

/** A recipe counts as recent when the element it makes was found recently, as recipes carry no time of their own. */
fun sortRecipes(recipes: List<Recipe>, discoveryOrder: List<String>, sort: RecipeSort, locale: Locale, name: (String) -> String): List<Recipe> = when (sort) {
    RecipeSort.RECENT -> {
        val position = discoveryOrder.withIndex().associate { it.value to it.index }
        recipes.sortedByDescending { position[it.resultId] ?: -1 }
    }
    RecipeSort.ALPHABET -> {
        val collator = Collator.getInstance(locale)
        recipes.sortedWith { first, second -> collator.compare(name(first.resultId), name(second.resultId)) }
    }
}
