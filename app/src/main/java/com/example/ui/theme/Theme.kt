package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CarLensDarkColorScheme = darkColorScheme(
    primary = ElectricBluePrimary,
    onPrimary = Color(0xFF003554),
    primaryContainer = ElectricBlueContainer,
    onPrimaryContainer = OnElectricBlueContainer,
    secondary = CyberAmber,
    onSecondary = Color(0xFF451A03),
    secondaryContainer = AmberContainer,
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = EmeraldValuation,
    onTertiary = Color(0xFF022C22),
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = OnEmerald,
    background = CarbonDark,
    onBackground = TextPrimary,
    surface = CarbonCard,
    onSurface = TextPrimary,
    surfaceVariant = CarbonCardVariant,
    onSurfaceVariant = TextSecondary,
    outline = CarbonBorder,
    error = RacingRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CarLensDarkColorScheme,
        typography = Typography,
        content = content
    )
}
