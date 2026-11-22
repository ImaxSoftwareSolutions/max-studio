package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    DARK("Dark Mode"),
    LIGHT("Light Mode"),
}

private val MaxStudioDarkScheme = darkColorScheme(
    primary = MaxBlueBright,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = CyanElectric,
    onSecondary = Color(0xFF0B1120),
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = EmeraldRun,
    onTertiary = Color(0xFF064E3B),
    tertiaryContainer = Color(0xFF064E3B),
    onTertiaryContainer = Color(0xFFD1FAE5),
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainerLowest = DarkBg,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkCard,
    surfaceContainerHigh = DarkElevated,
    surfaceContainerHighest = Color(0xFF333A46),
    outline = DarkBorder,
    outlineVariant = Color(0xFF252B34),
    error = Color(0xFFFB7185),
    onError = Color(0xFF4C0519),
)

private val MaxStudioLightScheme = lightColorScheme(
    primary = MaxBlueDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF0891B2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF164E63),
    tertiary = EmeraldRun,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF064E3B),
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCardVariant,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = LightCard,
    surfaceContainerHigh = Color(0xFFF1F5F9),
    surfaceContainerHighest = LightCardVariant,
    outline = LightBorder,
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFE11D48),
    onError = Color.White,
)

private val MaxStudioShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun MaxStudioTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val colorScheme = if (useDark) MaxStudioDarkScheme else MaxStudioLightScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DroidBlocksTypography,
        shapes = MaxStudioShapes,
        content = content,
    )
}
