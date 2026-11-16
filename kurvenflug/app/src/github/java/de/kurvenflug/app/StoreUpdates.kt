package de.kurvenflug.app

import androidx.activity.ComponentActivity

/** GitHub-Variante: Updates laufen über den eigenen Update-Knopf (siehe AppViewModel.checkForUpdateIfDue). */
class StoreUpdates(@Suppress("unused") private val activity: ComponentActivity) {
    fun onStart() {}
    fun onStop() {}
}
