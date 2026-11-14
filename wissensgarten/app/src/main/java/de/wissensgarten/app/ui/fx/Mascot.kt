package de.wissensgarten.app.ui.fx

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sin

/** Brummis Stimmung. */
enum class Mood { IDLE, HAPPY, CHEER, SAD, THINK, SLEEP }

private val Fur = Color(0xFFFFC93C)
private val FurDark = Color(0xFFF2A900)
private val Stripe = Color(0xFF3B2A20)
private val Wing = Color(0xCCE3F2FD)
private val Cheek = Color(0xFFFF8A80)

/**
 * Brummi, die Hummel – das Maskottchen. Fliegt leicht auf und ab, blinzelt,
 * schlägt mit den Flügeln und reagiert mit [mood]. Optional mit Sprechblase.
 */
@Composable
fun Brummi(
    mood: Mood,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    speech: String? = null,
    bubbleOnLeft: Boolean = false,
    onTap: (() -> Unit)? = null,
) {
    val t by rememberInfiniteTransition(label = "brummi").animateFloat(
        0f, 1000f, infiniteRepeatable(tween(1_000_000, easing = LinearEasing)), label = "t",
    )
    // Freudensprung bei jedem Stimmungswechsel zu HAPPY/CHEER
    val jump = remember { Animatable(0f) }
    LaunchedEffect(mood) {
        if (mood == Mood.HAPPY || mood == Mood.CHEER) {
            jump.snapTo(0f); jump.animateTo(1f, tween(220, easing = FastOutSlowInEasing)); jump.animateTo(0f, spring(dampingRatio = 0.35f))
        }
    }
    val tapMod = if (onTap != null) Modifier.clickable(MutableInteractionSource(), null) { onTap() } else Modifier
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (bubbleOnLeft) Bubble(speech, Modifier.weight(1f, fill = false))
        Canvas(Modifier.size(size).then(tapMod).graphicsLayer { translationY = -jump.value * size.toPx() * 0.25f }) {
            drawBrummi(mood, t)
        }
        if (!bubbleOnLeft) Bubble(speech, Modifier.weight(1f, fill = false))
    }
}

@Composable
private fun Bubble(text: String?, modifier: Modifier) {
    AnimatedContent(text, transitionSpec = {
        (scaleIn(spring(dampingRatio = 0.5f)) + fadeIn()) togetherWith fadeOut(tween(120))
    }, label = "bubble", modifier = modifier) { s ->
        if (s != null) Surface(
            color = Color.White, shape = MaterialTheme.shapes.large, shadowElevation = 3.dp,
            modifier = Modifier.padding(horizontal = 6.dp),
        ) {
            Text(
                s, color = Color(0xFF22261F), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Start, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

fun DrawScope.drawBrummi(mood: Mood, t: Float) {
    val w = size.width; val h = size.height
    val hover = when (mood) {
        Mood.SLEEP -> sin(t * 1.2f) * h * 0.02f
        Mood.SAD -> h * 0.04f + sin(t * 1.5f) * h * 0.015f
        Mood.CHEER -> sin(t * 9f) * h * 0.05f
        else -> sin(t * 2.4f) * h * 0.035f
    }
    val tilt = when (mood) { Mood.CHEER -> sin(t * 7f) * 12f; Mood.THINK -> -8f; Mood.SAD -> 6f; else -> sin(t * 1.3f) * 4f }
    val c = Offset(w * 0.5f, h * 0.55f + hover)
    val bw = w * 0.36f; val bh = h * 0.3f

    rotate(tilt, c) {
        // Flügel (schlagen schnell, außer beim Schlafen)
        val flap = if (mood == Mood.SLEEP) 0.3f else abs(sin(t * 38f))
        val wingH = bh * (0.6f + 0.6f * flap)
        drawOval(Wing, Offset(c.x - bw * 0.95f, c.y - bh - wingH * 0.75f), Size(bw * 0.85f, wingH))
        drawOval(Wing, Offset(c.x + bw * 0.1f, c.y - bh - wingH * 0.75f), Size(bw * 0.85f, wingH))
        drawOval(Color(0x553F6B3A), Offset(c.x - bw * 0.95f, c.y - bh - wingH * 0.75f), Size(bw * 0.85f, wingH), style = Stroke(w * 0.008f))
        drawOval(Color(0x553F6B3A), Offset(c.x + bw * 0.1f, c.y - bh - wingH * 0.75f), Size(bw * 0.85f, wingH), style = Stroke(w * 0.008f))

        // Fühler
        val ant = if (mood == Mood.SAD) 0.6f else 1f
        for (side in listOf(-1f, 1f)) {
            val base = Offset(c.x + side * bw * 0.35f, c.y - bh * 0.85f)
            val tip = Offset(c.x + side * bw * (0.55f + 0.05f * sin(t * 3f)), c.y - bh * (1.25f * ant + 0.3f))
            val p = Path().apply { moveTo(base.x, base.y); quadraticTo(base.x + side * bw * 0.05f, tip.y + bh * 0.2f, tip.x, tip.y) }
            drawPath(p, Stripe, style = Stroke(w * 0.022f, cap = StrokeCap.Round))
            drawCircle(Stripe, w * 0.035f, tip)
        }

        // Körper (pelzig: ein paar Kreise am Rand)
        drawOval(FurDark, Offset(c.x - bw - w * 0.01f, c.y - bh - w * 0.01f), Size(bw * 2 + w * 0.02f, bh * 2 + w * 0.02f))
        drawOval(Fur, Offset(c.x - bw, c.y - bh), Size(bw * 2, bh * 2))
        // Streifen unten
        for (i in 0 until 2) {
            val y = c.y + bh * (0.35f + i * 0.33f)
            val half = bw * (0.92f - i * 0.25f)
            drawLine(Stripe, Offset(c.x - half, y), Offset(c.x + half, y), w * 0.05f, StrokeCap.Round)
        }

        // Gesicht
        val eyeY = c.y - bh * 0.2f
        val eyeDx = bw * 0.38f
        val blink = (t * 0.35f % 1f) > 0.96f
        for (side in listOf(-1f, 1f)) {
            val e = Offset(c.x + side * eyeDx, eyeY)
            when {
                mood == Mood.SLEEP || (blink && mood != Mood.HAPPY && mood != Mood.CHEER) ->
                    drawArc(Stripe, 20f, 140f, false, Offset(e.x - w * 0.05f, e.y - w * 0.04f), Size(w * 0.1f, w * 0.07f), style = Stroke(w * 0.018f, cap = StrokeCap.Round))
                mood == Mood.HAPPY || mood == Mood.CHEER ->
                    drawArc(Stripe, 200f, 140f, false, Offset(e.x - w * 0.05f, e.y - w * 0.02f), Size(w * 0.1f, w * 0.08f), style = Stroke(w * 0.022f, cap = StrokeCap.Round))
                else -> {
                    drawCircle(Color.White, w * 0.06f, e)
                    val look = when (mood) { Mood.THINK -> Offset(w * 0.015f, -w * 0.025f); Mood.SAD -> Offset(0f, w * 0.015f); else -> Offset(sin(t * 0.7f) * w * 0.015f, 0f) }
                    drawCircle(Stripe, w * 0.034f, e + look)
                    drawCircle(Color.White, w * 0.011f, e + look + Offset(-w * 0.01f, -w * 0.012f))
                    if (mood == Mood.SAD) drawLine(Stripe, Offset(e.x - side * w * 0.06f, e.y - w * 0.085f), Offset(e.x + side * w * 0.03f, e.y - w * 0.065f), w * 0.014f, StrokeCap.Round)
                }
            }
            drawCircle(Cheek.copy(alpha = 0.6f), w * 0.035f, Offset(c.x + side * bw * 0.62f, eyeY + bh * 0.3f))
        }
        // Mund
        val m = Offset(c.x, eyeY + bh * 0.33f)
        when (mood) {
            Mood.HAPPY, Mood.CHEER -> {
                val mouth = Path().apply {
                    moveTo(m.x - w * 0.07f, m.y - w * 0.01f); quadraticTo(m.x, m.y + w * 0.1f, m.x + w * 0.07f, m.y - w * 0.01f); close()
                }
                drawPath(mouth, Stripe)
                drawCircle(Color(0xFFE57373), w * 0.025f, Offset(m.x, m.y + w * 0.035f))
            }
            Mood.SAD -> drawArc(Stripe, 200f, 140f, false, Offset(m.x - w * 0.05f, m.y), Size(w * 0.1f, w * 0.06f), style = Stroke(w * 0.016f, cap = StrokeCap.Round))
            Mood.THINK -> drawCircle(Stripe, w * 0.018f, Offset(m.x + w * 0.02f, m.y + w * 0.01f))
            Mood.SLEEP -> drawCircle(Stripe, w * 0.014f, m)
            Mood.IDLE -> drawArc(Stripe, 20f, 140f, false, Offset(m.x - w * 0.05f, m.y - w * 0.03f), Size(w * 0.1f, w * 0.06f), style = Stroke(w * 0.016f, cap = StrokeCap.Round))
        }
    }

    // Extras außerhalb der Drehung
    when (mood) {
        Mood.SLEEP -> for (i in 0 until 3) {
            val f = (t * 0.4f + i / 3f) % 1f
            val p = Offset(c.x + bw * (0.9f + f * 0.5f), c.y - bh * (1f + f * 1.6f))
            drawZ(p, w * (0.05f + 0.03f * i), Color(0xFF5C6BC0).copy(alpha = 1f - f))
        }
        Mood.CHEER -> for (i in 0 until 5) {
            val a = t * 3f + i * 1.256f
            val p = Offset(c.x + kotlin.math.cos(a) * bw * 1.4f, c.y + sin(a) * bh * 1.5f)
            drawCircle(Color(0xFFFFD54F), w * 0.022f, p)
        }
        Mood.THINK -> for (i in 0 until 3) drawCircle(Color.White.copy(alpha = 0.9f), w * (0.02f + i * 0.012f), Offset(c.x + bw * (0.9f + i * 0.22f), c.y - bh * (1.1f + i * 0.35f)))
        else -> Unit
    }
}

private fun DrawScope.drawZ(p: Offset, s: Float, color: Color) {
    val path = Path().apply { moveTo(p.x, p.y); lineTo(p.x + s, p.y); lineTo(p.x, p.y + s); lineTo(p.x + s, p.y + s) }
    drawPath(path, color, style = Stroke(s * 0.22f, cap = StrokeCap.Round))
}

/** Was Brummi sagt – kurze, freundliche Sätze. */
object BrummiSays {
    val praise = listOf("Super! 🌟", "Genau richtig!", "Wow, du bist gut!", "Bsss – stark!", "Volltreffer! 🎯", "Klasse gemacht!", "Weiter so!")
    val comfort = listOf("Macht nichts – jetzt weißt du's!", "Fast! Beim nächsten Mal klappt's.", "Kein Problem, das üben wir!", "Fehler sind Dünger fürs Wissen 🌱")
    val streak = listOf("🔥 Du bist on fire!", "Wahnsinn, was für eine Serie!", "Nicht zu stoppen! 🚀")
    val tips = listOf(
        "Wer jeden Tag ein bisschen gießt, hat bald einen Wald! 🌳",
        "Tipp: Schau dir das Merkbild genau an – Bilder bleiben im Kopf!",
        "Wusstest du? Hummeln fliegen sogar bei Regen. 🐝",
        "Fordere jemanden zum Duell heraus! ⚔️",
        "Im Laden gibt's neue Pflanzen für deine Tautropfen. 🛍️",
    )
    fun pick(list: List<String>, seed: Int) = list[Math.floorMod(seed, list.size)]
}

