package me.leaver.rexpocket.alerts

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.content.pm.PackageManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.LocusIdCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import kotlinx.coroutines.flow.MutableStateFlow
import me.leaver.rexpocket.BubbleActivity
import me.leaver.rexpocket.MainActivity
import me.leaver.rexpocket.R
import me.leaver.rexpocket.core.AlertTokens
import me.leaver.rexpocket.core.ProgramRecord
import me.leaver.rexpocket.ui.hueSeed

// Background alerts: a quiet notification that keeps the app connected while it is away, and a
// loud one when a program in a pane needs the person.

/** The pane an alert opens. */
data class AlertTarget(val hostId: String, val sessionId: String, val blockId: String)

/** One alert, already in the person's words. */
data class Alert(val target: AlertTarget, val title: String, val text: String, val host: String)

/** What the ongoing notification says while the app is away. */
data class LiveStatus(val working: Int, val waiting: Int, val lead: String?)

/** Null when nothing is running; a pane that waits for the person leads over one that works. */
internal fun liveStatusOf(status: Map<String, ProgramRecord>, nameOf: (String) -> String?): LiveStatus? {
    val waiting = status.filterValues { it.state == "blocked" }.keys.sorted()
    val working = status.filterValues { it.state == "working" }.keys.sorted()
    if (waiting.isEmpty() && working.isEmpty()) return null
    return LiveStatus(working.size, waiting.size, (waiting + working).firstNotNullOfOrNull(nameOf))
}

object Alerts {
    /**
     * Whether alerts are on, for the whole process: the watch notification's Stop turns them off
     * from outside any screen, and the screens follow.
     */
    val enabled = MutableStateFlow(false)

    private val tokens = AlertTokens<AlertTarget>()

    /** Notifications are on for the app, and on Android 13 and later the person has allowed them. */
    fun allowed(context: Context): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Starts watching [host]; called while the app is on screen, which is when Android allows it. */
    fun watch(context: Context, host: String) {
        ContextCompat.startForegroundService(context, Intent(context, WatchService::class.java).putExtra(EXTRA_HOST, host))
    }

    fun unwatch(context: Context) {
        context.stopService(Intent(context, WatchService::class.java))
    }

    fun raise(context: Context, alert: Alert) {
        // Notifications can be turned off in system settings while alerts are on; then there is no one to tell.
        if (!allowed(context)) return
        channels(context)
        // A pane already in a bubble says it there, with the bubble's dot, not in a second notification.
        val shown = shownBubble(context, alert.target.blockId)
        if (shown?.isBubbling == true) {
            bubble(context, alert, expand = false)
            return
        }
        if (shown != null) NotificationManagerCompat.from(context).cancel(alert.target.blockId, BUBBLE_ID)
        val builder = conversation(context, alert).setAutoCancel(true)
        if (bubblesAllowed(context)) {
            val action = Intent(context, BubbleReceiver::class.java).putTarget(alert)
            val pending = PendingIntent.getBroadcast(
                context, alert.target.blockId.hashCode(), action, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            builder.addAction(R.drawable.ic_bubble, context.getString(R.string.menu_bubble), pending)
        }
        post(context, alert.target.blockId, ATTENTION_ID, builder.build())
    }

    /** True on Android 11 and later, where an app can put a conversation in a bubble. */
    val bubblesSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    /** Bubbles are on for the phone and for every conversation of this app. */
    fun bubblesAllowed(context: Context): Boolean {
        if (!bubblesSupported) return false
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.areBubblesEnabled() && manager.bubblePreference == NotificationManager.BUBBLE_PREFERENCE_ALL
        } else {
            @Suppress("DEPRECATION")
            manager.areBubblesAllowed()
        }
    }

    fun openBubbleSettings(context: Context) {
        Toast.makeText(context, R.string.bubble_allow, Toast.LENGTH_LONG).show()
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_BUBBLE_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /** Posts [alert]'s pane as a bubble; [expand] opens it, which Android allows only from the screen. */
    fun bubble(context: Context, alert: Alert, expand: Boolean) {
        if (!allowed(context) || !bubblesSupported) return
        channels(context)
        val open = Intent(context, BubbleActivity::class.java)
            .setData(Uri.Builder().scheme(BUBBLE_SCHEME).authority("pane").appendPath(alert.target.blockId).build())
            .putTarget(alert)
        // Android fills in the bubble's task details, so this one intent has to stay mutable.
        val pending = PendingIntent.getActivity(
            context, alert.target.blockId.hashCode(), open, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val metadata = NotificationCompat.BubbleMetadata.Builder(pending, paneIcon(context, alert.target.sessionId))
            .setDesiredHeight(BUBBLE_HEIGHT_DP)
            .setAutoExpandBubble(expand)
            .setSuppressNotification(expand)
            .apply { if (expand) setDeleteIntent(bubbleGone(context, alert.target.blockId)) }
            .build()
        val notification = conversation(context, alert)
            .setBubbleMetadata(metadata)
            .setSilent(expand)
            .build()
        post(context, alert.target.blockId, BUBBLE_ID, notification)
    }

    fun closeBubble(context: Context, blockId: String) {
        NotificationManagerCompat.from(context).cancel(blockId, BUBBLE_ID)
    }

    private fun shownBubble(context: Context, blockId: String): Notification? =
        context.getSystemService(NotificationManager::class.java)?.activeNotifications
            ?.firstOrNull { it.tag == blockId && it.id == BUBBLE_ID }?.notification

    /** Android keeps a dragged-away bubble's notification; one with no question left has to go with it. */
    private fun bubbleGone(context: Context, blockId: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        blockId.hashCode(),
        Intent(context, BubbleReceiver::class.java).setAction(ACTION_BUBBLE_GONE).putExtra(EXTRA_BLOCK, blockId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    internal fun closeGoneBubble(context: Context, intent: Intent): Boolean {
        if (intent.action != ACTION_BUBBLE_GONE) return false
        intent.getStringExtra(EXTRA_BLOCK)?.let { closeBubble(context, it) }
        return true
    }

    private val Notification.isBubbling: Boolean get() = flags and Notification.FLAG_BUBBLE != 0

    /** The bubble stays for the person to reopen; its "waiting" row leaves the shade. */
    private fun quietBubble(context: Context, blockId: String, shown: Notification) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val metadata = shown.bubbleMetadata ?: return
        val intent = metadata.intent ?: return
        val icon = metadata.icon ?: return
        val quiet = Notification.BubbleMetadata.Builder(intent, icon)
            .setDesiredHeight(metadata.desiredHeight)
            .setSuppressNotification(true)
            .setDeleteIntent(bubbleGone(context, blockId))
            .build()
        val notification = Notification.Builder.recoverBuilder(context, shown)
            .setBubbleMetadata(quiet)
            .setOnlyAlertOnce(true)
            .build()
        post(context, blockId, BUBBLE_ID, notification)
    }

    /** The pane as an Android conversation: its own name and colour, under Conversations in the shade. */
    private fun conversation(context: Context, alert: Alert): NotificationCompat.Builder {
        val target = alert.target
        val shortcutId = "pane:${target.hostId}:${target.blockId}"
        val icon = paneIcon(context, target.sessionId)
        val pane = Person.Builder().setKey(shortcutId).setName(alert.title).setIcon(icon).setImportant(true).build()
        ShortcutManagerCompat.pushDynamicShortcut(
            context,
            ShortcutInfoCompat.Builder(context, shortcutId)
                .setLongLived(true)
                .setShortLabel(alert.title)
                .setIcon(icon)
                .setPerson(pane)
                .setLocusId(LocusIdCompat(shortcutId))
                // A shortcut outlives the process and its alert tokens, so it opens the app's list.
                .setIntent(Intent(context, MainActivity::class.java).setAction(Intent.ACTION_VIEW))
                .build(),
        )
        val token = tokens.issue(target)
        val open = Intent(context, MainActivity::class.java)
            .setData(Uri.Builder().scheme(SCHEME).authority("open").appendPath(token).build())
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val tap = PendingIntent.getActivity(context, token.hashCode(), open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val you = Person.Builder().setName(context.getString(R.string.alerts_you)).build()
        return NotificationCompat.Builder(context, CHANNEL_ATTENTION)
            .setSmallIcon(R.drawable.ic_notifications)
            .setStyle(NotificationCompat.MessagingStyle(you).addMessage(alert.text, System.currentTimeMillis(), pane))
            .setSubText(alert.host)
            .setShortcutId(shortcutId)
            .setLocusId(LocusIdCompat(shortcutId))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(tap)
    }

    /** The session's own hue behind the terminal mark, so a bubble matches its card in the list. */
    private fun paneIcon(context: Context, sessionId: String): IconCompat {
        val px = (ADAPTIVE_ICON_DP * context.resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(hueSeed(sessionId).toInt())
        ContextCompat.getDrawable(context, R.drawable.ic_terminal)?.mutate()?.let { glyph ->
            val inset = px * 3 / 10
            glyph.setTint(Color.WHITE)
            glyph.setBounds(inset, inset, px - inset, px - inset)
            glyph.draw(canvas)
        }
        return IconCompat.createWithAdaptiveBitmap(bitmap)
    }

    private fun post(context: Context, tag: String, id: Int, notification: Notification) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED ||
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            NotificationManagerCompat.from(context).notify(tag, id, notification)
        }
    }

    private fun Intent.putTarget(alert: Alert): Intent = putExtra(EXTRA_HOST_ID, alert.target.hostId)
        .putExtra(EXTRA_SESSION, alert.target.sessionId)
        .putExtra(EXTRA_BLOCK, alert.target.blockId)
        .putExtra(EXTRA_TITLE, alert.title)
        .putExtra(EXTRA_TEXT, alert.text)
        .putExtra(EXTRA_HOST, alert.host)

    /** The alert a bubble's intent carries; these intents are private to the app. */
    fun alertIn(intent: Intent?): Alert? {
        val hostId = intent?.getStringExtra(EXTRA_HOST_ID) ?: return null
        val session = intent.getStringExtra(EXTRA_SESSION) ?: return null
        val block = intent.getStringExtra(EXTRA_BLOCK) ?: return null
        return Alert(
            AlertTarget(hostId, session, block),
            intent.getStringExtra(EXTRA_TITLE).orEmpty(),
            intent.getStringExtra(EXTRA_TEXT).orEmpty(),
            intent.getStringExtra(EXTRA_HOST).orEmpty(),
        )
    }

    @Volatile internal var watching = false
    private var shownLive: Pair<String, LiveStatus?>? = null

    /** Updates the ongoing notification; on Android 16 a running pane shows as a live update. */
    @Synchronized
    fun live(context: Context, host: String, status: LiveStatus?) {
        if (!watching || !allowed(context) || shownLive == (host to status)) return
        shownLive = host to status
        channels(context)
        val notification = watchNotification(context, host, status)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED ||
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            NotificationManagerCompat.from(context).notify(WATCH_ID, notification)
        }
    }

    @Synchronized
    internal fun watchStopped() {
        watching = false
        shownLive = null
    }

    internal fun watchNotification(context: Context, host: String, status: LiveStatus?): Notification {
        val stop = PendingIntent.getService(
            context, 0, Intent(context, WatchService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE,
        )
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, if (status != null) CHANNEL_LIVE else CHANNEL_WATCH)
            .setSmallIcon(R.drawable.ic_notifications)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(0, context.getString(R.string.alerts_stop), stop)
        if (status == null) {
            return builder
                .setContentTitle(context.getString(R.string.alerts_title))
                .setContentText(context.getString(R.string.alerts_watching, host))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build()
        }
        val text = listOfNotNull(status.lead, host).joinToString(" · ")
        val chip = if (status.waiting > 0) {
            context.getString(R.string.live_chip_waiting, status.waiting)
        } else {
            context.getString(R.string.live_chip_working, status.working)
        }
        return builder
            .setContentTitle(context.getString(R.string.home_activity, status.working, status.waiting))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText(chip)
            .build()
    }

    /** The pane no longer waits, or the person went to it: its alert has said what it had to. */
    fun settle(context: Context, blockId: String) {
        val manager = NotificationManagerCompat.from(context)
        manager.cancel(blockId, ATTENTION_ID)
        val shown = shownBubble(context, blockId) ?: return
        if (shown.isBubbling) quietBubble(context, blockId, shown) else manager.cancel(blockId, BUBBLE_ID)
    }

    /** The pane an alert's intent names, once; null for anything this process did not issue. */
    fun consume(intent: Intent?): AlertTarget? {
        val uri = intent?.data ?: return null
        if (uri.scheme != SCHEME || uri.authority != "open" || uri.pathSegments.size != 1) return null
        return tokens.consume(uri.lastPathSegment)
    }

    internal fun channels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_WATCH, context.getString(R.string.alerts_channel_watch), NotificationManager.IMPORTANCE_MIN)
                .apply { description = context.getString(R.string.alerts_channel_watch_body) },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ATTENTION, context.getString(R.string.alerts_channel_attention), NotificationManager.IMPORTANCE_HIGH)
                .apply { description = context.getString(R.string.alerts_channel_attention_body) },
        )
        // Above minimum importance, which Android requires of a live update; still silent.
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_LIVE, context.getString(R.string.alerts_channel_live), NotificationManager.IMPORTANCE_LOW)
                .apply { description = context.getString(R.string.alerts_channel_live_body) },
        )
    }

    internal const val CHANNEL_WATCH = "watch"
    internal const val CHANNEL_ATTENTION = "attention"
    internal const val CHANNEL_LIVE = "live"
    internal const val EXTRA_HOST = "host"
    internal const val ACTION_STOP = "me.leaver.rexpocket.alerts.STOP"
    internal const val WATCH_ID = 1
    private const val ATTENTION_ID = 2
    private const val BUBBLE_ID = 3
    private const val SCHEME = "rexpocket-alert"
    private const val BUBBLE_SCHEME = "rexpocket-bubble"
    private const val ACTION_BUBBLE_GONE = "me.leaver.rexpocket.action.BUBBLE_GONE"
    private const val EXTRA_HOST_ID = "host_id"
    private const val EXTRA_SESSION = "session"
    private const val EXTRA_BLOCK = "block"
    private const val EXTRA_TITLE = "title"
    private const val EXTRA_TEXT = "text"
    private const val BUBBLE_HEIGHT_DP = 640
    private const val ADAPTIVE_ICON_DP = 108
}

/**
 * Holds the app in the foreground while it is away, so its connection - and the tailnet node it
 * runs over - stay up and Rex's status events keep arriving. It watches nothing itself: the
 * connection the screens already have does, and this only keeps it alive. It ends with the task.
 */
class WatchService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == Alerts.ACTION_STOP) {
            Alerts.enabled.value = false
            stopSelf()
            return START_NOT_STICKY
        }
        Alerts.channels(this)
        val host = intent?.getStringExtra(Alerts.EXTRA_HOST).orEmpty()
        Alerts.watching = true
        ServiceCompat.startForeground(this, Alerts.WATCH_ID, Alerts.watchNotification(this, host, null), foregroundType())
        // Killed for memory, the connection is gone with the process; the app restarts it, not this.
        return START_NOT_STICKY
    }

    // Keeping a remote terminal connection open fits none of the platform's named types; the app is
    // installed from its releases, not a store, so the special-use type says what it is instead.
    private fun foregroundType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0

    // Android ends a stopWithTask service with any of the app's tasks, and a bubble is a task too.
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (rootIntent?.component?.className != BubbleActivity::class.java.name) stopSelf()
    }

    override fun onDestroy() {
        Alerts.watchStopped()
        super.onDestroy()
    }
}

/** The Open as bubble action on an alert: the pane moves from the shade into a bubble. */
class BubbleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Alerts.closeGoneBubble(context, intent)) return
        val alert = Alerts.alertIn(intent) ?: return
        Alerts.settle(context, alert.target.blockId)
        Alerts.bubble(context, alert, expand = false)
    }
}
