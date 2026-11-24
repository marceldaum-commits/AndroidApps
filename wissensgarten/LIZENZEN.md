# Lizenzen fremder Bestandteile für Brummis Vorlese-Stimme

Seit der Umstellung auf die **KI-generierte Stimme vom eigenen Server** steckt **kein fremder Baustein** für die Stimme mehr in App oder WebApp
(die frühere Offline-Stimme samt ihrer Rechen-Bausteine und dem Lautschrift-Wörterbuch ist entfernt).

| Bestandteil | Wozu | Hinweis |
|---|---|---|
| KI-generierte Stimme (eigene Server-Stimmen: Deutsch Chatterbox „de-1“, Englisch/Französisch/Spanisch Kokoro-82M) | KI-generierte Vorlese-Stimme | **nicht** in App oder WebApp enthalten. Unser eigener Server stimmen.mathenachhilfe-online.com erzeugt die Audiodateien (MP3, ID3-Hinweis „KI-generiert“) und liefert sie aus. Es gibt keine Verbindung zu OpenAI und keinen Schlüssel. In den Einstellungen gekennzeichnet als „🤖 KI-generierte Stimme“. Lizenzliste der Modelle nach Recht V13: folgt (Wortlaut der AppZentrale). |
| Sprachausgabe des Geräts | stille Notlösung ohne Internet | Teil von Android bzw. des Browsers |

Der Hinweistext liegt bei: `content/src/main/assets/lizenzen.txt` (im APK als Asset, in der WebApp als `lizenzen.txt`).
