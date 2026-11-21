package com.mindpause.summary

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mindpause.domain.DailyLine
import com.mindpause.prefs
import com.mindpause.service.Notifications
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** One inexact alarm a day at about 21:00. No exact-alarm permission needed. */
object EveningScheduler {
    private val TIME: LocalTime = LocalTime.of(21, 0)

    fun schedule(context: Context) {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        var next = LocalDateTime.of(LocalDate.now(zone), TIME)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val at = next.atZone(zone).toInstant().toEpochMilli()

        val am = context.getSystemService(AlarmManager::class.java)
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, 0,
            Intent(context, EveningReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}

class EveningReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.prefs
        if (prefs.eveningLine && prefs.onboardingDone) {
            val today = prefs.today()
            if (today.checkIns > 0) {
                Notifications.postEvening(context, DailyLine.text(today.checkIns, today.stops))
            }
        }
        EveningScheduler.schedule(context)
    }
}
