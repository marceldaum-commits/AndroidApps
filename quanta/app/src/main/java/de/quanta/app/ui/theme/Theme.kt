package de.quanta.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

/** Weltraum mit Neon – für Jugendliche, nicht verspielt-kindlich. */
object Q {
    val Space = Color(0xFF0B1026)
    val Deep = Color(0xFF151B3D)
    val Panel = Color(0xFF1E2550)
    val Cyan = Color(0xFF38E8FF)
    val Pink = Color(0xFFFF5CA8)
    val Yellow = Color(0xFFFFD54F)
    val Green = Color(0xFF5CFFA1)
    val Orange = Color(0xFFFF8A3D)
    val Violet = Color(0xFFA78BFA)
    val Text = Color(0xFFE8ECFF)
    val Muted = Color(0xFF9AA3D0)
    val Grid = Color(0x22FFFFFF)
}

private val scheme = darkColorScheme(
    primary = Q.Cyan, onPrimary = Color(0xFF00222A),
    secondary = Q.Pink, onSecondary = Color.White,
    tertiary = Q.Yellow,
    background = Q.Space, onBackground = Q.Text,
    surface = Q.Deep, onSurface = Q.Text,
    surfaceVariant = Q.Panel, onSurfaceVariant = Q.Muted,
    surfaceContainer = Q.Deep, surfaceContainerHigh = Q.Panel, surfaceContainerLow = Q.Deep,
)

@Composable
fun QuantaTheme(content: @Composable () -> Unit) {
    val t = Typography()
    MaterialTheme(
        colorScheme = scheme,
        typography = t.copy(
            titleLarge = t.titleLarge.copy(fontWeight = FontWeight.Black),
            titleMedium = t.titleMedium.copy(fontWeight = FontWeight.Bold),
            headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Black),
        ),
        content = content,
    )
}
