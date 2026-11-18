package de.wissensgarten.app.ui.garden

import de.wissensgarten.app.ui.common.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.app.ui.fx.*
import de.wissensgarten.core.model.*
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
private fun Top(title: String, onBack: () -> Unit) {
    Row(Modifier.statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück") }
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}

// ───────────────────────────── Bienenstock ─────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HiveScreen(totalReps: Int, furniture: (String) -> Int, onFurnish: (String, Int) -> Unit, onBack: () -> Unit) {
    BackHandler { onBack() }
    val sfx = LocalSfx.current
    val combs = Hive.combs(totalReps)
    val fill by animateFloatAsState(Hive.combProgress(totalReps), tween(1400, easing = FastOutSlowInEasing), label = "f")
    val t by rememberInfiniteTransition(label = "h").animateFloat(0f, 1f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "t")
    val next = Hive.next(totalReps)
    // Auswahl lokal halten – reagiert sofort, gespeichert wird über onFurnish
    val chosenMap = remember { mutableStateMapOf<String, Int>().apply { Hive.rooms.forEach { put(it.id, furniture(it.id)) } } }
    Column(Modifier.fillMaxSize().background(de.wissensgarten.app.ui.fx.SafeBrush.verticalGradient(listOf(Color(0xFFFFF8E1), Color(0xFFFFECB3))))) {
        Top("🍯 Brummis Bienenstock", onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Wabenfeld: volle Waben golden, aktuelle füllt sich
                Canvas(Modifier.size(150.dp)) {
                    val r = size.width / 8.8f
                    var k = 0
                    for (row in 0 until 4) for (col in 0 until 4) {
                        val cx = r * 1.2f + col * r * 1.75f + (if (row % 2 == 1) r * 0.87f else 0f)
                        val cy = r * 1.2f + row * r * 1.5f
                        val hex = Path().apply {
                            for (s in 0..5) { val a = (PI / 3 * s + PI / 6).toFloat(); val p = Offset(cx + cos(a) * r, cy + sin(a) * r); if (s == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
                            close()
                        }
                        val full = k < combs % 16 || (combs >= 16 && k < 16)
                        val current = k == combs % 16
                        drawPath(hex, Color(0xFFFFE082).copy(alpha = 0.5f))
                        if (full) drawPath(hex, Color(0xFFFFB300))
                        if (current) {
                            drawContext.canvas.save()
                            drawContext.canvas.clipPath(hex)
                            val lvl = cy + r - 2 * r * fill
                            drawRect(Color(0xFFFFC107), Offset(cx - r, lvl + sin(t * 2 * PI.toFloat()) * 2f), androidx.compose.ui.geometry.Size(2 * r, cy + r - lvl + 4f))
                            drawContext.canvas.restore()
                        }
                        drawPath(hex, Color(0xFFE65100), style = Stroke(2.5f))
                        k++
                    }
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("$combs Waben voll", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Jede gegossene Karte füllt Honig ein – 10 Karten = 1 Wabe.", style = MaterialTheme.typography.bodySmall)
                    if (next != null) Text("Noch ${next.needCombs - combs} Waben bis: ${next.emoji} ${next.name}", style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 6.dp))
                    else Text("Alle Räume offen – du bist Honig-Meister! 👑", style = MaterialTheme.typography.labelLarge)
                }
            }
            Brummi(Mood.HAPPY, size = 72.dp, speech = "Willkommen in meinem Zuhause! Richte die Räume ein, wie du magst. 🐝")
            Hive.rooms.forEach { room ->
                val open = combs >= room.needCombs
                val chosen = chosenMap[room.id] ?: -1
                ElevatedCard(Modifier.fillMaxWidth().graphicsLayer { alpha = if (open) 1f else 0.55f }) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val bob = if (open) sin((t + room.needCombs * 0.1f) * 2 * PI.toFloat()) * 3f else 0f
                            Text(if (open) room.emoji else "🔒", fontSize = 30.sp, modifier = Modifier.graphicsLayer { translationY = bob })
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(room.name, style = MaterialTheme.typography.titleMedium)
                                Text(if (open) (room.furniture.getOrNull(chosen)?.let { "Eingerichtet mit ${it.first} ${it.second}" } ?: "Noch leer – wähle etwas aus!")
                                    else "Ab ${room.needCombs} Waben", style = MaterialTheme.typography.bodySmall)
                            }
                            room.furniture.getOrNull(chosen)?.let { Text(it.first, fontSize = 34.sp) }
                        }
                        if (open) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            room.furniture.forEachIndexed { i, (e, n) ->
                                FilterChip(selected = chosen == i, onClick = { chosenMap[room.id] = i; onFurnish(room.id, i); sfx?.play(Sound.PLOP) }, label = { Text("$e $n") })
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ───────────────────────────── Garten-Tiere ─────────────────────────────

@Composable
fun CrittersCard(current: Int, best: Int) {
    val earned = Critters.earned(best)
    val next = Critters.next(best)
    val awake = current > 0
    var said by remember { mutableStateOf<String?>(null) }
    val sfx = LocalSfx.current
    val t by rememberInfiniteTransition(label = "c").animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "t")
    ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🏡 Garten-Bewohner", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(if (current > 0) "🔥 $current ${if (current == 1) "Tag" else "Tage"}" else "😴 Pause", style = MaterialTheme.typography.labelLarge)
            }
            if (earned.isEmpty()) Text("Lerne 3 Tage hintereinander – dann zieht der erste Bewohner ein! 🦔", style = MaterialTheme.typography.bodySmall)
            else Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                earned.forEachIndexed { i, c ->
                    val hop = if (awake) maxOf(0f, sin((t + i * 0.23f) * 2 * PI.toFloat())) * -10f else 0f
                    Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.clickable { said = c.line; sfx?.play(Sound.PLOP) }) {
                        Text(c.emoji, fontSize = 34.sp, modifier = Modifier.graphicsLayer { translationY = hop; alpha = if (awake) 1f else 0.6f })
                        if (!awake) Text("💤", fontSize = 14.sp)
                    }
                }
            }
            said?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
            Text(
                when {
                    !awake && earned.isNotEmpty() -> "Die Tiere schlafen, bis du wieder gießt. Sie bleiben für immer bei dir! 💚"
                    next != null -> "Noch ${next.days - best} ${if (next.days - best == 1) "Tag" else "Tage"} Serie bis ${next.emoji} ${next.name}"
                    else -> "Alle Bewohner sind da – was für ein Garten! 🌈"
                }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ───────────────────────────── Tagesaufgaben ─────────────────────────────

@Composable
fun QuestCard(quests: List<Quests.Quest>, progress: Map<String, Int>, weekDone: Int, chestOpen: Boolean, onChest: () -> Unit) {
    val sfx = LocalSfx.current
    ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("📋 Heutige Aufgaben", style = MaterialTheme.typography.titleMedium)
            quests.forEach { q ->
                val have = (progress[q.kind.name] ?: 0).coerceAtMost(q.amount)
                val done = Quests.done(q, progress)
                val s by animateFloatAsState(if (done) 1.15f else 1f, spring(0.35f, 300f), label = "q")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(30.dp).graphicsLayer { scaleX = s; scaleY = s }.clip(CircleShape)
                        .background(if (done) Color(0xFF7CB342) else MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                        Text(if (done) "✓" else q.kind.emoji, color = if (done) Color.White else Color.Unspecified, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(q.text, style = MaterialTheme.typography.bodyMedium)
                        if (q.amount > 1) LinearProgressIndicator(progress = { have / q.amount.toFloat() }, modifier = Modifier.fillMaxWidth().padding(top = 3.dp))
                    }
                    if (q.amount > 1) Text("$have/${q.amount}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 8.dp))
                }
            }
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                val wiggle by rememberInfiniteTransition(label = "k").animateFloat(-6f, 6f, infiniteRepeatable(tween(300), RepeatMode.Reverse), label = "w")
                val ready = weekDone >= Quests.CHEST_AT && !chestOpen
                Text(if (chestOpen) "📭" else "🎁", fontSize = 30.sp, modifier = Modifier.graphicsLayer { rotationZ = if (ready) wiggle else 0f })
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (chestOpen) "Wochenkiste geöffnet – neue gibt's am Montag!" else "Wochenkiste: ${weekDone.coerceAtMost(Quests.CHEST_AT)}/${Quests.CHEST_AT} Aufgaben",
                        style = MaterialTheme.typography.bodyMedium)
                    if (!chestOpen) LinearProgressIndicator(progress = { (weekDone / Quests.CHEST_AT.toFloat()).coerceAtMost(1f) }, modifier = Modifier.fillMaxWidth().padding(top = 3.dp))
                }
                if (ready) Button(onClick = { sfx?.play(Sound.FANFARE); onChest() }, modifier = Modifier.padding(start = 8.dp)) { Text("Öffnen!") }
            }
        }
    }
}

// ───────────────────────────── Gedächtnis-Spaziergang ─────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LociScreen(suggest: List<StudyCard>, all: Map<String, StudyCard>, placed: Map<Int, String>, peers: Map<String, List<String>>,
               onPlace: (Map<Int, String>) -> Unit, onAnswer: (StudyCard, Boolean) -> Unit, onWalked: () -> Unit, onBack: () -> Unit) {
    BackHandler { onBack() }
    var map by remember { mutableStateOf(placed.filterValues { it in all }) }
    var walking by remember { mutableStateOf(false) }
    var pick by remember { mutableStateOf<StudyCard?>(null) }
    var story by remember { mutableStateOf<String?>(null) }
    val stops = map.keys.sorted()
    var stop by remember { mutableIntStateOf(0) }
    var answered by remember { mutableStateOf<Boolean?>(null) }
    var right by remember { mutableIntStateOf(0) }
    val sfx = LocalSfx.current
    val particles = rememberParticles()
    val t by rememberInfiniteTransition(label = "l").animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "t")
    // Brummi fliegt zum aktuellen Ort
    val target = if (walking && stop < stops.size) Loci.places[stops[stop]] else Loci.places[0]
    val bx by animateFloatAsState(target.x, tween(900, easing = FastOutSlowInEasing), label = "bx")
    val by by animateFloatAsState(target.y, tween(900, easing = FastOutSlowInEasing), label = "by")
    var size by remember { mutableStateOf(IntSize.Zero) }
    val dens = LocalDensity.current
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(Color(0xFFF1F8E9))) {
            Top("🚶 Gedächtnis-Spaziergang", onBack)
            // Gartenkarte mit Orten
            Box(Modifier.fillMaxWidth().height(260.dp).padding(horizontal = 12.dp).clip(RoundedCornerShape(20.dp))
                .background(de.wissensgarten.app.ui.fx.SafeBrush.verticalGradient(listOf(Color(0xFFB2DFDB), Color(0xFFA5D6A7), Color(0xFF81C784)))).onSizeChanged { size = it }) {
                Canvas(Modifier.fillMaxSize()) {
                    val pts = Loci.places.map { Offset(it.x * size.width, it.y * size.height) }
                    val path = Path().apply { pts.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
                    drawPath(path, Color(0xFFD7CCC8), style = Stroke(14f, pathEffect = PathEffect.cornerPathEffect(40f)))
                    drawPath(path, Color(0xFFBCAAA4), style = Stroke(3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f), -t * 52f)))
                }
                Loci.places.forEachIndexed { i, p ->
                    val cardId = map[i]
                    val active = walking && stops.getOrNull(stop) == i
                    val pulse = if (pick != null && !walking) 1f + 0.08f * sin(t * 2 * PI.toFloat() * 2) else if (active) 1.2f else 1f
                    with(dens) {
                        Column(Modifier.offset(x = (p.x * size.width).toDp() - 30.dp, y = (p.y * size.height).toDp() - 30.dp).width(60.dp)
                            .graphicsLayer { scaleX = pulse; scaleY = pulse }
                            .clickable(enabled = !walking) {
                                val c = pick
                                if (c != null) {
                                    map = map.filterValues { it != c.id } + (i to c.id); onPlace(map)
                                    story = Loci.story(i, c); pick = null; sfx?.play(Sound.PLOP)
                                } else if (cardId != null) { all[cardId]?.let { story = Loci.story(i, it) } }
                                else story = "Erst unten eine Karte antippen – dann den Ort hier. 👇"
                            }, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(p.emoji, fontSize = 30.sp)
                            Text(cardId?.let { all[it]?.img?.take(4) ?: "📌" } ?: p.name, fontSize = 10.sp, maxLines = 1,
                                modifier = Modifier.background(Color.White.copy(alpha = 0.75f), RoundedCornerShape(6.dp)).padding(horizontal = 3.dp))
                        }
                    }
                }
                with(dens) {
                    Box(Modifier.offset(x = (bx * size.width).toDp() - 20.dp, y = (by * size.height).toDp() - 52.dp)) { Brummi(Mood.HAPPY, size = 40.dp) }
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!walking) {
                    story?.let { Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(16.dp)) {
                        Text(if (it.startsWith("Stell")) "🧠 $it\nMach die Augen zu und stell es dir ganz verrückt vor!" else it, Modifier.padding(12.dp))
                    } }
                    Text(if (pick == null) "1. Tippe eine Karte an – 2. dann einen Ort im Garten." else "Wohin mit „${pick!!.back}“? Tippe auf einen Ort!",
                        style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        suggest.forEach { c ->
                            val here = c.id in map.values
                            FilterChip(selected = pick?.id == c.id, onClick = { pick = c; sfx?.play(Sound.TAP) },
                                label = { Text((c.img?.let { "$it " } ?: "") + c.back + if (here) " ✓" else "", maxLines = 1) })
                        }
                    }
                    Text("Wissenschaftlich: Orte sind für unser Gedächtnis wie Haken – Gedächtnis-Weltmeister machen das genauso. Am besten 3 bis 8 Karten.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { walking = true; stop = 0; right = 0; story = null; sfx?.play(Sound.TAP) }, enabled = map.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("🚶 Spaziergang starten (${map.size} Orte)") }
                } else if (stop < stops.size) {
                    val card = all.getValue(map.getValue(stops[stop]))
                    val place = Loci.places[stops[stop]]
                    val options = remember(stop) { Games.options(card, peers[card.bedId].orEmpty()) ?: listOf(card.back) }
                    Text("${place.emoji} Wir sind am ${place.name}. Was war hier?", style = MaterialTheme.typography.titleMedium)
                    Text(card.front, style = MaterialTheme.typography.titleLarge)
                    if (answered == null) de.wissensgarten.app.ui.common.TheoryButton(card.bedId)
                    options.forEach { o ->
                        val isRight = o == card.back
                        Button(onClick = {
                            if (answered != null) return@Button
                            answered = isRight; if (isRight) { right++; sfx?.play(Sound.CORRECT); particles.burst(Burst.SPARKLE, place.x, place.y * 0.4f, 24) } else sfx?.play(Sound.WRONG, volume = 0.4f)
                            onAnswer(card, isRight)
                        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(
                            containerColor = when { answered != null && isRight -> Color(0xFF81C784); else -> MaterialTheme.colorScheme.surfaceVariant },
                            contentColor = Color(0xFF22261F))) { Text(o) }
                    }
                    answered?.let { ok ->
                        Text(if (ok) "Genau! ${Loci.story(stops[stop], card)}" else "Fast! ${Loci.story(stops[stop], card)}", color = MaterialTheme.colorScheme.primary)
                        LaunchedEffect(stop) { delay(1600); answered = null; stop++ }
                    }
                } else {
                    LaunchedEffect(Unit) { onWalked(); sfx?.play(Sound.FANFARE); particles.burst(Burst.CONFETTI, 0.5f, 0.3f, 50) }
                    Brummi(Mood.CHEER, size = 96.dp, speech = de.wissensgarten.core.model.Laufzeit.palastText(right, stops.size),
                        speechRuntime = de.wissensgarten.core.model.Laufzeit.palast(right, stops.size))
                    OutlinedButton(onClick = { walking = false }) { Text("Orte ändern") }
                    Button(onClick = onBack) { Text("Zurück in den Garten") }
                }
            }
        }
        ParticleLayer(particles, Modifier.fillMaxSize())
    }
}
