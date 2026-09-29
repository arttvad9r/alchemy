package com.artt.alchemy.game

import com.artt.alchemy.data.initialPlayerProgress
import com.artt.alchemy.data.recordAttempt
import com.artt.alchemy.data.reset
import com.artt.alchemy.data.sanitized
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressRulesTest {
    @Test
    fun initial_progress_contains_only_the_four_base_elements() {
        assertEquals(AlchemyCatalog.baseElementIds, initialPlayerProgress().unlockedIds)
    }

    @Test
    fun first_successful_recipe_unlocks_result_and_marks_recipe_known() {
        val progress = initialPlayerProgress().recordAttempt(Combination("fire", "water", "steam"))

        assertEquals(AlchemyCatalog.baseElementIds + "steam", progress.unlockedIds)
        assertEquals(setOf(recipeKey("fire", "water")), progress.knownRecipeKeys)
        assertEquals(1, progress.successfulMixCount)
        assertEquals(1, progress.mixAttemptCount)
    }

    @Test
    fun repeated_successful_recipe_does_not_add_a_second_unique_unlock() {
        val first = initialPlayerProgress().recordAttempt(Combination("fire", "water", "steam"))
        val repeated = first.recordAttempt(Combination("water", "fire", "steam"))

        assertEquals(AlchemyCatalog.baseElementIds + "steam", repeated.unlockedIds)
        assertEquals(1, repeated.knownRecipeKeys.size)
        assertEquals(2, repeated.successfulMixCount)
    }

    @Test
    fun reset_restores_initial_progress() {
        val changed = initialPlayerProgress().recordAttempt(Combination("fire", "water", "steam"))

        assertEquals(initialPlayerProgress(), changed.reset())
    }

    @Test
    fun discovery_order_lists_base_elements_first_then_results_as_found() {
        val progress = initialPlayerProgress()
            .recordAttempt(Combination("fire", "water", "steam"))
            .recordAttempt(Combination("earth", "water", "mud"))
            .recordAttempt(Combination("water", "fire", "steam"))

        assertEquals(listOf("fire", "water", "earth", "air", "steam", "mud"), progress.discoveryOrder)
    }

    @Test
    fun save_from_before_the_order_was_kept_gets_catalog_order() {
        val old = initialPlayerProgress().copy(
            unlockedIds = AlchemyCatalog.baseElementIds + setOf("mud", "steam"),
            discoveryOrder = emptyList()
        )

        assertEquals(listOf("fire", "water", "earth", "air", "steam", "mud"), old.sanitized().discoveryOrder)
    }

    @Test
    fun sanitizing_keeps_known_order_and_drops_unknown_entries() {
        val dirty = initialPlayerProgress().copy(
            unlockedIds = AlchemyCatalog.baseElementIds + setOf("mud", "steam", "no_such_element"),
            discoveryOrder = listOf("mud", "no_such_element", "fire", "mud"),
            knownRecipeKeys = setOf(recipeKey("fire", "water"), "no|such"),
            musicVolume = 3f,
            effectsVolume = -1f,
            mixAttemptCount = -5
        )

        val clean = dirty.sanitized()

        assertEquals(listOf("mud", "fire", "water", "earth", "air", "steam"), clean.discoveryOrder)
        assertEquals(AlchemyCatalog.baseElementIds + setOf("mud", "steam"), clean.unlockedIds)
        assertEquals(setOf(recipeKey("fire", "water")), clean.knownRecipeKeys)
        assertEquals(1f, clean.musicVolume, 0f)
        assertEquals(0f, clean.effectsVolume, 0f)
        assertEquals(0, clean.mixAttemptCount)
    }
}
