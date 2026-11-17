package me.leaver.rexpocket.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import me.leaver.rexpocket.core.ProgramRecord
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.leaver.rexpocket.R
import me.leaver.rexpocket.core.PocketState
import me.leaver.rexpocket.core.WindowInfo

/**
 * The sessions of the connected host down the side of a wide window, as the Rex desktop app lays
 * them out: each session's name over its tabs, the tab on screen marked, and a tab one tap away. On
 * a tablet or a desktop window this takes the place of going back to the list to change tabs.
 */
@Composable
internal fun SessionSidebar(
    state: PocketState,
    hostLabel: String,
    onHosts: () -> Unit,
    onOpenTab: (sessionId: String, windowId: String) -> Unit,
    onOpenSession: (String) -> Unit,
    onNewTab: (String) -> Unit,
    onNewSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxHeight().background(Colors.bg).statusBarsPadding().navigationBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(TOP_BAR).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundButton(R.drawable.ic_back, stringResource(R.string.action_back), onHosts, tint = Colors.muted, size = 36.dp, iconSize = 20.dp)
            Spacer(Modifier.width(4.dp))
            Text(
                hostLabel.ifBlank { state.endpointLabel },
                color = Colors.ink,
                fontSize = TextStyles.subhead,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(8.dp))
            Dot(phaseTint(state.phase), 6.dp)
            Spacer(Modifier.weight(1f))
            RoundButton(R.drawable.ic_add, stringResource(R.string.session_new), onNewSession, tint = Colors.muted, size = 36.dp, iconSize = 20.dp)
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 16.dp)) {
            state.sessions.forEach { session ->
                val view = state.views[session.id]
                val active = session.id == state.activeSessionId
                item(key = "s-${session.id}") {
                    SessionLabel(session.label, always = active, onNewTab = { onNewTab(session.id) })
                }
                if (view == null) {
                    item(key = "o-${session.id}") {
                        SidebarRow(
                            title = stringResource(R.string.session_open_to_load),
                            program = null,
                            selected = false,
                            status = null,
                            muted = true,
                            onClick = { onOpenSession(session.id) },
                        )
                    }
                } else {
                    view.windows.forEach { window ->
                        item(key = "w-${session.id}-${window.id}") {
                            SidebarRow(
                                title = tabTitle(view, window, state),
                                program = tabProgram(window, state),
                                selected = active && window.id == state.view?.activeWindowId,
                                status = window.activity(state),
                                onClick = { onOpenTab(session.id, window.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** What a tab's programs are doing, the one waiting on a person first: that is the one to go to. */
private fun WindowInfo.activity(state: PocketState): ProgramRecord? {
    val records = blocks.mapNotNull { state.blockStatus[it.id] }
    return records.firstOrNull { it.state == "blocked" } ?: records.firstOrNull { it.state == "working" }
}

/**
 * A session's name over its tabs. Its new-tab button shows on the session on screen, and on the
 * others while a pointer is over them, as on the desktop, instead of a column of plus signs.
 */
@Composable
private fun SessionLabel(label: String, always: Boolean, onNewTab: () -> Unit) {
    val hover = remember { MutableInteractionSource() }
    val hovered by hover.collectIsHoveredAsState()
    Row(
        Modifier.fillMaxWidth().hoverable(hover).padding(start = 12.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = Colors.muted,
            fontSize = TextStyles.label,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        // The slot is kept while the button is away, so the label never shifts under the pointer.
        Box(Modifier.size(32.dp)) {
            androidx.compose.animation.AnimatedVisibility(visible = always || hovered, enter = fadeIn(), exit = fadeOut()) {
                RoundButton(R.drawable.ic_add, stringResource(R.string.menu_new_tab), onNewTab, tint = Colors.muted, size = 32.dp, iconSize = 16.dp)
            }
        }
    }
}

@Composable
private fun SidebarRow(
    title: String,
    program: String?,
    selected: Boolean,
    status: ProgramRecord?,
    onClick: () -> Unit,
    muted: Boolean = false,
) {
    val plate by animateColorAsState(if (selected) Colors.key else Colors.clear, label = "sidebar-row")
    Row(
        Modifier.fillMaxWidth().height(ROW).clip(RoundedCornerShape(10.dp)).background(plate)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (program != null || !muted) {
            Box {
                ProgramTile(program, size = 22.dp)
                // Green while it works, amber while it waits on you: the same colours as the title bar.
                if (status != null) Dot(statusTint(status), 7.dp, Modifier.align(Alignment.TopEnd), ring = Colors.bg)
            }
            Spacer(Modifier.width(10.dp))
        }
        Text(
            title,
            color = if (muted) Colors.muted else Colors.ink,
            fontSize = TextStyles.subhead,
            // One weight for every row; the plate marks the tab on screen. A heavier one on top of
            // the system's bold-text setting turns the selected row nearly black.
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** As tall as the terminal's compact title bar, so the two read as one line across the window. */
private val TOP_BAR = 48.dp
private val ROW = 40.dp
