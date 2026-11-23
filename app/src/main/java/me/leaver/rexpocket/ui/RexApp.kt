package me.leaver.rexpocket.ui

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import me.leaver.rexpocket.R
import me.leaver.rexpocket.core.Trouble
import me.leaver.rexpocket.data.SavedHost
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import me.leaver.rexpocket.update.InstallResult
import me.leaver.rexpocket.update.UpdateState

// The app root: lays the list and the pane out for the window in hand, and hosts the dialogs that
// float over both.

@Composable
fun RexPocketApp(model: RexViewModel = viewModel()) {
    val route by model.route.collectAsState()
    val hosts by model.hosts.collectAsState()
    val pocket by model.pocket.collectAsState()
    val font by model.fontScale.collectAsState()
    val terminalTypeface by model.terminalTypeface.collectAsState()
    val terminalFont by model.terminalFont.collectAsState()
    var fontSettingsOpen by remember { mutableStateOf(false) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    val tailscale by model.tailscale.collectAsState()
    val tailscaleStatus by model.tailscaleStatus.collectAsState()
    val tailnetError by model.tailnetError.collectAsState()
    val opening by model.opening.collectAsState()
    val tailnetPath by model.tailnetPath.collectAsState()
    val alerts by model.alerts.collectAsState()
    val keyBarShift by model.keyBarShift.collectAsState()
    val keyBarAlt by model.keyBarAlt.collectAsState()
    val keyBar by model.keyBar.collectAsState()
    // Held here, not in the terminal screen: going back to the list ends that screen, and a key bar
    // put away by hand must stay away in the next session too.
    var keyBarChoice by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val upload by model.upload.collectAsState()
    val update by model.update.collectAsState()
    val updateChannel by model.updateChannel.collectAsState()
    val context = LocalContext.current
    var tailscaleOpen by remember { mutableStateOf(false) }
    // Each offer is shown once per app run; the settings row still has it afterwards.
    var offeredOnce by remember { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(Unit) { model.restore() }
    // Once a day: look for a newer build and say so. Nothing is downloaded until it is asked for.
    LaunchedEffect(Unit) { model.checkForUpdates() }

    // The settings row and the offer do the same next thing: download what was found, install what
    // was downloaded, or ask again. Nothing here runs on its own.
    val install = {
        if (model.installUpdate() == InstallResult.NeedsPermission) {
            toast(context, context.getString(R.string.update_allow_install))
        }
    }
    val updateTapped: (UpdateState) -> Unit = { state ->
        when (state) {
            is UpdateState.Available -> model.downloadUpdate()
            is UpdateState.Ready -> install()
            is UpdateState.Downloading -> Unit
            else -> model.checkForUpdates(force = true)
        }
    }
    LifecycleStartEffect(model) {
        model.setForeground(true)
        onStopOrDispose { model.setForeground(false) }
    }
    // A node that still needs a login can only say so through the link it prints, so take the
    // user straight to it instead of leaving a URL to copy.
    val authUrl = tailscaleStatus.authUrl
    LaunchedEffect(authUrl) {
        if (authUrl.isBlank()) return@LaunchedEffect
        openLink(context, authUrl)
    }
    val appearance by model.appearance.collectAsState()
    RexTheme(appearance) {
        Box(Modifier.fillMaxSize().background(Colors.bg)) {
            // A tablet or a desk window keeps the list beside the pane: a session or a tab is
            // switched without leaving the terminal it was showing. With nothing open, the pane
            // side says so and waits for a pick.
            val home: @Composable (Boolean) -> Unit = { detail ->
                HomeScreen(
                    detail = detail,
                    hosts = hosts,
                    connected = model.host,
                    state = pocket,
                    tailscale = tailscale,
                    tailnetPath = tailnetPath,
                    opening = opening,
                    cachedSessions = model::cachedSessions,
                    onConnect = { model.connect(it) },
                    onOpenSession = model::openSessionOn,
                    onOpenTab = model::openTabOn,
                    onNewSession = model::createSessionOn,
                    onNewTab = model::newTabOn,
                    onDisconnect = model::disconnect,
                    onRetry = model::retryNow,
                    onProbePath = model::probeTailnetPath,
                    onSave = model::saveHost,
                    onDelete = model::deleteHost,
                    onTailscale = { tailscaleOpen = true },
                    onSettings = { settingsOpen = true },
                )
            }
            val term: @Composable (Boolean) -> Unit = { embedded ->
                TermScreen(
                    embedded = embedded,
                    keyBarWithKeyboard = keyBar,
                    keyBarChoice = keyBarChoice,
                    onKeyBarChoice = { keyBarChoice = it },
                    keyBarShift = keyBarShift,
                    keyBarAlt = keyBarAlt,
                    state = pocket,
                    fontScale = font,
                    terminalTypeface = terminalTypeface,
                    onFont = model::setFont,
                    onBack = model::showHome,
                    onOpenSession = model::openSession,
                    watchPane = model::watchPane,
                    onRetry = model::retryNow,
                    onDismissNotice = model::dismissNotice,
                    onAct = model::act,
                    upload = upload,
                    onAttach = model::attachUpload,
                    onCancelUpload = model::cancelUpload,
                    onDismissUpload = model::dismissUpload,
                    onRetryUpload = model::retryUpload,
                    onSettings = { settingsOpen = true },
                )
            }
            // Navigation 3: the host list is the list pane and the terminal the detail pane, side by
            // side when the window has room for both, one at a time when it does not. The back stack
            // follows the model's route, which reconnects, alerts and a closed session also move.
            val backStack = remember(route, settingsOpen) {
                buildList<NavKey> {
                    add(HostsKey)
                    if (route == Route.Term) add(TerminalKey)
                    if (settingsOpen) add(SettingsKey)
                }
            }
            val directive = rememberPaneDirective()
            val twoPanes = directive.maxHorizontalPartitions > 1
            val strategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)
            val slide = with(LocalDensity.current) { NAV_SLIDE.roundToPx() }
            NavDisplay(
                backStack = backStack,
                onBack = { if (settingsOpen) settingsOpen = false else model.showHome() },
                sceneStrategies = listOf(DialogSceneStrategy(), strategy),
                transitionSpec = { navForward(slide) },
                popTransitionSpec = { navBack(slide) },
                predictivePopTransitionSpec = { navBack(slide) },
                entryProvider = entryProvider {
                    entry<HostsKey>(metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = { PaneCard { NoPaneOpen() } })) {
                        if (twoPanes) {
                            home(route == Route.Term)
                        } else {
                            // One column in a window too wide for one reads better held to a width.
                            BoxWithConstraints(Modifier.fillMaxSize().background(Colors.bg), contentAlignment = Alignment.TopCenter) {
                                Box(Modifier.width(minOf(maxWidth, COLUMN_MAX)).fillMaxHeight()) { home(false) }
                            }
                        }
                    }
                    entry<TerminalKey>(metadata = ListDetailSceneStrategy.detailPane()) {
                        // Beside the list there is no back button: the list is the way back.
                        if (twoPanes) PaneCard { term(true) } else term(false)
                    }
                    entry<SettingsKey>(
                        // A dialog scene in every window, so what is under it stays composed: the
                        // terminal keeps its stream, scrollback, latched keys and focus. Beside the
                        // list it is a card over both panes; on a phone it fills the screen.
                        metadata = DialogSceneStrategy.dialog(DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)),
                    ) {
                        val settings: @Composable () -> Unit = {
                            Settings(
                                onBack = { settingsOpen = false },
                                showBack = !twoPanes,
                                appearance = appearance,
                                onAppearance = model::setAppearance,
                                terminalFont = terminalFont,
                                tailscale = tailscale,
                                tailscaleStatus = tailscaleStatus,
                                tailnetError = tailnetError,
                                onTailscale = { tailscaleOpen = true },
                                alerts = alerts,
                                onAlerts = model::setAlerts,
                                keyBarShift = keyBarShift,
                                keyBarAlt = keyBarAlt,
                                onKeyBarShift = model::setKeyBarShift,
                                onKeyBarAlt = model::setKeyBarAlt,
                                keyBar = keyBar,
                                onKeyBar = model::setKeyBar,
                                onFontSettings = { fontSettingsOpen = true },
                                onDiagnosticsReport = { limit -> model.diagnosticReport(limit) },
                                update = update,
                                onUpdate = { updateTapped(update) },
                                updateChannel = updateChannel,
                                onUpdateChannel = model::setUpdateChannel,
                            )
                        }
                        DialogBars()
                        if (twoPanes) {
                            Box(Modifier.widthIn(max = 600.dp).fillMaxWidth(0.9f).fillMaxHeight(0.9f).clip(RoundedCornerShape(28.dp))) { settings() }
                        } else {
                            Box(Modifier.fillMaxSize().enterFromEnd()) { settings() }
                        }
                    }
                },
            )
        }
        val offered = update
        val offeredTag = when (offered) {
            is UpdateState.Available -> offered.tag
            is UpdateState.Ready -> offered.tag
            else -> null
        }
        val offerKey = offeredTag?.let { if (offered is UpdateState.Ready) "$it-ready" else "$it-available" }
        if (offeredTag != null && offerKey != null && offerKey !in offeredOnce) {
            UpdateOfferDialog(
                tag = offeredTag,
                downloaded = offered is UpdateState.Ready,
                onConfirm = {
                    offeredOnce = offeredOnce + offerKey
                    updateTapped(offered)
                },
                onLater = { offeredOnce = offeredOnce + offerKey },
            )
        }
        if (fontSettingsOpen) {
            FontSettingsDialog(terminalFont, terminalTypeface, model::selectTerminalFont) { fontSettingsOpen = false }
        }
        if (tailscaleOpen) {
            TailscaleDialog(
                initial = tailscale,
                status = tailscaleStatus,
                error = tailnetError,
                onDismiss = { tailscaleOpen = false },
                onJoin = { name ->
                    model.joinTailnet(name)
                    tailscaleOpen = false
                },
                onLeave = {
                    model.leaveTailnet()
                    tailscaleOpen = false
                },
            )
        }
        val sshHost = model.host
        val sshProblem = pocket.problem
        if (sshHost != null && sshHost.viaSsh && sshProblem != null &&
            (sshProblem.trouble == Trouble.SshHostKeyUnknown || sshProblem.trouble == Trouble.SshHostKeyChanged)
        ) {
            SshHostKeyDialog(
                host = sshHost,
                changed = sshProblem.trouble == Trouble.SshHostKeyChanged,
                fingerprint = sshProblem.detail.orEmpty(),
                onTrust = { fingerprint -> model.trustSshHostKey(fingerprint) },
                onCancel = { model.disconnect() },
            )
        }
    }
}

/**
 * The right side of a two-pane window: the terminal on a rounded sheet of its own, set in from the
 * window's edges on the page the list sits on, so the two read as panes rather than two halves.
 */
@Composable
internal fun PaneCard(content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Colors.bg)
            .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top + WindowInsetsSides.End + WindowInsetsSides.Bottom))
            .padding(start = 8.dp, top = 8.dp, end = 16.dp, bottom = 16.dp)
            .clip(PANE_SHAPE)
            // At night the terminal's black and the page are close: a faint edge keeps the sheet a sheet.
            .then(if (Colors.isDark) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), PANE_SHAPE) else Modifier),
    ) { content() }
}

private val PANE_SHAPE = RoundedCornerShape(28.dp)

/**
 * The pane side of a window that has room for two, with nothing open in it yet: the list on the
 * left is where a pane is picked, and it stays there.
 */
@Composable
private fun NoPaneOpen() {
    Column(
        Modifier.fillMaxSize().background(Colors.terminal).statusBarsPadding().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Glyph(R.drawable.ic_sessions, Colors.muted, size = 44.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.pane_none_title),
            color = Colors.ink,
            fontSize = TextStyles.title,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 360.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.pane_none_body),
            color = Colors.muted,
            fontSize = TextStyles.label,
            lineHeight = labelLeading,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 360.dp),
        )
    }
}

/**
 * The SSH key of the machine, before anything about the account is sent. First use and a changed
 * key take the same dialog; only the wording, and how loudly it warns, differ.
 */
@Composable
private fun SshHostKeyDialog(
    host: SavedHost,
    changed: Boolean,
    fingerprint: String,
    onTrust: (String) -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.ssh_host_key_title, host.label), color = Colors.ink) },
        text = {
            Text(
                stringResource(
                    if (changed) R.string.ssh_host_key_changed_body else R.string.ssh_host_key_unknown_body,
                    fingerprint,
                ),
                color = Colors.muted,
            )
        },
        confirmButton = {
            TextButton(onClick = { onTrust(fingerprint) }) {
                Text(stringResource(R.string.action_trust), color = if (changed) Colors.danger else Colors.accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel), color = Colors.muted) }
        },
    )
}

/** A dialog window edge to edge draws its own bars: their icons follow the theme, as the app's do. */
@Composable
private fun DialogBars() {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window ?: return
    val view = LocalView.current
    val dark = Colors.isDark
    SideEffect {
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
        window.setDimAmount(0f)
    }
}

/** A full-screen dialog arrives the way a pushed screen does: sliding in from the end, fading up. */
@Composable
private fun Modifier.enterFromEnd(): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(450, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))) }
    return graphicsLayer {
        translationX = (1f - progress.value) * NAV_SLIDE.toPx()
        alpha = progress.value
    }
}
