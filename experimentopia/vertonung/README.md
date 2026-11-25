# Textliste für das Tonstudio (Stand 25.11.2026, App-Version 2.16.4 vom 24.11.2026)

- `texte.json` – Format 1 (`{"format":1,"app":"experimentopia","texte":[{"text","sprache":"de"}]}`), nur Deutsch, Stücke höchstens 300 Zeichen, ohne Wiederholungen: **2 998 Stücke, 268 940 Zeichen**. Tempo 1,0.
- `texte-langsam.json` – dieselben Stücke, die zusätzlich in Tempo 0,8 vertont werden sollen (nur Blubb-Sätze und Merksätze): **559 Stücke, 34 962 Zeichen**. Alle sind auch in `texte.json` enthalten.
- Erzeugen: `EXPORT_TONSTUDIO=$PWD/vertonung ./gradlew :core:model:jvmTest --tests '*TonstudioListeTest*'`
- Das Vorlesen in der App bleibt AUS (`Tonstudio.ENABLED=false`), bis die vertonten Dateien da sind und der Leitstand/Recht freigeben (KI-generierte Stimme wird gekennzeichnet, V36).
- Bei Textänderungen in der App muss die Liste neu erzeugt werden.
