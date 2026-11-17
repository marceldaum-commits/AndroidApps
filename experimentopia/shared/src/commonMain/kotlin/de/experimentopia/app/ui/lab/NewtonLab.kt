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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.experimentopia.app.ui.common.*
import de.experimentopia.app.ui.fx.*
import de.experimentopia.app.ui.theme.E
import de.experimentopia.core.model.*

@Composable
fun NewtonLab(s: NewtonSetup, free: Boolean, onGoal: () -> Unit) {
    var mode by remember { mutableStateOf(s.mode) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (free) ChipRow(NewtonMode.entries, mode, { when (it) { NewtonMode.CART -> "🛒 Wagen ziehen (F = m·a)"; NewtonMode.PUSH -> "🛹 Abstoßen (actio = reactio)"; NewtonMode.GLIDE -> "🧊 Schubs (Trägheit)" } }, { mode = it })
        when (mode) { NewtonMode.CART -> CartLab(s, free, onGoal); NewtonMode.PUSH -> PushLab(s, free, onGoal); NewtonMode.GLIDE -> GlideLab(free, onGoal) }
    }
}

private data class Run(val m: Double, val f: Double) { val a get() = Newton.accel(f, m); val t get() = Newton.timeFor(Newton.TRACK_M, a) }

@Composable
private fun CartLab(s: NewtonSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val tm = rememberTextMeasurer()
    val now by rememberSeconds()
    var m by remember { mutableDoubleStateOf(s.masses.firstOrNull() ?: 2.0) }
    var f by remember { mutableDoubleStateOf(s.forces.firstOrNull() ?: 10.0) }
    var startAt by remember { mutableFloatStateOf(-1f) }
    val runs = remember { mutableStateListOf<Run>() }
    val cur = Run(m, f)
    // Zeitlupe, damit man den Unterschied sieht: die Strecke dauert in der App etwa doppelt so lang wie echt
    val slow = 2.0
    val t = if (startAt < 0) 0.0 else ((now - startAt) / slow).coerceAtMost(cur.t)
    val x = 0.5 * cur.a * t * t
    val arrived = startAt >= 0 && t >= cur.t
    LaunchedEffect(arrived) { if (arrived) { sfx?.play(Sound.CLANK); if (runs.none { it == cur }) runs += cur } }
    val need = s.masses.flatMap { mm -> s.forces.map { ff -> Run(mm, ff) } }
    GoalWhen(!free && need.all { it in runs }, onGoal)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(190.dp).testTag("wagenbahn")) {
            val w = size.width; val h = size.height; val d = density
            val x0 = w * 0.08f; val x1 = w * 0.9f; val y = h * 0.62f
            drawLine(E.Muted, Offset(x0, y + 22 * d), Offset(x1 + 20 * d, y + 22 * d), 4 * d, StrokeCap.Round)
            for (k in 0..4) { val xx = x0 + (x1 - x0) * k / 4; drawLine(E.Muted.copy(alpha = 0.6f), Offset(xx, y + 26 * d), Offset(xx, y + 34 * d), 2 * d); label(tm, "${Fmt.num(Newton.TRACK_M * k / 4)} m", Offset(xx, y + 44 * d), 10.sp, E.Muted) }
            drawLine(E.Sun, Offset(x1, y - 40 * d), Offset(x1, y + 22 * d), 3 * d)
            label(tm, "🏁", Offset(x1 + 8 * d, y - 48 * d), 18.sp)
            val cx = x0 + ((x / Newton.TRACK_M) * (x1 - x0)).toFloat()
            // Wagen mit Ladung
            val bw = 54 * d; val bh = 26 * d
            drawRoundRect(E.Sky, Offset(cx - bw, y - bh), Size(bw, bh), CornerRadius(6 * d))
            drawCircle(E.Text, 6 * d, Offset(cx - bw * 0.75f, y + 2 * d)); drawCircle(E.Text, 6 * d, Offset(cx - bw * 0.25f, y + 2 * d))
            val blocks = (m / 1.0).toInt().coerceIn(1, 8)
            repeat(blocks) { k -> drawRoundRect(E.Orange, Offset(cx - bw + 4 * d + (k % 4) * 12 * d, y - bh - 10 * d - (k / 4) * 10 * d), Size(10 * d, 9 * d), CornerRadius(2 * d)) }
            label(tm, "${Fmt.num(m)} kg", Offset(cx - bw / 2, y - bh / 2), 11.sp, E.Night, true)
            // Zugseil und Kraftpfeil (Länge ∼ Kraft)
            val arrow = (f.toFloat() * 2.2f * d).coerceAtMost(110 * d)
            drawLine(E.Pink, Offset(cx, y - bh / 2), Offset(cx + arrow, y - bh / 2), 5 * d, StrokeCap.Round)
            drawLine(E.Pink, Offset(cx + arrow, y - bh / 2), Offset(cx + arrow - 9 * d, y - bh / 2 - 7 * d), 5 * d, StrokeCap.Round)
            drawLine(E.Pink, Offset(cx + arrow, y - bh / 2), Offset(cx + arrow - 9 * d, y - bh / 2 + 7 * d), 5 * d, StrokeCap.Round)
            label(tm, "F = ${Fmt.num(f)} N", Offset(cx + arrow / 2, y - bh / 2 - 16 * d), 11.sp, E.Pink, true)
            if (startAt >= 0) label(tm, "⏱ ${Fmt.num(t, 2)} s", Offset(w * 0.2f, h * 0.12f), 13.sp, E.Sun, true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Readout("Beschleunigung a = F / m", if (arrived) "${Fmt.num(cur.a)} m/s²" else "?", E.Mint, Modifier.weight(1.3f))
            Readout("Zeit für 2 m", if (arrived) "${Fmt.num(cur.t, 2)} s" else "?", E.Sun, Modifier.weight(1f))
        }
        GlowButton({ startAt = now; sfx?.play(Sound.WHOOSH) }, Modifier.fillMaxWidth().pulse(startAt < 0), color = E.Mint) { Text("🚦 Los!", fontWeight = FontWeight.Bold) }
        if (s.masses.size > 1 || free) { Text("Masse des Wagens:", color = E.Muted, fontSize = 13.sp); ChipRow(if (free) listOf(1.0, 2.0, 4.0, 6.0) else s.masses, m, { "${Fmt.num(it)} kg" + if (runs.any { r -> r.m == it && r.f == f }) " ✓" else "" }, { m = it; startAt = -1f }) }
        if (s.forces.size > 1 || free) { Text("Zugkraft des Motors:", color = E.Muted, fontSize = 13.sp); ChipRow(if (free) listOf(5.0, 10.0, 20.0, 40.0) else s.forces, f, { "${Fmt.num(it)} N" + if (runs.any { r -> r.f == it && r.m == m }) " ✓" else "" }, { f = it; startAt = -1f }) }
        if (runs.isNotEmpty()) Panel(Modifier.fillMaxWidth()) {
            Text("Deine Fahrten", color = E.Sun, fontWeight = FontWeight.Bold)
            runs.forEach { r -> Text("${Fmt.num(r.m)} kg · ${Fmt.num(r.f)} N → a = ${Fmt.num(r.a)} m/s², ${Fmt.num(r.t, 2)} s", color = E.Text, fontSize = 14.sp) }
        }
        if (free) Panel(border = E.Sun) {
            Text("Eigene Werte", fontWeight = FontWeight.Bold, color = E.Sun)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Masse", "kg", 0.1, 100.0, m, { m = it; startAt = -1f }, Modifier.weight(1f))
                NumberField("Kraft", "N", 0.5, 500.0, f, { f = it; startAt = -1f }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PushLab(s: NewtonSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val tm = rememberTextMeasurer()
    val now by rememberSeconds()
    val pairs = if (free) listOf(30.0 to 30.0, 30.0 to 90.0, 40.0 to 60.0) else s.pairs
    var pair by remember { mutableStateOf(pairs.first()) }
    var own by remember { mutableDoubleStateOf(50.0) }
    var startAt by remember { mutableFloatStateOf(-1f) }
    val done = remember { mutableStateListOf<Pair<Double, Double>>() }
    val (v1, v2) = Newton.pushSpeeds(pair.first, pair.second)
    val t = if (startAt < 0) 0f else (now - startAt).coerceAtMost(2.5f)
    LaunchedEffect(startAt) { if (startAt >= 0) { kotlinx.coroutines.delay(1200); if (pair !in done) done += pair } }
    GoalWhen(!free && s.pairs.all { it in done }, onGoal)
    fun who(m: Double) = when { m <= 35 -> "👧"; m >= 80 -> "👨"; else -> "🧑" }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(170.dp).testTag("skateboards")) {
            val w = size.width; val h = size.height; val d = density
            val y = h * 0.6f; val scale = w * 0.06f   // Bildpunkte je Meter
            drawLine(E.Muted, Offset(0f, y + 22 * d), Offset(w, y + 22 * d), 3 * d)
            val c = w / 2
            val xl = c - 24 * d - (v1 * t).toFloat() * scale; val xr = c + 24 * d + (v2 * t).toFloat() * scale
            fun skater(x: Float, m: Double) {
                drawRoundRect(E.Orange, Offset(x - 22 * d, y + 8 * d), Size(44 * d, 6 * d), CornerRadius(3 * d))
                drawCircle(E.Text, 4 * d, Offset(x - 14 * d, y + 18 * d)); drawCircle(E.Text, 4 * d, Offset(x + 14 * d, y + 18 * d))
                label(tm, who(m), Offset(x, y - 14 * d), 32.sp); label(tm, "${Fmt.num(m, 0)} kg", Offset(x, y - 44 * d), 11.sp, E.Text, true)
            }
            skater(xl, pair.first); skater(xr, pair.second)
            if (startAt >= 0 && t < 0.4f) label(tm, "💥", Offset(c, y - 10 * d), 26.sp)
            if (startAt >= 0) { label(tm, "← ${Fmt.num(v1, 1)} m/s", Offset(xl, y + 40 * d), 11.sp, E.Mint, true); label(tm, "${Fmt.num(v2, 1)} m/s →", Offset(xr, y + 40 * d), 11.sp, E.Pink, true) }
        }
        GlowButton({ startAt = now; sfx?.play(Sound.POP) }, Modifier.fillMaxWidth().pulse(startAt < 0), color = E.Mint) { Text("🤲 Abstoßen!", fontWeight = FontWeight.Bold) }
        if (startAt >= 0) Text("Beide bekommen denselben Schubs (Kraftstoß ${Fmt.num(Newton.PUSH_NS, 0)} N·s). Geschwindigkeit = Schubs ÷ Masse: links ${Fmt.num(v1, 1)} m/s, rechts ${Fmt.num(v2, 1)} m/s.", color = E.Text, fontSize = 14.sp)
        if (pairs.size > 1) ChipRow(pairs, pair, { "${who(it.first)} ${Fmt.num(it.first, 0)} kg + ${who(it.second)} ${Fmt.num(it.second, 0)} kg" + if (it in done) " ✓" else "" }, { pair = it; startAt = -1f })
        if (free) Panel(border = E.Sun) {
            Text("Eigene Werte: rechte Person", fontWeight = FontWeight.Bold, color = E.Sun)
            NumberField("Masse rechts", "kg", 5.0, 300.0, own, { own = it; pair = pair.first to it; startAt = -1f })
        }
    }
}

/** Trägheit: einmal schubsen, dann ohne Motor rollen lassen – spiegelglatt oder mit Reibung */
@Composable
private fun GlideLab(free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val tm = rememberTextMeasurer()
    val now by rememberSeconds()
    var friction by remember { mutableStateOf(false) }
    var startAt by remember { mutableFloatStateOf(-1f) }
    val done = remember { mutableStateListOf<Boolean>() }
    val t = if (startAt < 0) 0.0 else (now - startAt).toDouble()
    val (x, v) = Glide.at(t, friction)
    val finished = startAt >= 0 && (x >= Glide.TRACK || (friction && v <= 0.0))
    LaunchedEffect(finished) { if (finished) { sfx?.play(if (friction) Sound.SOFT else Sound.CLANK); if (friction !in done) done += friction } }
    GoalWhen(!free && done.containsAll(listOf(false, true)), onGoal)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(170.dp).testTag("gleitbahn")) {
            val w = size.width; val h = size.height; val d = density
            val x0 = w * 0.1f; val x1 = w * 0.92f; val y = h * 0.6f
            drawLine(if (friction) Color(0xFFB5651D) else Color(0xFFBFE8FF), Offset(x0 - 20 * d, y + 22 * d), Offset(x1 + 10 * d, y + 22 * d), (if (friction) 8 else 4) * d, StrokeCap.Round)
            for (k in 0..3) { val xx = x0 + (x1 - x0) * k / 3; label(tm, "${k} m", Offset(xx, y + 40 * d), 10.sp, E.Muted) }
            val cx = x0 + ((minOf(x, Glide.TRACK) / Glide.TRACK) * (x1 - x0)).toFloat()
            val bw = 50 * d; val bh = 24 * d
            drawRoundRect(E.Sky, Offset(cx - bw, y - bh), Size(bw, bh), CornerRadius(6 * d))
            drawCircle(E.Text, 6 * d, Offset(cx - bw * 0.75f, y + 2 * d)); drawCircle(E.Text, 6 * d, Offset(cx - bw * 0.25f, y + 2 * d))
            if (startAt >= 0 && t < 0.4) label(tm, "✋", Offset(cx - bw - 14 * d, y - bh / 2), 22.sp)
            label(tm, if (friction) "Teppich (mit Reibung)" else "spiegelglatt, ohne Reibung", Offset(w / 2, h * 0.1f), 12.sp, E.Muted)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Readout("Geschwindigkeit", "${Fmt.num(maxOf(v, 0.0), 2)} m/s", E.Mint, Modifier.weight(1f))
            Readout("Strecke", "${Fmt.num(minOf(x, Glide.TRACK), 2)} m", E.Sun, Modifier.weight(1f))
        }
        ChipRow(listOf(false, true), friction, { b -> (if (b) "🟫 Teppich" else "🧊 spiegelglatt") + if (b in done) " ✓" else "" }, { friction = it; startAt = -1f })
        GlowButton({ startAt = now; sfx?.play(Sound.WHOOSH) }, Modifier.fillMaxWidth().pulse(startAt < 0), color = E.Mint) { Text("✋ Schubsen!", fontWeight = FontWeight.Bold) }
        Text("Nach dem Schubs gibt es keinen Motor mehr – nur noch die Bahn.", color = E.Muted, fontSize = 13.sp)
    }
}
