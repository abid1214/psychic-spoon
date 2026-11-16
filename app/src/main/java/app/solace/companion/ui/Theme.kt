package app.solace.companion.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF3B4A8F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Color(0xFF0E1B55),
    secondary = Color(0xFF5B5D72),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E6F2),
    onSecondaryContainer = Color(0xFF181A2C),
    tertiary = Color(0xFF9A4A1F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDCC7),
    onTertiaryContainer = Color(0xFF351000),
    background = Color(0xFFFBF8F4),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFBF8F4),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE9E4DC),
    onSurfaceVariant = Color(0xFF4A4740),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF6F2EC),
    surfaceContainer = Color(0xFFF1EDE6),
    surfaceContainerHigh = Color(0xFFEBE7E0),
    surfaceContainerHighest = Color(0xFFE5E1DA),
    outline = Color(0xFF7B776F),
    outlineVariant = Color(0xFFD3CEC5),
    error = Color(0xFFB3261E),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFB9C3FF),
    onPrimary = Color(0xFF1C2A6B),
    primaryContainer = Color(0xFF2F3D82),
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Color(0xFFC4C5DD),
    onSecondary = Color(0xFF2D2F42),
    secondaryContainer = Color(0xFF33354A),
    onSecondaryContainer = Color(0xFFE0E1F9),
    tertiary = Color(0xFFFFB68A),
    onTertiary = Color(0xFF552100),
    tertiaryContainer = Color(0xFF6E3410),
    onTertiaryContainer = Color(0xFFFFDCC7),
    background = Color(0xFF14151B),
    onBackground = Color(0xFFE5E2E6),
    surface = Color(0xFF14151B),
    onSurface = Color(0xFFE5E2E6),
    surfaceVariant = Color(0xFF45464F),
    onSurfaceVariant = Color(0xFFC6C5D0),
    surfaceContainerLowest = Color(0xFF0F1015),
    surfaceContainerLow = Color(0xFF1B1C23),
    surfaceContainer = Color(0xFF1F2028),
    surfaceContainerHigh = Color(0xFF292A33),
    surfaceContainerHighest = Color(0xFF34353E),
    outline = Color(0xFF90909A),
    outlineVariant = Color(0xFF45464F),
)

@Composable
fun SolaceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        content = content,
    )
}
