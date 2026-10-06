package com.trevit.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.trevit.app.AppState
import com.trevit.app.Screen
import com.trevit.app.i18n.LocalLanguage
import com.trevit.app.i18n.tr

@Composable
fun TrevitApp(state: AppState) {
    // 설정의 언어·테마를 앱 전체에 적용 (바꾸면 즉시 다시 그려진다)
    CompositionLocalProvider(LocalLanguage provides state.language) {
    TrevitTheme(state.themeMode) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            AnimatedContent(
                targetState = state.screen,
                transitionSpec = {
                    (slideInHorizontally { it / 4 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 4 } + fadeOut())
                },
                label = "screen",
            ) { screen ->
                when (screen) {
                    is Screen.Intro -> IntroScreen(state)
                    is Screen.Login -> LoginScreen(state)
                    is Screen.Signup -> SignupScreen(state)
                    is Screen.Setup -> SetupScreen(state)
                    is Screen.Profile -> ProfileScreen(state)
                    is Screen.Generating -> GeneratingScreen(state)
                    is Screen.Result -> ResultScreen(state)
                    is Screen.Journey -> JourneyScreen(state, screen.dayIndex)
                    is Screen.Settings -> SettingsScreen(state)
                }
            }

            state.errorMessage?.let { message ->
                AlertDialog(
                    onDismissRequest = { state.errorMessage = null },
                    title = { Text(tr("app.planFailed")) },
                    text = { Text(message) },
                    confirmButton = {
                        TextButton(onClick = {
                            state.errorMessage = null
                            state.screen = Screen.Generating
                        }) { Text(tr("common.retry")) }
                    },
                    dismissButton = {
                        TextButton(onClick = { state.loadDemoPlan() }) { Text(tr("app.demoPlan")) }
                    },
                )
            }
        }
    }
    }
}
