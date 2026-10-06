package com.trevit.app

/**
 * 앱 설정 저장소 (언어·테마 등). 플랫폼별로 구현을 넣어준다.
 * - Android: SharedPreferences (MainActivity)
 * - 웹: localStorage (wasmJsMain Main.kt)
 */
interface AppPrefs {
    fun get(key: String): String?
    fun put(key: String, value: String?)

    /** 저장하지 않는 기본 구현 (미리보기·테스트용) */
    object None : AppPrefs {
        override fun get(key: String): String? = null
        override fun put(key: String, value: String?) = Unit
    }
}
