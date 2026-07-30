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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.trevit.app.AGE_GROUPS
import com.trevit.app.AppState
import com.trevit.app.FOOD_PREFS
import com.trevit.app.KEYWORD_OPTIONS
import com.trevit.app.R
import com.trevit.app.Screen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(state: AppState) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("프로필") },
                navigationIcon = {
                    IconButton(onClick = { state.screen = Screen.Setup }) {
                        Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = "뒤로")
                    }
                },
            )
        },
        bottomBar = {
            Button(
                onClick = { state.screen = Screen.Generating },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
            ) {
                Text("플랜 만들기", style = MaterialTheme.typography.titleMedium)
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
            SectionLabel("성별")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("남", "여", "선택 안 함").forEach { option ->
                    val value = option.takeIf { it != "선택 안 함" }
                    FilterChip(
                        selected = state.gender == value && (value != null || state.gender == null),
                        onClick = { state.gender = value },
                        label = { Text(option) },
                    )
                }
            }

            SectionLabel("연령대")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AGE_GROUPS.forEach { age ->
                    FilterChip(
                        selected = state.ageGroup == age,
                        onClick = { state.ageGroup = if (state.ageGroup == age) null else age },
                        label = { Text(age) },
                    )
                }
            }

            SectionLabel("MBTI")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MbtiAxis(
                    options = 'E' to 'I',
                    selected = state.mbtiEI,
                    onSelect = { state.mbtiEI = it },
                    modifier = Modifier.weight(1f),
                )
                MbtiAxis(
                    options = 'S' to 'N',
                    selected = state.mbtiSN,
                    onSelect = { state.mbtiSN = it },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MbtiAxis(
                    options = 'T' to 'F',
                    selected = state.mbtiTF,
                    onSelect = { state.mbtiTF = it },
                    modifier = Modifier.weight(1f),
                )
                MbtiAxis(
                    options = 'J' to 'P',
                    selected = state.mbtiJP,
                    onSelect = { state.mbtiJP = it },
                    modifier = Modifier.weight(1f),
                )
            }

            SectionLabel("음식")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FOOD_PREFS.forEach { food ->
                    FilterChip(
                        selected = state.foodPreference == food,
                        onClick = { state.foodPreference = food },
                        label = { Text(food) },
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("걷기 최소화", style = MaterialTheme.typography.titleSmall)
                Switch(
                    checked = state.avoidWalking,
                    onCheckedChange = { state.avoidWalking = it },
                )
            }

            SectionLabel("선호 키워드")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KEYWORD_OPTIONS.forEach { keyword ->
                    FilterChip(
                        selected = keyword in state.keywords,
                        onClick = { state.toggleKeyword(keyword) },
                        label = { Text(keyword) },
                    )
                }
            }

            SectionLabel("취향 메모")
            OutlinedTextField(
                value = state.preferenceNote,
                onValueChange = { state.preferenceNote = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("예: 매운 음식 좋아요, 조용한 카페 위주로") },
                minLines = 2,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MbtiAxis(
    options: Pair<Char, Char>,
    selected: Char?,
    onSelect: (Char?) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier) {
        listOf(options.first, options.second).forEachIndexed { i, c ->
            SegmentedButton(
                selected = selected == c,
                onClick = { onSelect(if (selected == c) null else c) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = 2),
            ) { Text(c.toString()) }
        }
    }
}
