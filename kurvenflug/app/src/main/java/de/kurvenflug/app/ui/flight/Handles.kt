package de.kurvenflug.app.ui.flight

import androidx.compose.ui.graphics.Color
import de.kurvenflug.app.ui.theme.Q
import de.kurvenflug.core.model.Kind
import de.kurvenflug.core.model.Level
import de.kurvenflug.core.model.Vec
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Ein Griff auf dem Spielfeld: Wo er sitzt (aus den aktuellen Werten) und welche Werte sich ändern,
 * wenn man ihn an eine Stelle zieht. So formt man die Kurve mit dem Finger – die Formel rechnet mit.
 */
class Handle(val pos: Vec, val color: Color, val apply: (Vec) -> Map<String, Double>)

/** Pixel pro Einheit beim Wurf-Zielpfeil: Länge = Tempo × [AIM] */
private const val AIM = 0.6

fun handlesFor(level: Level, values: Map<String, Double>): List<Handle> {
    val v = level.values(values)
    val free = level.params.associateBy { it.key }
    fun snap(key: String, x: Double): Map<String, Double> = free[key]?.let { mapOf(key to it.snap(x)) } ?: emptyMap()
    fun has(vararg k: String) = k.any { it in free }
    val out = ArrayList<Handle>()
    when (level.kind) {
        Kind.LINE -> {
            val m = v.getValue("m"); val b = v.getValue("b")
            if (has("b")) out += Handle(Vec(0.0, b), Q.Pink) { p -> snap("b", p.y) }
            if (has("m")) out += Handle(Vec(4.0, 4 * m + b), Q.Cyan) { p -> snap("m", (p.y - b) / 4) }
        }
        Kind.PARABOLA -> {
            val a = v.getValue("a"); val d = v.getValue("d"); val e = v.getValue("e")
            if (has("d", "e")) out += Handle(Vec(d, e), Q.Pink) { p -> snap("d", p.x) + snap("e", p.y) }
            if (has("a")) out += Handle(Vec(d + 2, 4 * a + e), Q.Cyan) { p -> snap("a", (p.y - e) / 4) }
        }
        Kind.SINE -> {
            val a = v.getValue("a"); val b = v.getValue("b"); val d = v.getValue("d")
            if (has("d")) out += Handle(Vec(0.0, d), Q.Yellow) { p -> snap("d", p.y) }
            if (has("a", "b")) out += Handle(Vec(PI / 2 / b, a + d), Q.Cyan) { p ->
                (if (p.x > 0.3) snap("b", PI / 2 / p.x) else emptyMap()) + snap("a", p.y - d)
            }
        }
        Kind.EXP -> {
            val a = v.getValue("a"); val b = v.getValue("b"); val d = v.getValue("d")
            if (has("a")) out += Handle(Vec(0.0, a + d), Q.Cyan) { p -> snap("a", p.y - d) }
            if (has("b")) out += Handle(Vec(1.0, a * b + d), Q.Pink) { p -> if (a != 0.0) snap("b", (p.y - d) / a) else emptyMap() }
        }
        // Wurf und Schleuder: Zielpfeil wie bei einer Steinschleuder – Richtung = Winkel, Länge = Tempo
        Kind.THROW, Kind.ORBIT -> {
            val ang = Math.toRadians(v.getValue("angle")); val s = v.getValue("v")
            out += Handle(Vec(level.launch.x + cos(ang) * s * AIM, level.launch.y + sin(ang) * s * AIM), Q.Yellow) { p ->
                val dx = p.x - level.launch.x; val dy = p.y - level.launch.y
                snap("angle", Math.toDegrees(atan2(dy, dx))) + snap("v", hypot(dx, dy) / AIM)
            }
        }
        Kind.POINT, Kind.RENDEZVOUS ->
            out += Handle(Vec(v.getValue("x"), v.getValue("y")), Q.Yellow) { p -> snap("x", p.x) + snap("y", p.y) }
    }
    return out
}
