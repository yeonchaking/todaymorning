package com.yeon.todaymorning.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Material3 ColorScheme이 제공하지 않는 디자인 시스템 색(신호등 success/amber/danger,
 * surface 단계, outline-soft, track-off 등)을 담는 확장 팔레트.
 * Claude Design v2 시안의 t-light / t-dark 토큰을 그대로 옮긴 것.
 *
 * 사용: `val c = AppTheme.colors` → `c.success` 등.
 */
data class AppColors(
    val appBg: Color,
    val surface: Color,
    val surface2: Color,
    val surface3: Color,
    val on: Color,
    val onVar: Color,
    val outline: Color,
    val outlineSoft: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryCtr: Color,
    val onPrimaryCtr: Color,
    val success: Color,
    val successCtr: Color,
    val onSuccessCtr: Color,
    val amber: Color,
    val amberCtr: Color,
    val onAmberCtr: Color,
    val danger: Color,
    val dangerCtr: Color,
    val onDangerCtr: Color,
    val trackOff: Color,
    val isDark: Boolean,
)

// ── t-light ───────────────────────────────────────────────────────────────
val LightAppColors = AppColors(
    appBg = Color(0xFFF4F7FC),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFEDF2FB),
    surface3 = Color(0xFFE1E9F6),
    on = Color(0xFF1A1C20),
    onVar = Color(0xFF565B66),
    outline = Color(0xFFD9E0EC),
    outlineSoft = Color(0xFFEAEFF6),
    primary = Color(0xFF1A5FD9),
    onPrimary = Color(0xFFFFFFFF),
    primaryCtr = Color(0xFFDCE6FF),
    onPrimaryCtr = Color(0xFF15336E),
    success = Color(0xFF1E7D34),
    successCtr = Color(0xFFCFEFD5),
    onSuccessCtr = Color(0xFF0A3315),
    amber = Color(0xFFB5740A),
    amberCtr = Color(0xFFFBE6BF),
    onAmberCtr = Color(0xFF3D2606),
    danger = Color(0xFFC5291C),
    dangerCtr = Color(0xFFFBDAD6),
    onDangerCtr = Color(0xFF410B06),
    trackOff = Color(0xFFC3CAD6),
    isDark = false,
)

// ── t-dark ────────────────────────────────────────────────────────────────
val DarkAppColors = AppColors(
    appBg = Color(0xFF0F1318),
    surface = Color(0xFF181D24),
    surface2 = Color(0xFF202730),
    surface3 = Color(0xFF2A323D),
    on = Color(0xFFE3E6EB),
    onVar = Color(0xFFA4AAB5),
    outline = Color(0xFF323A45),
    outlineSoft = Color(0xFF242B34),
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF06305F),
    primaryCtr = Color(0xFF274C88),
    onPrimaryCtr = Color(0xFFD8E2FF),
    success = Color(0xFF7FD48C),
    successCtr = Color(0xFF1C3D24),
    onSuccessCtr = Color(0xFFC7F0CC),
    amber = Color(0xFFF0B84A),
    amberCtr = Color(0xFF3C2C12),
    onAmberCtr = Color(0xFFFBE6BF),
    danger = Color(0xFFFF8A7E),
    dangerCtr = Color(0xFF48201B),
    onDangerCtr = Color(0xFFFBD9D4),
    trackOff = Color(0xFF3A424E),
    isDark = true,
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

/** 짧은 접근자: `AppTheme.colors.success` */
object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}
