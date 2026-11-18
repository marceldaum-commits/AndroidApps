package de.wissensgarten.app.ui.share

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.app.ui.common.BackHandler
import de.wissensgarten.app.ui.common.Clock
import de.wissensgarten.app.ui.fx.*
import de.wissensgarten.core.model.GardenFilm
import de.wissensgarten.core.model.Stage
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Eine Lernkarte für den Bastelbogen. */
data class CraftItem(val front: String, val back: String, val img: String?)

/** Dateinamen ohne Sonderzeichen („Lena/Ben“ darf keinen Pfad zerschießen). */
fun safeFileName(name: String) = name.map { if (it.isLetterOrDigit() || it == '_' || it == '-') it else '_' }.joinToString("").ifBlank { "Garten" }

/**
 * Malt ein Bild des Gartens zu einem Zeitpunkt – für den Zeitraffer-Film und den Filmstreifen zum Teilen.
 * Reines Compose-Zeichnen: läuft gleich in App und WebApp.
 */
object FilmPainter {
    private val petals = listOf(Color(0xFFE91E63), Color(0xFFFFC107), Color(0xFF9C27B0), Color(0xFFFF5722), Color(0xFF03A9F4))

    private fun paint(w: Int, h: Int, block: DrawScope.() -> Unit): ImageBitmap {
        val img = ImageBitmap(w, h)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(img), Size(w.toFloat(), h.toFloat()), block)
        return img
    }

    fun frame(f: GardenFilm.Frame, measurer: TextMeasurer, w: Int = 640, h: Int = 400, name: String = ""): ImageBitmap = paint(w, h) {
        drawFrame(f, measurer, Offset.Zero, w.toFloat(), h.toFloat(), name)
    }

    private fun DrawScope.drawFrame(f: GardenFilm.Frame, measurer: TextMeasurer, o: Offset, w: Float, h: Float, name: String) {
        drawRect(de.wissensgarten.app.ui.fx.SafeBrush.verticalGradient(listOf(Color(0xFF81D4FA), Color(0xFFE1F5FE)), startY = o.y, endY = o.y + h * 0.7f), o, Size(w, h))
        drawCircle(Color(0xFFFFEB3B), h * 0.08f, Offset(o.x + w * 0.85f, o.y + h * 0.15f))
        val ground = o.y + h * 0.62f
        drawRect(Color(0xFF8BC34A), Offset(o.x, ground), Size(w, o.y + h - ground))
        drawRect(Color(0xFF7CB342), Offset(o.x, ground), Size(w, 10f))
        val k = h / 400f
        f.stages.take(60).forEachIndexed { i, s ->
            val r = Random(i * 7919)
            val x = o.x + (i % 12 + 0.5f + (r.nextFloat() - 0.5f) * 0.4f) * w / 12
            val y = ground + 30f + (i / 12) * 26f
            plant(s, x, y, k, petals[i % petals.size])
        }
        val label = (if (name.isNotBlank()) "$name · " else "") + Clock.shortDate(Clock.date(f.time))
        val t = measurer.measure(label, TextStyle(fontSize = (h * 0.065f).sp, fontWeight = FontWeight.Bold, color = Color(0xFF33691E)))
        drawRoundRect(Color(0xAAFFFFFF), Offset(o.x + 12f, o.y + 12f), Size(t.size.width + 16f, t.size.height + 8f), CornerRadius(14f))
        drawText(t, topLeft = Offset(o.x + 20f, o.y + 16f))
        drawText(measurer, "🌱 ${f.stages.size} Pflanzen", Offset(o.x + 18f, o.y + h - h * 0.08f),
            TextStyle(fontSize = (h * 0.045f).sp, color = Color(0xFF33691E)))
    }

    private fun DrawScope.plant(s: Stage, x: Float, y: Float, k: Float, petal: Color) {
        val green = Color(0xFF388E3C)
        when (s) {
            Stage.SEED -> drawOval(Color(0xFF795548), Offset(x - 5 * k, y - 3 * k), Size(10 * k, 6 * k))
            Stage.SPROUT -> {
                drawRect(green, Offset(x - 1.5f * k, y - 12 * k), Size(3 * k, 12 * k))
                drawOval(green, Offset(x - 9 * k, y - 16 * k), Size(9 * k, 7 * k)); drawOval(green, Offset(x, y - 16 * k), Size(9 * k, 7 * k))
            }
            Stage.SHOOT -> {
                drawRect(green, Offset(x - 2 * k, y - 26 * k), Size(4 * k, 26 * k))
                repeat(3) { j -> val yy = y - (8 + j * 7) * k
                    drawOval(green, Offset(x - 11 * k, yy - 4 * k), Size(11 * k, 7 * k)); drawOval(green, Offset(x, yy - 7 * k), Size(11 * k, 7 * k)) }
            }
            Stage.BUSH -> { drawCircle(Color(0xFF43A047), 15 * k, Offset(x, y - 14 * k)); drawCircle(Color(0xFF66BB6A), 9 * k, Offset(x - 7 * k, y - 18 * k)) }
            Stage.BLOOM -> {
                drawRect(green, Offset(x - 2 * k, y - 34 * k), Size(4 * k, 34 * k))
                for (a in 0 until 6) { val ang = a * PI / 3
                    drawCircle(petal, 6 * k, Offset(x + (cos(ang) * 8 * k).toFloat(), y - 38 * k + (sin(ang) * 8 * k).toFloat())) }
                drawCircle(Color(0xFFFFEB3B), 5 * k, Offset(x, y - 38 * k))
            }
            Stage.TREE -> {
                drawRect(Color(0xFF6D4C41), Offset(x - 4 * k, y - 34 * k), Size(8 * k, 34 * k))
                drawCircle(Color(0xFF2E7D32), 20 * k, Offset(x, y - 46 * k)); drawCircle(Color(0xFF43A047), 11 * k, Offset(x - 9 * k, y - 52 * k))
                drawCircle(Color(0xFFE53935), 3.5f * k, Offset(x + 8 * k, y - 40 * k)); drawCircle(Color(0xFFE53935), 3.5f * k, Offset(x - 6 * k, y - 36 * k))
            }
        }
    }

    /** Filmstreifen: alle Bilder in zwei Spalten – zum Verschicken an Oma & Opa. */
    fun strip(frames: List<GardenFilm.Frame>, name: String, measurer: TextMeasurer): ImageBitmap {
        val fw = 640; val fh = 400; val pad = 24; val cols = 2
        val rows = (frames.size + cols - 1) / cols
        return paint(cols * fw + (cols + 1) * pad, rows * fh + (rows + 1) * pad + 90) {
            drawRect(Color(0xFF263238))
            drawText(measurer, "🎬 So ist ${name}s Wissensgarten gewachsen", Offset(pad.toFloat(), 22f),
                TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White))
            frames.forEachIndexed { i, f ->
                drawFrame(f, measurer, Offset((pad + (i % cols) * (fw + pad)).toFloat(), (90 + pad + (i / cols) * (fh + pad)).toFloat()), fw.toFloat(), fh.toFloat(), name)
            }
        }
    }
}

@Composable
fun GardenFilmScreen(frames: List<GardenFilm.Frame>, name: String, onShare: () -> Unit, onBack: () -> Unit) {
    BackHandler { onBack() }
    val sfx = LocalSfx.current
    val measurer = rememberTextMeasurer()
    val bitmaps = remember(frames) { frames.map { FilmPainter.frame(it, measurer, name = name) } }
    var i by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(true) }
    LaunchedEffect(playing, bitmaps) {
        if (!playing || bitmaps.isEmpty()) return@LaunchedEffect
        while (i < bitmaps.size - 1) { delay(1100); i++; sfx?.play(Sound.GROW, pitch = 0.8f + i * 0.06f, volume = 0.5f) }
        sfx?.play(Sound.FANFARE); playing = false
    }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück") }
            Text("🎬 Mein Garten-Film", style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (bitmaps.isEmpty()) { Text("Noch keine Pflanzen – säe Samen, dann gibt's bald einen Film!"); return@Column }
            Crossfade(i, animationSpec = tween(500), label = "film") { k ->
                Image(bitmaps[k], contentDescription = "Garten am Bild ${k + 1}", contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)))
            }
            Slider(value = i.toFloat(), onValueChange = { playing = false; i = it.toInt() }, valueRange = 0f..(bitmaps.size - 1).toFloat(), steps = (bitmaps.size - 2).coerceAtLeast(0))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { i = 0; playing = true }) { Text("▶ Nochmal abspielen") }
                OutlinedButton(onClick = onShare) { Text("💌 An Oma schicken") }
            }
            val young = remember(frames) { frames.map { it.stages }.distinct().size <= 1 || frames.last().time - frames.first().time < 2 * de.wissensgarten.core.model.Scheduler.DAY_MS }
            Brummi(if (playing) Mood.HAPPY else Mood.CHEER, size = 72.dp,
                speech = when {
                    young && !playing -> "Dein Garten ist noch ganz jung! Gieß ein paar Tage – dann wird der Film richtig spannend. 🌱"
                    playing -> "Schau, wie alles wächst! 🌱"
                    else -> "Das hast du alles selbst gegossen! 🌻"
                })
            Text("Der Film wird aus dem Lernstand berechnet – ohne Fotos, alles auf dem Gerät.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

