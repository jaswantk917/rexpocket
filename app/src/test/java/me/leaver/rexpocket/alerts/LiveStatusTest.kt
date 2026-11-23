package me.leaver.rexpocket.alerts

import me.leaver.rexpocket.core.Problem
import me.leaver.rexpocket.core.ProgramRecord
import me.leaver.rexpocket.core.Trouble
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveStatusTest {
    private fun record(block: String, state: String) = ProgramRecord(block, state, "agent", "claude", null, null, null)

    @Test fun nothingRunningShowsNoLiveStatus() {
        assertNull(liveStatusOf(emptyMap()) { it })
        assertNull(liveStatusOf(mapOf("a" to record("a", "idle"))) { it })
    }

    @Test fun aWaitingPaneLeadsOverAWorkingOne() {
        val status = mapOf("a" to record("a", "working"), "b" to record("b", "blocked"), "c" to record("c", "working"))
        assertEquals(LiveStatus(working = 2, waiting = 1, lead = "b"), liveStatusOf(status) { it })
    }

    @Test fun aPaneWithoutANameDoesNotLead() {
        val status = mapOf("a" to record("a", "blocked"), "b" to record("b", "working"))
        assertEquals("b", liveStatusOf(status) { block -> block.takeIf { it == "b" } }?.lead)
    }

    @Test fun aBlipLeavesTheNotificationAlone() {
        assertEquals(WatchLink.Up, watchLinkOf(down = false, Problem(Trouble.Dropped, null)))
    }

    @Test fun aDropThatLastsIsReconnecting() {
        assertEquals(WatchLink.Reconnecting, watchLinkOf(down = true, Problem(Trouble.Unreachable, null)))
        assertEquals(WatchLink.Reconnecting, watchLinkOf(down = true, null))
    }

    @Test fun aProblemNoRetryCanFixNeedsThePerson() {
        for (trouble in listOf(Trouble.SshAuth, Trouble.SshCommand, Trouble.SshHostKeyChanged, Trouble.SshHostKeyUnknown)) {
            assertEquals(WatchLink.NeedsPerson, watchLinkOf(down = true, Problem(trouble, null)))
        }
    }
}
