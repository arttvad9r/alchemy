package com.artt.alchemy.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlchemyEngineTest {
    private val engine = AlchemyEngine(AlchemyCatalog)

    @Test
    fun catalog_has_120_unique_elements_and_180_unique_pair_keys() {
        assertEquals(120, AlchemyCatalog.elements.size)
        assertEquals(120, AlchemyCatalog.elements.map(ElementDefinition::id).toSet().size)
        assertEquals(180, AlchemyCatalog.recipes.size)
        assertEquals(180, AlchemyCatalog.recipes.map { recipeKey(it.firstId, it.secondId) }.toSet().size)
    }

    @Test
    fun every_catalog_element_is_reachable_from_base_elements() {
        val known = AlchemyCatalog.baseElementIds.toMutableSet()
        var changed: Boolean
        do {
            changed = false
            AlchemyCatalog.recipes.forEach { recipe ->
                if (recipe.firstId in known && recipe.secondId in known) {
                    changed = known.add(recipe.resultId) || changed
                }
            }
        } while (changed)

        assertEquals(AlchemyCatalog.elements.map(ElementDefinition::id).toSet(), known)
    }

    @Test
    fun combine_is_independent_of_ingredient_order() {
        assertEquals("steam", engine.combine("fire", "water"))
        assertEquals("steam", engine.combine("water", "fire"))
    }

    @Test
    fun unknown_pair_returns_null() {
        assertNull(engine.combine("fire", "fire"))
    }

    @Test
    fun four_base_elements_are_present() {
        assertEquals(setOf("fire", "water", "earth", "air"), AlchemyCatalog.baseElementIds)
        assertTrue(AlchemyCatalog.baseElementIds.all { id -> AlchemyCatalog.elements.any { it.id == id } })
    }
}
