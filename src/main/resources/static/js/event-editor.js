(() => {
 document.querySelectorAll('.editor-sections section>details').forEach(d=>{const f=d.querySelector('form'),s=d.querySelector('summary');if(f){f.dataset.originalAction=f.action;s.dataset.originalText=s.textContent;s.dataset.originalHidden=String(s.hidden);}});
 document.querySelectorAll('[data-edit-point],[data-edit-activity]').forEach(link=>link.addEventListener('click',event=>{
 if(event.button!==0||event.ctrlKey||event.metaKey||event.shiftKey)return;
 const section=link.closest('section'),details=section.querySelector(':scope>details'),form=details?.querySelector('form');if(!form)return;
 event.preventDefault();form.reset();form.action=link.dataset.saveUrl;
 for(const name of ['name','directions','title','instructions','rewardPoints','version']){const input=form.elements.namedItem(name);if(input&&link.dataset[name]!=null)input.value=link.dataset[name];}
 form.querySelector('input[name="active"]').checked=link.dataset.active==='true';
 for(const name of ['pointIds','interestIds']){const selected=(link.dataset[name]||'').split(',');form.querySelectorAll(`input[name="${name}"]`).forEach(i=>i.checked=selected.includes(i.value));}
 details.querySelector('summary').hidden=false;details.open=true;form.querySelector('input:not([type=hidden])').focus();
 }));
 document.querySelectorAll('[data-cancel-section]').forEach(b=>b.addEventListener('click',()=>{const d=b.closest('details'),s=d.querySelector('summary');b.form.reset();if(b.form.dataset.originalAction)b.form.action=b.form.dataset.originalAction;if(s.dataset.originalText)s.textContent=s.dataset.originalText;s.hidden=s.dataset.originalHidden==='true';d.open=false;if(!s.hidden)s.focus();}));
 document.querySelectorAll('[data-words-editor]').forEach(form=>{const input=form.querySelector('textarea'),chips=form.querySelector('[data-word-chips]');
 const words=()=>{const unique=new Map();for(const s of input.value.split(/[,\n]/).map(s=>s.trim().replace(/\s+/g,' ')).filter(Boolean)){const key=s.normalize('NFD').replace(/\p{Diacritic}/gu,'').toLowerCase();if(!unique.has(key))unique.set(key,s);}return [...unique.values()];};
 const render=()=>{chips.replaceChildren();words().forEach((w,i)=>{const b=document.createElement('button');b.type='button';b.className='btn btn-outline-secondary';b.textContent=w+' ×';b.setAttribute('aria-label','Quitar '+w);b.onclick=()=>{const next=words();next.splice(i,1);input.value=next.join('\n');render();form.querySelector('details').open=true;};chips.append(b);});};
 input.addEventListener('input',render);form.addEventListener('reset',()=>queueMicrotask(render));form.addEventListener('submit',()=>input.value=words().join('\n'));render();
 });
})();
