package me.leaver.rexpocket.ui

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.foundation.layout.FlowRow
import me.leaver.rexpocket.data.TERMINAL_FAMILIES
import me.leaver.rexpocket.data.TerminalFontState
import me.leaver.rexpocket.data.Appearance
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalView
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import me.leaver.rexpocket.alerts.Alerts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.leaver.rexpocket.BuildConfig
import me.leaver.rexpocket.R
import me.leaver.rexpocket.core.BlockInfo
import me.leaver.rexpocket.core.HostAddress
import me.leaver.rexpocket.core.HostAddressParser
import me.leaver.rexpocket.core.ListedSession
import me.leaver.rexpocket.core.Phase
import me.leaver.rexpocket.core.PocketState
import me.leaver.rexpocket.core.ProgramRecord
import me.leaver.rexpocket.core.SessionView
import me.leaver.rexpocket.core.TailnetPath
import me.leaver.rexpocket.core.UpdateChannel
import me.leaver.rexpocket.core.WindowInfo
import me.leaver.rexpocket.data.HostStore
import me.leaver.rexpocket.data.SavedHost
import me.leaver.rexpocket.data.TailscaleSettings
import me.leaver.rexpocket.update.UpdateState
import me.leaver.rexpocket.diagnostics.Diagnostics
import me.leaver.rexpocket.net.TailscaleStatus

// The hosts, each with its sessions under it and their tabs under those: one list to search, one
// place to connect from.
//
// Everything sits on two keylines, the way the reference lines up: [KEY_MARK] for the host's mark,
// a session's name and a tab's tile, [KEY_TEXT] for every title. Trailing controls sit in 48 dp
// slots flush with the right edge, so a menu, a fold and a row's chevron share one centre line.

/** Where the credit for the interface goes; the README carries the long version. */
private const val SOURCE_URL = "https://github.com/leizhiyuan/rexpocket"

/** The page margin: the cards and the floating controls stand this far from the screen's edge. */
private val PAGE = 16.dp

/** The first keyline, inside the page margin: marks, session names, tiles. */
private val KEY_MARK = 16.dp

/** The second keyline, inside the page margin: every title. */
private val KEY_TEXT = 64.dp

private val TILE = 36.dp

/** One session of a host, with the tabs the phone knows of; [view] is null until it is loaded. */
private data class HomeSession(
    val session: ListedSession,
    val view: SessionView?,
    val tabs: List<WindowInfo>,
    val activity: Boolean,
)

/** A pane that is working or waiting is what the eye filters on. */
private fun PocketState.activityOf(block: BlockInfo): ProgramRecord? =
    blockStatus[block.id]?.takeIf { it.state == "working" || it.state == "blocked" }

/** The folds of the list, as one saveable value: which hosts and sessions are open. */
private val StringSetSaver = Saver<Set<String>, List<String>>(
    save = { it.toList() },
    restore = { it.toSet() },
)

@Composable
internal fun HomeScreen(
    hosts: List<SavedHost>,
    connected: SavedHost?,
    state: PocketState,
    /** True while the open pane stands beside this list, so the tab on the right carries a mark. */
    detail: Boolean = false,
    tailscale: TailscaleSettings,
    tailnetPath: TailnetPath?,
    opening: Opening?,
    cachedSessions: (String) -> List<ListedSession>,
    onConnect: (SavedHost) -> Unit,
    onOpenSession: (SavedHost, String) -> Unit,
    onOpenTab: (SavedHost, String, String) -> Unit,
    onNewSession: (SavedHost) -> Unit,
    onNewTab: (SavedHost, String) -> Unit,
    onDisconnect: () -> Unit,
    onRetry: () -> Unit,
    onProbePath: () -> Unit,
    onSave: (SavedHost) -> Unit,
    onDelete: (String) -> Unit,
    onTailscale: () -> Unit,
    onSettings: () -> Unit,
) {
    var editing by remember { mutableStateOf<SavedHost?>(null) }
    var creating by remember { mutableStateOf(false) }
    val view = LocalView.current
    // What the person was looking at survives the window being rebuilt under it, which a resize in
    // a free-form window does: the search stays open on its query, and the folds stay folded.
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var activeOnly by rememberSaveable { mutableStateOf(false) }
    var collapsedHosts by rememberSaveable(stateSaver = StringSetSaver) { mutableStateOf(emptySet<String>()) }
    var collapsedSessions by rememberSaveable(stateSaver = StringSetSaver) { mutableStateOf(emptySet<String>()) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val closeSearch: () -> Unit = {
        searchOpen = false
        query = ""
        keyboard?.hide()
    }

    LaunchedEffect(searchOpen) {
        if (searchOpen) {
            focus.requestFocus()
            keyboard?.show()
        }
    }
    // Back closes the search first, as the platform's search does.
    BackHandler(enabled = searchOpen, onBack = closeSearch)

    if (hosts.isEmpty()) {
        EmptyHome(onAdd = { creating = true })
    } else {
        val q = query.trim().lowercase()
        val rows = hosts.mapNotNull { host ->
            val live = connected?.id == host.id
            val allSessions = if (live && state.phase == Phase.Live) state.sessions else cachedSessions(host.id)
            val hostMatch = q.isEmpty() || host.label.lowercase().contains(q) || host.host.contains(q)
            val sessions = allSessions.mapNotNull { session ->
                val view = if (live) state.views[session.id] else null
                val tabs = view?.windows.orEmpty()
                val sessionActivity = tabs.any { window -> window.blocks.any { state.activityOf(it) != null } }
                if (activeOnly && !sessionActivity) return@mapNotNull null
                val labelMatch = hostMatch || session.label.lowercase().contains(q)
                // The eye keeps the tabs that are doing something; search then narrows by name.
                val activeTabs = if (activeOnly) tabs.filter { window -> window.blocks.any { state.activityOf(it) != null } } else tabs
                val shownTabs = if (labelMatch || view == null) activeTabs else activeTabs.filter { tabTitle(view, it, state).lowercase().contains(q) }
                if (!labelMatch && shownTabs.isEmpty()) return@mapNotNull null
                HomeSession(session, view, shownTabs, sessionActivity)
            }
            if (sessions.isEmpty() && (activeOnly || !hostMatch)) return@mapNotNull null
            host to sessions
        }
        val list = rememberLazyListState()
        val bar = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        // The toolbar folds down to its button while the list is read downwards, as Orion's dock does.
        val toolbarOpen by remember { derivedStateOf { !list.canScrollBackward || list.lastScrolledBackward } }
        // What needs a look, across every tab the phone knows of; the host count when nothing does.
        val working = state.blockStatus.values.count { it.state == "working" }
        val waiting = state.blockStatus.values.count { it.state == "blocked" }
        val subtitle = if (working + waiting > 0) {
            stringResource(R.string.home_activity, working, waiting)
        } else {
            pluralStringResource(R.plurals.host_count, hosts.size, hosts.size)
        }
        Scaffold(
            modifier = Modifier.fillMaxSize().nestedScroll(bar.nestedScrollConnection),
            containerColor = Colors.bg,
            contentWindowInsets = WindowInsets(0),
            topBar = {
                if (searchOpen) {
                    SearchBar(query = query, focus = focus, onQuery = { query = it }, onClose = closeSearch)
                } else {
                    LargeFlexibleTopAppBar(
                        title = { Text(stringResource(R.string.home_title), fontWeight = FontWeight.Bold) },
                        subtitle = { Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Colors.bg,
                            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                        scrollBehavior = bar,
                    )
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()).imePadding()) {
                val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                LazyColumn(
                    Modifier.fillMaxSize(),
                    state = list,
                    contentPadding = PaddingValues(
                        start = PAGE,
                        end = PAGE,
                        top = 4.dp,
                        // Clear of the toolbar, so the last tab can be scrolled above it.
                        bottom = if (searchOpen) 16.dp else navBottom + 112.dp,
                    ),
                ) {
                    var shown = 0
                    rows.forEachIndexed { index, (host, sessions) ->
                        val live = connected?.id == host.id
                        val hostExpanded = host.id !in collapsedHosts || searchOpen
                        val hostOrder = shown++
                        item(key = "host-${host.id}") {
                            Column(Modifier.animateItem().reveal(hostOrder)) {
                                if (index > 0) Spacer(Modifier.height(20.dp))
                                HostHeader(
                                    host = host,
                                    live = live,
                                    status = if (live) phaseText(state) else null,
                                    phase = if (live) state.phase else null,
                                    sessionCount = sessions.size,
                                    expanded = hostExpanded,
                                    onToggle = {
                                        if (live) {
                                            collapsedHosts = if (hostExpanded) collapsedHosts + host.id else collapsedHosts - host.id
                                        } else {
                                            onConnect(host)
                                        }
                                    },
                                    onConnect = { onConnect(host) },
                                    onNewSession = { onNewSession(host) },
                                    onEdit = { editing = host },
                                    onDisconnect = onDisconnect,
                                )
                                if (live) HostStatus(state, tailnetPath, onRetry, onProbePath)
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                        if (hostExpanded) {
                            sessions.forEach { row ->
                                val sessionExpanded = row.session.id !in collapsedSessions || searchOpen
                                val openingThis = opening == Opening.Existing(row.session.id)
                                val showTabs = sessionExpanded && row.view != null
                                val order = shown++
                                item(key = "session-${row.session.id}") {
                                    Column(Modifier.animateItem().reveal(order)) {
                                        SessionHeader(
                                            row = row,
                                            live = live,
                                            opening = openingThis,
                                            expanded = sessionExpanded,
                                            joined = showTabs,
                                            onToggle = {
                                                if (row.view == null) {
                                                    onOpenSession(host, row.session.id)
                                                } else {
                                                    collapsedSessions = if (sessionExpanded) collapsedSessions + row.session.id else collapsedSessions - row.session.id
                                                }
                                            },
                                            onNewTab = { onNewTab(host, row.session.id) },
                                        )
                                        if (sessionExpanded && row.view == null) {
                                            Spacer(Modifier.height(SEGMENT_GAP))
                                            EmptyTabs(
                                                text = when {
                                                    openingThis -> stringResource(R.string.session_opening)
                                                    !live -> stringResource(R.string.session_cached)
                                                    else -> stringResource(R.string.session_open_to_load)
                                                },
                                                canOpen = !openingThis,
                                                onOpen = { onOpenSession(host, row.session.id) },
                                            )
                                        }
                                    }
                                }
                                if (showTabs) {
                                    item(key = "tabs-${row.session.id}") {
                                        TabGroup(
                                            row = row,
                                            state = state,
                                            opening = openingThis,
                                            open = detail && live && state.activeSessionId == row.session.id,
                                            onOpen = { windowId -> onOpenTab(host, row.session.id, windowId) },
                                            modifier = Modifier.animateItem().reveal(order),
                                        )
                                    }
                                }
                                item(key = "gap-${row.session.id}") { Spacer(Modifier.height(12.dp)) }
                            }
                        }
                    }
                    if (rows.isEmpty()) {
                        item { NotFound(activeOnly = activeOnly) }
                    }
                }
                if (!searchOpen) {
                    HomeToolbar(
                        expanded = toolbarOpen,
                        activeOnly = activeOnly,
                        onSearch = { searchOpen = true },
                        onActiveOnly = {
                            activeOnly = !activeOnly
                            Haptics.toggle(view, activeOnly)
                        },
                        onSettings = onSettings,
                        onAdd = { creating = true },
                        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp),
                    )
                }
            }
        }
    }

    if (creating || editing != null) {
        HostDialog(
            initial = editing,
            tailscaleEnabled = tailscale.enabled,
            onDismiss = { creating = false; editing = null },
            onDelete = editing?.let { host -> { onDelete(host.id); creating = false; editing = null } },
            onSave = {
                onSave(it)
                creating = false
                editing = null
            },
            onJoinTailnet = {
                creating = false
                editing = null
                onTailscale()
            },
        )
    }

}

/**
 * Settings, and the diagnostics sheet they lead to. Kept out of the host list so the terminal can
 * open them too: on a phone the list is not on screen while a pane is.
 */
@Composable
internal fun Settings(
    onBack: () -> Unit,
    showBack: Boolean,
    appearance: Appearance,
    onAppearance: (Appearance) -> Unit,
    terminalFont: TerminalFontState,
    tailscale: TailscaleSettings,
    tailscaleStatus: TailscaleStatus,
    tailnetError: String?,
    onTailscale: () -> Unit,
    alerts: Boolean,
    onAlerts: (Boolean) -> Unit,
    keyBar: Boolean,
    onKeyBar: (Boolean) -> Unit,
    onFontSettings: () -> Unit,
    onDiagnosticsReport: (limit: Int) -> String,
    update: UpdateState,
    onUpdate: () -> Unit,
    updateChannel: UpdateChannel,
    onUpdateChannel: (UpdateChannel) -> Unit,
) {
    val context = LocalContext.current
    // Android 13 asks for notifications at run time; alerts come on once the person allows them.
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onAlerts(true) else toast(context, context.getString(R.string.alerts_blocked))
    }
    val toggleAlerts = {
        when {
            alerts -> onAlerts(false)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED ->
                ask.launch(Manifest.permission.POST_NOTIFICATIONS)
            !Alerts.allowed(context) -> toast(context, context.getString(R.string.alerts_blocked))
            else -> onAlerts(true)
        }
    }
    var diagnostics by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(Diagnostics.capture(context) != null) }
    val joined = tailscale.enabled && tailscaleStatus.backendState == "Running"
    val tailnet = when {
        tailnetError != null -> stringResource(R.string.tailscale_state_failed)
        joined -> stringResource(R.string.tailscale_state_running)
        tailscale.enabled -> tailnetState(tailscaleStatus)
        else -> stringResource(R.string.tailscale_not_joined)
    }
    val fontName = terminalFont.selected?.let { id -> TERMINAL_FAMILIES.firstOrNull { it.id == id }?.label }
        ?: stringResource(R.string.terminal_font_system)

    SettingsScreen(
        SettingsModel(
            appearance = appearance,
            onAppearance = onAppearance,
            tailnet = tailnet,
            onTailscale = onTailscale,
            alerts = alerts,
            onAlerts = toggleAlerts,
            keyBar = keyBar,
            onKeyBar = onKeyBar,
            terminalFont = fontName,
            onTerminalFont = onFontSettings,
            recording = recording,
            onDiagnostics = { diagnostics = true },
            update = update,
            onUpdate = onUpdate,
            channel = updateChannel,
            onChannel = onUpdateChannel,
            onSource = { openLink(context, SOURCE_URL) },
        ),
        onBack = onBack,
        showBack = showBack,
    )

    if (diagnostics) {
        DiagnosticsSheet(
            onDismiss = { diagnostics = false },
            report = onDiagnosticsReport,
            onRecordingChanged = { recording = it },
        )
    }
}

/** The fold's chevron: down while open, turned to point right while folded. */
@Composable
private fun FoldChevron(expanded: Boolean) {
    val turn by animateFloatAsState(if (expanded) 0f else -90f, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "fold")
    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        Glyph(R.drawable.ic_expand, Colors.muted, Modifier.rotate(turn))
    }
}

/**
 * A host's mark: the computer on an expressive cookie, filled with the scheme's primary container
 * while it is the one connected, and its connection's colour cut into the corner.
 */
@Composable
private fun HostMark(live: Boolean, phase: Phase?) {
    val scheme = MaterialTheme.colorScheme
    val fill by animateColorAsState(if (live) scheme.primaryContainer else scheme.surfaceContainerHighest, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "mark")
    val ink = if (live) scheme.onPrimaryContainer else scheme.onSurfaceVariant
    val turn by animateFloatAsState(if (live) 0f else -30f, MaterialTheme.motionScheme.slowSpatialSpec(), label = "mark-turn")
    Box(Modifier.size(52.dp)) {
        Box(
            Modifier.size(52.dp).graphicsLayer { rotationZ = turn }.clip(MaterialShapes.Cookie9Sided.toShape()).background(fill),
            contentAlignment = Alignment.Center,
        ) {
            Glyph(R.drawable.ic_host, ink, Modifier.graphicsLayer { rotationZ = -turn }, size = 24.dp)
        }
        if (phase != null) Dot(phaseTint(phase), 14.dp, Modifier.align(Alignment.BottomEnd), ring = Colors.bg)
    }
}

/**
 * One host: its mark, its name, and what it is doing. Tapping it folds its sessions, or connects it
 * when it is not the one open. Its menu is anchored to it, the way Android menus are.
 */
@Composable
private fun HostHeader(
    host: SavedHost,
    live: Boolean,
    status: String?,
    phase: Phase?,
    sessionCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onConnect: () -> Unit,
    onNewSession: () -> Unit,
    onEdit: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val folded = stringResource(R.string.state_folded)
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp)
            .pressable(RoundedCornerShape(28.dp), onLongClick = { menu = true }, onClick = onToggle)
            .semantics { if (live && !expanded) stateDescription = folded }
            .padding(start = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HostMark(live, phase)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                host.label,
                style = MaterialTheme.typography.titleLargeEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val line = (if (live) status.orEmpty() else stringResource(R.string.host_not_connected)) +
                if (sessionCount > 0) " · " + pluralStringResource(R.plurals.session_count, sessionCount, sessionCount) else ""
            AnimatedContent(line, transitionSpec = { captionSwap() }, label = "host-status") { text ->
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (live) phaseTint(phase ?: Phase.Idle) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (live) FoldChevron(expanded)
        Box {
            RoundButton(R.drawable.ic_more, stringResource(R.string.action_more), { menu = true }, tint = Colors.muted, size = 48.dp)
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, shape = RoundedCornerShape(20.dp)) {
                if (!live) MenuItem(stringResource(R.string.action_connect), R.drawable.ic_tailnet) { menu = false; onConnect() }
                MenuItem(stringResource(R.string.session_new), R.drawable.ic_add) { menu = false; onNewSession() }
                MenuItem(stringResource(R.string.action_edit_host), R.drawable.ic_edit) { menu = false; onEdit() }
                if (live) {
                    HorizontalDivider()
                    MenuItem(stringResource(R.string.action_disconnect), R.drawable.ic_disconnect, danger = true) { menu = false; onDisconnect() }
                }
            }
        }
    }
}

/** A row of an anchored menu: its icon, its label, red when it takes something away. */
@Composable
internal fun MenuItem(label: String, @androidx.annotation.DrawableRes icon: Int, danger: Boolean = false, onClick: () -> Unit) {
    val tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    DropdownMenuItem(
        text = { Text(label, color = tint) },
        leadingIcon = { Glyph(icon, if (danger) tint else MaterialTheme.colorScheme.onSurfaceVariant) },
        onClick = onClick,
    )
}

/** A status line swaps by sliding: the new words rise in as the old ones rise out. */
internal fun <S> AnimatedContentTransitionScope<S>.captionSwap(): ContentTransform =
    (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())

/** The line under a connected host: why it is not live, and how the tailnet carries it. */
@Composable
private fun HostStatus(state: PocketState, path: TailnetPath?, onRetry: () -> Unit, onProbe: () -> Unit) {
    val problem = rememberSettledProblem(state.phase, state.problem.takeIf { state.phase != Phase.Live })
    Column(Modifier.padding(start = 72.dp)) {
        if (problem != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    problemText(problem, state.endpointLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onRetry, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.action_retry_now)) }
            }
        }
        if (path != null) {
            val ms = path.latencyMs?.let { " · ${it.toInt()} ms" }.orEmpty()
            val (text, color) = when (path) {
                is TailnetPath.Direct -> stringResource(R.string.tailnet_path_direct) + ms to Colors.ok
                is TailnetPath.PeerRelay -> stringResource(R.string.tailnet_path_peer_relay) + ms to Colors.amber
                is TailnetPath.Derp ->
                    stringResource(R.string.tailnet_path_derp, path.regionCode.ifBlank { path.regionId.toString() }) + ms to Colors.amber
                is TailnetPath.Unreachable -> stringResource(R.string.tailnet_path_unreachable, path.reason) to Colors.danger
            }
            // Tapping the path measures it again.
            Row(
                Modifier.pressable(RoundedCornerShape(50), onClick = onProbe)
                    .background(color.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Dot(color, 6.dp)
                Spacer(Modifier.width(6.dp))
                Text(text, style = MaterialTheme.typography.labelMedium, color = color)
            }
        }
    }
}

/** Corner radius of a segmented group's outer corners, and of the corners where segments meet. */
private val SEGMENT_OUTER = 24.dp
private val SEGMENT_INNER = 6.dp

/** The gap between the segments of one group: the page shows through, no hairlines. */
private val SEGMENT_GAP = 2.dp

/** The shape of segment [index] of [count], as Material 3's segmented lists draw them. */
private fun segmentShape(index: Int, count: Int): RoundedCornerShape {
    val top = if (index == 0) SEGMENT_OUTER else SEGMENT_INNER
    val bottom = if (index == count - 1) SEGMENT_OUTER else SEGMENT_INNER
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

/**
 * A session: the head of its segmented group - its name, what it holds, the plus that opens a tab
 * in it, and the fold. [joined] when its tabs follow it in the same group.
 */
@Composable
private fun SessionHeader(
    row: HomeSession,
    live: Boolean,
    opening: Boolean,
    expanded: Boolean,
    joined: Boolean,
    onToggle: () -> Unit,
    onNewTab: () -> Unit,
) {
    val tabs = row.view?.windows?.size ?: 0
    val detail = when {
        opening -> stringResource(R.string.session_opening)
        row.view != null -> pluralStringResource(R.plurals.session_window_count, tabs, tabs)
        !live -> stringResource(R.string.session_cached)
        else -> null
    }
    val shape = if (joined) segmentShape(0, 2) else RoundedCornerShape(SEGMENT_OUTER)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp)
            .pressable(shape, onClick = onToggle)
            .background(Colors.surface)
            .padding(start = 20.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    row.session.label,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (row.activity) {
                    Spacer(Modifier.width(8.dp))
                    Dot(MaterialTheme.colorScheme.primary, 8.dp)
                }
            }
            if (detail != null) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (opening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        if (opening) LoadingIndicator(Modifier.size(32.dp))
        RoundButton(
            R.drawable.ic_add,
            stringResource(R.string.menu_new_tab),
            onNewTab,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            size = 40.dp,
            enabled = !opening,
        )
        if (row.view != null) FoldChevron(expanded)
    }
}

/** The tabs of one session: the rest of its segmented group, the tab on screen beside it marked. */
@Composable
private fun TabGroup(
    row: HomeSession,
    state: PocketState,
    opening: Boolean,
    /** True for the session whose tab the pane beside the list is showing. */
    open: Boolean,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        row.tabs.forEachIndexed { index, window ->
            Spacer(Modifier.height(SEGMENT_GAP))
            val activity = window.blocks.firstNotNullOfOrNull { state.activityOf(it) }
            TabRow(
                title = row.view?.let { tabTitle(it, window, state) } ?: window.label,
                program = tabProgram(window, state),
                panes = window.blocks.size,
                activity = activity,
                opening = opening,
                open = open && window.id == state.view?.activeWindowId,
                // The session's head is segment 0; its tabs follow it.
                shape = segmentShape(index + 1, row.tabs.size + 1),
                onOpen = { onOpen(window.id) },
            )
        }
    }
}

/**
 * One tab: the mark of what runs in it, its name and panes, and what it is doing at the end - a
 * living loader while an agent works, an amber "needs you" while one waits.
 */
@Composable
private fun TabRow(
    title: String,
    program: String?,
    panes: Int,
    activity: ProgramRecord?,
    opening: Boolean,
    /** True for the tab the pane beside the list is showing. */
    open: Boolean,
    shape: RoundedCornerShape,
    onOpen: () -> Unit,
) {
    val selected = stringResource(R.string.state_selected)
    val scheme = MaterialTheme.colorScheme
    val fill by animateColorAsState(if (open) scheme.secondaryContainer else Colors.surface, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "tab")
    Row(
        Modifier.fillMaxWidth().heightIn(min = 68.dp)
            .pressable(shape, enabled = !opening, onClick = onOpen)
            .background(fill)
            .semantics { if (open) stateDescription = selected }
            .padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProgramTile(program, size = TILE)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (open) scheme.onSecondaryContainer else scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                pluralStringResource(R.plurals.pane_count, panes, panes),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        AnimatedContent(activity?.state, transitionSpec = { (scaleIn(initialScale = 0.6f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut()) }, label = "tab-activity") { state ->
            when (state) {
                "working" -> LoadingIndicator(Modifier.size(36.dp))
                "blocked" -> activity?.let { StatusPill(statusWord(it).orEmpty(), Colors.amber) }
                else -> Spacer(Modifier.size(1.dp))
            }
        }
    }
}

/** A short state on a tonal pill in its own colour. */
@Composable
private fun StatusPill(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.16f)).padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun EmptyTabs(text: String, canOpen: Boolean, onOpen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(segmentShape(1, 2)).background(Colors.surface)
            .padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        if (canOpen) {
            TextButton(onClick = onOpen, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.session_tap_open)) }
        }
    }
}

@Composable
private fun NotFound(activeOnly: Boolean) {
    Text(
        stringResource(if (activeOnly) R.string.no_activity else R.string.search_none),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        textAlign = TextAlign.Center,
    )
}

/**
 * The list's controls, under the thumb: an expressive floating toolbar with search, the activity
 * filter and settings, and the button that adds a host. Scrolling down folds it to the button.
 */
@Composable
private fun HomeToolbar(
    expanded: Boolean,
    activeOnly: Boolean,
    onSearch: () -> Unit,
    onActiveOnly: () -> Unit,
    onSettings: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val on = stringResource(R.string.filter_on)
    val off = stringResource(R.string.filter_off)
    HorizontalFloatingToolbar(
        expanded = expanded,
        modifier = modifier,
        floatingActionButton = {
            Tip(stringResource(R.string.action_add_host)) {
                FloatingToolbarDefaults.VibrantFloatingActionButton(onClick = onAdd) {
                    Glyph(R.drawable.ic_add, MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        },
    ) {
        Tip(stringResource(R.string.action_search)) {
            IconButton(onClick = onSearch, shapes = IconButtonDefaults.shapes()) { Glyph(R.drawable.ic_search, LocalContentColor.current) }
        }
        Tip(stringResource(R.string.action_show_active)) {
            FilledIconToggleButton(
                checked = activeOnly,
                onCheckedChange = { onActiveOnly() },
                shapes = IconButtonDefaults.toggleableShapes(),
                modifier = Modifier.semantics { stateDescription = if (activeOnly) on else off },
            ) {
                Glyph(if (activeOnly) R.drawable.ic_eye_filled else R.drawable.ic_eye, LocalContentColor.current)
            }
        }
        Tip(stringResource(R.string.settings)) {
            IconButton(onClick = onSettings, shapes = IconButtonDefaults.shapes()) { Glyph(R.drawable.ic_settings, LocalContentColor.current) }
        }
    }
}

/** The search, in place of the app bar while it is open: back, the field, and clear. */
@Composable
private fun SearchBar(
    query: String,
    focus: FocusRequester,
    onQuery: (String) -> Unit,
    onClose: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = PAGE, vertical = 8.dp).height(56.dp)
            .clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundButton(R.drawable.ic_back, stringResource(R.string.action_back), onClose, size = 48.dp)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(stringResource(R.string.search_hint), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
        if (query.isNotEmpty()) {
            RoundButton(R.drawable.ic_close, stringResource(R.string.action_clear), { onQuery("") }, tint = Colors.muted, size = 48.dp)
        }
    }
}


/** No hosts yet: a big expressive mark, what to run on the computer, and the one thing to do. */
@Composable
private fun EmptyHome(onAdd: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().background(Colors.bg).statusBarsPadding().navigationBarsPadding().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(132.dp).reveal(0).clip(MaterialShapes.Cookie9Sided.toShape()).background(scheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Glyph(R.drawable.ic_host, scheme.onPrimaryContainer, size = 56.dp)
        }
        Spacer(Modifier.height(28.dp))
        Text(
            stringResource(R.string.home_no_hosts),
            style = MaterialTheme.typography.headlineMediumEmphasized,
            color = scheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.reveal(1),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.home_no_hosts_body),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.reveal(2).clip(RoundedCornerShape(16.dp)).background(scheme.surfaceContainerHigh).padding(16.dp),
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onAdd,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.reveal(3).heightIn(min = ButtonDefaults.MediumContainerHeight),
            contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
        ) {
            Glyph(R.drawable.ic_add, scheme.onPrimary, size = ButtonDefaults.iconSizeFor(ButtonDefaults.MediumContainerHeight))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_add_host), style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight))
        }
    }
}

/** Tailscale's backend state in the app's own words; empty while nothing is running. */
@Composable
internal fun tailnetState(status: TailscaleStatus): String = when (status.backendState) {
    "" -> ""
    "Running" -> stringResource(R.string.tailscale_state_running)
    "Starting" -> stringResource(R.string.tailscale_state_starting)
    "NeedsLogin" -> stringResource(R.string.tailscale_state_needs_login)
    "NeedsMachineAuth" -> stringResource(R.string.tailscale_state_needs_approval)
    "Stopped", "NoState" -> stringResource(R.string.tailscale_state_stopped)
    else -> status.text.ifBlank { status.backendState }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TailscaleDialog(
    initial: TailscaleSettings,
    status: TailscaleStatus,
    error: String?,
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit,
    onLeave: () -> Unit,
) {
    val context = LocalContext.current
    var hostname by remember { mutableStateOf(initial.hostname) }
    val state = tailnetState(status)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tailscale_title), color = Colors.ink) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Field(stringResource(R.string.tailscale_hostname), hostname) { hostname = it }
                if (initial.enabled) {
                    Text(
                        listOfNotNull(
                            state.ifBlank { null },
                            status.networkName.ifBlank { null },
                        ).joinToString(" · ").ifBlank { stringResource(R.string.tailscale_state_starting) },
                        color = if (status.backendState == "Running") Colors.accent else Colors.amber,
                        fontSize = TextStyles.label,
                    )
                }
                error?.let { Text(it, color = Colors.danger, fontSize = TextStyles.label) }
                if (status.authUrl.isNotBlank()) {
                    Text(
                        status.authUrl,
                        color = Colors.accent,
                        fontSize = TextStyles.label,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.combinedClickable(
                            onClick = { openLink(context, status.authUrl) },
                            onLongClick = {
                                clipboardSet(context, status.authUrl)
                                toast(context, context.getString(R.string.tailscale_login_copied))
                            },
                        ),
                    )
                    Text(stringResource(R.string.tailscale_login_hint), color = Colors.muted, fontSize = TextStyles.label, lineHeight = labelLeading)
                }
                Text(stringResource(R.string.tailscale_hint), color = Colors.muted, fontSize = TextStyles.label, lineHeight = labelLeading)
            }
        },
        confirmButton = {
            TextButton(onClick = { onJoin(hostname) }) {
                Text(
                    stringResource(if (initial.enabled) R.string.action_save else R.string.tailscale_join),
                    color = Colors.accent,
                )
            }
        },
        dismissButton = {
            Row {
                if (initial.enabled) {
                    TextButton(onClick = onLeave) { Text(stringResource(R.string.tailscale_leave), color = Colors.danger) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = Colors.muted) }
            }
        },
    )
}

/**
 * Adding a host is a name and an address. The port, the scheme and whether the address has to go
 * through the built-in Tailscale node all follow from the address, and the line under the field
 * says what the app made of it - or, while there is nothing to make of it yet, what can be typed
 * there. `ssh://user@host` adds the account: Rex is then reached through that machine's SSH daemon,
 * with a password or a private key.
 */
@Composable
internal fun HostDialog(
    initial: SavedHost?,
    tailscaleEnabled: Boolean,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?,
    onSave: (SavedHost) -> Unit,
    onJoinTailnet: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var label by remember { mutableStateOf(initial?.label ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }
    var address by remember {
        mutableStateOf(
            initial?.let { host ->
                if (host.viaSsh) {
                    "ssh://${host.host}:${host.port}"
                } else {
                    val scheme = if (host.plaintext) "ws" else "wss"
                    "$scheme://${host.host}:${host.port}"
                }
            } ?: "",
        )
    }
    var sshUser by remember { mutableStateOf(initial?.sshUser.orEmpty()) }
    var password by remember { mutableStateOf(initial?.sshPassword.orEmpty()) }
    var privateKey by remember { mutableStateOf(initial?.sshPrivateKey.orEmpty()) }
    var passphrase by remember { mutableStateOf(initial?.sshPassphrase.orEmpty()) }
    var useKey by remember { mutableStateOf(!initial?.sshPrivateKey.isNullOrEmpty()) }
    val parsed = remember(address) { HostAddressParser.parse(address) }
    val ssh = parsed?.viaSsh == true
    // The account can be in the address or in its own field; the address wins when it names one.
    LaunchedEffect(parsed?.sshUser) {
        val fromAddress = parsed?.sshUser
        if (!fromAddress.isNullOrBlank() && fromAddress != sshUser) sshUser = fromAddress
    }
    val user = parsed?.sshUser?.takeIf { it.isNotBlank() } ?: sshUser.trim()
    val sameSshHost = initial != null && initial.viaSsh && parsed != null &&
        initial.host == parsed.host && initial.port == parsed.port && initial.sshUser == user
    val pinned = if (sameSshHost) initial.sshHostKey else ""
    val credentialReady = if (useKey) privateKey.isNotBlank() else password.isNotBlank()
    val identityReady = !ssh || (user.isNotBlank() && credentialReady)
    val canSave = parsed != null && identityReady

    fun savedHost(viaTailscale: Boolean): SavedHost? {
        val address = parsed ?: return null
        return asSavedHost(
            initial = initial,
            label = label,
            parsed = address,
            viaTailscale = viaTailscale,
            sshUser = user,
            sshPassword = if (!useKey) password else "",
            sshPrivateKey = if (useKey) privateKey else "",
            sshPassphrase = if (useKey) passphrase else "",
            sshHostKey = pinned,
        )
    }

    val pickKey = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val key = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                }.getOrNull()
            }
            if (key.isNullOrBlank()) {
                toast(context, context.getString(R.string.ssh_key_unreadable))
            } else {
                privateKey = key
                useKey = true
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) stringResource(R.string.action_add_host) else stringResource(R.string.action_edit_host),
                color = Colors.ink,
            )
        },
        text = {
            Column(
                Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Field(
                    stringResource(R.string.field_name),
                    label,
                    placeholder = stringResource(R.string.field_name_optional),
                ) { label = it }
                Field(
                    stringResource(R.string.field_address),
                    address,
                    placeholder = stringResource(R.string.field_address_placeholder),
                ) { address = it }
                if (parsed != null) {
                    // What the app made of the address, as chips: where, how, and through what.
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ParsedChip(if (ssh && user.isNotBlank()) "$user@${parsed.host}:${parsed.port}" else "${parsed.host}:${parsed.port}", R.drawable.ic_host)
                        ParsedChip(
                            stringResource(if (ssh) R.string.host_transport_ssh else if (parsed.plaintext) R.string.host_transport_plain else R.string.host_transport_tls),
                            R.drawable.ic_swap,
                            warn = parsed.plaintext && !ssh,
                        )
                        if (parsed.viaTailscale) ParsedChip(stringResource(R.string.tailscale_title), R.drawable.ic_tailnet)
                    }
                } else {
                    // Nothing was typed yet, or what was typed is not an address: say what can be.
                    if (address.isNotBlank()) {
                        Text(stringResource(R.string.host_address_invalid), color = Colors.danger, fontSize = TextStyles.label)
                    }
                    Text(
                        stringResource(R.string.host_address_hint),
                        color = Colors.muted,
                        fontSize = TextStyles.label,
                        lineHeight = labelLeading,
                    )
                }
                if (ssh) {
                    Text(stringResource(R.string.ssh_address_hint), color = Colors.muted, fontSize = TextStyles.label)
                    Field(stringResource(R.string.ssh_user), sshUser) { sshUser = it }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                        ToggleButton(
                            checked = !useKey,
                            onCheckedChange = { useKey = false },
                            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.ssh_auth_password), maxLines = 1) }
                        ToggleButton(
                            checked = useKey,
                            onCheckedChange = { useKey = true },
                            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.ssh_auth_key), maxLines = 1) }
                    }
                    if (useKey) {
                        Field(stringResource(R.string.ssh_private_key), privateKey, password = true, singleLine = false, minLines = 3) { privateKey = it }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { pickKey.launch(arrayOf("*/*")) }, shapes = ButtonDefaults.shapes()) {
                                Glyph(R.drawable.ic_attach_file, MaterialTheme.colorScheme.primary, size = 18.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.ssh_key_file))
                            }
                            Spacer(Modifier.width(12.dp))
                            if (privateKey.isNotBlank()) {
                                Text(stringResource(R.string.ssh_key_loaded), color = Colors.ok, fontSize = TextStyles.label)
                            }
                        }
                        Field(stringResource(R.string.ssh_passphrase), passphrase, password = true) { passphrase = it }
                    } else {
                        Field(stringResource(R.string.ssh_password), password, password = true) { password = it }
                    }
                    if (!identityReady) {
                        Text(
                            stringResource(if (user.isBlank()) R.string.ssh_user_required else R.string.ssh_identity_required),
                            color = Colors.amber,
                            fontSize = TextStyles.label,
                        )
                    }
                    if (pinned.isNotBlank()) {
                        Text(stringResource(R.string.ssh_host_key_trusted, pinned), color = Colors.muted, fontSize = TextStyles.label)
                    }
                }
                if (parsed != null && parsed.viaTailscale && !tailscaleEnabled) {
                    Text(
                        stringResource(R.string.tailscale_need_join_host),
                        color = Colors.amber,
                        fontSize = TextStyles.label,
                        modifier = Modifier.clickable {
                            savedHost(viaTailscale = true)?.let(onSave)
                            onJoinTailnet()
                        },
                    )
                }
                if (parsed?.plaintext == true && !ssh) {
                    Text(stringResource(R.string.field_plaintext_warning), color = Colors.amber, fontSize = TextStyles.label)
                }
            }
        },
        confirmButton = {
            // Nothing to save until the address says where to connect and how to get in.
            Button(
                enabled = canSave,
                onClick = {
                    val address = parsed ?: return@Button
                    savedHost(viaTailscale = address.viaTailscale)?.let(onSave)
                },
                shapes = ButtonDefaults.shapes(),
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
    if (confirmDelete && onDelete != null && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.confirm_delete_host, initial.label), color = Colors.ink) },
            text = { Text(stringResource(R.string.confirm_delete_host_body), color = Colors.muted) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text(stringResource(R.string.action_delete), color = Colors.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel), color = Colors.muted) }
            },
        )
    }
}

/** One thing the address says, on a tonal chip; amber when it is a risk. */
@Composable
private fun ParsedChip(text: String, @androidx.annotation.DrawableRes icon: Int, warn: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    val fill = if (warn) Colors.amber.copy(alpha = 0.16f) else scheme.secondaryContainer
    val ink = if (warn) Colors.amber else scheme.onSecondaryContainer
    Row(
        Modifier.height(32.dp).clip(RoundedCornerShape(8.dp)).background(fill).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Glyph(icon, ink, size = 16.dp)
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

internal fun asSavedHost(
    initial: SavedHost?,
    label: String,
    parsed: HostAddress,
    viaTailscale: Boolean,
    sshUser: String = "",
    sshPassword: String = "",
    sshPrivateKey: String = "",
    sshPassphrase: String = "",
    sshHostKey: String = "",
) = SavedHost(
    id = initial?.id ?: HostStore.newId(),
    label = label.trim().ifBlank { parsed.host },
    host = parsed.host,
    port = parsed.port,
    plaintext = parsed.plaintext,
    viaTailscale = viaTailscale,
    sshUser = if (parsed.viaSsh) sshUser.trim() else "",
    sshPassword = if (parsed.viaSsh) sshPassword else "",
    sshPrivateKey = if (parsed.viaSsh) sshPrivateKey else "",
    sshPassphrase = if (parsed.viaSsh) sshPassphrase else "",
    sshHostKey = if (parsed.viaSsh) sshHostKey else "",
)
