package com.payvoice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/** Holds a single TextToSpeech instance, initialised once. */
object Speaker {
    private const val TAG = "PayVoice"
    private var tts: TextToSpeech? = null
    private var ready = false
    private val pending = ArrayList<String>()

    @Synchronized
    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            synchronized(this) {
                if (status == TextToSpeech.SUCCESS) {
                    val engine = tts
                    if (engine != null) {
                        val result = engine.setLanguage(Locale("en", "IN"))
                        if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED
                        ) {
                            Log.w(TAG, "en-IN not available, using engine default")
                        }
                        ready = true
                        pending.forEach { engine.speak(it, TextToSpeech.QUEUE_ADD, null, null) }
                        pending.clear()
                    }
                } else {
                    Log.e(TAG, "TextToSpeech init failed: $status")
                }
            }
        }
    }

    @Synchronized
    fun speak(context: Context, text: String) {
        init(context)
        val engine = tts
        if (ready && engine != null) {
            engine.speak(text, TextToSpeech.QUEUE_ADD, null, null)
        } else {
            pending.add(text)
        }
    }
}
