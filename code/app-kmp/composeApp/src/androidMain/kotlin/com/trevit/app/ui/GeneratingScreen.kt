package com.trevit.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trevit.app.AppState
import com.trevit.app.Screen
import kotlinx.coroutines.delay

/** 웹 `js/ask.js` 의 LOADING_MESSAGES — 2.6초마다 바뀐다 */
private val LOADING_MESSAGES = listOf(
    "예산을 배분하고 있어요",
    "취향에 맞는 장소를 고르는 중이에요",
    "예산을 알뜰하게 쓰는 조합을 찾고 있어요",
    "버스 노선과 도보 경로를 살피는 중이에요",
    "경로를 숨기는 중",
)

/**
 * 웹 `.loading-overlay` — 흰 바탕에 민트 스피너와 진행 문구만 둔다.
 * 화면을 벗어나면(취소) 이 코루틴과 HTTP 요청이 함께 취소된다.
 */
@Composable
fun GeneratingScreen(state: AppState) {
    LaunchedEffect(Unit) { state.generatePlan() }

    var messageIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2600)
            messageIndex = (messageIndex + 1) % LOADING_MESSAGES.size
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(webBg())
            .padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // 웹 `.spinner { 52px, border 4px, top-color primary-500 }`
        CircularProgressIndicator(
            color = WebMint,
            trackColor = webBorder(),
            strokeWidth = 4.dp,
            modifier = Modifier.size(52.dp),
        )
        // 웹 `.loading-overlay { gap: 20px }`
        Spacer(Modifier.height(20.dp))
        Text(
            "AI가 계획을 세우는 중",
            fontSize = 14.5.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.Bold,
            color = webTextLabel(),
            textAlign = TextAlign.Center,
        )
        Text(
            LOADING_MESSAGES[messageIndex],
            fontSize = 14.5.sp,
            lineHeight = 25.sp,
            color = webTextLabel(),
            textAlign = TextAlign.Center,
        )
        // 웹에는 없지만, 응답이 오래 걸릴 때 빠져나올 길은 남겨 둔다
        Spacer(Modifier.height(24.dp))
        val interaction = remember { MutableInteractionSource() }
        Text(
            "취소",
            modifier = Modifier
                .clickable(interactionSource = interaction, indication = null) {
                    state.screen = Screen.Profile
                }
                .padding(10.dp),
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = webTextDim(),
            textDecoration = TextDecoration.Underline,
        )
    }
}
