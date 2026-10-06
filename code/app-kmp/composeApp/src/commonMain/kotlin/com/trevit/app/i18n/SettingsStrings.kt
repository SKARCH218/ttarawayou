package com.trevit.app.i18n

/** 설정 화면 번역. listOf(한국어, English, 日本語, 中文) */
internal val settingsStrings: Map<String, List<String>> = mapOf(
    "settings.title" to listOf("설정", "Settings", "設定", "设置"),
    "settings.back" to listOf("뒤로", "Back", "戻る", "返回"),

    // ---- 섹션 ----
    "settings.section.account" to listOf("계정", "Account", "アカウント", "账户"),
    "settings.section.app" to listOf("앱 설정", "App settings", "アプリ設定", "应用设置"),
    "settings.section.info" to listOf("정보", "About", "情報", "关于"),
    "settings.section.developer" to listOf("개발자 옵션", "Developer options", "開発者オプション", "开发者选项"),

    // ---- 프로필 카드 ----
    "settings.googleAccount" to listOf("구글 계정", "Google account", "Googleアカウント", "Google 账户"),
    "settings.emailAccount" to listOf("이메일 계정", "Email account", "メールアカウント", "邮箱账户"),
    "settings.notLoaded" to listOf(
        "계정 정보를 불러오지 못했어요",
        "Couldn't load your account",
        "アカウント情報を読み込めませんでした",
        "无法加载账户信息",
    ),

    // ---- 계정 항목 ----
    "settings.nickname" to listOf("닉네임 변경", "Change nickname", "ニックネームを変更", "修改昵称"),
    "settings.password" to listOf("비밀번호 변경", "Change password", "パスワードを変更", "修改密码"),
    "settings.password.googleNote" to listOf(
        "구글로 가입한 계정은 비밀번호가 없어요",
        "Google accounts don't have a password",
        "Googleで登録したアカウントにはパスワードがありません",
        "通过 Google 注册的账户没有密码",
    ),
    "settings.logout" to listOf("로그아웃", "Log out", "ログアウト", "退出登录"),
    "settings.withdraw" to listOf("회원 탈퇴", "Delete account", "退会する", "注销账户"),

    // ---- 앱 설정 ----
    "settings.language" to listOf("언어", "Language", "言語", "语言"),
    "settings.theme" to listOf("화면 테마", "Theme", "テーマ", "主题"),
    "settings.theme.system" to listOf("시스템 설정 따르기", "Use system setting", "システム設定に従う", "跟随系统"),
    "settings.theme.light" to listOf("라이트", "Light", "ライト", "浅色"),
    "settings.theme.dark" to listOf("다크", "Dark", "ダーク", "深色"),

    // ---- 정보 ----
    "settings.version" to listOf("앱 버전", "App version", "アプリのバージョン", "应用版本"),

    // ---- 개발자 옵션 ----
    "settings.server" to listOf("서버 주소", "Server address", "サーバーアドレス", "服务器地址"),
    "settings.server.hint" to listOf(
        "개발·테스트용이에요. 잘못 바꾸면 앱이 서버에 연결되지 않아요.",
        "For development and testing. A wrong address will disconnect the app from the server.",
        "開発・テスト用です。誤った値にするとサーバーに接続できなくなります。",
        "仅供开发和测试使用。地址错误会导致应用无法连接服务器。",
    ),

    // ---- 다이얼로그 ----
    "settings.nickname.label" to listOf("새 닉네임", "New nickname", "新しいニックネーム", "新昵称"),
    "settings.nickname.hint" to listOf("2~12자", "2–12 characters", "2〜12文字", "2~12 个字符"),
    "settings.password.current" to listOf("현재 비밀번호", "Current password", "現在のパスワード", "当前密码"),
    "settings.password.new" to listOf("새 비밀번호", "New password", "新しいパスワード", "新密码"),
    "settings.password.confirm" to listOf("새 비밀번호 확인", "Confirm new password", "新しいパスワード（確認）", "确认新密码"),
    "settings.password.rule" to listOf(
        "영문과 숫자를 섞어 8자 이상",
        "At least 8 characters with letters and numbers",
        "英字と数字を含む8文字以上",
        "至少 8 位，包含字母和数字",
    ),
    "settings.logout.confirm" to listOf("로그아웃할까요?", "Log out now?", "ログアウトしますか？", "确定要退出登录吗？"),
    "settings.withdraw.warning" to listOf(
        "탈퇴하면 계정과 로그인 정보가 모두 삭제되고 되돌릴 수 없어요.",
        "Deleting your account removes your account and sign-in data permanently. This can't be undone.",
        "退会するとアカウントとログイン情報がすべて削除され、元に戻せません。",
        "注销后，账户和登录信息将被永久删除且无法恢复。",
    ),
    "settings.withdraw.passwordLabel" to listOf("비밀번호 확인", "Confirm password", "パスワードを確認", "确认密码"),
    "settings.withdraw.confirm" to listOf("탈퇴하기", "Delete account", "退会する", "确认注销"),
    "settings.processing" to listOf("처리 중…", "Working…", "処理中…", "处理中…"),

    // ---- 완료 안내 ----
    "settings.done.nickname" to listOf("닉네임을 바꿨어요", "Nickname changed", "ニックネームを変更しました", "昵称已修改"),
    "settings.done.password" to listOf("비밀번호를 바꿨어요", "Password changed", "パスワードを変更しました", "密码已修改"),
    "settings.done.server" to listOf("서버 주소를 저장했어요", "Server address saved", "サーバーアドレスを保存しました", "服务器地址已保存"),

    // ---- 오류 (AuthState 계정 설정) ----
    "settings.err.loginRequired" to listOf("로그인이 필요해요.", "Please log in.", "ログインが必要です。", "请先登录。"),
    "settings.err.currentPasswordEmpty" to listOf(
        "현재 비밀번호를 입력해 주세요.",
        "Enter your current password.",
        "現在のパスワードを入力してください。",
        "请输入当前密码。",
    ),
    "settings.err.passwordWeak" to listOf(
        "비밀번호는 영문과 숫자를 섞어 8자 이상이어야 해요.",
        "Password must be at least 8 characters with letters and numbers.",
        "パスワードは英字と数字を含む8文字以上にしてください。",
        "密码至少 8 位，并且需要同时包含字母和数字。",
    ),
    "settings.err.passwordMismatch" to listOf(
        "새 비밀번호가 서로 달라요.",
        "The new passwords don't match.",
        "新しいパスワードが一致しません。",
        "两次输入的新密码不一致。",
    ),
    "settings.err.passwordSame" to listOf(
        "지금 쓰는 비밀번호와 달라야 해요.",
        "Choose a password different from your current one.",
        "現在とは異なるパスワードにしてください。",
        "新密码不能与当前密码相同。",
    ),
    "settings.err.nicknameLength" to listOf(
        "닉네임은 2~12자로 입력해 주세요.",
        "Nickname must be 2–12 characters.",
        "ニックネームは2〜12文字で入力してください。",
        "昵称需为 2~12 个字符。",
    ),
    "settings.err.network" to listOf(
        "요청을 처리하지 못했어요 — 서버 연결을 확인해 주세요.",
        "Couldn't complete the request — check your connection to the server.",
        "リクエストを処理できませんでした。サーバーへの接続を確認してください。",
        "请求失败，请检查与服务器的连接。",
    ),
)
