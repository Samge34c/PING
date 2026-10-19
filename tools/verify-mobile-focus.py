import asyncio,json,argparse
from urllib.parse import urlparse
from pathlib import Path
from playwright.async_api import async_playwright
parser=argparse.ArgumentParser(description='Checks mobile controls/focus using a new fictitious event on a local exclusive test app. Requires existing Python Playwright and Chromium; installs nothing.')
parser.add_argument('--base-url',default='http://127.0.0.1:8089');parser.add_argument('--output',required=True);parser.add_argument('--create-fictitious-event',action='store_true',required=True)
args=parser.parse_args();BASE=args.base_url.rstrip('/');OUT=Path(args.output).resolve()
if urlparse(BASE).hostname not in ('127.0.0.1','localhost','::1'):parser.error('Use a local exclusive test app only.')
repo=Path(__file__).resolve().parents[1]
if OUT.is_relative_to(repo):parser.error('Private cookies/output must be outside the repository.')
OUT.mkdir(parents=True,exist_ok=True)
async def checkpoint(cs,state):
 for i,c in enumerate(cs):await c.storage_state(path=str(OUT/f'session-{i}.json'))
 (OUT/'state.json').write_text(json.dumps(state))
async def main():
 async with async_playwright() as p:
  browser=await p.chromium.launch(executable_path='/usr/bin/chromium',args=['--no-sandbox']);admin=await browser.new_page(viewport={'width':1440,'height':1000});await admin.goto(BASE+'/events/new');await admin.locator('#name').fill('Evento ficticio · Interacción móvil');await admin.get_by_role('button',name='Guardar evento',exact=True).click();await admin.wait_for_url('**/events/*');event=admin.url.replace(BASE,'');code=await admin.locator('code').inner_text();await admin.get_by_role('button',name='Abrir evento',exact=True).click()
  await admin.goto(BASE+event+'/encounters/points/new');await admin.locator('#name').fill('Mesa de bienvenida');await admin.locator('#directions').fill('Junto al letrero amarillo de entrada del evento ficticio.');await admin.get_by_role('button',name='Guardar punto',exact=True).click()
  await admin.goto(BASE+event+'/encounters/activities/new');await admin.locator('#title').fill('Una recomendación compartida');await admin.locator('#instructions').fill('Compartan una canción y expliquen qué les gusta de ella.');await admin.locator('[name=pointIds]').first.check();await admin.locator('#rewardPoints').fill('37');await admin.get_by_role('button',name='Guardar actividad',exact=True).click()
  contexts=[await browser.new_context(viewport={'width':390,'height':844},is_mobile=True,has_touch=True) for _ in range(2)];a,b=[await c.new_page() for c in contexts];state={'event':event,'code':code};evidence={}
  for page,name in [(a,'Lina del ensayo móvil'),(b,'Nico del ensayo móvil')]:
   await page.goto(BASE+'/join?code='+code);await page.wait_for_load_state('load')
   if page==a:
    evidence['aliasFontPx']=await page.locator('#alias').evaluate('(e)=>getComputedStyle(e).fontSize');button=page.get_by_role('button',name='Elegir intereses',exact=True);await button.tap();await page.wait_for_timeout(180);evidence['afterInvalidTapTransform']=await button.evaluate('(e)=>getComputedStyle(e).transform');await page.screenshot(path=str(OUT/'after-alias-390.png'))
   await page.locator('#alias').fill(name);await page.get_by_role('button',name='Elegir intereses').click();await page.wait_for_load_state('load')
   if page==a:
    evidence['categoryFontPx']=await page.locator('[data-interest-filter]').evaluate('(e)=>getComputedStyle(e).fontSize');await page.screenshot(path=str(OUT/'after-interests-390.png'))
   await page.get_by_text('Música',exact=True).click();await page.get_by_role('button',name='Ir a Mi Ping').click();await page.wait_for_url('**/me');await page.goto(BASE+'/connect');await page.get_by_role('button',name='Activar disponibilidad').click()
  await a.get_by_role('button',name='Buscar conexión',exact=True).click();await a.wait_for_url('**/matches/*');match=a.url.replace(BASE,'');state['match']=match;await b.goto(BASE+'/connect');await a.get_by_role('button',name='Aceptar conexión',exact=True).click();await b.get_by_role('button',name='Aceptar conexión',exact=True).click();await a.reload();await a.locator('.am-confirmation').wait_for();await checkpoint(contexts,state)
  button=a.get_by_role('button',name='Confirmar encuentro y actividad',exact=True);await button.scroll_into_view_if_needed();await button.focus();await a.wait_for_timeout(100)
  before=await a.evaluate("()=>({scroll:scrollY,top:document.querySelector('#am-confirm-title').getBoundingClientRect().top,focus:document.activeElement.textContent.trim()})");await a.screenshot(path=str(OUT/'after-confirm-focus-360-light.png'))
  async with a.expect_navigation(wait_until='load',timeout=10000):await b.get_by_role('button',name='Ya llegué',exact=True).click()
  after=await a.evaluate("()=>({scroll:scrollY,top:document.querySelector('#am-confirm-title').getBoundingClientRect().top,focus:document.activeElement.tagName})");await a.wait_for_timeout(100);after=await a.evaluate("()=>({scroll:scrollY,top:document.querySelector('#am-confirm-title').getBoundingClientRect().top,focus:document.activeElement.tagName,id:document.activeElement.id})");assert after['id']=='am-confirm-action',after;assert abs(before['scroll']-after['scroll'])<2,(before,after);evidence.update({'beforePoll':before,'afterPoll':after,'width':390});await checkpoint(contexts,state);
  # The same-phase focus survives a second update at360px/light too.
  await a.set_viewport_size({'width':360,'height':800});await a.get_by_role('button',name='Claro',exact=True).click();await a.locator('#am-confirm-action').scroll_into_view_if_needed();await a.locator('#am-confirm-action').focus()
  async with a.expect_navigation(wait_until='load',timeout=10000):await b.get_by_role('button',name='Confirmar encuentro y actividad',exact=True).click()
  await a.wait_for_timeout(100);assert await a.locator('#am-confirm-action').evaluate('(e)=>e===document.activeElement');assert await a.locator('.am-confirmation').get_by_text('37',exact=False).count()>0
  await a.screenshot(path=str(OUT/'after-confirm-focus-360-light.png'))
  # Storage denial must preserve the native reload/flow, never retry a POST.
  await a.evaluate("()=>{const original=Storage.prototype.setItem;Storage.prototype.setItem=function(k,v){if(k==='ping-live-focus')throw new DOMException('blocked','SecurityError');return original.call(this,k,v)}}")
  await admin.goto(BASE+event)
  async with a.expect_navigation(wait_until='load',timeout=10000):await admin.get_by_role('button',name='Pausar',exact=True).click()
  assert await a.get_by_role('button',name='Confirmar encuentro y actividad',exact=True).is_enabled()
  await a.get_by_role('button',name='Confirmar encuentro y actividad',exact=True).click();await b.reload();assert await a.locator('.completion-totals .ping-balance strong').inner_text()=='37';assert await b.locator('.completion-totals .ping-balance strong').inner_text()=='37'
  for _ in range(3):await a.reload();assert await a.locator('.completion-totals .ping-balance strong').inner_text()=='37'
  assert await a.evaluate("sessionStorage.getItem('ping-live-focus')") is None
  evidence.update({'focusPreservedTwice':True,'completionPoints37Both':True,'threeReloadsNoExtraPoints':True,'presentationRecordConsumed':True,'storageBlockedStillCompletes':True,'secondUpdate360Light':True})
  (OUT/'after.json').write_text(json.dumps(evidence));await checkpoint(contexts,state);print(json.dumps(evidence));await browser.close()
asyncio.run(main())
