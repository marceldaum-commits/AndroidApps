package de.experimentopia.app.ui.fx

import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.math.floor

// Was Android und der Browser verschieden lösen. Die Bildschirme kennen nur diese Schnittstellen; jede Plattform füllt sie auf ihre Art.

/** Kurze Geräusche und Töne fester Höhe. Beide werden im Code erzeugt ([Synth]) – keine Audiodateien. */
interface SoundPlayer {
    fun play(s: Sound, pitch: Float = 1f, volume: Float = 0.9f)
    /** Ton der Frequenz [hz] (Klangröhren, Gläser, Saite) */
    fun tone(hz: Double, timbre: Timbre)
}
val LocalSfx = staticCompositionLocalOf<SoundPlayer?> { null }

/** Vibration ein/aus (Einstellungen). */
val LocalVibration = staticCompositionLocalOf { true }
enum class Buzz { TICK, HIT, BIG }
/** Kurzes Ruckeln – auf Geräten ohne Vibration passiert einfach nichts */
fun interface Buzzer { fun buzz(b: Buzz, enabled: Boolean) }
val LocalBuzzer = staticCompositionLocalOf<Buzzer> { Buzzer { _, _ -> } }

/** Runden wie Math.round (x,5 nach oben) – gibt es im Browser nicht */
fun roundI(x: Float): Int = floor(x + 0.5f).toInt()
fun roundI(x: Double): Long = floor(x + 0.5).toLong()

/** Datum aus Millisekunden (UTC) als TT.MM.JJJJ – ohne Java-Bibliotheken */
fun dateText(millis: Long): String {
    // Tage seit 1.1.1970 → Kalenderdatum (Algorithmus von Howard Hinnant)
    val z = millis / 86_400_000L + 719_468
    val era = (if (z >= 0) z else z - 146_096) / 146_097
    val doe = z - era * 146_097
    val yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = if (mp < 10) mp + 3 else mp - 9
    val y = yoe + era * 400 + if (m <= 2) 1 else 0
    return "${d.toString().padStart(2, '0')}.${m.toString().padStart(2, '0')}.$y"
}

/** Hintergrundmusik: je Welt eigene, immer neue Phrasen mit Pausen dazwischen; [world] null = Stille (z. B. im Versuch) */
interface MusicPlayer {
    fun world(w: de.experimentopia.core.model.World?)
    /** Erkennungsmelodie (einmal nach der ersten Berührung) */
    fun jingle()
    fun configure(p: de.experimentopia.core.model.AudioPrefs)
    /** Leiser stellen, während vorgelesen wird */
    fun duck(on: Boolean)
    /** Browser erlauben Ton erst nach dem ersten Antippen; Android spielt gleich beim Start */
    val needsTouch: Boolean get() = true
}
val LocalMusic = staticCompositionLocalOf<MusicPlayer?> { null }

/**
 * Vorlesen mit der Stimme „Nova“ (KI-Stimme von OpenAI, vorab auf unserem Stimmen-Server vertont); ohne Internet liest die Stimme des Geräts bzw. Browsers.
 * Die Texte werden vorher mit [de.experimentopia.core.model.Speech.clean] sprechbar gemacht und in Sätze geteilt ([de.experimentopia.core.model.Nova]).
 */
interface Reader {
    val speaking: Boolean
    /** Nova war in dieser Sitzung nicht erreichbar und die Gerätestimme ist eingesprungen (für einen einmaligen Hinweis) */
    val novaDown: Boolean get() = false
    fun say(text: String)
    fun stop()
    fun configure(p: de.experimentopia.core.model.AudioPrefs)
    /** Alle festen Nova-Sätze für unterwegs ohne Internet */
    val offline: OfflineVoice
    fun refreshOffline()
    /** [best]: Bestqualität (unkomprimiert, 128 kbit/s) statt Standard */
    fun loadOffline(best: Boolean)
    fun cancelOffline()
    fun deleteOffline()
}

/** Stand der Offline-Stimme: Größe aller Sätze laut Server ([totalBytes], null = unbekannt), schon gespeichert, Fortschritt beim Laden */
data class OfflineVoice(
    val totalBytes: Long? = null,
    val totalBestBytes: Long? = null,
    val savedBytes: Long = 0,
    val loading: Boolean = false,
    val done: Int = 0,
    val count: Int = 0,
    val complete: Boolean = false,
    val note: String? = null,
)
val LocalReader = staticCompositionLocalOf<Reader?> { null }
/** Automatisch vorlesen (Einstellung) */
val LocalAutoRead = staticCompositionLocalOf { false }
/** Vorlesen eingeschaltet (Einstellung) – dann gibt es überall 🔊-Knöpfe */
val LocalReadOn = staticCompositionLocalOf { false }

/** Link im Browser öffnen (Kasse, Impressum) */
val LocalOpenLink = staticCompositionLocalOf<(String) -> Unit> { {} }
