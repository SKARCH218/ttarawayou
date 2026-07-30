package com.trevit.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trevit.app.AppState
import com.trevit.app.DURATIONS
import com.trevit.app.R
import com.trevit.app.REGIONS
import com.trevit.app.Screen
import com.trevit.app.won
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SetupScreen(state: AppState) {
    var regionQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("여행 설정") },
                navigationIcon = {
                    IconButton(onClick = { state.screen = Screen.Home }) {
                        Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = "뒤로")
                    }
                },
            )
        },
        bottomBar = {
            Button(
                // 목적을 포함한 취향 질문은 프로필 설문(한 질문씩)에서 물어본다
                onClick = { state.startProfile() },
                enabled = state.region != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
            ) {
                Text("다음", style = MaterialTheme.typography.titleMedium)
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionLabel("지역")
            OutlinedTextField(
                value = regionQuery,
                onValueChange = { regionQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("검색") },
                leadingIcon = {
                    Icon(
                        painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                },
                singleLine = true,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                REGIONS.filter { regionQuery.isBlank() || it.contains(regionQuery.trim()) }
                    .forEach { region ->
                        FilterChip(
                            selected = state.region == region,
                            onClick = { state.region = region },
                            label = { Text(region) },
                        )
                    }
            }

            SectionLabel("예산")
            Text(
                won(state.budget.toLong()),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Slider(
                value = state.budget,
                onValueChange = { state.budget = (it / 10_000f).roundToInt() * 10_000f },
                valueRange = 50_000f..1_000_000f,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("5만", style = MaterialTheme.typography.labelSmall)
                Text("100만", style = MaterialTheme.typography.labelSmall)
            }

            SectionLabel("기간")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                DURATIONS.forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = state.durationIndex == i,
                        onClick = { state.durationIndex = i },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = DURATIONS.size),
                    ) { Text(label) }
                }
            }

            SectionLabel("인원")
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedIconButton(
                    onClick = { if (state.people > 1) state.people-- },
                    enabled = state.people > 1,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_remove_minus),
                        contentDescription = "감소",
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    "${state.people}명",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                OutlinedIconButton(
                    onClick = { if (state.people < 4) state.people++ },
                    enabled = state.people < 4,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_add_plus),
                        contentDescription = "증가",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp),
    )
}
