package de.raumlicht.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.testTag
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

/** Tourplan: sechs Venues, jeder Aufgabe ein Level. Nichts ist gesperrt – wer für die Klausur ein Thema braucht, springt direkt hin. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    chapters: List<Chapter>, stars: Map<String, Int>, onLevel: (String) -> Unit, onSettings: () -> Unit,
    updateVersion: String? = null, onUpdate: () -> Unit = {},
    dailyDone: Boolean = false, jamBest: Int = 0,
    onDaily: () -> Unit = {}, onJam: () -> Unit = {}, onExam: () -> Unit = {}, onMore: () -> Unit = {},
    controller: StageController = remember { StageController() },
) {
    val total = chapters.sumOf { c -> c.levels.sumOf { stars[it.id] ?: 0 } }; val max = chapters.sumOf { it.levels.size } * 3
    val next = chapters.flatMap { it.levels }.firstOrNull { (stars[it.id] ?: 0) == 0 }
    // Aufgeklappt ist immer nur ein Kapitel – zu Beginn das, in dem es weitergeht
    var open by rememberSaveable { mutableIntStateOf(next?.chapter ?: 1) }
    var area by rememberSaveable { mutableStateOf((chapters.firstOrNull { it.number == open } ?: chapters.first()).area.name) }
    LaunchedEffect(controller) { controller.setState(Showreel.scene()); controller.camera(az = 0.75, el = 0.3, dist = 17.0) }
    // Alle paar Sekunden ein kleiner Lichtblitz – die Bühne lebt
    LaunchedEffect(controller) { while (true) { delay(6000); controller.party(1.2) } }

    LazyColumn(Modifier.fillMaxSize().background(Q.Space), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(240.dp)) {
                Stage(controller, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Q.Space.copy(alpha = 0.55f), 0.3f to Color.Transparent, 0.75f to Color.Transparent, 1f to Q.Space)))
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 18.dp, end = 6.dp, top = 6.dp), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text("RAUMLICHT", fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp, color = Q.Text)
                        Text("★ $total / $max", color = Q.Yellow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    IconButton(onClick = onSettings) { Text("⚙", fontSize = 22.sp, color = Q.Text) }
                }
                if (next != null) NeonButton({ onLevel(next.id) }, Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).pulse(true, 0.02f), color = Q.Green) {
                    Text(if (total == 0) "▶ Tour starten" else "▶ Weiter: ${next.id} ${next.title}", fontWeight = FontWeight.Black, maxLines = 1)
                }
            }
        }
        if (updateVersion != null) item { de.raumlicht.app.ui.settings.UpdateBanner(updateVersion, onUpdate) }
        item { ModeTiles(dailyDone, jamBest, onDaily, onJam, onExam, onMore) }
        // Drei Gebiete wie im Abitur – so muss niemand suchen
        item {
            Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Area.entries.forEach { a ->
                    val list = chapters.filter { it.area == a }; val c = stageColor(a.color); val on = a.name == area
                    val interaction = remember { MutableInteractionSource() }
                    Column(Modifier.weight(1f).testTag("gebiet-${a.name}").pressBounce(interaction).clip(RoundedCornerShape(14.dp)).background(if (on) c.copy(alpha = 0.28f) else Q.Deep)
                        .border(if (on) 2.dp else 1.dp, if (on) c else Q.Muted.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .clickable(interaction, null) { area = a.name; open = (list.firstOrNull { ch -> ch.levels.any { it.id == next?.id } } ?: list.first()).number }.padding(vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${a.emoji} ${a.title}", fontWeight = FontWeight.Black, fontSize = 14.sp, color = if (on) Q.Text else Q.Muted)
                        Text("★ ${list.sumOf { ch -> ch.levels.sumOf { stars[it.id] ?: 0 } }} / ${list.sumOf { it.levels.size } * 3}", fontSize = 11.sp, color = if (on) Q.Yellow else Q.Muted)
                    }
                }
            }
        }
        // Alle Kapitel des Gebiets mit Namen auf einen Blick – darunter das gewählte Kapitel mit seinen Aufgaben
        item {
            FlowRow(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                chapters.filter { it.area.name == area }.forEach { c -> ChapterChip(c, stars, open == c.number, c.levels.any { it.id == next?.id }) { open = c.number } }
            }
        }
        item {
            val c = chapters.firstOrNull { it.number == open } ?: chapters.first()
            AnimatedContent(c, transitionSpec = { (slideInHorizontally { if (targetState.number > initialState.number) it / 3 else -it / 3 } + fadeIn()) togetherWith fadeOut() }, label = "kapitel") { ch ->
                ChapterDetail(ch, stars, next?.id, onLevel)
            }
        }
    }
}

/** Ein Kapitel als Schildchen: Symbol, Name, Fortschritt. */
@Composable
private fun ChapterChip(c: Chapter, stars: Map<String, Int>, selected: Boolean, current: Boolean, onClick: () -> Unit) {
    val accent = stageColor(c.color); val interaction = remember { MutableInteractionSource() }
    val played = c.levels.count { (stars[it.id] ?: 0) > 0 }; val done = played == c.levels.size
    Row(Modifier.testTag("kapitel-${c.number}").pressBounce(interaction).clip(RoundedCornerShape(12.dp)).background(if (selected) accent.copy(alpha = 0.32f) else Q.Deep)
        .border(if (selected || current) 2.dp else 1.dp, if (selected) accent else if (current) Q.Green else accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
        .clickable(interaction, null, onClick = onClick).padding(horizontal = 9.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(c.emoji, fontSize = 14.sp)
        Text(c.title, Modifier.padding(start = 5.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Q.Text, maxLines = 1)
        Text(if (done) " ✓" else if (played > 0) " $played/${c.levels.size}" else "", fontSize = 11.sp, color = if (done) Q.Green else Q.Muted, fontWeight = FontWeight.Bold)
    }
}

/** Das gewählte Kapitel mit seinen Aufgaben. */
@Composable
private fun ChapterDetail(c: Chapter, stars: Map<String, Int>, nextId: String?, onLevel: (String) -> Unit) {
    val accent = stageColor(c.color)
    val got = c.levels.sumOf { stars[it.id] ?: 0 }
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp).clip(RoundedCornerShape(20.dp))
        .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.2f), Q.Deep))).border(1.5.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(20.dp)).padding(bottom = 12.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("KAPITEL ${c.number} · ${c.levels.size} AUFGABEN", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text(c.title, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                Text(c.topic, color = Q.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            Text("★ $got/${c.levels.size * 3}", color = Q.Yellow, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
