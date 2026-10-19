(() => {
  const root = document.querySelector('[data-ping-live]');
  if (!root || !root.dataset.pingLive) return;
  let submitting = false, timer, running = false, stopped = false, controller;
  let pendingUpdate = false, lastRevision = root.dataset.recognitionRevision || '';
  const message = document.querySelector('[data-live-status]');
  const normalMessage = message?.cloneNode(true);
  const schedule = () => {
    clearTimeout(timer);
    if (!stopped && !submitting && document.visibilityState === 'visible') timer = setTimeout(check, 5000);
  };
  const editorBusy = () => window.PingHintEditor?.isDirty() || window.PingHintEditor?.isSaving();
  const notice = (text, url, label) => {
    if (!message) return;
    message.replaceChildren(document.createTextNode(text));
    if (url) { const link = document.createElement('a'); link.href = url; link.textContent = label; message.append(' ', link); }
  };
  const withdrawHint = () => {
    const hint = document.querySelector('[data-other-hint]');
    if (hint) { hint.querySelector('[data-hint-text]')?.replaceChildren(); hint.hidden = true; }
  };
  const sessionLost = () => {
    stopped = true; withdrawHint(); clearTimeout(timer);
    notice('Tu sesión ya no está disponible. Volver a entrar no recupera tu identidad ni tu saldo anterior.', '/join', 'Volver a entrar');
    message?.querySelector('a')?.classList.add('btn', 'btn-primary');
    root.querySelectorAll('form[method="post"] button').forEach(button => { button.disabled = true; });
  };
  const refreshHint = async signal => {
    if (!root.dataset.hintUrl) return;
    const response = await fetch(root.dataset.hintUrl, {credentials:'same-origin', cache:'no-store', signal});
    if (response.status === 401) { sessionLost(); return; }
    if (!response.ok) { withdrawHint(); throw new Error('hint unavailable'); }
    const hint = await response.json();
    if (!hint.active) { withdrawHint(); return; }
    const container = document.querySelector('[data-other-hint]');
    if (container) { container.querySelector('[data-hint-text]').textContent = hint.text; container.hidden = !hint.text; }
    lastRevision = hint.revision;
  };
  const check = async () => {
    if (submitting || running || stopped || document.visibilityState !== 'visible') return;
    running = true; controller = new AbortController();
    // A stalled poll must not hold all future updates indefinitely.
    const timeout = setTimeout(() => controller.abort(), 12000);
    try {
      const response = await fetch(root.dataset.stateUrl, {credentials:'same-origin',cache:'no-store',headers:{Accept:'application/json'},signal:controller.signal});
      if (response.status === 401) { sessionLost(); return; }
      if (!response.ok) throw new Error('state unavailable');
      const state = await response.json(); if (submitting || document.visibilityState !== 'visible') return;
      if (state.matchStatus !== 'ACTIVE') withdrawHint();
      else if (state.recognitionRevision !== lastRevision) await refreshHint(controller.signal);
      if (stopped) return;
      if (state.signature !== root.dataset.pingLive) {
        if (editorBusy()) { pendingUpdate = true; notice('El encuentro cambió. Tu borrador sigue intacto; guarda o cancela para actualizar la pantalla.'); }
        else { location.reload(); return; }
      } else if (normalMessage && message) {
        message.replaceChildren(...[...normalMessage.childNodes].map(node => node.cloneNode(true)));
      }
    } catch (_) {
      if (!submitting && !stopped && document.visibilityState === 'visible') notice('Actualización pendiente. Conservamos el último estado; necesitas conexión para comprobar los cambios.', location.pathname, 'Actualizar estado');
    } finally { clearTimeout(timeout); running = false; schedule(); }
  };
  document.addEventListener('submit', event => {
    if (!event.defaultPrevented) { submitting = true; clearTimeout(timer); controller?.abort(); }
  });
  document.addEventListener('ping:form-restored', () => { submitting = false; check(); });
  document.addEventListener('ping:hint-editor-settled', () => { if (pendingUpdate) location.reload(); else { submitting = false; check(); } });
  window.addEventListener('pagehide', () => { clearTimeout(timer); controller?.abort(); });
  window.addEventListener('pageshow', event => { if (event.persisted) { submitting = false; stopped = false; withdrawHint(); lastRevision = ''; check(); } });
  document.addEventListener('visibilitychange', () => { clearTimeout(timer); if (document.visibilityState === 'visible') check(); });
  window.addEventListener('online', check);
  schedule();
})();
