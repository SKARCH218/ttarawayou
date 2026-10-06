package com.trevit.service

/**
 * 알고리즘(휴리스틱) 플랜 설명 문구 — AI가 응답하지 못했을 때 "AI의 한마디" 자리에 쓴다.
 * 앱 화면 언어(ko | en | ja | zh)에 맞춰 만든다.
 * 템플릿: listOf(한국어, English, 日本語, 中文), {0}·{1}… 은 값으로 치환.
 */
object PlanDescriber {

    private val templates: Map<String, List<String>> = mapOf(
        "area" to listOf("{0} {1} 일대", "{1}, {0}", "{0} {1}周辺", "{0}{1}一带"),
        "intro" to listOf(
            "{0} 중심으로 {1}일 일정에 볼거리 {2}곳과 식사 {3}번을 배치했어요.",
            "We planned a {1}-day trip around {0} with {2} sights and {3} meals.",
            "{0}を中心に、{1}日間の日程に見どころ{2}か所と食事{3}回を組み込みました。",
            "以{0}为中心，在{1}天的行程中安排了{2}个景点和{3}顿饭。",
        ),
        "purpose" to listOf(
            "'{0}' 목적에 맞게 이동 부담이 적은 순서로 묶었어요.",
            "Stops are grouped in an easy-going order to suit your {0} trip.",
            "「{0}」の目的に合わせ、移動の負担が少ない順にまとめました。",
            "根据“{0}”的旅行目的，按移动负担较小的顺序进行了安排。",
        ),
        "avoidWalking" to listOf(
            "걷기 최소화를 켜셨으니 산·등산 코스는 빼고 이동 거리를 줄였어요.",
            "Since you prefer less walking, we skipped hiking spots and kept distances short.",
            "歩く量を減らす設定なので、登山コースを外して移動距離を短くしました。",
            "由于你希望少走路，已去掉爬山路线并缩短了移动距离。",
        ),
        "keywords" to listOf(
            "{0} 키워드에 맞는 장소를 우선 골랐어요.",
            "Places matching {0} were picked first.",
            "{0}のキーワードに合う場所を優先して選びました。",
            "优先选择了符合“{0}”的地点。",
        ),
        "food" to listOf(
            "식사는 {0} 위주로 찾았어요.",
            "Meals focus on {0} food.",
            "食事は{0}を中心に選びました。",
            "餐食以{0}为主。",
        ),
        "mbti" to listOf(
            "{0} 성향도 참고했어요.",
            "Your {0} personality was taken into account too.",
            "{0}の傾向も参考にしました。",
            "也参考了你的{0}性格。",
        ),
    )

    /** 앱이 서버로 보내는 한국어 선택지 값 → 표시명. 모르는 값(사용자가 직접 입력한 '기타')은 그대로 */
    private val labels: Map<String, List<String>> = mapOf(
        // 지역 (RegionService 이름) + 특수값
        "현재 위치" to listOf("현재 위치", "your current location", "現在地", "当前位置"),
        "서울" to listOf("서울", "Seoul", "ソウル", "首尔"),
        "인천" to listOf("인천", "Incheon", "仁川", "仁川"),
        "강화" to listOf("강화", "Ganghwa", "江華", "江华"),
        "수원" to listOf("수원", "Suwon", "水原", "水原"),
        "가평" to listOf("가평", "Gapyeong", "加平", "加平"),
        "양평" to listOf("양평", "Yangpyeong", "楊平", "杨平"),
        "파주" to listOf("파주", "Paju", "坡州", "坡州"),
        "포천" to listOf("포천", "Pocheon", "抱川", "抱川"),
        "용인" to listOf("용인", "Yongin", "龍仁", "龙仁"),
        "남양주" to listOf("남양주", "Namyangju", "南楊州", "南杨州"),
        "이천" to listOf("이천", "Icheon", "利川", "利川"),
        "여주" to listOf("여주", "Yeoju", "驪州", "骊州"),
        "화성" to listOf("화성", "Hwaseong", "華城", "华城"),
        "시흥" to listOf("시흥", "Siheung", "始興", "始兴"),
        "과천" to listOf("과천", "Gwacheon", "果川", "果川"),
        "광주" to listOf("광주", "Gwangju", "広州", "广州"),
        "김포" to listOf("김포", "Gimpo", "金浦", "金浦"),
        "안산" to listOf("안산", "Ansan", "安山", "安山"),
        "송도" to listOf("송도", "Songdo", "松島", "松岛"),
        "고양" to listOf("고양", "Goyang", "高陽", "高阳"),
        "연천" to listOf("연천", "Yeoncheon", "漣川", "涟川"),
        // 여행 목적
        "휴양" to listOf("휴양", "relaxation", "リラックス", "休闲"),
        "관광" to listOf("관광", "sightseeing", "観光", "观光"),
        "미식" to listOf("미식", "food", "グルメ", "美食"),
        "액티비티" to listOf("액티비티", "activity", "アクティビティ", "户外活动"),
        // 음식
        "한식" to listOf("한식", "Korean", "韓国料理", "韩餐"),
        "양식" to listOf("양식", "Western", "洋食", "西餐"),
        "일식" to listOf("일식", "Japanese", "和食", "日料"),
        "중식" to listOf("중식", "Chinese", "中華料理", "中餐"),
        // 가고 싶은 곳
        "산" to listOf("산", "mountains", "山", "山"),
        "바다" to listOf("바다", "the sea", "海", "海"),
        "공원" to listOf("공원", "parks", "公園", "公园"),
        "강" to listOf("강", "rivers", "川", "江河"),
    )

    private fun index(lang: String?): Int = when (lang?.lowercase()?.take(2)) {
        "en" -> 1
        "ja" -> 2
        "zh" -> 3
        else -> 0
    }

    private fun t(i: Int, key: String, vararg args: Any?): String {
        var s = templates.getValue(key)[i]
        args.forEachIndexed { n, a -> s = s.replace("{$n}", a.toString()) }
        return s
    }

    private fun label(i: Int, value: String): String = labels[value]?.get(i) ?: value

    fun describe(
        lang: String?,
        regionName: String,
        zoneLabel: String?,
        days: Int,
        spots: Int,
        meals: Int,
        purpose: String?,
        avoidWalking: Boolean,
        keywords: List<String>?,
        foodPreference: String?,
        mbti: String?,
    ): String {
        val i = index(lang)
        val region = label(i, regionName)
        val area = if (zoneLabel != null) t(i, "area", region, zoneLabel) else region
        val listSep = if (i == 1) ", " else "·"

        val parts = ArrayList<String>()
        parts += t(i, "intro", area, days, spots, meals)
        purpose?.let { parts += t(i, "purpose", label(i, it)) }
        if (avoidWalking) parts += t(i, "avoidWalking")
        keywords?.takeIf { it.isNotEmpty() }?.let { kw ->
            parts += t(i, "keywords", kw.joinToString(listSep) { label(i, it) })
        }
        foodPreference?.takeIf { it != "상관없음" }?.let { parts += t(i, "food", label(i, it)) }
        mbti?.let { parts += t(i, "mbti", it) }
        // 일본어·중국어는 문장 사이에 띄어쓰기를 하지 않는다
        return parts.joinToString(if (i >= 2) "" else " ")
    }
}
