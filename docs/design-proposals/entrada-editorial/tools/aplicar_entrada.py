"""Aplica la propuesta «Entrada editorial» a un árbol de PING (propuesta; no se ejecuta sobre main).

Uso, desde la raíz del árbol destino ya actualizado con el cierre de rendimiento:
    python -I docs/design-proposals/entrada-editorial/tools/aplicar_entrada.py [raíz]
Cada sustitución exige encontrar exactamente un ancla; si la base cambió, se detiene sin escribir nada
para que la integración se haga a mano sobre el texto vigente. Las variantes WebP se copian a img/sofa/.
"""
import pathlib
import re
import shutil
import sys

ROOT = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else ".").resolve()
HERE = pathlib.Path(__file__).resolve().parents[1]
T = "src/main/resources/templates/"
NAV = '<nav th:replace="~{fragments/navigation :: bottomNav}"></nav><div th:replace="~{fragments/navigation :: moreMenu}"></div>'
HEADER = '<header th:replace="~{entry/interests :: topbar}"></header>'
ART = ('<figure class="fe-art"><img th:src="@{/img/sofa/encuentro-editorial-1152.webp}" '
       "th:srcset=\"@{/img/sofa/encuentro-editorial-768.webp} + ' 768w, ' + @{/img/sofa/encuentro-editorial-1152.webp} + ' 1152w, ' + @{/img/sofa/encuentro-editorial-1400.webp} + ' 1400w'\" "
       'sizes="(min-width: 768px) 566px, calc(100vw - 32px)" width="1400" height="940" fetchpriority="high" decoding="async" '
       'alt="Ilustración de dos personas que se ríen al descubrir un gusto en común en un evento con música, juegos de mesa e ilustración."></figure>')
CSS = (HERE / "integracion" / "entrada-editorial.css").read_text(encoding="utf-8")
EDITS = {
    T + "home.html": [
        (re.compile(r'<figure class="fe-art"><img [^>]*></figure>'), ART),
        (re.compile(r'<a th:href="@\{/events\}">Explorar eventos</a>'), ""),
    ],
    T + "fragments/layout.html": [
        (re.compile(r'<a data-nav="events" th:href="\$\{session\.pingDemoParticipantId != null\} \? @\{/me/event\} : @\{/events\}">.*?</a>'),
         '<a data-nav="events" th:if="${session.pingDemoParticipantId != null or isOrganizer == true}" th:href="${session.pingDemoParticipantId != null} ? @{/me/event} : @{/events}">'
         '<svg th:replace="~{fragments/layout :: icon(\'calendar\')}"></svg><th:block th:text="${session.pingDemoParticipantId != null} ? \'Evento\' : \'Eventos\'"></th:block></a>'
         '<a data-nav="join" th:if="${session.pingDemoParticipantId == null and isOrganizer != true}" th:href="@{/join/event}">'
         '<svg th:replace="~{fragments/layout :: icon(\'qr\')}"></svg>Unirme a un evento</a>'),
    ],
    T + "entry/event.html": [(re.compile(re.escape(HEADER)), HEADER + NAV)],
    T + "account/login.html": [(re.compile(re.escape(HEADER)), HEADER + NAV)],
    T + "account/register.html": [(re.compile(re.escape(HEADER)), HEADER + NAV)],
    "src/main/resources/static/js/ui.js": [
        (re.compile(re.escape("path.startsWith('/login') ? 'login'")), "path.startsWith('/login') || path.startsWith('/register') ? 'login'"),
        (re.compile(re.escape("'main,.ping-topbar,.ping-sidebar,")), "'main,.ping-topbar,.entry-topbar,.ping-sidebar,"),
    ],
}

def read(rel):  # conserva el final de línea del árbol destino (CRLF en checkouts de Windows)
    return (ROOT / rel).read_bytes().decode("utf-8")


pending = {}
for rel, edits in EDITS.items():
    text = read(rel)
    for pattern, replacement in edits:
        hits = len(pattern.findall(text))
        if hits != 1:
            sys.exit(f"{rel}: el ancla {pattern.pattern[:60]!r} aparece {hits} veces; integra a mano.")
        text = pattern.sub(lambda _: replacement, text)
    pending[rel] = text
css_rel = "src/main/resources/static/css/v2.css"
css_text = read(css_rel)
eol = "\r\n" if "\r\n" in css_text else "\n"
pending[css_rel] = css_text.rstrip("\r\n") + eol + CSS.strip("\n").replace("\n", eol) + eol
for rel, text in pending.items():
    (ROOT / rel).write_bytes(text.encode("utf-8"))
for width in (768, 1152, 1400):
    shutil.copy2(HERE / "variantes" / f"encuentro-editorial-{width}.webp", ROOT / f"src/main/resources/static/img/sofa/encuentro-editorial-{width}.webp")
print("Aplicado:", ", ".join(sorted(pending)))
