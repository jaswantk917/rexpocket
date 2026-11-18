package me.leaver.rexpocket.ui

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The desk keyboard's shortcuts, and the chords they must leave to the program in the pane. */
class ShortcutsTest {
    private fun ctrl(key: Int, shift: Boolean = false) = terminalShortcut(key, ctrl = true, shift = shift, alt = false, meta = false)

    @Test fun tabsAreWalkedAndJumpedTo() {
        assertEquals(TerminalShortcut.NextTab, ctrl(KeyEvent.KEYCODE_TAB))
        assertEquals(TerminalShortcut.PreviousTab, ctrl(KeyEvent.KEYCODE_TAB, shift = true))
        assertEquals(TerminalShortcut.Tab(0), ctrl(KeyEvent.KEYCODE_1))
        assertEquals(TerminalShortcut.Tab(8), ctrl(KeyEvent.KEYCODE_9))
        assertEquals(TerminalShortcut.NewTab, ctrl(KeyEvent.KEYCODE_T, shift = true))
    }

    @Test fun pasteAndTextSize() {
        assertEquals(TerminalShortcut.Paste, ctrl(KeyEvent.KEYCODE_V, shift = true))
        assertEquals(TerminalShortcut.Bigger, ctrl(KeyEvent.KEYCODE_EQUALS))
        assertEquals(TerminalShortcut.Smaller, ctrl(KeyEvent.KEYCODE_MINUS))
        assertEquals(TerminalShortcut.DefaultSize, ctrl(KeyEvent.KEYCODE_0))
    }

    @Test fun theShellsOwnChordsStillReachIt() {
        // Ctrl+C, Ctrl+T, Ctrl+V and Ctrl+R mean something to the program; Alt and Meta chords too.
        listOf(KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_T, KeyEvent.KEYCODE_V, KeyEvent.KEYCODE_R).forEach { assertNull(ctrl(it)) }
        assertNull(terminalShortcut(KeyEvent.KEYCODE_TAB, ctrl = false, shift = false, alt = false, meta = false))
        assertNull(terminalShortcut(KeyEvent.KEYCODE_1, ctrl = true, shift = false, alt = true, meta = false))
        assertNull(terminalShortcut(KeyEvent.KEYCODE_1, ctrl = true, shift = false, alt = false, meta = true))
    }

    @Test fun textSizeStepsStayInTheirRange() {
        assertEquals(1.1f, stepFontScale(1f, bigger = true))
        assertEquals(0.9f, stepFontScale(1f, bigger = false))
        assertEquals(MAX_FONT_SCALE, stepFontScale(MAX_FONT_SCALE, bigger = true))
        assertEquals(MIN_FONT_SCALE, stepFontScale(MIN_FONT_SCALE, bigger = false))
    }
}
