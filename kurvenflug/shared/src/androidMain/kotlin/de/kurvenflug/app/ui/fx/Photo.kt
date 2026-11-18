package de.kurvenflug.app.ui.fx

import android.content.Context
import android.content.Intent
import android.graphics.*
import androidx.core.content.FileProvider
import de.kurvenflug.core.model.*
import java.io.File

/**
 * Fotomodus: die geschaffte Flugbahn als Bild – Raster, Stationen, leuchtende Bahn, Titel und Sterne.
 * Nur das Bild wird geteilt, keine Namen oder sonstigen Daten.
 */
object Photo {
    fun render(level: Level, path: List<Vec>, stars: Int, values: Map<String, Double>, size: Int = 1080): Bitmap {
        val bmp = Bitmap.createBitmap(size, (size * 1.25f).toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val top = size * 0.18f
        c.drawColor(Color.rgb(13, 17, 46))
        val star = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val rnd = java.util.Random(level.id.hashCode().toLong())
        repeat(120) { star.alpha = 60 + rnd.nextInt(150); c.drawCircle(rnd.nextFloat() * size, rnd.nextFloat() * bmp.height, 1f + rnd.nextFloat() * 2.5f, star) }
        fun px(p: Vec) = PointF(((p.x + Flight.R) / (2 * Flight.R) * size).toFloat(), (top + (Flight.R - p.y) / (2 * Flight.R) * size).toFloat())
        val grid = Paint().apply { color = Color.argb(40, 120, 200, 255); strokeWidth = 2f }
        for (i in -9..9) {
            val a = px(Vec(i.toDouble(), -Flight.R)); val b = px(Vec(i.toDouble(), Flight.R)); c.drawLine(a.x, a.y, b.x, b.y, grid)
            val d = px(Vec(-Flight.R, i.toDouble())); val e = px(Vec(Flight.R, i.toDouble())); c.drawLine(d.x, d.y, e.x, e.y, grid)
        }
        val rock = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(140, 110, 100) }
        level.asteroids.forEach { val o = px(Vec(it.x, it.y)); c.drawCircle(o.x, o.y, (it.r / (2 * Flight.R) * size).toFloat(), rock) }
        val station = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(76, 245, 154) }
        level.targets.forEach { val o = px(Vec(it.x, it.y)); c.drawCircle(o.x, o.y, maxOf(14f, (it.r / (2 * Flight.R) * size).toFloat()), station) }
        // leuchtende Bahn
        if (path.size > 1) {
            val p = Path().apply { val s = px(path[0]); moveTo(s.x, s.y); path.drop(1).forEach { v -> val q = px(v); lineTo(q.x, q.y) } }
            c.drawPath(p, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 26f; color = Color.argb(70, 61, 232, 255); strokeCap = Paint.Cap.ROUND })
            c.drawPath(p, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 9f; color = Color.rgb(61, 232, 255); strokeCap = Paint.Cap.ROUND })
        }
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(61, 232, 255); textSize = size * 0.06f; typeface = Typeface.DEFAULT_BOLD }
        drawLogo(c, size * 0.06f, size * 0.025f, size * 0.085f)
        c.drawText("KURVENFLUG", size * 0.16f, size * 0.09f, title)
        val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = size * 0.042f }
        c.drawText("${level.title}  " + "★".repeat(stars) + "☆".repeat(3 - stars), size * 0.06f, size * 0.15f, sub)
        val formula = level.params.joinToString("   ") { "${it.label} = ${num(level.values(values).getValue(it.key))}${it.unit}" }
        val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(169, 176, 214); textSize = size * 0.04f }
        c.drawText(formula, size * 0.06f, top + size + size * 0.045f, small)
        return bmp
    }

    fun share(context: Context, level: Level, path: List<Vec>, stars: Int, values: Map<String, Double>) {
        val dir = File(context.cacheDir, "photos").apply { mkdirs() }
        val f = File(dir, "kurvenflug.png")
        f.outputStream().use { render(level, path, stars, values).compress(Bitmap.CompressFormat.PNG, 95, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", f)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Meine Flugbahn in Kurvenflug: ${level.title} 🚀")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Flugbahn teilen").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** Das App-Logo (wie icons/icon.svg, innerer Bereich wie das Android-Symbol) bei (x, y) mit Kantenlänge w */
internal fun drawLogo(c: Canvas, x: Float, y: Float, w: Float) {
    val crop = 18f; val k = w / (108f - 2 * crop)
    fun px(v: Float) = x + (v - crop) * k
    fun py(v: Float) = y + (v - crop) * k
    c.drawRoundRect(android.graphics.RectF(x, y, x + w, y + w), w * 0.22f, w * 0.22f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(11, 16, 38) })
    val arc = Path().apply { moveTo(px(30f), py(76f)); quadTo(px(46f), py(26f), px(76f), py(62f)) }
    c.drawPath(arc, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3.5f * k; color = Color.rgb(56, 232, 255); strokeCap = Paint.Cap.ROUND })
    c.drawCircle(px(76f), py(62f), 7f * k, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 92, 168) })
    val probe = Path().apply { moveTo(px(50f), py(38f)); lineTo(px(58f), py(43f)); lineTo(px(50f), py(48f)); lineTo(px(52f), py(43f)); close() }
    c.drawPath(probe, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 213, 79) })
    val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    c.drawCircle(px(36f), py(38f), 1.5f * k, white); c.drawCircle(px(70f), py(34f), 1.2f * k, white)
}

