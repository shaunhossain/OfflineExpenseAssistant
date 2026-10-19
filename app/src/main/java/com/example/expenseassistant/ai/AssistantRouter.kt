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

            val prompt = PromptBuilder.buildUserPrompt(trimmed, financialContext)
            val responseTokens = gemmaEngine.generateResponse(prompt).toList()
            val fullResponse = responseTokens.joinToString("").trim()
            Log.d(TAG, "Gemma output: $fullResponse")

            if (fullResponse.isNotBlank() && !fullResponse.startsWith("Error:")) {
                val parsed = ModelOutputParser.parse(fullResponse)
                if (parsed != null && parsed.intent.isNotBlank() && parsed.intent != "unknown") {
                    // Option C Guardrails against hallucinated transactions from small LLM on queries
                    if (parsed.intent == "prepare_expense" || parsed.intent == "prepare_income") {
                        val rawHasDigits = trimmed.any { it.isDigit() }
                        val hasQuestionWord = trimmed.contains("koto", ignoreCase = true) ||
                                trimmed.contains("how much", ignoreCase = true) ||
                                trimmed.contains("how many", ignoreCase = true) ||
                                trimmed.contains("kothay", ignoreCase = true) ||
                                trimmed.contains("ki ", ignoreCase = true) ||
                                trimmed.startsWith("ki", ignoreCase = true) ||
                                trimmed.contains("?")

                        if (!rawHasDigits || hasQuestionWord) {
                            val targetQuery = parsed.note?.takeIf { it.isNotBlank() }
                                ?: parsed.category?.takeIf { it.isNotBlank() && it != "Other" }
                                ?: trimmed
                            Log.w(TAG, "Prevented hallucinated expense without digits in input: redirecting to get_item_total for '$targetQuery'")
                            return toolRegistry.executeTool(
                                toolName = "get_item_total",
                                query = targetQuery,
                                sourceText = trimmed
                            )
                        }
                    }

                    val amountMinor = parsed.amount?.let { (it * 100).toLong() }
                    return toolRegistry.executeTool(
                        toolName = parsed.intent,
                        amountMinor = amountMinor,
                        category = parsed.category,
                        note = parsed.note,
                        sourceText = trimmed,
                        hour = parsed.reminderHour,
                        minute = parsed.reminderMinute,
                        query = parsed.query,
                        items = parsed.items
                    )
                }

                // If parsed is null, check if deterministic parser recognizes an expense/income transaction
                val fastTxFallback = BanglishNormalizer.tryFastDeterministicParse(trimmed)
                if (fastTxFallback.matched && fastTxFallback.intent.startsWith("prepare_")) {
                    Log.d(TAG, "Gemma did not output structured action JSON for transaction input. Executing deterministic fallback.")
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
                        occurredAt = fastTxFallback.occurredAt
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
                    occurredAt = fastFallback.occurredAt
                )
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
                occurredAt = fastResult.occurredAt
            )
        }

        // 2b. Check offline fallback conversational responses
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

