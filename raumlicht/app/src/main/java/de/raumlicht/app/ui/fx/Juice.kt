package de.raumlicht.app.ui.fx

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import de.raumlicht.app.ui.theme.Q
import kotlin.math.sin
import kotlin.random.Random

/** Knopf federt beim Drücken ein. */
fun Modifier.pressBounce(interaction: MutableInteractionSource): Modifier = composed {
    val pressed by interaction.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.93f else 1f, spring(dampingRatio = 0.45f, stiffness = 600f), label = "press")
    graphicsLayer { scaleX = s; scaleY = s }
}

/** Leichtes Pulsieren, um Aufmerksamkeit zu lenken. */
fun Modifier.pulse(on: Boolean, amount: Float = 0.04f): Modifier = composed {
    if (!on) return@composed this
    val p by rememberInfiniteTransition(label = "pulse").animateFloat(1f, 1f + amount,
        infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")
    graphicsLayer { scaleX = p; scaleY = p }
}

/** Leuchtender Neon-Knopf mit Klick-Geräusch und Haptik. */
@Composable
fun NeonButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Q.Cyan,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val sfx = LocalSfx.current
    val view = LocalView.current
    val vib = LocalVibration.current
    val c = if (enabled) color else Q.Muted.copy(alpha = 0.5f)
    Row(
        modifier.pressBounce(interaction).clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(c.copy(alpha = 0.35f), c.copy(alpha = 0.15f))))
            .border(2.dp, c, RoundedCornerShape(16.dp))
            .clickable(interaction, null, enabled = enabled) { sfx?.play(Sound.TAP); view.buzz(Buzz.TICK, vib); onClick() }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) { androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides Q.Text) { content() } }
}

/** Funkelnder Sternenhimmel als Hintergrund. */
@Composable
fun Starfield(modifier: Modifier = Modifier, count: Int = 90, seed: Int = 1) {
    val t by rememberInfiniteTransition(label = "stars").animateFloat(0f, 1000f,
        infiniteRepeatable(tween(1_000_000, easing = LinearEasing)), label = "t")
    val stars = remember(seed, count) {
        val r = Random(seed)
        List(count) { Triple(Offset(r.nextFloat(), r.nextFloat()), r.nextFloat() * 1.6f + 0.6f, r.nextFloat() * 6f) }
    }
    Canvas(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Q.Space, Color(0xFF141A44), Q.Space)))) {
        stars.forEach { (p, size, phase) ->
            val a = 0.45f + 0.4f * sin(t * 1.3f + phase)
            drawCircle(Color.White.copy(alpha = a.coerceIn(0.1f, 1f)), size * density / 2, Offset(p.x * this.size.width, p.y * this.size.height))
        }
    }
}
