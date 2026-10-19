package com.example.expenseassistant.domain

import com.example.expenseassistant.data.ExpenseEntity
import java.util.Calendar

data class HabitInsight(
    val title: String,
    val description: String,
    val suggestedReminderHour: Int? = null,
    val suggestedReminderMinute: Int = 0,
    val categoryHint: String? = null
)

object SpendingAnalyzer {

    fun analyzeHabits(expenses: List<ExpenseEntity>): List<HabitInsight> {
        val insights = mutableListOf<HabitInsight>()
        if (expenses.isEmpty()) {
            insights.add(
                HabitInsight(
                    title = "Daily Evening Routine",
                    description = "Track your daily expenses every evening at 9:00 PM.",
                    suggestedReminderHour = 21,
                    suggestedReminderMinute = 0
                )
            )
            return insights
        }

        val cal = Calendar.getInstance()
        val hourDistribution = IntArray(24)
        val categoryCounts = mutableMapOf<String, Int>()

        expenses.forEach { exp ->
            cal.timeInMillis = exp.occurredAt
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hourDistribution[hour]++
            categoryCounts[exp.category] = (categoryCounts[exp.category] ?: 0) + 1
        }

        // Top category
        val topCategory = categoryCounts.maxByOrNull { it.value }
        if (topCategory != null && topCategory.value >= 3) {
            insights.add(
                HabitInsight(
                    title = "Frequent ${topCategory.key} Expenses",
                    description = "You've recorded ${topCategory.value} ${topCategory.key} transactions. Keeping an eye here helps balance your budget.",
                    categoryHint = topCategory.key
                )
            )
        }

        // Check evening activity (between 19:00 and 22:00)
        val eveningCount = hourDistribution.slice(19..22).sum()
        if (eveningCount >= 2) {
            insights.add(
                HabitInsight(
                    title = "Evening Spending Log",
                    description = "Many of your transactions happen in the evening. A 9:00 PM reminder can keep entries up to date.",
                    suggestedReminderHour = 21,
                    suggestedReminderMinute = 0
                )
            )
        }

        // Lunch activity (between 12:00 and 15:00)
        val lunchCount = hourDistribution.slice(12..15).sum()
        if (lunchCount >= 3) {
            insights.add(
                HabitInsight(
                    title = "Post-Lunch Tracking",
                    description = "Regular lunch expenses detected. Log meals right after lunch around 2:30 PM.",
                    suggestedReminderHour = 14,
                    suggestedReminderMinute = 30,
                    categoryHint = "Food"
                )
            )
        }

        return insights
    }
}
