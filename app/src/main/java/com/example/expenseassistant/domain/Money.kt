package com.example.expenseassistant.domain

import java.text.NumberFormat
import java.util.Locale

/**
 * Representation of monetary value using minor units (Poisha for BDT).
 * 1 Taka = 100 Poisha.
 * Avoids any floating-point arithmetic errors in financial calculations.
 */
data class Money(
    val amountMinor: Long,
    val currency: String = "BDT"
) : Comparable<Money> {

    val majorAmount: Double
        get() = amountMinor / 100.0

    val taka: Long
        get() = amountMinor / 100

    val poisha: Long
        get() = amountMinor % 100

    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Cannot add different currencies: $currency vs ${other.currency}" }
        return Money(amountMinor + other.amountMinor, currency)
    }

    operator fun minus(other: Money): Money {
        require(currency == other.currency) { "Cannot subtract different currencies: $currency vs ${other.currency}" }
        return Money(amountMinor - other.amountMinor, currency)
    }

    operator fun times(factor: Int): Money = Money(amountMinor * factor, currency)

    override fun compareTo(other: Money): Int {
        require(currency == other.currency) { "Cannot compare different currencies: $currency vs ${other.currency}" }
        return amountMinor.compareTo(other.amountMinor)
    }

    /**
     * Formats the money into user-friendly currency string (e.g. "৳50", "৳120.50").
     */
    fun toFormattedString(): String {
        val symbol = if (currency == "BDT") "৳" else "$currency "
        return if (amountMinor % 100 == 0L) {
            val formattedTaka = NumberFormat.getNumberInstance(Locale.US).format(taka)
            "$symbol$formattedTaka"
        } else {
            val formatted = String.format(Locale.US, "%.2f", majorAmount)
            "$symbol$formatted"
        }
    }

    companion object {
        val ZERO = Money(0L, "BDT")

        fun fromTaka(taka: Long, currency: String = "BDT"): Money {
            return Money(taka * 100L, currency)
        }

        fun fromMajor(major: Double, currency: String = "BDT"): Money {
            return Money((major * 100.0 + 0.5).toLong(), currency)
        }
    }
}
