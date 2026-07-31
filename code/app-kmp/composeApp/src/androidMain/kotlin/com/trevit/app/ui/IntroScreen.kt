package com.trevit.app.ui

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trevit.app.AppState
import com.trevit.app.R
import com.trevit.app.Screen
import kotlinx.coroutines.delay

private const val SYMBOL_FADE_MS = 520
private const val WORDMARK_DELAY_MS = 260L
private const val TAGLINE_DELAY_MS = 560L
private const val AUTO_ADVANCE_MS = 1700L

/**
 * 브랜드 인트로. 심볼이 부드럽게 등장한 뒤 워드마크·태그라인이 따라 나오고,
 * 약 1.7초 후 홈으로 자동 전환된다. 화면을 탭하면 즉시 스킵.
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
        delay(AUTO_ADVANCE_MS - TAGLINE_DELAY_MS)
        if (state.screen is Screen.Intro) state.screen = Screen.Home
    }

    val symbolAlpha by animateFloatAsState(
        targetValue = if (symbolVisible) 1f else 0f,
        animationSpec = tween(SYMBOL_FADE_MS, easing = LinearOutSlowInEasing),
        label = "symbolAlpha",
    )
    val symbolScale by animateFloatAsState(
        targetValue = if (symbolVisible) 1f else 0.88f,
        animationSpec = tween(SYMBOL_FADE_MS, easing = LinearOutSlowInEasing),
        label = "symbolScale",
    )
    val wordmarkAlpha by animateFloatAsState(
        targetValue = if (wordmarkVisible) 1f else 0f,
        animationSpec = tween(420, easing = LinearOutSlowInEasing),
        label = "wordmarkAlpha",
    )
    val taglineAlpha by animateFloatAsState(
        targetValue = if (taglineVisible) 1f else 0f,
        animationSpec = tween(420, easing = LinearOutSlowInEasing),
        label = "taglineAlpha",
    )

    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                interactionSource = interaction,
                indication = null,
            ) { state.screen = Screen.Home },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // 웹 인트로의 `box-shadow: 0 0 40px rgba(0,195,137,.35)` — 심볼과 함께 페이드 인
            Box(contentAlignment = Alignment.Center) {
                MintGlow(
                    size = 260.dp,
                    alpha = 0.35f,
                    modifier = Modifier.alpha(symbolAlpha),
                )
                Icon(
                    painter = painterResource(R.drawable.ic_travit_symbol),
                    contentDescription = "트레빗",
                    tint = BrandMint,
                    modifier = Modifier
                        .width(148.dp)
                        .height(92.dp)
                        .alpha(symbolAlpha)
                        .scale(symbolScale),
                )
            }
            Spacer(Modifier.height(20.dp))
            Icon(
                painter = painterResource(R.drawable.ic_travit_wordmark),
                contentDescription = null,
                tint = BrandMint,
                modifier = Modifier
                    .width(150.dp)
                    .height(41.dp)
                    .alpha(wordmarkAlpha),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "AI가 여행 계획을 대신 세워드려요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(taglineAlpha),
            )
        }
    }
}
