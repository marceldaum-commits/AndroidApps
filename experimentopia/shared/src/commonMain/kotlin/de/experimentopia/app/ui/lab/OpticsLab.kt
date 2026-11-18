package de.experimentopia.app.ui.lab

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.experimentopia.app.ui.common.*
import de.experimentopia.app.ui.fx.*
import de.experimentopia.app.ui.theme.E
import de.experimentopia.core.model.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

private val Laser = Color(0xFFFF3B5C)
private val Beam = Color(0xFFFFF2A8)

/** Einfache Hasenfigur (auch als Schatten), [c] = Mitte, [hh] = Gesamthöhe in Pixeln */
private fun DrawScope.bunny(c: Offset, hh: Float, color: Color) {
    val bodyH = hh * 0.48f; val bodyW = hh * 0.5f
    drawOval(color, Offset(c.x - bodyW / 2, c.y + hh / 2 - bodyH), Size(bodyW, bodyH))
    val headR = hh * 0.17f; val headC = Offset(c.x + hh * 0.06f, c.y + hh / 2 - bodyH - headR * 0.7f)
    drawCircle(color, headR, headC)
    val earW = hh * 0.1f; val earH = hh * 0.34f
    drawOval(color, Offset(headC.x - headR * 0.75f, headC.y - headR - earH * 0.8f), Size(earW, earH))
    drawOval(color, Offset(headC.x + headR * 0.1f, headC.y - headR - earH * 0.85f), Size(earW, earH))
    drawCircle(color, hh * 0.07f, Offset(c.x - bodyW / 2, c.y + hh / 2 - bodyH * 0.5f))
}

@Composable
fun ShadowLab(s: ShadowSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val tm = rememberTextMeasurer()
    var obj by remember { mutableDoubleStateOf(s.objCm) }
    var wall by remember { mutableDoubleStateOf(s.wallCm) }
    var lamp by remember { mutableDoubleStateOf(wall * 0.7) }
    val a = lamp.coerceIn(2.0, wall - 2.0)
    val shadow = Optics.shadow(obj, a, wall)
    val target = s.targetCm
    val hit = target != null && abs(shadow - target) <= 1.0
    LaunchedEffect(hit) { if (hit) sfx?.play(Sound.STAR) }
    GoalWhen(!free && hit, onGoal)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(270.dp).testTag("schattenbuehne")) {
            val w = size.width; val h = size.height
            drawRoundRect(Color(0xFF0B0820), Offset.Zero, size, CornerRadius(22f * density))
            // Seitenansicht links: Lampe – Figur – Wand
            val sideW = w * 0.6f
            val x0 = sideW * 0.08f; val xw = sideW * 0.95f; val cy = h * 0.5f
            val k = (xw - x0) / wall.toFloat()
            val vs = h * 0.8f / maxOf(60.0, (target ?: 0.0) * 1.2).toFloat()
            val xf = x0 + (a * k).toFloat()
            val half = (obj / 2 * vs).toFloat(); val sh = (shadow / 2 * vs).toFloat()
            clipRect(0f, 0f, sideW, h) {
                // Lichtkegel
                drawPath(Path().apply { moveTo(x0, cy); lineTo(xw, cy - h); lineTo(xw, cy + h); close() }, Beam.copy(alpha = 0.10f))
                // Schattenraum hinter der Figur
                drawPath(Path().apply { moveTo(xf, cy - half); lineTo(xw, cy - sh); lineTo(xw, cy + sh); lineTo(xf, cy + half); close() }, Color.Black.copy(alpha = 0.55f))
                drawLine(Beam.copy(alpha = 0.6f), Offset(x0, cy), Offset(xw, cy - sh), 1.5f * density)
                drawLine(Beam.copy(alpha = 0.6f), Offset(x0, cy), Offset(xw, cy + sh), 1.5f * density)
            }
            drawRect(Color(0xFFE9E0C8), Offset(xw, h * 0.04f), Size(6f * density, h * 0.92f))
            label(tm, "💡", Offset(x0, cy), 22.sp)
            bunny(Offset(xf, cy), half * 2, E.Orange)
            label(tm, "${Fmt.num(a, 0)} cm", Offset((x0 + xf) / 2, h - 16f * density), 11.sp, E.Sun, true)
            drawLine(E.Sun.copy(alpha = 0.6f), Offset(x0, h - 28f * density), Offset(xf, h - 28f * density), 2f * density, StrokeCap.Round)
            // Wand von vorn rechts: Schatten als Hase
            val wx = sideW + 8f * density; val ww = w - wx - 8f * density
            drawRoundRect(Brush.radialGradient(listOf(Color(0xFFFFF6D8), Color(0xFFCDBF95)), Offset(wx + ww / 2, h / 2), ww), Offset(wx, h * 0.04f), Size(ww, h * 0.92f), CornerRadius(12f * density))
            val scaleW = minOf(1f, (h * 0.86f) / (shadow * vs).toFloat())
            clipRect(wx, h * 0.04f, wx + ww, h * 0.96f) {
                target?.let { t -> val th = (t * vs).toFloat() * scaleW; drawRect(if (hit) E.Green else E.Pink, Offset(wx + ww / 2 - th * 0.3f, h / 2 - th / 2), Size(th * 0.6f, th), style = Stroke(2f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))) }
                bunny(Offset(wx + ww / 2, h / 2), (shadow * vs).toFloat() * scaleW, Color(0xFF231C33))
            }
            label(tm, "Wand", Offset(wx + ww / 2, h * 0.09f), 11.sp, Color(0xFF5A4A30), true)
            label(tm, "${Fmt.num(shadow, 1)} cm", Offset(wx + ww / 2, h * 0.91f), 13.sp, Color(0xFF3A2A10), true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Readout("Figur", Fmt.unit(obj, "cm", 1), E.Orange, Modifier.weight(1f))
            Readout("Schatten", Fmt.unit(shadow, "cm", 1), E.Mint, Modifier.weight(1f))
            target?.let { Readout("Ziel", "${Fmt.num(it, 0)} cm ± 1", E.Sun, Modifier.weight(1f)) }
        }
        ValueSlider("💡 Lampe ↔ Figur", a.toFloat(), 2f..(wall.toFloat() - 2f), "${Fmt.num(a, 0)} cm", { lamp = roundI(it).toDouble() }, color = E.Sun)
        Text(when {
            target == null -> "Schatten = Figur · ${Fmt.num(wall, 0)} cm ÷ ${Fmt.num(a, 0)} cm = ${Fmt.num(shadow, 1)} cm"
            hit -> "🎯 ${Fmt.num(shadow, 1)} cm – genau richtig!"
            shadow < target -> "Noch zu klein – Lampe näher an die Figur."
            else -> "Zu groß – Lampe weiter weg von der Figur."
        }, color = E.Text, fontSize = 14.sp)
        if (free) Panel(border = E.Sun) {
            Text("Eigene Werte", fontWeight = FontWeight.Bold, color = E.Sun)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Figur", "cm", 1.0, 50.0, obj, { obj = it }, Modifier.weight(1f))
                NumberField("Lampe bis Wand", "cm", 20.0, 500.0, wall, { wall = it; lamp = lamp.coerceAtMost(it - 2) }, Modifier.weight(1f))
            }
        }
    }
}

private fun dir(deg: Double) = Offset(cos(toRad(deg)).toFloat(), -sin(toRad(deg)).toFloat())

@Composable
fun MirrorLab(s: MirrorSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val view = LocalBuzzer.current; val vib = LocalVibration.current
    val tm = rememberTextMeasurer()
    var mirror by remember { mutableDoubleStateOf(10.0) }
    var beam by remember { mutableDoubleStateOf(s.beamDeg) }
    val out = Optics.reflectDir(beam, mirror)
    val alpha = Optics.incidence(beam, mirror)
    val hit = remember { mutableStateListOf<Int>() }
    val now = s.targets.indexOfFirst { Optics.angleDiff(it, out) <= 2.0 }
    LaunchedEffect(now) { if (now >= 0 && now !in hit) { hit += now; sfx?.play(Sound.STAR); view.buzz(Buzz.HIT, vib) } }
    GoalWhen(!free && s.targets.isNotEmpty() && hit.size == s.targets.size, onGoal)
    val showAngles = s.showAngles || free
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(290.dp).testTag("spiegel")) {
            val w = size.width; val h = size.height
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF0E0A26), Color(0xFF1C1440))), Offset.Zero, size, CornerRadius(22f * density))
            val c = Offset(w * 0.5f, h * 0.7f); val r = minOf(w * 0.46f, h * 0.62f)
            // Sterne
            s.targets.forEachIndexed { i, t ->
                val p = c + dir(t) * (r * 0.95f)
                if (i == now) drawCircle(E.Sun.copy(alpha = 0.35f), 22f * density, p)
                label(tm, if (i in hit) "🌟" else "⭐", p, 24.sp)
            }
            // Laser
            val src = c - dir(beam) * r
            clipRect(0f, 0f, w, h) {
                drawLine(Laser.copy(alpha = 0.35f), src, c, 9f * density, StrokeCap.Round)
                drawLine(Laser, src, c, 3f * density, StrokeCap.Round)
                val end = c + dir(out) * (r * 1.4f)
                drawLine(Laser.copy(alpha = 0.35f), c, end, 9f * density, StrokeCap.Round)
                drawLine(Laser, c, end, 3f * density, StrokeCap.Round)
            }
            drawCircle(Color(0xFF4A4A5A), 10f * density, src); drawCircle(Laser, 4f * density, src)
            // Spiegel
            val md = dir(mirror) * (r * 0.32f)
            drawLine(Color(0xFF8C8FA0), c - md, c + md, 10f * density, StrokeCap.Round)
            drawLine(Color(0xFFE8F4FF), c - md, c + md, 5f * density, StrokeCap.Round)
            // Lot auf der Seite, von der der Laser kommt
            var n = mirror + 90
            val toSrc = -dir(beam)
            if (dir(n).x * toSrc.x + dir(n).y * toSrc.y < 0) n += 180
            if (showAngles) {
                drawLine(E.Muted, c, c + dir(n) * (r * 0.55f), 2f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                label(tm, "Lot", c + dir(n) * (r * 0.62f), 11.sp, E.Muted)
                // Beschriftung außen neben den Strahlen (nicht zwischen Lot und Strahl)
                val sa = if (Optics.angleDiff(n + 1, beam + 180) < Optics.angleDiff(n - 1, beam + 180)) 1 else -1
                label(tm, "α = ${Fmt.num(alpha, 0)}°", c + dir(beam + 180 + sa * 18) * (r * 0.42f), 12.sp, Laser, true)
                label(tm, "β = ${Fmt.num(Optics.reflectAngle(alpha), 0)}°", c + dir(out - sa * 18) * (r * 0.42f), 12.sp, E.Mint, true)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (showAngles) {
                Readout("Einfall α", "${Fmt.num(alpha, 0)}°", Laser, Modifier.weight(1f))
                Readout("Reflexion β", "${Fmt.num(Optics.reflectAngle(alpha), 0)}°", E.Mint, Modifier.weight(1f))
            }
            if (s.targets.isNotEmpty()) Readout("Sterne", "${hit.size} von ${s.targets.size}", E.Sun, Modifier.weight(1f))
        }
        ValueSlider("↩️ Spiegel drehen", mirror.toFloat(), -45f..90f, "${Fmt.num(mirror, 0)}°", { mirror = roundI(it).toDouble() }, color = E.Sky)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton({ mirror = (mirror - 1).coerceAtLeast(-45.0) }, Modifier.weight(1f), color = E.Sky) { Text("↺ 1° zurück", fontWeight = FontWeight.Bold) }
            GlowButton({ mirror = (mirror + 1).coerceAtMost(90.0) }, Modifier.weight(1f), color = E.Sky) { Text("1° weiter ↻", fontWeight = FontWeight.Bold) }
        }
        if (free) Panel(border = E.Sun) {
            Text("Eigene Werte", fontWeight = FontWeight.Bold, color = E.Sun)
            ValueSlider("🔴 Laser-Richtung", beam.toFloat(), -85f..-5f, "${Fmt.num(-beam, 0)}° nach unten", { beam = roundI(it).toDouble() }, color = Laser)
            NumberField("Spiegel", "°", -45.0, 90.0, mirror, { mirror = it })
        }
    }
}

private fun Medium.tint() = when (this) {
    Medium.LUFT -> Color(0xFF141034); Medium.WASSER -> Color(0xFF1C4C8C); Medium.GLAS -> Color(0xFF2C6C78); Medium.DIAMANT -> Color(0xFF6C6C9C)
}

@Composable
fun RefractLab(s: RefractSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val view = LocalBuzzer.current; val vib = LocalVibration.current
    val tm = rememberTextMeasurer()
    var top by remember { mutableStateOf(s.top) }
    var bottom by remember { mutableStateOf(s.bottom) }
    var angle by remember { mutableDoubleStateOf(20.0) }
    val beta = Optics.refract(angle, top.n, bottom.n)
    val crit = Optics.critical(top.n, bottom.n)
    val total = beta == null
    var foundTotal by remember { mutableStateOf(false) }
    LaunchedEffect(total) { if (total) { foundTotal = true; sfx?.play(Sound.DING); view.buzz(Buzz.HIT, vib) } }
    val tgt = s.targetDeg
    val hit = s.goal == RefractGoal.HIT && beta != null && tgt != null && abs(beta - tgt) <= 1.5
    LaunchedEffect(hit) { if (hit) sfx?.play(Sound.STAR) }
    GoalWhen(!free && (hit || (s.goal == RefractGoal.TOTAL && total)), onGoal)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(290.dp).testTag("lichtknick")) {
            val w = size.width; val h = size.height
            val p = Offset(w * 0.5f, h * 0.48f)
            drawRoundRect(top.tint(), Offset.Zero, size, CornerRadius(22f * density))
            drawRoundRect(bottom.tint(), Offset(0f, p.y), Size(w, h - p.y), CornerRadius(22f * density))
            drawRect(bottom.tint(), Offset(0f, p.y), Size(w, 24f * density))
            drawLine(Color.White.copy(alpha = 0.5f), Offset(0f, p.y), Offset(w, p.y), 2f * density)
            label(tm, "${top.emoji} ${top.title} (n = ${Fmt.num(top.n)})", Offset(w * 0.78f, 16f * density), 11.sp, E.Text, true)
            label(tm, "${bottom.emoji} ${bottom.title} (n = ${Fmt.num(bottom.n)})", Offset(w * 0.22f, h - 16f * density), 11.sp, E.Text, true)
            drawLine(E.Muted, Offset(p.x, p.y - h * 0.42f), Offset(p.x, p.y + h * 0.42f), 2f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
            val a = toRad(angle).toFloat(); val l = h * 0.6f
            val src = p + Offset(-sin(a), -cos(a)) * l
            // Münze als Ziel
            s.targetDeg?.let { t ->
                val cp = Offset(p.x + (h * 0.42f) * tan(toRad(t)).toFloat(), p.y + h * 0.42f)
                if (hit) drawCircle(E.Sun.copy(alpha = 0.35f), 22f * density, cp)
                drawOval(Color(0xFFB8860B), Offset(cp.x - 14f * density, cp.y - 5f * density), Size(28f * density, 12f * density))
                drawOval(Color(0xFFFFD84D), Offset(cp.x - 14f * density, cp.y - 8f * density), Size(28f * density, 12f * density))
            }
            clipRect(0f, 0f, w, h) {
                drawLine(Laser.copy(alpha = 0.35f), src, p, 9f * density, StrokeCap.Round)
                drawLine(Laser, src, p, 3f * density, StrokeCap.Round)
                // Reflexion: schwach – bei Totalreflexion voll
                val refl = p + Offset(sin(a), -cos(a)) * l
                drawLine(Laser.copy(alpha = if (total) 1f else 0.25f), p, refl, (if (total) 3f else 2f) * density, StrokeCap.Round)
                if (total) drawLine(Laser.copy(alpha = 0.35f), p, refl, 9f * density, StrokeCap.Round)
                if (beta != null) {
                    val b = toRad(beta).toFloat()
                    val end = p + Offset(sin(b), cos(b)) * l
                    drawLine(Laser.copy(alpha = 0.35f), p, end, 9f * density, StrokeCap.Round)
                    drawLine(Laser, p, end, 3f * density, StrokeCap.Round)
                    label(tm, "β = ${Fmt.num(beta, 1)}°", p + Offset(sin(b) + 0.25f, cos(b)) * (h * 0.22f), 12.sp, E.Mint, true)
                }
            }
            label(tm, "α = ${Fmt.num(angle, 0)}°", p + Offset(-sin(a) - 0.25f, -cos(a)) * (h * 0.22f), 12.sp, E.Sun, true)
            if (total) label(tm, "Totalreflexion!", Offset(w * 0.75f, p.y + h * 0.2f), 16.sp, E.Sun, true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Readout("Einfall α", "${Fmt.num(angle, 0)}°", E.Sun, Modifier.weight(1f))
            Readout("Brechung β", beta?.let { "${Fmt.num(it, 1)}°" } ?: "kein Licht", E.Mint, Modifier.weight(1f))
            if (tgt != null) Readout("Ziel", "${Fmt.num(tgt, 0)}° ± 1,5", E.Pink, Modifier.weight(1f))
            else if (crit != null && (foundTotal || free)) Readout("Grenzwinkel", "${Fmt.num(crit, 1)}°", E.Pink, Modifier.weight(1f))
        }
        ValueSlider("🔴 Einfallswinkel", angle.toFloat(), 0f..85f, "${Fmt.num(angle, 0)}°", { angle = roundI(it).toDouble() }, color = E.Sun)
        Text(when {
            hit -> "🎯 Volltreffer – die Münze leuchtet!"
            s.goal == RefractGoal.HIT && beta != null && tgt != null -> if (beta < tgt) "Noch zu steil – Winkel größer machen." else "Zu flach – Winkel kleiner machen."
            total -> "Kein Licht kommt mehr heraus – alles wird zurückgespiegelt."
            top.n < bottom.n -> "Licht knickt zum Lot hin: β ist kleiner als α."
            top.n > bottom.n -> "Licht knickt vom Lot weg: β ist größer als α."
            else -> "Gleicher Stoff – kein Knick."
        }, color = E.Text, fontSize = 14.sp)
        Text("Brechungsgesetz: n₁ · sin α = n₂ · sin β", color = E.Muted, fontSize = 12.sp)
        if (free) Panel(border = E.Sun) {
            Text("Eigene Werte", fontWeight = FontWeight.Bold, color = E.Sun)
            Text("Oben:", color = E.Muted, fontSize = 12.sp)
            ChipRow(Medium.entries, top, { "${it.emoji} ${it.title}" }, { top = it })
            Text("Unten:", color = E.Muted, fontSize = 12.sp)
            ChipRow(Medium.entries, bottom, { "${it.emoji} ${it.title}" }, { bottom = it })
            NumberField("Einfallswinkel", "°", 0.0, 89.0, angle, { angle = it })
        }
    }
}

@Composable
fun LensLab(s: LensSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val view = LocalBuzzer.current; val vib = LocalVibration.current
    val tm = rememberTextMeasurer()
    var f by remember { mutableDoubleStateOf(s.f) }
    var g by remember { mutableDoubleStateOf(s.g ?: 30.0) }
    var screen by remember { mutableDoubleStateOf(45.0) }
    val b = Optics.imageDist(f, g)
    val mag = Optics.magnification(f, g)
    val sharp = b != null && abs(screen - b) <= 0.5
    val want = s.mag
    val sized = sharp && want != null && mag != null && abs(mag - want) <= 0.05
    val loupe = g < f
    LaunchedEffect(sharp) { if (sharp) { sfx?.play(Sound.DING); view.buzz(Buzz.TICK, vib) } }
    LaunchedEffect(loupe) { if (loupe) sfx?.play(Sound.STAR) }
    GoalWhen(!free && when (s.goal) { LensGoal.SHARP -> sharp; LensGoal.SIZE -> sized; LensGoal.MAGNIFIER -> loupe; LensGoal.FREE -> false }, onGoal)
    val moveObject = s.g == null || free
    val maxG = 50.0; val maxS = 70.0
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(260.dp).testTag("optische_bank")) {
            val w = size.width; val h = size.height
            drawRoundRect(Color(0xFF0B0820), Offset.Zero, size, CornerRadius(22f * density))
            val axis = h * 0.55f
            val xl = w * 0.4f
            val k = minOf((xl - 14f * density) / maxG.toFloat(), (w - xl - 14f * density) / maxS.toFloat())
            val objH = h * 0.18f
            drawLine(E.Line, Offset(8f * density, axis), Offset(w - 8f * density, axis), 2f * density)
            // Brennpunkte
            for (sg in listOf(-1, 1)) { val fx = xl + sg * (f * k).toFloat(); drawCircle(E.Sun, 4f * density, Offset(fx, axis)); label(tm, "F", Offset(fx, axis + 14f * density), 11.sp, E.Sun, true) }
            // Kerze
            val xo = xl - (g * k).toFloat()
            drawRect(Color(0xFFF3E9D2), Offset(xo - 4f * density, axis - objH * 0.8f), Size(8f * density, objH * 0.8f))
            drawOval(Brush.radialGradient(listOf(Color.White, E.Orange), Offset(xo, axis - objH * 0.9f), objH * 0.2f), Offset(xo - objH * 0.08f, axis - objH * 1.05f), Size(objH * 0.16f, objH * 0.3f))
            val tip = Offset(xo, axis - objH)
            // Schirm
            val xs = xl + (screen * k).toFloat()
            drawRect(Color(0xFFE9E0C8), Offset(xs, h * 0.08f), Size(5f * density, h * 0.84f))
            clipRect(0f, 0f, w, h) {
                val ray = Beam.copy(alpha = 0.75f); val sw = 1.6f * density
                // 1) parallel zur Achse, dann durch F'
                val pl = Offset(xl, tip.y)
                drawLine(ray, tip, pl, sw)
                val fR = Offset(xl + (f * k).toFloat(), axis)
                val d1 = fR - pl
                drawLine(ray, pl, pl + d1 * (((xs - xl) / d1.x).coerceAtLeast(0.01f)), sw)
                // 2) durch die Mitte
                val d2 = Offset(xl, axis) - tip
                drawLine(ray, tip, tip + d2 * ((xs - tip.x) / d2.x), sw)
                // 3) durch F (vorne), dann parallel – nur, wenn die Kerze außerhalb von F steht
                if (!loupe) {
                    val fL = Offset(xl - (f * k).toFloat(), axis)
                    val d3 = fL - tip
                    val pl3 = tip + d3 * ((xl - tip.x) / d3.x)
                    drawLine(ray, tip, pl3, sw); drawLine(ray, pl3, Offset(xs, pl3.y), sw)
                } else {
                    // Lupe: Strahlen rückwärts verlängert → aufrechtes, großes Bild
                    val vm = Optics.virtualMag(f, g) ?: 1.0
                    val vb = f * g / (f - g)
                    val vx = xl - (vb * k).toFloat()
                    val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    drawLine(E.Sky.copy(alpha = 0.7f), pl, pl - d1 * 4f, sw, pathEffect = dash)
                    drawLine(E.Sky.copy(alpha = 0.7f), tip, tip - d2 * 4f, sw, pathEffect = dash)
                    val vh = (objH * vm).toFloat()
                    drawRect(E.Sky.copy(alpha = 0.35f), Offset(vx - 4f * density * vm.toFloat(), axis - vh * 0.8f), Size(8f * density * vm.toFloat(), vh * 0.8f))
                    drawCircle(E.Sky.copy(alpha = 0.6f), 5f * density * vm.toFloat(), Offset(vx, axis - vh * 0.9f))
                    label(tm, "Lupenbild ×${Fmt.num(vm, 1)}", Offset(vx.coerceAtLeast(50f * density), (axis - vh - 12f * density).coerceAtLeast(14f * density)), 11.sp, E.Sky, true)
                }
            }
            // Linse
            drawOval(Color(0x667CC8FF), Offset(xl - 7f * density, h * 0.1f), Size(14f * density, h * 0.8f))
            drawOval(E.Sky, Offset(xl - 7f * density, h * 0.1f), Size(14f * density, h * 0.8f), style = Stroke(2f * density))
            // Bild auf dem Schirm (umgedreht, unscharf je nach Abstand)
            if (b != null) {
                val imgTop = axis + (objH * screen / g).toFloat()
                val blur = (abs(screen - b) / b * h * 0.25).toFloat()
                val alpha = (1f / (1f + blur / (6f * density))).coerceIn(0.12f, 1f)
                drawCircle(E.Orange.copy(alpha = 0.25f * alpha), (6f * density + blur), Offset(xs, imgTop))
                drawLine(Color(0xFFFFE9B0).copy(alpha = alpha), Offset(xs - 6f * density, axis), Offset(xs - 6f * density, imgTop), (5f * density + blur * 0.5f), StrokeCap.Round)
                drawCircle(E.Orange.copy(alpha = alpha), 4f * density + blur * 0.4f, Offset(xs - 6f * density, imgTop))
                if (sharp) label(tm, "scharf! ✨", Offset((xs - 40f * density).coerceAtLeast(40f * density), h * 0.95f - 6f * density), 12.sp, E.Green, true)
            }
            label(tm, "g = ${Fmt.num(g, 1)} cm", Offset((xo + xl) / 2, axis + 30f * density), 10.sp, E.Orange, true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Readout("Brennweite f", "${Fmt.num(f, 1)} cm", E.Sun, Modifier.weight(1f))
            Readout("Bildweite b", b?.let { "${Fmt.num(it, 1)} cm" } ?: "kein Bild", E.Mint, Modifier.weight(1f))
            Readout("Bildgröße", mag?.let { "×${Fmt.num(it, 2)}" } ?: Optics.virtualMag(f, g)?.let { "Lupe ×${Fmt.num(it, 1)}" } ?: "–", E.Pink, Modifier.weight(1f))
        }
        if (moveObject) ValueSlider("🕯️ Kerze ↔ Linse", g.toFloat(), 3f..maxG.toFloat(), "${Fmt.num(g, 1)} cm", { g = roundI(it * 2) / 2.0 }, color = E.Orange)
        ValueSlider("⬜ Linse ↔ Schirm", screen.toFloat(), 5f..maxS.toFloat(), "${Fmt.num(screen, 1)} cm", { screen = roundI(it * 2) / 2.0 }, color = E.Sky)
        Text(when {
            loupe -> "🔍 Kerze innerhalb der Brennweite: kein Bild auf dem Schirm – durch die Linse sieht man sie groß und aufrecht."
            b == null -> "Kerze genau im Brennpunkt: Die Strahlen laufen parallel – nirgends ein Bild."
            s.goal == LensGoal.SIZE && sharp && !sized -> "Scharf, aber ×${Fmt.num(mag!!, 2)} – gesucht ist ×${Fmt.num(s.mag!!, 0)}. Kerze verschieben und neu scharf stellen."
            sharp -> "✨ Scharf! Das Bild steht auf dem Kopf."
            screen < b -> "Unscharf – Schirm weiter weg schieben."
            else -> "Unscharf – Schirm näher an die Linse."
        }, color = E.Text, fontSize = 14.sp)
        Text("Linsengleichung: 1/f = 1/g + 1/b · Maßstab B/G = b/g", color = E.Muted, fontSize = 12.sp)
        if (free) Panel(border = E.Sun) {
            Text("Eigene Werte", fontWeight = FontWeight.Bold, color = E.Sun)
            NumberField("Brennweite", "cm", 2.0, 30.0, f, { f = it })
        }
    }
}
