// Antes del CSS: evita mostrar oscuro brevemente si la preferencia es clara.
(() => {
  let theme = 'dark';
  try { if (localStorage.getItem('ping-theme') === 'light') theme = 'light'; } catch (_) { /* Almacenamiento no disponible: oscuro. */ }
  document.documentElement.dataset.theme = theme;
  document.documentElement.dataset.bsTheme = theme;
})();
