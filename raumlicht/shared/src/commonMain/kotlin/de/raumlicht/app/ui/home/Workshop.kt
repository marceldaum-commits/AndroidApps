package de.raumlicht.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.raumlicht.app.ui.fx.*
import de.raumlicht.app.ui.level.Stepper
import de.raumlicht.app.ui.lux.Lux
import de.raumlicht.app.ui.lux.LuxMood
import de.raumlicht.app.ui.stage.Stage
import de.raumlicht.app.ui.stage.StageController
import de.raumlicht.app.ui.stage.StageFrame
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.core.model.*
import kotlin.math.abs

// ───────────────────────── Achterbahn-Baumeister ─────────────────────────

/**
 * Bahn selbst bauen: fünf Stützpunkte hoch- und runterziehen (in der Bühne oder mit den Knöpfen), dann Probefahrt mit echter Physik.
 * Aufträge verlangen Sterne oder Tempo; „Freies Bauen“ lässt alles offen.
 */
@Composable
fun CoasterBuildScreen(onBack: () -> Unit, controller: StageController = remember { StageController() }) {
    var goalIdx by rememberSaveable { mutableIntStateOf(0) }                      // −1 = freies Bauen
    val goal = BuiltTrack.goals.getOrNull(goalIdx)
    var heights by rememberSaveable(goalIdx) { mutableStateOf(goal?.start ?: listOf(5, 3, 4, 2, 1)) }
    val track = remember(heights) { BuiltTrack(heights) }
    var run by remember(goalIdx) { mutableStateOf<TrackRun?>(null) }
    var frame by remember(goalIdx) { mutableIntStateOf(0) }
    var playing by remember(goalIdx) { mutableStateOf(false) }
    var inside by rememberSaveable { mutableStateOf(false) }
    var done by rememberSaveable { mutableStateOf(setOf<Int>()) }
    val sfx = LocalSfx.current; val view = LocalBuzzer.current; val vib = LocalVibration.current; val speaker = LocalSpeaker.current
    fun set(i: Int, h: Int) { val c = h.coerceIn(BuiltTrack.HEIGHTS.first, BuiltTrack.HEIGHTS.last); if (!playing && c != heights[i]) { heights = heights.toMutableList().also { it[i] = c }; run = null; frame = 0; sfx?.play(Sound.TICK, 0.8f + c * 0.08f) } }
    val r = run; val x = r?.xs?.getOrNull(frame.coerceAtMost((r.xs.size - 1).coerceAtLeast(0))) ?: 0.0
    val finishedShow = r != null && !playing && frame >= r.xs.size - 1
    val message = when {
        r == null || !finishedShow -> goal?.text ?: "Bau, was du willst – zieh die gelben Punkte hoch und runter."
        !r.finished -> "Bei x = ${Geo.num(r.stuckAt ?: 0.0)} geht dem Wagen der Schwung aus. Höher als der Start kommt er nie – bau den Hügel flacher oder den Start höher."
        goal != null && r.stars < goal.stars.size -> "Im Ziel – aber nur ${r.stars} von ${goal.stars.size} Sternen. Die Bahn muss genau durch die Sterne führen."
        goal != null && r.endSpeed < goal.minEndSpeed -> "Im Ziel mit Tempo ${Geo.num(r.endSpeed)} – gebraucht wird ${Geo.num(goal.minEndSpeed)}. Mehr Gefälle bis zum Ziel!"
        else -> "Geschafft! ${Geo.num(r.seconds)} Sekunden, Spitzentempo ${Geo.num(r.topSpeed)}."
    }

    LaunchedEffect(heights, goalIdx, playing) { controller.setState(track.scene(goal, handles = !playing)) }
    // Mitfahren nur während der Fahrt – danach wieder der Blick von außen, sonst sieht man die Stützpunkte nicht
    LaunchedEffect(x, inside, r, playing) { if (r != null) controller.ride(x / track.length, inside && playing) else controller.rideOff() }
    DisposableEffect(controller, playing) {
        controller.onDrag = { id, p -> id.removePrefix("h").toIntOrNull()?.let { i -> if (i in heights.indices) set(i, p[2]) } }
        onDispose { controller.onDrag = null; controller.rideOff() }
    }
    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        val rr = run ?: return@LaunchedEffect
        var t = 0.0; var last = 0L
        while (frame < rr.xs.size - 1) withFrameNanos { now -> val dt = if (last == 0L) 0.0 else ((now - last) / 1e9).coerceAtMost(0.05); last = now; t += dt; frame = (t * 60).toInt().coerceAtMost(rr.xs.size - 1) }
        playing = false
        val ok = rr.finished && (goal == null || track.solves(goal, rr))
        if (ok) { controller.party(3.0); sfx?.play(Sound.WIN); view.buzz(Buzz.HIT, vib); if (goal != null) done = done + goalIdx } else { sfx?.play(Sound.WRONG); view.buzz(Buzz.CRASH, vib) }
        speaker?.say(if (ok) "Geschafft!" else if (!rr.finished) "Der Schwung reicht nicht." else "Im Ziel, aber der Auftrag fehlt noch.")
    }

    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding()) {
        Bar("Achterbahn-Baumeister", onBack) { Text("${done.size}/${BuiltTrack.goals.size} ✓", color = Q.Yellow, fontWeight = FontWeight.Bold) }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pick("✏️ Freies Bauen", goalIdx < 0, Q.Yellow, "auftrag-frei") { goalIdx = -1 }
            BuiltTrack.goals.forEachIndexed { i, g -> Pick((if (i in done) "✓ " else "") + g.title, goalIdx == i, Q.Cyan, "auftrag-$i") { goalIdx = i } }
        }
        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp)) { Stage(controller, Modifier.fillMaxSize()); StageFrame(Q.Cyan) }
        Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 12.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Lux(if (finishedShow && r!!.finished) LuxMood.HAPPY else if (finishedShow) LuxMood.SAD else LuxMood.IDLE, size = 46.dp)
            Text(message, Modifier.padding(start = 8.dp).testTag("bau-meldung"), color = Q.Text, fontSize = 13.sp)
        }
        // Stützpunkte auch per Knopf: Höhe 1 bis 6
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            heights.forEachIndexed { i, h -> Column(Modifier.weight(1f).background(Q.Deep, RoundedCornerShape(12.dp)).border(1.dp, Q.Yellow.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).padding(vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Pick("▲", false, Q.Yellow, "hoch-$i") { set(i, h + 1) }
                Text("$h", Modifier.testTag("hoehe-$i"), color = Q.Yellow, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Pick("▼", false, Q.Yellow, "runter-$i") { set(i, h - 1) }
            } }
        }
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeonButton({ inside = !inside }, Modifier.testTag("bau-sicht"), color = Q.Violet) { Text(if (inside) "👁 von außen" else "🎢 einsteigen", fontWeight = FontWeight.Bold) }
            NeonButton({ run = track.run(goal); frame = 0; playing = true; sfx?.play(Sound.LAUNCH) }, Modifier.weight(1f).testTag("probefahrt"), color = Q.Green, enabled = !playing) {
                Text(if (playing) "fährt …" else "▶ Probefahrt", fontWeight = FontWeight.Black) }
        }
    }
}

// ───────────────────────── Geisterfahrt gegen f′ ─────────────────────────

private fun DrawScope.plot(from: Double, to: Double, lo: Double, hi: Double, color: Color, width: Float, f: (Double) -> Double) {
    val path = Path(); var pen = false
    for (i in 0..160) { val xv = from + (to - from) * i / 160; val v = f(xv); val px = size.width * i / 160; val py = (size.height * (hi - v) / (hi - lo)).toFloat()
        if (!v.isFinite() || py < -size.height || py > 2 * size.height) { pen = false; continue }
        if (pen) path.lineTo(px, py) else path.moveTo(px, py); pen = true }
    drawPath(path, color, style = Stroke(width))
}
private fun DrawScope.zeroLine(lo: Double, hi: Double) { val y = (size.height * hi / (hi - lo)).toFloat(); drawLine(Q.Text.copy(alpha = 0.7f), Offset(0f, y), Offset(size.width, y), 2f) }

/**
 * Grafisches Ableiten als Zeichenspiel: oben die Kurve f, unten skizziert man mit dem Finger f′.
 * „Auflösen“ blendet die echte Ableitung ein und zeigt die Trefferquote. Auch mit eigener Funktion.
 */
@Composable
fun GhostScreen(onBack: () -> Unit) {
    var rideId by rememberSaveable { mutableStateOf(Ghost.rides.first().id) }
    var term by rememberSaveable { mutableStateOf("x^3-3x") }
    var built by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val own = remember(built) { Expr.parse(built).getOrNull()?.let { Coaster.custom(it, -2.0, 2.0) } }
    val isOwn = rideId == Coaster.CUSTOM
    val ride = (if (isOwn) own else Coaster.byId(rideId)) ?: Ghost.rides.first()
    val key = if (isOwn) "eigene:$built" else rideId
    var guess by remember(key) { mutableStateOf(List<Double?>(Ghost.COLUMNS) { null }) }
    var shown by remember(key) { mutableStateOf(false) }
    val (lo, hi) = remember(key) { Ghost.window(ride) }
    val fRange = remember(key) { (0..80).map { ride.f(ride.from + (ride.to - ride.from) * it / 80) }.let { v -> val a = minOf(v.min(), 0.0); val b = maxOf(v.max(), 0.0); val p = maxOf((b - a) * 0.12, 0.3); (a - p) to (b + p) } }
    val drawn = guess.count { it != null }
    val score = if (shown) Ghost.score(ride, guess) else 0
    val sfx = LocalSfx.current; val speaker = LocalSpeaker.current
    fun paint(a: Offset, b: Offset, w: Float, h: Float) {
        if (shown) return
        fun col(o: Offset) = (o.x / w * Ghost.COLUMNS).toInt().coerceIn(0, Ghost.COLUMNS - 1)
        fun value(o: Offset) = hi - (o.y / h).coerceIn(0f, 1f) * (hi - lo)
        val c0 = col(a); val c1 = col(b); val g = guess.toMutableList()
        for (c in minOf(c0, c1)..maxOf(c0, c1)) { val k = if (c1 == c0) 1.0 else (c - c0).toDouble() / (c1 - c0); g[c] = value(a) + (value(b) - value(a)) * k }
        guess = g
    }

    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding()) {
        Bar("Geisterfahrt gegen f′", onBack) { Text("f(x) = ${ride.text}", color = Q.Cyan, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1) }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pick("✏️ Eigene Funktion", isOwn, Q.Yellow, "geist-eigene") { rideId = Coaster.CUSTOM }
            Ghost.rides.forEach { c -> Pick(c.title, rideId == c.id, Q.Cyan, "geist-${c.id}") { rideId = c.id } }
        }
        if (isOwn) Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(term, { term = it; error = null }, Modifier.weight(1f).testTag("geist-eingabe"), singleLine = true, label = { Text(error ?: "f(x) =  (von −2 bis 2)") }, isError = error != null)
            NeonButton({ Expr.parse(term).fold({ e -> if (Coaster.custom(e, -2.0, 2.0) == null) error = "dort nicht definiert" else { error = null; built = term } }, { error = it.message }) }, Modifier.testTag("geist-bauen"), color = Q.Yellow) { Text("OK", fontWeight = FontWeight.Black) }
        }
        Text("Die Kurve f", Modifier.padding(start = 14.dp, top = 4.dp), color = Q.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Canvas(Modifier.fillMaxWidth().weight(0.8f).padding(horizontal = 10.dp, vertical = 4.dp).background(Q.Deep, RoundedCornerShape(16.dp)).border(1.5.dp, Q.Cyan.copy(alpha = 0.6f), RoundedCornerShape(16.dp)).testTag("kurve")) {
            zeroLine(fRange.first, fRange.second); plot(ride.from, ride.to, fRange.first, fRange.second, Q.Cyan, 6f, ride.f)
            // nach dem Auflösen: an Hoch- und Tiefpunkten ein Lot nach unten – dort ist f′ null
            if (shown) ride.spots.filter { it.kind == SpotKind.HIGH || it.kind == SpotKind.LOW }.forEach { s -> val px = (size.width * (s.x - ride.from) / (ride.to - ride.from)).toFloat()
                drawLine(Q.Yellow, Offset(px, 0f), Offset(px, size.height), 2f, pathEffect = de.raumlicht.app.ui.fx.safeDash(10f, 8f)) }
        }
        Text(if (shown) "Pink: die echte Ableitung · Gelb: deine Skizze" else "Zeichne hier mit dem Finger, wie f′ aussieht", Modifier.padding(start = 14.dp), color = Q.Pink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Canvas(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 10.dp, vertical = 4.dp).background(Q.Deep, RoundedCornerShape(16.dp)).border(1.5.dp, Q.Pink.copy(alpha = 0.6f), RoundedCornerShape(16.dp)).testTag("zeichnen")
            .pointerInput(key, shown) { detectDragGestures { change, drag -> paint(change.position - drag, change.position, size.width.toFloat(), size.height.toFloat()) } }
            .pointerInput(key, shown) { detectTapGestures { o -> paint(o, o, size.width.toFloat(), size.height.toFloat()) } }) {
            zeroLine(lo, hi)
            if (shown) ride.spots.filter { it.kind == SpotKind.HIGH || it.kind == SpotKind.LOW }.forEach { s -> val px = (size.width * (s.x - ride.from) / (ride.to - ride.from)).toFloat()
                drawLine(Q.Yellow, Offset(px, 0f), Offset(px, size.height), 2f, pathEffect = de.raumlicht.app.ui.fx.safeDash(10f, 8f)) }
            val w = size.width / Ghost.COLUMNS
            guess.forEachIndexed { c, v -> if (v != null) { val y = (size.height * (hi - v) / (hi - lo)).toFloat(); drawLine(Q.Yellow, Offset(c * w, y), Offset((c + 1) * w, y), 7f) } }
            if (shown) plot(ride.from, ride.to, lo, hi, Q.Pink, 6f, ride.df)
        }
        Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 12.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Lux(if (shown && score >= 75) LuxMood.WOW else LuxMood.IDLE, size = 46.dp)
            Text(if (shown) Ghost.verdict(score) else if (drawn < Ghost.drawn) "Zeichne die ganze Breite durch (${drawn * 100 / Ghost.COLUMNS} %), dann löse ich auf." else "Fertig? Dann löse ich auf.",
                Modifier.padding(start = 8.dp).testTag("geist-meldung"), color = if (shown) Q.Yellow else Q.Text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeonButton({ guess = List(Ghost.COLUMNS) { null }; shown = false }, Modifier.testTag("geist-neu"), color = Q.Violet) { Text("🧽 Neu", fontWeight = FontWeight.Bold) }
            NeonButton({ shown = true; val s = Ghost.score(ride, guess); sfx?.play(if (s >= 75) Sound.WIN else Sound.HIT); speaker?.say(Ghost.verdict(s)) }, Modifier.weight(1f).testTag("aufloesen"), color = Q.Green, enabled = !shown && drawn >= Ghost.drawn) {
                Text("👻 Auflösen", fontWeight = FontWeight.Black) }
        }
    }
}

// ───────────────────────── Raumflug mit Ringen ─────────────────────────

/** Ringflug: Start und drei Ringe sind gegeben, gesucht ist der Richtungsvektor. Stimmt er nicht, fliegt Lux sichtbar vorbei. Jede Runde ist neu. */
@Composable
fun RingScreen(onBack: () -> Unit, controller: StageController = remember { StageController() }, seed: Long = remember { nowNanos() % 1_000_000 }) {
    var round by rememberSaveable { mutableIntStateOf(0) }
    var streak by rememberSaveable { mutableIntStateOf(0) }
    val run = remember(round) { RingRun(seed + round) }
    var d by rememberSaveable(round) { mutableStateOf(listOf(1, 0, 0)) }
    val guess = V3(d[0], d[1], d[2])
    var u by remember(round) { mutableFloatStateOf(0f) }
    var flying by remember(round) { mutableStateOf(false) }
    var flown by remember(round) { mutableStateOf(false) }
    var inside by rememberSaveable { mutableStateOf(false) }
    val sfx = LocalSfx.current; val view = LocalBuzzer.current; val vib = LocalVibration.current; val speaker = LocalSpeaker.current
    val hits = run.hits(guess)

    LaunchedEffect(round, d) { controller.setState(run.scene(guess)) }
    LaunchedEffect(round, u, inside, flying, flown) { if (flying || flown) controller.ride(u.toDouble(), inside) else controller.rideOff() }
    DisposableEffect(controller) { onDispose { controller.rideOff() } }
    LaunchedEffect(flying) {
        if (!flying) return@LaunchedEffect
        var last = 0L
        while (u < 1f) withFrameNanos { now -> val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f); last = now; u = (u + dt / 3.5f).coerceAtMost(1f) }
        flying = false; flown = true
        if (hits == 3) { controller.party(3.0); sfx?.play(Sound.WIN); view.buzz(Buzz.HIT, vib); streak++ } else { sfx?.play(Sound.WRONG); view.buzz(Buzz.CRASH, vib); streak = 0 }
        speaker?.say(run.verdict(guess))
    }

    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding()) {
        Bar("Ringflug", onBack) { Text(if (streak >= 2) "Serie $streak" else "Runde ${round + 1}", Modifier.testTag("ring-runde"), color = Q.Yellow, fontWeight = FontWeight.Bold) }
        Text("A${run.a.fmt()}   →   R₁${run.rings[0].fmt()}  R₂${run.rings[1].fmt()}  R₃${run.rings[2].fmt()}", Modifier.padding(horizontal = 14.dp, vertical = 2.dp).testTag("ring-daten"), color = Q.Cyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp)) { Stage(controller, Modifier.fillMaxSize()); StageFrame(Q.Cyan) }
        Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 12.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Lux(if (flown && hits == 3) LuxMood.HAPPY else if (flown) LuxMood.SAD else LuxMood.IDLE, size = 46.dp)
            Text(if (flown) run.verdict(guess) else "Stell meine Flugrichtung ein, damit ich durch alle drei Ringe komme. g: x⃗ = A + t · (${d.joinToString("|") { Geo.num(it) }})",
                Modifier.padding(start = 8.dp).testTag("ring-meldung"), color = if (flown && hits == 3) Q.Green else Q.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(Q.Pink, Q.Green, Q.Cyan).forEachIndexed { i, c ->
                Stepper(null, d[i], Geo.num(d[i]), c, "d$i", Modifier.weight(1f), canDown = d[i] > -3 && !flying, canUp = d[i] < 3 && !flying) { s -> d = d.toMutableList().also { it[i] += s }; u = 0f; flown = false } }
        }
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeonButton({ inside = !inside }, color = Q.Violet) { Text(if (inside) "👁 außen" else "🚀 innen", fontWeight = FontWeight.Bold) }
            if (flown && hits == 3) NeonButton({ round++ }, Modifier.weight(1f).testTag("ring-weiter"), color = Q.Yellow) { Text("Nächste Runde ▶", fontWeight = FontWeight.Black) }
            else NeonButton({ u = 0f; flown = false; flying = true; sfx?.play(Sound.LAUNCH) }, Modifier.weight(1f).testTag("ring-flug"), color = Q.Green, enabled = !flying && !guess.isZero) {
                Text(if (flying) "fliegt …" else "▶ Losfliegen", fontWeight = FontWeight.Black) }
        }
    }
}
