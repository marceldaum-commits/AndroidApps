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
  function save() { mem = state; try { localStorage.setItem(KEY, JSON.stringify(state)); } catch (e) {} }
  const fresh = () => ({ v: 3, klasse: null, name: '', lang: 'en', karten: {}, introSeen: {}, eigene: {}, progress: {}, settings: { sound: true, voice: true, vibrate: true, slow: false, recognition: 'device', stimme: 'frei' }, week: { key: '', n: 0 } });
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
  const PK = window.PAKETE;
  window.APP_SETTINGS = state.settings;

  // ================================================================== Hilfen
  const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const h = (html) => { const t = document.createElement('template'); t.innerHTML = html.trim(); return t.content.firstElementChild; };
  const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
  const shuffle = (a) => { a = a.slice(); for (let i = a.length - 1; i > 0; i--) { const j = Math.floor(Math.random() * (i + 1)); [a[i], a[j]] = [a[j], a[i]]; } return a; };
  const pickOthers = (all, not, n) => shuffle(all.filter((x) => x !== not)).slice(0, n);
  let token = 0; // wechselt bei jedem Bildschirmwechsel – laufende Abläufe brechen dann ab
  function screen(html) { token++; A.stop(); $app.innerHTML = ''; const el = typeof html === 'string' ? h(html) : html; $app.appendChild(el); window.scrollTo(0, 0); return el; }
  const alive = (t) => t === token;
  // Test-Stufe am Hostnamen (Standard UMZUG-LERNEN): „lernen-test.“, Übergang „quasselquest-test.“, lokal.
  const isTest = () => /(^|[.-])(test|staging)([.-]|$)|localhost|127\.0\.0\.1/.test(location.hostname) || location.protocol === 'file:';

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
  function confetti(n) {
    if (!cx) return;
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
    const el = screen(`<div class="stage">
      ${ruby('Hello! Ich bin <b>Ruby</b>, ein Papagei, der viele Sprachen spricht. Papageien sprechen alles nach – und du bald auch! 🦜', 'talk')}
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
    const el = screen(`<div>
      <div class="top"><button class="allapps" data-allapps>← Alle Apps</button><div class="spacer"></div>
        <span class="pill" title="Stempel im Reisepass">📘 ${stampCount()}</span>
        <button class="iconbtn" data-settings aria-label="Einstellungen">⚙️</button></div>
      <div class="title">Quasselquest <span style="font-size:.6em;color:var(--muted)">Sprachen mit Ruby</span></div>
      ${langBar()}
      ${ruby(greet + tip)}
      ${en ? `<div class="world">
        <div class="waves"></div><div class="boatdeco">⛵</div>
        <h2>${w1.emoji} ${esc(w1.title)}</h2><div class="sub">Welt 1 · ${esc(w1.de)}</div>
        <div class="stations">${w1.episodes.map((e) => {
          const p = prog(e.id); const cls = e.soon ? 'soon' : (p && p.done ? 'done' : (e.id === next ? 'next' : ''));
          return `<button class="station ${cls}" data-ep="${e.id}" aria-label="${esc(e.title)}"><span class="dot">${e.soon ? '🔜' : e.emoji}</span>
            <span class="lbl">${esc(e.title)}</span><span class="stars">${p && p.done ? '⭐'.repeat(p.stars) : ''}</span></button>`;
        }).join('')}</div>
      </div>` : ''}
      <div class="row" style="margin-top:14px">${en ? '<button class="btn white small" data-pass>📘 Mein Reisepass</button>' : ''}
        <span class="pill" title="Wochenziel">🎯 Diese Woche: ${Math.min(weekCount(), WEEK_GOAL)}/${WEEK_GOAL}${weekCount() >= WEEK_GOAL ? ' ✅' : ''}</span></div>
      <div class="section-h">${L.emoji} Wörter & Regeln üben</div>
      ${packTiles()}
      ${en ? `<div class="section-h">Weitere Orte auf der Reise</div>
      <div class="worlds">${C.worlds.slice(1).map((w) => `<button class="wtile" data-world="${w.id}"><span class="soon">kommt bald</span><span class="e">${w.emoji}</span><b>${esc(w.title)}</b><small>${esc(w.de)} · ${w.stufe === 'gs' ? 'Kl. 3–4' : 'Kl. 5–6'}</small></button>`).join('')}</div>` : ''}
      <div class="foot">Version ${esc(window.APP_VERSION)} vom ${esc(window.APP_VERSION_DATE)} · <a href="datenschutz.html">Datenschutz</a></div>
    </div>`);
    el.querySelector('[data-allapps]').onclick = openAllApps;
    el.querySelector('[data-settings]').onclick = () => { A.sfx('tap'); settings(); };
    const ps = el.querySelector('[data-pass]'); if (ps) ps.onclick = () => { A.sfx('tap'); passport(); };
    el.querySelectorAll('[data-lang]').forEach((b) => b.onclick = () => {
      if (state.lang === b.dataset.lang) return;
      state.lang = b.dataset.lang; save(); A.sfx('pop'); A.vibrate(15); home();
      const L2 = PK.LANGS[state.lang]; if (L2.who) speak(L2.hallo.replace(/ !$/, '!'), L2.who);
    });
    el.querySelectorAll('[data-pack]').forEach((b) => b.onclick = () => { A.sfx('pop'); A.vibrate(20); openPack(packIndex().find((x) => x.id === b.dataset.pack)); });
    const mine = el.querySelector('[data-mine]'); if (mine) mine.onclick = () => { A.sfx('pop'); myLists(); };
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
      ${inner}</div>`);
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

  function runStep(step, run, frac) {
    if (step.kind === 'scene') return scene(run, frac);
    if (step.kind === 'mission') return mission(run, frac);
    const g = step.g;
    if (g.type === 'detective') return detective(run, frac, g);
    if (g.type === 'echo') return echo(run, frac, g);
    if (g.type === 'train') return train(run, frac, g);
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
      const pool = g.items.filter((x) => x !== last);
      target = pool[Math.floor(Math.random() * pool.length)]; last = target;
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
    const phrase = () => C.rubySaysText(g.mode, target, cmd);
    const play = async () => {
      const b = el.querySelector('[data-play]'); b.classList.add('playing');
      await speak(phrase(), 'ruby');
      if (alive(tk)) b.classList.remove('playing');
    };
    async function next() {
      if (round >= rounds) { el._done('ok'); return; }
      cmd = order[round]; round++;
      target = g.targets[Math.floor(Math.random() * g.targets.length)];
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
        rubySay(el, 'Hihi, reingelegt! 🦜 Ich habe nicht „Ruby says“ gesagt. Macht nichts – weiter!', 'dance');
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
    const line = () => g.lines[i];
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
      rubySay(el, `Satz ${i + 1} von ${g.lines.length}. Sprich mir nach!`);
      play();
    }
    function done(stars, msg) {
      run.max++; if (stars >= 2) run.stars++;
      el.querySelector('[data-st]').innerHTML = [0, 1, 2].map((k) => `<span style="display:inline-block;animation:popbig .5s ${k * .15}s both">${k < stars ? '⭐' : '☆'}</span>`).join('');
      el.querySelector('[data-next]').hidden = false;
      rubySay(el, msg, 'dance'); A.sfx('right'); if (stars >= 2) confetti(30);
    }
    el.querySelector('[data-mic]').onclick = async () => {
      const mic = el.querySelector('[data-mic]'); const ml = el.querySelector('[data-ml]');
      if (!A.micSupported()) { done(2, 'Toll! Wenn du laut nachsprichst, lernst du am meisten. 🎉'); return; }
      if (recording) {
        const r = recording; recording = null; mic.classList.remove('on'); mic.textContent = '🎤';
        const res = await r.stop(); if (!alive(tk)) return;
        lastUrl = res.url;
        const spoke = res.loudFrames > 6 && res.ms > 500;
        ml.textContent = spoke ? 'Hör dir selbst zu: erst Ruby, dann du!' : 'Ich habe fast nichts gehört. Sprich ruhig lauter!';
        if (spoke) {
          el.querySelector('[data-cmp]').hidden = false;
          done(res.loudFrames > 25 ? 3 : 2, 'Squawk! Das klang super! 🎉 Tipp auf „Vergleichen“.');
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
    el.querySelector('[data-quiet]').onclick = () => { A.sfx('tap'); if (el.querySelector('[data-next]').hidden) done(1, 'Okay! Beim nächsten Mal vielleicht laut? 😊'); };
    el.querySelector('[data-next]').onclick = () => {
      A.sfx('whoosh'); if (recording) { recording.stop(); recording = null; }
      if (lastUrl) { try { URL.revokeObjectURL(lastUrl); } catch (e) {} lastUrl = null; }
      i++; if (i >= g.lines.length) el._done('ok'); else show();
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
    const s = () => g.sentences[i];
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
        i++; if (i >= g.sentences.length) el._done('ok'); else show();
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
      rubySay(el, `Zug ${i + 1} von ${g.sentences.length}: Bau den Satz, den du hörst!`);
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
        const finish = (msg) => {
          run.max++; run.stars++;
          box.replaceWith(h(`<div class="me">🗣️ ${esc(text)}</div>`));
          A.sfx('right'); A.vibrate(25); rubySay(el, msg, 'dance'); resolve();
        };
        box.querySelector('[data-hear]').onclick = () => { A.sfx('tap'); speak(text, 'ruby'); };
        box.querySelector('[data-quiet]').onclick = () => { A.sfx('tap'); run.max++; box.replaceWith(h(`<div class="me">🤫 ${esc(text)}</div>`)); resolve(); };
        const mic = box.querySelector('[data-mic]'); const heard = box.querySelector('[data-heard]');
        let busy = false; let recH = null;
        mic.onclick = async () => {
          if (recH) { // Aufnahme ohne Erkennung beenden
            const r = recH; recH = null; mic.classList.remove('on'); mic.textContent = '🎤';
            const res = await r.stop(); if (res.url) URL.revokeObjectURL(res.url);
            if (res.loudFrames > 6) finish('Super gesprochen! 🎉'); else { heard.textContent = 'Ich habe fast nichts gehört – noch mal, ruhig lauter!'; A.sfx('soft'); }
            return;
          }
          if (busy) return; busy = true; A.stop(); A.sfx('pop');
          const mode = A.recognitionMode();
          if (mode !== 'none') {
            mic.classList.add('on'); heard.textContent = 'Ich höre zu …';
            const alts = await A.listen(); mic.classList.remove('on'); busy = false;
            if (!alive(tk)) return;
            if (alts && alts.length) {
              tries++;
              const sim = A.similarity(text, alts);
              if (sim >= 0.5) { finish(`Ich habe dich verstanden: „${esc(alts[0])}“ ⭐`); return; }
              if (tries >= 2) { finish('Gut gemacht! Das Mikrofon tut sich manchmal schwer – du hast es super versucht! 🎉'); return; }
              heard.textContent = `Ich habe „${alts[0]}“ gehört. Hör dir den Satz noch mal an und versuch es noch einmal!`; A.sfx('soft'); return;
            }
            if (alts && !alts.length) { tries++; if (tries >= 2) { finish('Gut gemacht! Weiter geht\'s! 🎉'); return; } heard.textContent = 'Ich hab dich nicht ganz gehört – noch mal?'; A.sfx('soft'); return; }
            // null: Erkennung nicht erlaubt/verfügbar → Aufnahme mit Lautstärke
          }
          busy = false;
          if (!A.micSupported()) { finish('Toll! 🎉'); return; }
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
      ${ep.wg ? `<a class="btn white small" style="text-decoration:none" href="https://wissensgarten.mathenachhilfe-online.com/#paket=${encodeURIComponent(ep.wg)}" target="_blank" rel="noopener">🌻 Wörter wiederholen im Wissensgarten</a>` : ''}
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
        <label class="radio"><input type="radio" name="stimme" value="frei" ${A.voiceMode() === 'frei' ? 'checked' : ''}><span><b>Freie Stimmen (Standard)</b><br><span class="muted">Vorab aufgenommen, funktionieren auch ohne Internet. Computer-Stimmen (Piper).</span></span></label>
        <label class="radio" style="opacity:${window.NOVA_FREIGEGEBEN ? 1 : .45}"><input type="radio" name="stimme" value="nova" ${window.NOVA_FREIGEGEBEN ? '' : 'disabled'} ${A.voiceMode() === 'nova' ? 'checked' : ''}><span><b>Nova – KI-Stimme ${window.NOVA_FREIGEGEBEN ? '' : '(kommt bald)'}</b><br><span class="muted">Besonders natürliche Stimme, mit KI erzeugt (OpenAI) und von unserem Stimm-Server geladen.</span></span></label>
        <label class="radio"><input type="radio" name="stimme" value="geraet" ${A.voiceMode() === 'geraet' ? 'checked' : ''}><span><b>Stimme des Geräts</b><br><span class="muted">${A.canSpeak() ? 'Die englische Stimme deines Geräts.' : 'Auf diesem Gerät gibt es keine englische Stimme.'}</span></span></label>
      </div>
      <div class="card" style="margin-top:14px">
        <div class="set"><span>👪 Für Eltern</span><button class="btn small blue" data-parents>Öffnen</button></div>
        <div class="set"><span>🔒 Datenschutz</span><a class="btn small white" href="datenschutz.html">Lesen</a></div>
      </div>
      <div class="foot">Version ${esc(window.APP_VERSION)} vom ${esc(window.APP_VERSION_DATE)}<br>Freie Stimmen (Piper): cori, john, thorsten, siwis, davefx – Angaben unter <a href="lizenzen.txt">Lizenzen</a></div>
    </div>`);
    el.querySelector('[data-back]').onclick = () => { A.sfx('tap'); home(); };
    el.querySelectorAll('[data-sw]').forEach((b) => b.onclick = () => { s[b.dataset.sw] = !s[b.dataset.sw]; save(); A.sfx('tap'); settings(); });
    el.querySelectorAll('[data-k]').forEach((b) => b.onclick = () => { state.klasse = +b.dataset.k; save(); A.sfx('tap'); settings(); });
    el.querySelectorAll('input[name=stimme]').forEach((x) => x.onchange = () => { s.stimme = x.value; save(); A.sfx('tap'); speak("Hello! I'm Ruby.", 'ruby'); });
    el.querySelector('[data-name]').onclick = async () => { await askName(); settings(); };
    el.querySelector('[data-parents]').onclick = () => { A.sfx('tap'); parentGate(); };
  }

  function parentGate() {
    const year = new Date().getFullYear();
    const bg = h(`<div class="modal-bg"><div class="modal card"><h2 style="font-size:20px">👪 Eltern-Tor</h2>
      <p class="muted">Dieser Bereich ist für Erwachsene. Bitte gib dein Geburtsjahr ein.</p>
      <input class="input" inputmode="numeric" maxlength="4" placeholder="z. B. 1985"><div class="err"></div>
      <div class="row" style="margin-top:8px"><button class="btn white" data-x>Abbrechen</button><button class="btn blue" data-ok>Weiter</button></div></div></div>`);
    document.body.appendChild(bg);
    bg.querySelector('[data-x]').onclick = () => bg.remove();
    bg.querySelector('[data-ok]').onclick = () => {
      const y = parseInt(bg.querySelector('input').value, 10);
      if (!(y >= 1900 && y <= year - 18)) { bg.querySelector('.err').textContent = 'Dieser Bereich ist nur für Erwachsene.'; A.sfx('soft'); return; }
      bg.remove(); parents();
    };
  }

  function parents() {
    const s = state.settings;
    const total = C.worlds[0].episodes.filter((e) => !e.soon).length;
    const cando = C.worlds.flatMap((w) => w.episodes || []).filter((e) => e.kann && prog(e.id) && prog(e.id).done).map((e) => e.kann);
    const r = (v, label, hint) => `<label class="radio"><input type="radio" name="rec" value="${v}" ${s.recognition === v ? 'checked' : ''}><span><b>${label}</b><br><span class="muted">${hint}</span></span></label>`;
    const el = screen(`<div>
      <div class="top"><button class="iconbtn" data-back aria-label="Zurück">←</button><div class="title" style="font-size:22px">👪 Für Eltern</div></div>
      <div class="card"><h3>Fortschritt</h3><p>${stampCount()} von ${total} Episoden geschafft · diese Woche ${weekCount()} (Wochenziel ${WEEK_GOAL}) · Klasse ${state.klasse || '–'}</p>
        <h3 style="margin-top:10px">Das kann ${esc(state.name || 'euer Kind')} jetzt auf Englisch</h3>
        ${cando.length ? `<ul>${cando.map((x) => `<li>${esc(x)}</li>`).join('')}</ul>` : '<p class="muted">Sobald die erste Episode geschafft ist, steht hier, was euer Kind schon kann.</p>'}</div>
      <div class="card" style="margin-top:14px"><h3>Mikrofon und Spracherkennung</h3>
        <p class="muted">Das Mikrofon geht nur an, wenn euer Kind auf 🎤 tippt. Beim Nachsprechen (Papagei-Echo) bleibt die Aufnahme auf dem Gerät und wird sofort wieder gelöscht.
        In der Sprech-Mission kann eine Spracherkennung prüfen, ob der Satz verstanden wurde. <b>Standard: nur auf dem Gerät</b> – die Stimme eures Kindes verlässt das Gerät nicht.</p>
        ${r('device', 'Nur auf dem Gerät (empfohlen)', 'Erkennung nur, wenn der Browser sie auf dem Gerät kann (z. B. Chrome ab Version 139). Sonst wird nur geprüft, ob laut genug gesprochen wurde.')}
        ${r('off', 'Aus', 'Keine Spracherkennung; nur Aufnahme und Lautstärke auf dem Gerät.')}
        ${r('browser', 'Auch Online-Erkennung des Browsers erlauben', 'Nur mit eurer Zustimmung: Je nach Browser wird die Aufnahme des gesprochenen Satzes an den Sprachdienst des Browser-Herstellers gesendet (z. B. Google bei Chrome, Apple bei Safari). Wir selbst erhalten nichts.')}
        <p class="muted" style="font-size:14px">Jetzt aktiv: <b data-mode></b></p>
        <p class="muted" style="font-size:14px" data-hint></p></div>
      <div class="card" style="margin-top:14px"><h3>Kommt in den nächsten Wochen</h3>
        <p class="muted">Eltern-Handy koppeln mit verschlüsselter Sicherung, Pro-Freischaltung per Code, Hinweis auf Englisch-Nachhilfe, anonyme abschaltbare Zählung.</p></div>
      <div class="card" style="margin-top:14px"><h3>Daten</h3><p class="muted">Alles wird nur in diesem Browser gespeichert. Kein Konto, keine Werbung, kein Tracking.</p>
        <div class="row"><a class="btn small white" href="datenschutz.html">Datenschutz</a><button class="btn small white" data-reset>🗑️ Alle Daten löschen</button></div></div>
      <div class="foot">Version ${esc(window.APP_VERSION)} vom ${esc(window.APP_VERSION_DATE)}</div>
    </div>`);
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
      const ms = PK.modes(c, pack.lang);
      const pickM = ms.filter((m) => m !== lastMode);
      const m = (pickM.length ? pickM : ms)[Math.floor(Math.random() * (pickM.length ? pickM.length : ms.length))];
      lastMode = m; plan.push({ mode: m, card: c });
    }
    for (let i = 0; i < plan.length; i++) {
      const res = await packStep(pack, L, plan[i], i / plan.length, run);
      if (res === 'quit') { save(); return home(); }
    }
    const goal = noteEpisodeDone(); save();
    packSummary(pack, run, goal);
  }

  function packFrame(pack, frac, title, sub) {
    const el = screen(`<div class="stage">
      <div class="ep-top"><button class="iconbtn" data-quit aria-label="Beenden">✕</button>
        <div class="progress"><i style="width:${Math.round(frac * 100)}%"></i></div><span class="pill">${pack.emoji}</span></div>
      <div class="game-title">${title}<small>${sub}</small></div>
      <div data-body class="stage" style="min-height:0"></div>${ruby('', '')}</div>`);
    requestAnimationFrame(() => { const i = el.querySelector('.progress > i'); if (i) i.style.width = Math.round(frac * 100 + 8) + '%'; });
    return el;
  }

  function packStep(pack, L, step, frac, run) {
    return new Promise((resolve) => {
      let tk = token;   // wird nach jedem packFrame (neuer Bildschirm) neu gesetzt
      const say = (t) => (t && L.who ? speak(t, L.who) : Promise.resolve(false));
      const done = (cardsFirst) => {
        for (const [c, ok] of cardsFirst) { PK.note(state.karten, pack, c, ok); run.total++; if (ok) run.first++; }
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

      const c = step.card; const k = PK.classify(c, pack.lang);
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
        const t = PK.bauteile(c, pack.lang);
        const el = packFrame(pack, frac, '🔤 Buchstaben-Bau', 'Bau das Wort Buchstabe für Buchstabe!'); tk = token;
        el.querySelector('[data-quit]').onclick = () => resolve('quit');
        const body = el.querySelector('[data-body]');
        const letters = [...t.wort];
        const extraPool = 'aeinrstlodmuhgk';
        const extra = []; while (extra.length < 2) { const x = extraPool[Math.floor(Math.random() * extraPool.length)]; if (!letters.includes(x)) extra.push(x); }
        const tiles = shuffle(letters.map((l, i) => ({ l, i })).concat(extra.map((l) => ({ l, i: -1 }))));
        body.innerHTML = `<div class="qcard"><div class="qimg">${esc(c.img || pack.emoji)}</div><div class="q">${esc(c.q)}</div>
          ${k.vorher && A.canHear(k.vorher, L.who || 'de') ? '<button class="speaker" data-hear aria-label="Anhören">🔊</button>' : (k.kind === 'diktat' ? `<div class="novoice">${esc(c.a)}<small>Keine Stimme auf diesem Gerät – lies das Wort und bau es nach.</small></div>` : '')}</div>
          <div class="slots">${t.vor ? `<span class="pre">${esc(t.vor.trim())}</span>` : ''}${letters.map((_, i) => `<span class="slot" data-s="${i}"></span>`).join('')}</div>
          <div class="tiles">${tiles.map((x, i) => `<button class="tile" data-t="${i}"${DIAG ? ` data-pos="${x.i}"` : ''}>${esc(x.l)}</button>`).join('')}</div>`;
        let pos = 0; let mistakes = 0;
        const hb = body.querySelector('[data-hear]'); if (hb) { hb.onclick = () => { A.sfx('tap'); say(k.vorher); }; say(k.vorher); }
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
