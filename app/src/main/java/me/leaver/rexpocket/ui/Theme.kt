package me.leaver.rexpocket.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.ktx.harmonize
import me.leaver.rexpocket.data.Appearance
import me.leaver.rexpocket.terminal.TerminalTheme

/**
 * A seed the app's colours can grow from when the wallpaper's are not wanted, or not offered
 * (before Android 12). Rex's own green comes first: it is the prompt on the terminal tile.
 */
internal enum class SchemeSeed(val id: String, val seed: Color) {
    Rex("rex", Color(0xFF2E9D5B)),
    Blue("blue", Color(0xFF3A6FD8)),
    Violet("violet", Color(0xFF7A5AF0)),
    Amber("amber", Color(0xFFE09A1F)),
    Slate("slate", Color(0xFF5F7484)),
    ;

    companion object {
        fun of(id: String): SchemeSeed? = entries.firstOrNull { it.id == id }
    }
}

/** True when this phone can take its colours from the wallpaper. */
internal val dynamicColorAvailable: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * The colour scheme for an appearance: the wallpaper's on Android 12 and later unless a seed was
 * picked, Rex green otherwise. Pure black takes the dark surfaces to black, for OLED and for a
 * terminal that should read as one sheet of black glass.
 */
internal fun colorSchemeFor(context: Context, appearance: Appearance, dark: Boolean): ColorScheme {
    val seed = SchemeSeed.of(appearance.scheme)
    val base = when {
        seed == null && dynamicColorAvailable -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> dynamicColorScheme(seedColor = (seed ?: SchemeSeed.Rex).seed, isDark = dark, isAmoled = false, style = PaletteStyle.TonalSpot)
    }
    return if (dark && appearance.pureBlack) base.pureBlack() else base
}

/** Harmonic's pure palette: black page, neutral greys stepping up for each container. */
private fun ColorScheme.pureBlack() = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0C0C0C),
    surfaceContainer = Color(0xFF141414),
    surfaceContainerHigh = Color(0xFF1F1F1F),
    surfaceContainerHighest = Color(0xFF292929),
    surfaceBright = Color(0xFF292929),
)

/**
 * The colours screens draw with, named by job and read from the Material scheme in use. Kept while
 * the screens move to the scheme's roles one by one; each one names the role it stands for.
 */
object Colors {
    private var scheme by mutableStateOf(androidx.compose.material3.lightColorScheme())
    private var dark by mutableStateOf(false)
    private var pure by mutableStateOf(false)

    /** The page: Harmonic's page background, a step below the cards in the day. */
    val bg: Color get() = if (dark) scheme.surface else scheme.surfaceContainer

    /** A card or a row: the surface that holds content. */
    val surface: Color get() = if (dark) scheme.surfaceContainer else scheme.surfaceBright

    /** A control over content: the dock, the pane bar, a floating toolbar. */
    val raised: Color get() = scheme.surfaceContainerHigh

    /**
     * The pane's paper: neutral, never tinted by the scheme - a program's colours were chosen for
     * white or black - and black with pure black on.
     */
    val terminal: Color get() = if (!dark) Color.White else if (pure) Color.Black else Color(0xFF0E0E10)

    /** A key on the dock, an inactive plate. */
    val key: Color get() = scheme.surfaceContainerHighest

    /** A latched key, a selected plate. */
    val keyOn: Color get() = scheme.secondaryContainer

    val line: Color get() = scheme.outlineVariant
    val edge: Color get() = scheme.outlineVariant.copy(alpha = 0.5f)
    val shadow: Color get() = if (dark) Color.Transparent else Color(0x33000000)
    val ink: Color get() = scheme.onSurface
    val muted: Color get() = scheme.onSurfaceVariant
    val accent: Color get() = scheme.primary
    val onAccent: Color get() = scheme.onPrimary

    /** Running, online, joined: green, pulled towards the scheme so it belongs to it. */
    val ok: Color get() = (if (dark) Color(0xFF6DD58C) else Color(0xFF1E7A3E)).harmonize(scheme.primary, matchSaturation = false)

    /** Waiting on a person. */
    val amber: Color get() = (if (dark) Color(0xFFF2C14E) else Color(0xFF8A5A00)).harmonize(scheme.primary, matchSaturation = false)

    val danger: Color get() = scheme.error

    /** The tile behind a terminal glyph: dark in both appearances, as a terminal is. */
    val badge: Color get() = if (dark) scheme.surfaceContainerHighest else scheme.inverseSurface
    val badgeInk: Color get() = if (dark) Color(0xFF6DD58C) else Color(0xFF7BE29A)

    /** The light plate behind a program's mark. */
    val tile: Color get() = scheme.surfaceContainerHigh
    val tileInk: Color get() = scheme.onSurface

    val scrim: Color get() = scheme.scrim.copy(alpha = 0.32f)
    val clear: Color = Color(0x00000000)
    val isDark: Boolean get() = dark

    internal fun use(next: ColorScheme, dark: Boolean, pure: Boolean) {
        if (scheme != next) scheme = next
        if (this.dark != dark) this.dark = dark
        if (this.pure != pure) this.pure = pure
    }
}

/** The Material type scale, every role - the emphasized ones too - set in [family]. */
private fun typography(family: FontFamily?): Typography {
    val base = Typography()
    if (family == null) return base
    fun TextStyle.f() = copy(fontFamily = family)
    return Typography(
        base.displayLarge.f(), base.displayMedium.f(), base.displaySmall.f(),
        base.headlineLarge.f(), base.headlineMedium.f(), base.headlineSmall.f(),
        base.titleLarge.f(), base.titleMedium.f(), base.titleSmall.f(),
        base.bodyLarge.f(), base.bodyMedium.f(), base.bodySmall.f(),
        base.labelLarge.f(), base.labelMedium.f(), base.labelSmall.f(),
        base.displayLargeEmphasized.f(), base.displayMediumEmphasized.f(), base.displaySmallEmphasized.f(),
        base.headlineLargeEmphasized.f(), base.headlineMediumEmphasized.f(), base.headlineSmallEmphasized.f(),
        base.titleLargeEmphasized.f(), base.titleMediumEmphasized.f(), base.titleSmallEmphasized.f(),
        base.bodyLargeEmphasized.f(), base.bodyMediumEmphasized.f(), base.bodySmallEmphasized.f(),
        base.labelLargeEmphasized.f(), base.labelMediumEmphasized.f(), base.labelSmallEmphasized.f(),
    )
}

/**
 * Material 3 Expressive, in the appearance the person chose: the scheme, Google Sans Flex, the
 * expressive motion scheme. The system bars follow the scheme's lightness.
 */
@Composable
fun RexTheme(appearance: Appearance = Appearance(), content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val target = remember(appearance, dark) { colorSchemeFor(context, appearance, dark) }
    val scheme = target.animated()
    val font = rememberAppFont(appearance.appFont)
    val type = remember(font) { typography(font) }
    val view = LocalView.current
    Colors.use(scheme, dark, dark && appearance.pureBlack)
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialExpressiveTheme(colorScheme = scheme, motionScheme = MotionScheme.expressive(), typography = type, content = content)
}

/** A change of scheme crossfades over a quarter second, as Harmonic's does, instead of snapping. */
@Composable
private fun ColorScheme.animated(): ColorScheme {
    @Composable fun Color.a() = animateColorAsState(this, tween(250), label = "scheme").value
    return copy(
        primary = primary.a(), onPrimary = onPrimary.a(), primaryContainer = primaryContainer.a(), onPrimaryContainer = onPrimaryContainer.a(),
        secondary = secondary.a(), onSecondary = onSecondary.a(), secondaryContainer = secondaryContainer.a(), onSecondaryContainer = onSecondaryContainer.a(),
        tertiary = tertiary.a(), onTertiary = onTertiary.a(), tertiaryContainer = tertiaryContainer.a(), onTertiaryContainer = onTertiaryContainer.a(),
        background = background.a(), onBackground = onBackground.a(), surface = surface.a(), onSurface = onSurface.a(),
        surfaceVariant = surfaceVariant.a(), onSurfaceVariant = onSurfaceVariant.a(), surfaceTint = surfaceTint.a(),
        inverseSurface = inverseSurface.a(), inverseOnSurface = inverseOnSurface.a(), inversePrimary = inversePrimary.a(),
        error = error.a(), onError = onError.a(), errorContainer = errorContainer.a(), onErrorContainer = onErrorContainer.a(),
        outline = outline.a(), outlineVariant = outlineVariant.a(), scrim = scrim.a(),
        surfaceBright = surfaceBright.a(), surfaceDim = surfaceDim.a(), surfaceContainer = surfaceContainer.a(),
        surfaceContainerHigh = surfaceContainerHigh.a(), surfaceContainerHighest = surfaceContainerHighest.a(),
        surfaceContainerLow = surfaceContainerLow.a(), surfaceContainerLowest = surfaceContainerLowest.a(),
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** The spacing scale, in the steps the screens actually use. */
object Space {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 20.dp
    val xxl = 28.dp
}

object Radius {
    val chip = 12.dp
    val card = 16.dp
    val dock = 24.dp
    val sheet = 24.dp
}

/**
 * Five sizes, five jobs: an empty screen's title, a sheet or dialog title, a row's title, a group
 * header, and a label. Nothing else is allowed, which is what keeps the app from collecting a dozen
 * near-identical font sizes. They are the platform's own steps (headline, title, body, title-small,
 * body-small) rather than a second scale beside it.
 */
object TextStyles {
    val display = 28.sp
    val title = 20.sp
    val body = 16.sp
    val subhead = 14.sp
    val label = 13.sp
}

/** Line height for multi-line labels at [TextStyles.label], such as a dialog's hint. */
val labelLeading: TextUnit = 18.sp

/**
 * The terminal's own paper and ink, for the appearance in use: white paper in the day, the page at
 * night. Every palette entry stays the program's; only the defaults a program never sets bend.
 */
internal fun terminalTheme(): TerminalTheme = TerminalTheme(
    foreground = Colors.ink.toArgb(),
    background = Colors.terminal.toArgb(),
    cursor = Colors.accent.toArgb(),
    selection = Colors.line.toArgb(),
)
