package de.wissensgarten.app.ui.play

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.app.ui.fx.*
import de.wissensgarten.core.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/** Kopfzeile mit Schließen-Knopf für die Extra-Spiele. */
@Composable
private fun GameTop(title: String, sub: String, onClose: () -> Unit, color: Color = MaterialTheme.colorScheme.onBackground) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Schließen", tint = color) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = color)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = color.copy(alpha = 0.8f))
        }
    }
}

/** Antwortknöpfe, die nacheinander hereinploppen. */
@Composable
private fun OptionButtons(options: List<String>, enabled: Boolean, highlight: Int? = null, reveal: String? = null, onPick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, o ->
            var shown by remember(options) { mutableStateOf(false) }
            LaunchedEffect(options) { delay(60L * i); shown = true }
            val s by animateFloatAsState(if (shown) (if (highlight == i) 1.08f else 1f) else 0.6f, spring(0.45f, 400f), label = "s")
            val bg = when {
                reveal != null && o == reveal -> Color(0xFF81C784)
                highlight == i -> Color(0xFFFFD54F)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            Button(
                onClick = { onPick(i) }, enabled = enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).graphicsLayer { scaleX = s; scaleY = s; alpha = if (shown) 1f else 0f },
                colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = Color(0xFF22261F),
                    disabledContainerColor = bg, disabledContentColor = Color(0xFF22261F)),
                shape = RoundedCornerShape(18.dp),
            ) { Text(o, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center) }
        }
    }
}

// ───────────────────────────── Unkraut-Jäten ─────────────────────────────

/** Ein freches Unkraut-Monster: zackige Blätter, Kulleraugen, wackelt. [pull] 0..1 zieht es aus der Erde. */
fun DrawScope.drawWeed(t: Float, pull: Float, loose: Boolean) {
    val w = size.width; val h = size.height
    val ground = h * 0.78f
    val lift = pull * h * 0.55f
    val wob = sin(t * 2 * PI.toFloat() * (if (loose) 3f else 1f)) * (if (loose) 10f else 5f)
    // Erde
    drawOval(Color(0xFF6D4C41), Offset(w * 0.18f, ground - 18f), Size(w * 0.64f, 44f))
    // Wurzeln (sichtbar beim Ziehen)
    if (pull > 0.05f) for (k in -2..2) {
        val p = Path().apply { moveTo(w / 2 + k * 8f, ground - lift); quadraticTo(w / 2 + k * 22f, ground - lift * 0.4f + 20f, w / 2 + k * 30f, ground + 10f - lift * 0.2f) }
        drawPath(p, Color(0xFFA1887F), style = Stroke(4f))
    }
    val cx = w / 2 + wob; val base = ground - lift
    // Blätter
    for (k in 0 until 7) {
        val a = (-PI / 2 + (k - 3) * 0.42).toFloat()
        val len = h * (0.36f + (k % 2) * 0.08f)
        val tip = Offset(cx + kotlin.math.cos(a) * len, base + sin(a) * len)
        val p = Path().apply {
            moveTo(cx - 10f, base); quadraticTo(cx + (tip.x - cx) * 0.4f - 18f, base + (tip.y - base) * 0.5f, tip.x, tip.y)
            quadraticTo(cx + (tip.x - cx) * 0.4f + 18f, base + (tip.y - base) * 0.5f, cx + 10f, base); close()
        }
        drawPath(p, if (k % 2 == 0) Color(0xFF558B2F) else Color(0xFF7CB342))
    }
    // Gesicht
    val fy = base - h * 0.16f
    drawCircle(Color(0xFF689F38), h * 0.12f, Offset(cx, fy))
    listOf(-1, 1).forEach { s ->
        drawCircle(Color.White, h * 0.035f, Offset(cx + s * h * 0.045f, fy - 4f))
        drawCircle(Color.Black, h * 0.018f, Offset(cx + s * h * 0.045f + (if (loose) s * 3f else 0f), fy - 2f))
    }
    val mouth = Path().apply {
        moveTo(cx - 14f, fy + 14f)
        if (loose) quadraticTo(cx, fy + 2f, cx + 14f, fy + 14f) else quadraticTo(cx, fy + 26f, cx + 14f, fy + 14f)
    }
    drawPath(mouth, Color(0xFF33691E), style = Stroke(4f))
}

@Composable
fun WeedScreen(cards: List<StudyCard>, peers: Map<String, List<String>>, total: Int,
               onRight: (StudyCard) -> Unit, onWrong: (StudyCard) -> Unit, onClose: () -> Unit) {
    val list = remember { cards.take(Weeds.PER_ROUND) }
    var index by remember { mutableIntStateOf(0) }
    var pulled by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(0) } // 0 Frage, 1 ziehen, 2 falsch
    var pull by remember { mutableFloatStateOf(0f) }
    val sfx = LocalSfx.current
    val haptics = LocalHapticFeedback.current
    val vib = LocalVibration.current
    val particles = rememberParticles()
    val scope = rememberCoroutineScope()
    val t by rememberInfiniteTransition(label = "u").animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "t")
    BackHandler { onClose() }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFDFF3E4), Color(0xFFF6EFD9))))) {
        Column(Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 16.dp)) {
            GameTop("🌿 Unkraut-Jäten", "Richtig antworten – dann rausziehen!", onClose)
            if (index >= list.size) {
                WeedDone(pulled, list.size, total + pulled, onClose)
                return@Column
            }
            val card = list[index]
            val options = remember(card.id) { Games.options(card, peers[card.bedId].orEmpty()) ?: listOf(card.back) }
            LinearProgressIndicator(progress = { index / list.size.toFloat() }, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
            Box(Modifier.fillMaxWidth().height(220.dp).pointerInput(phase) {
                if (phase == 1) detectVerticalDragGestures(onDragEnd = {
                    if (pull < 0.85f) scope.launch { animate(pull, 0f, animationSpec = spring(0.5f)) { v, _ -> pull = v } }
                }) { _, dy ->
                    if (pull >= 1f) return@detectVerticalDragGestures
                    pull = (pull - dy / 500f).coerceIn(0f, 1f)
                    if (pull >= 1f) {
                        sfx?.play(Sound.PLOP); sfx?.play(Sound.CORRECT, volume = 0.6f)
                        if (vib) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        particles.burst(Burst.LEAVES, 0.5f, 0.4f, 40)
                        pulled++
                        onRight(card)
                        scope.launch { delay(900); pull = 0f; phase = 0; index++ }
                    }
                }
            }) {
                Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = if (pull >= 1f) 0.3f else 1f }) { drawWeed(t, pull, phase == 1) }
                if (phase == 1 && pull < 1f) Text("👆 Nach oben ziehen!", Modifier.align(Alignment.TopCenter).padding(top = 4.dp)
                    .graphicsLayer { translationY = -sin(t * 2 * PI.toFloat()) * 8f }, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF33691E))
                if (pull >= 1f) Text("🌸 Weg damit!", Modifier.align(Alignment.Center), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD81B60))
            }
            Text((card.img?.let { "$it  " } ?: "") + card.front, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
            when (phase) {
                0 -> OptionButtons(options, true) { i ->
                    if (options[i] == card.back) { sfx?.play(Sound.CORRECT); phase = 1 }
                    else { sfx?.play(Sound.WRONG, volume = 0.5f); phase = 2; onWrong(card) }
                }
                1 -> Text("Richtig! Jetzt pack es an den Blättern und zieh! 💪", Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                2 -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    OptionButtons(options, false, reveal = card.back) {}
                    Spacer(Modifier.height(10.dp))
                    Brummi(Mood.THINK, size = 64.dp, speech = "Das Unkraut ist zäh – bald probieren wir's nochmal!")
                    Button(onClick = { phase = 0; index++ }) { Text("Weiter ▶") }
                }
            }
        }
        ParticleLayer(particles, Modifier.fillMaxSize())
    }
}

@Composable
private fun WeedDone(pulled: Int, of: Int, totalPulled: Int, onClose: () -> Unit) {
    val flowers = totalPulled / Weeds.FLOWER_EVERY
    val toNext = Weeds.FLOWER_EVERY - totalPulled % Weeds.FLOWER_EVERY
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
        Brummi(if (pulled > 0) Mood.CHEER else Mood.HAPPY, size = 110.dp)
        Text(if (pulled == of) "Beet ist sauber! ✨" else "$pulled von $of Unkräutern gezupft", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text("🌺 Sonderblumen: $flowers · noch $toNext Unkraut bis zur nächsten", textAlign = TextAlign.Center)
        Text("Unkraut, das noch steht, verschwindet auch von allein, wenn die Karte sitzt.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Button(onClick = onClose) { Text("Zurück in den Garten") }
    }
}

// ───────────────────────────── Gewitter-Blitzrunde ─────────────────────────────

@Composable
fun BlitzScreen(pool: List<StudyCard>, peers: Map<String, List<String>>, onDone: (correct: Int) -> Unit, onClose: () -> Unit) {
    val deck = remember { pool.shuffled() }
    var started by remember { mutableStateOf(false) }
    var left by remember { mutableIntStateOf(Blitz.SECONDS) }
    var i by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var flash by remember { mutableFloatStateOf(0f) }
    var finished by remember { mutableStateOf(false) }
    val sfx = LocalSfx.current
    val particles = rememberParticles()
    val scope = rememberCoroutineScope()
    val shake = rememberShake()
    val bright by animateFloatAsState(Blitz.brightness(correct), tween(600), label = "b")
    val rain by rememberInfiniteTransition(label = "r").animateFloat(0f, 1f, infiniteRepeatable(tween(700, easing = LinearEasing)), label = "rain")
    BackHandler { onClose() }
    LaunchedEffect(started) {
        if (!started) return@LaunchedEffect
        while (left > 0) { delay(1000); left-- }
        finished = true; onDone(correct); sfx?.play(Sound.FANFARE)
        repeat(3) { particles.burst(Burst.CONFETTI, 0.5f, 0.3f, 40); delay(250) }
    }
    val sky = lerp(Color(0xFF263238), Color(0xFF81D4FA), bright)
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(sky, lerp(Color(0xFF455A64), Color(0xFFE1F5FE), bright))))) {
        // Regen + Blitz
        Canvas(Modifier.fillMaxSize()) {
            if (!finished) for (k in 0 until 60) {
                val x = (k * 73 % 100) / 100f * size.width
                val y = ((k * 37 % 100) / 100f + rain) % 1f * size.height
                drawLine(Color.White.copy(alpha = 0.35f * (1 - bright) + 0.1f), Offset(x, y), Offset(x - 6f, y + 26f), 3f)
            }
            if (flash > 0f) drawRect(Color.White.copy(alpha = flash))
            if (finished) { // Regenbogen
                listOf(0xFFE53935, 0xFFFB8C00, 0xFFFDD835, 0xFF43A047, 0xFF1E88E5, 0xFF8E24AA).forEachIndexed { k, c ->
                    drawArc(Color(c), 180f, 180f, false, Offset(size.width * 0.05f + k * 18f, size.height * 0.12f + k * 18f),
                        Size(size.width * 0.9f - k * 36f, size.width * 0.9f - k * 36f), style = Stroke(18f))
                }
            }
        }
        val fg = if (bright > 0.5f) Color(0xFF1B2A35) else Color.White
        Column(Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 16.dp)) {
            GameTop("⚡ Gewitter-Blitzrunde", if (started && !finished) "Noch $left Sekunden · $correct richtig" else "60 Sekunden – so viele wie möglich", onClose, fg)
            when {
                !started -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
                    Text("⛈️", fontSize = 80.sp)
                    Text("Jede richtige Antwort vertreibt ein Stück Gewitter. Falsche Antworten kosten nichts – einfach weiter!",
                        color = fg, textAlign = TextAlign.Center)
                    Button(onClick = { started = true; sfx?.play(Sound.TAP) }, modifier = Modifier.height(56.dp)) { Text("Los geht's! ⚡", fontSize = 20.sp) }
                }
                finished -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
                    Brummi(Mood.CHEER, size = 110.dp)
                    Text("$correct richtig! 🌈", style = MaterialTheme.typography.headlineMedium, color = fg)
                    Text("+${Blitz.dew(correct)} 💧 Tautropfen", color = fg)
                    Button(onClick = onClose) { Text("Zurück in den Garten") }
                }
                deck.isEmpty() -> Text("Noch keine gelernten Karten – erst ein bisschen gießen!", color = fg)
                else -> {
                    val card = deck[i % deck.size]
                    val options = remember(i) { Games.options(card, peers[card.bedId].orEmpty()) ?: listOf(card.back) }
                    Spacer(Modifier.weight(0.3f))
                    AnimatedContent(i, transitionSpec = { (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut()) }, label = "q") { k ->
                        val shown = deck[k % deck.size]
                        Text((shown.img?.let { "$it  " } ?: "") + shown.front, style = MaterialTheme.typography.titleLarge, color = fg,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().shake(shake))
                    }
                    Spacer(Modifier.height(16.dp))
                    OptionButtons(options, true) { k ->
                        if (options[k] == card.back) {
                            correct++; sfx?.play(Sound.CORRECT, pitch = 1f + (correct % 8) * 0.04f)
                            particles.burst(Burst.SPARKLE, 0.5f, 0.35f, 18)
                            scope.launch { flash = 0.6f; animate(0.6f, 0f, animationSpec = tween(300)) { v, _ -> flash = v } }
                        } else { sfx?.play(Sound.WRONG, volume = 0.3f); scope.launch { shake.shake() } }
                        i++
                    }
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        ParticleLayer(particles, Modifier.fillMaxSize())
    }
}

// ───────────────────────────── Rhythmus-Runde ─────────────────────────────

@Composable
fun RhythmScreen(pool: List<StudyCard>, peers: Map<String, List<String>>, onDone: (points: Int, right: Int) -> Unit, onClose: () -> Unit) {
    val deck = remember { pool.shuffled().take(Rhythm.QUESTIONS) }
    var i by remember { mutableIntStateOf(0) }
    var points by remember { mutableIntStateOf(0) }
    var right by remember { mutableIntStateOf(0) }
    var elapsed by remember { mutableLongStateOf(0L) }
    var verdict by remember { mutableStateOf<Rhythm.Hit?>(null) }
    var started by remember { mutableStateOf(false) }
    val sfx = LocalSfx.current
    val particles = rememberParticles()
    BackHandler { onClose() }
    val done = started && i >= deck.size
    LaunchedEffect(done) { if (done) { onDone(points, right); sfx?.play(Sound.FANFARE); repeat(3) { particles.burst(Burst.CONFETTI, 0.5f, 0.3f, 36); delay(250) } } }
    // Takt: Uhr für die aktuelle Frage + Klick auf jedem Schlag
    LaunchedEffect(i, started, verdict) {
        if (!started || done || verdict != null) return@LaunchedEffect
        val start = withInfiniteAnimationFrameMillis { it } - elapsed
        var lastBeat = -1L
        while (true) {
            val now = withInfiniteAnimationFrameMillis { it }
            elapsed = now - start
            val beat = elapsed / Rhythm.BEAT_MS
            if (beat != lastBeat) { lastBeat = beat; sfx?.play(Sound.TAP, pitch = if (beat % 4 == 0L) 1.3f else 1f, volume = 0.5f) }
        }
    }
    val beatPhase = (elapsed % Rhythm.BEAT_MS) / Rhythm.BEAT_MS.toFloat()
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF4A148C), Color(0xFF880E4F))))) {
        // pulsierende Lichter im Takt
        Canvas(Modifier.fillMaxSize()) {
            val pulse = 1f - beatPhase
            listOf(0.15f to 0.2f, 0.85f to 0.25f, 0.5f to 0.08f, 0.1f to 0.85f, 0.9f to 0.8f).forEachIndexed { k, (x, y) ->
                drawCircle(Color(listOf(0xFFFF4081, 0xFF40C4FF, 0xFFFFEB3B, 0xFF69F0AE, 0xFFE040FB)[k]).copy(alpha = 0.25f * pulse + 0.05f),
                    60f + 40f * pulse, Offset(size.width * x, size.height * y))
            }
        }
        Column(Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            GameTop("🎵 Rhythmus-Runde", if (started && !done) "Frage ${i + 1} von ${deck.size} · $points Punkte" else "Tippe im Takt!", onClose, Color.White)
            when {
                deck.isEmpty() -> Text("Noch keine gelernten Karten – erst ein bisschen gießen!", color = Color.White)
                !started -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
                    Text("🥁", fontSize = 80.sp)
                    Text("Die Antworten leuchten nacheinander im Takt auf. Tippe, wenn die richtige leuchtet – je genauer im Beat, desto mehr Punkte!",
                        color = Color.White, textAlign = TextAlign.Center)
                    Button(onClick = { started = true }, modifier = Modifier.height(56.dp)) { Text("Musik ab! 🎶", fontSize = 20.sp) }
                }
                done -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
                    Brummi(Mood.CHEER, size = 110.dp)
                    Text("$points Punkte! 🎉", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text("$right von ${deck.size} im Takt getroffen · +${Rhythm.dew(points)} 💧", color = Color.White)
                    Button(onClick = onClose) { Text("Zurück in den Garten") }
                }
                else -> {
                    val card = deck[i]
                    val options = remember(i) { (Games.options(card, peers[card.bedId].orEmpty()) ?: listOf(card.back)).take(4) }
                    val correctIdx = options.indexOf(card.back)
                    val lit = Rhythm.lit(elapsed, options.size)
                    Spacer(Modifier.height(16.dp))
                    Text((card.img?.let { "$it  " } ?: "") + card.front, style = MaterialTheme.typography.titleLarge, color = Color.White, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(20.dp))
                    OptionButtons(options, verdict == null, highlight = if (verdict == null) lit else null, reveal = if (verdict != null) card.back else null) { k ->
                        val h = Rhythm.judge(elapsed, k, correctIdx, options.size)
                        verdict = h; points += h.points
                        if (h != Rhythm.Hit.MISS) { right++; sfx?.play(Sound.CORRECT); particles.burst(Burst.SPARKLE, 0.5f, 0.5f, 24) }
                        else sfx?.play(Sound.WRONG, volume = 0.3f)
                    }
                    Spacer(Modifier.height(16.dp))
                    verdict?.let { v ->
                        val s by animateFloatAsState(1f, spring(0.4f, 300f), label = "v")
                        Text(v.label, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFEB3B), modifier = Modifier.graphicsLayer { scaleX = s; scaleY = s })
                        LaunchedEffect(v, i) { delay(900); verdict = null; elapsed = 0; i++ }
                    }
                }
            }
        }
        ParticleLayer(particles, Modifier.fillMaxSize())
    }
}
