// OGQ Design System — Kotlin Tokens (Jetpack Compose)
// Auto-generated from OGQ Design System v1.0.0
// Do not edit manually.

package com.ogqcorp.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle

// ── Colors ──

object OGQColors {
    // Mono
    val mono000 = Color(0xFFFFFFFF)
    val mono010 = Color(0xFFFCFDFD)
    val mono030 = Color(0xFFF9FBFB)
    val mono050 = Color(0xFFF4F6F6)
    val mono080 = Color(0xFFEEF1F1)
    val mono100 = Color(0xFFD8DFDF)
    val mono200 = Color(0xFFA7B6B9)
    val mono300 = Color(0xFF89989C)
    val mono400 = Color(0xFF7D8D92)
    val mono500 = Color(0xFF76888F)
    val mono600 = Color(0xFF6C7E84)
    val mono700 = Color(0xFF57676B)
    val mono800 = Color(0xFF425052)
    val mono900 = Color(0xFF262D2E)
    val mono990 = Color(0xFF262D2E)

    // Primary (Teal)
    val primary010 = Color(0xFFFBFDFC)
    val primary030 = Color(0xFFEEF7F4)
    val primary050 = Color(0xFFDFF4EA)
    val primary080 = Color(0xFFCBECDF)
    val primary100 = Color(0xFFB5E5D7)
    val primary200 = Color(0xFF9BDFCC)
    val primary300 = Color(0xFF7FDEC1)
    val primary400 = Color(0xFF58DAB1)
    val primary500 = Color(0xFF32D29D)
    val primary600 = Color(0xFF00C389)
    val primary700 = Color(0xFF00B57F)
    val primary800 = Color(0xFF00996E)
    val primary900 = Color(0xFF007A58)
    val primary990 = Color(0xFF006147)

    // Secondary (Purple)
    val secondary010 = Color(0xFFFBFBFE)
    val secondary030 = Color(0xFFF8F6FE)
    val secondary050 = Color(0xFFEBE8FC)
    val secondary080 = Color(0xFFE0DAFB)
    val secondary100 = Color(0xFFD2C8F9)
    val secondary200 = Color(0xFFBFB0F2)
    val secondary300 = Color(0xFFA793EC)
    val secondary400 = Color(0xFF947BE5)
    val secondary500 = Color(0xFF7E5AE2)
    val secondary600 = Color(0xFF703FE4)
    val secondary700 = Color(0xFF5B1DCD)
    val secondary800 = Color(0xFF4917A6)
    val secondary900 = Color(0xFF37117E)
    val secondary990 = Color(0xFF290D5E)

    // Semantic
    val successLight = Color(0xFFDFF4EA)
    val success = Color(0xFF00C389)
    val successDark = Color(0xFF007A58)
    val warningLight = Color(0xFFFFF3E0)
    val warning = Color(0xFFFF9800)
    val warningDark = Color(0xFFE65100)
    val errorLight = Color(0xFFFFE2E4)
    val error = Color(0xFFE21235)
    val errorDark = Color(0xFFC90E2E)
    val infoLight = Color(0xFFE6F4FF)
    val info = Color(0xFF1384D7)
    val infoDark = Color(0xFF0D5FA3)

    // Accent
    val green = Color(0xFF00FF85)
    val orange = Color(0xFFFF550D)
    val purple = Color(0xFF7F00FF)

    // Social
    val naver = Color(0xFF03C75A)
    val facebook = Color(0xFF3D5A98)
    val afreeca = Color(0xFF0545B1)
    val google = Color(0xFF4285F4)
    val apple = Color(0xFF000000)
    val kakao = Color(0xFFFEE500)
}

// ── Typography ──

object OGQTypography {
    // Enhanced Scale
    val displayXL = TextStyle(fontSize = 56.sp, lineHeight = 61.6.sp, letterSpacing = (-1.4).sp, fontWeight = FontWeight.Bold)
    val displayLG = TextStyle(fontSize = 48.sp, lineHeight = 55.2.sp, letterSpacing = (-0.96).sp, fontWeight = FontWeight.Bold)
    val displayMD = TextStyle(fontSize = 36.sp, lineHeight = 43.2.sp, letterSpacing = (-0.54).sp, fontWeight = FontWeight.SemiBold)
    val headingXL = TextStyle(fontSize = 30.sp, lineHeight = 37.5.sp, letterSpacing = (-0.3).sp, fontWeight = FontWeight.SemiBold)
    val headingLG = TextStyle(fontSize = 24.sp, lineHeight = 31.2.sp, letterSpacing = (-0.12).sp, fontWeight = FontWeight.SemiBold)
    val headingMD = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
    val headingSM = TextStyle(fontSize = 18.sp, lineHeight = 25.2.sp, fontWeight = FontWeight.Medium)
    val bodyLG = TextStyle(fontSize = 18.sp, lineHeight = 28.8.sp, fontWeight = FontWeight.Normal)
    val bodyMD = TextStyle(fontSize = 16.sp, lineHeight = 25.6.sp, fontWeight = FontWeight.Normal)
    val bodySM = TextStyle(fontSize = 14.sp, lineHeight = 21.sp, letterSpacing = 0.07.sp, fontWeight = FontWeight.Normal)
    val caption = TextStyle(fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 0.12.sp, fontWeight = FontWeight.Normal)
    val overline = TextStyle(fontSize = 11.sp, lineHeight = 16.5.sp, letterSpacing = 0.88.sp, fontWeight = FontWeight.SemiBold)

    // OGQ Original Mixin Scales (single-line)
    val h1Bold = TextStyle(fontSize = 56.sp, lineHeight = 56.sp, fontWeight = FontWeight.Bold)
    val h2Bold = TextStyle(fontSize = 48.sp, lineHeight = 48.sp, fontWeight = FontWeight.Bold)
    val h3Bold = TextStyle(fontSize = 44.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold)
    val h4Bold = TextStyle(fontSize = 40.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold)
    val h5Bold = TextStyle(fontSize = 36.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
    val h6Bold = TextStyle(fontSize = 32.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
    val h7Bold = TextStyle(fontSize = 28.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
    val h8Bold = TextStyle(fontSize = 24.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
    val b1Regular = TextStyle(fontSize = 20.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal)
    val b2Regular = TextStyle(fontSize = 18.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal)
    val b3Regular = TextStyle(fontSize = 16.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal)
    val b4Regular = TextStyle(fontSize = 14.sp, lineHeight = 14.sp, fontWeight = FontWeight.Normal)
    val b5Regular = TextStyle(fontSize = 12.sp, lineHeight = 12.sp, fontWeight = FontWeight.Normal)
    val c1Regular = TextStyle(fontSize = 11.sp, lineHeight = 12.sp, fontWeight = FontWeight.Normal)
    val c2Regular = TextStyle(fontSize = 10.sp, lineHeight = 10.sp, fontWeight = FontWeight.Normal)
}

// ── Spacing ──

object OGQSpacing {
    val s0 = 0.dp
    val s2 = 2.dp
    val s4 = 4.dp
    val s6 = 6.dp
    val s8 = 8.dp
    val s10 = 10.dp
    val s12 = 12.dp
    val s16 = 16.dp
    val s20 = 20.dp
    val s24 = 24.dp
    val s28 = 28.dp
    val s32 = 32.dp
    val s40 = 40.dp
    val s48 = 48.dp
    val s64 = 64.dp
    val s80 = 80.dp
    val s120 = 120.dp
}

// ── Border Radius ──

object OGQRadius {
    val none = 0.dp
    val sm = 4.dp
    val r6 = 6.dp
    val md = 8.dp
    val lg = 12.dp
    val xl = 16.dp
    val xxl = 20.dp
    val full = 1000.dp
}

// ── Motion ──

object OGQMotion {
    const val instant = 50
    const val fast = 100
    const val normal = 200
    const val moderate = 300
    const val slow = 400
    const val slower = 500
    const val slowest = 700
}
