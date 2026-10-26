(() => {
 if(window.PingScannerReady)return;window.PingScannerReady=true;
 let decoder;
 const loadDecoder=()=>decoder ||=new Promise((resolve,reject)=>{if(window.ZXingBrowser){resolve(window.ZXingBrowser);return;}const script=document.createElement('script');script.src='/vendor/zxing/zxing-browser.min.js';script.integrity='sha384-HRtzk9lZgkbSgvUyQrnfC/GxiXZgwaNyD7hC9wcXlsBpDhkS80ISl73juef2FRuf';script.crossOrigin='anonymous';script.onload=()=>resolve(window.ZXingBrowser);script.onerror=()=>{decoder=null;script.remove();reject(new Error('decoder unavailable'));};document.head.append(script);});
 document.querySelectorAll('[data-credential-form]').forEach(form=>{const input=form.querySelector('[name=credential]');input.addEventListener('input',()=>form.dataset.credentialDirty=String(!!input.value.trim()));form.querySelector('[data-clear-credential]')?.addEventListener('click',()=>{form.reset();form.dataset.credentialDirty='false';document.dispatchEvent(new Event('ping:credential-settled'));input.focus();});});
 document.querySelectorAll('[data-scanner]').forEach(root=>{
  if(root.dataset.ready)return;root.dataset.ready='true';
  const start=root.querySelector('[data-scan-start]'),cancel=root.querySelector('[data-scan-stop]'),video=root.querySelector('video'),status=root.querySelector('[data-scan-status]'),input=document.getElementById(root.dataset.target),camera=root.querySelector('[data-scan-camera]'),cameraLabel=root.querySelector('[data-scan-camera-label]');
  // a) A QR that opens an address, b) camera capture, c) decoding and d) server validation are separate steps:
  // this script only does b) and c) locally and fills the form; the server keeps validating the code.
  const secure=window.isSecureContext,capable=secure&&!!navigator.mediaDevices?.getUserMedia;let sessionGone=false;
  let stream,capture,timer,hint,running=false,generation=0;
  const state=value=>{root.dataset.scanState=value;};
  const stop=(notify=true)=>{generation++;running=false;root.dataset.scannerRunning='false';clearTimeout(timer);clearTimeout(hint);if(root.dataset.scanState!=='read')state('idle');if(capture){capture.width=0;capture.height=0;capture=null;}stream?.getTracks().forEach(t=>t.stop());stream=null;video.srcObject=null;video.hidden=true;cancel.hidden=true;cameraLabel.hidden=true;start.disabled=!capable||sessionGone;if(notify)document.dispatchEvent(new Event('ping:credential-settled'));};
  const parse=text=>{
   if(root.dataset.purpose==='EVENT_JOIN'){
    if(/^[A-Z0-9]{5,32}$/.test(text))return text;
    try{const url=new URL(text);const hosts=new Set([location.host]);if(root.dataset.allowedOrigin)hosts.add(new URL(root.dataset.allowedOrigin).host);/* Same PING host over http or https: only the code is extracted, nothing is opened. */if(!['http:','https:'].includes(url.protocol)||!hosts.has(url.host)||url.username||url.password||url.pathname!==new URL(root.dataset.joinPath,location.origin).pathname)return null;const code=url.searchParams.get('code')||'';return /^[A-Z0-9]{5,32}$/.test(code)?code:null;}catch(_){return null;}
   }
   const prefix='PING:'+root.dataset.purpose+':';return text.startsWith(prefix)&&/^[A-Za-z0-9_-]{43}$/.test(text.slice(prefix.length))?text:null;
  };
  const accept=(text,revision)=>{if(!running||revision!==generation)return;const value=parse(text);if(!value){clearTimeout(hint);state('wrong');status.textContent='Leímos un QR, pero no corresponde a esta acción. Prueba el QR correcto o introduce el código.';return;}input.value=value;input.dispatchEvent(new Event('input',{bubbles:true}));state('read');stop();status.textContent='Código leído. Revisa y pulsa el botón del formulario para continuar.';input.focus();};
  cancel.addEventListener('click',()=>{stop();status.textContent='Cámara detenida. Puedes introducir el código.';});
  document.addEventListener('visibilitychange',()=>{if(document.visibilityState==='hidden'&&running){stop(false);status.textContent='Cámara detenida al salir de la pantalla. Puedes reabrirla.';}});
  window.addEventListener('pagehide',()=>stop(false));document.addEventListener('ping:session-lost',()=>{sessionGone=true;stop(false);start.disabled=true;status.textContent='Sesión finalizada. Entra de nuevo antes de escanear.';});
  if(!capable){start.disabled=true;state('unavailable');status.textContent=secure?'Este navegador no permite usar la cámara en esta página. Introduce el código.':'La cámara necesita abrir PING por HTTPS (la dirección https:// del panel, con el certificado de PING en confianza). Mientras tanto, introduce el código.';return;}
  camera.addEventListener('change',()=>{stop();start.click();});
  start.addEventListener('click',async()=>{
   if(sessionGone||running)return;const revision=++generation;running=true;root.dataset.scannerRunning='true';start.disabled=true;cancel.hidden=false;state('requesting');status.textContent='Solicitando acceso a la cámara…';
   try{
    const next=await navigator.mediaDevices.getUserMedia({video:camera.value?{deviceId:{exact:camera.value}}:{facingMode:{ideal:'environment'}},audio:false});if(!running||revision!==generation){next.getTracks().forEach(t=>t.stop());return;}stream=next;video.srcObject=stream;video.hidden=false;await video.play();state('active');status.textContent='Apunta al QR. No enviamos imágenes ni abrimos enlaces externos.';hint=setTimeout(()=>{if(running&&revision===generation&&root.dataset.scanState==='active')status.textContent='Aún no se lee el QR. Acércalo hasta que ocupe buena parte de la imagen, evita reflejos y mantén el teléfono quieto.';},12000);
    if(navigator.mediaDevices.enumerateDevices){const devices=(await navigator.mediaDevices.enumerateDevices()).filter(d=>d.kind==='videoinput');if(!running||revision!==generation)return;const selected=stream.getVideoTracks()[0]?.getSettings().deviceId;camera.replaceChildren();devices.forEach((d,i)=>{const option=document.createElement('option');option.value=d.deviceId;option.textContent=d.label||'Cámara '+(i+1);option.selected=d.deviceId===selected;camera.append(option);});cameraLabel.hidden=devices.length<2;}
    let native;
    try{if(window.BarcodeDetector&&(await BarcodeDetector.getSupportedFormats()).includes('qr_code'))native=new BarcodeDetector({formats:['qr_code']});}catch(_){}
    if(native){const scan=async()=>{if(!running||revision!==generation)return;try{const found=await native.detect(video);if(found.length)accept(found[0].rawValue,revision);}catch(_){}if(running&&revision===generation)timer=setTimeout(scan,220);};scan();}
    else{
     const lib=await loadDecoder();if(!running||revision!==generation)return;
     const reader=new lib.BrowserQRCodeReader();capture=document.createElement('canvas');let turn=0;
     // Some QR alignment patterns fail in one orientation in the pinned JS detector.
     // Retry one quarter-turn per frame locally; no bitmap URLs, uploads or frame retention.
     const scanLocal=()=>{if(!running||revision!==generation)return;
      try{if(video.videoWidth&&video.videoHeight){const angle=(turn++%4)*Math.PI/2,scale=Math.min(1,960/Math.max(video.videoWidth,video.videoHeight)),w=Math.round(video.videoWidth*scale),h=Math.round(video.videoHeight*scale),quarter=Math.round(angle/(Math.PI/2))%2;
       capture.width=quarter?h:w;capture.height=quarter?w:h;const ctx=capture.getContext('2d',{willReadFrequently:true});ctx.imageSmoothingEnabled=false;ctx.translate(capture.width/2,capture.height/2);ctx.rotate(angle);ctx.drawImage(video,-w/2,-h/2,w,h);accept(reader.decodeFromCanvas(capture).getText(),revision);
      }}catch(_){/* No readable QR in this frame: keep scanning until cancelled. */}
      if(running&&revision===generation)timer=setTimeout(scanLocal,220);
     };scanLocal();
    }
   }catch(error){if(revision!==generation)return;stop();state('error');if(error.name==='OverconstrainedError'){camera.replaceChildren();cameraLabel.hidden=true;}
    const messages={NotAllowedError:'Permiso de cámara denegado. En Safari: menú de la página (aA) → Ajustes del sitio web → Cámara → Permitir; después pulsa «Escanear QR». También puedes introducir el código.',SecurityError:'El navegador bloqueó la cámara en esta página. Abre PING por HTTPS con su certificado en confianza o introduce el código.',NotFoundError:'No encontramos una cámara. Introduce el código.',NotReadableError:'La cámara está ocupada. Cierra la otra aplicación y reintenta.',OverconstrainedError:'La cámara elegida ya no está disponible. Pulsa «Escanear QR» para usar la trasera.',AbortError:'La cámara no pudo iniciarse. Pulsa «Escanear QR» para reintentar o introduce el código.'};
    status.textContent=error.message==='decoder unavailable'?'No se pudo cargar el lector de QR. Recarga la página o introduce el código.':messages[error.name]||'No pudimos abrir el lector. Reintenta o introduce el código manualmente.';}
  });
 });
})();
