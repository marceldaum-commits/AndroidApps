package de.kurvenflug.app.ui.flight

import de.kurvenflug.app.ui.fx.rememberBuzzer
import de.kurvenflug.app.ui.fx.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kurvenflug.app.ui.fx.*
import de.kurvenflug.app.ui.theme.Q
import de.kurvenflug.app.ui.volt.Volt
import de.kurvenflug.app.ui.volt.VoltMood
import de.kurvenflug.core.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

private enum class Phase { AIM, FLYING, DONE }

/** Farben der Regler – dieselbe Farbe in Formel und Regler, damit man sieht, was was ist. */
private val paramColors = listOf(Q.Cyan, Q.Pink, Q.Yellow)

/**
 * Ein Level von Kurvenflug: Formel einstellen, starten, zusehen, daraus lernen.
 * @param onSolved Sterne, eingesammelte Energiezellen, Versuche
 */
@Composable
fun LevelScreen(
    level: Level,
    chapter: Chapter,
    probeColor: Color,
    showIntro: Boolean,
    onIntroSeen: () -> Unit,
    onSolved: (stars: Int, cells: Set<Int>, attempts: Int) -> Unit,
    onNext: (() -> Unit)?,
    onBack: () -> Unit,
    /** für Bildschirmfotos: Startwerte vorgeben */
    initialValues: Map<String, Double>? = null,
    /** Beschriftung des Weiter-Knopfs (Duell, Boss, Sturm) */
    nextLabel: String = "Weiter ›",
    /** „Nochmal“ anbieten? (nicht im Duell/Sturm/Boss) */
    allowRetry: Boolean = true,
    /** nach jedem Fehlversuch (Sturm: Spiel vorbei, Boss: Schild hält) */
    onMiss: ((attempt: Int) -> Unit)? = null,
    /** eigene Zeile über dem Titel (z. B. „Duell · Spieler 2“) */
    header: String? = null,
    /** Rechenweg: so viele Schritte rechnet Volt vor (beim ersten Rechenweg einer Bahnart 1, danach 0) */
    voltSolves: Int = 0,
    /** Rechenweg geöffnet (für das Ausblenden beim nächsten Mal) */
    onWalkthrough: () -> Unit = {},
    /** Belohnung nach dem Schaffen: vielleicht ein Sticker (null = in diesem Modus keine) */
    onReward: ((stars: Int) -> StickerDrop?)? = null,
    /** Rechen-Stern verdient: Sterne nachträglich erhöhen */
    onCheckStar: (stars: Int) -> Unit = {},
) {
    val sfx = LocalSfx.current
    val view = rememberBuzzer()
    val vib = LocalVibration.current

    var values by rememberSaveable(level.id) { mutableStateOf(HashMap(initialValues ?: level.start)) }
    var attempt by rememberSaveable(level.id) { mutableIntStateOf(0) }
    var help by rememberSaveable(level.id) { mutableStateOf(false) }
    /** Kurve mit dem Finger ziehen (zeigt die ganze Bahn → zählt wie Hilfe) */
    var drag by rememberSaveable(level.id) { mutableStateOf(false) }
    // 📖 Theorie: passende Erklärung jederzeit nachlesen – kostet keine Sterne
    var theory by rememberSaveable(level.id) { mutableStateOf(false) }
    // Für Klänge, die nach dem Flug weiterlaufen (Sterne), unabhängig vom Flug-Effekt
    val soundScope = rememberCoroutineScope()
    val theoryText = remember(level.id, chapter.number) { de.kurvenflug.core.model.Theories.of(level, chapter) }
    var grabbed by remember(level.id) { mutableIntStateOf(-1) }
    var usedHelp by rememberSaveable(level.id) { mutableStateOf(false) }
    var phase by rememberSaveable(level.id) { mutableStateOf(Phase.AIM) }
    var stars by rememberSaveable(level.id) { mutableIntStateOf(0) }
    var intro by rememberSaveable(level.id) { mutableStateOf(showIntro) }
    var ghosts by remember(level.id) { mutableStateOf(listOf<List<Vec>>()) }
    var result by remember(level.id) { mutableStateOf<FlightResult?>(null) }
    var shown by remember(level.id) { mutableIntStateOf(0) }
    var hits by remember(level.id) { mutableStateOf(setOf<Int>()) }
    var taken by remember(level.id) { mutableStateOf(setOf<Int>()) }
    var bursts by remember(level.id) { mutableStateOf(listOf<Pair<Burst, Long>>()) }
    var mood by remember(level.id) { mutableStateOf(VoltMood.IDLE) }
    var speech by remember(level.id) { mutableStateOf(level.task) }
    var gap by remember(level.id) { mutableStateOf<Gap?>(null) }
    var reward by remember(level.id) { mutableStateOf<StickerDrop?>(null) }
    var winPath by remember(level.id) { mutableStateOf<List<Vec>>(emptyList()) }
    val ctx = de.kurvenflug.app.LocalPlatform.current
    val steps = remember(level.id) { Walkthrough.of(level) }
    var walkOpen by rememberSaveable(level.id) { mutableStateOf(false) }
    // Rechen-Stern: nach dem Treffer die passende Rechnung erkennen (nur in normalen Leveln)
    val check = remember(level.id) { if (allowRetry) Understand.check(level) else null }
    var answer by rememberSaveable(level.id) { mutableIntStateOf(-1) }
    var flightStars by rememberSaveable(level.id) { mutableIntStateOf(0) }

    var walkVoltSolves by rememberSaveable(level.id) { mutableIntStateOf(voltSolves) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    // Wird ein abgeschlossenes Level nach dem Drehen wiederhergestellt, fehlt der Flug → Ergebnis direkt zeigen
    LaunchedEffect(Unit) { if (phase == Phase.FLYING) phase = Phase.AIM }

    // Uhr für Funkeln und Partikel
    // (Endlos-Animation statt eigener Bild-Schleife – Tests und Akku danken es)
    val clockF by rememberInfiniteTransition(label = "clock").animateFloat(0f, 3_600_000f,
        infiniteRepeatable(tween(3_600_000, easing = LinearEasing)), label = "ms")
    val clock = clockF.toLong()

    BackHandler { onBack() }
    // Offene Fenster zuerst schließen, statt gleich das Level zu verlassen
    BackHandler(enabled = walkOpen) { walkOpen = false }
    BackHandler(enabled = intro) { intro = false; onIntroSeen() }
    BackHandler(enabled = theory) { theory = false }

    val preview2 = remember(values, help, level.id) {
        Flight.path2(level, values).let { p -> if (help) p else p.take(min(p.size, 100)) }
    }
    val preview = remember(values, help, level.id) {
        if (level.kind == Kind.POINT || level.kind == Kind.RADIUS && !help) emptyList()
        else Flight.path(level, values).let { p ->
            if (help) p else when (level.kind) {
                Kind.THROW, Kind.ORBIT -> p.take(min(p.size, 70))                 // die ersten ~1,5 Kästchen: Abschussrichtung
                Kind.LASER -> Flight.firstLeg(p)                                  // bis zum ersten Spiegel – den Abprall konstruierst du selbst
                Kind.RAMP -> p.take(1)                                            // nur die Kugel am Start
                Kind.BINOM -> p                                                  // das ganze Histogramm – die Säulen sind die Verteilung
                Kind.TANGENT -> { val x0 = level.fixed.getValue("x0"); p.dropWhile { it.x < x0 - 1e-9 }.take(100) }   // ab dem Absprung
                else -> p.take(min(p.size, 100))          // die ersten 2 Kästchen am Rand
            }
        }
    }

    fun toPx(p: Vec) = Offset(((p.x + Flight.R) / (2 * Flight.R) * canvasSize.width).toFloat(), ((Flight.R - p.y) / (2 * Flight.R) * canvasSize.height).toFloat())

    fun launch() {
        if (phase != Phase.AIM) return
        attempt++
        if (help) usedHelp = true
        val r = Flight.fly(level, values)
        result = r; shown = 0; hits = emptySet(); taken = emptySet(); gap = null
        phase = Phase.FLYING
        mood = VoltMood.THINK; speech = "3 … 2 … 1 … 🚀"
        sfx?.play(Sound.LAUNCH)
    }

    // Flug abspielen – Treffer, Zellen und Absturz genau dann, wenn die Sonde dort ist
    LaunchedEffect(phase, result) {
        val r = result ?: return@LaunchedEffect
        if (phase != Phase.FLYING) return@LaunchedEffect
        val total = r.path.size
        val firstHit = level.targets.indices.associateWith { i -> r.path.indexOfFirst { level.targets[i].contains(it) } }
        val firstCell = level.cells.indices.associateWith { i -> r.path.indexOfFirst { level.cells[i].contains(it, 0.15) } }
        val dur = (total * 2.0).coerceIn(900.0, 2600.0)
        val t0 = withFrameMillis { it }
        var last = -1
        while (true) {
            val now = withFrameMillis { it }
            val idx = min(total - 1, ((now - t0) / dur * total).toInt())
            for (i in (last + 1)..idx) {
                firstCell.forEach { (c, at) -> if (at == i && c in r.cells) {
                    taken = taken + c; sfx?.play(Sound.CELL); view.buzz(Buzz.TICK, vib)
                } }
                if (level.kind != Kind.POINT) firstHit.forEach { (tg, at) -> if (at == i && tg in r.hitTargets) {
                    hits = hits + tg; sfx?.play(Sound.HIT); view.buzz(Buzz.HIT, vib)
                    bursts = bursts + (Burst(toPx(Vec(level.targets[tg].x, level.targets[tg].y)), listOf(Q.Green, Q.Cyan, Color.White), seed = tg + attempt) to clockF.toLong())
                } }
            }
            // Flug-Ton: Tonhöhe folgt der Kurve (oben hoch, unten tief) – leise, etwa 8× pro Sekunde
            if (idx / 6 != last / 6 || last < 0) {
                val y = r.path[idx].y
                sfx?.play(Sound.FLIGHT, pitch = (1.0 + y / 14.0).toFloat().coerceIn(0.55f, 1.7f), volume = 0.16f)
            }
            shown = idx; last = idx
            if (idx >= total - 1) break
        }
        val end = r.path.last()
        if (level.kind == Kind.POINT && r.hitTargets.isNotEmpty()) {
            hits = r.hitTargets; sfx?.play(Sound.HIT); view.buzz(Buzz.HIT, vib)
            bursts = bursts + (Burst(toPx(end), listOf(Q.Green, Q.Cyan, Color.White), seed = attempt) to clockF.toLong())
        }
        if (r.crash != null) {
            sfx?.play(Sound.CRASH); view.buzz(Buzz.CRASH, vib)
            bursts = bursts + (Burst(toPx(end), listOf(Q.Orange, Q.Yellow, Color(0xFFFF4A4A)), count = 48, speed = 200f, seed = attempt) to clockF.toLong())
        }
        delay(450)
        if (r.success) {
            flightStars = Flight.stars(attempt, usedHelp)
            answer = -1
            stars = starsWithCheck(flightStars, check != null, null)
            sfx?.play(Sound.WIN)
            soundScope.launch { repeat(flightStars) { i -> delay(650L + 260L * i); sfx?.play(Sound.STAR, pitch = 1f + 0.26f * i, volume = 0.6f) } }
            mood = if (flightStars == 3) VoltMood.DANCE else VoltMood.WOW
            speech = when {
                check != null && flightStars == 3 -> "Volltreffer! Und wie hast du's gerechnet? Dafür gibt's den 3. Stern 🧠"
                check != null -> "Treffer! 💪 Weißt du auch, wie man's rechnet? 🧠"
                flightStars == 3 -> "Volltreffer beim ersten Versuch! Genial berechnet! 🤯"
                flightStars == 2 -> "Treffer! Stark! 💪"
                else -> "Geschafft! Dranbleiben zahlt sich aus. 🙌"
            }
            winPath = r.path
            reward = onReward?.invoke(stars)
            bursts = bursts + (Burst(Offset(canvasSize.width / 2f, canvasSize.height / 3f), listOf(Q.Pink, Q.Yellow, Q.Cyan, Q.Green), count = 70, speed = 320f, seed = 99, gravity = 400f) to clockF.toLong())
            // Sofort speichern (2 Sterne, wenn die Rechen-Frage noch offen ist) – der Rechen-Stern kommt per [onCheckStar] dazu
            onSolved(stars, r.cells, attempt)
            phase = Phase.DONE
        } else {
            ghosts = (ghosts + listOf(r.path)).takeLast(3)
            result = null; shown = 0; hits = emptySet(); taken = emptySet()
            mood = VoltMood.OOPS
            gap = Feedback.gap(level, values, r)
            val fb = Feedback.after(level, values, r)
            speech = if (attempt >= 2) "$fb\n💡 ${level.tip}" else fb
            if (r.crash == null) sfx?.play(if (gap?.let { kotlin.math.abs(it.to - it.from) <= 1.0 } == true) Sound.NEAR else Sound.WRONG)
            phase = Phase.AIM
            onMiss?.invoke(attempt)
        }
    }

    val r = result
    val spaceView = SpaceView(
        level = level,
        preview = if (phase == Phase.AIM) preview else emptyList(),
        crosshair = if (phase == Phase.AIM && (level.kind == Kind.POINT && help || level.kind == Kind.RENDEZVOUS)) Vec(values.getValue("x"), values.getValue("y")) else null,
        preview2 = if (phase == Phase.AIM) preview2 else emptyList(),
        gap = if (phase == Phase.AIM) gap else null,
        handles = if (phase == Phase.AIM && drag) handlesFor(level, values).mapIndexed { i, h -> Triple(h.pos, h.color, i == grabbed) } else emptyList(),
        trail2 = r?.path2?.let { p2 -> p2.take(((shown + 1).toDouble() / r.path.size * p2.size).toInt().coerceIn(0, p2.size)) }.orEmpty(),
        probe2 = if (phase == Phase.FLYING && r != null && r.path2.isNotEmpty()) r.path2[((shown.toDouble() / r.path.size) * r.path2.size).toInt().coerceIn(0, r.path2.size - 1)] else null,
        ghosts = ghosts,
        trail = r?.path?.take(shown + 1).orEmpty(),
        probe = if (phase == Phase.FLYING && r != null) r.path[min(shown, r.path.size - 1)] else null,
        hitTargets = if (phase == Phase.DONE) level.targets.indices.toSet() else hits,
        takenCells = taken,
        revealHidden = phase == Phase.DONE,
        bursts = bursts.map { (b, at) -> b to (clock - at) / 1000f }.filter { it.second in 0f..1.4f },
        probeColor = probeColor,
        time = clock / 1000f,
    )

    Box(Modifier.fillMaxSize()) {
        Starfield(seed = level.chapter * 10 + level.number)
        BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding()) {
        // Niedrige Bildschirme (z. B. 320 × 640): Spielfeld etwas kleiner, damit Regler und „Start“ ohne Scrollen sichtbar bleiben
        val field = if (maxHeight < 720.dp) minOf(maxWidth - 24.dp, maxHeight * 0.4f) else maxWidth - 24.dp
        Column(Modifier.fillMaxSize()) {
            // Kopfzeile
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Q.Text) }
                Column(Modifier.weight(1f)) {
                    Text(header ?: if (level.number == 0) "${chapter.emoji} ${chapter.title}" else "${chapter.emoji} ${chapter.title} · Level ${level.number}", style = MaterialTheme.typography.labelMedium, color = Q.Muted)
                    Text(level.title, style = MaterialTheme.typography.titleLarge, color = Q.Text)
                }
                Text(if (attempt == 0) "" else "Versuch $attempt", style = MaterialTheme.typography.labelMedium, color = Q.Muted, modifier = Modifier.padding(end = 12.dp))
            }
            Volt(mood, Modifier.padding(horizontal = 12.dp).heightIn(min = 64.dp), size = 60.dp, speech = speech)
            // Sagt Volt gerade etwas anderes (Rückmeldung nach einem Fehlversuch), bleibt die Aufgabe trotzdem sichtbar
            AnimatedVisibility(speech != level.task && phase == Phase.AIM, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Text("📋 Aufgabe: ${level.task}", color = Q.Muted, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(Q.Panel.copy(alpha = 0.6f)).clickable { speech = level.task }.padding(horizontal = 10.dp, vertical = 5.dp))
            }
            Spacer(Modifier.height(6.dp))
            // Spielfeld
            Box(
                Modifier.align(Alignment.CenterHorizontally).size(field).clip(RoundedCornerShape(18.dp))
                    .background(Color(0x66070B20)).border(1.5.dp, Q.Cyan.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            ) {
                SpaceCanvas(spaceView, Modifier.fillMaxSize().onSizeChanged { canvasSize = it }.pointerInput(drag, phase, level.id) {
                    if (!drag || phase != Phase.AIM) return@pointerInput
                    fun world(o: Offset) = Vec(o.x / size.width * 2 * Flight.R - Flight.R, Flight.R - o.y / size.height * 2 * Flight.R)
                    detectDragGestures(
                        onDragStart = { o ->
                            val w = world(o)
                            val hs = handlesFor(level, values)
                            grabbed = hs.indices.minByOrNull { kotlin.math.hypot(hs[it].pos.x - w.x, hs[it].pos.y - w.y) }
                                ?.takeIf { kotlin.math.hypot(hs[it].pos.x - w.x, hs[it].pos.y - w.y) < 2.0 } ?: -1
                            if (grabbed >= 0) view.buzz(Buzz.TICK, vib)
                        },
                        onDragEnd = { grabbed = -1 }, onDragCancel = { grabbed = -1 },
                        onDrag = { change, _ ->
                            val h = handlesFor(level, values).getOrNull(grabbed) ?: return@detectDragGestures
                            change.consume()
                            val upd = h.apply(world(change.position))
                            if (upd.any { (k, x) -> values[k] != x }) { values = HashMap(values).apply { putAll(upd) }; sfx?.play(Sound.TICK, volume = 0.3f) }
                        },
                    )
                })
            }
            Spacer(Modifier.height(8.dp))
            // Steuerung
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Ab dem 3. Fehlversuch: Rechenweg zum Mitrechnen (nicht in Duell, Sturm, Boss)
                AnimatedVisibility(steps != null && allowRetry && attempt >= 3 && phase == Phase.AIM,
                    enter = fadeIn() + expandVertically() + scaleIn(initialScale = 0.8f)) {
                    NeonButton(onClick = { sfx?.play(Sound.TAP); walkVoltSolves = voltSolves; walkOpen = true; onWalkthrough() },
                        color = Q.Violet, modifier = Modifier.fillMaxWidth().pulse(true, 0.02f)) {
                        Text("🧮 Rechenweg mit Volt", fontWeight = FontWeight.Black)
                    }
                }
                Formula(level, values)
                if (allowRetry && !level.hidden) LiveCalc(level, values, visible = phase == Phase.AIM)
                level.params.forEachIndexed { i, p ->
                    ParamRow(p, values.getValue(p.key), paramColors[i % paramColors.size], enabled = phase == Phase.AIM) { nv ->
                        values = HashMap(values).apply { put(p.key, nv) }
                    }
                }
                // Schmale Handys (320 dp): Knöpfe nur mit Symbol, ohne „3 ⭐ möglich“ – sonst wird „Start“ zerquetscht
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                // Mit drei Knöpfen (Bahn, Theorie, Ziehen) braucht es ab normaler Handybreite nur Symbole – sonst wird „Start“ zerquetscht
                val narrow = maxWidth < 470.dp
                val tiny = maxWidth < 360.dp
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = help, onClick = { help = !help; if (!help) drag = false; sfx?.play(Sound.TICK) }, enabled = phase == Phase.AIM,
                        label = { Text(if (narrow) "👁" else if (level.kind == Kind.POINT) "👁 Hilfe" else "👁 Bahn") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Q.Violet.copy(alpha = 0.35f), labelColor = Q.Text, selectedLabelColor = Q.Text),
                    )
                    if (theoryText != null) FilterChip(
                        selected = theory, onClick = { theory = true; sfx?.play(Sound.TICK) },
                        label = { Text(if (narrow) "📖" else "📖 Theorie") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Q.Cyan.copy(alpha = 0.3f), labelColor = Q.Text, selectedLabelColor = Q.Text),
                    )
                    FilterChip(
                        selected = drag, onClick = { drag = !drag; if (drag) help = true; sfx?.play(Sound.TICK) }, enabled = phase == Phase.AIM,
                        label = { Text(if (narrow) "✋" else "✋ Ziehen") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Q.Yellow.copy(alpha = 0.3f), labelColor = Q.Text, selectedLabelColor = Q.Text),
                    )
                    Spacer(Modifier.weight(1f))
                    if (!tiny) Text(if (help) "max.\n2 ⭐" else "3 ⭐\nmöglich", style = MaterialTheme.typography.labelSmall, color = Q.Muted)
                    NeonButton(onClick = { launch() }, color = Q.Green, enabled = phase == Phase.AIM, modifier = Modifier.pulse(phase == Phase.AIM && attempt == 0, 0.03f)) {
                        Text("🚀 Start", fontWeight = FontWeight.Black, color = Q.Text, fontSize = 18.sp, maxLines = 1, softWrap = false)
                    }
                }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        }

        // Ergebnis
        AnimatedVisibility(phase == Phase.DONE, enter = fadeIn() + scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = 0.6f)), exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)) {
            ResultCard(stars, taken.size + 0, level.cells.size, usedHelp, nextLabel, allowRetry, reward,
                check = check, answer = answer, worked = steps,
                onAnswer = { i ->
                    answer = i
                    val ok = i == check?.correct
                    val before = stars
                    stars = starsWithCheck(flightStars, true, ok)
                    if (ok) { sfx?.play(if (stars > before) Sound.WIN else Sound.CELL); view.buzz(Buzz.HIT, vib); mood = VoltMood.DANCE
                        speech = if (stars > before) "Rechen-Stern! Du hast's verstanden! ⭐🧠" else "Genau so rechnet man's! 🧠" }
                    else { sfx?.play(Sound.WRONG); mood = VoltMood.IDLE; speech = "Fast! Schau dir den Rechenweg an – beim nächsten Mal klappt's. 💡" }
                    if (stars > before) onCheckStar(stars)
                },
                onPhoto = { ctx.sharePhoto(level, winPath, stars, values) },
                onRetry = {
                    answer = -1
                    phase = Phase.AIM; attempt = 0; usedHelp = false; ghosts = emptyList(); result = null; hits = emptySet(); taken = emptySet()
                    values = HashMap(level.start); mood = VoltMood.IDLE; speech = level.task; gap = null
                },
                onNext = onNext, onBack = onBack)
        }

        if (walkOpen && steps != null) WalkthroughOverlay(level, steps, walkVoltSolves) { walkOpen = false }
        if (intro) IntroOverlay(chapter) { intro = false; onIntroSeen() }
        if (theory && theoryText != null) TheoryOverlay(theoryText) { theory = false }
    }
}

/** Viertel als gemischter Bruch: 2,75 → „2¾“, −1,25 → „−1¼“ (sonst null) */
private fun frac(x: Double): String? {
    val q = de.kurvenflug.core.model.roundHalfUp(kotlin.math.abs(x) * 4)
    if (kotlin.math.abs(kotlin.math.abs(x) * 4 - q) > 1e-6 || q % 4 == 0L) return null
    val whole = q / 4; val part = when (q % 4) { 1L -> "¼"; 2L -> "½"; else -> "¾" }
    return (if (x < 0) "−" else "") + (if (whole > 0) "$whole" else "") + part
}

private fun lin(m: Double, b: Double) = "y = ${when (m) { 1.0 -> ""; -1.0 -> "−"; else -> num(m) }}x ${if (b < 0) "−" else "+"} ${num(kotlin.math.abs(b))}"

/** Die Formel mit den eingestellten Zahlen – farbig wie die Regler. */
@Composable
private fun Formula(level: Level, values: Map<String, Double>) {
    val v = level.values(values)
    val colorOf = level.params.mapIndexed { i, p -> p.key to paramColors[i % paramColors.size] }.toMap()
    val text = buildAnnotatedString {
        fun colored(key: String, s: String) {
            val c = colorOf[key]
            if (c != null) withStyle(SpanStyle(color = c, fontWeight = FontWeight.Black)) { append(s) } else append(s)
        }
        fun n(key: String, unit: String = "") = colored(key, num(v.getValue(key)) + unit)
        // Negative Zahlen in Klammern, damit „· (−2)“ eindeutig ist
        fun factor(key: String) { if (v.getValue(key) < 0) { append("("); n(key); append(")") } else n(key) }
        // Summand wie im Schulbuch: „+ 3“ bzw. „− 2“ statt „+ (−2)“; das Vorzeichen gehört zur Reglerfarbe
        fun term(key: String, sign: Int = 1) {
            val raw = v.getValue(key)
            // Bei „x − d“ bleibt für d = 0 das Minus der Scheitelform stehen
            val minus = if (sign < 0) raw >= 0 else raw < 0
            append(" "); colored(key, (if (minus) "− " else "+ ") + num(kotlin.math.abs(raw)))
        }
        when (level.kind) {
            Kind.TANGENT -> {
                val x0 = v.getValue("x0"); val y0 = Upper.f(Upper.c(v), x0)
                append("t(x) = "); factor("m"); append(" · (x ${if (x0 < 0) "+" else "−"} ${num(kotlin.math.abs(x0))}) + ${num(y0)}")
                append("\n"); withStyle(SpanStyle(color = Q.Pink, fontSize = 14.sp)) { append("f(x) = ${Upper.text(Upper.c(v))}") }
            }
            Kind.PEAK -> {
                append("Halt bei x = "); n("x")
                append("\n"); withStyle(SpanStyle(color = Q.Pink, fontSize = 14.sp)) { append("f(x) = ${Upper.text(Upper.c(v))}") }
            }
            Kind.AREA -> {
                append("∫ f dx = "); n("A")
                append("\n"); withStyle(SpanStyle(color = Q.Pink, fontSize = 14.sp)) { append("f(x) = ${Upper.text(Upper.c(v))}, ab x = 0") }
            }
            Kind.VECTOR -> {
                append("v = ( "); n("v1"); append(" | "); n("v2"); append(" )")
                append("\n"); withStyle(SpanStyle(color = Q.Muted, fontSize = 14.sp)) {
                    append("X = P + t · v,  P = (${num(v.getValue("px"))} | ${num(v.getValue("py"))}),  t = ${num(v.getValue("T"))}") }
            }
            Kind.POINT -> if (level.params.none { it.key == "y" }) {
                // Zahlenstrahl: Kommazahl und – bei Vierteln – auch als Bruch
                append("x = "); n("x"); frac(v.getValue("x"))?.let { withStyle(SpanStyle(color = Q.Muted)) { append("  = $it") } }
            } else { append("Ziel  ( "); n("x"); append(" | "); n("y"); append(" )") }
            Kind.LINE -> { append("y = "); factor("m"); append(" · x"); term("b") }
            Kind.PARABOLA -> {
                append("y = "); factor("a"); append(" · (x"); term("d", -1); append(")²"); term("e")
                level.given?.let { append("\n"); withStyle(SpanStyle(color = Q.Muted, fontSize = 14.sp)) { append("gegeben: $it") } }
            }
            Kind.THROW -> { append("α = "); n("angle", "°"); append("   v = "); n("v", " m/s") }
            Kind.ORBIT -> { append("α = "); n("angle", "°"); append("   v = "); n("v") }
            Kind.PROP -> { append("y = "); factor("k"); append(" · x") }
            Kind.ANTI -> { append("y = "); factor("k"); append(" : x") }
            Kind.LOG -> {
                append("y = log")
                withStyle(SpanStyle(baselineShift = androidx.compose.ui.text.style.BaselineShift.Subscript, fontSize = 14.sp)) { n("b") }
                append("(x)")
                if (v.getValue("d") != 0.0 || "d" in colorOf) term("d")
            }
            Kind.RADIUS -> {
                append("r = "); n("r"); append("\n")
                withStyle(SpanStyle(color = Q.Muted, fontSize = 14.sp)) {
                    append(if (level.triangle != null) "sin α = GK : r    cos α = AK : r" else "r² = Δx² + Δy²")
                }
            }
            Kind.LASER -> {
                append("α = "); n("angle", "°")
                if (level.triangle != null) { append("\n"); withStyle(SpanStyle(color = Q.Muted, fontSize = 14.sp)) { append("tan α = Höhe : Weite") } }
            }
            Kind.RAMP -> {
                append("h = "); n("h", " m"); append("   Kante h₀ = "); append(num(v.getValue("h0")) + " m")
                append("\n"); withStyle(SpanStyle(color = Q.Muted, fontSize = 14.sp)) { append("m·g·h = ½·m·v²") }
            }
            Kind.MOTION -> { append("s = "); factor("v"); append(" · t"); term("s0") }
            Kind.RENDEZVOUS -> {
                val eq = level.equations ?: listOf("A:  " + lin(v.getValue("m1"), v.getValue("b1")), "B:  " + lin(v.getValue("m2"), v.getValue("b2")))
                withStyle(SpanStyle(color = Q.Cyan)) { append(eq[0]) }; append("\n")
                withStyle(SpanStyle(color = Q.Orange)) { append(eq[1]) }; append("\n")
                append("Treffpunkt  ( "); n("x"); append(" | "); n("y"); append(" )")
            }
            Kind.SINE -> {
                append("y = "); factor("a"); append(" · sin("); factor("b")
                if ("c" in v) { append(" · (x"); term("c", -1); append("))") } else append(" · x)")
                if (v.getValue("d") != 0.0 || "d" in colorOf) term("d")
            }
            Kind.ACCEL -> { append("s = ½ · "); factor("a"); append(" · t² + "); factor("v0"); append(" · t") }
            Kind.BINOM -> {
                append("B( n = "); n("n"); append(" ; p = "); n("p"); append(" )")
                append("\n"); withStyle(SpanStyle(color = Q.Muted, fontSize = 14.sp)) {
                    append(if ((v["cum"] ?: 0.0) > 0.5) "kumuliert: P(X ≤ k) – Säulen" else "Dichte: P(X = k) – Säulen"); append(" · 1 Kästchen = 10 %") }
            }
            Kind.POWER -> {
                append("y = "); factor("a"); append(" · x")
                if ("n" in colorOf) { append("^"); n("n") } else append(when (v.getValue("n")) { 2.0 -> "²"; 3.0 -> "³"; 4.0 -> "⁴"; 5.0 -> "⁵"; 0.5 -> "^½  (√x)"; else -> "^" + num(v.getValue("n")) })
                if ((v["d"] ?: 0.0) != 0.0 || "d" in colorOf) term("d")
            }
            Kind.EXPE -> {
                append("y = "); factor("a"); append(" · e^("); factor("k"); append(" · x)")
                if ((v["d"] ?: 0.0) != 0.0) term("d")
            }
            Kind.EXP -> {
                append("y = "); factor("a"); append(" · "); factor("b"); append("ˣ")
                if (v.getValue("d") != 0.0 || "d" in colorOf) term("d")
            }
        }
    }
    Text(text, color = Q.Text, fontFamily = FontFamily.Monospace, fontSize = 20.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Q.Panel).padding(horizontal = 14.dp, vertical = 10.dp))
}

@Composable
private fun ParamRow(p: Param, value: Double, color: Color, enabled: Boolean, onChange: (Double) -> Unit) {
    val sfx = LocalSfx.current
    val view = rememberBuzzer()
    val vib = LocalVibration.current
    fun set(v: Double) {
        val s = p.snap(v)
        if (s != value) { sfx?.play(Sound.TICK, volume = 0.5f); view.buzz(Buzz.TICK, vib); onChange(s) }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(p.label, color = color, fontWeight = FontWeight.Black, modifier = Modifier.widthIn(min = 64.dp, max = 112.dp).padding(end = 4.dp), maxLines = 1)
        StepButton("−", color, enabled) { set(value - p.step) }
        Slider(
            value = value.toFloat(), onValueChange = { set(it.toDouble()) },
            valueRange = p.min.toFloat()..p.max.toFloat(), enabled = enabled,
            colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color, inactiveTrackColor = color.copy(alpha = 0.2f)),
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        )
        StepButton("+", color, enabled) { set(value + p.step) }
    }
}

@Composable
private fun StepButton(label: String, color: Color, enabled: Boolean, onClick: () -> Unit) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Box(
        Modifier.size(40.dp).pressBounce(interaction).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = if (enabled) 0.18f else 0.06f))
            .border(1.5.dp, color.copy(alpha = if (enabled) 0.8f else 0.2f), RoundedCornerShape(12.dp))
            .then(if (enabled) Modifier.clickableNoRipple(interaction, onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = color, fontSize = 22.sp, fontWeight = FontWeight.Black) }
}

private fun Modifier.clickableNoRipple(i: androidx.compose.foundation.interaction.MutableInteractionSource, onClick: () -> Unit) =
    this.then(Modifier.clickable(i, null, onClick = onClick))

@Composable
private fun ResultCard(stars: Int, cells: Int, totalCells: Int, helped: Boolean, nextLabel: String, allowRetry: Boolean,
                       reward: StickerDrop?, onPhoto: () -> Unit,
                       check: CalcCheck? = null, answer: Int = -1, worked: List<Step>? = null, onAnswer: (Int) -> Unit = {},
                       onRetry: () -> Unit, onNext: (() -> Unit)?, onBack: () -> Unit) {
    Card(
        Modifier.padding(16.dp).fillMaxWidth().navigationBarsPadding(),
        colors = CardDefaults.cardColors(containerColor = Q.Panel),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()).padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row {
                for (i in 1..3) {
                    val s = remember { Animatable(0f) }
                    val earned = i <= stars
                    LaunchedEffect(earned) { if (s.value > 0f) s.snapTo(0f); delay(if (s.value == 0f && earned && i == 3) 0L else 200L * i); s.animateTo(1f, spring(dampingRatio = 0.4f)) }
                    Text(if (i <= stars) "⭐" else "☆", fontSize = 44.sp, color = if (i <= stars) Color.Unspecified else Q.Muted,
                        modifier = Modifier.graphicsLayer { scaleX = s.value; scaleY = s.value; rotationZ = (1 - s.value) * -90f })
                }
            }
            if (totalCells > 0) Text("⚡ $cells von $totalCells Energiezellen", color = Q.Yellow, style = MaterialTheme.typography.bodyMedium)
            if (helped && stars == 2 && check == null) Text("Ohne Zielhilfe gibt's 3 ⭐ – traust du dich?", color = Q.Muted, style = MaterialTheme.typography.bodySmall)
            if (check != null) CalcCheckCard(check, answer, worked, onAnswer)
            reward?.let { d ->
                // Überraschungs-Sticker: ploppt nach den Sternen herein
                val pop = remember { Animatable(0f) }
                LaunchedEffect(d) { delay(900); pop.animateTo(1f, spring(dampingRatio = 0.4f)) }
                Row(Modifier.padding(top = 10.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value; alpha = pop.value.coerceIn(0f, 1f) },
                    verticalAlignment = Alignment.CenterVertically) {
                    de.kurvenflug.app.ui.collection.StickerTile(d.sticker, true, d.glitter, Modifier.width(84.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(if (d.new) "🎁 Neuer Sticker!" else "🎁 Sticker (schon im Album)", color = Q.Yellow, fontWeight = FontWeight.Black)
                        Text("${d.sticker.name}${if (d.glitter) " ✨ Glitzer!" else ""} · ${d.sticker.rarity.label}", color = Q.Muted,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            TextButton(onClick = onPhoto) { Text("📸 Flugbahn als Bild teilen", color = Q.Cyan) }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (allowRetry) NeonButton(onClick = onRetry, color = Q.Violet) { Text("↻ Nochmal", color = Q.Text) }
                if (onNext != null) NeonButton(onClick = onNext, color = Q.Green, modifier = Modifier.pulse(true, 0.03f)) { Text(nextLabel, color = Q.Text, fontWeight = FontWeight.Black) }
                else NeonButton(onClick = onBack, color = Q.Green) { Text("Fertig! 🏆", color = Q.Text, fontWeight = FontWeight.Black) }
            }
        }
    }
}

/** 📖 Theorie zum Nachlesen: alle Erklärungen des Kapitels auf einen Blick, plus Merkformel. */
@Composable
fun TheoryOverlay(t: de.kurvenflug.core.model.Theory, onClose: () -> Unit) {
    val sfx = LocalSfx.current
    LaunchedEffect(Unit) { sfx?.play(Sound.BEEP, volume = 0.5f) }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    Box(Modifier.fillMaxSize().background(Color(0xF0070B20)).clickableNoRipple(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {}) {
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("📖 ${t.emoji} ${t.title}", style = MaterialTheme.typography.headlineSmall, color = Q.Cyan, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(t.grades, color = Q.Muted, modifier = Modifier.weight(1f))
                // alles am Stück vorlesen
                de.kurvenflug.app.ui.fx.SpeakButton({ de.kurvenflug.core.model.ReadTexts.theoryAll(t) })
            }
            Volt(VoltMood.HAPPY, size = 64.dp, speech = "Alles Wichtige zum Nachlesen – das kostet keine Sterne! 💡")
            t.points.forEachIndexed { i, text ->
                val a by animateFloatAsState(if (shown) 1f else 0f, tween(350, delayMillis = 90 * i), label = "theory$i")
                Row(Modifier.fillMaxWidth().graphicsLayer { alpha = a; translationY = (1 - a) * 40f }.clip(RoundedCornerShape(16.dp))
                    .background(Q.Panel).border(1.dp, Q.Cyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp)).padding(12.dp)) {
                    Text("${i + 1}", color = Q.Cyan, fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.width(28.dp))
                    Text(text, color = Q.Text, modifier = Modifier.weight(1f))
                    de.kurvenflug.app.ui.fx.SpeakButton({ text }, Modifier.padding(start = 6.dp))
                }
            }
            t.formula?.let { f ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Q.Yellow.copy(alpha = 0.12f)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("💡 $f", color = Q.Yellow, modifier = Modifier.weight(1f))
                    de.kurvenflug.app.ui.fx.SpeakButton({ f }, Modifier.padding(start = 6.dp))
                }
            }
            NeonButton(onClick = onClose, color = Q.Cyan, modifier = Modifier.fillMaxWidth()) {
                Text("Verstanden – weiter üben! 🚀", color = Q.Text, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Volt erklärt das Kapitel in wenigen kurzen Schritten. */
@Composable
fun IntroOverlay(chapter: Chapter, onDone: () -> Unit) {
    // Story: Volt begrüßt den Planeten, dann die Erklärung
    val pages = remember(chapter.number) { listOfNotNull(Story.arrival(chapter.number)) + chapter.intro }
    var page by rememberSaveable { mutableIntStateOf(0) }
    val sfx = LocalSfx.current
    LaunchedEffect(page) { sfx?.play(Sound.BEEP, volume = 0.5f) }
    Box(Modifier.fillMaxSize().background(Color(0xE6070B20)).clickableNoRipple(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {}, contentAlignment = Alignment.Center) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("${chapter.emoji} ${chapter.title}", style = MaterialTheme.typography.headlineMedium, color = Q.Cyan)
            Text(chapter.grades, color = Q.Muted)
            AnimatedContent(page, transitionSpec = { (slideInHorizontally { it / 2 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 2 } + fadeOut()) }, label = "intro") { p ->
                Volt(if (p == pages.lastIndex) VoltMood.HAPPY else VoltMood.IDLE, size = 84.dp, speech = pages[p])
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pages.indices.forEach { i -> Box(Modifier.size(if (i == page) 10.dp else 7.dp).clip(RoundedCornerShape(50)).background(if (i == page) Q.Cyan else Q.Muted)) }
            }
            NeonButton(onClick = { if (page < pages.lastIndex) page++ else onDone() }, color = Q.Cyan) {
                Text(if (page < pages.lastIndex) "Weiter ›" else "Los geht's! 🚀", color = Q.Text, fontWeight = FontWeight.Bold)
            }
            if (page < pages.lastIndex) TextButton(onClick = onDone) { Text("Überspringen", color = Q.Muted) }
        }
    }
}


/** Die Rechnung läuft beim Schieben mit: „17 · tan 13° ≈ 3,9 · Station: 4 ↑“ */
@Composable
private fun LiveCalc(level: Level, values: Map<String, Double>, visible: Boolean) {
    val line = remember(level.id, values) { Understand.live(level, values) } ?: return
    val ok = line.endsWith("✓")
    // Bleibt beim Flug im Bild (nur unsichtbar) – wegfallende Knoten verwirren die Barrierefreiheit im Browser
    Text(line, color = if (ok) Q.Green else Q.Cyan, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace,
        modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = if (visible) 1f else 0f }.clip(RoundedCornerShape(10.dp)).background(Q.Panel.copy(alpha = 0.6f))
            .border(1.dp, (if (ok) Q.Green else Q.Cyan).copy(alpha = 0.35f), RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 6.dp))
}

/**
 * Rechen-Stern: drei Rechnungen, eine stimmt. Danach der ganze Lösungsweg mit den Zahlen des Levels
 * (ausgearbeitetes Beispiel), Schritt für Schritt eingeblendet.
 */
@Composable
private fun CalcCheckCard(check: CalcCheck, answer: Int, worked: List<Step>?, onAnswer: (Int) -> Unit) {
    Column(Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Q.Space.copy(alpha = 0.6f))
        .border(1.dp, Q.Violet.copy(alpha = 0.6f), RoundedCornerShape(16.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🧠 Rechen-Stern: ${check.question}", color = Q.Text, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            de.kurvenflug.app.ui.fx.SpeakButton({
                if (answer < 0) de.kurvenflug.core.model.ReadTexts.calcQuestion(check) else de.kurvenflug.core.model.ReadTexts.calcAnswer(check, worked)
            })
        }
        check.options.forEachIndexed { i, o ->
            val right = i == check.correct
            val col = when {
                answer < 0 -> Q.Cyan
                right -> Q.Green
                i == answer -> Q.Orange
                else -> Q.Muted
            }
            val wiggle = remember { Animatable(0f) }
            LaunchedEffect(answer) { if (answer == i && !right) { repeat(3) { wiggle.animateTo(8f, tween(50)); wiggle.animateTo(-8f, tween(50)) }; wiggle.animateTo(0f) } }
            val pop = remember { Animatable(1f) }
            LaunchedEffect(answer) { if (answer >= 0 && right) { pop.animateTo(1.08f, tween(120)); pop.animateTo(1f, spring(dampingRatio = 0.4f)) } }
            Text((if (answer >= 0 && right) "✓ " else if (answer == i) "✗ " else "") + o,
                color = if (answer < 0) Q.Text else col, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().graphicsLayer { translationX = wiggle.value; scaleX = pop.value; scaleY = pop.value }
                    .clip(RoundedCornerShape(12.dp)).border(2.dp, col.copy(alpha = if (answer < 0) 0.6f else 1f), RoundedCornerShape(12.dp))
                    .background(col.copy(alpha = if (answer >= 0 && right) 0.18f else 0.06f))
                    .clickable { if (answer < 0) onAnswer(i) }.padding(horizontal = 12.dp, vertical = 10.dp))
        }
        if (answer >= 0 && worked != null) {
            Text("📝 So rechnet man's", color = Q.Yellow, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            // Schritt für Schritt einblenden, wie an der Tafel
            val reveal = remember(worked) { Animatable(0f) }
            LaunchedEffect(worked) { reveal.animateTo(worked.size.toFloat(), tween(400 * worked.size, easing = LinearEasing)) }
            worked.forEachIndexed { i, st ->
                Text("${i + 1}. ${st.solved()}", color = Q.Text, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.graphicsLayer { val a = (reveal.value - i).coerceIn(0f, 1f); alpha = a; translationX = (1 - a) * 40f })
            }
        }
    }
}
