package com.example.expenseassistant

import android.app.Application
import com.example.expenseassistant.ai.GemmaEngine
import com.example.expenseassistant.data.AppDatabase
import com.example.expenseassistant.data.ExpenseRepository
import com.example.expenseassistant.reminders.NotificationHelper
import com.example.expenseassistant.reminders.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OfflineExpenseApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { ExpenseRepository(database.expenseDao()) }

    override fun onCreate() {
        super.onCreate()

        // 1. Setup notification channels
        NotificationHelper.createNotificationChannel(this)

        // 2. Schedule background habit analysis worker
        ReminderScheduler.scheduleHabitAnalysis(this)

        // 3. Warm up the local Gemma Engine asynchronously off the main thread
        applicationScope.launch {
            GemmaEngine.getInstance().initialize(this@OfflineExpenseApp)
        }
    }
}
