package me.leaver.rexpocket.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipFilesTest {
    @Test fun aCopiedScreenshotIsAFile() {
        assertTrue(isClipFile("content", hasText = false))
    }

    @Test fun aWebLinkStaysText() {
        assertFalse(isClipFile("https", hasText = false))
    }

    @Test fun anItemWithTextStaysText() {
        assertFalse(isClipFile("content", hasText = true))
    }

    @Test fun plainTextIsNotAFile() {
        assertFalse(isClipFile(null, hasText = true))
    }
}
