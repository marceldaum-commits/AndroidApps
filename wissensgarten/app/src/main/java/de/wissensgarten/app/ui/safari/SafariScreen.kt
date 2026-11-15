package de.wissensgarten.app.ui.safari

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import de.wissensgarten.app.ui.fx.*
import de.wissensgarten.core.model.Safari
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import de.wissensgarten.app.ui.fx.Locale

/**
 * Wissens-Safari: Ein Ding um dich herum fotografieren, das Handy erkennt es (auf dem Gerät),
 * Brummi sagt das Wort auf Deutsch und Englisch – und es wächst als Pflanze mit DEINEM Foto im Garten.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafariScreen(found: Int, englishDefault: Boolean, onPlant: (Safari.Word, Boolean, Bitmap?) -> Unit, onBack: () -> Unit,
                 previewPhoto: Bitmap? = null, previewWords: List<Safari.Word>? = null) {
    val ctx = LocalContext.current
    val sfx = LocalSfx.current
    val voice = LocalVoice.current
    val scope = rememberCoroutineScope()
    val particles = rememberParticles()
    var photo by remember { mutableStateOf(previewPhoto) }
    var scanning by remember { mutableStateOf(false) }
    var words by remember { mutableStateOf(previewWords) }
    var chosen by remember { mutableStateOf<Safari.Word?>(null) }
    var english by remember { mutableStateOf(englishDefault) }
    var count by remember { mutableIntStateOf(found) }
    var message by remember { mutableStateOf<String?>(null) }
    val photoFile = remember { File(ctx.cacheDir, "safari").apply { mkdirs() }.resolve("foto.jpg") }
    val photoUri: Uri = remember { FileProvider.getUriForFile(ctx, "${ctx.packageName}.updates", photoFile) }

    fun recognize(bmp: Bitmap) {
        photo = bmp; scanning = true; words = null; chosen = null; message = null
        sfx?.play(Sound.WATER, volume = 0.4f)
        runCatching {
            com.google.mlkit.vision.label.ImageLabeling.getClient(com.google.mlkit.vision.label.defaults.ImageLabelerOptions.DEFAULT_OPTIONS)
                .process(com.google.mlkit.vision.common.InputImage.fromBitmap(bmp, 0))
                .addOnSuccessListener { labels ->
                    scope.launch {
                        delay(900) // Brummi „schaut genau hin“
                        val c = Safari.candidates(labels.map { it.text to it.confidence })
                        words = c; scanning = false
                        if (c.isEmpty()) { message = "Hmm, das kenne ich noch nicht. Probier ein anderes Ding – zum Beispiel ein Spielzeug, Obst oder ein Tier!"; sfx?.play(Sound.PLOP, pitch = 0.7f) }
                        else sfx?.play(Sound.GROW)
                    }
                }
                .addOnFailureListener { scanning = false; words = emptyList(); message = "Die Erkennung lädt noch – versuch es gleich noch mal." }
        }.onFailure { scanning = false; words = emptyList(); message = "Die Erkennung ist auf diesem Handy nicht verfügbar." }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (!ok) return@rememberLauncherForActivityResult
        // im Hintergrund laden (große Fotos ruckeln sonst) – verkleinert und richtig gedreht
        scope.launch { de.wissensgarten.app.ui.common.Images.load(ctx, photoUri, maxSide = 1024)?.let { recognize(it) } }
    }

    fun choose(w: Safari.Word) {
        chosen = w
        sfx?.play(Sound.FANFARE)
        particles.burst(Burst.CONFETTI, count = 50)
        scope.launch {
            voice?.speakAndWait("Das ist ${w.de}!")
            voice?.speakAndWait(w.en, Locale.ENGLISH)
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("🔭 Wissens-Safari") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } },
            actions = { Text("📷 $count entdeckt  ", style = MaterialTheme.typography.labelLarge) })
    }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val p = photo
                if (p == null) {
                    Spacer(Modifier.height(24.dp))
                    Brummi(Mood.HAPPY, size = 120.dp, speech = "Zeig mir etwas aus deiner Welt! Ich sag dir, wie es heißt – auf Deutsch und Englisch.")
                    Spacer(Modifier.height(24.dp))
                    Text("Ideen: 🧸 Spielzeug · 🍎 Obst · 🐶 Haustier · 👟 Schuhe · 🌸 Blumen", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.size(260.dp).clip(RoundedCornerShape(24.dp)).border(4.dp, Color(0xFFFFD54F), RoundedCornerShape(24.dp))) {
                        Image(p.asImageBitmap(), "Dein Foto", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        if (scanning) Magnifier()
                        chosen?.let { w ->
                            val pop = remember(w) { Animatable(0f) }
                            LaunchedEffect(w) { pop.animateTo(1f, spring(dampingRatio = 0.4f)) }
                            Text(w.emoji, fontSize = 54.sp, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value })
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    when {
                        scanning -> Text("Brummi schaut genau hin … 🔍", style = MaterialTheme.typography.titleMedium)
                        chosen != null -> {
                            val w = chosen!!
                            Text(w.de, fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color(0xFF2E5E2A))
                            Text("🇬🇧 ${w.en}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Lernen auf:", style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.width(8.dp))
                                FilterChip(!english, { english = false }, label = { Text("Deutsch") })
                                Spacer(Modifier.width(6.dp))
                                FilterChip(english, { english = true }, label = { Text("Englisch") })
                            }
                            Spacer(Modifier.height(10.dp))
                            JuicyButton(onClick = {
                                onPlant(w, english, photo); count++
                                sfx?.play(Sound.GROW); particles.burst(Burst.LEAVES, count = 40)
                                message = "🌱 Gepflanzt! Es wächst jetzt in „Meine Safari“ – mit deinem Foto."
                                photo = null; chosen = null; words = null
                            }, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("🌱 In meinen Garten pflanzen", fontWeight = FontWeight.Bold) }
                        }
                        !words.isNullOrEmpty() -> {
                            Text("Was ist auf dem Foto?", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            words!!.forEach { w ->
                                OutlinedButton(onClick = { choose(w) }, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                    Text("${w.emoji}  ${w.de}", fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
                message?.let { Text(it, textAlign = TextAlign.Center, modifier = Modifier.padding(12.dp)) }
                Spacer(Modifier.weight(1f))
                if (!scanning && chosen == null) JuicyButton(onClick = { runCatching { camera.launch(photoUri) } },
                    modifier = Modifier.fillMaxWidth().height(60.dp).padding(bottom = 8.dp).pulse(photo == null, 0.03f)) {
                    Text(if (photo == null) "📷 Ding fotografieren" else "📷 Noch ein Ding", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Text("Das Foto bleibt auf deinem Handy.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp))
            }
            ParticleLayer(particles, Modifier.matchParentSize())
        }
    }
}

/** Lupe, die über das Foto kreist, während erkannt wird. */
@Composable
private fun Magnifier() {
    val t by rememberInfiniteTransition(label = "lupe").animateFloat(0f, 6.283f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "t")
    Box(Modifier.fillMaxSize().background(Color(0x33000000))) {
        Text("🔍", fontSize = 64.sp, modifier = Modifier.align(Alignment.Center).graphicsLayer {
            translationX = kotlin.math.cos(t) * 60f * density; translationY = kotlin.math.sin(t) * 60f * density
        })
    }
}

/** Fotos aus der Kamera richtig herum drehen (EXIF). */
private fun rotate(file: File, bmp: Bitmap): Bitmap {
    val deg = runCatching {
        when (androidx.exifinterface.media.ExifInterface(file.path).getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, 1)) {
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }.getOrDefault(0f)
    if (deg == 0f) return bmp
    val m = android.graphics.Matrix().apply { postRotate(deg) }
    return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
}

/** Einstieg im Garten. */
@Composable
fun SafariEntryCard(onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🔭", fontSize = 30.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Wissens-Safari", style = MaterialTheme.typography.titleMedium, color = Color(0xFF1B5E20))
                Text("Fotografier Dinge um dich herum – Brummi sagt, wie sie heißen, und sie wachsen in deinem Garten.",
                    style = MaterialTheme.typography.bodySmall, color = Color(0xFF1B5E20))
            }
        }
    }
}
