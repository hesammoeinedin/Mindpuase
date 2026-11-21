package com.mindpause.ui

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindpause.ui.theme.MP

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MP.Sage)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = MP.Night, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 56.dp) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .border(1.dp, MP.Line, RoundedCornerShape(height / 2))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = MP.Mist, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun TextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = MP.Muted) {
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = color, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MP.Surface)
            .border(1.dp, MP.CardLine, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, fontSize = 14.sp, color = MP.Muted, lineHeight = 21.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MP.Night,
                    checkedTrackColor = MP.Sage,
                    checkedBorderColor = MP.Sage,
                    uncheckedThumbColor = MP.Muted,
                    uncheckedTrackColor = MP.Line,
                    uncheckedBorderColor = MP.Line,
                ),
            )
        }
    }
}

@Composable
fun RingMark(size: Dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(MP.RingFill)
            .border(1.5.dp, MP.RingStroke, CircleShape)
    )
}

/** The slowly breathing ring, with the breath cue inside. */
@Composable
fun BreathRing(label: String, size: Dp = 176.dp) {
    val context = LocalContext.current
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val transition = rememberInfiniteTransition(label = "breath")
    val scale by transition.animateFloat(
        initialValue = 0.84f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale",
    )
    val s = if (reduceMotion) 1f else scale
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = s
                    scaleY = s
                    alpha = 0.6f + 0.4f * ((s - 0.84f) / 0.16f)
                }
                .clip(CircleShape)
                .background(MP.RingFill)
                .border(1.5.dp, MP.RingStroke, CircleShape)
        )
        Text(label, color = MP.Sage, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun StepDots(step: Int, total: Int = 4) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 1..total) {
            val color = when {
                i == step -> MP.Sage
                i < step -> MP.RingStroke
                else -> MP.Line
            }
            Box(
                Modifier
                    .width(if (i == step) 24.dp else 8.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}
