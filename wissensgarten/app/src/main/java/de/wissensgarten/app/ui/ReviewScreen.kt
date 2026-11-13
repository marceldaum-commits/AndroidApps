package de.wissensgarten.app.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.wissensgarten.app.data.Card
import de.wissensgarten.app.data.GardenState
import de.wissensgarten.app.data.Grade
import de.wissensgarten.app.data.Scheduler
import de.wissensgarten.app.data.SessionItem
import de.wissensgarten.app.data.Stage
import de.wissensgarten.app.ui.theme.Water
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Phase { QUESTION, ANSWER, GROWING }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    items: List<SessionItem>,
    state: GardenState,
    onGrade: (SessionItem, Grade) -> Unit,
    onClose: () -> Unit,
) {
    var index by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(Phase.QUESTION) }
    var grown by remember { mutableIntStateOf(0) }
    var dewEarned by remember { mutableIntStateOf(0) }
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val growAnim = remember { Animatable(1f) }
    val dropAnim = remember { Animatable(0f) }

    if (index >= items.size) {
        SessionDone(items.size, grown, dewEarned, onClose)
        return
    }
    val item = items[index]
    val bed = state.beds.firstOrNull { it.id == item.bedId }
    // aktuelle Version der Karte (nach dem Gießen schon gewachsen)
    val liveCard = bed?.cards?.firstOrNull { it.id == item.card.id } ?: item.card
    val now = System.currentTimeMillis()
    val shown = if (phase == Phase.GROWING) liveCard else item.card

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
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Plant(
                    Stage.of(shown),
                    if (phase == Phase.GROWING) 0f else Scheduler.wilt(shown, now),
                    bed?.hue ?: 120f,
                    shown.id.hashCode() and 0xff,
                    Modifier.fillMaxHeight(0.95f).aspectRatio(0.85f),
                    grow = growAnim.value,
                )
                if (dropAnim.value > 0f && dropAnim.value < 1f) WaterDrops(dropAnim.value, Modifier.matchParentSize())
            }

            AnimatedContent(phase, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) }, label = "phase") { p ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(item.card.front, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    when (p) {
                        Phase.QUESTION -> {
                            Text(if (item.card.isSeed) "Neuer Samen – kennst du es schon?" else "Weißt du es noch?",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(20.dp))
                            Button(
                                onClick = { phase = Phase.ANSWER },
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                            ) { Text("Antwort zeigen", style = MaterialTheme.typography.titleMedium) }
                        }
                        Phase.ANSWER, Phase.GROWING -> {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(item.card.back, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(18.dp))
                            }
                            Spacer(Modifier.height(16.dp))
                            GradeButtons(item.card, now, enabled = p == Phase.ANSWER) { grade ->
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                val after = Scheduler.review(item.card, grade, now)
                                if (Stage.of(after) > Stage.of(item.card)) grown++
                                onGrade(item, grade)
                                dewEarned += if (grade == Grade.AGAIN) 1 else 3
                                phase = Phase.GROWING
                                scope.launch {
                                    launch {
                                        dropAnim.snapTo(0f)
                                        dropAnim.animateTo(1f, tween(700, easing = LinearEasing))
                                    }
                                    growAnim.snapTo(0.55f)
                                    growAnim.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
                                    delay(350)
                                    phase = Phase.QUESTION
                                    index++
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun GradeButtons(card: Card, now: Long, enabled: Boolean, onGrade: (Grade) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Grade.entries.forEach { g ->
            val colors = when (g) {
                Grade.AGAIN -> ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                Grade.GOOD -> ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                else -> ButtonDefaults.filledTonalButtonColors()
            }
            FilledTonalButton(
                onClick = { onGrade(g) },
                enabled = enabled,
                colors = colors,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                modifier = Modifier.weight(1f).heightIn(min = 64.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(g.label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    Text(formatDays(Scheduler.previewDays(card, g, now)), style = MaterialTheme.typography.labelSmall)
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
            val start = -0.1f - (i % 3) * 0.12f
            val y = size.height * (start + progress * 0.9f)
            if (y > 0 && y < size.height * 0.75f) {
                drawCircle(Water.copy(alpha = 0.85f), radius = size.minDimension * 0.022f, center = Offset(x, y))
            }
        }
    }
}

@Composable
private fun SessionDone(count: Int, grown: Int, dew: Int, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🌿", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(12.dp))
        Text("Garten gegossen!", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "$count ${plantsWord(count)} versorgt" + if (grown > 0) ", $grown davon sind gewachsen." else ".",
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.WaterDrop, null, tint = MaterialTheme.colorScheme.secondary)
            Text(" +$dew Tautropfen", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Zurück in den Garten") }
    }
}
