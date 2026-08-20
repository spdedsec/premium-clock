/* Design reminder — Chronographic Modernism: an alarm is an interruption, so this screen uses only the essential decision: snooze or dismiss. */
package com.premiumclock.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.premiumclock.app.PremiumClockApplication
import com.premiumclock.app.data.AlarmEntity
import com.premiumclock.app.data.TimeEventEntity
import com.premiumclock.app.services.NotificationHelper
import kotlinx.coroutines.launch

class AlarmRingActivity : ComponentActivity() {
    private var alarm by mutableStateOf<AlarmEntity?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getLongExtra("alarm_id", -1)
        val app = application as PremiumClockApplication
        lifecycleScope.launch { alarm = app.database.alarmDao().alarm(id) }
        setContent { PremiumClockTheme(dark = true) { val current = alarm; Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Text("ALARM", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp); Spacer(Modifier.height(18.dp)); Text(current?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "—:—", fontFamily = FontFamily.Monospace, fontSize = 86.sp, letterSpacing = (-6).sp); Text(current?.label ?: "Wake up", modifier = Modifier.padding(top = 8.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(66.dp)); OutlinedButton(onClick = { current?.let { item -> lifecycleScope.launch { app.alarmScheduler.snooze(item); app.database.eventDao().insert(TimeEventEntity(type = "alarm_snoozed", detail = item.label)); NotificationHelper.cancelAlarm(this@AlarmRingActivity, item.id); finish() } } }, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Snooze ${current?.snoozeMinutes ?: 9} minutes") }; Spacer(Modifier.height(12.dp)); Button(onClick = { current?.let { item -> lifecycleScope.launch { app.database.eventDao().insert(TimeEventEntity(type = "alarm_dismissed", detail = item.label)); NotificationHelper.cancelAlarm(this@AlarmRingActivity, item.id); finish() } } ?: finish() }, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Dismiss") }; Text("Keep your morning simple.", modifier = Modifier.padding(top = 18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center) } } }
    }
}
