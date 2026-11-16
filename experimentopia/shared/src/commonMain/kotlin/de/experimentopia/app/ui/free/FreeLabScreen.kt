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
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BlubbSays("Hier gibt es keine Aufgaben. Stell eigene Fragen, gib eigene Werte ein und probier aus, was passiert!", Mood.HAPPY, size = 56.dp, speak = false)
                Island.entries.forEach { island ->
                    Text("${island.emoji} ${island.title}", color = island.color.color(), fontWeight = FontWeight.Black)
                    Lab.entries.filter { it.island == island }.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEachIndexed { k, lab ->
                                val lock = locked(lab)
                                Column(Modifier.weight(1f).popIn(k * 60).clip(RoundedCornerShape(18.dp)).background(E.Deep).border(1.5.dp, island.color.color().copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                                    .clickable { onOpen(lab) }.padding(12.dp).testTag("labor_${lab.name}")) {
                                    Row(verticalAlignment = Alignment.CenterVertically) { Text(lab.emoji, fontSize = 26.sp); if (lock) Text(" 🔒", fontSize = 14.sp) }
                                    Text(lab.title, color = E.Text, fontWeight = FontWeight.Bold)
                                    Text(lab.blurb, color = E.Muted, fontSize = 11.sp, maxLines = 3)
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
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
