package com.trevit.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.compose.resources.Font
import com.trevit.app.resources.*
import com.ogqcorp.designsystem.OGQColors

/** OGQ 디자인 시스템 색 토큰 기반 Material3 스킴 (Primary=Teal, Secondary=Purple) */
private val LightColors = lightColorScheme(
    primary = OGQColors.primary600,
    onPrimary = OGQColors.mono000,
    primaryContainer = OGQColors.primary050,
    onPrimaryContainer = OGQColors.primary990,
    secondary = OGQColors.secondary600,
    onSecondary = OGQColors.mono000,
    secondaryContainer = OGQColors.secondary050,
    onSecondaryContainer = OGQColors.secondary990,
    tertiary = OGQColors.secondary500,
    onTertiary = OGQColors.mono000,
    tertiaryContainer = OGQColors.secondary080,
    onTertiaryContainer = OGQColors.secondary990,
    error = OGQColors.error,
    onError = OGQColors.mono000,
    errorContainer = OGQColors.errorLight,
    onErrorContainer = OGQColors.errorDark,
    background = OGQColors.mono010,
    onBackground = OGQColors.mono900,
    surface = OGQColors.mono000,
    onSurface = OGQColors.mono900,
    surfaceVariant = OGQColors.mono050,
    onSurfaceVariant = OGQColors.mono700,
    outline = OGQColors.mono300,
    outlineVariant = OGQColors.mono100,
)

private val DarkColors = darkColorScheme(
    primary = OGQColors.primary400,
    onPrimary = OGQColors.primary990,
    primaryContainer = OGQColors.primary900,
    onPrimaryContainer = OGQColors.primary050,
    secondary = OGQColors.secondary300,
    onSecondary = OGQColors.secondary990,
    secondaryContainer = OGQColors.secondary800,
    onSecondaryContainer = OGQColors.secondary050,
    tertiary = OGQColors.secondary200,
    onTertiary = OGQColors.secondary990,
    tertiaryContainer = OGQColors.secondary900,
    onTertiaryContainer = OGQColors.secondary080,
    error = Color(0xFFFF6B81),
    onError = OGQColors.mono990,
    background = Color(0xFF181D1E),
    onBackground = OGQColors.mono050,
    surface = Color(0xFF202627),
    onSurface = OGQColors.mono050,
    surfaceVariant = Color(0xFF2C3435),
    onSurfaceVariant = OGQColors.mono200,
    outline = OGQColors.mono400,
    outlineVariant = OGQColors.mono700,
)

/**
 * 토스페이스 이모지 폰트.
 *
 * 전체 Typography에 적용하면 안 된다 — 이 폰트는 숫자·문자 글리프의 자간이 넓어
 * "300,000원"이 "3 0 0 , 0 0 0 원"처럼 벌어져 보인다.
 * 이모지를 그리는 Text에만 `fontFamily = TossFaceFontFamily`로 직접 지정한다.
 */
val TossFaceFontFamily: FontFamily
    @Composable get() = FontFamily(Font(Res.font.tossface))

// 본문은 시스템 폰트(한글 최적화)를 그대로 쓴다
private val AppTypography: Typography = Typography()

/** 트레빗 브랜드 민트 — design-system/brand 로고와 동일한 값 */
val BrandMint = Color(0xFF03B083)

// Journey 지도/컨페티에서 쓰는 브랜드 단축 색
val MysteryPurple = OGQColors.secondary600
val MysteryPurpleLight = OGQColors.secondary300
val MintAccent = OGQColors.primary500
val SunsetOrange = OGQColors.orange

@Composable
fun TrevitTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
