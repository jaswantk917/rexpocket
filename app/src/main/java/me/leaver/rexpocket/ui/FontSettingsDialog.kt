package me.leaver.rexpocket.ui

import android.graphics.Typeface
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.leaver.rexpocket.R
import me.leaver.rexpocket.data.TERMINAL_FAMILIES
import me.leaver.rexpocket.data.TerminalFontState
import me.leaver.rexpocket.data.terminalFamily

/** The terminal's font: the system's monospace, or one of the families downloaded on demand. */
@Composable
internal fun FontSettingsDialog(state: TerminalFontState, onSelect: (String?) -> Unit, onDismiss: () -> Unit) {
    val enabled = !state.initializing && state.downloading == null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.terminal_font_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.terminal_font_hint))
                FontOption(
                    title = stringResource(R.string.terminal_font_system),
                    detail = stringResource(R.string.terminal_font_system_detail),
                    preview = FontFamily.Monospace,
                    selected = state.selected == null,
                    enabled = enabled,
                ) { onSelect(null) }
                TERMINAL_FAMILIES.forEach { family ->
                    val face = state.available[family.id]
                    val status = when {
                        state.downloading == family.id -> stringResource(R.string.terminal_font_downloading, (state.progress * 100).toInt())
                        face != null -> stringResource(R.string.terminal_font_saved)
                        else -> stringResource(R.string.terminal_font_download, family.size)
                    }
                    val note = if (family.id == "meslo") stringResource(R.string.terminal_font_meslo_detail) else null
                    FontOption(
                        title = family.label,
                        detail = listOfNotNull(note, status).joinToString(" · "),
                        // A family is previewed in its own face once it is on the phone.
                        preview = face?.let { remember(it) { FontFamily(it) } },
                        selected = state.selected == family.id,
                        enabled = enabled,
                    ) { onSelect(family.id) }
                    if (state.downloading == family.id) {
                        LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                    }
                }
                if (state.initializing) Text(stringResource(R.string.terminal_font_loading))
                state.failed?.let { id ->
                    Text(stringResource(R.string.terminal_font_failed, terminalFamily(id)?.label.orEmpty()), color = Colors.danger)
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.terminal_font_credit), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) } },
    )
}

@Composable
private fun FontOption(
    title: String,
    detail: String,
    preview: FontFamily?,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(title)
            if (preview != null) {
                Text(PREVIEW, fontFamily = preview, maxLines = 1, overflow = TextOverflow.Clip, color = Colors.muted)
            }
            Text(detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Characters that tell monospace faces apart: zero and O, one, l and I, braces, and an arrow. */
private const val PREVIEW = "0O 1lI {} [] => ~/src \$ git"
