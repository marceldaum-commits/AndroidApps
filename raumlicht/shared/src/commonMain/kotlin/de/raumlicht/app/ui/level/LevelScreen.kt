package de.raumlicht.app.ui.level

import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val X = listOf("x₁", "x₂", "x₃")

/**
 * Ein Aufgabe: oben die 3D-Bühne, darunter Lux' Auftrag, die Formel und die Regler.
 * Einstellen (Griff ziehen oder Zahlen tippen) → „Los“ → Lux fliegt bzw. der Laser feuert.
 */
@Composable
fun LevelScreen(
    level: Level, chapter: Chapter, showIntro: Boolean, onIntroSeen: () -> Unit,
    onSolved: (stars: Int, attempts: Int) -> Unit, onNext: (() -> Unit)?, onBack: () -> Unit,
    initial: Values? = null, controller: StageController = remember(level.id) { StageController() },
    /** Eigene Überschrift (Jam-Session, Tagesaufgabe) statt „Aufgabe 1-2“ */
    caption: String? = null,
    /** Kleine Anzeige rechts oben, z. B. die Serie in der Jam-Session */
    badge: String? = null,
    doneLabel: String = "Tour geschafft! 🏆",
    /** Zusatzknopf oben links in der Bühne, z. B. „🎢 Mitfahren“ im Achterbahn-Kapitel */
    extra: Pair<String, () -> Unit>? = null,
    /** Ohne Hilfsmittel: kein Tipp-Knopf (Pflichtteil) */
    strict: Boolean = false,
    /** Sachzusammenhang, der über mehrere Teilaufgaben gilt (Prüfungsaufgaben: „Ein Lichtbogen hat die Form von f(x) = …“) – bleibt bei jeder Teilaufgabe sichtbar */
    context: String? = null,
) {
    // gewählte Vektorschreibweise (Einstellungen): dieselbe Aufgabe, nur anders aufgeschrieben
    val strictVectors = de.raumlicht.app.ui.settings.LocalNotation.current.strict
    @Suppress("NAME_SHADOWING") val level = remember(level.id, strictVectors) { if (strictVectors) level.withText(de.raumlicht.core.model.Notation::strict) else level }
    // Theorie zur Aufgabe: jederzeit nachlesbar, kostet keinen Stern (nur im Pflichtteil „ohne Hilfsmittel“ nicht)
    var theory by remember(level.id) { mutableStateOf(false) }
    // KI-Tutor: nach einem Fehlversuch bietet Lux an, es anders zu erklären (nicht im Pflichtteil „ohne Hilfsmittel“)
    val ki = LocalKi.current
    // Anonyme Zählung (abschaltbar): nur „gestartet / geschafft / Theorie geöffnet“ mit dem Gebiet – nie die Aufgabe, nie Sterne oder Versuche
    val count = de.raumlicht.app.ui.fx.LocalCount.current
    val topic = remember(chapter.number) { if (chapter.number > 0) de.raumlicht.core.model.Zaehler.topic(chapter.area) else null }
    LaunchedEffect(level.id) { count("level_start", null, topic) }
    var kiOpen by remember(level.id) { mutableStateOf(false) }
    var lastTry by remember(level.id) { mutableStateOf<Pair<Values, String>?>(null) }
    var values by remember(level.id) { mutableStateOf(initial ?: level.start) }
    var phase by remember(level.id) { mutableStateOf(Phase.AIM) }
    var attempts by remember(level.id) { mutableIntStateOf(0) }
    var usedTip by remember(level.id) { mutableStateOf(false) }
    var busy by remember(level.id) { mutableStateOf(false) }
    var says by remember(level.id) { mutableStateOf(level.mission to LuxMood.IDLE) }
    var override by remember(level.id) { mutableStateOf<String?>(null) }
    var stars by remember(level.id) { mutableIntStateOf(0) }
    var intro by remember(level.id) { mutableStateOf(showIntro) }
    var introStep by remember(level.id) { mutableIntStateOf(0) }
    val sink = LocalMistakeSink.current
    var taps by remember { mutableIntStateOf(0) }
    var showInfo by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sfx = LocalSfx.current; val view = LocalBuzzer.current; val vib = LocalVibration.current
    val accent = stageColor(chapter.color)

    fun change(key: String, v: List<Int>) {
        if (busy || phase == Phase.DONE) return
        val n = level.set(values, key, v)
        if (n != values) { values = n; override = null; sfx?.play(Sound.TICK); view.buzz(Buzz.TICK, vib) }
    }
    // Während Lux das Kapitel erklärt, führt die Bühne die erste Aufgabe vor: Ausgangslage → eingestellt → gelöst
    // Lux liest jede neue Sprechblase vor (abschaltbar); während der Kapitel-Erklärung spricht die Erklärung
    val speaker = LocalSpeaker.current
    LaunchedEffect(says.first, intro, introStep) { speaker?.say(if (intro) chapter.intro.getOrElse(introStep) { "" } else says.first) }
    LaunchedEffect(values, phase, override, intro, introStep) {
        controller.setState(when {
            !intro -> override ?: level.scene(values, phase)
            introStep == 0 -> level.scene(level.start, Phase.AIM)
            introStep >= chapter.intro.lastIndex -> level.scene(level.solution, Phase.DONE)
            else -> level.scene(level.solution, Phase.AIM)
        })
        if (intro && introStep >= chapter.intro.lastIndex && introStep > 0) controller.party(1.5)
    }
    DisposableEffect(controller, level.id) {
        controller.onDrag = { _, p -> level.handle?.let { h -> change(h.control, p.mapIndexed { i, x -> x - h.anchor.ints[i] }) } }
        onDispose { controller.onDrag = null }
    }

    fun go() {
        if (busy || phase == Phase.DONE) return
        busy = true; attempts++
        val out = level.evaluate(values)
        scope.launch {
            sfx?.play(Sound.LAUNCH)
            out.flyTo?.let { to ->
                override = level.encode(level.objects(values, Phase.AIM).map { if (it.type == "drone") it.copy(p = to.list) else it })
                delay(1100)
            }
            // Lichtshow zum Hören: jeder Strahl ein Ton desselben Glas-Akkords; geht Licht durch eine Scheibe, schillert es
            val glass = level.objects(values, Phase.AIM).any { it.type == "plane" || it.type == "pane" }
            out.beams.forEachIndexed { i, b ->
                val ms = ((b.to - b.from).length * 110).toInt().coerceIn(450, 1100)
                controller.fire(b, ms); delay(ms + 200L)
                if (b.hit) { if (glass && i == 0) sfx?.play(Sound.REFRACT, volume = 0.7f); sfx?.play(Sound.HIT, de.raumlicht.core.model.Klang.beamPitch(i)); view.buzz(Buzz.HIT, vib) }
            }
            if (out.success) {
                stars = Level.stars(attempts, usedTip)
                // Finale: Lux fährt eine Strecke ab und/oder blitzt Ziele an – je nach Aufgabe
                val aim = level.objects(values, Phase.AIM)
                val fin = level.finale?.invoke(values) ?: Finale.default(aim, level.objects(values, Phase.DONE), out)
                fin.path.forEach { pt -> override = level.encode(aim.map { if (it.type == "drone") it.copy(p = pt.list) else it }); delay(90) }
                fin.beams.forEachIndexed { i, b -> controller.fire(b, 240); sfx?.play(Sound.HIT, de.raumlicht.core.model.Klang.beamPitch(i), 0.8f); view.buzz(Buzz.TICK, vib); delay(170) }
                if (fin.beams.isNotEmpty()) delay(260)
                // Drehbuch: Schritt für Schritt das tun, wonach gefragt war – Lux sagt dazu, was gerade passiert
                fin.acts.forEach { act ->
                    says = act.say to LuxMood.HAPPY
                    act.beams.forEachIndexed { i, b -> controller.fire(b, 260); sfx?.play(Sound.HIT, de.raumlicht.core.model.Klang.beamPitch(i), 0.8f); view.buzz(Buzz.TICK, vib); delay(200) }
                    act.objects?.let { o -> override = level.encode(o + aim.filter { it.type == "drone" }) }
                    delay(act.ms.toLong())
                }
                out.flyTo?.let { controller.burst(it, "green") }
                // letzte Aufgabe eines Kapitels: die Erkennungsmelodie festlich
                controller.party(); sfx?.play(if (chapter.levels.lastOrNull()?.id == level.id) Sound.CHAPTER else Sound.WIN); view.buzz(Buzz.HIT, vib)
                override = null; phase = Phase.DONE
                says = out.message to LuxMood.HAPPY
                onSolved(stars, attempts); count("level_geschafft", null, topic)
            } else {
                sfx?.play(Sound.WRONG); view.buzz(Buzz.CRASH, vib)
                out.flyTo?.let { to -> override = level.encode(level.objects(values, Phase.AIM).map { if (it.type == "drone") it.copy(p = to.list, mood = "sad") else it }) }
                lastTry = values to out.message
                if (MistakeBook.isNamed(out.message)) sink(level.id, out.message)      // typischer Fehler → ins Fehler-Tagebuch
                says = (out.message + if (attempts == 2 && !usedTip && !strict) de.raumlicht.core.model.Vorlesen.TIP_OFFER else "") to LuxMood.SAD
            }
            busy = false
        }
    }

    Box(Modifier.fillMaxSize().background(Q.Space)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            // Kopfzeile
            Row(Modifier.fillMaxWidth().padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { BackArrow() }
                Column(Modifier.weight(1f)) {
                    Text(caption ?: "${chapter.emoji} ${chapter.title} · Aufgabe ${level.id}", style = MaterialTheme.typography.labelMedium, color = accent)
                    Text(level.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                }
                if (level.abi) Text("🎓 Abi", Modifier.padding(end = 8.dp).background(Q.Orange.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).border(1.dp, Q.Orange, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
                    color = Q.Orange, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                if (badge != null) Text(badge, Modifier.padding(end = 8.dp), color = Q.Orange, fontWeight = FontWeight.Black, fontSize = 15.sp)
                StarRow(if (phase == Phase.DONE) stars else Level.stars(attempts + 1, usedTip), lit = phase == Phase.DONE)
            }
            // Bühne – runde Ecken kommen als Rahmen ÜBER der Bühne (StageFrame); die Web-Ansicht selbst wird nicht beschnitten.
            val stageBox: @Composable (Modifier) -> Unit = { m ->
                Box(m.padding(horizontal = 8.dp)) {
                    Stage(controller, Modifier.fillMaxSize())
                    StageFrame(accent)
                    if (extra != null) Text(extra.first, Modifier.align(Alignment.TopStart).padding(10.dp).clip(RoundedCornerShape(12.dp)).background(Q.Space.copy(alpha = 0.8f)).border(1.5.dp, Q.Yellow, RoundedCornerShape(12.dp))
                        .clickable(remember { MutableInteractionSource() }, null) { extra.second() }.padding(horizontal = 12.dp, vertical = 7.dp), color = Q.Yellow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(de.raumlicht.app.ui.stage.orbitHint, Modifier.align(Alignment.TopEnd).padding(10.dp), color = Q.Muted.copy(alpha = 0.8f), fontSize = 11.sp)
                    // Dreimal auf den Hinweis tippen zeigt die Selbstauskunft der Bühne (für Fehlersuche)
                    if (showInfo) Text(controller.info ?: "Die Bühne hat sich noch nicht gemeldet.", Modifier.align(Alignment.BottomStart).background(Q.Space.copy(alpha = 0.85f)).padding(8.dp), color = Q.Yellow, fontSize = 10.sp, lineHeight = 12.sp)
                    Box(Modifier.align(Alignment.TopEnd).size(150.dp, 40.dp).clickable(remember { MutableInteractionSource() }, null) { taps++; if (taps >= 3) { showInfo = !showInfo; taps = 0 } })
                }
            }
            // Auftrag, Formel, Regler und Knöpfe
            val panel: @Composable ColumnScope.() -> Unit = {
                // Lux spricht
                Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Lux(says.second, size = 50.dp)
                    AnimatedContent(says.first, Modifier.weight(1f), transitionSpec = { (slideInVertically { it / 3 } + fadeIn()) togetherWith fadeOut() }, label = "lux") { text ->
                        // Antippen holt die Aufgabenstellung zurück
                        Text(text, Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)).background(Q.Panel)
                            .clickable(remember { MutableInteractionSource() }, null) { if (phase == Phase.AIM) says = level.mission to LuxMood.IDLE }.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium, color = Q.Text)
                    }
                }
                // Steht gerade ein Tipp oder eine Rückmeldung da, lässt sich die Aufgabenstellung wieder einblenden
                val kiChip = ki != null && !strict && lastTry != null
                if (phase == Phase.AIM && (kiChip || says.first != level.mission)) Row(Modifier.align(Alignment.End).padding(end = 12.dp, top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (kiChip) Text("🤖 Erklär’s mir anders", Modifier.testTag("ki").pulse(!busy && !kiOpen, 0.03f).clip(RoundedCornerShape(10.dp))
                    .background(Q.Deep).border(1.dp, Q.Yellow.copy(alpha = 0.8f), RoundedCornerShape(10.dp)).clickable(remember { MutableInteractionSource() }, null) { if (!busy) kiOpen = true }
                    .padding(horizontal = 10.dp, vertical = 5.dp), color = Q.Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                if (says.first != level.mission) Text("📋 Aufgabe zeigen", Modifier.testTag("aufgabe").clip(RoundedCornerShape(10.dp))
                    .background(Q.Deep).border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(10.dp)).clickable(remember { MutableInteractionSource() }, null) { says = level.mission to LuxMood.IDLE }
                    .padding(horizontal = 10.dp, vertical = 5.dp), color = Q.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                // Formel
                if (context != null) Text(context, Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 6.dp).testTag("zusammenhang").background(Q.Deep, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodySmall, color = Q.Text)
                FormulaView(level.formula(if (phase == Phase.DONE) level.solutionShown(values) else values), { stageColor(level.control(it).color) },
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).background(Q.Deep, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 8.dp))

                if (phase == Phase.AIM) {
                    level.controls.forEach { c ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(c.name, Modifier.width(30.dp), color = stageColor(c.color), fontWeight = FontWeight.Black, fontSize = 18.sp, textAlign = TextAlign.Center)
                            repeat(c.size) { i ->
                                Stepper(if (c.kind == ControlKind.SCALAR) null else X[i], values.getValue(c.key)[i], Geo.num(values.getValue(c.key)[i] * c.step) + c.suffix, stageColor(c.color), "${c.key}-$i", Modifier.weight(1f),
                                    canDown = values.getValue(c.key)[i] > c.min[i], canUp = values.getValue(c.key)[i] < c.max[i]) { d ->
                                    change(c.key, values.getValue(c.key).toMutableList().also { it[i] += d })
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // nach dem ersten Fehlversuch winkt die Theorie – nachlesen ist ausdrücklich erwünscht
                        if (!strict) NeonButton({ theory = true; count("theorie", null, topic) }, Modifier.weight(1.25f).testTag("theorie").pulse(!busy && attempts >= 1, 0.03f), color = Q.Cyan, enabled = !busy) { Text("📖 Theorie", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1) }
                        if (!strict) NeonButton({ usedTip = true; says = level.tip to LuxMood.THINK }, Modifier.weight(1f), color = Q.Yellow, enabled = !busy) { Text("💡 Tipp", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1) }
                        NeonButton(::go, Modifier.weight(1.5f).pulse(!busy && attempts == 0, 0.025f), color = accent, enabled = !busy) { Text("▶ Los", fontWeight = FontWeight.Black, fontSize = 18.sp, maxLines = 1) }
                    }
                } else {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp).background(verticalFade(listOf(Q.Green.copy(alpha = 0.18f), Q.Deep)), RoundedCornerShape(16.dp))
                        .border(1.5.dp, Q.Green.copy(alpha = 0.6f), RoundedCornerShape(16.dp)).padding(12.dp)) {
                        Text("So rechnet man’s", color = Q.Green, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        level.way.forEachIndexed { i, s -> Text("${i + 1}.  $s", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp)) }
                    }
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NeonButton({ values = level.start; attempts = 0; usedTip = false; phase = Phase.AIM; says = level.mission to LuxMood.IDLE }, Modifier.weight(1f), color = Q.Muted) { Text("↻ Neu") }
                        NeonButton(onNext ?: onBack, Modifier.weight(2f).pulse(true, 0.025f), color = Q.Green) { Text(if (onNext != null) "Weiter ▸" else doneLabel, fontWeight = FontWeight.Black, fontSize = 18.sp) }
                    }
                }
            }
            // Hochformat: Bühne oben, Regler darunter. Querformat und Tablet quer: Bühne links groß, Regler rechts.
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                if (maxWidth > maxHeight * 1.15f) Row(Modifier.fillMaxSize()) {
                    stageBox(Modifier.weight(1.25f).fillMaxHeight().padding(bottom = 8.dp))
                    Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) { panel() }
                } else Column(Modifier.fillMaxSize()) {
                    stageBox(Modifier.fillMaxWidth().weight(1f))
                    panel()
                }
            }
        }
        if (intro) IntroOverlay(chapter, onStep = { introStep = it }) { intro = false; onIntroSeen() }
        TheorySheet(if (theory) remember(level.id) { de.raumlicht.core.model.Theory.forLevel(level) } else null, accent) { theory = false }
        KiSheet(kiOpen && phase == Phase.AIM, level, lastTry, accent) { kiOpen = false }
    }
}

/** Nach dem Lösen zeigt die Formel die Lösung. */
private fun Level.solutionShown(values: Values): Values = if (evaluate(values).success) values else solution

@Composable
fun StarRow(n: Int, lit: Boolean, size: Int = 20) {
    Row {
        repeat(3) { i ->
            val on = i < n
            val s by animateFloatAsState(if (on && lit) 1f else 0.8f, spring(dampingRatio = 0.35f, stiffness = 300f), label = "stern")
            Text("★", fontSize = size.sp, color = if (on) (if (lit) Q.Yellow else Q.Yellow.copy(alpha = 0.55f)) else Q.Muted.copy(alpha = 0.3f),
                modifier = Modifier.graphicsLayer { scaleX = s; scaleY = s })
        }
    }
}

@Composable
fun Stepper(label: String?, value: Int, text: String, color: Color, tag: String, modifier: Modifier, canDown: Boolean, canUp: Boolean, onStep: (Int) -> Unit) {
    Row(modifier.height(46.dp).background(Q.Deep, RoundedCornerShape(14.dp)).border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(14.dp)), verticalAlignment = Alignment.CenterVertically) {
        StepButton("−", color, canDown, "minus-$tag", "${label ?: "Wert"} kleiner") { onStep(-1) }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            if (label != null) de.raumlicht.app.ui.fx.FitText(label, fontSize = 10.sp, min = 7f, color = Q.Muted, style = androidx.compose.material3.LocalTextStyle.current.copy(lineHeight = 10.sp))
            // Blättert zwischen den fertig beschrifteten Werten (z. B. „20 %“ → „30 %“) – nie über den internen Zählwert
            AnimatedContent(value to text, transitionSpec = { (slideInVertically { if (targetState.first > initialState.first) it else -it } + fadeIn()) togetherWith fadeOut() },
                contentKey = { it.first }, label = "wert") {
                // auf schmalen Handys wird ein langer Wert („50 % rechts“) kleiner statt umzubrechen
                de.raumlicht.app.ui.fx.FitText(it.second, fontSize = 19.sp, min = 11f, fontWeight = FontWeight.Black, color = color, style = androidx.compose.material3.LocalTextStyle.current.copy(lineHeight = 20.sp))
            }
        }
        StepButton("+", color, canUp, "plus-$tag", "${label ?: "Wert"} größer") { onStep(1) }
    }
}

@Composable
private fun StepButton(sign: String, color: Color, enabled: Boolean, tag: String, name: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    // [name] sagt Vorlese-Hilfen, was der Knopf tut („x₁ größer“) – „plus“ allein hilft nicht
    Box(Modifier.testTag(tag).size(42.dp).pressBounce(interaction).clip(CircleShape).clickable(interaction, null, enabled = enabled, role = androidx.compose.ui.semantics.Role.Button, onClickLabel = name, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(sign, Modifier.clearAndSetSemantics { contentDescription = name }, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (enabled) color else Q.Muted.copy(alpha = 0.3f))
    }
}

/** Typische Fehler gehen an das Fehler-Tagebuch: (Aufgaben-ID, Meldung) */
val LocalMistakeSink = staticCompositionLocalOf<(String, String) -> Unit> { { _, _ -> } }

/**
 * Lux erklärt das Kapitel – einmal, Schritt für Schritt. Die Karte sitzt unten, damit man oben in der Bühne sieht,
 * wie die erste Aufgabe vorgeführt wird ([onStep] meldet den aktuellen Schritt).
 */
@Composable
fun IntroOverlay(chapter: Chapter, onStep: (Int) -> Unit = {}, onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(step) { onStep(step) }
    val accent = stageColor(chapter.color)
    Box(Modifier.fillMaxSize().background(verticalFade(0f to Color.Transparent, 0.45f to Q.Space.copy(alpha = 0.55f), 1f to Q.Space)).clickable(remember { MutableInteractionSource() }, null) {}, contentAlignment = Alignment.BottomCenter) {
        Column(Modifier.systemBarsPadding().padding(12.dp).background(Q.Deep, RoundedCornerShape(24.dp)).border(2.dp, accent, RoundedCornerShape(24.dp)).padding(20.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${chapter.emoji}  ${chapter.venue}", color = accent, style = MaterialTheme.typography.labelLarge)
            Text(chapter.title, style = MaterialTheme.typography.headlineMedium)
            Text(chapter.topic, color = Q.Muted)
            Lux(if (step == chapter.intro.lastIndex) LuxMood.HAPPY else LuxMood.IDLE, size = 64.dp)
            AnimatedContent(step, transitionSpec = { (slideInVertically { it / 2 } + fadeIn()) togetherWith fadeOut() }, label = "intro") { i ->
                Text(chapter.intro[i], style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                chapter.intro.indices.forEach { Box(Modifier.size(8.dp).background(if (it == step) accent else Q.Muted.copy(alpha = 0.4f), CircleShape)) }
            }
            NeonButton({ if (step < chapter.intro.lastIndex) step++ else onDone() }, Modifier.fillMaxWidth(), color = accent) {
                Text(if (step < chapter.intro.lastIndex) "Weiter ›" else "Licht an! ▶", fontWeight = FontWeight.Black)
            }
        }
    }
}
