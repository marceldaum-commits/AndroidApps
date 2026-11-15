package de.kurvenflug.app.ui.story

import androidx.compose.animation.core.*
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kurvenflug.app.ui.flight.LevelScreen
import de.kurvenflug.app.ui.fx.*
import de.kurvenflug.app.ui.theme.Q
import de.kurvenflug.core.model.*
import kotlinx.coroutines.delay
import kotlin.math.sin

/** Ein Crew-Mitglied sagt etwas – Sprechblase mit wackelndem Bild. */
@Composable
fun CrewSays(m: CrewMember, text: String, modifier: Modifier = Modifier) {
    val t by rememberInfiniteTransition(label = "crew").animateFloat(0f, 1000f, infiniteRepeatable(tween(1_000_000, easing = LinearEasing)), label = "t")
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(m.emoji, fontSize = 40.sp, modifier = Modifier.graphicsLayer { rotationZ = 6f * sin(t * 2.5f); translationY = 4f * sin(t * 3.1f) })
        Spacer(Modifier.width(8.dp))
        Column(Modifier.clip(RoundedCornerShape(16.dp)).background(Q.Panel).border(1.5.dp, Q.Pink.copy(alpha = 0.6f), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(m.name, color = Q.Pink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            Text(text, color = Q.Text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Karte auf der Startseite, wenn ein neuer Notruf da ist. */
@Composable
fun RescueCallCard(m: CrewMember, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val sfx = LocalSfx.current
    Row(Modifier.fillMaxWidth().pressBounce(interaction).pulse(true, 0.015f).clip(RoundedCornerShape(20.dp))
        .background(Brush.horizontalGradient(listOf(Q.Pink.copy(alpha = 0.3f), Q.Panel))).border(2.dp, Q.Pink, RoundedCornerShape(20.dp))
        .clickable(interaction, null) { sfx?.play(Sound.TAP); onClick() }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("🆘", fontSize = 32.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Notruf empfangen!", fontWeight = FontWeight.Black, color = Q.Text)
            Text("Jemand braucht Hilfe – 5 Etappen, gemischte Aufgaben", style = MaterialTheme.typography.bodySmall, color = Q.Muted)
        }
        Text("›", color = Q.Pink, fontWeight = FontWeight.Black, fontSize = 22.sp)
    }
}

/** Crew & Notrufe: gerettete Freunde, offene Notrufe, was noch fehlt. */
@Composable
fun CrewScreen(rescued: Set<Int>, parts: Int, gold: Int, onRescue: (Int) -> Unit, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Starfield(seed = 83)
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Q.Text) }
                Text("🎖️ Crew · ${rescued.size}/${Season2.crew.size}", style = MaterialTheme.typography.titleLarge, color = Q.Text)
            }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Volts Freunde sind in der Galaxie gestrandet. Jede Rettung: 5 Etappen mit Aufgaben aus allen Themen – so oft probieren, wie du willst.",
                    color = Q.Muted, style = MaterialTheme.typography.bodySmall)
                Season2.crew.forEach { m ->
                    val saved = m.n in rescued
                    val lock = Season2.lock(m.n, parts, gold)
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        .background(if (saved) Q.Green.copy(alpha = 0.12f) else if (lock == null) Q.Pink.copy(alpha = 0.15f) else Q.Panel)
                        .border(1.5.dp, if (saved) Q.Green.copy(alpha = 0.5f) else if (lock == null) Q.Pink else Q.Muted.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
                        .padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (saved || lock == null) m.emoji else "❔", fontSize = 34.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (saved) m.name else if (lock == null) "Notruf von ${m.name}!" else "???", fontWeight = FontWeight.Black, color = Q.Text)
                                Text(if (saved) m.role else lock ?: "Bereit zur Rettung", style = MaterialTheme.typography.labelSmall, color = Q.Muted)
                            }
                            if (saved) Text("✅", fontSize = 20.sp)
                        }
                        if (saved) Text("„${m.lines.first()}“", color = Q.Text, style = MaterialTheme.typography.bodySmall)
                        else if (lock == null) {
                            Text("„${m.call}“", color = Q.Text, style = MaterialTheme.typography.bodySmall)
                            NeonButton(onClick = { onRescue(m.n) }, color = Q.Pink, modifier = Modifier.fillMaxWidth()) { Text("🚀 Zur Rettung!", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
}

/** Eine Rettung: 5 Etappen nacheinander, ohne Scheitern – am Ende zieht der Freund in die Crew ein. */
@Composable
fun RescueScreen(n: Int, probeColor: Color, onWon: (stars: Int, misses: Int) -> Unit, onBack: () -> Unit, reward: ((Int) -> StickerDrop?)? = null) {
    val m = Season2.member(n) ?: return
    val stages = remember(n) { Season2.stages(n) }
    var stage by rememberSaveable { mutableIntStateOf(0) }
    var misses by rememberSaveable { mutableIntStateOf(0) }
    var intro by rememberSaveable { mutableStateOf(true) }
    var won by rememberSaveable { mutableStateOf(false) }
    val sfx = LocalSfx.current
    val frame = remember(n) { Chapter(0, "Rettung: ${m.name}", "gemischte Aufgaben", m.emoji, emptyList(), stages) }
    Box(Modifier.fillMaxSize()) {
        key(stage) {
            LevelScreen(
                stages[stage], frame, probeColor, false, {},
                onSolved = { _, _, _ -> if (stage == stages.lastIndex) { won = true; onWon(Bosses.stars(misses), misses) } },
                onNext = if (stage < stages.lastIndex) ({ stage++ }) else ({ }),
                onBack = onBack,
                nextLabel = if (stage < stages.lastIndex) "Weiter zur Etappe ${stage + 2} ›" else "Gerettet! 🎉",
                onMiss = { misses++ },
                header = "🆘 ${m.emoji} ${m.name} retten · " + "🟢".repeat(stage) + "⚪".repeat(stages.size - stage),
                onReward = reward,
            )
        }
        if (intro) Box(Modifier.fillMaxSize().background(Color(0xEE070B20)).clickable(remember { MutableInteractionSource() }, null) {}, contentAlignment = Alignment.Center) {
            LaunchedEffect(Unit) { sfx?.play(Sound.BEEP) }
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("🆘 Notruf!", style = MaterialTheme.typography.headlineMedium, color = Q.Pink)
                CrewSays(m, m.call)
                Text("5 Etappen mit Aufgaben aus verschiedenen Themen. Kein Zeitdruck – probier so oft, wie du willst.", color = Q.Muted, textAlign = TextAlign.Center)
                NeonButton(onClick = { intro = false }, color = Q.Pink) { Text("Wir kommen! 🚀", fontWeight = FontWeight.Black) }
            }
        }
        if (won) Box(Modifier.fillMaxSize().background(Color(0xEE070B20)).clickable(remember { MutableInteractionSource() }, null) {}, contentAlignment = Alignment.Center) {
            val pop = remember { Animatable(0.3f) }
            LaunchedEffect(Unit) { delay(300); sfx?.play(Sound.WIN); pop.animateTo(1f, spring(dampingRatio = 0.4f)) }
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(m.emoji, fontSize = 80.sp, modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value })
                Text("${m.name} ist gerettet!", style = MaterialTheme.typography.headlineMedium, color = Q.Green, textAlign = TextAlign.Center)
                CrewSays(m, m.thanks)
                Text("Neu in deiner Crew: ${m.role}", color = Q.Muted)
                NeonButton(onClick = onBack, color = Q.Green) { Text("Willkommen an Bord! 🛸", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
