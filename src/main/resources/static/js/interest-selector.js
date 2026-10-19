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
    }, {passive:true});
    window.addEventListener('pointerup', release, {passive:true});
    window.addEventListener('pointercancel', release, {passive:true});
    window.addEventListener('blur', release);
    window.addEventListener('pagehide', release);
    document.addEventListener('scroll', release, {capture:true,passive:true});
    const taxonomy=root.querySelector('[data-taxonomy]');let currentNode=null;
    const nodes=taxonomy?[...taxonomy.querySelectorAll('[data-node-key]')]:[];
    if(taxonomy){taxonomy.hidden=false;filter.closest('div').hidden=true;nodes.forEach(b=>b.addEventListener('click',()=>{currentNode=b.dataset.nodeKey;filter.value='all';if(search)search.value='';applyFilter();}));taxonomy.querySelector('[data-taxonomy-back]').onclick=()=>{currentNode=nodes.find(b=>b.dataset.nodeKey===currentNode)?.dataset.nodeParent||null;if(search)search.value='';applyFilter();};}
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
        group.querySelector('legend').hidden=!!taxonomy&&filter.value==='all'&&group.dataset.interestGroup!=='retained';
        let matches = 0;
        group.querySelectorAll('[data-interest-name]').forEach(row => {
          const memberships=(row.dataset.interestNodes||'').split('|');
          const descendant=key=>key===currentNode || nodes.find(b=>b.dataset.nodeKey===key)?.dataset.nodeParent===currentNode;
          row.hidden = !normalize(row.dataset.interestName).includes(query) || (!!taxonomy&&!query&&filter.value==='all'&&(!currentNode||!memberships.some(descendant)));

          if (!row.hidden) matches++;
        });
        group.hidden = (filter.value !== 'all' && filter.value !== group.dataset.interestGroup) || matches === 0;
        if (!group.hidden) visible += matches;
      });
      if(taxonomy){nodes.forEach(b=>b.hidden=!!query||(b.dataset.nodeParent||null)!==currentNode);taxonomy.querySelector('[data-taxonomy-heading]').textContent=query?'Resultados':nodes.find(b=>b.dataset.nodeKey===currentNode)?.dataset.nodeLabel||'Explora tus mundos';taxonomy.querySelector('[data-taxonomy-back]').hidden=!currentNode&&!query;}
      root.querySelector('[data-interest-empty]').hidden = visible !== 0 || (!!taxonomy&&!query&&nodes.some(b=>!b.hidden));
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
    root.querySelector('[data-clear-filter]').addEventListener('click', () => { currentNode=null; filter.value = 'all'; if (search) search.value = ''; applyFilter(); filter.focus(); });
    form.addEventListener('change', event => { if (event.target.matches('input[name="interestIds"]')) updateSelection(); });
    updateSelection(); applyFilter();
  };
  document.querySelectorAll('[data-interest-editor]').forEach(init);
  document.addEventListener('ping:interest-editor-ready', event => init(event.detail.form));
})();
