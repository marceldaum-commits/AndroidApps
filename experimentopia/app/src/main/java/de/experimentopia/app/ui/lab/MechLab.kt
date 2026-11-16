package de.experimentopia.app.ui.lab

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.experimentopia.app.ui.common.*
import de.experimentopia.app.ui.fx.*
import de.experimentopia.app.ui.theme.E
import de.experimentopia.core.model.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LeverLab(s: LeverSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val view = LocalView.current; val vib = LocalVibration.current
    val tm = rememberTextMeasurer()
    // bewegliche Gewichte: Index in tray → Platz (0 = noch nicht gesetzt)
    val placed = remember { mutableStateListOf<Load>() }
    var picked by remember { mutableIntStateOf(0) }
    var tray by remember { mutableStateOf(s.tray) }
    var held by remember { mutableStateOf(s.goal == LeverGoal.RELEASE) }
    var own by remember { mutableDoubleStateOf(4.0) }
    val loads = s.fixed + placed
    val angle by animateFloatAsState(if (held) 0f else Lever.angleDeg(loads).toFloat(), spring(dampingRatio = 0.35f, stiffness = 60f), label = "beam")
    var balancedSince by remember { mutableStateOf(false) }
    LaunchedEffect(loads.toList(), held) {
        balancedSince = false
        if (s.goal == LeverGoal.BALANCE && placed.isNotEmpty() && Lever.balanced(loads)) { kotlinx.coroutines.delay(700); balancedSince = true; sfx?.play(Sound.DING) }
    }
    GoalWhen(!free && (if (s.goal == LeverGoal.RELEASE) !held else balancedSince), onGoal)

    fun place(pos: Int) {
        if (pos == 0 || tray.isEmpty()) return
        val w = tray.getOrNull(picked) ?: return
        // gleiches Gewicht aus dem Vorrat: im Auftrag nur einmal, im Freien Labor beliebig oft
        if (!free) placed.removeAll { it.label == w.label && it.emoji == w.emoji }
        placed += w.copy(pos = pos)
        sfx?.play(Sound.CLANK); view.buzz(Buzz.TICK, vib)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(230.dp).testTag("wippe").pointerInput(tray, picked, placed.size) {
            detectTapGestures { p ->
                val cx = size.width / 2f; val field = size.width * 0.085f
                val pos = Math.round((p.x - cx) / field).coerceIn(-Lever.SLOTS, Lever.SLOTS)
                val hit = placed.lastOrNull { it.pos == pos }
                if (hit != null && (free || tray.any { it.label == hit.label })) { placed.remove(hit); sfx?.play(Sound.POP) } else place(pos)
            }
        }) {
            val w = size.width; val h = size.height
            val cx = w / 2; val pivotY = h * 0.62f; val field = w * 0.085f
            // Boden und Drehpunkt
            drawRect(Color(0xFF2A1F55), Offset(0f, h * 0.88f), Size(w, h * 0.12f))
            val tri = Path().apply { moveTo(cx, pivotY); lineTo(cx - (26f * density), h * 0.88f); lineTo(cx + (26f * density), h * 0.88f); close() }
            drawPath(tri, E.Orange)
            if (held) { drawRect(E.Muted, Offset(cx - field * 5.3f, pivotY + (8f * density)), Size((22f * density), h * 0.88f - pivotY - (8f * density))); drawRect(E.Muted, Offset(cx + field * 5.3f - (22f * density), pivotY + (8f * density)), Size((22f * density), h * 0.88f - pivotY - (8f * density))) }
            rotate(angle, Offset(cx, pivotY)) {
                drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFFB07A3C), Color(0xFFD9A15A), Color(0xFFB07A3C))), Offset(cx - field * 5.5f, pivotY - (10f * density)), Size(field * (11f * density), (16f * density)), CornerRadius((8f * density)))
                for (p in -Lever.SLOTS..Lever.SLOTS) {
                    if (p == 0) continue
                    drawCircle(Color.White.copy(alpha = 0.5f), (4f * density), Offset(cx + p * field, pivotY - (2f * density)))
                    label(tm, "${abs(p)}", Offset(cx + p * field, pivotY + (18f * density)), 11.sp, E.Muted)
                }
                // Gewichte stapeln sich auf ihrem Feld
                loads.groupBy { it.pos }.forEach { (p, l) ->
                    l.forEachIndexed { k, ld ->
                        val y = pivotY - (30f * density) - k * (46f * density)
                        val isBlock = ld.emoji == "🧱"
                        if (isBlock) {
                            val bw = (22f * density) + ld.massKg.toFloat().coerceAtMost((10f * density)) * (3f * density)
                            drawRoundRect(E.Sky, Offset(cx + p * field - bw / 2, y - (18f * density)), Size(bw, (36f * density)), CornerRadius((6f * density)))
                            label(tm, Fmt.num(ld.massKg), Offset(cx + p * field, y), 13.sp, E.Night, bold = true)
                        } else {
                            label(tm, ld.emoji, Offset(cx + p * field, y - (6f * density)), 30.sp)
                            label(tm, Fmt.num(ld.massKg) + " kg", Offset(cx + p * field, y + (22f * density)), 10.sp, E.Text)
                        }
                    }
                }
            }
            label(tm, "Drehpunkt", Offset(cx, h * 0.94f), 10.sp, E.Night)
        }
        val lt = Lever.leftTorque(loads); val rt = Lever.rightTorque(loads)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Readout("links: Masse × Abstand", Fmt.num(lt), if (lt > rt) E.Pink else E.Mint, Modifier.weight(1f))
            Readout("Wippe", when (Lever.tilt(loads)) { -1 -> "⬅️ links unten"; 1 -> "rechts unten ➡️"; else -> "⚖️ gerade" }, E.Sun, Modifier.weight(1.2f))
            Readout("rechts: Masse × Abstand", Fmt.num(rt), if (rt > lt) E.Pink else E.Mint, Modifier.weight(1f))
        }
        if (s.goal == LeverGoal.RELEASE && held) GlowButton({ held = false; sfx?.play(Sound.WHOOSH) }, Modifier.fillMaxWidth().pulse(true), color = E.Pink) { Text("🙌 Loslassen!", fontWeight = FontWeight.Bold) }
        if (tray.isNotEmpty()) {
            Text("Gewicht wählen, dann auf ein Feld der Wippe tippen (nochmal tippen nimmt es weg):", color = E.Muted, fontSize = 13.sp)
            ChipRow(tray.indices.toList(), picked, { i -> "${if (tray[i].emoji == "🧱") "🧱" else tray[i].emoji} ${tray[i].label}" }, { picked = it }, color = E.Sky)
        }
        if (free) Panel(border = E.Sun) {
            Text("Eigenes Gewicht", fontWeight = FontWeight.Bold, color = E.Sun)
            Row(verticalAlignment = Alignment.CenterVertically) {
                NumberField("Masse", "kg", 0.1, 200.0, own, { own = it }, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                GlowButton({ tray = tray + Load(own, 0); picked = tray.size - 1 }, color = E.Sun) { Text("+ dazu") }
            }
            GlowButton({ placed.clear() }, color = E.Sky) { Text("🧽 Wippe leeren") }
        }
    }
}

@Composable
fun PulleyLab(s: PulleySetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val tm = rememberTextMeasurer()
    var ropes by remember { mutableIntStateOf(s.ropes.first()) }
    var mass by remember { mutableDoubleStateOf(s.massKg) }
    var lift by remember { mutableFloatStateOf(0f) }   // Höhe in m
    var strain by remember { mutableIntStateOf(0) }
    val f = Pulley.forceN(mass, ropes)
    val maxN = s.maxN
    val tooHeavy = maxN != null && f > maxN + 1e-9
    GoalWhen(!free && lift >= s.heightM.toFloat() - 0.001f && !tooHeavy, onGoal)
    LaunchedEffect(ropes, mass) { lift = 0f }
    val now by rememberSeconds()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(300.dp).testTag("flaschenzug").pointerInput(ropes, tooHeavy) {
            detectVerticalDragGestures { _, dy ->
                if (dy > 0) {
                    if (tooHeavy) { strain++; if (strain % 15 == 1) sfx?.play(Sound.SOFT) }
                    else {
                        val before = lift
                        lift = (lift + dy / (120.dp.toPx() * ropes)).coerceAtMost(s.heightM.toFloat())
                        if ((before * 10).toInt() != (lift * 10).toInt()) sfx?.play(Sound.TAP, pitch = 0.8f + lift * 0.4f, volume = 0.4f)
                    }
                }
            }
        }) {
            val w = size.width; val h = size.height
            val ceil = (18f * density)
            drawRect(Color(0xFF3A2E70), Offset(0f, 0f), Size(w, ceil))
            val crateX = w * 0.38f
            val unit = 120.dp.toPx()
            val crateTop = h - (70f * density) - lift * unit
            val crateW = (90f * density)
            val pulleyR = (22f * density)
            // Kiste
            drawRoundRect(Color(0xFFB07A3C), Offset(crateX - crateW / 2, crateTop), Size(crateW, (62f * density)), CornerRadius((8f * density)))
            label(tm, "${Fmt.num(mass)} kg", Offset(crateX, crateTop + (31f * density)), 14.sp, E.Night, bold = true)
            val pullX = w * 0.78f
            if (ropes == 1) {
                drawCircle(E.Sky, pulleyR, Offset(crateX + pulleyR, ceil + (40f * density)), style = Stroke((5f * density)))
                drawLine(E.Text, Offset(crateX, crateTop), Offset(crateX, ceil + (40f * density)), (3f * density))
                drawLine(E.Text, Offset(crateX + 2 * pulleyR, ceil + (40f * density)), Offset(crateX + 2 * pulleyR, h * 0.55f + lift * unit), (3f * density))
                label(tm, "✊", Offset(crateX + 2 * pulleyR, h * 0.55f + lift * unit + (16f * density)), 26.sp)
            } else {
                // Unterer (loser) Block an der Kiste, oberer (fester) Block an der Decke
                val lowY = crateTop - (34f * density); val topY = ceil + (40f * density)
                val span = (16f * density) * ropes
                drawRoundRect(E.Sky.copy(alpha = 0.5f), Offset(crateX - span / 2 - (8f * density), topY - (18f * density)), Size(span + (16f * density), (36f * density)), CornerRadius((14f * density)))
                drawRoundRect(E.Mint.copy(alpha = 0.5f), Offset(crateX - span / 2 - (8f * density), lowY - (18f * density)), Size(span + (16f * density), (36f * density)), CornerRadius((14f * density)))
                drawLine(E.Text, Offset(crateX, lowY + (18f * density)), Offset(crateX, crateTop), (3f * density))
                for (k in 0 until ropes) {
                    val x = crateX - span / 2 + span * k / (ropes - 1).coerceAtLeast(1)
                    drawLine(E.Sun, Offset(x, topY), Offset(x, lowY), (3f * density))
                }
                drawLine(E.Text, Offset(crateX + span / 2, topY), Offset(pullX, topY), (3f * density))
                drawLine(E.Text, Offset(pullX, topY), Offset(pullX, h * 0.55f + (lift * unit * ropes) % (h * 0.35f)), (3f * density))
                label(tm, "✊", Offset(pullX, h * 0.55f + (lift * unit * ropes) % (h * 0.35f) + (16f * density)), 26.sp)
                label(tm, "$ropes tragende Seile", Offset(crateX, (topY + lowY) / 2), 12.sp, E.Sun, bold = true)
            }
            // Höhenmarke
            drawLine(E.Mint.copy(alpha = 0.6f), Offset(crateX - (70f * density), h - (70f * density) - s.heightM.toFloat() * unit), Offset(crateX + (70f * density), h - (70f * density) - s.heightM.toFloat() * unit), (2f * density))
            label(tm, "Ziel ${Fmt.num(s.heightM)} m", Offset(crateX - (100f * density), h - (70f * density) - s.heightM.toFloat() * unit), 11.sp, E.Mint)
            if (tooHeavy && strain > 0) label(tm, "😣💦", Offset(pullX + (40f * density), h * 0.5f + sin(now * 30f) * (3f * density)), 24.sp)
        }
        Text("👇 Am Seil nach unten wischen, um zu ziehen", color = E.Muted, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Readout("Zugkraft", "${Fmt.num(f, 0)} N", if (tooHeavy) E.Red else E.Mint, Modifier.weight(1f))
            Readout("Seil gezogen", "${Fmt.num(lift * ropes.toDouble(), 2)} m", E.Sky, Modifier.weight(1f))
            Readout("Höhe", "${Fmt.num(lift.toDouble(), 2)} m", E.Sun, Modifier.weight(1f))
        }
        Text("Arbeit = Kraft × Seil = ${Fmt.num(f, 0)} N · ${Fmt.num(lift * ropes.toDouble())} m = ${Fmt.num(f * lift * ropes, 0)} J" + (s.maxN?.let { "   ·   erlaubt: höchstens ${Fmt.num(it, 0)} N" } ?: ""), color = E.Muted, fontSize = 12.sp)
        if (tooHeavy) Text("😣 Zu schwer! Mit mehr tragenden Seilen wird es leichter.", color = E.Orange, fontSize = 14.sp)
        if (s.ropes.size > 1) ChipRow(s.ropes, ropes, { if (it == 1) "1 Seil (feste Rolle)" else "$it Seile" }, { ropes = it })
        if (free) NumberField("Masse der Last", "kg", 1.0, 2000.0, mass, { mass = it })
    }
}

@Composable
fun RampLab(s: RampSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val tm = rememberTextMeasurer()
    var len by remember { mutableDoubleStateOf(s.lengths.first()) }
    var mass by remember { mutableDoubleStateOf(s.massKg) }
    var pos by remember { mutableFloatStateOf(0f) }   // 0..1 entlang der Rampe
    val done = remember { mutableStateListOf<Double>() }
    LaunchedEffect(len, mass) { pos = 0f }
    GoalWhen(!free && s.lengths.all { it in done }, onGoal)
    val f = Ramp.forceN(mass, s.heightM, len)
    val roll by animateFloatAsState(pos * 720f, label = "roll")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(220.dp).testTag("rampe").pointerInput(len) {
            detectHorizontalDragGestures { _, dx ->
                if (dx > 0 && pos < 1f) {
                    pos = (pos + dx / (size.width * 0.9f)).coerceAtMost(1f)
                    if (pos >= 1f && len !in done) { done += len; sfx?.play(Sound.DING) }
                }
            }
        }) {
            val w = size.width; val h = size.height
            val ground = h * 0.9f
            val maxL = (s.lengths + len).max()
            val scale = (w * 0.8f) / maxL.toFloat()
            val hPx = s.heightM.toFloat() * scale
            val x0 = w * 0.08f; val x1 = x0 + sqrtF((len * len - s.heightM * s.heightM).toFloat()) * scale
            drawRect(Color(0xFF2A1F55), Offset(0f, ground), Size(w, h - ground))
            drawRect(Color(0xFF5B4A9E), Offset(x1, ground - hPx), Size(w - x1, hPx))
            val ramp = Path().apply { moveTo(x0, ground); lineTo(x1, ground - hPx); lineTo(x1, ground); close() }
            drawPath(ramp, Color(0xFF8C6239))
            label(tm, "${Fmt.num(len)} m", Offset((x0 + x1) / 2 - (10f * density), ground - hPx / 2 - (18f * density)), 12.sp, E.Sun, bold = true)
            label(tm, "${Fmt.num(s.heightM)} m", Offset(x1 + (26f * density), ground - hPx / 2), 12.sp, E.Text)
            val bx = x0 + (x1 - x0) * pos; val by = ground - hPx * pos
            val ang = Math.toDegrees(kotlin.math.atan2(hPx.toDouble(), (x1 - x0).toDouble())).toFloat()
            rotate(-ang, Offset(bx, by)) { rotate(roll, Offset(bx, by - (26f * density))) { label(tm, "🛢️", Offset(bx, by - (26f * density)), 34.sp) } }
            label(tm, "💪", Offset(bx - (40f * density), by - (22f * density)), 24.sp)
        }
        Text("👉 Nach rechts wischen, um das Fass hochzuschieben", color = E.Muted, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Readout("Schiebekraft", "${Fmt.num(f, 0)} N", E.Mint, Modifier.weight(1f))
            Readout("ohne Rampe", "${Fmt.num(mass * Planet.ERDE.g, 0)} N", E.Muted, Modifier.weight(1f))
            Readout("Weg", "${Fmt.num(len * pos)} m", E.Sun, Modifier.weight(1f))
        }
        ChipRow(s.lengths, len, { "Rampe ${Fmt.num(it)} m" + if (it in done) " ✓" else "" }, { len = it })
        if (free) NumberField("Masse", "kg", 1.0, 1000.0, mass, { mass = it })
    }
}

private fun sqrtF(x: Float) = kotlin.math.sqrt(x.coerceAtLeast(0f))
