(() => {
  const editor = document.querySelector('[data-hint-editor]');
  if (!editor) return;
  const form = editor.querySelector('form'), input = editor.querySelector('[data-hint-input]');
  const invitation = editor.querySelector('[data-hint-invite]');
  try { if (sessionStorage.getItem('ping-hint-invite-dismissed')) invitation?.remove(); } catch (_) { }
  editor.querySelector('[data-hint-invite-dismiss]')?.addEventListener('click', () => {
    invitation?.remove(); try { sessionStorage.setItem('ping-hint-invite-dismissed', 'true'); } catch (_) { }
  });
  let initial = input.value, dirty = false, saving = false;
  const count = () => Array.from(input.value).length;
  const update = () => {
    dirty = input.value !== initial;
    editor.querySelector('[data-hint-count]').textContent = `${count()} / 120 caracteres`;
    editor.querySelector('[data-hint-draft]').hidden = !dirty;
    input.setCustomValidity(count() > 120 ? 'Usa como máximo 120 caracteres.' : '');
  };
  window.PingHintEditor = {isDirty: () => dirty, isSaving: () => saving};
  input.addEventListener('input', update);
  editor.querySelectorAll('[data-hint-suggestion]').forEach(button => button.addEventListener('click', () => {
    input.value = button.dataset.hintSuggestion; update(); input.focus();
  }));
  form.addEventListener('submit', event => {
    update();
    if (!form.checkValidity()) { event.preventDefault(); form.reportValidity(); return; }
    saving = true;
  });
  form.addEventListener('reset', () => {
    saving = false;
    queueMicrotask(() => { input.value = initial; update(); editor.open = false; document.dispatchEvent(new Event('ping:hint-editor-settled')); });
  });
  window.addEventListener('pageshow', () => { saving = false; update(); });
  editor.addEventListener('toggle', () => { if (editor.open) update(); });
  document.querySelector('[data-error-summary]')?.focus();
  update();
})();
