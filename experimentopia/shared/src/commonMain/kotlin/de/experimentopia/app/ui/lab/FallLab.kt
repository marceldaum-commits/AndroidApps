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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.random.Random

private fun planetSky(p: Planet?) = when (p) {
    Planet.MOND -> listOf(Color(0xFF05040C), Color(0xFF14121F)); Planet.MARS -> listOf(Color(0xFF3B1A12), Color(0xFF7A3520))
    Planet.JUPITER -> listOf(Color(0xFF3A2614), Color(0xFF8A5A2C)); Planet.VAKUUM -> listOf(Color(0xFF0E1A2A), Color(0xFF16263C))
    else -> listOf(Color(0xFF1B2A55), Color(0xFF3B5BA0))
}

@Composable
fun FallLab(s: FallSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val view = LocalBuzzer.current; val vib = LocalVibration.current
    val tm = rememberTextMeasurer()
    val now by rememberSeconds()
    var a by remember { mutableStateOf(s.a) }
    var b by remember { mutableStateOf(s.b) }
    var planet by remember { mutableStateOf(s.planets.first()) }
    var height by remember { mutableDoubleStateOf(s.heightM) }
    var ownG by remember { mutableStateOf<Double?>(null) }
    var dropAt by remember { mutableFloatStateOf(-1f) }
    val tried = remember { mutableStateListOf<Planet>() }
    val g = ownG ?: planet.g
    val rho = if (ownG != null) Fall.AIR_RHO else planet.airRho; val air = rho > 0
    val simA = remember(a, planet, height, ownG) { Fall.simulate(a, height, g, rho) }
    val simB = remember(b, planet, height, ownG) { b?.let { Fall.simulate(it, height, g, rho) } }
    val tA = simA.last().t; val tB = simB?.last()?.t
    val maxT = maxOf(tA, tB ?: 0.0)
    // Zeitlupe bei kurzen Stürzen, damit man etwas sieht
    val speed = (maxT / 2.5).coerceIn(0.15, 1.0)
    val simT = if (dropAt < 0) 0.0 else (now - dropAt) * speed
    val landedA = dropAt >= 0 && simT >= tA
    val landedB = dropAt >= 0 && tB != null && simT >= tB
    LaunchedEffect(landedA) { if (landedA) { sfx?.play(Sound.CLANK); view.buzz(Buzz.TICK, vib) } }
    LaunchedEffect(landedB) { if (landedB) sfx?.play(Sound.CLANK, pitch = 1.2f) }
    val allLanded = landedA && (simB == null || landedB)
    LaunchedEffect(allLanded) { if (allLanded && planet !in tried) tried += planet }
    GoalWhen(!free && s.planets.all { it in tried }, onGoal)
    fun yAt(sim: List<Fall.Sample>, t: Double): Double { if (t <= 0) return 0.0; val i = (t / 0.01).toInt(); return sim.getOrNull(i)?.y ?: sim.last().y }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(300.dp).testTag("fallturm")) {
            val w = size.width; val h = size.height
            drawRoundRect(Brush.verticalGradient(planetSky(if (ownG != null) null else planet)), Offset.Zero, size, CornerRadius((22f * density)))
            if (!air || planet == Planet.MOND) { val r = Random(3); repeat(30) { drawCircle(Color.White.copy(alpha = 0.6f), 1.5f, Offset(r.nextFloat() * w, r.nextFloat() * h * 0.7f)) } }
            val top = h * 0.1f; val ground = h * 0.9f
            drawRect(when (planet) { Planet.MOND -> Color(0xFF8A8A92); Planet.MARS -> Color(0xFFB0532C); Planet.JUPITER -> Color(0xFFC08A50); else -> Color(0xFF3E8E4A) }, Offset(0f, ground), Size(w, h - ground))
            if (planet == Planet.VAKUUM && ownG == null) {
                drawRoundRect(Color.White.copy(alpha = 0.12f), Offset(w * 0.2f, top - (20f * density)), Size(w * 0.6f, ground - top + (24f * density)), CornerRadius((30f * density)))
                drawRoundRect(Color.White.copy(alpha = 0.6f), Offset(w * 0.2f, top - (20f * density)), Size(w * 0.6f, ground - top + (24f * density)), CornerRadius((30f * density)), style = Stroke((3f * density)))
                label(tm, "keine Luft", Offset(w * 0.5f, top - (30f * density)), 11.sp, E.Sky)
            }
            // Maßstab
            for (k in 0..4) { val y = top + (ground - top) * k / 4f; drawLine(E.Muted.copy(alpha = 0.5f), Offset(w * 0.06f, y), Offset(w * 0.1f, y), (2f * density)); label(tm, "${Fmt.num(height * k / 4, 1)} m", Offset(w * 0.12f + (18f * density), y), 9.sp, E.Muted) }
            fun obj(f: Faller, sim: List<Fall.Sample>, x: Float, landed: Boolean) {
                val y = yAt(sim, simT)
                val py = top + ((y / height) * (ground - top - (24f * density))).toFloat()
                val sway = if (f == Faller.FEDER && air && dropAt >= 0 && !landed) sin(now * 3f) * (18f * density) else 0f
                label(tm, f.emoji, Offset(x + sway, py), 30.sp)
                if (landed) label(tm, "${Fmt.num(sim.last().t, 2)} s", Offset(x, ground + (16f * density)), 12.sp, Color.White, true)
            }
            obj(a, simA, w * (if (b == null) 0.5f else 0.38f), landedA)
            if (b != null && simB != null) obj(b!!, simB, w * 0.65f, landedB)
            if (dropAt >= 0 && !allLanded) label(tm, "⏱ ${Fmt.num(simT, 2)} s" + if (speed < 1) "  (Zeitlupe)" else "", Offset(w * 0.8f, top), 12.sp, E.Sun, true)
            if (allLanded && b != null) {
                val w1 = if (abs(tA - tB!!) <= 0.02 * maxT) "gleichzeitig! 🤝" else if (tA < tB) "${a.emoji} zuerst!" else "${b!!.emoji} zuerst!"
                label(tm, w1, Offset(w / 2, h * 0.5f), 20.sp, E.Sun, true)
            }
        }
        GlowButton({ dropAt = now; sfx?.play(Sound.WHOOSH) }, Modifier.fillMaxWidth().pulse(dropAt < 0), color = E.Mint) { Text("🖐️ Loslassen!", fontWeight = FontWeight.Bold) }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Readout("${a.emoji} Fallzeit", if (landedA) "${Fmt.num(tA, 2)} s" else "?", E.Mint, Modifier.weight(1f))
            if (b != null) Readout("${b!!.emoji} Fallzeit", if (landedB) "${Fmt.num(tB!!, 2)} s" else "?", E.Pink, Modifier.weight(1f))
            Readout("ohne Luft gerechnet", "${Fmt.num(Fall.timeNoAir(height, g), 2)} s", E.Sun, Modifier.weight(1f))
        }
        if (s.planets.size > 1 || free) ChipRow(if (free) Planet.entries else s.planets, if (ownG == null) planet else null, { "${it.emoji} ${it.title}" + if (it in tried) " ✓" else "" }, { planet = it; ownG = null; dropAt = -1f })
        Text("g = ${Fmt.num(g)} m/s² · ${if (!air) "ohne Luft" else if (rho < 0.5) "sehr dünne Luft" else "mit Luft"} · Höhe ${Fmt.num(height)} m", color = E.Muted, fontSize = 12.sp)
        if (free) Panel(border = E.Sun) {
            Text("Eigene Werte", fontWeight = FontWeight.Bold, color = E.Sun)
            Text("Links:", color = E.Muted, fontSize = 12.sp)
            ChipRow(Faller.entries, a, { "${it.emoji} ${it.title}" }, { a = it; dropAt = -1f })
            Text("Rechts:", color = E.Muted, fontSize = 12.sp)
            ChipRow(listOf<Faller?>(null) + Faller.entries, b, { it?.let { f -> "${f.emoji} ${f.title}" } ?: "nichts" }, { b = it; dropAt = -1f })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Höhe", "m", 0.5, 500.0, height, { height = it; dropAt = -1f }, Modifier.weight(1f))
                NumberField("eigenes g", "m/s²", 0.1, 300.0, g, { ownG = it; dropAt = -1f }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun PendulumLab(s: PendulumSetup, free: Boolean, onGoal: () -> Unit) {
    val sfx = LocalSfx.current
    val tm = rememberTextMeasurer()
    var lengths by remember { mutableStateOf(if (s.lengths.size >= 2) s.lengths.take(2) else listOf(s.lengths.first())) }
    val masses = if (s.lengths.size == 1 && s.massesKg.size >= 2) s.massesKg.take(2) else listOf(s.massesKg.first())
    val pends = if (lengths.size == 2) lengths.map { it to masses.first() } else if (masses.size == 2) masses.map { lengths.first() to it } else listOf(lengths.first() to masses.first())
    var amp by remember { mutableFloatStateOf(s.amplitudeDeg.toFloat()) }
    var g by remember { mutableDoubleStateOf(s.g) }
    // Zustand je Pendel: Winkel und Winkelgeschwindigkeit
    val theta = remember { mutableStateListOf(0.0, 0.0) }
    val omega = remember { mutableStateListOf(0.0, 0.0) }
    var running by remember { mutableStateOf(false) }
    var simT by remember { mutableDoubleStateOf(0.0) }
    val periods = pends.map { (l, _) -> Pendulum.period(l, g, amp.toDouble()) }
    LaunchedEffect(running, pends, g, amp) {
        if (!running) return@LaunchedEffect
        for (i in pends.indices) { theta[i] = toRad(amp.toDouble()); omega[i] = 0.0 }
        simT = 0.0
        var last = -1L
        while (running) withFrameNanos { n ->
            if (last > 0) {
                val dt = ((n - last) / 1e9).coerceAtMost(0.05)
                pends.forEachIndexed { i, (l, _) -> var th = theta[i]; var om = omega[i]; repeat(20) { val (a, b) = Pendulum.step(th, om, l, g, dt / 20); th = a; om = b }; theta[i] = th; omega[i] = om }
                simT += dt
            }
            last = n
        }
    }
    val target = s.targetT
    val tuned = target != null && abs(periods.first() - target) <= target * 0.01
    GoalWhen(!free && running && (if (target != null) tuned && simT > periods.first() else simT > periods.max() * 1.05), onGoal)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(280.dp).testTag("pendel")) {
            val w = size.width; val h = size.height
            drawRoundRect(Color(0xFF120C28), Offset.Zero, size, CornerRadius((22f * density)))
            drawLine(E.Muted, Offset(w * 0.1f, (14f * density)), Offset(w * 0.9f, (14f * density)), (6f * density), StrokeCap.Round)
            val maxL = (pends.maxOf { it.first }).coerceAtLeast(0.5)
            val scale = (h - (60f * density)) / maxL.toFloat()
            pends.forEachIndexed { i, (l, m) ->
                val px = w * (if (pends.size == 1) 0.5f else 0.32f + 0.36f * i)
                val pivot = Offset(px, (14f * density))
                val th = if (running) theta[i] else toRad(amp.toDouble())
                val bob = pivot + Offset((sin(th) * l * scale).toFloat(), (cos(th) * l * scale).toFloat())
                drawArc(E.Line, 90f - amp, 2 * amp, false, Offset(px - (l * scale).toFloat(), 14f - (l * scale).toFloat()), Size(2 * (l * scale).toFloat(), 2 * (l * scale).toFloat()), style = Stroke(2f))
                drawLine(E.Text, pivot, bob, (3f * density))
                val r = (12f * density) + (m * 14).toFloat().coerceAtMost((18f * density))
                drawCircle(Brush.radialGradient(listOf(Color.White, if (i == 0) E.Pink else E.Sky), bob - Offset(r / 3, r / 3), r * 1.5f), r, bob)
                label(tm, "${Fmt.num(l * 100, 0)} cm · ${Fmt.num(m * 1000, 0)} g", Offset(px, h - (14f * density)), 11.sp, E.Text)
            }
            if (running) label(tm, "⏱ ${Fmt.num(simT, 1)} s", Offset(w * 0.85f, (36f * density)), 12.sp, E.Sun, true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            pends.forEachIndexed { i, _ -> Readout("Schwingungsdauer ${if (pends.size > 1) i + 1 else ""}", if (running && simT > periods[i]) "${Fmt.num(periods[i], 2)} s" else "?", if (i == 0) E.Pink else E.Sky, Modifier.weight(1f)) }
            target?.let { Readout("Ziel", "${Fmt.num(it, 2)} s ± 1 %", E.Sun, Modifier.weight(1f)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton({ running = !running; if (running) sfx?.play(Sound.WHOOSH) }, Modifier.weight(1f).pulse(!running), color = E.Mint) { Text(if (running) "⏹ Anhalten" else "👉 Anstoßen", fontWeight = FontWeight.Bold) }
        }
        if (target != null || free) ValueSlider("Pendellänge", lengths.first().toFloat(), 0.1f..2f, "${Fmt.num(lengths.first() * 100, 0)} cm",
            { lengths = listOf(roundI(it * 100) / 100.0) + lengths.drop(1); running = false }, color = E.Pink)
        if (target != null) Text(if (tuned) "🎯 ${Fmt.num(periods.first(), 3)} s – passt!" else "Jetzt: ${Fmt.num(periods.first(), 3)} s – ${if (periods.first() < target) "zu schnell, länger machen" else "zu langsam, kürzer machen"}", color = E.Text, fontSize = 14.sp)
        if (s.g != Planet.ERDE.g) Text("🌕 Hier gilt g = ${Fmt.num(s.g)} m/s²", color = E.Sun, fontSize = 13.sp)
        Text("Eine Schwingung = einmal hin und zurück. Formel für kleine Ausschläge: T = 2π·√(l/g)", color = E.Muted, fontSize = 12.sp)
        if (free) Panel(border = E.Sun) {
            Text("Eigene Werte", fontWeight = FontWeight.Bold, color = E.Sun)
            ValueSlider("Ausschlag", amp, 5f..80f, "${amp.toInt()}°", { amp = it; running = false }, color = E.Sun)
            Text("Bei großen Ausschlägen wird das Pendel etwas langsamer: ${Fmt.num(Pendulum.period(lengths.first(), g, amp.toDouble()), 3)} s statt ${Fmt.num(Pendulum.periodSmall(lengths.first(), g), 3)} s.", color = E.Muted, fontSize = 12.sp)
            NumberField("Länge", "m", 0.05, 20.0, lengths.first(), { lengths = listOf(it); running = false })
            ChipRow(Planet.entries.filter { it != Planet.VAKUUM }, Planet.entries.firstOrNull { it.g == g && it != Planet.VAKUUM }, { "${it.emoji} ${it.title}" }, { g = it.g; running = false })
            NumberField("eigenes g", "m/s²", 0.1, 300.0, g, { g = it; running = false })
        }
    }
}
