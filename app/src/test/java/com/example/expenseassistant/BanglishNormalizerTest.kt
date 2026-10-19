package com.example.expenseassistant

import com.example.expenseassistant.ai.BanglishNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BanglishNormalizerTest {

    @Test
    fun testBusVara() {
        val result = BanglishNormalizer.tryFastDeterministicParse("50 tk bus vara")
        assertTrue(result.matched)
        assertEquals("prepare_expense", result.intent)
        assertEquals(5000L, result.amountMinor)
        assertEquals("Transport", result.category)
    }

    @Test
    fun testChaBill() {
        val result = BanglishNormalizer.tryFastDeterministicParse("100 tk cha ar bill")
        assertTrue(result.matched)
        assertEquals("prepare_expense", result.intent)
        assertEquals(10000L, result.amountMinor)
        assertEquals("Food", result.category)
    }

    @Test
    fun testLunchExpense() {
        val result = BanglishNormalizer.tryFastDeterministicParse("ajke lunch 250 taka")
        assertTrue(result.matched)
        assertEquals("prepare_expense", result.intent)
        assertEquals(25000L, result.amountMinor)
        assertEquals("Food", result.category)
    }

    @Test
    fun testSalaryIncome() {
        val result = BanglishNormalizer.tryFastDeterministicParse("salary paisi 30000 tk")
        assertTrue(result.matched)
        assertEquals("prepare_income", result.intent)
        assertEquals(3000000L, result.amountMinor)
        assertEquals("Salary", result.category)
    }

    @Test
    fun testSalayPeyechiIncome() {
        val result = BanglishNormalizer.tryFastDeterministicParse("100 tk salay peyechi")
        assertTrue(result.matched)
        assertEquals("prepare_income", result.intent)
        assertEquals(10000L, result.amountMinor)
        assertEquals("Salary", result.category)

        val result2 = BanglishNormalizer.tryFastDeterministicParse("5000 tk bonus pelam")
        assertTrue(result2.matched)
        assertEquals("prepare_income", result2.intent)
        assertEquals(500000L, result2.amountMinor)
        assertEquals("Salary", result2.category)

        val result3 = BanglishNormalizer.tryFastDeterministicParse("100 tk paisi")
        assertTrue(result3.matched)
        assertEquals("prepare_income", result3.intent)
        assertEquals(10000L, result3.amountMinor)
        assertEquals("Salary", result3.category)
    }

    @Test
    fun testSpendingSummaryQuery() {
        val result = BanglishNormalizer.tryFastDeterministicParse("ei mashe koto khoroch hoise?")
        assertTrue(result.matched)
        assertEquals("get_spending_summary", result.intent)
    }

    @Test
    fun testReminderQuery() {
        val result = BanglishNormalizer.tryFastDeterministicParse("raat 9 tay expense entry reminder dio")
        assertTrue(result.matched)
        assertEquals("propose_reminder", result.intent)
        assertEquals(21, result.reminderHour)
        assertEquals(0, result.reminderMinute)
    }

    @Test
    fun testMultipleExpenses() {
        val input = "50 tk sosage , 320 tk murgi , 420 tk bazar"
        val result = BanglishNormalizer.tryFastDeterministicParse(input)
        assertTrue(result.matched)
        assertEquals("prepare_multiple_expenses", result.intent)
        assertEquals(3, result.multipleItems.size)

        assertEquals(5000L, result.multipleItems[0].amountMinor)
        assertEquals("sosage", result.multipleItems[0].note)
        assertEquals("Food", result.multipleItems[0].category)

        assertEquals(32000L, result.multipleItems[1].amountMinor)
        assertEquals("murgi", result.multipleItems[1].note)
        assertEquals("Grocery", result.multipleItems[1].category)

        assertEquals(42000L, result.multipleItems[2].amountMinor)
        assertEquals("bazar", result.multipleItems[2].note)
        assertEquals("Grocery", result.multipleItems[2].category)
    }

    @Test
    fun testFullMonthDataQuery() {
        val q1 = BanglishNormalizer.tryFastDeterministicParse("full month data")
        assertTrue(q1.matched)
        assertEquals("get_spending_summary", q1.intent)

        val q2 = BanglishNormalizer.tryFastDeterministicParse("month data")
        assertTrue(q2.matched)
        assertEquals("get_spending_summary", q2.intent)

        val q3 = BanglishNormalizer.tryFastDeterministicParse("all data")
        assertTrue(q3.matched)
        assertEquals("get_spending_summary", q3.intent)
    }

    @Test
    fun testTotalBusVara() {
        val q1 = BanglishNormalizer.tryFastDeterministicParse("total bus vara koto")
        assertTrue(q1.matched)
        assertEquals("get_item_total", q1.intent)
        assertEquals("bus", q1.searchQuery)

        val q2 = BanglishNormalizer.tryFastDeterministicParse("bus vara koto")
        assertTrue(q2.matched)
        assertEquals("get_item_total", q2.intent)
        assertEquals("bus", q2.searchQuery)

        val q3 = BanglishNormalizer.tryFastDeterministicParse("bus e koto gese")
        assertTrue(q3.matched)
        assertEquals("get_item_total", q3.intent)
        assertEquals("bus", q3.searchQuery)
    }

    @Test
    fun testBazarKhorochQuery() {
        val q1 = BanglishNormalizer.tryFastDeterministicParse("bazar e koto khoroch hoise")
        assertTrue(q1.matched)
        assertEquals("get_item_total", q1.intent)
        assertEquals("bazar", q1.searchQuery)

        val q2 = BanglishNormalizer.tryFastDeterministicParse("bazar khoroch koto")
        assertTrue(q2.matched)
        assertEquals("get_item_total", q2.intent)
        assertEquals("bazar", q2.searchQuery)

        val q3 = BanglishNormalizer.tryFastDeterministicParse("bazar ar koto tk gelo")
        assertTrue(q3.matched)
        assertEquals("get_item_total", q3.intent)
        assertEquals("bazar", q3.searchQuery)

        val q4 = BanglishNormalizer.tryFastDeterministicParse("bus e koto tk gelo")
        assertTrue(q4.matched)
        assertEquals("get_item_total", q4.intent)
        assertEquals("bus", q4.searchQuery)
    }

    @Test
    fun testBalanceQuery() {
        val q1 = BanglishNormalizer.tryFastDeterministicParse("amar balance koto")
        assertTrue(q1.matched)
        assertEquals("get_financial_balance", q1.intent)

        val q2 = BanglishNormalizer.tryFastDeterministicParse("koto taka baki")
        assertTrue(q2.matched)
        assertEquals("get_financial_balance", q2.intent)
    }

    @Test
    fun testPeriodSummaryQuery() {
        val q1 = BanglishNormalizer.tryFastDeterministicParse("ajke koto khoroch")
        assertTrue(q1.matched)
        assertEquals("get_period_summary", q1.intent)
        assertEquals("today", q1.searchQuery)

        val q2 = BanglishNormalizer.tryFastDeterministicParse("gotokal koto khoroch")
        assertTrue(q2.matched)
        assertEquals("get_period_summary", q2.intent)
        assertEquals("yesterday", q2.searchQuery)
    }

    @Test
    fun testHighestExpenseQuery() {
        val q1 = BanglishNormalizer.tryFastDeterministicParse("highest expense konta")
        assertTrue(q1.matched)
        assertEquals("get_highest_expense", q1.intent)

        val q2 = BanglishNormalizer.tryFastDeterministicParse("shobcheye beshi khoroch")
        assertTrue(q2.matched)
        assertEquals("get_highest_expense", q2.intent)
    }

    @Test
    fun testNaturalLanguageExpenseSentence() {
        val res1 = BanglishNormalizer.tryFastDeterministicParse("ami 200 tk diye lunch korsi")
        assertTrue(res1.matched)
        assertEquals("prepare_expense", res1.intent)
        assertEquals(20000L, res1.amountMinor)
        assertEquals("Food", res1.category)
        assertEquals("lunch", res1.note)

        val res2 = BanglishNormalizer.tryFastDeterministicParse("rickshaw vara dilam 60 tk")
        assertTrue(res2.matched)
        assertEquals("prepare_expense", res2.intent)
        assertEquals(6000L, res2.amountMinor)
        assertEquals("Transport", res2.category)
        assertEquals("rickshaw vara", res2.note)
    }

    @Test
    fun testGreetingAndHelp() {
        val g = BanglishNormalizer.tryFastDeterministicParse("hi")
        assertTrue(g.matched)
        assertEquals("get_help_or_greeting", g.intent)

        val h = BanglishNormalizer.tryFastDeterministicParse("ki korte paro")
        assertTrue(h.matched)
        assertEquals("get_help_or_greeting", h.intent)
    }

    @Test
    fun testConversationalPrefixStripping() {
        val q1 = BanglishNormalizer.tryFastDeterministicParse("can you tell me total bus vara koto")
        assertTrue(q1.matched)
        assertEquals("get_item_total", q1.intent)
        assertEquals("bus", q1.searchQuery)

        val q2 = BanglishNormalizer.tryFastDeterministicParse("bolo to amar balance koto")
        assertTrue(q2.matched)
        assertEquals("get_financial_balance", q2.intent)

        val q3 = BanglishNormalizer.tryFastDeterministicParse("please show me ajke koto khoroch")
        assertTrue(q3.matched)
        assertEquals("get_period_summary", q3.intent)
        assertEquals("today", q3.searchQuery)
    }

    @Test
    fun testPoliteNaturalLanguageExpenses() {
        val r1 = BanglishNormalizer.tryFastDeterministicParse("can you add 150 tk for medicine please")
        assertTrue(r1.matched)
        assertEquals("prepare_expense", r1.intent)
        assertEquals(15000L, r1.amountMinor)
        assertEquals("Health", r1.category)

        val r2 = BanglishNormalizer.tryFastDeterministicParse("please record 80 taka for rickshaw")
        assertTrue(r2.matched)
        assertEquals("prepare_expense", r2.intent)
        assertEquals(8000L, r2.amountMinor)
        assertEquals("Transport", r2.category)
    }

    @Test
    fun testConversationalAiHelper() {
        val resp1 = com.example.expenseassistant.ai.ConversationalAiHelper.tryConversationalResponse("who are you")
        org.junit.Assert.assertNotNull(resp1)
        assertTrue(resp1!!.contains("Gemma"))

        val resp2 = com.example.expenseassistant.ai.ConversationalAiHelper.tryConversationalResponse("how to save money")
        org.junit.Assert.assertNotNull(resp2)
        assertTrue(resp2!!.contains("50/30/20"))

        val resp3 = com.example.expenseassistant.ai.ConversationalAiHelper.tryConversationalResponse("50/30/20 rule")
        org.junit.Assert.assertNotNull(resp3)
        assertTrue(resp3!!.contains("Needs"))
    }

    @Test
    fun testComplexCompoundNaturalLanguageExpenses() {
        val input1 = "Ajke shokale 60 tk diye nasta korsi ar dupure 180 tk diye khabar khaisi"
        val r1 = BanglishNormalizer.tryFastDeterministicParse(input1)
        assertTrue(r1.matched)
        assertEquals("prepare_multiple_expenses", r1.intent)
        assertEquals(2, r1.multipleItems.size)
        assertEquals(6000L, r1.multipleItems[0].amountMinor)
        assertEquals("nasta", r1.multipleItems[0].note)
        assertEquals("Food", r1.multipleItems[0].category)
        assertEquals(18000L, r1.multipleItems[1].amountMinor)
        assertEquals("khabar", r1.multipleItems[1].note)
        assertEquals("Food", r1.multipleItems[1].category)

        val input2 = "60 tk nasta ar 180 tk lunch"
        val r2 = BanglishNormalizer.tryFastDeterministicParse(input2)
        assertTrue(r2.matched)
        assertEquals("prepare_multiple_expenses", r2.intent)
        assertEquals(2, r2.multipleItems.size)
        assertEquals(6000L, r2.multipleItems[0].amountMinor)
        assertEquals(18000L, r2.multipleItems[1].amountMinor)

        val input3 = "bazar korlam 500 taka ar rickshaw vara dilam 40 taka"
        val r3 = BanglishNormalizer.tryFastDeterministicParse(input3)
        assertTrue(r3.matched)
        assertEquals("prepare_multiple_expenses", r3.intent)
        assertEquals(2, r3.multipleItems.size)
        assertEquals(50000L, r3.multipleItems[0].amountMinor)
        assertEquals("Grocery", r3.multipleItems[0].category)
        assertEquals(4000L, r3.multipleItems[1].amountMinor)
        assertEquals("Transport", r3.multipleItems[1].category)
    }

    @Test
    fun testComplexCommuteAndJourneySentences() {
        val r1 = BanglishNormalizer.tryFastDeterministicParse("Office theke bashay ashte rickshaw te 80 taka laglo")
        assertTrue(r1.matched)
        assertEquals("prepare_expense", r1.intent)
        assertEquals(8000L, r1.amountMinor)
        assertEquals("Transport", r1.category)

        val r2 = BanglishNormalizer.tryFastDeterministicParse("basha theke office ashte 80 taka rickshaw vara dilam")
        assertTrue(r2.matched)
        assertEquals("prepare_expense", r2.intent)
        assertEquals(8000L, r2.amountMinor)
        assertEquals("Transport", r2.category)

        val r3 = BanglishNormalizer.tryFastDeterministicParse("dost er bashay jete 50 taka cng lagse")
        assertTrue(r3.matched)
        assertEquals("prepare_expense", r3.intent)
        assertEquals(5000L, r3.amountMinor)
        assertEquals("Transport", r3.category)
    }

    @Test
    fun testInvertedAndStructuredComplexExpenses() {
        val r1 = BanglishNormalizer.tryFastDeterministicParse("basha vara 12000 taka dilam ajke")
        assertTrue(r1.matched)
        assertEquals("prepare_expense", r1.intent)
        assertEquals(1200000L, r1.amountMinor)
        assertEquals("Bills", r1.category)

        val r2 = BanglishNormalizer.tryFastDeterministicParse("Medicine kinlam 450 takar")
        assertTrue(r2.matched)
        assertEquals("prepare_expense", r2.intent)
        assertEquals(45000L, r2.amountMinor)
        assertEquals("Health", r2.category)
        assertEquals("medicine", r2.note)

        val r3 = BanglishNormalizer.tryFastDeterministicParse("amader electricity bill 1850 taka paid korsi")
        assertTrue(r3.matched)
        assertEquals("prepare_expense", r3.intent)
        assertEquals(185000L, r3.amountMinor)
        assertEquals("Bills", r3.category)

        val r4 = BanglishNormalizer.tryFastDeterministicParse("dost der treat dilam 1500 taka")
        assertTrue(r4.matched)
        assertEquals("prepare_expense", r4.intent)
        assertEquals(150000L, r4.amountMinor)
        assertEquals("Food", r4.category)
    }

    @Test
    fun testComplexNaturalLanguageQueries() {
        val q1 = BanglishNormalizer.tryFastDeterministicParse("Ei mashe shobcheye beshi taka kothay khoroch hoise?")
        assertTrue(q1.matched)
        assertEquals("get_highest_expense", q1.intent)

        val q2 = BanglishNormalizer.tryFastDeterministicParse("ajke saradine amar total koto taka khoroch holo?")
        assertTrue(q2.matched)
        assertEquals("get_period_summary", q2.intent)
        assertEquals("today", q2.searchQuery)

        val q3 = BanglishNormalizer.tryFastDeterministicParse("bazar e mot koto geche?")
        assertTrue(q3.matched)
        assertEquals("get_item_total", q3.intent)
        assertEquals("bazar", q3.searchQuery)

        val q4 = BanglishNormalizer.tryFastDeterministicParse("amar rickshaw vara te total koto taka gelo?")
        assertTrue(q4.matched)
        assertEquals("get_item_total", q4.intent)
        assertEquals("rickshaw", q4.searchQuery)
    }

    @Test
    fun testMockDataRequest() {
        val q1 = BanglishNormalizer.tryFastDeterministicParse("push 1000 mock transaction on db on pixel 8")
        assertTrue(q1.matched)
        assertEquals("seed_mock_transactions", q1.intent)

        val q2 = BanglishNormalizer.tryFastDeterministicParse("push 1000 mock transaction")
        assertTrue(q2.matched)
        assertEquals("seed_mock_transactions", q2.intent)

        val q3 = BanglishNormalizer.tryFastDeterministicParse("mock data")
        assertTrue(q3.matched)
        assertEquals("seed_mock_transactions", q3.intent)
    }

    @Test
    fun testTarikhExpense() {
        val res = BanglishNormalizer.tryFastDeterministicParse("4 tarikh 100 tk sawpno bazar")
        assertTrue(res.matched)
        assertEquals("prepare_expense", res.intent)
        assertEquals(10000L, res.amountMinor)
        assertEquals("Grocery", res.category)
        assertEquals("sawpno bazar", res.note)
        org.junit.Assert.assertNotNull(res.occurredAt)

        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = res.occurredAt!!
        assertEquals(4, cal.get(java.util.Calendar.DAY_OF_MONTH))

        // Inverted: "sawpno bazar 100 tk 4 tarikh"
        val res2 = BanglishNormalizer.tryFastDeterministicParse("sawpno bazar 100 tk 4 tarikh")
        assertTrue(res2.matched)
        assertEquals("prepare_expense", res2.intent)
        assertEquals(10000L, res2.amountMinor)
        assertEquals("Grocery", res2.category)
        org.junit.Assert.assertNotNull(res2.occurredAt)

        // "4 tarikhe 250 tk lunch"
        val res3 = BanglishNormalizer.tryFastDeterministicParse("4 tarikhe 250 tk lunch")
        assertTrue(res3.matched)
        assertEquals("prepare_expense", res3.intent)
        assertEquals(25000L, res3.amountMinor)
        assertEquals("Food", res3.category)
        org.junit.Assert.assertNotNull(res3.occurredAt)

        // "gotokal 80 tk rickshaw vara"
        val res4 = BanglishNormalizer.tryFastDeterministicParse("gotokal 80 tk rickshaw vara")
        assertTrue(res4.matched)
        assertEquals("prepare_expense", res4.intent)
        assertEquals(8000L, res4.amountMinor)
        assertEquals("Transport", res4.category)
        org.junit.Assert.assertNotNull(res4.occurredAt)
    }

    @Test
    fun testMinedBanglishExpenseVariations() {
        // "4 tarek 120 tk bajar korlam" (bajar -> bazar, tarek -> tarikh)
        val r1 = BanglishNormalizer.tryFastDeterministicParse("4 tarek 120 tk bajar korlam")
        assertTrue(r1.matched)
        assertEquals("prepare_expense", r1.intent)
        assertEquals(12000L, r1.amountMinor)
        assertEquals("Grocery", r1.category)
        assertEquals("bazar", r1.note)
        org.junit.Assert.assertNotNull(r1.occurredAt)

        // "amdr 1500 tk electricity bill disi"
        val r2 = BanglishNormalizer.tryFastDeterministicParse("amdr 1500 tk electricity bill disi")
        assertTrue(r2.matched)
        assertEquals("prepare_expense", r2.intent)
        assertEquals(150000L, r2.amountMinor)
        assertEquals("Bills", r2.category)

        // "15 tarek 450 tk medicin kinsi"
        val r3 = BanglishNormalizer.tryFastDeterministicParse("15 tarek 450 tk medicin kinsi")
        assertTrue(r3.matched)
        assertEquals("prepare_expense", r3.intent)
        assertEquals(45000L, r3.amountMinor)
        assertEquals("Health", r3.category)
        org.junit.Assert.assertNotNull(r3.occurredAt)

        // "35000 tk beton payci"
        val r4 = BanglishNormalizer.tryFastDeterministicParse("35000 tk beton payci")
        assertTrue(r4.matched)
        assertEquals("prepare_income", r4.intent)
        assertEquals(3500000L, r4.amountMinor)
        assertEquals("Salary", r4.category)

        // "office ashte 60 tk riksa bada dilam"
        val r5 = BanglishNormalizer.tryFastDeterministicParse("office ashte 60 tk riksa bada dilam")
        assertTrue(r5.matched)
        assertEquals("prepare_expense", r5.intent)
        assertEquals(6000L, r5.amountMinor)
        assertEquals("Transport", r5.category)
    }

    @Test
    fun testMinedBanglishQueryVariations() {
        // "amar balance koto ase" (ase instead of ache)
        val q1 = BanglishNormalizer.tryFastDeterministicParse("amar balance koto ase")
        assertTrue(q1.matched)
        assertEquals("get_financial_balance", q1.intent)

        // "koto taka ace"
        val q2 = BanglishNormalizer.tryFastDeterministicParse("koto taka ace")
        assertTrue(q2.matched)
        assertEquals("get_financial_balance", q2.intent)

        // "bajar e koto khoros hoise" (bajar, khoros)
        val q3 = BanglishNormalizer.tryFastDeterministicParse("bajar e koto khoros hoise")
        assertTrue(q3.matched)
        assertEquals("get_item_total", q3.intent)
        assertEquals("bazar", q3.searchQuery)

        // "riksa vara te koto lagce" (riksa, lagce)
        val q4 = BanglishNormalizer.tryFastDeterministicParse("riksa vara te koto lagce")
        assertTrue(q4.matched)
        assertEquals("get_item_total", q4.intent)
        assertEquals("riksa", q4.searchQuery)

        // "ajk saradin e koto koroch holo" (ajk, koroch)
        val q5 = BanglishNormalizer.tryFastDeterministicParse("ajk saradin e koto koroch holo")
        assertTrue(q5.matched)
        assertEquals("get_period_summary", q5.intent)
        assertEquals("today", q5.searchQuery)

        // "gotokalke koto khoros holo" (gotokalke, khoros)
        val q6 = BanglishNormalizer.tryFastDeterministicParse("gotokalke koto khoros holo")
        assertTrue(q6.matched)
        assertEquals("get_period_summary", q6.intent)
        assertEquals("yesterday", q6.searchQuery)

        // "sobcheye beshi khoros konta" (sobcheye, khoros)
        val q7 = BanglishNormalizer.tryFastDeterministicParse("sobcheye beshi khoros konta")
        assertTrue(q7.matched)
        assertEquals("get_highest_expense", q7.intent)
    }
}
