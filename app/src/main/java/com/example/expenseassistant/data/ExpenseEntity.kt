package com.example.expenseassistant.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.expenseassistant.domain.Money

enum class TransactionType {
    EXPENSE,
    INCOME
}

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amountMinor: Long,
    val currency: String = "BDT",
    val type: String = TransactionType.EXPENSE.name,
    val category: String,
    val note: String,
    val occurredAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val sourceText: String = "",
    val needsReview: Boolean = false
) {
    val money: Money
        get() = Money(amountMinor, currency)

    val isExpense: Boolean
        get() = type == TransactionType.EXPENSE.name

    val isIncome: Boolean
        get() = type == TransactionType.INCOME.name
}
