package com.trevit.app.voice

private fun browserSpeak(text: String): Unit = js(
    "{ if (window.speechSynthesis) { const u = new SpeechSynthesisUtterance(text); u.lang = 'ko-KR'; window.speechSynthesis.speak(u); } }"
)

private fun browserStop(): Unit = js("{ if (window.speechSynthesis) window.speechSynthesis.cancel(); }")

actual fun speak(text: String) = browserSpeak(text)

actual fun stopSpeaking() = browserStop()
