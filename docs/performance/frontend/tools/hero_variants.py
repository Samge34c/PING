"""Variantes reducidas de la ilustración de Inicio para srcset (misma imagen, menos píxeles en pantallas pequeñas).

El original de 1536 px se conserva y sigue sirviendo a escritorio y pantallas densas. Uso (raíz del repositorio):
python docs/performance/frontend/tools/hero_variants.py   (requiere Pillow con WebP; no es dependencia de la app)
"""
import pathlib

from PIL import Image

SOFA = pathlib.Path(__file__).resolve().parents[4] / "src/main/resources/static/img/sofa"
SOURCE = SOFA / "convencion-social.webp"

with Image.open(SOURCE) as original:
    image = original.convert("RGB")
    for width in (768, 1152):
        height = round(image.height * width / image.width)
        target = SOFA / f"convencion-social-{width}.webp"
        image.resize((width, height), Image.LANCZOS).save(target, "WEBP", quality=85, method=6)
        print(target.name, target.stat().st_size, "B")
