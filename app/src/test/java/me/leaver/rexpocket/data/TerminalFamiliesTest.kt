package me.leaver.rexpocket.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every downloadable family is pinned, checked and complete, and saved where no other one is. */
class TerminalFamiliesTest {
    @Test fun idsAndDirectoriesAreUnique() {
        assertEquals(TERMINAL_FAMILIES.size, TERMINAL_FAMILIES.map { it.id }.toSet().size)
        assertEquals(TERMINAL_FAMILIES.size, TERMINAL_FAMILIES.map { it.directory }.toSet().size)
    }

    @Test fun everySourceIsAPinnedCommit() {
        TERMINAL_FAMILIES.forEach { family ->
            assertTrue(family.id, Regex("^https://raw\\.githubusercontent\\.com/[^/]+/[^/]+/[0-9a-f]{40}(/.*)?$").matches(family.source))
        }
    }

    @Test fun everyFaceIsDownloadedAndChecked() {
        TERMINAL_FAMILIES.forEach { family ->
            val names = family.assets.map { it.name }.toSet()
            listOf(family.regular, family.bold, family.italic, family.boldItalic).forEach { assertTrue("${family.id}: $it", it in names) }
            family.assets.forEach { assertTrue("${family.id}: ${it.name}", Regex("^[0-9a-f]{64}$").matches(it.sha256)) }
        }
    }

    @Test fun theFirstVersionsDownloadIsStillFound() {
        assertEquals("jetbrains-mono-v1", terminalFamily("jetbrains")?.directory)
        assertNull(terminalFamily(null))
        assertNull(terminalFamily("missing"))
    }
}
