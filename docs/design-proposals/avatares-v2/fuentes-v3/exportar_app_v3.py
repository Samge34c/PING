"""Exporta los avatares V3 para la aplicación (DEC-058) desde el paquete PING_Avatares_V2 (no lo modifica).

Salida: src/main/resources/static/img/avatars/v3/
  base/{animal}-{pelaje}.svg              cuerpo completo (24)
  base/conejo-{pelaje}-orejas.svg         orejas del conejo para dibujar sobre el sombrero bucket (6)
  front/{animal}-{pieza}-{color}.svg      parte delantera de cada pieza (256)
  rear/{animal}-{pieza}-{color}.svg       parte trasera de auriculares, micrófono, vincha y pase (64)
Todas comparten el lienzo 256×256, de modo que se superponen por capas sin recalcular posiciones.
Correcciones V3: contorno oscuro de 8,5 px bajo el armazón de las gafas cuadradas y orejas del conejo sobre el bucket.
Uso: python -I docs/design-proposals/avatares-v2/fuentes-v3/exportar_app_v3.py
"""
import json
import re
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[1] / 'PING_Avatares_V2'
REPO = Path(__file__).resolve().parents[4]
SALIDA = REPO / 'src' / 'main' / 'resources' / 'static' / 'img' / 'avatars' / 'v3'
cat = json.loads((RAIZ / 'catalogo.json').read_text(encoding='utf-8'))

INKSCAPE = re.compile(r'\s+inkscape:[a-z]+="[^"]*"')
CUADRADAS = re.compile(r'(<path d="([^"]+)" fill="none" stroke="#[0-9A-Fa-f]{6}" stroke-width="5"([^>]*)/>)')


def svg(markup):
    return ('<svg xmlns="http://www.w3.org/2000/svg" width="256" height="256" viewBox="0 0 256 256">'
            + INKSCAPE.sub('', markup) + '</svg>\n')


def escribir(ruta, contenido):
    ruta.parent.mkdir(parents=True, exist_ok=True)
    ruta.write_text(contenido, encoding='utf-8', newline='\n')


cuenta = {'base': 0, 'front': 0, 'rear': 0, 'orejas': 0}
for animal, pelajes in cat['baseBodies'].items():
    for pelaje, markup in pelajes.items():
        escribir(SALIDA / 'base' / f'{animal}-{pelaje}.svg', svg(markup)); cuenta['base'] += 1
        if animal == 'conejo':
            orejas = re.search(r'<g id="01-orejas"[^>]*>.*?</g>', markup, re.S).group(0)
            escribir(SALIDA / 'base' / f'conejo-{pelaje}-orejas.svg', svg(orejas)); cuenta['orejas'] += 1

for animal, piezas in cat['accessories'].items():
    for pieza, colores in piezas.items():
        for color, partes in colores.items():
            front = partes['front']
            if pieza == 'gafas-cuadradas':
                front = CUADRADAS.sub(lambda m: f'<path d="{m.group(2)}" fill="none" stroke="#33213F" stroke-width="8.5"{m.group(3)}/>' + m.group(1), front)
            escribir(SALIDA / 'front' / f'{animal}-{pieza}-{color}.svg', svg(front)); cuenta['front'] += 1
            if partes['rear'].strip():
                escribir(SALIDA / 'rear' / f'{animal}-{pieza}-{color}.svg', svg(partes['rear'])); cuenta['rear'] += 1
print(cuenta)
