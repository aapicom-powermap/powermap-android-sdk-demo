package com.powermap.demo.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val PrimaryColor = Color(0xFFFF5722) // Deep Orange 500 (Matches Flutter)
val SuccessColor = Color(0xFF10B981)
val ErrorColor = Color(0xFFEF4444)
val SurfaceLight = Color.White.copy(alpha = 0.9f)
val SurfaceDark = Color(0xFF1E293B).copy(alpha = 0.9f)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    secondary = Color(0xFFFF9800), // Orange 500
    tertiary = ErrorColor,
    background = Color(0xFFF8FAFC),
    surface = SurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1E293B),
    onSurface = Color(0xFF1E293B),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF9800), // Orange for dark mode
    secondary = SuccessColor,
    tertiary = ErrorColor,
    background = Color(0xFF0F172A),
    surface = SurfaceDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
)

// Typography (Fallback to default Roboto if custom fonts aren't available immediately)
val AppTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

val CardShape = RoundedCornerShape(24.dp)
val ButtonShape = RoundedCornerShape(16.dp)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
