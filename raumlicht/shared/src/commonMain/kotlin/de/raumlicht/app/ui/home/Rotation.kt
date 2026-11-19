package de.raumlicht.app.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.raumlicht.app.ui.fx.LocalSfx
import de.raumlicht.app.ui.fx.Sound
import de.raumlicht.app.ui.level.Stepper
import de.raumlicht.app.ui.lux.Lux
import de.raumlicht.app.ui.lux.LuxMood
import de.raumlicht.app.ui.stage.Stage
import de.raumlicht.app.ui.stage.StageController
import de.raumlicht.app.ui.stage.StageFrame
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.core.model.Bounds
import de.raumlicht.core.model.Erleben
import de.raumlicht.core.model.Geo
import de.raumlicht.core.model.RotationLab
import de.raumlicht.core.model.V3
import kotlin.math.round

/**
 * Rotationskörper-Labor: Kurve wählen, Zahlen und Grenzen einstellen – die Kurve dreht sich um die x-Achse und der Körper wächst im Raum.
 * Darunter steht das Volumen mit dem ganzen Rechenweg. Hier wird nichts abgefragt: ausprobieren, zusehen, verstehen. Rechnen übt man im Mathe-Trainer.
 */
@Composable
fun RotationScreen(onBack: () -> Unit, onTrainer: () -> Unit, controller: StageController = remember { StageController() }) {
    val start = RotationLab.presets.first().second
    var shape by rememberSaveable { mutableStateOf(start.shape.name) }
    var p by rememberSaveable { mutableIntStateOf(start.p) }; var q by rememberSaveable { mutableIntStateOf(start.q) }
    var a by rememberSaveable { mutableIntStateOf(start.a) }; var b by rememberSaveable { mutableIntStateOf(start.b) }
    var showWay by rememberSaveable { mutableStateOf(true) }
    val sh = RotationLab.Shape.valueOf(shape)
    val setup = RotationLab.Setup(sh, p, q, a, b)
    val problem = RotationLab.problem(setup)
    val sfx = LocalSfx.current
    // Der Körper wächst von der linken zur rechten Grenze – bei jeder gültigen Änderung neu
    val grow = remember { Animatable(1f) }
    var shown by remember { mutableStateOf(setup) }                                       // zuletzt gültige Einstellung: bleibt stehen, solange die Eingabe nicht passt
    LaunchedEffect(setup) {
        if (problem != null) return@LaunchedEffect
        shown = setup; grow.snapTo(0.04f); grow.animateTo(1f, tween(850)); sfx?.play(Sound.HIT); controller.party(1.0)
    }
    LaunchedEffect(shown, grow.value) {
        val to = shown.a + (shown.b - shown.a) * grow.value.toDouble()
        val profile = RotationLab.profile(shown).filter { it.first <= to + 1e-9 }.ifEmpty { RotationLab.profile(shown).take(2) }
        controller.setState(de.raumlicht.core.model.Stage.encode(Bounds(), listOf(de.raumlicht.core.model.Stage.lathe("körper", V3(4.0, 4.0 - (shown.a + shown.b) / 2.0, 3.0), profile, "violet"))))
    }
    LaunchedEffect(controller) { controller.camera(az = 0.9, el = 0.32, dist = 15.5) }

    fun use(s: RotationLab.Setup) { shape = s.shape.name; p = s.p; q = s.q; a = s.a; b = s.b }
    fun pick(next: RotationLab.Shape) { val pr = RotationLab.presets.first { it.second.shape == next }.second; use(pr) }

    val controls: @Composable ColumnScope.() -> Unit = {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Lux(if (problem != null) LuxMood.THINK else LuxMood.HAPPY, size = 46.dp)
            Text(problem ?: "${RotationLab.text(setup)} dreht sich um die x-Achse: ${RotationLab.name(setup)}.",
                Modifier.weight(1f).testTag("rotation-satz").background(Q.Panel, RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                color = if (problem != null) Q.Orange else Q.Text, fontSize = 14.sp)
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RotationLab.presets.forEach { (name, s) -> Pick(name, s == setup, Q.Violet, "rotation-vorlage-$name") { sfx?.play(Sound.TAP); use(s) } }
        }
        Text("EIGENE KURVE", Modifier.padding(start = 14.dp, top = 6.dp), color = Q.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RotationLab.Shape.entries.forEach { s -> Pick("${s.emoji} ${s.title}", s == sh, Q.Cyan, "rotation-form-${s.name}") { sfx?.play(Sound.TAP); if (s != sh) pick(s) } }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val rp = RotationLab.RANGE_P.getValue(sh)
            Stepper(sh.pLabel, p, if (sh == RotationLab.Shape.PARABEL) Geo.num(p / 10.0) else "$p", Q.Cyan, "rotation-p", Modifier.weight(1f), canDown = p > rp.first, canUp = p < rp.last) { p = (p + it).coerceIn(rp.first, rp.last) }
            val rq = RotationLab.RANGE_Q[sh]
            if (rq != null && sh.qLabel != null) Stepper(sh.qLabel, q, "$q", Q.Cyan, "rotation-q", Modifier.weight(1f), canDown = q > rq.first, canUp = q < rq.last) { q = (q + it).coerceIn(rq.first, rq.last) }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val rx = RotationLab.RANGE_X
            Stepper("von x =", a, "$a", Q.Yellow, "rotation-a", Modifier.weight(1f), canDown = a > rx.first, canUp = a < rx.last) { a = (a + it).coerceIn(rx.first, rx.last) }
            Stepper("bis x =", b, "$b", Q.Yellow, "rotation-b", Modifier.weight(1f), canDown = b > rx.first, canUp = b < rx.last) { b = (b + it).coerceIn(rx.first, rx.last) }
        }
        if (problem == null) {
            val v = RotationLab.piTimes(setup)
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Dial("Volumen", "$v · π", "exakt", Q.Violet, Modifier.weight(1f).testTag("rotation-volumen"))
                Dial("gerundet", "≈ " + Geo.num(round(RotationLab.volume(setup) * 100) / 100), "Volumeneinheiten", Q.Yellow, Modifier.weight(1f))
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp).clip(RoundedCornerShape(16.dp)).background(Q.Deep).border(1.dp, Q.Violet.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .clickable { showWay = !showWay }.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(if (showWay) "SO WIRD GERECHNET ▾" else "SO WIRD GERECHNET ▸", Modifier.testTag("rotation-weg-kopf"), color = Q.Violet, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
                if (showWay) {
                    RotationLab.way(setup).forEachIndexed { i, line -> Text("${i + 1}. $line", Modifier.testTag("rotation-weg-$i"), color = Q.Text, fontSize = 14.sp, lineHeight = 19.sp) }
                    RotationLab.check(setup)?.let { Text(it, Modifier.testTag("rotation-probe"), color = Q.Green, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
        Text("Rechnen üben im Mathe-Trainer ›", Modifier.padding(horizontal = 10.dp, vertical = 6.dp).testTag("rotation-trainer").clip(RoundedCornerShape(12.dp)).border(1.dp, Q.Cyan.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .clickable(onClick = onTrainer).padding(horizontal = 12.dp, vertical = 10.dp), color = Q.Cyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
    }

    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding()) {
        Bar("Rotationskörper-Labor", onBack)
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            if (maxWidth > maxHeight * 1.15f) Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1.1f).fillMaxHeight().padding(horizontal = 8.dp)) { Stage(controller, Modifier.fillMaxSize()); StageFrame(Q.Violet) }
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), content = controls)
            } else Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().weight(0.8f).padding(horizontal = 8.dp)) { Stage(controller, Modifier.fillMaxSize()); StageFrame(Q.Violet) }
                Column(Modifier.fillMaxWidth().weight(1.2f).verticalScroll(rememberScrollState()), content = controls)
            }
        }
    }
}

/** Fußteil der Seite „Analysis & Zufall erleben“: Wer rechnen üben will, findet die passenden Einheiten im Mathe-Trainer */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrainerLinks(onOpen: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(18.dp)).background(Q.Deep).border(1.dp, Q.Cyan.copy(alpha = 0.55f), RoundedCornerShape(18.dp)).padding(14.dp).testTag("trainer-links"),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Mehr üben im Mathe-Trainer", color = Q.Cyan, fontWeight = FontWeight.Black, fontSize = 15.sp)
        Text(Erleben.GONE + " Dort übst du in Stufen, mit jedem Rechenschritt.", color = Q.Text, fontSize = 13.sp, lineHeight = 18.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Erleben.links.forEach { l -> Text(l.title + " ›", Modifier.testTag("trainer-${l.unit}").clip(RoundedCornerShape(10.dp)).border(1.dp, Q.Cyan.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                .clickable { onOpen(l.url) }.padding(horizontal = 10.dp, vertical = 8.dp), color = Q.Cyan, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        }
    }
}
