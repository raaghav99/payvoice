package com.payvoice

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Diagnostic logger. Writes to logcat AND to files/debug.log inside the app,
 * because some OEM ROMs (Oplus/Realme) drop logcat lines over a per-process quota.
 * Read with: adb shell run-as com.payvoice cat files/debug.log
 */
object DebugLog {
    private const val TAG = "PayVoice"
    private const val MAX_BYTES = 512 * 1024L
    private var file: File? = null
    private val fmt = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)

    @Synchronized
    fun init(context: Context) {
        if (file == null) file = File(context.applicationContext.filesDir, "debug.log")
    }

    @Synchronized
    fun d(msg: String) {
        Log.d(TAG, msg)
        val f = file ?: return
        try {
            if (f.length() > MAX_BYTES) f.writeText("")
            f.appendText("${fmt.format(Date())} [pid=${android.os.Process.myPid()} " +
                "t=${Thread.currentThread().name}] $msg\n")
        } catch (e: Exception) {
            Log.w(TAG, "debug file write failed: $e")
        }
    }

    fun e(msg: String, t: Throwable? = null) = d("ERROR $msg ${t?.let { it.toString() } ?: ""}")
}
