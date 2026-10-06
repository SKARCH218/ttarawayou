package com.trevit.app.voice

import android.speech.tts.TextToSpeech
import com.trevit.app.i18n.AppLanguage
import com.trevit.app.map.AndroidAppContext
import java.util.Locale

/** 기기 내장 TTS 엔진. 첫 안내 때 초기화하고, 준비 전에 들어온 문장은 모아 뒀다가 읽는다 */
private object AndroidVoice {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var currentLang: AppLanguage? = null
    private val pending = ArrayList<Pair<String, AppLanguage>>()

    fun speak(text: String, lang: AppLanguage) {
        if (ready) {
            say(text, lang)
            return
        }
        pending.add(text to lang)
        if (tts != null) return
        val ctx = AndroidAppContext.context ?: return
        tts = TextToSpeech(ctx) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) pending.forEach { (t, l) -> say(t, l) }
            pending.clear()
        }
    }

    fun stop() {
        pending.clear()
        tts?.stop()
    }

    private fun say(text: String, lang: AppLanguage) {
        val engine = tts ?: return
        if (currentLang != lang) {
            engine.language = when (lang) {
                AppLanguage.KO -> Locale.KOREAN
                AppLanguage.EN -> Locale.US
                AppLanguage.JA -> Locale.JAPANESE
                AppLanguage.ZH -> Locale.SIMPLIFIED_CHINESE
            }
            currentLang = lang
        }
        engine.speak(text, TextToSpeech.QUEUE_ADD, null, text.hashCode().toString())
    }
}

actual fun speak(text: String, lang: AppLanguage) = AndroidVoice.speak(text, lang)

actual fun stopSpeaking() = AndroidVoice.stop()
