package de.wissensgarten.app.ui.play

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.app.ui.fx.*
import de.wissensgarten.core.model.Syllables
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Wörter, die sich zum Silben-Klatschen eignen (deutsch, ein Wort, 2–4 Silben). */
fun clapWords(answers: List<String>): List<String> = answers.map { it.trim() }
    .filter { w -> w.length in 3..14 && w.all { it.isLetter() } && Syllables.count(w) in 2..4 }
    .distinct().take(3)

/**
 * Silben-Klatschen (Silbenmethode): Brummi spricht das Wort in Silben vor, die Silben leuchten im Takt auf.
 * Dann klatscht das Kind: jeder Tipp auf die Hände lässt die nächste Silbe aufploppen.
 * Zu viel geklatscht? Kein Problem – einfach nochmal. Belohnung: Tautropfen.
 */
@Composable
fun ClapBonus(words: List<String>, onDone: (mistakes: Int) -> Unit) {
    val sfx = LocalSfx.current
    val voice = LocalVoice.current
    val scope = rememberCoroutineScope()
    val particles = rememberParticles()
    var w by remember { mutableIntStateOf(0) }
    var mistakes by remember { mutableIntStateOf(0) }
    val word = words[w]
    val parts = remember(word) { Syllables.split(word) }
    var lit by remember(word) { mutableIntStateOf(0) }
    var demo by remember(word) { mutableStateOf(true) }
    var hint by remember(word) { mutableStateOf("Hör gut zu …") }
    val handScale = remember { Animatable(1f) }
    val pops = remember(word) { parts.map { Animatable(1f) } }

    // Vormachen: Silbe für Silbe sprechen und leuchten lassen
    LaunchedEffect(word) {
        delay(400)
        voice?.speak(parts.joinToString(" – "))
        for (i in parts.indices) {
            lit = i + 1
            sfx?.play(Sound.CLAP, volume = 0.6f)
            launch { pops[i].snapTo(1.4f); pops[i].animateTo(1f, spring(dampingRatio = 0.35f)) }
            delay(520)
        }
        delay(500)
        lit = 0; demo = false; hint = "Jetzt du: Klatsch jede Silbe! 👏"
    }

    fun clap() {
        if (demo) return
        sfx?.play(Sound.CLAP)
        scope.launch { handScale.snapTo(0.8f); handScale.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = 700f)) }
        if (lit >= parts.size) {
            // zu viel geklatscht – freundlich nochmal
            mistakes++; lit = 0; hint = "Ups, eine zu viel! Nochmal von vorn 🙂"; sfx?.play(Sound.PLOP, pitch = 0.7f)
            return
        }
        val i = lit
        lit++
        scope.launch { pops[i].snapTo(1.5f); pops[i].animateTo(1f, spring(dampingRatio = 0.35f)) }
        if (lit == parts.size) scope.launch {
            delay(700)
            if (lit != parts.size) return@launch
            sfx?.play(Sound.CORRECT)
            particles.burst(Burst.SPARKLE, count = 30)
            hint = "${parts.size} Silben – super! 🎉"
            voice?.speak(word)
            delay(1100)
            if (w + 1 < words.size) w++ else onDone(mistakes)
        }
    }

    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(color = Color(0xFFE3F2FD), shape = CircleShape) {
                Text("👏 Silben-Klatschen  ${w + 1}/${words.size}", fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
            }
            Spacer(Modifier.height(18.dp))
            // Silben als Kärtchen: dunkel bis geklatscht, dann blau/rot
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                parts.forEachIndexed { i, p ->
                    val on = i < lit
                    Box(Modifier.graphicsLayer { scaleX = pops[i].value; scaleY = pops[i].value }
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (!on) Color(0xFFE8E4D8) else if (i % 2 == 0) SILBE_BLAU else SILBE_ROT)
                        .padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(p, fontSize = 30.sp, fontWeight = FontWeight.Black, color = if (on) Color.White else Color(0xFF9E9A8C))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(hint, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier.size(150.dp).graphicsLayer { scaleX = handScale.value; scaleY = handScale.value }
                    .clip(CircleShape).background(if (demo) Color(0xFFF1EDE0) else Color(0xFFFFE082))
                    .clickable(MutableInteractionSource(), null) { clap() },
                contentAlignment = Alignment.Center,
            ) { Text("👏", fontSize = 76.sp) }
        }
        ParticleLayer(particles, Modifier.matchParentSize())
    }
}
