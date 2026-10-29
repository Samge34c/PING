# Cierre visual — Entrada editorial (DEC-063)

Encargo de Samuel del 29 de octubre de 2026: pulido visual de entrega con prioridad móvil. No se atribuye aprobación a Julián. Sin commit, push, merge ni despliegue.

## Estado (29 oct 2026, 12:45 Bogotá)

| Parte | Estado |
|---|---|
| Figma | **Hecho.** Página nueva «PING · Entrada editorial · Propuesta (29 oct 2026)», `156:317`: copias intactas de `133:1462`, `133:1497`, `133:1572`; propuesta móvil, escritorio, contraste y tablero de arte con motivos. Originales y comparaciones históricas sin tocar. |
| Arte y recursos | **Hecho.** `docs/design-proposals/entrada-editorial/` (copia principal, sin rastrear): fuentes, variantes WebP, procedencia, herramientas, prototipo y capturas. |
| Integración en código | **Preparada, no aplicada.** La escritura en `.worktrees/perf-closure-current` fue bloqueada por el control de permisos (recurso compartido); el worktree de Codex quedó intacto (hash de su diff `1369a6d7…` antes y después). Falta decidir dónde integrar (ver «Siguiente paso»). |

## Base comprobada

- Producción (`https://ping-b63o.onrender.com/`, HTML del 29 oct) = `deploy/render-demo@3a528ca`. Hallazgos presentes: `/events` → 302 `/login`; `.topbar-note` `#BDB5CA` sobre `#F8E836` (1,56:1, visible desde 1024 px); barra de visitante solo en Inicio.
- Rendimiento: `perf/closure-current` sobre `3a528ca`, **sin commit**, en `.worktrees/perf-closure-current`. `CIERRE_RENDIMIENTO.md` (sin rastrear allí) libera `home.html`, `layout.html`, `brand.css` e `interests/selector.html` el 29 oct a las 12:20:38. La consulta Tandem (una, `job-43a585a9`) llegó antes de esa liberación y no habló con la sesión de Codex.

## Qué cambia (presentación)

Detalle y motivos en `docs/design-proposals/entrada-editorial/README.md`; decisión preparada en `integracion/DEC-063.md`.

- `templates/home.html`: arte nuevo con `srcset` 768/1152/1400 y `sizes="(min-width: 768px) 566px, calc(100vw - 32px)"`; se retira «Explorar eventos».
- `templates/fragments/layout.html`: barra lateral, visitante → «Unirme a un evento» (`/join/event`); «Eventos» solo con participación u organización.
- `templates/entry/event.html`, `account/login.html`, `account/register.html`: fragmentos `bottomNav` + `moreMenu` tras la cabecera.
- `static/js/ui.js`: `/register` marca la pestaña «Entrar»; `.entry-topbar` entra en la lista `inert` del menú.
- `static/css/v2.css`: bloque final de `integracion/entrada-editorial.css` (10 líneas).
- `static/img/sofa/encuentro-editorial-{768,1152,1400}.webp` nuevos (100 448 / 160 342 / 192 982 B). `convencion-social*.webp` se conservan (Hub y `EventExperienceTests`).

`brand.css` e `interests/selector.html` no se tocan. Intereses no recibe barra.

## Comprobaciones hechas

- `git apply --check`: `integracion/entrada-editorial.patch` sobre `3a528ca` y `integracion/entrada-editorial-sobre-cierre.patch` sobre el estado actual de Codex. `tools/aplicar_entrada.py` probado en copias de `3a528ca`, `b54aaae` y del cierre; conserva CRLF y se detiene si un ancla no aparece exactamente una vez.
- Copia temporal del cierre + visual: `bootstrap_subset.py --check` al día antes y después (no hay que regenerar). Pruebas afectadas: ver «Resultado de pruebas».
- Prototipo con HTML real de producción y CSS local, Chromium emulado por DevTools: 390 primero, 360, ~300 (equivalente a texto 130 %), 1280, tema claro, foco por teclado, campo enfocado, imagen bloqueada. Capturas en `docs/design-proposals/entrada-editorial/capturas/` (móvil 01–15, escritorio 16–17).
- Revisión de la captura móvil: la pantalla y su acción se entienden; el arte va después de título, explicación, CTA de 52 px y acceso; la barra no tapa contenido (relleno medido por `ui.js`); identidad Festival Social y marca «P en común» conservadas.

## Resultado de pruebas

Copia temporal completa del cierre de Codex con el cambio visual aplicado (fuera de cualquier worktree), `./mvnw -o test`, perfil de pruebas del repositorio, 29 oct 12:30: **92 pruebas, 0 fallos/errores/omitidas** — `AccountSecurityTests` 23, `OAuthLoginFlowTests` 10, `EventExperienceTests` 28, `EventEntryFlowTests` 5, `EventTests` 23, `FrontendBundleTests` 1, `PublicResourceTests` 2. No se repitió la suite completa (358 de Codex): el cambio es de plantillas, CSS y un mapeo de `ui.js`. Tras integrar en el árbol real, repetir solo estas clases.

## Siguiente paso (decisión de Samuel)

1. **Integrar en el worktree de cierre** (lo que propone `CIERRE_RENDIMIENTO.md`, «siguiente paso para Claude»): autorizar la escritura en `.worktrees/perf-closure-current` y ejecutar desde la copia principal
   `python -I docs/design-proposals/entrada-editorial/tools/aplicar_entrada.py .worktrees/perf-closure-current`.
   Después: añadir `integracion/DEC-063.md` al `DECISION_LOG.md` de ese árbol, actualizar `docs/HOME_ART_PROVENANCE.json` (asset → `encuentro-editorial-*`, remitir a `PROCEDENCIA.json`) y la sección «Home» de `DESIGN.md` (ya no hay «explorar eventos»), y repetir solo las pruebas afectadas. Separación: diff de Codex = instantánea `1369a6d7…`; visual = `entrada-editorial-sobre-cierre.patch` + 3 WebP.
2. **O esperar** a que el cierre de rendimiento tenga commit/PR, crear `design/entrada-editorial` desde esa base y ejecutar el mismo script.

Los recursos del prototipo (`docs/design-proposals/entrada-editorial/`) deben acompañar a la PR; decidir si las fuentes PNG (2,5 MB c/u) se versionan o se guardan fuera de Git.

## Pendientes y límites

- No probado en iPhone/Safari ni con teclado o áreas seguras reales; emulación Chromium únicamente.
- El `viewport` no declara `viewport-fit=cover`, así que `env(safe-area-inset-bottom)` vale 0 (comportamiento previo, en `layout.html`). Sin cambio aquí.
- Orden de tabulación: la barra inferior precede al contenido en todas las pantallas (patrón previo, con «Saltar al contenido»); en Unirme/Entrar se mantiene igual por coherencia.
- Inicio y Hub quedan con ilustraciones de estilo distinto (el Hub se conserva por instrucción).
- `PagePresentation` sigue tratando `/events` como página pública para metadatos; fuera de alcance visual.
- Aceptación humana de DEC-063 pendiente; sin aprobación atribuida a Julián.
