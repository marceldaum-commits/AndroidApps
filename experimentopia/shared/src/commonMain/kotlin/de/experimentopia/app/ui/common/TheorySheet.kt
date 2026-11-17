package de.experimentopia.app.ui.common

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.experimentopia.app.ui.blubb.BlubbSays
import de.experimentopia.app.ui.blubb.Mood
import de.experimentopia.app.ui.fx.*
import de.experimentopia.app.ui.theme.E
import de.experimentopia.core.model.*

/** Kleiner Knopf „📖 Theorie“ – überall dort, wo Kinder nachlesen wollen */
@Composable
fun TheoryButton(topic: Topic, onClick: () -> Unit, modifier: Modifier = Modifier, long: Boolean = true) {
    val sfx = LocalSfx.current
    val shape = if (long) androidx.compose.foundation.shape.RoundedCornerShape(18.dp) else CircleShape
    Row(modifier.clip(shape).background(E.Sky.copy(alpha = 0.16f)).border(1.5.dp, E.Sky, shape)
        .clickable { sfx?.play(Sound.WHOOSH, volume = 0.5f); onClick() }.padding(horizontal = 12.dp, vertical = 7.dp).testTag("theorie"),
        verticalAlignment = Alignment.CenterVertically) {
        Text("📖", fontSize = 16.sp, modifier = Modifier.semantics { contentDescription = "Theorie: ${topic.title}" })
        if (long) {
            Spacer(Modifier.width(6.dp))
            Text("Theorie: ${topic.title}", color = E.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 3)
        }
    }
}

/**
 * Theorie-Seite zu einem Fachbereich. Gleitet von unten herein, liegt über dem Versuch und verrät keine Lösung.
 * [pro] = Profi-Teil (ab Klasse 7) gleich aufgeklappt.
 */
@Composable
fun TheorySheet(visible: Boolean, topic: Topic, pro: Boolean, onClose: () -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
        Box(Modifier.fillMaxSize().background(Color(0xCC0B0820)).clickable(remember { MutableInteractionSource() }, null) { onClose() })
    }
    AnimatedVisibility(visible, enter = slideInVertically(tween(320)) { it } + fadeIn(), exit = slideOutVertically(tween(260)) { it } + fadeOut()) {
        val card = Theory.of(topic)
        var showPro by remember(topic) { mutableStateOf(pro) }
        Box(Modifier.fillMaxSize().systemBarsPadding().padding(top = 36.dp), contentAlignment = Alignment.BottomCenter) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(E.Deep)
                .border(2.dp, E.Sky, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .clickable(remember { MutableInteractionSource() }, null) {}
                .verticalScroll(rememberScrollState()).padding(16.dp).testTag("theorieseite"),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.align(Alignment.CenterHorizontally).size(44.dp, 5.dp).clip(CircleShape).background(E.Line))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(topic.emoji, fontSize = 30.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Theorie", color = E.Sky, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(topic.title, color = E.Text, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                }
                BlubbSays(card.idea, Mood.HAPPY, size = 52.dp, speak = false)
                ReadButton(ReadTexts.theory(topic, showPro))
                card.basics.forEachIndexed { i, pt -> Point(pt, i) }
                Panel(border = E.Sun) {
                    Text("🏠 Im Alltag", color = E.Sun, fontWeight = FontWeight.Bold)
                    Text(card.everyday, color = E.Text, fontSize = 14.sp)
                }
                // Profi-Teil: Formeln und Fachwörter
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(E.Violet.copy(alpha = 0.14f)).clickable { showPro = !showPro }
                    .padding(horizontal = 12.dp, vertical = 10.dp).testTag("profi"), verticalAlignment = Alignment.CenterVertically) {
                    Text("🎓 Für Profis (ab Klasse 7)", color = E.Violet, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(if (showPro) "▲" else "▼", color = E.Violet)
                }
                AnimatedVisibility(showPro) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { card.pro.forEachIndexed { i, pt -> Point(pt, i) } }
                }
                Panel {
                    Text("📚 Wörter", color = E.Mint, fontWeight = FontWeight.Bold)
                    card.words.forEach { (w, m) -> Text("$w – $m", color = E.Text, fontSize = 13.sp) }
                }
                GlowButton(onClose, Modifier.fillMaxWidth().testTag("theorie_zu"), color = E.Mint) { Text("✅ Alles klar – weiter forschen!", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun Point(p: TheoryPoint, i: Int) {
    Row(Modifier.fillMaxWidth().popIn(i * 70).clip(RoundedCornerShape(14.dp)).background(E.Panel).padding(10.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(E.Night), contentAlignment = Alignment.Center) { Text(p.emoji, fontSize = 22.sp) }
        Spacer(Modifier.width(10.dp))
        Text(p.text, color = E.Text, fontSize = 14.sp, modifier = Modifier.weight(1f))
    }
}
