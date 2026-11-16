package de.experimentopia.app.ui.journal

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.experimentopia.app.ui.blubb.BlubbSays
import de.experimentopia.app.ui.blubb.Mood
import de.experimentopia.app.ui.common.*
import de.experimentopia.app.ui.fx.BubbleBackground
import de.experimentopia.app.ui.fx.GlowButton
import de.experimentopia.app.ui.fx.dateText
import de.experimentopia.app.ui.fx.popIn
import de.experimentopia.app.ui.theme.E
import de.experimentopia.app.ui.theme.color
import de.experimentopia.core.model.*

enum class JournalTab(val title: String) { VERSUCHE("🧪 Versuche"), WISSEN("📒 Merksätze"), ICH("🧭 Selbsteinschätzung") }

@Composable
fun JournalScreen(entries: List<NoteRow>, onBack: () -> Unit, start: JournalTab = JournalTab.VERSUCHE, onOpen: (Experiment) -> Unit = {}, openFirst: Boolean = false) {
    var tab by remember { mutableStateOf(start) }
    var open by remember { mutableStateOf(if (openFirst) entries.firstOrNull()?.id else null) }
    Box(Modifier.fillMaxSize()) {
        BubbleBackground(tint = E.Sun)
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Bar("📒 Forscherbuch", onBack)
            ChipRow(JournalTab.entries, tab, { it.title }, { tab = it }, Modifier.padding(horizontal = 16.dp), color = E.Sun)
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (entries.isEmpty()) BlubbSays("Dein Forscherbuch ist noch leer. Mach deinen ersten Versuch – ich schreibe alles mit!", Mood.THINK, speak = false)
                when (tab) {
                    JournalTab.VERSUCHE -> {
                        if (entries.isNotEmpty()) Text("Tipp einen Eintrag an – dann steht dort die ganze Erklärung.", color = E.Muted, fontSize = 13.sp)
                        entries.forEachIndexed { i, j ->
                            val e = Catalog.byId(j.experimentId) ?: return@forEachIndexed
                            val expanded = open == j.id
                            Panel(Modifier.fillMaxWidth().popIn((i * 40).coerceAtMost(400)).clickable { open = if (expanded) null else j.id }.testTag("eintrag_${j.id}"), border = if (j.right) E.Mint else E.Sun) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(e.emoji, fontSize = 26.sp)
                                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                        Text(e.title, color = E.Text, fontWeight = FontWeight.Bold)
                                        Text("${dateText(j.at)} · ${e.topic.emoji} ${e.topic.title}" + if (j.daily) " · 📅" else "", color = E.Muted, fontSize = 11.sp)
                                    }
                                    Text(if (j.right) "🎯" else "💡", fontSize = 24.sp)
                                }
                                Text("Ich dachte: ${j.guess}  ${Sure.entries.getOrNull(j.sure)?.let { "${it.emoji} (${it.title})" } ?: ""}", color = E.Text, fontSize = 14.sp)
                                Text("Es war: ${j.result}", color = if (j.right) E.Mint else E.Sun, fontSize = 14.sp)
                                if (expanded) Explanation(e, onOpen) else Text("▾ Erklärung anzeigen", color = E.Sky, fontSize = 13.sp)
                            }
                        }
                    }
                    JournalTab.WISSEN -> {
                        val done = entries.map { it.experimentId }.toSet()
                        Island.entries.forEach { island ->
                            val facts = Catalog.of(island).filter { it.id in done }
                            if (facts.isNotEmpty()) {
                                Text("${island.emoji} ${island.title}", color = island.color.color(), fontWeight = FontWeight.Black)
                                facts.forEach { e ->
                                    var more by remember { mutableStateOf(false) }
                                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(E.Deep).clickable { more = !more }.padding(10.dp)) {
                                        Row { Text(e.emoji, fontSize = 18.sp); Text("  ${e.fact}", color = E.Text, fontSize = 14.sp) }
                                        if (more) Explanation(e, onOpen)
                                    }
                                }
                            }
                        }
                    }
                    JournalTab.ICH -> {
                        val rows = Calibration.rows(entries.map { (Sure.entries.getOrNull(it.sure) ?: Sure.ETWAS) to it.right })
                        BlubbSays(Calibration.advice(rows), Mood.THINK, speak = false)
                        Text("Wie oft lag deine Vermutung richtig – je nachdem, wie sicher du warst?", color = E.Muted, fontSize = 13.sp)
                        rows.forEach { r ->
                            Column(Modifier.testTag("kalibrierung_${r.sure.name}")) {
                                Row { Text("${r.sure.emoji} ${r.sure.title}", color = E.Text, modifier = Modifier.weight(1f)); Text(if (r.total == 0) "–" else "${r.right} von ${r.total}", color = E.Sun, fontWeight = FontWeight.Bold) }
                                Box(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(E.Panel)) {
                                    if (r.total > 0) Box(Modifier.fillMaxWidth(r.rate.toFloat().coerceAtLeast(0.02f)).fillMaxHeight().clip(CircleShape).background(E.Mint))
                                }
                            }
                        }
                        Text("Warum das wichtig ist: Wer gut einschätzen kann, was er schon weiß, lernt gezielter – das nennt man Metakognition.", color = E.Muted, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun BadgesScreen(stats: Stats, onBack: () -> Unit) {
    val earned = Badges.earned(stats).map { it.id }.toSet()
    Box(Modifier.fillMaxSize()) {
        BubbleBackground(tint = E.Pink)
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Bar("🏅 Abzeichen", onBack) { Pill("${earned.size}/${Badges.all.size}", E.Pink) }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Badges.all.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEachIndexed { k, b ->
                            val on = b.id in earned
                            Column(Modifier.weight(1f).popIn(k * 80).clip(RoundedCornerShape(18.dp)).background(if (on) E.Panel else E.Deep.copy(alpha = 0.6f)).padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (on) b.emoji else "❔", fontSize = 34.sp)
                                Text(b.title, color = if (on) E.Text else E.Muted, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(b.text, color = E.Muted, fontSize = 11.sp)
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Ausführliche Erklärung zu einem Versuch – für das Forscherbuch */
@Composable
fun Explanation(e: Experiment, onOpen: (Experiment) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
        Section("❓ Die Frage", e.predict.question + "\n" + e.story)
        Section("👀 Was passiert ist", e.reveal)
        Section("🧠 Warum?", e.why.first())
        Section("📒 Merksatz", e.fact)
        Section("🧩 Knobelfrage", e.transfer.question + "\nLösung: " + e.transfer.choices[e.transfer.answer].text + (if (e.transfer.explain.isNotBlank()) " – " + e.transfer.explain else ""))
        GlowButton({ onOpen(e) }, Modifier.fillMaxWidth(), color = E.Pink) { Text("🔁 Nochmal ausprobieren", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun Section(title: String, text: String) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(E.Panel).padding(10.dp)) {
        Text(title, color = E.Sun, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(text, color = E.Text, fontSize = 14.sp)
    }
}
