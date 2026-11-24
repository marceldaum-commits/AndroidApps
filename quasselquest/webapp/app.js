// Englisch mit Ruby – Prototyp (Welt 1). Schlichtes JavaScript ohne Bibliotheken, läuft in Safari/WebKit, Chrome, Firefox.
(function () {
  'use strict';
  const C = window.CONTENT;
  const A = window.AUDIO;
  const $app = document.getElementById('app');
  const DIAG = /[?&]diag=1/.test(location.search);

  // ================================================================== Speicher (nur im Browser)
  const KEY = 'quasselquest.v1';
  let mem = null;
  function load() {
    try { const s = localStorage.getItem(KEY) || localStorage.getItem('englisch.v1'); if (s) return JSON.parse(s); } catch (e) {} // alter Prototyp-Schlüssel
    return mem || null;
  }
  function save() { mem = state; try { localStorage.setItem(KEY, JSON.stringify(state)); } catch (e) {} if (typeof famPlan === 'function') famPlan(false); }
  const fresh = () => ({ v: 3, klasse: null, name: '', lang: 'en', karten: {}, introSeen: {}, eigene: {}, tage: {}, hinweis: {}, tests: {}, lp: {}, insel: {}, progress: {}, settings: { sound: true, voice: true, vibrate: true, slow: false, recognition: 'device', stimme: 'frei', zaehlung: true, nachhilfe: true }, week: { key: '', n: 0 } });
  let state = load() || fresh();
  state.settings = Object.assign(fresh().settings, state.settings || {});
  // Stand 1 → 2: Spracherkennung standardmäßig nur auf dem Gerät; Wochenziel statt Tagesserie
  if ((state.v || 1) < 2) {
    if (state.settings.recognition === 'browser') state.settings.recognition = 'device';
    delete state.streak; state.week = { key: '', n: 0 }; state.v = 2;
  }
  if (!state.week) state.week = { key: '', n: 0 };
  // Stand 2 → 3 (19.11.2026): Quasselquest wird die Sprachen-App – Sprache wählbar, Wortschatz-Pakete mit Wiederholung
  if (state.v < 3) { state.lang = 'en'; state.karten = {}; state.introSeen = {}; state.v = 3; }
  if (!state.eigene) state.eigene = {};
  if (!state.tage) state.tage = {};
  if (!state.hinweis) state.hinweis = {};
  if (!state.tests) state.tests = {};
  if (!state.lp) state.lp = {};
  if (!state.insel) state.insel = {};
  const PK = window.PAKETE;
  window.APP_SETTINGS = state.settings;

  // ================================================================== Hilfen
  const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const h = (html) => { const t = document.createElement('template'); t.innerHTML = html.trim(); return t.content.firstElementChild; };
  const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
  const shuffle = (a) => { a = a.slice(); for (let i = a.length - 1; i > 0; i--) { const j = Math.floor(Math.random() * (i + 1)); [a[i], a[j]] = [a[j], a[i]]; } return a; };
  const pickOthers = (all, not, n) => shuffle(all.filter((x) => x !== not)).slice(0, n);
  let token = 0; // wechselt bei jedem Bildschirmwechsel – laufende Abläufe brechen dann ab
  function screen(html) { token++; A.stop(); A.verwerfen(); $app.innerHTML = ''; const el = typeof html === 'string' ? h(html) : html; $app.appendChild(el); window.scrollTo(0, 0); if (window.splashEnde) { const e = window.splashEnde; window.splashEnde = null; requestAnimationFrame(() => e()); } return el; }
  const alive = (t) => t === token;
  /**
   * Weg 2 (Server-Auswertung, nur wenn die Eltern es gewählt haben und das Feature-Flag an ist; sonst null und KEIN Aufruf).
   * Zeigt „Ruby hört genau hin“ und liefert { status:'ok', sterne, text } oder { status:'nicht_moeglich' } – Aufnahme (Blob) wird danach verworfen.
   */
  async function serverAuswerten(lang, ziel, res, hinweisEl) {
    if (!window.AUSWERTUNG || !window.AUSWERTUNG.serverFuer(lang) || !res || !res.blob) return null;
    if (hinweisEl) hinweisEl.textContent = '⏳ Ruby hört genau hin …';
    const r = await window.AUSWERTUNG.pruefen(lang, ziel, res.blob); res.blob = null;
    return r.status === 'nicht_gefragt' ? null : r;
  }
  const NICHT_MOEGLICH = 'Die Aussprache-Auswertung ist auf diesem Gerät gerade nicht möglich.';
  // Test-Stufe am Hostnamen (Standard UMZUG-LERNEN): „lernen-test.“, Übergang „quasselquest-test.“, lokal.
  const isTest = () => /(^|[.-])(test|staging)([.-]|$)|localhost|127\.0\.0\.1/.test(location.hostname) || location.protocol === 'file:';

  // ================================================================== Anonyme Zählung (gemeinsames Format, RATGEBER Teil 5) – abschaltbar
  // Nur app, v, ev, ort, thema. Keine Kennung, keine Cookies, kein Lernstand. Aus = kein Aufruf.
  const ZAEHL_EV = ['start', 'elternbereich', 'pro_ansehen', 'pro_kasse', 'code_eingeloest', 'probestunde_klick', 'neukunde_klick', 'nachhilfe_hinweis'];
  function zaehlen(ev, ort, thema) {
    if (state.settings.zaehlung === false || !ZAEHL_EV.includes(ev) || location.protocol === 'file:' || (!DIAG && /127\.0\.0\.1|localhost/.test(location.hostname))) return;
    const body = { app: 'quasselquest', v: window.APP_VERSION, ev };
    if (ort) body.ort = ort; if (thema) body.thema = thema;
    try {
      fetch(`https://${isTest() ? 'trainer-test' : 'trainer'}.mathenachhilfe-online.com/zaehler/v1/e`,
        { method: 'POST', body: JSON.stringify(body), keepalive: true, credentials: 'omit', mode: 'no-cors', headers: { 'Content-Type': 'text/plain' } }).catch(() => {});
    } catch (e) {}
  }
  if (DIAG) window.__zaehlTest = (ev) => ZAEHL_EV.includes(ev);

  // Antworten je Sprache und Tag (für „Thema hakt“ im Elternbereich) – nur auf dem Gerät
  function tally(lang, ok) {
    const d = window.PAKETE.today(); const t = state.tage[lang] = state.tage[lang] || {};
    const e = t[d] = t[d] || [0, 0]; e[0]++; if (ok) e[1]++;
    for (const k of Object.keys(t)) if (+k < d - 60) delete t[k];
  }
  function stats(lang, days) {
    const d = window.PAKETE.today(); let n = 0; let ok = 0;
    for (const [k, v] of Object.entries(state.tage[lang] || {})) if (+k > d - days) { n += v[0]; ok += v[1]; }
    return { n, ok, pct: n ? Math.round(100 * ok / n) : null };
  }

  // ================================================================== Ruby (SVG)
  function rubySVG() {
    return `<svg viewBox="0 0 100 100" aria-hidden="true"><g class="body">
      <path d="M30 70 L18 98 L30 92 L34 99 L40 74 Z" fill="#1e88e5"/><path d="M34 72 L30 99 L40 94 L44 74 Z" fill="#fdd835"/>
      <ellipse cx="50" cy="56" rx="22" ry="28" fill="#e53935"/>
      <ellipse cx="54" cy="62" rx="12" ry="16" fill="#ff7043" opacity=".55"/>
      <g class="wing"><path d="M32 46 C18 54 20 76 36 82 C44 70 44 54 32 46 Z" fill="#c62828"/><path d="M26 66 C28 74 32 78 36 82 C33 74 30 70 26 66 Z" fill="#1e88e5"/><path d="M28 58 C30 66 33 70 37 74 C35 66 32 62 28 58 Z" fill="#fdd835"/></g>
      <circle cx="58" cy="28" r="18" fill="#e53935"/>
      <circle cx="63" cy="27" r="7.5" fill="#fff"/><circle cx="65" cy="27" r="4" fill="#1d2b3a"/><circle cx="66.5" cy="25.5" r="1.4" fill="#fff"/>
      <rect class="eyelid" x="55" y="19" width="16" height="16" rx="8" fill="#e53935"/>
      <path d="M72 24 C86 22 90 34 82 42 C80 36 76 33 72 33 Z" fill="#fdd835"/>
      <path class="beak-low" d="M73 36 C78 37 81 40 80 44 C76 43 73 40 73 36 Z" fill="#f9a825"/>
      <path d="M40 84 L38 92 M48 85 L48 93 M44 92 L52 92 M34 92 L42 92" stroke="#6d4c41" stroke-width="3" stroke-linecap="round"/>
    </g></svg>`;
  }
  const ruby = (text, cls) => `<div class="ruby-wrap"><div class="ruby ${cls || ''}" data-ruby>${rubySVG()}</div><div class="bubble" data-rubytext>${text || ''}</div></div>`;
  function rubySay(root, text, mood) {
    const b = root.querySelector('[data-rubytext]'); const r = root.querySelector('[data-ruby]');
    if (b) { b.innerHTML = text; b.style.animation = 'none'; void b.offsetWidth; b.style.animation = ''; }
    if (r) { r.classList.remove('dance', 'talk'); if (mood) { void r.offsetWidth; r.classList.add(mood); setTimeout(() => r.classList.remove(mood), 1600); } }
  }

  // ================================================================== Konfetti
  const cv = document.getElementById('confetti'); const cx = cv.getContext && cv.getContext('2d');
  let parts = []; let craf = 0;
  const REDUCED = !!(window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches);
  function confetti(n) {
    if (!cx || REDUCED) return;
    const dpr = window.devicePixelRatio || 1; cv.width = innerWidth * dpr; cv.height = innerHeight * dpr; cx.setTransform(dpr, 0, 0, dpr, 0, 0);
    const cols = ['#e53935', '#1e88e5', '#fdd835', '#43a047', '#fb8c00', '#8e24aa'];
    for (let i = 0; i < (n || 80); i++) parts.push({ x: innerWidth / 2 + (Math.random() - .5) * 120, y: innerHeight * .35, vx: (Math.random() - .5) * 12, vy: -Math.random() * 12 - 4, s: 6 + Math.random() * 6, c: cols[i % cols.length], r: Math.random() * 6, vr: (Math.random() - .5) * .4, life: 120 + Math.random() * 60 });
    if (!craf) craf = requestAnimationFrame(tick);
  }
  function tick() {
    cx.clearRect(0, 0, innerWidth, innerHeight);
    parts = parts.filter((p) => p.life-- > 0 && p.y < innerHeight + 20);
    for (const p of parts) { p.vy += .35; p.vx *= .99; p.x += p.vx; p.y += p.vy; p.r += p.vr; cx.save(); cx.translate(p.x, p.y); cx.rotate(p.r); cx.fillStyle = p.c; cx.fillRect(-p.s / 2, -p.s / 3, p.s, p.s * .66); cx.restore(); }
    craf = parts.length ? requestAnimationFrame(tick) : 0;
    if (!craf) cx.clearRect(0, 0, innerWidth, innerHeight);
  }

  // ================================================================== Stimme mit Wort-Hervorhebung
  function charVoice(who) { return (C.characters[who] || C.characters.ruby).voice; }
  async function speak(text, who, opts) {
    return A.say(text, Object.assign({}, charVoice(who), { who: who || 'ruby' }, opts || {}));
  }
  /** Ohne Stimme (z. B. Deutsch vorübergehend): Wort kurz zeigen, dann verdecken – aus dem Kopf bauen/schreiben. */
  const merkBox = (wort) => `<div class="novoice merk" data-merk data-wort="${esc(wort)}">${esc(wort)}<small>Merk dir das Wort!</small></div>`;
  function merkStart(m) {
    if (m.dataset.zu) return; m.dataset.zu = '1'; const wort = m.dataset.wort;
    const zeigen = () => { m.innerHTML = `${esc(wort)}<small>Merk dir das Wort!</small>`; setTimeout(verdecken, 3000); };
    const verdecken = () => { if (!m.isConnected) return; m.innerHTML = '<button class="btn white small" data-merk-zeig>👀 Noch mal zeigen</button>'; m.querySelector('[data-merk-zeig]').onclick = () => { A.sfx('tap'); zeigen(); }; };
    setTimeout(verdecken, 3000);
  }
  new MutationObserver(() => document.querySelectorAll('[data-merk]:not([data-zu])').forEach(merkStart)).observe(document.body, { childList: true, subtree: true });
  const noVoiceBox = (text, who) => A.canHear(text, who || 'ruby') ? '' :
    `<div class="novoice" data-novoice>${esc(text)}<small>${state.settings.voice === false ? 'Die Stimme ist ausgeschaltet – lies den Satz.' : 'Auf diesem Gerät gibt es keine englische Stimme – lies den Satz.'}</small></div>`;

  // ================================================================== Kopfzeile
  function allAppsUrl() {
    const host = isTest() ? 'staging' : 'www';
    return `https://${host}.mathenachhilfe-online.com/lern-apps/?src=${encodeURIComponent(window.APP_ID)}-kopf`;
  }
  function openAllApps() {
    const url = allAppsUrl();
    const standalone = (window.matchMedia && matchMedia('(display-mode: standalone)').matches) || navigator.standalone === true;
    if (standalone) window.open(url, '_blank', 'noopener'); else location.href = url;
  }

  // ================================================================== Fortschritt
  const prog = (id) => state.progress[id] || null;
  function stampCount() { return Object.values(state.progress).filter((p) => p && p.done).length; }
  function nextEpisode() {
    for (const w of C.worlds) for (const e of w.episodes || []) if (!e.soon && !(prog(e.id) && prog(e.id).done)) return e.id;
    return null;
  }
  // Wochenziel statt Tagesserie: nichts geht verloren, jede Woche beginnt neu und freundlich.
  const WEEK_GOAL = 3;
  function weekKey(d) {
    d = new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()));
    const day = d.getUTCDay() || 7; d.setUTCDate(d.getUTCDate() + 4 - day);
    const y0 = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
    return d.getUTCFullYear() + '-W' + Math.ceil(((d - y0) / 864e5 + 1) / 7);
  }
  function weekCount() { return state.week.key === weekKey(new Date()) ? state.week.n : 0; }
  function noteEpisodeDone() {
    const k = weekKey(new Date());
    if (state.week.key !== k) state.week = { key: k, n: 0 };
    state.week.n++;
    return state.week.n === WEEK_GOAL;
  }

  // ================================================================== Start
  function start() {
    zaehlen('start');
    document.addEventListener('pointerdown', A.unlock, { once: true });
    if (!state.klasse) return onboarding();
    const m = /episode=([a-z0-9]+)/.exec(location.hash);
    if (m && C.findEpisode(m[1]) && !C.findEpisode(m[1]).episode.soon) { history.replaceState(null, '', location.pathname + location.search); return playEpisode(m[1]); }
    const sp = /sprache=([a-z]{2})/.exec(location.hash); const pk = /paket=([a-z0-9-]+)/.exec(location.hash);
    if (sp && PK.LANGS[sp[1]]) { state.lang = sp[1]; save(); }
    if (/welt=|sprache=|paket=/.test(location.hash)) history.replaceState(null, '', location.pathname + location.search);
    if (pk) { const meta = packIndex().find((x) => x.id === pk[1]); if (meta) { state.lang = meta.sprache; save(); return openPack(meta); } }
    home();
  }

  const KLASSEN = [3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13];
  function onboarding() {
    let klasse = null;
    const el = screen(`<div class="stage onb">
      ${ruby('Hello! Ich bin <b>Ruby</b>, ein Papagei, der viele Sprachen spricht. Papageien sprechen alles nach – und du bald auch! 🎉', 'talk')}
      <div class="card"><h2 style="font-size:20px">In welche Klasse gehst du?</h2><p class="muted" style="margin:6px 0 12px">Du kannst das später ändern.</p>
        <div class="grade">${KLASSEN.map((k) => `<button data-k="${k}" aria-label="Klasse ${k}">${k}</button>`).join('')}</div></div>
      <div class="card"><h2 style="font-size:20px">Wie heißt du? <small class="muted">(freiwillig)</small></h2>
        <p class="muted" style="margin:6px 0 10px">Ruby und die Kinder am Hafen sagen dann deinen Namen. Er bleibt nur auf diesem Gerät.</p>
        <input class="input" data-name maxlength="15" autocomplete="off" placeholder="Vorname oder Spitzname"><div class="err" data-err></div></div>
      <button class="btn blue" data-go disabled>Los geht's! ⚓</button>
    </div>`);
    el.querySelectorAll('[data-k]').forEach((b) => b.onclick = () => {
      A.sfx('tap'); klasse = +b.dataset.k; el.querySelectorAll('[data-k]').forEach((x) => x.classList.toggle('sel', x === b)); el.querySelector('[data-go]').disabled = false;
    });
    el.querySelector('[data-go]').onclick = () => {
      const raw = el.querySelector('[data-name]').value.trim();
      const err = raw ? C.checkName(raw) : null;
      if (err) { el.querySelector('[data-err]').textContent = err; A.sfx('soft'); return; }
      state.klasse = klasse; state.name = raw; save(); A.sfx('squawk'); home();
    };
  }

  // ================================================================== Startseite
  let leisteX = 0;   // Scroll-Position der Sprachleiste über Neuzeichnen hinweg
  function langBar() {
    return `<div class="langbar" role="tablist" aria-label="Sprache wählen">${PK.ORDER.map((l) => `<button role="tab" class="lang ${state.lang === l ? 'sel' : ''}" data-lang="${l}" aria-selected="${state.lang === l}"><span>${PK.LANGS[l].emoji}</span>${esc(PK.LANGS[l].name)}</button>`).join('')}</div>`;
  }
  function packTiles() {
    const k = state.klasse || 5;
    const list = packIndex().filter((p) => p.sprache === state.lang)
      .sort((a, b) => Math.abs(Math.min(...a.grades) - k) - Math.abs(Math.min(...b.grades) - k));
    const eigene = (state.eigene[state.lang] || []);
    const mein = `<button class="wtile ptile mein" data-mine><span class="e">📒</span><b>Mein Schulbuch</b><small>${eigene.length ? eigene.length + (eigene.length === 1 ? ' eigene Liste' : ' eigene Listen') : 'Deine Wörter aus der Schule eintippen'}</small></button>`;
    if (!list.length) return `<div class="worlds">${mein}</div>`;
    return `<div class="worlds">${mein}${list.map((p) => {
      const sitzt = packMastered(p.id); const passt = p.grades.includes(k);
      return `<button class="wtile ptile" data-pack="${p.id}">${passt ? '<span class="soon fit">passt zu Kl. ' + k + '</span>' : ''}<span class="e">${p.emoji}</span><b>${esc(p.title)}</b>
        <small>Kl. ${esc(gradeText(p.grades))} · ${p.karten} Karten</small><span class="bar"><i style="width:${Math.round(100 * sitzt / p.karten)}%"></i></span><small>${sitzt} sitzen schon</small></button>`;
    }).join('')}</div>`;
  }
  const gradeText = (g) => { const x = g.filter((n) => n <= 13); return x.length ? (x.length > 1 ? Math.min(...x) + '–' + Math.max(...x) : String(x[0])) : 'alle'; };

  function home() {
    const L = PK.LANGS[state.lang];
    const en = state.lang === 'en';
    const w1 = C.worlds[0];
    const next = nextEpisode();
    const greet = (state.name ? `${esc(L.hallo.replace(/[!¡ ]+$/, ''))}, <b>${esc(state.name)}</b>! ` : esc(L.hallo) + ' ');
    const tip = en ? (next ? (prog('w1e1') ? 'Weiter geht\'s! Tipp auf den hüpfenden Kreis.' : 'Komm mit zum Hafen! Tipp auf den hüpfenden Kreis.') : 'Du hast alle Orte am Hafen geschafft! Bald geht die Reise weiter. 🚢')
      : `Heute üben wir <b>${esc(L.name)}</b>. Such dir ein Paket aus – ich spiele mit dir!${L.who ? '' : ' (Latein lesen wir – vorlesen kann ich es noch nicht.)'}`;
    const el = screen(`<div class="home">
      <div class="top"><button class="allapps" data-allapps>← Alle Apps</button><div class="spacer"></div>
        <span class="pill" title="Stempel im Reisepass">📘 ${stampCount()}</span>
        <button class="iconbtn" data-settings aria-label="Einstellungen">⚙️</button></div>
      <div class="title">Quasselquest <span style="font-size:.6em;color:var(--muted)">Sprachen mit Ruby</span></div>
      ${langBar()}
      ${ruby(greet + tip)}
      ${testAktiv() ? testCard() : ''}
      ${en ? '' : '<div data-lp class="lp"></div>'}
      ${en ? `<div class="world">
        <div class="waves"></div><div class="boatdeco">⛵</div>
        <h2>${w1.emoji} ${esc(w1.title)}</h2><div class="sub">Welt 1 · ${esc(w1.de)}</div>
        <div class="stations">${w1.episodes.map((e) => {
          const p = prog(e.id); const cls = e.soon ? 'soon' : (p && p.done ? 'done' : (e.id === next ? 'next' : ''));
          return `<button class="station ${cls}" data-ep="${e.id}" aria-label="${esc(e.title)}"><span class="dot">${e.soon ? '🔜' : e.emoji}</span>
            <span class="lbl">${esc(e.title)}</span><span class="stars">${p && p.done ? '⭐'.repeat(p.stars) : ''}</span></button>`;
        }).join('')}</div>
      </div><div data-lp class="lp"></div>` : ''}
      <div class="row" style="margin-top:14px">${en ? '<button class="btn white small" data-pass>📘 Mein Reisepass</button>' : ''}
        <span class="pill" title="Wochenziel">🎯 Diese Woche: ${Math.min(weekCount(), WEEK_GOAL)}/${WEEK_GOAL}${weekCount() >= WEEK_GOAL ? ' ✅' : ''}</span></div>
      ${testAktiv() ? '' : testCard()}
      <div class="section-h">${L.emoji} Wörter & Regeln üben</div>
      ${packTiles()}
      ${en ? `<div class="section-h">Weitere Orte auf der Reise</div>
      <div class="worlds">${C.worlds.slice(1).map((w) => `<button class="wtile" data-world="${w.id}"><span class="soon">kommt bald</span><span class="e">${w.emoji}</span><b>${esc(w.title)}</b><small>${esc(w.de)} · ${w.stufe === 'gs' ? 'Kl. 3–4' : 'Kl. 5–6'}</small></button>`).join('')}</div>` : ''}
      <div class="foot">Version ${esc(window.APP_VERSION)} vom ${esc(window.APP_VERSION_DATE)} · <a href="datenschutz.html">Datenschutz</a> · <a href="lizenzen.txt">KI-Stimmen &amp; Lizenzen</a> · <button class="linkbtn" data-kids-home>🔒 Deine Daten</button></div>
    </div>`);
    el.querySelector('[data-kids-home]').onclick = () => { A.sfx('tap'); kidsPrivacy(home); };
    el.querySelector('[data-allapps]').onclick = () => { A.sfx('tap'); openAllApps(); }; // Marcel 19.11.2026 (V10 geändert): Website-Link ohne Eltern-Tor, neutral
    el.querySelector('[data-settings]').onclick = () => { A.sfx('tap'); elternTor(settings); };
    const ps = el.querySelector('[data-pass]'); if (ps) ps.onclick = () => { A.sfx('tap'); passport(); };
    el.querySelectorAll('[data-lang]').forEach((b) => b.onclick = () => {
      if (state.lang === b.dataset.lang) return;
      state.lang = b.dataset.lang; save(); A.sfx('pop'); A.vibrate(15); home();
      const L2 = PK.LANGS[state.lang]; if (L2.who) speak(L2.hallo.replace(/ !$/, '!'), L2.who);
    });
    el.querySelectorAll('[data-pack]').forEach((b) => b.onclick = () => { A.sfx('pop'); A.vibrate(20); openPack(packIndex().find((x) => x.id === b.dataset.pack)); });
    const mine = el.querySelector('[data-mine]'); if (mine) mine.onclick = () => { A.sfx('pop'); myLists(); };
    const tn = el.querySelector('[data-test-new]'); if (tn) tn.onclick = () => { A.sfx('pop'); testSetup(); };
    const tu = el.querySelector('[data-test-ueben]'); if (tu) tu.onclick = async () => { A.sfx('pop'); const p = await packById(tu.dataset.testUeben); if (p) packRound(p); };
    const tp = el.querySelector('[data-test-probe]'); if (tp) tp.onclick = () => { A.sfx('pop'); testProbe(); };
    const te = el.querySelector('[data-test-edit]'); if (te) te.onclick = () => { A.sfx('tap'); testSetup(); };
    el.querySelectorAll('[data-ep]').forEach((b) => b.onclick = () => {
      const f = C.findEpisode(b.dataset.ep);
      if (f.episode.soon) { A.sfx('soft'); rubySay(el, `„${esc(f.episode.title)}“ baue ich gerade. Bald kannst du hier spielen! 🔨`, 'talk'); return; }
      A.sfx('pop'); A.vibrate(20); playEpisode(b.dataset.ep);
    });
    el.querySelectorAll('[data-world]').forEach((b) => b.onclick = () => {
      A.sfx('soft'); b.classList.remove('wiggle'); void b.offsetWidth; b.classList.add('wiggle');
      const w = C.worlds.find((x) => x.id === +b.dataset.world);
      rubySay(el, `Nach <b>${esc(w.title)}</b> fliegen wir bald! Dort geht es um: ${esc(w.de)}.`, 'talk');
    });
    // Sprachleiste: Position halten und die gewählte Sprache sichtbar in die Mitte rücken (sprang sonst an den Anfang)
    const bar = el.querySelector('.langbar'); const gew = bar && bar.querySelector('.lang.sel');
    if (bar) { bar.scrollLeft = leisteX; if (gew) { const ziel = gew.offsetLeft - (bar.clientWidth - gew.offsetWidth) / 2;
      requestAnimationFrame(() => { try { bar.scrollTo({ left: Math.max(0, ziel), behavior: leisteX ? 'smooth' : 'auto' }); } catch (e) { bar.scrollLeft = Math.max(0, ziel); } }); }
      bar.addEventListener('scroll', () => { leisteX = bar.scrollLeft; }, { passive: true }); }
    lernpfad(state.lang).then((lp) => { const box = el.querySelector('[data-lp]'); if (lp && box && box.isConnected) lpPfadZeigen(el, box, lp); });
    maybeApplyUpdate();
  }

  // ================================================================== Episode
  async function playEpisode(id) {
    const { episode: ep } = C.findEpisode(id);
    const steps = [{ kind: 'scene' }].concat(ep.games.map((g) => ({ kind: 'game', g })), [{ kind: 'mission' }]);
    const run = { ep, stars: 0, max: 0, vals: {} };
    for (let i = 0; i < steps.length; i++) {
      const res = await runStep(steps[i], run, i / steps.length);
      if (res === 'quit') { save(); return home(); }
    }
    for (let i = 0; i < run.max; i++) tally('en', i < run.stars);
    const stars = Math.max(1, Math.min(3, Math.round(3 * (run.max ? run.stars / run.max : 1))));
    const before = prog(id);
    state.progress[id] = { done: true, stars: Math.max(stars, before ? before.stars || 0 : 0), at: Date.now() };
    const goal = noteEpisodeDone();
    save();
    reward(ep, stars, goal);
  }

  function epFrame(run, frac, inner) {
    const el = screen(`<div class="stage">
      <div class="ep-top"><button class="iconbtn" data-quit aria-label="Beenden">✕</button>
        <div class="progress"><i style="width:${Math.round(frac * 100)}%"></i></div>
        <button class="iconbtn" data-explain aria-label="Ruby erklärt">💡</button></div>
      ${inner}<div class="ki-hinweis">🤖 KI-generierte Stimme</div></div>`);
    requestAnimationFrame(() => { const i = el.querySelector('.progress > i'); if (i) i.style.width = Math.round(frac * 100 + 6) + '%'; });
    el.querySelector('[data-explain]').onclick = () => { A.sfx('tap'); explain(run.ep); };
    return el;
  }
  function stepPromise(el, run) {
    return new Promise((resolve) => {
      el.querySelector('[data-quit]').onclick = () => { A.sfx('tap'); resolve('quit'); };
      el._done = resolve;
    });
  }

  /** Satz-Pool einer Quest um die passenden Lernpfad-Sätze erweitern (g.mehr = Einheiten-IDs), damit sich lange nichts wiederholt. */
  async function mitLernpfad(g) {
    if (!g.mehr) return g;
    const lp = await lernpfad('en'); if (!lp) return g;
    const extra = lpEinheiten(lp).filter((e) => g.mehr.includes(e.id)).flatMap((e) => e.saetze.map(([en, de, img]) => ({ en, de, img })));
    const eigene = new Set((g.lines || g.sentences).map((x) => x.en));
    const neu = extra.filter((x) => !eigene.has(x.en) && (g.type !== 'train' || PK.zugGeeignet(x.en)));
    return g.type === 'echo' ? { ...g, lines: g.lines.concat(neu) } : { ...g, sentences: g.sentences.concat(neu) };
  }
  function runStep(step, run, frac) {
    if (step.kind === 'scene') return scene(run, frac);
    if (step.kind === 'mission') return mission(run, frac);
    const g = step.g;
    if (g.type === 'detective') return detective(run, frac, g);
    if (g.type === 'echo' || g.type === 'train') return mitLernpfad(g).then((g2) => (g.type === 'echo' ? echo(run, frac, g2) : train(run, frac, g2)));
    if (g.type === 'rubysays') return rubySays(run, frac, g);
    return Promise.resolve();
  }

  // ------------------------------------------------------------------ Szene hören
  function scene(run, frac) {
    const ep = run.ep; let i = 0;
    const el = epFrame(run, frac, `<div class="game-title">🎧 Hör zu! <small>${esc(ep.title)}</small></div>
      <div data-box></div>
      <div class="row center"><button class="btn white" data-again>🔊 Noch mal</button><button class="btn blue" data-next>Weiter ➜</button></div>`);
    const p = stepPromise(el, run);
    const tk = token;
    const box = el.querySelector('[data-box]');
    async function show() {
      const s = ep.scene[i]; const ch = C.characters[s.who];
      const words = s.en.split(' ').map((w, k) => `<span class="w" data-w="${k}">${esc(w)}</span>`).join(' ');
      box.innerHTML = `<div class="stage" style="min-height:0;gap:8px"><div class="who"><span>${ch.emoji}</span>${esc(ch.name)}</div>
        <div class="scene-pic">${s.img}</div><div class="scene-line">${words}</div><div class="hint-de">${esc(s.de)}</div></div>`;
      box.querySelectorAll('.w').forEach((w) => w.onclick = () => { A.sfx('tap'); speak(C.tapWord(w.textContent), s.who); w.classList.add('on'); setTimeout(() => w.classList.remove('on'), 500); });
      await highlightSpeak(box, s.en, s.who, tk);
    }
    async function highlightSpeak(root, text, who, t) {
      const ws = root.querySelectorAll('.w');
      let k = 0;
      const step = Math.max(260, 2600 / Math.max(ws.length, 1));
      const hearable = A.canHear(text, who);
      const timer = setInterval(() => { ws.forEach((w) => w.classList.remove('on')); if (ws[k]) ws[k].classList.add('on'); k++; if (k > ws.length) clearInterval(timer); }, hearable ? step : 380);
      const heard = await speak(text, who);
      if (!alive(t)) { clearInterval(timer); return; }
      if (!heard) await sleep(380 * (ws.length + 1));
      clearInterval(timer); ws.forEach((w) => w.classList.remove('on'));
    }
    el.querySelector('[data-again]').onclick = () => { A.sfx('tap'); show(); };
    el.querySelector('[data-next]').onclick = () => {
      A.sfx('whoosh'); i++;
      if (i >= ep.scene.length) el._done('ok'); else show();
    };
    show();
    return p;
  }

  // ------------------------------------------------------------------ Hör-Detektiv
  function detective(run, frac, g) {
    const el = epFrame(run, frac, `<div class="game-title">🔍 ${esc(g.title)} <small>Hör zu und tipp das richtige Bild an!</small></div>
      <div class="listen-row"><button class="speaker" data-play aria-label="Anhören">🔊</button><button class="slowbtn" data-slow aria-label="Langsam">🐢</button></div>
      <div data-nv></div><div class="choices" data-ch></div>
      ${ruby('', '')}`);
    const p = stepPromise(el, run); const tk = token;
    let round = 0; let last = null; let target = null; let firstTry = true;
    const rounds = g.rounds || 5;
    const reihe = PK.ziehen(g.items, rounds); // jede Runde ein anderes Ziel, erst wenn alle dran waren, wiederholt sich eines
    const card = (it) => {
      if (g.mode === 'count') return `<span class="num">${it.count}</span><span class="fishes">${'🐟'.repeat(it.count)}</span>`;
      if (g.mode === 'colour') return `<span class="blob" style="background:${it.css}"></span>`;
      return it.img;
    };
    const play = async (slow) => {
      const b = el.querySelector('[data-play]'); b.classList.add('playing');
      await speak(target.en, 'ruby', slow ? { slow: true } : null);
      if (alive(tk)) b.classList.remove('playing');
    };
    async function next() {
      if (round >= rounds) { el._done('ok'); return; }
      round++; firstTry = true;
      target = reihe[round - 1]; last = target;
      const n = Math.min(4, g.items.length);
      const opts = shuffle([target].concat(pickOthers(g.items, target, n - 1)));
      el.querySelector('[data-nv]').innerHTML = noVoiceBox(target.en);
      const ch = el.querySelector('[data-ch]'); ch.innerHTML = '';
      opts.forEach((o) => {
        const b = h(`<button class="choice" aria-label="Antwort"${DIAG && o === target ? ' data-ok' : ''}>${card(o)}</button>`);
        b.onclick = async () => {
          if (o === target) {
            run.max++; if (firstTry) run.stars++;
            b.classList.add('right'); A.sfx('right'); A.vibrate(25); confetti(25);
            rubySay(el, ['Super!', 'Yes! Well done!', 'Richtig!', 'Great!'][round % 4] + ` – <b>${esc(target.en)}</b> = ${esc(target.de)}`, 'dance');
            ch.querySelectorAll('button').forEach((x) => x.disabled = true);
            await sleep(1100); if (alive(tk)) next();
          } else {
            firstTry = false; b.classList.add('wrong'); b.disabled = true; A.sfx('soft');
            rubySay(el, 'Hmm, hör noch mal ganz genau hin! 👂', 'talk'); play(true);
          }
        };
        ch.appendChild(b);
      });
      rubySay(el, `Runde ${round} von ${rounds}: Was hörst du?`);
      await sleep(350); if (alive(tk)) play();
    }
    el.querySelector('[data-play]').onclick = () => { A.sfx('tap'); play(); };
    el.querySelector('[data-slow]').onclick = () => { A.sfx('tap'); play(true); };
    next();
    return p;
  }

  // ------------------------------------------------------------------ Ruby says
  function boatSVG(css) {
    return `<svg class="boat" viewBox="0 0 92 72" aria-hidden="true"><path d="M46 4 L46 46 L18 46 Z" fill="#fff" stroke="#9fb6c9" stroke-width="2"/><path d="M50 10 L50 46 L74 46 Z" fill="${css}" stroke="rgba(0,0,0,.2)" stroke-width="2"/><rect x="45" y="4" width="3" height="44" fill="#6d4c41"/><path d="M8 50 L84 50 L72 66 L20 66 Z" fill="${css}" stroke="rgba(0,0,0,.25)" stroke-width="2"/></svg>`;
  }
  function rubySays(run, frac, g) {
    const el = epFrame(run, frac, `<div class="game-title">🙋 ${esc(g.title)} <small>Tipp nur, wenn Ruby „Ruby says“ sagt!</small></div>
      <div class="listen-row"><button class="speaker" data-play aria-label="Anhören">🔊</button></div>
      <div class="cmd" data-cmd></div><div class="timer" data-timer hidden><i></i></div>
      <div class="choices three" data-ch></div>${ruby('', '')}`);
    const p = stepPromise(el, run); const tk = token;
    const rounds = g.rounds || 6;
    // Mindestens zwei Runden ohne „Ruby says“, aber nie die erste
    const plan = [true, true, false, true, false, true].slice(0, rounds);
    const order = [plan[0]].concat(shuffle(plan.slice(1)));
    let round = 0; let cmd = null; let target = null; let waiting = null;
    const zielReihe = PK.ziehen(g.targets, rounds);
    const phrase = () => C.rubySaysText(g.mode, target, cmd);
    const play = async () => {
      const b = el.querySelector('[data-play]'); b.classList.add('playing');
      await speak(phrase(), 'ruby');
      if (alive(tk)) b.classList.remove('playing');
    };
    async function next() {
      if (round >= rounds) { el._done('ok'); return; }
      cmd = order[round]; round++;
      target = zielReihe[round - 1];
      const ch = el.querySelector('[data-ch]'); ch.innerHTML = '';
      const txt = phrase();
      el.querySelector('[data-cmd]').innerHTML = A.canHear(txt, 'ruby') ? '' : esc(txt);
      shuffle(g.targets).forEach((t) => {
        const inner = g.mode === 'count' ? `<span class="fishes" style="font-size:22px">${'🐟'.repeat(t.count)}</span>` : boatSVG(t.css);
        const b = h(`<button class="choice" aria-label="Ziel"${DIAG && t === target ? ' data-ok' : ''}${DIAG && !cmd ? ' data-trick' : ''}>${inner}</button>`);
        b.onclick = () => tapped(t, b);
        ch.appendChild(b);
      });
      rubySay(el, `Runde ${round} von ${rounds} – gut zuhören!`);
      await sleep(300); if (!alive(tk)) return;
      await play(); if (!alive(tk)) return;
      if (!cmd) {
        const tm = el.querySelector('[data-timer]'); tm.hidden = false; const bar = tm.firstElementChild;
        bar.style.transition = 'none'; bar.style.width = '100%'; void bar.offsetWidth; bar.style.transition = 'width 4s linear'; bar.style.width = '0%';
        waiting = setTimeout(async () => {
          if (!alive(tk)) return; tm.hidden = true; run.max++; run.stars++;
          A.sfx('right'); confetti(30); rubySay(el, 'Stark! Ruby hat <b>nicht</b> „Ruby says“ gesagt – und du hast nicht getippt! 😎', 'dance');
          await sleep(1500); if (alive(tk)) next();
        }, 4000);
      }
    }
    async function tapped(t, b) {
      if (!alive(tk) || b.disabled) return;
      if (!cmd) {
        clearTimeout(waiting); el.querySelector('[data-timer]').hidden = true; run.max++;
        A.sfx('squawk'); b.classList.add('wrong'); el.querySelectorAll('.choice').forEach((x) => x.disabled = true);
        rubySay(el, 'Hihi, reingelegt! 😄 Ich habe nicht „Ruby says“ gesagt. Macht nichts – weiter!', 'dance');
        await sleep(1900); if (alive(tk)) next(); return;
      }
      if (t === target) {
        run.max++; run.stars++; b.classList.add('right'); A.sfx('right'); A.vibrate(25); confetti(25);
        el.querySelectorAll('.choice').forEach((x) => x.disabled = true);
        rubySay(el, `Yes! <b>${esc(target.en)}</b>!`, 'dance');
        await sleep(1100); if (alive(tk)) next();
      } else {
        b.classList.add('wrong'); b.disabled = true; A.sfx('soft'); rubySay(el, 'Fast! Hör noch mal.', 'talk'); play();
      }
    }
    el.querySelector('[data-play]').onclick = () => { A.sfx('tap'); play(); };
    next();
    return p;
  }

  // ------------------------------------------------------------------ Papagei-Echo
  function echo(run, frac, g) {
    const el = epFrame(run, frac, `<div class="game-title">🦜 ${esc(g.title)} <small>Hör zu – und sprich nach wie ein Papagei!</small></div>
      <div class="echo-line" data-line></div>
      <div class="listen-row"><button class="speaker" data-play aria-label="Anhören">🔊</button>
        <button class="mic" data-mic aria-label="Aufnehmen">🎤</button></div>
      <div class="mic-label" data-ml>Erst anhören, dann auf 🎤 tippen und nachsprechen.</div>
      <div class="echo-stars" data-st></div>
      <div class="row center"><button class="btn white small" data-cmp hidden>👂 Vergleichen</button><button class="btn blue" data-next hidden>Weiter ➜</button></div>
      <button class="quiet" data-quiet>Ich sage es leise für mich</button>
      ${ruby('', '')}
      <div class="ai-note">Deine Aufnahme bleibt auf diesem Gerät und wird nach dem Anhören gelöscht.</div>`);
    const p = stepPromise(el, run); const tk = token;
    let i = 0; let recording = null; let lastUrl = null;
    const zeilen = PK.ziehen(g.lines, Math.min(5, g.lines.length)); // 5 verschiedene, bei jedem Spielen andere Auswahl/Reihenfolge
    const line = () => zeilen[i];
    const play = async (slow) => {
      const b = el.querySelector('[data-play]'); b.classList.add('playing'); el.querySelector('[data-ruby]').classList.add('talk');
      await speak(line().en, 'ruby', slow ? { slow: true } : null);
      if (!alive(tk)) return; b.classList.remove('playing'); el.querySelector('[data-ruby]').classList.remove('talk');
    };
    function show() {
      const l = line();
      el.querySelector('[data-line]').innerHTML = `<div class="pic">${l.img}</div><div class="en">${esc(l.en)}</div><div class="hint-de">${esc(l.de)}</div>`;
      el.querySelector('[data-st]').textContent = '';
      el.querySelector('[data-next]').hidden = true; el.querySelector('[data-cmp]').hidden = true;
      el.querySelector('[data-ml]').textContent = A.micSupported() ? 'Erst anhören, dann auf 🎤 tippen und nachsprechen.' : 'Sprich den Satz laut nach – Ruby hört dir zu!';
      rubySay(el, `Satz ${i + 1} von ${zeilen.length}. Sprich mir nach!`);
      play();
    }
    // Sterne gibt es nur, wenn die Aussprache wirklich gemessen wurde (stars > 0). Bloßes Aufnehmen wird nicht bewertet.
    function done(stars, msg) {
      if (stars > 0) { run.max++; if (stars >= 2) run.stars++; }
      el.querySelector('[data-st]').innerHTML = stars > 0 ? [0, 1, 2].map((k) => `<span style="display:inline-block;animation:popbig .5s ${k * .15}s both">${k < stars ? '⭐' : '☆'}</span>`).join('') : '';
      el.querySelector('[data-next]').hidden = false;
      rubySay(el, msg, stars > 0 ? 'dance' : 'talk'); A.sfx(stars > 0 ? 'right' : 'pop'); if (stars >= 2) confetti(30);
    }
    el.querySelector('[data-mic]').onclick = async () => {
      const mic = el.querySelector('[data-mic]'); const ml = el.querySelector('[data-ml]');
      if (!A.micSupported()) { done(0, 'Wenn du laut nachsprichst, lernst du am meisten. 😊'); return; }
      if (recording) {
        const r = recording; recording = null; mic.classList.remove('on'); mic.textContent = '🎤';
        const res = await r.stop(); if (!alive(tk)) return;
        lastUrl = res.url;
        const sv = await serverAuswerten('en', line().en, res, ml); if (!alive(tk)) return;
        if (sv) {
          el.querySelector('[data-cmp]').hidden = !res.url;
          if (sv.status === 'ok' && sv.sterne > 0) { ml.textContent = 'Hör dir selbst zu: erst Ruby, dann du!'; done(sv.sterne, `Ich habe dich verstanden: „${esc(sv.text)}“ ⭐`); }
          else if (sv.status === 'ok') { ml.textContent = 'Hör noch einmal zu und versuch es noch einmal!'; A.sfx('soft'); rubySay(el, 'Probier es noch einmal! Hör dir Ruby an und sprich mir nach. 🦜', 'talk'); }
          else { ml.textContent = NICHT_MOEGLICH; done(0, 'Gut gemacht! Hör mal, wie es im Vergleich klingt. 👂'); }
          return;
        }
        const spoke = res.loudFrames > 6 && res.ms > 500;
        ml.textContent = spoke ? 'Hör dir selbst zu: erst Ruby, dann du!' : 'Ich habe fast nichts gehört. Sprich ruhig lauter!';
        if (spoke) {
          el.querySelector('[data-cmp]').hidden = false;
          done(0, 'Gut gemacht! Hör mal, wie es im Vergleich klingt: Tipp auf „Vergleichen“. 👂');
        } else { A.sfx('soft'); rubySay(el, 'Noch mal? Tipp auf 🎤 und sprich laut nach!', 'talk'); }
        return;
      }
      A.stop(); A.sfx('pop');
      try {
        recording = await A.record((lvl) => mic.style.setProperty('--lvl', lvl.toFixed(2)));
        if (!alive(tk)) { recording.stop(); return; }
        mic.classList.add('on'); mic.textContent = '⏹️'; ml.textContent = 'Ich höre zu … tipp auf ⏹️, wenn du fertig bist.';
      } catch (e) {
        recording = null;
        ml.textContent = 'Das Mikrofon ist nicht erlaubt. Sprich einfach laut nach und tipp auf „leise für mich“.';
        rubySay(el, 'Kein Mikrofon? Kein Problem – sprich trotzdem laut mit!', 'talk');
      }
    };
    el.querySelector('[data-cmp]').onclick = async () => { A.sfx('tap'); await play(); if (!alive(tk)) return; await sleep(250); const u = lastUrl; lastUrl = null; await A.playUrl(u); el.querySelector('[data-cmp]').hidden = true; };
    el.querySelector('[data-play]').onclick = () => { A.sfx('tap'); play(); };
    el.querySelector('[data-quiet]').onclick = () => { A.sfx('tap'); if (el.querySelector('[data-next]').hidden) done(0, 'Okay! Beim nächsten Mal vielleicht laut? 😊'); };
    el.querySelector('[data-next]').onclick = () => {
      A.sfx('whoosh'); if (recording) { recording.stop(); recording = null; }
      if (lastUrl) { try { URL.revokeObjectURL(lastUrl); } catch (e) {} lastUrl = null; }
      i++; if (i >= zeilen.length) el._done('ok'); else show();
    };
    show();
    return p;
  }

  // ------------------------------------------------------------------ Satz-Zug
  function train(run, frac, g) {
    const el = epFrame(run, frac, `<div class="game-title">🚂 ${esc(g.title)} <small>Hör zu und häng die Waggons richtig an!</small></div>
      <div class="listen-row"><button class="speaker" data-play aria-label="Anhören">🔊</button><button class="slowbtn" data-slow aria-label="Langsam">🐢</button></div>
      <div data-nv></div><div class="hint-de" data-de></div>
      <div class="track"><div class="train" data-train></div></div>
      <div class="pool" data-pool></div>
      ${ruby('', '')}`);
    const p = stepPromise(el, run); const tk = token;
    let i = 0; let words = []; let placed = []; let firstTry = true;
    const saetze = PK.ziehen(g.sentences, Math.min(5, g.sentences.length)); // jedes Mal andere Auswahl/Reihenfolge
    const s = () => saetze[i];
    const play = async (slow) => { const b = el.querySelector('[data-play]'); b.classList.add('playing'); await speak(s().en, 'ruby', slow ? { slow: true } : null); if (alive(tk)) b.classList.remove('playing'); };
    function draw() {
      const tr = el.querySelector('[data-train]');
      tr.innerHTML = '<span class="loco">🚂</span>' + placed.map((k, j) => `<button class="wagon" data-t="${j}">${esc(words[k])}</button>`).join('');
      tr.querySelectorAll('[data-t]').forEach((b) => b.onclick = () => { A.sfx('tap'); placed.splice(+b.dataset.t); draw(); });
      const pool = el.querySelector('[data-pool]');
      pool.innerHTML = words.map((w, k) => `<button class="wagon ${placed.includes(k) ? 'used' : ''}" data-k="${k}">${esc(w)}</button>`).join('');
      pool.querySelectorAll('[data-k]').forEach((b) => b.onclick = () => { if (placed.includes(+b.dataset.k)) return; A.sfx('pop'); A.vibrate(10); placed.push(+b.dataset.k); draw(); check(); });
    }
    async function check() {
      if (placed.length < words.length) return;
      const built = placed.map((k) => words[k]).join(' ');
      const tr = el.querySelector('[data-train]');
      if (built === s().en) {
        run.max++; if (firstTry) run.stars++;
        A.sfx('choo'); rubySay(el, 'Tschu-tschuuu! Richtig! 🎉', 'dance'); confetti(40);
        speak(s().en, 'ruby');
        tr.classList.add('go'); el.querySelectorAll('.wagon').forEach((x) => x.disabled = true);
        await sleep(1900); if (!alive(tk)) return;
        i++; if (i >= saetze.length) el._done('ok'); else show();
      } else {
        firstTry = false; A.sfx('soft');
        const want = C.words(s().en);
        let ok = 0; while (ok < placed.length && words[placed[ok]] === want[ok]) ok++;
        tr.querySelectorAll('.wagon').forEach((w, j) => { if (j >= ok) w.classList.add('bad'); });
        rubySay(el, ok ? `Die ersten ${ok} Waggons stimmen! Der Rest fährt zurück. Hör noch mal.` : 'Hmm, der erste Waggon passt noch nicht. Hör noch mal!', 'talk');
        await sleep(700); if (!alive(tk)) return;
        placed = placed.slice(0, ok); draw(); play(true);
      }
    }
    function show() {
      firstTry = true; placed = [];
      const ws = C.words(s().en);
      // Mischen, aber nie in der richtigen Reihenfolge
      let mix = shuffle(ws.map((w, k) => k)); let guard = 0;
      while (ws.length > 1 && mix.every((k, j) => ws[k] === ws[j]) && guard++ < 10) mix = shuffle(mix);
      words = mix.map((k) => ws[k]);
      el.querySelector('[data-de]').textContent = '„' + s().de + '“';
      el.querySelector('[data-nv]').innerHTML = noVoiceBox(s().en);
      if (DIAG) el.querySelector('[data-train]').dataset.want = s().en;
      el.querySelector('[data-train]').classList.remove('go');
      draw();
      rubySay(el, `Zug ${i + 1} von ${saetze.length}: Bau den Satz, den du hörst!`);
      play();
    }
    el.querySelector('[data-play]').onclick = () => { A.sfx('tap'); play(); };
    el.querySelector('[data-slow]').onclick = () => { A.sfx('tap'); play(true); };
    show();
    return p;
  }

  // ------------------------------------------------------------------ Sprech-Mission (Dialog-Bühne)
  function askName() {
    return new Promise((resolve) => {
      const bg = h(`<div class="modal-bg"><div class="modal card"><h2 style="font-size:20px">Wie heißt du?</h2>
        <p class="muted">Dann kannst du Mia deinen Namen sagen. Der Name bleibt nur auf diesem Gerät.</p>
        <input class="input" maxlength="15" autocomplete="off" placeholder="Vorname oder Spitzname"><div class="err"></div>
        <div class="row" style="margin-top:8px"><button class="btn blue" data-ok>Fertig</button></div></div></div>`);
      document.body.appendChild(bg);
      const inp = bg.querySelector('input'); setTimeout(() => inp.focus(), 50);
      bg.querySelector('[data-ok]').onclick = () => {
        const err = C.checkName(inp.value);
        if (err) { bg.querySelector('.err').textContent = err; A.sfx('soft'); return; }
        state.name = inp.value.trim(); save(); bg.remove(); A.sfx('right'); resolve(state.name);
      };
    });
  }

  function mission(run, frac) {
    const m = run.ep.mission; const npc = C.characters[m.with];
    const el = epFrame(run, frac, `<div class="game-title">🎭 Sprech-Mission <small>${esc(m.title)}</small></div>
      <div class="dialog" data-dlg></div>${ruby('', '')}`);
    const p = stepPromise(el, run); const tk = token;
    const dlg = el.querySelector('[data-dlg]');
    const vals = run.vals; vals.name = state.name;
    let k = 0;
    const fill = (t) => C.fill(t, vals);

    async function npcSay(who, en, de, extra) {
      const ch = C.characters[who];
      const node = h(`<div class="npc"><div class="face">${ch.emoji}</div><div class="bubble"><b>${esc(ch.name)}:</b> ${esc(en)}<small>${esc(de)}</small>${extra || ''}</div></div>`);
      dlg.appendChild(node); node.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
      await speak(en, who);
    }
    async function stepRun() {
      if (k >= m.steps.length) {
        await npcSay(m.end.who, fill(m.end.en), fill(m.end.de));
        if (!alive(tk)) return;
        A.sfx('fanfare'); confetti(60); rubySay(el, 'Mission geschafft! Du hast Englisch gesprochen! 🎉', 'dance');
        const b = h('<button class="btn green" style="align-self:center">Zum Stempel ➜</button>'); dlg.appendChild(b); b.scrollIntoView({ block: 'nearest' });
        b.onclick = () => { A.sfx('tap'); el._done('ok'); };
        return;
      }
      const st = m.steps[k];
      const extra = st.show && st.show.boat ? `<div style="text-align:center">${boatSVG((C.colours.find((c) => c.id === st.show.boat) || {}).css)}</div>` : '';
      await npcSay(st.who, fill(st.en), fill(st.de), extra);
      if (!alive(tk)) return;
      if (st.choose) await choose(st.choose);
      if (!alive(tk)) return;
      offer(st);
    }
    function choose(kind) {
      return new Promise((resolve) => {
        const box = h(`<div class="card"><div style="font-weight:800;margin-bottom:8px">${kind === 'number' ? 'Wie viele Fische möchtest du? Tipp eine Zahl an:' : 'Welche Farbe soll dein Boot haben?'}</div><div class="picker"></div></div>`);
        const pk = box.querySelector('.picker');
        if (kind === 'number') for (let n = 1; n <= 10; n++) { const b = h(`<button>${n}</button>`); b.onclick = () => { A.sfx('pop'); vals.n = n; speak(C.numbers[n - 1], 'ruby'); box.remove(); resolve(); }; pk.appendChild(b); }
        else C.colours.forEach((c) => { const b = h(`<button class="sw" style="background:${c.css}" aria-label="${c.de}"></button>`); b.onclick = () => { A.sfx('pop'); vals.c = c.id; speak(c.en, 'ruby'); box.remove(); resolve(); }; pk.appendChild(b); });
        dlg.appendChild(box); box.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
      });
    }
    function offer(st) {
      const box = h('<div class="opts"></div>');
      rubySay(el, 'Was sagst du? Tipp auf eine Antwort.');
      shuffle(st.options).forEach((o) => {
        const b = h(`<button class="opt"${DIAG && o.ok ? ' data-ok' : ''}>${esc(fill(o.en))}<small>${esc(fill(o.de))}</small></button>`);
        b.onclick = async () => {
          if (o.en.includes('{name}') && !vals.name) { vals.name = await askName(); b.innerHTML = `${esc(fill(o.en))}<small>${esc(fill(o.de))}</small>`; }
          A.sfx('tap');
          if (!o.ok) {
            b.classList.add('no'); b.disabled = true;
            const ch = C.characters[st.who];
            const node = h(`<div class="npc"><div class="face">${ch.emoji}</div><div class="bubble">${esc(o.reply.en)}<small>${esc(o.reply.de)}</small></div></div>`);
            box.before(node); speak(o.reply.en, st.who); rubySay(el, 'Hihi! Probier eine andere Antwort. 😄', 'talk');
            return;
          }
          box.remove();
          await sayIt(fill(o.en));
          if (!alive(tk)) return;
          k++; stepRun();
        };
        box.appendChild(b);
      });
      dlg.appendChild(box); box.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
    }
    /** Das Kind sagt den Satz laut. Erkennung, wenn möglich und erlaubt; sonst Lautstärke; sonst „leise für mich“. */
    function sayIt(text) {
      return new Promise((resolve) => {
        let tries = 0;
        const box = h(`<div class="card center" style="display:flex;flex-direction:column;gap:10px;align-items:center">
          <div style="font-weight:900;font-size:22px">„${esc(text)}“</div>
          <div class="row center"><button class="slowbtn" data-hear aria-label="Anhören">🔊</button><button class="mic" data-mic aria-label="Sprechen">🎤</button></div>
          <div class="heard" data-heard>Tipp auf 🎤 und sag es laut!</div>
          <button class="quiet" data-quiet>Ich sage es leise für mich</button></div>`);
        dlg.appendChild(box); box.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
        const finish = (msg, gemessen) => { // Stern nur, wenn die Erkennung den Satz wirklich verstanden hat
          if (gemessen) { run.max++; run.stars++; }
          box.replaceWith(h(`<div class="me">🗣️ ${esc(text)}</div>`));
          A.sfx('right'); A.vibrate(25); rubySay(el, msg, 'dance'); resolve();
        };
        box.querySelector('[data-hear]').onclick = () => { A.sfx('tap'); speak(text, 'ruby'); };
        box.querySelector('[data-quiet]').onclick = () => { A.sfx('tap'); box.replaceWith(h(`<div class="me">🤫 ${esc(text)}</div>`)); resolve(); };
        const mic = box.querySelector('[data-mic]'); const heard = box.querySelector('[data-heard]');
        let busy = false; let recH = null;
        mic.onclick = async () => {
          if (recH) { // Aufnahme ohne Erkennung beenden
            const r = recH; recH = null; mic.classList.remove('on'); mic.textContent = '🎤';
            const res = await r.stop(); if (res.url) URL.revokeObjectURL(res.url);
            const sv = await serverAuswerten('en', text, res, heard);
            if (sv) {
              tries++;
              if (sv.status === 'ok' && sv.sterne > 0) { finish(`Ich habe dich verstanden: „${esc(sv.text)}“ ⭐`, true); return; }
              if (sv.status !== 'ok' || tries >= 2) { finish(sv.status === 'ok' ? 'Du hast es versucht – weiter geht\'s! 👍' : NICHT_MOEGLICH); return; }
              heard.textContent = 'Hör dir den Satz noch mal an und versuch es noch einmal!'; A.sfx('soft'); return;
            }
            if (res.loudFrames > 6) finish('Gut gemacht – du hast es laut gesagt! 👍'); else { heard.textContent = 'Ich habe fast nichts gehört – noch mal, ruhig lauter!'; A.sfx('soft'); }
            return;
          }
          if (busy) return; busy = true; A.stop(); A.sfx('pop');
          const mode = window.AUSWERTUNG && window.AUSWERTUNG.serverFuer('en') ? 'none' : A.recognitionMode();   // Server gewählt: nur aufnehmen
          if (mode !== 'none') {
            mic.classList.add('on'); heard.textContent = 'Ich höre zu …';
            const alts = await A.listen(); mic.classList.remove('on'); busy = false;
            if (!alive(tk)) return;
            if (alts && alts.length) {
              tries++;
              const sim = A.similarity(text, alts);
              if (sim >= 0.5) { finish(`Ich habe dich verstanden: „${esc(alts[0])}“ ⭐`, true); return; }
              if (tries >= 2) { finish('Das Mikrofon tut sich manchmal schwer – du hast es versucht! Weiter geht\'s. 👍'); return; }
              heard.textContent = `Ich habe „${alts[0]}“ gehört. Hör dir den Satz noch mal an und versuch es noch einmal!`; A.sfx('soft'); return;
            }
            if (alts && !alts.length) { tries++; if (tries >= 2) { finish('Weiter geht\'s! 👍'); return; } heard.textContent = 'Ich hab dich nicht ganz gehört – noch mal?'; A.sfx('soft'); return; }
            // null: Erkennung nicht erlaubt/verfügbar → Aufnahme mit Lautstärke
          }
          busy = false;
          if (!A.micSupported()) { finish('Laut sagen hilft beim Lernen! 👍'); return; }
          try {
            recH = await A.record((lvl) => mic.style.setProperty('--lvl', lvl.toFixed(2)));
            mic.classList.add('on'); mic.textContent = '⏹️'; heard.textContent = 'Sprich jetzt – dann tipp auf ⏹️.';
          } catch (e) { heard.textContent = 'Das Mikrofon ist nicht erlaubt. Sag es laut und tipp auf „leise für mich“.'; }
        };
      });
    }
    stepRun();
    return p;
  }

  // ------------------------------------------------------------------ Belohnung
  function reward(ep, stars, goal) {
    const el = screen(`<div class="reward">
      ${ruby(`Du hast „${esc(ep.title)}“ geschafft! Hier ist dein Stempel! 🎉${goal ? ' Und dein Wochenziel ist erreicht – Squawk! 🏆' : ''}`, 'dance')}
      <div class="stamp">${ep.emoji}</div>
      <div class="bigstars">${[0, 1, 2].map((k) => `<span style="animation-delay:${0.5 + k * 0.25}s">${k < stars ? '⭐' : '☆'}</span>`).join('')}</div>
      <div class="row center"><button class="btn white" data-explain>💡 Ruby erklärt</button><button class="btn blue" data-home>Zum Hafen ⚓</button></div>
    </div>`);
    setTimeout(() => { A.sfx('stamp'); A.vibrate(60); confetti(120); }, 250);
    el.querySelector('[data-home]').onclick = () => { A.sfx('tap'); home(); };
    el.querySelector('[data-explain]').onclick = () => { A.sfx('tap'); explain(ep); };
  }

  function explain(ep) {
    const bg = h(`<div class="modal-bg"><div class="modal card explain">
      ${ruby(`<b>${esc(ep.explain.title)}</b>`, 'talk')}
      <ul>${ep.explain.points.map((x) => `<li>${esc(x)}</li>`).join('')}</ul>
      <div class="row center"><button class="btn blue" data-close>Verstanden! 👍</button></div></div></div>`);
    document.body.appendChild(bg);
    bg.querySelector('[data-close]').onclick = () => { A.sfx('tap'); bg.remove(); };
    bg.onclick = (e) => { if (e.target === bg) bg.remove(); };
  }

  // ------------------------------------------------------------------ Reisepass
  function passport() {
    const eps = C.worlds[0].episodes;
    const el = screen(`<div>
      <div class="top"><button class="iconbtn" data-back aria-label="Zurück">←</button><div class="title" style="font-size:22px">📘 Mein Reisepass</div></div>
      ${ruby(`${stampCount()} ${stampCount() === 1 ? 'Stempel' : 'Stempel'} gesammelt. Jeder Ort am Hafen gibt einen Stempel!`)}
      <div class="passgrid">${eps.map((e) => { const pr = prog(e.id); return `<div class="passitem ${pr && pr.done ? '' : 'empty'}"><div class="s">${pr && pr.done ? e.emoji : '◌'}</div><b>${esc(e.title)}</b><div class="de">${esc(e.de)}</div><div>${pr && pr.done ? '⭐'.repeat(pr.stars) : ''}</div></div>`; }).join('')}</div>
    </div>`);
    el.querySelector('[data-back]').onclick = () => { A.sfx('tap'); home(); };
  }

  // ------------------------------------------------------------------ Einstellungen + Eltern-Tor
  function settings() {
    const s = state.settings;
    const sw = (key, label) => `<div class="set"><span>${label}</span><button class="switch ${s[key] ? 'on' : ''}" data-sw="${key}" role="switch" aria-checked="${!!s[key]}" aria-label="${label}"></button></div>`;
    const el = screen(`<div>
      <div class="top"><button class="iconbtn" data-back aria-label="Zurück">←</button><div class="title" style="font-size:22px">⚙️ Einstellungen</div></div>
      <div class="card">
        ${sw('sound', '🔔 Töne')}${sw('voice', '🗣️ Stimme (Englisch vorlesen)')}${sw('slow', '🐢 Immer langsamer sprechen')}${sw('vibrate', '📳 Vibration')}

        <div class="set"><span>🎒 Klasse</span><span class="row" style="justify-content:flex-end">${KLASSEN.map((k) => `<button class="btn small ${state.klasse === k ? 'blue' : 'white'}" data-k="${k}">${k}</button>`).join('')}</span></div>
        <div class="set"><span>🙂 Name</span><span class="row"><b>${esc(state.name || '–')}</b><button class="btn small white" data-name>Ändern</button></span></div>
      </div>
      <div class="card" style="margin-top:14px"><h3 style="margin-bottom:4px">🎙️ Welche Stimme?</h3>
        <label class="radio"><input type="radio" name="stimme" value="frei" ${A.voiceMode() === 'frei' ? 'checked' : ''}><span><b>Unsere KI-Stimmen (Standard)</b><br><span class="muted">Vorab aufgenommen, mit KI erzeugt, funktionieren auch ohne Internet.</span></span></label>
        <label class="radio"><input type="radio" name="stimme" value="geraet" ${A.voiceMode() === 'geraet' ? 'checked' : ''}><span><b>Stimme des Geräts</b><br><span class="muted">${A.canSpeak() ? 'Die englische Stimme deines Geräts.' : 'Auf diesem Gerät gibt es keine englische Stimme.'}</span></span></label>
      </div>
      <div class="card" style="margin-top:14px">
        <div class="set"><span>👪 Für Eltern</span><button class="btn small blue" data-parents>Öffnen</button></div>
        <div class="set"><span>🔒 Deine Daten – kurz erklärt</span><button class="btn small white" data-kids>Ansehen</button></div>
        <div class="set"><span>📄 Datenschutz für Erwachsene</span><a class="btn small white" href="datenschutz.html">Lesen</a></div>
      </div>
      <div class="foot">Version ${esc(window.APP_VERSION)} vom ${esc(window.APP_VERSION_DATE)}<br>🤖 KI-generierte Stimmen – Angaben unter <a href="lizenzen.txt">KI-Stimmen &amp; Lizenzen</a></div>
    </div>`);
    el.querySelector('[data-back]').onclick = () => { A.sfx('tap'); home(); };
    el.querySelectorAll('[data-sw]').forEach((b) => b.onclick = () => { s[b.dataset.sw] = !s[b.dataset.sw]; save(); A.sfx('tap'); settings(); });
    el.querySelectorAll('[data-k]').forEach((b) => b.onclick = () => { state.klasse = +b.dataset.k; save(); A.sfx('tap'); settings(); });
    el.querySelectorAll('input[name=stimme]').forEach((x) => x.onchange = () => { s.stimme = x.value; save(); A.sfx('tap'); speak("Hello! I'm Ruby.", 'ruby'); });
    el.querySelector('[data-name]').onclick = async () => { await askName(); settings(); };
    el.querySelector('[data-parents]').onclick = () => { A.sfx('tap'); parentGate(); };
    el.querySelector('[data-kids]').onclick = () => { A.sfx('tap'); kidsPrivacy(); };
  }

  /** Datenschutz in Kindersprache (Pflicht 8): kurz, ehrlich, mit Ruby. */
  function kidsPrivacy(zurueck) {
    const punkte = [
      ['🏠', window.FAMILIE_AKTIV && window.FAMILIE && window.FAMILIE.gekoppelt()
        ? 'Dein Lernstand bleibt auf deinen Geräten. Wenn deine Eltern es einschalten, wird er verschlüsselt auf unserem Server in Deutschland gesichert, damit du auf mehreren Geräten weiterüben kannst. Wir schauen ihn uns nicht an und geben ihn nicht weiter. Deine eigenen Wörterlisten werden dabei auch verschlüsselt gesichert. Deine Antworten und dein Name werden dabei nicht an uns gesendet.'   // Recht V40 Fassung A + V42 (Wörterlisten)
        : 'Deine Antworten, dein Name und dein Lernstand bleiben auf <b>diesem Gerät</b>.'],
      ['🙈', 'Du brauchst <b>kein Konto</b> und keine E-Mail. Deinen Namen musst du nicht verraten – ein Spitzname reicht.'],
      ['🎤', 'Das Mikrofon geht <b>nur an, wenn du auf 🎤 tippst</b>. Deine Aufnahme bleibt auf dem Gerät und wird nach dem Anhören gelöscht. Nur wenn deine Eltern es erlauben, darf die Spracherkennung deines Browsers übers Internet mithelfen.'],
      ['🚫', 'Hier gibt es <b>keine Werbung</b>. Wir zählen nur anonym, wie oft etwas in der App benutzt wird – ohne deinen Namen und ohne deine Antworten. Deine Eltern können das Zählen ausschalten.'],
      ['📒', 'Deine eigenen Wortlisten bleiben auch nur hier auf dem Gerät.'],
      ['👪', 'Manche Dinge, zum Beispiel die Spracherkennung übers Internet, können nur Erwachsene einschalten.'],
    ];
    const el = screen(`<div>
      <div class="top"><button class="iconbtn" data-back aria-label="Zurück">←</button><div class="title" style="font-size:22px">🔒 Deine Daten</div></div>
      ${ruby('Squawk! Ich passe auf deine Daten auf wie auf meine Federn. 🪶', 'talk')}
      <div class="card">${punkte.map(([e, t]) => `<div class="set" style="justify-content:flex-start;gap:14px"><span style="font-size:30px">${e}</span><span style="font-weight:600">${t}</span></div>`).join('')}</div>
      <p class="muted" style="margin-top:12px">Für Erwachsene: <a href="datenschutz.html">ausführliche Datenschutzerklärung</a> · <a href="datenblatt-schule.html">Datenblatt für Schulen</a></p>
    </div>`);
    el.querySelector('[data-back]').onclick = () => { A.sfx('tap'); (zurueck || settings)(); };
  }

  // ------------------------------------------------------------------ Eltern-Tor (gemeinsamer Standard ELTERN-TOR der AppZentrale)
  // Malaufgabe 13–19 × 6–9 in Worten, mit Vorlese-Knopf, Eingabe über eigene Ziffern-Tastatur. Nach einem Fehler sofort eine
  // neue Aufgabe. Kein Zeitdruck, jederzeit abbrechen. Gelöst = 10 Minuten offen (nur im Arbeitsspeicher).
  const ZAHLWORT = { 6: 'sechs', 7: 'sieben', 8: 'acht', 9: 'neun', 13: 'dreizehn', 14: 'vierzehn', 15: 'fünfzehn', 16: 'sechzehn', 17: 'siebzehn', 18: 'achtzehn', 19: 'neunzehn' };
  let torOffenBis = 0;
  function elternTor(dann, grund) {
    if (Date.now() < torOffenBis) { dann(); return; }
    let a; let b; let eingabe = '';
    const bg = h(`<div class="modal-bg"><div class="modal card tor" role="dialog" aria-labelledby="tor-t">
      <h2 id="tor-t" style="font-size:20px">👪 Eltern-Tor</h2>
      ${grund ? `<p style="font-weight:800;margin:4px 0">${esc(grund)}</p>` : ''}<p class="muted">Dieser Bereich ist für Erwachsene. Bitte rechne:</p>
      <div class="tor-aufgabe"><b data-aufgabe aria-live="polite"></b></div>
      <div class="tor-eingabe" data-eingabe aria-label="Deine Eingabe">&nbsp;</div><div class="err" data-err></div>
      <div class="tor-tasten">${[1, 2, 3, 4, 5, 6, 7, 8, 9].map((z) => `<button class="tor-taste" data-z="${z}" aria-label="${z}">${z}</button>`).join('')}
        <button class="tor-taste" data-weg aria-label="Löschen">⌫</button><button class="tor-taste" data-z="0" aria-label="0">0</button><button class="tor-taste ok" data-ok aria-label="Bestätigen">OK</button></div>
      <div class="row" style="margin-top:8px"><button class="btn white" data-x>Abbrechen</button></div></div></div>`);
    document.body.appendChild(bg);
    const neu = (hinweis) => {
      a = 13 + Math.floor(Math.random() * 7); b = 6 + Math.floor(Math.random() * 4); eingabe = '';
      const t = `${ZAHLWORT[a]} mal ${ZAHLWORT[b]}`;
      const auf = bg.querySelector('[data-aufgabe]'); auf.textContent = t[0].toUpperCase() + t.slice(1); auf.setAttribute('aria-label', 'Aufgabe: ' + t);
      if (DIAG) bg.querySelector('.tor').dataset.loesung = a * b;
      bg.querySelector('[data-eingabe]').innerHTML = '&nbsp;'; bg.querySelector('[data-err]').textContent = hinweis || '';
    };
    const zeig = () => { bg.querySelector('[data-eingabe]').textContent = eingabe || ' '; };
    bg.querySelectorAll('[data-z]').forEach((t) => t.onclick = () => { if (eingabe.length < 4) { eingabe += t.dataset.z; zeig(); A.sfx('tap'); } });
    bg.querySelector('[data-weg]').onclick = () => { eingabe = eingabe.slice(0, -1); zeig(); A.sfx('tap'); };
    bg.querySelector('[data-x]').onclick = () => { A.sfx('tap'); bg.remove(); };
    bg.querySelector('[data-ok]').onclick = () => {
      if (!eingabe) return;
      if (+eingabe !== a * b) { A.sfx('soft'); neu('Das stimmt leider nicht. Hier ist eine neue Aufgabe.'); return; }
      bg.remove(); torOffenBis = Date.now() + 10 * 60 * 1000; zaehlen('elternbereich'); dann();
    };
    neu();
  }
  function parentGate() { elternTor(parents); }

  /** Nachhilfe-Hinweis (Auftrag C1): nur hier hinter dem Eltern-Tor, nur wenn ein Fach hakt, je Fach höchstens alle 14 Tage. */
  function nachhilfeHinweis() {
    if (state.settings.nachhilfe === false) return null;
    const d = PK.today();
    for (const l of PK.ORDER) {
      const st = stats(l, 14); const thema = PK.LANGS[l].thema;
      if (st.n >= 10 && st.pct < 60 && !(state.hinweis[thema] && d - state.hinweis[thema] < 14)) return { l, thema, st };
    }
    return null;
  }
  /** Hinweis „Klassenarbeit bald“: 3–14 Tage vor einem eingetragenen Test, je Test einmal ausblendbar. */
  function arbeitHinweis() {
    if (state.settings.nachhilfe === false) return null;
    const d = PK.today();
    for (const l of PK.ORDER) {
      const t = state.tests[l]; if (!t) continue;
      const rest = dayOf(t.date) - d;
      if (rest >= 3 && rest <= 14 && state.hinweis['ka-' + l] !== t.date) return { l, t, thema: PK.LANGS[l].thema };
    }
    return null;
  }
  function probestundeUrl(src, thema) {
    return `https://${isTest() ? 'staging' : 'www'}.mathenachhilfe-online.com/probestunde/?src=quasselquest-${src}&thema=${encodeURIComponent(thema)}&klasse=${encodeURIComponent(state.klasse || '')}`;
  }
  function lernstandHtml() {
    const name = esc(state.name || 'Euer Kind');
    const rows = [];
    for (const l of PK.ORDER) {
      const L = PK.LANGS[l]; const st = stats(l, 14); const st7 = stats(l, 7);
      const packs = packIndex().filter((p) => p.sprache === l).map((p) => ({ p, m: packMastered(p.id) })).filter((x) => x.m > 0);
      const eigen = (state.eigene[l] || []).map((x) => ({ p: { title: x.name + ' (eigene Liste)', karten: x.words.length }, m: packMastered('eigen-' + x.id) })).filter((x) => x.m > 0);
      const can = l === 'en' ? C.worlds.flatMap((w) => w.episodes || []).filter((e) => e.kann && prog(e.id) && prog(e.id).done).map((e) => e.kann) : [];
      if (!st.n && !packs.length && !eigen.length && !can.length) continue;
      rows.push(`<div class="lern"><h3>${L.emoji} ${esc(L.name)}</h3>
        <p>${st7.n ? `Diese Woche ${st7.n} Antworten, davon ${st7.pct} % beim ersten Versuch richtig.` : 'Diese Woche noch nicht geübt.'}</p>
        ${can.length ? `<p><b>${name} kann jetzt:</b></p><ul>${can.map((x) => `<li>${esc(x)}</li>`).join('')}</ul>` : ''}
        ${packs.concat(eigen).length ? `<p><b>Sitzt schon:</b></p><ul>${packs.concat(eigen).map((x) => `<li>${esc(x.p.title)}: ${x.m} von ${x.p.karten} Karten</li>`).join('')}</ul>` : ''}</div>`);
    }
    return rows.length ? rows.join('') : '<p class="muted">Sobald euer Kind übt, steht hier in Worten, was es schon kann – für jede Sprache.</p>';
  }

  // ================================================================== Quasselquest Pro (PRO.md) – Sperre je Sprache; alles AUS, solange window.PRO_AKTIV nicht true ist
  const SCHLOSS_TEXT = 'Das gehört zu Quasselquest Pro.';   // Recht: in der Kinderansicht NUR dieser Satz – kein Preis, kein Kaufknopf, keine Aufforderung
  const NAME_SPRACHE = { en: 'Englisch', de: 'Deutsch', fr: 'Französisch', la: 'Latein', es: 'Spanisch' };
  /** Ist dieses Ding gesperrt? el = Element mit data-lp-e (Lernpfad-Einheit), data-pack (Paket) oder data-ep (Episode). */
  function proGesperrt(el) {
    if (!window.PRO_AKTIV || !window.PRO) return false;
    let lang = null; let frei = true;
    if (el.dataset.lpE) { const [l, ab, e] = el.dataset.lpE.split('-'); lang = l; frei = +ab < 2 || +e === 1; }                                   // Abschnitt 1 frei, ab 2 je erste Einheit
    else if (el.dataset.pack) { const m = PACK_INDEX.find((x) => x.id === el.dataset.pack); if (!m) return false; lang = m.sprache; const erstes = PACK_INDEX.find((x) => x.sprache === lang); frei = /grundwortschatz/.test(m.id) || (erstes && erstes.id === m.id); }
    else if (el.dataset.ep) { lang = 'en'; frei = /e1$/.test(el.dataset.ep); }                                                                    // erste Episode jeder Welt
    else return false;
    return !frei && !window.PRO.darf(lang);
  }
  function proHinweis() {
    const bg = h(`<div class="modal-bg" style="position:fixed;inset:0;background:rgba(0,0,0,.45);display:grid;place-items:center;z-index:9000"><div class="card center" style="max-width:320px"><div style="font-size:42px">🔒</div><p style="font-weight:800;font-size:20px">${SCHLOSS_TEXT}</p><button class="btn blue" data-ok>OK</button></div></div>`);
    document.body.appendChild(bg); bg.querySelector('[data-ok]').onclick = () => { A.sfx('tap'); bg.remove(); };
  }
  if (window.PRO_AKTIV) {
    document.addEventListener('click', (ev) => { const t = ev.target.closest && ev.target.closest('[data-lp-e],[data-pack],[data-ep]'); if (t && proGesperrt(t)) { ev.preventDefault(); ev.stopPropagation(); A.sfx('soft'); proHinweis(); } }, true);
    const schloss = () => document.querySelectorAll('[data-lp-e],[data-pack],[data-ep]').forEach((t) => { const zu = proGesperrt(t); if (zu && !t.dataset.proZu) { t.dataset.proZu = '1'; t.classList.add('pro-zu'); t.insertAdjacentHTML('beforeend', '<span class="pro-schloss" aria-hidden="true">🔒</span>'); } });
    new MutationObserver(schloss).observe(document.body, { childList: true, subtree: true });
    // Link …/#pro=CODE oder ?pro=CODE: nicht in der Adresszeile lassen, sondern für den Elternbereich merken
    const m = /[#?&]pro=([A-Za-z0-9-]+)/.exec(location.hash + location.search);
    if (m) { try { sessionStorage.setItem('qq-prolink', m[1]); history.replaceState(null, '', location.pathname); } catch (e) {} }
    window.addEventListener('familie-bereit', () => { if (window.FAMILIE) window.FAMILIE.sperrlisteLaden(); });
  }
  const PRO_FEHLER = { form: 'Das ist kein gültiger Code. Bitte noch einmal prüfen.', signatur: 'Dieser Code ist nicht gültig.', fremd: 'Dieser Code gehört zu einer anderen App.', gesperrt: 'Dieser Code wurde gesperrt (zum Beispiel nach einer Erstattung).', speicher: 'Der Code konnte auf diesem Gerät nicht gespeichert werden.' };
  function proKarte(box) {
    if (!box) return; const P = window.PRO; let gemerkt = ''; try { gemerkt = sessionStorage.getItem('qq-prolink') || ''; } catch (e) {}
    const frei = [...P.sprachenFrei()].map((l) => NAME_SPRACHE[l]).filter(Boolean);
    box.innerHTML = `<p class="muted">${frei.length ? 'Freigeschaltet: <b>' + esc(frei.join(', ')) + '</b>' : 'Noch nicht freigeschaltet. Kostenlos bleiben der Grundwortschatz, die erste Episode, der erste Lernpfad-Abschnitt und die erste Einheit jedes weiteren Abschnitts.'}</p>
      <div class="row"><input class="input" data-code autocomplete="off" spellcheck="false" placeholder="Code, z. B. QQ-…" value="${esc(gemerkt)}" style="font-size:16px"><button class="btn small blue" data-los>Einlösen</button></div><p class="muted" data-pmeld aria-live="polite"></p>`;
    const meld = (t, gut) => { const m = box.querySelector('[data-pmeld]'); m.textContent = t; m.style.color = gut ? '' : '#b3261e'; };
    box.querySelector('[data-los]').onclick = async () => {
      A.sfx('tap'); const test = window.FAMILIE ? (nr) => window.FAMILIE.codeGesperrt(nr) : null;
      const r = await P.einloesen(box.querySelector('[data-code]').value, test); if (!r.ok) { meld(PRO_FEHLER[r.grund] || 'Das hat nicht geklappt.'); return; }
      try { sessionStorage.removeItem('qq-prolink'); } catch (e) {} A.sfx('right'); proKarte(box);
    };
  }
  // ================================================================== Familien-Speicher (Elternbereich, hinter dem Tor; Recht A6/V8)
  const FAM_TEXT = 'Eure Lernstände werden verschlüsselt auf unserem Server in Deutschland gespeichert. Der Schlüssel liegt auf euren Geräten. Damit wir euch helfen können, falls ihr den Familien-Zugang verliert, hinterlegen wir zusätzlich eine verschlüsselte Notfall-Kopie. Sie lässt sich nur mit einem getrennt und offline aufbewahrten Schlüssel von Mathenachhilfe Online öffnen. Technisch könnten wir damit eure Lernstände lesen; wir tun das nicht und nutzen ihn nur, wenn ihr uns um Wiederherstellung bittet. Jede Wiederherstellung seht ihr hier im Protokoll.';
  const FAM_SICHER = 'Wer den Familien-Link oder den QR-Code hat, kann die Lernstände eurer Familie öffnen, ändern und löschen. Teilt ihn nur mit Personen, denen ihr vertraut, und bewahrt das Notfall-Blatt sicher auf.';
  const FAM_LOESCHEN = 'Das löscht die Familie in ALLEN Lern-Apps: alle Lernstände aller Kinder auf unserem Server, die Notfall-Kopie und das Protokoll. Gekaufte Codes werden wieder frei und lassen sich in einer neuen Familie erneut einlösen. Die Lernstände auf diesem Gerät bleiben erhalten.';
  const FAM_DSE = 'https://www.mathenachhilfe-online.com/datenschutz/#familien-speicher';
  const famFehler = (r) => ({ netz: 'Keine Verbindung. Der Lernstand bleibt auf diesem Gerät.', 'familie-weg': 'Diese Familie gibt es nicht mehr. Lernstand bleibt auf diesem Gerät.', 'noch-nicht-live': 'Der Familien-Speicher ist noch nicht freigeschaltet.',
    'zu-oft': 'Zu viele Anfragen – bitte später noch einmal.', 'zu-gross': 'Der Lernstand ist zu groß zum Sichern.', schreibrecht: 'Dieses Gerät darf hier nichts ändern (falscher Link?).', schluessel: 'Der Familien-Link passt nicht zu den gespeicherten Daten.', ungueltig: 'Das ist kein gültiger Familien-Link.', 'gibt-es-schon': 'Diese Familie gibt es schon.', nummer: 'Die Familien-Nummer stimmt nicht.' }[r && r.grund] || 'Das hat nicht geklappt. Bitte später noch einmal versuchen.');
  /** Fremden Stand zusammengeführt übernehmen (Felder des Formats v3), ohne neues Sichern auszulösen. */
  function famUebernehmen(stand) {
    if (!stand) return;
    for (const k of ['karten', 'tage', 'lp', 'progress', 'eigene', 'week', 'hinweis', 'tests', 'introSeen']) if (stand[k] !== undefined) state[k] = stand[k];
    mem = state; try { localStorage.setItem(KEY, JSON.stringify(state)); } catch (e) {}
  }
  let famLetzteZeit = 0; let famTimer = null;
  /** Automatisch sichern: nach jeder Runde, aber höchstens alle 2 Minuten; sofort, wenn die App in den Hintergrund geht. */
  function famPlan(sofort) {
    if (!window.FAMILIE_AKTIV || !window.FAMILIE || !window.FAMILIE.gekoppelt()) return;
    clearTimeout(famTimer);
    famTimer = setTimeout(async () => { famLetzteZeit = Date.now(); const r = await window.FAMILIE.sichern(state); if (r.ok && r.stand && r.stand !== state) famUebernehmen(r.stand); }, sofort ? 0 : Math.max(1500, 120000 - (Date.now() - famLetzteZeit)));
  }
  async function famStart() {
    if (!window.FAMILIE_AKTIV || !window.FAMILIE || !window.FAMILIE.gekoppelt()) return;
    const r = await window.FAMILIE.laden(state); if (r.ok && r.stand) famUebernehmen(r.stand);
  }
  if (window.FAMILIE_AKTIV) {
    document.addEventListener('visibilitychange', () => { if (document.visibilityState === 'hidden') famPlan(true); });
    window.addEventListener('familie-bereit', () => famStart());
    if (window.FAMILIE) famStart();
    // Familien-Link geöffnet: nicht in der Adresszeile lassen, sondern für den Elternbereich merken
    if (/^#familie=/.test(location.hash)) { try { sessionStorage.setItem('qq-famlink', location.href); history.replaceState(null, '', location.pathname + location.search); } catch (e) {} }
  }
  function famKopieren(text) { try { if (navigator.clipboard) return navigator.clipboard.writeText(text).then(() => true, () => false); } catch (e) {} return Promise.resolve(false); }
  function familieKarte(box) {
    if (!box) return;
    const F = window.FAMILIE; if (!F) { box.innerHTML = '<p class="muted">Wird geladen …</p>'; window.addEventListener('familie-bereit', () => familieKarte(box), { once: true }); return; }
    const meld = (t, gut) => { const m = box.querySelector('[data-fmeld]'); if (m) { m.textContent = t; m.style.color = gut ? '' : '#b3261e'; } };
    const st = F.status();
    if (!st.gekoppelt) {
      let gemerkt = ''; try { gemerkt = sessionStorage.getItem('qq-famlink') || ''; } catch (e) {}
      box.innerHTML = `<p class="muted">Mit dem Familien-Speicher übt euer Kind auf mehreren Geräten mit demselben Stand weiter – und nichts geht verloren, wenn ein Gerät kaputtgeht. Ohne Kopplung bleibt alles wie bisher nur auf diesem Gerät.</p>
        <div class="row"><button class="btn small blue" data-neu>Familie anlegen</button><button class="btn small white" data-beitritt>Familie beitreten</button></div><div data-fbereich></div><p class="muted" data-fmeld aria-live="polite"></p>`;
      const zone = box.querySelector('[data-fbereich]');
      const einwilligung = (knopf, inhalt) => `<div class="card" style="margin-top:10px"><p class="muted" data-fpflicht>${FAM_TEXT}</p><p class="muted">Eure eigenen Wörterlisten („Mein Schulbuch“) werden dabei auch verschlüsselt mitgesichert.</p><p class="muted"><a href="${FAM_DSE}" target="_blank" rel="noopener">Mehr dazu in der Datenschutzerklärung (Abschnitt 10m)</a></p>${inhalt || ''}<div class="row"><button class="btn small blue" data-ok>${knopf}</button><button class="btn small white" data-abbruch>Abbrechen</button></div></div>`;
      box.querySelector('[data-neu]').onclick = () => {
        A.sfx('tap'); zone.innerHTML = einwilligung('Einverstanden – Familie anlegen');
        zone.querySelector('[data-abbruch]').onclick = () => { zone.innerHTML = ''; };
        zone.querySelector('[data-ok]').onclick = async () => { meld('Familie wird angelegt …', true); const r = await F.anlegen(state); if (!r.ok) { meld(famFehler(r)); return; } A.sfx('right'); if (r.stand && r.stand !== state) famUebernehmen(r.stand); familieKarte(box); };
      };
      box.querySelector('[data-beitritt]').onclick = () => {
        A.sfx('tap'); zone.innerHTML = einwilligung('Einverstanden – Familie beitreten', `<input class="input" data-flink autocomplete="off" spellcheck="false" placeholder="Familien-Link einfügen" value="${esc(gemerkt)}" style="font-size:16px">`);
        zone.querySelector('[data-abbruch]').onclick = () => { zone.innerHTML = ''; try { sessionStorage.removeItem('qq-famlink'); } catch (e) {} };
        zone.querySelector('[data-ok]').onclick = async () => {
          const v = await F.beitretenVorbereiten(zone.querySelector('[data-flink]').value); if (!v.ok) { meld(famFehler(v)); return; }
          const kinder = v.kinder || [];
          zone.innerHTML = `<div class="card" style="margin-top:10px"><h3>Wer übt hier?</h3>${kinder.map((k, i) => `<label class="radio"><input type="radio" name="fkind" value="${esc(k.kindId)}" ${i === 0 ? 'checked' : ''}><span><b>${esc(k.name || 'Profil ' + (i + 1))} ${esc(k.farbe || '')}</b></span></label>`).join('')}
            <label class="radio"><input type="radio" name="fkind" value="" ${kinder.length ? '' : 'checked'}><span><b>Neues Profil in dieser Familie</b></span></label><div class="row"><button class="btn small blue" data-weiter>Weiter</button></div></div>`;
          zone.querySelector('[data-weiter]').onclick = async () => {
            const w = zone.querySelector('input[name=fkind]:checked'); meld('Stand wird geladen …', true);
            const r = await F.beitreten(v, w && w.value ? w.value : null, state); if (!r.ok) { meld(famFehler(r)); return; }
            famUebernehmen(r.stand); try { sessionStorage.removeItem('qq-famlink'); } catch (e) {} A.sfx('right');
            if (r.abweichungen && r.abweichungen.length) alert('Dieses Gerät hatte auch Einstellungen oder eine Insel, die sich unterscheiden. Hier bleibt der Stand dieses Geräts.');
            familieKarte(box);
          };
        };
      };
      if (gemerkt) box.querySelector('[data-beitritt]').click();
      return;
    }
    box.innerHTML = `<p><b>Gekoppelt</b> · Familien-Nummer <b>${esc(st.nummer)}</b>${st.kindName ? ' · ' + esc(st.kindName) : ''}</p>
      <p class="muted">${st.letzte ? 'Zuletzt gesichert: ' + esc(new Date(st.letzte).toLocaleString('de-DE')) : 'Noch nicht gesichert.'}</p>
      <p class="muted" data-fsicher>${FAM_SICHER}</p>
      <div class="row"><button class="btn small blue" data-jetzt>Jetzt sichern</button><button class="btn small white" data-link>Link teilen</button><button class="btn small white" data-blatt>Notfall-Blatt</button><button class="btn small white" data-del>🗑️ Familie löschen</button></div>
      <pre class="muted" data-fblatt hidden style="white-space:pre-wrap"></pre><p class="muted" data-fmeld aria-live="polite"></p>`;
    box.querySelector('[data-jetzt]').onclick = async () => { A.sfx('tap'); meld('Wird gesichert …', true); const r = await F.sichern(state); if (!r.ok) { meld(famFehler(r)); if (r.grund === 'familie-weg') setTimeout(() => familieKarte(box), 1500); return; } if (r.stand !== state) famUebernehmen(r.stand); famLetzteZeit = Date.now(); familieKarte(box); };
    box.querySelector('[data-link]').onclick = async () => { A.sfx('tap'); const ok = await famKopieren(F.link()); const p = box.querySelector('[data-fblatt]'); p.hidden = false; p.textContent = F.link() + '\n\n' + FAM_SICHER; meld(ok ? 'Link kopiert.' : 'Bitte den Link markieren und kopieren.', true); };
    box.querySelector('[data-blatt]').onclick = () => { A.sfx('tap'); const b = F.notfallBlatt(); const p = box.querySelector('[data-fblatt]'); p.hidden = false; p.textContent = b.text + '\n\n' + FAM_SICHER; };
    box.querySelector('[data-del]').onclick = async () => {
      A.sfx('tap'); if (!confirm(FAM_LOESCHEN)) return;
      const n = prompt('Zum Bestätigen die Familien-Nummer eintippen (' + st.nummer + '):'); if (n === null) return;
      const r = await F.loeschen(n); if (!r.ok) { meld(famFehler(r)); return; } familieKarte(box);
    };
  }
  /** Offline-Download je Sprache (offline.js): Größe, Fortschritt, Löschen. */
  async function offlineKarte(box) {
    const O = window.OFFLINE; if (!box) return;
    if (!O || !O.unterstuetzt()) { box.innerHTML = '<p class="muted">Dieser Browser kann keine Sprachen zum Offline-Lernen speichern.</p>'; return; }
    const mb = (b) => b < 1e6 ? 'unter 1 MB' : 'ca. ' + Math.round(b / 1e5) / 10 + ' MB';
    const zeilen = [];
    for (const l of ['en', 'fr', 'es', 'la', 'de']) {
      const L = PK.LANGS[l]; const b = await O.groesse(l);
      if (l === 'en') { zeilen.push(`<div class="set"><span>${L.emoji} ${esc(L.name)}<br><small class="muted" style="font-weight:600">Immer offline (${mb(b)})</small></span><span>✅</span></div>`); continue; }
      const ton = ['fr', 'es'].includes(l) ? '' : ' – nur Texte, ohne Ton';
      zeilen.push(`<div class="set" data-ol="${l}"><span>${L.emoji} ${esc(L.name)}<br><small class="muted" style="font-weight:600" data-st>${O.geladen(l) ? 'Auf diesem Gerät gespeichert' : 'Zum Laden: ' + mb(b) + ton}</small><span class="progress" hidden style="display:block;margin-top:6px"><i style="width:0%"></i></span></span>
        <span class="row"><button class="btn small ${O.geladen(l) ? 'white' : 'blue'}" data-act>${O.geladen(l) ? 'Wieder löschen' : 'Für offline laden'}</button></span></div>`);
    }
    box.innerHTML = zeilen.join('');
    box.querySelectorAll('[data-ol]').forEach((row) => {
      const l = row.dataset.ol; const btn = row.querySelector('[data-act]'); const st = row.querySelector('[data-st]'); const bar = row.querySelector('.progress'); let laeuft = false;
      btn.onclick = async () => {
        A.sfx('tap');
        if (laeuft) { O.abbrechen = true; return; }
        if (O.geladen(l)) { await O.loeschen(l); offlineKarte(box); return; }
        laeuft = true; btn.textContent = 'Abbrechen'; bar.hidden = false; const fill = bar.querySelector('i');
        try {
          await O.laden(l, (f, g) => { fill.style.width = Math.round(100 * f / Math.max(1, g)) + '%'; st.textContent = `Lädt … ${f} von ${g} Dateien`; });
          A.sfx('right');
        } catch (e) { st.textContent = 'Das Laden hat nicht geklappt (Internet?). Bitte später noch einmal versuchen.'; laeuft = false; btn.textContent = 'Für offline laden'; return; }
        offlineKarte(box);
      };
    });
  }
  function parents() {
    const s = state.settings;
    const hin = nachhilfeHinweis();
    if (hin) zaehlen('nachhilfe_hinweis', 'elternbericht', hin.thema);
    const ka = arbeitHinweis();
    if (ka) zaehlen('nachhilfe_hinweis', 'klassenarbeit', ka.thema);
    const sw = (key, label, text) => `<div class="set"><span>${label}<br><small class="muted" style="font-weight:600">${text}</small></span><button class="switch ${s[key] !== false ? 'on' : ''}" data-sw="${key}" role="switch" aria-checked="${s[key] !== false}" aria-label="${label}"></button></div>`;
    const r = (v, label, hint) => `<label class="radio"><input type="radio" name="rec" value="${v}" ${s.recognition === v ? 'checked' : ''}><span><b>${label}</b><br><span class="muted">${hint}</span></span></label>`;
    const el = screen(`<div>
      <div class="top"><button class="iconbtn" data-back aria-label="Zurück">←</button><div class="title" style="font-size:22px">👪 Für Eltern</div></div>
      ${hin ? `<div class="card hint-card"><p><b>${esc(PK.LANGS[hin.l].name)}</b> fällt ${esc(state.name || 'eurem Kind')} gerade schwer (${hin.st.pct} % richtig in den letzten zwei Wochen). Üben in der App hilft – manchmal braucht es aber eine Erklärung von einem Menschen. Wenn ihr mögt: Wir bieten Online-Nachhilfe an.</p>
        <div class="row"><a class="btn small blue" data-probe href="${probestundeUrl('elternbericht', hin.thema)}" target="_blank" rel="noopener">Kostenlose Probestunde anfragen</a><button class="btn small white" data-hide>Ausblenden</button></div></div>` : ''}
      ${ka ? `<div class="card hint-card"><p>Am <b>${esc(datumText(ka.t.date))}</b> ist die Arbeit in <b>${esc(PK.LANGS[ka.l].name)}</b>. Falls ihr noch Unterstützung wollt: Eine kostenlose Probestunde kann vor der Arbeit helfen.</p>
        <div class="row"><a class="btn small blue" data-probe-ka href="${probestundeUrl('klassenarbeit', ka.thema)}" target="_blank" rel="noopener">Kostenlose Probestunde anfragen</a><button class="btn small white" data-hide-ka>Ausblenden</button></div></div>` : ''}
      <div class="card"><h2 style="font-size:20px">Lernstand</h2><p class="muted">Klasse ${state.klasse || '–'} · diese Woche ${weekCount()} Runden (Wochenziel ${WEEK_GOAL})</p>${lernstandHtml()}</div>
      <div class="card" style="margin-top:14px"><h3>Mikrofon und Spracherkennung</h3>
        <p class="muted">Das Mikrofon geht nur an, wenn euer Kind auf 🎤 tippt. Beim Nachsprechen (Papagei-Echo) bleibt die Aufnahme auf dem Gerät und wird sofort wieder gelöscht.
        In der Sprech-Mission kann eine Spracherkennung prüfen, ob der Satz verstanden wurde. <b>Standard: nur auf dem Gerät</b> – die Stimme eures Kindes verlässt das Gerät nicht.</p>
        ${r('device', 'Nur auf dem Gerät (empfohlen)', 'Erkennung nur, wenn der Browser sie auf dem Gerät kann (z. B. Chrome ab Version 139). Sonst wird nur geprüft, ob laut genug gesprochen wurde.')}
        ${r('off', 'Aus', 'Keine Spracherkennung; nur Aufnahme und Lautstärke auf dem Gerät.')}
        ${r('browser', 'Auch Online-Erkennung des Browsers erlauben', 'Nur mit eurer Zustimmung: Je nach Browser wird die Aufnahme des gesprochenen Satzes an den Sprachdienst des Browser-Herstellers gesendet (z. B. Google bei Chrome, Apple bei Safari). Wir selbst erhalten nichts.')}
        <p class="muted" style="font-size:14px">Jetzt aktiv: <b data-mode></b></p>
        <p class="muted" style="font-size:14px" data-hint></p></div>
      ${window.AUSWERTUNG && window.AUSWERTUNG.aktiv() ? `<div class="card" style="margin-top:14px" data-auswertung><h3>🎯 Aussprache-Auswertung</h3>
        <p class="muted">Beim Nachsprechen kann die App prüfen, ob der Satz verstanden wurde, und Sterne geben. Ihr entscheidet, wie – die Wahl könnt ihr jederzeit ändern. Deutsch und Latein werden nie ausgewertet.</p>
        <label class="radio" style="opacity:.5"><input type="radio" name="ausw" value="geraet" disabled><span><b>Im Gerät</b><br><span class="muted">Die Aufnahme bleibt auf dem Gerät. Auf diesem Gerät noch nicht verfügbar.</span></span></label>
        <label class="radio"><input type="radio" name="ausw" value="server" ${window.AUSWERTUNG.wahl() === 'server' ? 'checked' : ''}><span><b>Über unseren Server in Deutschland</b><br><span class="muted">Kein Download. Die Aufnahme (höchstens 15 Sekunden) wird nur gesendet, wenn euer Kind nachspricht, sofort ausgewertet und gelöscht. Es wird nichts gespeichert und nichts mitgeschickt, was euer Kind erkennbar macht.</span></span></label>
        <label class="radio"><input type="radio" name="ausw" value="aus" ${window.AUSWERTUNG.wahl() === 'aus' ? 'checked' : ''}><span><b>Aus</b><br><span class="muted">Keine Auswertung, keine Sterne fürs Nachsprechen. Aufnehmen und selbst Vergleichen bleibt möglich.</span></span></label></div>` : ''}
      ${window.PRO_AKTIV ? '<div class="card" style="margin-top:14px" data-pro-karte><h3>⭐ Quasselquest Pro</h3><div data-pro></div></div>' : ''}
      ${window.FAMILIE_AKTIV ? '<div class="card" style="margin-top:14px" data-familie-karte><h3>👪 Familie – Lernstand sichern</h3><div data-fam></div></div>' : ''}
      <div class="card" style="margin-top:14px"><h3>📥 Offline lernen</h3>
        <p class="muted">Ohne Internet üben: Ihr könnt eine Sprache einmalig auf dieses Gerät laden – <b>am besten im WLAN</b>, denn es sind einige Megabyte. Englisch ist immer offline. Geladene Sprachen aktualisieren sich bei neuen Vertonungen von selbst, und ihr könnt sie jederzeit wieder löschen.</p>
        <div data-offline></div></div>
      <div class="card" style="margin-top:14px"><h3>Einstellungen für Eltern</h3>
        ${sw('nachhilfe', 'Hinweise auf unsere Nachhilfe zeigen', 'Nur hier im Elternbereich, wenn eine Sprache länger schwerfällt – höchstens alle 14 Tage, ausblendbar.')}
        ${sw('zaehlung', 'Anonyme Nutzungszählung', 'Hilft uns, die App zu verbessern: Gezählt wird nur, dass die App oder der Elternbereich geöffnet bzw. ein Link angetippt wurde – ohne Namen, Kennung, Cookies oder Lernstand.')}</div>
      <div class="card" style="margin-top:14px"><h3>Kommt bald</h3>
        <p class="muted">Eltern-Handy koppeln mit verschlüsselter Sicherung (immer kostenlos).</p></div>
      <div class="card" style="margin-top:14px"><h3>Daten</h3><p class="muted">Alles wird nur in diesem Browser gespeichert. Kein Konto, keine Werbung, kein Tracking.</p>
        <div class="row"><a class="btn small white" href="datenschutz.html">Datenschutz</a><a class="btn small white" href="datenblatt-schule.html">Datenblatt für Schulen</a><button class="btn small white" data-reset>🗑️ Alle Daten löschen</button></div></div>
      <div class="foot">Version ${esc(window.APP_VERSION)} vom ${esc(window.APP_VERSION_DATE)}</div>
    </div>`);
    el.querySelectorAll('[data-sw]').forEach((b) => b.onclick = () => { s[b.dataset.sw] = s[b.dataset.sw] === false; save(); A.sfx('tap'); b.classList.toggle('on', s[b.dataset.sw] !== false); b.setAttribute('aria-checked', String(s[b.dataset.sw] !== false)); });
    const probe = el.querySelector('[data-probe]'); if (probe) probe.onclick = () => zaehlen('probestunde_klick', 'elternbericht', hin.thema);
    const hide = el.querySelector('[data-hide]'); if (hide) hide.onclick = () => { state.hinweis[hin.thema] = PK.today(); save(); A.sfx('tap'); parents(); };
    const pka = el.querySelector('[data-probe-ka]'); if (pka) pka.onclick = () => zaehlen('probestunde_klick', 'klassenarbeit', ka.thema);
    const hka = el.querySelector('[data-hide-ka]'); if (hka) hka.onclick = () => { state.hinweis['ka-' + ka.l] = ka.t.date; save(); A.sfx('tap'); parents(); };
    const modeTxt = () => ({ device: 'Erkennung auf dem Gerät', browser: 'Erkennung des Browsers', none: 'keine Erkennung (nur Lautstärke)' }[A.recognitionMode()]);
    const hint = () => {
      const local = A.recognitionMode && (() => { const SR = window.SpeechRecognition || window.webkitSpeechRecognition; return !!SR && 'processLocally' in SR.prototype; })();
      el.querySelector('[data-hint]').textContent = local ? 'Dieses Gerät kann Sprache auf dem Gerät erkennen – die Stimme eures Kindes bleibt hier.'
        : 'Auf diesem Gerät (zum Beispiel iPad oder iPhone mit Safari) gibt es die Aussprache-Prüfung nur mit der Online-Erkennung des Herstellers (Apple bzw. Google). Ohne eure Erlaubnis prüft Quasselquest nur, ob laut genug gesprochen wurde – üben kann euer Kind trotzdem alles.';
    };
    el.querySelector('[data-mode]').textContent = modeTxt(); hint();
    el.querySelectorAll('input[name=rec]').forEach((x) => x.onchange = () => {
      if (x.value === 'browser' && !confirm('Online-Erkennung erlauben?\n\nDann kann der Browser die Aufnahme des gesprochenen Satzes an den Sprachdienst seines Herstellers senden (z. B. Google oder Apple). Ihr könnt das jederzeit wieder ausschalten.')) {
        el.querySelector(`input[name=rec][value="${s.recognition}"]`).checked = true; return;
      }
      s.recognition = x.value; save(); el.querySelector('[data-mode]').textContent = modeTxt();
    });
    offlineKarte(el.querySelector('[data-offline]'));
    if (window.FAMILIE_AKTIV) familieKarte(el.querySelector('[data-fam]'));
    if (window.PRO_AKTIV) proKarte(el.querySelector('[data-pro]'));
    el.querySelectorAll('input[name=ausw]').forEach((x) => x.onchange = () => { s.auswertung = x.value; save(); A.sfx('tap'); });
    el.querySelector('[data-back]').onclick = () => { A.sfx('tap'); settings(); };
    el.querySelector('[data-reset]').onclick = () => {
      if (!confirm('Wirklich alle Daten (Fortschritt, Name, Einstellungen) aus diesem Browser löschen?')) return;
      try { localStorage.removeItem(KEY); } catch (e) {}
      state = fresh(); mem = null; window.APP_SETTINGS = state.settings; onboarding();
    };
  }

  // ================================================================== Wörter & Regeln: Pakete
  let PACK_INDEX = [];
  const packCache = new Map();
  const packIndex = () => PACK_INDEX;
  const packsReady = fetch('pakete/index.json').then((r) => r.json()).then((j) => { PACK_INDEX = j.pakete || []; }).catch(() => {});
  async function loadPack(meta) {
    if (packCache.has(meta.id)) return packCache.get(meta.id);
    const p = await (await fetch('pakete/' + meta.id + '.json')).json();
    p.lang = meta.sprache; packCache.set(meta.id, p);
    return p;
  }
  function packMastered(id) {
    let n = 0; const pre = id + '#';
    for (const k in state.karten) if (k.startsWith(pre) && state.karten[k].b >= 3) n++;
    return n;
  }

  async function openPack(meta) {
    if (!meta) return home();
    let pack;
    try { pack = await loadPack(meta); } catch (e) { rubyToast('Das Paket lädt gerade nicht. Bist du offline? Versuch es gleich noch einmal.'); return; }
    if (!state.introSeen[pack.id] && pack.intro && pack.intro.length) return packIntro(pack);
    return packRound(pack);
  }
  function rubyToast(text) { const r = $app.querySelector('[data-rubytext]'); if (r) rubySay($app, esc(text), 'talk'); }

  function packIntro(pack) {
    let i = 0;
    const el = screen(`<div class="stage">
      <div class="ep-top"><button class="iconbtn" data-quit aria-label="Zurück">✕</button><div class="game-title" style="flex:1">${pack.emoji} ${esc(pack.title)}<small>${esc(pack.description)}</small></div></div>
      <div data-box></div><div class="row center"><button class="btn blue" data-next>Weiter ➜</button></div>${ruby('', '')}</div>`);
    const show = () => {
      const it = pack.intro[i];
      el.querySelector('[data-box]').innerHTML = `<div class="scene-pic">${esc(it.img || pack.emoji)}</div><div class="card" style="font-size:19px;font-weight:700;line-height:1.45">${esc(it.text)}</div>`;
      rubySay(el, `${i + 1} von ${pack.intro.length}`);
    };
    el.querySelector('[data-quit]').onclick = () => { A.sfx('tap'); home(); };
    el.querySelector('[data-next]').onclick = () => {
      A.sfx('whoosh'); i++;
      if (i >= pack.intro.length) { state.introSeen[pack.id] = 1; save(); packRound(pack); } else show();
    };
    show();
  }

  /** Eine Runde: 8 Karten, fällige zuerst. Jede Karte in einer passenden Spielart, gemischt; einmal Memory. */
  async function packRound(pack) {
    const L = PK.LANGS[pack.lang];
    const cards = PK.pick(state.karten, pack, 8, 5);
    const run = { first: 0, total: 0 };
    // Spielarten verteilen: abwechslungsreich, aber passend zur Karte
    let lastMode = '';
    const plan = [];
    const memo = cards.filter((c) => ['zu', 'von'].includes(PK.classify(c, pack.lang).kind));
    let memoryUsed = false;
    for (let i = 0; i < cards.length; i++) {
      const c = cards[i];
      if (!memoryUsed && i >= 3 && memo.filter((x) => !plan.some((p) => p.cards && p.cards.includes(x)) && cards.indexOf(x) >= i).length >= 3) {
        const trio = memo.filter((x) => cards.indexOf(x) >= i).slice(0, 3);
        if (trio.includes(c)) { plan.push({ mode: 'memory', cards: trio }); memoryUsed = true; continue; }
      }
      if (plan.some((p) => p.cards && p.cards.includes(c))) continue;
      const ms = PK.modes(c, pack.lang, pack);
      const pickM = ms.filter((m) => m !== lastMode);
      const m = (pickM.length ? pickM : ms)[Math.floor(Math.random() * (pickM.length ? pickM.length : ms.length))];
      lastMode = m; plan.push({ mode: m, card: c });
    }
    for (let i = 0; i < plan.length; i++) {
      const res = await packStep(pack, L, plan[i], i / plan.length, run);
      if (res === 'quit') { save(); return home(); }
    }
    const t = state.tests[pack.lang]; if (t && t.packs.includes(pack.id)) { t.done = t.done || {}; t.done[PK.today()] = 1; }
    const goal = noteEpisodeDone(); save();
    packSummary(pack, run, goal);
  }

  function packFrame(pack, frac, title, sub) {
    const el = screen(`<div class="stage">
      <div class="ep-top"><button class="iconbtn" data-quit aria-label="Beenden">✕</button>
        <div class="progress"><i style="width:${Math.round(frac * 100)}%"></i></div><span class="pill">${pack.emoji}</span></div>
      <div class="game-title">${title}<small>${sub}</small></div>
      <div data-body class="stage" style="min-height:0"></div>${ruby('', '')}${PK.LANGS[pack.lang].who ? '<div class="ki-hinweis">🤖 KI-generierte Stimme</div>' : ''}</div>`);
    requestAnimationFrame(() => { const i = el.querySelector('.progress > i'); if (i) i.style.width = Math.round(frac * 100 + 8) + '%'; });
    return el;
  }

  function packStep(pack, L, step, frac, run) {
    return new Promise((resolve) => {
      let tk = token;   // wird nach jedem packFrame (neuer Bildschirm) neu gesetzt
      const say = (t) => (t && L.who ? speak(t, L.who) : Promise.resolve(false));
      const done = (cardsFirst) => {
        for (const [c, ok] of cardsFirst) { PK.note(state.karten, pack, c, ok); tally(pack.lang, ok); run.total++; if (ok) run.first++; }
        save(); setTimeout(() => { if (alive(tk)) resolve('ok'); }, 1300);
      };
      const praise = () => ['Super!', 'Richtig!', 'Klasse!', 'Stark!', 'Genau!'][Math.floor(Math.random() * 5)];
      const showInfo = (el, c, extra) => rubySay(el, `${praise()} ${extra || ''}${c.info ? '<br><small style="font-weight:600">💡 ' + esc(c.info) + '</small>' : ''}`, 'dance');

      if (step.mode === 'memory') {
        const el = packFrame(pack, frac, '🧠 Memory', 'Finde die Paare: Wort und Bedeutung!'); tk = token;
        el.querySelector('[data-quit]').onclick = () => resolve('quit');
        const items = [];
        step.cards.forEach((c, n) => { const k = PK.classify(c, pack.lang); items.push({ n, t: k.ziel, ziel: true, c, k }, { n, t: k.de, c, k }); });
        const tiles = shuffle(items);
        const body = el.querySelector('[data-body]');
        body.innerHTML = `<div class="memory">${tiles.map((x, i) => `<button class="mem" data-i="${i}"${DIAG ? ` data-pair="${x.n}"` : ''}><span class="back">${pack.emoji}</span><span class="front">${esc(x.t)}</span></button>`).join('')}</div>`;
        let open = []; let found = 0; const miss = new Set();
        body.querySelectorAll('.mem').forEach((b) => b.onclick = async () => {
          if (open.length >= 2 || b.classList.contains('up')) return;
          A.sfx('tap'); b.classList.add('up'); open.push(b);
          if (open.length < 2) return;
          const [x, y] = open.map((o) => tiles[+o.dataset.i]);
          if (x.n === y.n) {
            open.forEach((o) => o.classList.add('ok')); open = []; found++; A.sfx('right'); A.vibrate(20);
            await say(x.k.sprich);
            if (found === step.cards.length) { confetti(50); rubySay(el, 'Alle Paare gefunden! 🎉', 'dance'); done(step.cards.map((c, n) => [c, !miss.has(n)])); }
          } else {
            miss.add(x.n); miss.add(y.n); A.sfx('soft');
            setTimeout(() => { open.forEach((o) => o.classList.remove('up')); open = []; }, 900);
          }
        });
        rubySay(el, 'Dreh zwei Karten um. Passen sie zusammen?');
        return;
      }

      if (['neu', 'regel', 'sprechen', 'geschichte', 'wimmel', 'tpr', 'lehrer'].includes(step.mode)) return lpSchritt(pack, L, step, frac, resolve, say, run);
      const c = step.card; const k = PK.classify(c, pack.lang);
      if (step.mode === 'zug') {
        const el = packFrame(pack, frac, '🚂 Satz-Zug', 'Bau den Satz Wort für Wort!'); tk = token;
        el.querySelector('[data-quit]').onclick = () => resolve('quit');
        const body = el.querySelector('[data-body]');
        const teile = PK.zugTeile(c.a);
        const andere = pack.cards.filter((x) => x.satz != null && x.id.endsWith('z') && x.a !== c.a).flatMap((x) => PK.zugTeile(x.a)).filter((t) => !teile.includes(t));
        const extra = shuffle([...new Set(andere)]).slice(0, teile.length > 5 ? 1 : 2);
        const tiles = shuffle(teile.map((t, i) => ({ t, i })).concat(extra.map((t) => ({ t, i: -1 }))));
        body.innerHTML = `<div class="qcard"><div class="qimg">${esc(c.img || pack.emoji)}</div><div class="q">${esc(k.de || c.q)}</div></div>
          <div class="zug" data-zug><span class="lok">🚂</span></div>
          <div class="tiles">${tiles.map((x, i) => `<button class="tile wort" data-t="${i}"${DIAG ? ` data-pos="${x.i}"` : ''}>${esc(x.t)}</button>`).join('')}</div>`;
        let pos = 0; let fehler = 0;
        body.querySelectorAll('.tile').forEach((b) => b.onclick = async () => {
          if (b.disabled || pos >= teile.length) return;
          const x = tiles[+b.dataset.t];
          if (x.t === teile[pos]) {
            b.disabled = true; b.classList.add('used'); A.sfx('pop');
            const w = document.createElement('span'); w.className = 'waggon'; w.textContent = x.t; body.querySelector('[data-zug]').appendChild(w); pos++;
            if (pos === teile.length) {
              body.querySelector('[data-zug]').classList.add('fahrt'); A.sfx('right'); A.vibrate(25); confetti(35);
              body.querySelectorAll('.tile').forEach((y) => y.disabled = true);
              showInfo(el, c, `<b>${esc(c.a)}</b>`); await say(k.sprich || c.a); done([[c, fehler === 0]]);
            }
          } else {
            fehler++; b.classList.remove('shake'); void b.offsetWidth; b.classList.add('shake'); A.sfx('soft');
            if (fehler === 2) rubySay(el, `Tipp: Als Nächstes kommt <b>${esc(teile[pos])}</b>.`, 'talk');
          }
        });
        rubySay(el, 'Welches Wort kommt zuerst?');
        return;
      }
      if (step.mode === 'ballon') {
        const el = packFrame(pack, frac, '🎈 Ballon-Jagd', 'Tipp den richtigen Ballon an!'); tk = token;
        el.querySelector('[data-quit]').onclick = () => resolve('quit');
        const body = el.querySelector('[data-body]');
        const opts = shuffle([c.a].concat(PK.wrongs(c, pack, 3)));
        const cols = ['#e53935', '#1e88e5', '#43a047', '#fb8c00', '#8e24aa'];
        body.innerHTML = `<div class="qcard"><div class="qimg">${esc(c.img || pack.emoji)}</div><div class="q">${esc(c.q)}</div>
          ${k.vorher && L.who ? '<button class="slowbtn" data-hear aria-label="Anhören">🔊</button>' : ''}</div>
          <div class="balloons">${opts.map((o, i) => `<button class="balloon" style="--c:${cols[i % cols.length]};--d:${(i * 0.37).toFixed(2)}s" data-o="${i}"${DIAG && o === c.a ? ' data-ok' : ''}>${esc(o)}</button>`).join('')}</div>`;
        let first = true;
        const hb = body.querySelector('[data-hear]'); if (hb) hb.onclick = () => { A.sfx('tap'); say(k.vorher); };
        if (k.vorher) say(k.vorher);
        body.querySelectorAll('.balloon').forEach((b) => b.onclick = async () => {
          if (b.disabled) return;
          if (opts[+b.dataset.o] === c.a) {
            b.classList.add('pop'); A.sfx('pop'); A.vibrate(25); confetti(30);
            body.querySelectorAll('.balloon').forEach((x) => x.disabled = true);
            showInfo(el, c, `<b>${esc(c.a)}</b>`);
            await say(k.sprich); done([[c, first]]);
          } else {
            first = false; b.classList.add('wrong'); b.disabled = true; A.sfx('soft');
            rubySay(el, warum(c, pack.lang, opts[+b.dataset.o]), 'talk');
          }
        });
        rubySay(el, 'Welcher Ballon passt?');
        return;
      }

      if (step.mode === 'hoeren') {
        const el = packFrame(pack, frac, '🔍 Hör-Detektiv', 'Hör zu: Was bedeutet das?'); tk = token;
        el.querySelector('[data-quit]').onclick = () => resolve('quit');
        const body = el.querySelector('[data-body]');
        const opts = shuffle([k.de].concat(PK.wrongs(c, pack, 3, 'de')));
        body.innerHTML = `<div class="listen-row"><button class="speaker" data-play aria-label="Anhören">🔊</button><button class="slowbtn" data-slow aria-label="Langsam">🐢</button></div>
          ${A.canHear(k.sprich, L.who) ? '' : `<div class="novoice">${esc(k.sprich)}<small>Keine Stimme verfügbar – lies das Wort.</small></div>`}
          <div class="opts">${opts.map((o, i) => `<button class="opt" data-o="${i}"${DIAG && o === k.de ? ' data-ok' : ''}>${esc(o)}</button>`).join('')}</div>`;
        const play = async (slow) => { const b = body.querySelector('[data-play]'); b.classList.add('playing'); await speak(k.sprich, L.who, slow ? { slow: true } : null); if (alive(tk)) b.classList.remove('playing'); };
        body.querySelector('[data-play]').onclick = () => { A.sfx('tap'); play(); };
        body.querySelector('[data-slow]').onclick = () => { A.sfx('tap'); play(true); };
        let first = true;
        body.querySelectorAll('.opt').forEach((b) => b.onclick = () => {
          if (b.disabled) return;
          if (opts[+b.dataset.o] === k.de) {
            b.classList.add('sel'); A.sfx('right'); A.vibrate(25); confetti(25);
            body.querySelectorAll('.opt').forEach((x) => x.disabled = true);
            showInfo(el, c, `<b>${esc(k.ziel)}</b> = ${esc(k.de)}`); done([[c, first]]);
          } else { first = false; b.classList.add('no'); b.disabled = true; A.sfx('soft'); rubySay(el, 'Hör noch mal ganz genau hin! 👂', 'talk'); play(true); }
        });
        rubySay(el, 'Was hörst du?');
        setTimeout(() => { if (alive(tk)) play(); }, 300);
        return;
      }

      if (step.mode === 'bauen') {
        const t = PK.bauteile(c, pack.lang); const bf = PK.bauFrage(c, pack.lang) || { text: c.q, hoer: null };
        const hoer = k.kind === 'diktat' ? k.vorher : bf.hoer;
        const el = packFrame(pack, frac, '🔤 Buchstaben-Bau', 'Bau das Wort Buchstabe für Buchstabe!'); tk = token;
        el.querySelector('[data-quit]').onclick = () => resolve('quit');
        const body = el.querySelector('[data-body]');
        const letters = [...t.wort];
        const extraPool = 'aeinrstlodmuhgk';
        const extra = []; while (extra.length < 2) { const x = extraPool[Math.floor(Math.random() * extraPool.length)]; if (!letters.includes(x)) extra.push(x); }
        const tiles = shuffle(letters.map((l, i) => ({ l, i })).concat(extra.map((l) => ({ l, i: -1 }))));
        body.innerHTML = `<div class="qcard"><div class="qimg">${esc(c.img || pack.emoji)}</div><div class="q">${esc(k.kind === 'diktat' ? c.q : bf.text)}</div>
          ${hoer && L.who && A.canHear(hoer, L.who) ? '<button class="speaker" data-hear aria-label="Anhören">🔊</button>' : (hoer ? merkBox(c.a) : '')}</div>
          <div class="slots">${t.vor ? `<span class="pre">${esc(t.vor.trim())}</span>` : ''}${letters.map((_, i) => `<span class="slot" data-s="${i}"></span>`).join('')}</div>
          <div class="tiles">${tiles.map((x, i) => `<button class="tile" data-t="${i}"${DIAG ? ` data-pos="${x.i}"` : ''}>${esc(x.l)}</button>`).join('')}</div>`;
        let pos = 0; let mistakes = 0;
        const hb = body.querySelector('[data-hear]'); if (hb) { hb.onclick = () => { A.sfx('tap'); say(hoer); }; say(hoer); }
        body.querySelectorAll('.tile').forEach((b) => b.onclick = async () => {
          if (b.disabled || pos >= letters.length) return;
          const x = tiles[+b.dataset.t];
          if (x.l === letters[pos]) {
            b.disabled = true; b.classList.add('used'); A.sfx('pop');
            const sl = body.querySelector(`[data-s="${pos}"]`); sl.textContent = x.l; sl.classList.add('full'); pos++;
            if (pos === letters.length) {
              A.sfx('right'); A.vibrate(25); confetti(35); body.querySelector('.slots').classList.add('done');
              body.querySelectorAll('.tile').forEach((y) => y.disabled = true);
              showInfo(el, c, `<b>${esc(c.a)}</b>`); await say(k.sprich || c.a); done([[c, mistakes === 0]]);
            }
          } else {
            mistakes++; b.classList.remove('shake'); void b.offsetWidth; b.classList.add('shake'); A.sfx('soft');
            if (mistakes === 2) rubySay(el, `Tipp: Der nächste Buchstabe ist <b>${esc(letters[pos])}</b>.`, 'talk');
          }
        });
        rubySay(el, 'Welcher Buchstabe kommt zuerst?');
      }
    });
  }

  // ------------------------------------------------------------------ Lernpfad „Neu lernen“ (LERNPFAD.md)
  const LP = {};
  function lernpfad(lang) {
    if (!PK.LERNPFAD.includes(lang)) return Promise.resolve(null);
    if (!LP[lang]) LP[lang] = fetch('lernpfad/' + lang + '.json').then((r) => { if (!r.ok) throw new Error('lp'); return r.json(); }).catch(() => { delete LP[lang]; return null; });
    return LP[lang];
  }
  const lpDone = (e) => state.lp[e.id] || 0;
  const lpEinheiten = (lp) => lp.abschnitte.flatMap((a) => a.einheiten);
  function lpPfadZeigen(el, box, lp) {
    const n = PK.lektionenFuer(lp.sprache).length;
    let offen = true;
    box.innerHTML = `<div class="section-h">🗺️ ${esc(lp.titel)}</div>` + lp.abschnitte.map((a) => `<div class="lp-abschnitt">
      <div class="lp-kopf"><span class="e">${esc(a.emoji)}</span><div><b>Abschnitt ${esc(a.id)}: ${esc(a.titel)}</b><small>${esc(a.ziel)}</small></div></div>
      <div class="lp-weg">${a.einheiten.map((e, i) => {
        const d = Math.min(lpDone(e), n); const fertig = d >= n; const jetzt = offen && !fertig; const zu = !offen;
        if (!fertig) offen = false;
        return `<button class="lp-knoten${fertig ? ' fertig' : ''}${jetzt ? ' jetzt' : ''}${zu ? ' zu' : ''}" style="--x:${[0, -1, 0, 1][i % 4]};--p:${(d / n).toFixed(2)}" data-lp-e="${esc(e.id)}" aria-label="${esc(e.titel)}">
          <span class="ring"><span class="kern">${zu ? '🔒' : esc(e.emoji)}</span></span>${fertig ? '<span class="krone">👑</span>' : ''}
          <span class="lbl">${esc(e.titel)}</span>${jetzt ? `<span class="los">${d ? 'Weiter' : 'Start'}</span>` : ''}</button>`;
      }).join('')}</div></div>`).join('') + `<button class="inselknopf" data-insel>🏝️ <b>Meine Insel</b><small>${inselZahl(lp)} Wörter wohnen schon dort</small></button>` + '<p class="muted lp-mehr">Weitere Abschnitte (Mein Alltag, Unterwegs, Erlebnisse) kommen bald.</p>';
    box.querySelector('[data-insel]').onclick = () => { A.sfx('pop'); insel(lp); };
    box.querySelectorAll('[data-lp-e]').forEach((b) => b.onclick = () => {
      const e = lpEinheiten(lp).find((x) => x.id === b.dataset.lpE);
      if (b.classList.contains('zu')) { A.sfx('soft'); b.classList.remove('wiggle'); void b.offsetWidth; b.classList.add('wiggle'); rubySay(el, 'Diese Einheit öffnet sich, wenn du die davor geschafft hast. Du schaffst das! 💪', 'talk'); return; }
      A.sfx('pop'); A.vibrate(20); einheitSeite(lp, e);
    });
  }
  const einheitFertig = (lp) => (e) => lpDone(e) >= PK.lektionenFuer(lp.sprache).length;
  const inselZahl = (lp) => PK.inselDinge(lp, einheitFertig(lp)).length;
  /** Meine Insel: alle Wörter fertiger Einheiten als Dinge – frei verschieben, antippen = hören. Ruby stellt Suchfragen. */
  function insel(lp) {
    const L = PK.LANGS[lp.sprache]; const dinge = PK.inselDinge(lp, einheitFertig(lp));
    const pos = state.insel[lp.sprache] = state.insel[lp.sprache] || {};
    const raster = PK.inselPlaetze(dinge.length);
    dinge.forEach((d, i) => { if (!pos[d.id]) pos[d.id] = raster[i]; });
    const hoehe = Math.max(420, Math.ceil(dinge.length / 6) * 78 + 80);
    const el = screen(`<div>
      <div class="top"><button class="iconbtn" data-back aria-label="Zurück">←</button><div class="title" style="font-size:24px">🏝️ Meine Insel</div><div class="spacer"></div>${dinge.length >= 4 ? '<button class="btn small blue" data-such>🔍 Ruby fragt</button>' : ''}</div>
      ${ruby(dinge.length ? 'Das ist deine Insel! Jedes Wort, das du gelernt hast, wohnt hier. Zieh die Dinge, wohin du willst – und tipp sie an!' : 'Deine Insel ist noch leer. Schaff deine erste Einheit im Lernpfad – dann ziehen die Wörter hier ein! 🌱')}
      <div class="insel" data-insel style="height:${hoehe}px">${dinge.map((d) => `<button class="iding${pos[d.id].x > 66 ? ' rechts' : (pos[d.id].x < 34 ? ' links' : '')}" data-id="${esc(d.id)}" style="left:${pos[d.id].x}%;top:${pos[d.id].y}%">${esc(d.img)}<span class="wlabel" hidden><b>${esc(d.z)}</b><small>${esc(d.de)}</small></span></button>`).join('')}</div></div>`);
    el.querySelector('[data-back]').onclick = () => { A.sfx('tap'); save(); home(); };
    const feld = el.querySelector('[data-insel]'); let suche = null;
    el.querySelectorAll('.iding').forEach((b) => {
      const d = dinge.find((x) => x.id === b.dataset.id);
      b.onpointerdown = (ev) => {
        ev.preventDefault(); const r = feld.getBoundingClientRect(); let moved = false; const sx = ev.clientX; const sy = ev.clientY;
        const mv = (e2) => { if (Math.abs(e2.clientX - sx) + Math.abs(e2.clientY - sy) < 6) return; moved = true;
          const x = Math.max(4, Math.min(96, (e2.clientX - r.left) / r.width * 100)); const y = Math.max(6, Math.min(94, (e2.clientY - r.top) / r.height * 100));
          b.style.left = x + '%'; b.style.top = y + '%'; pos[d.id] = { x: Math.round(x), y: Math.round(y) }; };
        const up = () => {
          window.removeEventListener('pointermove', mv); window.removeEventListener('pointerup', up);
          if (moved) { A.sfx('pop'); save(); return; }
          if (suche) { if (d.id === suche.id) { A.sfx('right'); confetti(25); rubySay(el, `Super! <b>${esc(d.z)}</b> 🎉`, 'dance'); suche = null; setTimeout(() => { if (alive(tok)) frage(); }, 1300); } else { A.sfx('soft'); rubySay(el, 'Nein, such weiter! 👀', 'talk'); if (L.who) speak(PK.sprechbar(suche.z), L.who); } return; }
          A.sfx('tap'); b.classList.remove('hupf'); void b.offsetWidth; b.classList.add('hupf');
          el.querySelectorAll('.iding .wlabel').forEach((x) => x.hidden = true); const lab = b.querySelector('.wlabel'); lab.hidden = false; setTimeout(() => { if (alive(tok)) lab.hidden = true; }, 2000);
          if (L.who) speak(PK.sprechbar(d.z), L.who);
        };
        window.addEventListener('pointermove', mv); window.addEventListener('pointerup', up);
      };
    });
    const tok = token; let n = 0;
    const frage = () => { if (n >= 5) { rubySay(el, 'Du kennst deine Insel super! 🏆', 'dance'); A.sfx('fanfare'); confetti(60); n = 0; return; } n++; suche = dinge[Math.floor(Math.random() * dinge.length)]; rubySay(el, `Wo ist … 🔊 (${n}/5)`, 'talk'); if (L.who) speak(PK.sprechbar(suche.z), L.who); else rubySay(el, `Wo ist <b>${esc(suche.z)}</b>? (${n}/5)`, 'talk'); };
    const sb = el.querySelector('[data-such]'); if (sb) sb.onclick = () => { A.sfx('pop'); n = 0; frage(); };
  }
  function einheitSeite(lp, e) {
    const L = PK.LANGS[lp.sprache]; const ls = PK.lektionenFuer(lp.sprache); const d = lpDone(e);
    const el = screen(`<div>
      <div class="top"><button class="iconbtn" data-back aria-label="Zurück">←</button><div class="title" style="font-size:24px">${esc(e.emoji)} ${esc(e.titel)}</div></div>
      ${ruby(`<b>Dein Ziel:</b> ${esc(e.ziel)}`)}
      <div class="lp-lektionen">${ls.map((l, k) => { const st = k < d ? 'fertig' : (k === d ? 'jetzt' : 'zu');
        return `<button class="lp-lektion ${st}" data-l="${k}"${st === 'zu' ? ' disabled' : ''}><span class="e">${k < d ? '✅' : (st === 'zu' ? '🔒' : l.e)}</span><span class="t"><b>${k + 1}. ${esc(l.t)}</b><small>${esc(l.sub)}</small></span>${st === 'jetzt' ? '<span class="go">▶</span>' : ''}</button>`; }).join('')}</div>
      <div class="section-h">Die Wörter dieser Einheit</div>
      <div class="lp-woerter">${e.woerter.map(([z, de, img], i) => `<button class="lp-wort" data-w="${i}"><span class="b">${esc(img)}</span><b>${esc(z)}</b><small>${esc(de)}</small></button>`).join('')}</div>
    </div>`);
    el.querySelector('[data-back]').onclick = () => { A.sfx('tap'); home(); };
    el.querySelectorAll('.lp-lektion').forEach((b) => b.onclick = () => { if (b.disabled) return; A.sfx('pop'); A.vibrate(20); lektionStarten(lp, e, +b.dataset.l); });
    el.querySelectorAll('.lp-wort').forEach((b) => b.onclick = () => {
      A.sfx('tap'); const w = e.woerter[+b.dataset.w]; if (L.who) speak(PK.sprechbar(w[0]), L.who);
      b.classList.remove('wiggle'); void b.offsetWidth; b.classList.add('wiggle');
    });
  }
  async function lektionStarten(lp, e, k) {
    const lang = lp.sprache; const L = PK.LANGS[lang]; const l = PK.lektionenFuer(lang)[k];
    const pack = PK.lernPack(e, lang); pack.lp = lp;
    const steps = PK.lektion(pack, l.nr);
    // Wiederholung: bis zu 2 fällige Wörter aus früheren Einheiten einstreuen (gemischtes Üben)
    const faellig = [];
    for (const x of lpEinheiten(lp)) {
      if (x.id === e.id || !lpDone(x)) continue;
      const p = PK.lernPack(x, lang);
      for (const c of p.cards) { const st = state.karten[PK.key(p, c)]; if (st && st.d <= PK.today() && c.wort != null) faellig.push({ p, c }); }
    }
    shuffle(faellig).slice(0, 2).forEach(({ p, c }, i) => steps.splice(Math.min(steps.length, 2 + i * 4), 0, { mode: 'ballon', card: c, pack: p }));
    const run = { first: 0, total: 0 };
    for (let i = 0; i < steps.length; i++) {
      const res = await packStep(steps[i].pack || pack, L, steps[i], i / steps.length, run);
      if (res === 'quit') { save(); return einheitSeite(lp, e); }
    }
    const vorher = lpDone(e); if (k + 1 > vorher) state.lp[e.id] = k + 1;
    const goal = noteEpisodeDone(); save();
    lektionFertig(lp, e, k, run, goal, vorher);
  }
  function lektionFertig(lp, e, k, run, goal, vorher) {
    const ls = PK.lektionenFuer(lp.sprache); const n = ls.length;
    const krone = k + 1 === n && vorher < n;
    const q = run.total ? run.first / run.total : 1; const sterne = q >= 0.9 ? 3 : (q >= 0.6 ? 2 : 1);
    const el = screen(`<div class="reward">
      ${ruby(krone ? `Du hast die Einheit „${esc(e.titel)}“ geschafft! Hier ist deine Krone! 👑` : `Lektion „${esc(ls[k].t)}“ geschafft!${run.total ? ` ${run.first} von ${run.total} gleich beim ersten Mal richtig.` : ''}${goal ? ' Und dein Wochenziel ist erreicht! 🏆' : ''}`, 'dance')}
      <div class="stamp">${krone ? '👑' : ls[k].e}</div>
      <div class="bigstars">${[0, 1, 2].map((x) => `<span style="animation-delay:${0.5 + x * 0.25}s">${x < sterne ? '⭐' : '☆'}</span>`).join('')}</div>
      ${krone ? `<div class="card center"><b>Das kannst du jetzt:</b><br>${esc(e.ziel)}<br><br>🏝️ ${e.woerter.length} neue Bewohner ziehen auf deine Insel!</div>` : ''}
      <div class="row center"><button class="btn white" data-unit>Zur Einheit</button><button class="btn blue" data-next>${k + 1 < n ? 'Nächste Lektion ➜' : 'Zum Lernpfad 🗺️'}</button></div></div>`);
    setTimeout(() => { A.sfx(krone ? 'fanfare' : 'stamp'); A.vibrate(40); confetti(krone ? 150 : 70); }, 250);
    el.querySelector('[data-unit]').onclick = () => { A.sfx('tap'); einheitSeite(lp, e); };
    el.querySelector('[data-next]').onclick = () => { A.sfx('pop'); if (k + 1 < n) lektionStarten(lp, e, k + 1); else home(); };
  }
  /** Spielarten des Lernpfads ohne Karte: Kennenlernen, Regel entdecken, Nachsprechen, Geschichte. */
  function lpSchritt(pack, L, step, frac, resolve, say, run) {
    const e = pack.einheit;
    const titel = { neu: '🌱 Neue Wörter', regel: '💡 Regel entdecken', sprechen: '🎤 Sprich nach!', geschichte: '📖 ' + esc(e.geschichte.titel), wimmel: '🔍 Wimmelbild', tpr: '🖐️ Mach, was ich sage!', lehrer: '🎓 Ruby lernt von dir' }[step.mode];
    const sub = { neu: L.who ? 'Tipp jede Karte an: hören und umdrehen!' : 'Tipp jede Karte an und dreh sie um!', regel: esc(e.regel.titel), sprechen: 'Erst anhören, dann nachsprechen.', geschichte: L.who ? 'Hör zu! Tipp auf eine Sprechblase, dann siehst du die Übersetzung.' : 'Lies mit! Tipp auf eine Sprechblase, dann siehst du die Übersetzung.', wimmel: 'Entdecke alle neuen Wörter im Bild!', tpr: L.who ? 'Hör zu und zieh das Richtige zur richtigen Person!' : 'Lies den Befehl und zieh das Richtige zur richtigen Person!', lehrer: 'Ruby hat einen Fehler gemacht. Findest du ihn?' }[step.mode];
    const el = packFrame(pack, frac, titel, sub); const tk = token;
    el.querySelector('[data-quit]').onclick = () => resolve('quit');
    const body = el.querySelector('[data-body]');
    const weiterKnopf = (txt, hidden) => `<div class="row center"><button class="btn blue" data-weiter${hidden ? ' hidden' : ''}>${txt}</button></div>`;
    const wertung = (ok) => { run.total++; if (ok) run.first++; tally(pack.lang, ok); };
    const figuren = (pack.lp && pack.lp.figuren) || {};

    if (step.mode === 'wimmel') {
      // Entdecken: alle Wörter verstecken sich im Bild. Antippen = hören + Deutsch. Danach: „Find …!“
      const plaetze = PK.wimmelPlaetze(e.woerter.length, e.id.split('').reduce((a, ch) => a + ch.charCodeAt(0), 0));
      const deko = ['🌳', '🌼', '☁️', '🪨', '🌿', '🍄'];
      body.innerHTML = `<div class="wimmel" data-w>${deko.map((d, i) => `<span class="deko" style="left:${(i * 17 + 5) % 92}%;top:${i % 2 ? 6 : 88}%">${d}</span>`).join('')}
        ${e.woerter.map(([z, de, img], i) => `<button class="wding${plaetze[i].x > 66 ? ' rechts' : (plaetze[i].x < 34 ? ' links' : '')}" data-i="${i}" style="left:${plaetze[i].x}%;top:${plaetze[i].y}%;--d:${(i * 0.08).toFixed(2)}s" aria-label="Ding ${i + 1}">${esc(img)}<span class="wlabel" hidden><b>${esc(z)}</b><small>${esc(de)}</small></span></button>`).join('')}</div>
        <div class="wimmel-stand" data-stand>0 von ${e.woerter.length} entdeckt</div>${weiterKnopf('Spiel starten ➜', true)}`;
      const gefunden = new Set(); let phase = 'entdecken'; let ziel = -1; let runde = 0; let erster = true;
      const ziele = shuffle(e.woerter.map((_, i) => i)).slice(0, Math.min(5, e.woerter.length));
      const ohneTon = !L.who; // Latein: Wort lesen statt hören
      const frage = () => { ziel = ziele[runde]; erster = true; if (DIAG) body.querySelector('[data-w]').dataset.ziel = ziel; rubySay(el, ohneTon ? `Wo ist <b>${esc(e.woerter[ziel][0].split(',')[0])}</b>? 👀` : `Find … 🔊`); say(PK.sprechbar(e.woerter[ziel][0])); };
      body.querySelectorAll('.wding').forEach((b) => b.onclick = async () => {
        const i = +b.dataset.i; const [z] = e.woerter[i];
        if (phase === 'entdecken') {
          A.sfx('pop'); A.vibrate(10); b.classList.add('gefunden'); body.querySelectorAll('.wlabel').forEach((x) => x.hidden = true); b.querySelector('.wlabel').hidden = false;
          setTimeout(() => { if (alive(tk)) b.querySelector('.wlabel').hidden = true; }, 2200);
          say(PK.sprechbar(z));
          if (!gefunden.has(i)) {
            gefunden.add(i); body.querySelector('[data-stand]').textContent = `${gefunden.size} von ${e.woerter.length} entdeckt`;
            if (gefunden.size === e.woerter.length) { confetti(40); A.sfx('right'); body.querySelector('[data-weiter]').hidden = false; rubySay(el, ohneTon ? 'Du hast alles entdeckt! 🎉 Jetzt spielen wir: Ich zeige ein Wort – du findest es!' : 'Du hast alles entdeckt! 🎉 Jetzt spielen wir: Ich sage ein Wort – du findest es!', 'dance'); }
          }
          return;
        }
        if (i === ziel) {
          A.sfx('right'); A.vibrate(25); b.classList.remove('hupf'); void b.offsetWidth; b.classList.add('hupf'); confetti(20); wertung(erster);
          rubySay(el, `${['Super!', 'Richtig!', 'Klasse!'][runde % 3]} <b>${esc(z)}</b> = ${esc(e.woerter[i][1])}`, 'dance');
          runde++; if (runde >= ziele.length) { await sleep(1100); if (alive(tk)) resolve('ok'); return; }
          await sleep(1300); if (alive(tk)) frage();
        } else { erster = false; A.sfx('soft'); b.classList.remove('shake'); void b.offsetWidth; b.classList.add('shake'); rubySay(el, ohneTon ? `Lies noch mal: <b>${esc(e.woerter[ziel][0].split(',')[0])}</b> 👀` : 'Hör noch mal! 👂', 'talk'); say(PK.sprechbar(e.woerter[ziel][0]), ); }
      });
      body.querySelector('[data-weiter]').onclick = () => {
        A.sfx('whoosh'); phase = 'finden'; body.querySelector('[data-weiter]').hidden = true; body.querySelector('[data-stand]').textContent = ohneTon ? 'Lies das Wort und tipp auf das Ding!' : 'Hör zu und tipp auf das Wort!';
        body.querySelectorAll('.wlabel').forEach((x) => x.hidden = true); frage();
      };
      el.querySelector('[data-rubytext]') && rubySay(el, `${e.woerter.length} Wörter verstecken sich hier! Tipp alles an, was du siehst.`);
      return;
    }

    if (step.mode === 'tpr') {
      // Mach, was ich sage: Befehl hören, Ding(e) zur richtigen Figur ziehen (oder antippen, dann Figur antippen)
      const a = step.aktion; const n = a.anzahl || 1;
      const leute = Object.entries(figuren).filter(([id]) => !['brown', 'martin', 'garcia', 'ted'].includes(id) || id === a.an);
      const zielLeute = shuffle([leute.find(([id]) => id === a.an)].concat(shuffle(leute.filter(([id]) => id !== a.an)).slice(0, 2)).filter(Boolean));
      const andere = shuffle([...new Set((e.aktionen || []).map((x) => x.ding).concat(e.woerter.map((w) => w[2])).filter((d) => d !== a.ding))]).slice(0, 3);
      const dinge = shuffle(Array.from({ length: n + (n > 1 ? 1 : 0) }, () => a.ding).concat(andere));
      const ohneTon = !L.who; // Latein: Befehl lesen statt hören
      body.innerHTML = `<div class="tpr-befehl">${ohneTon ? '' : '<button class="speaker" data-play aria-label="Anhören">🔊</button><button class="slowbtn" data-slow aria-label="Langsam">🐢</button>'}<span data-text class="tpr-text"${ohneTon ? '' : ' hidden'}>${esc(a.sag)}</span></div>
        <div class="tpr-leute">${zielLeute.map(([id, F]) => `<div class="tpr-person" data-an="${esc(id)}"${DIAG && id === a.an ? ' data-ziel' : ''}><span class="e">${id === 'ruby' ? `<span class="mini-ruby">${rubySVG()}</span>` : esc(F.emoji)}</span><b>${esc(F.name)}</b><span class="hand" data-hand></span></div>`).join('')}</div>
        <div class="tpr-dinge">${dinge.map((d, i) => `<button class="tpr-ding" data-i="${i}" data-d="${esc(d)}"${DIAG && d === a.ding ? ' data-ok' : ''}>${esc(d)}</button>`).join('')}</div>
        ${ohneTon ? '' : '<button class="quiet" data-zeig>Ich habe es nicht verstanden – zeig mir den Satz</button>'}`;
      const play = async (slow) => { if (ohneTon) return; const b = body.querySelector('[data-play]'); b.classList.add('playing'); await speak(a.sag, L.who, slow ? { slow: true } : null); if (alive(tk)) b.classList.remove('playing'); };
      if (!ohneTon) body.querySelector('[data-play]').onclick = () => { A.sfx('tap'); play(); };
      if (!ohneTon) body.querySelector('[data-slow]').onclick = () => { A.sfx('tap'); play(true); };
      if (!ohneTon) body.querySelector('[data-zeig]').onclick = () => { A.sfx('tap'); body.querySelector('[data-text]').hidden = false; body.querySelector('[data-zeig]').hidden = true; erster = false; play(true); };
      let erster = true; let gegeben = 0; let gewaehlt = null; let fertig = false;
      const geben = (btn, an) => {
        if (fertig || !btn || btn.disabled) return;
        const ok = btn.dataset.d === a.ding && an === a.an;
        if (!ok) {
          erster = false; A.sfx('soft'); btn.classList.remove('shake'); void btn.offsetWidth; btn.classList.add('shake');
          rubySay(el, btn.dataset.d !== a.ding ? (ohneTon ? 'Hmm, das passt nicht. Lies noch mal genau! 👀' : 'Hmm, das passt nicht. Hör noch mal genau hin! 👂') : (ohneTon ? 'Das richtige Ding – aber wem sollst du es geben? Schau auf die Endung! 👀' : 'Das richtige Ding – aber zu wem soll es? Hör noch mal! 👂'), 'talk'); play(true); return;
        }
        btn.disabled = true; btn.classList.add('weg'); gegeben++; A.sfx('pop'); A.vibrate(15);
        const hand = body.querySelector(`[data-an="${a.an}"] [data-hand]`); hand.textContent += a.ding; hand.classList.remove('popbig'); void hand.offsetWidth; hand.classList.add('popbig');
        if (gegeben >= n) {
          fertig = true; wertung(erster); A.sfx('right'); confetti(35);
          body.querySelector('[data-text]').hidden = false;
          rubySay(el, `Genau! <b>${esc(a.sag)}</b><br><small>${esc(a.de)}</small>`, 'dance');
          setTimeout(() => { if (alive(tk)) resolve('ok'); }, 2000);
        } else rubySay(el, `${gegeben} … weiter! 😊`, 'talk');
      };
      // Ziehen mit Finger/Maus; Antippen + Person antippen geht auch
      body.querySelectorAll('.tpr-ding').forEach((b) => {
        b.onclick = () => { if (b.disabled) return; body.querySelectorAll('.tpr-ding').forEach((x) => x.classList.remove('gewaehlt')); b.classList.add('gewaehlt'); gewaehlt = b; A.sfx('tap'); };
        b.onpointerdown = (ev) => {
          if (b.disabled) return; ev.preventDefault();
          const r = b.getBoundingClientRect(); const dx = ev.clientX - r.left; const dy = ev.clientY - r.top; let moved = false;
          const ghost = b.cloneNode(true); ghost.className = 'tpr-ding ghost'; ghost.style.left = r.left + 'px'; ghost.style.top = r.top + 'px'; document.body.appendChild(ghost);
          const mv = (e2) => { moved = true; ghost.style.left = (e2.clientX - dx) + 'px'; ghost.style.top = (e2.clientY - dy) + 'px'; };
          const up = (e2) => {
            window.removeEventListener('pointermove', mv); window.removeEventListener('pointerup', up); ghost.remove();
            if (!moved) { b.onclick(); return; }
            const ziel = document.elementsFromPoint(e2.clientX, e2.clientY).find((x) => x.dataset && x.dataset.an);
            if (ziel) geben(b, ziel.dataset.an);
          };
          window.addEventListener('pointermove', mv); window.addEventListener('pointerup', up);
        };
      });
      body.querySelectorAll('.tpr-person').forEach((p) => p.onclick = () => { if (gewaehlt) { geben(gewaehlt, p.dataset.an); gewaehlt && gewaehlt.classList.remove('gewaehlt'); gewaehlt = null; } else { A.sfx('tap'); rubySay(el, 'Erst ein Ding antippen oder ziehen!', 'talk'); } });
      rubySay(el, ohneTon ? 'Lies den Befehl und tu es! 👀' : 'Hör gut zu! 👂');
      if (!ohneTon) setTimeout(() => { if (alive(tk)) play(); }, 300);
      return;
    }

    if (step.mode === 'lehrer') {
      // Ruby lernt von dir: Fehler im Satz finden, richtiges Wort beibringen. Der falsche Satz wird NICHT vorgesprochen.
      const f = step.fehler; const teile = PK.fehlerTeile(f);
      body.innerHTML = `<div class="lehrer-tafel"><span class="tafel-ruby mini-ruby">${rubySVG()}</span><div class="tafel-satz">${teile.map((x, i) => `<button class="tafel-wort" data-i="${i}"${DIAG && x.falsch ? ' data-ok' : ''}>${esc(x.t)}</button>`).join(' ')}</div></div>
        <div class="opts" data-opts hidden></div>`;
      let erster = true; let phase = 'finden';
      body.querySelectorAll('.tafel-wort').forEach((b) => b.onclick = () => {
        if (phase !== 'finden') return;
        if (teile[+b.dataset.i].falsch) {
          phase = 'korrigieren'; A.sfx('pop'); b.classList.add('falsch');
          const opts = shuffle([f.korrektur].concat(f.choices || []));
          const box = body.querySelector('[data-opts]'); box.hidden = false;
          box.innerHTML = opts.map((o, i) => `<button class="opt" data-o="${i}"${DIAG && o === f.korrektur ? ' data-ok' : ''}>${esc(o)}</button>`).join('');
          rubySay(el, 'Oh! Da habe ich mich vertan. Was ist richtig? 🤔', 'talk');
          box.querySelectorAll('.opt').forEach((o) => o.onclick = async () => {
            if (o.disabled) return;
            if (opts[+o.dataset.o] === f.korrektur) {
              phase = 'fertig'; box.querySelectorAll('.opt').forEach((x) => x.disabled = true); o.classList.add('sel');
              b.textContent = b.textContent.replace(f.wort, f.korrektur); b.classList.remove('falsch'); b.classList.add('richtig');
              A.sfx('right'); A.vibrate(25); confetti(40); wertung(erster);
              rubySay(el, `Danke, das merke ich mir! 🎓<br><small>💡 ${esc(f.tipp)}</small>`, 'dance');
              await say(f.richtig); await sleep(1400); if (alive(tk)) resolve('ok');
            } else { erster = false; o.classList.add('no'); o.disabled = true; A.sfx('soft'); rubySay(el, `Hmm, ${esc(opts[+o.dataset.o])}? Ich glaube, das stimmt auch nicht …`, 'talk'); }
          });
        } else { A.sfx('soft'); b.classList.remove('shake'); void b.offsetWidth; b.classList.add('shake'); rubySay(el, `„${esc(b.textContent)}“ lasse ich so. Ich meine ein anderes Wort – such weiter! 🔍`, 'talk'); }
      });
      rubySay(el, 'Ich übe auch! Aber in meinem Satz ist ein Fehler. Tipp auf das falsche Wort!');
      return;
    }

    if (step.mode === 'neu') {
      body.innerHTML = `<div class="neu-grid">${e.woerter.map(([z, de, img], i) => `<button class="neu-karte" data-i="${i}" style="--d:${(i * 0.06).toFixed(2)}s">
        <span class="vorn"><span class="bild">${esc(img)}</span><b>${esc(z)}</b></span><span class="hinten"><span class="bild">${esc(img)}</span><b>${esc(de)}</b></span></button>`).join('')}</div>
        ${weiterKnopf('Weiter ➜')}`;
      const w = body.querySelector('[data-weiter]'); w.disabled = true;
      const gesehen = new Set();
      body.querySelectorAll('.neu-karte').forEach((b) => b.onclick = () => {
        const i = +b.dataset.i; A.sfx('tap'); A.vibrate(10); b.classList.toggle('um'); say(PK.sprechbar(e.woerter[i][0]));
        if (!gesehen.has(i)) { gesehen.add(i); if (gesehen.size === e.woerter.length) { w.disabled = false; w.classList.add('wiggle'); A.sfx('right'); confetti(30); rubySay(el, 'Super! Jetzt kennst du alle neuen Wörter. Los geht\'s! 🎉', 'dance'); } }
      });
      w.onclick = () => { if (w.disabled) return; A.sfx('whoosh'); resolve('ok'); };
      rubySay(el, `${e.woerter.length} neue Wörter! Tipp jede Karte an – ich sage dir, wie man sie spricht.`);
      return;
    }
    if (step.mode === 'regel') {
      const r = e.regel;
      body.innerHTML = `<div class="regel-bsp">${r.beispiele.map(([z, de], i) => `<button class="regel-zeile" data-i="${i}" style="--d:${(i * 0.25).toFixed(2)}s"><b>${esc(z)}</b><small>${esc(de)}</small></button>`).join('')}</div>
        <div class="regel-text" data-text hidden>💡 ${esc(r.text)}</div>
        <div class="row center"><button class="btn white" data-zeig>🤔 Was fällt dir auf? Zeig mir die Regel!</button></div>${weiterKnopf('Verstanden ➜', true)}`;
      body.querySelectorAll('.regel-zeile').forEach((b) => b.onclick = () => { A.sfx('tap'); const z = r.beispiele[+b.dataset.i][0]; if (!z.includes('→')) say(z); });
      body.querySelector('[data-zeig]').onclick = () => {
        A.sfx('pop'); body.querySelector('[data-text]').hidden = false; body.querySelector('[data-zeig]').hidden = true; body.querySelector('[data-weiter]').hidden = false;
        confetti(20); rubySay(el, 'Genau! Schau dir die Beispiele noch einmal an – jetzt siehst du die Regel überall.', 'dance');
      };
      body.querySelector('[data-weiter]').onclick = () => { A.sfx('whoosh'); resolve('ok'); };
      rubySay(el, 'Schau dir die Beispiele an. Tipp darauf zum Anhören. Was fällt dir auf? 🤔');
      return;
    }
    if (step.mode === 'sprechen') {
      const text = PK.sprechbar(step.text);
      body.innerHTML = `<div class="echo-line"><div class="pic">${esc(step.img || pack.emoji)}</div><div class="en">${esc(step.text)}</div><div class="hint-de">${esc(step.de)}</div></div>
        <div class="listen-row"><button class="speaker" data-play aria-label="Anhören">🔊</button><button class="slowbtn" data-slow aria-label="Langsam">🐢</button><button class="mic" data-mic aria-label="Aufnehmen">🎤</button></div>
        <div class="mic-label" data-ml>Erst anhören, dann auf 🎤 tippen und nachsprechen.</div>
        ${weiterKnopf('Weiter ➜', true)}<button class="quiet" data-quiet>Ich sage es leise für mich</button>
        <div class="ai-note">Deine Stimme bleibt auf diesem Gerät.</div>`;
      const mic = body.querySelector('[data-mic]'); const ml = body.querySelector('[data-ml]'); const w = body.querySelector('[data-weiter]'); const quiet = body.querySelector('[data-quiet]');
      const play = async (slow) => { const b = body.querySelector('[data-play]'); b.classList.add('playing'); await speak(text, L.who, slow ? { slow: true } : null); if (alive(tk)) b.classList.remove('playing'); };
      let rec = null; let busy = false; let tries = 0; let fertig = false;
      const finish = (msg, gut) => { if (fertig) return; fertig = true; ml.textContent = ''; rubySay(el, msg, 'dance'); A.sfx('right'); if (gut) confetti(30); w.hidden = false; quiet.hidden = true; };
      mic.onclick = async () => {
        if (fertig) return;
        if (rec) {
          const r = rec; rec = null; mic.classList.remove('on'); mic.textContent = '🎤';
          const res = await r.stop(); if (res.url) URL.revokeObjectURL(res.url); if (!alive(tk)) return;
          const sv = await serverAuswerten(pack.lang, text, res, ml); if (!alive(tk)) return;
          if (sv) {
            tries++;
            if (sv.status === 'ok' && sv.sterne > 0) { finish(`Ich habe dich verstanden: „${esc(sv.text)}“ ⭐`, true); return; }
            if (sv.status !== 'ok' || tries >= 2) { finish(sv.status === 'ok' ? 'Du hast es versucht – weiter geht\'s! 👍' : NICHT_MOEGLICH, false); return; }
            ml.textContent = 'Hör noch einmal zu und versuch es wieder!'; A.sfx('soft'); play(true); return;
          }
          if (res.loudFrames > 6) finish('Gut gemacht – du hast laut mitgesprochen! 👍', false); else { ml.textContent = 'Ich habe fast nichts gehört – noch mal, ruhig lauter!'; A.sfx('soft'); }
          return;
        }
        if (busy) return; busy = true; A.stop(); A.sfx('pop');
        if (!(window.AUSWERTUNG && window.AUSWERTUNG.serverFuer(pack.lang)) && A.recognitionMode() !== 'none') {
          mic.classList.add('on'); ml.textContent = 'Ich höre zu …';
          const alts = await A.listen(pack.lang); mic.classList.remove('on'); busy = false;
          if (!alive(tk)) return;
          if (alts && alts.length) {
            tries++;
            if (A.similarity(text, alts) >= 0.5) { finish(`Ich habe dich verstanden: „${esc(alts[0])}“ ⭐`, true); return; }
            if (tries >= 2) { finish('Das Mikrofon tut sich manchmal schwer – du hast es versucht! Weiter geht\'s. 👍', false); return; }
            ml.textContent = `Ich habe „${alts[0]}“ gehört. Hör noch einmal zu und versuch es wieder!`; A.sfx('soft'); play(true); return;
          }
          if (alts && !alts.length) { tries++; if (tries >= 2) { finish('Weiter geht\'s! 👍', false); return; } ml.textContent = 'Ich hab dich nicht ganz gehört – noch mal?'; A.sfx('soft'); return; }
        }
        busy = false;
        if (!A.micSupported()) { finish('Laut mitsprechen hilft am meisten! 👍', false); return; }
        try {
          rec = await A.record((lvl) => mic.style.setProperty('--lvl', lvl.toFixed(2)));
          if (!alive(tk)) { rec.stop(); rec = null; return; }
          mic.classList.add('on'); mic.textContent = '⏹️'; ml.textContent = 'Sprich jetzt – dann tipp auf ⏹️.';
        } catch (err) { rec = null; ml.textContent = 'Das Mikrofon ist nicht erlaubt. Sprich einfach laut mit und tipp auf „leise für mich“.'; }
      };
      body.querySelector('[data-play]').onclick = () => { A.sfx('tap'); play(); };
      body.querySelector('[data-slow]').onclick = () => { A.sfx('tap'); play(true); };
      quiet.onclick = () => { A.sfx('tap'); finish('Okay! Beim nächsten Mal vielleicht laut? 😊', false); };
      w.onclick = () => { A.sfx('whoosh'); if (rec) { rec.stop(); rec = null; } resolve('ok'); };
      rubySay(el, 'Hör gut zu und sprich mir nach!');
      setTimeout(() => { if (alive(tk)) play(); }, 300);
      return;
    }
    // Geschichte: Sprechblasen erscheinen nacheinander, mit Stimme der Figur; Antippen zeigt die Übersetzung
    const g = e.geschichte; const fig = (pack.lp && pack.lp.figuren) || {};
    body.innerHTML = `<div class="chat" data-chat></div>${weiterKnopf('Weiter ➜')}`;
    const chat = body.querySelector('[data-chat]'); const w = body.querySelector('[data-weiter]');
    let i = 0;
    const zeige = () => {
      const [f, z, de] = g.zeilen[i]; const F = fig[f] || { name: f, emoji: '🙂', who: L.who };
      const div = document.createElement('div'); div.className = 'chat-zeile ' + (f === g.zeilen[0][0] ? 'links' : 'rechts');
      div.innerHTML = `<span class="wer" title="${esc(F.name)}">${f === 'ruby' ? `<span class="mini-ruby">${rubySVG()}</span>` : esc(F.emoji)}</span><button class="blase"><small>${esc(F.name)}</small><b>${esc(z)}</b><span class="de" hidden>${esc(de)}</span></button>`;
      chat.appendChild(div); try { div.scrollIntoView({ behavior: 'smooth', block: 'nearest' }); } catch (err) {}
      div.querySelector('.blase').onclick = () => { A.sfx('tap'); const x = div.querySelector('.de'); x.hidden = !x.hidden; if (F.who) speak(z, F.who); };
      A.sfx('pop'); if (F.who) speak(z, F.who);
      if (i === g.zeilen.length - 1) w.textContent = 'Zu den Fragen ➜';
    };
    w.onclick = () => { i++; if (i >= g.zeilen.length) { A.sfx('whoosh'); resolve('ok'); return; } A.sfx('tap'); zeige(); };
    rubySay(el, L.who ? 'Eine kleine Geschichte! Hör gut zu – danach habe ich zwei Fragen.' : 'Eine kleine Geschichte! Lies gut mit – danach habe ich zwei Fragen.');
    zeige();
  }

  // ------------------------------------------------------------------ Klassenarbeits-Generalprobe (Pflicht 2)
  const dayOf = (iso) => { const [y, m, d] = iso.split('-').map(Number); return Math.floor((Date.UTC(y, m - 1, d)) / 864e5); };
  const isoOf = (day) => new Date(day * 864e5).toISOString().slice(0, 10);
  const datumText = (iso) => new Date(iso + 'T12:00:00').toLocaleDateString('de-DE', { weekday: 'long', day: 'numeric', month: 'long' });
  function testSources(lang) {
    return packIndex().filter((p) => p.sprache === lang).map((p) => ({ id: p.id, title: p.title, emoji: p.emoji }))
      .concat((state.eigene[lang] || []).map((l) => ({ id: 'eigen-' + l.id, title: l.name + ' (eigene Liste)', emoji: '📒' })));
  }
  async function packById(id) {
    if (id.startsWith('eigen-')) { const l = (state.eigene[state.lang] || []).find((x) => 'eigen-' + x.id === id); return l ? PK.listePack(l, state.lang) : null; }
    const meta = packIndex().find((x) => x.id === id); return meta ? loadPack(meta) : null;
  }
  function testAktiv() { const t = state.tests[state.lang]; return !!(t && dayOf(t.date) >= PK.today()); }
  function testCard() {
    const t = state.tests[state.lang];
    if (!t || dayOf(t.date) < PK.today()) return `<button class="testcard" data-test-new>📝 <b>Mein Test ist am …</b><small>Klassenarbeit oder Vokabeltest? Ich mache dir einen Lernplan.</small></button>`;
    const heute = PK.today(); const rest = dayOf(t.date) - heute;
    const plan = PK.lernplan(t.packs, heute, dayOf(t.date));
    const heuteDran = plan[0];
    const src = testSources(state.lang).find((x) => heuteDran && x.id === heuteDran.paket);
    const erledigt = t.done && t.done[heute];
    return `<div class="testcard on"><div>📝 <b>${rest === 0 ? 'Heute ist dein Test – viel Erfolg!' : `Test am ${esc(datumText(t.date))}`}</b>
      <small>${rest > 0 ? `Noch ${rest} ${rest === 1 ? 'Tag' : 'Tage'}. ` : ''}${erledigt ? '✅ Für heute geschafft!' : (heuteDran && heuteDran.probe ? 'Heute: Generalprobe!' : (src ? `Heute dran: ${esc(src.title)}` : ''))}</small></div>
      <div class="row">${src && !erledigt ? `<button class="btn small blue" data-test-ueben="${src.id}">▶ Üben</button>` : ''}<button class="btn small ${heuteDran && heuteDran.probe ? 'blue' : 'white'}" data-test-probe>📝 Generalprobe</button><button class="btn small white" data-test-edit aria-label="Test ändern">✏️</button></div></div>`;
  }
  function testSetup() {
    const t = state.tests[state.lang]; const L = PK.LANGS[state.lang];
    const min = isoOf(PK.today() + 1); const max = isoOf(PK.today() + 60);
    const el = screen(`<div>
      <div class="top"><button class="iconbtn" data-back aria-label="Zurück">←</button><div class="title" style="font-size:22px">📝 Mein Test · ${esc(L.name)}</div></div>
      ${ruby('Wann ist dein Test, und was kommt dran? Ich plane jeden Tag eine kurze Runde – und am Ende eine Generalprobe wie in der Klassenarbeit!')}
      <div class="card"><label style="font-weight:800">Datum</label>
        <input class="input" type="date" data-date min="${min}" max="${max}" value="${esc(t ? t.date : '')}" style="margin:6px 0 12px">
        <div style="font-weight:800;margin-bottom:6px">Was kommt dran?</div>
        <div class="checklist">${testSources(state.lang).map((x) => `<label class="radio"><input type="checkbox" value="${x.id}" ${t && t.packs.includes(x.id) ? 'checked' : ''}><span>${x.emoji} ${esc(x.title)}</span></label>`).join('')}</div>
        <div class="err" data-err></div>
        <div class="row" style="margin-top:8px"><button class="btn blue" data-save>💾 Speichern</button>${t ? '<button class="btn white small" data-del>🗑️ Test löschen</button>' : ''}</div></div></div>`);
    el.querySelector('[data-back]').onclick = () => { A.sfx('tap'); home(); };
    el.querySelector('[data-save]').onclick = () => {
      const date = el.querySelector('[data-date]').value; const packs = [...el.querySelectorAll('.checklist input:checked')].map((x) => x.value);
      const err = !date ? 'Bitte wähle das Datum deines Tests.' : (dayOf(date) <= PK.today() ? 'Der Test sollte mindestens morgen sein.' : (dayOf(date) > PK.today() + 60 ? 'Plane höchstens 60 Tage im Voraus.' : (!packs.length ? 'Wähle mindestens ein Paket oder eine eigene Liste.' : '')));
      if (err) { el.querySelector('[data-err]').textContent = err; A.sfx('soft'); return; }
      state.tests[state.lang] = { date, packs, done: (t && t.done) || {} }; save(); A.sfx('right'); confetti(40); home();
    };
    const del = el.querySelector('[data-del]'); if (del) del.onclick = () => { if (!confirm('Test wirklich löschen?')) return; delete state.tests[state.lang]; save(); home(); };
  }

  /** Generalprobe im Format einer Klassenarbeit: 12 Aufgaben, Antworten tippen (Vokabeln) oder wählen, KEINE Hilfe währenddessen, Auswertung am Ende. */
  async function testProbe() {
    const t = state.tests[state.lang]; if (!t) return testSetup();
    const packs = (await Promise.all(t.packs.map(packById))).filter(Boolean);
    const pool = shuffle(packs.flatMap((p) => p.cards.map((c) => ({ p, c }))));
    const items = pool.slice(0, Math.min(12, pool.length));
    const antworten = [];
    for (let i = 0; i < items.length; i++) {
      const res = await probeStep(items[i], i, items.length);
      if (res === 'quit') return home();
      antworten.push(res);
    }
    // Auswertung: Wiederholung anpassen (falsch → morgen wieder)
    antworten.forEach((r, i) => { PK.note(state.karten, items[i].p, items[i].c, r.ok); tally(state.lang, r.ok); });
    save();
    probeErgebnis(items, antworten);
  }
  function probeStep(item, i, n) {
    return new Promise((resolve) => {
      const { p, c } = item; const k = PK.classify(c, p.lang);
      const bf = p.lang === 'de' && k.kind === 'frage' ? PK.bauFrage(c, p.lang) : null;
      const tippen = ['zu', 'formen', 'luecke', 'diktat'].includes(k.kind) || (!!bf && /^[\p{L} -]{1,30}$/u.test(c.a));
      const hoerProbe = k.kind === 'diktat' || k.kind === 'von' ? k.vorher : (tippen && bf ? bf.hoer : null);
      const el = screen(`<div class="stage">
        <div class="ep-top"><button class="iconbtn" data-quit aria-label="Abbrechen">✕</button><div class="progress"><i style="width:${Math.round(100 * i / n)}%"></i></div><span class="pill">${i + 1}/${n}</span></div>
        <div class="game-title">📝 Generalprobe<small>Wie in der Klassenarbeit: erst am Ende gibt es die Auflösung.</small></div>
        <div class="qcard"><div class="qimg">${esc(c.img || p.emoji)}</div><div class="q">${esc(k.kind === 'diktat' ? (PK.LANGS[p.lang].who ? 'Schreib das Wort, das du hörst.' : 'Merk dir das Wort – dann schreib es auf.') : (tippen && bf ? bf.text : c.q))}</div>
          ${hoerProbe && PK.LANGS[p.lang].who ? '<button class="slowbtn" data-hear aria-label="Anhören">🔊</button>' : (k.kind === 'diktat' ? merkBox(c.a) : '')}</div>
        ${tippen ? `<input class="input" data-in autocomplete="off" autocapitalize="off" spellcheck="false" placeholder="Deine Antwort" style="font-size:22px;text-align:center">
          <button class="btn blue" data-ok>Weiter ➜</button>`
          : `<div class="opts">${shuffle([c.a].concat(PK.wrongs(c, p, 3))).map((o) => `<button class="opt" data-o="${esc(o)}">${esc(o)}</button>`).join('')}</div>`}
        </div>`);
      const L = PK.LANGS[p.lang];
      const hb = el.querySelector('[data-hear]'); if (hb) { hb.onclick = () => { A.sfx('tap'); if (L.who) speak(hoerProbe, L.who); }; if (L.who) speak(hoerProbe, L.who); }
      el.querySelector('[data-quit]').onclick = () => { if (confirm('Generalprobe abbrechen?')) resolve('quit'); };
      if (tippen) {
        const inp = el.querySelector('[data-in]'); setTimeout(() => inp.focus(), 60);
        const go = () => { A.sfx('tap'); const r = PK.pruefeTipp(inp.value, c, p.lang); resolve({ ...r, eingabe: inp.value.trim() }); };
        el.querySelector('[data-ok]').onclick = go; inp.onkeydown = (e) => { if (e.key === 'Enter') go(); };
      } else {
        el.querySelectorAll('.opt').forEach((b) => b.onclick = () => { A.sfx('tap'); resolve({ ok: b.dataset.o === c.a, eingabe: b.dataset.o }); });
      }
    });
  }
  function probeErgebnis(items, antworten) {
    const richtig = antworten.filter((r) => r.ok).length; const n = items.length;
    const el = screen(`<div>
      <div class="top"><div class="title" style="font-size:22px">📝 Auswertung der Generalprobe</div></div>
      ${ruby(richtig === n ? `Alle ${n} richtig – perfekt, du bist bereit! 🏆` : (richtig === 0 ? 'Diesmal hat es noch nicht geklappt – genau dafür ist die Generalprobe da! Was noch nicht sitzt, kommt ab morgen in deinen Runden dran. Du schaffst das! 💪' : `${richtig} von ${n} richtig – gut gemacht! Was noch nicht sitzt, kommt ab morgen in deinen Runden dran.`), 'dance')}
      <div class="card">${items.map(({ p, c }, i) => { const r = antworten[i];
        return `<div class="probe-row ${r.ok ? 'ok' : 'no'}"><div>${r.ok ? '✅' : (r.fast ? '🟡' : '❌')} <b>${esc(c.q)}</b></div>
          ${r.ok ? `<div>${esc(c.a)}</div>` : `<div>Deine Antwort: <s>${esc(r.eingabe || '–')}</s> · Richtig: <b>${esc(c.a)}</b></div><div class="muted" style="font-size:14px">🤔 ${esc(r.hinweis || PK.regel(c, p.lang, r.eingabe || ''))}</div>`}</div>`; }).join('')}</div>
      <div class="row center" style="margin-top:14px"><button class="btn blue" data-home>Fertig 🏠</button></div></div>`);
    setTimeout(() => { A.sfx('fanfare'); confetti(richtig === n ? 120 : 50); }, 200);
    el.querySelector('[data-home]').onclick = () => { A.sfx('tap'); home(); };
  }

  // ------------------------------------------------------------------ „Mein Schulbuch“
  function myLists() {
    const L = PK.LANGS[state.lang]; const lists = state.eigene[state.lang] || [];
    const el = screen(`<div>
      <div class="top"><button class="iconbtn" data-back aria-label="Zurück">←</button><div class="title" style="font-size:22px">📒 Mein Schulbuch · ${esc(L.name)}</div></div>
      ${ruby(state.lang === 'de' ? 'Tipp deine Lernwörter ein – ich mache ein Diktat-Spiel daraus!' : 'Tipp die Wörter aus deinem Schulbuch ein – ich mache Spiele daraus! 🦜')}
      <div class="worlds">${lists.map((l) => `<div class="wtile ptile"><span class="e">📒</span><b>${esc(l.name)}</b><small>${l.words.length} Wörter · ${packMastered('eigen-' + l.id)} sitzen</small>
        <span class="row" style="margin-top:6px"><button class="btn small blue" data-play="${l.id}">▶ Üben</button><button class="btn small white" data-edit="${l.id}">✏️</button></span></div>`).join('')}
        <button class="wtile ptile" data-new><span class="e">➕</span><b>Neue Liste</b><small>z. B. „Lektion 3“</small></button></div>
      <p class="muted" style="font-size:14px;margin-top:12px">Deine Listen bleiben nur auf diesem Gerät. Bitte nur eigene Wörter eintippen, keine ganzen Seiten aus dem Buch abfotografieren oder kopieren.</p>
    </div>`);
    el.querySelector('[data-back]').onclick = () => { A.sfx('tap'); home(); };
    el.querySelector('[data-new]').onclick = () => { A.sfx('tap'); listEditor(null); };
    el.querySelectorAll('[data-edit]').forEach((b) => b.onclick = () => { A.sfx('tap'); listEditor(lists.find((l) => l.id === b.dataset.edit)); });
    el.querySelectorAll('[data-play]').forEach((b) => b.onclick = () => {
      const l = lists.find((x) => x.id === b.dataset.play); A.sfx('pop'); packRound(PK.listePack(l, state.lang));
    });
  }

  function listEditor(liste) {
    const L = PK.LANGS[state.lang]; const de = state.lang === 'de';
    const beispiel = de ? 'Fahrrad\nVogel\nStraße' : { en: 'Hund = dog\nKatze = cat\ndie Schule = school', fr: 'der Hund = le chien\ndie Katze = le chat\ndie Schule = l’école', es: 'der Hund = el perro\ndie Katze = el gato\ndie Schule = la escuela', la: 'lieben = amare\nder Herr = dominus\ndas Wort = verbum' }[state.lang];
    const text = liste ? liste.words.map((w) => (de ? w.ziel : `${w.de} = ${w.ziel}`)).join('\n') : '';
    const el = screen(`<div>
      <div class="top"><button class="iconbtn" data-back aria-label="Zurück">←</button><div class="title" style="font-size:22px">${liste ? '✏️ Liste ändern' : '➕ Neue Liste'}</div></div>
      <div class="card"><label style="font-weight:800">Name der Liste</label>
        <input class="input" data-name maxlength="30" placeholder="z. B. Lektion 3" value="${esc(liste ? liste.name : '')}" style="margin:6px 0 12px">
        <label style="font-weight:800">${de ? 'Ein Lernwort pro Zeile' : `Ein Wort pro Zeile: Deutsch = ${esc(L.name)}`}</label>
        <p class="muted" style="margin:6px 0 0;font-size:14px">Bitte keine Namen oder Adressen als Wörter eintragen.</p>
        <textarea class="input" data-text rows="9" placeholder="${esc(beispiel)}" style="margin-top:6px;font-size:18px;-webkit-user-select:text;user-select:text">${esc(text)}</textarea>
        <div class="err" data-err></div><div class="muted" data-ok style="font-weight:700"></div>
        <div class="row" style="margin-top:8px"><button class="btn blue" data-save>💾 Speichern</button>${liste ? '<button class="btn white small" data-del>🗑️ Löschen</button>' : ''}</div></div>
    </div>`);
    const ta = el.querySelector('[data-text]');
    const check = () => {
      const r = PK.parseListe(ta.value, state.lang);
      el.querySelector('[data-err]').innerHTML = r.fehler.slice(0, 4).map(esc).join('<br>') + (r.fehler.length > 4 ? `<br>… und ${r.fehler.length - 4} weitere` : '');
      el.querySelector('[data-ok]').textContent = r.words.length ? `✅ ${r.words.length} ${r.words.length === 1 ? 'Wort' : 'Wörter'} erkannt` : '';
      return r;
    };
    ta.oninput = check; if (text) check();
    el.querySelector('[data-back]').onclick = () => { A.sfx('tap'); myLists(); };
    el.querySelector('[data-save]').onclick = () => {
      const r = check(); const name = el.querySelector('[data-name]').value.trim() || 'Meine Liste';
      if (r.fehler.length) { A.sfx('soft'); rubyToast('Schau noch mal auf die rot markierten Zeilen.'); return; }
      if (r.words.length < 4) { A.sfx('soft'); el.querySelector('[data-err]').textContent = 'Bitte mindestens 4 Wörter – sonst wird das Spiel zu leicht.'; return; }
      const all = state.eigene[state.lang] = state.eigene[state.lang] || [];
      if (liste) { liste.name = name; liste.words = r.words; } else all.push({ id: Date.now().toString(36), name, words: r.words });
      save(); A.sfx('right'); confetti(40); myLists();
    };
    const del = el.querySelector('[data-del]'); if (del) del.onclick = () => {
      if (!confirm(`Liste „${liste.name}“ wirklich löschen?`)) return;
      state.eigene[state.lang] = (state.eigene[state.lang] || []).filter((l) => l !== liste); save(); myLists();
    };
  }

  /** „Warum?“: falsche Antwort mit markierter Fehlerstelle und Regel in einem Kindersatz (verrät die Lösung nicht). */
  function warum(card, lang, wrong) {
    const d = PK.diff(wrong, card.a);
    const markiert = d.fehlt ? `${esc(d.vor)}<mark class="fehl">␣</mark>${esc(d.nach)}` : `${esc(d.vor)}<mark class="fehl">${esc(d.fehler)}</mark>${esc(d.nach)}`;
    return `Fast! In „${markiert}“ stimmt die markierte Stelle nicht.<br><small style="font-weight:600">🤔 Warum? ${esc(PK.regel(card, lang, wrong))}</small>`;
  }

  function packSummary(pack, run, goal) {
    const stars = run.total ? Math.max(1, Math.round(3 * run.first / run.total)) : 1;
    const el = screen(`<div class="reward">
      ${ruby(`Runde geschafft! ${run.first} von ${run.total} beim ersten Versuch. ${goal ? 'Dein Wochenziel ist erreicht – Squawk! 🏆' : ''}`, 'dance')}
      <div class="stamp" style="border-color:var(--sea);color:var(--sea)">${pack.emoji}</div>
      <div class="bigstars">${[0, 1, 2].map((k) => `<span style="animation-delay:${0.5 + k * 0.25}s">${k < stars ? '⭐' : '☆'}</span>`).join('')}</div>
      <p style="font-weight:800">${packMastered(pack.id)} von ${pack.cards.length} Karten sitzen schon. Wiederholt wird automatisch, wenn es Zeit ist.</p>
      <div class="row center"><button class="btn white" data-again>🔁 Noch eine Runde</button><button class="btn blue" data-home>Fertig 🏠</button></div></div>`);
    setTimeout(() => { A.sfx('fanfare'); confetti(100); }, 250);
    el.querySelector('[data-home]').onclick = () => { A.sfx('tap'); if (pack.eigen) myLists(); else home(); };
    el.querySelector('[data-again]').onclick = () => { A.sfx('tap'); packRound(pack); };
  }

  // ================================================================== Offline + Update
  let updateReady = false;
  function maybeApplyUpdate() {
    if (!updateReady || !navigator.serviceWorker) return;
    navigator.serviceWorker.getRegistration().then((reg) => { if (reg && reg.waiting) reg.waiting.postMessage('skipWaiting'); });
  }
  if ('serviceWorker' in navigator && location.protocol !== 'file:') {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('sw.js').then((reg) => {
        const mark = () => { if (reg.waiting && navigator.serviceWorker.controller) { updateReady = true; if (!$app.querySelector('.stage')) maybeApplyUpdate(); } };
        reg.addEventListener('updatefound', () => { const w = reg.installing; if (w) w.addEventListener('statechange', mark); });
        mark();
        setTimeout(() => { if (window.OFFLINE) window.OFFLINE.abgleichen().then(() => window.OFFLINE.grundausstattung()); }, 8000);   // geladene Sprachen nach einem Update aktualisieren
        // Beim Öffnen und alle 30 Minuten nach einer neuen Version fragen
        setInterval(() => reg.update().catch(() => {}), 30 * 60 * 1000);
      }).catch(() => {});
      // Nur bei einem Update neu laden – nicht beim allerersten Besuch, wenn der Service Worker erstmals übernimmt
      const hadController = !!navigator.serviceWorker.controller;
      let reloaded = false;
      navigator.serviceWorker.addEventListener('controllerchange', () => { if (reloaded || !hadController) return; reloaded = true; location.reload(); });
    });
  }

  if (DIAG) window.quasselTest = { state: () => state, C, A, home, playEpisode };
  // Erst die Liste der vertonten Sätze laden (höchstens 2 Sekunden warten), dann starten
  Promise.race([Promise.all([A.ready, packsReady]), sleep(2500)]).then(() => { try { start(); } catch (e) { showError(e); } });
  function showError(e) {
    $app.innerHTML = '<div class="card" style="margin-top:40px"><b>Ups, da ist etwas schiefgegangen.</b><p>Bitte lade die Seite neu.</p><pre style="white-space:pre-wrap;font-size:12px">' + esc(e && e.stack || e) + '</pre></div>';
  }
})();
