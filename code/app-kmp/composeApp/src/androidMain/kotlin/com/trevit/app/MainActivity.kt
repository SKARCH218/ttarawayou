package com.trevit.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.trevit.app.map.AndroidAppContext
import com.trevit.app.ui.TrevitApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 멀티플랫폼 위치 조회에 쓸 컨텍스트 등록 + 위치 권한 요청
        AndroidAppContext.context = applicationContext
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
        val initialBaseUrl = prefs.getString("baseUrl", AppState.DEFAULT_BASE_URL)
            ?: AppState.DEFAULT_BASE_URL
        val initialAuthToken = prefs.getString("authToken", null)
        setContent {
            val state = remember {
                AppState(
                    initialBaseUrl = initialBaseUrl,
                    onBaseUrlSaved = { url -> prefs.edit().putString("baseUrl", url).apply() },
                    initialAuthToken = initialAuthToken,
                    // 로그아웃하면 null이 와서 저장된 토큰을 지운다
                    onAuthTokenSaved = { token ->
                        val editor = prefs.edit()
                        if (token == null) editor.remove("authToken") else editor.putString("authToken", token)
                        editor.apply()
                    },
                )
            }
            TrevitApp(state)
        }
    }
}
