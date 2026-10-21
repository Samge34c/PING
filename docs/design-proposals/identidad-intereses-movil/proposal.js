/* PING · propuesta aislada (identidad + intereses). PROTOTIPO: no envía nada, no llama a PING.
   JavaScript nativo, sin dependencias. Catálogo real de la BD de prueba en catalog-data.js.
   Los puntos «CONTRATO» marcan lo que la app real ya hace y Codex debe conservar (ver HANDOFF_CODEX.md). */
(() => {
  'use strict';
  const C = window.PROPUESTA_CATALOGO;
  const ASSETS = '../../../src/main/resources/static/img/avatars/'; // recursos locales reales de la app

  // Opciones permitidas: copia de static/img/avatars/manifest.json (lista cerrada; sin URLs ni subidas).
  const AV = {
    animal: { folder: 'animales', label: 'Animal', list: [['gato', 'Gato'], ['perro', 'Perro'], ['zorro', 'Zorro'], ['oso', 'Oso'], ['conejo', 'Conejo'], ['panda', 'Panda'], ['rana', 'Rana'], ['buho', 'Búho']] },
    hat: { folder: 'sombreros', label: 'Sombrero', list: [['none', 'Sin sombrero'], ['gorra', 'Gorra'], ['gorro', 'Gorro'], ['fiesta', 'Sombrero de fiesta'], ['corona', 'Corona']] },
    accessory: { folder: 'accesorios', label: 'Accesorio', list: [['none', 'Sin accesorio'], ['gafas', 'Gafas'], ['pajarita', 'Pajarita'], ['bufanda', 'Bufanda']] },
    background: { label: 'Color', list: [['violeta', 'Violeta'], ['amarillo', 'Amarillo'], ['menta', 'Menta'], ['coral', 'Coral'], ['azul', 'Azul'], ['crema', 'Crema']] }
  };
  const DEFAULT_AVATAR = { animal: 'gato', hat: 'none', accessory: 'none', background: 'violeta' };

  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => [...r.querySelectorAll(s)];
  const norm = v => v.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLocaleLowerCase('es').trim();
  const NS = 'http://www.w3.org/2000/svg';
  const icon = id => { const s = document.createElementNS(NS, 'svg'); s.setAttribute('class', 'ping-icon'); s.setAttribute('aria-hidden', 'true'); const u = document.createElementNS(NS, 'use'); u.setAttribute('href', '#' + id); s.append(u); return s; };
  const el = (tag, attrs = {}, ...kids) => { const n = document.createElement(tag); for (const [k, v] of Object.entries(attrs)) { if (v === false || v == null) continue; if (k === 'class') n.className = v; else if (k === 'text') n.textContent = v; else n.setAttribute(k, v === true ? '' : v); } n.append(...kids.flat().filter(Boolean)); return n; };
  const plural = n => n === 1 ? '1 interés seleccionado' : n + ' intereses seleccionados'; // CONTRATO: contador real singular/plural

  const st = { screen: 'identity', alias: '', avatar: { ...DEFAULT_AVATAR }, tab: 'animal', node: null, sub: null, q: '', states: { identity: 'normal', interests: 'normal' }, visited: new Set(), opener: null };

  /* ───────── Tema (misma clave y comportamiento que theme-init.js / theme-toggle.js) ───────── */
  const setTheme = t => { document.documentElement.dataset.theme = t; $$('[data-theme-option]').forEach(b => b.setAttribute('aria-pressed', String(b.dataset.themeOption === t))); try { localStorage.setItem('ping-theme', t); } catch (_) { /* sin almacenamiento: no pasa nada */ } };
  $$('[data-theme-option]').forEach(b => b.addEventListener('click', () => setTheme(b.dataset.themeOption)));

  /* ───────── Avatar ───────── */
  const layers = a => [['animales', a.animal], ['accesorios', a.accessory], ['sombreros', a.hat]].filter(([, id]) => id && id !== 'none'); // orden real: animal → accesorio → sombrero
  const paint = (node, a) => { node.dataset.bg = a.background; node.replaceChildren(...layers(a).map(([f, id]) => el('img', { src: `${ASSETS}${f}/${encodeURIComponent(id)}.svg`, alt: '', width: 100, height: 100, decoding: 'async' }))); };
  const labelOf = (k, id) => (AV[k].list.find(([v]) => v === id) || [])[1] || id;

  function buildAvatarEditor() {
    const tabs = $('[data-tabs]'), panels = $('[data-panels]');
    tabs.replaceChildren(); panels.replaceChildren();
    for (const k of ['animal', 'hat', 'accessory', 'background']) {
      tabs.append(el('button', { type: 'button', class: 'tab', role: 'tab', id: 'tab-' + k, 'aria-controls': 'panel-' + k, 'data-tab': k },
        el('b', { text: AV[k].label }), el('small', { 'data-tab-value': k })));
      const grid = el('div', { class: k === 'background' ? 'opts opt-colors' : 'opts', 'data-cols': k === 'animal' ? 4 : k === 'hat' ? 5 : k === 'accessory' ? 4 : null });
      for (const [id, label] of AV[k].list) {
        if (k === 'background') {
          grid.append(el('label', { class: 'opt-color', 'data-bg': id }, el('input', { type: 'radio', name: k, value: id }), el('i'), el('span', { text: label })));
        } else {
          grid.append(el('label', { class: 'opt', 'data-kind': k, 'data-id': id },
            el('input', { type: 'radio', name: k, value: id }),
            el('span', { class: 'opt-face' + (id === 'none' ? ' is-none' : '') }),
            el('span', { class: 'opt-tick' }, icon('check')),
            el('span', { class: 'opt-name', text: label })));
        }
      }
      panels.append(el('div', { role: 'tabpanel', id: 'panel-' + k, 'aria-labelledby': 'tab-' + k, hidden: k !== st.tab }, grid));
    }
    // CONTRATO: radios nativos name=animal|hat|accessory|background, valores = ids permitidos; el servidor valida la lista.
    tabs.addEventListener('click', e => { const b = e.target.closest('[data-tab]'); if (b) selectTab(b.dataset.tab, true); });
    tabs.addEventListener('keydown', e => {
      const order = ['animal', 'hat', 'accessory', 'background'], i = order.indexOf(st.tab);
      const next = { ArrowRight: order[(i + 1) % 4], ArrowLeft: order[(i + 3) % 4], Home: order[0], End: order[3] }[e.key];
      if (next) { e.preventDefault(); selectTab(next, true); }
    });
    panels.addEventListener('change', e => { if (e.target.matches('input[type=radio]')) { st.avatar[e.target.name] = e.target.value; syncAvatar(); } });
  }
  function selectTab(k, focus) {
    st.tab = k;
    $$('[data-tab]').forEach(b => { const on = b.dataset.tab === k; b.setAttribute('aria-selected', String(on)); b.tabIndex = on ? 0 : -1; if (on && focus) b.focus(); });
    $$('[role=tabpanel]').forEach(p => { p.hidden = p.id !== 'panel-' + k; });
  }
  function syncAvatar() {
    const a = st.avatar;
    $$('[data-avatar-preview]').forEach(n => paint(n, a));
    $$('input[type=radio]', $('[data-panels]')).forEach(i => { i.checked = a[i.name] === i.value; });
    $$('.opt-face', $('[data-panels]')).forEach(f => {
      const o = f.parentElement, kind = o.dataset.kind, id = o.dataset.id;
      paint(f, { ...a, [kind]: id });
      if (id === 'none') f.dataset.bg = a.background;
    });
    for (const k of Object.keys(AV)) { const v = $(`[data-tab-value="${k}"]`); if (v) { v.textContent = labelOf(k, a[k]); v.parentElement.setAttribute('aria-label', `${AV[k].label}: ${labelOf(k, a[k])}`); } }
    $$('.me .avatar').forEach(n => paint(n, a));
  }

  /* ───────── Identidad ───────── */
  const alias = $('#alias'), aliasError = $('#alias-error');
  function validateAlias(show) {
    const ok = alias.value.trim().length > 0;
    alias.setAttribute('aria-invalid', String(!ok && show));
    aliasError.hidden = ok || !show;
    return ok;
  }
  alias.addEventListener('input', () => { st.alias = alias.value; if (!aliasError.hidden) validateAlias(true); });
  $('#identity-form').addEventListener('submit', e => {
    e.preventDefault();
    if (st.states.identity === 'closed') return;
    if (!validateAlias(true)) { alias.focus(); return; } // CONTRATO: alias obligatorio, máx. 80; mensaje real viene de th:errors
    go('interests', { keep: true });
  });

  /* ───────── Intereses ───────── */
  const fams = C.families.map(f => ({ ...f, ids: [...new Set([...f.direct, ...f.subs.flatMap(s => s.ids)])] }));
  const byId = new Map();
  for (const f of fams) { for (const id of f.direct) tag(id).f.add(f.key); for (const s of f.subs) for (const id of s.ids) { const t = tag(id); t.f.add(f.key); t.s.add(s.key); } }
  function tag(id) { if (!byId.has(id)) byId.set(id, { f: new Set(), s: new Set() }); return byId.get(id); }
  const chipsBox = $('[data-chips]');
  const chipFor = new Map();
  Object.entries(C.interests).sort((a, b) => a[1].localeCompare(b[1], 'es')).forEach(([id, name]) => {
    const t = byId.get(Number(id)) || { f: new Set(), s: new Set() };
    const input = el('input', { type: 'checkbox', name: 'interestIds', value: id, id: 'interest-' + id }); // CONTRATO: name=interestIds, value=ID del interés
    const chip = el('label', { class: 'chip', for: 'interest-' + id, 'data-name': norm(name), 'data-fams': [...t.f].join('|'), 'data-subs': [...t.s].join('|') },
      input, el('span', { class: 'chip-mark' }, icon('check')), el('span', {}, name));
    chipFor.set(Number(id), chip); chipsBox.append(chip);
  });
  const selected = () => [...chipFor].filter(([, c]) => c.querySelector('input').checked).map(([id]) => id);
  const nameOf = id => C.interests[id];
  let inactive = new Set();

  const worldsBox = $('[data-worlds]');
  fams.forEach((f, i) => {
    const b = el('button', { type: 'button', class: 'world', 'data-node': f.key, 'data-tone': i % 3 }, // el tono es decorativo; todas las familias tienen el mismo tamaño
      el('b', { text: f.label }),
      el('span', { class: 'world-foot' }, el('span', { class: 'world-count', hidden: true }), el('small', { text: `${f.ids.length} opciones` }), icon(f.icon)));
    b.addEventListener('click', () => openFamily(f.key, b));
    worldsBox.append(b);
  });

  function openFamily(key, opener) { st.node = key; st.sub = null; st.q = ''; $('#q').value = ''; st.opener = opener || null; applyView(); $('[data-fam-title]').focus({ preventScroll: false }); }
  function closeFamily() { const o = st.opener; st.node = null; st.sub = null; applyView(); if (o) o.focus(); }

  function applyView() {
    const q = norm(st.q), f = fams.find(x => x.key === st.node);
    const mode = q ? 'results' : f ? 'family' : 'worlds';
    $('[data-view=worlds]').hidden = mode !== 'worlds';
    $('[data-view=list]').hidden = mode === 'worlds';
    $('[data-fam-head]').hidden = mode !== 'family';
    $('[data-subnav]').hidden = mode !== 'family' || !f.subs.length;
    $('[data-results-title]').hidden = mode !== 'results';
    if (mode === 'family') {
      $('[data-fam-title]').replaceChildren(icon(f.icon), document.createTextNode(f.label));
      const sub = $('[data-subnav]'); sub.replaceChildren(
        ...[{ key: null, label: 'Todo' }, ...f.subs].map(s => { const b = el('button', { type: 'button', class: 'subchip', 'aria-pressed': String((st.sub || null) === s.key), text: s.label }); b.addEventListener('click', () => { st.sub = s.key; applyView(); }); return b; }));
    }
    let visible = 0;
    chipFor.forEach((chip, id) => {
      const inact = inactive.has(id);
      if (inact) return; // los conservados viven en su propia sección y siguen visibles
      let show = false;
      if (mode === 'results') show = chip.dataset.name.includes(q);
      else if (mode === 'family') show = chip.dataset.fams.split('|').includes(f.key) && (!st.sub || chip.dataset.subs.split('|').includes(st.sub));
      chip.hidden = !show; if (show) visible++; // los checkboxes ocultos conservan su estado (CONTRATO)
    });
    const status = $('[data-filter-status]');
    status.textContent = mode === 'results' ? `${visible} opciones visibles. Tus elecciones se conservan.` : '';
    $('[data-empty]').hidden = !(mode === 'results' && visible === 0);
    if (mode === 'family') refreshFamilyMeta();
  }
  function refreshFamilyMeta() {
    const f = fams.find(x => x.key === st.node); if (!f) return;
    const sel = new Set(selected());
    const pool = st.sub ? f.subs.find(s => s.key === st.sub).ids : f.ids;
    const n = pool.filter(id => sel.has(id) && !inactive.has(id)).length;
    $('[data-fam-meta]').textContent = `${pool.filter(id => !inactive.has(id)).length} opciones · ${n === 1 ? '1 elegido' : n + ' elegidos'}`;
  }
  $('#q').addEventListener('input', e => { st.q = e.target.value; applyView(); });
  $('#q').addEventListener('keydown', e => { if (e.key === 'Enter') e.preventDefault(); }); // CONTRATO: Enter filtra, no envía
  $('[data-fam-back]').addEventListener('click', closeFamily);
  $('[data-clear-filter]').addEventListener('click', () => { st.q = ''; $('#q').value = ''; applyView(); $('#q').focus(); });

  function refreshSelection(changedId) {
    const ids = selected(), n = ids.length;
    $$('[data-selected-count]').forEach(p => { p.textContent = plural(n); });
    const sel = new Set(ids);
    $$('.world').forEach(w => {
      const f = fams.find(x => x.key === w.dataset.node), c = f.ids.filter(id => sel.has(id)).length, badge = $('.world-count', w);
      badge.textContent = c; badge.hidden = c === 0; $('small', w).textContent = c ? `de ${f.ids.length}` : `${f.ids.length} opciones`;
      w.setAttribute('aria-label', `${f.label}, ${f.ids.length} opciones${c ? ', ' + (c === 1 ? '1 elegido' : c + ' elegidos') : ''}`);
    });
    // Bandeja «Elegidos»: CONTRATO = resumen de hasta 3 nombres + «+N más» y detalle con todos.
    $('[data-tray]').classList.toggle('is-empty', n === 0);
    $('[data-skip-hint]').hidden = n > 0; $('[data-tray-body]').hidden = n === 0;
    $('[data-tray-count]').textContent = n;
    const chips = $('[data-tray-chips]'), all = $('[data-tray-all]');
    const prevTray = st.tray || new Set(); st.tray = new Set(ids);
    const mk = id => { const b = el('button', { type: 'button', class: 'tray-chip', 'data-new': prevTray.has(id) ? null : '', 'aria-label': 'Quitar ' + nameOf(id) }, el('span', { text: nameOf(id) }), icon('close')); b.addEventListener('click', () => { const i = chipFor.get(id).querySelector('input'); i.checked = false; i.dispatchEvent(new Event('change', { bubbles: true })); }); return b; };
    const more = el('button', { type: 'button', class: 'tray-chip more', 'aria-controls': 'tray-all', 'aria-expanded': String(!all.hidden), 'aria-label': 'Ver todos los intereses elegidos' }, el('span', { text: all.hidden ? `+${n - 3} más` : 'Ocultar' }));
    more.addEventListener('click', () => { all.hidden = !all.hidden; refreshSelection(); });
    if (n <= 3) all.hidden = true;
    chips.replaceChildren(...ids.slice(0, 3).map(id => mk(id)), ...(n > 3 ? [more] : []));
    all.replaceChildren(...ids.map(mk));
    refreshFamilyMeta();
  }
  $('#interests-form').addEventListener('change', e => { if (e.target.matches('input[name=interestIds]')) refreshSelection(Number(e.target.value)); });

  // Marca local de presión (mismo criterio que interest-selector.js: solo feedback visual, nunca bloquea el scroll)
  let pressed;
  chipsBox.addEventListener('pointerdown', e => { const c = e.target.closest('.chip'); if (e.isPrimary && c) { pressed = c; c.dataset.pressed = 'true'; } }, { passive: true });
  const release = () => { if (pressed) delete pressed.dataset.pressed; pressed = null; };
  window.addEventListener('pointerup', release, { passive: true }); window.addEventListener('pointercancel', release, { passive: true });
  document.addEventListener('scroll', release, { capture: true, passive: true });

  function setSelection(ids) { chipFor.forEach((c, id) => { c.querySelector('input').checked = ids.includes(id); }); refreshSelection(); }
  const pick = () => {
    const first = k => fams.find(f => f.key === k);
    return [first('games').subs[2].ids[0], first('games').subs[2].ids[1], first('music').subs[0].ids[0], first('screen').subs[0].ids[0], first('tech').subs[0].ids[0]];
  };

  /* ───────── Estados simulados (no son estados reales de PING; ilustran los contratos) ───────── */
  const STATES = {
    identity: [['normal', 'Normal'], ['error', 'Alias vacío tras enviar'], ['closed', 'Evento sin entradas']],
    interests: [['normal', 'Nuevo (sin selección)'], ['previa', 'Con selección previa'], ['conservados', 'Con intereses conservados'], ['error', 'Error del servidor']]
  };
  function applyState(s) {
    st.states[st.screen] = s;
    const identity = st.screen === 'identity';
    if (identity) {
      $('[data-closed]').hidden = s !== 'closed'; $('#identity-cta').disabled = s === 'closed';
      $('[data-event-status]').textContent = s === 'closed' ? 'Pausado' : 'Abierto'; // etiquetas reales de EventStatus: Borrador/Abierto/Pausado/Cerrado
      alias.value = s === 'error' ? '' : alias.value; validateAlias(s === 'error');
    } else {
      inactive = new Set(); $('[data-retained]').hidden = true; $('[data-interest-error]').hidden = s !== 'error';
      // devolver los chips conservados a la lista principal antes de recalcular
      chipFor.forEach(c => { if (c.parentElement !== chipsBox) { chipsBox.append(c); c.querySelector('small')?.remove(); } });
      if (s === 'normal') setSelection([]);
      if (s === 'previa' || s === 'error') setSelection(pick());
      if (s === 'conservados') {
        const kept = fams.find(f => f.key === 'legacy').direct.slice(0, 2); inactive = new Set(kept);
        setSelection([...kept, ...pick().slice(0, 3)]);
        const box = $('[data-retained-list]'); box.replaceChildren();
        kept.forEach(id => { const c = chipFor.get(id); c.append(el('small', { text: 'No disponible para añadir' })); c.hidden = false; box.append(c); });
        $('[data-retained]').hidden = false;
      }
      applyView();
    }
  }

  /* ───────── Navegación del prototipo ───────── */
  function go(screen, opts = {}) {
    st.screen = screen;
    ['identity', 'interests', 'end'].forEach(s => { $('#screen-' + s).hidden = s !== screen; });
    if (screen !== 'end') $('#pp-screen').value = screen;
    const sel = $('#pp-state'); sel.replaceChildren(...(STATES[screen] || []).map(([v, l]) => el('option', { value: v, text: l })));
    if (screen === 'interests') { $$('[data-me-alias]').forEach(n => { n.textContent = alias.value.trim() || 'Tu alias'; }); }
    if (screen !== 'end') {
      if (opts.state || !(opts.keep && st.visited.has(screen))) applyState(opts.state || 'normal'); // volver no borra el borrador (como la app)
      sel.value = st.states[screen]; st.visited.add(screen);
    }
    window.scrollTo(0, 0);
    if (!opts.silent) $('#screen-' + screen + ' h1').focus({ preventScroll: true });
  }
  $$('h1').forEach(h => { h.tabIndex = -1; });
  $('#pp-screen').addEventListener('change', e => go(e.target.value));
  $('#pp-state').addEventListener('change', e => applyState(e.target.value));
  $('#pp-reset').addEventListener('click', () => { alias.value = ''; st.alias = ''; st.avatar = { ...DEFAULT_AVATAR }; st.node = null; st.sub = null; st.q = ''; $('#q').value = ''; st.visited.clear(); st.states = { identity: 'normal', interests: 'normal' }; syncAvatar(); selectTab('animal'); go('identity', { silent: true }); });
  $('[data-act=complete]').addEventListener('click', () => go('end'));
  $('[data-act=back]').addEventListener('click', () => go('identity', { keep: true }));
  $$('[data-act=changeEvent], [data-act=home]').forEach(b => b.addEventListener('click', e => { e.preventDefault(); $('[data-proto-note]').hidden = false; $('[data-proto-note]').focus(); }));
  $('[data-act=restart]').addEventListener('click', () => $('#pp-reset').click());

  /* ───────── Arranque ───────── */
  buildAvatarEditor(); syncAvatar(); selectTab('animal');
  const p = new URLSearchParams(location.search);
  try { setTheme(p.get('theme') || (localStorage.getItem('ping-theme') === 'light' ? 'light' : 'dark')); } catch (_) { setTheme('dark'); }
  const wanted = (p.get('screen') || location.hash.slice(1) || 'identity').replace('identidad', 'identity').replace('intereses', 'interests');
  if (p.get('alias')) { alias.value = p.get('alias'); st.alias = alias.value; }
  go(wanted === 'interests' ? 'interests' : 'identity', { silent: true, state: p.get('state') || undefined });
  if (p.get('family')) { openFamily(p.get('family')); if (p.get('sub')) { st.sub = p.get('sub'); applyView(); } }
  if (p.get('tab')) selectTab(p.get('tab'));
  if (p.get('q')) { $('#q').value = p.get('q'); st.q = p.get('q'); applyView(); }
  if (p.has('tools')) $('#pp-tools').open = true;
})();
