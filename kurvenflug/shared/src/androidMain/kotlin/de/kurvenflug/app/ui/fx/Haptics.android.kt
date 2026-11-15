package de.kurvenflug.app.ui.fx

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

@Composable
actual fun rememberBuzzer(): Buzzer {
    val view = LocalView.current
    return remember(view) {
        Buzzer { b, enabled ->
            if (enabled) view.performHapticFeedback(when (b) {
                Buzz.TICK -> HapticFeedbackConstants.CLOCK_TICK
                Buzz.HIT -> HapticFeedbackConstants.CONFIRM.takeIf { android.os.Build.VERSION.SDK_INT >= 30 } ?: HapticFeedbackConstants.VIRTUAL_KEY
                Buzz.CRASH -> HapticFeedbackConstants.LONG_PRESS
            })
        }
    }
}

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) = androidx.activity.compose.BackHandler(enabled, onBack)
