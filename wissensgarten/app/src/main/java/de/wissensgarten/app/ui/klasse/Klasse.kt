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
import de.wissensgarten.core.model.EmojiLexicon
import de.wissensgarten.core.model.Worksheet
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

/** Ein erkannter Eintrag vom Arbeitsblatt. */
private class Found(val q: String, val a: String, val img: String?, selected: Boolean = true) {
    var selected by mutableStateOf(selected)
}

/** Zeilen nach Höhe zusammenfassen – so werden auch zweispaltige Vokabellisten erkannt. */
private fun rowsFrom(lines: List<Pair<String, Rect>>): String {
    val sorted = lines.sortedBy { it.second.centerY() }
    val rows = mutableListOf<MutableList<Pair<String, Rect>>>()
    for (l in sorted) {
        val r = rows.lastOrNull()
        val ref = r?.first()?.second
        if (ref != null && kotlin.math.abs(l.second.centerY() - ref.centerY()) < ref.height() * 0.6f) r += l else rows += mutableListOf(l)
    }
    return rows.joinToString("\n") { row -> row.sortedBy { it.second.left }.joinToString("\t") { it.first } }
}

/**
 * Foto vom Arbeitsblatt → Karten. Die Texterkennung läuft komplett auf dem Gerät (ML Kit),
 * das Foto wird nicht gespeichert und nirgends hochgeladen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorksheetScreen(bedName: String, lexicon: () -> EmojiLexicon, onAdd: (List<ClassCard>) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val sfx = LocalSfx.current
    val scope = rememberCoroutineScope()
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var found by remember { mutableStateOf<List<Found>?>(null) }
    val particles = rememberParticles()

    fun recognize(bmp: Bitmap) {
        photo = bmp; scanning = true; found = null
        sfx?.play(Sound.WATER, volume = 0.5f)
        runCatching {
            com.google.mlkit.vision.text.TextRecognition.getClient(com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(com.google.mlkit.vision.common.InputImage.fromBitmap(bmp, 0))
                .addOnSuccessListener { res ->
                    val lines = res.textBlocks.flatMap { b -> b.lines.mapNotNull { l -> l.boundingBox?.let { l.text to it } } }
                    val pairs = (Worksheet.parse(rowsFrom(lines)).ifEmpty { Worksheet.parse(res.text) })
                    val lex = lexicon()
                    scope.launch {
                        delay(600) // die Scan-Animation einmal durchlaufen lassen
                        found = pairs.map { (q, a) -> Found(q, a, lex.guess(a, q)) }
                        scanning = false
                        if (pairs.isNotEmpty()) { sfx?.play(Sound.FANFARE); particles.burst(Burst.SPARKLE, count = 40) } else sfx?.play(Sound.WRONG, volume = 0.4f)
                    }
                }
                .addOnFailureListener { scanning = false; found = emptyList() }
        }.onFailure { scanning = false; found = emptyList() }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { it?.let(::recognize) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            val src = android.graphics.ImageDecoder.createSource(ctx.contentResolver, uri)
            android.graphics.ImageDecoder.decodeBitmap(src) { d, info, _ ->
                d.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                val max = maxOf(info.size.width, info.size.height)
                if (max > 2000) d.setTargetSampleSize(max / 2000 + 1)
            }
        }.getOrNull()?.let(::recognize)
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("📷 Arbeitsblatt → $bedName", maxLines = 1) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } })
    }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                val f = found
                if (photo == null) {
                    Spacer(Modifier.height(12.dp))
                    Brummi(Mood.HAPPY, size = 100.dp, modifier = Modifier.align(Alignment.CenterHorizontally),
                        speech = "Fotografier deine Vokabelliste oder deinen Lernzettel – ich mache Karten daraus!")
                    Spacer(Modifier.height(16.dp))
                    Text("Am besten klappt’s mit „Wort – Bedeutung“ pro Zeile oder zwei Spalten.", style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                photo?.let { bmp ->
                    if (f == null || f.isEmpty()) ScanningPhoto(bmp, scanning, Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 300.dp).padding(top = 8.dp))
                }
                if (f != null) {
                    if (f.isEmpty()) Text("Hm, da konnte ich keine Paare finden. Probier ein schärferes Foto, gerade von oben, mit „Wort – Bedeutung“.",
                        textAlign = TextAlign.Center, modifier = Modifier.padding(12.dp).fillMaxWidth())
                    else {
                        Text("Ich habe ${f.size} Karten gefunden! Tipp weg, was nicht passt.", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                        LazyColumn(Modifier.weight(1f).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            itemsIndexed(f) { i, it ->
                                PoppingCard(i, it.img, it.q, it.a) { Checkbox(it.selected, { v -> it.selected = v; sfx?.play(Sound.TAP) }) }
                            }
                        }
                        val n = f.count { it.selected }
                        JuicyButton(onClick = {
                            onAdd(f.filter { it.selected }.map { ClassCard(it.q, it.a, it.img) })
                            sfx?.play(Sound.GROW)
                            onBack()
                        }, enabled = n > 0, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("🌱 $n Karten pflanzen", fontWeight = FontWeight.Bold) }
                    }
                }
                if (!scanning) Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val takePhoto = { runCatching { camera.launch(null) }; Unit }
                    val pick = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    if (photo != null) {
                        OutlinedButton(onClick = takePhoto, modifier = Modifier.weight(1f)) { Text("📷 Neues Foto") }
                        OutlinedButton(onClick = pick, modifier = Modifier.weight(1f)) { Text("🖼️ Galerie") }
                    } else {
                        JuicyButton(onClick = takePhoto, modifier = Modifier.weight(1f).height(56.dp)) { Text("📷 Foto machen") }
                        JuicyButton(onClick = pick, modifier = Modifier.weight(1f).height(56.dp)) { Text("🖼️ Aus Galerie") }
                    }
                }
            }
            ParticleLayer(particles, Modifier.matchParentSize())
        }
    }
}

/** Foto mit grünem Scan-Balken, der darüber gleitet, solange erkannt wird. */
@Composable
private fun ScanningPhoto(bmp: Bitmap, scanning: Boolean, modifier: Modifier) {
    val img = remember(bmp) { bmp.asImageBitmap() }
    val y by rememberInfiniteTransition(label = "scan").animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "y")
    Box(modifier.clip(RoundedCornerShape(18.dp))) {
        Image(img, null, Modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
        if (scanning) Canvas(Modifier.matchParentSize()) {
            val yy = size.height * y
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0x6643A047)), startY = yy - 60f, endY = yy), Offset(0f, yy - 60f), Size(size.width, 60f))
            drawLine(Color(0xFF43A047), Offset(0f, yy), Offset(size.width, yy), strokeWidth = 6f)
        }
    }
}
