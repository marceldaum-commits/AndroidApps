package de.wissensgarten.app.platform

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.speech.RecognizerIntent
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.compose.LifecycleResumeEffect
import de.wissensgarten.app.*
import de.wissensgarten.app.ui.common.decodeImage
import de.wissensgarten.app.ui.common.encodePng
import de.wissensgarten.app.ui.fx.Locale
import de.wissensgarten.app.ui.radio.MicPermission
import de.wissensgarten.app.ui.share.CraftItem
import de.wissensgarten.app.ui.share.CraftSheet
import de.wissensgarten.app.ui.share.KeepsakeData
import de.wissensgarten.app.ui.share.Keepsakes
import de.wissensgarten.app.widget.GardenWidget
import de.wissensgarten.core.model.AppRelease
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File

/** Android-Seite der Plattform-Brücke: alles, was es nur auf dem Handy gibt. */
class AndroidHost(private val activity: Activity, private val onFlushUsage: () -> Unit) : AppHost {
    private val app = activity.application as WissensgartenApp
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override val features = Features(
        selfUpdate = BuildConfig.SELF_UPDATE, safari = BuildConfig.SAFARI, reminders = true, sync = true, packUpdates = true, plusPurchase = BuildConfig.SELF_UPDATE,
        qrScan = true, tilt = true, keepsakes = true, widget = true, label = "App",
    )
    override val sfx get() = app.sfx
    override val music get() = app.music
    override val voice get() = app.voice
    override val listener by lazy { AndroidListener(activity) }
    override val images = object : ImageStore {
        private val dir = File(activity.filesDir, "merkbilder").apply { mkdirs() }
        private fun f(name: String) = File(dir, "$name.png")
        override fun save(name: String, image: ImageBitmap) { f(name).writeBytes(image.encodePng()) }
        override fun load(name: String) = f(name).takeIf { it.exists() }?.readBytes()?.let { decodeImage(it) }
        override fun exists(name: String) = f(name).exists()
        override fun delete(name: String) { f(name).delete() }
    }

    override fun toast(message: String) = android.widget.Toast.makeText(activity, message, android.widget.Toast.LENGTH_LONG).show()
    override fun shareText(text: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        activity.startActivity(Intent.createChooser(send, "Teilen").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    override fun shareImage(image: ImageBitmap, fileName: String, text: String) {
        val f = Keepsakes.savePng(activity, image.asAndroidBitmap(), fileName)
        Keepsakes.share(activity, f, "image/png", text)
    }
    override fun printCraftSheet(bedName: String, emoji: String, items: List<CraftItem>) {
        val f = CraftSheet.pdf(activity, bedName, emoji, items)
        Keepsakes.share(activity, f, "application/pdf", "✂️ Lernkarten zum Ausdrucken: $bedName")
    }
    override suspend fun sharePostcard(data: KeepsakeData) {
        val f = withContext(Dispatchers.Default) { Keepsakes.savePng(activity, Keepsakes.postcard(data), "Postkarte-${Keepsakes.safe(data.name)}.png") }
        Keepsakes.share(activity, f, "image/png", "💌 Grüße aus ${data.name}s Wissensgarten! 🌻")
    }
    override suspend fun shareCertificate(data: KeepsakeData) {
        val f = withContext(Dispatchers.Default) { Keepsakes.certificatePdf(activity, data) }
        Keepsakes.share(activity, f, "application/pdf", "🏅 Urkunde für ${data.name} aus dem Wissensgarten")
    }
    override suspend fun wipeAll(): String? = DataWipe.wipeAll(app)
    override fun restart() = DataWipe.restart(activity)
    override fun refreshWidget() { scope.launch { runCatching { GardenWidget().updateAll(activity) } } }
    override fun setReminder(on: Boolean, hour: Int) {
        if (on) de.wissensgarten.app.notify.Reminders.schedule(activity, hour) else de.wissensgarten.app.notify.Reminders.cancel(activity)
    }
    override fun syncSoon(minGapMs: Long) = app.sync.syncSoon(minGapMs)
    override fun flushUsage() = onFlushUsage()
    override suspend fun checkUpdate(installed: String): AppRelease? = de.wissensgarten.app.update.AppUpdater.check().getOrThrow()
    override suspend fun fetchPackUpdates(known: Map<String, Int>): Int = de.wissensgarten.app.update.PackDownloader.fetch(activity, known)
    override suspend fun fetchPlusPacks(code: String, known: Map<String, Int>): Int =
        de.wissensgarten.app.update.PackDownloader.fetch(activity, known, de.wissensgarten.app.PLUS_API, code)
    override fun keepScreenOn(on: Boolean) {
        val w = activity.window
        if (on) { w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); w.attributes = w.attributes.apply { screenBrightness = 0.02f } }
        else { w.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); w.attributes = w.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE } }
    }
    override fun scanQr(onResult: (String?) -> Unit) {
        runCatching {
            val opts = com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE).build()
            com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(activity, opts).startScan()
                .addOnSuccessListener { b -> b.rawValue?.let { onResult(it) } }
                .addOnFailureListener { onResult(null) }
        }.onFailure { onResult(null) }
    }
    override fun speechAvailable() = runCatching { android.speech.SpeechRecognizer.isRecognitionAvailable(activity) }.getOrDefault(false)
    override fun tilt(): Flow<Float>? = callbackFlow {
        val sm = activity.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val acc = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) { trySend(e.values[0]) }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (acc != null) sm.registerListener(l, acc, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sm?.unregisterListener(l) }
    }

    @Composable
    override fun rememberSpeechPrompt(onResult: (List<String>) -> Unit): (Locale, String) -> Unit {
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            onResult(if (res.resultCode == Activity.RESULT_OK) res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).orEmpty() else emptyList())
        }
        return { locale, prompt ->
            launcher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.tag)
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                .putExtra(RecognizerIntent.EXTRA_PROMPT, prompt))
        }
    }

    @Composable
    override fun rememberMicPermission(): MicPermission {
        fun has() = ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        var granted by remember { mutableStateOf(has()) }
        var askedOnce by remember { mutableStateOf(false) }
        val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it; askedOnce = true }
        // Zurück aus den Android-Einstellungen: neu prüfen
        LifecycleResumeEffect(Unit) { granted = has(); onPauseOrDispose { } }
        val blocked = askedOnce && !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
        return remember(granted, blocked) {
            object : MicPermission {
                override val granted = granted
                override val blocked = blocked
                override fun request() {
                    if (blocked) activity.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.parse("package:${activity.packageName}")))
                    else ask.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    }

    @Composable override fun rememberNotificationPermission(onResult: (Boolean) -> Unit) = de.wissensgarten.app.notify.rememberNotificationPermission(onResult)
    @Composable override fun SyncCard() = de.wissensgarten.app.ui.settings.SyncSectionLive(app.sync)
    @Composable override fun UpdateSection(installed: String, update: AppRelease?, autoUpdate: Boolean, onAutoUpdate: (Boolean) -> Unit) =
        de.wissensgarten.app.ui.settings.UpdateSection(installed, update?.let { de.wissensgarten.app.ui.settings.UpdateState.Available(it) }
            ?: de.wissensgarten.app.ui.settings.UpdateState.Idle, autoUpdate, onAutoUpdate)
    @Composable override fun WelcomeRestore(onRestored: (Boolean) -> Unit) = de.wissensgarten.app.ui.settings.WelcomeRestore(app.sync, onRestored)
    @Composable override fun SafariScreen(found: Int, englishDefault: Boolean, onPlant: (de.wissensgarten.core.model.Safari.Word, Boolean, ImageBitmap?) -> Unit, onBack: () -> Unit) =
        de.wissensgarten.app.ui.safari.SafariScreen(found, englishDefault, { w, en, bmp -> onPlant(w, en, bmp?.asImageBitmap()) }, onBack)
}
