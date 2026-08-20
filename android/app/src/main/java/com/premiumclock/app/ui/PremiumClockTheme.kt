/*
 * Design reminder — Chronographic Modernism:
 * The native palette uses warm paper, graphite type, hairline dividers and a restrained vermilion signal.
 * Avoid decorative cards; use spacing and hierarchy to make the clock feel like a composed instrument.
 */
package com.premiumclock.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

val Paper = Color(0xFFF4F1EA)
val PaperRaised = Color(0xFFFBFAF7)
val Ink = Color(0xFF1D1D1B)
val Quiet = Color(0xFF75736D)
val Signal = Color(0xFFD6472D)
val DarkInk = Color(0xFF121210)
val DarkPaper = Color(0xFF1A1A17)

private val LightScheme: ColorScheme = lightColorScheme(primary = Signal, onPrimary = Color(0xFFFFFAF5), background = Paper, onBackground = Ink, surface = PaperRaised, onSurface = Ink, surfaceVariant = Color(0xFFEBE8E1), onSurfaceVariant = Quiet, outline = Color(0x33201F1C))
private val DarkScheme: ColorScheme = darkColorScheme(primary = Color(0xFFEF6A50), onPrimary = DarkInk, background = DarkInk, onBackground = Paper, surface = DarkPaper, onSurface = Paper, surfaceVariant = Color(0xFF252520), onSurfaceVariant = Color(0xFFAAA8A0), outline = Color(0x33F4F1EA))

@Composable
fun PremiumClockTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, typography = MaterialTheme.typography.copy(displayLarge = MaterialTheme.typography.displayLarge.copy(fontFamily = FontFamily.Monospace), displayMedium = MaterialTheme.typography.displayMedium.copy(fontFamily = FontFamily.Monospace), titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.SansSerif), bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.SansSerif)), shapes = MaterialTheme.shapes.copy(small = RoundedCornerShape(8.dp), medium = RoundedCornerShape(14.dp)), content = content)
}
