package com.payvoice

import java.math.BigDecimal

/** Result of parsing a payment notification. */
data class PaymentInfo(val name: String?, val amount: BigDecimal)

/** Pure Kotlin parsing logic. No Android dependencies. */
object PaymentParser {

    private val POSITIVE = Regex("received|credited|paid you|sent you|deposited", RegexOption.IGNORE_CASE)
    private val NEGATIVE = Regex("debited|sent to|paid to|requested", RegexOption.IGNORE_CASE)

    private val AMOUNT = Regex(
        "(?:\u20B9|\\brs\\.?|\\binr)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)",
        RegexOption.IGNORE_CASE
    )

    // A name word starts with an uppercase letter; 1 to 4 words, letters and spaces only.
    private const val NAME = "[A-Z][A-Za-z]*(?: +[A-Z][A-Za-z]*){0,3}"

    private val FROM = Regex("(?i:\\bfrom) +($NAME)")
    private val PAID_YOU = Regex("($NAME) +(?i:paid you)")
    private val SENT_YOU = Regex("($NAME) +(?i:sent you)")
    private val NAME_ONLY = Regex("^\\s*([A-Za-z]+(?: +[A-Za-z]+){0,3})")

    private val TRAILING_STOP = setOf(
        "via", "on", "in", "to", "at", "for", "by", "ref", "upi", "bank", "account",
        "using", "your", "has", "is", "and", "rs", "inr", "avl", "bal", "a", "with"
    )
    private val LEADING_STOP = setOf(
        "received", "money", "payment", "hello", "hi", "you", "upi", "credited", "deposited"
    )
    private val UPI_SKIP = setOf("cr", "dr", "p2a", "p2m", "p2p", "upi", "imps", "neft")

    /** Returns null when the text is not a payment-received notification. */
    fun parse(rawText: String): PaymentInfo? {
        val text = rawText.replace(Regex("\\s+"), " ").trim()
        if (text.isEmpty()) return null
        if (!POSITIVE.containsMatchIn(text)) return null
        if (NEGATIVE.containsMatchIn(text)) return null
        val amount = extractAmount(text) ?: return null
        return PaymentInfo(extractName(text), amount)
    }

    fun extractAmount(text: String): BigDecimal? {
        val m = AMOUNT.find(text) ?: return null
        return try {
            BigDecimal(m.groupValues[1].replace(",", ""))
        } catch (e: NumberFormatException) {
            null
        }
    }

    fun extractName(text: String): String? {
        FROM.find(text)?.let { clean(it.groupValues[1])?.let { n -> return n } }
        PAID_YOU.find(text)?.let { clean(it.groupValues[1])?.let { n -> return n } }
        SENT_YOU.find(text)?.let { clean(it.groupValues[1])?.let { n -> return n } }
        return fromUpiReference(text)
    }

    private fun fromUpiReference(text: String): String? {
        val idx = text.indexOf("UPI/", ignoreCase = true)
        if (idx < 0) return null
        val segments = text.substring(idx + 4).split("/").take(5)
        for (seg in segments) {
            val m = NAME_ONLY.find(seg) ?: continue
            val candidate = m.groupValues[1].trim()
            if (candidate.lowercase() in UPI_SKIP) continue
            clean(candidate)?.let { return it }
        }
        return null
    }

    /** Drops leading/trailing filler words; returns null when nothing is left. */
    private fun clean(candidate: String): String? {
        var words = candidate.trim().split(Regex(" +")).filter { it.isNotEmpty() }
        while (words.isNotEmpty() && words.first().lowercase() in LEADING_STOP) {
            words = words.drop(1)
        }
        val stopAt = words.indexOfFirst { it.lowercase() in TRAILING_STOP }
        if (stopAt >= 0) words = words.take(stopAt)
        words = words.take(4)
        return if (words.isEmpty()) null else words.joinToString(" ")
    }

    /** Spoken form of an amount, e.g. "250 rupees" or "250 rupees 50 paise". */
    fun spokenAmount(amount: BigDecimal): String {
        val scaled = amount.setScale(2, java.math.RoundingMode.HALF_UP)
        val rupees = scaled.toBigInteger()
        val paise = scaled.subtract(BigDecimal(rupees)).movePointRight(2).toInt()
        return if (paise == 0) "$rupees rupees" else "$rupees rupees $paise paise"
    }

    fun speech(info: PaymentInfo): String {
        val amt = spokenAmount(info.amount)
        return if (info.name != null) "${info.name} paid $amt" else "$amt received"
    }
}
