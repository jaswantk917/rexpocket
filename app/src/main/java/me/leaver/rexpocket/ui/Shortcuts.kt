package me.leaver.rexpocket.ui

import android.view.KeyEvent

/**
 * What a desk keyboard can do on the terminal screen beyond typing. Only chords a terminal program
 * has no use for, the ones Windows Terminal and GNOME Terminal take: Ctrl+C, Ctrl+T and the rest
 * still reach the shell.
 */
internal sealed interface TerminalShortcut {
    data object NextTab : TerminalShortcut
    data object PreviousTab : TerminalShortcut
    /** The tab at this position, counted from zero. */
    data class Tab(val index: Int) : TerminalShortcut
    data object NewTab : TerminalShortcut
    data object Paste : TerminalShortcut
    data object Bigger : TerminalShortcut
    data object Smaller : TerminalShortcut
    data object DefaultSize : TerminalShortcut
}

internal fun terminalShortcut(keyCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean, meta: Boolean): TerminalShortcut? {
    if (!ctrl || alt || meta) return null
    return when {
        keyCode == KeyEvent.KEYCODE_TAB -> if (shift) TerminalShortcut.PreviousTab else TerminalShortcut.NextTab
        shift && keyCode == KeyEvent.KEYCODE_T -> TerminalShortcut.NewTab
        shift && keyCode == KeyEvent.KEYCODE_V -> TerminalShortcut.Paste
        shift -> null
        keyCode in KeyEvent.KEYCODE_1..KeyEvent.KEYCODE_9 -> TerminalShortcut.Tab(keyCode - KeyEvent.KEYCODE_1)
        keyCode == KeyEvent.KEYCODE_EQUALS || keyCode == KeyEvent.KEYCODE_PLUS -> TerminalShortcut.Bigger
        keyCode == KeyEvent.KEYCODE_MINUS -> TerminalShortcut.Smaller
        keyCode == KeyEvent.KEYCODE_0 -> TerminalShortcut.DefaultSize
        else -> null
    }
}

/** One step of the text size from the keyboard, inside the range a pinch allows. */
internal fun stepFontScale(scale: Float, bigger: Boolean): Float =
    ((Math.round(scale * 10) + if (bigger) 1 else -1) / 10f).coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)

internal const val MIN_FONT_SCALE = 0.7f
internal const val MAX_FONT_SCALE = 1.8f
