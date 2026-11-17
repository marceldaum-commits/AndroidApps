package de.wissensgarten.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Datenschutzerklärung – eine Quelle für App-Bildschirm und Webseite (web/datenschutz/index.html wird
 * im Test daraus erzeugt). Für Eltern verständlich, ohne Juristendeutsch.
 */
object PrivacyText {
    const val TITLE = "Datenschutz im Wissensgarten"
    const val UPDATED = "Stand: November 2026"
    const val WEB_URL = "https://www.mathenachhilfe-online.com/datenschutz/"

    val sections: List<Pair<String, String>> = listOf(
        "Kurz gesagt" to
            "Keine Werbung. Kein Tracking. Keine Analyse-Werkzeuge. Kein Konto nötig. Wir verkaufen keine Daten – an niemanden. " +
            "Der Lernstand bleibt auf eurem Gerät, solange ihr nicht selbst den Geräte-Sync einschaltet.",
        "Anbieter" to
            "Wissensgarten wird von mathenachhilfe-online.com angeboten. Kontakt und Anschrift findet ihr im Impressum auf www.mathenachhilfe-online.com.",
        "Was auf dem Gerät gespeichert wird" to
            "Profile (Vorname oder Spitzname, Avatar-Bild, Klassenstufe), welche Karten wann gelernt wurden, gesammelte Tautropfen, " +
            "Deko und Gartenfreunde, Einstellungen, selbst gemalte Merkbilder und eingetragene Klassenarbeiten. Diese Daten verlassen das Gerät nicht – " +
            "außer ihr nutzt den Geräte-Sync (siehe unten).",
        "Mikrofon (Brummi-Radio, Sprech-Spiel)" to
            "Nur wenn euer Kind eine Sprech-Funktion startet und Android das Mikrofon freigegeben hat. Die Spracherkennung von Android " +
            "wandelt die gesprochene Antwort in Text um – je nach Gerät geschieht das auf dem Gerät oder über den Sprachdienst von Google. " +
            "Der Wissensgarten speichert keine Aufnahmen und sendet sie nirgendwohin.",
        "Vorlesen" to
            "Brummi liest mit der Sprachausgabe des Geräts (Android bzw. Browser) vor. Dabei werden nur die Texte der Lernkarten verwendet. " +
            "In der WebApp bieten wir nur Stimmen an, die auf dem Gerät sprechen – keine Online-Stimmen. " +
            "Auf Wunsch gibt es die natürliche Stimme „Thorsten“: Eltern können sie in den Einstellungen einmalig laden (ca. 63 MB). " +
            "Die Stimmdatei kommt dann von unserem eigenen Server (stimmen.mathenachhilfe-online.com); er sieht dabei, wie bei jedem Download, " +
            "die IP-Adresse und den Zeitpunkt – ohne Cookies, ohne Konto. Danach rechnet die Stimme ganz auf dem Gerät – kein Text verlässt das Gerät. " +
            "Die Stimmdatei lässt sich in den Einstellungen jederzeit wieder entfernen.",
        "Kamera und QR-Codes" to
            "Den QR-Code-Scanner für Klassen-Codes öffnen nur Eltern (nach der Eltern-Aufgabe). Er nutzt den Code-Scanner der Google-Play-Dienste " +
            "auf dem Gerät; der Wissensgarten bekommt nur den Text des Codes, kein Foto. In der GitHub-Version gibt es zusätzlich die Wissens-Safari: " +
            "Die Bilderkennung läuft auf dem Gerät, das Foto bleibt auf dem Gerät.",
        "Geräte-Sync mit Google (freiwillig)" to
            "Eltern können in den Einstellungen den Sync einschalten, um auf mehreren Geräten weiterzulernen. Dann meldet ihr euch mit eurem " +
            "Google-Konto an und der Wissensgarten legt eine Datei mit Profilen und Lernstand in einem versteckten App-Ordner eures Google Drive ab. " +
            "Nur die App auf euren Geräten kann diese Datei lesen – wir als Anbieter haben keinen Zugriff. Ihr könnt den Sync jederzeit trennen " +
            "und die Datei mit „Alle Daten löschen“ entfernen.",
        "Teilen: Duelle, Klassen-Codes, Postkarten, Garten-Film, Bastelbogen" to
            "Wenn ihr ein Duell, einen Klassen-Code, eine Postkarte, den Garten-Film oder einen Bastelbogen teilt, enthält das geteilte Bild oder der Text nur Fragen, Antworten, Punkte und den Spitznamen, den ihr selbst eingegeben habt. " +
            "Geteilt wird nur über die App, die ihr auswählt (z. B. einen Messenger). Teilen ist erst nach der Eltern-Aufgabe möglich.",
        "Erinnerungen" to
            "Die tägliche Gieß-Erinnerung ist eine Benachrichtigung, die das Gerät selbst plant. Es werden dafür keine Daten gesendet.",
        "Updates" to
            "App-Updates: In der Play-Store-Version kommen neue Versionen über Google Play – die App lädt sie im Hintergrund und spielt sie ein, " +
            "ohne dass der Store geöffnet werden muss. In der GitHub-Version fragt die App bei GitHub nach, ob es eine neue Version gibt. " +
            "Neue Wissenspakete: Höchstens einmal am Tag schaut die App bei GitHub (raw.githubusercontent.com, betrieben von GitHub Inc.) nach neuen oder " +
            "verbesserten Wissenspaketen und lädt sie herunter. Das sind nur Inhalte (Fragen, Texte, Merkbilder) – kein Programmcode. " +
            "Dabei wird nichts über euch oder euer Kind gesendet; GitHub sieht nur, wie bei jedem Webseitenaufruf, die IP-Adresse und den Zeitpunkt. " +
            "Das Nachladen der Pakete und (in der GitHub-Version) die Update-Suche lassen sich in den Einstellungen abschalten.",
        "Löschen" to
            "Einstellungen → „Alle Daten löschen“ entfernt alle Profile, den Lernstand, die Einstellungen und – falls verbunden – die Sync-Datei im Google Drive. " +
            "Auch das Deinstallieren der App löscht alle Daten auf dem Gerät.",
        "Für Kinder gemacht" to
            "Der Wissensgarten richtet sich an Kinder und ihre Eltern. Bereiche, in denen Daten das Gerät verlassen könnten (Sync, Teilen, Updates, Kamera), " +
            "sind durch eine Eltern-Aufgabe geschützt. Es gibt keine Werbung und keine Links zu sozialen Netzwerken.",
        "Käufe (Wissensgarten Plus)" to
            "Alle Pakete bis Klasse 10 und alle Spielarten sind frei. „Wissensgarten Plus“ ist freiwillig und wird nur im Eltern-Bereich nach der Eltern-Aufgabe gekauft. " +
            "WebApp und GitHub-Version: Der Kauf läuft über den Shop auf mathenachhilfe-online.com (WooCommerce). Dort gebt ihr die für Kauf und Rechnung nötigen Daten an; " +
            "Rechnung und Freischalt-Code kommen per E-Mail. In der App wird kein Konto angelegt. Der Code wird nur auf diesem Gerät gespeichert und offline geprüft " +
            "(digitale Unterschrift) – er enthält keinen Namen und keine E-Mail-Adresse. Der 7-Tage-Test wird ebenfalls nur auf dem Gerät vermerkt. " +
            "Mit aktivem Plus lädt die App höchstens einmal am Tag neue Plus-Pakete von unserem Server (wissensgarten.mathenachhilfe-online.com) und schickt dabei den Code mit, " +
            "damit der Server prüfen kann, ob Plus gültig ist. Der Server sieht dabei den Code, die IP-Adresse und den Zeitpunkt; daraus wird kein Profil gebildet, " +
            "Lernstand und Profile werden nie übertragen. In der Play-Store-Version gibt es noch keine Käufe – dort ist im Moment alles frei.",
        "Eure Rechte" to
            "Ihr habt nach der DSGVO das Recht auf Auskunft, Berichtigung, Löschung und Beschwerde bei einer Datenschutz-Aufsichtsbehörde. " +
            "Da wir selbst keine Daten von euch erhalten, könnt ihr alles direkt in der App einsehen und löschen. Bei Fragen schreibt uns über das Impressum.",
    )

    /** Webseite für den Play Store (Pflicht-Link „Datenschutzerklärung“). */
    fun html(): String = buildString {
        fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        append("<!doctype html>\n<html lang=\"de\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
        append("<title>${esc(TITLE)}</title>\n<style>body{font-family:system-ui,sans-serif;max-width:720px;margin:0 auto;padding:16px;line-height:1.55;color:#22261f;background:#fbf6ec}")
        append("h1{color:#3f6b3a}h2{color:#3f6b3a;font-size:1.1rem;margin-top:1.6em}.stand{color:#666}</style></head><body>\n")
        append("<h1>🌻 ${esc(TITLE)}</h1>\n<p class=\"stand\">${esc(UPDATED)}</p>\n")
        sections.forEach { (h, t) -> append("<h2>${esc(h)}</h2>\n<p>${esc(t)}</p>\n") }
        append("</body></html>\n")
    }
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück") }
            Text(PrivacyText.TITLE, style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(PrivacyText.UPDATED, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PrivacyText.sections.forEach { (h, t) ->
                Text(h, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text(t, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
