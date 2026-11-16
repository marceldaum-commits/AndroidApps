package de.experimentopia.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.experimentopia.app.ui.common.*
import de.experimentopia.app.ui.fx.BubbleBackground
import de.experimentopia.app.ui.theme.E
import de.experimentopia.app.ui.theme.color
import de.experimentopia.core.model.*

/** Gezielt suchen: nach Fach, nach Thema (Fachbereich) oder mit einem Stichwort */
@Composable
fun TopicScreen(results: Map<String, Int>, grade: Int, locked: (Experiment) -> Boolean, onOpen: (Experiment) -> Unit, onBack: () -> Unit,
                startTopic: Topic? = null, startQuery: String = "") {
    var subject by remember { mutableStateOf<Subject?>(null) }
    var topic by remember { mutableStateOf(startTopic) }
    var query by remember { mutableStateOf(startQuery) }
    val found = Search.find(query, subject, topic)
    Box(Modifier.fillMaxSize()) {
        BubbleBackground(tint = E.Sky)
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Bar("🔎 Themen & Suche", onBack)
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().testTag("suche"), singleLine = true,
                    label = { Text("Suchen, z. B. Newton, Magnet, Säure, Pendel") }, leadingIcon = { Text("🔎") },
                    trailingIcon = { if (query.isNotEmpty()) Text("✖️", Modifier.clickable { query = "" }.padding(8.dp)) })
                ChipRow(listOf<Subject?>(null) + Subject.entries, subject, { it?.let { s -> "${s.emoji} ${s.title}" } ?: "Alle Fächer" }, { subject = it; topic = null }, color = E.Sky)
                Text("Fachbereiche", color = E.Text, fontWeight = FontWeight.Black)
                Topic.entries.filter { subject == null || it.subject == subject }.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { t ->
                            val list = t.experiments(); val done = list.count { it.id in results }
                            val sel = topic == t
                            Row(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (sel) E.Panel else E.Deep)
                                .border(2.dp, if (sel) E.Sun else E.Line, RoundedCornerShape(16.dp)).clickable { topic = if (sel) null else t }
                                .padding(10.dp).testTag("thema_${t.name}"), verticalAlignment = Alignment.CenterVertically) {
                                Text(t.emoji, fontSize = 24.sp)
                                Column(Modifier.padding(start = 6.dp)) {
                                    Text(t.title, color = E.Text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("$done/${list.size} · ${t.subject.title}", color = E.Muted, fontSize = 11.sp)
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                Text(if (found.isEmpty()) "Nichts gefunden – probier ein anderes Wort." else "${found.size} Versuche" + (topic?.let { " · ${it.title}" } ?: ""), color = E.Sun, fontWeight = FontWeight.Bold)
                found.groupBy { it.topic }.forEach { (t, list) ->
                    Text("${t.emoji} ${t.title}", color = E.Text, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 4.dp))
                    list.forEach { e -> ExperimentRow(e, results[e.id], grade, locked(e)) { onOpen(e) } }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** Eine Zeile mit Versuch, Insel, Klasse und Sternen */
@Composable
fun ExperimentRow(e: Experiment, stars: Int?, grade: Int, lock: Boolean, onClick: () -> Unit) {
    val fits = grade == 0 || grade in e.grades
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(E.Deep.copy(alpha = if (fits) 1f else 0.55f)).clickable(onClick = onClick)
        .padding(10.dp).testTag("treffer_${e.id}"), verticalAlignment = Alignment.CenterVertically) {
        Text(if (lock) "🔒" else e.emoji, fontSize = 26.sp)
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(e.title, color = E.Text, fontWeight = FontWeight.Bold)
            Text(e.predict.question, color = E.Muted, fontSize = 12.sp, maxLines = 2)
            Text("${e.island.emoji} ${e.island.title} · ${e.gradeText}", color = e.island.color.color(), fontSize = 11.sp)
        }
        if (stars != null) Stars(stars, 12)
    }
}
