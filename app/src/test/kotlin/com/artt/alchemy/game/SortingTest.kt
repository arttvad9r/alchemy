package com.artt.alchemy.game

import org.junit.Assert.assertEquals
import org.junit.Test

class SortingTest {
    private val elements = AlchemyCatalog.elements
    private val russian = java.util.Locale.forLanguageTag("ru")
    private val order = listOf("fire", "water", "earth", "air", "steam", "mud")
    private val opened = elements.filter { it.id in order }

    @Test
    fun recent_puts_the_newest_element_first_and_keeps_catalog_order_for_the_rest() {
        val sorted = sortElements(opened, order, ElementSort.RECENT, russian, ::russianName).map { it.id }

        assertEquals(order.reversed(), sorted)
    }

    @Test
    fun alphabet_orders_by_name_in_russian() {
        val names = sortElements(opened, order, ElementSort.ALPHABET, russian, ::russianName).map { russianName(it.id) }

        assertEquals(names.sortedWith(java.text.Collator.getInstance(russian)), names)
    }

    @Test
    fun group_keeps_catalog_order_inside_a_group() {
        val sorted = sortElements(opened, order, ElementSort.GROUP, russian, ::russianName)

        assertEquals(sorted.map { it.group.ordinal }, sorted.map { it.group.ordinal }.sorted())
        ElementGroup.entries.forEach { group ->
            assertEquals(opened.filter { it.group == group }, sorted.filter { it.group == group })
        }
    }

    @Test
    fun recipes_are_recent_by_the_element_they_make_and_alphabetical_by_its_name() {
        val known = AlchemyCatalog.recipes.filter { it.resultId in order }

        assertEquals(
            known.map { it.resultId }.sortedByDescending(order::indexOf),
            sortRecipes(known, order, RecipeSort.RECENT, russian, ::russianName).map { it.resultId }
        )
        val names = sortRecipes(known, order, RecipeSort.ALPHABET, russian, ::russianName).map { russianName(it.resultId) }
        assertEquals(names.sortedWith(java.text.Collator.getInstance(russian)), names)
    }

    private fun russianName(id: String): String = ElementResources.russian.name(id)!!
}
