package de.wissensgarten.app.ui.duel

import de.wissensgarten.app.ui.common.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.app.ui.fx.*
import de.wissensgarten.core.model.Duel
import de.wissensgarten.core.model.DuelResult
import de.wissensgarten.core.model.Duels
import de.wissensgarten.core.model.Pack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val RightGreen = Color(0xFFBFE3AE)
private val WrongOrange = Color(0xFFFFCBB0)

/** Das Duell selbst: Countdown, 10 Fragen auf Zeit, danach Vergleich und Teilen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuelPlayScreen(
    duel: Duel,
    pack: Pack,
    myName: String,
    onFinish: (correct: Int, millis: Long, score: Int) -> Pair<Duel, Int>,
    onClose: () -> Unit,
) {
    val questions = remember(duel.seed, pack.id) { Duels.questions(pack, duel.seed) }
    val sfx = LocalSfx.current
    val scope = rememberCoroutineScope()
    val particles = rememberParticles()
    val shake = rememberShake()
    val share = rememberShareDuel()
    val context = Unit
    var countdown by remember { mutableIntStateOf(3) }
    var index by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<String?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var totalMs by remember { mutableLongStateOf(0L) }
    var result by remember { mutableStateOf<Pair<Duel, Int>?>(null) }
    val timer = remember { Animatable(1f) }
    val toBeat = duel.ranking.firstOrNull { !it.name.equals(myName, true) }

    BackHandler { onClose() }

    // Countdown 3-2-1
    LaunchedEffect(Unit) {
        while (countdown > 0) { sfx?.play(Sound.TAP, pitch = 1.5f); delay(700); countdown-- }
        sfx?.play(Sound.PLOP)
    }

    fun answer(o: String?, elapsed: Long) {
        if (picked != null || result != null) return
        val q = questions[index]
        picked = o ?: "\u0000"
        totalMs += elapsed
        val right = o == q.answer
        if (right) {
            correct++
            score += Duels.points(true, elapsed)
            sfx?.play(Sound.CORRECT, pitch = 1f + 0.05f * correct)
            particles.burst(Burst.SPARKLE, 0.5f, 0.55f, 24)
        } else {
            sfx?.play(Sound.WRONG)
            scope.launch { shake.shake() }
        }
        scope.launch {
            delay(if (right) 700 else 1300)
            if (index + 1 < questions.size) { picked = null; index++ } else {
                result = onFinish(correct, totalMs, score)
            }
        }
    }

    // Zeit pro Frage
    LaunchedEffect(index, countdown) {
        if (countdown > 0 || result != null) return@LaunchedEffect
        timer.snapTo(1f)
        timer.animateTo(0f, tween(Duels.SECONDS * 1000, easing = LinearEasing))
        if (picked == null) answer(null, Duels.SECONDS * 1000L)
    }

    Box(Modifier.fillMaxSize()) {
        when {
            result != null -> DuelResultView(result!!.first, pack, myName, result!!.second, onClose) {
                val me = result!!.first.results.first { it.name.equals(myName, true) }
                share(context, Duels.shareText(result!!.first, pack.title, me))
            }
            countdown > 0 -> Countdown(countdown, pack, toBeat)
            else -> Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text("${pack.emoji} ${pack.title}", style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                Text("Frage ${index + 1} von ${questions.size}", style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Abbrechen") } },
                        actions = {
                            Text("${animatedCount(score, 500)}", fontWeight = FontWeight.Black, fontSize = 22.sp, color = DuelOrange, modifier = Modifier.padding(end = 16.dp))
                        },
                    )
                },
            ) { pad ->
                val q = questions[index]
                Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
                    LinearProgressIndicator(
                        progress = { timer.value },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                        color = if (timer.value < 0.3f) Color(0xFFE53935) else DuelOrange,
                    )
                    toBeat?.let {
                        Text(
                            "Zu schlagen: ${it.avatar} ${it.name} – ${it.score} Punkte",
                            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                    AnimatedContent(index, transitionSpec = {
                        (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
                    }, label = "frage") { i ->
                        Text(questions[i].question, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(24.dp))
                    Column(Modifier.shake(shake)) {
                        q.options.forEach { o ->
                            val bg = when {
                                picked == null -> MaterialTheme.colorScheme.surfaceContainerLow
                                o == q.answer -> RightGreen
                                o == picked -> WrongOrange
                                else -> MaterialTheme.colorScheme.surfaceContainerLow
                            }
                            OutlinedButton(
                                onClick = { answer(o, ((1f - timer.value) * Duels.SECONDS * 1000).toLong()) },
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = bg, contentColor = if (picked != null && (o == q.answer || o == picked)) Color(0xFF22261F) else MaterialTheme.colorScheme.onSurface),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp).padding(vertical = 4.dp),
                            ) { Text(o, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center) }
                        }
                    }
                    if (picked == "\u0000") Text("⏰ Zeit um! Richtig: ${q.answer}", color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }
            }
        }
        ParticleLayer(particles, Modifier.matchParentSize())
    }
}

@Composable
private fun Countdown(n: Int, pack: Pack, toBeat: DuelResult?) {
    val scale = remember(n) { Animatable(2f) }
    LaunchedEffect(n) { scale.animateTo(1f, spring(dampingRatio = 0.4f)) }
    Column(
        Modifier.fillMaxSize().background(de.wissensgarten.app.ui.fx.SafeBrush.verticalGradient(listOf(Color(0xFFFFB74D), DuelOrange))).padding(24.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("${pack.emoji} ${pack.title}", color = Color.White, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        toBeat?.let { Text("Gegen ${it.avatar} ${it.name}: ${it.score} Punkte", color = Color.White, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(40.dp))
        Text("$n", fontSize = 120.sp, fontWeight = FontWeight.Black, color = Color.White,
            modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value })
        Text("Mach dich bereit!", color = Color.White, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun DuelResultView(duel: Duel, pack: Pack, myName: String, dew: Int, onClose: () -> Unit, onShare: () -> Unit) {
    val sfx = LocalSfx.current
    val particles = rememberParticles()
    val ranking = duel.ranking
    val myRank = ranking.indexOfFirst { it.name.equals(myName, true) }
    val won = myRank == 0
    val alone = ranking.size == 1
    LaunchedEffect(Unit) {
        if (won || alone) { sfx?.play(Sound.FANFARE); particles.burst(Burst.CONFETTI, count = 110) } else sfx?.play(Sound.GROW)
    }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                when { alone -> "Geschafft! 💪"; won -> "Du gewinnst! 🏆"; else -> "Platz ${myRank + 1} – Revanche? 😤" },
                style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center,
            )
            Text("${pack.emoji} ${pack.title}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Podium(ranking.take(3), myName)
            Spacer(Modifier.height(16.dp))
            ranking.drop(3).forEachIndexed { i, r ->
                Text("${i + 4}. ${r.avatar} ${r.name} – ${r.score}", style = MaterialTheme.typography.bodyLarge)
            }
            if (dew > 0) Text("+$dew 💧 Tautropfen", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(20.dp))
            Text(
                if (alone) "Schick den Code an Familie oder Freunde – wer schafft mehr?" else "Schick den neuen Stand zurück, damit alle ihn sehen.",
                textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            JuicyButton(
                onClick = onShare, modifier = Modifier.fillMaxWidth().height(58.dp).pulse(true, 0.03f),
                colors = ButtonDefaults.buttonColors(containerColor = DuelOrange),
            ) { Text(if (alone) "Herausfordern 📤" else "Ergebnis teilen 📤", fontWeight = FontWeight.Bold, fontSize = 17.sp) }
            TextButton(onClick = onClose, modifier = Modifier.padding(top = 8.dp)) { Text("Fertig") }
        }
        ParticleLayer(particles, Modifier.matchParentSize())
    }
}

@Composable
private fun Podium(top: List<DuelResult>, myName: String) {
    // Reihenfolge auf dem Podest: 2 – 1 – 3
    val order = listOfNotNull(top.getOrNull(1)?.let { it to 2 }, top.getOrNull(0)?.let { it to 1 }, top.getOrNull(2)?.let { it to 3 })
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
        order.forEach { (r, place) ->
            val h = when (place) { 1 -> 120.dp; 2 -> 90.dp; else -> 70.dp }
            val rise = remember { Animatable(0f) }
            LaunchedEffect(Unit) { delay(place * 200L); rise.animateTo(1f, spring(dampingRatio = 0.5f)) }
            Column(Modifier.width(104.dp).padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(r.avatar, fontSize = 38.sp)
                Text(r.name, fontWeight = if (r.name.equals(myName, true)) FontWeight.Black else FontWeight.Medium, maxLines = 1)
                Text("${r.score}", fontWeight = FontWeight.Bold, color = DuelOrange)
                Text("${r.correct}/${Duels.QUESTIONS} · ${r.millis / 1000}s", style = MaterialTheme.typography.labelSmall)
                Box(
                    Modifier.fillMaxWidth().height(h * rise.value).clip(MaterialTheme.shapes.small)
                        .background(when (place) { 1 -> Color(0xFFFFD54F); 2 -> Color(0xFFCFD8DC); else -> Color(0xFFFFAB91) }),
                    contentAlignment = Alignment.TopCenter,
                ) { Text("$place", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.padding(top = 6.dp)) }
            }
        }
    }
}
