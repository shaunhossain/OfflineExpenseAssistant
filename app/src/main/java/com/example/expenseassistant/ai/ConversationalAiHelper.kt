package com.example.expenseassistant.ai

import com.example.expenseassistant.data.SpendingSummary

object ConversationalAiHelper {

    fun isAdvisoryOrCoachingQuery(input: String): Boolean {
        val raw = input.trim().lowercase()
        val norm = BanglishNormalizer.normalize(input)
        return raw.contains("save money") || raw.contains("taka bachabo") || norm.contains("tk bachabo") ||
                raw.contains("taka jomabo") || norm.contains("tk jomabo") || raw.contains("kivabe save") ||
                raw.contains("khoroch komabo") || raw.contains("khoroch komanor") ||
                raw.contains("savings") || raw.contains("shonchoy") || raw.contains("sonchoy") ||
                raw.contains("budget") || raw.contains("masik budget") ||
                raw.contains("50/30/20") || raw.contains("50 30 20") ||
                raw.contains("emergency fund") || raw.contains("emergency taka") || norm.contains("emergency tk") ||
                raw.contains("bipod er taka") || raw.contains("advice") ||
                raw.contains("poramorsho") || raw.contains("consult") || raw.contains("coaching") ||
                raw.contains("how to save") || raw.contains("keno hisab") ||
                raw.contains("how am i doing") || raw.contains("budget kemon") ||
                raw.contains("khoroch kemon") || raw.contains("hisab kemon") ||
                raw.contains("status") || raw.contains("tips") || raw.contains("upay")
    }

    fun formatDataGroundedAdvisory(summary: SpendingSummary, userInput: String): String {
        val spentTaka = summary.totalExpense.amountMinor / 100L
        val incomeTaka = summary.totalIncome.amountMinor / 100L
        val balanceTaka = summary.netBalance.amountMinor / 100L
        val topCategory = summary.categoryBreakdown.maxByOrNull { it.totalMinor }
        val topCategoryName = topCategory?.category ?: "General"
        val topCategoryTaka = (topCategory?.totalMinor ?: 0L) / 100L

        return if (incomeTaka > 0L) {
            val ratioPercent = ((spentTaka.toDouble() / incomeTaka.toDouble()) * 100).toInt()
            val coachingTip = when {
                ratioPercent < 50 -> "🎉 Excellent budgeting! You've used under 50% of your income. The remainder can accelerate your savings or emergency fund."
                ratioPercent <= 80 -> "👍 You're on track! Your largest expense is $topCategoryName (৳$topCategoryTaka). Keep an eye on it to ensure you hit your month-end goals."
                else -> "⚠️ Attention: You have spent $ratioPercent% of your monthly income. Consider pausing non-essential purchases for the rest of this month."
            }

            "📊 Your Monthly Financial Status:\n\n" +
                    "• Total Spent: ৳$spentTaka ($ratioPercent% of income)\n" +
                    "• Total Income: ৳$incomeTaka\n" +
                    "• Net Remaining: ৳$balanceTaka\n" +
                    "• Top Category: $topCategoryName (৳$topCategoryTaka)\n\n" +
                    "💡 $coachingTip"
        } else {
            "📊 Your Monthly Spending Snapshot:\n\n" +
                    "• Total Expenses: ৳$spentTaka across ${summary.transactionCount} entries\n" +
                    "• Top Category: $topCategoryName (৳$topCategoryTaka)\n\n" +
                    "💡 Tip: Record your monthly income (e.g. 'salary 30000 tk') to unlock real-time 50/30/20 budget ratio tracking!"
        }
    }

    fun isPersonaOrSmallTalk(input: String): Boolean {
        val text = BanglishNormalizer.normalize(input)
        return text.contains("who are you") || text.contains("tumi ke") || text.contains("apni ke") ||
                text.contains("what are you") || text.contains("about yourself") ||
                text.contains("tell me about you") || text.contains("introduce yourself") ||
                text.contains("who made you") || text.contains("who created you") ||
                text.contains("ke baniyeche") || text.contains("ke toiri koreche") ||
                text.contains("how are you") || text.contains("kemon acho") ||
                text.contains("kemon achen") || text.contains("ki khobor") || text.contains("ki obostha") ||
                text == "hi" || text == "hello" || text == "hey" ||
                text == "salam" || text == "assalamu alaikum" || text == "nomoshkar" ||
                text.startsWith("hi ") || text.startsWith("hello ") || text.startsWith("hey ") ||
                text.contains("thank") || text.contains("dhonnobad") || text.contains("shukriya") ||
                text == "help" || text.contains("ki korte paro") || text.contains("tomar kaj ki")
    }

    /**
     * Fallback responses when Gemma 3 engine is not yet ready (model loading, extracting, error).
     */
    fun tryOfflineFallbackResponse(userInput: String): String? {
        val text = BanglishNormalizer.normalize(userInput)

        // 1. Identity & Persona
        if (text.contains("who are you") || text.contains("tumi ke") || text.contains("apni ke") ||
            text.contains("what are you") || text.contains("about yourself") ||
            text.contains("tell me about you") || text.contains("introduce yourself")
        ) {
            return "I am your offline personal AI Expense Assistant powered by Google Gemma! 🤖\n\n" +
                    "I run 100% on your device, meaning your financial transactions, balance, and income never leave your phone.\n\n" +
                    "Here is what I can do for you:\n" +
                    "• Record expenses: '50 tk bus vara', 'lunch 250 tk', 'ami 200 tk diye biryani kheyechi'\n" +
                    "• Record multiple items: '50 tk sosage, 320 tk murgi, 420 tk bazar'\n" +
                    "• Answer questions: 'total bus vara koto', 'amar balance koto', 'ajke koto khoroch'\n" +
                    "• Financial advice: Ask me about budgeting, saving tips, or the 50/30/20 rule!"
        }

        if (text.contains("who made you") || text.contains("who created you") ||
            text.contains("ke baniyeche") || text.contains("ke toiri koreche")
        ) {
            return "I was built using Google's Gemma 3 on-device AI model and modern Android Jetpack Compose to bring private, intelligent offline expense management to Bangladesh! 🇧🇩"
        }

        // 2. Greetings & Status
        if (text.contains("how are you") || text.contains("kemon acho") ||
            text.contains("kemon achen") || text.contains("ki khobor") || text.contains("ki obostha")
        ) {
            return "I'm doing great, thank you! Ready to help you track your expenses and keep your budget in check. What would you like to record or calculate today? 😊"
        }

        if (text == "hi" || text == "hello" || text == "hey" || text == "salam" ||
            text == "assalamu alaikum" || text == "nomoshkar" ||
            text.startsWith("hi ") || text.startsWith("hello ") || text.startsWith("hey ")
        ) {
            return "Hello! 👋 I'm your offline Gemma Expense Assistant.\n" +
                    "You can talk to me naturally in English, Bangla, or Banglish.\n" +
                    "Try: '50 tk bus vara', 'total bus vara koto', or ask 'how to save money?'"
        }

        // 3. Financial Advice & Budgeting Tips
        if (text.contains("save money") || text.contains("taka bachabo") || text.contains("tk bachabo") ||
            text.contains("taka jomabo") || text.contains("tk jomabo") ||
            text.contains("khoroch komabo") || text.contains("kivabe save") || text.contains("savings tip") ||
            text.contains("shonchoy") || text.contains("sonchoy") || text.contains("how to save") || text.contains("savings advice")
        ) {
            return "💡 Smart Money-Saving Tips for Daily Life:\n\n" +
                    "1. Follow the 50/30/20 Rule:\n" +
                    "   • 50% for Needs (Rent, groceries, utilities, commute)\n" +
                    "   • 30% for Wants (Dining out, entertainment, shopping)\n" +
                    "   • 20% for Savings & Emergency fund\n\n" +
                    "2. Track Every Little Expense:\n" +
                    "   Daily chai, snacks, and rickshaw fares add up quickly! Logging them keeps you aware.\n\n" +
                    "3. Meal Planning:\n" +
                    "   Cook meals at home and pack lunch for office instead of ordering takeaway.\n\n" +
                    "4. The 24-Hour Rule:\n" +
                    "   Wait 24 hours before making any non-essential impulse purchase."
        }

        if (text.contains("50/30/20") || text.contains("50 30 20") || text.contains("budget rule") || text.contains("budget kivabe")) {
            return "📊 The 50/30/20 Budgeting Rule:\n\n" +
                    "• 50% Needs: Essential expenses like house rent, grocery bazar, bus/rickshaw fares, and bills.\n" +
                    "• 30% Wants: Shopping, entertainment, eating out with friends.\n" +
                    "• 20% Savings: Emergency savings, DPS, or investments.\n\n" +
                    "For example, if your monthly income is 40,000 BDT:\n" +
                    "• 20,000 BDT for Needs\n" +
                    "• 12,000 BDT for Wants\n" +
                    "• 8,000 BDT for Savings"
        }

        if (text.contains("emergency fund") || text.contains("emergency taka") || text.contains("emergency tk")) {
            return "🛡️ What is an Emergency Fund?\n\n" +
                    "An emergency fund is money set aside strictly for unexpected events like medical emergencies, sudden home repairs, or temporary job changes.\n\n" +
                    "Aim to build a fund covering 3 to 6 months of your essential living expenses and keep it in an easily accessible liquid account (e.g. high-yield savings or bKash savings)."
        }

        if (text.contains("why track") || text.contains("keno hisab") || text.contains("benefit of expense tracking")) {
            return "🎯 Why Tracking Expenses Matters:\n\n" +
                    "1. Stops Financial Leaks: Small unmonitored spends (tea, snacks, ride shares) often consume 20-30% of income.\n" +
                    "2. Stress-Free Month-End: No more wondering where all your salary went!\n" +
                    "3. Helps Achieve Goals: Whether buying a gadget, traveling, or investing, knowing your numbers gets you there faster."
        }

        // 4. Gratitude
        if (text.contains("thank") || text.contains("dhonnobad") || text.contains("thx") || text.contains("shukriya")) {
            return "You're very welcome! Feel free to ask anytime or log your next expense. Have a great day! ✨"
        }

        // 5. Help & Capabilities
        if (text == "help" || text.contains("ki korte paro") || text.contains("what can you do") ||
            text.contains("how to use") || text.contains("commands") || text.contains("options")
        ) {
            return "✨ Here is what you can do with your Assistant:\n\n" +
                    "📝 Log Expenses (Natural Language):\n" +
                    "• '50 tk bus vara' or 'lunch 250 tk'\n" +
                    "• 'ami 200 tk diye biryani kheyechi'\n" +
                    "• '50 tk sosage, 320 tk murgi, 420 tk bazar'\n\n" +
                    "💵 Log Income:\n" +
                    "• 'salary paisi 30000 tk'\n\n" +
                    "🔍 Ask Questions:\n" +
                    "• 'total bus vara koto'\n" +
                    "• 'bazar e koto khoroch hoise'\n" +
                    "• 'amar balance koto'\n" +
                    "• 'ajke koto khoroch'\n" +
                    "• 'highest expense konta'\n\n" +
                    "⏰ Set Reminders:\n" +
                    "• 'raat 9 tay reminder dio'\n\n" +
                    "💬 Chat & Advice:\n" +
                    "• Ask for budgeting advice, 50/30/20 rule, or savings tips!"
        }

        return null
    }

    /**
     * Backward-compatible helper method.
     */
    fun tryConversationalResponse(userInput: String): String? = tryOfflineFallbackResponse(userInput)
}

