package com.trevit.app.voice

import android.speech.tts.TextToSpeech
import com.trevit.app.map.AndroidAppContext
import java.util.Locale

/** 기기 내장 TTS 엔진. 첫 안내 때 초기화하고, 준비 전에 들어온 문장은 모아 뒀다가 읽는다 */
private object AndroidVoice {
    private var tts: TextToSpeech? = null
    private var ready = false
    private val pending = ArrayList<String>()

    fun speak(text: String) {
        if (ready) {
            say(text)
            return
        }
        pending.add(text)
        if (tts != null) return
        val ctx = AndroidAppContext.context ?: return
        tts = TextToSpeech(ctx) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                tts?.language = Locale.KOREAN
                pending.forEach(::say)
            }
            pending.clear()
        }
    }

    fun stop() {
        pending.clear()
        tts?.stop()
    }

    private fun say(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, text.hashCode().toString())
    }
}

actual fun speak(text: String) = AndroidVoice.speak(text)

actual fun stopSpeaking() = AndroidVoice.stop()
