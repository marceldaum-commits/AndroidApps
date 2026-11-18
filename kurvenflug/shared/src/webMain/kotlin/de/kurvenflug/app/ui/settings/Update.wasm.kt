package de.kurvenflug.app.ui.settings

import androidx.compose.runtime.Composable

/** Im Browser gibt es keinen Update-Knopf: neue Versionen kommen beim nächsten Öffnen von selbst. */
@Composable
actual fun PlatformUpdateSection(installed: String, initial: UpdateState, autoCheck: Boolean, onAutoCheck: ((Boolean) -> Unit)?) {}
