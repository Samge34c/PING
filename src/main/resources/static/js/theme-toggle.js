(() => {
  const apply = theme => {
    theme = theme === 'light' ? 'light' : 'dark';
    document.documentElement.dataset.theme = theme;
    document.documentElement.dataset.bsTheme = theme;
    const color = getComputedStyle(document.documentElement).getPropertyValue('--ping-bg').trim();
    if (color) document.querySelector('meta[name="theme-color"]')?.setAttribute('content', color);
    document.querySelectorAll('[data-theme-option]').forEach(button => {
      button.setAttribute('aria-pressed', String(button.dataset.themeOption === theme));
    });
  };
  apply(document.documentElement.dataset.theme);
  document.querySelectorAll('[data-theme-option]').forEach(button => button.addEventListener('click', () => {
    const theme = button.dataset.themeOption;
    apply(theme);
    try { localStorage.setItem('ping-theme', theme); } catch (_) { /* El selector sigue funcionando durante esta visita. */ }
  }));
  window.addEventListener('storage', event => {
    if (event.key === 'ping-theme' || event.key === null) apply(event.newValue);
  });
})();
