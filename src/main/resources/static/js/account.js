document.querySelectorAll('[data-show-password]').forEach(b=>b.addEventListener('click',()=>{const input=document.getElementById(b.dataset.showPassword);const show=input.type==='password';input.type=show?'text':'password';b.textContent=show?'Ocultar':'Mostrar';b.setAttribute('aria-pressed',String(show));}));
// Una sola salida al proveedor por pulsación; al volver con «Atrás» (bfcache) el botón vuelve a estar disponible.
document.querySelectorAll('[data-oauth-start]').forEach(a=>a.addEventListener('click',e=>{if(a.getAttribute('aria-disabled')==='true'){e.preventDefault();return;}a.setAttribute('aria-disabled','true');}));
addEventListener('pageshow',()=>document.querySelectorAll('[data-oauth-start]').forEach(a=>a.removeAttribute('aria-disabled')));
