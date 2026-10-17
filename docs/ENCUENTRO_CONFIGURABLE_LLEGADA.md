# Continuidad PING — configuración, llegada y recompensas

## Estado y recuperación

Actualizado17 octubre2026 Bogotá. Copia `/workspace/PING`, remoto `https://github.com/Samge34c/PING.git`. Recuperación verificada: PR36 en borrador, rama `feature/event-encounter-arrival`, último respaldo inicial `9d233b60ec2493f12403e4c0b6278792221fc571`; GitHub no tenía avances posteriores. Árbol inicial limpio, base `feature/connect-event-entry` @ `c288f778d8d7cdf72ec3abb4306473aa756b352b`. Cadena anterior conservada. Sin main, merge, force-push ni publicación pública.

Este documento se actualiza antes de cada commit; el SHA del commit que contiene la actualización se confirma después mediante `git ls-remote origin refs/heads/<rama>` y en su PR. No inferir un respaldo de un archivo local. Respaldo bloque1 confirmado: `087b3a7489c88a630b0064adea8e50824e51906b`, PR36 actualizada/en borrador. Respaldo bloque2 confirmado: `bce34589cc346ab73d21fcd277b5f3c029e11f4d`, rama `feature/event-points-rewards`, PR37 en borrador hacia PR36. Bloque3 en `fix/event-journey-consistency` desde ese SHA; comprobado y preparado para su propio commit/PR en borrador hacia PR37. No hay trabajo de otros sin conservar.

## Decisiones y comportamiento actual — bloque1 / DEC-042

**Corrige DEC-041:** «Ya llegué» es opcional e informativo para todos los encuentros, incluidos los que quedaron con `arrival_required=true`. No condiciona confirmar, no acredita puntos, no confirma actividad y no inventa llegadas anteriores. Aceptación/confirmación bilaterales, privacidad, CSRF, Saga y reintentos conservados. Las llegadas propias se actualizan en la sesión ajena con polling existente.

`/events/{id}/encounters` administra puntos con nombre/indicaciones/activo y actividades con título/instrucciones/intereses relacionados/puntos compatibles/activo. Formularios de edición con versión. No hay eliminación de registros ni autenticación nueva: es la administración académica existente, no debe exponerse públicamente. Palabras base siguen configurándose en Editar evento.

Una actividad con intereses exige al menos uno de ellos entre los intereses comunes; sin selección es neutral. Entre actividades válidas se prioriza mayor afinidad y luego ID estable. Solo se elige un punto activo compatible de ese evento. Sin combinación válida, aceptar vuelve a la propuesta con mensaje recuperable; no activa ni inventa fallback. Editar/desactivar no cambia las instrucciones guardadas de misiones activas.

Sin configuración estructurada se conserva el catálogo anterior, incluido SOFA. Las listas V14 permanecen intactas y pueden importarse explícitamente una sola vez: todos sus puntos se consideran compatibles con sus actividades neutrales, ya que las listas antiguas no especificaban restricciones. Importar/crear un punto inicia configuración estructurada; se necesita una actividad compatible para activar nuevas propuestas. Se puede editar después sin perder el texto original.

## Migraciones y compatibilidad

V1–V14 publicadas intactas. V15 aditiva: flag de configuración estructurada, tablas de puntos/actividades con relaciones a puntos e intereses; snapshot de indicaciones/título en matches. Las listas originales no se borran. El flag arrival_required de V14 queda como dato histórico sin efecto. No se reescriben activos, palabras, puntos, saldos ni Saga anteriores.

**MySQL8.4.11 real verificado** en contenedor `ping-next-mysql`, loopback3307, base exclusiva `ping_block1_exclusive`: Flyway13→14→15 con evento anterior, ACTIVE parcialmente confirmado, Saga pendiente/primer crédito y saldos30/20. Valores y listas conservados; reejecución0. Spring/Hibernate valida y arranca en otra base exclusiva `ping_ui_exclusive`. La demo no se reutilizó ni se borró. Fuente reproducible del ensayo: `src/test/java/com/ping/event/MySqlUpgradeVerification.java`, main con classpath de test, `DB_URL` de una base NUEVA con «exclusive» en el nombre y `DB_PASSWORD` externo. No ejecutar en una base usada; no hace clean/drop.

## Verificaciones del bloque1

- `verify` con JDK21 completo/Wrapper: **180 pruebas,0 fallos/errores/omitidas**, JAR. Antes:13 dirigidas,0 fallos, sobre llegada informativa/compatibilidad/privacidad/snapshot/recuperación/importación/versiones. La utilidad MySQL es un main explícito, no una prueba omitida presentada como ejecutada por Surefire.
- Chromium real sobre MySQL8084: configuración por formularios, dos altas normales por código/intereses, disponibilidad/búsqueda explícitas, aceptación, misma misión, llegada solo de una persona, polling en la otra, confirmaciones de ambas hasta COMPLETED/saldo10. Sin perfiles demo, estado forzado ni errores JS.
- Capturas examinadas390 principal,360 claro y1440 oscuro: acción y estado legibles, indicaciones útiles en evento genérico sin mapa SOFA, confirmación accesible sin llegada bilateral, navegación no tapa formularios al desplazarse. Se conserva Festival Social, Manrope y paleta. No se rediseñan Hub/ACTIVE.
- `git diff --check`, revisión de diff y ficheros nuevos; sin secretos/cookies/logs/dependencias en Git. Skills locales usadas: ping-efficient-work y verification-before-completion; contexto visual existente consultado selectivamente. Hooks desactivados; sin subagentes.

Evidencia móvil primero: [llegada informativa390](screenshots/structured-informative/04-one-arrived-mobile.png), [confirmación disponible360 claro](screenshots/structured-informative/06-arrival-360-light.png), [actividad estructurada](screenshots/structured-informative/01-structured-activity-mobile.png). Después [escritorio1440](screenshots/structured-informative/07-active-desktop.png). Capturas anteriores de `event-encounter-arrival` son históricas y muestran la condición corregida; no representan el comportamiento actual.

## Secuencia autorizada

Después de confirmar push/actualizar PR36, crear rama desde ese commit: puntos enteros0–100 por actividad con snapshot, créditos y compensaciones Saga variables y anteriores10; recompensas por evento con coste1–10000, lugar/indicaciones y snapshot en canjes. Preservar beneficio demo y permisos existentes. Respaldar antes del recorrido integrado del bloque3.

## Arranque y Windows (incluido clone --single-branch)

Detener solo la app PING propia antes de cambiar código; conservar MySQL/datos/variables privadas. Con árbol limpio: `git remote set-branches --add origin fix/event-journey-consistency`; `git fetch origin`; `git switch --track origin/fix/event-journey-consistency` si no existe local; si existe, `git switch fix/event-journey-consistency` y `git pull --ff-only`. No usar reset/clean/main para actualizar. Conservar cambios locales en su rama antes de cambiar.

JDK21 completo, Maven Wrapper del repo; `./mvnw verify` / `.\mvnw.cmd verify`. Para iniciar `PingApplication`: perfil `mysql`; variables externas `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`; opcional `PING_PUBLIC_BASE_URL` real para QR. Bind predeterminado127.0.0.1; no abrir Internet. Flyway aplica nuevas migraciones sin borrar la base; ensayar primero en base exclusiva. En este entorno Java de sistema carece de ct.sym: JDK funcional `/workspace/tools/jdk-21.0.12.1+1`, caché/settings Maven fuera de Git. No copiar credenciales/variables de este entorno a Git.

MySQL y app de ensayo pertenecen a esta tarea; credenciales aleatorias, logs y harness en `/workspace/work/ping-next/` fuera de Git. Pendiente teléfono físico/Safari. No prometer ejecución después de terminar el turno. Los bloques siguientes se registran abajo; no recuperar archivos de otros chats para arrancar. El código, migraciones, recursos y este documento están en la cadena de ramas GitHub.

## Bloque2 — DEC-043 / puntos y recompensas de evento

Actividad configurable0–100 puntos enteros, guardados al activar en el match y al preparar la Saga en su registro durable. Créditos primero/segundo y compensaciones segundo/primero usan el importe de la Saga; no consultan la configuración actual. Reintentos/recuperación conservan importe y recibos. Valores anteriores10. Llegadas/recargas no premian;0 completa sin crédito. V16 aditiva, sin tocar V1–V15.

`/events/{id}/rewards`: crear/editar/desactivar nombre/descripción/coste1–10000/lugar/indicaciones con versión y pertenencia al evento. Mismos permisos académicos, sin autenticación nueva. Catálogo del participante incluye sus recompensas y el snack demo global anterior. Solicitud solo acepta beneficio/clave; servidor decide coste/evento y guarda nombre/descripción/lugar/indicaciones/coste. Editar/desactivar no cambia canjes emitidos; reintento conserva recibo, entrega/incidencia no debita de nuevo. Importes mostrados en ACTIVE, resultado, historial y admin.

Verificado:35 pruebas dirigidas (variables0/37/100, fallos de crédito/cierre/reversión, recuperación/idempotencia, snapshots, ámbito del evento, costes inválidos y solicitudes concurrentes); `verify` general **192 pruebas,0 fallos/errores/omitidas**, JAR. Se añadió después aserción explícita COMPLETED al caso0 y precondición de base vacía a la utilidad MySQL: verificación dirigida final8 casos/JAR. MySQL8.4.11 exclusivo `ping_final_upgrade_exclusive`: Flyway13→14→15→16, saldos30/20 y recibos intactos, importes10 anteriores, snapshot de canje previo20/nombredemo, reejecución0. La utilidad rechaza bases con tablas; nunca clean/drop.

Chromium/MySQL: dos altas nuevas reciben37 cada una al confirmar (solo una marcó llegada). Recompensa del evento30 → saldo7 → solicitudREQUESTED → entregaDELIVERED por admin. Indicaciones en catálogo y canje;390 y360 claro, escritorio operativo, sin erroresJS/overflow. Se examinó el resultado: saldo/coste/faltantes y lugar visibles, barra no bloquea acciones; identidad y temas conservados. [Beneficio390](screenshots/event-points-rewards/10-event-reward-mobile.png), [entrega390](screenshots/event-points-rewards/12-delivered-mobile.png), [360 claro](screenshots/event-points-rewards/13-claim-360-light.png), [escritorio](screenshots/event-points-rewards/07-active-desktop.png). Capturas adicionales/harness/logs fuera de Git.

La revisión automática rechazó crear un Issue del bloque2 por considerarlo publicación externa no autorizada. No se reintentó ni se eludió; la decisión/alcance se registran aquí y en la PR en borrador expresamente autorizada. No hay un Issue nuevo publicado.

Siguiente acción concreta tras push/PR del bloque2: revisar integrado y corregir tres problemas observados, sin rediseñar: beneficio demo aparece antes que el propio del evento; «Seguir conectando»/«Buscar otra conexión» vuelven a Mi Ping en vez de Conectar; llegada opcional aún dice «Esperando…/En camino» aunque no bloquea. Registrar evidencia y mantener ambas rutas sin efectos por GET.

## Bloque3 — DEC-044 / recorrido integrado final

Corregido sobre el respaldo del bloque2: beneficios propios antes del demo; «Seguir conectando» y «Buscar otra conexión» van a `/connect`; llegada sin marcar dice «Sin registrar», no supone ubicación ni espera obligatoria; confirmación pregunta por actividad terminada. Nuevos puntos exigen indicaciones; importación antigua y misiones sin mapa tienen orientación honesta para ubicar el punto/consultar organizador. Sin migraciones ni cambios de recompensa/Saga/autenticación.

34 pruebas dirigidas aprobadas. Chromium real/MySQL, dos contextos nuevos: QR PNG generado en servidor, decodificado con ZXing y URL exacta utilizada por primera persona; segunda escribe código. Evento → alias/intereses → Mi Ping → Conectar; varias navegaciones GET conservan firma/disponibilidad/matches. Disponibilidad/búsqueda explícitas, propuesta/aceptación y recuperación por Conectar; misión sin mapa SOFA en evento genérico e indicaciones visibles. Solo uno marca llegada; polling informa al otro y ambos confirman37/37. «Buscar otra conexión» abre Conectar sin crear propuesta. Recompensa local aparece primero,30 →saldo7 →solicitud y POST repetido con misma clave: un solo canje/sin descuento extra →entrega registrada. Sin erroresJS/overflow.

Revisión visual manual390 principal,360 claro y1440 oscuro: la acción de canje queda visible antes de la barra, saldo/coste/faltantes contiguos e indicaciones encontrables; llegada opcional y confirmación separadas; no hay decoración/portada nueva. En escritorio Mis canjes comparte espacio con catálogo sin perder navegación. [Beneficio390](screenshots/journey-final/10-event-reward-mobile.png), [llegada390](screenshots/journey-final/04-one-arrived-mobile.png), [confirmación360](screenshots/journey-final/06-arrival-360-light.png), [entregado390](screenshots/journey-final/12-delivered-mobile.png), después [escritorio1440](screenshots/journey-final/14-benefits-desktop.png). Solo capturas representativas en Git; QR PNG de ensayo, extras y sesiones fuera. Decodificar QR no equivale a cámara de teléfono.

Archivos principales: `encounter/EncounterService,PointForm`, `reward/RewardService`, templates `demo/active-mission,match`, `rewards/benefits`, `encounter/point`; pruebas StructuredEncounter/EventBenefit y regresiones de entrada/matches. General final `verify` del bloque3: **193 pruebas,0 fallos/errores/omitidas**, BUILD SUCCESS/JAR. No hay cambios de código posteriores a esa ejecución; solo esta documentación y selección de capturas. Credenciales y datos de bases exclusivas no se publican.

Pendiente real: teléfono físico/Safari y prueba Wi-Fi Windows. No hay publicación pública, merge o autenticación nueva. Próxima acción humana: actualizar la copia Windows a la última rama (comandos arriba), arrancar con su configuración privada y comprobar el recorrido desde teléfono; revisar PR36→PR37→PR del bloque3 en ese orden, sin integrar automáticamente. Toda implementación de los tres bloques está en los commits de trabajo, no se promete ejecución posterior al turno.
