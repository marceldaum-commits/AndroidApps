package de.wissensgarten.app.ui.share

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.wissensgarten.app.ui.fx.*
import de.wissensgarten.core.model.GardenFilm
import de.wissensgarten.core.model.Stage
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/** Malt ein Bild des Gartens zu einem Zeitpunkt – für den Zeitraffer-Film und den Filmstreifen zum Teilen. */
object FilmPainter {
    private val date = SimpleDateFormat("d. MMM yyyy", Locale.GERMAN)
    private val petals = intArrayOf(0xFFE91E63.toInt(), 0xFFFFC107.toInt(), 0xFF9C27B0.toInt(), 0xFFFF5722.toInt(), 0xFF03A9F4.toInt())

    fun frame(f: GardenFilm.Frame, w: Int = 640, h: Int = 400, name: String = ""): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, 0f, h * 0.7f, 0xFF81D4FA.toInt(), 0xFFE1F5FE.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p); p.shader = null
        p.color = 0xFFFFEB3B.toInt(); c.drawCircle(w * 0.85f, h * 0.15f, h * 0.08f, p)
        val ground = h * 0.62f
        p.color = 0xFF8BC34A.toInt(); c.drawRect(0f, ground, w.toFloat(), h.toFloat(), p)
        p.color = 0xFF7CB342.toInt(); c.drawRect(0f, ground, w.toFloat(), ground + 10f, p)
        // Pflanzen in Reihen – feste Plätze, damit man sie wachsen sieht
        val plants = f.stages.take(60)
        val cols = 12
        plants.forEachIndexed { i, s ->
            val r = Random(i * 7919)
            val row = i / cols
            val x = (i % cols + 0.5f + (r.nextFloat() - 0.5f) * 0.4f) * w / cols
            val y = ground + 30f + row * 26f
            plant(c, p, s, x, y, h / 400f, petals[i % petals.size])
        }
        p.color = 0xCC000000.toInt(); p.textSize = h * 0.065f; p.isFakeBoldText = true
        val label = (if (name.isNotBlank()) "$name · " else "") + date.format(Date(f.time))
        p.color = 0xAAFFFFFF.toInt(); c.drawRoundRect(12f, 12f, 24f + p.measureText(label), 20f + p.textSize * 1.25f, 14f, 14f, p)
        p.color = 0xFF33691E.toInt(); c.drawText(label, 18f, 12f + p.textSize, p)
        p.textSize = h * 0.045f; p.isFakeBoldText = false; p.color = 0xFF33691E.toInt()
        c.drawText("🌱 ${f.stages.size} Pflanzen", 18f, h - 14f, p)
        return bmp
    }

    private fun plant(c: Canvas, p: Paint, s: Stage, x: Float, y: Float, k: Float, petal: Int) {
        val green = 0xFF388E3C.toInt()
        when (s) {
            Stage.SEED -> { p.color = 0xFF795548.toInt(); c.drawOval(x - 5 * k, y - 3 * k, x + 5 * k, y + 3 * k, p) }
            Stage.SPROUT -> { p.color = green; c.drawRect(x - 1.5f * k, y - 12 * k, x + 1.5f * k, y, p); c.drawOval(x - 9 * k, y - 16 * k, x, y - 9 * k, p); c.drawOval(x, y - 16 * k, x + 9 * k, y - 9 * k, p) }
            Stage.SHOOT -> { p.color = green; c.drawRect(x - 2 * k, y - 26 * k, x + 2 * k, y, p); repeat(3) { j -> val yy = y - (8 + j * 7) * k; c.drawOval(x - 11 * k, yy - 4 * k, x, yy + 3 * k, p); c.drawOval(x, yy - 7 * k, x + 11 * k, yy, p) } }
            Stage.BUSH -> { p.color = 0xFF43A047.toInt(); c.drawCircle(x, y - 14 * k, 15 * k, p); p.color = 0xFF66BB6A.toInt(); c.drawCircle(x - 7 * k, y - 18 * k, 9 * k, p) }
            Stage.BLOOM -> {
                p.color = green; c.drawRect(x - 2 * k, y - 34 * k, x + 2 * k, y, p)
                p.color = petal; for (a in 0 until 6) { val ang = a * Math.PI / 3; c.drawCircle(x + (Math.cos(ang) * 8 * k).toFloat(), y - 38 * k + (Math.sin(ang) * 8 * k).toFloat(), 6 * k, p) }
                p.color = 0xFFFFEB3B.toInt(); c.drawCircle(x, y - 38 * k, 5 * k, p)
            }
            Stage.TREE -> {
                p.color = 0xFF6D4C41.toInt(); c.drawRect(x - 4 * k, y - 34 * k, x + 4 * k, y, p)
                p.color = 0xFF2E7D32.toInt(); c.drawCircle(x, y - 46 * k, 20 * k, p); p.color = 0xFF43A047.toInt(); c.drawCircle(x - 9 * k, y - 52 * k, 11 * k, p)
                p.color = 0xFFE53935.toInt(); c.drawCircle(x + 8 * k, y - 40 * k, 3.5f * k, p); c.drawCircle(x - 6 * k, y - 36 * k, 3.5f * k, p)
            }
        }
    }

    /** Filmstreifen: alle Bilder untereinander in zwei Spalten – zum Verschicken an Oma & Opa. */
    fun strip(frames: List<GardenFilm.Frame>, name: String): Bitmap {
        val fw = 640; val fh = 400; val pad = 24; val cols = 2
        val rows = (frames.size + cols - 1) / cols
        val bmp = Bitmap.createBitmap(cols * fw + (cols + 1) * pad, rows * fh + (rows + 1) * pad + 90, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp); val p = Paint(Paint.ANTI_ALIAS_FLAG)
        c.drawColor(0xFF263238.toInt())
        p.color = 0xFFFFFFFF.toInt(); p.textSize = 44f; p.isFakeBoldText = true
        c.drawText("🎬 So ist ${name}s Wissensgarten gewachsen", pad.toFloat(), 62f, p)
        frames.forEachIndexed { i, f ->
            val x = pad + (i % cols) * (fw + pad); val y = 90 + pad + (i / cols) * (fh + pad)
            c.drawBitmap(frame(f, fw, fh), x.toFloat(), y.toFloat(), null)
        }
        return bmp
    }
}

@Composable
fun GardenFilmScreen(frames: List<GardenFilm.Frame>, name: String, onShare: () -> Unit, onBack: () -> Unit) {
    BackHandler { onBack() }
    val sfx = LocalSfx.current
    val bitmaps = remember(frames) { frames.map { FilmPainter.frame(it, name = name).asImageBitmap() } }
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

/**
 * Bastelbogen: Lernkarten als A4-PDF zum Ausdrucken. Links die Frage, rechts die Antwort –
 * in der Mitte falten, ausschneiden, fertig. Für den Kühlschrank oder die Autofahrt.
 */
object CraftSheet {
    data class Item(val front: String, val back: String, val img: String?)
    private const val W = 595; private const val H = 842; private const val ROWS = 6

    fun pdf(ctx: Context, bedName: String, emoji: String, items: List<Item>): File {
        val doc = PdfDocument()
        val pages = items.chunked(ROWS)
        pages.forEachIndexed { pi, chunk ->
            val page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pi + 1).create())
            drawPage(page.canvas, bedName, emoji, chunk, pi, pages.size)
            doc.finishPage(page)
        }
        val f = File(File(ctx.cacheDir, "teilen").apply { mkdirs() }, "Bastelbogen-${Keepsakes.safe(bedName)}.pdf")
        f.outputStream().use { doc.writeTo(it) }
        doc.close()
        return f
    }

    /** Vorschau einer Seite als Bild (für Bildschirmfotos und Tests). */
    fun preview(bedName: String, emoji: String, items: List<Item>, scale: Float = 2f): Bitmap {
        val bmp = Bitmap.createBitmap((W * scale).toInt(), (H * scale).toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp); c.drawColor(0xFFFFFFFF.toInt()); c.scale(scale, scale)
        drawPage(c, bedName, emoji, items.take(ROWS), 0, (items.size + ROWS - 1) / ROWS)
        return bmp
    }

    private fun drawPage(c: Canvas, bedName: String, emoji: String, chunk: List<Item>, pi: Int, pages: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = 0xFF3F6B3A.toInt(); p.textSize = 20f; p.isFakeBoldText = true
        c.drawText("$emoji $bedName – Lernkarten zum Basteln", 36f, 44f, p)
        p.isFakeBoldText = false; p.textSize = 10f; p.color = 0xFF777777.toInt()
        c.drawText("Ausschneiden ✂, in der Mitte falten – vorne die Frage, hinten die Antwort. Seite ${pi + 1}/$pages · Wissensgarten 🐝", 36f, 62f, p)
        val top = 78f; val rowH = (H - top - 30f) / ROWS; val left = 36f; val right = W - 36f; val mid = W / 2f
        chunk.forEachIndexed { r, it ->
            val y = top + r * rowH
            p.style = Paint.Style.STROKE; p.color = 0xFFBBBBBB.toInt(); p.strokeWidth = 1f
            p.pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
            c.drawRect(left, y, right, y + rowH - 6f, p)
            p.color = 0xFF8BC34A.toInt(); c.drawLine(mid, y + 4f, mid, y + rowH - 10f, p)
            p.pathEffect = null; p.style = Paint.Style.FILL
            cell(c, it.img, it.front, left, mid, y, 0xFF22261F.toInt(), "FRAGE")
            cell(c, null, it.back, mid, right, y, 0xFF2E7D32.toInt(), "ANTWORT")
        }
    }

    private fun cell(c: Canvas, img: String?, text: String, x0: Float, x1: Float, y: Float, color: Int, tag: String) {
        val tp = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG)
        tp.textSize = 8f; tp.color = 0xFF9E9E9E.toInt(); c.drawText(tag, x0 + 10f, y + 14f, tp)
        var yy = y + 20f
        if (img != null) { tp.textSize = 28f; tp.color = 0xFF000000.toInt(); c.drawText(img.take(6), x0 + 12f, yy + 28f, tp); yy += 36f }
        tp.textSize = 13f; tp.color = color
        val layout = android.text.StaticLayout.Builder.obtain(text, 0, text.length, tp, (x1 - x0 - 24f).toInt()).setMaxLines(5)
            .setEllipsize(android.text.TextUtils.TruncateAt.END).build()
        c.save(); c.translate(x0 + 12f, yy + 4f); layout.draw(c); c.restore()
    }
}
