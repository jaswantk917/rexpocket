package me.leaver.rexpocket.ui

import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import me.leaver.rexpocket.BuildConfig
import me.leaver.rexpocket.R
import me.leaver.rexpocket.data.Appearance
import me.leaver.rexpocket.core.UpdateChannel
import me.leaver.rexpocket.update.UpdateState

/** What the settings screen shows and changes; the app holds it and passes it in. */
internal class SettingsModel(
    val appearance: Appearance,
    val onAppearance: (Appearance) -> Unit,
    val tailnet: String,
    val onTailscale: () -> Unit,
    val alerts: Boolean,
    val onAlerts: () -> Unit,
    val keyBar: Boolean,
    val onKeyBar: (Boolean) -> Unit,
    val keyBarAlt: Boolean,
    val onKeyBarAlt: (Boolean) -> Unit,
    val liveStatus: Boolean,
    val onLiveStatus: (Boolean) -> Unit,
    val terminalFont: String,
    val onTerminalFont: () -> Unit,
    val recording: Boolean,
    val onDiagnostics: () -> Unit,
    val update: UpdateState,
    val onUpdate: () -> Unit,
    val channel: UpdateChannel,
    val onChannel: (UpdateChannel) -> Unit,
    val onSource: () -> Unit,
)

/**
 * Settings, as a screen of its own, in the grammar of Android's own settings: a large title, and
 * groups of rows on rounded cards that the page shows through between rows. Each group has its
 * colour, in the tile behind each row's icon. Whole rows toggle; a value sits in the accent at the
 * end of a row that opens something.
 */
@Composable
internal fun SettingsScreen(settings: SettingsModel, onBack: () -> Unit, showBack: Boolean = true) {
    val bar = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scheme = MaterialTheme.colorScheme
    val view = LocalView.current
    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(bar.nestedScrollConnection),
        containerColor = Colors.bg,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    RoundButton(
                        if (showBack) R.drawable.ic_back else R.drawable.ic_close,
                        stringResource(if (showBack) R.string.action_back else R.string.action_close),
                        onBack,
                        plate = scheme.surfaceContainerHighest,
                        size = 40.dp,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Colors.bg, scrolledContainerColor = scheme.surfaceContainer),
                scrollBehavior = bar,
            )
        },
    ) { padding ->
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            Modifier.fillMaxSize(),
            state = rememberLazyListState(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = navBottom + 32.dp),
        ) {
            // The switches first, as tiles under the thumb: each is one tap, and the on ones fill.
            item(key = "quick") {
                Column(Modifier.padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.settings_quick),
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.primary,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickTile(R.drawable.ic_settings_dark, R.string.appearance_pure_black, settings.appearance.pureBlack, Modifier.weight(1f)) {
                            Haptics.toggle(view, it)
                            settings.onAppearance(settings.appearance.copy(pureBlack = it))
                        }
                        QuickTile(R.drawable.ic_text, R.string.appearance_app_font, settings.appearance.appFont, Modifier.weight(1f)) {
                            Haptics.toggle(view, it)
                            settings.onAppearance(settings.appearance.copy(appFont = it))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickTile(R.drawable.ic_keyboard, R.string.keybar_title, settings.keyBar, Modifier.weight(1f)) {
                            Haptics.toggle(view, it)
                            settings.onKeyBar(it)
                        }
                        // No tick here: turning alerts on may stop at a permission prompt; the tick
                        // comes when the switch actually moves.
                        QuickTile(R.drawable.ic_notifications, R.string.alerts_title, settings.alerts, Modifier.weight(1f)) { settings.onAlerts() }
                    }
                }
            }
            item(key = "appearance") {
                Card(R.string.settings_group_appearance, 0, MaterialShapes.Flower) {
                    SchemePicker(settings.appearance) { settings.onAppearance(settings.appearance.copy(scheme = it)) }
                }
            }
            item(key = "terminal") {
                Card(R.string.settings_group_terminal, 4, MaterialShapes.Cookie6Sided) {
                    LinkRow(R.drawable.ic_text, stringResource(R.string.terminal_font_title), settings.terminalFont, settings.onTerminalFont)
                    SwitchRow(R.drawable.ic_keyboard, stringResource(R.string.keybar_alt), settings.keyBarAlt, settings.onKeyBarAlt)
                }
            }
            item(key = "connection") {
                Card(R.string.settings_group_connection, 1, MaterialShapes.Clover8Leaf) {
                    LinkRow(R.drawable.ic_tailnet, stringResource(R.string.tailscale_title), settings.tailnet, settings.onTailscale)
                    // It is the alerts' ongoing notification, so it exists only while they are on.
                    if (settings.alerts) {
                        SwitchRow(R.drawable.ic_notifications, stringResource(R.string.live_setting), settings.liveStatus, settings.onLiveStatus)
                    }
                }
            }
            item(key = "version") {
                Card(R.string.settings_group_version, 3, MaterialShapes.SoftBurst) {
                    ChannelRow(settings.channel, settings.onChannel)
                    LinkRow(R.drawable.ic_arrow_down, stringResource(R.string.update_title), updateDetail(settings.update).orEmpty(), settings.onUpdate)
                }
            }
            item(key = "about") {
                Card(R.string.settings_group_about, 5, MaterialShapes.Puffy) {
                    LinkRow(
                        R.drawable.ic_build,
                        stringResource(R.string.diagnostics),
                        stringResource(if (settings.recording) R.string.diagnostics_state_on else R.string.diagnostics_state_off),
                        settings.onDiagnostics,
                    )
                    LinkRow(R.drawable.ic_open_in_new, stringResource(R.string.settings_source), "", settings.onSource)
                }
            }
            item(key = "credits") {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 20.dp)) {
                    Text("RexPocket ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.settings_credits), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** The hue of the card a row sits in; rows take their colours from it. */
private val LocalCard = staticCompositionLocalOf<Tint?> { null }

/**
 * A quick switch, as Orion's quick settings draw them: a rounded tile at rest that fills with the
 * accent and rounds into a pill when on, its icon's backdrop turning into another shape with it.
 */
@Composable
private fun QuickTile(@DrawableRes icon: Int, @StringRes label: Int, on: Boolean, modifier: Modifier = Modifier, onChange: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val corner by animateDpAsState(if (on) 38.dp else 22.dp, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "tile-shape")
    val fill by animateColorAsState(if (on) scheme.primary else scheme.surfaceContainerHigh, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "tile-fill")
    val ink = if (on) scheme.onPrimary else scheme.onSurface
    val shape = RoundedCornerShape(corner)
    Row(
        modifier.height(76.dp).clip(shape).background(fill)
            .toggleable(on, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MorphBackdrop(on, ink.copy(alpha = 0.14f), 44.dp, from = MaterialShapes.Circle, to = MaterialShapes.Cookie9Sided) {
            Glyph(icon, ink, size = 22.dp)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(label), style = MaterialTheme.typography.titleSmall, color = ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(stringResource(if (on) R.string.state_on else R.string.state_off), style = MaterialTheme.typography.labelMedium, color = ink.copy(alpha = 0.72f))
        }
    }
}

/**
 * A category: a card in its own hue with its name large and a shape turning in its corner, its rows
 * under it in the same hue, quieter, with the page showing through 2 dp between them.
 */
@Composable
private fun Card(@StringRes title: Int, hue: Int, shape: androidx.graphics.shapes.RoundedPolygon, rows: @Composable () -> Unit) {
    val tint = rememberTint("settings", hue)
    Column(Modifier.padding(bottom = 16.dp).clip(RoundedCornerShape(28.dp)), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(Modifier.fillMaxWidth().background(tint.container)) {
            Box(Modifier.matchParentSize().clipToBounds()) {
                StillOrnament(shape, tint.ornament, 150.dp, Modifier.align(Alignment.TopEnd).offset(x = 40.dp, y = (-46).dp), angle = hue * 17f)
            }
            Text(
                stringResource(title),
                style = MaterialTheme.typography.headlineSmall,
                color = tint.onContainer,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 18.dp),
            )
        }
        CompositionLocalProvider(LocalCard provides tint) { rows() }
    }
}

@Composable
private fun RowTile(@DrawableRes icon: Int) {
    val tint = LocalCard.current
    Box(
        Modifier.size(40.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(tint?.container ?: MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Glyph(icon, tint?.onContainer ?: MaterialTheme.colorScheme.onSecondaryContainer, size = 22.dp)
    }
}

@Composable
private fun RowText(title: String, detail: String?, modifier: Modifier = Modifier) {
    val ink = LocalCard.current?.onContainer ?: MaterialTheme.colorScheme.onSurface
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = ink)
        if (!detail.isNullOrEmpty()) {
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = ink.copy(alpha = 0.72f))
        }
    }
}

private val ROW_MIN = 72.dp

@Composable
private fun rowFill(): Color = LocalCard.current?.surface ?: Colors.surface

/** A row that opens something; its current value at the end, in the card's accent. */
@Composable
private fun LinkRow(@DrawableRes icon: Int, title: String, value: String, onClick: () -> Unit) {
    val tint = LocalCard.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = ROW_MIN)
            .pressable(RoundedCornerShape(4.dp), onClick = onClick).background(rowFill())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowTile(icon)
        Spacer(Modifier.width(16.dp))
        RowText(title, null, Modifier.weight(1f))
        if (value.isNotEmpty()) {
            Spacer(Modifier.width(12.dp))
            Text(
                value,
                style = MaterialTheme.typography.labelLarge,
                color = tint?.accent ?: MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 160.dp),
            )
        }
        Glyph(R.drawable.ic_chevron_right, (tint?.onContainer ?: MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.7f), size = 20.dp)
    }
}

@Composable
private fun SwitchRow(@DrawableRes icon: Int, title: String, on: Boolean, onChange: (Boolean) -> Unit) {
    val view = LocalView.current
    val toggle = {
        Haptics.toggle(view, !on)
        onChange(!on)
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = ROW_MIN)
            .pressable(RoundedCornerShape(4.dp), role = Role.Switch, onClick = toggle).background(rowFill())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowTile(icon)
        Spacer(Modifier.width(16.dp))
        RowText(title, null, Modifier.weight(1f))
        Switch(checked = on, onCheckedChange = null)
    }
}

/** Stable or preview, as two connected toggle buttons on their own line under the title. */
@Composable
private fun ChannelRow(channel: UpdateChannel, onChannel: (UpdateChannel) -> Unit) {
    val view = LocalView.current
    Column(Modifier.fillMaxWidth().background(rowFill()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowTile(R.drawable.ic_swap)
            Spacer(Modifier.width(16.dp))
            RowText(stringResource(R.string.update_channel), stringResource(R.string.update_channel_detail), Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
            listOf(UpdateChannel.Stable to R.string.update_channel_stable, UpdateChannel.Preview to R.string.update_channel_preview)
                .forEachIndexed { index, (value, label) ->
                    ToggleButton(
                        checked = channel == value,
                        onCheckedChange = {
                            if (channel != value) {
                                Haptics.tick(view)
                                onChannel(value)
                            }
                        },
                        shapes = if (index == 0) ButtonGroupDefaults.connectedLeadingButtonShapes() else ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        modifier = Modifier.weight(1f).semantics { selected = channel == value },
                    ) { Text(stringResource(label), maxLines = 1) }
                }
        }
    }
}

/**
 * The colours, as swatches of the schemes themselves: the wallpaper's first where the phone has it,
 * then the seeds. The chosen one turns into a cookie with a check, as Orion's palette picker does.
 */
@Composable
private fun SchemePicker(appearance: Appearance, onPick: (String) -> Unit) {
    val context = LocalContext.current
    val dark = Colors.isDark
    val view = LocalView.current
    val options = buildList {
        if (dynamicColorAvailable) add(Triple(Appearance.DYNAMIC, R.string.appearance_scheme_wallpaper, colorSchemeFor(context, Appearance(), dark).primary))
        SchemeSeed.entries.forEach { seed ->
            val label = when (seed) {
                SchemeSeed.Rex -> R.string.appearance_scheme_rex
                SchemeSeed.Blue -> R.string.appearance_scheme_blue
                SchemeSeed.Violet -> R.string.appearance_scheme_violet
                SchemeSeed.Amber -> R.string.appearance_scheme_amber
                SchemeSeed.Slate -> R.string.appearance_scheme_slate
            }
            add(Triple(seed.id, label, dynamicColorScheme(seed.seed, dark, false, style = PaletteStyle.TonalSpot).primary))
        }
    }
    val current = if (appearance.scheme == Appearance.DYNAMIC && !dynamicColorAvailable) SchemeSeed.Rex.id else appearance.scheme
    val chosen = options.firstOrNull { it.first == current }
    Column(Modifier.fillMaxWidth().background(rowFill()).padding(vertical = 16.dp)) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            RowTile(R.drawable.ic_palette)
            Spacer(Modifier.width(16.dp))
            RowText(stringResource(R.string.appearance_scheme), chosen?.let { stringResource(it.second) }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            options.forEach { (id, label, color) ->
                item(key = id) {
                    val on = id == current
                    Swatch(color, on, stringResource(label), wallpaper = id == Appearance.DYNAMIC) {
                        if (!on) {
                            Haptics.tick(view)
                            onPick(id)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Swatch(color: Color, on: Boolean, label: String, wallpaper: Boolean, onClick: () -> Unit) {
    val turn by animateFloatAsState(if (on) 0f else -45f, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "swatch-turn")
    val ring by animateColorAsState(if (on) MaterialTheme.colorScheme.onSurface else Color.Transparent, MaterialTheme.motionScheme.fastEffectsSpec(), label = "swatch-ring")
    val shape = if (on) MaterialShapes.Cookie9Sided.toShape() else CircleShape
    Box(
        Modifier.size(SWATCH).semantics { contentDescription = label; selected = on }
            .pressable(CircleShape, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(SWATCH).graphicsLayer { rotationZ = turn }.clip(shape).background(ring).padding(if (on) 3.dp else 0.dp)
                .clip(shape)
                .background(
                    if (wallpaper) Brush.sweepGradient(listOf(color, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.secondary, color))
                    else Brush.linearGradient(listOf(color, color)),
                ),
        )
        if (on) Glyph(R.drawable.ic_check, contentColorFor(color), size = 24.dp)
    }
}

private val SWATCH: Dp = 56.dp

/** Black or white, whichever reads on [color]. */
private fun contentColorFor(color: Color): Color =
    if (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue > 0.6) Color.Black else Color.White
