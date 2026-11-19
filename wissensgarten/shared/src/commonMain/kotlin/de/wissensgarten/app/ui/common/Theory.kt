package de.wissensgarten.app.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.app.ui.fx.LocalReadAloud
import de.wissensgarten.app.ui.fx.LocalVoice
import de.wissensgarten.app.ui.fx.Locale
import de.wissensgarten.core.model.IntroScene

/** Theorie zu einem Beet: Titel + Brummis Erklär-Szenen des Pakets (verrät nie die Antwort der aktuellen Karte). */
data class Theory(val title: String, val scenes: List<IntroScene>,
                  /** Abgestimmte Links zu unseren anderen Lern-Apps (Üben, Tempo, Erleben, Ausprobieren) */
                  val links: List<de.wissensgarten.core.model.Querverweise.Link> = emptyList())

/** Liefert die Theorie zu einem Beet (bedId) – oder null bei eigenen Beeten ohne Erklärung. */
val LocalTheory = staticCompositionLocalOf<(String) -> Theory?> { { null } }

/**
 * Knopf „📖 Theorie“ für jede Übung: öffnet Brummis Erklärung zum Thema, damit niemand frustriert hängen bleibt.
 * Kostet nichts, wertet nichts – danach geht es mit derselben Frage weiter.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TheoryButton(bedId: String, modifier: Modifier = Modifier, onOpen: () -> Unit = {}) {
    val theory = LocalTheory.current(bedId) ?: return
    if (theory.scenes.isEmpty()) return
    var open by remember { mutableStateOf(false) }
    val wiggle by rememberInfiniteTransition(label = "theorie").animateFloat(-3f, 3f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "w")
    AssistChip(
        onClick = { open = true; onOpen() },
        label = { Text("📖 Theorie") },
        modifier = modifier.graphicsLayer { rotationZ = wiggle * 0.4f }.semantics { contentDescription = "Theorie zum Thema anzeigen" },
    )
    if (open) TheorySheet(theory) { open = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TheorySheet(theory: Theory, onClose: () -> Unit) {
    val voice = LocalVoice.current
    val readAloud = LocalReadAloud.current
    LaunchedEffect(theory) { if (readAloud) voice?.speak(de.wissensgarten.core.model.Nova.theorie(theory.scenes), Locale.GERMAN) }
    ModalBottomSheet(onDismissRequest = { voice?.stop(); onClose() }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🐝", fontSize = 36.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Brummi erklärt", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(theory.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            itemsIndexed(theory.scenes) { i, scene ->
                var shown by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { kotlinx.coroutines.delay(120L * i); shown = true }
                AnimatedVisibility(shown, enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 3 }) {
                    Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFFFFF8E1), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(scene.img, fontSize = 32.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(scene.text, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF3B3A30))
                        }
                    }
                }
            }
            if (theory.links.isNotEmpty()) item {
                val uri = androidx.compose.ui.platform.LocalUriHandler.current
                var shown by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { kotlinx.coroutines.delay(120L * theory.scenes.size + 200); shown = true }
                AnimatedVisibility(shown, enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 3 }) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Hier geht's weiter", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        theory.links.forEach { l ->
                            OutlinedButton(onClick = { voice?.stop(); runCatching { uri.openUri(l.url) } }, modifier = Modifier.fillMaxWidth()
                                .semantics { contentDescription = l.text }) {
                                Text(l.emoji, fontSize = 20.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(l.text, modifier = Modifier.weight(1f))
                            }
                        }
                        Text("Brummi bleibt hier – merken und wiederholen geht im Wissensgarten weiter.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Button(onClick = { voice?.stop(); onClose() }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Text("Alles klar – weiter geht's! 🌱") }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
