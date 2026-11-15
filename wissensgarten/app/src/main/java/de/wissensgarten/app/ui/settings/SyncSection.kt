package de.wissensgarten.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.wissensgarten.app.sync.DriveSync
import de.wissensgarten.app.sync.SyncStatus
import de.wissensgarten.app.ui.fx.*
import kotlinx.coroutines.launch

/**
 * Startet die Google-Verbindung (mit Google-Dialog, falls nötig) und gleicht danach direkt ab.
 * [onResult]: Erfolg mit „lag schon ein Garten im Drive?“ oder Fehlermeldung.
 */
@Composable
fun rememberGoogleConnect(sync: DriveSync, onResult: (Result<Boolean>) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    val latest by rememberUpdatedState(onResult)
    fun afterAuth(r: com.google.android.gms.auth.api.identity.AuthorizationResult) {
        sync.markConnected(r)
        scope.launch { latest(sync.syncNow()) }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        sync.resultFromIntent(res.data).fold(onSuccess = { afterAuth(it) }, onFailure = { latest(Result.failure(it)) })
    }
    return {
        scope.launch {
            sync.authorize().fold(
                onSuccess = { o ->
                    when (o) {
                        is DriveSync.AuthOutcome.Done -> afterAuth(o.result)
                        is DriveSync.AuthOutcome.NeedsUser -> launcher.launch(IntentSenderRequest.Builder(o.sender).build())
                    }
                },
                onFailure = { latest(Result.failure(it)) },
            )
        }
    }
}

/** Einstellungen-Karte „Auf allen Geräten“ – verbunden mit dem echten Sync. */
@Composable
fun SyncSectionLive(sync: DriveSync) {
    val status by sync.status.collectAsState()
    val sfx = LocalSfx.current
    var message by remember { mutableStateOf<String?>(null) }
    val connect = rememberGoogleConnect(sync) { r ->
        r.fold(
            onSuccess = { found -> sfx?.play(Sound.FANFARE); message = if (found) "Garten zusammengeführt! 🎉" else "Verbunden! Dein Garten ist jetzt gesichert. ☁️" },
            onFailure = { message = it.message },
        )
    }
    val scope = rememberCoroutineScope()
    SyncCard(status, message,
        onConnect = { message = null; connect() },
        onSyncNow = { message = null; scope.launch { sync.syncNow().onSuccess { sfx?.play(Sound.CORRECT, volume = 0.6f) } } },
        onDisconnect = { sync.disconnect(); message = "Getrennt. Dein Garten bleibt auf diesem Gerät." },
    )
}

/** Reine Anzeige – auch für Bildschirmfotos. */
@Composable
fun SyncCard(status: SyncStatus, message: String?, onConnect: () -> Unit, onSyncNow: () -> Unit, onDisconnect: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Auf allen Geräten", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Cloud(status.running)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    if (status.connected) {
                        Text("Verbunden" + (status.account?.let { " mit $it" } ?: ""), fontWeight = FontWeight.Bold)
                        Text(when {
                            status.running -> "Gleiche ab …"
                            status.lastSync > 0 -> "Zuletzt abgeglichen: ${ago(status.lastSync)}"
                            else -> "Noch nicht abgeglichen"
                        }, style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("Auf dem Handy anfangen, auf dem Tablet weitermachen", fontWeight = FontWeight.Bold)
                        Text("Der Garten der ganzen Familie wird in deinem Google Drive gespeichert – in einem versteckten Ordner, den nur der Wissensgarten sieht. Kein eigenes Konto, kein fremder Server.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            (message ?: status.error)?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = if (message == null) MaterialTheme.colorScheme.error else Color.Unspecified) }
            if (!status.connected) JuicyButton(onClick = onConnect, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("☁️ Mit Google-Konto verbinden", fontWeight = FontWeight.Bold)
            } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                JuicyButton(onClick = onSyncNow, enabled = !status.running, modifier = Modifier.weight(1f)) { Text("🔄 Jetzt abgleichen") }
                OutlinedButton(onClick = { confirm = true }) { Text("Trennen") }
            }
            if (status.connected) Text("Gleicht automatisch ab: beim Öffnen und nach jeder Gieß-Runde. Ein Elternteil verbindet – alle Profile ziehen mit.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Dieses Gerät trennen?") },
        text = { Text("Der Garten bleibt auf diesem Gerät und im Google Drive. Es wird nur nicht mehr abgeglichen.") },
        confirmButton = { TextButton(onClick = { confirm = false; onDisconnect() }) { Text("Trennen") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Abbrechen") } },
    )
}

@Composable
private fun Cloud(running: Boolean) {
    val t = rememberInfiniteTransition(label = "cloud")
    val bob by t.animateFloat(-3f, 3f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "s")
    Box(contentAlignment = Alignment.Center) {
        Text("☁️", fontSize = 40.sp, modifier = Modifier.graphicsLayer { translationY = bob * density })
        if (running) Text("🔄", fontSize = 16.sp, modifier = Modifier.graphicsLayer { rotationZ = spin })
    }
}

private fun ago(t: Long): String {
    val m = (System.currentTimeMillis() - t) / 60_000
    return when {
        m < 1 -> "gerade eben"
        m < 60 -> "vor $m Min."
        m < 24 * 60 -> "vor ${m / 60} Std."
        else -> "vor ${m / 1440} Tagen"
    }
}

/** Erster Start auf einem neuen Gerät: Garten aus dem Google Drive holen. */
@Composable
fun WelcomeRestore(sync: DriveSync, onRestored: (Boolean) -> Unit) {
    val status by sync.status.collectAsState()
    var msg by remember { mutableStateOf<String?>(null) }
    val connect = rememberGoogleConnect(sync) { r ->
        r.fold(
            onSuccess = { found ->
                if (found) onRestored(true)
                else msg = "Im Google Drive liegt noch kein Garten – leg einfach los, er wird dann automatisch gesichert."
            },
            onFailure = { msg = it.message },
        )
    }
    OutlinedButton(onClick = { msg = null; connect() }, enabled = !status.running) {
        Text(if (status.running) "☁️ Hole deinen Garten …" else "☁️ Schon einen Garten? Mit Google wiederherstellen",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
    msg?.let { Text(it, style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 8.dp)) }
}
