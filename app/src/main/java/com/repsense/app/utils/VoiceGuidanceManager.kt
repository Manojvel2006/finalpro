package com.repsense.app.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceGuidanceManager private constructor() : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    var isEnabled: Boolean = true
    private var lastSpokenTime = 0L
    private val cooldownMs = 2500L // 2.5s cooldown between voice tips

    fun initialize(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            isInitialized = true
        }
    }

    fun speak(text: String, ignoreCooldown: Boolean = false) {
        if (!isEnabled || !isInitialized) return
        val now = System.currentTimeMillis()
        if (!ignoreCooldown && now - lastSpokenTime < cooldownMs) return

        lastSpokenTime = now
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "repsense_tts_${now}")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    companion object {
        val instance: VoiceGuidanceManager by lazy { VoiceGuidanceManager() }
    }
}
