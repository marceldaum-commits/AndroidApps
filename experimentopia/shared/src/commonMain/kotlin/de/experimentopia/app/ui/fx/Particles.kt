package de.experimentopia.app.ui.fx

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import de.experimentopia.app.ui.theme.E
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Funken, Spritzer, Blasen – rein berechnet aus der Zeit seit dem Ausbruch. */
data class Burst(val at: Offset, val colors: List<Color>, val count: Int = 30, val speed: Float = 240f, val seed: Int = 1, val gravity: Float = 0f, val life: Float = 1.2f)

fun DrawScope.drawBurst(b: Burst, seconds: Float) {
    if (seconds < 0f || seconds > b.life) return
    val r = Random(b.seed)
    val fade = (1f - seconds / b.life).coerceIn(0f, 1f)
    repeat(b.count) { i ->
        val ang = r.nextFloat() * 6.283f
        val sp = b.speed * (0.3f + r.nextFloat()) * density
        val x = b.at.x + cos(ang) * sp * seconds
        val y = b.at.y + sin(ang) * sp * seconds + 0.5f * b.gravity * density * seconds * seconds
        drawCircle(b.colors[i % b.colors.size].copy(alpha = fade), (2f + r.nextFloat() * 3f) * density * (1f - seconds / (b.life * 2)), Offset(x, y))
    }
}

/** Konfetti-Regen über den ganzen Bildschirm, ausgelöst, wenn [trigger] größer wird. */
@Composable
fun Confetti(trigger: Int, modifier: Modifier = Modifier, pieces: Int = 90) {
    if (trigger <= 0) return
    val t = remember(trigger) { mutableFloatStateOf(0f) }
    LaunchedEffect(trigger) {
        var start = -1L
        while (t.floatValue < 3.2f) withFrameNanos { n -> if (start < 0) start = n; t.floatValue = (n - start) / 1e9f }
    }
    val colors = listOf(E.Mint, E.Pink, E.Sun, E.Sky, E.Violet, E.Orange)
    Canvas(modifier.fillMaxSize()) {
        val s = t.floatValue
        if (s >= 3.2f) return@Canvas
        val r = Random(trigger * 31)
        repeat(pieces) { i ->
            val x0 = r.nextFloat() * size.width
            val vx = (r.nextFloat() - 0.5f) * 120f * density
            val vy = (-420f - r.nextFloat() * 380f) * density
            val g = 900f * density
            val delay = r.nextFloat() * 0.25f
            val tt = (s - delay).coerceAtLeast(0f)
            val x = x0 + vx * tt + sin(tt * 6f + i) * 14f * density
            val y = size.height * 0.95f + vy * tt + 0.5f * g * tt * tt
            val rot = tt * (200f + r.nextFloat() * 400f)
            val a = (1f - (s / 3.2f)).coerceIn(0f, 1f)
            rotate(rot, Offset(x, y)) {
                drawRect(colors[i % colors.size].copy(alpha = a), Offset(x - 4 * density, y - 2.5f * density), Size(8 * density, 5 * density))
            }
        }
    }
}

/** Sammelt Ausbrüche und malt sie – für Labore: `val fx = rememberBursts(); fx.add(...)` und `drawBursts(fx, now)`. */
class Bursts { val list = mutableStateListOf<Pair<Burst, Float>>(); fun add(b: Burst, now: Float) { list += b to now; if (list.size > 12) list.removeAt(0) } }

@Composable fun rememberBursts() = remember { Bursts() }

fun DrawScope.drawBursts(b: Bursts, now: Float) { b.list.forEach { (burst, at) -> drawBurst(burst, now - at) } }
