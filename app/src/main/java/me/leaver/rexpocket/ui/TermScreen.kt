package me.leaver.rexpocket.ui

import android.os.SystemClock
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import me.leaver.rexpocket.terminal.TerminalSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt
import me.leaver.rexpocket.R
import me.leaver.rexpocket.core.BlockInfo
import me.leaver.rexpocket.core.KeyModifiers
import me.leaver.rexpocket.core.KeyPress
import me.leaver.rexpocket.core.keyPressFor
import me.leaver.rexpocket.core.Phase
import me.leaver.rexpocket.core.PocketSession
import me.leaver.rexpocket.core.PocketState
import me.leaver.rexpocket.core.ProgramMark
import me.leaver.rexpocket.core.programMark
import me.leaver.rexpocket.core.ProgramRecord
import me.leaver.rexpocket.core.SessionView
import me.leaver.rexpocket.core.TerminalFeed
import me.leaver.rexpocket.core.UploadPhase
import me.leaver.rexpocket.core.WindowInfo
import me.leaver.rexpocket.core.toProblem

// The terminal, built like the reference: a floating top bar that says what this is and who holds
// it, the pane edge to edge on its own paper, and a floating dock under the thumb.

/**
 * The line under the header. A connection that is down stays there with a way to retry; an action
 * that failed shows for a few seconds and goes, or goes when tapped, so it never outlives its cause.
 */
@Composable
internal fun TermBanner(state: PocketState, onRetry: () -> Unit, onDismiss: (Long) -> Unit) {
    val problem = rememberSettledProblem(state.phase, state.problem.takeIf { state.phase != Phase.Live })
    val notice = state.notice
    LaunchedEffect(notice?.id) {
        if (notice != null) {
            delay(NOTICE_MS)
            onDismiss(notice.id)
        }
    }
    val (text, action) = when {
        problem != null -> problemText(problem, state.endpointLabel) to onRetry
        notice != null -> problemText(notice.problem, state.endpointLabel) to { onDismiss(notice.id) }
        else -> return
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = action).padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = Colors.danger, fontSize = TextStyles.label, modifier = Modifier.weight(1f))
        Text(
            stringResource(if (problem != null) R.string.action_retry_now else R.string.action_dismiss),
            color = if (problem != null) Colors.accent else Colors.muted,
            fontSize = TextStyles.label,
        )
    }
}

/** Something long-pressed or picked from the menu that needs a rename or a confirmation. */
internal data class Rename(val kind: Level, val id: String, val current: String)

internal data class Confirm(val kind: Level, val id: String, val name: String)

internal enum class Level { Session, Tab, Pane }

/** The bottom sheet open over the terminal. */
internal sealed interface Sheet {
    data object Sessions : Sheet
    data object Actions : Sheet
    data object Navigator : Sheet
    data class TabActions(val windowId: String) : Sheet
    data class PaneActions(val blockId: String) : Sheet
}

@Composable
internal fun TermScreen(
    state: PocketState,
    fontScale: Float,
    terminalTypeface: android.graphics.Typeface,
    onFont: (Float) -> Unit,
    onBack: () -> Unit,
    onOpenSession: (String) -> Unit,
    watchPane: (String) -> Flow<TerminalFeed>,
    onRetry: () -> Unit,
    onDismissNotice: (Long) -> Unit,
    onAct: (suspend PocketSession.() -> Unit) -> Unit,
    upload: UploadStatus?,
    onAttach: (android.net.Uri) -> Unit,
    onCancelUpload: () -> Unit,
    onDismissUpload: () -> Unit,
    onRetryUpload: () -> Unit,
    /** Beside the session sidebar of a wide window: a one-line title bar, and no way back to draw. */
    compact: Boolean = false,
    /** Whether the person showed (true) or hid (false) the key row; null leaves it to the keyboard. */
    keyRowChoice: Boolean? = null,
    onKeyRowChoice: (Boolean) -> Unit = {},
    /** Opens or folds the sidebar beside a compact terminal; null where there is none. */
    onToggleSidebar: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val ime = LocalSoftwareKeyboardController.current
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onAttach(uri)
    }
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var mods by remember { mutableStateOf(KeyModifiers()) }
    var link by remember { mutableStateOf<String?>(null) }
    var rename by remember { mutableStateOf<Rename?>(null) }
    var confirm by remember { mutableStateOf<Confirm?>(null) }
    var termSession by remember { mutableStateOf<TerminalSession?>(null) }
    // The pane's editor: what the keyboard key raises, and what Back and the session list drop.
    var keyboard by remember { mutableStateOf<TerminalImeView?>(null) }
    val view = state.view
    val tab = view?.activeWindow()
    val focused = view?.focusedBlock()
    val status = focused?.let { state.blockStatus[it.id] }
    val panes = tab?.blocks.orEmpty()
    // A desk keyboard brings its own Escape, Ctrl, Tab and arrows, so the key row only takes room.
    val configuration = LocalConfiguration.current
    val hardKeyboard = hasHardKeyboard(configuration.keyboard, configuration.hardKeyboardHidden)
    val keyRowShown = keyRowShown(keyRowChoice, hardKeyboard)

    // The phone keyboard covers the pane instead of shrinking it: a smaller pane would be a resize
    // on the desktop, and a redraw of the program in it, every time the keyboard comes or goes. So
    // the pane keeps its size, the controls ride up on the keyboard, and the pane slides up only as
    // far as it takes to keep the row being typed on in sight. Both follow the keyboard's own
    // animation, read at placement, so nothing is measured again while it moves.
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val navInsets = WindowInsets.navigationBars
    val lift = { (imeInsets.getBottom(density) - navInsets.getBottom(density)).coerceAtLeast(0) }
    var paneHeight by remember { mutableIntStateOf(0) }
    var caretBottom by remember { mutableIntStateOf(0) }
    val paneShift = { paneShift(caretBottom, paneHeight, covered = lift()) }

    fun pasteClipboard() {
        val text = clipboard(context)
        if (text.isNullOrEmpty()) toast(context, context.getString(R.string.toast_clipboard_empty)) else onAct { paste(text) }
    }

    /** Sends a key with the modifiers the key row holds; the ones on for one key are then used up. */
    fun press(key: KeyPress, own: String = "") {
        val e = mods.event(key, own)
        onAct { sendKey(e.code, mods = e.mods, text = e.text, codepoint = e.codepoint, consumedMods = e.consumedMods) }
        mods = mods.consume()
    }

    /**
     * A character from the phone keyboard while a modifier is on becomes that key with the modifier,
     * so C-r or Alt-b can be typed with the keyboard's letters. True when it was taken here.
     */
    fun typed(bytes: ByteArray, flushFirst: () -> Unit): Boolean {
        if (!mods.any) return false
        val key = keyPressFor(String(bytes, Charsets.UTF_8)) ?: return false
        flushFirst()
        press(key)
        return true
    }

    // Typing goes straight to the terminal, as it does on the desktop: the keyboard key raises the
    // IME for the pane, and the key row stays above it. There is no composer between them.
    fun toggleKeyboard() {
        val target = keyboard ?: return
        val ime = context.getSystemService(InputMethodManager::class.java)
        // Whether the keyboard is up, not whether the pane has focus: the IME can be showing while
        // focus sits in its own window, and then the same key must put it away.
        val showing = ViewCompat.getRootWindowInsets(target)?.isVisible(WindowInsetsCompat.Type.ime()) == true
        if (showing) {
            ime?.hideSoftInputFromWindow(target.windowToken, 0)
            target.clearFocus()
        } else {
            target.showIme()
        }
    }

    fun run(shortcut: TerminalShortcut) {
        val windows = view?.windows.orEmpty()
        val at = windows.indexOfFirst { it.id == view?.activeWindowId }
        fun goTo(index: Int) {
            val target = windows.getOrNull(index) ?: return
            if (target.id != view?.activeWindowId) onAct { focusWindow(target.id) }
        }
        when (shortcut) {
            TerminalShortcut.NextTab -> if (windows.isNotEmpty()) goTo((at + 1).mod(windows.size))
            TerminalShortcut.PreviousTab -> if (windows.isNotEmpty()) goTo((at - 1).mod(windows.size))
            is TerminalShortcut.Tab -> goTo(shortcut.index)
            TerminalShortcut.NewTab -> onAct { newWindow() }
            TerminalShortcut.Paste -> pasteClipboard()
            TerminalShortcut.Bigger -> onFont(stepFontScale(fontScale, bigger = true))
            TerminalShortcut.Smaller -> onFont(stepFontScale(fontScale, bigger = false))
            TerminalShortcut.DefaultSize -> onFont(1f)
        }
    }

    // Keys arrived from a keyboard: from then on a newly shown pane takes the focus, so Ctrl+Tab
    // twice, or typing straight after it, does not need a click on the new pane first.
    var keysTyped by remember { mutableStateOf(false) }

    // Set on every composition, so a shortcut always acts on the tabs and size on screen now.
    SideEffect {
        keyboard?.onShortcut = { event ->
            keysTyped = true
            val shortcut = terminalShortcut(event.keyCode, event.isCtrlPressed, event.isShiftPressed, event.isAltPressed, event.isMetaPressed)
            if (shortcut != null && event.repeatCount == 0) run(shortcut)
            shortcut != null
        }
    }

    // With a desk keyboard, typing goes to the pane from the start, without a tap to focus it.
    LaunchedEffect(keyboard, hardKeyboard) {
        if (hardKeyboard || keysTyped) keyboard?.requestFocus()
    }

    // An open sheet handles Back itself and closes.
    BackHandler(onBack = onBack)

    // The keyboard belongs to the pane that is on screen: leaving the terminal drops it, so the
    // session list does not come back with a keyboard over it.
    DisposableEffect(Unit) {
        onDispose {
            keyboard?.clearFocus()
            ime?.hide()
        }
    }

    // One paper for the whole screen, the terminal's: the top bar and the dock float on it, as they
    // do in the reference, instead of sitting on bands of a second grey.
    Column(Modifier.fillMaxSize().background(Colors.terminal).statusBarsPadding().navigationBarsPadding()) {
        SessionTopBar(
            state = state,
            view = view,
            tab = tab,
            status = status,
            onBack = onBack,
            onNavigate = { if (view != null) sheet = Sheet.Navigator },
            onHolder = { if (state.owning) onAct { releasePane() } else onAct { takeOver() } },
            onMore = { sheet = Sheet.Actions },
            onShowKeys = if (keyRowShown) null else { { onKeyRowChoice(true) } },
            onToggleSidebar = onToggleSidebar,
            compact = compact,
        )
        TermBanner(state, onRetry = onRetry, onDismiss = onDismissNotice)
        // The pane sits on its own paper: white in the day, the page at night, edge to edge, the
        // way a terminal fills the screen.
        Box(Modifier.weight(1f).fillMaxWidth().background(Colors.terminal).clipToBounds().onSizeChanged { paneHeight = it.height }) {
            val shown = focused?.id
            key(shown) {
                TerminalPane(
                    blockId = shown.orEmpty(),
                    feeds = watchPane,
                    fontScale = fontScale,
                    terminalTypeface = terminalTypeface,
                    onFont = onFont,
                    onInput = { bytes -> onAct { writeBytes(bytes) } },
                    onTyped = ::typed,
                    onLink = { link = it },
                    onResize = { cols, rows, cw, ch -> onAct { setSize(cols, rows, cw, ch) } },
                    onSession = { termSession = it },
                    onKeyboard = { keyboard = it },
                    // A horizontal swipe on the pane moves to the pane beside it, as the dots
                    // under it say there is one.
                    onSwipe = { forward ->
                        if (panes.size > 1) {
                            val index = panes.indexOfFirst { it.id == focused?.id }
                            val next = (index + if (forward) 1 else -1).coerceIn(0, panes.lastIndex)
                            if (next != index) onAct { focusBlock(panes[next].id) }
                        }
                    },
                    onCaretBottom = { caretBottom = it },
                    onError = { onAct { note(it.toProblem()) } },
                    modifier = Modifier.fillMaxSize().offset { IntOffset(0, -paneShift()) }.padding(horizontal = 3.dp, vertical = 2.dp),
                )
            }
        }
        Column(
            Modifier.fillMaxWidth().offset { IntOffset(0, -lift()) }.background(Colors.terminal),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (panes.size > 1) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp), contentAlignment = Alignment.Center) {
                    PaneBar(
                        tab = tab,
                        focused = focused,
                        status = state.blockStatus,
                        programs = state.programs,
                        onSelect = { block -> onAct { focusBlock(block.id) } },
                    )
                }
            }
            upload?.let {
                UploadPill(
                    status = it,
                    onCancel = onCancelUpload,
                    onDismiss = onDismissUpload,
                    onRetry = onRetryUpload,
                )
            }
            // Shown or hidden at once rather than animated: each frame of a growing row would be one
            // more resize of the pane on the desktop.
            if (keyRowShown) KeyBar(
                mods = mods,
                busy = status?.state == "working" || status?.state == "blocked",
                imeUp = imeShown(),
                onMod = { mods = mods.tap(it, SystemClock.uptimeMillis()) },
                onKey = { key, own -> press(key, own) },
                onNewLine = { press(KeyPress("enter", 0), "shift") },
                onPaste = ::pasteClipboard,
                onKeyboard = { toggleKeyboard() },
                onHide = { onKeyRowChoice(false) },
                // On a tablet or a desktop window the row stays the width of a hand, not of the screen.
                modifier = Modifier.widthIn(max = KEY_BAR_MAX_WIDTH),
            )
        }
    }

    when (val open = sheet) {
        null -> Unit
        Sheet.Sessions -> SessionSheet(
            state = state,
            onDismiss = { sheet = null },
            onOpen = { id -> if (id != state.activeSessionId) onOpenSession(id) },
            onCreate = { onAct { createShell(null) } },
        )
        Sheet.Actions -> ActionSheet(onDismiss = { sheet = null }) {
            Group(stringResource(R.string.level_pane)) {
                SheetItem(stringResource(R.string.menu_split_right), icon = R.drawable.ic_split_right) { onAct { split(vertical = false) } }
                SheetItem(stringResource(R.string.menu_split_below), icon = R.drawable.ic_split_below) { onAct { split(vertical = true) } }
                if (focused != null) {
                    SheetItem(stringResource(R.string.menu_rename_pane), icon = R.drawable.ic_edit) { rename = Rename(Level.Pane, focused.id, focused.label) }
                    SheetItem(stringResource(R.string.menu_close_pane), Colors.danger, icon = R.drawable.ic_close) {
                        confirm = Confirm(Level.Pane, focused.id, focused.label)
                    }
                }
            }
            Group {
                if (upload?.running != true) {
                    SheetItem(stringResource(R.string.upload_attach), icon = R.drawable.ic_attach_file) { filePicker.launch("*/*") }
                }
                if (keyRowShown) {
                    SheetItem(stringResource(R.string.key_row_hide), icon = R.drawable.ic_keyboard_hide) { onKeyRowChoice(false) }
                } else {
                    SheetItem(stringResource(R.string.key_row_show), icon = R.drawable.ic_keyboard) { onKeyRowChoice(true) }
                }
            }
            Group(stringResource(R.string.level_tab)) {
                SheetItem(stringResource(R.string.menu_new_tab), icon = R.drawable.ic_add) { onAct { newWindow() } }
                if (view != null && tab != null) {
                    val title = tabTitle(view, tab, state)
                    SheetItem(stringResource(R.string.menu_rename_tab), icon = R.drawable.ic_edit) { rename = Rename(Level.Tab, tab.id, tab.label) }
                    SheetItem(stringResource(R.string.menu_close_tab), Colors.danger, icon = R.drawable.ic_close) { confirm = Confirm(Level.Tab, tab.id, title) }
                }
            }
            Group(stringResource(R.string.level_session)) {
                SheetItem(stringResource(R.string.header_sessions), icon = R.drawable.ic_sessions) { sheet = Sheet.Sessions }
                if (view != null) {
                    SheetItem(stringResource(R.string.menu_rename_session), icon = R.drawable.ic_edit) {
                        rename = Rename(Level.Session, view.sessionId, view.label)
                    }
                    SheetItem(stringResource(R.string.menu_kill_session), Colors.danger, icon = R.drawable.ic_delete) {
                        confirm = Confirm(Level.Session, view.sessionId, view.label)
                    }
                }
            }
        }
        Sheet.Navigator -> if (view != null) {
            ActionSheet(onDismiss = { sheet = null }, title = view.label) {
                Group {
                    SheetItem(stringResource(R.string.header_sessions), detail = pluralStringResource(R.plurals.session_count, state.sessions.size, state.sessions.size), icon = R.drawable.ic_sessions) {
                        sheet = Sheet.Sessions
                    }
                }
                Group(label = stringResource(R.string.group_tabs), icons = false) {
                    view.windows.forEach { window ->
                        SheetItem(
                            label = tabTitle(view, window, state),
                            detail = window.blocks.size.takeIf { it > 1 }?.let { pluralStringResource(R.plurals.pane_count, it, it) },
                            marked = window.id == tab?.id,
                        ) {
                            if (window.id != view.activeWindowId) onAct { focusWindow(window.id) }
                        }
                    }
                }
                if (panes.size > 1) {
                    Group(label = stringResource(R.string.group_panes), icons = false) {
                        panes.forEachIndexed { index, block ->
                            SheetItem(
                                label = "${index + 1}  ${paneTitle(block, state.blockStatus[block.id], state.programs[block.id])}",
                                detail = state.blockStatus[block.id]?.let { statusWord(it) },
                                marked = block.id == focused?.id,
                            ) {
                                if (block.id != focused?.id) onAct { focusBlock(block.id) }
                            }
                        }
                    }
                }
                Group {
                    SheetItem(stringResource(R.string.menu_new_tab), icon = R.drawable.ic_add) { onAct { newWindow() } }
                    SheetItem(stringResource(R.string.menu_split_right), icon = R.drawable.ic_split_right) { onAct { split(vertical = false) } }
                    SheetItem(stringResource(R.string.menu_split_below), icon = R.drawable.ic_split_below) { onAct { split(vertical = true) } }
                }
            }
        }
        is Sheet.TabActions -> {
            // The tab may close on the desktop while its sheet is open; then the sheet goes too.
            val window = view?.windows?.firstOrNull { it.id == open.windowId }
            if (view != null && window != null) {
                val title = tabTitle(view, window, state)
                ActionSheet(onDismiss = { sheet = null }, title = title) {
                    Group {
                        SheetItem(stringResource(R.string.menu_rename_tab), icon = R.drawable.ic_edit) { rename = Rename(Level.Tab, window.id, window.label) }
                        SheetItem(stringResource(R.string.menu_close_tab), Colors.danger, icon = R.drawable.ic_close) { confirm = Confirm(Level.Tab, window.id, title) }
                    }
                }
            }
        }
        is Sheet.PaneActions -> {
            val block = tab?.blocks?.firstOrNull { it.id == open.blockId }
            if (tab != null && block != null) {
                val number = tab.blocks.indexOf(block) + 1
                ActionSheet(onDismiss = { sheet = null }, title = "$number  ${paneTitle(block, state.blockStatus[block.id], state.programs[block.id])}") {
                    Group {
                        SheetItem(stringResource(R.string.menu_rename_pane), icon = R.drawable.ic_edit) { rename = Rename(Level.Pane, block.id, block.label) }
                        SheetItem(stringResource(R.string.menu_close_pane), Colors.danger, icon = R.drawable.ic_close) { confirm = Confirm(Level.Pane, block.id, block.label) }
                    }
                }
            }
        }
    }

    link?.let { url ->
        LinkSheet(
            url = url,
            onDismiss = { link = null },
            onOpen = { openLink(context, url, copied = R.string.toast_link_copied) },
            onCopy = {
                clipboardSet(context, url)
                toast(context, context.getString(R.string.toast_copied))
            },
            onShare = { shareText(context, url) },
        )
    }

    rename?.let { target ->
        var text by remember(target) { mutableStateOf(target.current) }
        AlertDialog(
            onDismissRequest = { rename = null },
            containerColor = Colors.surface,
            title = {
                Text(
                    when (target.kind) {
                        Level.Tab -> stringResource(R.string.menu_rename_tab)
                        Level.Pane -> stringResource(R.string.menu_rename_pane)
                        Level.Session -> stringResource(R.string.menu_rename_session)
                    },
                    color = Colors.ink,
                )
            },
            text = { Field(stringResource(R.string.field_name), text, autoFocus = true) { text = it } },
            confirmButton = {
                TextButton(onClick = {
                    rename = null
                    val label = text.trim()
                    if (label.isEmpty() || label == target.current) return@TextButton
                    onAct {
                        when (target.kind) {
                            Level.Tab -> renameWindow(label, target.id)
                            Level.Pane -> renameBlock(label, target.id)
                            Level.Session -> renameSession(label)
                        }
                    }
                }) { Text(stringResource(R.string.action_save), color = Colors.accent) }
            },
            dismissButton = {
                TextButton(onClick = { rename = null }) { Text(stringResource(R.string.action_cancel), color = Colors.muted) }
            },
        )
    }

    confirm?.let { target ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            containerColor = Colors.surface,
            title = {
                Text(
                    when (target.kind) {
                        Level.Tab -> stringResource(R.string.confirm_close_tab, target.name)
                        Level.Pane -> stringResource(R.string.confirm_close_pane, target.name)
                        Level.Session -> stringResource(R.string.confirm_kill_session, target.name)
                    },
                    color = Colors.ink,
                )
            },
            text = {
                Text(
                    when (target.kind) {
                        Level.Tab -> stringResource(R.string.confirm_close_tab_body)
                        Level.Pane -> stringResource(R.string.confirm_close_pane_body)
                        Level.Session -> stringResource(R.string.confirm_kill_session_body)
                    },
                    color = Colors.muted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    onAct {
                        when (target.kind) {
                            Level.Tab -> closeWindow(target.id)
                            Level.Pane -> closeBlock(target.id)
                            Level.Session -> destroySession()
                        }
                    }
                }) { Text(stringResource(R.string.action_close), color = Colors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) { Text(stringResource(R.string.action_cancel), color = Colors.muted) }
            },
        )
    }
}

/**
 * The floating top bar of the reference: a round back button, the pane's tile, the tab with the
 * directory it is in, the session under it, and the menu. No bar behind it; the paper is the
 * background. The title block opens the tabs and panes of the session.
 */
@Composable
private fun SessionTopBar(
    state: PocketState,
    view: SessionView?,
    tab: WindowInfo?,
    status: ProgramRecord?,
    onBack: () -> Unit,
    onNavigate: () -> Unit,
    onHolder: () -> Unit,
    onMore: () -> Unit,
    /** Brings the key row back; null while it is on screen. */
    onShowKeys: (() -> Unit)?,
    onToggleSidebar: (() -> Unit)? = null,
    compact: Boolean = false,
) {
    val title = when {
        view == null -> state.endpointLabel.ifBlank { stringResource(R.string.app_name) }
        tab != null -> tabTitle(view, tab, state)
        else -> view.label
    }
    val path = state.pwd?.takeIf { it.isNotBlank() }?.let { shortPath(it) }
    if (compact) {
        CompactTopBar(state, view, tab, status, title, path, onNavigate, onHolder, onMore, onShowKeys, onToggleSidebar)
        return
    }
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundButton(R.drawable.ic_back, stringResource(R.string.action_back), onBack, plate = Colors.key, size = 40.dp)
        Row(
            Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(Radius.card))
                .clickable(enabled = view != null, role = Role.Button, onClickLabel = stringResource(R.string.menu_navigate), onClick = onNavigate)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProgramTile(tab?.focusedBlock()?.id?.let { state.programs[it] }, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        title,
                        color = Colors.ink,
                        fontSize = TextStyles.body,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (path != null) {
                        Spacer(Modifier.width(6.dp))
                        Text(path, color = Colors.muted, fontSize = TextStyles.subhead, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(status?.let { statusTint(it) } ?: phaseTint(state.phase), 6.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        view?.label ?: phaseText(state),
                        color = Colors.muted,
                        fontSize = TextStyles.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Glyph(R.drawable.ic_expand, Colors.muted, size = 18.dp)
                }
            }
        }
        ShowKeysButton(onShowKeys, size = 40.dp, plate = Colors.key)
        if (state.phase == Phase.Live) {
            HolderChip(state.owning, onHolder)
            Spacer(Modifier.width(4.dp))
        }
        RoundButton(R.drawable.ic_more, stringResource(R.string.action_more), onMore, plate = Colors.key, size = 40.dp)
    }
}

/**
 * The title bar beside the sidebar, one line as on the Rex desktop app: the sidebar already names
 * the host and session, so this says only what runs here and where, and keeps the holder and menu.
 */
@Composable
private fun CompactTopBar(
    state: PocketState,
    view: SessionView?,
    tab: WindowInfo?,
    status: ProgramRecord?,
    title: String,
    path: String?,
    onNavigate: () -> Unit,
    onHolder: () -> Unit,
    onMore: () -> Unit,
    onShowKeys: (() -> Unit)?,
    onToggleSidebar: (() -> Unit)?,
) {
    Row(
        Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // At the sidebar's edge, in the same place whether it is open or folded away.
        if (onToggleSidebar != null) {
            RoundButton(R.drawable.ic_sidebar, stringResource(R.string.sidebar_toggle), onToggleSidebar, tint = Colors.muted, size = 36.dp, iconSize = 20.dp)
        }
        Row(
            Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(Radius.chip))
                .clickable(enabled = view != null, role = Role.Button, onClickLabel = stringResource(R.string.menu_navigate), onClick = onNavigate)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProgramTile(tab?.focusedBlock()?.id?.let { state.programs[it] }, size = 24.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                title,
                color = Colors.ink,
                fontSize = TextStyles.subhead,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (path != null) {
                Spacer(Modifier.width(8.dp))
                Text(path, color = Colors.muted, fontSize = TextStyles.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Dot(status?.let { statusTint(it) } ?: phaseTint(state.phase), 6.dp)
        }
        ShowKeysButton(onShowKeys, size = 36.dp)
        if (state.phase == Phase.Live) {
            HolderChip(state.owning, onHolder, height = 32.dp)
            Spacer(Modifier.width(4.dp))
        }
        RoundButton(R.drawable.ic_more, stringResource(R.string.action_more), onMore, tint = Colors.muted, size = 36.dp, iconSize = 20.dp)
    }
}

/** The way back to a hidden key row, which comes in where it can be seen while the row is gone. */
@Composable
private fun ShowKeysButton(onShowKeys: (() -> Unit)?, size: androidx.compose.ui.unit.Dp, plate: Color? = null) {
    AnimatedVisibility(
        visible = onShowKeys != null,
        enter = fadeIn() + scaleIn(initialScale = 0.6f),
        exit = fadeOut() + scaleOut(targetScale = 0.6f),
    ) {
        Row {
            RoundButton(
                R.drawable.ic_keyboard,
                stringResource(R.string.key_row_show),
                { onShowKeys?.invoke() },
                plate = plate,
                tint = if (plate == null) Colors.muted else Colors.ink,
                size = size,
                iconSize = if (size < 40.dp) 20.dp else 22.dp,
            )
            Spacer(Modifier.width(4.dp))
        }
    }
}

/**
 * Who holds the pane, and the one tap that changes it. While the desk holds it the chip is the
 * action colour and says "Take over"; while this phone does it is quiet and says so, and tapping
 * it hands the pane back. The pane has the holder's size, so this is what decides how it looks.
 */
@Composable
private fun HolderChip(owning: Boolean, onClick: () -> Unit, height: androidx.compose.ui.unit.Dp = 40.dp) {
    val label = stringResource(if (owning) R.string.holder_phone else R.string.action_take_over)
    val action = stringResource(if (owning) R.string.action_release else R.string.action_take_over)
    Box(
        Modifier.height(height).clip(RoundedCornerShape(50))
            .background(if (owning) Colors.key else Colors.accent)
            .clickable(role = Role.Button, onClickLabel = action, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (owning) Colors.muted else Colors.onAccent,
            fontSize = TextStyles.label,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/**
 * The panes of the shown tab, in one floating pill under the pane, as the reference draws them: a
 * segment per pane with the tile of what runs in it and its name, the one on screen on a soft plate
 * in the action colour. Tapping a segment goes there; a horizontal swipe on the pane does the same.
 * Each segment is a full 48 dp target.
 */
@Composable
private fun PaneBar(
    tab: WindowInfo?,
    focused: BlockInfo?,
    status: Map<String, ProgramRecord>,
    programs: Map<String, String>,
    onSelect: (BlockInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val blocks = tab?.blocks.orEmpty()
    val scroll = rememberScrollState()
    val selected = stringResource(R.string.state_selected)
    Row(
        modifier.floating(RoundedCornerShape(50)).horizontalScroll(scroll).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        blocks.forEachIndexed { index, block ->
            val on = block.id == focused?.id
            val record = status[block.id]
            val busy = record?.state == "working" || record?.state == "blocked"
            Box(
                Modifier.height(48.dp)
                    .clickable(role = Role.Tab) { if (!on) onSelect(block) }
                    .semantics { if (on) stateDescription = selected }
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    Modifier.fillMaxHeight().clip(RoundedCornerShape(50))
                        .then(if (on) Modifier.background(Colors.keyOn) else Modifier)
                        .padding(start = 6.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProgramTile(programs[block.id], size = 24.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        paneTitle(block, record, programs[block.id]),
                        color = if (on) Colors.accent else Colors.ink,
                        fontSize = TextStyles.subhead,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 104.dp),
                    )
                    if (busy) {
                        Spacer(Modifier.width(6.dp))
                        Dot(statusTint(record), 6.dp)
                    }
                }
            }
        }
    }
}

/**
 * One file's way to the desktop: what it is doing, how far it has got, and the one control that
 * makes sense there - stop it while it runs, retry a failure that inserted nothing, dismiss the
 * rest. An unconfirmed finish says so and offers no retry, because Rex may already have the file.
 */
@Composable
private fun UploadPill(
    status: UploadStatus,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    val done = status.phase == UploadPhase.Done
    val failed = status.phase == UploadPhase.Failed || status.phase == UploadPhase.Uncertain || status.phase == UploadPhase.Unsupported
    val tint = when {
        done -> Colors.ok
        failed -> Colors.danger
        else -> Colors.ink
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp)
            .floating(RoundedCornerShape(Radius.card)).padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                uploadText(status),
                color = tint,
                fontSize = TextStyles.subhead,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (status.phase == UploadPhase.Failed) {
                Text(
                    status.error.orEmpty(),
                    color = Colors.muted,
                    fontSize = TextStyles.label,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        when {
            status.phase == UploadPhase.Failed -> {
                if (status.retryable) {
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry), color = Colors.accent, fontSize = TextStyles.label) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_dismiss), color = Colors.muted, fontSize = TextStyles.label) }
            }
            failed -> TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_dismiss), color = Colors.muted, fontSize = TextStyles.label)
            }
            done -> Unit
            else -> TextButton(onClick = onCancel) {
                Text(stringResource(R.string.action_cancel), color = Colors.muted, fontSize = TextStyles.label)
            }
        }
    }
}

@Composable
private fun uploadText(status: UploadStatus): String = when (status.phase) {
    UploadPhase.Preparing -> stringResource(R.string.upload_preparing, status.name)
    UploadPhase.Sending -> stringResource(R.string.upload_sending, status.name, (status.progress * 100).roundToInt())
    UploadPhase.Finishing -> stringResource(R.string.upload_finishing, status.name)
    UploadPhase.Done -> stringResource(R.string.upload_done, status.name)
    UploadPhase.Unsupported -> stringResource(R.string.upload_need_update)
    UploadPhase.Uncertain -> stringResource(R.string.upload_uncertain)
    UploadPhase.Failed -> stringResource(R.string.upload_failed, status.name)
    UploadPhase.Canceled -> stringResource(R.string.upload_canceled)
}

/**
 * The foot of the terminal, as the reference lays it out: the pane bar when the tab is split, then
 * one floating pill - keyboard, Enter, the special keys, the attach key - and the circle that makes
 * a tab. All of it is 56 dp, the same height as the home's floating controls, on the page margin.
 */
/** Whether the phone keyboard is up, which decides which way the keyboard key points. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun imeShown(): Boolean = WindowInsets.isImeVisible

/**
 * A tab's name, as the desktop's sidebar writes it. A tab someone named keeps its name; one left at
 * Rex's generated "Window" label takes the title its focused pane's program set, such as
 * `π - rexpocket`, then the program's own name, and only then its position.
 */
@Composable
internal fun tabTitle(view: SessionView, window: WindowInfo, state: PocketState? = null): String {
    if (!GENERATED_WINDOW_LABEL.matches(window.label)) return window.label
    val pane = window.focusedBlock()?.id
    val named = pane?.let { state?.titles?.get(it) }
    if (named != null) return named
    val program = pane?.let { state?.programs?.get(it) }
    if (program != null && programMark(program) != ProgramMark.Shell) return program
    return stringResource(R.string.tab_number, view.windows.indexOf(window) + 1)
}

/** What a tab's tile shows: the program in its focused pane. */
internal fun tabProgram(window: WindowInfo, state: PocketState): String? =
    window.focusedBlock()?.id?.let { state.programs[it] }

/** A pane left at the default "shell" label is named after the program running in it. */
internal fun paneTitle(block: BlockInfo, status: ProgramRecord?, program: String? = null): String {
    val app = status?.app?.takeIf { it.isNotBlank() } ?: program
    return if (block.label == "shell" && app != null) app else block.label
}

/** What the program in the pane is doing, in the app's words; null when Rex has not said. */
@Composable
internal fun statusWord(status: ProgramRecord): String? = statusWordRes(status)?.let { stringResource(it) }

/** [statusWord] as a resource, for the alerts, which are written outside a screen. */
internal fun statusWordRes(status: ProgramRecord): Int? = when (status.state) {
    "working" -> R.string.state_running
    "blocked" -> when (status.kind) {
        "permission" -> R.string.state_waiting_approval
        "question" -> R.string.state_waiting_answer
        "auth" -> R.string.state_waiting_login
        else -> R.string.state_waiting_you
    }
    "done" -> R.string.state_done
    "error" -> R.string.state_failed
    "idle" -> R.string.state_idle
    else -> null
}

/** Running is green, waiting on a person is amber, failure is red, the rest is quiet grey. */
internal fun statusTint(status: ProgramRecord): Color = when {
    status.state == "working" -> Colors.ok
    status.state == "blocked" -> Colors.amber
    status.state == "error" -> Colors.danger
    else -> Colors.muted
}

/** The connection's own state, for when no program has reported one. */
internal fun phaseTint(phase: Phase): Color = when (phase) {
    Phase.Live -> Colors.ok
    Phase.Failed -> Colors.danger
    Phase.Connecting, Phase.Reconnecting -> Colors.amber
    else -> Colors.muted
}

/**
 * A path as a phone can show it: the home directory as `~`, and a long path cut to its last
 * segments, because the directory being worked in is the end of it.
 */
internal fun shortPath(path: String): String {
    var p = path.trim().trimEnd('/')
    if (p.isEmpty()) return path
    // Rex reports a directory as a file URI, sometimes with the machine as its authority
    // (`file://host/Users/me`). The header wants the path inside it, not the URI.
    p = p.removePrefix("file:")
    p = when {
        p.startsWith("///") -> "/" + p.trimStart('/')
        p.startsWith("//") -> "/" + p.trimStart('/').substringAfter('/', "")
        p.startsWith("/") -> p
        else -> "/" + p.substringAfter('/', "")
    }
    if (p == "/") return "/"
    val home = Regex("^/(Users|home)/[^/]+").find(p)?.value
    val short = if (home != null) "~" + p.removePrefix(home) else p
    if (short.length <= 24) return short
    // The last directory is the one being worked in; the ones above it only cost width.
    val segments = short.split('/').filter { it.isNotEmpty() }
    return "…/" + segments.last()
}

private val KEY_BAR_MAX_WIDTH = 720.dp

/**
 * How far the pane slides up when the keyboard covers [covered] pixels of its foot: only as far as
 * keeps the caret row in sight, and never further than the keyboard reaches.
 */
internal fun paneShift(caretBottom: Int, paneHeight: Int, covered: Int): Int =
    (caretBottom - (paneHeight - covered)).coerceIn(0, covered.coerceAtLeast(0))

/** The person's own choice wins; left alone, the key row gives way to a desk keyboard. */
internal fun keyRowShown(choice: Boolean?, hardKeyboard: Boolean): Boolean = choice ?: !hardKeyboard

/** A desk keyboard is plugged in and open, from the activity's configuration. */
internal fun hasHardKeyboard(keyboard: Int, hardKeyboardHidden: Int): Boolean =
    keyboard == Configuration.KEYBOARD_QWERTY && hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO

/** How long a failed action stays on the terminal screen. */
internal const val NOTICE_MS = 4_000L

internal val GENERATED_WINDOW_LABEL = Regex("Window [0-9a-f]{6,}")
