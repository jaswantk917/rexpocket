package me.leaver.rexpocket.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.window.core.layout.WindowSizeClass
import kotlinx.serialization.Serializable

/** The host list. */
@Serializable
internal data object HostsKey : NavKey

/** The open session's terminal; the model knows which session that is. */
@Serializable
internal data object TerminalKey : NavKey

@Serializable
internal data object SettingsKey : NavKey

/**
 * When the list and the terminal stand side by side: an expanded window that is not short. A
 * phone on its side is wide enough by class but has no height for two panes, and a terminal
 * narrower than a medium window is worse than one pane at a time. No gap between the panes: a
 * hairline divides them, as before.
 */
@Composable
internal fun rememberPaneDirective(): PaneScaffoldDirective {
    val info = currentWindowAdaptiveInfoV2()
    return remember(info) {
        val base = calculatePaneScaffoldDirective(info)
        val roomy = info.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) &&
            info.windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
        base.copy(
            maxHorizontalPartitions = if (roomy) 2 else 1,
            horizontalPartitionSpacerSize = 0.dp,
        )
    }
}

/** How far a screen slides as it comes or goes: Android's own activity motion, as Harmonic has it. */
internal val NAV_SLIDE = 96.dp

private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

internal fun <T : Any> AnimatedContentTransitionScope<Scene<T>>.navForward(slide: Int): ContentTransform =
    (slideInHorizontally(tween(450, easing = Emphasized)) { slide } + fadeIn(tween(83, delayMillis = 50))) togetherWith
        (slideOutHorizontally(tween(450, easing = Emphasized)) { -slide } + fadeOut(tween(83, delayMillis = 35)))

internal fun <T : Any> AnimatedContentTransitionScope<Scene<T>>.navBack(slide: Int): ContentTransform =
    (slideInHorizontally(tween(450, easing = Emphasized)) { -slide } + fadeIn(tween(83, delayMillis = 50))) togetherWith
        (slideOutHorizontally(tween(450, easing = Emphasized)) { slide } + fadeOut(tween(83, delayMillis = 35)))
