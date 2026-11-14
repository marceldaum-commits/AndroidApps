package de.wissensgarten.app.ui.play

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.wissensgarten.app.ui.fx.*
import java.io.File

/**
 * Selbst gemalte Merkbilder: Wer ein Bild selbst malt, merkt sich den Inhalt deutlich besser
 * („Drawing Effect“, Wammes et al. 2016). Gespeichert wird nur als PNG im App-Speicher – nichts verlässt das Gerät.
 */
object Drawings {
    /** Zähler, damit sich Bilder nach dem Speichern sofort überall aktualisieren. */
    var rev by mutableIntStateOf(0)
        private set

    private fun dir(ctx: Context) = File(ctx.filesDir, "merkbilder").apply { mkdirs() }
    fun file(ctx: Context, cardId: String) = File(dir(ctx), cardId.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".png")
    fun has(ctx: Context, cardId: String) = file(ctx, cardId).exists()

    fun load(ctx: Context, cardId: String): ImageBitmap? =
        file(ctx, cardId).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path)?.asImageBitmap() }

    fun save(ctx: Context, cardId: String, strokes: List<DrawStroke>, size: Int = 512) {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        strokes.forEach { s ->
            val p = android.graphics.Paint().apply {
                isAntiAlias = true; style = android.graphics.Paint.Style.STROKE
                strokeCap = android.graphics.Paint.Cap.ROUND; strokeJoin = android.graphics.Paint.Join.ROUND
                color = s.color.toArgb(); strokeWidth = s.width * size
            }
            val path = android.graphics.Path()
            s.points.forEachIndexed { i, o -> if (i == 0) path.moveTo(o.x * size, o.y * size) else path.lineTo(o.x * size, o.y * size) }
            if (s.points.size == 1) path.lineTo(s.points[0].x * size + 0.5f, s.points[0].y * size)
            c.drawPath(path, p)
        }
        file(ctx, cardId).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        rev++
    }

    fun delete(ctx: Context, cardId: String) { file(ctx, cardId).delete(); rev++ }
}

/** Ein Pinselstrich, Punkte normiert auf 0..1. */
class DrawStroke(val color: Color, val width: Float, val points: MutableList<Offset> = mutableStateListOf())

/** Eigenes Bild statt Emojis, falls gemalt – ploppt auf und schwebt wie das Merkbild. */
@Composable
fun DrawnPicture(cardId: String, size: Dp, modifier: Modifier = Modifier): Boolean {
    val ctx = LocalContext.current
    val rev = Drawings.rev
    val img = remember(cardId, rev) { Drawings.load(ctx, cardId) } ?: return false
    val pop = remember(cardId, rev) { Animatable(0f) }
    LaunchedEffect(cardId, rev) { pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 300f)) }
    val t by rememberInfiniteTransition(label = "dp").animateFloat(-3f, 3f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "r")
    Image(img, "Selbst gemaltes Merkbild", modifier.size(size).graphicsLayer { scaleX = pop.value; scaleY = pop.value; rotationZ = t }
        .clip(RoundedCornerShape(20.dp)).background(Color.White).border(3.dp, Color(0xFFFFD54F), RoundedCornerShape(20.dp)))
    return true
}

private val PALETTE = listOf(
    Color(0xFF22261F), Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFDD835),
    Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFF8E24AA), Color(0xFF8D6E63),
)

/** Malfläche: mit dem Finger malen, Farben und Pinsel wählen, rückgängig machen. Das Emoji-Merkbild schimmert als Vorlage durch. */
@Composable
fun DrawDialog(cardId: String, hint: String, img: String?, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val sfx = LocalSfx.current
    val strokes = remember { mutableStateListOf<DrawStroke>() }
    var color by remember { mutableStateOf(PALETTE[1]) }
    var width by remember { mutableFloatStateOf(0.025f) }
    var showTemplate by remember { mutableStateOf(true) }
    val particles = rememberParticles()
    Dialog(onDismissRequest = onDone, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFFFFFBF0), modifier = Modifier.padding(12.dp).fillMaxWidth()) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("✏️ Mal dein Merkbild!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(hint, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = Color(0xFF4D4A3F), maxLines = 3)
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp)).background(Color.White)
                    .border(3.dp, Color(0xFFFFD54F), RoundedCornerShape(20.dp))) {
                    if (showTemplate && !img.isNullOrBlank()) Text(img, fontSize = 96.sp, modifier = Modifier.align(Alignment.Center).alpha(0.15f))
                    Canvas(Modifier.matchParentSize().pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { o ->
                                strokes += DrawStroke(color, width).also { it.points += Offset(o.x / size.width, o.y / size.height) }
                            },
                            onDrag = { ch, _ ->
                                val p = ch.position
                                strokes.lastOrNull()?.points?.add(Offset((p.x / size.width).coerceIn(0f, 1f), (p.y / size.height).coerceIn(0f, 1f)))
                            },
                        )
                    }) {
                        strokes.forEach { s ->
                            val path = Path()
                            s.points.forEachIndexed { i, o -> if (i == 0) path.moveTo(o.x * size.width, o.y * size.height) else path.lineTo(o.x * size.width, o.y * size.height) }
                            drawPath(path, s.color, style = Stroke(s.width * size.width, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                    }
                    ParticleLayer(particles, Modifier.matchParentSize())
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PALETTE.forEach { c ->
                        val sel = c == color
                        val s by animateDpAsState(if (sel) 34.dp else 28.dp, spring(dampingRatio = 0.4f), label = "c")
                        Box(Modifier.size(s).clip(CircleShape).background(c).border(if (sel) 3.dp else 0.dp, Color.White, CircleShape)
                            .clickable { sfx?.play(Sound.TAP); color = c })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.012f to 8.dp, 0.025f to 14.dp, 0.05f to 22.dp).forEach { (w, d) ->
                        Box(Modifier.size(36.dp).clip(CircleShape).background(if (w == width) Color(0xFFFFE082) else Color(0xFFF1EDE0))
                            .clickable { sfx?.play(Sound.TAP); width = w }, contentAlignment = Alignment.Center) {
                            Box(Modifier.size(d).clip(CircleShape).background(color))
                        }
                    }
                    TextButton(onClick = { strokes.removeLastOrNull(); sfx?.play(Sound.PLOP, pitch = 0.8f) }, enabled = strokes.isNotEmpty()) { Text("↩️") }
                    TextButton(onClick = { strokes.clear() }, enabled = strokes.isNotEmpty()) { Text("🧽") }
                    if (!img.isNullOrBlank()) TextButton(onClick = { showTemplate = !showTemplate }) { Text(if (showTemplate) "👁️" else "🙈") }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (Drawings.has(ctx, cardId)) OutlinedButton(onClick = { Drawings.delete(ctx, cardId); onDone() }) { Text("Emojis zurück") }
                    JuicyButton(onClick = {
                        if (strokes.isNotEmpty()) {
                            Drawings.save(ctx, cardId, strokes.toList())
                            sfx?.play(Sound.FANFARE)
                        }
                        onDone()
                    }, enabled = strokes.isNotEmpty()) { Text("Fertig! 🎨", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
