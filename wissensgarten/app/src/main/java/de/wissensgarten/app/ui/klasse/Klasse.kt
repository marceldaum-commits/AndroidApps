package de.wissensgarten.app.ui.klasse

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
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
import androidx.compose.ui.window.Dialog
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import de.wissensgarten.app.ui.fx.*
import de.wissensgarten.app.ui.play.MerkBild
import de.wissensgarten.core.model.ClassCard
import de.wissensgarten.core.model.ClassCodes
import de.wissensgarten.core.model.ClassSet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot

fun qrMatrix(text: String): BitMatrix? = runCatching {
    QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L, EncodeHintType.MARGIN to 1))
}.getOrNull()

/**
 * QR-Code, der wie eine Welle von der Mitte nach außen „aufblüht“.
 */
@Composable
fun QrBloom(text: String, modifier: Modifier = Modifier) {
    val m = remember(text) { qrMatrix(text) } ?: return
    val grow = remember(text) { Animatable(0f) }
    LaunchedEffect(text) { grow.animateTo(1f, tween(1400, easing = FastOutSlowInEasing)) }
    Canvas(modifier.aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(Color.White)) {
        val n = m.width
        val cell = size.width / n
        val c = (n - 1) / 2f
        val maxD = hypot(c, c)
        for (y in 0 until n) for (x in 0 until n) {
            if (!m[x, y]) continue
            val d = hypot(x - c, y - c) / maxD
            val s = ((grow.value * 1.4f - d * 0.4f - (1 - grow.value) * 0f) / 1f).coerceIn(0f, 1f)
            if (s <= 0f) continue
            val w = cell * s
            val col = if (s < 1f) Color(0xFF2E7D32) else Color(0xFF1B1B1B)
            drawRoundRect(col, Offset(x * cell + (cell - w) / 2, y * cell + (cell - w) / 2), Size(w, w), CornerRadius(w * 0.25f * (1 - s)))
        }
    }
}

/** Lehrkraft / Eltern: Beet als QR-Code zeigen oder Link teilen. */
@Composable
fun ClassShareDialog(set: ClassSet, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val sfx = LocalSfx.current
    val link = remember(set) { ClassCodes.link(set) }
    val fits = link.length <= ClassCodes.MAX_LENGTH
    LaunchedEffect(Unit) { sfx?.play(Sound.GROW) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFFFFFBF0)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🏫 Klassen-Code", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${set.emoji} ${set.title} · ${set.cards.size} Karten", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                if (set.cards.isEmpty()) Text("Erst ein paar Karten anlegen – dann kann die Klasse sie scannen.", textAlign = TextAlign.Center)
                else if (fits) {
                    QrBloom(link, Modifier.fillMaxWidth())
                    Text("Am Beamer zeigen: Die Klasse scannt im Wissensgarten unter Samen → „Klassen-Code“ und hat sofort das Beet. Kein Konto, keine Daten.",
                        style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
                } else Text("Für einen QR-Code sind es zu viele Karten – teile stattdessen den Link (z. B. im Klassen-Chat).",
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss) { Text("Schließen") }
                    if (set.cards.isNotEmpty()) JuicyButton(onClick = {
                        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(Intent.EXTRA_TEXT, "🌱 Lernbeet „${set.title}“ für den Wissensgarten (${set.cards.size} Karten):\n$link"), "Klassen-Code teilen"))
                    }) { Text("📤 Link teilen") }
                }
            }
        }
    }
}

/** Liste der Karten, die nacheinander hereinploppen. */
@Composable
private fun PoppingCard(i: Int, emoji: String?, front: String, back: String, trailing: @Composable () -> Unit = {}) {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(60L * i.coerceAtMost(15)); pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 400f)) }
    Card(Modifier.fillMaxWidth().graphicsLayer { scaleX = pop.value; scaleY = pop.value; alpha = pop.value.coerceIn(0f, 1f) },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F3E8))) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji ?: "🌱", fontSize = 28.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.Center)
            Column(Modifier.weight(1f)) {
                Text(front, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(back, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF5D5A4F))
            }
            trailing()
        }
    }
}

/** Schüler: Klassen-Code scannen oder einfügen, Vorschau ansehen, Beet pflanzen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassImportScreen(initial: ClassSet?, onDecode: (String) -> ClassSet?, onPlant: (ClassSet) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val sfx = LocalSfx.current
    var set by remember { mutableStateOf(initial) }
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val particles = rememberParticles()
    fun accept(raw: String) {
        val s = onDecode(raw)
        if (s == null) { error = "Hm, das ist kein gültiger Klassen-Code."; sfx?.play(Sound.WRONG, volume = 0.5f) }
        else { set = s; error = null; sfx?.play(Sound.FANFARE); particles.burst(Burst.CONFETTI, count = 60) }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("🏫 Klassen-Code") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } })
    }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            val s = set
            if (s == null) Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Brummi(if (error != null) Mood.THINK else Mood.HAPPY, size = 110.dp,
                    speech = error ?: "Deine Lehrerin oder dein Lehrer hat einen QR-Code? Scann ihn – dann wächst das Beet bei dir!")
                Spacer(Modifier.height(20.dp))
                JuicyButton(onClick = {
                    runCatching {
                        val opts = com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions.Builder()
                            .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE).build()
                        com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(ctx, opts).startScan()
                            .addOnSuccessListener { b -> b.rawValue?.let { accept(it) } }
                            .addOnFailureListener { error = "Der Scanner ist gerade nicht verfügbar – füge den Link unten ein." }
                    }.onFailure { error = "Der Scanner ist gerade nicht verfügbar – füge den Link unten ein." }
                }, modifier = Modifier.fillMaxWidth().height(60.dp)) { Text("📷 QR-Code scannen", style = MaterialTheme.typography.titleMedium) }
                Spacer(Modifier.height(20.dp))
                Text("… oder Link / Code einfügen:", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(text, { text = it; error = null }, Modifier.fillMaxWidth(), placeholder = { Text("https://…/klasse/#WGK1-…") }, maxLines = 3)
                TextButton(onClick = { accept(text) }, enabled = text.isNotBlank()) { Text("Öffnen") }
            } else Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                Text("${s.emoji} ${s.title}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                Text("${s.cards.size} Karten warten auf dich", style = MaterialTheme.typography.bodyMedium)
                LazyColumn(Modifier.weight(1f).padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(s.cards) { i, c -> PoppingCard(i, c.img, c.q, c.a) }
                }
                JuicyButton(onClick = { sfx?.play(Sound.GROW); onPlant(s) }, modifier = Modifier.fillMaxWidth().height(58.dp).padding(bottom = 4.dp)) {
                    Text("🌱 Als Beet pflanzen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
            }
            ParticleLayer(particles, Modifier.matchParentSize())
        }
    }
}
