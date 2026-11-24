# Texte-Abgleich für Recht (Stand 2.16.2 vom 24.11.2026)

Recht kann das Repository nicht lesen. Hier stehen die Stellen, an denen die Texte und Regeln stehen (Repo `marceldaum-commits/AndroidApps`, Ordner `experimentopia/`, Branch `ccr-2ccbaa44-nkkmyu`). Wortlaute: `docs/RECHT-VORGABEN.md`.

| Thema | Fundstelle |
|---|---|
| Eltern-Tor: Titel, Aufgabe in Worten, Fehlertext, Tastatur, Abbrechen | `shared/src/commonMain/kotlin/de/experimentopia/app/ui/settings/ParentGate.kt:43`, `shared/src/commonMain/kotlin/de/experimentopia/app/ui/settings/ParentGate.kt:46`, `shared/src/commonMain/kotlin/de/experimentopia/app/ui/settings/ParentGate.kt:51` |
| Eltern-Tor: Zahlwörter („siebzehn mal acht“), Beschreibung für Bildschirmleser | `core/model/src/commonMain/kotlin/de/experimentopia/core/model/ParentGateTask.kt:20`, `core/model/src/commonMain/kotlin/de/experimentopia/core/model/ParentGateTask.kt:28` |
| Schloss (Kinder): „… gehört zum Forscher-Pass.“ | `core/model/src/commonMain/kotlin/de/experimentopia/core/model/Say.kt:17` |
| Schloss-Seite für Kinder: „Für Eltern“, „Hier geht es nur mit einer Frage für Erwachsene weiter.“, Frei bleibt | `shared/src/commonMain/kotlin/de/experimentopia/app/ui/settings/SettingsScreen.kt:155`, `shared/src/commonMain/kotlin/de/experimentopia/app/ui/settings/SettingsScreen.kt:156` |
| Probestunde V15 (2) Kinder-Hilfesatz und Knöpfe | `core/model/src/commonMain/kotlin/de/experimentopia/core/model/Nachhilfe.kt:7`, `core/model/src/commonMain/kotlin/de/experimentopia/core/model/Nachhilfe.kt:12`, `core/model/src/commonMain/kotlin/de/experimentopia/core/model/Nachhilfe.kt:13` |
| Probestunde V15 (3) Tipp Klasse 9–10 und Knopf „Mehr erfahren“ | `core/model/src/commonMain/kotlin/de/experimentopia/core/model/Nachhilfe.kt:8`, `core/model/src/commonMain/kotlin/de/experimentopia/core/model/Nachhilfe.kt:15`, `core/model/src/commonMain/kotlin/de/experimentopia/core/model/Nachhilfe.kt:16` |
| Probestunde V15 (1) Elternhinweis (Wortlaut), Knöpfe, Schalter | `core/model/src/commonMain/kotlin/de/experimentopia/core/model/Nachhilfe.kt:18`, `core/model/src/commonMain/kotlin/de/experimentopia/core/model/Nachhilfe.kt:19`, `core/model/src/commonMain/kotlin/de/experimentopia/core/model/Nachhilfe.kt:20` |
| Probestunde: Auslöser und Pausen (nur Gerät, 1× je 4 Wochen je Thema, 1× pro Woche Tipp) | `core/model/src/commonMain/kotlin/de/experimentopia/core/model/Nachhilfe.kt:34`, `core/model/src/commonMain/kotlin/de/experimentopia/core/model/Nachhilfe.kt:35`, `core/model/src/commonMain/kotlin/de/experimentopia/core/model/Nachhilfe.kt:41` |
| Probestunde: Anzeige Elternbereich / Tipp (wegklickbar) | `shared/src/commonMain/kotlin/de/experimentopia/app/ui/settings/ParentArea.kt:54`, `shared/src/commonMain/kotlin/de/experimentopia/app/ui/home/NachhilfeTipp.kt:27`, `shared/src/commonMain/kotlin/de/experimentopia/app/ui/home/NachhilfeTipp.kt:31` |
| Probestunde: Link nur ?src=… (keine Thema/Klasse), Zählung ohne Thema | `core/model/src/commonMain/kotlin/de/experimentopia/core/model/Zaehlung.kt:25`, `core/model/src/commonMain/kotlin/de/experimentopia/core/model/Zaehlung.kt:47` |
| Familien-Speicher: Pflichttext, Datenschutz-Link 10m, Knöpfe anlegen/beitreten | `core/model/src/commonMain/kotlin/de/experimentopia/core/model/Familie.kt:19`, `core/model/src/commonMain/kotlin/de/experimentopia/core/model/Familie.kt:24`, `core/model/src/commonMain/kotlin/de/experimentopia/core/model/Familie.kt:26` |
| Familien-Speicher: Sicherheitshinweis (Familien-Seite, Link/QR, Notfall-Blatt) | `core/model/src/commonMain/kotlin/de/experimentopia/core/model/Familie.kt:29`, `shared/src/commonMain/kotlin/de/experimentopia/app/ui/settings/FamilyCard.kt:205`, `shared/src/commonMain/kotlin/de/experimentopia/app/ui/settings/FamilyCard.kt:210` |
| Familien-Speicher: Löschen (Warnung, Familien-Nummer, „Familie gibt es nicht mehr“) | `core/model/src/commonMain/kotlin/de/experimentopia/core/model/Familie.kt:31`, `core/model/src/commonMain/kotlin/de/experimentopia/core/model/Familie.kt:33`, `core/model/src/commonMain/kotlin/de/experimentopia/core/model/Familie.kt:90` |
| Familien-Speicher: Einwilligungsdialog, Gate vor Anlegen/Beitreten | `shared/src/commonMain/kotlin/de/experimentopia/app/ui/settings/FamilyCard.kt:232`, `shared/src/commonMain/kotlin/de/experimentopia/app/ui/settings/FamilyCard.kt:238` |
| Elternbereich hinter dem Tor (Pro, Preis, Kauf, Eltern-Handy, Familie, Zurücksetzen, Zählung) | `shared/src/commonMain/kotlin/de/experimentopia/app/ui/settings/SettingsScreen.kt:66`, `shared/src/commonMain/kotlin/de/experimentopia/app/ui/settings/SettingsScreen.kt:79` |
| Datenschutz-Text in der App (ohne Preis, ohne Vorlese-Stimme) | `shared/src/commonMain/kotlin/de/experimentopia/app/ui/settings/SettingsScreen.kt:27` |
| Startanimation (Splash, atmet ≈ 0,4 Hz, Ausblenden < 1 s, überspringbar, reduced-motion) | `web/src/wasmJsMain/resources/index.html:53`, `web/src/wasmJsMain/resources/index.html:54`, `web/src/wasmJsMain/resources/index.html:58` |
| Lizenzhinweis Stimme (kein fremder Anbieter) | `LIZENZEN.md:7` |

## Wortlaut im Programmtext (direkt aus dem Code)

**KIND_HILFE**

> Du kommst hier gerade nicht weiter? Das ist okay. Probier eine leichtere Stufe oder schau dir die Erklärung noch einmal an. Und du kannst jederzeit deine Eltern oder deine Lehrkraft fragen.

**TIPP_OBERSTUFE**

> Tipp: Wenn ein Thema länger hakt, kann eine persönliche Erklärung helfen. Mit deinen Eltern könnt ihr bei uns eine kostenlose, unverbindliche Probestunde anfragen.

**ELTERN_ANGEBOT**

> Unser Angebot – eine kostenlose, unverbindliche Probestunde Online-Nachhilfe.

**SCHALTER**

> Hinweise auf Nachhilfe ausblenden

**PFLICHTTEXT**

> Eure Lernstände werden verschlüsselt auf unserem Server in Deutschland gespeichert. Der Schlüssel liegt auf euren Geräten. Damit wir euch helfen können, falls ihr den Familien-Zugang verliert, hinterlegen wir zusätzlich eine verschlüsselte Notfall-Kopie. Sie lässt sich nur mit einem getrennt und offline aufbewahrten Schlüssel von Mathenachhilfe Online öffnen. Technisch könnten wir damit eure Lernstände lesen; wir tun das nicht und nutzen ihn nur, wenn ihr uns um Wiederherstellung bittet. Jede Wiederherstellung seht ihr hier im Protokoll.

**SICHERHEIT**

> Wer den Familien-Link oder den QR-Code hat, kann die Lernstände eurer Familie öffnen, ändern und löschen. Teilt ihn nur mit Personen, denen ihr vertraut, und bewahrt das Notfall-Blatt sicher auf.

**LOESCHEN_WARNUNG**

> Das löscht die Familie in ALLEN Lern-Apps: alle Lernstände aller Kinder auf unserem Server, die Notfall-Kopie und das Protokoll. Gekaufte Codes werden wieder frei und lassen sich in einer neuen Familie erneut einlösen. Die Lernstände auf diesem Gerät bleiben erhalten.

**FAMILIE_WEG**

> Diese Familie gibt es nicht mehr. Lernstand bleibt auf diesem Gerät.

**Schloss (Kinder):** „<Versuch>“ gehört zum Forscher-Pass.

