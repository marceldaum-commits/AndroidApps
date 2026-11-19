package de.raumlicht.app.ui.level

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.raumlicht.app.ui.fx.Buzz
import de.raumlicht.app.ui.fx.LocalBuzzer
import de.raumlicht.app.ui.fx.LocalSfx
import de.raumlicht.app.ui.fx.LocalVibration
import de.raumlicht.app.ui.fx.NeonButton
import de.raumlicht.app.ui.fx.Sound
import de.raumlicht.app.ui.lux.Lux
import de.raumlicht.app.ui.lux.LuxMood
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.core.model.Ki
import de.raumlicht.core.model.Level
import de.raumlicht.core.model.Values
import kotlinx.coroutines.launch

/**
 * Der KI-Tutor, wie ihn die Aufgaben-Seite sieht (null = hier nicht verfügbar).
 * @param hasCode ein gültiger, nicht abgelaufener KI-Abi-Pass ist eingetragen (sonst laufen die Proben)
 * @param ask schickt eine Frage zur Aufgabe an den Server und liefert die Antwort (Status 0 = keine Verbindung)
 * @param open öffnet die Seite zum KI-Abi-Pass
 */
class KiTutor(val hasCode: Boolean, val ask: suspend (level: Level, values: Values, feedback: String, kind: Ki.Kind, question: String, history: List<Ki.Turn>) -> Ki.Answer, val open: () -> Unit)
val LocalKi = staticCompositionLocalOf<KiTutor?> { null }

private const val AGAIN = "Erklär es mir bitte noch einmal anders."

/**
 * „Erklär’s mir anders“: Karte von unten, in der Lux (mit KI-Hilfe) den Fehlversuch [attempt] (Eingabe und Rückmeldung der App) neu erklärt und Rückfragen beantwortet.
 * Die erste Erklärung startet beim Öffnen von selbst; der Wortwechsel bleibt stehen, solange man in der Aufgabe ist. Vorgelesen wird hier nichts.
 */
@Composable
fun KiSheet(open: Boolean, level: Level, attempt: Pair<Values, String>?, accent: Color, onClose: () -> Unit) {
    val ki = LocalKi.current ?: return
    val sfx = LocalSfx.current; val buzzer = LocalBuzzer.current; val vib = LocalVibration.current
    var turns by remember(level.id) { mutableStateOf(listOf<Ki.Turn>()) }
    var loading by remember(level.id) { mutableStateOf(false) }
    var problem by remember(level.id) { mutableStateOf<Int?>(null) }
    var rest by remember(level.id) { mutableStateOf<Int?>(null) }
    var input by remember(level.id) { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    fun ask(kind: Ki.Kind, question: String) {
        val (values, feedback) = attempt ?: return
        if (loading) return
        loading = true; problem = null
        val before = turns
        if (question.isNotEmpty()) turns = turns + Ki.Turn("schueler", question)
        sfx?.play(Sound.TICK)
        scope.launch {
            val a = ki.ask(level, values, feedback, kind, question, before)
            a.rest?.let { rest = it }
            if (a.status == 200 && a.text.isNotEmpty()) { turns = turns + Ki.Turn("lux", a.text); sfx?.play(Sound.HIT, 1.3f, 0.6f); buzzer.buzz(Buzz.TICK, vib) }
            else { turns = before; problem = if (a.status == 200) 500 else a.status; if (kind == Ki.Kind.QUESTION) input = question }
            loading = false
        }
    }
    LaunchedEffect(open) { if (open && turns.isEmpty() && !loading && problem == null) ask(Ki.Kind.EXPLAIN, "") }
    AnimatedVisibility(open, enter = fadeIn(tween(160)), exit = fadeOut(tween(160))) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(remember { MutableInteractionSource() }, null, onClick = onClose))
    }
    AnimatedVisibility(open, enter = slideInVertically(spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)) { it } + fadeIn(), exit = slideOutVertically(tween(180)) { it } + fadeOut()) {
        Box(Modifier.fillMaxSize().systemBarsPadding().imePadding(), contentAlignment = Alignment.BottomCenter) {
            Column(Modifier.widthIn(max = 620.dp).fillMaxWidth().fillMaxHeight(0.86f).padding(horizontal = 10.dp).padding(bottom = 10.dp).testTag("ki-karte")
                .background(Q.Deep, RoundedCornerShape(24.dp)).border(1.5.dp, accent.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                // Tipper auf die Karte schließen sie nicht (ohne die Karte für Vorlese-Hilfen zu einem einzigen Knopf zu verschmelzen)
                .pointerInput(Unit) { detectTapGestures { } }) {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🤖", fontSize = 28.sp)
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text("KI-TUTOR", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                        Text("Lux erklärt’s anders", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Q.Text)
                    }
                    Text("✕", Modifier.testTag("ki-zu").clip(CircleShape).clickable(onClickLabel = "KI-Tutor schließen", onClick = onClose).padding(12.dp), fontSize = 18.sp, color = Q.Muted)
                }
                val scroll = rememberScrollState()
                LaunchedEffect(turns.size, loading, problem) { scroll.animateScrollTo(scroll.maxValue) }
                Column(Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    turns.forEachIndexed { i, t ->
                        if (t.role == "lux") Bubble(i) { Row(verticalAlignment = Alignment.Top) {
                            Lux(LuxMood.HAPPY, size = 44.dp)
                            Text(t.text, Modifier.padding(start = 8.dp).weight(1f).testTag("ki-antwort").background(Q.Panel, RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)).padding(12.dp), style = MaterialTheme.typography.bodyMedium, color = Q.Text)
                        } } else Bubble(i) { Text(t.text, Modifier.fillMaxWidth().padding(start = 52.dp).background(accent.copy(alpha = 0.16f), RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp))
                            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)).padding(12.dp), style = MaterialTheme.typography.bodyMedium, color = Q.Text) }
                    }
                    if (loading) Row(Modifier.testTag("ki-denkt"), verticalAlignment = Alignment.CenterVertically) { Lux(LuxMood.THINK, size = 44.dp); Dots(accent, Modifier.padding(start = 12.dp)) }
                    problem?.let { p ->
                        Row(verticalAlignment = Alignment.Top) {
                            Lux(LuxMood.SAD, size = 44.dp)
                            Text(Ki.problem(p, ki.hasCode), Modifier.padding(start = 8.dp).weight(1f).testTag("ki-problem").background(Q.Panel, RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)).padding(12.dp), style = MaterialTheme.typography.bodyMedium, color = Q.Text)
                        }
                        // Proben oder Pass aufgebraucht: nur die sachliche Auskunft – kein Hinweis auf den Pass, kein Sprung zur Kaufseite (die KI-Seite bleibt in den Einstellungen)
                        if (p != 402 && p != 429) NeonButton({ if (turns.isEmpty()) ask(Ki.Kind.EXPLAIN, "") else problem = null }, Modifier.fillMaxWidth().testTag("ki-nochmal"), color = accent) { Text("Noch einmal versuchen", fontWeight = FontWeight.Bold) }
                    }
                }
                // Rückfrage stellen oder noch einmal anders erklären lassen
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val ready = !loading && turns.isNotEmpty() && problem != 402 && problem != 429
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(input, { input = it.take(Ki.MAX_QUESTION) }, Modifier.weight(1f).testTag("ki-frage"), placeholder = { Text("Deine Rückfrage …") }, maxLines = 3, enabled = ready)
                        NeonButton({ val q = input.trim(); input = ""; ask(Ki.Kind.QUESTION, q) }, Modifier.testTag("ki-senden"), color = accent, enabled = ready && input.isNotBlank()) { Text("➤", fontWeight = FontWeight.Black, fontSize = 18.sp) }
                    }
                    if (ready) Text("🔄 Noch mal anders erklären", Modifier.testTag("ki-anders").clip(RoundedCornerShape(10.dp)).background(Q.Space).border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                        .clickable { ask(Ki.Kind.EXPLAIN, AGAIN) }.padding(horizontal = 10.dp, vertical = 6.dp), color = Q.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text((rest?.let { n -> (if (ki.hasCode) "Heute noch $n KI-${if (n == 1) "Punkt" else "Punkte"}" else "Noch $n ${if (n == 1) "Probe" else "Proben"} frei") + " · " } ?: "") +
                        "Antworten schreibt eine KI – sie kann sich irren. Rechne mit.", Modifier.testTag("ki-rest"), color = Q.Muted, fontSize = 11.sp, lineHeight = 14.sp)
                }
            }
        }
    }
}

/** Jede neue Sprechblase ploppt herein */
@Composable
private fun Bubble(key: Int, content: @Composable () -> Unit) {
    val pop = remember(key) { Animatable(0.85f) }
    LaunchedEffect(key) { pop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) }
    Box(Modifier.fillMaxWidth().graphicsLayer { scaleX = pop.value; scaleY = pop.value; alpha = ((pop.value - 0.85f) / 0.15f).coerceIn(0f, 1f) }) { content() }
}

/** Drei hüpfende Punkte: Lux denkt nach */
@Composable
private fun Dots(color: Color, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition()
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { i ->
            val y by t.animateFloat(0f, 0f, infiniteRepeatable(keyframes { durationMillis = 900; 0f at i * 150; -7f at i * 150 + 150; 0f at i * 150 + 300 }, RepeatMode.Restart))
            Box(Modifier.size(9.dp).graphicsLayer { translationY = y * density }.background(color, CircleShape))
        }
    }
}
