package com.yeon.todaymorning.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// Claude Design 시안(t-light/t-dark) 기반 M3 ColorScheme.
// 라일락·flame 등 시안 전용 색은 AppColors(LocalAppColors)로 별도 제공.
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1A5FD9),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDAE2FF),
    onPrimaryContainer = Color(0xFF16306B),
    secondary = Color(0xFF7C5CD6),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8E0F8),
    onSecondaryContainer = Color(0xFF473A82),
    tertiary = Color(0xFFD9870A),
    background = Color(0xFFF3F6FE),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFEAF0FB),
    onSurfaceVariant = Color(0xFF5A5E67),
    outline = Color(0xFFD5DBE6),
    error = Color(0xFFD92D20),
    errorContainer = Color(0xFFFCDAD7),
    onErrorContainer = Color(0xFF410E0B),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA8C8FF),
    onPrimary = Color(0xFF06305F),
    primaryContainer = Color(0xFF274C88),
    onPrimaryContainer = Color(0xFFD8E2FF),
    secondary = Color(0xFFB9A2F0),
    onSecondary = Color(0xFF2A1E55),
    secondaryContainer = Color(0xFF352C50),
    onSecondaryContainer = Color(0xFFDCD0F8),
    tertiary = Color(0xFFF0B84A),
    background = Color(0xFF0E1116),
    onBackground = Color(0xFFE4E6EB),
    surface = Color(0xFF171B21),
    onSurface = Color(0xFFE4E6EB),
    surfaceVariant = Color(0xFF1F242C),
    onSurfaceVariant = Color(0xFFA7ACB6),
    outline = Color(0xFF333A44),
    error = Color(0xFFFF8077),
    errorContainer = Color(0xFF4A1F1B),
    onErrorContainer = Color(0xFFFFDAD6),
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
