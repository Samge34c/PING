"""Genera el prototipo aislado actual/propuesta a partir del HTML público servido por producción (prototipo/vivo/).

Las rutas con huella (/css/brand-<hash>.css) se dirigen al estático local del repositorio, de modo que actual y
propuesta comparten CSS, fuentes, contenido y tamaño; la propuesta solo añade lo de integracion/entrada-editorial.css
y los mismos cambios de marcado que tools/aplicar_entrada.py. Servir desde la raíz del repositorio:
    python -m http.server 8765
    http://localhost:8765/docs/design-proposals/entrada-editorial/prototipo/   (?tema=claro para el tema claro)
"""
import pathlib
import re

HERE = pathlib.Path(__file__).resolve().parents[1]
PROTO = HERE / "prototipo"
STATIC = "/src/main/resources/static/"
VAR = "/docs/design-proposals/entrada-editorial/variantes/"
PAGES = {"inicio": ("home.html", "home"), "unirme": ("join_event.html", "join"), "entrar": ("login.html", "login"), "crear": ("register.html", "login")}
THEME = "<script>if(new URLSearchParams(location.search).get('tema')==='claro'){document.documentElement.dataset.theme='light';document.documentElement.dataset.bsTheme='light'}</script>"
CSS = (HERE / "integracion" / "entrada-editorial.css").read_text(encoding="utf-8")


def local(html):
    html = re.sub(r'(?<=["(])/(css|js|img|fonts|vendor)/([^"#?)]+?)-[0-9a-f]{32}\.(\w+)', lambda m: f"{STATIC}{m[1]}/{m[2]}.{m[3]}", html)
    html = re.sub(r'(?<=["(])/(css|js|img|fonts|vendor)/', lambda m: f"{STATIC}{m[1]}/", html)
    return html.replace("</head>", THEME + "</head>", 1)


def current(html, section):
    # ui.js deduce la pestaña de la ruta real; en el prototipo se marca a mano la que tendría producción.
    return html.replace(f'data-nav="{section}" href', f'data-nav="{section}" aria-current="page" href')


home = (PROTO / "vivo" / "home.html").read_text(encoding="utf-8")
nav = re.search(r'<nav class="ping-bottom-nav".*?</nav>', home, re.S)[0]
for name, (source, section) in PAGES.items():
    html = local((PROTO / "vivo" / source).read_text(encoding="utf-8"))
    (PROTO / f"actual-{name}.html").write_text(current(html, section), encoding="utf-8", newline="\n")
    if name == "inicio":
        html = re.sub(r'<figure class="fe-art"><img [^>]*></figure>',
                      f'<figure class="fe-art"><img src="{VAR}encuentro-editorial-1152.webp" srcset="{VAR}encuentro-editorial-768.webp 768w, {VAR}encuentro-editorial-1152.webp 1152w, {VAR}encuentro-editorial-1400.webp 1400w" sizes="(min-width: 768px) 566px, calc(100vw - 32px)" width="1400" height="940" fetchpriority="high" decoding="async" alt="Ilustración de dos personas que se ríen al descubrir un gusto en común en un evento con música, juegos de mesa e ilustración."></figure>', html)
        html = html.replace('<a href="/events">Explorar eventos</a>', "", 1)
        html = re.sub(r'<a data-nav="events" href="/events">.*?</a>', lambda _: f'<a data-nav="join" href="/join/event"><svg class="ping-icon" aria-hidden="true" focusable="false"><use href="{STATIC}img/icons.svg#qr"></use></svg>Unirme a un evento</a>', html, count=1, flags=re.S)
    else:
        html = html.replace("</header>", "</header>" + local(nav), 1)
    html = html.replace("</head>", f"<style>{CSS}</style></head>", 1)
    (PROTO / f"propuesta-{name}.html").write_text(current(html, section), encoding="utf-8", newline="\n")
print(sorted(p.name for p in PROTO.glob("*.html")))
