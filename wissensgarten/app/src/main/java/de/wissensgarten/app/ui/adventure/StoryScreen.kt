package de.wissensgarten.app.ui.adventure

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.app.ui.fx.*
import de.wissensgarten.core.model.Chapter
import de.wissensgarten.core.model.Story

/** Kapitelübersicht von „Brummi und die Wissensblume“. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryScreen(grown: Int, read: Set<Int>, onBack: () -> Unit, onRead: (Chapter) -> Unit) {
    val next = Story.next(grown)
    Scaffold(topBar = {
        TopAppBar(title = { Text("📖 Brummis Abenteuer") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } })
    }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Box(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge)
                    .background(Brush.linearGradient(listOf(Color(0xFF5C6BC0), Color(0xFFAB47BC)))).padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Brummi(Mood.HAPPY, size = 72.dp)
                        Column(Modifier.weight(1f)) {
                            Text("Brummi und die Wissensblume", color = Color.White, style = MaterialTheme.typography.titleLarge)
                            Text(
                                if (next != null) "Noch ${next.unlockAt - grown} gewachsene Pflanzen bis Kapitel ${next.number}!" else "Du hast alle Kapitel freigeschaltet! 🏆",
                                color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium,
                            )
                            if (next != null) {
                                val prev = Story.chapters.getOrNull(next.number - 2)?.unlockAt ?: 0
                                LinearProgressIndicator(
                                    progress = { ((grown - prev).toFloat() / (next.unlockAt - prev)).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(8.dp).clip(CircleShape),
                                    color = Color(0xFFFFD54F), trackColor = Color.White.copy(alpha = 0.3f),
                                )
                            }
                        }
                    }
                }
            }
            items(Story.chapters, key = { it.number }) { ch ->
                val open = grown >= ch.unlockAt
                val new = open && ch.number !in read
                Card(
                    onClick = { if (open) onRead(ch) },
                    colors = CardDefaults.cardColors(containerColor = if (open) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().then(if (new) Modifier.pulse(true, 0.02f) else Modifier),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (open) ch.emoji else "🔒", fontSize = 34.sp, modifier = Modifier.graphicsLayer { alpha = if (open) 1f else 0.5f })
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Kapitel ${ch.number}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (open) ch.title else "???", style = MaterialTheme.typography.titleMedium)
                            if (!open) Text("ab ${ch.unlockAt} gewachsenen Pflanzen", style = MaterialTheme.typography.bodySmall)
                        }
                        if (new) Surface(color = Color(0xFFFF7043), shape = CircleShape) {
                            Text("NEU", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}


/** Karte im Garten: Einstieg in Brummis Abenteuer, mit Hinweis auf neue Kapitel. */
@Composable
fun StoryEntryCard(newChapter: Chapter?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().then(if (newChapter != null) Modifier.pulse(true, 0.02f) else Modifier),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6)),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("📖", fontSize = 32.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Brummis Abenteuer", style = MaterialTheme.typography.titleMedium, color = Color(0xFF311B92))
                Text(
                    if (newChapter != null) "Neues Kapitel: ${newChapter.title}!" else "Die Geschichte der Wissensblume",
                    style = MaterialTheme.typography.bodySmall, color = Color(0xFF311B92),
                )
            }
            if (newChapter != null) Surface(color = Color(0xFFFF7043), shape = CircleShape) {
                Text("NEU", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }
    }
}
