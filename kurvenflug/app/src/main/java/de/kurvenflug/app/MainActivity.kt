package de.kurvenflug.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import de.kurvenflug.core.model.Daily
import de.kurvenflug.core.model.Levels
import de.kurvenflug.core.model.MusicMood

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()
    /** Duell-Code, der per „Teilen“ (z. B. aus WhatsApp) hereinkam */
    private val incomingDuel = androidx.compose.runtime.mutableStateOf<String?>(null)

    private fun readShared(intent: android.content.Intent?) {
        val text = intent?.takeIf { it.action == android.content.Intent.ACTION_SEND }?.getStringExtra(android.content.Intent.EXTRA_TEXT) ?: return
        if (de.kurvenflug.core.model.Codec.detect(text) != null) incomingDuel.value = text
    }

    /** Level, das ein Widget-Knopf öffnen soll (kurvenflug://level/<id>) */
    private val widgetLevel = androidx.compose.runtime.mutableStateOf<String?>(null)

    private fun readWidget(intent: android.content.Intent?) {
        val uri = intent?.data?.takeIf { it.scheme == "kurvenflug" && it.host == "level" } ?: return
        val id = uri.lastPathSegment ?: return
        val known = Levels.byId(id) != null || Daily.isDaily(id) || de.kurvenflug.core.model.Learning.levelFor(id) != null
        if (known) widgetLevel.value = id
    }

    override fun onNewIntent(intent: android.content.Intent) { super.onNewIntent(intent); readShared(intent); readWidget(intent) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as KurvenflugApp
        if (savedInstanceState == null) { readShared(intent); readWidget(intent) }
        (application as KurvenflugApp).music.prepare(MusicMood.MENU, MusicMood.FLIGHT)
        val platform = AndroidPlatform(this, BuildConfig.VERSION_NAME, BuildConfig.SELF_UPDATE, app.sfx, app.music)
        setContent { KurvenflugRoot(vm.state, platform, incomingDuel, widgetLevel) }
    }

    override fun onStart() {
        super.onStart()
        (application as KurvenflugApp).music.onForeground(true)
        // Auch beim Zurückkehren aus dem Hintergrund nach neuen Versionen schauen
        vm.checkForUpdateIfDue(BuildConfig.VERSION_NAME)
    }
    override fun onStop() { (application as KurvenflugApp).music.onForeground(false); super.onStop() }
}
