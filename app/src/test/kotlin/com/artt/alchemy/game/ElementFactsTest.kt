package com.artt.alchemy.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ElementFactsTest {
    @Test
    fun every_element_has_exactly_one_fact() {
        assertEquals(AlchemyCatalog.elementsById.keys, elementFacts.keys)
    }

    @Test
    fun facts_are_short_enough_to_read_at_a_glance() {
        val tooLong = elementFacts.filterValues { it.isBlank() || it.length > MAX_FACT_LENGTH }
        assertTrue("Blank or too long: ${tooLong.keys}", tooLong.isEmpty())
    }

    private companion object {
        const val MAX_FACT_LENGTH = 130
    }
}
