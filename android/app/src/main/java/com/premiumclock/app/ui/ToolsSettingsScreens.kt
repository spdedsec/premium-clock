/*
 * Design reminder — Chronographic Modernism:
 * Tools are composed as an editorial workbench, not a wall of utility widgets.
 * Keep each calculation legible, local-first and easy to dismiss after a quick decision.
 */
package com.premiumclock.app.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.premiumclock.app.data.TimeEventEntity
import com.premiumclock.app.data.WorldClockEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class Tool(val label: String) { WORLD("World time"), CONVERT("Converter"), DATES("Dates"), BEDTIME("Bedtime"), FOCUS("Focus") }

@Composable
fun ToolsScreen(vm: ClockViewModel) {
    var selected by remember { mutableStateOf(Tool.WORLD) }
    LazyColumn(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
        item { SectionHeading("TOOLS", "Time, in context.", "Plan across cities, translate local time and work in deliberate intervals."); Spacer(Modifier.height(17.dp)); ToolTabs(selected) { selected = it }; Spacer(Modifier.height(18.dp)) }
        item { when (selected) { Tool.WORLD -> WorldTimeTool(vm); Tool.CONVERT -> TimeConverterTool(); Tool.DATES -> DateTools(); Tool.BEDTIME -> BedtimeTool(); Tool.FOCUS -> FocusTool(vm) } }
    }
}

@Composable private fun ToolTabs(selected: Tool, onSelect: (Tool) -> Unit) { androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(Tool.entries) { tool -> TextButton(onClick = { onSelect(tool) }, modifier = Modifier.background(if (tool == selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)) { Text(tool.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (tool == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) } } } }

@Composable private fun WorldTimeTool(vm: ClockViewModel) {
    val worlds by vm.worldClocks.collectAsState()
    val settings by vm.settings.collectAsState()
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var adding by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1_000) } }
    Column { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text("WORLD TIME", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("Places you keep close", fontWeight = FontWeight.Bold, fontSize = 18.sp) }; TextButton(onClick = { adding = true }) { Icon(Icons.Outlined.Add, null, Modifier.padding(end = 2.dp)); Text("City") } }
        if (worlds.isEmpty()) EmptyState("No cities pinned", "Add a city to compare it against local time.") else worlds.forEach { clock -> WorldRow(clock, settings.use24Hour, now) { vm.removeWorldClock(clock) } }
    }
    if (adding) WorldClockDialog(onDismiss = { adding = false }) { zone, name -> vm.addWorldClock(zone, name); adding = false }
}

@Composable private fun WorldRow(clock: WorldClockEntity, use24Hour: Boolean, now: Long, onRemove: () -> Unit) { val zone = remember(clock.zoneId, now) { ZoneId.of(clock.zoneId) }; val zoned = remember(clock.zoneId, now) { Instant.ofEpochMilli(now).atZone(zone) }; val formatter = remember(use24Hour) { DateTimeFormatter.ofPattern(if (use24Hour) "HH:mm" else "h:mm a", Locale.getDefault()) }; val divider = MaterialTheme.colorScheme.outline; Row(Modifier.fillMaxWidth().drawBehind { drawLine(divider, Offset(0f, size.height - 1.dp.toPx()), Offset(size.width, size.height - 1.dp.toPx()), 1f) }.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Public, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(clock.displayName, fontWeight = FontWeight.Bold); Text(zoned.format(DateTimeFormatter.ofPattern("EEE, MMM d")), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text(zoned.format(formatter), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 22.sp); IconButton(onClick = onRemove) { Icon(Icons.Outlined.Close, "Remove ${clock.displayName}", tint = MaterialTheme.colorScheme.onSurfaceVariant) } } }

@Composable private fun WorldClockDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) { val cities = listOf("America/New_York" to "New York", "Europe/London" to "London", "Europe/Paris" to "Paris", "Asia/Tokyo" to "Tokyo", "Asia/Singapore" to "Singapore", "Australia/Sydney" to "Sydney", "Asia/Kolkata" to "Mumbai", "America/Los_Angeles" to "Los Angeles"); var query by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = onDismiss, title = { Text("Add world clock", fontWeight = FontWeight.Black) }, text = { Column { OutlinedTextField(query, { query = it }, label = { Text("Search city") }, modifier = Modifier.fillMaxWidth()); cities.filter { it.second.contains(query, true) || it.first.contains(query, true) }.forEach { (zone, name) -> Row(Modifier.fillMaxWidth().clickable { onAdd(zone, name) }.padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(name, fontWeight = FontWeight.Bold); Text(zone.substringAfter("/"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) } } } }, confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }) }

@Composable private fun TimeConverterTool() {
    var sourceTime by remember { mutableStateOf("09:30") }; var sourceZone by remember { mutableStateOf("America/New_York") }; var targetZone by remember { mutableStateOf("Asia/Tokyo") }
    val converted = remember(sourceTime, sourceZone, targetZone) { runCatching { val parts = sourceTime.split(":"); val local = LocalTime.of(parts[0].toInt(), parts[1].toInt()); ZonedDateTime.of(LocalDate.now(ZoneId.of(sourceZone)), local, ZoneId.of(sourceZone)).withZoneSameInstant(ZoneId.of(targetZone)) }.getOrNull() }
    Column { InstrumentTitle("TIME CONVERTER", "Translate a local appointment without leaving the device."); OutlinedTextField(sourceTime, { sourceTime = it }, label = { Text("Time (HH:MM)") }, modifier = Modifier.fillMaxWidth(), singleLine = true); ZonePicker("From", sourceZone) { sourceZone = it }; ZonePicker("To", targetZone) { targetZone = it }; Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium).padding(20.dp)) { Text("RESULT", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp); Text(converted?.format(DateTimeFormatter.ofPattern("EEE, MMM d · HH:mm z", Locale.getDefault())) ?: "Enter a valid time", modifier = Modifier.padding(top = 9.dp), fontFamily = FontFamily.Monospace, fontSize = 19.sp, fontWeight = FontWeight.Medium); Text(targetZone, modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) } }
}
@Composable private fun ZonePicker(label: String, selected: String, onChange: (String) -> Unit) { var expanded by remember { mutableStateOf(false) }; Column(Modifier.padding(vertical = 7.dp)) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold); TextButton(onClick = { expanded = !expanded }) { Text(selected, fontFamily = FontFamily.Monospace) }; if (expanded) Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("America/New_York", "Europe/London", "Asia/Kolkata", "Asia/Tokyo", "Australia/Sydney").forEach { zone -> TextButton(onClick = { onChange(zone); expanded = false }) { Text(zone.substringAfter("/"), fontSize = 10.sp) } } } } }

@Composable private fun DateTools() { var offset by remember { mutableIntStateOf(0) }; val date = remember(offset) { LocalDate.now().plusDays(offset.toLong()) }; Column { InstrumentTitle("DATE TOOLS", "Name the distance between two moments."); Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium).padding(18.dp)) { Text("ANCHOR DATE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp); Text(date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")), modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { offset -= 1 }) { Text("Yesterday") }; OutlinedButton(onClick = { offset = 0 }) { Text("Today") }; OutlinedButton(onClick = { offset += 1 }) { Text("Tomorrow") } } }; Spacer(Modifier.height(12.dp)); DateMetric("DAY OF YEAR", date.dayOfYear.toString()); DateMetric("DAYS FROM TODAY", Duration.between(LocalDate.now().atStartOfDay(), date.atStartOfDay()).toDays().toString()); DateMetric("90 DAYS FROM ANCHOR", date.plusDays(90).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))) } }
@Composable private fun DateMetric(label: String, value: String) { val divider = MaterialTheme.colorScheme.outline; Row(Modifier.fillMaxWidth().drawBehind { drawLine(divider, Offset(0f, size.height - 1.dp.toPx()), Offset(size.width, size.height - 1.dp.toPx()), 1f) }.padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp); Text(value, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 14.sp) } }

@Composable private fun BedtimeTool() { var wakeTime by remember { mutableStateOf("07:00") }; val bedtimes = remember(wakeTime) { runCatching { val p = wakeTime.split(":"); val wake = LocalTime.of(p[0].toInt(), p[1].toInt()); listOf(9, 8, 7).map { hours -> wake.minusHours(hours.toLong()).format(DateTimeFormatter.ofPattern("HH:mm")) to "${hours}h sleep window" } }.getOrDefault(emptyList()) }; Column { InstrumentTitle("BEDTIME", "Work back from a wake time."); OutlinedTextField(wakeTime, { wakeTime = it }, label = { Text("Wake time (HH:MM)") }, modifier = Modifier.fillMaxWidth(), singleLine = true); Text("These are planning windows, not health guidance.", modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp); Spacer(Modifier.height(11.dp)); if (bedtimes.isEmpty()) Text("Enter a valid 24-hour time.", color = MaterialTheme.colorScheme.primary) else bedtimes.forEach { (time, label) -> Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium).padding(17.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp); Text(time, fontFamily = FontFamily.Monospace, fontSize = 29.sp, fontWeight = FontWeight.Medium) }; Icon(Icons.Outlined.Schedule, null, tint = MaterialTheme.colorScheme.primary) }; Spacer(Modifier.height(8.dp)) } } }

@Composable private fun FocusTool(vm: ClockViewModel) { var duration by remember { mutableIntStateOf(25) }; Column { InstrumentTitle("FOCUS", "One interval. One commitment."); Text("Choose a focused working block. Starting it creates a persistent timer, visible even when the app is in the background.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 19.sp); Spacer(Modifier.height(18.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(25, 50, 90).forEach { minutes -> OutlinedButton(onClick = { duration = minutes }, modifier = Modifier.weight(1f)) { Text("$minutes min", color = if (duration == minutes) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) } } }; Spacer(Modifier.height(20.dp)); Button(onClick = { vm.createTimer("Focus · $duration min", duration) }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Begin $duration-minute focus") }; Spacer(Modifier.height(12.dp)); Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium).padding(16.dp)) { Text("A QUIET DEFAULT", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp); Text("The timer is created locally; no account or sync is required.", modifier = Modifier.padding(top = 7.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium) } } }
@Composable private fun InstrumentTitle(kicker: String, title: String) { Column(Modifier.padding(bottom = 14.dp)) { Text(kicker, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp); Text(title, fontSize = 22.sp, letterSpacing = (-1).sp, fontWeight = FontWeight.Black) } }

@Composable
fun SettingsScreen(vm: ClockViewModel) {
    val context = LocalContext.current; val scope = rememberCoroutineScope(); val settings by vm.settings.collectAsState(); val events by vm.events.collectAsState();
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if (uri != null) scope.launch { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(vm.backupJson()) }; Toast.makeText(context, "Backup exported", Toast.LENGTH_SHORT).show() } }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) { val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }; if (raw != null) vm.importBackup(raw) { ok -> Toast.makeText(context, if (ok) "Backup imported" else "Could not read that backup", Toast.LENGTH_SHORT).show() } } }
    LazyColumn(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
        item { SectionHeading("SETTINGS", "Make time yours.", "Preferences live on this device. Choose only the detail you need."); Spacer(Modifier.height(22.dp)); InstrumentTitle("APPEARANCE", "A considered surface") }
        item { SettingChoice("Theme", settings.theme.replaceFirstChar { it.titlecase() }, listOf("system", "light", "dark")) { choice -> vm.updateSettings { it.copy(theme = choice) } }; SettingChoice("Signal color", settings.accent.replaceFirstChar { it.titlecase() }, listOf("vermilion", "graphite", "ocean")) { choice -> vm.updateSettings { it.copy(accent = choice) } }; SettingSwitch("24-hour time", "Use 00:00 through 23:59 throughout the app.", settings.use24Hour) { checked -> vm.updateSettings { it.copy(use24Hour = checked) } }; SettingSwitch("Show seconds", "Show a live seconds readout on the clock.", settings.showSeconds) { checked -> vm.updateSettings { it.copy(showSeconds = checked) } }; Spacer(Modifier.height(22.dp)); InstrumentTitle("LOCAL DATA", "Keep a copy") }
        item { SettingsAction("Export backup", "Create a local JSON backup of alarms, timers, cities and preferences.", Icons.Outlined.Download) { exportLauncher.launch("premium-clock-backup.json") }; SettingsAction("Import backup", "Restore a previously exported local backup.", Icons.Outlined.FileOpen) { importLauncher.launch(arrayOf("application/json", "text/*")) }; Spacer(Modifier.height(22.dp)); InstrumentTitle("LOCAL INSIGHTS", "Your rhythm, on device") }
        item { InsightsPanel(events, onClear = vm::clearAnalytics); Spacer(Modifier.height(22.dp)); Text("Premium Clock · Offline-first", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold); Text("Exact alarms may require system approval in Android settings.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)) }
    }
}
@Composable private fun SettingChoice(label: String, selected: String, options: List<String>, onChange: (String) -> Unit) { Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp); Text(selected, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }; androidx.compose.foundation.lazy.LazyRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(options) { option -> TextButton(onClick = { onChange(option) }, modifier = Modifier.background(if (option == selected.lowercase()) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)) { Text(option.replaceFirstChar { it.titlecase() }, fontSize = 10.sp, fontWeight = FontWeight.Bold) } } }; HorizontalDivider(Modifier.padding(top = 11.dp)) } }
@Composable private fun SettingsAction(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) { val divider = MaterialTheme.colorScheme.outline; Row(Modifier.fillMaxWidth().clickable(onClick = onClick).drawBehind { drawLine(divider, Offset(0f, size.height - 1.dp.toPx()), Offset(size.width, size.height - 1.dp.toPx()), 1f) }.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, lineHeight = 15.sp) }; Icon(Icons.Outlined.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun InsightsPanel(events: List<TimeEventEntity>, onClear: () -> Unit) { val alarms = events.count { it.type.startsWith("alarm") }; val timers = events.count { it.type.startsWith("timer") }; Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium).padding(17.dp)) { Text("THIS DEVICE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp); Row(Modifier.fillMaxWidth().padding(top = 13.dp), horizontalArrangement = Arrangement.SpaceBetween) { InsightMetric("Alarm events", alarms.toString()); InsightMetric("Timer events", timers.toString()); InsightMetric("All events", events.size.toString()) }; TextButton(onClick = onClear, modifier = Modifier.align(Alignment.End)) { Text("Clear local history") } } }
@Composable private fun InsightMetric(label: String, value: String) { Column { Text(value, fontFamily = FontFamily.Monospace, fontSize = 24.sp, fontWeight = FontWeight.Medium); Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp) } }
