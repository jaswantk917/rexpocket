package me.leaver.rexpocket.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.expandHorizontally
import androidx.compose.ui.graphics.blur.BlurStop
import androidx.compose.ui.graphics.blur.BlurRadiusSpec
import androidx.compose.ui.draw.blur
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.Immutable
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.foundation.layout.FlowRow
import me.leaver.rexpocket.data.FontCatalog
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
private const val SOURCE_URL = "https://github.com/jaswantk917/rexpocket"

/** The page margin: the cards and the floating controls stand this far from the screen's edge. */
private val PAGE = 16.dp

/** The first keyline, inside the page margin: marks, session names, tiles. */
private val KEY_MARK = 16.dp

/** The second keyline, inside the page margin: every title. */
private val KEY_TEXT = 64.dp

private val TILE = 36.dp

/** One session of a host, with the tabs the phone knows of; [view] is null until it is loaded. */
@Immutable
private data class HomeSession(
    val session: ListedSession,
    val view: SessionView?,
    val tabs: List<TabItem>,
    val activity: Boolean,
)

/**
 * One tab as its row shows it, worked out once per state. Rows take this rather than the whole
 * state, so a title or a status changing in one tab recomposes that row and not the list.
 */
@Immutable
private data class TabItem(
    val id: String,
    val title: String,
    val program: String?,
    val panes: Int,
    val activity: ProgramRecord?,
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
                val items = if (view == null) emptyList() else activeTabs.map { window ->
                    TabItem(
                        id = window.id,
                        title = tabTitle(view, window, state),
                        program = tabProgram(window, state),
                        panes = window.blocks.size,
                        activity = window.blocks.firstNotNullOfOrNull { state.activityOf(it) },
                    )
                }
                val shownTabs = if (labelMatch) items else items.filter { it.title.lowercase().contains(q) }
                if (!labelMatch && shownTabs.isEmpty()) return@mapNotNull null
                HomeSession(session, view, shownTabs, sessionActivity)
            }
            if (sessions.isEmpty() && (activeOnly || !hostMatch)) return@mapNotNull null
            host to sessions
        }
        val list = rememberLazyListState()
        val bar = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        // The button says its name only at the top of the list; anywhere else it is just a plus.
        val toolbarOpen by remember { derivedStateOf { !list.canScrollBackward } }
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
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RexLogo(collapsed = { bar.state.collapsedFraction })
                                Spacer(Modifier.width(12.dp))
                                Text(stringResource(R.string.home_title))
                            }
                        },
                        subtitle = { Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        actions = {
                            if (detail) {
                                // Beside the terminal the list is a column, too narrow for a button
                                // floating over its cards: the same action sits in the bar.
                                val addLabel = stringResource(R.string.session_new_on, (connected ?: hosts.first()).label)
                                Tip(addLabel) {
                                    FilledTonalIconButton(
                                        onClick = { onNewSession(connected ?: hosts.first()) },
                                        shapes = IconButtonDefaults.shapes(),
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                        ),
                                        modifier = Modifier.semantics { contentDescription = addLabel },
                                    ) {
                                        Glyph(R.drawable.ic_add, LocalContentColor.current)
                                    }
                                }
                            }
                            Tip(stringResource(R.string.action_search)) {
                                IconButton(onClick = { searchOpen = true }, shapes = IconButtonDefaults.shapes()) {
                                    Glyph(R.drawable.ic_search, LocalContentColor.current)
                                }
                            }
                            Tip(stringResource(R.string.settings)) {
                                IconButton(onClick = onSettings, shapes = IconButtonDefaults.shapes()) {
                                    Glyph(R.drawable.ic_settings, LocalContentColor.current)
                                }
                            }
                        },
                        // With the blur, Ghanti's header: no bar of its own, the page's colour fading from
                        // 85% to 65% over the softened cards (drawn behind it below). Without, a solid bar.
                        colors = if (BLUR_UNDER_BAR) {
                            TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent)
                        } else {
                            TopAppBarDefaults.topAppBarColors(containerColor = Colors.bg, scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer)
                        },
                        modifier = if (BLUR_UNDER_BAR) {
                            Modifier.drawBehind {
                                drawRect(Brush.verticalGradient(0f to Colors.bg.copy(alpha = 0.85f), 0.65f to Colors.bg.copy(alpha = 0.65f), 1f to Colors.bg.copy(alpha = 0.65f)))
                            }
                        } else {
                            Modifier
                        },
                        scrollBehavior = bar,
                    )
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().imePadding()) {
                val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                LazyColumn(
                    // Ghanti's edges: the list runs under the app bar and the nav bar and melts into a
                    // progressive blur there, held strong until close to the bar so the cards just
                    // under it are soft, not merely hazy. The bar's height is read while drawing, so
                    // collapsing it moves the blur without recomposing.
                    Modifier.fillMaxSize().blur {
                        if (!BLUR_UNDER_BAR) return@blur
                        val h = size.height.value
                        if (h <= 0f) return@blur
                        val topEnd = (padding.calculateTopPadding().value / h).coerceIn(0.01f, 0.4f)
                        val bottomStart = (1f - (navBottom + 24.dp).value / h).coerceIn(0.6f, 0.99f)
                        blurRadiusSpec = BlurRadiusSpec.verticalGradient(
                            listOf(
                                BlurStop(0f, 18.dp),
                                BlurStop(topEnd * 0.75f, 8.dp),
                                BlurStop(topEnd, 0.dp),
                                BlurStop(bottomStart, 0.dp),
                                BlurStop(1f, 24.dp),
                            ),
                        )
                    },
                    state = list,
                    contentPadding = PaddingValues(
                        start = PAGE,
                        end = PAGE,
                        top = padding.calculateTopPadding() + 4.dp,
                        // Clear of the toolbar, so the last tab can be scrolled above it.
                        bottom = if (searchOpen) 16.dp else navBottom + if (detail) 24.dp else 112.dp,
                    ),
                ) {
                    if (!searchOpen && hosts.isNotEmpty()) {
                        item(key = "filters") {
                            Row(Modifier.animateItem().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = activeOnly,
                                    onClick = {
                                        activeOnly = !activeOnly
                                        Haptics.toggle(view, activeOnly)
                                    },
                                    label = { Text(stringResource(R.string.filter_active)) },
                                    leadingIcon = {
                                        AnimatedVisibility(activeOnly, enter = expandHorizontally() + fadeIn(), exit = shrinkHorizontally() + fadeOut()) {
                                            Glyph(R.drawable.ic_check, LocalContentColor.current, size = FilterChipDefaults.IconSize)
                                        }
                                    },
                                    shape = RoundedCornerShape(50),
                                )
                            }
                        }
                    }
                    rows.forEachIndexed { index, (host, sessions) ->
                        val live = connected?.id == host.id
                        val hostExpanded = host.id !in collapsedHosts || searchOpen
                        item(key = "host-${host.id}") {
                            Column(Modifier.animateItem()) {
                                if (index > 0) Spacer(Modifier.height(20.dp))
                                HostCard(
                                    host = host,
                                    live = live,
                                    status = if (live) phaseText(state) else null,
                                    phase = if (live) state.phase else null,
                                    sessionCount = sessions.size,
                                    working = sessions.sumOf { r -> r.tabs.count { it.activity?.state == "working" } },
                                    waiting = sessions.sumOf { r -> r.tabs.count { it.activity?.state == "blocked" } },
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
                                ) { HostStatus(state, tailnetPath, onRetry, onProbePath) }
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                        // What needs a look, before everything else: every tab working or waiting,
                        // across the sessions, one tap from its terminal.
                        val attention = if (live && hostExpanded) {
                            sessions.flatMap { r -> r.tabs.filter { it.activity != null }.map { r to it } }
                                .sortedBy { (_, t) -> if (t.activity?.state == "blocked") 0 else 1 }
                        } else {
                            emptyList()
                        }
                        if (attention.isNotEmpty()) {
                            item(key = "attention-${host.id}") {
                                Column(Modifier.animateItem().padding(bottom = 20.dp)) {
                                    SectionTitle(stringResource(R.string.home_attention))
                                    LazyRow(
                                        // Edge to edge: the row scrolls under the page margins.
                                        modifier = Modifier.layout { measurable, constraints ->
                                            val bleed = PAGE.roundToPx() * 2
                                            val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + bleed))
                                            layout(constraints.maxWidth, placeable.height) { placeable.place(-bleed / 2, 0) }
                                        },
                                        contentPadding = PaddingValues(horizontal = PAGE),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        items(attention, key = { (r, t) -> r.session.id + "/" + t.id }) { (r, t) ->
                                            AttentionCard(r.session.label, r.session.id, t, Modifier.animateItem()) {
                                                onOpenTab(host, r.session.id, t.id)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (live && hostExpanded && sessions.isNotEmpty()) {
                            item(key = "sessions-title-${host.id}") {
                                SectionTitle(stringResource(R.string.header_sessions), Modifier.animateItem())
                            }
                        }
                        if (hostExpanded) {
                            sessions.forEach { row ->
                                val sessionExpanded = row.session.id !in collapsedSessions || searchOpen
                                val openingThis = opening == Opening.Existing(row.session.id)
                                val showTabs = sessionExpanded && row.view != null
                                item(key = "session-${row.session.id}") {
                                    Column(Modifier.animateItem()) {
                                        SessionHeader(
                                            row = row,
                                            live = live,
                                            opening = openingThis,
                                            expanded = sessionExpanded,
                                            joined = showTabs,
                                            opens = row.view == null,
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
                                                tint = rememberTint(row.session.id),
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
                                    // One item per tab: a session with many tabs is composed as it
                                    // scrolls in, a row at a time, not all at once.
                                    val onScreen = if (detail && live && state.activeSessionId == row.session.id) state.view?.activeWindowId else null
                                    row.tabs.forEachIndexed { index, tab ->
                                        item(key = "tab-${row.session.id}-${tab.id}") {
                                            Column(Modifier.animateItem()) {
                                                Spacer(Modifier.height(SEGMENT_GAP))
                                                TabRow(
                                                    tab = tab,
                                                    number = index + 1,
                                                    tint = rememberTint(row.session.id),
                                                    opening = openingThis,
                                                    open = tab.id == onScreen,
                                                    shape = segmentShape(index + 1, row.tabs.size + 1),
                                                    morphKey = tabMorphKey(row.session.id, tab.id),
                                                    onOpen = { onOpenTab(host, row.session.id, tab.id) },
                                                )
                                            }
                                        }
                                    }
                                }
                                item(key = "gap-${row.session.id}") { Spacer(Modifier.height(12.dp)) }
                            }
                        }
                    }
                    if (rows.isEmpty()) {
                        item { NotFound(activeOnly = activeOnly) }
                    }
                    if (!searchOpen && !activeOnly) {
                        item(key = "add-host") {
                            // Held to the left, so the floating button at the right never covers it.
                            TextButton(
                                onClick = { creating = true },
                                shapes = ButtonDefaults.shapes(),
                                modifier = Modifier.animateItem().padding(top = 4.dp).heightIn(min = 48.dp),
                            ) {
                                Glyph(R.drawable.ic_host, MaterialTheme.colorScheme.primary, size = 20.dp)
                                Spacer(Modifier.width(10.dp))
                                Text(stringResource(R.string.action_add_another_host))
                            }
                        }
                    }
                }
                if (!searchOpen && !detail) {
                    // The one thing this screen starts, kept quiet: a tonal button that folds to its
                    // plus as soon as the list moves, and grows into the new session's terminal.
                    val addLabel = stringResource(R.string.session_new_on, (connected ?: hosts.first()).label)
                    val fabShape = RoundedCornerShape(16.dp)
                    val morph = LocalPaneMorph.current
                    Box(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 16.dp, bottom = 16.dp)) {
                        Tip(addLabel) {
                            SmallExtendedFloatingActionButton(
                                text = { Text(stringResource(R.string.session_new)) },
                                icon = { Glyph(R.drawable.ic_add, MaterialTheme.colorScheme.onSecondaryContainer) },
                                onClick = {
                                    morph?.start(NEW_SESSION_MORPH, fabShape)
                                    onNewSession(connected ?: hosts.first())
                                },
                                expanded = toolbarOpen,
                                shape = fabShape,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                elevation = FloatingActionButtonDefaults.loweredElevation(),
                                modifier = Modifier.morphCard(NEW_SESSION_MORPH, fabShape).semantics { contentDescription = addLabel },
                            )
                        }
                    }
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
    keyBarAlt: Boolean,
    onKeyBarAlt: (Boolean) -> Unit,
    liveStatus: Boolean,
    onLiveStatus: (Boolean) -> Unit,
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
    val view = LocalView.current
    val setAlerts = { on: Boolean ->
        Haptics.toggle(view, on)
        onAlerts(on)
    }
    // Android 13 asks for notifications at run time; alerts come on once the person allows them.
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) setAlerts(true) else toast(context, context.getString(R.string.alerts_blocked))
    }
    val toggleAlerts = {
        when {
            alerts -> setAlerts(false)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED ->
                ask.launch(Manifest.permission.POST_NOTIFICATIONS)
            !Alerts.allowed(context) -> toast(context, context.getString(R.string.alerts_blocked))
            else -> setAlerts(true)
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
    val fontName = FontCatalog.find(terminalFont.selected)?.name ?: stringResource(R.string.terminal_font_system)

    SettingsScreen(
        SettingsModel(
            appearance = appearance,
            onAppearance = onAppearance,
            tailnet = tailnet,
            onTailscale = onTailscale,
            alerts = alerts,
            onAlerts = toggleAlerts,
            keyBarAlt = keyBarAlt,
            onKeyBarAlt = onKeyBarAlt,
            liveStatus = liveStatus,
            onLiveStatus = onLiveStatus,
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
private fun FoldChevron(expanded: Boolean, color: Color = Colors.muted) {
    val turn by animateFloatAsState(if (expanded) 0f else -90f, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "fold")
    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        Glyph(R.drawable.ic_expand, color, Modifier.rotate(turn))
    }
}

/**
 * A host's mark: the computer on a cookie that turns into a sunburst once it is the one connected,
 * its connection's colour cut into the corner.
 */
@Composable
private fun HostMark(live: Boolean, phase: Phase?, fill: Color, ink: Color) {
    Box(Modifier.size(56.dp)) {
        MorphBackdrop(active = live, color = fill, size = 56.dp) {
            Glyph(R.drawable.ic_host, ink, size = 26.dp)
        }
        if (phase != null) Dot(phaseTint(phase), 14.dp, Modifier.align(Alignment.BottomEnd), ring = Colors.bg)
    }
}

/**
 * One host. The connected one is a hero: a card in the host's own hue with a big shape turning
 * slowly in its corner, its name large, what it is doing, and a row of counts - sessions, working,
 * waiting - the way an app page leads with its numbers. A host that is not connected is a quiet
 * row that offers to connect. The menu is anchored to the card.
 */
@Composable
private fun HostCard(
    host: SavedHost,
    live: Boolean,
    status: String?,
    phase: Phase?,
    sessionCount: Int,
    working: Int,
    waiting: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onConnect: () -> Unit,
    onNewSession: () -> Unit,
    onEdit: () -> Unit,
    onDisconnect: () -> Unit,
    footer: @Composable () -> Unit,
) {
    val folded = stringResource(R.string.state_folded)
    val tint = rememberTint("host-" + host.id)
    var menu by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(32.dp)
    val fill by animateColorAsState(if (live) tint.container else Colors.surface, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "host-card")
    val ink = if (live) tint.onContainer else MaterialTheme.colorScheme.onSurface
    Box(
        // The fill comes after the press, so the whole card gives under the finger, not only what is on it.
        Modifier.fillMaxWidth()
            .pressable(shape, onLongClick = { menu = true }, onClick = onToggle).background(fill)
            .semantics { if (live && !expanded) stateDescription = folded },
    ) {
        // Behind the content, at the card's size: the shapes never make the card taller.
        if (live) {
            Box(Modifier.matchParentSize()) {
                Ornament(MaterialShapes.Cookie12Sided, tint.ornament, 220.dp, Modifier.align(Alignment.TopEnd).offset(x = 70.dp, y = (-60).dp))
                Ornament(MaterialShapes.Clover4Leaf, tint.ornament, 96.dp, Modifier.align(Alignment.BottomStart).offset(x = (-30).dp, y = 34.dp), periodMs = 32_000)
            }
        }
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = if (live) 20.dp else 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HostMark(live, phase, if (live) tint.accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHighest, if (live) tint.onContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        host.label,
                        style = if (live) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                        color = ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val line = if (live) status.orEmpty() else stringResource(R.string.host_not_connected)
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
                if (!live) {
                    FilledTonalButton(onClick = onConnect, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.action_connect)) }
                } else {
                    FoldChevron(expanded, ink)
                }
                Box {
                    RoundButton(R.drawable.ic_more, stringResource(R.string.action_more), { menu = true }, tint = ink.copy(alpha = 0.7f), size = 44.dp)
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
            if (live) {
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth().padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Stat(sessionCount, pluralStringResource(R.plurals.stat_sessions, sessionCount), ink, Modifier.weight(1f))
                    StatDivider(ink)
                    Stat(working, stringResource(R.string.stat_working), ink, Modifier.weight(1f))
                    StatDivider(ink)
                    Stat(waiting, stringResource(R.string.stat_waiting), if (waiting > 0) Colors.amber else ink, Modifier.weight(1f))
                }
                footer()
            }
        }
    }
}

/**
 * Rex's mark in the app bar: a cookie in the primary container that turns slowly, and that the
 * scroll turns further and shrinks as the large title folds away - the bar moves with the list.
 */
@Composable
private fun RexLogo(collapsed: () -> Float) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier.size(40.dp).graphicsLayer {
            val k = collapsed()
            val scale = 1f - 0.3f * k
            scaleX = scale
            scaleY = scale
        },
        contentAlignment = Alignment.Center,
    ) {
        Ornament(MaterialShapes.Cookie9Sided, scheme.primaryContainer, 40.dp, periodMs = 18_000)
        Box(Modifier.graphicsLayer { rotationZ = collapsed() * 180f }) {
            Glyph(R.drawable.ic_terminal, scheme.onPrimaryContainer, size = 22.dp)
        }
    }
}

/** A section of the list, titled as Orion titles its shelves: large and light, an action at the end. */
@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(start = 4.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        action()
    }
}

/**
 * A tab that needs a look, as a card on the shelf: its session's hue, what runs in it big on a
 * shape that keeps turning while it works, its name, its session, and what it is doing.
 */
@Composable
private fun AttentionCard(session: String, sessionId: String, tab: TabItem, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    val tint = rememberTint(sessionId)
    val waiting = tab.activity?.state == "blocked"
    val shape = RoundedCornerShape(28.dp)
    val morph = LocalPaneMorph.current
    val morphKey = shelfMorphKey(sessionId, tab.id)
    Column(
        modifier.width(168.dp).morphCard(morphKey, shape)
            .pressable(shape, onClick = {
                morph?.start(morphKey, shape)
                onOpen()
            })
            .background(tint.container).padding(16.dp),
    ) {
        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            // It turns while the agent works and rests while it waits for you.
            if (waiting) {
                StillOrnament(MaterialShapes.Cookie12Sided, tint.accent.copy(alpha = 0.18f), 64.dp)
            } else {
                Ornament(MaterialShapes.Sunny, tint.accent.copy(alpha = 0.18f), 64.dp, periodMs = 9_000)
            }
            ProgramTile(tab.program, size = 36.dp)
        }
        Spacer(Modifier.height(14.dp))
        Text(tab.title, style = MaterialTheme.typography.titleMedium, color = tint.onContainer, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
        Text(session, style = MaterialTheme.typography.bodySmall, color = tint.onContainer.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(12.dp))
        if (waiting) {
            StatusPill(tab.activity?.let { statusWord(it) }.orEmpty(), Colors.amber)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LoadingIndicator(Modifier.size(28.dp), color = tint.accent)
                Spacer(Modifier.width(6.dp))
                Text(tab.activity?.let { statusWord(it) }.orEmpty(), style = MaterialTheme.typography.labelLarge, color = tint.accent, maxLines = 1)
            }
        }
    }
}

/** One count in a hero's row: the number large, what it counts under it. The number rolls when it changes. */
@Composable
private fun Stat(value: Int, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedContent(value, transitionSpec = {
            val up = targetState > initialState
            (slideInVertically { if (up) it else -it } + fadeIn()) togetherWith (slideOutVertically { if (up) -it else it } + fadeOut())
        }, label = "stat") { n ->
            Text(n.toString(), style = MaterialTheme.typography.headlineSmall, color = color)
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = color.copy(alpha = 0.72f), maxLines = 1)
    }
}

@Composable
private fun StatDivider(color: Color) {
    Box(Modifier.width(1.dp).height(28.dp).background(color.copy(alpha = 0.18f)))
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
    Column(Modifier.padding(top = 12.dp)) {
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
 * A session: the head of its card, in the session's own hue - the hue its terminal wears too. Its
 * name, what it holds, the plus that opens a tab in it, and the fold, with a shape turning in its
 * corner. [joined] when its tabs follow it in the same card.
 */
@Composable
private fun SessionHeader(
    row: HomeSession,
    live: Boolean,
    opening: Boolean,
    expanded: Boolean,
    joined: Boolean,
    /** True when a tap opens the session rather than folding it. */
    opens: Boolean,
    onToggle: () -> Unit,
    onNewTab: () -> Unit,
) {
    val morph = LocalPaneMorph.current
    val morphKey = sessionMorphKey(row.session.id)
    val tint = rememberTint(row.session.id)
    val tabs = row.view?.windows?.size ?: 0
    val detail = when {
        opening -> stringResource(R.string.session_opening)
        row.view != null -> pluralStringResource(R.plurals.session_window_count, tabs, tabs)
        !live -> stringResource(R.string.session_cached)
        else -> null
    }
    val shape = if (joined) segmentShape(0, 2) else RoundedCornerShape(SEGMENT_OUTER)
    Box(
        Modifier.fillMaxWidth().morphCard(morphKey, shape)
            .pressable(shape, onClick = {
                if (opens) morph?.start(morphKey, shape)
                onToggle()
            })
            .background(tint.container),
    ) {
        Box(Modifier.matchParentSize()) {
            // Still, at an angle of its own: a list of turning shapes would never stop drawing.
            StillOrnament(MaterialShapes.Sunny, tint.ornament, 120.dp, Modifier.align(Alignment.CenterEnd).offset(x = 36.dp), angle = (row.session.id.hashCode() % 45).toFloat())
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(start = 20.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        row.session.label,
                        style = MaterialTheme.typography.titleLarge,
                        color = tint.onContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (row.activity) {
                        Spacer(Modifier.width(8.dp))
                        Dot(tint.accent, 8.dp)
                    }
                }
                if (detail != null) {
                    Text(detail, style = MaterialTheme.typography.bodySmall, color = tint.onContainer.copy(alpha = 0.72f), maxLines = 1)
                }
            }
            if (opening) LoadingIndicator(Modifier.size(32.dp), color = tint.accent)
            RoundButton(R.drawable.ic_add, stringResource(R.string.menu_new_tab), onNewTab, tint = tint.onContainer, size = 44.dp, enabled = !opening)
            if (row.view != null) FoldChevron(expanded, tint.onContainer)
        }
    }
}

/**
 * One tab, numbered as Harmonic numbers its stories: its number, the mark of what runs in it, its
 * name and panes, and what it is doing at the end - a living loader while an agent works, an amber
 * "needs you" while one waits. Its row is the session's hue, quieter; the tab beside the list wears
 * the full hue with an edge in the accent.
 */
@Composable
private fun TabRow(
    tab: TabItem,
    number: Int,
    tint: Tint,
    opening: Boolean,
    /** True for the tab the pane beside the list is showing. */
    open: Boolean,
    shape: RoundedCornerShape,
    morphKey: String,
    onOpen: () -> Unit,
) {
    val morph = LocalPaneMorph.current
    val selected = stringResource(R.string.state_selected)
    val fill by animateColorAsState(if (open) tint.container else tint.surface, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "tab")
    val edge by animateDpAsState(if (open) 4.dp else 0.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "tab-edge")
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = 64.dp).morphCard(morphKey, shape)
            .pressable(shape, enabled = !opening, onClick = {
                morph?.start(morphKey, shape)
                onOpen()
            })
            .background(fill)
            .semantics { if (open) stateDescription = selected },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(edge).fillMaxHeight().background(tint.accent))
        Text(
            number.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = tint.onContainer.copy(alpha = 0.55f),
            textAlign = TextAlign.End,
            modifier = Modifier.width(36.dp),
        )
        Spacer(Modifier.width(12.dp))
        ProgramTile(tab.program, size = TILE)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text(
                tab.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = tint.onContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                pluralStringResource(R.plurals.pane_count, tab.panes, tab.panes),
                style = MaterialTheme.typography.bodySmall,
                color = tint.onContainer.copy(alpha = 0.65f),
                maxLines = 1,
            )
        }
        val activity = tab.activity
        AnimatedContent(activity?.state, transitionSpec = { (scaleIn(initialScale = 0.6f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut()) }, label = "tab-activity") { state ->
            when (state) {
                "working" -> LoadingIndicator(Modifier.size(36.dp), color = tint.accent)
                "blocked" -> activity?.let { StatusPill(statusWord(it).orEmpty(), Colors.amber) }
                else -> Spacer(Modifier.size(1.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
    }
}

/** A short state on a tonal pill in its own colour. */
@Composable
private fun StatusPill(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        // Wraps rather than cut "waiting for you" at large text; 16dp is still a full pill on one line.
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.16f)).padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun EmptyTabs(text: String, tint: Tint, canOpen: Boolean, onOpen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(segmentShape(1, 2)).background(tint.surface)
            .padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = tint.onContainer.copy(alpha = 0.72f), modifier = Modifier.weight(1f))
        if (canOpen) {
            TextButton(onClick = onOpen, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.session_tap_open), color = tint.accent) }
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
            Modifier.size(132.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(scheme.primaryContainer),
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
                    )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.home_no_hosts_body),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(scheme.surfaceContainerHigh).padding(16.dp),
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onAdd,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.heightIn(min = ButtonDefaults.MediumContainerHeight),
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

/**
 * The edge blur needs Android 13 to vary its radius, and is heavy for Mali GPUs, so it is on for
 * Pixels only, as in Ghanti; elsewhere the bar stays solid instead of letting sharp cards through.
 */
private val BLUR_UNDER_BAR = android.os.Build.VERSION.SDK_INT >= 33 && android.os.Build.MANUFACTURER.equals("Google", ignoreCase = true)
