(() => {
  const luminance = color => {
    // Canvas converts color-mix / modern CSS syntax to RGB without approximating it.
    const canvas = document.createElement('canvas'); canvas.width = canvas.height = 1;
    const context = canvas.getContext('2d'); context.fillStyle = color; context.fillRect(0, 0, 1, 1);
    const rgb = Array.from(context.getImageData(0, 0, 1, 1).data).slice(0, 3).map(v => {
      const s = v / 255; return s <= .04045 ? s / 12.92 : ((s + .055) / 1.055) ** 2.4;
    });
    return rgb[0] * .2126 + rgb[1] * .7152 + rgb[2] * .0722;
  };
  const ratio = (a, b) => { const x = luminance(a), y = luminance(b); return (Math.max(x, y) + .05) / (Math.min(x, y) + .05); };
  document.querySelectorAll('[data-ping-theme]').forEach(preview => {
    const style = getComputedStyle(preview), button = getComputedStyle(preview.querySelector('.btn-primary'));
    const muted = getComputedStyle(preview.querySelector('.text-secondary')), input = getComputedStyle(preview.querySelector('input'));
    const values = [ratio(style.color, style.backgroundColor), ratio(muted.color, style.backgroundColor), ratio(button.color, button.backgroundColor), ratio(input.borderTopColor, input.backgroundColor)];
    const valid = values.slice(0, 3).every(v => v >= 4.5) && values[3] >= 3;
    const output = preview.querySelector('[data-contrast]'); output.dataset.valid = String(valid);
    output.textContent = `Contraste texto ${values[0].toFixed(1)}:1 · secundario ${values[1].toFixed(1)}:1 · botón ${values[2].toFixed(1)}:1 · borde ${values[3].toFixed(1)}:1. ${valid ? 'Cumple mínimos de referencia.' : 'Requiere ajustar contraste.'}`;
  });
})();
