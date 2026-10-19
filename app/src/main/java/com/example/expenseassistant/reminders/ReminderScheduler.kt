package com.example.expenseassistant.reminders

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    private const val HABIT_WORK_NAME = "habit_analysis_work"

    fun scheduleHabitAnalysis(context: Context) {
        val habitRequest = PeriodicWorkRequestBuilder<HabitAnalysisWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            HABIT_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            habitRequest
        )
    }

    fun scheduleDailyReminder(
        context: Context,
        reminderId: Long,
        hourOfDay: Int,
        minute: Int,
        title: String,
        message: String
    ) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }

        val initialDelay = target.timeInMillis - now.timeInMillis

        val dailyRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag("reminder_$reminderId")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_reminder_$reminderId",
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyRequest
        )
    }

    fun cancelReminder(context: Context, reminderId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork("daily_reminder_$reminderId")
    }
}
