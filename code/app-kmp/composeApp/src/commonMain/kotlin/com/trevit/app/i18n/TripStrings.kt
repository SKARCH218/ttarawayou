package com.trevit.app.i18n

/** 플랜 결과·여정(지도)·공용 컴포넌트 화면 번역. listOf(한국어, English, 日本語, 中文) */
internal val tripStrings: Map<String, List<String>> = mapOf(
    // ---- 플랜 결과 (ResultScreen) ----
    "result.title" to listOf("플랜 완성", "Your plan is ready", "プラン完成", "行程已完成"),
    "result.subtitle" to listOf(
        "장소는 도착할 때 공개돼요",
        "Places are revealed when you arrive",
        "場所は到着したときに公開されます",
        "地点将在抵达时揭晓",
    ),
    "result.plannedByAi" to listOf("AI가 설계한 플랜", "Plan designed by AI", "AIが設計したプラン", "由 AI 设计的行程"),
    "result.plannedByAlgo" to listOf(
        "알고리즘이 설계한 플랜",
        "Plan designed by our algorithm",
        "アルゴリズムが設計したプラン",
        "由算法设计的行程",
    ),
    "result.aiNote" to listOf("AI의 한마디", "A word from AI", "AIからひとこと", "AI 的一句话"),
    "result.totalBudget" to listOf("총 예산", "Total budget", "総予算", "总预算"),
    "result.estimatedCost" to listOf("예상 총비용", "Estimated total", "予想総費用", "预计总费用"),
    "result.remainingBudget" to listOf("남는 예산", "Budget left", "残る予算", "剩余预算"),
    "result.tokenBalance" to listOf("남은 토큰", "Tokens left", "残りトークン", "剩余代币"),
    "result.budgetUsage" to listOf("예산 배분 사용률", "Budget usage by category", "予算配分の使用率", "预算分配使用率"),
    "result.lodging" to listOf("숙박", "Lodging", "宿泊", "住宿"),
    "result.attraction" to listOf("관광", "Sightseeing", "観光", "观光"),
    "result.food" to listOf("식비", "Food", "食費", "餐饮"),
    "result.transport" to listOf("교통", "Transport", "交通", "交通"),
    "result.restart" to listOf("← 처음부터 다시", "← Start over", "← 最初からやり直す", "← 重新开始"),
    "result.badgeLocked" to listOf("잠김", "Locked", "ロック", "未解锁"),
    "result.badgeDone" to listOf("완료", "Done", "完了", "已完成"),
    "result.dayTitleFollow" to listOf(
        "Day {0} 여정 따라가기",
        "Follow Day {0} journey",
        "{0}日目の旅程をたどる",
        "跟随第{0}天行程",
    ),
    "result.dayTitleDone" to listOf(
        "Day {0} 여정 (완료)",
        "Day {0} journey (done)",
        "{0}日目の旅程（完了）",
        "第{0}天行程（已完成）",
    ),
    "result.dayInfoDone" to listOf(
        "완료한 여정 · 다시 보기",
        "Completed journey · View again",
        "完了した旅程 · もう一度見る",
        "已完成的行程 · 再看一遍",
    ),
    "result.dayInfoLocked" to listOf(
        "Day {0} 완료 후 열려요 · {1} 시작 예정",
        "Unlocks after Day {0} · Starts at {1}",
        "{0}日目の完了後に開きます · {1} 開始予定",
        "完成第{0}天后解锁 · 预计 {1} 开始",
    ),
    "result.dayInfo" to listOf(
        "{0} 시작 · 비밀 장소 {1}곳 · 이동 약 {2}분 · {3}",
        "Starts {0} · {1} secret spots · ~{2} min travel · {3}",
        "{0} 開始 · 秘密の場所 {1}か所 · 移動 約{2}分 · {3}",
        "{0} 出发 · {1} 个秘密地点 · 移动约 {2} 分钟 · {3}",
    ),

    // ---- 여정 지도 (JourneyScreen) ----
    "journey.title" to listOf("Day {0} 여정", "Day {0} journey", "{0}日目の旅程", "第{0}天行程"),
    "journey.toNext" to listOf(
        "다음 비밀 장소까지 {0} · 약 {1}분",
        "{0} to the next secret spot · ~{1} min",
        "次の秘密の場所まで {0} · 約{1}分",
        "距下一个秘密地点 {0} · 约 {1} 分钟",
    ),
    "journey.progress" to listOf("비밀 장소 {0} / {1}", "Secret spots {0} / {1}", "秘密の場所 {0} / {1}", "秘密地点 {0} / {1}"),
    "journey.boardAlight" to listOf(
        "{0} 승차 → {1} 하차",
        "Board at {0} → Get off at {1}",
        "{0} で乗車 → {1} で下車",
        "{0} 上车 → {1} 下车",
    ),
    "journey.stopFallback" to listOf("정류장", "stop", "停留所", "车站"),
    "journey.alightNow" to listOf(
        "이번 정거장에서 하차하세요",
        "Get off at this stop",
        "この停留所で降りてください",
        "请在本站下车",
    ),
    "journey.stopsLeft" to listOf("하차까지 {0}정거장", "{0} stops to go", "下車まで {0} 停留所", "还有 {0} 站下车"),
    "journey.hintPlaying" to listOf(
        "보라색 길을 따라가는 중이에요",
        "Following the purple path",
        "紫の道をたどっています",
        "正在沿着紫色路线前进",
    ),
    "journey.hintIdle" to listOf(
        "보라색 길을 따라가세요 — 시뮬레이션을 눌러 주세요",
        "Follow the purple path — tap Simulate",
        "紫の道をたどってください — シミュレーションを押してください",
        "请沿着紫色路线前进 — 点击“模拟”",
    ),
    "journey.simulate" to listOf("시뮬레이션", "Simulate", "シミュレーション", "模拟"),
    "journey.pause" to listOf("일시정지", "Pause", "一時停止", "暂停"),
    "journey.backCd" to listOf("뒤로", "Back", "戻る", "返回"),
    "journey.revealArrived" to listOf("도착! 이곳은…", "You've arrived! This place is…", "到着！ここは…", "到达！这里是…"),
    "journey.revealLast" to listOf(
        "오늘의 여정 완료! 마지막 장소는…",
        "Today's journey complete! The last place is…",
        "今日の旅程完了！最後の場所は…",
        "今日行程完成！最后一站是…",
    ),
    "journey.mysteryPlace" to listOf("미스터리 장소", "Mystery place", "ミステリースポット", "神秘地点"),
    "journey.nextPlace" to listOf("다음 비밀 장소로 →", "To the next secret spot →", "次の秘密の場所へ →", "前往下一个秘密地点 →"),
    "journey.finish" to listOf("여정 마치기", "Finish journey", "旅程を終える", "结束行程"),
    "journey.dayDone" to listOf("Day {0} 완료", "Day {0} complete", "{0}日目 完了", "第{0}天完成"),
    "journey.tripDone" to listOf("여행 완료", "Trip complete", "旅行完了", "旅行完成"),
    "journey.todayCost" to listOf("오늘 쓴 비용 {0}", "Spent today: {0}", "今日の費用 {0}", "今日花费 {0}"),
    "journey.totalSummary" to listOf(
        "총 비용 {0} · 남는 예산 {1}",
        "Total cost {0} · Budget left {1}",
        "総費用 {0} · 残る予算 {1}",
        "总费用 {0} · 剩余预算 {1}",
    ),
    "journey.startDay" to listOf("Day {0} 시작", "Start Day {0}", "{0}日目を始める", "开始第{0}天"),
    "journey.backToPlan" to listOf("플랜으로 돌아가기", "Back to plan", "プランに戻る", "返回行程"),

    // ---- 공용 컴포넌트 (Components.kt) ----
    "comp.decrease" to listOf("감소", "Decrease", "減らす", "减少"),
    "comp.increase" to listOf("증가", "Increase", "増やす", "增加"),

    // ---- 장소 유형 (map/Geo.kt) ----
    "geo.lodging" to listOf("숙소", "Lodging", "宿泊先", "住宿"),
    "geo.restaurant" to listOf("맛집", "Restaurant", "グルメ", "美食"),
    "geo.attraction" to listOf("관광지", "Attraction", "観光地", "景点"),
    "geo.start" to listOf("출발지", "Starting point", "出発地", "出发地"),
    "geo.mystery" to listOf("미스터리", "Mystery", "ミステリー", "神秘"),
)
