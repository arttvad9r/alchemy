package com.artt.alchemy.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ElementLinksTest {
    @Test
    fun exactly_39_elements_are_final() {
        assertEquals(39, ElementLinks.finalElementIds.size)
    }

    @Test
    fun base_elements_are_never_final() {
        assertTrue(ElementLinks.finalElementIds.none(AlchemyCatalog.baseElementIds::contains))
    }

    @Test
    fun every_element_that_is_not_base_has_exactly_one_curated_recipe() {
        val curatedByResult = curatedRecipes().groupBy(Recipe::resultId)
        AlchemyCatalog.elements.map(ElementDefinition::id).filterNot(AlchemyCatalog.baseElementIds::contains).forEach { id ->
            assertEquals(id, 1, curatedByResult[id].orEmpty().size)
            assertTrue(id, ElementLinks.recipesByResult.getValue(id).first() == curatedByResult.getValue(id).single())
        }
    }

    @Test
    fun base_elements_have_no_source_recipe() {
        assertTrue(AlchemyCatalog.baseElementIds.all { it !in ElementLinks.recipesByResult })
    }

    @Test
    fun a_recipe_of_the_same_element_twice_is_listed_once() {
        assertEquals(1, ElementLinks.recipesByIngredient.getValue("earth").count { it.firstId == it.secondId })
    }

    @Test
    fun partner_of_a_pair_of_the_same_element_is_that_element() {
        assertEquals("earth", Recipe("earth", "earth", "stone").partnerOf("earth"))
        assertEquals("water", Recipe("fire", "water", "steam").partnerOf("fire"))
        assertEquals("fire", Recipe("fire", "water", "steam").partnerOf("water"))
    }

    @Test
    fun final_elements_are_not_ingredients() {
        assertFalse("steam" in ElementLinks.finalElementIds)
        assertTrue(ElementLinks.finalElementIds.all { it !in ElementLinks.recipesByIngredient })
    }
}
