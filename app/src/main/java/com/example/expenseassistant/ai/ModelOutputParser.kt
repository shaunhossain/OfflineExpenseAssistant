package com.example.expenseassistant.ai

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
    val rawJson: String = "",
    val id: Long? = null,
    val limit: Int? = null,
    val isActive: Boolean? = null,
    val occurredAt: Long? = null
)

object ModelOutputParser {

    fun sanitizeJson(candidate: String): String {
        var s = candidate.trim()

        // Normalize smart / typographic quotes to standard ASCII quotes
        s = s.replace('“', '"').replace('”', '"')
        s = s.replace('‘', '\'').replace('’', '\'')

        // Remove trailing commas before closing braces or brackets: e.g. ", }" or ", ]"
        s = s.replace(Regex(",\\s*([}\\]])"), "$1")

        // If single quotes are used for JSON properties/values: e.g. {'intent': 'prepare_expense'}
        if (s.contains("'") && !s.contains("\"")) {
            s = s.replace("'", "\"")
        }

        return s
    }

    fun parse(rawOutput: String): ParsedModelIntent? {
        val trimmed = rawOutput.trim()

        // Locate outermost '{' and '}'
        val startIndex = trimmed.indexOf('{')
        val endIndex = trimmed.lastIndexOf('}')

        if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) {
            return null
        }

        val rawCandidate = trimmed.substring(startIndex, endIndex + 1)
        val candidate = sanitizeJson(rawCandidate)

        return try {
            val jsonElement = JsonParser.parseString(candidate)
            if (!jsonElement.isJsonObject) return null
            val obj = jsonElement.asJsonObject

            var intent = obj.get("intent")?.asString ?: "prepare_expense"
            val amount = parseAmount(obj)
            val currency = obj.get("currency")?.asString ?: "BDT"
            val category = if (obj.has("category") && !obj.get("category").isJsonNull) {
                obj.get("category").asString.trim()
            } else null
            val note = if (obj.has("note") && !obj.get("note").isJsonNull) {
                obj.get("note").asString.trim()
            } else null
            val reminderHour = parseNullableInt(obj, "reminder_hour")
            val reminderMinute = parseNullableInt(obj, "reminder_minute")
            val query = if (obj.has("query") && !obj.get("query").isJsonNull) {
                obj.get("query").asString.trim()
            } else null
            val id = parseNullableLong(obj, "id")
            val limit = parseNullableInt(obj, "limit")
            val isActive = if (obj.has("isActive") && !obj.get("isActive").isJsonNull) {
                try {
                    obj.get("isActive").asBoolean
                } catch (e: Exception) {
                    obj.get("isActive").asString.toBooleanStrictOrNull()
                }
            } else null
            val occurredAt = parseNullableLong(obj, "occurredAt")

            val items = mutableListOf<com.example.expenseassistant.mcp.SingleItemConfirmation>()
            if (obj.has("items") && obj.get("items").isJsonArray) {
                val array = obj.getAsJsonArray("items")
                for (elem in array) {
                    if (elem.isJsonObject) {
                        val itemObj = elem.asJsonObject
                        val itemAmt = parseAmount(itemObj) ?: 0.0
                        val itemCat = if (itemObj.has("category") && !itemObj.get("category").isJsonNull) {
                            itemObj.get("category").asString.trim()
                        } else "Other"
                        val itemNote = if (itemObj.has("note") && !itemObj.get("note").isJsonNull) {
                            itemObj.get("note").asString.trim()
                        } else ""
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
                rawJson = candidate,
                id = id,
                limit = limit,
                isActive = isActive,
                occurredAt = occurredAt
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseAmount(obj: JsonObject): Double? {
        if (!obj.has("amount") || obj.get("amount").isJsonNull) return null
        val elem = obj.get("amount")
        return try {
            if (elem.isJsonPrimitive) {
                val prim = elem.asJsonPrimitive
                if (prim.isNumber) {
                    prim.asDouble
                } else {
                    val rawStr = prim.asString.replace("tk", "", ignoreCase = true)
                        .replace("taka", "", ignoreCase = true)
                        .replace("৳", "")
                        .replace(",", "")
                        .trim()
                    rawStr.toDoubleOrNull()
                }
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun parseNullableInt(obj: JsonObject, field: String): Int? {
        if (!obj.has(field) || obj.get(field).isJsonNull) return null
        return try {
            obj.get(field).asInt
        } catch (e: Exception) {
            obj.get(field).asString.toIntOrNull()
        }
    }

    private fun parseNullableLong(obj: JsonObject, field: String): Long? {
        if (!obj.has(field) || obj.get(field).isJsonNull) return null
        return try {
            obj.get(field).asLong
        } catch (e: Exception) {
            obj.get(field).asString.toLongOrNull()
        }
    }
}

