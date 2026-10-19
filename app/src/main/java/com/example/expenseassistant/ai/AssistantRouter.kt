package com.example.expenseassistant.ai

import android.util.Log
import com.example.expenseassistant.mcp.McpToolRegistry
import com.example.expenseassistant.mcp.ToolResult
import kotlinx.coroutines.flow.toList

class AssistantRouter(
    private val gemmaEngine: GemmaEngine,
    private val toolRegistry: McpToolRegistry
) {

    suspend fun processUserInput(rawInput: String): ToolResult {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) {
            return ToolResult.PlainMessage("Please enter an expense or question.")
        }

        // 1. Fast deterministic path for zero-latency handling of standard phrases
        val fastResult = BanglishNormalizer.tryFastDeterministicParse(trimmed)
        if (fastResult.matched) {
            Log.d(TAG, "Fast deterministic route matched: intent=${fastResult.intent}")
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

        // 2. Fast conversational AI knowledge check (instant answers for advice, 50/30/20, persona, etc.)
        val conversationalDirect = ConversationalAiHelper.tryConversationalResponse(trimmed)
        if (conversationalDirect != null) {
            Log.d(TAG, "Conversational AI knowledge route matched")
            return ToolResult.PlainMessage(conversationalDirect)
        }

        // 3. On-Device Gemma 3 270M Generative AI Path
        Log.d(TAG, "Routing to local Gemma 3 270M model...")
        val prompt = PromptBuilder.buildUserPrompt(trimmed)
        val responseTokens = gemmaEngine.generateResponse(prompt).toList()
        val fullResponse = responseTokens.joinToString("").trim()

        Log.d(TAG, "Gemma output: $fullResponse")

        val parsed = ModelOutputParser.parse(fullResponse)
        if (parsed != null && parsed.intent.isNotBlank() && parsed.intent != "unknown") {
            // Guardrail against hallucinated expenses from small LLMs when user asked a question or gave no digits
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

        // 4. If Gemma generated conversational or descriptive text, return it directly to the user
        if (fullResponse.isNotBlank() && !fullResponse.startsWith("Error:") && fullResponse != "{}" && fullResponse != "null") {
            return ToolResult.PlainMessage(fullResponse)
        }

        // 5. Intelligent fallback if Gemma is still loading or could not resolve
        val engineState = gemmaEngine.state.value
        val modelLoadingNotice = when (engineState) {
            is EngineState.PreparingModel -> "⏳ Gemma AI is extracting model (${engineState.progressPercent}%)..."
            is EngineState.LoadingEngine -> "⏳ Gemma AI is loading into RAM..."
            is EngineState.Error -> "⚠️ Offline model notice: ${engineState.message}"
            else -> null
        }

        val generalHelp = ConversationalAiHelper.tryConversationalResponse("help")
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
