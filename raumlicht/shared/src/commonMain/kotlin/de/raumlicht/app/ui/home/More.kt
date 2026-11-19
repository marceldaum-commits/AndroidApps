package de.raumlicht.app.ui.home

import de.raumlicht.app.ui.fx.verticalFade
import de.raumlicht.app.ui.fx.nowMillis
import de.raumlicht.app.ui.fx.nowNanos
import de.raumlicht.app.ui.fx.count
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.raumlicht.app.ui.fx.NeonButton
import de.raumlicht.app.ui.fx.LocalShareText
import de.raumlicht.app.ui.fx.LocalShareCard
import de.raumlicht.app.ui.fx.pressBounce
import de.raumlicht.app.ui.level.LevelScreen
import de.raumlicht.app.ui.level.Stepper
import de.raumlicht.app.ui.level.stageColor
import de.raumlicht.app.ui.lux.Lux
import de.raumlicht.app.ui.lux.LuxMood
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.core.model.*

@Composable
private fun Chip(text: String, on: Boolean, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Text(text, modifier.pressBounce(interaction).clip(RoundedCornerShape(12.dp)).background(if (on) color.copy(alpha = 0.35f) else Q.Deep)
        .border(if (on) 2.dp else 1.dp, if (on) color else Q.Muted.copy(alpha = 0.4f), RoundedCornerShape(12.dp)).clickable(interaction, null, onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        fontWeight = FontWeight.Bold, color = Q.Text, fontSize = 13.sp)
}

@Composable
private fun Note(text: String, mood: LuxMood = LuxMood.IDLE) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Lux(mood, size = 52.dp)
        Text(text, Modifier.weight(1f).background(Q.Panel, RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)).padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

// ───────────────────────── Lernstand ─────────────────────────

/** Pro Thema ein Balken: sicher, wackelig oder offen – mit direktem Sprung ins Kapitel. */
@Composable
fun LearnScreen(topics: List<TopicState>, onChapter: (Int) -> Unit, onBack: () -> Unit,
                /** Sachlicher Hinweis auf unsere Nachhilfe, wenn ein Gebiet hakt (null = keiner) – ohne Figur, ohne Preis; [onHint] öffnet die Probestunde, [onHideHint] blendet ihn für dieses Gebiet aus */
                hint: de.raumlicht.core.model.Nachhilfe.Hint? = null, onHint: () -> Unit = {}, onHideHint: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding()) {
        Bar("Lernstand", onBack)
        Text(Learn.summary(topics), Modifier.padding(horizontal = 18.dp, vertical = 4.dp), color = Q.Yellow, fontWeight = FontWeight.Bold)
        LazyColumn(contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 24.dp, top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (hint != null) item(key = "nachhilfe") {
                Column(Modifier.fillMaxWidth().testTag("nachhilfe-hinweis").clip(RoundedCornerShape(16.dp)).background(Q.Deep).border(1.dp, Q.Muted.copy(alpha = 0.5f), RoundedCornerShape(16.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(hint.text, color = Q.Text, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Kostenlose Probestunde anfragen", Modifier.testTag("nachhilfe-probestunde").clip(RoundedCornerShape(10.dp)).border(1.dp, Q.Cyan.copy(alpha = 0.7f), RoundedCornerShape(10.dp)).clickable(onClick = onHint).padding(horizontal = 10.dp, vertical = 7.dp),
                            color = Q.Cyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Ausblenden", Modifier.testTag("nachhilfe-aus").clip(RoundedCornerShape(10.dp)).clickable(onClick = onHideHint).padding(horizontal = 8.dp, vertical = 7.dp), color = Q.Muted, fontSize = 13.sp)
                    }
                    Text("Ein Angebot von Nachhilfe – online. Abschaltbar in den Einstellungen.", color = Q.Muted, fontSize = 11.sp)
                }
            }
            itemsIndexed(topics) { i, t ->
                // Überschrift, sobald ein neues Gebiet beginnt
                if (i == 0 || topics[i - 1].chapter.area != t.chapter.area) Text("${t.chapter.area.emoji} ${t.chapter.area.title}".uppercase(), Modifier.padding(start = 4.dp, top = if (i == 0) 0.dp else 10.dp, bottom = 6.dp), color = stageColor(t.chapter.area.color), fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                val c = when (t.grip) { Grip.SICHER -> Q.Green; Grip.WACKELIG -> Q.Orange; Grip.OFFEN -> Q.Muted }
                val interaction = remember { MutableInteractionSource() }
                val w by animateFloatAsState(t.share, spring(dampingRatio = 0.7f, stiffness = 120f), label = "balken")
                Column(Modifier.fillMaxWidth().pressBounce(interaction).clip(RoundedCornerShape(16.dp)).background(Q.Deep).border(1.dp, c.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .clickable(interaction, null) { onChapter(t.chapter.number) }.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t.chapter.emoji, fontSize = 20.sp)
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(t.chapter.title, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("${t.played} von ${t.total} Aufgaben gespielt · ★ ${t.stars}", color = Q.Muted, fontSize = 12.sp)
                        }
                        Text(t.grip.label, color = c, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                    Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Q.Panel)) {
                        Box(Modifier.fillMaxWidth(w).fillMaxHeight().background(c))
                    }
                }
            }
        }
    }
}

// ───────────────────────── Klausur-Planer ─────────────────────────

/** Termin und Kapitel eintragen – die App teilt den Stoff auf die verbleibenden Tage auf. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlannerScreen(plan: Plan?, today: Long, stars: Map<String, Int>, onSave: (Plan?) -> Unit, onSolved: (Level, Int, Int) -> Unit, onBack: () -> Unit,
                  /** Sachlicher Hinweis auf die kostenlose Probestunde, 3 bis 14 Tage vor der Klausur (null = keiner) */
                  hint: de.raumlicht.core.model.Nachhilfe.ExamHint? = null, onHint: () -> Unit = {}, onHideHint: () -> Unit = {}) {
    var days by rememberSaveable { mutableIntStateOf(plan?.let { Planner.daysLeft(it, today).coerceIn(1, 60) } ?: 7) }
    var chosen by rememberSaveable { mutableStateOf(plan?.chapters?.toList() ?: emptyList()) }
    var playing by remember { mutableStateOf<List<Level>?>(null) }
    val current = if (chosen.isEmpty()) null else Plan(today + days, chosen.toSet())
    playing?.let { list -> TrainingPlay(list, onSolved, "Klausur-Paket", "🗓️") { playing = null }; return }

    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding().verticalScroll(rememberScrollState())) {
        Bar("Klausur-Planer", onBack)
        Note("Sag mir, wann die Klausur ist und was drankommt. Ich teile den Stoff in kleine Tagespakete auf – ohne Stress am Abend davor.")
        Text("Klausur in … Tagen", Modifier.padding(start = 16.dp, top = 8.dp), color = Q.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Stepper(null, days, if (days == 1) "morgen" else "in $days Tagen", Q.Orange, "tage", Modifier.fillMaxWidth(), canDown = days > 1, canUp = days < 60) { d -> days += d; current?.let { onSave(Plan(today + days, chosen.toSet())) } }
        }
        Text("Diese Kapitel kommen dran", Modifier.padding(start = 16.dp, top = 8.dp, bottom = 6.dp), color = Q.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        FlowRow(Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Levels.chapters.forEach { c ->
                Chip("${c.emoji} ${c.title}", c.number in chosen, stageColor(c.color), Modifier.testTag("kapitel-${c.number}")) {
                    chosen = if (c.number in chosen) chosen - c.number else chosen + c.number
                    onSave(if (chosen.isEmpty()) null else Plan(today + days, chosen.toSet()))
                }
            }
        }
        if (current != null) {
            val pack = Planner.today(current, today, stars); val rest = Planner.open(current, stars).size
            Column(Modifier.fillMaxWidth().padding(14.dp).background(verticalFade(listOf(Q.Orange.copy(alpha = 0.2f), Q.Deep)), RoundedCornerShape(18.dp)).border(1.5.dp, Q.Orange.copy(alpha = 0.7f), RoundedCornerShape(18.dp)).padding(14.dp)) {
                Text(Planner.headline(current, today, stars) ?: "", fontWeight = FontWeight.Black, fontSize = 17.sp)
                Text(if (rest == 0) "In diesen Kapiteln hast du alles mit drei Sternen. Mehr geht nicht!" else "Insgesamt noch ${count(rest, "Aufgabe", "Aufgaben")} ohne drei Sterne. Heute: ${pack.joinToString(", ") { it.id }}", color = Q.Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                if (pack.isNotEmpty()) NeonButton({ playing = pack }, Modifier.fillMaxWidth().padding(top = 10.dp), color = Q.Green) { Text("▶ Heutiges Paket spielen", fontWeight = FontWeight.Black) }
            }
            NeonButton({ chosen = emptyList(); onSave(null) }, Modifier.padding(horizontal = 14.dp), color = Q.Muted) { Text("Plan löschen") }
        }
        // Hinweis auf die kostenlose Probestunde: ganz unten, getrennt von Lux und vom Tagespaket – die Figur wirbt nie
        Spacer(Modifier.height(10.dp))
        if (hint != null) Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp).testTag("klausur-hinweis").clip(RoundedCornerShape(16.dp)).background(Q.Deep).border(1.dp, Q.Muted.copy(alpha = 0.5f), RoundedCornerShape(16.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(hint.text, color = Q.Text, fontSize = 14.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Kostenlose Probestunde anfragen", Modifier.testTag("klausur-probestunde").clip(RoundedCornerShape(10.dp)).border(1.dp, Q.Cyan.copy(alpha = 0.7f), RoundedCornerShape(10.dp)).clickable(onClick = onHint).padding(horizontal = 10.dp, vertical = 7.dp),
                    color = Q.Cyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Ausblenden", Modifier.testTag("klausur-aus").clip(RoundedCornerShape(10.dp)).clickable(onClick = onHideHint).padding(horizontal = 8.dp, vertical = 7.dp), color = Q.Muted, fontSize = 13.sp)
            }
            Text("Ein Angebot von Nachhilfe – online. Abschaltbar in den Einstellungen.", color = Q.Muted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ───────────────────────── Klassen-Modus ─────────────────────────

/** Lehrkraft stellt ein Aufgabenpaket zusammen und teilt den Code; die Klasse spielt es und meldet das Ergebnis zurück. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClassScreen(onSolved: (Level, Int, Int) -> Unit, onBack: () -> Unit) {
    var picked by rememberSaveable { mutableStateOf(listOf<String>()) }
    var chapter by rememberSaveable { mutableIntStateOf(1) }
    var code by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var pack by remember { mutableStateOf<List<Level>?>(null) }
    var idx by rememberSaveable { mutableIntStateOf(0) }
    var hits by rememberSaveable { mutableIntStateOf(0) }
    val share = LocalShareText.current; val shareCard = LocalShareCard.current
    val run = pack
    if (run != null && idx < run.size) {
        val level = run[idx]
        LevelScreen(level, Levels.chapters.first { it.number == level.chapter }, false, {}, onSolved = { s, a -> if (s == 3) hits++; onSolved(level, s, a) }, onNext = { idx++ }, onBack = { pack = null },
            caption = "🏫 Klassen-Paket · ${idx + 1}/${run.size} · Aufgabe ${Levels.label(level.id)}", badge = "🎯 $hits")
        return
    }
    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding().verticalScroll(rememberScrollState())) {
        Bar("Klassen-Paket", onBack)
        if (run != null) {
            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Lux(if (hits * 2 >= run.size) LuxMood.HAPPY else LuxMood.THINK, size = 120.dp)
                Text("$hits von ${run.size}", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Q.Yellow)
                Text("beim ersten Versuch gelöst", color = Q.Muted)
                NeonButton({ share(ClassCode.resultText(run, hits)) }, Modifier.fillMaxWidth().padding(top = 16.dp), color = Q.Pink) { Text("📤 Ergebnis an die Lehrkraft schicken", fontWeight = FontWeight.Bold) }
                NeonButton({ pack = null }, Modifier.fillMaxWidth().padding(top = 8.dp), color = Q.Green) { Text("Fertig ✓", fontWeight = FontWeight.Bold) }
            }
            return@Column
        }
        Note("Für den Unterricht: ein Aufgabenpaket als Code. Kein Konto, keine Namen, keine Daten – das Ergebnis schickt jede Person selbst.")
        Text("Code von der Lehrkraft", Modifier.padding(start = 16.dp, top = 8.dp, bottom = 6.dp), color = Q.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(code, { code = it; error = false }, Modifier.weight(1f).testTag("klassen-code"), singleLine = true, isError = error, placeholder = { Text("RLK-…") },
                supportingText = { if (error) Text("Diesen Code kenne ich nicht – bitte prüfen.") })
            NeonButton({ ClassCode.find(code)?.let { pack = it; idx = 0; hits = 0 } ?: run { error = true } }, color = Q.Pink) { Text("Los") }
        }
        Text("Paket zusammenstellen (bis ${ClassCode.MAX} Aufgaben)", Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp), color = Q.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Levels.chapters.forEach { c -> Chip("${c.emoji} ${Levels.shown(c.number)}", c.number == chapter, stageColor(c.color)) { chapter = c.number } }
        }
        val ch = Levels.chapters.first { it.number == chapter }
        Text("${ch.area.title} · ${ch.title}", Modifier.padding(start = 16.dp, top = 8.dp), color = stageColor(ch.color), fontWeight = FontWeight.Bold)
        FlowRow(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ch.levels.forEach { l ->
                Chip("${Levels.label(l.id)} ${l.title}", l.id in picked, stageColor(ch.color), Modifier.testTag("wahl-${l.id}")) {
                    picked = if (l.id in picked) picked - l.id else if (picked.size < ClassCode.MAX) picked + l.id else picked
                }
            }
        }
        if (picked.isNotEmpty()) {
            val levels = picked.mapNotNull { Levels.byId(it) }; val c = ClassCode.encode(levels)
            Column(Modifier.fillMaxWidth().padding(14.dp).background(Q.Deep, RoundedCornerShape(18.dp)).border(1.5.dp, Q.Pink.copy(alpha = 0.7f), RoundedCornerShape(18.dp)).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${count(levels.size, "Aufgabe", "Aufgaben")}: ${levels.joinToString(", ") { it.id }}", color = Q.Muted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                Text(c, Modifier.padding(vertical = 8.dp).testTag("paket-code"), fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = Q.Pink)
                NeonButton({ share(ClassCode.shareText(levels)) }, Modifier.fillMaxWidth(), color = Q.Pink) { Text("📤 Code an die Klasse schicken", fontWeight = FontWeight.Bold) }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ───────────────────────── Zu zweit an einem Gerät ─────────────────────────

/** Abwechselnd rechnen und schießen: Einen Punkt gibt es nur für einen Treffer beim ersten Versuch. */
@Composable
fun MatchScreen(onBack: () -> Unit, session: Long = remember { nowMillis() % 1_000_000 }) {
    var match by remember { mutableStateOf(Match()) }
    var n by rememberSaveable { mutableIntStateOf(0) }
    var hit by remember { mutableStateOf(false) }
    var started by rememberSaveable { mutableStateOf(false) }
    val winner = match.winner
    when {
        !started -> Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            Bar("Zu zweit", onBack)
            Spacer(Modifier.height(24.dp)); Lux(LuxMood.HAPPY, size = 130.dp)
            Text("Blau gegen Pink", fontSize = 26.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp))
            Text("Ihr spielt abwechselnd an einem Gerät. Einen Punkt gibt es nur, wenn der erste Versuch sitzt – also erst rechnen, dann schießen. Wer nach einer vollen Runde mit ${Match.TARGET} oder mehr Punkten vorn liegt, gewinnt.",
                Modifier.padding(horizontal = 28.dp, vertical = 12.dp), textAlign = TextAlign.Center)
            NeonButton({ started = true }, Modifier.padding(16.dp), color = Q.Green) { Text("▶ Blau fängt an", fontWeight = FontWeight.Black) }
        }
        winner != null -> Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            Bar("Zu zweit", onBack)
            Spacer(Modifier.height(24.dp)); Lux(LuxMood.HAPPY, size = 130.dp)
            Text("${Match.names[winner]} gewinnt!", fontSize = 30.sp, fontWeight = FontWeight.Black, color = stageColor(Match.colors[winner]), modifier = Modifier.padding(top = 8.dp))
            Text("${match.scores[0]} : ${match.scores[1]}", fontSize = 40.sp, fontWeight = FontWeight.Black, color = Q.Yellow)
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeonButton({ match = Match(); n += 1 }, Modifier.weight(1f), color = Q.Pink) { Text("↻ Revanche", fontWeight = FontWeight.Bold) }
                NeonButton(onBack, Modifier.weight(1f), color = Q.Green) { Text("Fertig ✓", fontWeight = FontWeight.Bold) }
            }
        }
        else -> {
            val level = remember(session, n) { Gen.jam(JamTopic.MIX, session, n) }
            LevelScreen(level, Chapter(0, "Zu zweit", level.title, "Zu zweit", "👥", Match.colors[match.turn], emptyList(), emptyList()), false, {},
                onSolved = { s, _ -> hit = s == 3 }, onNext = { match = match.after(hit); hit = false; n++ }, onBack = onBack,
                caption = "👥 ${Match.names[match.turn]} ist dran · ${level.title}", badge = "${match.scores[0]} : ${match.scores[1]}")
        }
    }
}
