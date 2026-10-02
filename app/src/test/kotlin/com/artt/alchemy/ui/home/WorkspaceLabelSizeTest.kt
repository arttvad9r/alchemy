package com.artt.alchemy.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceLabelSizeTest {
    @Test
    fun short_workspace_keeps_element_names_readable() {
        assertTrue("Element names must keep at least a 12px size at density 1", workspaceLabelSize(400f, 100f) >= 12f)
    }
}
