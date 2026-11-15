package de.raumlicht.app.ui.home

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import de.raumlicht.core.model.JamTopic
import de.raumlicht.core.model.SheetCard
import kotlinx.coroutines.delay

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Q.Text) }
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}

/** Jam-Session: Thema wählen, dann endlos neue Aufgaben. Eine Serie zählt Treffer beim ersten Versuch. */
@Composable
fun JamPicker(best: Int, onTopic: (JamTopic) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding()) {
        TopBar("Jam-Session", onBack)
        Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Lux(LuxMood.HAPPY, size = 64.dp)
            Text("Endlos neue Aufgaben, jedes Mal andere Zahlen. Triffst du beim ersten Versuch, wächst deine Serie." + if (best > 0) " Dein Rekord: $best 🔥" else "",
                Modifier.padding(start = 8.dp).background(Q.Panel, RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)).padding(12.dp), style = MaterialTheme.typography.bodyMedium)
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(JamTopic.entries) { i, t ->
                val c = stageColor(t.color); val interaction = remember { MutableInteractionSource() }
                var shown by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { delay(60L * i); shown = true }
                val s by animateFloatAsState(if (shown) 1f else 0.6f, spring(dampingRatio = 0.5f, stiffness = 300f), label = "karte")
                Row(Modifier.fillMaxWidth().graphicsLayer { scaleX = s; scaleY = s; alpha = if (shown) 1f else 0f }.pressBounce(interaction).clip(RoundedCornerShape(20.dp))
                    .background(Brush.horizontalGradient(listOf(c.copy(alpha = 0.25f), Q.Deep))).border(1.5.dp, c.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                    .clickable(interaction, null) { onTopic(t) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(t.emoji, fontSize = 30.sp)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(t.title, style = MaterialTheme.typography.titleMedium)
                        Text(if (t == JamTopic.MIX) "Themen bunt gemischt – wie in der Klausur" else "${t.kinds.size} Aufgabenarten", color = Q.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("▶", color = c, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

/** Spickzettel: alle Formeln zum Nachschlagen. */
@Composable
fun SheetScreen(cards: List<SheetCard>, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding()) {
        TopBar("Spickzettel", onBack)
        LazyColumn(contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(cards) { _, card ->
                val c = stageColor(card.color)
                Column(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(c.copy(alpha = 0.14f), Q.Deep)), RoundedCornerShape(18.dp))
                    .border(1.dp, c.copy(alpha = 0.5f), RoundedCornerShape(18.dp)).padding(14.dp)) {
                    Text("${card.emoji}  ${card.title}", color = c, style = MaterialTheme.typography.titleMedium)
                    card.lines.forEach { line ->
                        Row(Modifier.padding(top = 6.dp)) {
                            Text("›", color = c, fontWeight = FontWeight.Black, modifier = Modifier.padding(end = 8.dp))
                            Text(line, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

/** Abzeichen: verdient durch Können, nie durch bloßes Wiederkommen. */
@Composable
fun BadgesScreen(badges: List<Badge>, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding()) {
        TopBar("Abzeichen", onBack)
        Text("${badges.count { it.earned }} von ${badges.size} verdient", Modifier.padding(horizontal = 18.dp), color = Q.Yellow, fontWeight = FontWeight.Bold)
        LazyVerticalGrid(GridCells.Fixed(2), contentPadding = PaddingValues(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(badges) { i, b ->
                var shown by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { delay(40L * i); shown = true }
                val s by animateFloatAsState(if (shown) 1f else 0.5f, spring(dampingRatio = 0.45f, stiffness = 300f), label = "abzeichen")
                Column(Modifier.graphicsLayer { scaleX = s; scaleY = s }.alpha(if (b.earned) 1f else 0.45f)
                    .background(if (b.earned) Brush.verticalGradient(listOf(Q.Yellow.copy(alpha = 0.22f), Q.Deep)) else Brush.verticalGradient(listOf(Q.Panel, Q.Deep)), RoundedCornerShape(18.dp))
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

/** Schmale Leiste auf der Startseite: Tagesgig, Jam-Session, Spickzettel, Abzeichen. */
@Composable
fun ModeTiles(dailyDone: Boolean, jamBest: Int, badges: Int, badgeCount: Int, onDaily: () -> Unit, onJam: () -> Unit, onSheet: () -> Unit, onBadges: () -> Unit) {
    Row(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tile("📅", "Tagesgig", if (dailyDone) "✓" else "neu", Q.Orange, !dailyDone, Modifier.weight(1f), onDaily)
        Tile("🎧", "Jam", if (jamBest > 0) "🔥 $jamBest" else "endlos", Q.Pink, false, Modifier.weight(1f), onJam)
        Tile("📓", "Formeln", "Spickzettel", Q.Violet, false, Modifier.weight(1f), onSheet)
        Tile("🏅", "Abzeichen", "$badges/$badgeCount", Q.Yellow, false, Modifier.weight(1f), onBadges)
    }
}

@Composable
private fun Tile(emoji: String, title: String, sub: String, color: androidx.compose.ui.graphics.Color, highlight: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Column(modifier.pressBounce(interaction).clip(RoundedCornerShape(16.dp)).background(Brush.verticalGradient(listOf(color.copy(alpha = if (highlight) 0.32f else 0.14f), Q.Deep)))
        .border(if (highlight) 2.dp else 1.dp, color.copy(alpha = if (highlight) 1f else 0.4f), RoundedCornerShape(16.dp)).clickable(interaction, null, onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 22.sp)
        Text(title, fontWeight = FontWeight.Bold, color = Q.Text, fontSize = 12.sp, maxLines = 1)
        Text(sub, color = if (highlight) color else Q.Muted, fontSize = 10.sp, maxLines = 1)
    }
}
