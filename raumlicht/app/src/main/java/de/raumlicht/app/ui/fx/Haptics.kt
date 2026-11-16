package de.raumlicht.app.ui.fx

import android.view.HapticFeedbackConstants
import android.view.View

/** Android: Vibration über die Ansicht (keine Berechtigung nötig). */
object Haptic {
    fun View.buzzNow(b: Buzz, enabled: Boolean) {
        if (!enabled) return
        performHapticFeedback(when (b) {
            Buzz.TICK -> HapticFeedbackConstants.CLOCK_TICK
            Buzz.HIT -> HapticFeedbackConstants.CONFIRM.takeIf { android.os.Build.VERSION.SDK_INT >= 30 } ?: HapticFeedbackConstants.VIRTUAL_KEY
            Buzz.CRASH -> HapticFeedbackConstants.LONG_PRESS
        })
    }
}
