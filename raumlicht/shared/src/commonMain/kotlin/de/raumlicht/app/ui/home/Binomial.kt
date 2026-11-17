package de.raumlicht.app.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.raumlicht.app.ui.fx.*
import de.raumlicht.app.ui.level.Stepper
import de.raumlicht.app.ui.lux.Lux
import de.raumlicht.app.ui.lux.LuxMood
import de.raumlicht.app.ui.theme.Q
import de.raumlicht.core.model.Binomial
import de.raumlicht.core.model.BinomialMode
import de.raumlicht.core.model.Geo

/**
 * Binomial-Labor: n, p und k frei einstellen und sofort sehen, wie die Verteilung aussieht.
 * Drei Fragen (genau, höchstens, mindestens k Treffer); die Säulen zeigen wahlweise die einzelnen Wahrscheinlichkeiten P(X = k)
 * oder die aufsummierten P(X ≤ k). Säulen antippen oder mit dem Finger darüberfahren wählt k.
 */
@Composable
fun BinomialScreen(onBack: () -> Unit) {
    var n by rememberSaveable { mutableIntStateOf(10) }
    var percent by rememberSaveable { mutableIntStateOf(50) }
    var k by rememberSaveable { mutableIntStateOf(3) }
    var mode by rememberSaveable { mutableStateOf(BinomialMode.AT_MOST) }
    var summed by rememberSaveable { mutableStateOf(false) }                 // Säulen aufsummiert zeichnen (kumulierte Verteilung)
    val dist = remember(n, percent) { Binomial(n, percent / 100.0) }
    val kk = k.coerceIn(0, n)
    val sfx = LocalSfx.current; val speaker = LocalSpeaker.current
    fun pick(v: Int) { val c = v.coerceIn(0, n); if (c != k) { k = c; sfx?.play(Sound.TICK, 0.8f + c.toFloat() / n) } }
    // Säulen wachsen bei jeder Änderung von n, p oder der Darstellung neu heraus
    val grow = remember { Animatable(0f) }
    LaunchedEffect(n, percent, summed) { grow.snapTo(0.35f); grow.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 220f)) }
    val measurer = rememberTextMeasurer()
    val result = dist.ask(mode, kk)

    Column(Modifier.fillMaxSize().background(Q.Space).systemBarsPadding()) {
        Bar("Binomialverteilung", onBack) { Text("B($n; ${Geo.num(percent / 100.0)})", Modifier.testTag("verteilung"), color = Q.Pink, fontWeight = FontWeight.Bold) }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BinomialMode.entries.forEach { m -> Pick(m.title, mode == m, Q.Yellow, "frage-${m.name.lowercase()}") { mode = m; sfx?.play(Sound.TAP) } }
            Pick(if (summed) "Säulen: aufsummiert" else "Säulen: einzeln", summed, Q.Cyan, "saeulen") { summed = !summed; sfx?.play(Sound.TAP) }
        }
        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 10.dp, vertical = 4.dp).background(Q.Deep, RoundedCornerShape(18.dp)).border(1.5.dp, Q.Pink.copy(alpha = 0.6f), RoundedCornerShape(18.dp))) {
            Canvas(Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, top = 26.dp, bottom = 8.dp).testTag("saeulenbild")
                .pointerInput(n) { detectTapGestures { o -> pick((o.x / size.width * (n + 1)).toInt()) } }
                .pointerInput(n) { detectDragGestures { change, _ -> pick((change.position.x / size.width * (n + 1)).toInt()) } }) {
                val labelH = 18.dp.toPx(); val w = size.width; val h = size.height - labelH; val slot = w / (n + 1); val gap = if (n > 40) 0.5f else slot * 0.14f
                val values = if (summed) dist.upTo else dist.single; val top = if (summed) 1.0 else values.max()
                for (g in 1..3) drawLine(Q.Muted.copy(alpha = 0.18f), Offset(0f, h * g / 4), Offset(w, h * g / 4), 1f)
                // Streubereich μ ± σ und der Erwartungswert
                val mx = ((dist.mean + 0.5) * slot).toFloat(); val sx = (dist.sigma * slot).toFloat()
                drawRect(Q.Pink.copy(alpha = 0.10f), Offset((mx - sx).coerceAtLeast(0f), 0f), Size(((mx + sx).coerceAtMost(w) - (mx - sx).coerceAtLeast(0f)), h))
                drawLine(Q.Pink, Offset(mx, 0f), Offset(mx, h), 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                values.forEachIndexed { x, v ->
                    val bh = (h * v / top * grow.value).toFloat().coerceAtMost(h); val on = dist.counts(mode, kk, x)
                    drawRoundRect(if (x == kk) Q.Yellow else if (on) Q.Yellow.copy(alpha = 0.6f) else Q.Cyan.copy(alpha = 0.35f), Offset(x * slot + gap, h - bh), Size((slot - 2 * gap).coerceAtLeast(1f), bh), CornerRadius(3f))
                }
                drawLine(Q.Muted.copy(alpha = 0.7f), Offset(0f, h), Offset(w, h), 2f)
                // Beschriftung: 0, k und n (bei wenigen Säulen jede)
                val style = TextStyle(color = Q.Muted, fontSize = 10.sp); val bold = TextStyle(color = Q.Yellow, fontSize = 11.sp, fontWeight = FontWeight.Black)
                val every = when { n <= 12 -> 1; n <= 30 -> 5; n <= 60 -> 10; else -> 20 }
                for (x in 0..n) if (x == kk || (x % every == 0 && kotlin.math.abs(x - kk) * slot > 14.dp.toPx())) {
                    val t = measurer.measure(x.toString(), if (x == kk) bold else style)
                    drawText(t, topLeft = Offset(((x + 0.5f) * slot - t.size.width / 2).coerceIn(0f, w - t.size.width), h + 3f))
                }
                val mu = measurer.measure("μ = ${Geo.num(dist.mean)}", TextStyle(color = Q.Pink, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                drawText(mu, topLeft = Offset((mx + 6f).coerceAtMost(w - mu.size.width), -20.dp.toPx()))
            }
            Text(if (summed) "P(X ≤ k) aufsummiert" else "P(X = k)", Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 6.dp), color = Q.Muted, fontSize = 10.sp)
        }
        // Ergebnis mit Ansatz und Lux' Übersetzung in Alltagssprache
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp).background(Q.Deep, RoundedCornerShape(14.dp)).border(1.dp, Q.Yellow.copy(alpha = 0.6f), RoundedCornerShape(14.dp)).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Lux(if (result > 0.5) LuxMood.HAPPY else LuxMood.THINK, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Text("P(X ${mode.sign} $kk) = ${Binomial.percent(result)}", Modifier.testTag("ergebnis"), color = Q.Yellow, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(dist.approach(mode, kk), Modifier.testTag("ansatz"), color = Q.Text, fontSize = 12.sp)
                Text(dist.story(mode, kk), Modifier.testTag("satz"), color = Q.Muted, fontSize = 12.sp)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Dial("Erwartungswert μ = n · p", Geo.num(dist.mean), "so viele Treffer im Schnitt", Q.Pink, Modifier.weight(1f).testTag("mu"))
            Dial("Streuung σ", Geo.num(dist.sigma), dist.spreadText(), Q.Violet, Modifier.weight(1f).testTag("sigma"))
        }
        // Auf schmalen Handys (unter 360 dp) bekommen die Schnellwahl-Knöpfe eine eigene Zeile – sonst bleibt für den Regler kein Platz
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp)) {
            val narrow = maxWidth < 340.dp
            @Composable fun picks() = listOf(10, 20, 50, 100).forEach { c -> Pick("$c", n == c, Q.Cyan, "n-$c") { n = c; k = k.coerceAtMost(n) } }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Stepper("Versuche n", n, n.toString(), Q.Cyan, "n", Modifier.weight(1f), n > 1, n < Binomial.MAX_N) { d -> n = (n + d).coerceIn(1, Binomial.MAX_N); k = k.coerceAtMost(n) }
                    if (!narrow) picks()
                }
                if (narrow) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { picks() }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Pick("−10", false, Q.Pink, "p-minus10") { percent = (percent - 10).coerceAtLeast(1) }
            Stepper("Trefferchance p", percent, "$percent %", Q.Pink, "p", Modifier.weight(1f), percent > 1, percent < 99) { d -> percent = (percent + d).coerceIn(1, 99) }
            Pick("+10", false, Q.Pink, "p-plus10") { percent = (percent + 10).coerceAtMost(99) }
        }
        Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 2.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Stepper("Treffer k (oder Säule antippen)", kk, kk.toString(), Q.Yellow, "k", Modifier.weight(1f), kk > 0, kk < n) { d -> pick(kk + d) }
            NeonButton({ speaker?.say(dist.story(mode, kk)) }, Modifier.testTag("vorlesen"), color = Q.Violet) { Text("🔊") }
        }
    }
}
