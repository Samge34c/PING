(() => {
 if(window.PingScannerReady)return;window.PingScannerReady=true;
 let decoder;
 const loadDecoder=()=>decoder ||=new Promise((resolve,reject)=>{if(window.ZXingBrowser){resolve(window.ZXingBrowser);return;}const script=document.createElement('script');script.src='/vendor/zxing/zxing-browser.min.js';script.integrity='sha384-HRtzk9lZgkbSgvUyQrnfC/GxiXZgwaNyD7hC9wcXlsBpDhkS80ISl73juef2FRuf';script.crossOrigin='anonymous';script.onload=()=>resolve(window.ZXingBrowser);script.onerror=()=>{decoder=null;script.remove();reject(new Error('decoder unavailable'));};document.head.append(script);});
 document.querySelectorAll('[data-credential-form]').forEach(form=>{const input=form.querySelector('[name=credential]');input.addEventListener('input',()=>form.dataset.credentialDirty=String(!!input.value.trim()));form.querySelector('[data-clear-credential]')?.addEventListener('click',()=>{form.reset();form.dataset.credentialDirty='false';document.dispatchEvent(new Event('ping:credential-settled'));input.focus();});});
 // Orientation is only claimed when the browser or the device label says so; otherwise cameras are numbered.
 const BACK=/\b(back|rear|environment|trasera|posterior)\b/i,FRONT=/\b(front|user|frontal|delantera|facetime)\b/i;
 const facingOf=(track,device)=>{const s=track?.getSettings?.()||{};if(s.facingMode==='environment'||s.facingMode==='user')return s.facingMode;const label=device?.label||track?.label||'';return BACK.test(label)?'environment':FRONT.test(label)?'user':null;};
 const NAMES={environment:'Cámara trasera',user:'Cámara frontal'};
 document.querySelectorAll('[data-scanner]').forEach(root=>{
  if(root.dataset.ready)return;root.dataset.ready='true';
  const $=s=>root.querySelector(s);
  const start=$('[data-scan-start]'),stopButton=$('[data-scan-stop]'),switcher=$('[data-scan-switch]'),stage=$('[data-scan-stage]'),controls=$('[data-scan-controls]'),advanced=$('[data-scan-advanced]'),camera=$('[data-scan-camera]'),cameraName=$('[data-scan-camera-name]'),video=$('video'),status=$('[data-scan-status]'),input=document.getElementById(root.dataset.target);
  // a) A QR that opens an address, b) camera capture, c) decoding and d) server validation are separate steps:
  // this script only does b) and c) locally and fills the form; the server keeps validating the code.
  const secure=window.isSecureContext,capable=secure&&!!navigator.mediaDevices?.getUserMedia;let sessionGone=false;
  let stream,capture,timer,hint,running=false,generation=0,devices=[],current={facing:'environment',deviceId:null},orientations=false;
  const state=value=>{root.dataset.scanState=value;};
  const release=()=>{stream?.getTracks().forEach(t=>t.stop());stream=null;video.srcObject=null;};
  const stop=(notify=true)=>{generation++;running=false;root.dataset.scannerRunning='false';clearTimeout(timer);clearTimeout(hint);if(root.dataset.scanState!=='read')state('idle');if(capture){capture.width=0;capture.height=0;capture=null;}release();stage.hidden=true;controls.hidden=true;advanced.hidden=true;start.hidden=false;start.disabled=!capable||sessionGone;if(notify)document.dispatchEvent(new Event('ping:credential-settled'));};
  const parse=text=>{
   if(root.dataset.purpose==='EVENT_JOIN'){
    if(/^[A-Z0-9]{5,32}$/.test(text))return text;
    try{const url=new URL(text);const hosts=new Set([location.host]);if(root.dataset.allowedOrigin)hosts.add(new URL(root.dataset.allowedOrigin).host);/* Same PING host over http or https: only the code is extracted, nothing is opened. */if(!['http:','https:'].includes(url.protocol)||!hosts.has(url.host)||url.username||url.password||url.pathname!==new URL(root.dataset.joinPath,location.origin).pathname)return null;const code=url.searchParams.get('code')||'';return /^[A-Z0-9]{5,32}$/.test(code)?code:null;}catch(_){return null;}
   }
   const prefix='PING:'+root.dataset.purpose+':';return text.startsWith(prefix)&&/^[A-Za-z0-9_-]{43}$/.test(text.slice(prefix.length))?text:null;
  };
  const accept=(text,revision)=>{if(!running||revision!==generation)return;const value=parse(text);if(!value){clearTimeout(hint);state('wrong');status.textContent='Leímos un QR, pero no corresponde a esta acción. Prueba el QR correcto o escribe el código.';return;}input.value=value;input.dispatchEvent(new Event('input',{bubbles:true}));state('read');stop();status.textContent='Código leído. Revisa y pulsa el botón del formulario para continuar.';input.focus();};
  stopButton.addEventListener('click',()=>{stop();status.textContent='Cámara cerrada. Puedes escribir el código.';start.focus();});
  document.addEventListener('visibilitychange',()=>{if(document.visibilityState==='hidden'&&running){stop(false);status.textContent='Cámara cerrada al salir de la pantalla. Puedes volver a abrirla.';}});
  window.addEventListener('pagehide',()=>stop(false));document.addEventListener('ping:session-lost',()=>{sessionGone=true;stop(false);start.disabled=true;status.textContent='Sesión finalizada. Entra de nuevo antes de escanear.';});
  if(!capable){start.disabled=true;state('unavailable');status.textContent=secure?'Este navegador no permite usar la cámara en esta página. Escribe el código.':'La cámara necesita abrir PING por HTTPS. Mientras tanto, escribe el código.';return;}
  // The decoded image is exactly the part of the frame the stage shows (object-fit: cover), so the frame never suggests a crop that is not applied.
  const visibleCrop=()=>{const vw=video.videoWidth,vh=video.videoHeight,sw=stage.clientWidth||1,sh=stage.clientHeight||1,ratio=sw/sh;let w=vw,h=vh;if(vw/vh>ratio)w=Math.round(vh*ratio);else h=Math.round(vw/ratio);return{x:Math.round((vw-w)/2),y:Math.round((vh-h)/2),w,h};};
  const describe=()=>{const track=stream?.getVideoTracks()[0];const device=devices.find(d=>d.deviceId===track?.getSettings?.().deviceId);const facing=facingOf(track,device);current={facing,deviceId:track?.getSettings?.().deviceId||null};const index=devices.findIndex(d=>d.deviceId===current.deviceId);const name=facing?NAMES[facing]:(devices.length>1&&index>=0?'Cámara '+(index+1)+' de '+devices.length:'Cámara');cameraName.textContent=facing||devices.length>1?name:'';video.classList.toggle('qr-mirror',facing==='user');
   // Two orientations are offered only when both can be identified; otherwise «Cambiar cámara» steps through the real devices.
   const known=devices.map(d=>facingOf(null,d));orientations=!!facing&&known.includes('environment')&&known.includes('user')||(!!facing&&devices.length===2&&known.some(Boolean));
   switcher.hidden=devices.length<2;switcher.textContent=orientations?(facing==='user'?'Usar cámara trasera':'Usar cámara frontal'):'Cambiar cámara';
   advanced.hidden=devices.length<3;camera.replaceChildren();devices.forEach((d,i)=>{const option=document.createElement('option');option.value=d.deviceId;option.textContent=(facingOf(null,d)?NAMES[facingOf(null,d)]+' · ':'')+'Cámara '+(i+1);option.selected=d.deviceId===current.deviceId;camera.append(option);});
   return name;};
  const open=async(constraints,label)=>{
   if(sessionGone)return;const revision=++generation;running=true;root.dataset.scannerRunning='true';clearTimeout(timer);clearTimeout(hint);
   // Release the previous stream before asking for another: phones usually allow one open camera.
   release();start.hidden=true;stage.hidden=false;controls.hidden=false;switcher.disabled=true;state('requesting');status.textContent=label||'Solicitando acceso a la cámara…';
   try{
    const next=await navigator.mediaDevices.getUserMedia({video:{...constraints,width:{ideal:1280},height:{ideal:720}},audio:false});if(!running||revision!==generation){next.getTracks().forEach(t=>t.stop());return;}stream=next;video.srcObject=stream;await video.play();if(!running||revision!==generation)return;
    if(navigator.mediaDevices.enumerateDevices){devices=(await navigator.mediaDevices.enumerateDevices()).filter(d=>d.kind==='videoinput'&&d.deviceId);if(!running||revision!==generation)return;}
    const name=describe();switcher.disabled=false;state('active');status.textContent=(name==='Cámara'?'Cámara lista':name)+' · Centra el QR dentro del marco. No guardamos imágenes ni abrimos enlaces.';hint=setTimeout(()=>{if(running&&revision===generation&&root.dataset.scanState==='active')status.textContent='Aún no se lee el QR. Acércalo hasta que llene buena parte del marco, evita reflejos y mantén el teléfono quieto.';},12000);
    let native;
    try{if(window.BarcodeDetector&&(await BarcodeDetector.getSupportedFormats()).includes('qr_code'))native=new BarcodeDetector({formats:['qr_code']});}catch(_){}
    const lib=native?null:await loadDecoder();if(!running||revision!==generation)return;
    const reader=lib?new lib.BrowserQRCodeReader():null;capture=document.createElement('canvas');let turn=0;
    // Retry one quarter-turn per frame for the pinned JS detector; frames never leave this canvas and are overwritten.
    const scan=async()=>{if(!running||revision!==generation)return;
     try{if(video.videoWidth&&video.videoHeight){const crop=visibleCrop(),scale=Math.min(1,960/Math.max(crop.w,crop.h)),w=Math.round(crop.w*scale),h=Math.round(crop.h*scale),angle=native?0:(turn++%4)*Math.PI/2,quarter=Math.round(angle/(Math.PI/2))%2;
      capture.width=quarter?h:w;capture.height=quarter?w:h;const ctx=capture.getContext('2d',{willReadFrequently:true});ctx.imageSmoothingEnabled=false;ctx.translate(capture.width/2,capture.height/2);ctx.rotate(angle);ctx.drawImage(video,crop.x,crop.y,crop.w,crop.h,-w/2,-h/2,w,h);
      if(native){const found=await native.detect(capture);if(found.length)accept(found[0].rawValue,revision);}else accept(reader.decodeFromCanvas(capture).getText(),revision);
     }}catch(_){/* No readable QR in this frame: keep scanning until closed. */}
     if(running&&revision===generation)timer=setTimeout(scan,220);
    };scan();
   }catch(error){if(revision!==generation)return;const tried=constraints.deviceId||constraints.facingMode?.exact;stop();state('error');
    if(error.name==='OverconstrainedError'&&tried){status.textContent='Esa cámara no está disponible. Pulsa «Escanear QR» para usar la predeterminada.';return;}
    const messages={NotAllowedError:'Permiso de cámara denegado. En Safari: menú aA → Ajustes del sitio web → Cámara → Permitir; después pulsa «Escanear QR». También puedes escribir el código.',SecurityError:'El navegador bloqueó la cámara en esta página. Abre PING por HTTPS o escribe el código.',NotFoundError:'No encontramos una cámara en este dispositivo. Escribe el código.',NotReadableError:'La cámara está ocupada por otra aplicación o pestaña. Ciérrala y vuelve a intentarlo.',OverconstrainedError:'La cámara no admite esta configuración. Pulsa «Escanear QR» para reintentar.',AbortError:'La cámara no pudo iniciarse. Pulsa «Escanear QR» para reintentar o escribe el código.'};
    status.textContent=error.message==='decoder unavailable'?'No se pudo cargar el lector de QR. Recarga la página o escribe el código.':messages[error.name]||'No pudimos abrir la cámara. Reintenta o escribe el código.';}
  };
  start.addEventListener('click',()=>{if(sessionGone||running)return;open({facingMode:{ideal:'environment'}});});
  switcher.addEventListener('click',async()=>{if(!running||devices.length<2)return;
   if(orientations&&current.facing){const target=current.facing==='user'?'environment':'user';const device=devices.find(d=>facingOf(null,d)===target)||devices.find(d=>d.deviceId!==current.deviceId);
    await open({deviceId:{exact:device.deviceId}},'Cambiando a la '+NAMES[target].toLowerCase()+'…');return;}
   const index=devices.findIndex(d=>d.deviceId===current.deviceId),next=devices[(index+1)%devices.length];await open({deviceId:{exact:next.deviceId}},'Cambiando de cámara…');});
  camera.addEventListener('change',()=>{if(camera.value&&camera.value!==current.deviceId)open({deviceId:{exact:camera.value}},'Cambiando de cámara…');});
 });
})();
