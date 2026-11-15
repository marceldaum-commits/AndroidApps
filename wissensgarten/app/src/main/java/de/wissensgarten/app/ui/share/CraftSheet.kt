package de.wissensgarten.app.ui.share

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import java.io.File

/**
 * Bastelbogen: Lernkarten als A4-PDF zum Ausdrucken. Links die Frage, rechts die Antwort –
 * in der Mitte falten, ausschneiden, fertig. Für den Kühlschrank oder die Autofahrt.
 */
object CraftSheet {
    
    private const val W = 595; private const val H = 842; private const val ROWS = 6

    fun pdf(ctx: Context, bedName: String, emoji: String, items: List<CraftItem>): File {
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
    fun preview(bedName: String, emoji: String, items: List<CraftItem>, scale: Float = 2f): Bitmap {
        val bmp = Bitmap.createBitmap((W * scale).toInt(), (H * scale).toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp); c.drawColor(0xFFFFFFFF.toInt()); c.scale(scale, scale)
        drawPage(c, bedName, emoji, items.take(ROWS), 0, (items.size + ROWS - 1) / ROWS)
        return bmp
    }

    private fun drawPage(c: Canvas, bedName: String, emoji: String, chunk: List<CraftItem>, pi: Int, pages: Int) {
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
