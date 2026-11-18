package de.wissensgarten.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import de.wissensgarten.app.ui.fx.Listener
import de.wissensgarten.app.ui.fx.Music
import de.wissensgarten.app.ui.fx.Sfx
import de.wissensgarten.app.ui.fx.Voice
import de.wissensgarten.app.ui.share.CraftItem
import de.wissensgarten.app.ui.share.KeepsakeData
import de.wissensgarten.core.model.AppRelease
import kotlinx.coroutines.launch

/** Was es auf einer Plattform gibt – fehlende Funktionen blendet die Oberfläche aus. */
data class Features(
    /** Eigener Updater (GitHub-APK) */
    val selfUpdate: Boolean = false,
    /** Wissens-Safari (Kamera + Bilderkennung) */
    val safari: Boolean = false,
    /** Wissensgarten Plus über die Website kaufen (Web/GitHub-APK; in der Play-Fassung nein) */
    val plusPurchase: Boolean = false,
    /** Wissenspakete ohne App-Update nachladen (nur Daten) */
    val packUpdates: Boolean = false,
    /** Tägliche Gieß-Erinnerung als Benachrichtigung */
    val reminders: Boolean = false,
    /** Geräte-Sync über Google Drive */
    val sync: Boolean = false,
    /** QR-Codes mit der Kamera scannen */
    val qrScan: Boolean = false,
    /** Kipp-Spiel mit Lagesensor */
    val tilt: Boolean = false,
    /** Postkarte und Urkunde zum Verschicken */
    val keepsakes: Boolean = true,
    /** Startbildschirm-Widget */
    val widget: Boolean = false,
    /** Name für Texte, z. B. „App“ oder „WebApp“ */
    val label: String = "App",
)

/**
 * Plattform-Brücke: Android-App und WebApp liefern je eine Umsetzung, die Oberfläche ist dieselbe.
 */
interface AppHost {
    val features: Features
    val sfx: Sfx
    val music: Music
    val voice: Voice?
    /** Spracherkennung (Brummi-Radio, Sprech-Spiel); null = gibt es nicht */
    val listener: Listener?
    /** Gespeicherte Bilder (selbst gemalte Merkbilder, Safari-Fotos) */
    val images: ImageStore

    fun toast(message: String)
    /** WebApp: Ist gerade ein ungefährlicher Moment für ein automatisches Update (Startbildschirm, keine Runde)? */
    fun setSafeToUpdate(safe: Boolean) {}
    fun shareText(text: String)
    fun shareImage(image: ImageBitmap, fileName: String, text: String)
    fun printCraftSheet(bedName: String, emoji: String, items: List<CraftItem>)
    suspend fun sharePostcard(data: KeepsakeData) { toast("Die Postkarte gibt es in der Android-App.") }
    suspend fun shareCertificate(data: KeepsakeData) { toast("Die Urkunde gibt es in der Android-App.") }
    /** „Alle Daten löschen“ – liefert eine Fehlermeldung (z. B. Drive offline) oder null */
    suspend fun wipeAll(): String?
    fun restart()
    fun refreshWidget() {}
    fun setReminder(on: Boolean, hour: Int) {}
    fun syncSoon(minGapMs: Long = 0) {}
    /** Gesunde Lernzeit: bisherige Vordergrund-Zeit speichern */
    fun flushUsage() {}
    suspend fun checkUpdate(installed: String): AppRelease? = null
    /** Neue Wissenspakete (nur Daten) nachladen; liefert die Zahl neuer/aktualisierter Pakete. Web: kommt mit der WebApp. */
    suspend fun fetchPackUpdates(known: Map<String, Int>): Int = 0
    /** Plus-Pakete vom Plus-Endpunkt laden (nur mit gültigem Code); liefert die Zahl neuer/aktualisierter Pakete. */
    suspend fun fetchPlusPacks(code: String, known: Map<String, Int>): Int = 0
    /** QR-Code mit der Kamera scannen (nur wenn features.qrScan) */
    fun scanQr(onResult: (String?) -> Unit) { onResult(null) }
    /** Spracherkennung vorhanden? (Sprech-Spiel) */
    fun speechAvailable(): Boolean = listener?.available() == true
    /** Seitliche Neigung des Geräts (Kipp-Spiel); null = kein Sensor */
    fun tilt(): kotlinx.coroutines.flow.Flow<Float>? = null
    /** Bildschirm anlassen (Brummi-Radio) */
    fun keepScreenOn(on: Boolean) {}

    /** Antwort sprechen lassen (Sprech-Spiel): liefert eine Funktion, die zuhört und die erkannten Texte meldet. */
    @Composable fun rememberSpeechPrompt(onResult: (List<String>) -> Unit): (de.wissensgarten.app.ui.fx.Locale, String) -> Unit {
        val l = listener
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        return { locale, _ ->
            scope.launch {
                val h = l?.listen(locale)
                onResult((h as? de.wissensgarten.app.ui.fx.Heard.Words)?.list.orEmpty())
            }
        }
    }
    @Composable fun rememberMicPermission(): de.wissensgarten.app.ui.radio.MicPermission = de.wissensgarten.app.ui.radio.MicPermission.Granted
    @Composable fun SyncCard() {}
    @Composable fun UpdateSection(installed: String, update: AppRelease?, autoUpdate: Boolean, onAutoUpdate: (Boolean) -> Unit) {}
    @Composable fun WelcomeRestore(onRestored: (Boolean) -> Unit) {}
    /** Berechtigung für Benachrichtigungen anfragen; liefert eine Funktion zum Auslösen */
    @Composable fun rememberNotificationPermission(onResult: (Boolean) -> Unit): () -> Unit = { onResult(false) }
    @Composable fun SafariScreen(found: Int, englishDefault: Boolean, onPlant: (de.wissensgarten.core.model.Safari.Word, Boolean, ImageBitmap?) -> Unit, onBack: () -> Unit) {}
}

/** Bilder dauerhaft ablegen (Android: Dateien, Web: Browser-Speicher). */
interface ImageStore {
    fun save(name: String, image: ImageBitmap)
    fun load(name: String): ImageBitmap?
    fun exists(name: String): Boolean
    fun delete(name: String)
}
