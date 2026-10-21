package com.example.expenseassistant.ai

import android.util.Log
import com.example.expenseassistant.data.ExpenseRepository
import com.example.expenseassistant.mcp.McpToolRegistry
import com.example.expenseassistant.mcp.ToolResult
import kotlinx.coroutines.flow.toList

class AssistantRouter(
    private val gemmaEngine: GemmaEngine,
    private val toolRegistry: McpToolRegistry,
    private val repository: ExpenseRepository? = null
) {

    suspend fun processUserInput(rawInput: String): ToolResult {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) {
            return ToolResult.PlainMessage("Please enter an expense or question.")
        }

        val engineState = gemmaEngine.state.value

        // 1. GEMMA 3 FIRST: Prioritize on-device LLM whenever the engine is Ready
        if (engineState is EngineState.Ready) {
            Log.d(TAG, "Gemma 3 engine is ready: routing primary inference to on-device LLM...")

            // Option B: Contextual Financial Intelligence injection
            val financialContext = if (ConversationalAiHelper.isAdvisoryOrCoachingQuery(trimmed)) {
                try {
                    repository?.getMonthlySummary()?.let { FinancialContext.fromSpendingSummary(it) }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not fetch financial context for advisory query", e)
                    null
                }
            } else null

            val prompt = PromptBuilder.buildEnrichedUserPrompt(trimmed, financialContext)
            val responseTokens = gemmaEngine.generateResponse(prompt).toList()
            val fullResponse = responseTokens.joinToString("").trim()
            Log.d(TAG, "Gemma output: $fullResponse")

            if (fullResponse.isNotBlank() && !fullResponse.startsWith("Error:")) {
                val parsed = ModelOutputParser.parse(fullResponse)
                if (parsed != null && parsed.intent.isNotBlank() && parsed.intent != "unknown") {
                    val fastDeterministic = BanglishNormalizer.tryFastDeterministicParse(trimmed)

                    // Option C Guardrails against hallucinated transactions from small LLM on queries
                    if (parsed.intent == "prepare_expense" || parsed.intent == "prepare_income") {
                        val rawHasDigits = trimmed.any { it.isDigit() } ||
                                (fastDeterministic.matched && (fastDeterministic.amountMinor ?: 0L) > 0L)
                        val hasQuestionWord = trimmed.contains("koto", ignoreCase = true) ||
                                trimmed.contains("kto", ignoreCase = true) ||
                                trimmed.contains("কত") ||
                                trimmed.contains("how much", ignoreCase = true) ||
                                trimmed.contains("how many", ignoreCase = true) ||
                                trimmed.contains("kothay", ignoreCase = true) ||
                                trimmed.contains("ki ", ignoreCase = true) ||
                                trimmed.startsWith("ki", ignoreCase = true) ||
                                trimmed.contains("?")

                        if (!rawHasDigits || hasQuestionWord) {
                            if (fastDeterministic.matched && fastDeterministic.intent !in setOf("prepare_expense", "prepare_income", "prepare_multiple_expenses")) {
                                return toolRegistry.executeTool(
                                    toolName = fastDeterministic.intent,
                                    amountMinor = fastDeterministic.amountMinor,
                                    category = fastDeterministic.category,
                                    note = fastDeterministic.note,
                                    sourceText = trimmed,
                                    hour = fastDeterministic.reminderHour,
                                    minute = fastDeterministic.reminderMinute,
                                    query = fastDeterministic.searchQuery,
                                    items = fastDeterministic.multipleItems,
                                    occurredAt = fastDeterministic.occurredAt,
                                    id = fastDeterministic.id,
                                    limit = fastDeterministic.limit,
                                    isActive = fastDeterministic.isActive
                                )
                            }

                            val targetQuery = if (fastDeterministic.matched && fastDeterministic.intent == "get_item_total" && !fastDeterministic.searchQuery.isNullOrBlank()) {
                                fastDeterministic.searchQuery
                            } else {
                                parsed.note?.takeIf { it.isNotBlank() }
                                    ?: parsed.category?.takeIf { it.isNotBlank() && it != "Other" }
                                    ?: trimmed
                            }
                            val targetPeriod = if (fastDeterministic.matched && fastDeterministic.intent == "get_item_total") fastDeterministic.note else null
                            Log.w(TAG, "Prevented hallucinated expense without digits in input: redirecting to get_item_total for '$targetQuery' ($targetPeriod)")
                            return toolRegistry.executeTool(
                                toolName = "get_item_total",
                                query = targetQuery,
                                note = targetPeriod,
                                sourceText = trimmed
                            )
                        }

                        // Cross-validation: if deterministic parser detected multiple expenses in a Banglish compound sentence
                        // but the 270M LLM only extracted a single expense, promote to prepare_multiple_expenses
                        if (fastDeterministic.matched &&
                            fastDeterministic.intent == "prepare_multiple_expenses" &&
                            fastDeterministic.multipleItems.size >= 2 &&
                            parsed.items.isEmpty()
                        ) {
                            return toolRegistry.executeTool(
                                toolName = "prepare_multiple_expenses",
                                items = fastDeterministic.multipleItems,
                                sourceText = trimmed
                            )
                        }
                    }

                    // Cross-validation: reconcile Banglish income vs expense direction, dates, amounts, and categories
                    val resolvedIntent = if (
                        fastDeterministic.matched &&
                        fastDeterministic.intent == "prepare_income" &&
                        parsed.intent == "prepare_expense"
                    ) {
                        "prepare_income"
                    } else {
                        parsed.intent
                    }

                    val amountMinor = parsed.amount?.let { (it * 100).toLong() } ?: fastDeterministic.amountMinor
                    val resolvedCategory = if (parsed.category.isNullOrBlank() || (parsed.category == "Other" && !fastDeterministic.category.isNullOrBlank() && fastDeterministic.category != "Other")) {
                        fastDeterministic.category ?: parsed.category
                    } else {
                        parsed.category
                    }
                    val cleanParsedNote = parsed.note?.let { BanglishNormalizer.cleanExpenseNote(it) }?.takeIf { it.isNotBlank() }
                    val detNote = fastDeterministic.note?.let { BanglishNormalizer.cleanExpenseNote(it) }?.takeIf { it.isNotBlank() }

                    val finalNote = if (resolvedIntent == "get_item_total") {
                        detNote ?: cleanParsedNote
                    } else if (cleanParsedNote != null) {
                        val rawInput = trimmed.lowercase()
                        val normalizedInput = BanglishNormalizer.normalize(trimmed).lowercase()
                        val parsedWords = cleanParsedNote.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }

                        val parsedInInput = parsedWords.any { word ->
                            rawInput.contains(word) || normalizedInput.contains(word) ||
                                    BanglishTransliterator.transliterateSentence(rawInput).contains(word) ||
                                    BanglishTransliterator.translateToEnglishGloss(rawInput).contains(word)
                        }

                        if (!parsedInInput) {
                            Log.w(TAG, "Gemma hallucinated or carried over stale note '$cleanParsedNote' from previous turn. Overriding with '${detNote ?: "Expense"}' from current input.")
                            detNote ?: "Expense"
                        } else {
                            cleanParsedNote
                        }
                    } else {
                        detNote
                    }
                    val resolvedQuery = parsed.query?.takeIf { it.isNotBlank() } ?: fastDeterministic.searchQuery
                    val resolvedOccurredAt = parsed.occurredAt ?: fastDeterministic.occurredAt
                    val resolvedItems = if (parsed.items.isNotEmpty()) parsed.items else fastDeterministic.multipleItems

                    if (resolvedIntent in setOf("prepare_expense", "prepare_income", "prepare_multiple_expenses")) {
                        gemmaEngine.resetConversation()
                    }

                    return toolRegistry.executeTool(
                        toolName = resolvedIntent,
                        amountMinor = amountMinor,
                        category = resolvedCategory,
                        note = finalNote,
                        sourceText = trimmed,
                        hour = parsed.reminderHour ?: fastDeterministic.reminderHour,
                        minute = parsed.reminderMinute ?: fastDeterministic.reminderMinute,
                        query = resolvedQuery,
                        items = resolvedItems,
                        occurredAt = resolvedOccurredAt,
                        id = parsed.id ?: fastDeterministic.id,
                        limit = parsed.limit ?: fastDeterministic.limit,
                        isActive = parsed.isActive ?: fastDeterministic.isActive
                    )
                }

                // If parsed is null, check if deterministic parser recognizes an action
                val fastTxFallback = BanglishNormalizer.tryFastDeterministicParse(trimmed)
                if (fastTxFallback.matched) {
                    Log.d(TAG, "Gemma did not output structured action JSON for input. Executing deterministic fallback: ${fastTxFallback.intent}")
                    return toolRegistry.executeTool(
                        toolName = fastTxFallback.intent,
                        amountMinor = fastTxFallback.amountMinor,
                        category = fastTxFallback.category,
                        note = fastTxFallback.note,
                        sourceText = trimmed,
                        hour = fastTxFallback.reminderHour,
                        minute = fastTxFallback.reminderMinute,
                        query = fastTxFallback.searchQuery,
                        items = fastTxFallback.multipleItems,
                        occurredAt = fastTxFallback.occurredAt,
                        id = fastTxFallback.id,
                        limit = fastTxFallback.limit,
                        isActive = fastTxFallback.isActive
                    )
                }

                // Generative text returned directly (conversational advice, budgeting coaching, persona)
                if (fullResponse != "{}" && fullResponse != "null") {
                    return ToolResult.PlainMessage(fullResponse)
                }
            }

            // If Gemma generated an empty/corrupted response, attempt fast deterministic fallback
            Log.w(TAG, "Gemma produced empty/corrupted response. Falling back to deterministic parser.")
            val fastFallback = BanglishNormalizer.tryFastDeterministicParse(trimmed)
            if (fastFallback.matched) {
                return toolRegistry.executeTool(
                    toolName = fastFallback.intent,
                    amountMinor = fastFallback.amountMinor,
                    category = fastFallback.category,
                    note = fastFallback.note,
                    sourceText = trimmed,
                    hour = fastFallback.reminderHour,
                    minute = fastFallback.reminderMinute,
                    query = fastFallback.searchQuery,
                    items = fastFallback.multipleItems,
                    occurredAt = fastFallback.occurredAt,
                    id = fastFallback.id,
                    limit = fastFallback.limit,
                    isActive = fastFallback.isActive
                )
            }

            // Check data-grounded offline advisory
            if (ConversationalAiHelper.isAdvisoryOrCoachingQuery(trimmed) && repository != null) {
                try {
                    val summary = repository.getMonthlySummary()
                    return ToolResult.PlainMessage(ConversationalAiHelper.formatDataGroundedAdvisory(summary, trimmed))
                } catch (e: Exception) {
                    Log.w(TAG, "Could not compute data-grounded advisory", e)
                }
            }

            val conversationalFallback = ConversationalAiHelper.tryOfflineFallbackResponse(trimmed)
            if (conversationalFallback != null) {
                return ToolResult.PlainMessage(conversationalFallback)
            }
        }

        // 2. DETERMINISTIC FALLBACK (when Gemma is not Ready: Preparing, Loading, Error, or Uninitialized)
        Log.d(TAG, "Gemma engine state ($engineState) not ready. Using fast deterministic route.")

        // 2a. Check fast deterministic regex parser
        val fastResult = BanglishNormalizer.tryFastDeterministicParse(trimmed)
        if (fastResult.matched) {
            return toolRegistry.executeTool(
                toolName = fastResult.intent,
                amountMinor = fastResult.amountMinor,
                category = fastResult.category,
                note = fastResult.note,
                sourceText = trimmed,
                hour = fastResult.reminderHour,
                minute = fastResult.reminderMinute,
                query = fastResult.searchQuery,
                items = fastResult.multipleItems,
                occurredAt = fastResult.occurredAt,
                id = fastResult.id,
                limit = fastResult.limit,
                isActive = fastResult.isActive
            )
        }

        // 2b. Check offline fallback conversational responses
        if (ConversationalAiHelper.isAdvisoryOrCoachingQuery(trimmed) && repository != null) {
            try {
                val summary = repository.getMonthlySummary()
                return ToolResult.PlainMessage(ConversationalAiHelper.formatDataGroundedAdvisory(summary, trimmed))
            } catch (e: Exception) {
                Log.w(TAG, "Could not compute data-grounded advisory", e)
            }
        }

        val conversationalFallback = ConversationalAiHelper.tryOfflineFallbackResponse(trimmed)
        if (conversationalFallback != null) {
            return ToolResult.PlainMessage(conversationalFallback)
        }

        // 2c. Engine status notice if loading or in error
        val modelLoadingNotice = when (engineState) {
            is EngineState.PreparingModel -> "⏳ Gemma AI is extracting model (${engineState.progressPercent}%)..."
            is EngineState.LoadingEngine -> "⏳ Gemma AI is loading into RAM..."
            is EngineState.Error -> "⚠️ Offline model notice: ${engineState.message}"
            else -> null
        }

        val generalHelp = ConversationalAiHelper.tryOfflineFallbackResponse("help")
        val fallbackMessage = if (modelLoadingNotice != null) {
            "$modelLoadingNotice\n\nIn the meantime, you can:\n• Log expenses: '50 tk bus vara' or 'lunch 250 tk'\n• Ask: 'total bus vara koto', 'amar balance koto'\n• Ask: 'how to save money' or '50/30/20 rule'"
        } else {
            generalHelp ?: "I'm your offline personal finance AI! You can log expenses ('50 tk bus vara') or ask questions like 'total bus vara koto' or 'how to save money?'"
        }

        return ToolResult.PlainMessage(fallbackMessage)
    }

    companion object {
        private const val TAG = "AssistantRouter"
    }
}

