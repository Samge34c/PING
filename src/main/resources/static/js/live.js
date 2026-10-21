(() => {
  const root = document.querySelector('[data-ping-live]');
  if (!root || !root.dataset.pingLive) return;
  let submitting = false, timer, running = false, stopped = false, controller;
  let pendingUpdate = false, lastRevision = root.dataset.recognitionRevision || '';
  const message = document.querySelector('[data-live-status]');
  const normalMessage = message?.cloneNode(true);
  // Presentation only: no field contents, hints or identity are retained here.
  const focusKey = 'ping-live-focus';
  const rememberFocus = nextPhase => {
    const active = document.activeElement;
    if (nextPhase !== root.dataset.livePhase || !active?.id || !root.contains(active)) return;
    const fallback = active.closest('section[aria-labelledby]')?.getAttribute('aria-labelledby');
    try { sessionStorage.setItem(focusKey, JSON.stringify({path:location.pathname, id:active.id, fallback, at:Date.now()})); } catch (_) { }
  };
  window.addEventListener('pageshow', event => {
    if (event.persisted) return; // Keep native Back/Forward focus and scroll.
    try {
      const raw = sessionStorage.getItem(focusKey); sessionStorage.removeItem(focusKey);
      if (!raw || performance.getEntriesByType('navigation')[0]?.type !== 'reload') return;
      const saved = JSON.parse(raw);
      if (saved.path !== location.pathname || Date.now() - saved.at > 15000) return;
      requestAnimationFrame(() => {
        const target = document.getElementById(saved.id) || document.getElementById(saved.fallback);
        if (target && root.contains(target) && !target.disabled) target.focus({preventScroll:true});
      });
    } catch (_) { /* Storage unavailable: native navigation remains usable. */ }
  });
  const schedule = () => {
    clearTimeout(timer);
    if (!stopped && !submitting && document.visibilityState === 'visible') timer = setTimeout(check, 5000);
  };
  const editorBusy = () => window.PingHintEditor?.isDirty() || window.PingHintEditor?.isSaving() || !!root.querySelector('[data-credential-dirty="true"],[data-scanner-running="true"]');
  // Avisos de recuperación: arriba del contenido de la pantalla (no al final de una página larga) y uno solo.
  const bannerId = 'ping-recovery';
  const banner = ({ tone, title, text, action }) => {
    let box = document.getElementById(bannerId);
    if (!box) { box = document.createElement('div'); box.id = bannerId; box.className = 'recovery-banner'; root.prepend(box); }
    box.dataset.tone = tone;
    if (tone === 'error') box.dataset.sticky = ''; else delete box.dataset.sticky;
    box.setAttribute('role', tone === 'error' ? 'alert' : 'status');
    const heading = document.createElement('strong'); heading.textContent = title;
    const body = document.createElement('span'); body.textContent = text;
    box.replaceChildren(heading, body);
    if (action) {
      const control = document.createElement(action.url ? 'a' : 'button');
      control.className = 'btn ' + (tone === 'error' ? 'btn-primary' : 'btn-outline-primary');
      control.textContent = action.label;
      if (action.url) control.href = action.url; else { control.type = 'button'; control.addEventListener('click', action.run); }
      box.append(control);
    }
    // El aviso sustituye al estado del pie para no duplicar el mensaje.
    if (message) message.hidden = true;
    return box;
  };
  const clearBanner = () => { document.getElementById(bannerId)?.remove(); if (message) message.hidden = false; };
  const withdrawHint = () => {
    const hint = document.querySelector('[data-other-hint]');
    if (hint) { hint.querySelector('[data-hint-text]')?.replaceChildren(); hint.hidden = true; }
  };
  const sessionLost = () => {
    stopped = true; withdrawHint(); clearTimeout(timer);
    banner({ tone: 'error', title: 'Tu sesión ya no está disponible',
      text: 'Los botones de esta pantalla están desactivados. Entra a tu cuenta para recuperar tus participaciones.',
      action: { label: 'Entrar a mi cuenta', url: '/login' } });
    document.dispatchEvent(new Event('ping:session-lost'));
    root.querySelectorAll('[data-private-credential]').forEach(el=>{el.removeAttribute('src');el.textContent='';el.hidden=true;});
    // A manually entered or scanned personal code is private too. Keep ordinary drafts
    // during network failures, but withdraw credentials when the session is gone.
    root.querySelectorAll('[data-credential-form]').forEach(form => {
      const input = form.querySelector('[name="credential"]');
      if (input) { input.value = ''; input.defaultValue = ''; }
      form.dataset.credentialDirty = 'false';
    });
    root.querySelectorAll('form[method="post"] button').forEach(button => { button.disabled = true; button.setAttribute('aria-describedby', bannerId); });
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
        if (editorBusy()) { pendingUpdate = true; banner({ tone: 'wait', title: 'El encuentro cambió', text: 'Tu borrador sigue intacto; guarda o cancela para actualizar la pantalla.' }); }
        else { rememberFocus(state.matchStatus); location.reload(); return; }
      } else {
        clearBanner();
        if (normalMessage && message) message.replaceChildren(...[...normalMessage.childNodes].map(node => node.cloneNode(true)));
      }
    } catch (_) {
      if (!submitting && !stopped && document.visibilityState === 'visible') banner({ tone: 'wait', title: 'Sin conexión',
        text: 'Mostramos el último estado y seguimos intentando. Lo que escribiste en esta pantalla se conserva.',
        action: { label: 'Reintentar ahora', run: () => { clearTimeout(timer); check(); } } });
    } finally { clearTimeout(timeout); running = false; schedule(); }
  };
  document.addEventListener('submit', event => {
    if (!event.defaultPrevented) { submitting = true; clearTimeout(timer); controller?.abort(); }
  });
  document.addEventListener('ping:form-restored', () => { submitting = false; check(); });
  const settled=()=>{if(pendingUpdate&&!editorBusy())location.reload();else{submitting=false;check();}};
  document.addEventListener('ping:hint-editor-settled',settled);document.addEventListener('ping:credential-settled',settled);
  window.addEventListener('pagehide', () => { clearTimeout(timer); controller?.abort(); });
  window.addEventListener('pageshow', event => { if (event.persisted) { submitting = false; stopped = false; withdrawHint(); lastRevision = ''; check(); } });
  document.addEventListener('visibilitychange', () => { clearTimeout(timer); if (document.visibilityState === 'visible') check(); });
  window.addEventListener('online', check);
  schedule();
})();
