package de.kurvenflug.app.ui.volt

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.kurvenflug.app.ui.theme.Q
import kotlin.math.sin

/** Volts Hut aus der Werkstatt (0 = ohne). */
val LocalVoltHat = androidx.compose.runtime.staticCompositionLocalOf { 0 }

/** Volt plappert beim Sprechen (in den Einstellungen abschaltbar). */
val LocalVoltVoice = androidx.compose.runtime.staticCompositionLocalOf { false }

/** Volts Stimmungen. */
enum class VoltMood { IDLE, THINK, HAPPY, OOPS, WOW, SLEEP, DANCE }

/**
 * Volt, der kleine Roboter: schwebt, blinzelt, die Antenne blinkt.
 * Seine Augen sind Bildschirme und zeigen die Stimmung. Optional mit Sprechblase.
 */
@Composable
fun Volt(mood: VoltMood, modifier: Modifier = Modifier, size: Dp = 72.dp, speech: String? = null) {
    val hat = LocalVoltHat.current
    val t by rememberInfiniteTransition(label = "volt").animateFloat(0f, 1000f,
        infiniteRepeatable(tween(1_000_000, easing = LinearEasing)), label = "t")
    val hop = remember { Animatable(0f) }
    val spin = remember { Animatable(0f) }
    val sfx = de.kurvenflug.app.ui.fx.LocalSfx.current
    val voice = LocalVoltVoice.current
    // Vorlesen an: Volt spricht richtig (antippen = nochmal); sonst plappert er in Roboter-Sprache
    val readAloud = de.kurvenflug.app.ui.fx.LocalReadAloud.current
    val reader = de.kurvenflug.app.ui.fx.LocalReader.current
    val me = remember { Any() }
    LaunchedEffect(speech, readAloud) {
        if (speech == null || mood == VoltMood.SLEEP) return@LaunchedEffect
        if (readAloud) de.kurvenflug.app.ui.fx.Voice.say(reader, speech, me) else if (voice) sfx?.babble(speech)
    }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { de.kurvenflug.app.ui.fx.Voice.leave(reader, me) } }
    LaunchedEffect(mood) {
        if (mood == VoltMood.DANCE) {
            // Freudentanz: Hüpfer und eine ganze Drehung
            spin.snapTo(0f)
            repeat(2) { hop.animateTo(1f, tween(160)); hop.animateTo(0f, spring(dampingRatio = 0.4f)) }
            spin.animateTo(360f, tween(600, easing = FastOutSlowInEasing))
        } else if (mood == VoltMood.HAPPY || mood == VoltMood.WOW) {
            hop.snapTo(0f); hop.animateTo(1f, tween(180)); hop.animateTo(0f, spring(dampingRatio = 0.35f))
        } else if (mood == VoltMood.OOPS) {
            repeat(3) { hop.animateTo(-0.15f, tween(50)); hop.animateTo(0.15f, tween(50)) }; hop.animateTo(0f)
        }
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(size).graphicsLayer {
            if (mood == VoltMood.OOPS) translationX = hop.value * size.toPx() * 0.3f
            else translationY = -hop.value * size.toPx() * 0.25f
            rotationZ = spin.value
        }) { drawVolt(mood, t, hat) }
        AnimatedContent(speech, transitionSpec = { (scaleIn(spring(dampingRatio = 0.5f)) + fadeIn()) togetherWith fadeOut(tween(100)) },
            label = "say", modifier = Modifier.weight(1f, fill = false)) { s ->
            if (s != null) Surface(
                color = Q.Panel, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(start = 8.dp).border(1.5.dp, Q.Cyan.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .then(if (readAloud) Modifier.clip(RoundedCornerShape(16.dp)).clickable { de.kurvenflug.app.ui.fx.Voice.say(reader, s, me) } else Modifier),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(s, color = Q.Text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f, fill = false).padding(start = 12.dp, end = if (readAloud) 4.dp else 12.dp, top = 9.dp, bottom = 9.dp))
                    if (readAloud) Text("🔊", fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp, end = 8.dp).graphicsLayer { alpha = 0.7f })
                }
            }
        }
    }
}

fun DrawScope.drawVolt(mood: VoltMood, t: Float, hat: Int = 0) {
    val w = size.width; val h = size.height
    val bob = sin(t * (if (mood == VoltMood.SLEEP) 0.8f else 2.2f)) * h * 0.03f
    val body = Color(0xFFDDE3FF); val shade = Color(0xFFAEB7E8); val dark = Color(0xFF1A1F45)
    // Düse unten
    val flame = 0.6f + 0.4f * sin(t * 25f)
    drawCircle(Q.Orange.copy(alpha = 0.7f), w * 0.07f * flame, Offset(w * 0.5f, h * 0.92f + bob))
    drawCircle(Q.Yellow.copy(alpha = 0.9f), w * 0.04f * flame, Offset(w * 0.5f, h * 0.9f + bob))
    // Körper
    drawRoundRect(shade, Offset(w * 0.3f, h * 0.62f + bob), Size(w * 0.4f, h * 0.24f), CornerRadius(w * 0.08f))
    drawRoundRect(body, Offset(w * 0.32f, h * 0.62f + bob), Size(w * 0.36f, h * 0.21f), CornerRadius(w * 0.07f))
    drawCircle(Q.Cyan, w * 0.035f, Offset(w * 0.5f, h * 0.72f + bob))
    // Arme: bei Freude hoch
    val armUp = when (mood) {
        VoltMood.HAPPY, VoltMood.WOW -> -0.12f
        VoltMood.DANCE -> -0.12f + 0.1f * sin(t * 12f)
        VoltMood.SLEEP -> 0.1f
        else -> 0.04f
    }
    drawLine(shade, Offset(w * 0.31f, h * 0.68f + bob), Offset(w * 0.17f, h * (0.72f + armUp) + bob), w * 0.05f, StrokeCap.Round)
    drawLine(shade, Offset(w * 0.69f, h * 0.68f + bob), Offset(w * 0.83f, h * (0.72f + armUp) + bob), w * 0.05f, StrokeCap.Round)
    // Antenne mit blinkendem Licht
    drawLine(shade, Offset(w * 0.5f, h * 0.2f + bob), Offset(w * 0.5f, h * 0.08f + bob), w * 0.025f)
    val blink = if ((t * 1.5f).toInt() % 2 == 0) Q.Pink else Q.Pink.copy(alpha = 0.4f)
    drawCircle(blink, w * 0.045f, Offset(w * 0.5f, h * 0.07f + bob))
    // Kopf
    drawRoundRect(shade, Offset(w * 0.16f, h * 0.2f + bob), Size(w * 0.68f, h * 0.44f), CornerRadius(w * 0.16f))
    drawRoundRect(body, Offset(w * 0.18f, h * 0.2f + bob), Size(w * 0.64f, h * 0.41f), CornerRadius(w * 0.15f))
    // Bildschirm-Gesicht
    val face = Offset(w * 0.25f, h * 0.27f + bob); val fs = Size(w * 0.5f, h * 0.27f)
    drawRoundRect(dark, face, fs, CornerRadius(w * 0.08f))
    val eye = when (mood) { VoltMood.OOPS -> Q.Orange; VoltMood.HAPPY, VoltMood.WOW, VoltMood.DANCE -> Q.Green; VoltMood.SLEEP -> Q.Violet; else -> Q.Cyan }
    val ey = face.y + fs.height * 0.45f
    val lx = face.x + fs.width * 0.3f; val rx = face.x + fs.width * 0.7f
    val er = w * 0.055f
    val blinking = (t * 0.8f) % 4f < 0.08f
    val stroke = Stroke(w * 0.035f, cap = StrokeCap.Round)
    when {
        mood == VoltMood.SLEEP -> {
            // Augen zu, kleine „z“ steigen auf
            listOf(lx, rx).forEach { x -> drawArc(eye, 20f, 140f, false, Offset(x - er, ey - er * 1.2f), Size(er * 2, er * 2), style = stroke) }
            for (i in 0..2) {
                val k = ((t * 0.6f + i / 3f) % 1f)
                val zx = w * (0.78f + 0.08f * k); val zy = h * (0.2f - 0.15f * k) + bob; val zs = w * (0.03f + 0.03f * k)
                val a = 1f - k
                drawLine(Q.Violet.copy(alpha = a), Offset(zx, zy), Offset(zx + zs, zy), w * 0.015f)
                drawLine(Q.Violet.copy(alpha = a), Offset(zx + zs, zy), Offset(zx, zy + zs), w * 0.015f)
                drawLine(Q.Violet.copy(alpha = a), Offset(zx, zy + zs), Offset(zx + zs, zy + zs), w * 0.015f)
            }
        }
        blinking -> { drawLine(eye, Offset(lx - er, ey), Offset(lx + er, ey), w * 0.035f, StrokeCap.Round); drawLine(eye, Offset(rx - er, ey), Offset(rx + er, ey), w * 0.035f, StrokeCap.Round) }
        mood == VoltMood.HAPPY || mood == VoltMood.DANCE -> listOf(lx, rx).forEach { x ->
            drawArc(eye, 200f, 140f, false, Offset(x - er, ey - er * 0.4f), Size(er * 2, er * 2), style = stroke)
        }
        mood == VoltMood.OOPS -> listOf(lx, rx).forEach { x ->
            drawLine(eye, Offset(x - er * 0.8f, ey - er * 0.8f), Offset(x + er * 0.8f, ey + er * 0.8f), w * 0.03f, StrokeCap.Round)
            drawLine(eye, Offset(x + er * 0.8f, ey - er * 0.8f), Offset(x - er * 0.8f, ey + er * 0.8f), w * 0.03f, StrokeCap.Round)
        }
        mood == VoltMood.WOW -> listOf(lx, rx).forEach { x -> drawCircle(eye, er * 1.15f, Offset(x, ey), style = stroke) }
        mood == VoltMood.THINK -> {
            drawCircle(eye, er * 0.8f, Offset(lx, ey))
            drawLine(eye, Offset(rx - er, ey - er * 0.3f), Offset(rx + er, ey - er * 0.6f), w * 0.035f, StrokeCap.Round)
        }
        else -> listOf(lx, rx).forEach { x -> drawCircle(eye, er * 0.85f, Offset(x, ey)) }
    }
    // Mund: kleine Pixel-Linie
    val my = face.y + fs.height * 0.8f
    val mw = if (mood == VoltMood.WOW) fs.width * 0.1f else fs.width * 0.22f
    if (mood == VoltMood.WOW) drawCircle(eye, w * 0.025f, Offset(w * 0.5f, my))
    else drawLine(eye.copy(alpha = 0.8f), Offset(w * 0.5f - mw / 2, my), Offset(w * 0.5f + mw / 2, my), w * 0.022f, StrokeCap.Round)
    drawHat(hat, w, h, bob)
}

/** Hüte: 1 Helm, 2 Partyhut, 3 Kopfhörer, 4 Krone. */
private fun DrawScope.drawHat(hat: Int, w: Float, h: Float, bob: Float) {
    val top = h * 0.2f + bob
    when (hat) {
        1 -> {
            drawArc(Color(0xFFFF5252), 180f, 180f, true, Offset(w * 0.2f, top - h * 0.12f), Size(w * 0.6f, h * 0.26f))
            drawLine(Color.White, Offset(w * 0.5f, top - h * 0.12f), Offset(w * 0.5f, top + h * 0.01f), w * 0.04f)
        }
        2 -> {
            val p = androidx.compose.ui.graphics.Path().apply { moveTo(w * 0.38f, top + h * 0.01f); lineTo(w * 0.5f, top - h * 0.22f); lineTo(w * 0.62f, top + h * 0.01f); close() }
            drawPath(p, Q.Pink)
            drawCircle(Q.Yellow, w * 0.04f, Offset(w * 0.5f, top - h * 0.22f))
            drawCircle(Q.Yellow, w * 0.02f, Offset(w * 0.47f, top - h * 0.08f)); drawCircle(Q.Cyan, w * 0.02f, Offset(w * 0.54f, top - h * 0.13f))
        }
        3 -> {
            drawArc(Color(0xFF263238), 180f, 180f, false, Offset(w * 0.12f, top - h * 0.06f), Size(w * 0.76f, h * 0.4f), style = Stroke(w * 0.05f))
            drawRoundRect(Q.Pink, Offset(w * 0.08f, top + h * 0.1f), Size(w * 0.1f, h * 0.18f), CornerRadius(w * 0.04f))
            drawRoundRect(Q.Pink, Offset(w * 0.82f, top + h * 0.1f), Size(w * 0.1f, h * 0.18f), CornerRadius(w * 0.04f))
        }
        4 -> {
            val p = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.32f, top + h * 0.01f); lineTo(w * 0.32f, top - h * 0.12f); lineTo(w * 0.41f, top - h * 0.05f)
                lineTo(w * 0.5f, top - h * 0.15f); lineTo(w * 0.59f, top - h * 0.05f); lineTo(w * 0.68f, top - h * 0.12f); lineTo(w * 0.68f, top + h * 0.01f); close()
            }
            drawPath(p, Q.Yellow)
            drawCircle(Q.Pink, w * 0.025f, Offset(w * 0.5f, top - h * 0.05f))
        }
    }
}
