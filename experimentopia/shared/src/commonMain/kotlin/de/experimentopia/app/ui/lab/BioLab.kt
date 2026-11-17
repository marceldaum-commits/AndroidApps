package de.experimentopia.app.ui.lab

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
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
import kotlin.math.sin

private fun pct(x: Double) = "${Fmt.num(x * 100, 0)} %"

/** Läuft von 0 bis 1 in [ms], sobald [key] sich ändert und > 0 ist */
@Composable
private fun progressOf(key: Int, ms: Int): Float {
    val a = remember { Animatable(0f) }
    LaunchedEffect(key) { if (key > 0) { a.snapTo(0f); a.animateTo(1f, tween(ms, easing = LinearEasing)) } }
    return if (key > 0) a.value else 0f
}

// ───────────────────────────── Gewächshaus (Genetik) ─────────────────────────────

private val BLOOD = listOf("AA", "A0", "BB", "B0", "AB", "00")
private fun bloodColor(g: String) = when (Blood.group(g)) { "A" -> E.Red; "B" -> E.Sky; "AB" -> E.Violet; else -> E.Sun }

@Composable
private fun Parent(label: String, g: String, trait: Trait?, modifier: Modifier = Modifier) {
    val col = trait?.color(g)?.color() ?: bloodColor(g)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(col).border(3.dp, Color.White.copy(alpha = 0.8f), CircleShape), contentAlignment = Alignment.Center) {
            Text(g, fontWeight = FontWeight.Black, fontSize = 18.sp, color = if (col.red + col.green + col.blue > 1.6f) Color(0xFF221845) else Color.White)
        }
        Text(label, color = E.Muted, fontSize = 11.sp)
        Text(trait?.look(g) ?: "Blutgruppe ${Blood.group(g)}", color = E.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeneticsLab(s: GeneticsSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    var trait by remember { mutableStateOf(s.trait) }
    var blood by remember { mutableStateOf(s.trait == null) }
    var p1 by remember { mutableStateOf(s.p1) }
    var p2 by remember { mutableStateOf(s.p2) }
    var crossed by remember { mutableIntStateOf(0) }
    var sown by remember { mutableIntStateOf(0) }
    var n by remember { mutableIntStateOf(100) }
    var seed by remember { mutableIntStateOf(1) }
    val canChoose = free || s.choosable
    val options = if (blood) BLOOD else trait!!.genotypes()
    val seeds = remember(p1, p2, n, seed) { Genetics.sow(p1, p2, n, seed) }
    LaunchedEffect(p1, p2, trait, blood) { crossed = 0; sown = 0 }
    val looks: (String) -> String = { g -> if (blood) Blood.group(g) else trait!!.look(g) }
    val colorOf: (String) -> Color = { g -> if (blood) bloodColor(g) else trait!!.color(g).color() }
    val recessiveShare = if (blood) null else Genetics.share(trait!!, p1, p2, trait!!.recessiveLook)
    val goalOk = s.goalRecessive?.let { recessiveShare != null && kotlin.math.abs(recessiveShare - it) < 1e-9 } ?: true
    GoalWhen(!free && crossed > 0 && sown > 0 && goalOk, onGoal)
    val grow = progressOf(sown, 1600)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (free) ChipRow(listOf<Trait?>(null) + Trait.entries, if (blood) null else trait, { it?.let { t -> "${t.emoji} ${t.organism}" } ?: "🩸 Blutgruppen" }, {
            blood = it == null; trait = it ?: trait ?: Trait.ERBSE
            val opts = if (it == null) BLOOD else it.genotypes(); p1 = opts[1.coerceAtMost(opts.size - 1)]; p2 = opts[1.coerceAtMost(opts.size - 1)]
        })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Parent("Elternteil 1", p1, if (blood) null else trait)
            Text("×", fontSize = 30.sp, color = E.Sun, fontWeight = FontWeight.Black)
            Parent("Elternteil 2", p2, if (blood) null else trait)
        }
        if (canChoose) {
            Text("Erbanlagen von Elternteil 1:", color = E.Muted, fontSize = 12.sp)
            ChipRow(options, p1, { it }, { p1 = it }, Modifier.testTag("eltern1"))
            Text("Erbanlagen von Elternteil 2:", color = E.Muted, fontSize = 12.sp)
            ChipRow(options, p2, { it }, { p2 = it }, Modifier.testTag("eltern2"), color = E.Pink)
        }
        if (!blood) Text("${trait!!.big} = ${trait!!.dominantLook}" + (if (trait!!.intermediate != null) ", ${trait!!.small} = ${trait!!.recessiveLook} – keins ist stärker (gemischt: ${trait!!.intermediate})" else " ist dominant, ${trait!!.small} = ${trait!!.recessiveLook} ist rezessiv (versteckt sich)"), color = E.Muted, fontSize = 12.sp)
        else Text("A und B sind dominant über 0. A und B zusammen ergeben AB.", color = E.Muted, fontSize = 12.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton({ crossed++; sfx?.play(Sound.POP) }, Modifier.testTag("kreuzen"), color = E.Violet) { Text("✖️ Kreuzen") }
            GlowButton({ seed++; sown++; sfx?.play(Sound.BUBBLE) }, Modifier.testTag("aussaeen"), color = E.Green, enabled = crossed > 0) { Text(if (blood) "👶 $n Kinder" else "🌱 $n säen") }
        }
        if (crossed > 0) {
            // Kreuzungsquadrat
            val sq = Genetics.square(p1, p2)
            Panel(border = E.Violet) {
                Text("Kreuzungsquadrat", fontWeight = FontWeight.Bold, color = E.Violet)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(34.dp))
                    p2.forEach { a -> Text("$a", Modifier.weight(1f), color = E.Pink, textAlign = TextAlign.Center, fontWeight = FontWeight.Black) }
                }
                sq.forEachIndexed { i, row ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${p1[i]}", Modifier.width(34.dp), color = E.Mint, textAlign = TextAlign.Center, fontWeight = FontWeight.Black)
                        row.forEachIndexed { j, g ->
                            Box(Modifier.weight(1f).padding(3.dp).height(46.dp).popIn((i * 2 + j) * 120).clip(RoundedCornerShape(12.dp)).background(colorOf(g).copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
                                Text("$g\n${looks(g)}", textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF160F2E), lineHeight = 13.sp)
                            }
                        }
                    }
                }
                val shares = Genetics.cross(p1, p2).entries.groupBy({ looks(it.key) }, { it.value }).mapValues { it.value.sum() }
                Text("Erwartet: " + shares.entries.joinToString(" · ") { "${it.key} ${pct(it.value)}" }, color = E.Text, fontSize = 13.sp, modifier = Modifier.testTag("erwartet"))
            }
        }
        if (sown > 0) {
            val visible = (seeds.size * grow).toInt()
            Canvas(Modifier.fillMaxWidth().height(if (n > 100) 170.dp else 130.dp).testTag("beet")) {
                drawRoundRect(Color(0xFF5A3A22), Offset.Zero, size, CornerRadius(16 * density))
                val cols = if (n <= 8) 8 else if (n <= 100) 20 else 40
                val rows = (n + cols - 1) / cols
                val cw = size.width / cols; val ch = size.height / rows
                val r = minOf(cw, ch) * 0.38f
                for (k in 0 until visible) {
                    val c = Offset(cw * (k % cols + 0.5f), ch * (k / cols + 0.5f))
                    drawCircle(colorOf(seeds[k]), r, c)
                    if (n <= 100) drawCircle(Color.White.copy(alpha = 0.35f), r * 0.35f, c + Offset(-r * 0.3f, -r * 0.3f))
                }
            }
            val counts = seeds.groupingBy(looks).eachCount()
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                counts.entries.sortedByDescending { it.value }.forEach { (look, k) -> Readout(look, "$k von $n", modifier = Modifier.testTag("zaehlung")) }
            }
            if (s.goalRecessive != null && !free && !goalOk) Text("🐝 Blubb: Noch nicht ${pct(s.goalRecessive!!)} ${trait?.recessiveLook} – probier andere Eltern!", color = E.Sun, fontSize = 13.sp)
        }
        Text("Wie viele Samen?", color = E.Muted, fontSize = 12.sp)
        ChipRow(listOf(8, 100, 1000), n, { "$it" }, { n = it; if (sown > 0) { seed++; sown++ } }, color = E.Green)
    }
}

// ───────────────────────────── Osmose-Gläser ─────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OsmosisLab(s: OsmosisSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val tm = rememberTextMeasurer()
    var own by remember { mutableDoubleStateOf(3.0) }
    val salts = if (free) s.salts + own else s.salts
    var run by remember { mutableIntStateOf(0) }
    var pick by remember { mutableStateOf(salts.last()) }
    val t = progressOf(run, 2500)
    LaunchedEffect(t >= 1f) { if (t >= 1f && run > 0) sfx?.play(Sound.DING) }
    GoalWhen(!free && t >= 1f, onGoal)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Tipp ein Glas an, um es unter die Lupe zu nehmen.", color = E.Muted, fontSize = 13.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.testTag("salzglaeser")) {
            salts.forEach { salt ->
                val change = Osmosis.massChange(salt) * t
                Column(Modifier.width(78.dp).clip(RoundedCornerShape(14.dp)).background(if (pick == salt) E.Panel else E.Deep)
                    .border(2.dp, if (pick == salt) E.Sun else E.Line, RoundedCornerShape(14.dp)).clickable { pick = salt; sfx?.play(Sound.TAP) }.padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(Modifier.size(60.dp, 86.dp)) {
                        val w = size.width; val h = size.height
                        drawRoundRect(Color(0xFF9FD8FF).copy(alpha = 0.18f + (salt / 10.0).toFloat().coerceAtMost(0.4f)), Offset(w * 0.1f, h * 0.2f), Size(w * 0.8f, h * 0.78f), CornerRadius(8 * density))
                        // Salzkörnchen
                        repeat((salt * 3).toInt().coerceAtMost(30)) { k -> drawCircle(Color.White.copy(alpha = 0.7f), 1.3f * density, Offset(w * (0.18f + (k * 37 % 64) / 100f), h * (0.3f + (k * 53 % 66) / 100f))) }
                        // Kartoffelstreifen: wird länger und praller oder kürzer und schlapp
                        val len = h * 0.62f * (1 + change.toFloat() / 60f)
                        val bend = if (change < 0) (-change.toFloat() / 25f) * w * 0.35f else 0f
                        val x0 = w * 0.5f; val y0 = h * 0.28f
                        val path = Path().apply { moveTo(x0, y0); quadraticTo(x0 + bend, y0 + len * 0.5f, x0 + bend * 1.6f, y0 + len) }
                        drawPath(path, Color(0xFFF2D48A), style = Stroke(w * (0.16f + change.toFloat() / 300f), cap = androidx.compose.ui.graphics.StrokeCap.Round))
                        drawRoundRect(Color.White.copy(alpha = 0.7f), Offset(w * 0.1f, h * 0.2f), Size(w * 0.8f, h * 0.78f), CornerRadius(8 * density), style = Stroke(2 * density))
                    }
                    Text("${Fmt.num(salt, 1)} % Salz", color = E.Text, fontSize = 11.sp)
                    Text(if (run > 0) (if (change >= 0) "+" else "") + Fmt.num(change, 1) + " %" else "–", color = if (change > 0.05) E.Green else if (change < -0.05) E.Pink else E.Muted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
        GlowButton({ run++; sfx?.play(Sound.WHOOSH) }, Modifier.testTag("warten"), color = E.Sun) { Text(if (run == 0) "⏳ 30 Minuten warten" else "⏳ Nochmal von vorn") }
        // Mikroskop: Zellen mit Saftraum (Vakuole)
        if (s.onion || free) {
            val change = Osmosis.massChange(pick) * t
            val k = (1f + change.toFloat() / 30f).coerceIn(0.35f, 1.05f)
            Canvas(Modifier.fillMaxWidth().height(150.dp).testTag("mikroskop")) {
                val w = size.width; val h = size.height
                drawCircle(Color(0xFF0B0820), minOf(w, h) * 0.5f, Offset(w / 2, h / 2))
                val rad = minOf(w, h) * 0.5f
                val cw = rad * 0.42f; val ch = rad * 0.26f
                for (row in -4..4) for (col in -4..4) {
                    val ox = w / 2 + col * cw + (if (row % 2 != 0) cw / 2 else 0f) - cw / 2; val oy = h / 2 + row * ch - ch / 2
                    val c = Offset(ox + cw / 2, oy + ch / 2)
                    if ((c - Offset(w / 2, h / 2)).getDistance() > rad - cw * 0.45f) continue
                    drawRect(Color(0xFF7ED957).copy(alpha = 0.7f), Offset(ox, oy), Size(cw, ch), style = Stroke(2 * density))
                    drawRoundRect(Color(0xFFB0306F).copy(alpha = 0.85f), Offset(c.x - cw * 0.42f * k, c.y - ch * 0.42f * k), Size(cw * 0.84f * k, ch * 0.84f * k), CornerRadius(10 * density * k))
                }
                label(tm, "🔬 ${Fmt.num(pick, 1)} %", Offset(w * 0.12f, h * 0.1f), 12.sp, E.Muted)
            }
            Text(when (Osmosis.direction(pick)) { 1 -> "Wasser strömt in die Zellen – sie werden prall."; -1 -> "Wasser verlässt die Zellen – der violette Saftraum schrumpft (Plasmolyse)."; else -> "Innen und außen gleich viel gelöst – fast nichts passiert." },
                color = E.Text, fontSize = 13.sp)
        }
        if (free) Panel(border = E.Sun) {
            Text("Eigenes Glas: wie viel Salz?", fontWeight = FontWeight.Bold, color = E.Sun)
            NumberField("Salz", "%", 0.0, 26.0, own, { own = it; pick = it })
        }
    }
}

// ───────────────────────────── Nährstoff-Detektiv ─────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodLab(s: FoodSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    var test by remember { mutableStateOf(s.tests.first()) }
    val done = remember { mutableStateMapOf<Pair<Food, FoodTest>, Boolean>() }
    var last by remember { mutableStateOf<Food?>(null) }
    GoalWhen(!free && s.foods.all { f -> s.tests.all { (f to it) in done } }, onGoal)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (s.tests.size > 1) ChipRow(s.tests, test, { "${it.emoji} ${it.title}" }, { test = it }, color = E.Violet)
        Text("${test.emoji} ${test.title}: positiv = ${test.positive}. Tipp ein Lebensmittel an!", color = E.Muted, fontSize = 13.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.testTag("lebensmittel")) {
            s.foods.forEach { f ->
                val tested = (f to test) in done
                val col by animateColorAsState(if (!tested) E.Deep else if (test.detects(f)) test.posColor.color() else test.negColor.color().copy(alpha = 0.45f), tween(900), label = "f")
                Column(Modifier.width(92.dp).clip(RoundedCornerShape(16.dp)).background(E.Deep).border(2.dp, if (tested && test.detects(f)) E.Green else E.Line, RoundedCornerShape(16.dp))
                    .clickable { done[f to test] = true; last = f; sfx?.play(if (test.detects(f)) Sound.AHA else Sound.BUBBLE) }.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(54.dp).clip(CircleShape).background(col), contentAlignment = Alignment.Center) { Text(f.emoji, fontSize = 28.sp) }
                    Text(f.title, color = E.Text, fontSize = 12.sp, maxLines = 1)
                    Text(if (!tested) "antippen" else if (test.detects(f)) "✓ ${test.positive}" else test.negative, color = if (tested && test.detects(f)) E.Green else E.Muted, fontSize = 10.sp, textAlign = TextAlign.Center)
                }
            }
        }
        last?.let { f -> Panel(border = E.Mint) { Text("${f.emoji} ${f.title}", fontWeight = FontWeight.Bold, color = E.Mint); Text(f.note, color = E.Text, fontSize = 13.sp) } }
        if (done.isNotEmpty()) GlowButton({ done.clear(); last = null; sfx?.play(Sound.WHOOSH) }, color = E.Sky) { Text("🧽 Neue Proben") }
    }
}

// ───────────────────────────── Verdauungs-Labor (Enzym) ─────────────────────────────

@Composable
fun EnzymeLab(s: EnzymeSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    var saliva by remember { mutableStateOf(s.saliva) }
    var own by remember { mutableDoubleStateOf(30.0) }
    val temps = if (free) s.temps + own else s.temps
    var run by remember { mutableIntStateOf(0) }
    val t = progressOf(run, 6000)
    val minutes = 20.0 * t
    GoalWhen(!free && t >= 1f, onGoal)
    LaunchedEffect(t >= 1f) { if (t >= 1f && run > 0) sfx?.play(Sound.DING) }
    val starch = Color(0xFF1E1A4A); val iod = Color(0xFFC07A2C)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Jedes Röhrchen: Stärkebrei + Iod (blau-schwarz) ${if (saliva) "+ Speichel 👅" else "– ohne Speichel"}.", color = E.Muted, fontSize = 13.sp)
        Row(Modifier.fillMaxWidth().testTag("roehrchen"), horizontalArrangement = Arrangement.SpaceEvenly) {
            temps.forEach { temp ->
                val left = Enzyme.starchLeft(temp, minutes, saliva)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(Modifier.size(40.dp, 110.dp)) {
                        val w = size.width; val h = size.height
                        drawRoundRect(lerp(iod, starch, left.toFloat()), Offset(w * 0.2f, h * 0.25f), Size(w * 0.6f, h * 0.72f), CornerRadius(w * 0.3f))
                        drawRoundRect(Color.White.copy(alpha = 0.8f), Offset(w * 0.2f, h * 0.05f), Size(w * 0.6f, h * 0.92f), CornerRadius(w * 0.3f), style = Stroke(2 * density))
                        if (temp >= 60 && saliva) repeat(3) { k -> drawCircle(Color.White.copy(alpha = 0.4f), 2.5f * density, Offset(w * (0.35f + 0.15f * k), h * (0.85f - ((t * 3 + k * 0.3f) % 1f) * 0.5f))) }
                    }
                    Text("${Fmt.num(temp, 0)} °C", color = if (temp >= 60) E.Red else if (temp < 15) E.Sky else E.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(if (left < 0.05) "weg!" else pct(left), color = if (left < 0.05) E.Green else E.Muted, fontSize = 11.sp)
                }
            }
        }
        Readout("Zeit", "${Fmt.num(minutes, 0)} min", E.Sun)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton({ run++; sfx?.play(Sound.BUBBLE) }, Modifier.testTag("start"), color = E.Mint) { Text(if (run == 0) "▶️ Start" else "🔁 Nochmal") }
        }
        Text("Prozent = wie viel Stärke noch da ist. Ist sie weg, zeigt Iod wieder seine braune Farbe.", color = E.Muted, fontSize = 12.sp)
        if (free) Panel(border = E.Sun) {
            ChipRow(listOf(true, false), saliva, { if (it) "👅 mit Speichel" else "🚫 ohne Speichel" }, { saliva = it; run = 0 })
            NumberField("Eigene Temperatur", "°C", 0.0, 100.0, own, { own = it })
        }
    }
}

// ───────────────────────────── Wasserpest (Fotosynthese) ─────────────────────────────

@Composable
fun PhotoLab(s: PhotoSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val tm = rememberTextMeasurer()
    val now by rememberSeconds()
    var d by remember { mutableDoubleStateOf(s.distances.first()) }
    var co2 by remember { mutableStateOf(!s.co2Choice) }
    var lamp by remember { mutableStateOf(true) }
    val seen = remember { mutableStateListOf<Triple<Double, Boolean, Boolean>>() }
    val rate = Photosynthesis.bubbles(d, co2, lampOn = lamp)
    LaunchedEffect(d, co2, lamp) { kotlinx.coroutines.delay(1200); val k = Triple(d, co2, lamp); if (k !in seen) { seen += k; sfx?.play(Sound.DING, volume = 0.5f) } }
    val needed = buildList {
        s.distances.forEach { add(Triple(it, !s.co2Choice, true)) }
        if (s.co2Choice) s.distances.forEach { add(Triple(it, true, true)) }
        if (s.darkChoice) add(Triple(s.distances.first(), !s.co2Choice, false))
    }
    GoalWhen(!free && needed.all { it in seen }, onGoal)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(220.dp).testTag("wasserpest")) {
            val w = size.width; val h = size.height
            drawRoundRect(Color(0xFF0B0820), Offset.Zero, size, CornerRadius(20 * density))
            val gx = w * 0.72f; val gw = w * 0.22f
            // Lampe: Abstand 5..50 cm → links
            val lx = gx - gw * 0.6f - (d / 50.0).toFloat() * w * 0.55f
            if (lamp) drawCircle(Brush.radialGradient(listOf(E.Sun.copy(alpha = 0.45f), Color.Transparent), Offset(lx, h * 0.45f), w * 0.4f), w * 0.4f, Offset(lx, h * 0.45f))
            drawCircle(if (lamp) E.Sun else E.Muted, 16 * density, Offset(lx, h * 0.45f))
            label(tm, "💡", Offset(lx, h * 0.45f), 18.sp)
            label(tm, "${Fmt.num(d, 0)} cm", Offset((lx + gx - gw / 2) / 2, h * 0.88f), 12.sp, E.Muted)
            drawLine(E.Muted.copy(alpha = 0.5f), Offset(lx, h * 0.8f), Offset(gx - gw / 2, h * 0.8f), 1.5f * density)
            // Glas mit Wasser und Pflanze
            drawRect(Color(0xFF3DB8FF).copy(alpha = 0.25f), Offset(gx - gw / 2, h * 0.18f), Size(gw, h * 0.72f))
            drawRect(Color.White.copy(alpha = 0.7f), Offset(gx - gw / 2, h * 0.1f), Size(gw, h * 0.8f), style = Stroke(2 * density))
            for (k in 0..5) {
                val y = h * (0.85f - k * 0.1f)
                drawLine(Color(0xFF2EAD5B), Offset(gx, h * 0.88f), Offset(gx + sin(now + k) * 3f, y), 3 * density)
                drawLine(Color(0xFF6BEB7A), Offset(gx, y), Offset(gx + (if (k % 2 == 0) 1 else -1) * gw * 0.3f, y - h * 0.05f), 3 * density)
            }
            // Bläschen: Rate pro Minute → sichtbar
            val perSec = rate / 60.0 * 4
            val count = (perSec * 3).toInt().coerceAtMost(40)
            for (k in 0 until count) {
                val p = ((now * 0.33f + k / count.toFloat()) % 1f)
                drawCircle(Color.White.copy(alpha = 0.8f * (1 - p)), (2f + (k % 3)) * density, Offset(gx + ((k * 7) % 11 - 5) * density, h * 0.6f - p * h * 0.45f))
            }
            if (co2) label(tm, "CO₂", Offset(gx, h * 0.06f), 11.sp, E.Mint)
        }
        Readout("Bläschen pro Minute", Fmt.num(rate, 0), E.Mint, Modifier.fillMaxWidth().testTag("blaeschen"))
        if (free) ValueSlider("Abstand der Lampe", d.toFloat(), 5f..50f, "${Fmt.num(d, 0)} cm", { d = roundI(it).toDouble() }, color = E.Sun)
        else ChipRow(s.distances, d, { "${Fmt.num(it, 0)} cm" }, { d = it }, color = E.Sun, selectedIf = { it == d })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (s.co2Choice || free) ChipRow(listOf(false, true), co2, { if (it) "💨 mit Natron (CO₂)" else "ohne Natron" }, { co2 = it }, color = E.Mint)
        }
        if (s.darkChoice || free) ChipRow(listOf(true, false), lamp, { if (it) "💡 Lampe an" else "🌑 Lampe aus" }, { lamp = it }, color = E.Sun)
        if (!free) Text("Gemessen: ${seen.count { it in needed }} von ${needed.size}", color = E.Muted, fontSize = 12.sp)
    }
}

// ───────────────────────────── Räuber & Beute ─────────────────────────────

@Composable
fun EcoLab(s: EcoSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val tm = rememberTextMeasurer()
    var hares by remember { mutableDoubleStateOf(s.hares) }
    var lynx by remember { mutableDoubleStateOf(s.lynx) }
    var noLynx by remember { mutableStateOf(false) }
    var run by remember { mutableIntStateOf(0) }
    val ran = remember { mutableStateListOf<Boolean>() }
    val pts = remember(hares, lynx, noLynx) { PredatorPrey.simulate(hares, lynx, withLynx = !noLynx) }
    val t = progressOf(run, 5000)
    LaunchedEffect(t >= 1f) { if (t >= 1f && run > 0) { if (noLynx !in ran) ran += noLynx; sfx?.play(Sound.DING) } }
    GoalWhen(!free && (if (s.compareNoLynx) ran.containsAll(listOf(false, true)) else ran.isNotEmpty()), onGoal)
    val shown = if (run == 0) 1 else (pts.size * t).toInt().coerceIn(1, pts.size)
    val cur = pts[shown - 1]
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(210.dp).testTag("populationen")) {
            val w = size.width; val h = size.height
            drawRoundRect(Color(0xFF12301E), Offset.Zero, size, CornerRadius(20 * density))
            val maxH = maxOf(pts.maxOf { it.hares }, 1.0) * 1.1; val maxL = maxOf(pts.maxOf { it.lynx }, 1.0) * 1.1
            fun px(i: Int) = w * 0.05f + w * 0.9f * i / (pts.size - 1)
            val ph = Path(); val pl = Path()
            for (i in 0 until shown) {
                val yh = h * 0.92f - (pts[i].hares / maxH).toFloat() * h * 0.82f; val yl = h * 0.92f - (pts[i].lynx / maxL).toFloat() * h * 0.82f
                if (i == 0) { ph.moveTo(px(i), yh); pl.moveTo(px(i), yl) } else { ph.lineTo(px(i), yh); pl.lineTo(px(i), yl) }
            }
            drawPath(ph, E.Sun, style = Stroke(3 * density)); if (!noLynx) drawPath(pl, E.Orange.copy(alpha = 0.9f), style = Stroke(3 * density, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f * density, 6f * density))))
            label(tm, "🐇", Offset(px(shown - 1), h * 0.92f - (cur.hares / maxH).toFloat() * h * 0.82f), 18.sp)
            if (!noLynx) label(tm, "🐆", Offset(px(shown - 1), h * 0.92f - (cur.lynx / maxL).toFloat() * h * 0.82f), 18.sp)
            label(tm, "Jahre →", Offset(w * 0.88f, h * 0.96f), 10.sp, E.Muted)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Readout("🐇 Hasen", Fmt.num(cur.hares, 0), E.Sun, Modifier.weight(1f))
            Readout("🐆 Luchse", Fmt.num(cur.lynx, 0), E.Orange, Modifier.weight(1f))
            Readout("Jahr", Fmt.num(cur.year, 0), E.Muted, Modifier.weight(1f))
        }
        Text("Gelbe Linie: Hasen · gestrichelte Linie: Luchse (eigene Skala)", color = E.Muted, fontSize = 12.sp)
        if (s.compareNoLynx || free) ChipRow(listOf(false, true), noLynx, { if (it) "🌲 Wald ohne Luchse" else "🐆 mit Luchsen" }, { noLynx = it; run = 0 }, color = E.Orange)
        GlowButton({ run++; sfx?.play(Sound.WHOOSH) }, Modifier.testTag("zeit"), color = E.Green) { Text("⏩ 30 Jahre laufen lassen") }
        if (free) Panel(border = E.Sun) {
            Text("Eigener Wald", fontWeight = FontWeight.Bold, color = E.Sun)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NumberField("Hasen", "", 1.0, 1500.0, hares, { hares = it; run = 0 }, Modifier.weight(1f))
                NumberField("Luchse", "", 0.0, 200.0, lynx, { lynx = it; run = 0 }, Modifier.weight(1f))
            }
        }
    }
}

// ───────────────────────────── Trenn-Werkstatt ─────────────────────────────

private val MIXTURES = listOf(
    Mixture("Sand-Salz-Wasser", "🏖️", setOf(Component.SAND, Component.SALZ, Component.WASSER)),
    Mixture("Eisen-Schwefel-Gemisch", "🔩", setOf(Component.EISEN, Component.SCHWEFEL)),
    Mixture("Öl auf Wasser", "🌻", setOf(Component.OEL, Component.WASSER)),
    Mixture("Kies, Sand und Eisen", "🌑", setOf(Component.KIES, Component.SAND, Component.EISEN)),
    Mixture("Kaffee mit Krümeln", "☕", setOf(Component.KAFFEESATZ, Component.WASSER)),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SeparationLab(s: SeparationSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val tm = rememberTextMeasurer()
    val now by rememberSeconds()
    var mixture by remember { mutableStateOf(s.mixture) }
    var rest by remember(mixture) { mutableStateOf(mixture.parts) }
    val bowls = remember(mixture) { mutableStateListOf<Pair<Component, Method>>() }
    var msg by remember(mixture) { mutableStateOf("Wähle ein Verfahren!") }
    var shake by remember { mutableIntStateOf(0) }
    val fx = rememberBursts()
    val done = rest.size <= 1
    GoalWhen(!free && done, onGoal)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (free) ChipRow(MIXTURES, mixture, { "${it.emoji} ${it.title}" }, { mixture = it }, color = E.Pink)
        Canvas(Modifier.fillMaxWidth().height(170.dp).shake(shake).testTag("gemisch")) {
            val w = size.width; val h = size.height
            drawRoundRect(Color(0xFF0B0820), Offset.Zero, size, CornerRadius(20 * density))
            val bx = w * 0.3f; val bw = w * 0.36f
            val water = Component.WASSER in rest
            if (water) drawRect(Color(0xFF3DB8FF).copy(alpha = if (Component.SALZ in rest) 0.45f else 0.3f), Offset(bx - bw / 2, h * 0.35f), Size(bw, h * 0.55f))
            if (Component.OEL in rest) drawRect(Color(0xFFFFD84D).copy(alpha = 0.6f), Offset(bx - bw / 2, h * 0.27f), Size(bw, h * 0.1f))
            rest.filter { it != Component.WASSER && it != Component.OEL }.forEachIndexed { i, c ->
                val col = when (c) { Component.SAND -> Color(0xFFE8C77A); Component.SALZ -> Color.White; Component.EISEN -> Color(0xFF8A8A9A); Component.SCHWEFEL -> E.Sun; Component.KIES -> Color(0xFF6B5B4B); else -> Color(0xFF5A3A22) }
                repeat(if (c == Component.SALZ && water) 0 else 30) { k ->
                    val x = bx - bw / 2 + bw * ((k * 37 + i * 13) % 100) / 100f; val y = h * (0.88f - i * 0.07f) - ((k * 17) % 7) * density
                    drawCircle(col, (if (c == Component.KIES) 4.5f else 2f) * density, Offset(x, y))
                }
            }
            drawRect(Color.White.copy(alpha = 0.75f), Offset(bx - bw / 2, h * 0.15f), Size(bw, h * 0.77f), style = Stroke(2 * density))
            label(tm, mixture.emoji, Offset(bx, h * 0.08f), 18.sp)
            // Schalen mit Getrenntem
            bowls.forEachIndexed { i, (c, m) ->
                val cx = w * 0.62f + (i % 2) * w * 0.18f; val cy = h * (0.3f + (i / 2) * 0.4f)
                drawArc(E.Panel, 0f, 180f, true, Offset(cx - 26 * density, cy - 12 * density), Size(52 * density, 36 * density))
                label(tm, if (m == Method.VERDAMPFEN) "💨" else c.emoji, Offset(cx, cy), 18.sp)
                label(tm, c.title, Offset(cx, cy + 24 * density), 10.sp, E.Muted)
            }
            drawBursts(fx, now)
        }
        Text(msg, color = E.Text, fontSize = 14.sp, modifier = Modifier.testTag("trennmeldung"))
        Text("Noch im Glas: " + rest.joinToString(", ") { "${it.emoji} ${it.title}" }, color = E.Muted, fontSize = 12.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Method.entries.forEach { m ->
                GlowButton({
                    val e = Separation.extract(m, rest)
                    if (done) return@GlowButton
                    if (e.isEmpty()) { msg = "🙈 ${m.title} trennt hier nichts. ${m.how}"; shake++; sfx?.play(Sound.SOFT) }
                    else {
                        e.forEach { bowls += it to m }; rest = rest - e
                        msg = if (m == Method.VERDAMPFEN) "💨 Das Wasser verdampft – übrig bleibt ${rest.joinToString { it.title }}." else "✨ ${m.title}: ${e.joinToString { it.title }} ist getrennt!"
                        if (rest.size <= 1) { rest.forEach { bowls += it to m }; msg += " Alles getrennt! 🎉" }
                        fx.add(Burst(Offset(300f, 200f), listOf(E.Mint, E.Sun, E.Sky), 26, 160f, bowls.size), now); sfx?.play(Sound.POP)
                    }
                }, color = E.Violet) { Text("${m.emoji} ${m.title}", fontSize = 13.sp) }
            }
        }
        GlowButton({ rest = mixture.parts; bowls.clear(); msg = "Wähle ein Verfahren!"; sfx?.play(Sound.WHOOSH) }, color = E.Sky) { Text("🔁 Neu mischen") }
    }
}

// ───────────────────────────── Chromatografie ─────────────────────────────

@Composable
fun ChromaLab(s: ChromaSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val tm = rememberTextMeasurer()
    var run by remember { mutableIntStateOf(0) }
    val t = progressOf(run, 4500)
    GoalWhen(!free && t >= 1f, onGoal)
    LaunchedEffect(t >= 1f) { if (t >= 1f && run > 0) sfx?.play(Sound.WIN) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(240.dp).testTag("chromatografie")) {
            val w = size.width; val h = size.height
            drawRoundRect(Color(0xFF0B0820), Offset.Zero, size, CornerRadius(20 * density))
            drawRect(Color(0xFF3DB8FF).copy(alpha = 0.35f), Offset(0f, h * 0.88f), Size(w, h * 0.12f))
            val n = s.markers.size; val sw = w / (n + 1) * 0.7f
            val start = h * 0.8f; val top = h * 0.08f
            val front = start - (start - top) * t
            s.markers.forEachIndexed { i, m ->
                val cx = w * (i + 1) / (n + 1)
                drawRect(Color(0xFFF7F3E8), Offset(cx - sw / 2, top - 4 * density), Size(sw, h * 0.92f - top))
                drawRect(Color(0xFF9FD8FF).copy(alpha = 0.35f), Offset(cx - sw / 2, front), Size(sw, h * 0.92f - front))
                if (t < 0.05f) drawCircle(m.color.color(), 7 * density, Offset(cx, start))
                m.dyes.forEach { dye ->
                    val y = start - (start - front) * dye.rf.toFloat()
                    drawOval(dye.color.color().copy(alpha = 0.85f), Offset(cx - sw * 0.32f, y - 6 * density - 6 * density * t), Size(sw * 0.64f, 12 * density + 12 * density * t))
                }
                label(tm, m.title.substringBefore(" "), Offset(cx, h * 0.96f), 10.sp, Color(0xFF160F2E), bold = true)
            }
        }
        GlowButton({ run++; sfx?.play(Sound.SPLASH) }, Modifier.testTag("eintauchen"), color = E.Sky) { Text(if (run == 0) "💧 Papier ins Wasser stellen" else "🔁 Nochmal") }
        if (t >= 1f) Text(s.markers.joinToString("\n") { m -> "${m.title}: " + m.dyes.joinToString(" + ") { it.title } }, color = E.Text, fontSize = 13.sp)
        Text("Gelb klebt am wenigsten am Papier und wandert am weitesten, Blau klebt am stärksten.", color = E.Muted, fontSize = 12.sp)
    }
}

// ───────────────────────────── Malkasten ─────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaintLab(s: PaintSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val now by rememberSeconds()
    val drops = remember { mutableStateMapOf<Paint, Int>() }
    val mixed = Painting.mix(drops)
    val name = if (drops.isEmpty()) "noch leer" else Painting.name(mixed)
    val hit = s.targetName != null && drops.isNotEmpty() && name == s.targetName
    LaunchedEffect(hit) { if (hit) sfx?.play(Sound.WIN) }
    GoalWhen(!free && hit, onGoal)
    val col by animateColorAsState(Color(mixed.r.toFloat(), mixed.g.toFloat(), mixed.b.toFloat()), tween(500), label = "p")
    var swirl by remember { mutableFloatStateOf(-10f) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(170.dp).testTag("palette")) {
            val w = size.width; val h = size.height
            drawRoundRect(Color(0xFFF7F3E8), Offset.Zero, size, CornerRadius(26 * density))
            val c = Offset(w / 2, h / 2); val r = h * 0.38f
            drawCircle(Color(0xFFE5DFD0), r + 6 * density, c)
            drawCircle(if (drops.isEmpty()) Color(0xFFF0EBDD) else col, r, c)
            val age = now - swirl
            if (age in 0f..1.2f) repeat(8) { k ->
                val a = k * 0.785f + age * 6f
                drawCircle(Color.White.copy(alpha = 0.4f * (1 - age / 1.2f)), 5 * density, c + Offset(kotlin.math.cos(a), sin(a)) * (r * 0.6f * age))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Ergebnis: $name", color = E.Text, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).testTag("mischfarbe"))
            s.targetName?.let { Pill("Ziel: $it", if (hit) E.Green else E.Sun) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("farbtoepfe")) {
            s.paints.forEach { p ->
                val pc = Color(p.refl.r.toFloat(), p.refl.g.toFloat(), p.refl.b.toFloat())
                Column(Modifier.width(74.dp).clip(RoundedCornerShape(16.dp)).background(E.Deep).clickable {
                    drops[p] = (drops[p] ?: 0) + 1; swirl = now; sfx?.play(Sound.SPLASH, pitch = 0.9f + 0.1f * p.ordinal, volume = 0.6f)
                }.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(pc).border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape))
                    Text(p.title.substringBefore(" "), color = E.Text, fontSize = 12.sp)
                    Text("${drops[p] ?: 0} Tropfen", color = E.Muted, fontSize = 10.sp)
                }
            }
        }
        GlowButton({ drops.clear(); sfx?.play(Sound.WHOOSH) }, color = E.Sky) { Text("🚿 Pinsel auswaschen") }
        Text("Malfarben schlucken Licht: Je mehr Farben, desto dunkler. Beim Licht ist es umgekehrt!", color = E.Muted, fontSize = 12.sp)
    }
}
