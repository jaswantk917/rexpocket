package me.leaver.rexpocket.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

// What makes Rex look like itself rather than a Material sample: every session has a hue of its
// own, carried from its card in the list to its terminal; big organic shapes sit behind heroes and
// turn slowly; and the shape behind an icon answers a press by becoming another shape.

/** The hues sessions are given: soft, distinct from each other, each a whole tonal family. */
private val HUES = longArrayOf(
    0xFF7C6CF0, // lavender
    0xFF2F8FD8, // sky
    0xFFE0703F, // peach
    0xFF3C8C78, // sage
    0xFFD9487A, // rose
    0xFFB8902E, // sand
)

/** One hue's colours, in the light or dark the phone is in. */
@Immutable
internal data class Tint(
    /** A card's fill. */
    val container: Color,
    /** Text and icons on [container]. */
    val onContainer: Color,
    /** The rows under a card's head: the card's hue, quieter. */
    val surface: Color,
    /** The hue at full strength: a mark, a selection edge. */
    val accent: Color,
    /** A decorative shape behind content on [container]. */
    val ornament: Color,
)

/** The hue a key always gets, the same on every screen and every launch. */
internal fun hueSeed(key: String): Long = HUES[(key.hashCode() and 0x7fffffff) % HUES.size]

/**
 * Worked-out tints, kept for the life of the process: building a hue's tonal family is a few
 * milliseconds, too slow to redo each time a card scrolls back in. Six hues, light and dark, and
 * the page they sit on: a handful of entries.
 */
private val tints = HashMap<Triple<Long, Boolean, Color>, Tint>()

/** The tint for [key]; [hue] picks one of the hues by number instead, for a set that must not repeat. */
@Composable
internal fun rememberTint(key: String, hue: Int? = null): Tint {
    val dark = Colors.isDark
    val page = Colors.surface
    val seed = hue?.let { HUES[it % HUES.size] } ?: hueSeed(key)
    return tints.getOrPut(Triple(seed, dark, page)) {
        val s = dynamicColorScheme(Color(seed), dark, false, style = PaletteStyle.TonalSpot)
        Tint(
            container = s.primaryContainer,
            onContainer = s.onPrimaryContainer,
            surface = lerp(s.primaryContainer, page, if (dark) 0.62f else 0.55f),
            accent = s.primary,
            ornament = s.onPrimaryContainer.copy(alpha = if (dark) 0.10f else 0.07f),
        )
    }
}

/** A Material shape at rest behind content, turned to [angle]: an ornament that costs no frames. */
@Composable
internal fun StillOrnament(shape: RoundedPolygon, color: Color, size: Dp, modifier: Modifier = Modifier, angle: Float = 0f) {
    Box(modifier.size(size).graphicsLayer { rotationZ = angle }.background(color, shape.toShape()))
}

/** A Material shape that turns slowly behind content: a hero's ornament. Still in previews. */
@Composable
internal fun Ornament(shape: RoundedPolygon, color: Color, size: Dp, modifier: Modifier = Modifier, periodMs: Int = 24_000, turn: Float = 0f) {
    val still = LocalInspectionMode.current
    val spin by rememberInfiniteTransition(label = "ornament").animateFloat(
        0f,
        if (still) 0f else 360f,
        infiniteRepeatable(tween(periodMs, easing = LinearEasing)),
        label = "ornament-turn",
    )
    Box(modifier.size(size).graphicsLayer { rotationZ = spin + turn }.background(color, shape.toShape()))
}

/** A shape between two Material shapes, [progress] of the way from the first to the second. */
internal class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = morph.toPath(progress)
        path.transform(android.graphics.Matrix().apply { setScale(size.width, size.height) })
        return Outline.Generic(path.asComposePath())
    }
}

/**
 * An icon's backdrop that answers [active]: it turns into [to] and a quarter of a turn on the motion
 * scheme's spring while the icon stays upright, and back when [active] goes. A press, a toggle,
 * a connection made.
 */
@Composable
internal fun MorphBackdrop(
    active: Boolean,
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    from: RoundedPolygon = MaterialShapes.Cookie9Sided,
    to: RoundedPolygon = MaterialShapes.Sunny,
    content: @Composable BoxScope.() -> Unit,
) {
    val k by animateFloatAsState(if (active) 1f else 0f, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "backdrop")
    val morph = remember(from, to) { Morph(from, to) }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Box(Modifier.matchParentSize().graphicsLayer { rotationZ = k * 90f }.background(color, MorphShape(morph, k)))
        content()
    }
}
