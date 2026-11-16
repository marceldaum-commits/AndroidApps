package de.experimentopia.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.experimentopia.app.ui.blubb.Blubb
import de.experimentopia.app.ui.blubb.BlubbSays
import de.experimentopia.app.ui.blubb.Mood
import de.experimentopia.app.ui.common.*
import de.experimentopia.app.ui.fx.*
import de.experimentopia.app.ui.theme.E
import de.experimentopia.app.ui.theme.color
import de.experimentopia.core.model.*
import kotlin.math.sin

/** Kleine Insel als Bild: Sandstrand, Wellenring, Emoji in der Mitte */
@Composable
fun IslandBlob(island: Island, modifier: Modifier = Modifier, size: Int = 84) {
    val t by rememberSeconds()
    Box(modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val c = Offset(this.size.width / 2, this.size.height / 2); val r = this.size.minDimension / 2
            repeat(2) { k -> val p = (t * 0.4f + k * 0.5f) % 1f; drawCircle(E.Sky.copy(alpha = 0.35f * (1 - p)), r * (0.8f + 0.2f * p), c, style = Stroke(2f)) }
            val blob = Path().apply {
                for (i in 0..36) { val a = i / 36f * 6.283f; val rr = r * (0.72f + 0.05f * sin(a * 3 + island.ordinal) + 0.015f * sin(t * 2 + a * 5)); val p = c + Offset(kotlin.math.cos(a) * rr, kotlin.math.sin(a) * rr); if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
                close()
            }
            drawPath(blob, Color(0xFFF2D49B))
            drawCircle(Brush.radialGradient(listOf(island.color.color(), island.color.color().copy(alpha = 0.7f)), c, r * 0.6f), r * 0.58f, c)
        }
        Text(island.emoji, fontSize = (size / 2.6).sp)
    }
}

@Composable
fun HomeScreen(
    results: Map<String, Int>,
    stats: Stats,
    daily: Experiment, dailyDone: Boolean,
    grade: Int,
    welcome: Boolean,
    onIsland: (Island) -> Unit,
    onDaily: () -> Unit,
    onFreeLab: () -> Unit,
    onTopics: () -> Unit = {},
    onJournal: () -> Unit,
    onBadges: () -> Unit,
    onSettings: () -> Unit,
    onGrade: (Int) -> Unit,
    onWelcomed: () -> Unit,
    banner: (@Composable () -> Unit)? = null,
) {
    Box(Modifier.fillMaxSize()) {
        BubbleBackground()
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Blubb(Mood.HAPPY, size = 70.dp)
                BoxWithConstraints(Modifier.weight(1f).padding(start = 8.dp)) { val narrow = maxWidth < 230.dp; Column {
                    // auf schmalen Handys (320 px) kleiner, damit das Wort nicht umbricht
                    Text("Experimentopia", style = TextStyle(brush = Brush.horizontalGradient(listOf(E.Mint, E.Sky, E.Pink)), fontSize = if (narrow) 21.sp else 28.sp, fontWeight = FontWeight.Black), maxLines = 1, softWrap = false)
                    Text("Dein Versuchslabor für die Hosentasche", color = E.Muted, fontSize = 13.sp)
                } }
                IconButton(onClick = onSettings, modifier = Modifier.testTag("einstellungen")) { Text("⚙️", fontSize = 24.sp) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill("⭐ ${stats.stars}/${Catalog.all.size * 3}", E.Sun)
                Pill("🎯 ${stats.hits} Treffer", E.Mint)
                Pill("💡 ${stats.ahas} Aha!", E.Pink)
            }
            banner?.invoke()
            if (welcome) Panel(border = E.Mint) {
                BlubbSays("Hallo! Ich bin Blubb, der neugierigste Kolben der Welt. Auf sieben Inseln warten Rätsel. Du vermutest, probierst aus – und erklärst mir dann, warum. Falsch vermuten ist hier super: Das sind Aha-Momente!", Mood.CHEER, size = 56.dp)
                GlowButton(onWelcomed, Modifier.fillMaxWidth().testTag("loslegen")) { Text("🚀 Los geht's!", fontWeight = FontWeight.Bold) }
            }
            // Rätsel des Tages
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brush.horizontalGradient(listOf(E.Sun.copy(alpha = 0.28f), E.Pink.copy(alpha = 0.2f))))
                .border(2.dp, E.Sun, RoundedCornerShape(22.dp)).clickable(onClick = onDaily).padding(14.dp).testTag("tagesraetsel"), verticalAlignment = Alignment.CenterVertically) {
                Text(daily.emoji, fontSize = 40.sp, modifier = Modifier.pulse(!dailyDone, 0.06f))
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("📅 Rätsel des Tages", color = E.Sun, fontWeight = FontWeight.Black)
                    Text(daily.title, color = E.Text, fontWeight = FontWeight.Bold)
                    Text("${daily.island.emoji} ${daily.island.title} · ${daily.gradeText}", color = E.Muted, fontSize = 12.sp)
                }
                Text(if (dailyDone) "✅" else "›", fontSize = 28.sp, color = E.Sun)
            }
            // gezielt suchen: Fachbereiche (Mechanik, Newton, Strom, Säuren …) und Stichwörter
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(E.Deep).border(2.dp, E.Sky, RoundedCornerShape(20.dp))
                .clickable(onClick = onTopics).padding(14.dp).testTag("themen"), verticalAlignment = Alignment.CenterVertically) {
                Text("🔎", fontSize = 28.sp)
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text("Nach Thema suchen", color = E.Sky, fontWeight = FontWeight.Black)
                    Text(Topic.entries.joinToString(" · ") { it.emoji + " " + it.title.substringBefore(" (").substringBefore(",").substringBefore(" &") }, color = E.Muted, fontSize = 12.sp, maxLines = 2)
                }
                Text("›", fontSize = 28.sp, color = E.Sky)
            }
            Text("…oder wähle eine Insel", color = E.Text, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Island.entries.forEachIndexed { i, island ->
                val list = Catalog.of(island)
                val done = list.count { it.id in results }
                val st = list.sumOf { results[it.id] ?: 0 }
                val fits = grade == 0 || list.any { grade in it.grades }
                Row(Modifier.fillMaxWidth().padding(start = if (i % 2 == 0) 0.dp else 28.dp, end = if (i % 2 == 0) 28.dp else 0.dp)
                    .clip(RoundedCornerShape(28.dp)).background(E.Deep.copy(alpha = if (fits) 0.92f else 0.5f))
                    .border(2.dp, island.color.color().copy(alpha = if (fits) 0.8f else 0.3f), RoundedCornerShape(28.dp))
                    .clickable(onClick = { onIsland(island) }).padding(10.dp).testTag("insel_${island.name}"), verticalAlignment = Alignment.CenterVertically) {
                    IslandBlob(island, Modifier.popIn(i * 70))
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(island.title, color = E.Text, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text("${island.subtitle} · ${island.grades}", color = E.Muted, fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(E.Panel)) {
                            Box(Modifier.fillMaxWidth(done / list.size.toFloat()).fillMaxHeight().clip(CircleShape).background(island.color.color()))
                        }
                        Text("$done von ${list.size} Versuchen · ⭐ $st", color = E.Muted, fontSize = 11.sp)
                    }
                }
            }
            Text("Klasse", color = E.Muted, fontSize = 13.sp)
            ChipRow(listOf(0) + (3..10).toList(), grade, { if (it == 0) "alle" else "$it" }, onGrade)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlowButton(onFreeLab, Modifier.weight(1f).testTag("freieslabor"), color = E.Mint) { Text("🔬 Freies\nLabor", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                GlowButton(onJournal, Modifier.weight(1f).testTag("forscherbuch"), color = E.Sun) { Text("📒 Forscher-\nbuch", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                GlowButton(onBadges, Modifier.weight(1f).testTag("abzeichen"), color = E.Pink) { Text("🏅 ${Badges.earned(stats).size}/${Badges.all.size}\nAbzeichen", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun IslandScreen(island: Island, results: Map<String, Int>, grade: Int, locked: (Experiment) -> Boolean, onOpen: (Experiment) -> Unit, onBack: () -> Unit) {
    val list = Catalog.of(island)
    val t by rememberSeconds()
    Box(Modifier.fillMaxSize()) {
        BubbleBackground(tint = island.color.color())
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Bar("${island.emoji} ${island.title}", onBack) { Pill("⭐ ${list.sumOf { results[it.id] ?: 0 }}/${list.size * 3}", E.Sun) }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BlubbSays(islandIntro(island), Mood.HAPPY, size = 56.dp, speak = false)
                Spacer(Modifier.height(8.dp))
                list.forEachIndexed { i, e ->
                    if (i == 0 || list[i - 1].topic != e.topic) Text("${e.topic.emoji} ${e.topic.title}", color = island.color.color(), fontWeight = FontWeight.Black, fontSize = 17.sp,
                        modifier = Modifier.padding(top = if (i == 0) 0.dp else 12.dp))
                    val done = e.id in results
                    val lock = locked(e)
                    val fits = grade == 0 || grade in e.grades
                    val shift = (sin(i * 1.1f) * 70).dp
                    val next = !done && list.take(i).all { it.id in results }
                    Column(Modifier.fillMaxWidth().padding(start = (90.dp + shift).coerceAtLeast(0.dp)).clickable(remember { MutableInteractionSource() }, null) { onOpen(e) }.testTag("versuch_${e.id}"),
                        horizontalAlignment = Alignment.Start) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(70.dp).pulse(next && !lock, 0.05f).clip(CircleShape)
                                .background(if (done) island.color.color() else E.Deep.copy(alpha = if (fits) 1f else 0.5f))
                                .border(3.dp, if (next) E.Sun else island.color.color().copy(alpha = if (fits) 0.9f else 0.3f), CircleShape), contentAlignment = Alignment.Center) {
                                Text(if (lock) "🔒" else e.emoji, fontSize = 30.sp)
                            }
                            Column(Modifier.padding(start = 10.dp)) {
                                Text(e.title, color = if (fits) E.Text else E.Muted, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(e.gradeText, color = E.Muted, fontSize = 11.sp)
                                    if (done) Stars(results[e.id] ?: 0, 13) else if (lock) Text("Forscher-Pass", color = E.Sun, fontSize = 11.sp)
                                }
                            }
                        }
                        if (i < list.lastIndex) Text("·  ·  ·", color = island.color.color().copy(alpha = 0.6f), modifier = Modifier.padding(start = 26.dp))
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

fun islandIntro(i: Island) = when (i) {
    Island.WASSER -> "Willkommen auf der Wasserinsel! Hier geht es ums Schwimmen und Sinken – und um ein Geheimnis namens Dichte."
    Island.KRAFT -> "Auf der Kraftwerk-Insel hebst du Papas, Klaviere und Steine – mit Köpfchen statt Muskeln."
    Island.STROM -> "Achtung, Blitzinsel! Lampen, Schalter, Magnete. Hier ist alles sicher – zu Hause bitte nie an die Steckdose!"
    Island.CHEMIE -> "Im Chemie-Vulkan blubbert, zischt und knallt es. Hier darfst du alles mischen – zu Hause nur mit Erwachsenen."
    Island.WAERME -> "Feuer & Eis: Was schmilzt, was kocht, und warum bleibt das Thermometer manchmal stehen?"
    Island.LICHTKLANG -> "Auf dieser Insel mischst du Licht, zauberst Regenbögen und spielst Musik mit Wasser."
    Island.FALL -> "Vom Fallturm bis zum Mond: Was fällt schneller – und warum tickt eine Pendeluhr so gleichmäßig?"
}
