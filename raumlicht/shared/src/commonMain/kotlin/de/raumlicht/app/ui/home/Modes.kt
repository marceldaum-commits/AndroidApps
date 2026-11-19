package de.raumlicht.app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.raumlicht.app.ui.fx.*
import de.raumlicht.app.ui.level.FormulaView
import de.raumlicht.app.ui.level.LevelScreen
import de.raumlicht.app.ui.level.Stepper
import de.raumlicht.app.ui.level.stageColor
import de.raumlicht.app.ui.lux.Lux
import de.raumlicht.app.ui.lux.LuxMood
import de.raumlicht.app.ui.stage.LocalLuxSkin
import de.raumlicht.app.ui.stage.Stage
import de.raumlicht.app.ui.stage.StageController
import de.raumlicht.app.ui.stage.StageFrame
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.core.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun Bar(title: String, onBack: () -> Unit, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(end = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { BackArrow() }
        de.raumlicht.app.ui.fx.FitText(title, Modifier.weight(1f).padding(end = 8.dp), fontSize = MaterialTheme.typography.titleLarge.fontSize, min = 14f, style = MaterialTheme.typography.titleLarge)
        trailing()
    }
}

@Composable
private fun Speech(text: String, mood: LuxMood = LuxMood.IDLE) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Lux(mood, size = 52.dp)
        Text(text, Modifier.weight(1f).background(Q.Panel, RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)).padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium)
    }
}


// ───────────────────────── Backstage: alle weiteren Bereiche ─────────────────────────

/** [group]: Überschrift, unter der der Eintrag auf der Seite „Mehr“ steht */
data class BackstageEntry(val emoji: String, val title: String, val sub: String, val color: Color, val group: String = "", val onClick: () -> Unit)

/** [title]/[intro]/[footer]: dieselbe Liste dient auch als Seite „Analysis & Zufall erleben“ */
@Composable
fun BackstageScreen(entries: List<BackstageEntry>, title: String = "Mehr", intro: String? = null, footer: (@Composable () -> Unit)? = null, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding()) {
        Bar(title, onBack)
        if (intro != null) Speech(intro, LuxMood.HAPPY)
        LazyColumn(contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(entries) { i, e ->
                // Überschrift, sobald eine neue Gruppe beginnt
                if (e.group.isNotEmpty() && (i == 0 || entries[i - 1].group != e.group)) Text(e.group.uppercase(), Modifier.padding(start = 4.dp, top = if (i == 0) 0.dp else 8.dp, bottom = 6.dp), color = Q.Muted, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                val interaction = remember { MutableInteractionSource() }
                var shown by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { delay(45L * i); shown = true }
                val s by animateFloatAsState(if (shown) 1f else 0.7f, spring(dampingRatio = 0.5f, stiffness = 300f), label = "eintrag")
                Row(Modifier.fillMaxWidth().graphicsLayer { scaleX = s; scaleY = s; alpha = if (shown) 1f else 0f }.pressBounce(interaction).clip(RoundedCornerShape(18.dp))
                    .background(horizontalFade(listOf(e.color.copy(alpha = 0.2f), Q.Deep))).border(1.dp, e.color.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                    .clickable(interaction, null, onClick = e.onClick).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(e.emoji, fontSize = 26.sp)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(e.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(e.sub, color = Q.Muted, fontSize = 12.sp)
                    }
                    if (e.group == "Kommt bald") Text("bald", color = Q.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold) else Text("›", color = e.color, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
            }
            if (footer != null) item { footer() }
        }
    }
}

// ───────────────────────── Abi-Simulation ─────────────────────────

@Composable
fun ExamPicker(exams: List<Exam>, best: Map<String, Int>, onExam: (Exam, Boolean) -> Unit, guided: Boolean = false, onBack: () -> Unit) {
    var timed by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding()) {
        Bar(if (guided) "Rechenweg in Schritten" else "Abi-Prüfungsaufgaben", onBack)
        Speech(if (guided) "Schwere Verfahren, in kleine Schritte zerlegt. Nach jedem Schritt sage ich dir, ob du auf Kurs bist." else "Eine Aufgabe, mehrere Teile – wie im Abitur. Volle Punkte gibt es nur beim ersten Versuch.")
        if (!guided) Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("⏱ Auf Zeit spielen", fontWeight = FontWeight.Bold)
                Text("Freiwillig: ${Exam.clock(Exam.seconds(exams.first()))} Minuten je Aufgabe. Läuft die Zeit ab, zählt, was bis dahin gelöst ist.", color = Q.Muted, fontSize = 12.sp)
            }
            Switch(timed, { timed = it }, Modifier.testTag("zeit"))
        }
        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(exams) { _, e ->
                val interaction = remember { MutableInteractionSource() }
                Column(Modifier.fillMaxWidth().pressBounce(interaction).clip(RoundedCornerShape(20.dp)).background(horizontalFade(listOf(Q.Orange.copy(alpha = 0.22f), Q.Deep)))
                    .border(1.5.dp, Q.Orange.copy(alpha = 0.7f), RoundedCornerShape(20.dp)).clickable(interaction, null) { onExam(e, timed) }.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(e.emoji, fontSize = 28.sp)
                        Text(e.title, Modifier.weight(1f).padding(start = 10.dp), style = MaterialTheme.typography.titleMedium)
                        Text(if (guided) count(e.parts.size, "Schritt", "Schritte") else best[e.id]?.let { "$it / ${e.maxPoints} BE" } ?: "${e.maxPoints} BE", color = if (best[e.id] != null) Q.Yellow else Q.Muted, fontWeight = FontWeight.Bold)
                    }
                    Text(e.intro, Modifier.padding(top = 6.dp), color = Q.Muted, style = MaterialTheme.typography.bodySmall)
                    Text(e.parts.joinToString("   ") { "${it.label}) ${it.level.title}" }, Modifier.padding(top = 6.dp), color = Q.Orange, fontSize = 12.sp)
                }
            }
        }
    }
}

/** Spielt die Teilaufgaben nacheinander und wertet am Ende in Bewertungseinheiten aus. */
@Composable
fun ExamPlay(exam: Exam, onFinished: (points: Int) -> Unit, onBack: () -> Unit, timed: Boolean = false, guided: Boolean = false) {
    var idx by rememberSaveable(exam.id) { mutableIntStateOf(0) }
    var got by rememberSaveable(exam.id) { mutableStateOf(listOf<Int>()) }
    var left by rememberSaveable(exam.id) { mutableIntStateOf(Exam.seconds(exam)) }
    var timeUp by rememberSaveable(exam.id) { mutableStateOf(false) }
    // Zeit-Modus: Die Uhr läuft nur, solange noch Teilaufgaben offen sind
    LaunchedEffect(timed, idx >= exam.parts.size) {
        while (timed && idx < exam.parts.size && left > 0) { delay(1000); left-- }
        if (timed && left <= 0 && idx < exam.parts.size) { timeUp = true; onFinished(got.sum()); idx = exam.parts.size }
    }
    if (idx < exam.parts.size) {
        val part = exam.parts[idx]
        LevelScreen(part.level, Chapter(0, exam.title, exam.title, "Abi-Simulation", "🎓", "orange", emptyList(), emptyList()), false, {},
            onSolved = { s, _ -> got = got + Exam.points(part, s); if (idx == exam.parts.lastIndex) onFinished(got.sum()) },
            onNext = { idx++ }, onBack = onBack,
            caption = if (guided) "🧩 ${exam.title} · Schritt ${part.label} von ${exam.parts.size}" else "🎓 ${exam.title} · Teil ${part.label}) · ${part.points} BE",
            badge = if (guided) null else (if (timed) "⏱ ${Exam.clock(left)}  " else "") + "${got.sum()} BE",
            context = exam.intro.takeIf { it.isNotBlank() })
    } else {
        val total = got.sum(); val share = LocalShareText.current; val shareCard = LocalShareCard.current
        Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Bar(if (guided) exam.title else "Auswertung", onBack)
            Lux(if (total * 100 >= exam.maxPoints * 60) LuxMood.HAPPY else LuxMood.THINK, size = 120.dp)
            Text(if (guided) "Geschafft!" else "$total von ${exam.maxPoints} BE", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Q.Yellow)
            if (timed) Text(if (timeUp) "⏱ Die Zeit ist abgelaufen" else "⏱ geschafft mit ${Exam.clock(left)} Restzeit", color = if (timeUp) Q.Orange else Q.Green, fontWeight = FontWeight.Bold)
            Text(if (guided) "Du hast das Verfahren Schritt für Schritt durchgerechnet. Jetzt klappt es auch am Stück." else Exam.verdict(total, exam.maxPoints), Modifier.padding(horizontal = 24.dp, vertical = 8.dp), textAlign = TextAlign.Center)
            exam.parts.forEachIndexed { i, p ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp).background(Q.Deep, RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${p.label})  ${p.level.title}", Modifier.weight(1f))
                    Text(if (guided) "✓" else "${got.getOrElse(i) { 0 }} / ${p.points} BE", color = if (got.getOrElse(i) { 0 } == p.points) Q.Green else Q.Orange, fontWeight = FontWeight.Bold)
                }
            }
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeonButton({ idx = 0; got = emptyList(); left = Exam.seconds(exam); timeUp = false }, Modifier.weight(1f), color = Q.Muted) { Text("↻ Noch einmal") }
                if (!guided) NeonButton({ shareCard(exam.emoji + " " + exam.title, "$total von ${exam.maxPoints} BE", Exam.verdict(total, exam.maxPoints)) }, Modifier.testTag("karte"), color = Q.Pink) { Text("📤") }
                NeonButton(onBack, Modifier.weight(1f), color = Q.Green) { Text("Fertig ✓", fontWeight = FontWeight.Black) }
            }
        }
    }
}

// ───────────────────────── Lage-Check ─────────────────────────

/** Acht schnelle Runden: Wie liegen g und h zueinander? Erst entscheiden – dann zeigt die Bühne die Geraden. */
@Composable
fun LageScreen(best: Int, onScore: (Int) -> Boolean, onBack: () -> Unit, session: Long = remember { nowMillis() % 100_000 }, controller: StageController = remember { StageController() }) {
    var n by rememberSaveable { mutableIntStateOf(0) }
    var score by rememberSaveable { mutableIntStateOf(0) }
    var answer by remember(n) { mutableStateOf<Relation?>(null) }
    var record by remember { mutableStateOf(false) }
    val sfx = LocalSfx.current; val view = LocalBuzzer.current; val vib = LocalVibration.current
    val done = n >= LageCheck.ROUNDS
    val round = remember(session, n) { LageCheck.round(session * 100 + n.coerceAtMost(LageCheck.ROUNDS - 1)) }
    LaunchedEffect(round, answer) { controller.setState(round.scene(answer != null)) }

    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding()) {
        Bar("Lage von Geraden", onBack) { Text(if (done) "" else "Runde ${n + 1}/${LageCheck.ROUNDS}   ✓ $score", color = Q.Yellow, fontWeight = FontWeight.Bold) }
        if (done) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Lux(if (score >= 6) LuxMood.HAPPY else LuxMood.THINK, size = 130.dp)
                Text("$score von ${LageCheck.ROUNDS}", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Q.Yellow)
                Text(if (record) "Neuer Rekord! 🎉" else "Rekord: ${maxOf(best, score)}", color = Q.Muted, modifier = Modifier.padding(8.dp))
                Text("Merke: Erst die Richtungsvektoren vergleichen. Vielfache → parallel oder identisch (Punktprobe). Sonst gleichsetzen.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NeonButton({ n = 0; score = 0; record = false }, Modifier.weight(1f), color = Q.Pink) { Text("↻ Neue Runde", fontWeight = FontWeight.Bold) }
                    NeonButton(onBack, Modifier.weight(1f), color = Q.Green) { Text("Fertig ✓", fontWeight = FontWeight.Black) }
                }
            }
            return@Column
        }
        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp)) {
            Stage(controller, Modifier.fillMaxSize())
            StageFrame(Q.Yellow)
            if (answer == null) Text("Erst entscheiden – dann siehst du die Geraden.", Modifier.align(Alignment.Center).background(Q.Space.copy(alpha = 0.7f), RoundedCornerShape(12.dp)).padding(12.dp), color = Q.Muted)
        }
        FormulaView(round.formula, { Q.Text }, Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).background(Q.Deep, RoundedCornerShape(16.dp)).padding(12.dp))
        val a = answer
        if (a != null) Speech((if (a == round.relation) "Richtig! " else "Nicht ganz – ${if (round.relation == Relation.INTERSECT) "sie schneiden sich" else "sie sind " + LageCheck.names[round.relation]}. ") + round.explanation, if (a == round.relation) LuxMood.HAPPY else LuxMood.SAD)
        listOf(listOf(Relation.PARALLEL, Relation.IDENTICAL), listOf(Relation.INTERSECT, Relation.SKEW)).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { r ->
                    val c = when { a == null -> Q.Cyan; r == round.relation -> Q.Green; r == a -> Q.Orange; else -> Q.Muted }
                    NeonButton({
                        if (answer == null) {
                            answer = r
                            if (r == round.relation) { score++; sfx?.play(Sound.HIT); view.buzz(Buzz.HIT, vib); controller.party(1.5) } else { sfx?.play(Sound.WRONG); view.buzz(Buzz.CRASH, vib) }
                        }
                    }, Modifier.weight(1f).testTag("lage-${r.name}"), color = c, enabled = a == null || r == round.relation || r == a) { Text(LageCheck.names.getValue(r), fontWeight = FontWeight.Bold) }
                }
            }
        }
        NeonButton({ if (n == LageCheck.ROUNDS - 1) record = onScore(score); n++ }, Modifier.fillMaxWidth().padding(12.dp), color = Q.Green, enabled = a != null) {
            Text(if (n == LageCheck.ROUNDS - 1) "Auswertung ▸" else "Weiter ▸", fontWeight = FontWeight.Black)
        }
    }
}

// ───────────────────────── Duell ─────────────────────────

/** Duell per Code: beide spielen dieselben fünf Aufgaben und vergleichen, wie viele beim ersten Versuch saßen. */
@Composable
fun DuelScreen(onBack: () -> Unit, incoming: String? = null) {
    var duel by remember { mutableStateOf(incoming?.let { DuelCode.find(it) }) }
    var playing by rememberSaveable { mutableStateOf(false) }
    var n by rememberSaveable { mutableIntStateOf(0) }
    var hits by rememberSaveable { mutableIntStateOf(0) }
    var code by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val share = LocalShareText.current; val shareCard = LocalShareCard.current
    val d = duel
    when {
        d != null && playing && n < DuelCode.TASKS -> {
            val level = remember(d, n) { d.level(n) }
            LevelScreen(level, Chapter(0, "Duell", d.topic.title, "Duell", "⚔️", "pink", emptyList(), emptyList()), false, {},
                onSolved = { s, _ -> if (s == 3) hits++ }, onNext = { n++ }, onBack = onBack,
                caption = "⚔️ Duell ${d.code} · ${n + 1}/${DuelCode.TASKS}", badge = "🎯 $hits")
        }
        d != null && playing -> Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding().padding(bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Bar("Duell-Ergebnis", onBack)
            Spacer(Modifier.height(24.dp))
            Lux(if (hits >= 3) LuxMood.HAPPY else LuxMood.THINK, size = 130.dp)
            Text("$hits von ${DuelCode.TASKS}", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Q.Yellow)
            Text("beim ersten Versuch getroffen", color = Q.Muted)
            Text(d.code, Modifier.padding(16.dp).background(Q.Deep, RoundedCornerShape(12.dp)).border(1.dp, Q.Pink, RoundedCornerShape(12.dp)).padding(horizontal = 18.dp, vertical = 10.dp), fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp, color = Q.Pink)
            Text("Schick den Code weiter. Wer ihn eingibt, bekommt genau dieselben Aufgaben.", Modifier.padding(horizontal = 28.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
            NeonButton({ share(DuelCode.shareText(d, hits)) }, Modifier.fillMaxWidth().padding(16.dp), color = Q.Pink) { Text("📤 Herausfordern", fontWeight = FontWeight.Black) }
            NeonButton(onBack, Modifier.fillMaxWidth().padding(horizontal = 16.dp), color = Q.Green) { Text("Fertig ✓", fontWeight = FontWeight.Bold) }
        }
        else -> Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding().verticalScroll(rememberScrollState())) {
            Bar("Duell", onBack)
            Speech("Fünf Aufgaben, ein Code. Schick ihn an Freunde – alle bekommen dieselben Aufgaben. Wer trifft öfter beim ersten Versuch?")
            if (d != null) {
                Text("Herausforderung ${d.code}", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = Q.Pink, fontWeight = FontWeight.Bold)
                NeonButton({ n = 0; hits = 0; playing = true }, Modifier.fillMaxWidth().padding(horizontal = 16.dp), color = Q.Green) { Text("▶ ${d.topic.emoji} ${d.topic.title} annehmen", fontWeight = FontWeight.Black) }
            }
            Text("Neues Duell starten", Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp), color = Q.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            JamTopic.entries.forEach { t ->
                NeonButton({ duel = DuelCode.create(t); n = 0; hits = 0; playing = true }, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), color = stageColor(t.color)) { Text("${t.emoji}  ${t.title}", fontWeight = FontWeight.Bold) }
            }
            Text("Code bekommen?", Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp), color = Q.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(code, { code = it; error = false }, Modifier.weight(1f).testTag("duell-code"), singleLine = true, isError = error,
                    placeholder = { Text("RL-…") }, supportingText = { if (error) Text("Diesen Code kenne ich nicht – bitte prüfen.") })
                NeonButton({ DuelCode.find(code)?.let { duel = it; n = 0; hits = 0; playing = true } ?: run { error = true } }, color = Q.Pink) { Text("Los") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ───────────────────────── Schwachstellen-Training ─────────────────────────

/** Spielt noch einmal die Aufgaben, die nicht beim ersten Versuch geklappt haben. */
@Composable
fun TrainingPlay(levels: List<Level>, onSolved: (Level, Int, Int) -> Unit, title: String = "Fehler wiederholen", emoji: String = "🔁", onBack: () -> Unit) {
    var idx by rememberSaveable { mutableIntStateOf(0) }
    val level = levels.getOrNull(idx)
    if (level == null) {
        Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            Bar(title, onBack)
            Spacer(Modifier.height(40.dp))
            Lux(LuxMood.HAPPY, size = 130.dp)
            Text(if (levels.isEmpty()) "Nichts offen!" else "Runde geschafft!", fontSize = 26.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 12.dp))
            Text(if (levels.isEmpty()) "Alle gespielten Aufgaben hast du beim ersten Versuch gelöst. Hier landen nur Aufgaben, die mehrere Anläufe gebraucht haben."
                else if (levels.size == 1) "Du hast deinen Wackelkandidaten noch einmal gespielt." else "Du hast alle ${levels.size} Wackelkandidaten noch einmal gespielt.", Modifier.padding(horizontal = 28.dp, vertical = 10.dp), textAlign = TextAlign.Center)
            NeonButton(onBack, Modifier.padding(16.dp), color = Q.Green) { Text("Zurück", fontWeight = FontWeight.Bold) }
        }
        return
    }
    val chapter = Levels.chapters.first { it.number == level.chapter }
    LevelScreen(level, chapter, false, {}, onSolved = { s, a -> onSolved(level, s, a) }, onNext = { idx++ }, onBack = onBack,
        caption = "$emoji $title · ${idx + 1}/${levels.size} · Aufgabe ${Levels.label(level.id)}")
}

// ───────────────────────── Lux-Werkstatt ─────────────────────────

@Composable
fun WorkshopScreen(tourStars: Int, selected: String, onSelect: (String) -> Unit, badges: Int = 0, look: Pair<String, String> = "none" to "none",
                   onLook: (trail: String, hat: String) -> Unit = { _, _ -> }, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        Bar("Lux gestalten", onBack) { Text("★ $tourStars · 🏅 $badges", color = Q.Yellow, fontWeight = FontWeight.Bold) }
        CompositionLocalProvider(LocalLuxSkin provides selected) { Lux(LuxMood.HAPPY, size = 150.dp) }
        Text("Such dir Lux’ Leuchtfarbe aus. Neue Farben schaltest du mit Sternen aus der Tour frei.", Modifier.padding(horizontal = 24.dp, vertical = 8.dp), textAlign = TextAlign.Center, color = Q.Muted)
        // Zubehör: Leuchtspur und Aufsatz, freigeschaltet durch Erfolge (in der 3D-Bühne zu sehen)
        listOf("Leuchtspur" to Looks.trails, "Aufsatz" to Looks.hats).forEachIndexed { row, (title, list) ->
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, Modifier.width(78.dp), color = Q.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                list.forEach { x ->
                    val open = badges >= x.badges; val on = (if (row == 0) look.first else look.second) == x.id
                    Pick(if (open) "${x.emoji} ${x.name}" else "🔒 ${x.name} · ${count(x.badges, "Erfolg", "Erfolge")}", on, Q.Cyan, "zubehoer-${x.id}-$row") { if (open) { if (row == 0) onLook(x.id, look.second) else onLook(look.first, x.id) } }
                }
            }
        }
        LazyVerticalGrid(GridCells.Fixed(2), contentPadding = PaddingValues(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(Skins.all) { _, s ->
                val open = tourStars >= s.stars; val c = stageColor(s.color); val interaction = remember { MutableInteractionSource() }
                Row(Modifier.pressBounce(interaction).alpha(if (open) 1f else 0.5f).clip(RoundedCornerShape(18.dp)).background(horizontalFade(listOf(c.copy(alpha = 0.25f), Q.Deep)))
                    .border(if (s.color == selected) 2.5.dp else 1.dp, if (s.color == selected) Q.Text else c.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                    .clickable(interaction, null, enabled = open) { onSelect(s.color) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(30.dp).background(c, CircleShape))
                    Column(Modifier.padding(start = 10.dp)) {
                        Text(s.name, fontWeight = FontWeight.Bold)
                        Text(if (open) (if (s.color == selected) "ausgewählt ✓" else "frei") else "🔒 ab ★ ${s.stars}", color = Q.Muted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ───────────────────────── Show-Baukasten ─────────────────────────

/** Eigene Lichtshow bauen: Laser, Scheiben und Kugeln per Formel setzen, starten, als Code teilen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BuilderScreen(initial: List<ShowItem>, onSave: (List<ShowItem>) -> Unit, onBack: () -> Unit, controller: StageController = remember { StageController() }) {
    var items by remember { mutableStateOf(initial) }
    var sel by rememberSaveable { mutableIntStateOf(0) }
    var load by remember { mutableStateOf(false) }
    var calc by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var bad by remember { mutableStateOf(false) }
    val share = LocalShareText.current; val shareCard = LocalShareCard.current; val scope = rememberCoroutineScope(); val sfx = LocalSfx.current
    fun change(l: List<ShowItem>) { items = l; onSave(l) }
    LaunchedEffect(items) { controller.setState(Show.scene(items)) }

    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding()) {
        Bar("3D-Rechner & Baukasten", onBack) { Text("${items.size}/${Show.MAX}", color = Q.Muted) }
        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp)) { Stage(controller, Modifier.fillMaxSize()); StageFrame(Q.Violet) }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            items.forEachIndexed { i, it ->
                val interaction = remember { MutableInteractionSource() }
                Text("${it.type.emoji} ${i + 1}", Modifier.pressBounce(interaction).clip(RoundedCornerShape(12.dp)).background(if (i == sel) Q.Violet.copy(alpha = 0.4f) else Q.Deep)
                    .border(if (i == sel) 2.dp else 1.dp, if (i == sel) Q.Text else Q.Muted.copy(alpha = 0.4f), RoundedCornerShape(12.dp)).clickable(interaction, null) { sel = i }.padding(horizontal = 12.dp, vertical = 8.dp), fontWeight = FontWeight.Bold)
            }
            if (items.size < Show.MAX) ShowType.entries.forEach { t ->
                val interaction = remember { MutableInteractionSource() }
                Text("+${t.emoji}", Modifier.testTag("neu-${t.name}").pressBounce(interaction).clip(RoundedCornerShape(12.dp)).border(1.dp, Q.Green.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .clickable(interaction, null) { val at = items.size; change(items + ShowItem(t, t.start)); sel = at; sfx?.play(Sound.TAP) }.padding(horizontal = 12.dp, vertical = 8.dp), color = Q.Green, fontWeight = FontWeight.Bold)
            }
        }
        val item = items.getOrNull(sel)
        if (item != null) {
            Text("${item.type.title}:  ${item.text}", Modifier.fillMaxWidth().padding(horizontal = 12.dp).background(Q.Deep, RoundedCornerShape(12.dp)).padding(10.dp), fontWeight = FontWeight.Medium)
            FlowRow(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), maxItemsInEachRow = 3) {
                item.type.labels.forEachIndexed { i, label ->
                    Stepper(label, item.v[i], Geo.num(item.v[i]), Q.Violet, "show-$i", Modifier.weight(1f), canDown = item.v[i] > item.type.min[i], canUp = item.v[i] < item.type.max[i]) { d ->
                        change(items.toMutableList().also { l -> l[sel] = item.set(i, item.v[i] + d) }); sfx?.play(Sound.TICK)
                    }
                }
            }
        } else Speech("Füg mit + einen Laser, eine Scheibe oder eine Kugel hinzu.")
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeonButton({ if (item != null) { change(items.filterIndexed { i, _ -> i != sel }); sel = (sel - 1).coerceAtLeast(0) } }, color = Q.Orange, enabled = item != null) { Text("🗑") }
            NeonButton({ load = true; code = ""; bad = false }, color = Q.Muted) { Text("Code") }
            NeonButton({ calc = true }, Modifier.testTag("rechner"), color = Q.Yellow, enabled = items.size >= 2) { Text("🧮") }
            NeonButton({ share("Meine Raumlicht-Show: ${Show.encode(items)}") }, color = Q.Pink, enabled = items.isNotEmpty()) { Text("📤") }
            NeonButton({
                scope.launch {
                    sfx?.play(Sound.LAUNCH)
                    // Im Takt der Musik (104 Schläge pro Minute): auf jeden Schlag ein Laser, auf jeden vierten ein Lichtgewitter
                    val lasers = items.mapIndexedNotNull { i, it -> it.beam(i) }
                    repeat(12) { beat ->
                        if (lasers.isNotEmpty()) controller.fire(lasers[beat % lasers.size], 260)
                        if (beat % 4 == 3) controller.party(1.2)
                        sfx?.play(Sound.TICK, 1f + (beat % 4) * 0.1f); delay(577)
                    }
                    controller.party(5.0); sfx?.play(Sound.WIN)
                }
            }, Modifier.weight(1f), color = Q.Green, enabled = items.isNotEmpty()) { Text("▶ Show", fontWeight = FontWeight.Black) }
        }
    }
    // 3D-Rechner: was sich aus den Bausteinen ausrechnen lässt (zum Nachprüfen von Schulbuch-Aufgaben)
    if (calc) AlertDialog(onDismissRequest = { calc = false }, confirmButton = { TextButton({ calc = false }) { Text("Schließen") } }, title = { Text("🧮 3D-Rechner") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val facts = Show.facts(items)
            if (facts.isEmpty()) Text("Dazu kann ich nichts ausrechnen. Probier Laser, Scheibe, Kugel und Punkt in Kombination.")
            facts.forEach { Text("› $it", style = MaterialTheme.typography.bodyMedium) }
        }
    })
    if (load) AlertDialog(onDismissRequest = { load = false }, confirmButton = {
        TextButton({ Show.decode(code)?.let { change(it); sel = 0; load = false } ?: run { bad = true } }) { Text("Laden") }
    }, dismissButton = { TextButton({ load = false }) { Text("Abbrechen") } }, title = { Text("Show-Code laden") }, text = {
        Column {
            Text("Dein Code: ${Show.encode(items)}", style = MaterialTheme.typography.bodySmall, color = Q.Muted)
            OutlinedTextField(code, { code = it; bad = false }, singleLine = true, isError = bad, placeholder = { Text("RLS-…") }, supportingText = { if (bad) Text("Das ist kein gültiger Show-Code.") })
        }
    })
}
