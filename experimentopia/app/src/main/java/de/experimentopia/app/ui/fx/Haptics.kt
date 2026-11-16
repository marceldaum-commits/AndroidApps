package de.experimentopia.app.ui.fx

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.staticCompositionLocalOf

/** Vibration ein/aus (Einstellungen). */
val LocalVibration = staticCompositionLocalOf { true }

enum class Buzz { TICK, HIT, BIG }

fun View.buzz(b: Buzz, enabled: Boolean) {
    if (!enabled) return
    performHapticFeedback(when (b) {
        Buzz.TICK -> HapticFeedbackConstants.CLOCK_TICK
        Buzz.HIT -> if (android.os.Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
        Buzz.BIG -> HapticFeedbackConstants.LONG_PRESS
    })
}
