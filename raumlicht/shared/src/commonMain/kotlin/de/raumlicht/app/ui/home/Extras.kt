package de.raumlicht.app.ui.home

import de.raumlicht.app.ui.fx.horizontalFade
import de.raumlicht.app.ui.fx.verticalFade
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.saveable.rememberSaveable
import de.raumlicht.app.ui.fx.BackArrow
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.raumlicht.app.ui.fx.pressBounce
import de.raumlicht.app.ui.level.stageColor
import de.raumlicht.app.ui.lux.Lux
import de.raumlicht.app.ui.lux.LuxMood
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.core.model.Badge
import de.raumlicht.core.model.JamLevel
import de.raumlicht.core.model.JamTopic
import de.raumlicht.core.model.Practice
import de.raumlicht.core.model.SheetCard
import kotlinx.coroutines.delay

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { BackArrow() }
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}

/** Endlos üben: Thema wählen, dann endlos neue Aufgaben. Eine Serie zählt Treffer beim ersten Versuch. */
@Composable
fun JamPicker(best: Int, onTopic: (JamTopic) -> Unit, onBack: () -> Unit, startLevel: String = "", onLevel: (String) -> Unit = {},
              onPick: (JamTopic, JamLevel?) -> Unit = { t, _ -> onTopic(t) },
              /** Stufen, die zum Abi-Pass gehören und noch gesperrt sind (zeigen ein Schloss) */
              lockedLevel: (JamLevel?) -> Boolean = { false }) {
    // Schwierigkeit: „Alle“ mischt wie bisher; sonst kommen nur Aufgaben der gewählten Stufe (jede nachweislich schwerer als die vorige)
    var levelName by rememberSaveable { mutableStateOf(startLevel) }
    val level = JamLevel.entries.firstOrNull { it.name == levelName }
    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding()) {
        TopBar("Endlos üben", onBack)
        Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Lux(LuxMood.HAPPY, size = 64.dp)
            Text("Endlos neue Aufgaben, jedes Mal andere Zahlen. Triffst du beim ersten Versuch, wächst deine Serie." + if (best > 0) " Dein Rekord: $best 🔥" else "",
                Modifier.padding(start = 8.dp).background(Q.Panel, RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)).padding(12.dp), style = MaterialTheme.typography.bodyMedium)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (listOf<JamLevel?>(null) + JamLevel.entries).forEach { l ->
                val on = l == level; val c = when (l) { null -> Q.Muted; JamLevel.EINSTIEG -> Q.Green; JamLevel.ABI -> Q.Yellow; JamLevel.PROFI -> Q.Pink }; val interaction = remember { MutableInteractionSource() }
                Column(Modifier.weight(1f).testTag("stufe-${l?.name?.lowercase() ?: "alle"}").pressBounce(interaction).clip(RoundedCornerShape(14.dp)).background(if (on) c.copy(alpha = 0.28f) else Q.Deep)
                    .border(if (on) 2.dp else 1.dp, if (on) c else Q.Muted.copy(alpha = 0.35f), RoundedCornerShape(14.dp)).clickable(interaction, null) { levelName = l?.name ?: ""; onLevel(levelName) }.padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(l?.emoji ?: "🎧", fontSize = 16.sp); Text((l?.title ?: "Alle") + if (lockedLevel(l)) " 🔒" else "", fontWeight = FontWeight.Black, fontSize = 13.sp, color = if (on) Q.Text else Q.Muted)
                }
            }
        }
        Text(level?.let { "${it.title}: ${it.hint}" } ?: "Alle Stufen gemischt", Modifier.padding(horizontal = 18.dp, vertical = 4.dp).testTag("stufe-text"), color = Q.Muted, style = MaterialTheme.typography.bodySmall)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(JamTopic.entries.filter { Practice.available(it, level) }, key = { _, t -> t.name }) { i, t ->
                val c = stageColor(t.color); val interaction = remember { MutableInteractionSource() }
                var shown by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { delay(60L * i); shown = true }
                val s by animateFloatAsState(if (shown) 1f else 0.6f, spring(dampingRatio = 0.5f, stiffness = 300f), label = "karte")
                Row(Modifier.fillMaxWidth().graphicsLayer { scaleX = s; scaleY = s; alpha = if (shown) 1f else 0f }.pressBounce(interaction).clip(RoundedCornerShape(20.dp))
                    .background(horizontalFade(listOf(c.copy(alpha = 0.25f), Q.Deep))).border(1.5.dp, c.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                    .clickable(interaction, null) { onPick(t, level) }.padding(16.dp).testTag("thema-${t.name}"), verticalAlignment = Alignment.CenterVertically) {
                    Text(t.emoji, fontSize = 30.sp)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(t.title, style = MaterialTheme.typography.titleMedium)
                        Text(if (t == JamTopic.MIX) "Themen bunt gemischt – wie in der Klausur" else "${Practice.count(t, level)} Aufgabenarten", color = Q.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("▶", color = c, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

/** Spickzettel: alle Formeln zum Nachschlagen – mit direktem Sprung in die passenden Aufgaben und in die Jam-Session. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SheetScreen(cards: List<SheetCard>, onChapter: (Int) -> Unit = {}, onJam: (JamTopic) -> Unit = {}, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding()) {
        TopBar("Formelsammlung", onBack)
        LazyColumn(contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(cards) { _, card ->
                val c = stageColor(card.color)
                Column(Modifier.fillMaxWidth().background(horizontalFade(listOf(c.copy(alpha = 0.14f), Q.Deep)), RoundedCornerShape(18.dp))
                    .border(1.dp, c.copy(alpha = 0.5f), RoundedCornerShape(18.dp)).padding(14.dp)) {
                    Text("${card.emoji}  ${card.title}", color = c, style = MaterialTheme.typography.titleMedium)
                    card.lines.forEach { line ->
                        Row(Modifier.padding(top = 6.dp)) {
                            Text("›", color = c, fontWeight = FontWeight.Black, modifier = Modifier.padding(end = 8.dp))
                            Text(line, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        card.chapters.forEach { n -> Jump("▶ Kapitel $n", c) { onChapter(n) } }
                        card.jam?.let { t -> Jump("♾️ Üben", Q.Pink) { onJam(t) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun Jump(text: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Text(text, Modifier.pressBounce(interaction).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.18f)).border(1.dp, color.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
        .clickable(interaction, null, onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp), color = Q.Text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
}

/** Abzeichen: verdient durch Können, nie durch bloßes Wiederkommen. */
@Composable
fun BadgesScreen(badges: List<Badge>, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding()) {
        TopBar("Erfolge", onBack)
        Text("${badges.count { it.earned }} von ${badges.size} verdient", Modifier.padding(horizontal = 18.dp), color = Q.Yellow, fontWeight = FontWeight.Bold)
        LazyVerticalGrid(GridCells.Fixed(2), contentPadding = PaddingValues(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(badges) { i, b ->
                var shown by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { delay(40L * i); shown = true }
                val s by animateFloatAsState(if (shown) 1f else 0.5f, spring(dampingRatio = 0.45f, stiffness = 300f), label = "abzeichen")
                Column(Modifier.graphicsLayer { scaleX = s; scaleY = s }.alpha(if (b.earned) 1f else 0.45f)
                    .background(if (b.earned) verticalFade(listOf(Q.Yellow.copy(alpha = 0.22f), Q.Deep)) else verticalFade(listOf(Q.Panel, Q.Deep)), RoundedCornerShape(18.dp))
                    .border(1.5.dp, if (b.earned) Q.Yellow else Q.Muted.copy(alpha = 0.4f), RoundedCornerShape(18.dp)).padding(12.dp).height(112.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (b.earned) b.emoji else "🔒", fontSize = 32.sp)
                    Text(b.title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Text(b.text, color = Q.Muted, fontSize = 11.sp, lineHeight = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/** Schmale Leiste auf der Startseite: Tagesaufgabe, Jam-Session, Abi-Simulation und der Eingang zu allem Weiteren. */
@Composable
fun ModeTiles(dailyDone: Boolean, jamBest: Int, onDaily: () -> Unit, onJam: () -> Unit, onExam: () -> Unit, onMore: () -> Unit) {
    Row(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tile("🎟️", "Heute", if (dailyDone) "geschafft ✓" else "Tagesaufgabe", Q.Orange, !dailyDone, Modifier.weight(1f), onDaily)
        Tile("♾️", "Üben", if (jamBest > 0) "Serie 🔥 $jamBest" else "endlos", Q.Pink, false, Modifier.weight(1f), onJam)
        Tile("🎓", "Abi", "Prüfung üben", Q.Yellow, false, Modifier.weight(1f), onExam)
        Tile("☰", "Mehr", "alle Bereiche", Q.Violet, false, Modifier.weight(1f), onMore)
    }
}

@Composable
private fun Tile(emoji: String, title: String, sub: String, color: androidx.compose.ui.graphics.Color, highlight: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Column(modifier.pressBounce(interaction).clip(RoundedCornerShape(16.dp)).background(verticalFade(listOf(color.copy(alpha = if (highlight) 0.32f else 0.14f), Q.Deep)))
        .border(if (highlight) 2.dp else 1.dp, color.copy(alpha = if (highlight) 1f else 0.4f), RoundedCornerShape(16.dp)).clickable(interaction, null, onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 22.sp)
        Text(title, fontWeight = FontWeight.Bold, color = Q.Text, fontSize = 12.sp, maxLines = 1)
        Text(sub, color = if (highlight) color else Q.Muted, fontSize = 10.sp, maxLines = 1)
    }
}
