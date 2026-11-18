package me.leaver.rexpocket.ui

import androidx.window.core.layout.WindowSizeClass
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** When the list sits beside the terminal: the one part of the layout a computer can check. */
class AdaptiveTest {
    private fun window(width: Int, height: Int) = WindowSizeClass(width, height)

    @Test
    fun `a phone keeps one pane`() = assertFalse(roomForTwoPanes(window(411, 891)))

    @Test
    fun `a phone held sideways is wide but has no height for two panes`() = assertFalse(roomForTwoPanes(window(891, 411)))

    @Test
    fun `an unfolded foldable keeps one pane`() = assertFalse(roomForTwoPanes(window(673, 841)))

    @Test
    fun `a tablet has the list beside the terminal`() = assertTrue(roomForTwoPanes(window(1280, 800)))

    @Test
    fun `a desk window has the list beside the terminal`() = assertTrue(roomForTwoPanes(window(1920, 1080)))
}
