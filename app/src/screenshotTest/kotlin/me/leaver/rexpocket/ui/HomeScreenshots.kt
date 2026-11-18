package me.leaver.rexpocket.ui

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import me.leaver.rexpocket.core.PocketState
import me.leaver.rexpocket.data.SavedHost
import me.leaver.rexpocket.data.TailscaleSettings

@Composable
private fun Home(hosts: List<SavedHost>, connected: SavedHost?, state: PocketState) {
    RexTheme {
        HomeScreen(
            hosts = hosts,
            connected = connected,
            state = state,
            tailscale = TailscaleSettings(),
            tailnetPath = null,
            opening = null,
            cachedSessions = { emptyList() },
            onConnect = {},
            onOpenSession = { _, _ -> },
            onOpenTab = { _, _, _ -> },
            onNewSession = {},
            onNewTab = { _, _ -> },
            onDisconnect = {},
            onRetry = {},
            onProbePath = {},
            onSave = {},
            onDelete = {},
            onTailscale = {},
            onSettings = {},
        )
    }
}

@PreviewTest
@FormFactorPreviews
@Composable
fun HostsLive() = Home(listOf(Fixtures.mac, Fixtures.pi), Fixtures.mac, Fixtures.live)

@PreviewTest
@FormFactorPreviews
@Composable
fun HostsEmpty() = Home(emptyList(), null, PocketState())
