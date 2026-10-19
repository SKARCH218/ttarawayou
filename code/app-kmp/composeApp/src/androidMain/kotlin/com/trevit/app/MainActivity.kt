package com.trevit.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.trevit.app.map.AndroidAppContext
import com.trevit.app.ui.ThemeMode
import com.trevit.app.ui.TrevitApp
import java.util.Locale

/** enableEdgeToEdge 기본값과 같은 내비게이션바 반투명 막 */
private val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 멀티플랫폼 위치 조회에 쓸 컨텍스트 등록 + 위치 권한 요청
        AndroidAppContext.context = applicationContext
        AndroidAppContext.requestLocationPermission = {
            runOnUiThread {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    1001,
                )
            }
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
                1001,
            )
        }


        val prefs = getSharedPreferences("ttarawayu", Context.MODE_PRIVATE)
        // 기본 서버: 에뮬레이터는 이 PC의 로컬 백엔드(10.0.2.2), 실제 휴대폰은 운영 서버.
        // (예전엔 항상 10.0.2.2 라서 휴대폰에 설치하면 서버에 붙지 못했다) 설정 → 개발자 옵션에서 바꿀 수 있다
        val defaultBaseUrl = if (isEmulator()) AppState.DEFAULT_BASE_URL else AppState.PRODUCTION_BASE_URL
        val initialBaseUrl = prefs.getString("baseUrl", defaultBaseUrl) ?: defaultBaseUrl
        val initialAuthToken = prefs.getString("authToken", null)
        setContent {
            val state = remember {
                AppState(
                    initialBaseUrl = initialBaseUrl,
                    defaultBaseUrl = defaultBaseUrl,
                    onBaseUrlSaved = { url -> prefs.edit().putString("baseUrl", url).apply() },
                    initialAuthToken = initialAuthToken,
                    // 로그아웃하면 null이 와서 저장된 토큰을 지운다
                    onAuthTokenSaved = { token ->
                        val editor = prefs.edit()
                        if (token == null) editor.remove("authToken") else editor.putString("authToken", token)
                        editor.apply()
                    },
                    // 언어·테마 등 설정 화면 값
                    prefs = object : AppPrefs {
                        override fun get(key: String): String? = prefs.getString(key, null)
                        override fun put(key: String, value: String?) {
                            val editor = prefs.edit()
                            if (value == null) editor.remove(key) else editor.putString(key, value)
                            editor.apply()
                        }
                    },
                    systemLanguageCode = Locale.getDefault().language,
                )
            }
            // 상태바·내비게이션바 아이콘 색을 앱 테마(설정의 라이트/다크)에 맞춘다.
            // 기본값은 기기 설정만 따라서, 앱을 라이트로 고정하면 흰 배경에 흰 아이콘이 됐다.
            val dark = when (state.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
                )
                onDispose {}
            }
            TrevitApp(state)
        }
    }
}

/** 안드로이드 에뮬레이터에서 도는지 (실제 휴대폰이면 false) */
private fun isEmulator(): Boolean =
    Build.FINGERPRINT.startsWith("generic") || Build.FINGERPRINT.contains("emulator") ||
        Build.MODEL.contains("sdk_gphone") || Build.MODEL.contains("Emulator") ||
        Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish") ||
        Build.PRODUCT.contains("sdk")
