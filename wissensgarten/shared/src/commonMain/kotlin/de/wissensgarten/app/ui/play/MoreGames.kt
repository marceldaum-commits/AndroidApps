package de.wissensgarten.app.ui.play

import androidx.compose.ui.focus.focusRequester
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import de.wissensgarten.app.ui.fx.Locale
import de.wissensgarten.app.ui.fx.LocalHost
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.app.ui.fx.*
import de.wissensgarten.core.model.GameResult
import de.wissensgarten.core.model.SpokenAnswer
import de.wissensgarten.core.model.StudyCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

// ---------------------------------------------------------------- Brummi beibringen

/**
 * Lernen durch Lehren: Brummi rät – meist falsch. Das Kind entscheidet, ob er recht hat,
 * und zeigt ihm sonst die richtige Antwort.
 */
@Composable
fun TeachGame(card: StudyCard, options: List<String>, onResult: (GameResult) -> Unit) {
    val rnd = remember(card.id) { Random(card.id.hashCode() + card.memory.reps * 3) }
    val brummiRight = remember(card.id) { rnd.nextInt(10) < 3 }
    val guess = remember(card.id) { if (brummiRight) card.back else options.filter { it != card.back }.random(rnd) }
    var step by remember(card.id) { mutableIntStateOf(0) } // 0 = Urteil, 1 = Korrektur, 2 = fertig
    var mood by remember(card.id) { mutableStateOf(Mood.THINK) }
    // Brummis Sätze mit Karten-Inhalt sind Laufzeit-Sätze (ganze Sätze für Nova; eigene Karten: Gerätestimme)
    val ausPaket = LocalSubject.current != null
    val L = de.wissensgarten.core.model.Laufzeit
    val guessCard = remember(card.id) { card.copy(back = guess) }
    var lineRt by remember(card.id) { mutableStateOf(L.lehrerRaet(card, guessCard, ausPaket)) }
    var line by remember(card.id) { mutableStateOf(L.lehrerRaetText(card.front, guess)) }
    val sfx = LocalSfx.current
    val scope = rememberCoroutineScope()

    fun finish(r: GameResult, m: Mood, text: String, rt: de.wissensgarten.core.model.LaufzeitText? = null) {
        step = 2; mood = m; line = text; lineRt = rt
        sfx?.play(if (r == GameResult.WRONG) Sound.WRONG else Sound.CORRECT)
        scope.launch { delay(if (r == GameResult.WRONG) 1500 else 1000); onResult(r) }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Surface(color = Color(0xFFFFF3C4), shape = CircleShape) {
            Text("🎓 Bring Brummi was bei!", color = Color(0xFF4E3B00), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
        }
        Spacer(Modifier.height(10.dp))
        Brummi(mood, size = 90.dp, speech = line, speechRuntime = lineRt)
        Spacer(Modifier.height(14.dp))
        when (step) {
            0 -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                JuicyButton(onClick = {
                    if (brummiRight) finish(GameResult.WRONG, Mood.SAD, L.lehrerHatteRechtText(guess), L.lehrerHatteRecht(card, ausPaket))
                    else { step = 1; mood = Mood.THINK; lineRt = null; line = "Oh! Was ist denn richtig?"; sfx?.play(Sound.TAP) }
                }, modifier = Modifier.weight(1f).height(58.dp), sound = null,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)) { Text("Nein, Brummi! ✋") }
                JuicyButton(onClick = {
                    if (brummiRight) finish(GameResult.PERFECT, Mood.CHEER, "Juhu, ich hatte recht! Danke! 🐝")
                    else finish(GameResult.WRONG, Mood.SAD, L.lehrerEigentlichText(card.back), L.lehrerEigentlich(card, ausPaket))
                }, modifier = Modifier.weight(1f).height(58.dp), sound = null,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) { Text("Stimmt! 👍") }
            }
            1 -> Column(Modifier.fillMaxWidth()) {
                options.filter { it != guess }.forEach { o ->
                    OutlinedButton(
                        onClick = {
                            if (o == card.back) finish(GameResult.PERFECT, Mood.CHEER, L.lehrerDankeText(card.back), L.lehrerDanke(card, ausPaket))
                            else finish(GameResult.WRONG, Mood.SAD, L.lehrerNichtRichtigText(card.back), L.lehrerNichtRichtig(card, ausPaket))
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(vertical = 3.dp),
                    ) { Text(o, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center) }
                }
            }
            else -> Unit
        }
    }
}

// ---------------------------------------------------------------- Kipp-Spiel

private class Cloud(val text: String, val lane: Int, var y: Float, val speed: Float) {
    var gone by mutableStateOf(false)
}

/** Handy kippen (oder wischen): die Gießkanne unten fängt die richtige Antwort-Wolke. */
@Composable
fun TiltGame(card: StudyCard, options: List<String>, onResult: (GameResult) -> Unit) {
    val host = LocalHost.current
    val sfx = LocalSfx.current
    val particles = rememberParticles()
    val scope = rememberCoroutineScope()
    val lanes = options.size
    val clouds = remember(card.id) { options.mapIndexed { i, o -> Cloud(o, i, -0.25f - i * 0.35f, 0.075f + (i % 2) * 0.02f) } }
    var canX by remember(card.id) { mutableFloatStateOf(0.5f) } // 0..1
    var misses by remember(card.id) { mutableIntStateOf(0) }
    var done by remember(card.id) { mutableStateOf(false) }
    var frame by remember { mutableLongStateOf(0L) }
    var area by remember { mutableStateOf(IntSize.Zero) }

    // Neigungssensor
    LaunchedEffect(card.id) {
        // Seitliche Neigung (m/s², wie Androids Beschleunigungssensor x-Achse)
        host?.tilt()?.collect { x -> canX = (canX - x * 0.012f).coerceIn(0.05f, 0.95f) }
    }
    // Wolken fallen
    LaunchedEffect(card.id) {
        var last = 0L
        while (!done) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0.016f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                for (c in clouds) {
                    if (c.gone) continue
                    c.y += c.speed * dt
                    if (c.y > 0.8f) {
                        val laneX = (c.lane + 0.5f) / lanes
                        if (abs(laneX - canX) < 0.5f / lanes) {
                            // gefangen!
                            if (c.text == card.back) {
                                done = true
                                sfx?.play(Sound.CORRECT)
                                particles.burst(Burst.WATER, canX, 0.8f, 30)
                                particles.burst(Burst.SPARKLE, canX, 0.75f, 24)
                                scope.launch { delay(600); onResult(if (misses == 0) GameResult.PERFECT else GameResult.SHAKY) }
                            } else {
                                c.gone = true; misses++
                                sfx?.play(Sound.WRONG, volume = 0.6f)
                                if (misses >= 2) { done = true; scope.launch { delay(700); onResult(GameResult.WRONG) } }
                            }
                        }
                        if (!c.gone && !done && c.y > 1.05f) c.y = -0.2f // verpasst → kommt wieder
                    }
                }
                frame = now
            }
        }
    }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        QuestionText(card.front)
        Text("📱 Kipp das Handy (oder wisch), um die richtige Wolke zu fangen!", style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        Box(
            Modifier.fillMaxWidth().height(320.dp).padding(top = 8.dp).clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF90CAF9), Color(0xFFE3F2FD), Color(0xFFC5E1A5))))
                .onSizeChanged { area = it }
                .pointerInput(card.id) { detectHorizontalDragGestures { ch, dx -> ch.consume(); canX = (canX + dx / size.width).coerceIn(0.05f, 0.95f) } },
        ) {
            frame // neu zeichnen
            val density = LocalDensity.current
            clouds.forEach { c ->
                if (c.gone || c.y < -0.2f) return@forEach
                val x = area.width * (c.lane + 0.5f) / lanes
                val y = area.height * c.y
                Box(
                    Modifier.offset { IntOffset((x - with(density) { 50.dp.toPx() }).roundToInt(), (y - with(density) { 26.dp.toPx() }).roundToInt()) }
                        .size(100.dp, 52.dp).clip(RoundedCornerShape(26.dp)).background(Color.White),
                    contentAlignment = Alignment.Center,
                ) { Text(c.text, fontWeight = FontWeight.Bold, fontSize = if (c.text.length > 10) 12.sp else 15.sp, textAlign = TextAlign.Center, color = Color(0xFF22261F)) }
            }
            // Gießkanne
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width * canX; val cy = size.height * 0.88f
                val w = 70.dp.toPx(); val h = 40.dp.toPx()
                drawRoundRect(Water, Offset(cx - w / 2, cy - h / 2), Size(w, h), androidx.compose.ui.geometry.CornerRadius(12f))
                drawArc(Color(0xFF2F78AD), 180f, 180f, false, Offset(cx - w * 0.3f, cy - h), Size(w * 0.6f, h), style = androidx.compose.ui.graphics.drawscope.Stroke(6f))
                rotate(-30f, Offset(cx - w / 2, cy)) { drawRect(Color(0xFF2F78AD), Offset(cx - w / 2 - w * 0.45f, cy - 6f), Size(w * 0.5f, 12f)) }
            }
            ParticleLayer(particles, Modifier.matchParentSize())
        }
    }
}

private val Water = Color(0xFF4F9FD6)

// ---------------------------------------------------------------- Sprechen


/** Antwort laut sagen. Fremdsprachen werden in ihrer Sprache erkannt – Aussprache-Training. */
@Composable
fun SpeakGame(card: StudyCard, options: List<String>?, locale: Locale, onResult: (GameResult) -> Unit) {
    val sfx = LocalSfx.current
    val scope = rememberCoroutineScope()
    var heard by remember(card.id) { mutableStateOf<String?>(null) }
    var tries by remember(card.id) { mutableIntStateOf(0) }
    var typing by remember(card.id) { mutableStateOf(false) }
    val pulse = rememberInfiniteTransition(label = "mic").animateFloat(1f, 1.12f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "p")

    val ask = LocalHost.current?.rememberSpeechPrompt { results ->
        if (results.isEmpty()) return@rememberSpeechPrompt
        heard = results.first()
        tries++
        if (results.any { SpokenAnswer.matches(it, card.back) }) {
            sfx?.play(Sound.CORRECT)
            scope.launch { delay(700); onResult(if (tries == 1) GameResult.PERFECT else GameResult.SHAKY) }
        } else {
            sfx?.play(Sound.WRONG, volume = 0.6f)
            if (tries >= 2) scope.launch { delay(1200); onResult(GameResult.WRONG) }
        }
    }

    if (typing && options != null) { ChoiceGame(card, options, onResult); return }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        QuestionText(card.front)
        Text("🎤 Sag die Antwort laut!", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier.size(110.dp).graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }.clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFFFF8A65), Color(0xFFE53935)))),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = {
                sfx?.play(Sound.PLOP)
                if (ask == null) typing = true else runCatching { ask(locale, card.front) }.onFailure { typing = true }
            }, modifier = Modifier.fillMaxSize()) { Text("🎤", fontSize = 48.sp) }
        }
        heard?.let { h ->
            val ok = SpokenAnswer.matches(h, card.back)
            Text(
                if (ok) "Ich habe „$h“ gehört – richtig! 🎉" else if (tries >= 2) "„$h“ – richtig ist: ${card.back}" else "Ich habe „$h“ gehört. Versuch's noch mal!",
                textAlign = TextAlign.Center, fontWeight = FontWeight.Bold,
                color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
        if (options != null) TextButton(onClick = { typing = true }, modifier = Modifier.padding(top = 8.dp)) { Text("Lieber antippen") }
    }
}

/**
 * Antwort selbst eintippen (ab Klasse 5): Freies Erinnern ist die stärkste Form des Wiederholens.
 * Kleine Tippfehler zählen als „fast“ (Brummi zeigt die richtige Schreibweise) – außer bei Deutsch, wo die Schreibweise Lernstoff ist.
 * Zwei Versuche; „Weiß nicht“ deckt auf, ohne zu beschämen.
 */
@Composable
fun TypeGame(card: StudyCard, strictSpelling: Boolean, onResult: (GameResult) -> Unit) {
    val sfx = LocalSfx.current
    val scope = rememberCoroutineScope()
    var text by remember(card.id) { mutableStateOf("") }
    var tries by remember(card.id) { mutableIntStateOf(0) }
    var verdict by remember(card.id) { mutableStateOf<de.wissensgarten.core.model.TypedAnswer.Verdict?>(null) }
    var done by remember(card.id) { mutableStateOf(false) }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val shake = remember(card.id) { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(card.id) { delay(350); runCatching { focus.requestFocus() } }

    fun finish(r: GameResult) { if (done) return; done = true; keyboard?.hide(); scope.launch { delay(if (r == GameResult.WRONG) 1600 else 900); onResult(r) } }
    fun check() {
        if (done || text.isBlank()) return
        tries++
        val v = de.wissensgarten.core.model.TypedAnswer.check(text, card.back, strictSpelling)
        verdict = v
        when (v) {
            de.wissensgarten.core.model.TypedAnswer.Verdict.RIGHT -> { sfx?.play(Sound.CORRECT); finish(if (tries == 1) GameResult.PERFECT else GameResult.SHAKY) }
            de.wissensgarten.core.model.TypedAnswer.Verdict.ALMOST -> { sfx?.play(Sound.CORRECT, volume = 0.6f); finish(GameResult.SHAKY) }
            de.wissensgarten.core.model.TypedAnswer.Verdict.WRONG -> {
                sfx?.play(Sound.WRONG, volume = 0.6f)
                scope.launch { for (x in listOf(14f, -12f, 8f, -4f, 0f)) shake.animateTo(x, androidx.compose.animation.core.tween(45)) }
                if (tries >= 2) finish(GameResult.WRONG)
            }
        }
    }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        QuestionText(card.front)
        Text("⌨️ Tipp die Antwort ein – ganz aus dem Kopf!", style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = text, onValueChange = { if (!done) text = it.take(40) },
            modifier = Modifier.fillMaxWidth().focusRequester(focus).graphicsLayer { translationX = shake.value },
            singleLine = true, enabled = !done, textStyle = MaterialTheme.typography.titleLarge,
            label = { Text(if (tries == 0) "Deine Antwort" else "Noch ein Versuch") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Done, autoCorrectEnabled = false),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { check() }),
        )
        verdict?.let { v ->
            val msg = when {
                v == de.wissensgarten.core.model.TypedAnswer.Verdict.RIGHT -> "Genau richtig! 🎉"
                v == de.wissensgarten.core.model.TypedAnswer.Verdict.ALMOST -> "Fast! Ganz genau: ${card.back}"
                done -> "Richtig ist: ${card.back}"
                strictSpelling && de.wissensgarten.core.model.TypedAnswer.check(text, card.back) != de.wissensgarten.core.model.TypedAnswer.Verdict.WRONG ->
                    "Ganz knapp – achte auf die Schreibweise!"
                else -> "Noch nicht – versuch's noch mal!"
            }
            Text(msg, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold,
                color = if (v == de.wissensgarten.core.model.TypedAnswer.Verdict.WRONG) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp))
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = { if (!done) { verdict = de.wissensgarten.core.model.TypedAnswer.Verdict.WRONG; tries = 2; finish(GameResult.WRONG) } }, enabled = !done) {
                Text("Weiß ich nicht")
            }
            JuicyButton(onClick = { check() }, enabled = !done && text.isNotBlank()) { Text("Prüfen ✓") }
        }
    }
}
