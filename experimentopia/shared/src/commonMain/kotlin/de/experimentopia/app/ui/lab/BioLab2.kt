package de.experimentopia.app.ui.lab

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.experimentopia.app.ui.common.*
import de.experimentopia.app.ui.fx.*
import de.experimentopia.app.ui.theme.E
import de.experimentopia.app.ui.theme.color
import de.experimentopia.core.model.*
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** Läuft von 0 bis 1 in [ms], sobald [key] > 0 ist und sich ändert */
@Composable
private fun runOf(key: Int, ms: Int): Float {
    val a = remember { Animatable(0f) }
    LaunchedEffect(key) { if (key > 0) { a.snapTo(0f); a.animateTo(1f, tween(ms, easing = LinearEasing)) } }
    return if (key > 0) a.value else 0f
}

// ───────────────────────────── Mikroskop ─────────────────────────────

private fun DrawScope.microbe(m: Microbe, c: Offset, len: Float, ang: Float, t: Float) {
    val col = m.color.color()
    if (len < 2.5f * density) { drawCircle(col, maxOf(len / 2, 1.2f * density), c); return }
    rotate(ang, c) {
        when (m) {
            Microbe.PANTOFFEL -> {
                val w = len * 0.36f
                if (len > 25 * density) for (k in 0 until 28) { val a = k / 28f * 6.283f; val p = c + Offset(cos(a) * len / 2, sin(a) * w / 2); drawLine(col.copy(alpha = 0.6f), p, p + Offset(cos(a + sin(t * 12 + k)) * 5 * density, sin(a) * 5 * density), 1f * density) }
                drawOval(col.copy(alpha = 0.85f), Offset(c.x - len / 2, c.y - w / 2), Size(len, w))
                drawOval(Color(0xFFB98CFF).copy(alpha = 0.7f), Offset(c.x - len * 0.08f, c.y - w * 0.18f), Size(len * 0.18f, w * 0.36f))
            }
            Microbe.WASSERFLOH -> {
                drawOval(col.copy(alpha = 0.75f), Offset(c.x - len * 0.4f, c.y - len * 0.28f), Size(len * 0.75f, len * 0.56f))
                drawCircle(Color(0xFF222222), len * 0.06f, c + Offset(len * 0.32f, -len * 0.1f))
                val sw = sin(t * 8) * 0.3f
                listOf(-1f, 1f).forEach { s -> drawLine(col, c + Offset(len * 0.3f, -len * 0.05f), c + Offset(len * 0.5f, s * len * (0.35f + sw * s)), 2.5f * density) }
                drawCircle(E.Red.copy(alpha = 0.6f + 0.4f * sin(t * 15)), len * 0.05f, c + Offset(0f, -len * 0.12f))
            }
            Microbe.AMOEBE -> {
                val path = Path()
                for (k in 0..24) { val a = k / 24f * 6.283f; val r = len / 2 * (0.7f + 0.3f * sin(a * 3 + t * 1.5f) + 0.12f * sin(a * 5 - t)); val p = c + Offset(cos(a) * r, sin(a) * r); if (k == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y) }
                path.close(); drawPath(path, col.copy(alpha = 0.7f)); drawCircle(Color(0xFF3A6B55), len * 0.08f, c)
            }
            Microbe.AUGENTIER -> {
                drawOval(col, Offset(c.x - len / 2, c.y - len * 0.14f), Size(len, len * 0.28f))
                drawCircle(E.Red, len * 0.05f, c + Offset(len * 0.36f, 0f))
                drawLine(col, c + Offset(len / 2, 0f), c + Offset(len * 0.9f, sin(t * 20) * len * 0.12f), 1.2f * density)
            }
            Microbe.BAKTERIUM -> drawRoundRect(col, Offset(c.x - len / 2, c.y - len * 0.2f), Size(len, len * 0.4f), CornerRadius(len * 0.2f))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MicroscopeLab(s: MicroscopeSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val tm = rememberTextMeasurer()
    val now by rememberSeconds()
    var mag by remember { mutableIntStateOf(1) }
    var focus by remember { mutableStateOf(s.target ?: s.microbes.first()) }
    var lamp by remember { mutableStateOf(false) }
    val drift by animateFloatAsState(if (lamp) 1f else 0f, tween(3000), label = "d")
    val target = s.target
    val ok = target != null && Microscope.sight(target, mag) == Sight.GUT && (!s.lamp || lamp || free)
    LaunchedEffect(ok) { if (ok) sfx?.play(Sound.WIN) }
    GoalWhen(!free && ok, onGoal)
    // Lage aller Lebewesen in mm (zufällig, aber fest)
    val swarm = remember(s.microbes) {
        val r = Random(5)
        s.microbes.flatMap { m -> val n = if (m == Microbe.BAKTERIUM) 40 else 4; val R = (m.sizeMm * 1.6).coerceIn(0.02, 1.5); List(n) { k -> Triple(m, Offset(((r.nextFloat() - 0.5f) * 2 * R).toFloat(), ((r.nextFloat() - 0.5f) * 2 * R).toFloat()), r.nextFloat() * 6.28f) }.mapIndexed { k, p -> if (k == 0) Triple(m, Offset.Zero, p.third) else p } }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(260.dp).testTag("mikroskopbild")) {
            val w = size.width; val h = size.height
            drawRoundRect(Color(0xFF06040F), Offset.Zero, size, CornerRadius(22 * density))
            val rad = minOf(w, h) * 0.46f; val ctr = Offset(w / 2, h / 2)
            val field = Microscope.fieldMm(mag)
            val pxPerMm = (2 * rad / field).toFloat()
            val circle = Path().apply { addOval(androidx.compose.ui.geometry.Rect(ctr, rad)) }
            drawCircle(Brush.radialGradient(listOf(Color(0xFFEAF6FF), Color(0xFFBFD9EA)), ctr, rad), rad, ctr)
            if (lamp) drawCircle(Brush.horizontalGradient(listOf(Color.Transparent, E.Sun.copy(alpha = 0.25f)), ctr.x - rad, ctr.x + rad), rad, ctr)
            // Kamera folgt dem gewählten Lebewesen
            fun wander(m: Microbe, base: Offset, ph: Float, light: Boolean = true): Offset {
                val amp = (m.sizeMm * 0.8).toFloat()
                var p = base + Offset(sin(now * 0.4f + ph) * amp, cos(now * 0.33f + ph * 1.3f) * amp)
                if (m.seeksLight && light) p += Offset(drift * (m.sizeMm * 1.6).toFloat(), 0f)
                return p
            }
            // Die Kamera folgt dem Lebewesen, aber nicht seinem Weg zum Licht – so sieht man das Schwimmen
            val cam = swarm.first { it.first == focus }.let { wander(it.first, it.second, it.third, light = false) }
            clipPath(circle) {
                swarm.forEach { (m, base, ph) ->
                    // Gezeichnet wird, was man wirklich erkennt: unsichtbar, nur ein Pünktchen oder mit Einzelheiten
                    val st = Microscope.sight(m, mag)
                    if (st == Sight.NICHTS) return@forEach
                    val p = ctr + (wander(m, base, ph) - cam) * pxPerMm
                    microbe(m, p, if (st == Sight.PUNKT) 2f * density else (m.sizeMm * pxPerMm).toFloat(), (ph * 57f + sin(now * 0.3f + ph) * 20f) * (if (m.seeksLight) 1f - drift else 1f), now)
                }
            }
            drawCircle(Color(0xFF222222), rad, ctr, style = Stroke(6 * density))
            label(tm, if (mag == 1) "👁️ bloßes Auge" else "${mag}-fach", Offset(w * 0.12f, h * 0.08f), 12.sp, E.Muted)
            label(tm, "Bild: ${Fmt.num(field, if (field < 1) 3 else 1)} mm", Offset(w * 0.86f, h * 0.92f), 11.sp, E.Muted)
            if (lamp) label(tm, "💡", Offset(w * 0.94f, h * 0.5f), 20.sp)
        }
        if (s.lamp || free) ChipRow(listOf(false, true), lamp, { if (it) "💡 Lampe rechts an" else "Lampe aus" }, { lamp = it }, color = E.Sun)
        Text("Vergrößerung", color = E.Muted, fontSize = 12.sp)
        ChipRow(Microscope.MAGS, mag, { if (it == 1) "👁️ Auge" else if (it == 10) "🔍 Lupe 10×" else "🔬 $it×" }, { mag = it }, Modifier.testTag("vergroesserung"), color = E.Sky)
        Text("Was willst du ansehen?", color = E.Muted, fontSize = 12.sp)
        ChipRow(s.microbes, focus, { it.title }, { focus = it }, color = E.Green)
        Panel(border = if (ok) E.Green else E.Line) {
            s.microbes.forEach { m ->
                val st = Microscope.sight(m, mag)
                Text("${m.title} (${Fmt.num(m.sizeMm, if (m.sizeMm < 0.01) 3 else 2)} mm): ${st.text}", fontSize = 13.sp,
                    color = when (st) { Sight.GUT -> E.Green; Sight.ZU_GROSS -> E.Orange; Sight.PUNKT -> E.Text; Sight.NICHTS -> E.Muted }, fontWeight = if (m == target) FontWeight.Bold else null)
            }
            Text(focus.note, color = E.Muted, fontSize = 12.sp)
        }
    }
}

// ───────────────────────────── Kresse-Garten ─────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GerminationLab(s: GerminationSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    var run by remember { mutableIntStateOf(0) }
    var water by remember { mutableStateOf(true) }
    var air by remember { mutableStateOf(true) }
    var light by remember { mutableStateOf(true) }
    var temp by remember { mutableDoubleStateOf(22.0) }
    val dishes = if (free) listOf(Dish("Meine Schale", water, temp, air, light)) else s.dishes
    val t = runOf(run, 4000)
    val day = s.days * t
    GoalWhen(!free && t >= 1f, onGoal)
    LaunchedEffect(t >= 1f) { if (t >= 1f && run > 0) sfx?.play(Sound.DING) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("schalen")) {
            dishes.forEach { d ->
                val hgt = Germination.heightCm(d, day)
                Column(Modifier.width(150.dp).clip(RoundedCornerShape(16.dp)).background(if (d.light) E.Deep else Color(0xFF0B0820)).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${if (d.light) "☀️" else "🌑"} ${d.label}", color = E.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Canvas(Modifier.size(130.dp, 120.dp)) {
                        val w = size.width; val h = size.height
                        val base = h * 0.82f
                        // Schale
                        if (!d.air) drawRect(Color(0xFF3DB8FF).copy(alpha = 0.4f), Offset(w * 0.08f, h * 0.2f), Size(w * 0.84f, base - h * 0.2f + 8 * density))
                        drawRoundRect(if (d.water) Color(0xFFDDEBFF) else Color(0xFFF2EEE4), Offset(w * 0.08f, base - 4 * density), Size(w * 0.84f, 12 * density), CornerRadius(4 * density))
                        drawLine(Color.White.copy(alpha = 0.7f), Offset(w * 0.05f, base + 8 * density), Offset(w * 0.95f, base + 8 * density), 2 * density)
                        val leaf = if (d.light) Color(0xFF4CD06A) else Color(0xFFF2E58A)
                        val stem = if (d.light) Color(0xFF8BE39A) else Color(0xFFFFF4C2)
                        for (k in 0 until 9) {
                            val x = w * (0.14f + k * 0.09f)
                            val sprouted = day >= Germination.daysToSprout(d) + k * 0.06
                            if (!sprouted) { drawOval(Color(0xFF8B5A2B), Offset(x - 3 * density, base - 6 * density), Size(6 * density, 4 * density)); continue }
                            val hh = ((hgt * (0.85 + (k % 3) * 0.08)) / 10.0 * (base - h * 0.06f)).toFloat().coerceAtLeast(3 * density)
                            val sway = sin(k * 1.7f) * 3 * density
                            drawLine(stem, Offset(x, base), Offset(x + sway, base - hh), (if (d.light) 2.2f else 1.4f) * density)
                            if (hh > 8 * density) {
                                val top = Offset(x + sway, base - hh)
                                drawOval(leaf, Offset(top.x - 7 * density, top.y - 3 * density), Size(7 * density, 4.5f * density))
                                drawOval(leaf, Offset(top.x, top.y - 3 * density), Size(7 * density, 4.5f * density))
                            }
                        }
                    }
                    Text("${Fmt.num(d.tempC, 0)} °C · ${if (d.water) (if (d.air) "feucht" else "unter Wasser") else "trocken"}", color = E.Muted, fontSize = 11.sp)
                    Text(if (!Germination.canGerminate(d)) "keimt nicht" else if (day < Germination.daysToSprout(d)) "noch nichts zu sehen" else "${Fmt.num(hgt, 1)} cm hoch", color = if (hgt > 0) E.Green else E.Muted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
        Readout("Tag", Fmt.num(day, 1), E.Sun)
        GlowButton({ run++; sfx?.play(Sound.WHOOSH) }, Modifier.testTag("tage"), color = E.Green) { Text(if (run == 0) "⏩ ${Fmt.num(s.days, 0)} Tage vergehen lassen" else "🔁 Neu aussäen") }
        if (free) Panel(border = E.Sun) {
            Text("Deine Schale", fontWeight = FontWeight.Bold, color = E.Sun)
            ChipRow(listOf(true, false), water, { if (it) "💧 nass" else "trocken" }, { water = it; run = 0 })
            ChipRow(listOf(true, false), air, { if (it) "Luft dran" else "🌊 ganz unter Wasser" }, { air = it; run = 0 }, color = E.Sky)
            ChipRow(listOf(true, false), light, { if (it) "☀️ hell" else "🌑 dunkel" }, { light = it; run = 0 }, color = E.Violet)
            NumberField("Temperatur", "°C", -10.0, 50.0, temp, { temp = it; run = 0 })
        }
    }
}

// ───────────────────────────── Nahrungsketten ─────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodChainLab(s: FoodChainSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val chain = remember { mutableStateListOf<Organism>() }
    var msg by remember { mutableStateOf("Tipp die Lebewesen der Reihe nach an – zuerst eine Pflanze.") }
    var shake by remember { mutableIntStateOf(0) }
    var removed by remember { mutableStateOf(false) }
    val gone = if (removed) s.remove else null
    val starving = gone?.let { FoodWeb.starving(it) } ?: emptySet()
    val ok = if (s.goalLength > 0) chain.size >= s.goalLength && FoodWeb.valid(chain) else removed
    LaunchedEffect(ok) { if (ok) sfx?.play(Sound.WIN) }
    GoalWhen(!free && ok, onGoal)
    fun add(o: Organism) {
        when {
            chain.isEmpty() && !o.producer -> { msg = "🙈 Am Anfang steht immer eine Pflanze – die macht ihre Nahrung selbst."; shake++; sfx?.play(Sound.SOFT) }
            chain.isNotEmpty() && !FoodWeb.canFollow(chain.last(), o) -> {
                val f = FoodWeb.food(o).filter { it != gone }
                msg = if (o.producer) "🙈 ${o.title} frisst niemanden – Pflanzen leben von Licht." else "🙈 ${o.title} frisst keine ${chain.last().title}. ${o.title} frisst: ${f.joinToString { it.title }}."
                shake++; sfx?.play(Sound.SOFT)
            }
            else -> { chain += o; msg = if (chain.size == 1) "${o.emoji} ${o.title} – wer frisst das?" else "✨ ${chain[chain.size - 2].title} wird von ${o.title} gefressen."; sfx?.play(Sound.POP, pitch = 0.8f + chain.size * 0.1f) }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Die Kette
        Row(Modifier.fillMaxWidth().shake(shake).clip(RoundedCornerShape(18.dp)).background(Color(0xFF12301E)).padding(10.dp).testTag("kette"),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (chain.isEmpty()) Text("🌱 → ? → ?", color = E.Muted, fontSize = 22.sp)
            chain.forEachIndexed { i, o ->
                if (i > 0) Text("→", color = E.Sun, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 2.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.popIn(0)) {
                    Text(o.emoji, fontSize = 30.sp)
                    Text(o.title, color = E.Text, fontSize = 10.sp, maxLines = 1)
                }
            }
        }
        Text(msg, color = E.Text, fontSize = 14.sp, modifier = Modifier.testTag("kettenmeldung"))
        Text("Der Pfeil heißt: „wird gefressen von“.", color = E.Muted, fontSize = 12.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Organism.entries.filter { it != gone }.forEach { o ->
                Column(Modifier.width(70.dp).clip(RoundedCornerShape(14.dp)).background(if (o in starving) E.Red.copy(alpha = 0.25f) else E.Deep)
                    .border(1.5.dp, if (o in chain) E.Sun else E.Line, RoundedCornerShape(14.dp)).clickable { add(o) }.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (o in starving) "😢" else o.emoji, fontSize = 24.sp)
                    Text(o.title, color = E.Text, fontSize = 10.sp, maxLines = 1)
                }
            }
        }
        if ((s.energy || free) && chain.isNotEmpty()) Panel(border = E.Sun) {
            Text("⚡ Energie (10-%-Regel)", fontWeight = FontWeight.Bold, color = E.Sun)
            chain.forEachIndexed { i, o ->
                val e = FoodWeb.energy(10000.0, i)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(o.emoji, Modifier.width(30.dp)); Box(Modifier.height(14.dp).fillMaxWidth((0.08f + 0.92f * (e / 10000.0).toFloat()).coerceAtMost(1f) * 0.7f).clip(CircleShape).background(E.Sun))
                    Text("  ${Fmt.num(e, if (e < 1) 2 else 0)} kJ", color = E.Text, fontSize = 12.sp)
                }
            }
        }
        if ((s.poison || free) && chain.isNotEmpty()) Panel(border = E.Pink) {
            Text("☠️ Gift pro Gramm", fontWeight = FontWeight.Bold, color = E.Pink)
            Text(chain.mapIndexed { i, o -> "${o.emoji} ${Fmt.num(FoodWeb.poison(i), 0)}×" }.joinToString("   "), color = E.Text, fontSize = 14.sp)
        }
        s.remove?.let { r ->
            val many = if (r == Organism.MAUS) "Mäuse" else r.title
            GlowButton({ removed = !removed; chain.clear(); sfx?.play(if (removed) Sound.AHA else Sound.POP) }, Modifier.testTag("verschwinden"), color = E.Red) {
                Text(if (removed) "${r.emoji} $many zurückholen" else "🚫 Alle $many verschwinden lassen")
            }
            if (removed) Text(if (starving.isEmpty()) "Alle finden noch Futter." else "Ohne $many hungert: " + starving.joinToString { "${it.emoji} ${it.title}" } + ". Alle anderen weichen auf anderes Futter aus.", color = E.Text, fontSize = 13.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton({ if (chain.isNotEmpty()) chain.removeAt(chain.size - 1) }, color = E.Sky) { Text("↩️ Zurück") }
            GlowButton({ chain.clear(); msg = "Tipp die Lebewesen der Reihe nach an – zuerst eine Pflanze."; sfx?.play(Sound.WHOOSH) }, color = E.Sky) { Text("🧹 Neue Kette") }
        }
    }
}

// ───────────────────────────── Herz & Puls ─────────────────────────────

@Composable
private fun BeatingHeart(bpm: Double, now: Float, modifier: Modifier = Modifier) {
    val phase = (now * bpm.toFloat() / 60f) % 1f
    val k = 1f + 0.16f * exp(-phase * 9f)
    Text("❤️", fontSize = 64.sp, modifier = modifier.graphicsLayer { scaleX = k; scaleY = k })
}

@Composable
private fun Ecg(bpm: Double, now: Float, color: Color) {
    Canvas(Modifier.fillMaxWidth().height(70.dp)) {
        val w = size.width; val h = size.height
        drawRoundRect(Color(0xFF0B0820), Offset.Zero, size, CornerRadius(14 * density))
        val beat = 60.0 / bpm; val secs = 3.0
        val p = Path()
        for (i in 0..200) {
            val tt = now - secs + secs * i / 200.0
            val ph = ((tt % beat) + beat) % beat / beat
            val y = when { ph < 0.08 -> 0.0; ph < 0.10 -> -0.15; ph < 0.12 -> 0.9; ph < 0.14 -> -0.35; ph in 0.3..0.4 -> 0.15 * sin((ph - 0.3) / 0.1 * 3.14); else -> 0.0 }
            val x = w * i / 200f; val yy = h * 0.6f - (y * h * 0.45).toFloat()
            if (i == 0) p.moveTo(x, yy) else p.lineTo(x, yy)
        }
        drawPath(p, color, style = Stroke(2 * density))
    }
}

@Composable
fun PulseLab(s: PulseSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val now by rememberSeconds()
    var mode by remember { mutableStateOf(s.mode) }
    var age by remember { mutableIntStateOf(s.age) }
    var act by remember { mutableStateOf(Activity.RUHE) }
    val tried = remember { mutableStateListOf(Activity.RUHE) }
    val taps = remember { mutableStateListOf<Float>() }
    var counted by remember { mutableStateOf<Int?>(null) }
    var run by remember { mutableIntStateOf(0) }
    val t = runOf(run, 5000)
    val tapBpm = if (taps.size >= 4) 60.0 / ((taps.last() - taps[taps.size - 4]) / 3.0) else null
    val goal = when (mode) {
        PulseMode.ACTIVITY -> tried.size >= 2
        PulseMode.TAP -> tapBpm != null || counted != null
        PulseMode.RECOVERY -> t >= 1f
    }
    GoalWhen(!free && goal, onGoal)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (free) ChipRow(PulseMode.entries, mode, { when (it) { PulseMode.ACTIVITY -> "🏃 Bewegung"; PulseMode.TAP -> "✋ Selbst messen"; PulseMode.RECOVERY -> "⏱️ Erholung" } }, { mode = it })
        when (mode) {
            PulseMode.ACTIVITY -> {
                val bpm = Heart.pulse(age, act)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BeatingHeart(bpm, now, Modifier.testTag("herz"))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Readout("Puls (Schläge pro Minute)", Fmt.num(bpm, 0), E.Pink, Modifier.fillMaxWidth().testTag("puls"))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Readout("Blut pro Minute", "${Fmt.num(Heart.litersPerMinute(age, act), 1)} l", E.Red, Modifier.weight(1f))
                            Readout("Schläge am Tag", Fmt.num(Heart.perDay(bpm), 0), E.Sun, Modifier.weight(1f))
                        }
                    }
                }
                Ecg(bpm, now, E.Mint)
                ChipRow(Activity.entries, act, { "${it.emoji} ${it.title}" }, { act = it; if (it !in tried) tried += it; sfx?.play(Sound.TAP) }, color = E.Pink)
                Text("Alter", color = E.Muted, fontSize = 12.sp)
                ChipRow(listOf(6, 10, 14, 30), age, { if (it == 30) "Erwachsen" else "$it Jahre" }, { age = it }, color = E.Sky)
                Text("Zum Schläge-am-Tag-Wert: Puls × 60 Minuten × 24 Stunden (als ob man den ganzen Tag so weitermacht).", color = E.Muted, fontSize = 12.sp)
            }
            PulseMode.TAP -> {
                Text("Leg zwei Finger an den Hals oder innen ans Handgelenk. Tipp bei jedem Schlag aufs Herz!", color = E.Text, fontSize = 14.sp)
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val since = taps.lastOrNull()?.let { now - it } ?: 9f
                    val k = 1f + 0.25f * exp(-since * 8f)
                    Box(Modifier.size(130.dp).graphicsLayer { scaleX = k; scaleY = k }.clip(CircleShape).background(E.Pink.copy(alpha = 0.2f)).border(3.dp, E.Pink, CircleShape)
                        .clickable { taps += now; if (taps.size > 12) taps.removeAt(0); sfx?.play(Sound.TAP, pitch = 0.7f) }.testTag("tipp"), contentAlignment = Alignment.Center) { Text("❤️", fontSize = 60.sp) }
                }
                Readout("Dein Puls", tapBpm?.let { "${Fmt.num(it, 0)} pro Minute" } ?: "noch ${maxOf(0, 4 - taps.size)}× tippen", E.Pink, Modifier.fillMaxWidth())
                Panel(border = E.Sun) {
                    Text("Oder: 15 Sekunden zählen", fontWeight = FontWeight.Bold, color = E.Sun)
                    NumberField("Schläge in 15 Sekunden", "", 5.0, 60.0, 20.0, { counted = it.toInt() })
                    counted?.let { Text("$it × 4 = ${Heart.fromCount(it)} Schläge pro Minute", color = E.Text, fontWeight = FontWeight.Bold) }
                }
                Text("Normal sind für Kinder etwa 70–110, für Erwachsene etwa 60–80 Schläge pro Minute in Ruhe.", color = E.Muted, fontSize = 12.sp)
            }
            PulseMode.RECOVERY -> {
                val rest = Heart.restPulse(age); val peak = Heart.maxPulse(age) * 0.9
                val minutes = 5.0 * t
                Canvas(Modifier.fillMaxWidth().height(190.dp).testTag("erholung")) {
                    val w = size.width; val h = size.height
                    drawRoundRect(Color(0xFF0B0820), Offset.Zero, size, CornerRadius(18 * density))
                    fun y(p: Double) = h * 0.9f - ((p - 60) / (peak + 10 - 60)).toFloat() * h * 0.8f
                    drawLine(E.Muted.copy(alpha = 0.4f), Offset(0f, y(rest)), Offset(w, y(rest)), 1 * density)
                    listOf(true to E.Green, false to E.Orange).forEach { (trained, col) ->
                        val p = Path(); val n = (100 * t).toInt()
                        for (i in 0..n) { val m = 5.0 * i / 100; val x = w * 0.05f + w * 0.9f * i / 100f; val yy = y(Heart.recovery(rest, peak, m, trained)); if (i == 0) p.moveTo(x, yy) else p.lineTo(x, yy) }
                        if (run > 0) drawPath(p, col, style = Stroke(3 * density))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Readout("🏃 trainiert", Fmt.num(if (run > 0) Heart.recovery(rest, peak, minutes, true) else rest, 0), E.Green, Modifier.weight(1f))
                    Readout("🛋️ untrainiert", Fmt.num(if (run > 0) Heart.recovery(rest, peak, minutes, false) else rest, 0), E.Orange, Modifier.weight(1f))
                    Readout("Minuten", Fmt.num(minutes, 1), E.Muted, Modifier.weight(1f))
                }
                GlowButton({ run++; sfx?.play(Sound.WHOOSH) }, Modifier.testTag("sprint"), color = E.Pink) { Text(if (run == 0) "⚡ Beide sprinten!" else "🔁 Nochmal sprinten") }
                Text("Grün: trainiertes Kind · Orange: untrainiertes Kind · graue Linie: Ruhepuls", color = E.Muted, fontSize = 12.sp)
            }
        }
    }
}
