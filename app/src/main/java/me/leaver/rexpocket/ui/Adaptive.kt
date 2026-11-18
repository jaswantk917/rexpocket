package me.leaver.rexpocket.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** One column in a window too wide for one: the width a line of text and a row still read at. */
internal val COLUMN_MAX = 560.dp

/** The widest a terminal's dock is drawn: one hand's reach, however wide the window is. */
internal val DOCK_MAX = 720.dp

/**
 * A row of controls held to the width two thumbs can reach across and centred, so a terminal's dock
 * does not run the whole width of a desk window. On a phone it is the window, edge to edge.
 */
@Composable
internal fun ReachableWidth(max: Dp = DOCK_MAX, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.widthIn(max = max).fillMaxWidth()) { content() }
    }
}
