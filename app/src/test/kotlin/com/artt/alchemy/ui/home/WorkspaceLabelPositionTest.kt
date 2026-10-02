package com.artt.alchemy.ui.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceLabelPositionTest {
    private val label = Rect(-80f, -28f, 80f, 7f)
    private val board = Size(400f, 400f)

    @Test
    fun automatic_spawn_near_bottom_places_large_name_above_icon() {
        val icon = Rect(170.96f, 322.6f, 229.04f, 380.68f)
        val position = workspaceLabelPosition(icon, label, board, gap = 4.4f)
        assertTrue("Large name must not cover the bottom frame", position.y + label.bottom <= icon.top)
    }

    @Test
    fun name_of_element_near_left_edge_stays_on_board() {
        val position = workspaceLabelPosition(Rect(6f, 140f, 64f, 198f), label, board)
        assertTrue(position.x + label.left >= 0f)
    }

    @Test
    fun lifted_element_keeps_its_name_inside_frame_after_scale() {
        val icon = Rect(338f, 340f, 396f, 398f)
        val scale = 1.12f
        val position = workspaceLabelPosition(icon, label, board, scale)
        val right = icon.center.x + (position.x + label.right - icon.center.x) * scale
        val bottom = icon.center.y + (position.y + label.bottom - icon.center.y) * scale
        assertTrue("Lifted name must clear the right frame", right <= 400.01f)
        assertTrue("Lifted name must clear the bottom frame", bottom <= 400.01f)
    }

    @Test
    fun element_with_room_keeps_its_name_below_icon() {
        assertEquals(Offset(200f, 286f), workspaceLabelPosition(Rect(171f, 200f, 229f, 258f), label, board))
    }
}
