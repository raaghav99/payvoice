package com.payvoice

/** Suppresses repeats of the same name and amount within a time window. */
class DuplicateFilter(
    private val windowMs: Long = 5000L,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private var lastKey: String? = null
    private var lastTime = 0L

    @Synchronized
    fun isDuplicate(info: PaymentInfo): Boolean {
        val key = "${info.name ?: ""}|${info.amount.stripTrailingZeros().toPlainString()}"
        val now = clock()
        if (key == lastKey && now - lastTime < windowMs) return true
        lastKey = key
        lastTime = now
        return false
    }
}
