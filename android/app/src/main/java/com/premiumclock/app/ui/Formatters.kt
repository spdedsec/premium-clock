package com.premiumclock.app.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatDuration(millis: Long, tenths: Boolean = false): String {
    val value = millis.coerceAtLeast(0) / 1_000
    val core = if (value >= 3_600) String.format(Locale.US, "%02d:%02d:%02d", value / 3_600, value % 3_600 / 60, value % 60) else String.format(Locale.US, "%02d:%02d", value / 60, value % 60)
    return if (tenths) "$core.${(millis.coerceAtLeast(0) % 1_000) / 100}" else core
}
fun zoneTime(zoneId: String, use24Hour: Boolean): String = DateTimeFormatter.ofPattern(if (use24Hour) "HH:mm" else "h:mm a", Locale.getDefault()).format(Instant.now().atZone(ZoneId.of(zoneId)))
fun zoneDate(zoneId: String): String = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()).format(Instant.now().atZone(ZoneId.of(zoneId)))

