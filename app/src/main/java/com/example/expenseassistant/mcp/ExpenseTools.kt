package com.example.expenseassistant.mcp

import com.example.expenseassistant.data.ExpenseEntity
import com.example.expenseassistant.data.ExpenseRepository
import com.example.expenseassistant.data.SpendingSummary
import com.example.expenseassistant.data.TransactionType
import com.example.expenseassistant.domain.HabitInsight
import com.example.expenseassistant.domain.Money
import com.example.expenseassistant.domain.SpendingAnalyzer
import com.example.expenseassistant.domain.TransactionValidator
import com.example.expenseassistant.domain.ValidationResult
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class SingleItemConfirmation(
    val amountMinor: Long,
    val category: String,
    val note: String,
    val type: TransactionType = TransactionType.EXPENSE,
    val occurredAt: Long? = null
)

sealed interface ToolResult {
    data class RequireConfirmation(
        val validationResult: ValidationResult,
        val proposedType: TransactionType,
        val sourceText: String
    ) : ToolResult

    data class RequireMultipleConfirmation(
        val items: List<SingleItemConfirmation>,
        val totalAmountMinor: Long,
        val sourceText: String
    ) : ToolResult

    data class RequireReminderConfirmation(
        val title: String,
        val message: String,
        val hourOfDay: Int,
        val minute: Int,
        val reason: String
    ) : ToolResult

    data class SummaryResult(
        val summary: SpendingSummary,
        val message: String
    ) : ToolResult

    data class SearchResults(
        val query: String,
        val results: List<com.example.expenseassistant.data.ExpenseEntity>
    ) : ToolResult

    data class HabitPatternsResult(
        val insights: List<HabitInsight>
    ) : ToolResult

    data class RequireDeleteConfirmation(
        val transactionId: Long,
        val itemDescription: String,
        val amountMinor: Long,
        val message: String
    ) : ToolResult

    data class RequireClearConfirmation(
        val message: String
    ) : ToolResult

    data class DatabaseStatsResult(
        val stats: com.example.expenseassistant.data.DatabaseStats,
        val message: String
    ) : ToolResult

    data class ReminderListResult(
        val reminders: List<com.example.expenseassistant.data.ReminderEntity>,
        val message: String
    ) : ToolResult

    data class RecentTransactionsResult(
        val transactions: List<com.example.expenseassistant.data.ExpenseEntity>,
        val message: String
    ) : ToolResult

    data class PlainMessage(
        val message: String
    ) : ToolResult
}

data class ToolDefinition(
    val name: String,
    val description: String,
    val parametersSchema: String
)

class ExpenseTools(private val repository: ExpenseRepository) {

    val availableTools: List<ToolDefinition> = listOf(
        ToolDefinition(
            name = "prepare_expense",
            description = "Validates and prepares an expense for user confirmation before saving.",
            parametersSchema = """{"amountMinor": "Long", "category": "String", "note": "String"}"""
        ),
        ToolDefinition(
            name = "prepare_multiple_expenses",
            description = "Validates and prepares multiple expenses for user confirmation before saving.",
            parametersSchema = """{"items": "List"}"""
        ),
        ToolDefinition(
            name = "prepare_income",
            description = "Validates and prepares an income entry for user confirmation before saving.",
            parametersSchema = """{"amountMinor": "Long", "note": "String"}"""
        ),
        ToolDefinition(
            name = "get_spending_summary",
            description = "Fetches current month deterministic total expense, income, and category breakdown.",
            parametersSchema = """{}"""
        ),
        ToolDefinition(
            name = "get_item_total",
            description = "Calculates total amount spent on a specific item or category, with optional timeframe filter.",
            parametersSchema = """{"query": "String", "period": "String?"}"""
        ),
        ToolDefinition(
            name = "get_financial_balance",
            description = "Calculates current monthly balance, income, savings rate, and expense totals.",
            parametersSchema = """{}"""
        ),
        ToolDefinition(
            name = "get_highest_expense",
            description = "Retrieves the single largest expense recorded in the current month.",
            parametersSchema = """{}"""
        ),
        ToolDefinition(
            name = "get_period_summary",
            description = "Fetches itemized transaction summary for a specified period (today, yesterday, week).",
            parametersSchema = """{"period": "String"}"""
        ),
        ToolDefinition(
            name = "search_transactions",
            description = "Searches persisted transactions by keyword or category.",
            parametersSchema = """{"query": "String"}"""
        ),
        ToolDefinition(
            name = "get_recent_transactions",
            description = "Retrieves the latest transactions with their unique IDs, notes, categories, and amounts.",
            parametersSchema = """{"limit": "Int?"}"""
        ),
        ToolDefinition(
            name = "delete_transaction",
            description = "Prepares confirmation to delete a specific transaction by its unique ID.",
            parametersSchema = """{"id": "Long"}"""
        ),
        ToolDefinition(
            name = "delete_last_transaction",
            description = "Prepares confirmation to delete the most recently entered transaction.",
            parametersSchema = """{}"""
        ),
        ToolDefinition(
            name = "update_transaction",
            description = "Updates an existing transaction record by ID.",
            parametersSchema = """{"id": "Long", "amountMinor": "Long?", "category": "String?", "note": "String?"}"""
        ),
        ToolDefinition(
            name = "propose_reminder",
            description = "Proposes a daily or one-off expense reminder for user confirmation.",
            parametersSchema = """{"hour": "Int", "minute": "Int", "message": "String"}"""
        ),
        ToolDefinition(
            name = "list_reminders",
            description = "Lists all configured reminders with IDs, scheduled times, and active statuses.",
            parametersSchema = """{}"""
        ),
        ToolDefinition(
            name = "delete_reminder",
            description = "Deletes a scheduled reminder by its ID.",
            parametersSchema = """{"id": "Long"}"""
        ),
        ToolDefinition(
            name = "toggle_reminder",
            description = "Pauses or activates an existing reminder by ID.",
            parametersSchema = """{"id": "Long", "isActive": "Boolean"}"""
        ),
        ToolDefinition(
            name = "get_database_stats",
            description = "Fetches aggregate database telemetry including total transactions, counts, and date range.",
            parametersSchema = """{}"""
        ),
        ToolDefinition(
            name = "clear_all_transactions",
            description = "Prepares a confirmation request to delete all recorded transactions.",
            parametersSchema = """{}"""
        ),
        ToolDefinition(
            name = "get_spending_patterns",
            description = "Analyzes local database for spending habits and recommended reminders.",
            parametersSchema = """{}"""
        ),
        ToolDefinition(
            name = "seed_mock_transactions",
            description = "Seeds realistic categorized mock transactions across the last 6 months into the database.",
            parametersSchema = """{"count": "Int"}"""
        )
    )

    fun prepareExpense(
        amountMinor: Long?,
        category: String?,
        note: String?,
        sourceText: String,
        occurredAt: Long? = null
    ): ToolResult {
        val validation = TransactionValidator.validate(
            rawAmountMinor = amountMinor,
            rawCategory = category,
            rawNote = note,
            intentType = "EXPENSE",
            occurredAt = occurredAt
        )
        return ToolResult.RequireConfirmation(
            validationResult = validation,
            proposedType = TransactionType.EXPENSE,
            sourceText = sourceText
        )
    }

    fun prepareMultipleExpenses(
        items: List<SingleItemConfirmation>,
        sourceText: String
    ): ToolResult {
        val total = items.sumOf { it.amountMinor }
        return ToolResult.RequireMultipleConfirmation(
            items = items,
            totalAmountMinor = total,
            sourceText = sourceText
        )
    }

    fun prepareIncome(
        amountMinor: Long?,
        note: String?,
        sourceText: String,
        occurredAt: Long? = null
    ): ToolResult {
        val validation = TransactionValidator.validate(
            rawAmountMinor = amountMinor,
            rawCategory = "Salary",
            rawNote = note,
            intentType = "INCOME",
            occurredAt = occurredAt
        )
        return ToolResult.RequireConfirmation(
            validationResult = validation,
            proposedType = TransactionType.INCOME,
            sourceText = sourceText
        )
    }

    suspend fun getSpendingSummary(): ToolResult {
        val summary = repository.getMonthlySummary()
        val allExpenses = repository.getAllExpenses()
        val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())

        val sb = StringBuilder()
        sb.append("📅 Full Month Overview ($monthName)\n\n")
        sb.append("• Total Expense: ${summary.totalExpense.toFormattedString()}\n")
        sb.append("• Total Income: ${summary.totalIncome.toFormattedString()}\n")
        sb.append("• Net Balance: ${summary.netBalance.toFormattedString()}\n")
        sb.append("• This Month: ${summary.transactionCount} transactions\n")
        sb.append("• Total in Database: ${allExpenses.size} transactions across all months\n")

        if (summary.categoryBreakdown.isNotEmpty()) {
            sb.append("\n📊 Category Breakdown:\n")
            summary.categoryBreakdown.forEach { cat ->
                val catMoney = Money(cat.totalMinor).toFormattedString()
                sb.append("• ${cat.category}: $catMoney (${cat.count} tx)\n")
            }
        }

        if (summary.transactions.isNotEmpty()) {
            sb.append("\n📝 Recent Transactions:\n")
            val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            summary.transactions.take(15).forEachIndexed { idx, tx ->
                val amountStr = Money(tx.amountMinor).toFormattedString()
                val dateStr = dateFormat.format(Date(tx.occurredAt))
                val sign = if (tx.type == TransactionType.INCOME.name) "+" else "-"
                sb.append("${idx + 1}. $sign$amountStr • ${tx.note} [${tx.category}] ($dateStr)\n")
            }
            if (summary.transactions.size > 15) {
                sb.append("... and ${summary.transactions.size - 15} more in the Analytics tab.\n")
            }
        } else {
            sb.append("\nNo transactions recorded yet this month.\nTry entering an expense like: '50 tk sosage, 320 tk murgi'.")
        }

        return ToolResult.SummaryResult(summary = summary, message = sb.toString().trim())
    }

    suspend fun searchTransactions(query: String): ToolResult {
        val list = repository.search(query)
        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val msg = if (list.isNotEmpty()) {
            val total = Money(list.sumOf { it.amountMinor }).toFormattedString()
            val items = list.take(10).mapIndexed { idx, tx ->
                "${idx + 1}. ${Money(tx.amountMinor).toFormattedString()} • ${tx.note} [${tx.category}] (${dateFormat.format(Date(tx.occurredAt))})"
            }.joinToString("\n")
            "Found ${list.size} transaction(s) matching '$query' (Total: $total):\n$items"
        } else {
            "No transactions found matching '$query'."
        }
        return ToolResult.SearchResults(query = query, results = list)
    }

    fun proposeReminder(
        hour: Int?,
        minute: Int?,
        message: String?
    ): ToolResult {
        val h = hour ?: 21
        val m = minute ?: 0
        val msg = message ?: "Time to record today's expenses!"
        return ToolResult.RequireReminderConfirmation(
            title = "Expense Reminder",
            message = msg,
            hourOfDay = h,
            minute = m,
            reason = "Scheduled via assistant"
        )
    }

    suspend fun getSpendingPatterns(): ToolResult {
        val expenses = repository.getMonthlySummary()
        // Fetch all recent expenses to analyze
        val list = repository.search("")
        val insights = SpendingAnalyzer.analyzeHabits(list)
        return ToolResult.HabitPatternsResult(insights)
    }

    suspend fun getItemOrCategoryTotal(keyword: String, period: String? = null): ToolResult {
        val cleanKeyword = keyword.trim().lowercase()
        if (cleanKeyword.isBlank()) {
            return getSpendingSummary()
        }

        val allExpenses = repository.getAllExpenses()
        val mappedCategory = TransactionValidator.mapToValidCategory(null, cleanKeyword, TransactionType.EXPENSE)

        val words = cleanKeyword.split(Regex("[\\s,]+")).filter { it.length > 1 }
        val matches = allExpenses.filter { entity ->
            val noteLower = entity.note.lowercase()
            val catLower = entity.category.lowercase()
            words.any { word -> noteLower.contains(word) || catLower.contains(word) } ||
                    cleanKeyword.contains(catLower) ||
                    (mappedCategory != "Other" && entity.category.equals(mappedCategory, ignoreCase = true))
        }

        if (matches.isEmpty()) {
            val message = "No transactions found matching '$keyword' yet.\nYou can record an expense by typing e.g. '50 tk $keyword'."
            return ToolResult.PlainMessage(message)
        }

        // Apply period filter if specified
        val (filteredMatches, periodLabel) = when {
            period?.contains("week", ignoreCase = true) == true || period?.contains("shoptah", ignoreCase = true) == true -> {
                val cal = Calendar.getInstance()
                cal.firstDayOfWeek = Calendar.MONDAY
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                if (cal.timeInMillis > System.currentTimeMillis()) {
                    cal.add(Calendar.DAY_OF_MONTH, -7)
                }
                val startOfWeek = cal.timeInMillis
                Pair(matches.filter { it.occurredAt >= startOfWeek }, "this week")
            }
            period?.contains("month", ignoreCase = true) == true || period?.contains("mash", ignoreCase = true) == true -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(matches.filter { it.occurredAt >= cal.timeInMillis }, "this month")
            }
            period?.contains("today", ignoreCase = true) == true || period?.contains("aj", ignoreCase = true) == true -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(matches.filter { it.occurredAt >= cal.timeInMillis }, "today")
            }
            else -> Pair(matches, null)
        }

        val icon = when {
            cleanKeyword.contains("bus") || cleanKeyword.contains("vara") || cleanKeyword.contains("rickshaw") || cleanKeyword.contains("transport") -> "🚌"
            cleanKeyword.contains("bazar") || cleanKeyword.contains("grocery") || cleanKeyword.contains("murgi") -> "🛒"
            cleanKeyword.contains("cha") || cleanKeyword.contains("tea") || cleanKeyword.contains("coffee") -> "☕"
            cleanKeyword.contains("food") || cleanKeyword.contains("lunch") || cleanKeyword.contains("dinner") || cleanKeyword.contains("nasta") -> "🍽️"
            cleanKeyword.contains("medicine") || cleanKeyword.contains("health") || cleanKeyword.contains("doctor") -> "💊"
            cleanKeyword.contains("bill") || cleanKeyword.contains("recharge") || cleanKeyword.contains("wifi") -> "📱"
            cleanKeyword.contains("shopping") || cleanKeyword.contains("cloth") -> "🛍️"
            else -> "📊"
        }

        if (periodLabel != null && filteredMatches.isEmpty()) {
            val allTimeTotal = Money(matches.sumOf { it.amountMinor }).toFormattedString()
            val msg = "$icon No expenses found for '$keyword' $periodLabel.\n(All-time total: $allTimeTotal across ${matches.size} transactions)"
            return ToolResult.PlainMessage(msg)
        }

        val totalMinor = filteredMatches.sumOf { it.amountMinor }
        val totalFormatted = Money(totalMinor).toFormattedString()
        val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

        val sb = StringBuilder()
        val titleLabel = if (periodLabel != null) " ($periodLabel)" else ""
        sb.append("$icon Total for '$keyword'$titleLabel: $totalFormatted (${filteredMatches.size} transactions)\n\n")
        sb.append("Recent matching expenses:\n")
        filteredMatches.take(8).forEachIndexed { idx, tx ->
            val dateStr = dateFormat.format(Date(tx.occurredAt))
            sb.append("${idx + 1}. ${Money(tx.amountMinor).toFormattedString()} • ${tx.note} [${tx.category}] ($dateStr)\n")
        }
        if (filteredMatches.size > 8) {
            sb.append("... and ${filteredMatches.size - 8} more transactions.\n")
        }

        return ToolResult.PlainMessage(sb.toString().trim())
    }

    suspend fun getFinancialBalance(): ToolResult {
        val summary = repository.getMonthlySummary()
        val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
        val savingsRateStr = if (summary.totalIncome.amountMinor > 0) {
            val rate = ((summary.netBalance.amountMinor.toDouble() / summary.totalIncome.amountMinor.toDouble()) * 100).coerceAtLeast(0.0)
            String.format(Locale.US, "%.1f%%", rate)
        } else {
            "N/A"
        }
        val msg = """
💰 Financial Balance & Savings ($monthName):
• Total Income: ${summary.totalIncome.toFormattedString()}
• Total Expenses: ${summary.totalExpense.toFormattedString()}
• Net Remaining / Savings: ${summary.netBalance.toFormattedString()}
• Savings Rate: $savingsRateStr
Across ${summary.transactionCount} transactions this month.
        """.trimIndent()
        return ToolResult.PlainMessage(msg)
    }

    suspend fun getHighestExpense(): ToolResult {
        val summary = repository.getMonthlySummary()
        val highest = summary.transactions
            .filter { it.isExpense }
            .maxByOrNull { it.amountMinor }
        if (highest == null) {
            return ToolResult.PlainMessage("No expenses recorded yet this month.")
        }
        val dateStr = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(highest.occurredAt))
        val msg = """
🏆 Highest Expense This Month:
• Amount: ${Money(highest.amountMinor).toFormattedString()}
• Note: ${highest.note}
• Category: ${highest.category}
• Date: $dateStr
        """.trimIndent()
        return ToolResult.PlainMessage(msg)
    }

    suspend fun getPeriodSummary(period: String): ToolResult {
        val isToday = period.lowercase().contains("today") || period.lowercase().contains("aj")
        val isYesterday = period.lowercase().contains("yesterday") || period.lowercase().contains("gotokal")
        val isWeek = period.lowercase().contains("week") || period.lowercase().contains("shoptah")

        val summary = when {
            isToday -> repository.getTodaySummary()
            isYesterday -> repository.getYesterdaySummary()
            isWeek -> repository.getThisWeekSummary()
            else -> repository.getTodaySummary()
        }
        val title = when {
            isToday -> "📅 Today's Summary"
            isYesterday -> "📅 Yesterday's Summary"
            isWeek -> "📅 This Week's Summary"
            else -> "📅 Spending Summary"
        }

        if (summary.transactions.isEmpty()) {
            return ToolResult.PlainMessage("$title:\nNo transactions recorded for this period.")
        }

        val sb = StringBuilder()
        sb.append("$title:\n")
        sb.append("• Total Spent: ${summary.totalExpense.toFormattedString()} (${summary.transactionCount} tx)\n")
        if (summary.totalIncome.amountMinor > 0) {
            sb.append("• Income: ${summary.totalIncome.toFormattedString()}\n")
        }
        sb.append("\nTransactions:\n")
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        summary.transactions.forEachIndexed { idx, tx ->
            val sign = if (tx.type == TransactionType.INCOME.name) "+" else "-"
            sb.append("${idx + 1}. $sign${Money(tx.amountMinor).toFormattedString()} • ${tx.note} [${tx.category}] (${timeFormat.format(Date(tx.occurredAt))})\n")
        }

        return ToolResult.PlainMessage(sb.toString().trim())
    }

    fun getHelpOrGreeting(isHelp: Boolean): ToolResult {
        val msg = """
Hello! 👋 I'm your offline Gemma Expense Assistant.
I understand natural English and Banglish!

💡 Things you can ask me:
• Add single expense: "50 tk bus vara" or "lunch 250 tk"
• Add multiple expenses: "50 tk sosage, 320 tk murgi, 420 tk bazar"
• Natural sentence: "ami 200 tk diye lunch korsi"
• Check specific totals: "total bus vara koto" or "bazar e koto khoroch"
• Check balance: "amar balance koto"
• Today's hisab: "ajke koto khoroch hoise"
• Full overview: "full month data"
• Highest expense: "highest expense konta"
• Reminders: "raat 9 tay reminder dio"
        """.trimIndent()
        return ToolResult.PlainMessage(msg)
    }

    suspend fun seedMockTransactions(count: Int = 1000): ToolResult {
        val random = java.util.Random()
        val nowMs = System.currentTimeMillis()
        val sixMonthsMs = 180L * 24 * 60 * 60 * 1000
        val startMs = nowMs - sixMonthsMs

        val categoriesAndItems = mapOf(
            "Food" to listOf(
                Pair("cha ar biscuit", 15 to 40),
                Pair("shokaler nasta (paratha, dim, cha)", 60 to 120),
                Pair("dupure lunch (bhat, mach, dal)", 120 to 250),
                Pair("raate dinner", 100 to 280),
                Pair("biryani treat with friends", 250 to 600),
                Pair("coffee and snacks", 120 to 350),
                Pair("burger ar fries", 180 to 450),
                Pair("afternoon nasta (singara, somosa)", 30 to 80),
                Pair("office canteen lunch", 80 to 150),
                Pair("sosage and eggs", 100 to 250)
            ),
            "Transport" to listOf(
                Pair("bus vara office to basha", 25 to 50),
                Pair("rickshaw vara to metro station", 40 to 80),
                Pair("metro rail fare", 60 to 100),
                Pair("cng fare to Dhanmondi", 200 to 350),
                Pair("uber ride to Gulshan", 250 to 550),
                Pair("pathao bike ride", 70 to 180),
                Pair("rickshaw vara local market", 30 to 60)
            ),
            "Grocery" to listOf(
                Pair("shobji bazar (alu, potol, tomato)", 150 to 450),
                Pair("chal and dal 5kg", 450 to 950),
                Pair("tel (soyabean oil 2L)", 350 to 420),
                Pair("dim 1 dozen", 140 to 170),
                Pair("murgi (chicken 1kg)", 220 to 380),
                Pair("macher bazar (rui mach)", 350 to 800),
                Pair("dudh and pauruti", 110 to 220)
            ),
            "Bills" to listOf(
                Pair("electricity bill", 1200 to 3500),
                Pair("wifi broadband bill", 800 to 1500),
                Pair("gas bill", 1080 to 1080),
                Pair("mobile recharge", 100 to 500)
            ),
            "Shopping" to listOf(
                Pair("formal shirt for office", 1200 to 2800),
                Pair("jeans pant", 1500 to 3200),
                Pair("shoes / sneaker", 1800 to 4500),
                Pair("daraz parcel", 450 to 1800)
            ),
            "Health" to listOf(
                Pair("napa extra, seclo and vitamins", 150 to 650),
                Pair("prescription medicines", 450 to 2200),
                Pair("doctor consultation fee", 800 to 1500)
            ),
            "Entertainment" to listOf(
                Pair("Star Cineplex movie ticket", 400 to 900),
                Pair("popcorn and drink", 250 to 500),
                Pair("weekend hangout", 400 to 1200)
            ),
            "Education" to listOf(
                Pair("Nilkhet boi shopping", 400 to 1500),
                Pair("stationery, khata, pen", 120 to 400)
            )
        )

        val incomeItems = listOf(
            Pair("Monthly Salary credited", 65000 to 85000),
            Pair("Performance bonus", 10000 to 25000),
            Pair("Freelance project payment", 12000 to 30000),
            Pair("Mobile banking cashback", 100 to 500)
        )

        val list = mutableListOf<ExpenseEntity>()
        val incomeCount = (count * 0.06).toInt()
        val expenseCount = count - incomeCount

        for (i in 0 until incomeCount) {
            val (name, range) = incomeItems[random.nextInt(incomeItems.size)]
            val amountTk = range.first + random.nextInt(range.second - range.first + 1)
            val occurredAt = startMs + (random.nextDouble() * (nowMs - startMs)).toLong()
            list.add(
                ExpenseEntity(
                    amountMinor = amountTk.toLong() * 100L,
                    currency = "BDT",
                    type = TransactionType.INCOME.name,
                    category = "Salary",
                    note = name,
                    occurredAt = occurredAt,
                    createdAt = occurredAt + random.nextInt(60000),
                    sourceText = "$amountTk tk $name",
                    needsReview = false
                )
            )
        }

        val categories = categoriesAndItems.keys.toList()
        for (i in 0 until expenseCount) {
            val cat = categories[random.nextInt(categories.size)]
            val items = categoriesAndItems[cat]!!
            val (name, range) = items[random.nextInt(items.size)]
            val amountTk = range.first + random.nextInt(range.second - range.first + 1)
            val occurredAt = startMs + (random.nextDouble() * (nowMs - startMs)).toLong()
            list.add(
                ExpenseEntity(
                    amountMinor = amountTk.toLong() * 100L,
                    currency = "BDT",
                    type = TransactionType.EXPENSE.name,
                    category = cat,
                    note = name,
                    occurredAt = occurredAt,
                    createdAt = occurredAt + random.nextInt(60000),
                    sourceText = "$amountTk tk $name",
                    needsReview = false
                )
            )
        }

        repository.insertExpenses(list)
        val allExpenses = repository.getAllExpenses()

        val msg = "✓ Successfully pushed $count realistic mock transactions into the local SQLite database!\n\n" +
                "• Date Range: Last 6 months\n" +
                "• Categories: Food, Transport, Grocery, Bills, Shopping, Health, Salary\n" +
                "• Total in Database now: ${allExpenses.size} transactions.\n\n" +
                "You can see all of them in the Analytics tab or ask 'full month data'."

        return ToolResult.PlainMessage(msg)
    }

    suspend fun getRecentTransactions(limit: Int = 10): ToolResult {
        val list = repository.getRecentExpensesSync(limit)
        if (list.isEmpty()) {
            return ToolResult.PlainMessage("No recent transactions found.")
        }
        val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("📋 Recent Transactions (${list.size}):\n")
        list.forEachIndexed { idx, tx ->
            val amountStr = Money(tx.amountMinor).toFormattedString()
            val sign = if (tx.type == TransactionType.INCOME.name) "+" else "-"
            val dateStr = dateFormat.format(Date(tx.occurredAt))
            sb.append("${idx + 1}. [ID: ${tx.id}] $sign$amountStr • ${tx.note} [${tx.category}] ($dateStr)\n")
        }
        return ToolResult.RecentTransactionsResult(list, sb.toString().trim())
    }

    suspend fun deleteTransaction(id: Long?): ToolResult {
        if (id == null) {
            return ToolResult.PlainMessage("Please specify a transaction ID to delete (e.g. 'delete transaction 5').")
        }
        val tx = repository.getExpenseById(id)
            ?: return ToolResult.PlainMessage("Transaction with ID #$id was not found. Try 'recent transactions dekhao' to see valid IDs.")
        val amountStr = Money(tx.amountMinor).toFormattedString()
        return ToolResult.RequireDeleteConfirmation(
            transactionId = tx.id,
            itemDescription = "${tx.note} [${tx.category}]",
            amountMinor = tx.amountMinor,
            message = "Are you sure you want to delete transaction #$id: '${tx.note}' ($amountStr)?"
        )
    }

    suspend fun deleteLastTransaction(): ToolResult {
        val tx = repository.getLatestExpense()
            ?: return ToolResult.PlainMessage("No transactions found to delete.")
        val amountStr = Money(tx.amountMinor).toFormattedString()
        val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        val dateStr = dateFormat.format(Date(tx.occurredAt))
        return ToolResult.RequireDeleteConfirmation(
            transactionId = tx.id,
            itemDescription = "${tx.note} [${tx.category}]",
            amountMinor = tx.amountMinor,
            message = "Are you sure you want to delete your last transaction: '${tx.note}' ($amountStr) from $dateStr?"
        )
    }

    suspend fun updateTransaction(
        id: Long?,
        amountMinor: Long? = null,
        category: String? = null,
        note: String? = null,
        occurredAt: Long? = null
    ): ToolResult {
        if (id == null) {
            return ToolResult.PlainMessage("Please provide the transaction ID to update.")
        }
        val existing = repository.getExpenseById(id)
            ?: return ToolResult.PlainMessage("Transaction with ID #$id was not found.")

        val newNote = note?.takeIf { it.isNotBlank() } ?: existing.note
        val newAmount = if (amountMinor != null && amountMinor > 0) amountMinor else existing.amountMinor
        val newCat = category?.takeIf { it.isNotBlank() } ?: existing.category
        val newOccurred = occurredAt ?: existing.occurredAt

        val updated = existing.copy(
            amountMinor = newAmount,
            category = newCat,
            note = newNote,
            occurredAt = newOccurred
        )
        repository.updateExpense(updated)
        val formattedAmount = Money(newAmount).toFormattedString()
        return ToolResult.PlainMessage("✅ Updated transaction #$id: '$newNote' ($formattedAmount) [${newCat}]")
    }

    suspend fun listReminders(): ToolResult {
        val list = repository.getAllRemindersSync()
        if (list.isEmpty()) {
            return ToolResult.PlainMessage("No expense reminders configured yet. You can set one by typing 'raat 9 tay reminder dio'.")
        }
        val sb = StringBuilder()
        sb.append("⏰ Scheduled Reminders (${list.size}):\n")
        list.forEachIndexed { idx, rem ->
            val status = if (rem.isActive) "Active" else "Paused"
            val hour12 = if (rem.hourOfDay == 0) 12 else if (rem.hourOfDay > 12) rem.hourOfDay - 12 else rem.hourOfDay
            val amPm = if (rem.hourOfDay < 12) "AM" else "PM"
            val timeStr = String.format(Locale.getDefault(), "%02d:%02d %s", hour12, rem.minute, amPm)
            sb.append("${idx + 1}. [ID: ${rem.id}] $timeStr • \"${rem.message}\" ($status)\n")
        }
        return ToolResult.ReminderListResult(list, sb.toString().trim())
    }

    suspend fun deleteReminder(id: Long?): ToolResult {
        if (id == null) {
            return ToolResult.PlainMessage("Please specify a reminder ID to delete (e.g. 'delete reminder 1').")
        }
        repository.deleteReminder(id)
        return ToolResult.PlainMessage("🗑️ Deleted reminder #$id.")
    }

    suspend fun toggleReminder(id: Long?, isActive: Boolean?): ToolResult {
        if (id == null) {
            return ToolResult.PlainMessage("Please specify a reminder ID to toggle.")
        }
        val targetActive = isActive ?: true
        repository.toggleReminder(id, targetActive)
        val statusText = if (targetActive) "Active" else "Paused"
        return ToolResult.PlainMessage("⏰ Reminder #$id is now $statusText.")
    }

    suspend fun getDatabaseStats(): ToolResult {
        val stats = repository.getDatabaseStats()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val earliestStr = stats.earliestOccurredAt?.let { dateFormat.format(Date(it)) } ?: "None"
        val latestStr = stats.latestOccurredAt?.let { dateFormat.format(Date(it)) } ?: "None"
        val netSavingsMinor = stats.totalIncomeMinor - stats.totalExpenseMinor
        val netSign = if (netSavingsMinor >= 0) "+" else "-"

        val msg = """
📊 Database & Financial Telemetry:
• Total Transactions: ${stats.totalTransactions} (${stats.expenseTransactions} expenses, ${stats.incomeTransactions} incomes)
• All-time Expenses: ${Money(stats.totalExpenseMinor).toFormattedString()}
• All-time Income: ${Money(stats.totalIncomeMinor).toFormattedString()}
• Net Savings: $netSign${Money(Math.abs(netSavingsMinor)).toFormattedString()}
• Active Reminders: ${stats.totalReminders}
• Date Span: $earliestStr to $latestStr
        """.trimIndent()
        return ToolResult.DatabaseStatsResult(stats, msg)
    }

    fun clearAllTransactions(): ToolResult {
        return ToolResult.RequireClearConfirmation(
            message = "⚠️ Are you sure you want to delete ALL expense and income records? This action cannot be undone."
        )
    }
}
