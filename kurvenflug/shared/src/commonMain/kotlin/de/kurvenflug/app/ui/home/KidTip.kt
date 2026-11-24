package de.kurvenflug.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import de.kurvenflug.app.LocalPlatform
import de.kurvenflug.app.ui.theme.Q
import de.kurvenflug.core.model.ParentOffers

/**
 * Tipp für Klasse 9–10 (Recht V15, ohne Tor): höchstens einmal pro Woche, wegklickbar, ohne Preis und ohne Kaufsprache.
 * Der Wortlaut steht in [ParentOffers.KID_TIP_TEXT]; [Mehr erfahren] führt nur zur Probestunden-Seite.
 */
@Composable
fun KidTip(onSeen: () -> Unit, onClose: () -> Unit) {
    val platform = LocalPlatform.current
    LaunchedEffect(Unit) { onSeen() }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Q.Panel).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(ParentOffers.KID_TIP_TEXT, color = Q.Text, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { platform.countAt("probestunde_klick", "kindertipp", null); platform.openUrl(ParentOffers.KID_TIP_LINK) }) { Text("Mehr erfahren") }
            TextButton(onClick = onClose) { Text("Schließen", color = Q.Muted) }
        }
    }
}
