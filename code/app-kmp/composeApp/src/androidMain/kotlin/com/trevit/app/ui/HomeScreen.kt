package com.trevit.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.trevit.app.AppState
import com.trevit.app.R
import com.trevit.app.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(state: AppState) {
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(painterResource(R.drawable.ic_settings), contentDescription = "설정")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // 웹 인트로처럼 로고 뒤에 은은한 민트 글로우를 깐다
            Box(contentAlignment = Alignment.Center) {
                MintGlow(size = 230.dp)
                Icon(
                    painter = painterResource(R.drawable.ic_travit_symbol),
                    contentDescription = null,
                    tint = BrandMint,
                    modifier = Modifier
                        .width(132.dp)
                        .height(82.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            GradientTitle(
                "트레빗",
                style = MaterialTheme.typography.displaySmall,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "AI가 여행 계획을 대신 세워드려요",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(40.dp))
            PrimaryCta(
                text = "여행 만들기",
                onClick = { state.screen = Screen.Setup },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showSettings) {
        var urlInput by remember { mutableStateOf(state.baseUrl) }
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("서버 주소") },
            text = {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Base URL") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    state.saveBaseUrl(urlInput)
                    showSettings = false
                }) { Text("저장") }
            },
            dismissButton = {
                TextButton(onClick = { showSettings = false }) { Text("취소") }
            },
        )
    }
}
