(() => {
  const normalize = value => value.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLocaleLowerCase('es').trim();
  const init = form => {
    if (!form || form.dataset.categoriesReady) return;
    const root = form.querySelector('[data-interest-selector]');
    if (!root) return;
    form.dataset.categoriesReady = 'true';
    const groups = [...root.querySelectorAll('[data-interest-group]')];
    const filter = root.querySelector('[data-interest-filter]');
    const search = root.querySelector('[data-interest-search]');
    const rows = [...root.querySelectorAll('[data-interest-name]')];
    const updateSelection = () => {
      const selected = rows.filter(row => row.querySelector('input').checked);
      root.querySelector('[data-selection-summary]').textContent = selected.length ? selected.map(row => row.dataset.interestName).join(' · ') : 'Aún ninguno. Puedes continuar sin elegir.';
      const count = form.querySelector('[data-selected-count]');
      if (count) count.textContent = selected.length === 1 ? '1 interés seleccionado' : selected.length + ' intereses seleccionados';
    };
    const applyFilter = () => {
      const query = normalize(search?.value || '');
      let visible = 0;
      groups.forEach(group => {
        let matches = 0;
        group.querySelectorAll('[data-interest-name]').forEach(row => {
          row.hidden = !normalize(row.dataset.interestName).includes(query);
          if (!row.hidden) matches++;
        });
        group.hidden = (filter.value !== 'all' && filter.value !== group.dataset.interestGroup) || matches === 0;
        if (!group.hidden) visible += matches;
      });
      root.querySelector('[data-interest-empty]').hidden = visible !== 0;
      root.querySelector('[data-filter-status]').textContent = query || filter.value !== 'all' ? visible + ' opciones visibles. Tus elecciones se conservan.' : '';
      // Hiding rows never disables, removes or resets their native checkboxes.
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
    root.querySelector('[data-clear-filter]').addEventListener('click', () => { filter.value = 'all'; if (search) search.value = ''; applyFilter(); filter.focus(); });
    form.addEventListener('change', event => { if (event.target.matches('input[name="interestIds"]')) updateSelection(); });
    updateSelection(); applyFilter();
  };
  document.querySelectorAll('[data-interest-editor]').forEach(init);
  document.addEventListener('ping:interest-editor-ready', event => init(event.detail.form));
})();
