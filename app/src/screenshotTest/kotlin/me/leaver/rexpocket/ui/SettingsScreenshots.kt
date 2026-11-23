package me.leaver.rexpocket.ui

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import me.leaver.rexpocket.core.UpdateChannel
import me.leaver.rexpocket.data.Appearance
import me.leaver.rexpocket.update.UpdateState

@PreviewTest
@FormFactorPreviews
@Composable
fun SettingsPage() = RexTheme(Appearance(scheme = SchemeSeed.Rex.id)) {
    SettingsScreen(
        SettingsModel(
            appearance = Appearance(scheme = SchemeSeed.Rex.id),
            onAppearance = {},
            tailnet = "Connected",
            onTailscale = {},
            alerts = true,
            onAlerts = {},
            keyBar = false,
            onKeyBar = {},
            keyBarShift = false,
            keyBarAlt = false,
            onKeyBarShift = {},
            onKeyBarAlt = {},
            liveStatus = true,
            onLiveStatus = {},
            terminalFont = "Meslo LG NF",
            onTerminalFont = {},
            recording = false,
            onDiagnostics = {},
            update = UpdateState.UpToDate,
            onUpdate = {},
            channel = UpdateChannel.Stable,
            onChannel = {},
            onSource = {},
        ),
        onBack = {},
    )
}
