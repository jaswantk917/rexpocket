package me.leaver.rexpocket.ui

import me.leaver.rexpocket.core.UploadPhase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UploadStatusTest {
    /** A paste landing between two files must not replace the ones still waiting. */
    @Test fun aSentFileWithMoreBehindItIsStillRunning() {
        assertTrue(UploadStatus("a.png", UploadPhase.Done, waiting = 1).running)
    }

    @Test fun theLastSentFileIsDone() {
        assertFalse(UploadStatus("a.png", UploadPhase.Done).running)
    }

    @Test fun aFailureStopsTheQueue() {
        assertFalse(UploadStatus("a.png", UploadPhase.Failed, waiting = 1).running)
    }
}
