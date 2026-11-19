package me.leaver.rexpocket.ui

import android.graphics.Typeface
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.android.tools.screenshot.PreviewTest
import kotlinx.coroutines.flow.emptyFlow
import me.leaver.rexpocket.data.TailscaleSettings

@Preview(name = "Fold open", device = "spec:width=841dp,height=701dp,dpi=420")
@Preview(name = "Fold open dark", device = "spec:width=841dp,height=701dp,dpi=420", uiMode = 0x21)
@Preview(name = "Tablet", device = "spec:width=1280dp,height=800dp,dpi=240")
@Preview(name = "Desktop", device = "spec:width=1920dp,height=1080dp,dpi=160")
annotation class WidePreviews

@PreviewTest
@WidePreviews
@Composable
fun ListBesideTerminal() {
    RexTheme {
        val directive = rememberPaneDirective()
        val strategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)
        NavDisplay(
            backStack = listOf<NavKey>(HostsKey, TerminalKey),
            onBack = {},
            sceneStrategies = listOf(strategy),
            entryProvider = entryProvider {
                entry<HostsKey>(metadata = ListDetailSceneStrategy.listPane()) {
                    HomeScreen(
                        hosts = listOf(Fixtures.mac, Fixtures.pi), connected = Fixtures.mac, state = Fixtures.live, detail = true,
                        tailscale = TailscaleSettings(), tailnetPath = null, opening = null, cachedSessions = { emptyList() },
                        onConnect = {}, onOpenSession = { _, _ -> }, onOpenTab = { _, _, _ -> }, onNewSession = {}, onNewTab = { _, _ -> },
                        onDisconnect = {}, onRetry = {}, onProbePath = {}, onSave = {}, onDelete = {}, onTailscale = {}, onSettings = {},
                    )
                }
                entry<TerminalKey>(metadata = ListDetailSceneStrategy.detailPane()) {
                    PaneCard { TermScreen(
                        state = Fixtures.live, fontScale = 1f, terminalTypeface = Typeface.MONOSPACE, onFont = {}, onBack = {},
                        onOpenSession = {}, watchPane = { emptyFlow() }, onRetry = {}, onDismissNotice = {}, onAct = {},
                        upload = null, onAttach = {}, onCancelUpload = {}, onDismissUpload = {}, onRetryUpload = {},
                        keyBarWithKeyboard = false, embedded = true,
                    ) }
                }
            },
        )
    }
}
