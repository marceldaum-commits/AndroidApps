package de.raumlicht.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.raumlicht.app.BuildConfig
import de.raumlicht.app.ui.theme.Q

/** Android: die gemeinsame Einstellungsseite, ergänzt um den App-Update-Abschnitt und die Versionsangabe aus dem Bau. */
@Composable
fun SettingsScreen(
    music: Boolean = true, onMusic: (Boolean) -> Unit = {},
    sound: Boolean, onSound: (Boolean) -> Unit,
    vibration: Boolean, onVibration: (Boolean) -> Unit,
    autoUpdate: Boolean, onAutoUpdate: (Boolean) -> Unit,
    installed: String,
    update: UpdateState,
    onBack: () -> Unit,
    onReset: () -> Unit = {},
    volume: Int = 100, onVolume: (Int) -> Unit = {},
    voice: Boolean = true, onVoice: (Boolean) -> Unit = {}, voiceOffered: Boolean = de.raumlicht.core.model.Vorlesen.ENABLED,
    offline: OfflineVoice? = null,
    lastProblem: String? = null, onProblemSeen: () -> Unit = {},
) = SettingsPage(music, onMusic, sound, onSound, vibration, onVibration, "Version $installed vom ${BuildConfig.VERSION_DATE}", onBack,
    top = { if (BuildConfig.SELF_UPDATE) UpdateSection(installed, update, autoUpdate, onAutoUpdate) },
    onReset = onReset, volume = volume, onVolume = onVolume, voice = voice, onVoice = onVoice, voiceOffered = voiceOffered, offline = offline,
    lastProblem = lastProblem, onProblemSeen = onProblemSeen, soundNote = silenceReason(),
    licenses = androidx.compose.ui.platform.LocalContext.current.let { ctx -> remember { runCatching { ctx.assets.open("lizenzen.txt").bufferedReader().use { it.readText() } }.getOrNull() } },
    privacy = "Raumlicht speichert nur den Spielstand auf diesem Gerät. Keine Anmeldung, keine Werbung, keine Käufe. Ins Internet geht die App nur, " +
        (if (BuildConfig.SELF_UPDATE) "um nach Updates zu schauen (abschaltbar)" else "wenn der Play Store nach Updates schaut") +
        de.raumlicht.core.model.Vorlesen.privacyNote)

/**
 * Warum Raumlicht gerade still ist, obwohl Musik und Geräusche eingeschaltet sind: Die Medien-Lautstärke des Handys steht auf 0.
 * (Der Klingelton-Schalter – stumm oder Vibration – zählt seit 2.18.12 nicht mehr: Laut ist, was die Medien-Lautstärke sagt.)
 * Wird jede Sekunde neu geprüft, damit der Hinweis verschwindet, sobald man lauter stellt.
 */
@Composable
fun silenceReason(): String? {
    val audio = androidx.compose.ui.platform.LocalContext.current.getSystemService(android.media.AudioManager::class.java)
    fun reason(): String? = runCatching {
        if (audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) == 0) "🔈 Die Medien-Lautstärke deines Handys steht auf 0. Drück die Lauter-Taste, dann hörst du Raumlicht wieder." else null
    }.getOrNull()
    var note by remember { mutableStateOf(reason()) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(1000); note = reason() } }
    return note
}
