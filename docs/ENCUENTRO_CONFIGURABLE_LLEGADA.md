# Continuidad PING — configuración, llegada y recompensas

## Estado y recuperación

Actualizado17 octubre2026 Bogotá. Copia `/workspace/PING`, remoto `https://github.com/Samge34c/PING.git`. Recuperación verificada: PR36 en borrador, rama `feature/event-encounter-arrival`, último respaldo inicial `9d233b60ec2493f12403e4c0b6278792221fc571`; GitHub no tenía avances posteriores. Árbol inicial limpio, base `feature/connect-event-entry` @ `c288f778d8d7cdf72ec3abb4306473aa756b352b`. Cadena anterior conservada. Sin main, merge, force-push ni publicación pública.

Este documento se actualiza antes de cada commit; el SHA del commit que contiene la actualización se confirma después mediante `git ls-remote origin refs/heads/<rama>` y en su PR. No inferir un respaldo de un archivo local. Estado de esta unidad: bloque1 implementado y verificado, preparado para commit/push; bloques2/3 autorizados y siguientes. No hay trabajo de otros sin conservar.

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

## Siguiente bloque autorizado

Después de confirmar push/actualizar PR36, crear rama desde ese commit: puntos enteros0–100 por actividad con snapshot, créditos y compensaciones Saga variables y anteriores10; recompensas por evento con coste1–10000, lugar/indicaciones y snapshot en canjes. Preservar beneficio demo y permisos existentes. Respaldar antes del recorrido integrado del bloque3.

## Arranque y Windows (incluido clone --single-branch)

Con árbol limpio: `git remote set-branches --add origin feature/event-encounter-arrival`; `git fetch origin`; `git switch --track origin/feature/event-encounter-arrival` si no existe local; si existe, `git switch feature/event-encounter-arrival` y `git pull --ff-only`. No usar reset/clean/main para actualizar. Conservar cambios locales en su rama antes de cambiar.

JDK21 completo, Maven Wrapper del repo; `./mvnw verify` / `.\mvnw.cmd verify`. Para iniciar `PingApplication`: perfil `mysql`; variables externas `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`; opcional `PING_PUBLIC_BASE_URL` real para QR. Bind predeterminado127.0.0.1; no abrir Internet. Flyway aplica nuevas migraciones sin borrar la base; ensayar primero en base exclusiva. En este entorno Java de sistema carece de ct.sym: JDK funcional `/workspace/tools/jdk-21.0.12.1+1`, caché/settings Maven fuera de Git. No copiar credenciales/variables de este entorno a Git.

MySQL y app de ensayo pertenecen a esta tarea; credenciales aleatorias, logs y harness en `/workspace/work/ping-next/` fuera de Git. Pendiente teléfono físico/Safari. No prometer ejecución después de terminar el turno. Próxima acción concreta: confirmar respaldo del bloque1 y empezar bloque2, sin pedir otra autorización.
