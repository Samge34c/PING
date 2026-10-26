// DEC-057: presentation only. The phase comes from the server (data-live-phase); no polling or timers here.
(() => {
  const root = document.querySelector('[data-live-phase]');
  if (!root) return;
  // Missing artwork: keep the reserved circle and show the alias initial instead of a broken image.
  document.querySelectorAll('.pair-avatar img').forEach(img => {
    const fail = () => img.closest('.pair-avatar')?.classList.add('is-fallback');
    if (img.complete && img.naturalWidth === 0) fail(); else img.addEventListener('error', fail, { once: true });
  });
  const id = location.pathname.split('/')[2];
  const phase = root.dataset.livePhase;
  let previous = null;
  try { const key = 'ping-pair-phase-' + id; previous = sessionStorage.getItem(key); sessionStorage.setItem(key, phase); } catch (_) { return; }
  // The wave marks a transition this tab actually observed: OFFERED here, ACTIVE (both accepted) now. Reloads show the stable ring.
  if (phase !== 'ACTIVE' || previous !== 'OFFERED' || matchMedia('(prefers-reduced-motion: reduce)').matches) return;
  const host = document.querySelector('[data-connected-avatar]');
  if (!host) return;
  host.addEventListener('animationend', () => host.classList.remove('is-celebrating'), { once: true });
  host.classList.add('is-celebrating');
})();
