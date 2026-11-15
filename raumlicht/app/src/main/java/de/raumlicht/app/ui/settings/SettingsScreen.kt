package de.raumlicht.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.raumlicht.app.BuildConfig
import de.raumlicht.app.ui.theme.Q

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
    /** Knöpfe zum Aussuchen der Stimme (nur wenn Lux spricht) */
    voiceControls: @Composable () -> Unit = {},
    /** Bericht über den letzten unerwarteten Abbruch (null = keiner) */
    lastProblem: String? = null, onProblemSeen: () -> Unit = {},
) {
    val speaker = de.raumlicht.app.ui.fx.LocalSpeaker.current
    val sfx = de.raumlicht.app.ui.fx.LocalSfx.current
    var ask by remember { mutableStateOf(false) }; var done by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Q.Text) }
            Text("Einstellungen", style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (BuildConfig.SELF_UPDATE) UpdateSection(installed, update, autoUpdate, onAutoUpdate)
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Ton & Gefühl", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Toggle("🎵 Musik", "Elektronische Klänge im Hintergrund", music, onMusic)
                    Toggle("🔊 Geräusche", "Laser, Treffer, Lux’ Piepsen", sound, onSound)
                    Toggle("🗣️ Lux spricht", if (voiceAvailable) "Lux liest ihre Sprechblasen laut vor (Sprachausgabe des Geräts)" else "Auf diesem Gerät ist keine deutsche Stimme eingerichtet – Lux bleibt stumm.", voice) {
                        onVoice(it); if (it) speaker?.say("Hallo, ich bin Lux!") }
                    if (voice && voiceAvailable) voiceControls()
                    Toggle("📳 Vibration", "Kurzes Ruckeln bei Treffern", vibration, onVibration)
                    Text("🔉 Lautstärke: $volume %", Modifier.padding(top = 8.dp).testTag("lautstaerke-text"), style = MaterialTheme.typography.bodyLarge)
                    Text("Für Musik und Geräusche zusammen", style = MaterialTheme.typography.bodySmall, color = Q.Muted)
                    Slider(volume / 100f, { onVolume((it * 20).toInt() * 5) }, Modifier.testTag("lautstaerke"), onValueChangeFinished = { sfx?.play(de.raumlicht.app.ui.fx.Sound.HIT) })
                }
            }
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Datenschutz & Inhalte", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Raumlicht speichert nur den Spielstand auf diesem Gerät. Keine Anmeldung, keine Werbung, keine Käufe. " +
                        "Ins Internet geht die App nur, um nach Updates zu schauen (abschaltbar) – und einmalig, wenn du die natürliche Stimme lädst. Gesprochen wird immer auf dem Gerät.", style = MaterialTheme.typography.bodySmall)
                    Text("Inhalte: Mathematik der Oberstufe. Analytische Geometrie – Punkte und Vektoren im Raum, Geraden in Parameterform, " +
                        "Lagebeziehungen, Ebenen in Koordinatenform, Durchstoßpunkte, Normalenvektor, Skalarprodukt, Lotfußpunkt, " +
                        "Abstände, Spiegelung, Winkel, Kugeln, Kreuzprodukt mit Flächen und Volumen, Bewegungsaufgaben; dazu Analysis (Funktionen, Ableitung, Kurvendiskussion, Integral, Rotationskörper) und Stochastik (Laplace, Pfadregel, Vierfeldertafel, Binomialverteilung). Jedes Kapitel endet mit einer Aufgabe im Abitur-Stil.", style = MaterialTheme.typography.bodySmall)
                    Text("Version $installed vom ${BuildConfig.VERSION_DATE}", style = MaterialTheme.typography.labelSmall, color = Q.Muted)
                }
            }
            if (lastProblem != null) ElevatedCard(Modifier.fillMaxWidth()) {
                val clip = androidx.compose.ui.platform.LocalClipboardManager.current
                var copied by remember { mutableStateOf(false) }
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Letzter unerwarteter Abbruch", style = MaterialTheme.typography.titleMedium, color = Q.Orange)
                    Text("Die App ist zuletzt unerwartet zugegangen. Dieser Bericht bleibt auf dem Gerät – du kannst ihn kopieren und weitergeben, damit der Fehler gefunden wird.", style = MaterialTheme.typography.bodySmall, color = Q.Muted)
                    Text(lastProblem.take(1200), Modifier.testTag("abbruch-bericht"), style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ clip.setText(androidx.compose.ui.text.AnnotatedString(lastProblem)); copied = true }, Modifier.testTag("abbruch-kopieren")) { Text(if (copied) "✓ Kopiert" else "Bericht kopieren") }
                        OutlinedButton(onProblemSeen, Modifier.testTag("abbruch-weg")) { Text("Ausblenden") }
                    }
                }
            }
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Neu anfangen", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(if (done) "✓ Alles zurückgesetzt – du startest wieder bei null." else "Setzt alles zurück, was du bisher gemacht hast: Sterne, Rekorde, Abi-Punkte, Fehler-Tagebuch und Kalender.",
                        style = MaterialTheme.typography.bodySmall, color = if (done) Q.Green else Q.Muted)
                    OutlinedButton({ ask = true }, Modifier.testTag("zuruecksetzen"), colors = ButtonDefaults.outlinedButtonColors(contentColor = Q.Orange)) { Text("🧹 Fortschritt zurücksetzen") }
                }
            }
        }
    }
    if (ask) AlertDialog(
        onDismissRequest = { ask = false },
        title = { Text("Wirklich alles zurücksetzen?") },
        text = { Text("Alle Sterne, Rekorde, Abi-Punkte, das Fehler-Tagebuch, der Kalender und das freigespielte Aussehen von Lux werden gelöscht. Das lässt sich nicht rückgängig machen.\n\nTon-Einstellungen und dein Klausur-Plan bleiben.") },
        confirmButton = { TextButton({ ask = false; done = true; onReset() }, Modifier.testTag("zuruecksetzen-ja")) { Text("Ja, zurücksetzen", color = Q.Orange) } },
        dismissButton = { TextButton({ ask = false }, Modifier.testTag("zuruecksetzen-nein")) { Text("Abbrechen") } },
    )
}

@Composable
private fun Toggle(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = Q.Muted)
        }
        Switch(on, onChange)
    }
}

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
