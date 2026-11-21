package com.mindpause.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mindpause.R
import com.mindpause.data.Prefs
import com.mindpause.ui.MainActivity
import java.text.DateFormat
import java.util.Date

object Notifications {
    const val CHANNEL_RUNNING = "running"
    const val CHANNEL_EVENING = "evening"
    const val ID_RUNNING = 1
    const val ID_EVENING = 2

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_RUNNING, "While MindPause runs", NotificationManager.IMPORTANCE_LOW).apply {
                description = "A quiet notification Android requires while MindPause is on."
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_EVENING, "Evening line", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "One line each evening about today's check-ins."
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
        )
    }

    private fun openAppIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    fun running(context: Context, prefs: Prefs): Notification {
        val paused = prefs.isPaused()
        val title: String
        val text: String
        val actionLabel: String
        val action: String
        if (paused) {
            val until = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(prefs.pausedUntil))
            title = "Paused until $until"
            text = "Check-ins resume after that"
            actionLabel = "Resume now"
            action = PauseService.ACTION_RESUME
        } else {
            title = "Quietly with you"
            text = "First check-in after ${prefs.intervalMinutes} minutes of screen time"
            actionLabel = "Pause 1 hour"
            action = PauseService.ACTION_PAUSE
        }
        val actionIntent = PendingIntent.getService(
            context, 1,
            Intent(context, PauseService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, CHANNEL_RUNNING)
            .setSmallIcon(R.drawable.ic_stat_mindpause)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openAppIntent(context))
            .addAction(0, actionLabel, actionIntent)
            .build()
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun postEvening(context: Context, line: String) {
        if (!canPost(context)) return
        val n = NotificationCompat.Builder(context, CHANNEL_EVENING)
            .setSmallIcon(R.drawable.ic_stat_mindpause)
            .setContentTitle("MindPause")
            .setContentText(line)
            .setStyle(NotificationCompat.BigTextStyle().bigText(line))
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(ID_EVENING, n)
        } catch (_: SecurityException) {
        }
    }
}
