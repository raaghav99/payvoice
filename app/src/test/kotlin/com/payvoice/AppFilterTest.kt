package com.payvoice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppFilterTest {
    @Test
    fun offAllowsEverything() {
        assertTrue(AppFilter.allows(false, "com.random.app"))
        assertTrue(AppFilter.allows(false, "com.phonepe.app"))
    }

    @Test
    fun onAllowsOnlyDefaultList() {
        assertTrue(AppFilter.allows(true, "com.google.android.apps.nbu.paisa.user"))
        assertTrue(AppFilter.allows(true, "com.phonepe.app"))
        assertTrue(AppFilter.allows(true, "net.one97.paytm"))
        assertTrue(AppFilter.allows(true, "in.org.npci.upiapp"))
        assertTrue(AppFilter.allows(true, "com.google.android.apps.messaging"))
        assertFalse(AppFilter.allows(true, "com.random.app"))
    }
}
