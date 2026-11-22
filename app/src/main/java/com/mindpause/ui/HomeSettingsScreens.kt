package com.mindpause.ui

import android.provider.Settings
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindpause.domain.DailyLine
import com.mindpause.domain.EscalationPolicy
import com.mindpause.prefs
import com.mindpause.service.BackgroundHelp
import com.mindpause.service.PauseService
import com.mindpause.service.ServiceControl
import com.mindpause.ui.theme.MP
import java.text.DateFormat
import java.util.Date

@Composable
fun HomeScreen(tick: Int, onSettings: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.prefs
    var localTick by remember { mutableIntStateOf(0) }
    val refresh = remember(tick, localTick) { Any() }

    val today = remember(refresh) { prefs.today() }
    val enabled = remember(refresh) { prefs.enabled }
    val paused = remember(refresh) { prefs.isPaused() }
    val overlayOk = remember(refresh) { Settings.canDrawOverlays(context) }
    val batteryOk = remember(refresh) { BackgroundHelp.isBatteryUnrestricted(context) }

    val statusTitle = when {
        !enabled -> "MindPause is off"
        paused -> "Paused until " + DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(prefs.pausedUntil))
        else -> "MindPause is on"
    }
    val statusSub = when {
        !enabled -> "No check-ins"
        paused -> "Check-ins resume after that"
        else -> "First check-in after ${prefs.intervalMinutes} minutes"
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(start = 28.dp, end = 20.dp, top = 16.dp, bottom = 32.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RingMark(22.dp)
            Text("MindPause", Modifier.padding(start = 10.dp).weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MP.Mist)
            TextLink("Settings", onSettings)
        }
        Column(
            Modifier
                .weight(1f)
                .padding(end = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            Text("Today", fontSize = 14.sp, color = MP.Muted, letterSpacing = 0.5.sp)
            Text(
                DailyLine.text(today.checkIns, today.stops),
                fontSize = 32.sp,
                lineHeight = 43.sp,
                fontWeight = FontWeight.Light,
            )
        }
        Column(Modifier.padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (enabled && !overlayOk) {
                Card {
                    Text("Check-ins can't appear yet", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("Allow “Display over other apps” for MindPause.", fontSize = 14.sp, color = MP.Muted)
                    TextLink("Open setting", { openOverlaySettings(context) }, color = MP.Sage)
                }
            } else if (enabled && !batteryOk) {
                Card {
                    Text("MindPause may stop in the background", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("Let it run without battery limits so it keeps going after you close all apps.", fontSize = 14.sp, lineHeight = 21.sp, color = MP.Muted)
                    TextLink("Allow", { BackgroundHelp.requestBatteryUnrestricted(context) }, color = MP.Sage)
                }
            }
            SwitchRow(statusTitle, statusSub, checked = enabled) { on ->
                prefs.enabled = on
                if (on) {
                    ServiceControl.start(context)
                } else {
                    prefs.pausedUntil = 0L
                    ServiceControl.stop(context)
                }
                localTick++
            }
            if (enabled) {
                SecondaryButton(
                    if (paused) "Resume now" else "Pause for 1 hour",
                    onClick = {
                        if (paused) {
                            prefs.pausedUntil = 0L
                            ServiceControl.send(context, PauseService.ACTION_RESUME)
                        } else {
                            prefs.pausedUntil = System.currentTimeMillis() + 60 * 60_000L
                            ServiceControl.send(context, PauseService.ACTION_PAUSE)
                        }
                        localTick++
                    },
                    height = 52.dp,
                )
            }
            Text(
                "Nothing leaves this phone.",
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                color = MP.Faint,
            )
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.prefs
    var minutes by remember { mutableIntStateOf(prefs.intervalMinutes) }
    var evening by remember { mutableStateOf(prefs.eveningLine) }

    fun setMinutes(m: Int) {
        minutes = EscalationPolicy.clampInterval(m)
        prefs.intervalMinutes = minutes
        ServiceControl.send(context, PauseService.ACTION_REFRESH)
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextLink("‹  Back", onBack, color = MP.Mist)
            Text("Settings", Modifier.padding(start = 4.dp), fontSize = 22.sp, fontWeight = FontWeight.Medium)
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("First check-in after", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("Then every 5 minutes if you keep going.", fontSize = 14.sp, color = MP.Muted)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RoundIconButton("−", "Fewer minutes") { setMinutes(minutes - 1) }
                    Text(
                        "$minutes minutes",
                        Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Light,
                    )
                    RoundIconButton("+", "More minutes") { setMinutes(minutes + 1) }
                }
                Text("3 to 10 minutes", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 13.sp, color = MP.Faint)
            }
            SwitchRow("Evening line", "One notification at 21:00 with today's line", checked = evening) {
                evening = it
                prefs.eveningLine = it
            }
            Text(
                "Keep running",
                Modifier.padding(start = 8.dp, top = 12.dp),
                fontSize = 14.sp,
                color = MP.Muted,
                letterSpacing = 0.5.sp,
            )
            KeepRunningCards()
        }
        Spacer(Modifier.size(8.dp))
        Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Everything stays on this phone. MindPause has no account, no internet access and no tracking.",
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = MP.Muted,
            )
            Text("Version 1.1", fontSize = 13.sp, color = MP.Faint)
        }
    }
}

@Composable
private fun RoundIconButton(symbol: String, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .border(1.dp, MP.Line, CircleShape)
            .semantics { contentDescription = label }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, fontSize = 22.sp, color = MP.Mist)
    }
}
