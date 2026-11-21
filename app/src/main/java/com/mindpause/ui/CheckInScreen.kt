package com.mindpause.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindpause.domain.CheckInMessage
import com.mindpause.ui.theme.MP

sealed interface OverlayUi {
    data class Ask(val message: CheckInMessage) : OverlayUi
    data object Done : OverlayUi
}

@Composable
fun CheckInScreen(ui: OverlayUi?, onDone: () -> Unit, onMore: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(700), label = "fade")

    Box(
        Modifier
            .fillMaxSize()
            .background(MP.Night)
            .alpha(alpha)
            .padding(start = 28.dp, end = 28.dp, top = 64.dp, bottom = 48.dp)
    ) {
        when (ui) {
            is OverlayUi.Ask -> Ask(ui.message, onDone, onMore)
            OverlayUi.Done -> Done()
            null -> Unit
        }
    }
}

@Composable
private fun Ask(message: CheckInMessage, onDone: () -> Unit, onMore: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(44.dp, Alignment.CenterVertically),
        ) {
            Text(message.time, fontSize = 15.sp, color = MP.Muted, letterSpacing = 0.3.sp, textAlign = TextAlign.Center)
            BreathRing(message.cue)
            Text(
                message.question,
                modifier = Modifier.widthIn(max = 310.dp),
                fontSize = 28.sp,
                lineHeight = 37.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton("I'm done", onDone)
            SecondaryButton("5 more minutes", onMore)
        }
    }
}

@Composable
private fun Done() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
    ) {
        RingMark(96.dp)
        Text("Good.", fontSize = 34.sp, fontWeight = FontWeight.Light)
        Text("Put it down gently.", fontSize = 16.sp, color = MP.Muted, textAlign = TextAlign.Center)
    }
}
