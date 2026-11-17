package de.wissensgarten.app.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import de.wissensgarten.app.widget.Flower
import de.wissensgarten.app.widget.GardenBitmap
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Alles, was auf Postkarte und Urkunde steht. Wird aus den echten Lerndaten berechnet. */

/**
 * Oma-Postkarte (Bild zum Teilen) und Urkunde (PDF zum Ausdrucken).
 * Gemalt wird im Code – kein Server, nichts wird hochgeladen; geteilt wird nur, wenn man selbst auf „Senden“ tippt.
 */
object Keepsakes {
    private val CREAM = 0xFFFFFBF0.toInt()
    private val GREEN = 0xFF2E5E2A.toInt()
    private val BROWN = 0xFF4E3B00.toInt()

    /** [hour]: Tageszeit des Himmels (Bildschirmfoto-Tests geben sie fest vor) */
    fun postcard(d: KeepsakeData, hour: Int = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)): Bitmap {
        val w = 1080; val h = 1350
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(CREAM)
        val garden = GardenBitmap.render(w, 520, d.flowers, hour.coerceIn(8, 17), party = true)
        c.drawBitmap(garden, 0f, 0f, null)
        // Briefmarke oben rechts
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = Color.WHITE; c.drawRoundRect(RectF(w - 230f, 30f, w - 40f, 250f), 12f, 12f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 6f; p.pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f); p.color = 0xFFE53935.toInt()
        c.drawRoundRect(RectF(w - 222f, 38f, w - 48f, 242f), 10f, 10f, p)
        p.pathEffect = null; p.style = Paint.Style.FILL
        text(c, "🐝", w - 135f, 175f, 110f, Color.BLACK, center = true)

        var y = 610f
        text(c, "Grüße aus ${d.name}s Wissensgarten! ${d.avatar}", w / 2f, y, 52f, GREEN, bold = true, center = true); y += 80
        val stats = listOf(
            "💧" to "${d.reviewsWeek}× gegossen diese Woche",
            "🌱" to "${d.newThisWeek} neue Pflanzen gesät",
            "🌳" to "${d.grown} Pflanzen gewachsen, ${d.trees} davon schon Bäume",
        )
        stats.forEach { (e, t) -> text(c, "$e  $t", 90f, y, 40f, BROWN); y += 58 }
        if (d.learned.isNotEmpty()) {
            y += 20
            text(c, "Das weiß ich jetzt:", 90f, y, 40f, GREEN, bold = true); y += 58
            d.learned.take(3).forEach { (e, a) -> text(c, "$e  ${a.take(36)}", 120f, y, 38f, BROWN); y += 56 }
        }
        // Brummis Gruß in einer Sprechblase
        val by = maxOf(y + 70f, h - 230f).coerceAtMost(h - 140f)
        p.color = Color.WHITE; c.drawRoundRect(RectF(60f, by - 70f, w - 60f, by + 60f), 60f, 60f, p)
        text(c, "🐝  ${d.name} war fleißig wie eine Biene!", w / 2f, by + 15f, 42f, BROWN, center = true)
        text(c, "Wissensgarten", w / 2f, h - 40f, 30f, 0xFF8A8270.toInt(), center = true)
        return bmp
    }

    fun certificatePdf(ctx: Context, d: KeepsakeData): File {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        drawCertificate(page.canvas, d)
        doc.finishPage(page)
        val f = File(dir(ctx), "Urkunde-${safe(d.name)}.pdf")
        f.outputStream().use { doc.writeTo(it) }
        doc.close()
        return f
    }

    /** Urkunde auf A4 (595 × 842 Punkte) zeichnen – für PDF und Vorschau. */
    fun drawCertificate(c: Canvas, d: KeepsakeData) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        c.drawColor(CREAM)
        // Doppelrahmen in Gold mit Blumen in den Ecken
        p.style = Paint.Style.STROKE; p.color = 0xFFC9A227.toInt()
        p.strokeWidth = 6f; c.drawRect(24f, 24f, 571f, 818f, p)
        p.strokeWidth = 2f; c.drawRect(36f, 36f, 559f, 806f, p)
        p.style = Paint.Style.FILL
        listOf(48f to 66f, 547f to 66f, 48f to 800f, 547f to 800f).forEach { (x, y) -> text(c, "🌸", x, y, 26f, Color.BLACK, center = true) }

        text(c, "URKUNDE", 297f, 120f, 44f, GREEN, bold = true, center = true)
        text(c, "Wissensgarten", 297f, 150f, 16f, 0xFF8A8270.toInt(), center = true)
        text(c, "für", 297f, 205f, 18f, BROWN, center = true)
        text(c, "${d.name} ${d.avatar}", 297f, 250f, 36f, BROWN, bold = true, center = true)
        val garden = GardenBitmap.render(480, 220, d.flowers, 11, party = false)
        c.drawBitmap(garden, null, RectF(77f, 280f, 518f, 482f), null)
        var y = 525f
        listOf(
            "hat ${d.grown} Pflanzen wachsen lassen,",
            "davon ${d.trees} prächtige Bäume,",
            "und ${d.reviewsTotal}-mal fleißig gegossen.",
        ).forEach { text(c, it, 297f, y, 18f, BROWN, center = true); y += 28 }
        if (d.beds.isNotEmpty()) { y += 8; text(c, "Beete: " + d.beds.take(4).joinToString(" · "), 297f, y, 13f, 0xFF6D5A1E.toInt(), center = true) }
        // Siegel
        p.color = 0xFFE53935.toInt(); c.drawCircle(450f, 700f, 52f, p)
        p.color = 0xFFFFD54F.toInt(); c.drawCircle(450f, 700f, 42f, p)
        text(c, "🐝", 450f, 716f, 40f, Color.BLACK, center = true)
        text(c, LocalDate.now().format(DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMAN)), 150f, 690f, 16f, BROWN, center = true)
        p.color = BROWN; p.strokeWidth = 1.5f; c.drawLine(80f, 700f, 220f, 700f, p)
        text(c, "Datum", 150f, 718f, 12f, 0xFF8A8270.toInt(), center = true)
        text(c, "Brummi, Gartenmeister", 450f, 775f, 12f, 0xFF8A8270.toInt(), center = true)
    }

    fun savePng(ctx: Context, bmp: Bitmap, name: String): File =
        File(dir(ctx), name).also { f -> f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }

    /** Teilen-Dialog (WhatsApp, Mail, Drucken …) */
    fun share(ctx: Context, file: File, mime: String, text: String) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.updates", file)
        val send = Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri).putExtra(Intent.EXTRA_TEXT, text)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(Intent.createChooser(send, "Teilen").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun dir(ctx: Context) = File(ctx.cacheDir, "teilen").apply { mkdirs() }

    /** Namen wie „Lena/Ben“ dürfen keinen Dateipfad zerschießen */
    fun safe(name: String) = name.replace(Regex("[^\\p{L}\\p{N}_-]"), "_").ifBlank { "Garten" }

    private fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = false, center: Boolean = false) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size; this.color = color
            typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
            textAlign = if (center) Paint.Align.CENTER else Paint.Align.LEFT
        }
        c.drawText(s, x, y, p)
    }
}
