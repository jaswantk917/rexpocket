package me.leaver.rexpocket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import me.leaver.rexpocket.alerts.Alerts
import me.leaver.rexpocket.ui.BubbleApp
import me.leaver.rexpocket.ui.RexViewModel

/** One pane in an Android bubble, on a connection of its own. */
class BubbleActivity : ComponentActivity() {
    private val model: RexViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = Alerts.alertIn(intent)?.target
        if (target == null) {
            finish()
            return
        }
        enableEdgeToEdge()
        if (savedInstanceState == null) model.openBubble(target)
        setContent {
            BubbleApp(model) {
                Alerts.closeBubble(this, target.blockId)
                finish()
            }
        }
    }
}
