package com.trevit.app.voice

import com.trevit.app.i18n.AppLanguage
import com.trevit.app.i18n.translate

/** 음성 안내. 앞 문장이 끝나면 이어서 읽는다. [lang] 은 읽을 언어 */
expect fun speak(text: String, lang: AppLanguage)

/** 읽고 있거나 대기 중인 안내를 모두 멈춘다 */
expect fun stopSpeaking()

/** 거리를 소리 내어 읽기 좋은 문장으로 ("350미터", "1.2킬로미터") */
fun spokenDistance(meters: Double, lang: AppLanguage): String =
    if (meters >= 1000) {
        val tenths = (meters / 100).toInt()
        val km = if (tenths % 10 == 0) "${tenths / 10}" else "${tenths / 10}.${tenths % 10}"
        translate(lang, "voice.kilometers", km)
    } else {
        translate(lang, "voice.meters", ((meters / 10).toInt() * 10).coerceAtLeast(10))
    }
