package de.experimentopia.app.ui.blubb

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.experimentopia.app.ui.fx.LocalSfx
import de.experimentopia.app.ui.fx.Sound
import de.experimentopia.app.ui.fx.rememberSeconds
import de.experimentopia.app.ui.theme.E
import kotlinx.coroutines.delay
import kotlin.math.sin

/** Blubbs Stimmung – die Flüssigkeit im Kolben wechselt die Farbe mit. */
enum class Mood(val liquid: Color) {
    HAPPY(E.Mint), WOW(E.Sun), THINK(E.Sky), OOPS(E.Violet), CHEER(E.Pink)
}

/**
 * Blubb, der Erlenmeyerkolben: ein neugieriger Laborkolben mit Gesicht. Die Flüssigkeit schwappt,
 * Bläschen steigen auf, er blinzelt – und hüpft vor Freude.
 */
@Composable
fun Blubb(mood: Mood, modifier: Modifier = Modifier, size: Dp = 96.dp) {
    val t by rememberSeconds()
    val liquid by animateColorAsState(mood.liquid, tween(500), label = "liquid")
    Canvas(modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        val hop = if (mood == Mood.CHEER) -kotlin.math.abs(sin(t * 7f)) * h * 0.06f else 0f
        val tilt = when (mood) { Mood.THINK -> 6f; Mood.OOPS -> -5f; else -> sin(t * 1.4f) * 2f }
        withTransform({ translate(0f, hop); rotate(tilt, Offset(w / 2, h * 0.9f)) }) {
            // Umriss des Kolbens
            val neckL = w * 0.38f; val neckR = w * 0.62f; val top = h * 0.08f; val shoulder = h * 0.42f
            val body = Path().apply {
                moveTo(neckL, top); lineTo(neckL, shoulder)
                lineTo(w * 0.1f, h * 0.86f); quadraticTo(w * 0.07f, h * 0.95f, w * 0.18f, h * 0.95f)
                lineTo(w * 0.82f, h * 0.95f); quadraticTo(w * 0.93f, h * 0.95f, w * 0.9f, h * 0.86f)
                lineTo(neckR, shoulder); lineTo(neckR, top); close()
            }
            // Leuchten
            drawCircle(Brush.radialGradient(listOf(liquid.copy(alpha = 0.35f), Color.Transparent), Offset(w / 2, h * 0.75f), w * 0.6f), w * 0.6f, Offset(w / 2, h * 0.75f))
            drawPath(body, Color.White.copy(alpha = 0.10f))
            // Flüssigkeit mit Welle
            val level = h * 0.52f
            clipPath(body) {
                val wave = Path().apply {
                    moveTo(0f, h)
                    var x = 0f
                    while (x <= w) { lineTo(x, level + sin(x / w * 9f + t * 3.2f) * h * 0.02f); x += w / 24 }
                    lineTo(w, h); close()
                }
                drawPath(wave, Brush.verticalGradient(listOf(liquid.copy(alpha = 0.95f), liquid.copy(alpha = 0.7f)), level, h))
                // Bläschen
                repeat(5) { i ->
                    val p = ((t * (0.35f + i * 0.07f) + i * 0.23f) % 1f)
                    val bx = w * (0.3f + 0.1f * i) + sin(t * 2 + i) * w * 0.02f
                    val by = h * 0.92f - p * (h * 0.92f - level)
                    drawCircle(Color.White.copy(alpha = 0.55f * (1 - p)), w * (0.018f + 0.006f * (i % 3)), Offset(bx, by))
                }
            }
            drawPath(body, Color.White.copy(alpha = 0.9f), style = Stroke(w * 0.035f, join = StrokeJoin.Round))
            drawLine(Color.White.copy(alpha = 0.9f), Offset(w * 0.32f, top), Offset(w * 0.68f, top), w * 0.05f, StrokeCap.Round)
            // Glanz
            drawLine(Color.White.copy(alpha = 0.45f), Offset(w * 0.26f, h * 0.66f), Offset(w * 0.36f, h * 0.5f), w * 0.03f, StrokeCap.Round)

            // Gesicht
            val eyeY = h * 0.70f
            val blink = ((t % 3.7f) < 0.12f)
            val eyeR = w * 0.075f
            listOf(w * 0.38f, w * 0.62f).forEach { ex ->
                if (blink) drawLine(E.Night, Offset(ex - eyeR, eyeY), Offset(ex + eyeR, eyeY), w * 0.025f, StrokeCap.Round)
                else {
                    val big = if (mood == Mood.WOW) 1.25f else 1f
                    drawCircle(Color.White, eyeR * big, Offset(ex, eyeY))
                    val look = when (mood) { Mood.THINK -> Offset(eyeR * 0.35f, -eyeR * 0.4f); else -> Offset(sin(t * 0.7f) * eyeR * 0.25f, 0f) }
                    drawCircle(E.Night, eyeR * 0.55f * big, Offset(ex, eyeY) + look)
                    drawCircle(Color.White, eyeR * 0.18f, Offset(ex, eyeY) + look + Offset(-eyeR * 0.2f, -eyeR * 0.25f))
                }
            }
            // Bäckchen
            drawCircle(E.Pink.copy(alpha = 0.45f), w * 0.05f, Offset(w * 0.27f, h * 0.79f))
            drawCircle(E.Pink.copy(alpha = 0.45f), w * 0.05f, Offset(w * 0.73f, h * 0.79f))
            // Mund
            val my = h * 0.82f
            when (mood) {
                Mood.HAPPY -> drawArc(E.Night, 15f, 150f, false, Offset(w * 0.42f, my - h * 0.05f), Size(w * 0.16f, h * 0.08f), style = Stroke(w * 0.03f, cap = StrokeCap.Round))
                Mood.CHEER -> drawArc(E.Night, 0f, 180f, true, Offset(w * 0.4f, my - h * 0.05f), Size(w * 0.2f, h * 0.11f))
                Mood.WOW -> drawOval(E.Night, Offset(w * 0.46f, my - h * 0.03f), Size(w * 0.08f, h * 0.08f))
                Mood.THINK -> drawLine(E.Night, Offset(w * 0.45f, my), Offset(w * 0.56f, my - h * 0.015f), w * 0.03f, StrokeCap.Round)
                Mood.OOPS -> { val p = Path().apply { moveTo(w * 0.42f, my); quadraticTo(w * 0.46f, my - h * 0.03f, w * 0.5f, my); quadraticTo(w * 0.54f, my + h * 0.03f, w * 0.58f, my) }
                    drawPath(p, E.Night, style = Stroke(w * 0.028f, cap = StrokeCap.Round)) }
            }
        }
    }
}

/** Blubb mit Sprechblase. Der Text tippt sich schnell ein – antippen zeigt sofort alles. */
@Composable
fun BlubbSays(text: String, mood: Mood = Mood.HAPPY, modifier: Modifier = Modifier, size: Dp = 72.dp, speak: Boolean = true) {
    val sfx = LocalSfx.current
    var shown by remember(text) { mutableIntStateOf(0) }
    LaunchedEffect(text) {
        if (speak) sfx?.play(Sound.BLUBB, volume = 0.5f)
        while (shown < text.length) { delay(12); shown = (shown + 2).coerceAtMost(text.length) }
    }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Blubb(mood, size = size)
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier.weight(1f).background(E.Panel, RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
                .clickable(remember { MutableInteractionSource() }, null) { shown = text.length }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Platz für den ganzen Text reservieren, damit nichts springt
            Text(text, style = MaterialTheme.typography.bodyLarge, color = Color.Transparent)
            Text(text.take(shown), style = MaterialTheme.typography.bodyLarge, color = E.Text)
        }
    }
}
