(() => {
  if (!window.HTMLDialogElement || !HTMLDialogElement.prototype.showModal) return;
  let sheet, opener;
  const close = () => sheet?.close();
  const show = content => {
    sheet?.remove(); sheet = document.createElement('dialog'); sheet.className = 'help-sheet';
    const handle = document.createElement('div'); handle.className = 'help-handle'; handle.setAttribute('aria-hidden','true');
    const body = document.createElement('div'); body.className = 'help-body'; body.append(content);
    const title = body.querySelector('h1,h2'); if (title) {title.id='context-help-title';title.tabIndex=-1;title.autofocus=true;sheet.setAttribute('aria-labelledby',title.id);}
    const button=document.createElement('button');button.type='button';button.className='btn btn-outline-primary';button.textContent='Entendido';button.addEventListener('click',close);
    sheet.append(handle,body,button);document.body.append(sheet);
    sheet.addEventListener('close',()=>{opener?.focus();sheet.remove();});
    sheet.addEventListener('click',event=>{if(event.target===sheet){const r=sheet.getBoundingClientRect();if(event.clientX<r.left||event.clientX>r.right||event.clientY<r.top||event.clientY>r.bottom)close();}});
    sheet.showModal();(title || button).focus({preventScroll:true});sheet.scrollTop=0;
  };
  document.querySelectorAll('[data-help-details]').forEach(details=>{
    const summary=details.querySelector('summary');summary.addEventListener('click',event=>{
      event.preventDefault();opener=summary;show(details.querySelector('[data-help-content]').cloneNode(true));
    });
  });
  document.querySelectorAll('a[data-event-help]').forEach(link=>link.addEventListener('click',async event=>{
    event.preventDefault();opener=link;link.setAttribute('aria-busy','true');
    try{
      const response=await fetch(link.href,{credentials:'same-origin',cache:'no-store'});
      if(!response.ok||new URL(response.url).pathname!=='/me/help')throw Error('Unavailable');
      const doc=new DOMParser().parseFromString(await response.text(),'text/html');const main=doc.querySelector('main.event-extras');if(!main)throw Error('Unavailable');
      const content=document.createElement('div');main.querySelectorAll('h1,section').forEach(node=>content.append(node.cloneNode(true)));show(content);
    }catch(_){location.assign(link.href);}finally{link.removeAttribute('aria-busy');}
  }));
  window.addEventListener('pagehide',()=>sheet?.remove());
})();
