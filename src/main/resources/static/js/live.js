(() => {
  const root = document.querySelector('[data-ping-live]'); if (!root) return;
  let submitting = false, timer, pendingUpdate = false, lastRevision = root.dataset.recognitionRevision || '';
  const message = document.querySelector('[data-live-status]');
  const schedule = () => { clearTimeout(timer); timer = setTimeout(check, 5000); };
  const editorBusy = () => window.PingHintEditor?.isDirty() || window.PingHintEditor?.isSaving();
  const withdrawHint = () => {
    const hint = document.querySelector('[data-other-hint]');
    if (hint) { hint.querySelector('[data-hint-text]')?.replaceChildren(); hint.hidden = true; }
  };
  const refreshHint = async () => {
    if (!root.dataset.hintUrl) return;
    const response = await fetch(root.dataset.hintUrl, {credentials:'same-origin', cache:'no-store'});
    if (!response.ok) { withdrawHint(); return; }
    const hint = await response.json();
    if (!hint.active) { withdrawHint(); return; }
    const container = document.querySelector('[data-other-hint]');
    if (container) { container.querySelector('[data-hint-text]').textContent = hint.text; container.hidden = !hint.text; }
    lastRevision = hint.revision;
  };
  const check = async () => {
    if (submitting) return;
    if (document.visibilityState !== 'visible') { schedule(); return; }
    try {
      const response = await fetch(root.dataset.stateUrl, {credentials:'same-origin',cache:'no-store',headers:{Accept:'application/json'}});
      if (!response.ok) { if (message) message.textContent='No pudimos actualizar ahora. Puedes actualizar manualmente; reintentaremos.'; schedule(); return; }
      const state = await response.json(); if (submitting) return;
      if (state.matchStatus !== 'ACTIVE') withdrawHint();
      else if (state.recognitionRevision !== lastRevision) await refreshHint();
      if (state.signature !== root.dataset.pingLive) {
        if (editorBusy()) { pendingUpdate = true; if (message) message.textContent='El encuentro cambió. Tu borrador sigue intacto; guarda o cancela para actualizar la pantalla.'; }
        else { location.reload(); return; }
      }
    } catch (_) { if (message) message.textContent='No pudimos actualizar ahora. Tu borrador sigue disponible.'; }
    schedule();
  };
  document.addEventListener('submit', event => { if (!event.defaultPrevented) { submitting=true;clearTimeout(timer); } });
  document.addEventListener('ping:hint-editor-settled', () => { if (pendingUpdate) location.reload(); else { submitting=false;check(); } });
  window.addEventListener('pagehide', () => clearTimeout(timer));
  window.addEventListener('pageshow', event => { if (event.persisted) { submitting=false;withdrawHint();clearTimeout(timer);lastRevision='';check(); } });
  document.addEventListener('visibilitychange', () => { if (document.visibilityState==='visible') {clearTimeout(timer);check();} });
  schedule();
})();
