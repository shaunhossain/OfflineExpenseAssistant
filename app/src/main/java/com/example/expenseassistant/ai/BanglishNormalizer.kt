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

        // Normalize spoken number words (der, arai, sare sotero, barosho, etc.)
        text = normalizeSpokenNumbers(text)

        // Normalize common Banglish variations using word boundaries
        text = text.replace(Regex("\\b(?:khoros|khorosh|koroch|koros|khoroc|koroc)\\b", RegexOption.IGNORE_CASE), "khoroch")
        text = text.replace(Regex("\\b(?:khoroser|korocher|koroser|khorocer|korocer)\\b", RegexOption.IGNORE_CASE), "khorocher")
        text = text.replace(Regex("\\bbhara\\b", RegexOption.IGNORE_CASE), "vara")
        text = text.replace(Regex("\\bbajar\\b", RegexOption.IGNORE_CASE), "bazar")
        text = text.replace(Regex("\\b(?:paise|paichi|peyechi|peyesi|pelam|payci)\\b", RegexOption.IGNORE_CASE), "paisi")
        text = text.replace(Regex("\\b(?:salay|salari)\\b", RegexOption.IGNORE_CASE), "salary")
        text = text.replace(Regex("\\bveton\\b", RegexOption.IGNORE_CASE), "beton")
        text = text.replace(Regex("\\bhishab\\b", RegexOption.IGNORE_CASE), "hisab")

        return text.trim()
    }

    /**
     * Converts spoken Bengali/Banglish numerical expressions and traditional fractions
     * into standard ASCII numbers.
     * Handles:
     * - "der" (1.5), "der hajar" (1500), "dersho" (150)
     * - "arai" (2.5), "arai hajar" (2500), "araisho" (250)
     * - "sare <num>" (num + 0.5), e.g. "sare sotero" -> 17.5, "sare tin hajar" -> 3500, "sare barosho" -> 1250
     * - Hundred-words: "barosho" (1200), "choddosho" (1400), "ponerosho" (1500), "satsho" (700), etc.
     * - "sowa" (+0.25) and "poune" (-0.25)
     * - Native Bengali script forms: দেড়শ, আড়াইশ, বারোশ, সাড়ে সতেরো, etc.
     */
    fun normalizeSpokenNumbers(raw: String): String {
        var s = raw

        // 1. Bengali script hundreds and fractions
        val bnScriptHundreds = listOf(
            "দেড়শো" to "150", "দেড়শ" to "150",
            "আড়াইশো" to "250", "আড়াইশ" to "250",
            "একশো" to "100", "একশ" to "100",
            "দুইশো" to "200", "দুইশ" to "200",
            "তিনশো" to "300", "তিনশ" to "300",
            "চারশো" to "400", "চারশ" to "400",
            "পাঁচশো" to "500", "পাঁচশ" to "500",
            "ছয়শো" to "600", "ছয়শ" to "600",
            "সাতশো" to "700", "সাতশ" to "700",
            "আটশো" to "800", "আটশ" to "800",
            "নয়শো" to "900", "নয়শ" to "900",
            "এগারোশো" to "1100", "এগারোশ" to "1100",
            "বারোশো" to "1200", "বারোশ" to "1200",
            "তেরোশো" to "1300", "তেরোশ" to "1300",
            "চৌদ্দশো" to "1400", "চৌদ্দশ" to "1400",
            "পনেরোশো" to "1500", "পনেরোশ" to "1500",
            "ষোলশো" to "1600", "ষোলশ" to "1600",
            "সতেরোশো" to "1700", "সতেরোশ" to "1700",
            "আঠারোশো" to "1800", "আঠারোশ" to "1800",
            "উনিশশো" to "1900", "উনিশশ" to "1900",
            "বিশশো" to "2000", "বিশশ" to "2000"
        )
        s = s.replace(Regex("দেড়\\s*হাজার"), "1500")
        s = s.replace(Regex("আড়াই\\s*হাজার"), "2500")
        s = s.replace(Regex("দেড়\\s*লাখ"), "150000")
        s = s.replace(Regex("আড়াই\\s*লাখ"), "250000")
        s = s.replace(Regex("দেড়\\s*(?:টাকা|tk\\b)"), "1.5 tk")
        s = s.replace(Regex("আড়াই\\s*(?:টাকা|tk\\b)"), "2.5 tk")
        for ((bnWord, numVal) in bnScriptHundreds) {
            s = s.replace(bnWord, numVal)
        }

        // 2. Banglish Hundreds map
        val hundredsMap = listOf(
            "eksho" to 100, "ek sho" to 100,
            "dersho" to 150, "der sho" to 150, "derhsho" to 150,
            "duisho" to 200, "dushho" to 200, "duso" to 200, "dui sho" to 200,
            "araisho" to 250, "arai sho" to 250, "arhaisho" to 250,
            "tinsho" to 300, "tin sho" to 300,
            "charsho" to 400, "char sho" to 400,
            "pachsho" to 500, "pasho" to 500, "pach sho" to 500,
            "choysho" to 600, "choy sho" to 600,
            "satsho" to 700, "shatsho" to 700, "sat sho" to 700,
            "atsho" to 800, "aatsho" to 800, "aat sho" to 800, "at sho" to 800,
            "noysho" to 900, "noy sho" to 900,
            "egarosho" to 1100, "egaro sho" to 1100,
            "barosho" to 1200, "baro sho" to 1200,
            "terosho" to 1300, "tero sho" to 1300,
            "choddosho" to 1400, "chouddosho" to 1400, "choddo sho" to 1400,
            "ponerosho" to 1500, "ponero sho" to 1500,
            "sholosho" to 1600, "solosho" to 1600, "solo sho" to 1600,
            "soterosho" to 1700, "shoterosho" to 1700, "sotero sho" to 1700,
            "atharsho" to 1800, "atharosho" to 1800, "atharo sho" to 1800,
            "unishsho" to 1900, "unish sho" to 1900,
            "bishsho" to 2000, "bis sho" to 2000
        )

        val bengaliNumberWords = mapOf(
            "shunno" to 0, "ek" to 1, "dui" to 2, "tin" to 3, "char" to 4,
            "pach" to 5, "paach" to 5, "choy" to 6, "sat" to 7, "shat" to 7,
            "aat" to 8, "at" to 8, "noy" to 9, "dosh" to 10,
            "egaro" to 11, "baro" to 12, "tero" to 13, "choddo" to 14, "chouddo" to 14,
            "ponero" to 15, "sholo" to 16, "solo" to 16, "sotero" to 17, "shotero" to 17,
            "atharo" to 18, "athara" to 18, "unish" to 19, "bish" to 20, "bis" to 20,
            "kuri" to 20, "ekrish" to 21, "baish" to 22, "teish" to 23, "chobbish" to 24,
            "pocchish" to 25, "pochish" to 25, "chabbish" to 26, "sathash" to 27,
            "athash" to 28, "unirish" to 29, "trish" to 30, "tirish" to 30,
            "poytirish" to 35, "chollish" to 40, "ponchash" to 50
        )

        // 3. "sare" with hundred-words e.g. "sare barosho" -> 1250, "sare tinsho" -> 350
        val hundredWordsRegex = hundredsMap.map { it.first.replace(" ", "\\s+") }.joinToString("|")
        val sareHundredPattern = Pattern.compile("\\b(?:sare|share|sade)\\s+($hundredWordsRegex)\\b", Pattern.CASE_INSENSITIVE)
        val mSareH = sareHundredPattern.matcher(s)
        val sbSareH = StringBuffer()
        while (mSareH.find()) {
            val matchedWord = mSareH.group(1)?.lowercase()?.replace(Regex("\\s+"), " ") ?: ""
            val valH = hundredsMap.firstOrNull { it.first == matchedWord }?.second
            if (valH != null) {
                mSareH.appendReplacement(sbSareH, (valH + 50).toString())
            } else {
                mSareH.appendReplacement(sbSareH, mSareH.group(0) ?: "")
            }
        }
        mSareH.appendTail(sbSareH)
        s = sbSareH.toString()

        // 4. "sare <num> (hajar|hazar|k)" -> (num + 0.5) * 1000
        val sareHajarPattern = Pattern.compile("\\b(?:sare|share|sade)\\s+([a-zA-Z]+|\\d+)\\s*(?:hajar|hazar|k)\\b", Pattern.CASE_INSENSITIVE)
        val mSareHajar = sareHajarPattern.matcher(s)
        val sbSareHajar = StringBuffer()
        while (mSareHajar.find()) {
            val numStr = mSareHajar.group(1)?.lowercase() ?: ""
            val numVal = bengaliNumberWords[numStr]?.toDouble() ?: numStr.toDoubleOrNull()
            if (numVal != null) {
                val total = (numVal + 0.5) * 1000
                val formatted = if (total % 1.0 == 0.0) total.toLong().toString() else total.toString()
                mSareHajar.appendReplacement(sbSareHajar, formatted)
            } else {
                mSareHajar.appendReplacement(sbSareHajar, mSareHajar.group(0) ?: "")
            }
        }
        mSareHajar.appendTail(sbSareHajar)
        s = sbSareHajar.toString()

        // 5. "sare <num> sho" -> (num + 0.5) * 100
        val sareShoPattern = Pattern.compile("\\b(?:sare|share|sade)\\s+([a-zA-Z]+|\\d+)\\s*sho\\b", Pattern.CASE_INSENSITIVE)
        val mSareSho = sareShoPattern.matcher(s)
        val sbSareSho = StringBuffer()
        while (mSareSho.find()) {
            val numStr = mSareSho.group(1)?.lowercase() ?: ""
            val numVal = bengaliNumberWords[numStr]?.toDouble() ?: numStr.toDoubleOrNull()
            if (numVal != null) {
                val total = (numVal + 0.5) * 100
                val formatted = if (total % 1.0 == 0.0) total.toLong().toString() else total.toString()
                mSareSho.appendReplacement(sbSareSho, formatted)
            } else {
                mSareSho.appendReplacement(sbSareSho, mSareSho.group(0) ?: "")
            }
        }
        mSareSho.appendTail(sbSareSho)
        s = sbSareSho.toString()

        // 6. "sare <num>" e.g. "sare sotero" -> 17.5, "sare tin" -> 3.5
        val numWordsRegex = bengaliNumberWords.keys.joinToString("|")
        val sarePlainPattern = Pattern.compile("\\b(?:sare|share|sade)\\s+($numWordsRegex|\\d+)\\b", Pattern.CASE_INSENSITIVE)
        val mSarePlain = sarePlainPattern.matcher(s)
        val sbSarePlain = StringBuffer()
        while (mSarePlain.find()) {
            val numStr = mSarePlain.group(1)?.lowercase() ?: ""
            val numVal = bengaliNumberWords[numStr]?.toDouble() ?: numStr.toDoubleOrNull()
            if (numVal != null) {
                val total = numVal + 0.5
                val formatted = if (total % 1.0 == 0.0) total.toLong().toString() else total.toString()
                mSarePlain.appendReplacement(sbSarePlain, formatted)
            } else {
                mSarePlain.appendReplacement(sbSarePlain, mSarePlain.group(0) ?: "")
            }
        }
        mSarePlain.appendTail(sbSarePlain)
        s = sbSarePlain.toString()

        // 7. "der hajar" / "arai hajar" / "lakh"
        s = s.replace(Regex("\\b(?:der|derh|dedh)\\s*(?:hajar|hazar|k)\\b", RegexOption.IGNORE_CASE), "1500")
        s = s.replace(Regex("\\b(?:arai|arhai|adhai)\\s*(?:hajar|hazar|k)\\b", RegexOption.IGNORE_CASE), "2500")
        s = s.replace(Regex("\\b(?:der|derh|dedh)\\s*lakh\\b", RegexOption.IGNORE_CASE), "150000")
        s = s.replace(Regex("\\b(?:arai|arhai|adhai)\\s*lakh\\b", RegexOption.IGNORE_CASE), "250000")

        // 8. "der sho" / "arai sho"
        s = s.replace(Regex("\\b(?:der|derh|dedh)\\s*sho\\b", RegexOption.IGNORE_CASE), "150")
        s = s.replace(Regex("\\b(?:arai|arhai|adhai)\\s*sho\\b", RegexOption.IGNORE_CASE), "250")

        // 9. Replace standalone hundreds: "barosho", "dersho", "araisho", "satsho", etc.
        for ((w, v) in hundredsMap) {
            val p = Pattern.compile("\\b" + Pattern.quote(w) + "\\b", Pattern.CASE_INSENSITIVE)
            s = p.matcher(s).replaceAll(v.toString())
        }

        // 10. Standalone "der" / "arai" followed by currency (tk/taka)
        s = s.replace(Regex("\\b(?:der|derh|dedh)\\b(?=\\s*(?:tk|taka\\b))", RegexOption.IGNORE_CASE), "1.5")
        s = s.replace(Regex("\\b(?:arai|arhai|adhai)\\b(?=\\s*(?:tk|taka\\b))", RegexOption.IGNORE_CASE), "2.5")

        // 11. "sowa" (+0.25) and "poune" (-0.25)
        s = s.replace(Regex("\\b(?:sowa|showa)\\s+hajar\\b", RegexOption.IGNORE_CASE), "1250")
        s = s.replace(Regex("\\b(?:poune|pone|powne)\\s+hajar\\b", RegexOption.IGNORE_CASE), "750")
        val sowaPattern = Pattern.compile("\\b(?:sowa|showa)\\s+($numWordsRegex|\\d+)\\b", Pattern.CASE_INSENSITIVE)
        val mSowa = sowaPattern.matcher(s)
        val sbSowa = StringBuffer()
        while (mSowa.find()) {
            val numStr = mSowa.group(1)?.lowercase() ?: ""
            val numVal = bengaliNumberWords[numStr]?.toDouble() ?: numStr.toDoubleOrNull()
            if (numVal != null) {
                val total = numVal + 0.25
                val formatted = if (total % 1.0 == 0.0) total.toLong().toString() else total.toString()
                mSowa.appendReplacement(sbSowa, formatted)
            } else {
                mSowa.appendReplacement(sbSowa, mSowa.group(0) ?: "")
            }
        }
        mSowa.appendTail(sbSowa)
        s = sbSowa.toString()

        val pounePattern = Pattern.compile("\\b(?:poune|pone|powne)\\s+($numWordsRegex|\\d+)\\b", Pattern.CASE_INSENSITIVE)
        val mPoune = pounePattern.matcher(s)
        val sbPoune = StringBuffer()
        while (mPoune.find()) {
            val numStr = mPoune.group(1)?.lowercase() ?: ""
            val numVal = bengaliNumberWords[numStr]?.toDouble() ?: numStr.toDoubleOrNull()
            if (numVal != null) {
                val total = numVal - 0.25
                val formatted = if (total % 1.0 == 0.0) total.toLong().toString() else total.toString()
                mPoune.appendReplacement(sbPoune, formatted)
            } else {
                mPoune.appendReplacement(sbPoune, mPoune.group(0) ?: "")
            }
        }
        mPoune.appendTail(sbPoune)
        s = sbPoune.toString()

        return s
    }

    fun isDateWord(w: String): Boolean {
        val lower = w.lowercase().trim()
        val dateWords = setOf("tarikh", "tarik", "tarek", "tarike", "tarikhe", "date", "tarikher", "tarikhor", "tareker")
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
                t.contains("balace") ||
                t.contains("baki") ||
                t.contains("savings") ||
                (t.contains("koto") && (t.contains("ache") || t.contains("ase") || t.contains("ace") || t.contains("roilo")))
    }

    fun isHighestExpenseQuery(text: String): Boolean {
        val t = text.lowercase().trim()
        return t.contains("highest") ||
                t.contains("shobcheye beshi") ||
                t.contains("sobcheye beshi") ||
                t.contains("shobcheye boro") ||
                t.contains("sobcheye boro") ||
                t.contains("biggest expense") ||
                t.contains("max expense") ||
                t.contains("maximum khoroch") ||
                t.contains("maximum expense") ||
                t.contains("top expense") ||
                (t.contains("beshi taka") && (t.contains("kothay") || t.contains("khoroch") || t.contains("khoros") || t.contains("koroch") || t.contains("gese") || t.contains("gelo") || t.contains("hoise")))
    }

    fun parsePeriodSummary(text: String): String? {
        val t = text.lowercase().trim()
        val isToday = t.contains("today") || t.contains("ajke") || t.contains("ajker") || t.contains("ajk") || t.contains("aaj") ||
                t.contains("saradin") || t.contains("saradine") || t.contains("all day") ||
                ((t.startsWith("aj ") || t.startsWith("aaj ")) && (t.contains("khoroch") || t.contains("khoros") || t.contains("koroch")))
        val isYesterday = t.contains("yesterday") || t.contains("gotokal") || t.contains("gotokaler") || t.contains("gotokalke")

        val spendIndicators = t.contains("khoroch") || t.contains("khoros") || t.contains("koroch") || t.contains("expense") || t.contains("summary") || t.contains("hisab") || t.contains("hishab") || t.contains("gese") || t.contains("gelo") || t.contains("holo") || t.contains("hoise") || t.contains("data")

        if (isToday && spendIndicators) {
            return "today"
        }
        if (isYesterday && spendIndicators) {
            return "yesterday"
        }
        return null
    }

    fun cleanQueryKeyword(raw: String): String {
        var k = raw.trim().lowercase()
        k = k.removePrefix("total").removePrefix("shob").removePrefix("mot").removePrefix("moth").trim()
        k = k.removePrefix("amar ").removePrefix("amader ").trim()
        k = k.removeSuffix("koto").removeSuffix("koto taka").trim()

        val genericWords = setOf(
            "vara", "bhara", "bada", "khoroch", "khoros", "koroch", "cost", "bill", "expense", "spending",
            "taka", "tk", "gese", "gelo", "galo", "giyese", "geche", "gache", "giese", "gesilo",
            "hoise", "holo", "lagse", "lagce", "laglo", "lagsilo",
            "disi", "dilam", "korsi", "kinlam", "paisi", "pelam"
        )
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
        val stops = setOf("khoroch", "khoros", "koroch", "hisab", "hishab", "summary", "balance", "balace", "data", "expense", "taka", "tk", "all", "shob", "sob", "total", "report", "mash", "mashe", "ei mash", "ei mashe", "month", "this month")
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
            "^(.+?)(?:\\s+(?:e|te|er|ar|ye)|e|te|er|ar|ye)?(?:\\s+(?:mot|total))?\\s+koto(?:\\s*(?:tk|taka))?(?:\\s+(?:gese|gelo|galo|giyese|geche|gache|giese|lagse|lagce|laglo|lagsilo|holo|hoise|khoroch(?:\\s*(?:hoise|holo|hoyeche))?|khoros|koroch|cost))?$",
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
            "^(.+?)(?:\\s+(?:ar|er))?(?:\\s+(?:vara|bhara|bada|khoroch|khoros|koroch|cost|bill))\\s+(?:mot\\s+)?koto(?:\\s*(?:tk|taka))?(?:\\s+(?:gese|gelo|geche|gache|giese|lagse|lagce|laglo|lagsilo|holo|hoise))?$",
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
            "^(?:mot\\s+)?koto(?:\\s*(?:tk|taka))?\\s+(.+?)(?:\\s+(?:gese|gelo|galo|geche|gache|giese|hoise|holo|lagse|lagce|laglo|lagsilo))?$",
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
            "dilam", "disi", "dici", "diasilam", "disilam", "dise", "disen", "diyechi", "ditesi", "dese", "dite",
            "korsi", "korci", "korechi", "korlam", "korsilam", "korchilam", "kore", "kora",
            "kinlam", "kinci", "kinechi", "kinsi", "kinbo", "kina", "kinte",
            "kheyechi", "khelam", "khailam", "khaisi", "khawa",
            "peyechi", "peyesi", "pelam", "paisi", "paise", "paici", "payci",
            "laglo", "lagse", "lagce", "lagbe", "lagsilo",
            "gese", "gelo", "galo", "giyese", "gache", "giese", "gesilo", "holo", "hoise",
            "diye", "diya", "dia", "dya", "ashte", "ashar", "ashlam", "jete", "jawar", "jawyar", "gelam",
            "pothe", "shomoy", "hoye", "nilam", "nisi", "ferar",
            "theke", "thika", "theikha", "theikei",
            "jonno", "jnne", "jonne",
            "tarikh", "tarik", "tarek", "tarike", "tarikhe", "date",
            "spent", "paid", "bought", "had", "ate", "cost", "add", "record", "log", "please", "enter"
        )
        val words = rawNote.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val filtered = words.filter { it.lowercase() !in verbs }
        return if (filtered.isNotEmpty()) filtered.joinToString(" ") else rawNote.trim()
    }

    fun extractDateAndClean(rawText: String): ExtractedDate {
        var text = rawText
        var targetTime: Long? = null

        // 1. Explicit Day of Month: e.g. "4 tarikh", "4 tarikhe", "tarikh 4", "4 date", "15 tarikh", "4 tarik", "4 tarek"
        val dayPattern = Pattern.compile(
            "\\b(?:(?:tarikh|tarik|tarek|date)\\s*(\\d{1,2})|(\\d{1,2})\\s*(?:tarikh|tarik|tarek|tarike|tarikhe|tarikher|date|th\\s*e|th\\b|no\\b|number\\b))\\b",
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
                text = text.replace(Regex("\\b(?:gotokal|gotokalke|gotokaler|yesterday)\\b", RegexOption.IGNORE_CASE), " ").trim()
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
        val incomeKeywords = listOf("salary", "salay", "salari", "beton", "veton", "income", "bonus", "cashback", "remittance", "credited", "deposit")
        val receiveVerbs = listOf("paisi", "peyechi", "paise", "paichi", "pelam", "peyesi", "payci", "received", "got", "dhukse", "dhuklo", "ashlo", "aslo")

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
            "aj", "ajke", "ajk", "aaj", "kalke", "gotokal", "dupure", "shokale", "raate", "bikale", "shondhay",
            "tarikh", "tarik", "tarek", "tarike", "tarikhe", "date",
            "diye", "diya", "dia", "dite", "diasilam", "disilam", "dilam", "disi", "dici", "dise", "disen",
            "korsi", "korci", "korechi", "korlam", "korsilam", "korchilam", "kore", "kora",
            "kheyechi", "khelam", "khailam", "khaisi", "khawa",
            "kinlam", "kinci", "kinechi", "kinsi", "kinbo", "kina", "kinte",
            "laglo", "lagse", "lagce", "lagbe", "lagsilo",
            "gese", "gelo", "galo", "giyese", "gache", "giese", "gesilo", "holo", "hoise",
            "theke", "thika", "theikha", "theikei", "er", "ar", "ebong", "jonno", "jnne", "jonne",
            "ashte", "ashar", "ashlam", "jete", "jawar", "jawyar", "gelam", "jaite", "jaoar",
            "pothe", "shomoy", "hoye", "nilam", "nisi", "ferar",
            "expense", "khoroch", "khoros", "koroch", "tk", "taka", "takar", "tkr", "te", "e", "ye",
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
                textWithoutAmt.contains("salari") || textWithoutAmt.contains("beton") ||
                textWithoutAmt.contains("veton") || textWithoutAmt.contains("bonus") ||
                textWithoutAmt.contains("income") || textWithoutAmt.contains("cashback") ||
                textWithoutAmt.contains("paisi") || textWithoutAmt.contains("peyechi") ||
                textWithoutAmt.contains("pelam") || textWithoutAmt.contains("payci")

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
