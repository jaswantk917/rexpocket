package me.leaver.rexpocket.ui

import androidx.compose.material3.MaterialShapes
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.onClick
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ScrollState
import androidx.annotation.DrawableRes
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.selected
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
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
import me.leaver.rexpocket.terminal.TerminalSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt
import me.leaver.rexpocket.R
import me.leaver.rexpocket.core.BlockInfo
import me.leaver.rexpocket.core.KeyModifiers
import me.leaver.rexpocket.core.KeyPress
import me.leaver.rexpocket.core.ListedSession
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
    /** True when the key bar is kept even with a keyboard on the desk. */
    keyBarWithKeyboard: Boolean = false,
    /** The key bar put away (false) or brought back (true) by hand; null leaves it to the setting. */
    keyBarChoice: Boolean? = null,
    onKeyBarChoice: (Boolean) -> Unit = {},
    keyBarAlt: Boolean = false,
    /** True when this pane shares the window with the list, which is then the way back. */
    embedded: Boolean = false,
    onSettings: (() -> Unit)? = null,
    /** Puts this pane in a bubble; null where bubbles are not offered, and inside one. */
    onBubble: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val ime = LocalSoftwareKeyboardController.current
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onAttach(uri)
    }
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var mods by remember(keyBarAlt) { mutableStateOf(KeyModifiers()) }
    var link by remember { mutableStateOf<String?>(null) }
    var rename by remember { mutableStateOf<Rename?>(null) }
    var confirm by remember { mutableStateOf<Confirm?>(null) }
    var termSession by remember { mutableStateOf<TerminalSession?>(null) }
    // The pane's editor: what Back and the session list drop.
    var keyboard by remember { mutableStateOf<TerminalImeView?>(null) }
    val view = state.view
    val tab = view?.activeWindow()
    val focused = view?.focusedBlock()
    val status = focused?.let { state.blockStatus[it.id] }
    val panes = tab?.blocks.orEmpty()

    /** Puts what the clipboard holds into the program, where a key on the bar or the menu asks for it. */
    fun pasteClipboard() {
        val text = clipboard(context)
        if (text.isNullOrEmpty()) {
            toast(context, context.getString(R.string.toast_clipboard_empty))
        } else {
            onAct { paste(text) }
        }
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
        val key = keyPressFor(String(bytes, Charsets.UTF_8)) ?: run {
            // An IME commit is still the next input, even when it is not a single key.
            mods = mods.afterTextCommit(String(bytes, Charsets.UTF_8))
            return false
        }
        flushFirst()
        press(key)
        return true
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
    var keysTyped by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(keyboard) {
        if (keysTyped) keyboard?.requestFocus()
    }

    // Set on every composition, so a shortcut always acts on the tabs and size on screen now.
    SideEffect {
        keyboard?.onShortcut = { event ->
            keysTyped = true
            val shortcut = terminalShortcut(event.keyCode, event.isCtrlPressed, event.isShiftPressed, event.isAltPressed, event.isMetaPressed)
            if (shortcut != null && event.repeatCount == 0) run(shortcut)
            shortcut != null
        }
    }


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
    //
    // The keyboard takes no room from the terminal. The pane keeps the grid it has and slides up by
    // what the keyboard covers - the bottom of the terminal, the rows being typed into, stays where
    // it was - while the dock and the rest of the bottom chrome rise over it. A program is therefore
    // never resized by the screen keyboard opening, so it never redraws or reflows for it.
    val hardwareKeys = rememberHardwareKeyboard()
    val dock = keyBarChoice ?: keyBarShown(hardwareKeys, keyBarWithKeyboard)
    var bottomPx by remember { mutableIntStateOf(0) }
    var panePx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val slide = imeShift(imeHeight(), panePx)

    // What the session can do, in Android's anchored menu: first the tree of what is open, each level
    // with its own rename and close, then the rest.
    val overflow: @Composable (Boolean, () -> Unit) -> Unit = { expanded, dismiss ->
        DropdownMenu(expanded = expanded, onDismissRequest = dismiss, shape = RoundedCornerShape(20.dp)) {
            if (view != null) {
                LevelRow(
                    level = stringResource(R.string.level_session),
                    name = view.label,
                    depth = 0,
                    renameLabel = stringResource(R.string.menu_rename_session),
                    closeLabel = stringResource(R.string.menu_kill_session),
                    closeIcon = R.drawable.ic_delete,
                    onRename = { dismiss(); rename = Rename(Level.Session, view.sessionId, view.label) },
                    onClose = { dismiss(); confirm = Confirm(Level.Session, view.sessionId, view.label) },
                )
                if (tab != null) {
                    val title = tabTitle(view, tab, state)
                    LevelRow(
                        level = stringResource(R.string.level_tab),
                        name = title,
                        depth = 1,
                        renameLabel = stringResource(R.string.menu_rename_tab),
                        closeLabel = stringResource(R.string.menu_close_tab),
                        closeIcon = R.drawable.ic_close,
                        onRename = { dismiss(); rename = Rename(Level.Tab, tab.id, tab.label) },
                        onClose = { dismiss(); confirm = Confirm(Level.Tab, tab.id, title) },
                    )
                }
                if (focused != null) {
                    LevelRow(
                        level = stringResource(R.string.level_pane),
                        name = paneTitle(focused, status, state.programs[focused.id]),
                        depth = 2,
                        renameLabel = stringResource(R.string.menu_rename_pane),
                        closeLabel = stringResource(R.string.menu_close_pane),
                        closeIcon = R.drawable.ic_close,
                        onRename = { dismiss(); rename = Rename(Level.Pane, focused.id, focused.label) },
                        onClose = { dismiss(); confirm = Confirm(Level.Pane, focused.id, focused.label) },
                    )
                }
                HorizontalDivider()
            }
            MenuItem(stringResource(R.string.header_sessions), R.drawable.ic_sessions) { dismiss(); sheet = Sheet.Sessions }
            if (focused != null) {
                MenuItem(stringResource(R.string.menu_split_right), R.drawable.ic_split_right) { dismiss(); onAct { split(vertical = false) } }
                MenuItem(stringResource(R.string.menu_split_below), R.drawable.ic_split_below) { dismiss(); onAct { split(vertical = true) } }
            }
            HorizontalDivider()
            // Paste is here as well as on the keys: a keyboard on the desk takes the keys away, and a
            // chord only reaches the program, never the phone's clipboard.
            MenuItem(stringResource(R.string.key_paste), R.drawable.ic_paste) { dismiss(); pasteClipboard() }
            if (upload?.running != true) {
                MenuItem(stringResource(R.string.upload_attach), R.drawable.ic_attach_file) { dismiss(); filePicker.launch("*/*") }
            }
            if (dock) {
                MenuItem(stringResource(R.string.key_row_hide), R.drawable.ic_keyboard_hide) { dismiss(); onKeyBarChoice(false) }
            } else {
                MenuItem(stringResource(R.string.key_row_show), R.drawable.ic_keyboard) { dismiss(); onKeyBarChoice(true) }
            }
            onBubble?.let { bubble ->
                MenuItem(stringResource(R.string.menu_bubble), R.drawable.ic_bubble) { dismiss(); bubble() }
            }
            onSettings?.let { open ->
                MenuItem(stringResource(R.string.settings), R.drawable.ic_settings) { dismiss(); open() }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Colors.terminal).navigationBarsPadding()) {
        Column(Modifier.fillMaxSize().padding(bottom = with(density) { bottomPx.toDp() })) {
            TermTopBar(
                state = state,
                view = view,
                tab = tab,
                status = status,
                onBack = onBack,
                showBack = !embedded,
                onHolder = { if (state.owning) onAct { releasePane() } else onAct { takeOver() } },
                overflow = overflow,
            )
            TermBanner(state, onRetry = onRetry, onDismiss = onDismissNotice)
            // The pane sits on its own paper: white in the day, the page at night, edge to edge, the
            // way a terminal fills the screen.
            Box(Modifier.weight(1f).fillMaxWidth().background(Colors.terminal).clipToBounds()) {
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
                        hardwareKeyboard = hardwareKeys,
                        // A horizontal swipe on the pane moves to the pane beside it, as the dots
                        // under it say there is one.
                        onSwipe = { forward ->
                            if (panes.size > 1) {
                                val index = panes.indexOfFirst { it.id == focused?.id }
                                val next = (index + if (forward) 1 else -1).coerceIn(0, panes.lastIndex)
                                if (next != index) onAct { focusBlock(panes[next].id) }
                            }
                        },
                        onError = { onAct { note(it.toProblem()) } },
                        modifier = Modifier.fillMaxSize()
                            // Moved, not resized: see imeShift. An offset keeps the pane's measured size
                            // - and so the grid the program was given - exactly as it was.
                            .offset { IntOffset(0, -slide.roundToInt()) }
                            .padding(horizontal = 3.dp, vertical = 2.dp)
                            .onSizeChanged { panePx = it.height },
                    )
                }
            }
        }
        // The upload card floats over the bottom of the pane, above the strip and keys, so sending a
        // file never resizes the terminal.
        Box(Modifier.align(Alignment.BottomCenter).imePadding().padding(bottom = with(density) { bottomPx.toDp() } + 8.dp)) {
            upload?.let {
                ReachableWidth {
                    UploadPill(
                        status = it,
                        onCancel = onCancelUpload,
                        onDismiss = onDismissUpload,
                        onRetry = onRetryUpload,
                    )
                }
            }
        }
        // The bottom of the screen, on its own: the pane's size is measured above without it, so the
        // keyboard can lift this stack without the terminal ever changing shape.
        Box(Modifier.align(Alignment.BottomCenter).imePadding()) {
            Column(Modifier.onSizeChanged { bottomPx = it.height }) {
                // The strip belongs to the pane, end to end: its chips line up with the text.
                Box(Modifier.fillMaxWidth()) {
                    PaneStrip(
                        state = state,
                        view = view,
                        onSession = { id -> if (id != state.activeSessionId) onOpenSession(id) },
                        onTab = { window -> if (window.id != view?.activeWindowId) onAct { focusWindow(window.id) } },
                        onPane = { block -> if (block.id != focused?.id) onAct { focusBlock(block.id) } },
                        onPaneMenu = { block -> sheet = Sheet.PaneActions(block.id) },
                        onNewTab = { onAct { newWindow() } },
                        onShowKeys = if (dock) null else { { onKeyBarChoice(true) } },
                    )
                }
                if (dock) {
                    // The dock is one hand's reach wide, whatever the window is: a tablet or a desk
                    // window keeps the keys together in the middle instead of spreading them across
                    // the bottom edge.
                    ReachableWidth {
                        KeyBar(
                            mods = mods,
                            busy = status?.state == "working" || status?.state == "blocked",
                            onMod = { mods = mods.tap(it, SystemClock.uptimeMillis()) },
                            onKey = { key, own -> press(key, own) },
                            showAlt = keyBarAlt,
                            onNewLine = { press(KeyPress("enter", 0), "shift") },
                            onPaste = { pasteClipboard() },
                            onAttach = { filePicker.launch("*/*") },
                            attachReady = upload?.running != true,
                            onHide = { onKeyBarChoice(false) },
                        )
                    }
                }
            }
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
 * The terminal's top bar: a tonal band that frames the pane. Back (not beside the list), the tab's
 * name over its session and directory, who holds the pane, and the menu. Fixed height, so the pane's grid never changes for it.
 */
@Composable
private fun TermTopBar(
    state: PocketState,
    view: SessionView?,
    tab: WindowInfo?,
    status: ProgramRecord?,
    onBack: () -> Unit,
    showBack: Boolean,
    onHolder: () -> Unit,
    overflow: @Composable (expanded: Boolean, onDismiss: () -> Unit) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val title = when {
        view == null -> state.endpointLabel.ifBlank { stringResource(R.string.app_name) }
        tab != null -> tabTitle(view, tab, state)
        else -> view.label
    }
    val path = state.pwd?.takeIf { it.isNotBlank() }?.let { shortPath(it) }
    val under = listOfNotNull(view?.label ?: phaseText(state), path).joinToString(" · ")
    var more by remember { mutableStateOf(false) }
    // The session's own hue, the one its card wears in the list: the colour says which session this is.
    val tint = rememberTint(view?.sessionId ?: "")
    val band by animateColorAsState(if (view != null) tint.container else scheme.surfaceContainer, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "top-bar")
    val ink = if (view != null) tint.onContainer else scheme.onSurface
    Box(Modifier.fillMaxWidth().background(band).statusBarsPadding().height(TOP_BAR)) {
    if (view != null) {
        Box(Modifier.matchParentSize().clipToBounds()) {
            StillOrnament(MaterialShapes.Sunny, tint.ornament, 140.dp, Modifier.align(Alignment.CenterEnd).offset(x = (-40).dp))
        }
    }
    Row(
        Modifier.fillMaxSize().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            RoundButton(R.drawable.ic_back, stringResource(R.string.action_back), onBack, tint = ink, plate = ink.copy(alpha = 0.10f), size = 40.dp)
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 4.dp)) {
            AnimatedContent(title, transitionSpec = { captionSwap() }, label = "tab-title") { text ->
                Text(text, style = MaterialTheme.typography.titleMediumEmphasized, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(status?.let { statusTint(it) } ?: phaseTint(state.phase), 6.dp)
                Spacer(Modifier.width(6.dp))
                Text(under, style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = 0.72f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (state.phase == Phase.Live) HolderChip(state.owning, onHolder)
        Box {
            RoundButton(R.drawable.ic_more, stringResource(R.string.action_more), { more = true }, tint = ink.copy(alpha = 0.8f), size = 48.dp)
            overflow(more) { more = false }
        }
    }
    }
}

/** One level of what is open, in the menu's tree: what it is, its name, its own rename and close. */
@Composable
private fun LevelRow(
    level: String,
    name: String,
    depth: Int,
    renameLabel: String,
    closeLabel: String,
    @DrawableRes closeIcon: Int,
    onRename: () -> Unit,
    onClose: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val branch = scheme.outlineVariant
    Row(
        Modifier.width(280.dp).heightIn(min = 56.dp)
            .drawBehind {
                if (depth > 0) {
                    val x = (LEVEL_START + LEVEL_INDENT * (depth - 1) + 6.dp).toPx()
                    val mid = size.height / 2
                    val bend = 6.dp.toPx()
                    val elbow = Path().apply {
                        moveTo(x, 0f)
                        lineTo(x, mid - bend)
                        quadraticTo(x, mid, x + bend, mid)
                        lineTo(x + 12.dp.toPx(), mid)
                    }
                    drawPath(elbow, branch, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
                }
            }
            .padding(start = LEVEL_START + LEVEL_INDENT * depth, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(level, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant, maxLines = 1)
            Text(name, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        RoundButton(R.drawable.ic_edit, renameLabel, onRename, tint = scheme.onSurfaceVariant, size = 40.dp, iconSize = 20.dp)
        RoundButton(closeIcon, closeLabel, onClose, tint = scheme.error, size = 40.dp, iconSize = 20.dp)
    }
}

private val LEVEL_START = 16.dp
private val LEVEL_INDENT = 20.dp

/** The top bar's height under the status bar. */
private val TOP_BAR = 64.dp

/**
 * The strip under the pane, above the keys: every session as a chip to scroll and tap, the open one
 * holding its tabs and, when its tab is split, its panes. It is always this tall, so switching never
 * resizes the terminal.
 */
@Composable
private fun PaneStrip(
    state: PocketState,
    view: SessionView?,
    onSession: (String) -> Unit,
    onTab: (WindowInfo) -> Unit,
    onPane: (BlockInfo) -> Unit,
    onPaneMenu: (BlockInfo) -> Unit,
    onNewTab: () -> Unit,
    onShowKeys: (() -> Unit)?,
) {
    val scheme = MaterialTheme.colorScheme
    val scroll = rememberScrollState()
    val reach = remember { StripReach() }
    val margin = with(LocalDensity.current) { 28.dp.roundToPx() }
    LaunchedEffect(reach.span, scroll.viewportSize) {
        val span = reach.span ?: return@LaunchedEffect
        scroll.animateScrollTo(scrollToShow(scroll.value, scroll.viewportSize, span.first, span.last, margin))
    }
    Row(Modifier.fillMaxWidth().height(STRIP), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).scrollFade(scroll).horizontalScroll(scroll)
                .onPlaced {
                    reach.content = it
                    reach.measure()
                }
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            stripSessions(state.sessions, view).forEach { session -> key(session.id) {
                val open = session.id == view?.sessionId
                SessionChips(session, open, if (open) view else state.views[session.id], state, reach, onSession, onTab, onPane, onPaneMenu)
            } }
            if (view != null) {
                RoundButton(R.drawable.ic_add, stringResource(R.string.menu_new_tab), onNewTab, tint = scheme.onSurfaceVariant, size = 40.dp, iconSize = 20.dp)
            }
        }
        AnimatedVisibility(
            visible = onShowKeys != null,
            enter = fadeIn() + scaleIn(initialScale = 0.6f),
            exit = fadeOut() + scaleOut(targetScale = 0.6f),
        ) {
            RoundButton(R.drawable.ic_keyboard, stringResource(R.string.key_row_show), { onShowKeys?.invoke() }, plate = scheme.secondaryContainer, tint = scheme.onSecondaryContainer, size = 40.dp, iconSize = 20.dp)
        }
    }
}

/** Where the chosen chip sits in the strip's scrolling content, once both have been placed. */
private class StripReach {
    var content: LayoutCoordinates? = null
    var chip: LayoutCoordinates? = null
    var span by mutableStateOf<IntRange?>(null)

    fun measure() {
        val from = content?.takeIf { it.isAttached } ?: return
        val to = chip?.takeIf { it.isAttached } ?: return
        val box = from.localBoundingBoxOf(to, clipBounds = false)
        span = box.left.roundToInt()..box.right.roundToInt()
    }
}

/** The scroll that shows [start]..[end] clear of the faded ends, moving no further than it must. */
internal fun scrollToShow(current: Int, viewport: Int, start: Int, end: Int, margin: Int): Int = when {
    end - start + 2 * margin >= viewport || start - margin < current -> start - margin
    end + margin > current + viewport -> end + margin - viewport
    else -> current
}.coerceAtLeast(0)

/** Fades an end of the strip while there is more that way, so a hidden chip is not a secret. */
private fun Modifier.scrollFade(scroll: ScrollState): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
        drawContent()
        val edge = 28.dp.toPx()
        if (scroll.canScrollBackward) {
            drawRect(Brush.horizontalGradient(0f to Color.Transparent, 1f to Color.Black, endX = edge), size = Size(edge, size.height), blendMode = BlendMode.DstIn)
        }
        if (scroll.canScrollForward) {
            val from = size.width - edge
            drawRect(Brush.horizontalGradient(0f to Color.Black, 1f to Color.Transparent, startX = from, endX = size.width), topLeft = Offset(from, 0f), size = Size(edge, size.height), blendMode = BlendMode.DstIn)
        }
    }

/** The sessions the strip offers, in Rex's order; the open one is there even before the list has it. */
internal fun stripSessions(sessions: List<ListedSession>, view: SessionView?): List<ListedSession> =
    if (view == null || sessions.any { it.id == view.sessionId }) sessions
    else listOf(ListedSession(view.sessionId, view.label)) + sessions

/** One session's chip; opened, it grows to hold the session's tabs and the panes of its tab. */
@Composable
private fun SessionChips(
    session: ListedSession,
    open: Boolean,
    shown: SessionView?,
    state: PocketState,
    reach: StripReach,
    onSession: (String) -> Unit,
    onTab: (WindowInfo) -> Unit,
    onPane: (BlockInfo) -> Unit,
    onPaneMenu: (BlockInfo) -> Unit,
) {
    val tint = rememberTint(session.id)
    val tabs = shown?.windows.orEmpty()
    val tab = shown?.activeWindow()
    val panes = tab?.blocks.orEmpty()
    val holds = open && (tabs.size > 1 || panes.size > 1)
    val holder by animateColorAsState(
        if (holds) tint.container.copy(alpha = 0.35f) else Color.Transparent,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "session-holder",
    )
    val grow = expandHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn()
    val shrink = shrinkHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut()
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(holder).animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Holding tabs, the session is the holder's title and the chosen tab is the solid chip.
        StripChip(
            session.label,
            open,
            tint,
            tab?.let { tabProgram(it, state) },
            activityIn(tabs.flatMap { it.blocks }, state).takeUnless { holds },
            quiet = holds,
            reach = reach.takeIf { open && !holds },
        ) { onSession(session.id) }
        AnimatedVisibility(open && tabs.size > 1, enter = grow, exit = shrink) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                if (shown != null) tabs.forEach { window -> key(window.id) {
                    val chosen = window.id == tab?.id
                    StripChip(tabTitle(shown, window, state), chosen, tint, tabProgram(window, state), activityIn(window.blocks, state), inHolder = true, reach = reach.takeIf { open && chosen && panes.size <= 1 }) { onTab(window) }
                } }
            }
        }
        AnimatedVisibility(open && panes.size > 1, enter = grow, exit = shrink) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                panes.forEach { block -> key(block.id) {
                    val record = state.blockStatus[block.id]
                    val chosen = block.id == shown?.focusedBlock()?.id
                    StripChip(
                        paneTitle(block, record, state.programs[block.id]),
                        chosen,
                        tint,
                        state.programs[block.id],
                        record?.takeIf { it.state == "working" || it.state == "blocked" },
                        inHolder = true,
                        reach = reach.takeIf { open && chosen },
                        onLongClick = { onPaneMenu(block) },
                    ) { onPane(block) }
                } }
            }
        }
    }
}

/** What most needs a look among [blocks]: a pane waiting on the person, else one at work. */
private fun activityIn(blocks: List<BlockInfo>, state: PocketState): ProgramRecord? {
    val records = blocks.mapNotNull { state.blockStatus[it.id] }
    return records.firstOrNull { it.state == "blocked" } ?: records.firstOrNull { it.state == "working" }
}

/** A filter chip of the strip: tap to go there; the chosen one reports where it is, to be scrolled to. */
@Composable
private fun StripChip(
    label: String,
    on: Boolean,
    tint: Tint,
    program: String?,
    activity: ProgramRecord?,
    quiet: Boolean = false,
    inHolder: Boolean = false,
    reach: StripReach? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalView.current
    val fill by animateColorAsState(
        when {
            quiet -> Color.Transparent
            on -> tint.container
            inHolder -> Color.Transparent
            else -> scheme.surfaceContainerHigh
        },
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "strip-chip",
    )
    val start by animateDpAsState(if (quiet) 14.dp else 8.dp, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "strip-start")
    Row(
        Modifier.height(40.dp)
            .then(
                if (reach != null) {
                    Modifier.onPlaced {
                        reach.chip = it
                        reach.measure()
                    }
                } else {
                    Modifier
                },
            )
            .pressable(
                RoundedCornerShape(50),
                role = Role.Tab,
                onLongClick = onLongClick,
                onClick = {
                    if (!on) {
                        Haptics.tick(haptics)
                        onClick()
                    }
                },
            )
            .semantics { selected = on }
            .background(fill).padding(start = start, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(!quiet, enter = expandHorizontally() + fadeIn(), exit = shrinkHorizontally() + fadeOut()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgramTile(program, size = 20.dp)
                Spacer(Modifier.width(6.dp))
            }
        }
        Text(
            label,
            style = if (on) MaterialTheme.typography.labelLargeEmphasized else MaterialTheme.typography.labelLarge,
            color = if (on) tint.onContainer else scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 120.dp),
        )
        if (activity != null) {
            Spacer(Modifier.width(6.dp))
            Dot(statusTint(activity), 6.dp)
        }
    }
}

/** The pane strip's height. */
private val STRIP = 52.dp

/**
 * Who holds the pane, as an icon: the phone while this phone does, the desktop while the desk does.
 * The pane has the holder's size, so this is what decides how it looks. While the desk holds it the
 * button is filled in the accent, because taking over is the thing to do; while this phone does it
 * is quiet, and tapping it hands the pane back. The words are in its tooltip and for TalkBack.
 */
@Composable
private fun HolderChip(owning: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val tip = stringResource(if (owning) R.string.holder_phone_tip else R.string.holder_desk_tip)
    val fill by animateColorAsState(if (owning) scheme.surfaceContainerHighest else scheme.primary, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "holder")
    val ink = if (owning) scheme.onSurfaceVariant else scheme.onPrimary
    Tip(tip) {
        Box(
            Modifier.size(40.dp).pressable(CircleShape, onClick = onClick)
                .semantics { contentDescription = tip }
                .background(fill),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(owning, transitionSpec = { (scaleIn(initialScale = 0.6f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut()) }, label = "holder-icon") { mine ->
                Glyph(if (mine) R.drawable.ic_phone else R.drawable.ic_desktop, ink, size = 20.dp)
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

/** How long a failed action stays on the terminal screen. */
internal const val NOTICE_MS = 4_000L

internal val GENERATED_WINDOW_LABEL = Regex("Window [0-9a-f]{6,}")
