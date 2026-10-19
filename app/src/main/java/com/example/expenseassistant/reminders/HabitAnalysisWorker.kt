package com.example.expenseassistant.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expenseassistant.data.AppDatabase
import com.example.expenseassistant.domain.SpendingAnalyzer

class HabitAnalysisWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val dao = database.expenseDao()
            val expenses = dao.getAllExpensesSync()

            val insights = SpendingAnalyzer.analyzeHabits(expenses)
            if (insights.isNotEmpty()) {
                val topInsight = insights.first()
                NotificationHelper.showNotification(
                    context = applicationContext,
                    notificationId = 1001,
                    title = topInsight.title,
                    message = topInsight.description
                )
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
