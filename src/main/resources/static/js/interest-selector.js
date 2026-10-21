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

    // ───────── Jerarquía de mundos (solo con taxonomía) ─────────
    const taxonomy = root.querySelector('[data-taxonomy]');
    const worlds = taxonomy ? [...taxonomy.querySelectorAll('.ic-world')] : [];
    const subs = taxonomy ? [...taxonomy.querySelectorAll('.ic-sub[data-node-key]')] : [];
    const subAll = taxonomy?.querySelector('[data-sub-all]');
    const subsBox = taxonomy?.querySelector('[data-subs]');
    const nav = taxonomy?.querySelector('[data-taxonomy-nav]');
    const heading = taxonomy?.querySelector('[data-taxonomy-heading]');
    const back = taxonomy?.querySelector('[data-taxonomy-back]');
    const parentOf = key => subs.find(button => button.dataset.nodeKey === key)?.dataset.nodeParent || null;
    const familyOf = row => new Set((row.dataset.interestNodes || '').split('|').filter(Boolean).map(key => parentOf(key) || key));
    const families = new Map(rows.map(row => [row, familyOf(row)]));
    let family = null, sub = null, opener = null;

    const refreshWorlds = () => worlds.forEach(button => {
      const members = rows.filter(row => families.get(row).has(button.dataset.nodeKey));
      const chosen = members.filter(row => checkbox(row).checked).length;
      const selected = button.querySelector('[data-node-selected]');
      const count = button.querySelector('[data-node-count]');
      selected.hidden = !chosen; selected.textContent = chosen;
      count.textContent = chosen ? 'de ' + members.length : plural(members.length, 'opción', 'opciones');
      button.dataset.chosen = chosen ? 'true' : 'false';
      button.setAttribute('aria-label', button.dataset.nodeLabel + ': ' + (chosen ? plural(chosen, 'elegido', 'elegidos') + ' de ' + members.length : plural(members.length, 'opción', 'opciones')));
    });
    if (taxonomy) {
      const base = spriteBase();
      worlds.forEach(button => button.querySelector('[data-world-icon]').setAttribute('href', base + '#' + (worldIcons[button.dataset.nodeKey] || 'heart')));
      back.querySelector('[data-back-icon]').setAttribute('href', base + '#arrow');
      taxonomy.hidden = false;
      filter.closest('div').hidden = true;
      worlds.forEach(button => button.addEventListener('click', () => { opener = button; family = button.dataset.nodeKey; sub = null; if (search) search.value = ''; applyFilter(); heading.focus({ preventScroll: true }); heading.scrollIntoView({ block: 'start' }); }));
      subs.forEach(button => button.addEventListener('click', () => { sub = button.dataset.nodeKey; applyFilter(); }));
      subAll.addEventListener('click', () => { sub = null; applyFilter(); });
      back.addEventListener('click', () => { const target = opener; family = null; sub = null; if (search) search.value = ''; applyFilter(); (target || worlds[0])?.focus({ preventScroll: true }); });
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
      tray.hidden = !selected.length;
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

    const updateSelection = () => {
      const selected = rows.filter(row => checkbox(row).checked);
      root.querySelector('[data-selection-summary]').textContent = selected.length ? selected.slice(0, 3).map(row => row.dataset.interestName).join(' · ') + (selected.length > 3 ? ' · +' + (selected.length - 3) + ' más' : '') : 'Aún ninguno. Puedes continuar sin elegir.';
      const details = root.querySelector('[data-selected-details]');
      if (details) { details.hidden = selected.length <= 3; details.querySelector('[data-selected-list]').textContent = selected.map(row => row.dataset.interestName).join(' · '); }
      const count = form.querySelector('[data-selected-count]');
      if (count) count.textContent = selected.length === 1 ? '1 interés seleccionado' : selected.length + ' intereses seleccionados';
      renderTray(selected);
      if (taxonomy) refreshWorlds();
    };

    // ───────── Filtro y búsqueda (ocultar nunca desmarca ni deshabilita) ─────────
    const applyFilter = () => {
      const query = normalize(search?.value || '');
      const current = sub || family;
      const descendant = key => key === current || parentOf(key) === current;
      const browsing = !!taxonomy && !query && filter.value === 'all';
      let visible = 0;
      groups.forEach(group => {
        group.querySelector('legend').classList.toggle('visually-hidden', browsing && group.dataset.interestGroup !== 'retained');
        let matches = 0;
        group.querySelectorAll('[data-interest-name]').forEach(row => {
          const memberships = (row.dataset.interestNodes || '').split('|');
          const outside = browsing && group.dataset.interestGroup !== 'retained' && (!current || !memberships.some(descendant));
          row.hidden = !normalize(row.dataset.interestName).includes(query) || outside;
          if (!row.hidden) matches++;
        });
        group.hidden = (filter.value !== 'all' && filter.value !== group.dataset.interestGroup) || matches === 0;
        if (!group.hidden) visible += matches;
      });
      if (taxonomy) {
        const atWorlds = !family && !query;
        worlds.forEach(button => { button.hidden = !atWorlds; });
        subsBox.hidden = !family || !!query;
        subs.forEach(button => { button.hidden = button.dataset.nodeParent !== family; button.setAttribute('aria-pressed', String(button.dataset.nodeKey === sub)); });
        subAll.setAttribute('aria-pressed', String(!sub));
        nav.hidden = atWorlds;
        heading.textContent = query ? 'Resultados' : worlds.find(button => button.dataset.nodeKey === family)?.dataset.nodeLabel || 'Explora tus mundos';
        taxonomy.querySelector('.ic-worlds').hidden = !atWorlds;
      }
      root.querySelector('[data-interest-empty]').hidden = visible !== 0 || (!!taxonomy && !query && !family);
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
    root.querySelector('[data-clear-filter]').addEventListener('click', () => { family = null; sub = null; filter.value = 'all'; if (search) search.value = ''; applyFilter(); (taxonomy ? worlds[0] : filter)?.focus(); });
    form.addEventListener('change', event => { if (event.target.matches('input[name="interestIds"]')) updateSelection(); });
    updateSelection(); applyFilter();
  };
  document.querySelectorAll('[data-interest-editor]').forEach(init);
  document.addEventListener('ping:interest-editor-ready', event => init(event.detail.form));
})();
