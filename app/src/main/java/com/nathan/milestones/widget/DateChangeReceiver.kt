package com.nathan.milestones.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Runs at midnight (our alarm), after the phone reboots, and when the
 * time or time zone changes. Refreshes widgets and sets the next alarm.
 */
class DateChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WidgetUpdater.updateAll(context)
        WidgetUpdater.scheduleMidnightUpdate(context)
    }
}