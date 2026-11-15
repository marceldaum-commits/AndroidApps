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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
    dailyDone: Boolean = false, jamBest: Int = 0, badges: Int = 0, badgeCount: Int = 0,
    onDaily: () -> Unit = {}, onJam: () -> Unit = {}, onSheet: () -> Unit = {}, onBadges: () -> Unit = {},
    controller: StageController = remember { StageController() },
) {
    val total = chapters.sumOf { c -> c.levels.sumOf { stars[it.id] ?: 0 } }; val max = chapters.sumOf { it.levels.size } * 3
    val next = chapters.flatMap { it.levels }.firstOrNull { (stars[it.id] ?: 0) == 0 }
    // Aufgeklappt ist immer nur ein Kapitel – zu Beginn das, in dem es weitergeht
    var open by rememberSaveable { mutableIntStateOf(next?.chapter ?: 1) }
    LaunchedEffect(controller) { controller.setState(Showreel.scene()); controller.camera(az = 0.75, el = 0.3, dist = 17.0) }
    // Alle paar Sekunden ein kleiner Lichtblitz – die Bühne lebt
    LaunchedEffect(controller) { while (true) { delay(6000); controller.party(1.2) } }

    LazyColumn(Modifier.fillMaxSize().background(Q.Space), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(250.dp)) {
                Stage(controller, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Q.Space.copy(alpha = 0.55f), 0.3f to Color.Transparent, 0.75f to Color.Transparent, 1f to Q.Space)))
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 18.dp, end = 6.dp, top = 6.dp), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text("RAUMLICHT", fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp, color = Q.Text)
                        Text("★ $total / $max", color = Q.Yellow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Einstellungen", tint = Q.Text) }
                }
                if (next != null) NeonButton({ onLevel(next.id) }, Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).pulse(true, 0.02f), color = Q.Green) {
                    Text(if (total == 0) "▶ Tour starten" else "▶ Weiter: ${next.id} ${next.title}", fontWeight = FontWeight.Black, maxLines = 1)
                }
            }
        }
        if (updateVersion != null) item { de.raumlicht.app.ui.settings.UpdateBanner(updateVersion, onUpdate) }
        item { ModeTiles(dailyDone, jamBest, badges, badgeCount, onDaily, onJam, onSheet, onBadges) }
        item { Text("TOUR", Modifier.padding(start = 18.dp, top = 12.dp, bottom = 2.dp), color = Q.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) }
        itemsIndexed(chapters) { _, c -> ChapterCard(c, stars, next?.id, open == c.number, { open = if (open == c.number) -1 else c.number }, onLevel) }
        item {
            Text("Analytische Geometrie der Oberstufe · keine Werbung · keine Anmeldung", Modifier.fillMaxWidth().padding(20.dp),
                color = Q.Muted, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun ChapterCard(c: Chapter, stars: Map<String, Int>, nextId: String?, expanded: Boolean, onToggle: () -> Unit, onLevel: (String) -> Unit) {
    val accent = stageColor(c.color)
    val got = c.levels.sumOf { stars[it.id] ?: 0 }; val played = c.levels.count { (stars[it.id] ?: 0) > 0 }; val done = played == c.levels.size
    val turn by animateFloatAsState(if (expanded) 90f else 0f, spring(dampingRatio = 0.6f), label = "pfeil")
    val head = remember { MutableInteractionSource() }
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp).clip(RoundedCornerShape(18.dp))
        .background(Brush.horizontalGradient(listOf(accent.copy(alpha = if (expanded) 0.2f else 0.1f), Q.Deep)))
        .border(if (expanded) 1.5.dp else 1.dp, accent.copy(alpha = if (expanded) 0.8f else 0.3f), RoundedCornerShape(18.dp))
        .animateContentSize(spring(dampingRatio = 0.75f, stiffness = 400f))) {
        Row(Modifier.fillMaxWidth().clickable(head, null, onClick = onToggle).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(c.emoji, fontSize = 22.sp)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text("${c.number} · ${c.title}", fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                Text(c.topic, color = Q.Muted, fontSize = 12.sp, maxLines = 1)
            }
            Text(if (done) "✓ " else "", color = Q.Green, fontWeight = FontWeight.Black)
            Text("★ $got", color = if (got > 0) Q.Yellow else Q.Muted, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("›", Modifier.padding(start = 10.dp).graphicsLayer { rotationZ = turn }, color = accent, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
        // Fortschrittsbalken: wie viele Gigs des Kapitels schon gespielt sind
        Box(Modifier.fillMaxWidth().height(3.dp).background(Q.Panel)) {
            Box(Modifier.fillMaxWidth(played / c.levels.size.toFloat()).fillMaxHeight().background(accent))
        }
        if (expanded) {
            Text("${c.venue} · ${c.levels.size} Gigs, der letzte ist der Abi-Check", Modifier.padding(start = 12.dp, top = 8.dp), color = accent, fontSize = 11.sp)
        Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
}
