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
    data object Home : Screen
    data object Setup : Screen        // 여행 설정: 지역/목적/예산/기간/인원
    data object Profile : Screen      // 프로필: 성별/연령/MBTI/음식/키워드
    data object Generating : Screen   // 생성 중
    data object Result : Screen       // 결과 플랜
    data class Journey(val dayIndex: Int) : Screen
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

/** 앱 전역 상태 홀더 (단일 Activity + Compose 상태 기반 내비게이션) */
class AppState(
    initialBaseUrl: String,
    private val onBaseUrlSaved: (String) -> Unit,
) {
    var baseUrl by mutableStateOf(initialBaseUrl)
        private set
    var screen by mutableStateOf<Screen>(Screen.Home)
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
    var ageGroup by mutableStateOf<String?>(null)
    var mbtiEI by mutableStateOf<Char?>(null)
    var mbtiSN by mutableStateOf<Char?>(null)
    var mbtiTF by mutableStateOf<Char?>(null)
    var mbtiJP by mutableStateOf<Char?>(null)
    var foodPreference by mutableStateOf<String?>(null)
    var avoidWalking by mutableStateOf(false)
    val keywords = mutableStateListOf<String>()
    var preferenceNote by mutableStateOf("")

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
