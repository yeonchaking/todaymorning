package com.yeon.todaymorning.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Material3 ColorScheme이 제공하지 않는 디자인 시스템 색(라일락·flame·surface 단계 등)을
 * 담는 확장 팔레트. Claude Design 시안의 t-light / t-dark 토큰을 그대로 옮긴 것.
 *
 * 사용: `val c = LocalAppColors.current` → `c.lilac` 등.
 */
data class AppColors(
    val appBg: Color,
    val surface: Color,
    val surface2: Color,
    val surface3: Color,
    val on: Color,
    val onVar: Color,
    val outline: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryCtr: Color,
    val onPrimaryCtr: Color,
    val lilac: Color,
    val onLilac: Color,
    val lilacStrong: Color,
    val success: Color,
    val onSuccess: Color,
    val successCtr: Color,
    val danger: Color,
    val dangerCtr: Color,
    val amber: Color,
    val amberCtr: Color,
    val flame: Color,
    val header: Color,
    val onHeader: Color,
    /** 헤더 그라디언트(홈 히어로) 3색. */
    val headerGradient: List<Color>,
    val isDark: Boolean,
)

// ── t-light ───────────────────────────────────────────────────────────────
val LightAppColors = AppColors(
    appBg = Color(0xFFF3F6FE),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFEAF0FB),
    surface3 = Color(0xFFDEE7F7),
    on = Color(0xFF191C20),
    onVar = Color(0xFF5A5E67),
    outline = Color(0xFFD5DBE6),
    primary = Color(0xFF1A5FD9),
    onPrimary = Color(0xFFFFFFFF),
    primaryCtr = Color(0xFFDAE2FF),
    onPrimaryCtr = Color(0xFF16306B),
    lilac = Color(0xFFE8E0F8),
    onLilac = Color(0xFF473A82),
    lilacStrong = Color(0xFF7C5CD6),
    success = Color(0xFF2E7D32),
    onSuccess = Color(0xFFFFFFFF),
    successCtr = Color(0xFFCDEFCF),
    danger = Color(0xFFD92D20),
    dangerCtr = Color(0xFFFCDAD7),
    amber = Color(0xFFD9870A),
    amberCtr = Color(0xFFFCE9C4),
    flame = Color(0xFFFF7A1A),
    header = Color(0xFF1A5FD9),
    onHeader = Color(0xFFFFFFFF),
    headerGradient = listOf(Color(0xFF2A6CF0), Color(0xFF1A4FCB), Color(0xFF5A3FD0)),
    isDark = false,
)

// ── t-dark ────────────────────────────────────────────────────────────────
val DarkAppColors = AppColors(
    appBg = Color(0xFF0E1116),
    surface = Color(0xFF171B21),
    surface2 = Color(0xFF1F242C),
    surface3 = Color(0xFF2A313A),
    on = Color(0xFFE4E6EB),
    onVar = Color(0xFFA7ACB6),
    outline = Color(0xFF333A44),
    primary = Color(0xFFA8C8FF),
    onPrimary = Color(0xFF06305F),
    primaryCtr = Color(0xFF274C88),
    onPrimaryCtr = Color(0xFFD8E2FF),
    lilac = Color(0xFF352C50),
    onLilac = Color(0xFFDCD0F8),
    lilacStrong = Color(0xFFB9A2F0),
    success = Color(0xFF7FD488),
    onSuccess = Color(0xFF06310C),
    successCtr = Color(0xFF1E3D23),
    danger = Color(0xFFFF8077),
    dangerCtr = Color(0xFF4A1F1B),
    amber = Color(0xFFF0B84A),
    amberCtr = Color(0xFF3E2E12),
    flame = Color(0xFFFF9347),
    header = Color(0xFF14233E),
    onHeader = Color(0xFFDDE7FF),
    // 다크에서도 히어로 헤더는 채도 있는 그라디언트 유지(시안 라이트와 동일 계열, 약간 어둡게)
    headerGradient = listOf(Color(0xFF234FB0), Color(0xFF173B97), Color(0xFF3D2C9C)),
    isDark = true,
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

/** 짧은 접근자: `AppTheme.colors.lilac` */
object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}
