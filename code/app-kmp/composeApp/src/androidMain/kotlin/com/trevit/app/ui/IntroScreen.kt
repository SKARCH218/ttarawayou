package com.trevit.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trevit.app.AppState
import com.trevit.app.R
import com.trevit.app.Screen
import kotlinx.coroutines.delay

// 웹 `.intro-*` 애니메이션 타이밍 (style.css @keyframes introIn + setup.js)
private const val SYMBOL_FADE_MS = 700
private const val WORDMARK_DELAY_MS = 280L
private const val TAGLINE_DELAY_MS = 500L
private const val AUTO_ADVANCE_MS = 1600L

/**
 * 웹 `.intro` — 흰 바탕에 심볼·워드마크·태그라인이 차례로 떠오르고 1.6초 뒤 사라진다.
 * 웹과 마찬가지로 인트로가 걷히면 곧바로 여행 설정 화면이다. 탭하면 즉시 스킵.
 */
@Composable
fun IntroScreen(state: AppState) {
    var symbolVisible by remember { mutableStateOf(false) }
    var wordmarkVisible by remember { mutableStateOf(false) }
    var taglineVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        symbolVisible = true
        delay(WORDMARK_DELAY_MS)
        wordmarkVisible = true
        delay(TAGLINE_DELAY_MS - WORDMARK_DELAY_MS)
        taglineVisible = true
        // 인트로가 도는 동안 저장된 토큰이 아직 유효한지 확인해 둔다
        val loggedIn = state.auth.restoreSession()
        delay(AUTO_ADVANCE_MS - TAGLINE_DELAY_MS)
        if (state.screen is Screen.Intro) {
            state.screen = if (loggedIn) Screen.Setup else Screen.Login
        }
    }

    val symbolAlpha by animateFloatAsState(
        targetValue = if (symbolVisible) 1f else 0f,
        animationSpec = tween(SYMBOL_FADE_MS),
        label = "symbolAlpha",
    )
    val symbolScale by animateFloatAsState(
        targetValue = if (symbolVisible) 1f else 0.94f,
        animationSpec = tween(SYMBOL_FADE_MS),
        label = "symbolScale",
    )
    val wordmarkAlpha by animateFloatAsState(
        targetValue = if (wordmarkVisible) 1f else 0f,
        animationSpec = tween(600),
        label = "wordmarkAlpha",
    )
    val taglineAlpha by animateFloatAsState(
        targetValue = if (taglineVisible) 1f else 0f,
        animationSpec = tween(600),
        label = "taglineAlpha",
    )

    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .fillMaxSize()
            // 웹 `.intro { background: mono-000 }` — 인트로만 순백이다
            .background(if (isDarkIntro()) webSurface() else Color.White)
            .clickable(interactionSource = interaction, indication = null) {
                // 탭해서 건너뛰어도 로그인 여부는 그대로 따른다
                state.screen = if (state.auth.isLoggedIn) Screen.Setup else Screen.Login
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            // 웹 `.intro { gap: 18px }`
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // 웹 `.intro-symbol { width: 132px }` — 원본 비율 94:58
            Icon(
                painter = painterResource(R.drawable.ic_travit_symbol),
                contentDescription = "트레빗",
                tint = BrandMint,
                modifier = Modifier
                    .width(132.dp)
                    .height(81.dp)
                    .alpha(symbolAlpha)
                    .scale(symbolScale),
            )
            // 웹 `.intro-wordmark { width: 116px }` — 원본 비율 107:29
            Icon(
                painter = painterResource(R.drawable.ic_travit_wordmark),
                contentDescription = null,
                tint = BrandMint,
                modifier = Modifier
                    .width(116.dp)
                    .height(31.dp)
                    .alpha(wordmarkAlpha),
            )
            // 웹 `.intro-tagline { font-size: 13.5px; color: mono-500 }`
            Text(
                "AI가 여행 계획을 대신 세워드려요",
                fontSize = 13.5.sp,
                color = webTextFaint(),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(taglineAlpha),
            )
        }
    }
}

@Composable
private fun isDarkIntro(): Boolean = androidx.compose.foundation.isSystemInDarkTheme()
