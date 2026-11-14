package de.kurvenflug.app.ui.fx

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.staticCompositionLocalOf

/** Vibration ein/aus (Einstellungen). */
val LocalVibration = staticCompositionLocalOf { true }

enum class Buzz { TICK, HIT, CRASH }

fun View.buzz(b: Buzz, enabled: Boolean) {
    if (!enabled) return
    performHapticFeedback(when (b) {
        Buzz.TICK -> HapticFeedbackConstants.CLOCK_TICK
        Buzz.HIT -> HapticFeedbackConstants.CONFIRM.takeIf { android.os.Build.VERSION.SDK_INT >= 30 } ?: HapticFeedbackConstants.VIRTUAL_KEY
        Buzz.CRASH -> HapticFeedbackConstants.LONG_PRESS
    })
}
