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
        occurredAt: Long? = null
    ): ToolResult {
        Log.i("McpToolRegistry", "Executing tool: $toolName")
        return when (toolName) {
            "prepare_expense" -> tools.prepareExpense(amountMinor, category, note, sourceText, occurredAt)
            "prepare_multiple_expenses" -> tools.prepareMultipleExpenses(items, sourceText)
            "prepare_income" -> tools.prepareIncome(amountMinor, note, sourceText, occurredAt)
            "get_spending_summary" -> tools.getSpendingSummary()
            "get_item_total" -> tools.getItemOrCategoryTotal(query ?: note ?: "")
            "get_financial_balance" -> tools.getFinancialBalance()
            "get_highest_expense" -> tools.getHighestExpense()
            "get_period_summary" -> tools.getPeriodSummary(query ?: note ?: "today")
            "get_help_or_greeting" -> tools.getHelpOrGreeting(query?.contains("help", ignoreCase = true) == true || note?.contains("help", ignoreCase = true) == true)
            "search_transactions" -> tools.searchTransactions(query ?: "")
            "propose_reminder" -> tools.proposeReminder(hour, minute, note)
            "get_spending_patterns" -> tools.getSpendingPatterns()
            "seed_mock_transactions" -> tools.seedMockTransactions(1000)
            else -> ToolResult.PlainMessage("Unknown tool or unsupported action: $toolName")
        }
    }
}
