package de.raumlicht.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.raumlicht.app.ui.theme.Q

@Composable
fun SettingsPage(
    music: Boolean = true, onMusic: (Boolean) -> Unit = {},
    sound: Boolean, onSound: (Boolean) -> Unit,
    vibration: Boolean, onVibration: (Boolean) -> Unit,
    /** z. B. „Version 2.4.1 vom 15.11.2026“ */
    versionText: String,
    onBack: () -> Unit,
    /** Plattform-eigener Abschnitt ganz oben (Android: App-Update) */
    top: @Composable () -> Unit = {},
    onReset: () -> Unit = {},
    volume: Int = 100, onVolume: (Int) -> Unit = {},
    voice: Boolean = true, onVoice: (Boolean) -> Unit = {},
    /** Warum gerade nichts zu hören ist oder sein könnte (z. B. Handy stumm geschaltet) – steht gut sichtbar über den Ton-Schaltern; null = kein Hinweis */
    soundNote: String? = null,
    /** Bericht über den letzten unerwarteten Abbruch (null = keiner) */
    lastProblem: String? = null, onProblemSeen: () -> Unit = {},
    /** Gibt es Hintergrundmusik? (im Browser noch nicht) */
    hasMusic: Boolean = true,
    /** Lizenzen der fremden Bausteine: als Text zum Anzeigen ([licenses], Android) oder als eigene Aktion ([onLicenses], Web öffnet die Datei) */
    licenses: String? = null, onLicenses: (() -> Unit)? = null,
    /** Was die App speichert und wann sie ins Internet geht – je Plattform verschieden */
    privacy: String = "Raumlicht speichert nur den Spielstand auf diesem Gerät. Keine Anmeldung, keine Werbung, keine Käufe. " +
        "Ins Internet geht die App nur, um nach Updates zu schauen (abschaltbar) – und zum Vorlesen, wenn du „Lux spricht“ einschaltest. " + de.raumlicht.core.model.Vorlesen.PRIVACY,
) {
    val speaker = de.raumlicht.app.ui.fx.LocalSpeaker.current
    val sfx = de.raumlicht.app.ui.fx.LocalSfx.current
    var ask by remember { mutableStateOf(false) }; var done by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { de.raumlicht.app.ui.fx.BackArrow() }
            Text("Einstellungen", style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            top()
            ProEntry()
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Ton & Gefühl", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    val mix = LocalMix.current
                    if (soundNote != null) Text(soundNote, Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag("ton-hinweis").background(Q.Yellow.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                        .border(1.dp, Q.Yellow.copy(alpha = 0.7f), RoundedCornerShape(12.dp)).padding(10.dp), color = Q.Yellow, style = MaterialTheme.typography.bodyMedium)
                    if (hasMusic) Toggle("🎵 Musik", "Gläserne, schwebende Klänge – für Raumlicht komponiert. In Prüfungen auf Zeit ist sie aus.", music, onMusic)
                    if (hasMusic && music) { Text("Musik: ${mix.music} %", Modifier.testTag("musik-laut-text"), style = MaterialTheme.typography.bodySmall, color = Q.Muted)
                        Slider(mix.music / 100f, { mix.setMusic((it * 20).toInt() * 5) }, Modifier.testTag("musik-laut")) }
                    Toggle("🔊 Geräusche", "Laser, Treffer, Glasklänge der Lichtshow", sound, onSound)
                    if (sound) { Text("Geräusche: ${mix.sound} %", Modifier.testTag("ton-laut-text"), style = MaterialTheme.typography.bodySmall, color = Q.Muted)
                        Slider(mix.sound / 100f, { mix.setSound((it * 20).toInt() * 5) }, Modifier.testTag("ton-laut"), onValueChangeFinished = { sfx?.play(de.raumlicht.app.ui.fx.Sound.HIT) }) }
                    Toggle("🗣️ Lux spricht", "Aus, bis du es einschaltest: Lux liest ihre Sprechblasen laut vor.", voice) {
                        onVoice(it); if (it) speaker?.say("Hallo, ich bin Lux!") }
                    if (voice) {
                        // Stimmenwahl: Nova oder Vorlesen aus. Die Kennzeichnung als KI-Stimme ist Pflicht.
                        Text("Stimme: ${de.raumlicht.core.model.Vorlesen.VOICE_LABEL}", Modifier.padding(top = 2.dp).testTag("stimme-name"), style = MaterialTheme.typography.bodyLarge)
                        Text("Nova braucht Internet. Schon gehörte Sätze bleiben auf dem Gerät gespeichert. Ohne Netz springt still die Stimme des Geräts ein.",
                            style = MaterialTheme.typography.bodySmall, color = Q.Muted)
                        OutlinedButton({ speaker?.say("Hallo, ich bin Lux. So klinge ich jetzt.") }, Modifier.testTag("hoerprobe")) { Text("▶ So klingt es") }
                    }
                    Toggle("📳 Vibration", "Kurzes Ruckeln bei Treffern", vibration, onVibration)
                    Text("🔉 Lautstärke: $volume %", Modifier.padding(top = 8.dp).testTag("lautstaerke-text"), style = MaterialTheme.typography.bodyLarge)
                    Text("Für alles zusammen: Musik, Geräusche und Stimme", style = MaterialTheme.typography.bodySmall, color = Q.Muted)
                    Slider(volume / 100f, { onVolume((it * 20).toInt() * 5) }, Modifier.testTag("lautstaerke"), onValueChangeFinished = { sfx?.play(de.raumlicht.app.ui.fx.Sound.HIT) })
                }
            }
            ElevatedCard(Modifier.fillMaxWidth()) {
                val notation = LocalNotation.current
                Column(Modifier.padding(16.dp)) {
                    Text("Mathe-Schreibweise", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Box(Modifier.testTag("schreibweise")) { Toggle("📐 Strenge Vektorschreibweise", if (notation.strict) "An: mit Ortsvektoren, z. B. AB⃗ = OB⃗ − OA⃗ und g: x⃗ = OA⃗ + t · d⃗" else "Aus: kurz wie in vielen Schulheften, z. B. AB⃗ = B − A und g: x⃗ = A + t · d⃗",
                        notation.strict, notation.set) }
                    Text("Frag im Zweifel, wie es deine Lehrkraft haben möchte. Die Rechnung ist dieselbe.", style = MaterialTheme.typography.bodySmall, color = Q.Muted)
                }
            }
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Datenschutz & Inhalte", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    // Sobald es den Abi-Pass gibt, stimmt „keine Käufe“ nicht mehr
                    Text(if (LocalProEntry.current == null) privacy else privacy.replace("keine Käufe", "kein Abo – nur der einmalige Abi-Pass, bezahlt auf unserer Website"), style = MaterialTheme.typography.bodySmall)
                    Text("Inhalte: Mathematik der Oberstufe. Analytische Geometrie – Punkte und Vektoren im Raum, Geraden in Parameterform, " +
                        "Lagebeziehungen, Ebenen in Koordinatenform, Durchstoßpunkte, Normalenvektor, Skalarprodukt, Lotfußpunkt, " +
                        "Abstände, Spiegelung, Winkel, Kugeln, Kreuzprodukt mit Flächen und Volumen, Bewegungsaufgaben; dazu Analysis (Funktionen, Ableitung, Kurvendiskussion, Integral, Rotationskörper) und Stochastik (Laplace, Pfadregel, Vierfeldertafel, Binomialverteilung). Jedes Kapitel endet mit einer Aufgabe im Abitur-Stil.", style = MaterialTheme.typography.bodySmall)
                    if (licenses != null || onLicenses != null) TextButton({ if (onLicenses != null) onLicenses() else showLicenses = true }, Modifier.testTag("lizenzen")) { Text("Fremde Bausteine & Lizenzen") }
                    Text(versionText, style = MaterialTheme.typography.labelSmall, color = Q.Muted)
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
    if (showLicenses && licenses != null) AlertDialog(
        onDismissRequest = { showLicenses = false },
        title = { Text("Fremde Bausteine & Lizenzen") },
        text = { Text(licenses, Modifier.verticalScroll(rememberScrollState()).testTag("lizenzen-text"), style = MaterialTheme.typography.bodySmall) },
        confirmButton = { TextButton({ showLicenses = false }) { Text("Schließen") } },
    )
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
