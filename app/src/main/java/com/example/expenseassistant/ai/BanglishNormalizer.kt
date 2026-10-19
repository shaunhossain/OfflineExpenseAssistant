package com.example.expenseassistant.ai

import com.example.expenseassistant.domain.TransactionValidator
import com.example.expenseassistant.mcp.SingleItemConfirmation
import java.util.regex.Pattern

data class ExtractedDate(
    val cleanedText: String,
    val timestamp: Long?
)

data class QuickParseResult(
    val matched: Boolean,
    val intent: String,
    val amountMinor: Long? = null,
    val category: String? = null,
    val note: String? = null,
    val occurredAt: Long? = null,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val searchQuery: String? = null,
    val multipleItems: List<SingleItemConfirmation> = emptyList()
)

object BanglishNormalizer {

    fun normalize(input: String): String {
        var text = input.trim().lowercase()

        // Replace Bengali digits with ASCII digits
        val bengaliDigits = "০১২৩৪৫৬৭৮৯"
        bengaliDigits.forEachIndexed { index, char ->
            text = text.replace(char.toString(), index.toString())
        }

        // Normalize currency terms
        text = text.replace("টাকায়", "tk")
        text = text.replace("টাকার", "tk")
        text = text.replace("টাকা", "tk")
        text = text.replace("৳", "tk ")
        text = text.replace("takar", "tk")
        text = text.replace("taka", "tk")
        text = text.replace("bdt", "tk")

        // Normalize common Banglish variations
        text = text.replace("khoros", "khoroch")
        text = text.replace("khorosh", "khoroch")
        text = text.replace("bhara", "vara")
        text = text.replace("paise", "paisi")
        text = text.replace("paichi", "paisi")
        text = text.replace("peyechi", "paisi")
        text = text.replace("peyesi", "paisi")
        text = text.replace("pelam", "paisi")
        text = text.replace("salay", "salary")

        return text.trim()
    }

    fun isDateWord(w: String): Boolean {
        val lower = w.lowercase().trim()
        val dateWords = setOf("tarikh", "tarik", "tarike", "tarikhe", "date", "tarikher", "tarikhor")
        return dateWords.contains(lower)
    }

    fun parseSingleExpensePart(rawPart: String, defaultOccurredAt: Long? = null): SingleItemConfirmation? {
        val part = rawPart.trim().removePrefix("and").removePrefix("ar").removePrefix("ebong").trim()
        if (part.isBlank()) return null

        // Pattern A: "<amount> tk <note>" or "<amount> <note>"
        val amountFirstRegex = Pattern.compile("^(\\d+(?:\\.\\d+)?)\\s*(?:tk)?\\s+(.+)$", Pattern.CASE_INSENSITIVE)
        val m1 = amountFirstRegex.matcher(part)
        if (m1.find()) {
            val amount = m1.group(1)?.toDoubleOrNull() ?: return null
            var note = m1.group(2)?.trim() ?: ""
            note = cleanExpenseNote(note.removePrefix("tk").trim())
            if (note.isNotBlank() && !isDateWord(note) && amount > 0) {
                val category = TransactionValidator.mapToValidCategory(null, note, com.example.expenseassistant.data.TransactionType.EXPENSE)
                return SingleItemConfirmation(
                    amountMinor = (amount * 100).toLong(),
                    category = category,
                    note = note,
                    occurredAt = defaultOccurredAt
                )
            }
        }

        // Pattern B: "<note> <amount> tk" or "<note> <amount>"
        val amountLastRegex = Pattern.compile("^(.+?)\\s+(\\d+(?:\\.\\d+)?)\\s*(?:tk)?$", Pattern.CASE_INSENSITIVE)
        val m2 = amountLastRegex.matcher(part)
        if (m2.find()) {
            var note = m2.group(1)?.trim() ?: ""
            val amount = m2.group(2)?.toDoubleOrNull() ?: return null
            note = note.removePrefix("ajke").removePrefix("aj").trim()
            note = cleanExpenseNote(note)
            if (note.isNotBlank() && !isDateWord(note) && amount > 0) {
                val category = TransactionValidator.mapToValidCategory(null, note, com.example.expenseassistant.data.TransactionType.EXPENSE)
                return SingleItemConfirmation(
                    amountMinor = (amount * 100).toLong(),
                    category = category,
                    note = note,
                    occurredAt = defaultOccurredAt
                )
            }
        }

        // Pattern C: Natural Language clause fallback: e.g. "60 tk diye nasta korsi", "dupure 180 tk lunch"
        val natural = parseNaturalLanguageExpense(part, defaultOccurredAt)
        if (natural != null && natural.amountMinor != null && !natural.note.isNullOrBlank() && !isDateWord(natural.note)) {
            return SingleItemConfirmation(
                amountMinor = natural.amountMinor,
                category = natural.category ?: "Other",
                note = natural.note,
                occurredAt = defaultOccurredAt
            )
        }

        return null
    }

    fun parseMultipleExpenses(text: String, defaultOccurredAt: Long? = null): List<SingleItemConfirmation> {
        // Try splitting by comma, semicolon, newline, or conjunction words
        val parts = text.split(Regex("[,;\\n+]|\\s+(?:and|ar|ebong|plus|&)\\s+"))
        if (parts.size >= 2) {
            val parsedList = parts.mapNotNull { parseSingleExpensePart(it, defaultOccurredAt) }
            if (parsedList.size >= 2) {
                return parsedList
            }
        }

        // Fallback: match consecutive pattern without explicit commas: "50 tk sosage 320 tk murgi 420 tk bazar"
        val regex = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:tk)?\\s+([a-zA-Z\u0980-\u09FF]+(?:\\s+[a-zA-Z\u0980-\u09FF]+)*)")
        val matcher = regex.matcher(text)
        val foundItems = mutableListOf<SingleItemConfirmation>()
        while (matcher.find()) {
            val amt = matcher.group(1)?.toDoubleOrNull()
            val nt = matcher.group(2)?.trim()
            if (amt != null && amt > 0 && !nt.isNullOrBlank()) {
                val cleanNote = cleanExpenseNote(nt)
                if (cleanNote.isNotBlank() && !isDateWord(cleanNote)) {
                    val cat = TransactionValidator.mapToValidCategory(null, cleanNote, com.example.expenseassistant.data.TransactionType.EXPENSE)
                    foundItems.add(
                        SingleItemConfirmation(
                            amountMinor = (amt * 100).toLong(),
                            category = cat,
                            note = cleanNote,
                            occurredAt = defaultOccurredAt
                        )
                    )
                }
            }
        }
        if (foundItems.size >= 2) {
            return foundItems
        }

        return emptyList()
    }

    fun isGreetingOrHelp(text: String): Pair<Boolean, Boolean>? {
        val t = text.lowercase().trim()
        val isHelp = t == "help" || t.contains("ki korte paro") || t.contains("tumi ki korte paro") ||
                t.contains("how to use") || t.contains("commands") || t.contains("options") || t.contains("kivabe use")
        if (isHelp) return Pair(true, true)

        val greetings = setOf("hi", "hello", "hey", "salam", "assalamu alaikum", "kemon acho", "good morning", "good evening", "good afternoon", "halo", "ola")
        if (greetings.contains(t) || t.startsWith("hi ") || t.startsWith("hello ") || t.startsWith("hey ")) {
            return Pair(true, false)
        }
        return null
    }

    fun isMockDataRequest(text: String): Boolean {
        val t = text.lowercase().trim()
        return t.contains("mock") || t.contains("dummy data") || t.contains("seed data") ||
                (t.contains("push") && t.contains("transaction")) ||
                (t.contains("add") && t.contains("transaction") && (t.contains("1000") || t.contains("100")))
    }

    fun isBalanceQuery(text: String): Boolean {
        val t = text.lowercase().trim()
        return t.contains("balance") ||
                t.contains("baki") ||
                t.contains("savings") ||
                (t.contains("koto") && t.contains("ache"))
    }

    fun isHighestExpenseQuery(text: String): Boolean {
        val t = text.lowercase().trim()
        return t.contains("highest") ||
                t.contains("shobcheye beshi") ||
                t.contains("shobcheye boro") ||
                t.contains("biggest expense") ||
                t.contains("max expense") ||
                t.contains("maximum khoroch") ||
                t.contains("maximum expense") ||
                t.contains("top expense") ||
                (t.contains("beshi taka") && (t.contains("kothay") || t.contains("khoroch") || t.contains("gese") || t.contains("gelo") || t.contains("hoise")))
    }

    fun parsePeriodSummary(text: String): String? {
        val t = text.lowercase().trim()
        val isToday = t.contains("today") || t.contains("ajke") || t.contains("ajker") ||
                t.contains("saradin") || t.contains("saradine") || t.contains("all day") ||
                (t.startsWith("aj ") && t.contains("khoroch"))
        val isYesterday = t.contains("yesterday") || t.contains("gotokal") || t.contains("gotokaler")

        if (isToday && (t.contains("khoroch") || t.contains("expense") || t.contains("summary") || t.contains("hisab") || t.contains("gese") || t.contains("gelo") || t.contains("holo") || t.contains("data"))) {
            return "today"
        }
        if (isYesterday && (t.contains("khoroch") || t.contains("expense") || t.contains("summary") || t.contains("hisab") || t.contains("gese") || t.contains("gelo") || t.contains("holo") || t.contains("data"))) {
            return "yesterday"
        }
        return null
    }

    fun cleanQueryKeyword(raw: String): String {
        var k = raw.trim().lowercase()
        k = k.removePrefix("total").removePrefix("shob").removePrefix("mot").removePrefix("moth").trim()
        k = k.removePrefix("amar ").removePrefix("amader ").trim()
        k = k.removeSuffix("koto").removeSuffix("koto taka").trim()

        val genericWords = setOf("vara", "bhara", "khoroch", "cost", "bill", "expense", "spending", "taka", "tk", "gese", "gelo", "galo", "giyese", "geche", "hoise", "holo", "lagse", "laglo")
        val parts = k.split(Regex("\\s+")).filter { it.isNotBlank() }
        val nonGeneric = parts.filter { it !in genericWords }

        val base = if (nonGeneric.isNotEmpty()) nonGeneric.joinToString(" ") else k

        var trimmed = base.trim()
        if (trimmed.endsWith(" te") || trimmed.endsWith(" er") || trimmed.endsWith(" ar") || trimmed.endsWith(" e") || trimmed.endsWith(" ye")) {
            trimmed = trimmed.substring(0, trimmed.lastIndexOf(' ')).trim()
        }
        return if (trimmed.isNotBlank()) trimmed else raw.trim()
    }

    private fun isStopWord(word: String): Boolean {
        val w = word.lowercase().trim()
        val stops = setOf("khoroch", "hisab", "summary", "balance", "data", "expense", "taka", "tk", "all", "shob", "total", "report", "mash", "mashe", "ei mash", "ei mashe", "month", "this month")
        return stops.contains(w)
    }

    fun parseItemTotalQuery(text: String): String? {
        val clean = text.replace("?", "").trim()
        if (clean.isBlank()) return null
        if (Pattern.compile("\\d+\\s*(?:tk|taka)").matcher(clean).find()) return null

        // 1. English: "how much (did i spend )?(spent )?(on )?<item>"
        val pEn = Pattern.compile("^(?:how much(?: did i spend)?(?: spent)?(?: on)?|total spent on)\\s+(.+)$", Pattern.CASE_INSENSITIVE)
        val mEn = pEn.matcher(clean)
        if (mEn.find()) {
            val target = mEn.group(1)?.trim()
            if (!target.isNullOrBlank() && !isStopWord(target)) return cleanQueryKeyword(target)
        }

        // 2. "(total|mot) <item> [vara|khoroch|cost]? [koto|koto taka|koto tk]?"
        val pTotal = Pattern.compile("^(?:total|mot|moth)\\s+(.+?)(?:\\s+koto(?:\\s*(?:tk|taka))?)?$", Pattern.CASE_INSENSITIVE)
        val mTotal = pTotal.matcher(clean)
        if (mTotal.find()) {
            val raw = mTotal.group(1)?.trim()
            if (!raw.isNullOrBlank() && !isStopWord(raw)) {
                return cleanQueryKeyword(raw)
            }
        }

        // 3. "<item> [e/te/er/ar/ye] [mot]? koto [tk/taka] [gese/gelo/galo/giyese/geche/lagse/laglo/holo/hoise/khoroch/cost]?"
        val pInflected = Pattern.compile(
            "^(.+?)(?:\\s+(?:e|te|er|ar|ye)|e|te|er|ar|ye)?(?:\\s+(?:mot|total))?\\s+koto(?:\\s*(?:tk|taka))?(?:\\s+(?:gese|gelo|galo|giyese|geche|lagse|laglo|holo|hoise|khoroch(?:\\s*(?:hoise|holo|hoyeche))?|cost))?$",
            Pattern.CASE_INSENSITIVE
        )
        val mInf = pInflected.matcher(clean)
        if (mInf.find()) {
            val raw = mInf.group(1)?.trim()
            if (!raw.isNullOrBlank() && !isStopWord(raw)) {
                return cleanQueryKeyword(raw)
            }
        }

        // 4. "<item> [vara/khoroch/cost/bill] [mot]? koto [tk/taka]?"
        val pVara = Pattern.compile(
            "^(.+?)(?:\\s+(?:ar|er))?(?:\\s+(?:vara|khoroch|cost|bill))\\s+(?:mot\\s+)?koto(?:\\s*(?:tk|taka))?(?:\\s+(?:gese|gelo|geche|lagse|laglo|holo|hoise))?$",
            Pattern.CASE_INSENSITIVE
        )
        val mVara = pVara.matcher(clean)
        if (mVara.find()) {
            val raw = mVara.group(1)?.trim()
            if (!raw.isNullOrBlank() && !isStopWord(raw)) {
                return cleanQueryKeyword(raw)
            }
        }

        // 5. "[mot]? koto [tk/taka] <item> [vara/khoroch/e/te] [gese/gelo/geche/hoise/holo/lagse/laglo]?"
        val pKotoPrefix = Pattern.compile(
            "^(?:mot\\s+)?koto(?:\\s*(?:tk|taka))?\\s+(.+?)(?:\\s+(?:gese|gelo|galo|geche|hoise|holo|lagse|laglo))?$",
            Pattern.CASE_INSENSITIVE
        )
        val mKoto = pKotoPrefix.matcher(clean)
        if (mKoto.find()) {
            val raw = mKoto.group(1)?.trim()
            if (!raw.isNullOrBlank() && !isStopWord(raw)) {
                return cleanQueryKeyword(raw)
            }
        }

        return null
    }

    fun isGeneralSummaryQuery(text: String): Boolean {
        val t = text.lowercase().trim()
        val specificItems = listOf("bus", "vara", "bhara", "bazar", "cha", "tea", "coffee", "rickshaw", "food", "lunch", "dinner", "nasta", "medicine", "bill", "recharge", "wifi", "rent", "shopping", "cloth", "uber", "pathao")
        if (specificItems.any { t.contains(it) }) {
            return false
        }

        return t.contains("full month") || t.contains("this month") || t.contains("ei mashe") ||
                t.contains("month data") || t.contains("monthly data") || t.contains("monthly summary") ||
                t.contains("monthly expense") || t.contains("all data") || t.contains("shob data") ||
                t.contains("full data") || t.contains("all transaction") || t.contains("all transactions") ||
                t.contains("show data") || t.contains("data dekhao") || t.contains("all expense") ||
                t.contains("shob khoroch") || t.contains("total khoroch") || t.contains("shob hisab") ||
                t.contains("report") || t.contains("koto khoroch")
    }

    fun cleanConversationalPrefix(raw: String): String {
        var s = raw.trim()
        val prefixes = listOf(
            "can you please tell me ", "could you please tell me ",
            "can you tell me ", "could you tell me ", "please tell me ", "tell me ",
            "can you show me ", "could you show me ", "please show me ", "show me ",
            "i want to know ", "can you check ", "could you check ", "please check ", "check ",
            "bolo to ", "bolo ", "janano jabe ", "dekhao to ", "dekhao ",
            "ami jante chai ", "ektu bolo to ", "ektu bolben ", "ektu janan "
        )
        for (p in prefixes) {
            if (s.startsWith(p, ignoreCase = true)) {
                s = s.substring(p.length).trim()
                break
            }
        }
        return s
    }

    fun cleanExpenseNote(rawNote: String): String {
        val verbs = setOf(
            "dilam", "disi", "diasilam", "korsi", "korechi", "korlam", "kore", "kora",
            "kinlam", "kinechi", "kina", "kheyechi", "khelam", "khailam", "khaisi",
            "peyechi", "peyesi", "pelam", "paisi", "paise", "paichi",
            "laglo", "lagse", "gese", "gelo", "holo", "hoise", "galo", "giyese", "geche",
            "diye", "diya", "dite", "ashte", "ashar", "ashlam", "jete", "jawar", "jawyar", "gelam",
            "pothe", "shomoy", "hoye", "nilam", "nisi", "ferar",
            "tarikh", "tarik", "tarike", "tarikhe", "date",
            "spent", "paid", "bought", "had", "ate", "cost", "add", "record", "log", "please", "enter"
        )
        val words = rawNote.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val filtered = words.filter { it.lowercase() !in verbs }
        return if (filtered.isNotEmpty()) filtered.joinToString(" ") else rawNote.trim()
    }

    fun extractDateAndClean(rawText: String): ExtractedDate {
        var text = rawText
        var targetTime: Long? = null

        // 1. Explicit Day of Month: e.g. "4 tarikh", "4 tarikhe", "tarikh 4", "4 date", "15 tarikh", "4 tarik"
        val dayPattern = Pattern.compile(
            "\\b(?:(?:tarikh|tarik|date)\\s*(\\d{1,2})|(\\d{1,2})\\s*(?:tarikh|tarik|tarike|tarikhe|date|th\\s*e|th\\b))\\b",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = dayPattern.matcher(text)
        if (matcher.find()) {
            val dayStr = matcher.group(1) ?: matcher.group(2)
            val day = dayStr?.toIntOrNull()
            if (day != null && day in 1..31) {
                val cal = java.util.Calendar.getInstance()
                val currentDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
                cal.set(java.util.Calendar.DAY_OF_MONTH, day)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 12)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                // If day is greater than today's day of month, user likely meant last month
                if (day > currentDay) {
                    cal.add(java.util.Calendar.MONTH, -1)
                }
                targetTime = cal.timeInMillis
                text = text.removeRange(matcher.start(), matcher.end()).trim()
            }
        }

        // 2. Relative Days: "gotokal", "yesterday", "porshu", "ajke", "today"
        if (targetTime == null) {
            val lower = text.lowercase()
            if (lower.contains("gotokal") || lower.contains("yesterday")) {
                val cal = java.util.Calendar.getInstance()
                cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 12)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                targetTime = cal.timeInMillis
                text = text.replace(Regex("\\b(?:gotokal|gotokalke|yesterday)\\b", RegexOption.IGNORE_CASE), " ").trim()
            } else if (lower.contains("porshu") || lower.contains("gotoporshu")) {
                val cal = java.util.Calendar.getInstance()
                cal.add(java.util.Calendar.DAY_OF_YEAR, -2)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 12)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                targetTime = cal.timeInMillis
                text = text.replace(Regex("\\b(?:gotoporshu|porshu)\\b", RegexOption.IGNORE_CASE), " ").trim()
            }
        }

        return ExtractedDate(
            cleanedText = text.replace(Regex("\\s+"), " ").trim(),
            timestamp = targetTime
        )
    }

    fun parseIncomeTransaction(text: String, defaultOccurredAt: Long? = null): QuickParseResult? {
        val t = text.lowercase().trim()
        val incomeKeywords = listOf("salary", "salay", "beton", "income", "bonus", "cashback", "remittance", "credited", "deposit")
        val receiveVerbs = listOf("paisi", "peyechi", "paise", "paichi", "pelam", "received", "got", "dhukse", "dhuklo")

        val hasIncomeKeyword = incomeKeywords.any { t.contains(it) }
        val hasReceiveVerb = receiveVerbs.any { t.contains(it) }

        if (!hasIncomeKeyword && !hasReceiveVerb) return null

        // Ignore if this is an interrogative or check query
        if (t.contains("koto") || t.contains("how much") || t.contains("check") || t.contains("bolo")) return null

        // Extract amount
        val amtPattern = Pattern.compile("(?:(?:tk|taka)\\s*)?(\\d+(?:\\.\\d+)?)(?:\\s*(?:tk|taka))?", Pattern.CASE_INSENSITIVE)
        val matcher = amtPattern.matcher(t)
        var foundAmount: Double? = null
        var matchStart = -1
        var matchEnd = -1

        while (matcher.find()) {
            val amt = matcher.group(1)?.toDoubleOrNull()
            if (amt != null && amt > 0) {
                foundAmount = amt
                matchStart = matcher.start()
                matchEnd = matcher.end()
                break
            }
        }

        if (foundAmount == null || matchStart == -1) return null

        val beforeAmt = t.substring(0, matchStart)
        val afterAmt = t.substring(matchEnd)
        val textWithoutAmt = "$beforeAmt $afterAmt".trim()

        val stopWords = setOf(
            "ami", "amra", "amar", "tumi", "apni", "amader",
            "aj", "ajke", "kalke", "gotokal", "tarikh", "tarik", "tarike", "tarikhe", "date",
            "tk", "taka", "takar", "tkr", "theke", "er", "ar", "diye",
            "paisi", "peyechi", "paise", "paichi", "pelam", "received", "got", "dhukse", "dhuklo"
        )
        val words = textWithoutAmt.split(Regex("[^a-zA-Z\u0980-\u09FF]+")).filter { it.isNotBlank() }
        val filtered = words.filter { it !in stopWords }

        var note = if (filtered.isNotEmpty()) {
            filtered.joinToString(" ").replaceFirstChar { it.uppercase() }
        } else {
            "Salary"
        }
        if (note.equals("salay", ignoreCase = true)) note = "Salary"

        return QuickParseResult(
            matched = true,
            intent = "prepare_income",
            amountMinor = (foundAmount * 100).toLong(),
            category = "Salary",
            note = note,
            occurredAt = defaultOccurredAt
        )
    }

    fun parseNaturalLanguageExpense(text: String, defaultOccurredAt: Long? = null): QuickParseResult? {
        val clean = text.trim()
        if (clean.isBlank()) return null

        val amtPattern = Pattern.compile("(?:(?:tk|taka)\\s*)?(\\d+(?:\\.\\d+)?)(?:\\s*(?:tk|taka))?", Pattern.CASE_INSENSITIVE)
        val matcher = amtPattern.matcher(clean)
        var foundAmount: Double? = null
        var matchStart = -1
        var matchEnd = -1

        while (matcher.find()) {
            val amt = matcher.group(1)?.toDoubleOrNull()
            if (amt != null && amt > 0) {
                foundAmount = amt
                matchStart = matcher.start()
                matchEnd = matcher.end()
                break
            }
        }

        if (foundAmount == null || matchStart == -1) return null

        val beforeAmt = clean.substring(0, matchStart)
        val afterAmt = clean.substring(matchEnd)
        val textWithoutAmt = "$beforeAmt $afterAmt".lowercase()

        val stopWords = setOf(
            "ami", "amra", "amar", "tumi", "apni", "amader", "tar", "tader", "ke", "der",
            "aj", "ajke", "kalke", "gotokal", "dupure", "shokale", "raate", "bikale", "shondhay",
            "tarikh", "tarik", "tarike", "tarikhe", "date",
            "diye", "diasilam", "dilam", "disi", "korsi", "korechi", "korlam", "kore", "kora",
            "kheyechi", "khelam", "khailam", "khaisi", "kinlam", "kinechi", "kina",
            "laglo", "lagse", "gese", "gelo", "galo", "giyese", "geche", "holo", "hoise",
            "theke", "er", "ar", "ebong", "jonno", "diya", "dite",
            "ashte", "ashar", "ashlam", "jete", "jawar", "jawyar", "gelam", "jaite", "jaoar",
            "pothe", "shomoy", "hoye", "nilam", "nisi", "ferar",
            "expense", "khoroch", "tk", "taka", "takar", "tkr", "te", "e", "ye",
            "in", "at", "for", "on", "a", "an", "the", "i", "we", "my",
            "spent", "paid", "bought", "had", "ate", "cost",
            "can", "could", "you", "please", "add", "record", "log", "enter", "track", "save", "write"
        )

        val words = textWithoutAmt.split(Regex("[^a-zA-Z\u0980-\u09FF]+")).filter { it.isNotBlank() }
        val filteredWords = words.filter { it !in stopWords }

        var note = if (filteredWords.isNotEmpty()) {
            filteredWords.joinToString(" ")
        } else if (words.isNotEmpty()) {
            words.joinToString(" ")
        } else {
            "Expense"
        }

        val isIncome = textWithoutAmt.contains("salary") || textWithoutAmt.contains("salay") ||
                textWithoutAmt.contains("beton") || textWithoutAmt.contains("bonus") ||
                textWithoutAmt.contains("income") || textWithoutAmt.contains("cashback") ||
                textWithoutAmt.contains("paisi") || textWithoutAmt.contains("peyechi") ||
                textWithoutAmt.contains("pelam")

        val intent = if (isIncome) "prepare_income" else "prepare_expense"
        val category = if (isIncome) "Salary" else TransactionValidator.mapToValidCategory(null, note, com.example.expenseassistant.data.TransactionType.EXPENSE)
        if (isIncome && (note.equals("salay", ignoreCase = true) || note.isBlank())) {
            note = "Salary"
        }

        return QuickParseResult(
            matched = true,
            intent = intent,
            amountMinor = (foundAmount * 100).toLong(),
            category = category,
            note = note,
            occurredAt = defaultOccurredAt
        )
    }

    /**
     * High-speed deterministic regex parser for common Bangladeshi expense patterns and queries.
     * Yields instant response times for natural language questions, summaries, and expense sentences.
     */
    fun tryFastDeterministicParse(rawInput: String): QuickParseResult {
        val unwrap = cleanConversationalPrefix(rawInput)
        val text = normalize(unwrap)

        // 1. Check for greetings / help
        val greetingHelp = isGreetingOrHelp(text)
        if (greetingHelp != null) {
            val isHelp = greetingHelp.second
            return QuickParseResult(
                matched = true,
                intent = "get_help_or_greeting",
                searchQuery = if (isHelp) "help" else "greeting"
            )
        }

        // 1.5 Check for mock data seeding
        if (isMockDataRequest(text)) {
            return QuickParseResult(
                matched = true,
                intent = "seed_mock_transactions"
            )
        }

        // 2. Check for balance queries: "amar balance koto", "koto taka baki", etc.
        if (isBalanceQuery(text)) {
            return QuickParseResult(
                matched = true,
                intent = "get_financial_balance"
            )
        }

        // 3. Check for highest expense: "highest expense", "shobcheye beshi khoroch", etc.
        if (isHighestExpenseQuery(text)) {
            return QuickParseResult(
                matched = true,
                intent = "get_highest_expense"
            )
        }

        // 4. Check for period summary (today / yesterday)
        val period = parsePeriodSummary(text)
        if (period != null) {
            return QuickParseResult(
                matched = true,
                intent = "get_period_summary",
                searchQuery = period
            )
        }

        // 5. Check for general monthly summary intent
        if (isGeneralSummaryQuery(text)) {
            return QuickParseResult(
                matched = true,
                intent = "get_spending_summary"
            )
        }

        // 6. Check for item / category specific queries: e.g. "total bus vara koto", "bazar e koto khoroch hoise"
        val itemKeyword = parseItemTotalQuery(text)
        if (itemKeyword != null) {
            return QuickParseResult(
                matched = true,
                intent = "get_item_total",
                searchQuery = itemKeyword
            )
        }

        // 7. Check for reminder intent: "raat 9 tay reminder", "9 pm reminder dio", etc.
        val reminderRegex = Pattern.compile("(?:shokal|bikel|shondha|raat)?\\s*(\\d{1,2})(?:\\s*(?:tay|ta|am|pm|:(\\d{2})))?.*?reminder", Pattern.CASE_INSENSITIVE)
        val reminderMatcher = reminderRegex.matcher(text)
        if (reminderMatcher.find()) {
            var hour = reminderMatcher.group(1)?.toIntOrNull() ?: 21
            val minute = reminderMatcher.group(2)?.toIntOrNull() ?: 0

            if (text.contains("raat") || text.contains("shondha") || text.contains("pm")) {
                if (hour < 12) hour += 12
            } else if (text.contains("shokal") || text.contains("am")) {
                if (hour == 12) hour = 0
            }

            return QuickParseResult(
                matched = true,
                intent = "propose_reminder",
                reminderHour = hour,
                reminderMinute = minute,
                note = "Daily expense entry reminder"
            )
        }

        // Date and Day Extraction (e.g. "4 tarikh", "4 tarikhe", "15 tarikh", "gotokal", etc.)
        val dateInfo = extractDateAndClean(text)
        val textWithoutDate = dateInfo.cleanedText
        val occurredAt = dateInfo.timestamp

        // 8. Check for income: "salary paisi 30000 tk", "100 tk salay peyechi", "5000 tk bonus pelam", etc.
        val incomeParsed = parseIncomeTransaction(textWithoutDate, occurredAt)
        if (incomeParsed != null) {
            return incomeParsed
        }

        // 9. Check for MULTIPLE expenses: e.g. "50 tk sosage , 320 tk murgi , 420 tk bazar"
        val multipleItems = parseMultipleExpenses(textWithoutDate, occurredAt)
        if (multipleItems.size >= 2) {
            return QuickParseResult(
                matched = true,
                intent = "prepare_multiple_expenses",
                multipleItems = multipleItems
            )
        }

        // 10. Strict Pattern: "<amount> tk <note>" -> e.g. "50 tk bus vara", "100 tk cha ar bill"
        val amountFirstRegex = Pattern.compile("^(\\d+(?:\\.\\d+)?)\\s*(?:tk)?\\s+(.+)$")
        val amountFirstMatcher = amountFirstRegex.matcher(textWithoutDate)
        if (amountFirstMatcher.find()) {
            val amount = amountFirstMatcher.group(1)?.toDoubleOrNull()
            var note = amountFirstMatcher.group(2)?.trim() ?: ""
            note = cleanExpenseNote(note.removePrefix("tk").trim())
            if (amount != null && amount > 0 && note.isNotBlank() && !isDateWord(note)) {
                val category = TransactionValidator.mapToValidCategory(null, note, com.example.expenseassistant.data.TransactionType.EXPENSE)
                return QuickParseResult(
                    matched = true,
                    intent = "prepare_expense",
                    amountMinor = (amount * 100).toLong(),
                    category = category,
                    note = note,
                    occurredAt = occurredAt
                )
            }
        }

        // 11. Strict Pattern: "<note> <amount> tk" -> e.g. "ajke lunch 250 tk", "cha ar bill 100 tk", "rickshaw vara dilam 60 tk"
        val amountLastRegex = Pattern.compile("^(.+?)\\s+(\\d+(?:\\.\\d+)?)\\s*(?:tk)?$")
        val amountLastMatcher = amountLastRegex.matcher(textWithoutDate)
        if (amountLastMatcher.find()) {
            var note = amountLastMatcher.group(1)?.trim() ?: ""
            val amount = amountLastMatcher.group(2)?.toDoubleOrNull()
            note = note.removePrefix("ajke").removePrefix("aj").trim()
            note = cleanExpenseNote(note)
            if (amount != null && amount > 0 && note.isNotBlank() && !isDateWord(note)) {
                val category = TransactionValidator.mapToValidCategory(null, note, com.example.expenseassistant.data.TransactionType.EXPENSE)
                return QuickParseResult(
                    matched = true,
                    intent = "prepare_expense",
                    amountMinor = (amount * 100).toLong(),
                    category = category,
                    note = note,
                    occurredAt = occurredAt
                )
            }
        }

        // 12. Natural Language Sentences with filler words/verbs: e.g. "ami 200 tk diye lunch korsi", "rickshaw vara dilam 60 tk"
        val naturalExpense = parseNaturalLanguageExpense(textWithoutDate, occurredAt)
        if (naturalExpense != null) {
            return naturalExpense
        }

        return QuickParseResult(matched = false, intent = "")
    }
}
