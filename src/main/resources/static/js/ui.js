(() => {
  const path = location.pathname;
  const section = path === '/' ? 'home' : path.startsWith('/me/benefits') || path.startsWith('/me/claims') ? 'benefits' : path.includes('/interests') ? 'interests' : path.startsWith('/me/connections') ? 'history' : path.startsWith('/me') ? 'me' : path.startsWith('/connect') || path.startsWith('/matches') || path.startsWith('/demo') ? 'connect' : path.startsWith('/events') || path.startsWith('/organizer') ? 'events' : path.startsWith('/participants') ? 'people' : path.startsWith('/admin') ? 'admin' : path.startsWith('/account') ? 'account' : path.startsWith('/login') || path.startsWith('/register') ? 'login' : path.startsWith('/join') ? 'join' : '';
  document.querySelectorAll('[data-nav]').forEach(a => { if (a.dataset.nav === section) a.setAttribute('aria-current', 'page'); });
  document.querySelectorAll('[data-page-title]').forEach(el => { el.textContent = document.title.split(' · ')[0]; });
  document.querySelectorAll('.session-chip').forEach(chip => {
    const id = chip.dataset.sessionId, name = chip.dataset.sessionName;
    let alias = name;
    try { if (id && name) sessionStorage.setItem('ping-alias-' + id, name); else if (id) alias = sessionStorage.getItem('ping-alias-' + id); } catch (_) { }
    if (alias) { chip.querySelector('[data-session-alias]').textContent = alias; chip.querySelector('[data-session-initial]').textContent = alias.slice(0, 1).toUpperCase(); }
  });
  const menu = document.querySelector('#mobile-menu'), toggle = document.querySelector('[data-more-toggle]');
  if (toggle && ['home', 'interests', 'account'].includes(section)) toggle.setAttribute('aria-current', 'page');
  // La navegación inferior puede ocupar varias filas con texto ampliado: se mide para reservar el espacio bajo el contenido.
  const bottomNav = document.querySelector('.ping-bottom-nav');
  if (bottomNav) {
    const reserve = () => document.documentElement.style.setProperty('--ping-nav-h', Math.ceil(bottomNav.getBoundingClientRect().height) + 'px');
    reserve();
    if ('ResizeObserver' in window) new ResizeObserver(reserve).observe(bottomNav); else addEventListener('resize', reserve);
  }
  const background = [...document.querySelectorAll('main,.ping-topbar,.entry-topbar,.ping-sidebar,.ping-bottom-nav,.ping-footer,.h-header,.h-scroll')];
  const closeMenu = () => { if (!menu || menu.hidden) return; menu.hidden = true; background.forEach(el => { el.inert = false; }); toggle.setAttribute('aria-expanded', 'false'); document.body.classList.remove('menu-open'); toggle.focus(); };
  toggle?.addEventListener('click', () => { menu.hidden = false; background.forEach(el => { el.inert = true; }); toggle.setAttribute('aria-expanded', 'true'); document.body.classList.add('menu-open'); menu.querySelector('button').focus(); });
  document.querySelectorAll('[data-more-close]').forEach(el => el.addEventListener('click', closeMenu));
  document.addEventListener('keydown', e => {
    if (!menu || menu.hidden) return;
    if (e.key === 'Escape') closeMenu();
    if (e.key === 'Tab') { const items = [...menu.querySelectorAll('a,button')], first = items[0], last = items.at(-1); if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); } else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); } }
  });
  document.querySelectorAll('[data-dismiss-toast]').forEach(btn => btn.addEventListener('click', () => btn.closest('.ping-toast').remove()));
  document.querySelectorAll('[data-tabs]').forEach(group => {
    const buttons = [...group.querySelectorAll('[role="tab"]')];
    const activate = btn => { buttons.forEach(b => { const selected = b === btn; b.setAttribute('aria-selected', String(selected)); b.tabIndex = selected ? 0 : -1; group.querySelector('#' + b.getAttribute('aria-controls')).hidden = !selected; }); };
    buttons.forEach((btn, i) => { btn.addEventListener('click', () => activate(btn)); btn.addEventListener('keydown', e => { if (e.key === 'ArrowRight' || e.key === 'ArrowLeft') { e.preventDefault(); const next = buttons[(i + (e.key === 'ArrowRight' ? 1 : buttons.length - 1)) % buttons.length]; activate(next); next.focus(); } }); });
    if (buttons.length) activate(buttons[0]);
  });
  const interestData = { Videojuegos: ['game', 'Juega, comparte, conecta.'], Música: ['music', 'Tu próxima canción favorita.'], Cine: ['film', 'Historias que nos encuentran.'], Tecnología: ['code', 'Ideas para lo que viene.'], Viajes: ['plane', 'Lugares que merecen contarse.'], Fotografía: ['camera', 'Otra forma de ver el mundo.'], Anime: ['spark', 'Universos para compartir.'], Deportes: ['ball', 'La energía de estar juntos.'] };
  const decorateInterests = root => root.querySelectorAll('[data-interest-name]').forEach(tile => {
    const [icon, description] = interestData[tile.dataset.interestName] || ['heart', 'Un tema para conectar.'];
    const use = tile.querySelector('.interest-icon use'); if (use) use.setAttribute('href', use.getAttribute('href').split('#')[0] + '#' + icon);
    const label = tile.querySelector('[data-interest-description]'); if (label) label.textContent = description;
  });
  decorateInterests(document);
  // El selector por categorías (interest-selector.js) escribe su propio resumen; este contador queda para formularios sin él.
  const updateInterestCount = form => { if (!form || form.dataset.categoriesReady) return; const count = form.querySelectorAll('input[type="checkbox"]:checked').length; const label = form.querySelector('[data-selected-count]'); if (label) label.textContent = count === 1 ? '1 interés seleccionado' : count + ' intereses seleccionados'; };
  document.querySelectorAll('[data-interest-editor]').forEach(updateInterestCount);
  document.addEventListener('change', e => { const form = e.target.closest('[data-interest-editor]'); if (form) updateInterestCount(form); });
  const inlineEditor = document.querySelector('[data-interest-selection-url]');
  if (inlineEditor) fetch(inlineEditor.dataset.interestSelectionUrl, { credentials: 'same-origin' }).then(r => { if (!r.ok) throw new Error(); return r.text(); }).then(html => {
    const form = new DOMParser().parseFromString(html, 'text/html').querySelector('[data-interest-editor]');
    if (form) { inlineEditor.replaceChildren(document.importNode(form, true)); decorateInterests(inlineEditor); updateInterestCount(inlineEditor.querySelector('form')); document.dispatchEvent(new CustomEvent('ping:interest-editor-ready', {detail:{form:inlineEditor.querySelector('form')}})); }
  }).catch(() => { /* El enlace al editor permanece disponible. */ });
  document.querySelectorAll('[data-demo-person]').forEach(button => button.addEventListener('click', () => { const select = document.querySelector('#participantId'); select.value = button.dataset.demoPerson; document.querySelectorAll('[data-demo-person]').forEach(b => b.setAttribute('aria-pressed', String(b === button))); select.focus(); }));
  const pending = new Map();
  const restore = form => {
    clearTimeout(pending.get(form)); pending.delete(form);
    form.classList.remove('is-submitting'); delete form.dataset.submitting; form.removeAttribute('aria-busy');
    form.querySelectorAll('[data-submit-lock]').forEach(btn => { btn.removeAttribute('aria-disabled'); delete btn.dataset.submitLock; });
    form.querySelector('[data-submit-status]')?.remove();
  };
  const feedback = (form, text, focus = false, anchor = null) => {
    let status = form.querySelector('[data-submit-status]');
    if (!status) {
      status = document.createElement('p'); status.dataset.submitStatus = ''; status.className = 'submit-status'; status.setAttribute('role','status'); status.tabIndex = -1;
      // Junto al botón pulsado cuando es hijo directo del formulario; si no, al final, para no romper filas de botones.
      if (anchor && anchor.parentElement === form) anchor.after(status); else form.append(status);
    }
    status.textContent = text;
    if (focus) status.focus();
    return status;
  };
  document.addEventListener('submit', e => {
    if (e.defaultPrevented || !(e.target instanceof HTMLFormElement) || e.target.method.toLowerCase() !== 'post') return;
    const form = e.target;
    if (form.dataset.submitting) { e.preventDefault(); return; }
    if (!navigator.onLine) {
      e.preventDefault();
      feedback(form, 'Necesitas conexión para enviar. Tus datos siguen en este formulario; vuelve a intentarlo cuando tengas red.', true, e.submitter);
      document.dispatchEvent(new Event('ping:form-restored')); return;
    }
    form.dataset.submitting = 'true'; form.classList.add('is-submitting'); form.setAttribute('aria-busy','true');
    // Keep original button labels and name/value in native POSTs (feedback/delivery).
    form.querySelectorAll('button:not([type="button"]):not([type="reset"]),input[type="submit"]').forEach(btn => { btn.dataset.submitLock = ''; btn.setAttribute('aria-disabled','true'); });
    feedback(form, 'Enviando…', false, e.submitter);
    pending.set(form, setTimeout(() => {
      const status = feedback(form, 'Está tardando. Puede haberse guardado: comprueba el estado antes de repetir la acción.');
      const link = document.createElement('a');
      link.href = location.pathname.startsWith('/join') ? location.pathname : location.pathname.startsWith('/matches/') ? location.pathname : location.pathname.startsWith('/me/benefits') ? '/me/benefits' : '/connect';
      link.textContent = 'Comprobar estado'; status.append(' ',link);
    }, 15000));
    queueMicrotask(() => { if (e.defaultPrevented) { restore(form); document.dispatchEvent(new Event('ping:form-restored')); } });
  });
  window.addEventListener('pagehide', () => { pending.forEach(clearTimeout); });
  window.addEventListener('pageshow', () => {
    document.querySelectorAll('form.is-submitting').forEach(restore);
    document.querySelector('[data-error-summary]')?.focus();
    document.querySelector('[aria-invalid="true"]')?.focus();
  });
  const metricsUrl = document.querySelector('[data-event-metrics-url]');
  if (metricsUrl) fetch(metricsUrl.dataset.eventMetricsUrl, {credentials:'same-origin'}).then(r => { if (!r.ok) throw new Error(); return r.text(); }).then(html => {
    const metrics = new DOMParser().parseFromString(html, 'text/html');
    document.querySelectorAll('[data-event-people]').forEach(el => { const count = metrics.querySelector('[data-event-id="' + el.dataset.eventPeople + '"] [data-metric="participants"]'); if (count) el.textContent = count.textContent.trim() + ' participantes'; });
  }).catch(() => { /* Se conserva el acceso a participantes sin inventar cifras. */ });
})();
document.querySelectorAll('[data-admin-time]').forEach(el => { el.textContent = new Date().toLocaleTimeString('es-CO', {hour:'2-digit',minute:'2-digit'}); });
