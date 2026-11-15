// Raumlicht – 3D-Bühne. Reiner Anzeiger: Die App (Kotlin) schickt den Zustand, die Szene zeichnet und meldet Gesten zurück.
// Mathe-Koordinaten (x1 | x2 | x3) wie in der Schule: x1 nach vorn, x2 nach rechts, x3 nach oben.
import * as THREE from 'three';
import { EffectComposer } from 'three/examples/jsm/postprocessing/EffectComposer.js';
import { Pass } from 'three/examples/jsm/postprocessing/Pass.js';
import { UnrealBloomPass } from 'three/examples/jsm/postprocessing/UnrealBloomPass.js';
import { OutputPass } from 'three/examples/jsm/postprocessing/OutputPass.js';
import { RoomEnvironment } from 'three/examples/jsm/environments/RoomEnvironment.js';

const V = (p) => new THREE.Vector3(p[1], p[2], p[0]);          // Mathe → Szene
const fromV = (v) => [v.z, v.x, v.y];                            // Szene → Mathe
const COLORS = { cyan: 0x38e8ff, pink: 0xff4fa3, yellow: 0xffd54f, green: 0x5cffa1, orange: 0xff8a3d, violet: 0xa78bfa, white: 0xffffff, red: 0xff5566 };
const col = (c) => new THREE.Color(typeof c === 'string' ? (COLORS[c] ?? c) : (c ?? 0x38e8ff));
const hdr = (c, k) => col(c).multiplyScalar(k);
const emit = (name, data) => {
  const msg = JSON.stringify({ type: name, ...data });
  if (window.Android && window.Android.event) window.Android.event(msg);
  if (window.onSceneEvent) window.onSceneEvent(JSON.parse(msg));
};

window.addEventListener('error', (e) => emit('error', { msg: String(e.message || e) + ' (Zeile ' + (e.lineno || '?') + ')' }));
window.addEventListener('unhandledrejection', (e) => emit('error', { msg: String(e.reason) }));

// ───────── Grundaufbau ─────────
const canvas = document.getElementById('c');
const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, powerPreference: 'high-performance' });
let pixelRatio = Math.min(window.devicePixelRatio || 1, 2);
renderer.setPixelRatio(pixelRatio);
renderer.outputColorSpace = THREE.SRGBColorSpace;
renderer.toneMapping = THREE.ACESFilmicToneMapping;
renderer.toneMappingExposure = 1.05;
renderer.shadowMap.enabled = true;
renderer.shadowMap.type = THREE.PCFSoftShadowMap;
renderer.shadowMap.autoUpdate = false;
renderer.autoClear = false;

const scene = new THREE.Scene();
scene.fog = new THREE.FogExp2(0x05071a, 0.022);
const pmrem = new THREE.PMREMGenerator(renderer);
scene.environment = pmrem.fromScene(new RoomEnvironment(), 0.04).texture;
scene.environmentIntensity = 1.0;

const camera = new THREE.PerspectiveCamera(42, 1, 0.1, 400);
const sky = new THREE.Group(), decor = new THREE.Group(), world = new THREE.Group(), fx = new THREE.Group();
scene.add(sky, decor, world, fx);

// Licht: kühles Mondlicht von oben (Schatten), warmes Gegenlicht
const key = new THREE.DirectionalLight(0xbfd0ff, 2.2);
key.position.set(6, 16, 8); key.castShadow = true;
key.shadow.mapSize.set(2048, 2048); key.shadow.radius = 6; key.shadow.bias = -0.0004;
Object.assign(key.shadow.camera, { left: -14, right: 14, top: 14, bottom: -14, near: 1, far: 50 });
scene.add(key, key.target);
const rim = new THREE.DirectionalLight(0xff4fa3, 0.55); rim.position.set(-10, 5, -8); scene.add(rim);
scene.add(new THREE.HemisphereLight(0x5566cc, 0x1a1030, 1.3));

// Himmel: Farbverlauf + Sterne
{
  const g = new THREE.SphereGeometry(200, 32, 16);
  const m = new THREE.ShaderMaterial({
    side: THREE.BackSide, depthWrite: false, fog: false,
    uniforms: { t: { value: 0 } },
    vertexShader: 'varying vec3 p; void main(){ p = normalize(position); gl_Position = projectionMatrix*modelViewMatrix*vec4(position,1.); }',
    fragmentShader: `varying vec3 p; uniform float t;
      void main(){ float h = p.y;
        vec3 top = vec3(0.006,0.008,0.03), mid = vec3(0.02,0.014,0.07), hor = vec3(0.07,0.022,0.11);
        vec3 c = mix(hor, mid, smoothstep(-0.05,0.25,h)); c = mix(c, top, smoothstep(0.2,0.8,h));
        c = mix(c, vec3(0.01,0.012,0.035), 1.0 - smoothstep(-0.35,0.0,h));
        float glow = exp(-abs(h)*9.0); c += vec3(0.05,0.012,0.08)*glow*(0.8+0.2*sin(t*0.3+p.x*4.));
        gl_FragColor = vec4(c,1.); }`,
  });
  sky.add(new THREE.Mesh(g, m)); sky.userData.mat = m;
  const n = 900, pos = new Float32Array(n * 3), sz = new Float32Array(n);
  for (let i = 0; i < n; i++) {
    const a = Math.random() * Math.PI * 2, h = Math.pow(Math.random(), 0.7) * 0.95 + 0.04, r = Math.sqrt(1 - h * h);
    pos.set([Math.cos(a) * r * 180, h * 180, Math.sin(a) * r * 180], i * 3); sz[i] = Math.random();
  }
  const sg = new THREE.BufferGeometry(); sg.setAttribute('position', new THREE.BufferAttribute(pos, 3)); sg.setAttribute('s', new THREE.BufferAttribute(sz, 1));
  const sm = new THREE.ShaderMaterial({
    transparent: true, depthWrite: false, fog: false, blending: THREE.AdditiveBlending, uniforms: { t: { value: 0 }, pr: { value: pixelRatio } },
    vertexShader: 'attribute float s; varying float a; uniform float t, pr; void main(){ a = 0.45+0.55*sin(t*(0.6+s*2.)+s*40.); gl_PointSize = (1.2+s*2.2)*pr; gl_Position = projectionMatrix*modelViewMatrix*vec4(position,1.); }',
    fragmentShader: 'varying float a; void main(){ float d = length(gl_PointCoord-.5); gl_FragColor = vec4(vec3(0.85,0.9,1.)*a*1.6, 1.0 - smoothstep(.0,.5,d)); }',
  });
  sky.add(new THREE.Points(sg, sm)); sky.userData.stars = sm;
}

// Boden: dunkler, glänzender Bühnenboden (leicht durchsichtig – darunter wird die Welt gespiegelt gezeichnet)
const floorMat = new THREE.MeshStandardMaterial({ color: 0x060918, roughness: 0.62, metalness: 0.0, transparent: true, opacity: 0.945, envMapIntensity: 0.15 });
const floor = new THREE.Mesh(new THREE.CircleGeometry(90, 64), floorMat);
floor.rotation.x = -Math.PI / 2; floor.position.y = -0.002; floor.receiveShadow = true; floor.renderOrder = -5;
scene.add(floor);

// Weiche Leuchtscheibe (für Punkte, Treffer, Lichtkegel am Boden)
const glowTex = (() => {
  const c = document.createElement('canvas'); c.width = c.height = 128; const x = c.getContext('2d');
  const g = x.createRadialGradient(64, 64, 0, 64, 64, 64);
  g.addColorStop(0, 'rgba(255,255,255,1)'); g.addColorStop(0.25, 'rgba(255,255,255,.55)'); g.addColorStop(0.6, 'rgba(255,255,255,.12)'); g.addColorStop(1, 'rgba(255,255,255,0)');
  x.fillStyle = g; x.fillRect(0, 0, 128, 128); return new THREE.CanvasTexture(c);
})();
const glowSprite = (c, size, opacity = 1) => {
  const s = new THREE.Sprite(new THREE.SpriteMaterial({ map: glowTex, color: col(c), blending: THREE.AdditiveBlending, depthWrite: false, transparent: true, opacity }));
  s.scale.setScalar(size); return s;
};

// Beschriftung als Sprite (immer gleich groß auf dem Bildschirm)
function label(text, color = '#e8ecff', size = 1, bg = true) {
  const pad = 14, fs = 44, c = document.createElement('canvas'), x = c.getContext('2d');
  const font = `700 ${fs}px system-ui, "Roboto", "Segoe UI", sans-serif`; x.font = font;
  // Vektorpfeile (U+20D7) selbst zeichnen – nicht jede Schrift kann sie
  const raw = text, arrows = []; text = '';
  for (const ch of raw) { if (ch === '\u20d7') arrows.push(text.length - 1); else text += ch; }
  const w = Math.ceil(x.measureText(text).width) + pad * 2, h = fs + pad * (arrows.length ? 2.2 : 1.4);
  c.width = w; c.height = h; x.font = font; x.textBaseline = 'middle';
  if (bg) { x.fillStyle = 'rgba(8,10,30,.72)'; x.beginPath(); x.roundRect(0, 0, w, h, h / 2.4); x.fill(); x.strokeStyle = color; x.globalAlpha = .55; x.lineWidth = 2.5; x.stroke(); x.globalAlpha = 1; }
  const base = h / 2 + (arrows.length ? 8 : 2);
  x.fillStyle = color; x.fillText(text, pad, base);
  x.strokeStyle = color; x.lineWidth = 4; x.lineCap = 'round'; x.lineJoin = 'round';
  for (const i of arrows) {
    const x0 = pad + x.measureText(text.slice(0, i)).width, x1 = x0 + x.measureText(text[i]).width + 3, y = base - fs * 0.5;
    x.beginPath(); x.moveTo(x0, y); x.lineTo(x1, y); x.moveTo(x1 - 8, y - 6); x.lineTo(x1, y); x.lineTo(x1 - 8, y + 6); x.stroke();
  }
  const t = new THREE.CanvasTexture(c); t.anisotropy = 4;
  const s = new THREE.Sprite(new THREE.SpriteMaterial({ map: t, depthTest: false, depthWrite: false, transparent: true, sizeAttenuation: false, fog: false }));
  s.renderOrder = 50; s.userData.aspect = w / h; s.userData.size = size; s.userData.isLabel = true;
  s.scale.set(0.034 * size * (w / h), 0.034 * size, 1);
  return s;
}
const hex = (c) => '#' + col(c).getHexString();

// ───────── Raster und Achsen ─────────
const gridGroup = new THREE.Group(); decor.add(gridGroup);
let bounds = { min: [0, 0, 0], max: [8, 8, 6] };
let axesGroup = null, flat = false, plain = false;   // flat: ebenes x-y-Koordinatensystem (Analysis) statt Raumachsen
function buildGrid() {
  gridGroup.clear();
  const { min, max } = bounds, pts = [], strong = [];
  for (let a = min[0]; a <= max[0]; a++) (a === 0 ? strong : pts).push(V([a, min[1], 0]), V([a, max[1], 0]));
  for (let b = min[1]; b <= max[1]; b++) (b === 0 ? strong : pts).push(V([min[0], b, 0]), V([max[0], b, 0]));
  const mk = (p, c, o) => { const l = new THREE.LineSegments(new THREE.BufferGeometry().setFromPoints(p), new THREE.LineBasicMaterial({ color: c, transparent: true, opacity: o, depthWrite: false })); l.position.y = 0.004; return l; };
  gridGroup.add(mk(pts, 0x6f7dff, 0.28), mk(strong, 0x9aa8ff, 0.5));
  // Gitterpunkte leuchten leicht
  const dots = []; for (let a = min[0]; a <= max[0]; a++) for (let b = min[1]; b <= max[1]; b++) dots.push(V([a, b, 0]).setY(0.006));
  gridGroup.add(new THREE.Points(new THREE.BufferGeometry().setFromPoints(dots), new THREE.PointsMaterial({ color: 0x9fb0ff, size: 0.07, transparent: true, opacity: 0.8, depthWrite: false })));
  // Achsen
  const axis = (dir, len, neg, c, name) => {
    const g = new THREE.Group(), d = V(dir), q = new THREE.Quaternion().setFromUnitVectors(new THREE.Vector3(0, 1, 0), d);
    const total = len + neg + 0.9;
    const rod = new THREE.Mesh(new THREE.CylinderGeometry(0.022, 0.022, total, 8), new THREE.MeshBasicMaterial({ color: hdr(c, 1.5) }));
    rod.quaternion.copy(q); rod.position.copy(d.clone().multiplyScalar((len + 0.9 - neg) / 2));
    const tip = new THREE.Mesh(new THREE.ConeGeometry(0.09, 0.3, 16), new THREE.MeshBasicMaterial({ color: hdr(c, 1.7) }));
    tip.quaternion.copy(q); tip.position.copy(d.clone().multiplyScalar(len + 1.0));
    const l = label(name, hex(c), 1.0, false); l.position.copy(d.clone().multiplyScalar(len + 1.5));
    g.add(rod, tip, l);
    for (let i = -neg; i <= len; i++) {
      if (i === 0) continue;
      const tk = new THREE.Mesh(new THREE.SphereGeometry(0.045, 8, 8), new THREE.MeshBasicMaterial({ color: hdr(c, 1.3) }));
      tk.position.copy(d.clone().multiplyScalar(i)); g.add(tk);
      if (i % 2 === 0) { const n = label(String(i), hex(c), 0.62, false); n.material.opacity = 0.85; n.position.copy(d.clone().multiplyScalar(i)).add(dir[2] ? new THREE.Vector3(-0.3, 0, 0.3) : new THREE.Vector3(0, -0.01, 0).add(dir[0] ? new THREE.Vector3(-0.42, 0, 0) : new THREE.Vector3(0, 0, -0.42))); g.add(n); }
    }
    return g;
  };
  axesGroup = new THREE.Group(); axesGroup.visible = !flat && !plain; gridGroup.visible = !plain;
  axesGroup.add(axis([1, 0, 0], max[0], -min[0], 'pink', 'x₁'), axis([0, 1, 0], max[1], -min[1], 'green', 'x₂'), axis([0, 0, 1], max[2], 0, 'cyan', 'x₃'));
  gridGroup.add(axesGroup);
}

// ───────── Bühnen-Deko: Scheinwerfer mit Lichtkegeln, Dunst ─────────
const heads = [];
{
  const coneGeo = new THREE.ConeGeometry(1, 1, 24, 1, true); coneGeo.translate(0, -0.5, 0);
  for (let i = 0; i < 6; i++) {
    const a = (i / 6) * Math.PI * 2 + 0.4, g = new THREE.Group();
    g.position.set(Math.cos(a) * 17 + 4, 11, Math.sin(a) * 17 + 4);
    const c = [COLORS.violet, COLORS.cyan, COLORS.pink][i % 3];
    const m = new THREE.ShaderMaterial({
      transparent: true, depthWrite: false, side: THREE.DoubleSide, blending: THREE.AdditiveBlending, fog: false,
      uniforms: { c: { value: col(c) }, k: { value: 0.1 } },
      vertexShader: 'varying float h, dz; varying vec3 n, v; void main(){ h = -position.y; n = normalize(normalMatrix*normal); vec4 mv = modelViewMatrix*vec4(position,1.); v = normalize(-mv.xyz); dz = -mv.z; gl_Position = projectionMatrix*mv; }',
      fragmentShader: 'varying float h, dz; varying vec3 n, v; uniform vec3 c; uniform float k; void main(){ float f = pow(clamp(abs(dot(normalize(n), normalize(v))), 0.0001, 1.), 1.6); float q = clamp(1.-h, 0., 1.); gl_FragColor = vec4(c*k*f*q*q*3.*smoothstep(9.,24.,dz), 1.); }',
    });
    const cone = new THREE.Mesh(coneGeo, m); cone.scale.set(1.7, 24, 1.7);
    const lamp = new THREE.Mesh(new THREE.SphereGeometry(0.28, 12, 12), new THREE.MeshBasicMaterial({ color: hdr(c, 3), fog: false }));
    g.add(cone, lamp); decor.add(g);
    heads.push({ g, cone, m, a, base: new THREE.Vector3(4 + Math.cos(a + 2.4) * 5, 0, 4 + Math.sin(a + 2.4) * 5), ph: i * 1.3 });
  }
}
const dust = (() => {
  const n = 260, pos = new Float32Array(n * 3), s = new Float32Array(n);
  for (let i = 0; i < n; i++) { pos.set([Math.random() * 30 - 11, Math.random() * 11, Math.random() * 30 - 11], i * 3); s[i] = Math.random(); }
  const g = new THREE.BufferGeometry(); g.setAttribute('position', new THREE.BufferAttribute(pos, 3)); g.setAttribute('s', new THREE.BufferAttribute(s, 1));
  const m = new THREE.ShaderMaterial({
    transparent: true, depthWrite: false, blending: THREE.AdditiveBlending, uniforms: { t: { value: 0 }, pr: { value: pixelRatio } },
    vertexShader: 'attribute float s; uniform float t, pr; varying float a; void main(){ vec3 p = position; p.y = mod(p.y + t*(0.08+s*0.12), 11.); p.x += sin(t*0.2+s*30.)*0.6; vec4 mv = modelViewMatrix*vec4(p,1.); a = 0.18+0.16*sin(t+s*20.); gl_PointSize = min(pr*(1.+s*2.)*7./-mv.z, 5.*pr); gl_Position = projectionMatrix*mv; }',
    fragmentShader: 'varying float a; void main(){ float d = length(gl_PointCoord-.5); gl_FragColor = vec4(vec3(0.6,0.7,1.)*a, 1.0 - smoothstep(.1,.5,d)); }',
  });
  const p = new THREE.Points(g, m); p.frustumCulled = false; decor.add(p); return m;
})();

// ───────── Bausteine der Welt ─────────
const UP = new THREE.Vector3(0, 1, 0);
const damp = (cur, target, dt, k = 12) => cur.lerp(target, 1 - Math.exp(-k * dt));
const cylGeo = new THREE.CylinderGeometry(1, 1, 1, 12, 1, true); cylGeo.translate(0, 0.5, 0);
function placeRod(mesh, a, b, r) {
  const d = b.clone().sub(a), len = d.length();
  mesh.position.copy(a); mesh.scale.set(r, Math.max(len, 1e-4), r);
  if (len > 1e-6) mesh.quaternion.setFromUnitVectors(UP, d.multiplyScalar(1 / len));
}
// Leuchtender Stab: heißer Kern + weicher Schein (Bloom macht den Rest)
function glowRod(c, r = 0.03, power = 2.6, haloScale = 4.2) {
  const g = new THREE.Group();
  const core = new THREE.Mesh(cylGeo, new THREE.MeshBasicMaterial({ color: hdr(c, power).lerp(new THREE.Color(power, power, power), 0.35), transparent: true }));
  const halo = new THREE.Mesh(cylGeo, new THREE.MeshBasicMaterial({ color: col(c), transparent: true, opacity: 0.16, blending: THREE.AdditiveBlending, depthWrite: false }));
  g.add(core, halo); g.userData = { core, halo, r };
  halo.visible = haloScale > 0;
  g.set = (a, b) => { placeRod(core, a, b, r); if (haloScale > 0) placeRod(halo, a, b, r * haloScale); };
  g.fade = (o) => { core.material.opacity = o; halo.material.opacity = 0.16 * o; };
  return g;
}
function dashed(c, opacity = 0.7) {
  const l = new THREE.Line(new THREE.BufferGeometry().setFromPoints([new THREE.Vector3(), new THREE.Vector3(0, 1, 0)]),
    new THREE.LineDashedMaterial({ color: col(c), dashSize: 0.16, gapSize: 0.12, transparent: true, opacity, depthWrite: false }));
  l.set = (a, b) => { l.geometry.setFromPoints([a, b]); l.computeLineDistances(); };
  return l;
}

const makers = {};
let luxTrail = 'none', luxHat = 'none';   // Zubehör aus der Werkstatt
let luxSkin = 'pink';   // Farbe von Lux' Leuchtband (Werkstatt)

// Punkt: leuchtende Kugel, Lot auf den Boden, optional Koordinaten-Quader
makers.point = (s) => {
  const g = new THREE.Group(), c = s.color ?? 'yellow', pos = V(s.p);
  const style = s.style ?? 'orb';
  let body;
  if (style === 'target') {
    body = new THREE.Group();
    const mat = new THREE.MeshStandardMaterial({ color: 0x222633, metalness: 1, roughness: 0.25, emissive: col(c), emissiveIntensity: 1.6 });
    const r1 = new THREE.Mesh(new THREE.TorusGeometry(0.34, 0.03, 12, 48), mat), r2 = new THREE.Mesh(new THREE.TorusGeometry(0.25, 0.025, 12, 48), mat);
    const core = new THREE.Mesh(new THREE.OctahedronGeometry(0.11, 0), new THREE.MeshBasicMaterial({ color: hdr(c, 3.2) }));
    r1.castShadow = r2.castShadow = true;
    body.add(r1, r2, core); body.userData = { r1, r2, core };
  } else if (style === 'ghost') {
    body = new THREE.Mesh(new THREE.SphereGeometry(0.13, 20, 20), new THREE.MeshBasicMaterial({ color: col(c), transparent: true, opacity: 0.35, wireframe: true }));
  } else {
    body = new THREE.Mesh(new THREE.SphereGeometry(0.13, 24, 24), new THREE.MeshBasicMaterial({ color: hdr(c, 3) }));
    body.castShadow = true;
  }
  const halo = glowSprite(c, style === 'ghost' ? 0.5 : 1.3, 0.75), pool = glowSprite(c, 1.6, 0.35);
  pool.material.depthTest = false;
  const drop = dashed(c, 0.55);
  const guide = new THREE.Group(); const gl = [dashed(c, 0.45), dashed(c, 0.45), dashed(c, 0.45)]; guide.add(...gl);
  const foot = new THREE.Mesh(new THREE.RingGeometry(0.09, 0.13, 24), new THREE.MeshBasicMaterial({ color: hdr(c, 1.6), transparent: true, opacity: 0.9, side: THREE.DoubleSide, depthWrite: false }));
  foot.rotation.x = -Math.PI / 2;
  let lab = null, labText = null;
  g.add(body, halo, drop, guide, foot);
  const cur = pos.clone(); let tgt = pos.clone(), t0 = Math.random() * 10, spec = s;
  return {
    g, pos: cur,
    update(n) {
      spec = n; tgt = V(n.p);
      if (n.label !== labText) { if (lab) g.remove(lab); lab = n.label ? label(n.label, hex(c)) : null; if (lab) g.add(lab); labText = n.label; }
      guide.visible = !!n.guides; drop.visible = n.drop !== false; foot.visible = n.drop !== false;
    },
    tick(dt, t) {
      damp(cur, tgt, dt, 10);
      body.position.copy(cur); halo.position.copy(cur);
      if (style === 'target') { const u = body.userData; u.r1.rotation.set(t * 0.9 + t0, t * 0.6, 0); u.r2.rotation.set(t * -0.7, t * 1.1 + t0, 0.5); u.core.rotation.y = t * 2; u.core.scale.setScalar(1 + 0.15 * Math.sin(t * 4)); }
      else body.scale.setScalar(1 + 0.06 * Math.sin(t * 3 + t0));
      const f = new THREE.Vector3(cur.x, 0.01, cur.z);
      drop.set(cur, f); foot.position.copy(f);
      if (guide.visible) { const a = new THREE.Vector3(0, 0.012, cur.z), o = new THREE.Vector3(0, 0.012, 0); gl[0].set(o, a); gl[1].set(a, f); gl[2].set(new THREE.Vector3(0, cur.y, 0), cur); }
      if (lab) lab.position.copy(cur).add(new THREE.Vector3(0, 0.55, 0));
    },
  };
};

// Vektor: Pfeil von → nach
makers.vector = (s) => {
  const g = new THREE.Group(), c = s.color ?? 'cyan';
  const rod = glowRod(c, 0.04, 2.2);
  const tip = new THREE.Mesh(new THREE.ConeGeometry(0.13, 0.36, 20), new THREE.MeshBasicMaterial({ color: hdr(c, 2.6) }));
  g.add(rod, tip);
  const a = V(s.from), b = V(s.to); let ta = a.clone(), tb = b.clone(), lab = null, labText = null;
  return {
    g,
    update(n) { ta = V(n.from); tb = V(n.to); if (n.label !== labText) { if (lab) g.remove(lab); lab = n.label ? label(n.label, hex(c)) : null; if (lab) g.add(lab); labText = n.label; } },
    tick(dt) {
      damp(a, ta, dt); damp(b, tb, dt);
      const d = b.clone().sub(a), len = d.length(); g.visible = len > 0.02;
      if (!g.visible) return; d.normalize();
      const head = Math.min(0.36, len * 0.5);
      rod.set(a, b.clone().addScaledVector(d, -head));
      tip.scale.setScalar(head / 0.36); tip.position.copy(b).addScaledVector(d, -head / 2); tip.quaternion.setFromUnitVectors(UP, d);
      if (lab) lab.position.copy(a).lerp(b, 0.5).add(new THREE.Vector3(0, 0.42, 0));
    },
  };
};

// Gerade / Laserstrahl: Stützpunkt p, Richtung d, Parameterbereich [t0, t1]
// Ebenes Achsenkreuz für Funktionsgraphen: beschriftet in Einheiten der Funktion (Ursprung s.p, Maßstab s.d)
makers.axes2d = (s) => {
  const g = new THREE.Group(), o = V(s.p), [sx, sy] = s.d, [[x0, x1], [y0, y1]] = s.corners;
  const P = (x, y) => new THREE.Vector3(o.x + x * sx, o.y + y * sy, o.z);
  const yx = Math.min(Math.max(0, x0), x1);                       // die y-Achse steht bei x = 0, sonst am Rand
  // Schrittweite der Beschriftung: 1, 2 oder 5 mal Zehnerpotenz – so passt jeder Maßstab (auch 0,1 oder 500)
  const step = (k, gap) => { for (let p = -4; p <= 8; p++) for (const m of [1, 2, 5]) { const n = m * 10 ** p; if (n * k >= gap) return n; } return 1e9; };
  const fmt = (v) => String(Number(v.toPrecision(6))).replace('-', '−').replace('.', ',');
  const main = [], grid = [], ex = 0.45 / sx, ey = 0.45 / sy;
  main.push(P(x0 - ex * 0.5, 0), P(x1 + ex, 0), P(yx, y0 - ey * 0.5), P(yx, y1 + ey));
  const mkTip = (p, up) => { const t = new THREE.Mesh(new THREE.ConeGeometry(0.08, 0.26, 12), new THREE.MeshBasicMaterial({ color: hdr('white', 1.2) })); t.position.copy(p); if (!up) t.rotation.z = -Math.PI / 2; g.add(t); };
  mkTip(P(x1 + ex, 0), false); mkTip(P(yx, y1 + ey), true);
  const name = (t, p) => { const l = label(t, '#ffffff', 0.95, false); l.position.copy(p); g.add(l); };
  name('x', P(x1 + ex, 0).add(new THREE.Vector3(0.1, -0.38, 0))); name('y', P(yx, y1 + ey).add(new THREE.Vector3(-0.38, 0.05, 0)));
  const num = (v, p) => { const l = label(fmt(v), '#dfe6ff', 0.62, false); l.material.opacity = 0.9; l.position.copy(p); g.add(l); };
  const dx = step(sx, 0.9), dy = step(sy, 0.55);   // Zahlen an der x-Achse sind breiter und brauchen mehr Abstand
  for (let i = Math.ceil(x0 / dx - 1e-9); i * dx <= x1 + 1e-9; i++) { const x = i * dx;
    if (x !== yx) grid.push(P(x, y0), P(x, y1));
    main.push(P(x, 0).add(new THREE.Vector3(0, -0.09, 0)), P(x, 0).add(new THREE.Vector3(0, 0.09, 0)));
    if (x !== 0) num(x, P(x, 0).add(new THREE.Vector3(0, -0.34, 0)));
  }
  for (let i = Math.ceil(y0 / dy - 1e-9); i * dy <= y1 + 1e-9; i++) { const y = i * dy;
    if (y !== 0) grid.push(P(x0, y), P(x1, y));
    main.push(P(yx, y).add(new THREE.Vector3(-0.09, 0, 0)), P(yx, y).add(new THREE.Vector3(0.09, 0, 0)));
    if (y !== 0) num(y, P(yx, y).add(new THREE.Vector3(-0.36, 0, 0)));
  }
  if (yx === 0) num(0, P(0, 0).add(new THREE.Vector3(-0.3, -0.32, 0)));
  const seg = (pts, c, op) => new THREE.LineSegments(new THREE.BufferGeometry().setFromPoints(pts), new THREE.LineBasicMaterial({ color: c, transparent: true, opacity: op, depthWrite: false }));
  g.add(seg(grid, 0x8f9dff, 0.2), seg(main, 0xffffff, 0.95));
  return { g, update() {}, tick() {} };
};

// Showbühne (Stochastik): Leuchtringe und ein Lauflicht statt Gitter und Achsen
makers.podium = (s) => {
  const g = new THREE.Group(), o = V(s.p); g.position.copy(o);
  const ring = (r, w, c, op) => { const m = new THREE.Mesh(new THREE.RingGeometry(r - w, r, 96), new THREE.MeshBasicMaterial({ color: hdr(c, 1.6), transparent: true, opacity: op, side: THREE.DoubleSide, depthWrite: false, blending: THREE.AdditiveBlending })); m.rotation.x = -Math.PI / 2; m.position.y = 0.012; g.add(m); return m; };
  ring(5.2, 0.07, 'pink', 0.9); ring(4.4, 0.03, 'violet', 0.6); ring(3.3, 0.05, 'cyan', 0.75); ring(2.0, 0.03, 'violet', 0.5);
  const disc = new THREE.Mesh(new THREE.CircleGeometry(5.2, 96), new THREE.MeshBasicMaterial({ color: col('violet'), transparent: true, opacity: 0.07, depthWrite: false })); disc.rotation.x = -Math.PI / 2; disc.position.y = 0.008; g.add(disc);
  const N = 36, pos = new Float32Array(N * 3), colr = new Float32Array(N * 3);
  for (let i = 0; i < N; i++) { const a = (i / N) * Math.PI * 2; pos[i * 3] = Math.cos(a) * 4.8; pos[i * 3 + 1] = 0.05; pos[i * 3 + 2] = Math.sin(a) * 4.8; }
  const geo = new THREE.BufferGeometry(); geo.setAttribute('position', new THREE.BufferAttribute(pos, 3)); geo.setAttribute('color', new THREE.BufferAttribute(colr, 3));
  const bulbs = new THREE.Points(geo, new THREE.PointsMaterial({ size: 0.5, map: glowTex, vertexColors: true, transparent: true, depthWrite: false, blending: THREE.AdditiveBlending })); g.add(bulbs);
  const pal = [col('pink'), col('yellow'), col('cyan')];
  return { g, update() {}, tick(dt, t) {
    for (let i = 0; i < N; i++) { const k = 0.35 + 0.65 * Math.pow(0.5 + 0.5 * Math.sin(t * 2.2 - i * 0.52), 3), c = pal[i % 3]; colr[i * 3] = c.r * k * 1.6; colr[i * 3 + 1] = c.g * k * 1.6; colr[i * 3 + 2] = c.b * k * 1.6; }
    geo.attributes.color.needsUpdate = true;
  } };
};

makers.line = (s) => {
  const g = new THREE.Group(), c = s.color ?? 'cyan';
  const rod = s.dashed ? null : glowRod(c, s.thin ? 0.018 : 0.035, s.thin ? 1.6 : 3);
  const dl = s.dashed ? dashed(c, 0.8) : null;
  g.add(rod ?? dl);
  const a = V(s.p).addScaledVector(V(s.d), s.t0 ?? 0), b = V(s.p).addScaledVector(V(s.d), s.t1 ?? 1);
  let ta = a.clone(), tb = b.clone(), lab = null, labText = null;
  return {
    g,
    update(n) {
      ta = V(n.p).addScaledVector(V(n.d), n.t0 ?? 0); tb = V(n.p).addScaledVector(V(n.d), n.t1 ?? 1);
      if (n.label !== labText) { if (lab) g.remove(lab); lab = n.label ? label(n.label, hex(c)) : null; if (lab) g.add(lab); labText = n.label; }
    },
    tick(dt) { damp(a, ta, dt); damp(b, tb, dt); (rod ?? dl).set(a, b); if (lab) lab.position.copy(a).lerp(b, 0.82).add(new THREE.Vector3(0, 0.4, 0)); },
  };
};

// Ebene: als Glasscheibe. Entweder Ecken (corners) oder n·x = d, beschnitten auf den Raum
function planePolygon(n, d) {
  const { min, max } = bounds, pts = [], N = new THREE.Vector3(...n);
  const c = [min, max], e = [];
  for (let i = 0; i < 2; i++) for (let j = 0; j < 2; j++) {
    e.push([[min[0], c[i][1], c[j][2]], [max[0], c[i][1], c[j][2]]], [[c[i][0], min[1], c[j][2]], [c[i][0], max[1], c[j][2]]], [[c[i][0], c[j][1], min[2]], [c[i][0], c[j][1], max[2]]]);
  }
  for (const [p, q] of e) {
    const P = new THREE.Vector3(...p), Q = new THREE.Vector3(...q), fp = N.dot(P) - d, fq = N.dot(Q) - d;
    if (Math.abs(fp) < 1e-9) pts.push(P); else if (Math.abs(fq) < 1e-9) pts.push(Q); else if (fp * fq < 0) pts.push(P.clone().lerp(Q, fp / (fp - fq)));
  }
  const uniq = pts.filter((p, i) => pts.findIndex((q) => q.distanceTo(p) < 1e-6) === i);
  if (uniq.length < 3) return [];
  const ctr = uniq.reduce((s, p) => s.add(p), new THREE.Vector3()).multiplyScalar(1 / uniq.length);
  const u = uniq[0].clone().sub(ctr).normalize(), w = N.clone().normalize().cross(u);
  uniq.sort((p, q) => Math.atan2(p.clone().sub(ctr).dot(w), p.clone().sub(ctr).dot(u)) - Math.atan2(q.clone().sub(ctr).dot(w), q.clone().sub(ctr).dot(u)));
  return uniq.map((p) => [p.x, p.y, p.z]);
}
makers.plane = (s) => {
  const g = new THREE.Group(), c = s.color ?? 'violet';
  const mat = new THREE.ShaderMaterial({
    transparent: true, depthWrite: false, side: THREE.DoubleSide,
    uniforms: { c: { value: col(c) }, t: { value: 0 }, o: { value: s.opacity ?? 1 } },
    vertexShader: 'varying vec3 wp, n, v; void main(){ vec4 w = modelMatrix*vec4(position,1.); wp = w.xyz; n = normalize(mat3(modelMatrix)*normal); v = normalize(cameraPosition-w.xyz); gl_Position = projectionMatrix*viewMatrix*w; }',
    fragmentShader: `varying vec3 wp, n, v; uniform vec3 c; uniform float t, o;
      float gridLine(float x){ float f = abs(fract(x-.5)-.5)/fwidth(x); return 1.-min(f,1.); }
      void main(){ float fr = pow(clamp(1.-abs(dot(normalize(n), normalize(v))), 0., 1.), 2.5);
        vec3 a = abs(n); vec2 uv = a.y > a.x && a.y > a.z ? wp.xz : (a.x > a.z ? wp.zy : wp.xy);
        float g = max(gridLine(uv.x), gridLine(uv.y));
        float sweep = smoothstep(.92,1.,sin((wp.x+wp.y+wp.z)*.9 - t*1.2));
        vec3 colr = c*(0.22 + fr*1.5 + g*0.55 + sweep*0.35);
        gl_FragColor = vec4(colr, (0.16 + fr*0.5 + g*0.2 + sweep*0.08)*o); }`,
  });
  const mesh = new THREE.Mesh(new THREE.BufferGeometry(), mat); mesh.renderOrder = 5;
  const edge = new THREE.LineLoop(new THREE.BufferGeometry(), new THREE.LineBasicMaterial({ color: hdr(c, 2.2), transparent: true, opacity: 0.95 }));
  g.add(mesh, edge);
  let cur = [], tgt = [], lab = null, labText = null;
  const rebuild = () => {
    const pts = cur; if (pts.length < 3) { mesh.visible = edge.visible = false; return; } mesh.visible = edge.visible = true;
    const pos = []; for (let i = 1; i < pts.length - 1; i++) pos.push(pts[0], pts[i], pts[i + 1]);
    mesh.geometry.setFromPoints(pos); mesh.geometry.computeVertexNormals(); edge.geometry.setFromPoints(pts);
    if (lab) lab.position.copy(pts[0]).lerp(pts[Math.min(2, pts.length - 1)], 0.5).lerp(pts[1], 0.55);
  };
  return {
    g,
    update(n) {
      const raw = n.corners ?? planePolygon(n.n, n.rhs);
      tgt = raw.map((p) => V(p));
      if (cur.length !== tgt.length) cur = tgt.map((p) => p.clone());
      if (n.label !== labText) { if (lab) g.remove(lab); lab = n.label ? label(n.label, hex(c)) : null; if (lab) g.add(lab); labText = n.label; }
      rebuild();
    },
    tick(dt, t) { mat.uniforms.t.value = t; let moved = false; cur.forEach((p, i) => { if (p.distanceToSquared(tgt[i]) > 1e-6) { damp(p, tgt[i], dt); moved = true; } }); if (moved) rebuild(); },
  };
};

// Kanten eines Körpers (Liste von Punktpaaren) – z. B. Pyramide, Zeltdach, Haus
makers.edges = (s) => {
  const g = new THREE.Group(), c = s.color ?? 'white';
  const rods = s.segments.map(() => glowRod(c, 0.02, 1.5)); g.add(...rods);
  let faces = null;
  if (s.faces) {
    faces = new THREE.Mesh(new THREE.BufferGeometry(), new THREE.MeshPhysicalMaterial({ color: col(c), transparent: true, opacity: 0.2, roughness: 0.15, metalness: 0.1, side: THREE.DoubleSide, depthWrite: false, emissive: col(c), emissiveIntensity: 0.08 }));
    g.add(faces);
  }
  return {
    g,
    update(n) {
      n.segments.forEach((sg, i) => rods[i] && rods[i].set(V(sg[0]), V(sg[1])));
      if (faces && n.faces) { const pos = []; n.faces.forEach((f) => { for (let i = 1; i < f.length - 1; i++) pos.push(V(f[0]), V(f[i]), V(f[i + 1])); }); faces.geometry.setFromPoints(pos); faces.geometry.computeVertexNormals(); }
    },
    tick() {},
  };
};

// Kugel: Discokugel aus Glas mit Längen- und Breitenkreisen; der Radius ändert sich weich
makers.sphere = (s) => {
  const g = new THREE.Group(), c = s.color ?? 'cyan';
  const mat = new THREE.ShaderMaterial({
    transparent: true, depthWrite: false, side: THREE.DoubleSide,
    uniforms: { c: { value: col(c) }, t: { value: 0 } },
    vertexShader: 'varying vec3 n, v, lp; void main(){ lp = position; vec4 w = modelMatrix*vec4(position,1.); n = normalize(mat3(modelMatrix)*normal); v = normalize(cameraPosition-w.xyz); gl_Position = projectionMatrix*viewMatrix*w; }',
    fragmentShader: `varying vec3 n, v, lp; uniform vec3 c; uniform float t;
      void main(){ float fr = pow(clamp(1.-abs(dot(normalize(n), normalize(v))), 0., 1.), 2.2);
        vec3 q = normalize(lp); float lon = atan(q.z, q.x) + t*0.25, lat = asin(clamp(q.y, -1., 1.));
        vec2 cell = vec2(lon, lat) * 7.6394; vec2 f = abs(fract(cell) - .5);
        float tile = 1.0 - smoothstep(.40, .47, max(f.x, f.y));
        float spark = step(.93, fract(sin(dot(floor(cell), vec2(12.9898, 78.233))) * 43758.5453 + t*0.6));
        vec3 colr = c*(0.10 + fr*1.6 + tile*0.16) + vec3(1.)*spark*tile*0.9;
        gl_FragColor = vec4(colr, 0.10 + fr*0.55 + tile*0.07 + spark*tile*0.5); }`,
  });
  const ball = new THREE.Mesh(new THREE.SphereGeometry(1, 48, 32), mat); ball.renderOrder = 4;
  const ring = new THREE.LineLoop(new THREE.BufferGeometry().setFromPoints(Array.from({ length: 64 }, (_, i) => new THREE.Vector3(Math.cos(i / 64 * Math.PI * 2), 0, Math.sin(i / 64 * Math.PI * 2)))),
    new THREE.LineBasicMaterial({ color: hdr(c, 1.6), transparent: true, opacity: 0.7 }));
  g.add(ball, ring);
  const cur = V(s.p); let tgt = cur.clone(), r = s.rhs, tr = s.rhs, lab = null, labText = null;
  return {
    g, pop: () => ({ p: cur, c }), ball: () => ({ p: cur, r }),
    update(n) { tgt = V(n.p); tr = n.rhs; if (n.label !== labText) { if (lab) g.parent && g.remove(lab); lab = n.label ? label(n.label, hex(c)) : null; labText = n.label; if (lab) world.add(lab); } },
    tick(dt, t) { damp(cur, tgt, dt); r += (tr - r) * (1 - Math.exp(-10 * dt)); ball.position.copy(cur); ball.scale.setScalar(r); ring.position.copy(cur); ring.scale.setScalar(r); mat.uniforms.t.value = t;
      if (lab) lab.position.copy(cur).add(new THREE.Vector3(0, r + 0.4, 0)); },
  };
};

// Rotationskörper: ein Profil (x, Radius) dreht sich um eine Achse parallel zur x₂-Achse
makers.lathe = (s) => {
  const g = new THREE.Group(), c = s.color ?? 'violet';
  const mat = new THREE.ShaderMaterial({
    transparent: true, depthWrite: false, side: THREE.DoubleSide,
    uniforms: { c: { value: col(c) }, t: { value: 0 } },
    vertexShader: 'varying vec3 n, v, lp; void main(){ lp = position; vec4 w = modelMatrix*vec4(position,1.); n = normalize(mat3(modelMatrix)*normal); v = normalize(cameraPosition-w.xyz); gl_Position = projectionMatrix*viewMatrix*w; }',
    fragmentShader: `varying vec3 n, v, lp; uniform vec3 c; uniform float t;
      void main(){ float fr = pow(clamp(1.-abs(dot(normalize(n), normalize(v))), 0., 1.), 2.0);
        float ring = 1.0 - smoothstep(.03, .08, abs(fract(lp.x*2. - t*0.15) - .5));
        float rib = 1.0 - smoothstep(.02, .06, abs(fract(atan(lp.z, lp.y)*1.9099) - .5));
        gl_FragColor = vec4(c*(0.16 + fr*1.5 + ring*0.5 + rib*0.25), 0.14 + fr*0.5 + ring*0.18 + rib*0.08); }`,
  });
  const body = new THREE.Mesh(new THREE.BufferGeometry(), mat); body.renderOrder = 4;
  const curve = glowRodChain(c);
  const axis = dashed('white', 0.5);
  g.add(body, curve.g, axis);
  let key = '';
  return {
    g,
    update(n) {
      const k = JSON.stringify(n.corners) + n.p; if (k === key) return; key = k;
      const pts = n.corners.map((q) => new THREE.Vector2(Math.max(q[1], 0.001), q[0]));
      const geo = new THREE.LatheGeometry(pts, 56); geo.rotateZ(-Math.PI / 2);
      body.geometry.dispose(); body.geometry = geo;
      const o = V(n.p); body.position.copy(o);
      // leuchtende Kurve f(x) über der Achse und die Drehachse
      curve.set(n.corners.map((q) => o.clone().add(new THREE.Vector3(q[0], q[1], 0))));
      const x0 = n.corners[0][0], x1 = n.corners[n.corners.length - 1][0];
      axis.set(o.clone().add(new THREE.Vector3(x0 - 0.8, 0, 0)), o.clone().add(new THREE.Vector3(x1 + 0.8, 0, 0)));
    },
    tick(dt, t) { mat.uniforms.t.value = t; },
  };
};
// Leuchtender Linienzug (für Kurven)
function glowRodChain(c, radius = 0.028, power = 2.6, haloScale = 4.2) {
  const g = new THREE.Group(); let rods = [];
  return { g, set(points) {
    while (rods.length < points.length - 1) { const r = glowRod(c, radius, power, haloScale); rods.push(r); g.add(r); }
    rods.forEach((r, i) => { r.visible = i < points.length - 1; if (r.visible) r.set(points[i], points[i + 1]); });
  } };
}

// Achterbahn: zwei Schienen mit Schwellen und Stützen entlang einer Kurve – zum Mitfahren aus der Fahrersicht
makers.track = (s) => {
  const g = new THREE.Group(), c = s.color ?? 'cyan';
  const railL = glowRodChain(c, 0.022, 2.0, 0), railR = glowRodChain(c, 0.022, 2.0, 0), ties = new THREE.Group(), posts = new THREE.Group();
  const cart = new THREE.Group();
  cart.add(new THREE.Mesh(new THREE.SphereGeometry(0.2, 20, 16), new THREE.MeshBasicMaterial({ color: hdr('yellow', 3) })), glowSprite('yellow', 1.6, 0.8));
  cart.visible = false;
  g.add(railL.g, railR.g, ties, posts, cart);
  const side = new THREE.Vector3(0, 0, 1), tieGeo = new THREE.BoxGeometry(0.06, 0.03, 0.5), tieMat = new THREE.MeshBasicMaterial({ color: hdr(c, 0.55), transparent: true, opacity: 0.8 });
  let pts = [], key = '';
  return {
    g, isTrack: true,
    update(n) {
      const k = JSON.stringify(n.corners); if (k === key) return; key = k;
      pts = n.corners.map((q) => V(q));
      if (n.style === 'line') { railL.set(pts); railR.g.visible = false; ties.clear(); posts.clear(); return; }
      railL.set(pts.map((q) => q.clone().addScaledVector(side, 0.2))); railR.set(pts.map((q) => q.clone().addScaledVector(side, -0.2)));
      ties.clear(); posts.clear();
      pts.forEach((q, i) => {
        const next = pts[Math.min(i + 1, pts.length - 1)], prev = pts[Math.max(i - 1, 0)];
        const tie = new THREE.Mesh(tieGeo, tieMat); tie.position.copy(q).add(new THREE.Vector3(0, -0.03, 0));
        tie.rotation.z = Math.atan2(next.y - prev.y, next.x - prev.x); ties.add(tie);
        if (i % 6 === 0 && q.y > 0.3) { const post = dashed(c, 0.35); post.set(q.clone().add(new THREE.Vector3(0, -0.05, 0)), new THREE.Vector3(q.x, 0.01, q.z)); posts.add(post); }
      });
    },
    // Ort auf der Bahn für u in 0..1
    at(u) { if (pts.length < 2) return new THREE.Vector3(); const f = THREE.MathUtils.clamp(u, 0, 1) * (pts.length - 1), i = Math.min(Math.floor(f), pts.length - 2); return pts[i].clone().lerp(pts[i + 1], f - i); },
    showCart(p) { cart.visible = !!p; if (p) cart.position.copy(p).add(new THREE.Vector3(0, 0.22, 0)); },
    tick() {},
  };
};

// Lux, die Lichtdrohne
makers.drone = (s) => {
  const g = new THREE.Group(), body = new THREE.Group(); g.add(body); body.scale.setScalar(1.75);
  const shell = new THREE.MeshPhysicalMaterial({ color: 0xaab2c6, metalness: 0.35, roughness: 0.25, clearcoat: 1, clearcoatRoughness: 0.08 });
  const dark = new THREE.MeshStandardMaterial({ color: 0x141826, metalness: 0.85, roughness: 0.3 });
  const hull = new THREE.Mesh(new THREE.SphereGeometry(0.26, 40, 28), shell); hull.scale.set(1, 0.72, 1); hull.castShadow = true;
  const visor = new THREE.Mesh(new THREE.SphereGeometry(0.2, 32, 20, 0, Math.PI * 2, 0, Math.PI * 0.42), new THREE.MeshPhysicalMaterial({ color: 0x05070f, metalness: 0.9, roughness: 0.08, clearcoat: 1 }));
  visor.rotation.x = Math.PI / 2; visor.position.set(0, 0.015, 0.105); visor.scale.set(1.02, 1, 0.9);
  const eyeM = new THREE.MeshBasicMaterial({ color: hdr('cyan', 3.2) });
  const eyes = [-1, 1].map((k) => { const e = new THREE.Mesh(new THREE.SphereGeometry(0.036, 16, 12), eyeM); e.position.set(k * 0.07, 0.035, 0.292); e.scale.set(1, 1.5, 0.45); return e; });
  const band = new THREE.Mesh(new THREE.TorusGeometry(0.262, 0.016, 12, 64), new THREE.MeshBasicMaterial({ color: hdr(luxSkin, 2.4) })); band.rotation.x = Math.PI / 2; band.position.y = -0.03;
  body.add(hull, visor, ...eyes, band);
  const rotors = [];
  for (let i = 0; i < 4; i++) {
    const a = Math.PI / 4 + (i * Math.PI) / 2, arm = new THREE.Mesh(new THREE.CylinderGeometry(0.018, 0.024, 0.3, 10), dark);
    arm.rotation.z = Math.PI / 2; arm.rotation.y = -a; arm.position.set(Math.cos(a) * 0.32, 0.0, Math.sin(a) * 0.32); arm.castShadow = true;
    const hub = new THREE.Group(); hub.position.set(Math.cos(a) * 0.46, 0.03, Math.sin(a) * 0.46);
    const ring = new THREE.Mesh(new THREE.TorusGeometry(0.13, 0.014, 10, 40), shell); ring.rotation.x = Math.PI / 2; ring.castShadow = true;
    const motor = new THREE.Mesh(new THREE.CylinderGeometry(0.03, 0.035, 0.07, 14), dark);
    const disc = new THREE.Mesh(new THREE.CircleGeometry(0.118, 28), new THREE.MeshBasicMaterial({ color: 0x9fd8ff, transparent: true, opacity: 0.13, side: THREE.DoubleSide, depthWrite: false })); disc.rotation.x = -Math.PI / 2; disc.position.y = 0.03;
    const blade = new THREE.Mesh(new THREE.BoxGeometry(0.23, 0.004, 0.022), dark); blade.position.y = 0.035;
    hub.add(ring, motor, disc, blade); body.add(arm, hub); rotors.push(blade);
  }
  const lamp = new THREE.Mesh(new THREE.SphereGeometry(0.05, 16, 12), new THREE.MeshBasicMaterial({ color: hdr('yellow', 3) })); lamp.position.y = -0.19; body.add(lamp);
  // Aufsätze: Antenne, Lichtring, Krone
  const hats = {
    antenna: (() => { const h = new THREE.Group(); const rod = new THREE.Mesh(new THREE.CylinderGeometry(0.008, 0.008, 0.26, 6), dark); rod.position.y = 0.3;
      const tip = new THREE.Mesh(new THREE.SphereGeometry(0.035, 12, 10), new THREE.MeshBasicMaterial({ color: hdr('red', 3) })); tip.position.y = 0.44; h.add(rod, tip); return h; })(),
    halo: (() => { const h = new THREE.Mesh(new THREE.TorusGeometry(0.2, 0.018, 10, 40), new THREE.MeshBasicMaterial({ color: hdr('yellow', 3) })); h.rotation.x = Math.PI / 2; h.position.y = 0.36; return h; })(),
    crown: (() => { const h = new THREE.Group(); for (let i = 0; i < 6; i++) { const a = i / 6 * Math.PI * 2, c = new THREE.Mesh(new THREE.ConeGeometry(0.045, 0.14, 8), new THREE.MeshBasicMaterial({ color: hdr('yellow', 2.6) }));
      c.position.set(Math.cos(a) * 0.13, 0.26, Math.sin(a) * 0.13); h.add(c); } return h; })(),
  };
  Object.values(hats).forEach((h) => { h.visible = false; body.add(h); });
  // Leuchtspur: eine Kette kleiner Lichter, die hinter Lux verblassen
  const trailN = 40, trailPos = new Float32Array(trailN * 3), trailCol = new Float32Array(trailN * 3);
  const trailGeo = new THREE.BufferGeometry(); trailGeo.setAttribute('position', new THREE.BufferAttribute(trailPos, 3)); trailGeo.setAttribute('color', new THREE.BufferAttribute(trailCol, 3));
  const trail = new THREE.Points(trailGeo, new THREE.PointsMaterial({ size: 0.55, map: glowTex, vertexColors: true, transparent: true, depthWrite: false, blending: THREE.AdditiveBlending }));
  trail.frustumCulled = false; g.add(trail); let trailAt = 0, trailHead = 0, trailFilled = false;
  const halo = glowSprite('cyan', 1.1, 0.35); body.add(halo);
  const drop = dashed('cyan', 0.35), foot = glowSprite('cyan', 1.2, 0.3); foot.material.depthTest = false; g.add(drop, foot);
  const cur = V(s.p), vel = new THREE.Vector3(); let tgt = cur.clone(), mood = 'idle', moodAt = -99, lab = null, labText = null;
  return {
    g, pos: cur, skin(c) { band.material.color.copy(hdr(c, 2.4)); },
    update(n) { tgt = V(n.p); const m0 = mood; mood = n.mood ?? 'idle'; if (mood !== m0) moodAt = clock.elapsedTime; eyeM.color.copy(hdr(mood === 'sad' ? 'orange' : mood === 'happy' ? 'green' : 'cyan', 3.2));
      if (n.label !== labText) { if (lab) g.remove(lab); lab = n.label ? label(n.label, hex('cyan')) : null; if (lab) g.add(lab); labText = n.label; } },
    tick(dt, t) {
      // Feder-Flug mit Neigung in Flugrichtung
      const acc = tgt.clone().sub(cur).multiplyScalar(38).addScaledVector(vel, -9); vel.addScaledVector(acc, dt); cur.addScaledVector(vel, dt);
      body.position.copy(cur).add(new THREE.Vector3(0, Math.sin(t * 2.2) * 0.05, 0));
      body.rotation.z = THREE.MathUtils.clamp(-vel.x * 0.12, -0.5, 0.5); body.rotation.x = THREE.MathUtils.clamp(vel.z * 0.12, -0.5, 0.5);
      const look = Math.atan2(camera.position.x - cur.x, camera.position.z - cur.z); body.rotation.y += (look - body.rotation.y) * Math.min(1, dt * 4);
      const since = t - moodAt;
      if (mood === 'happy') {
        // Jubel: Spirale nach oben mit Salto, danach fröhliches Wackeln
        if (since < 1.4) { const k = since / 1.4, e = Math.sin(k * Math.PI); body.position.add(new THREE.Vector3(Math.cos(k * 12) * 0.55 * e, e * 0.9, Math.sin(k * 12) * 0.55 * e)); body.rotation.z += k * Math.PI * 2; }
        else body.rotation.y += Math.sin(t * 9) * 0.25;
      } else if (mood === 'sad') {
        // Daneben: kurz den Kopf schütteln und absacken
        const k = Math.max(0, 1 - since / 1.2); body.rotation.y += Math.sin(since * 22) * 0.4 * k; body.position.y -= 0.18 * Math.min(1, since * 3); body.rotation.x += 0.25;
      }
      rotors.forEach((r, i) => (r.rotation.y = t * 38 * (i % 2 ? 1 : -1)));
      const blink = (t % 3.7) < 0.12 ? 0.15 : 1; eyes.forEach((e) => (e.scale.y = (mood === 'happy' ? 0.8 : 1.5) * blink));
      const f = new THREE.Vector3(cur.x, 0.02, cur.z); drop.set(body.position, f); foot.position.copy(f);
      if (lab) lab.position.copy(body.position).add(new THREE.Vector3(0, 0.6, 0));
      Object.entries(hats).forEach(([k, h]) => { h.visible = k === luxHat; }); if (luxHat === 'halo') hats.halo.rotation.z = t * 2;
      trail.visible = luxTrail !== 'none';
      if (trail.visible && t - trailAt > 0.022) {
        trailAt = t; if (!trailFilled) { for (let i = 0; i < trailN; i++) trailPos.set([body.position.x, body.position.y, body.position.z], i * 3); trailFilled = true; }
        trailHead = (trailHead + 1) % trailN; trailPos.set([body.position.x, body.position.y - 0.1, body.position.z], trailHead * 3);
        for (let i = 0; i < trailN; i++) { const age = ((trailHead - i + trailN) % trailN) / trailN, k = (1 - age) * (1 - age) * 3.2;
          const c = luxTrail === 'rainbow' ? new THREE.Color().setHSL((t * 0.4 + age) % 1, 1, 0.6) : col(luxSkin); trailCol.set([c.r * k, c.g * k, c.b * k], i * 3); }
        trailGeo.attributes.position.needsUpdate = true; trailGeo.attributes.color.needsUpdate = true;
      }
    },
  };
};

// Griff zum Ziehen entlang der drei Achsen
makers.handle = (s) => {
  const g = new THREE.Group(), c = s.color ?? 'yellow';
  const ball = new THREE.Mesh(new THREE.SphereGeometry(0.17, 24, 24), new THREE.MeshBasicMaterial({ color: hdr(c, 2.4), transparent: true, opacity: 0.95 }));
  const ring = new THREE.Mesh(new THREE.RingGeometry(0.26, 0.31, 40), new THREE.MeshBasicMaterial({ color: hdr('white', 1.6), transparent: true, opacity: 0.9, side: THREE.DoubleSide, depthTest: false })); ring.renderOrder = 40;
  g.add(ball, ring);
  const arrows = [['pink', [1, 0, 0]], ['green', [0, 1, 0]], ['cyan', [0, 0, 1]]].map(([ac, d], i) => {
    const a = new THREE.Group(), dir = V(d);
    [-1, 1].forEach((k) => {
      const cone = new THREE.Mesh(new THREE.ConeGeometry(0.1, 0.26, 16), new THREE.MeshBasicMaterial({ color: hdr(ac, 2.2), depthTest: false, transparent: true }));
      cone.renderOrder = 41; cone.position.copy(dir.clone().multiplyScalar(k * 0.62)); cone.quaternion.setFromUnitVectors(UP, dir.clone().multiplyScalar(k)); a.add(cone);
    });
    a.userData.axis = i; a.visible = !s.axes || s.axes.includes(i); g.add(a); return a;
  });
  const cur = V(s.p); let tgt = cur.clone();
  return {
    g, pos: cur, isHandle: true, arrows,
    update(n) { tgt = V(n.p); arrows.forEach((a, i) => (a.visible = !n.axes || n.axes.includes(i))); },
    tick(dt, t) { damp(cur, tgt, dt, 20); g.position.copy(cur); ring.quaternion.copy(camera.quaternion); ring.scale.setScalar(1 + 0.08 * Math.sin(t * 4)); },
  };
};

// ───────── Zustand übernehmen ─────────
const objects = new Map();
function setState(state) {
  if (state.bounds && JSON.stringify(state.bounds) !== JSON.stringify(bounds)) { bounds = state.bounds; buildGrid(); frameBounds(false); }
  if (!!state.flat !== flat || !!state.plain !== plain) {
    flat = !!state.flat; plain = !!state.plain; if (axesGroup) axesGroup.visible = !flat && !plain; gridGroup.visible = !plain;
    // Funktionsgraphen schaut man von vorn an – sonst täuscht die Perspektive beim Ablesen; die Showbühne der Stochastik leicht von oben
    frameBounds(false);
    if (flat) { cam.tAz = 0; cam.tEl = 0.1; cam.tTarget.copy(V([4, (bounds.min[1] + bounds.max[1]) / 2, 3.0])); cam.tDist *= 0.8; }
    else if (plain) { cam.tAz = 0.16; cam.tEl = 0.2; cam.tTarget.copy(V([4, (bounds.min[1] + bounds.max[1]) / 2, 2.7])); cam.tDist *= 0.8; }
    else { cam.tAz = 0.62; cam.tEl = 0.42; }
  }
  const seen = new Set();
  for (const s of state.objects ?? []) {
    seen.add(s.id);
    let o = objects.get(s.id);
    if (o && o.sig !== sig(s)) { world.remove(o.g); objects.delete(s.id); o = null; }
    if (!o) { o = makers[s.type](s); o.sig = sig(s); o.id = s.id; objects.set(s.id, o); world.add(o.g); o.g.scale.setScalar(0.001); o.born = clock.elapsedTime; }
    o.update(s);
  }
  // Was verschwindet, zerplatzt in Funken (z. B. abgeschossene Kugeln)
  for (const [id, o] of objects) if (!seen.has(id)) { if (o.pop && clock.elapsedTime - o.born > 0.5) { const q = o.pop(); burst(fromV(q.p), q.c, 70, 0.9); shake = Math.max(shake, 0.18); } world.remove(o.g); objects.delete(id); }
}
// Woran man erkennt, dass ein Ding neu gebaut werden muss. Das Achsenkreuz ist starr gebaut: Ändert sich sein Ort oder Maßstab, entsteht es neu.
const sig = (s) => [s.type, s.color, s.style, s.dashed, s.thin, s.segments?.length, !!s.faces, s.type === 'axes2d' ? JSON.stringify([s.p, s.d, s.corners]) : ''].join('|');

// ───────── Effekte ─────────
const effects = [];
// Funkenregen
function burst(p, c = 'yellow', n = 90, power = 1) {
  const pos = new Float32Array(n * 3), vel = [], o = V(p);
  for (let i = 0; i < n; i++) { const d = new THREE.Vector3().randomDirection().multiplyScalar((1.5 + Math.random() * 4.5) * power); d.y = Math.abs(d.y) * 0.9 + 1; vel.push(d); pos.set([o.x, o.y, o.z], i * 3); }
  const g = new THREE.BufferGeometry(); g.setAttribute('position', new THREE.BufferAttribute(pos, 3));
  const m = new THREE.PointsMaterial({ color: hdr(c, 2.5), size: 0.11, map: glowTex, transparent: true, depthWrite: false, blending: THREE.AdditiveBlending });
  const pts = new THREE.Points(g, m); pts.frustumCulled = false; fx.add(pts);
  const ring = new THREE.Mesh(new THREE.RingGeometry(0.92, 1, 64), new THREE.MeshBasicMaterial({ color: hdr(c, 2.5), transparent: true, side: THREE.DoubleSide, depthWrite: false, blending: THREE.AdditiveBlending }));
  ring.position.copy(o); fx.add(ring);
  const flash = glowSprite(c, 0.1, 1); flash.position.copy(o); fx.add(flash);
  let t = 0;
  effects.push((dt) => {
    t += dt; const a = g.attributes.position;
    for (let i = 0; i < n; i++) { vel[i].y -= 6.5 * dt; vel[i].multiplyScalar(1 - 1.2 * dt); let y = a.getY(i) + vel[i].y * dt; if (y < 0.03) { y = 0.03; vel[i].y *= -0.45; } a.setXYZ(i, a.getX(i) + vel[i].x * dt, y, a.getZ(i) + vel[i].z * dt); }
    a.needsUpdate = true; m.opacity = Math.max(0, 1 - t / 1.6);
    ring.scale.setScalar(0.2 + t * 5); ring.material.opacity = Math.max(0, 1 - t / 0.7); ring.quaternion.copy(camera.quaternion);
    flash.scale.setScalar(0.1 + Math.sin(Math.min(t / 0.35, 1) * Math.PI) * 5 * power); flash.material.opacity = Math.max(0, 1 - t / 0.35);
    if (t > 1.6) { fx.remove(pts, ring, flash); g.dispose(); return false; } return true;
  });
}
// Laser abfeuern: Strahl wächst von a nach b, Kopf leuchtet; danach Meldung an die App
function fire({ id = 'shot', from, to, color = 'cyan', ms = 900, hit = true, keep = false, burstColor }) {
  const a = V(from), b = V(to), rod = glowRod(color, 0.05, 4), head = glowSprite(color, 1.4, 1); fx.add(rod, head);
  let t = 0; const T = ms / 1000;
  effects.push((dt) => {
    t += dt; const k = Math.min(1, t / T), e = 1 - Math.pow(1 - k, 2.2), p = a.clone().lerp(b, e);
    rod.set(a, p); head.position.copy(p); head.scale.setScalar(1.2 + Math.sin(t * 40) * 0.25);
    if (k < 1) return true;
    if (hit) { burst(fromV(b), burstColor ?? color, 110, 1.15); shake = 0.35; }
    else burst(fromV(b), 'orange', 22, 0.45);   // Fehlschuss verpufft
    emit('fired', { id, hit });
    let f = 0; fx.remove(head);
    effects.push((d2) => { f += d2; if (keep && hit) return f < 30; rod.fade(Math.max(0, 1 - f / 0.6)); if (f > 0.6) { fx.remove(rod); return false; } return true; });
    return false;
  });
}
// Lichtshow als Belohnung: Scheinwerfer drehen auf, Funken regnen
let show = 0;
function party(sec = 4) {
  show = sec;
  const { min, max } = bounds;
  for (let i = 0; i < 6; i++) setTimeout(() => burst([min[0] + Math.random() * (max[0] - min[0]), min[1] + Math.random() * (max[1] - min[1]), 1.5 + Math.random() * 4], Object.keys(COLORS)[i % 6], 70, 0.9), i * 260);
}

// ───────── Kamera: mit dem Finger drehen, mit zwei Fingern zoomen ─────────
const cam = { target: new THREE.Vector3(4, 2, 4), az: 0.62, el: 0.42, dist: 19, tAz: 0.62, tEl: 0.42, tDist: 19, tTarget: new THREE.Vector3(4, 2, 4) };
let shake = 0, lastTouch = 0;
// Mitfahrt auf einer Bahn: { id, u (Ziel 0..1), cu (aktuell), view: 'fp' (Fahrersicht) | 'out' (von außen) }
let ride = null;
function setFov(f) { if (camera.fov !== f) { camera.fov = f; camera.updateProjectionMatrix(); } }
function frameBounds(instant) {
  const { min, max } = bounds, c = [(min[0] + max[0]) / 2, (min[1] + max[1]) / 2, max[2] * 0.36];
  cam.tTarget.copy(V(c));
  const size = Math.max(max[0] - min[0], max[1] - min[1], max[2] * 1.2), aspect = innerWidth / innerHeight;
  cam.tDist = size * (aspect < 1 ? 1.62 / Math.max(aspect, 0.55) : 1.75) + 4.5;
  if (instant) { cam.target.copy(cam.tTarget); cam.dist = cam.tDist; }
}
function setCamera({ az, el, dist, target }) { if (az != null) cam.tAz = az; if (el != null) cam.tEl = el; if (dist != null) cam.tDist = dist; if (target) cam.tTarget.copy(V(target)); }

const ray = new THREE.Raycaster(), pointers = new Map();
let drag = null, pinch = 0;
const ndc = (e) => new THREE.Vector2((e.clientX / innerWidth) * 2 - 1, -(e.clientY / innerHeight) * 2 + 1);
const toScreen = (v) => { const p = v.clone().project(camera); return new THREE.Vector2((p.x + 1) / 2 * innerWidth, (1 - p.y) / 2 * innerHeight); };
function pickHandle(e) {
  const m = new THREE.Vector2(e.clientX, e.clientY); let best = null;
  for (const o of objects.values()) {
    if (!o.isHandle) continue;
    const dirs = [[1, 0, 0], [0, 1, 0], [0, 0, 1]];
    o.arrows.forEach((a, i) => {
      if (!a.visible) return;
      [-1, 1].forEach((k) => { const d = toScreen(o.pos.clone().addScaledVector(V(dirs[i]), k * 0.62)).distanceTo(m); if (d < 34 && (!best || d < best.d)) best = { o, axis: i, d }; });
    });
    const dc = toScreen(o.pos).distanceTo(m);
    if (dc < 30 && (!best || dc < best.d)) best = { o, axis: -1, d: dc };
  }
  return best;
}
// Nächster Punkt der Achse zum Sehstrahl → Wert entlang der Achse
function axisValue(origin, dir, e) {
  ray.setFromCamera(ndc(e), camera);
  const r0 = ray.ray.origin, rd = ray.ray.direction, w = origin.clone().sub(r0), b = dir.dot(rd), d = dir.dot(w), e2 = rd.dot(w), den = 1 - b * b;
  return Math.abs(den) < 1e-5 ? 0 : (b * e2 - d) / den;
}
canvas.addEventListener('pointerdown', (e) => {
  canvas.setPointerCapture(e.pointerId); pointers.set(e.pointerId, e); lastTouch = clock.elapsedTime;
  if (pointers.size === 1) {
    const h = pickHandle(e);
    if (h) {
      const start = fromV(h.o.pos).map(Math.round);
      // Tippen auf die Kugel: in der Bodenebene ziehen (x1, x2); auf einen Pfeil: entlang dieser Achse
      drag = { id: h.o.id, axis: h.axis, start, last: start.slice(), origin: h.o.pos.clone(), v0: h.axis >= 0 ? axisValue(h.o.pos, V([[1, 0, 0], [0, 1, 0], [0, 0, 1]][h.axis]), e) : 0 };
      emit('grab', { id: drag.id });
    } else drag = { orbit: true, x: e.clientX, y: e.clientY, x0: e.clientX, y0: e.clientY, t0: performance.now() };
  } else { drag = null; const [a, b] = [...pointers.values()]; pinch = Math.hypot(a.clientX - b.clientX, a.clientY - b.clientY); }
});
canvas.addEventListener('pointermove', (e) => {
  if (!pointers.has(e.pointerId)) return; pointers.set(e.pointerId, e); lastTouch = clock.elapsedTime;
  if (pointers.size === 2) { const [a, b] = [...pointers.values()], d = Math.hypot(a.clientX - b.clientX, a.clientY - b.clientY); if (pinch > 0) cam.tDist = THREE.MathUtils.clamp(cam.tDist * pinch / d, 7, 46); pinch = d; return; }
  if (!drag) return;
  if (drag.orbit) { cam.tAz -= (e.clientX - drag.x) * 0.008; cam.tEl = THREE.MathUtils.clamp(cam.tEl + (e.clientY - drag.y) * 0.006, 0.06, 1.45); drag.x = e.clientX; drag.y = e.clientY; return; }
  const p = drag.start.slice();
  if (drag.axis >= 0) p[drag.axis] = Math.round(drag.start[drag.axis] + axisValue(drag.origin, V([[1, 0, 0], [0, 1, 0], [0, 0, 1]][drag.axis]), e) - drag.v0);
  else { ray.setFromCamera(ndc(e), camera); const hit = new THREE.Vector3(); if (ray.ray.intersectPlane(new THREE.Plane(UP, -drag.origin.y), hit)) { const m = fromV(hit); p[0] = Math.round(m[0]); p[1] = Math.round(m[1]); } }
  const { min, max } = bounds; for (let i = 0; i < 3; i++) p[i] = THREE.MathUtils.clamp(p[i], Math.min(min[i], -max[i] * (i < 2 ? 0 : 0)), max[i]);
  if (p.some((v, i) => v !== drag.last[i])) { drag.last = p; emit('drag', { id: drag.id, p }); }
});
// Kurzes Antippen einer Kugel (ohne Wischen) meldet 'tap' – großzügig gemessen, Finger sind dicker als Mauszeiger
function tapped(e) {
  ray.setFromCamera(ndc(e), camera); let best = null;
  for (const o of objects.values()) { if (!o.ball) continue; const b = o.ball(), d = ray.ray.distanceToPoint(b.p); if (d < b.r * 1.5 + 0.15 && (!best || d < best.d)) best = { id: o.id, d }; }
  return best;
}
const up = (e) => {
  pointers.delete(e.pointerId);
  if (drag && !drag.orbit) emit('drop', { id: drag.id, p: drag.last });
  else if (drag && e.type === 'pointerup' && Math.hypot(e.clientX - drag.x0, e.clientY - drag.y0) < 14 && performance.now() - drag.t0 < 450) {
    const h = tapped(e);
    if (h) emit('tap', { id: h.id });
    else { // daneben getippt: die Stelle in der Ebene x₁ = 4 melden (dort schweben die Kugeln)
      const hit = new THREE.Vector3(); ray.setFromCamera(ndc(e), camera);
      if (ray.ray.intersectPlane(new THREE.Plane(new THREE.Vector3(0, 0, 1), -4), hit)) emit('miss', { p: fromV(hit).map((v) => Math.round(v * 100) / 100) });
    }
  }
  drag = null; pinch = 0;
};
canvas.addEventListener('pointerup', up); canvas.addEventListener('pointercancel', up);

// ───────── Zeichnen: Himmel → gespiegelte Welt → Boden und Welt ─────────
function drawStage(r, target) {
  r.setRenderTarget(target); r.setClearColor(0x000000, 1); r.clear();
  const vis = (s, d, f, w) => { sky.visible = s; decor.visible = d; floor.visible = f; world.visible = w; fx.visible = w; };
  vis(true, false, false, false); r.render(scene, camera); r.clearDepth();
  if (quality > 0) { vis(false, false, false, true); scene.scale.y = -1; scene.updateMatrixWorld(true); labelsVisible(false); r.render(scene, camera); labelsVisible(true); scene.scale.y = 1; scene.updateMatrixWorld(true); r.clearDepth(); }
  vis(false, true, true, true); r.shadowMap.needsUpdate = true; r.render(scene, camera);
  vis(true, true, true, true);
}
class StagePass extends Pass {
  constructor() { super(); this.needsSwap = false; }
  render(r, writeBuffer, readBuffer) { drawStage(r, this.renderToScreen ? null : readBuffer); }
}
function labelsVisible(v) { scene.traverse((o) => { if (o.userData.isLabel) o.visible = v; }); for (const o of objects.values()) if (o.isHandle) o.g.visible = v; }
let quality = 1;
const composer = new EffectComposer(renderer);
composer.addPass(new StagePass());
const bloom = new UnrealBloomPass(new THREE.Vector2(256, 256), 0.62, 0.7, 0.92);
composer.addPass(bloom); composer.addPass(new OutputPass());
// Leuchten (Bloom) braucht Farbpuffer mit hoher Genauigkeit. Kann der Grafikchip das nicht, wird direkt gezeichnet – ohne Schein, aber sichtbar.
const gl = renderer.getContext();
let direct = /[?&]direkt/.test(location.search) || !(renderer.capabilities.isWebGL2 && (gl.getExtension('EXT_color_buffer_float') || gl.getExtension('EXT_color_buffer_half_float')));
let checked = false;
function draw(dt) {
  if (direct) { drawStage(renderer, null); return; }
  composer.render(dt);
  if (!checked && gl.drawingBufferHeight > 16 && clock.elapsedTime > 0.3) {
    checked = true;
    // Einmal prüfen, ob in den Puffer wirklich gezeichnet werden konnte und etwas Sichtbares herauskommt
    renderer.setRenderTarget(composer.renderTarget1); const incomplete = gl.checkFramebufferStatus(gl.FRAMEBUFFER) !== gl.FRAMEBUFFER_COMPLETE; renderer.setRenderTarget(null);
    const px = new Uint8Array(4 * 64); gl.readPixels(0, Math.floor(gl.drawingBufferHeight * 0.9), 64, 1, gl.RGBA, gl.UNSIGNED_BYTE, px);
    const dark = px.every((v, i) => i % 4 === 3 || v === 0);
    if (incomplete || dark) { direct = true; emit('mode', { direct: true, incomplete, dark }); drawStage(renderer, null); }
  }
}

function resize() {
  const w = innerWidth, h = innerHeight; if (!w || !h) return;
  renderer.setPixelRatio(pixelRatio); renderer.setSize(w, h, false); composer.setPixelRatio(pixelRatio); composer.setSize(w, h);
  camera.aspect = w / h; camera.updateProjectionMatrix(); frameBounds(false);
}
addEventListener('resize', resize);

// Selbstdiagnose: Was sieht die Bühne wirklich? Meldet Gerät, Größe und ob das Bild einfarbig ist.
let diagnosed = false, sizeWarned = false;
function diagnose() {
  const W = gl.drawingBufferWidth, H = gl.drawingBufferHeight, px = new Uint8Array(4), seen = new Set();
  for (let i = 1; i < 8; i++) for (let j = 1; j < 8; j++) { gl.readPixels(Math.floor(W * i / 8), Math.floor(H * j / 8), 1, 1, gl.RGBA, gl.UNSIGNED_BYTE, px); seen.add(px[0] >> 2 << 16 | px[1] >> 2 << 8 | px[2] >> 2); }
  const dbg = gl.getExtension('WEBGL_debug_renderer_info');
  const info = { size: innerWidth + 'x' + innerHeight, buffer: W + 'x' + H, dpr: window.devicePixelRatio, gpu: dbg ? gl.getParameter(dbg.UNMASKED_RENDERER_WEBGL) : '?',
    webgl2: renderer.capabilities.isWebGL2, direct, lost: gl.isContextLost(), colors: seen.size, dist: Math.round(cam.dist * 10) / 10, objects: objects.size, calls: renderer.info.render.calls };
  emit('info', info);
  if (seen.size < 3) {
    // Einfarbiges Bild: erst ohne Nachbearbeitung versuchen, dann melden
    if (!direct) { direct = true; diagnosed = false; clock.elapsedTime = 1.5; } else emit('error', { msg: 'Bild einfarbig – ' + JSON.stringify(info) });
  }
}
canvas.addEventListener('webglcontextlost', (e) => { e.preventDefault(); emit('error', { msg: 'Grafik-Kontext verloren' }); });
const clock = new THREE.Clock(); let frames = 0, slow = 0, paused = false, fixedTime = null;
function frame() {
  if (paused) return;
  requestAnimationFrame(frame);
  step(Math.min(clock.getDelta(), 0.05));
}
function step(dt) {
  const t = fixedTime ?? clock.elapsedTime;
  // Kamera: weich nachführen, in Ruhe ganz leicht schwenken
  if (!isFinite(cam.dist)) cam.dist = cam.tDist; if (!isFinite(cam.az)) cam.az = cam.tAz;
  const idle = t - lastTouch > 4 ? Math.sin(t * 0.25) * 0.05 : 0, k = 1 - Math.exp(-7 * dt);
  cam.az += (cam.tAz + idle - cam.az) * k; cam.el += (cam.tEl - cam.el) * k; cam.dist += (cam.tDist - cam.dist) * k; cam.target.lerp(cam.tTarget, k);
  shake = Math.max(0, shake - dt * 1.4);
  const sh = new THREE.Vector3((Math.random() - 0.5), (Math.random() - 0.5), (Math.random() - 0.5)).multiplyScalar(shake * 0.35);
  camera.position.set(cam.target.x + Math.sin(cam.az) * Math.cos(cam.el) * cam.dist, cam.target.y + Math.sin(cam.el) * cam.dist, cam.target.z + Math.cos(cam.az) * Math.cos(cam.el) * cam.dist).add(sh);
  camera.lookAt(cam.target);
  const track = ride && objects.get(ride.id);
  // In der Fahrersicht stünde die y-Achse als Strich direkt vor der Nase und die x-Achse läge im Gleis – dort ausblenden
  const ax2 = objects.get('achsen'); if (ax2) ax2.g.visible = !(ride && ride.view === 'fp');
  if (track && track.at) {
    ride.cu += (ride.u - ride.cu) * (1 - Math.exp(-12 * dt));
    const here = track.at(ride.cu);
    if (ride.view === 'fp') {
      // Fahrersicht: knapp über den Schienen, Blick entlang der Bahn
      const a = track.at(ride.cu - 0.02), b = track.at(ride.cu + 0.04), dir = b.sub(a); if (dir.lengthSq() < 1e-8) dir.set(1, 0, 0); dir.normalize();
      // „Oben“ ist die Senkrechte zur Flugrichtung, die möglichst nach oben zeigt – so klappt es auf der Achterbahn und im freien Raum
      const up = UP.clone().addScaledVector(dir, -UP.dot(dir)); if (up.lengthSq() < 1e-6) up.set(0, 0, -1); up.normalize();
      // Sitzhöhe über der Schiene; der Blick geht ein Stück die Strecke entlang, damit man sieht, was kommt
      camera.position.copy(here).addScaledVector(up, 0.75).addScaledVector(dir, -0.35).add(sh);
      const ahead = track.at(ride.cu + 0.09);
      camera.up.copy(up); camera.lookAt(ahead.lengthSq() > 0 && ride.cu < 0.9 ? ahead.addScaledVector(up, 0.25) : here.clone().addScaledVector(dir, 2.5)); camera.up.set(0, 1, 0);
      setFov(68); track.showCart(null); bloom.strength = 0.3;
    } else { setFov(42); track.showCart(here); bloom.strength = 0.62; }
  } else { setFov(42); bloom.strength = 0.62; }
  key.target.position.copy(cam.tTarget); key.position.copy(cam.tTarget).add(new THREE.Vector3(5, 16, 7));

  sky.userData.mat.uniforms.t.value = t; sky.userData.stars.uniforms.t.value = t; dust.uniforms.t.value = t;
  show = Math.max(0, show - dt);
  const s = Math.min(1, show);
  heads.forEach((h, i) => {
    const sp = 0.25 + s * 2.2, r = 3 + s * 3;
    const aim = cam.tTarget.clone().setY(0).add(new THREE.Vector3(Math.cos(t * sp + h.ph) * r + (h.base.x - 4) * 0.5, 0, Math.sin(t * sp * 1.3 + h.ph) * r + (h.base.z - 4) * 0.5));
    const d = aim.sub(h.g.position).normalize(); h.cone.quaternion.setFromUnitVectors(new THREE.Vector3(0, -1, 0), d);
    h.m.uniforms.k.value = 0.012 + s * 0.2 * (0.6 + 0.4 * Math.sin(t * 9 + i));
    if (s > 0) h.m.uniforms.c.value.setHSL((t * 0.25 + i / 6) % 1, 0.9, 0.6);
  });
  for (const o of objects.values()) {
    o.tick(dt, t);
    const age = t - o.born; if (age < 0.6) { const e = Math.min(1, age / 0.45), b = 1 + 2.2 * Math.pow(e - 1, 3) + 1.2 * Math.pow(e - 1, 2); o.g.scale.setScalar(Math.max(0.001, b)); } else o.g.scale.setScalar(1);
  }
  for (let i = effects.length - 1; i >= 0; i--) if (!effects[i](dt)) effects.splice(i, 1);
  draw(dt);
  if (!diagnosed && fixedTime == null && clock.elapsedTime > 2.5 && gl.drawingBufferHeight > 16) { diagnosed = true; diagnose(); }
  if (!sizeWarned && clock.elapsedTime > 3 && (!innerWidth || !innerHeight)) { sizeWarned = true; emit('error', { msg: 'Bühne hat keine Größe: ' + innerWidth + 'x' + innerHeight }); }

  // Bei schwachen Geräten automatisch sparen
  if (fixedTime == null) { frames++; if (dt > 0.03) slow++; if (frames === 90) { if (slow > 45 && pixelRatio > 1) { pixelRatio = Math.max(1, pixelRatio * 0.75); resize(); } else if (slow > 45 && quality > 0) quality = 0; frames = slow = 0; } }
}

buildGrid(); resize(); frameBounds(true); frame();
window.RL = {
  setState: (s) => setState(typeof s === 'string' ? JSON.parse(s) : s),
  fire: (s) => fire(typeof s === 'string' ? JSON.parse(s) : s),
  burst: (p, c) => burst(p, c), party, setCamera,
  // Mitfahren: ride({id, u, view}) setzt die Stelle auf der Bahn, ride(null) beendet die Fahrt
  ride: (r) => { if (!r) { const t = ride && objects.get(ride.id); if (t && t.showCart) t.showCart(null); ride = null; return; }
    ride = { id: r.id ?? 'bahn', u: r.u ?? 0, cu: ride && ride.id === (r.id ?? 'bahn') ? ride.cu : (r.u ?? 0), view: r.view ?? 'fp' }; },
  // Zubehör für Lux: Leuchtspur ('none' | 'sparks' | 'rainbow') und Aufsatz ('none' | 'antenna' | 'halo' | 'crown')
  look: (l) => { if (l.trail) luxTrail = l.trail; if (l.hat) luxHat = l.hat; },
  skin: (c) => { luxSkin = c; for (const o of objects.values()) if (o.skin) o.skin(c); },
  pause: () => { paused = true; }, resume: () => { if (paused) { paused = false; clock.getDelta(); frame(); } },
  // Für Bildschirmfotos: Zeit von Hand vorspulen
  snapshot: () => { step(0); return canvas.toDataURL('image/png'); },
  advance: (sec, stepSize = 1 / 30) => { paused = true; lastTouch = -99; for (let t = 0; t < sec; t += stepSize) { clock.elapsedTime += stepSize; step(stepSize); } },
};
emit('ready', {});
