# Mi Ping · Festival Social

Implementación de `/me` autorizada por Samuel (DEC-033, Issue #14). Rama `feature/mi-ping-mobile` desde `d088e59`, que ya contiene Hub y ACTIVE. No se atribuye aprobación a Julián. El commit/push de AGENTS.md se completó antes de esta rama. Samuel autoriza ahora respaldar este incremento de Mi Ping y abrir PR en borrador hacia `feature/active-festival-social`; separado de la PR #25 y sin merge. No se repiten las pruebas válidas: los hashes de código coinciden con las fuentes verificadas.

## Cambios implementados

- Identidad y saldo reales compactos; evento enlazado y estado visible. Un panel operativo morado con acción amarilla según disponibilidad, evento y encuentro. Sin portada ni arte nuevo.
- Secuencia móvil: estado/acción → intereses → pista propia opcional → historial. Escritorio utiliza el espacio adicional para intereses/pista, sin cambiar la lectura del DOM.
- El encuentro actual aparece una sola vez. Historial desplegable conserva anteriores, actividad, fechas, feedback, puntos y enlaces; no se eliminan los detalles que solo viven allí.
- OFFERED distingue respuesta propia/ajena; ACTIVE enlaza al encuentro correcto, incluyendo espera unilateral y Saga pendiente. Sin confirmar desde Mi Ping ni adelantar puntos. Palabra y pista ajena no aparecen aquí.
- Formularios/rutas, CSRF, versión, fragmento de pista propia, borradores, live.js y protección contra envíos duplicados conservados. Solo plantilla y CSS acotado de presentación; Java de producción, migraciones, servicios, Hub/ACTIVE y navegación compartida sin cambios.

## Evidencia y verificación

Capturas reales con MySQL: [móvil 390 px](screenshots/mi-ping-mobile.png) primero y [escritorio 1440 px](screenshots/mi-ping-desktop.png). Estado de la demo comparado antes/después sin cambios; sin confirmaciones, puntos ni edición de pistas de esa demo. PING se recargó por caché de plantilla; MySQL no se reinició.

Evidencia privada fuera del checkout: `/workspace/work/ping-mi-ping-20261016/` (0700). No versionar cookies, sesiones, credenciales, logs ni archivos de MySQL.

- Navegador Chromium con aplicación Spring real y datos H2 aislados: 11 vistas dirigidas (390 principal, 360 con alias largo/editor, 1440 y tema claro), más verificaciones concretas de espera propia e historial tras conservar actividad. No se ejecutó una matriz de todas las combinaciones.
- Formularios reales: activar/desactivar disponibilidad con versión, buscar, aceptar desde dos sesiones, confirmación unilateral sin puntos y bilateral con 10 puntos, guardar/quitar/cancelar pista propia, borrador intacto ante cambio en vivo. Pista ajena ausente de Mi Ping; acceso a intereses, beneficios, Hub y retorno al encuentro existentes. Estados pausado/cerrado no ofrecen búsqueda ni activación.
- `verification.json`, `remaining-states.json`, `history-check.json` y `mysql-final.json` conservan resultados. Sin errores JavaScript; sin overflow; controles/foco inferior alcanzables sobre la navegación.
- Suite: `clean verify` ejecutó 154 casos; inicialmente 153 pasaron y FinalMvpTests detectó los textos anteriores y la pérdida del resumen de actividad histórica. Se conservó ese resumen y se actualizó únicamente la aserción del nombre/acción ACTIVE. La repetición dirigida usa `-Dtest=FinalMvpTests verify`, sin repetir los otros 140 casos. Resultado final: **14/14 del grupo, sin fallos ni errores; BUILD SUCCESS y JAR generado**. Los otros 140 casos pasaron en la ejecución general sin repetirse. Salida final en `final-mvp-verify-history.log`; los fallos intermedios también se conservan.
- Revisión manual con Redesign e Impeccable; ping-efficient-work para continuidad, systematic-debugging para fallos concretos de preparación/verificación y verification-before-completion para las afirmaciones. Sin nuevas instalaciones ni subagentes. Hooks siguen desactivados. El detector mecánico advierte jerarquía plana (no resuelve CSS externo: h1 28, h2 24/28, cuerpo 15/16 px) y un borde lateral antiguo de otra pantalla (brand.css:1076); no se alteraron pantallas ajenas para silenciarlos.

## Lectura visual móvil

1. Estado y siguiente acción se entienden; el botón principal aparece antes de intereses/pista.
2. No hay portada decorativa ni encuentro repetido; el saldo ocupa la cabecera compacta. Los alias largos crecen sin truncarse.
3. La navegación conserva espacio inferior/área segura; se comprobó que los últimos controles y el historial sean alcanzables al desplazarse y con foco.
4. Manrope, morado, amarillo y negro mantienen Festival Social, con densidad propia de una pantalla operativa.

Pendiente: revisión humana, Safari/dispositivos físicos y zoom nativo del navegador. No se declara certificación de accesibilidad ni prueba exhaustiva de todos los estados/anchos. No se ha recuperado el respaldo de la tarea antigua.
