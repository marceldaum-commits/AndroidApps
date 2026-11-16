package de.raumlicht.app.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.raumlicht.app.ui.fx.*
import de.raumlicht.app.ui.level.LevelScreen
import de.raumlicht.app.ui.level.Stepper
import de.raumlicht.app.ui.level.stageColor
import de.raumlicht.app.ui.lux.Lux
import de.raumlicht.app.ui.lux.LuxMood
import de.raumlicht.app.ui.stage.Stage
import de.raumlicht.app.ui.stage.StageController
import de.raumlicht.app.ui.stage.StageFrame
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.core.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

@Composable
fun Dial(label: String, value: String, sub: String, color: Color, modifier: Modifier) {
    Column(modifier.background(Q.Deep, RoundedCornerShape(14.dp)).border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text(label, color = Q.Muted, fontSize = 11.sp, maxLines = 1)
        Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 18.sp, maxLines = 1)
        if (sub.isNotEmpty()) Text(sub, color = Q.Text, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
fun Pick(text: String, on: Boolean, color: Color, tag: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Text(text, Modifier.testTag(tag).pressBounce(interaction).clip(RoundedCornerShape(12.dp)).background(if (on) color.copy(alpha = 0.3f) else Q.Deep)
        .border(if (on) 2.dp else 1.dp, if (on) color else Q.Muted.copy(alpha = 0.4f), RoundedCornerShape(12.dp)).clickable(interaction, null, onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 7.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Q.Text)
}

// ───────────────────────── Flug durch den Raum ─────────────────────────

/**
 * Geometrie aus der Fahrersicht: Man fliegt in Lux eine Gerade entlang – durch Ebenen hindurch und an Kugeln vorbei.
 * Das Cockpit zeigt Ort, Parameter t und den Abstand zur nächsten Ebene; an Durchstoßpunkten meldet sich Lux.
 */
@Composable
fun FlightScreen(onBack: () -> Unit, controller: StageController = remember { StageController() },
                 /** Zuletzt gebauter eigener Flug (siehe [Flights.custom]) – steht beim nächsten Mal wieder da */
                 custom: String = "", onCustom: (String) -> Unit = {}) {
    var chosen by rememberSaveable { mutableStateOf(Flights.all.first().id) }
    // Eigener Flug: fünf Zeilen mit Zahlenfeldern (Start, Richtung, zwei Ebenen, Kugel); gebaut wird auf Knopfdruck
    val first = remember { (custom.takeIf { Flights.custom(it).isSuccess } ?: Flights.EXAMPLE).split(';') }
    fun cells(i: Int, n: Int, fallback: List<String>) = first.getOrNull(i).orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }.takeIf { it.size == n } ?: fallback
    val fields = rememberSaveable(saver = androidx.compose.runtime.saveable.listSaver(save = { it.toList() }, restore = { it.toMutableStateList() })) {
        (cells(0, 3, listOf("0", "0", "1")) + cells(1, 3, listOf("2", "2", "1")) + cells(2, 4, listOf("1", "0", "0", "4")) + cells(3, 4, listOf("1", "1", "1", "16")) + cells(4, 4, listOf("4", "4", "3", "2"))).toMutableStateList() }
    var useE1 by rememberSaveable { mutableStateOf(first.getOrNull(2).orEmpty().isNotBlank()) }
    var useE2 by rememberSaveable { mutableStateOf(first.getOrNull(3).orEmpty().isNotBlank()) }
    var useBall by rememberSaveable { mutableStateOf(first.getOrNull(4).orEmpty().isNotBlank()) }
    var built by rememberSaveable { mutableStateOf(custom.takeIf { Flights.custom(it).isSuccess } ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var editing by rememberSaveable { mutableStateOf(built.isEmpty()) }
    val own = remember(built) { Flights.custom(built).getOrNull() }
    fun code() = listOf(fields.subList(0, 3), fields.subList(3, 6), if (useE1) fields.subList(6, 10) else emptyList(), if (useE2) fields.subList(10, 14) else emptyList(), if (useBall) fields.subList(14, 18) else emptyList())
        .joinToString(";") { it.joinToString(",") }
    fun build(): Boolean { val c = code(); Flights.custom(c).onFailure { error = it.message; return false }; error = null; built = c; onCustom(c); return true }
    val isOwn = chosen == Flights.CUSTOM
    val flight = (if (isOwn) own else Flights.byId(chosen)) ?: Flights.all.first()
    val id = if (isOwn) "eigener:$built" else chosen
    var u by rememberSaveable(id) { mutableFloatStateOf(0f) }
    var driving by remember(id) { mutableStateOf(false) }
    var inside by rememberSaveable { mutableStateOf(true) }
    var seen by remember(id) { mutableStateOf<FlightEvent?>(null) }
    val sfx = LocalSfx.current; val view = LocalBuzzer.current; val vib = LocalVibration.current
    val d = flight.data(u.toDouble())

    LaunchedEffect(id) { controller.setState(flight.scene()) }
    LaunchedEffect(id, u, inside) { controller.ride(u.toDouble(), inside) }
    DisposableEffect(controller) { onDispose { controller.rideOff() } }
    LaunchedEffect(driving, id) {
        var last = 0L
        while (driving) withFrameNanos { now ->
            val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f); last = now
            u = (u + 0.09f * dt).coerceAtMost(1f); if (u >= 1f) driving = false
        }
    }
    LaunchedEffect(d.t) {
        val e = flight.eventNear(d.t)
        if (e != null && e != seen) { seen = e; sfx?.play(Sound.HIT); view.buzz(Buzz.HIT, vib); controller.party(1.0) }
        else if (e == null && seen != null && abs(seen!!.t - d.t) > 0.3) seen = null
    }

    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding()) {
        Bar("Flug durch den Raum", onBack)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pick("✏️ Eigener Flug", isOwn, Q.Yellow, "flug-eigener") { chosen = Flights.CUSTOM; editing = own == null }
            Flights.all.forEach { f -> Pick(f.title, f.id == chosen, Q.Cyan, "flug-${f.id}") { chosen = f.id } }
        }
        if (isOwn && editing) Column(Modifier.fillMaxWidth().heightIn(max = 330.dp).verticalScroll(rememberScrollState()).padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            @Composable fun line(label: String, from: Int, n: Int, tag: String, on: Boolean? = null, toggle: () -> Unit = {}, last: String = "") = Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (on == null) Text(label, Modifier.width(84.dp), color = Q.Muted, fontSize = 13.sp) else Box(Modifier.width(84.dp)) { Pick(label, on, Q.Violet, "$tag-an", toggle) }
                if (on != false) (0 until n).forEach { k ->
                    if (k == 3) Text(last, color = Q.Muted, fontSize = 13.sp)
                    Bound(fields[from + k], "$tag-$k") { fields[from + k] = it; error = null }
                }
            }
            line("Start A", 0, 3, "start"); line("Richtung", 3, 3, "richtung")
            line("Ebene 1", 6, 4, "e1", useE1, { useE1 = !useE1 }, "="); line("Ebene 2", 10, 4, "e2", useE2, { useE2 = !useE2 }, "="); line("Kugel M", 14, 4, "kugel", useBall, { useBall = !useBall }, "r")
            error?.let { Text(it, Modifier.testTag("flug-fehler"), color = Q.Orange, fontSize = 12.sp) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ebene: Normalenvektor und rechte Seite, z. B. 1 | 1 | 1 = 16 für x₁ + x₂ + x₃ = 16.", Modifier.weight(1f), color = Q.Muted, fontSize = 11.sp)
                NeonButton({ if (build()) { editing = false; sfx?.play(Sound.CELL) } }, Modifier.testTag("flug-bauen"), color = Q.Yellow) { Text("Flug bauen", fontWeight = FontWeight.Black) }
            }
        }
        if (isOwn && !editing) Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp).clip(RoundedCornerShape(12.dp)).background(Q.Deep).border(1.5.dp, Q.Yellow, RoundedCornerShape(12.dp))
            .clickable { editing = true }.padding(horizontal = 12.dp, vertical = 9.dp).testTag("flug-zeile"), verticalAlignment = Alignment.CenterVertically) {
            Text("g: ${flight.text}", Modifier.weight(1f).testTag("flug-term"), color = Q.Text, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text("✏️ ändern", color = Q.Yellow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        if (!isOwn) Text("g: ${flight.text}", Modifier.padding(horizontal = 14.dp, vertical = 2.dp), color = Q.Cyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp)) {
            Stage(controller, Modifier.fillMaxSize())
            StageFrame(Q.Cyan)
            val s = seen
            if (s != null) Row(Modifier.align(Alignment.TopCenter).padding(12.dp).background(Q.Space.copy(alpha = 0.85f), RoundedCornerShape(16.dp)).border(1.5.dp, stageColor(s.color), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Lux(LuxMood.WOW, size = 40.dp)
                Column(Modifier.padding(start = 8.dp)) { Text(s.title, fontWeight = FontWeight.Black, color = stageColor(s.color)); Text(s.text, fontSize = 12.sp, color = Q.Text) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Dial("Parameter t", Geo.num(d.t), "", Q.Yellow, Modifier.weight(0.8f).testTag("flug-t"))
            Dial("Ort", "(${Geo.num(d.pos.x)}|${Geo.num(d.pos.y)}|${Geo.num(d.pos.z)})", "", Q.Cyan, Modifier.weight(1.6f))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Dial("Nächste Ebene: ${d.nearest}", "Abstand ${Geo.num(d.distance)}", if (d.distance < 0.05) "du bist genau in der Ebene" else "", if (d.distance < 0.05) Q.Green else Q.Violet, Modifier.weight(1f).testTag("flug-abstand"))
        }
        Slider(u, { u = it; driving = false }, Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("flug-regler"),
            colors = SliderDefaults.colors(thumbColor = Q.Yellow, activeTrackColor = Q.Cyan, inactiveTrackColor = Q.Panel))
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeonButton({ inside = !inside }, color = Q.Violet) { Text(if (inside) "👁 von außen" else "🚀 einsteigen", fontWeight = FontWeight.Bold) }
            NeonButton({ if (u >= 0.999f) u = 0f; driving = !driving; if (driving) sfx?.play(Sound.LAUNCH) }, Modifier.weight(1f).testTag("fliegen"), color = Q.Green) {
                Text(if (driving) "⏸ Anhalten" else if (u >= 0.999f) "↻ Noch einmal" else "▶ Losfliegen", fontWeight = FontWeight.Black)
            }
        }
    }
}

// ───────────────────────── Galton-Brett ─────────────────────────

/** Zufall zum Anfassen: Kugeln fallen durch das Nagelbrett, in den Fächern wächst die Binomialverteilung. */
@Composable
fun GaltonScreen(onBack: () -> Unit, controller: StageController = remember { StageController() }, random: Random = remember { Random(nowNanos()) }) {
    var rows by rememberSaveable { mutableIntStateOf(8) }
    val board = remember(rows) { Galton(rows) }
    var tenths by rememberSaveable { mutableIntStateOf(5) }
    var counts by remember(rows) { mutableStateOf(List(board.rows + 1) { 0 }) }
    var ball by remember { mutableStateOf<V3?>(null) }
    var running by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope(); val sfx = LocalSfx.current
    val p = tenths / 10.0; val total = counts.sum()
    LaunchedEffect(counts, ball, tenths, rows) { controller.setState(board.scene(counts, p, ball)) }

    fun run(n: Int) {
        if (running) return
        running = true
        scope.launch {
            repeat(n) { i ->
                val (bin, path) = board.drop(p, random)
                // Die ersten Kugeln fallen sichtbar von Nagel zu Nagel, danach geht es schneller
                if (i < 4) for (pt in path) { ball = pt; delay(70) } else { ball = path.last(); delay(35) }
                counts = counts.toMutableList().also { it[bin]++ }
                if (i < 4) sfx?.play(Sound.TICK, 0.9f + bin * 0.05f)
            }
            ball = null; running = false; sfx?.play(Sound.HIT); controller.party(1.5)
        }
    }

    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding()) {
        Bar("Galton-Brett", onBack) { Text("$total Kugeln", color = Q.Yellow, fontWeight = FontWeight.Bold) }
        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp)) { Stage(controller, Modifier.fillMaxSize()); StageFrame(Q.Pink) }
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Dial("Reihen n", "${board.rows}", "", Q.Text, Modifier.weight(0.7f))
            Dial("Erwartungswert μ", Geo.num(board.mean(p)), "n · p", Q.Yellow, Modifier.weight(1f).testTag("galton-mu"))
            Dial("Streuung σ", Geo.num(board.sigma(p)), "√(n·p·(1−p))", Q.Cyan, Modifier.weight(1f))
        }
        Text(if (total == 0) "Stell p ein – die Wahrscheinlichkeit, dass eine Kugel am Nagel nach rechts fällt. Dann lass Kugeln regnen."
            else "Blau: was gefallen ist. Gelb: was die Binomialverteilung vorhersagt. Je mehr Kugeln, desto besser passt es.",
            Modifier.padding(horizontal = 14.dp), color = Q.Muted, fontSize = 12.sp)
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("n", color = Q.Text, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Stepper(null, rows, "$rows Reihen", Q.Text, "n", Modifier.weight(0.8f), canDown = rows > Galton.ROWS.first && !running, canUp = rows < Galton.ROWS.last && !running) { d -> rows += d }
            Text("p", color = Q.Pink, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Stepper(null, tenths, "${tenths * 10} % rechts", Q.Pink, "p", Modifier.weight(1f), canDown = tenths > 1 && !running, canUp = tenths < 9 && !running) { d ->
                tenths += d; counts = List(board.rows + 1) { 0 }       // neue Wahrscheinlichkeit, neues Experiment
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeonButton({ counts = List(board.rows + 1) { 0 } }, color = Q.Muted, enabled = !running && total > 0) { Text("Leeren") }
            NeonButton({ run(10) }, Modifier.testTag("galton-10"), color = Q.Cyan, enabled = !running) { Text("10", fontWeight = FontWeight.Bold) }
            NeonButton({ run(100) }, Modifier.weight(1f).testTag("galton-100"), color = Q.Green, enabled = !running) { Text(if (running) "es regnet …" else "▶ 100 Kugeln", fontWeight = FontWeight.Black) }
        }
    }
}

// ───────────────────────── Hilfsmittelfreier Teil ─────────────────────────

/** Sechs kurze Aufgaben quer durch Geometrie, Analysis und Stochastik – ohne Tipp, wie im Pflichtteil. */
@Composable
fun QuickScreen(onSolved: (Level, Int, Int) -> Unit, onBack: () -> Unit, seed: Long = remember { nowMillis() % 1_000_000 }) {
    val pack = remember(seed) { Quick.pick(seed) }
    var idx by rememberSaveable { mutableIntStateOf(-1) }
    var hits by rememberSaveable { mutableIntStateOf(0) }
    when {
        idx in pack.indices -> {
            val level = pack[idx]; val chapter = Levels.chapters.first { it.number == level.chapter }
            LevelScreen(level, chapter, false, {}, onSolved = { s, a -> if (s == 3) hits++; onSolved(level, s, a) }, onNext = { idx++ }, onBack = onBack,
                caption = "✍️ Ohne Hilfsmittel · ${idx + 1}/${pack.size} · ${chapter.area.title}", badge = "🎯 $hits", strict = true)
        }
        else -> Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            Bar("Ohne Hilfsmittel", onBack)
            Spacer(Modifier.height(24.dp)); Lux(if (idx < 0) LuxMood.IDLE else if (hits >= 4) LuxMood.HAPPY else LuxMood.THINK, size = 130.dp)
            if (idx < 0) {
                Text("Wie im Pflichtteil", fontSize = 26.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp))
                Text("Sechs kurze Aufgaben: je zwei aus Geometrie, Analysis und Stochastik. Kein Tipp, kein Taschenrechner – nur du und dein Kopf.", Modifier.padding(horizontal = 28.dp, vertical = 12.dp), textAlign = TextAlign.Center)
                NeonButton({ idx = 0; hits = 0 }, Modifier.padding(16.dp), color = Q.Green) { Text("▶ Los geht's", fontWeight = FontWeight.Black) }
            } else {
                Text("$hits von ${pack.size}", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Q.Yellow, modifier = Modifier.padding(top = 8.dp))
                Text("beim ersten Versuch – ganz ohne Hilfe", color = Q.Muted)
                NeonButton(onBack, Modifier.padding(16.dp), color = Q.Green) { Text("Fertig ✓", fontWeight = FontWeight.Black) }
            }
        }
    }
}

// ───────────────────────── Fehler-Tagebuch ─────────────────────────

/** Ein Eintrag im Fehler-Tagebuch: welcher typische Fehler, bei welcher Aufgabe, wie oft. */
data class DiaryEntry(val count: Int, val levelId: String, val message: String) {
    companion object {
        fun parse(text: String): List<DiaryEntry> = text.lines().mapNotNull { line -> line.split("\t").takeIf { it.size == 3 }?.let { DiaryEntry(it[0].toIntOrNull() ?: return@mapNotNull null, it[1], it[2]) } }
        fun encode(entries: List<DiaryEntry>) = entries.joinToString("\n") { "${it.count}\t${it.levelId}\t${it.message}" }
        /** Zählt den Fehler hoch (oder legt ihn an); behält die 30 häufigsten */
        fun add(text: String, levelId: String, message: String): String {
            val list = parse(text).toMutableList(); val clean = message.replace("\t", " ").replace("\n", " ")
            val i = list.indexOfFirst { it.message == clean }
            if (i >= 0) list[i] = list[i].copy(count = list[i].count + 1) else list += DiaryEntry(1, levelId, clean)
            return encode(list.sortedByDescending { it.count }.take(30))
        }
    }
}

@Composable
fun DiaryScreen(entries: List<DiaryEntry>, onLevel: (String) -> Unit, onClear: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding()) {
        Bar("Fehler-Tagebuch", onBack) { if (entries.isNotEmpty()) Text("Leeren", Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onClear).padding(8.dp), color = Q.Muted, fontSize = 13.sp) }
        if (entries.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Lux(LuxMood.HAPPY, size = 120.dp)
                Text("Noch nichts notiert", fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 10.dp))
                Text("Passiert dir ein typischer Fehler – falsch herum abgezogen, Kosinus statt Sinus, das Drittel vergessen –, schreibe ich ihn hier auf. So siehst du, worauf du achten musst.", textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
            }
            return@Column
        }
        Text("Deine häufigsten Fehler – das ist keine Strafe, sondern deine persönliche Checkliste.", Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = Q.Muted, fontSize = 12.sp)
        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(entries) { i, e ->
                val level = Levels.byId(e.levelId); val c = if (i < 3) Q.Orange else Q.Muted
                Column(Modifier.fillMaxWidth().background(Q.Deep, RoundedCornerShape(16.dp)).border(1.dp, c.copy(alpha = 0.6f), RoundedCornerShape(16.dp)).padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${e.count}×", color = c, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        Text(level?.let { l -> "${Levels.chapters.first { it.number == l.chapter }.title} · ${l.title}" } ?: "Übung", Modifier.weight(1f).padding(start = 10.dp), color = Q.Muted, fontSize = 12.sp, maxLines = 1)
                        if (level != null) Pick("▶ Üben", false, Q.Green, "tagebuch-${e.levelId}") { onLevel(e.levelId) }
                    }
                    Text(e.message, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// ───────────────────────── Kalender der Tagesaufgaben ─────────────────────────

@Composable
fun CalendarScreen(today: Long, days: Set<Long>, onToday: () -> Unit, onBack: () -> Unit) {
    val grid = remember(today) { Calendar.grid(today) }
    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        Bar("Kalender", onBack)
        Text("${days.size} Tagesaufgaben gelöst · längste Serie ${Calendar.bestRun(days)}", color = Q.Yellow, fontWeight = FontWeight.Bold)
        Text("Verpasste Tage kosten nichts – jeder gelöste Tag leuchtet.", color = Q.Muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 10.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So").forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, color = Q.Muted, fontSize = 12.sp) }
        }
        grid.forEachIndexed { w, week ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEachIndexed { i, day ->
                    val done = day != null && day in days
                    var shown by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { delay(18L * (w * 7 + i)); shown = true }
                    val s by animateFloatAsState(if (shown) 1f else 0.4f, spring(dampingRatio = 0.5f, stiffness = 300f), label = "tag")
                    Box(Modifier.weight(1f).aspectRatio(1f).graphicsLayer { scaleX = s; scaleY = s }.clip(RoundedCornerShape(12.dp))
                        .background(when { day == null -> Color.Transparent; done -> Q.Orange.copy(alpha = 0.45f); else -> Q.Deep })
                        .border(if (day == today) 2.dp else 1.dp, when { day == null -> Color.Transparent; day == today -> Q.Text; done -> Q.Orange; else -> Q.Muted.copy(alpha = 0.3f) }, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center) {
                        if (day != null) Text(if (done) "🎟️" else "${Calendar.dayOfMonth(day)}", fontSize = if (done) 18.sp else 13.sp, color = if (day == today) Q.Text else Q.Muted)
                    }
                }
            }
        }
        NeonButton(onToday, Modifier.padding(18.dp), color = if (today in days) Q.Muted else Q.Green) { Text(if (today in days) "Heute schon geschafft ✓" else "▶ Tagesaufgabe von heute", fontWeight = FontWeight.Bold) }
    }
}
