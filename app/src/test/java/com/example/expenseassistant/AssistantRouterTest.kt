package com.example.expenseassistant

import com.example.expenseassistant.ai.AssistantRouter
import com.example.expenseassistant.ai.EngineState
import com.example.expenseassistant.ai.GemmaEngine
import com.example.expenseassistant.data.CategoryTotal
import com.example.expenseassistant.data.ExpenseDao
import com.example.expenseassistant.data.ExpenseEntity
import com.example.expenseassistant.data.ExpenseRepository
import com.example.expenseassistant.data.ReminderEntity
import com.example.expenseassistant.mcp.ExpenseTools
import com.example.expenseassistant.mcp.McpToolRegistry
import com.example.expenseassistant.mcp.ToolResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeExpenseDao : ExpenseDao {
    val expenses = mutableListOf<ExpenseEntity>()
    val reminders = mutableListOf<ReminderEntity>()

    override suspend fun insertExpense(expense: ExpenseEntity): Long {
        expenses.add(expense)
        return expenses.size.toLong()
    }

    override suspend fun insertExpenses(expenses: List<ExpenseEntity>): List<Long> {
        this.expenses.addAll(expenses)
        return (1..expenses.size).map { it.toLong() }
    }

    override suspend fun updateExpense(expense: ExpenseEntity) {}
    override suspend fun deleteExpense(expense: ExpenseEntity) {}
    override suspend fun deleteExpenseById(id: Long) {}
    override fun getAllExpenses(): Flow<List<ExpenseEntity>> = flowOf(expenses)
    override fun getRecentExpenses(limit: Int): Flow<List<ExpenseEntity>> = flowOf(expenses.take(limit))
    override suspend fun getAllExpensesSync(): List<ExpenseEntity> = expenses
    override fun getExpensesBetween(startTime: Long, endTime: Long): Flow<List<ExpenseEntity>> = flowOf(expenses)
    override suspend fun getExpensesBetweenSync(startTime: Long, endTime: Long): List<ExpenseEntity> = expenses
    override suspend fun searchExpenses(query: String): List<ExpenseEntity> = expenses.filter { it.note.contains(query) }
    var totalExpenseMinor: Long = 0L
    var totalIncomeMinor: Long = 0L
    var categoryTotals: List<CategoryTotal> = emptyList()

    override suspend fun getCategoryTotals(startTime: Long, endTime: Long, type: String): List<CategoryTotal> = categoryTotals
    override suspend fun getTotalAmountBetween(startTime: Long, endTime: Long, type: String): Long =
        if (type == "EXPENSE") totalExpenseMinor else totalIncomeMinor
    override suspend fun deleteAllExpenses() { expenses.clear() }

    override suspend fun insertReminder(reminder: ReminderEntity): Long = 1L
    override suspend fun updateReminder(reminder: ReminderEntity) {}
    override suspend fun deleteReminder(reminder: ReminderEntity) {}
    override suspend fun deleteReminderById(id: Long) {}
    override fun getAllReminders(): Flow<List<ReminderEntity>> = flowOf(reminders)
    override suspend fun getActiveRemindersSync(): List<ReminderEntity> = reminders
    override suspend fun getReminderById(id: Long): ReminderEntity? = reminders.firstOrNull { it.id == id }
    override suspend fun getExpenseById(id: Long): ExpenseEntity? = expenses.firstOrNull { it.id == id }
    override suspend fun getLatestExpense(): ExpenseEntity? = expenses.maxByOrNull { it.occurredAt }
    override suspend fun getRecentExpensesSync(limit: Int): List<ExpenseEntity> = expenses.sortedByDescending { it.occurredAt }.take(limit)
    override suspend fun getExpenseCount(): Int = expenses.size
    override suspend fun getTotalExpenseTransactionCount(): Int = expenses.count { it.type == "EXPENSE" }
    override suspend fun getTotalIncomeTransactionCount(): Int = expenses.count { it.type == "INCOME" }
    override suspend fun getEarliestExpenseDate(): Long? = expenses.minOfOrNull { it.occurredAt }
    override suspend fun getLatestExpenseDate(): Long? = expenses.maxOfOrNull { it.occurredAt }
    override suspend fun getReminderCount(): Int = reminders.size
    override suspend fun deleteAllReminders() { reminders.clear() }
}

class AssistantRouterTest {

    private lateinit var gemmaEngine: GemmaEngine
    private lateinit var fakeDao: FakeExpenseDao
    private lateinit var repository: ExpenseRepository
    private lateinit var tools: ExpenseTools
    private lateinit var toolRegistry: McpToolRegistry
    private lateinit var router: AssistantRouter

    @Before
    fun setUp() {
        gemmaEngine = GemmaEngine.getInstance()
        fakeDao = FakeExpenseDao()
        repository = ExpenseRepository(fakeDao)
        tools = ExpenseTools(repository)
        toolRegistry = McpToolRegistry(tools)
        router = AssistantRouter(gemmaEngine, toolRegistry, repository)
    }

    @After
    fun tearDown() {
        gemmaEngine.setStateForTesting(EngineState.Uninitialized)
        gemmaEngine.testResponseEmitter = null
    }

    @Test
    fun testEngineUnreadyFallsBackToDeterministicParser() = runBlocking {
        gemmaEngine.setStateForTesting(EngineState.Uninitialized)

        val result = router.processUserInput("50 tk bus vara")
        assertTrue(result is ToolResult.RequireConfirmation)
        val conf = result as ToolResult.RequireConfirmation
        assertEquals(5000L, conf.validationResult.normalizedAmountMinor)
        assertEquals("Transport", conf.validationResult.normalizedCategory)
    }

    @Test
    fun testEngineUnreadyFallsBackToConversationalHelper() = runBlocking {
        gemmaEngine.setStateForTesting(EngineState.Uninitialized)

        val result = router.processUserInput("how to save money")
        assertTrue(result is ToolResult.PlainMessage)
        val msg = (result as ToolResult.PlainMessage).message
        assertTrue(msg.contains("50/30/20"))
    }

    @Test
    fun testEngineReadyProcessesActionOutput() = runBlocking {
        gemmaEngine.setStateForTesting(EngineState.Ready)
        gemmaEngine.testResponseEmitter = {
            flowOf("""{"intent":"prepare_expense","amount":80.0,"category":"Transport","note":"rickshaw"}""")
        }

        val result = router.processUserInput("rickshaw vara 80 tk")
        assertTrue(result is ToolResult.RequireConfirmation)
        val conf = result as ToolResult.RequireConfirmation
        assertEquals(8000L, conf.validationResult.normalizedAmountMinor)
        assertEquals("Transport", conf.validationResult.normalizedCategory)
        assertEquals("rickshaw", conf.validationResult.normalizedNote)
    }

    @Test
    fun testEngineReadyGuardrailPreventsHallucinatedZeroDigitExpense() = runBlocking {
        gemmaEngine.setStateForTesting(EngineState.Ready)
        gemmaEngine.testResponseEmitter = {
            // Simulated hallucinated expense from LLM when user asked a question
            flowOf("""{"intent":"prepare_expense","amount":null,"category":"Transport","note":"bus vara"}""")
        }

        val result = router.processUserInput("bus vara koto")
        // Should redirect to get_item_total search
        assertTrue(result is ToolResult.PlainMessage)
        val msg = (result as ToolResult.PlainMessage).message
        assertTrue(msg.contains("bus vara") || msg.contains("No transactions found"))
    }

    @Test
    fun testEngineReadyReturnsGenerativeAdvice() = runBlocking {
        gemmaEngine.setStateForTesting(EngineState.Ready)
        val adviceText = "Based on your spending, allocating 20% into an emergency fund will keep your budget safe!"
        gemmaEngine.testResponseEmitter = {
            flowOf(adviceText)
        }

        val result = router.processUserInput("how to save money?")
        assertTrue(result is ToolResult.PlainMessage)
        assertEquals(adviceText, (result as ToolResult.PlainMessage).message)
    }

    @Test
    fun testEngineReadyMalformedOutputFallsBackToDeterministic() = runBlocking {
        gemmaEngine.setStateForTesting(EngineState.Ready)
        gemmaEngine.testResponseEmitter = {
            flowOf("I am not sure what you mean.")
        }

        val result = router.processUserInput("100 tk cha")
        assertTrue(result is ToolResult.RequireConfirmation)
        val conf = result as ToolResult.RequireConfirmation
        assertEquals(10000L, conf.validationResult.normalizedAmountMinor)
        assertEquals("Food", conf.validationResult.normalizedCategory)
    }

    @Test
    fun testEngineReadyProcessesMfsTransfer() = runBlocking {
        gemmaEngine.setStateForTesting(EngineState.Ready)
        gemmaEngine.testResponseEmitter = {
            flowOf("""{"intent":"prepare_expense","amount":100.0,"category":"Other","note":"ma ke bkash"}""")
        }

        val result = router.processUserInput("100tk ma ke bkash korlam")
        assertTrue(result is ToolResult.RequireConfirmation)
        val conf = result as ToolResult.RequireConfirmation
        assertEquals(10000L, conf.validationResult.normalizedAmountMinor)
        assertEquals("Other", conf.validationResult.normalizedCategory)
        assertEquals("ma ke bkash", conf.validationResult.normalizedNote)
    }

    @Test
    fun testEngineUnreadyMfsTransferFallsBack() = runBlocking {
        gemmaEngine.setStateForTesting(EngineState.Uninitialized)

        val result = router.processUserInput("100tk ma ke bkash korlam")
        assertTrue(result is ToolResult.RequireConfirmation)
        val conf = result as ToolResult.RequireConfirmation
        assertEquals(10000L, conf.validationResult.normalizedAmountMinor)
        assertEquals("Other", conf.validationResult.normalizedCategory)
        assertEquals("ma ke bkash", conf.validationResult.normalizedNote)
    }

    @Test
    fun testDataGroundedOfflineAdvisory() = runBlocking {
        gemmaEngine.setStateForTesting(EngineState.Uninitialized)
        fakeDao.totalIncomeMinor = 3000000L // 30,000 BDT
        fakeDao.totalExpenseMinor = 1200000L // 12,000 BDT
        fakeDao.categoryTotals = listOf(CategoryTotal("Grocery", 500000L, 1))

        val result = router.processUserInput("amar budget kemon")
        assertTrue(result is ToolResult.PlainMessage)
        val msg = (result as ToolResult.PlainMessage).message
        assertTrue(msg.contains("Monthly Financial Status"))
        assertTrue(msg.contains("12000"))
        assertTrue(msg.contains("30000"))
        assertTrue(msg.contains("Grocery"))
    }

    @Test
    fun testDataGroundedAdvisoryOnGemmaError() = runBlocking {
        gemmaEngine.setStateForTesting(EngineState.Ready)
        gemmaEngine.testResponseEmitter = {
            flowOf("Error: Status Code: 13. Message: Task failed with state: 7")
        }
        fakeDao.totalIncomeMinor = 5000000L // 50,000 BDT
        fakeDao.totalExpenseMinor = 2000000L // 20,000 BDT
        fakeDao.categoryTotals = listOf(CategoryTotal("Bills", 800000L, 1))

        val result = router.processUserInput("how am I doing with my budget")
        assertTrue(result is ToolResult.PlainMessage)
        val msg = (result as ToolResult.PlainMessage).message
        assertTrue(msg.contains("Monthly Financial Status"))
        assertTrue(msg.contains("20000"))
        assertTrue(msg.contains("50000"))
        assertTrue(msg.contains("Bills"))
    }

    @Test
    fun testLunchTotalCostThisWeekRouter() = runBlocking {
        gemmaEngine.setStateForTesting(EngineState.Uninitialized)
        fakeDao.expenses.add(
            ExpenseEntity(
                id = 1,
                amountMinor = 25000L, // 250 tk
                category = "Food",
                note = "lunch",
                type = "EXPENSE",
                occurredAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
        )

        val result = router.processUserInput("lunch a total koto cost hoiche ai week a")
        assertTrue(result is ToolResult.PlainMessage)
        val msg = (result as ToolResult.PlainMessage).message
        assertTrue(msg.contains("Total for 'lunch'"))
        assertTrue(msg.contains("this week"))
        assertTrue(msg.contains("250"))
    }

    @Test
    fun testRouterDeleteLastTransaction() = runBlocking {
        gemmaEngine.setStateForTesting(EngineState.Uninitialized)
        fakeDao.expenses.add(
            ExpenseEntity(
                id = 42,
                amountMinor = 15000L,
                category = "Food",
                note = "cha nasta",
                type = "EXPENSE",
                occurredAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
        )

        val result = router.processUserInput("last expense delete koro")
        assertTrue(result is ToolResult.RequireDeleteConfirmation)
        val del = result as ToolResult.RequireDeleteConfirmation
        assertEquals(42L, del.transactionId)
        assertTrue(del.message.contains("cha nasta"))
    }

    @Test
    fun testRouterDeleteTransactionById() = runBlocking {
        gemmaEngine.setStateForTesting(EngineState.Uninitialized)
        fakeDao.expenses.add(
            ExpenseEntity(
                id = 7,
                amountMinor = 50000L,
                category = "Transport",
                note = "cng",
                type = "EXPENSE",
                occurredAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
        )

        val result = router.processUserInput("delete transaction 7")
        assertTrue(result is ToolResult.RequireDeleteConfirmation)
        val del = result as ToolResult.RequireDeleteConfirmation
        assertEquals(7L, del.transactionId)
        assertTrue(del.message.contains("cng"))
    }

    @Test
    fun testRouterRecentTransactions() = runBlocking {
        gemmaEngine.setStateForTesting(EngineState.Uninitialized)
        fakeDao.expenses.add(
            ExpenseEntity(
                id = 1,
                amountMinor = 10000L,
                category = "Food",
                note = "coffee",
                type = "EXPENSE",
                occurredAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
        )

        val result = router.processUserInput("recent transactions dekhao")
        assertTrue(result is ToolResult.RecentTransactionsResult)
        val recent = result as ToolResult.RecentTransactionsResult
        assertEquals(1, recent.transactions.size)
        assertTrue(recent.message.contains("coffee"))
    }

    @Test
    fun testRouterRemindersListAndDelete() = runBlocking {
        gemmaEngine.setStateForTesting(EngineState.Uninitialized)
        fakeDao.reminders.add(
            ReminderEntity(
                id = 3,
                title = "Expense Reminder",
                message = "log dinner",
                hourOfDay = 21,
                minute = 30,
                isRecurringDaily = true,
                isActive = true,
                reason = "test"
            )
        )

        val listResult = router.processUserInput("reminder gula dekhao")
        assertTrue(listResult is ToolResult.ReminderListResult)
        val list = listResult as ToolResult.ReminderListResult
        assertEquals(1, list.reminders.size)
        assertTrue(list.message.contains("log dinner"))

        val deleteResult = router.processUserInput("delete reminder 3")
        assertTrue(deleteResult is ToolResult.PlainMessage)
        val deleteMsg = (deleteResult as ToolResult.PlainMessage).message
        assertTrue(deleteMsg.contains("Deleted reminder #3"))
    }

    @Test
    fun testRouterToggleReminder() = runBlocking {
        gemmaEngine.setStateForTesting(EngineState.Uninitialized)
        fakeDao.reminders.add(
            ReminderEntity(
                id = 1,
                title = "Expense Reminder",
                message = "daily",
                hourOfDay = 20,
                minute = 0,
                isRecurringDaily = true,
                isActive = true,
                reason = "test"
            )
        )

        val toggleResult = router.processUserInput("reminder 1 bondho koro")
        assertTrue(toggleResult is ToolResult.PlainMessage)
        val msg = (toggleResult as ToolResult.PlainMessage).message
        assertTrue(msg.contains("Reminder #1 is now Paused"))
    }

    @Test
    fun testRouterDatabaseStats() = runBlocking {
        gemmaEngine.setStateForTesting(EngineState.Uninitialized)
        fakeDao.expenses.add(
            ExpenseEntity(
                id = 1,
                amountMinor = 10000L,
                category = "Food",
                note = "burger",
                type = "EXPENSE",
                occurredAt = 1000L,
                createdAt = 1000L
            )
        )
        fakeDao.totalExpenseMinor = 10000L

        val result = router.processUserInput("db stats dekhao")
        assertTrue(result is ToolResult.DatabaseStatsResult)
        val stats = result as ToolResult.DatabaseStatsResult
        assertEquals(1, stats.stats.totalTransactions)
        assertTrue(stats.message.contains("Database & Financial Telemetry"))
    }

    @Test
    fun testRouterClearAllTransactions() = runBlocking {
        gemmaEngine.setStateForTesting(EngineState.Uninitialized)
        val result = router.processUserInput("clear all transactions")
        assertTrue(result is ToolResult.RequireClearConfirmation)
    }

    @Test
    fun testRouterCrossValidatesBanglishCategoryAndAmountWhenLlmMisclassifies() = runBlocking {
        gemmaEngine.setStateForTesting(EngineState.Ready)
        // Simulate 270M LLM returning wrong category ("Other") and missing note on Banglish input
        gemmaEngine.testResponseEmitter = {
            flowOf("""{"intent":"prepare_expense","amount":60.0,"category":"Other","note":""}""")
        }

        val result = router.processUserInput("60 tk riksa bhara")
        assertTrue(result is ToolResult.RequireConfirmation)
        val conf = result as ToolResult.RequireConfirmation
        assertEquals(6000L, conf.validationResult.normalizedAmountMinor)
        assertEquals("Transport", conf.validationResult.normalizedCategory)
    }

    @Test
    fun testRouterCrossValidatesMultiItemBanglishWhenLlmReturnsSingleItem() = runBlocking {
        gemmaEngine.setStateForTesting(EngineState.Ready)
        // Simulate 270M LLM only extracting 1 expense when user gave 2 Banglish expenses
        gemmaEngine.testResponseEmitter = {
            flowOf("""{"intent":"prepare_expense","amount":50.0,"category":"Food","note":"cha"}""")
        }

        val result = router.processUserInput("50 tk cha, 320 tk murgi")
        assertTrue(result is ToolResult.RequireMultipleConfirmation)
        val multi = result as ToolResult.RequireMultipleConfirmation
        assertEquals(2, multi.items.size)
        assertEquals(5000L, multi.items[0].amountMinor)
        assertEquals(32000L, multi.items[1].amountMinor)
    }

    @Test
    fun testMcpToolRegistryAliases() = runBlocking {
        // Alias: get_balance -> getFinancialBalance
        val balanceRes = toolRegistry.executeTool("get_balance")
        assertTrue(balanceRes is ToolResult.PlainMessage)
        val balanceMsg = (balanceRes as ToolResult.PlainMessage).message
        assertTrue(balanceMsg.contains("Financial Balance") || balanceMsg.contains("Savings"))

        // Alias: search -> searchTransactions
        val searchRes = toolRegistry.executeTool("search", query = "napa")
        assertTrue(searchRes is ToolResult.SearchResults)

        // Alias: recent_transactions -> getRecentTransactions
        val recentRes = toolRegistry.executeTool("recent_transactions", limit = 5)
        assertTrue(recentRes is ToolResult.RecentTransactionsResult || recentRes is ToolResult.PlainMessage)

        // Alias: delete_transaction_by_id -> deleteTransaction
        val delRes = toolRegistry.executeTool("delete_transaction_by_id", id = 99L)
        assertTrue(delRes is ToolResult.PlainMessage || delRes is ToolResult.RequireDeleteConfirmation)

        // Alias: set_reminder -> proposeReminder
        val remRes = toolRegistry.executeTool("set_reminder", hour = 21, minute = 0, note = "Log expenses")
        assertTrue(remRes is ToolResult.RequireReminderConfirmation)
    }

    @Test
    fun testImperativeSaveSentencesAndSavingsQueriesThroughRouter() = runBlocking {
        gemmaEngine.setStateForTesting(EngineState.Uninitialized)

        // Imperative save command
        val saveExp = router.processUserInput("save this: 50 tk rickshaw")
        assertTrue(saveExp is ToolResult.RequireConfirmation)
        val conf = saveExp as ToolResult.RequireConfirmation
        assertEquals(5000L, conf.validationResult.normalizedAmountMinor)
        assertEquals("rickshaw", conf.validationResult.normalizedNote)

        // Savings accumulation query
        val saveQuery = router.processUserInput("koto taka save holo")
        assertTrue(saveQuery is ToolResult.PlainMessage)
        val queryMsg = (saveQuery as ToolResult.PlainMessage).message
        assertTrue(queryMsg.contains("Financial Balance") || queryMsg.contains("Savings"))

        // Native Bengali savings query
        val bnQuery = router.processUserInput("কত টাকা সঞ্চয় হলো")
        assertTrue(bnQuery is ToolResult.PlainMessage)

        // Search queries through router
        val s1 = router.processUserInput("search koro medicine")
        assertTrue(s1 is ToolResult.SearchResults)

        val s2 = router.processUserInput("napa search koro")
        assertTrue(s2 is ToolResult.SearchResults)

        val s3 = router.processUserInput("খুঁজে বের করো নাপা")
        assertTrue(s3 is ToolResult.SearchResults)

        // Item total query through router
        val it1 = router.processUserInput("bazar er hisab dao")
        assertTrue(it1 is ToolResult.PlainMessage)

        // Log / statement query through router
        val l1 = router.processUserInput("shesh 5 ta hisab dekhao")
        assertTrue(l1 is ToolResult.RecentTransactionsResult || l1 is ToolResult.PlainMessage)

        val l2 = router.processUserInput("দৈনিক হিসাব দেখাও")
        assertTrue(l2 is ToolResult.RecentTransactionsResult || l2 is ToolResult.PlainMessage)

        // Reminder list query through router
        val r1 = router.processUserInput("সব রিমাইন্ডার দেখাও")
        assertTrue(r1 is ToolResult.ReminderListResult || r1 is ToolResult.PlainMessage)

        // Remaining balance query through router
        val b1 = router.processUserInput("oboshishto koto ache")
        assertTrue(b1 is ToolResult.PlainMessage)
    }

    @Test
    fun testSequentialExpensesDoNotLeakPreviousDescription() = runBlocking {
        gemmaEngine.setStateForTesting(EngineState.Ready)

        // Turn 1: User inputs "50 tk bus vara"
        gemmaEngine.testResponseEmitter = {
            flowOf("""{"intent":"prepare_expense","amount":50,"currency":"BDT","category":"Transport","note":"bus vara"}""")
        }
        val res1 = router.processUserInput("50 tk bus vara")
        assertTrue(res1 is ToolResult.RequireConfirmation)
        val conf1 = res1 as ToolResult.RequireConfirmation
        assertEquals(5000L, conf1.validationResult.normalizedAmountMinor)
        assertEquals("bus vara", conf1.validationResult.normalizedNote)

        // Turn 2: User inputs "100 tk cha", but simulated small LLM has multi-turn drift and leaks previous note "bus vara"
        gemmaEngine.testResponseEmitter = {
            flowOf("""{"intent":"prepare_expense","amount":100,"currency":"BDT","category":"Transport","note":"bus vara"}""")
        }
        val res2 = router.processUserInput("100 tk cha")
        assertTrue(res2 is ToolResult.RequireConfirmation)
        val conf2 = res2 as ToolResult.RequireConfirmation
        assertEquals(10000L, conf2.validationResult.normalizedAmountMinor)
        // Must be "cha", NOT the stale "bus vara"!
        assertEquals("cha", conf2.validationResult.normalizedNote)

        // Turn 3: User inputs "200 tk dinner"
        gemmaEngine.testResponseEmitter = {
            flowOf("""{"intent":"prepare_expense","amount":200,"currency":"BDT","category":"Food","note":"dinner"}""")
        }
        val res3 = router.processUserInput("200 tk dinner")
        assertTrue(res3 is ToolResult.RequireConfirmation)
        val conf3 = res3 as ToolResult.RequireConfirmation
        assertEquals(20000L, conf3.validationResult.normalizedAmountMinor)
        assertEquals("dinner", conf3.validationResult.normalizedNote)

        // Turn 4: User inputs "80 tk" without a description, and LLM repeats previous note "dinner"
        gemmaEngine.testResponseEmitter = {
            flowOf("""{"intent":"prepare_expense","amount":80,"currency":"BDT","category":"Food","note":"dinner"}""")
        }
        val res4 = router.processUserInput("80 tk")
        assertTrue(res4 is ToolResult.RequireConfirmation)
        val conf4 = res4 as ToolResult.RequireConfirmation
        assertEquals(8000L, conf4.validationResult.normalizedAmountMinor)
        // Must NOT be "dinner" from the previous turn!
        assertEquals("Expense", conf4.validationResult.normalizedNote)
    }
}
