package com.example.expenseassistant.domain

import com.example.expenseassistant.data.TransactionType

data class ValidationResult(
    val isValid: Boolean,
    val normalizedAmountMinor: Long,
    val normalizedCategory: String,
    val normalizedNote: String,
    val type: TransactionType,
    val needsReview: Boolean,
    val errorMessage: String? = null,
    val occurredAt: Long? = null
)

object TransactionValidator {

    val VALID_CATEGORIES = listOf(
        "Food",
        "Transport",
        "Grocery",
        "Bills",
        "Shopping",
        "Salary",
        "Health",
        "Entertainment",
        "Education",
        "Other"
    )

    private const val MAX_TRANSACTION_BDT = 10_000_000L // 1 Crore BDT upper boundary safety check

    fun validate(
        rawAmountMinor: Long?,
        rawCategory: String?,
        rawNote: String?,
        intentType: String?,
        occurredAt: Long? = null
    ): ValidationResult {
        if (rawAmountMinor == null || rawAmountMinor <= 0L) {
            return ValidationResult(
                isValid = false,
                normalizedAmountMinor = 0L,
                normalizedCategory = "Other",
                normalizedNote = rawNote ?: "",
                type = TransactionType.EXPENSE,
                needsReview = true,
                errorMessage = "Invalid or missing amount",
                occurredAt = occurredAt
            )
        }

        val taka = rawAmountMinor / 100L
        if (taka > MAX_TRANSACTION_BDT) {
            return ValidationResult(
                isValid = false,
                normalizedAmountMinor = rawAmountMinor,
                normalizedCategory = "Other",
                normalizedNote = rawNote ?: "",
                type = TransactionType.EXPENSE,
                needsReview = true,
                errorMessage = "Amount exceeds safety limit (৳$MAX_TRANSACTION_BDT)",
                occurredAt = occurredAt
            )
        }

        val text = "${rawCategory ?: ""} ${rawNote ?: ""}".lowercase()
        val isIncome = intentType?.uppercase()?.contains("INCOME") == true ||
            rawCategory?.equals("Salary", ignoreCase = true) == true ||
            text.contains("salary") || text.contains("salay") || text.contains("salari") ||
            text.contains("beton") || text.contains("veton") ||
            text.contains("income") || text.contains("peyechi") || text.contains("paisi") ||
            text.contains("paici") || text.contains("payci") ||
            text.contains("pelam") || text.contains("credit") || text.contains("bonus")

        val type = if (isIncome) {
            TransactionType.INCOME
        } else {
            TransactionType.EXPENSE
        }

        val normalizedCategory = mapToValidCategory(rawCategory, rawNote, type)
        val cleanNote = (rawNote ?: normalizedCategory).trim()
        val needsReview = cleanNote.isBlank() || cleanNote.length < 2

        return ValidationResult(
            isValid = true,
            normalizedAmountMinor = rawAmountMinor,
            normalizedCategory = normalizedCategory,
            normalizedNote = if (cleanNote.isNotBlank()) cleanNote else normalizedCategory,
            type = type,
            needsReview = needsReview,
            errorMessage = null,
            occurredAt = occurredAt
        )
    }

    fun mapToValidCategory(rawCategory: String?, note: String?, type: TransactionType): String {
        if (type == TransactionType.INCOME) {
            return "Salary"
        }

        val text = "${rawCategory ?: ""} ${note ?: ""}".lowercase()

        return when {
            text.contains("basha vara") || text.contains("bari vara") || text.contains("flat vara") ||
                    text.contains("বাসা ভাড়া") || text.contains("বাড়ি ভাড়া") ||
                    text.contains("house rent") || text.contains("room rent") -> "Bills"

            text.contains("bus") || text.contains("vara") || text.contains("bhara") || text.contains("bada") ||
                    text.contains("rickshaw") || text.contains("riksa") || text.contains("riksha") || text.contains("rikshaw") ||
                    text.contains("cng") || text.contains("uber") || text.contains("pathao") ||
                    text.contains("metro") || text.contains("train") || text.contains("transport") ||
                    text.contains("gari") || text.contains("auto") || text.contains("tempo") ||
                    text.contains("বাস") || text.contains("ভাড়া") || text.contains("রিকশা") ||
                    text.contains("গাড়ি") || text.contains("সিএনজি") -> "Transport"

            text.contains("cha") || text.contains("tea") || text.contains("coffee") ||
                    text.contains("lunch") || text.contains("dinner") || text.contains("breakfast") ||
                    text.contains("nasta") || text.contains("nashta") ||
                    text.contains("biryani") || text.contains("biriyani") || text.contains("biriani") ||
                    text.contains("burger") || text.contains("sosage") || text.contains("sausage") ||
                    text.contains("treat") || text.contains("khabar") ||
                    text.contains("misti") || text.contains("mishti") ||
                    text.contains("jhalmuri") || text.contains("singara") || text.contains("shingara") ||
                    text.contains("somosa") || text.contains("samucha") ||
                    text.contains("নাস্তা") || text.contains("চা") || text.contains("কফি") ||
                    text.contains("খাবার") || text.contains("লাঞ্চ") || text.contains("ডিনার") ||
                    text.contains("food") || text.contains("restaurant") -> "Food"

            text.contains("bazar") || text.contains("bajar") || text.contains("bazaar") ||
                    text.contains("grocery") || text.contains("groceries") ||
                    text.contains("chal") || text.contains("chaal") || text.contains("dal") || text.contains("daal") ||
                    text.contains("oil") || text.contains("tel") ||
                    text.contains("shobji") || text.contains("sobji") || text.contains("soji") ||
                    text.contains("mach") || text.contains("maach") ||
                    text.contains("murgi") || text.contains("chicken") ||
                    text.contains("mangsho") || text.contains("meat") || text.contains("gosht") || text.contains("gostho") ||
                    text.contains("dim") || text.contains("egg") ||
                    text.contains("dudh") || text.contains("dud") ||
                    text.contains("sawpno") || text.contains("swapno") || text.contains("shwapno") ||
                    text.contains("agora") || text.contains("meenabazar") || text.contains("unimart") ||
                    text.contains("chaldal") || text.contains("বাজার") || text.contains("সবজি") ||
                    text.contains("মাছ") || text.contains("মাংস") || text.contains("চাল") ||
                    text.contains("ডাল") || text.contains("তেল") || text.contains("ডিম") ||
                    text.contains("মুদি") -> "Grocery"

            text.contains("bill") || text.contains("current") || text.contains("electricity") ||
                    text.contains("gas") || text.contains("wifi") || text.contains("internet") ||
                    text.contains("recharge") || text.contains("reacharge") || text.contains("rechrg") ||
                    text.contains("rent") || text.contains("utility") ||
                    text.contains("বিল") || text.contains("বিদ্যুৎ") || text.contains("গ্যাস") ||
                    text.contains("পানি") || text.contains("রিচার্জ") -> "Bills"

            text.contains("cloth") || text.contains("shirt") || text.contains("pant") ||
                    text.contains("shopping") || text.contains("shoe") || text.contains("daraz") ||
                    text.contains("জামা") || text.contains("কাপড়") -> "Shopping"

            text.contains("medicine") || text.contains("medicin") || text.contains("medical") ||
                    text.contains("osudh") || text.contains("oshudh") || text.contains("oushodh") ||
                    text.contains("osud") || text.contains("osod") ||
                    text.contains("doctor") || text.contains("hospital") || text.contains("pharmacy") ||
                    text.contains("ঔষধ") || text.contains("ওষুধ") || text.contains("ডাক্তার") ||
                    text.contains("হাসপাতাল") ||
                    text.contains("health") -> "Health"

            text.contains("movie") || text.contains("cinema") || text.contains("game") -> "Entertainment"

            text.contains("tuition") || text.contains("school") || text.contains("college") ||
                    text.contains("book") || text.contains("boi") || text.contains("বই") -> "Education"

            else -> {
                VALID_CATEGORIES.firstOrNull { it.equals(rawCategory, ignoreCase = true) } ?: "Other"
            }
        }
    }
}
