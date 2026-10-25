from pathlib import Path
from html import escape
import ast, hashlib, json, shutil, subprocess, zipfile
import xml.etree.ElementTree as ET

ROOT=Path(__file__).parent
PACKAGED=(ROOT/'primitivas_v1.py').exists()
OUT=ROOT.parent if PACKAGED else ROOT/'PING_Avatares_V2'
OUT.mkdir(exist_ok=True)
for name in ['01-bases','02-accesorios','03-ejemplos','04-vistas','05-fuentes','06-referencia']:
    (OUT/name).mkdir(exist_ok=True)

# Reuse only the approved V1 drawing primitives and animal paths, without
# running the old build or changing its outputs.
previous=(ROOT/'primitivas_v1.py').read_text() if PACKAGED else Path('/workspace/scratch/8d570c1e4a86/ping-avatar-delivery/build.py').read_text()
tree=ast.parse(previous)
names={'path','ellipse','group','eyes','mouth','base'}
nodes=[n for n in tree.body if isinstance(n,ast.FunctionDef) and n.name in names]
primitives=ast.unparse(ast.Module(body=nodes,type_ignores=[]))
(OUT/'05-fuentes'/'primitivas_v1.py').write_text(primitives,encoding='utf-8')
INK='#33213F'; CREAM='#FFF0DA'; PURPLE='#9A78D5'; YELLOW='#F9DA54'
exec(compile(ast.Module(body=nodes,type_ignores=[]),'primitivas_v1.py','exec'),globals())
approved_base=base

ANIMALS={'panda-rojo':'Panda rojo','nutria':'Nutria','gato':'Gato','conejo':'Conejo'}
PALETTE_NAMES={'natural':'Natural','dorado':'Dorado','lila':'Lila','perla':'Perla','cacao':'Cacao','coral':'Coral'}
# coat / ear-back / inner-ear / shadow / muzzle / minor-detail
PALETTES={
 'panda-rojo':{
  'natural':['#DC7345','#FFF0DA','#B45343','#C3623F','#FFF0DA','#FFF0DA'],
  'dorado':['#DDA354','#FFF0DA','#AE7045','#BE8042','#FFF1D9','#FFF1D9'],
  'lila':['#A18AC7','#EAE0F5','#7C5F9D','#8468AC','#F9EFDE','#F9EFDE'],
  'perla':['#A7B2BF','#EDF1F3','#727F96','#8796AC','#FFF6E7','#FFF6E7'],
  'cacao':['#97715B','#F1E3CF','#6F4D44','#785642','#FFF0DA','#FFF0DA'],
  'coral':['#DD9290','#FFEEE4','#B86D79','#BE707A','#FFF1DD','#FFF1DD']},
 'nutria':{
  'natural':['#B77C57','#A86E51','#765043','#7C5144','#FFF0DA','#7C5144'],
  'dorado':['#D1A163','#B88B55','#8C6848','#A97C4D','#FFF1D7','#886342'],
  'lila':['#A58FC3','#8B74AC','#6C538C','#806596','#F9EFDE','#6C538C'],
  'perla':['#A9B8BB','#8E9FA7','#6C7F8B','#8297A2','#FFF4E2','#667C88'],
  'cacao':['#826250','#70523F','#4E3D35','#634B3D','#EEDAC4','#5E453B'],
  'coral':['#D99B8E','#BF817E','#95626E','#B77979','#FFF0DA','#95626E']},
 'gato':{
  'natural':['#B7A6D1','#B09DCB','#EDACAD','#7C6C97','#FFF0DA','#E4DAED'],
  'dorado':['#DEA56D','#C98C5D','#EFB3A4','#AC744B','#FFF0D9','#EBC38E'],
  'lila':['#A18BC9','#8F77B8','#E6ADC4','#72568E','#FFF0E0','#C7B5E1'],
  'perla':['#B5C0CC','#A0ADBC','#E5B6B9','#7A8B9F','#FFF6E8','#DEE5EC'],
  'cacao':['#8F776C','#7E635B','#D6A0A2','#65504D','#F4E4D3','#B49C88'],
  'coral':['#D9A0A0','#C58C97','#EDBDB4','#AD7586','#FFF0DE','#EBC2BB']},
 'conejo':{
  'natural':['#FFF0DA','#FFF0DA','#EBA6A7','#F2D9C4','#FFF4E5','#EDBAB4'],
  'dorado':['#E8C18D','#E8C18D','#DB9C9B','#D4AA78','#F9E3C7','#DFA8A1'],
  'lila':['#D3C3E7','#D3C3E7','#DDA9C1','#B5A1CE','#EFE2F4','#DAAFBF'],
  'perla':['#CCD5DE','#CCD5DE','#E7B0B4','#ACBCCB','#F0F2F1','#DDB7BC'],
  'cacao':['#BA9D86','#BA9D86','#D59E9F','#A48670','#EFDFCD','#CE9C96'],
  'coral':['#EBC2BE','#EBC2BE','#DFA1B1','#D4A6AC','#FFF0E7','#DEA9AC']}
}
ACCENTS={
 'violeta':{'label':'Violeta PING','main':'#9A78D5','shade':'#7350A5','light':'#C7B0EB','detail':'#F9DA54'},
 'amarillo':{'label':'Amarillo','main':'#F5D45A','shade':'#D3A73B','light':'#FFF0A6','detail':'#7757AB'},
 'menta':{'label':'Menta','main':'#83C6B0','shade':'#529A88','light':'#C0E9DB','detail':'#F9DA54'},
 'coral':{'label':'Coral','main':'#EC9A8D','shade':'#C46D78','light':'#FFD0BD','detail':'#7757AB'}
}

def rabbit():
    ears=path('M 84 121 C 57 102 37 51 48 20 Q 52 9 63 17 C 88 37 103 77 103 113 Z',CREAM,INK)
    ears+=path('M 153 114 C 151 77 170 37 195 18 Q 207 11 211 24 C 218 54 192 105 173 121 Z',CREAM,INK)
    ears+=path('M 78 100 C 61 77 51 43 56 30 Q 82 49 92 100 Z','#EBA6A7')
    ears+=path('M 166 101 Q 168 62 201 31 Q 205 62 179 104 Z','#EBA6A7')
    head=path('M 128 101 C 95 99 72 115 61 139 C 49 156 48 175 57 193 C 66 215 96 230 128 230 C 164 230 191 215 200 192 C 209 174 206 154 195 138 C 183 115 162 100 128 101 Z',CREAM,INK)
    mask=path('M 62 187 Q 78 214 128 215 Q 174 215 195 188 Q 185 218 152 225 Q 118 234 90 219 Q 70 207 62 187 Z','#F2D9C4')
    cheeks=ellipse(75,178,9,4,'#EDBAB4')+ellipse(181,178,9,4,'#EDBAB4')
    brows=path('M 78 128 Q 89 120 101 127 M 155 127 Q 167 119 178 128','none',INK,3.3)
    eye=''
    for x in [92,164]:
        eye+=path(f'M {x-13} 147 C {x-13} 128 {x+12} 127 {x+14} 145 Q {x+8} 157 {x-6} 157 Q {x-13} 155 {x-13} 147 Z','#FFFDF5',INK,3.2)
        eye+=ellipse(x+2,144,8.5,11.5,'#70453B')+ellipse(x+4,145,5.5,8.4,INK)+ellipse(x,138,3.2,3.2,'#FFFDF5')
    muzzle=path('M 120 169 Q 128 165 136 169 Q 139 173 129 179 Q 127 180 120 174 Q 117 171 120 169 Z','#D98F98',INK,2.2)
    muzzle+=path('M 128 179 L 128 184 M 110 184 Q 118 194 128 184 Q 139 193 147 183','none',INK,3)
    return group('01-orejas',ears)+group('02-cabeza',head)+group('03-manchas-y-volumen',mask+cheeks)+group('04-cejas-y-detalles',brows)+group('05-ojos',eye)+group('06-hocico',muzzle)

def recolor_base(kind,body,palette):
    c=PALETTES[kind][palette]
    if palette=='natural':return body
    if kind=='panda-rojo': mapping=dict(zip(['#DC7345','#FFF0DA','#B45343','#C3623F'],[c[0],c[4],c[2],c[3]]))
    elif kind=='nutria':mapping=dict(zip(['#B77C57','#A86E51','#765043','#7C5144','#FFF0DA'],[c[0],c[1],c[2],c[5],c[4]]))
    elif kind=='gato':mapping=dict(zip(['#B7A6D1','#B09DCB','#EDACAD','#7C6C97','#FFF0DA','#E4DAED'],c))
    else:mapping=dict(zip(['#FFF0DA','#EBA6A7','#F2D9C4','#EDBAB4'],[c[0],c[2],c[3],c[5]]))
    import re
    return re.sub('|'.join(re.escape(k) for k in mapping),lambda m:mapping[m[0]],body)

def base_body(kind,palette='natural'):
    return recolor_base(kind,rabbit() if kind=='conejo' else approved_base(kind),palette)

ACCESS_META=[
 ('auriculares','Auriculares','audio'),('microfono','Auriculares con micrófono','audio'),
 ('gorro','Gorro tejido','cabeza'),('gorra','Gorra','cabeza'),('bucket','Sombrero bucket','cabeza'),
 ('banda','Banda deportiva','cabeza'),('lazo','Lazo','cabeza'),('pin-estrella','Pin estrella','cabeza'),
 ('vincha','Vincha de estrellas','cabeza'),
 ('gafas-redondas','Gafas redondas','cara'),('gafas-cuadradas','Gafas cuadradas','cara'),('gafas-sol','Gafas de sol','cara'),
 ('panuelo','Pañuelo','cuello'),('bufanda','Bufanda','cuello'),('corbatin','Corbatín','cuello'),('pase','Pase del evento','cuello')
]
ACCESS_INFO={k:{'label':label,'slot':slot} for k,label,slot in ACCESS_META}

def accessory(name,kind='panda-rojo',tone='violeta'):
    c=ACCENTS[tone]; m,s,l,d=c['main'],c['shade'],c['light'],c['detail']
    rear='';front=''
    if name in ['auriculares','microfono']:
        rear=path('M 32 154 C 20 39 234 39 224 154','none',INK,15)+path('M 32 154 C 20 39 234 39 224 154','none',m,8)
        front=ellipse(31,162,18,31,m,INK,4)+ellipse(225,162,18,31,m,INK,4)
        front+=path('M 33 142 Q 23 162 32 181 M 223 142 Q 233 163 224 181','none',l,4)+ellipse(25,162,3,5,d)+ellipse(33,162,3,5,d)
        if name=='microfono':
            front+=path('M 232 172 Q 238 201 195 201','none',INK,8)+path('M 232 172 Q 238 201 195 201','none',s,4)+path('M 182 195 Q 178 201 183 207 L 195 207 Q 201 201 195 195 Z',m,INK,3)
    elif name=='gorro':
        front=path('M 44 105 Q 46 59 80 49 Q 122 28 161 47 Q 196 53 208 93 L 206 107 Z',m,INK)
        front+=path('M 73 84 Q 79 62 99 49 M 112 78 Q 121 54 132 42 M 155 79 Q 160 60 156 47 M 186 89 Q 183 71 173 56','none',s,3)
        front+=path('M 42 101 Q 123 71 209 97 L 208 117 Q 128 92 45 125 Z',m,INK)
        front+=path('M 53 105 L 55 119 M 69 100 L 71 114 M 86 96 L 88 110 M 105 92 L 107 107 M 124 90 L 125 105 M 143 90 L 143 104 M 162 92 L 161 106 M 181 95 L 179 110 M 199 100 L 197 113','none',s,2.2)
    elif name=='gorra':
        front=path('M 48 107 Q 48 62 85 51 Q 130 35 172 56 Q 200 72 203 105 Z',m,INK)
        front+=path('M 125 47 Q 117 74 120 102','none',s,3)
        front+=path('M 45 105 Q 105 91 173 101 Q 200 101 226 117 Q 229 122 221 126 Q 178 127 143 113 Q 90 116 46 119 Z',s,INK)
        front+=ellipse(93,82,4.5,6,d)+ellipse(104,82,4.5,6,d)
    elif name=='bucket':
        front=path('M 65 101 L 79 54 Q 125 37 175 55 L 189 102 Z',m,INK)
        front+=path('M 68 92 Q 127 105 186 92 L 189 104 Q 126 119 65 105 Z',s)
        front+=path('M 65 101 Q 45 116 36 133 Q 122 150 221 132 Q 209 114 189 101 Q 127 114 65 101 Z',m,INK)
        front+=path('M 48 128 Q 126 140 209 127','none',l,3)
        front=f'<g transform="translate(0 -20)">{front}</g>'
    elif name=='banda':
        front=path('M 46 109 Q 129 87 209 110 L 208 126 Q 129 105 47 126 Z',m,INK,3.5)
        front+=path('M 50 115 Q 128 96 205 116','none',l,3)+ellipse(169,113,3.3,4,d)+ellipse(177,114,3.3,4,d)
    elif name=='lazo':
        front=path('M 171 94 Q 145 79 153 63 Q 169 58 181 79 Q 194 60 208 68 Q 211 84 186 95 Z',m,INK,3.5)
        front+=path('M 168 94 L 162 110 L 179 104 L 188 112 L 187 94 Z',s,INK,3)+ellipse(181,88,7,9,l,INK,3)
    elif name=='pin-estrella':
        front=path('M 183 80 L 189 91 L 202 93 L 192 102 L 194 115 L 182 109 L 171 115 L 173 102 L 163 93 L 177 91 Z',d,INK,3)
        front+=path('M 164 102 L 153 109','none',m,6)
    elif name=='vincha':
        rear=path('M 51 125 Q 51 70 128 68 Q 206 70 206 125','none',INK,9)+path('M 51 125 Q 51 70 128 68 Q 206 70 206 125','none',m,5)
        front=path('M 82 79 L 79 54 M 175 79 L 178 54','none',s,4)
        for x in [78,179]:
            front+=path(f'M {x} 34 L {x+5} 44 L {x+16} 45 L {x+8} 53 L {x+10} 64 L {x} 59 L {x-10} 64 L {x-8} 53 L {x-16} 45 L {x-5} 44 Z',d,INK,2.7)
    elif name=='gafas-redondas':
        x1,x2=(92,164) if kind=='conejo' else (87,169)
        for x in [x1,x2]:front+=ellipse(x,147,25,25,'none',INK,8)+ellipse(x,147,25,25,'none',m,4.5)
        front+=path(f'M {x1+26} 144 Q 128 137 {x2-26} 144 M {x1-27} 143 L 46 140 M {x2+27} 143 L 210 140','none',m,5)
    elif name in ['gafas-cuadradas','gafas-sol']:
        x1,x2=(92,164) if kind=='conejo' else (87,169)
        for x in [x1,x2]:
            fill=INK if name=='gafas-sol' else 'none'
            front+=path(f'M {x-24} 128 Q {x-28} 128 {x-27} 135 L {x-24} 163 Q {x-22} 168 {x-15} 168 L {x+17} 167 Q {x+24} 166 {x+25} 160 L {x+28} 135 Q {x+28} 129 {x+22} 128 Z',fill,m,5)
            if name=='gafas-sol':front+=path(f'M {x-17} 136 L {x-7} 136 L {x+11} 158 L {x+1} 158 Z',l)
        front+=path(f'M {x1+28} 137 Q 128 131 {x2-28} 137 M {x1-29} 134 L 45 129 M {x2+29} 134 L 211 129','none',m,5)
    elif name=='panuelo':
        front=path('M 78 214 Q 127 231 178 213 L 172 234 Q 126 247 81 233 Z',m,INK,3.5)
        front+=path('M 98 231 Q 77 232 64 250 Q 88 253 107 240 Z',m,INK,3)+path('M 102 231 Q 116 240 114 252 Q 97 247 95 237 Z',l,INK,3)
        front+=path('M 115 231 Q 136 238 163 227','none',s,3)
    elif name=='bufanda':
        front=path('M 75 214 Q 130 231 183 214 L 178 234 Q 130 250 77 234 Z',m,INK,3.5)
        front+=path('M 154 233 L 166 253 L 190 253 L 178 229 Z',m,INK,3)+path('M 84 224 Q 130 239 173 223 M 171 242 L 183 240','none',l,3)
    elif name=='corbatin':
        front=path('M 122 231 Q 99 215 86 222 L 86 249 Q 104 253 123 239 Z',m,INK,3.5)+path('M 134 231 Q 157 215 171 222 L 171 249 Q 151 253 133 239 Z',m,INK,3.5)
        front+=path('M 94 229 L 118 236 M 163 229 L 139 236','none',s,3)+path('M 122 227 Q 128 224 135 228 L 135 242 Q 129 245 122 241 Z',l,INK,3)
    elif name=='pase':
        rear=path('M 81 208 L 120 239 L 133 241 L 177 207','none',INK,8)+path('M 81 208 L 120 239 L 133 241 L 177 207','none',m,4)
        front=path('M 116 234 Q 116 231 120 231 L 142 231 Q 146 231 146 235 L 146 251 L 116 251 Z',l,INK,3)+path('M 119 232 L 143 232 L 143 238 L 119 238 Z',m)+ellipse(126,245,3,3,d)+ellipse(134,245,3,3,d)
    else:raise ValueError(name)
    # Hats share the forehead anchor. On the rabbit its forehead is lower;
    # move only headwear, leaving its long ears visible above the hat.
    if kind=='conejo' and name in ['gorro','gorra','bucket','banda','lazo','pin-estrella','vincha']:
        offset=4 if name in ['gorro','gorra','banda'] else 13
        front=f'<g transform="translate(0 {offset})">{front}</g>' if front else ''
        rear=f'<g transform="translate(0 {offset})">{rear}</g>' if rear else ''
    return rear,front

def doc(body,title):
    return '<svg xmlns="http://www.w3.org/2000/svg" xmlns:inkscape="http://www.inkscape.org/namespaces/inkscape" width="256" height="256" viewBox="0 0 256 256"><title>'+escape(title)+'</title>'+body+'</svg>'

def compose(kind,palette='natural',selected=None,tone='violeta'):
    rear='';front=''
    for acc in selected or []:
        a,b=accessory(acc,kind,tone);rear+=group('trasero-'+acc,a);front+=group('frontal-'+acc,b)
    return group('00-accesorios-traseros',rear)+base_body(kind,palette)+group('07-accesorios-frontales',front)

catalog={'version':'2.0','canvas':{'width':256,'height':256},'animals':ANIMALS,'paletteLabels':PALETTE_NAMES,'palettes':PALETTES,'accentColors':ACCENTS,'accessoryInfo':ACCESS_INFO,'baseBodies':{},'accessories':{},'rules':{'maxPerSlot':1,'blockedPairs':[['auriculares','gorro'],['auriculares','gorra'],['auriculares','bucket'],['auriculares','vincha'],['microfono','gorro'],['microfono','gorra'],['microfono','bucket'],['microfono','vincha']]}}
for kind in ANIMALS:
    catalog['baseBodies'][kind]={}
    catalog['accessories'][kind]={}
    for pal in PALETTE_NAMES:
        b=base_body(kind,pal);catalog['baseBodies'][kind][pal]=b
        (OUT/'01-bases'/f'{kind}--{pal}.svg').write_text(doc(compose(kind,pal),f'PING · {ANIMALS[kind]} · {PALETTE_NAMES[pal]}'),encoding='utf-8')
    for acc in ACCESS_INFO:
        catalog['accessories'][kind][acc]={}
        for tone in ACCENTS:
            a,b=accessory(acc,kind,tone);catalog['accessories'][kind][acc][tone]={'rear':a,'front':b}
        # Export one useful default, keep all four accent palettes in catalog.
        a,b=accessory(acc,kind)
        (OUT/'02-accesorios'/f'{kind}--{acc}.svg').write_text(doc(group('00-accesorio-trasero',a)+group('07-accesorio-frontal',b),f'{ACCESS_INFO[acc]["label"]} · {ANIMALS[kind]}'),encoding='utf-8')

EXAMPLES=[
 ('panda-rojo','natural',['auriculares','gafas-redondas'],'violeta'),
 ('panda-rojo','lila',['gorra','panuelo'],'amarillo'),
 ('panda-rojo','perla',['gafas-sol','bufanda'],'coral'),
 ('panda-rojo','cacao',['pin-estrella','pase'],'menta'),
 ('nutria','natural',['gorro','gafas-redondas'],'amarillo'),
 ('nutria','lila',['bucket','pase'],'menta'),
 ('nutria','dorado',['microfono','gafas-cuadradas'],'violeta'),
 ('nutria','perla',['banda','corbatin'],'coral'),
 ('gato','natural',['gafas-redondas','panuelo'],'violeta'),
 ('gato','dorado',['lazo','pase'],'menta'),
 ('gato','coral',['gafas-sol','bufanda'],'violeta'),
 ('gato','perla',['vincha','corbatin'],'amarillo'),
 ('conejo','natural',['panuelo'],'violeta'),
 ('conejo','lila',['gafas-redondas','lazo'],'amarillo'),
 ('conejo','perla',['banda','pase'],'coral'),
 ('conejo','dorado',['auriculares','gafas-cuadradas'],'menta')]
catalog['examples']=[]
for i,(kind,pal,acc,tone) in enumerate(EXAMPLES,1):
    filename=f'{i:02d}-{kind}-{pal}.svg'
    (OUT/'03-ejemplos'/filename).write_text(doc(compose(kind,pal,acc,tone),'PING · Combinación editable'),encoding='utf-8')
    catalog['examples'].append({'file':filename,'animal':kind,'palette':pal,'accessories':acc,'accent':tone})
(OUT/'catalogo.json').write_text(json.dumps(catalog,ensure_ascii=False,indent=2),encoding='utf-8')

def sheet(filename,title,subtitle,items,cols=4,cell=278,dark=False):
    rows=(len(items)+cols-1)//cols;w=cols*cell+64;h=118+rows*(cell+28)+45
    bg='#17121E' if dark else '#FAF7F0';fg='#FFF3DE' if dark else INK;muted='#C8B8DC' if dark else '#786780'
    s=[f'<svg xmlns="http://www.w3.org/2000/svg" xmlns:inkscape="http://www.inkscape.org/namespaces/inkscape" width="{w}" height="{h}" viewBox="0 0 {w} {h}"><rect width="{w}" height="{h}" fill="{bg}"/><text x="32" y="48" fill="{fg}" font-family="sans-serif" font-size="29" font-weight="bold">{escape(title)}</text><text x="32" y="78" fill="{muted}" font-family="sans-serif" font-size="15">{escape(subtitle)}</text>']
    import re
    for i,(body,label) in enumerate(items):
        x=32+(i%cols)*cell;y=110+(i//cols)*(cell+28)
        # Unique groups for a valid whole-sheet DOM.
        body=re.sub(r'id="([^"]+)"',lambda m:f'id="item-{i}-{m[1]}"',body)
        scale=(cell-20)/256
        s.append(f'<g transform="translate({x+10} {y}) scale({scale})">{body}</g><text x="{x+cell/2}" y="{y+cell+4}" fill="{fg}" font-family="sans-serif" text-anchor="middle" font-size="14">{escape(label)}</text>')
    s.append('</svg>');(OUT/'04-vistas'/f'{filename}.svg').write_text(''.join(s),encoding='utf-8')

sheet('01-personajes','PING · Cuatro caras, una familia','Conejo V2 con sonrisa cerrada y proporciones revisadas.',[(compose(k),v) for k,v in ANIMALS.items()])
sheet('02-paletas','PING · Seis paletas por personaje','Cambian pelaje y marcas; se conservan las formas y la identidad.',[(compose(k,p),f'{ANIMALS[k]} · {PALETTE_NAMES[p]}') for k in ANIMALS for p in PALETTE_NAMES],cols=6,cell=205)
sheet('03-combinaciones','PING · Personaliza tu forma de conectar','16 ejemplos de combinaciones; piezas y colores editables.',[(compose(k,p,a,t),f'{ANIMALS[k]} · {PALETTE_NAMES[p]}') for k,p,a,t in EXAMPLES])
sheet('04-oscuro','PING · En fondo oscuro','Contornos, accesorios y siluetas revisados sobre una superficie oscura.',[(compose(k,p,a,t),f'{ANIMALS[k]} · {PALETTE_NAMES[p]}') for k,p,a,t in EXAMPLES],dark=True)
for kind,label in ANIMALS.items():
    sheet(f'05-accesorios-{kind}',f'PING · Accesorios para {label.lower()}','Una pieza de cada tipo; todas usan el mismo lienzo y anclajes.',[(compose(kind,'natural',[a]),ACCESS_INFO[a]['label']) for a in ACCESS_INFO],cell=220)

# Small-size preview uses real SVG instances at exact CSS pixel dimensions.
small=['<svg xmlns="http://www.w3.org/2000/svg" width="900" height="430" viewBox="0 0 900 430"><rect width="900" height="430" fill="#FAF7F0"/><text x="32" y="44" fill="#33213F" font-family="sans-serif" font-size="25" font-weight="bold">PING · Lectura a tamaño real</text>']
for row,(k,label) in enumerate(ANIMALS.items()):
    y=83+row*82;small.append(f'<text x="32" y="{y+31}" fill="#5A4566" font-family="sans-serif" font-size="15">{label}</text>')
    for col,size in enumerate([32,48,64,96]):
        x=180+col*155;small.append(f'<svg x="{x}" y="{y}" width="{size}" height="{size}" viewBox="0 0 256 256" xmlns:inkscape="http://www.inkscape.org/namespaces/inkscape">'+compose(k)+'</svg>')
        if row==0:small.append(f'<text x="{x}" y="72" fill="#786780" font-family="sans-serif" font-size="12">{size} px</text>')
small.append('</svg>');(OUT/'04-vistas'/'06-tamanos.svg').write_text(''.join(small),encoding='utf-8')

html='''<!doctype html><html lang="es"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>PING · Configurador de avatares V2</title><style>
*{box-sizing:border-box}body{margin:0;background:#17121e;color:#fff3de;font:16px system-ui,sans-serif}main{max-width:1120px;margin:auto;padding:28px 20px}h1{font-size:clamp(24px,4vw,36px);margin:0 0 8px}p{color:#c8b8dc;line-height:1.5}.layout{display:grid;grid-template-columns:minmax(270px,.85fr) 1fr;gap:24px;align-items:start}.panel{background:#241b2e;border:1px solid #51415f;border-radius:20px;padding:22px}.preview{background:#faf7f0;border-radius:16px;display:flex;align-items:center;justify-content:center;min-height:330px}.preview.dark{background:#352940}.preview svg{width:min(100%,320px);height:auto;display:block}label{display:block;margin:14px 0 7px;font-weight:650}select,button{font:inherit;min-height:46px;border-radius:10px;border:1px solid #796589;padding:10px 12px}select{width:100%;background:#17121e;color:#fff3de}button{background:#f9da54;color:#33213f;font-weight:750;cursor:pointer}button.secondary{background:transparent;color:#ddd0ee}button:focus-visible,select:focus-visible{outline:3px solid #f9da54;outline-offset:3px}.actions{display:flex;flex-wrap:wrap;gap:10px;margin-top:18px}.fine{font-size:13px}.notice{min-height:24px;color:#f9da54;font-size:14px}.grid{display:grid;grid-template-columns:1fr 1fr;gap:0 16px}code{overflow-wrap:anywhere}@media(max-width:700px){main{padding:20px 14px}.layout{grid-template-columns:1fr}.preview{min-height:285px}.panel{padding:16px}.grid{grid-template-columns:1fr}}
</style><main><h1>PING · Hazlo tuyo</h1><p>Cuatro personajes, seis paletas y dieciséis accesorios. Funciona sin Internet. Exporta un SVG con piezas editables.</p><div class="layout"><section class="panel"><div id="preview" class="preview" aria-label="Vista previa del avatar"></div><div class="actions"><button id="download" type="button">Descargar SVG</button><button id="background" class="secondary" type="button">Ver fondo oscuro</button></div><p id="summary" class="fine"></p><p class="fine">La descarga conserva grupos y transparencias. La selección no se guarda ni se envía a ningún servidor.</p></section><section class="panel"><div class="grid"><div><label for="animal">Personaje</label><select id="animal"></select></div><div><label for="palette">Pelaje</label><select id="palette"></select></div><div><label for="tone">Color de accesorios</label><select id="tone"></select></div><div><label for="head">Cabeza</label><select id="head"></select></div><div><label for="audio">Audio</label><select id="audio"></select></div><div><label for="face">Gafas</label><select id="face"></select></div><div><label for="neck">Cuello</label><select id="neck"></select></div></div><p id="notice" class="notice" role="status" aria-live="polite"></p><button id="reset" class="secondary" type="button">Quitar accesorios</button><p class="fine">Los sombreros voluminosos y los auriculares se excluyen entre sí para evitar superposiciones. Los demás anclajes están adaptados por animal. Al importar en Figma conserva los grupos traseros y frontales.</p></section></div></main><script>
const CATALOG=__CATALOG__;
const $=id=>document.getElementById(id);const slots={head:'cabeza',audio:'audio',face:'cara',neck:'cuello'};
const option=(v,t)=>{let e=document.createElement('option');e.value=v;e.textContent=t;return e};
for(const[k,v]of Object.entries(CATALOG.animals))$('animal').append(option(k,v));
for(const[k,v]of Object.entries(CATALOG.paletteLabels))$('palette').append(option(k,v));
for(const[k,v]of Object.entries(CATALOG.accentColors))$('tone').append(option(k,v.label));
for(const[id,slot]of Object.entries(slots)){ $(id).append(option('','Sin accesorio'));for(const[k,v]of Object.entries(CATALOG.accessoryInfo))if(v.slot===slot)$(id).append(option(k,v.label));}
function choices(){return Object.keys(slots).map(id=>$(id).value).filter(Boolean)}
function valid(changed){let selected=choices();for(const[a,b]of CATALOG.rules.blockedPairs)if(selected.includes(a)&&selected.includes(b)){for(const id of Object.keys(slots))if(id!==changed&&($(id).value===a||$(id).value===b))$(id).value='';$('notice').textContent='Se retiró el accesorio que se superponía con tu última elección.';}}
function artwork(){let k=$('animal').value,p=$('palette').value,t=$('tone').value;let back='',front='';for(const a of choices()){let part=CATALOG.accessories[k][a][t];back+='<g id="trasero-'+a+'">'+part.rear+'</g>';front+='<g id="frontal-'+a+'">'+part.front+'</g>';}
return '<svg xmlns="http://www.w3.org/2000/svg" xmlns:inkscape="http://www.inkscape.org/namespaces/inkscape" width="256" height="256" viewBox="0 0 256 256"><title>PING · Avatar personalizado</title><g id="00-accesorios-traseros">'+back+'</g>'+CATALOG.baseBodies[k][p]+'<g id="07-accesorios-frontales">'+front+'</g></svg>'}
function update(){ $('preview').innerHTML=artwork();$('summary').textContent=CATALOG.animals[$('animal').value]+' · '+CATALOG.paletteLabels[$('palette').value]+' · '+choices().length+' accesorios';}
for(const id of ['animal','palette','tone',...Object.keys(slots)])$(id).addEventListener('change',()=>{$('notice').textContent='';valid(id);update()});
$('reset').addEventListener('click',()=>{for(const id of Object.keys(slots))$(id).value='';$('notice').textContent='';update()});
$('background').addEventListener('click',()=>{let dark=$('preview').classList.toggle('dark');$('background').textContent=dark?'Ver fondo claro':'Ver fondo oscuro'});
$('download').addEventListener('click',()=>{let blob=new Blob([artwork()],{type:'image/svg+xml;charset=utf-8'});let url=URL.createObjectURL(blob);let a=document.createElement('a');a.href=url;a.download='ping-'+$('animal').value+'-'+$('palette').value+'.svg';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000)});
window.PINGAvatar={artwork,choices,catalog:CATALOG};update();
</script></html>'''
(OUT/'CONFIGURADOR.html').write_text(html.replace('__CATALOG__',json.dumps(catalog,ensure_ascii=False,separators=(',',':'))),encoding='utf-8')

prompt='''Continúa el sistema de avatares de PING en Figma usando el paquete PING_Avatares_V2 que adjunto o que he extraído en el proyecto.

OBJETIVO
Importa y mejora estos iconos 2D editables. Samuel aprueba panda rojo, nutria y gato como dirección; conserva sus caras y su estilo. El conejo fue corregido: sin dientes salientes, mejillas suaves, ojos más próximos y sonrisa cerrada. Puedes refinar proporciones y uniones si detectas defectos; no vuelvas a personajes realistas, cabezas humanas ni criaturas deformes.

FIGMA
https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/
Lee el archivo actual y conserva las páginas anteriores. Trabaja en una nueva página «PING · Avatares V3». Usa la conexión de Figma ya configurada; no presupongas falta de permisos por una etiqueta de asiento.

ANTES DE EDITAR
Comprueba repositorio, rama y cambios. Lee AGENTS.md y el contexto de diseño necesario, LEEME.md, catalogo.json por secciones y las vistas del paquete. No cargues cientos de SVG a contexto. La imagen artística de referencia y los SVG son diferentes: los vectores son una interpretación simplificada, no un calco idéntico.

SKILLS
Carga figma-use y las habilidades que esta requiera antes de escribir. Reutiliza logo-design únicamente para coherencia de marca cuando proceda; los personajes no deben rehacer el logo. Aplica habilidades de ilustración, diseño móvil y verificación si están disponibles y son pertinentes. No instales un catálogo entero ni actives hooks.

IMPORTACIÓN EDITABLE
Importa SVG como vectores nativos, por ejemplo mediante figma.createNodeFromSvg cuando la herramienta disponible lo permita. No los conviertas en PNG ni uses una captura plana como supuesto componente editable. Conserva nombres y orden de capas: accesorios traseros → orejas → cabeza → manchas → detalles → ojos → hocico → accesorios frontales. Verifica una importación pequeña primero y revisa su captura antes de importar el resto. Procesa lotes manejables; evita respuestas enormes de la herramienta.

COMPONENTES Y COLORES
Hay cuatro animales, seis paletas por animal y dieciséis diseños de accesorio adaptados a cada especie. Los cuatro colores de accesorios están en catalogo.json y el configurador; los SVG sueltos de accesorios están exportados en violeta. Crea componentes de base y accesorios reutilizables, con propiedades claras para animal, pelaje y slots Cabeza/Audio/Cara/Cuello. Aplica estilos o variables a rellenos cuando aporte valor. No generes miles de variantes de todas las combinaciones: usa instancias, intercambio de componentes y propiedades.

AJUSTE VISUAL
Conserva el carácter 2D, curvas limpias y lectura pequeña. Revisa contornos, alineación óptica de ojos y hocico, separación de gafas, orejas con sombreros, contraste en ambos fondos y accesorios que tapan expresiones. Corrige con criterio; no reconstruyas todo por comodidad. Los anclajes del conejo son distintos y ya están contemplados en los accesorios por especie. Dos sombreros o dos tipos de gafas no se combinan; auriculares y sombreros voluminosos se excluyen. Usa las reglas del catálogo como punto de partida y documenta cualquier compatibilidad nueva que hayas revisado visualmente.

APLICACIÓN MÓVIL
Diseña la selección de personaje y personalización a 390×844, comprueba 360×740 y texto ampliado. Vista previa protagonista, controles claros y continuar alcanzable. Muestra también avatar en cabecera de Mi Ping y en tarjeta de encuentro. La persona elige su apariencia; los gustos no asignan automáticamente un animal ni personalidad. No se suben fotografías.

VERIFICACIÓN PROPORCIONAL
Revisa 32/48/64/96 px, temas claro y oscuro, todos los animales con accesorios representativos, y nombres largos. Si simplificas una versión de 32 px, conserva una variante detallada para el editor. No ejecutes la suite Java por una propuesta de Figma. No afirmes haber probado Figma o un teléfono si no lo hiciste.

ALCANCE Y CIERRE
Puedes mejorar y completar la propuesta de forma autónoma dentro de esta página. No modifiques backend, almacenamiento, registro, matching, puntos ni código de PING en esta tarea. Guarda las fuentes, exportaciones y una continuidad breve en una rama de diseño adecuada; no sobrescribas cambios ajenos ni hagas merge o push a main.
Entrega enlaces reales a los frames, captura móvil, componentes editables, correcciones concretas y límites de lo comprobado. Si falla una herramienta, trabaja en lo independiente y explica el bloqueo preciso. Empieza importando una base y un accesorio y continúa hasta dejar una propuesta revisable, no solo un plan.
'''
(OUT/'PROMPT_CLAUDE_FIGMA.md').write_text(prompt,encoding='utf-8')

(OUT/'LEEME.md').write_text('''# PING · Avatares editables V2

## Qué cambió
- Se conservan las caras vectoriales de panda rojo, nutria y gato de V1.
- Conejo nuevo: mejillas redondeadas, ojos más próximos, nariz rosada pequeña, sonrisa cerrada y sin dientes salientes.
- 6 paletas para cada animal (24 bases): Natural, Dorado, Lila, Perla, Cacao y Coral.
- 16 accesorios en cuatro colores (Violeta, Amarillo, Menta, Coral), adaptados al conejo y a los otros animales.
- 16 combinaciones de ejemplo, configurador local y vistas de las piezas reales.

## Empezar
1. Extrae TODO el ZIP conservando sus carpetas.
2. Abre CONFIGURADOR.html con Chrome, Edge o Safari de escritorio. No necesita servidor ni conexión a Internet.
3. Elige animal, pelaje y accesorios. El color de accesorios se aplica conjuntamente en este configurador; en Figma puedes recolorear cada pieza por separado.
4. Descarga el SVG. Arrástralo a Figma para editar formas y rellenos; conserva el orden de grupos traseros y frontales.

## Carpetas
- 01-bases: 24 SVG transparentes sin accesorios.
- 02-accesorios: 64 SVG (16 accesorios × 4 animales), exportados en violeta. En catalogo.json y CONFIGURADOR.html están las cuatro opciones de color.
- 03-ejemplos: 16 combinaciones listas para importar.
- 04-vistas: láminas SVG/PNG renderizadas desde los vectores reales, incluidos todos los accesorios por especie y tamaños de lectura.
- 05-fuentes: generador Python y primitivas, para reconstrucción y ampliación.
- 06-referencia: referencia artística generada; no es la fuente vectorial ni una copia idéntica de los SVG.
- catalogo.json: cuerpos SVG, accesorios, colores, slots, ejemplos y reglas de superposición.
- PROMPT_CLAUDE_FIGMA.md: encargo listo para Claude Code.

## Accesorios
Audio: auriculares, auriculares con micrófono.
Cabeza: gorro, gorra, bucket, banda, lazo, pin estrella, vincha de estrellas.
Cara: gafas redondas, cuadradas, de sol.
Cuello: pañuelo, bufanda, corbatín, pase del evento.

Una pieza por slot. Los auriculares y los sombreros voluminosos se excluyen para evitar cruces; el configurador conserva tu última elección y retira la incompatible. El sistema ofrece mezclas de slots permitidas, pero no se ha revisado manualmente cada combinación múltiple posible. Todos los accesorios individuales por especie aparecen en las láminas.

## Editabilidad y alcance
Los personajes y accesorios SVG contienen formas y curvas nativas, sin imágenes incrustadas ni fuentes. Los grupos están nombrados. Las vistas sí contienen texto de presentación. Un SVG importado sigue necesitando convertirse a componentes de Figma; esto aún no está hecho ni verificado en Figma.

No hay integración en PING, animaciones, persistencia de elección ni cambios en reglas de producto. Estos recursos no son una identidad aprobada definitivamente. La referencia artística usa ImageGen y los SVG se construyen como interpretación vectorial simplificada. No atribuyas la referencia raster a un calco exacto.

## Reconstruir
Con Python 3: ejecuta `python 05-fuentes/build_v2.py`. El generador reconstruye los SVG, el catálogo y el configurador con la biblioteca estándar de Python. Los PNG incluidos son exportaciones de las láminas SVG hechas con Inkscape; pueden volver a exportarse con Figma o Inkscape. Al reconstruir no borra archivos ajenos; crea o actualiza los resultados con los mismos nombres dentro del paquete.
''',encoding='utf-8')

# Save reproducible sources; the bootstrap below uses packaged primitives when moved.
if Path(__file__).resolve()!=(OUT/'05-fuentes'/'build_v2.py').resolve():
    shutil.copy2(__file__,OUT/'05-fuentes'/'build_v2.py')
ref=Path('/workspace/scratch/8d570c1e4a86/generated_images/exec-c46ac18e-a2ce-4dcb-9c3c-b5f1a88c745a.png')
if ref.exists():shutil.copy2(ref,OUT/'06-referencia'/'referencia-artistica-conejo-v2.png')

svg_files=list(OUT.rglob('*.svg'))
for f in svg_files:
    xml=ET.parse(f).getroot();assert not xml.findall('.//{http://www.w3.org/2000/svg}image'),f
    assert 'data:image' not in f.read_text(),f

print(json.dumps({'output':str(OUT),'bases':24,'accessory_svg':64,'accessory_designs':16,'accent_colors':4,'examples':16,'svg_total':len(svg_files)},ensure_ascii=False))
