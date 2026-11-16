package de.experimentopia.app.ui.fx

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import de.experimentopia.app.ui.theme.E
import kotlin.math.sin
import kotlin.random.Random

/** Sekunden seit Start – läuft, solange der Bildschirm sichtbar ist. Für alle Animationen, die aus der Zeit berechnet werden. */
@Composable
fun rememberSeconds(): State<Float> {
    val t = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var start = -1L
        while (true) withFrameNanos { n -> if (start < 0) start = n; t.floatValue = (n - start) / 1e9f }
    }
    return t
}

/** Knopf federt beim Drücken ein. */
fun Modifier.pressBounce(interaction: MutableInteractionSource): Modifier = composed {
    val pressed by interaction.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.92f else 1f, spring(dampingRatio = 0.4f, stiffness = 650f), label = "press")
    graphicsLayer { scaleX = s; scaleY = s }
}

/** Leichtes Pulsieren, um Aufmerksamkeit zu lenken. */
fun Modifier.pulse(on: Boolean, amount: Float = 0.04f): Modifier = composed {
    if (!on) return@composed this
    val p by rememberInfiniteTransition(label = "pulse").animateFloat(1f, 1f + amount,
        infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")
    graphicsLayer { scaleX = p; scaleY = p }
}

/** Wackeln, wenn [key] sich ändert (falsche Antwort – freundlich, nicht strafend). */
fun Modifier.shake(key: Int): Modifier = composed {
    val a = remember { Animatable(0f) }
    LaunchedEffect(key) { if (key > 0) { a.snapTo(0f); a.animateTo(0f, keyframes { durationMillis = 420; -14f at 60; 12f at 130; -8f at 200; 5f at 270; 0f at 420 }) } }
    graphicsLayer { translationX = a.value * density }
}

/** Ploppt beim Erscheinen auf. */
fun Modifier.popIn(delayMs: Int = 0): Modifier = composed {
    val s = remember { Animatable(0f) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(delayMs.toLong()); s.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f)) }
    graphicsLayer { scaleX = s.value; scaleY = s.value; alpha = s.value.coerceIn(0f, 1f) }
}

/** Leuchtender Glas-Knopf mit Klick-Geräusch und Haptik. */
@Composable
fun GlowButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = E.Mint,
    enabled: Boolean = true,
    sound: Sound = Sound.TAP,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val sfx = LocalSfx.current
    val view = LocalBuzzer.current
    val vib = LocalVibration.current
    val c = if (enabled) color else E.Muted.copy(alpha = 0.45f)
    Row(
        modifier.pressBounce(interaction).clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(c.copy(alpha = 0.42f), c.copy(alpha = 0.16f))))
            .border(2.dp, c, RoundedCornerShape(18.dp))
            .clickable(interaction, null, enabled = enabled) { sfx?.play(sound); view.buzz(Buzz.TICK, vib); onClick() }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) { CompositionLocalProvider(LocalContentColor provides E.Text) { content() } }
}

/** Hintergrund: Nachtlabor mit aufsteigenden, leuchtenden Bläschen. */
@Composable
fun BubbleBackground(modifier: Modifier = Modifier, tint: Color = E.Mint, count: Int = 26, seed: Int = 7) {
    val t by rememberSeconds()
    val bubbles = remember(seed, count) {
        val r = Random(seed)
        List(count) { floatArrayOf(r.nextFloat(), r.nextFloat(), 3f + r.nextFloat() * 9f, 0.02f + r.nextFloat() * 0.05f, r.nextFloat() * 6f) }
    }
    Canvas(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1C1340), E.Night, Color(0xFF0F0A22))))) {
        bubbles.forEachIndexed { i, b ->
            val y = (b[1] - t * b[3]).let { it - kotlin.math.floor(it) }
            val x = b[0] + 0.015f * sin(t * 1.3f + b[4])
            val col = listOf(tint, E.Pink, E.Sky, E.Sun)[i % 4]
            val c = Offset(x * size.width, y * size.height)
            val r = b[2] * density * 0.5f
            drawCircle(col.copy(alpha = 0.10f), r * 2.2f, c)
            drawCircle(col.copy(alpha = 0.35f), r, c, style = Stroke(1.2f * density))
            drawCircle(Color.White.copy(alpha = 0.35f), r * 0.25f, c + Offset(-r * 0.35f, -r * 0.35f))
        }
    }
}
