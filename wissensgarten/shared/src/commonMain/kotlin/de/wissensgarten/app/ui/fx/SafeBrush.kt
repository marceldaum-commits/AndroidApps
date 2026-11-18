package de.wissensgarten.app.ui.fx

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush

/**
 * Farbverläufe, die nie ein ganzes Bild kosten (gleiche Aufrufe wie `Brush.verticalGradient` usw.).
 *
 * Anlass: Safari (iPad/iPhone) liefert beim Anlegen eines Verlaufs manchmal nichts zurück („Can't wrap nullptr“) –
 * die Ausnahme bricht dann jedes Bild ab, der Bildschirm bleibt weiß, obwohl die App läuft (bei Raumlicht auf dem iPad gefunden).
 * Diese Verläufe rechnen mit festen, endlichen Punkten und fallen bei einem Fehler stufenweise zurück:
 * ohne Zwischenstufen → zwei Farben → einfarbig. Ein Gerät, das keine Verläufe kann, scheitert so nicht in jedem Bild erneut.
 */
object SafeBrush {
    fun verticalGradient(colors: List<Color>, startY: Float = 0f, endY: Float = Float.POSITIVE_INFINITY): Brush =
        Safe(colors, null, Kind.VERTICAL, Offset(0f, startY), Offset(0f, endY))
    fun verticalGradient(vararg colorStops: Pair<Float, Color>, startY: Float = 0f, endY: Float = Float.POSITIVE_INFINITY): Brush =
        Safe(colorStops.map { it.second }, colorStops.map { it.first }, Kind.VERTICAL, Offset(0f, startY), Offset(0f, endY))
    fun horizontalGradient(colors: List<Color>, startX: Float = 0f, endX: Float = Float.POSITIVE_INFINITY): Brush =
        Safe(colors, null, Kind.HORIZONTAL, Offset(startX, 0f), Offset(endX, 0f))
    fun linearGradient(colors: List<Color>, start: Offset = Offset.Zero, end: Offset = Offset.Infinite): Brush =
        Safe(colors, null, Kind.LINEAR, start, end)
    fun linearGradient(vararg colorStops: Pair<Float, Color>, start: Offset = Offset.Zero, end: Offset = Offset.Infinite): Brush =
        Safe(colorStops.map { it.second }, colorStops.map { it.first }, Kind.LINEAR, start, end)
    fun radialGradient(colors: List<Color>, center: Offset = Offset.Unspecified, radius: Float = Float.POSITIVE_INFINITY): Brush =
        Safe(colors, null, Kind.RADIAL, center, Offset.Zero, radius)

    private enum class Kind { VERTICAL, HORIZONTAL, LINEAR, RADIAL }

    /** Ab welcher Rückfall-Stufe es auf diesem Gerät klappt (0 = voller Verlauf … 3 = einfarbig) */
    private var step = 0
    private val reported = HashSet<String>()
    private fun once(text: String) { if (reported.add(text)) drawNote(text) }

    private class Safe(val colors: List<Color>, val stops: List<Float>?, val kind: Kind, val from: Offset, val to: Offset, val radius: Float = 0f) : ShaderBrush() {
        override fun createShader(size: Size): Shader {
            val w = fin(size.width); val h = fin(size.height)
            fun x(v: Float) = if (v.isInfinite() || v.isNaN()) w else v
            fun y(v: Float) = if (v.isInfinite() || v.isNaN()) h else v
            fun make(c: List<Color>, s: List<Float>?): Shader = when (kind) {
                Kind.VERTICAL -> LinearGradientShader(Offset(0f, y(from.y)), Offset(0f, y(to.y)), c, s)
                Kind.HORIZONTAL -> LinearGradientShader(Offset(x(from.x), 0f), Offset(x(to.x), 0f), c, s)
                Kind.LINEAR -> LinearGradientShader(Offset(x(from.x), y(from.y)), Offset(x(to.x), y(to.y)), c, s)
                Kind.RADIAL -> {
                    val c0 = if (from == Offset.Unspecified || from.x.isNaN()) Offset(w / 2, h / 2) else from
                    val r = if (radius.isInfinite() || radius.isNaN() || radius <= 0f) maxOf(w, h) / 2 else radius
                    RadialGradientShader(c0, r, c, s)
                }
            }
            val what = "Verlauf " + kind.name.lowercase()
            if (colors.size == 1) return solidShader(colors[0])
            if (step <= 0) try { return make(colors, stops) } catch (e: Throwable) { once("$what gescheitert (${size.width}×${size.height}, ${colors.size} Farben): $e") }
            if (step <= 1 && stops != null) try { return make(colors, null).also { step = maxOf(step, 1); once("$what: geht ohne Zwischenstufen") } } catch (e: Throwable) { once("$what ohne Zwischenstufen gescheitert: $e") }
            if (step <= 2) try { return make(listOf(colors.first(), colors.last()), null).also { if (colors.size > 2 || stops != null) step = maxOf(step, 2); once("$what: geht mit zwei Farben") } } catch (e: Throwable) { once("$what mit zwei Farben gescheitert: $e") }
            step = 3
            return solidShader(colors[colors.size / 2]).also { once("$what: einfarbig als Ersatz") }
        }
    }

    private fun fin(v: Float) = if (v.isNaN() || v.isInfinite() || v <= 0f) 1f else v
}

/** Einfarbige Füllung als Shader – der letzte Rückfall, wenn sich kein Verlauf anlegen lässt */
expect fun solidShader(color: Color): Shader

/** Meldung für die Browser-Konsole bzw. das Fehlerprotokoll (Android: ohne Wirkung) */
expect fun drawNote(text: String)
