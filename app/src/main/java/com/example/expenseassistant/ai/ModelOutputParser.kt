package com.example.expenseassistant.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser

data class ParsedModelIntent(
    val intent: String,
    val amount: Double? = null,
    val currency: String = "BDT",
    val category: String? = null,
    val note: String? = null,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val query: String? = null,
    val items: List<com.example.expenseassistant.mcp.SingleItemConfirmation> = emptyList(),
    val rawJson: String = ""
)

object ModelOutputParser {

    private val gson = Gson()

    fun parse(rawOutput: String): ParsedModelIntent? {
        val trimmed = rawOutput.trim()

        // Locate first '{' and last '}'
        val startIndex = trimmed.indexOf('{')
        val endIndex = trimmed.lastIndexOf('}')

        if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) {
            return null
        }

        val jsonCandidate = trimmed.substring(startIndex, endIndex + 1)

        return try {
            val jsonElement = JsonParser.parseString(jsonCandidate)
            if (!jsonElement.isJsonObject) return null
            val obj = jsonElement.asJsonObject

            var intent = obj.get("intent")?.asString ?: "prepare_expense"
            val amount = if (obj.has("amount") && !obj.get("amount").isJsonNull) {
                obj.get("amount").asDouble
            } else null
            val currency = obj.get("currency")?.asString ?: "BDT"
            val category = if (obj.has("category") && !obj.get("category").isJsonNull) {
                obj.get("category").asString
            } else null
            val note = if (obj.has("note") && !obj.get("note").isJsonNull) {
                obj.get("note").asString
            } else null
            val reminderHour = if (obj.has("reminder_hour") && !obj.get("reminder_hour").isJsonNull) {
                obj.get("reminder_hour").asInt
            } else null
            val reminderMinute = if (obj.has("reminder_minute") && !obj.get("reminder_minute").isJsonNull) {
                obj.get("reminder_minute").asInt
            } else null
            val query = if (obj.has("query") && !obj.get("query").isJsonNull) {
                obj.get("query").asString
            } else null

            val items = mutableListOf<com.example.expenseassistant.mcp.SingleItemConfirmation>()
            if (obj.has("items") && obj.get("items").isJsonArray) {
                val array = obj.getAsJsonArray("items")
                for (elem in array) {
                    if (elem.isJsonObject) {
                        val itemObj = elem.asJsonObject
                        val itemAmt = if (itemObj.has("amount")) itemObj.get("amount").asDouble else 0.0
                        val itemCat = if (itemObj.has("category")) itemObj.get("category").asString else "Other"
                        val itemNote = if (itemObj.has("note")) itemObj.get("note").asString else ""
                        if (itemAmt > 0 && itemNote.isNotBlank()) {
                            items.add(
                                com.example.expenseassistant.mcp.SingleItemConfirmation(
                                    amountMinor = (itemAmt * 100).toLong(),
                                    category = itemCat,
                                    note = itemNote
                                )
                            )
                        }
                    }
                }
                if (items.size >= 2) {
                    intent = "prepare_multiple_expenses"
                }
            }

            ParsedModelIntent(
                intent = intent,
                amount = amount,
                currency = currency,
                category = category,
                note = note,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                query = query,
                items = items,
                rawJson = jsonCandidate
            )
        } catch (e: Exception) {
            null
        }
    }
}
