package de.wissensgarten.app.ui.play

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.app.ui.fx.LocalSfx
import de.wissensgarten.app.ui.fx.Sound
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

/** Teilt einen Emoji-String in einzelne Emojis (Graphem-Cluster). */
fun splitEmojis(s: String): List<String> = de.wissensgarten.app.ui.common.graphemes(s).filter { it.isNotBlank() }

/**
 * Merkbild: die Emojis ploppen nacheinander auf und schweben dann leicht.
 * Antippen lässt sie hüpfen – damit sich das Bild einprägt.
 */
@Composable
fun MerkBild(img: String?, modifier: Modifier = Modifier, emojiSize: TextUnit = 64.sp, key: Any = img ?: "", cardId: String? = null) {
    if (cardId != null && DrawnPicture(cardId, (emojiSize.value * 1.7f).dp, modifier)) return
    if (img.isNullOrBlank()) return
    val parts = remember(img) { splitEmojis(img) }
    val sfx = LocalSfx.current
    val scope = rememberCoroutineScope()
    val pops = remember(key) { parts.map { Animatable(0f) } }
    val float by rememberInfiniteTransition(label = "float").animateFloat(0f, 6.283f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "f")
    LaunchedEffect(key) {
        pops.forEachIndexed { i, a ->
            delay(140L * i + 80)
            launch { a.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 300f)) }
        }
    }
    Row(
        modifier.clickable(androidx.compose.runtime.remember { MutableInteractionSource() }, null) {
            sfx?.play(Sound.PLOP, pitch = 1.3f)
            scope.launch { pops.forEach { launch { it.snapTo(1.35f); it.animateTo(1f, spring(dampingRatio = 0.3f)) } } }
        },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        parts.forEachIndexed { i, e ->
            Text(
                e, fontSize = emojiSize,
                modifier = Modifier.graphicsLayer {
                    val s = pops.getOrNull(i)?.value ?: 1f
                    scaleX = s; scaleY = s
                    translationY = sin(float + i * 1.3f) * 6f * density
                    rotationZ = sin(float * 0.5f + i) * 6f
                },
            )
        }
    }
}
