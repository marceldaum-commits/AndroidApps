package de.experimentopia.app.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Das Experimentopia-Logo – genau das App-Symbol (Android `ic_launcher_foreground`, Web `icon-512.png`, Ladebild):
 * Erlenmeyerkolben mit leuchtender Flüssigkeit, zwei Augen, drei Blasen. In der App schweben die Blasen sanft.
 * Gezeichnet im Koordinatensystem des App-Symbols (108 × 108), Ausschnitt 16…92 × 8…84.
 */
@Composable
fun AppLogo(size: Dp = 64.dp, modifier: Modifier = Modifier) {
    val t by rememberInfiniteTransition(label = "logo").animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse), label = "b")
    Canvas(modifier.size(size)) {
        val s = this.size.width / 76f
        fun p(x: Float, y: Float) = Offset((x - 16f) * s, (y - 8f) * s)
        drawCircle(Brush.radialGradient(listOf(Color(0x553DF5C8), Color(0x003DF5C8)), center = p(54f, 66f), radius = 34f * s), 34f * s, p(54f, 66f))
        val liquid = Path().apply {
            moveTo(p(37f, 66f).x, p(37f, 66f).y); lineTo(p(71f, 66f).x, p(71f, 66f).y); lineTo(p(77f, 78f).x, p(77f, 78f).y)
            quadraticTo(p(78f, 82f).x, p(78f, 82f).y, p(74f, 82f).x, p(74f, 82f).y); lineTo(p(34f, 82f).x, p(34f, 82f).y)
            quadraticTo(p(30f, 82f).x, p(30f, 82f).y, p(31f, 78f).x, p(31f, 78f).y); close()
        }
        drawPath(liquid, Color(0xFF3DF5C8))
        val glass = Path().apply {
            moveTo(p(47f, 30f).x, p(47f, 30f).y); lineTo(p(47f, 50f).x, p(47f, 50f).y); lineTo(p(31f, 78f).x, p(31f, 78f).y)
            quadraticTo(p(30f, 82f).x, p(30f, 82f).y, p(34f, 82f).x, p(34f, 82f).y); lineTo(p(74f, 82f).x, p(74f, 82f).y)
            quadraticTo(p(78f, 82f).x, p(78f, 82f).y, p(77f, 78f).x, p(77f, 78f).y); lineTo(p(61f, 50f).x, p(61f, 50f).y); lineTo(p(61f, 30f).x, p(61f, 30f).y)
        }
        val white = Color(0xFFF3EEFF)
        drawPath(glass, white, style = Stroke(3f * s, join = StrokeJoin.Round))
        drawLine(white, p(43f, 30f), p(65f, 30f), 3f * s, cap = StrokeCap.Round)
        val dark = Color(0xFF160F2E)
        drawCircle(dark, 2.5f * s, p(47f, 73f)); drawCircle(dark, 2.5f * s, p(61f, 73f))
        val up = t * 2.5f
        drawCircle(Color(0xFFFF5FA2), 4f * s, p(54f, 22f - up))
        drawCircle(Color(0xFFFFD84D), 2.5f * s, p(63f, 14f + up * 0.6f))
        drawCircle(Color(0xFF7CC8FF), 2f * s, p(46f, 15f - up * 0.8f))
    }
}
