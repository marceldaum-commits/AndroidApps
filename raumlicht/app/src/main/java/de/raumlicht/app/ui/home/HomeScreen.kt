package de.raumlicht.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import de.raumlicht.app.ui.fx.NeonButton
import de.raumlicht.app.ui.fx.pressBounce
import de.raumlicht.app.ui.fx.pulse
import de.raumlicht.app.ui.level.StarRow
import de.raumlicht.app.ui.level.stageColor
import de.raumlicht.app.ui.stage.Stage
import de.raumlicht.app.ui.stage.StageController
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.core.model.*
import kotlinx.coroutines.delay

/** Tourplan: sechs Venues, jeder Gig ein Level. Nichts ist gesperrt – wer für die Klausur ein Thema braucht, springt direkt hin. */
@Composable
fun HomeScreen(
    chapters: List<Chapter>, stars: Map<String, Int>, onLevel: (String) -> Unit, onSettings: () -> Unit,
    updateVersion: String? = null, onUpdate: () -> Unit = {},
    controller: StageController = remember { StageController() },
) {
    val total = stars.values.sum(); val max = chapters.sumOf { it.levels.size } * 3
    val next = chapters.flatMap { it.levels }.firstOrNull { (stars[it.id] ?: 0) == 0 }
    LaunchedEffect(controller) { controller.setState(Showreel.scene()); controller.camera(az = 0.75, el = 0.3, dist = 17.0) }
    // Alle paar Sekunden ein kleiner Lichtblitz – die Bühne lebt
    LaunchedEffect(controller) { while (true) { delay(6000); controller.party(1.2) } }

    LazyColumn(Modifier.fillMaxSize().background(Q.Space), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(330.dp)) {
                Stage(controller, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Q.Space.copy(alpha = 0.55f), 0.3f to Color.Transparent, 0.75f to Color.Transparent, 1f to Q.Space)))
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 18.dp, end = 6.dp, top = 6.dp), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text("RAUMLICHT", fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp, color = Q.Text)
                        Text("Vektoren. Laser. Deine Show.", color = Q.Cyan, fontWeight = FontWeight.Medium)
                    }
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Einstellungen", tint = Q.Text) }
                }
                Row(Modifier.align(Alignment.BottomStart).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("★ $total", color = Q.Yellow, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Text(" / $max", color = Q.Muted, fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    if (next != null) NeonButton({ onLevel(next.id) }, Modifier.pulse(true, 0.03f), color = Q.Green) {
                        Text(if (total == 0) "▶ Tour starten" else "▶ Weiter: ${next.title}", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        if (updateVersion != null) item { de.raumlicht.app.ui.settings.UpdateBanner(updateVersion, onUpdate) }
        itemsIndexed(chapters) { _, c -> ChapterCard(c, stars, next?.id, onLevel) }
        item {
            Text("Analytische Geometrie der Oberstufe · keine Werbung · keine Anmeldung", Modifier.fillMaxWidth().padding(20.dp),
                color = Q.Muted, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun ChapterCard(c: Chapter, stars: Map<String, Int>, nextId: String?, onLevel: (String) -> Unit) {
    val accent = stageColor(c.color)
    val got = c.levels.sumOf { stars[it.id] ?: 0 }; val done = c.levels.all { (stars[it.id] ?: 0) > 0 }
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp)
        .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.16f), Q.Deep)), RoundedCornerShape(22.dp))
        .border(1.5.dp, accent.copy(alpha = if (done) 0.9f else 0.4f), RoundedCornerShape(22.dp)).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c.emoji, fontSize = 30.sp)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text("${c.number} · ${c.venue}".uppercase(), color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text(c.title, style = MaterialTheme.typography.titleLarge)
                Text(c.topic, color = Q.Muted, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("★ $got/${c.levels.size * 3}", color = Q.Yellow, fontWeight = FontWeight.Bold)
                if (done) Text("gespielt ✓", color = Q.Green, fontSize = 11.sp)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            c.levels.forEach { l ->
                val s = stars[l.id] ?: 0
                val interaction = remember { MutableInteractionSource() }
                Column(Modifier.pulse(l.id == nextId, 0.05f).pressBounce(interaction).weight(1f).clip(RoundedCornerShape(14.dp))
                    .background(if (s > 0) accent.copy(alpha = 0.22f) else Q.Panel)
                    .border(if (l.id == nextId) 2.dp else 1.dp, if (l.id == nextId) Q.Green else accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .clickable(interaction, null) { onLevel(l.id) }.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (l.abi) "🎓" else l.id.substringAfter('-'), fontWeight = FontWeight.Black, fontSize = 17.sp, color = Q.Text)
                    Box(Modifier.graphicsLayer { scaleX = 0.62f; scaleY = 0.62f }) { StarRow(s, lit = true, size = 16) }
                }
            }
        }
    }
}
