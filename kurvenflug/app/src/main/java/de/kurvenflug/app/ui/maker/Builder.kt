package de.kurvenflug.app.ui.maker

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kurvenflug.app.ui.flight.SpaceCanvas
import de.kurvenflug.app.ui.flight.SpaceView
import de.kurvenflug.app.ui.fx.*
import de.kurvenflug.app.ui.modes.ModeCard
import de.kurvenflug.app.ui.theme.Q
import de.kurvenflug.app.ui.volt.Volt
import de.kurvenflug.app.ui.volt.VoltMood
import de.kurvenflug.core.model.*
import kotlin.math.hypot
import kotlin.math.round

fun shareText(ctx: android.content.Context, text: String, title: String = "Teilen") {
    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), title))
}

/** Werkzeug im Baukasten: was ein Tipp aufs Feld setzt. */
private enum class Tool(val label: String) { STATION("🪐 Station"), ROCK("☄️ Asteroid"), CELL("⚡ Zelle"), PLANET("🌑 Planet") }

/**
 * Baukasten: eigenes Level bauen. Tippen setzt/entfernt Objekte, die Regler legen *deine* Lösung fest.
 * Teilen geht erst, wenn die Lösung trifft und die Startwerte nicht – so sind geteilte Level immer fair.
 */
@Composable
fun BuilderScreen(
    initial: LevelSpec?,
    probeColor: Color,
    onSave: (code: String) -> Unit,
    onTest: (Level) -> Unit,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val sfx = LocalSfx.current
    var kind by rememberSaveable { mutableStateOf(initial?.let { Kind.valueOf(it.kind) } ?: Kind.LINE) }
    var title by rememberSaveable { mutableStateOf(initial?.title ?: "") }
    var task by rememberSaveable { mutableStateOf(initial?.task ?: "") }
    var planet by rememberSaveable { mutableStateOf(initial?.planet ?: "EARTH") }
    var tool by rememberSaveable { mutableStateOf(Tool.STATION) }
    // Objekte als „x;y;r;Masse“-Zeilen (lassen sich speichern/wiederherstellen)
    var objects by rememberSaveable { mutableStateOf(initialObjects(initial)) }
    var solution by rememberSaveable { mutableStateOf(HashMap(initial?.solution ?: Templates.params(kind).associate { it.key to it.start })) }
    LaunchedEffect(kind) { if (solution.keys != Templates.params(kind).map { it.key }.toSet()) solution = HashMap(Templates.params(kind).associate { it.key to it.start }) }

    val spec = LevelSpec(
        kind = kind.name, title = title.ifBlank { "Mein Level" }, task = task.ifBlank { defaultTask(kind) },
        targets = objects.filter { it.startsWith("S") }.map(::coords),
        rocks = objects.filter { it.startsWith("R") || it.startsWith("P") }.map(::coords),
        cells = objects.filter { it.startsWith("C") }.map(::coords),
        masses = if (kind == Kind.ORBIT) objects.filter { it.startsWith("R") || it.startsWith("P") }.map { if (it.startsWith("P")) 30.0 else 0.0 } else emptyList(),
        planet = planet, solution = solution,
    )
    val level = remember(spec) { spec.toLevel("eigenes") }
    val problem = if (level == null) "Dieses Level geht nicht." else Codec.check(level)
    val result = remember(level) { level?.let { Flight.fly(it, it.solution) } }

    BackHandler { onBack() }
    Box(Modifier.fillMaxSize()) {
        Starfield(seed = 31)
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Q.Text) }
                Text("🛠️ Baukasten", style = MaterialTheme.typography.titleLarge, color = Q.Text)
            }
            // Bahnart
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Templates.buildable.forEach { k ->
                    FilterChip(selected = kind == k, onClick = { kind = k; sfx?.play(Sound.TICK) }, label = { Text(Templates.label(k)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Q.Cyan.copy(alpha = 0.3f), labelColor = Q.Text, selectedLabelColor = Q.Text))
                }
            }
            // Spielfeld: Tippen setzt oder entfernt
            Box(
                Modifier.padding(12.dp).fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(18.dp))
                    .background(Color(0x66070B20)).border(1.5.dp, Q.Cyan.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            ) {
                val view = SpaceView(
                    level = level ?: Levels.all.first(), preview = result?.path.orEmpty(), crosshair = null, ghosts = emptyList(),
                    trail = emptyList(), probe = null, hitTargets = result?.hitTargets.orEmpty(), takenCells = emptySet(),
                    revealHidden = true, bursts = emptyList(), probeColor = probeColor, time = 0f,
                )
                SpaceCanvas(view, Modifier.fillMaxSize().pointerInput(tool, kind) {
                    detectTapGestures { o ->
                        val x = snapHalf(o.x / size.width * 2 * Flight.R - Flight.R)
                        val y = snapHalf(Flight.R - o.y / size.height * 2 * Flight.R)
                        val hit = objects.firstOrNull { val c = coords(it); hypot(c[0] - x, c[1] - y) <= c[2] + 0.2 }
                        objects = if (hit != null) { sfx?.play(Sound.TAP); objects - hit } else {
                            val t = if (tool == Tool.PLANET && kind != Kind.ORBIT) Tool.ROCK else tool
                            val count = objects.count { it.startsWith(t.name.first().toString()) || (t == Tool.ROCK && it.startsWith("P")) }
                            val max = when (t) { Tool.STATION -> LevelSpec.MAX_TARGETS; Tool.CELL -> LevelSpec.MAX_CELLS; else -> LevelSpec.MAX_ROCKS }
                            if (count >= max) { sfx?.play(Sound.WRONG, volume = 0.4f); objects } else {
                                sfx?.play(Sound.CELL, volume = 0.5f)
                                val r = when (t) { Tool.STATION -> if (kind == Kind.RENDEZVOUS) 0.35 else 0.7; Tool.ROCK -> 1.0; Tool.CELL -> 0.45; Tool.PLANET -> 1.3 }
                                objects + "${t.name.first()};$x;$y;$r"
                            }
                        }
                    }
                })
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tool.entries.filter { it != Tool.PLANET || kind == Kind.ORBIT }.forEach { t ->
                        FilterChip(selected = tool == t, onClick = { tool = t }, label = { Text(t.label) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Q.Pink.copy(alpha = 0.3f), labelColor = Q.Text, selectedLabelColor = Q.Text))
                    }
                }
                if (kind == Kind.THROW) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Planet.entries.forEach { p -> FilterChip(selected = planet == p.name, onClick = { planet = p.name }, label = { Text("${p.emoji} ${p.label}") }) }
                }
                Text("Deine Lösung (nur du siehst die Bahn):", color = Q.Muted, style = MaterialTheme.typography.labelMedium)
                Templates.params(kind).forEach { p ->
                    val value = solution[p.key] ?: p.start
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${p.label} = ${num(value)}${p.unit}", color = Q.Text, modifier = Modifier.width(110.dp))
                        Slider(value.toFloat(), { solution = HashMap(solution).apply { put(p.key, p.snap(it.toDouble())) } },
                            valueRange = p.min.toFloat()..p.max.toFloat(), modifier = Modifier.weight(1f))
                    }
                }
                OutlinedTextField(title, { title = it.take(40) }, Modifier.fillMaxWidth(), label = { Text("Name des Levels") }, singleLine = true)
                OutlinedTextField(task, { task = it.take(200) }, Modifier.fillMaxWidth(), label = { Text("Auftrag (was Volt sagt)") })
                Volt(if (problem == null) VoltMood.HAPPY else VoltMood.THINK, size = 48.dp,
                    speech = problem ?: "Passt! Dein Level ist fair und lösbar. Teste es oder schick es los! 🚀")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeonButton(onClick = { level?.let(onTest) }, color = Q.Violet, enabled = problem == null) { Text("▶ Testflug") }
                    NeonButton(onClick = {
                        val code = Codec.encodeLevel(spec); onSave(code)
                        shareText(ctx, "🚀 Mein Kurvenflug-Level „${spec.title}“ – schaffst du es? Code in Kurvenflug → Baukasten → Code eingeben:\n$code", "Level teilen")
                    }, color = Q.Green, enabled = problem == null) { Text("📨 Speichern & teilen", fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

private fun snapHalf(v: Double) = round(v.coerceIn(-9.0, 9.0) * 2) / 2
private fun coords(s: String): List<Double> = s.split(';').drop(1).map { it.toDouble() }.take(3)
private fun initialObjects(s: LevelSpec?): List<String> {
    if (s == null) return emptyList()
    fun line(p: Char, l: List<Double>) = "$p;${l[0]};${l[1]};${l.getOrElse(2) { 0.7 }}"
    return s.targets.map { line('S', it) } + s.rocks.mapIndexed { i, r -> line(if (s.masses.getOrElse(i) { 0.0 } > 0) 'P' else 'R', r) } + s.cells.map { line('C', it) }
}
private fun defaultTask(k: Kind) = when (k) {
    Kind.POINT -> "Lies die Koordinaten ab und flieg hin!"
    Kind.THROW -> "Triff das Ziel mit dem richtigen Wurf!"
    Kind.ORBIT -> "Lass dich von den Planeten zur Station schleudern!"
    else -> "Triff alle Stationen!"
}

/** Startseite des Baukastens: neues Level, meine Level, Code eingeben. */
@Composable
fun BuilderHome(
    myLevels: List<String>,
    onNew: () -> Unit,
    onEdit: (String) -> Unit,
    onPlay: (String) -> Unit,
    onDelete: (String) -> Unit,
    onCode: (String) -> Unit,
    onChallenge: () -> Unit,
    onBack: () -> Unit,
    onResults: () -> Unit = {},
) {
    val ctx = LocalContext.current
    var code by rememberSaveable { mutableStateOf("") }
    val type = Codec.detect(code)
    Box(Modifier.fillMaxSize()) {
        Starfield(seed = 32)
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Q.Text) }
                Text("🛠️ Baukasten & Codes", style = MaterialTheme.typography.titleLarge, color = Q.Text)
            }
            Volt(VoltMood.HAPPY, size = 56.dp, speech = "Bau dein eigenes Level und schick es Freunden – wer kann es knacken?")
            ModeCard("➕", "Neues Level bauen", "Stationen, Asteroiden, Planeten setzen – Lösung festlegen – teilen", Q.Green, onNew)
            ModeCard("🏫", "Klassen-Challenge", "Für Lehrkräfte: mehrere Level als Hausaufgabe-Code", Q.Yellow, onChallenge)
            ModeCard("🏆", "Ergebnisse sammeln", "Für Lehrkräfte: Ergebnis-Codes einfügen – Bestenliste mit Prüfcode", Q.Orange, onResults)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Q.Panel).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🔑 Code eingeben", style = MaterialTheme.typography.titleMedium, color = Q.Text)
                Text("Level-, Challenge- oder Duell-Code einfügen", style = MaterialTheme.typography.bodySmall, color = Q.Muted)
                OutlinedTextField(code, { code = it.take(3000) }, Modifier.fillMaxWidth(), placeholder = { Text("KFL1:… / KFC1:… / KF-…") }, maxLines = 3)
                NeonButton(onClick = { onCode(code) }, color = Q.Yellow, enabled = type != null, modifier = Modifier.fillMaxWidth()) {
                    Text(when (type) { Codec.Type.LEVEL -> "Level spielen"; Codec.Type.CHALLENGE -> "Challenge starten"; Codec.Type.DUEL -> "Duell annehmen"; null -> "Code einfügen" }, fontWeight = FontWeight.Bold)
                }
            }
            if (myLevels.isNotEmpty()) Text("Meine Level", style = MaterialTheme.typography.titleMedium, color = Q.Text)
            myLevels.forEach { c ->
                val spec = Codec.decodeLevel(c) ?: return@forEach
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Q.Panel).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable { onPlay(c) }) {
                        Text(spec.title, fontWeight = FontWeight.Bold, color = Q.Text)
                        Text(Templates.label(Kind.valueOf(spec.kind)), style = MaterialTheme.typography.labelSmall, color = Q.Muted)
                    }
                    TextButton(onClick = { onPlay(c) }) { Text("▶") }
                    TextButton(onClick = { onEdit(c) }) { Text("✏️") }
                    TextButton(onClick = { shareText(ctx, "🚀 Kurvenflug-Level „${spec.title}“:\n$c", "Level teilen") }) { Text("📨") }
                    TextButton(onClick = { onDelete(c) }) { Text("🗑️") }
                }
            }
        }
    }
}
