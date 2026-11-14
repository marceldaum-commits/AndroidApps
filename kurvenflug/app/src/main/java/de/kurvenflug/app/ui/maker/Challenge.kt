package de.kurvenflug.app.ui.maker

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kurvenflug.app.ui.flight.LevelScreen
import de.kurvenflug.app.ui.flight.SpaceCanvas
import de.kurvenflug.app.ui.flight.SpaceView
import de.kurvenflug.app.ui.fx.*
import de.kurvenflug.app.ui.theme.Q
import de.kurvenflug.app.ui.volt.Volt
import de.kurvenflug.app.ui.volt.VoltMood
import de.kurvenflug.core.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Lehrkraft stellt eine Challenge zusammen: eingebaute Level + eigene Level, Titel, Code teilen. */
@Composable
fun ChallengeComposer(myLevels: List<String>, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var title by rememberSaveable { mutableStateOf("") }
    var picked by rememberSaveable { mutableStateOf(listOf<String>()) }   // „r:2-6“ oder „c:<code>“
    BackHandler { onBack() }
    fun toggle(k: String) { picked = if (k in picked) picked - k else if (picked.size < 10) picked + k else picked }
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Q.Text) }
            Text("🏫 Klassen-Challenge", style = MaterialTheme.typography.titleLarge, color = Q.Text)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Wähle bis zu 10 Level. Die Schüler spielen sie der Reihe nach und zeigen am Ende ihr Ergebnis mit Prüfcode.",
                color = Q.Muted, style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(title, { title = it.take(40) }, Modifier.fillMaxWidth(), label = { Text("Titel, z. B. „Hausaufgabe 8b“") }, singleLine = true)
            Levels.chapters.forEach { c ->
                Text("${c.emoji} ${c.title} · ${c.grades}", color = Q.Text, fontWeight = FontWeight.Bold)
                FlowRowCompat(c.levels.map { l -> "r:${l.id}" to "${l.number}" }, picked, ::toggle)
            }
            if (myLevels.isNotEmpty()) {
                Text("🛠️ Meine Level", color = Q.Text, fontWeight = FontWeight.Bold)
                FlowRowCompat(myLevels.mapNotNull { c -> Codec.decodeLevel(c)?.let { "c:$c" to it.title } }, picked, ::toggle)
            }
        }
        Column(Modifier.padding(16.dp)) {
            Text("${picked.size} Level gewählt", color = Q.Muted)
            NeonButton(onClick = {
                val refs = picked.filter { it.startsWith("r:") }.map { it.removePrefix("r:") }
                val custom = picked.filter { it.startsWith("c:") }.mapNotNull { Codec.decodeLevel(it.removePrefix("c:")) }
                var ri = 0; var ci = 0
                val order = picked.map { if (it.startsWith("r:")) "r${ri++}" else "c${ci++}" }
                val code = Codec.encodeChallenge(ChallengeSpec(title.ifBlank { "Kurvenflug-Challenge" }, refs, custom, order))
                shareText(ctx, "🏫 ${title.ifBlank { "Kurvenflug-Challenge" }} – ${picked.size} Level. In Kurvenflug: Baukasten → Code eingeben:\n$code", "Challenge teilen")
            }, color = Q.Yellow, enabled = picked.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("📨 Challenge-Code teilen", fontWeight = FontWeight.Bold) }
        }
    }
}

/** Einfache umbrechende Chip-Reihe (4 je Zeile). */
@Composable
private fun FlowRowCompat(items: List<Pair<String, String>>, picked: List<String>, onToggle: (String) -> Unit) {
    items.chunked(4).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { (k, label) ->
                val i = picked.indexOf(k)
                FilterChip(selected = i >= 0, onClick = { onToggle(k) }, label = { Text(if (i >= 0) "${i + 1}. $label" else label, maxLines = 1) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Q.Yellow.copy(alpha = 0.3f), labelColor = Q.Text, selectedLabelColor = Q.Text))
            }
            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/**
 * Challenge spielen: Level nacheinander, Versuche je Level werden notiert.
 * Am Ende: Ergebnis-Karte mit Datum und Prüfcode zum Vorzeigen.
 */
@Composable
fun ChallengeRun(spec: ChallengeSpec, probeColor: Color, onBack: () -> Unit) {
    val levels = remember(spec) { spec.levels() }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var attempts by rememberSaveable { mutableStateOf(listOf<Int>()) }
    var name by rememberSaveable { mutableStateOf("") }
    var started by rememberSaveable { mutableStateOf(false) }
    if (levels.isEmpty()) { LaunchedEffect(Unit) { onBack() }; return }
    val done = attempts.size >= levels.size
    Box(Modifier.fillMaxSize()) {
        if (!done) key(index) {
            val l = levels[index]
            LevelScreen(l, Chapter(0, spec.title, "", "🏫", emptyList(), listOf(l)), probeColor, false, {},
                onSolved = { _, _, a -> attempts = attempts + a },
                onNext = { if (index < levels.lastIndex) index++ }, onBack = onBack,
                nextLabel = if (index < levels.lastIndex) "Nächstes Level ›" else "Ergebnis ›", allowRetry = false,
                header = "🏫 ${spec.title} · Level ${index + 1} von ${levels.size}")
        }
        if (!started) Box(Modifier.fillMaxSize().background(Color(0xEE070B20)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("🏫", fontSize = 64.sp)
                Text(spec.title, style = MaterialTheme.typography.headlineMedium, color = Q.Yellow, textAlign = TextAlign.Center)
                Text("${levels.size} Level hintereinander. Am Ende bekommst du eine Ergebnis-Karte zum Vorzeigen.", color = Q.Text, textAlign = TextAlign.Center)
                OutlinedTextField(name, { name = it.take(30) }, label = { Text("Dein Name (für die Karte)") }, singleLine = true)
                NeonButton(onClick = { started = true }, color = Q.Yellow, enabled = name.isNotBlank()) { Text("Los geht's 🚀", fontWeight = FontWeight.Black) }
            }
        }
        if (done) ResultSheet(spec, levels, attempts, name, onBack)
    }
}

@Composable
private fun ResultSheet(spec: ChallengeSpec, levels: List<Level>, attempts: List<Int>, name: String, onBack: () -> Unit) {
    val stars = attempts.map { Flight.stars(it, false) }
    val date = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY).format(Date()) }
    // Prüfcode: aus Ergebnis + Challenge – schnell nachrechenbar, schwer zu fälschen ohne App
    val check = remember { Integer.toHexString((spec.title + name + attempts.joinToString() + date).hashCode() and 0xFFFF).uppercase().padStart(4, '0') }
    Box(Modifier.fillMaxSize().background(Q.Space), contentAlignment = Alignment.Center) {
        Starfield(seed = 5)
        Column(Modifier.padding(20.dp).clip(RoundedCornerShape(24.dp)).background(Q.Panel).border(2.dp, Q.Yellow, RoundedCornerShape(24.dp)).padding(20.dp)
            .verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🏆 ${spec.title}", style = MaterialTheme.typography.titleLarge, color = Q.Yellow, textAlign = TextAlign.Center)
            Text(name, style = MaterialTheme.typography.headlineMedium, color = Q.Text)
            Text("⭐ ${stars.sum()} von ${levels.size * 3}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Q.Yellow)
            levels.forEachIndexed { i, l ->
                Row(Modifier.fillMaxWidth()) {
                    Text("${i + 1}. ${l.title}", color = Q.Text, modifier = Modifier.weight(1f), maxLines = 1)
                    Text("${attempts[i]}× · " + "★".repeat(stars[i]), color = Q.Muted)
                }
            }
            Text("$date · Prüfcode $check", fontFamily = FontFamily.Monospace, color = Q.Muted, style = MaterialTheme.typography.labelMedium)
            Text("Zeig diesen Bildschirm deiner Lehrkraft (oder mach ein Bildschirmfoto).", color = Q.Muted, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
            NeonButton(onClick = onBack, color = Q.Green) { Text("Fertig") }
        }
    }
}

// ---------------------------------------------------------------- Formel-Detektiv

/**
 * Formel-Detektiv: Die Kurve ist zu sehen, du tippst die Formel. 8 Fälle, die Bahnart wechselt.
 * Umkehrung des Spiels – vom Bild zur Gleichung.
 */
@Composable
fun DetectiveScreen(best: Int, probeColor: Color, onFinished: (solved: Int) -> Unit, onBack: () -> Unit,
                    seed: Long = remember { System.currentTimeMillis() % 100_000 }) {
    val rounds = 8
    val sfx = LocalSfx.current
    var round by rememberSaveable { mutableIntStateOf(0) }
    var solved by rememberSaveable { mutableIntStateOf(0) }
    var tries by rememberSaveable { mutableIntStateOf(0) }
    var inputs by rememberSaveable(round) { mutableStateOf(mapOf<String, String>()) }
    var verdict by rememberSaveable(round) { mutableStateOf<Boolean?>(null) }
    var guessPath by remember(round) { mutableStateOf(listOf<Vec>()) }
    BackHandler { onBack() }
    if (round >= rounds) {
        LaunchedEffect(Unit) { onFinished(solved) }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Starfield(seed = 13)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(24.dp)) {
                Text("🔎", fontSize = 64.sp)
                Text("$solved von $rounds Fällen gelöst!", style = MaterialTheme.typography.headlineMedium, color = Q.Yellow, textAlign = TextAlign.Center)
                if (solved > best) Text("Neuer Rekord! 🏆", color = Q.Green, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NeonButton(onClick = onBack, color = Q.Violet) { Text("Zurück") }
                    NeonButton(onClick = { round = 0; solved = 0 }, color = Q.Yellow) { Text("Neue Fälle ↻") }
                }
            }
        }
        return
    }
    val level = remember(seed, round) { Detective.case(seed, round) }
    val target = remember(level) { Flight.path(level, level.solution) }
    val marks = remember(level) { level.targets }
    Box(Modifier.fillMaxSize()) {
        Starfield(seed = 14)
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Q.Text) }
                Text("🔎 Formel-Detektiv · Fall ${round + 1}/$rounds", style = MaterialTheme.typography.titleLarge, color = Q.Text, modifier = Modifier.weight(1f))
                Text("✔ $solved", color = Q.Green, modifier = Modifier.padding(end = 12.dp))
            }
            Volt(when (verdict) { true -> VoltMood.WOW; false -> VoltMood.OOPS; null -> VoltMood.THINK }, Modifier.padding(horizontal = 12.dp), size = 52.dp,
                speech = when (verdict) {
                    true -> "Fall gelöst! 🕵️ Genau diese Kurve."
                    false -> "Noch nicht – deine Kurve ist gestrichelt eingezeichnet. Vergleich mit den Punkten!"
                    null -> "Welche Formel steckt hinter dieser Bahn? Die Punkte haben genaue Koordinaten."
                })
            Box(Modifier.padding(12.dp).fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(18.dp)).background(Color(0x66070B20))
                .border(1.5.dp, Q.Yellow.copy(alpha = 0.4f), RoundedCornerShape(18.dp))) {
                SpaceCanvas(SpaceView(level = level.copy(asteroids = emptyList(), cells = emptyList(), hidden = false), preview = guessPath, crosshair = null,
                    ghosts = emptyList(), trail = target, probe = null, hitTargets = if (verdict == true) level.targets.indices.toSet() else emptySet(),
                    takenCells = emptySet(), revealHidden = true, bursts = emptyList(), probeColor = Q.Yellow, time = 0f), Modifier.fillMaxSize())
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(when (level.kind) {
                    Kind.LINE -> "y = m · x + b"; Kind.PARABOLA -> "y = a · (x − d)² + e"; Kind.EXP -> "y = a · bˣ"; else -> "y = a · sin(b · x) + d"
                }, fontFamily = FontFamily.Monospace, fontSize = 20.sp, color = Q.Text)
                Text("Punkte: " + marks.joinToString("   ") { "(${num(it.x)} | ${num(it.y)})" }, color = Q.Muted, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    level.params.forEach { p ->
                        OutlinedTextField(inputs[p.key].orEmpty(), { inputs = inputs + (p.key to it.take(8)) }, Modifier.weight(1f),
                            label = { Text(p.label) }, singleLine = true, enabled = verdict != true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (verdict != true) NeonButton(onClick = {
                        val guess = level.params.associate { p -> p.key to (Detective.parse(inputs[p.key].orEmpty()) ?: Double.NaN) }
                        if (guess.values.any { it.isNaN() }) { sfx?.play(Sound.WRONG); return@NeonButton }
                        tries++
                        val ok = Detective.matches(level, guess)
                        verdict = ok
                        guessPath = Flight.path(level, guess)
                        if (ok) { solved++; sfx?.play(Sound.WIN) } else sfx?.play(Sound.WRONG)
                    }, color = Q.Yellow) { Text("🔎 Prüfen", fontWeight = FontWeight.Bold) }
                    NeonButton(onClick = { round++; tries = 0 }, color = if (verdict == true) Q.Green else Q.Violet) {
                        Text(if (verdict == true) "Nächster Fall ›" else "Überspringen")
                    }
                }
                Text("Tipp: Kommazahlen mit Komma, auch Brüche wie 1/2 gehen.", color = Q.Muted, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
