package de.experimentopia.app.ui.lab

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private fun Rgb.c() = Color(r.toFloat(), g.toFloat(), b.toFloat())

@Composable
fun LightLab(s: LightSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val tm = rememberTextMeasurer()
    var mode by remember { mutableStateOf(s.mode) }
    // ADD: Helligkeit der Lampen R, G, B · SUB: Stärke der Filter C, M, Y
    val v = remember { mutableStateListOf(0f, 0f, 0f) }
    LaunchedEffect(mode) { v[0] = 0f; v[1] = 0f; v[2] = 0f }
    val result = if (mode == LightMode.ADD) Rgb(v[0].toDouble(), v[1].toDouble(), v[2].toDouble()) else Colors.filter(v[0].toDouble(), v[1].toDouble(), v[2].toDouble())
    val target = s.target
    val hit = target != null && result.close(target)
    LaunchedEffect(hit) { if (hit) sfx?.play(Sound.WIN) }
    GoalWhen(!free && hit, onGoal)
    val names = if (mode == LightMode.ADD) listOf("Rot", "Grün", "Blau") else listOf("Türkis (Cyan)", "Pink (Magenta)", "Gelb")
    val cols = if (mode == LightMode.ADD) listOf(Rgb.ROT, Rgb.GRUEN, Rgb.BLAU) else listOf(Rgb.CYAN, Rgb.MAGENTA, Rgb.GELB)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (free) ChipRow(LightMode.entries, mode, { if (it == LightMode.ADD) "🔦 Licht mischen" else "🎞️ Farbfilter" }, { mode = it })
        Canvas(Modifier.fillMaxWidth().height(250.dp).graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.testTag("lichtbuehne")) {
            val w = size.width; val h = size.height
            drawRoundRect(Color(0xFF06040F), Offset.Zero, size, CornerRadius(22f))
            if (mode == LightMode.ADD) {
                // drei Lichtkegel, die sich auf der Wand überlagern (additiv)
                val c = Offset(w / 2, h * 0.58f); val rr = h * 0.27f
                listOf(-90.0, 30.0, 150.0).forEachIndexed { i, a ->
                    val p = c + Offset(cos(toRad(a)).toFloat(), sin(toRad(a)).toFloat()) * (rr * 0.55f)
                    val col = Color(cols[i].r.toFloat() * v[i], cols[i].g.toFloat() * v[i], cols[i].b.toFloat() * v[i])
                    drawCircle(col, rr, p, blendMode = BlendMode.Plus)
                    // Lampe
                    val lamp = Offset(w * (0.2f + 0.3f * i), h * 0.07f)
                    drawCircle(cols[i].c().copy(alpha = 0.3f + 0.7f * v[i]), 14f, lamp)
                    val cone = Path().apply { moveTo(lamp.x, lamp.y); lineTo(p.x - rr * 0.6f, p.y); lineTo(p.x + rr * 0.6f, p.y); close() }
                    drawPath(cone, col.copy(alpha = 0.10f), blendMode = BlendMode.Plus)
                }
            } else {
                // weiße Lampe, Folien, Lichtfleck
                val y = h * 0.5f
                drawCircle(Color.White, 22f, Offset(w * 0.08f, y))
                var beam = Rgb.WEISS
                drawRect(Color.White.copy(alpha = 0.8f), Offset(w * 0.08f, y - 10f), Size(w * 0.17f, 20f))
                for (i in 0..2) {
                    val x = w * (0.3f + 0.15f * i)
                    drawRoundRect(cols[i].c().copy(alpha = 0.15f + 0.6f * v[i]), Offset(x - 10f, y - 60f), Size(20f, 120f), CornerRadius(6f))
                    beam = Rgb(beam.r * (1 - (if (i == 0) v[i].toDouble() else 0.0)), beam.g * (1 - (if (i == 1) v[i].toDouble() else 0.0)), beam.b * (1 - (if (i == 2) v[i].toDouble() else 0.0)))
                    drawRect(beam.c(), Offset(x + 10f, y - 10f), Size(w * 0.15f - 20f, 20f))
                }
                drawCircle(Brush.radialGradient(listOf(result.c(), result.c().copy(alpha = 0f)), Offset(w * 0.86f, y), h * 0.3f), h * 0.3f, Offset(w * 0.86f, y))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(40.dp).background(result.c(), CircleShape).border(2.dp, Color.White, CircleShape))
            Text("Ergebnis: ${Colors.name(result)}", color = E.Text, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).testTag("farbname"))
            s.target?.let { t -> Text("Ziel:", color = E.Muted); Box(Modifier.size(30.dp).background(t.c(), CircleShape).border(2.dp, if (hit) E.Green else E.Line, CircleShape)) }
        }
        names.forEachIndexed { i, n ->
            ValueSlider(if (mode == LightMode.ADD) "💡 Lampe $n" else "🎞️ Folie $n", v[i], 0f..1f, "${(v[i] * 100).toInt()} %",
                { x -> val q = roundI(x * 4) / 4f; if (q != v[i]) { v[i] = q; sfx?.play(Sound.TAP, pitch = 0.8f + q * 0.5f, volume = 0.4f) } }, steps = 3, color = cols[i].c().let { if (it == Color(0f, 0f, 1f)) E.Sky else it })
        }
        if (free) Panel(border = E.Sun) {
            Text("Eigene Werte (0 bis 255, wie im Computer)", fontWeight = FontWeight.Bold, color = E.Sun)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 0..2) NumberField(names[i].take(4), "", 0.0, 255.0, (v[i] * 255.0), { x -> v[i] = (x / 255.0).toFloat() }, Modifier.weight(1f))
            }
        }
    }
}

/** Wellenlänge (nm) → Bildschirmfarbe (Näherung) */
private fun spectral(nm: Double): Color {
    val (r, g, b) = when {
        nm < 440 -> Triple(-(nm - 440) / 60, 0.0, 1.0)
        nm < 490 -> Triple(0.0, (nm - 440) / 50, 1.0)
        nm < 510 -> Triple(0.0, 1.0, -(nm - 510) / 20)
        nm < 580 -> Triple((nm - 510) / 70, 1.0, 0.0)
        nm < 645 -> Triple(1.0, -(nm - 645) / 65, 0.0)
        else -> Triple(1.0, 0.0, 0.0)
    }
    return Color(r.toFloat().coerceIn(0f, 1f), g.toFloat().coerceIn(0f, 1f), b.toFloat().coerceIn(0f, 1f))
}

private data class V(val x: Double, val y: Double) {
    operator fun plus(o: V) = V(x + o.x, y + o.y); operator fun minus(o: V) = V(x - o.x, y - o.y); operator fun times(k: Double) = V(x * k, y * k)
    fun dot(o: V) = x * o.x + y * o.y; fun o() = Offset(x.toFloat(), y.toFloat())
}

/** Brechung nach Snellius (Vektorform). n zeigt gegen den Strahl. null = Totalreflexion */
private fun refract(d: V, n: V, eta: Double): V? {
    val cosi = -n.dot(d); val k = 1 - eta * eta * (1 - cosi * cosi)
    if (k < 0) return null
    return d * eta + n * (eta * cosi - sqrt(k))
}

/** Schnitt Strahl p + t·d mit Strecke a–b: t oder null */
private fun hit(p: V, d: V, a: V, b: V): Double? {
    val e = b - a; val den = d.x * e.y - d.y * e.x
    if (kotlin.math.abs(den) < 1e-12) return null
    val t = ((a.x - p.x) * e.y - (a.y - p.y) * e.x) / den
    val u = ((a.x - p.x) * d.y - (a.y - p.y) * d.x) / den
    return if (t > 1e-6 && u in 0.0..1.0) t else null
}

@Composable
fun PrismLab(free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val tm = rememberTextMeasurer()
    var rot by remember { mutableFloatStateOf(0f) }
    var touched by remember { mutableStateOf(false) }
    var spread by remember { mutableDoubleStateOf(0.0) }
    GoalWhen(!free && touched && spread > 1.5, onGoal)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(260.dp).testTag("prisma")) {
            val w = size.width.toDouble(); val h = size.height.toDouble()
            drawRoundRect(Color(0xFF06040F), Offset.Zero, size, CornerRadius(22f))
            val c = V(w * 0.45, h * 0.5); val rad = h * 0.3
            val tri = (0..2).map { k -> val a = toRad(-90.0 + 120.0 * k + rot); V(c.x + rad * cos(a), c.y + rad * sin(a)) }
            val edges = listOf(tri[0] to tri[1], tri[1] to tri[2], tri[2] to tri[0])
            val start = V(0.0, c.y + rad * 0.1); val dir = V(1.0, 0.0)
            var minDev = 1e9; var maxDev = -1e9
            var drewIn = false
            for (nm in 400..700 step 10) {
                val n = Prism.n(nm.toDouble())
                // Eintritt
                val first = edges.mapNotNull { (a, b) -> hit(start, dir, a, b)?.let { it to (a to b) } }.minByOrNull { it.first } ?: continue
                val p1 = start + dir * first.first
                if (!drewIn) { drawLine(Color.White, start.o(), p1.o(), 5f); drewIn = true }
                fun outward(a: V, b: V): V { val e = b - a; var nn = V(e.y, -e.x); val l = sqrt(nn.dot(nn)); nn = nn * (1 / l); val mid = (a + b) * 0.5; return if ((mid - c).dot(nn) < 0) nn * -1.0 else nn }
                val n1 = outward(first.second.first, first.second.second)
                val d2 = refract(dir, n1, 1 / n) ?: continue
                val second = edges.filter { it != first.second }.mapNotNull { (a, b) -> hit(p1, d2, a, b)?.let { it to (a to b) } }.minByOrNull { it.first } ?: continue
                val p2 = p1 + d2 * second.first
                drawLine(spectral(nm.toDouble()).copy(alpha = 0.35f), p1.o(), p2.o(), 2f)
                val n2 = outward(second.second.first, second.second.second) * -1.0
                val d3 = refract(d2, n2, n) ?: continue
                val far = p2 + d3 * (w * 1.5)
                drawLine(spectral(nm.toDouble()), p2.o(), far.o(), 3f, blendMode = BlendMode.Plus)
                val dev = toDeg(kotlin.math.atan2(d3.y, d3.x))
                minDev = minOf(minDev, dev); maxDev = maxOf(maxDev, dev)
            }
            spread = if (maxDev > minDev) maxDev - minDev else 0.0
            val path = Path().apply { moveTo(tri[0].o().x, tri[0].o().y); lineTo(tri[1].o().x, tri[1].o().y); lineTo(tri[2].o().x, tri[2].o().y); close() }
            drawPath(path, Color(0x55A8D8FF)); drawPath(path, Color.White.copy(alpha = 0.8f), style = Stroke(3f))
            label(tm, "weißes Licht", Offset(60f, (c.y + rad * 0.1 - 18).toFloat()), 11.sp, E.Muted)
        }
        ValueSlider("Prisma drehen", rot, -60f..60f, "${rot.toInt()}°", { rot = it; if (!touched) { touched = true }; if ((it.toInt() % 10) == 0) sfx?.play(Sound.TAP, volume = 0.3f) }, color = E.Violet)
        Text(if (spread > 0.1) "Die Farben fächern sich um ${Fmt.num(spread, 1)}° auf. Violett wird am stärksten abgelenkt, Rot am wenigsten." else "Dreh das Prisma – manchmal wird das Licht innen komplett gespiegelt (Totalreflexion).", color = E.Text, fontSize = 14.sp)
        Text("Brechzahl von Glas: Rot ${Fmt.num(Prism.n(700.0), 4)}, Violett ${Fmt.num(Prism.n(400.0), 4)}", color = E.Muted, fontSize = 12.sp)
    }
}
