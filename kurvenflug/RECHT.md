# Rechtliche Vorgaben für KurvenFlug (Raum „Recht“ / Marcel, Stand 19.11.2026)

Gesammelt aus den Mitteilungen der AppZentrale. Wortlaute **genau so** übernehmen. Umsetzung ab Di 24.11. (Pause bis dahin).
Jede Live-Meldung enthält „Rechtsbezug: ja/nein (was)“.

## Schloss-Text (Kinderansicht)
Nur: **„Das gehört zu KurvenFlug Pro.“** – kein Preis, kein Kaufknopf, keine Aufforderung (auch nicht „Deine Eltern können …“).
Dazu neutral „← Zurück“ und „Elternbereich (für Erwachsene)“ (→ Eltern-Tor). Umgesetzt auf dem Branch (658e335), Test `KidsNoPurchaseAppealTest`.

## A) Weg zur Probestunde (Recht V15)
1. **Elternbereich / Elternbericht** (nur hinter dem Tor):
   - Auslöser objektiv, z. B. ein Thema über 2 Wochen unter 50 % richtig. Berechnung NUR auf dem Gerät.
   - Höchstens 1× in 4 Wochen je Thema (heute `ParentOffers.HINT_PAUSE_DAYS = 7` → umstellen), wegklickbar,
     Schalter „Hinweise auf Nachhilfe ausblenden“. Als eigenes Angebot erkennbar; keine Angst, keine Noten-Prognose, keine Dringlichkeit.
   - Wortlaut: „Hinweis: In <Thema> hat <Spitzname> in den letzten Wochen öfter Schwierigkeiten. Das ist normal und lässt sich üben – die App zeigt dazu passende Übungen. Wenn ihr persönliche Unterstützung möchtet: Unser Angebot – eine kostenlose, unverbindliche Probestunde Online-Nachhilfe. [Probestunde anfragen] · [Solche Hinweise ausblenden]“
   - [Probestunde anfragen] → `https://www.mathenachhilfe-online.com/probestunde/` (optional `?src=kurvenflug-…`).
2. **Kinderansicht bei Schwierigkeiten** – NUR: „Du kommst hier gerade nicht weiter? Das ist okay. Probier eine leichtere Stufe oder schau dir die Erklärung noch einmal an. Und du kannst jederzeit deine Eltern oder deine Lehrkraft fragen.“
   Knöpfe [Leichter üben] [Erklärung ansehen]. KEIN Außenlink, kein Bezug auf Probestunde oder Elternbereich.
3. **Kl. 9–10, ohne Tor** (höchstens 1× pro Woche, wegklickbar, kein Preis):
   „Tipp: Wenn ein Thema länger hakt, kann eine persönliche Erklärung helfen. Mit deinen Eltern könnt ihr bei uns eine kostenlose, unverbindliche Probestunde anfragen. [Mehr erfahren]“
   [Mehr erfahren] → `https://www.mathenachhilfe-online.com/probestunde/` (optional `?src=`). Kl. 5–8: nur hinter dem Tor.
- Nie „gratis Nachhilfe“ ohne das Wort „Probestunde“. Zählung „Probestunden-Link angetippt“ nur anonym, ohne Thema/Lernstand
  (heute sendet `UsageCount.single` ein `thema` mit → für diesen Klick weglassen).

## B) Website-Link ohne Tor (ersetzt V10.2, Marcel „Ohne Tor, neutral“)
- Erlaubt ohne Tor: „← Alle Apps“, „Website von Nachhilfe Online“, „Über uns“. Ziel nur Startseite oder `/lern-apps/` (später Übersicht auf lernen.…), nie Shop/Produkt/Warenkorb/Kasse/Geschenke; nur `src`-Parameter.
- Nicht in Kinderansichten: Kaufsprache („Pro holen“, „Shop“, „Angebote entdecken“, „Rabatt“, „Jetzt freischalten“, „Mehr bekommen“),
  Figuren, die auf den Link zeigen, Link als Belohnung.
- WEITER HINTER DEM TOR: Kauf-/Shop-Knöpfe, Elternbereich, **Einstellungen**, Kopplung/Familie, Löschen, Kamera/Klassen-Code.
  → GEKLÄRT 19.11. (gilt für alle Apps): OHNE Tor als Schnell-Schalter: Ton, Musik, Lautstärke, Vorlesen an/aus + Tempo, Schriftgröße, Hell/Dunkel.
    HINTER dem Tor: Pro, Update, Daten zurücksetzen, Profile löschen, Datenschutz- und Zähl-Schalter, Familie, Online-Erkennung,
    Hinweise auf Nachhilfe, Klassen-Code/Kamera, Teilen. KurvenFlug-Einstellungen am Di entsprechend aufteilen.
- App-zu-App (V10b): unter Kl. 9 zu Raumlicht nur galton, zufall, rotation, raumflug oder freie Aufgaben (umgesetzt: `?ziel=level/3-1`, fb26aa5). Mathe-Trainer hat ab Di selbst ein Tor.

## C) Eltern-Tor-Standard
Malaufgabe 13–19 × 6–9 in Worten (mit Vorlese-Knopf), eigene Ziffern-Tastatur, nach einem Fehler neue Aufgabe, kein Zeitdruck,
Abbrechen jederzeit, Bildschirmleser-Beschreibung, 10 Minuten offen (nur Arbeitsspeicher), Grund sachlich. Gelöstes Tor zählt als „elternbereich“.
Vorlese-Knopf im Tor (Marcel 21.11.): „erst ohne, dann Tonstudio“ – zunächst OHNE Vorlese-Knopf, später feste Tonstudio-Sätze (Rechenblitz liefert 84 Sätze de/en/uk). Keine Gerätestimme.

## D) Familien-Speicher
Pflichttexte A6/V8 und Lösch-Regel: siehe `web/familie/README.md`.

## E) Zählwert „umzug“ (Recht + Leitstand 24.11., freigegeben mit Auflagen)
- Senden einmal pro Gerät, nur im Schritt, in dem die Übernahme abgeschlossen wird: `kf.js umzugHolen()`, Zweig `res === 'ok'`.
- Einmal-Merker = das vorhandene Flag `kurvenflug.umzug.angekommen` (Umzugsfunktion selbst, kein eigenes Zähl-Merkmal speichern).
- Nur senden, wenn der Zähl-Schalter NICHT „aus“ ist (nach der Übernahme `kurvenflug.anonCount`). Inhalt `{"app":"kurvenflug","ev":"umzug","v":"<Version>"}`, same-origin `/zaehler/v1/e`, keine Kennung, keine Uhrzeit.
- Datenschutzerklärung nur ergänzen, wenn sie Ereignisse einzeln auflistet (Meldung an AppZentrale).
- Zählung: alle Sendungen (auch `kf.js leave()`) von trainer.… auf same-origin `/zaehler/v1/e` umstellen.
