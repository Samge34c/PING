# Claude Code en PING

@AGENTS.md

AGENTS.md es la fuente de reglas; este archivo solo añade lo propio de Claude Code. Contexto obligatorio: `docs/AI_CONTEXT.md`, `docs/PING_PREDEV_v1.1.md`, `docs/DECISION_LOG.md` y el Issue asociado; léelos una vez y busca por módulo con `rg`.

## Skills

Fuente única y versiones fijadas: `.agents/skills/` (registro `.agents/skill-installation.json`). `.claude/skills/` solo contiene punteros de texto para que Claude Code las descubra; no los edites ni copies contenido. Detalle, límites y comprobaciones: `docs/SKILLS_PING.md`.

Carga **solo la pertinente**, una vez:

| Necesidad | Skill |
|---|---|
| Organizar un trabajo de varios pasos | `ping-efficient-work` |
| Cambio estructural de una pantalla | `redesign-existing-projects` |
| Comportamiento táctil/móvil (hover, zoom de campos, áreas seguras) | `mobile-native` |
| Movimiento y feedback de interacción | `emil-design-eng` |
| Duda concreta de UX/accesibilidad | `ui-ux-pro-max` |
| Revisión visual final acotada, sobre capturas | `impeccable` |
| Defecto reproducible | `systematic-debugging` |
| Evidencia antes de afirmar «funciona/terminado» | `verification-before-completion` |

Reglas:

- Cuatro skills ligeras pueden cargarse solas por su descripción: `ping-efficient-work`, `mobile-native`, `systematic-debugging` y `verification-before-completion`. Las otras cuatro son de invocación manual (`/nombre` o lectura directa del `SKILL.md`).
- Si una skill no aparece, léela manualmente desde `.agents/skills/<nombre>/SKILL.md`; no bloquea el trabajo.
- Impeccable: hooks desactivados; no ejecutar `scripts/impeccable` (puede descargar un motor) ni sus comandos `hooks`, `live`, `init` o `pin`.
- No instalar plugins ni colecciones, no actualizar revisiones fijadas, no abrir subagentes por rutina, no repetir auditorías o suites sin cambios.
- PRODUCT.md manda sobre las pautas genéricas de las skills: Festival Social (negro, morado, amarillo), Manrope, recursos locales, datos reales.

## Trabajo visual

Las propuestas se entregan como prototipo aislado en `docs/design-proposals/`. Integrarlas en la app exige decisión registrada en `docs/DECISION_LOG.md` y aceptación de Samuel y Julián (AGENTS.md).
