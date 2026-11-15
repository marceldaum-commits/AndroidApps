package de.kurvenflug.app.ui.fx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import de.kurvenflug.app.jsOnBack
import de.kurvenflug.app.jsVibrate

@Composable
actual fun rememberBuzzer(): Buzzer = remember {
    Buzzer { b, enabled -> if (enabled) jsVibrate(when (b) { Buzz.TICK -> 8; Buzz.HIT -> 25; Buzz.CRASH -> 60 }) }
}

/** Zurück im Browser (Zurück-Knopf, Wischgeste) oder Esc: der zuletzt angemeldete, aktive Zurück-Wunsch gewinnt – wie auf Android. */
internal object WebBack {
    class Entry(var enabled: Boolean, var onBack: () -> Unit)
    val stack = mutableListOf<Entry>()
    private var installed = false
    fun install() {
        if (installed) return
        installed = true
        jsOnBack { stack.lastOrNull { it.enabled }?.let { it.onBack(); true } ?: false }
    }
}

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    val entry = remember { WebBack.Entry(enabled, onBack) }
    SideEffect { entry.enabled = enabled; entry.onBack = onBack }
    DisposableEffect(entry) {
        WebBack.install(); WebBack.stack += entry
        onDispose { WebBack.stack -= entry }
    }
}
