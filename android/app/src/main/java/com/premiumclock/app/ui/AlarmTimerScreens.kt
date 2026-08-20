/*
 * Design reminder — Chronographic Modernism:
 * Alarms are a clean schedule and timers are calibrated instruments. Advanced controls appear only when adding/editing,
 * never as a crowded dashboard. Use the vermilion signal only for live, enabled, or primary states.
 */
package com.premiumclock.app.ui

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.premiumclock.app.data.AlarmEntity
import com.premiumclock.app.data.TimerEntity
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun AlarmsScreen(vm: ClockViewModel) {
    val alarms by vm.alarms.collectAsState()
    var editing by remember { mutableStateOf<AlarmEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
        SectionHeading("WAKE", "Alarms, considered.", "Build a schedule that stays quiet until it needs to be heard.")
        Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${alarms.size} saved alarm${if (alarms.size == 1) "" else "s"}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { creating = true }) { Icon(Icons.Outlined.Add, null, Modifier.size(17.dp)); Spacer(Modifier.width(4.dp)); Text("New alarm") }
        }
        if (alarms.isEmpty()) EmptyState("No alarms yet", "Open → see time → create alarm.") else LazyColumn { items(alarms, key = { it.id }) { alarm -> AlarmRow(alarm, onToggle = { vm.toggleAlarm(alarm) }, onEdit = { editing = alarm }, onDelete = { vm.deleteAlarm(alarm) }) } }
    }
    if (creating) AlarmEditorDialog(initial = null, onDismiss = { creating = false }) { saved -> vm.saveAlarm(saved); creating = false }
    editing?.let { alarm -> AlarmEditorDialog(initial = alarm, onDismiss = { editing = null }) { saved -> vm.saveAlarm(saved); editing = null } }
}

@Composable
private fun AlarmRow(alarm: AlarmEntity, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val divider = MaterialTheme.colorScheme.outline
    Row(Modifier.fillMaxWidth().drawBehind { drawLine(divider, Offset(0f, size.height - 1.dp.toPx()), Offset(size.width, size.height - 1.dp.toPx()), 1f) }.clickable(onClick = onEdit).padding(vertical = 17.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("%02d:%02d".format(Locale.US, alarm.hour, alarm.minute), fontFamily = FontFamily.Monospace, fontSize = 29.sp, letterSpacing = (-2).sp, color = if (alarm.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(15.dp))
        Column(Modifier.weight(1f)) { Text(alarm.label.ifBlank { "Alarm" }, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(if (alarm.repeatSet().isEmpty()) "One time" else alarm.repeatSet().sorted().joinToString(" · ") { listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")[it] }, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium, fontSize = 11.sp) }
        Switch(checked = alarm.enabled, onCheckedChange = { onToggle() })
        IconButton(onClick = onDelete) { Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete ${alarm.label}", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun AlarmEditorDialog(initial: AlarmEntity?, onDismiss: () -> Unit, onSave: (AlarmEntity) -> Unit) {
    val context = LocalContext.current
    var hour by remember(initial) { mutableStateOf((initial?.hour ?: 7).toString().padStart(2, '0')) }
    var minute by remember(initial) { mutableStateOf((initial?.minute ?: 0).toString().padStart(2, '0')) }
    var label by remember(initial) { mutableStateOf(initial?.label ?: "") }
    var repeat by remember(initial) { mutableStateOf(initial?.repeatSet() ?: emptySet()) }
    var snooze by remember(initial) { mutableIntStateOf(initial?.snoozeMinutes ?: 9) }
    var strict by remember(initial) { mutableStateOf(initial?.strictMode ?: false) }
    var gentle by remember(initial) { mutableStateOf(initial?.gradualVolume ?: false) }
    var preAlarm by remember(initial) { mutableIntStateOf(initial?.preAlarmMinutes ?: 0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New alarm" else "Edit alarm", fontWeight = FontWeight.Black) },
        text = { LazyColumn { item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(hour, { hour = it.take(2).filter(Char::isDigit) }, label = { Text("Hour") }, modifier = Modifier.weight(1f), singleLine = true); OutlinedTextField(minute, { minute = it.take(2).filter(Char::isDigit) }, label = { Text("Minute") }, modifier = Modifier.weight(1f), singleLine = true) }
            OutlinedTextField(label, { label = it }, label = { Text("Label") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(8.dp)); Text("REPEAT", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { listOf("S", "M", "T", "W", "T", "F", "S").forEachIndexed { index, day -> DayToggle(day, index in repeat) { repeat = if (index in repeat) repeat - index else repeat + index } }
            }
            SettingSwitch("Gentle Wake", "Increase alarm volume gradually.", gentle) { gentle = it }
            SettingSwitch("Strict Alarm", "Require an extra confirmation to dismiss.", strict) { strict = it }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Snooze duration", fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Stepper(snooze, { snooze = (snooze - 1).coerceAtLeast(1) }, { snooze = (snooze + 1).coerceAtMost(60) }, "min") }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Pre-alarm", fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Stepper(preAlarm, { preAlarm = (preAlarm - 5).coerceAtLeast(0) }, { preAlarm = (preAlarm + 5).coerceAtMost(60) }, "min") }
        } } },
        confirmButton = { Button(onClick = { val h = hour.toIntOrNull()?.coerceIn(0, 23) ?: 7; val m = minute.toIntOrNull()?.coerceIn(0, 59) ?: 0; onSave(AlarmEntity(id = initial?.id ?: 0, hour = h, minute = m, label = label.ifBlank { "Alarm" }, repeatDays = repeat.sorted().joinToString(","), enabled = initial?.enabled ?: true, snoozeMinutes = snooze, strictMode = strict, gradualVolume = gentle, preAlarmMinutes = preAlarm)); val manager = context.getSystemService(AlarmManager::class.java); if (Build.VERSION.SDK_INT >= 31 && !manager.canScheduleExactAlarms()) context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable private fun DayToggle(label: String, selected: Boolean, onClick: () -> Unit) { Text(label, modifier = Modifier.size(30.dp).clip(CircleShape).background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onClick).padding(top = 7.dp), color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }

@Composable fun SettingSwitch(title: String, description: String, value: Boolean, onChange: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth().padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp) }; Switch(value, onChange) } }

@Composable private fun Stepper(value: Int, down: () -> Unit, up: () -> Unit, suffix: String) { Row(verticalAlignment = Alignment.CenterVertically) { TextButton(onClick = down) { Text("−", fontSize = 19.sp) }; Text("$value $suffix", fontFamily = FontFamily.Monospace, fontSize = 12.sp); TextButton(onClick = up) { Text("+", fontSize = 17.sp) } } }

@Composable
fun TimersScreen(vm: ClockViewModel) {
    val timers by vm.timers.collectAsState()
    val stopwatch by vm.stopwatch.collectAsState()
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var showCustom by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(200) } }
    val stopwatchElapsed = if (stopwatch.running) stopwatch.elapsedMillis + (now - (stopwatch.startedAtMillis ?: now)) else stopwatch.elapsedMillis
    Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
        SectionHeading("MEASURE", "Intervals with intent.", "Run multiple timers, then keep an accurate record with the stopwatch.")
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("COUNTDOWNS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp); TextButton(onClick = { showCustom = true }) { Icon(Icons.Outlined.Add, null, Modifier.size(17.dp)); Text("New timer") } }
        LazyColumn {
            if (timers.isEmpty()) item { EmptyState("No countdowns", "Quick-start a preset below, or add a precise custom interval.") }
            items(timers, key = { it.id }) { timer -> TimerRow(timer, now, onToggle = { vm.toggleTimer(timer) }, onReset = { vm.resetTimer(timer) }, onDelete = { vm.deleteTimer(timer) }) }
            item { Spacer(Modifier.height(18.dp)); Text("QUICK START", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp); Spacer(Modifier.height(8.dp)); QuickPresetRow { minutes -> vm.createTimer("$minutes min timer", minutes) }; Spacer(Modifier.height(26.dp)); StopwatchPanel(stopwatchElapsed, stopwatch.running, stopwatch.laps, onToggle = vm::toggleStopwatch, onLap = { vm.lapStopwatch(stopwatchElapsed) }, onReset = vm::resetStopwatch) }
        }
    }
    if (showCustom) TimerEditorDialog(onDismiss = { showCustom = false }) { name, minutes, repeat -> vm.createTimer(name, minutes, repeat); showCustom = false }
}

@Composable private fun TimerRow(timer: TimerEntity, now: Long, onToggle: () -> Unit, onReset: () -> Unit, onDelete: () -> Unit) { val remaining = if (timer.running) (timer.endAtMillis ?: now) - now else timer.remainingMillis; val progress = 1f - remaining.coerceAtLeast(0).toFloat() / timer.durationMillis.coerceAtLeast(1).toFloat(); val divider = MaterialTheme.colorScheme.outline; Row(Modifier.fillMaxWidth().drawBehind { drawLine(divider, Offset(0f, size.height - 1.dp.toPx()), Offset(size.width, size.height - 1.dp.toPx()), 1f) }.padding(vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) { TimerProgress(progress); Spacer(Modifier.width(13.dp)); Column(Modifier.weight(1f)) { Text(timer.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(if (timer.running) "Running" else if (remaining <= 0) "Complete" else "Paused", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium); Text(formatDuration(remaining), fontFamily = FontFamily.Monospace, fontSize = 22.sp, letterSpacing = (-1).sp) }; IconButton(onClick = onToggle) { Icon(if (timer.running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, if (timer.running) "Pause" else "Start") }; IconButton(onClick = onReset) { Icon(Icons.Outlined.Refresh, "Reset") }; IconButton(onClick = onDelete) { Icon(Icons.Outlined.DeleteOutline, "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun TimerProgress(progress: Float) { val track = MaterialTheme.colorScheme.surfaceVariant; val signal = MaterialTheme.colorScheme.primary; Canvas(Modifier.size(45.dp)) { val stroke = 5.dp.toPx(); drawCircle(track, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke)); drawArc(signal, -90f, progress.coerceIn(0f, 1f) * 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Square)) } }
@Composable private fun QuickPresetRow(onStart: (Int) -> Unit) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { listOf(1, 5, 10, 25, 50).forEach { minute -> OutlinedButton(onClick = { onStart(minute) }, modifier = Modifier.width(60.dp).height(46.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text("$minute\nmin", fontFamily = FontFamily.Monospace, fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center) } } } }
@Composable private fun StopwatchPanel(elapsed: Long, running: Boolean, laps: List<Long>, onToggle: () -> Unit, onLap: () -> Unit, onReset: () -> Unit) { Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium).padding(18.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("ELAPSED", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp); Text("Stopwatch", fontWeight = FontWeight.Bold, fontSize = 14.sp) }; Icon(Icons.Outlined.Timer, null) }; Text(formatDuration(elapsed, true), modifier = Modifier.fillMaxWidth().padding(vertical = 21.dp), fontFamily = FontFamily.Monospace, fontSize = 46.sp, letterSpacing = (-3).sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = onToggle, modifier = Modifier.weight(1f)) { Icon(if (running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, null); Spacer(Modifier.width(5.dp)); Text(if (running) "Pause" else "Start") }; OutlinedButton(onClick = onLap, enabled = running, modifier = Modifier.weight(1f)) { Text("Lap") }; IconButton(onClick = onReset) { Icon(Icons.Outlined.Refresh, "Reset") } }; laps.take(3).forEachIndexed { index, lap -> Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("Lap ${laps.size - index}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp); Text(formatDuration(lap, true), fontFamily = FontFamily.Monospace, fontSize = 11.sp) } } } }
@Composable private fun TimerEditorDialog(onDismiss: () -> Unit, onSave: (String, Int, Boolean) -> Unit) { var name by remember { mutableStateOf("") }; var minutes by remember { mutableStateOf("10") }; var repeat by remember { mutableStateOf(false) }; AlertDialog(onDismissRequest = onDismiss, title = { Text("New timer", fontWeight = FontWeight.Black) }, text = { Column { OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true); OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit) }, label = { Text("Minutes") }, modifier = Modifier.fillMaxWidth(), singleLine = true); SettingSwitch("Repeat timer", "Restart this interval after completion.", repeat) { repeat = it } } }, confirmButton = { Button(onClick = { onSave(name.ifBlank { "Custom timer" }, minutes.toIntOrNull()?.coerceAtLeast(1) ?: 1, repeat) }) { Text("Start") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }) }
@Composable fun SectionHeading(kicker: String, title: String, description: String) { Column { ClockKicker(kicker, "PREMIUM CLOCK"); Spacer(Modifier.height(11.dp)); Text(title, fontSize = 37.sp, lineHeight = 35.sp, letterSpacing = (-2.5).sp, fontWeight = FontWeight.Black); Text(description, modifier = Modifier.padding(top = 9.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 19.sp) } }
@Composable fun EmptyState(title: String, description: String) { Column(Modifier.fillMaxWidth().padding(vertical = 38.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(title, fontWeight = FontWeight.Bold); Text(description, modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center) } }
