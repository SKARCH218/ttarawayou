package com.trevit.app.voice

/** 한국어 음성 안내. 앞 문장이 끝나면 이어서 읽는다 */
expect fun speak(text: String)

/** 읽고 있거나 대기 중인 안내를 모두 멈춘다 */
expect fun stopSpeaking()

/** 거리를 소리 내어 읽기 좋은 문장으로 ("350미터", "1.2킬로미터") */
fun spokenDistance(meters: Double): String =
    if (meters >= 1000) {
        val tenths = (meters / 100).toInt()
        if (tenths % 10 == 0) "${tenths / 10}킬로미터" else "${tenths / 10}.${tenths % 10}킬로미터"
    } else {
        "${((meters / 10).toInt() * 10).coerceAtLeast(10)}미터"
    }
