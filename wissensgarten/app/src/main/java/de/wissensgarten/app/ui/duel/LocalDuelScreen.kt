package de.wissensgarten.app.ui.duel

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.app.ui.fx.*
import de.wissensgarten.core.model.DuelQuestion
import kotlinx.coroutines.delay

private val BLUE = Color(0xFF1E88E5)
private val ORANGE = Color(0xFFFB8C00)

/**
 * Zu zweit an einem Handy: Das Handy liegt zwischen zwei Kindern, die obere Hälfte steht auf dem Kopf.
 * Gleiche Frage für beide – wer zuerst richtig tippt, bekommt den Punkt. Falsch getippt? Nur diese Frage aussetzen.
 */
@Composable
fun LocalDuelScreen(questions: List<DuelQuestion>, title: String, onClose: () -> Unit, onDew: (Int) -> Unit = {}) {
    val sfx = LocalSfx.current
    val particles = rememberParticles()
    var round by remember { mutableIntStateOf(0) }
    var scoreA by remember { mutableIntStateOf(0) }
    var scoreB by remember { mutableIntStateOf(0) }
    var lockedA by remember { mutableStateOf(false) }
    var lockedB by remember { mutableStateOf(false) }
    var winner by remember { mutableStateOf<Int?>(null) }   // 0 = Blau, 1 = Orange, -1 = keiner
    var done by remember { mutableStateOf(false) }
    var restart by remember { mutableIntStateOf(0) }
    val q = questions.getOrNull(round)
    val timeLeft = remember(round, restart) { Animatable(1f) }

    LaunchedEffect(round, restart) {
        lockedA = false; lockedB = false; winner = null
        if (q == null) return@LaunchedEffect
        timeLeft.snapTo(1f)
        timeLeft.animateTo(0f, tween(15_000, easing = LinearEasing))
        if (winner == null) { winner = -1; sfx?.play(Sound.PLOP, pitch = 0.7f) }
    }
    LaunchedEffect(winner) {
        if (winner == null) return@LaunchedEffect
        delay(1500)
        if (round + 1 < questions.size) round++ else {
            done = true; sfx?.play(Sound.FANFARE); particles.burst(Burst.CONFETTI, count = 90)
            // Tautropfen nur für die erste Partie – „Nochmal“ ist zum Spaß, nicht zum Sammeln
            if (restart == 0) onDew(scoreA + scoreB)
        }
    }
    fun tap(player: Int, option: String) {
        val qq = q ?: return
        if (winner != null) return
        if (player == 0 && lockedA || player == 1 && lockedB) return
        if (option == qq.answer) {
            winner = player
            if (player == 0) scoreA++ else scoreB++
            sfx?.play(Sound.CORRECT)
            particles.burst(Burst.SPARKLE, 0.5f, if (player == 0) 0.75f else 0.25f, 30)
        } else {
            sfx?.play(Sound.WRONG, volume = 0.5f)
            if (player == 0) lockedA = true else lockedB = true
            if ((player == 0 && lockedB) || (player == 1 && lockedA)) winner = -1
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFFFFFBF0))) {
        Column(Modifier.fillMaxSize()) {
            // Spieler 2 (oben, gedreht)
            Half(Modifier.weight(1f).rotate(180f), "Orange", ORANGE, scoreB, q, lockedB, winner, 1, done, scoreA, scoreB) { tap(1, it) }
            // Mitte: Thema, Runde, Zeit
            Box(Modifier.fillMaxWidth().height(40.dp).background(Color(0xFF3F6B3A)), contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(timeLeft.value).align(Alignment.CenterStart).background(Color(0x33FFFFFF)))
                Text(if (done) "🏁 Fertig!" else "$title · ${round + 1}/${questions.size}", color = Color.White, fontWeight = FontWeight.Bold)
            }
            // Spieler 1 (unten)
            Half(Modifier.weight(1f), "Blau", BLUE, scoreA, q, lockedA, winner, 0, done, scoreA, scoreB) { tap(0, it) }
        }
        if (done) Row(Modifier.align(Alignment.Center).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            JuicyButton(onClick = { round = 0; scoreA = 0; scoreB = 0; done = false; restart++ }) { Text("🔁 Noch mal") }
            OutlinedButton(onClick = onClose, colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)) { Text("Fertig") }
        }
        ParticleLayer(particles, Modifier.matchParentSize())
    }
}

@Composable
private fun Half(
    modifier: Modifier, name: String, color: Color, score: Int, q: DuelQuestion?, locked: Boolean, winner: Int?, me: Int,
    done: Boolean, scoreA: Int, scoreB: Int, onTap: (String) -> Unit,
) {
    val bump = remember { Animatable(1f) }
    LaunchedEffect(score) { if (score > 0) { bump.snapTo(1.4f); bump.animateTo(1f, spring(dampingRatio = 0.35f)) } }
    Column(modifier.fillMaxWidth().background(color.copy(alpha = 0.08f)).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(color))
            Text("  Team $name", fontWeight = FontWeight.Bold, color = color)
            Spacer(Modifier.weight(1f))
            Text("$score", fontSize = 30.sp, fontWeight = FontWeight.Black, color = color, modifier = Modifier.graphicsLayer { scaleX = bump.value; scaleY = bump.value })
        }
        if (done) {
            val mine = if (me == 0) scoreA else scoreB
            val theirs = if (me == 0) scoreB else scoreA
            Spacer(Modifier.weight(1f))
            Text(when { mine > theirs -> "🏆 Gewonnen!"; mine < theirs -> "🥈 Knapp! Nächstes Mal!"; else -> "🤝 Unentschieden!" },
                fontSize = 30.sp, fontWeight = FontWeight.Black, color = color)
            Spacer(Modifier.weight(1f))
            return@Column
        }
        if (q == null) return@Column
        Text(q.question, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp), maxLines = 3)
        Text(when {
            winner == me -> "⚡ Du warst schneller!"
            winner != null && winner >= 0 -> "Das andere Team war schneller"
            winner == -1 -> "Richtig wäre: ${q.answer}"
            locked -> "Ups – diese Frage setzt du aus"
            else -> " "
        }, style = MaterialTheme.typography.labelLarge, color = color)
        Spacer(Modifier.height(6.dp))
        q.options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                row.forEach { o ->
                    val reveal = winner != null && o == q.answer
                    Button(
                        onClick = { onTap(o) }, enabled = !locked && winner == null,
                        modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (reveal) Color(0xFF43A047) else color,
                            disabledContainerColor = if (reveal) Color(0xFF43A047) else color.copy(alpha = 0.3f), disabledContentColor = Color.White),
                    ) { Text(o, maxLines = 2, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

/** Einstieg (in der Familien-Übersicht) */
@Composable
fun LocalDuelEntryCard(onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)), shape = MaterialTheme.shapes.extraLarge) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("👫", fontSize = 34.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Zu zweit an einem Handy", style = MaterialTheme.typography.titleMedium)
                Text("Handy flach hinlegen, jeder tippt von seiner Seite – wer ist schneller?", style = MaterialTheme.typography.bodySmall)
            }
            Text("›", fontSize = 24.sp)
        }
    }
}
