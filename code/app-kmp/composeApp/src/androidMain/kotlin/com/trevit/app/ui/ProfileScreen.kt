package com.trevit.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trevit.app.AGE_GROUPS
import com.trevit.app.AppState
import com.trevit.app.FOOD_PREFS
import com.trevit.app.GENDER_OPTIONS
import com.trevit.app.KEYWORD_OPTIONS
import com.trevit.app.PURPOSES
import com.trevit.app.ProfileQuestion
import com.trevit.app.R
import com.trevit.app.Screen
import com.trevit.app.WALKING_OPTIONS
import kotlinx.coroutines.delay

/** 선택 후 다음 질문으로 넘어가기 전, 칩이 선택된 걸 눈으로 확인할 만큼의 여유 */
private const val AUTO_ADVANCE_DELAY_MS = 240L

/**
 * 프로필 설문. 한 화면에 질문 하나씩 보여주고 좌우 슬라이드로 전환한다.
 * 레이아웃: 상단 진행 표시 → 중앙 이모지 → 질문 → 선택지 → 하단 뒤로/다음.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(state: AppState) {
    // 선택 → 짧은 딜레이 → 자동 다음. 토큰을 증가시켜 매번 다시 트리거한다.
    var advanceToken by remember { mutableIntStateOf(0) }
    LaunchedEffect(advanceToken) {
        if (advanceToken == 0) return@LaunchedEffect
        delay(AUTO_ADVANCE_DELAY_MS)
        state.nextQuestion()
    }
    val autoAdvance = { advanceToken++ }

    Scaffold(
        // 키보드가 올라오면 화면 전체를 밀어 올린다(하단 버튼이 가려지지 않도록)
        modifier = Modifier.imePadding(),
        topBar = { QuestionProgressBar(state) },
        bottomBar = { QuestionNavBar(state) },
    ) { padding ->
        AnimatedContent(
            targetState = state.questionIndex,
            transitionSpec = {
                val forward = targetState > initialState
                val enterOffset: (Int) -> Int = { if (forward) it else -it }
                val exitOffset: (Int) -> Int = { if (forward) -it else it }
                (slideInHorizontally(tween(320), enterOffset) + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(320), exitOffset) + fadeOut(tween(220)))
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            label = "question",
        ) { index ->
            val question = ProfileQuestion.ordered[index]
            QuestionPage(question) {
                when (question) {
                    ProfileQuestion.Purpose -> SingleChoice(
                        options = PURPOSES,
                        selected = state.purpose,
                        onSelect = { state.purpose = it; autoAdvance() },
                    )

                    ProfileQuestion.Gender -> SingleChoice(
                        options = GENDER_OPTIONS,
                        selected = when {
                            state.gender != null -> state.gender
                            state.genderNotSpecified -> "선택 안 함"
                            else -> null
                        },
                        onSelect = { option ->
                            state.gender = option.takeIf { it != "선택 안 함" }
                            state.genderNotSpecified = option == "선택 안 함"
                            autoAdvance()
                        },
                    )

                    ProfileQuestion.AgeGroup -> SingleChoice(
                        options = AGE_GROUPS,
                        selected = state.ageGroup,
                        onSelect = { state.ageGroup = it; autoAdvance() },
                    )

                    ProfileQuestion.Mbti -> MbtiChoice(state)

                    ProfileQuestion.Food -> SingleChoice(
                        options = FOOD_PREFS,
                        selected = state.foodPreference,
                        onSelect = { state.foodPreference = it; autoAdvance() },
                    )

                    ProfileQuestion.Places -> MultiChoice(
                        options = KEYWORD_OPTIONS,
                        selected = state.keywords,
                        onToggle = state::toggleKeyword,
                    )

                    ProfileQuestion.Walking -> SingleChoice(
                        options = WALKING_OPTIONS,
                        selected = WALKING_OPTIONS[if (state.avoidWalking) 1 else 0]
                            .takeIf { state.walkingAnswered },
                        onSelect = { option ->
                            state.avoidWalking = option == WALKING_OPTIONS[1]
                            state.walkingAnswered = true
                            autoAdvance()
                        },
                    )

                    ProfileQuestion.Note -> OutlinedTextField(
                        value = state.preferenceNote,
                        onValueChange = { state.preferenceNote = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("예: 매운 음식 좋아요, 조용한 카페 위주로") },
                        minLines = 3,
                    )
                }
            }
        }
    }
}

/** 상단: 뒤로 화살표 + 진행 바 + "3 / 8" */
@Composable
private fun QuestionProgressBar(state: AppState) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 4.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { state.previousQuestion() }) {
            Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = "뒤로")
        }
        LinearProgressIndicator(
            progress = { (state.questionIndex + 1f) / state.questionCount },
            // 색을 명시하지 않으면 트랙이 Material 기본 보라로 떠서 브랜드와 충돌한다
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            drawStopIndicator = {},
            modifier = Modifier
                .weight(1f)
                .height(6.dp),
        )
        Spacer(Modifier.size(12.dp))
        Text(
            "${state.questionIndex + 1} / ${state.questionCount}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 하단: 뒤로 / (다중 선택·서술형이면) 다음 · 건너뛰기 */
@Composable
private fun QuestionNavBar(state: AppState) {
    val question = state.question
    Row(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(onClick = { state.previousQuestion() }) { Text("뒤로") }

        if (question.autoAdvance) {
            // 단일 선택 질문은 고르는 순간 넘어가므로 건너뛰기만 둔다
            TextButton(onClick = { state.nextQuestion() }) { Text("건너뛰기") }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 답을 지우고 넘어가는 명시적 건너뛰기 (MBTI는 화면 안에 "모름"이 있어 제외)
                if (question != ProfileQuestion.Mbti) {
                    TextButton(onClick = {
                        when (question) {
                            ProfileQuestion.Places -> state.keywords.clear()
                            ProfileQuestion.Note -> state.preferenceNote = ""
                            else -> Unit
                        }
                        state.nextQuestion()
                    }) { Text("건너뛰기") }
                    Spacer(Modifier.size(8.dp))
                }
                Button(
                    onClick = { state.nextQuestion() },
                    modifier = Modifier.height(48.dp),
                ) {
                    Text(
                        if (state.questionIndex == state.questionCount - 1) "플랜 만들기" else "다음",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}

/**
 * 질문 한 페이지의 공통 골격. 이모지를 화면 중앙에 크게 두고 그 아래에 질문·선택지를 쌓는다.
 * 키보드가 올라오거나 선택지가 많으면 스크롤로 흘러가되, 짧으면 세로 중앙 정렬을 유지한다.
 */
@Composable
private fun QuestionPage(
    question: ProfileQuestion,
    options: @Composable () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                question.emoji,
                fontSize = 80.sp,
                lineHeight = 104.sp,
                fontFamily = TossFaceFontFamily, // 이모지만 토스페이스로
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                question.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            options()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SingleChoice(
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(option, style = MaterialTheme.typography.bodyLarge) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MultiChoice(
    options: List<String>,
    selected: List<String>,
    onToggle: (String) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option in selected,
                    onClick = { onToggle(option) },
                    label = { Text(option, style = MaterialTheme.typography.bodyLarge) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "여러 개 고를 수 있어요",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** MBTI만 예외적으로 4줄(축별 토글) + "모름" */
@Composable
private fun MbtiChoice(state: AppState) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MbtiAxis('E' to 'I', state.mbtiEI) { state.mbtiEI = it }
        MbtiAxis('S' to 'N', state.mbtiSN) { state.mbtiSN = it }
        MbtiAxis('T' to 'F', state.mbtiTF) { state.mbtiTF = it }
        MbtiAxis('J' to 'P', state.mbtiJP) { state.mbtiJP = it }
        TextButton(onClick = {
            state.mbtiEI = null
            state.mbtiSN = null
            state.mbtiTF = null
            state.mbtiJP = null
            state.nextQuestion()
        }) { Text("모름") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MbtiAxis(
    options: Pair<Char, Char>,
    selected: Char?,
    onSelect: (Char?) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        listOf(options.first, options.second).forEachIndexed { i, c ->
            SegmentedButton(
                selected = selected == c,
                onClick = { onSelect(if (selected == c) null else c) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = 2),
            ) { Text(c.toString()) }
        }
    }
}
