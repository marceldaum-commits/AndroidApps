package de.experimentopia.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.ui.graphics.graphicsLayer
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
    onSubject: (Subject) -> Unit = {},
    onJournal: () -> Unit,
    onBadges: () -> Unit,
    onSettings: () -> Unit,
    onGrade: (Int) -> Unit,
    onWelcomed: () -> Unit,
    banner: (@Composable () -> Unit)? = null,
    /** Forscher-Pass freigeschaltet: deutlich sichtbares Abzeichen im Kopf */
    pass: Boolean = false,
) {
    // Breite Bildschirme (PC, Tablet quer): eigene Anordnung in zwei Spalten – links Rätsel, Fächer und Werkzeuge, rechts die Inseln als Kartenraster
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 900.dp
        val islandCols = 2
        BubbleBackground(count = if (wide) 48 else 26)
        val header: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                de.experimentopia.app.ui.common.AppLogo(if (wide) 84.dp else 70.dp)
                BoxWithConstraints(Modifier.weight(1f).padding(start = 8.dp)) { val narrow = maxWidth < 230.dp; Column {
                    // auf schmalen Handys (320 px) kleiner, damit das Wort nicht umbricht
                    Text("Experimentopia", style = TextStyle(brush = Brush.horizontalGradient(listOf(E.Mint, E.Sky, E.Pink)), fontSize = if (narrow) 21.sp else if (wide) 36.sp else 28.sp, fontWeight = FontWeight.Black), maxLines = 1, softWrap = false)
                    Text(if (wide) "Dein Versuchslabor – ${Catalog.all.size} Experimente auf ${Island.entries.size} Inseln" else "Dein Versuchslabor für die Hosentasche", color = E.Muted, fontSize = if (wide) 15.sp else 13.sp)
                } }
                if (wide) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(end = 12.dp)) { pills(stats) }
                IconButton(onClick = onSettings, modifier = Modifier.testTag("einstellungen")) { Text("⚙️", fontSize = 24.sp) }
            }
        }
        val passRow: @Composable () -> Unit = {
            if (pass) Row(Modifier.fillMaxWidth().popIn().clip(RoundedCornerShape(16.dp)).background(Brush.horizontalGradient(listOf(E.Sun.copy(alpha = 0.3f), E.Orange.copy(alpha = 0.18f))))
                .border(2.dp, E.Sun, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 8.dp).testTag("pass_aktiv"), verticalAlignment = Alignment.CenterVertically) {
                Text("💛", fontSize = 22.sp, modifier = Modifier.pulse(true, 0.08f))
                Spacer(Modifier.width(8.dp))
                Text("Forscher-Pass aktiv – alle Inseln und Geräte sind offen!", color = E.Sun, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        val welcomeCard: @Composable () -> Unit = {
            if (welcome) Panel(border = E.Mint) {
                BlubbSays(de.experimentopia.core.model.Say.welcome(), Mood.CHEER, size = 56.dp)
                GlowButton(onWelcomed, Modifier.fillMaxWidth().testTag("loslegen")) { Text("🚀 Los geht's!", fontWeight = FontWeight.Bold) }
            }
        }
        val dailyCard: @Composable () -> Unit = {
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
        }
        val subjects: @Composable () -> Unit = {
            // am Schulstoff entlang: erst das Fach wählen
            Text("📚 Lernen nach Fach", color = E.Text, fontWeight = FontWeight.Black, fontSize = 20.sp)
            SubjectCards(results, onSubject)
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
        }
        val tools: @Composable () -> Unit = {
            Text("Klasse", color = E.Muted, fontSize = 13.sp)
            ChipRow(listOf(0) + (3..10).toList(), grade, { if (it == 0) "alle" else "$it" }, onGrade)
            // Auf schmalen Handys (320 dp) Bild über dem Wort, damit nichts mitten im Wort umbricht
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val narrow = maxWidth < 400.dp
                val badge = "${Badges.earned(stats).size}/${Badges.all.size}"
                @Composable fun label(emoji: String, wide: String, small: String) =
                    if (narrow) Text("$emoji\n$small", fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 17.sp)
                    else Text("$emoji $wide", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                val pad = if (narrow) 4.dp else 18.dp
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlowButton(onFreeLab, Modifier.weight(1f).fillMaxHeight().testTag("freieslabor"), color = E.Mint, padH = pad) { label("🔬", "Freies\nLabor", "Freies\nLabor") }
                    GlowButton(onJournal, Modifier.weight(1f).fillMaxHeight().testTag("forscherbuch"), color = E.Sun, padH = pad) { label("📒", "Forscher-\nbuch", "Forscher-\nbuch") }
                    GlowButton(onBadges, Modifier.weight(1f).fillMaxHeight().testTag("abzeichen"), color = E.Pink, padH = pad) { label("🏅", "$badge\nAbzeichen", "$badge\nAbzeichen") }
                }
            }
        }
        if (wide) {
            // Hintergrund über die ganze Breite, Inhalt höchstens 1600 dp breit in der Mitte
            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 1600.dp).fillMaxWidth().systemBarsPadding().padding(horizontal = 32.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                header()
                passRow()
                banner?.invoke()
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    Column(Modifier.weight(0.42f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        de.experimentopia.app.ui.common.InstallCard()
                        welcomeCard(); dailyCard(); subjects(); tools()
                        // Platz für Blubb: ein Wissens-Häppchen zum Thema des Tagesrätsels
                        if (!welcome) Panel(Modifier.fillMaxWidth(), border = E.Mint.copy(alpha = 0.5f)) {
                            BlubbSays(de.experimentopia.core.model.Say.didYouKnow(daily.topic), Mood.HAPPY, size = 64.dp, speak = false)
                            Text("💡 ${Theory.of(daily.topic).everyday}", color = E.Muted, fontSize = 13.sp)
                        }
                    }
                    Column(Modifier.weight(0.58f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("🗺️ Wähle eine Insel", color = E.Text, fontWeight = FontWeight.Black, fontSize = 22.sp)
                        Island.entries.chunked(islandCols).forEachIndexed { r, row ->
                            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                row.forEachIndexed { j, island -> IslandTile(island, results, grade, r * islandCols + j, Modifier.weight(1f).fillMaxHeight()) { onIsland(island) } }
                                repeat(islandCols - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            }
        } else Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            header()
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { pills(stats) }
            passRow()
            banner?.invoke()
            de.experimentopia.app.ui.common.InstallCard()
            welcomeCard(); dailyCard(); subjects()
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
            tools()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun pills(stats: Stats) {
    Pill("⭐ ${stats.stars}/${Catalog.all.size * 3}", E.Sun)
    Pill("🎯 ${stats.hits} Treffer", E.Mint)
    Pill("💡 ${stats.ahas} Aha!", E.Pink)
}

/** Große Inselkarte für breite Bildschirme: Insel schwebt sanft, Fortschritt, Themen der Insel; beim Darüberfahren hebt sie sich */
@Composable
private fun IslandTile(island: Island, results: Map<String, Int>, grade: Int, index: Int, modifier: Modifier, onClick: () -> Unit) {
    val list = Catalog.of(island)
    val done = list.count { it.id in results }
    val st = list.sumOf { results[it.id] ?: 0 }
    val fits = grade == 0 || list.any { grade in it.grades }
    val t by rememberSeconds()
    val hover = remember { MutableInteractionSource() }
    val hovered by hover.collectIsHoveredAsState()
    val lift by androidx.compose.animation.core.animateFloatAsState(if (hovered) 1f else 0f, label = "lift")
    val topics = list.map { it.topic }.distinct()
    Column(modifier.popIn(index * 70).graphicsLayer { translationY = -6f * density * lift; scaleX = 1f + 0.02f * lift; scaleY = 1f + 0.02f * lift }
        .clip(RoundedCornerShape(26.dp))
        .background(Brush.verticalGradient(listOf(island.color.color().copy(alpha = if (fits) 0.22f else 0.08f), E.Deep.copy(alpha = if (fits) 0.95f else 0.5f))))
        .border(2.dp, island.color.color().copy(alpha = if (fits) 0.5f + 0.5f * lift else 0.3f), RoundedCornerShape(26.dp))
        .hoverable(hover).clickable(onClick = onClick).padding(16.dp).testTag("insel_${island.name}"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IslandBlob(island, Modifier.graphicsLayer { translationY = sin(t * 1.3f + index) * 4f * density }, size = 104)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(island.title, color = E.Text, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(island.grades, color = island.color.color(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(island.subtitle, color = E.Muted, fontSize = 13.sp)
            }
        }
        Text(topics.joinToString("  ") { it.emoji + " " + it.title.substringBefore(" (").substringBefore(",") }, color = E.Text.copy(alpha = 0.85f), fontSize = 12.sp, lineHeight = 17.sp)
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(E.Panel)) {
            Box(Modifier.fillMaxWidth(done / list.size.toFloat()).fillMaxHeight().clip(CircleShape).background(island.color.color()))
        }
        Text("$done von ${list.size} Versuchen · ⭐ $st", color = E.Muted, fontSize = 12.sp)
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
            BoxWithConstraints(Modifier.fillMaxSize()) {
            // Der Pfad schlängelt sich nur so weit, dass rechts immer mindestens 160 dp für den Titel bleiben (schmale Handys ab 320 dp)
            val swing = (maxWidth - 32.dp - 70.dp - 10.dp - 160.dp).coerceAtLeast(0.dp)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BlubbSays(islandIntro(island), Mood.HAPPY, size = 56.dp, speak = false)
                Spacer(Modifier.height(8.dp))
                list.forEachIndexed { i, e ->
                    if (i == 0 || list[i - 1].topic != e.topic) Text("${e.topic.emoji} ${e.topic.title}", color = island.color.color(), fontWeight = FontWeight.Black, fontSize = 17.sp,
                        modifier = Modifier.padding(top = if (i == 0) 0.dp else 12.dp))
                    val done = e.id in results
                    val lock = locked(e)
                    val fits = grade == 0 || grade in e.grades
                    val shift = swing * ((sin(i * 1.1f) + 1f) / 2f)
                    val next = !done && list.take(i).all { it.id in results }
                    Column(Modifier.fillMaxWidth().padding(start = shift).clickable(remember { MutableInteractionSource() }, null) { onOpen(e) }.testTag("versuch_${e.id}"),
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
}

fun islandIntro(i: Island) = de.experimentopia.core.model.Say.island(i)

/** Zahl der Inseln als Wort */
fun islandCountWord() = de.experimentopia.core.model.Say.islandCountWord()
