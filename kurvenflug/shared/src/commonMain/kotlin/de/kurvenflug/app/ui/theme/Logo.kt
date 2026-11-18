package de.kurvenflug.app.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Das Kurvenflug-Logo – genau das App-Symbol (icons/icon.svg, Android ic_launcher_foreground): Flugbahn, Ziel-Planet, Sonde,
 * zwei Sterne auf dunklem Grund. Koordinaten im 108er-Raster wie in den Symbol-Dateien.
 */
@Composable
fun KurvenflugLogo(modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { contentDescription = "Kurvenflug-Logo" }) {
        val k = size.minDimension / 108f
        fun p(x: Float, y: Float) = Offset(x * k, y * k)
        drawRoundRect(Color(0xFF0B1026), size = Size(108 * k, 108 * k), cornerRadius = CornerRadius(24 * k))
        val crop = 18f   // wie das Android-Symbol: der innere Bereich (sichere Zone) füllt das Bild
        val s = 108f / (108f - 2 * crop)
        fun q(x: Float, y: Float) = p((x - crop) * s, (y - crop) * s)
        val path = Path().apply { val a = q(30f, 76f); val c = q(46f, 26f); val e = q(76f, 62f); moveTo(a.x, a.y); quadraticTo(c.x, c.y, e.x, e.y) }
        drawPath(path, Color(0xFF38E8FF), style = Stroke(width = 3.5f * k * s, cap = StrokeCap.Round))
        drawCircle(Color(0xFFFF5CA8), 7f * k * s, q(76f, 62f))
        val probe = Path().apply { val a = q(50f, 38f); val b = q(58f, 43f); val c = q(50f, 48f); val d = q(52f, 43f)
            moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); lineTo(d.x, d.y); close() }
        drawPath(probe, Color(0xFFFFD54F))
        drawCircle(Color.White, 1.5f * k * s, q(36f, 38f)); drawCircle(Color.White, 1.2f * k * s, q(70f, 34f))
    }
}
