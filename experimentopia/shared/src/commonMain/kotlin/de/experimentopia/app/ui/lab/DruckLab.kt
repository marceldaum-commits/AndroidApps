package de.experimentopia.app.ui.lab

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import kotlin.math.max
import kotlin.math.min

/*
 * Druck-Werkstatt: Nagelbrett, Tauchtiefe, Hebebühne und Magdeburger Halbkugeln.
 * Alle Zahlen kommen aus [Druck]; unter dem Gerät steht die Rechnung mit den eingestellten Werten.
 */
private val Board3 = Color(0xFF0B0820)
private val Water = Color(0xFF1E6FD9)
private fun num(v: Double, d: Int = 1) = Fmt.num(v, d)

@Composable
fun PressureLab(s: PressureSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current; val tm = rememberTextMeasurer()
    var nails by remember { mutableIntStateOf(if (s.mode == PressureMode.NAILS) 100 else 500) }
    var depth by remember { mutableDoubleStateOf(0.0) }
    var area2 by remember { mutableDoubleStateOf(0.02) }
    var radiusCm by remember { mutableDoubleStateOf(2.0) }
    val pNail = Druck.nailPressure(nails)
    val pTotal = Druck.total(depth)
    val f2 = Druck.hydraulicForce(s.f1, s.a1, area2)
    val fHemi = Druck.hemisphereForce(radiusCm / 100)
    val hit = when (s.mode) {
        PressureMode.NAILS -> s.maxKPa?.let { pNail <= it * 1000 } == true
        PressureMode.DIVE -> s.targetBar?.let { PressureUi.hit(pTotal / Druck.BAR, it) } == true
        PressureMode.HYDRAULIC -> s.minForceN?.let { f2 >= it - 1e-6 } == true
        PressureMode.HEMISPHERE -> s.minForceN?.let { fHemi >= it } == true
    }
    LaunchedEffect(hit) { if (hit) sfx?.play(Sound.STAR) }
    GoalWhen(!free && hit, onGoal)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(220.dp).testTag("druck")) {
            val w = size.width; val h = size.height
            drawRoundRect(Board3, Offset.Zero, size, CornerRadius(22f * density))
            when (s.mode) {
                PressureMode.NAILS -> {
                    val y0 = h * 0.62f; val n = min(40, max(3, nails / 25))
                    drawRoundRect(Color(0xFF8B5A2B), Offset(w * 0.12f, y0 + 6f * density), Size(w * 0.76f, 18f * density), CornerRadius(6f * density))
                    for (i in 0 until n) { val x = w * 0.14f + (w * 0.72f) * (i + 0.5f) / n; drawLine(Color(0xFFC9CFD9), Offset(x, y0 + 6f * density), Offset(x, y0 - 12f * density), 2f * density) }
                    label(tm, "🛌", Offset(w * 0.5f, y0 - 46f * density), 44.sp)
                    label(tm, "${nails} Nägel · ${num(pNail / 1000, 0)} kPa", Offset(w * 0.5f, h * 0.9f), 15.sp, if (hit) E.Green else E.Mint, true)
                }
                PressureMode.DIVE -> {
                    val top = h * 0.12f; val bottom = h * 0.92f; val frac = (depth / 100.0).toFloat().coerceIn(0f, 1f)
                    drawRect(Water.copy(alpha = 0.35f + 0.4f * frac), Offset(w * 0.1f, top), Size(w * 0.5f, bottom - top))
                    drawLine(Color.White, Offset(w * 0.1f, top), Offset(w * 0.6f, top), 2f * density)
                    var m = 0; while (m <= 100) { val y = top + (bottom - top) * m / 100f; drawLine(Color(0x33FFFFFF), Offset(w * 0.1f, y), Offset(w * 0.14f, y), 1f * density); if (m % 20 == 0) label(tm, "$m m", Offset(w * 0.07f, y), 9.sp, E.Muted); m += 10 }
                    label(tm, "🤿", Offset(w * 0.35f, top + (bottom - top) * frac), 30.sp)
                    // Druckanzeige
                    val bars = (pTotal / Druck.BAR).toFloat(); val maxBar = 11f
                    drawRoundRect(Color(0xFF1B1747), Offset(w * 0.72f, top), Size(w * 0.14f, bottom - top), CornerRadius(8f * density))
                    drawRoundRect(if (hit) E.Green else E.Orange, Offset(w * 0.72f, bottom - (bottom - top) * (bars / maxBar).coerceIn(0f, 1f)), Size(w * 0.14f, (bottom - top) * (bars / maxBar).coerceIn(0f, 1f)), CornerRadius(8f * density))
                    label(tm, "${num(bars.toDouble(), 1)} bar", Offset(w * 0.79f, top - 4f * density), 13.sp, E.Sun, true)
                }
                PressureMode.HYDRAULIC -> {
                    val a1w = w * 0.12f; val a2w = (w * 0.12f + (w * 0.5f - w * 0.12f) * (area2.toFloat() / 0.30f))
                    val base = h * 0.78f; val x1 = w * 0.18f; val x2 = w * 0.62f
                    drawRect(Water.copy(alpha = 0.55f), Offset(x1 - a1w / 2, base - 8f * density), Size(x2 + a2w / 2 - (x1 - a1w / 2), 8f * density))
                    drawRect(Color(0xFFC9CFD9), Offset(x1 - a1w / 2, base - 28f * density), Size(a1w, 16f * density))
                    drawRect(Color(0xFFC9CFD9), Offset(x2 - a2w / 2, base - 28f * density), Size(a2w, 16f * density))
                    label(tm, "👇", Offset(x1, base - 56f * density), 26.sp); label(tm, "${num(s.f1, 0)} N", Offset(x1, base - 88f * density), 12.sp, E.Sun, true)
                    label(tm, "🚗", Offset(x2, base - 56f * density), 30.sp); label(tm, "${num(f2, 0)} N", Offset(x2, base - 88f * density), 12.sp, if (hit) E.Green else E.Mint, true)
                }
                PressureMode.HEMISPHERE -> {
                    val r = (radiusCm / 20.0).toFloat().coerceIn(0.1f, 1f) * h * 0.30f; val cx = w * 0.5f; val cy = h * 0.5f
                    drawArc(Color(0xFF8F95A8), 90f, 180f, true, Offset(cx - r, cy - r), Size(2 * r, 2 * r))
                    drawArc(Color(0xFFB4BAC8), -90f, 180f, true, Offset(cx - r, cy - r), Size(2 * r, 2 * r))
                    drawLine(E.Pink, Offset(cx - r - 40f * density, cy), Offset(cx - r - 8f * density, cy), 3f * density); drawLine(E.Pink, Offset(cx + r + 8f * density, cy), Offset(cx + r + 40f * density, cy), 3f * density)
                    label(tm, "🐴", Offset(cx - r - 58f * density, cy), 24.sp); label(tm, "🐴", Offset(cx + r + 58f * density, cy), 24.sp)
                    label(tm, "${num(fHemi, 0)} N", Offset(cx, h * 0.9f), 15.sp, if (hit) E.Green else E.Mint, true)
                }
            }
        }
        val shown = when (s.mode) { PressureMode.NAILS -> Fmt.unit(pNail / 1000, "kPa", 0); PressureMode.DIVE -> Fmt.unit(pTotal / Druck.BAR, "bar", 2); PressureMode.HYDRAULIC -> Fmt.unit(f2, "N", 0); PressureMode.HEMISPHERE -> Fmt.unit(fHemi, "N", 0) }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            when (s.mode) {
                PressureMode.NAILS -> { Readout("Nägel", "$nails", E.Orange, Modifier.weight(1f)); Readout("Fläche", Fmt.unit(nails * Druck.NAIL_TIP_M2 * 1e6, "mm²", 0), E.Sun, Modifier.weight(1f)) }
                PressureMode.DIVE -> { Readout("Tiefe", Fmt.unit(depth, "m", 0), E.Sky, Modifier.weight(1f)); Readout("Wasser", Fmt.unit(Druck.waterColumn(depth) / Druck.BAR, "bar", 2), E.Sun, Modifier.weight(1f)) }
                PressureMode.HYDRAULIC -> { Readout("A₁", Fmt.unit(s.a1, "m²", 3), E.Orange, Modifier.weight(1f)); Readout("A₂", Fmt.unit(area2, "m²", 2), E.Sun, Modifier.weight(1f)) }
                PressureMode.HEMISPHERE -> { Readout("Radius", Fmt.unit(radiusCm, "cm", 0), E.Orange, Modifier.weight(1f)); Readout("Fläche", Fmt.unit(PI * (radiusCm / 100) * (radiusCm / 100), "m²", 4), E.Sun, Modifier.weight(1f)) }
            }
            Readout(if (s.mode == PressureMode.NAILS || s.mode == PressureMode.DIVE) "Druck" else "Kraft", shown, E.Mint, Modifier.weight(1f))
        }
        s.maxKPa?.let { Text("🎯 Ziel: höchstens ${num(it, 0)} kPa", color = E.Sun, fontSize = 13.sp) }
        s.targetBar?.let { Text("🎯 Ziel: ${num(it, 1)} bar Gesamtdruck (± 3 %)", color = E.Sun, fontSize = 13.sp) }
        s.minForceN?.let { Text("🎯 Ziel: mindestens ${num(it, 0)} N", color = E.Sun, fontSize = 13.sp) }
        when (s.mode) {
            PressureMode.NAILS -> ValueSlider("🛏️ Anzahl der Nägel", PressureUi.NAILS.indexOf(nails).coerceAtLeast(0).toFloat(), 0f..(PressureUi.NAILS.size - 1).toFloat(), "$nails", { nails = PressureUi.NAILS[roundI(it).coerceIn(PressureUi.NAILS.indices)] }, PressureUi.NAILS.size - 2, E.Orange, Modifier.testTag("naegel"))
            PressureMode.DIVE -> ValueSlider("🤿 Tauchtiefe", depth.toFloat(), 0f..100f, "${num(depth, 0)} m", { depth = roundI(it).toDouble() }, 99, E.Sky, Modifier.testTag("tiefe"))
            PressureMode.HYDRAULIC -> ValueSlider("🚗 Fläche des großen Kolbens", PressureUi.AREAS.indexOfFirst { kotlin.math.abs(it - area2) < 1e-9 }.coerceAtLeast(0).toFloat(), 0f..(PressureUi.AREAS.size - 1).toFloat(), "${num(area2, 2)} m²", { area2 = PressureUi.AREAS[roundI(it).coerceIn(PressureUi.AREAS.indices)] }, PressureUi.AREAS.size - 2, E.Sky, Modifier.testTag("kolben"))
            PressureMode.HEMISPHERE -> ValueSlider("⚫ Radius der Halbkugeln", (radiusCm - 1).toFloat(), 0f..19f, "${num(radiusCm, 0)} cm", { radiusCm = roundI(it).toDouble() + 1 }, 18, E.Sky, Modifier.testTag("radius"))
        }
        Text(when (s.mode) {
            PressureMode.NAILS -> "Druck: p = F ÷ A = ${num(Druck.weight(Druck.PERSON_KG), 1)} N ÷ (${nails} · 4·10⁻⁶ m²) = ${num(pNail, 0)} Pa = ${num(pNail / 1000, 0)} kPa"
            PressureMode.DIVE -> "Gesamtdruck: p = p_Luft + ρ · g · h = 101 325 Pa + 1000 · 9,81 · ${num(depth, 0)} m = ${num(pTotal, 0)} Pa = ${num(pTotal / Druck.BAR, 2)} bar"
            PressureMode.HYDRAULIC -> "Hydraulik: F₂ = F₁ · A₂ ÷ A₁ = ${num(s.f1, 0)} N · ${num(area2, 2)} m² ÷ ${num(s.a1, 3)} m² = ${num(f2, 0)} N"
            PressureMode.HEMISPHERE -> "Luftdruck: F = p · π · r² = 101 325 Pa · π · (${num(radiusCm / 100, 2)} m)² = ${num(fHemi, 0)} N"
        }, color = E.Text, fontSize = 13.sp, modifier = Modifier.testTag("rechnung"))
        if (!free) Text(if (hit) "🎯 Geschafft!" else when (s.mode) { PressureMode.NAILS -> "Noch zu spitz – mehr Nägel."; PressureMode.DIVE -> "Noch nicht ganz – tiefer oder flacher tauchen."; PressureMode.HYDRAULIC -> "Noch zu schwach – größerer Kolben."; PressureMode.HEMISPHERE -> "Noch zu schwach – größere Halbkugeln." }, color = if (hit) E.Green else E.Muted, fontSize = 13.sp)
        if (free) Panel(border = E.Sun) {
            Text("Eigene Werte", fontWeight = FontWeight.Bold, color = E.Sun)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (s.mode) {
                    PressureMode.NAILS -> NumberField("Nägel", "", 1.0, 5000.0, nails.toDouble(), { nails = it.toInt() }, Modifier.weight(1f))
                    PressureMode.DIVE -> NumberField("Tiefe", "m", 0.0, 100.0, depth, { depth = it }, Modifier.weight(1f))
                    PressureMode.HYDRAULIC -> NumberField("A₂", "m²", 0.01, 1.0, area2, { area2 = it }, Modifier.weight(1f))
                    PressureMode.HEMISPHERE -> NumberField("Radius", "cm", 1.0, 50.0, radiusCm, { radiusCm = it }, Modifier.weight(1f))
                }
            }
        }
    }
}
