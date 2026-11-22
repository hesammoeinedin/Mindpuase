package com.mindpause.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.mindpause.prefs

/**
 * Keeps [PauseService] alive.
 *
 * - When the app is swiped away or the service is killed, a restart is
 *   scheduled a couple of seconds later.
 * - Every 15 minutes a check restarts the service if it isn't running.
 *
 * Android only allows these background restarts when MindPause is exempt from
 * battery optimisation (or, up to Android 14, holds "display over other apps").
 * A phone that force-stops apps cancels the alarms too; auto-start settings on
 * those phones are the only remedy.
 */
object KeepAlive {
    private const val CHECK_INTERVAL_MS = 15 * 60_000L
    const val RESTART_DELAY_MS = 2_000L

    fun schedule(context: Context, delayMs: Long = CHECK_INTERVAL_MS) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.setAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + delayMs,
            pendingIntent(context),
        )
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, 10,
            Intent(context, KeepAliveReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}

class KeepAliveReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.prefs
        if (!prefs.onboardingDone || !prefs.enabled) return
        if (!PauseService.isRunning) ServiceControl.start(context)
        KeepAlive.schedule(context)
    }
}
