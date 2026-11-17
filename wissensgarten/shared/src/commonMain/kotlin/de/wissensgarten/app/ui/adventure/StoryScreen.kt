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

/**
 * Abschluss eines Kapitels: Konfetti, und vor allem ein klarer nächster Schritt –
 * entweder das nächste Kapitel lesen oder Pflanzen gießen, um es freizuschalten.
 */
@Composable
fun ChapterEnd(done: Chapter, grown: Int, onNext: (Chapter) -> Unit, onWater: () -> Unit, onList: () -> Unit) {
    val sfx = LocalSfx.current
    val particles = rememberParticles()
    val next = Story.chapters.firstOrNull { it.number == done.number + 1 }
    val open = next != null && grown >= next.unlockAt
    androidx.compose.runtime.LaunchedEffect(Unit) {
        sfx?.play(Sound.FANFARE)
        particles.burst(Burst.CONFETTI, count = 70)
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF5C6BC0), Color(0xFFAB47BC), Color(0xFFF3E5F5))))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Kapitel ${done.number} geschafft! 🎉", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            val runtime = when {
                next == null -> null
                open -> de.wissensgarten.core.model.Laufzeit.kapitelOffen(next.number)
                else -> de.wissensgarten.core.model.Laufzeit.kapitelWartet(next.unlockAt - grown)
            }
            Brummi(Mood.CHEER, size = 120.dp, speech = when {
                next == null -> "Wir haben es geschafft – die Wissensblume leuchtet wieder! Danke, dass du mir geholfen hast!"
                else -> runtime!!.text
            }, speechRuntime = runtime)
            Spacer(Modifier.height(20.dp))
            if (next != null && !open) {
                val prev = done.unlockAt
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🌱", fontSize = 24.sp)
                    LinearProgressIndicator(
                        progress = { ((grown - prev).toFloat() / (next.unlockAt - prev)).coerceIn(0f, 1f) },
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp).height(12.dp).clip(CircleShape),
                        color = Color(0xFFFFD54F), trackColor = Color.White.copy(alpha = 0.35f),
                    )
                    Text("🔒 ${next.number}", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Text("$grown von ${next.unlockAt} Pflanzen gewachsen", color = Color.White, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                Spacer(Modifier.height(20.dp))
                JuicyButton(onClick = onWater, modifier = Modifier.fillMaxWidth().height(60.dp).pulse(true, 0.03f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F9FD6))) {
                    Text("💧 Jetzt Pflanzen wachsen lassen", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            } else if (next != null) {
                JuicyButton(onClick = { onNext(next) }, modifier = Modifier.fillMaxWidth().height(60.dp).pulse(true, 0.03f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F6B3A))) {
                    Text("📖 Weiter mit Kapitel ${next.number}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            TextButton(onClick = onList, modifier = Modifier.padding(top = 8.dp)) { Text("Zur Kapitelübersicht", color = Color.White) }
        }
        ParticleLayer(particles, Modifier.matchParentSize())
    }
}
