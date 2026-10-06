package com.trevit.app.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/**
 * 앱 다국어 지원 — 설정에서 바로 바뀌는(재시작 없는) 가벼운 자체 번역 테이블.
 *
 * - 화면 문구는 `tr("키")` 로 쓴다. 번역 테이블은 키 → [한국어, 영어, 일본어, 중국어] 순서의 목록.
 * - 번역이 비어 있으면 한국어로, 키 자체가 없으면 키 문자열로 대체한다.
 * - `{0}`, `{1}` … 자리표시자는 인자로 치환된다. 예: tr("trip.remainStops", 3)
 * - 백엔드로 보내는 선택지 값(지역·여행 유형·음식 등)은 한국어 그대로 두고,
 *   화면 표시만 `optLabel(값)` 으로 번역한다 (키는 "opt.<값>").
 * - 서버가 보내는 한국어 오류 문구는 `serverMessage(문구)` 로 번역한다 (키는 "srv.<문구>").
 */
enum class AppLanguage(val code: String, val nativeName: String) {
    KO("ko", "한국어"),
    EN("en", "English"),
    JA("ja", "日本語"),
    ZH("zh", "中文"),
    ;

    companion object {
        /** 저장값/기기 언어 코드("en-US" 등)로 찾고, 모르면 한국어 */
        fun fromCode(code: String?): AppLanguage {
            val c = code?.lowercase()?.substringBefore('-')?.substringBefore('_') ?: return KO
            return entries.firstOrNull { it.code == c } ?: KO
        }
    }
}

/** 현재 화면 언어 — TrevitApp 이 AppState.language 로 제공한다 */
val LocalLanguage = compositionLocalOf { AppLanguage.KO }

/** 전체 번역 테이블 (화면별 파일을 합친다) */
private val TRANSLATIONS: Map<String, List<String>> by lazy {
    commonStrings + settingsStrings + authStrings + setupStrings + tripStrings
}

fun hasTranslation(key: String): Boolean = TRANSLATIONS.containsKey(key)

/** 비컴포저블 코드(상태 클래스 등)용 번역 */
fun translate(lang: AppLanguage, key: String, vararg args: Any?): String {
    val row = TRANSLATIONS[key]
    var text = row?.getOrNull(lang.ordinal)?.takeIf { it.isNotEmpty() }
        ?: row?.getOrNull(0)
        ?: key
    args.forEachIndexed { i, a -> text = text.replace("{$i}", a.toString()) }
    return text
}

/** 화면 문구 번역 */
@Composable
fun tr(key: String, vararg args: Any?): String = translate(LocalLanguage.current, key, *args)

/** 선택지 값(한국어, 서버로 보내는 값)의 표시용 이름. 번역이 없으면 값 그대로 */
@Composable
fun optLabel(value: String): String = optLabel(LocalLanguage.current, value)

fun optLabel(lang: AppLanguage, value: String): String =
    if (hasTranslation("opt.$value")) translate(lang, "opt.$value") else value

/** 토큰 금액 표시 — "300,000토큰" / "300,000 tokens" / … (화면에서는 won() 대신 이걸 쓴다) */
@Composable
fun tokens(value: Long): String = tr("unit.tokens", com.trevit.app.comma(value))

fun tokens(lang: AppLanguage, value: Long): String = translate(lang, "unit.tokens", com.trevit.app.comma(value))

/** 서버가 준 한국어 문구를 번역. 모르는 문구면 그대로 둔다 */
fun serverMessage(lang: AppLanguage, message: String?): String? {
    if (message == null) return null
    val key = "srv.${message.trim()}"
    return if (hasTranslation(key)) translate(lang, key) else message
}
