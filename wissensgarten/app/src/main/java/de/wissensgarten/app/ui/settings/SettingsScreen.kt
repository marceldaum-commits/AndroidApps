package de.wissensgarten.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.wissensgarten.app.ui.common.AvatarPicker
import de.wissensgarten.app.ui.common.LevelPicker
import de.wissensgarten.core.data.ThemeMode
import de.wissensgarten.core.data.db.ProfileEntity
import de.wissensgarten.core.model.Levels

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    profile: ProfileEntity,
    canDeleteProfile: Boolean,
    theme: ThemeMode,
    sound: Boolean,
    onSound: (Boolean) -> Unit,
    readAloud: de.wissensgarten.core.data.ReadAloud = de.wissensgarten.core.data.ReadAloud.AUTO,
    onReadAloud: (de.wissensgarten.core.data.ReadAloud) -> Unit = {},
    reminderOn: Boolean = false,
    reminderHour: Int = 17,
    onReminder: (Boolean, Int) -> Unit = { _, _ -> },
    packCount: Int,
    cardCount: Int,
    version: String,
    onTheme: (ThemeMode) -> Unit,
    onUpdateProfile: (ProfileEntity) -> Unit,
    onDeleteProfile: () -> Unit,
    onTutorial: () -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Einstellungen", style = MaterialTheme.typography.headlineMedium)

        Section("Profil") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(profile.avatar, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(profile.name, style = MaterialTheme.typography.titleMedium)
                    Text(Levels.label(profile.level), style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = { editing = true }) { Text("Ändern") }
            }
            Text("Neue Samen pro Tag", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 5, 10, 15, 20, 30).forEach { n ->
                    FilterChip(selected = profile.seedsPerDay == n, onClick = { onUpdateProfile(profile.copy(seedsPerDay = n)) }, label = { Text("$n") })
                }
            }
            Text(
                "Weniger neue Samen = weniger Gießarbeit an den Folgetagen.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Section("Erinnerung") {
            var denied by remember { mutableStateOf(false) }
            var pendingHour by remember { mutableIntStateOf(reminderHour) }
            val ask = de.wissensgarten.app.notify.rememberNotificationPermission { ok ->
                denied = !ok
                onReminder(ok, pendingHour)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Täglich ans Gießen erinnern", Modifier.weight(1f))
                Switch(checked = reminderOn, onCheckedChange = { on -> if (on) { pendingHour = reminderHour; ask() } else onReminder(false, reminderHour) })
            }
            if (reminderOn) {
                Text("Uhrzeit", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 12, 14, 15, 16, 17, 18, 19, 20).forEach { h ->
                        FilterChip(selected = reminderHour == h, onClick = { onReminder(true, h) }, label = { Text("$h Uhr") })
                    }
                }
            }
            Text(
                if (denied) "Benachrichtigungen sind blockiert. Erlaube sie in den Android-Einstellungen für den Wissensgarten."
                else "Nur wenn Pflanzen Durst haben – für alle Familienmitglieder in einer Nachricht.",
                style = MaterialTheme.typography.bodySmall,
                color = if (denied) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Section("Brummi liest vor") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                de.wissensgarten.core.data.ReadAloud.entries.forEach { r -> FilterChip(selected = r == readAloud, onClick = { onReadAloud(r) }, label = { Text(r.label) }) }
            }
            Text("Automatisch: für Grundschulkinder an. Mit 🔊 an jeder Frage geht's immer. Englisch, Französisch und Spanisch werden in der richtigen Sprache gesprochen.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Section("Ton") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Geräusche & Musik", Modifier.weight(1f))
                Switch(checked = sound, onCheckedChange = onSound)
            }
        }

        Section("Aussehen") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { t -> FilterChip(selected = t == theme, onClick = { onTheme(t) }, label = { Text(t.label) }) }
            }
        }

        Section("Hilfe") {
            OutlinedButton(onClick = onTutorial, modifier = Modifier.fillMaxWidth()) { Text("Anleitung noch mal ansehen") }
            Text(
                "So funktioniert's: Jede Karte ist eine Pflanze. Sie welkt so, wie man vergisst (Vergessenskurve). " +
                    "Wer gießt, wenn sie Durst hat, wiederholt zum besten Zeitpunkt – so bleibt Wissen mit wenig Aufwand dauerhaft.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Section("Über") {
            Text("Wissensgarten $version")
            Text("$packCount Wissenspakete · $cardCount Karten", style = MaterialTheme.typography.bodySmall)
            Text("Alle Daten bleiben auf diesem Gerät.", style = MaterialTheme.typography.bodySmall)
        }

        if (canDeleteProfile) TextButton(onClick = { confirmDelete = true }) {
            Text("Profil „${profile.name}“ löschen", color = MaterialTheme.colorScheme.error)
        }
    }

    if (editing) EditProfileDialog(profile, onDismiss = { editing = false }) { onUpdateProfile(it); editing = false }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Profil löschen?") },
        text = { Text("Der Garten von ${profile.name} mit allen Beeten und dem Lernstand wird gelöscht.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; onDeleteProfile() }) { Text("Löschen") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Abbrechen") } },
    )
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun EditProfileDialog(p: ProfileEntity, onDismiss: () -> Unit, onSave: (ProfileEntity) -> Unit) {
    var name by remember { mutableStateOf(p.name) }
    var avatar by remember { mutableStateOf(p.avatar) }
    var level by remember { mutableIntStateOf(p.level) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profil ändern") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it.take(20) }, label = { Text("Name") }, singleLine = true)
                AvatarPicker(avatar) { avatar = it }
                LevelPicker(level) { level = it }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(p.copy(name = name.trim(), avatar = avatar, level = level)) }, enabled = name.isNotBlank()) { Text("Speichern") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
