package de.kurvenflug.app.ui.fx

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Funken für Treffer, Explosionen und Konfetti – rein berechnet aus der Zeit seit dem Ausbruch. */
data class Burst(val at: Offset, val colors: List<Color>, val count: Int = 36, val speed: Float = 260f, val seed: Int = 1, val gravity: Float = 0f)

fun DrawScope.drawBurst(b: Burst, seconds: Float) {
    if (seconds < 0f || seconds > 1.4f) return
    val r = Random(b.seed)
    repeat(b.count) { i ->
        val ang = r.nextFloat() * 6.283f
        val sp = b.speed * (0.35f + r.nextFloat()) * density
        val x = b.at.x + cos(ang) * sp * seconds
        val y = b.at.y + sin(ang) * sp * seconds + 0.5f * b.gravity * density * seconds * seconds
        val alpha = (1f - seconds / 1.4f).coerceIn(0f, 1f)
        drawCircle(b.colors[i % b.colors.size].copy(alpha = alpha), (2.5f + r.nextFloat() * 2.5f) * density * (1f - seconds / 2f), Offset(x, y))
    }
}
