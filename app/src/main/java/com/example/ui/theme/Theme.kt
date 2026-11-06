package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ObsidianDarkColorScheme = darkColorScheme(
    primary = CrimsonRed,
    onPrimary = Color.White,
    primaryContainer = CrimsonContainer,
    onPrimaryContainer = OnCrimsonContainer,
    secondary = CrimsonRedLight,
    onSecondary = Color.White,
    secondaryContainer = CarbonSurfaceHigh,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = CyanStream,
    onTertiary = ObsidianBlack,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = EmeraldSuccess,
    background = ObsidianBlack,
    onBackground = TextPrimaryDark,
    surface = CarbonSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = CarbonSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = CarbonBorder,
    outlineVariant = CarbonSurfaceHigh,
    error = Color(0xFFFF5252),
    onError = Color.White
)

private val StudioLightColorScheme = lightColorScheme(
    primary = CrimsonRedDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDADE),
    onPrimaryContainer = Color(0xFF40000A),
    secondary = CrimsonRed,
    onSecondary = Color.White,
    secondaryContainer = SoftGraySurface,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = Color(0xFF00796B),
    background = AlabasterBg,
    onBackground = TextPrimaryLight,
    surface = PureWhiteSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = SoftGraySurface,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFD0D4E0)
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ObsidianDarkColorScheme else StudioLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
