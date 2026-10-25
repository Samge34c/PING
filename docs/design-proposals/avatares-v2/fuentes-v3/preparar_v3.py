"""Prepara las plantillas V3 a partir del paquete PING_Avatares_V2 (no modifica el paquete).

- Bases: alinea las 6 paletas de cada animal elemento por elemento; cada combinación distinta de colores
  se convierte en un «papel» con su variable (modos = paletas). En la plantilla se sustituye por un
  color marcador único que Figma enlaza después a la variable.
- Accesorios: lo mismo con los 4 colores de acento.
- Correcciones V3 documentadas en CORRECCIONES (se aplican solo a la plantilla).
"""
import json
import re
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[1] / 'PING_Avatares_V2'
SALIDA = Path(__file__).resolve().parent
HEX = re.compile(r'#[0-9A-Fa-f]{6}')

cat = json.loads((RAIZ / 'catalogo.json').read_text(encoding='utf-8'))
PALETAS = list(cat['paletteLabels'].keys())
ACENTOS = list(cat['accentColors'].keys())

CORRECCIONES = {
    # Gafas cuadradas: el armazón era solo violeta de 5 px, sin contorno; se pierde en claro y a 48 px.
    # Se añade el mismo contorno oscuro (8 px) que ya usan las gafas redondas.
    'gafas-cuadradas': lambda s: re.sub(
        r'(<path d="([^"]+)" fill="none" stroke="#[0-9A-Fa-f]{6}" stroke-width="5"([^>]*)/>)',
        lambda m: f'<path d="{m.group(2)}" fill="none" stroke="#33213F" stroke-width="8.5"{m.group(3)}/>' + m.group(1), s),
}


def plantilla(versiones, prefijo):
    """versiones: lista de markup con idéntica estructura. Devuelve markup con marcadores y tabla de papeles."""
    secuencias = [HEX.findall(v) for v in versiones]
    papeles, marcadores = {}, []
    for tupla in zip(*secuencias):
        tupla = tuple(h.upper() for h in tupla)
        if len(set(tupla)) == 1:
            marcadores.append(tupla[0])
            continue
        if tupla not in papeles:
            papeles[tupla] = f'#{prefijo:02X}{len(papeles) + 1:02X}EE'
        marcadores.append(papeles[tupla])
    it = iter(marcadores)
    markup = HEX.sub(lambda m: next(it), versiones[0])
    return markup, {v: list(k) for k, v in papeles.items()}


def nombre_papel(markup, marcador):
    """Nombre legible: grupo donde aparece el marcador."""
    grupos = re.findall(r'<g id="([^"]+)"[^>]*>(.*?)</g>', markup, re.S)
    for gid, cuerpo in grupos:
        if marcador in cuerpo:
            return re.sub(r'^\d+-', '', gid)
    return 'color'


salida = {'paletas': PALETAS, 'acentos': ACENTOS, 'bases': {}, 'accesorios': {}, 'slots': {}, 'reglas': cat['rules']}
for i, animal in enumerate(cat['baseBodies']):
    versiones = [cat['baseBodies'][animal][p] for p in PALETAS]
    markup, papeles = plantilla(versiones, 0xA0 + i)
    markup = re.sub(r'\s+inkscape:[a-z]+="[^"]*"', '', markup)
    nombres, usados = {}, {}
    for m in papeles:
        base = nombre_papel(markup, m)
        usados[base] = usados.get(base, 0) + 1
        nombres[m] = f'{animal}/{base}-{usados[base]}'
    salida['bases'][animal] = {'markup': markup, 'papeles': {m: {'nombre': nombres[m], 'valores': v} for m, v in papeles.items()}}

for clave, info in cat['accessoryInfo'].items():
    salida['slots'][clave] = info['slot']
violeta = cat['accentColors']['violeta']
rol_de = {violeta[k].upper(): k for k in ('main', 'shade', 'light', 'detail')}
NOMBRES_ROL = {'main': 'principal', 'shade': 'sombra', 'light': 'claro', 'detail': 'detalle'}
MARCA_ROL = {'main': '#B001EE', 'shade': '#B002EE', 'light': '#B003EE', 'detail': '#B004EE'}
for animal, piezas in cat['accessories'].items():
    salida['accesorios'][animal] = {}
    for pieza, colores in piezas.items():
        partes = {}
        for parte in ('rear', 'front'):
            versiones = [colores[c][parte] for c in ACENTOS]
            if pieza in CORRECCIONES:
                versiones = [CORRECCIONES[pieza](v) for v in versiones]
            seqs = [HEX.findall(v) for v in versiones]
            marcas = []
            for tupla in zip(*seqs):
                tupla = tuple(h.upper() for h in tupla)
                if len(set(tupla)) == 1:
                    marcas.append(tupla[0])
                    continue
                rol = rol_de[tupla[0]]
                esperado = tuple(cat['accentColors'][c][rol].upper() for c in ACENTOS)
                assert tupla == esperado, (animal, pieza, tupla, esperado)
                marcas.append(MARCA_ROL[rol])
            it = iter(marcas)
            partes[parte] = HEX.sub(lambda m: next(it), versiones[0])
        salida['accesorios'][animal][pieza] = partes
salida['acento'] = {MARCA_ROL[r]: {'nombre': 'acento/' + NOMBRES_ROL[r], 'valores': [cat['accentColors'][c][r] for c in ACENTOS]} for r in MARCA_ROL}
(SALIDA / 'plantillas_v3.json').write_text(json.dumps(salida, ensure_ascii=False), encoding='utf-8')
print({a: len(b['papeles']) for a, b in salida['bases'].items()}, 'papeles por animal;',
      sum(len(p) for p in salida['accesorios'].values()), 'accesorios')
