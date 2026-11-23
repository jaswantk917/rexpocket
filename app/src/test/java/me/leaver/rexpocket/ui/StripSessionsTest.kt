package me.leaver.rexpocket.ui

import me.leaver.rexpocket.core.ListedSession
import me.leaver.rexpocket.core.SessionView
import org.junit.Assert.assertEquals
import org.junit.Test

class StripSessionsTest {
    private val listed = listOf(ListedSession("a", "one"), ListedSession("b", "two"))
    private fun view(id: String) = SessionView(revision = 1, sessionId = id, label = id, activeWindowId = "w", windows = emptyList())

    @Test fun keepsRexsOrderWhenTheOpenSessionIsListed() {
        assertEquals(listOf("a", "b"), stripSessions(listed, view("b")).map { it.id })
    }

    @Test fun showsTheOpenSessionBeforeTheListHasIt() {
        assertEquals(listOf("new", "a", "b"), stripSessions(listed, view("new")).map { it.id })
    }

    @Test fun withNothingOpenItIsJustTheList() {
        assertEquals(listed, stripSessions(listed, null))
    }

    @Test fun aChipAlreadyInViewDoesNotMoveTheStrip() {
        assertEquals(100, scrollToShow(current = 100, viewport = 400, start = 200, end = 300, margin = 20))
    }

    @Test fun aChipPastTheEndScrollsJustFarEnough() {
        assertEquals(320, scrollToShow(current = 0, viewport = 400, start = 600, end = 700, margin = 20))
    }

    @Test fun aChipBeforeTheStartScrollsBackClearOfTheFade() {
        assertEquals(80, scrollToShow(current = 300, viewport = 400, start = 100, end = 200, margin = 20))
    }

    @Test fun aChipWiderThanTheViewShowsItsStart() {
        assertEquals(480, scrollToShow(current = 0, viewport = 400, start = 500, end = 950, margin = 20))
    }

    @Test fun neverScrollsBeforeTheFirstChip() {
        assertEquals(0, scrollToShow(current = 50, viewport = 400, start = 8, end = 100, margin = 20))
    }
}
