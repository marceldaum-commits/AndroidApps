package de.raumlicht.app.ui.home

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File

/** Ergebnis-Karte zum Teilen: ein Bild mit Aufgabe, Punkten und Urteil – wird nur auf Knopfdruck erzeugt und über „Teilen“ verschickt. */
object ShareCard {
    fun draw(title: String, big: String, line: String): Bitmap {
        val w = 1080; val h = 1080
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), intArrayOf(0xFF2A1B5E.toInt(), 0xFF10173A.toInt(), 0xFF05071A.toInt()), null, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p); p.shader = null
        // Laserstrahlen als Schmuck
        p.strokeWidth = 10f; p.color = 0xFF38E8FF.toInt(); c.drawLine(-40f, 900f, 1120f, 260f, p)
        p.color = 0xFFFF4FA3.toInt(); c.drawLine(-40f, 300f, 1120f, 1000f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 6f; p.color = 0x8038E8FF.toInt(); c.drawRoundRect(RectF(40f, 40f, w - 40f, h - 40f), 60f, 60f, p); p.style = Paint.Style.FILL
        p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        p.color = 0xFF38E8FF.toInt(); p.textSize = 52f; p.letterSpacing = 0.25f; c.drawText("RAUMLICHT", w / 2f, 170f, p); p.letterSpacing = 0f
        p.color = 0xFFE8ECFF.toInt(); p.textSize = 76f; c.drawText(title, w / 2f, 330f, p)
        p.color = 0xFFFFD54F.toInt(); p.textSize = 190f; c.drawText(big, w / 2f, 600f, p)
        p.color = 0xFFE8ECFF.toInt(); p.textSize = 44f; p.typeface = Typeface.DEFAULT
        // Urteil in Zeilen umbrechen
        val words = line.split(" "); var row = ""; var y = 730f
        for (word in words) { if (p.measureText("$row $word") > w - 200f) { c.drawText(row.trim(), w / 2f, y, p); y += 60f; row = word } else row += " $word" }
        c.drawText(row.trim(), w / 2f, y, p)
        p.color = 0xFF9AA3D0.toInt(); p.textSize = 36f; c.drawText("Mathe-Abi in 3D", w / 2f, 990f, p)
        return bmp
    }

    fun share(ctx: Context, title: String, big: String, line: String) {
        runCatching {
            val file = File(ctx.cacheDir, "share").apply { mkdirs() }.resolve("raumlicht-ergebnis.png")
            file.outputStream().use { draw(title, big, line).compress(Bitmap.CompressFormat.PNG, 100, it) }
            val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".updates", file)
            ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
                .putExtra(Intent.EXTRA_TEXT, "$title: $big").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Ergebnis teilen"))
        }
    }
}
