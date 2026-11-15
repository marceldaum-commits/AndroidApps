package de.wissensgarten.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.SizeTransform
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.app.ui.common.*
import de.wissensgarten.app.ui.fx.*
import de.wissensgarten.core.model.Levels
import de.wissensgarten.core.model.Pack
import de.wissensgarten.core.model.Stage

private enum class Step { WELCOME, WHO, NAME, LEVEL, BEDS }

/**
 * Einrichtung: beim ersten Start komplett, beim Hinzufügen eines Familienmitglieds ohne Begrüßung.
 * Beete sind frei wählbar – die Klassenstufe hebt nur passende Pakete hervor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    packs: List<Pack>,
    addingProfile: Boolean,
    onCancel: () -> Unit,
    /** Auf der Willkommensseite: „Schon einen Garten? Wiederherstellen“ */
    welcomeExtra: @Composable () -> Unit = {},
    onFinish: (name: String, avatar: String, level: Int, packIds: Set<String>, ownBeds: List<Pair<String, String>>) -> Unit,
) {
    val steps = if (addingProfile) listOf(Step.NAME, Step.LEVEL, Step.BEDS) else Step.entries
    var index by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[index]
    var forChild by rememberSaveable { mutableStateOf(addingProfile) }
    var name by rememberSaveable { mutableStateOf("") }
    var avatar by rememberSaveable { mutableStateOf(AVATARS.first()) }
    var level by rememberSaveable { mutableIntStateOf(0) }
    var chosen by remember { mutableStateOf(setOf<String>()) }
    var ownBeds by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var showOwnForm by remember { mutableStateOf(false) }

    val back: () -> Unit = { if (index > 0) index-- else onCancel() }
    BackHandler(enabled = index > 0 || addingProfile) { back() }

    Scaffold(
        topBar = {
            if (step != Step.WELCOME) TopAppBar(
                title = { LinearProgressIndicator(progress = { (index + 1f) / steps.size }, Modifier.fillMaxWidth().height(8.dp)) },
                navigationIcon = {
                    IconButton(onClick = back) {
                        Icon(if (index == 0) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                },
            )
        },
        bottomBar = {
            val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
            val focus = androidx.compose.ui.platform.LocalFocusManager.current
            val total = chosen.size + ownBeds.size
            val (label, enabled) = when (step) {
                Step.WELCOME -> "Los geht's" to true
                Step.WHO -> "Weiter" to true
                Step.NAME -> "Weiter" to name.isNotBlank()
                Step.LEVEL -> "Weiter" to (level > 0)
                Step.BEDS -> (if (total == 0) "Mindestens ein Beet wählen" else "$total ${if (total == 1) "Beet" else "Beete"} anlegen") to (total > 0)
            }
            Surface(tonalElevation = 3.dp) {
                JuicyButton(
                    sound = if (step == Step.BEDS) Sound.LEVEL_UP else Sound.TAP,
                    onClick = {
                        // Tastatur vor dem Seitenwechsel schließen – sonst springt das Layout mitten in der Animation
                        keyboard?.hide(); focus.clearFocus()
                        if (step == Step.BEDS) onFinish(name, avatar, level, chosen, ownBeds)
                        else {
                            // Schneller Start: zwei passende Pakete vorauswählen (abwählbar) – ein Tipp bis zum Garten
                            if (step == Step.LEVEL && chosen.isEmpty()) chosen = recommendedPacks(packs, level)
                            index++
                        }
                    },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(58.dp),
                ) { Text(label, style = MaterialTheme.typography.titleMedium) }
            }
        },
    ) { pad ->
        AnimatedContent(
            targetState = step,
            // Ganze Seite schieben, alte Seite blendet schnell aus – nichts liegt übereinander.
            // Richtung folgt der Navigation (vor = von rechts, zurück = von links).
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                (slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { if (forward) it else -it } +
                    fadeIn(tween(220, delayMillis = 90))) togetherWith
                    (slideOutHorizontally(tween(340, easing = FastOutSlowInEasing)) { if (forward) -it else it } +
                        fadeOut(tween(120))) using SizeTransform(clip = true)
            },
            label = "onboarding",
            modifier = Modifier.padding(pad).fillMaxSize().clipToBounds(),
        ) { s ->
            when (s) {
                Step.WELCOME -> Welcome(welcomeExtra)
                Step.WHO -> Page("Wer lernt hier?", "Du kannst später weitere Familienmitglieder hinzufügen.") {
                    ChoiceCard("🙋", "Ich selbst", "Ich will selbst etwas lernen", !forChild) { forChild = false }
                    ChoiceCard("👨‍👧", "Mein Kind", "Ich richte den Garten für mein Kind ein", forChild) { forChild = true }
                }
                Step.NAME -> Page(
                    if (forChild) "Wie heißt dein Kind?" else "Wie heißt du?",
                    "Und welches Tier soll den Garten bewachen?",
                ) {
                    OutlinedTextField(name, { name = it.take(20) }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    AvatarPicker(avatar) { avatar = it }
                }
                Step.LEVEL -> Page(
                    if (forChild) "In welcher Klasse ist ${name.ifBlank { "dein Kind" }}?" else "In welcher Klasse bist du?",
                    "Damit wir passende Beete vorschlagen. Du kannst trotzdem alles wählen.",
                ) { LevelPicker(level) { level = it } }
                Step.BEDS -> PackBrowser(
                    packs = packs,
                    level = level,
                    isChosen = { it.id in chosen },
                    onToggle = { p -> chosen = if (p.id in chosen) chosen - p.id else chosen + p.id },
                    chosenLabel = "Gewählt",
                    chooseLabel = "Wählen",
                    header = {
                        item {
                            Text("Welche Beete willst du anlegen?", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "Wähle beliebig viele Wissenspakete – passend zu ${Levels.label(level)} steht oben, " +
                                    "alle anderen findest du über die Filter. Oder lege ein eigenes Beet an.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                        item {
                            ElevatedCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp)) {
                                    Text("✏️ Eigenes Beet", style = MaterialTheme.typography.titleMedium)
                                    Text("Für eigene Vokabeln, Lernzettel – alles, was du dir merken willst.", style = MaterialTheme.typography.bodySmall)
                                    if (ownBeds.isNotEmpty()) {
                                        ownBeds.forEach { (n, e) ->
                                            InputChip(
                                                selected = true, onClick = { ownBeds = ownBeds - (n to e) },
                                                label = { Text("$e $n") },
                                                trailingIcon = { Icon(Icons.Filled.Close, "Entfernen", Modifier.size(16.dp)) },
                                            )
                                        }
                                    }
                                    if (showOwnForm) {
                                        Spacer(Modifier.height(6.dp))
                                        OwnBedForm { n, e -> ownBeds = ownBeds + (n.trim() to e); showOwnForm = false }
                                    } else TextButton(onClick = { showOwnForm = true }) { Text("Eigenes Beet hinzufügen") }
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun Welcome(extra: @Composable () -> Unit = {}) {
    val t = rememberInfiniteTransition(label = "grow")
    val grow by t.animateFloat(0.4f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "g")
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Plant(Stage.SHOOT, 0f, 200f, 3, Modifier.size(90.dp), grow)
            Plant(Stage.BLOOM, 0f, 330f, 5, Modifier.size(130.dp), grow)
            Plant(Stage.TREE, 0f, 45f, 1, Modifier.size(110.dp), grow)
        }
        Spacer(Modifier.height(24.dp))
        Text("Wissensgarten", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(12.dp))
        Text(
            "Lass dein Wissen wachsen.\nJede Karte ist ein Samen – wer regelmäßig gießt, erntet Wissen, das bleibt.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Für Grundschule bis Abitur – und für Erwachsene.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        extra()
    }
}

@Composable
private fun Page(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@Composable
private fun ChoiceCard(emoji: String, title: String, text: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = if (selected) CardDefaults.outlinedCardBorder().copy(width = 2.dp) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 34.sp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Zwei passende kostenlose Pakete für die Klassenstufe, möglichst aus verschiedenen Fächern (Mathe zuerst). */
internal fun recommendedPacks(packs: List<Pack>, level: Int): Set<String> {
    val fit = packs.filter { level in it.grades && !it.isPlus }
        .sortedWith(compareBy<Pack>({ if (it.subject == "mathe") 0 else 1 }, { it.grades.size }))
    val out = LinkedHashSet<String>()
    for (p in fit) {
        if (out.size >= 2) break
        if (out.none { id -> packs.first { it.id == id }.subject == p.subject }) out += p.id
    }
    return out
}
