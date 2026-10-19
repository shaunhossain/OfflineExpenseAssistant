package com.example.expenseassistant.domain

import com.example.expenseassistant.data.TransactionType

data class ValidationResult(
    val isValid: Boolean,
    val normalizedAmountMinor: Long,
    val normalizedCategory: String,
    val normalizedNote: String,
    val type: TransactionType,
    val needsReview: Boolean,
    val errorMessage: String? = null
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
        intentType: String?
    ): ValidationResult {
        if (rawAmountMinor == null || rawAmountMinor <= 0L) {
            return ValidationResult(
                isValid = false,
                normalizedAmountMinor = 0L,
                normalizedCategory = "Other",
                normalizedNote = rawNote ?: "",
                type = TransactionType.EXPENSE,
                needsReview = true,
                errorMessage = "Invalid or missing amount"
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
                errorMessage = "Amount exceeds safety limit (৳$MAX_TRANSACTION_BDT)"
            )
        }

        val text = "${rawCategory ?: ""} ${rawNote ?: ""}".lowercase()
        val isIncome = intentType?.uppercase()?.contains("INCOME") == true ||
            rawCategory?.equals("Salary", ignoreCase = true) == true ||
            text.contains("salary") || text.contains("salay") || text.contains("beton") ||
            text.contains("income") || text.contains("peyechi") || text.contains("paisi") ||
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
            errorMessage = null
        )
    }

    fun mapToValidCategory(rawCategory: String?, note: String?, type: TransactionType): String {
        if (type == TransactionType.INCOME) {
            return "Salary"
        }

        val text = "${rawCategory ?: ""} ${note ?: ""}".lowercase()

        return when {
            text.contains("basha vara") || text.contains("bari vara") || text.contains("flat vara") ||
                    text.contains("house rent") || text.contains("room rent") -> "Bills"

            text.contains("bus") || text.contains("vara") || text.contains("rickshaw") ||
                    text.contains("cng") || text.contains("uber") || text.contains("pathao") ||
                    text.contains("metro") || text.contains("train") || text.contains("transport") -> "Transport"

            text.contains("cha") || text.contains("tea") || text.contains("coffee") ||
                    text.contains("lunch") || text.contains("dinner") || text.contains("breakfast") ||
                    text.contains("nasta") || text.contains("biryani") || text.contains("burger") ||
                    text.contains("sosage") || text.contains("sausage") || text.contains("treat") ||
                    text.contains("khabar") || text.contains("misti") || text.contains("mishti") ||
                    text.contains("jhalmuri") || text.contains("singara") || text.contains("somosa") ||
                    text.contains("food") || text.contains("restaurant") -> "Food"

            text.contains("bazar") || text.contains("bazaar") || text.contains("grocery") ||
                    text.contains("chal") || text.contains("dal") || text.contains("oil") ||
                    text.contains("tel") || text.contains("shobji") || text.contains("mach") ||
                    text.contains("murgi") || text.contains("chicken") || text.contains("mangsho") ||
                    text.contains("meat") || text.contains("dim") || text.contains("egg") -> "Grocery"

            text.contains("bill") || text.contains("current") || text.contains("electricity") ||
                    text.contains("gas") || text.contains("wifi") || text.contains("internet") ||
                    text.contains("recharge") || text.contains("rent") || text.contains("utility") -> "Bills"

            text.contains("cloth") || text.contains("shirt") || text.contains("pant") ||
                    text.contains("shopping") || text.contains("shoe") || text.contains("daraz") -> "Shopping"

            text.contains("medicine") || text.contains("osudh") || text.contains("doctor") ||
                    text.contains("hospital") || text.contains("pharmacy") || text.contains("health") -> "Health"

            text.contains("movie") || text.contains("cinema") || text.contains("game") -> "Entertainment"

            text.contains("tuition") || text.contains("school") || text.contains("college") ||
                    text.contains("book") || text.contains("boi") -> "Education"

            else -> {
                VALID_CATEGORIES.firstOrNull { it.equals(rawCategory, ignoreCase = true) } ?: "Other"
            }
        }
    }
}
