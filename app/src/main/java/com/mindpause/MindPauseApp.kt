package com.mindpause

import android.app.Application
import android.content.Context
import com.mindpause.data.Prefs
import com.mindpause.service.Notifications
import com.mindpause.summary.EveningScheduler

class MindPauseApp : Application() {
    lateinit var prefs: Prefs
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        Notifications.createChannels(this)
        EveningScheduler.schedule(this)
    }
}

val Context.prefs: Prefs
    get() = (applicationContext as MindPauseApp).prefs
