package me.leaver.rexpocket.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MorphKeyTest {
    @Test fun theCardItOpenedFromStaysWhileItsTabIsShown() {
        assertNull(followedMorphKey(tabMorphKey("s", "w"), "s", "w"))
        assertNull(followedMorphKey(shelfMorphKey("s", "w"), "s", "w"))
        assertNull(followedMorphKey(sessionMorphKey("s"), "s", "w"))
    }

    @Test fun movingToAnotherTabLandsOnThatTabsRow() {
        assertEquals(tabMorphKey("s", "w2"), followedMorphKey(tabMorphKey("s", "w1"), "s", "w2"))
        assertEquals(tabMorphKey("s2", "w"), followedMorphKey(sessionMorphKey("s1"), "s2", "w"))
    }

    @Test fun aNewSessionLandsOnItsRowOnceItExists() {
        assertEquals(tabMorphKey("s", "w"), followedMorphKey(NEW_SESSION_MORPH, "s", "w"))
    }

    @Test fun nothingToFollowWithoutAnOpenTab() {
        assertNull(followedMorphKey(NEW_SESSION_MORPH, null, "w"))
        assertNull(followedMorphKey(NEW_SESSION_MORPH, "s", null))
    }
}
