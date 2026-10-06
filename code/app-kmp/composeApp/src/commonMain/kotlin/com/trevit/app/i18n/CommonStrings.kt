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
    "opt.광주" to listOf("광주", "Gwangju", "光州", "光州"),
    "opt.김포" to listOf("김포", "Gimpo", "金浦", "金浦"),
    "opt.안산" to listOf("안산", "Ansan", "安山", "安山"),
    "opt.경기광주" to listOf("경기광주", "Gwangju (Gyeonggi)", "京畿広州", "京畿广州"),
    "opt.부산" to listOf("부산", "Busan", "釜山", "釜山"),
    "opt.대구" to listOf("대구", "Daegu", "大邱", "大邱"),
    "opt.대전" to listOf("대전", "Daejeon", "大田", "大田"),
    "opt.울산" to listOf("울산", "Ulsan", "蔚山", "蔚山"),
    "opt.세종" to listOf("세종", "Sejong", "世宗", "世宗"),
    "opt.경주" to listOf("경주", "Gyeongju", "慶州", "庆州"),
    "opt.전주" to listOf("전주", "Jeonju", "全州", "全州"),
    "opt.강릉" to listOf("강릉", "Gangneung", "江陵", "江陵"),
    "opt.속초" to listOf("속초", "Sokcho", "束草", "束草"),
    "opt.춘천" to listOf("춘천", "Chuncheon", "春川", "春川"),
    "opt.여수" to listOf("여수", "Yeosu", "麗水", "丽水"),
    "opt.순천" to listOf("순천", "Suncheon", "順天", "顺天"),
    "opt.통영" to listOf("통영", "Tongyeong", "統営", "统营"),
    "opt.거제" to listOf("거제", "Geoje", "巨済", "巨济"),
    "opt.안동" to listOf("안동", "Andong", "安東", "安东"),
    "opt.포항" to listOf("포항", "Pohang", "浦項", "浦项"),
    "opt.제주" to listOf("제주", "Jeju", "済州", "济州"),
    "opt.서귀포" to listOf("서귀포", "Seogwipo", "西帰浦", "西归浦"),

    // ---- 여행 유형 ----
    "opt.휴양" to listOf("휴양", "Relaxation", "リラックス", "休闲"),
    "opt.관광" to listOf("관광", "Sightseeing", "観光", "观光"),
    "opt.미식" to listOf("미식", "Food tour", "グルメ", "美食"),
    "opt.액티비티" to listOf("액티비티", "Activities", "アクティビティ", "户外活动"),
    "opt.쇼핑" to listOf("쇼핑", "Shopping", "ショッピング", "购物"),
    "opt.문화·예술" to listOf("문화·예술", "Culture & arts", "文化・芸術", "文化艺术"),

    // ---- 누구와 ----
    "opt.혼자" to listOf("혼자", "Solo", "ひとり", "独自"),
    "opt.연인" to listOf("연인", "Partner", "恋人", "恋人"),
    "opt.친구" to listOf("친구", "Friends", "友達", "朋友"),
    "opt.가족" to listOf("가족", "Family", "家族", "家人"),
    "opt.아이와 함께" to listOf("아이와 함께", "With kids", "子ども連れ", "带孩子"),

    // ---- 분위기 ----
    "opt.힙한 핫플" to listOf("힙한 핫플", "Trendy hotspots", "話題のスポット", "网红热门地"),
    "opt.조용한 힐링" to listOf("조용한 힐링", "Quiet & relaxing", "静かな癒やし", "安静疗愈"),
    "opt.로컬 감성" to listOf("로컬 감성", "Local feel", "ローカル感", "本地风情"),
    "opt.전통·역사" to listOf("전통·역사", "Tradition & history", "伝統・歴史", "传统历史"),
    "opt.자연" to listOf("자연", "Nature", "自然", "自然"),

    // ---- 꼭 해보고 싶은 것 ----
    "opt.카페 투어" to listOf("카페 투어", "Café hopping", "カフェ巡り", "咖啡馆探店"),
    "opt.야경" to listOf("야경", "Night views", "夜景", "夜景"),
    "opt.시장 구경" to listOf("시장 구경", "Markets", "市場めぐり", "逛市场"),
    "opt.전시·박물관" to listOf("전시·박물관", "Museums & exhibits", "展示・博物館", "展览博物馆"),
    "opt.산책" to listOf("산책", "Strolls", "散歩", "散步"),
    "opt.사진 명소" to listOf("사진 명소", "Photo spots", "写真スポット", "拍照胜地"),
    "opt.체험" to listOf("체험", "Hands-on activities", "体験", "体验活动"),

    // ---- 여행 페이스 ----
    "opt.여유롭게" to listOf("여유롭게 (하루 2곳)", "Relaxed (2 spots/day)", "ゆったり（1日2か所）", "悠闲（每天 2 处）"),
    "opt.적당히" to listOf("적당히 (하루 3곳)", "Balanced (3 spots/day)", "ほどほど（1日3か所）", "适中（每天 3 处）"),
    "opt.꽉 채워서" to listOf("꽉 채워서 (하루 4곳)", "Packed (4 spots/day)", "ぎっしり（1日4か所）", "紧凑（每天 4 处）"),

    // ---- 음식 ----
    "opt.한식" to listOf("한식", "Korean", "韓国料理", "韩餐"),
    "opt.양식" to listOf("양식", "Western", "洋食", "西餐"),
    "opt.일식" to listOf("일식", "Japanese", "和食", "日料"),
    "opt.중식" to listOf("중식", "Chinese", "中華料理", "中餐"),
    "opt.해산물" to listOf("해산물", "Seafood", "海鮮", "海鲜"),
    "opt.디저트" to listOf("디저트", "Desserts", "デザート", "甜点"),
    "opt.길거리 음식" to listOf("길거리 음식", "Street food", "屋台グルメ", "街头小吃"),
    "opt.상관없음" to listOf("상관없음", "Anything", "こだわらない", "都可以"),

    // ---- 가고 싶은 곳 ----
    "opt.산" to listOf("산", "Mountains", "山", "山"),
    "opt.바다" to listOf("바다", "Sea", "海", "海"),
    "opt.공원" to listOf("공원", "Parks", "公園", "公园"),
    "opt.강" to listOf("강", "Rivers", "川", "江河"),
    "opt.호수" to listOf("호수", "Lakes", "湖", "湖泊"),
    "opt.섬" to listOf("섬", "Islands", "島", "海岛"),

    // ---- 도보 ----
    "opt.괜찮아요" to listOf("괜찮아요", "Walking is fine", "大丈夫です", "没问题"),
    "opt.적게 걷고 싶어요" to listOf("적게 걷고 싶어요", "I'd rather walk less", "あまり歩きたくない", "希望少走路"),

    // ---- 기타(직접 입력) ----
    "opt.기타" to listOf("기타", "Other", "その他", "其他"),
)
