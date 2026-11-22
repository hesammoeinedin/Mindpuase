package com.mindpause.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mindpause.summary.EveningScheduler

/** Restarts MindPause after a reboot or an app update. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                EveningScheduler.schedule(context)
                ServiceControl.startIfEnabled(context)
                KeepAlive.schedule(context)
            }
        }
    }
}
