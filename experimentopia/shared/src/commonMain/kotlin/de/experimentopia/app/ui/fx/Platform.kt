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
 * Vorlesen mit den eigenen Stimmen des Tonstudios (vorab vertonte MP3-Dateien auf unserem Server; keine Gerätestimme, kein fremder Sprachdienst).
 * Zurzeit aus ([de.experimentopia.core.model.Tonstudio.ENABLED]); die Texte werden mit [de.experimentopia.core.model.Speech.clean] sprechbar gemacht und in Sätze geteilt.
 * Gekennzeichnet als „KI-generierte Stimme“.
 */
interface Reader {
    val speaking: Boolean
    fun say(text: String)
    fun stop()
    fun configure(p: de.experimentopia.core.model.AudioPrefs)
}
val LocalReader = staticCompositionLocalOf<Reader?> { null }
/** Automatisch vorlesen (Einstellung) */
val LocalAutoRead = staticCompositionLocalOf { false }
/** Vorlesen eingeschaltet (Einstellung) – dann gibt es überall 🔊-Knöpfe */
val LocalReadOn = staticCompositionLocalOf { false }

/** „App installieren“ (WebApp als PWA): ausgeblendet, Installations-Dialog des Browsers, oder Anleitung fürs iPhone/iPad */
enum class InstallState { HIDDEN, PROMPT, IOS }
interface Installer {
    val state: InstallState
    fun install()
}
val LocalInstaller = staticCompositionLocalOf<Installer?> { null }

/** Link im Browser öffnen (Kasse, Impressum) */
val LocalOpenLink = staticCompositionLocalOf<(String) -> Unit> { {} }

/**
 * Eltern-Kopplung: was die Plattform beisteuert – sichere Zufallsbytes, HTTP zum Eltern-Server, Teilen, Speicher.
 * null = gibt es auf dieser Plattform nicht (dann bleibt der Knopf verborgen).
 */
interface ParentSync {
    /** kryptografisch sichere Zufallsbytes (Browser: crypto.getRandomValues) */
    fun random(n: Int): ByteArray
    /** PUT/DELETE mit Kopfzeile X-Schreib; [done] bekommt den HTTP-Status (0 = kein Netz) */
    fun send(method: String, url: String, write: String, body: String?, done: (Int) -> Unit)
    /** Link teilen (Teilen-Menü) oder kopieren; [done]: "geteilt", "kopiert" oder "" */
    fun share(title: String, url: String, done: (String) -> Unit)
    /** gespeicherte Kopplung (JSON) – nur auf diesem Gerät */
    var stored: String?
    /** App läuft auf der Test-Stufe → Test-Server */
    val test: Boolean
    fun now(): Long
    /** Zeitzone des Geräts in Millisekunden (für die Tage im Bericht) */
    val offsetMs: Long
    /** GET ohne Schreibrecht (Sicherung holen); [done] bekommt Status und Antworttext */
    fun get(url: String, done: (Int, String) -> Unit)
    /** Der ganze Speicherstand der App (Fortschritt, Forscherbuch, Einstellungen, Monatssummen, Pass-Code) – ohne Geräte-IDs und ohne die Kopplung selbst */
    fun snapshot(): Map<String, String>
    /** Speicherstand ersetzen und die App neu starten */
    fun restore(data: Map<String, String>)
    /** dauerhafte Monatssummen (JSON, [de.experimentopia.core.model.Monthly]) – Teil des Speicherstands */
    var months: String?
    /** Version der App (steht in der Sicherung) */
    val version: String
    /** Name des Forscherbuchs im Speicherstand (wird gekürzt, wenn die Sicherung zu groß wird) */
    val journalKey: String
}
val LocalParentSync = staticCompositionLocalOf<ParentSync?> { null }

/** Anonyme Nutzungszählung ([de.experimentopia.core.model.Zaehlung]) – abschaltbar im Elternbereich; null = gibt es hier nicht */
interface Counter {
    val enabled: kotlinx.coroutines.flow.StateFlow<Boolean>
    fun setEnabled(on: Boolean)
    /** sendet nur, wenn eingeschaltet und Ereignis/Ort erlaubt sind */
    fun count(ev: String, ort: String? = null, thema: String? = null)
}
val LocalCounter = staticCompositionLocalOf<Counter?> { null }

/** „← Alle Apps“ zur Website: im Browser im selben Tab, in der installierten App im normalen Browser; null = Knopf ausblenden */
val LocalAllApps = staticCompositionLocalOf<(() -> Unit)?> { null }

/** App läuft auf der Test-Stufe (experimentopia-test…/lokal) → Querverweise zeigen auf die Test-Seiten der anderen Apps */
val LocalTestStage = staticCompositionLocalOf { false }
