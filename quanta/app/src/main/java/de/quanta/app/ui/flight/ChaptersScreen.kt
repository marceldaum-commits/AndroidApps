package de.quanta.app.ui.flight

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.quanta.app.ui.fx.*
import de.quanta.app.ui.theme.Q
import de.quanta.core.model.Chapter
import de.quanta.core.model.Level

/** Ein Level ist offen, wenn es das erste im Kapitel ist oder das vorige geschafft wurde. */
fun unlocked(chapter: Chapter, index: Int, stars: Map<String, Int>) = index == 0 || (stars[chapter.levels[index - 1].id] ?: 0) > 0

/** Kurvenflug: Kapitel nach Klassenstufe – alle Kapitel sind offen, innerhalb geht es Level für Level. */
@Composable
fun ChaptersScreen(chapters: List<Chapter>, stars: Map<String, Int>, onLevel: (Level) -> Unit, onBack: () -> Unit) {
    val colors = listOf(Q.Cyan, Q.Pink, Q.Yellow, Q.Green)
    Box(Modifier.fillMaxSize()) {
        Starfield(seed = 3)
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Q.Text) }
                Text("🚀 Kurvenflug", style = MaterialTheme.typography.titleLarge, color = Q.Text)
            }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                chapters.forEachIndexed { ci, c ->
                    val color = colors[ci % colors.size]
                    val got = c.levels.sumOf { stars[it.id] ?: 0 }
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Q.Panel.copy(alpha = 0.9f))
                            .border(1.5.dp, color.copy(alpha = 0.5f), RoundedCornerShape(20.dp)).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(c.emoji, fontSize = 28.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.title, style = MaterialTheme.typography.titleMedium, color = Q.Text)
                                Text(c.grades, style = MaterialTheme.typography.labelMedium, color = color)
                            }
                            Text("$got / ${c.levels.size * 3} ⭐", color = Q.Yellow, fontWeight = FontWeight.Bold)
                        }
                        // Level-Kacheln
                        c.levels.chunked(4).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { l ->
                                    val idx = c.levels.indexOf(l)
                                    LevelTile(l, stars[l.id] ?: 0, unlocked(c, idx, stars), color, Modifier.weight(1f)) { onLevel(l) }
                                }
                                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun LevelTile(l: Level, stars: Int, open: Boolean, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val sfx = LocalSfx.current
    val next = open && stars == 0
    Column(
        modifier.aspectRatio(0.9f).pressBounce(interaction).pulse(next, 0.05f).clip(RoundedCornerShape(14.dp))
            .background(if (open) color.copy(alpha = if (stars > 0) 0.25f else 0.12f) else Color(0x22000000))
            .border(2.dp, if (next) color else color.copy(alpha = if (open) 0.4f else 0.12f), RoundedCornerShape(14.dp))
            .clickable(interaction, null) { if (open) { sfx?.play(Sound.TAP); onClick() } else sfx?.play(Sound.WRONG, volume = 0.3f) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(if (open) "${l.number}" else "🔒", fontSize = 22.sp, fontWeight = FontWeight.Black, color = if (open) Q.Text else Q.Muted)
        Text(if (open) "★".repeat(stars) + "☆".repeat(3 - stars) else "", color = Q.Yellow, fontSize = 12.sp)
    }
}
