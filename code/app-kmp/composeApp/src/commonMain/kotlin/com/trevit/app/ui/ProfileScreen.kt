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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
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
    // 모든 질문을 [다음] 버튼으로 통일 — 선택해도 자동으로 넘어가지 않는다.
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
                    ProfileQuestion.Purpose -> SingleChoiceCustom(
                        options = PURPOSES,
                        selected = state.purpose,
                        onSelectPreset = { state.purpose = it },
                        onCustomChange = { state.purpose = it.ifBlank { null } },
                        customPlaceholder = "원하는 여행 유형을 입력하세요",
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
                        },
                    )

                    ProfileQuestion.AgeGroup -> SingleChoice(
                        options = AGE_GROUPS,
                        selected = state.ageGroup,
                        wide = question.wide,
                        onSelect = { state.ageGroup = it },
                    )

                    ProfileQuestion.Mbti -> MbtiChoice(state)

                    ProfileQuestion.Food -> SingleChoiceCustom(
                        options = FOOD_PREFS,
                        selected = state.foodPreference,
                        onSelectPreset = { state.foodPreference = it },
                        onCustomChange = { state.foodPreference = it.ifBlank { null } },
                        customPlaceholder = "좋아하는 음식을 입력하세요",
                    )

                    ProfileQuestion.Places -> MultiChoiceCustom(
                        options = KEYWORD_OPTIONS,
                        selected = state.keywords,
                        onToggle = state::toggleKeyword,
                        customPlaceholder = "가고 싶은 곳을 입력하세요",
                    )

                    ProfileQuestion.Walking -> SingleChoice(
                        options = WALKING_OPTIONS,
                        selected = WALKING_OPTIONS[if (state.avoidWalking) 1 else 0]
                            .takeIf { state.walkingAnswered },
                        wide = question.wide,
                        onSelect = { option ->
                            state.avoidWalking = option == WALKING_OPTIONS[1]
                            state.walkingAnswered = true
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

    // 일정 제작에 꼭 필요한 질문 — 하나라도 선택해야 다음으로 넘어갈 수 있다
    val required = question == ProfileQuestion.Purpose ||
        question == ProfileQuestion.Gender ||
        question == ProfileQuestion.AgeGroup ||
        question == ProfileQuestion.Food ||
        question == ProfileQuestion.Walking
    val answered = when (question) {
        ProfileQuestion.Purpose -> state.purpose != null
        ProfileQuestion.Gender -> state.gender != null || state.genderNotSpecified
        ProfileQuestion.AgeGroup -> state.ageGroup != null
        ProfileQuestion.Food -> state.foodPreference != null
        ProfileQuestion.Walking -> state.walkingAnswered
        else -> true
    }
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
            // 모든 질문에 [다음] 버튼. 필수 질문은 선택 전엔 비활성.
            PrimaryCta(
                text = if (last) "플랜 만들기" else "다음",
                enabled = !required || answered,
                onClick = { state.nextQuestion() },
                modifier = Modifier.weight(1f),
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
            maxItemsInEachRow = 4,
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
        maxItemsInEachRow = 4,
    ) {
        options.forEach { option ->
            WebChip(option, option in selected, { onToggle(option) })
        }
    }
}

/**
 * 단일 선택 + "기타" 직접 입력. 프리셋을 고르면 기존처럼 자동으로 다음으로 넘어가고,
 * "기타"를 고르면 입력창이 열려 원하는 값을 직접 적은 뒤 [다음]으로 넘어간다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SingleChoiceCustom(
    options: List<String>,
    selected: String?,
    onSelectPreset: (String) -> Unit,
    onCustomChange: (String) -> Unit,
    customPlaceholder: String,
) {
    // 선택값이 프리셋에 없으면(=사용자 입력) 기타 모드로 본다
    var customMode by remember { mutableStateOf(selected != null && selected !in options) }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            maxItemsInEachRow = 4,
        ) {
            options.forEach { option ->
                WebChip(option, !customMode && selected == option, {
                    customMode = false
                    onSelectPreset(option)
                })
            }
            WebChip("기타", customMode, {
                customMode = true
                if (selected == null || selected in options) onCustomChange("")
            })
        }
        if (customMode) {
            Spacer(Modifier.height(12.dp))
            // 입력만 받고, 진행은 하단 공통 [다음] 버튼으로 통일
            CustomInputField(
                value = if (selected != null && selected !in options) selected else "",
                onValueChange = onCustomChange,
                placeholder = customPlaceholder,
                onSubmit = {},
            )
        }
    }
}

/**
 * 다중 선택 + "기타" 직접 입력. "기타"를 누르면 입력창이 열리고, 적은 값을 추가하면
 * 선택된 칩으로 나타난다(칩을 다시 누르면 제거). 프리셋과 섞어서 여러 개 고를 수 있다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MultiChoiceCustom(
    options: List<String>,
    selected: List<String>,
    onToggle: (String) -> Unit,
    customPlaceholder: String,
) {
    var addMode by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    val customSelected = selected.filter { it !in options }
    val addCustom = {
        val t = draft.trim()
        if (t.isNotEmpty() && t !in selected && t !in options) {
            onToggle(t)
            draft = ""
        }
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            maxItemsInEachRow = 4,
        ) {
            options.forEach { option ->
                WebChip(option, option in selected, { onToggle(option) })
            }
            // 사용자가 직접 추가한 항목 (누르면 제거)
            customSelected.forEach { item ->
                WebChip(item, true, { onToggle(item) })
            }
            WebChip("기타", addMode, { addMode = !addMode })
        }
        if (addMode) {
            Spacer(Modifier.height(12.dp))
            CustomInputField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = customPlaceholder,
                onSubmit = addCustom,
            )
            Spacer(Modifier.height(10.dp))
            PrimaryCta(
                text = "추가",
                enabled = draft.isNotBlank(),
                onClick = addCustom,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** "기타" 선택 시 나타나는 직접 입력창 (엔터/완료로 제출) */
@Composable
private fun CustomInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onSubmit: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .border(1.5.dp, webBorderStrong(), RoundedCornerShape(12.dp))
            .background(webFill(), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, fontSize = 14.5.sp, color = webTextDim())
        }
        BasicTextField(
            value = value,
            onValueChange = { if (it.length <= 40) onValueChange(it) },
            singleLine = true,
            textStyle = TextStyle(fontSize = 14.5.sp, color = webText()),
            cursorBrush = SolidColor(WebMint),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier = Modifier.fillMaxWidth(),
        )
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
