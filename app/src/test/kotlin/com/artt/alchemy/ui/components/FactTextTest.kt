package com.artt.alchemy.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class FactTextTest {
    @Test
    fun numbers_units_and_dashes_are_bound_to_their_neighbours() {
        assertEquals(
            "порыв — 408 км/⁠ч в 1996 году",
            withTypographicBinding("порыв — 408 км/ч в 1996 году")
        )
    }
}
