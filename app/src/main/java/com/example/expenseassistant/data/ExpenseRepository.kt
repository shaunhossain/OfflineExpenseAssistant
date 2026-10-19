package com.example.expenseassistant.data

import com.example.expenseassistant.domain.Money
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

data class SpendingSummary(
    val totalExpense: Money,
    val totalIncome: Money,
    val netBalance: Money,
    val transactionCount: Int,
    val categoryBreakdown: List<CategoryTotal>,
    val transactions: List<ExpenseEntity> = emptyList()
)

class ExpenseRepository(private val dao: ExpenseDao) {

    val allExpenses: Flow<List<ExpenseEntity>> = dao.getAllExpenses()
    val recentExpenses: Flow<List<ExpenseEntity>> = dao.getRecentExpenses(50)
    val allReminders: Flow<List<ReminderEntity>> = dao.getAllReminders()

    suspend fun saveExpense(
        amountMinor: Long,
        category: String,
        note: String,
        type: TransactionType = TransactionType.EXPENSE,
        occurredAt: Long = System.currentTimeMillis(),
        sourceText: String = "",
        needsReview: Boolean = false
    ): Long {
        val entity = ExpenseEntity(
            amountMinor = amountMinor,
            category = category,
            note = note,
            type = type.name,
            occurredAt = occurredAt,
            createdAt = System.currentTimeMillis(),
            sourceText = sourceText,
            needsReview = needsReview
        )
        return dao.insertExpense(entity)
    }

    suspend fun insertExpenses(expenses: List<ExpenseEntity>) = dao.insertExpenses(expenses)

    suspend fun deleteExpense(id: Long) = dao.deleteExpenseById(id)

    suspend fun updateExpense(expense: ExpenseEntity) = dao.updateExpense(expense)

    suspend fun search(query: String): List<ExpenseEntity> = dao.searchExpenses(query)

    suspend fun getMonthlySummary(calendar: Calendar = Calendar.getInstance()): SpendingSummary {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis

        cal.add(Calendar.MONTH, 1)
        val endTime = cal.timeInMillis - 1

        val expenses = dao.getExpensesBetweenSync(startTime, endTime)
        val expenseTotalMinor = dao.getTotalAmountBetween(startTime, endTime, TransactionType.EXPENSE.name)
        val incomeTotalMinor = dao.getTotalAmountBetween(startTime, endTime, TransactionType.INCOME.name)
        val categoryBreakdown = dao.getCategoryTotals(startTime, endTime, TransactionType.EXPENSE.name)

        return SpendingSummary(
            totalExpense = Money(expenseTotalMinor),
            totalIncome = Money(incomeTotalMinor),
            netBalance = Money(incomeTotalMinor - expenseTotalMinor),
            transactionCount = expenses.size,
            categoryBreakdown = categoryBreakdown,
            transactions = expenses
        )
    }

    suspend fun getTodaySummary(): SpendingSummary {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis

        cal.add(Calendar.DAY_OF_MONTH, 1)
        val endTime = cal.timeInMillis - 1

        val expenses = dao.getExpensesBetweenSync(startTime, endTime)
        val expenseTotalMinor = dao.getTotalAmountBetween(startTime, endTime, TransactionType.EXPENSE.name)
        val incomeTotalMinor = dao.getTotalAmountBetween(startTime, endTime, TransactionType.INCOME.name)
        val categoryBreakdown = dao.getCategoryTotals(startTime, endTime, TransactionType.EXPENSE.name)

        return SpendingSummary(
            totalExpense = Money(expenseTotalMinor),
            totalIncome = Money(incomeTotalMinor),
            netBalance = Money(incomeTotalMinor - expenseTotalMinor),
            transactionCount = expenses.size,
            categoryBreakdown = categoryBreakdown,
            transactions = expenses
        )
    }

    suspend fun getYesterdaySummary(): SpendingSummary {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis

        cal.add(Calendar.DAY_OF_MONTH, 1)
        val endTime = cal.timeInMillis - 1

        val expenses = dao.getExpensesBetweenSync(startTime, endTime)
        val expenseTotalMinor = dao.getTotalAmountBetween(startTime, endTime, TransactionType.EXPENSE.name)
        val incomeTotalMinor = dao.getTotalAmountBetween(startTime, endTime, TransactionType.INCOME.name)
        val categoryBreakdown = dao.getCategoryTotals(startTime, endTime, TransactionType.EXPENSE.name)

        return SpendingSummary(
            totalExpense = Money(expenseTotalMinor),
            totalIncome = Money(incomeTotalMinor),
            netBalance = Money(incomeTotalMinor - expenseTotalMinor),
            transactionCount = expenses.size,
            categoryBreakdown = categoryBreakdown,
            transactions = expenses
        )
    }

    suspend fun getAllExpenses(): List<ExpenseEntity> = dao.getAllExpensesSync()

    suspend fun saveReminder(
        title: String,
        message: String,
        hourOfDay: Int,
        minute: Int,
        isRecurringDaily: Boolean = true,
        reason: String = "User requested",
        categoryHint: String? = null
    ): Long {
        val reminder = ReminderEntity(
            title = title,
            message = message,
            hourOfDay = hourOfDay,
            minute = minute,
            isRecurringDaily = isRecurringDaily,
            isActive = true,
            reason = reason,
            categoryHint = categoryHint
        )
        return dao.insertReminder(reminder)
    }

    suspend fun toggleReminder(id: Long, isActive: Boolean) {
        val existing = dao.getReminderById(id) ?: return
        dao.updateReminder(existing.copy(isActive = isActive))
    }

    suspend fun deleteReminder(id: Long) = dao.deleteReminderById(id)

    suspend fun clearAllData() {
        dao.deleteAllExpenses()
        dao.deleteAllReminders()
    }
}
