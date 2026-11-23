package me.leaver.rexpocket.ui

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.LocalNavAnimatedContentScope

/** The card a terminal grows out of and shrinks back into. */
internal data class MorphSource(val key: String, val shape: CornerBasedShape)

internal fun shelfMorphKey(sessionId: String, windowId: String) = "shelf/$sessionId/$windowId"
internal fun tabMorphKey(sessionId: String, windowId: String) = "tab/$sessionId/$windowId"
internal fun sessionMorphKey(sessionId: String) = "session/$sessionId"
internal const val NEW_SESSION_MORPH = "new-session"

/** How long a card takes to become the terminal; the source may change once it is over. */
internal const val MORPH_MS = 450

@Stable
internal class PaneMorph(val layout: SharedTransitionScope) {
    var source by mutableStateOf<MorphSource?>(null)
        private set

    fun start(key: String, shape: CornerBasedShape) {
        source = MorphSource(key, shape)
    }

    fun clear() {
        source = null
    }

    /** The terminal moved to another tab or session: going back lands on that one's row. */
    fun follow(sessionId: String?, windowId: String?) {
        val next = followedMorphKey(source?.key, sessionId, windowId) ?: return
        source = MorphSource(next, RoundedCornerShape(20.dp))
    }
}

/** The row to land on for the tab on screen; null while the current source already stands for it. */
internal fun followedMorphKey(current: String?, sessionId: String?, windowId: String?): String? {
    if (sessionId == null || windowId == null) return null
    val same = current == tabMorphKey(sessionId, windowId) ||
        current == shelfMorphKey(sessionId, windowId) ||
        current == sessionMorphKey(sessionId)
    return if (same) null else tabMorphKey(sessionId, windowId)
}

/** Null beside the list: both panes stay on screen there, so nothing has to grow. */
internal val LocalPaneMorph = staticCompositionLocalOf<PaneMorph?> { null }

/** The screens hold still while the shared card carries the change between them. */
internal fun <T : Any> AnimatedContentTransitionScope<Scene<T>>.morphHold(): ContentTransform =
    EnterTransition.None togetherWith ExitTransition.KeepUntilTransitionsFinished

/** Runs [onLanded] once this screen's transition is over; Android's animation speed stretches it, so it can't be timed. */
@Composable
internal fun AfterMorphLands(vararg keys: Any?, onLanded: () -> Unit) {
    val transition = LocalNavAnimatedContentScope.current.transition
    val landed = !transition.isRunning && transition.currentState == transition.targetState
    val latest by rememberUpdatedState(onLanded)
    LaunchedEffect(landed, *keys) { if (landed) latest() }
}

/** Marks the card the terminal grows from; only the tapped one is shared. */
@Composable
internal fun Modifier.morphCard(key: String, shape: CornerBasedShape): Modifier {
    val morph = LocalPaneMorph.current ?: return this
    if (morph.source?.key != key) return this
    return morphBounds(morph, key, shape, restingRoundness = 1f, inFront = false)
}

/** Marks the terminal page, the end of the morph. */
@Composable
internal fun Modifier.morphPage(): Modifier {
    val morph = LocalPaneMorph.current ?: return this
    val source = morph.source ?: return this
    return morphBounds(morph, source.key, source.shape, restingRoundness = 0f, inFront = true)
}

@Composable
private fun Modifier.morphBounds(
    morph: PaneMorph,
    key: String,
    shape: CornerBasedShape,
    restingRoundness: Float,
    inFront: Boolean,
): Modifier = with(morph.layout) {
    val scope = LocalNavAnimatedContentScope.current
    val roundness by scope.transition.animateFloat(
        transitionSpec = { tween(MORPH_MS, easing = MorphEasing) },
        label = "morph-corners",
    ) { state -> if (state == EnterExitState.Visible) restingRoundness else 1f - restingRoundness }
    val clip = remember(shape) { MorphClip(shape) { roundness } }
    sharedBounds(
        sharedContentState = rememberSharedContentState(key),
        animatedVisibilityScope = scope,
        enter = if (inFront) fadeIn(tween(180, delayMillis = 30)) else EnterTransition.None,
        exit = if (inFront) fadeOut(tween(200, delayMillis = 170)) else fadeOut(tween(1, delayMillis = MORPH_MS)),
        boundsTransform = BoundsTransform { _, _ -> tween(MORPH_MS, easing = MorphEasing) },
        // Scaled, never remeasured: a terminal laid out at each size on the way would resize the
        // program running in it.
        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.FillWidth, Alignment.TopCenter),
        clipInOverlayDuringTransition = OverlayClip(clip),
        zIndexInOverlay = if (inFront) 1f else 0f,
    )
}

private val MorphEasing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** The card's corners at full [roundness], square at none. */
private class MorphClip(private val shape: CornerBasedShape, private val roundness: () -> Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val k = roundness()
        fun radius(corner: androidx.compose.foundation.shape.CornerSize) = CornerRadius(corner.toPx(size, density) * k)
        return Outline.Rounded(
            RoundRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height,
                topLeftCornerRadius = radius(shape.topStart),
                topRightCornerRadius = radius(shape.topEnd),
                bottomRightCornerRadius = radius(shape.bottomEnd),
                bottomLeftCornerRadius = radius(shape.bottomStart),
            ),
        )
    }
}
