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
    voice: Boolean = true, onVoice: (Boolean) -> Unit = {}, voiceAvailable: Boolean = true,
    voiceControls: @Composable () -> Unit = {},
    lastProblem: String? = null, onProblemSeen: () -> Unit = {},
) = SettingsPage(music, onMusic, sound, onSound, vibration, onVibration, "Version $installed vom ${BuildConfig.VERSION_DATE}", onBack,
    top = { if (BuildConfig.SELF_UPDATE) UpdateSection(installed, update, autoUpdate, onAutoUpdate) },
    onReset = onReset, volume = volume, onVolume = onVolume, voice = voice, onVoice = onVoice, voiceAvailable = voiceAvailable, voiceControls = voiceControls,
    lastProblem = lastProblem, onProblemSeen = onProblemSeen,
    licenses = androidx.compose.ui.platform.LocalContext.current.let { ctx -> remember { runCatching { ctx.assets.open("lizenzen.txt").bufferedReader().use { it.readText() } }.getOrNull() } },
    privacy = "Raumlicht speichert nur den Spielstand auf diesem Gerät. Keine Anmeldung, keine Werbung, keine Käufe. Ins Internet geht die App nur, " +
        (if (BuildConfig.SELF_UPDATE) "um nach Updates zu schauen (abschaltbar)" else "wenn der Play Store nach Updates schaut") +
        " – und einmalig, wenn du die natürliche Stimme lädst. Gesprochen wird immer auf dem Gerät.")

/** Stimme aussuchen: durch die deutschen Stimmen des Geräts schalten (jede stellt sich vor) und die Stimmlage wählen. */
@Composable
fun VoiceControls(position: Pair<Int, Int>, bright: Boolean, onNext: () -> Unit, onBright: (Boolean) -> Unit) {
    var isBright by remember { mutableStateOf(bright) }
    Column(Modifier.padding(bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onNext, Modifier.testTag("stimme-wechseln"), enabled = position.second > 1) { Text(if (position.second > 1) "Andere Stimme (${position.first} von ${position.second})" else "Nur eine Stimme vorhanden") }
            OutlinedButton({ isBright = !isBright; onBright(isBright) }, Modifier.testTag("stimmlage")) { Text(if (isBright) "Lage: hell" else "Lage: natürlich") }
        }
        Text("Klingt es noch nach Automat? Dann liegt es an der Stimme des Handys. Bessere gibt es in den Android-Einstellungen unter " +
            "„Sprachausgabe“ → Einstellungen der Sprachausgabe → „Sprachdaten installieren“ → Deutsch.", style = MaterialTheme.typography.bodySmall, color = Q.Muted)
    }
}

/** Natürliche Stimme: einmal laden (freiwillig), danach ein- und ausschalten oder wieder entfernen. */
@Composable
fun NaturalVoiceCard(state: de.raumlicht.app.ui.fx.PackState, active: Boolean, problem: String?, onLoad: () -> Unit, onCancel: () -> Unit, onRemove: () -> Unit, onUse: (Boolean) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("✨ Natürliche Stimme „Thorsten“", style = MaterialTheme.typography.bodyLarge)
        when (state) {
            de.raumlicht.app.ui.fx.PackState.Ready -> {
                Text(problem ?: if (active) "Lux spricht mit der natürlichen Stimme – ganz ohne Internet." else "Geladen. Schalte sie ein, damit Lux so spricht.", Modifier.testTag("stimme-status"),
                    style = MaterialTheme.typography.bodySmall, color = if (problem != null) Q.Orange else if (active) Q.Green else Q.Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ onUse(!active) }, Modifier.testTag("stimme-nutzen")) { Text(if (active) "Handy-Stimme nehmen" else "Einschalten") }
                    OutlinedButton(onRemove, Modifier.testTag("stimme-entfernen")) { Text("Entfernen (${de.raumlicht.app.ui.fx.VoicePack.MB} MB frei)") }
                }
            }
            is de.raumlicht.app.ui.fx.PackState.Loading -> {
                Text("Lädt … ${state.percent} %", Modifier.testTag("stimme-status"), style = MaterialTheme.typography.bodySmall, color = Q.Cyan)
                LinearProgressIndicator({ state.percent / 100f }, Modifier.fillMaxWidth())
                OutlinedButton(onCancel, Modifier.testTag("stimme-abbrechen")) { Text("Abbrechen") }
            }
            else -> {
                Text((state as? de.raumlicht.app.ui.fx.PackState.Failed)?.let { "Das hat nicht geklappt: ${it.why}" }
                    ?: "Klingt viel weniger nach Automat und rechnet auf dem Handy. Einmaliger Download von ${de.raumlicht.app.ui.fx.VoicePack.MB} MB (am besten im WLAN), danach ohne Internet.",
                    Modifier.testTag("stimme-status"), style = MaterialTheme.typography.bodySmall, color = if (state is de.raumlicht.app.ui.fx.PackState.Failed) Q.Orange else Q.Muted)
                OutlinedButton(onLoad, Modifier.testTag("stimme-laden")) { Text("Stimme laden (${de.raumlicht.app.ui.fx.VoicePack.MB} MB)") }
            }
        }
    }
}
