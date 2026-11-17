package de.experimentopia.app.ui.lab

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.experimentopia.app.ui.common.*
import de.experimentopia.app.ui.fx.*
import de.experimentopia.app.ui.theme.E
import de.experimentopia.app.ui.theme.color
import de.experimentopia.core.model.*
import kotlin.math.cbrt
import kotlin.math.sin

private val NEUTRAL = Indicator.color(7.0).color()

/** Ein Glas mit Rotkohlsaft, das nach dem Eingießen die Farbe wechselt */
@Composable
private fun JuiceGlass(title: String, emoji: String, pH: Double?, onPour: () -> Unit, modifier: Modifier = Modifier) {
    val col by animateColorAsState(pH?.let { Indicator.color(it).color() } ?: NEUTRAL, tween(1400), label = "c")
    val now by rememberSeconds()
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(E.Deep).clickable { onPour() }.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 24.sp)
        Canvas(Modifier.size(60.dp, 70.dp)) {
            val w = size.width; val h = size.height
            val glass = Path().apply { moveTo(w * 0.15f, h * 0.05f); lineTo(w * 0.22f, h * 0.95f); lineTo(w * 0.78f, h * 0.95f); lineTo(w * 0.85f, h * 0.05f); close() }
            clipPath(glass) {
                val lvl = h * 0.35f + sin(now * 3f) * 1.5f
                drawRect(Brush.verticalGradient(listOf(col.copy(alpha = 0.85f), col), lvl, h), Offset(0f, lvl), Size(w, h - lvl))
                if (pH != null) repeat(4) { k -> val p = (now * 0.7f + k * 0.25f) % 1f; drawCircle(Color.White.copy(alpha = 0.5f * (1 - p)), 2.5f, Offset(w * (0.35f + 0.1f * k), h - p * (h - lvl))) }
            }
            drawPath(glass, Color.White.copy(alpha = 0.8f), style = Stroke(2.5f))
        }
        Text(title, color = E.Text, fontSize = 11.sp, maxLines = 1)
        Text(pH?.let { "pH ${Fmt.num(it, 1)}" } ?: "antippen", color = if (pH != null) E.Sun else E.Muted, fontSize = 11.sp)
    }
}

/** pH-Skala mit Rotkohlfarben und den getesteten Stoffen darauf */
@Composable
private fun PhScale(marks: List<Pair<String, Double>>) {
    val tm = rememberTextMeasurer()
    Canvas(Modifier.fillMaxWidth().height(64.dp)) {
        val w = size.width
        for (i in 0 until 140) { val p = 1 + i * 13.0 / 140; drawRect(Indicator.color(p).color(), Offset(w * i / 140f, (26f * density)), Size(w / 140f + 1, (18f * density))) }
        listOf(1, 4, 7, 10, 14).forEach { p -> label(tm, "$p", Offset(w * (p - 1) / 13f, (54f * density)), 10.sp, E.Muted) }
        label(tm, "sauer", Offset(w * 0.1f, (12f * density)), 11.sp, E.Pink); label(tm, "neutral", Offset(w * 0.46f, (12f * density)), 11.sp, E.Violet); label(tm, "basisch", Offset(w * 0.85f, (12f * density)), 11.sp, E.Mint)
        marks.forEach { (e, p) -> label(tm, e, Offset(w * ((p - 1) / 13).toFloat(), (35f * density)), 16.sp) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhLab(s: PhSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val poured = remember { mutableStateListOf<Household>() }
    var last by remember { mutableStateOf<Household?>(null) }
    var ownPh by remember { mutableDoubleStateOf(5.0) }
    var ownShown by remember { mutableStateOf(false) }
    GoalWhen(!free && s.substances.all { it in poured }, onGoal)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Jedes Glas enthält violetten Rotkohlsaft. Tipp ein Glas an, um den Stoff hineinzugießen.", color = E.Muted, fontSize = 13.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.testTag("glaeser")) {
            s.substances.forEach { h ->
                JuiceGlass(h.title, h.emoji, if (h in poured) h.pH else null, {
                    if (h !in poured) { poured += h; last = h; sfx?.play(Sound.GLASS); sfx?.play(Sound.BUBBLE, volume = 0.5f) }
                }, Modifier.width(92.dp))
            }
            if (free) JuiceGlass("Mein Stoff", "🧪", if (ownShown) ownPh else null, { ownShown = true; sfx?.play(Sound.BUBBLE) }, Modifier.width(92.dp))
        }
        PhScale(poured.map { it.emoji to it.pH } + if (free && ownShown) listOf("🧪" to ownPh) else emptyList())
        last?.let { h -> Panel(border = Indicator.color(h.pH).color()) {
            Text("${h.emoji} ${h.title}: pH ${Fmt.num(h.pH, 1)} – ${Indicator.kind(h.pH)}, der Saft wird ${Indicator.name(h.pH)}", fontWeight = FontWeight.Bold, color = E.Text)
            Text(h.note, color = E.Muted, fontSize = 13.sp)
        } }
        if (free) Panel(border = E.Sun) {
            Text("Eigener pH-Wert: Welche Farbe zeigt der Saft?", fontWeight = FontWeight.Bold, color = E.Sun)
            NumberField("pH-Wert", "", 0.0, 14.0, ownPh, { ownPh = it; ownShown = true })
        }
        GlowButton({ poured.clear(); last = null; ownShown = false; sfx?.play(Sound.WHOOSH) }, color = E.Sky) { Text("🧽 Gläser spülen") }
    }
}

@Composable
fun TitrationLab(s: TitrationSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val tm = rememberTextMeasurer()
    val now by rememberSeconds()
    var acidC by remember { mutableDoubleStateOf(s.acidC) }
    var acidMl by remember { mutableDoubleStateOf(s.acidMl) }
    var baseC by remember { mutableDoubleStateOf(s.baseC ?: 0.1) }
    var useBase by remember { mutableStateOf(s.baseC != null) }
    var step by remember { mutableDoubleStateOf(s.stepMl) }
    var beaker by remember { mutableStateOf(Beaker().addAcid(acidC, acidMl)) }
    val curve = remember { mutableStateListOf(0.0 to beaker.pH) }
    var added by remember { mutableDoubleStateOf(0.0) }
    var dropAt by remember { mutableFloatStateOf(-10f) }
    fun reset() { beaker = Beaker().addAcid(acidC, acidMl); added = 0.0; curve.clear(); curve += 0.0 to beaker.pH }
    LaunchedEffect(acidC, acidMl, baseC, useBase) { reset() }
    val col by animateColorAsState(Indicator.color(beaker.pH).color(), tween(500), label = "c")
    GoalWhen(!free && kotlin.math.abs(beaker.pH - s.targetPh) <= s.tolPh, onGoal)
    val maxMl = 1000.0

    fun add() {
        if (beaker.volumeMl + step > maxMl) { sfx?.play(Sound.SOFT); return }
        val before = beaker.pH
        beaker = if (useBase) beaker.addBase(baseC, step) else beaker.addWater(step)
        added += step; curve += added to beaker.pH; dropAt = now
        sfx?.play(if (kotlin.math.abs(beaker.pH - before) > 3) Sound.AHA else Sound.BUBBLE, volume = 0.6f)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().height(250.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Canvas(Modifier.weight(1f).fillMaxHeight().testTag("becherglas")) {
                val w = size.width; val h = size.height
                // Bürette
                val bx = w * 0.5f
                drawRect(Color.White.copy(alpha = 0.15f), Offset(bx - (8f * density), 0f), Size((16f * density), h * 0.32f))
                drawRect(if (useBase) E.Sky.copy(alpha = 0.6f) else Color(0xFF9FD8FF).copy(alpha = 0.5f), Offset(bx - (6f * density), h * 0.05f), Size((12f * density), h * 0.25f))
                val dt = now - dropAt
                if (dt in 0f..0.5f) drawCircle(E.Sky, (6f * density), Offset(bx, h * 0.34f + dt * h * 0.6f))
                // Becherglas
                val left = w * 0.15f; val right = w * 0.85f; val top = h * 0.42f; val bottom = h * 0.97f
                val fill = (beaker.volumeMl / (if (useBase) 100.0 else maxMl)).coerceIn(0.06, 1.0).toFloat()
                val lvl = bottom - (bottom - top) * fill
                drawRect(Brush.verticalGradient(listOf(col.copy(alpha = 0.8f), col), lvl, bottom), Offset(left, lvl + sin(now * 3f) * (2f * density)), Size(right - left, bottom - lvl))
                drawRoundRect(Color.White.copy(alpha = 0.8f), Offset(left, top), Size(right - left, bottom - top), CornerRadius((10f * density)), style = Stroke((3f * density)))
                label(tm, "${Fmt.num(beaker.volumeMl, 0)} mL", Offset((left + right) / 2, top - (14f * density)), 12.sp, E.Text)
            }
            // pH-Kurve
            Canvas(Modifier.weight(1.2f).fillMaxHeight().background(Color(0xFF0B0820), RoundedCornerShape(14.dp)).testTag("phkurve")) {
                val w = size.width; val h = size.height; val pad = (26f * density)
                val xMax = (curve.maxOf { it.first }.coerceAtLeast(if (useBase) 20.0 else 100.0))
                fun px(x: Double) = pad + ((x / xMax) * (w - pad - (8f * density))).toFloat()
                fun py(p: Double) = (h - pad) - ((p / 14.0) * (h - pad - (8f * density))).toFloat()
                drawLine(E.Muted, Offset(pad, py(7.0)), Offset(w - (8f * density), py(7.0)), 1f)
                label(tm, "7", Offset((12f * density), py(7.0)), 10.sp, E.Muted); label(tm, "14", Offset((12f * density), py(14.0) + (6f * density)), 10.sp, E.Muted); label(tm, "0", Offset((12f * density), py(0.0)), 10.sp, E.Muted)
                label(tm, if (useBase) "mL Lauge" else "mL Wasser", Offset(w / 2, h - (10f * density)), 10.sp, E.Muted)
                for (i in 1 until curve.size) drawLine(E.Mint, Offset(px(curve[i - 1].first), py(curve[i - 1].second)), Offset(px(curve[i].first), py(curve[i].second)), (3f * density))
                curve.forEach { (x, p) -> drawCircle(Indicator.color(p).color(), 4.5f, Offset(px(x), py(p))) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Readout("pH-Wert", Fmt.num(beaker.pH, 2), Indicator.color(beaker.pH).color(), Modifier.weight(1f).testTag("phwert"))
            Readout(if (useBase) "Lauge zugetropft" else "Wasser zugegeben", "${Fmt.num(added, 1)} mL", E.Sky, Modifier.weight(1f))
            Readout("Lösung ist", Indicator.kind(beaker.pH), E.Sun, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton({ add() }, Modifier.weight(1f), color = E.Sky, sound = Sound.POP) { Text(if (useBase) "💧 +${Fmt.num(step)} mL Lauge" else "🚰 +${Fmt.num(step)} mL Wasser", fontWeight = FontWeight.Bold) }
            GlowButton({ reset(); sfx?.play(Sound.WHOOSH) }, color = E.Muted) { Text("↺ Neu") }
        }
        Text("Im Glas: ${Fmt.num(acidMl)} mL Salzsäure (${Fmt.num(acidC, 3)} mol/L)" + if (useBase) " · Natronlauge ${Fmt.num(baseC, 3)} mol/L" else "", color = E.Muted, fontSize = 12.sp)
        if (free) Panel(border = E.Sun) {
            Text("Eigene Werte", fontWeight = FontWeight.Bold, color = E.Sun)
            ChipRow(listOf(true, false), useBase, { if (it) "Lauge zutropfen" else "Mit Wasser verdünnen" }, { useBase = it })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Säure", "mol/L", 0.0001, 2.0, acidC, { acidC = it }, Modifier.weight(1f))
                NumberField("Säure", "mL", 1.0, 100.0, acidMl, { acidMl = it }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (useBase) NumberField("Lauge", "mol/L", 0.0001, 2.0, baseC, { baseC = it }, Modifier.weight(1f))
                NumberField("Schritt", "mL", 0.1, 100.0, step, { step = it }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun FizzLab(s: FizzSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val tm = rememberTextMeasurer()
    val now by rememberSeconds()
    var vinegar by remember { mutableDoubleStateOf(s.vinegarMl) }
    var spoons by remember { mutableIntStateOf(0) }
    var fizzAt by remember { mutableFloatStateOf(-10f) }
    val gas = Fizz.gasL(spoons.toDouble(), vinegar)
    val r by animateFloatAsState(cbrt(gas).toFloat(), spring(dampingRatio = 0.5f, stiffness = 30f), label = "ballon")
    GoalWhen(!free && gas >= s.targetL, onGoal)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(280.dp).testTag("ballon")) {
            val w = size.width; val h = size.height; val cx = w / 2
            val bottom = h * 0.97f; val neckTop = h * 0.5f
            // Flasche
            val bottle = Path().apply {
                moveTo(cx - (18f * density), neckTop); lineTo(cx - (18f * density), h * 0.62f); lineTo(cx - (70f * density), h * 0.72f); lineTo(cx - (70f * density), bottom)
                lineTo(cx + (70f * density), bottom); lineTo(cx + (70f * density), h * 0.72f); lineTo(cx + (18f * density), h * 0.62f); lineTo(cx + (18f * density), neckTop); close()
            }
            clipPath(bottle) {
                val lvl = bottom - (bottom - h * 0.72f) * (vinegar / 250.0).toFloat().coerceIn(0.1f, 0.95f)
                drawRect(Color(0xFFFFE9A8).copy(alpha = 0.55f), Offset(0f, lvl), Size(w, bottom - lvl))
                val f = now - fizzAt
                if (f in 0f..3f) repeat(40) { k ->
                    val p = ((f * 0.8f + k * 0.037f) % 1f)
                    drawCircle(Color.White.copy(alpha = 0.7f * (1 - f / 3f)), (3f * density) + (k % 3), Offset(cx - (60f * density) + (k * 29 % 120), bottom - p * (bottom - neckTop)))
                }
                if (spoons > 0) drawRect(Color.White.copy(alpha = 0.8f), Offset(cx - (60f * density), bottom - (10f * density)), Size((120f * density), (10f * density)))
            }
            drawPath(bottle, Color.White.copy(alpha = 0.8f), style = Stroke((3f * density)))
            // Ballon
            val br = (20f * density) + r * (60f * density)
            val bc = Offset(cx, neckTop - br * 0.9f)
            drawLine(E.Pink, Offset(cx - (16f * density), neckTop), Offset(cx - (8f * density), neckTop - (12f * density)), (6f * density)); drawLine(E.Pink, Offset(cx + (16f * density), neckTop), Offset(cx + (8f * density), neckTop - (12f * density)), (6f * density))
            drawOval(Brush.radialGradient(listOf(Color(0xFFFF8CC0), E.Pink), bc - Offset(br * 0.3f, br * 0.3f), br * 1.4f), Offset(bc.x - br, bc.y - br * 1.1f), Size(br * 2, br * 2.1f))
            drawCircle(Color.White.copy(alpha = 0.5f), br * 0.18f, bc - Offset(br * 0.4f, br * 0.45f))
            label(tm, "${Fmt.num(gas, 2)} L", bc, 14.sp, Color.White, true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Readout("Natron (je 5 g)", "$spoons TL", E.Text, Modifier.weight(1f))
            Readout("Essig", "${Fmt.num(vinegar, 0)} mL", E.Sun, Modifier.weight(1f))
            Readout("Gas (CO₂)", "${Fmt.num(gas, 2)} L", E.Pink, Modifier.weight(1f))
        }
        if (spoons > 0) Text(if (Fizz.limiting(spoons.toDouble(), vinegar) == "Essig") "🛑 Der Essig ist aufgebraucht – mehr Natron bringt kein Gas mehr." else "Es ist noch Essigsäure übrig – mehr Natron gibt mehr Gas.", color = E.Text, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton({ if (spoons < (if (free) 20 else s.maxSpoons)) { spoons++; fizzAt = now; sfx?.play(Sound.FIZZ) } }, Modifier.weight(1f), color = E.Pink, sound = Sound.POP) { Text("🥄 +1 Löffel Natron", fontWeight = FontWeight.Bold) }
            GlowButton({ spoons = 0; sfx?.play(Sound.WHOOSH) }, color = E.Muted) { Text("↺ Neu") }
        }
        Text("Rechnung: 1 Teelöffel Natron (5 g) ≈ 0,06 mol, 100 mL Essig (5 %) ≈ 0,08 mol Essigsäure. 1 mol Gas ≈ 24 L. Echte Ballons werden etwas kleiner, weil sich CO₂ im Wasser löst.", color = E.Muted, fontSize = 11.sp)
        if (free) NumberField("Essig", "mL", 10.0, 500.0, vinegar, { vinegar = it })
    }
}
