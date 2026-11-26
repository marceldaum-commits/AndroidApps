package de.experimentopia.app.ui.lab

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.experimentopia.app.ui.common.*
import de.experimentopia.app.ui.fx.*
import de.experimentopia.app.ui.theme.E
import de.experimentopia.core.model.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * Achterbahn-Werkstatt: Starthöhe, Masse und Reibung einstellen; der Wagen fährt los.
 * Alle Zahlen kommen aus [Energie]; unter dem Gerät steht die Rechnung mit den eingestellten Werten.
 */
private val Rail = Color(0xFFFFB02E)
private val Board2 = Color(0xFF0B0820)

private fun num(v: Double, d: Int = 1) = Fmt.num(v, d)

/** Die Strecke in Weltkoordinaten (x in Metern, y = Höhe in Metern): Hügel, Tal, dann Looping bzw. zweiter Hügel bzw. ebene Strecke */
/** [k] = Meter in y-Richtung je Meter in x-Richtung auf dem Bildschirm (damit der Looping rund aussieht) */
private fun track(mode: CoasterMode, startM: Double, k: Double): List<Pair<Double, Double>> {
    val pts = mutableListOf<Pair<Double, Double>>()
    val slope = 28.0
    for (i in 0..40) { val x = slope * i / 40; pts += x to startM * (1 + cos(PI * x / slope)) / 2 }
    when (mode) {
        CoasterMode.LOOP -> {
            val cx = 44.0; val r = Energie.LOOP_RADIUS_M
            for (x in 29..(cx - r * k - 1).toInt()) pts += x.toDouble() to 0.0
            for (i in 0..60) { val a = 2 * PI * i / 60; pts += (cx + r * sin(a) * k) to (r - r * cos(a)) }   // Schleife leicht zusammengedrückt, damit sie ins Bild passt
            for (x in (cx + r * k + 1).toInt()..60) pts += x.toDouble() to 0.0
        }
        CoasterMode.HILL -> {
            for (x in 29..35) pts += x.toDouble() to 0.0
            for (i in 0..24) { val x = 36.0 + 16.0 * i / 24; pts += x to Energie.HILL2_M * (1 + cos(PI * (x - 44.0) / 8.0)) / 2 }
            for (x in 53..60) pts += x.toDouble() to 0.0
        }
        else -> for (x in 29..60) pts += x.toDouble() to 0.0
    }
    return pts
}

@Composable
fun CoasterLab(s: CoasterSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current; val tm = rememberTextMeasurer()
    var h by remember { mutableDoubleStateOf(s.startM) }
    var mass by remember { mutableDoubleStateOf(s.massKg) }
    var fr by remember { mutableIntStateOf(s.frictionPct) }
    var pos by remember { mutableDoubleStateOf(s.startM) }                 // SPLIT: Höhe des Wagens auf der Strecke
    val f = fr / 100.0
    val vMs = Energie.speed(h, f); val vKmh = Energie.toKmh(vMs)
    val ePot = Energie.potential(mass, h)
    val loopOk = Energie.passesLoop(h, friction = f); val hillOk = Energie.passesHill(h, friction = f)
    val splitShare = Energie.kineticShare(s.startM, pos)
    val hit = when (s.mode) {
        CoasterMode.ENERGY -> s.minEnergyKJ?.let { ePot / 1000 >= it } == true
        CoasterMode.SPEED -> s.targetKmh?.let { CoasterUi.hit(vKmh, it) } == true
        CoasterMode.LOOP -> loopOk && (s.maxStartM?.let { h <= it } ?: true)
        CoasterMode.HILL -> hillOk && (s.maxStartM?.let { h <= it } ?: true)
        CoasterMode.SPLIT -> kotlin.math.abs(splitShare - 0.5) <= CoasterUi.SPLIT_TOLERANCE
    }
    LaunchedEffect(hit) { if (hit) sfx?.play(Sound.STAR) }
    GoalWhen(!free && hit, onGoal)

    // Fahrt: 0 = oben, 1 = Ende der Strecke (bei Misserfolg bleibt der Wagen davor stehen)
    val prog = remember { Animatable(0f) }
    var run by remember { mutableIntStateOf(0) }
    val shownH = if (s.mode == CoasterMode.SPLIT) s.startM else h
    val fails = (s.mode == CoasterMode.LOOP && !loopOk) || (s.mode == CoasterMode.HILL && !hillOk)
    LaunchedEffect(run) { if (run > 0) { prog.snapTo(0f); prog.animateTo(if (fails) 0.62f else 1f, tween(if (fails) 2200 else 4200, easing = LinearEasing)) } }
    LaunchedEffect(s.mode, shownH) { prog.snapTo(0f) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(240.dp).testTag("achterbahn")) {
            val w = size.width; val ht = size.height
            drawRoundRect(Board2, Offset.Zero, size, CornerRadius(22f * density))
            val maxH = max(15.0, max(shownH, if (s.mode == CoasterMode.HILL) Energie.HILL2_M else 0.0)) * 1.12
            val sx = (w * 0.92f) / 60f; val sy = (ht * 0.74f) / maxH.toFloat()
            val x0 = w * 0.04f; val y0 = ht * 0.86f
            val pts = track(s.mode, shownH, (sy / sx).toDouble())
            fun px(p: Pair<Double, Double>) = Offset(x0 + p.first.toFloat() * sx, y0 - p.second.toFloat() * sy)
            // Höhenlinien alle 10 m
            var g = 10.0
            while (g < maxH) { drawLine(Color(0x22FFFFFF), Offset(x0, y0 - g.toFloat() * sy), Offset(w - x0, y0 - g.toFloat() * sy), 1f * density); label(tm, "${num(g, 0)} m", Offset(w - x0 - 18f * density, y0 - g.toFloat() * sy - 8f * density), 9.sp, E.Muted); g += 10.0 }
            val path = Path().apply { pts.forEachIndexed { i, p -> val o = px(p); if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }
            drawPath(path, Rail, style = Stroke(4f * density, cap = StrokeCap.Round))
            drawLine(Color(0xFF3B3470), Offset(x0, y0), Offset(w - x0, y0), 3f * density)
            // Wagen entlang der Strecke (nach Bogenlänge)
            val total = (1 until pts.size).sumOf { val a = pts[it - 1]; val b = pts[it]; sqrt((b.first - a.first) * (b.first - a.first) + (b.second - a.second) * (b.second - a.second)) }
            var want = total * prog.value; var idx = 1
            while (idx < pts.size - 1) { val a = pts[idx - 1]; val b = pts[idx]; val d = sqrt((b.first - a.first) * (b.first - a.first) + (b.second - a.second) * (b.second - a.second)); if (want <= d) break; want -= d; idx++ }
            val a = pts[idx - 1]; val b = pts[idx]; val d = max(1e-9, sqrt((b.first - a.first) * (b.first - a.first) + (b.second - a.second) * (b.second - a.second))); val t = (want / d).coerceIn(0.0, 1.0)
            val here = if (s.mode == CoasterMode.SPLIT) pts.minByOrNull { kotlin.math.abs(it.second - pos) + it.first * 0.001 } ?: a else (a.first + (b.first - a.first) * t) to (a.second + (b.second - a.second) * t)
            val wo = px(here); label(tm, "🎢", Offset(wo.x, wo.y - 14f * density), 24.sp)
            label(tm, "Start ${num(shownH, 0)} m", Offset(x0 + 90f * density, y0 - shownH.toFloat() * sy - 12f * density), 11.sp, E.Sun, true)
            if (s.mode == CoasterMode.LOOP) label(tm, if (loopOk) "🔄 klappt" else "🔄 zu wenig Schwung", Offset(px(44.0 to 12.0).x, px(44.0 to 12.0).y), 11.sp, if (loopOk) E.Green else E.Pink, true)
            if (s.mode == CoasterMode.HILL) label(tm, if (hillOk) "⛰️ drüber" else "⛰️ bleibt stecken", Offset(px(44.0 to Energie.HILL2_M).x, px(44.0 to Energie.HILL2_M).y - 14f * density), 11.sp, if (hillOk) E.Green else E.Pink, true)
        }
        // Energie-Balken: Lage / Bewegung / Wärme
        val total = Energie.potential(mass, if (s.mode == CoasterMode.SPLIT) s.startM else h)
        val kinShare = if (s.mode == CoasterMode.SPLIT) splitShare else (1 - f)
        val potShare = if (s.mode == CoasterMode.SPLIT) 1 - splitShare else 0.0
        val heatShare = if (s.mode == CoasterMode.SPLIT) 0.0 else f
        Canvas(Modifier.fillMaxWidth().height(34.dp).testTag("energiebalken")) {
            val w = size.width; var x = 0f
            fun seg(share: Double, c: Color) { val sw = (w * share).toFloat(); if (sw > 0f) drawRoundRect(c, Offset(x, 0f), Size(sw, size.height), CornerRadius(8f * density)); x += sw }
            seg(potShare, E.Sky); seg(kinShare, E.Green); seg(heatShare, E.Pink)
        }
        Text(if (s.mode == CoasterMode.SPLIT) "🟦 Lageenergie ${num(potShare * 100, 0)} %   🟩 Bewegungsenergie ${num(kinShare * 100, 0)} %" else "🟩 Bewegungsenergie unten ${num(kinShare * 100, 0)} %   🟥 Wärme durch Reibung ${num(heatShare * 100, 0)} %", color = E.Muted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Readout("Höhe", Fmt.unit(if (s.mode == CoasterMode.SPLIT) pos else h, "m", 1), E.Sky, Modifier.weight(1f))
            Readout("Lageenergie", Fmt.unit((if (s.mode == CoasterMode.SPLIT) Energie.potential(mass, pos) else ePot) / 1000, "kJ", 1), E.Orange, Modifier.weight(1f))
            Readout(if (s.mode == CoasterMode.SPLIT) "Tempo hier" else "Tempo unten", Fmt.unit(if (s.mode == CoasterMode.SPLIT) Energie.toKmh(Energie.speed(s.startM - pos)) else vKmh, "km/h", 0), E.Mint, Modifier.weight(1f))
        }
        s.minEnergyKJ?.let { Text("🎯 Ziel: mindestens ${num(it, 0)} kJ Lageenergie", color = E.Sun, fontSize = 13.sp) }
        s.targetKmh?.let { Text("🎯 Ziel: ${num(it, 0)} km/h unten (± 3 %)", color = E.Sun, fontSize = 13.sp) }
        s.maxStartM?.let { Text("🎯 Starthöhe höchstens ${num(it, 0)} m", color = E.Sun, fontSize = 13.sp) }
        if (s.mode != CoasterMode.SPLIT && (s.adjustStart || free)) ValueSlider("⛰️ Starthöhe", CoasterUi.HEIGHTS.indexOf(h.coerceIn(1.0, 60.0).let { kotlin.math.floor(it) }).coerceAtLeast(0).toFloat(), 0f..(CoasterUi.HEIGHTS.size - 1).toFloat(), "${num(h, 0)} m", { h = CoasterUi.HEIGHTS[roundI(it).toInt().coerceIn(CoasterUi.HEIGHTS.indices)] }, CoasterUi.HEIGHTS.size - 2, E.Sky, Modifier.testTag("hoehe"))
        if (s.mode == CoasterMode.SPLIT) ValueSlider("📍 Stelle auf der Strecke (Höhe)", pos.toFloat(), 0f..s.startM.toFloat(), "${num(pos, 0)} m", { pos = roundI(it).toDouble() }, s.startM.toInt() - 1, E.Sky, Modifier.testTag("stelle"))
        if (s.adjustMass || free) ValueSlider("⚖️ Masse des Wagens", CoasterUi.MASSES.indexOf(mass.coerceIn(100.0, 1000.0)).coerceAtLeast(0).toFloat(), 0f..(CoasterUi.MASSES.size - 1).toFloat(), "${num(mass, 0)} kg", { mass = CoasterUi.MASSES[roundI(it).toInt().coerceIn(CoasterUi.MASSES.indices)] }, CoasterUi.MASSES.size - 2, E.Orange, Modifier.testTag("masse"))
        if (s.adjustFriction || free) ChipRow(CoasterUi.FRICTIONS, fr, { if (it == 0) "ohne Reibung" else "$it % Reibung" }, { fr = it }, Modifier.testTag("reibung"), color = E.Pink)
        if (s.mode != CoasterMode.SPLIT) GlowButton({ run++; sfx?.play(Sound.WHOOSH) }, Modifier.fillMaxWidth().pulse(prog.value == 0f), color = E.Mint) { Text("🎢 Los geht's!", fontWeight = FontWeight.Bold) }
        // Rechnung mit den eingestellten Werten
        Text("Lageenergie: E = m · g · h = ${num(mass, 0)} kg · 9,81 m/s² · ${num(if (s.mode == CoasterMode.SPLIT) pos else h, 0)} m = ${num((if (s.mode == CoasterMode.SPLIT) Energie.potential(mass, pos) else ePot), 0)} J", color = E.Text, fontSize = 13.sp, modifier = Modifier.testTag("rechnung"))
        if (s.mode != CoasterMode.SPLIT) Text("Tempo unten: v = √(2 · g · h · (1 − ${num(f, 2)})) = √(2 · 9,81 · ${num(h, 0)} · ${num(1 - f, 2)}) = ${num(vMs, 1)} m/s = ${num(vKmh, 0)} km/h", color = E.Text, fontSize = 13.sp)
        if (s.mode == CoasterMode.LOOP) Text("Looping (R = ${num(Energie.LOOP_RADIUS_M, 0)} m): nötig h · (1 − r) ≥ 2,5 · R = ${num(2.5 * Energie.LOOP_RADIUS_M, 1)} m; hier ${num(h * (1 - f), 1)} m", color = E.Text, fontSize = 13.sp)
        if (s.mode == CoasterMode.HILL) Text("Hügel (${num(Energie.HILL2_M, 0)} m): nötig h · (1 − r) ≥ ${num(Energie.HILL2_M, 0)} m; hier ${num(h * (1 - f), 1)} m", color = E.Text, fontSize = 13.sp)
        if (s.mode == CoasterMode.SPLIT) Text("Bewegungsanteil = (H − h) ÷ H = (${num(s.startM, 0)} − ${num(pos, 0)}) ÷ ${num(s.startM, 0)} = ${num(splitShare * 100, 0)} %", color = E.Text, fontSize = 13.sp)
        if (!free) Text(when { hit -> "🎯 Geschafft!"; s.mode == CoasterMode.LOOP -> "Noch kein Looping – höher starten, bei Reibung noch höher."; s.mode == CoasterMode.HILL -> "Der Wagen bleibt stecken – höher starten."; s.mode == CoasterMode.SPLIT -> "Such die Stelle, an der beide Anteile etwa gleich groß sind."; else -> "Noch nicht ganz – Höhe verändern." }, color = if (hit) E.Green else E.Muted, fontSize = 13.sp)
        if (free) Panel(border = E.Sun) {
            Text("Eigene Werte", fontWeight = FontWeight.Bold, color = E.Sun)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Starthöhe", "m", 1.0, 100.0, h, { h = it }, Modifier.weight(1f))
                NumberField("Masse", "kg", 10.0, 5000.0, mass, { mass = it }, Modifier.weight(1f))
            }
        }
    }
}
