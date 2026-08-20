package com.premiumclock.app.widgets

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.premiumclock.app.ui.MainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val WidgetPaper = ColorProvider(android.graphics.Color.rgb(244, 241, 234))
private val WidgetInk = ColorProvider(android.graphics.Color.rgb(29, 29, 27))
private val WidgetSignal = ColorProvider(android.graphics.Color.rgb(214, 71, 45))

abstract class InstrumentWidget(private val title: String, private val value: () -> String, private val detail: () -> String) : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent { Column(GlanceModifier.fillMaxSize().background(WidgetPaper).clickable(actionStartActivity<MainActivity>()).padding(16.dp), verticalAlignment = Alignment.Vertical.CenterVertically, horizontalAlignment = Alignment.Horizontal.Start) { Text(title.uppercase(), style = TextStyle(color = WidgetSignal, fontWeight = FontWeight.Bold, fontSize = 10.sp)); Text(value(), style = TextStyle(color = WidgetInk, fontStyle = FontStyle.Italic, fontSize = 31.sp)); Text(detail(), style = TextStyle(color = WidgetInk, fontSize = 11.sp)) } }
}
class ClockWidget : InstrumentWidget("Premium Clock", { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }, { SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date()) })
class AlarmWidget : InstrumentWidget("Next alarm", { "Open app" }, { "Set and review alarms" })
class TimerWidget : InstrumentWidget("Timer", { "Open app" }, { "Start a quick countdown" })
class StopwatchWidget : InstrumentWidget("Stopwatch", { "00:00.0" }, { "Tap to measure an interval" })
class ClockWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = ClockWidget() }
class AlarmWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = AlarmWidget() }
class TimerWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = TimerWidget() }
class StopwatchWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = StopwatchWidget() }
