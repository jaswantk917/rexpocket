package me.leaver.rexpocket.ui

import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The phone keyboard covers the pane instead of resizing it, and the pane slides up only as far as
 * keeps the row being typed on in sight. A desk keyboard hides the key row.
 */
class KeyboardLayoutTest {
    private val pane = 2000

    @Test
    fun noKeyboardNoShift() {
        assertEquals(0, paneShift(caretBottom = pane, paneHeight = pane, covered = 0))
    }

    @Test
    fun aCaretAboveTheKeyboardStaysPut() {
        assertEquals(0, paneShift(caretBottom = 400, paneHeight = pane, covered = 900))
    }

    @Test
    fun aCaretUnderTheKeyboardSlidesJustAboveIt() {
        // The keyboard reaches 1100 px from the top; the caret row ends at 1500.
        assertEquals(400, paneShift(caretBottom = 1500, paneHeight = pane, covered = 900))
    }

    @Test
    fun theShiftNeverPassesTheKeyboard() {
        assertEquals(900, paneShift(caretBottom = pane, paneHeight = pane, covered = 900))
        assertEquals(900, paneShift(caretBottom = pane + 300, paneHeight = pane, covered = 900))
    }

    @Test
    fun theShiftFollowsTheKeyboardAsItAnimates() {
        val steps = listOf(0, 100, 300, 600, 900).map { paneShift(caretBottom = pane, paneHeight = pane, covered = it) }
        assertEquals(listOf(0, 100, 300, 600, 900), steps)
    }

    @Test
    fun anUnmeasuredPaneDoesNotShift() {
        assertEquals(0, paneShift(caretBottom = 0, paneHeight = 0, covered = 0))
    }

    @Test
    fun leftAloneTheKeyRowGivesWayToADeskKeyboard() {
        assertTrue(keyRowShown(choice = null, hardKeyboard = false))
        assertFalse(keyRowShown(choice = null, hardKeyboard = true))
    }

    @Test
    fun theKeyRowChoiceWinsEitherWay() {
        assertTrue(keyRowShown(choice = true, hardKeyboard = true))
        assertFalse(keyRowShown(choice = false, hardKeyboard = false))
    }

    @Test
    fun aConnectedDeskKeyboardIsSeen() {
        assertTrue(hasHardKeyboard(Configuration.KEYBOARD_QWERTY, Configuration.HARDKEYBOARDHIDDEN_NO))
    }

    @Test
    fun aPhoneWithoutOneOrWithItFoldedAwayIsNot() {
        assertFalse(hasHardKeyboard(Configuration.KEYBOARD_NOKEYS, Configuration.HARDKEYBOARDHIDDEN_YES))
        assertFalse(hasHardKeyboard(Configuration.KEYBOARD_QWERTY, Configuration.HARDKEYBOARDHIDDEN_YES))
        assertFalse(hasHardKeyboard(Configuration.KEYBOARD_12KEY, Configuration.HARDKEYBOARDHIDDEN_NO))
    }
}
