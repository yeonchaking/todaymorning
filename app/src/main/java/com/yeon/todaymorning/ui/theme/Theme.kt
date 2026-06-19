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
    errorContainer = Color