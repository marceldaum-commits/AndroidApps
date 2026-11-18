package de.raumlicht.app.ui.level

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.raumlicht.app.ui.fx.LocalSpeaker
import de.raumlicht.app.ui.fx.NeonButton
import de.raumlicht.app.ui.lux.Lux
import de.raumlicht.app.ui.lux.LuxMood
import de.raumlicht.app.ui.settings.noted
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.core.model.TheoryCard
import kotlinx.coroutines.delay

/** Öffnet die Formelsammlung (von der Theorie-Karte aus); null = kein Knopf dafür */
val LocalOpenSheet = staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * Die passende Theorie zur Aufgabe – als Karte, die von unten hereinfährt. Kostet keinen Stern (anders als der Tipp): Wer nicht weiterweiß, soll nachlesen dürfen.
 * Aufbau: Lux erklärt die Idee, dann die Schritte (erscheinen nacheinander), ein kleines Beispiel mit anderen Zahlen und der typische Fehler.
 */
@Composable
fun TheorySheet(card: TheoryCard?, accent: Color, onClose: () -> Unit) {
    val speaker = LocalSpeaker.current; val openSheet = LocalOpenSheet.current
    // die Karte bleibt beim Herausfahren stehen, auch wenn [card] schon wieder null ist
    var shown by remember { mutableStateOf(card) }; if (card != null) shown = card
    LaunchedEffect(card?.key) { if (card != null) speaker?.say(card.idea) }
    AnimatedVisibility(card != null, enter = fadeIn(tween(160)), exit = fadeOut(tween(160))) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(remember { MutableInteractionSource() }, null, onClick = onClose))
    }
    AnimatedVisibility(card != null, enter = slideInVertically(spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)) { it } + fadeIn(), exit = slideOutVertically(tween(180)) { it } + fadeOut()) {
        val c = shown ?: return@AnimatedVisibility
        Box(Modifier.fillMaxSize().systemBarsPadding(), contentAlignment = Alignment.BottomCenter) {
            Column(Modifier.widthIn(max = 620.dp).fillMaxWidth().fillMaxHeight(0.86f).padding(horizontal = 10.dp).padding(bottom = 10.dp).testTag("theorie-karte")
                .background(Q.Deep, RoundedCornerShape(24.dp)).border(1.5.dp, accent.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                .clickable(remember { MutableInteractionSource() }, null) {}) {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(c.emoji, fontSize = 30.sp)
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text("THEORIE", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                        Text(c.title, Modifier.testTag("theorie-titel"), fontSize = 20.sp, fontWeight = FontWeight.Black, color = Q.Text)
                    }
                    Text("✕", Modifier.testTag("theorie-zu").clip(CircleShape).clickable(onClickLabel = "Theorie schließen", onClick = onClose).padding(12.dp), fontSize = 18.sp, color = Q.Muted)
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Lux(LuxMood.THINK, size = 56.dp)
                        Text(noted(c.idea), Modifier.padding(start = 8.dp).background(Q.Panel, RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)).padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                    // Die Formel zuerst und hervorgehoben – mit der Bedeutung der Größen darunter
                    c.formula?.let { f ->
                        Column(Modifier.fillMaxWidth().testTag("theorie-formel").background(accent.copy(alpha = 0.14f), RoundedCornerShape(16.dp)).border(1.5.dp, accent.copy(alpha = 0.8f), RoundedCornerShape(16.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("DIE FORMEL", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                            f.lines.forEach { Text(noted(it), color = Q.Text, fontWeight = FontWeight.Black, fontSize = 17.sp, lineHeight = 23.sp) }
                            Text(noted(f.meaning), Modifier.padding(top = 2.dp), color = Q.Muted, fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                    Block("So gehst du vor", accent) {
                        c.steps.forEachIndexed { i, s ->
                            // die Schritte erscheinen nacheinander – so liest man sie auch nacheinander
                            var on by remember(c.key) { mutableStateOf(false) }
                            LaunchedEffect(c.key) { delay(180L + i * 170L); on = true }
                            AnimatedVisibility(on, enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 3 }) {
                                Row(Modifier.padding(top = if (i == 0) 0.dp else 6.dp)) {
                                    Text("${i + 1}", Modifier.size(22.dp).background(accent.copy(alpha = 0.25f), CircleShape).wrapContentSize(), color = accent, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                    Text(noted(s), Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    Block("Beispiel", Q.Green) { c.example.forEach { Text(noted(it), Modifier.padding(top = 2.dp), style = MaterialTheme.typography.bodyMedium) } }
                    Row(Modifier.fillMaxWidth().background(Q.Orange.copy(alpha = 0.12f), RoundedCornerShape(14.dp)).border(1.dp, Q.Orange.copy(alpha = 0.5f), RoundedCornerShape(14.dp)).padding(12.dp)) {
                        Text("⚠️", fontSize = 16.sp)
                        Text("Achtung: " + noted(c.trap), Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodySmall, color = Q.Text)
                    }
                    Text("Nachlesen kostet keinen Stern.", color = Q.Muted, fontSize = 12.sp)
                }
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (openSheet != null) NeonButton({ onClose(); openSheet() }, Modifier.weight(1f).testTag("theorie-sammlung"), color = Q.Muted) { Text("📚 Formelsammlung", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    NeonButton(onClose, Modifier.weight(1f).testTag("theorie-ok"), color = accent) { Text("Alles klar!", fontWeight = FontWeight.Black) }
                }
            }
        }
    }
}

@Composable
private fun Block(title: String, color: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(Q.Space.copy(alpha = 0.6f), RoundedCornerShape(14.dp)).border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(14.dp)).padding(12.dp)) {
        Text(title, color = color, fontWeight = FontWeight.Black, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
        content()
    }
}
