package de.wissensgarten.app.ui.adventure

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.app.ui.common.Plant
import de.wissensgarten.app.ui.fx.*
import de.wissensgarten.core.model.Hybrid
import de.wissensgarten.core.model.Stage

fun Hybrid.species() = "kreuzung:${b.hue}"

/** Enthüllung einer neuen Kreuzungs-Blume: wächst aus dem Boden, Fanfare, Konfetti. */
@Composable
fun HybridReveal(h: Hybrid, onClose: () -> Unit) {
    val sfx = LocalSfx.current
    val particles = rememberParticles()
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        sfx?.play(Sound.GROW)
        grow.animateTo(1f, tween(1400, easing = FastOutSlowInEasing))
        sfx?.play(Sound.FANFARE)
        particles.burst(Burst.CONFETTI, count = 90)
        particles.burst(Burst.SPARKLE, 0.5f, 0.4f, 30)
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFE1F5FE), Color(0xFFF3E5F5), Color(0xFFE8F5E9)))), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text("🔀 Neue Kreuzung gezüchtet!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("${h.a.emoji} ${h.a.label}  ×  ${h.b.emoji} ${h.b.label}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
            Plant(if (grow.value < 0.5f) Stage.BUSH else Stage.BLOOM, 0f, h.a.hue, 4, Modifier.size(240.dp), grow = grow.value, species = h.species())
            Text(h.name, fontSize = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text("Wer Fächer mischt, lernt besser – das Gehirn muss jedes Mal neu umschalten. Darum blüht hier etwas ganz Besonderes.",
                textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(20.dp))
            JuicyButton(onClick = onClose, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Ins Album! 🌺", fontWeight = FontWeight.Bold) }
        }
        ParticleLayer(particles, Modifier.matchParentSize())
    }
}

/** Karte im Garten: Mischrunde starten. */
@Composable
fun MixEntryCard(a: String, b: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F7FA))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🔀", fontSize = 30.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Mischrunde: $a × $b", style = MaterialTheme.typography.titleMedium, color = Color(0xFF004D40))
                Text("Beide Fächer abwechselnd – schaffst du 75 %, züchtest du eine Kreuzungs-Blume!", style = MaterialTheme.typography.bodySmall, color = Color(0xFF004D40))
            }
        }
    }
}
