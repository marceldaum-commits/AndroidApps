package de.kurvenflug.app.ui.story

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import de.kurvenflug.app.ui.fx.*
import de.kurvenflug.app.ui.theme.Q
import de.kurvenflug.app.ui.volt.Volt
import de.kurvenflug.app.ui.volt.VoltMood
import de.kurvenflug.core.model.*
import kotlin.math.sin

/** Farben der Nova: violett-rosa-türkis, anders als die Aurora */
val NovaColors = listOf(Color(0xFFB18CFF), Color(0xFFFF6FD8), Color(0xFF3DE8FF), Color(0xFF8C9EFF), Color(0xFFE040FB), Color(0xFF64FFDA))

/** Karte auf der Startseite: Weiter in Andromeda */
@Composable
fun AndromedaCard(planet: Int, nova: Int, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val sfx = LocalSfx.current
    Row(Modifier.fillMaxWidth().pressBounce(interaction).clip(RoundedCornerShape(20.dp))
        .background(Brush.horizontalGradient(listOf(Q.Violet.copy(alpha = 0.35f), Q.Panel))).border(1.5.dp, Q.Violet, RoundedCornerShape(20.dp))
        .clickable(interaction, null) { sfx?.play(Sound.TAP); onClick() }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("🌌", fontSize = 32.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Andromeda · Planet $planet", fontWeight = FontWeight.Black, color = Q.Text)
            Text(if (nova < Andromeda.PARTS) "Nova: $nova von ${Andromeda.PARTS} Teilen" else "Nova fertig – die Reise geht weiter!", style = MaterialTheme.typography.bodySmall, color = Q.Muted)
        }
        Text("›", color = Q.Violet, fontWeight = FontWeight.Black, fontSize = 22.sp)
    }
}

/**
 * Andromeda: endloser Pfad aus erzeugten Planeten. Geschaffte leuchten, der aktuelle pulsiert, der nächste ist
 * noch verhüllt. Oben wächst die Nova Teil für Teil.
 */
@Composable
fun AndromedaScreen(stars: Map<String, Int>, onLevel: (Level) -> Unit, onBack: () -> Unit) {
    val done = Andromeda.completed(stars)
    val current = done + 1
    val nova = Andromeda.novaParts(stars)
    var open by rememberSaveable { mutableStateOf<Int?>(null) }
    val sfx = LocalSfx.current
    val time by rememberInfiniteTransition(label = "andro").animateFloat(0f, 1000f, infiniteRepeatable(tween(1_000_000, easing = LinearEasing)), label = "t")
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1B0F3A), Q.Space)))) {
        Starfield(seed = 97)
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Q.Text) }
                Text("🌌 Andromeda", style = MaterialTheme.typography.titleLarge, color = Q.Text)
            }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Volt(if (nova >= Andromeda.PARTS) VoltMood.DANCE else VoltMood.HAPPY, size = 52.dp, speech = de.kurvenflug.core.model.VoltLines.andromeda(done, nova))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0x55070B20)).border(1.5.dp, Q.Violet.copy(alpha = 0.5f), RoundedCornerShape(20.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(110.dp)) {
                        ShipCanvas(Story.parts.take(nova).map { it.chapter }.toSet(), Modifier.fillMaxSize(), time, flames = nova >= Andromeda.PARTS, palette = NovaColors)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Die ${Andromeda.SHIP}", fontWeight = FontWeight.Black, color = Q.Text, fontSize = 18.sp)
                        Text("$nova von ${Andromeda.PARTS} Teilen", color = Q.Violet)
                        Text("Planeten geschafft: $done", color = Q.Muted, style = MaterialTheme.typography.labelSmall)
                    }
                }
                // Pfad: neueste oben – der aktuelle Planet zuerst, darunter die geschafften
                val shown = (current + 1 downTo 1).toList()
                shown.forEachIndexed { i, n ->
                    val p = Andromeda.planet(n)
                    val isDone = n <= done; val isCurrent = n == current; val veiled = n > current
                    val pulse = if (isCurrent) 1f + 0.05f * sin(time * 4f) else 1f
                    Row(Modifier.fillMaxWidth().padding(start = (24 + 60 * (0.5f + 0.5f * sin(n * 1.3f))).dp).clip(RoundedCornerShape(18.dp))
                        .background(if (isCurrent) Q.Violet.copy(alpha = 0.25f) else Q.Panel.copy(alpha = 0.8f))
                        .border(1.5.dp, if (isCurrent) Q.Pink else if (isDone) Q.Green.copy(alpha = 0.5f) else Q.Muted.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
                        .clickable(enabled = !veiled) { sfx?.play(Sound.TAP); open = n }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                            Canvas(Modifier.fillMaxSize()) {
                                val c = NovaColors[n % NovaColors.size]
                                drawCircle(Brush.radialGradient(listOf(c, c.copy(alpha = 0.3f), Color.Transparent)), size.minDimension / 2 * pulse)
                                if (isDone) drawCircle(Q.Green, size.minDimension / 2 - 3f, style = Stroke(4f))
                            }
                            Text(if (veiled) "❔" else p.emoji, fontSize = 24.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (veiled) "Unbekannter Planet" else p.name, fontWeight = FontWeight.Bold, color = if (veiled) Q.Muted else Q.Text)
                            Text(when { isDone -> "geschafft ✅" + if (n <= Andromeda.PARTS) " · Nova-Teil $n" else ""; isCurrent -> "Hier geht's weiter – 3 Etappen"; else -> "Erst den Planeten davor schaffen" },
                                style = MaterialTheme.typography.labelSmall, color = Q.Muted)
                        }
                        if (isCurrent) Text("🚀", fontSize = 22.sp, modifier = Modifier.padding(end = 4.dp))
                    }
                }
            }
        }
    }
    open?.let { n ->
        val c = Andromeda.chapter(n)
        Dialog(onDismissRequest = { open = null }) {
            Column(Modifier.clip(RoundedCornerShape(24.dp)).background(Q.Space).border(2.dp, Q.Violet, RoundedCornerShape(24.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                de.kurvenflug.app.ui.flight.ChapterCard(c, 4, stars, { open = null; onLevel(it) })
                Text("Schwierigkeit: hoch – mehr Asteroiden, krumme Zahlen.", color = Q.Muted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                TextButton(onClick = { open = null }, modifier = Modifier.align(Alignment.End)) { Text("Schließen", color = Q.Muted) }
            }
        }
    }
}
