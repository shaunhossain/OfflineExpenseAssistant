package com.example.expenseassistant

import com.example.expenseassistant.ai.ModelOutputParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ModelOutputParserTest {

    @Test
    fun testParseCleanJson() {
        val json = """{"intent":"prepare_expense","amount":50.0,"currency":"BDT","category":"Transport","note":"bus vara","reminder_hour":null,"reminder_minute":null,"query":null}"""
        val result = ModelOutputParser.parse(json)
        assertNotNull(result)
        assertEquals("prepare_expense", result!!.intent)
        assertEquals(50.0, result.amount ?: 0.0, 0.01)
        assertEquals("Transport", result.category)
        assertEquals("bus vara", result.note)
    }

    @Test
    fun testParseMarkdownCodeBlock() {
        val output = """
            Here is the parsed intent:
            ```json
            {
              "intent": "prepare_income",
              "amount": 30000,
              "currency": "BDT",
              "category": "Salary",
              "note": "salary paisi"
            }
            ```
        """.trimIndent()
        val result = ModelOutputParser.parse(output)
        assertNotNull(result)
        assertEquals("prepare_income", result!!.intent)
        assertEquals(30000.0, result.amount ?: 0.0, 0.01)
        assertEquals("Salary", result.category)
    }

    @Test
    fun testCorruptJsonFallback() {
        val corrupted = "Sorry, I am not able to parse this."
        val result = ModelOutputParser.parse(corrupted)
        assertNull(result)
    }

    @Test
    fun testTrailingCommaRepair() {
        val output = """{"intent": "prepare_expense", "amount": 60, "category": "Transport", "note": "rickshaw",}"""
        val result = ModelOutputParser.parse(output)
        assertNotNull(result)
        assertEquals("prepare_expense", result!!.intent)
        assertEquals(60.0, result.amount ?: 0.0, 0.01)
        assertEquals("rickshaw", result.note)
    }

    @Test
    fun testSmartQuotesAndSingleQuotes() {
        val output = """{‘intent’: ‘prepare_expense’, ‘amount’: 120, ‘category’: ‘Food’, ‘note’: ‘breakfast’}"""
        val result = ModelOutputParser.parse(output)
        assertNotNull(result)
        assertEquals("prepare_expense", result!!.intent)
        assertEquals(120.0, result.amount ?: 0.0, 0.01)
        assertEquals("breakfast", result.note)
    }

    @Test
    fun testStringAmountWithCurrencySuffix() {
        val output = """{"intent": "prepare_expense", "amount": "250 tk", "category": "Food", "note": "lunch"}"""
        val result = ModelOutputParser.parse(output)
        assertNotNull(result)
        assertEquals("prepare_expense", result!!.intent)
        assertEquals(250.0, result.amount ?: 0.0, 0.01)
    }

    @Test
    fun testMultipleExpensesParsing() {
        val output = """
            {"intent":"prepare_multiple_expenses","items":[
                {"amount": 50, "category": "Food", "note": "tea"},
                {"amount": "300 tk", "category": "Grocery", "note": "chicken"}
            ]}
        """.trimIndent()
        val result = ModelOutputParser.parse(output)
        assertNotNull(result)
        assertEquals("prepare_multiple_expenses", result!!.intent)
        assertEquals(2, result.items.size)
        assertEquals(5000L, result.items[0].amountMinor)
        assertEquals(30000L, result.items[1].amountMinor)
    }
}

