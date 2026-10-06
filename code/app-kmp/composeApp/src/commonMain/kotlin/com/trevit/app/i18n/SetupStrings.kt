package com.trevit.app.i18n

/** 여행 설정·취향 질문·플랜 생성 중 화면 번역. listOf(한국어, English, 日本語, 中文) */
internal val setupStrings: Map<String, List<String>> = mapOf(
    // ---- 여행 설정 화면 (SetupScreen) ----
    "setup.settings" to listOf("설정", "Settings", "設定", "设置"),
    "setup.logo" to listOf("트레빗", "Travit", "Travit", "Travit"),
    "setup.title" to listOf("어디로 떠나볼까요?", "Where shall we go?", "どこへ出かけましょうか？", "想去哪里呢？"),
    "setup.subtitle" to listOf(
        "장소는 도착 전까지 비밀",
        "Places stay secret until you arrive",
        "行き先は到着までヒミツ",
        "目的地到达前保密",
    ),
    "setup.nickname" to listOf("{0}님", "{0}", "{0}さん", "{0}"),
    "setup.region" to listOf("지역", "Region", "地域", "地区"),
    "setup.regionSearch" to listOf("지역 검색", "Search regions", "地域を検索", "搜索地区"),
    "setup.budget" to listOf("예산", "Budget", "予算", "预算"),
    "setup.tokenUnit" to listOf("토큰", "tokens", "トークン", "代币"),
    "setup.balance" to listOf("보유 ", "Balance ", "保有 ", "余额 "),
    "setup.days" to listOf("기간", "Duration", "期間", "天数"),
    "setup.daysUnit" to listOf("일", "days", "日", "天"),
    "setup.people" to listOf("인원", "Travelers", "人数", "人数"),
    "setup.peopleUnit" to listOf("명", "people", "名", "人"),
    "setup.notEnoughTokens" to listOf(
        "보유 토큰({0})이 부족해요. 보유 토큰 옆 + 버튼으로 충전해 주세요.",
        "Not enough tokens (you have {0}). Tap + next to your balance to top up.",
        "保有トークン（{0}）が足りません。残高の横の＋ボタンからチャージしてください。",
        "代币余额（{0}）不足，请点击余额旁的 + 按钮充值。",
    ),

    // ---- 토큰 구매 (AddTokensButton / StoreDialog) ----
    "setup.buyTokens" to listOf("토큰 구매", "Buy tokens", "トークン購入", "购买代币"),
    "setup.storeRate" to listOf(
        "1토큰 = 1원 고정환율 · 실제 결제 없이 바로 충전돼요",
        "Fixed rate: 1 token = ₩1 · Added instantly, no real payment",
        "1トークン = 1ウォンの固定レート · 実際の決済なしですぐにチャージされます",
        "固定汇率 1 代币 = 1 韩元 · 无需实际付款，立即到账",
    ),
    "setup.loading" to listOf("불러오는 중…", "Loading…", "読み込み中…", "加载中…"),
    "setup.buy" to listOf("구매", "Buy", "購入", "购买"),
    "setup.purchased" to listOf("구매 완료", "Purchased", "購入完了", "已购买"),

    // ---- 취향 질문 (ProfileQuestion 의 title/hint 와 한국어가 같아야 한다) ----
    "profile.q.Purpose.title" to listOf(
        "어떤 여행을 원하세요?",
        "What kind of trip do you want?",
        "どんな旅行がしたいですか？",
        "你想要什么样的旅行？",
    ),
    "profile.q.TravelWith.title" to listOf("누구와 함께 가세요?", "Who are you traveling with?", "誰と行きますか？", "和谁一起去？"),
    "profile.q.TravelWith.hint" to listOf(
        "함께 즐기기 좋은 곳으로 골라 드려요",
        "We'll pick places you can enjoy together",
        "一緒に楽しめる場所を選びます",
        "我们会挑选适合一起玩的地方",
    ),
    "profile.q.Mood.title" to listOf("어떤 분위기가 좋아요?", "What vibe do you like?", "どんな雰囲気が好きですか？", "你喜欢什么氛围？"),
    "profile.q.Mood.hint" to listOf("여러 개 고를 수 있어요", "You can pick more than one", "複数選べます", "可以多选"),
    "profile.q.Activities.title" to listOf(
        "꼭 해보고 싶은 게 있나요?",
        "Anything you really want to do?",
        "ぜひやってみたいことはありますか？",
        "有特别想做的事吗？",
    ),
    "profile.q.Activities.hint" to listOf(
        "매일 일정에 하나씩은 넣어 드려요",
        "We'll fit at least one into each day",
        "毎日の日程に一つは入れます",
        "每天的行程至少安排一项",
    ),
    "profile.q.Pace.title" to listOf(
        "여행 페이스는 어떻게 할까요?",
        "What pace do you prefer?",
        "旅のペースはどうしますか？",
        "你想要什么样的旅行节奏？",
    ),
    "profile.moodPlaceholder" to listOf(
        "원하는 분위기를 입력하세요",
        "Describe the vibe you want",
        "希望する雰囲気を入力してください",
        "请输入想要的氛围",
    ),
    "profile.activitiesPlaceholder" to listOf(
        "해보고 싶은 것을 입력하세요",
        "Enter something you want to do",
        "やってみたいことを入力してください",
        "请输入想做的事",
    ),
    "profile.q.Purpose.hint" to listOf("여러 개 고를 수 있어요", "You can pick more than one", "複数選べます", "可以多选"),
    "profile.q.Food.title" to listOf("어떤 음식을 좋아하세요?", "What food do you like?", "どんな料理が好きですか？", "你喜欢什么菜？"),
    "profile.q.Food.hint" to listOf("여러 개 고를 수 있어요", "You can pick more than one", "複数選べます", "可以多选"),
    "profile.q.MustVisit.title" to listOf(
        "꼭 가고 싶은 곳이 있나요?",
        "Any place you must visit?",
        "絶対に行きたい場所はありますか？",
        "有一定要去的地方吗？",
    ),
    "profile.q.MustVisit.hint" to listOf(
        "장소 이름을 적으면 일정에 꼭 넣어 드려요 (최대 5곳)",
        "Enter a place name and we'll make sure it's in your plan (up to 5)",
        "場所の名前を入れると必ず日程に入れます（最大5か所）",
        "输入地点名称，我们一定会把它排进行程（最多 5 个）",
    ),
    "profile.mustVisitPlaceholder" to listOf("예: 경복궁, 광장시장", "e.g. Gyeongbokgung, Gwangjang Market", "例：景福宮、広蔵市場", "例如：景福宫、广藏市场"),
    "profile.mustVisitMax" to listOf(
        "최대 {0}곳까지 넣을 수 있어요",
        "You can add up to {0} places",
        "最大{0}か所まで追加できます",
        "最多可以添加 {0} 个地点",
    ),
    "profile.q.Places.title" to listOf(
        "어떤 곳에 가고 싶으세요?",
        "Where would you like to go?",
        "どんな場所に行きたいですか？",
        "你想去什么样的地方？",
    ),
    "profile.q.Places.hint" to listOf("여러 개 고를 수 있어요", "You can pick more than one", "複数選べます", "可以多选"),
    "profile.q.Walking.title" to listOf(
        "많이 걷는 건 괜찮으세요?",
        "Are you okay with lots of walking?",
        "たくさん歩いても大丈夫ですか？",
        "可以接受多走路吗？",
    ),
    "profile.q.Note.title" to listOf(
        "더 알려주실 취향이 있나요?",
        "Anything else we should know?",
        "ほかに好みはありますか？",
        "还有其他偏好吗？",
    ),
    "profile.q.Note.hint" to listOf(
        "AI가 장소를 고를 때 참고해요",
        "AI uses this when choosing places",
        "AIが場所を選ぶときの参考にします",
        "AI 挑选地点时会参考",
    ),

    // ---- 취향 질문 화면 (ProfileScreen) ----
    "profile.purposePlaceholder" to listOf(
        "원하는 여행 유형을 입력하세요",
        "Enter the kind of trip you want",
        "希望する旅行タイプを入力してください",
        "请输入想要的旅行类型",
    ),
    "profile.foodPlaceholder" to listOf(
        "좋아하는 음식을 입력하세요",
        "Enter a food you like",
        "好きな料理を入力してください",
        "请输入喜欢的食物",
    ),
    "profile.placesPlaceholder" to listOf(
        "가고 싶은 곳을 입력하세요",
        "Enter a place you'd like to go",
        "行きたい場所を入力してください",
        "请输入想去的地方",
    ),
    "profile.notePlaceholder" to listOf(
        "예: 매운 음식 좋아요, 조용한 카페 위주로",
        "e.g. I love spicy food, mostly quiet cafés",
        "例：辛い料理が好き、静かなカフェ中心で",
        "例：喜欢辣的食物，以安静的咖啡馆为主",
    ),
    "profile.add" to listOf("추가", "Add", "追加", "添加"),
    "profile.toSetup" to listOf("설정", "Setup", "設定", "设置"),
    "profile.makePlan" to listOf("플랜 만들기", "Create plan", "プランを作成", "生成行程"),

    // ---- 플랜 생성 중 (GeneratingScreen) ----
    "gen.title" to listOf("AI가 계획을 세우는 중", "AI is planning your trip", "AIがプランを作成中", "AI 正在规划行程"),
    "gen.msg.budget" to listOf("예산을 배분하고 있어요", "Allocating your budget", "予算を配分しています", "正在分配预算"),
    "gen.msg.places" to listOf(
        "취향에 맞는 장소를 고르는 중이에요",
        "Picking places that match your taste",
        "好みに合う場所を選んでいます",
        "正在挑选符合你喜好的地点",
    ),
    "gen.msg.combo" to listOf(
        "예산을 알뜰하게 쓰는 조합을 찾고 있어요",
        "Finding the most budget-friendly combination",
        "予算を賢く使える組み合わせを探しています",
        "正在寻找最省钱的组合",
    ),
    "gen.msg.routes" to listOf(
        "버스 노선과 도보 경로를 살피는 중이에요",
        "Checking bus routes and walking paths",
        "バス路線と徒歩ルートを確認しています",
        "正在查看公交线路和步行路线",
    ),
    "gen.msg.hide" to listOf("경로를 숨기는 중", "Hiding the route", "ルートを隠しています", "正在隐藏路线"),

    // ---- AppState 안내 문구 ----
    "state.walletLoadFailed" to listOf(
        "서버에 연결할 수 없어요 — 잠시 후 다시 시도해 주세요.",
        "Can't connect to the server — please try again shortly.",
        "サーバーに接続できません — しばらくしてから再度お試しください。",
        "无法连接服务器 — 请稍后再试。",
    ),
    "state.storeLoadFailed" to listOf(
        "상품을 불러오지 못했어요 — 서버 연결을 확인해 주세요.",
        "Couldn't load products — please check your server connection.",
        "商品を読み込めませんでした — サーバー接続を確認してください。",
        "无法加载商品 — 请检查服务器连接。",
    ),
    "state.purchaseFailed" to listOf(
        "구매에 실패했어요 — 서버 연결을 확인해 주세요.",
        "Purchase failed — please check your server connection.",
        "購入に失敗しました — サーバー接続を確認してください。",
        "购买失败 — 请检查服务器连接。",
    ),
    "state.serverUnreachable" to listOf(
        "서버에 연결할 수 없습니다",
        "Can't reach the server",
        "サーバーに接続できません",
        "无法连接到服务器",
    ),
)
