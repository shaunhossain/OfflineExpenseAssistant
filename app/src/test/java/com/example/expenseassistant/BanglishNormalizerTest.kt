package com.example.expenseassistant

import com.example.expenseassistant.ai.BanglishNormalizer
import com.example.expenseassistant.ai.BanglishTransliterator
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
    fun testLunchTotalCostThisWeek() {
        val q1 = BanglishNormalizer.tryFastDeterministicParse("lunch a total koto cost hoiche ai week a")
        assertTrue(q1.matched)
        assertEquals("get_item_total", q1.intent)
        assertEquals("lunch", q1.searchQuery)
        assertEquals("week", q1.note)

        val q2 = BanglishNormalizer.tryFastDeterministicParse("lunch a koto cost hoiche")
        assertTrue(q2.matched)
        assertEquals("get_item_total", q2.intent)
        assertEquals("lunch", q2.searchQuery)

        val q3 = BanglishNormalizer.tryFastDeterministicParse("ai week a lunch a total koto cost hoiche")
        assertTrue(q3.matched)
        assertEquals("get_item_total", q3.intent)
        assertEquals("lunch", q3.searchQuery)
        assertEquals("week", q3.note)

        val q4 = BanglishNormalizer.tryFastDeterministicParse("bazar e koto khoroch hoise ai mashe")
        assertTrue(q4.matched)
        assertEquals("get_item_total", q4.intent)
        assertEquals("bazar", q4.searchQuery)
        assertEquals("month", q4.note)

        val q5 = BanglishNormalizer.tryFastDeterministicParse("ai week a total koto cost hoiche")
        assertTrue(q5.matched)
        assertEquals("get_period_summary", q5.intent)
        assertEquals("week", q5.searchQuery)
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

    @Test
    fun testBengaliSpokenFractionsAndHundreds() {
        // "der tk diye cha khailam" (1.5 tk)
        val r1 = BanglishNormalizer.tryFastDeterministicParse("der tk diye cha khailam")
        assertTrue(r1.matched)
        assertEquals("prepare_expense", r1.intent)
        assertEquals(150L, r1.amountMinor)
        assertEquals("Food", r1.category)

        // "arai tk bus vara" (2.5 tk)
        val r2 = BanglishNormalizer.tryFastDeterministicParse("arai tk bus vara")
        assertTrue(r2.matched)
        assertEquals("prepare_expense", r2.intent)
        assertEquals(250L, r2.amountMinor)
        assertEquals("Transport", r2.category)

        // "sare sotero tk cha" (17.5 tk)
        val r3 = BanglishNormalizer.tryFastDeterministicParse("sare sotero tk cha")
        assertTrue(r3.matched)
        assertEquals("prepare_expense", r3.intent)
        assertEquals(1750L, r3.amountMinor)
        assertEquals("Food", r3.category)

        // "barosho tk bazar korlam" (1200 tk)
        val r4 = BanglishNormalizer.tryFastDeterministicParse("barosho tk bazar korlam")
        assertTrue(r4.matched)
        assertEquals("prepare_expense", r4.intent)
        assertEquals(120000L, r4.amountMinor)
        assertEquals("Grocery", r4.category)

        // "dersho tk nasta" (150 tk)
        val r5 = BanglishNormalizer.tryFastDeterministicParse("dersho tk nasta")
        assertTrue(r5.matched)
        assertEquals("prepare_expense", r5.intent)
        assertEquals(15000L, r5.amountMinor)
        assertEquals("Food", r5.category)

        // "araisho tk rickshaw" (250 tk)
        val r6 = BanglishNormalizer.tryFastDeterministicParse("araisho tk rickshaw")
        assertTrue(r6.matched)
        assertEquals("prepare_expense", r6.intent)
        assertEquals(25000L, r6.amountMinor)
        assertEquals("Transport", r6.category)

        // "der hajar taka lunch" (1500 tk)
        val r7 = BanglishNormalizer.tryFastDeterministicParse("der hajar taka lunch")
        assertTrue(r7.matched)
        assertEquals("prepare_expense", r7.intent)
        assertEquals(150000L, r7.amountMinor)
        assertEquals("Food", r7.category)

        // "arai hajar tk rent" (2500 tk)
        val r8 = BanglishNormalizer.tryFastDeterministicParse("arai hajar tk rent")
        assertTrue(r8.matched)
        assertEquals("prepare_expense", r8.intent)
        assertEquals(250000L, r8.amountMinor)
        assertEquals("Bills", r8.category)

        // "sare tin hajar tk bill" (3500 tk)
        val r9 = BanglishNormalizer.tryFastDeterministicParse("sare tin hajar tk bill")
        assertTrue(r9.matched)
        assertEquals("prepare_expense", r9.intent)
        assertEquals(350000L, r9.amountMinor)
        assertEquals("Bills", r9.category)

        // "sare barosho taka groceries" (1250 tk)
        val r10 = BanglishNormalizer.tryFastDeterministicParse("sare barosho taka groceries")
        assertTrue(r10.matched)
        assertEquals("prepare_expense", r10.intent)
        assertEquals(125000L, r10.amountMinor)
        assertEquals("Grocery", r10.category)

        // Bengali script: "দেড়শ টাকা নাস্তা" (150 tk)
        val r11 = BanglishNormalizer.tryFastDeterministicParse("দেড়শ টাকা নাস্তা")
        assertTrue(r11.matched)
        assertEquals("prepare_expense", r11.intent)
        assertEquals(15000L, r11.amountMinor)
        assertEquals("Food", r11.category)

        // Bengali script: "বারোশ টাকা বাজার" (1200 tk)
        val r12 = BanglishNormalizer.tryFastDeterministicParse("বারোশ টাকা বাজার")
        assertTrue(r12.matched)
        assertEquals("prepare_expense", r12.intent)
        assertEquals(120000L, r12.amountMinor)
        assertEquals("Grocery", r12.category)

        // "derso tk nasta" (150 tk)
        val r13 = BanglishNormalizer.tryFastDeterministicParse("derso tk nasta")
        assertTrue(r13.matched)
        assertEquals(15000L, r13.amountMinor)
        assertEquals("Food", r13.category)

        // "der soto tk lunch" (150 tk)
        val r14 = BanglishNormalizer.tryFastDeterministicParse("der soto tk lunch")
        assertTrue(r14.matched)
        assertEquals(15000L, r14.amountMinor)
        assertEquals("Food", r14.category)

        // "der so tk cha" (150 tk)
        val r15 = BanglishNormalizer.tryFastDeterministicParse("der so tk cha")
        assertTrue(r15.matched)
        assertEquals(15000L, r15.amountMinor)
        assertEquals("Food", r15.category)

        // "araiso tk rickshaw" (250 tk)
        val r16 = BanglishNormalizer.tryFastDeterministicParse("araiso tk rickshaw")
        assertTrue(r16.matched)
        assertEquals(25000L, r16.amountMinor)
        assertEquals("Transport", r16.category)

        // "baro soto tk bazar" (1200 tk)
        val r17 = BanglishNormalizer.tryFastDeterministicParse("baro soto tk bazar")
        assertTrue(r17.matched)
        assertEquals(120000L, r17.amountMinor)
        assertEquals("Grocery", r17.category)

        // Bengali script: "দেড় শত টাকা নাস্তা" (150 tk)
        val r18 = BanglishNormalizer.tryFastDeterministicParse("দেড় শত টাকা নাস্তা")
        assertTrue(r18.matched)
        assertEquals(15000L, r18.amountMinor)
        assertEquals("Food", r18.category)
    }

    @Test
    fun testJoinedCurrencyNormalization() {
        assertEquals("100 tk ma ke bkash korlam", BanglishNormalizer.normalize("100tk ma ke bkash korlam"))
        assertEquals("500 tk bhai k pathalam", BanglishNormalizer.normalize("500taka bhai k pathalam"))
        assertEquals("50 tk", BanglishNormalizer.normalize("50৳"))
        assertEquals("100 tk", BanglishNormalizer.normalize("১০০টাকা"))
        assertEquals("tk 200", BanglishNormalizer.normalize("tk200"))
    }

    @Test
    fun testMfsTransferComprehension() {
        val r1 = BanglishNormalizer.tryFastDeterministicParse("100tk ma ke bkash korlam")
        assertTrue(r1.matched)
        assertEquals("prepare_expense", r1.intent)
        assertEquals(10000L, r1.amountMinor)
        assertEquals("ma ke bkash", r1.note)
        assertEquals("Other", r1.category)

        val r2 = BanglishNormalizer.tryFastDeterministicParse("500taka bhai k pathalam")
        assertTrue(r2.matched)
        assertEquals("prepare_expense", r2.intent)
        assertEquals(50000L, r2.amountMinor)
        assertEquals("bhai k", r2.note)
        assertEquals("Other", r2.category)

        val r3 = BanglishNormalizer.tryFastDeterministicParse("dost k 250 tk dilam")
        assertTrue(r3.matched)
        assertEquals("prepare_expense", r3.intent)
        assertEquals(25000L, r3.amountMinor)
        assertEquals("dost k", r3.note)
        assertEquals("Other", r3.category)
    }

    @Test
    fun testMagnitudeUnitsNormalization() {
        assertEquals("50000", BanglishNormalizer.normalize("50k"))
        assertEquals("2500", BanglishNormalizer.normalize("2.5k"))
        assertEquals("50000", BanglishNormalizer.normalize("50,000"))
        assertEquals("150000", BanglishNormalizer.normalize("1.5 lakh"))
        assertEquals("200000", BanglishNormalizer.normalize("২ লাখ"))
        assertEquals("50000", BanglishNormalizer.normalize("৫০ হাজার"))
        assertEquals("10000000", BanglishNormalizer.normalize("1 crore"))

        val r1 = BanglishNormalizer.tryFastDeterministicParse("50k salary paisi")
        assertTrue(r1.matched)
        assertEquals("prepare_income", r1.intent)
        assertEquals(5000000L, r1.amountMinor)

        val r2 = BanglishNormalizer.tryFastDeterministicParse("1.5 lakh basha vara")
        assertTrue(r2.matched)
        assertEquals("prepare_expense", r2.intent)
        assertEquals(15000000L, r2.amountMinor)
        assertEquals("Bills", r2.category)

        val r3 = BanglishNormalizer.tryFastDeterministicParse("2.5k shopping")
        assertTrue(r3.matched)
        assertEquals("prepare_expense", r3.intent)
        assertEquals(250000L, r3.amountMinor)
        assertEquals("Shopping", r3.category)

        val r4 = BanglishNormalizer.tryFastDeterministicParse("২ লাখ বাসা ভাড়া")
        assertTrue(r4.matched)
        assertEquals("prepare_expense", r4.intent)
        assertEquals(20000000L, r4.amountMinor)
        assertEquals("Bills", r4.category)
    }

    @Test
    fun testImplicitAmountParsing() {
        // "bus vara 50" without currency symbol
        val r1 = BanglishNormalizer.tryFastDeterministicParse("bus vara 50")
        assertTrue(r1.matched)
        assertEquals("prepare_expense", r1.intent)
        assertEquals(5000L, r1.amountMinor)
        assertEquals("Transport", r1.category)

        // "lunch 180" without currency symbol
        val r2 = BanglishNormalizer.tryFastDeterministicParse("lunch 180")
        assertTrue(r2.matched)
        assertEquals("prepare_expense", r2.intent)
        assertEquals(18000L, r2.amountMinor)
        assertEquals("Food", r2.category)

        // "30 rickshaw" without currency symbol
        val r3 = BanglishNormalizer.tryFastDeterministicParse("30 rickshaw")
        assertTrue(r3.matched)
        assertEquals("prepare_expense", r3.intent)
        assertEquals(3000L, r3.amountMinor)
        assertEquals("Transport", r3.category)

        // Non-financial bare number sentence must NOT be matched as an expense
        val r4 = BanglishNormalizer.tryFastDeterministicParse("amra 3 jon")
        org.junit.Assert.assertFalse(r4.matched)
    }

    @Test
    fun testLocalizedMerchantAndUtilityCategories() {
        val r1 = BanglishNormalizer.tryFastDeterministicParse("foodpanda 450")
        assertTrue(r1.matched)
        assertEquals("Food", r1.category)
        assertEquals(45000L, r1.amountMinor)

        val r2 = BanglishNormalizer.tryFastDeterministicParse("pathao food 350")
        assertTrue(r2.matched)
        assertEquals("Food", r2.category)
        assertEquals(35000L, r2.amountMinor)

        val r3 = BanglishNormalizer.tryFastDeterministicParse("pathao 120")
        assertTrue(r3.matched)
        assertEquals("Transport", r3.category)
        assertEquals(12000L, r3.amountMinor)

        val r4 = BanglishNormalizer.tryFastDeterministicParse("chaldal 1200")
        assertTrue(r4.matched)
        assertEquals("Grocery", r4.category)
        assertEquals(120000L, r4.amountMinor)

        val r5 = BanglishNormalizer.tryFastDeterministicParse("desco bill 2300")
        assertTrue(r5.matched)
        assertEquals("Bills", r5.category)
        assertEquals(230000L, r5.amountMinor)

        val r6 = BanglishNormalizer.tryFastDeterministicParse("wasa 600")
        assertTrue(r6.matched)
        assertEquals("Bills", r6.category)
        assertEquals(60000L, r6.amountMinor)

        val r7 = BanglishNormalizer.tryFastDeterministicParse("titas gas bill 1080")
        assertTrue(r7.matched)
        assertEquals("Bills", r7.category)
        assertEquals(108000L, r7.amountMinor)

        val r8 = BanglishNormalizer.tryFastDeterministicParse("daraz 1500")
        assertTrue(r8.matched)
        assertEquals("Shopping", r8.category)
        assertEquals(150000L, r8.amountMinor)
    }

    @Test
    fun testMinedCorpusVariants() {
        val r1 = BanglishNormalizer.tryFastDeterministicParse("bekash 500 tk ma k delam")
        assertTrue(r1.matched)
        assertEquals("prepare_expense", r1.intent)
        assertEquals(50000L, r1.amountMinor)

        val r2 = BanglishNormalizer.tryFastDeterministicParse("nogod 250 tk recharge")
        assertTrue(r2.matched)
        assertEquals("prepare_expense", r2.intent)
        assertEquals(25000L, r2.amountMinor)
        assertEquals("Bills", r2.category)

        val r3 = BanglishNormalizer.tryFastDeterministicParse("shingara 40")
        assertTrue(r3.matched)
        assertEquals("prepare_expense", r3.intent)
        assertEquals(4000L, r3.amountMinor)
        assertEquals("Food", r3.category)

        val r4 = BanglishNormalizer.tryFastDeterministicParse("porota 30")
        assertTrue(r4.matched)
        assertEquals("prepare_expense", r4.intent)
        assertEquals(3000L, r4.amountMinor)
        assertEquals("Food", r4.category)

        val r5 = BanglishNormalizer.tryFastDeterministicParse("fuska 60")
        assertTrue(r5.matched)
        assertEquals("prepare_expense", r5.intent)
        assertEquals(6000L, r5.amountMinor)
        assertEquals("Food", r5.category)

        val r6 = BanglishNormalizer.tryFastDeterministicParse("500 tk daktar fee")
        assertTrue(r6.matched)
        assertEquals("prepare_expense", r6.intent)
        assertEquals(50000L, r6.amountMinor)
        assertEquals("Health", r6.category)

        val r7 = BanglishNormalizer.tryFastDeterministicParse("biddut bill 1500")
        assertTrue(r7.matched)
        assertEquals("prepare_expense", r7.intent)
        assertEquals(150000L, r7.amountMinor)
        assertEquals("Bills", r7.category)

        val r8 = BanglishNormalizer.tryFastDeterministicParse("chaal 55")
        assertTrue(r8.matched)
        assertEquals("prepare_expense", r8.intent)
        assertEquals(5500L, r8.amountMinor)
        assertEquals("Grocery", r8.category)

        val r9 = BanglishNormalizer.tryFastDeterministicParse("deem 140")
        assertTrue(r9.matched)
        assertEquals("prepare_expense", r9.intent)
        assertEquals(14000L, r9.amountMinor)
        assertEquals("Grocery", r9.category)

        val r10 = BanglishNormalizer.tryFastDeterministicParse("piyaj 80")
        assertTrue(r10.matched)
        assertEquals("prepare_expense", r10.intent)
        assertEquals(8000L, r10.amountMinor)
        assertEquals("Grocery", r10.category)
    }

    @Test
    fun testTransliterator() {
        assertEquals("টাকা", com.example.expenseassistant.ai.BanglishTransliterator.transliterateWord("taka"))
        assertEquals("খরচ", com.example.expenseassistant.ai.BanglishTransliterator.transliterateWord("khoroch"))
        assertEquals("বিকাশ", com.example.expenseassistant.ai.BanglishTransliterator.transliterateWord("bkash"))
        assertEquals("বাজার", com.example.expenseassistant.ai.BanglishTransliterator.transliterateWord("bazar"))
        assertEquals("ভাড়া", com.example.expenseassistant.ai.BanglishTransliterator.transliterateWord("vara"))

        val sentence = com.example.expenseassistant.ai.BanglishTransliterator.transliterateSentence("50 tk bus vara")
        assertEquals("50 টাকা বাস ভাড়া", sentence)
    }

    @Test
    fun testDeterministicLifecycleCommands() {
        val r1 = BanglishNormalizer.tryFastDeterministicParse("last expense delete koro")
        assertTrue(r1.matched)
        assertEquals("delete_last_transaction", r1.intent)

        val r2 = BanglishNormalizer.tryFastDeterministicParse("delete last transaction")
        assertTrue(r2.matched)
        assertEquals("delete_last_transaction", r2.intent)

        val r3 = BanglishNormalizer.tryFastDeterministicParse("delete transaction 5")
        assertTrue(r3.matched)
        assertEquals("delete_transaction", r3.intent)
        assertEquals(5L, r3.id)

        val r4 = BanglishNormalizer.tryFastDeterministicParse("transaction 12 delete koro")
        assertTrue(r4.matched)
        assertEquals("delete_transaction", r4.intent)
        assertEquals(12L, r4.id)

        val r5 = BanglishNormalizer.tryFastDeterministicParse("clear all transactions")
        assertTrue(r5.matched)
        assertEquals("clear_all_transactions", r5.intent)

        val r6 = BanglishNormalizer.tryFastDeterministicParse("shob transaction delete koro")
        assertTrue(r6.matched)
        assertEquals("clear_all_transactions", r6.intent)

        val r7 = BanglishNormalizer.tryFastDeterministicParse("recent transactions dekhao")
        assertTrue(r7.matched)
        assertEquals("get_recent_transactions", r7.intent)
        assertEquals(10, r7.limit)

        val r8 = BanglishNormalizer.tryFastDeterministicParse("recent 5 ta transaction dekhao")
        assertTrue(r8.matched)
        assertEquals("get_recent_transactions", r8.intent)
        assertEquals(5, r8.limit)

        val r9 = BanglishNormalizer.tryFastDeterministicParse("reminder gula dekhao")
        assertTrue(r9.matched)
        assertEquals("list_reminders", r9.intent)

        val r10 = BanglishNormalizer.tryFastDeterministicParse("delete reminder 2")
        assertTrue(r10.matched)
        assertEquals("delete_reminder", r10.intent)
        assertEquals(2L, r10.id)

        val r11 = BanglishNormalizer.tryFastDeterministicParse("reminder 1 bondho koro")
        assertTrue(r11.matched)
        assertEquals("toggle_reminder", r11.intent)
        assertEquals(1L, r11.id)
        assertEquals(false, r11.isActive)

        val r12 = BanglishNormalizer.tryFastDeterministicParse("reminder 1 chalu koro")
        assertTrue(r12.matched)
        assertEquals("toggle_reminder", r12.intent)
        assertEquals(1L, r12.id)
        assertEquals(true, r12.isActive)

        val r13 = BanglishNormalizer.tryFastDeterministicParse("db stats dekhao")
        assertTrue(r13.matched)
        assertEquals("get_database_stats", r13.intent)
    }

    @Test
    fun testPersonToPersonExpenses() {
        val r1 = BanglishNormalizer.tryFastDeterministicParse("rahim ke 500 tk dilam")
        assertTrue(r1.matched)
        assertEquals("prepare_expense", r1.intent)
        assertEquals(50000L, r1.amountMinor)
        assertEquals("rahim", r1.note)

        val r2 = BanglishNormalizer.tryFastDeterministicParse("rahim k 500")
        assertTrue(r2.matched)
        assertEquals("prepare_expense", r2.intent)
        assertEquals(50000L, r2.amountMinor)
        assertEquals("rahim", r2.note)

        val r3 = BanglishNormalizer.tryFastDeterministicParse("rahim 500 tk")
        assertTrue(r3.matched)
        assertEquals("prepare_expense", r3.intent)
        assertEquals(50000L, r3.amountMinor)
        assertEquals("rahim", r3.note)

        val r4 = BanglishNormalizer.tryFastDeterministicParse("rahim 500")
        assertTrue(r4.matched)
        assertEquals("prepare_expense", r4.intent)
        assertEquals(50000L, r4.amountMinor)
        assertEquals("rahim", r4.note)

        val r5 = BanglishNormalizer.tryFastDeterministicParse("rahimke 500")
        assertTrue(r5.matched)
        assertEquals("prepare_expense", r5.intent)
        assertEquals(50000L, r5.amountMinor)
        assertEquals("rahim", r5.note)

        val r6 = BanglishNormalizer.tryFastDeterministicParse("sabbir k 200")
        assertTrue(r6.matched)
        assertEquals("prepare_expense", r6.intent)
        assertEquals(20000L, r6.amountMinor)
        assertEquals("sabbir", r6.note)

        val r7 = BanglishNormalizer.tryFastDeterministicParse("tanvir 1000")
        assertTrue(r7.matched)
        assertEquals("prepare_expense", r7.intent)
        assertEquals(100000L, r7.amountMinor)
        assertEquals("tanvir", r7.note)
    }

    @Test
    fun testPersonIncomeAndTransfers() {
        val r1 = BanglishNormalizer.tryFastDeterministicParse("rahim 500 taka dilo")
        assertTrue(r1.matched)
        assertEquals("prepare_income", r1.intent)
        assertEquals(50000L, r1.amountMinor)
        assertEquals("Rahim", r1.note)

        val r2 = BanglishNormalizer.tryFastDeterministicParse("karim er theke 1000 tk paisi")
        assertTrue(r2.matched)
        assertEquals("prepare_income", r2.intent)
        assertEquals(100000L, r2.amountMinor)
        assertEquals("Karim", r2.note)

        val r3 = BanglishNormalizer.tryFastDeterministicParse("karim er kach theke 1000 tk pelam")
        assertTrue(r3.matched)
        assertEquals("prepare_income", r3.intent)
        assertEquals(100000L, r3.amountMinor)
        assertEquals("Karim", r3.note)

        val r4 = BanglishNormalizer.tryFastDeterministicParse("abbu 2000 tk pathaise")
        assertTrue(r4.matched)
        assertEquals("prepare_income", r4.intent)
        assertEquals(200000L, r4.amountMinor)
        assertEquals("Abbu", r4.note)
    }

    @Test
    fun testPersonQueries() {
        val r1 = BanglishNormalizer.tryFastDeterministicParse("rahim k koto disi")
        assertTrue(r1.matched)
        assertEquals("get_item_total", r1.intent)
        assertEquals("rahim", r1.searchQuery)

        val r2 = BanglishNormalizer.tryFastDeterministicParse("rahim ke total koto dilam")
        assertTrue(r2.matched)
        assertEquals("get_item_total", r2.intent)
        assertEquals("rahim", r2.searchQuery)

        val r3 = BanglishNormalizer.tryFastDeterministicParse("rahim er hisab")
        assertTrue(r3.matched)
        assertEquals("get_item_total", r3.intent)
        assertEquals("rahim", r3.searchQuery)

        val r4 = BanglishNormalizer.tryFastDeterministicParse("tanvir er hisab")
        assertTrue(r4.matched)
        assertEquals("get_item_total", r4.intent)
        assertEquals("tanvir", r4.searchQuery)
    }

    @Test
    fun testBanglishTransliteratorNamesAndInflections() {
        assertEquals("রহিম", BanglishTransliterator.transliterateWord("rahim"))
        assertEquals("তানভীর", BanglishTransliterator.transliterateWord("tanvir"))
        assertEquals("সাকিব", BanglishTransliterator.transliterateWord("sakib"))
        assertEquals("রহিমকে", BanglishTransliterator.transliterateWord("rahimke"))
        assertEquals("তানভীরএর", BanglishTransliterator.transliterateWord("tanvirer"))

        val sentence = BanglishTransliterator.transliterateSentence("rahimke 500 tk dilam")
        assertEquals("রহিমকে 500 টাকা দিলাম", sentence)
    }

    @Test
    fun testBanglishPhoneticProfilerAndEnglishGlossBridge() {
        assertEquals(
            BanglishTransliterator.getBanglishPhoneticProfile("vara"),
            BanglishTransliterator.getBanglishPhoneticProfile("bhara")
        )
        assertEquals(
            BanglishTransliterator.getBanglishPhoneticProfile("rickshaw"),
            BanglishTransliterator.getBanglishPhoneticProfile("riksa")
        )

        val canonicalRiksa = BanglishTransliterator.resolveCanonicalBanglishWord("riksa")
        assertEquals("rickshaw", canonicalRiksa)

        val gloss = BanglishTransliterator.translateToEnglishGloss("50 tk riksa bhara")
        assertTrue(gloss.contains("rickshaw"))
        assertTrue(gloss.contains("fare"))
    }

    @Test
    fun testStandaloneSpokenNumbersAndHybridBengaliScript() {
        // Standalone spoken tens before tk
        val r1 = BanglishNormalizer.tryFastDeterministicParse("ponchash tk bus vara")
        assertTrue(r1.matched)
        assertEquals("prepare_expense", r1.intent)
        assertEquals(5000L, r1.amountMinor)
        assertEquals("Transport", r1.category)

        val r2 = BanglishNormalizer.tryFastDeterministicParse("shottor tk riksa bhara")
        assertTrue(r2.matched)
        assertEquals("prepare_expense", r2.intent)
        assertEquals(7000L, r2.amountMinor)
        assertEquals("Transport", r2.category)

        // Hybrid Bengali script + Banglish query
        val q1 = BanglishNormalizer.tryFastDeterministicParse("আজকে কত খরচ")
        assertTrue(q1.matched)
        assertEquals("get_period_summary", q1.intent)
        assertEquals("today", q1.searchQuery)
    }

    @Test
    fun testRegionalDialectCurrencyAndVerbs() {
        // "50 teha bus vara" (regional currency: teha -> tk)
        val r1 = BanglishNormalizer.tryFastDeterministicParse("50 teha bus vara")
        assertTrue(r1.matched)
        assertEquals("prepare_expense", r1.intent)
        assertEquals(5000L, r1.amountMinor)
        assertEquals("Transport", r1.category)

        // "350 tyaha foodpanda khabar"
        val r2 = BanglishNormalizer.tryFastDeterministicParse("350 tyaha foodpanda khabar")
        assertTrue(r2.matched)
        assertEquals("prepare_expense", r2.intent)
        assertEquals(35000L, r2.amountMinor)
        assertEquals("Food", r2.category)

        // "office jaite 60 tk riksa bada diasilam" (regional verb diasilam + bada)
        val r3 = BanglishNormalizer.tryFastDeterministicParse("office jaite 60 tk riksa bada diasilam")
        assertTrue(r3.matched)
        assertEquals("prepare_expense", r3.intent)
        assertEquals(6000L, r3.amountMinor)
        assertEquals("Transport", r3.category)

        // "daraz delivery charge 80 tk"
        val r4 = BanglishNormalizer.tryFastDeterministicParse("daraz delivery charge 80 tk")
        assertTrue(r4.matched)
        assertEquals("prepare_expense", r4.intent)
        assertEquals(8000L, r4.amountMinor)
        assertEquals("Shopping", r4.category)

        // "dining bill 2500 tk"
        val r5 = BanglishNormalizer.tryFastDeterministicParse("dining bill 2500 tk")
        assertTrue(r5.matched)
        assertEquals("prepare_expense", r5.intent)
        assertEquals(250000L, r5.amountMinor)
        assertEquals("Food", r5.category)

        // "kisti dilam 1500 tk"
        val r6 = BanglishNormalizer.tryFastDeterministicParse("kisti dilam 1500 tk")
        assertTrue(r6.matched)
        assertEquals("prepare_expense", r6.intent)
        assertEquals(150000L, r6.amountMinor)
        assertEquals("Bills", r6.category)

        // "brta fee 2500 tk"
        val r7 = BanglishNormalizer.tryFastDeterministicParse("brta fee 2500 tk")
        assertTrue(r7.matched)
        assertEquals("prepare_expense", r7.intent)
        assertEquals(250000L, r7.amountMinor)
        assertEquals("Transport", r7.category)

        // "bike servicing 1200 tega" (tega currency)
        val r8 = BanglishNormalizer.tryFastDeterministicParse("bike servicing 1200 tega")
        assertTrue(r8.matched)
        assertEquals("prepare_expense", r8.intent)
        assertEquals(120000L, r8.amountMinor)
        assertEquals("Transport", r8.category)

        // "mobile display change 1500 tk"
        val r9 = BanglishNormalizer.tryFastDeterministicParse("mobile display change 1500 tk")
        assertTrue(r9.matched)
        assertEquals("prepare_expense", r9.intent)
        assertEquals(150000L, r9.amountMinor)
        assertEquals("Shopping", r9.category)

        // "ecg test 500 tk"
        val r10 = BanglishNormalizer.tryFastDeterministicParse("ecg test 500 tk")
        assertTrue(r10.matched)
        assertEquals("prepare_expense", r10.intent)
        assertEquals(50000L, r10.amountMinor)
        assertEquals("Health", r10.category)

        // "gym fee 1500 tk"
        val r11 = BanglishNormalizer.tryFastDeterministicParse("gym fee 1500 tk")
        assertTrue(r11.matched)
        assertEquals("prepare_expense", r11.intent)
        assertEquals(150000L, r11.amountMinor)
        assertEquals("Entertainment", r11.category)

        // "domain renewal 1200 tk"
        val r12 = BanglishNormalizer.tryFastDeterministicParse("domain renewal 1200 tk")
        assertTrue(r12.matched)
        assertEquals("prepare_expense", r12.intent)
        assertEquals(120000L, r12.amountMinor)
        assertEquals("Bills", r12.category)

        // "pickup vara 1500 tk"
        val r13 = BanglishNormalizer.tryFastDeterministicParse("pickup vara 1500 tk")
        assertTrue(r13.matched)
        assertEquals("prepare_expense", r13.intent)
        assertEquals(150000L, r13.amountMinor)
        assertEquals("Transport", r13.category)

        // "diaper 850 tk"
        val r14 = BanglishNormalizer.tryFastDeterministicParse("diaper 850 tk")
        assertTrue(r14.matched)
        assertEquals("prepare_expense", r14.intent)
        assertEquals(85000L, r14.amountMinor)
        assertEquals("Shopping", r14.category)

        // "ukil fee 3000 tk"
        val r15 = BanglishNormalizer.tryFastDeterministicParse("ukil fee 3000 tk")
        assertTrue(r15.matched)
        assertEquals("prepare_expense", r15.intent)
        assertEquals(300000L, r15.amountMinor)
        assertEquals("Bills", r15.category)

        // "biye stage 15000 tk"
        val r16 = BanglishNormalizer.tryFastDeterministicParse("biye stage 15000 tk")
        assertTrue(r16.matched)
        assertEquals("prepare_expense", r16.intent)
        assertEquals(1500000L, r16.amountMinor)
        assertEquals("Entertainment", r16.category)

        // "harpic 220 tk"
        val r17 = BanglishNormalizer.tryFastDeterministicParse("harpic 220 tk")
        assertTrue(r17.matched)
        assertEquals("prepare_expense", r17.intent)
        assertEquals(22000L, r17.amountMinor)
        assertEquals("Grocery", r17.category)

        // "borhani 120 tk"
        val r18 = BanglishNormalizer.tryFastDeterministicParse("borhani 120 tk")
        assertTrue(r18.matched)
        assertEquals("prepare_expense", r18.intent)
        assertEquals(12000L, r18.amountMinor)
        assertEquals("Food", r18.category)

        // "traffic fine 1000 tk"
        val r19 = BanglishNormalizer.tryFastDeterministicParse("traffic fine 1000 tk")
        assertTrue(r19.matched)
        assertEquals("prepare_expense", r19.intent)
        assertEquals(100000L, r19.amountMinor)
        assertEquals("Transport", r19.category)

        // "turf vara 2500 tk"
        val r20 = BanglishNormalizer.tryFastDeterministicParse("turf vara 2500 tk")
        assertTrue(r20.matched)
        assertEquals("prepare_expense", r20.intent)
        assertEquals(250000L, r20.amountMinor)
        assertEquals("Entertainment", r20.category)

        // "darji bill 650 tk"
        val r21 = BanglishNormalizer.tryFastDeterministicParse("darji bill 650 tk")
        assertTrue(r21.matched)
        assertEquals("prepare_expense", r21.intent)
        assertEquals(65000L, r21.amountMinor)
        assertEquals("Shopping", r21.category)

        // "laundry bill 350 tk"
        val r22 = BanglishNormalizer.tryFastDeterministicParse("laundry bill 350 tk")
        assertTrue(r22.matched)
        assertEquals("prepare_expense", r22.intent)
        assertEquals(35000L, r22.amountMinor)
        assertEquals("Bills", r22.category)

        // "form fillup 1200 tk"
        val r23 = BanglishNormalizer.tryFastDeterministicParse("form fillup 1200 tk")
        assertTrue(r23.matched)
        assertEquals("prepare_expense", r23.intent)
        assertEquals(120000L, r23.amountMinor)
        assertEquals("Education", r23.category)

        // "salami dilam 500 tk"
        val r24 = BanglishNormalizer.tryFastDeterministicParse("salami dilam 500 tk")
        assertTrue(r24.matched)
        assertEquals("prepare_expense", r24.intent)
        assertEquals(50000L, r24.amountMinor)
        assertEquals("Other", r24.category)

        // "salami paisi 1000 tk" (Income)
        val r25 = BanglishNormalizer.tryFastDeterministicParse("salami paisi 1000 tk")
        assertTrue(r25.matched)
        assertEquals("prepare_income", r25.intent)
        assertEquals(100000L, r25.amountMinor)
        assertEquals("Salary", r25.category)

        // "cement er bosta 520 tk"
        val r26 = BanglishNormalizer.tryFastDeterministicParse("cement er bosta 520 tk")
        assertTrue(r26.matched)
        assertEquals("prepare_expense", r26.intent)
        assertEquals(52000L, r26.amountMinor)
        assertEquals("Bills", r26.category)

        // "rajmistri hajira 800 tk"
        val r27 = BanglishNormalizer.tryFastDeterministicParse("rajmistri hajira 800 tk")
        assertTrue(r27.matched)
        assertEquals("prepare_expense", r27.intent)
        assertEquals(80000L, r27.amountMinor)
        assertEquals("Bills", r27.category)

        // "biman ticket 4500 tk"
        val r28 = BanglishNormalizer.tryFastDeterministicParse("biman ticket 4500 tk")
        assertTrue(r28.matched)
        assertEquals("prepare_expense", r28.intent)
        assertEquals(450000L, r28.amountMinor)
        assertEquals("Transport", r28.category)

        // "bike wash 150 tk"
        val r29 = BanglishNormalizer.tryFastDeterministicParse("bike wash 150 tk")
        assertTrue(r29.matched)
        assertEquals("prepare_expense", r29.intent)
        assertEquals(15000L, r29.amountMinor)
        assertEquals("Transport", r29.category)

        // "gorur bhushi 1800 tk"
        val r30 = BanglishNormalizer.tryFastDeterministicParse("gorur bhushi 1800 tk")
        assertTrue(r30.matched)
        assertEquals("prepare_expense", r30.intent)
        assertEquals(180000L, r30.amountMinor)
        assertEquals("Grocery", r30.category)

        // "kambal kinlam 2200 tk"
        val r31 = BanglishNormalizer.tryFastDeterministicParse("kambal kinlam 2200 tk")
        assertTrue(r31.matched)
        assertEquals("prepare_expense", r31.intent)
        assertEquals(220000L, r31.amountMinor)
        assertEquals("Shopping", r31.category)

        // "gamca medical fee 8500 tk"
        val r32 = BanglishNormalizer.tryFastDeterministicParse("gamca medical fee 8500 tk")
        assertTrue(r32.matched)
        assertEquals("prepare_expense", r32.intent)
        assertEquals(850000L, r32.amountMinor)
        assertEquals("Health", r32.category)

        // "bokshish 50 tk"
        val r33 = BanglishNormalizer.tryFastDeterministicParse("bokshish 50 tk")
        assertTrue(r33.matched)
        assertEquals("prepare_expense", r33.intent)
        assertEquals(5000L, r33.amountMinor)
        assertEquals("Other", r33.category)

        // "bike e tel vorlam 500 tk"
        val r34 = BanglishNormalizer.tryFastDeterministicParse("bike e tel vorlam 500 tk")
        assertTrue(r34.matched)
        assertEquals("prepare_expense", r34.intent)
        assertEquals(50000L, r34.amountMinor)
        assertEquals("Transport", r34.category)

        // "hair cut ar shave 250 tk"
        val r35 = BanglishNormalizer.tryFastDeterministicParse("hair cut ar shave 250 tk")
        assertTrue(r35.matched)
        assertEquals("prepare_expense", r35.intent)
        assertEquals(25000L, r35.amountMinor)
        assertEquals("Shopping", r35.category)

        // "facial koralam 1200 tk"
        val r36 = BanglishNormalizer.tryFastDeterministicParse("facial koralam 1200 tk")
        assertTrue(r36.matched)
        assertEquals("prepare_expense", r36.intent)
        assertEquals(120000L, r36.amountMinor)
        assertEquals("Shopping", r36.category)

        // "mehedi dilam 400 tk"
        val r37 = BanglishNormalizer.tryFastDeterministicParse("mehedi dilam 400 tk")
        assertTrue(r37.matched)
        assertEquals("prepare_expense", r37.intent)
        assertEquals(40000L, r37.amountMinor)
        assertEquals("Shopping", r37.category)

        // "homeopathy oshudh 350 tk"
        val r38 = BanglishNormalizer.tryFastDeterministicParse("homeopathy oshudh 350 tk")
        assertTrue(r38.matched)
        assertEquals("prepare_expense", r38.intent)
        assertEquals(35000L, r38.amountMinor)
        assertEquals("Health", r38.category)

        // "spiral binding 80 tk"
        val r39 = BanglishNormalizer.tryFastDeterministicParse("spiral binding 80 tk")
        assertTrue(r39.matched)
        assertEquals("prepare_expense", r39.intent)
        assertEquals(8000L, r39.amountMinor)
        assertEquals("Education", r39.category)

        // "multiplug kinlam 450 tk"
        val r40 = BanglishNormalizer.tryFastDeterministicParse("multiplug kinlam 450 tk")
        assertTrue(r40.matched)
        assertEquals("prepare_expense", r40.intent)
        assertEquals(45000L, r40.amountMinor)
        assertEquals("Shopping", r40.category)

        // "daab er pani 120 tk"
        val r41 = BanglishNormalizer.tryFastDeterministicParse("daab er pani 120 tk")
        assertTrue(r41.matched)
        assertEquals("prepare_expense", r41.intent)
        assertEquals(12000L, r41.amountMinor)
        assertEquals("Food", r41.category)

        // "lichu kinlam 450 tk"
        val r42 = BanglishNormalizer.tryFastDeterministicParse("lichu kinlam 450 tk")
        assertTrue(r42.matched)
        assertEquals("prepare_expense", r42.intent)
        assertEquals(45000L, r42.amountMinor)
        assertEquals("Grocery", r42.category)

        // "koshai hajira 3000 tk"
        val r43 = BanglishNormalizer.tryFastDeterministicParse("koshai hajira 3000 tk")
        assertTrue(r43.matched)
        assertEquals("prepare_expense", r43.intent)
        assertEquals(300000L, r43.amountMinor)
        assertEquals("Bills", r43.category)

        // "savlon ar hexisol 190 tk"
        val r44 = BanglishNormalizer.tryFastDeterministicParse("savlon ar hexisol 190 tk")
        assertTrue(r44.matched)
        assertEquals("prepare_expense", r44.intent)
        assertEquals(19000L, r44.amountMinor)
        assertEquals("Health", r44.category)

        // "inhaler kinlam 220 tk"
        val r45 = BanglishNormalizer.tryFastDeterministicParse("inhaler kinlam 220 tk")
        assertTrue(r45.matched)
        assertEquals("prepare_expense", r45.intent)
        assertEquals(22000L, r45.amountMinor)
        assertEquals("Health", r45.category)

        // "khelna kinlam 350 tk"
        val r46 = BanglishNormalizer.tryFastDeterministicParse("khelna kinlam 350 tk")
        assertTrue(r46.matched)
        assertEquals("prepare_expense", r46.intent)
        assertEquals(35000L, r46.amountMinor)
        assertEquals("Shopping", r46.category)

        // "blender kinlam 2800 tk"
        val r47 = BanglishNormalizer.tryFastDeterministicParse("blender kinlam 2800 tk")
        assertTrue(r47.matched)
        assertEquals("prepare_expense", r47.intent)
        assertEquals(280000L, r47.amountMinor)
        assertEquals("Shopping", r47.category)

        // "chaka e hawa dilam 20 tk"
        val r48 = BanglishNormalizer.tryFastDeterministicParse("chaka e hawa dilam 20 tk")
        assertTrue(r48.matched)
        assertEquals("prepare_expense", r48.intent)
        assertEquals(2000L, r48.amountMinor)
        assertEquals("Transport", r48.category)

        // "bakorkhani kinlam 60 tk"
        val r49 = BanglishNormalizer.tryFastDeterministicParse("bakorkhani kinlam 60 tk")
        assertTrue(r49.matched)
        assertEquals("prepare_expense", r49.intent)
        assertEquals(6000L, r49.amountMinor)
        assertEquals("Food", r49.category)

        // "panipuri khelam 50 taka"
        val r50 = BanglishNormalizer.tryFastDeterministicParse("panipuri khelam 50 taka")
        assertTrue(r50.matched)
        assertEquals("prepare_expense", r50.intent)
        assertEquals(5000L, r50.amountMinor)
        assertEquals("Food", r50.category)

        // "tanki saf koralam 800 tk"
        val r51 = BanglishNormalizer.tryFastDeterministicParse("tanki saf koralam 800 tk")
        assertTrue(r51.matched)
        assertEquals("prepare_expense", r51.intent)
        assertEquals(80000L, r51.amountMinor)
        assertEquals("Bills", r51.category)

        // "daroan er beton 2000 tk"
        val r52 = BanglishNormalizer.tryFastDeterministicParse("daroan er beton 2000 tk")
        assertTrue(r52.matched)
        assertEquals("prepare_expense", r52.intent)
        assertEquals(200000L, r52.amountMinor)
        assertEquals("Bills", r52.category)

        // "saree fall piko 150 tk"
        val r53 = BanglishNormalizer.tryFastDeterministicParse("saree fall piko 150 tk")
        assertTrue(r53.matched)
        assertEquals("prepare_expense", r53.intent)
        assertEquals(15000L, r53.amountMinor)
        assertEquals("Shopping", r53.category)

        // "shoe polish ar selai 60 tk"
        val r54 = BanglishNormalizer.tryFastDeterministicParse("shoe polish ar selai 60 tk")
        assertTrue(r54.matched)
        assertEquals("prepare_expense", r54.intent)
        assertEquals(6000L, r54.amountMinor)
        assertEquals("Shopping", r54.category)

        // "board challenge fee 300 tk"
        val r55 = BanglishNormalizer.tryFastDeterministicParse("board challenge fee 300 tk")
        assertTrue(r55.matched)
        assertEquals("prepare_expense", r55.intent)
        assertEquals(30000L, r55.amountMinor)
        assertEquals("Education", r55.category)

        // "cbc ar urine test 850 tk"
        val r56 = BanglishNormalizer.tryFastDeterministicParse("cbc ar urine test 850 tk")
        assertTrue(r56.matched)
        assertEquals("prepare_expense", r56.intent)
        assertEquals(85000L, r56.amountMinor)
        assertEquals("Health", r56.category)

        // Native Bengali script prompts:
        // "রিকশা ভাড়া ৫০ টাকা"
        val r57 = BanglishNormalizer.tryFastDeterministicParse("রিকশা ভাড়া ৫০ টাকা")
        assertTrue(r57.matched)
        assertEquals("prepare_expense", r57.intent)
        assertEquals(5000L, r57.amountMinor)
        assertEquals("Transport", r57.category)

        // "নাস্তা খেলাম ৬০ টাকা"
        val r58 = BanglishNormalizer.tryFastDeterministicParse("নাস্তা খেলাম ৬০ টাকা")
        assertTrue(r58.matched)
        assertEquals("prepare_expense", r58.intent)
        assertEquals(6000L, r58.amountMinor)
        assertEquals("Food", r58.category)

        // "চাল ডাল কিনলাম ৫০০ টাকা"
        val r59 = BanglishNormalizer.tryFastDeterministicParse("চাল ডাল কিনলাম ৫০০ টাকা")
        assertTrue(r59.matched)
        assertEquals("prepare_expense", r59.intent)
        assertEquals(50000L, r59.amountMinor)
        assertEquals("Grocery", r59.category)

        // "বিদ্যুৎ বিল দিলাম ১২০০ টাকা"
        val r60 = BanglishNormalizer.tryFastDeterministicParse("বিদ্যুৎ বিল দিলাম ১২০০ টাকা")
        assertTrue(r60.matched)
        assertEquals("prepare_expense", r60.intent)
        assertEquals(120000L, r60.amountMinor)
        assertEquals("Bills", r60.category)

        // "ওষুধ কিনলাম ২৫০ টাকা"
        val r61 = BanglishNormalizer.tryFastDeterministicParse("ওষুধ কিনলাম ২৫০ টাকা")
        assertTrue(r61.matched)
        assertEquals("prepare_expense", r61.intent)
        assertEquals(25000L, r61.amountMinor)
        assertEquals("Health", r61.category)
    }

    @Test
    fun testHistoryAndLogQueries() {
        val q1 = BanglishNormalizer.tryFastDeterministicParse("history dekhao")
        assertTrue(q1.matched)
        assertEquals("get_recent_transactions", q1.intent)

        val q2 = BanglishNormalizer.tryFastDeterministicParse("amar history dekhao")
        assertTrue(q2.matched)
        assertEquals("get_recent_transactions", q2.intent)

        val q3 = BanglishNormalizer.tryFastDeterministicParse("transaction history")
        assertTrue(q3.matched)
        assertEquals("get_recent_transactions", q3.intent)

        val q4 = BanglishNormalizer.tryFastDeterministicParse("log dekhao")
        assertTrue(q4.matched)
        assertEquals("get_recent_transactions", q4.intent)

        val q5 = BanglishNormalizer.tryFastDeterministicParse("statement dekhao")
        assertTrue(q5.matched)
        assertEquals("get_recent_transactions", q5.intent)

        val q6 = BanglishNormalizer.tryFastDeterministicParse("monthly statement")
        assertTrue(q6.matched)
        assertEquals("get_recent_transactions", q6.intent)

        val q7 = BanglishNormalizer.tryFastDeterministicParse("khatiyan dekhao")
        assertTrue(q7.matched)
        assertEquals("get_recent_transactions", q7.intent)

        val q8 = BanglishNormalizer.tryFastDeterministicParse("ledger dekhao")
        assertTrue(q8.matched)
        assertEquals("get_recent_transactions", q8.intent)

        val q9 = BanglishNormalizer.tryFastDeterministicParse("shob transaction dekhao")
        assertTrue(q9.matched)
        assertEquals("get_recent_transactions", q9.intent)

        val q10 = BanglishNormalizer.tryFastDeterministicParse("khorocher list dekhao")
        assertTrue(q10.matched)
        assertEquals("get_recent_transactions", q10.intent)

        val q11 = BanglishNormalizer.tryFastDeterministicParse("recent 5 ta transaction")
        assertTrue(q11.matched)
        assertEquals("get_recent_transactions", q11.intent)
        assertEquals(5, q11.limit)

        val q12 = BanglishNormalizer.tryFastDeterministicParse("shesh 10 ta expense dekhao")
        assertTrue(q12.matched)
        assertEquals("get_recent_transactions", q12.intent)
        assertEquals(10, q12.limit)

        val q13 = BanglishNormalizer.tryFastDeterministicParse("search biryani")
        assertTrue(q13.matched)
        assertEquals("search_transactions", q13.intent)
        assertEquals("biryani", q13.searchQuery)

        val q14 = BanglishNormalizer.tryFastDeterministicParse("find napa")
        assertTrue(q14.matched)
        assertEquals("search_transactions", q14.intent)
        assertEquals("napa", q14.searchQuery)

        val q15 = BanglishNormalizer.tryFastDeterministicParse("khujo rickshaw")
        assertTrue(q15.matched)
        assertEquals("search_transactions", q15.intent)
        assertEquals("rickshaw", q15.searchQuery)

        val q16 = BanglishNormalizer.tryFastDeterministicParse("khuje dao medicine")
        assertTrue(q16.matched)
        assertEquals("search_transactions", q16.intent)
        assertEquals("medicine", q16.searchQuery)

        // Native Bengali script query tests
        val q17 = BanglishNormalizer.tryFastDeterministicParse("লেনদেনের ইতিহাস দেখাও")
        assertTrue(q17.matched)
        assertEquals("get_recent_transactions", q17.intent)

        val q18 = BanglishNormalizer.tryFastDeterministicParse("স্টেটমেন্ট দেখাও")
        assertTrue(q18.matched)
        assertEquals("get_recent_transactions", q18.intent)

        val q19 = BanglishNormalizer.tryFastDeterministicParse("খরচের তালিকা দেখাও")
        assertTrue(q19.matched)
        assertEquals("get_recent_transactions", q19.intent)

        val q20 = BanglishNormalizer.tryFastDeterministicParse("সার্চ নাপা")
        assertTrue(q20.matched)
        assertEquals("search_transactions", q20.intent)
        assertEquals("নাপা", q20.searchQuery)
    }

    @Test
    fun testSaveAndRecordSentencesAndSavingsQueries() {
        // Imperative save commands with prefixes
        val s1 = BanglishNormalizer.tryFastDeterministicParse("save this: 50 tk rickshaw")
        assertTrue(s1.matched)
        assertEquals("prepare_expense", s1.intent)
        assertEquals(5000L, s1.amountMinor)
        assertEquals("rickshaw", s1.note)

        val s2 = BanglishNormalizer.tryFastDeterministicParse("likhe rakho 500 taka current bill")
        assertTrue(s2.matched)
        assertEquals("prepare_expense", s2.intent)
        assertEquals(50000L, s2.amountMinor)
        assertEquals("current bill", s2.note)
        assertEquals("Bills", s2.category)

        val s3 = BanglishNormalizer.tryFastDeterministicParse("khatai tolo 300 taka bazar")
        assertTrue(s3.matched)
        assertEquals("prepare_expense", s3.intent)
        assertEquals(30000L, s3.amountMinor)
        assertEquals("bazar", s3.note)
        assertEquals("Grocery", s3.category)

        val s4 = BanglishNormalizer.tryFastDeterministicParse("note kore rakho 200 tk coffee")
        assertTrue(s4.matched)
        assertEquals("prepare_expense", s4.intent)
        assertEquals(20000L, s4.amountMinor)
        assertEquals("coffee", s4.note)

        val s5 = BanglishNormalizer.tryFastDeterministicParse("entry koro 450 taka medicine")
        assertTrue(s5.matched)
        assertEquals("prepare_expense", s5.intent)
        assertEquals(45000L, s5.amountMinor)
        assertEquals("medicine", s5.note)
        assertEquals("Health", s5.category)

        // Suffix save commands
        val s6 = BanglishNormalizer.tryFastDeterministicParse("300 taka bazar khatai tolo")
        assertTrue(s6.matched)
        assertEquals("prepare_expense", s6.intent)
        assertEquals(30000L, s6.amountMinor)
        assertEquals("bazar", s6.note)

        val s7 = BanglishNormalizer.tryFastDeterministicParse("500 taka current bill likhe rakho")
        assertTrue(s7.matched)
        assertEquals("prepare_expense", s7.intent)
        assertEquals(50000L, s7.amountMinor)
        assertEquals("current bill", s7.note)

        // Native Bengali script save commands
        val s8 = BanglishNormalizer.tryFastDeterministicParse("সেভ করো ৫০ টাকা রিকশা ভাড়া")
        assertTrue(s8.matched)
        assertEquals("prepare_expense", s8.intent)
        assertEquals(5000L, s8.amountMinor)

        val s9 = BanglishNormalizer.tryFastDeterministicParse("খাতায় তোলো ৩০০ টাকা বাজার")
        assertTrue(s9.matched)
        assertEquals("prepare_expense", s9.intent)
        assertEquals(30000L, s9.amountMinor)

        val s10 = BanglishNormalizer.tryFastDeterministicParse("লিখে রাখো ৫০০ টাকা কারেন্ট বিল")
        assertTrue(s10.matched)
        assertEquals("prepare_expense", s10.intent)
        assertEquals(50000L, s10.amountMinor)

        val s11 = BanglishNormalizer.tryFastDeterministicParse("তুলে রাখো ১৫০ টাকা দুপুরের খাবার")
        assertTrue(s11.matched)
        assertEquals("prepare_expense", s11.intent)
        assertEquals(15000L, s11.amountMinor)

        val s12 = BanglishNormalizer.tryFastDeterministicParse("হিসাব রাখো ১০০০ টাকা বাসা ভাড়া")
        assertTrue(s12.matched)
        assertEquals("prepare_expense", s12.intent)
        assertEquals(100000L, s12.amountMinor)

        // Savings queries (balance / savings accumulation)
        val sq1 = BanglishNormalizer.tryFastDeterministicParse("koto taka save holo")
        assertTrue(sq1.matched)
        assertEquals("get_financial_balance", sq1.intent)

        val sq2 = BanglishNormalizer.tryFastDeterministicParse("amar savings koto ache")
        assertTrue(sq2.matched)
        assertEquals("get_financial_balance", sq2.intent)

        val sq3 = BanglishNormalizer.tryFastDeterministicParse("koto taka jomse")
        assertTrue(sq3.matched)
        assertEquals("get_financial_balance", sq3.intent)

        val sq4 = BanglishNormalizer.tryFastDeterministicParse("koto taka bachlo")
        assertTrue(sq4.matched)
        assertEquals("get_financial_balance", sq4.intent)

        val sq5 = BanglishNormalizer.tryFastDeterministicParse("কত টাকা সঞ্চয় হলো")
        assertTrue(sq5.matched)
        assertEquals("get_financial_balance", sq5.intent)

        val sq6 = BanglishNormalizer.tryFastDeterministicParse("koto save korlam ei mashe")
        assertTrue(sq6.matched)
        assertEquals("get_financial_balance", sq6.intent)

        val sq7 = BanglishNormalizer.tryFastDeterministicParse("ei mashe koto taka jomse")
        assertTrue(sq7.matched)
        assertEquals("get_financial_balance", sq7.intent)

        val sq8 = BanglishNormalizer.tryFastDeterministicParse("koto taka sonchoy holo")
        assertTrue(sq8.matched)
        assertEquals("get_financial_balance", sq8.intent)

        val sq9 = BanglishNormalizer.tryFastDeterministicParse("কত টাকা জমলো")
        assertTrue(sq9.matched)
        assertEquals("get_financial_balance", sq9.intent)

        val sq10 = BanglishNormalizer.tryFastDeterministicParse("আমার সঞ্চয় কত আছে")
        assertTrue(sq10.matched)
        assertEquals("get_financial_balance", sq10.intent)
    }

    @Test
    fun testExtendedSearchAndQuerySentences() {
        // Prefix search
        val q1 = BanglishNormalizer.tryFastDeterministicParse("search koro medicine")
        assertTrue(q1.matched)
        assertEquals("search_transactions", q1.intent)
        assertEquals("medicine", q1.searchQuery)

        val q2 = BanglishNormalizer.tryFastDeterministicParse("khuje ber koro napa")
        assertTrue(q2.matched)
        assertEquals("search_transactions", q2.intent)
        assertEquals("napa", q2.searchQuery)

        val q3 = BanglishNormalizer.tryFastDeterministicParse("khoj koro doctor")
        assertTrue(q3.matched)
        assertEquals("search_transactions", q3.intent)
        assertEquals("doctor", q3.searchQuery)

        // Suffix search
        val q4 = BanglishNormalizer.tryFastDeterministicParse("napa search koro")
        assertTrue(q4.matched)
        assertEquals("search_transactions", q4.intent)
        assertEquals("napa", q4.searchQuery)

        val q5 = BanglishNormalizer.tryFastDeterministicParse("rickshaw khuje dao")
        assertTrue(q5.matched)
        assertEquals("search_transactions", q5.intent)
        assertEquals("rickshaw", q5.searchQuery)

        val q6 = BanglishNormalizer.tryFastDeterministicParse("medicine khujo")
        assertTrue(q6.matched)
        assertEquals("search_transactions", q6.intent)
        assertEquals("medicine", q6.searchQuery)

        val q7 = BanglishNormalizer.tryFastDeterministicParse("bazar khuje ber koro")
        assertTrue(q7.matched)
        assertEquals("search_transactions", q7.intent)
        assertEquals("bazar", q7.searchQuery)

        // Bengali script search
        val q8 = BanglishNormalizer.tryFastDeterministicParse("খুঁজে বের করো নাপা")
        assertTrue(q8.matched)
        assertEquals("search_transactions", q8.intent)
        assertEquals("নাপা", q8.searchQuery)

        val q9 = BanglishNormalizer.tryFastDeterministicParse("নাপা সার্চ করো")
        assertTrue(q9.matched)
        assertEquals("search_transactions", q9.intent)
        assertEquals("নাপা", q9.searchQuery)

        val q10 = BanglishNormalizer.tryFastDeterministicParse("রিকশা খুঁজে দাও")
        assertTrue(q10.matched)
        assertEquals("search_transactions", q10.intent)
        assertEquals("rickshaw", q10.searchQuery)

        // Item total query alternatives: "<item> er hisab dao/dekhao"
        val q11 = BanglishNormalizer.tryFastDeterministicParse("bazar er hisab dao")
        assertTrue(q11.matched)
        assertEquals("get_item_total", q11.intent)
        assertEquals("bazar", q11.searchQuery)

        val q12 = BanglishNormalizer.tryFastDeterministicParse("rickshaw er hisab dekhao")
        assertTrue(q12.matched)
        assertEquals("get_item_total", q12.intent)
        assertEquals("rickshaw", q12.searchQuery)

        val q13 = BanglishNormalizer.tryFastDeterministicParse("bazar er hisab dekhao")
        assertTrue(q13.matched)
        assertEquals("get_item_total", q13.intent)
        assertEquals("bazar", q13.searchQuery)

        // Bengali script log & statement queries
        val q14 = BanglishNormalizer.tryFastDeterministicParse("shesh 5 ta hisab dekhao")
        assertTrue(q14.matched)
        assertEquals("get_recent_transactions", q14.intent)
        assertEquals(5, q14.limit)

        val q15 = BanglishNormalizer.tryFastDeterministicParse("daily log dekhao")
        assertTrue(q15.matched)
        assertEquals("get_recent_transactions", q15.intent)

        val q16 = BanglishNormalizer.tryFastDeterministicParse("দৈনিক হিসাব দেখাও")
        assertTrue(q16.matched)
        assertEquals("get_recent_transactions", q16.intent)

        val q17 = BanglishNormalizer.tryFastDeterministicParse("দৈনিক লগ দেখাও")
        assertTrue(q17.matched)
        assertEquals("get_recent_transactions", q17.intent)

        val q18 = BanglishNormalizer.tryFastDeterministicParse("সাম্প্রতিক খরচ দেখাও")
        assertTrue(q18.matched)
        assertEquals("get_recent_transactions", q18.intent)

        // Extended balance query with oboshishto
        val q19 = BanglishNormalizer.tryFastDeterministicParse("oboshishto koto ache")
        assertTrue(q19.matched)
        assertEquals("get_financial_balance", q19.intent)

        val q20 = BanglishNormalizer.tryFastDeterministicParse("অবশিষ্ট কত টাকা আছে")
        assertTrue(q20.matched)
        assertEquals("get_financial_balance", q20.intent)
    }

    @Test
    fun testMinedEverydayExpensePatternsAcrossDomains() {
        // Multi-item with conjunctions (Banglish & Bengali)
        val m1 = BanglishNormalizer.tryFastDeterministicParse("chal 550 tk, dal 130 tk ar tel 190 tk")
        assertTrue(m1.matched)
        assertEquals("prepare_multiple_expenses", m1.intent)
        assertEquals(3, m1.multipleItems.size)
        assertEquals(55000L, m1.multipleItems[0].amountMinor)
        assertEquals(13000L, m1.multipleItems[1].amountMinor)
        assertEquals(19000L, m1.multipleItems[2].amountMinor)

        val m2 = BanglishNormalizer.tryFastDeterministicParse("20 tk cha ar 15 tk shingara")
        assertTrue(m2.matched)
        assertEquals("prepare_multiple_expenses", m2.intent)
        assertEquals(2, m2.multipleItems.size)

        val m3 = BanglishNormalizer.tryFastDeterministicParse("rickshaw 60 tk ebong lunch 220 tk")
        assertTrue(m3.matched)
        assertEquals("prepare_multiple_expenses", m3.intent)
        assertEquals(2, m3.multipleItems.size)

        val m4 = BanglishNormalizer.tryFastDeterministicParse("১০০ টাকা চাল, ২০০ টাকা ডাল আর ৫০ টাকা তেল")
        assertTrue(m4.matched)
        assertEquals("prepare_multiple_expenses", m4.intent)
        assertEquals(3, m4.multipleItems.size)

        // Household bills & utilities
        val b1 = BanglishNormalizer.tryFastDeterministicParse("current bill 1450 tk dilam")
        assertTrue(b1.matched)
        assertEquals("prepare_expense", b1.intent)
        assertEquals(145000L, b1.amountMinor)
        assertEquals("Bills", b1.category)

        val b2 = BanglishNormalizer.tryFastDeterministicParse("basha vara 16000 taka dilam")
        assertTrue(b2.matched)
        assertEquals("prepare_expense", b2.intent)
        assertEquals(1600000L, b2.amountMinor)
        assertEquals("Bills", b2.category)

        val b3 = BanglishNormalizer.tryFastDeterministicParse("wifi bill paid 600 tk")
        assertTrue(b3.matched)
        assertEquals("prepare_expense", b3.intent)
        assertEquals(60000L, b3.amountMinor)
        assertEquals("Bills", b3.category)

        val b4 = BanglishNormalizer.tryFastDeterministicParse("বিদ্যুৎ বিল দিলাম ১২৫০ টাকা")
        assertTrue(b4.matched)
        assertEquals("prepare_expense", b4.intent)
        assertEquals(125000L, b4.amountMinor)
        assertEquals("Bills", b4.category)

        // P2P Transfers & staff wages
        val t1 = BanglishNormalizer.tryFastDeterministicParse("rahim ke 500 taka dilam")
        assertTrue(t1.matched)
        assertEquals("prepare_expense", t1.intent)
        assertEquals(50000L, t1.amountMinor)
        assertEquals("rahim", t1.note)

        val t2 = BanglishNormalizer.tryFastDeterministicParse("karim k 1000 tk pathalam bkash e")
        assertTrue(t2.matched)
        assertEquals("prepare_expense", t2.intent)
        assertEquals(100000L, t2.amountMinor)

        val t3 = BanglishNormalizer.tryFastDeterministicParse("bua er beton 3000 tk dilam")
        assertTrue(t3.matched)
        assertEquals("prepare_expense", t3.intent)
        assertEquals(300000L, t3.amountMinor)
        assertEquals("Bills", t3.category)

        // Income, salary, bonuses
        val i1 = BanglishNormalizer.tryFastDeterministicParse("salary paisi 45000 tk")
        assertTrue(i1.matched)
        assertEquals("prepare_income", i1.intent)
        assertEquals(4500000L, i1.amountMinor)
        assertEquals("Salary", i1.category)

        val i2 = BanglishNormalizer.tryFastDeterministicParse("eid bonus pelam 15000 taka")
        assertTrue(i2.matched)
        assertEquals("prepare_income", i2.intent)
        assertEquals(1500000L, i2.amountMinor)

        val i3 = BanglishNormalizer.tryFastDeterministicParse("tuition fee 4000 tk pelam")
        assertTrue(i3.matched)
        assertEquals("prepare_income", i3.intent)
        assertEquals(400000L, i3.amountMinor)

        val i4 = BanglishNormalizer.tryFastDeterministicParse("বেতন পেলাম ৫০,০০০ টাকা")
        assertTrue(i4.matched)
        assertEquals("prepare_income", i4.intent)
        assertEquals(5000000L, i4.amountMinor)

        // Deletions, clearing, reminders in Bengali & Banglish
        val d1 = BanglishNormalizer.tryFastDeterministicParse("shesh hisab ta muche felo")
        assertTrue(d1.matched)
        assertEquals("delete_last_transaction", d1.intent)

        val d2 = BanglishNormalizer.tryFastDeterministicParse("last transaction batil koro")
        assertTrue(d2.matched)
        assertEquals("delete_last_transaction", d2.intent)

        val d3 = BanglishNormalizer.tryFastDeterministicParse("শেষ খরচটা মুছে ফেলো")
        assertTrue(d3.matched)
        assertEquals("delete_last_transaction", d3.intent)

        val c1 = BanglishNormalizer.tryFastDeterministicParse("shob hisab muche felo")
        assertTrue(c1.matched)
        assertEquals("clear_all_transactions", c1.intent)

        val c2 = BanglishNormalizer.tryFastDeterministicParse("সব খরচ মুছে ফেলো")
        assertTrue(c2.matched)
        assertEquals("clear_all_transactions", c2.intent)

        val r1 = BanglishNormalizer.tryFastDeterministicParse("সব রিমাইন্ডার দেখাও")
        assertTrue(r1.matched)
        assertEquals("list_reminders", r1.intent)
    }

    @Test
    fun testRegexPatternBasedQueryExtraction() {
        // 1. Search Queries with Prefix & Suffix Regex Group Captures
        val s1 = BanglishNormalizer.tryFastDeterministicParse("search biryani")
        assertTrue(s1.matched)
        assertEquals("search_transactions", s1.intent)
        assertEquals("biryani", s1.searchQuery)

        val s2 = BanglishNormalizer.tryFastDeterministicParse("find napa extra")
        assertTrue(s2.matched)
        assertEquals("search_transactions", s2.intent)
        assertEquals("napa extra", s2.searchQuery)

        val s3 = BanglishNormalizer.tryFastDeterministicParse("khujo rickshaw")
        assertTrue(s3.matched)
        assertEquals("search_transactions", s3.intent)
        assertEquals("rickshaw", s3.searchQuery)

        val s4 = BanglishNormalizer.tryFastDeterministicParse("khuje ber koro medicine")
        assertTrue(s4.matched)
        assertEquals("search_transactions", s4.intent)
        assertEquals("medicine", s4.searchQuery)

        val s5 = BanglishNormalizer.tryFastDeterministicParse("bazar search koro")
        assertTrue(s5.matched)
        assertEquals("search_transactions", s5.intent)
        assertEquals("bazar", s5.searchQuery)

        val s6 = BanglishNormalizer.tryFastDeterministicParse("uber khuje dao")
        assertTrue(s6.matched)
        assertEquals("search_transactions", s6.intent)
        assertEquals("uber", s6.searchQuery)

        // 2. Recent Transactions / Statement Limits extracted via Regex Group Captures
        val l1 = BanglishNormalizer.tryFastDeterministicParse("shesh 5 ta hisab dekhao")
        assertTrue(l1.matched)
        assertEquals("get_recent_transactions", l1.intent)
        assertEquals(5, l1.limit)

        val l2 = BanglishNormalizer.tryFastDeterministicParse("recent 20 transactions")
        assertTrue(l2.matched)
        assertEquals("get_recent_transactions", l2.intent)
        assertEquals(20, l2.limit)

        val l3 = BanglishNormalizer.tryFastDeterministicParse("last 15 expenses")
        assertTrue(l3.matched)
        assertEquals("get_recent_transactions", l3.intent)
        assertEquals(15, l3.limit)

        val l4 = BanglishNormalizer.tryFastDeterministicParse("transaction history")
        assertTrue(l4.matched)
        assertEquals("get_recent_transactions", l4.intent)
        assertEquals(10, l4.limit) // Default limit

        // 3. Entity ID Captures (Deletions & Reminders)
        val d1 = BanglishNormalizer.tryFastDeterministicParse("delete transaction #42")
        assertTrue(d1.matched)
        assertEquals("delete_transaction", d1.intent)
        assertEquals(42L, d1.id)

        val d2 = BanglishNormalizer.tryFastDeterministicParse("expense 17 muche felo")
        assertTrue(d2.matched)
        assertEquals("delete_transaction", d2.intent)
        assertEquals(17L, d2.id)

        val rem1 = BanglishNormalizer.tryFastDeterministicParse("delete reminder 3")
        assertTrue(rem1.matched)
        assertEquals("delete_reminder", rem1.intent)
        assertEquals(3L, rem1.id)

        val rem2 = BanglishNormalizer.tryFastDeterministicParse("pause reminder 2")
        assertTrue(rem2.matched)
        assertEquals("toggle_reminder", rem2.intent)
        assertEquals(2L, rem2.id)
        assertEquals(false, rem2.isActive)

        val rem3 = BanglishNormalizer.tryFastDeterministicParse("reminder 2 chalu koro")
        assertTrue(rem3.matched)
        assertEquals("toggle_reminder", rem3.intent)
        assertEquals(2L, rem3.id)
        assertEquals(true, rem3.isActive)

        // 4. Item Total & Period Group Captures
        val it1 = BanglishNormalizer.tryFastDeterministicParse("lunch a total koto cost hoiche ai week a")
        assertTrue(it1.matched)
        assertEquals("get_item_total", it1.intent)
        assertEquals("lunch", it1.searchQuery)
        assertEquals("week", it1.note)

        val it2 = BanglishNormalizer.tryFastDeterministicParse("bazar er hisab dao")
        assertTrue(it2.matched)
        assertEquals("get_item_total", it2.intent)
        assertEquals("bazar", it2.searchQuery)

        val it3 = BanglishNormalizer.tryFastDeterministicParse("rahim er hisab dekhao")
        assertTrue(it3.matched)
        assertEquals("get_item_total", it3.intent)
        assertEquals("rahim", it3.searchQuery)

        // 5. Balance & Highest Expense Regex Intent Verification
        val bal1 = BanglishNormalizer.tryFastDeterministicParse("oboshishto koto ache")
        assertTrue(bal1.matched)
        assertEquals("get_financial_balance", bal1.intent)

        val bal2 = BanglishNormalizer.tryFastDeterministicParse("amar savings koto")
        assertTrue(bal2.matched)
        assertEquals("get_financial_balance", bal2.intent)

        val hi1 = BanglishNormalizer.tryFastDeterministicParse("highest expense")
        assertTrue(hi1.matched)
        assertEquals("get_highest_expense", hi1.intent)
    }

    @Test
    fun testAntiHallucinationAndAdvancedQueryPatterns() {
        // 1. Transaction Updates (Guards against model hallucinating new expenses)
        val u1 = BanglishNormalizer.tryFastDeterministicParse("update transaction 5 to 500 tk")
        assertTrue(u1.matched)
        assertEquals("update_transaction", u1.intent)
        assertEquals(5L, u1.id)
        assertEquals(50000L, u1.amountMinor)

        val u2 = BanglishNormalizer.tryFastDeterministicParse("transaction 12 update koro 250 tk")
        assertTrue(u2.matched)
        assertEquals("update_transaction", u2.intent)
        assertEquals(12L, u2.id)
        assertEquals(25000L, u2.amountMinor)

        val u3 = BanglishNormalizer.tryFastDeterministicParse("expense #7 change koro 120 taka lunch")
        assertTrue(u3.matched)
        assertEquals("update_transaction", u3.intent)
        assertEquals(7L, u3.id)
        assertEquals(12000L, u3.amountMinor)
        assertEquals("lunch", u3.note)
        assertEquals("Food", u3.category)

        // 2. Cancellation & Negative Guardrails (Guards against model creating fake expenses)
        val c1 = BanglishNormalizer.tryFastDeterministicParse("kono khoroch hoy nai")
        assertTrue(c1.matched)
        assertEquals("get_help_or_greeting", c1.intent)
        assertEquals("cancel", c1.searchQuery)

        val c2 = BanglishNormalizer.tryFastDeterministicParse("ajke kono khoroch kori nai")
        assertTrue(c2.matched)
        assertEquals("get_help_or_greeting", c2.intent)
        assertEquals("cancel", c2.searchQuery)

        val c3 = BanglishNormalizer.tryFastDeterministicParse("bad dao")
        assertTrue(c3.matched)
        assertEquals("get_help_or_greeting", c3.intent)
        assertEquals("cancel", c3.searchQuery)

        val c4 = BanglishNormalizer.tryFastDeterministicParse("thak dorkar nai")
        assertTrue(c4.matched)
        assertEquals("get_help_or_greeting", c4.intent)
        assertEquals("cancel", c4.searchQuery)

        // 3. Spending Patterns & Trend Queries (Guards against model confusing with spending summary)
        val p1 = BanglishNormalizer.tryFastDeterministicParse("spending pattern dekhao")
        assertTrue(p1.matched)
        assertEquals("get_spending_patterns", p1.intent)

        val p2 = BanglishNormalizer.tryFastDeterministicParse("amar expense patterns kemon")
        assertTrue(p2.matched)
        assertEquals("get_spending_patterns", p2.intent)

        val p3 = BanglishNormalizer.tryFastDeterministicParse("খরচের ধরন দেখাও")
        assertTrue(p3.matched)
        assertEquals("get_spending_patterns", p3.intent)

        val p4 = BanglishNormalizer.tryFastDeterministicParse("ব্যয়ের বিশ্লেষণ")
        assertTrue(p4.matched)
        assertEquals("get_spending_patterns", p4.intent)

        // 4. Pocket, Hand Cash & Physical Balance Queries
        val b1 = BanglishNormalizer.tryFastDeterministicParse("pocket e koto taka ache")
        assertTrue(b1.matched)
        assertEquals("get_financial_balance", b1.intent)

        val b2 = BanglishNormalizer.tryFastDeterministicParse("haate koto taka ache")
        assertTrue(b2.matched)
        assertEquals("get_financial_balance", b2.intent)

        val b3 = BanglishNormalizer.tryFastDeterministicParse("cash koto ache")
        assertTrue(b3.matched)
        assertEquals("get_financial_balance", b3.intent)

        val b4 = BanglishNormalizer.tryFastDeterministicParse("টাকা পয়সার হিসাব")
        assertTrue(b4.matched)
        assertEquals("get_financial_balance", b4.intent)

        val b5 = BanglishNormalizer.tryFastDeterministicParse("পকেটে কত টাকা আছে")
        assertTrue(b5.matched)
        assertEquals("get_financial_balance", b5.intent)

        // 5. Category Queries with Category / Sector / Babod Markers
        val cat1 = BanglishNormalizer.tryFastDeterministicParse("food category te koto khoroch")
        assertTrue(cat1.matched)
        assertEquals("get_item_total", cat1.intent)
        assertEquals("food", cat1.searchQuery)

        val cat2 = BanglishNormalizer.tryFastDeterministicParse("shopping e koto gelo")
        assertTrue(cat2.matched)
        assertEquals("get_item_total", cat2.intent)
        assertEquals("shopping", cat2.searchQuery)

        val cat3 = BanglishNormalizer.tryFastDeterministicParse("medical khate koto laglo")
        assertTrue(cat3.matched)
        assertEquals("get_item_total", cat3.intent)
        assertEquals("medical", cat3.searchQuery)

        val cat4 = BanglishNormalizer.tryFastDeterministicParse("bills babod koto taka dilam")
        assertTrue(cat4.matched)
        assertEquals("get_item_total", cat4.intent)
        assertEquals("bills", cat4.searchQuery)

        // 6. Relative N-Day Statement Limit Captures
        val n1 = BanglishNormalizer.tryFastDeterministicParse("shesh 7 diner hisab dekhao")
        assertTrue(n1.matched)
        assertEquals("get_recent_transactions", n1.intent)
        assertEquals(7, n1.limit)

        val n2 = BanglishNormalizer.tryFastDeterministicParse("last 3 days khoroch")
        assertTrue(n2.matched)
        assertEquals("get_recent_transactions", n2.intent)
        assertEquals(3, n2.limit)

        // 7. Capability & Help Inquiries
        val h1 = BanglishNormalizer.tryFastDeterministicParse("what can you do")
        assertTrue(h1.matched)
        assertEquals("get_help_or_greeting", h1.intent)
        assertEquals("help", h1.searchQuery)

        val h2 = BanglishNormalizer.tryFastDeterministicParse("tumi ki ki korte paro")
        assertTrue(h2.matched)
        assertEquals("get_help_or_greeting", h2.intent)
        assertEquals("help", h2.searchQuery)

        val h3 = BanglishNormalizer.tryFastDeterministicParse("কীভাবে ব্যবহার করব")
        assertTrue(h3.matched)
        assertEquals("get_help_or_greeting", h3.intent)
        assertEquals("help", h3.searchQuery)
    }
}
