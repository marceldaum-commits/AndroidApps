package de.raumlicht.app.ui.fx

import androidx.compose.runtime.staticCompositionLocalOf

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

/** Text weitergeben (Android: Teilen-Dialog, Web: Teilen oder Zwischenablage) */
val LocalShareText = staticCompositionLocalOf<(String) -> Unit> { {} }
/** Ergebnis-Karte als Bild teilen: Titel, Punkte, Urteil. Wo es das nicht gibt, bleibt der Knopf ohne Wirkung. */
val LocalShareCard = staticCompositionLocalOf<(String, String, String) -> Unit> { { _, _, _ -> } }

/** Uhrzeit in Millisekunden (für Zufalls-Startwerte und Abstände zwischen Ereignissen) */
expect fun nowMillis(): Long
expect fun nowNanos(): Long
