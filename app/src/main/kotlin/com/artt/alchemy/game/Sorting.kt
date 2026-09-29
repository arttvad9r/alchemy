package com.artt.alchemy.game

import java.text.Collator
import java.util.Locale

enum class ElementSort { RECENT, ALPHABET, GROUP }

enum class RecipeSort { RECENT, ALPHABET }

private val russianCollator: Collator = Collator.getInstance(Locale.forLanguageTag("ru"))

private val nameOrder: Comparator<ElementDefinition> = Comparator { first, second -> russianCollator.compare(first.name, second.name) }

/** [elements] ordered for display; ties keep their incoming (catalog) order. */
fun sortElements(elements: List<ElementDefinition>, discoveryOrder: List<String>, sort: ElementSort): List<ElementDefinition> = when (sort) {
    ElementSort.RECENT -> {
        val position = discoveryOrder.withIndex().associate { it.value to it.index }
        elements.sortedByDescending { position[it.id] ?: -1 }
    }
    ElementSort.ALPHABET -> elements.sortedWith(nameOrder)
    ElementSort.GROUP -> elements.sortedBy { it.group.ordinal }
}

/** A recipe counts as recent when the element it makes was found recently, as recipes carry no time of their own. */
fun sortRecipes(recipes: List<Recipe>, discoveryOrder: List<String>, sort: RecipeSort): List<Recipe> = when (sort) {
    RecipeSort.RECENT -> {
        val position = discoveryOrder.withIndex().associate { it.value to it.index }
        recipes.sortedByDescending { position[it.resultId] ?: -1 }
    }
    RecipeSort.ALPHABET -> recipes.sortedWith { first, second ->
        russianCollator.compare(AlchemyCatalog.elementsById.getValue(first.resultId).name, AlchemyCatalog.elementsById.getValue(second.resultId).name)
    }
}
