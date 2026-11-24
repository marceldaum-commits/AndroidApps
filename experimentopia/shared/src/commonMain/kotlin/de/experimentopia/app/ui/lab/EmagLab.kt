package de.experimentopia.app.ui.lab

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
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
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/*
 * Labore zu „Motor, Dynamo & Trafo“: Elektromagnet, Motor-Werkstatt, Dynamo-Werkstatt, Trafo-Station.
 * Alle Zahlen kommen aus [Emag]; unter jedem Gerät steht die Rechnung mit den eingestellten Werten.
 */
private val Copper = Color(0xFFD98A4B)
private val Iron = Color(0xFF8F95A8)
private val NorthRed = Color(0xFFE5484D)
private val SouthBlue = Color(0xFF3D8BFF)
private val Board = Color(0xFF0B0820)

private fun <T> List<T>.at(f: Float) = this[roundI(f).toInt().coerceIn(indices)]
private fun List<*>.indexF(i: Int) = i.coerceIn(0, size - 1).toFloat()
private fun num(v: Double, d: Int = 2) = Fmt.num(v, d)

/** Ein gleichmäßig laufender Wert (Phase in Umdrehungen), der mit [rate] (Umdrehungen pro Sekunde) wächst */
@Composable
private fun rememberPhase(rate: Double): Float {
    var phase by remember { mutableFloatStateOf(0f) }
    val r by rememberUpdatedState(rate)
    LaunchedEffect(Unit) { var last = 0L; while (true) withFrameNanos { t -> if (last != 0L) phase = (phase + (t - last) / 1e9f * r.toFloat()) % 1000f; last = t } }
    return phase
}

private fun DrawScope.windings(x0: Float, x1: Float, cy: Float, h: Float, n: Int, color: Color, width: Float) {
    val k = max(3, min(26, n))
    for (i in 0 until k) { val x = x0 + (x1 - x0) * (i + 0.5f) / k; drawOval(color, Offset(x - h * 0.07f, cy - h / 2), Size(h * 0.14f, h), style = Stroke(width)) }
}

@Composable
private fun Formula(text: String) = Text(text, color = E.Text, fontSize = 13.sp, modifier = Modifier.testTag("rechnung"))

// ─────────────────────────── Elektromagnet ───────────────────────────
@Composable
fun CoilLab(s: CoilSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current; val tm = rememberTextMeasurer()
    var n by remember { mutableIntStateOf(s.windings) }
    var u by remember { mutableDoubleStateOf(s.volts) }
    var core by remember { mutableStateOf(s.core) }
    val i = Emag.coilCurrent(u); val b = Emag.coilField(n, i, core) * 1000      // mT
    val minT = s.minMilliT; val tgtT = s.targetMilliT
    val hit = (s.minMilliT?.let { b >= it } ?: false) || (s.targetMilliT?.let { EmagUi.hit(b, it) } ?: false)
    LaunchedEffect(hit) { if (hit) sfx?.play(Sound.STAR) }
    GoalWhen(!free && hit, onGoal)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(230.dp).testTag("spule")) {
            val w = size.width; val h = size.height; val cy = h * 0.42f
            drawRoundRect(Board, Offset.Zero, size, CornerRadius(22f * density))
            val x0 = w * 0.30f; val x1 = w * 0.78f; val coilH = h * 0.26f
            if (core) drawRoundRect(Iron, Offset(x0 - w * 0.03f, cy - coilH * 0.28f), Size(x1 - x0 + w * 0.06f, coilH * 0.56f), CornerRadius(6f * density))
            // Feldlinien: je stärker das Feld, desto deutlicher
            val alpha = (ln(b + 1.0) / 5.0).coerceIn(0.0, 1.0).toFloat()
            if (b > 0.01) clipRect(0f, 0f, w, h) { for (k in 1..3) { val r = coilH * (0.55f + k * 0.3f)
                drawArc(E.Sky.copy(alpha = alpha * (1f - k * 0.18f)), -60f, -240f, false, Offset(x0 - r * 0.25f, cy - r), Size((x1 - x0) + r * 0.5f, 2 * r), style = Stroke(2f * density, cap = StrokeCap.Round)) } }
            windings(x0, x1, cy, coilH, n / 10, Copper, 3f * density)
            // Batterie und Drähte
            drawLine(Copper, Offset(x0, cy + coilH / 2), Offset(w * 0.12f, cy + coilH / 2), 2.5f * density); drawLine(Copper, Offset(w * 0.12f, cy + coilH / 2), Offset(w * 0.12f, h * 0.60f), 2.5f * density)
            label(tm, "🔋", Offset(w * 0.12f, h * 0.68f), 26.sp); label(tm, "${num(u, 1)} V", Offset(w * 0.12f, h * 0.86f), 12.sp, E.Sun, true)
            // Büroklammern: mehr Feld, mehr Klammern (nur Anschauung)
            val clips = if (b < 0.05) 0 else min(14, (kotlin.math.sqrt(b) * 1.5).toInt())
            for (k in 0 until clips) label(tm, "📎", Offset(x1 + w * 0.02f - (k % 5) * w * 0.045f + (k / 5) * w * 0.02f, h * 0.70f + (k / 5) * h * 0.09f), 16.sp)
            label(tm, "${num(b, 1)} mT", Offset(w * 0.54f, h * 0.90f), 15.sp, if (hit) E.Green else E.Mint, true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Readout("Windungen", "$n", E.Orange, Modifier.weight(1f)); Readout("Strom", Fmt.unit(i, "A", 2), E.Sun, Modifier.weight(1f)); Readout("Feld", Fmt.unit(b, "mT", 1), E.Mint, Modifier.weight(1f))
        }
        s.targetMilliT?.let { Text("🎯 Ziel: ${num(it, 1)} mT (± 3 %)", color = E.Sun, fontSize = 13.sp) }
        s.minMilliT?.let { Text("🎯 Ziel: mindestens ${num(it, 1)} mT", color = E.Sun, fontSize = 13.sp) }
        if (s.adjustWindings || free) ValueSlider("🌀 Windungen", EmagUi.WINDINGS.indexOf(n).coerceAtLeast(0).toFloat(), 0f..(EmagUi.WINDINGS.size - 1).toFloat(), "$n", { n = EmagUi.WINDINGS.at(it) }, EmagUi.WINDINGS.size - 2, color = E.Orange)
        if (s.adjustVolts || free) ValueSlider("🔋 Spannung", EmagUi.COIL_VOLTS.indexOfFirst { kotlin.math.abs(it - u) < 1e-9 }.coerceAtLeast(0).toFloat(), 0f..(EmagUi.COIL_VOLTS.size - 1).toFloat(), "${num(u, 1)} V", { u = EmagUi.COIL_VOLTS.at(it) }, EmagUi.COIL_VOLTS.size - 2, color = E.Sun)
        if (s.adjustCore || free) ChipRow(listOf(false, true), core, { if (it) "🔩 mit Eisenkern" else "ohne Eisenkern" }, { core = it }, Modifier.testTag("kern"), color = E.Sky)
        Formula("I = U ÷ R = ${num(u, 1)} V ÷ ${num(Emag.COIL_OHM, 0)} Ω = ${num(i, 2)} A")
        Formula("B = ${if (core) "μr · " else ""}μ₀ · N · I ÷ l = ${if (core) "${num(Emag.MU_R_IRON, 0)} · " else ""}1,2566·10⁻⁶ · $n · ${num(i, 2)} ÷ ${num(Emag.COIL_LENGTH_M, 2)} = ${num(b, 2)} mT")
        if (!free) Text(when { hit -> "🎯 Geschafft!"; minT != null && b < minT -> "Noch zu schwach – mehr Strom, mehr Windungen oder ein Eisenkern."; tgtT != null && b < tgtT -> "Noch zu schwach."; tgtT != null -> "Zu stark."; else -> "" }, color = E.Text, fontSize = 14.sp)
        if (free) Panel(border = E.Sun) {
            Text("Eigene Werte", fontWeight = FontWeight.Bold, color = E.Sun)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Windungen", "", 1.0, 5000.0, n.toDouble(), { n = it.toInt() }, Modifier.weight(1f))
                NumberField("Spannung", "V", 0.0, 24.0, u, { u = it }, Modifier.weight(1f))
            }
        }
    }
}

// ─────────────────────────── Motor-Werkstatt ───────────────────────────
@Composable
fun MotorLab(s: MotorSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current; val tm = rememberTextMeasurer()
    var mode by remember { mutableStateOf(s.mode) }
    var rightCurrent by remember { mutableStateOf(!s.wantRight) }          // Start: Schaukel schlägt zur falschen Seite aus
    var amps by remember { mutableDoubleStateOf(2.0) }
    var volts by remember { mutableDoubleStateOf(2.0) }
    var tesla by remember { mutableDoubleStateOf(s.teslas) }
    var len by remember { mutableDoubleStateOf(s.lengthM) }
    val f = Emag.conductorForce(tesla, amps, len); val rpm = Emag.motorRpm(volts)
    val tgtN = s.targetN; val tgtRpm = s.targetRpm
    val hit = when (mode) {
        MotorMode.DIRECTION -> rightCurrent == s.wantRight
        MotorMode.FORCE -> s.targetN?.let { EmagUi.hit(f, it) } == true
        MotorMode.SPEED -> s.targetRpm?.let { EmagUi.hit(rpm, it) } == true
    } && (!free || false)
    LaunchedEffect(hit) { if (hit) sfx?.play(Sound.STAR) }
    GoalWhen(!free && hit && mode == s.mode, onGoal)
    val swing by animateFloatAsState(if (rightCurrent) 1f else -1f, label = "schaukel")
    val phase = rememberPhase(rpm / 60.0 / 20.0)              // zur Anschauung 20-fach verlangsamt
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (free) ChipRow(MotorMode.entries, mode, { when (it) { MotorMode.DIRECTION -> "↔️ Richtung"; MotorMode.FORCE -> "💪 Kraft"; MotorMode.SPEED -> "⚙️ Drehzahl" } }, { mode = it }, Modifier.testTag("motorart"), color = E.Orange)
        Canvas(Modifier.fillMaxWidth().height(230.dp).testTag("motor")) {
            val w = size.width; val h = size.height
            drawRoundRect(Board, Offset.Zero, size, CornerRadius(22f * density))
            // Magnet: Nordpol links, Südpol rechts
            val mw = w * 0.16f
            drawRoundRect(NorthRed, Offset(w * 0.12f, h * 0.22f), Size(mw, h * 0.56f), CornerRadius(8f * density)); label(tm, "N", Offset(w * 0.12f + mw / 2, h * 0.5f), 26.sp, Color.White, true)
            drawRoundRect(SouthBlue, Offset(w * 0.72f, h * 0.22f), Size(mw, h * 0.56f), CornerRadius(8f * density)); label(tm, "S", Offset(w * 0.72f + mw / 2, h * 0.5f), 26.sp, Color.White, true)
            if (mode == MotorMode.SPEED) {
                val c = Offset(w * 0.5f, h * 0.5f); val r = h * 0.30f; val a = (phase * 2 * PI).toFloat()
                drawCircle(Iron.copy(alpha = 0.3f), r, c, style = Stroke(2f * density))
                for (k in 0 until 2) { val an = a + k * PI.toFloat(); val p1 = Offset(c.x + cos(an) * r, c.y + sin(an) * r * 0.9f); val p2 = Offset(c.x - cos(an) * r, c.y - sin(an) * r * 0.9f); drawLine(Copper, p1, p2, 5f * density, StrokeCap.Round); break }
                drawCircle(Iron, 7f * density, c)
                label(tm, "${num(rpm, 0)} min⁻¹", Offset(w * 0.5f, h * 0.9f), 15.sp, if (hit) E.Green else E.Mint, true)
            } else {
                // Leiterschaukel: Aufhängung oben, Draht pendelt zur Seite, in die die Kraft zeigt
                val pivot = Offset(w * 0.5f, h * 0.12f); val L = h * 0.62f
                val force = if (mode == MotorMode.DIRECTION) 1.0 else min(1.0, f / 0.06)
                val ang = (swing * (0.15 + 0.55 * force)).toFloat()
                val tip = Offset(pivot.x + sin(ang) * L, pivot.y + cos(ang) * L)
                drawLine(Copper, pivot, tip, 5f * density, StrokeCap.Round); drawCircle(Iron, 6f * density, pivot)
                label(tm, if (swing > 0) "➡️ Kraft nach rechts" else "⬅️ Kraft nach links", Offset(w * 0.5f, h * 0.90f), 14.sp, E.Sun, true)
                label(tm, "Strom ${if (swing > 0) "⊗" else "⊙"}", Offset(w * 0.5f, h * 0.05f + 4f * density), 11.sp, E.Muted)
            }
        }
        when (mode) {
            MotorMode.DIRECTION -> {
                ChipRow(listOf(false, true), rightCurrent, { if (it) "🔋 Strom →" else "🔋 Strom ←" }, { rightCurrent = it }, Modifier.testTag("polen"), color = E.Orange)
                Text("Kraft auf den Leiter: Dreifingerregel – Daumen Strom, Zeigefinger Feld (Nord → Süd), Mittelfinger Kraft.", color = E.Muted, fontSize = 12.sp)
                Text(if (hit) "🎯 Die Schaukel schlägt nach ${if (s.wantRight) "rechts" else "links"} aus!" else "Pole den Strom um und schau, wohin die Schaukel ausschlägt.", color = E.Text, fontSize = 14.sp)
            }
            MotorMode.FORCE -> {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Readout("Feld B", Fmt.unit(tesla, "T", 2), E.Sky, Modifier.weight(1f)); Readout("Länge l", Fmt.unit(len * 100, "cm", 0), E.Orange, Modifier.weight(1f)); Readout("Kraft F", Fmt.unit(f, "N", 3), E.Mint, Modifier.weight(1f))
                }
                s.targetN?.let { Text("🎯 Ziel: ${num(it, 2)} N (± 3 %)", color = E.Sun, fontSize = 13.sp) }
                ValueSlider("⚡ Strom", EmagUi.FORCE_AMPS.indexOfFirst { kotlin.math.abs(it - amps) < 1e-9 }.coerceAtLeast(0).toFloat(), 0f..(EmagUi.FORCE_AMPS.size - 1).toFloat(), "${num(amps, 1)} A", { amps = EmagUi.FORCE_AMPS.at(it) }, EmagUi.FORCE_AMPS.size - 2, color = E.Sun)
                Formula("F = B · I · l = ${num(tesla, 2)} T · ${num(amps, 1)} A · ${num(len, 2)} m = ${num(f, 3)} N")
                if (!free) Text(if (hit) "🎯 Genau richtig!" else if (tgtN != null && f < tgtN) "Noch zu schwach – mehr Strom." else "Zu stark – weniger Strom.", color = E.Text, fontSize = 14.sp)
                if (free) Panel(border = E.Sun) {
                    Text("Eigene Werte", fontWeight = FontWeight.Bold, color = E.Sun)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField("Feld B", "T", 0.01, 2.0, tesla, { tesla = it }, Modifier.weight(1f)); NumberField("Länge l", "m", 0.01, 0.5, len, { len = it }, Modifier.weight(1f))
                    }
                }
            }
            MotorMode.SPEED -> {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Readout("Spannung", Fmt.unit(volts, "V", 1), E.Sun, Modifier.weight(1f)); Readout("Drehzahl", Fmt.unit(rpm, "min⁻¹", 0), E.Mint, Modifier.weight(1f)) }
                s.targetRpm?.let { Text("🎯 Ziel: ${num(it, 0)} min⁻¹ (± 3 %)", color = E.Sun, fontSize = 13.sp) }
                ValueSlider("🔋 Spannung", EmagUi.MOTOR_VOLTS.indexOfFirst { kotlin.math.abs(it - volts) < 1e-9 }.coerceAtLeast(0).toFloat(), 0f..(EmagUi.MOTOR_VOLTS.size - 1).toFloat(), "${num(volts, 1)} V", { volts = EmagUi.MOTOR_VOLTS.at(it) }, EmagUi.MOTOR_VOLTS.size - 2, color = E.Sun)
                Formula("n = ${num(Emag.RPM_PER_VOLT, 0)} min⁻¹/V · U = ${num(Emag.RPM_PER_VOLT, 0)} · ${num(volts, 1)} V = ${num(rpm, 0)} min⁻¹ (Leerlauf, zur Anschauung 20-fach verlangsamt)")
                if (!free) Text(if (hit) "🎯 Genau richtig!" else if (tgtRpm != null && rpm < tgtRpm) "Noch zu langsam – mehr Spannung." else "Zu schnell – weniger Spannung.", color = E.Text, fontSize = 14.sp)
            }
        }
    }
}

// ─────────────────────────── Dynamo-Werkstatt ───────────────────────────
@Composable
fun DynamoLab(s: DynamoSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current; val tm = rememberTextMeasurer()
    var mode by remember { mutableStateOf(s.mode) }
    var n by remember { mutableIntStateOf(s.windings) }
    var v by remember { mutableDoubleStateOf(0.1) }
    var hz by remember { mutableDoubleStateOf(1.0) }
    val volts = if (mode == DynamoMode.MAGNET) Emag.inducedByMagnet(n, v) else Emag.generatorPeak(n, s.teslas, s.areaM2, hz)
    val hit = (s.minV?.let { volts >= it } ?: false) || (s.targetV?.let { EmagUi.hit(volts, it) } ?: false)
    LaunchedEffect(hit) { if (hit) sfx?.play(Sound.STAR) }
    GoalWhen(!free && hit && mode == s.mode, onGoal)
    // Zur Anschauung: Magnet pendelt mit v/0,1 m Durchgängen pro Sekunde, der Generator dreht mit f/10
    val phase = rememberPhase(if (mode == DynamoMode.MAGNET) v / (2 * Emag.PASS_LENGTH_M) else hz / 10.0)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (free) ChipRow(DynamoMode.entries, mode, { if (it == DynamoMode.MAGNET) "🧲 Magnet in Spule" else "🚲 Generator" }, { mode = it }, Modifier.testTag("dynamoart"), color = E.Orange)
        Canvas(Modifier.fillMaxWidth().height(230.dp).testTag("dynamo")) {
            val w = size.width; val h = size.height
            drawRoundRect(Board, Offset.Zero, size, CornerRadius(22f * density))
            if (mode == DynamoMode.MAGNET) {
                val cy = h * 0.40f; val x0 = w * 0.38f; val x1 = w * 0.66f
                windings(x0, x1, cy, h * 0.22f, n / 20, Copper, 3f * density)
                val mx = (w * 0.52f + sin(phase * 2 * PI.toFloat()) * w * 0.28f)
                val mw = w * 0.20f
                drawRoundRect(NorthRed, Offset(mx - mw / 2, cy - h * 0.06f), Size(mw / 2, h * 0.12f), CornerRadius(4f * density)); drawRoundRect(SouthBlue, Offset(mx, cy - h * 0.06f), Size(mw / 2, h * 0.12f), CornerRadius(4f * density))
                label(tm, "N", Offset(mx - mw / 4, cy), 12.sp, Color.White, true); label(tm, "S", Offset(mx + mw / 4, cy), 12.sp, Color.White, true)
                // Messgerät: Zeigerausschlag ∝ Spannung
                val c = Offset(w * 0.52f, h * 0.86f); val frac = (volts / 0.25).coerceIn(0.0, 1.0).toFloat()
                drawArc(Iron, 200f, 140f, false, Offset(c.x - w * 0.12f, c.y - w * 0.12f), Size(w * 0.24f, w * 0.24f), style = Stroke(3f * density))
                val a = toRad(200.0 + 140.0 * frac).toFloat(); drawLine(E.Pink, c, Offset(c.x + cos(a) * w * 0.11f, c.y + sin(a) * w * 0.11f), 3f * density, StrokeCap.Round)
                label(tm, "${num(volts * 1000, 0)} mV", Offset(w * 0.78f, h * 0.86f), 15.sp, if (hit) E.Green else E.Mint, true)
            } else {
                // Spule dreht sich zwischen N und S; unten läuft die Wechselspannung
                drawRoundRect(NorthRed, Offset(w * 0.06f, h * 0.10f), Size(w * 0.07f, h * 0.38f), CornerRadius(6f * density)); drawRoundRect(SouthBlue, Offset(w * 0.40f, h * 0.10f), Size(w * 0.07f, h * 0.38f), CornerRadius(6f * density))
                val c = Offset(w * 0.265f, h * 0.30f); val a = (phase * 2 * PI).toFloat(); val half = h * 0.17f
                drawLine(Copper, Offset(c.x - cos(a) * w * 0.07f, c.y - half), Offset(c.x - cos(a) * w * 0.07f, c.y + half), 4f * density); drawLine(Copper, Offset(c.x + cos(a) * w * 0.07f, c.y - half), Offset(c.x + cos(a) * w * 0.07f, c.y + half), 4f * density)
                drawLine(Copper, Offset(c.x - cos(a) * w * 0.07f, c.y - half), Offset(c.x + cos(a) * w * 0.07f, c.y - half), 4f * density); drawLine(Copper, Offset(c.x - cos(a) * w * 0.07f, c.y + half), Offset(c.x + cos(a) * w * 0.07f, c.y + half), 4f * density)
                // Kurve u(t) über 2 Perioden, Cursor zeigt die Drehung
                val gx = w * 0.56f; val gw = w * 0.40f; val gy = h * 0.42f; val gh = h * 0.26f
                drawLine(E.Muted.copy(alpha = 0.5f), Offset(gx, gy), Offset(gx + gw, gy), 1.5f * density)
                val path = Path(); for (k in 0..80) { val t = k / 80f; val y = gy - sin(t * 4f * PI.toFloat()) * gh; if (k == 0) path.moveTo(gx, y) else path.lineTo(gx + t * gw, y) }
                drawPath(path, E.Mint, style = Stroke(2.5f * density))
                val ph = (phase * 2 * PI.toFloat()) % (4f * PI.toFloat()); drawCircle(E.Sun, 5f * density, Offset(gx + ph / (4f * PI.toFloat()) * gw, gy - sin(ph) * gh))
                label(tm, "Û = ${num(volts, 2)} V", Offset(w * 0.5f, h * 0.86f), 15.sp, if (hit) E.Green else E.Mint, true)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Readout("Windungen", "$n", E.Orange, Modifier.weight(1f))
            if (mode == DynamoMode.MAGNET) Readout("Tempo", Fmt.unit(v, "m/s", 1), E.Sun, Modifier.weight(1f)) else Readout("Drehfrequenz", Fmt.unit(hz, "Hz", 0), E.Sun, Modifier.weight(1f))
            Readout(if (mode == DynamoMode.MAGNET) "Spannung" else "Scheitel", if (volts < 1) Fmt.unit(volts * 1000, "mV", 1) else Fmt.unit(volts, "V", 2), E.Mint, Modifier.weight(1f))
        }
        s.targetV?.let { Text("🎯 Ziel: ${if (it < 1) "${num(it * 1000, 0)} mV" else "${num(it, 1)} V"} (± 3 %)", color = E.Sun, fontSize = 13.sp) }
        s.minV?.let { Text("🎯 Ziel: mindestens ${num(it * 1000, 0)} mV", color = E.Sun, fontSize = 13.sp) }
        if (mode == DynamoMode.MAGNET) {
            if (s.adjustWindings || free) ValueSlider("🌀 Windungen", EmagUi.DYNAMO_WINDINGS.indexOf(n).coerceAtLeast(0).toFloat(), 0f..(EmagUi.DYNAMO_WINDINGS.size - 1).toFloat(), "$n", { n = EmagUi.DYNAMO_WINDINGS.at(it) }, EmagUi.DYNAMO_WINDINGS.size - 2, color = E.Orange)
            ValueSlider("🧲 Tempo des Magneten", EmagUi.MAGNET_SPEEDS.indexOfFirst { kotlin.math.abs(it - v) < 1e-9 }.coerceAtLeast(0).toFloat(), 0f..(EmagUi.MAGNET_SPEEDS.size - 1).toFloat(), "${num(v, 1)} m/s", { v = EmagUi.MAGNET_SPEEDS.at(it) }, EmagUi.MAGNET_SPEEDS.size - 2, color = E.Sun)
            val dt = Emag.PASS_LENGTH_M / v
            Formula("Δt = s ÷ v = ${num(Emag.PASS_LENGTH_M, 2)} m ÷ ${num(v, 1)} m/s = ${num(dt, 3)} s")
            Formula("U = N · ΔΦ ÷ Δt = $n · ${"2,5·10⁻⁵"} Wb ÷ ${num(dt, 3)} s = ${num(volts * 1000, 1)} mV")
        } else {
            ValueSlider("🚲 Drehfrequenz", EmagUi.GEN_HZ.indexOfFirst { kotlin.math.abs(it - hz) < 1e-9 }.coerceAtLeast(0).toFloat(), 0f..(EmagUi.GEN_HZ.size - 1).toFloat(), "${num(hz, 0)} Hz", { hz = EmagUi.GEN_HZ.at(it) }, EmagUi.GEN_HZ.size - 2, color = E.Sun)
            Formula("Û = N · B · A · 2π · f = $n · ${num(s.teslas, 1)} T · ${num(s.areaM2, 3)} m² · 2π · ${num(hz, 0)} s⁻¹ = ${num(volts, 2)} V")
            Formula("U_eff = Û ÷ √2 = ${num(Emag.effective(volts), 2)} V")
        }
        if (!free) Text(if (hit) "🎯 Geschafft!" else "Verstell den Regler und beobachte die Spannung.", color = E.Text, fontSize = 14.sp)
    }
}

// ─────────────────────────── Trafo-Station ───────────────────────────
@Composable
fun TrafoLab(s: TrafoSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current; val tm = rememberTextMeasurer()
    var mode by remember { mutableStateOf(s.mode) }
    var n2 by remember { mutableIntStateOf(300) }
    var kv by remember { mutableDoubleStateOf(EmagUi.LINE_KV.first()) }
    val u2 = Emag.secondaryVolts(s.u1, s.n1, n2); val i2 = u2 / s.loadOhm; val i1 = Emag.primaryCurrent(i2, s.n1, n2)
    val share = Emag.lossShare(s.powerW, kv * 1000, s.lineOhm)
    val hit = when (mode) {
        TrafoMode.RATIO -> (s.targetU2?.let { EmagUi.hit(u2, it) } ?: false) || (s.targetI2?.let { EmagUi.hit(i2, it) } ?: false)
        TrafoMode.LINE -> s.maxLossShare?.let { share <= it } == true
    }
    LaunchedEffect(hit) { if (hit) sfx?.play(Sound.STAR) }
    GoalWhen(!free && hit && mode == s.mode, onGoal)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (free) ChipRow(TrafoMode.entries, mode, { if (it == TrafoMode.RATIO) "🔌 Umspannen" else "🗼 Fernleitung" }, { mode = it }, Modifier.testTag("trafoart"), color = E.Orange)
        Canvas(Modifier.fillMaxWidth().height(220.dp).testTag("trafo")) {
            val w = size.width; val h = size.height
            drawRoundRect(Board, Offset.Zero, size, CornerRadius(22f * density))
            if (mode == TrafoMode.RATIO) {
                // Eisenkern (Rahmen) mit zwei Spulen – die Windungszahlen sind sichtbar im Verhältnis
                drawRoundRect(Iron, Offset(w * 0.28f, h * 0.12f), Size(w * 0.44f, h * 0.62f), CornerRadius(8f * density), style = Stroke(14f * density))
                val l = Offset(w * 0.28f, h * 0.43f); val r = Offset(w * 0.72f, h * 0.43f)
                windings(l.x - w * 0.05f, l.x + w * 0.05f, l.y, h * 0.24f, s.n1 / 50, Copper, 3f * density); windings(r.x - w * 0.05f, r.x + w * 0.05f, r.y, h * 0.24f, n2 / 50, E.Pink, 3f * density)
                label(tm, "${num(s.u1, 0)} V", Offset(w * 0.10f, h * 0.43f), 17.sp, E.Sun, true); label(tm, "${num(u2, 1)} V", Offset(w * 0.90f, h * 0.43f), 17.sp, if (hit) E.Green else E.Mint, true)
                label(tm, "N₁ = ${s.n1}", Offset(w * 0.22f, h * 0.88f), 13.sp, E.Muted); label(tm, "N₂ = $n2", Offset(w * 0.78f, h * 0.88f), 13.sp, E.Muted)
            } else {
                // Kraftwerk → Hochspannungsleitung → Stadt; der rote Balken zeigt, wie viel unterwegs verloren geht
                label(tm, "🏭", Offset(w * 0.10f, h * 0.40f), 30.sp); label(tm, "🏘️", Offset(w * 0.90f, h * 0.40f), 30.sp)
                drawLine(Copper, Offset(w * 0.18f, h * 0.40f), Offset(w * 0.82f, h * 0.40f), 3f * density)
                label(tm, "${num(kv, 0)} kV", Offset(w * 0.5f, h * 0.28f), 16.sp, E.Sun, true)
                val bx = w * 0.12f; val bw = w * 0.76f
                drawRoundRect(E.Panel, Offset(bx, h * 0.62f), Size(bw, h * 0.12f), CornerRadius(6f * density))
                drawRoundRect(if (hit) E.Green else E.Red, Offset(bx, h * 0.62f), Size(max(3f * density, (bw * (share * 5).coerceIn(0.0, 1.0)).toFloat()), h * 0.12f), CornerRadius(6f * density))
                label(tm, "Verlust ${num(share * 100, 2)} % (Balken: 0 … 20 %)", Offset(w * 0.5f, h * 0.88f), 13.sp, if (hit) E.Green else E.Text, true)
            }
        }
        if (mode == TrafoMode.RATIO) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Readout("U₂", Fmt.unit(u2, "V", 1), E.Mint, Modifier.weight(1f)); Readout("I₂ (${num(s.loadOhm, 0)} Ω)", Fmt.unit(i2, "A", 2), E.Sun, Modifier.weight(1f)); Readout("I₁", Fmt.unit(i1, "A", 2), E.Orange, Modifier.weight(1f)) }
            s.targetU2?.let { Text("🎯 Ziel: U₂ = ${num(it, 1)} V (± 3 %)", color = E.Sun, fontSize = 13.sp) }
            s.targetI2?.let { Text("🎯 Ziel: I₂ = ${num(it, 1)} A (± 3 %)", color = E.Sun, fontSize = 13.sp) }
            ValueSlider("🌀 Windungen sekundär (N₂)", EmagUi.N2_STEPS.indexOf(n2).coerceAtLeast(0).toFloat(), 0f..(EmagUi.N2_STEPS.size - 1).toFloat(), "$n2", { n2 = EmagUi.N2_STEPS.at(it) }, EmagUi.N2_STEPS.size - 2, color = E.Pink)
            Formula("U₂ = U₁ · N₂ ÷ N₁ = ${num(s.u1, 0)} V · $n2 ÷ ${s.n1} = ${num(u2, 1)} V")
            Formula("I₂ = U₂ ÷ R = ${num(u2, 1)} V ÷ ${num(s.loadOhm, 0)} Ω = ${num(i2, 2)} A;  P = U₂ · I₂ = ${num(u2 * i2, 1)} W;  I₁ = P ÷ U₁ = ${num(u2 * i2 / s.u1, 2)} A")
            if (!free) Text(if (hit) "🎯 Genau richtig!" else "Verstell N₂ und beobachte U₂ und I₂.", color = E.Text, fontSize = 14.sp)
        } else {
            val i = Emag.lineCurrent(s.powerW, kv * 1000)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Readout("Strom I", Fmt.unit(i, "A", 1), E.Sun, Modifier.weight(1f)); Readout("Verlust", Fmt.unit(Emag.lineLoss(s.powerW, kv * 1000, s.lineOhm) / 1000, "kW", 1), E.Red, Modifier.weight(1f)); Readout("Anteil", Fmt.unit(share * 100, "%", 2), E.Mint, Modifier.weight(1f)) }
            s.maxLossShare?.let { Text("🎯 Ziel: höchstens ${num(it * 100, 0)} % Verlust", color = E.Sun, fontSize = 13.sp) }
            ChipRow(EmagUi.LINE_KV, kv, { "${num(it, 0)} kV" }, { kv = it }, Modifier.testTag("leitung"), color = E.Sun)
            Formula("I = P ÷ U = ${num(s.powerW / 1e6, 0)} MW ÷ ${num(kv, 0)} kV = ${num(i, 1)} A")
            Formula("P_V = I² · R = (${num(i, 1)} A)² · ${num(s.lineOhm, 0)} Ω = ${num(Emag.lineLoss(s.powerW, kv * 1000, s.lineOhm) / 1000, 1)} kW = ${num(share * 100, 2)} % von ${num(s.powerW / 1e6, 0)} MW")
            if (!free) Text(if (hit) "🎯 Geschafft – kaum etwas geht verloren!" else "Zu viel Verlust – wähle eine höhere Spannung.", color = E.Text, fontSize = 14.sp)
        }
    }
}
