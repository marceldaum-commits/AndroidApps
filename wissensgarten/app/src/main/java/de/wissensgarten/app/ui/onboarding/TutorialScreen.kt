package de.wissensgarten.app.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.wissensgarten.app.ui.common.Plant
import de.wissensgarten.app.ui.common.formatDays
import de.wissensgarten.core.model.Memory
import de.wissensgarten.core.model.Rating
import de.wissensgarten.core.model.Scheduler
import de.wissensgarten.core.model.Stage
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Interaktive Anleitung: Samen säen → keimen → Zeitraffer zeigt Welken → gießen → wachsen.
 * Die Pflanze rechnet mit der echten Vergessenskurve, damit man das Prinzip wirklich sieht.
 */
@Composable
fun TutorialScreen(onDone: () -> Unit) {
    val start = remember { System.currentTimeMillis() }
    var step by remember { mutableIntStateOf(0) }
    var memory by remember { mutableStateOf(Memory()) }
    var days by remember { mutableFloatStateOf(0f) }
    var answered by remember { mutableStateOf<String?>(null) }
    var beforeStability by remember { mutableDoubleStateOf(0.0) }
    val grow = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val now = start + (days * Scheduler.DAY_MS).toLong()

    fun growTo(m: Memory) {
        memory = m
        scope.launch { grow.snapTo(0.5f); grow.animateTo(1f, tween(700)) }
    }

    Scaffold { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Anleitung ${step + 1}/5", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onDone) { Text("Überspringen") }
            }
            Plant(
                Stage.of(memory), Scheduler.wilt(memory, now), 330f, 5,
                Modifier.fillMaxWidth(0.7f).aspectRatio(0.9f), grow.value,
            )
            if (!memory.isSeed) {
                val r = Scheduler.retrievability(memory, now)
                Text(
                    "Erinnerung: ${(r * 100).roundToInt()} %",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (r < Scheduler.TARGET) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                )
                LinearProgressIndicator(
                    progress = { r.toFloat() }, Modifier.fillMaxWidth(0.7f).height(10.dp).padding(top = 4.dp),
                    color = if (r < Scheduler.TARGET) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(20.dp))

            when (step) {
                0 -> Explain(
                    "Jede Karte ist ein Samen 🌰",
                    "In deinem Garten wächst Wissen. Jede Frage, die du lernst, ist eine eigene Pflanze.",
                ) { Button(onClick = { step = 1 }, Modifier.fillMaxWidth().height(54.dp)) { Text("Ersten Samen säen") } }

                1 -> Explain("Probier's aus!", "Wie viele Beine hat eine Spinne?") {
                    listOf("6", "8", "10").forEach { o ->
                        val color = when {
                            answered == null -> ButtonDefaults.outlinedButtonColors()
                            o == "8" -> ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFCDE8BE))
                            o == answered -> ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFFDCC4))
                            else -> ButtonDefaults.outlinedButtonColors()
                        }
                        OutlinedButton(
                            onClick = {
                                if (answered == null) {
                                    answered = o
                                    growTo(Scheduler.review(memory, if (o == "8") Rating.GOOD else Rating.AGAIN, start))
                                }
                            },
                            colors = color,
                            modifier = Modifier.fillMaxWidth().height(54.dp).padding(vertical = 3.dp),
                        ) { Text(o, style = MaterialTheme.typography.titleMedium) }
                    }
                    if (answered != null) {
                        Text(
                            if (answered == "8") "Richtig! Dein Samen keimt. 🌱" else "Fast – es sind 8. Auch so keimt der Samen, nur schwächer.",
                            textAlign = TextAlign.Center, modifier = Modifier.padding(8.dp),
                        )
                        Button(onClick = { step = 2 }, Modifier.fillMaxWidth().height(54.dp)) { Text("Weiter") }
                    }
                }

                2 -> Explain(
                    "Wissen welkt ⏳",
                    "Ohne Wiederholung vergisst man – das ist ganz normal. Schieb die Zeit vor und schau, was passiert:",
                ) {
                    Text("${days.roundToInt()} Tage später", style = MaterialTheme.typography.titleMedium)
                    Slider(days, { days = it }, valueRange = 0f..30f, modifier = Modifier.fillMaxWidth())
                    val thirsty = Scheduler.isThirsty(memory, now)
                    if (thirsty) {
                        Text(
                            "Jetzt hat die Pflanze Durst – das ist der beste Moment zum Wiederholen! " +
                                "Laut Vergessenskurve ist das nach ${formatDays(memory.stability)} der Fall.",
                            textAlign = TextAlign.Center,
                        )
                        Button(onClick = { step = 3 }, Modifier.fillMaxWidth().height(54.dp)) { Text("Weiter") }
                    }
                }

                3 -> Explain(
                    "Gießen = Wiederholen 💧",
                    "Wiederholst du, wenn die Pflanze Durst hat, wächst sie – und hält danach viel länger.",
                ) {
                    Button(
                        onClick = {
                            beforeStability = memory.stability
                            growTo(Scheduler.review(memory, Rating.GOOD, now))
                            step = 4
                        },
                        Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    ) { Text("Gießen") }
                }

                else -> Explain(
                    "Sie wächst! 🌿",
                    "Vorher hielt das Wissen ${formatDays(beforeStability)}, jetzt etwa ${formatDays(memory.stability)}. " +
                        "Mit jedem rechtzeitigen Gießen wird aus dem Keimling ein Busch, eine Blüte und schließlich ein Baum – " +
                        "dann sitzt das Wissen für Monate.\n\nKein Stress: Welke Pflanzen kann man jederzeit retten.",
                ) { Button(onClick = onDone, Modifier.fillMaxWidth().height(54.dp)) { Text("Zu meinem Garten") } }
            }
        }
    }
}

@Composable
private fun Explain(title: String, text: String, actions: @Composable ColumnScope.() -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        actions()
    }
}
