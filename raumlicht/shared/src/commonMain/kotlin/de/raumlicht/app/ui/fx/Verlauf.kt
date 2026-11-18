package de.raumlicht.app.ui.fx

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush

// Farbverläufe, die nie ein ganzes Bild kosten.
// Anlass (16.11.2026): Auf einem iPad mit Safari 27.2 lieferte das Anlegen eines Verlaufs nichts zurück („Can't wrap nullptr“); die Ausnahme brach
// jedes Bild beim ersten Verlauf ab – der Bildschirm blieb weiß, obwohl die App lief. Diese Verläufe rechnen deshalb mit festen, endlichen Punkten
// (nicht mit „unendlich“ wie Brush.verticalGradient) und fallen bei einem Fehler stufenweise zurück: ohne Zwischenstufen → zwei Farben → einfarbig.
// Welche Stufe trägt, steht einmal in der Mitschrift der Diagnose-Seite.

/** Einfarbige Füllung als Shader – der letzte Rückfall, wenn sich kein Verlauf anlegen lässt */
expect fun solidShader(color: Color): Shader
/** Zeile für die Mitschrift der Diagnose-Seite (nur im Browser; sonst ohne Wirkung) */
expect fun drawNote(text: String)

private var reported = HashSet<String>()
private fun once(text: String) { if (reported.add(text)) drawNote(text) }
/**
 * Gestrichelte Linie – oder null (dann durchgezogen), wenn die Zeichenbibliothek das Muster nicht anlegen kann.
 * Auf iPads mit neuem Safari scheitern solche Anfragen wie die Verläufe („Can't wrap nullptr“); ohne diesen Schutz bliebe der ganze Bildschirm leer.
 */
private var dashWorks = true
fun safeDash(on: Float, off: Float): androidx.compose.ui.graphics.PathEffect? {
    if (!dashWorks) return null
    return try { androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(on, off)).also { once("Strichmuster: in Ordnung") } }
    catch (e: Throwable) { dashWorks = false; once("Strichmuster gescheitert: ${e::class.simpleName}: ${e.message} – Linien durchgezogen"); null }
}
private fun finite(v: Float) = if (v.isNaN() || v.isInfinite() || v <= 0f) 1f else v

// Ab welcher Stufe es auf diesem Gerät klappt – damit ein Gerät, das keine Verläufe kann, nicht in jedem Bild erneut scheitert
private var step = 0

private class SafeGradient(val colors: List<Color>, val stops: List<Float>?, val kind: Int, val from: Offset? = null, val to: Offset? = null, val radius: Float = 0f) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val w = finite(size.width); val h = finite(size.height)
        fun make(c: List<Color>, s: List<Float>?): Shader = when (kind) {
            0 -> LinearGradientShader(from ?: Offset.Zero, to ?: Offset(0f, h), c, s)
            1 -> LinearGradientShader(from ?: Offset.Zero, to ?: Offset(w, 0f), c, s)
            else -> RadialGradientShader(from ?: Offset(w / 2, h / 2), if (radius > 0f) radius else maxOf(w, h) / 2, c, s)
        }
        val what = "Verlauf " + listOf("senkrecht", "waagerecht", "rund")[kind]
        if (step <= 0) try { return make(colors, stops).also { once("$what: in Ordnung") } } catch (e: Throwable) { once("$what gescheitert (${size.width} × ${size.height}, ${colors.size} Farben, Stufen ${stops != null}): $e") }
        if (step <= 1 && stops != null) try { return make(colors, null).also { step = maxOf(step, 1); once("$what: geht ohne Zwischenstufen") } } catch (e: Throwable) { once("$what ohne Zwischenstufen gescheitert: $e") }
        if (step <= 2) try { return make(listOf(colors.first(), colors.last()), null).also { if (stops == null && colors.size == 2) Unit else step = maxOf(step, 2); once("$what: geht mit zwei Farben") } } catch (e: Throwable) { once("$what mit zwei Farben gescheitert: $e") }
        step = 3
        return try { solidShader(colors[colors.size / 2]).also { once("$what: einfarbig als Ersatz") } } catch (e: Throwable) { once("auch einfarbig gescheitert: $e"); throw e }
    }
}

/** Senkrechter Verlauf von oben nach unten; ohne Angabe über die ganze Fläche */
fun verticalFade(colors: List<Color>, startY: Float? = null, endY: Float? = null): Brush =
    SafeGradient(colors, null, 0, startY?.let { Offset(0f, it) }, endY?.let { Offset(0f, it) })
fun verticalFade(vararg stops: Pair<Float, Color>): Brush = SafeGradient(stops.map { it.second }, stops.map { it.first }, 0)
/** Waagerechter Verlauf von links nach rechts */
fun horizontalFade(colors: List<Color>): Brush = SafeGradient(colors, null, 1)
/** Runder Verlauf; ohne Angabe aus der Mitte der Fläche */
fun radialFade(colors: List<Color>, center: Offset? = null, radius: Float = 0f): Brush = SafeGradient(colors, null, 2, center, radius = radius)
