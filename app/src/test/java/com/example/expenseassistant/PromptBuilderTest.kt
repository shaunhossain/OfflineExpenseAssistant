package com.example.expenseassistant

import com.example.expenseassistant.ai.FinancialContext
import com.example.expenseassistant.ai.PromptBuilder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {

    @Test
    fun testSystemPromptContainsActionAndConversationalModes() {
        val systemPrompt = PromptBuilder.buildSystemPrompt()
        assertTrue(systemPrompt.contains("MODE 1: ACTION MODE"))
        assertTrue(systemPrompt.contains("MODE 2: CONVERSATIONAL & ADVISORY MODE"))
        assertTrue(systemPrompt.contains("prepare_expense"))
        assertTrue(systemPrompt.contains("prepare_multiple_expenses"))
        assertTrue(systemPrompt.contains("prepare_income"))
        assertTrue(systemPrompt.contains("get_item_total"))
        assertTrue(systemPrompt.contains("get_spending_summary"))
        assertTrue(systemPrompt.contains("50 tk bus vara"))
    }

    @Test
    fun testUserPromptWithoutFinancialContext() {
        val prompt = PromptBuilder.buildUserPrompt("50 tk bus vara")
        assertTrue(prompt == "50 tk bus vara")
        assertFalse(prompt.contains("[Financial Context:"))
    }

    @Test
    fun testUserPromptWithFinancialContext() {
        val context = FinancialContext(
            totalExpenseFormatted = "৳15,000",
            totalIncomeFormatted = "৳40,000",
            balanceFormatted = "৳25,000",
            topCategory = "Food",
            topCategoryAmountFormatted = "৳6,500"
        )
        val prompt = PromptBuilder.buildUserPrompt("how to save money?", context)
        assertTrue(prompt.contains("[Financial Context: Month Expense=৳15,000, Income=৳40,000, Balance=৳25,000, Top Spending: Food (৳6,500)]"))
        assertTrue(prompt.endsWith("how to save money?"))
    }

    @Test
    fun testFromSpendingSummary() {
        val summary = com.example.expenseassistant.data.SpendingSummary(
            totalExpense = com.example.expenseassistant.domain.Money(1200000L),
            totalIncome = com.example.expenseassistant.domain.Money(3500000L),
            netBalance = com.example.expenseassistant.domain.Money(2300000L),
            transactionCount = 15,
            categoryBreakdown = listOf(
                com.example.expenseassistant.data.CategoryTotal("Food", 700000L, 5),
                com.example.expenseassistant.data.CategoryTotal("Transport", 300000L, 3)
            )
        )
        val context = FinancialContext.fromSpendingSummary(summary)
        assertTrue(context.totalExpenseFormatted.contains("12,000"))
        assertTrue(context.totalIncomeFormatted.contains("35,000"))
        assertTrue(context.balanceFormatted.contains("23,000"))
        org.junit.Assert.assertEquals("Food", context.topCategory)
        assertTrue(context.topCategoryAmountFormatted?.contains("7,000") == true)
    }

    @Test
    fun testSystemPromptContainsGlossaryAndMfsFewShots() {
        val systemPrompt = PromptBuilder.buildSystemPrompt()
        assertTrue(systemPrompt.contains("BANGLISH FINANCIAL GLOSSARY & TAXONOMY"))
        assertTrue(systemPrompt.contains("bKash, Nagad, Rocket"))
        assertTrue(systemPrompt.contains("ma/ammu"))
        assertTrue(systemPrompt.contains("100tk ma ke bkash korlam"))
        assertTrue(systemPrompt.contains("dost k nagad e dilam 250"))
        assertTrue(systemPrompt.contains("ammu k 500 taka pathalam"))
    }

    @Test
    fun testUserPromptNormalizesJoinedCurrency() {
        val prompt = PromptBuilder.buildUserPrompt("100tk ma ke bkash korlam")
        assertTrue(prompt == "100 tk ma ke bkash korlam")

        val prompt2 = PromptBuilder.buildUserPrompt("500taka bhai k pathalam")
        assertTrue(prompt2 == "500 tk bhai k pathalam")
    }

    @Test
    fun testBuildBanglishSemanticBridgeAndEnrichedPrompt() {
        val bridge = PromptBuilder.buildBanglishSemanticBridge("60 tk riksa bhara")
        assertTrue(bridge.isNotEmpty())
        assertTrue(bridge.contains("[Banglish Semantic Bridge:"))
        assertTrue(bridge.contains("rickshaw"))
        assertTrue(bridge.contains("fare"))

        val enriched = PromptBuilder.buildEnrichedUserPrompt("50 tk bus vara")
        assertTrue(enriched.startsWith("50 tk bus vara"))
        assertTrue(enriched.contains("[Banglish Semantic Bridge:"))
        assertTrue(enriched.contains("EnglishMeaning="))
        assertTrue(enriched.contains("BengaliScript="))
    }
}
