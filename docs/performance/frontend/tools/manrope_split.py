"""Divide la Manrope variable oficial en dos archivos con los mismos glifos, ejes y funciones OpenType.

- Manrope-Variable-core.ttf: latín, latín extendido A, puntuación, flechas y signos que usa la interfaz. Se precarga.
- Manrope-Variable-extra.ttf: el resto de caracteres de la fuente (griego, cirílico, vietnamita, etc.). El navegador
  solo lo descarga si la página contiene alguno (unicode-range en brand.css). Ningún glifo se pierde.

Uso (desde la raíz del repositorio): python docs/performance/frontend/tools/manrope_split.py
Requiere fontTools (solo para regenerar; no es dependencia de la aplicación).
"""
import pathlib

from fontTools import subset
from fontTools.ttLib import TTFont

FONTS = pathlib.Path(__file__).resolve().parents[4] / "src/main/resources/static/fonts"
SOURCE = FONTS / "Manrope-Variable.ttf"
CORE = ("U+0000-00FF,U+0100-017F,U+0131,U+0152-0153,U+02BB-02BC,U+02C6,U+02DA,U+02DC,U+0300-0301,U+0303,U+0308,"
        "U+2000-206F,U+20AC,U+2122,U+2190-2199,U+2212,U+2248,U+2265,U+FEFF,U+FFFD")


def ranges(cps):
    out, start, prev = [], None, None
    for cp in sorted(cps):
        if start is None: start = prev = cp
        elif cp == prev + 1: prev = cp
        else: out.append((start, prev)); start = prev = cp
    out.append((start, prev))
    return ",".join(f"U+{a:04X}" if a == b else f"U+{a:04X}-{b:04X}" for a, b in out)


def write(unicodes, target):
    options = subset.Options()
    options.layout_features = ["*"]
    options.name_IDs = ["*"]
    options.name_languages = ["*"]
    options.notdef_outline = True
    font = TTFont(SOURCE)
    sub = subset.Subsetter(options)
    sub.populate(unicodes=unicodes)
    sub.subset(font)
    font.save(target)


def main():
    core = set(subset.parse_unicodes(CORE))
    available = set(TTFont(SOURCE).getBestCmap())
    extra = available - core
    write(core & available, FONTS / "Manrope-Variable-core.ttf")
    write(extra, FONTS / "Manrope-Variable-extra.ttf")
    print("unicode-range del núcleo:", CORE)
    print("unicode-range del resto: ", ranges(extra))


if __name__ == "__main__":
    main()
