package me.leaver.rexpocket.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(name = "Phone", device = "spec:width=411dp,height=891dp")
@Preview(name = "Phone dark", device = "spec:width=411dp,height=891dp", uiMode = 0x21)
@Composable
fun HostEditorSsh() = RexTheme {
    HostDialog(
        initial = Fixtures.mac.copy(host = "mac.tail1234.ts.net", sshUser = "jaswant", sshPassword = "x", plaintext = false),
        tailscaleEnabled = true,
        onDismiss = {},
        onDelete = {},
        onSave = {},
        onJoinTailnet = {},
    )
}
