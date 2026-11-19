// Umzug auf die gemeinsame Adresse (Entscheidung Marcel, 19.11.2026; Standard UMZUG-LERNEN der AppZentrale)
//   alt:  https://experimentopia(-test).mathenachhilfe-online.com/
//   neu:  https://lernen(-test).mathenachhilfe-online.com/experimentopia/
// Der Lernstand liegt im Speicher des Browsers und gehört zur Adresse – darum zieht er einmal mit:
//   ALT: alles „experimentopia.*“ (+ alte Eltern-Kopplung „exEltern“) einsammeln, auf dem Gerät mit AES-256-GCM verschlüsseln,
//        als Einmal-Sicherung zum Server (…/eltern/v1/s/<id>; der Server sieht nur Zufallszeichen) und weiter zur neuen Adresse
//        mit „#umzug=<id>.<schlüssel>“. Der Schlüssel steht nur in der Adresse hinter „#“ – die geht nie an einen Server.
//   NEU: holen, entschlüsseln, nur übernehmen, wenn hier noch kein Lernstand ist, danach die Sicherung am Server löschen.
// Klappt etwas nicht (kein Netz, Server weg, neue Adresse noch nicht erreichbar), bleibt alles, wie es ist: Die App startet am alten Ort,
// beim nächsten Öffnen wird es noch einmal versucht. Nichts wird gelöscht.
(function (w) {
  'use strict';
  var P = 'experimentopia.';
  /** Diese Schlüssel sind Ladeprotokoll/Technik, kein Lernstand (zählen nicht für „hier ist schon etwas“ und ziehen nicht mit) */
  function technisch(k) { return k === P + 'schritte' || k.indexOf(P + 'modus') === 0 || k === P + 'umzug' || k === P + 'diag'; }
  var U = {
    ALT: /^experimentopia(-test)?\.mathenachhilfe-online\.com$/,
    /** Ziel zur alten Adresse (null = wir sind nicht am alten Ort) */
    ziel: function (host) { var m = U.ALT.exec(host); return m ? 'https://lernen' + (m[1] || '') + '.mathenachhilfe-online.com/experimentopia/' : null; },
    server: function (host) { return /-test|^localhost|^127\.|staging/.test(host) ? 'https://trainer-test.mathenachhilfe-online.com' : 'https://trainer.mathenachhilfe-online.com'; },
    b64u: function (bytes) { var s = ''; for (var i = 0; i < bytes.length; i++) s += String.fromCharCode(bytes[i]); return btoa(s).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, ''); },
    unb64u: function (t) { var s = atob(t.replace(/-/g, '+').replace(/_/g, '/') + '===='.slice(0, (4 - t.length % 4) % 4)); var b = new Uint8Array(s.length); for (var i = 0; i < s.length; i++) b[i] = s.charCodeAt(i); return b; },
    zufall: function (n) { var b = new Uint8Array(n); w.crypto.getRandomValues(b); return b; },
    /** Lernstand einsammeln: { schlüssel: wert } ohne Technik; alte Eltern-Kopplung „exEltern“ unter neuem Namen */
    einsammeln: function (ls) {
      var d = {}, n = 0;
      for (var i = 0; i < ls.length; i++) { var k = ls.key(i); if (k && k.indexOf(P) === 0 && !technisch(k)) { d[k] = ls.getItem(k); n++; } }
      var alt = ls.getItem('exEltern'); if (alt && !d[P + 'eltern']) { d[P + 'eltern'] = alt; n++; }
      return n ? d : null;
    },
    /** Ist hier schon ein Lernstand? */
    leer: function (ls) { for (var i = 0; i < ls.length; i++) { var k = ls.key(i); if (k && ((k.indexOf(P) === 0 && !technisch(k)) || k === 'exEltern')) return false; } return true; },
    schluessel: function (roh, zweck) { return w.crypto.subtle.importKey('raw', roh, { name: 'AES-GCM' }, false, [zweck]); },
    verschluesseln: function (text, keyB, iv) {
      return U.schluessel(keyB, 'encrypt').then(function (k) { return w.crypto.subtle.encrypt({ name: 'AES-GCM', iv: iv }, k, new TextEncoder().encode(text)); })
        .then(function (ct) { return U.b64u(iv) + '.' + U.b64u(new Uint8Array(ct)); });
    },
    entschluesseln: function (body, keyB) {
      var d = body; try { var j = JSON.parse(body); if (j && typeof j.d === 'string') d = j.d; } catch (e) {}
      var t = String(d).trim().split('.'); if (t.length !== 2) return Promise.reject(new Error('Format'));
      return U.schluessel(keyB, 'decrypt').then(function (k) { return w.crypto.subtle.decrypt({ name: 'AES-GCM', iv: U.unb64u(t[0]) }, k, U.unb64u(t[1])); })
        .then(function (pt) { return new TextDecoder().decode(pt); });
    },
    /** „#umzug=<id>.<key>&thema=…“ → { id, key, rest: „thema=…“ } */
    lesen: function (hash) {
      var h = String(hash || '').replace(/^#/, ''), teile = h.split('&'), r = null, rest = [];
      for (var i = 0; i < teile.length; i++) { var m = /^umzug=([A-Za-z0-9_-]{22})\.([A-Za-z0-9_-]{43})$/.exec(teile[i]); if (m) r = { id: m[1], key: m[2] }; else if (teile[i]) rest.push(teile[i]); }
      if (r) r.rest = rest.join('&');
      return r;
    },
    MAX: 500 * 1024,
    /** ALT: einsammeln, verschlüsseln, hochladen; liefert die neue Adresse (mit #umzug=… und den bisherigen Zusätzen) */
    weg: function (loc, ls, fetchFn) {
      var ziel = U.ziel(loc.hostname); if (!ziel) return Promise.resolve(null);
      var rest = String(loc.hash || '').replace(/^#/, ''), such = loc.search || '';
      var hin = function (marke) { return ziel + such + ((marke || rest) ? '#' + [marke, rest].filter(Boolean).join('&') : ''); };
      // Erreichbarkeit der neuen Adresse (ohne Lesen der Antwort): Steht sie noch nicht, bleiben wir hier
      return fetchFn(ziel + 'version.json?nc=' + Date.now(), { mode: 'no-cors', cache: 'no-store' }).then(function () {
        var daten = U.einsammeln(ls);
        if (!daten) return hin('');                      // nichts zu tragen
        var schon = ls.getItem(P + 'umzug');             // schon einmal hochgeladen → dieselbe Sicherung wiederverwenden
        if (schon && /^[A-Za-z0-9_-]{22}\.[A-Za-z0-9_-]{43}\.\d+$/.test(schon) && Date.now() - +schon.split('.')[2] < 6 * 86400000) return hin('umzug=' + schon.split('.').slice(0, 2).join('.'));
        var id = U.b64u(U.zufall(16)), schreib = U.b64u(U.zufall(16)), keyB = U.zufall(32);
        var text = JSON.stringify({ format: 1, app: 'experimentopia', umzug: 1, stand: new Date().toISOString(), schreib: schreib, daten: { speicher: daten } });
        if (text.length > U.MAX) return Promise.reject(new Error('zu groß: ' + text.length));
        return U.verschluesseln(text, keyB, U.zufall(12)).then(function (body) {
          return fetchFn(U.server(loc.hostname) + '/eltern/v1/s/' + id, { method: 'PUT', headers: { 'X-Schreib': schreib, 'Content-Type': 'text/plain' }, body: body, cache: 'no-store' });
        }).then(function (r) {
          if (!r.ok) throw new Error('Server ' + r.status);
          var marke = id + '.' + U.b64u(keyB);
          try { ls.setItem(P + 'umzug', marke + '.' + Date.now()); } catch (e) {}
          return hin('umzug=' + marke);
        });
      });
    },
    /** NEU: Sicherung holen und – nur wenn hier noch leer – übernehmen; danach am Server löschen. Ergebnis: 'übernommen' | 'schon da' | 'weg' | Fehler */
    her: function (u, ls, host, fetchFn) {
      var url = U.server(host) + '/eltern/v1/s/' + u.id;
      return fetchFn(url, { cache: 'no-store' }).then(function (r) {
        if (r.status === 404) return 'weg';
        if (!r.ok) throw new Error('Server ' + r.status);
        return r.text().then(function (t) { return U.entschluesseln(t, U.unb64u(u.key)); }).then(function (text) {
          var j = JSON.parse(text), sp = (j && j.daten && j.daten.speicher) || {}, ergebnis = 'schon da';
          if (j.app !== 'experimentopia') throw new Error('fremde Daten');
          if (U.leer(ls)) { for (var k in sp) if (k.indexOf(P) === 0 && !technisch(k) && typeof sp[k] === 'string') ls.setItem(k, sp[k]); ergebnis = 'übernommen'; }
          return fetchFn(url, { method: 'DELETE', headers: { 'X-Schreib': j.schreib }, cache: 'no-store' }).then(function () { return ergebnis; }, function () { return ergebnis; });
        });
      });
    }
  };
  w.__exUmzugApi = U;
  if (typeof location === 'undefined' || typeof localStorage === 'undefined') return;   // in Tests (Node): nur die Funktionen

  var ls; try { ls = w.localStorage; } catch (e) { return; }
  // Alte Namen ohne App-Vorsilbe einmalig übernehmen (gemeinsame Adresse: jede App nur mit ihrer Vorsilbe)
  try {
    var alt = ls.getItem('exEltern'); if (alt !== null) { if (ls.getItem(P + 'eltern') === null) ls.setItem(P + 'eltern', alt); ls.removeItem('exEltern'); }
    ls.removeItem('exSchritte');
    for (var i = ls.length - 1; i >= 0; i--) { var k = ls.key(i); if (k && /^exModus/.test(k)) ls.removeItem(k); }
  } catch (e) {}

  var setText = function (t) { var s = document.getElementById('start-text'); if (s) s.textContent = t; };
  if (U.ziel(location.hostname) && !/[?&]umzug=nein/.test(location.search)) {
    // ALT: App gar nicht erst starten, solange der Umzug läuft; klappt er nicht, normal weiter
    w.__exUmzugWarte = U.weg(location, ls, w.fetch.bind(w)).then(function (neu) {
      if (!neu) return;
      if (w.exStep) w.exStep('Umzug → ' + neu.split('#')[0]);
      setText('🚚 Experimentopia zieht um …');
      location.replace(neu);
      return new Promise(function () {});            // App hier nicht mehr starten
    }).catch(function (e) { if (w.exStep) w.exStep('Umzug später (' + (e && e.message || e) + ')'); });
    return;
  }
  var u = U.lesen(location.hash);
  if (u) {
    // NEU: Marke sofort aus der Adresse nehmen (Schlüssel nicht im Verlauf lassen), Rest (#thema=…, #pro=…) bleibt
    try { history.replaceState(null, '', location.pathname + location.search + (u.rest ? '#' + u.rest : '')); } catch (e) {}
    w.__exUmzugWarte = Promise.race([
      U.her(u, ls, location.hostname, w.fetch.bind(w)).then(function (r) { if (w.exStep) w.exStep('Umzug: ' + r); }),
      new Promise(function (ok) { setTimeout(ok, 10000); })
    ]).catch(function (e) { if (w.exStep) w.exStep('Umzug fehlgeschlagen (' + (e && e.message || e) + ')'); });
  }
})(typeof window !== 'undefined' ? window : globalThis);
