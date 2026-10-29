(() => {
 const button=document.querySelector('[data-download-png]'),error=document.querySelector('[data-card-error]');
 if(!button||!window.HTMLCanvasElement)return;
 button.hidden=false;
 button.addEventListener('click',async()=>{
  if(button.disabled)return;button.disabled=true;button.textContent='Generando imagen…';button.setAttribute('aria-busy','true');error.hidden=true;
  let url,downloadUrl;
  try{
   const response=await fetch(button.dataset.cardUrl,{credentials:'same-origin',cache:'no-store'});
   if(!response.ok||!response.headers.get('content-type')?.includes('image/svg+xml'))throw Error('Unavailable');
   const svg=await response.blob();url=URL.createObjectURL(svg);const img=new Image();img.src=url;
   await new Promise((resolve,reject)=>{img.onload=resolve;img.onerror=reject;});
   const canvas=document.createElement('canvas');canvas.width=1080;canvas.height=1350;const context=canvas.getContext('2d');context.drawImage(img,0,0,1080,1350);
   const blob=await new Promise(resolve=>canvas.toBlob(resolve,'image/png'));canvas.width=canvas.height=0;if(!blob)throw Error('Unavailable');
   downloadUrl=URL.createObjectURL(blob);const link=document.createElement('a');link.href=downloadUrl;link.download='mi-experiencia-ping.png';link.click();
   setTimeout(()=>URL.revokeObjectURL(downloadUrl),10000);
  }catch(_){error.hidden=false;}finally{if(url)URL.revokeObjectURL(url);button.disabled=false;button.removeAttribute('aria-busy');button.textContent='Descargar imagen PNG';}
 });
})();
