// Rechenblitz – Konfiguration
// reportEndpoint: URL, die Fehlermeldungen per POST (JSON) annimmt und als E-Mail weiterleitet.
// Solange sie leer ist, werden Meldungen auf dem Gerät gespeichert und später übertragen.
// Format der Meldungen: siehe MELDUNGEN-API.md im Repo-Stamm.
// '/api/meldung' ist relativ: läuft auf rechenblitz(-test).mathenachhilfe-online.com (Leitstand, Übergabe 27).
window.RECHENBLITZ_CONFIG = {
  reportEndpoint: '/api/meldung',
  // Rück-Link „← Lernraum“ oben links und im Menü; leer = ausgeblendet
  lernraumUrl: 'https://www.mathenachhilfe-online.com/lernraum/',
  // true: Links zur Website bekommen ?utm_source=rechenblitz… (nur sinnvoll mit Besucherstatistik)
  utm: false,
  // Öffentliche Adresse für QR-Codes, Flyer, Urkunden und Einladungen (immer die Live-Seite)
  publicUrl: 'https://rechenblitz.mathenachhilfe-online.com/',
  // Klassen-Challenge: Server-Adresse (z. B. '/api'), leer = Funktion ausgeblendet – siehe KLASSEN-API.md
  // Rechenblitz Pro (7,99 € einmalig): Kassen-URL des Woo-Produkts RB-PRO. Leer = keine Sperren, alles frei (pro.js, Meldung 54)
  proCheckout: '',
  classApi: '/api',
  // Erinnerung aufs Handy (Web-Push, Leitstand-Übergabe 38): Server-Pfad und vorgeschlagene Uhrzeit; endpoint '' = ausgeblendet
  push: { endpoint: '/api/push', time: '17:00' },
  // Nur in der Android-App (APK, solange nicht im Play Store): neue Versionen von den GitHub-Release-Seiten anbieten.
  // Für die Play-Store-Fassung auf null setzen (dort aktualisiert Google Play).
  apkUpdates: { repo: 'marceldaum-commits/School' },
  // natürliche Stimme „Thorsten“ (nur Deutsch, auf Wunsch): Rechenwerk + Wörterbuch aus /stimme/ dieser Seite, Modell vom gemeinsamen Stimmen-Server.
  // Ist das Modell nicht erreichbar, bietet die App Thorsten nicht an. sha256 = Prüfsumme der Modell-Datei auf dem Server.
  voice: { runtime: '/stimme/', model: 'https://stimmen.mathenachhilfe-online.com/thorsten/de_DE-thorsten-medium.onnx', sha256: '7e64762d8e5118bb578f2eea6207e1a35a8e0c30595010b666f983fc87bb7819', mb: 75 },
  // Adventskalender: Karte ab 15.11., Türchen 1.–24.12., nachholbar bis 6.1.
  // adventPreview: 'auto' = auf Testseiten alle Türchen offen; adventRaffleUrl = Seite zur Verlosung (leer = keine)
  advent: true,
  adventPreview: 'auto',
  adventRaffleUrl: '',
  adventRaffleMin: 12,
  // Gutschein für alle 24 Türchen: jeder Code wird vom Server frisch erzeugt und gilt nur einmal (Leitstand 13.11.):
  // POST /api/gutschein { klasse, tueren } → { code, prozent, bis, gilt }. 10 % auf jedes Stundenpaket, einmal je Familie,
  // bis 15.01.2027, nur bei zutreffender Klassenstufe. Leer lassen (endpoint: '') = Gutschein und Werbung dafür ausgeblendet.
  adventVoucher: { endpoint: '/api/gutschein', percent: 10, text: 'auf jedes Stundenpaket', until: '2027-01-15' }
};
