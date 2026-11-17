'use strict';
/* =====================================================================
   Zahlen-Dungeon (Roadmap Tag 6, 17.11.2026) – kurzer Roguelike-Lauf in 3–5 Minuten
   7 Räume, 5 Herzen. Vor jedem Raum zwei Türen: Monster, Schatz oder Lagerfeuer.
   Gegner = Aufgaben: Jede richtige Antwort ist ein Treffer (schnell = Volltreffer, doppelter Schaden),
   jede falsche kostet ein Herz – danach kommt wie immer der Lösungsweg. Im letzten Raum wartet der Wächter.
   Aufgaben: Themen der eingestellten Klasse (sonst das gewählte Thema und die aktuelle Stufe).
   Vorbereitet für Tag 7/8: Rechen-Karten (Strategien) und mehr Bosse.
   ===================================================================== */
const DG = { ROOMS: 7, HEARTS: 5 };
const DG_MON = {
  schleim: { name: 'Schleimi', hp: 2 },
  fleder: { name: 'Flatter', hp: 3 },
  geist: { name: 'Der Zahlengeist', hp: 3 },
  golem: { name: 'Steinbeißer', hp: 4 },
  waechter: { name: 'Der Wächter', hp: 6, boss: true },
};
const DG_DEPTH = [['schleim', 'fleder'], ['schleim', 'fleder'], ['fleder', 'geist'], ['fleder', 'geist'], ['geist', 'golem'], ['geist', 'golem']];
let DGN = null;   // laufender Dungeon
const dgState = () => S.dungeon || (S.dungeon = { best: 0, runs: 0, wins: 0, beaten: 0 });

/** gezeichnete Monster (eigene SVGs) */
function dgMonSvg(kind, size = 72) {
  const eye = (x, y, r = 5, c = '#1d1b2f') => `<circle cx="${x}" cy="${y}" r="${r + 2.5}" fill="#fff"/><circle cx="${x + 1}" cy="${y + 1}" r="${r}" fill="${c}"/><circle cx="${x + 2.5}" cy="${y - 1.5}" r="1.4" fill="#fff"/>`;
  const art = {
    schleim: `<path d="M-38 30 C-42 0 -26 -30 0 -32 C26 -30 42 0 38 30 Z" fill="#5fd068"/><path d="M-30 -6 C-26 -20 -14 -26 -6 -26" stroke="#b9f5bf" stroke-width="5" fill="none" stroke-linecap="round"/>${eye(-12, 2)}${eye(12, 2)}<path d="M-10 16 Q0 24 10 16" stroke="#1d6b2c" stroke-width="3.5" fill="none" stroke-linecap="round"/><ellipse cx="0" cy="32" rx="40" ry="5" fill="#000" opacity=".18"/>`,
    fleder: `<path d="M-14 -4 C-30 -26 -46 -18 -48 -4 C-40 -10 -34 -2 -38 6 C-30 0 -24 8 -26 14 C-20 6 -14 8 -12 10 Z" fill="#6a4fb4"/><path d="M14 -4 C30 -26 46 -18 48 -4 C40 -10 34 -2 38 6 C30 0 24 8 26 14 C20 6 14 8 12 10 Z" fill="#6a4fb4"/><circle cx="0" cy="2" r="20" fill="#8e6bd8"/><path d="M-14 -14 L-10 -28 L-4 -16 Z M14 -14 L10 -28 L4 -16 Z" fill="#8e6bd8"/>${eye(-7, -1, 4, '#c0142f')}${eye(7, -1, 4, '#c0142f')}<path d="M-5 11 L-3 16 L-1 11 M1 11 L3 16 L5 11" fill="#fff"/>`,
    geist: `<path d="M-28 34 L-28 -6 C-28 -28 -14 -36 0 -36 C14 -36 28 -28 28 -6 L28 34 L19 26 L10 34 L0 26 L-10 34 L-19 26 Z" fill="#e8f1ff" stroke="#9bb7e0" stroke-width="2"/>${eye(-10, -10, 4.5)}${eye(10, -10, 4.5)}<ellipse cx="0" cy="6" rx="6" ry="7" fill="#1d1b2f"/><text x="-21" y="22" font-size="11" font-weight="800" fill="#9bb7e0" font-family="sans-serif">7</text><text x="13" y="22" font-size="11" font-weight="800" fill="#9bb7e0" font-family="sans-serif">3</text>`,
    golem: `<rect x="-34" y="-30" width="68" height="62" rx="16" fill="#9b8f84"/><path d="M-20 -30 L-14 -14 L-22 -4 M18 -30 L12 -18 M24 32 L18 18 L26 8" stroke="#6f655c" stroke-width="3" fill="none"/><rect x="-24" y="-12" width="16" height="9" rx="3" fill="#ffb347"/><rect x="8" y="-12" width="16" height="9" rx="3" fill="#ffb347"/><rect x="-16" y="10" width="32" height="8" rx="3" fill="#4e463f"/><path d="M-12 10 v4 M-4 10 v4 M4 10 v4 M12 10 v4" stroke="#d9d2c9" stroke-width="2.5"/>`,
    waechter: `<path d="M-30 -18 L-40 -44 L-18 -26 Z M30 -18 L40 -44 L18 -26 Z" fill="#ffd166"/><path d="M-36 34 C-44 4 -34 -28 0 -30 C34 -28 44 4 36 34 Z" fill="#e8673a"/><path d="M-20 22 C-10 30 10 30 20 22 L16 34 L-16 34 Z" fill="#fff3d6"/><path d="M-14 22 l3 6 l3 -6 M2 22 l3 6 l3 -6" fill="#fff"/>${eye(-13, -6, 5, '#7a0d0d')}${eye(13, -6, 5, '#7a0d0d')}<path d="M-22 -16 L-6 -11 M22 -16 L6 -11" stroke="#7a2a12" stroke-width="4" stroke-linecap="round"/><path d="M-6 6 Q0 10 6 6" stroke="#7a2a12" stroke-width="3" fill="none"/><path d="M-12 -32 L-6 -42 L0 -34 L6 -42 L12 -32 Z" fill="#ffd166"/>`,
  }[kind];
  return `<svg viewBox="-50 -50 100 100" width="${size}" height="${size}" aria-hidden="true">${art}</svg>`;
}
const dgDoorSvg = (icon, glow) => `<svg viewBox="0 0 80 110" width="78" height="106" aria-hidden="true"><path d="M6 108 V40 A34 34 0 0 1 74 40 V108 Z" fill="#3b2a20"/><path d="M14 108 V42 A26 26 0 0 1 66 42 V108 Z" fill="${glow}"/><path d="M40 16 V108 M14 60 H66 M14 84 H66" stroke="#2a1c14" stroke-width="2" opacity=".35"/><circle cx="58" cy="76" r="3" fill="#ffd166"/><text x="40" y="66" font-size="28" text-anchor="middle">${icon}</text></svg>`;
const dgHearts = () => Array.from({ length: DG.HEARTS }, (_, i) => `<span class="dg-heart ${i < DGN.hearts ? '' : 'lost'}">❤️</span>`).join('');

/** Aufgaben-Quelle: Themen der Klasse (ohne lange Textaufgaben), sonst gewähltes Thema */
function dgTopics() {
  const k = S.settings.klasse;
  if (k && CLASS_TOPICS[k]) { const t = CLASS_TOPICS[k].filter(([tp]) => tp !== 'word'); if (t.length) return t.map(([tp, l]) => [tp, S.autoLvl[tp] ?? l]); }
  const op = S.sel.op === 'mix' ? 'add' : S.sel.op;
  return [[op, currentLevel(op)]];
}
function dgInfo() {
  const k = S.settings.klasse;
  return k && CLASS_TOPICS[k] ? `Aufgaben aus Klasse ${k}` : `${TOPICS[S.sel.op === 'mix' ? 'add' : S.sel.op].name} · ${lvlName(currentLevel(S.sel.op === 'mix' ? 'add' : S.sel.op))}`;
}

function startDungeon() {
  if (Sheet.open) closeSheet();
  DGN = { room: 0, hearts: DG.HEARTS, mon: null, beaten: 0, chests: 0, funken: 0, won: false, dead: false, recent: [] };
  dgState().runs++; save();
  startGame('dungeon', { gen: dgGen, op: 'mix', lvl: 0, title: 'Zahlen-Dungeon' });
  dgHud();
}
function dgGen() {
  if (!DGN || DGN.won || DGN.dead) return null;   // Ende → Ergebnis
  if (!DGN.mon || DGN.mon.dead) setTimeout(dgDoors, 30);   // zwischen zwei Räumen: zuerst die Türen
  const boss = DGN.mon && !DGN.mon.dead && DG_MON[DGN.mon.kind].boss, tops = dgTopics();
  let t;
  for (let i = 0; i < 20; i++) {
    const [tp, l] = pick(tops), lvl = Math.min(l + (boss ? 1 : 0), Math.max(l, 4));
    t = makeTask(tp, lvl); if (!DGN.recent.includes(keyOf(t))) break;
  }
  DGN.recent.push(keyOf(t)); if (DGN.recent.length > 12) DGN.recent.shift();
  return t;
}

/* ---------- Anzeige über der Aufgabe ---------- */
function dgHud() {
  let el = $('dgHud');
  if (!el) { el = document.createElement('div'); el.id = 'dgHud'; el.className = 'dg-hud'; $('game').insertBefore(el, $('game').querySelector('.win')); }
  if (!DGN) return;
  const m = DGN.mon, def = m && DG_MON[m.kind];
  el.innerHTML = `<div class="dg-me"><div class="dg-hearts" aria-label="Herzen">${dgHearts()}</div><span class="dg-room">Raum ${Math.min(DGN.room, DG.ROOMS)}/${DG.ROOMS}</span></div>
    ${m ? `<div class="dg-foe ${m.dead ? 'dead' : ''} ${def.boss ? 'boss' : ''}"><div class="dg-mon" id="dgMon">${dgMonSvg(m.kind, def.boss ? 62 : 54)}</div>
      <div class="dg-foeinfo"><b>${esc(def.name)}</b><div class="dg-hp"><i style="width:${Math.max(0, m.hp) / m.max * 100}%"></i></div></div></div>` : '<div class="dg-foe empty"></div>'}`;
}
function dgFloat(txt, cls) {
  const f = document.createElement('div'); f.className = 'dg-dmg ' + (cls || ''); f.textContent = txt;
  ($('dgMon') || $('dgHud')).appendChild(f); setTimeout(() => f.remove(), 900);
}

/** aus submit(): richtige Antwort = Treffer, falsche = ein Herz weniger */
function dgAnswer(ok, dt, t) {
  if (!DGN || !DGN.mon || DGN.mon.dead) return;
  const m = DGN.mon, def = DG_MON[m.kind];
  if (ok) {
    const crit = dt < (t && t.story ? 12000 : 4000), dmg = crit ? 2 : 1;
    m.hp -= dmg; dgHud(); $('dgMon')?.classList.add('hit'); dgFloat(crit ? `−${dmg} Volltreffer!` : `−${dmg}`, crit ? 'crit' : '');
    Sound.tone(crit ? 45 : 50, 0, .09, 'square', .12); Sound.tone(crit ? 40 : 45, .07, .12, 'square', .1);
    if (m.hp <= 0) {
      m.dead = true; DGN.beaten++; dgState().beaten++;
      const f = def.boss ? 20 : 3; DGN.funken += f; addFunken(f);
      setTimeout(() => { if (DGN && DGN.mon === m) { dgHud(); dgFloat(`+${f} 💫`, 'gold'); } }, 150);
      [0, 4, 7, 12].forEach((n, i) => Sound.tone(67 + n, .15 + i * .08, .18, 'triangle', .14));
      if (def.boss) { DGN.won = true; DGN.room = DG.ROOMS; Confetti.burst(90); }
    }
  } else {
    DGN.hearts--; dgHud(); vibrate([80, 40, 80]);
    $('game').classList.remove('dg-ouch'); void $('game').offsetWidth; $('game').classList.add('dg-ouch');
    Sound.tone(40, 0, .25, 'sawtooth', .12, 33);
    if (DGN.hearts <= 0) DGN.dead = true;
  }
}

/* ---------- Türen zwischen den Räumen ---------- */
function dgOverlay(html) {
  let ov = $('dgOv'); if (!ov) { ov = document.createElement('div'); ov.id = 'dgOv'; ov.className = 'dg-ov'; $('game').appendChild(ov); }
  ov.innerHTML = html; i18nTree?.(ov); return ov;
}
function dgCloseOverlay() { $('dgOv')?.remove(); }
function dgDoors() {
  if (!G || G.mode !== 'dungeon' || G.over || !DGN || DGN.won || DGN.dead || (DGN.mon && !DGN.mon.dead)) return;
  G.paused = true; $('solution').hidden = true;
  const last = DGN.room >= DG.ROOMS - 1;
  let doors;
  if (last) doors = [{ type: 'boss' }];
  else {
    // höchstens 2 Schätze je Lauf (sonst ließen sich alle Kämpfe umgehen), Lagerfeuer nur mit fehlenden Herzen, Raum 1 immer Kampf
    const r = rand(), extra = DGN.room === 0 ? 'kampf' : DGN.hearts < DG.HEARTS && r < .3 ? 'rast' : DGN.chests < 2 && r < .6 ? 'schatz' : 'kampf';
    doors = shuffle([{ type: 'kampf' }, { type: extra }]);
  }
  const D = { kampf: ['👾', '#5a2e2e', 'Monster', 'Kampf!'], schatz: ['💎', '#2e4a5a', 'Schatz', 'Funken finden'], rast: ['🔥', '#5a4a2e', 'Lagerfeuer', '+1 Herz'], boss: ['👑', '#6a2414', 'Der Wächter', 'Letzter Raum'] };
  const ov = dgOverlay(`<div class="dg-room-head"><b>${last ? 'Letzter Raum' : `Raum ${DGN.room + 1} von ${DG.ROOMS}`}</b><div class="dg-hearts">${dgHearts()}</div></div>
    <p class="dg-say">${DGN.room === 0 ? 'Willkommen im Zahlen-Dungeon! Welche Tür nimmst du?' : last ? 'Hinter dieser Tür wartet der Wächter. Bist du bereit?' : 'Zwei Türen. Wohin jetzt?'}</p>
    <div class="dg-doors">${doors.map((d, i) => { const [ic, col, name, sub] = D[d.type]; return `<button class="dg-door ${d.type}" data-i="${i}">${dgDoorSvg(ic, col)}<b>${name}</b><span>${sub}</span></button>`; }).join('')}</div>
    <p class="small dg-info">${esc(dgInfo())}</p>`);
  ov.querySelectorAll('.dg-door').forEach(b => b.onclick = () => { Sound.click(); dgEnter(doors[+b.dataset.i].type); });
}
function dgEnter(type) {
  if (!DGN) return;
  DGN.room++;
  if (type === 'schatz' || type === 'rast') {
    let msg;
    if (type === 'schatz') { const f = rnd(5, 10); DGN.funken += f; DGN.chests++; addFunken(f); msg = `<div class="dg-big">💎</div><b>Ein Schatz!</b><p>Du findest <b>${f} 💫</b>.</p>`; [0, 7, 12].forEach((n, i) => Sound.tone(79 + n, i * .09, .2, 'sine', .12)); }
    else { DGN.hearts = Math.min(DG.HEARTS, DGN.hearts + 1); msg = `<div class="dg-big dg-fire">🔥</div><b>Ein Lagerfeuer</b><p>Du ruhst dich aus: <b>+1 ❤️</b></p>`; Sound.tone(60, 0, .4, 'sine', .1); }
    save(); dgHud();
    const ov = dgOverlay(`<div class="dg-reward">${msg}<div class="dg-hearts">${dgHearts()}</div><button class="btn primary" id="dgOn">Weiter</button></div>`);
    $('dgOn').onclick = () => { Sound.click(); if (DGN.room >= DG.ROOMS) DGN.room = DG.ROOMS - 1; dgDoors(); };
    return;
  }
  const kind = type === 'boss' ? 'waechter' : pick(DG_DEPTH[Math.min(DGN.room - 1, DG_DEPTH.length - 1)]), def = DG_MON[kind];
  DGN.mon = { kind, hp: def.hp, max: def.hp, dead: false };
  dgHud(); dgCloseOverlay();
  dgOverlay(`<div class="dg-intro ${def.boss ? 'boss' : ''}">${dgMonSvg(kind, 120)}<b>${esc(def.name)}</b><span>${def.boss ? 'Besiege ihn mit 6 Treffern!' : `${def.hp} Treffer besiegen das Monster.`}</span></div>`);
  Sound.tone(def.boss ? 36 : 48, 0, .3, 'sawtooth', .08, def.boss ? 30 : 44);
  setTimeout(() => { if (!G || G.mode !== 'dungeon' || G.over) return; dgCloseOverlay(); G.paused = false; G.locked = false; nextTask(); }, def.boss ? 1400 : 900);
}

/** Ergebnis-Karte */
function dgFinish() {
  dgCloseOverlay();
  if (!DGN) return '';
  const st = dgState(), reached = DGN.won ? DG.ROOMS : Math.max(1, DGN.room), rec = reached > st.best;
  if (rec) st.best = reached;
  if (DGN.won) st.wins++;
  save();
  const head = DGN.won ? `${dgMonSvg('waechter', 74)}<div><b>Dungeon geschafft!</b><span>Du hast den Wächter besiegt. 👑</span></div>`
    : DGN.dead ? `${dgMonSvg(DGN.mon ? DGN.mon.kind : 'schleim', 64)}<div><b>Keine Herzen mehr …</b><span>Raum ${reached} von ${DG.ROOMS} – beim nächsten Mal kommst du weiter!</span></div>`
      : `${dgMonSvg('schleim', 56)}<div><b>Lauf beendet</b><span>Raum ${reached} von ${DG.ROOMS}</span></div>`;
  return `<div class="card dg-res ${DGN.won ? 'won' : ''}"><div class="dg-res-head">${head}</div>
    <div class="dg-res-stats"><span>👾 <b>${DGN.beaten}</b> besiegt</span><span>💎 <b>${DGN.chests}</b> Schätze</span><span>💫 <b>+${DGN.funken}</b></span></div>
    ${rec ? '<span class="newbest">Neuer Rekord!</span>' : `<span class="small muted">Rekord: Raum ${st.best} von ${DG.ROOMS}${st.wins ? ` · ${st.wins}× geschafft` : ''}</span>`}</div>`;
}

/* ---------- Startseite ---------- */
function renderDungeonCard() {
  const el = $('dungeonCard'); if (!el) return;
  const st = dgState();
  el.hidden = false;
  el.innerHTML = `<div class="ch-ico dg-ico" aria-hidden="true">${dgMonSvg('schleim', 40)}</div><div class="ch-txt"><b>Zahlen-Dungeon</b><span>${st.runs ? `Rekord: Raum ${st.best}/${DG.ROOMS}${st.wins ? ` · ${st.wins}× geschafft 👑` : ''}` : '7 Räume, 5 Herzen – schaffst du den Wächter?'}</span></div>
    <button class="btn small primary" id="dgGo">Los</button>`;
  $('dgGo').onclick = () => { Sound.init(); Sound.click(); startDungeon(); };
}
