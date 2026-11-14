package de.wissensgarten.app.ui.fx

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class DayTime { DAWN, DAY, EVENING, NIGHT;
    companion object {
        fun of(hour: Int) = when (hour) {
            in 5..7 -> DAWN; in 8..16 -> DAY; in 17..20 -> EVENING; else -> NIGHT
        }
        fun now(nowMs: Long): Pair<DayTime, Float> {
            val c = Calendar.getInstance().apply { timeInMillis = nowMs }
            val h = c.get(Calendar.HOUR_OF_DAY) + c.get(Calendar.MINUTE) / 60f
            return of(h.toInt()) to h
        }
    }
}

private fun skyColors(t: DayTime) = when (t) {
    DayTime.DAWN -> Color(0xFF8EC5EA) to Color(0xFFFFCF9E)
    DayTime.DAY -> Color(0xFF5DADE2) to Color(0xFFCDEBFA)
    DayTime.EVENING -> Color(0xFF7466C9) to Color(0xFFFFA285)
    DayTime.NIGHT -> Color(0xFF101C40) to Color(0xFF34477A)
}

/**
 * Lebendiger Himmel über dem Garten: Tageszeit, Sonne/Mond, ziehende Wolken,
 * Hügel, Schmetterlinge (wenn alles gesund ist) und Glühwürmchen in der Nacht.
 * Inhalt (z. B. Pflanzen, Begrüßung) wird darüber gelegt.
 */
@Composable
fun SkyScene(
    nowMs: Long,
    happy: Boolean,
    modifier: Modifier = Modifier,
    decos: Set<String> = emptySet(),
    surprises: List<Pair<Int, String>> = emptyList(),
    onSurprise: (Int) -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val (time, hour) = DayTime.now(nowMs)
    val date = java.time.Instant.ofEpochMilli(nowMs).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    val season = de.wissensgarten.core.model.Seasons.season(date)
    val festival = de.wissensgarten.core.model.Seasons.festival(date)
    val wind = LocalWind.current
    Box(modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))) {
        Canvas(Modifier.matchParentSize()) {
            val t = wind?.value ?: 0f
            val (top, bottom) = skyColors(time)
            drawRect(Brush.verticalGradient(listOf(top, bottom)))
            if (time == DayTime.NIGHT) stars(t)
            sunOrMoon(time, hour)
            if ("regenbogen" in decos && time != DayTime.NIGHT) drawDeco("regenbogen", 0f, 0f, 0f, t, false)
            clouds(t, if (time == DayTime.NIGHT) 0.18f else 0.9f)
            hills(time, season)
            if ("zaun" in decos) drawDeco("zaun", 0f, size.height - 22.dp.toPx(), 40.dp.toPx(), t, false)
            if (happy && time != DayTime.NIGHT) butterflies(t)
            if (time == DayTime.NIGHT && season != de.wissensgarten.core.model.Season.WINTER) fireflies(t)
            seasonParticles(season, t)
            if (festival?.id == "silvester") fireworks(t)
        }
        content()
        // Fest-Überraschungen: in den Hügeln versteckt, antippen zum Einsammeln
        if (surprises.isNotEmpty()) BoxWithConstraints(Modifier.matchParentSize()) {
            val spots = listOf(0.1f to 0.55f, 0.5f to 0.5f, 0.86f to 0.6f)
            val wobble = (wind?.value ?: 0f)
            surprises.forEach { (i, emoji) ->
                val (fx, fy) = spots[i % spots.size]
                androidx.compose.material3.Text(
                    emoji, fontSize = androidx.compose.ui.unit.TextUnit(30f, androidx.compose.ui.unit.TextUnitType.Sp),
                    modifier = Modifier.offset(x = maxWidth * fx - 18.dp, y = maxHeight * fy)
                        .graphicsLayer { rotationZ = kotlin.math.sin(wobble * 3f + i) * 12f }
                        .clickable(androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null) { onSurprise(i) },
                )
            }
        }
        // Vordergrund-Deko zwischen den Pflanzen
        val front = decos.filter { it in DecoSlots.front }
        if (front.isNotEmpty()) Canvas(Modifier.matchParentSize()) {
            val t = wind?.value ?: 0f
            val ground = size.height - 12.dp.toPx()
            for (id in front) {
                val h = (if (id == "teich") 38 else 44).dp.toPx()
                drawDeco(id, size.width * DecoSlots.front.getValue(id), ground, h, t, time == DayTime.NIGHT)
            }
        }
    }
}

private fun DrawScope.stars(t: Float) {
    val r = kotlin.random.Random(11)
    repeat(40) {
        val x = r.nextFloat() * size.width
        val y = r.nextFloat() * size.height * 0.55f
        val tw = 0.5f + 0.5f * sin(t * (1f + r.nextFloat() * 2f) + it)
        drawCircle(Color.White.copy(alpha = 0.3f + 0.6f * tw), 1.2f.dp.toPx() * (0.6f + r.nextFloat()), Offset(x, y))
    }
}

private fun DrawScope.sunOrMoon(time: DayTime, hour: Float) {
    val w = size.width; val h = size.height
    if (time == DayTime.NIGHT) {
        val c = Offset(w * 0.8f, h * 0.4f)
        val r = h * 0.085f
        drawCircle(Color(0xFFFFF8E1).copy(alpha = 0.12f), r * 2.2f, c)
        // echte Sichel: Kreis minus versetzter Kreis
        val full = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }
        val cut = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c + Offset(r * 0.5f, -r * 0.25f), r * 0.88f)) }
        drawPath(Path.combine(androidx.compose.ui.graphics.PathOperation.Difference, full, cut), Color(0xFFFFF8E1))
    } else {
        // Sonne wandert von links (6 Uhr) nach rechts (21 Uhr)
        val f = ((hour - 6f) / 15f).coerceIn(0f, 1f)
        val c = Offset(w * (0.12f + 0.76f * f), h * (0.56f - 0.2f * sin(PI * f).toFloat()))
        val r = h * 0.08f
        val sun = if (time == DayTime.DAY) Color(0xFFFFE082) else Color(0xFFFFB74D)
        drawCircle(sun.copy(alpha = 0.25f), r * 2.2f, c)
        drawCircle(sun.copy(alpha = 0.45f), r * 1.5f, c)
        drawCircle(sun, r, c)
    }
}

private fun DrawScope.clouds(t: Float, alpha: Float) {
    val w = size.width; val h = size.height
    listOf(Triple(0.1f, 0.18f, 1f), Triple(0.55f, 0.1f, 0.7f), Triple(0.85f, 0.3f, 0.85f)).forEachIndexed { i, (x0, y, s) ->
        val speed = 0.006f + i * 0.003f
        val x = (((x0 + t * speed) % 1.3f) - 0.15f) * w
        val r = h * 0.06f * s
        val cy = y * h + h * 0.12f
        // eine zusammenhängende Form, damit sich durchsichtige Teile nicht überlagern
        val cloud = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(Offset(x + r * 1.2f, cy - r * 0.2f), r * 1.15f))
            addOval(androidx.compose.ui.geometry.Rect(Offset(x + r * 0.3f, cy + r * 0.3f), r * 0.8f))
            addOval(androidx.compose.ui.geometry.Rect(Offset(x + r * 2.2f, cy + r * 0.35f), r * 0.75f))
            addOval(androidx.compose.ui.geometry.Rect(x - r * 0.5f, cy + r * 0.1f, x + r * 3.0f, cy + r * 1.1f))
        }
        val merged = Path.combine(androidx.compose.ui.graphics.PathOperation.Union, cloud, Path())
        drawPath(merged, Color.White.copy(alpha = alpha))
    }
}

private fun DrawScope.hills(time: DayTime, season: de.wissensgarten.core.model.Season) {
    val w = size.width; val h = size.height
    val dim = when (time) { DayTime.NIGHT -> 0.55f; DayTime.EVENING -> 0.2f; else -> 0f }
    val (b0, f0) = when (season) {
        de.wissensgarten.core.model.Season.WINTER -> Color(0xFFF1F5F9) to Color(0xFFDDE6EE)
        de.wissensgarten.core.model.Season.AUTUMN -> Color(0xFFD4B24C) to Color(0xFFB98C3A)
        de.wissensgarten.core.model.Season.SPRING -> Color(0xFFAEDB7A) to Color(0xFF7DBB5F)
        else -> Color(0xFF9CCC65) to Color(0xFF6FA35A)
    }
    val back = lerp(b0, Color(0xFF1B2A1A), dim)
    val front = lerp(f0, Color(0xFF162414), dim)
    val p1 = Path().apply {
        moveTo(0f, h * 0.62f)
        cubicTo(w * 0.25f, h * 0.48f, w * 0.45f, h * 0.7f, w * 0.7f, h * 0.56f)
        cubicTo(w * 0.85f, h * 0.48f, w * 0.95f, h * 0.55f, w, h * 0.58f)
        lineTo(w, h); lineTo(0f, h); close()
    }
    drawPath(p1, back)
    val p2 = Path().apply {
        moveTo(0f, h * 0.78f)
        cubicTo(w * 0.3f, h * 0.68f, w * 0.6f, h * 0.84f, w, h * 0.72f)
        lineTo(w, h); lineTo(0f, h); close()
    }
    drawPath(p2, front)
}

private fun DrawScope.butterflies(t: Float) {
    val w = size.width; val h = size.height
    listOf(Color(0xFFFF8A65) to 0f, Color(0xFFBA68C8) to 2.1f).forEach { (c, ph) ->
        val x = w * (0.5f + 0.38f * sin(t * 0.23f + ph))
        val y = h * (0.42f + 0.1f * sin(t * 0.61f + ph * 2))
        val flap = 0.25f + 0.75f * abs(sin(t * 9f + ph))
        val s = h * 0.055f
        val light = lerp(c, Color.White, 0.35f)
        rotate(sin(t * 0.5f + ph) * 20f, Offset(x, y)) {
            val ww = s * flap
            // Vorderflügel
            drawOval(c, Offset(x - ww, y - s * 0.75f), Size(ww, s * 0.8f))
            drawOval(c, Offset(x, y - s * 0.75f), Size(ww, s * 0.8f))
            // Hinterflügel
            drawOval(light, Offset(x - ww * 0.75f, y - s * 0.05f), Size(ww * 0.75f, s * 0.55f))
            drawOval(light, Offset(x, y - s * 0.05f), Size(ww * 0.75f, s * 0.55f))
            drawOval(Color(0xFF4E342E), Offset(x - s * 0.08f, y - s * 0.6f), Size(s * 0.16f, s * 1.1f))
        }
    }
}

private fun DrawScope.fireflies(t: Float) {
    val w = size.width; val h = size.height
    repeat(9) { i ->
        val x = w * (0.08f + 0.84f * ((i * 0.37f + 0.05f * sin(t * 0.3f + i)) % 1f))
        val y = h * (0.6f + 0.25f * ((i * 0.53f) % 1f) + 0.04f * cos(t * 0.7f + i))
        val glow = (0.5f + 0.5f * sin(t * 2f + i * 1.7f)).coerceIn(0f, 1f)
        drawCircle(Color(0xFFFFF176).copy(alpha = 0.25f * glow), h * 0.025f, Offset(x, y))
        drawCircle(Color(0xFFFFF59D).copy(alpha = 0.9f * glow), h * 0.008f, Offset(x, y))
    }
}

/** Text auf dem Himmel: hell bei Nacht/Abend, dunkel am Tag. */
fun skyTextColor(nowMs: Long): Color = when (DayTime.now(nowMs).first) {
    DayTime.NIGHT, DayTime.EVENING -> Color.White
    else -> Color(0xFF14321A)
}


/** Schnee, Herbstlaub oder Blütenblätter – je nach Jahreszeit. */
private fun DrawScope.seasonParticles(season: de.wissensgarten.core.model.Season, t: Float) {
    val w = size.width; val h = size.height
    val (count, color) = when (season) {
        de.wissensgarten.core.model.Season.WINTER -> 45 to Color.White
        de.wissensgarten.core.model.Season.AUTUMN -> 14 to Color(0xFFE67E22)
        de.wissensgarten.core.model.Season.SPRING -> 16 to Color(0xFFF8BBD0)
        else -> return
    }
    val r = kotlin.random.Random(21)
    repeat(count) { i ->
        val speed = 0.04f + r.nextFloat() * 0.05f
        val x0 = r.nextFloat()
        val y = ((r.nextFloat() + t * speed) % 1.1f) - 0.05f
        val x = (x0 + sin(t * 0.8f + i) * 0.03f) * w
        val p = Offset(x, y * h)
        when (season) {
            de.wissensgarten.core.model.Season.WINTER -> drawCircle(color.copy(alpha = 0.85f), (1.5f + r.nextFloat() * 2.5f) * density, p)
            else -> rotate(t * 90f + i * 40f, p) {
                val c = if (season == de.wissensgarten.core.model.Season.AUTUMN) listOf(Color(0xFFE67E22), Color(0xFFC0392B), Color(0xFFF1C40F))[i % 3] else color
                drawOval(c, Offset(p.x - 5 * density, p.y - 3 * density), Size(10 * density, 6 * density))
            }
        }
    }
}

/** Silvester: Raketen platzen am Himmel. */
private fun DrawScope.fireworks(t: Float) {
    val w = size.width; val h = size.height
    val cols = listOf(Color(0xFFFFD54F), Color(0xFFFF5252), Color(0xFF69F0AE), Color(0xFF40C4FF))
    for (k in 0 until 3) {
        val phase = (t * 0.45f + k * 0.33f) % 1f
        val c = Offset(w * (0.2f + 0.3f * k), h * (0.18f + 0.08f * (k % 2)))
        val r = phase * h * 0.18f
        for (j in 0 until 12) {
            val a = j * 0.5236f
            drawCircle(cols[(k + j) % 4].copy(alpha = (1 - phase)), 2.5f * density, Offset(c.x + kotlin.math.cos(a) * r, c.y + sin(a) * r))
        }
    }
}
