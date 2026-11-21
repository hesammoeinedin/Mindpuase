package com.mindpause.domain

/** One check-in screen's text. */
data class CheckInMessage(val time: String, val cue: String, val question: String)

/**
 * The rules of a session, kept free of Android so they can be unit-tested.
 *
 * - First check-in after the user's interval (3..10 minutes).
 * - Each "5 more minutes" schedules the next one 5 minutes later.
 * - The message tier rises with each check-in in the session; tier 4 repeats.
 * - The minutes shown are the real elapsed minutes of the session.
 */
object EscalationPolicy {
    const val MIN_INTERVAL = 3
    const val MAX_INTERVAL = 10
    const val SNOOZE_MINUTES = 5
    const val MINUTE_MS = 60_000L

    /** A screen-off shorter than this does not end the session. */
    const val SCREEN_OFF_GRACE_MS = 30_000L

    /** While on a call, try again after this long. */
    const val CALL_RETRY_MS = 30_000L

    fun clampInterval(minutes: Int): Int = minutes.coerceIn(MIN_INTERVAL, MAX_INTERVAL)

    fun firstDelayMs(intervalMinutes: Int): Long = clampInterval(intervalMinutes) * MINUTE_MS

    fun snoozeDelayMs(): Long = SNOOZE_MINUTES * MINUTE_MS

    /** [checkInNumber] is 1 for the first check-in of a session. */
    fun tierFor(checkInNumber: Int): Int = checkInNumber.coerceIn(1, 4)

    /** Elapsed session time rounded to whole minutes, never below 1. */
    fun elapsedMinutes(sessionStartMs: Long, nowMs: Long): Int =
        ((nowMs - sessionStartMs + MINUTE_MS / 2) / MINUTE_MS).toInt().coerceAtLeast(1)

    fun message(tier: Int, minutes: Int): CheckInMessage = when (tier.coerceIn(1, 4)) {
        1 -> CheckInMessage(
            time = "You've been here $minutes minutes.",
            cue = "Breathe.",
            question = "Is this where you want to be?",
        )
        2 -> CheckInMessage(
            time = "$minutes minutes.",
            cue = "Take one breath.",
            question = "What were you looking for when you picked up your phone?",
        )
        3 -> CheckInMessage(
            time = "Still here after $minutes minutes.",
            cue = "Breathe slowly.",
            question = "Is this serving you — or just filling time?",
        )
        else -> CheckInMessage(
            time = "$minutes minutes.",
            cue = "One breath.",
            question = "What would feel better than this right now?",
        )
    }
}

object DailyLine {
    fun text(checkIns: Int, stops: Int): String {
        if (checkIns <= 0) return "No check-ins yet today."
        val c = if (checkIns == 1) "1 check-in" else "$checkIns check-ins"
        val s = if (stops == 1) "1 time" else "$stops times"
        return "Today you had $c. You chose to stop $s."
    }
}
