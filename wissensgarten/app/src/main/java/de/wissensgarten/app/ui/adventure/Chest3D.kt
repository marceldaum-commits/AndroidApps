package de.wissensgarten.app.ui.adventure

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Kleine 3D-Szene für die Schatztruhe – ohne 3D-Bibliothek:
 * Flächen werden gedreht, perspektivisch projiziert, nach Tiefe sortiert (Maler-Algorithmus)
 * und je nach Lichteinfall schattiert.
 */
private data class V3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: V3) = V3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: V3) = V3(x - o.x, y - o.y, z - o.z)
    fun cross(o: V3) = V3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    fun dot(o: V3) = x * o.x + y * o.y + z * o.z
    fun norm(): V3 { val l = sqrt(dot(this)).coerceAtLeast(1e-6f); return V3(x / l, y / l, z / l) }
}

private class Face(val pts: List<V3>, val color: Color, val glow: Boolean = false)

private val Wood = Color(0xFF9A5B2B)
private val WoodDark = Color(0xFF7A4520)
private val Gold = Color(0xFFFFC83D)
private val Inside = Color(0xFF3E2414)
private val LIGHT = V3(-0.5f, 0.8f, 0.6f).norm()

private fun box(x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float, c: Color, sides: Set<Int>? = null): List<Face> {
    val p = listOf(
        V3(x0, y0, z0), V3(x1, y0, z0), V3(x1, y1, z0), V3(x0, y1, z0),
        V3(x0, y0, z1), V3(x1, y0, z1), V3(x1, y1, z1), V3(x0, y1, z1),
    )
    return listOf(
        Face(listOf(p[4], p[5], p[6], p[7]), c), // vorne (z1)
        Face(listOf(p[1], p[0], p[3], p[2]), c), // hinten
        Face(listOf(p[0], p[4], p[7], p[3]), c), // links
        Face(listOf(p[5], p[1], p[2], p[6]), c), // rechts
        Face(listOf(p[3], p[7], p[6], p[2]), c), // oben
        Face(listOf(p[0], p[1], p[5], p[4]), c), // unten
    ).filterIndexed { i, _ -> sides == null || i in sides }
}

/** Gewölbter Deckel: Querschnitt (z,y) als Bogen, entlang x ausgezogen. Liegt im Scharnier-Koordinatensystem. */
private fun lid(x0: Float, x1: Float, depth: Float, height: Float, bulge: Float, c: Color, caps: Boolean = true): List<Face> {
    val seg = 6
    val cs = buildList {
        add(0f to 0f)
        for (i in 0..seg) {
            val t = i / seg.toFloat()
            add(depth * (1 - t) to height + bulge * sin(PI * t).toFloat())
        }
        add(depth to 0f).also { } // wird unten wieder geschlossen
    }.let { listOf(0f to 0f, depth to 0f) + it.drop(1).dropLast(1) } // Ring: hinten-unten, vorne-unten, Bogen vorne→hinten
    val faces = mutableListOf<Face>()
    for (i in cs.indices) {
        val a = cs[i]; val b = cs[(i + 1) % cs.size]
        faces += Face(listOf(V3(x0, a.second, a.first), V3(x1, a.second, a.first), V3(x1, b.second, b.first), V3(x0, b.second, b.first)), c)
    }
    if (caps) {
        faces += Face(cs.map { V3(x0, it.second, it.first) }, lerp(c, Color.Black, 0.1f))
        faces += Face(cs.map { V3(x1, it.second, it.first) }, lerp(c, Color.Black, 0.1f))
    }
    return faces
}

/**
 * Zeichnet die Truhe.
 * @param yaw Drehung um die Hochachse (Grad), @param open Deckel 0 = zu, 1 = offen, @param glow Licht aus dem Inneren 0..1
 */
fun DrawScope.drawChest3D(yaw: Float, open: Float, glow: Float, wobble: Float = 0f) {
    val w = 2.0f; val hb = 1.05f; val d = 1.3f
    val lidH = 0.32f; val bulge = 0.38f
    val hingeAngle = -open * 115f * (PI / 180f).toFloat()

    val faces = mutableListOf<Face>()
    // Korpus + Innenraum (Innenfläche leicht unter dem Rand)
    faces += box(-w / 2, 0f, -d / 2, w / 2, hb, d / 2, Wood).filterIndexed { i, _ -> i != 4 }
    faces += Face(listOf(V3(-w / 2 + 0.06f, hb - 0.08f, d / 2 - 0.06f), V3(w / 2 - 0.06f, hb - 0.08f, d / 2 - 0.06f),
        V3(w / 2 - 0.06f, hb - 0.08f, -d / 2 + 0.06f), V3(-w / 2 + 0.06f, hb - 0.08f, -d / 2 + 0.06f)), Inside, glow = true)
    // Goldbeschläge am Korpus
    for (bx in listOf(-0.62f, 0.62f)) faces += box(bx - 0.08f, -0.01f, -d / 2 - 0.02f, bx + 0.08f, hb + 0.005f, d / 2 + 0.02f, Gold, sides = setOf(0, 1))
    faces += box(-w / 2 - 0.02f, 0.0f, -d / 2 - 0.02f, w / 2 + 0.02f, 0.1f, d / 2 + 0.02f, WoodDark)
    // Schloss
    faces += box(-0.16f, hb - 0.42f, d / 2, 0.16f, hb - 0.06f, d / 2 + 0.07f, Gold)

    // Deckel (um Scharnier hinten-oben gedreht)
    val lidFaces = lid(-w / 2 - 0.02f, w / 2 + 0.02f, d + 0.04f, lidH, bulge, Wood) +
        listOf(-0.62f, 0.62f).flatMap { bx -> lid(bx - 0.08f, bx + 0.08f, d + 0.08f, lidH + 0.02f, bulge + 0.02f, Gold, caps = false).drop(1) }
    val hinge = V3(0f, hb, -d / 2 - 0.02f)
    fun rotLid(p: V3): V3 {
        val y = p.y * cos(hingeAngle) - p.z * sin(hingeAngle)
        val z = p.y * sin(hingeAngle) + p.z * cos(hingeAngle)
        return V3(p.x, y, z) + hinge
    }
    faces += lidFaces.map { f -> Face(f.pts.map(::rotLid), f.color) }

    // Kamera: Drehteller (yaw) + leichter Blick von oben
    val yr = (yaw + wobble) * (PI / 180f).toFloat()
    val pitch = 22f * (PI / 180f).toFloat()  // Blick leicht von oben
    fun view(p0: V3): V3 {
        val p = p0 - V3(0f, 0.75f, 0f)
        val x1 = p.x * cos(yr) + p.z * sin(yr)
        val z1 = -p.x * sin(yr) + p.z * cos(yr)
        val y2 = p.y * cos(pitch) - z1 * sin(pitch)
        val z2 = p.y * sin(pitch) + z1 * cos(pitch)
        return V3(x1, y2, z2)
    }
    val camDist = 6.5f
    val scale = size.minDimension * 0.62f
    val cx = size.width / 2; val cy = size.height * 0.55f
    fun project(v: V3): Offset { val f = camDist / (camDist - v.z); return Offset(cx + v.x * f * scale / 2.2f, cy - v.y * f * scale / 2.2f) }

    // Schatten am Boden
    drawOval(Color.Black.copy(alpha = 0.25f), Offset(cx - scale * 0.55f, cy + scale * 0.28f), androidx.compose.ui.geometry.Size(scale * 1.1f, scale * 0.22f))

    // Lichtschein aus der offenen Truhe (hinter dem Deckel, über dem Korpus)
    val viewFaces = faces.map { f -> f to f.pts.map(::view) }
        .sortedBy { (_, v) -> v.sumOf { it.z.toDouble() } / v.size }
    for ((f, v) in viewFaces) {
        val n = (v[1] - v[0]).cross(v[2] - v[0]).norm().let { if (it.z < 0) V3(-it.x, -it.y, -it.z) else it }
        val light = 0.42f + 0.58f * max(0f, n.dot(LIGHT))
        val base = if (f.glow) lerp(Inside, Color(0xFFFFE082), glow) else f.color
        val c = if (f.glow) base else lerp(Color.Black, base, light.coerceIn(0f, 1.1f).coerceAtMost(1f))
        val path = Path().apply {
            val p0 = project(v[0]); moveTo(p0.x, p0.y)
            for (k in 1 until v.size) { val p = project(v[k]); lineTo(p.x, p.y) }
            close()
        }
        drawPath(path, c)
        drawPath(path, Color.Black.copy(alpha = 0.12f), style = Stroke(1.2f))
    }

    // Goldener Lichtstrahl nach oben
    if (glow > 0.01f) {
        val top = project(view(V3(0f, hb, 0f)))
        drawCircle(
            Brush.radialGradient(listOf(Color(0xAAFFF3B0), Color(0x00FFF3B0)), Offset(top.x, top.y - scale * 0.25f), scale * 0.6f * glow),
            scale * 0.6f * glow, Offset(top.x, top.y - scale * 0.25f),
        )
        for (i in 0 until 7) {
            val a = -PI / 2 + (i - 3) * 0.2
            val len = scale * (0.55f + 0.15f * (i % 2)) * glow
            drawLine(Color(0x66FFF59D), top, Offset(top.x + cos(a).toFloat() * len, top.y + sin(a).toFloat() * len), strokeWidth = scale * 0.05f)
        }
    }
}


/** Bildschirm-Y der vorderen Oberkante (Truhe frontal, yaw = 0) – dort taucht der Freund auf. */
fun chestRimY(width: Float, height: Float): Float {
    val hb = 1.05f; val d = 1.3f
    val pitch = 22f * (PI / 180f).toFloat()
    val py = hb - 0.75f; val pz = d / 2
    val y2 = py * cos(pitch) - pz * sin(pitch)
    val z2 = py * sin(pitch) + pz * cos(pitch)
    val camDist = 6.5f
    val scale = minOf(width, height) * 0.62f
    val f = camDist / (camDist - z2)
    return height * 0.55f - y2 * f * scale / 2.2f
}
