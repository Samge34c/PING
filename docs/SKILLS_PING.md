# Selección de skills de PING

Registro de configuración del 16 de octubre de 2026. Copia: `/workspace/work/ping-verificacion-final-mvp`; rama `reconstruction/festival-social`, base `e2759ae672adc60235b2a3c492c48a370724d544`.

Conservación posterior autorizada por el usuario: commit y push de configuración/skills/documentación únicamente a `reconstruction/festival-social`. Las menciones a «sin commit/push» siguientes describen la tarea original de instalación. El estado de publicación se comprueba en Git, no mediante este texto; no se autoriza merge ni publicación de datos o cachés.

**Seleccionar skills según la tarea; no cargar todas por defecto.** Esta selección no autoriza rediseñar, cambiar producto, instalar herramientas o alterar el stack. Mandan las instrucciones del usuario, AGENTS.md y PRODUCT.md.

## Selección exacta y rutas

Todas las rutas son de proyecto, relativas a la raíz indicada arriba. Hay exactamente seis directorios con SKILL.md en `.agents/skills/`.

| Skill | SKILL.md local | Activación y ejemplo | Cuándo no usarla |
|---|---|---|---|
| impeccable | [.agents/skills/impeccable/SKILL.md](../.agents/skills/impeccable/SKILL.md) | Crítica visual, jerarquía, composición o pulido de una pantalla; revisar ACTIVE con capturas reales si la app funciona. | Backend, Git o MySQL sin trabajo visual. No ejecutarlo por rutina al terminar cada comando. |
| ui-ux-pro-max | [.agents/skills/ui-ux-pro-max/SKILL.md](../.agents/skills/ui-ux-pro-max/SKILL.md) | Pregunta concreta de interacción, navegación, formularios, accesibilidad o patrones. Ejemplo: resumen de errores de un formulario. Buscar localmente y leer solo los resultados pertinentes. | No elegir un estilo predeterminado, migrar stack ni generar un sistema visual completo para una duda puntual. |
| redesign-existing-projects | [.agents/skills/redesign-existing-projects/SKILL.md](../.agents/skills/redesign-existing-projects/SKILL.md) | Cambio estructural de una pantalla existente; por ejemplo, reorganizar Hub SOFA conservando sus destinos y contenido. También llamado Taste en las conversaciones. | Un ajuste pequeño de CSS; cambios de backend; reemplazar identidad o información real por recetas genéricas. |
| systematic-debugging | [.agents/skills/systematic-debugging/SKILL.md](../.agents/skills/systematic-debugging/SKILL.md) | Fallo reproducible, prueba fallida, arranque o integración inesperados. Ejemplo: JDBC rechaza una conexión MySQL. Leer error/diff, localizar frontera, formular y probar una hipótesis. | No hacer una auditoría completa por una tarea trivial ni variar soluciones sin evidencia nueva. |
| verification-before-completion | [.agents/skills/verification-before-completion/SKILL.md](../.agents/skills/verification-before-completion/SKILL.md) | Antes de afirmar que algo funciona, se corrigió o terminó. Ejemplo: validar manifiesto, licencias y rutas para afirmar instalación. | No usarla como mandato para repetir suites sin cambios ni para confundir un HTTP 200 con un flujo completo. |
| ping-efficient-work | [.agents/skills/ping-efficient-work/SKILL.md](../.agents/skills/ping-efficient-work/SKILL.md) | Exploración de código, múltiples comandos o trabajo por pasos. Acotar búsquedas, conservar logs y estado, reutilizar servicios. | Pregunta sencilla sin investigación; nunca una excusa para omitir requisitos o evidencia. |

## Combinación sin duplicación

- Un rediseño estructural puede usar Redesign/Taste para guiar el cambio e Impeccable para revisar el resultado, sin repetir dos auditorías idénticas.
- UI UX Pro Max se añade solo ante una duda concreta. El resultado se revisa por pertinencia: una coincidencia del buscador no es automáticamente una recomendación aplicable.
- Un fallo durante el trabajo activa systematic-debugging; la comprobación final de lo afirmado usa verification-before-completion.
- ping-efficient-work organiza tareas de varios pasos; no fuerza a cargar las otras cinco.
- Cargar únicamente la referencia necesaria, no toda la colección ni otras skills por menciones internas. No activar subagentes para ahorrar tokens.

## Procedencia, versiones, licencias y dependencias

Los commits completos, hashes de SKILL.md y detalles de instalación están en [.agents/skill-installation.json](../.agents/skill-installation.json). Se conservan las licencias junto a cada skill.

| Skill | Fuente original y revisión fijada | Licencia | Dependencias o referencias externas |
|---|---|---|---|
| impeccable | [pbakaus/impeccable](https://github.com/pbakaus/impeccable/tree/5bf0a069dc857e1b5f4b090e004d9d5b231416ca), `5bf0a069dc85`; skill 4.6.0, motor probado 0.1.15 | Apache-2.0; NOTICE.md conserva atribución MIT a ehmo para referencias nativas | Launcher de shell y motor nativo. `curl`/`wget` y red solo si falta el motor. No necesita Node para el uso manual instalado. Algunos flujos mencionan generación de imágenes, navegador, subagentes y herramientas de otros harnesses: no son capacidades concedidas por instalar la skill. |
| ui-ux-pro-max | [nextlevelbuilder/ui-ux-pro-max-skill](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill/tree/56ea9cf174ed86786187daa9de0e6247cb0747e4), `56ea9cf174ed` | MIT, Next Level Builder | Python 3 y datos/scripts locales; la consulta probada no necesita paquetes Python externos. Los ejemplos con rutas de plugin se sustituyen por la ruta local real, sin cambiar SKILL.md. |
| redesign-existing-projects | [Leonxlnx/taste-skill](https://github.com/Leonxlnx/taste-skill/tree/f624f37a8c650b9cc550253104925efe4c8de246), `f624f37a8c65`, subcarpeta `skills/redesign-skill` | MIT, Leonxlnx | Instrucciones sin CLI ni dependencias de ejecución propias. Usa evidencia y herramientas del proyecto existente. |
| systematic-debugging | [obra/superpowers](https://github.com/obra/superpowers/tree/bb92a77741419a4ab5f06e711a283343f1ada0c3), `bb92a7774141`, `skills/systematic-debugging` | MIT, Jesse Vincent | Instrucciones y tres referencias locales. Menciona test-driven-development, ausente, y verification-before-completion, disponible manualmente. Helper Bash orientado a npm/ejemplo TypeScript: incluidos como upstream, sin ejecutar ni validar para PING. |
| verification-before-completion | [obra/superpowers](https://github.com/obra/superpowers/tree/bb92a77741419a4ab5f06e711a283343f1ada0c3), `bb92a7774141`, `skills/verification-before-completion` | MIT, Jesse Vincent | Instrucciones sin CLI propio; necesita la comprobación real adecuada a cada afirmación. No requiere instalar la colección Superpowers. |
| ping-efficient-work | Creada para este proyecto por petición del usuario, versión 1.0.0 | MIT, Proyecto PING | `rg`, lectura, Git y las herramientas que ya requiera la tarea. Sin referencias ni subskills obligatorias. |

No se atribuyen a PING las métricas de éxito o ahorro de los ejemplos upstream. No se ha medido ahorro de tokens.

## Integración local y diferencias respecto a upstream

Los SKILL.md externos se conservan byte por byte. Las nuevas notas [systematic-debugging/PING_NOTES.md](../.agents/skills/systematic-debugging/PING_NOTES.md) y [verification-before-completion/PING_NOTES.md](../.agents/skills/verification-before-completion/PING_NOTES.md) identifican la adaptación de integración: límites, rutas manuales y selección proporcional de evidencia. No son otras skills.

- La referencia `superpowers:verification-before-completion` se resuelve en la ruta de esta selección, no mediante un plugin supuesto.
- `superpowers:test-driven-development` no está instalado. No se añadirá en cadena. Usar una reproducción mínima pertinente con las herramientas ya autorizadas; si una tarea futura necesita esa skill específicamente, informar de su ausencia.
- El helper `find-polluter.sh` usa `npm test`, oculta su salida y tolera fallos. No ejecutarlo en PING ni sustituir Maven por npm para satisfacer una referencia. Su dependencia no es un bloqueo para leer y aplicar el método de debugging.
- El flujo formal `impeccable critique` incluye requisitos propios de subagentes y consentimiento en Codex. Las definiciones anidadas que ya venían con Impeccable no se consideran herramientas registradas ni autorización para iniciarlas. La configuración no activa ese flujo. Distinguir una revisión manual de su ejecución completa.
- Las pautas genéricas de color, tipografía y tarjetas se subordinan a PRODUCT.md: negro/morado/amarillo, datos reales y funcionalidad preservada.

## Instalación, descubrimiento y comprobación separados

| Skill | Instalación en disco | Descubrimiento automático por esta sesión | Comprobación real de esta tarea |
|---|---|---|---|
| impeccable | Ya existía; no reinstalada | No enumerada por skills.list del executor | Hash conservado; `engine-probe` exit 0 (`impeccable-engine 0.1.15`); `hooks status` exit 0, disabled. No se lanzó auditoría visual. |
| ui-ux-pro-max | Ya existía; no reinstalada | No enumerada | Hash conservado; búsqueda `error summary validation --domain ux -n 1` exit 0, un resultado pertinente de formulario/accesibilidad. No se aplicó al código. |
| redesign-existing-projects | Ya existía; no reinstalada | No enumerada | SKILL.md legible y hash conservado; revisión de activación. No se inventa ejecución de un CLI inexistente. |
| systematic-debugging | Copiados solo SKILL.md, referencias de soporte, licencia y nota local | No enumerada | Integridad contra el commit upstream, lectura y caso de selección MySQL. No se provocó un fallo ni se afirma haber depurado la app en esta tarea. |
| verification-before-completion | Copiados SKILL.md, licencia y nota local | No enumerada | Integridad y lectura; aplicado para verificar la configuración antes de informar. |
| ping-efficient-work | Creada localmente, breve, con licencia | No enumerada | Lectura manual y comprobación de los requisitos de la petición; aplicada a esta configuración de varios pasos. |

`skills.list` con autoridad executor devuelve **0 skills**, incluso después de instalar. La instalación no demuestra activación automática. Se verifican lectura manual y herramientas existentes; no se reinicia el entorno para forzar descubrimiento. El catálogo de skills cloud de la sesión es distinto de esta selección de proyecto y no se modifica.

Las tres instalaciones previas eran las únicas skills de proyecto encontradas; no se borró ni habilitó ninguna adicional. Los clones upstream en `/workspace/work/skill-sources/` son material de inspección fuera de la instalación, no colecciones activadas.

### Casos pequeños de selección (revisión manual)

| Caso sin modificar PING | Selección comprobada | No activar por defecto |
|---|---|---|
| Reorganizar estructuralmente el Hub SOFA existente | redesign-existing-projects; Impeccable para una revisión posterior distinta | Debugging si no hay fallo; búsqueda UX si no hay duda concreta |
| Duda: cómo presentar errores de validación accesibles | ui-ux-pro-max, consulta puntual probada | Redesign completo o composición |
| Error reproducible de conexión MySQL | systematic-debugging: error sanitizado → configuración presente/ausente → frontera → hipótesis; verification antes de decir resuelto | Skills visuales; reiniciar/borrar la base; simular un fallo para este caso |
| Afirmar que están instaladas las seis | verification-before-completion: inventario, nombres, rutas, hashes, licencias y estado Git | Suite Java, capturas nuevas o una comprobación HTTP como sustituto de integridad |

Estos casos comprueban la elección de instrucciones, no un router automático ni que se hayan ejecutado cambios visuales o una depuración MySQL.

## Uso manual mínimo

Desde la raíz del proyecto, leer `.agents/skills/<nombre>/SKILL.md` y, para Superpowers, su PING_NOTES.md. No cargar todas las rutas en bloque para una tarea normal.

Consulta UX concreta:

```sh
python .agents/skills/ui-ux-pro-max/scripts/search.py 'error summary validation' --domain ux -n 1
```

Para un trabajo visual pertinente, usar las referencias de Impeccable que correspondan y sus capturas reales. El motor actual está en una caché de proyecto; seleccionar esa caché para no escribir en el home:

```sh
IMPECCABLE_HOME="$PWD/.agents/skills/impeccable/.runtime" .agents/skills/impeccable/scripts/impeccable engine-probe
```

No confundir este probe con una auditoría visual. No ejecutar `context`, `detect`, `critique` o un flujo de composición en cada tarea de Git/backend.

## Restauración y conservación en otra copia

1. La instalación sigue **solo local** mientras estos archivos no se incorporen a un commit y se publiquen con autorización. En una tarea futura, revisar y versionar los seis directorios de `.agents/skills/` (con sus licencias/notas), `.agents/.gitignore`, `.agents/skill-installation.json`, este documento y la referencia breve en AGENTS.md. Conservar también PRODUCT.md y `.impeccable/config.json` ya preparados. Esta tarea no hace commits, push ni merge.
2. Cuando estén en GitHub, obtenerlos de la rama/commit autorizado. Conservar `hook.enabled: false`; no copiar manifests de hooks de instaladores externos. No cambiar los hooks Git de la otra copia. Las cachés, credenciales, estados de navegador, logs y volúmenes MySQL no pertenecen al paquete de skills.
3. Si falta una copia externa, usar el commit completo y `source_directory` del manifiesto: clonar la fuente en un directorio temporal fuera del checkout, fijar ese commit y copiar **solo la carpeta indicada** a `.agents/skills/<nombre>`. Para Superpowers copiar únicamente `upstream_files`, sin CREATION-LOG, tests históricos, otras skills o instaladores globales; LICENSE se obtiene de la raíz upstream. `installed_files` describe el resultado e incluye las notas propias, que se restauran desde PING. Conservar LICENSE y NOTICE cuando corresponda. Comprobar el SHA-256 antes de declararla restaurada.
4. `ping-efficient-work` es propia: restaurarla desde el commit de PING que la contenga; no buscarla en un catálogo externo. Las notas de integración y el manifiesto también deben restaurarse desde PING.
5. El binario/cache de Impeccable no se versiona; si falta, su launcher puede obtener el motor fijado en scripts/VERSION con autorización de red y caché local, o se puede proporcionar el binario oficial compatible. Declarar cualquier bloqueo de red/arquitectura. Python 3 y `rg` deben comprobarse, no darse por instalados por tener SKILL.md.
6. Verificar nombres/rutas/licencias, lectura manual, una consulta UX pertinente y motor/hooks. Consultar skills.list si está disponible; si no las enumera, registrar la limitación y usar lectura manual. No reiniciar el entorno ni modificar configuración interna de Codex para aparentar activación.

## Límites de esta configuración

- Impeccable conserva hooks desactivados; no se reinstalaron sus hooks. Los demás hooks/configuraciones de Git permanecen como estaban al comenzar esta tarea.
- No se ejecutan las 140 pruebas Java históricamente aprobadas: esta tarea no cambia código. Esos resultados corresponden a la base `e2759ae`, no a una ejecución nueva de hoy.
- No se modifica backend, MySQL, datos, recursos ni demo; no se reconstruye Festival Social ni se recupera el ZIP de la tarea anterior.
- La selección expresa instrucciones disponibles, no herramientas nuevas: las skills de texto no añaden APIs, navegador, descarga de archivos, subagentes o acceso entre tareas.


## Ampliación móvil DEC-048 — 19 octubre2026

Las seis skills anteriores conservan sus hashes, licencias y configuración. Ahora hay **ocho** skills de proyecto, incluidas únicamente estas dos instrucciones de [emilkowalski/skills](https://github.com/emilkowalski/skills/tree/2d228630ba1446365f614a108cb8252f1feda202), revisión2d228630ba1446365f614a108cb8252f1feda202, licencia MIT/Emil Kowalski2026 copiada de la raíz oficial en cada carpeta:

| Skill local | Usar para | Límites de integración |
|---|---|---|
| [mobile-native](../.agents/skills/mobile-native/SKILL.md) | Hover según capacidad, inputs, pulsación, viewport/áreas seguras de una superficie móvil concreta. | Sin sniffing, bloqueo de zoom o gestos globales. PING conserva scroll nativo; no aplicar el reset de overscroll por rutina. Emulación no demuestra Safari/teclado/notch reales. |
| [emil-design-eng](../.agents/skills/emil-design-eng/SKILL.md) | Respuesta inmediata de componentes y movimiento breve que comunique estado. | Antes/después en tabla y evidencia de movimiento. CSS nativo; ejemplos React/Motion/Expo no autorizan dependencias. Sin efectos decorativos globales ni acciones retrasadas. |

Ambas contienen solo SKILL.md y LICENSE, sin ejecutables ni dependencias necesarias. No se instaló el resto de la colección ni se ejecutó instalador global. Manifiesto conserva fuente, commit y SHA-256. Lectura manual completa: executor skills.list continúa sin enumerarlas; esto no bloquea su uso.

Selección del bloque: ping-efficient-work organiza, mobile-native/emil-design-eng guían las correcciones concretas; Impeccable revisa capturas manualmente (motor no disponible, hooks siguen desactivados); verification exige evidencia. Redesign y UX se reservan a dudas estructurales/de interacción; systematic-debugging se activa solo ante fallos reproducibles. No se encadenan otras skills citadas. La exigencia upstream de hardware se registra como pendiente real de iPhone, según el alcance explícito del usuario; no se declara esa prueba por usar Chromium táctil.


## Integración con Claude Code — 21 octubre 2026

Autorización expresa del usuario de la sesión de Claude Code, 21 octubre 2026: preparar las skills pertinentes con alcance de proyecto en la rama `claude/sweet-ritchie-ja97c7`, creada desde `ci/development-validation` @ `f2923b98ff893c22138ef6552859e5a323ffa7c3` (sin avances posteriores comprobados en el remoto; PR #50 abierta en borrador). No se atribuye aprobación a ningún integrante concreto; la aceptación de Samuel y Julián sigue pendiente.

**Estado: preparada; el descubrimiento NO está verificado de forma definitiva.** La verificación en una sesión nueva sigue **pendiente** (ver «Cómo verificarlo»). Lo observado hasta ahora es parcial y se detalla abajo.

**Qué se añadió.** `CLAUDE.md` (breve: importa AGENTS.md y fija la regla de selección) y `.claude/skills/<nombre>/SKILL.md` para las ocho skills. Cada archivo es un **puntero de texto** de unas diez líneas con `name`, una `description` breve y la ruta de la fuente fijada en `.agents/skills/`. No copia contenido, scripts, referencias ni licencias; no cambia revisiones. Motivos: Claude Code descubre `.claude/skills/` y documenta que una entrada puede ser un enlace simbólico a otra carpeta ([documentación oficial](https://code.claude.com/docs/en/skills)), pero el repositorio no usa enlaces simbólicos y el equipo trabaja también en Windows, donde Git puede dejarlos como archivos de texto; una copia duplicaría 6 MB y abriría una divergencia de versiones. `.agents/skills/` queda intacta para Codex.

**Invocación.** Automática (por descripción) solo en `ping-efficient-work`, `mobile-native`, `systematic-debugging` y `verification-before-completion`. Manual (`disable-model-invocation: true`) en `redesign-existing-projects`, `emil-design-eng`, `ui-ux-pro-max` e `impeccable`, para que sus descripciones no se carguen por defecto y no se encadenen. No se midió el ahorro de contexto de esta decisión.

**Impeccable.** Hooks desactivados (`.impeccable/config.json`, `hook.enabled: false`); no se creó `.claude/settings*.json`. Según su repositorio oficial, el instalador de Claude Code añade un hook en `.claude/settings.local.json`, el lanzador descarga un motor en `~/.impeccable/` y la skill consulta un servicio externo al iniciar un diseño nuevo; por eso su puntero prohíbe ejecutar `scripts/impeccable` y los comandos `hooks`, `live`, `init` y `pin`. El motor no está instalado y no se descargó.

### Observado en la sesión de trabajo (21 octubre 2026) — parcial

| Momento | Observación |
|---|---|
| Justo tras crear los punteros, en la misma sesión | La herramienta `Skill` devolvió `Unknown skill: mobile-native`. Coincide con la documentación: un `.claude/skills/` que no existía al iniciar necesita `/reload-skills` o sesión nueva |
| Tras un turno posterior de esa sesión | El listado de skills disponibles incluyó las **4 automáticas** (`mobile-native`, `ping-efficient-work`, `systematic-debugging`, `verification-before-completion`) y **no** las 4 manuales |
| Invocar `mobile-native` con `Skill` | Cargó el puntero («Launching skill: mobile-native») |
| Invocar `emil-design-eng` con `Skill` | Rechazada: «cannot be used with Skill tool due to disable-model-invocation… ask the user to run /emil-design-eng». Es el comportamiento diseñado de una skill manual, no un fallo |

Esto **no sustituye** la comprobación en una sesión nueva ni la invocación manual por el usuario (`/emil-design-eng`), que una sesión del modelo no puede realizar. Tampoco se probó en Codex ni en Windows.

### Cómo verificarlo (sin motores ni hooks)

Hacerlo en una **sesión nueva** de Claude Code abierta en la raíz del repositorio y en esta rama; o, en una sesión ya abierta, tras ejecutar `/reload-skills`. Comprobar antes que existen `CLAUDE.md` y `.claude/skills/mobile-native/SKILL.md`.

1. **Automática — `mobile-native`.**
   1. Comprobar que figura entre las skills disponibles (el listado de skills del cliente o, si no, preguntar «¿qué skills de proyecto tienes disponibles?»).
   2. Sin nombrarla, pedir una tarea móvil concreta, por ejemplo: «Revisa si el campo de alias del prototipo provoca zoom en iOS».
   3. **Éxito:** la sesión carga `mobile-native` y lee `.agents/skills/mobile-native/SKILL.md` (visible como lectura de archivo). **Fallo:** no aparece o no lee el archivo fuente; entonces `/reload-skills`, revisar que el `name:` del puntero coincide con su carpeta y anotar la versión de Claude Code.
2. **Manual — `emil-design-eng`.**
   1. Escribir `/emil-design-eng`: debe ofrecerse y poder invocarse.
   2. Comprobar que **no** aparece en el listado que el modelo usa por su cuenta y que, si se le pide «usa emil-design-eng», responde que está reservada a la invocación del usuario.
   3. **Éxito:** tras `/emil-design-eng`, lee `.agents/skills/emil-design-eng/SKILL.md`. **Fallo:** no se ofrece o no lee el archivo fuente.
3. **No ejecutar** en esta comprobación: `scripts/impeccable`, `/impeccable …`, instaladores, plugins ni hooks, y no usar `impeccable` ni `ui-ux-pro-max` como prueba.
4. Registrar aquí el resultado con fecha, versión de Claude Code y lo observado, y retirar la etiqueta «pendiente» solo entonces.

### Estado de lectura y uso (21 octubre 2026)

| Skill | Archivos | Lectura | Descubrimiento | Ejecutada |
|---|---|---|---|---|
| ping-efficient-work | Sí; SHA-256 coincide | **Íntegra** (17 líneas) | Observada (parcial, ver arriba) | No aplica |
| mobile-native | Sí; coincide | **Íntegra** (311 líneas) | Observada y cargada con `Skill` (parcial) | No aplica |
| systematic-debugging | Sí; coincide | Solo frontmatter | Observada en el listado (parcial) | No aplica |
| verification-before-completion | Sí; coincide | **Íntegra** (120 líneas) y PING_NOTES | Observada en el listado (parcial) | No aplica |
| redesign-existing-projects | Sí; coincide | **Íntegra** (181 líneas) | Manual; falta `/nombre` del usuario | No aplica |
| emil-design-eng | Sí; coincide | **Íntegra** (674 líneas, leída a petición expresa) | Manual; rechazada por `Skill` por diseño; falta `/nombre` del usuario | No aplica; la herramienta `Skill` la reserva al usuario |
| ui-ux-pro-max | Sí; coincide | Solo frontmatter | Manual; sin comprobar | No ejecutada (script Python local, sin red) |
| impeccable | Sí; coincide | Frontmatter y fragmentos, no íntegra | Manual; sin comprobar | **No** ejecutada (descarga motor) |

«Leída» no equivale a «aplicada»: lo aplicado en cada trabajo se declara en su propia nota (para la propuesta visual, `docs/design-proposals/identidad-intereses-movil/README.md`).

### Cuándo usar cada una

| Situación | Skill | No usar cuando |
|---|---|---|
| Trabajo con exploración y varios pasos | ping-efficient-work | Pregunta sencilla |
| Cambio estructural de una pantalla | redesign-existing-projects | Ajuste menor de CSS; si exigiría cambiar identidad, tipografía o datos |
| Hover, zoom de campos, pulsación, áreas seguras, teclado | mobile-native | Se pretende aplicar reglas globales o bloquear zoom |
| Feedback de selección, transiciones de estado | emil-design-eng | Efecto decorativo o que retrase una acción |
| Duda concreta de UX/accesibilidad | ui-ux-pro-max | Elegir un estilo o generar un sistema completo |
| Revisión visual final de una pantalla, sobre capturas | impeccable | Como rutina; no repetir si ya hubo una revisión equivalente |
| Defecto reproducible | systematic-debugging | Sin fallo observado |
| Antes de afirmar «funciona/terminado» | verification-before-completion | Para repetir suites sin cambios |

Una tarea no carga las ocho. Un cambio estructural puede usar redesign y, después, una sola revisión con Impeccable.

### Qué no se hizo

No se instalaron plugins, colecciones ni servicios; no se actualizó ninguna revisión (Taste ahora publica `design-taste-frontend`; PING conserva su revisión fijada); no se añadieron skills nuevas, porque las ocho cubren el encargo; no se activaron hooks; no se ejecutaron binarios. Evaluación de las quince referencias: `docs/REFERENCIAS_VISUALES_Y_SKILLS.md`.

## logo-design — 24 octubre 2026

Instalada a petición explícita de Samuel para el trabajo de marca V2. Fuente: https://github.com/kaankiziltug/logo-design-skill, commit `734090317a2155c5bf1ae8bd4cc85c6848a0128f` (release 1.4.5), licencia MIT (atribución en `.agents/skills/logo-design/LICENSE`). Copia selectiva con alcance de proyecto: `SKILL.md`, `references/`, `scripts/`, `templates/`, `LICENSE` y `TRADEMARKS.md`. **No** se instaló la biblioteca `assets/library/` (1 432 logotipos de terceros, marcas registradas, solo de referencia), por lo que `search_library.py` no funciona. Scripts revisados: Python estándar; `render_png.py` usa renderizadores locales (aquí Chrome) mediante subprocess, sin red. Sin instalador, plugin ni hooks. Puntero manual en `.claude/skills/logo-design` (`disable-model-invocation: true`). Registro y SHA-256 en `.agents/skill-installation.json`. Uso: conceptos, construcción SVG y pruebas de la propuesta `docs/design-proposals/v2`; la pausa de la skill tras los conceptos se omitió porque el encargo autorizaba elegir sin consulta intermedia.
