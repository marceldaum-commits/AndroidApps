package de.wissensgarten.app.ui.review

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.wissensgarten.app.ui.common.Plant
import de.wissensgarten.app.ui.common.formatDays
import de.wissensgarten.app.ui.common.plantsWord
import de.wissensgarten.app.ui.theme.Water
import de.wissensgarten.core.data.db.BedEntity
import de.wissensgarten.core.model.Memory
import de.wissensgarten.core.model.Rating
import de.wissensgarten.core.model.Scheduler
import de.wissensgarten.core.model.Stage
import de.wissensgarten.core.model.StudyCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Phase { QUESTION, ANSWER, GROWING }

private val RightGreen = Color(0xFFCDE8BE)
private val WrongOrange = Color(0xFFFFD2B8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    items: List<StudyCard>,
    beds: Map<String, BedEntity>,
    simpleRating: Boolean,
    onRate: (StudyCard, Rating) -> Unit,
    onClose: () -> Unit,
) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(Phase.QUESTION) }
    var picked by remember { mutableStateOf<String?>(null) }
    var grownMemory by remember { mutableStateOf<Memory?>(null) }
    var grown by rememberSaveable { mutableIntStateOf(0) }
    var dew by rememberSaveable { mutableIntStateOf(0) }
    var correct by rememberSaveable { mutableIntStateOf(0) }
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val growAnim = remember { Animatable(1f) }
    val dropAnim = remember { Animatable(0f) }

    BackHandler { onClose() }

    if (items.isEmpty() || index >= items.size) {
        SessionDone(items.size, grown, dew, correct, onClose)
        return
    }
    val card = items[index]
    val bed = beds[card.bedId]
    val now = remember(index) { System.currentTimeMillis() }
    val shownMemory = if (phase == Phase.GROWING) grownMemory ?: card.memory else card.memory

    fun rate(r: Rating) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        val after = Scheduler.review(card.memory, r, now)
        if (Stage.of(after) > Stage.of(card.memory)) grown++
        if (r != Rating.AGAIN) correct++
        dew += if (r == Rating.AGAIN) 1 else 3
        grownMemory = after
        onRate(card, r)
        phase = Phase.GROWING
        scope.launch {
            launch { dropAnim.snapTo(0f); dropAnim.animateTo(1f, tween(700, easing = LinearEasing)) }
            growAnim.snapTo(0.55f)
            growAnim.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
            delay(350)
            picked = null
            grownMemory = null
            phase = Phase.QUESTION
            index++
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    LinearProgressIndicator(
                        progress = { index / items.size.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Runde beenden") } },
                actions = { Text("${index + 1}/${items.size}", Modifier.padding(end = 16.dp)) },
            )
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "${bed?.emoji ?: ""} ${bed?.name ?: ""}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(Modifier.fillMaxWidth().weight(0.8f), contentAlignment = Alignment.Center) {
                Plant(
                    Stage.of(shownMemory),
                    if (phase == Phase.GROWING) 0f else Scheduler.wilt(shownMemory, now),
                    bed?.hue ?: 120f,
                    card.id.hashCode() and 0xff,
                    Modifier.fillMaxHeight(0.95f).aspectRatio(0.85f),
                    grow = growAnim.value,
                )
                if (dropAnim.value > 0f && dropAnim.value < 1f) WaterDrops(dropAnim.value, Modifier.matchParentSize())
            }

            Column(
                Modifier.weight(1.2f).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(card.front, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))

                if (card.isChoice) {
                    // Auswahl-Aufgabe: automatisch bewertet
                    card.options().forEach { o ->
                        val isRight = o == card.back
                        val bg = when {
                            picked == null -> MaterialTheme.colorScheme.surfaceContainerLow
                            isRight -> RightGreen
                            o == picked -> WrongOrange
                            else -> MaterialTheme.colorScheme.surfaceContainerLow
                        }
                        OutlinedButton(
                            onClick = {
                                if (picked == null) {
                                    picked = o
                                    phase = Phase.ANSWER
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = bg, contentColor = if (picked != null && (isRight || o == picked)) Color(0xFF22261F) else MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(vertical = 3.dp),
                        ) { Text(o, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center) }
                    }
                    if (picked != null) {
                        val right = picked == card.back
                        Feedback(right, card)
                        Button(
                            onClick = { rate(if (right) Rating.GOOD else Rating.AGAIN) },
                            enabled = phase == Phase.ANSWER,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                        ) { Text(if (right) "Gießen 💧" else "Weiter", style = MaterialTheme.typography.titleMedium) }
                    }
                } else {
                    AnimatedContent(phase == Phase.QUESTION, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) }, label = "phase") { asking ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            if (asking) {
                                Text(
                                    if (card.memory.isSeed) "Neuer Samen – kennst du es schon?" else "Weißt du es noch?",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = { phase = Phase.ANSWER }, modifier = Modifier.fillMaxWidth().height(60.dp)) {
                                    Text("Antwort zeigen", style = MaterialTheme.typography.titleMedium)
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(card.back, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                                        card.info?.let { Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp)) }
                                    }
                                }
                                Spacer(Modifier.height(14.dp))
                                RatingButtons(card.memory, now, simpleRating, enabled = phase == Phase.ANSWER, onRate = ::rate)
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
private fun Feedback(right: Boolean, card: StudyCard) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (right) "Richtig! 🎉" else "Richtig ist: ${card.back}",
            style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
            color = if (right) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
        )
        card.info?.let { Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp)) }
    }
}

@Composable
private fun RatingButtons(memory: Memory, now: Long, simple: Boolean, enabled: Boolean, onRate: (Rating) -> Unit) {
    val ratings = if (simple) listOf(Rating.AGAIN, Rating.GOOD) else Rating.entries
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        ratings.forEach { r ->
            val colors = when (r) {
                Rating.AGAIN -> ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                Rating.GOOD -> ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                else -> ButtonDefaults.filledTonalButtonColors()
            }
            val label = if (simple) (if (r == Rating.AGAIN) "Wusste ich nicht" else "Wusste ich!") else r.label
            FilledTonalButton(
                onClick = { onRate(r) },
                enabled = enabled,
                colors = colors,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                modifier = Modifier.weight(1f).heightIn(min = 64.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    if (!simple) Text(formatDays(Scheduler.previewDays(memory, r, now)), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun WaterDrops(progress: Float, modifier: Modifier) {
    Canvas(modifier) {
        val n = 7
        for (i in 0 until n) {
            val x = size.width * (0.3f + 0.4f * i / (n - 1))
            val y = size.height * (-0.1f - (i % 3) * 0.12f + progress * 0.9f)
            if (y > 0 && y < size.height * 0.75f) drawCircle(Water.copy(alpha = 0.85f), size.minDimension * 0.022f, Offset(x, y))
        }
    }
}

@Composable
private fun SessionDone(count: Int, grown: Int, dew: Int, correct: Int, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🌿", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(12.dp))
        Text(if (count == 0) "Nichts zu tun" else "Garten gegossen!", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        if (count > 0) {
            Text(
                "$count ${plantsWord(count)} versorgt, $correct gewusst" + if (grown > 0) ", $grown gewachsen." else ".",
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WaterDrop, null, tint = MaterialTheme.colorScheme.secondary)
                Text(" +$dew Tautropfen", style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Zurück in den Garten") }
    }
}
