package de.experimentopia.app.ui.lab

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
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
import kotlin.math.sin
import kotlin.random.Random

/** Farbe einer Temperatur: kalt blau, warm orange, heiß rot */
fun tempColor(c: Double): Color = when {
    c < 0 -> E.Sky
    c < 40 -> lerp(E.Sky, E.Sun, (c / 40).toFloat())
    c < 100 -> lerp(E.Sun, E.Red, ((c - 40) / 60).toFloat())
    else -> E.Red
}

/** Thermometer mit roter Säule */
fun DrawScope.thermometer(tm: TextMeasurer, x: Float, top: Float, bottom: Float, tempC: Double, min: Double, max: Double) {
    drawRoundRect(Color.White.copy(alpha = 0.2f), Offset(x - (8f * density), top), Size((16f * density), bottom - top), CornerRadius((8f * density)))
    val f = ((tempC - min) / (max - min)).coerceIn(0.0, 1.0).toFloat()
    drawRoundRect(tempColor(tempC), Offset(x - (5f * density), bottom - (bottom - top) * f), Size((10f * density), (bottom - top) * f), CornerRadius((5f * density)))
    drawCircle(tempColor(tempC), (14f * density), Offset(x, bottom + (8f * density)))
    label(tm, "${Fmt.num(tempC, 0)} °C", Offset(x, top - (14f * density)), 12.sp, E.Text, true)
}

@Composable
fun MeltLab(s: MeltSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val tm = rememberTextMeasurer()
    val now by rememberSeconds()
    var temp by remember { mutableFloatStateOf(-5f) }
    val melted = s.things.filter { temp >= it.meltC }
    var lastCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(melted.size) { if (melted.size > lastCount) sfx?.play(Sound.DING); lastCount = melted.size }
    GoalWhen(!free && melted.size == s.things.size, onGoal)
    // Schmelzfortschritt je Ding (federnd)
    val prog = s.things.associateWith { animateFloatAsState(if (temp >= it.meltC) 1f else 0f, tween(1500), label = it.name).value }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(230.dp).testTag("heizplatte")) {
            val w = size.width; val h = size.height
            val plateY = h * 0.72f
            // Hitzeflimmern
            val heat = ((temp + 5) / s.maxC.toFloat()).coerceIn(0f, 1f)
            repeat(8) { k -> val x = w * (0.1f + k * 0.1f); val p = (now * 0.8f + k * 0.13f) % 1f
                drawLine(E.Orange.copy(alpha = heat * 0.35f * (1 - p)), Offset(x + sin(now * 4 + k) * (6f * density), plateY - p * h * 0.6f), Offset(x + sin(now * 4 + k + 1) * (6f * density), plateY - p * h * 0.6f - (18f * density)), (3f * density), StrokeCap.Round) }
            drawRoundRect(lerp(Color(0xFF55507A), E.Red, heat), Offset(w * 0.05f, plateY), Size(w * 0.75f, (18f * density)), CornerRadius((9f * density)))
            drawRect(Color(0xFF3A3360), Offset(w * 0.1f, plateY + (18f * density)), Size(w * 0.65f, h - plateY - (18f * density)))
            s.things.forEachIndexed { i, m ->
                val x = w * (0.15f + 0.6f * (i + 0.5f) / s.things.size)
                val p = prog[m] ?: 0f
                // Pfütze wächst, das Ding wird flach
                if (p > 0) drawOval(Color(0xFFFFE0A0).copy(alpha = 0.35f * p), Offset(x - (28f * density) * p, plateY - (6f * density)), Size((56f * density) * p, (10f * density)))
                scale(1f + p * 0.3f, (1f - p * 0.75f).coerceAtLeast(0.2f), Offset(x, plateY)) { label(tm, m.emoji, Offset(x, plateY - (26f * density)), 34.sp) }
                label(tm, if (p > 0.5f) "flüssig" else "${Fmt.num(m.meltC, 0)} °C?", Offset(x, plateY + (36f * density)), 10.sp, E.Muted)
            }
            thermometer(tm, w * 0.9f, h * 0.12f, h * 0.82f, temp.toDouble(), -10.0, s.maxC)
        }
        ValueSlider("Heizplatte", temp, -10f..s.maxC.toFloat(), "${Fmt.num(temp.toDouble(), 0)} °C", { temp = it }, color = E.Orange)
        melted.lastOrNull()?.let { m -> Panel(border = E.Orange) { Text("${m.emoji} ${m.title} schmilzt bei ${Fmt.num(m.meltC, 0)} °C", fontWeight = FontWeight.Bold, color = E.Text); Text(m.note, color = E.Muted, fontSize = 13.sp) } }
    }
}

@Composable
fun CurveLab(s: HeatCurveSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val tm = rememberTextMeasurer()
    val now by rememberSeconds()
    var mass by remember { mutableDoubleStateOf(s.massG) }
    var power by remember { mutableDoubleStateOf(s.powerW) }
    var start by remember { mutableDoubleStateOf(s.startC) }
    var heating by remember { mutableStateOf(false) }
    var energy by remember { mutableDoubleStateOf(0.0) }
    val total = Heat.energyToSteam(mass, start) * 1.08
    // Ablauf dauert etwa 14 s – Zeitraffer
    val speed = total / 14.0
    val curve = remember { mutableStateListOf<Pair<Double, Double>>() }
    LaunchedEffect(heating, mass, start) {
        if (!heating) return@LaunchedEffect
        var last = -1L
        while (heating && energy < total) withFrameNanos { n ->
            if (last > 0) { energy = (energy + speed * (n - last) / 1e9).coerceAtMost(total); val st = Heat.heat(mass, start, energy); curve += energy / power to st.tempC }
            last = n
        }
        heating = false
    }
    val st = Heat.heat(mass, start, energy)
    var phaseSeen by remember { mutableStateOf("") }
    LaunchedEffect(st.phase) { if (st.phase != phaseSeen && energy > 0) { phaseSeen = st.phase; sfx?.play(Sound.DING, volume = 0.5f) } }
    GoalWhen(!free && (if (s.untilSteam) st.steam >= 0.999 else st.steam > 0.02), onGoal)
    fun reset() { energy = 0.0; curve.clear(); heating = false }
    LaunchedEffect(mass, start, power) { reset() }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().height(220.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Teilchenblick
            Canvas(Modifier.weight(1f).fillMaxHeight().background(Color(0xFF0B0820), RoundedCornerShape(14.dp)).testTag("teilchen")) {
                val w = size.width; val h = size.height
                val n = 36; val r = Random(4)
                val nIce = (n * st.ice).toInt(); val nSteam = (n * st.steam).toInt()
                val jig = ((0.6f + (st.tempC.toFloat() + 40f) / 60f).coerceIn(0.5f, 3f)) * density
                for (k in 0 until n) {
                    val ph = r.nextFloat() * 6.28f; val sp = 0.4f + r.nextFloat()
                    val p = when {
                        k < nIce -> { val gx = k % 6; val gy = k / 6; Offset(w * (0.2f + gx * 0.12f) + sin(now * 25 * sp + ph) * jig, h * (0.92f - gy * 0.1f) + sin(now * 23 * sp + ph * 2) * jig) }
                        k >= n - nSteam -> { val x = ((r.nextFloat() + now * 0.35f * sp * (if (k % 2 == 0) 1 else -1)) % 1f + 1f) % 1f; val y = ((r.nextFloat() + now * 0.3f * sp) % 1f); Offset(w * (0.05f + 0.9f * tri(x)), h * (0.05f + 0.9f * tri(y))) }
                        else -> Offset(w * (0.12f + 0.76f * tri(((r.nextFloat() + now * 0.06f * sp) % 1f + 1f) % 1f)), h * (0.6f + 0.33f * tri((r.nextFloat() + now * 0.08f * sp) % 1f)))
                    }
                    drawCircle(if (k < nIce) E.Sky else if (k >= n - nSteam) Color.White.copy(alpha = 0.8f) else Color(0xFF4FA8FF), (7f * density), p)
                }
                label(tm, st.phase, Offset(w / 2, (14f * density)), 12.sp, E.Sun, true)
            }
            // Temperaturkurve
            Canvas(Modifier.weight(1.3f).fillMaxHeight().background(Color(0xFF0B0820), RoundedCornerShape(14.dp)).testTag("heizkurve")) {
                val w = size.width; val h = size.height; val pad = (28f * density)
                val tMax = total / power
                fun px(t: Double) = pad + (t / tMax * (w - pad - (6f * density))).toFloat()
                fun py(c: Double) = (h - pad) - ((c - (start - 10)) / (130 - (start - 10)) * (h - pad - (8f * density))).toFloat()
                listOf(0.0, 100.0).forEach { c -> drawLine(E.Muted.copy(alpha = 0.5f), Offset(pad, py(c)), Offset(w - (6f * density), py(c)), 1f); label(tm, "${c.toInt()}°", Offset((13f * density), py(c)), 10.sp, E.Muted) }
                label(tm, "Zeit →", Offset(w / 2, h - (10f * density)), 10.sp, E.Muted)
                for (i in 1 until curve.size) drawLine(tempColor(curve[i].second), Offset(px(curve[i - 1].first), py(curve[i - 1].second)), Offset(px(curve[i].first), py(curve[i].second)), 3.5f)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Readout("Temperatur", "${Fmt.num(st.tempC, 1)} °C", tempColor(st.tempC), Modifier.weight(1f))
            Readout("Zustand", st.phase, E.Sun, Modifier.weight(1.3f))
            Readout("Zeit", "${Fmt.num(energy / power, 0)} s", E.Sky, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton({ heating = !heating; if (heating) sfx?.play(Sound.WHOOSH) }, Modifier.weight(1f).pulse(!heating && energy == 0.0), color = if (heating) E.Muted else E.Orange) { Text(if (heating) "⏸ Pause" else "🔥 Heizen", fontWeight = FontWeight.Bold) }
            GlowButton({ reset() }, color = E.Muted) { Text("↺ Neu") }
        }
        Text("${Fmt.num(mass, 0)} g Eis, Start ${Fmt.num(start, 0)} °C, Herd ${Fmt.num(power, 0)} W · Zeitraffer", color = E.Muted, fontSize = 12.sp)
        if (free) Panel(border = E.Sun) {
            Text("Eigene Werte", fontWeight = FontWeight.Bold, color = E.Sun)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Masse", "g", 1.0, 2000.0, mass, { mass = it }, Modifier.weight(1f))
                NumberField("Start", "°C", -60.0, -0.1, start, { start = it }, Modifier.weight(1f))
            }
            NumberField("Herdleistung", "W", 50.0, 5000.0, power, { power = it })
        }
    }
}

/** Dreieckswelle 0..1..0 – Teilchen prallen an den Wänden ab */
private fun tri(x: Float) = if (x < 0.5f) x * 2 else 2 - x * 2

@Composable
fun MixLab(s: MixSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val tm = rememberTextMeasurer()
    var a by remember { mutableStateOf(s.a) }
    var mA by remember { mutableDoubleStateOf(s.massA) }; var tA by remember { mutableDoubleStateOf(s.tA) }
    var mB by remember { mutableDoubleStateOf(s.massB) }; var tB by remember { mutableDoubleStateOf(s.tB) }
    var poured by remember { mutableStateOf(false) }
    val mix = Heat.mix(mA, a.c, tA, mB, s.b.c, tB)
    val pour by animateFloatAsState(if (poured) 1f else 0f, tween(1400), label = "pour")
    val shownT = tB + (mix - tB) * pour
    val targetC = s.targetC; val adjustA = s.adjustA
    val ok = targetC == null || kotlin.math.abs(mix - targetC) <= 1.0
    LaunchedEffect(poured) { if (poured) { sfx?.play(if (a == Stuff.WASSER) Sound.SPLASH else Sound.FIZZ); kotlinx.coroutines.delay(1500); sfx?.play(if (ok) Sound.DING else Sound.SOFT) } }
    GoalWhen(!free && poured && pour > 0.99f && ok, onGoal)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(220.dp).testTag("mischen")) {
            val w = size.width; val h = size.height
            fun beaker(cx: Float, fill: Float, temp: Double, text: String) {
                val bw = w * 0.26f; val top = h * 0.3f; val bottom = h * 0.92f
                val lvl = bottom - (bottom - top) * fill
                drawRect(Brush.verticalGradient(listOf(tempColor(temp).copy(alpha = 0.6f), tempColor(temp)), lvl, bottom), Offset(cx - bw / 2, lvl), Size(bw, bottom - lvl))
                drawRoundRect(Color.White.copy(alpha = 0.8f), Offset(cx - bw / 2, top), Size(bw, bottom - top), CornerRadius((10f * density)), style = Stroke((3f * density)))
                if (temp > 50) repeat(3) { k -> drawLine(Color.White.copy(alpha = 0.3f), Offset(cx - (20f * density) + k * (20f * density), top - (8f * density)), Offset(cx - (14f * density) + k * (20f * density), top - (30f * density)), (3f * density), StrokeCap.Round) }
                label(tm, text, Offset(cx, h * 0.18f), 12.sp, E.Text, true)
            }
            val ax = w * 0.22f + (w * 0.28f) * pour
            if (pour < 0.98f) {
                if (a == Stuff.WASSER) beaker(ax, 0.55f * (1 - pour), tA, "${Fmt.num(mA, 0)} g · ${Fmt.num(tA, 0)} °C")
                else { label(tm, a.emoji, Offset(ax, h * 0.6f + pour * h * 0.2f), 40.sp); label(tm, "${a.title} ${Fmt.num(mA, 0)} g · ${Fmt.num(tA, 0)} °C", Offset(w * 0.25f, h * 0.18f), 12.sp, E.Text, true) }
            }
            beaker(w * 0.68f, 0.45f + 0.35f * pour * (if (a == Stuff.WASSER) (mA / (mA + mB)).toFloat() * 2 else 0.2f).coerceAtMost(1f), shownT, if (pour > 0.5f) "Mischung" else "${Fmt.num(mB, 0)} g · ${Fmt.num(tB, 0)} °C")
            thermometer(tm, w * 0.93f, h * 0.25f, h * 0.85f, shownT, 0.0, 100.0)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Readout("Mischtemperatur", if (poured) "${Fmt.num(shownT, 1)} °C" else "?", tempColor(shownT), Modifier.weight(1f).testTag("mischtemperatur"))
            s.targetC?.let { Readout("Ziel", "${Fmt.num(it, 0)} °C ± 1", E.Sun, Modifier.weight(1f)) }
        }
        if (poured && mix >= 100) Text("♨️ Über 100 °C würde das Wasser kochen und verdampfen – das einfache Modell gilt nur darunter.", color = E.Orange, fontSize = 13.sp)
        if (poured && !ok) Text("${Fmt.num(mix, 1)} °C – " + (if (mix > (targetC ?: 0.0)) "zu heiß. Weniger heißes Wasser!" else "zu kalt. Mehr heißes Wasser!"), color = E.Orange, fontWeight = FontWeight.Bold)
        if (adjustA != null && !free) ValueSlider("Heißes Wasser", mA.toFloat(), adjustA.toFloat()..300f, "${Fmt.num(mA, 0)} g", { mA = (roundI(it / adjustA) * adjustA).coerceAtLeast(adjustA); poured = false }, color = E.Orange)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton({ poured = true }, Modifier.weight(1f).pulse(!poured), color = E.Orange, enabled = !poured) { Text(if (a == Stuff.WASSER) "🍶 Zusammenkippen" else "👇 Hineinwerfen", fontWeight = FontWeight.Bold) }
            GlowButton({ poured = false }, color = E.Muted) { Text("↺ Neu") }
        }
        if (free) Panel(border = E.Sun) {
            Text("Eigene Werte", fontWeight = FontWeight.Bold, color = E.Sun)
            ChipRow(Stuff.entries, a, { "${it.emoji} ${it.title}" }, { a = it; poured = false })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Masse ${a.title}", "g", 1.0, 5000.0, mA, { mA = it; poured = false }, Modifier.weight(1f))
                NumberField("Temperatur", "°C", 0.0, if (a == Stuff.WASSER) 100.0 else 1000.0, tA, { tA = it; poured = false }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Wasser", "g", 1.0, 5000.0, mB, { mB = it; poured = false }, Modifier.weight(1f))
                NumberField("Temperatur", "°C", 0.0, 100.0, tB, { tB = it; poured = false }, Modifier.weight(1f))
            }
            Text("Rechnung: T = (m₁·c₁·T₁ + m₂·c₂·T₂) / (m₁·c₁ + m₂·c₂) · c(${a.title}) = ${Fmt.num(a.c, 3)} J/(g·K)", color = E.Muted, fontSize = 11.sp)
        }
    }
}
