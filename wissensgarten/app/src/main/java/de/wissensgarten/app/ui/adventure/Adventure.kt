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
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("🗺️ Abenteuerpfad", style = MaterialTheme.typography.titleLarge)
                Text("$done von ${stations.size} Stationen geschafft", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onIntro != null) FilledTonalButton(onClick = onIntro) { Text("▶️ Erklärung") }
        }
        Box(
            Modifier.fillMaxWidth().height(nodeH * stations.size + 30.dp).padding(horizontal = 12.dp)
                .clip(MaterialTheme.shapes.extraLarge).background(Brush.verticalGradient(Meadow)),
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
                            val bx = if (xFrac < 0.5f) mw * xFrac + 44.dp else mw * xFrac - 130.dp
                            Brummi(Mood.HAPPY, Modifier.align(Alignment.CenterStart).offset(x = bx), size = 64.dp)
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
                .clip(CircleShape).background(Brush.radialGradient(listOf(color.copy(alpha = 0.85f), color)))
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
            when { st.state == StationState.CURRENT -> "Start!"; st.thirsty > 0 -> "${st.thirsty} durstig"; else -> "Station ${st.index + 1}" },
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
    val wobble = rememberInfiniteTransition(label = "w").animateFloat(-6f, 6f, infiniteRepeatable(tween(160), RepeatMode.Reverse), label = "r")
    val lid = remember { Animatable(0f) }
    val rise = remember { Animatable(0f) }
    LaunchedEffect(opening) {
        if (!opening) return@LaunchedEffect
        sfx?.play(Sound.GROW)
        // Spannung: Deckel ruckelt kurz an, bevor er aufspringt (bildgesteuert statt Wartezeit)
        lid.animateTo(-0.06f, tween(250)); lid.animateTo(0.03f, tween(150)); lid.animateTo(0f, tween(100))
        friend = onOpen()
        sfx?.play(Sound.FANFARE)
        particles.burst(Burst.SPARKLE, 0.5f, 0.45f, 40)
        particles.burst(Burst.CONFETTI, count = 100)
        lid.animateTo(1f, spring(dampingRatio = 0.4f))
        rise.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 120f))
    }
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF3949AB), Color(0xFF7E57C2), Color(0xFFFFB74D))))
            .clickable(MutableInteractionSource(), null) { if (!opening) opening = true },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text(if (friend == null) "Du hast eine Schatztruhe gefunden!" else "Ein neuer Gartenfreund!", color = Color.White,
                style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(30.dp))
            Box(Modifier.size(240.dp), contentAlignment = Alignment.BottomCenter) {
                friend?.let { f ->
                    Text(f.emoji, fontSize = 110.sp, modifier = Modifier.align(Alignment.Center).graphicsLayer {
                        translationY = (1 - rise.value) * 220f - 40f; scaleX = rise.value; scaleY = rise.value
                    })
                }
                Canvas(Modifier.size(180.dp, 130.dp).graphicsLayer { rotationZ = if (!opening) wobble.value else 0f }) {
                    val w = size.width; val h = size.height
                    val wood = Color(0xFF8D5524); val gold = Color(0xFFFFD54F)
                    drawRoundRect(wood, Offset(0f, h * 0.35f), Size(w, h * 0.65f), androidx.compose.ui.geometry.CornerRadius(14f))
                    drawRect(gold, Offset(w * 0.45f, h * 0.35f), Size(w * 0.1f, h * 0.65f))
                    drawRect(gold, Offset(0f, h * 0.5f), Size(w, h * 0.06f))
                    // Deckel klappt auf
                    rotate(-110f * lid.value, Offset(w * 0.02f, h * 0.35f)) {
                        drawRoundRect(Color(0xFFA0522D), Offset(0f, 0f), Size(w, h * 0.38f), androidx.compose.ui.geometry.CornerRadius(30f))
                        drawRect(gold, Offset(w * 0.45f, 0f), Size(w * 0.1f, h * 0.38f))
                    }
                    drawCircle(gold, w * 0.05f, Offset(w * 0.5f, h * 0.45f))
                }
            }
            Spacer(Modifier.height(20.dp))
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
fun FriendsAlbum(opened: Int, earned: Int, inGarden: Set<String>, onToggle: (String) -> Unit, onOpenChest: () -> Unit) {
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E57C2))) { Text("🎁 ${earned - opened} Truhe(n) öffnen!", fontWeight = FontWeight.Bold) }
                }
            }
        }
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
