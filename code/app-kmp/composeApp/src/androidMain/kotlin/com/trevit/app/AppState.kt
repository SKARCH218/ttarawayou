package com.trevit.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.trevit.shared.PlanRepository
import com.trevit.shared.PlanRequest
import com.trevit.shared.PlanResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 화면 라우팅. 웹과 같은 순서 — 인트로 → 설정 → 질문 8개 → 플랜 → 여정.
 * (웹에 없는 별도 홈 화면은 두지 않는다. 인트로가 끝나면 바로 설정이다.)
 */
sealed interface Screen {
    data object Intro : Screen        // 브랜드 인트로(스플래시)
    data object Setup : Screen        // 여행 설정: 지역/예산/기간/인원
    data object Profile : Screen      // 취향 질문: 한 질문씩 넘기는 설문
    data object Generating : Screen   // 생성 중
    data object Result : Screen       // 결과 플랜
    data class Journey(val dayIndex: Int) : Screen
}

/**
 * 취향 질문 정의 (웹 `js/ask.js` 의 QUESTIONS 와 1:1). 화면에는 한 번에 하나씩만 보여준다.
 *
 * [autoAdvance] true면 값을 고르는 순간 다음 질문으로 넘어간다(단일 선택).
 * [wide] true면 웹 `.ask-opt.wide` 처럼 선택지를 한 줄에 하나씩 꽉 채운다.
 */
enum class ProfileQuestion(
    val emoji: String,
    val title: String,
    val autoAdvance: Boolean,
    val hint: String? = null,
    val wide: Boolean = false,
) {
    Purpose("🧭", "어떤 여행을 원하세요?", true),
    Gender("🙂", "성별을 알려주세요", true, "취향이 비슷한 여행자의 코스를 참고해요", wide = true),
    AgeGroup("🎂", "연령대는 어떻게 되세요?", true),
    Mbti("🧩", "MBTI를 알려주세요", false, "모르면 건너뛰어도 괜찮아요"),
    Food("🍚", "어떤 음식을 좋아하세요?", true),
    Places("🏞️", "어떤 곳에 가고 싶으세요?", false, "여러 개 고를 수 있어요"),
    Walking("🚶", "많이 걷는 건 괜찮으세요?", true, wide = true),
    Note("💬", "더 알려주실 취향이 있나요?", false, "AI가 장소를 고를 때 참고해요"),
    ;

    companion object {
        val ordered: List<ProfileQuestion> = entries.toList()
    }
}

val REGIONS = listOf(
    "서울", "인천", "강화", "수원", "가평", "양평", "파주", "포천", "용인",
    "남양주", "이천", "여주", "화성", "시흥", "과천", "광주", "김포", "안산",
)

val PURPOSES = listOf("휴양", "관광", "미식", "액티비티")
val AGE_GROUPS = listOf("10대", "20대", "30대", "40대", "50대+")
val FOOD_PREFS = listOf("한식", "양식", "일식", "중식", "상관없음")
val KEYWORD_OPTIONS = listOf("산", "바다", "공원", "강")
val GENDER_OPTIONS = listOf("남", "여", "선택 안 함")
val WALKING_OPTIONS = listOf("괜찮아요", "적게 걷고 싶어요")

/** 웹 `js/setup.js` 와 같은 예산 한계 */
const val MIN_BUDGET = 50_000L
const val BUDGET_STEP = 10_000L

/** 앱 전역 상태 홀더 (단일 Activity + Compose 상태 기반 내비게이션) */
class AppState(
    initialBaseUrl: String,
    private val onBaseUrlSaved: (String) -> Unit,
) {
    var baseUrl by mutableStateOf(initialBaseUrl)
        private set
    var screen by mutableStateOf<Screen>(Screen.Intro)
    var plan by mutableStateOf<PlanResponse?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
    var completedDays by mutableIntStateOf(0)

    // ---- 여행 설정 (웹 index.html + setup.js 와 같은 항목·한계) ----
    var region by mutableStateOf<String?>(null)
    var purpose by mutableStateOf<String?>(null)
    var budget by mutableStateOf(300_000L)
    var days by mutableIntStateOf(2)            // 1~3일
    var people by mutableIntStateOf(1)          // 1~4명

    /** 보유 토큰 (GET /api/wallet). 웹처럼 예산 슬라이더의 최대치가 된다. */
    var walletBalance by mutableStateOf<Long?>(null)
        private set

    /** 설정 화면 하단 `.error-box` 에 띄우는 문구 */
    var setupError by mutableStateOf<String?>(null)

    /** 웹 `state.maxBudget` — 보유 토큰을 1만 단위로 내림한 값 */
    val maxBudget: Long
        get() {
            val raw = maxOf(MIN_BUDGET, walletBalance ?: MIN_BUDGET)
            return MIN_BUDGET + ((raw - MIN_BUDGET) / BUDGET_STEP) * BUDGET_STEP
        }

    fun clampBudget(value: Long): Long = value.coerceIn(MIN_BUDGET, maxOf(MIN_BUDGET, maxBudget))

    /** 보유 토큰 조회 → 예산을 한계 안으로 되돌린다 (웹 loadWallet) */
    suspend fun loadWallet() {
        try {
            walletBalance = withContext(Dispatchers.IO) { repository.fetchWallet(baseUrl) }
            setupError = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            walletBalance = 0
            setupError = "서버에 연결할 수 없어요 — 잠시 후 다시 시도해 주세요."
        }
        budget = clampBudget(budget)
    }

    /** 테스트용 토큰 충전 (웹 chargeBtn) */
    suspend fun chargeWallet() {
        try {
            walletBalance = withContext(Dispatchers.IO) { repository.resetWallet(baseUrl) }
            setupError = null
            budget = clampBudget(budget)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            setupError = "충전에 실패했어요 — 서버 연결을 확인해 주세요."
        }
    }

    // ---- 프로필 ----
    var gender by mutableStateOf<String?>(null)       // "남" | "여" | null(선택 안 함)

    /**
     * "선택 안 함"을 명시적으로 고른 상태. [gender]는 두 경우 모두 null을 보내지만
     * 설문 UI에서 "아직 안 고름"과 구분해 칩을 표시하려면 별도 플래그가 필요하다.
     */
    var genderNotSpecified by mutableStateOf(false)

    /** [avoidWalking]은 Boolean이라 "아직 안 고름"을 표현할 수 없어 별도 플래그를 둔다. */
    var walkingAnswered by mutableStateOf(false)

    var ageGroup by mutableStateOf<String?>(null)
    var mbtiEI by mutableStateOf<Char?>(null)
    var mbtiSN by mutableStateOf<Char?>(null)
    var mbtiTF by mutableStateOf<Char?>(null)
    var mbtiJP by mutableStateOf<Char?>(null)
    var foodPreference by mutableStateOf<String?>(null)
    var avoidWalking by mutableStateOf(false)
    val keywords = mutableStateListOf<String>()
    var preferenceNote by mutableStateOf("")

    // ---- 프로필 설문 진행 상태 ----
    /** 현재 보여주는 질문 인덱스 (0 ~ ProfileQuestion.ordered.lastIndex) */
    var questionIndex by mutableIntStateOf(0)
        private set

    val question: ProfileQuestion get() = ProfileQuestion.ordered[questionIndex]
    val questionCount: Int get() = ProfileQuestion.ordered.size

    /** 프로필 설문 시작 — 항상 첫 질문부터 */
    fun startProfile() {
        questionIndex = 0
        screen = Screen.Profile
    }

    /** 다음 질문으로. 마지막 질문이면 플랜 생성으로 넘어간다. */
    fun nextQuestion() {
        if (questionIndex < ProfileQuestion.ordered.lastIndex) {
            questionIndex++
        } else {
            screen = Screen.Generating
        }
    }

    /** 이전 질문으로. 첫 질문에서는 여행 설정 화면으로 돌아간다. */
    fun previousQuestion() {
        if (questionIndex > 0) questionIndex-- else screen = Screen.Setup
    }

    private val repository = PlanRepository()

    private val mbtiOrNull: String?
        get() {
            val chars = listOfNotNull(mbtiEI, mbtiSN, mbtiTF, mbtiJP)
            return if (chars.size == 4) chars.joinToString("") else null
        }

    fun saveBaseUrl(url: String) {
        val cleaned = url.trim().ifBlank { DEFAULT_BASE_URL }
        baseUrl = cleaned
        onBaseUrlSaved(cleaned)
    }

    fun toggleKeyword(keyword: String) {
        if (!keywords.remove(keyword)) keywords.add(keyword)
    }

    private fun buildRequest() = PlanRequest(
        budget = budget,
        days = days,
        people = people,
        region = region,
        gender = when (gender) {
            "남" -> "MALE"
            "여" -> "FEMALE"
            else -> null
        },
        ageGroup = ageGroup,
        mbti = mbtiOrNull,
        purpose = purpose,
        foodPreference = foodPreference?.takeIf { it != "상관없음" },
        avoidWalking = avoidWalking,
        keywords = keywords.toList().ifEmpty { null },
        preferenceNote = preferenceNote.trim().ifBlank { null },
        startLatitude = null,
        startLongitude = null,
    )

    /**
     * 플랜 생성. Generating 화면의 LaunchedEffect에서 호출한다.
     * 취소로 화면을 벗어나면 코루틴이 취소되어 HTTP 요청도 함께 끊긴다.
     */
    suspend fun generatePlan() {
        errorMessage = null
        try {
            val response = withContext(Dispatchers.IO) {
                repository.createPlan(baseUrl, buildRequest())
            }
            plan = response
            completedDays = 0
            screen = Screen.Result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorMessage = e.message ?: "서버에 연결할 수 없습니다"
            screen = Screen.Profile
        }
    }

    /** 백엔드 없이 시연하기 위한 내장 데모 플랜 */
    fun loadDemoPlan() {
        plan = buildDemoPlan(budget, days, people, region)
        completedDays = 0
        errorMessage = null
        screen = Screen.Result
    }

    /** 웹 `.btn-ghost` "← 처음부터 다시" — 플랜을 버리고 설정 화면으로 */
    fun resetPlan() {
        plan = null
        completedDays = 0
        screen = Screen.Setup
    }

    fun onDayCompleted(dayIndex: Int) {
        if (dayIndex + 1 > completedDays) completedDays = dayIndex + 1
    }

    companion object {
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8080"
    }
}

/** 금액 포맷 — 웹 `common.js` 의 formatWon 과 같은 "300,000토큰" 꼴 */
fun won(value: Long): String = "%,d토큰".format(value)
