package com.trevit.app

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.window.ComposeViewport
import com.trevit.app.resources.Res
import com.trevit.app.ui.TrevitApp
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * 웹(Compose Multiplatform/wasm) 진입점.
 * 안드로이드 앱과 완전히 같은 commonMain 화면(TrevitApp)을 브라우저 캔버스에 렌더링한다.
 * 설정 저장은 SharedPreferences 대신 localStorage를 쓴다.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class)
fun main() {
    // 안드로이드 기본값(10.0.2.2)은 에뮬레이터 전용.
    // 로컬 개발(run-app-web.sh, 웹은 3000·백엔드는 8080 별도 프로세스)만 :8080 을 붙인다.
    // 그 외(운영 배포 등)는 백엔드 jar 가 정적 파일까지 같이 서빙하므로 같은 origin 이 곧 API 서버다.
    // 예전엔 항상 :8080 을 붙였는데, 운영은 80 포트(URL에 포트 표시 없음)라 API 요청이
    // 존재하지 않는 8080으로 나가 응답 없이 조용히 멈췄다 — "이메일 인증 버튼이 멈춘다" 신고의 원인.
    val loc = window.location
    val webDefaultBaseUrl = if (loc.port == "3000") "http://${loc.hostname.ifBlank { "localhost" }}:8080" else loc.origin

    ComposeViewport(document.getElementById("trevitApp")!!) {
        // 웹(스키아)은 브라우저 시스템 폰트를 못 쓰므로 한글 폰트(Pretendard)를 폴백으로 등록.
        // 등록 전에 그리면 한글이 전부 □로 나오기 때문에 로드가 끝난 뒤에만 앱을 그린다.
        var fontsReady by remember { mutableStateOf(false) }
        val fontFamilyResolver = LocalFontFamilyResolver.current
        LaunchedEffect(Unit) {
            val pretendard = FontFamily(
                Font("PretendardRegular", Res.readBytes("font/pretendard_regular.otf"), FontWeight.Normal),
                Font("PretendardSemiBold", Res.readBytes("font/pretendard_semibold.otf"), FontWeight.SemiBold),
                Font("PretendardBold", Res.readBytes("font/pretendard_bold.otf"), FontWeight.Bold),
            )
            fontFamilyResolver.preload(pretendard)
            fontsReady = true
        }
        if (fontsReady) {
            val state = remember {
                AppState(
                    initialBaseUrl = localStorage.getItem("baseUrl") ?: webDefaultBaseUrl,
                    onBaseUrlSaved = { url -> localStorage.setItem("baseUrl", url) },
                    initialAuthToken = localStorage.getItem("authToken"),
                    onAuthTokenSaved = { token ->
                        if (token == null) localStorage.removeItem("authToken")
                        else localStorage.setItem("authToken", token)
                    },
                )
            }
            TrevitApp(state)
        }
    }
}
