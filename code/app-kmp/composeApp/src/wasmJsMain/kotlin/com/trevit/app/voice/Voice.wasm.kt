package com.trevit.app.voice

import com.trevit.app.i18n.AppLanguage

private fun browserSpeak(text: String, lang: String): Unit = js(
    "{ if (window.speechSynthesis) { const u = new SpeechSynthesisUtterance(text); u.lang = lang; window.speechSynthesis.speak(u); } }"
)

private fun browserStop(): Unit = js("{ if (window.speechSynthesis) window.speechSynthesis.cancel(); }")

actual fun speak(text: String, lang: AppLanguage) = browserSpeak(
    text,
    when (lang) {
        AppLanguage.KO -> "ko-KR"
        AppLanguage.EN -> "en-US"
        AppLanguage.JA -> "ja-JP"
        AppLanguage.ZH -> "zh-CN"
    },
)

actual fun stopSpeaking() = browserStop()
