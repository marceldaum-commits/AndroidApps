package de.wissensgarten.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Elternbereich: „📱 Eltern-Handy koppeln“ – Knopf mit Zustand, öffnet den Dialog.
 * Nur hinter dem Eltern-Tor (Einstellungen). Im Kinderbereich gibt es davon nichts.
 */
@Composable
fun ElternKopplungKnopf(
    name: String,
    link: String?,
    letzterUpload: String?,
    onKoppeln: (spitzname: String) -> Unit,
    onAktualisieren: (onResult: (Boolean) -> Unit) -> Unit,
    onTeilen: (String) -> Unit,
    onAufheben: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
        Text(if (link == null) "📱 Eltern-Handy koppeln" else "📱 Eltern-Handy gekoppelt ✓")
    }
    Text("Seht auf eurem eigenen Handy, wie es bei $name läuft – ohne Konto, Ende-zu-Ende verschlüsselt.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (open) ElternKopplungDialog(name, link, letzterUpload, onKoppeln, onAktualisieren, onTeilen, onAufheben) { open = false }
}

@Composable
fun ElternKopplungDialog(
    name: String,
    link: String?,
    letzterUpload: String?,
    onKoppeln: (String) -> Unit,
    onAktualisieren: (onResult: (Boolean) -> Unit) -> Unit,
    onTeilen: (String) -> Unit,
    onAufheben: () -> Unit,
    onDismiss: () -> Unit,
) {
    var spitzname by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var sicher by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📱 Eltern-Handy koppeln") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (link == null) {
                    Text("Nach jeder Lernrunde legt der Wissensgarten einen kurzen Bericht für euch bereit: Lernzeit, was gut sitzt, wo es hakt, die nächste Klassenarbeit. " +
                        "Ihr öffnet ihn mit einem Link auf eurem Handy.")
                    Text("Der Bericht wird auf diesem Gerät verschlüsselt. Den Schlüssel kennt nur euer Link – unser Server kann ihn nicht lesen. " +
                        "Ohne Kopplung wird nichts gesendet. Nach 90 Tagen ohne Aktualisierung wird er gelöscht.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(spitzname, { spitzname = it.take(20) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        label = { Text("Spitzname im Bericht (freiwillig)") }, placeholder = { Text("z. B. $name") })
                    Text("Lasst das Feld leer, steht im Bericht kein Name.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("Scannt den Code mit eurem Handy oder schickt euch den Link. Wer den Link hat, kann den Bericht lesen – teilt ihn nur mit euch selbst.",
                        style = MaterialTheme.typography.bodyMedium)
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        de.wissensgarten.app.ui.klasse.QrBloom(link, Modifier.widthIn(max = 220.dp).fillMaxWidth())
                    }
                    letzterUpload?.let { Text("Zuletzt aktualisiert: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    status?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                    Button(onClick = { onTeilen(link) }, modifier = Modifier.fillMaxWidth()) { Text("Link schicken") }
                    OutlinedButton(onClick = { status = "Wird aktualisiert …"; onAktualisieren { ok -> status = if (ok) "Bericht aktualisiert ✓" else "Gerade nicht erreichbar – wir versuchen es nach der nächsten Lernrunde wieder." } },
                        modifier = Modifier.fillMaxWidth()) { Text("Jetzt aktualisieren") }
                    if (!sicher) TextButton(onClick = { sicher = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Kopplung aufheben", color = MaterialTheme.colorScheme.error)
                    } else Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Der Bericht wird auf dem Server gelöscht, der Link funktioniert danach nicht mehr.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        Button(onClick = { onAufheben(); onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()) { Text("Ja, Kopplung aufheben") }
                    }
                }
            }
        },
        confirmButton = {
            if (link == null) TextButton(onClick = { onKoppeln(spitzname) }) { Text("Koppeln") }
            else TextButton(onClick = onDismiss) { Text("Fertig") }
        },
        dismissButton = { if (link == null) TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

/**
 * Elternbereich: „Fortschritt wiederherstellen“ – neues Gerät, gelöschte Browserdaten … Mit dem Kopplungs-Link vom Eltern-Handy
 * kommt der gesicherte Lernstand zurück. Vorher wird immer nachgefragt; vorhandener Lernstand wird zusammengeführt, nie überschrieben.
 */
@Composable
fun WiederherstellenKnopf(
    pruefen: suspend (String) -> de.wissensgarten.app.AppViewModel.Fund,
    einspielen: suspend (de.wissensgarten.app.AppViewModel.Fund.Gefunden) -> String,
    onFertig: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    TextButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) { Text("🔄 Fortschritt wiederherstellen") }
    if (!open) return
    val scope = rememberCoroutineScope()
    var link by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var fund by remember { mutableStateOf<de.wissensgarten.app.AppViewModel.Fund.Gefunden?>(null) }
    AlertDialog(
        onDismissRequest = { if (!busy) open = false },
        title = { Text("🔄 Fortschritt wiederherstellen") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val f = fund
                if (f == null) {
                    Text("Neues Gerät oder Browserdaten gelöscht? Ist ein Eltern-Handy gekoppelt, gibt es eine verschlüsselte Sicherung. " +
                        "Fügt hier den Kopplungs-Link ein – auf der Eltern-Seite gibt es dafür „Neues Gerät des Kindes? Fortschritt übertragen“.")
                    OutlinedTextField(link, { link = it.trim(); fehler = null }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        label = { Text("Kopplungs-Link einfügen") }, placeholder = { Text("https://…/eltern-bericht/#…") })
                } else {
                    Text("Gefunden: der Garten von ${f.name.ifBlank { "eurem Kind" }} mit ${f.karten} Karten" +
                        (if (f.bilder > 0) " und ${f.bilder} selbst gemalten Merkbildern" else "") + ".")
                    if (f.hierSchonLernstand) Text("Auf diesem Gerät gibt es schon einen Lernstand. Beides wird zusammengeführt – es geht nichts verloren.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Danach ist dieses Gerät wieder mit dem Eltern-Handy gekoppelt.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            val f = fund
            TextButton(enabled = !busy && (f != null || link.isNotBlank()), onClick = {
                busy = true
                scope.launch {
                    if (f == null) when (val r = pruefen(link)) {
                        is de.wissensgarten.app.AppViewModel.Fund.Fehler -> fehler = r.text
                        is de.wissensgarten.app.AppViewModel.Fund.Gefunden -> fund = r
                    } else { val msg = einspielen(f); open = false; onFertig(msg) }
                    busy = false
                }
            }) { Text(if (f == null) "Prüfen" else "Wiederherstellen") }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = { open = false }) { Text("Abbrechen") } },
    )
}
