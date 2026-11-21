package com.mindpause.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.mindpause.prefs

/** Small helpers to start, stop and talk to [PauseService]. */
object ServiceControl {

    /** Starts the service if the user finished setup and MindPause is on. */
    fun startIfEnabled(context: Context) {
        val prefs = context.prefs
        if (prefs.onboardingDone && prefs.enabled) start(context)
    }

    fun start(context: Context) {
        try {
            ContextCompat.startForegroundService(context, Intent(context, PauseService::class.java))
        } catch (_: Exception) {
            // Android may refuse a start from the background; the next app open retries.
        }
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, PauseService::class.java))
    }

    fun send(context: Context, action: String) {
        try {
            context.startService(Intent(context, PauseService::class.java).setAction(action))
        } catch (_: Exception) {
        }
    }
}
