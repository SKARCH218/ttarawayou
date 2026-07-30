package com.trevit.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.trevit.app.ui.TrevitApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = getSharedPreferences("ttarawayu", Context.MODE_PRIVATE)
        val initialBaseUrl = prefs.getString("baseUrl", AppState.DEFAULT_BASE_URL)
            ?: AppState.DEFAULT_BASE_URL
        setContent {
            val state = remember {
                AppState(initialBaseUrl) { url ->
                    prefs.edit().putString("baseUrl", url).apply()
                }
            }
            TrevitApp(state)
        }
    }
}
