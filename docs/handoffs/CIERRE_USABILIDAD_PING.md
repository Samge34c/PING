# Cierre funcional, visual y de usabilidad de PING (DEC-066)

Encargo de Samuel del 8 de noviembre de 2026 (sesión de Claude Code). No se atribuye aprobación a Julián. **Sin push, merge, despliegue, Render ni Aiven.** Documento único de continuidad de este cierre.

## Base, rama y commits

- Worktree propio `C:\Users\river\PING-cierre`, rama `feature/cierre-usabilidad`, creada desde `7f20b49` (`origin/fix/evento-actividades-live`, contiene `191522f` y `3a528ca` de `deploy/render-demo`). Es la versión integrada más reciente comprobable; **no se verificó qué SHA está desplegado en Render**: resolverlo antes de publicar.
- Otros worktrees y la copia `PING-movil` (con archivos sin seguimiento ajenos) no se tocaron.
- Commits locales, en orden:
  - `cb90ecb` Bloque 3 · siguiente paso, método de comprobación y revisión del personal (incluye migración V25).
  - `06d6056` Bloque 2 · escáner.
  - `8972019` Bloque 4 · imágenes de recompensas y catálogo real.
  - `9a4b3ce` Bloque 5 · organización por evento.
  - `2057fbd` Pulido de la revisión móvil.
  - `5be325b` Clases de ayuda propias (fallo detectado por la suite completa).
  - Commit de documentación (este archivo, DEC-066, hoja de prueba y capturas).

## Hallazgos (diagnóstico sobre `7f20b49`, MySQL aislado)

Recorrido real con cinco cuentas (general, personal con permiso, Ana, Beto, Carolina; después Daniela) registradas por correo (buzón local), evento propio con dos lugares, tres actividades y dos recompensas.

| Id | Prio | Ruta · rol | Problema | Estado |
|---|---|---|---|---|
| H1 | P1 | `/matches/{id}` · participante | La acción del paso actual («Ya llegué», verificar, confirmar) quedaba bajo el pliegue; arriba solo había un enlace. No se decía cuántos puntos ni cuándo hasta el final. | Resuelto: tarjeta «Ahora» con pasos y una acción; hechos de la actividad (recompensa, cómo se comprueba, cuándo llegan). |
| H2 | P1 | `/me/event` · participante | Catálogo como lista sin «qué me toca»; parecía que todas se pueden hacer; no había completadas ni pendientes. | Resuelto: saldo/ganados/pendientes, «Continuar mi encuentro» o «Buscar una conexión», completadas reales, explicación de asignación. |
| H3 | P1 | Actividades · organización | No existía forma de exigir comprobación de la tarea más allá de la confirmación bilateral. | Resuelto: método por actividad y revisión del personal (DEC-066). |
| H4 | P1 | `/me/benefits` · participante | El «Snack del evento · DEMO» global aparecía como beneficio real en cualquier evento. | Resuelto: solo en demo o simulación SOFA; el servidor también lo rechaza fuera de ese contexto. |
| H5 | P1 | `/organizer` · personal | Lista de enlaces sin evento seleccionado, sin pendientes ni estados. | Resuelto: tablero por evento (diseño Figma B). |
| H6 | P1 | Escáner · todos | Lista de todas las cámaras, vídeo 4:3 con letterbox, sin alternancia simple. | Resuelto (Bloque 2). |
| H7 | P2 | Entrada/Conectar · participante | «Sin vínculo oficial con SOFA» en eventos que no son SOFA. | Resuelto. |
| H8 | P2 | `/events/{id}/plan` · participante | Un lugar archivado aparecía como «· Inactivo» en el selector. | Resuelto. |
| H9 | P2 | `/events/{id}/rewards` · organización | Texto obsoleto «sin autenticación nueva… beneficio demo». | Resuelto. |
| H10 | P2 | Editor de lugares | Se pueden crear dos lugares con el mismo nombre. | Abierto (no bloquea; reproducido creando «Plazoleta central» dos veces). |
| H11 | P2 | Recompensas | Tarjeta «La recompensa depende de la actividad del encuentro» (confuso). | Resuelto con instrucción de recibo. |

Comprobado sin defectos en la base: registro + verificación por correo, alias/avatar/gustos, disponibilidad y búsqueda, aceptación bilateral, llegadas, códigos personales (un código de canje se rechaza en el encuentro), confirmación con doble envío sin duplicar, rechazo y nueva búsqueda, exclusión de parejas completadas/rechazadas, canje idempotente (misma clave), recibo, entrega por personal autorizado, 403 para participantes en rutas de organización. Google: no hay credenciales en este entorno; solo cubren las pruebas OIDC locales existentes (pendiente externo).

## Lo implementado

**Bloque 3 · Actividades, verificación y puntos**
- `encounter/CheckMethod`, campos en `EventActivity`/`ActivityForm` y snapshot en `Match` (`activity_check`, `check_instructions`, `response_prompt`).
- `review/ActivityReviewService` y `ActivityReviewController`: `POST /matches/{id}/review` (pareja), `GET /events/{id}/reviews` y `POST /events/{id}/reviews/{match}` (personal con permiso del evento; no puede revisar un encuentro propio; auditoría `APPROVE_TASK` / `REQUEST_TASK_CHANGES`).
- Saga: `MatchCompletionSagaSteps.begin` no prepara la finalización mientras la revisión no esté aprobada; `beginReviewed` + `MatchCompletionSaga.afterReview` arrancan **la misma** finalización tras aprobar si ambos confirmaron. Nunca hay otro saldo ni otra acreditación.
- `match/EncounterProgress` (siguiente paso), `match/ActivityProgress` (saldo/ganados/pendientes/completadas), firma de estado en vivo con la revisión de la revisión (sondeo recarga al aprobar).
- Plantillas: `demo/active-mission.html`, `encounter/guide.html`, `encounter/list.html`, `review/list.html`; JS `event-editor.js`.

**Bloque 2 · Cámara** — `static/js/qr-scanner.js`, `templates/verification/scanner.html`.

**Bloque 4 · Imágenes** — `reward/BenefitImageService` (decodifica, valida tipo real, ≤8 MiB, ≥160 px, ≤24 MP, sin animación; orientación EXIF; 1200 px; JPEG sin metadatos ≤700 KB), `BenefitImages`, rutas `POST/GET /events/{e}/rewards/{id}/image`, `POST …/image/remove`, `GET /me/benefits/{id}/image`; `static/img/rewards/predeterminada.svg`; `static/js/reward-images.js`.

**Bloque 5 · Organización** — `auth/OrganizerBoard`, `OrganizerController`, `templates/account/organizer.html`; `returnTo=organizer` fijo en abrir/pausar/reanudar.

**Estilos** — `static/css/cierre.css` (solo tokens existentes).

**Migración aditiva** `V25__activity_review_and_reward_images.sql`: columnas con valores por defecto compatibles, tabla `activity_reviews`, `benefits.image_revision` y `benefit_images`.

## Decisiones de compatibilidad

- Encuentros activados antes de V25 o con «Confirmación de ambos» completan como siempre aunque después la actividad pase a revisión (probado).
- La regla de dos llegadas + dos verificaciones no se toca; enviar a revisión exige lo mismo.
- Aprobar sin confirmaciones no acredita; confirmar sin aprobación no acredita; repetir no duplica (probado en H2 y MySQL).
- El tablero no añade requisitos: «Abrir evento» sigue disponible; solo informa lo que ya necesita un encuentro nuevo.
- Imágenes en MySQL, no en disco (Render es efímero); la lista lee solo `image_revision`.

## Propuesta pendiente (no implementada)

Tareas independientes de un encuentro u otras formas de ganar puntos: requieren decidir quién las asigna, límite por persona, si cuentan para la exclusión de parejas, cómo se previene el abuso (una persona sola confirmando) y si usan la misma revisión. No se habilitó ninguna economía nueva. Evidencia fotográfica: posible ampliación separada, con almacenamiento, privacidad y retención propios.

## Pruebas

Ver sección **Resultados** al final (SHA y suite completa).

Dirigidas (H2): `ActivityReviewTests` 6/6, `BenefitImageTests` 4/4, `OrganizerBoardTests` 1/1; regresiones de Saga, verificación, encuentro, guía, estado en vivo, editor, entrega, demo, cuentas y OAuth en verde (se ajustaron dos aserciones de formato: firma en vivo con revisión y copia de la guía).

En la app real (Chrome headless por CDP, MySQL 8.4 aislado en 3317, buzón local):
- Recorrido completo con dos sesiones de participantes y personal: entrada → actividad → verificación → envío → corrección → reenvío → confirmaciones → aprobación → +15 a cada una (una Saga, intento 1) → canje → recibo → entrega. Aprobaciones repetidas por HTTP: 200 y saldo sin cambios.
- Migración: V24 → V25 sobre la base con datos reales de prueba (encuentros completados, canje entregado); arranque con `ddl-auto=validate`; reinicio posterior «Schema is up to date. No migration necessary».
- Escáner con cámara simulada de Chrome (archivo `.y4m` del QR real del evento): lectura correcta y cámara liberada; QR situado fuera del área visible no se lee; abrir/cerrar rápido ×5 sin pistas vivas; ocultar la pestaña detiene la cámara; QR del evento en el escáner del encuentro → «no corresponde», campo vacío.
- Escáner con `mediaDevices` simulado en la página: trasera+frontal (alterna y refleja la frontal), cuatro lentes (alterna y ofrece «Elegir otra cámara»), dos cámaras sin orientación («Cámara 1 de 2»), una cámara (sin botón), permiso denegado, sin cámara, cámara ocupada; nunca se pide audio; al cerrar no quedan pistas vivas. **Son simulaciones; no equivalen a Safari/iPhone físico.**
- Sin errores JS ni desbordamiento horizontal en los recorridos; capturas 390 oscuro, 360 claro y 1366 escritorio.

## Capturas (móvil primero)

[`docs/screenshots/cierre-usabilidad/`](../screenshots/cierre-usabilidad/): `antes/` (base `7f20b49`) y `despues/`.

| Antes | Después |
|---|---|
| `antes/01-encuentro-390` acción bajo el pliegue | `despues/01-encuentro-llegar-390`, `02-enviar-actividad-390`, `03-seccion-revision-390`, `04-correccion-390`, `07-completado-390`, `17-verificar-360-claro` |
| `antes/02-evento-390` lista sin acción | `despues/18-evento-actividad-360-claro` |
| `antes/03-beneficios-390` snack DEMO, sin imagen | `despues/12-beneficios-imagen-390`, `14-recibo-360-claro` |
| `antes/04-organizacion-390` lista de enlaces | `despues/15-organizacion-personal-390`, `16-organizacion-general-390`, `05-revisar-tareas-390`, `06-revisar-tareas-escritorio`, `08-actividad-metodo-390`, `13-recompensa-imagen-390` |
| (escáner 4:3 con lista) | `despues/09-escaner-abierto-390`, `10-frontal-simulada-390` (simulada), `11-escaner-qr-equivocado-390` |

Revisión móvil (390): la pantalla del encuentro se entiende por la tarjeta «Ahora» y su única acción; el espacio superior lo ocupa el paso, no decoración; la barra inferior no tapa la acción en la primera vista; mantiene Festival Social (negro, morado, amarillo, Manrope).

## Evaluación por tareas (heurística, no es prueba con usuarios)

| Objetivo | Primera acción visible | Resultado esperado | Posible confusión | Recuperación |
|---|---|---|---|---|
| Hacer la actividad | Tarjeta «Ahora» del encuentro | Paso y botón del estado real | Esperar puntos al enviar | Texto «Enviar no es aprobar ni suma puntos» y estado «Pendiente de revisión» |
| Saber qué me toca | Evento → «Continuar mi encuentro» / «Buscar una conexión» | Llega al encuentro o a Conectar | Creer que puede elegir otra actividad | «No se eligen ni se reclaman aquí…» |
| Canjear | Beneficios: coste, saldo, «te faltan» | Recibo con código y lugar | Confundir ganados con saldo | «Tu saldo» separado de «Ganados» |
| Revisar (personal) | Organización → «Revisar tareas · N pendientes» | Aprobar o pedir corrección | Creer que aprobar paga siempre | Mensaje tras aprobar y nota de la lista |
| Entregar | Organización → «Entregar un canje · N por entregar» | Entregado sin cobro extra | — | Recibo muestra «Entregado» |

Hoja para probar con personas: [cierre-usabilidad/HOJA_PRUEBA_PERSONAS.md](cierre-usabilidad/HOJA_PRUEBA_PERSONAS.md).

## Figma

Archivo `WCrjKj5eGOZQ8wUaxsEqfc`, página nueva **«PING · Cierre de usabilidad · Propuesta (8 nov 2026)»** `188:262` (originales intactos): A Encuentro `188:276`, B Organización `189:340`, C Recompensa con imagen `189:419`, D Escáner `189:453`, componente local «Cierre · Paso del encuentro» `188:275`. Construidos con variables V2, estilos Manrope y componentes V2 (Botón, Cabecera, Navegación), no con capturas.

## Pendientes físicos o externos

- iPhone/Safari y Android reales: permiso de cámara, alternancia trasera/frontal, enfoque y lectura a distancia, áreas seguras y teclado sobre el campo de respuesta.
- Fotografía real de una recompensa tomada con el móvil (orientación EXIF probada con un JPEG construido en la prueba).
- Google OAuth real y correo real (solo buzón local en este entorno).
- Resolver el SHA desplegado en Render y rebasar/integrar antes de publicar; push, PR y despliegue requieren autorización de Samuel; aceptación de Julián pendiente.
- Prueba con personas usando la hoja.

## Entorno de verificación (fuera del repositorio)

Scratchpad de la sesión: MySQL 8.4 `--initialize-insecure` en 3317 (datadir nuevo, solo loopback, bases `ping_cierre_exclusive` y `ping_upgrade_exclusive`), buzón `tools/dev-mailbox.py` en 1027, app en 8091, arnés CDP. Credenciales generadas solo para ese entorno; no están en Git. Se puede borrar sin afectar a otras instancias (no se tocó el MySQL del servicio en 3306).

## Siguiente acción exacta para retomar

1. `cd C:\Users\river\PING-cierre && git status --short --branch && git log --oneline -8`.
2. Revisar este documento y las capturas `despues/`; decidir aceptación de DEC-066 (Samuel y Julián).
3. Con autorización: comprobar el SHA Live de Render, integrar `feature/cierre-usabilidad` sobre él, ejecutar `./mvnw clean verify`, probar en un iPhone por HTTPS el escáner y la hoja de cinco tareas; después push/PR en borrador.

## Resultados

- **Suite completa** `./mvnw -o clean verify` sobre `2057fbd`: 383 pruebas, **1 fallo** (`FrontendBundleTests`: usé `form-text` y `py-1`, ausentes del subconjunto de Bootstrap). Causa corregida en `5be325b` con una clase propia, sin regenerar el bundle.
- **Suite completa repetida** sobre `5be325b` (código final; después solo documentación): **383 pruebas, 0 fallos, 0 errores, 0 omitidas, BUILD SUCCESS**, 08/11/2026 19:45:52 Bogotá. JAR SHA-256 `b20ac2b8beda6cf6…` (prefijo), el mismo que sirvió las últimas capturas `17` y `18`.
- Recorrido integrado, migración y escáner: ver «Pruebas» (ejecutados sobre `2057fbd`; `5be325b` solo cambia el nombre de una clase CSS de ayuda en dos formularios de organización).
- No probado: dispositivos físicos, Safari, Google real, correo real, rendimiento con muchas recompensas con imagen (las listas no cargan binarios; las imágenes son ≤700 KB, perezosas y cacheadas 7 días).
