(() => {
  const root = document.querySelector('[data-ping-live]'); if (!root) return;
  let submitting = false, timer;
  const message = document.querySelector('[data-live-status]');
  const schedule = () => { clearTimeout(timer); timer = setTimeout(check, 5000); };
  const unavailable = () => { if (message) message.textContent = 'No pudimos actualizar ahora. Reintentaremos automáticamente; también puedes actualizar manualmente.'; };
  const check = async () => {
    if (submitting) return;
    if (document.visibilityState !== 'visible') { schedule(); return; }
    try {
      const response = await fetch(root.dataset.stateUrl, {credentials: 'same-origin', cache: 'no-store', headers: {Accept: 'application/json'}});
      if (!response.ok) { unavailable(); schedule(); return; }
      const state = await response.json();
      if (submitting) return;
      if (state.signature !== root.dataset.pingLive) { location.reload(); return; }
      if (message) message.textContent = 'Actualización automática activa; también puedes actualizar manualmente.';
    } catch (_) { unavailable(); }
    schedule();
  };
  document.addEventListener('submit', () => { submitting = true; clearTimeout(timer); });
  window.addEventListener('pagehide', () => clearTimeout(timer));
  window.addEventListener('pageshow', event => {
    if (event.persisted) { submitting = false; clearTimeout(timer); check(); }
  });
  schedule();
})();
