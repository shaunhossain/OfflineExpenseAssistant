package com.example.expenseassistant.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class CategoryTotal(
    val category: String,
    val totalMinor: Long,
    val count: Int
)

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>): List<Long>

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Query("SELECT * FROM expenses ORDER BY occurredAt DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY occurredAt DESC LIMIT :limit")
    fun getRecentExpenses(limit: Int = 30): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY occurredAt DESC")
    suspend fun getAllExpensesSync(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE occurredAt >= :startTime AND occurredAt <= :endTime ORDER BY occurredAt DESC")
    fun getExpensesBetween(startTime: Long, endTime: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE occurredAt >= :startTime AND occurredAt <= :endTime ORDER BY occurredAt DESC")
    suspend fun getExpensesBetweenSync(startTime: Long, endTime: Long): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE note LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY occurredAt DESC")
    suspend fun searchExpenses(query: String): List<ExpenseEntity>

    @Query("SELECT category, SUM(amountMinor) as totalMinor, COUNT(*) as count FROM expenses WHERE occurredAt >= :startTime AND occurredAt <= :endTime AND type = :type GROUP BY category ORDER BY totalMinor DESC")
    suspend fun getCategoryTotals(startTime: Long, endTime: Long, type: String = "EXPENSE"): List<CategoryTotal>

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM expenses WHERE occurredAt >= :startTime AND occurredAt <= :endTime AND type = :type")
    suspend fun getTotalAmountBetween(startTime: Long, endTime: Long, type: String): Long

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): ExpenseEntity?

    @Query("SELECT * FROM expenses ORDER BY occurredAt DESC, id DESC LIMIT 1")
    suspend fun getLatestExpense(): ExpenseEntity?

    @Query("SELECT * FROM expenses ORDER BY occurredAt DESC, id DESC LIMIT :limit")
    suspend fun getRecentExpensesSync(limit: Int = 10): List<ExpenseEntity>

    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun getExpenseCount(): Int

    @Query("SELECT COUNT(*) FROM expenses WHERE type = 'EXPENSE'")
    suspend fun getTotalExpenseTransactionCount(): Int

    @Query("SELECT COUNT(*) FROM expenses WHERE type = 'INCOME'")
    suspend fun getTotalIncomeTransactionCount(): Int

    @Query("SELECT MIN(occurredAt) FROM expenses")
    suspend fun getEarliestExpenseDate(): Long?

    @Query("SELECT MAX(occurredAt) FROM expenses")
    suspend fun getLatestExpenseDate(): Long?

    @Query("SELECT COUNT(*) FROM reminders")
    suspend fun getReminderCount(): Int

    // Reminders
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)

    @Query("SELECT * FROM reminders ORDER BY hourOfDay ASC, minute ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isActive = 1")
    suspend fun getActiveRemindersSync(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): ReminderEntity?

    @Query("DELETE FROM reminders")
    suspend fun deleteAllReminders()
}
