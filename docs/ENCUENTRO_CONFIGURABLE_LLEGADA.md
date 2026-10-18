# Continuidad PING — configuración, llegada y recompensas

## Estado y recuperación

**Bloque vigente: DEC-045, categorías y selección móvil**, rama `feature/interest-categories-mobile`, [PR39 en borrador](https://github.com/Samge34c/PING/pull/39), sobre552906a/PR38. Las secciones anteriores describen la cadena conservada. [Cierre y cómo retomar](#cierre-dec-045--unidad3).

Actualizado17 octubre2026 Bogotá. Copia `/workspace/PING`, remoto `https://github.com/Samge34c/PING.git`. Recuperación verificada: PR36 en borrador, rama `feature/event-encounter-arrival`, último respaldo inicial `9d233b60ec2493f12403e4c0b6278792221fc571`; GitHub no tenía avances posteriores. Árbol inicial limpio, base `feature/connect-event-entry` @ `c288f778d8d7cdf72ec3abb4306473aa756b352b`. Cadena anterior conservada. Sin main, merge, force-push ni publicación pública.

Este documento se actualiza antes de cada commit; el SHA del commit que contiene la actualización se confirma después mediante `git ls-remote origin refs/heads/<rama>` y en su PR. No inferir un respaldo de un archivo local. Respaldo bloque1 confirmado: `087b3a7489c88a630b0064adea8e50824e51906b`, PR36 actualizada/en borrador. Respaldo bloque2 confirmado: `bce34589cc346ab73d21fcd277b5f3c029e11f4d`, rama `feature/event-points-rewards`, PR37 en borrador hacia PR36. Respaldo de código bloque3 confirmado: `e85da1b1a0ad7acd1608d8c569d01f18730dc4d5`, rama `fix/event-journey-consistency`, [PR38](https://github.com/Samge34c/PING/pull/38) en borrador hacia PR37. Esta actualización final solo registra respaldos/PR ya verificados; el código probado no cambia. HEAD puede incluir este commit documental posterior: consultar el SHA exacto remoto en PR38/git ls-remote. No queda implementación local sin subir. No hay trabajo de otros sin conservar.

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

Pendiente real: teléfono físico/Safari y prueba Wi-Fi Windows. No hay publicación pública, merge o autenticación nueva. Próxima acción humana: actualizar la copia Windows a la última rama (comandos arriba), arrancar con su configuración privada y comprobar el recorrido desde teléfono; revisar PR36→PR37→PR38 en ese orden, sin integrar automáticamente. Toda implementación de los tres bloques está en los commits de trabajo, no se promete ejecución posterior al turno.

### Referencias finales verificadas antes de esta actualización documental

| Bloque | Rama | Código remoto verificado | PR borrador |
|---|---|---|---|
| Llegada/estructura/MySQL | feature/event-encounter-arrival | 087b3a7489c88a630b0064adea8e50824e51906b | [36](https://github.com/Samge34c/PING/pull/36) |
| Puntos/recompensas | feature/event-points-rewards | bce34589cc346ab73d21fcd277b5f3c029e11f4d | [37](https://github.com/Samge34c/PING/pull/37) |
| Recorrido integrado | fix/event-journey-consistency | e85da1b1a0ad7acd1608d8c569d01f18730dc4d5 | [38](https://github.com/Samge34c/PING/pull/38) |

Main remoto sigue en97f9b4302d0c2bb05221d45e4ac5c1d92d608ac7; no se integró nada. Cada push se contrastó con git ls-remote. Los logs completos actuales de verificación están fuera de Git en /workspace/work/ping-next; no contienen credenciales exportadas. Las PR y los fuentes de prueba documentan evidencia recuperable sin depender de ese directorio.

## Bloque actual DEC-045 — categorías de intereses

Base remota verificada552906a80a88285c7eca7221bbd6b7dd4e3913b7, sin avances posteriores ni cambios locales; rama feature/interest-categories-mobile hacia fix/event-journey-consistency. Un único documento de continuidad, sin depender de otros chats. Último respaldo conocido al iniciar:552906a/PR38; los commits de esta unidad se contrastan después con git ls-remote y su PR en borrador.

Unidad1 implementada: V17 crea categorías globales/orden/activo/versión, asociación opcional de cada interés y fila de serialización del catálogo. Solo agrupa los8 intereses existentes en3 categorías; otros sin categoría siguen disponibles. IDs/nombres/relaciones/encuentros intactos. Validación transaccional conserva selecciones inactivas ya guardadas, rechaza nuevas y permite retirada voluntaria; borrador nuevo no puede finalizar con inactivas. Matching sigue por IDs, sin cambios de reglas ni estados.

Verificación unidad1:13 pruebas dirigidas aprobadas (categorías/migración/intereses existentes),0 fallos/errores/omitidas. **MySQL8.4.11 real**, base nueva ping_categories_upgrade_exclusive: Flyway16→17 conserva IDs/nombres,2 selecciones anteriores, interés sin categoría, snapshot ACTIVE con importe37 y saldos21/34; reejecución0. Fuente reproducible InterestCategoryMigrationTests y main InterestCategoryMySqlVerification (solo acepta base exclusiva nueva y vacía). V1–V16 publicadas intactas. Logs fuera de Git /workspace/work/ping-categories.

Estado local de esta unidad: modelo/validación probados y preparados para commit/push. Aún faltan administración, selector compartido alta/edición, todos los consumidores, revisión móvil y suite general. Siguiente acción exacta después de confirmar este respaldo: implementar administración con nombre/orden/activo y asociación, reutilizando /interests; conectar selector por categorías y protección de borradores/versión. No declarar este punto como entrega visual terminada.


### Respaldo unidad1 y estado estable unidad2

Unidad1 publicada y contrastada con remoto: **7e47010506ac0f35644e753a314269ec8f791462**. PR en borrador [39](https://github.com/Samge34c/PING/pull/39), base fix/event-journey-consistency. No se modificó main.

Unidad2 implementa administración académica existente en `/interests/categories`: nombre/orden/estado, asignar/mover/quitar intereses; versión invalida también formularios de categorías cuya asociación cambió desde otro editor. No se borran intereses ni selecciones. Catálogo público, edición de participante, borrador de entrada, actividades y preparación demo revisados. Actividades conservan relaciones anteriores inactivas, pero rechazan nuevas; preparar demo omite presets inactivos nuevos y sigue siendo idempotente.

Alta y edición usan un fragmento común: tres categorías abiertas, opciones táctiles, selección visible, resumen completo y contador, filtro nativo sin reiniciar checkboxes. Intereses sin categoría en «Otros intereses»; anteriores inactivos en «Conservados», con explicación distinta para borrador y selección persistida. Búsqueda local solo cuando hay más de12 opciones. CSRF, versión, revisión/recibo del borrador e IDs del matching conservados. Acción al final de las8 opciones, sin barra superpuesta; errores409 conservan la selección enviada y permiten recargar la guardada.

Verificación dirigida de unidad2:28 pruebas existentes/categorías aprobadas y6 administrativas/consumidores aprobadas (34 en total). Primera ejecución restringida falló al abrir Tomcat con `java.net.SocketException: Operation not permitted`; con red local autorizada pasó. Una aserción nueva se corrigió para respetar entidades HTML de Thymeleaf. No se modificó código del producto para esos fallos.

Chromium/MySQL exclusivo `ping_categories_ui_exclusive`, distinto del ensayo de migración y de la demo: dos contextos nuevos, QR PNG decodificado y utilizado + código manual, alta, filtros sin perder selección al enviar, volver a alias y edición inline, Space sobre checkbox nativo. Desactivar categoría conserva Música/Tecnología; guardado posterior y matching real entre dos selecciones de Música conservadas. Sin errores JS ni overflow. Capturas examinadas a390 oscuro,360 claro y1440 oscuro. Se retiró barra sticky tras observar que tapaba una fila; botón al final queda al alcance al desplazar y sin cubrir opciones. [Alta390](screenshots/interest-categories/01-join-390.png), [edición360](screenshots/interest-categories/03-edit-360-light.png), [conservados390](screenshots/interest-categories/06-retained-390.png), después [escritorio1440](screenshots/interest-categories/04-edit-desktop.png).

Estado al guardar unidad2: administración/selector y consumidores completos, pruebas dirigidas y revisión visual aprobadas; falta comprobar búsqueda con catálogo de ensayo mayor y cierre general Maven. Siguiente acción exacta tras contrastar push: probar búsqueda/vacío/teclado con fixtures solo en base exclusiva, revisar correcciones y ejecutar verify final. Teléfono físico/Safari siguen pendientes; decodificar QR no equivale a probar su cámara. No hay cachés, secretos, sesiones, dumps ni logs en el commit.


### Cierre DEC-045 — unidad3

Últimos respaldos remotos contrastados antes de esta actualización: unidad1 `7e47010506ac0f35644e753a314269ec8f791462`; unidad2 `ff29bdf32af68a81315b79f4d3edff9e80a5429e`. La tercera unidad contiene esta sección, la corrección de teclado y la evidencia final; su SHA se contrasta tras push y queda publicado en PR39 y en la punta de la rama. El documento no intenta adivinar el hash del commit que lo contiene.

Búsqueda ensayada con13 opciones:8 reales +5 fixtures solo en ping_categories_ui_exclusive, retirados al terminar. Sin resultados, búsqueda sin tildes (`tecnologia`), limpiar filtro y POST con elecciones ocultas conservaron Música/Tecnología. Se reprodujo y corrigió Enter en búsqueda: enviaba el alta; ahora solo filtra localmente. Reejecución dirigida pasó; alta sin JavaScript también terminó con las categorías abiertas. Ningún gusto adicional entra en V17 ni en el catálogo publicado.

**Cierre general:204 pruebas,0 fallos/errores/omitidas, BUILD SUCCESS y JAR.** Comando: JDK21 completo + Wrapper `./mvnw -B -o -Dmaven.repo.local=/workspace/.m2/repository verify`, con red local permitida para los servidores HTTP de prueba. No se descargó ni instaló nada. `clean verify` offline se detuvo antes de pruebas por plugin maven-clean-plugin3.5.0 ausente en caché; se retiraron únicamente target/classes y target/test-classes y se ejecutó verify, recompilando98 fuentes principales y26 de prueba. No hubo código posterior a ese resultado; solo documentación y encuadre de capturas. Suite general H2 separada del ensayo **MySQL8.4.11 V16→17** de unidad1 y de la aplicación real MySQL del navegador.

Revisión manual final: pantalla reconocible, títulos y categorías antes de opciones, selección explícita por check/borde/superficie y resumen; no hay portada ni espacio decorativo que desplace el formulario. Acción después de8 opciones, en flujo para no taparlas. Captura de viewport real a360 con los8 gustos originales confirma botón completo por encima de la navegación: [acción360 claro](screenshots/interest-categories/07-action-360-light.png). Ambos temas conservan controles y contraste de Festival Social. Las capturas full-page muestran la navegación fija en la posición del viewport; no representan una pantalla física de toda esa altura. Capturas y comprobación DOM sin overflow, teclado nativo/Space y foco; sin errores JS en el recorrido principal. Impeccable se revisó manualmente con sus referencias porque no había motor local; no se instaló ni activaron hooks.

Límites: no se probó teléfono físico, cámara, Safari ni Wi-Fi de Windows. Administración académica conserva permisos existentes, sin autenticación nueva. Sin cambios de disponibilidad, alternativas, palabras, llegada informativa, Saga, puntos, canjes o main. c288f778 sigue siendo ancestro; base remota552906a y main97f9b430 se comprobaron sin avances. Bases y navegador son ensayos aislados; no constituyen respaldo de datos. Aplicación de ensayo8087 detenida al acabar; las credenciales, cookies y logs permanecen fuera de Git.

Para retomar en otra copia, revisar primero `git status --short` y conservar cualquier cambio. Desde árbol limpio:

```powershell
git fetch origin feature/interest-categories-mobile:refs/remotes/origin/feature/interest-categories-mobile
git switch feature/interest-categories-mobile
git pull --ff-only origin feature/interest-categories-mobile
git rev-parse HEAD
git ls-remote origin refs/heads/feature/interest-categories-mobile
```

Comparar los dos SHA y PR39. Mantener configuración MySQL privada de la copia; Flyway aplica V17 una vez y Hibernate valida. Abrir evento → entrar por código/QR → seleccionar/filtrar/guardar → editar; administración de categorías en `/interests/categories`. Si se interrumpe antes del push de unidad3, remoto seguroff29bdf y cambios locales exactos por `git status`; ejecutar diff/check, commit y push normal, nunca reset/force-push. Tras publicarla, no queda implementación pendiente de DEC-045: la próxima comprobación es teléfono físico/Windows. Propuesta de siguiente bloque, aún no implementada: gestionar altas y edición de gustos reales desde la administración, manteniendo matching por IDs.


## DEC-046 — Plano por evento, recuperación inicial

Copia /workspace/PING, árbol limpio y remoto21ff7c9b9c2936de9da336207ddce5331c2284d4/PR39 sin avances posteriores. Rama nueva feature/event-floorplan-mobile; base PR39. Las6 skills están versionadas, legibles y hashes/licencias verificados; executor enumera0: aplicación manual. Impeccable context no ejecutó motor local (exit127); se usa PRODUCT/DESIGN y referencias manuales, hooks siguen desactivados. No restaurar variantes ni repetir instalaciones.

Persistencia/alcance en DEC-046 antes del código. Último respaldo remoto seguro21ff7c9. Trabajo local: documentación de decisión; siguiente acción: V18, normalizador de imagen, persistencia separada y snapshot desde ID real del punto. Después carga/colocación, vista móvil y prueba MySQL exclusiva/reinicio/recorrido de dos sesiones. No se reutiliza la demo.


### DEC-046 unidad1 — persistencia y compatibilidad

Implementados V18 (V1–V17 intactas), versiones/bytes separados en MySQL, normalización PNG/JPEG/WebP y posiciones por ID real del punto. Aceptación bilateral toma snapshot de versión/x/y bajo el mismo bloqueo del evento; las respuestas ajenas/OFFERED/COMPLETED no exponen coordenadas. Sustituir exige versión actual/acción explícita, no copia posiciones y conserva imágenes anteriores; mover o quitar marcador no borra el punto. Revisión rechaza formularios obsoletos, puntos de otro evento, valores no finitos o fuera0–1. Versiones antiguas se conservan sin purga automática: incluirlas todas en el respaldo MySQL; cada imagen normalizada tiene límite4MiB.

18 verificaciones dirigidas aprobadas:7 nuevas de imagen/persistencia/snapshots,9 regresiones de encuentros/intereses y2 migraciones. V17→V18 real en MySQL8.4.11, base nueva ping_floorplan_upgrade_exclusive: ACTIVE anterior/palabra/indicaciones/recompensa37, saldos21/34,2 selecciones y3 categorías intactos; referencias históricas nulas y reejecución0. Prueba histórica de categorías fijada explícitamente a V17 para no confundir V18 con su ensayo. Fuentes FloorplanTests/FloorplanMigrationTests/FloorplanMySqlVerification. Se corrigió el mapeo JPA de floorplan_x/y tras reproducir validación fallida; no se cambió V18 para ocultarla.

Entorno: contenedor nuevo ping-floorplan-mysql8.4.11 en loopback3308, bases distintas upgrade_exclusive y ui_exclusive; contenedor/datos anteriores no utilizados. Maven directo falló sin ruta y luego CA del JDK descargado no confiaba en el proxy; configuración existente de proxy + almacén CA del sistema resolvieron TLS sin desactivarlo. Dependencia WebP TwelveMonkeys3.12.0 de Maven Central, lector puro Java (BSD3); jars y cachés fuera de Git. Logs en /workspace/work/ping-floorplan, secretos privados fuera del checkout.

Estado al preparar unidad1: backend probado, sin interfaz todavía. Último remoto seguro antes de push21ff7c9; el commit que contiene esta sección se contrasta después en remoto/PR. Siguiente acción exacta: agregar carga/preview/colocación a la administración existente, con errores recuperables y alternativa numérica accesible; después vista del participante y recorrido/reinicio. No declarar terminado este punto.
