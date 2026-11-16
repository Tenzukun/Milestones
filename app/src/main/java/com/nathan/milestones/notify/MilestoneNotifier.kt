package com.nathan.milestones.notify

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.nathan.milestones.MainActivity
import com.nathan.milestones.R
import com.nathan.milestones.data.CounterRepository
import com.nathan.milestones.logic.MilestoneCalculator
import com.nathan.milestones.widget.DateChangeReceiver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Sends "🎉 Happy 2 months!" notifications.
 *
 * 1. An alarm goes off around 9:00 AM every day (scheduleDailyCheck).
 * 2. The alarm runs runDailyCheck, which looks at every date:
 *    - today is a milestone -> "🎉 Happy 2 months!"
 *    - tomorrow is a milestone (if "day before" is on) -> "Tomorrow: 3 months"
 * 3. It saves today's date so it never sends the same reminders twice in one day.
 */
object MilestoneNotifier {

    const val ACTION_DAILY_CHECK = "com.nathan.milestones.ACTION_DAILY_CHECK"
    private const val CHANNEL_ID = "milestones"
    private val CHECK_TIME: LocalTime = LocalTime.of(19, 34) // change this to pick a different time

    /** Android 8+ needs a "channel" before showing notifications. Safe to call many times. */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Milestones",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Anniversary and milestone reminders" }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /** Android 13+ needs the user to allow notifications. Older versions always allow them. */
    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

    /** Sets the alarm for the next 9:00 AM (today if it's still before 9, otherwise tomorrow). */
    fun scheduleDailyCheck(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, DateChangeReceiver::class.java).setAction(ACTION_DAILY_CHECK)
        val pending = PendingIntent.getBroadcast(
            context, 1, intent, // request code 1, so it doesn't replace the midnight alarm (0)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val now = LocalDateTime.now()
        var next = LocalDate.now().atTime(CHECK_TIME)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // Inexact alarm: no special permission needed, may arrive a few minutes late
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
    }

    fun runDailyCheck(context: Context) {
        val repo = CounterRepository(context)
        val today = LocalDate.now()

        // Only once per day, even if the alarm fires twice
        if (repo.getLastNotifyCheck() == today.toString()) return
        if (!hasPermission(context)) return
        createChannel(context)

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val tomorrow = today.plusDays(1)

        repo.getAll().filter { it.notify }.forEachIndexed { index, counter ->
            // Milestone today
            MilestoneCalculator.celebrationOn(counter, today)?.let { message ->
                manager.notify(
                    counter.id.hashCode(),
                    build(context, counter.title, "🎉 $message", index)
                )
            }
            // Heads-up for tomorrow
            if (counter.notifyDayBefore) {
                MilestoneCalculator.milestoneOn(counter.startDate, tomorrow, counter.unit)?.let { m ->
                    manager.notify(
                        counter.id.hashCode() + 1,
                        build(context, counter.title, "Tomorrow: ${m.label}! Time to plan something 💝", index)
                    )
                }
            }
        }
        repo.setLastNotifyCheck(today.toString())
    }

    private fun build(context: Context, title: String, text: String, index: Int): Notification {
        // Tapping the notification opens the app
        val open = PendingIntent.getActivity(
            context, 100 + index,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
    }
}