package com.payvoice

import java.math.BigDecimal
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateFilterTest {
    private var now = 0L
    private val filter = DuplicateFilter(5000L) { now }
    private fun p(name: String?, amt: String) = PaymentInfo(name, BigDecimal(amt))

    @Test
    fun firstIsNotDuplicate() = assertFalse(filter.isDuplicate(p("Rahul", "250")))

    @Test
    fun sameWithinWindowIsDuplicate() {
        filter.isDuplicate(p("Rahul", "250"))
        now = 4999
        assertTrue(filter.isDuplicate(p("Rahul", "250.00")))
    }

    @Test
    fun sameAfterWindowIsAllowed() {
        filter.isDuplicate(p("Rahul", "250"))
        now = 5000
        assertFalse(filter.isDuplicate(p("Rahul", "250")))
    }

    @Test
    fun differentAmountOrNameIsAllowed() {
        filter.isDuplicate(p("Rahul", "250"))
        assertFalse(filter.isDuplicate(p("Rahul", "251")))
        assertFalse(filter.isDuplicate(p("Amit", "251")))
    }

    @Test
    fun noNameCase() {
        filter.isDuplicate(p(null, "100"))
        assertTrue(filter.isDuplicate(p(null, "100")))
    }
}
