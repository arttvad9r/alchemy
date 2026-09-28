package com.artt.alchemy.ui.components

import com.artt.alchemy.game.AlchemyCatalog
import org.junit.Assert.assertEquals
import org.junit.Test

class ElementIconsTest {
    @Test
    fun everyCatalogElementHasExactlyOneIcon() {
        assertEquals(AlchemyCatalog.elements.map { it.id }.toSet(), elementIcons.keys)
    }
}
