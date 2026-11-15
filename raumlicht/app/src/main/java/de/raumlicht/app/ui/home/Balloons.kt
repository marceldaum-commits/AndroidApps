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
import androidx.compose.ui.platform.LocalView
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
 * Ballerbude – reiner Spaß, kein Rechnen: 45 Sekunden lang steigen Leuchtballons auf, antippen heißt abschießen (Lux feuert).
 * Gold bringt viel, der dicke orange Blindgänger kostet Punkte und die Serie. Verpasste Ballons kosten nichts.
 */
@Composable
fun BalloonScreen(best: Int, onScore: (Int) -> Unit, onBack: () -> Unit, controller: StageController = remember { StageController() }, seed: Long = remember { System.nanoTime() }) {
    var round by remember { mutableIntStateOf(0) }                     // 0 = noch nicht gestartet
    val game = remember(round) { BalloonGame(seed + round) }
    var running by remember { mutableStateOf(false) }
    var t by remember(round) { mutableDoubleStateOf(0.0) }
    var popped by remember(round) { mutableStateOf(setOf<Int>()) }
    var score by remember(round) { mutableStateOf(BalloonScore()) }
    var flash by remember(round) { mutableStateOf<String?>(null) }
    var record by remember(round) { mutableStateOf(false) }
    val sfx = LocalSfx.current; val view = LocalView.current; val vib = LocalVibration.current
    val over = round > 0 && !running && t >= game.duration

    // Uhr der Runde
    LaunchedEffect(running, round) {
        var last = 0L
        while (running) withFrameNanos { now ->
            val dt = if (last == 0L) 0.0 else ((now - last) / 1e9).coerceAtMost(0.1); last = now
            t = (t + dt).coerceAtMost(game.duration)
            if (t >= game.duration) { running = false; record = score.points > best; onScore(score.points); controller.party(if (record) 4.0 else 2.0); sfx?.play(Sound.WIN) }
        }
    }
    // Bühne in kleinen Schritten nachführen (die Kugeln gleiten dazwischen von selbst weiter)
    LaunchedEffect(round, (t * 14).toInt(), popped, running) {
        controller.setState(if (round == 0) BalloonGame(seed).scene(1.6, emptySet()) else game.scene(t, popped, if (over) "happy" else "idle"))
    }
    DisposableEffect(controller, round, running) {
        controller.onTap = { id ->
            val b = game.byId(id)
            if (running && b != null && b.id !in popped && game.alive(t, popped).any { it.id == b.id }) {
                val p = game.pos(b, t); popped = popped + b.id; score = score.hit(b.kind)
                controller.fire(Beam(BalloonGame.LUX, p, b.kind.color, true), 140); controller.burst(p, b.kind.color)
                when (b.kind) {
                    BalloonKind.DUD -> { flash = "Blindgänger! −2"; sfx?.play(Sound.WRONG); view.buzz(Buzz.CRASH, vib) }
                    BalloonKind.GOLD -> { flash = "Gold! +${b.kind.points + (score.combo - 1) / 5}"; controller.party(1.2); sfx?.play(Sound.WIN, 1.3f); view.buzz(Buzz.HIT, vib) }
                    else -> { flash = if (score.combo % 5 == 0) "Serie ${score.combo}! Jeder Treffer +${score.bonus}" else null; sfx?.play(Sound.HIT, (1f + score.combo * 0.04f).coerceAtMost(1.9f)); view.buzz(Buzz.TICK, vib) }
                }
            }
        }
        // Daneben getippt: Lux ballert genau dorthin ins Leere – und die Serie ist weg
        controller.onMiss = { p ->
            if (running) {
                controller.fire(Beam(BalloonGame.LUX, p, "white", false), 140); sfx?.play(Sound.TICK, 0.7f); view.buzz(Buzz.TICK, vib)
                flash = if (score.combo >= 2) "Daneben! Serie weg" else "Daneben!"; score = score.miss()
            }
        }
        onDispose { controller.onTap = null; controller.onMiss = null }
    }

    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding()) {
        Bar("Ballerbude", onBack) { Text("Bestwert $best", Modifier.testTag("bestwert"), color = Q.Yellow, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${score.points}", Modifier.testTag("punkte"), color = Q.Yellow, fontWeight = FontWeight.Black, fontSize = 30.sp)
            Text(if (score.combo >= 2) "Serie ${score.combo}" else "Punkte", Modifier.testTag("serie"), color = if (score.combo >= 5) Q.Pink else Q.Muted, fontWeight = FontWeight.Bold)
            LinearProgressIndicator({ (1 - t / game.duration).toFloat() }, Modifier.weight(1f).height(8.dp), color = Q.Cyan, trackColor = Q.Panel)
            Text("${(game.duration - t).toInt()} s", Modifier.testTag("zeit"), color = Q.Text, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp)) {
            Stage(controller, Modifier.fillMaxSize())
            StageFrame(Q.Pink)
            flash?.let { Text(it, Modifier.align(Alignment.TopCenter).padding(top = 14.dp).background(Q.Space.copy(alpha = 0.8f), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 6.dp).testTag("meldung"),
                color = if (it.startsWith("Blind")) Q.Orange else Q.Yellow, fontWeight = FontWeight.Black) }
            if (!running) Column(Modifier.align(Alignment.Center).padding(24.dp).background(Q.Space.copy(alpha = 0.88f), RoundedCornerShape(24.dp)).border(2.dp, Q.Pink, RoundedCornerShape(24.dp)).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Lux(if (over) LuxMood.WOW else LuxMood.HAPPY, size = 72.dp)
                if (over) {
                    Text(if (record) "Neuer Bestwert!" else "Runde vorbei!", color = Q.Yellow, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Text("${score.points} Punkte · ${score.hits} Treffer · längste Serie ${score.bestCombo}", Modifier.testTag("ergebnis"), color = Q.Text, textAlign = TextAlign.Center)
                } else {
                    Text("Nur zum Spaß 🎈", color = Q.Yellow, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Text("45 Sekunden. Tipp die Ballons an – ich baller sie ab!\nGold bringt 5 Punkte, kleine grüne 2.\nDen dicken orangen lieber nicht: −2 und die Serie ist weg.\nDaneben getippt? Dann ballere ich daneben.", color = Q.Text, textAlign = TextAlign.Center, fontSize = 14.sp)
                }
                NeonButton({ round++; running = true; sfx?.play(Sound.LAUNCH) }, Modifier.testTag("ballern"), color = Q.Green) { Text(if (over) "↻ Noch eine Runde" else "▶ Los geht's", fontWeight = FontWeight.Black) }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}
