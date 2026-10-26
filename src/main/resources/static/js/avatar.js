// DEC-058 · Editor de avatar V3 (/join y /me/avatar). El servidor entrega radios nativos con el catálogo y valida al guardar;
// aquí se agrupan en pestañas, se componen la vista previa y las miniaturas con las mismas reglas de capas que AvatarV3.layers(),
// se retira la pieza incompatible avisando antes de guardar y se marca el borrador «sin guardar». Sin temporizadores ni consultas.
document.querySelectorAll('[data-avatar-v3]').forEach(root => {
  const form = root.closest('form') || root;
  const bulky = new Set((root.dataset.bulky || '').split(',').filter(Boolean));
  const withRear = new Set(['auriculares', 'microfono', 'vincha', 'pase']);
  const discs = { natural: '#ECE4F6', dorado: '#FBE9CF', lila: '#E6DDF5', perla: '#E1E8F0', cacao: '#E9DDD2', coral: '#FAE0DB' };
  const slots = ['head', 'audio', 'face', 'neck'];
  const value = name => form.querySelector(`input[name="${name}"]:checked`)?.value;
  const label = name => form.querySelector(`input[name="${name}"]:checked`)?.dataset.label || '';
  const state = () => {
    const s = { animal: value('animal'), palette: value('palette') };
    slots.forEach(k => { s[k] = value(k) || 'none'; s[k + 'Color'] = value(k + 'Color') || 'violeta'; });
    return s;
  };
  const base = '/img/avatars/v3/';
  function layers(s) {
    const out = [];
    [['neck'], ['head'], ['audio']].forEach(([k]) => { if (withRear.has(s[k])) out.push(`${base}rear/${s.animal}-${s[k]}-${s[k + 'Color']}.svg`); });
    out.push(`${base}base/${s.animal}-${s.palette}.svg`);
    ['neck', 'face', 'head'].forEach(k => { if (s[k] !== 'none') out.push(`${base}front/${s.animal}-${s[k]}-${s[k + 'Color']}.svg`); });
    if (s.animal === 'conejo' && s.head === 'bucket') out.push(`${base}base/conejo-${s.palette}-orejas.svg`);
    if (s.audio !== 'none') out.push(`${base}front/${s.animal}-${s.audio}-${s.audioColor}.svg`);
    return out;
  }
  const paint = (node, s, lazy) => {
    node.style.setProperty('--avatar-disc', discs[s.palette] || discs.natural);
    const srcs = layers(s);
    const current = [...node.querySelectorAll('img')].map(i => i.getAttribute('src').replace(/-[0-9a-f]{32}(?=\.svg$)/, ''));
    if (current.join('|') === srcs.join('|')) return;
    node.replaceChildren(...srcs.map(src => { const img = document.createElement('img'); img.alt = ''; img.width = 256; img.height = 256; img.decoding = 'async'; if (lazy) img.loading = 'lazy'; img.src = src; return img; }));
  };
  const blank = s => ({ animal: s.animal, palette: s.palette, head: 'none', headColor: s.headColor, audio: 'none', audioColor: s.audioColor, face: 'none', faceColor: s.faceColor, neck: 'none', neckColor: s.neckColor });

  // Pestañas: Animal, Pelaje y un slot por pieza (con su fila de color). Sin JS se ven todos los grupos apilados.
  const targets = [...root.querySelectorAll(':scope > [data-av-panel], :scope > [data-av3-slot]')];
  const keyOf = t => t.dataset.avPanel || t.dataset.av3Slot;
  const titleOf = t => (t.dataset.label || t.querySelector('[data-av-panel]')?.dataset.label);
  let tabs = [];
  if (targets.length) {
    root.classList.add('av3-js');
    const list = document.createElement('div'); list.className = 'av3-tabs'; list.setAttribute('role', 'tablist'); list.setAttribute('aria-label', 'Partes del avatar');
    const uid = Math.random().toString(36).slice(2, 7);
    tabs = targets.map((t, i) => {
      const tab = document.createElement('button'); tab.type = 'button'; tab.className = 'av3-tab'; tab.setAttribute('role', 'tab');
      tab.id = `av3-tab-${uid}-${i}`; t.id = t.id || `av3-panel-${uid}-${i}`; tab.setAttribute('aria-controls', t.id);
      t.setAttribute('role', 'tabpanel'); t.setAttribute('aria-labelledby', tab.id); tab.textContent = titleOf(t);
      tab.addEventListener('click', () => select(i, false));
      tab.addEventListener('keydown', e => { const d = e.key === 'ArrowRight' ? 1 : e.key === 'ArrowLeft' ? -1 : 0; if (!d) return; e.preventDefault(); select((i + d + tabs.length) % tabs.length, true); });
      list.append(tab); return tab;
    });
    root.querySelector('.av3-conflict').after(list);
  }
  function select(i, focus) {
    tabs.forEach((tab, j) => { const on = i === j; tab.setAttribute('aria-selected', String(on)); tab.tabIndex = on ? 0 : -1; targets[j].hidden = !on; });
    if (focus) tabs[i].focus();
    tabs[i].scrollIntoView({ block: 'nearest', inline: 'nearest' });
    paintTiles(targets[i]);
  }
  function paintTiles(target) {
    const s = state();
    target.querySelectorAll('[data-av3-tile]').forEach(tile => {
      const kind = tile.dataset.kind, id = tile.dataset.id, t = blank(s);
      if (kind === 'animal') t.animal = id; else if (kind === 'palette') t.palette = id; else t[kind] = id;
      paint(tile, t, true);
    });
  }

  // Estado del borrador frente a lo guardado (radios con defaultChecked).
  const chip = form.querySelector('[data-av3-chip]'), undo = form.querySelector('[data-av3-undo]'), ring = form.querySelector('[data-av3-ring]');
  const summary = form.querySelector('[data-av3-summary]'), notice = root.querySelector('[data-av3-conflict]');
  const radios = () => [...root.querySelectorAll('input[type="radio"]')];
  // Referencia de «guardado»: data-saved trae la selección persistida (/me/avatar). Tras un 400 los radios muestran el borrador
  // rechazado, así que no se compara con defaultChecked. data-saved="none": no hay V3 guardado y el borrador es una propuesta.
  const savedAttr = root.dataset.saved;
  const proposal = savedAttr === 'none';
  const saved = savedAttr && !proposal ? Object.fromEntries(savedAttr.split(';').map(p => p.split('='))) : null;
  const isSaved = r => saved ? saved[r.name] === r.value : r.defaultChecked;
  const dirty = () => proposal || radios().some(r => r.checked !== isSaved(r));
  const changed = () => radios().some(r => r.checked !== isSaved(r));
  const describe = () => {
    const s = state(), parts = [label('animal'), 'pelaje ' + label('palette').toLowerCase()];
    slots.forEach(k => { if (s[k] !== 'none') parts.push(label(k) + ' color ' + label(k + 'Color').toLowerCase()); });
    return parts.join(', ');
  };
  function refresh() {
    const s = state();
    form.querySelectorAll('[data-avatar-preview]').forEach(p => { paint(p, s, false); p.setAttribute('aria-label', 'Vista previa de tu avatar: ' + describe()); });
    if (summary) summary.textContent = describe();
    slots.forEach(k => { const row = root.querySelector(`[data-av3-colors="${k}"]`); if (row) { const off = s[k] === 'none'; row.classList.toggle('is-off', off); row.querySelectorAll('input').forEach(i => { i.disabled = off; }); } });
    const isDirty = dirty();
    if (chip) chip.textContent = proposal ? 'Propuesta · sin guardar' : isDirty ? 'Probando · sin guardar' : 'Tu avatar guardado';
    ring?.classList.toggle('is-trying', isDirty);
    if (undo) { undo.hidden = false; undo.disabled = !changed(); }
    const visible = targets.find(t => !t.hidden); if (visible && targets.length) paintTiles(visible);
  }
  // Incompatibilidad: la última elección se conserva y se retira la otra pieza, explicándolo antes de guardar.
  function resolve(changed) {
    const s = state();
    if (s.audio === 'none' || !bulky.has(s.head)) return;
    const drop = changed === 'audio' ? 'head' : 'audio';
    const keep = changed === 'audio' ? label('audio') : label('head');
    const removed = drop === 'audio' ? label('audio') : label('head');
    form.querySelector(`input[name="${drop}"][value="none"]`).checked = true;
    notice.textContent = `Quitamos «${removed}»: no combina con «${keep}» porque se cruzan. Puedes elegir otra pieza.`;
    notice.hidden = false;
  }
  root.addEventListener('change', e => {
    const name = e.target.name;
    if (name === 'head' || name === 'audio') { notice.hidden = true; resolve(name); }
    if (name === 'animal' || name === 'palette') targets.forEach(t => { if (!t.hidden) paintTiles(t); });
    refresh();
  });
  undo?.addEventListener('click', () => { radios().forEach(r => { r.checked = isSaved(r); }); notice.hidden = true; notice.textContent = ''; refresh(); undo.disabled = true; chip?.focus?.(); });
  if (targets.length) select(0, false);
  refresh();
});

// D07: «Así te verán» refleja el alias mientras se escribe (solo texto; el servidor valida al guardar).
document.querySelectorAll('[data-alias-echo]').forEach(echo => {
  const input = echo.closest('form')?.querySelector('#alias');
  if (!input) return;
  input.addEventListener('input', () => { echo.textContent = input.value.trim() || echo.dataset.empty; });
});
