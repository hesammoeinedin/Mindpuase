package com.mindpause.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mindpause.prefs
import com.mindpause.service.ServiceControl
import com.mindpause.ui.theme.MP
import com.mindpause.ui.theme.MindPauseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Make sure the service is running whenever the app is opened.
        ServiceControl.startIfEnabled(this)
        setContent {
            MindPauseTheme { MindPauseRoot() }
        }
    }
}

private object Screen {
    const val WELCOME = "welcome"
    const val INTERVAL = "interval"
    const val PERMISSIONS = "permissions"
    const val BATTERY = "battery"
    const val HOME = "home"
    const val SETTINGS = "settings"
}

@Composable
private fun MindPauseRoot() {
    val context = LocalContext.current
    val prefs = context.prefs
    var screen by rememberSaveable { mutableStateOf(if (prefs.onboardingDone) Screen.HOME else Screen.WELCOME) }
    val tick = rememberResumeTick()

    val back: String? = when (screen) {
        Screen.INTERVAL -> Screen.WELCOME
        Screen.PERMISSIONS -> Screen.INTERVAL
        Screen.BATTERY -> Screen.PERMISSIONS
        Screen.SETTINGS -> Screen.HOME
        else -> null
    }
    BackHandler(enabled = back != null) { back?.let { screen = it } }

    Box(
        Modifier
            .fillMaxSize()
            .background(MP.Night)
            .safeDrawingPadding()
    ) {
        when (screen) {
            Screen.WELCOME -> WelcomeScreen(onNext = { screen = Screen.INTERVAL })
            Screen.INTERVAL -> IntervalScreen(onNext = { screen = Screen.PERMISSIONS })
            Screen.PERMISSIONS -> PermissionsScreen(tick, onNext = { screen = Screen.BATTERY })
            Screen.BATTERY -> BatteryScreen(
                onDone = {
                    finishOnboarding(context)
                    screen = Screen.HOME
                },
            )
            Screen.HOME -> HomeScreen(tick, onSettings = { screen = Screen.SETTINGS })
            Screen.SETTINGS -> SettingsScreen(onBack = { screen = Screen.HOME })
        }
    }
}

private fun finishOnboarding(context: Context) {
    val prefs = context.prefs
    prefs.onboardingDone = true
    prefs.enabled = true
    ServiceControl.start(context)
}

fun openAppDetails(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {
    }
}

fun openOverlaySettings(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {
        openAppDetails(context)
    }
}

/** Increments every time the activity resumes, so screens re-read settings and permissions. */
@Composable
fun rememberResumeTick(): Int {
    val owner = LocalLifecycleOwner.current
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick++
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return tick
}
