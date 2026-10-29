(() => {
  const editor = document.querySelector('[data-hint-editor]'); if (!editor) return;
  const form = editor.querySelector('form'), input = editor.querySelector('[data-hint-input]');
  const panel = editor.querySelector('[data-hint-edit-panel]');
  // The server-persisted value is distinct from a submitted draft after 400/409.
  const persisted = editor.querySelector('[data-hint-saved-value]').textContent;
  let dirty = false, saving = false;
  const update = () => {
    dirty = input.value !== persisted; const count = Array.from(input.value).length;
    editor.querySelector('[data-hint-count]').textContent = `${count} / 120 caracteres`;
    editor.querySelector('[data-hint-draft]').hidden = !dirty;
    input.setCustomValidity(count > 120 ? 'Usa como máximo 120 caracteres.' : '');
  };
  window.PingHintEditor = {isDirty: () => dirty, isSaving: () => saving};
  input.addEventListener('input', update);
  const suggestions = editor.querySelector('[data-hint-suggestions]'); suggestions.hidden = false;
  suggestions.querySelectorAll('[data-hint-suggestion]').forEach(button => button.addEventListener('click', () => {
    input.value = button.dataset.hintSuggestion; update(); input.focus();
  }));
  form.addEventListener('submit', event => {
    update(); if (!form.checkValidity()) { event.preventDefault(); form.reportValidity(); return; }
    saving = true; // Saved content is rendered only after the server confirms and redirects.
  });
  editor.querySelector('[data-hint-cancel]').addEventListener('click', event => {
    // After a failed POST, return to a fresh GET rather than leaving a resolved
    // error summary or a stale server revision on the recovery page.
    if (editor.dataset.hintError === 'true') return;
    event.preventDefault(); input.value = persisted; saving = false; update();
    editor.querySelector('#recognition-errors').textContent = ''; input.removeAttribute('aria-invalid');
    if (persisted) panel.open = false;
    (persisted ? panel.querySelector('summary') : input).focus();
    document.dispatchEvent(new Event('ping:hint-editor-settled'));
  });
  panel.addEventListener('toggle', () => { if (panel.open) update(); });
  window.addEventListener('pageshow', () => { saving = false; update(); });
  document.addEventListener('ping:form-restored', () => { saving = false; update(); });
  document.querySelector('[data-error-summary]')?.focus(); update();
})();
