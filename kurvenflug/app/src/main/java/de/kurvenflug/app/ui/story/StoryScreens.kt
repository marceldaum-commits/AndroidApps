package de.kurvenflug.app.ui.story

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import de.kurvenflug.app.ui.fx.*
import de.kurvenflug.app.ui.theme.Q
import de.kurvenflug.app.ui.volt.Volt
import de.kurvenflug.app.ui.volt.VoltMood
import de.kurvenflug.core.model.*
import kotlin.math.sin

private val planetColors = listOf(Q.Cyan, Q.Pink, Q.Yellow, Q.Green, Q.Violet, Q.Orange)

/**
 * Sternenkarte: jedes Kapitel ein Planet auf einem geschwungenen Pfad (nach Klassenstufe sortiert).
 * Geschaffte Wegstücke leuchten, Volts Rakete schwebt beim aktuellen Planeten, Planeten der eigenen Klasse strahlen.
 * Antippen öffnet die Level des Kapitels.
 */
@Composable
fun GalaxyMap(
    chapters: List<Chapter>, allChapters: List<Chapter>, stars: Map<String, Int>, grade: Int?, current: Int?,
    onLevel: (Level) -> Unit, bossStars: Map<Int, Int>, onBoss: ((Int) -> Unit)?,
) {
    var open by rememberSaveable { mutableStateOf<Int?>(null) }
    val sfx = LocalSfx.current
    val rowH = 118.dp
    val time by rememberInfiniteTransition(label = "map").animateFloat(0f, 1000f, infiniteRepeatable(tween(1_000_000, easing = LinearEasing)), label = "t")
    BoxWithConstraints(Modifier.fillMaxWidth().height(rowH * chapters.size + 24.dp)) {
        val w = maxWidth
        fun x(i: Int) = w * (0.5f + 0.3f * sin(i * 1.15f + 0.4f))
        fun y(i: Int) = rowH * i + rowH / 2
        val done = chapters.map { c -> c.levels.count { (stars[it.id] ?: 0) > 0 } }
        Canvas(Modifier.fillMaxSize()) {
            // Pfad zwischen den Planeten
            for (i in 0 until chapters.size - 1) {
                val a = Offset(x(i).toPx(), y(i).toPx()); val b = Offset(x(i + 1).toPx(), y(i + 1).toPx())
                val lit = done[i] == chapters[i].levels.size
                val path = Path().apply { moveTo(a.x, a.y); cubicTo(a.x, (a.y + b.y) / 2, b.x, (a.y + b.y) / 2, b.x, b.y) }
                drawPath(path, if (lit) Q.Cyan.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.15f),
                    style = Stroke(if (lit) 6f else 4f, pathEffect = if (lit) null else PathEffect.dashPathEffect(floatArrayOf(14f, 12f), -time * 0.5f)))
            }
        }
        chapters.forEachIndexed { i, c ->
            val idx = allChapters.indexOf(c)
            val mine = grade != null && grade in c.gradeRange
            Planet(c, idx, stars, mine, Story.found(listOf(c), stars).isNotEmpty(), time + i,
                Modifier.offset(x = x(i) - 60.dp, y = y(i) - 46.dp)) { sfx?.play(Sound.TAP); open = c.number }
            if (c.number == current) {
                val bob = 4f * sin(time * 3f)
                Text("🚀", fontSize = 28.sp, modifier = Modifier.offset(x = x(i) + 34.dp, y = y(i) - 44.dp + bob.dp))
            }
        }
    }
    open?.let { n ->
        val c = allChapters.first { it.number == n }
        Dialog(onDismissRequest = { open = null }) {
            Column(Modifier.clip(RoundedCornerShape(24.dp)).background(Q.Space).border(2.dp, Q.Cyan, RoundedCornerShape(24.dp))
                .verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Story.part(n)?.let { p ->
                    val got = Story.found(listOf(c), stars).isNotEmpty()
                    Text(if (got) "${p.emoji} ${p.name} gefunden ✅" else "Hier treibt: ${p.emoji} ${p.name}",
                        color = if (got) Q.Green else Q.Yellow, fontWeight = FontWeight.Bold)
                }
                de.kurvenflug.app.ui.flight.ChapterCard(c, allChapters.indexOf(c), stars, { open = null; onLevel(it) },
                    bossStars[c.number] ?: 0, onBoss?.let { b -> { open = null; b(c.number) } })
                TextButton(onClick = { open = null }, modifier = Modifier.align(Alignment.End)) { Text("Schließen", color = Q.Muted) }
            }
        }
    }
}

@Composable
private fun Planet(c: Chapter, index: Int, stars: Map<String, Int>, mine: Boolean, found: Boolean, time: Float, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val color = planetColors[index % planetColors.size]
    val got = c.levels.sumOf { stars[it.id] ?: 0 }
    val share = got.toFloat() / (c.levels.size * 3)
    Column(modifier.width(120.dp).pressBounce(interaction).clickable(interaction, null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2
                if (mine) drawCircle(color.copy(alpha = 0.18f + 0.1f * sin(time * 2f)), r)
                drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.9f), color.copy(alpha = 0.35f), Q.Space),
                    center = center - Offset(r * 0.25f, r * 0.25f), radius = r * 0.9f), r * 0.62f)
                drawCircle(Color.White.copy(alpha = 0.08f), r * 0.86f, style = Stroke(5.dp.toPx()))
                drawArc(color, -90f, 360f * share, false, topLeft = Offset(r * 0.14f, r * 0.14f), size = Size(r * 1.72f, r * 1.72f),
                    style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
            }
            Text(c.emoji, fontSize = 26.sp)
            if (found) Text("🔧", fontSize = 16.sp, modifier = Modifier.align(Alignment.TopEnd))
        }
        Text(c.title, color = if (mine) Q.Text else Q.Muted, fontWeight = if (mine) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Q.Space.copy(alpha = 0.85f)).padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

/** Karte auf der Startseite: wie weit die Aurora schon ist. */
@Composable
fun AuroraCard(found: Int, total: Int, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val sfx = LocalSfx.current
    Row(
        Modifier.fillMaxWidth().pressBounce(interaction).clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(Q.Orange.copy(alpha = 0.25f), Q.Panel)))
            .border(1.5.dp, Q.Orange.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .clickable(interaction, null) { sfx?.play(Sound.TAP); onClick() }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(52.dp)) { ShipCanvas(found = Story.parts.take(found).map { it.chapter }.toSet(), Modifier.fillMaxSize(), time = 0f) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Volts Schiff ${Story.SHIP}", fontWeight = FontWeight.Black, color = Q.Text)
            Text("$found von $total Bauteilen gefunden", style = MaterialTheme.typography.bodySmall, color = Q.Muted)
        }
        Text("›", color = Q.Orange, fontWeight = FontWeight.Black, fontSize = 22.sp)
    }
}

/** Hangar: die Aurora wächst mit jedem gefundenen Bauteil zusammen. */
@Composable
fun HangarScreen(found: List<Part>, onBack: () -> Unit, gold: List<Part> = emptyList(), onCrew: (() -> Unit)? = null) {
    val time by rememberInfiniteTransition(label = "hangar").animateFloat(0f, 1000f, infiniteRepeatable(tween(1_000_000, easing = LinearEasing)), label = "t")
    val complete = found.size == Story.parts.size
    Box(Modifier.fillMaxSize()) {
        Starfield(seed = 73)
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Q.Text) }
                Text("🛸 Hangar · ${found.size}/${Story.parts.size}", style = MaterialTheme.typography.titleLarge, color = Q.Text)
            }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Volt(if (complete) VoltMood.WOW else VoltMood.HAPPY, size = 56.dp, speech = when {
                    gold.size == Story.parts.size -> "Alles Gold! Die ${Story.SHIP} fliegt mit Warp-Antrieb – du bist ein echter Profi! 🌟"
                    complete && gold.isNotEmpty() -> "Schon ${gold.size} Gold-Teile! Jedes Kapitel mit lauter 3 ⭐ macht ein Teil golden. ✨"
                    complete -> "Die ${Story.SHIP} fliegt! Jetzt machen wir sie golden: Schaff ein Kapitel ganz mit 3 ⭐ – dann glänzt sein Teil. ✨"
                    found.isEmpty() -> "Hier bauen wir die ${Story.SHIP} wieder zusammen. Schaff ein ganzes Kapitel – dann finden wir das erste Teil!"
                    else -> "Schon ${found.size} Teile! Jeder Planet, den du ganz schaffst, bringt eins mehr. 🔧"
                })
                Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(Color(0x55070B20))
                    .border(1.5.dp, Q.Orange.copy(alpha = 0.5f), RoundedCornerShape(24.dp)).padding(24.dp)) {
                    ShipCanvas(found.map { it.chapter }.toSet(), Modifier.fillMaxSize(), time, flames = complete, gold = gold.map { it.chapter }.toSet())
                }
                if (complete) Text("✨ Gold-Teile: ${gold.size}/${Story.parts.size}", color = Q.Yellow, fontWeight = FontWeight.Black)
                if (onCrew != null) NeonButton(onClick = onCrew, color = Q.Pink, modifier = Modifier.fillMaxWidth()) {
                    Text("🆘 Notrufe & Crew", fontWeight = FontWeight.Bold)
                }
                Story.parts.forEach { p ->
                    val got = found.any { it.chapter == p.chapter }
                    val isGold = gold.any { it.chapter == p.chapter }
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (isGold) Q.Yellow.copy(alpha = 0.16f) else if (got) Q.Green.copy(alpha = 0.12f) else Q.Panel).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(if (got) p.emoji else "❔", fontSize = 22.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (got) p.name + (if (isGold) " ✨ Gold" else "") else "???", fontWeight = FontWeight.Bold,
                                color = if (isGold) Q.Yellow else if (got) Q.Text else Q.Muted)
                            val c = Levels.chapters.firstOrNull { it.number == p.chapter }
                            Text(if (got) p.found else "Auf dem Planeten ${c?.emoji ?: ""} ${c?.title ?: ""}",
                                style = MaterialTheme.typography.labelSmall, color = Q.Muted)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Die Aurora als Zeichnung aus 18 Teilen (in der Reihenfolge von [Story.parts]).
 * Gefundene Teile leuchten in Farbe, fehlende sind nur als gestrichelter Umriss zu sehen.
 */
@Composable
fun ShipCanvas(found: Set<Int>, modifier: Modifier, time: Float, flames: Boolean = false, gold: Set<Int> = emptySet()) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        fun o(x: Float, y: Float) = Offset(x * w, y * h)
        fun poly(vararg p: Float) = Path().apply { moveTo(p[0] * w, p[1] * h); for (i in 2 until p.size step 2) lineTo(p[i] * w, p[i + 1] * h); close() }
        val shapes: List<DrawScope.(Color, Boolean) -> Unit> = listOf(
            { c, f -> part(poly(0.5f, 0.02f, 0.38f, 0.2f, 0.62f, 0.2f), c, f) },                       // Nase
            { c, f -> part(poly(0.38f, 0.66f, 0.62f, 0.66f, 0.62f, 0.78f, 0.38f, 0.78f), c, f) },      // Tank-Sektion
            { c, f -> part(poly(0.38f, 0.7f, 0.2f, 0.9f, 0.38f, 0.84f), c, f) },                        // Ruder links
            { c, f -> part(poly(0.38f, 0.4f, 0.62f, 0.4f, 0.62f, 0.54f, 0.38f, 0.54f), c, f) },        // Frachtraum
            { c, f -> part(circle(o(0.5f, 0.47f), 0.035f * w), c, f) },                               // Uhr
            { c, f -> part(circle(o(0.5f, 0.12f), 0.025f * w), c, f) },                               // Scheinwerfer
            { c, f -> part(poly(0.38f, 0.42f, 0.08f, 0.72f, 0.38f, 0.66f), c, f) },                     // linker Flügel
            { c, f -> part(poly(0.38f, 0.54f, 0.62f, 0.54f, 0.62f, 0.66f, 0.38f, 0.66f), c, f) },      // Andockring-Sektion
            { c, f -> part(poly(0.6f, 0.2f, 0.75f, 0.06f, 0.77f, 0.08f, 0.62f, 0.23f), c, f) },         // Antenne
            { c, f -> part(poly(0.62f, 0.42f, 0.92f, 0.72f, 0.62f, 0.66f), c, f) },                     // rechter Flügel
            { c, f -> part(poly(0.4f, 0.78f, 0.44f, 0.78f, 0.3f, 0.97f, 0.26f, 0.97f), c, f); part(poly(0.56f, 0.78f, 0.6f, 0.78f, 0.74f, 0.97f, 0.7f, 0.97f), c, f) }, // Landebeine
            { c, f -> part(poly(0.4f, 0.78f, 0.48f, 0.78f, 0.48f, 0.88f, 0.4f, 0.88f), c, f) },         // Energiespeicher
            { c, f -> part(circle(o(0.5f, 0.28f), 0.06f * w), c, f) },                                // Kristallfenster
            { c, f -> part(poly(0.38f, 0.2f, 0.62f, 0.2f, 0.62f, 0.4f, 0.38f, 0.4f), c, f) },          // Schild-Sektion
            { c, f -> part(circle(o(0.22f, 0.64f), 0.035f * w), c, f) },                              // Zielcomputer
            { c, f -> part(circle(o(0.78f, 0.64f), 0.035f * w), c, f) },                              // Sonar
            { c, f -> part(poly(0.62f, 0.7f, 0.8f, 0.9f, 0.62f, 0.84f), c, f) },                        // Rechenkern (Ruder rechts)
            { c, f -> part(poly(0.52f, 0.78f, 0.6f, 0.78f, 0.6f, 0.88f, 0.52f, 0.88f), c, f) },         // Antrieb
        )
        val warp = gold.size == Story.parts.size
        if (flames) for (x in listOf(0.44f, 0.56f)) {
            val fl = (if (warp) 0.11f else 0.06f) + 0.02f * sin(time * 20f + x * 10f)
            drawPath(poly(x - 0.035f, 0.88f, x + 0.035f, 0.88f, x, 0.88f + fl), if (warp) Q.Cyan else Q.Orange)
        }
        Story.parts.forEachIndexed { i, p ->
            val g = p.chapter in gold
            val color = if (g) Color(0xFFFFD54F) else planetColors[i % planetColors.size]
            shapes.getOrNull(i)?.invoke(this, color, p.chapter in found)
            // Gold funkelt
            if (g && sin(time * 3f + i * 1.3f) > 0.85f) drawCircle(Color.White, 0.012f * w, Offset(w * (0.2f + (i * 0.37f) % 0.6f), h * (0.1f + (i * 0.53f) % 0.8f)))
        }
    }
}

private fun circle(c: Offset, r: Float) = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }

private fun DrawScope.part(path: Path, color: Color, found: Boolean) {
    if (found) {
        drawPath(path, Brush.verticalGradient(listOf(color, color.copy(alpha = 0.6f))))
        drawPath(path, Color.White.copy(alpha = 0.6f), style = Stroke(2f))
    } else drawPath(path, Color.White.copy(alpha = 0.25f), style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))))
}

/** Volt erzählt die Geschichte – einmal beim ersten Start. */
@Composable
fun StoryIntroOverlay(onDone: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val sfx = LocalSfx.current
    LaunchedEffect(page) { sfx?.play(Sound.BEEP, volume = 0.5f) }
    Box(Modifier.fillMaxSize().background(Color(0xEE070B20)).clickable(remember { MutableInteractionSource() }, null) {}, contentAlignment = Alignment.Center) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("🛸 Die Reise der ${Story.SHIP}", style = MaterialTheme.typography.headlineMedium, color = Q.Orange)
            Box(Modifier.size(140.dp)) { ShipCanvas(if (page == 0) emptySet() else Story.parts.take(page * 4).map { it.chapter }.toSet(), Modifier.fillMaxSize(), 0f) }
            AnimatedContent(page, transitionSpec = { (slideInHorizontally { it / 2 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 2 } + fadeOut()) }, label = "story") { p ->
                Volt(if (p == 0) VoltMood.OOPS else if (p == Story.intro.lastIndex) VoltMood.HAPPY else VoltMood.THINK, size = 72.dp, speech = Story.intro[p])
            }
            NeonButton(onClick = { if (page < Story.intro.lastIndex) page++ else onDone() }, color = Q.Orange) {
                Text(if (page < Story.intro.lastIndex) "Weiter ›" else "Klar helfe ich! 🚀", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Feier, wenn ein Kapitel geschafft ist und ein Bauteil zurück ist. */
@Composable
fun PartFoundOverlay(parts: List<Part>, foundTotal: Set<Int>, onDone: () -> Unit, gold: Set<Int> = emptySet(), isGold: Boolean = false) {
    val sfx = LocalSfx.current
    val pop = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) { sfx?.play(Sound.WIN); pop.animateTo(1f, spring(dampingRatio = 0.45f)) }
    val time by rememberInfiniteTransition(label = "found").animateFloat(0f, 1000f, infiniteRepeatable(tween(1_000_000, easing = LinearEasing)), label = "t")
    Box(Modifier.fillMaxSize().background(Color(0xEE070B20)).clickable(remember { MutableInteractionSource() }, null) {}, contentAlignment = Alignment.Center) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(if (isGold) "✨ Gold-Teil!" else "🔧 Bauteil gefunden!", style = MaterialTheme.typography.headlineMedium, color = if (isGold) Q.Yellow else Q.Orange)
            parts.forEach { p ->
                Text(p.emoji, fontSize = 64.sp, modifier = Modifier.graphicsLayerScale(pop.value))
                Text(p.name, style = MaterialTheme.typography.titleLarge, color = Q.Text)
                Volt(VoltMood.DANCE, size = 56.dp, speech = if (isGold) "Alle Level mit 3 ⭐ – mein ${p.name} glänzt jetzt golden!" else p.found)
            }
            Box(Modifier.size(140.dp)) { ShipCanvas(foundTotal, Modifier.fillMaxSize(), time, flames = foundTotal.size == Story.parts.size, gold = gold) }
            Text("${foundTotal.size} von ${Story.parts.size} Teilen", color = Q.Muted)
            NeonButton(onClick = onDone, color = Q.Orange) { Text("Ab in den Hangar! 🛸", fontWeight = FontWeight.Black) }
        }
    }
}

private fun Modifier.graphicsLayerScale(s: Float) = this.then(Modifier.graphicsLayer { scaleX = s; scaleY = s })
