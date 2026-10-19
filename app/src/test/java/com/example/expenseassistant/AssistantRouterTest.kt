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
    override suspend fun getCategoryTotals(startTime: Long, endTime: Long, type: String): List<CategoryTotal> = emptyList()
    override suspend fun getTotalAmountBetween(startTime: Long, endTime: Long, type: String): Long = 0L
    override suspend fun deleteAllExpenses() { expenses.clear() }

    override suspend fun insertReminder(reminder: ReminderEntity): Long = 1L
    override suspend fun updateReminder(reminder: ReminderEntity) {}
    override suspend fun deleteReminder(reminder: ReminderEntity) {}
    override suspend fun deleteReminderById(id: Long) {}
    override fun getAllReminders(): Flow<List<ReminderEntity>> = flowOf(reminders)
    override suspend fun getActiveRemindersSync(): List<ReminderEntity> = reminders
    override suspend fun getReminderById(id: Long): ReminderEntity? = reminders.firstOrNull { it.id == id }
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
}
