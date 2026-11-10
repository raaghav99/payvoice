package com.payvoice

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class PayVoiceListenerService : NotificationListenerService() {

    private val duplicates = DuplicateFilter()

    override fun onCreate() {
        super.onCreate()
        DebugLog.init(applicationContext)
        DebugLog.d("SERVICE onCreate")
        Speaker.init(applicationContext)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        DebugLog.d("SERVICE onListenerConnected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        DebugLog.d("SERVICE onListenerDisconnected -> requestRebind")
        requestRebind(ComponentName(this, PayVoiceListenerService::class.java))
    }

    override fun onDestroy() {
        DebugLog.d("SERVICE onDestroy")
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) {
            DebugLog.d("POSTED sbn=null")
            return
        }
        val n = sbn.notification
        val extras = n?.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)
        val big = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)
        DebugLog.d(
            "POSTED pkg=${sbn.packageName} id=${sbn.id} flags=0x${n?.flags?.toString(16)} " +
                "channel=${n?.channelId} title=[$title] text=[$text] big=[$big]"
        )

        if (sbn.packageName == packageName) { DebugLog.d("SKIP own package"); return }
        if (n == null) { DebugLog.d("SKIP null notification"); return }
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) { DebugLog.d("SKIP group summary"); return }
        val allowed = AppFilter.isAllowed(applicationContext, sbn.packageName)
        if (!allowed) { DebugLog.d("SKIP app filter (enabled=${AppFilter.isEnabled(applicationContext)})"); return }
        if (extras == null) { DebugLog.d("SKIP null extras"); return }

        val combined = listOf(title, text, big).filterNotNull().joinToString(" | ")
        DebugLog.d("COMBINED [$combined]")

        val info = PaymentParser.parse(combined)
        if (info == null) { DebugLog.d("PARSE no payment found"); return }
        DebugLog.d("PARSE ok name=${info.name} amount=${info.amount}")

        if (duplicates.isDuplicate(info)) { DebugLog.d("SKIP duplicate"); return }

        val phrase = PaymentParser.speech(info)
        DebugLog.d("SPEAK request [$phrase]")
        Speaker.speak(applicationContext, phrase)
    }
}
