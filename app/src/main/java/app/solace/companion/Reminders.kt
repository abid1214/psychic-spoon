package app.solace.companion

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import app.solace.companion.data.Session
import app.solace.companion.data.Store
import app.solace.companion.ui.formatTime

object Reminders {
    const val CHANNEL = "session_reminders"
    private const val ACTION = "app.solace.companion.REMIND"

    fun createChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                context.getString(R.string.channel_reminders),
                NotificationManager.IMPORTANCE_HIGH,
            )
        )
    }

    private fun pending(context: Context, sessionId: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION)
            .setData(Uri.parse("solace://session/$sessionId"))
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Cancel alarms for sessions that changed or went away, then set alarms for the current ones. */
    fun sync(context: Context, old: List<Session>, new: List<Session>) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val now = System.currentTimeMillis()
        val current = new.associateBy { it.id }
        old.filter { current[it.id] != it }.forEach { am.cancel(pending(context, it.id)) }
        new.forEach { s ->
            if (s.reminderMin < 0) return@forEach
            val at = s.startMillis - s.reminderMin * 60_000L
            if (at > now) {
                // Inexact on purpose: needs no special permission and is close enough for a reminder.
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(context, s.id))
            }
        }
    }

    fun notify(context: Context, sessionId: String) {
        val data = Store.data
        val session = data.sessions.firstOrNull { it.id == sessionId } ?: return
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        val who = data.provider(session.providerId)?.name?.takeIf { it.isNotBlank() }
        val title = if (who != null) "Session with $who" else "Therapy session"
        val where = if (session.telehealth) "Video session" else session.location
        val text = listOf("Starts at ${formatTime(session.startMillis)}", where)
            .filter { it.isNotBlank() }.joinToString(" · ")
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            // Keep details off the lock screen.
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(sessionId.hashCode(), n)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Store.init(context)
        Reminders.createChannel(context)
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Alarms do not survive a reboot.
            Reminders.sync(context, emptyList(), Store.data.sessions)
        } else {
            intent.data?.lastPathSegment?.let { Reminders.notify(context, it) }
        }
    }
}
