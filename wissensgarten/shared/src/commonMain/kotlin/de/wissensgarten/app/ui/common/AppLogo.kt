package de.wissensgarten.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate

/**
 * Das Logo des Wissensgartens – genau dieselbe Zeichnung wie das App-Symbol
 * (Android `ic_launcher_foreground.xml`, WebApp `icons/icon-512.png`): Erdhügel, Stängel, zwei Blätter, Blüte als aufgeschlagenes Buch.
 */
@Composable
fun AppLogo(modifier: Modifier = Modifier, grow: Float = 1f) {
    Canvas(modifier) { drawAppLogo(grow) }
}

/** Zeichnet das Logo in die Fläche (Ausschnitt 30..78 × 22..84 des 108er-Symbols, damit es den Platz gut füllt). */
fun DrawScope.drawAppLogo(grow: Float = 1f) {
    val vx = 26f; val vy = 20f; val vw = 56f; val vh = 66f
    val s = minOf(size.width / vw, size.height / vh)
    val ox = (size.width - vw * s) / 2f - vx * s
    val oy = (size.height - vh * s) / 2f - vy * s
    translate(ox, oy) {
        scale(s, s, pivot = Offset.Zero) {
            fun p(build: Path.() -> Unit) = Path().apply(build)
            // Erdhügel
            drawPath(p { moveTo(30f, 74f); quadraticTo(54f, 62f, 78f, 74f); lineTo(78f, 78f); quadraticTo(54f, 84f, 30f, 78f); close() }, Color(0xFF7A5232))
            // Pflanze wächst aus der Erde (grow 0..1)
            scale(1f, 0.6f + 0.4f * grow, pivot = Offset(54f, 72f)) {
                drawPath(p { moveTo(54f, 72f); quadraticTo(52f, 56f, 54f, 40f) }, Color(0xFF3F6B3A), style = Stroke(width = 4f, cap = StrokeCap.Round))
                drawPath(p { moveTo(53f, 60f); quadraticTo(38f, 58f, 34f, 46f); quadraticTo(48f, 46f, 53f, 60f); close() }, Color(0xFF6FA35A))
                drawPath(p { moveTo(55f, 54f); quadraticTo(70f, 52f, 74f, 40f); quadraticTo(60f, 40f, 55f, 54f); close() }, Color(0xFF3F6B3A))
                drawPath(p { moveTo(54f, 40f); quadraticTo(46f, 30f, 38f, 32f); lineTo(38f, 26f); quadraticTo(47f, 24f, 54f, 32f); close() }, Color(0xFFF28AB2))
                drawPath(p { moveTo(54f, 40f); quadraticTo(62f, 30f, 70f, 32f); lineTo(70f, 26f); quadraticTo(61f, 24f, 54f, 32f); close() }, Color(0xFFE26A9C))
                drawCircle(Color(0xFFFFD54F), radius = 3f, center = Offset(54f, 30f))
            }
        }
    }
}
