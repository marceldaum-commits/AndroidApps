package de.experimentopia.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.experimentopia.app.ui.blubb.BlubbSays
import de.experimentopia.app.ui.blubb.Mood
import de.experimentopia.app.ui.common.*
import de.experimentopia.app.ui.fx.*
import de.experimentopia.app.ui.theme.E
import de.experimentopia.core.model.*

/** Farbe je Fach */
fun Subject.color(): Color = when (this) { Subject.PHYSIK -> E.Sky; Subject.CHEMIE -> E.Pink; Subject.BIOLOGIE -> E.Green }

/** Die drei Fach-Karten für den Startbildschirm */
@Composable
fun SubjectCards(results: Map<String, Int>, onSubject: (Subject) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Subject.entries.forEachIndexed { i, s ->
            val list = Catalog.all.filter { it.topic.subject == s }
            val done = list.count { it.id in results }
            Column(Modifier.weight(1f).popIn(i * 80).clip(RoundedCornerShape(20.dp)).background(s.color().copy(alpha = 0.16f))
                .border(2.dp, s.color(), RoundedCornerShape(20.dp)).clickable { onSubject(s) }.padding(vertical = 12.dp, horizontal = 6.dp).testTag("fach_${s.name}"),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(s.emoji, fontSize = 32.sp)
                Text(s.title, color = E.Text, fontWeight = FontWeight.Black, fontSize = 15.sp, maxLines = 1)
                Text("$done von ${list.size}", color = E.Muted, fontSize = 11.sp)
            }
        }
    }
}

/**
 * Lernen nach Fach: Fachbereiche in Schulstoff-Reihenfolge, gefiltert nach Klasse. Je Fachbereich: Lernziel (Grundidee),
 * Fortschritt, „Nächster Versuch“, Theorie und alle Versuche zum Aufklappen.
 */
@Composable
fun SubjectScreen(start: Subject, results: Map<String, Int>, grade: Int, locked: (Experiment) -> Boolean, onOpen: (Experiment) -> Unit, onGrade: (Int) -> Unit, onBack: () -> Unit) {
    var subject by remember { mutableStateOf(start) }
    var theoryTopic by remember { mutableStateOf<Topic?>(null) }
    var open by remember { mutableStateOf<Topic?>(null) }
    val done = results.keys
    val topics = Curriculum.topics(subject, grade)
    Box(Modifier.fillMaxSize()) {
        BubbleBackground(tint = subject.color())
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Bar("${subject.emoji} ${subject.title}", onBack)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ChipRow(Subject.entries, subject, { "${it.emoji} ${it.title}" }, { subject = it; open = null }, color = subject.color())
                BlubbSays(de.experimentopia.core.model.Say.subject(subject.title), Mood.HAPPY, size = 52.dp, speak = false)
                Text("Deine Klasse", color = E.Muted, fontSize = 12.sp)
                ChipRow(listOf(0) + (3..10).toList(), grade, { if (it == 0) "Alle" else "Kl. $it" }, onGrade, Modifier.testTag("klasse"), color = E.Sun)
                if (topics.isEmpty()) Text("Für diese Klasse gibt es in ${subject.title} noch keine Versuche – probier eine andere Klasse.", color = E.Muted)
                topics.forEachIndexed { i, t ->
                    val (n, total) = Curriculum.progress(t, done)
                    val next = Curriculum.next(t, done, grade)
                    val frac by animateFloatAsState(n.toFloat() / total, tween(700), label = "p")
                    Column(Modifier.fillMaxWidth().popIn(i * 60).clip(RoundedCornerShape(20.dp)).background(E.Deep)
                        .border(2.dp, if (n == total) E.Green else subject.color().copy(alpha = 0.6f), RoundedCornerShape(20.dp)).padding(12.dp).testTag("bereich_${t.name}"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(36.dp).clip(CircleShape).background(subject.color()), contentAlignment = Alignment.Center) {
                                Text(if (n == total) "✓" else "${i + 1}", color = E.Night, fontWeight = FontWeight.Black)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${t.emoji} ${t.title}", color = E.Text, fontWeight = FontWeight.Black)
                                Text("Kl. ${Curriculum.grades(t).first}–${Curriculum.grades(t).last} · $n von $total Versuchen", color = E.Muted, fontSize = 12.sp)
                            }
                        }
                        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(E.Panel)) {
                            Box(Modifier.fillMaxWidth(frac.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(if (n == total) E.Green else subject.color()))
                        }
                        Text("🎯 ${Theory.of(t).idea}", color = E.Text, fontSize = 13.sp)
                        if (next != null) GlowButton({ onOpen(next) }, Modifier.fillMaxWidth().testTag("weiter_${t.name}"), color = subject.color()) {
                            Text("▶️ Weiter: ${if (locked(next)) "🔒 " else ""}${next.title} (${next.gradeText})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        } else Text("🏆 Alles geschafft in diesem Bereich!", color = E.Green, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            TheoryButton(t, { theoryTopic = t }, long = false)
                            Text(if (open == t) "▲ Versuche zuklappen" else "▼ Alle $total Versuche", color = E.Sky, fontSize = 13.sp,
                                modifier = Modifier.clip(CircleShape).clickable { open = if (open == t) null else t }.padding(horizontal = 10.dp, vertical = 7.dp))
                        }
                        AnimatedVisibility(open == t) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Curriculum.experiments(t, grade).forEach { e -> ExperimentRow(e, results[e.id], grade, locked(e)) { onOpen(e) } }
                            }
                        }
                    }
                }
                ComingSoon(subject, grade, topics.size)
                Spacer(Modifier.height(24.dp))
            }
        }
        TheorySheet(theoryTopic != null, theoryTopic ?: Topic.AUFTRIEB, pro = grade >= 7) { theoryTopic = null }
    }
}

/** Themen aus dem Lehrplan, an denen noch gebaut wird – ohne Datum, ohne Kaufhinweis. Antippen lässt die Kachel wackeln. */
@Composable
private fun ComingSoon(subject: Subject, grade: Int, offset: Int) {
    val list = Coming.of(subject, grade)
    if (list.isEmpty()) return
    var poked by remember(subject) { mutableStateOf<Upcoming?>(null) }
    var pokes by remember { mutableIntStateOf(0) }
    Spacer(Modifier.height(4.dp))
    Text("🚧 Kommt bald", color = E.Text, fontWeight = FontWeight.Black, fontSize = 17.sp, modifier = Modifier.testTag("kommt_bald"))
    Text("Daran forschen wir gerade – aus dem Lehrplan der Klassen 5–10.", color = E.Muted, fontSize = 12.sp)
    list.chunked(2).forEachIndexed { r, row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEachIndexed { j, u ->
                val me = poked == u
                Column(Modifier.weight(1f).popIn((offset + r * 2 + j) * 50).shake(if (me) pokes else 0).clip(RoundedCornerShape(18.dp)).background(E.Deep.copy(alpha = 0.7f))
                    .border(2.dp, subject.color().copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                    .clickable { poked = u; pokes++ }.padding(10.dp).testTag("bald_${r * 2 + j}"), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(u.emoji, fontSize = 26.sp)
                        Spacer(Modifier.weight(1f))
                        Text("⏳ bald", color = E.Sun, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(CircleShape).background(E.Sun.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                    Text(u.title, color = E.Text.copy(alpha = 0.85f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(u.gradeText, color = E.Muted, fontSize = 11.sp)
                    AnimatedVisibility(me) { Text(u.teaser, color = E.Text, fontSize = 12.sp) }
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}
