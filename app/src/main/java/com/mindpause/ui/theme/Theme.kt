package com.mindpause.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.mindpause.R

object MP {
    val Night = Color(0xFF0B0E12)
    val Ink = Color(0xFFE8E6E1)
    val Mist = Color(0xFFC9CDD2)
    val Muted = Color(0xFF9AA3AE)
    val Faint = Color(0xFF7D8692)
    val Sage = Color(0xFF8FB8A8)
    val RingStroke = Color(0xFF5E8576)
    val RingFill = Color(0xFF121A19)
    val Surface = Color(0xFF12161C)
    val CardLine = Color(0xFF1F252D)
    val Line = Color(0xFF2A313B)
}

@OptIn(ExperimentalTextApi::class)
private fun manrope(weight: Int) = Font(
    R.font.manrope,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Manrope = FontFamily(manrope(300), manrope(400), manrope(500), manrope(600))

@Composable
fun MindPauseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = MP.Night,
            surface = MP.Surface,
            primary = MP.Sage,
            onPrimary = MP.Night,
            onBackground = MP.Ink,
            onSurface = MP.Ink,
        ),
    ) {
        ProvideTextStyle(TextStyle(fontFamily = Manrope, color = MP.Ink), content = content)
    }
}
