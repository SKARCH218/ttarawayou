package com.trevit.app.i18n

/**
 * 공용 번역 — 선택지 값 표시명(opt.*), 공통 버튼, 앱 전역 다이얼로그.
 * 각 항목: listOf(한국어, English, 日本語, 中文)
 */
internal val commonStrings: Map<String, List<String>> = mapOf(
    // ---- 공통 버튼 ----
    "common.ok" to listOf("확인", "OK", "OK", "确定"),
    "common.cancel" to listOf("취소", "Cancel", "キャンセル", "取消"),
    "common.save" to listOf("저장", "Save", "保存", "保存"),
    "common.close" to listOf("닫기", "Close", "閉じる", "关闭"),
    "common.next" to listOf("다음", "Next", "次へ", "下一步"),
    "common.back" to listOf("이전", "Back", "戻る", "上一步"),
    "common.change" to listOf("변경", "Change", "変更", "更改"),
    "common.retry" to listOf("다시 시도", "Try again", "再試行", "重试"),

    // ---- 단위 ----
    "unit.tokens" to listOf("{0}토큰", "{0} tokens", "{0}トークン", "{0} 代币"),

    // ---- 앱 전역: 플랜 생성 실패 다이얼로그 ----
    "app.planFailed" to listOf("플랜 생성 실패", "Couldn't create a plan", "プランを作成できませんでした", "行程生成失败"),
    "app.demoPlan" to listOf("데모 플랜", "Demo plan", "デモプラン", "演示行程"),
    "app.serverUnreachable" to listOf(
        "서버에 연결할 수 없습니다",
        "Can't reach the server",
        "サーバーに接続できません",
        "无法连接到服务器",
    ),

    // ---- 지역 (값은 한국어로 서버에 보낸다) ----
    "opt.서울" to listOf("서울", "Seoul", "ソウル", "首尔"),
    "opt.인천" to listOf("인천", "Incheon", "仁川", "仁川"),
    "opt.강화" to listOf("강화", "Ganghwa", "江華", "江华"),
    "opt.수원" to listOf("수원", "Suwon", "水原", "水原"),
    "opt.가평" to listOf("가평", "Gapyeong", "加平", "加平"),
    "opt.양평" to listOf("양평", "Yangpyeong", "楊平", "杨平"),
    "opt.파주" to listOf("파주", "Paju", "坡州", "坡州"),
    "opt.포천" to listOf("포천", "Pocheon", "抱川", "抱川"),
    "opt.용인" to listOf("용인", "Yongin", "龍仁", "龙仁"),
    "opt.남양주" to listOf("남양주", "Namyangju", "南楊州", "南杨州"),
    "opt.이천" to listOf("이천", "Icheon", "利川", "利川"),
    "opt.여주" to listOf("여주", "Yeoju", "驪州", "骊州"),
    "opt.화성" to listOf("화성", "Hwaseong", "華城", "华城"),
    "opt.시흥" to listOf("시흥", "Siheung", "始興", "始兴"),
    "opt.과천" to listOf("과천", "Gwacheon", "果川", "果川"),
    "opt.광주" to listOf("광주", "Gwangju", "広州", "广州"),
    "opt.김포" to listOf("김포", "Gimpo", "金浦", "金浦"),
    "opt.안산" to listOf("안산", "Ansan", "安山", "安山"),

    // ---- 여행 유형 ----
    "opt.휴양" to listOf("휴양", "Relaxation", "リラックス", "休闲"),
    "opt.관광" to listOf("관광", "Sightseeing", "観光", "观光"),
    "opt.미식" to listOf("미식", "Food tour", "グルメ", "美食"),
    "opt.액티비티" to listOf("액티비티", "Activities", "アクティビティ", "户外活动"),

    // ---- 성별 ----
    "opt.남" to listOf("남", "Male", "男性", "男"),
    "opt.여" to listOf("여", "Female", "女性", "女"),
    "opt.선택 안 함" to listOf("선택 안 함", "Prefer not to say", "回答しない", "不透露"),

    // ---- 연령대 ----
    "opt.10대" to listOf("10대", "Teens", "10代", "10多岁"),
    "opt.20대" to listOf("20대", "20s", "20代", "20多岁"),
    "opt.30대" to listOf("30대", "30s", "30代", "30多岁"),
    "opt.40대" to listOf("40대", "40s", "40代", "40多岁"),
    "opt.50대+" to listOf("50대+", "50s+", "50代以上", "50岁以上"),

    // ---- 음식 ----
    "opt.한식" to listOf("한식", "Korean", "韓国料理", "韩餐"),
    "opt.양식" to listOf("양식", "Western", "洋食", "西餐"),
    "opt.일식" to listOf("일식", "Japanese", "和食", "日料"),
    "opt.중식" to listOf("중식", "Chinese", "中華料理", "中餐"),
    "opt.상관없음" to listOf("상관없음", "Anything", "こだわらない", "都可以"),

    // ---- 가고 싶은 곳 ----
    "opt.산" to listOf("산", "Mountains", "山", "山"),
    "opt.바다" to listOf("바다", "Sea", "海", "海"),
    "opt.공원" to listOf("공원", "Parks", "公園", "公园"),
    "opt.강" to listOf("강", "Rivers", "川", "江河"),

    // ---- 도보 ----
    "opt.괜찮아요" to listOf("괜찮아요", "Walking is fine", "大丈夫です", "没问题"),
    "opt.적게 걷고 싶어요" to listOf("적게 걷고 싶어요", "I'd rather walk less", "あまり歩きたくない", "希望少走路"),

    // ---- 기타(직접 입력) ----
    "opt.기타" to listOf("기타", "Other", "その他", "其他"),
)
