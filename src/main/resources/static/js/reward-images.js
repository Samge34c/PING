// DEC-066: a broken reward image falls back once to the generic PING image (no inline handlers under CSP).
(() => {
 const swap = img => { const fallback = img.dataset.fallback; if (!fallback || img.dataset.fellBack) return; img.dataset.fellBack = 'true'; img.removeAttribute('alt'); img.setAttribute('aria-hidden', 'true'); img.src = fallback; };
 document.querySelectorAll('img[data-fallback]').forEach(img => { img.addEventListener('error', () => swap(img)); if (img.complete && img.naturalWidth === 0) swap(img); });
})();
