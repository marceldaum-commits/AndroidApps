package de.kurvenflug.app.ui.flight

import de.kurvenflug.app.ui.fx.rememberBuzzer
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kurvenflug.app.ui.fx.*
import de.kurvenflug.app.ui.theme.Q
import de.kurvenflug.app.ui.volt.Volt
import de.kurvenflug.app.ui.volt.VoltMood
import de.kurvenflug.core.model.Level
import de.kurvenflug.core.model.Step
import de.kurvenflug.core.model.num
import kotlinx.coroutines.launch

/**
 * Rechenweg mit Volt: Schritt für Schritt, jedes Zwischenergebnis tippt das Kind selbst an.
 * Die ersten [voltSolves] Schritte rechnet Volt vor (beim ersten Rechenweg einer Bahnart) – danach rechnet das Kind allein.
 * Den Regler stellt das Kind am Ende selbst ein; Volt sagt nur, welche Zahl wohin gehört.
 */
@Composable
fun WalkthroughOverlay(level: Level, steps: List<Step>, voltSolves: Int, onClose: () -> Unit) {
    val sfx = LocalSfx.current
    val view = rememberBuzzer()
    val vib = LocalVibration.current
    var current by rememberSaveable(level.id) { mutableIntStateOf(voltSolves.coerceIn(0, steps.size - 1)) }
    var wrongPick by remember { mutableStateOf<Int?>(null) }
    val shake = remember { Animatable(0f) }
    val pop = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val done = current >= steps.size
    val settings = steps.mapNotNull { s -> s.sets?.let { k -> "${level.params.first { it.key == k }.label} = ${s.answerText}" } }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(remember { MutableInteractionSource() }, null) {}) {
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().systemBarsPadding().padding(10.dp)
                .clip(RoundedCornerShape(24.dp)).background(Q.Panel).border(2.dp, Q.Violet, RoundedCornerShape(24.dp))
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("🧮 Rechenweg mit Volt", fontWeight = FontWeight.Black, color = Q.Text, fontSize = 20.sp)
            Volt(
                if (done) VoltMood.WOW else if (wrongPick != null) VoltMood.OOPS else VoltMood.THINK, size = 48.dp,
                speech = when {
                    done -> "Super gerechnet! 🎉 Jetzt stell ${settings.joinToString(" und ")} ein und starte!"
                    wrongPick != null -> "Fast! Schau dir die Rechnung noch mal genau an. 🔍"
                    current < voltSolves || (current == voltSolves && voltSolves > 0) -> "Den ersten Schritt habe ich dir vorgerechnet. Jetzt du! 💪"
                    else -> "Tipp die richtige Zahl an. Du schaffst das! 💪"
                },
            )
            steps.forEachIndexed { i, s ->
                when {
                    i < current -> SolvedStep(s, byVolt = i < voltSolves)
                    i == current -> {
                        Column(
                            Modifier.fillMaxWidth().graphicsLayer { translationX = shake.value; scaleX = pop.value; scaleY = pop.value }
                                .clip(RoundedCornerShape(16.dp)).background(Q.Violet.copy(alpha = 0.18f)).padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("Schritt ${i + 1} von ${steps.size}", style = MaterialTheme.typography.labelSmall, color = Q.Muted)
                            Text(s.text, color = Q.Text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                s.choices.forEachIndexed { ci, c ->
                                    val right = num(c) == s.answerText
                                    NeonButton(
                                        onClick = {
                                            if (right) {
                                                sfx?.play(Sound.HIT); view.buzz(Buzz.HIT, vib); wrongPick = null
                                                scope.launch { pop.snapTo(1.12f); pop.animateTo(1f, spring(dampingRatio = 0.4f)) }
                                                current++
                                                if (current >= steps.size) sfx?.play(Sound.WIN)
                                            } else {
                                                sfx?.play(Sound.WRONG); view.buzz(Buzz.TICK, vib); wrongPick = ci
                                                scope.launch {
                                                    for (x in listOf(18f, -14f, 10f, -6f, 0f)) shake.animateTo(x, tween(45))
                                                }
                                            }
                                        },
                                        color = if (wrongPick == ci) Q.Orange else Q.Cyan,
                                        modifier = Modifier.weight(1f),
                                    ) { Text(num(c), fontWeight = FontWeight.Black, fontSize = 18.sp) }
                                }
                            }
                        }
                    }
                    else -> Text("Schritt ${i + 1}: …", color = Q.Muted.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                }
            }
            AnimatedVisibility(done, enter = fadeIn() + scaleIn(initialScale = 0.7f, animationSpec = spring(dampingRatio = 0.5f))) {
                NeonButton(onClick = onClose, color = Q.Green, modifier = Modifier.fillMaxWidth()) {
                    Text("🚀 Los geht's – Regler einstellen", fontWeight = FontWeight.Black)
                }
            }
            if (!done) Text("Schließen", color = Q.Muted, modifier = Modifier.align(Alignment.End)
                .clickable { sfx?.play(Sound.TAP); onClose() }.padding(6.dp))
        }
    }
}

@Composable
private fun SolvedStep(s: Step, byVolt: Boolean) {
    val (before, after) = s.text.substringBefore("?") to s.text.substringAfter("?", "")
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Q.Green.copy(alpha = 0.12f)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(if (byVolt) "🤖" else "✅", fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Text(buildAnnotatedString {
            append(before)
            withStyle(SpanStyle(color = Q.Green, fontWeight = FontWeight.Black)) { append(s.answerText) }
            append(after)
        }, color = Q.Text)
    }
}
