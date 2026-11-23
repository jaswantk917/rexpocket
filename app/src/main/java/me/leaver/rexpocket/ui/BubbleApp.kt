package me.leaver.rexpocket.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleStartEffect
import me.leaver.rexpocket.core.Phase

/** The terminal alone, for a bubble: no list to go back to, and [onGone] once its session ends. */
@Composable
internal fun BubbleApp(model: RexViewModel, onGone: () -> Unit) {
    val appearance by model.appearance.collectAsState()
    val pocket by model.pocket.collectAsState()
    val font by model.fontScale.collectAsState()
    val typeface by model.terminalTypeface.collectAsState()
    val upload by model.upload.collectAsState()
    val keyBar by model.keyBar.collectAsState()
    val keyBarShift by model.keyBarShift.collectAsState()
    val keyBarAlt by model.keyBarAlt.collectAsState()
    var keyBarChoice by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var opened by remember { mutableStateOf(false) }
    LifecycleStartEffect(model) {
        model.setForeground(true)
        onStopOrDispose { model.setForeground(false) }
    }
    LaunchedEffect(pocket.activeSessionId, pocket.phase) {
        if (pocket.activeSessionId != null) opened = true
        else if (opened && pocket.phase == Phase.Live) onGone()
    }
    RexTheme(appearance) {
        TermScreen(
            embedded = true,
            keyBarWithKeyboard = keyBar,
            keyBarChoice = keyBarChoice,
            onKeyBarChoice = { keyBarChoice = it },
            keyBarShift = keyBarShift,
            keyBarAlt = keyBarAlt,
            state = pocket,
            fontScale = font,
            terminalTypeface = typeface,
            onFont = model::setFont,
            onBack = {},
            onOpenSession = model::openSession,
            watchPane = model::watchPane,
            onRetry = model::retryNow,
            onDismissNotice = model::dismissNotice,
            onAct = model::act,
            upload = upload,
            onAttach = model::attachUpload,
            onCancelUpload = model::cancelUpload,
            onDismissUpload = model::dismissUpload,
            onRetryUpload = model::retryUpload,
        )
    }
}
