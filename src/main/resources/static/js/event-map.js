(() => {
  const root = document.querySelector('[data-event-map]');
  if (!root) return;
  const panels = [...root.querySelectorAll('[data-zone-panel]')];
  const links = [...root.querySelectorAll('[data-zone-link]')];
  const pan = root.querySelector('.map-pan');
  const canvas = root.querySelector('.map-canvas');
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)');
  function select(id, focus = false) {
    const panel = panels.find(p => p.dataset.zonePanel === id);
    if (!panel) return;
    const marker = panel.dataset.pavilion;
    panels.forEach(p => { p.hidden = p !== panel; });
    links.forEach(a => {
      const selected = a.classList.contains('map-hotspot') ? a.dataset.pavilion === marker : a.dataset.zoneLink === id;
      a.classList.toggle('is-selected', selected);
      if (selected) a.setAttribute('aria-current', 'location'); else a.removeAttribute('aria-current');
    });
    const hotspot = links.find(a => a.classList.contains('map-hotspot') && a.dataset.pavilion === marker);
    if (hotspot) pan.scrollTo({left: Math.max(0, hotspot.offsetLeft - pan.clientWidth / 2), top: Math.max(0, hotspot.offsetTop - pan.clientHeight / 2), behavior: focus && !reduced.matches ? 'smooth' : 'instant'});
    if (focus) panel.focus({preventScroll: true});
  }
  links.forEach(a => a.addEventListener('click', e => {
    if (e.button !== 0 || e.ctrlKey || e.metaKey || e.shiftKey || e.altKey) return;
    e.preventDefault();
    const url = new URL(a.href);
    const id = url.searchParams.get('zone');
    select(id, true); history.pushState(null, '', url.pathname + url.search);
  }));
  window.addEventListener('popstate', () => select(new URL(location.href).searchParams.get('zone') || panels[0].dataset.zonePanel));
  let zoom = 1;
  window.addEventListener('resize', () => {
    canvas.style.width = `${Math.max(pan.clientWidth, 520) * zoom}px`;
    select(new URL(location.href).searchParams.get('zone') || panels[0].dataset.zonePanel);
  });
  const controls = [...root.querySelectorAll('[data-map-zoom]')];
  controls.forEach(button => button.addEventListener('click', () => {
    zoom = Math.max(1, Math.min(2, zoom + (button.dataset.mapZoom === 'in' ? .25 : -.25)));
    canvas.style.width = `${Math.max(pan.clientWidth, 520) * zoom}px`;
    controls.forEach(b => { b.disabled = b.dataset.mapZoom === 'in' ? zoom === 2 : zoom === 1; });
    select(new URL(location.href).searchParams.get('zone') || panels[0].dataset.zonePanel);
  }));
  requestAnimationFrame(() => select(new URL(location.href).searchParams.get('zone') || panels[0].dataset.zonePanel));
})();
