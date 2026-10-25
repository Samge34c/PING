"""Genera los scripts de importación a Figma por lotes (bases y accesorios por animal) desde plantillas_v3.json.

Corrección V3 aplicada aquí: el sombrero bucket del conejo tapaba las orejas; se dibujan las orejas
(con sus colores de pelaje) por encima de la copa.
"""
import json
import re
from pathlib import Path

D = Path(__file__).resolve().parent
d = json.loads((D / 'plantillas_v3.json').read_text(encoding='utf-8'))
ANIMAL = {'panda-rojo': 'Panda rojo', 'nutria': 'Nutria', 'gato': 'Gato', 'conejo': 'Conejo'}
PIEZA = {'auriculares': 'Auriculares', 'microfono': 'Auriculares con micrófono', 'gorro': 'Gorro tejido', 'gorra': 'Gorra',
         'bucket': 'Sombrero bucket', 'banda': 'Banda deportiva', 'lazo': 'Lazo', 'pin-estrella': 'Pin estrella',
         'vincha': 'Vincha de estrellas', 'gafas-redondas': 'Gafas redondas', 'gafas-cuadradas': 'Gafas cuadradas',
         'gafas-sol': 'Gafas de sol', 'panuelo': 'Pañuelo', 'bufanda': 'Bufanda', 'corbatin': 'Corbatín', 'pase': 'Pase del evento'}
SLOT = {'audio': 'Audio', 'cabeza': 'Cabeza', 'cara': 'Cara', 'cuello': 'Cuello'}
MARK = {mk: v['nombre'] for b in d['bases'].values() for mk, v in b['papeles'].items()}
MARK.update({mk: v['nombre'] for mk, v in d['acento'].items()})

HEAD = r'''
const page=figma.root.children.find(p=>p.name==='PING · Avatares V3');await figma.setCurrentPageAsync(page);
const VAR={};for(const v of await figma.variables.getLocalVariablesAsync())VAR[v.name]=v;
const MAP={};for(const [mk,n] of Object.entries(MARK))MAP[mk]=VAR[n];
const toHex=c=>'#'+[c.r,c.g,c.b].map(x=>Math.round(x*255).toString(16).padStart(2,'0')).join('').toUpperCase();
function bind(root){let n=0;for(const node of root.findAll(x=>'fills'in x)){for(const prop of ['fills','strokes']){const ps=node[prop];if(!Array.isArray(ps)||!ps.length)continue;let ch=false;const np=ps.map(p=>{if(p.type!=='SOLID')return p;const v=MAP[toHex(p.color)];if(!v)return p;ch=true;n++;return figma.variables.setBoundVariableForPaint(p,'color',v)});if(ch)node[prop]=np}}return n}
function importComp(name,markup){const c=figma.createComponent();c.name=name;c.resize(256,256);c.fills=[];c.clipsContent=false;if(markup){const f=figma.createNodeFromSvg(`<svg xmlns="http://www.w3.org/2000/svg" width="256" height="256" viewBox="0 0 256 256">${markup}</svg>`);for(const ch of [...f.children])c.appendChild(ch);f.remove()}return [c,bind(c)]}
let hold=page.findOne(n=>n.type==='FRAME'&&n.name===HOLD);if(!hold){hold=figma.createFrame();hold.name=HOLD;hold.fills=[];hold.layoutMode='HORIZONTAL';hold.layoutWrap='WRAP';hold.itemSpacing=24;hold.counterAxisSpacing=24;hold.resize(2400,100);hold.primaryAxisSizingMode='FIXED';hold.counterAxisSizingMode='AUTO';hold.x=0;hold.y=Y0}
const out=[];for(const it of ITEMS){if(hold.children.some(n=>n.description===it.desc))continue;const [c,b]=importComp(it.name,it.markup);c.description=it.desc;hold.appendChild(c);out.push([it.desc,c.id,b])}
return {hold:hold.id,count:out.length,items:out};
'''


def script(hold, y0, items):
    return (f'const MARK={json.dumps(MARK, ensure_ascii=False)};\nconst HOLD={json.dumps(hold)};const Y0={y0};\n'
            f'const ITEMS={json.dumps(items, ensure_ascii=False)};\n' + HEAD)


bases = [{'name': f'Animal={ANIMAL[a]}', 'desc': f'base:{a}', 'markup': b['markup']} for a, b in d['bases'].items()]
(D / 'lote_bases.js').write_text(script('Lote · Bases', 400, bases), encoding='utf-8')

orejas_conejo = re.search(r'<g id="01-orejas">.*?</g>', d['bases']['conejo']['markup'], re.S).group(0).replace('01-orejas', 'orejas-sobre-sombrero')
for i, (animal, piezas) in enumerate(d['accesorios'].items()):
    items = []
    for pieza, partes in piezas.items():
        front = partes['front']
        if animal == 'conejo' and pieza == 'bucket':
            front = front + orejas_conejo
        slot = SLOT[d['slots'][pieza]]
        items.append({'name': f'Animal={ANIMAL[animal]}, Pieza={PIEZA[pieza]}', 'desc': f'frontal:{slot}:{animal}:{pieza}',
                      'markup': f'<g id="07-accesorio-frontal">{front}</g>'})
        if partes['rear'].strip():
            items.append({'name': f'Animal={ANIMAL[animal]}, Pieza={PIEZA[pieza]}', 'desc': f'trasero:{slot}:{animal}:{pieza}',
                          'markup': f'<g id="00-accesorio-trasero">{partes["rear"]}</g>'})
    (D / f'lote_acc_{animal}.js').write_text(script(f'Lote · Accesorios {ANIMAL[animal]}', 800 + i * 700, items), encoding='utf-8')
print([p.name + ' ' + str(p.stat().st_size) for p in sorted(D.glob('lote_*.js'))])
