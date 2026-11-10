package com.payvoice

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class PayVoiceListenerService : NotificationListenerService() {

    private val duplicates = DuplicateFilter()

    override fun onCreate() {
        super.onCreate()
        Speaker.init(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (sbn.packageName == packageName) return
        val n = sbn.notification ?: return
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (!AppFilter.isAllowed(applicationContext, sbn.packageName)) return

        val extras = n.extras ?: return
        val combined = listOf(
            extras.getCharSequence(Notification.EXTRA_TITLE),
            extras.getCharSequence(Notification.EXTRA_TEXT),
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
        ).filterNotNull().joinToString(" | ")

        val info = PaymentParser.parse(combined) ?: return
        if (duplicates.isDuplicate(info)) return

        Log.d(TAG, "Payment detected from ${sbn.packageName}")
        Speaker.speak(applicationContext, PaymentParser.speech(info))
    }

    companion object {
        private const val TAG = "PayVoice"
    }
}
