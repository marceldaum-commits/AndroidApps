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
