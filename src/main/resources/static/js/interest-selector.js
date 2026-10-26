(() => {
  const normalize = value => value.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLocaleLowerCase('es').trim();
  // Icono local de cada mundo (sprite existente); una clave desconocida usa el corazón.
  const worldIcons = { games: 'game', music: 'music', screen: 'film', anime: 'spark', tech: 'code', tabletop: 'users', creative: 'camera', active: 'ball', everyday: 'smile', platforms: 'bolt', legacy: 'heart' };
  const spriteBase = () => (document.querySelector('use[href*="icons"]')?.getAttribute('href') || '/img/icons.svg#x').split('#')[0];
  const plural = (n, one, many) => n + ' ' + (n === 1 ? one : many);

  const init = form => {
    if (!form || form.dataset.categoriesReady) return;
    const root = form.querySelector('[data-interest-selector]');
    if (!root) return;
    form.dataset.categoriesReady = 'true';
    const groups = [...root.querySelectorAll('[data-interest-group]')];
    const filter = root.querySelector('[data-interest-filter]');
    const search = root.querySelector('[data-interest-search]');
    const rows = [...root.querySelectorAll('[data-interest-name]')];
    const checkbox = row => row.querySelector('input');

    // Touch browsers do not consistently expose :active on labels. Visual feedback only;
    // the native label/checkbox still owns selection and scrolling is never prevented.
    let pressed, pointer;
    const release = event => {
      if (event?.pointerId != null && event.pointerId !== pointer) return;
      if (pressed) delete pressed.dataset.pressed;
      pressed = null; pointer = null;
    };
    root.addEventListener('pointerdown', event => {
      if (!event.isPrimary || pressed) return;
      const label = event.target.closest('.interest-row-content');
      if (!label?.control || label.control.disabled) return;
      pressed = label; pointer = event.pointerId; label.dataset.pressed = 'true';
    }, { passive: true });
    window.addEventListener('pointerup', release, { passive: true });
    window.addEventListener('pointercancel', release, { passive: true });
    window.addEventListener('blur', release);
    window.addEventListener('pagehide', release);
    document.addEventListener('scroll', release, { capture: true, passive: true });

    // ───────── Mundos y familias (solo con taxonomía) · DEC-058 D08-1 ─────────
    // Las filas reales (checkbox incluido) se colocan en la familia del mundo elegido; al buscar vuelven a su categoría.
    const taxonomy = root.querySelector('[data-taxonomy]');
    const worlds = taxonomy ? [...taxonomy.querySelectorAll('.ic-world')] : [];
    const fams = taxonomy ? [...taxonomy.querySelectorAll('.ic-fam[data-family-key]')] : [];
    const rest = taxonomy?.querySelector('[data-family-rest]');
    const familiesBox = taxonomy?.querySelector('[data-families]');
    const nav = taxonomy?.querySelector('[data-taxonomy-nav]');
    const sections = [...fams, rest].filter(Boolean);
    const parentOf = key => fams.find(section => section.dataset.familyKey === key)?.dataset.familyParent || null;
    const memberships = row => (row.dataset.interestNodes || '').split('|').filter(Boolean);
    const worldsOf = new Map(rows.map(row => [row, new Set(memberships(row).map(key => parentOf(key) || key))]));
    const home = new Map(rows.map(row => [row, row.parentElement]));
    const retained = new Set(rows.filter(row => row.closest('[data-interest-group]')?.dataset.interestGroup === 'retained'));
    const familyIn = (row, world) => memberships(row).find(key => parentOf(key) === world) || (worldsOf.get(row).has(world) ? 'rest' : null);
    const familyKey = row => memberships(row).find(key => parentOf(key)) || memberships(row)[0] || home.get(row).closest('[data-interest-group]')?.dataset.interestGroup;
    let family = null, reviewing = false;

    const restoreAll = () => { rows.forEach(row => home.get(row).append(row)); sections.forEach(section => { section.hidden = true; }); };
    const placeWorld = world => {
      const buckets = new Map();
      rows.forEach(row => {
        if (retained.has(row)) return;
        const key = familyIn(row, world);
        const section = key === 'rest' ? rest : fams.find(item => item.dataset.familyKey === key);
        if (!section) return;
        if (!buckets.has(section)) buckets.set(section, []);
        buckets.get(section).push(row);
      });
      sections.forEach(section => {
        const list = buckets.get(section);
        section.hidden = !list;
        if (list) { list.forEach(row => { row.hidden = false; }); section.querySelector('[data-family-list]').append(...list); }
      });
      return [...buckets.values()].reduce((total, list) => total + list.length, 0);
    };
    const refreshWorlds = () => {
      worlds.forEach(button => {
        const members = rows.filter(row => worldsOf.get(row).has(button.dataset.nodeKey));
        const chosen = members.filter(row => checkbox(row).checked).length;
        button.querySelector('[data-node-selected]').textContent = chosen || '';
        button.dataset.chosen = chosen ? 'true' : 'false';
        button.setAttribute('aria-label', button.dataset.nodeLabel + ': ' + (chosen ? plural(chosen, 'elegido', 'elegidos') + ' de ' + members.length : plural(members.length, 'opción', 'opciones')));
      });
      sections.forEach(section => {
        if (section.hidden) return;
        const list = [...section.querySelectorAll('[data-interest-name]')];
        const chosen = list.filter(row => checkbox(row).checked).length;
        section.querySelector('[data-family-count]').textContent = chosen ? chosen + ' de ' + list.length : '';
      });
    };
    if (taxonomy) {
      const base = spriteBase();
      worlds.forEach(button => button.querySelector('[data-world-icon]').setAttribute('href', base + '#' + (worldIcons[button.dataset.nodeKey] || 'heart')));
      taxonomy.hidden = false;
      root.classList.add('is-taxonomy');
      filter.closest('div').hidden = true;
      // Abre el primer mundo con elecciones; si no hay ninguna, el primero del catálogo.
      family = (worlds.find(button => rows.some(row => checkbox(row).checked && worldsOf.get(row).has(button.dataset.nodeKey))) || worlds[0])?.dataset.nodeKey || null;
      worlds.forEach(button => button.addEventListener('click', () => {
        family = button.dataset.nodeKey;
        if (search) search.value = '';
        applyFilter();
        button.scrollIntoView({ block: 'nearest', inline: 'nearest' });
      }));
    }

    // ───────── Bandeja de elegidos ─────────
    const tray = root.querySelector('[data-tray]');
    const trayChips = root.querySelector('[data-tray-chips]');
    const trayAll = root.querySelector('[data-tray-all]');
    const trayMore = root.querySelector('[data-tray-more]');
    const trayCount = root.querySelector('[data-tray-count]');
    let previous = new Set(rows.filter(row => checkbox(row).checked).map(row => checkbox(row).value));
    const chip = (row, fresh) => {
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'ic-chip';
      if (fresh) button.dataset.new = '';
      button.setAttribute('aria-label', 'Quitar ' + row.dataset.interestName);
      const name = document.createElement('span'); name.textContent = row.dataset.interestName;
      const mark = document.createElement('i'); mark.setAttribute('aria-hidden', 'true'); mark.textContent = '×';
      button.append(name, mark);
      button.addEventListener('click', () => {
        const input = checkbox(row);
        input.checked = false;
        input.dispatchEvent(new Event('change', { bubbles: true }));
        // La bandeja se vuelve a dibujar: el foco pasa al primer chip que quede o, si no hay, a la búsqueda.
        const container = trayAll.hidden ? trayChips : trayAll;
        (tray.hidden ? (search || filter) : container.querySelector('.ic-chip'))?.focus();
      });
      return button;
    };
    const renderTray = selected => {
      if (!tray) return;
      const ids = new Set(selected.map(row => checkbox(row).value));
      tray.hidden = !selected.length || (!!taxonomy && !reviewing);
      root.querySelector('.ic-summary')?.classList.toggle('is-tray', selected.length > 0);
      trayCount.textContent = selected.length;
      trayChips.replaceChildren(...selected.slice(0, 3).map(row => chip(row, !previous.has(checkbox(row).value))));
      trayAll.replaceChildren(...selected.map(row => chip(row, false)));
      const extra = selected.length > 3;
      trayMore.hidden = !extra;
      if (!extra) { trayAll.hidden = true; trayMore.setAttribute('aria-expanded', 'false'); }
      trayMore.textContent = trayAll.hidden ? '+' + (selected.length - 3) + ' más' : 'Menos';
      trayChips.hidden = !trayAll.hidden && extra;
      previous = ids;
    };
    trayMore?.addEventListener('click', () => {
      const open = trayAll.hidden;
      trayAll.hidden = !open;
      trayChips.hidden = open;
      trayMore.setAttribute('aria-expanded', String(open));
      trayMore.textContent = open ? 'Menos' : '+' + (rows.filter(row => checkbox(row).checked).length - 3) + ' más';
    });

    // «Revisar» del pie abre la bandeja con todos los elegidos; quitar sigue siendo desmarcar el checkbox real.
    const review = taxonomy ? form.querySelector('[data-review]') : null;
    const setReview = open => {
      reviewing = open;
      if (!review) return;
      review.setAttribute('aria-expanded', String(open));
      review.textContent = open ? 'Ocultar' : 'Revisar';
    };
    review?.addEventListener('click', () => {
      setReview(!reviewing);
      updateSelection();
      if (!reviewing) return;
      if (!trayMore.hidden && trayAll.hidden) trayMore.click();
      tray.scrollIntoView({ block: 'nearest' });
      (trayAll.hidden ? trayChips : trayAll).querySelector('.ic-chip')?.focus({ preventScroll: true });
    });
    const updateSelection = () => {
      const selected = rows.filter(row => checkbox(row).checked);
      root.querySelector('[data-selection-summary]').textContent = selected.length ? selected.slice(0, 3).map(row => row.dataset.interestName).join(' · ') + (selected.length > 3 ? ' · +' + (selected.length - 3) + ' más' : '') : 'Aún ninguno. Puedes continuar sin elegir.';
      const details = root.querySelector('[data-selected-details]');
      if (details) { details.hidden = selected.length <= 3; details.querySelector('[data-selected-list]').textContent = selected.map(row => row.dataset.interestName).join(' · '); }
      const count = form.querySelector('[data-selected-count]');
      if (count) count.textContent = taxonomy
        ? (selected.length ? plural(selected.length, 'elegido', 'elegidos') + ' en ' + plural(new Set(selected.map(familyKey)).size, 'familia', 'familias') : 'Aún ninguno. Puedes continuar sin elegir.')
        : (selected.length === 1 ? '1 interés seleccionado' : selected.length + ' intereses seleccionados');
      if (review) { review.hidden = !selected.length; if (!selected.length && reviewing) setReview(false); }
      renderTray(selected);
      if (taxonomy) refreshWorlds();
    };

    // ───────── Filtro y búsqueda (ocultar nunca desmarca ni deshabilita) ─────────
    const applyFilter = () => {
      const query = normalize(search?.value || '');
      const browsing = !!taxonomy && !query && filter.value === 'all' && !!family;
      if (taxonomy) restoreAll();
      let visible = 0;
      groups.forEach(group => {
        let matches = 0;
        group.querySelectorAll('[data-interest-name]').forEach(row => {
          row.hidden = !normalize(row.dataset.interestName).includes(query);
          if (!row.hidden) matches++;
        });
        group.hidden = (filter.value !== 'all' && filter.value !== group.dataset.interestGroup) || matches === 0 || (browsing && group.dataset.interestGroup !== 'retained');
        if (!group.hidden) visible += matches;
      });
      if (taxonomy) {
        if (browsing) visible += placeWorld(family);
        worlds.forEach(button => button.setAttribute('aria-pressed', String(browsing && button.dataset.nodeKey === family)));
        familiesBox.hidden = !browsing;
        nav.hidden = browsing;
        refreshWorlds();
      }
      root.querySelector('[data-interest-empty]').hidden = visible !== 0;
      root.querySelector('[data-filter-status]').textContent = query || filter.value !== 'all' ? plural(visible, 'opción visible', 'opciones visibles') + '. Tus elecciones se conservan.' : '';
    };
    root.querySelector('[data-filter-controls]').hidden = false;
    filter.addEventListener('change', applyFilter);
    search?.addEventListener('input', applyFilter);
    search?.addEventListener('keydown', event => {
      if (event.key === 'Enter') {
        event.preventDefault(); // This field filters locally; finishing remains an explicit action.
        applyFilter();
      }
    });
    root.querySelector('[data-clear-filter]').addEventListener('click', () => { filter.value = 'all'; if (search) search.value = ''; applyFilter(); (taxonomy ? worlds.find(button => button.dataset.nodeKey === family) || worlds[0] : filter)?.focus(); });
    form.addEventListener('change', event => { if (event.target.matches('input[name="interestIds"]')) updateSelection(); });
    updateSelection(); applyFilter();
  };
  document.querySelectorAll('[data-interest-editor]').forEach(init);
  document.addEventListener('ping:interest-editor-ready', event => init(event.detail.form));
})();
