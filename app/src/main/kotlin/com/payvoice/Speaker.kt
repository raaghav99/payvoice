package com.payvoice

import android.content.Context
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/** Holds a single TextToSpeech instance, initialised once. */
object Speaker {
    private var tts: TextToSpeech? = null
    private var ready = false
    private val pending = ArrayList<String>()
    private val counter = AtomicInteger()

    @Synchronized
    fun init(context: Context) {
        if (tts != null) return
        DebugLog.init(context)
        DebugLog.d("TTS init start")
        tts = TextToSpeech(context.applicationContext) { status ->
            synchronized(this) {
                DebugLog.d("TTS init callback status=$status (SUCCESS=${TextToSpeech.SUCCESS})")
                if (status == TextToSpeech.SUCCESS) {
                    val engine = tts
                    if (engine != null) {
                        val result = engine.setLanguage(Locale("en", "IN"))
                        DebugLog.d("TTS setLanguage(en-IN) result=$result engine=${engine.defaultEngine}")
                        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) = DebugLog.d("TTS onStart $utteranceId")
                            override fun onDone(utteranceId: String?) = DebugLog.d("TTS onDone $utteranceId")
                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) = DebugLog.d("TTS onError $utteranceId")
                            override fun onError(utteranceId: String?, errorCode: Int) =
                                DebugLog.d("TTS onError $utteranceId code=$errorCode")
                        })
                        ready = true
                        DebugLog.d("TTS ready, flushing ${pending.size} pending")
                        pending.forEach { say(engine, it) }
                        pending.clear()
                    }
                } else {
                    DebugLog.e("TTS init failed status=$status")
                }
            }
        }
    }

    private fun say(engine: TextToSpeech, text: String) {
        val id = "pv-${counter.incrementAndGet()}"
        val rc = engine.speak(text, TextToSpeech.QUEUE_ADD, null, id)
        DebugLog.d("TTS speak id=$id rc=$rc (SUCCESS=0) text=[$text]")
    }

    @Synchronized
    fun speak(context: Context, text: String) {
        init(context)
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        DebugLog.d(
            "AUDIO state music=${am.getStreamVolume(AudioManager.STREAM_MUSIC)}/" +
                "${am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)} " +
                "ringer=${am.ringerMode} mode=${am.mode} musicActive=${am.isMusicActive}"
        )
        val engine = tts
        if (ready && engine != null) {
            say(engine, text)
        } else {
            DebugLog.d("TTS not ready, queueing [$text]")
            pending.add(text)
        }
    }
}
