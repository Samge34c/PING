(() => {
  const button = document.querySelector('[data-copy-entry]');
  const input = document.querySelector('#event-entry-link');
  const status = document.querySelector('[data-copy-status]');
  button?.addEventListener('click', async () => {
    try {
      if (navigator.clipboard?.writeText && window.isSecureContext) await navigator.clipboard.writeText(input.value);
      else {
        input.focus(); input.select(); input.setSelectionRange(0, input.value.length);
        if (!document.execCommand('copy')) throw new Error('copy unavailable');
      }
      status.textContent = 'Enlace copiado.'; button.focus();
    } catch (_) {
      input.focus(); input.select(); input.setSelectionRange(0, input.value.length);
      status.textContent = 'El enlace está seleccionado. Usa Copiar en tu navegador.';
    }
  });
})();
