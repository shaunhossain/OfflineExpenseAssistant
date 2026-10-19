package com.example.expenseassistant

import com.example.expenseassistant.data.TransactionType
import com.example.expenseassistant.domain.Money
import com.example.expenseassistant.domain.TransactionValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyAndValidatorTest {

    @Test
    fun testMoneyMinorMath() {
        val m1 = Money.fromTaka(50) // 5000 minor
        val m2 = Money.fromTaka(100) // 10000 minor
        val sum = m1 + m2
        assertEquals(15000L, sum.amountMinor)
        assertEquals("৳150", sum.toFormattedString())

        val diff = m2 - m1
        assertEquals(5000L, diff.amountMinor)
        assertEquals("৳50", diff.toFormattedString())
    }

    @Test
    fun testTransactionValidatorSafetyBounds() {
        // Zero or negative
        val invalidZero = TransactionValidator.validate(0L, "Food", "Zero", "EXPENSE")
        assertFalse(invalidZero.isValid)

        // Exceeds 1 Crore boundary
        val invalidHuge = TransactionValidator.validate(20_000_000_00L, "Food", "Huge", "EXPENSE")
        assertFalse(invalidHuge.isValid)

        // Normal valid
        val valid = TransactionValidator.validate(5000L, "Transport", "bus vara", "EXPENSE")
        assertTrue(valid.isValid)
        assertEquals(TransactionType.EXPENSE, valid.type)
        assertEquals("Transport", valid.normalizedCategory)
    }

    @Test
    fun testCategoryMapping() {
        assertEquals("Food", TransactionValidator.mapToValidCategory(null, "cha ar nasta", TransactionType.EXPENSE))
        assertEquals("Transport", TransactionValidator.mapToValidCategory(null, "cng vara", TransactionType.EXPENSE))
        assertEquals("Bills", TransactionValidator.mapToValidCategory(null, "wifi bill", TransactionType.EXPENSE))
        assertEquals("Grocery", TransactionValidator.mapToValidCategory(null, "shobji bazar", TransactionType.EXPENSE))
        assertEquals("Salary", TransactionValidator.mapToValidCategory(null, "salary", TransactionType.INCOME))
    }
}
