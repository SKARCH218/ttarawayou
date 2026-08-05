package com.trevit.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.trevit.app.AGE_GROUPS
import com.trevit.app.AppState
import com.trevit.app.FOOD_PREFS
import com.trevit.app.GENDER_OPTIONS
import com.trevit.app.KEYWORD_OPTIONS
import com.trevit.app.PURPOSES
import com.trevit.app.ProfileQuestion
import com.trevit.app.WALKING_OPTIONS
import kotlinx.coroutines.delay

/** 웹 `setTimeout(next, 180)` — 고른 걸 눈으로 확인할 만큼의 여유 */
private const val AUTO_ADVANCE_DELAY_MS = 180L

/**
 * 취향 질문 — 웹 `ask.html` + `.ask-*` 규칙을 그대로 옮긴 화면.
 * 상단 8칸 진행 바 + "n / 8" → 가운데 이모지·질문·선택지 → 하단 [이전]·[다음] + 건너뛰기.
 */
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

    Column(
        Modifier
            .fillMaxSize()
            .background(webBg())
            .statusBarsPadding()
            .imePadding()
            // 웹 `.ask-screen { padding: 20px+safe 22px 28px }`
            .padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 28.dp),
    ) {
        SegmentedProgress(state.questionCount, state.questionIndex)
        Spacer(Modifier.height(8.dp))
        // 웹 `.ask-count` — 12px semibold mono-400, 오른쪽 정렬
        Text(
            "${state.questionIndex + 1} / ${state.questionCount}",
            modifier = Modifier.fillMaxWidth(),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = webTextDim(),
            textAlign = TextAlign.End,
        )

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
                .weight(1f)
                .fillMaxWidth(),
            label = "question",
        ) { index ->
            val question = ProfileQuestion.ordered[index]
            QuestionPage(question) {
                when (question) {
                    ProfileQuestion.Purpose -> SingleChoice(
                        options = PURPOSES,
                        selected = state.purpose,
                        wide = question.wide,
                        onSelect = { state.purpose = it; autoAdvance() },
                    )

                    ProfileQuestion.Gender -> SingleChoice(
                        options = GENDER_OPTIONS,
                        selected = when {
                            state.gender != null -> state.gender
                            state.genderNotSpecified -> "선택 안 함"
                            else -> null
                        },
                        wide = question.wide,
                        onSelect = { option ->
                            state.gender = option.takeIf { it != "선택 안 함" }
                            state.genderNotSpecified = option == "선택 안 함"
                            autoAdvance()
                        },
                    )

                    ProfileQuestion.AgeGroup -> SingleChoice(
                        options = AGE_GROUPS,
                        selected = state.ageGroup,
                        wide = question.wide,
                        onSelect = { state.ageGroup = it; autoAdvance() },
                    )

                    ProfileQuestion.Mbti -> MbtiChoice(state)

                    ProfileQuestion.Food -> SingleChoice(
                        options = FOOD_PREFS,
                        selected = state.foodPreference,
                        wide = question.wide,
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
                        wide = question.wide,
                        onSelect = { option ->
                            state.avoidWalking = option == WALKING_OPTIONS[1]
                            state.walkingAnswered = true
                            autoAdvance()
                        },
                    )

                    ProfileQuestion.Note -> NoteField(state)
                }
            }
        }

        QuestionNav(state)
    }
}

/**
 * 웹 `.ask-nav` + `.ask-skip`.
 * 단일 선택 질문은 고르는 순간 넘어가므로 웹처럼 [다음]·건너뛰기를 감추고 [이전]만 남긴다.
 */
@Composable
private fun QuestionNav(state: AppState) {
    val question = state.question
    val last = state.questionIndex == state.questionCount - 1

    Column(Modifier.navigationBarsPadding()) {
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            WebBackButton(
                text = if (state.questionIndex == 0) "설정" else "이전",
                onClick = { state.previousQuestion() },
            )
            if (!question.autoAdvance) {
                PrimaryCta(
                    text = if (last) "플랜 만들기" else "다음",
                    onClick = { state.nextQuestion() },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (!question.autoAdvance) {
            // 웹 `.ask-skip` — 밑줄 친 회색 글자, 가운데
            val interaction = remember { MutableInteractionSource() }
            Text(
                "건너뛰기",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = interaction, indication = null) {
                        when (question) {
                            ProfileQuestion.Places -> state.keywords.clear()
                            ProfileQuestion.Note -> state.preferenceNote = ""
                            else -> Unit
                        }
                        state.nextQuestion()
                    }
                    .padding(10.dp),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = webTextDim(),
                textDecoration = TextDecoration.Underline,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * 웹 `.ask-body` — 이모지를 화면 정가운데에 두고 질문·힌트·선택지를 그 아래에 쌓는다.
 * 선택지가 길거나 키보드가 올라오면 스크롤되지만 짧으면 세로 중앙 정렬을 유지한다.
 */
@Composable
private fun QuestionPage(
    question: ProfileQuestion,
    options: @Composable () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 웹 `.ask-emoji { font-size: 76px }` — 토스페이스는 이모지에만
            Text(
                question.emoji,
                fontSize = 76.sp,
                lineHeight = 76.sp,
                fontFamily = TossFaceFontFamily,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            // 웹 `.ask-question { 21px bold, line-height 1.45, tracking -0.02em }`
            Text(
                question.title,
                fontSize = 21.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.02f).em,
                color = webText(),
                textAlign = TextAlign.Center,
            )
            question.hint?.let {
                Spacer(Modifier.height(10.dp))
                // 웹 `.ask-hint { 13px, mono-500 }`
                Text(it, fontSize = 13.sp, color = webTextFaint(), textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(24.dp))
            options()
        }
    }
}

/** 웹 `.ask-options` — 알약 선택지를 가운데 정렬로 줄바꿈. [wide] 면 한 줄에 하나씩. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SingleChoice(
    options: List<String>,
    selected: String?,
    wide: Boolean,
    onSelect: (String) -> Unit,
) {
    if (wide) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            options.forEach { option ->
                WebChip(option, selected == option, { onSelect(option) }, Modifier.fillMaxWidth())
            }
        }
    } else {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            options.forEach { option ->
                WebChip(option, selected == option, { onSelect(option) })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MultiChoice(
    options: List<String>,
    selected: List<String>,
    onToggle: (String) -> Unit,
) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        options.forEach { option ->
            WebChip(option, option in selected, { onToggle(option) })
        }
    }
}

/** 웹 `.ask-mbti` — 축마다 한 줄, 한 줄에 두 칸 */
@Composable
private fun MbtiChoice(state: AppState) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        MbtiAxis('E' to 'I', state.mbtiEI) { state.mbtiEI = it }
        MbtiAxis('S' to 'N', state.mbtiSN) { state.mbtiSN = it }
        MbtiAxis('T' to 'F', state.mbtiTF) { state.mbtiTF = it }
        MbtiAxis('J' to 'P', state.mbtiJP) { state.mbtiJP = it }
    }
}

@Composable
private fun MbtiAxis(
    options: Pair<Char, Char>,
    selected: Char?,
    onSelect: (Char?) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(options.first, options.second).forEach { c ->
            WebChip(
                text = c.toString(),
                selected = selected == c,
                onClick = { onSelect(if (selected == c) null else c) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 웹 `.ask-note` — 패딩 13/14, radius 12, 1.5px mono-100, mono-050 배경, 14.5px */
@Composable
private fun NoteField(state: AppState) {
    Box(
        Modifier
            .fillMaxWidth()
            .border(1.5.dp, webBorderStrong(), RoundedCornerShape(12.dp))
            .background(webFill(), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        if (state.preferenceNote.isEmpty()) {
            Text(
                "예: 매운 음식 좋아요, 조용한 카페 위주로",
                fontSize = 14.5.sp,
                color = webTextDim(),
            )
        }
        BasicTextField(
            value = state.preferenceNote,
            onValueChange = { if (it.length <= 120) state.preferenceNote = it },
            singleLine = true,
            textStyle = TextStyle(fontSize = 14.5.sp, color = webText()),
            cursorBrush = SolidColor(WebMint),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
