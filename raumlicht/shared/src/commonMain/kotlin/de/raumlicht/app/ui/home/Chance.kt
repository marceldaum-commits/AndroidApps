package de.raumlicht.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.raumlicht.app.ui.fx.*
import de.raumlicht.app.ui.lux.Lux
import de.raumlicht.app.ui.lux.LuxMood
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.core.model.ChanceLab
import de.raumlicht.core.model.ChanceRun
import de.raumlicht.core.model.Geo
import kotlin.math.roundToInt

/**
 * Zufallsmaschine: Münze, Würfel, Glücksrad oder eine eigene Wahrscheinlichkeit – viele Male in Sekunden.
 * Erst wetten („Wie oft klappt es?“), dann zusehen, wie sich die relative Häufigkeit der Wahrscheinlichkeit nähert.
 */
@Composable
fun ChanceScreen(onBack: () -> Unit, seed: Long = remember { nowNanos() }) {
    var preset by rememberSaveable { mutableStateOf(ChanceLab.presets.first().id) }       // "eigene" = eigene Prozentzahl
    var percent by rememberSaveable { mutableIntStateOf(30) }
    // Text im Eingabefeld: darf zwischendurch leer sein, übernommen wird jede gültige Zahl von 1 bis 99
    var percentText by rememberSaveable { mutableStateOf("30") }
    var n by rememberSaveable { mutableIntStateOf(100) }
    var bet by rememberSaveable { mutableFloatStateOf(0.5f) }
    var runNo by remember { mutableIntStateOf(0) }
    var shown by remember { mutableIntStateOf(0) }                                         // so viele Versuche sind schon zu sehen
    val chosen = ChanceLab.presets.firstOrNull { it.id == preset }
    val p = chosen?.p ?: (percent / 100.0)
    val event = chosen?.event ?: "ein Treffer"
    val run = remember(runNo, p, n) { if (runNo == 0) null else ChanceRun(p, n, seed + runNo) }
    val betCount = (bet * n).roundToInt()
    val done = run != null && shown >= n
    val sfx = LocalSfx.current; val speaker = LocalSpeaker.current
    LaunchedEffect(run) {
        if (run == null) return@LaunchedEffect
        shown = 0; var acc = 0f; var last = 0L
        while (shown < n) withFrameNanos { now -> val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f); last = now
            acc += dt * n / 3f; val step = acc.toInt(); acc -= step                        // jede Runde dauert etwa drei Sekunden
            if (step > 0) { shown = (shown + step).coerceAtMost(n); if (n <= 100) sfx?.play(Sound.TICK, 0.8f + run.freq(shown).toFloat()) } }
        sfx?.play(Sound.HIT); speaker?.say(run.verdict(betCount))
    }

    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding()) {
        Bar("Zufallsmaschine", onBack) { Text("p = ${Geo.num(p * 100)} %", Modifier.testTag("p"), color = Q.Pink, fontWeight = FontWeight.Bold) }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ChanceLab.presets.forEach { c -> Pick(c.title, preset == c.id, Q.Pink, "versuch-${c.id}") { preset = c.id; runNo = 0 } }
            Pick("✏️ Eigene Chance", chosen == null, Q.Yellow, "versuch-eigene") { preset = "eigene"; runNo = 0 }
        }
        if (chosen == null) Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Trefferchance", color = Q.Muted, fontSize = 13.sp)
            Pick("−5", false, Q.Violet, "p-minus") { percent = (percent - 5).coerceAtLeast(1); percentText = percent.toString(); runNo = 0 }
            Bound(percentText, "p-eingabe") { percentText = it.removePrefix("-").take(2); percentText.toIntOrNull()?.takeIf { v -> v in 1..99 }?.let { v -> percent = v; runNo = 0 } }
            Text("%", color = Q.Text, fontWeight = FontWeight.Bold)
            Pick("+5", false, Q.Violet, "p-plus") { percent = (percent + 5).coerceAtMost(99); percentText = percent.toString(); runNo = 0 }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Versuche", color = Q.Muted, fontSize = 13.sp)
            ChanceLab.counts.forEach { c -> Pick(Geo.num(c), n == c, Q.Cyan, "n-$c") { n = c; runNo = 0 } }
        }
        // Verlauf: relative Häufigkeit (blau) gegen die Wahrscheinlichkeit (pink gestrichelt)
        Box(Modifier.fillMaxWidth().weight(1f).padding(10.dp).background(Q.Deep, RoundedCornerShape(18.dp)).border(1.5.dp, Q.Cyan.copy(alpha = 0.6f), RoundedCornerShape(18.dp))) {
            Canvas(Modifier.fillMaxSize().padding(14.dp).testTag("verlauf")) {
                val w = size.width; val h = size.height
                fun y(f: Double) = (h * (1 - f)).toFloat()
                for (g in 0..4) drawLine(Q.Muted.copy(alpha = 0.25f), Offset(0f, h * g / 4), Offset(w, h * g / 4), 1f)
                drawLine(Q.Pink, Offset(0f, y(p)), Offset(w, y(p)), 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
                if (run != null && shown > 0) {
                    val path = Path(); val stepPx = maxOf(1, shown / 600)
                    for (i in 1..shown step stepPx) { val x = w * i / n; if (i == 1) path.moveTo(x, y(run.freq(i))) else path.lineTo(x, y(run.freq(i))) }
                    drawPath(path, Q.Cyan, style = Stroke(4f))
                    drawCircle(Q.Yellow, 8f, Offset(w * shown / n, y(run.freq(shown))))
                }
            }
            Text("100 %", Modifier.align(Alignment.TopStart).padding(6.dp), color = Q.Muted, fontSize = 10.sp); Text("0 %", Modifier.align(Alignment.BottomStart).padding(6.dp), color = Q.Muted, fontSize = 10.sp)
            if (run == null) Text("Erst wetten, dann werfen:\nDie blaue Linie zeigt, wie oft es bisher geklappt hat.", Modifier.align(Alignment.Center).padding(24.dp), color = Q.Muted, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Dial("Bisher", if (run == null) "–" else "${run.hits[shown]} von $shown", if (run == null || shown == 0) "" else "${Geo.num(run.freq(shown) * 100)} %", Q.Cyan, Modifier.weight(1f).testTag("bisher"))
            Dial("Erwartet n · p", Geo.num(n * p), "± ${Geo.num(kotlin.math.sqrt(n * p * (1 - p)))} (σ)", Q.Pink, Modifier.weight(1f).testTag("erwartet"))
        }
        Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 12.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Lux(if (done) LuxMood.WOW else LuxMood.IDLE, size = 46.dp)
            Text(if (done) run!!.verdict(betCount) else "Deine Wette: Bei $n Versuchen kommt $betCount-mal $event.", Modifier.padding(start = 8.dp).testTag("wette"), color = if (done) Q.Yellow else Q.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Slider(bet, { bet = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("wett-regler"), enabled = run == null || done,
            colors = SliderDefaults.colors(thumbColor = Q.Yellow, activeTrackColor = Q.Pink, inactiveTrackColor = Q.Panel))
        NeonButton({ runNo++ }, Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp).testTag("werfen"), color = Q.Green, enabled = run == null || done) {
            Text(if (run != null && !done) "läuft …" else if (done) "↻ Noch einmal" else "▶ $n-mal ausprobieren", fontWeight = FontWeight.Black) }
    }
}
