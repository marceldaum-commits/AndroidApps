package de.raumlicht.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.raumlicht.app.ui.fx.*
import de.raumlicht.app.ui.lux.Lux
import de.raumlicht.app.ui.lux.LuxMood
import de.raumlicht.app.ui.stage.Stage
import de.raumlicht.app.ui.stage.StageController
import de.raumlicht.app.ui.stage.StageFrame
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.core.model.*

/**
 * Ballerbude: 45 Sekunden lang steigen Leuchtballons auf, antippen heißt abschießen (Lux feuert). Drei Spielarten:
 * reiner Spaß (Gold bringt viel, der dicke orange Blindgänger kostet Punkte und die Serie), mit Aufgabe (nur Zahlen, die zur Regel passen)
 * und zu zweit (jedem gehört eine Hälfte). Verpasste Ballons kosten nichts; daneben getippt heißt: Lux ballert daneben.
 */
@Composable
fun BalloonScreen(best: Int, onScore: (Int) -> Unit, onBack: () -> Unit, controller: StageController = remember { StageController() }, seed: Long = remember { nowNanos() }) {
    var round by remember { mutableIntStateOf(0) }                     // 0 = noch nicht gestartet
    var mode by remember { mutableStateOf(BalloonMode.SOLO) }
    val game = remember(round, mode) { BalloonGame(seed + round, mode = mode) }
    var running by remember { mutableStateOf(false) }
    var t by remember(round) { mutableDoubleStateOf(0.0) }
    var popped by remember(round) { mutableStateOf(setOf<Int>()) }
    // im Duell: links und rechts; sonst zählt nur der erste Stand
    var scores by remember(round) { mutableStateOf(listOf(BalloonScore(), BalloonScore())) }
    var flash by remember(round) { mutableStateOf<String?>(null) }
    var record by remember(round) { mutableStateOf(false) }
    val sfx = LocalSfx.current; val view = LocalBuzzer.current; val vib = LocalVibration.current; val speaker = LocalSpeaker.current
    val over = round > 0 && !running && t >= game.duration
    val duel = mode == BalloonMode.DUEL; val quiz = mode == BalloonMode.QUIZ
    val score = scores[0]
    val rule = game.ruleAt(t)

    // Uhr der Runde
    LaunchedEffect(running, round) {
        var last = 0L
        while (running) withFrameNanos { now ->
            val dt = if (last == 0L) 0.0 else ((now - last) / 1e9).coerceAtMost(0.1); last = now
            t = (t + dt).coerceAtMost(game.duration)
            if (t >= game.duration) {
                running = false
                if (mode == BalloonMode.SOLO) { val pts = scores[0].points; record = pts > best; onScore(pts) }       // frisch lesen: der Stand ändert sich während der Runde
                controller.party(if (record) 4.0 else 2.0); sfx?.play(Sound.WIN)
            }
        }
    }
    // Mit Aufgabe: Lux sagt jede neue Regel an
    LaunchedEffect(round, rule.text, running) { if (running && quiz) speaker?.say(de.raumlicht.core.model.Vorlesen.BALLOON_CALL + rule.text) }
    // Bühne in kleinen Schritten nachführen (die Kugeln gleiten dazwischen von selbst weiter)
    LaunchedEffect(round, (t * 14).toInt(), popped, running, mode) {
        controller.setState(if (round == 0) BalloonGame(seed).scene(1.6, emptySet()) else game.scene(t, popped, if (over) "happy" else "idle"))
    }
    DisposableEffect(controller, round, running, mode) {
        controller.onTap = { id ->
            val b = game.byId(id)
            if (running && b != null && b.id !in popped && game.alive(t, popped).any { it.id == b.id }) {
                val p = game.pos(b, t); popped = popped + b.id
                val who = if (game.mode == BalloonMode.DUEL) game.side(p) else 0; val old = scores[who]
                // die Regel gilt im Moment des Tippens (sie wechselt während der Runde – nicht den Stand vom Rundenbeginn nehmen)
                val fits = game.mode != BalloonMode.QUIZ || game.ruleAt(t).ok(game.numbers[b.id])
                val now = if (fits) old.hit(b.kind) else old.wrong()
                scores = scores.toMutableList().also { it[who] = now }
                controller.fire(Beam(BalloonGame.LUX, p, b.kind.color, true), 140); controller.burst(p, b.kind.color)
                when {
                    // der Abzug steht nur da, wenn wirklich Punkte abgezogen wurden (bei 0 Punkten geht es nicht tiefer)
                    !fits -> { flash = "${game.numbers[b.id]} passt nicht!" + (old.points - now.points).let { if (it > 0) " −$it" else "" }; sfx?.play(Sound.WRONG); view.buzz(Buzz.CRASH, vib) }
                    b.kind == BalloonKind.DUD -> { flash = "Blindgänger!" + (old.points - now.points).let { if (it > 0) " −$it" else "" }; sfx?.play(Sound.WRONG); view.buzz(Buzz.CRASH, vib) }
                    b.kind == BalloonKind.GOLD -> { flash = "Gold! +${b.kind.points + old.bonus}"; controller.party(1.2); sfx?.play(Sound.WIN, 1.3f); view.buzz(Buzz.HIT, vib) }
                    else -> { flash = if (now.combo % 5 == 0) "Serie ${now.combo}! Jeder Treffer +${now.bonus}" else null; sfx?.play(Sound.HIT, (1f + now.combo * 0.04f).coerceAtMost(1.9f)); view.buzz(Buzz.TICK, vib) }
                }
            }
        }
        // Daneben getippt: Lux ballert genau dorthin ins Leere – und die Serie beginnt von vorn
        controller.onMiss = { p ->
            if (running) {
                controller.fire(Beam(BalloonGame.LUX, p, "white", false), 140); sfx?.play(Sound.TICK, 0.7f); view.buzz(Buzz.TICK, vib)
                val who = if (game.mode == BalloonMode.DUEL) game.side(p) else 0
                flash = if (scores[who].combo >= 2) "Daneben – die Serie startet neu" else "Daneben!"; scores = scores.toMutableList().also { it[who] = it[who].miss() }
            }
        }
        onDispose { controller.onTap = null; controller.onMiss = null }
    }

    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding()) {
        Bar("Ballerbude", onBack) { Text("Bestwert $best", Modifier.testTag("bestwert"), color = Q.Yellow, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${score.points}", Modifier.testTag("punkte"), color = if (duel) Q.Pink else Q.Yellow, fontWeight = FontWeight.Black, fontSize = 30.sp)
            Text(if (duel) "links" else if (score.combo >= 2) "Serie ${score.combo}" else "Punkte", Modifier.testTag("serie"), color = if (duel || score.combo >= 5) Q.Pink else Q.Muted, fontWeight = FontWeight.Bold)
            LinearProgressIndicator({ (1 - t / game.duration).toFloat() }, Modifier.weight(1f).height(8.dp), color = Q.Cyan, trackColor = Q.Panel)
            Text("${(game.duration - t).toInt()} s", Modifier.testTag("zeit"), color = Q.Text, fontWeight = FontWeight.Bold)
            if (duel) { Text("rechts", color = Q.Cyan, fontWeight = FontWeight.Bold); Text("${scores[1].points}", Modifier.testTag("punkte-rechts"), color = Q.Cyan, fontWeight = FontWeight.Black, fontSize = 30.sp) }
        }
        if (quiz && round > 0) Text("Schieß ab: ${rule.text}", Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp).background(Q.Deep, RoundedCornerShape(12.dp)).border(1.5.dp, Q.Yellow, RoundedCornerShape(12.dp)).padding(8.dp).testTag("regel"),
            color = Q.Yellow, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp)) {
            Stage(controller, Modifier.fillMaxSize())
            StageFrame(Q.Pink)
            flash?.let { Text(it, Modifier.align(Alignment.TopCenter).padding(top = 14.dp).background(Q.Space.copy(alpha = 0.8f), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 6.dp).testTag("meldung"),
                color = if (it.startsWith("Blind") || it.contains("passt nicht")) Q.Orange else Q.Yellow, fontWeight = FontWeight.Black) }
            if (!running) Column(Modifier.align(Alignment.Center).padding(24.dp).background(Q.Space.copy(alpha = 0.88f), RoundedCornerShape(24.dp)).border(2.dp, Q.Pink, RoundedCornerShape(24.dp)).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Lux(if (over) LuxMood.WOW else LuxMood.HAPPY, size = 64.dp)
                if (over) {
                    Text(when { duel && scores[0].points == scores[1].points -> "Unentschieden!"; duel -> if (scores[0].points > scores[1].points) "Links gewinnt!" else "Rechts gewinnt!"; record -> "Neuer Bestwert!"; else -> "Runde vorbei!" },
                        Modifier.testTag("ausgang"), color = Q.Yellow, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Text(if (duel) "Links ${scores[0].points} · Rechts ${scores[1].points}" else "${count(score.points, "Punkt", "Punkte")} · ${score.hits} Treffer · längste Serie ${score.bestCombo}", Modifier.testTag("ergebnis"), color = Q.Text, textAlign = TextAlign.Center)
                } else {
                    Text("Nur zum Spaß 🎈", color = Q.Yellow, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Text("45 Sekunden. Tipp die Ballons an – ich ballere sie ab!\nGold bringt 5 Punkte, kleine grüne 2.\nDen dicken orangen lieber nicht: −2, und die Serie beginnt von vorn.\nDaneben getippt? Dann ballere ich daneben.", color = Q.Text, textAlign = TextAlign.Center, fontSize = 14.sp)
                }
                fun start(m: BalloonMode) { mode = m; round++; running = true; sfx?.play(Sound.LAUNCH) }
                NeonButton({ start(BalloonMode.SOLO) }, Modifier.testTag("ballern"), color = Q.Green) { Text(if (over && mode == BalloonMode.SOLO) "↻ Noch eine Runde" else "▶ Los geht’s", fontWeight = FontWeight.Black) }
                NeonButton({ start(BalloonMode.QUIZ) }, Modifier.testTag("ballern-aufgabe"), color = Q.Yellow) { Text("🧮 Mit Aufgabe: nur passende Zahlen", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                NeonButton({ start(BalloonMode.DUEL) }, Modifier.testTag("ballern-duell"), color = Q.Cyan) { Text("👥 Zu zweit: links gegen rechts", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}
