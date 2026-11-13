package de.wissensgarten.app.ui.review

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.app.ui.common.Plant
import de.wissensgarten.app.ui.common.formatDays
import de.wissensgarten.app.ui.fx.*
import de.wissensgarten.app.ui.theme.Water
import de.wissensgarten.core.data.GardenRepository
import de.wissensgarten.core.data.db.BedEntity
import de.wissensgarten.core.model.Memory
import de.wissensgarten.core.model.Rating
import de.wissensgarten.core.model.Scheduler
import de.wissensgarten.core.model.Stage
import de.wissensgarten.core.model.StudyCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Phase { QUESTION, ANSWER, GROWING }

private val RightGreen = Color(0xFFBFE3AE)
private val WrongOrange = Color(0xFFFFCBB0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    items: List<StudyCard>,
    beds: Map<String, BedEntity>,
    simpleRating: Boolean,
    onRate: (StudyCard, Rating, Int) -> Unit,
    onClose: () -> Unit,
) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(Phase.QUESTION) }
    var picked by remember { mutableStateOf<String?>(null) }
    var grownMemory by remember { mutableStateOf<Memory?>(null) }
    var grown by rememberSaveable { mutableIntStateOf(0) }
    var dew by rememberSaveable { mutableIntStateOf(0) }
    var correct by rememberSaveable { mutableIntStateOf(0) }
    var streak by rememberSaveable { mutableIntStateOf(0) }
    var bestStreak by rememberSaveable { mutableIntStateOf(0) }
    var levelUp by remember { mutableStateOf<String?>(null) }
    var gainText by remember { mutableStateOf("") }
    val haptics = LocalHapticFeedback.current
    val sfx = LocalSfx.current
    val scope = rememberCoroutineScope()
    val particles = rememberParticles()
    val shake = rememberShake()
    val growAnim = remember { Animatable(1f) }
    val pourAnim = remember { Animatable(0f) }
    val gainAnim = remember { Animatable(0f) }

    BackHandler { onClose() }

    if (items.isEmpty() || index >= items.size) {
        SessionDone(items.size, grown, dew, correct, bestStreak, onClose)
        return
    }
    val card = items[index]
    val bed = beds[card.bedId]
    val now = remember(index) { System.currentTimeMillis() }
    val shownMemory = if (phase == Phase.GROWING) grownMemory ?: card.memory else card.memory

    fun rate(r: Rating) {
        if (phase == Phase.GROWING) return
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        val after = Scheduler.review(card.memory, r, now)
        val up = Stage.of(after) > Stage.of(card.memory)
        if (up) grown++
        if (r != Rating.AGAIN) { correct++; streak++; bestStreak = maxOf(bestStreak, streak) } else streak = 0
        val mult = if (r != Rating.AGAIN) GardenRepository.comboMultiplier(streak) else 1
        val gain = GardenRepository.baseDew(r) * mult
        dew += gain
        gainText = "+$gain 💧" + if (mult > 1) "  ×$mult" else ""
        grownMemory = after
        onRate(card, r, gain)
        phase = Phase.GROWING
        sfx?.play(Sound.WATER)
        scope.launch {
            launch { gainAnim.snapTo(0f); gainAnim.animateTo(1f, tween(1300, easing = LinearOutSlowInEasing)) }
            launch {
                repeat(4) { particles.burst(Burst.WATER, 0.5f, 0.12f, 10); delay(120) }
            }
            pourAnim.snapTo(0f)
            pourAnim.animateTo(1f, tween(650, easing = LinearEasing))
            growAnim.snapTo(0.45f)
            val sprouted = up && card.memory.isSeed
            if (up && !sprouted) {
                levelUp = "⬆ Aufgestiegen: ${Stage.of(after).label}!"
                sfx?.play(Sound.LEVEL_UP)
                particles.burst(Burst.LEAVES, 0.5f, 0.3f, 36)
                particles.burst(Burst.SPARKLE, 0.5f, 0.25f, 24)
            } else if (sprouted) {
                levelUp = if (r == Rating.AGAIN) "🌱 Gesät" else "🌱 Gekeimt!"
                sfx?.play(Sound.GROW, volume = 0.7f)
                particles.burst(Burst.LEAVES, 0.5f, 0.45f, 10)
            } else sfx?.play(Sound.GROW, volume = 0.5f)
            growAnim.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 220f))
            delay(if (up && !sprouted) 900 else 350)
            levelUp = null
            picked = null
            grownMemory = null
            phase = Phase.QUESTION
            index++
        }
    }

    fun pick(o: String) {
        if (picked != null) return
        picked = o
        phase = Phase.ANSWER
        if (o == card.back) {
            sfx?.play(Sound.CORRECT, pitch = 1f + 0.06f * minOf(streak, 8))
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            particles.burst(Burst.SPARKLE, 0.5f, 0.62f, 28)
        } else {
            sfx?.play(Sound.WRONG)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch { shake.shake() }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val progress by animateFloatAsState(index / items.size.toFloat(), spring(), label = "progress")
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            color = MaterialTheme.colorScheme.secondary,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${bed?.emoji ?: ""} ${bed?.name ?: ""}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    StreakBadge(streak)
                }
                Box(Modifier.fillMaxWidth().weight(0.8f), contentAlignment = Alignment.Center) {
                    Plant(
                        Stage.of(shownMemory),
                        if (phase == Phase.GROWING) 0f else Scheduler.wilt(shownMemory, now),
                        bed?.hue ?: 120f,
                        card.id.hashCode() and 0xff,
                        Modifier.fillMaxHeight(0.95f).aspectRatio(0.85f),
                        grow = growAnim.value,
                        species = bed?.species ?: "klassik",
                    )
                    if (pourAnim.value > 0f && pourAnim.value < 1f) WateringCan(pourAnim.value, Modifier.matchParentSize())
                    if (gainAnim.value in 0.01f..0.99f) Text(
                        gainText,
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp)
                            .graphicsLayer {
                                translationY = 60.dp.toPx() * (1 - gainAnim.value) - 20.dp.toPx()
                                alpha = if (gainAnim.value < 0.7f) 1f else (1 - gainAnim.value) / 0.3f
                            },
                    )
                    LevelUpBanner(levelUp, Modifier.align(Alignment.TopCenter))
                }

                Column(
                    Modifier.weight(1.2f).verticalScroll(rememberScrollState()).shake(shake),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AnimatedContent(card.id, transitionSpec = {
                        (slideInHorizontally { it / 2 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 2 } + fadeOut())
                    }, label = "q") { _ ->
                        Text(card.front, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(14.dp))

                    if (card.isChoice) {
                        card.options().forEach { o -> ChoiceButton(o, card.back, picked) { pick(o) } }
                        if (picked != null) {
                            val right = picked == card.back
                            Feedback(right, card)
                            JuicyButton(
                                onClick = { rate(if (right) Rating.GOOD else Rating.AGAIN) },
                                enabled = phase == Phase.ANSWER,
                                modifier = Modifier.fillMaxWidth().height(58.dp).pulse(phase == Phase.ANSWER, 0.03f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                sound = null,
                            ) { Text(if (right) "Gießen 💧" else "Trotzdem gießen 💧", style = MaterialTheme.typography.titleMedium) }
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
                                    JuicyButton(onClick = { phase = Phase.ANSWER }, modifier = Modifier.fillMaxWidth().height(60.dp)) {
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
                                    RatingButtons(card.memory, now, simpleRating, enabled = phase == Phase.ANSWER) { r ->
                                        if (r != Rating.AGAIN) {
                                            sfx?.play(Sound.CORRECT, pitch = 1f + 0.06f * minOf(streak, 8))
                                            particles.burst(Burst.SPARKLE, 0.5f, 0.7f, 22)
                                        } else {
                                            sfx?.play(Sound.WRONG, volume = 0.6f)
                                            scope.launch { shake.shake() }
                                        }
                                        rate(r)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
        ParticleLayer(particles, Modifier.matchParentSize())
    }
}

@Composable
private fun StreakBadge(streak: Int) {
    AnimatedVisibility(streak >= 2, enter = scaleIn(spring(dampingRatio = 0.4f)) + fadeIn(), exit = scaleOut() + fadeOut()) {
        val mult = GardenRepository.comboMultiplier(streak)
        // springt bei jeder neuen Serie kurz auf
        val bump = remember { Animatable(1f) }
        LaunchedEffect(streak) { bump.snapTo(1.35f); bump.animateTo(1f, spring(dampingRatio = 0.3f)) }
        Row(
            Modifier.padding(start = 8.dp).graphicsLayer { scaleX = bump.value; scaleY = bump.value }
                .clip(CircleShape).background(if (mult > 1) Color(0xFFFF7043) else Color(0xFFFFB74D))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🔥 $streak", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
            if (mult > 1) Text("  ×$mult", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
    }
}

@Composable
private fun LevelUpBanner(text: String?, modifier: Modifier) {
    AnimatedVisibility(
        text != null, modifier = modifier,
        enter = scaleIn(spring(dampingRatio = 0.35f, stiffness = 300f)) + fadeIn(),
        exit = fadeOut() + scaleOut(),
    ) {
        var last by remember { mutableStateOf("") }
        if (text != null) last = text
        Surface(color = Color(0xFFFFD54F), shape = CircleShape, shadowElevation = 6.dp) {
            Text(
                last,
                fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF4E3B00),
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun ChoiceButton(option: String, answer: String, picked: String?, onClick: () -> Unit) {
    val isRight = option == answer
    val target = when {
        picked == null -> MaterialTheme.colorScheme.surfaceContainerLow
        isRight -> RightGreen
        option == picked -> WrongOrange
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val bg by animateColorAsState(target, tween(250), label = "bg")
    val pop = remember { Animatable(1f) }
    LaunchedEffect(picked) {
        if (picked != null && isRight) { pop.snapTo(1.08f); pop.animateTo(1f, spring(dampingRatio = 0.3f)) }
    }
    val interaction = remember { MutableInteractionSource() }
    val dim = picked != null && !isRight && option != picked
    OutlinedButton(
        onClick = onClick,
        interactionSource = interaction,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = bg,
            contentColor = if (picked != null && (isRight || option == picked)) Color(0xFF22261F) else MaterialTheme.colorScheme.onSurface,
        ),
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 4.dp)
            .pressBounce(interaction)
            .graphicsLayer { scaleX = pop.value; scaleY = pop.value; alpha = if (dim) 0.5f else 1f },
    ) {
        Text(
            (if (picked != null && isRight) "✓ " else if (picked == option && !isRight) "✗ " else "") + option,
            style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Feedback(right: Boolean, card: StudyCard) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (right) listOf("Richtig! 🎉", "Super! ⭐", "Genau! 🌟", "Klasse! 🙌")[card.id.hashCode().mod(4)] else "Richtig ist: ${card.back}",
            style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold,
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
                Rating.AGAIN -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
                Rating.HARD -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest, contentColor = MaterialTheme.colorScheme.onSurface)
                Rating.GOOD -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                Rating.EASY -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            val label = if (simple) (if (r == Rating.AGAIN) "Wusste ich nicht" else "Wusste ich!") else r.label
            JuicyButton(
                onClick = { onRate(r) },
                enabled = enabled,
                colors = colors,
                sound = null,
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

/** Gießkanne kippt über die Pflanze und gießt einen Wasserstrahl. */
@Composable
private fun WateringCan(progress: Float, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val tilt = when {
            progress < 0.2f -> progress / 0.2f
            progress > 0.85f -> (1f - progress) / 0.15f
            else -> 1f
        }
        val body = Offset(w * 0.68f, h * 0.08f)
        val bw = w * 0.2f; val bh = h * 0.13f
        val can = Color(0xFF4F9FD6); val canDark = Color(0xFF2F78AD)
        rotate(-35f * tilt, pivot = body + Offset(bw / 2, bh / 2)) {
            drawRoundRect(can, body, Size(bw, bh), androidx.compose.ui.geometry.CornerRadius(bw * 0.15f))
            drawArc(canDark, 180f, 180f, false, body + Offset(bw * 0.2f, -bh * 0.35f), Size(bw * 0.6f, bh * 0.7f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = bw * 0.08f))
            val spout = Path().apply {
                moveTo(body.x, body.y + bh * 0.6f); lineTo(body.x - bw * 0.55f, body.y + bh * 0.05f)
                lineTo(body.x - bw * 0.48f, body.y - bh * 0.05f); lineTo(body.x, body.y + bh * 0.35f); close()
            }
            drawPath(spout, canDark)
        }
        // Wasserstrahl
        if (tilt > 0.6f) {
            val from = Offset(w * 0.57f, h * 0.12f)
            for (i in 0 until 9) {
                val f = ((progress * 4 + i / 9f) % 1f)
                val p = Offset(from.x - w * 0.08f * f, from.y + h * 0.42f * f)
                drawLine(Water.copy(alpha = 0.8f), p, p + Offset(0f, h * 0.035f), strokeWidth = w * 0.012f, cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun SessionDone(count: Int, grown: Int, dew: Int, correct: Int, bestStreak: Int, onClose: () -> Unit) {
    val sfx = LocalSfx.current
    val particles = rememberParticles()
    var shownDew by remember { mutableIntStateOf(0) }
    val animDew = animatedCount(shownDew, 1500)
    val ratio = if (count == 0) 0f else correct.toFloat() / count
    val title = when {
        count == 0 -> "Nichts zu tun 🌿"
        ratio >= 0.9f -> "Grüner Daumen! 🏆"
        ratio >= 0.7f -> "Super gegossen! 🌟"
        else -> "Gut gemacht! 🌱"
    }
    val trophy = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (count > 0) {
            sfx?.play(Sound.FANFARE)
            particles.burst(Burst.CONFETTI, count = 90)
            launch { trophy.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 120f)) }
            delay(300)
            shownDew = dew
            delay(1300)
            sfx?.play(Sound.DEW)
            particles.burst(Burst.CONFETTI, count = 60)
        } else trophy.snapTo(1f)
    }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Plant(
                if (ratio >= 0.9f) Stage.TREE else Stage.BLOOM, 0f, 330f, 5,
                Modifier.size(170.dp).graphicsLayer { scaleX = trophy.value; scaleY = trophy.value },
            )
            Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            if (count > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("+$animDew", fontSize = 44.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                    Text(" 💧", fontSize = 36.sp)
                }
                Text("Tautropfen", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("🌱", "$count", "gegossen", Modifier.weight(1f))
                    StatTile("✅", "${(ratio * 100).toInt()} %", "gewusst", Modifier.weight(1f))
                    StatTile("🌳", "$grown", "gewachsen", Modifier.weight(1f))
                    StatTile("🔥", "$bestStreak", "beste Serie", Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(32.dp))
            JuicyButton(onClick = onClose, modifier = Modifier.fillMaxWidth().height(58.dp)) {
                Text("Zurück in den Garten", style = MaterialTheme.typography.titleMedium)
            }
        }
        ParticleLayer(particles, Modifier.matchParentSize())
    }
}

@Composable
private fun StatTile(icon: String, value: String, label: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 20.sp)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}
