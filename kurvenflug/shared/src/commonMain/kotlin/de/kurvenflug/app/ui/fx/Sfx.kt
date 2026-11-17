package de.kurvenflug.app.ui.fx

import androidx.compose.runtime.staticCompositionLocalOf

/** Spielt die Spiel-Geräusche (im Code erzeugt, siehe [Synth]). Android: SoundPool, Browser: Web Audio. */
abstract class Sfx {
    var enabled = true
    /** Lautstärke der Effekte 0…1 (Einstellungen) */
    var level = 0.9f

    /** @param pitch 1 = normal, >1 höher (z. B. bei Serien) */
    abstract fun play(s: Sound, pitch: Float = 1f, volume: Float = 0.9f)

    /**
     * Volts Plapper-Stimme: eine kurze Silbe je Wortteil, die Tonhöhe folgt den Vokalen im Text.
     * Höchstens 14 Silben, damit es niedlich bleibt und nicht nervt.
     */
    suspend fun babble(text: String) {
        if (!enabled) return
        val vowels = text.lowercase().filter { it in "aeiouäöü" }.take(14)
        for (c in vowels) {
            val pitch = when (c) { 'i' -> 1.45f; 'e' -> 1.25f; 'a' -> 1.0f; 'ä' -> 1.15f; 'o' -> 0.85f; 'ö' -> 0.95f; else -> 0.75f }
            play(Sound.VOICE, pitch, 0.35f)
            kotlinx.coroutines.delay(75)
        }
    }

}

val LocalSfx = staticCompositionLocalOf<Sfx?> { null }
