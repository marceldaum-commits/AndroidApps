package de.experimentopia.app.ui.lab

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.experimentopia.app.ui.common.*
import de.experimentopia.app.ui.fx.*
import de.experimentopia.app.ui.theme.E
import de.experimentopia.core.model.*
import kotlin.math.abs

/** Lage aller Plätze und Leitungen im Bild */
private class Geo(val centers: List<Offset>, val rows: List<Triple<Offset, Offset, List<Int>>>, val loop: List<Pair<Offset, Offset>>, val battery: Offset, val batHalf: Float)

private fun geometry(layout: Layout, w: Float, h: Float): Geo {
    val l = w * 0.09f; val r = w * 0.95f; val t = h * 0.16f; val b = h * 0.9f
    val maxRows = layout.columns.maxOf { it.size }
    val gap = if (maxRows > 1) (b - t) * 0.62f / (maxRows - 1) else 0f
    val pad = w * 0.04f
    val colW = (r - l - 2 * pad) / layout.columns.size
    val centers = MutableList(layout.slots.size) { Offset.Zero }
    val rows = mutableListOf<Triple<Offset, Offset, List<Int>>>()
    val loop = mutableListOf<Pair<Offset, Offset>>()
    val batY = (t + b) / 2; val batHalf = h * 0.09f
    loop += Offset(l, t) to Offset(l, batY - batHalf)
    loop += Offset(l, batY + batHalf) to Offset(l, b)
    loop += Offset(l, b) to Offset(r, b)
    loop += Offset(r, b) to Offset(r, t)
    loop += Offset(l, t) to Offset(l + pad, t)
    loop += Offset(r - pad, t) to Offset(r, t)
    layout.columns.forEachIndexed { i, col ->
        val x0 = l + pad + i * colW; val x1 = x0 + colW
        col.forEachIndexed { j, branch ->
            val y = t + j * gap
            rows += Triple(Offset(x0, y), Offset(x1, y), branch)
            branch.forEachIndexed { k, s -> centers[s] = Offset(x0 + colW * (k + 0.5f) / branch.size, y) }
        }
        if (col.size > 1) { loop += Offset(x0, t) to Offset(x0, t + (col.size - 1) * gap); loop += Offset(x1, t) to Offset(x1, t + (col.size - 1) * gap) }
    }
    return Geo(centers, rows, loop, Offset(l, batY), batHalf)
}

@Composable
fun CircuitLab(s: CircuitSetup, free: Boolean, onGoal: () -> Unit) {
    val now by rememberSeconds()
    val sfx = LocalSfx.current
    val view = LocalBuzzer.current; val vib = LocalVibration.current
    val tm = rememberTextMeasurer()
    var layout by remember { mutableStateOf(s.layout) }
    val slots = remember { mutableStateListOf(*s.start.toTypedArray()) }
    var volts by remember { mutableDoubleStateOf(s.volts) }
    var pick by remember { mutableStateOf(s.tray.firstOrNull()) }
    var editR by remember { mutableStateOf(s.adjustable) }
    val tested = remember { mutableStateListOf<Part>() }
    val combos = remember { mutableStateListOf<Pair<Boolean, Boolean>>() }
    var removed by remember { mutableStateOf(false) }
    var measured by remember { mutableStateOf(false) }
    var hadShort by remember { mutableStateOf(false) }
    val bursts = rememberBursts()
    val res = Circuits.solve(layout, slots, volts)
    LaunchedEffect(res.short) { if (res.short) { hadShort = true; sfx?.play(Sound.ZAP); view.buzz(Buzz.BIG, vib) } }
    LaunchedEffect(slots.toList()) {
        val sw = slots.filter { it.part.isSwitch }
        if (sw.size >= 2) Pair(sw[0].part == Part.SCHALTER_ZU, sw[1].part == Part.SCHALTER_ZU).let { if (it !in combos) combos += it }
        if (slots.indices.any { Circuits.glows(Circuits.solve(layout, slots, volts), slots, it) }) sfx?.play(Sound.DING, volume = 0.4f)
    }
    val goal = when (s.goal) {
        CircuitGoal.TEST_ALL -> s.tray.all { it in tested }
        CircuitGoal.BOTH_WAYS -> combos.size >= 4
        CircuitGoal.REMOVE_ONE -> removed
        CircuitGoal.SHORT -> hadShort
        CircuitGoal.AMPS -> abs(res.totalA - (s.targetA ?: 0.0)) <= 0.005
        CircuitGoal.MEASURE -> measured
        CircuitGoal.LIGHT -> slots.indices.any { Circuits.glows(res, slots, it) }
    }
    GoalWhen(!free && goal, onGoal)

    fun tapSlot(i: Int) {
        val p = slots[i].part
        when {
            p.isSwitch -> { slots[i] = Slot(p.toggled()); sfx?.play(Sound.SWITCH); view.buzz(Buzz.TICK, vib) }
            !free && s.goal == CircuitGoal.REMOVE_ONE && p == Part.LAMPE -> { slots[i] = Slot(Part.LEER); removed = true; sfx?.play(Sound.POP) }
            !free && s.goal == CircuitGoal.REMOVE_ONE && p == Part.LEER -> { slots[i] = Slot(Part.LAMPE); sfx?.play(Sound.CLANK) }
            p == Part.WIDERSTAND && (free || s.adjustable == i) -> { editR = i; sfx?.play(Sound.TAP) }
            i in s.locked && !free -> sfx?.play(Sound.SOFT, volume = 0.4f)
            pick != null -> {
                val part = pick!!
                slots[i] = Slot(part, if (part == Part.WIDERSTAND) 20.0 else part.ohm)
                if (part.isMaterial && part !in tested) tested += part
                if (part == Part.WIDERSTAND) editR = i
                sfx?.play(Sound.CLANK); view.buzz(Buzz.TICK, vib)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(260.dp).testTag("stromkreis").pointerInput(layout, pick, slots.size) {
            detectTapGestures { p ->
                val g = geometry(layout, size.width.toFloat(), size.height.toFloat())
                val hit = g.centers.indices.minByOrNull { (g.centers[it] - p).getDistance() }
                if (hit != null && (g.centers[hit] - p).getDistance() < 46.dp.toPx()) tapSlot(hit)
            }
        }) {
            val g = geometry(layout, size.width, size.height)
            val wire = Color(0xFFCFC6FF)
            g.loop.forEach { (a, b) -> drawLine(wire, a, b, 3f * density, StrokeCap.Round) }
            g.rows.forEach { (a, b, _) -> drawLine(wire, a, b, 3f * density, StrokeCap.Round) }
            // Elektronen: Punkte, schneller bei mehr Strom
            fun dots(a: Offset, b: Offset, amps: Double) {
                if (amps <= 1e-4) return
                val speed = (amps * 1.6).coerceAtMost(3.0).toFloat()
                val len = (b - a).getDistance(); val n = (len / (24f * density)).toInt().coerceAtLeast(1)
                repeat(n) { k -> val f = ((now * speed * 60f * density / len + k.toFloat() / n) % 1f); drawCircle(E.Sun.copy(alpha = 0.85f), 2.6f * density, a + (b - a) * f) }
            }
            val total = if (res.short) 3.0 else res.totalA
            g.loop.take(6).forEach { (a, b) -> dots(a, b, total) }   // die ersten sechs Stücke sind der Hauptkreis
            g.rows.forEach { (a, b, branch) -> dots(a, b, branch.firstOrNull()?.let { res.amps[it] } ?: 0.0) }
            // Batterie
            val bat = g.battery
            val d = density
            drawRoundRect(if (res.short) E.Red.copy(alpha = 0.5f + 0.3f * kotlin.math.sin(now * 20)) else Color(0xFF2B2160), Offset(bat.x - 16f * d, bat.y - g.batHalf), Size(32f * d, g.batHalf * 2), CornerRadius(8f * d))
            drawLine(E.Text, Offset(bat.x - 13f * d, bat.y - 5f * d), Offset(bat.x + 13f * d, bat.y - 5f * d), 3f * d)
            drawLine(E.Text, Offset(bat.x - 6f * d, bat.y + 5f * d), Offset(bat.x + 6f * d, bat.y + 5f * d), 5f * d)
            label(tm, "${Fmt.num(volts)} V", Offset(bat.x + 34f * d, bat.y), 13.sp, E.Sun, bold = true)
            if (res.short) { label(tm, "🔥", Offset(bat.x, bat.y - g.batHalf - 16f * d), 22.sp); if ((now * 3).toInt() % 2 == 0) bursts.add(Burst(bat, listOf(E.Sun, E.Orange, Color.White), 14, 160f, (now * 10).toInt()), now) }
            // Bauteile
            slots.forEachIndexed { i, sl -> drawPart(tm, sl, g.centers[i], Circuits.brightness(res, slots, i, volts), res.amps[i], now, editR == i) }
            drawBursts(bursts, now)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Readout("Stromstärke", if (res.short) "⚠️ sehr groß" else "${Fmt.num(res.totalA, 3)} A", if (res.short) E.Red else E.Mint,
                Modifier.weight(1f).let { m -> if (s.goal == CircuitGoal.MEASURE) m.pulse(!measured, 0.03f) else m }.pointerInput(Unit) { detectTapGestures { measured = true; sfx?.play(Sound.DING) } }.testTag("messgeraet"))
            Readout("Spannung", "${Fmt.num(volts)} V", E.Sun, Modifier.weight(1f))
            Readout("Widerstand gesamt", if (res.totalOhm.isInfinite()) "∞ (offen)" else "${Fmt.num(res.totalOhm, 1)} Ω", E.Sky, Modifier.weight(1f))
        }
        if (res.short) Text("⚠️ Kurzschluss! Der Strom nimmt den Weg ohne Widerstand. Die Batterie wird heiß.", color = E.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        val anyTest = slots.firstOrNull { it.part.isMaterial }
        if (anyTest != null) Text("${anyTest.part.emoji} ${anyTest.part.title}: " + if (anyTest.part.ohm.isInfinite()) "Isolator – kein Strom" else if (anyTest.part.ohm > 1000) "leitet fast nicht" else if (anyTest.part.ohm > 0) "leitet, aber schlechter als Metall" else "Leiter – Strom fließt", color = E.Text, fontSize = 14.sp)
        if (s.tray.isNotEmpty()) {
            Text("Bauteil wählen, dann auf einen Platz im Stromkreis tippen. Schalter antippen = umschalten.", color = E.Muted, fontSize = 13.sp)
            ChipRow(s.tray, pick, { "${it.emoji} ${it.title}" + if (it in tested) " ✓" else "" }, { pick = it }, color = E.Sun)
        } else Text(if (s.goal == CircuitGoal.REMOVE_ONE) "Tipp eine Lampe an, um sie herauszudrehen (nochmal: wieder rein)." else "Tipp die Schalter an.", color = E.Muted, fontSize = 13.sp)
        editR?.let { i -> if (slots.getOrNull(i)?.part == Part.WIDERSTAND)
            ValueSlider("Widerstand einstellen", slots[i].ohm.toFloat(), 2.5f..100f, "${Fmt.num(slots[i].ohm)} Ω", { v -> slots[i] = Slot(Part.WIDERSTAND, (roundI(v / 2.5f) * 2.5).toDouble()) }, steps = 38, color = E.Sky) }
        if (free) Panel(border = E.Sun) {
            Text("Schaltung wählen", fontWeight = FontWeight.Bold, color = E.Sun)
            val layouts = listOf("Reihe (2)" to Layout.single(2), "Reihe (3)" to Layout.single(3), "Parallel (2)" to Layout.ladder(0, listOf(1, 1)),
                "Parallel (3)" to Layout.ladder(0, listOf(1, 1, 1)), "Gemischt" to Layout.ladder(1, listOf(1, 1), 1))
            ChipRow(layouts, layouts.firstOrNull { it.second == layout }, { it.first }, { (_, l) -> layout = l; slots.clear(); slots.addAll(List(l.slots.size) { Slot(Part.LAMPE) }); editR = null })
            NumberField("Batterie", "V", 0.5, 24.0, volts, { volts = it })
        }
    }
}

private fun DrawScope.drawPart(tm: TextMeasurer, sl: Slot, c: Offset, bright: Float, amps: Double, now: Float, editing: Boolean) {
    val d = density
    val bw = 52f * d; val bh = 42f * d
    if (sl.part != Part.KABEL) drawRoundRect(Color(0xFF160F2E), Offset(c.x - bw / 2, c.y - bh / 2), Size(bw, bh), CornerRadius(10f * d))
    if (editing) drawRoundRect(E.Sky, Offset(c.x - bw / 2, c.y - bh / 2), Size(bw, bh), CornerRadius(10f * d), style = Stroke(2.5f * d))
    when (sl.part) {
        Part.LEER -> { drawRoundRect(E.Muted, Offset(c.x - bw / 2, c.y - bh / 2), Size(bw, bh), CornerRadius(10f * d), style = Stroke(2.5f * d, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f * d, 6f * d)))); label(tm, "?", c, 22.sp, E.Muted, bold = true) }
        Part.KABEL -> drawLine(E.Sun, Offset(c.x - bw / 2, c.y), Offset(c.x + bw / 2, c.y), 5f * d, StrokeCap.Round)
        Part.LAMPE -> {
            val glow = (24f + 56f * bright) * d
            if (bright > 0.005f) drawCircle(Brush.radialGradient(listOf(E.Sun.copy(alpha = (0.35f + bright * 0.6f).coerceAtMost(0.95f)), Color.Transparent), c, glow), glow, c)
            drawCircle(if (bright > 0.005f) Color(0xFFFFF3B0).copy(alpha = 0.4f + 0.6f * bright.coerceAtMost(1f)) else Color(0xFF4A4370), 15f * d, c)
            drawCircle(E.Text, 15f * d, c, style = Stroke(2.5f * d))
            val f = if (bright > 0.005f) Color.White else E.Muted
            drawLine(f, Offset(c.x - 6f * d, c.y + 5f * d), Offset(c.x + 6f * d, c.y - 5f * d), 2f * d); drawLine(f, Offset(c.x - 6f * d, c.y - 5f * d), Offset(c.x + 6f * d, c.y + 5f * d), 2f * d)
        }
        Part.MOTOR -> {
            drawCircle(E.Sky, 15f * d, c, style = Stroke(2.5f * d))
            rotate(now * 360f * (amps * 3).toFloat().coerceAtMost(6f), c) { repeat(3) { j -> rotate(j * 120f, c) { drawLine(E.Sky, c, c + Offset(0f, -12f * d), 4f * d, StrokeCap.Round) } } }
        }
        Part.SUMMER -> label(tm, "🔔", c + Offset(if (amps > 0.01) kotlin.math.sin(now * 60f) * 3f * d else 0f, 0f), 24.sp)
        Part.SCHALTER_AUF, Part.SCHALTER_ZU -> {
            val a = Offset(c.x - 17f * d, c.y); val b = Offset(c.x + 17f * d, c.y)
            drawCircle(E.Text, 4f * d, a); drawCircle(E.Text, 4f * d, b)
            val end = if (sl.part == Part.SCHALTER_ZU) b else Offset(c.x + 12f * d, c.y - 17f * d)
            drawLine(if (sl.part == Part.SCHALTER_ZU) E.Mint else E.Orange, a, end, 4f * d, StrokeCap.Round)
        }
        Part.WIDERSTAND -> {
            drawRect(E.Orange.copy(alpha = 0.25f), Offset(c.x - 19f * d, c.y - 8f * d), Size(38f * d, 16f * d))
            drawRect(E.Orange, Offset(c.x - 19f * d, c.y - 8f * d), Size(38f * d, 16f * d), style = Stroke(2.5f * d))
            label(tm, "${Fmt.num(sl.ohm, 1)} Ω", c + Offset(0f, 30f * d), 11.sp, E.Text)
        }
        else -> { drawCircle(Color(0xFF8E87B8), 3f * d, Offset(c.x - 22f * d, c.y)); drawCircle(Color(0xFF8E87B8), 3f * d, Offset(c.x + 22f * d, c.y)); label(tm, sl.part.emoji, c, 24.sp) }
    }
}
