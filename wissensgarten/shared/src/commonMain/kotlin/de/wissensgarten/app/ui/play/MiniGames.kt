package de.wissensgarten.app.ui.play

import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.app.ui.fx.*
import de.wissensgarten.core.model.GameResult
import de.wissensgarten.core.model.Games
import de.wissensgarten.core.model.StudyCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

private val RightGreen = Color(0xFFBFE3AE)
private val WrongOrange = Color(0xFFFFCBB0)
private val BalloonColors = listOf(Color(0xFFEF5350), Color(0xFF42A5F5), Color(0xFFFFCA28), Color(0xFF66BB6A), Color(0xFFAB47BC))

/** Frage mit Lautsprecher-Knopf; liest bei Bedarf automatisch vor. */
@Composable
fun QuestionText(text: String, modifier: Modifier = Modifier, autoRead: Boolean = true) {
    val voice = LocalVoice.current
    val auto = LocalReadAloud.current && autoRead
    val subject = LocalSubject.current
    LaunchedEffect(text) { if (auto) { delay(250); voice?.speak(text, Voice.localeFor(subject, text)) } }
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        SilbenText(text, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        if (voice != null) SpeakButton("Frage vorlesen", Modifier.padding(top = 8.dp)) { voice.speak(text, Voice.localeFor(subject, text)) }
    }
}

/**
 * Großer, gut sichtbarer Vorlese-Knopf (Daumengröße, mindestens 56 dp hoch) – an jeder Karte gleich.
 * Wackelt beim Erscheinen kurz, damit Kinder ihn entdecken.
 */
@Composable
fun SpeakButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val wiggle = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) { delay(400); repeat(2) { wiggle.animateTo(8f, tween(90)); wiggle.animateTo(-8f, tween(90)) }; wiggle.animateTo(0f, tween(90)) }
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp).graphicsLayer { rotationZ = wiggle.value }.semantics { contentDescription = label },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
    ) {
        Text("🔊", fontSize = 26.sp)
        Spacer(Modifier.width(10.dp))
        Text("Vorlesen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

// ---------------------------------------------------------------- Entdecken

/** Neue Karte: erst kennenlernen (Merkbild + Antwort), dann kurz prüfen. */
@Composable
fun DiscoverGame(card: StudyCard, options: List<String>?, onResult: (GameResult) -> Unit) {
    var checking by remember(card.id) { mutableStateOf(false) }
    val voice = LocalVoice.current
    val subject = LocalSubject.current
    val scope = rememberCoroutineScope()
    // Neue Karte: Frage, Antwort und Erklärung nacheinander vorlesen
    suspend fun readAll(withQuestion: Boolean) {
        val v = voice ?: return
        if (withQuestion) v.speakAndWait(card.front, Voice.localeFor(subject, card.front))
        v.speakAndWait(card.back, Voice.localeFor(subject, card.back))
        card.info?.takeIf { it.isNotBlank() }?.let { v.speakAndWait(it, Voice.localeFor(subject, it)) }
    }
    val auto = LocalReadAloud.current
    var manual by remember(card.id) { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    // beim Weitergehen zur Prüf-Frage hört das Vorlesen der Karte auf
    LaunchedEffect(card.id, checking) { if (checking) manual?.cancel() else if (auto) { delay(250); voice?.stop(); readAll(true) } }
    AnimatedContent(checking, transitionSpec = { (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut()) }, label = "discover") { check ->
        if (!check) Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Surface(color = Color(0xFFFFF3C4), shape = CircleShape) {
                Text("✨ Neu entdeckt", color = Color(0xFF4E3B00), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
            }
            Spacer(Modifier.height(10.dp))
            MerkBild(card.img, emojiSize = 72.sp, key = card.id, cardId = card.id)
            Spacer(Modifier.height(8.dp))
            QuestionText(card.front, autoRead = false)
            Text("↓", fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(card.back, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center)
            if (voice != null) SpeakButton("Antwort vorlesen", Modifier.padding(top = 8.dp)) { manual?.cancel(); voice.stop(); manual = scope.launch { readAll(false) } }
            card.info?.let { Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp)) }
            Spacer(Modifier.height(16.dp))
            JuicyButton(
                onClick = { if (options != null) checking = true else onResult(GameResult.PERFECT) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text("Gemerkt! 👍", style = MaterialTheme.typography.titleMedium) }
        } else Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("Und? Was war's?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            ChoiceGame(card, options!!, onResult)
        }
    }
}

// ---------------------------------------------------------------- Auswahl

@Composable
fun ChoiceGame(card: StudyCard, options: List<String>, onResult: (GameResult) -> Unit) {
    var picked by remember(card.id) { mutableStateOf<String?>(null) }
    val sfx = LocalSfx.current
    val shake = rememberShake()
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxWidth().shake(shake)) {
        QuestionText(card.front)
        Spacer(Modifier.height(14.dp))
        options.forEach { o ->
            val bg by animateColorAsState(
                when {
                    picked == null -> MaterialTheme.colorScheme.surfaceContainerLow
                    o == card.back -> RightGreen
                    o == picked -> WrongOrange
                    else -> MaterialTheme.colorScheme.surfaceContainerLow
                }, label = "bg",
            )
            val interaction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = {
                    if (picked != null) return@OutlinedButton
                    picked = o
                    val right = o == card.back
                    if (right) sfx?.play(Sound.CORRECT) else { sfx?.play(Sound.WRONG); scope.launch { shake.shake() } }
                    scope.launch { delay(if (right) 450 else 900); onResult(if (right) GameResult.PERFECT else GameResult.WRONG) }
                },
                interactionSource = interaction,
                colors = ButtonDefaults.outlinedButtonColors(containerColor = bg, contentColor = if (picked != null && (o == card.back || o == picked)) Color(0xFF22261F) else MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 4.dp).pressBounce(interaction),
            ) { Text(o, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center) }
        }
    }
}

// ---------------------------------------------------------------- Ballon-Jagd

private class Balloon(val text: String, val color: Color, val lane: Int, val speed: Float, val phase: Float) {
    var popped by mutableStateOf(false)
    var deflated by mutableStateOf(false)
}

/** Antworten steigen als Ballons auf – den richtigen antippen! Ein zweiter Versuch ist erlaubt. */
@Composable
fun BalloonGame(card: StudyCard, options: List<String>, onResult: (GameResult) -> Unit) {
    val sfx = LocalSfx.current
    val particles = rememberParticles()
    val scope = rememberCoroutineScope()
    val balloons = remember(card.id) {
        options.mapIndexed { i, o -> Balloon(o, BalloonColors[(i + card.id.hashCode()).mod(BalloonColors.size)], i, 0.055f + (i % 3) * 0.012f, i * 0.27f) }
    }
    var misses by remember(card.id) { mutableIntStateOf(0) }
    var done by remember(card.id) { mutableStateOf(false) }
    val t by rememberInfiniteTransition(label = "ballons").animateFloat(0f, 1000f, infiniteRepeatable(tween(1_000_000, easing = LinearEasing)), label = "t")
    var area by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    Column(Modifier.fillMaxWidth()) {
        QuestionText(card.front)
        Text("🎈 Tipp den richtigen Ballon an!", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        Box(
            Modifier.fillMaxWidth().height(300.dp).padding(top = 8.dp).clip(RoundedCornerShape(24.dp))
                .background(de.wissensgarten.app.ui.fx.SafeBrush.verticalGradient(listOf(Color(0xFFCDEBFA), Color(0xFFEAF6FD))))
                .onSizeChanged { area = it },
        ) {
            val lanes = balloons.size
            balloons.forEach { b ->
                if (b.popped) return@forEach
                val laneW = area.width / lanes.toFloat()
                // steigt auf, kommt unten wieder rein
                val prog = ((t * b.speed + b.phase) % 1.2f)
                val bw = with(density) { 96.dp.toPx() }; val bh = with(density) { 116.dp.toPx() }
                val x = laneW * (b.lane + 0.5f) - bw / 2 + sin(t * 1.3f + b.lane) * laneW * 0.12f
                val y = area.height * (1.05f - prog) - bh * 0.2f
                val deflate by animateFloatAsState(if (b.deflated) 0.55f else 1f, spring(), label = "deflate")
                Box(
                    Modifier.offset { androidx.compose.ui.unit.IntOffset(x.roundToInt(), y.roundToInt()) }
                        .size(96.dp, 116.dp)
                        .graphicsLayer { scaleX = deflate; scaleY = deflate; alpha = if (b.deflated) 0.45f else 1f }
                        .semantics { contentDescription = "Ballon: ${b.text}"; role = androidx.compose.ui.semantics.Role.Button }
                        .clickable(androidx.compose.runtime.remember { MutableInteractionSource() }, null) {
                            if (done || b.deflated) return@clickable
                            if (b.text == card.back) {
                                b.popped = true; done = true
                                sfx?.play(Sound.CORRECT)
                                particles.burst(Burst.SPARKLE, (x + bw / 2) / area.width, (y + bh * 0.4f) / area.height, 30)
                                particles.burst(Burst.CONFETTI, count = 0)
                                scope.launch { delay(500); onResult(if (misses == 0) GameResult.PERFECT else GameResult.SHAKY) }
                            } else {
                                b.deflated = true; misses++
                                sfx?.play(Sound.WRONG, volume = 0.7f)
                                if (misses >= 2) { done = true; scope.launch { delay(700); onResult(GameResult.WRONG) } }
                            }
                        },
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val w = size.width; val h = size.height
                        val body = Size(w * 0.92f, h * 0.78f)
                        drawLine(Color(0xFF8D6E63), Offset(w / 2, h * 0.78f), Offset(w / 2 + sin(t * 2f) * 6f, h), 2f)
                        drawOval(b.color, Offset((w - body.width) / 2, 0f), body)
                        drawOval(Color.White.copy(alpha = 0.35f), Offset(w * 0.2f, h * 0.08f), Size(w * 0.22f, h * 0.18f))
                        val knot = Path().apply { moveTo(w / 2 - 6f, h * 0.8f); lineTo(w / 2 + 6f, h * 0.8f); lineTo(w / 2, h * 0.76f); close() }
                        drawPath(knot, b.color)
                    }
                    Text(
                        b.text, color = Color.White, fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
                        fontSize = if (b.text.length > 12) 13.sp else 17.sp, lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 26.dp, start = 8.dp, end = 8.dp),
                    )
                }
            }
            ParticleLayer(particles, Modifier.matchParentSize())
        }
    }
}

// ---------------------------------------------------------------- Buchstabensalat

/** Antwort aus Buchstaben-Kacheln legen. Falscher Buchstabe wackelt, Tipp verrät den nächsten. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LetterGame(card: StudyCard, onResult: (GameResult) -> Unit) {
    val sfx = LocalSfx.current
    val scope = rememberCoroutineScope()
    val target = remember(card.id) { card.back }
    val tiles = remember(card.id) { Games.letterTiles(card.back, card.id.hashCode() + card.memory.reps).mapIndexed { i, c -> i to c } }
    val used = remember(card.id) { mutableStateListOf<Int>() }
    var mistakes by remember(card.id) { mutableIntStateOf(0) }
    val shakes = remember(card.id) { tiles.associate { it.first to Shake() } }
    var done by remember(card.id) { mutableStateOf(false) }
    val filled = used.size

    fun place(idx: Int) {
        if (done || idx in used) return
        val ch = tiles.first { it.first == idx }.second
        if (ch == target[filled]) {
            used += idx
            sfx?.play(Sound.TAP, pitch = 1f + filled * 0.06f)
            if (used.size == target.length) {
                done = true
                sfx?.play(Sound.CORRECT)
                scope.launch { delay(600); onResult(when { mistakes == 0 -> GameResult.PERFECT; mistakes <= 2 -> GameResult.SHAKY; else -> GameResult.WRONG }) }
            }
        } else {
            mistakes++
            sfx?.play(Sound.WRONG, volume = 0.5f)
            scope.launch { shakes.getValue(idx).shake() }
        }
    }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        // Merkbild zeigt, welches Wort gemeint ist (z. B. „Welches Wort ist richtig geschrieben?“)
        MerkBild(card.img, emojiSize = 44.sp, key = "l" + card.id, cardId = card.id)
        QuestionText(card.front)
        Text("🔤 Leg die Antwort aus den Buchstaben – auf Groß und klein achten!", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(14.dp))
        // Lücken
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            target.forEachIndexed { i, c ->
                val shown = i < filled
                val pop by animateFloatAsState(if (shown) 1f else 0.85f, spring(dampingRatio = 0.4f), label = "slot")
                Box(
                    Modifier.size(40.dp, 48.dp).graphicsLayer { scaleX = pop; scaleY = pop }.clip(RoundedCornerShape(10.dp))
                        .background(if (shown) (if (done) RightGreen else MaterialTheme.colorScheme.primaryContainer) else MaterialTheme.colorScheme.surfaceVariant)
                        .border(2.dp, if (i == filled && !done) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) { if (shown) Text("$c", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color(0xFF22261F)) }
            }
        }
        Spacer(Modifier.height(18.dp))
        // Kacheln
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tiles.forEach { (idx, c) ->
                val gone = idx in used
                val interaction = remember { MutableInteractionSource() }
                val alpha by animateFloatAsState(if (gone) 0f else 1f, label = "tile")
                Box(
                    Modifier.size(50.dp).shake(shakes.getValue(idx)).pressBounce(interaction)
                        .graphicsLayer { this.alpha = alpha; scaleX = 0.6f + 0.4f * alpha; scaleY = 0.6f + 0.4f * alpha }
                        .clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFE0B2))
                        .border(2.dp, Color(0xFFFFB74D), RoundedCornerShape(12.dp))
                        .clickable(interaction, null, enabled = !gone) { place(idx) },
                    contentAlignment = Alignment.Center,
                ) { Text("$c", fontWeight = FontWeight.Black, fontSize = 24.sp, color = Color(0xFF5D4037)) }
            }
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = {
            if (done) return@TextButton
            val next = tiles.first { it.first !in used && it.second == target[filled] }.first
            mistakes++; place(next)
        }) { Text("💡 Tipp") }
    }
}

// ---------------------------------------------------------------- Stimmt's?

/** Behauptung nach rechts (stimmt) oder links (stimmt nicht) wischen. */
@Composable
fun SwipeGame(card: StudyCard, options: List<String>, onResult: (GameResult) -> Unit) {
    val (shown, truth) = remember(card.id) { Games.swipeStatement(card, options) }
    val sfx = LocalSfx.current
    val scope = rememberCoroutineScope()
    val dx = remember(card.id) { Animatable(0f) }
    var answered by remember(card.id) { mutableStateOf<Boolean?>(null) }
    val density = LocalDensity.current
    val limit = with(density) { 110.dp.toPx() }

    fun decide(sayTrue: Boolean) {
        if (answered != null) return
        answered = sayTrue
        val right = sayTrue == truth
        if (right) sfx?.play(Sound.CORRECT) else sfx?.play(Sound.WRONG)
        scope.launch {
            dx.animateTo(if (sayTrue) limit * 4 else -limit * 4, tween(350))
            delay(if (right) 350 else 1100)
            onResult(if (right) GameResult.PERFECT else GameResult.WRONG)
        }
    }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("👆 Stimmt's? Wisch nach rechts oder links!", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) {
            val tint = (dx.value / limit).coerceIn(-1f, 1f)
            Card(
                modifier = Modifier.fillMaxWidth(0.9f).height(210.dp)
                    .graphicsLayer { translationX = dx.value; rotationZ = dx.value / 25f }
                    .pointerInput(card.id) {
                        detectDragGestures(
                            onDragEnd = {
                                when {
                                    dx.value > limit -> decide(true)
                                    dx.value < -limit -> decide(false)
                                    else -> scope.launch { dx.animateTo(0f, spring(dampingRatio = 0.5f)) }
                                }
                            },
                        ) { change, drag -> change.consume(); if (answered == null) scope.launch { dx.snapTo(dx.value + drag.x) } }
                    },
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        tint > 0.2f -> RightGreen.copy(alpha = 0.4f + 0.6f * tint)
                        tint < -0.2f -> WrongOrange.copy(alpha = 0.4f - 0.6f * tint)
                        else -> MaterialTheme.colorScheme.surfaceContainerLow
                    },
                ),
                elevation = CardDefaults.cardElevation(6.dp),
                shape = RoundedCornerShape(28.dp),
            ) {
                Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(card.front, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    Text("→", fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(shown, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, color = Color(0xFF22261F).takeIf { tint != 0f } ?: MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        answered?.let { said ->
            val right = said == truth
            Text(
                if (right) (if (truth) "Richtig – das stimmt! ✅" else "Richtig – das stimmt nicht! ✅") else "Leider falsch. Richtig ist: ${card.back}",
                fontWeight = FontWeight.Bold, color = if (right) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary, textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            JuicyButton(onClick = { decide(false) }, modifier = Modifier.weight(1f).height(56.dp), enabled = answered == null, sound = null,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)) { Text("👎 Stimmt nicht") }
            JuicyButton(onClick = { decide(true) }, modifier = Modifier.weight(1f).height(56.dp), enabled = answered == null, sound = null,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) { Text("Stimmt 👍") }
        }
    }
}

// ---------------------------------------------------------------- Zuordnen (Bonus)

/** Bonusrunde: Fragen links, Antworten rechts – Paare antippen. */
@Composable
fun MatchBonus(cards: List<StudyCard>, onDone: (mistakes: Int) -> Unit) {
    val sfx = LocalSfx.current
    val scope = rememberCoroutineScope()
    val left = remember { cards.shuffled(kotlin.random.Random(cards.hashCode())) }
    val right = remember { cards.shuffled(kotlin.random.Random(cards.hashCode() + 1)) }
    val matched = remember { mutableStateListOf<String>() }
    var selLeft by remember { mutableStateOf<String?>(null) }
    var mistakes by remember { mutableIntStateOf(0) }
    val shakes = remember { cards.associate { it.id to Shake() } }

    fun tryMatch(rightId: String) {
        val l = selLeft ?: return
        if (l == rightId) {
            matched += l; selLeft = null
            sfx?.play(Sound.CORRECT, pitch = 1f + matched.size * 0.08f)
            if (matched.size == cards.size) scope.launch { delay(500); onDone(mistakes) }
        } else {
            mistakes++; sfx?.play(Sound.WRONG, volume = 0.6f)
            scope.launch { launch { shakes.getValue(l).shake() }; shakes.getValue(rightId).shake() }
            selLeft = null
        }
    }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(color = Color(0xFFFFE0B2), shape = CircleShape) {
            Text("🧩 Bonus: Finde die Paare!", fontWeight = FontWeight.Bold, color = Color(0xFF5D4037), modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                left.forEach { c -> MatchChip(c.front, c.id in matched, selLeft == c.id, shakes.getValue(c.id)) { if (c.id !in matched) { sfx?.play(Sound.TAP); selLeft = c.id } } }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                right.forEach { c -> MatchChip(c.back, c.id in matched, false, shakes.getValue(c.id)) { if (c.id !in matched) tryMatch(c.id) } }
            }
        }
    }
}

@Composable
private fun MatchChip(text: String, done: Boolean, selected: Boolean, shake: Shake, onClick: () -> Unit) {
    val alpha by animateFloatAsState(if (done) 0.35f else 1f, label = "m")
    val bg = when { done -> RightGreen; selected -> Color(0xFFFFE082); else -> MaterialTheme.colorScheme.surfaceContainerLow }
    Box(
        Modifier.fillMaxWidth().heightIn(min = 58.dp).shake(shake).graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(16.dp)).background(bg)
            .border(2.dp, if (selected) Color(0xFFFFB300) else Color.Transparent, RoundedCornerShape(16.dp))
            .clickable(enabled = !done, onClick = onClick).padding(10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color(0xFF22261F).takeIf { done || selected } ?: MaterialTheme.colorScheme.onSurface) }
}

