# Quasselquest Pro – Vorbereitung (Stand 19.11.2026, Weitergabe Leitstand über AppZentrale)

Marcel: „erstelle und dann soll sie es freischalten wenn es sie gibt.. einfach vorbereiten alles“. Reihenfolge: nach 0.4.0 + PC-Ansicht.
**Versionsnummer (Marcel 19.11.):** Die Version, mit der Pro kommt, heißt **1.5.0** – danach normal weiter (1.5.1, 1.6.0 …). Bis dahin 0.4.x.

## Codes
- Format wie alle Lern-Apps; öffentlicher Ed25519-Schlüssel `979eb515eb05b04dcc42e5fb776f108cdbdc86bb3c7cf28e05721fdde9e6b812`
  (Vorbild: Raumlicht Pro.kt / KurvenFlug pro_code.py).
- Payload 9 B: `[1, 7 (App), Sprach-Byte] + Serie (4 B) + Tag (2 B, Tage seit 01.01.2020)` + 64 B Signatur → Crockford-Base32, Präfix „QQ-“.
- Sprach-Byte: 1 Englisch, 2 Deutsch, 3 Französisch, 4 Latein, 5 Spanisch, 9 alle Sprachen. Gekauft = für immer.
- Link: `…/quasselquest/#pro=CODE` und `?pro=CODE`.
- Vor jeder Offline-Prüfung `codeGesperrt()` (familie.js); Familien-Speicher: Recht `produkt` „pro-en“ … „pro-alle“, codeNummer „L7.<sprache>.<serie>“.
- Kaufschutz: Wissensgarten-Plus-Codes (App 3, Produkt 1 oder 3) schalten Englisch + Deutsch frei – offline mit demselben Schlüssel prüfen.
- Ed25519 im Browser: WebCrypto kann es nicht überall (ältere iPhones) → kleine eigene Prüf-Funktion mitliefern (wie X25519 in familie.js), mit RFC-8032-Testvektoren.

## Shop
„Quasselquest Pro“, je Sprache 7,99 €, alle Sprachen 19,99 € (SKUs QQ-PRO-EN/DE/FR/LA/ES/ALLE) – PRIVAT, bis wir die Code-Prüfung auf test melden.

## Kostenlos bleibt (Marcel)
Je Sprache: erste Welt, erste Episode jeder Welt, alle Spielarten, Grundwortschatz; Lernpfad Abschnitt 1 vollständig, ab Abschnitt 2 je erste Einheit;
Sicherung/Kopplung immer; Nachhilfe-Hinweis für alle Sprachen. Mein Schulbuch gratis (USP-Auftrag 1).

## Melden, wenn auf test (dann schaltet der Leitstand den Shop frei)
1. Code-Prüfung per Eingabe und Link mit Test-Codes (Test-Codes über AppZentrale → Marcel → Leitstand anfragen)
2. Sperre je Sprache  3. Kaufschutz Wissensgarten-Plus  4. Kauf nur hinter dem Eltern-Tor, keine Preise im Kinderteil, Schloss-Standardtext
5. codeGesperrt() vor Offline-Prüfung

## Recht (19.11.): freigegeben mit Auflagen
- Schloss-Text in der Kinderansicht NUR: „Das gehört zu Quasselquest Pro.“ (ohne Zusatz zu den Eltern). Der Weg zum Freischalten liegt im Elternbereich hinter dem Tor.
- Kaufschutz wie geplant. AGB § 13 und Information der Plus-Käufer erledigen Leitstand/Marcel; Shop bleibt bis dahin privat.
- **Bestätigt von Marcel (ELTERN-TOR.md aktualisiert, gilt für alle Apps):** Schloss nur „Das gehört zu Quasselquest Pro.“; der Weg ins Tor über einen neutralen Knopf „Für Eltern“.

## Stand 24.11.2026 (Version 0.5.2, alles hinter dem Flag `PRO_AKTIV = false` in webapp/version.js)
- **Fertig:** `webapp/pro.js` – Ed25519-Prüfung mit BigInt (RFC-8032-Testvektoren + Gegenprobe mit Node-Krypto, 15 Prüfungen in `tools/test-pro.mjs`), Crockford-Base32, Sprach-Byte 1/2/3/4/5/9, Kaufschutz Wissensgarten Plus (App 3, Produkt 1/3 → Englisch + Deutsch), Rechte nur auf dem Gerät.
- **Fertig:** Elternbereich „⭐ Quasselquest Pro“ (Code einlösen, Link `#pro=` / `?pro=` wird gemerkt), Sperrliste über `familie-bruecke.js` (`sperrlisteLaden`, `codeGesperrt`), Schloss in der Kinderansicht (nur „Das gehört zu Quasselquest Pro.“), Browser-Test in `tools/web-auswertung.mjs`.
- **Frei bleibt:** Grundwortschatz (je Sprache das erste Paket und alle „Grundwortschatz“-Pakete), erste Episode jeder Welt, Lernpfad Abschnitt 1 vollständig, ab Abschnitt 2 je erste Einheit, alle Spielarten, Mein Schulbuch, Kopplung/Sicherung, Nachhilfe-Hinweis.
- **Offen vor dem Einschalten:** (1) Test-Codes vom Leitstand (nur der öffentliche Schlüssel liegt vor; die Tests nutzen eigene Schlüssel). (2) Patch für `codeNummer()` in familie.js (Mathe-Trainer): lehnt Sprach-Byte 5 und 9 (und App > 7) ab, deshalb kann die Sperrliste Spanisch- und Alle-Sprachen-Codes noch nicht prüfen. (3) Recht-Freigabe des Shops/der Sperre, Preise nur hinter dem Tor. (4) Dann Version 1.5.0 und `PRO_AKTIV = true`.
