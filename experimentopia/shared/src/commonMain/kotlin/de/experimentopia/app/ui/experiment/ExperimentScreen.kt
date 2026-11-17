package de.experimentopia.app.ui.experiment

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.experimentopia.app.ui.blubb.Blubb
import de.experimentopia.app.ui.blubb.BlubbSays
import de.experimentopia.app.ui.blubb.Mood
import de.experimentopia.app.ui.common.*
import de.experimentopia.app.ui.fx.*
import de.experimentopia.app.ui.lab.LabView
import de.experimentopia.app.ui.theme.E
import de.experimentopia.app.ui.theme.color
import de.experimentopia.core.model.*
import kotlinx.coroutines.delay

enum class Phase(val title: String) { STORY("Rätsel"), PREDICT("Vermuten"), LAB("Ausprobieren"), REVEAL("Auflösung"), EXPLAIN("Erklären"), TRANSFER("Knobeln"), DONE("Geschafft") }

/** Was am Ende gespeichert wird */
data class Outcome(val stars: Int, val guess: String, val result: String, val right: Boolean, val sure: Sure, val explainedFirstTry: Boolean)

/**
 * Ein Versuch im Forscher-Kreislauf. Die Vermutung kostet nie Sterne – eine falsche Vermutung ist ein gefeierter Aha-Moment.
 * [startPhase] und [startGuess] nur für Bildschirmfotos und Tests.
 */
@Composable
fun ExperimentScreen(
    e: Experiment,
    onDone: (Outcome) -> Unit,
    onNext: (() -> Unit)?,
    onBack: () -> Unit,
    daily: Boolean = false,
    startPhase: Phase = Phase.STORY,
    startGuess: Int? = null,
) {
    val sfx = LocalSfx.current
    val view = LocalBuzzer.current; val vib = LocalVibration.current
    var phase by remember(e.id) { mutableStateOf(startPhase) }
    var guess by remember(e.id) { mutableStateOf(startGuess) }
    var sure by remember(e.id) { mutableStateOf<Sure?>(if (startGuess != null) Sure.ETWAS else null) }
    var goal by remember(e.id) { mutableStateOf(startPhase > Phase.LAB) }
    var explainTries by remember(e.id) { mutableIntStateOf(0) }
    var explained by remember(e.id) { mutableStateOf(false) }
    var transferPick by remember(e.id) { mutableStateOf<Int?>(null) }
    var confetti by remember(e.id) { mutableIntStateOf(0) }
    var saved by remember(e.id) { mutableStateOf(false) }
    var theory by remember(e.id) { mutableStateOf(false) }
    val right = guess == e.predict.answer
    // Begründungen gemischt, aber für jeden Versuch immer gleich
    val reasons = remember(e.id) { e.why.mapIndexed { i, w -> i to w }.shuffled(kotlin.random.Random(e.id.hashCode())) }
    val stars = Score.stars(explained && explainTries == 1, transferPick == e.transfer.answer)

    val reader = LocalReader.current; val readOn = LocalReadOn.current; val autoRead = LocalAutoRead.current
    DisposableEffect(e.id) { onDispose { reader?.stop() } }
    LaunchedEffect(phase) {
        if (readOn && autoRead) when (phase) {
            Phase.PREDICT -> reader?.say(ReadTexts.question(e)); Phase.LAB -> reader?.say(ReadTexts.task(e)); Phase.TRANSFER -> reader?.say(ReadTexts.transfer(e)); else -> {}
        }
        when (phase) {
            Phase.REVEAL -> if (right) { sfx?.play(Sound.WIN); confetti++ } else sfx?.play(Sound.AHA)
            Phase.DONE -> {
                sfx?.play(Sound.WIN); confetti++; view.buzz(Buzz.HIT, vib)
                if (!saved) { saved = true; onDone(Outcome(stars, e.predict.choices[guess ?: 0].text, e.predict.choices[e.predict.answer].text, right, sure ?: Sure.ETWAS, explainTries == 1)) }
            }
            else -> {}
        }
    }

    Box(Modifier.fillMaxSize()) {
        BubbleBackground(tint = e.island.color.color(), count = 14)
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Bar("${e.emoji} ${e.title}", onBack) { TheoryButton(e.topic, { theory = true }, long = false); Spacer(Modifier.width(6.dp)); Pill(e.gradeText, e.island.color.color()) }
            PhaseDots(phase)
            AnimatedContent(phase, transitionSpec = { (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut()) }, label = "phase", modifier = Modifier.weight(1f)) { ph ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (ph) {
                        Phase.STORY -> {
                            if (daily) Pill("📅 Rätsel des Tages", E.Sun)
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text(e.emoji, fontSize = 84.sp, modifier = Modifier.popIn()) }
                            BlubbSays(e.story, Mood.THINK)
                            GlowButton({ phase = Phase.PREDICT }, Modifier.fillMaxWidth().pulse(true, 0.02f).testTag("los")) { Text("🤔 Ich vermute …", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                        }
                        Phase.PREDICT -> {
                            BlubbSays("Was glaubst du? Es gibt kein Falsch – Forscher vermuten immer zuerst.", Mood.HAPPY, size = 56.dp)
                            Text(e.predict.question, style = MaterialTheme.typography.titleMedium, color = E.Text)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                TheoryButton(e.topic, { theory = true }, Modifier.weight(1f, fill = false)); ReadButton(ReadTexts.question(e))
                            }
                            e.predict.choices.forEachIndexed { i, c -> ChoiceCard(c, guess == i, { guess = i; sfx?.play(Sound.POP) }, Modifier.popIn(i * 60).testTag("wahl$i")) }
                            if (guess != null) {
                                Text("Wie sicher bist du dir?", color = E.Muted)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Sure.entries.forEach { s ->
                                        Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (sure == s) E.Panel else E.Deep)
                                            .border(2.dp, if (sure == s) E.Sun else E.Line, RoundedCornerShape(16.dp))
                                            .clickable { sure = s; sfx?.play(Sound.TAP) }.padding(vertical = 10.dp).testTag("sicher${s.ordinal}"), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(s.emoji, fontSize = 28.sp)
                                            Text(s.title, color = E.Text, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                            GlowButton({ phase = Phase.LAB; sfx?.play(Sound.WHOOSH) }, Modifier.fillMaxWidth().testTag("ausprobieren"), enabled = guess != null && sure != null, color = E.Pink) {
                                Text("🧪 Ausprobieren!", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                        Phase.LAB -> {
                            // die Frage und die eigene Vermutung bleiben sichtbar
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(E.Deep).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("❓", fontSize = 18.sp)
                                Column(Modifier.padding(start = 8.dp)) {
                                    Text(e.predict.question, color = E.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("Deine Vermutung: " + e.predict.choices[guess ?: 0].let { "${it.emoji} ${it.text}" } + "  ${sure?.emoji ?: ""}", color = E.Sun, fontSize = 13.sp)
                                }
                            }
                            TaskBanner(e.task, goal)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                TheoryButton(e.topic, { theory = true }, Modifier.weight(1f, fill = false)); ReadButton(ReadTexts.task(e))
                            }
                            Safety.note(e.id)?.let { SafetyNote(it) }
                            LabView(e.setup, free = false, onGoal = { if (!goal) { goal = true; sfx?.play(Sound.STAR) } })
                            GlowButton({ phase = Phase.REVEAL }, Modifier.fillMaxWidth().pulse(goal, 0.03f).testTag("aufloesung"), enabled = goal, color = E.Sun) {
                                Text(if (goal) "✨ Auflösung!" else "Erst den Auftrag erfüllen …", fontWeight = FontWeight.Bold)
                            }
                        }
                        Phase.REVEAL -> {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(if (right) "🎯" else "💡", fontSize = 80.sp, modifier = Modifier.popIn())
                            }
                            Text(if (right) "Treffer! Gut vermutet!" else "Aha-Moment!", style = MaterialTheme.typography.headlineMedium, color = if (right) E.Mint else E.Sun,
                                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().testTag("ergebnis"))
                            Panel(Modifier.fillMaxWidth(), border = E.Line) {
                                Text(e.predict.question, color = E.Text, fontWeight = FontWeight.Bold)
                                Text("Du hast vermutet:", color = E.Muted, fontSize = 13.sp)
                                Text(e.predict.choices[guess ?: 0].let { "${it.emoji} ${it.text}" }, color = E.Text, fontWeight = FontWeight.Bold)
                                Text("Herausgekommen ist:", color = E.Muted, fontSize = 13.sp)
                                Text(e.predict.choices[e.predict.answer].let { "${it.emoji} ${it.text}" }, color = E.Mint, fontWeight = FontWeight.Bold)
                            }
                            BlubbSays(ReadTexts.revealSay(e, right), if (right) Mood.CHEER else Mood.WOW)
                            GlowButton({ phase = Phase.EXPLAIN }, Modifier.fillMaxWidth().testTag("warum"), color = E.Sky) { Text("🗣️ Blubb fragt: Warum?", fontWeight = FontWeight.Bold) }
                        }
                        Phase.EXPLAIN -> {
                            var wrong by remember { mutableIntStateOf(-1) }
                            var shakeKey by remember { mutableIntStateOf(0) }
                            BlubbSays(when { explained -> "Ahhh! Jetzt versteh ich's! Danke, Professor!"; wrong >= 0 -> "Hmm … das glaube ich nicht ganz. Erklär's mir noch mal anders?"; else -> "Ich verstehe das nicht. Kannst du es mir erklären? Warum ist das so?" },
                                when { explained -> Mood.CHEER; wrong >= 0 -> Mood.OOPS; else -> Mood.THINK })
                            reasons.forEach { (orig, text) ->
                                ChoiceCard(Choice(text), false, {
                                    if (explained) return@ChoiceCard
                                    explainTries++
                                    if (orig == 0) { explained = true; sfx?.play(Sound.DING); view.buzz(Buzz.HIT, vib); if (readOn && autoRead) reader?.say(ReadTexts.fact(e)) } else { wrong = orig; shakeKey++; sfx?.play(Sound.SOFT) }
                                }, state = if (explained && orig == 0) true else if (wrong == orig) false else null, shakeKey = if (wrong == orig) shakeKey else 0,
                                    modifier = Modifier.testTag("grund$orig"))
                            }
                            if (explained) {
                                Panel(border = E.Sun) { Text("📒 Merksatz", color = E.Sun, fontWeight = FontWeight.Bold); Text(e.fact, color = E.Text, fontSize = 16.sp); ReadButton(ReadTexts.fact(e)) }
                                Safety.note(e.id)?.let { SafetyNote(it) }
                                GlowButton({ phase = Phase.TRANSFER }, Modifier.fillMaxWidth().testTag("knobeln"), color = E.Violet) { Text("🧩 Noch eine Knobelfrage!", fontWeight = FontWeight.Bold) }
                            }
                        }
                        Phase.TRANSFER -> {
                            BlubbSays("Jetzt bist du der Profi. Was passiert hier?", Mood.HAPPY, size = 56.dp)
                            Text(e.transfer.question, style = MaterialTheme.typography.titleMedium, color = E.Text)
                            ReadButton(ReadTexts.transfer(e))
                            e.transfer.choices.forEachIndexed { i, c ->
                                ChoiceCard(c, transferPick == i, { if (transferPick == null) { transferPick = i; sfx?.play(if (i == e.transfer.answer) Sound.DING else Sound.SOFT) } },
                                    state = transferPick?.let { if (i == e.transfer.answer) true else if (i == it) false else null }, modifier = Modifier.testTag("transfer$i"))
                            }
                            transferPick?.let { p ->
                                Text(if (p == e.transfer.answer) "✅ Richtig!" else "Fast! Die richtige Antwort ist markiert.", color = if (p == e.transfer.answer) E.Green else E.Sun, fontWeight = FontWeight.Bold)
                                if (e.transfer.explain.isNotBlank()) Text(e.transfer.explain, color = E.Text)
                                GlowButton({ phase = Phase.DONE }, Modifier.fillMaxWidth().testTag("fertig"), color = E.Mint) { Text("🏁 Fertig!", fontWeight = FontWeight.Bold) }
                            }
                        }
                        Phase.DONE -> DoneView(e, stars, right, onNext, onBack)
                    }
                }
            }
        }
        Confetti(confetti)
        TheorySheet(theory, e.topic, pro = e.grades.first >= 7) { theory = false }
    }
}

@Composable
private fun PhaseDots(phase: Phase) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Phase.entries.forEach { p ->
            Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(if (p <= phase) E.Mint else E.Panel))
        }
    }
    Text(phase.title, color = E.Muted, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, top = 2.dp))
}

@Composable
private fun DoneView(e: Experiment, stars: Int, right: Boolean, onNext: (() -> Unit)?, onBack: () -> Unit) {
    val sfx = LocalSfx.current
    var shown by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { repeat(stars) { delay(380); shown++; sfx?.play(Sound.STAR, pitch = 1f + shown * 0.12f) } }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Blubb(Mood.CHEER, size = 120.dp)
        Row { repeat(3) { i -> Text(if (i < shown) "⭐" else "☆", fontSize = 48.sp, color = if (i < shown) E.Sun else E.Muted, modifier = if (i < shown) Modifier.popIn() else Modifier) } }
        Text("⭐ Versuch gemacht   ⭐ Blubb gleich richtig erklärt   ⭐ Knobelfrage gelöst", color = E.Muted, fontSize = 11.sp, textAlign = TextAlign.Center)
        Pill(if (right) "🎯 Treffer gesammelt" else "💡 Aha-Moment gesammelt", if (right) E.Mint else E.Sun)
        Panel(Modifier.fillMaxWidth(), border = E.Sun) { Text("📒 Ins Forscherbuch:", color = E.Sun, fontWeight = FontWeight.Bold); Text(e.fact, color = E.Text) }
        if (onNext != null) GlowButton(onNext, Modifier.fillMaxWidth().pulse(true, 0.02f).testTag("naechster"), color = E.Pink) { Text("➡️ Nächster Versuch", fontWeight = FontWeight.Bold) }
        GlowButton(onBack, Modifier.fillMaxWidth(), color = E.Sky) { Text("🏝️ Zurück zur Insel") }
    }
}
