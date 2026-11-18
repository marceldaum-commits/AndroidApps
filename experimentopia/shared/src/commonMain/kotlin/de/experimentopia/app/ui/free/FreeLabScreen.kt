package de.experimentopia.app.ui.free

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.experimentopia.app.ui.blubb.BlubbSays
import de.experimentopia.app.ui.blubb.Mood
import de.experimentopia.app.ui.common.*
import de.experimentopia.app.ui.fx.BubbleBackground
import de.experimentopia.app.ui.fx.popIn
import de.experimentopia.app.ui.lab.LabView
import de.experimentopia.app.ui.theme.E
import de.experimentopia.app.ui.theme.color
import de.experimentopia.core.model.FreeLab
import de.experimentopia.core.model.Island
import de.experimentopia.core.model.Lab

/** Alle Geräte zum freien Forschen – mit eigenen Werten */
@Composable
fun FreeLabScreen(locked: (Lab) -> Boolean, onOpen: (Lab) -> Unit, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        BubbleBackground()
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Bar("🔬 Freies Labor", onBack)
            BoxWithConstraints(Modifier.fillMaxSize()) {
            val cols = if (maxWidth >= 1000.dp) 4 else if (maxWidth >= 680.dp) 3 else 2
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BlubbSays(de.experimentopia.core.model.Say.FREE_LAB, Mood.HAPPY, size = 56.dp, speak = false)
                // Breit: zwei Spalten wie ein Mauerwerk – die Inseln werden nach Gerätezahl ausgeglichen verteilt, so entstehen keine Lücken
                val inner = if (cols >= 4) 2 else cols
                val columns: List<List<Island>> = if (cols >= 4) {
                    val l = mutableListOf<Island>(); val r = mutableListOf<Island>(); var nl = 0; var nr = 0
                    Island.entries.forEach { i -> val n = (Lab.entries.count { it.island == i } + 1) / 2 + 1; if (nl <= nr) { l += i; nl += n } else { r += i; nr += n } }
                    listOf(l, r)
                } else listOf(Island.entries)
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.Top) {
                    columns.forEach { col -> Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        col.forEach { island ->
                            Text("${island.emoji} ${island.title}", color = island.color.color(), fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 6.dp))
                            Lab.entries.filter { it.island == island }.chunked(inner).forEach { row ->
                                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEachIndexed { k, lab -> LabTile(lab, locked(lab), k, Modifier.weight(1f).fillMaxHeight()) { onOpen(lab) } }
                                    repeat(inner - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    } }
                }
                Spacer(Modifier.height(24.dp))
            }
            }
        }
    }
}

@Composable
private fun LabTile(lab: Lab, lock: Boolean, k: Int, modifier: Modifier, onOpen: () -> Unit) {
    Column(modifier.popIn(k * 60).clip(RoundedCornerShape(18.dp)).background(E.Deep).border(1.5.dp, lab.island.color.color().copy(alpha = 0.6f), RoundedCornerShape(18.dp))
        .clickable { onOpen() }.padding(12.dp).testTag("labor_${lab.name}")) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text(lab.emoji, fontSize = 26.sp); if (lock) Text(" 🔒", fontSize = 14.sp) }
        Text(lab.title, color = E.Text, fontWeight = FontWeight.Bold)
        Text(lab.blurb, color = E.Muted, fontSize = 11.sp, maxLines = 3)
    }
}

@Composable
fun FreeLabRun(lab: Lab, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        BubbleBackground(tint = lab.island.color.color(), count = 12)
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Bar("${lab.emoji} ${lab.title}", onBack) { Pill("frei forschen", E.Mint) }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
                LabView(FreeLab.setup(lab), free = true, onGoal = {})
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
