package com.example.expenseassistant.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expenseassistant.data.AppDatabase

class DailyReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val activeReminders = database.expenseDao().getActiveRemindersSync()

            if (activeReminders.isNotEmpty()) {
                val reminder = activeReminders.first()
                NotificationHelper.showNotification(
                    context = applicationContext,
                    notificationId = reminder.id.toInt(),
                    title = reminder.title,
                    message = reminder.message
                )
            } else {
                NotificationHelper.showNotification(
                    context = applicationContext,
                    notificationId = 999,
                    title = "Daily Expense Reminder",
                    message = "Don't forget to record today's expenses and bills!"
                )
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
