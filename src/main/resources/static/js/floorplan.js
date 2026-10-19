(() => {
  const upload = document.querySelector('#floorplan-image');
  upload?.addEventListener('change', () => upload.setCustomValidity(upload.files[0]?.size > 8 * 1024 * 1024 ? 'Elige una imagen de hasta 8 MiB.' : ''));
  document.querySelectorAll('[data-floorplan]').forEach(root => {
    const viewport = root.querySelector('[data-fp-viewport]');
    const canvas = root.querySelector('[data-fp-canvas]');
    const image = root.querySelector('[data-fp-image]');
    const marker = root.querySelector('[data-fp-marker]');
    const controls = root.querySelector('[data-fp-controls]');
    const imageError = root.querySelector('[data-fp-image-error]');
    const retry = root.querySelector('[data-fp-retry]');
    const help = root.querySelector('[data-fp-status]');
    const imageSource = new URL(image.src);
    let retrying = false, retries = 0;
    const form = root.closest('[data-floorplan-editor]');
    const select = form?.querySelector('[data-fp-point]');
    const fieldX = form?.querySelector('[name="x"]');
    const fieldY = form?.querySelector('[name="y"]');
    const number = value => value === '' || value == null ? null : Number(value);
    let x = number(root.dataset.fpX), y = number(root.dataset.fpY), zoom = 1, fitWidth = 0, fitHeight = 0, lastWidth = 0;
    const valid = () => Number.isFinite(x) && Number.isFinite(y) && x >= 0 && x <= 1 && y >= 0 && y <= 1;
    const updateMarker = () => {
      marker.hidden = !valid();
      if (valid()) {
        marker.style.left = `${x * 100}%`; marker.style.top = `${y * 100}%`;
        marker.dataset.edgeX = x < .08 ? 'start' : x > .92 ? 'end' : 'middle';
        marker.dataset.edgeY = y < .18 ? 'start' : 'middle';
      }
      root.querySelector('[data-fp-center]').disabled = !valid();
    };
    const center = (cx, cy) => viewport.scrollTo({left: cx * fitWidth * zoom - viewport.clientWidth / 2, top: cy * fitHeight * zoom - viewport.clientHeight / 2});
    const layout = () => {
      const ratio = image.naturalHeight / image.naturalWidth;
      fitWidth = Math.min(viewport.clientWidth, 360 / ratio); fitHeight = fitWidth * ratio;
      canvas.style.width = `${fitWidth * zoom}px`; canvas.style.height = `${fitHeight * zoom}px`;
      viewport.style.height = `${fitHeight + 2}px`;
      root.querySelector('[data-fp-scale]').textContent = `${Math.round(zoom * 100)} %`;
      root.querySelector('[data-fp-zoom="out"]').disabled = zoom <= 1;
      root.querySelector('[data-fp-zoom="in"]').disabled = zoom >= 4;
      updateMarker();
    };
    const changeZoom = next => {
      const cx = (viewport.scrollLeft + viewport.clientWidth / 2 - canvas.offsetLeft) / (fitWidth * zoom);
      const cy = (viewport.scrollTop + viewport.clientHeight / 2) / (fitHeight * zoom);
      zoom = Math.max(1, Math.min(4, next)); layout(); center(Math.max(0, Math.min(1, cx)), Math.max(0, Math.min(1, cy)));
    };
    root.querySelector('[data-fp-zoom="in"]').addEventListener('click', () => changeZoom(zoom + .5));
    root.querySelector('[data-fp-zoom="out"]').addEventListener('click', () => changeZoom(zoom - .5));
    root.querySelector('[data-fp-reset]').addEventListener('click', () => { zoom = 1; layout(); viewport.scrollTo(0, 0); });
    root.querySelector('[data-fp-center]').addEventListener('click', () => { if (valid()) center(x, y); });
    const draft = () => { x = number(fieldX.value); y = number(fieldY.value); updateMarker(); form.querySelector('[data-fp-draft-status]').textContent = 'Posición ajustada sin guardar. Pulsa Guardar posición para aplicarla a próximos encuentros.'; };
    if (form) {
      const describe = () => {
        const option = select.selectedOptions[0];
        form.querySelector('[data-fp-description]').textContent = option?.dataset.name ? `${option.dataset.name} · ${option.dataset.active === 'true' ? 'Activo' : 'Inactivo'}. ${option.dataset.directions}` : 'Selecciona un punto existente para colocarlo.';
        marker.setAttribute('aria-label', option?.dataset.name ? `1 · ${option.dataset.name} · Punto seleccionado` : 'Punto seleccionado');
      };
      select.addEventListener('change', () => { const option = select.selectedOptions[0]; fieldX.value = option.dataset.x || ''; fieldY.value = option.dataset.y || ''; describe(); draft(); });
      fieldX.addEventListener('input', draft); fieldY.addEventListener('input', draft);
      describe(); x = number(fieldX.value); y = number(fieldY.value);
    }
    let drag = null, suppressClick = false;
    viewport.addEventListener('pointerdown', event => {
      if (event.pointerType !== 'mouse' || event.button !== 0 || zoom === 1) return;
      drag = {x:event.clientX,y:event.clientY,left:viewport.scrollLeft,top:viewport.scrollTop,moved:false};
    });
    viewport.addEventListener('pointermove', event => {
      if (!drag) return;
      if (Math.hypot(event.clientX - drag.x, event.clientY - drag.y) > 6) {drag.moved = true; viewport.setPointerCapture(event.pointerId);}
      if (drag.moved) {event.preventDefault();viewport.scrollLeft=drag.left-(event.clientX-drag.x);viewport.scrollTop=drag.top-(event.clientY-drag.y);}
    });
    viewport.addEventListener('pointerup', () => {suppressClick=!!drag?.moved;drag=null;});
    viewport.addEventListener('pointercancel', () => {drag=null;});
    canvas.addEventListener('click', event => {
      if (suppressClick) {suppressClick=false;return;}
      if (!form || !select.value) return;
      const box=canvas.getBoundingClientRect();fieldX.value=Math.max(0,Math.min(1,(event.clientX-box.left)/box.width)).toFixed(3);fieldY.value=Math.max(0,Math.min(1,(event.clientY-box.top)/box.height)).toFixed(3);draft();
    });
    let ready = false;
    const loaded = () => {
      if (ready || !image.naturalWidth) return; ready = true;
      viewport.hidden=false; imageError.hidden=true; help.hidden=false; controls.hidden=false; root.removeAttribute('aria-busy'); layout();
      if (retrying) { viewport.focus({preventScroll:true}); retrying=false; }
      new ResizeObserver(() => {if (viewport.clientWidth !== lastWidth) {lastWidth=viewport.clientWidth;layout();if(valid()&&zoom>1)center(x,y);}}).observe(viewport);
    };
    image.addEventListener('load', loaded);
    const failed = () => {
      ready=false; controls.hidden=true; viewport.hidden=true; help.hidden=true; imageError.hidden=false;
      root.removeAttribute('aria-busy'); retry.disabled=false; retry.textContent='Volver a cargar plano'; retrying=false;
    };
    retry.addEventListener('click', () => {
      retrying=true; retry.disabled=true; retry.textContent='Cargando…'; root.setAttribute('aria-busy','true');
      // Retry only this same version. Numeric form drafts and mission snapshots stay intact.
      const source=new URL(imageSource); source.searchParams.set('retry', String(++retries)); image.src=source.href;
    });
    image.addEventListener('error', failed);
    if (image.complete) { if (image.naturalWidth) loaded(); else failed(); }
  });
})();
