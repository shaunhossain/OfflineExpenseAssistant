package com.example.expenseassistant.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.expenseassistant.OfflineExpenseApp
import com.example.expenseassistant.ai.AssistantRouter
import com.example.expenseassistant.ai.EngineState
import com.example.expenseassistant.ai.GemmaEngine
import com.example.expenseassistant.data.ExpenseEntity
import com.example.expenseassistant.data.ReminderEntity
import com.example.expenseassistant.data.SpendingSummary
import com.example.expenseassistant.data.TransactionType
import com.example.expenseassistant.domain.HabitInsight
import com.example.expenseassistant.domain.Money
import com.example.expenseassistant.domain.SpendingAnalyzer
import com.example.expenseassistant.domain.ValidationResult
import com.example.expenseassistant.mcp.ExpenseTools
import com.example.expenseassistant.mcp.McpToolRegistry
import com.example.expenseassistant.mcp.SingleItemConfirmation
import com.example.expenseassistant.mcp.ToolResult
import com.example.expenseassistant.reminders.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val toolResult: ToolResult? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER,
    ASSISTANT
}

data class PendingConfirmation(
    val validationResult: ValidationResult,
    val proposedType: TransactionType,
    val sourceText: String
)

data class PendingMultipleConfirmation(
    val items: List<SingleItemConfirmation>,
    val totalAmountMinor: Long,
    val sourceText: String
)

data class PendingReminderConfirmation(
    val title: String,
    val message: String,
    val hourOfDay: Int,
    val minute: Int,
    val reason: String
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as OfflineExpenseApp
    private val repository = app.repository
    private val gemmaEngine = GemmaEngine.getInstance()
    private val tools = ExpenseTools(repository)
    private val toolRegistry = McpToolRegistry(tools)
    private val assistantRouter = AssistantRouter(gemmaEngine, toolRegistry, repository)

    val engineState: StateFlow<EngineState> = gemmaEngine.state

    val allExpenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReminders: StateFlow<List<ReminderEntity>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _monthlySummary = MutableStateFlow(
        SpendingSummary(Money.ZERO, Money.ZERO, Money.ZERO, 0, emptyList())
    )
    val monthlySummary: StateFlow<SpendingSummary> = _monthlySummary.asStateFlow()

    private val _habitInsights = MutableStateFlow<List<HabitInsight>>(emptyList())
    val habitInsights: StateFlow<List<HabitInsight>> = _habitInsights.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = "Hello! I'm your offline Gemma Expense Assistant.\nTry entering an expense in English or Banglish, like:\n• '50 tk bus vara'\n• '100 tk cha ar bill'\n• 'ei mashe koto khoroch hoise?'"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingConfirmation?>(null)
    val pendingConfirmation: StateFlow<PendingConfirmation?> = _pendingConfirmation.asStateFlow()

    private val _pendingMultipleConfirmation = MutableStateFlow<PendingMultipleConfirmation?>(null)
    val pendingMultipleConfirmation: StateFlow<PendingMultipleConfirmation?> = _pendingMultipleConfirmation.asStateFlow()

    private val _pendingReminderConfirmation = MutableStateFlow<PendingReminderConfirmation?>(null)
    val pendingReminderConfirmation: StateFlow<PendingReminderConfirmation?> = _pendingReminderConfirmation.asStateFlow()

    init {
        refreshSummary()
        refreshHabits()
    }

    fun refreshSummary() {
        viewModelScope.launch {
            _monthlySummary.value = repository.getMonthlySummary()
        }
    }

    fun refreshHabits() {
        viewModelScope.launch {
            val list = repository.search("")
            _habitInsights.value = SpendingAnalyzer.analyzeHabits(list)
        }
    }

    fun sendUserMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _isProcessing.value) return

        _chatMessages.value = _chatMessages.value + ChatMessage(
            sender = MessageSender.USER,
            text = trimmed
        )
        _isProcessing.value = true

        viewModelScope.launch {
            try {
                val result = assistantRouter.processUserInput(trimmed)
                handleToolResult(result, trimmed)
            } catch (e: Exception) {
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    sender = MessageSender.ASSISTANT,
                    text = "Error processing request: ${e.localizedMessage}"
                )
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun handleToolResult(result: ToolResult, sourceText: String) {
        when (result) {
            is ToolResult.RequireConfirmation -> {
                _pendingConfirmation.value = PendingConfirmation(
                    validationResult = result.validationResult,
                    proposedType = result.proposedType,
                    sourceText = sourceText
                )
                val taka = Money(result.validationResult.normalizedAmountMinor).toFormattedString()
                val promptText = "I found: $taka for ${result.validationResult.normalizedCategory} (${result.validationResult.normalizedNote}). Please confirm below."
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    sender = MessageSender.ASSISTANT,
                    text = promptText,
                    toolResult = result
                )
            }

            is ToolResult.RequireMultipleConfirmation -> {
                _pendingMultipleConfirmation.value = PendingMultipleConfirmation(
                    items = result.items,
                    totalAmountMinor = result.totalAmountMinor,
                    sourceText = sourceText
                )
                val totalTaka = Money(result.totalAmountMinor).toFormattedString()
                val itemsList = result.items.joinToString("\n") {
                    "• ${Money(it.amountMinor).toFormattedString()} - ${it.note} [${it.category}]"
                }
                val promptText = "Found ${result.items.size} expenses (Total: $totalTaka):\n$itemsList\n\nPlease confirm below to save all."
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    sender = MessageSender.ASSISTANT,
                    text = promptText,
                    toolResult = result
                )
            }

            is ToolResult.RequireReminderConfirmation -> {
                _pendingReminderConfirmation.value = PendingReminderConfirmation(
                    title = result.title,
                    message = result.message,
                    hourOfDay = result.hourOfDay,
                    minute = result.minute,
                    reason = result.reason
                )
                val timeFormatted = String.format("%02d:%02d", result.hourOfDay, result.minute)
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    sender = MessageSender.ASSISTANT,
                    text = "Would you like to set a reminder at $timeFormatted daily: '${result.message}'?",
                    toolResult = result
                )
            }

            is ToolResult.SummaryResult -> {
                refreshSummary()
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    sender = MessageSender.ASSISTANT,
                    text = result.message,
                    toolResult = result
                )
            }

            is ToolResult.SearchResults -> {
                val count = result.results.size
                val msg = if (count > 0) {
                    "Found $count transactions matching '${result.query}'."
                } else {
                    "No transactions found matching '${result.query}'."
                }
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    sender = MessageSender.ASSISTANT,
                    text = msg,
                    toolResult = result
                )
            }

            is ToolResult.HabitPatternsResult -> {
                val patternsText = if (result.insights.isNotEmpty()) {
                    result.insights.joinToString("\n\n") { "• ${it.title}: ${it.description}" }
                } else {
                    "Not enough transactions yet to detect spending patterns."
                }
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    sender = MessageSender.ASSISTANT,
                    text = "Spending Habits:\n$patternsText",
                    toolResult = result
                )
            }

            is ToolResult.PlainMessage -> {
                refreshSummary()
                refreshHabits()
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    sender = MessageSender.ASSISTANT,
                    text = result.message,
                    toolResult = result
                )
            }
        }
    }

    fun confirmSaveTransaction(
        amountMinor: Long,
        category: String,
        note: String,
        type: TransactionType,
        sourceText: String,
        occurredAt: Long? = null
    ) {
        viewModelScope.launch {
            repository.saveExpense(
                amountMinor = amountMinor,
                category = category,
                note = note,
                type = type,
                occurredAt = occurredAt ?: System.currentTimeMillis(),
                sourceText = sourceText,
                needsReview = false
            )
            _pendingConfirmation.value = null
            refreshSummary()
            refreshHabits()

            val taka = Money(amountMinor).toFormattedString()
            val dateNote = if (occurredAt != null) {
                val d = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(occurredAt))
                " on $d"
            } else ""
            _chatMessages.value = _chatMessages.value + ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = "✓ Successfully recorded $taka for $category ($note)$dateNote."
            )
        }
    }

    fun cancelPendingConfirmation() {
        _pendingConfirmation.value = null
        _chatMessages.value = _chatMessages.value + ChatMessage(
            sender = MessageSender.ASSISTANT,
            text = "Transaction entry canceled."
        )
    }

    fun confirmSaveMultipleTransactions(
        items: List<SingleItemConfirmation>,
        sourceText: String
    ) {
        viewModelScope.launch {
            items.forEach { item ->
                repository.saveExpense(
                    amountMinor = item.amountMinor,
                    category = item.category,
                    note = item.note,
                    type = item.type,
                    occurredAt = item.occurredAt ?: System.currentTimeMillis(),
                    sourceText = sourceText,
                    needsReview = false
                )
            }
            _pendingMultipleConfirmation.value = null
            refreshSummary()
            refreshHabits()

            val totalTaka = Money(items.sumOf { it.amountMinor }).toFormattedString()
            _chatMessages.value = _chatMessages.value + ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = "✓ Successfully recorded ${items.size} expenses totaling $totalTaka."
            )
        }
    }

    fun cancelPendingMultipleConfirmation() {
        _pendingMultipleConfirmation.value = null
        _chatMessages.value = _chatMessages.value + ChatMessage(
            sender = MessageSender.ASSISTANT,
            text = "Multiple transactions canceled."
        )
    }

    fun confirmSaveReminder(
        title: String,
        message: String,
        hourOfDay: Int,
        minute: Int,
        reason: String
    ) {
        viewModelScope.launch {
            val id = repository.saveReminder(
                title = title,
                message = message,
                hourOfDay = hourOfDay,
                minute = minute,
                reason = reason
            )
            ReminderScheduler.scheduleDailyReminder(
                context = app,
                reminderId = id,
                hourOfDay = hourOfDay,
                minute = minute,
                title = title,
                message = message
            )
            _pendingReminderConfirmation.value = null
            val time = String.format("%02d:%02d", hourOfDay, minute)
            _chatMessages.value = _chatMessages.value + ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = "✓ Reminder scheduled for $time daily: '$message'."
            )
        }
    }

    fun cancelPendingReminder() {
        _pendingReminderConfirmation.value = null
        _chatMessages.value = _chatMessages.value + ChatMessage(
            sender = MessageSender.ASSISTANT,
            text = "Reminder setup canceled."
        )
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            repository.deleteExpense(id)
            refreshSummary()
            refreshHabits()
        }
    }

    fun toggleReminder(id: Long, isActive: Boolean) {
        viewModelScope.launch {
            repository.toggleReminder(id, isActive)
            if (!isActive) {
                ReminderScheduler.cancelReminder(app, id)
            }
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            repository.deleteReminder(id)
            ReminderScheduler.cancelReminder(app, id)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            refreshSummary()
            refreshHabits()
            _chatMessages.value = listOf(
                ChatMessage(
                    sender = MessageSender.ASSISTANT,
                    text = "All local expense and reminder data has been permanently cleared."
                )
            )
        }
    }
}
