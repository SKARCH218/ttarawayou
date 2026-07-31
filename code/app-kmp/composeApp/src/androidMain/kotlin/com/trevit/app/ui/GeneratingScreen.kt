package com.trevit.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trevit.app.AppState
import com.trevit.app.Screen

@Composable
fun GeneratingScreen(state: AppState) {
    // 화면을 벗어나면(취소) 이 코루틴과 HTTP 요청이 함께 취소된다
    LaunchedEffect(Unit) { state.generatePlan() }

    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // 스피너 뒤 은은한 민트 글로우 — 웹 로딩 오버레이의 민트 톤을 옮긴 것
        Box(contentAlignment = Alignment.Center) {
            MintGlow(size = 180.dp, alpha = 0.22f)
            CircularProgressIndicator(color = WebMint, strokeWidth = 4.dp)
        }
        Spacer(Modifier.height(24.dp))
        GradientTitle(
            "AI가 계획을 세우는 중",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "최대 2분 정도 걸릴 수 있어요",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = { state.screen = Screen.Profile }) { Text("취소") }
    }
}
