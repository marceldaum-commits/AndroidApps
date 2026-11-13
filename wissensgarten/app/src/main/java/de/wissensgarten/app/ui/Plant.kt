package de.wissensgarten.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.StrokeCap
import de.wissensgarten.app.data.Stage
import de.wissensgarten.app.ui.theme.Soil
import de.wissensgarten.app.ui.theme.SoilDark
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val Fresh = Color(0xFF5E9E48)
private val FreshDark = Color(0xFF3C7330)
private val Dry = Color(0xFFB59A52)
private val Bark = Color(0xFF6B4A2B)

/**
 * Zeichnet eine Pflanze rein aus Formen – kein Bildmaterial nötig.
 * @param wilt 0 = frisch, 1 = welk (Stängel hängt, Farbe wird strohig)
 * @param hue Blütenfarbe des Beets
 * @param variant Zufallswert pro Karte, damit nicht alle Pflanzen gleich aussehen
 * @param grow 0…1 Animationsfortschritt beim Wachsen
 */
@Composable
fun Plant(stage: Stage, wilt: Float, hue: Float, variant: Int, modifier: Modifier = Modifier, grow: Float = 1f) {
    Canvas(modifier) { drawPlant(stage, wilt, hue, variant, grow) }
}

fun DrawScope.drawPlant(stage: Stage, wilt: Float, hue: Float, variant: Int, grow: Float = 1f) {
    val w = size.width
    val h = size.height
    val base = Offset(w / 2, h * 0.86f)
    val leaf = lerp(Fresh, Dry, wilt)
    val leafDark = lerp(FreshDark, Dry.copy(red = 0.55f), wilt)
    val petal = lerp(Color.hsv(hue, 0.55f, 0.95f), Color.hsv(hue, 0.2f, 0.65f), wilt)
    val center = lerp(Color(0xFFFFD54F), Dry, wilt)

    // Erdhügel
    drawOval(SoilDark, topLeft = Offset(w * 0.18f, h * 0.80f), size = Size(w * 0.64f, h * 0.14f))
    drawOval(Soil, topLeft = Offset(w * 0.2f, h * 0.79f), size = Size(w * 0.6f, h * 0.11f))

    if (stage == Stage.SEED) {
        drawOval(Color(0xFF8C6239), topLeft = Offset(w * 0.45f, h * 0.76f), size = Size(w * 0.1f, h * 0.07f))
        drawOval(Color(0xFFC79A6B), topLeft = Offset(w * 0.47f, h * 0.77f), size = Size(w * 0.03f, h * 0.02f))
        return
    }

    val heightFrac = when (stage) {
        Stage.SPROUT -> 0.22f
        Stage.SHOOT -> 0.40f
        Stage.BUSH -> 0.52f
        Stage.BLOOM -> 0.62f
        Stage.TREE -> 0.70f
        else -> 0f
    } * (0.4f + 0.6f * grow)
    val sway = ((variant % 7) - 3) * 0.012f
    val droop = wilt * 0.55f
    val stemH = h * heightFrac
    // Spitze: hängt bei Welke zur Seite
    val tip = Offset(base.x + w * (sway + droop * 0.45f), base.y - stemH * (1f - droop * 0.45f))
    val ctrl = Offset(base.x + w * sway * 2, base.y - stemH * 0.7f)

    if (stage == Stage.TREE) {
        drawTree(base, stemH, leaf, leafDark, petal, wilt, variant)
        return
    }

    val stem = Path().apply { moveTo(base.x, base.y); quadraticTo(ctrl.x, ctrl.y, tip.x, tip.y) }
    drawPath(stem, leafDark, style = Stroke(width = w * 0.045f, cap = StrokeCap.Round))

    val leafCount = when (stage) {
        Stage.SPROUT -> 2; Stage.SHOOT -> 4; Stage.BUSH -> 6; else -> 6
    }
    for (i in 0 until leafCount) {
        val t = 0.25f + 0.7f * (i / leafCount.toFloat())
        val p = quad(base, ctrl, tip, t)
        val side = if (i % 2 == 0) -1f else 1f
        val angle = side * (55f + wilt * 35f) + (if (stage == Stage.SPROUT) side * 10f else 0f)
        val len = w * (0.20f - 0.08f * t) * (0.7f + 0.3f * grow)
        drawLeaf(p, len, angle, if (i % 3 == 0) leafDark else leaf)
    }

    if (stage == Stage.BUSH) {
        // kleine Knospe
        drawCircle(lerp(leafDark, petal, 0.5f), radius = w * 0.045f, center = tip)
    }
    if (stage == Stage.BLOOM) {
        drawFlower(tip, w * 0.13f * (0.5f + 0.5f * grow), petal, center, variant, wilt)
    }
}

private fun quad(a: Offset, c: Offset, b: Offset, t: Float): Offset {
    val u = 1 - t
    return Offset(u * u * a.x + 2 * u * t * c.x + t * t * b.x, u * u * a.y + 2 * u * t * c.y + t * t * b.y)
}

private fun DrawScope.drawLeaf(at: Offset, len: Float, angleDeg: Float, color: Color) {
    rotate(angleDeg, pivot = at) {
        val p = Path().apply {
            moveTo(at.x, at.y)
            quadraticTo(at.x + len * 0.35f, at.y - len * 0.5f, at.x, at.y - len)
            quadraticTo(at.x - len * 0.35f, at.y - len * 0.5f, at.x, at.y)
            close()
        }
        drawPath(p, color)
    }
}

private fun DrawScope.drawFlower(c: Offset, r: Float, petal: Color, center: Color, variant: Int, wilt: Float) {
    val petals = 5 + variant % 3
    val tilt = wilt * 40f
    rotate(tilt + variant * 13f, pivot = c) {
        for (i in 0 until petals) {
            val a = 2 * PI * i / petals
            val pc = Offset(c.x + cos(a).toFloat() * r * 0.75f, c.y + sin(a).toFloat() * r * 0.75f)
            drawCircle(petal, radius = r * 0.55f * (1f - wilt * 0.35f), center = pc)
        }
        drawCircle(center, radius = r * 0.45f, center = c)
    }
}

private fun DrawScope.drawTree(base: Offset, h: Float, leaf: Color, leafDark: Color, fruit: Color, wilt: Float, variant: Int) {
    val w = size.width
    val top = Offset(base.x, base.y - h * 0.55f)
    drawLine(Bark, base, top, strokeWidth = w * 0.09f, cap = StrokeCap.Round)
    drawLine(Bark, Offset(base.x, base.y - h * 0.35f), Offset(base.x + w * 0.12f, base.y - h * 0.55f), strokeWidth = w * 0.04f, cap = StrokeCap.Round)
    val crown = Offset(base.x, base.y - h * 0.75f)
    val r = w * 0.26f * (1f - wilt * 0.2f)
    drawCircle(leafDark, r, Offset(crown.x - r * 0.45f, crown.y + r * 0.2f))
    drawCircle(leafDark, r * 0.9f, Offset(crown.x + r * 0.5f, crown.y + r * 0.25f))
    drawCircle(leaf, r, Offset(crown.x, crown.y - r * 0.15f))
    // Früchte
    for (i in 0 until 4) {
        val a = 2 * PI * (i + variant % 5 * 0.2) / 4
        val p = Offset(crown.x + cos(a).toFloat() * r * 0.55f, crown.y + sin(a).toFloat() * r * 0.45f)
        drawCircle(fruit, r * 0.13f, p)
    }
}
