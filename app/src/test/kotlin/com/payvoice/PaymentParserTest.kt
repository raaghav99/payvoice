package com.payvoice

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentParserTest {

    private fun parse(s: String): PaymentInfo {
        val r = PaymentParser.parse(s)
        assertNotNull("expected a payment: $s", r)
        return r!!
    }

    @Test
    fun gpay() {
        val r = parse("Rahul paid you \u20B9250.00 | Google Pay")
        assertEquals("Rahul", r.name)
        assertEquals(0, BigDecimal("250").compareTo(r.amount))
        assertEquals("Rahul paid 250 rupees", PaymentParser.speech(r))
    }

    @Test
    fun phonePe() {
        val r = parse("Received \u20B91,500 from Priya Sharma | Payment received")
        assertEquals("Priya Sharma", r.name)
        assertEquals(0, BigDecimal("1500").compareTo(r.amount))
    }

    @Test
    fun paytm() {
        val r = parse("Paytm | Rs. 100 received from Amit Kumar in your Paytm account")
        assertEquals("Amit Kumar", r.name)
        assertEquals(0, BigDecimal("100").compareTo(r.amount))
    }

    @Test
    fun paytmSentYou() {
        val r = parse("Money Received | Neha sent you Rs 75.5")
        assertEquals("Neha", r.name)
        assertEquals("Neha paid 75 rupees 50 paise", PaymentParser.speech(r))
    }

    @Test
    fun bankSms() {
        val r = parse("INR 2,000.00 credited to A/c XX1234 on 10-11-26 by UPI/CR/123456789012/RAHUL KUMAR/HDFC. Avl bal Rs 9,000.00")
        assertEquals("RAHUL KUMAR", r.name)
        assertEquals(0, BigDecimal("2000").compareTo(r.amount))
    }

    @Test
    fun bankSmsFromName() {
        val r = parse("Rs.500.00 credited to your a/c XX1234 from Priya Sharma via UPI")
        assertEquals("Priya Sharma", r.name)
    }

    @Test
    fun noNameSpeaksAmountOnly() {
        val r = parse("Rs 300 deposited in your account")
        assertNull(r.name)
        assertEquals("300 rupees received", PaymentParser.speech(r))
    }

    @Test
    fun decimalsSpoken() {
        assertEquals("250 rupees 50 paise", PaymentParser.spokenAmount(BigDecimal("250.50")))
        assertEquals("250 rupees 5 paise", PaymentParser.spokenAmount(BigDecimal("250.05")))
        assertEquals("250 rupees", PaymentParser.spokenAmount(BigDecimal("250.00")))
    }

    @Test
    fun skipsDebitsAndRequests() {
        assertNull(PaymentParser.parse("Rs 500 debited from your account"))
        assertNull(PaymentParser.parse("You paid to Rahul Rs 500, received confirmation"))
        assertNull(PaymentParser.parse("Rahul requested Rs 100 from you"))
        assertNull(PaymentParser.parse("You sent to Rahul Rs 100"))
    }

    @Test
    fun skipsWithoutAmountOrKeyword() {
        assertNull(PaymentParser.parse("Rahul paid you"))
        assertNull(PaymentParser.parse("Your order of Rs 500 is on the way"))
        assertNull(PaymentParser.parse(""))
    }

    @Test
    fun amountFormats() {
        fun a(t: String) = PaymentParser.extractAmount(t)!!.toPlainString()
        assertEquals("250", a("\u20B9250"))
        assertEquals("250", a("\u20B9 250"))
        assertEquals("250", a("Rs250"))
        assertEquals("250", a("Rs.250"))
        assertEquals("1234567.5", a("rs. 12,34,567.5"))
        assertEquals("99.99", a("INR 99.99"))
        assertEquals("10", a("inr10"))
        assertNull(PaymentParser.extractAmount("no money here 250"))
    }

    @Test
    fun caseInsensitiveKeywords() {
        val r = parse("RAHUL PAID YOU RS 250")
        assertEquals("RAHUL", r.name)
    }

    @Test
    fun nameLimitedToFourWords() {
        val r = parse("Received Rs 10 from Aa Bb Cc Dd Ee")
        assertEquals("Aa Bb Cc Dd", r.name)
    }

    @Test
    fun nameExtractionOrderPrefersFrom() {
        val r = parse("Received Rs 10 from Amit. Neha paid you")
        assertEquals("Amit", r.name)
    }

    @Test
    fun namesRejectDigitsAndSymbols() {
        val r = parse("Rs 10 credited. Sender R4hul paid you")
        assertNull(r.name?.takeIf { it.any { c -> c.isDigit() } })
    }

    @Test
    fun speechFormats() {
        assertEquals("Rahul paid 250 rupees 50 paise",
            PaymentParser.speech(PaymentInfo("Rahul", BigDecimal("250.50"))))
        assertEquals("1500 rupees received",
            PaymentParser.speech(PaymentInfo(null, BigDecimal("1500"))))
    }

    @Test
    fun paytmRealNotification() {
        // Real Paytm layout: title + text + bigText joined by the listener service.
        val body = "Deposited in your HDFC Bank - 6500 on 10 November at 9:54 PM"
        val r = parse("Received ₹1 from Renu Chaturvedi | $body | $body")
        assertEquals("Renu Chaturvedi", r.name)
        assertEquals(0, BigDecimal("1").compareTo(r.amount))
        assertEquals("Renu Chaturvedi paid 1 rupees", PaymentParser.speech(r))
    }

    @Test
    fun paytmDepositedWithoutName() {
        val r = parse("Paytm | Received Rs 250 from Rahul Sharma via UPI")
        assertEquals("Rahul Sharma", r.name)
        assertEquals(0, BigDecimal("250").compareTo(r.amount))
    }
}
