package com.artt.alchemy.game

import com.artt.alchemy.data.ActiveHint
import com.artt.alchemy.data.initialPlayerProgress
import com.artt.alchemy.data.recordAttempt
import com.artt.alchemy.data.requestHint
import com.artt.alchemy.data.sanitized
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HintsTest {
    @Test
    fun hint_needs_both_ingredients_open_and_the_result_not() {
        val unlocked = initialPlayerProgress().unlockedIds
        val recipe = nextHintRecipe(unlocked)

        assertNotNull(recipe)
        assertTrue(recipe!!.firstId in unlocked && recipe.secondId in unlocked)
        assertTrue(recipe.resultId !in unlocked)
    }

    @Test
    fun hint_prefers_the_shallowest_result() {
        val unlocked = initialPlayerProgress().unlockedIds
        val chosen = nextHintRecipe(unlocked)!!
        val candidates = AlchemyCatalog.recipes.filter { it.firstId in unlocked && it.secondId in unlocked && it.resultId !in unlocked }

        assertEquals(1, AlchemyCatalog.depthById.getValue(chosen.resultId))
        assertTrue(candidates.all { AlchemyCatalog.depthById.getValue(it.resultId) >= 1 })
    }

    @Test
    fun there_is_a_hint_until_every_element_is_open() {
        var progress = initialPlayerProgress()
        while (progress.unlockedIds.size < AlchemyCatalog.elements.size) {
            val recipe = nextHintRecipe(progress.unlockedIds)
            assertNotNull("a hint exists with ${progress.unlockedIds.size} elements open", recipe)
            progress = progress.recordAttempt(Combination(recipe!!.firstId, recipe.secondId, recipe.resultId))
        }

        assertNull(nextHintRecipe(progress.unlockedIds))
        assertNull(progress.requestHint().activeHint)
    }

    @Test
    fun first_request_starts_step_one_and_second_reveals_step_two() {
        val started = initialPlayerProgress().requestHint()
        val recipe = nextHintRecipe(initialPlayerProgress().unlockedIds)!!

        assertEquals(ActiveHint(recipeKey(recipe.firstId, recipe.secondId), step = 1), started.activeHint)
        assertEquals(2, started.requestHint().activeHint?.step)
    }

    @Test
    fun further_requests_keep_the_same_hint_until_its_element_is_found() {
        val second = initialPlayerProgress().requestHint().requestHint()

        assertEquals(second.activeHint, second.requestHint().activeHint)
    }

    @Test
    fun finding_the_hinted_element_clears_the_hint_and_the_next_request_picks_another() {
        val hinted = initialPlayerProgress().requestHint()
        val recipe = recipeForKey(hinted.activeHint!!.recipeKey)!!

        val solved = hinted.recordAttempt(Combination(recipe.firstId, recipe.secondId, recipe.resultId))

        assertNull(solved.activeHint)
        val next = solved.requestHint().activeHint
        assertNotNull(next)
        assertTrue(next != hinted.activeHint)
    }

    @Test
    fun finding_another_element_keeps_the_hint() {
        val hinted = initialPlayerProgress().requestHint()
        val other = AlchemyCatalog.recipes.first { recipeKey(it.firstId, it.secondId) != hinted.activeHint!!.recipeKey }

        val mixed = hinted.recordAttempt(Combination(other.firstId, other.secondId, other.resultId))

        assertEquals(hinted.activeHint, mixed.activeHint)
    }

    @Test
    fun a_saved_hint_whose_element_is_already_open_is_dropped_on_load() {
        val recipe = nextHintRecipe(initialPlayerProgress().unlockedIds)!!
        val stale = initialPlayerProgress().copy(
            unlockedIds = initialPlayerProgress().unlockedIds + recipe.resultId,
            activeHint = ActiveHint(recipeKey(recipe.firstId, recipe.secondId), step = 2)
        )

        assertNull(stale.sanitized().activeHint)
    }
}
