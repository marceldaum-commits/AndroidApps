package de.wissensgarten.app.ui.adventure

import de.wissensgarten.app.ui.common.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.app.ui.fx.*
import de.wissensgarten.app.ui.play.splitEmojis
import de.wissensgarten.core.model.Pack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Erklär-Geschichte zu einem Wissenspaket: Brummi erzählt in 3–5 Szenen.
 * Bilder fliegen herein, der Text erscheint wie gesprochen. Tippen = weiter.
 */
@Composable
fun IntroScreen(pack: Pack, onDone: () -> Unit) = StoryPlayer("${pack.emoji} ${pack.title}", pack.intro, onDone)

/** Spielt eine Szenenfolge ab (Erklär-Geschichte oder Kapitel aus Brummis Abenteuer). */
@Composable
fun StoryPlayer(title: String, scenes: List<de.wissensgarten.core.model.IntroScene>, onDone: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    var shown by remember { mutableIntStateOf(0) }
    val sfx = LocalSfx.current
    val particles = rememberParticles()
    BackHandler { onDone() }
    if (scenes.isEmpty()) { LaunchedEffect(Unit) { onDone() }; return }
    val scene = scenes[index]
    val last = index == scenes.lastIndex
    val typing = shown < scene.text.length

    val voice = LocalVoice.current
    val auto = LocalReadAloud.current
    // Schreibmaschinen-Effekt (und vorlesen)
    LaunchedEffect(index) {
        shown = 0
        if (auto) voice?.speak(scene.text)
        sfx?.play(Sound.PLOP, pitch = 1f + index * 0.08f)
        delay(350)
        while (shown < scene.text.length) { shown++; delay(22) }
    }

    fun advance() {
        when {
            typing -> shown = scene.text.length
            last -> { sfx?.play(Sound.LEVEL_UP); onDone() }
            else -> { sfx?.play(Sound.TAP); index++ }
        }
    }

    Box(
        Modifier.fillMaxSize()
            .background(de.wissensgarten.app.ui.fx.SafeBrush.verticalGradient(listOf(Color(0xFF8EC5EA), Color(0xFFCDEBFA), Color(0xFFE8F5E9))))
            .clickable(androidx.compose.runtime.remember { MutableInteractionSource() }, null) { advance() },
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Color(0xFF14321A), modifier = Modifier.weight(1f))
                TextButton(onClick = onDone) { Text("Überspringen", color = Color(0xFF14321A)) }
            }
            // Fortschritts-Punkte
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                scenes.indices.forEach { i ->
                    val w by animateDpAsState(if (i == index) 26.dp else 10.dp, label = "dot")
                    Box(Modifier.height(10.dp).width(w).clip(CircleShape).background(if (i <= index) Color(0xFF3F6B3A) else Color.White.copy(alpha = 0.6f)))
                }
            }
            // Szene: Emojis fliegen herein
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                AnimatedContent(index, transitionSpec = {
                    (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
                }, label = "szene") { i -> SceneImage(scenes[i].img, i) }
            }
            // Brummi erzählt
            Row(verticalAlignment = Alignment.Bottom) {
                Brummi(if (last && !typing) Mood.CHEER else if (typing) Mood.HAPPY else Mood.IDLE, size = 92.dp, onTap = { voice?.speak(scene.text) })
                Surface(color = Color.White, shape = MaterialTheme.shapes.extraLarge, shadowElevation = 4.dp, modifier = Modifier.weight(1f).padding(bottom = 24.dp)) {
                    Box(Modifier.padding(16.dp)) {
                        // Platz für den vollen Text reservieren, damit nichts springt
                        Text(scene.text, style = MaterialTheme.typography.titleMedium, color = Color.Transparent)
                        Text(scene.text.take(shown), style = MaterialTheme.typography.titleMedium, color = Color(0xFF22261F))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            JuicyButton(
                onClick = { advance() }, sound = null,
                modifier = Modifier.fillMaxWidth().height(58.dp).pulse(!typing, 0.03f),
                colors = ButtonDefaults.buttonColors(containerColor = if (last) Color(0xFF3F6B3A) else Color(0xFF4F9FD6)),
            ) { Text(if (last) "Los geht's! 🌱" else if (typing) "…" else "Weiter", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        }
        ParticleLayer(particles, Modifier.matchParentSize())
    }
}

@Composable
private fun SceneImage(img: String, sceneIndex: Int) {
    val parts = remember(img) { splitEmojis(img) }
    val anims = remember(img, sceneIndex) { parts.map { Animatable(0f) } }
    val t by rememberInfiniteTransition(label = "s").animateFloat(0f, 6.283f, infiniteRepeatable(tween(3500, easing = LinearEasing)), label = "t")
    LaunchedEffect(img, sceneIndex) {
        anims.forEachIndexed { i, a -> launch { delay(140L * i); a.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 180f)) } }
    }
    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        parts.forEachIndexed { i, e ->
            val p = anims.getOrNull(i)?.value ?: 1f
            val fromLeft = i % 2 == 0
            Text(
                e, fontSize = if (parts.size <= 2) 96.sp else 72.sp, textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = p.coerceIn(0f, 1f)
                    translationX = (1 - p) * (if (fromLeft) -600f else 600f)
                    translationY = sin(t + i * 1.4f) * 10f * density
                    rotationZ = (1 - p) * (if (fromLeft) -40f else 40f) + sin(t * 0.5f + i) * 5f
                    scaleX = 0.6f + 0.4f * p; scaleY = 0.6f + 0.4f * p
                },
            )
        }
    }
}
