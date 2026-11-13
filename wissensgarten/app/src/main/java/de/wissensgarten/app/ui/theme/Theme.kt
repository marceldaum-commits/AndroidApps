package de.wissensgarten.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Eigene, warme Garten-Palette statt Systemfarben: der Garten soll immer nach Garten aussehen.
val Moss = Color(0xFF3F6B3A)
val Leaf = Color(0xFF6FA35A)
val Soil = Color(0xFF7A5232)
val SoilDark = Color(0xFF4A3220)
val Cream = Color(0xFFFBF6EC)
val Water = Color(0xFF4F9FD6)
val Straw = Color(0xFFD8B25A)

private val Light = lightColorScheme(
    primary = Moss, onPrimary = Color.White,
    primaryContainer = Color(0xFFCDE8BE), onPrimaryContainer = Color(0xFF0E2A0B),
    secondary = Water, onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E8F7), onSecondaryContainer = Color(0xFF0B2D45),
    tertiary = Color(0xFFB0602F), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDCC4), onTertiaryContainer = Color(0xFF3A1600),
    background = Cream, onBackground = Color(0xFF22261F),
    surface = Cream, onSurface = Color(0xFF22261F),
    surfaceVariant = Color(0xFFEDE5D3), onSurfaceVariant = Color(0xFF4D4A3F),
    surfaceContainer = Color(0xFFF3ECDD), surfaceContainerHigh = Color(0xFFEDE5D3),
    surfaceContainerLow = Color(0xFFF7F1E5), surfaceContainerHighest = Color(0xFFE6DDC9),
    outline = Color(0xFF8A8270),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFA6D394), onPrimary = Color(0xFF123A0E),
    primaryContainer = Color(0xFF2A5124), onPrimaryContainer = Color(0xFFCDE8BE),
    secondary = Color(0xFF9CCBEE), onSecondary = Color(0xFF0B2D45),
    secondaryContainer = Color(0xFF214A68), onSecondaryContainer = Color(0xFFD3E8F7),
    tertiary = Color(0xFFFFB68A), onTertiary = Color(0xFF552100),
    tertiaryContainer = Color(0xFF7A3A12), onTertiaryContainer = Color(0xFFFFDCC4),
    background = Color(0xFF15190F), onBackground = Color(0xFFE5E3D6),
    surface = Color(0xFF15190F), onSurface = Color(0xFFE5E3D6),
    surfaceVariant = Color(0xFF2E3326), onSurfaceVariant = Color(0xFFC6C2B0),
    surfaceContainer = Color(0xFF1E2318), surfaceContainerHigh = Color(0xFF282D21),
    surfaceContainerLow = Color(0xFF1A1E14), surfaceContainerHighest = Color(0xFF32382B),
    outline = Color(0xFF908C7B),
)

private val Rounded = FontFamily.SansSerif

private val AppTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontWeight = FontWeight.Bold, fontFamily = Rounded),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold, fontFamily = Rounded),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold, fontFamily = Rounded),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold, fontFamily = Rounded),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun WissensgartenTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) Dark else Light,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}

val BigNumber = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Black)
