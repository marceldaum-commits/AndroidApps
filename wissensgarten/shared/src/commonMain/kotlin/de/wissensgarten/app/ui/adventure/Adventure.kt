package de.wissensgarten.app.ui.adventure

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.app.ui.fx.*
import de.wissensgarten.core.model.Friend
import de.wissensgarten.core.model.Friends
import de.wissensgarten.core.model.Rarity
import de.wissensgarten.core.model.Station
import de.wissensgarten.core.model.StationState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

private val Meadow = listOf(Color(0xFFE8F5E9), Color(0xFFDCEFD0), Color(0xFFCDE8BE))
private val NodeDone = Color(0xFF6FA35A)
private val NodeCurrent = Color(0xFFFFB300)
private val NodeLocked = Color(0xFFCFC8B8)

/**
 * Abenteuerpfad: Stationen schlängeln sich als Weg über eine Wiese.
 * Geschaffte Stationen sind grün, die aktuelle pulsiert (Brummi wartet dort), Truhen locken.
 */
@Composable
fun AdventurePathView(
    stations: List<Station>,
    onStation: (Station) -> Unit,
    onIntro: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val sfx = LocalSfx.current
    val done = stations.count { it.state == StationState.DONE }
    val nodeH = 108.dp
    val currentIdx = stations.indexOfFirst { it.state == StationState.CURRENT }
    val scroll = rememberScrollState()
    // Gleich dorthin scrollen, wo es weitergeht (eine Station davor bleibt sichtbar)
    val density = LocalDensity.current
    LaunchedEffect(currentIdx) {
        if (currentIdx > 1) scroll.animateScrollTo(with(density) { (nodeH * (currentIdx - 1)).roundToPx() })
    }
    Column(modifier.fillMaxWidth().verticalScroll(scroll)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("🗺️ Abenteuerpfad", style = MaterialTheme.typography.titleLarge)
                Text(if (currentIdx >= 0) "$done von ${stations.size} geschafft · weiter bei Station ${currentIdx + 1}" else "Alle ${stations.size} Stationen geschafft! 🏆", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onIntro != null) FilledTonalButton(onClick = onIntro) { Text("▶️ Erklärung") }
        }
        Box(
            Modifier.fillMaxWidth().height(nodeH * stations.size + 30.dp).padding(horizontal = 12.dp)
                .clip(MaterialTheme.shapes.extraLarge).background(de.wissensgarten.app.ui.fx.SafeBrush.verticalGradient(Meadow)),
        ) {
            val wind = LocalWind.current
            // Weg und Blumen-Deko
            Canvas(Modifier.matchParentSize()) {
                val step = nodeH.toPx()
                // Kreismitte: 12 dp Rand + (Zeilenhöhe – Kreis – Beschriftung) / 2 + Kreisradius
                val top = 12.dp.toPx() + (nodeH.toPx() - 96.dp.toPx()) / 2 + 38.dp.toPx()
                val path = Path()
                stations.indices.forEach { i ->
                    val p = nodePos(i, size.width, step, top)
                    if (i == 0) path.moveTo(p.x, p.y) else {
                        val prev = nodePos(i - 1, size.width, step, top)
                        path.cubicTo(prev.x, prev.y + step * 0.5f, p.x, p.y - step * 0.5f, p.x, p.y)
                    }
                }
                drawPath(path, Color(0xFFE6D3A3), style = Stroke(width = 26.dp.toPx(), cap = StrokeCap.Round))
                drawPath(path, Color(0xFFF3E6C4), style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 16f))))
                // Schon gegangener Weg leuchtet golden – bis zur aktuellen Station
                val walkedTo = if (currentIdx >= 0) currentIdx else stations.lastIndex
                if (walkedTo > 0) {
                    val walked = Path()
                    for (i in 0..walkedTo) {
                        val p = nodePos(i, size.width, step, top)
                        if (i == 0) walked.moveTo(p.x, p.y) else {
                            val prev = nodePos(i - 1, size.width, step, top)
                            walked.cubicTo(prev.x, prev.y + step * 0.5f, p.x, p.y - step * 0.5f, p.x, p.y)
                        }
                    }
                    drawPath(walked, Color(0xFFFFCA28), style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 16f))))
                }
                val r = kotlin.random.Random(7)
                repeat(stations.size * 3) {
                    val x = r.nextFloat() * size.width; val y = r.nextFloat() * size.height
                    val c = listOf(Color(0xFFF48FB1), Color(0xFFFFF59D), Color(0xFFB39DDB), Color.White)[it % 4]
                    val t = wind?.value ?: 0f
                    val sway = sin(t * 1.5f + it) * 3f
                    drawLine(Color(0xFF6FA35A), Offset(x, y), Offset(x + sway, y - 10.dp.toPx()), 2.dp.toPx())
                    for (k in 0 until 5) {
                        val a = k * 1.2566f
                        drawCircle(c, 3.dp.toPx(), Offset(x + sway + kotlin.math.cos(a) * 4.dp.toPx(), y - 10.dp.toPx() + sin(a) * 4.dp.toPx()))
                    }
                }
            }
            // Stationen
            stations.forEachIndexed { i, st ->
                BoxWithConstraints(Modifier.fillMaxWidth().height(nodeH).offset(y = nodeH * i + 12.dp)) {
                    val mw = maxWidth
                    val xFrac = nodeXFrac(i)
                    Box(Modifier.fillMaxSize()) {
                        StationNode(st, Modifier.align(Alignment.CenterStart).offset(x = mw * xFrac - 38.dp)) {
                            if (st.state == StationState.LOCKED) sfx?.play(Sound.WRONG, volume = 0.4f) else { sfx?.play(Sound.PLOP); onStation(st) }
                        }
                        if (st.state == StationState.CURRENT) {
                            // Brummi steht auf der freien Seite und zeigt, wo es weitergeht
                            val say = "Du bist hier! Tipp auf den Kreis 👆"
                            if (xFrac < 0.5f) Box(Modifier.align(Alignment.CenterStart).offset(x = mw * xFrac + 44.dp).width(mw * (1 - xFrac) - 44.dp)) {
                                Brummi(Mood.HAPPY, Modifier.align(Alignment.CenterStart), size = 56.dp, speech = say)
                            } else Box(Modifier.align(Alignment.CenterStart).width(mw * xFrac - 44.dp)) {
                                Brummi(Mood.HAPPY, Modifier.align(Alignment.CenterEnd), size = 56.dp, speech = say, bubbleOnLeft = true)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun nodeXFrac(i: Int) = 0.5f + 0.28f * sin(i * 1.15f)
private fun nodePos(i: Int, width: Float, step: Float, top: Float) = Offset(width * nodeXFrac(i), step * i + top)

@Composable
private fun StationNode(st: Station, modifier: Modifier, onClick: () -> Unit) {
    val current = st.state == StationState.CURRENT
    val pulse = rememberInfiniteTransition(label = "node").animateFloat(1f, if (current) 1.12f else 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "p")
    val color = when (st.state) { StationState.DONE -> NodeDone; StationState.CURRENT -> NodeCurrent; else -> NodeLocked }
    val interaction = remember { MutableInteractionSource() }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(76.dp).graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }.pressBounce(interaction)
                .clip(CircleShape).background(de.wissensgarten.app.ui.fx.SafeBrush.radialGradient(listOf(color.copy(alpha = 0.85f), color)))
                .border(5.dp, Color.White, CircleShape)
                .clickable(interaction, null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                when {
                    st.chest && st.state == StationState.DONE -> "🎉"
                    st.chest -> "🎁"
                    st.state == StationState.DONE -> if (st.thirsty > 0) "💧" else "⭐"
                    st.state == StationState.CURRENT -> "🌱"
                    else -> "🔒"
                }, fontSize = 32.sp,
            )
        }
        Text(
            when { st.state == StationState.CURRENT -> "📍 Station ${st.index + 1}"; st.thirsty > 0 -> "${st.thirsty} durstig"; else -> "Station ${st.index + 1}" },
            style = MaterialTheme.typography.labelMedium, fontWeight = if (current) FontWeight.Black else FontWeight.Medium,
            color = if (current) Color(0xFF8D5A00) else Color(0xFF4D4A3F),
        )
    }
}

// ---------------------------------------------------------------- Truhe öffnen

/** Vollbild: Truhe wackelt, springt auf, der neue Gartenfreund fliegt heraus. */
@Composable
fun ChestReveal(onOpen: () -> Friend?, onClose: () -> Unit) {
    val sfx = LocalSfx.current
    val particles = rememberParticles()
    var friend by remember { mutableStateOf<Friend?>(null) }
    var opening by remember { mutableStateOf(false) }
    val inf = rememberInfiniteTransition(label = "truhe")
    // Drehteller: langsam hin und her, damit man die 3D-Form sieht
    val spin by inf.animateFloat(-32f, 32f, infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "yaw")
    val bob by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    val shake = remember { Animatable(0f) }
    val lid = remember { Animatable(0f) }
    val glow = remember { Animatable(0f) }
    val rise = remember { Animatable(0f) }
    val face = remember { Animatable(0f) }   // dreht die Truhe zum Öffnen nach vorn
    LaunchedEffect(opening) {
        if (!opening) return@LaunchedEffect
        sfx?.play(Sound.GROW)
        launch { face.animateTo(1f, tween(400)) }
        // Spannung: Truhe ruckelt, Deckel hebt sich kurz an
        shake.animateTo(0f, keyframes { durationMillis = 600; 10f at 80; -10f at 160; 8f at 240; -8f at 320; 5f at 400; -3f at 480 })
        lid.animateTo(0.08f, tween(120)); lid.animateTo(0f, tween(120))
        friend = onOpen()
        sfx?.play(Sound.COLLECT)
        launch { glow.animateTo(1f, tween(500)) }
        particles.burst(Burst.SPARKLE, 0.5f, 0.45f, 40)
        particles.burst(Burst.CONFETTI, count = 100)
        launch { lid.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 140f)) }
        delay(150)
        rise.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 120f))
    }
    Box(
        Modifier.fillMaxSize().background(de.wissensgarten.app.ui.fx.SafeBrush.verticalGradient(listOf(Color(0xFF283593), Color(0xFF7E57C2), Color(0xFFFFB74D))))
            .clickable(androidx.compose.runtime.remember { MutableInteractionSource() }, null) { if (!opening) opening = true },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text(if (friend == null) "Du hast eine Schatztruhe gefunden!" else "Ein neuer Gartenfreund!", color = Color.White,
                style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize().graphicsLayer { translationY = -bob * 10f * density * (1 - face.value) }) {
                    drawChest3D(yaw = spin * (1 - face.value) + 0f, open = lid.value, glow = glow.value, wobble = shake.value)
                }
                // Freund steigt aus dem Inneren: alles unterhalb der Vorderkante wird abgeschnitten
                friend?.let { f ->
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val rim = with(LocalDensity.current) { chestRimY(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat()).toDp() }
                        Box(Modifier.fillMaxWidth().height(rim).clipToBounds(), contentAlignment = Alignment.BottomCenter) {
                            Text(f.emoji, fontSize = 96.sp, modifier = Modifier.graphicsLayer {
                                translationY = (1 - rise.value) * 130f * density - rise.value * 20f * density
                                scaleX = 0.7f + 0.3f * rise.value; scaleY = 0.7f + 0.3f * rise.value
                            })
                        }
                    }
                }
            }
            AnimatedVisibility(friend != null, enter = fadeIn() + expandVertically()) {
                friend?.let { f ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        RarityBadge(f.rarity)
                        Text(f.name, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text(f.text, color = Color.White, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                        Text("+15 💧", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        Spacer(Modifier.height(20.dp))
                        JuicyButton(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF5E35B1)),
                            modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("In meinen Garten! 🌷", fontWeight = FontWeight.Bold) }
                    }
                }
            }
            if (friend == null && !opening) Text("Tippe, um sie zu öffnen 👆", color = Color.White, modifier = Modifier.padding(top = 8.dp))
        }
        ParticleLayer(particles, Modifier.matchParentSize())
    }
}

@Composable
fun RarityBadge(r: Rarity) {
    val (bg, fg) = when (r) {
        Rarity.NORMAL -> Color(0xFFC8E6C9) to Color(0xFF1B5E20)
        Rarity.RARE -> Color(0xFFBBDEFB) to Color(0xFF0D47A1)
        Rarity.LEGENDARY -> Color(0xFFFFE082) to Color(0xFF6D4C00)
    }
    Surface(color = bg, shape = CircleShape) {
        Text(r.label.uppercase(), color = fg, fontWeight = FontWeight.Black, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
    }
}

// ---------------------------------------------------------------- Sammelalbum

/** Album aller Gartenfreunde. Die nächste Figur ist als Schatten sichtbar – man weiß, worauf man hinarbeitet. */
@Composable
fun FriendsAlbum(opened: Int, earned: Int, inGarden: Set<String>, onToggle: (String) -> Unit, onOpenChest: () -> Unit,
                 hybrids: List<de.wissensgarten.core.model.Hybrid> = emptyList(),
                 crystals: List<Pair<String, String>> = emptyList(), crystalsOnTheWay: Int = 0) {
    LazyVerticalGrid(GridCells.Adaptive(104.dp), contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text("🐾 Gartenfreunde: $opened von ${Friends.all.size}", style = MaterialTheme.typography.titleMedium)
                Text("Jede dritte Station auf einem Abenteuerpfad hat eine Truhe. Tippe Freunde an, um sie in deinen Garten zu holen (max. 6).",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (earned > opened) {
                    Spacer(Modifier.height(8.dp))
                    JuicyButton(onClick = onOpenChest, modifier = Modifier.fillMaxWidth().pulse(true), sound = Sound.PLOP,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E57C2))) { Text(if (earned - opened == 1) "🎁 Truhe öffnen!" else "🎁 ${earned - opened} Truhen öffnen!", fontWeight = FontWeight.Bold) }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text("💎 Kristalle: ${crystals.size}", style = MaterialTheme.typography.titleMedium)
                Text(if (crystals.isEmpty()) "Karten, die dir schwerfielen und die du jetzt sicher kannst, werden zu Kristallen." +
                        (if (crystalsOnTheWay > 0) " $crystalsOnTheWay sind schon auf dem Weg!" else "")
                    else "Früher schwer – jetzt sitzt es. Darauf kannst du stolz sein!",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(crystals.size) { i ->
            val (q, a) = crystals[i]
            val shine by rememberInfiniteTransition(label = "k").animateFloat(0.85f, 1.1f,
                infiniteRepeatable(tween(900 + i * 37 % 400), RepeatMode.Reverse), label = "s")
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE1F5FE))) {
                Column(Modifier.fillMaxWidth().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💎", fontSize = 34.sp, modifier = Modifier.graphicsLayer { scaleX = shine; scaleY = shine })
                    Text(a, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2, fontWeight = FontWeight.Bold)
                    Text(q, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2)
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text("🌺 Kreuzungen: ${hybrids.size}", style = MaterialTheme.typography.titleMedium)
                Text(if (hybrids.isEmpty()) "Spiel eine Mischrunde aus zwei Fächern – mit 75 % richtig züchtest du eine Mischblüte."
                    else "Jede Fächer-Kombination ergibt eine eigene Blume.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(hybrids.size) { i ->
            val h = hybrids[i]
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F7FA))) {
                Column(Modifier.fillMaxWidth().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    de.wissensgarten.app.ui.common.Plant(de.wissensgarten.core.model.Stage.BLOOM, 0f, h.a.hue, i, Modifier.size(64.dp), species = h.species())
                    Text(h.name, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2)
                    Text("${h.a.emoji}×${h.b.emoji}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { Text("🐾 Gartenfreunde", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
        items(Friends.all.size) { i ->
            val f = Friends.all[i]
            val have = i < opened
            val next = i == opened
            val chosen = f.id in inGarden
            Card(
                onClick = { if (have) onToggle(f.id) },
                colors = CardDefaults.cardColors(containerColor = when {
                    chosen -> MaterialTheme.colorScheme.primaryContainer
                    have -> MaterialTheme.colorScheme.surfaceContainerLow
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }),
            ) {
                Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(f.emoji, fontSize = 40.sp, modifier = Modifier.graphicsLayer { alpha = if (have) 1f else if (next) 0.25f else 0.08f })
                    Text(if (have || next) f.name else "???", style = MaterialTheme.typography.labelLarge, maxLines = 1, textAlign = TextAlign.Center)
                    Text(
                        when { chosen -> "🌷 im Garten"; have -> f.rarity.label; next -> "als Nächstes"; else -> "" },
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
