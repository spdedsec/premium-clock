/*
 * Design reminder — Chronographic Modernism:
 * Navigation is intentionally limited to Clock, Alarms, Timers, Tools, and Settings.
 * Time is the hero; every secondary screen uses progressive disclosure rather than dashboard clutter.
 */
package com.premiumclock.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val vm: ClockViewModel by viewModels()
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent {
            val settings by vm.settings.collectAsState()
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val dark = when (settings.theme) { "dark" -> true; "light" -> false; else -> systemDark }
            PremiumClockTheme(dark) { PremiumClockApp(vm) }
        }
    }
}

private enum class AppDestination(val label: String) { CLOCK("Clock"), ALARMS("Alarms"), TIMERS("Timers"), TOOLS("Tools"), SETTINGS("Settings") }

@Composable
private fun PremiumClockApp(vm: ClockViewModel = viewModel()) {
    var destinationName by rememberSaveable { mutableStateOf(AppDestination.CLOCK.name) }
    val destination = AppDestination.valueOf(destinationName)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) { listOf(AppDestination.CLOCK, AppDestination.ALARMS, AppDestination.TIMERS, AppDestination.TOOLS, AppDestination.SETTINGS).forEach { item -> NavigationBarItem(selected = destination == item, onClick = { destinationName = item.name }, icon = { Icon(destinationIcon(item), contentDescription = item.label) }, label = { Text(item.label, fontSize = 10.sp) }) } } }
    ) { inset ->
        Box(Modifier.fillMaxSize().padding(inset)) {
            when (destination) {
                AppDestination.CLOCK -> ClockScreen(vm)
                AppDestination.ALARMS -> AlarmsScreen(vm)
                AppDestination.TIMERS -> TimersScreen(vm)
                AppDestination.TOOLS -> ToolsScreen(vm)
                AppDestination.SETTINGS -> SettingsScreen(vm)
            }
        }
    }
}

@Composable
private fun destinationIcon(destination: AppDestination) = when (destination) { AppDestination.CLOCK -> Icons.Outlined.AccessTime; AppDestination.ALARMS -> Icons.Outlined.Alarm; AppDestination.TIMERS -> Icons.Outlined.Timer; AppDestination.TOOLS -> Icons.Outlined.Build; AppDestination.SETTINGS -> Icons.Outlined.Settings }

@Composable
private fun ClockScreen(vm: ClockViewModel) {
    val settings by vm.settings.collectAsState()
    val alarms by vm.alarms.collectAsState()
    val dividerColor = MaterialTheme.colorScheme.outline
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { now = LocalDateTime.now(); delay(250) } }
    val formatter = DateTimeFormatter.ofPattern(if (settings.use24Hour) "HH:mm" else "h:mm", Locale.getDefault())
    val seconds = now.format(DateTimeFormatter.ofPattern("ss"))
    val date = "${now.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}, ${now.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${now.dayOfMonth}"
    val next = alarms.filter { it.enabled }.minByOrNull { (it.hour * 60 + it.minute - now.hour * 60 - now.minute + 1_440) % 1_440 }
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 22.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Column {
            ClockKicker("LOCAL TIME", java.time.ZoneId.systemDefault().id.replace("/", " · "))
            Spacer(Modifier.height(30.dp))
            if (settings.clockStyle == "analog") SwissAnalogClock(now.hour, now.minute, now.second) else {
                val clockSize = when (settings.clockStyle) { "compact" -> 72.sp; "editorial" -> 98.sp; else -> 106.sp }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(now.format(formatter), modifier = Modifier.weight(1f), fontFamily = if (settings.clockStyle == "editorial") FontFamily.SansSerif else FontFamily.Monospace, fontWeight = if (settings.clockStyle == "editorial") FontWeight.Black else FontWeight.Medium, fontSize = clockSize, letterSpacing = (-7).sp, lineHeight = clockSize, maxLines = 1, overflow = TextOverflow.Clip)
                    if (settings.showSeconds) Text(":$seconds", modifier = Modifier.padding(bottom = 13.dp), color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontSize = 30.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(date, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(if (settings.use24Hour) "24-hour time" else "12-hour time", modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Column {
            Text("CLOCK STYLE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(9.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) { items(listOf("large", "compact", "editorial", "analog", "mono")) { style -> StyleChip(style.replaceFirstChar { it.titlecase() }, selected = settings.clockStyle == style) { vm.updateSettings { it.copy(clockStyle = style) } } } }
            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth().drawBehind { drawLine(dividerColor, Offset.Zero, Offset(size.width, 0f), 1f) }.padding(top = 17.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("NEXT ALARM", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp); Spacer(Modifier.height(5.dp)); Text(next?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "No alarm", fontFamily = FontFamily.Monospace, fontSize = 30.sp, fontWeight = FontWeight.Medium) }
                Text(next?.label ?: "Rest is unscripted.", modifier = Modifier.width(120.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
            }
        }
    }
}

@Composable
fun ClockKicker(label: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(width = 3.dp, height = 17.dp).background(MaterialTheme.colorScheme.primary)); Spacer(Modifier.width(9.dp)); Column { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
}

@Composable
private fun StyleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.small
    val surface = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    val signal = MaterialTheme.colorScheme.primary
    TextButton(onClick = onClick, modifier = Modifier.clip(shape).background(surface).drawBehind { if (selected) drawLine(signal, Offset(0f, size.height - 1.dp.toPx()), Offset(size.width, size.height - 1.dp.toPx()), 2.dp.toPx()) }) { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }
}

@Composable
private fun SwissAnalogClock(hour: Int, minute: Int, second: Int) {
    val ink = MaterialTheme.colorScheme.onSurface
    val signal = MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxWidth().height(285.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(260.dp)) {
            val center = Offset(size.width / 2, size.height / 2); val radius = size.minDimension / 2 - 11.dp.toPx()
            drawCircle(color = ink, radius = radius, center = center, style = Stroke(width = 1.5.dp.toPx()))
            repeat(60) { tick ->
                val angle = Math.toRadians((tick * 6 - 90).toDouble()); val outer = Offset(center.x + kotlin.math.cos(angle).toFloat() * radius, center.y + kotlin.math.sin(angle).toFloat() * radius); val innerRadius = radius - if (tick % 5 == 0) 13.dp.toPx() else 6.dp.toPx(); val inner = Offset(center.x + kotlin.math.cos(angle).toFloat() * innerRadius, center.y + kotlin.math.sin(angle).toFloat() * innerRadius); drawLine(ink, outer, inner, if (tick % 5 == 0) 2.dp.toPx() else 1.dp.toPx())
            }
            fun hand(degrees: Float, length: Float, width: Float, color: androidx.compose.ui.graphics.Color) { val a = Math.toRadians((degrees - 90).toDouble()); drawLine(color, center, Offset(center.x + kotlin.math.cos(a).toFloat() * length, center.y + kotlin.math.sin(a).toFloat() * length), width, cap = StrokeCap.Round) }
            hand((hour % 12 + minute / 60f) * 30f, radius * .49f, 5.dp.toPx(), ink); hand((minute + second / 60f) * 6f, radius * .72f, 3.dp.toPx(), ink); hand(second * 6f, radius * .80f, 1.5.dp.toPx(), signal); drawCircle(signal, 5.dp.toPx(), center)
        }
    }
}
