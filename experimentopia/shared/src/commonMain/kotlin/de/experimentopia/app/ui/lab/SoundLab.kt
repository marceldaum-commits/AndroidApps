package de.experimentopia.app.ui.lab

import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.experimentopia.app.ui.common.*
import de.experimentopia.app.ui.fx.*
import de.experimentopia.app.ui.theme.E
import de.experimentopia.core.model.*
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

private val TUBE_COLORS = listOf(E.Pink, E.Orange, E.Sun, E.Mint, E.Sky, E.Violet, E.Pink, E.Orange)

@Composable
fun SoundLab(s: SoundSetup, free: Boolean, onGoal: () -> Unit) {
    var mode by remember { mutableStateOf(s.mode) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (free) ChipRow(SoundMode.entries, mode, { when (it) { SoundMode.STRIKE -> "🥛 Gläser"; SoundMode.BLOW -> "🌬️ Röhren"; SoundMode.TUNE -> "🎼 Stimmen"; SoundMode.MELODY -> "🦆 Lied"; SoundMode.STRING -> "🎸 Saite" } }, { mode = it })
        when (mode) {
            SoundMode.STRIKE, SoundMode.BLOW -> Tubes(mode == SoundMode.BLOW, free, onGoal)
            SoundMode.TUNE -> Tuner(s.target ?: Note.A5, free, onGoal)
            SoundMode.MELODY -> Melody(s.melody.ifEmpty { listOf(Note.C5, Note.D5, Note.E5, Note.F5, Note.G5, Note.G5) }, free, onGoal)
            SoundMode.STRING -> Monochord(s.stringFrac, free, onGoal)
        }
    }
}

/** Gläser anschlagen oder über Röhren pusten – vier Gefäße mit einstellbarem Wasser */
@Composable
private fun Tubes(blow: Boolean, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val tm = rememberTextMeasurer()
    val now by rememberSeconds()
    val fill = remember(blow) { mutableStateListOf(0.15f, 0.4f, 0.65f, 0.88f) }
    val hitAt = remember(blow) { mutableStateListOf(-10f, -10f, -10f, -10f) }
    val played = remember(blow) { mutableStateListOf<Int>() }
    var sel by remember { mutableIntStateOf(0) }
    var ownHz by remember { mutableDoubleStateOf(440.0) }
    GoalWhen(!free && played.size >= 4, onGoal)
    fun hz(i: Int) = if (blow) Klang.tubeHz(Klang.TUBE_M * (1 - fill[i])) else Klang.glassHz(fill[i].toDouble())
    fun play(i: Int) { sel = i; hitAt[i] = now; if (i !in played) played += i; sfx?.tone(hz(i), if (blow) Timbre.FLUTE else Timbre.BELL) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(250.dp).testTag("klang").pointerInput(blow) {
            detectTapGestures { p -> val i = (p.x / (size.width / 4f)).toInt().coerceIn(0, 3); play(i) }
        }) {
            val w = size.width; val h = size.height
            for (i in 0..3) {
                val cx = w * (0.125f + 0.25f * i)
                val gw = if (blow) w * 0.09f else w * 0.17f
                val top = if (blow) h * 0.12f else h * 0.35f; val bottom = h * 0.9f
                val t = now - hitAt[i]
                val wob = if (t in 0f..1f) sin(t * 60f) * (4f * density) * exp(-t * 4f) else 0f
                val lvl = bottom - (bottom - top) * fill[i]
                drawRoundRect(Brush.verticalGradient(listOf(TUBE_COLORS[i].copy(alpha = 0.6f), TUBE_COLORS[i]), lvl, bottom), Offset(cx - gw / 2 + wob, lvl), Size(gw, bottom - lvl), CornerRadius((8f * density)))
                drawRoundRect(Color.White.copy(alpha = if (i == sel) 0.95f else 0.6f), Offset(cx - gw / 2 + wob, top), Size(gw, bottom - top), CornerRadius((10f * density)), style = Stroke(if (i == sel) (4f * density) else 2.5f))
                if (t in 0f..1.2f) {
                    // Schallwellen bzw. Pusten
                    repeat(3) { k -> val r = (t * 120f * density + k * (25f * density)); drawCircle(TUBE_COLORS[i].copy(alpha = (0.6f - t / 2f).coerceAtLeast(0f)), r, Offset(cx, top), style = Stroke((2f * density))) }
                }
                label(tm, "${Klang.round(hz(i))} Hz", Offset(cx, bottom + (14f * density)), 11.sp, E.Text, true)
                label(tm, if (blow) "🌬️" else "🥄", Offset(cx, top - (18f * density)), 18.sp)
            }
        }
        Text(if (blow) "Antippen = über die Röhre pusten. " else "Antippen = mit dem Löffel anschlagen. " + "Unten die Wasserhöhe des gewählten Gefäßes ändern.", color = E.Muted, fontSize = 13.sp)
        ValueSlider("Wasser in Gefäß ${sel + 1}", fill[sel], 0.05f..0.95f, "${(fill[sel] * 100).toInt()} %", { fill[sel] = it }, color = TUBE_COLORS[sel])
        val note = Note.nearest(hz(sel))
        Text("Gefäß ${sel + 1}: ${Klang.round(hz(sel))} Hz – in der Nähe von ${note.title}" + if (blow) " · Luftsäule ${Fmt.num(Klang.TUBE_M * (1 - fill[sel]) * 100, 1)} cm" else "", color = E.Text, fontSize = 13.sp)
        if (free && blow) Panel(border = E.Sun) {
            Text("Eigener Ton: Wie viel Wasser brauche ich?", fontWeight = FontWeight.Bold, color = E.Sun)
            NumberField("Frequenz", "Hz", Klang.tubeHz(Klang.TUBE_M * 0.95), Klang.tubeHz(Klang.TUBE_M * 0.05), ownHz, { ownHz = it })
            Text("Luftsäule = 343 / (4 · ${Fmt.num(ownHz, 0)}) = ${Fmt.num(Klang.airFor(ownHz) * 100, 1)} cm → Wasser ${Fmt.num(Klang.waterFor(ownHz) * 100, 1)} cm", color = E.Text, fontSize = 13.sp)
            GlowButton({ fill[sel] = (Klang.waterFor(ownHz) / Klang.TUBE_M).toFloat().coerceIn(0.05f, 0.95f); play(sel) }, color = E.Sun) { Text("In Röhre ${sel + 1} einfüllen und pusten") }
        }
    }
}

@Composable
private fun Tuner(target: Note, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val tm = rememberTextMeasurer()
    var water by remember { mutableFloatStateOf(8f) }   // cm
    var tunedBlow by remember { mutableStateOf(false) }
    val hz = Klang.tubeHz((Klang.TUBE_M * 100 - water) / 100.0)
    val cents = Klang.cents(hz, target.hz)
    GoalWhen(!free && tunedBlow, onGoal)
    val needle by animateFloatAsState(cents.toFloat().coerceIn(-100f, 100f), spring(dampingRatio = 0.5f), label = "nadel")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(170.dp).testTag("stimmgeraet")) {
            val w = size.width; val h = size.height
            val c = Offset(w / 2, h * 0.92f); val r = h * 0.78f
            drawArc(Color(0xFF2E2260), 200f, 140f, false, Offset(c.x - r, c.y - r), Size(2 * r, 2 * r), style = Stroke(18f))
            drawArc(E.Green, 263f, 14f, false, Offset(c.x - r, c.y - r), Size(2 * r, 2 * r), style = Stroke(18f))
            val ang = toRad(270.0 + needle * 0.7)
            drawLine(if (abs(cents) <= 15) E.Green else E.Pink, c, c + Offset((kotlin.math.cos(ang) * r).toFloat(), (kotlin.math.sin(ang) * r).toFloat()), (6f * density), StrokeCap.Round)
            drawCircle(E.Text, (10f * density), c)
            label(tm, "zu tief", Offset(w * 0.18f, h * 0.3f), 12.sp, E.Muted); label(tm, "zu hoch", Offset(w * 0.82f, h * 0.3f), 12.sp, E.Muted)
            label(tm, "Ziel ${target.title} = ${Fmt.num(target.hz, 0)} Hz", Offset(w / 2, (14f * density)), 13.sp, E.Sun, true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Readout("Wasser", "${Fmt.num(water.toDouble(), 1)} cm", E.Sky, Modifier.weight(1f))
            Readout("Luftsäule", "${Fmt.num(25.0 - water, 1)} cm", E.Text, Modifier.weight(1f))
            Readout("Ton", "${Klang.round(hz)} Hz", if (abs(cents) <= 15) E.Green else E.Pink, Modifier.weight(1f))
        }
        ValueSlider("Wasserhöhe in der 25-cm-Röhre", water, 0f..20f, "${Fmt.num(water.toDouble(), 1)} cm", { water = roundI(it * 10) / 10f }, steps = 0, color = E.Sky)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton({ water = (water - 0.1f).coerceAtLeast(0f) }, color = E.Muted) { Text("−1 mm") }
            GlowButton({ sfx?.tone(hz, Timbre.FLUTE); if (abs(cents) <= 15) { tunedBlow = true; sfx?.play(Sound.WIN) } }, Modifier.weight(1f), color = E.Sun) { Text("🌬️ Pusten", fontWeight = FontWeight.Bold) }
            GlowButton({ water = (water + 0.1f).coerceAtMost(20f) }, color = E.Muted) { Text("+1 mm") }
        }
        Text(if (abs(cents) <= 15) "🎯 Passt! Nur ${Fmt.num(abs(cents), 0)} Cent daneben – das hört kaum jemand." else "${Fmt.num(abs(cents), 0)} Cent ${if (cents < 0) "zu tief – mehr Wasser" else "zu hoch – weniger Wasser"} (100 Cent = ein Halbton)", color = E.Text, fontSize = 13.sp)
    }
}

@Composable
private fun Melody(melody: List<Note>, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val view = LocalBuzzer.current; val vib = LocalVibration.current
    val tm = rememberTextMeasurer()
    val now by rememberSeconds()
    val tubes = listOf(Note.C5, Note.D5, Note.E5, Note.F5, Note.G5, Note.A5)
    var pos by remember { mutableIntStateOf(0) }
    var hitAt by remember { mutableStateOf(-1 to -10f) }
    var demo by remember { mutableStateOf(false) }
    val done = pos >= melody.size
    GoalWhen(!free && done, onGoal)
    LaunchedEffect(demo) {
        if (!demo) return@LaunchedEffect
        for (n in melody) { val i = tubes.indexOf(n); hitAt = i to now; sfx?.tone(n.hz, Timbre.FLUTE); delay(380) }
        demo = false
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(240.dp).testTag("melodie").pointerInput(pos, demo) {
            detectTapGestures { p ->
                if (demo) return@detectTapGestures
                val i = (p.x / (size.width / tubes.size)).toInt().coerceIn(0, tubes.lastIndex)
                hitAt = i to now; sfx?.tone(tubes[i].hz, Timbre.FLUTE)
                if (!done && tubes[i] == melody[pos]) { pos++; view.buzz(Buzz.TICK, vib); if (pos == melody.size) sfx?.play(Sound.WIN) }
            }
        }) {
            val w = size.width; val h = size.height
            val next = if (done) -1 else tubes.indexOf(melody[pos])
            tubes.forEachIndexed { i, n ->
                val cx = w * (i + 0.5f) / tubes.size
                val water = (Klang.waterFor(n.hz) / Klang.TUBE_M).toFloat()
                val top = h * 0.1f; val bottom = h * 0.86f; val tw = w * 0.09f
                val lvl = bottom - (bottom - top) * water
                if (i == next) drawCircle(TUBE_COLORS[i].copy(alpha = 0.25f + 0.15f * sin(now * 6f)), tw * 1.6f, Offset(cx, (top + bottom) / 2))
                drawRoundRect(TUBE_COLORS[i], Offset(cx - tw / 2, lvl), Size(tw, bottom - lvl), CornerRadius((6f * density)))
                drawRoundRect(Color.White.copy(alpha = 0.8f), Offset(cx - tw / 2, top), Size(tw, bottom - top), CornerRadius((8f * density)), style = Stroke(if (i == next) (4f * density) else (2f * density)))
                val (hi, ht) = hitAt
                if (hi == i && now - ht < 0.8f) repeat(3) { k -> drawCircle(TUBE_COLORS[i].copy(alpha = 0.6f - (now - ht) * 0.7f), (now - ht) * (100f * density) + k * (18f * density), Offset(cx, top), style = Stroke((2f * density))) }
                label(tm, n.title, Offset(cx, bottom + (14f * density)), 12.sp, E.Text, true)
            }
        }
        // Fortschritt als Notenperlen
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) { melody.forEachIndexed { i, _ -> Text(if (i < pos) "🔵" else if (i == pos) "🟡" else "⚪", fontSize = 11.sp) } }
        Text(if (done) "🎉 Du hast das ganze Lied gespielt!" else "Tipp die leuchtende Röhre an. Je mehr Wasser, desto höher.", color = E.Text, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton({ demo = true }, color = E.Violet) { Text("▶️ Blubb spielt vor") }
            GlowButton({ pos = 0 }, color = E.Muted) { Text("↺ Von vorn") }
        }
    }
}

@Composable
private fun Monochord(target: Double, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val tm = rememberTextMeasurer()
    val now by rememberSeconds()
    var frac by remember { mutableFloatStateOf(1f) }
    var pluckAt by remember { mutableFloatStateOf(-10f) }
    var octave by remember { mutableStateOf(false) }
    val hz = Klang.stringHz(frac.toDouble())
    GoalWhen(!free && octave, onGoal)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(170.dp).testTag("saite").pointerInput(frac) { detectTapGestures { pluckAt = now; sfx?.tone(hz, Timbre.PLUCK); if (abs(frac - target.toFloat()) <= 0.011f) octave = true } }) {
            val w = size.width; val h = size.height
            val x0 = w * 0.06f; val x1 = w * 0.94f; val y = h * 0.45f
            drawRoundRect(Color(0xFF8C6239), Offset(x0 - (12f * density), y + (22f * density)), Size(x1 - x0 + (24f * density), (40f * density)), CornerRadius((10f * density)))
            val xb = x0 + (x1 - x0) * frac
            val t = now - pluckAt
            val amp = if (t in 0f..3f) (14f * density) * exp(-t * 1.6f) * sin(t * hz.toFloat() * 0.05f) else 0f
            val path = Path().apply { moveTo(x0, y); var x = x0; while (x <= xb) { val u = (x - x0) / (xb - x0); lineTo(x, y + amp * sin(kotlin.math.PI.toFloat() * u)); x += (4f * density) }; lineTo(xb, y) }
            drawPath(path, E.Sun, style = Stroke((3f * density)))
            drawLine(E.Muted, Offset(xb, y), Offset(x1, y), (2f * density))
            drawLine(E.Text, Offset(xb, y - (4f * density)), Offset(xb, y + (22f * density)), (8f * density), StrokeCap.Round)
            label(tm, "Steg", Offset(xb, y + (36f * density)), 11.sp, E.Night, true)
            label(tm, "½", Offset(x0 + (x1 - x0) * 0.5f, y - (26f * density)), 12.sp, E.Muted); label(tm, "⅔", Offset(x0 + (x1 - x0) * 2 / 3f, y - (26f * density)), 12.sp, E.Muted)
            label(tm, "👆 Saite antippen = zupfen", Offset(w / 2, h * 0.08f), 12.sp, E.Muted)
        }
        ValueSlider("Schwingende Länge", frac, 0.25f..1f, "${(frac * 100).toInt()} %", { frac = roundI(it * 100) / 100f }, color = E.Sun)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Readout("Frequenz", "${Klang.round(hz)} Hz", E.Sun, Modifier.weight(1f))
            Readout("im Vergleich zur ganzen Saite", "× ${Fmt.num(1 / frac.toDouble(), 2)}", E.Mint, Modifier.weight(1.4f))
        }
        Text("Ganze Saite: 262 Hz (c′). Halbe Länge → doppelte Frequenz = eine Oktave höher." + if (!free) " Ziel: ${roundI(target * 100.0)} % der Länge." else "", color = E.Muted, fontSize = 12.sp)
    }
}
