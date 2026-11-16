package de.experimentopia.app.ui.lab

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.experimentopia.app.ui.common.*
import de.experimentopia.app.ui.fx.*
import de.experimentopia.app.ui.theme.E
import de.experimentopia.core.model.*
import kotlin.math.sin

@Composable
fun MagnetLab(s: MagnetSetup, free: Boolean, onGoal: () -> Unit) {
    var poles by remember { mutableStateOf(s.items.isEmpty()) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (free || (s.poles && s.items.isNotEmpty())) ChipRow(listOf(false, true), poles, { if (it) "🧭 Zwei Magnete" else "🧲 Was wird angezogen?" }, { poles = it })
        if (poles) PolesLab(free, onGoal) else ItemsLab(s.items, free, onGoal)
    }
}

@Composable
private fun ItemsLab(items: List<MagnetItem>, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val view = LocalView.current; val vib = LocalVibration.current
    val tm = rememberTextMeasurer()
    val now by rememberSeconds()
    var magnet by remember { mutableStateOf(Offset(-1f, -1f)) }
    val stuck = remember { mutableStateListOf<MagnetItem>() }
    val tested = remember { mutableStateListOf<MagnetItem>() }
    var last by remember { mutableStateOf<MagnetItem?>(null) }
    GoalWhen(!free && items.all { it in tested }, onGoal)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(280.dp).testTag("magnettisch").pointerInput(items) {
            detectDragGestures(onDragStart = { magnet = it }) { ch, d ->
                ch.consume(); magnet += d
                val cols = 4
                items.forEachIndexed { i, it ->
                    val home = Offset(size.width * (0.14f + 0.24f * (i % cols)), size.height * (0.3f + 0.36f * (i / cols)))
                    if (it !in stuck && (home - magnet).getDistance() < 44.dp.toPx()) {
                        if (it !in tested) tested += it
                        last = it
                        if (it.magnetic) { stuck += it; sfx?.play(Sound.CLANK); view.buzz(Buzz.HIT, vib) }
                    }
                }
            }
        }) {
            drawRoundRect(Color(0xFF2A1F55), Offset.Zero, size, CornerRadius((24f * density)))
            val cols = 4
            items.forEachIndexed { i, it ->
                val home = Offset(size.width * (0.14f + 0.24f * (i % cols)), size.height * (0.3f + 0.36f * (i / cols)))
                val k = stuck.indexOf(it)
                val p = if (k >= 0 && magnet.x >= 0) magnet + Offset(-(30f * density) + (k % 3) * (30f * density), (46f * density) + (k / 3) * (26f * density)) else home
                val wobble = if (it in tested && !it.magnetic && it == last) sin(now * 30f) * (2f * density) else 0f
                label(tm, it.emoji, p + Offset(wobble, 0f), 30.sp)
                if (k < 0) {
                    label(tm, it.title, home + Offset(0f, (30f * density)), 10.sp, E.Muted)
                    if (it in tested) label(tm, if (it.magnetic) "🧲" else "✗", home + Offset((26f * density), -(22f * density)), 14.sp, if (it.magnetic) E.Mint else E.Muted)
                }
            }
            if (magnet.x >= 0) {
                // Hufeisenmagnet
                val m = magnet
                drawArc(E.Red, 180f, 180f, false, Offset(m.x - 28f, m.y - 40f), Size(56f, 56f), style = Stroke(16f))
                drawLine(E.Red, Offset(m.x - (28f * density), m.y - (12f * density)), Offset(m.x - (28f * density), m.y + (8f * density)), (16f * density))
                drawLine(E.Sky, Offset(m.x + (28f * density), m.y - (12f * density)), Offset(m.x + (28f * density), m.y + (8f * density)), (16f * density))
                drawLine(Color.White, Offset(m.x - (28f * density), m.y + (8f * density)), Offset(m.x - (28f * density), m.y + (18f * density)), (16f * density))
                drawLine(Color.White, Offset(m.x + (28f * density), m.y + (8f * density)), Offset(m.x + (28f * density), m.y + (18f * density)), (16f * density))
                repeat(3) { k -> drawCircle(E.Sun.copy(alpha = 0.25f * (1 - ((now * 1.5f + k / 3f) % 1f))), (30f * density) + (50f * density) * ((now * 1.5f + k / 3f) % 1f), m + Offset(0f, (14f * density)), style = Stroke((2f * density))) }
            } else label(tm, "👆 Hier wischen: der Magnet folgt deinem Finger", Offset(size.width / 2, size.height * 0.92f), 12.sp, E.Muted)
        }
        last?.let { Panel(border = if (it.magnetic) E.Mint else E.Muted) {
            Text("${it.emoji} ${it.title}: ${if (it.magnetic) "wird angezogen!" else "wird nicht angezogen"}", fontWeight = FontWeight.Bold, color = E.Text)
            Text(it.note, color = E.Muted, fontSize = 13.sp)
        } }
        Text("Getestet: ${tested.size} von ${items.size}", color = E.Muted, fontSize = 13.sp)
        GlowButton({ stuck.clear(); sfx?.play(Sound.WHOOSH) }, color = E.Sky) { Text("👋 Abschütteln") }
    }
}

@Composable
private fun PolesLab(free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val view = LocalView.current; val vib = LocalVibration.current
    val tm = rememberTextMeasurer()
    var flipped by remember { mutableStateOf(false) }   // false: rechter Magnet zeigt mit N nach links
    var x by remember { mutableFloatStateOf(0.75f) }
    var sawAttract by remember { mutableStateOf(false) }
    var sawRepel by remember { mutableStateOf(false) }
    val anim = remember { Animatable(0.75f) }
    GoalWhen(!free && sawAttract && sawRepel, onGoal)
    // linker Magnet: S | N  (N zeigt nach rechts). Rechter Magnet zeigt nach links mit N (gleich) oder S (verschieden).
    val facing = if (flipped) 'S' else 'N'
    val attract = Magnets.attract('N', facing)
    LaunchedEffect(x, flipped) {
        if (x < 0.62f) {
            if (attract) { anim.animateTo(0.5f, spring(stiffness = 300f)); sfx?.play(Sound.CLANK); view.buzz(Buzz.HIT, vib); sawAttract = true }
            else { anim.snapTo(x); sfx?.play(Sound.WHOOSH); anim.animateTo(0.8f, spring(dampingRatio = 0.3f, stiffness = 120f)); sawRepel = true }
            x = anim.value
        } else anim.snapTo(x)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(180.dp).testTag("pole").pointerInput(Unit) {
            detectDragGestures { ch, d -> ch.consume(); x = (x + d.x / size.width).coerceIn(0.45f, 0.92f) }
        }) {
            val y = size.height / 2; val mw = size.width * 0.22f; val mh = (46f * density)
            fun bar(cx: Float, leftPole: Char) {
                val rightPole = if (leftPole == 'N') 'S' else 'N'
                fun col(p: Char) = if (p == 'N') E.Red else E.Sky
                drawRoundRect(col(leftPole), Offset(cx - mw / 2, y - mh / 2), Size(mw / 2, mh), CornerRadius((8f * density)))
                drawRoundRect(col(rightPole), Offset(cx, y - mh / 2), Size(mw / 2, mh), CornerRadius((8f * density)))
                label(tm, "$leftPole", Offset(cx - mw / 4, y), 18.sp, Color.White, true); label(tm, "$rightPole", Offset(cx + mw / 4, y), 18.sp, Color.White, true)
            }
            val lx = size.width * 0.27f
            bar(lx, 'S')
            val rx = size.width * anim.value.coerceAtLeast(0.5f)
            bar(rx, facing)
            // Feldlinien andeuten
            val gap = rx - mw / 2 - (lx + mw / 2)
            if (gap > 4) repeat(4) { k ->
                val yy = y - (30f * density) + k * (20f * density)
                if (attract) drawLine(E.Sun.copy(alpha = 0.6f), Offset(lx + mw / 2, yy), Offset(rx - mw / 2, yy), (2f * density), StrokeCap.Round)
                else { drawLine(E.Pink.copy(alpha = 0.6f), Offset(lx + mw / 2, yy), Offset(lx + mw / 2 + gap * 0.35f, yy - (14f * density) + k * (9f * density)), (2f * density)); drawLine(E.Pink.copy(alpha = 0.6f), Offset(rx - mw / 2, yy), Offset(rx - mw / 2 - gap * 0.35f, yy - (14f * density) + k * (9f * density)), (2f * density)) }
            }
        }
        Text("👈 Den rechten Magneten zum linken schieben.", color = E.Muted, fontSize = 13.sp)
        GlowButton({ flipped = !flipped; x = 0.8f; sfx?.play(Sound.WHOOSH) }, color = E.Sun) { Text("🔄 Rechten Magneten umdrehen (zeigt jetzt mit ${if (flipped) "N" else "S"})") }
        Text((if (sawAttract) "✅" else "⬜") + " verschiedene Pole ausprobiert   " + (if (sawRepel) "✅" else "⬜") + " gleiche Pole ausprobiert", color = E.Text, fontSize = 13.sp)
    }
}
