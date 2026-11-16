package de.experimentopia.app.ui.lab

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import kotlin.random.Random

/** Kugelmodell eines Moleküls: Atome dicht gepackt um die Mitte (Wasserstoff kleiner) */
private fun DrawScope.molecule(formula: String, center: Offset, scale: Float) {
    val atoms = Formula.atoms(formula).flatMap { (e, n) -> List(n) { e } }.sortedByDescending { if (it == Element.H) 0 else 1 }
    val big = 11f * scale * density; val small = 7f * scale * density
    atoms.forEachIndexed { i, e ->
        val r = if (e == Element.H) small else big
        val p = if (i == 0) center else {
            val ring = sqrt(i.toFloat()).toInt().coerceAtLeast(1)
            val ang = i * 2.39996f   // goldener Winkel: gleichmäßig verteilt
            center + Offset(cos(ang), sin(ang)) * (ring * big * 1.25f)
        }
        val c = e.color.color()
        drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.9f), c, c.copy(alpha = 0.9f)), p - Offset(r * 0.35f, r * 0.35f), r * 1.4f), r, p)
    }
}

@Composable
fun ReactionLab(s: ReactionSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val view = LocalBuzzer.current; val vib = LocalVibration.current
    var reaction by remember { mutableStateOf(Reactions.byId(s.reactionId)) }
    val coeffs = remember(reaction) { mutableStateListOf(*Array(reaction.all.size) { 1 }) }
    val balanced = reaction.balanced(coeffs) && !reaction.notSmallest(coeffs)
    var boomAt by remember { mutableFloatStateOf(-10f) }
    val now by rememberSeconds()
    LaunchedEffect(balanced, reaction) {
        if (balanced) { boomAt = now; sfx?.play(when (reaction.effect) { Effect.KNALL -> Sound.BANG; Effect.SPRUDEL, Effect.SCHAUM -> Sound.FIZZ; Effect.BLITZ -> Sound.ZAP; else -> Sound.WIN }); view.buzz(Buzz.BIG, vib) }
    }
    GoalWhen(!free && balanced, onGoal)
    val left = Formula.count(reaction.left.map { it.formula }, coeffs.take(reaction.left.size))
    val right = Formula.count(reaction.right.map { it.formula }, coeffs.drop(reaction.left.size))
    val shake = if (balanced && reaction.effect == Effect.KNALL && now - boomAt < 0.5f) sin(now * 90f) * 10f * (0.5f - (now - boomAt)) else 0f

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.graphicsLayer { translationX = shake }) {
        if (free) ChipRow(Reactions.all, reaction, { "${it.emoji} ${it.title}" }, { reaction = it })
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF120C28)).testTag("reaktion")) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                reaction.all.forEachIndexed { k, m ->
                    if (k > 0) Text(if (k == reaction.left.size) "→" else "+", color = E.Text, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Stepper("−") { if (coeffs[k] > 1) { coeffs[k]--; sfx?.play(Sound.TAP, pitch = 0.9f) } }
                            Text("${coeffs[k]}", color = E.Sun, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp))
                            Stepper("+") { if (coeffs[k] < 15) { coeffs[k]++; sfx?.play(Sound.POP, pitch = 1f + coeffs[k] * 0.05f) } }
                        }
                        Canvas(Modifier.size(84.dp, 84.dp)) {
                            val n = coeffs[k]; val shown = n.coerceAtMost(4)
                            val sc = if (Formula.atoms(m.formula).values.sum() > 8) 0.45f else if (shown > 1) 0.75f else 1f
                            repeat(shown) { j -> molecule(m.formula, Offset(size.width * (if (shown == 1) 0.5f else 0.3f + 0.4f * (j % 2)), size.height * (if (shown <= 2) 0.5f else 0.3f + 0.4f * (j / 2))), sc) }
                            if (n > 4) drawCircle(E.Sun, 5f * density, Offset(size.width - 6f * density, 6f * density))
                        }
                        Text(Formula.pretty(m.formula), color = E.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(m.name, color = E.Muted, fontSize = 10.sp)
                    }
                }
            }
            if (balanced) EffectOverlay(reaction.effect, now - boomAt, Modifier.matchParentSize())
        }
        // Atome zählen
        Panel(border = if (balanced) E.Green else E.Line) {
            Text("Atome zählen: links und rechts gleich viele?", color = E.Muted, fontSize = 13.sp)
            (left.keys + right.keys).distinct().forEach { e ->
                val a = left[e] ?: 0; val b = right[e] ?: 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(18.dp).clip(CircleShape).background(e.color.color()).border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape))
                    Text(" ${e.symbol}", color = E.Text, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
                    Text("links $a", color = E.Text, modifier = Modifier.width(70.dp))
                    Text("rechts $b", color = E.Text, modifier = Modifier.width(80.dp))
                    Text(if (a == b) "✅" else if (a < b) "⬅️ links fehlen ${b - a}" else "➡️ rechts fehlen ${a - b}", color = if (a == b) E.Green else E.Orange, fontSize = 13.sp)
                }
            }
            if (reaction.notSmallest(coeffs)) Text("Stimmt – aber es geht mit kleineren Zahlen!", color = E.Sun, fontSize = 13.sp)
            if (balanced) Text("🎉 ${reaction.equation()}\n${reaction.story}", color = E.Green, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Stepper(text: String, onClick: () -> Unit) {
    Box(Modifier.size(32.dp).clip(CircleShape).background(E.Violet.copy(alpha = 0.3f)).border(2.dp, E.Violet, CircleShape).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Black, color = E.Text)
    }
}

/** Was bei der Reaktion zu sehen ist */
@Composable
private fun EffectOverlay(effect: Effect, t: Float, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val r = Random(effect.ordinal)
        when (effect) {
            Effect.KNALL, Effect.BLITZ -> {
                val a = (1f - t / 0.6f).coerceIn(0f, 1f)
                drawRect(Color.White.copy(alpha = a * 0.85f))
                if (effect == Effect.BLITZ) repeat(30) { val p = Offset(r.nextFloat() * w, r.nextFloat() * h); drawCircle(Color.White.copy(alpha = (0.8f * (1 - ((t * 2 + r.nextFloat()) % 1f)))), 3f, p) }
                else repeat(40) { k -> val ang = r.nextFloat() * 6.28f; val d = t * 400f * (0.4f + r.nextFloat()); drawCircle(listOf(E.Sun, E.Orange, Color.White)[k % 3].copy(alpha = (1 - t / 1.2f).coerceIn(0f, 1f)), 4f, Offset(w / 2 + cos(ang) * d, h / 2 + sin(ang) * d)) }
            }
            Effect.FLAMME -> repeat(36) { k ->
                val p = (t * 0.9f + k / 36f) % 1f
                val x = w * (0.1f + 0.8f * r.nextFloat()) + sin(t * 8 + k) * 6f
                drawCircle(listOf(E.Sun, E.Orange, E.Red)[k % 3].copy(alpha = 0.55f * (1 - p)), 10f * (1 - p) + 3f, Offset(x, h - p * h * 0.8f))
            }
            Effect.SPRUDEL, Effect.SCHAUM -> repeat(if (effect == Effect.SCHAUM) 70 else 40) { k ->
                val p = (t * 0.6f + k / 50f) % 1f
                val rad = if (effect == Effect.SCHAUM) 8f + (k % 5) * 3f else 4f + (k % 3) * 2f
                val top = if (effect == Effect.SCHAUM) h * (1 - (t / 3f).coerceAtMost(1f)) else 0f
                drawCircle(Color.White.copy(alpha = 0.5f), rad, Offset(w * r.nextFloat(), (h - p * (h - top))))
            }
            Effect.ROST -> repeat(50) { k -> drawCircle(Color(0xFFB5541C).copy(alpha = (t / 2f).coerceAtMost(0.7f)), 4f + (k % 4), Offset(w * r.nextFloat(), h * r.nextFloat())) }
            Effect.BLATT -> { drawRect(E.Green.copy(alpha = (0.25f * sin(t * 2)).coerceAtLeast(0f))); repeat(20) { k -> val p = (t * 0.4f + k / 20f) % 1f; drawCircle(Color(0xFFBFFFD0).copy(alpha = 0.7f * (1 - p)), 4f, Offset(w * r.nextFloat(), h - p * h)) } }
            Effect.KRISTALL -> repeat(18) { k -> val s = (t * 2 - k * 0.08f).coerceIn(0f, 1f) * 14f; val p = Offset(w * r.nextFloat(), h * r.nextFloat()); drawRect(Color.White.copy(alpha = 0.8f), p - Offset(s / 2, s / 2), Size(s, s)) }
            Effect.NEBEL -> repeat(12) { k -> val p = (t * 0.2f + k / 12f) % 1f; drawCircle(Color.White.copy(alpha = 0.12f * (1 - p)), 40f + 30f * p, Offset(w * r.nextFloat(), h * (1 - p))) }
        }
    }
}
