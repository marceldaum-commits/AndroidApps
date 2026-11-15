package de.kurvenflug.app.ui.together

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kurvenflug.app.ui.fx.*
import de.kurvenflug.app.ui.theme.Q
import de.kurvenflug.app.ui.volt.Volt
import de.kurvenflug.app.ui.volt.VoltMood
import de.kurvenflug.core.data.Profile
import de.kurvenflug.core.data.Settings
import de.kurvenflug.core.model.ChallengeResult
import de.kurvenflug.core.model.Codec
import de.kurvenflug.core.model.WeekSummary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
private fun Header(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Q.Text) }
        Text(title, style = MaterialTheme.typography.titleLarge, color = Q.Text)
    }
}

/**
 * Spieler auf diesem Gerät: wechseln, neu anlegen (Spitzname + Bild), umbenennen, löschen.
 * Jeder hat seinen eigenen Spielstand. Löschen nur mit Gedrückthalten.
 */
@Composable
fun ProfileScreen(
    profiles: List<Profile>, current: Int, onSwitch: (Int) -> Unit, onAdd: (String, String) -> Unit,
    onRename: (Int, String, String) -> Unit, onDelete: (Int) -> Unit, onBack: () -> Unit,
) {
    val sfx = LocalSfx.current
    var editing by rememberSaveable { mutableStateOf<Int?>(null) }   // -1 = neu
    var name by rememberSaveable { mutableStateOf("") }
    var avatar by rememberSaveable { mutableStateOf(Settings.AVATARS[0]) }
    Box(Modifier.fillMaxSize()) {
        Starfield(seed = 31)
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Header("👥 Spieler", onBack)
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Volt(VoltMood.HAPPY, size = 52.dp, speech = "Teilt ihr euch das Handy? Jeder bekommt seinen eigenen Spielstand – tipp einfach auf deinen Namen!")
                profiles.forEach { p ->
                    val active = p.id == current
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        .background(if (active) Q.Cyan.copy(alpha = 0.18f) else Q.Panel)
                        .border(2.dp, if (active) Q.Cyan else Q.Muted.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
                        .clickable { sfx?.play(Sound.TAP); onSwitch(p.id) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(p.avatar, fontSize = 34.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, fontWeight = FontWeight.Black, color = Q.Text, fontSize = 18.sp)
                            Text(if (active) "spielt gerade ✅" else "antippen zum Wechseln", color = Q.Muted, style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { editing = p.id; name = p.name; avatar = p.avatar }) { Text("✏️", fontSize = 18.sp) }
                    }
                }
                if (profiles.size < Settings.MAX_PROFILES) NeonButton(onClick = { editing = -1; name = ""; avatar = Settings.AVATARS[profiles.size % Settings.AVATARS.size] },
                    color = Q.Green, modifier = Modifier.fillMaxWidth()) { Text("➕ Neuer Spieler", fontWeight = FontWeight.Bold) }
                editing?.let { id ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Q.Panel).border(2.dp, Q.Violet, RoundedCornerShape(18.dp)).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (id == -1) "Neuer Spieler" else "Spieler ändern", fontWeight = FontWeight.Bold, color = Q.Text)
                        OutlinedTextField(name, { name = it.take(16) }, label = { Text("Spitzname") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Settings.AVATARS.forEach { a ->
                                Text(a, fontSize = 26.sp, modifier = Modifier.clip(CircleShape)
                                    .background(if (a == avatar) Q.Cyan.copy(alpha = 0.3f) else Color.Transparent)
                                    .clickable { sfx?.play(Sound.TICK); avatar = a }.padding(4.dp))
                            }
                        }
                        Text("Tipp: nur ein Spitzname – der echte Name ist nicht nötig.", color = Q.Muted, style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NeonButton(onClick = {
                                if (id == -1) onAdd(name, avatar) else onRename(id, name, avatar)
                                sfx?.play(Sound.HIT); editing = null
                            }, color = Q.Green) { Text("Speichern") }
                            TextButton(onClick = { editing = null }) { Text("Abbrechen", color = Q.Muted) }
                        }
                        if (id != -1 && profiles.size > 1) HoldToConfirm("Spieler löschen (2 s halten)") { onDelete(id); editing = null }
                    }
                }
            }
        }
    }
}

/** Gefährliche Knöpfe: 2 Sekunden gedrückt halten, ein Balken läuft voll. */
@Composable
fun HoldToConfirm(label: String, onConfirm: () -> Unit) {
    val fill = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp)).background(Q.Orange.copy(alpha = 0.15f))
        .border(2.dp, Q.Orange, RoundedCornerShape(14.dp))
        .pointerInput(Unit) {
            detectTapGestures(onPress = {
                var done = false
                val job = scope.launch { fill.animateTo(1f, tween(2000, easing = LinearEasing)); done = true; onConfirm() }
                tryAwaitRelease()
                if (!done) { job.cancel(); scope.launch { fill.animateTo(0f) } }
            })
        }, contentAlignment = Alignment.Center) {
        Box(Modifier.align(Alignment.CenterStart).fillMaxHeight().fillMaxWidth(fill.value).background(Q.Orange.copy(alpha = 0.5f)))
        Text(label, fontWeight = FontWeight.Bold, color = Q.Text)
    }
}

/**
 * Eltern-Ansicht: Wochenübersicht ohne Wertung – Tage mit Übung, Minuten, Themen, wo Hilfe gut täte, was gut läuft.
 * Alles bleibt auf dem Gerät.
 */
@Composable
fun ParentScreen(player: Profile, w: WeekSummary, dayLabels: List<String>, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Starfield(seed = 61)
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Header("👪 Für Eltern · ${player.avatar} ${player.name}", onBack)
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Die letzten 7 Tage", fontWeight = FontWeight.Black, color = Q.Text, fontSize = 18.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Tile("📅", "${w.activeDays} von 7", "Tage geübt", Q.Cyan, Modifier.weight(1f))
                    Tile("⏱️", "${w.minutes} min", "Übungszeit", Q.Violet, Modifier.weight(1f))
                    Tile("✅", "${w.solved}", "Level geschafft", Q.Green, Modifier.weight(1f))
                }
                // Balken je Tag – wachsen nacheinander ein
                val max = (w.days.maxOfOrNull { it.first } ?: 0).coerceAtLeast(1)
                Row(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(18.dp)).background(Q.Panel).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    w.days.forEachIndexed { i, (solved, min) ->
                        val grow = remember { Animatable(0f) }
                        LaunchedEffect(solved) { delay(70L * i); grow.animateTo(solved.toFloat() / max, tween(600, easing = FastOutSlowInEasing)) }
                        Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                            if (solved > 0) Text("$solved", color = Q.Green, style = MaterialTheme.typography.labelSmall)
                            Box(Modifier.fillMaxWidth().fillMaxHeight(0.7f * grow.value + 0.02f).clip(RoundedCornerShape(6.dp))
                                .background(Brush.verticalGradient(listOf(Q.Green, Q.Cyan.copy(alpha = 0.6f)))))
                            Text(dayLabels.getOrElse(i) { "" }, color = if (min > 0 || solved > 0) Q.Text else Q.Muted, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Section("📚 Geübte Themen", if (w.topics.isEmpty()) listOf("Diese Woche noch nichts – vielleicht gemeinsam die Tagesmission spielen?")
                    else w.topics.map { (c, n) -> "${c.emoji} ${c.title} (${c.grades}) – $n× gespielt" }, Q.Cyan)
                Section("🤝 Hier täte Hilfe gut", if (w.needsHelp.isEmpty()) listOf("Nichts – alles, woran es gehangen hat, wurde inzwischen geschafft. 👍")
                    else w.needsHelp.map { "${it.emoji} ${it.title}: mehrere Versuche, noch nicht geschafft. Lassen Sie sich die Aufgabe erklären – " +
                        "laut erklären hilft oft schon. Nach 3 Versuchen bietet Volt einen Rechenweg an." }, Q.Orange)
                Section("🌟 Läuft richtig gut", if (w.strong.isEmpty()) listOf("Noch zu wenig gespielt für eine Aussage.")
                    else w.strong.map { "${it.emoji} ${it.title} – meist beim ersten Versuch richtig gerechnet" }, Q.Green)
                Text("Hinweis: Die Übersicht bleibt auf diesem Gerät. Kurvenflug zählt nur Level, Versuche und Dauer – keine Standorte, " +
                    "nichts verlässt das Gerät. Pausen sind völlig in Ordnung; es gibt keine Serien, die verfallen.",
                    color = Q.Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun Tile(icon: String, value: String, label: String, color: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.15f)).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 22.sp)
        Text(value, fontWeight = FontWeight.Black, color = color, fontSize = 18.sp)
        Text(label, color = Q.Muted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Section(title: String, lines: List<String>, color: Color) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Q.Panel).border(1.5.dp, color.copy(alpha = 0.4f), RoundedCornerShape(16.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontWeight = FontWeight.Bold, color = color)
        lines.forEach { Text(it, color = Q.Text, style = MaterialTheme.typography.bodySmall) }
    }
}

/**
 * Bestenliste für Lehrkräfte: Ergebnis-Codes der Schüler (z. B. aus dem Klassen-Chat) einfügen,
 * die App prüft den Prüfcode und sortiert nach Sternen, dann Versuchen. Kein Server, kein Konto.
 */
@Composable
fun ResultsBoard(onBack: () -> Unit, initial: String = "") {
    var text by rememberSaveable { mutableStateOf(initial) }
    val results = remember(text) { Codec.decodeResults(text) }
    val ranking = remember(results) { ChallengeResult.ranking(results) }
    Box(Modifier.fillMaxSize()) {
        Starfield(seed = 43)
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Header("🏆 Ergebnisse sammeln", onBack)
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Die Schüler tippen am Ende der Challenge auf „Ergebnis senden“. Füge hier alle Nachrichten ein – auch viele auf einmal.",
                    color = Q.Muted, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(text, { text = it }, label = { Text("Ergebnis-Codes einfügen") }, modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp), maxLines = 5)
                if (ranking.isNotEmpty()) Text("${ranking.first().title} · ${ranking.size} Ergebnisse", fontWeight = FontWeight.Black, color = Q.Yellow)
                ranking.forEachIndexed { i, r ->
                    val medal = if (!r.valid) "–" else when (i) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "${i + 1}." }
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (i < 3) Q.Yellow.copy(alpha = 0.12f) else Q.Panel).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(medal, fontSize = 22.sp, modifier = Modifier.width(40.dp))
                        Column(Modifier.weight(1f)) {
                            Text(r.name, fontWeight = FontWeight.Bold, color = Q.Text)
                            Text("${r.date} · ${r.tries} Versuche", color = Q.Muted, style = MaterialTheme.typography.labelSmall)
                        }
                        Text("⭐ ${r.stars}", color = Q.Yellow, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(8.dp))
                        Text(if (r.valid) "✅" else "⚠️", fontSize = 16.sp)
                    }
                }
                if (results.any { !it.valid }) Text("⚠️ = Prüfcode passt nicht: Das Ergebnis wurde vermutlich verändert.", color = Q.Orange, style = MaterialTheme.typography.bodySmall)
                if (text.isNotBlank() && results.isEmpty()) Text("Keine Ergebnis-Codes gefunden. Sie beginnen mit „KFR1:“.", color = Q.Muted)
            }
        }
    }
}
