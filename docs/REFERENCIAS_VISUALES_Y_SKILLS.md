# Referencias visuales y de skills — evaluación para PING

Fecha de consulta: 21 de octubre de 2026. Base: `ci/development-validation` @ `f2923b98ff893c22138ef6552859e5a323ffa7c3`. Complementa [SKILLS_PING.md](SKILLS_PING.md); no instala nada ni cambia el producto.

## Qué se pudo comprobar

- **Las 15 publicaciones no se pudieron abrir desde este entorno** (Instagram y TikTok no resuelven por DNS: `ENOTFOUND`). No se inspeccionó ningún vídeo, audio, subtítulo, descripción ni portada. Todo lo que sigue sobre su contenido procede del análisis adjunto («PING · Referencias y encargo visual», 21 oct.) y es **evidencia de segunda mano**: se cita, no se verifica.
- **Sí se consultaron fuentes oficiales**: repositorios de Impeccable, Taste, UI UX Pro Max, Superpowers y Matt Pocock; documentación de Claude Code (skills y costes). No se pudo abrir la página de Emil Kowalski (`emilkowal.ski`, mismo error de DNS); su procedencia se apoya en el repositorio y el commit fijados en `.agents/skill-installation.json`.
- Convención: **[C]** afirmación del creador (según el adjunto), **[V]** verificado hoy en fuente oficial, **[P]** propuesta mía.

## Hallazgos verificados que cambian decisiones

| Hallazgo [V] | Fuente | Consecuencia |
|---|---|---|
| Claude Code descubre skills de proyecto en `.claude/skills/` y admite enlaces simbólicos a otra carpeta; la documentación no menciona `.agents/skills/`. | [Skills](https://code.claude.com/docs/en/skills) | Se añadieron punteros en `.claude/skills/` (ver SKILLS_PING.md). Un directorio nuevo requiere `/reload-skills` o sesión nueva. |
| Los cuerpos de skills cargan al invocarse; CLAUDE.md se carga siempre y conviene mantenerlo corto (<200 líneas). Los MCP cargan sus definiciones de forma diferida. Equipos de agentes: ~7× más tokens con teammates en modo plan; elegir modelo según la tarea. | [Costes](https://code.claude.com/docs/en/costs) | Respalda CLAUDE.md breve, invocación manual de skills pesadas y no abrir subagentes por rutina. **No** respalda promesas de porcentaje. |
| Impeccable: 61 reglas deterministas del detector (sin LLM ni clave), el instalador de Claude Code añade un hook, el lanzador descarga un motor en `~/.impeccable/` y la skill consulta un servicio externo al iniciar un diseño nuevo. Apache-2.0. | [pbakaus/impeccable](https://github.com/pbakaus/impeccable) | «Sin tokens» describe el detector, no la revisión hecha por el modelo. Se mantiene en lectura manual, sin hooks ni ejecución del lanzador. |
| Taste publica ahora `design-taste-frontend`; `redesign-existing-projects` quedó integrada en ella; otras skills pasaron a repositorios «classics» e «imagegen». MIT. | [Leonxlnx/taste-skill](https://github.com/Leonxlnx/taste-skill) | PING conserva su revisión fijada; no se actualiza durante un trabajo visual. |
| UI UX Pro Max: MIT, búsqueda BM25 local con Python estándar y sin llamadas de red; existe un nivel de pago (no usado). | [nextlevelbuilder/ui-ux-pro-max-skill](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill) | Ya instalada; solo consulta puntual. |
| Superpowers: marco con flujos «obligatorios», bootstrap inyectado al inicio y subagentes por tarea como opción más completa y no la más barata; MIT. | [obra/superpowers](https://github.com/obra/superpowers) | No instalar el marco: PING ya conserva `systematic-debugging` y `verification-before-completion`. |
| `grill-me` y `grill-with-docs` existen en el repositorio de Matt Pocock (MIT). **`zoom-out` no figura** en la página consultada. | [mattpocock/skills](https://github.com/mattpocock/skills) | Reservar `grill-me` para otros proyectos; no atribuir `zoom-out` a ese repositorio. |

## Las 15 referencias

Decisión: **Ahora** (aprovechar en esta iteración), **Reservar** (otro proyecto o más adelante), **Descartar**. «Existe» = ya hay algo equivalente en PING.

| N.º · fuente | Qué menciona [C] | Fuente oficial / identificación | Aplicación en PING [P] | Existe | Coste · mantenimiento · contexto | Decisión |
|---|---|---|---|---|---|---|
| 1 · Seyva (IG) | Taste, UI UX Pro Max, referencias visuales, Impeccable, agentes de construcción/revisión | Tres herramientas [V]; el flujo de agentes no se identificó | Dirección artística concreta y revisión sobre capturas reales | Las tres skills | Los agentes multiplican contexto [V costes] | **Ahora**: método (capturas + revisión única). Sin agentes |
| 2 · Bertorello (IG, diap. 3) | Catálogo de sitios gratuitos; la diapositiva 3 no se obtuvo | No identificada | Catálogo de recursos con licencia para otros proyectos | — | Licencias y mantenimiento por recurso | **Reservar**. No atribuir herramientas por adivinación |
| 3 · Sebastián Rivera (IG) | Repartir tareas por dificultad, separar planificación/construcción; «mods» sin nombrar | No identificada | Elegir modelo y esfuerzo según la tarea | Plan mode, `/model` [V costes] | Sin instalación; más agentes ≠ menos tokens [V] | **Ahora**: práctica ya documentada; sin intermediarios |
| 4 · Lucas Acosta (IG) | «15 skills»; lista por comentario | No identificada | — | 8 skills de proyecto | 15 descripciones añadirían contexto | **Descartar** hasta conocer la lista |
| 5 · Iván (IG, publicidad Manus) | 10 tendencias; solo glassmorfismo identificado | Publicidad; no verificable | Superficie translúcida solo si mejora una capa concreta | `color-mix` y superficies del tema | `backdrop-filter` cuesta rendimiento en móviles modestos | **Descartar** ahora; no se usó en el prototipo |
| 6 · Iván (IG, publicidad) | 20 elementos previos al lanzamiento; solo 404 personalizada | Publicidad; no verificable | Errores con recuperación | `errors/message.html`, estados recuperables en DEC-047 | — | **Reservar**: revisar solo si se observa un defecto |
| 7 · Santa IA (IG) | Crossfade, elemento compartido, iconos al pasar el cursor, blur | Coincide con principios de `emil-design-eng` | Continuidad familia → subfamilia y feedback de selección en intereses | `emil-design-eng`, DEC-048 | CSS nativo; hover irrelevante en móvil | **Ahora**, solo transiciones breves y reducidas; sin blur |
| 8 · Esteban Pérez (IG) | Referencia animada + Emil, Impeccable, Taste; valoración monetaria promocional | Herramientas [V]; cifra no verificable | Comparar prototipo con una referencia concreta | Las tres skills | La valoración no es un retorno medido | **Ahora**: comparar con captura real; **Descartar** la cifra |
| 9 · Rocho (TikTok) | grill-me, Superpowers, UI UX Pro Max, «zoom-out» | grill-me [V]; Superpowers [V]; `zoom-out` **no confirmado** | Aclarar decisiones materiales antes de un cambio costoso | AGENTS.md exige acordar alcance; `systematic-debugging`, `verification…` | Superpowers completo: bootstrap + flujos obligatorios [V] | **Reservar** `grill-me`; **Descartar** instalar Superpowers |
| 10 · Exe Carrizo (TikTok) | Skill derivada de frontend-design con detector local | Coincide con Impeccable [V]; el nombre no está confirmado en subtítulos | Revisar composición y tarjetas anidadas | Impeccable (manual) | Detector gratis, crítica con LLM no [V]; motor se descarga | **Ahora** en lectura manual. No prohibir el morado de marca |
| 11 · Xabat (TikTok) | Convertir señales de escritura artificial en instrucciones de estilo | Sin herramienta | Copy breve y situado («escribe el código que te muestra la otra persona») | `redesign…` (contenido) | Sin coste | **Ahora** como criterio de copy en el prototipo; sin reglas absolutas |
| 12 · Rosso (IG) | «3 MCP» y «doble de rendimiento»; por comentario | No identificados | Un MCP solo si resuelve un acceso necesario | GitHub MCP y CLI ya disponibles | Definiciones diferidas [V]; CLI más barato que MCP [V costes] | **Descartar**; rendimiento no medido |
| 13 · Mark (IG) | «Tokens gratis»; por comentario | No identificado | — | — | Condiciones desconocidas; riesgo con el código | **Descartar**; no registrar ni instalar |
| 14 · SantIA (IG) | Evitar releer el repositorio; portada y descripción discrepan | No identificada | Búsqueda localizada, índice breve, continuidad con SHA | `ping-efficient-work`, CLAUDE.md, notas de continuidad | Una skill de visión general ahorra exploración [V costes] | **Ahora**: principio ya aplicado; **Descartar** herramienta |
| 15 · Juan Romero (IG) | Documento de bienvenida de cliente | No identificada | Orientación breve para organizadores | Docs de entrada por código/QR | — | **Reservar** (organizador / otros proyectos) |

## Qué sirve para PING, en pocas líneas

1. **Método**: capturas reales del recorrido → elegir dos pantallas con evidencia → prototipo aislado → una sola revisión final.
2. **Skills**: bastan las ocho actuales; no se añade ninguna nueva. La carencia real era de **descubrimiento en Claude Code**; se mitiga con punteros, pero el descubrimiento **sigue pendiente de verificación definitiva** (sesión nueva y `/nombre` del usuario; ver `docs/SKILLS_PING.md`).
3. **Copy situado y feedback de selección**: aplicados en el prototipo de identidad e intereses.
4. **Lo que no se adopta**: créditos «gratis», MCP sin nombre, plugins de colecciones completas, glassmorfismo global, subagentes por rutina.

## Pendientes de evidencia

Abrir los reels y TikToks desde un entorno con acceso (o aportar clips, diapositivas o transcripciones) para: la diapositiva 3 del carrusel, los nombres de «mods», las quince skills, los tres MCP, las condiciones de los «tokens gratis», la discrepancia del reel de SantIA y el movimiento real de las demostraciones.
