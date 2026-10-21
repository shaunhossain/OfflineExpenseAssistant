package com.example.expenseassistant.mcp

import android.util.Log

class McpToolRegistry(private val tools: ExpenseTools) {

    suspend fun executeTool(
        toolName: String,
        amountMinor: Long? = null,
        category: String? = null,
        note: String? = null,
        sourceText: String = "",
        hour: Int? = null,
        minute: Int? = null,
        query: String? = null,
        items: List<SingleItemConfirmation> = emptyList(),
        occurredAt: Long? = null,
        id: Long? = null,
        limit: Int? = null,
        isActive: Boolean? = null
    ): ToolResult {
        Log.i("McpToolRegistry", "Executing tool: $toolName")
        return when (toolName) {
            "prepare_expense" -> tools.prepareExpense(amountMinor, category, note, sourceText, occurredAt)
            "prepare_multiple_expenses" -> tools.prepareMultipleExpenses(items, sourceText)
            "prepare_income" -> tools.prepareIncome(amountMinor, note, sourceText, occurredAt)
            "get_spending_summary" -> tools.getSpendingSummary()
            "get_financial_balance", "get_balance" -> tools.getFinancialBalance()
            "get_item_total" -> tools.getItemOrCategoryTotal(
                keyword = query ?: note ?: "",
                period = if (query != null && !note.isNullOrBlank()) note else null
            )
            "get_highest_expense" -> tools.getHighestExpense()
            "get_period_summary" -> tools.getPeriodSummary(query ?: note ?: "today")
            "get_help_or_greeting" -> tools.getHelpOrGreeting(query?.contains("help", ignoreCase = true) == true || note?.contains("help", ignoreCase = true) == true)
            "search_transactions", "search" -> tools.searchTransactions(query ?: "")
            "get_recent_transactions", "recent_transactions" -> tools.getRecentTransactions(limit ?: 10)
            "delete_transaction", "delete_transaction_by_id" -> tools.deleteTransaction(id)
            "delete_last_transaction" -> tools.deleteLastTransaction()
            "update_transaction" -> tools.updateTransaction(id, amountMinor, category, note, occurredAt)
            "propose_reminder", "set_reminder" -> tools.proposeReminder(hour, minute, note)
            "list_reminders" -> tools.listReminders()
            "delete_reminder" -> tools.deleteReminder(id)
            "toggle_reminder" -> tools.toggleReminder(id, isActive)
            "get_database_stats" -> tools.getDatabaseStats()
            "clear_all_transactions" -> tools.clearAllTransactions()
            "get_spending_patterns" -> tools.getSpendingPatterns()
            "seed_mock_transactions" -> tools.seedMockTransactions(1000)
            else -> ToolResult.PlainMessage("Unknown tool or unsupported action: $toolName")
        }
    }
}
