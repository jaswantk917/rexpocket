package me.leaver.rexpocket.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import me.leaver.rexpocket.core.PocketState
import me.leaver.rexpocket.core.isWebLink
import me.leaver.rexpocket.R
import kotlinx.coroutines.launch

// Bottom sheets: the session switcher and the grouped action menus.

/**
 * Every session on this Rex, nothing more. Picking one opens it on the pane the phone showed
 * there last time; the tab bar and pane strip take over from there.
 */
@Composable
internal fun SessionSheet(
    state: PocketState,
    onDismiss: () -> Unit,
    onOpen: (String) -> Unit,
    onCreate: () -> Unit,
) {
    ActionSheet(onDismiss = onDismiss, title = stringResource(R.string.sessions)) {
        Group(icons = false) {
            state.sessions.forEach { session ->
                val active = session.id == state.activeSessionId
                val tabs = state.views[session.id]?.windows?.size
                SheetItem(
                    label = session.label,
                    detail = tabs?.let { pluralStringResource(R.plurals.session_window_count, it, it) },
                    marked = active,
                ) { onOpen(session.id) }
            }
        }
        Group { SheetItem(stringResource(R.string.session_new), Colors.accent, icon = R.drawable.ic_add, onClick = onCreate) }
    }
}

/**
 * A link long-pressed in the pane: open it, copy it or share it. Only a web link can be opened; any
 * other address a program printed is still there to copy, but is not handed to another app.
 */
@Composable
internal fun LinkSheet(url: String, onDismiss: () -> Unit, onOpen: () -> Unit, onCopy: () -> Unit, onShare: () -> Unit) {
    ActionSheet(onDismiss = onDismiss, title = url) {
        Group {
            if (isWebLink(url)) SheetItem(stringResource(R.string.link_open), Colors.accent, icon = R.drawable.ic_open_in_new, onClick = onOpen)
            SheetItem(stringResource(R.string.link_copy), icon = R.drawable.ic_copy, onClick = onCopy)
            if (isWebLink(url)) SheetItem(stringResource(R.string.link_share), icon = R.drawable.ic_share, onClick = onShare)
        }
    }
}

/**
 * Lets a sheet row close its sheet. The row's action runs once the sheet has gone: a dialog it opens
 * would otherwise ask for the keyboard while the sheet's window still holds it, and lose it when that
 * window is removed.
 */
internal class SheetScope(val close: (() -> Unit) -> Unit)

/**
 * Android's modal bottom sheet in the scheme's low container: the platform's drag handle, an
 * emphasized title, and rows in segmented groups - the same grammar as the host list and settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActionSheet(onDismiss: () -> Unit, title: String? = null, content: @Composable SheetScope.() -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val rows = SheetScope { action ->
        scope.launch { state.hide() }.invokeOnCompletion {
            onDismiss()
            action()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            if (title != null) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
                )
            }
            rows.content()
        }
    }
}

/**
 * A segmented group of rows with an optional caption in the accent: rounded at its ends, the sheet
 * showing through 2 dp gaps between rows, no hairlines.
 */
@Composable
internal fun SheetScope.Group(
    label: String? = null,
    @Suppress("UNUSED_PARAMETER") icons: Boolean = true,
    content: @Composable SheetScope.() -> Unit,
) {
    if (label != null) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, top = 20.dp, bottom = 8.dp),
        )
    } else {
        Spacer(Modifier.height(12.dp))
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) { content() }
}

/**
 * One row of a sheet: an icon, the label, a detail or a check at the end, 56 dp high. The one that
 * is current sits in the secondary container.
 *
 * A row that only changes what its own detail shows - a switch, a pick between two - stays open
 * ([keepOpen]): closing the sheet would take away the one place the change is visible.
 */
@Composable
internal fun SheetScope.SheetItem(
    label: String,
    color: Color = Colors.ink,
    detail: String? = null,
    marked: Boolean = false,
    icon: Int? = null,
    keepOpen: Boolean = false,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val ink = if (marked) scheme.onSecondaryContainer else color
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .pressable(RoundedCornerShape(4.dp), onClick = { if (keepOpen) onClick() else close(onClick) })
            .background(if (marked) scheme.secondaryContainer else scheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Glyph(icon, if (color == Colors.ink) scheme.onSurfaceVariant else color, size = 24.dp)
            Spacer(Modifier.width(16.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = ink,
            fontWeight = if (marked) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (detail != null) {
            Spacer(Modifier.width(12.dp))
            Text(detail, style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant, maxLines = 1)
        }
        if (marked) {
            Spacer(Modifier.width(12.dp))
            Glyph(R.drawable.ic_check, scheme.onSecondaryContainer, size = 20.dp)
        }
    }
}
