package me.leaver.rexpocket.ui

import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import me.leaver.rexpocket.core.BlockInfo
import me.leaver.rexpocket.core.ListedSession
import me.leaver.rexpocket.core.Phase
import me.leaver.rexpocket.core.PocketState
import me.leaver.rexpocket.core.ProgramRecord
import me.leaver.rexpocket.core.SessionView
import me.leaver.rexpocket.core.WindowInfo
import me.leaver.rexpocket.data.SavedHost

/** The form factors the adaptive guidance names: phone, foldable, tablet, desktop; light and dark. */
@Preview(name = "Phone", device = Devices.PHONE, showBackground = true)
@Preview(name = "Phone dark", device = Devices.PHONE, showBackground = true, uiMode = 0x21)
@Preview(name = "Phone 1.3", device = Devices.PHONE, showBackground = true, fontScale = 1.3f)
@Preview(name = "Foldable", device = Devices.FOLDABLE, showBackground = true)
@Preview(name = "Tablet", device = Devices.TABLET, showBackground = true)
@Preview(name = "Desktop", device = Devices.DESKTOP, showBackground = true)
annotation class FormFactorPreviews

internal object Fixtures {
    val mac = SavedHost(id = "mac", label = "mac", host = "100.64.0.2", port = 7297, plaintext = true, viaTailscale = true)
    val pi = SavedHost(id = "pi", label = "build-pi", host = "pi.local", port = 7297, plaintext = true)

    private fun block(id: String, label: String) = BlockInfo(id, label, creator = null, flavor = null, owner = null)
    private fun tab(id: String, label: String, vararg blocks: BlockInfo) = WindowInfo(id, label, blocks.firstOrNull()?.id, null, blocks.toList())

    private val reviewer = SessionView(
        revision = 1, sessionId = "s1", label = "pr-fixer-loop", activeWindowId = "w1",
        windows = listOf(
            tab("w1", "Flutter PR reviewer", block("b1", "claude")),
            tab("w2", "Tab 2", block("b2", "zsh"), block("b3", "htop")),
        ),
    )
    private val issues = SessionView(
        revision = 1, sessionId = "s2", label = "rexpocket-issues", activeWindowId = "w3",
        windows = listOf(
            tab("w3", "Rexpocket keyboard and tablet", block("b4", "claude")),
            tab("w4", "Tab 2", block("b5", "zsh")),
            tab("w5", "Tab 3", block("b6", "zsh")),
        ),
    )

    val live = PocketState(
        phase = Phase.Live,
        endpointLabel = "100.64.0.2:7297",
        sessions = listOf(ListedSession("s1", "pr-fixer-loop"), ListedSession("s2", "rexpocket-issues"), ListedSession("s3", "gateway-origin-lock")),
        activeSessionId = "s1",
        view = reviewer,
        views = mapOf("s1" to reviewer, "s2" to issues),
        blockStatus = mapOf(
            "b1" to ProgramRecord("b1", "working", "agent", "claude", "Reviewing #405", null, null),
            "b4" to ProgramRecord("b4", "blocked", "agent", "claude", "Needs your OK", null, null),
        ),
        programs = mapOf("b1" to "claude", "b4" to "claude", "b2" to "zsh", "b3" to "htop", "b5" to "zsh", "b6" to "zsh"),
        pwd = "/Users/me/Development",
        owning = true,
    )
}
