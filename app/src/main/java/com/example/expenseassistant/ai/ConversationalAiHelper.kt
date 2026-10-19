package com.example.expenseassistant.ai

object ConversationalAiHelper {

    fun isAdvisoryOrCoachingQuery(input: String): Boolean {
        val text = input.trim().lowercase()
        return text.contains("save money") || text.contains("taka bachabo") ||
                text.contains("savings") || text.contains("shonchoy") ||
                text.contains("budget") || text.contains("50/30/20") || text.contains("50 30 20") ||
                text.contains("emergency fund") || text.contains("emergency taka") ||
                text.contains("advice") || text.contains("consult") || text.contains("coaching") ||
                text.contains("how to save") || text.contains("keno hisab") ||
                text.contains("tips")
    }

    fun isPersonaOrSmallTalk(input: String): Boolean {
        val text = input.trim().lowercase()
        return text.contains("who are you") || text.contains("tumi ke") ||
                text.contains("what are you") || text.contains("about yourself") ||
                text.contains("tell me about you") || text.contains("introduce yourself") ||
                text.contains("who made you") || text.contains("who created you") ||
                text.contains("ke baniyeche") || text.contains("how are you") ||
                text.contains("kemon acho") || text.contains("ki khobor") ||
                text == "hi" || text == "hello" || text == "hey" ||
                text.startsWith("hi ") || text.startsWith("hello ") || text.startsWith("hey ") ||
                text.contains("thank") || text.contains("dhonnobad")
    }

    /**
     * Fallback responses when Gemma 3 engine is not yet ready (model loading, extracting, error).
     */
    fun tryOfflineFallbackResponse(userInput: String): String? {
        val text = userInput.trim().lowercase()

        // 1. Identity & Persona
        if (text.contains("who are you") || text.contains("tumi ke") || text.contains("what are you") ||
            text.contains("about yourself") || text.contains("tell me about you") || text.contains("introduce yourself")
        ) {
            return "I am your offline personal AI Expense Assistant powered by Google Gemma! 🤖\n\n" +
                    "I run 100% on your device, meaning your financial transactions, balance, and income never leave your phone.\n\n" +
                    "Here is what I can do for you:\n" +
                    "• Record expenses: '50 tk bus vara', 'lunch 250 tk', 'ami 200 tk diye biryani kheyechi'\n" +
                    "• Record multiple items: '50 tk sosage, 320 tk murgi, 420 tk bazar'\n" +
                    "• Answer questions: 'total bus vara koto', 'amar balance koto', 'ajke koto khoroch'\n" +
                    "• Financial advice: Ask me about budgeting, saving tips, or the 50/30/20 rule!"
        }

        if (text.contains("who made you") || text.contains("who created you") || text.contains("ke baniyeche")) {
            return "I was built using Google's Gemma 3 on-device AI model and modern Android Jetpack Compose to bring private, intelligent offline expense management to Bangladesh! 🇧🇩"
        }

        // 2. Greetings & Status
        if (text.contains("how are you") || text.contains("kemon acho") || text.contains("ki khobor")) {
            return "I'm doing great, thank you! Ready to help you track your expenses and keep your budget in check. What would you like to record or calculate today? 😊"
        }

        if (text == "hi" || text == "hello" || text == "hey" || text == "salam" || text == "assalamu alaikum" ||
            text.startsWith("hi ") || text.startsWith("hello ") || text.startsWith("hey ")
        ) {
            return "Hello! 👋 I'm your offline Gemma Expense Assistant.\n" +
                    "You can talk to me naturally in English, Bangla, or Banglish.\n" +
                    "Try: '50 tk bus vara', 'total bus vara koto', or ask 'how to save money?'"
        }

        // 3. Financial Advice & Budgeting Tips
        if (text.contains("save money") || text.contains("taka bachabo") || text.contains("savings tip") ||
            text.contains("shonchoy") || text.contains("how to save") || text.contains("savings advice")
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

        if (text.contains("emergency fund") || text.contains("emergency taka")) {
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

