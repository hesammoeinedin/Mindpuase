package com.mindpause.service

import android.app.KeyguardManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mindpause.domain.EscalationPolicy
import com.mindpause.prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Runs while MindPause is on. Tracks continuous screen use and shows the
 * check-in overlay when a check-in is due.
 *
 * A session starts at unlock and ends when the screen has been off for longer
 * than [EscalationPolicy.SCREEN_OFF_GRACE_MS]. Timers only need to run while
 * the screen is on, when the device is awake, so plain coroutines are enough.
 */
class PauseService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var timerJob: Job? = null
    private var pauseJob: Job? = null
    private var doneJob: Job? = null

    private var sessionStart = 0L      // 0 = no session
    private var checkInCount = 0
    private var nextDueAt = 0L
    private var screenOffAt = 0L       // 0 = screen is on (or unknown)

    private lateinit var overlay: CheckInOverlay
    private lateinit var power: PowerManager
    private lateinit var keyguard: KeyguardManager
    private lateinit var audio: AudioManager

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> onScreenOff()
                Intent.ACTION_USER_PRESENT -> onUserPresent()
                Intent.ACTION_SCREEN_ON -> if (!keyguard.isKeyguardLocked) onUserPresent()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        power = getSystemService(PowerManager::class.java)
        keyguard = getSystemService(KeyguardManager::class.java)
        audio = getSystemService(AudioManager::class.java)
        overlay = CheckInOverlay(this, onDone = ::onDone, onMore = ::onMore)

        startInForeground()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        if (prefs.isPaused()) schedulePauseEnd() else if (isScreenInUse()) startSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Every start must call startForeground again on some Android versions.
        startInForeground()
        when (intent?.action) {
            ACTION_PAUSE -> pause()
            ACTION_RESUME -> endPause()
            ACTION_REFRESH -> updateNotification()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {
        }
        overlay.hide()
        scope.cancel()
        super.onDestroy()
    }

    // --- Screen state -------------------------------------------------------

    private fun now() = System.currentTimeMillis()

    private fun isScreenInUse() = power.isInteractive && !keyguard.isKeyguardLocked

    private fun onScreenOff() {
        timerJob?.cancel()
        screenOffAt = now()
        if (overlay.isShowing) {
            val wasAsking = overlay.isAsking
            doneJob?.cancel()
            overlay.hide()
            if (wasAsking) {
                // Left unanswered: treat like "5 more minutes" if they come straight back.
                nextDueAt = now() + EscalationPolicy.snoozeDelayMs()
            } else {
                // They had chosen "I'm done": that session is over.
                sessionStart = 0L
            }
        }
    }

    private fun onUserPresent() {
        val pausedUntil = prefs.pausedUntil
        if (pausedUntil != 0L && pausedUntil <= now()) endPause()
        if (prefs.isPaused() || overlay.isShowing) return

        // Already in an active session (SCREEN_ON and USER_PRESENT can both arrive).
        if (sessionStart != 0L && screenOffAt == 0L) return

        val resumed = sessionStart != 0L && now() - screenOffAt <= EscalationPolicy.SCREEN_OFF_GRACE_MS
        screenOffAt = 0L
        if (resumed) {
            scheduleAt(maxOf(nextDueAt, now() + 2_000L))
        } else {
            startSession()
        }
    }

    // --- Session ------------------------------------------------------------

    private fun startSession() {
        if (prefs.isPaused()) return
        sessionStart = now()
        checkInCount = 0
        screenOffAt = 0L
        scheduleAt(sessionStart + EscalationPolicy.firstDelayMs(prefs.intervalMinutes))
    }

    private fun scheduleAt(time: Long) {
        nextDueAt = time
        timerJob?.cancel()
        timerJob = scope.launch {
            delay((time - now()).coerceAtLeast(0L))
            fireCheckIn()
        }
    }

    private fun inCall(): Boolean = when (audio.mode) {
        AudioManager.MODE_IN_CALL, AudioManager.MODE_IN_COMMUNICATION, AudioManager.MODE_RINGTONE -> true
        else -> false
    }

    private fun fireCheckIn() {
        if (!isScreenInUse() || prefs.isPaused() || overlay.isShowing) return
        if (inCall()) {
            scheduleAt(now() + EscalationPolicy.CALL_RETRY_MS)
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            // Can't show anything without the permission; check again later.
            scheduleAt(now() + EscalationPolicy.snoozeDelayMs())
            return
        }
        checkInCount += 1
        val minutes = EscalationPolicy.elapsedMinutes(sessionStart, now())
        val message = EscalationPolicy.message(EscalationPolicy.tierFor(checkInCount), minutes)
        prefs.recordCheckIn()
        overlay.show(message)
    }

    private fun onMore() {
        overlay.hide()
        scheduleAt(now() + EscalationPolicy.snoozeDelayMs())
    }

    private fun onDone() {
        prefs.recordStop()
        overlay.showDone()
        goHome()
        doneJob?.cancel()
        doneJob = scope.launch {
            delay(1_400L)
            overlay.hide()
            // If they keep using the phone, a fresh, gentle session begins.
            if (isScreenInUse()) startSession() else sessionStart = 0L
        }
    }

    private fun goHome() {
        try {
            startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
        }
    }

    // --- Pause --------------------------------------------------------------

    private fun pause() {
        prefs.pausedUntil = now() + PAUSE_MS
        timerJob?.cancel()
        doneJob?.cancel()
        overlay.hide()
        sessionStart = 0L
        updateNotification()
        schedulePauseEnd()
    }

    private fun schedulePauseEnd() {
        pauseJob?.cancel()
        val wait = (prefs.pausedUntil - now()).coerceAtLeast(0L)
        pauseJob = scope.launch {
            delay(wait)
            endPause()
        }
    }

    private fun endPause() {
        pauseJob?.cancel()
        prefs.pausedUntil = 0L
        updateNotification()
        if (isScreenInUse() && !overlay.isShowing) startSession()
    }

    // --- Notification -------------------------------------------------------

    private fun startInForeground() {
        val notification = Notifications.running(this, prefs)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(Notifications.ID_RUNNING, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(Notifications.ID_RUNNING, notification)
        }
    }

    private fun updateNotification() {
        if (!Notifications.canPost(this)) return
        try {
            NotificationManagerCompat.from(this)
                .notify(Notifications.ID_RUNNING, Notifications.running(this, prefs))
        } catch (_: SecurityException) {
        }
    }

    companion object {
        const val ACTION_PAUSE = "com.mindpause.PAUSE"
        const val ACTION_RESUME = "com.mindpause.RESUME"
        const val ACTION_REFRESH = "com.mindpause.REFRESH"
        private const val PAUSE_MS = 60 * 60_000L
    }
}
