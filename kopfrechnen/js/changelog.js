'use strict';
// Rechenblitz – Versionen mit Datum (neueste oben). Wird unter „Was ist neu?“ angezeigt.
const APP_VERSION = '3.16.0';
const APP_DATE = '2026-11-16';
const CHANGELOG = [
  ['3.16.0', '2026-11-16', ['Viel Neues für Klasse 5–10: Terme, Funktionen, Zehnerpotenzen, Dreisatz, Zinsen, Wahrscheinlichkeit, Statistik und Kreis', '15 neue Textaufgaben – von der Leiter am Haus bis zum Handytarif', 'Fehler behoben: Einige Prozent-Aufgaben hatten eine halbe Zahl als Lösung']],
  ['3.15.0', '2026-11-16', ['Viele neue Aufgaben: 5 neue Themen – Verdoppeln, Geld, Uhr & Zeit, Geometrie und Zahlenfolgen', '30 neue Textaufgaben – von Vögeln im Baum bis zur Wanderkarte', 'Geldbeträge mit zwei Nachkommastellen, z. B. 4,80 €']],
  ['3.14.5', '2026-11-16', ['Android-App: Links zur Website nur nach kurzer Elternabfrage']],
  ['3.14.4', '2026-11-16', ['Vorbereitung für Google Play: Updates direkt in der App']],
  ['3.14.3', '2026-11-16', ['Vorbereitung für Google Play']],
  ['3.14.2', '2026-11-16', ['Urkunden drucken: neu „A4 quer – ganze Seite“']],
  ['3.14.1', '2026-11-16', ['Profile lassen sich jetzt einzeln löschen – mit 🗑️ neben jedem Namen']],
  ['3.14.0', '2026-11-16', ['Neu: dein Rechengarten 🌻 – jede richtige Antwort ist ein Wassertropfen, deine Pflanzen wachsen', 'Ernten, neue Samen und Beete freischalten – von der Karotte bis zum Apfelbaum', 'Gemütlich: Nichts welkt, Pausen sind okay. Oben zählen jetzt die Tage diese Woche']],
  ['3.13.4', '2026-11-15', ['Aufgeräumter Kopfbereich: das Logo führt zur Website']],
  ['3.13.3', '2026-11-15', ['Android-App: neue Versionen spielen sich von selbst ein – ganz ohne Knopf, nie mitten in einer Runde']],
  ['3.13.2', '2026-11-15', ['Rechenblitz im Browser aktualisiert sich jetzt von selbst – nie mitten in einer Runde']],
  ['3.13.1', '2026-11-15', ['Android-App: In den Einstellungen steht jetzt, woher die laufende Version geladen wurde']],
  ['3.13.0', '2026-11-15', ['Android-App: neue Versionen kommen jetzt von selbst – ohne Neuinstallation, der Lernstand bleibt', 'Ohne Internet läuft immer der zuletzt geladene Stand']],
  ['3.12.1', '2026-11-15', ['Abbrechen führt zurück zur Reise-Karte, zum Adventskalender oder zum Merk-Training', 'Zurück-Taste am Handy schließt Fenster und beendet Runden, statt die App zu verlassen', 'Tageschallenge: ein abgebrochener Start zählt nicht mehr als Versuch', 'Viele kleine Verbesserungen: Funken-Anzeige, Rätsel, Garderobe, Zurücksetzen']],
  ['3.12.0', '2026-11-15', ['Neu: Blitz-Welt 💫 – sammle Funken und kauf damit Outfits für Blitz und Deko für sein Zimmer', 'Rätsel des Tages: finde die geheime Blitz-Zahl in 6 Versuchen – mit Mathe-Hinweisen', 'Jahreszeiten-Events mit Sonder-Outfits – jetzt: Pilzhut zur Herbstzeit']],
  ['3.11.1', '2026-11-15', ['Neu: einzelne Bereiche zurücksetzen – Rechen-Reise, Merk-Training, Abzeichen oder Fehlerkiste']],
  ['3.11.0', '2026-11-15', ['Android-App: Rechenblitz-Widget für den Startbildschirm – Merk-Training, Tagesziel und Serie auf einen Blick']],
  ['3.10.1', '2026-11-15', ['Neu im Menü: „Nach Updates suchen“ – holt die neueste Version mit einem Tipp']],
  ['3.10.0', '2026-11-15', ['Rechen-Reise komplett: Wolkenstadt, Feuerberg und Sternenschloss', 'Großes Finale – Blitz leuchtet wieder golden', 'Sticker-Album, große Urkunde und neue Abzeichen']],
  ['3.9.0', '2026-11-14', ['Neu: Rechen-Reise 🗺️ – hilf Blitz, seine Funken einzusammeln: Zahlenwald, Rechenfluss und Funkenhöhle', 'Sterne, Sticker und Urkunden für jedes Kapitel – passend zu deiner Klasse', 'Abgebrochene Runden zählen jetzt trotzdem fürs Tagesziel', 'Viele Fehler behoben: sicherere Links, kein doppelter Countdown, Blitzrunde pausiert im Hintergrund']],
  ['3.8.0', '2026-11-14', ['Neu: Blitz der Fuchs 🦊 – feuert dich an, tröstet und erklärt Tricks', 'Merksätze zu schweren Aufgaben – zum Beispiel 5, 6, 7, 8 → 56 = 7 · 8', 'Sternbilder mit Namen zum Sammeln – von der Siebener-Schlange bis zum Achter-Drachen', 'Nach jedem Update zeigt dir Blitz, was neu ist']],
  ['3.7.2', '2026-11-14', ['Android-App: meldet neue Versionen selbst und aktualisiert sich mit einem Tipp']],
  ['3.7.1', '2026-11-14', ['Neues Symbol für das Merk-Training auf der Startseite: Mond und Sternbild']],
  ['3.7.0', '2026-11-14', ['Merk-Training zum Erleben: jede Aufgabe mit bewegtem Bild – Punktefeld, Zwanzigerfeld, Quadrat in Teilen, Pizza und Hunderterfeld', 'Neue Aufgaben werden erst gezeigt, dann gefragt', 'Dein Merk-Himmel: jede gemerkte Aufgabe ist ein Stern, ganze Reihen werden zu Sternbildern', '💡 Bild-Tipp und Sterne, die davonfliegen']],
  ['3.6.1', '2026-11-14', ['Neu: Klassen-Challenge 🏫 – mit der ganzen Klasse Punkte sammeln, wöchentliche Rangliste']],
  ['3.6.0', '2026-11-14', ['Neu: Merk-Training 🧠 – was wackelt, kommt öfter dran, was sitzt, seltener', 'Jeden Tag „Heute wiederholen“ in 3 Minuten', 'Passend zur Klasse: Plus bis 20, Einmaleins, Quadratzahlen, Brüche als Dezimalzahl und Prozent', 'Neues Abzeichen: Gedächtnis-Profi']],
  ['3.5.2', '2026-11-14', ['Der Adventsgutschein gilt jetzt für jedes Stundenpaket', 'Rechenblitz gibt es bald auch als Android-App – komplett ohne Internet spielbar']],
  ['3.5.1', '2026-11-13', ['Neu in den Einstellungen: Erinnerung aufs Handy – einmal am Tag zu deiner Uhrzeit']],
  ['3.5.0', '2026-11-13', ['Versionsnummer mit Datum', 'Neu im Menü: „Was ist neu?“ mit allen Änderungen']],
  ['3.4.2', '2026-11-13', ['Adventskalender: persönlicher Gutscheincode für alle 24 Türchen']],
  ['3.4.0', '2026-11-13', ['Einfachere Startseite mit einem großen Start-Knopf', 'Schwierigkeit stellt sich automatisch ein']],
  ['3.3.0', '2026-11-13', ['Englisch und Ukrainisch – passend zur Sprache des Geräts', 'Logo führt zur Website']],
  ['3.2.0', '2026-11-13', ['Adventskalender passend zur Klassenstufe']],
  ['3.1.0', '2026-11-13', ['Rechen-Adventskalender', 'Urkunden zum Teilen und Drucken', 'Flyer & Poster mit QR-Code', 'Freunde einladen']],
  ['3.0.0', '2026-11-13', ['17 Themen mit Lösungsweg bei jedem Fehler', 'Profile, Abzeichen, Duell, Tageschallenge', 'Einstufungstest und Wochenbericht']],
];
const fmtDateShort = s => new Date(s + 'T12:00').toLocaleDateString(typeof locale === 'function' ? locale() : 'de-DE', { day: '2-digit', month: '2-digit', year: 'numeric' });
const versionLabel = () => `${APP_VERSION} · ${fmtDateShort(APP_DATE)}`;
