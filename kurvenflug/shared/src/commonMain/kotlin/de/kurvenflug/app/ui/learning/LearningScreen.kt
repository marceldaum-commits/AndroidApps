package de.kurvenflug.app.ui.learning

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kurvenflug.app.ui.fx.*
import de.kurvenflug.app.ui.theme.Q
import de.kurvenflug.app.ui.volt.Volt
import de.kurvenflug.app.ui.volt.VoltMood
import de.kurvenflug.core.model.*
import kotlinx.coroutines.delay

private fun Mastery.color() = when (this) { Mastery.NEW -> Q.Muted; Mastery.PRACTICING -> Q.Orange; Mastery.SECURE -> Q.Green }

private fun ago(last: Long, now: Long): String {
    if (last == 0L) return "noch nicht geübt"
    return when (val d = ((now - last) / Learning.DAY_MS).toInt()) { 0 -> "heute geübt"; 1 -> "gestern geübt"; else -> "vor $d Tagen geübt" }
}

/** Karte auf der Startseite: Lernstand auf einen Blick, pulsiert, wenn heute etwas aufzufrischen ist. */
@Composable
fun LearningCard(status: List<TopicStatus>, review: Level?, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val sfx = LocalSfx.current
    val secure = status.count { it.mastery == Mastery.SECURE }
    val practicing = status.count { it.mastery == Mastery.PRACTICING }
    val topic = review?.let { r -> status.firstOrNull { it.chapter.number == r.chapter } }
    Row(
        Modifier.fillMaxWidth().pressBounce(interaction).pulse(review != null, 0.015f).clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(Q.Green.copy(alpha = 0.22f), Q.Panel)))
            .border(2.dp, if (review != null) Q.Green else Q.Green.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .clickable(interaction, null) { sfx?.play(Sound.TAP); onClick() }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (review != null) "🔁" else "📊", fontSize = 32.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(if (topic != null) "Auffrischen: ${topic.chapter.emoji} ${topic.chapter.title}" else "Mein Lernstand",
                fontWeight = FontWeight.Black, color = Q.Text)
            Text(if (topic != null) "Ein kurzes Level, damit es sitzt" else "$secure sicher · $practicing übt noch",
                style = MaterialTheme.typography.bodySmall, color = Q.Muted)
        }
        Text("›", color = Q.Green, fontWeight = FontWeight.Black, fontSize = 22.sp)
    }
}

/**
 * Lernstand für Kind und Eltern: je Thema Sterne, Zustand („Neu“, „Übt noch“, „Sicher“) und wann zuletzt geübt.
 * Oben das heutige Auffrisch-Level (verteiltes Wiederholen), unten eine kurze Erklärung für Eltern.
 */
@Composable
fun LearningScreen(status: List<TopicStatus>, review: Level?, doneToday: Boolean, now: Long, onReview: (Level) -> Unit, onBack: () -> Unit,
                   stars: Map<String, Int> = emptyMap(), onLevel: (Level) -> Unit = onReview, initialOpen: Int? = null) {
    // Antippen eines Themas klappt seine Level auf – mit Sternen, zum gezielten Nochmal-Spielen
    var open by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(initialOpen) }
    val topic = review?.let { r -> status.firstOrNull { it.chapter.number == r.chapter } }
    val secure = status.count { it.mastery == Mastery.SECURE }
    val started = status.count { it.mastery != Mastery.NEW }
    val speech = when {
        topic != null -> "Heute kurz auffrischen: ${topic.chapter.title}! Wer in Abständen wiederholt, behält es viel länger. 🧠"
        doneToday -> "Heute schon aufgefrischt – stark! 💪 Morgen schaue ich wieder nach."
        started == 0 -> "Spiel ein paar Level – dann zeige ich dir hier, was schon sitzt. 🚀"
        else -> "Alles frisch! Ich sage Bescheid, wenn ein Thema wieder dran ist. ✨"
    }
    Box(Modifier.fillMaxSize()) {
        Starfield(seed = 57)
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Q.Text) }
                Text("📊 Lernstand", style = MaterialTheme.typography.titleLarge, color = Q.Text)
            }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Volt(if (topic != null) VoltMood.THINK else if (secure > 0) VoltMood.HAPPY else VoltMood.IDLE, size = 60.dp, speech = speech)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Mastery.entries.reversed().forEach { m ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(m.color().copy(alpha = 0.15f)).padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${m.emoji} ${status.count { it.mastery == m }}", fontWeight = FontWeight.Black, color = m.color(), fontSize = 20.sp)
                            Text(m.label, style = MaterialTheme.typography.labelSmall, color = Q.Muted)
                        }
                    }
                }
                if (review != null && topic != null) NeonButton(onClick = { onReview(review) }, color = Q.Green, modifier = Modifier.fillMaxWidth()) {
                    Text("🔁 Jetzt auffrischen: ${topic.chapter.title}", fontWeight = FontWeight.Bold)
                }
                status.forEachIndexed { i, s ->
                    val expanded = open == s.chapter.number
                    TopicRow(s, now, i, expanded) { open = if (expanded) null else s.chapter.number }
                    androidx.compose.animation.AnimatedVisibility(expanded,
                        enter = androidx.compose.animation.expandVertically(spring(dampingRatio = 0.8f)) + androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()) {
                        de.kurvenflug.app.ui.flight.ChapterCard(s.chapter, i, stars, onLevel, header = false)
                    }
                }
                ParentNote()
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun TopicRow(s: TopicStatus, now: Long, index: Int, expanded: Boolean = false, onClick: () -> Unit = {}) {
    val sfx = LocalSfx.current
    val interaction = remember { MutableInteractionSource() }
    // Balken wachsen nacheinander – Fortschritt soll man sehen
    val bar = remember { Animatable(0f) }
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(s.share) {
        delay(80L * index)
        pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
    }
    LaunchedEffect(s.share) { delay(80L * index + 120); bar.animateTo(s.share, tween(700, easing = FastOutSlowInEasing)) }
    val color = s.mastery.color()
    Column(
        Modifier.fillMaxWidth().graphicsLayer { scaleX = pop.value; scaleY = pop.value; alpha = pop.value.coerceIn(0f, 1f) }
            .clip(RoundedCornerShape(16.dp)).background(Q.Panel)
            .border(1.5.dp, if (s.reviewDue) Q.Green else color.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .pressBounce(interaction).clickable(interaction, null) { sfx?.play(Sound.TAP); onClick() }.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.chapter.emoji, fontSize = 24.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(s.chapter.title, fontWeight = FontWeight.Bold, color = Q.Text)
                Text("${s.chapter.grades} · ${ago(s.lastPractice, now)}", style = MaterialTheme.typography.labelSmall, color = Q.Muted)
            }
            Text("${s.mastery.emoji} ${s.mastery.label}", color = color, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 4.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color.White.copy(alpha = 0.08f))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(bar.value).clip(RoundedCornerShape(5.dp))
                    .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.7f), color))))
            }
            Spacer(Modifier.width(10.dp))
            Text("${s.stars}/${s.maxStars} ⭐", style = MaterialTheme.typography.labelMedium, color = Q.Yellow)
        }
        if (s.reviewDue) Text("🔁 Mal wieder dran – kurz auffrischen hilft dem Gedächtnis", style = MaterialTheme.typography.labelSmall, color = Q.Green)
        Text(if (expanded) "▲ Level zuklappen" else "▼ Level anzeigen und nochmal spielen", style = MaterialTheme.typography.labelSmall, color = Q.Cyan)
    }
}

@Composable
private fun ParentNote() {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Q.Violet.copy(alpha = 0.15f)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("👪 Für Eltern", fontWeight = FontWeight.Black, color = Q.Text)
        Text("⭐ Sterne zeigen, wie sicher ein Thema sitzt: 3 Sterne heißt „beim ersten Versuch richtig gerechnet“.",
            style = MaterialTheme.typography.bodySmall, color = Q.Muted)
        Text("🔁 Geschaffte Themen kommen nach 2 Tagen (wackelig) bzw. 7 Tagen (sicher) als kurzes Auffrisch-Level wieder. " +
            "Wiederholen in Abständen hilft nachweislich beim Behalten. Verpasste Tage verfallen einfach – kein Druck.",
            style = MaterialTheme.typography.bodySmall, color = Q.Muted)
        Text("🔒 Alles bleibt auf diesem Gerät. Kein Konto, keine Werbung, keine Daten nach draußen.",
            style = MaterialTheme.typography.bodySmall, color = Q.Muted)
    }
}
