package com.trevit.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.trevit.app.i18n.AppLanguage
import com.trevit.app.i18n.translate
import com.trevit.app.map.getCurrentLocation
import com.trevit.app.ui.ThemeMode
import com.trevit.shared.PlanRepository
import com.trevit.shared.PlanRequest
import com.trevit.shared.PlanResponse
import com.trevit.shared.WalletProductDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 화면 라우팅. 웹과 같은 순서 — 인트로 → 설정 → 질문 8개 → 플랜 → 여정.
 * (웹에 없는 별도 홈 화면은 두지 않는다. 인트로가 끝나면 바로 설정이다.)
 */
sealed interface Screen {
    data object Intro : Screen        // 브랜드 인트로(스플래시)
    data object Login : Screen        // 로그인 (웹 login.html)
    data object Signup : Screen       // 회원가입 + 메일 인증 (웹 signup.html)
    data object Setup : Screen        // 여행 설정: 지역/예산/기간/인원
    data object Profile : Screen      // 취향 질문: 한 질문씩 넘기는 설문
    data object Generating : Screen   // 생성 중
    data object Result : Screen       // 결과 플랜
    data class Journey(val dayIndex: Int) : Screen
    data object Settings : Screen     // 설정 (계정·언어·테마·앱 정보·개발자 옵션)
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
    // 모든 질문을 [다음] 버튼으로 통일 — 선택 즉시 넘어가지 않는다
    val hint: String? = null,
    val wide: Boolean = false,
) {
    Purpose("🧭", "어떤 여행을 원하세요?", "여러 개 고를 수 있어요"),
    Gender("🙂", "성별을 알려주세요", "취향이 비슷한 여행자의 코스를 참고해요", wide = true),
    AgeGroup("🎂", "연령대는 어떻게 되세요?"),
    Mbti("🧩", "MBTI를 알려주세요", "모르면 건너뛰어도 괜찮아요"),
    Food("🍚", "어떤 음식을 좋아하세요?", "여러 개 고를 수 있어요"),
    Places("🏞️", "어떤 곳에 가고 싶으세요?", "여러 개 고를 수 있어요"),
    MustVisit("📍", "꼭 가고 싶은 곳이 있나요?", "장소 이름을 적으면 일정에 꼭 넣어 드려요 (최대 5곳)"),
    Walking("🚶", "많이 걷는 건 괜찮으세요?", wide = true),
    Note("💬", "더 알려주실 취향이 있나요?", "AI가 장소를 고를 때 참고해요"),
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
const val NO_FOOD_PREFERENCE = "상관없음"
const val MAX_MUST_VISIT = 5

/** 웹 `js/setup.js` 와 같은 예산 한계 */
const val MIN_BUDGET = 50_000L
const val BUDGET_STEP = 10_000L

/** 앱 전역 상태 홀더 (단일 Activity + Compose 상태 기반 내비게이션) */
class AppState(
    initialBaseUrl: String,
    private val onBaseUrlSaved: (String) -> Unit,
    initialAuthToken: String? = null,
    onAuthTokenSaved: (String?) -> Unit = {},
    /** 언어·테마 등 설정 저장소 */
    private val prefs: AppPrefs = AppPrefs.None,
    /** 기기 언어 코드 ("ko", "en-US" …) — 저장된 언어가 없을 때 기본값으로 쓴다 */
    systemLanguageCode: String? = null,
) {
    var baseUrl by mutableStateOf(initialBaseUrl)
        private set

    // ---- 앱 설정 (설정 화면) ----

    /** 화면 언어. 저장값 → 기기 언어 → 한국어 순 */
    var language by mutableStateOf(AppLanguage.fromCode(prefs.get(PREF_LANGUAGE) ?: systemLanguageCode))
        private set

    /** 화면 테마 (시스템/라이트/다크) */
    var themeMode by mutableStateOf(ThemeMode.fromCode(prefs.get(PREF_THEME)))
        private set

    fun changeLanguage(value: AppLanguage) {
        language = value
        prefs.put(PREF_LANGUAGE, value.code)
    }

    fun changeTheme(value: ThemeMode) {
        themeMode = value
        prefs.put(PREF_THEME, value.code)
    }

    /** 상태 클래스 안에서 쓰는 번역 (화면에서는 tr() 을 쓴다) */
    fun t(key: String, vararg args: Any?): String = translate(language, key, *args)

    /** 로그인 상태 — 웹과 같은 `/api/auth` 엔드포인트를 쓴다 */
    val auth = AuthState(
        baseUrlProvider = { baseUrl },
        initialToken = initialAuthToken,
        onTokenSaved = onAuthTokenSaved,
        languageProvider = { language },
    )
    var screen by mutableStateOf<Screen>(Screen.Intro)
    var plan by mutableStateOf<PlanResponse?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
    var completedDays by mutableIntStateOf(0)

    /** 여정 지도 음성 안내 켜짐 여부 */
    var voiceEnabled by mutableStateOf(true)

    // ---- 여행 설정 (웹 index.html + setup.js 와 같은 항목·한계) ----
    // 웹 select 의 첫 항목이 서울이라 앱도 같은 기본값에서 출발한다
    var region by mutableStateOf<String?>(REGIONS.first())
    val purposes = mutableStateListOf<String>()
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
            walletBalance = withContext(Dispatchers.Default) { repository.fetchWallet(baseUrl) }
            setupError = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            walletBalance = 0
            setupError = t("state.walletLoadFailed")
        }
        budget = clampBudget(budget)
    }

    // ---- 토큰 구매 (우측 상단 "토큰 구매" 버튼 → 상품 목록 다이얼로그) ----

    var showStore by mutableStateOf(false)
    var storeProducts by mutableStateOf<List<WalletProductDto>>(emptyList())
    var storeLoading by mutableStateOf(false)
    var storeError by mutableStateOf<String?>(null)
    /** 방금 구매를 완료한 상품 id — 다이얼로그에 짧게 "구매 완료"를 보여주는 용도 */
    var storeJustPurchasedId by mutableStateOf<String?>(null)
    /** 구매 버튼을 누른 상품 id (그 버튼만 로딩 표시) */
    var storePendingId by mutableStateOf<String?>(null)

    fun openStore() {
        showStore = true
        storeError = null
        storeJustPurchasedId = null
    }

    /** GET /api/wallet/products — 스토어를 열 때 한 번 불러온다 */
    suspend fun loadStoreProducts() {
        if (storeProducts.isNotEmpty()) return
        storeLoading = true
        try {
            storeProducts = withContext(Dispatchers.Default) { repository.fetchWalletProducts(baseUrl) }
            storeError = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            storeError = t("state.storeLoadFailed")
        } finally {
            storeLoading = false
        }
    }

    /** POST /api/wallet/purchase — 1토큰 = 1원 고정환율, 결제 시뮬레이션 */
    suspend fun purchase(productId: String) {
        storePendingId = productId
        try {
            walletBalance = withContext(Dispatchers.Default) { repository.purchaseWallet(baseUrl, productId) }
            storeJustPurchasedId = productId
            storeError = null
            setupError = null
            budget = clampBudget(budget)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            storeError = t("state.purchaseFailed")
        } finally {
            storePendingId = null
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
    val foodPreferences = mutableStateListOf<String>()
    var avoidWalking by mutableStateOf(false)
    val keywords = mutableStateListOf<String>()
    val mustVisit = mutableStateListOf<String>()
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

    fun togglePurpose(purpose: String) {
        if (!purposes.remove(purpose)) purposes.add(purpose)
    }

    /** "상관없음"은 다른 음식과 함께 고를 수 없다 — 고르면 나머지를 비우고, 다른 걸 고르면 빠진다 */
    fun toggleFood(food: String) {
        if (foodPreferences.remove(food)) return
        if (food == NO_FOOD_PREFERENCE) foodPreferences.clear() else foodPreferences.remove(NO_FOOD_PREFERENCE)
        foodPreferences.add(food)
    }

    fun addMustVisit(name: String) {
        val t = name.trim()
        if (t.isNotEmpty() && t !in mustVisit && mustVisit.size < MAX_MUST_VISIT) mustVisit.add(t)
    }

    private fun buildRequest(startLat: Double? = null, startLng: Double? = null) = PlanRequest(
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
        purpose = purposes.joinToString(", ").ifEmpty { null },
        foodPreference = foodPreferences.filter { it != NO_FOOD_PREFERENCE }.joinToString(", ").ifEmpty { null },
        avoidWalking = avoidWalking,
        keywords = keywords.toList().ifEmpty { null },
        preferenceNote = preferenceNote.trim().ifBlank { null },
        mustVisit = mustVisit.toList().ifEmpty { null },
        // 현재 위치가 있으면 1일차를 현재 위치에서 출발시킨다 (없으면 백엔드가 숙소 출발로 폴백)
        startLatitude = startLat,
        startLongitude = startLng,
        // AI가 플랜 설명을 사용자 화면 언어로 쓰게 한다
        language = language.code,
    )

    /**
     * 플랜 생성. Generating 화면의 LaunchedEffect에서 호출한다.
     * 취소로 화면을 벗어나면 코루틴이 취소되어 HTTP 요청도 함께 끊긴다.
     */
    suspend fun generatePlan() {
        errorMessage = null
        try {
            // 현재 위치를 먼저 얻어 1일차 출발지로 쓴다 (권한 없거나 실패하면 null → 숙소 출발)
            val loc = withContext(Dispatchers.Default) { getCurrentLocation() }
            val response = withContext(Dispatchers.Default) {
                repository.createPlan(baseUrl, buildRequest(loc?.first, loc?.second))
            }
            plan = response
            completedDays = 0
            screen = Screen.Result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorMessage = e.message ?: t("state.serverUnreachable")
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

        /** 설정 화면 '앱 정보'에 표시 (composeApp versionName 과 맞춘다) */
        const val APP_VERSION = "1.0"

        private const val PREF_LANGUAGE = "language"
        private const val PREF_THEME = "theme"
    }
}

/** 금액 포맷 — 웹 `common.js` 의 formatWon 과 같은 "300,000토큰" 꼴 */
fun won(value: Long): String = "${comma(value)}토큰"
