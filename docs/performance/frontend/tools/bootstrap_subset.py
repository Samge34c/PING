"""Genera static/vendor/bootstrap/bootstrap.ping.min.css a partir del Bootstrap 5.3.8 oficial local.

Conserva una regla si alguna parte de su selector puede coincidir con el marcado de PING: todas sus clases
(fuera de :not/:is/:where/:has) aparecen como palabra en templates, JavaScript público o Java. Las reglas sin
clases (variables :root, temas data-bs-theme, reboot de elementos) y todas las @font-face/@keyframes se conservan
completas. Es deliberadamente conservador: ante la duda, la regla se queda.

Uso (desde la raíz del repositorio):  python docs/performance/frontend/tools/bootstrap_subset.py [--check]
--check falla si el archivo generado no coincide con el actual (sirve tras cambiar templates o JS).
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[4]
RES = ROOT / "src/main/resources"
SOURCE = RES / "static/vendor/bootstrap/bootstrap.min.css"
TARGET = RES / "static/vendor/bootstrap/bootstrap.ping.min.css"
CORPUS = [(RES / "templates", "*.html"), (RES / "static/js", "*.js"), (ROOT / "src/main/java", "*.java")]
GROUPS = ("@media", "@supports", "@container", "@layer")


def corpus_words():
    words = set()
    for folder, pattern in CORPUS:
        for path in folder.rglob(pattern):
            words.update(re.findall(r"[A-Za-z0-9_-]+", path.read_text(encoding="utf-8")))
    return words


def blocks(css):
    """Divide CSS minificado en (prelude, body|None) de primer nivel respetando cadenas y comentarios."""
    out, i, n = [], 0, len(css)
    while i < n:
        if css.startswith("/*", i):  # comentario entre reglas (licencia): se conserva tal cual
            end = css.index("*/", i) + 2
            out.append((css[i:end], None)); i = end; continue
        start, quote = i, None
        while i < n:
            c = css[i]
            if quote:
                if c == "\\": i += 1
                elif c == quote: quote = None
            elif c in "\"'": quote = c
            elif css.startswith("/*", i):
                i = css.index("*/", i) + 2; continue
            elif c == ";" and css[start:i].lstrip().startswith("@"):
                out.append((css[start:i + 1], None)); i += 1; break
            elif c == "{":
                depth, j, q = 1, i + 1, None
                while depth:
                    d = css[j]
                    if q:
                        if d == "\\": j += 1
                        elif d == q: q = None
                    elif d in "\"'": q = d
                    elif d == "{": depth += 1
                    elif d == "}": depth -= 1
                    j += 1
                out.append((css[start:i], css[i + 1:j - 1])); i = j; break
            i += 1
        else:
            if css[start:].strip(): out.append((css[start:], None))
    return out


def required_classes(part):
    part = re.sub(r":(?:not|is|where|has)\((?:[^()]|\([^()]*\))*\)", "", part)
    part = re.sub(r"\[[^\]]*\]", "", part)
    return re.findall(r"\.(-?[_a-zA-Z][_a-zA-Z0-9-]*)", part)


def keep_selector(selector, words):
    parts = [p for p in selector.split(",") if p.strip()]
    kept = [p for p in parts if all(c in words for c in required_classes(p))]
    return ",".join(kept)


def subset(css, words):
    out = []
    for prelude, body in blocks(css):
        head = prelude.strip()
        if body is None:
            out.append(prelude); continue
        if head.startswith(GROUPS):
            inner = subset(body, words)
            if inner: out.append(f"{prelude}{{{inner}}}")
        elif head.startswith("@"):
            out.append(f"{prelude}{{{body}}}")  # @font-face, @keyframes, @page: completos
        else:
            sel = keep_selector(prelude, words)
            if sel: out.append(f"{sel}{{{body}}}")
    return "".join(out)


def main():
    css = SOURCE.read_text(encoding="utf-8")
    css = css.replace("/*# sourceMappingURL=bootstrap.min.css.map */", "")  # el mapa no corresponde al subconjunto
    result = subset(css, corpus_words()).rstrip() + "\n"
    banner = "/* PING: subconjunto generado de bootstrap.min.css 5.3.8 (MIT) por docs/performance/frontend/tools/bootstrap_subset.py. No editar a mano. */\n"
    result = result.replace('@charset "UTF-8";', '@charset "UTF-8";' + banner, 1)
    if "--check" in sys.argv:
        current = TARGET.read_text(encoding="utf-8") if TARGET.exists() else ""
        if current != result:
            sys.exit("bootstrap.ping.min.css no coincide con templates/JS/Java actuales: vuelve a generarlo.")
        print("bootstrap.ping.min.css al día"); return
    TARGET.write_text(result, encoding="utf-8", newline="\n")
    print(f"{SOURCE.name}: {len(css)} B -> {TARGET.name}: {len(result.encode())} B")


if __name__ == "__main__":
    main()
