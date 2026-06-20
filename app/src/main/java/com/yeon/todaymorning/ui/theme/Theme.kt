package com.yeon.todaymorning.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// Claude Design v2 시안(t-light/t-dark) 기반 M3 ColorScheme.
// 신호등(success/amber/danger)·surface 단계 등 시안 전용 색은 AppColors(LocalAppColors)로 제공.
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1A5FD9),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE6FF),
    onPrimaryContainer = Color(0xFF15336E),
    secondary = Color(0xFF1E7D34),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFEFD5),
    onSecondaryContainer = Color(0xFF0A3315),
    tertiary = Color(0xFFB5740A),
    tertiaryContainer = Color(0xFFFBE6BF),
    onTertiaryContainer = Color(0xFF3D2606),
    background = Color(0xFFF4F7FC),
    onBackground = Color(0xFF1A1C20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C20),
    surfaceVariant = Color(0xFFEDF2FB),
    onSurfaceVariant = Color(0xFF565B66),
    outline = Color(0xFFD9E0EC),
    error = Color(0xFFC5291C),
    errorContainer = Color(0xFFFBDAD6),
    onErrorContainer = Color(0xFF410B06),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF06305F),
    primaryContainer = Color(0xFF274C88),
    onPrimaryContainer = Color(0xFFD8E2FF),
    secondary = Color(0xFF7FD48C),
    onSecondary = Color(0xFF06310C),
    secondaryContainer = Color(0xFF1C3D24),
    onSecondaryContainer = Color(0xFFC7F0CC),
    tertiary = Color(0xFFF0B84A),
    tertiaryContainer = Color(0xFF3C2C12),
    onTertiaryContainer = Color(0xFFFBE6BF),
    background = Color(0xFF0F1318),
    onBackground = Color(0xFFE3E6EB),
    surface = Color(0xFF181D24),
    onSurface = Color(0xFFE3E6EB),
    surfaceVariant = Color(0xFF202730),
    onSurfaceVariant = Color(0xFFA4AAB5),
    outline = Color(0xFF323A45),
    error = Color(0xFFFF8A7E),
    errorContainer = Color(0xFF48201B),
    onErrorContainer = Color(0xFFFBD9D4),
)

@Composable
fun TodayCommuteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val appColors = if (darkTheme) DarkAppColors else LightAppColors

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TodayCommuteTypography,
            content = content
        )
    }
}
