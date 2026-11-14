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
import de.wissensgarten.core.model.GameMode
import de.wissensgarten.core.model.Games
import de.wissensgarten.app.ui.play.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Phase { PLAY, REVEAL, BONUS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    items: List<StudyCard>,
    beds: Map<String, BedEntity>,
    simpleRating: Boolean,
    onRate: (StudyCard, Rating, Int) -> Unit,
    onClose: () -> Unit,
    peers: Map<String, List<String>> = emptyMap(),
    onBonus: (Int) -> Unit = {},
    calm: Boolean = false,
    subjects: Map<String, String?> = emptyMap(),
    onComplete: (correct: Int, total: Int) -> Unit = { _, _ -> },
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val canSpeak = remember { runCatching { speechAvailable(context) }.getOrDefault(false) }
    val voice = LocalVoice.current
    val readAloud = LocalReadAloud.current
    var index by rememberSaveable { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(Phase.PLAY) }
    var grownMemory by remember { mutableStateOf<Memory?>(null) }
    var lastRight by remember { mutableStateOf(true) }
    var grown by rememberSaveable { mutableIntStateOf(0) }
    var dew by rememberSaveable { mutableIntStateOf(0) }
    var correct by rememberSaveable { mutableIntStateOf(0) }
    var streak by rememberSaveable { mutableIntStateOf(0) }
    var bestStreak by rememberSaveable { mutableIntStateOf(0) }
    var bonusAt by rememberSaveable { mutableIntStateOf(-1) }
    var bonusCards by remember { mutableStateOf<List<StudyCard>>(emptyList()) }
    var levelUp by remember { mutableStateOf<String?>(null) }
    var gainText by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf(Mood.IDLE) }
    var speech by remember { mutableStateOf<String?>(null) }
    val haptics = LocalHapticFeedback.current
    val sfx = LocalSfx.current
    val scope = rememberCoroutineScope()
    val particles = rememberParticles()
    val growAnim = remember { Animatable(1f) }
    val pourAnim = remember { Animatable(0f) }
    val gainAnim = remember { Animatable(0f) }

    BackHandler { onClose() }

    if (items.isEmpty() || index >= items.size) {
        LaunchedEffect(Unit) { onComplete(correct, items.size) }
        if (calm) GoodNightDone(items.size, correct, onClose) else SessionDone(items.size, grown, dew, correct, bestStreak, onClose)
        return
    }
    val card = items[index]
    val bed = beds[card.bedId]
    val now = remember(index) { System.currentTimeMillis() }
    val cardPeers = peers[card.bedId].orEmpty()
    val mode = remember(card.id) { Games.choose(card, cardPeers, simpleRating, canSpeak, calm) }
    val subject = subjects[card.bedId]
    val options = remember(card.id) { Games.options(card, cardPeers) }
    val shownMemory = if (phase == Phase.REVEAL) grownMemory ?: card.memory else card.memory

    // Brummi begrüßt jede Karte passend
    LaunchedEffect(card.id) {
        mood = if (calm) Mood.IDLE else if (mode == GameMode.DISCOVER) Mood.HAPPY else Mood.THINK
        speech = if (calm) "Psst … ganz ruhig 🌙" else when (mode) {
            GameMode.DISCOVER -> "Neu! Schau dir das Bild gut an 👀"
            GameMode.BALLOONS -> "Schnell, bevor sie wegfliegen!"
            GameMode.LETTERS -> "Bau das Wort zusammen!"
            GameMode.SWIPE -> "Stimmt das wirklich? 🤔"
            GameMode.TILT -> "Kipp das Handy! 📱"
            GameMode.SPEAK -> "Ich höre zu! 👂"
            GameMode.TEACH -> null
            else -> null
        }
    }

    // Wer malt oder in Ruhe schauen will, hält das automatische Weiterblättern an
    var revealPaused by remember { mutableStateOf(false) }

    fun next() {
        revealPaused = false
        levelUp = null
        grownMemory = null
        index++
        // Alle 6 Karten: Zuordnen-Bonus aus bereits gespielten Karten
        if (index < items.size && index % 6 == 0 && bonusAt != index) {
            Games.matchPairs(items.take(index).filter { !it.memory.isSeed || it.img != null })?.let {
                bonusAt = index; bonusCards = it; phase = Phase.BONUS
                mood = Mood.CHEER; speech = "Bonusrunde! 🧩"
                return
            }
        }
        phase = Phase.PLAY
    }

    fun finish(r: Rating) {
        if (phase != Phase.PLAY) return
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        val after = Scheduler.review(card.memory, r, now)
        val up = Stage.of(after) > Stage.of(card.memory)
        val right = r != Rating.AGAIN
        lastRight = right
        if (up) grown++
        if (right) { correct++; streak++; bestStreak = maxOf(bestStreak, streak) } else streak = 0
        val mult = if (right) GardenRepository.comboMultiplier(streak) else 1
        val gain = GardenRepository.baseDew(r) * mult
        dew += gain
        gainText = "+$gain 💧" + if (mult > 1) "  ×$mult" else ""
        grownMemory = after
        onRate(card, r, gain)
        mood = when { !right -> Mood.SAD; streak >= 5 -> Mood.CHEER; else -> Mood.HAPPY }
        speech = when {
            !right -> BrummiSays.pick(BrummiSays.comfort, card.id.hashCode())
            streak >= 5 && streak % 5 == 0 -> BrummiSays.pick(BrummiSays.streak, streak)
            else -> BrummiSays.pick(BrummiSays.praise, card.id.hashCode() + streak)
        }
        if (right) particles.burst(Burst.SPARKLE, 0.5f, 0.2f, 26)
        phase = Phase.REVEAL
        if (readAloud) voice?.speak(card.back, Voice.localeFor(subject, card.back))
        sfx?.play(Sound.WATER, volume = if (calm) 0.4f else 0.9f)
        scope.launch {
            launch { gainAnim.snapTo(0f); gainAnim.animateTo(1f, tween(1300, easing = LinearOutSlowInEasing)) }
            launch { repeat(4) { particles.burst(Burst.WATER, 0.3f, 0.1f, 8); delay(120) } }
            pourAnim.snapTo(0f)
            pourAnim.animateTo(1f, tween(650, easing = LinearEasing))
            growAnim.snapTo(0.45f)
            val sprouted = up && card.memory.isSeed
            if (up && !sprouted) {
                levelUp = "⬆ Aufgestiegen: ${Stage.of(after).label}!"
                sfx?.play(Sound.LEVEL_UP)
                particles.burst(Burst.LEAVES, 0.3f, 0.15f, 36)
            } else if (sprouted) {
                levelUp = if (right) "🌱 Gekeimt!" else "🌱 Gesät"
                sfx?.play(Sound.GROW, volume = 0.7f)
            } else sfx?.play(Sound.GROW, volume = 0.5f)
            growAnim.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 220f))
            // Richtig: automatisch weiter. Falsch: Zeit zum Lesen, weiter per Knopf.
            if (right) { delay(if (up && !sprouted) 1600 else 1300); if (phase == Phase.REVEAL && !revealPaused) next() }
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
            Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${bed?.emoji ?: ""} ${bed?.name ?: ""}",
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, modifier = Modifier.weight(1f, fill = false),
                    )
                    StreakBadge(streak)
                }
                // Pflanze links, Brummi mit Sprechblase rechts
                Row(Modifier.fillMaxWidth().height(150.dp), verticalAlignment = Alignment.Bottom) {
                    Box(Modifier.width(130.dp).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                        Plant(
                            Stage.of(shownMemory),
                            if (phase == Phase.REVEAL) 0f else Scheduler.wilt(shownMemory, now),
                            bed?.hue ?: 120f, card.id.hashCode() and 0xff,
                            Modifier.fillMaxHeight().aspectRatio(0.85f),
                            grow = growAnim.value, species = bed?.species ?: "klassik",
                        )
                        if (pourAnim.value > 0f && pourAnim.value < 1f) WateringCan(pourAnim.value, Modifier.matchParentSize())
                        if (gainAnim.value in 0.01f..0.99f) Text(
                            gainText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.align(Alignment.TopCenter).graphicsLayer {
                                translationY = 40.dp.toPx() * (1 - gainAnim.value)
                                alpha = if (gainAnim.value < 0.7f) 1f else (1 - gainAnim.value) / 0.3f
                            },
                        )
                    }
                    Brummi(mood, Modifier.weight(1f).align(Alignment.CenterVertically), size = 84.dp, speech = speech)
                }
                LevelUpBanner(levelUp, Modifier.align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(8.dp))

                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedContent(
                        targetState = Triple(phase, card.id, bonusAt),
                        transitionSpec = { (slideInHorizontally { it / 2 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 2 } + fadeOut()) },
                        contentKey = { if (it.first == Phase.REVEAL) "reveal-${it.second}" else "${it.first}-${it.second}" },
                        label = "spiel",
                    ) { (p, _, _) ->
                        when (p) {
                            Phase.BONUS -> MatchBonus(bonusCards) { mistakes ->
                                val bonus = (10 - mistakes * 2).coerceAtLeast(2)
                                dew += bonus
                                onBonus(bonus)
                                sfx?.play(Sound.DEW)
                                particles.burst(Burst.CONFETTI, count = 60)
                                mood = Mood.CHEER; speech = "+$bonus 💧 Bonus!"
                                phase = Phase.PLAY
                            }
                            Phase.REVEAL -> RevealCard(card, lastRight, revealPaused, onPause = { revealPaused = true }) { next() }
                            Phase.PLAY -> CompositionLocalProvider(LocalSubject provides subject) { when (mode) {
                                GameMode.DISCOVER -> DiscoverGame(card, options) { finish(it.rating) }
                                GameMode.BALLOONS -> BalloonGame(card, options!!) { finish(it.rating) }
                                GameMode.LETTERS -> LetterGame(card) { finish(it.rating) }
                                GameMode.SWIPE -> SwipeGame(card, options!!) { finish(it.rating) }
                                GameMode.CHOICE -> ChoiceGame(card, options!!) { finish(it.rating) }
                                GameMode.FLIP -> FlipGame(card, now, simpleRating) { finish(it) }
                                GameMode.TEACH -> TeachGame(card, options!!) { finish(it.rating) }
                                GameMode.TILT -> TiltGame(card, options!!) { finish(it.rating) }
                                GameMode.SPEAK -> SpeakGame(card, options, Voice.localeFor(subject, card.back)) { finish(it.rating) }
                            } }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
        ParticleLayer(particles, Modifier.matchParentSize())
    }
}

/** Nach dem Spiel: Merkbild + Antwort, damit sich Bild und Wissen verbinden. */
@Composable
private fun RevealCard(card: StudyCard, right: Boolean, paused: Boolean, onPause: () -> Unit, onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = if (right) Color(0xFFE6F4DD) else Color(0xFFFFEDE2)),
            shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                MerkBild(card.img, emojiSize = 56.sp, key = "r" + card.id, cardId = card.id)
                var drawing by remember(card.id) { mutableStateOf(false) }
                AssistChip(onClick = { onPause(); drawing = true }, label = { Text(if (de.wissensgarten.app.ui.play.Drawings.rev >= 0 && de.wissensgarten.app.ui.play.Drawings.has(androidx.compose.ui.platform.LocalContext.current, card.id)) "✏️ Neu malen" else "✏️ Selbst malen") })
                if (drawing) de.wissensgarten.app.ui.play.DrawDialog(card.id, "${card.front} → ${card.back}", card.img) { drawing = false }
                Text(card.front, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, color = Color(0xFF4D4A3F))
                Text(card.back, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, color = Color(0xFF22261F))
                card.info?.let { Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = Color(0xFF4D4A3F), modifier = Modifier.padding(top = 6.dp)) }
            }
        }
        if (!right || paused) {
            Spacer(Modifier.height(12.dp))
            JuicyButton(onClick = onNext, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Gemerkt – weiter", style = MaterialTheme.typography.titleMedium) }
        }
    }
}

/** Eigene Karten ohne Auswahl: aufdecken und selbst einschätzen. */
@Composable
private fun FlipGame(card: StudyCard, now: Long, simple: Boolean, onRate: (Rating) -> Unit) {
    var open by remember(card.id) { mutableStateOf(false) }
    val sfx = LocalSfx.current
    val flip by animateFloatAsState(if (open) 180f else 0f, tween(450), label = "flip")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth().height(180.dp).graphicsLayer { rotationY = flip; cameraDistance = 12f * density },
            colors = CardDefaults.cardColors(containerColor = if (flip > 90f) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Box(Modifier.fillMaxSize().graphicsLayer { rotationY = if (flip > 90f) 180f else 0f }.padding(16.dp), contentAlignment = Alignment.Center) {
                Text(if (flip > 90f) card.back else card.front, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(14.dp))
        if (!open) JuicyButton(onClick = { sfx?.play(Sound.PLOP); open = true }, modifier = Modifier.fillMaxWidth().height(58.dp)) {
            Text("Umdrehen 🔄", style = MaterialTheme.typography.titleMedium)
        } else RatingButtons(card.memory, now, simple, enabled = true) { r ->
            sfx?.play(if (r == Rating.AGAIN) Sound.WRONG else Sound.CORRECT, volume = 0.7f)
            onRate(r)
        }
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


/** Ende der Gute-Nacht-Runde: ruhig, Sternenhimmel, schlafender Brummi. */
@Composable
private fun GoodNightDone(count: Int, correct: Int, onClose: () -> Unit) {
    val sfx = LocalSfx.current
    LaunchedEffect(Unit) { sfx?.play(Sound.DEW, volume = 0.4f) }
    Box(
        Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF0D1B3D), Color(0xFF34477A)))),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val r = kotlin.random.Random(5)
            repeat(60) { drawCircle(Color.White.copy(alpha = 0.3f + r.nextFloat() * 0.6f), 1.5f + r.nextFloat() * 2f, Offset(r.nextFloat() * size.width, r.nextFloat() * size.height * 0.7f)) }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
            Brummi(Mood.SLEEP, size = 120.dp)
            Text("Gute Nacht! 🌙", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Text("$correct von $count gewusst. Dein Gehirn sortiert das jetzt im Schlaf – so bleibt es besser hängen.",
                color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(28.dp))
            JuicyButton(onClick = onClose, sound = null, modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C6BC0))) { Text("Schlaf gut 💤") }
        }
    }
}
