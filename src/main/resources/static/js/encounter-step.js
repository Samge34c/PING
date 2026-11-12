// DEC-070: views of the Verify card (scan or type their code) without leaving the step. Presentation only: which
// card and which actions exist is decided by the server; this script never enables an action the page did not render.
(() => {
  const card = document.getElementById('am-step');
  if (!card) return;
  const choose = card.querySelector('[data-verify-choose]'), theirs = card.querySelector('[data-verify-panel="theirs"]');
  const mine = card.querySelector('[data-verify-panel="mine"]');
  const input = theirs?.querySelector('[name="credential"]');
  // A redirect after an action lands on #am-step: move focus to the new card once, then drop the hash so later
  // live refreshes of the same address do not steal focus again.
  if (location.hash === '#am-step') {
    requestAnimationFrame(() => {
      const panelTitle = card.querySelector('[data-verify-panel]:not([hidden]) h3');
      (panelTitle || document.getElementById('am-step-title'))?.focus({preventScroll: true});
      card.scrollIntoView({block: 'start'});
      try { history.replaceState(history.state, '', location.pathname + location.search); } catch (_) { }
    });
  }
  if (!theirs) return;
  card.querySelectorAll('[data-verify-open]').forEach(link => link.addEventListener('click', event => {
    event.preventDefault();
    if (choose) choose.hidden = true;
    if (mine) mine.hidden = true;
    theirs.hidden = false; card.dataset.view = 'theirs';
    const start = theirs.querySelector('[data-scan-start]');
    if (link.dataset.verifyOpen === 'scan' && start && !start.disabled) { start.click(); document.getElementById('am-theirs-title')?.focus({preventScroll: true}); }
    else input?.focus();
    theirs.scrollIntoView({block: 'nearest'});
  }));
  theirs.querySelector('[data-verify-close]')?.addEventListener('click', event => {
    event.preventDefault();
    if (theirs.querySelector('[data-scanner-running="true"]')) theirs.querySelector('[data-scan-stop]')?.click();
    // A typed code is private and belongs to this view only.
    const form = theirs.querySelector('[data-credential-form]');
    if (form) { form.reset(); form.dataset.credentialDirty = 'false'; }
    theirs.hidden = true; delete card.dataset.view;
    if (choose) { choose.hidden = false; choose.querySelector('a,button')?.focus(); }
    document.dispatchEvent(new Event('ping:credential-settled'));
  });
})();
