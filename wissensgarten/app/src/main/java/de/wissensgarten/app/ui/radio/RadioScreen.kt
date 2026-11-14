package de.wissensgarten.app.ui.radio

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import de.wissensgarten.app.ui.fx.*
import de.wissensgarten.core.data.GardenRepository
import de.wissensgarten.core.model.Radio
import de.wissensgarten.core.model.Rating
import de.wissensgarten.core.model.StudyCard
import kotlinx.coroutines.delay
import java.util.Locale

enum class RadioPhase { READY, SPEAKING, LISTENING, DONE }

/**
 * Brummi-Radio: Fragen hören, laut antworten – der Bildschirm wird dunkel.
 * Für Auto, Schulweg oder vorm Einschlafen. Nichts wird aufgenommen oder gespeichert.
 */
@Composable
fun RadioScreen(
    cards: List<StudyCard>,
    peers: Map<String, List<String>>,
    subjects: Map<String, String?>,
    onRate: (StudyCard, Rating, Int) -> Unit,
    onClose: () -> Unit,
    /** nur für Bildschirmfotos: Prüfungen auf Mikrofon/Spracherkennung überspringen */
    preview: Boolean = false,
) {
    val ctx = LocalContext.current
    val voice = LocalVoice.current
    val sfx = LocalSfx.current
    val listener = remember { Listener(ctx) }
    var granted by remember { mutableStateOf(preview || ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    var phase by remember { mutableStateOf(RadioPhase.READY) }
    var caption by remember { mutableStateOf("") }
    var index by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var level by remember { mutableFloatStateOf(0f) }
    var running by remember { mutableStateOf(false) }
    var stopRequested by remember { mutableStateOf(false) }
    listener.onLevel = { level = it }

    // Bildschirm an lassen, aber ganz dunkel – das Handy kann in der Tasche oder im Auto liegen
    DisposableEffect(running) {
        val w = (ctx as? Activity)?.window
        if (running && w != null) {
            w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            w.attributes = w.attributes.apply { screenBrightness = 0.02f }
        }
        onDispose {
            w?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            w?.attributes = w?.attributes?.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE }
        }
    }
    DisposableEffect(Unit) { onDispose { voice?.stop(); listener.release() } }
    BackHandler { stopRequested = true; voice?.stop(); onClose() }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        suspend fun say(t: String, l: Locale = Locale.GERMAN) { phase = RadioPhase.SPEAKING; caption = t; voice?.speakAndWait(t, l) }
        say("Hallo, hier ist Brummi-Radio! Ich stelle dir Fragen – antworte einfach laut. Wenn du aufhören willst, sag: Stopp.")
        loop@ for ((i, card) in cards.withIndex()) {
            if (stopRequested) break
            index = i
            val q = Radio.question(card, peers[card.bedId].orEmpty())
            val subject = subjects[card.bedId]
            val answerLocale = Voice.localeFor(subject, card.back)
            var tries = 0
            while (true) {
                say(Radio.prompt(q), Voice.localeFor(subject, card.front).takeIf { q.mode == Radio.Mode.FREE } ?: Locale.GERMAN)
                if (q.mode == Radio.Mode.SELF) { delay(3500); say(Radio.selfReveal(q)) }
                if (stopRequested) break@loop
                sfx?.play(Sound.TAP, pitch = 1.6f, volume = 0.5f)
                phase = RadioPhase.LISTENING; caption = "Ich höre zu …"
                val heard = listener.listen(if (q.mode == Radio.Mode.FREE || q.mode == Radio.Mode.LEARN) answerLocale else Locale.GERMAN)
                when (val r = Radio.interpret(heard, q)) {
                    Radio.Reply.Stop -> break@loop
                    Radio.Reply.Repeat -> continue
                    Radio.Reply.Unclear -> {
                        if (++tries < 2) { say("Ich hab dich nicht verstanden. Nochmal!"); continue }
                        say(Radio.correction(q)); onRate(card, Rating.AGAIN, GardenRepository.baseDew(Rating.AGAIN)); break
                    }
                    else -> {
                        val rating = Radio.rating(r, q.mode) ?: Rating.AGAIN
                        val good = rating != Rating.AGAIN
                        if (good) { correct++; sfx?.play(Sound.CORRECT, volume = 0.7f) } else sfx?.play(Sound.PLOP, volume = 0.5f)
                        onRate(card, rating, GardenRepository.baseDew(rating))
                        when {
                            good && q.mode == Radio.Mode.LEARN -> say("Super nachgesprochen! Diese Pflanze ist gesät.")
                            good -> say(Radio.praise(i))
                            r == Radio.Reply.No -> say("Kein Problem – das üben wir bald wieder.")
                            q.mode == Radio.Mode.LEARN -> say("Hör nochmal: ${card.back}. Das üben wir bald wieder.")
                            else -> say(Radio.correction(q))
                        }
                        break
                    }
                }
            }
        }
        index = cards.size
        sfx?.play(Sound.FANFARE)
        say(Radio.summary(correct, cards.size))
        phase = RadioPhase.DONE
        running = false
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF0B1026))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🎧 Brummi-Radio", color = Color(0xFFFFE082), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Lernen mit den Ohren – Bildschirm darf aus bleiben", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
            // Fortschritt als Punkte
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 10.dp)) {
                cards.indices.forEach { i ->
                    Box(Modifier.size(if (i == index) 10.dp else 7.dp).clip(CircleShape)
                        .background(if (i < index) Color(0xFFFFD54F) else Color.White.copy(alpha = if (i == index) 0.9f else 0.25f)))
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Waves(phase == RadioPhase.LISTENING, level)
                Brummi(when (phase) {
                    RadioPhase.LISTENING -> Mood.THINK
                    RadioPhase.DONE -> Mood.CHEER
                    RadioPhase.SPEAKING -> Mood.HAPPY
                    else -> Mood.IDLE
                }, size = 150.dp)
            }
            Text(caption, color = Color.White.copy(alpha = 0.75f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge,
                maxLines = 4, modifier = Modifier.heightIn(min = 72.dp))
            Spacer(Modifier.height(16.dp))
            when {
                cards.isEmpty() -> { Text("Gerade ist nichts zu gießen. Komm später wieder! 🌱", color = Color.White, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp)); OutlinedButton(onClick = onClose) { Text("Zurück", color = Color.White) } }
                !preview && !listener.available() -> { Text("Auf diesem Handy ist keine Spracherkennung verfügbar.", color = Color.White, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp)); OutlinedButton(onClick = onClose) { Text("Zurück", color = Color.White) } }
                !granted -> {
                    Text("Damit Brummi deine Antworten hört, braucht er das Mikrofon. Es wird nichts aufgenommen oder gespeichert.",
                        color = Color.White, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    JuicyButton(onClick = { ask.launch(Manifest.permission.RECORD_AUDIO) }, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("🎤 Mikrofon erlauben") }
                }
                phase == RadioPhase.READY && !running -> JuicyButton(onClick = { running = true }, modifier = Modifier.fillMaxWidth().height(60.dp)) {
                    Text("▶️ Radio starten (${cards.size} Fragen)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                phase == RadioPhase.DONE -> JuicyButton(onClick = onClose, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Fertig – $correct von ${cards.size} richtig 🎉") }
                else -> OutlinedButton(onClick = { stopRequested = true; voice?.stop(); onClose() }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("⏹ Stopp", color = Color.White, fontSize = 18.sp)
                }
            }
        }
    }
}

/** Ringe um Brummi, die beim Zuhören mit der Stimme mitwachsen. */
@Composable
private fun Waves(active: Boolean, level: Float) {
    val t by rememberInfiniteTransition(label = "w").animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "t")
    val lv by animateFloatAsState(if (active) level else 0f, tween(120), label = "lv")
    Canvas(Modifier.size(300.dp)) {
        val base = size.minDimension * 0.28f
        for (k in 0 until 3) {
            val p = (t + k / 3f) % 1f
            val r = base + p * size.minDimension * 0.22f * (if (active) 1f else 0.5f) + lv * 30f
            drawCircle(Color(0xFFFFD54F).copy(alpha = (1f - p) * (if (active) 0.55f else 0.18f)), r, style = Stroke(3.dp.toPx()))
        }
    }
}

/** Einstieg im Garten. */
@Composable
fun RadioEntryCard(onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2147))) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🎧", fontSize = 30.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Brummi-Radio", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFFE082))
                Text("Lernen ohne Bildschirm – fürs Auto, den Schulweg oder vorm Einschlafen.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}
