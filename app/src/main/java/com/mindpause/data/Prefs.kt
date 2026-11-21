package com.mindpause.data

import android.content.Context
import androidx.core.content.edit
import com.mindpause.domain.EscalationPolicy
import java.time.LocalDate

data class DayCounts(val checkIns: Int, val stops: Int)

/** All app data. Small, on-device, nothing else. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("mindpause", Context.MODE_PRIVATE)

    var intervalMinutes: Int
        get() = EscalationPolicy.clampInterval(sp.getInt(K_INTERVAL, 3))
        set(v) = sp.edit { putInt(K_INTERVAL, EscalationPolicy.clampInterval(v)) }

    var onboardingDone: Boolean
        get() = sp.getBoolean(K_ONBOARDED, false)
        set(v) = sp.edit { putBoolean(K_ONBOARDED, v) }

    var enabled: Boolean
        get() = sp.getBoolean(K_ENABLED, true)
        set(v) = sp.edit { putBoolean(K_ENABLED, v) }

    /** Epoch ms; 0 = not paused. */
    var pausedUntil: Long
        get() = sp.getLong(K_PAUSED_UNTIL, 0L)
        set(v) = sp.edit { putLong(K_PAUSED_UNTIL, v) }

    var eveningLine: Boolean
        get() = sp.getBoolean(K_EVENING, true)
        set(v) = sp.edit { putBoolean(K_EVENING, v) }

    fun isPaused(now: Long = System.currentTimeMillis()): Boolean = pausedUntil > now

    @Synchronized
    fun today(): DayCounts {
        rollIfNeeded()
        return DayCounts(sp.getInt(K_CHECKINS, 0), sp.getInt(K_STOPS, 0))
    }

    @Synchronized
    fun recordCheckIn() {
        rollIfNeeded()
        sp.edit { putInt(K_CHECKINS, sp.getInt(K_CHECKINS, 0) + 1) }
    }

    @Synchronized
    fun recordStop() {
        rollIfNeeded()
        sp.edit { putInt(K_STOPS, sp.getInt(K_STOPS, 0) + 1) }
    }

    private fun rollIfNeeded() {
        val today = LocalDate.now().toString()
        if (sp.getString(K_DAY, null) != today) {
            sp.edit {
                putString(K_DAY, today)
                putInt(K_CHECKINS, 0)
                putInt(K_STOPS, 0)
            }
        }
    }

    private companion object {
        const val K_INTERVAL = "interval_minutes"
        const val K_ONBOARDED = "onboarding_done"
        const val K_ENABLED = "enabled"
        const val K_PAUSED_UNTIL = "paused_until"
        const val K_EVENING = "evening_line"
        const val K_DAY = "day"
        const val K_CHECKINS = "check_ins"
        const val K_STOPS = "stops"
    }
}
