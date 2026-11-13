package de.wissensgarten.app.ui.common

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
import de.wissensgarten.core.model.Stage
import de.wissensgarten.app.ui.fx.LocalWind
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
fun Plant(
    stage: Stage, wilt: Float, hue: Float, variant: Int, modifier: Modifier = Modifier, grow: Float = 1f,
    species: String = "klassik",
) {
    val wind = LocalWind.current
    Canvas(modifier) {
        // Wind nur in der Zeichenphase lesen – kein Neuaufbau der Oberfläche pro Bild
        val t = wind?.value ?: 0f
        val strength = (if (stage == Stage.TREE) 1.2f else 3.5f) * (1f - wilt * 0.6f)
        val sway = (sin(t * 1.6f + variant * 0.7f) + 0.4f * sin(t * 3.1f + variant)) * strength
        drawPlant(stage, wilt, hue, variant, grow, sway, species, t)
    }
}

fun DrawScope.drawPlant(
    stage: Stage, wilt: Float, hue: Float, variant: Int, grow: Float = 1f, sway: Float = 0f,
    species: String = "klassik", time: Float = 0f,
) {
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

    rotate(sway, pivot = base) {
    if (species == "kaktus") { drawCactus(stage, base, wilt, hue, grow, variant); return }
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
        when (species) {
            "tanne" -> drawPine(base, stemH, leaf, leafDark, wilt)
            "kirschbaum" -> drawCherryTree(base, stemH, leaf, wilt, variant)
            "zauberblume" -> drawMagicTree(base, stemH, wilt, time, variant)
            "rose" -> drawRoseBush(base, stemH, leaf, leafDark, hue, wilt, variant)
            "tulpe" -> drawTulipCluster(base, stemH, leafDark, hue, wilt, variant)
            "sonnenblume" -> drawGiantSunflower(base, stemH, leaf, leafDark, wilt, variant)
            else -> drawTree(base, stemH, leaf, leafDark, petal, wilt, variant)
        }
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
        val r = w * 0.13f * (0.5f + 0.5f * grow)
        when (species) {
            "sonnenblume" -> drawSunflower(tip, r * 1.3f, wilt, variant)
            "tulpe" -> drawTulip(tip, r, hue, wilt)
            "rose" -> drawRose(tip, r, hue, wilt)
            "tanne" -> drawCones(tip, r, wilt)
            "kirschbaum" -> { drawBlossom(tip, r * 0.8f, wilt); drawBlossom(quad(base, ctrl, tip, 0.65f), r * 0.55f, wilt) }
            "zauberblume" -> drawMagicFlower(tip, r, wilt, time, variant)
            else -> drawFlower(tip, r, petal, center, variant, wilt)
        }
    }
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


// ---------- Pflanzenarten aus dem Gartenladen ----------

private fun dry(c: Color, wilt: Float) = lerp(c, Color(0xFFB59A52), wilt * 0.8f)

private fun DrawScope.drawSunflower(c: Offset, r: Float, wilt: Float, variant: Int) {
    val petal = dry(Color(0xFFFFC107), wilt)
    val petalDark = dry(Color(0xFFFFA000), wilt)
    rotate(variant * 7f + wilt * 35f, c) {
        for (i in 0 until 14) {
            val a = 2 * PI * i / 14
            val p = Offset(c.x + cos(a).toFloat() * r * 0.8f, c.y + sin(a).toFloat() * r * 0.8f)
            drawCircle(if (i % 2 == 0) petal else petalDark, r * 0.32f, p)
        }
    }
    drawCircle(dry(Color(0xFF5D4037), wilt * 0.5f), r * 0.55f, c)
    for (i in 0 until 7) {
        val a = 2 * PI * i / 7
        drawCircle(Color(0xFF3E2723), r * 0.06f, Offset(c.x + cos(a).toFloat() * r * 0.3f, c.y + sin(a).toFloat() * r * 0.3f))
    }
}

private fun DrawScope.drawTulip(c: Offset, r: Float, hue: Float, wilt: Float) {
    val col = dry(Color.hsv(hue, 0.75f, 0.92f), wilt)
    val dark = dry(Color.hsv(hue, 0.85f, 0.72f), wilt)
    val cup = Path().apply {
        moveTo(c.x - r * 0.75f, c.y - r * 0.9f)
        quadraticTo(c.x - r * 0.9f, c.y + r * 0.6f, c.x, c.y + r * 0.7f)
        quadraticTo(c.x + r * 0.9f, c.y + r * 0.6f, c.x + r * 0.75f, c.y - r * 0.9f)
        lineTo(c.x + r * 0.35f, c.y - r * 0.4f)
        lineTo(c.x, c.y - r * 1.0f)
        lineTo(c.x - r * 0.35f, c.y - r * 0.4f)
        close()
    }
    drawPath(cup, col)
    val mid = Path().apply {
        moveTo(c.x - r * 0.3f, c.y - r * 0.7f); quadraticTo(c.x, c.y + r * 0.6f, c.x + r * 0.3f, c.y - r * 0.7f)
        lineTo(c.x, c.y - r * 1.1f); close()
    }
    drawPath(mid, dark)
}

private fun DrawScope.drawRose(c: Offset, r: Float, hue: Float, wilt: Float) {
    val h = if (hue in 60f..300f) 350f else hue
    val outer = dry(Color.hsv(h, 0.8f, 0.85f), wilt)
    val inner = dry(Color.hsv(h, 0.9f, 0.6f), wilt)
    for (i in 0 until 5) {
        val a = 2 * PI * i / 5 + 0.3
        drawCircle(outer, r * 0.5f, Offset(c.x + cos(a).toFloat() * r * 0.45f, c.y + sin(a).toFloat() * r * 0.45f))
    }
    drawCircle(inner, r * 0.5f, c)
    drawArc(outer, 200f, 260f, false, Offset(c.x - r * 0.3f, c.y - r * 0.3f), Size(r * 0.6f, r * 0.6f), style = Stroke(r * 0.12f))
    drawCircle(inner, r * 0.12f, c)
}

private fun DrawScope.drawCones(c: Offset, r: Float, wilt: Float) {
    val cone = dry(Color(0xFF8D6E63), wilt * 0.4f)
    listOf(-0.5f to 0.2f, 0.5f to 0.3f, 0f to -0.3f).forEach { (dx, dy) ->
        drawOval(cone, Offset(c.x + dx * r - r * 0.25f, c.y + dy * r - r * 0.4f), Size(r * 0.5f, r * 0.8f))
        drawLine(Color(0xFF5D4037), Offset(c.x + dx * r - r * 0.2f, c.y + dy * r), Offset(c.x + dx * r + r * 0.2f, c.y + dy * r), r * 0.06f)
    }
}

private fun DrawScope.drawBlossom(c: Offset, r: Float, wilt: Float) {
    val petal = dry(Color(0xFFF8BBD0), wilt)
    for (i in 0 until 5) {
        val a = 2 * PI * i / 5
        drawCircle(petal, r * 0.42f, Offset(c.x + cos(a).toFloat() * r * 0.5f, c.y + sin(a).toFloat() * r * 0.5f))
    }
    drawCircle(Color(0xFFE91E63).copy(alpha = 0.8f), r * 0.2f, c)
}

private fun DrawScope.drawMagicFlower(c: Offset, r: Float, wilt: Float, t: Float, variant: Int) {
    val pulse = 0.75f + 0.25f * sin(t * 2.5f + variant)
    val glow = dry(Color(0xFFB388FF), wilt)
    drawCircle(glow.copy(alpha = 0.18f * pulse), r * 2.2f, c)
    drawCircle(glow.copy(alpha = 0.3f * pulse), r * 1.5f, c)
    for (i in 0 until 6) {
        val a = 2 * PI * i / 6 + t * 0.3
        val p = Offset(c.x + cos(a).toFloat() * r * 0.7f, c.y + sin(a).toFloat() * r * 0.7f)
        drawCircle(if (i % 2 == 0) dry(Color(0xFF7C4DFF), wilt) else dry(Color(0xFF18FFFF), wilt), r * 0.42f, p)
    }
    drawCircle(Color.White, r * 0.35f * pulse, c)
    // Funken
    for (i in 0 until 4) {
        val a = t * 1.3f + i * 1.57f
        val p = Offset(c.x + cos(a) * r * 1.6f, c.y + sin(a) * r * 1.2f)
        drawCircle(Color.White.copy(alpha = 0.5f + 0.5f * sin(t * 4 + i)), r * 0.09f, p)
    }
}

private fun DrawScope.drawPine(base: Offset, h: Float, leaf: Color, leafDark: Color, wilt: Float) {
    val w = size.width
    drawLine(Bark, base, Offset(base.x, base.y - h * 0.3f), w * 0.07f, StrokeCap.Round)
    val green = lerp(Color(0xFF2E7D32), Color(0xFF8D8A4A), wilt * 0.7f)
    val light = lerp(Color(0xFF43A047), Color(0xFFA19B5A), wilt * 0.7f)
    for (i in 0 until 3) {
        val y0 = base.y - h * (0.2f + i * 0.25f)
        val half = w * (0.3f - i * 0.07f)
        val tri = Path().apply {
            moveTo(base.x - half, y0); lineTo(base.x + half, y0); lineTo(base.x, y0 - h * 0.42f); close()
        }
        drawPath(tri, if (i % 2 == 0) green else light)
    }
    drawStarShape(Offset(base.x, base.y - h * 1.05f), w * 0.05f, Color(0xFFFFD54F).copy(alpha = 1f - wilt))
}

private fun DrawScope.drawStarShape(c: Offset, r: Float, color: Color) {
    val p = Path()
    for (i in 0 until 10) {
        val a = -PI / 2 + PI * i / 5
        val rr = if (i % 2 == 0) r else r * 0.45f
        val x = c.x + cos(a).toFloat() * rr; val y = c.y + sin(a).toFloat() * rr
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close(); drawPath(p, color)
}

private fun DrawScope.drawCherryTree(base: Offset, h: Float, leaf: Color, wilt: Float, variant: Int) {
    val w = size.width
    val top = Offset(base.x, base.y - h * 0.55f)
    drawLine(Bark, base, top, w * 0.08f, StrokeCap.Round)
    drawLine(Bark, Offset(base.x, base.y - h * 0.4f), Offset(base.x - w * 0.14f, base.y - h * 0.62f), w * 0.04f, StrokeCap.Round)
    val crown = Offset(base.x, base.y - h * 0.78f)
    val r = w * 0.25f * (1f - wilt * 0.2f)
    val pink = dry(Color(0xFFF48FB1), wilt)
    val pinkLight = dry(Color(0xFFFCE4EC), wilt)
    drawCircle(pink, r, Offset(crown.x - r * 0.5f, crown.y + r * 0.2f))
    drawCircle(pink, r * 0.9f, Offset(crown.x + r * 0.55f, crown.y + r * 0.25f))
    drawCircle(pinkLight, r * 0.95f, Offset(crown.x, crown.y - r * 0.2f))
    val rnd = kotlin.random.Random(variant)
    repeat(9) {
        val p = Offset(crown.x + (rnd.nextFloat() - 0.5f) * r * 2.2f, crown.y + (rnd.nextFloat() - 0.5f) * r * 1.6f)
        drawCircle(dry(Color(0xFFEC407A), wilt), r * 0.08f, p)
    }
}

private fun DrawScope.drawMagicTree(base: Offset, h: Float, wilt: Float, t: Float, variant: Int) {
    val w = size.width
    drawLine(Color(0xFF5E35B1), base, Offset(base.x, base.y - h * 0.55f), w * 0.07f, StrokeCap.Round)
    val crown = Offset(base.x, base.y - h * 0.78f)
    val r = w * 0.26f
    val pulse = 0.8f + 0.2f * sin(t * 2f + variant)
    drawCircle(dry(Color(0xFFB388FF), wilt).copy(alpha = 0.2f * pulse), r * 1.6f, crown)
    val cols = listOf(Color(0xFF7C4DFF), Color(0xFF18FFFF), Color(0xFFFF4081), Color(0xFFB2FF59))
    for (i in 0 until 7) {
        val a = 2 * PI * i / 7 + t * 0.2
        val p = Offset(crown.x + cos(a).toFloat() * r * 0.6f, crown.y + sin(a).toFloat() * r * 0.5f)
        drawCircle(dry(cols[i % cols.size], wilt).copy(alpha = 0.85f), r * 0.32f, p)
    }
    drawCircle(Color.White.copy(alpha = 0.9f), r * 0.25f * pulse, crown)
}

private fun DrawScope.drawRoseBush(base: Offset, h: Float, leaf: Color, leafDark: Color, hue: Float, wilt: Float, variant: Int) {
    val w = size.width
    val c = Offset(base.x, base.y - h * 0.4f)
    val r = w * 0.3f
    drawCircle(leafDark, r, Offset(c.x - r * 0.35f, c.y + r * 0.2f))
    drawCircle(leafDark, r * 0.9f, Offset(c.x + r * 0.4f, c.y + r * 0.25f))
    drawCircle(leaf, r * 0.85f, Offset(c.x, c.y - r * 0.15f))
    listOf(-0.5f to -0.1f, 0.4f to -0.35f, 0.1f to 0.35f, -0.1f to -0.6f, 0.6f to 0.2f).forEach { (dx, dy) ->
        drawRose(Offset(c.x + dx * r, c.y + dy * r), r * 0.28f, hue, wilt)
    }
}

private fun DrawScope.drawTulipCluster(base: Offset, h: Float, leafDark: Color, hue: Float, wilt: Float, variant: Int) {
    val w = size.width
    listOf(-0.18f to 0.75f, 0f to 1f, 0.18f to 0.8f).forEachIndexed { i, (dx, hf) ->
        val tip = Offset(base.x + dx * w, base.y - h * hf * 0.9f)
        drawLine(leafDark, base, tip, w * 0.035f, StrokeCap.Round)
        drawTulip(tip, w * 0.11f, (hue + i * 40f) % 360f, wilt)
    }
    drawLeaf(Offset(base.x, base.y - h * 0.1f), w * 0.25f, -40f, leafDark)
    drawLeaf(Offset(base.x, base.y - h * 0.1f), w * 0.25f, 40f, leafDark)
}

private fun DrawScope.drawGiantSunflower(base: Offset, h: Float, leaf: Color, leafDark: Color, wilt: Float, variant: Int) {
    val w = size.width
    val tip = Offset(base.x, base.y - h * 0.95f)
    drawLine(leafDark, base, tip, w * 0.06f, StrokeCap.Round)
    for (i in 0 until 4) {
        val p = Offset(base.x, base.y - h * (0.2f + i * 0.18f))
        drawLeaf(p, w * 0.22f, if (i % 2 == 0) -60f else 60f, if (i % 2 == 0) leaf else leafDark)
    }
    drawSunflower(Offset(base.x + w * 0.16f, base.y - h * 0.55f), w * 0.09f, wilt, variant + 3)
    drawSunflower(tip, w * 0.2f, wilt, variant)
}

private fun DrawScope.drawCactus(stage: Stage, base: Offset, wilt: Float, hue: Float, grow: Float, variant: Int) {
    val w = size.width; val h = size.height
    val hf = when (stage) {
        Stage.SPROUT -> 0.16f; Stage.SHOOT -> 0.3f; Stage.BUSH -> 0.42f; Stage.BLOOM -> 0.52f; else -> 0.62f
    } * (0.4f + 0.6f * grow)
    val green = lerp(Color(0xFF43A047), Color(0xFFC0A85A), wilt * 0.8f)
    val dark = lerp(Color(0xFF2E7D32), Color(0xFF9E8A45), wilt * 0.8f)
    val bw = w * (if (stage >= Stage.BLOOM) 0.2f else 0.16f)
    val top = base.y - h * hf
    fun column(x: Float, yTop: Float, yBot: Float, width: Float) {
        drawRoundRect(green, Offset(x - width / 2, yTop), Size(width, yBot - yTop), androidx.compose.ui.geometry.CornerRadius(width / 2))
        drawLine(dark, Offset(x, yTop + width * 0.4f), Offset(x, yBot - 2f), width * 0.08f)
        // Stacheln
        var y = yTop + width * 0.5f
        while (y < yBot - width * 0.3f) {
            drawLine(Color(0xFFFFF9C4), Offset(x - width / 2, y), Offset(x - width / 2 - width * 0.12f, y - width * 0.06f), 1.5f)
            drawLine(Color(0xFFFFF9C4), Offset(x + width / 2, y), Offset(x + width / 2 + width * 0.12f, y - width * 0.06f), 1.5f)
            y += width * 0.45f
        }
    }
    column(base.x, top, base.y, bw)
    if (stage >= Stage.BUSH) {
        // Arme
        val armW = bw * 0.6f
        val ay = base.y - h * hf * 0.45f
        drawRoundRect(green, Offset(base.x - bw * 1.2f, ay - armW / 2), Size(bw * 0.8f, armW), androidx.compose.ui.geometry.CornerRadius(armW / 2))
        column(base.x - bw * 1.05f, ay - h * hf * 0.3f, ay + armW / 2, armW)
        if (stage >= Stage.BLOOM) {
            val by = base.y - h * hf * 0.62f
            drawRoundRect(green, Offset(base.x + bw * 0.4f, by - armW / 2), Size(bw * 0.8f, armW), androidx.compose.ui.geometry.CornerRadius(armW / 2))
            column(base.x + bw * 1.05f, by - h * hf * 0.22f, by + armW / 2, armW)
        }
    }
    if (stage >= Stage.BLOOM) {
        val flower = dry(Color.hsv(if (hue in 60f..300f) 330f else hue, 0.6f, 0.95f), wilt)
        for (i in 0 until 6) {
            val a = 2 * PI * i / 6
            drawCircle(flower, bw * 0.22f, Offset(base.x + cos(a).toFloat() * bw * 0.25f, top + sin(a).toFloat() * bw * 0.18f))
        }
        drawCircle(Color(0xFFFFEB3B), bw * 0.14f, Offset(base.x, top))
    }
}
