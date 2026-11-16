package com.nathan.milestones.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.nathan.milestones.MainActivity
import com.nathan.milestones.R
import com.nathan.milestones.data.CounterRepository
import com.nathan.milestones.logic.MilestoneCalculator
import com.nathan.milestones.model.CounterColors
import java.time.LocalDate
import java.time.ZoneId

object WidgetUpdater {

    const val ACTION_MIDNIGHT = "com.nathan.milestones.ACTION_MIDNIGHT"

    /** Refresh every Milestones widget on the home screen. */
    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, CounterWidgetProvider::class.java))
        ids.forEach { updateWidget(context, manager, it) }
    }

    fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        // RemoteViews = a layout that runs inside the home screen app, not inside our app
        val views = RemoteViews(context.packageName, R.layout.widget_counter)
        val counter = CounterRepository(context).getWidgetCounter(widgetId)

        if (counter == null) {
            // Not set up yet, or its date was deleted: tapping opens the picker
            views.setTextViewText(R.id.widget_title, "Milestones")
            views.setTextViewText(R.id.widget_headline, "Tap to choose a date")
            views.setViewVisibility(R.id.widget_celebration, View.GONE)
            views.setTextViewText(R.id.widget_detail, "")
            views.setInt(R.id.widget_bg, "setColorFilter", 0) // default pink
            val configIntent = Intent(context, WidgetConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(
                    context, widgetId, configIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        } else {
            val display = MilestoneCalculator.display(counter, LocalDate.now())
            views.setTextViewText(R.id.widget_title, "${counter.emoji} ${counter.title}")
            // Pink (0) keeps the gradient; other colours tint the background solid
            val tint = if (counter.colorIndex == 0) 0 else CounterColors.get(counter.colorIndex).start.toInt()
            views.setInt(R.id.widget_bg, "setColorFilter", tint)
            views.setTextViewText(R.id.widget_headline, display.headline)
            if (display.celebration != null) {
                views.setTextViewText(R.id.widget_celebration, "🎉 ${display.celebration}")
                views.setViewVisibility(R.id.widget_celebration, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_celebration, View.GONE)
            }
            views.setTextViewText(
                R.id.widget_detail,
                display.nextShort?.let { "Next: $it" } ?: display.detail
            )
            // Tapping the widget opens the app
            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(
                    context, widgetId, Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }
        manager.updateAppWidget(widgetId, views)
    }

    /**
     * Sets an alarm for just after midnight so widgets flip to the new day.
     * Inexact alarm = no special permission needed. It may fire a few minutes late, which is fine.
     */
    fun scheduleMidnightUpdate(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, DateChangeReceiver::class.java).setAction(ACTION_MIDNIGHT)
        val pending = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nextMidnight = LocalDate.now()
            .plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .plusSeconds(5)
            .toInstant()
            .toEpochMilli()
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, nextMidnight, pending)
    }
}