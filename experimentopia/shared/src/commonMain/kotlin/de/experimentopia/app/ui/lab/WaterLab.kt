package de.experimentopia.app.ui.lab

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
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
import de.experimentopia.core.model.FloatState as Swim
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/** Ein Gegenstand fürs Becken – aus der Liste oder mit eigenen Werten */
private data class Floaty(val key: String, val title: String, val emoji: String, val rho: Double, val note: String)
private fun Thing.f() = Floaty(name, title, emoji, rho, note)
private data class Dropped(val f: Floaty, val liquid: Liquid, val at: Float, val slot: Int)

@Composable
fun FloatLab(s: FloatSetup, free: Boolean, onGoal: () -> Unit) {
    val now by rememberSeconds()
    val sfx = LocalSfx.current
    val tm = rememberTextMeasurer()
    var liquid by remember { mutableStateOf(s.liquids.first()) }
    val dropped = remember { mutableStateListOf<Dropped>() }
    var last by remember { mutableStateOf<Dropped?>(null) }
    val bursts = rememberBursts()
    var custom by remember { mutableStateOf(Floaty("EIGEN", "Mein Ding", "🎁", 0.8, "Deine eigenen Werte")) }
    var mass by remember { mutableDoubleStateOf(80.0) }; var vol by remember { mutableDoubleStateOf(100.0) }
    // was schon einmal ausprobiert wurde, zählt für den Auftrag – auch wenn es wieder herausgenommen wird
    val tested = remember { mutableStateListOf<Pair<String, Liquid>>() }
    val goal = if (s.eachLiquid) s.things.all { t -> s.liquids.all { (t.name to it) in tested } } else s.things.all { t -> tested.any { it.first == t.name } }
    GoalWhen(!free && goal, onGoal)

    /** Antippen: hinein – nochmal antippen: wieder heraus */
    fun toggle(f: Floaty) {
        val here = dropped.filter { it.liquid == liquid }
        here.firstOrNull { it.f.key == f.key }?.let { old -> dropped.remove(old); if (last == old) last = null; sfx?.play(Sound.POP); return }
        val slot = (0..9).first { k -> here.none { it.slot == k } }
        val d = Dropped(f, liquid, now, slot)
        dropped += d; last = d
        if ((f.key to liquid) !in tested) tested += f.key to liquid
        sfx?.play(Sound.SPLASH)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            if (s.liquids.size > 1) ChipRow(s.liquids, liquid, { "${it.emoji} ${it.title}" }, { liquid = it }, Modifier.weight(1f), color = liquid.color.color())
            else Text("Becken mit ${liquid.title}", color = E.Muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
            GlowButton({ dropped.removeAll { it.liquid == liquid }; last = null; sfx?.play(Sound.WHOOSH) }, color = E.Sky) { Text("🧽 Leeren", fontSize = 13.sp) }
        }
        Canvas(Modifier.fillMaxWidth().height(270.dp).testTag("becken")) {
            val w = size.width; val h = size.height
            val left = w * 0.06f; val right = w * 0.94f; val top = h * 0.12f; val bottom = h * 0.96f
            val surface = top + (bottom - top) * 0.28f
            val obj = 46.dp.toPx()
            val lc = liquid.color.color()
            // Rückwand
            drawRoundRect(Color(0xFF1B1440), Offset(left, top), Size(right - left, bottom - top), CornerRadius(18f))
            val here = dropped.filter { it.liquid == liquid }
            // Gegenstände
            here.forEach { d ->
                val st = Buoyancy.state(d.f.rho, liquid.rho)
                val x = left + (right - left) * (0.12f + 0.19f * (d.slot % 5))
                val start = top - obj
                val target = when (st) {
                    Swim.SCHWIMMT -> surface + (Buoyancy.submerged(d.f.rho, liquid.rho).toFloat() - 0.5f) * obj
                    Swim.SCHWEBT -> (surface + bottom) / 2
                    Swim.SINKT -> bottom - obj / 2 - 4f
                }
                val t = now - d.at
                val y = when (st) {
                    Swim.SINKT -> { val p = (t / 1.1f).coerceIn(0f, 1f); start + (target - start) * (1 - (1 - p) * (1 - p)) }
                    else -> target + (start - target) * exp(-2.6f * t) * cos(6f * t)
                }
                label(tm, d.f.emoji, Offset(x, y), 30.sp)
                label(tm, d.f.title.substringBefore(' ').take(11), Offset(x, y + obj * 0.62f), 9.sp, Color.White)
            }
            // Flüssigkeit mit Welle (halbdurchsichtig, damit man Eingetauchtes sieht)
            val wave = Path().apply {
                moveTo(left, bottom); var x = left
                while (x <= right) { lineTo(x, surface + sin(x / 40f + now * 2.4f) * 3f); x += 8f }
                lineTo(right, bottom); close()
            }
            clipRect(left, top, right, bottom) { drawPath(wave, Brush.verticalGradient(listOf(lc.copy(alpha = 0.55f), lc.copy(alpha = 0.75f)), surface, bottom)) }
            // Lichtflecken
            repeat(6) { i -> drawCircle(Color.White.copy(alpha = 0.06f), 18f + i * 4, Offset(left + (right - left) * ((i * 0.17f + now * 0.03f) % 1f), surface + 30f + i * 18)) }
            drawRoundRect(Color.White.copy(alpha = 0.7f), Offset(left, top), Size(right - left, bottom - top), CornerRadius(18f), style = Stroke(3.dp.toPx()))
            drawBursts(bursts, now)
        }
        // Erklärung zum zuletzt eingetauchten Ding
        last?.let { d ->
            val st = Buoyancy.state(d.f.rho, d.liquid.rho)
            val sub = Buoyancy.submerged(d.f.rho, d.liquid.rho)
            Panel(border = if (st == Swim.SINKT) E.Orange else E.Mint) {
                Text("${d.f.emoji} ${d.f.title} ${st.title} in ${d.liquid.title}", fontWeight = FontWeight.Bold, color = E.Text)
                Text("Dichte: ${Fmt.num(d.f.rho)} g/cm³  ${if (d.f.rho < d.liquid.rho) "<" else if (st == Swim.SCHWEBT) "≈" else ">"}  ${d.liquid.title}: ${Fmt.num(d.liquid.rho)} g/cm³" +
                    if (st == Swim.SCHWIMMT) " · taucht zu ${Fmt.num(sub * 100, 0)} % ein" else "", color = E.Muted, fontSize = 13.sp)
                Text(d.f.note, color = E.Muted, fontSize = 13.sp)
            }
        }
        Text("Antippen = hineinwerfen · nochmal antippen = wieder herausholen", color = E.Muted, fontSize = 13.sp)
        val items = s.things.map { it.f() } + if (free) listOf(custom) else emptyList()
        ChipRow(items, null, { "${it.emoji} ${it.title}" + if (dropped.any { d -> d.f.key == it.key && d.liquid == liquid }) " ✓" else "" }, { toggle(it) },
            selectedIf = { dropped.any { d -> d.f.key == it.key && d.liquid == liquid } })
        if (free) Panel(border = E.Sun) {
            Text("🎁 Eigenes Ding: Masse und Volumen eingeben", fontWeight = FontWeight.Bold, color = E.Sun)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Masse", "g", 0.1, 10000.0, mass, { mass = it; custom = custom.copy(rho = mass / vol) }, Modifier.weight(1f))
                NumberField("Volumen", "cm³", 0.1, 10000.0, vol, { vol = it; custom = custom.copy(rho = mass / vol) }, Modifier.weight(1f))
            }
            Text("Dichte = Masse ÷ Volumen = ${Fmt.num(mass / vol)} g/cm³ → im ${liquid.title}: ${Buoyancy.state(mass / vol, liquid.rho).title}", color = E.Text, fontSize = 14.sp)
        }
    }
}

@Composable
fun TowerLab(s: TowerSetup, free: Boolean, onGoal: () -> Unit) {
    val now by rememberSeconds()
    val sfx = LocalSfx.current
    val tm = rememberTextMeasurer()
    val poured = remember { mutableStateListOf<Liquid>() }
    val things = remember { mutableStateListOf<Thing>() }
    val everPoured = remember { mutableStateListOf<Liquid>() }
    val layers = DensityTower.layers(poured)
    GoalWhen(!free && everPoured.size == s.liquids.size, onGoal)
    // Jede Flüssigkeit bekommt eine animierte Höhe (Index der Schicht) – neu Eingegossenes sinkt an seinen Platz
    val pos = Liquid.entries.associateWith { l ->
        val target = layers.indexOfFirst { l in it.liquids }.let { if (it < 0) layers.size.toFloat() else it.toFloat() }
        animateFloatAsState(target, spring(dampingRatio = 0.7f, stiffness = 40f), label = l.name).value
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { GlowButton({ poured.clear(); things.clear(); sfx?.play(Sound.WHOOSH) }, color = E.Sky) { Text("🧽 Glas leeren", fontSize = 13.sp) } }
        Canvas(Modifier.fillMaxWidth().height(300.dp).testTag("turm")) {
            val w = size.width; val h = size.height
            val left = w * 0.3f; val right = w * 0.7f; val bottom = h * 0.95f; val top = h * 0.06f
            val n = s.liquids.size.coerceAtLeast(1)
            val lh = (bottom - top) * 0.92f / n
            drawRoundRect(Color(0xFF1B1440), Offset(left, top), Size(right - left, bottom - top), CornerRadius(24f))
            layers.forEachIndexed { _, layer ->
                val idx = layer.liquids.map { pos[it] ?: 0f }.average().toFloat()
                val thick = lh * layer.liquids.size
                val y1 = bottom - idx * lh - thick
                val col = layer.liquids.map { it.color.color() }.let { cs -> Color(cs.map { it.red }.average().toFloat(), cs.map { it.green }.average().toFloat(), cs.map { it.blue }.average().toFloat()) }
                drawRect(Brush.verticalGradient(listOf(col.copy(alpha = 0.95f), col.copy(alpha = 0.7f)), y1, y1 + thick), Offset(left + 3, y1), Size(right - left - 6, thick))
                drawLine(Color.White.copy(alpha = 0.35f), Offset(left + 3, y1), Offset(right - 3, y1 + sin(now * 2) * 1.5f), 2f)
                label(tm, layer.liquids.joinToString(" ") { it.emoji } + "  " + Fmt.num(layer.rho), Offset(right + (w - right) / 2, y1 + thick / 2), 13.sp, E.Text)
            }
            // Dinge im Turm: schweben an der Grenze, wo die Flüssigkeit darunter dichter ist
            things.forEachIndexed { i, t ->
                // höchste Schicht, die dichter ist als das Ding: darauf schwimmt es
                val k = layers.indexOfLast { it.rho > t.rho }
                val below = layers.take(k + 1).sumOf { it.liquids.size } * lh
                val y = if (k < 0) bottom - 18f else bottom - below - 4f
                label(tm, t.emoji, Offset(left + (right - left) * (0.25f + 0.25f * (i % 3)), y.coerceIn(top + 20, bottom - 16)), 24.sp)
            }
            drawRoundRect(Color.White.copy(alpha = 0.75f), Offset(left, top), Size(right - left, bottom - top), CornerRadius(24f), style = Stroke(3.dp.toPx()))
            label(tm, "unten = dicht", Offset(left / 2, bottom - 12), 11.sp, E.Muted)
            label(tm, "oben = wenig dicht", Offset(left / 2, top + 12), 11.sp, E.Muted)
        }
        Text("Antippen = eingießen (beliebige Reihenfolge) · nochmal antippen = wieder abschöpfen", color = E.Muted, fontSize = 13.sp)
        ChipRow(s.liquids, null, { "${it.emoji} ${it.title}" + if (it in poured) " ✓" else "" }, { l -> if (l !in poured) { poured += l; if (l !in everPoured) everPoured += l; sfx?.play(Sound.BUBBLE) } else { poured -= l; sfx?.play(Sound.POP) } },
            selectedIf = { it in poured })
        if (layers.any { it.mixed }) Text("🌀 Wasser und Spiritus mischen sich – ohne Öl dazwischen gibt es keine Schicht.", color = E.Sun, fontSize = 13.sp)
        if (poured.size >= 2) {
            Text("Etwas hineinwerfen:", color = E.Muted, fontSize = 13.sp)
            ChipRow(listOf(Thing.TRAUBE, Thing.EIS, Thing.KORK, Thing.KARTOFFEL), null, { "${it.emoji} ${it.title} (${Fmt.num(it.rho)})" }, { if (it !in things) { things += it; sfx?.play(Sound.SPLASH) } else { things -= it; sfx?.play(Sound.POP) } },
                selectedIf = { it in things })
        }
    }
}
