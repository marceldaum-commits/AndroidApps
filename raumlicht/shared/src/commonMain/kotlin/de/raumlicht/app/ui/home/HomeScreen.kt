package de.raumlicht.app.ui.home

import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import de.raumlicht.app.ui.fx.horizontalFade
import de.raumlicht.app.ui.fx.verticalFade
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
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
    /** Aufgaben, die zu Raumlicht Pro gehören und noch gesperrt sind (zeigen ein Schloss) */
    locked: (String) -> Boolean = { false },
    /** „← Alle Apps“: zurück zur App-Auswahl auf der Website (null = gibt es hier nicht) */
    onApps: (() -> Unit)? = null,
    /** Zum Bereich „Analysis & Zufall erleben“ */
    onExperience: () -> Unit = {},
) {
    val total = chapters.sumOf { c -> c.levels.sumOf { stars[it.id] ?: 0 } }; val max = chapters.sumOf { it.levels.size } * 3
    val next = chapters.flatMap { it.levels }.firstOrNull { (stars[it.id] ?: 0) == 0 }
    // Aufgeklappt ist immer nur ein Kapitel – zu Beginn das, in dem es weitergeht
    var open by rememberSaveable { mutableIntStateOf(next?.chapter ?: 1) }
    LaunchedEffect(controller) { controller.setState(Showreel.scene()); controller.camera(az = 0.75, el = 0.3, dist = 17.0) }
    // Alle paar Sekunden ein kleiner Lichtblitz – die Bühne lebt
    LaunchedEffect(controller) { while (true) { delay(6000); controller.party(1.2) } }

    // Zwei Wege (seit 2.18.32): die Tour „Geometrie im Raum“ mit ihren Kapiteln – und daneben „Analysis & Zufall erleben“ zum Ausprobieren
    val areaTabs: @Composable (Modifier) -> Unit = { m ->
        Row(m.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val c = stageColor(Area.GEOMETRIE.color)
            Column(Modifier.weight(1f).fillMaxHeight().testTag("gebiet-GEOMETRIE").clip(RoundedCornerShape(14.dp)).background(c.copy(alpha = 0.28f)).border(2.dp, c, RoundedCornerShape(14.dp)).padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                de.raumlicht.app.ui.fx.FitText("📐 Geometrie im Raum", Modifier.padding(horizontal = 4.dp), fontWeight = FontWeight.Black, fontSize = 14.sp, min = 10f, color = Q.Text)
                Text("★ $total / $max", fontSize = 11.sp, color = Q.Yellow)
            }
            val interaction = remember { MutableInteractionSource() }
            Column(Modifier.weight(1f).fillMaxHeight().testTag("erleben").pressBounce(interaction).clip(RoundedCornerShape(14.dp)).background(verticalFade(listOf(Q.Pink.copy(alpha = 0.22f), Q.Deep)))
                .border(1.dp, Q.Pink.copy(alpha = 0.7f), RoundedCornerShape(14.dp)).clickable(interaction, null, onClick = onExperience).padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                de.raumlicht.app.ui.fx.FitText("🎢 Analysis & Zufall", Modifier.padding(horizontal = 4.dp), fontWeight = FontWeight.Black, fontSize = 14.sp, min = 10f, color = Q.Text)
                Text("erleben ›", fontSize = 11.sp, color = Q.Pink, fontWeight = FontWeight.Bold)
            }
        }
    }
    val chapter: @Composable (Boolean) -> Unit = { wide ->
        val c = chapters.firstOrNull { it.number == open } ?: chapters.first()
        AnimatedContent(c, transitionSpec = { (slideInHorizontally { if (targetState.number > initialState.number) it / 3 else -it / 3 } + fadeIn()) togetherWith fadeOut() }, label = "kapitel") { ch ->
            ChapterDetail(ch, stars, next?.id, onLevel, locked, wide)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Q.Space)) {
    // Breiter Bildschirm (PC, Tablet quer): nicht strecken, sondern aufteilen – links die Bühne groß, rechts Gebiete, Kapitel und Aufgaben ohne Wischen
    val full = maxWidth; val side = (full * 0.4f).coerceIn(440.dp, 640.dp)
    if (full >= 900.dp && full > maxHeight * 1.2f) Row(Modifier.fillMaxSize().testTag("start-breit")) {
        Box(Modifier.weight(1f).fillMaxHeight()) {
            Stage(controller, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(verticalFade(0f to Q.Space.copy(alpha = 0.6f), 0.25f to Color.Transparent, 0.6f to Color.Transparent, 1f to Q.Space.copy(alpha = 0.92f))))
            Box(Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(90.dp).background(horizontalFade(listOf(Color.Transparent, Q.Space))))
            Column(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = 32.dp, top = 22.dp)) {
                Text("RAUMLICHT", fontSize = if (full < 1200.dp) 32.sp else 42.sp, fontWeight = FontWeight.Black, letterSpacing = if (full < 1200.dp) 5.sp else 7.sp, color = Q.Text)
                Text("Mathe im Raum – Oberstufe und Abitur", color = Q.Muted, fontSize = 15.sp)
                Text("★ $total / $max", Modifier.padding(top = 8.dp), color = Q.Yellow, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                de.raumlicht.app.ui.settings.ProBadge(Modifier.padding(top = 8.dp))
            }
            Column(Modifier.align(Alignment.BottomCenter).widthIn(max = 680.dp).fillMaxWidth().padding(horizontal = 18.dp, vertical = 22.dp)) {
                if (next != null) NeonButton({ onLevel(next.id) }, Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp).pulse(true, 0.02f), color = Q.Green) {
                    Text(if (total == 0) "▶ Tour starten" else "▶ Weiter: ${Levels.label(next.id)} ${next.title}", fontWeight = FontWeight.Black, fontSize = 18.sp, maxLines = 1)
                }
                ModeTiles(dailyDone, jamBest, onDaily, onJam, onExam, onMore)
            }
        }
        Column(Modifier.width(side).fillMaxHeight().verticalScroll(rememberScrollState()).statusBarsPadding().padding(bottom = 24.dp)) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("TOURPLAN", Modifier.weight(1f), color = Q.Muted, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                if (onApps != null) AppsButton(onApps)
                IconButton(onClick = onSettings) { Text("⚙", Modifier.clearAndSetSemantics { contentDescription = "Einstellungen" }, fontSize = 24.sp, color = Q.Text) }
            }
            if (updateVersion != null) de.raumlicht.app.ui.settings.UpdateBanner(updateVersion, onUpdate)
            areaTabs(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 10.dp))
            val list = chapters
            Text("${list.size} Kapitel", Modifier.padding(start = 16.dp, bottom = 6.dp).testTag("kapitel-anzahl"), color = Q.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            FlowRow(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                list.forEach { c -> ChapterChip(c, stars, open == c.number, c.levels.any { it.id == next?.id }) { open = c.number } }
            }
            chapter(true)
        }
    } else
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(240.dp)) {
                Stage(controller, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(verticalFade(0f to Q.Space.copy(alpha = 0.55f), 0.3f to Color.Transparent, 0.75f to Color.Transparent, 1f to Q.Space)))
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 18.dp, end = 6.dp, top = 6.dp), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        // schrumpft auf schmalen Handys, damit daneben „← Alle Apps“ und das Zahnrad Platz haben
                        de.raumlicht.app.ui.fx.FitText("RAUMLICHT", fontSize = 28.sp, min = 17f, fontWeight = FontWeight.Black, color = Q.Text, style = LocalTextStyle.current.copy(letterSpacing = 4.sp))
                        Text("★ $total / $max", color = Q.Yellow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        de.raumlicht.app.ui.settings.ProBadge(Modifier.padding(top = 6.dp))
                    }
                    if (onApps != null) AppsButton(onApps)
                    IconButton(onClick = onSettings) { Text("⚙", Modifier.clearAndSetSemantics { contentDescription = "Einstellungen" }, fontSize = 22.sp, color = Q.Text) }
                }
                if (next != null) NeonButton({ onLevel(next.id) }, Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).pulse(true, 0.02f), color = Q.Green) {
                    Text(if (total == 0) "▶ Tour starten" else "▶ Weiter: ${Levels.label(next.id)} ${next.title}", fontWeight = FontWeight.Black, maxLines = 1)
                }
            }
        }
        if (updateVersion != null) item { de.raumlicht.app.ui.settings.UpdateBanner(updateVersion, onUpdate) }
        item { ModeTiles(dailyDone, jamBest, onDaily, onJam, onExam, onMore) }
        item { areaTabs(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 6.dp)) }
        // Alle Kapitel des Gebiets mit Namen auf einen Blick – darunter das gewählte Kapitel mit seinen Aufgaben
        // Aufgeräumt (seit 2.9.2): eine einzige Zeile zum Wischen statt einer Wand aus Knöpfen. Das gewählte Kapitel rutscht von selbst ins Bild.
        item {
            val list = chapters
            val scroll = rememberScrollState(); val places = remember { HashMap<Int, Int>() }
            LaunchedEffect(open) { withFrameNanos { }; places[open]?.let { x -> scroll.animateScrollTo((x - 60).coerceAtLeast(0)) } }
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${list.size} Kapitel", Modifier.weight(1f).testTag("kapitel-anzahl"), color = Q.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                if (scroll.maxValue > 0) Text(if (scroll.value < scroll.maxValue) "wischen ›" else "‹ wischen", color = Q.Muted, fontSize = 11.sp)
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(scroll).padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                list.forEach { c -> Box(Modifier.onGloballyPositioned { places[c.number] = it.positionInParent().x.toInt() }) {
                    ChapterChip(c, stars, open == c.number, c.levels.any { it.id == next?.id }) { open = c.number } } }
            }
        }
        item { chapter(false) }
    }
    }
}

/** „← Alle Apps“: kleiner, gut sichtbarer Weg zurück zur App-Auswahl auf der Website – ohne Preis, ohne Kaufhinweis; Tippfläche mindestens 44 dp hoch */
@Composable
private fun AppsButton(onClick: () -> Unit) {
    Box(Modifier.testTag("alle-apps").heightIn(min = 44.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
        Text(Nachhilfe.APPS_LABEL, Modifier.clip(RoundedCornerShape(12.dp)).background(Q.Deep.copy(alpha = 0.85f)).border(1.dp, Q.Text.copy(alpha = 0.55f), RoundedCornerShape(12.dp)).padding(horizontal = 11.dp, vertical = 7.dp),
            color = Q.Text, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
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
private fun ChapterDetail(c: Chapter, stars: Map<String, Int>, nextId: String?, onLevel: (String) -> Unit, locked: (String) -> Boolean = { false }, wide: Boolean = false) {
    val accent = stageColor(c.color)
    val got = c.levels.sumOf { stars[it.id] ?: 0 }
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp).clip(RoundedCornerShape(20.dp))
        .background(horizontalFade(listOf(accent.copy(alpha = 0.2f), Q.Deep))).border(1.5.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(20.dp)).padding(bottom = 12.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("KAPITEL ${Levels.shown(c.number)} · ${c.levels.size} AUFGABEN", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text(c.title, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                Text(c.topic, color = Q.Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            Text("★ $got/${c.levels.size * 3}", color = Q.Yellow, fontWeight = FontWeight.Bold)
        }
        // Breiter Bildschirm: jede Aufgabe als Zeile mit ihrem Namen – Platz ist da, und man sieht, worum es geht
        if (wide) Column(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            c.levels.forEach { l ->
                val s = stars[l.id] ?: 0
                val interaction = remember { MutableInteractionSource() }
                Row(Modifier.pulse(l.id == nextId, 0.015f).pressBounce(interaction).fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(if (s > 0) accent.copy(alpha = 0.22f) else Q.Panel)
                    .border(if (l.id == nextId) 2.dp else 1.dp, if (l.id == nextId) Q.Green else accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .clickable(interaction, null) { onLevel(l.id) }.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (l.abi) "🎓" else l.id.substringAfter('-'), Modifier.width(30.dp), fontWeight = FontWeight.Black, fontSize = 18.sp, color = Q.Text)
                    Text(l.title, Modifier.weight(1f).padding(end = 8.dp), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Q.Text, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    if (locked(l.id)) Text("🔒", Modifier.testTag("schloss-${l.id}"), fontSize = 13.sp) else StarRow(s, lit = true, size = 16)
                }
            }
        } else
        Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            c.levels.forEach { l ->
                val s = stars[l.id] ?: 0
                val interaction = remember { MutableInteractionSource() }
                Column(Modifier.pulse(l.id == nextId, 0.05f).pressBounce(interaction).weight(1f).clip(RoundedCornerShape(14.dp))
                    .background(if (s > 0) accent.copy(alpha = 0.22f) else Q.Panel)
                    .border(if (l.id == nextId) 2.dp else 1.dp, if (l.id == nextId) Q.Green else accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .clickable(interaction, null) { onLevel(l.id) }.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (l.abi) "🎓" else l.id.substringAfter('-'), fontWeight = FontWeight.Black, fontSize = 17.sp, color = Q.Text)
                    if (locked(l.id)) Text("🔒", Modifier.testTag("schloss-${l.id}"), fontSize = 11.sp) else Box(Modifier.graphicsLayer { scaleX = 0.62f; scaleY = 0.62f }) { StarRow(s, lit = true, size = 16) }
                }
            }
        }
    }
}
