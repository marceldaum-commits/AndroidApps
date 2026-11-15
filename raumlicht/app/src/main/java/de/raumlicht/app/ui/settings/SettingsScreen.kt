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
                        "Ins Internet geht die App nur, um nach Updates zu schauen (abschaltbar). Lux spricht mit der Sprachausgabe deines Geräts.", style = MaterialTheme.typography.bodySmall)
                    Text("Inhalte: Mathematik der Oberstufe. Analytische Geometrie – Punkte und Vektoren im Raum, Geraden in Parameterform, " +
                        "Lagebeziehungen, Ebenen in Koordinatenform, Durchstoßpunkte, Normalenvektor, Skalarprodukt, Lotfußpunkt, " +
                        "Abstände, Spiegelung, Winkel, Kugeln, Kreuzprodukt mit Flächen und Volumen, Bewegungsaufgaben; dazu Analysis (Funktionen, Ableitung, Kurvendiskussion, Integral, Rotationskörper) und Stochastik (Laplace, Pfadregel, Vierfeldertafel, Binomialverteilung). Jedes Kapitel endet mit einer Aufgabe im Abitur-Stil.", style = MaterialTheme.typography.bodySmall)
                    Text("Version $installed vom ${BuildConfig.VERSION_DATE}", style = MaterialTheme.typography.labelSmall, color = Q.Muted)
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
