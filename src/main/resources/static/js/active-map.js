// Only the inline encounter map. Links keep the real map route and match return.
(() => {
  const root = document.querySelector('[data-encounter-map]');
  if (!root) return;
  const pan = root.querySelector('.am-map-pan'), canvas = root.querySelector('.am-map-canvas');
  const assigned = root.querySelector('[data-assigned="true"]');
  const controls = [...root.querySelectorAll('[data-encounter-zoom]')];
  const reduced = matchMedia('(prefers-reduced-motion: reduce)');
  let zoom = 1;
  const center = (animate = false) => {
    if (assigned) pan.scrollTo({left: Math.max(0, assigned.offsetLeft - pan.clientWidth / 2), top: Math.max(0, assigned.offsetTop - pan.clientHeight / 2), behavior: animate && !reduced.matches ? 'smooth' : 'instant'});
  };
  const resize = () => { canvas.style.width = `${Math.max(pan.clientWidth, 360) * zoom}px`; center(); };
  controls.forEach(button => button.addEventListener('click', () => {
    zoom = Math.max(1, Math.min(2, zoom + (button.dataset.encounterZoom === 'in' ? .25 : -.25)));
    controls.forEach(control => { control.disabled = control.dataset.encounterZoom === 'out' ? zoom === 1 : zoom === 2; });
    resize();
  }));
  root.querySelector('[data-encounter-center]').addEventListener('click', () => center(true));
  window.addEventListener('resize', resize);
  requestAnimationFrame(resize);
})();
