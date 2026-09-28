package com.artt.alchemy.game

import org.junit.Assert.assertEquals
import org.junit.Test

class ElementRarityTest {
    @Test
    fun depthUsesTheShortestRecipeChain() {
        val recipes = listOf(
            Recipe("a", "a", "b"),
            Recipe("b", "b", "c"),
            Recipe("c", "c", "d"),
            Recipe("a", "b", "d")
        )

        assertEquals(mapOf("a" to 0, "b" to 1, "c" to 2, "d" to 2), recipeDepths(setOf("a"), recipes))
    }

    @Test
    fun catalogRaritiesFollowRecipeDepth() {
        assertEquals(ElementRarity.BASE, AlchemyCatalog.rarityById.getValue("fire"))
        assertEquals(ElementRarity.COMMON, AlchemyCatalog.rarityById.getValue("steam"))
        assertEquals(ElementRarity.RARE, AlchemyCatalog.rarityById.getValue("life"))
        assertEquals(ElementRarity.EPIC, AlchemyCatalog.rarityById.getValue("house"))
        assertEquals(ElementRarity.LEGENDARY, AlchemyCatalog.rarityById.getValue("astronomer"))
        assertEquals(AlchemyCatalog.elements.size, AlchemyCatalog.rarityById.size)
    }
}
