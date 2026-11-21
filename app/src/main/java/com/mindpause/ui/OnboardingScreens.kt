package com.mindpause.ui

import android.Manifest
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindpause.prefs
import com.mindpause.service.Notifications
import com.mindpause.ui.theme.MP

@Composable
private fun OnboardingFrame(
    step: Int,
    bottom: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(start = 28.dp, end = 28.dp, top = 32.dp, bottom = 32.dp)
    ) {
        StepDots(step)
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
            content = content,
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = bottom)
    }
}

@Composable
private fun Heading(text: String, size: Int = 30) {
    Text(text, fontSize = size.sp, lineHeight = (size * 1.25f).sp, fontWeight = FontWeight.Light)
}

@Composable
private fun Body(text: String) {
    Text(text, fontSize = 16.sp, lineHeight = 26.sp, color = MP.Muted)
}

@Composable
fun WelcomeScreen(onNext: () -> Unit) {
    OnboardingFrame(step = 1, bottom = { PrimaryButton("Begin", onNext) }) {
        RingMark(72.dp)
        Heading("A breath, now and then.", 34)
        Body("When you've been on your phone for a few minutes, MindPause checks in with one breath and one honest question.")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("No streaks. No scores. No graphs.", fontSize = 15.sp, color = MP.Mist)
            Text("Nothing leaves your phone.", fontSize = 15.sp, color = MP.Mist)
        }
    }
}

@Composable
fun IntervalScreen(onNext: () -> Unit) {
    val prefs = LocalContext.current.prefs
    var picked by remember { mutableIntStateOf(prefs.intervalMinutes) }
    OnboardingFrame(
        step = 2,
        bottom = {
            PrimaryButton("Continue", onClick = {
                prefs.intervalMinutes = picked
                onNext()
            })
            Text(
                "You can change this anytime in Settings.",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                color = MP.Faint,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Heading("When should I first check in?")
            Body("After this many minutes of screen time. Each time you choose “5 more minutes”, the next check-in comes 5 minutes later.")
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (row in listOf(3..6, 7..10)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (n in row) {
                        MinuteChip(n, selected = n == picked, onClick = { picked = n }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        Text(
            "First check-in after $picked minutes" + if (picked == 3) " (recommended)" else "",
            fontSize = 15.sp,
            color = MP.Mist,
        )
    }
}

@Composable
private fun MinuteChip(n: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .height(56.dp)
            .clip(shape)
            .background(if (selected) MP.Sage else MP.Surface)
            .border(1.dp, if (selected) MP.Sage else MP.Line, shape)
            .semantics { this.selected = selected }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("$n", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = if (selected) MP.Night else MP.Mist)
    }
}

@Composable
fun PermissionsScreen(tick: Int, onNext: () -> Unit) {
    val context = LocalContext.current
    var localTick by remember { mutableIntStateOf(0) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { localTick++ }

    // Re-read on every resume (coming back from system settings) and after the dialog.
    val overlayOk = remember(tick, localTick) { Settings.canDrawOverlays(context) }
    val notifOk = remember(tick, localTick) { Notifications.canPost(context) }

    OnboardingFrame(step = 3, bottom = { PrimaryButton("Continue", onNext) }) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Heading("Two permissions, and why.")
            Body("MindPause doesn't see which apps you use or what's on your screen. It only knows when the screen is on.")
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PermissionCard(
                title = "Display over other apps",
                body = "So a check-in can appear wherever you are.",
                granted = overlayOk,
                onAllow = { openOverlaySettings(context) },
            )
            PermissionCard(
                title = "Notifications",
                body = "Android requires one quiet notification while MindPause runs. You'll also get one line each evening.",
                granted = notifOk,
                onAllow = {
                    if (Build.VERSION.SDK_INT >= 33) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        openAppDetails(context)
                    }
                },
            )
        }
    }
}

@Composable
private fun PermissionCard(title: String, body: String, granted: Boolean, onAllow: () -> Unit) {
    Card {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(body, fontSize = 14.sp, lineHeight = 22.sp, color = MP.Muted)
        }
        if (granted) {
            Box(Modifier.height(44.dp), contentAlignment = Alignment.CenterStart) {
                Text("Allowed", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MP.Sage)
            }
        } else {
            val shape = RoundedCornerShape(22.dp)
            Box(
                Modifier
                    .height(44.dp)
                    .clip(shape)
                    .border(1.dp, MP.RingStroke, shape)
                    .clickable(role = Role.Button, onClick = onAllow)
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Allow", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MP.Sage)
            }
        }
    }
}

@Composable
fun BatteryScreen(onOpenSettings: () -> Unit, onSkip: () -> Unit) {
    OnboardingFrame(
        step = 4,
        bottom = {
            PrimaryButton("Open battery settings", onOpenSettings)
            SecondaryButton("Skip for now", onSkip)
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Heading("Let MindPause stay awake.")
            Body("Some phones close background apps to save battery. If yours does, check-ins won't arrive.")
        }
        Card {
            Text("In app settings", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MP.Mist)
            Text("1.  Open Battery (or App battery usage)", fontSize = 15.sp, lineHeight = 22.sp, color = MP.Muted)
            Text("2.  Choose “Unrestricted” or “Don't optimise”", fontSize = 15.sp, lineHeight = 22.sp, color = MP.Muted)
            Text(
                "MindPause uses almost no battery. It only listens for the screen turning on and off.",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = MP.Faint,
            )
        }
    }
}
