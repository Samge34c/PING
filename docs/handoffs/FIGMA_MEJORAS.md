# Handoff de diseño · Mejoras aprobadas de PING (Figma → Codex) · 29 oct 2026

Ruta: `C:\Users\river\PING-movil\docs\handoffs\FIGMA_MEJORAS.md` (copia principal; no forma parte del worktree de Codex).

Diseño de Claude en Figma; **nada está implementado**. Codex es el único escritor de código de este incremento. Las pantallas usan datos ficticios de ejemplo. No se atribuye aprobación a Julián. Antes de programar 2–6, registrar la decisión en `docs/DECISION_LOG.md` (AGENTS.md): esas funciones no tienen modelo de datos previo en el repositorio.

Archivo Figma `WCrjKj5eGOZQ8wUaxsEqfc`, página **«PING · Mejoras aprobadas · Diseño (29 oct 2026)» `161:262`**. Los originales de «PING · Selección final para implementación» (`133:262`) siguen intactos. Cada sección tiene un panel con estados, componentes, campos, comportamiento y decisiones; el índice está en la cabecera `161:263`.

## Decisiones ya tomadas

- **Pista:** se retira guardando el campo vacío. No hay botón «Quitar»: Guardado → Editar → vaciar → Guardar → Vacío. Se mantienen campos, límites y errores actuales.
- **Tarjeta descargable:** solo datos propios: alias, avatar V3, evento, encuentros completados, puntos y hasta 3 gustos. Nunca correo, códigos, palabra, pista, otras personas, plano ni ubicación.
- **Agenda:** usa los permisos existentes. La gestiona quien ya gestiona el evento; los participantes solo leen.
- **Retos voluntarios:** sin puntos extra. La recompensa es descriptiva y no toca matching, puntos, canjes ni Saga.

## Índice de nodos

| # | Pantallas (390 px) | Especificación |
|---|---|---|
| 1 · **Pista en Mi Ping (prioridad)** | Vacío `161:269` · Guardado `161:425` · Edición `161:535` · Error 400 `161:669` · Conflicto 409 `161:822` | `169:1452` |
| 2 · Preparación guiada del evento | En curso `164:860` · Lista para abrir `164:941` | `164:1025` |
| 3 · Ayuda contextual | Cerrada `164:1038` · Abierta `164:1081` | `164:1131` |
| 4 · Tarjeta descargable | Lista `166:1072` · Sin conexiones/generando/error `166:1191` | `166:1280` |
| 5 · Agenda | Con actividades `166:1300` · Vacía `166:1409` | `166:1473` |
| 6 · Retos voluntarios | En curso `166:1486` · Completado `166:1562` | `166:1645` |

**Componentes locales nuevos**, revisados con `figma-generate-library`:
- `Pista · Sugerencia` `172:1454`. Variantes `Estado=Normal|Pulsado`, con el patrón de pulsado de V2 · Botón. Propiedad de texto «Texto». Estilo `V2/Cuerpo fuerte`; relleno `v2/fondo/elevado` (`v2/fondo/superficie` al pulsar); borde `v2/borde/control`. Alto mínimo de 44 px.
- `Agenda · Actividad` `173:1483`. Variantes `Estado=Próxima|Ahora|Pasada`; el estado se indica con barra o atenuado y además con un rótulo («Ahora», «Terminada»). Propiedades de texto Hora, Título y Lugar; propiedad booleana «En común», que muestra `V2 · En común` anidado con su texto editable. Estilos `V2/Control`, `V2/Cuerpo fuerte`, `V2/Ayuda` y `V2/Etiqueta`; colores con variables V2.

**Componentes V2 reutilizados:** Mi Ping V3 `133:1777`, V2 · Botón `40:471`, Campo `40:503`, Aviso `40:532`, Paso de misión `42:748`, Cabecera `42:718`, Navegación `42:686`, En común `41:444`, Avatar V3 `67:575`, Marca C · Horizontal `39:311`, Unirme propuesto `156:500`.

## Base en código para la pista (cambio de presentación)

Ya existen `RecognitionHintForm` (hasta 120 puntos de código, `version`), `POST /me/recognition-hint`, `recognition/fragments.html :: editor` y `recognition-hint.js`. El cambio es que la tarjeta queda siempre visible tras «Tu avatar» en lugar del `<details>` plegado. Se mantienen campos, textos de error del servidor y la regla de lectura ajena (solo en ACTIVE con ambas aceptaciones).

## Pendientes reales

1. **Pista:** si «Añadir pista» edita en la propia tarjeta (lo propuesto) o abre `/me/recognition-hint`.
2. **Preparación guiada:** qué pasos son obligatorios para abrir (1–3 es solo un ejemplo), y si la guía sustituye o acompaña al detalle actual del evento.
3. **Ayuda contextual:** en qué pantallas va (propuesta: Unirme, pista, llegada/verificación, canje) y los textos definitivos.
4. **Tarjeta:** si se genera en el navegador o en el servidor; si se permite con 0 encuentros; si «Compartir» entra en este incremento; acceso desde Mi Ping o desde «Más».
5. **Agenda:** modelo de datos y editor de la organización (no diseñados); enlace al plano o punto; relación con las actividades de encuentro.
6. **Retos:** catálogo y quién lo define; ubicación (Mi Ping o Evento); mostrar u ocultar por persona.

Fuera de este handoff, sin cambios desde la entrega anterior: en `.worktrees/perf-closure-current` (DEC-063, sin commit) faltan por regenerar los dos `.patch` de `docs/design-proposals/entrada-editorial/integracion/` y por redactar la sección «Integración visual DEC-063» de `CIERRE_RENDIMIENTO.md`.
