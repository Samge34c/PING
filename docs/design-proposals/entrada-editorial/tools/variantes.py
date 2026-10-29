"""Variantes servibles del arte de entrada «Encuentro editorial» (propuesta, no integrada).

Recorta las franjas negras laterales de la fuente generada y exporta WebP para srcset a 768, 1152 y 1400 px
(la fuente útil mide 1406 px; no se amplía). Uso desde la raíz del repositorio:
python -I docs/design-proposals/entrada-editorial/tools/variantes.py   (Pillow con WebP; no es dependencia de la app)
"""
import hashlib
import json
import pathlib

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE = ROOT / "fuentes" / "encuentro-editorial-a-94e546.png"
OUT = ROOT / "variantes"
CONTENT_BOX = (140, 0, 1540, 940)  # contenido útil medido: x 137–1542, y 0–940
TARGETS = {768: 87, 1152: 84, 1400: 82}  # ancho -> calidad WebP

OUT.mkdir(exist_ok=True)
with Image.open(SOURCE) as original:
    master = original.convert("RGB").crop(CONTENT_BOX)
report = {}
for width, quality in TARGETS.items():
    height = round(master.height * width / master.width)
    target = OUT / f"encuentro-editorial-{width}.webp"
    image = master if width == master.width else master.resize((width, height), Image.LANCZOS)
    image.save(target, "WEBP", quality=quality, method=6)
    data = target.read_bytes()
    report[target.name] = {"width": width, "height": height, "quality": quality,
                           "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest()}
print(json.dumps(report, indent=2))
