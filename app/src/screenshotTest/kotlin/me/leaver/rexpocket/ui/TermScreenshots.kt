package me.leaver.rexpocket.ui

import android.graphics.Typeface
import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import kotlinx.coroutines.flow.emptyFlow
import me.leaver.rexpocket.core.ListedSession
import me.leaver.rexpocket.core.PocketState

@Composable
private fun Term(state: PocketState, keyBar: Boolean = false, embedded: Boolean = false) {
    RexTheme {
        TermScreen(
            state = state,
            fontScale = 1f,
            terminalTypeface = Typeface.MONOSPACE,
            onFont = {},
            onBack = {},
            onOpenSession = {},
            watchPane = { emptyFlow() },
            onRetry = {},
            onDismissNotice = {},
            onAct = {},
            upload = null,
            onAttach = { _, _ -> },
            onCancelUpload = {},
            onDismissUpload = {},
            onRetryUpload = {},
            keyBarWithKeyboard = keyBar,
            embedded = embedded,
        )
    }
}

@PreviewTest
@FormFactorPreviews
@Composable
fun TerminalLive() = Term(Fixtures.live)

@PreviewTest
@FormFactorPreviews
@Composable
fun TerminalTwoPanes() = Term(Fixtures.live.copy(activeSessionId = "s1", view = Fixtures.live.views["s1"]!!.copy(activeWindowId = "w2")))

@PreviewTest
@FormFactorPreviews
@Composable
fun TerminalManySessions() = Term(
    Fixtures.live.copy(
        sessions = Fixtures.live.sessions + listOf(
            ListedSession("s3", "a-session-with-a-very-long-name"),
            ListedSession("s4", "deploy"),
            ListedSession("s5", "logs"),
        ),
    ),
)
