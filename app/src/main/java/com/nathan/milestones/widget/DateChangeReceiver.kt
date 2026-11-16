package com.nathan.milestones.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nathan.milestones.notify.MilestoneNotifier

/**
 * Runs at midnight (widget refresh), at 9 AM (notification check), after the phone
 * reboots, and when the time or time zone changes.
 */
class DateChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 9 AM alarm: check for milestones, then set tomorrow's alarm
        if (intent.action == MilestoneNotifier.ACTION_DAILY_CHECK) {
            MilestoneNotifier.runDailyCheck(context)
            MilestoneNotifier.scheduleDailyCheck(context)
            return
        }
        // Midnight, reboot, or time change: refresh widgets and reset both alarms
        WidgetUpdater.updateAll(context)
        WidgetUpdater.scheduleMidnightUpdate(context)
        MilestoneNotifier.scheduleDailyCheck(context)
    }
}