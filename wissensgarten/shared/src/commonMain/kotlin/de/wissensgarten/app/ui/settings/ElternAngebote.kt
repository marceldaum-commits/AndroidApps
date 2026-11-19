package de.wissensgarten.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import de.wissensgarten.core.data.Settings
import de.wissensgarten.core.model.Angebote

/**
 * Elternbereich (nur hinter dem Eltern-Tor): Lern-Geschenk, Schalter für die anonyme Zählung und die Nachhilfe-Hinweise,
 * Absender-Hinweis. Keine Figuren, keine Preise, kein Druck.
 */
@Composable
fun ElternAngebote(settings: Settings, onGift: () -> Unit, kopplung: @Composable () -> Unit = {}) {
    ElternAngeboteContent(kopplung = kopplung,
        countUsage = settings.countUsage.collectAsState().value, onCountUsage = { settings.setCountUsage(it) },
        tutorHints = settings.tutorHints.collectAsState().value, onTutorHints = { settings.setTutorHints(it) },
        onGift = onGift,
        onlineSprache = settings.onlineSprache.collectAsState().value, onOnlineSprache = { settings.setOnlineSprache(it) },
    )
}

@Composable
fun ElternAngeboteContent(
    countUsage: Boolean, onCountUsage: (Boolean) -> Unit,
    tutorHints: Boolean, onTutorHints: (Boolean) -> Unit,
    onGift: () -> Unit,
    kopplung: @Composable () -> Unit = {},
    onlineSprache: Boolean = false, onOnlineSprache: (Boolean) -> Unit = {},
) {
    val uri = LocalUriHandler.current
    kopplung()
    Spacer(Modifier.height(4.dp))
    OutlinedButton(onClick = { onGift(); runCatching { uri.openUri(Angebote.geschenk) } }, modifier = Modifier.fillMaxWidth()) {
        Text("🎁 Lern-Geschenk verschenken")
    }
    Text("Zu Weihnachten oder zum Geburtstag: Wissensgarten Plus oder Nachhilfe als Karte zum Ausdrucken. Die App bleibt kostenlos nutzbar.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Anonyme Nutzungszählung")
            Text("Hilft uns, die App zu verbessern. Gezählt wird nur, dass etwas geöffnet oder angetippt wurde – ohne Namen, ohne Kennung, ohne Lernstand.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = countUsage, onCheckedChange = onCountUsage)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Hinweise auf unsere Nachhilfe zeigen")
            Text("Nur im Wochenbericht: Wenn ein Thema länger schwerfällt oder eine Klassenarbeit ansteht. Höchstens alle zwei Wochen, jederzeit ausblendbar.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = tutorHints, onCheckedChange = onTutorHints)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Online-Spracherkennung erlauben")
            // Wortlaut von Recht freigegeben (V6, 19.11.2026) – nicht ändern
            Text("Die gesprochenen Antworten schickt der Browser dann an den Sprachdienst seines Herstellers (z. B. Google bei Chrome, Apple bei Safari). " +
                "Der Hersteller wandelt sie in eigener Verantwortung in Text um, gegebenenfalls auch außerhalb der EU. Wir erhalten und speichern dabei nichts. " +
                "Ohne diese Erlaubnis erkennt der Garten Sprache nur, wenn das Gerät es selbst kann – sonst gibt es Antwort-Knöpfe. " +
                "Ihr könnt die Erlaubnis hier jederzeit wieder ausschalten. Mehr in der Datenschutzerklärung (Abschnitt 10k).",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = onlineSprache, onCheckedChange = onOnlineSprache)
    }
    TextButton(onClick = { runCatching { uri.openUri(Angebote.fusszeile) } }) { Text("Ein Angebot von Nachhilfe – online") }
}
