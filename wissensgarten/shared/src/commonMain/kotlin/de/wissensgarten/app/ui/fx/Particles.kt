package de.wissensgarten.app.ui.fx

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import kotlinx.coroutines.channels.Channel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class Burst { SPARKLE, WATER, LEAVES, CONFETTI }

/** Zufallsquelle für Partikel und gemischte Spielrunden – Bildschirmfoto-Tests setzen einen festen Startwert. */
object Zufall { var quelle: Random = Random.Default }

private class P(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    val color: Color, val size: Float, val kind: Burst,
    var rot: Float, val vr: Float, val life: Float, var age: Float = 0f,
)

private val SPARKLE_COLORS = listOf(Color(0xFFFFD54F), Color(0xFFFFF59D), Color.White, Color(0xFFFFB300))
private val WATER_COLORS = listOf(Color(0xFF4F9FD6), Color(0xFF81C4F0), Color(0xFFB3E0FF))
private val LEAF_COLORS = listOf(Color(0xFF6FA35A), Color(0xFF3F6B3A), Color(0xFF9CCC65), Color(0xFFC5E1A5))
private val CONFETTI_COLORS = listOf(
    Color(0xFFEF5350), Color(0xFFFFCA28), Color(0xFF66BB6A), Color(0xFF42A5F5), Color(0xFFAB47BC), Color(0xFFFF7043),
)

/**
 * Leichtes Partikelsystem. Positionen in Bruchteilen der Fläche (0..1),
 * damit Aufrufer keine Pixel kennen müssen. Läuft nur, solange Partikel leben.
 */
class Particles {
    private val list = mutableListOf<P>()
    private val wake = Channel<Unit>(Channel.CONFLATED)
    internal var frame by mutableLongStateOf(0L)

    fun burst(kind: Burst, x: Float = 0.5f, y: Float = 0.5f, count: Int = 24) {
        val r = Zufall.quelle
        repeat(count) {
            val a = when (kind) {
                Burst.WATER -> PI / 2 + r.nextDouble(-0.6, 0.6)            // nach unten
                Burst.CONFETTI -> PI / 2 + r.nextDouble(-0.4, 0.4)
                else -> r.nextDouble(0.0, 2 * PI)
            }
            val speed = when (kind) {
                Burst.SPARKLE -> r.nextDouble(0.25, 0.7)
                Burst.WATER -> r.nextDouble(0.1, 0.35)
                Burst.LEAVES -> r.nextDouble(0.2, 0.55)
                Burst.CONFETTI -> r.nextDouble(0.05, 0.25)
            }.toFloat()
            val colors = when (kind) {
                Burst.SPARKLE -> SPARKLE_COLORS; Burst.WATER -> WATER_COLORS
                Burst.LEAVES -> LEAF_COLORS; Burst.CONFETTI -> CONFETTI_COLORS
            }
            val px = if (kind == Burst.CONFETTI) r.nextFloat() else x + r.nextFloat() * 0.04f - 0.02f
            val py = if (kind == Burst.CONFETTI) -0.05f - r.nextFloat() * 0.3f else y
            list += P(
                px, py, (cos(a) * speed).toFloat(), (sin(a) * speed).toFloat() - if (kind == Burst.SPARKLE || kind == Burst.LEAVES) 0.25f else 0f,
                colors[r.nextInt(colors.size)],
                when (kind) { Burst.SPARKLE -> 0.012f; Burst.WATER -> 0.010f; Burst.LEAVES -> 0.022f; Burst.CONFETTI -> 0.016f } * (0.7f + r.nextFloat() * 0.6f),
                kind, r.nextFloat() * 360f, r.nextFloat() * 400f - 200f,
                when (kind) { Burst.CONFETTI -> 3.2f; Burst.LEAVES -> 1.6f; else -> 1.1f } * (0.8f + r.nextFloat() * 0.4f),
            )
        }
        wake.trySend(Unit)
    }

    internal suspend fun run() {
        var last = 0L
        while (true) {
            if (list.isEmpty()) { wake.receive(); last = 0L }
            withFrameNanos { now ->
                val dt = if (last == 0L) 0.016f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                val it = list.iterator()
                while (it.hasNext()) {
                    val p = it.next()
                    p.age += dt
                    if (p.age > p.life) { it.remove(); continue }
                    val gravity = when (p.kind) { Burst.WATER -> 1.6f; Burst.SPARKLE -> 0.5f; Burst.LEAVES -> 0.25f; Burst.CONFETTI -> 0.12f }
                    val drag = if (p.kind == Burst.LEAVES || p.kind == Burst.CONFETTI) 0.985f else 0.995f
                    p.vy += gravity * dt
                    p.vx *= drag; p.vy *= drag
                    if (p.kind == Burst.LEAVES || p.kind == Burst.CONFETTI) p.vx += sin(p.age * 5 + p.rot) * 0.02f * dt * 60
                    p.x += p.vx * dt; p.y += p.vy * dt
                    p.rot += p.vr * dt
                }
                frame = now
            }
        }
    }

    internal fun draw(scope: androidx.compose.ui.graphics.drawscope.DrawScope) = with(scope) {
        val w = size.width; val h = size.height; val m = size.minDimension
        for (p in list) {
            val alpha = (1f - p.age / p.life).coerceIn(0f, 1f).let { if (p.kind == Burst.CONFETTI) minOf(1f, it * 3) else it }
            val c = Offset(p.x * w, p.y * h)
            val s = p.size * m
            when (p.kind) {
                Burst.SPARKLE -> rotate(p.rot, c) { drawStar(c, s, p.color.copy(alpha = alpha)) }
                Burst.WATER -> drawCircle(p.color.copy(alpha = alpha), s, c)
                Burst.LEAVES -> rotate(p.rot, c) {
                    val path = Path().apply {
                        moveTo(c.x, c.y - s); quadraticTo(c.x + s * 0.7f, c.y, c.x, c.y + s); quadraticTo(c.x - s * 0.7f, c.y, c.x, c.y - s); close()
                    }
                    drawPath(path, p.color.copy(alpha = alpha))
                }
                Burst.CONFETTI -> rotate(p.rot, c) {
                    drawRect(p.color.copy(alpha = alpha), Offset(c.x - s / 2, c.y - s / 4), Size(s, s * 0.5f * (0.4f + 0.6f * kotlin.math.abs(sin(p.age * 8)))))
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStar(c: Offset, r: Float, color: Color) {
    val path = Path()
    for (i in 0 until 8) {
        val a = PI * i / 4
        val rr = if (i % 2 == 0) r else r * 0.35f
        val x = c.x + cos(a).toFloat() * rr; val y = c.y + sin(a).toFloat() * rr
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

@Composable
fun rememberParticles(): Particles {
    val p = remember { Particles() }
    LaunchedEffect(p) { p.run() }
    return p
}

/** Zeichenfläche für Partikel – über den Inhalt legen (matchParentSize). */
@Composable
fun ParticleLayer(particles: Particles, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        particles.frame // nur Zeichenphase invalidieren
        particles.draw(this)
    }
}
