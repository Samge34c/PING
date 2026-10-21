// Editor de avatar (/join y /me/avatar). El servidor entrega radios nativos con las listas permitidas;
// aquí se agrupan en pestañas, se actualiza la vista previa y las miniaturas se componen solo para el grupo visible.
document.querySelectorAll('[data-avatar-picker]').forEach(root => {
  const form = root.closest('form') || root;
  const previews = [...new Set([...form.querySelectorAll('[data-avatar-preview]'), ...root.querySelectorAll('[data-avatar-preview]')])];
  const panels = [...root.querySelectorAll('[data-av-panel]')];
  const folders = { animal: 'animales', accessory: 'accesorios', hat: 'sombreros' };
  const order = ['animal', 'accessory', 'hat']; // orden de capas: animal, accesorio, sombrero

  const checked = name => root.querySelector(`input[name="${name}"]:checked`);
  const current = () => ({
    animal: checked('animal')?.value, hat: checked('hat')?.value,
    accessory: checked('accessory')?.value, background: checked('background')?.value
  });
  const allowed = (name, id) => [...root.querySelectorAll(`input[name="${name}"]`)].some(input => input.value === id);

  function layers(avatar) {
    return order.filter(key => avatar[key] && avatar[key] !== 'none' && allowed(key, avatar[key])).map(key => {
      const img = document.createElement('img');
      img.alt = '';
      img.width = 100;
      img.height = 100;
      img.decoding = 'async';
      img.src = '/img/avatars/' + folders[key] + '/' + encodeURIComponent(avatar[key]) + '.svg';
      return img;
    });
  }

  function paintPreview() {
    const avatar = current();
    previews.forEach(node => {
      node.className = node.className.replace(/\bavatar-bg-\S+/g, '').trim() + ' avatar-bg-' + avatar.background;
      node.replaceChildren(...layers(avatar));
    });
  }

  // Miniaturas de sombrero y accesorio: se muestran sobre el animal elegido, solo para el panel activo.
  function paintPanel(panel) {
    if (!panel) return;
    const avatar = current();
    const signature = [avatar.animal, avatar.hat, avatar.accessory, avatar.background].join('|');
    if (panel.dataset.painted === signature) return;
    panel.dataset.painted = signature;
    panel.querySelectorAll('.av-face').forEach(face => {
      face.className = face.className.replace(/\bavatar-bg-\S+/g, '').trim() + ' avatar-bg-' + avatar.background;
      const kind = face.dataset.kind;
      if (kind === 'animal') return; // la imagen ya viene del servidor
      face.replaceChildren(...layers({ ...avatar, [kind]: face.dataset.id }));
    });
  }

  if (panels.length) {
    root.classList.add('av-js');
    const tabs = document.createElement('div');
    tabs.className = 'av-tabs';
    tabs.setAttribute('role', 'tablist');
    tabs.setAttribute('aria-label', 'Partes del avatar');
    const uid = Math.random().toString(36).slice(2, 7);
    panels.forEach(panel => {
      const key = panel.dataset.avPanel;
      const tab = document.createElement('button');
      tab.type = 'button';
      tab.className = 'av-tab';
      tab.id = `av-tab-${key}-${uid}`;
      tab.dataset.avTab = key;
      tab.setAttribute('role', 'tab');
      tab.setAttribute('aria-controls', `av-panel-${key}-${uid}`);
      tab.innerHTML = '<b></b><small></small>';
      tab.firstChild.textContent = panel.dataset.label;
      tabs.append(tab);
      panel.id = `av-panel-${key}-${uid}`;
      panel.setAttribute('role', 'tabpanel');
      panel.setAttribute('aria-labelledby', tab.id);
    });
    panels[0].before(tabs);

    const select = (key, focus) => {
      tabs.querySelectorAll('.av-tab').forEach(tab => {
        const on = tab.dataset.avTab === key;
        tab.setAttribute('aria-selected', String(on));
        tab.tabIndex = on ? 0 : -1;
        if (on && focus) tab.focus();
      });
      panels.forEach(panel => { panel.hidden = panel.dataset.avPanel !== key; });
      paintPanel(panels.find(panel => panel.dataset.avPanel === key));
    };
    const labels = () => tabs.querySelectorAll('.av-tab').forEach(tab => {
      const name = tab.dataset.avTab;
      const value = checked(name)?.dataset.label || '';
      tab.lastChild.textContent = value;
      tab.setAttribute('aria-label', `${panels.find(panel => panel.dataset.avPanel === name).dataset.label}: ${value}`);
    });
    tabs.addEventListener('click', event => {
      const tab = event.target.closest('.av-tab');
      if (tab) select(tab.dataset.avTab, false);
    });
    tabs.addEventListener('keydown', event => {
      const keys = panels.map(panel => panel.dataset.avPanel);
      const index = keys.indexOf(tabs.querySelector('[aria-selected="true"]').dataset.avTab);
      const next = { ArrowRight: keys[(index + 1) % keys.length], ArrowLeft: keys[(index + keys.length - 1) % keys.length], Home: keys[0], End: keys[keys.length - 1] }[event.key];
      if (next) { event.preventDefault(); select(next, true); }
    });
    root.addEventListener('change', () => {
      labels();
      paintPreview();
      paintPanel(panels.find(panel => !panel.hidden));
    });
    labels();
    select('animal', false);
  } else {
    root.addEventListener('change', paintPreview);
  }
});
