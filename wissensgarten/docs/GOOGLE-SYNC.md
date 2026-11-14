# Geräte-Sync mit Google – Einrichtung

Der Wissensgarten speichert auf Wunsch den Garten der ganzen Familie im **versteckten App-Ordner** (`appDataFolder`)
des eigenen Google Drive. Die App bekommt nur die Berechtigung `drive.appdata`: Sie sieht nichts anderes im Drive,
und andere Apps sehen den Ordner nicht. Kein eigener Server, keine Konten bei uns.

## Was wie zusammengeführt wird (`core/model/Sync.kt`)

| Daten | Regel |
| --- | --- |
| Lernstand einer Karte | der neueste (jüngste Wiederholung) gewinnt |
| Tautropfen | Änderungen beider Geräte seit dem letzten Abgleich werden verrechnet |
| Name, Avatar, Klasse, Beet-Pflanzenart | was auf einem Gerät geändert wurde, gewinnt |
| Gekauftes, Kapitel, Kreuzungen, Truhen | vereinigt / Maximum |
| Gieß-Protokoll (120 Tage) | vereinigt, doppelte einmal |
| Gelöschtes | bleibt gelöscht (Liste gelöschter IDs) |
| „Lena“ auf zwei Geräten getrennt angelegt | wird über den Namen zu einem Profil zusammengeführt (gleiche Pakete/Karten ebenso) und bleibt verbunden |

Selbst gemalte Merkbilder werden (noch) nicht abgeglichen.

## Einmalige Einrichtung in der Google Cloud Console

Ohne diese Einrichtung zeigt die App: „Google-Anmeldung ist für diese App noch nicht eingerichtet.“

1. Projekt wählen (z. B. „Wissensgarten-Build“) – https://console.cloud.google.com
2. **Google Drive API** aktivieren: https://console.cloud.google.com/apis/library/drive.googleapis.com
3. **OAuth-Zustimmungsbildschirm** (Google Auth Platform → Branding/Zielgruppe):
   - App-Name „Wissensgarten“, Support-E-Mail, Entwickler-Kontakt
   - Zielgruppe **Extern** (sonst können nur Konten von mathenachhilfe-online.com sich anmelden)
   - Datenzugriff: Bereich `https://www.googleapis.com/auth/drive.appdata` hinzufügen
   - Veröffentlichungsstatus **„In Produktion“** (im Testmodus nur eingetragene Testnutzer).
     `drive.appdata` ist kein „sensibler“ Bereich – eine Google-Prüfung ist dafür normalerweise nicht nötig.
4. **OAuth-Client** anlegen (Clients → Client erstellen):
   - Typ **Android**
   - Paketname: `de.wissensgarten.app`
   - SHA-1 des Upload-Schlüssels: `D8:9E:BD:D8:5E:85:D9:83:63:74:D6:A8:46:A6:A6:02:7C:D4:5D:52`
5. Fertig – in der App: Einstellungen → „Auf allen Geräten“ → **Mit Google-Konto verbinden**.

Für eine spätere Play-Store-Version kommt ein zweiter Android-Client mit dem SHA-1 des Play-App-Signaturschlüssels dazu.
