package de.wissensgarten.app.ui.garden

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Karte im Garten: einmalige, freundliche Frage nach der Erinnerung. */
@Composable
fun ReminderPromptCard(hour: Int, onYes: () -> Unit, onNo: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔔", fontSize = 28.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Soll ich ans Gießen erinnern?", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Einmal am Tag – nur wenn Pflanzen wirklich Durst haben.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onYes) { Text("Ja, um $hour Uhr") }
                TextButton(onClick = onNo) { Text("Nein, danke") }
            }
        }
    }
}
