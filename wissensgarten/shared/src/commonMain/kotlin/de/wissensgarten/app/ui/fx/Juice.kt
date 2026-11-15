package de.wissensgarten.app.ui.fx

import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** Gemeinsame „Windzeit“ in Sekunden – Pflanzen lesen sie beim Zeichnen und wiegen sich. */
val LocalWind = staticCompositionLocalOf<State<Float>?> { null }

@Composable
fun rememberWind(): State<Float> {
    val t = rememberInfiniteTransition(label = "wind")
    return t.animateFloat(0f, 3600f, infiniteRepeatable(tween(3_600_000, easing = LinearEasing)), label = "w")
}

/** Federt beim Drücken leicht ein. */
@Composable
fun Modifier.pressBounce(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.93f else 1f, spring(dampingRatio = 0.35f, stiffness = 600f), label = "press")
    return graphicsLayer { scaleX = s; scaleY = s }
}

/** Knopf mit Feder-Effekt und Klick-Ton. */
@Composable
fun JuicyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    sound: Sound? = Sound.TAP,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val sfx = LocalSfx.current
    val interaction = remember { MutableInteractionSource() }
    Button(
        onClick = { sound?.let { sfx?.play(it) }; onClick() },
        modifier = modifier.pressBounce(interaction),
        enabled = enabled,
        colors = colors,
        interactionSource = interaction,
        contentPadding = contentPadding,
        content = content,
    )
}

/** Sanftes Pulsieren, um Aufmerksamkeit zu lenken. */
@Composable
fun Modifier.pulse(active: Boolean, amount: Float = 0.04f): Modifier {
    if (!active) return this
    val t = rememberInfiniteTransition(label = "pulse")
    val s by t.animateFloat(1f, 1f + amount, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")
    return graphicsLayer { scaleX = s; scaleY = s }
}

/** Wackeln bei falscher Antwort. Aufruf: scope.launch { shake.shake() } */
class Shake {
    val offset = Animatable(0f)
    suspend fun shake() {
        offset.animateTo(0f, keyframes {
            durationMillis = 420
            -18f at 50; 16f at 110; -12f at 170; 9f at 230; -5f at 290; 2f at 350
        })
    }
}

@Composable
fun rememberShake() = remember { Shake() }

fun Modifier.shake(s: Shake) = graphicsLayer { translationX = s.offset.value * density }

/** Zahl, die hochzählt statt zu springen. */
@Composable
fun animatedCount(target: Int, durationMs: Int = 900): Int {
    val v by animateIntAsState(target, tween(durationMs, easing = FastOutSlowInEasing), label = "count")
    return v
}
