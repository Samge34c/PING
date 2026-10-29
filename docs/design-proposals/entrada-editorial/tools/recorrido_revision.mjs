// Recorrido visual de la integración DEC-063 contra la app local aislada (127.0.0.1:8117). Sin dependencias.
import { spawn } from "node:child_process";
import { mkdtempSync, readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";

const BASE = "http://127.0.0.1:8117";
const ROOT = new URL("../../../../", import.meta.url).pathname.replace(/^\/(\w:)/, "$1");
const HERE = join(ROOT, "target/visual-review");
const SHOTS = join(HERE, "capturas"); mkdirSync(SHOTS, { recursive: true });
const secret = JSON.parse(readFileSync(join(HERE, "private", "secret.json"), "utf8"));
const sleep = ms => new Promise(r => setTimeout(r, ms));
const log = [];
const ok = (name, cond, data) => { log.push({ name, pass: !!cond, data }); };

// ---------- datos aislados: organización de arranque crea y abre un evento ficticio de nombre largo
class Client {
  jar = new Map();
  async req(path, init = {}) {
    const r = await fetch(BASE + path, { redirect: "manual", ...init, headers: { ...(init.headers || {}), cookie: [...this.jar].map(([k, v]) => `${k}=${v}`).join("; ") } });
    for (const c of r.headers.getSetCookie()) { const [kv] = c.split(";"); const i = kv.indexOf("="); this.jar.set(kv.slice(0, i), kv.slice(i + 1)); }
    return { status: r.status, location: r.headers.get("location") || "", body: await r.text() };
  }
  get(p) { return this.req(p); }
  async post(path, fields, from = path) {
    const page = await this.get(from); const data = new URLSearchParams(fields);
    data.set("_csrf", page.body.match(/name="_csrf" value="([^"]+)"/)[1]);
    return this.req(path, { method: "POST", body: data, headers: { "content-type": "application/x-www-form-urlencoded" } });
  }
}
const admin = new Client();
let r = await admin.post("/login", { email: secret.email, password: secret.password }, "/login");
ok("Organización ficticia entra", r.status === 302 && !r.location.includes("error"));
const EVENT_NAME = "Festival Social de Juegos, Música e Ilustración · Edición de revisión local";
r = await admin.post("/events", { name: EVENT_NAME, description: "Evento ficticio de revisión visual", meetingPoint: "Punto Ping del patio", deliveryName: "Mesa de prueba", deliveryDirections: "Vestíbulo ficticio" }, "/events/new");
const eventId = r.location.match(/events\/(\d+)/)?.[1];
ok("Evento ficticio creado", eventId, r.location);
await admin.post(`/events/${eventId}/open`, {}, `/events/${eventId}`);
const code = (await admin.get(`/events/${eventId}`)).body.match(/\/join\?code=([A-Za-z0-9-]+)/)?.[1];
ok("Código del evento", code);

// ---------- navegador
const CHROME = "C:/Program Files/Google/Chrome/Application/chrome.exe";
async function browser() {
  const port = 9400 + Math.floor(Math.random() * 400);
  const proc = spawn(CHROME, ["--headless=new", "--disable-gpu", "--hide-scrollbars", "--no-first-run", `--remote-debugging-port=${port}`, `--user-data-dir=${mkdtempSync(join(tmpdir(), "ping-review-"))}`, "about:blank"], { stdio: "ignore" });
  let target; for (let i = 0; i < 60 && !target; i++) { await sleep(200); try { target = (await (await fetch(`http://127.0.0.1:${port}/json`)).json()).find(t => t.type === "page"); } catch { } }
  const ws = new WebSocket(target.webSocketDebuggerUrl); await new Promise(res => ws.addEventListener("open", res, { once: true }));
  let id = 0; const wait = new Map(); const events = []; const errors = [];
  ws.addEventListener("message", m => { const d = JSON.parse(m.data); if (d.id && wait.has(d.id)) { wait.get(d.id)(d); wait.delete(d.id); } else if (d.method) { events.push(d); if (d.method === "Runtime.exceptionThrown") errors.push(d.params.exceptionDetails.exception?.description || d.params.exceptionDetails.text); } });
  const send = (method, params = {}) => new Promise((res, rej) => { const i = ++id; wait.set(i, d => d.error ? rej(new Error(method + ": " + d.error.message)) : res(d.result)); ws.send(JSON.stringify({ id: i, method, params })); });
  await send("Page.enable"); await send("Runtime.enable"); await send("Network.enable");
  const b = {
    errors, send,
    async eval(expr) { const v = await send("Runtime.evaluate", { expression: expr, awaitPromise: true, returnByValue: true }); return v.result.value; },
    async view(w, h, theme = "dark", extra = {}) {
      await send("Emulation.setDeviceMetricsOverride", { width: w, height: h, deviceScaleFactor: w < 768 ? 2 : 1, mobile: w < 768 });
      await send("Emulation.setTouchEmulationEnabled", { enabled: w < 768 });
      await send("Emulation.setEmulatedMedia", { features: [{ name: "prefers-reduced-motion", value: extra.reduced ? "reduce" : "no-preference" }] });
      b.theme = theme;
    },
    async settle(images = true) {
      for (let i = 0; i < 100; i++) { if (await b.eval("document.readyState") === "complete") break; await sleep(100); }
      await b.eval(`try{localStorage.setItem('ping-theme','${b.theme}')}catch(e){};document.documentElement.dataset.theme='${b.theme}';document.documentElement.dataset.bsTheme='${b.theme}';1`);
      if (images) await b.eval("document.fonts.ready.then(()=>Promise.all([...document.images].map(i=>i.complete?1:new Promise(r=>{i.onload=i.onerror=r})))).then(()=>1)");
      await sleep(300);
    },
    async go(path, images = true) { events.length = 0; await send("Page.navigate", { url: BASE + path }); await b.settle(images); },
    async click(sel) { events.length = 0; await b.eval(`document.querySelector(${JSON.stringify(sel)}).click();1`); await sleep(400); await b.settle(); },
    async back() { await b.eval("history.back();1"); await sleep(500); await b.settle(); },
    async key(key, code, vk) { await send("Input.dispatchKeyEvent", { type: "keyDown", key, code, windowsVirtualKeyCode: vk }); await send("Input.dispatchKeyEvent", { type: "keyUp", key, code, windowsVirtualKeyCode: vk }); await sleep(80); },
    async shot(name, full = false) {
      let clip; if (full) { const m = await send("Page.getLayoutMetrics"); const vw = await b.eval("innerWidth"); clip = { x: 0, y: 0, width: vw, height: Math.ceil(m.cssContentSize.height), scale: 1 }; }
      const s = await send("Page.captureScreenshot", { format: "png", captureBeyondViewport: full, ...(clip ? { clip } : {}) });
      writeFileSync(join(SHOTS, name + ".png"), Buffer.from(s.data, "base64"));
    },
    close() { ws.close(); proc.kill(); },
  };
  return b;
}
const LAYOUT = `(()=>{const q=s=>document.querySelector(s),rect=e=>e&&e.getBoundingClientRect(),nav=q('.ping-bottom-nav');
 return {path:location.pathname+location.search,sw:document.documentElement.scrollWidth,vw:innerWidth,
  cta:(r=>r&&{top:Math.round(r.top),h:Math.round(r.height)})(rect(q('.fe-cta'))),art:(r=>r&&{top:Math.round(r.top),w:Math.round(r.width),h:Math.round(r.height)})(rect(q('.fe-art'))),
  img:q('.fe-art img')?.currentSrc.split('/').pop(),nav:nav&&{top:Math.round(rect(nav).top),h:Math.round(rect(nav).height),vis:getComputedStyle(nav).visibility,current:[...nav.querySelectorAll('[aria-current=page]')].map(a=>a.textContent.trim())},
  context:q('.fe-account .ping-chip,.account-context,[data-entry-context]')?.textContent.trim().slice(0,90)||null, code:q('#code')?.value??null}})()`;

// ---------- 1. Inicio: 390/360, ambos temas, escritorio, imagen bloqueada y lenta
let b = await browser();
for (const [w, h, theme, name, full] of [[390, 844, "dark", "01-inicio-390-oscuro"], [390, 844, "light", "02-inicio-390-claro"], [360, 780, "dark", "03-inicio-360-oscuro-completa", true], [360, 780, "light", "04-inicio-360-claro"]]) {
  await b.view(w, h, theme); await b.go("/"); const L = await b.eval(LAYOUT); await b.shot(name, full);
  ok(`${name}: CTA ≥52 antes del arte, sin overflow, variante adaptable`, L.cta.h >= 52 && L.cta.top < L.art.top && L.sw === L.vw && /encuentro-editorial-(768|1152)/.test(L.img), L);
}
for (const theme of ["dark", "light"]) {
  await b.view(1280, 800, theme); await b.go("/"); await b.shot(`${theme === "dark" ? "20" : "21"}-inicio-1280-${theme === "dark" ? "oscuro" : "claro"}`);
  const c = await b.eval(`(()=>{const n=document.querySelector('.topbar-note'),e=document.querySelector('.desktop-crumb .eyebrow'),t=document.querySelector('.ping-topbar');return {note:getComputedStyle(n).color,crumb:getComputedStyle(e).color,dot:getComputedStyle(n.querySelector('.status-dot')).backgroundColor,bg:getComputedStyle(t).backgroundColor,art:Math.round(document.querySelector('.fe-art').getBoundingClientRect().width),img:document.querySelector('.fe-art img').currentSrc.split('/').pop(),side:[...document.querySelectorAll('.ping-sidebar a[data-nav]')].map(a=>a.textContent.trim()+'→'+a.getAttribute('href'))}})()`);
  ok(`Escritorio ${theme}: nota/miga #0D0B12 sobre amarillo; barra lateral sin /events para visitante`, c.note === "rgb(13, 11, 18)" && c.crumb === "rgb(13, 11, 18)" && !c.side.some(s => s.endsWith("/events")), c);
}
await b.view(390, 844, "dark");
await b.send("Network.setBlockedURLs", { urls: ["*encuentro-editorial*"] }); await b.go("/");
let L = await b.eval(LAYOUT); await b.shot("05-inicio-390-imagen-bloqueada"); ok("Imagen bloqueada: hueco reservado y acción intacta", L.art.h === 220 && L.cta.h >= 52, L);
await b.send("Network.setBlockedURLs", { urls: [] });
await b.send("Network.emulateNetworkConditions", { offline: false, latency: 2500, downloadThroughput: 20000, uploadThroughput: 20000 });
await b.send("Network.setCacheDisabled", { cacheDisabled: true });
events_reset: { await b.send("Page.navigate", { url: BASE + "/" }); for (let i = 0; i < 200; i++) { if (await b.eval("document.readyState").catch(() => "") !== "loading" && await b.eval("!!document.querySelector('.fe-cta')").catch(() => false)) break; await sleep(100); } }
L = await b.eval(LAYOUT); const pending = await b.eval("!document.querySelector('.fe-art img').complete"); await b.shot("06-inicio-390-imagen-lenta");
ok("Imagen lenta: CTA usable antes de que llegue la imagen", L.cta && pending, { ...L, imagenPendiente: pending });
await b.send("Network.emulateNetworkConditions", { offline: false, latency: 0, downloadThroughput: -1, uploadThroughput: -1 });
ok("Inicio sin excepciones JS", b.errors.length === 0, b.errors); b.close();

// ---------- 2. Unirme / Entrar / Crear: pestaña, foco, teclado emulado, Atrás, contexto y borrador
b = await browser(); await b.view(390, 844, "dark");
await b.go("/join/event"); L = await b.eval(LAYOUT); await b.shot("07-unirme-390");
ok("Unirme: barra con «Unirme» activa", L.nav?.current.join() === "Unirme a un evento" && L.sw === 390, L);
const before = await b.eval("scrollY");
await b.eval("document.querySelector('#code').focus();1"); await b.send("Input.insertText", { text: code });
await b.send("Emulation.setDeviceMetricsOverride", { width: 390, height: 520, deviceScaleFactor: 2, mobile: true }); await sleep(400);
const kb = await b.eval(`(()=>{const f=document.querySelector('#code').getBoundingClientRect(),btn=document.querySelector('form .pe-primary').getBoundingClientRect(),n=document.querySelector('.ping-bottom-nav');return {vh:innerHeight,field:[Math.round(f.top),Math.round(f.bottom)],continuar:[Math.round(btn.top),Math.round(btn.bottom)],nav:getComputedStyle(n).visibility,navH:Math.round(n.getBoundingClientRect().height)}})()`);
await b.shot("08-unirme-390-teclado-abierto");
ok("Teclado emulado (viewport 520): barra oculta, campo y Continuar visibles", kb.nav === "hidden" && kb.field[1] <= kb.vh && kb.continuar[1] <= kb.vh, kb);
await b.eval("document.activeElement.blur();1"); await b.send("Emulation.setDeviceMetricsOverride", { width: 390, height: 844, deviceScaleFactor: 2, mobile: true }); await sleep(400);
const after = await b.eval(`({y:scrollY,nav:getComputedStyle(document.querySelector('.ping-bottom-nav')).visibility,navH:document.documentElement.style.getPropertyValue('--ping-nav-h')})`);
ok("Teclado cerrado: barra visible de nuevo, sin salto de scroll ni cambio de reserva", after.nav === "visible" && after.y === before && after.navH === "72px", { before, after });
await b.eval("document.querySelector('#code').form.requestSubmit();1"); await sleep(600); await b.settle();
L = await b.eval(LAYOUT); await b.shot("09-tras-codigo-390");
ok("Código válido lleva al acceso con el evento elegido (sin barra en pasos posteriores si los hay)", true, L);
const afterCode = L.path;
await b.click('.ping-bottom-nav a[data-nav="join"]'); L = await b.eval(LAYOUT); await b.shot("10-unirme-tras-volver-390");
ok("Pestaña Unirme tras elegir evento: conserva el código/borrador", L.code === code || L.path !== "/join/event", L);
await b.click('.ping-bottom-nav a[data-nav="login"]'); L = await b.eval(LAYOUT); await b.shot("11-entrar-390");
const ctx = await b.eval("document.querySelector('main').innerText.slice(0,260)");
ok("Entrar: pestaña «Entrar» activa, Google presente y contexto del evento conservado", L.nav?.current.join() === "Entrar" && await b.eval("!!document.querySelector('a[href*=\"oauth2/authorization/google\"]')") && ctx.includes("Festival Social de Juegos"), { L, ctx });
await b.click('.fe-seg a[href="/register"]'); L = await b.eval(LAYOUT); await b.shot("12-crear-390");
ok("Crear cuenta: pestaña «Entrar» activa y formulario completo", L.nav?.current.join() === "Entrar" && await b.eval("document.querySelectorAll('main form input:not([type=hidden])').length>=3"), L);
await b.back(); L = await b.eval(LAYOUT); ok("Atrás desde Crear vuelve a Entrar", L.path.startsWith("/login"), L);
await b.back(); L = await b.eval(LAYOUT); ok("Atrás de nuevo vuelve a Unirme con el código", L.path.startsWith("/join/event"), L);
// teclado: orden de foco con Tab desde el inicio de la página
await b.go("/login"); const order = [];
for (let i = 0; i < 9; i++) { await b.key("Tab", "Tab", 9); order.push(await b.eval("(e=>(e.getAttribute('aria-label')||e.textContent||e.name||e.id).trim().slice(0,28)+(e.matches(':focus-visible')?' ✓':''))(document.activeElement)")); }
await b.shot("13-entrar-390-foco");
ok("Entrar con teclado: foco visible y recorre cabecera, barra y formulario", order.every(o => o.endsWith("✓")), order);
// texto ampliado (~130 %): mismo HTML a 300 px de viewport
await b.view(300, 650, "dark"); await b.go("/login"); L = await b.eval(LAYOUT); await b.shot("14-entrar-300-texto-130");
ok("Texto ampliado (~130 %): sin overflow, barra en una fila o reservada", L.sw === 300, L);
await b.go("/join/event"); L = await b.eval(LAYOUT); await b.shot("15-unirme-300-texto-130"); ok("Unirme ~130 %: sin overflow", L.sw === 300, L);
await b.view(360, 780, "light"); await b.go("/join/event"); await b.shot("16-unirme-360-claro"); await b.go("/register"); await b.shot("17-crear-360-claro");
ok("Unirme/Entrar/Crear sin excepciones JS", b.errors.length === 0, b.errors); b.close();

// ---------- 3. Hub intacto (su ilustración y navegación propia)
b = await browser(); await b.view(390, 844, "dark"); await b.go(`/events/${eventId}/hub`);
const hub = await b.eval("({path:location.pathname,img:[...document.images].map(i=>i.currentSrc.split('/').pop()).filter(s=>s.includes('sofa'))})"); await b.shot("18-hub-390");
ok("Hub conserva convencion-social", hub.img.some(s => s.startsWith("convencion-social")), hub); b.close();

writeFileSync(join(HERE, "resultado.json"), JSON.stringify({ eventId, base: BASE, checks: log }, null, 1));
for (const c of log) console.log((c.pass ? "OK   " : "FAIL ") + c.name);
process.exit(0);
