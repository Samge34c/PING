(() => {
  const root = document.querySelector('[data-ping-live]'); if (!root) return;
  let submitting = false;
  document.addEventListener('submit', () => { submitting = true; });
  const check = async () => {
    if (submitting) return;
    if (document.visibilityState !== 'visible') { setTimeout(check, 5000); return; }
    try {
      const response = await fetch(root.dataset.stateUrl, {credentials: 'same-origin', cache: 'no-store', headers: {Accept: 'application/json'}});
      if (!response.ok) return;
      const state = await response.json();
      if (submitting) return;
      if (state.signature !== root.dataset.pingLive) { location.reload(); return; }
    } catch (_) {
      const message = document.querySelector('[data-live-status]');
      if (message) message.textContent = 'No pudimos actualizar ahora. Puedes usar Actualizar estado.';
    }
    setTimeout(check, 5000);
  };
  setTimeout(check, 5000);
})();
