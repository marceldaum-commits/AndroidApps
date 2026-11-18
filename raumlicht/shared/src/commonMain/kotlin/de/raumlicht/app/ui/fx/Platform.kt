package de.raumlicht.app.ui.fx

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

// Was Android und der Browser verschieden lösen. Die Bildschirme kennen nur diese Schnittstellen; jede Plattform füllt sie auf ihre Art.

/** Vibration ein/aus (Einstellungen). */
val LocalVibration = staticCompositionLocalOf { true }
enum class Buzz { TICK, HIT, CRASH }
/** Kurzes Ruckeln bei Treffern – auf Geräten ohne Vibration passiert einfach nichts */
fun interface Buzzer { fun buzz(b: Buzz, enabled: Boolean) }
val LocalBuzzer = staticCompositionLocalOf<Buzzer> { Buzzer { _, _ -> } }

/** Die Kurzklänge der App – jeder entspricht einem [de.raumlicht.core.model.Ton] aus der gläsernen Klangwelt (im Code erzeugt, in Android und WebApp derselbe Klang) */
enum class Sound { TAP, TICK, LAUNCH, HIT, CELL, CRASH, WIN, WRONG, BEEP, REFRACT, CHAPTER, JINGLE }
/** Kurze Geräusche. @param pitch 1 = normal, >1 höher (z. B. bei Serien) */
interface SoundPlayer { fun play(s: Sound, pitch: Float = 1f, volume: Float = 1f) }
val LocalSfx = staticCompositionLocalOf<SoundPlayer?> { null }

/** Wer Lux' Sprechblasen laut vorliest. */
fun interface Speaker {
    fun say(text: String)
    /** Nur sprechen, wenn Lux gerade nichts sagt – für Meldungen, die dicht aufeinander folgen können (sonst würde jede die vorige abschneiden) */
    fun sayIfQuiet(text: String) = say(text)
}
val LocalSpeaker = staticCompositionLocalOf<Speaker?> { null }

/**
 * Was die Stimme „Nova“ gerade tut – für zwei dezente Hinweise in der Oberfläche:
 * [loading] = ein Satz wird geholt (dauert es länger, erscheint „Nova lädt …“), [note] = eine Meldung, höchstens einmal je Sitzung („Nova gerade nicht erreichbar“).
 */
class VoiceStatus {
    var loading by androidx.compose.runtime.mutableStateOf(false)
    var note by androidx.compose.runtime.mutableStateOf<String?>(null)
    private var told = false
    /** Einmal je Sitzung sagen, dass Nova gerade nicht zu erreichen ist */
    fun unreachable() { if (!told) { told = true; note = UNREACHABLE } }
    companion object { const val UNREACHABLE = "Nova ist gerade nicht erreichbar." }
}
val LocalVoiceStatus = staticCompositionLocalOf<VoiceStatus?> { null }

/** Die zwei Hinweise zur Stimme: klein, oben in der Mitte, ohne etwas zu verdecken, das man antippen muss */
@androidx.compose.runtime.Composable
fun VoiceHints(modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    val status = LocalVoiceStatus.current ?: return
    // „lädt“ erst zeigen, wenn es spürbar dauert – sonst flackert es bei jedem Satz
    var slow by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(status.loading) { slow = false; if (status.loading) { kotlinx.coroutines.delay(800); slow = true } }
    androidx.compose.runtime.LaunchedEffect(status.note) { if (status.note != null) { kotlinx.coroutines.delay(5000); status.note = null } }
    val text = status.note ?: if (status.loading && slow) "🔊 Nova lädt …" else null
    androidx.compose.animation.AnimatedVisibility(text != null, modifier, enter = androidx.compose.animation.fadeIn(), exit = androidx.compose.animation.fadeOut()) {
        var last by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }; if (text != null) last = text
        androidx.compose.material3.Text(last, androidx.compose.ui.Modifier.padding(top = 6.dp).testTag("stimme-hinweis")
            .background(androidx.compose.ui.graphics.Color(0xE6101433), androidx.compose.foundation.shape.RoundedCornerShape(50))
            .border(1.dp, androidx.compose.ui.graphics.Color(0x8838E8FF), androidx.compose.foundation.shape.RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 5.dp),
            color = androidx.compose.ui.graphics.Color(0xFFDDE6FF), fontSize = 12.sp)
    }
}

/** Text weitergeben (Android: Teilen-Dialog, Web: Teilen oder Zwischenablage) */
/** Anonyme Nutzungszählung: (Ereignis, Ort, Thema) – sendet nur, wenn der Schalter an ist und die Plattform zählen kann; sonst ohne Wirkung */
val LocalCount = staticCompositionLocalOf<(event: String, place: String?, topic: String?) -> Unit> { { _, _, _ -> } }
val LocalShareText = staticCompositionLocalOf<(String) -> Unit> { {} }
/** Ergebnis-Karte als Bild teilen: Titel, Punkte, Urteil. Wo es das nicht gibt, bleibt der Knopf ohne Wirkung. */
val LocalShareCard = staticCompositionLocalOf<(String, String, String) -> Unit> { { _, _, _ -> } }

/** Uhrzeit in Millisekunden (für Zufalls-Startwerte und Abstände zwischen Ereignissen) */
expect fun nowMillis(): Long
expect fun nowNanos(): Long
