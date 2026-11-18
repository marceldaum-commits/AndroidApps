package de.wissensgarten.app.ui.fx

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Gartendeko, komplett gezeichnet. Jede Deko steht mit ihrer Unterkante auf [ground] und ist etwa [h] hoch.
 * Neue Deko: hier zeichnen, in ShopItems eintragen, Platz in [DecoSlots] vergeben.
 */
object DecoSlots {
    /** Vordergrund-Plätze (x-Anteil der Breite) – zwischen den Pflanzen im Panorama. */
    val front = mapOf(
        "zwerg" to 0.143f, "windrad" to 0.286f, "vogelhaus" to 0.429f,
        "bank" to 0.571f, "laterne" to 0.714f, "bienen" to 0.857f, "teich" to 0.93f,
    )
    /** Hintergrund (hinter den Pflanzen): ganze Breite. */
    val back = setOf("zaun", "regenbogen")
}

fun DrawScope.drawDeco(id: String, x: Float, ground: Float, h: Float, t: Float, night: Boolean) {
    when (id) {
        "zwerg" -> gnome(x, ground, h)
        "windrad" -> pinwheel(x, ground, h, t)
        "vogelhaus" -> birdhouse(x, ground, h, t)
        "bank" -> bench(x, ground, h)
        "laterne" -> lantern(x, ground, h, night, t)
        "bienen" -> beehive(x, ground, h, t)
        "teich" -> pond(x, ground, h, t)
        "zaun" -> fence(ground, h)
        "regenbogen" -> rainbow(h)
    }
}

private fun DrawScope.gnome(x: Float, g: Float, h: Float) {
    val u = h / 10f
    drawOval(Color(0x33000000), Offset(x - u * 2.2f, g - u * 0.6f), Size(u * 4.4f, u * 1.2f))
    drawRoundRect(Color(0xFF1E88E5), Offset(x - u * 1.8f, g - u * 4.2f), Size(u * 3.6f, u * 4f), CornerRadius(u * 1.4f))   // Körper
    drawRect(Color(0xFF5D4037), Offset(x - u * 1.8f, g - u * 2.2f), Size(u * 3.6f, u * 0.5f))                                // Gürtel
    drawCircle(Color(0xFFFFCCBC), u * 1.3f, Offset(x, g - u * 5.3f))                                                           // Gesicht
    val beard = Path().apply { moveTo(x - u * 1.3f, g - u * 5.0f); quadraticTo(x, g - u * 2.0f, x + u * 1.3f, g - u * 5.0f); close() }
    drawPath(beard, Color.White)
    drawCircle(Color(0xFFE57373), u * 0.35f, Offset(x, g - u * 5.1f))                                                          // Nase
    val hat = Path().apply { moveTo(x - u * 1.6f, g - u * 5.8f); lineTo(x + u * 1.6f, g - u * 5.8f); lineTo(x + u * 0.4f, g - u * 10f); close() }
    drawPath(hat, Color(0xFFE53935))
}

private fun DrawScope.pinwheel(x: Float, g: Float, h: Float, t: Float) {
    val u = h / 10f
    val hub = Offset(x, g - u * 7.5f)
    drawLine(Color(0xFF8D6E63), Offset(x, g), hub, u * 0.5f, StrokeCap.Round)
    val cols = listOf(Color(0xFFEF5350), Color(0xFFFFCA28), Color(0xFF42A5F5), Color(0xFF66BB6A))
    rotate(t * 160f, hub) {
        cols.forEachIndexed { i, c ->
            rotate(i * 90f, hub) {
                val p = Path().apply { moveTo(hub.x, hub.y); lineTo(hub.x + u * 2.6f, hub.y - u * 0.4f); lineTo(hub.x + u * 1.2f, hub.y - u * 2.2f); close() }
                drawPath(p, c)
            }
        }
    }
    drawCircle(Color.White, u * 0.45f, hub)
}

private fun DrawScope.birdhouse(x: Float, g: Float, h: Float, t: Float) {
    val u = h / 10f
    drawLine(Color(0xFF6D4C41), Offset(x, g), Offset(x, g - u * 6f), u * 0.6f)
    drawRect(Color(0xFFFFB74D), Offset(x - u * 2f, g - u * 9f), Size(u * 4f, u * 3.2f))
    val roof = Path().apply { moveTo(x - u * 2.6f, g - u * 8.8f); lineTo(x + u * 2.6f, g - u * 8.8f); lineTo(x, g - u * 11f); close() }
    drawPath(roof, Color(0xFFD84315))
    drawCircle(Color(0xFF3E2723), u * 0.7f, Offset(x, g - u * 7.6f))
    // Meise schaut ab und zu raus
    if (sin(t * 0.7f) > 0.3f) {
        drawCircle(Color(0xFFFDD835), u * 0.55f, Offset(x, g - u * 7.6f))
        drawCircle(Color.Black, u * 0.12f, Offset(x + u * 0.2f, g - u * 7.8f))
    }
}

private fun DrawScope.bench(x: Float, g: Float, h: Float) {
    val u = h / 10f
    val wood = Color(0xFF8D6E63); val dark = Color(0xFF5D4037)
    drawRect(dark, Offset(x - u * 3.5f, g - u * 3f), Size(u * 0.6f, u * 3f))
    drawRect(dark, Offset(x + u * 2.9f, g - u * 3f), Size(u * 0.6f, u * 3f))
    drawRoundRect(wood, Offset(x - u * 4f, g - u * 3.4f), Size(u * 8f, u * 0.9f), CornerRadius(u * 0.3f))
    drawRoundRect(wood, Offset(x - u * 4f, g - u * 5.6f), Size(u * 8f, u * 0.8f), CornerRadius(u * 0.3f))
    drawRoundRect(wood, Offset(x - u * 4f, g - u * 4.5f), Size(u * 8f, u * 0.7f), CornerRadius(u * 0.3f))
}

private fun DrawScope.lantern(x: Float, g: Float, h: Float, night: Boolean, t: Float) {
    val u = h / 10f
    val lamp = Offset(x, g - u * 9f)
    if (night) {
        val flick = 0.85f + 0.15f * sin(t * 9f)
        drawCircle(de.wissensgarten.app.ui.fx.SafeBrush.radialGradient(listOf(Color(0x99FFE082), Color(0x00FFE082)), lamp, u * 6f * flick), u * 6f * flick, lamp)
    }
    drawLine(Color(0xFF37474F), Offset(x, g), Offset(x, g - u * 8f), u * 0.6f)
    drawRoundRect(Color(0xFF37474F), Offset(x - u * 1.2f, g - u * 10.6f), Size(u * 2.4f, u * 3f), CornerRadius(u * 0.4f))
    drawRoundRect(if (night) Color(0xFFFFE082) else Color(0xFFFFF8E1), Offset(x - u * 0.8f, g - u * 10.2f), Size(u * 1.6f, u * 2.2f), CornerRadius(u * 0.3f))
}

private fun DrawScope.beehive(x: Float, g: Float, h: Float, t: Float) {
    val u = h / 10f
    drawLine(Color(0xFF6D4C41), Offset(x, g), Offset(x, g - u * 3f), u * 0.6f)
    listOf(3.2f, 2.6f, 2.0f, 1.3f).forEachIndexed { i, wf ->
        drawOval(if (i % 2 == 0) Color(0xFFFFCA28) else Color(0xFFFFB300), Offset(x - u * wf, g - u * (4.2f + i * 1.3f)), Size(u * wf * 2, u * 1.6f))
    }
    drawCircle(Color(0xFF4E342E), u * 0.5f, Offset(x, g - u * 4.2f))
    // Bienen
    for (i in 0 until 3) {
        val a = t * (2.2f + i * 0.5f) + i * 2.1f
        val p = Offset(x + cos(a) * u * (4f + i), g - u * 7f + sin(a * 1.7f) * u * 2f)
        drawOval(Color(0xFFFFEB3B), Offset(p.x - u * 0.5f, p.y - u * 0.35f), Size(u, u * 0.7f))
        drawLine(Color.Black, Offset(p.x, p.y - u * 0.35f), Offset(p.x, p.y + u * 0.35f), u * 0.18f)
        drawOval(Color.White.copy(alpha = 0.8f), Offset(p.x - u * 0.3f, p.y - u * 0.9f), Size(u * 0.5f, u * 0.6f))
    }
}

private fun DrawScope.pond(x: Float, g: Float, h: Float, t: Float) {
    val u = h / 10f
    val w = u * 11f
    drawOval(Color(0xFF6D4C41).copy(alpha = 0.6f), Offset(x - w / 2 - u * 0.4f, g - u * 3.2f), Size(w + u * 0.8f, u * 3.4f))
    drawOval(Color(0xFF4FC3F7), Offset(x - w / 2, g - u * 3f), Size(w, u * 3f))
    drawOval(Color(0xFF81D4FA), Offset(x - w * 0.35f, g - u * 2.6f), Size(w * 0.4f, u * 0.8f))
    // Seerose
    drawCircle(Color(0xFF66BB6A), u * 1f, Offset(x - u * 2f, g - u * 1.6f))
    drawCircle(Color(0xFFF8BBD0), u * 0.45f, Offset(x - u * 2f, g - u * 1.8f))
    // Frosch hüpft manchmal
    val hop = (sin(t * 1.1f).coerceAtLeast(0f)) * u * 1.5f
    val f = Offset(x + u * 1.5f, g - u * 2.2f - hop)
    drawOval(Color(0xFF43A047), Offset(f.x - u * 0.9f, f.y - u * 0.6f), Size(u * 1.8f, u * 1.2f))
    drawCircle(Color.White, u * 0.3f, Offset(f.x - u * 0.45f, f.y - u * 0.6f))
    drawCircle(Color.White, u * 0.3f, Offset(f.x + u * 0.45f, f.y - u * 0.6f))
    drawCircle(Color.Black, u * 0.13f, Offset(f.x - u * 0.45f, f.y - u * 0.6f))
    drawCircle(Color.Black, u * 0.13f, Offset(f.x + u * 0.45f, f.y - u * 0.6f))
}

private fun DrawScope.fence(g: Float, h: Float) {
    val u = h / 10f
    val white = Color(0xFFFAFAFA); val shade = Color(0xFFE0E0E0)
    val top = g - u * 6f
    drawRect(shade, Offset(0f, top + u * 1.5f), Size(size.width, u * 0.7f))
    drawRect(shade, Offset(0f, top + u * 4f), Size(size.width, u * 0.7f))
    var x = u
    while (x < size.width) {
        val p = Path().apply {
            moveTo(x, g); lineTo(x, top + u); lineTo(x + u * 0.7f, top); lineTo(x + u * 1.4f, top + u); lineTo(x + u * 1.4f, g); close()
        }
        drawPath(p, white)
        x += u * 3f
    }
}

private fun DrawScope.rainbow(h: Float) {
    val w = size.width
    val cols = listOf(Color(0xFFEF5350), Color(0xFFFFA726), Color(0xFFFFEE58), Color(0xFF66BB6A), Color(0xFF42A5F5), Color(0xFF7E57C2))
    val c = Offset(w * 0.72f, size.height * 0.62f)
    val r0 = size.height * 0.4f
    val band = size.height * 0.03f
    cols.forEachIndexed { i, col ->
        val r = r0 - i * band
        drawArc(col.copy(alpha = 0.55f), 180f, 180f, false, Offset(c.x - r, c.y - r), Size(r * 2, r * 2), style = Stroke(band))
    }
}

/** Vorschau einer Deko oder Pflanze im Laden. */
fun DrawScope.drawDecoPreview(id: String, t: Float) {
    val g = size.height * 0.9f
    val h = size.height * 0.7f
    when (id) {
        "zaun" -> { drawRect(Color(0xFF9CCC65), Offset(0f, g), Size(size.width, size.height - g)); fence(g, h * 0.8f) }
        "regenbogen" -> rainbow(h)
        "teich" -> pond(size.width / 2, g, h * 0.75f, t)
        else -> drawDeco(id, size.width / 2, g, h, t, night = id == "laterne")
    }
}

