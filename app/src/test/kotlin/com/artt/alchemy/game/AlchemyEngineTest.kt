package com.artt.alchemy.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlchemyEngineTest {
    private val engine = AlchemyEngine(AlchemyCatalog)

    @Test
    fun catalog_has_180_unique_elements_176_curated_recipes_and_unique_pairs() {
        assertEquals(180, AlchemyCatalog.elements.size)
        assertEquals(180, AlchemyCatalog.elements.map(ElementDefinition::id).toSet().size)
        assertEquals(176, curatedRecipes().size)
        assertEquals(AlchemyCatalog.recipes.size, AlchemyCatalog.recipes.map { recipeKey(it.firstId, it.secondId) }.toSet().size)
    }

    @Test
    fun alternative_recipes_make_the_same_elements_as_curated_ones() {
        assertEquals("stone", engine.combine("lava", "water"))
        assertEquals("glass", engine.combine("sand", "lightning"))
        assertEquals("wave", engine.combine("ocean", "moon"))
    }

    @Test
    fun alternative_recipes_keep_every_rarity_and_never_make_an_ingredient_from_itself() {
        val curated = recipeDepths(AlchemyCatalog.baseElementIds, curatedRecipes()).mapValues { (_, depth) -> rarityForDepth(depth) }
        assertEquals(curated, AlchemyCatalog.rarityById)
        assertTrue(AlchemyCatalog.recipes.none { it.resultId == it.firstId || it.resultId == it.secondId })
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
    fun importedProgressionContainsRecipesAcrossAllTiers() {
        assertEquals("stone", engine.combine("earth", "earth"))
        assertEquals("life", engine.combine("energy", "swamp"))
        assertEquals("city", engine.combine("settlement", "stone"))
        assertEquals("ship", engine.combine("boat", "sail"))
        assertEquals("alchemy", engine.combine("metal", "magic"))
        assertEquals("philosopher_stone", engine.combine("stone", "alchemy"))
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
