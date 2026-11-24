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
    update: de.wissensgarten.core.model.AppRelease? = null,
    autoUpdate: Boolean = true,
    onAutoUpdate: (Boolean) -> Unit = {},
    syncCard: @Composable () -> Unit = {},
    syllables: de.wissensgarten.core.data.ReadAloud = de.wissensgarten.core.data.ReadAloud.AUTO,
    onSyllables: (de.wissensgarten.core.data.ReadAloud) -> Unit = {},
    vibration: Boolean = true,
    onVibration: (Boolean) -> Unit = {},
    music: Boolean = true,
    onMusic: (Boolean) -> Unit = {},
    onPrivacy: () -> Unit = {},
    onWipeAll: () -> Unit = {},
    typing: Boolean = true,
    onTyping: (Boolean) -> Unit = {},
    dailyLimit: Int = 0,
    onDailyLimit: (Int) -> Unit = {},
    holiday: Boolean = false,
    onHoliday: (Boolean) -> Unit = {},
    onWeekReport: () -> Unit = {},
    packUpdates: Boolean = true,
    onPackUpdates: (Boolean) -> Unit = {},
    onCheckPacks: () -> Unit = {},
    onPlus: (() -> Unit)? = null,
    plusActive: Boolean = false,
    /** Zusätzliche Einstellungen unter „Brummi liest vor“ (Stimme, Tempo, Probe) bzw. unter „Ton“ (Lautstärken) */
    voiceExtras: @Composable () -> Unit = {},
    soundExtras: @Composable () -> Unit = {},
    parentExtras: @Composable ColumnScope.() -> Unit = {},
) {
    var editing by remember { mutableStateOf(false) }
    // Schnell-Schalter (Ton, Musik, Vorlesen, Aussehen) sind frei; alles Übrige liegt hinter dem Eltern-Tor (ELTERN-TOR.md, 19.11.2026)
    var unlocked by remember { mutableStateOf(de.wissensgarten.app.ui.common.ParentGate.isOpen()) }
    val gate = de.wissensgarten.app.ui.common.LocalParentGate.current
    var wipeStep by remember { mutableIntStateOf(0) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Einstellungen", style = MaterialTheme.typography.headlineMedium)

        // Ganz oben, damit man es findet
        val host = de.wissensgarten.app.ui.fx.LocalHost.current
        if (unlocked && host?.features?.selfUpdate == true) key(update?.version) {
            host.UpdateSection(version, update, autoUpdate, onAutoUpdate)
        }
        // WebApp: „App installieren“ (erscheint nur, wenn der Browser es kann und die App noch nicht installiert ist)
        if (host != null) InstallAppCard(host, inGarden = false)

        if (unlocked) syncCard()

        if (!unlocked) Section("🔒 Für Eltern") {
            Text("Hier liegen Plus, Updates, Sync, Erinnerung, Profile und „Alle Daten löschen“. Ton, Musik, Vorlesen und Aussehen darf jeder einstellen.",
                style = MaterialTheme.typography.bodySmall)
            Button(onClick = { gate("Der Elternbereich der Einstellungen: Plus, Updates, Sync, Daten löschen.") { unlocked = true } }, modifier = Modifier.fillMaxWidth()) {
                Text("👨‍👩‍👧 Elternbereich öffnen")
            }
        }

        if (unlocked) Section("Profil") {
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

        if (unlocked) Section("Für Eltern") {
            de.wissensgarten.app.ui.plus.PlusBadge(de.wissensgarten.app.ui.plus.LocalPlusStatus.current, onClick = onPlus)
            if (onPlus != null) Button(onClick = onPlus, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFFB8860B))) {
                Text(if (plusActive) "🌟 Wissensgarten Plus verwalten" else "🌟 Wissensgarten Plus – 7 Tage gratis testen")
            }
            OutlinedButton(onClick = onWeekReport, modifier = Modifier.fillMaxWidth()) { Text("📊 Wochenbericht ansehen") }
            Text("Gesunde Lernzeit pro Tag", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 10, 15, 20, 30, 45, 60).forEach { m ->
                    FilterChip(selected = dailyLimit == m, onClick = { onDailyLimit(m) }, label = { Text(if (m == 0) "frei" else "$m Min.") })
                }
            }
            Text(
                "Ist die Zeit um, verabschiedet sich Brummi freundlich: „Der Garten ruht sich aus 🌙“. Eine laufende Runde darf noch zu Ende gespielt werden.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("🏖️ Ferien-Modus")
                    Text("Die Pflanzen welken viermal langsamer, Erinnerungen pausieren. Lernen geht trotzdem – nichts geht verloren.",
                        style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = holiday, onCheckedChange = onHoliday)
            }
        }

        Section("Spielarten") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Antwort selbst eintippen")
                    Text("Ab Klasse 5 manchmal: die Antwort ganz aus dem Kopf schreiben. Kleine Tippfehler zählen als „fast“ (außer in Deutsch).",
                        style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = typing, onCheckedChange = onTyping)
            }
        }

        if (unlocked && host?.features?.packUpdates == true) Section("Neue Wissenspakete") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Automatisch nachladen")
                    Text("Brummi holt neue und verbesserte Pakete (nur Fragen und Texte) – ohne App-Update. Dein Fortschritt bleibt erhalten.",
                        style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = packUpdates, onCheckedChange = onPackUpdates)
            }
            OutlinedButton(onClick = onCheckPacks, modifier = Modifier.fillMaxWidth()) { Text("🐝 Jetzt nach neuen Paketen schauen") }
        }

        if (unlocked && host?.features?.reminders == true) Section("Erinnerung") {
            var denied by remember { mutableStateOf(false) }
            var pendingHour by remember { mutableIntStateOf(reminderHour) }
            val ask = host.rememberNotificationPermission { ok ->
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
            Text("Automatisch: für Grundschulkinder an. Mit dem kleinen Lautsprecher-Knopf 🔊 an jeder Karte geht's immer. Bei „Nie“ gibt es den Knopf nicht. Englisch, Französisch und Spanisch werden in der richtigen Sprache gesprochen.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            voiceExtras()
        }

        Section("Silben farbig") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                de.wissensgarten.core.data.ReadAloud.entries.forEach { r -> FilterChip(selected = r == syllables, onClick = { onSyllables(r) }, label = { Text(r.label) }) }
            }
            Text(de.wissensgarten.app.ui.fx.silbenString("Lesehilfe wie in der Fibel: Silben abwechselnd blau und rot. Automatisch für Klasse 1 und 2."),
                style = MaterialTheme.typography.bodySmall)
        }

        Section("Ton") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Geräusche", Modifier.weight(1f))
                Switch(checked = sound, onCheckedChange = onSound)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Vibration bei richtigen Antworten", Modifier.weight(1f))
                Switch(checked = vibration, onCheckedChange = onVibration)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Musik")
                    Text("Leise Melodien – tagsüber fröhlich, abends ein Schlaflied. Läuft schon andere Musik auf dem Handy, bleibt sie aus.",
                        style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = music, onCheckedChange = onMusic)
            }
            soundExtras()
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

        if (unlocked) Section("Für Eltern") { parentExtras() }

        if (unlocked) Section("Datenschutz & Daten") {
            Text("Keine Werbung, kein Konto nötig, keine Kennung. Ohne Sync bleibt der Lernstand auf diesem Gerät.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onPrivacy, modifier = Modifier.fillMaxWidth()) { Text("🔒 Datenschutzerklärung") }
            OutlinedButton(onClick = { wipeStep = 1 }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("🗑️ Alle Daten löschen") }
            TextButton(onClick = { de.wissensgarten.app.ui.common.ParentGate.close(); unlocked = false }) { Text("Eltern-Bereich wieder sperren") }
        }

        Section("Über") {
            Text("Wissensgarten · Version $version vom ${de.wissensgarten.app.AppInfo.DATE}")
            Text("$packCount Wissenspakete · $cardCount Karten", style = MaterialTheme.typography.bodySmall)
            LegalLinks()
        }

        if (unlocked && canDeleteProfile) TextButton(onClick = { confirmDelete = true }) {
            Text("Profil „${profile.name}“ löschen", color = MaterialTheme.colorScheme.error)
        }
    }

    if (wipeStep > 0) AlertDialog(
        onDismissRequest = { wipeStep = 0 },
        title = { Text(if (wipeStep == 1) "Alle Daten löschen?" else "Wirklich alles?") },
        text = { Text(if (wipeStep == 1) "Alle Profile, Gärten, der gesamte Lernstand und die Einstellungen werden gelöscht – auf diesem Gerät " +
            "und, falls der Sync verbunden ist, auch die Kopie im Google Drive. Das lässt sich nicht rückgängig machen."
            else "Letzte Rückfrage: Danach startet der Wissensgarten wie frisch installiert.") },
        confirmButton = { TextButton(onClick = { if (wipeStep == 1) wipeStep = 2 else { wipeStep = 0; onWipeAll() } }) {
            Text(if (wipeStep == 1) "Weiter" else "Endgültig löschen", color = MaterialTheme.colorScheme.error)
        } },
        dismissButton = { TextButton(onClick = { wipeStep = 0 }) { Text("Abbrechen") } },
    )
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
