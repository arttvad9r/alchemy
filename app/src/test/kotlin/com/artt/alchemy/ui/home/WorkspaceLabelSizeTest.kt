package com.artt.alchemy.ui.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceLabelSizeTest {
    @Test
    fun short_workspace_keeps_element_names_readable() {
        assertTrue("Element names must keep at least a 12px size at density 1", workspaceLabelSize(400f, 100f) >= 12f)
    }

    @Test
    fun element_names_follow_system_font_scale() {
        val regular = workspaceLabelSize(400f, 400f, Density(1f, 1f))
        val enlarged = workspaceLabelSize(400f, 400f, Density(1f, 2f))
        assertTrue("System font scale must enlarge element names", enlarged > regular)
    }

    @Test
    fun minimum_name_size_scales_with_display_density() {
        assertEquals(24f, workspaceLabelSize(400f, 100f, Density(2f)), 0.01f)
    }

    @Test
    fun large_labels_are_kept_inside_bottom_edge_when_item_settles() {
        val position = restingPosition(Offset(0.5f, 1f), 300f, 200f, labelHalfWidth = 40f, labelHeight = 60f)
        // On this board the icon ends 10.34px below the item centre; the whole 60px label follows.
        assertTrue(position.y * 200f + 10.34f + 60f <= 200.01f)
    }
}
