package me.leaver.rexpocket.ui

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * A surface that answers a press the expressive way: it gives a little under the finger (0.96, on
 * the motion scheme's fast spatial spring), ripples inside its own shape, and shows the hand to a
 * pointer. Buttons have this built in; rows and cards take it from here.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Modifier.pressable(
    shape: Shape,
    enabled: Boolean = true,
    role: Role = Role.Button,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && enabled) 0.96f else 1f,
        MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "press",
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clip(shape)
        .combinedClickable(source, ripple(), enabled = enabled, role = role, onLongClick = onLongClick, onClick = onClick)
        .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
}

/**
 * The first screenful rises into place, staggered by [index] (45 ms apart), once per item: a row
 * that scrolls back in does not arrive again, and rows past the ninth are simply there, so a fling
 * never shows rows sliding.
 */
@Composable
internal fun Modifier.reveal(index: Int): Modifier {
    // Previews and screenshots show the resting frame.
    if (index > 8 || LocalInspectionMode.current) return this
    var done by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (done) 1f else 0f) }
    val spec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(Unit) {
        if (done) return@LaunchedEffect
        delay(index * 45L)
        progress.animateTo(1f, spec)
        done = true
    }
    return graphicsLayer {
        alpha = progress.value.coerceIn(0f, 1f)
        translationY = (1f - progress.value) * 28.dp.toPx()
    }
}

/**
 * The app's haptics, kept to the owner's rule: a light tick when something latches or toggles, one
 * firm confirm for the big finished moments a person started (connecting, taking a pane over),
 * nothing for taps, scrolls or what the app does on its own.
 */
internal object Haptics {
    fun toggle(view: View, on: Boolean) {
        view.performHapticFeedback(if (on) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF)
    }

    fun tick(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
    }

    fun confirm(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }
}
