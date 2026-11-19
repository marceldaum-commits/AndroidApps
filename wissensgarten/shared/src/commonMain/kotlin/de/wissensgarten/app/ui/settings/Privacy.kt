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

    // Vorerst gibt es den Wissensgarten nur als WebApp (die Android-App ruht) – der Text beschreibt darum die WebApp.
    // Wenn die Android-App zurückkommt: Fassung für Android (Sync, Erinnerungen, Kamera, Updater) aus der Git-Geschichte holen.
    val sections: List<Pair<String, String>> = listOf(
        "Kurz gesagt" to
            "Keine Werbung. Keine Analyse-Werkzeuge, keine Cookies, keine Kennung. Kein Konto nötig. Wir verkaufen keine Daten – an niemanden. " +
            "Der Lernstand bleibt in eurem Browser auf eurem Gerät.",
        "Anbieter" to
            "Wissensgarten wird von mathenachhilfe-online.com angeboten. Kontakt und Anschrift findet ihr im Impressum auf www.mathenachhilfe-online.com.",
        "Was im Browser gespeichert wird" to
            "Profile (Vorname oder Spitzname, Avatar-Bild, Klassenstufe), welche Karten wann gelernt wurden, gesammelte Tautropfen, " +
            "Deko und Gartenfreunde, Einstellungen, selbst gemalte Merkbilder und eingetragene Klassenarbeiten. Das alles liegt nur im Speicher " +
            "eures Browsers auf diesem Gerät und wird nicht an uns gesendet. Es gibt keinen Geräte-Sync – auf einem anderen Gerät beginnt ein neuer Garten. " +
            "Damit der Garten auch ohne Internet startet, legt der Browser die Programmdateien der Web-App in seinem Zwischenspeicher ab.",
        "Mikrofon (Brummi-Radio, Sprech-Spiel)" to
            "Das Mikrofon wird nur benutzt, wenn euer Kind eine Sprech-Funktion selbst startet und ihr dem Browser das Mikrofon erlaubt habt. " +
            "Erkannt wird die Sprache dann nur auf dem Gerät selbst, wenn es das kann; sonst gibt es Antwort-Knöpfe. " +
            "Nur wenn ihr im Elternbereich „Online-Spracherkennung erlauben“ einschaltet (voreingestellt aus), schickt der Browser die gesprochenen Antworten " +
            "an den Sprachdienst seines Herstellers (z. B. Google bei Chrome, Apple bei Safari). Der Wissensgarten speichert keine Aufnahmen und sendet sie nirgendwohin.",
        "Vorlesen" to
            "Vorgelesen wird mit der Stimme Nova, einer mit KI (OpenAI) erzeugten Stimme. Feste Texte vertonen wir vorab. " +
            "Texte, die erst in der Aufgabe entstehen (z. B. Hör-noch-mal-Sätze im Brummi-Radio), schickt die App ohne Angaben zu dir " +
            "an unseren Server stimmen.mathenachhilfe-online.com; dieser lässt sie einmalig von OpenAI vertonen und speichert die Audiodatei. " +
            "Es werden keine Namen, Konten oder Geräte-Kennungen übertragen, und unser Server speichert keine Zugriffsprotokolle.",
        "Anonyme Nutzungszählung (abschaltbar)" to
            "Damit wir sehen, ob die App gebraucht wird und was wir verbessern sollten, meldet sie einzelne Ereignisse an unseren Server " +
            "trainer.mathenachhilfe-online.com: dass die App geöffnet wurde, dass der Elternbereich geöffnet wurde und ob dort ein Link (z. B. Plus, Probestunde, Lern-Geschenk) angetippt wurde. " +
            "Übertragen werden nur App-Name, Versionsnummer, Ereignis und gegebenenfalls Ort und Thema (z. B. „Bruchrechnung“) – keine Namen, keine Kennung, keine Gerätedaten, kein Lernstand, keine Cookies. " +
            "Der Server zählt nur Tagessummen und speichert für diese Aufrufe keine IP-Adressen. Ausschalten: Einstellungen → Für Eltern → „Anonyme Nutzungszählung“.",
        "Eltern-Handy koppeln (nur wenn ihr es einschaltet)" to
            "Koppelt ihr im Elternbereich euer Handy, legt der Wissensgarten nach Lernrunden (höchstens alle 10 Minuten) einen kurzen Lernbericht auf unserem Server trainer.mathenachhilfe-online.com ab: " +
            "Klassenstufe, Lernzeit, aktive Tage, Anzahl und Anteil richtiger Antworten, Themen, die gut sitzen oder schwerfallen, Fortschritt je Beet, die nächste eingetragene Klassenarbeit und – nur wenn ihr ihn eintragt – einen Spitznamen. " +
            "Der Bericht wird auf diesem Gerät mit AES-256 verschlüsselt; den Schlüssel enthält nur euer Link (der Teil nach „#“ wird nie an einen Server gesendet). Unser Server kann den Bericht daher nicht lesen. " +
            "„Kopplung aufheben“ löscht ihn sofort; ohne Aktualisierung wird er nach 90 Tagen gelöscht. Ohne Kopplung wird nichts gesendet. " +
            "Bei gekoppeltem Eltern-Handy wird zusätzlich eine Ende-zu-Ende-verschlüsselte Sicherung des Lernstands gespeichert (siehe Datenschutzerklärung 10i). " +
            "Mit dem Kopplungs-Link lässt sie sich auf einem neuen Gerät wiederherstellen („Fortschritt wiederherstellen“).",
        "Umzug auf die gemeinsame Adresse (einmalig)" to
            "Alle unsere Lern-Apps liegen jetzt unter lernen.mathenachhilfe-online.com. Wer den Wissensgarten noch unter der alten Adresse öffnet, " +
            "dessen Lernstand zieht einmal automatisch mit: Er wird auf diesem Gerät mit AES-256 verschlüsselt, kurz auf unserem Server trainer.mathenachhilfe-online.com abgelegt " +
            "und von der neuen Adresse sofort abgeholt und dort gelöscht. Den Schlüssel enthält nur der Weiterleitungs-Link (der Teil nach „#“ wird nie an einen Server gesendet) – " +
            "unser Server kann die Daten nicht lesen. Spätestens nach 30 Tagen wird eine nicht abgeholte Umzugs-Sicherung gelöscht. Auf der alten Adresse bleibt der Lernstand im Browser unverändert.",
        "Hinweise auf unsere Nachhilfe" to
            "Im Wochenbericht (nur hinter dem Eltern-Tor, nie im Kinderbereich) kann ein sachlicher Hinweis auf unsere Online-Nachhilfe erscheinen, wenn ein Thema länger schwerfällt oder eine Klassenarbeit ansteht. " +
            "Das wird nur auf dem Gerät berechnet; der Lernstand wird dafür nicht gesendet. Höchstens alle zwei Wochen je Thema, jederzeit ausblendbar oder ganz abschaltbar.",
        "Kamera" to
            "Die Web-App nutzt keine Kamera.",
        "Teilen: Duelle, Klassen-Codes, Garten-Film, Bastelbogen" to
            "Wenn ihr ein Duell, einen Klassen-Code, den Garten-Film oder einen Bastelbogen teilt, enthält das geteilte Bild oder der Text nur Fragen, Antworten, Punkte und den Spitznamen, den ihr selbst eingegeben habt. " +
            "Geteilt wird nur über die App, die ihr auswählt (z. B. einen Messenger). Teilen ist erst nach der Eltern-Aufgabe möglich.",
        "Auf den Startbildschirm legen" to
            "Ihr könnt die Web-App wie eine App auf den Startbildschirm legen („App installieren“ bzw. auf iPhone und iPad „Zum Home-Bildschirm“). " +
            "Das erledigt euer Browser; dabei werden keine Daten an uns gesendet.",
        "Updates" to
            "Die Web-App hält sich selbst aktuell: Beim Öffnen und ab und zu währenddessen fragt sie bei unserem Server " +
            "(lernen.mathenachhilfe-online.com), ob es eine neue Version gibt, und lädt sie in einem ruhigen Moment – nie mitten in einer Runde. " +
            "Neue und verbesserte Wissenspakete kommen mit der neuen Version. Dabei wird nichts über euch oder euer Kind gesendet; " +
            "der Server sieht nur, wie bei jedem Webseitenaufruf, die IP-Adresse und den Zeitpunkt.",
        "Löschen" to
            "Einstellungen → „Alle Daten löschen“ entfernt alle Profile, den Lernstand und die Einstellungen aus dem Browser. " +
            "Auch das Löschen der Website-Daten im Browser (bzw. das Entfernen der installierten Web-App) löscht alles auf dem Gerät.",
        "Für Kinder gemacht" to
            "Der Wissensgarten richtet sich an Kinder und ihre Eltern. Bereiche, in denen Daten das Gerät verlassen könnten (Teilen, Käufe), " +
            "sind durch eine Eltern-Aufgabe geschützt. Es gibt keine Werbung und keine Links zu sozialen Netzwerken.",
        "Käufe (Wissensgarten Plus)" to
            "Alle Pakete bis Klasse 10 und alle Spielarten sind frei. „Wissensgarten Plus“ ist freiwillig und wird nur im Eltern-Bereich nach der Eltern-Aufgabe gekauft. " +
            "Der Kauf läuft über den Shop auf mathenachhilfe-online.com (WooCommerce). Dort gebt ihr die für Kauf und Rechnung nötigen Daten an; " +
            "Rechnung und Freischalt-Code kommen per E-Mail. In der Web-App wird kein Konto angelegt. Der Code wird nur in diesem Browser gespeichert und offline geprüft " +
            "(digitale Unterschrift) – er enthält keinen Namen und keine E-Mail-Adresse. Der 7-Tage-Test wird ebenfalls nur im Browser vermerkt. " +
            "Mit aktivem Plus lädt die Web-App höchstens einmal am Tag neue Plus-Pakete von unserem Server (wissensgarten.mathenachhilfe-online.com) und schickt dabei den Code mit, " +
            "damit der Server prüfen kann, ob Plus gültig ist. Der Server sieht dabei den Code, die IP-Adresse und den Zeitpunkt; daraus wird kein Profil gebildet, " +
            "Lernstand und Profile werden nie übertragen.",
        "Eure Rechte" to
            "Ihr habt nach der DSGVO das Recht auf Auskunft, Berichtigung, Löschung und Beschwerde bei einer Datenschutz-Aufsichtsbehörde. " +
            "Da wir selbst keine Daten von euch erhalten, könnt ihr alles direkt in der Web-App einsehen und löschen. Bei Fragen schreibt uns über das Impressum.",
    )

    /** Eigene Webseite mit derselben Datenschutzerklärung (web/datenschutz/). */
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
