package de.experimentopia.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

/** Nachtlabor mit leuchtenden Flüssigkeiten – bunt, aber nicht babyhaft. */
object E {
    val Night = Color(0xFF160F2E)
    val Deep = Color(0xFF221845)
    val Panel = Color(0xFF2E2260)
    val Glass = Color(0x33FFFFFF)
    val Mint = Color(0xFF3DF5C8)
    val Pink = Color(0xFFFF5FA2)
    val Sun = Color(0xFFFFD84D)
    val Sky = Color(0xFF7CC8FF)
    val Orange = Color(0xFFFFA23D)
    val Violet = Color(0xFFB98CFF)
    val Red = Color(0xFFFF5A5A)
    val Green = Color(0xFF6BEB7A)
    val Text = Color(0xFFF3EEFF)
    val Muted = Color(0xFFA99BD6)
    val Line = Color(0x33FFFFFF)
}

fun Long.color() = Color(this.toInt())

private val scheme = darkColorScheme(
    primary = E.Mint, onPrimary = Color(0xFF00281F),
    secondary = E.Pink, onSecondary = Color.White,
    tertiary = E.Sun,
    background = E.Night, onBackground = E.Text,
    surface = E.Deep, onSurface = E.Text,
    surfaceVariant = E.Panel, onSurfaceVariant = E.Muted,
    surfaceContainer = E.Deep, surfaceContainerHigh = E.Panel, surfaceContainerLow = E.Deep, surfaceContainerHighest = E.Panel,
)

@Composable
fun ExperimentopiaTheme(content: @Composable () -> Unit) {
    val t = Typography()
    MaterialTheme(
        colorScheme = scheme,
        typography = t.copy(
            titleLarge = t.titleLarge.copy(fontWeight = FontWeight.Black),
            titleMedium = t.titleMedium.copy(fontWeight = FontWeight.Bold),
            headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Black),
            headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.Black),
        ),
        content = content,
    )
}
