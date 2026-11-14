package de.kurvenflug.app.ui.modes

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kurvenflug.app.ui.flight.LevelScreen
import de.kurvenflug.app.ui.fx.*
import de.kurvenflug.app.ui.theme.Q
import de.kurvenflug.app.ui.volt.Volt
import de.kurvenflug.app.ui.volt.VoltMood
import de.kurvenflug.core.model.*
import kotlinx.coroutines.delay

/** Kapitel-Rahmen für Spielmodi ohne eigenes Kapitel. */
private fun frame(title: String, emoji: String, level: Level) = Chapter(0, title, "", emoji, emptyList(), listOf(level))

/** Ganzseitige Einblendung, die alles darunter abdeckt (keine Klicks durch). */
@Composable
private fun Overlay(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xEE070B20)).clickable(remember { MutableInteractionSource() }, null) {},
        contentAlignment = Alignment.Center) {
        Column(Modifier.padding(28.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

@Composable
private fun BigEmoji(e: String, wobble: Boolean = true) {
    val w by rememberInfiniteTransition(label = "e").animateFloat(-6f, 6f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "w")
    Text(e, fontSize = 72.sp, modifier = Modifier.graphicsLayer { if (wobble) rotationZ = w })
}

// ---------------------------------------------------------------- Boss

/** Boss-Kampf: drei Etappen hintereinander, jede zerstört ein Schild-Segment. */
@Composable
fun BossScreen(boss: Boss, probeColor: Color, onWon: (stars: Int, misses: Int) -> Unit, onBack: () -> Unit) {
    var stage by rememberSaveable { mutableIntStateOf(0) }
    var misses by rememberSaveable { mutableIntStateOf(0) }
    var intro by rememberSaveable { mutableStateOf(true) }
    var won by rememberSaveable { mutableStateOf(false) }
    val sfx = LocalSfx.current
    val level = boss.stages[stage]
    Box(Modifier.fillMaxSize()) {
        key(stage) {
            LevelScreen(
                level, frame(boss.name, boss.emoji, level), probeColor, false, {},
                onSolved = { _, _, _ -> if (stage == boss.stages.lastIndex) { won = true; onWon(Bosses.stars(misses), misses) } },
                onNext = if (stage < boss.stages.lastIndex) ({ stage++ }) else ({ }),
                onBack = onBack,
                nextLabel = if (stage < boss.stages.lastIndex) "Nächstes Segment ›" else "Sieg! 🏆",
                allowRetry = false,
                onMiss = { misses++ },
                header = "${boss.emoji} ${boss.name} · Schild " + "🛡️".repeat(boss.stages.size - stage) + "💥".repeat(stage),
            )
        }
        if (intro) Overlay {
            LaunchedEffect(Unit) { sfx?.play(Sound.CRASH, volume = 0.5f) }
            BigEmoji(boss.emoji)
            Text("${boss.name} greift an!", style = MaterialTheme.typography.headlineMedium, color = Q.Pink, textAlign = TextAlign.Center)
            Text("Sein Schild hat 3 Segmente. Jede geschaffte Etappe zerstört eins. Wenige Fehlschüsse = mehr Sterne.",
                color = Q.Text, textAlign = TextAlign.Center)
            Text("🛡️🛡️🛡️", fontSize = 32.sp)
            NeonButton(onClick = { intro = false }, color = Q.Pink) { Text("Angriff! 🚀", fontWeight = FontWeight.Black) }
        }
        if (won) Overlay {
            LaunchedEffect(Unit) { delay(300); sfx?.play(Sound.WIN) }
            BigEmoji("💥")
            Text("${boss.name} besiegt!", style = MaterialTheme.typography.headlineMedium, color = Q.Green, textAlign = TextAlign.Center)
            val st = Bosses.stars(misses)
            Text("⭐".repeat(st) + "☆".repeat(3 - st), fontSize = 36.sp)
            Text(if (misses == 0) "Ohne einen einzigen Fehlschuss – legendär!" else "$misses Fehlschüsse – trotzdem gewonnen!", color = Q.Muted)
            NeonButton(onClick = onBack, color = Q.Green) { Text("Zurück zur Basis", fontWeight = FontWeight.Bold) }
        }
    }
}

// ---------------------------------------------------------------- Asteroidensturm

/** Ein Level nach dem anderen – ein Fehlschuss beendet den Sturm. Kein Zeitdruck. */
@Composable
fun StormScreen(best: Int, probeColor: Color, onFinished: (waves: Int) -> Boolean, onBack: () -> Unit, seed: Long = remember { System.currentTimeMillis() % 1_000_000 }) {
    var runSeed by rememberSaveable { mutableLongStateOf(seed) }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var over by rememberSaveable { mutableStateOf(false) }
    var record by rememberSaveable { mutableStateOf(false) }
    var intro by rememberSaveable { mutableStateOf(true) }
    val level = remember(runSeed, index) { Storm.level(runSeed, index) }
    Box(Modifier.fillMaxSize()) {
        key(runSeed, index) {
            LevelScreen(
                level, frame("Asteroidensturm", "☄️", level), probeColor, false, {},
                onSolved = { _, _, _ -> },
                onNext = { index++ },
                onBack = { if (index > 0) onFinished(index); onBack() },
                nextLabel = "Nächste Welle ›",
                allowRetry = false,
                onMiss = { over = true; record = onFinished(index) },
                header = "☄️ Asteroidensturm · Welle ${index + 1} · Rekord $best",
            )
        }
        if (intro) Overlay {
            BigEmoji("☄️")
            Text("Asteroidensturm", style = MaterialTheme.typography.headlineMedium, color = Q.Orange)
            Text("Welle um Welle, immer neue Bahnen. Jede muss beim ersten Versuch sitzen – ein Fehlschuss beendet den Sturm.\nKeine Uhr, kein Stress: Rechne in Ruhe!",
                color = Q.Text, textAlign = TextAlign.Center)
            if (best > 0) Text("Dein Rekord: $best Wellen", color = Q.Yellow, fontWeight = FontWeight.Bold)
            NeonButton(onClick = { intro = false }, color = Q.Orange) { Text("Los! 🚀", fontWeight = FontWeight.Black) }
        }
        if (over) Overlay {
            BigEmoji(if (record) "🏆" else "☄️")
            Text(if (record) "Neuer Rekord!" else "Sturm vorbei", style = MaterialTheme.typography.headlineMedium, color = if (record) Q.Yellow else Q.Orange)
            Text("Du hast $index ${if (index == 1) "Welle" else "Wellen"} geschafft.", color = Q.Text, fontSize = 20.sp)
            if (!record && best > 0) Text("Rekord: ${maxOf(best, index)} Wellen", color = Q.Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeonButton(onClick = onBack, color = Q.Violet) { Text("Zurück") }
                NeonButton(onClick = { runSeed += 7919; index = 0; over = false; record = false }, color = Q.Orange) { Text("Neuer Sturm ↻", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ---------------------------------------------------------------- Duell

private enum class DuelMode { MENU, LOCAL, CHALLENGE, ACCEPT }

/**
 * Duell: zu zweit am selben Handy (3 Runden, abwechselnd) oder per Code an einen Freund.
 * Wer weniger Versuche braucht, gewinnt.
 */
@Composable
fun DuelScreen(probeColor: Color, incomingCode: String?, onResult: (won: Boolean) -> Unit, onBack: () -> Unit) {
    val decoded = remember(incomingCode) { incomingCode?.let { DuelCode.decode(it) } }
    var mode by rememberSaveable { mutableStateOf(if (decoded != null) DuelMode.ACCEPT else DuelMode.MENU) }
    var accept by rememberSaveable { mutableStateOf(decoded?.let { DuelCode.encode(it.seed, it.attempts) }) }
    BackHandler(mode != DuelMode.MENU) { mode = DuelMode.MENU }
    when (mode) {
        DuelMode.MENU -> DuelMenu(onLocal = { mode = DuelMode.LOCAL }, onChallenge = { mode = DuelMode.CHALLENGE },
            onCode = { accept = it; mode = DuelMode.ACCEPT }, onBack = onBack)
        DuelMode.LOCAL -> LocalDuel(probeColor, onResult, onDone = { mode = DuelMode.MENU })
        DuelMode.CHALLENGE -> Challenge(probeColor, onResult = { onResult(false) }, onDone = { mode = DuelMode.MENU })
        DuelMode.ACCEPT -> {
            val c = accept?.let { DuelCode.decode(it) }
            if (c == null) LaunchedEffect(Unit) { mode = DuelMode.MENU }
            else AcceptChallenge(c, probeColor, onResult, onDone = { mode = DuelMode.MENU })
        }
    }
}

@Composable
private fun DuelMenu(onLocal: () -> Unit, onChallenge: () -> Unit, onCode: (String) -> Unit, onBack: () -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    val valid = DuelCode.decode(code.trim().uppercase()) != null
    Box(Modifier.fillMaxSize()) {
        Starfield(seed = 77)
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Q.Text) }
                Text("⚔️ Duell", style = MaterialTheme.typography.titleLarge, color = Q.Text)
            }
            Volt(VoltMood.HAPPY, size = 60.dp, speech = "Wer braucht weniger Versuche? Gleiche Mission für beide – Rechnen gewinnt!")
            ModeCard("👥", "Zu zweit an diesem Handy", "3 Runden, abwechselnd. Gebt das Handy weiter – nicht spicken!", Q.Cyan, onLocal)
            ModeCard("📨", "Freund herausfordern", "Du spielst eine Mission und schickst den Code per WhatsApp & Co.", Q.Pink, onChallenge)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Q.Panel).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🔑 Code eines Freundes", style = MaterialTheme.typography.titleMedium, color = Q.Text)
                OutlinedTextField(code, { code = it.take(40) }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("KF-…-…") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters))
                NeonButton(onClick = { onCode(code.trim().uppercase()) }, color = Q.Yellow, enabled = valid, modifier = Modifier.fillMaxWidth()) {
                    Text("Herausforderung annehmen", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ModeCard(emoji: String, title: String, text: String, color: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val sfx = LocalSfx.current
    Row(
        Modifier.fillMaxWidth().pressBounce(interaction).clip(RoundedCornerShape(20.dp))
            .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(color.copy(alpha = 0.25f), Q.Panel)))
            .border(2.dp, color.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
            .clickable(interaction, null) { sfx?.play(Sound.TAP); onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 36.sp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Q.Text)
            Text(text, style = MaterialTheme.typography.bodySmall, color = Q.Muted)
        }
        Text("›", fontSize = 28.sp, color = color)
    }
}

/** Zu zweit: 3 Runden, je Runde spielen beide dieselbe Mission. */
@Composable
private fun LocalDuel(probeColor: Color, onResult: (Boolean) -> Unit, onDone: () -> Unit) {
    val base = rememberSaveable { DuelCode.newSeed() }
    var round by rememberSaveable { mutableIntStateOf(0) }
    var player by rememberSaveable { mutableIntStateOf(0) }
    var handover by rememberSaveable { mutableStateOf(true) }
    var scores by rememberSaveable { mutableStateOf(listOf(0, 0)) }
    var finished by rememberSaveable { mutableStateOf(false) }
    val rounds = 3
    val names = listOf("Spieler 1", "Spieler 2")
    val colors = listOf(Q.Cyan, Q.Pink)
    val level = remember(round) { DuelCode.level(base + round) }
    fun advance() {
        if (player == 0) { player = 1; handover = true }
        else if (round < rounds - 1) { round++; player = 0; handover = true }
        else finished = true
    }
    Box(Modifier.fillMaxSize()) {
        key(round, player) {
            LevelScreen(
                level, frame("Duell", "⚔️", level), colors[player], false, {},
                onSolved = { _, _, a -> scores = scores.toMutableList().also { it[player] += a } },
                onNext = { advance() }, onBack = onDone,
                nextLabel = if (player == 0) "Handy weitergeben ›" else if (round < rounds - 1) "Nächste Runde ›" else "Ergebnis ›",
                allowRetry = false,
                header = "⚔️ Runde ${round + 1}/$rounds · ${names[player]} · Stand ${scores[0]} : ${scores[1]} Versuche",
            )
        }
        if (handover && !finished) Overlay {
            BigEmoji(if (player == 0) "🎮" else "🙈")
            Text("${names[player]} ist dran!", style = MaterialTheme.typography.headlineMedium, color = colors[player])
            Text(if (player == 1) "${names[0]}: Nicht spicken! Gib das Handy weiter." else "Runde ${round + 1} von $rounds",
                color = Q.Text, textAlign = TextAlign.Center)
            Text("Wenige Versuche gewinnen – rechne erst, dann starte.", color = Q.Muted, textAlign = TextAlign.Center)
            NeonButton(onClick = { handover = false }, color = colors[player]) { Text("Bereit! 🚀", fontWeight = FontWeight.Black) }
        }
        if (finished) Overlay {
            val (a, b) = scores
            LaunchedEffect(Unit) { onResult(false) }   // zählt als gespieltes Duell
            BigEmoji(if (a == b) "🤝" else "🏆")
            Text(when { a < b -> "${names[0]} gewinnt!"; b < a -> "${names[1]} gewinnt!"; else -> "Unentschieden!" },
                style = MaterialTheme.typography.headlineMedium, color = when { a < b -> colors[0]; b < a -> colors[1]; else -> Q.Yellow })
            Text("Versuche insgesamt: ${names[0]} $a · ${names[1]} $b", color = Q.Text)
            NeonButton(onClick = onDone, color = Q.Green) { Text("Fertig") }
        }
    }
}

private fun share(ctx: android.content.Context, text: String) {
    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Herausforderung senden"))
}

/** Du spielst vor, dann geht der Code an einen Freund. */
@Composable
private fun Challenge(probeColor: Color, onResult: () -> Unit, onDone: () -> Unit) {
    val seed = rememberSaveable { DuelCode.newSeed() }
    var attempts by rememberSaveable { mutableIntStateOf(0) }
    val ctx = LocalContext.current
    val level = remember(seed) { DuelCode.level(seed) }
    Box(Modifier.fillMaxSize()) {
        LevelScreen(level, frame("Herausforderung", "📨", level), probeColor, false, {},
            onSolved = { _, _, a -> attempts = a; onResult() }, onNext = { }, onBack = onDone,
            nextLabel = "Code senden 📨", allowRetry = false, header = "📨 Du spielst vor – dann ist dein Freund dran")
        if (attempts > 0) Overlay {
            BigEmoji("📨")
            Text("Geschafft in $attempts ${if (attempts == 1) "Versuch" else "Versuchen"}!", style = MaterialTheme.typography.headlineMedium, color = Q.Pink, textAlign = TextAlign.Center)
            Text("Dein Code:", color = Q.Muted)
            Text(DuelCode.encode(seed, attempts), fontSize = 28.sp, fontWeight = FontWeight.Black, color = Q.Yellow)
            NeonButton(onClick = { share(ctx, DuelCode.shareText(seed, attempts)) }, color = Q.Pink) { Text("An Freund senden", fontWeight = FontWeight.Bold) }
            TextButton(onClick = onDone) { Text("Fertig", color = Q.Muted) }
        }
    }
}

/** Einen Code annehmen: gleiche Mission, dann Vergleich. */
@Composable
private fun AcceptChallenge(c: DuelCode.Challenge, probeColor: Color, onResult: (Boolean) -> Unit, onDone: () -> Unit) {
    var mine by rememberSaveable { mutableIntStateOf(0) }
    var intro by rememberSaveable { mutableStateOf(true) }
    val ctx = LocalContext.current
    val level = remember(c) { DuelCode.level(c.seed) }
    Box(Modifier.fillMaxSize()) {
        LevelScreen(level, frame("Herausforderung", "🔑", level), probeColor, false, {},
            onSolved = { _, _, a -> mine = a; onResult(a < c.attempts) }, onNext = { }, onBack = onDone,
            nextLabel = "Ergebnis ›", allowRetry = false, header = "🔑 Zu schlagen: ${c.attempts} ${if (c.attempts == 1) "Versuch" else "Versuche"}")
        if (intro) Overlay {
            BigEmoji("⚔️")
            Text("Herausforderung!", style = MaterialTheme.typography.headlineMedium, color = Q.Yellow)
            Text("Dein Freund hat diese Mission in ${c.attempts} ${if (c.attempts == 1) "Versuch" else "Versuchen"} geschafft. Kannst du's besser?",
                color = Q.Text, textAlign = TextAlign.Center)
            NeonButton(onClick = { intro = false }, color = Q.Yellow) { Text("Annehmen! 🚀", fontWeight = FontWeight.Black) }
        }
        if (mine > 0) Overlay {
            BigEmoji(if (mine < c.attempts) "🏆" else if (mine == c.attempts) "🤝" else "💪")
            Text(DuelCode.verdict(mine, c.attempts), style = MaterialTheme.typography.titleLarge, color = Q.Text, textAlign = TextAlign.Center)
            NeonButton(onClick = { share(ctx, "Revanche! " + DuelCode.shareText(c.seed, mine)) }, color = Q.Pink) { Text("Ergebnis zurückschicken", fontWeight = FontWeight.Bold) }
            TextButton(onClick = onDone) { Text("Fertig", color = Q.Muted) }
        }
    }
}

// ---------------------------------------------------------------- Abzeichen

@Composable
fun BadgesScreen(stats: Stats, onBack: () -> Unit) {
    val earned = Badges.earned(stats).map { it.id }.toSet()
    Box(Modifier.fillMaxSize()) {
        Starfield(seed = 88)
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Q.Text) }
                Text("🏅 Abzeichen · ${earned.size}/${Badges.all.size}", style = MaterialTheme.typography.titleLarge, color = Q.Text)
            }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Badges.all.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { b ->
                            val got = b.id in earned
                            Column(
                                Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(if (got) Q.Yellow.copy(alpha = 0.15f) else Q.Panel)
                                    .border(1.5.dp, if (got) Q.Yellow else Q.Muted.copy(alpha = 0.2f), RoundedCornerShape(18.dp)).padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(if (got) b.emoji else "🔒", fontSize = 34.sp, modifier = Modifier.graphicsLayer { alpha = if (got) 1f else 0.5f })
                                Text(b.title, fontWeight = FontWeight.Bold, color = if (got) Q.Text else Q.Muted, textAlign = TextAlign.Center)
                                Text(b.how, style = MaterialTheme.typography.labelSmall, color = Q.Muted, textAlign = TextAlign.Center)
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Feier-Einblendung für frisch verdiente Abzeichen. */
@Composable
fun NewBadgesOverlay(badges: List<Badge>, onDone: () -> Unit) {
    val sfx = LocalSfx.current
    LaunchedEffect(Unit) { sfx?.play(Sound.WIN) }
    Overlay {
        Text("Neues Abzeichen!", style = MaterialTheme.typography.headlineMedium, color = Q.Yellow)
        badges.forEach { b ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                BigEmoji(b.emoji)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(b.title, style = MaterialTheme.typography.titleLarge, color = Q.Text)
                    Text(b.how, color = Q.Muted)
                }
            }
        }
        NeonButton(onClick = onDone, color = Q.Yellow) { Text("Juhu! 🎉", fontWeight = FontWeight.Black) }
    }
}
