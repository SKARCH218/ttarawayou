package com.trevit.service

import com.trevit.dto.PlanDtos
import com.trevit.dto.PlanDtos.PlanRequest
import com.trevit.entity.Place.PlaceType

/**
 * 취향 답 하나 → (1) 그 취향에 맞는 장소를 따로 찾아올 검색어, (2) 장소 정보에서 찾을 단어.
 * 고정 분류(관광명소·음식점)만 가져오면 같은 지역은 늘 같은 곳이 나오므로,
 * 취향별 검색 결과를 후보에 섞고 점수도 크게 올려 취향이 실제 일정에 드러나게 한다.
 */
object PreferenceKeywords {

    data class Pref(val label: String, val searches: List<Pair<String, PlaceType>>, val match: Regex)

    private val A = PlaceType.ATTRACTION
    private val R = PlaceType.RESTAURANT

    private fun p(label: String, match: String, vararg searches: Pair<String, PlaceType>) =
        Pref(label, searches.toList(), Regex(match))

    private val table: Map<String, Pref> = listOf(
        // ---- 여행 목적 ----
        p("휴양", "온천|스파|휴양|해변|정원|수목원|리조트", "온천" to A, "수목원" to A),
        p("관광", "궁|성곽|타워|명소|랜드마크|전망"),
        p("미식", "맛집|노포|시장|식당", "맛집" to R),
        p("액티비티", "체험|레저|서핑|카약|짚라인|루지|테마파크|놀이공원", "체험" to A, "테마파크" to A),
        p("쇼핑", "시장|아울렛|쇼핑|몰|백화점|거리", "쇼핑몰" to A, "아울렛" to A),
        p("문화·예술", "미술관|박물관|갤러리|공연|전시|아트", "미술관" to A, "공연장" to A),
        // ---- 분위기 ----
        p("힙한 핫플", "카페거리|핫플|팝업|편집숍|루프탑|골목", "카페거리" to A, "루프탑" to R),
        p("조용한 힐링", "수목원|정원|숲|산책|호수|한적|온천", "수목원" to A, "정원" to A),
        p("로컬 감성", "시장|골목|노포|마을|로컬", "전통시장" to A, "노포" to R),
        p("전통·역사", "궁|한옥|사찰|사$|성곽|향교|서원|유적|고분|박물관", "한옥마을" to A, "사찰" to A),
        p("자연", "산|숲|계곡|폭포|호수|바다|해변|수목원|자연|생태", "자연휴양림" to A, "계곡" to A),
        // ---- 꼭 해보고 싶은 것 ----
        p("카페 투어", "카페|커피|디저트|베이커리|로스터", "카페" to R),
        p("야경", "야경|전망대|타워|루프탑|대교|다리", "전망대" to A, "야경" to A),
        p("시장 구경", "시장|먹자골목|야시장", "전통시장" to A),
        p("전시·박물관", "박물관|미술관|전시|갤러리|기념관|과학관", "박물관" to A, "미술관" to A),
        p("산책", "산책|둘레길|공원|숲길|강변|호숫가|해안길", "산책로" to A, "공원" to A),
        p("사진 명소", "포토|사진|뷰|전망|벽화|정원|스카이", "포토존" to A, "벽화마을" to A),
        p("체험", "체험|공방|클래스|만들기|목장", "체험" to A, "공방" to A),
        // ---- 장소 유형 ----
        p("산", "(^|[^가-힣])산([^가-힣]|$)|[가-힣]{1,3}산$|봉$|등산", "산" to A),
        p("바다", "바다|해변|해수욕장|해안|항구|포구|등대", "해수욕장" to A, "해변" to A),
        p("공원", "공원", "공원" to A),
        p("강", "강$|강변|천변|나루", "강변" to A),
        p("호수", "호수|저수지|호반", "호수" to A),
        p("섬", "섬$|섬 ", "섬" to A),
        // ---- 음식 ----
        p("한식", "한식|국밥|백반|한정식|칼국수|비빔|삼겹|갈비|찌개|족발|보쌈|막국수|냉면", "한식" to R),
        p("양식", "파스타|스테이크|피자|브런치|버거|이탈리|프렌치|양식", "양식" to R),
        p("일식", "일식|스시|초밥|라멘|돈카츠|우동|이자카야|오마카세", "일식" to R),
        p("중식", "중식|짜장|짬뽕|중화|탕수육|마라|딤섬", "중식" to R),
        p("해산물", "해산물|횟집|회$|조개|대게|게장|물회|해물|수산", "횟집" to R, "해산물" to R),
        p("디저트", "디저트|베이커리|빵|케이크|빙수|제과", "디저트" to R, "베이커리" to R),
        p("길거리 음식", "분식|떡볶이|어묵|호떡|길거리|시장", "분식" to R),
    ).associateBy { it.label }

    /** 음식 질문에서 고른 값 ("상관없음" 제외) */
    fun foods(req: PlanRequest): List<String> =
        PlanDtos.splitChoices(req.foodPreference).filter { it != "상관없음" }

    /** 음식이 아닌 취향 값 (목적·분위기·하고 싶은 것·장소 유형) */
    fun others(req: PlanRequest): List<String> =
        (PlanDtos.splitChoices(req.purpose) + req.moods.orEmpty() + req.activities.orEmpty() +
            req.keywords.orEmpty()).distinct()

    fun of(label: String): Pref? = table[label]

    /**
     * 따로 찾아올 검색어 목록 (검색어, 장소 종류, 취향 라벨).
     * 취향마다 하나씩 돌아가며 뽑아 [limit] 개까지 — 외부 API 호출 수를 제한하면서 고르게 반영한다.
     * 표에 없는 값(직접 입력한 취향)은 그 단어 자체로 검색한다.
     */
    fun searches(req: PlanRequest, limit: Int): List<Triple<String, PlaceType, String>> {
        val lists = others(req).map { label -> label to (table[label]?.searches ?: listOf(label to A)) } +
            foods(req).map { label -> label to (table[label]?.searches ?: listOf(label to R)) }
        val out = ArrayList<Triple<String, PlaceType, String>>()
        var round = 0
        while (out.size < limit) {
            var added = false
            for ((label, searches) in lists) {
                val s = searches.getOrNull(round) ?: continue
                if (out.none { it.first == s.first }) out += Triple(s.first, s.second, label)
                added = true
                if (out.size >= limit) break
            }
            if (!added) break
            round++
        }
        return out
    }
}
