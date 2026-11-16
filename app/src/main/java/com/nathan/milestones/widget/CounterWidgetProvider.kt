package com.nathan.milestones.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.nathan.milestones.data.CounterRepository

class CounterWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WidgetUpdater.updateWidget(context, manager, it) }
        WidgetUpdater.scheduleMidnightUpdate(context)
    }

    override fun onEnabled(context: Context) {
        WidgetUpdater.scheduleMidnightUpdate(context)
    }

    /** A widget was removed from the home screen: forget which date it showed. */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val repo = CounterRepository(context)
        appWidgetIds.forEach { repo.removeWidget(it) }
    }
}