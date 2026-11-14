package de.kurvenflug.app.ui.flight

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import de.kurvenflug.app.ui.fx.Burst
import de.kurvenflug.app.ui.fx.drawBurst
import de.kurvenflug.app.ui.theme.Q
import de.kurvenflug.core.model.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Was gerade auf dem Spielfeld zu sehen ist. */
data class SpaceView(
    val level: Level,
    /** Vorschau der Bahn (Anfang oder ganz, je nach Zielhilfe) */
    val preview: List<Vec>,
    /** Markierung für Koordinaten-Level (nur mit Zielhilfe) */
    val crosshair: Vec?,
    /** frühere Versuche, blass */
    val ghosts: List<List<Vec>>,
    /** aktueller Flug bis hierher */
    val trail: List<Vec>,
    val probe: Vec?,
    val hitTargets: Set<Int>,
    val takenCells: Set<Int>,
    val revealHidden: Boolean,
    val bursts: List<Pair<Burst, Float>>,
    val probeColor: Color,
    val time: Float,
    /** zweite Sonde (Rendezvous) */
    val preview2: List<Vec> = emptyList(),
    val trail2: List<Vec> = emptyList(),
    val probe2: Vec? = null,
)

/** Zeichnet das All: Raster, Achsen, Stationen, Asteroiden, Energiezellen, Bahnen und die Sonde. */
@Composable
fun SpaceCanvas(v: SpaceView, modifier: Modifier = Modifier) {
    val tm = rememberTextMeasurer()
    Canvas(modifier) {
        val l = v.level
        fun px(p: Vec) = Offset(((p.x + Flight.R) / (2 * Flight.R) * size.width).toFloat(), ((Flight.R - p.y) / (2 * Flight.R) * size.height).toFloat())
        val unit = size.width / (2 * Flight.R).toFloat()

        when (l.kind) {
            Kind.THROW -> drawThrowScene(l, ::px, unit, tm)
            Kind.ORBIT -> drawOrbitScene(l, ::px, unit, v.time)
            else -> {
                drawGrid(::px, unit, tm)
                if (l.kind == Kind.SINE) drawPiMarks(::px, unit, tm)
            }
        }

        // Asteroiden und Planeten
        l.asteroids.forEachIndexed { i, a ->
            val mass = l.masses.getOrElse(i) { 0.0 }
            if (mass > 0) drawPlanet(px(Vec(a.x, a.y)), a.r.toFloat() * unit, mass, i, v.time)
            else drawAsteroid(px(Vec(a.x, a.y)), a.r.toFloat() * unit, i, l.kind == Kind.THROW)
        }
        // Energiezellen
        l.cells.forEachIndexed { i, c ->
            if (i in v.takenCells) return@forEachIndexed
            val o = px(Vec(c.x, c.y))
            val glow = 0.6f + 0.4f * sin(v.time * 4f + i)
            drawCircle(Q.Yellow.copy(alpha = 0.25f * glow), unit * 0.7f, o)
            drawBolt(o, unit * 0.5f)
        }
        // Stationen
        l.targets.forEachIndexed { i, t ->
            if (l.hidden && !v.revealHidden && i !in v.hitTargets) return@forEachIndexed
            drawStation(px(Vec(t.x, t.y)), t.r.toFloat() * unit, i in v.hitTargets, v.time + i)
        }
        // Frühere Versuche
        v.ghosts.forEach { g -> drawPathLine(g, ::px, Color.White.copy(alpha = 0.18f), unit * 0.08f, dashed = false) }
        // Vorschau
        if (v.preview.size > 1) drawPathLine(v.preview, ::px, v.probeColor.copy(alpha = 0.75f), unit * 0.1f, dashed = true)
        if (v.preview2.size > 1) drawPathLine(v.preview2, ::px, Q.Orange.copy(alpha = 0.75f), unit * 0.1f, dashed = true)
        if (v.trail2.size > 1) {
            drawPathLine(v.trail2, ::px, Q.Orange.copy(alpha = 0.35f), unit * 0.35f, dashed = false)
            drawPathLine(v.trail2, ::px, Q.Orange, unit * 0.12f, dashed = false)
        }
        v.probe2?.let { p ->
            val prev = v.trail2.getOrNull(v.trail2.size - 6) ?: p
            val ang = atan2(-(p.y - prev.y), p.x - prev.x).toFloat()
            drawProbe(px(p), unit, Math.toDegrees(ang.toDouble()).toFloat(), Q.Orange, v.time)
        }
        v.crosshair?.let { c ->
            val o = px(c)
            drawCircle(v.probeColor.copy(alpha = 0.8f), unit * 0.45f, o, style = Stroke(unit * 0.08f))
            drawLine(v.probeColor, o - Offset(unit * 0.7f, 0f), o + Offset(unit * 0.7f, 0f), unit * 0.06f)
            drawLine(v.probeColor, o - Offset(0f, unit * 0.7f), o + Offset(0f, unit * 0.7f), unit * 0.06f)
        }
        // Flug
        if (v.trail.size > 1) {
            drawPathLine(v.trail, ::px, v.probeColor.copy(alpha = 0.35f), unit * 0.35f, dashed = false)
            drawPathLine(v.trail, ::px, v.probeColor, unit * 0.12f, dashed = false)
        }
        v.probe?.let { p ->
            val prev = v.trail.getOrNull(v.trail.size - 6) ?: v.trail.firstOrNull() ?: p
            val ang = atan2(-(p.y - prev.y), p.x - prev.x).toFloat()
            drawProbe(px(p), unit, Math.toDegrees(ang.toDouble()).toFloat(), v.probeColor, v.time)
        }
        v.bursts.forEach { (b, s) -> drawBurst(b, s) }
    }
}

private fun DrawScope.label(tm: TextMeasurer, text: String, at: Offset, color: Color = Q.Muted, sizeSp: Int = 10) {
    val r = tm.measure(text, TextStyle(color = color, fontSize = sizeSp.sp))
    drawText(r, topLeft = Offset(at.x - r.size.width / 2f, at.y - r.size.height / 2f))
}

private fun DrawScope.drawGrid(px: (Vec) -> Offset, unit: Float, tm: TextMeasurer) {
    val r = Flight.R.toInt()
    for (i in -r..r) {
        val major = i % 5 == 0
        val c = if (i == 0) Q.Cyan.copy(alpha = 0.55f) else if (major) Color.White.copy(alpha = 0.16f) else Q.Grid
        drawLine(c, px(Vec(i.toDouble(), -Flight.R)), px(Vec(i.toDouble(), Flight.R)), if (i == 0) 2f else 1f)
        drawLine(c, px(Vec(-Flight.R, i.toDouble())), px(Vec(Flight.R, i.toDouble())), if (i == 0) 2f else 1f)
    }
    // Beschriftung jede zweite Linie
    for (i in -8..8 step 2) {
        if (i == 0) continue
        label(tm, i.toString().replace("-", "−"), px(Vec(i.toDouble(), 0.0)) + Offset(0f, unit * 0.55f))
        label(tm, i.toString().replace("-", "−"), px(Vec(0.0, i.toDouble())) + Offset(-unit * 0.6f, 0f))
    }
    label(tm, "x", px(Vec(9.6, 0.0)) + Offset(0f, -unit * 0.5f), Q.Cyan, 12)
    label(tm, "y", px(Vec(0.0, 9.6)) + Offset(unit * 0.5f, 0f), Q.Cyan, 12)
}

private fun DrawScope.drawThrowScene(l: Level, px: (Vec) -> Offset, unit: Float, tm: TextMeasurer) {
    val ground = px(Vec(-Flight.R, l.ground))
    val (top, bottom) = when (l.planet) {
        Planet.EARTH -> Color(0xFF3E8E5B) to Color(0xFF1D4630)
        Planet.MOON -> Color(0xFF9EA3B5) to Color(0xFF50556A)
        Planet.MARS -> Color(0xFFD0653A) to Color(0xFF6E2A14)
    }
    drawRect(Brush.verticalGradient(listOf(top, bottom), startY = ground.y, endY = size.height), Offset(0f, ground.y), Size(size.width, size.height - ground.y))
    // Meter-Markierungen ab Abschuss
    for (m in 2..18 step 2) {
        val x = l.launch.x + m
        if (x > Flight.R) break
        val o = px(Vec(x, l.ground))
        drawLine(Color.White.copy(alpha = 0.5f), o, o + Offset(0f, unit * 0.35f), 2f)
        label(tm, "$m m", o + Offset(0f, unit * 0.75f), Color.White.copy(alpha = 0.8f))
    }
    // Höhen-Hilfslinien
    for (h in 2..18 step 2) {
        val y = l.launch.y + h
        if (y > Flight.R) break
        drawLine(Q.Grid, px(Vec(-Flight.R, y)), px(Vec(Flight.R, y)), 1f)
        label(tm, "$h m", px(Vec(9.0, y)) + Offset(0f, -unit * 0.3f))
    }
    // Abschussrampe
    val lo = px(l.launch)
    drawCircle(Color(0xFF2B3266), unit * 0.6f, lo)
    drawCircle(Q.Cyan, unit * 0.6f, lo, style = Stroke(unit * 0.1f))
    label(tm, "${l.planet.emoji} ${l.planet.label} · g = ${num(l.planet.g)} m/s²", Offset(size.width / 2, unit * 0.8f), Q.Text, 12)
}

/** π-Marken auf der x-Achse – Sinus denkt in π, nicht in Kästchen. */
private fun DrawScope.drawPiMarks(px: (Vec) -> Offset, unit: Float, tm: TextMeasurer) {
    val marks = listOf(-2 to "−2π", -1 to "−π", 1 to "π", 2 to "2π")
    for ((k, text) in marks) {
        val o = px(Vec(k * Math.PI, 0.0))
        drawLine(Q.Pink.copy(alpha = 0.7f), o - Offset(0f, unit * 0.35f), o + Offset(0f, unit * 0.35f), 3f)
        label(tm, text, o + Offset(0f, -unit * 0.75f), Q.Pink, 11)
    }
    for (k in listOf(-1, 1)) {
        val o = px(Vec(k * Math.PI / 2, 0.0))
        drawLine(Q.Pink.copy(alpha = 0.45f), o - Offset(0f, unit * 0.25f), o + Offset(0f, unit * 0.25f), 2f)
    }
}

/** Weltraum ohne Boden: Startrampe und ein feines Raster zur Orientierung. */
private fun DrawScope.drawOrbitScene(l: Level, px: (Vec) -> Offset, unit: Float, t: Float) {
    for (i in -10..10 step 2) {
        drawLine(Q.Grid, px(Vec(i.toDouble(), -Flight.R)), px(Vec(i.toDouble(), Flight.R)), 1f)
        drawLine(Q.Grid, px(Vec(-Flight.R, i.toDouble())), px(Vec(Flight.R, i.toDouble())), 1f)
    }
    val lo = px(l.launch)
    drawCircle(Color(0xFF2B3266), unit * 0.6f, lo)
    drawCircle(Q.Cyan, unit * 0.6f, lo, style = Stroke(unit * 0.1f))
}

/** Planet mit Schwerkraft-Feld: Ringe zeigen, wie weit er zieht (mehr Masse → größeres Feld). */
private fun DrawScope.drawPlanet(o: Offset, r: Float, mass: Double, seed: Int, t: Float) {
    val colors = listOf(Color(0xFF7C4DFF) to Color(0xFF3D1A99), Color(0xFFFF8A3D) to Color(0xFF8A3300), Color(0xFF26C6DA) to Color(0xFF00616D))
    val (c1, c2) = colors[seed % colors.size]
    val field = (kotlin.math.sqrt(mass) * 0.75).toFloat() * r / 1.4f
    // Feldlinien: nach innen wandernde Ringe
    for (k in 0 until 3) {
        val ph = ((t * 0.35f + k / 3f) % 1f)
        val rr = r + (field * 2.2f) * (1 - ph)
        drawCircle(c1.copy(alpha = 0.25f * ph), rr, o, style = Stroke(2.5f))
    }
    drawCircle(Brush.radialGradient(listOf(c1.copy(alpha = 0.35f), Color.Transparent), o, r * 2.2f), r * 2.2f, o)
    drawCircle(Brush.radialGradient(listOf(c1, c2), o - Offset(r * 0.3f, r * 0.3f), r * 1.6f), r, o)
    drawArc(Color.White.copy(alpha = 0.15f), 200f, 80f, false, o - Offset(r * 0.75f, r * 0.75f), Size(r * 1.5f, r * 1.5f), style = Stroke(r * 0.12f))
}

private fun DrawScope.drawPathLine(path: List<Vec>, px: (Vec) -> Offset, color: Color, width: Float, dashed: Boolean) {
    val p = Path()
    path.forEachIndexed { i, v -> val o = px(v); if (i == 0) p.moveTo(o.x, o.y) else p.lineTo(o.x, o.y) }
    drawPath(p, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round,
        pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(width * 2.5f, width * 2f)) else null))
}

private fun DrawScope.drawStation(o: Offset, r: Float, hit: Boolean, t: Float) {
    val c = if (hit) Q.Green else Q.Pink
    val pulse = 1f + 0.08f * sin(t * 3f)
    drawCircle(Brush.radialGradient(listOf(c.copy(alpha = 0.45f), Color.Transparent), o, r * 2.2f), r * 2.2f, o)
    drawCircle(c, r * 0.62f * pulse, o)
    drawCircle(Color.White.copy(alpha = 0.6f), r * 0.25f, o + Offset(-r * 0.2f, -r * 0.2f))
    rotate(-20f, o) { drawOval(c, o - Offset(r * 1.15f, r * 0.28f), Size(r * 2.3f, r * 0.56f), style = Stroke(r * 0.14f)) }
}

private fun DrawScope.drawAsteroid(o: Offset, r: Float, seed: Int, rock: Boolean) {
    val base = if (rock) Color(0xFF6D5B4E) else Color(0xFF7A6F66)
    val p = Path()
    val n = 11
    for (i in 0 until n) {
        val a = i * 6.283f / n
        val k = 0.85f + 0.15f * sin(a * 3f + seed * 1.7f)
        val q = o + Offset(cos(a) * r * k, sin(a) * r * k)
        if (i == 0) p.moveTo(q.x, q.y) else p.lineTo(q.x, q.y)
    }
    p.close()
    drawPath(p, base)
    drawPath(p, Color(0xFFFF6B4A).copy(alpha = 0.35f), style = Stroke(r * 0.08f))
    drawCircle(Color.Black.copy(alpha = 0.25f), r * 0.22f, o + Offset(-r * 0.3f, -r * 0.2f))
    drawCircle(Color.Black.copy(alpha = 0.2f), r * 0.15f, o + Offset(r * 0.35f, r * 0.3f))
}

private fun DrawScope.drawBolt(o: Offset, s: Float) {
    val p = Path().apply {
        moveTo(o.x + s * 0.15f, o.y - s); lineTo(o.x - s * 0.5f, o.y + s * 0.1f); lineTo(o.x - s * 0.02f, o.y + s * 0.1f)
        lineTo(o.x - s * 0.2f, o.y + s); lineTo(o.x + s * 0.5f, o.y - s * 0.15f); lineTo(o.x + s * 0.02f, o.y - s * 0.15f); close()
    }
    drawPath(p, Q.Yellow)
}

private fun DrawScope.drawProbe(o: Offset, unit: Float, deg: Float, color: Color, t: Float) {
    rotate(deg, o) {
        val flame = 0.7f + 0.3f * sin(t * 40f)
        drawCircle(Q.Orange.copy(alpha = 0.8f), unit * 0.3f * flame, o - Offset(unit * 0.55f, 0f))
        drawCircle(Q.Yellow, unit * 0.16f * flame, o - Offset(unit * 0.5f, 0f))
        val p = Path().apply {
            moveTo(o.x + unit * 0.6f, o.y); lineTo(o.x - unit * 0.4f, o.y - unit * 0.35f)
            lineTo(o.x - unit * 0.25f, o.y); lineTo(o.x - unit * 0.4f, o.y + unit * 0.35f); close()
        }
        drawPath(p, color)
        drawCircle(Color.White, unit * 0.1f, o + Offset(unit * 0.12f, 0f))
    }
}
