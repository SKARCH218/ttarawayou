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

/** 화면 라우팅 */
sealed interface Screen {
    data object Intro : Screen        // 브랜드 인트로(스플래시)
    data object Home : Screen
    data object Setup : Screen        // 여행 설정: 지역/예산/기간/인원
    data object Profile : Screen      // 프로필: 한 질문씩 넘기는 설문
    data object Generating : Screen   // 생성 중
    data object Result : Screen       // 결과 플랜
    data class Journey(val dayIndex: Int) : Screen
}

/**
 * 프로필 설문 질문 정의. 화면에는 한 번에 하나씩만 보여준다.
 * [autoAdvance]가 true면 값을 고르는 순간 다음 질문으로 넘어간다(단일 선택 질문).
 */
enum class ProfileQuestion(
    val emoji: String,
    val title: String,
    val autoAdvance: Boolean,
) {
    Purpose("🧭", "어떤 여행을 원하세요?", true),
    Gender("🙂", "성별을 알려주세요", true),
    AgeGroup("🎂", "연령대는 어떻게 되세요?", true),
    Mbti("🧩", "MBTI를 알려주세요", false),
    Food("🍚", "어떤 음식을 좋아하세요?", true),
    Places("🏞️", "어떤 곳에 가고 싶으세요?", false),
    Walking("🚶", "많이 걷는 건 괜찮으세요?", true),
    Note("💬", "더 알려주실 취향이 있나요?", false),
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
val DURATIONS = listOf("당일", "1박 2일", "2박 3일")
val GENDER_OPTIONS = listOf("남", "여", "선택 안 함")
val WALKING_OPTIONS = listOf("괜찮아요", "적게 걷고 싶어요")

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

    // ---- 여행 설정 ----
    var region by mutableStateOf<String?>(null)
    var purpose by mutableStateOf<String?>(null)
    var budget by mutableStateOf(300_000f)
    var durationIndex by mutableIntStateOf(1)   // 0 당일 / 1 1박2일 / 2 2박3일
    var people by mutableIntStateOf(1)

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

    val days: Int get() = durationIndex + 1

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
        budget = budget.toLong(),
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
        plan = buildDemoPlan(budget.toLong(), days, people, region)
        completedDays = 0
        errorMessage = null
        screen = Screen.Result
    }

    fun onDayCompleted(dayIndex: Int) {
        if (dayIndex + 1 > completedDays) completedDays = dayIndex + 1
    }

    companion object {
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8080"
    }
}

/** 원화 포맷 */
fun won(value: Long): String = "%,d원".format(value)
