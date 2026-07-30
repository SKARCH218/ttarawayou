package com.trevit.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.trevit.app.R
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
 * 토스페이스 이모지 폰트 — fallback 1순위.
 * 이모지 전용이라 한글/숫자는 시스템 폰트로 자동 폴백된다.
 */
val TossFaceFontFamily = FontFamily(Font(R.font.tossface))

private val AppTypography: Typography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = TossFaceFontFamily),
        displayMedium = base.displayMedium.copy(fontFamily = TossFaceFontFamily),
        displaySmall = base.displaySmall.copy(fontFamily = TossFaceFontFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = TossFaceFontFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = TossFaceFontFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = TossFaceFontFamily),
        titleLarge = base.titleLarge.copy(fontFamily = TossFaceFontFamily),
        titleMedium = base.titleMedium.copy(fontFamily = TossFaceFontFamily),
        titleSmall = base.titleSmall.copy(fontFamily = TossFaceFontFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = TossFaceFontFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = TossFaceFontFamily),
        bodySmall = base.bodySmall.copy(fontFamily = TossFaceFontFamily),
        labelLarge = base.labelLarge.copy(fontFamily = TossFaceFontFamily),
        labelMedium = base.labelMedium.copy(fontFamily = TossFaceFontFamily),
        labelSmall = base.labelSmall.copy(fontFamily = TossFaceFontFamily),
    )
}

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
