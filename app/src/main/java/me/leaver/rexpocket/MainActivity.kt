package me.leaver.rexpocket

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.KeyboardShortcutGroup
import android.view.KeyboardShortcutInfo
import android.view.Menu
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import me.leaver.rexpocket.alerts.Alerts
import me.leaver.rexpocket.diagnostics.Diagnostics
import me.leaver.rexpocket.ui.RexPocketApp
import me.leaver.rexpocket.ui.RexViewModel

class MainActivity : ComponentActivity() {
    private val model: RexViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // The journal that holds the last few hours, a recording the last process left running (a
        // bug that kills the app is the one worth recording), and the crash handler that writes a
        // failure into it before the app goes.
        Diagnostics.install(this)
        // A tapped alert opens its pane; a recreated activity has already acted on its intent.
        if (savedInstanceState == null) Alerts.consume(intent)?.let(model::openAlert)
        setContent { RexPocketApp(model) }
    }

    // The system's keyboard shortcut list (Meta+/) names what the terminal screen handles; the keys
    // here must match terminalShortcut.
    override fun onProvideKeyboardShortcuts(data: MutableList<KeyboardShortcutGroup>, menu: Menu?, deviceId: Int) {
        super.onProvideKeyboardShortcuts(data, menu, deviceId)
        val ctrl = KeyEvent.META_CTRL_ON
        val shift = KeyEvent.META_SHIFT_ON
        fun item(label: String, key: Int, modifiers: Int) = KeyboardShortcutInfo(label, key, modifiers)
        val items = mutableListOf(
            item(getString(R.string.shortcut_next_tab), KeyEvent.KEYCODE_TAB, ctrl),
            item(getString(R.string.shortcut_previous_tab), KeyEvent.KEYCODE_TAB, ctrl or shift),
            item(getString(R.string.menu_new_tab), KeyEvent.KEYCODE_T, ctrl or shift),
            item(getString(R.string.key_paste), KeyEvent.KEYCODE_V, ctrl or shift),
            item(getString(R.string.shortcut_bigger), KeyEvent.KEYCODE_EQUALS, ctrl),
            item(getString(R.string.shortcut_smaller), KeyEvent.KEYCODE_MINUS, ctrl),
            item(getString(R.string.shortcut_default_size), KeyEvent.KEYCODE_0, ctrl),
        )
        for (n in 1..9) items += item(getString(R.string.tab_number, n), KeyEvent.KEYCODE_1 + n - 1, ctrl)
        data += KeyboardShortcutGroup(getString(R.string.shortcut_group), items)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Alerts.consume(intent)?.let(model::openAlert)
    }

    // Leaving the app is the usual trigger for a tunnel that has to find its way back, so the
    // moment it happens belongs in the diagnostics next to the node's own log.
    override fun onStart() {
        super.onStart()
        Diagnostics.event(LIFECYCLE_TAG, "foreground")
    }

    override fun onStop() {
        Diagnostics.event(LIFECYCLE_TAG, "background")
        super.onStop()
    }
}

private const val LIFECYCLE_TAG = "RexPocketLife"
