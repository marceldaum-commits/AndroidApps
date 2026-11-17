# Lizenzen fremder Bestandteile für Brummis Vorlese-Stimme

Seit der Umstellung auf **Nova** steckt **kein fremder Baustein** für die Stimme mehr in App oder WebApp
(die frühere Offline-Stimme samt ihrer Rechen-Bausteine und dem Lautschrift-Wörterbuch ist entfernt).

| Bestandteil | Wozu | Hinweis |
|---|---|---|
| Stimme „Nova“ (OpenAI) | KI-generierte Vorlese-Stimme | **nicht** in App oder WebApp enthalten. Unser Server stimmen.mathenachhilfe-online.com lässt die Texte von OpenAI vertonen und liefert die Audiodateien (MP3) aus. Die App selbst spricht nie mit OpenAI und hat keinen Schlüssel. In den Einstellungen gekennzeichnet als „Nova – KI-generierte Stimme“. |
| Sprachausgabe des Geräts | stille Notlösung ohne Internet | Teil von Android bzw. des Browsers |

Der Hinweistext liegt bei: `content/src/main/assets/lizenzen.txt` (im APK als Asset, in der WebApp als `lizenzen.txt`).
