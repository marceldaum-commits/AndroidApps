package de.raumlicht.app.ui.lux

import de.raumlicht.app.ui.fx.radialFade
import de.raumlicht.app.ui.fx.verticalFade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.raumlicht.app.ui.theme.Q
import kotlin.math.sin

enum class LuxMood { IDLE, HAPPY, SAD, WOW, THINK }

/** Lux, die Lichtdrohne – als gezeichnetes Gesicht für Sprechblasen (in der 3D-Bühne fliegt sie als Modell). */
@Composable
fun Lux(mood: LuxMood = LuxMood.IDLE, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    val inf = rememberInfiniteTransition(label = "lux")
    val t by inf.animateFloat(0f, 6.283f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "t")
    val blink by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(3700, easing = LinearEasing)), label = "b")
    val band = de.raumlicht.app.ui.level.stageColor(de.raumlicht.app.ui.stage.LocalLuxSkin.current)
    Canvas(modifier.size(size)) {
        val w = this.size.width; val bob = sin(t) * w * 0.03f + if (mood == LuxMood.HAPPY) sin(t * 4) * w * 0.03f else 0f
        val c = Offset(w / 2, w * 0.54f + bob)
        val eye = when (mood) { LuxMood.HAPPY -> Q.Green; LuxMood.SAD -> Q.Orange; LuxMood.WOW -> Q.Yellow; else -> Q.Cyan }
        // Schein
        drawCircle(radialFade(listOf(eye.copy(alpha = 0.35f), Color.Transparent), c, w * 0.6f), w * 0.6f, c)
        // Rotoren
        for (k in listOf(-1f, 1f)) {
            val rc = Offset(c.x + k * w * 0.36f, c.y - w * 0.16f)
            drawLine(Color(0xFF39405C), Offset(c.x + k * w * 0.2f, c.y - w * 0.05f), rc, w * 0.045f)
            drawOval(Color(0xFFCDD4E6), Offset(rc.x - w * 0.15f, rc.y - w * 0.035f), Size(w * 0.3f, w * 0.07f), style = Stroke(w * 0.03f))
            rotate(t * 57.3f * 6 * k, rc) { drawLine(Color.White.copy(alpha = 0.5f), Offset(rc.x - w * 0.12f, rc.y), Offset(rc.x + w * 0.12f, rc.y), w * 0.012f) }
        }
        // Körper
        drawOval(verticalFade(listOf(Color(0xFFF2F5FF), Color(0xFF9AA6C4)), c.y - w * 0.22f, c.y + w * 0.22f), Offset(c.x - w * 0.28f, c.y - w * 0.21f), Size(w * 0.56f, w * 0.42f))
        drawArc(band, 20f, 140f, false, Offset(c.x - w * 0.28f, c.y - w * 0.2f), Size(w * 0.56f, w * 0.42f), style = Stroke(w * 0.035f))
        // Visier
        drawRoundRect(Color(0xFF0A0E1E), Offset(c.x - w * 0.2f, c.y - w * 0.12f), Size(w * 0.4f, w * 0.2f), CornerRadius(w * 0.1f))
        // Augen
        val closed = blink > 0.96f
        for (k in listOf(-1f, 1f)) {
            val e = Offset(c.x + k * w * 0.09f, c.y - w * 0.02f)
            when {
                closed || mood == LuxMood.HAPPY -> drawArc(eye, 200f, 140f, false, Offset(e.x - w * 0.045f, e.y - w * 0.03f), Size(w * 0.09f, w * 0.09f), style = Stroke(w * 0.03f))
                mood == LuxMood.SAD -> drawArc(eye, 20f, 140f, false, Offset(e.x - w * 0.045f, e.y - w * 0.05f), Size(w * 0.09f, w * 0.09f), style = Stroke(w * 0.03f))
                mood == LuxMood.WOW -> drawCircle(eye, w * 0.05f, e)
                else -> drawOval(eye, Offset(e.x - w * 0.03f, e.y - w * 0.05f - if (mood == LuxMood.THINK) w * 0.015f else 0f), Size(w * 0.06f, w * 0.1f))
            }
        }
        // Lampe
        drawCircle(Q.Yellow, w * 0.04f, Offset(c.x, c.y + w * 0.23f))
    }
}
