package de.kurvenflug.app.ui.fx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** Vibration ein/aus (Einstellungen). */
val LocalVibration = staticCompositionLocalOf { true }

enum class Buzz { TICK, HIT, CRASH }

/** Kurzes Ruckeln – Android: Haptik der Ansicht, Browser: navigator.vibrate (wo vorhanden). */
fun interface Buzzer { fun buzz(b: Buzz, enabled: Boolean) }

@Composable
expect fun rememberBuzzer(): Buzzer

/** Zurück-Taste (Android) bzw. Zurück im Browser / Esc-Taste */
@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)
