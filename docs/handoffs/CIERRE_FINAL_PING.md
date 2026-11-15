# Cierre final de PING · continuidad

Encargo de Samuel: `PING_CIERRE_FINAL_MEGA_PROMPT_V2.md` (fuera del repo, en Descargas). Cada fase tiene una rama local y una entrada aquí. Los logs y la evidencia privada quedan fuera del repo, en `C:\Users\river\PING-evidencia\cierre-final\`.

## Estado (15 de noviembre de 2026)

| Fase | Estado | Rama |
|---|---|---|
| F0 Diagnóstico | Hecho | — (solo lectura) |
| F1 Base de datos y alojamiento | Respaldo V26 **verificado**; cambio a Aiven Developer **pendiente (G2)** | — |
| F2 Gustos compactos en Mi Ping | Aprobada por Samuel; commit local | `feature/cierre-gustos` desde `f5b5a9a` |
| F3 Latencia A→B | Aprobada por Samuel; commit local (contiene F2) | `feature/cierre-latencia` desde `6d4fcd1` |
| F4–F7 | Sin empezar | — |

## F0 · Diagnóstico
- **Base:** `f5b5a9a`, igual a `origin/feature/connect-state-cards`.
- **Versión publicada:** Render Free con `f5b5a9a` como Live, según el historial de Render que comprobó Samuel. Coincide con los estáticos servidos (`encounter-step.js`, `live.js` y `cierre.css` tienen el mismo hash de blob).
- **Suite base:** `.\mvnw.cmd -B clean verify`, 398 pruebas, 0 fallos (14 nov, 22:52–22:55).
- **Arranque en frío** (demo publicada tras ~20 min sin tráfico): `/login` respondió 200 a los **193 s**. En caliente responde en 0,42–0,79 s. El POST de inicio de sesión no se midió.
- **Demora A→B con PING abierto** (en local, con datos ficticios, n=35): mediana **2,56 s**, máximo 5,16 s. Casi todo es la espera de la consulta de `live.js` (5 s); consulta y recarga suman 0,10–0,28 s. En Render caliente se estima una mediana de ~3,5 s (estimación sin medir).

## F1 · Base de datos
- **Plan actual:** Aiven MySQL Free (1 CPU, 1 GB de RAM, 1 GB de disco), según la consola, comprobado por Samuel el 15 nov. Aiven apaga los Free y **borra datos y copias el 21 dic 2026**.
- **Respaldo de referencia: VERIFICADO.**
  - Creado el 15 nov 2026 a las 04:34 UTC, de Aiven MySQL 8.4.11 con TLS `VERIFY_IDENTITY`. Esquema **V26**, 44 tablas, 1.517.305 bytes. SHA-256 `2a1b15f2271d79016de645133c80a547b1d7c4eb1cec4bbcf31fae13506d0d87`.
  - Restaurado en una MySQL 8.4 aislada (127.0.0.1:3320). Las 44 tablas tienen el mismo número de filas que el manifiesto.
  - PING `f5b5a9a` arrancó sobre la copia sin migraciones, con HTTP 200. Tras arrancar y tras reiniciar, todas las filas eran idénticas.
  - Registro: `restauracion_verificada_f5b5a9a.json`, junto al respaldo en `%LOCALAPPDATA%\PING\backups\cierre-final\` (fuera de Git).
- El respaldo del 11 nov (V24, anterior a `00483fa`) queda solo como histórico. No es una copia reciente.
- **Paso a Aiven Developer (US$5/mes, cobro por horas): PENDIENTE de G2.** Procedimiento, costo, verificación y reversión en `PING_CIERRE_FINAL_MEGA_PROMPT_V2.md` §F1 y en la evidencia privada `F1\PASO_A_DEVELOPER.md`. Hay que hacerlo antes del 21 dic y lejos de la exposición.
- **Arranque en frío de Render Free: decisión aparte, sin tomar.** Opciones: cómputo de pago desde US$7/mes o el protocolo de calentamiento.

## F2 · Gustos compactos en Mi Ping (DEC-071)
- Problema: `templates/demo/dashboard.html` pintaba todos los gustos. La base real tiene 180 selecciones para 11 participantes, unas 16 de media.
- Cambio: se muestran hasta 4 y el resto queda en «Ver todos (N)» / «Ver menos», con el mismo `details.chip-more` de Conectar (DEC-070).
- **Archivos:** `templates/demo/dashboard.html` (sección `mp-interests`); prueba nueva `DemoImprovementTests.miPingKeepsEveryInterestAndOffersSeeAllOnlyAboveFour`, que comprueba que todos los nombres siguen en el HTML, que «Ver todos (N)» aparece solo con más de 4 y que los gustos guardados no cambian. Sin CSS, JS ni datos nuevos.
- **Pruebas focalizadas** (15 nov): `DemoImprovementTests`, `EventEntryFlowTests`, `Interest*Tests` y `MatchTests` → **58 pruebas, 0 fallos**. Suite completa pendiente para el candidato de F7.
- **Capturas reales** (JAR local de la rama, MySQL aislada y datos ficticios; evidencia en `PING-evidencia\cierre-final\F2\`):
  - A 390 px, oscuro, con 32 gustos y alias largo: la sección pasa de **862 a 194 px** y la página de 2440 a 1790 px. Desplegada mide 1010 px.
  - A 360 px: 194 px plegada y 1058 px desplegada.
  - En escritorio a 1280 px: 98 px plegada.
  - Sin desplazamiento horizontal en ningún ancho. «Ir a Conectar» sigue en la primera pantalla.
- **Casos:** con 0 gustos, el texto «Sin gustos elegidos…»; con 1 y con 4, sin control; con 5, cuatro visibles y «Ver todos (5)»; con 32, cuatro visibles y «Ver todos (32)».
- **Teclado:** Tab llega al control (foco amarillo de 2,4 px). Enter despliega y Espacio pliega. Tras recargar vuelve plegada. «Editar» abre `/participants/{id}/interests` y Atrás vuelve a `/me`.
- **Contraste del control:** 8,98:1 en oscuro y 6,24:1 en claro (14 px). Área táctil de 44 px de alto.
- **Revisión móvil (390 px):**
  - ¿Se entiende la pantalla? Sí. La acción principal sigue arriba y los gustos son un resumen de 4 con un acceso claro al resto.
  - ¿Algo desplaza lo importante? Ya no: el muro de 862 px desapareció.
  - ¿La navegación tapa contenido? No; la barra inferior queda fija y el contenido se desplaza bajo ella como en el resto de la app.
  - ¿Tiene identidad propia? Sí: los mismos chips morados y el enlace de Conectar (DEC-070).
- **Límite conocido:** al desplegar, «Ver menos» queda entre los 4 primeros y el resto, igual que en Conectar. Se mantiene por coherencia; cambiarlo afectaría a ambas pantallas.
- **Nota de prueba:** tras 10 inicios de sesión en 30 min, el límite de PING bloqueó el siguiente. Es el comportamiento correcto. Solo en la base de prueba aislada se vaciaron las filas de `auth_throttle` para seguir.
- **Estado:** Samuel aprobó F2 el 15 nov 2026 y autorizó un único commit local en `feature/cierre-gustos`. Sin push, PR, merge ni despliegue. DEC-071 sigue «propuesta» hasta que conste la aceptación de Julián.

## F3 · Latencia: cuánto tarda la otra sesión en enterarse de «aceptar» (DEC-072)
- **Rama:** `feature/cierre-latencia` desde `6d4fcd1` (F2). Sin commit.
- **Causa medida** (14 nov, n=35 en todas las transiciones): casi todo el tiempo era la espera de la siguiente consulta de `live.js` (5 s después de cada respuesta). `/me/state` cuesta ~6 ms en el servidor (p95 8 ms, 68 bytes) y la recarga suma 0,1–0,3 s.
- **Cambio:** solo `static/js/live.js`. Consulta cada ~1,5 s (±15 %) mientras el servidor indica `OFFERED` o `ACTIVE`, y cada 5 s en el resto y tras un error. Todo lo demás de `live.js` queda igual.
- **Medición LOCAL, no de producción:** PING y una MySQL aislada en 127.0.0.1 en el PC de Samuel, con datos ficticios y dos sesiones Playwright en el mismo equipo. Sin red real, sin Render y sin teléfonos. Mismo arnés y misma base, 30 muestras por condición. La otra sesión, con la pantalla abierta, se considera enterada cuando termina la recarga que hace `live.js`.

| | Antes (5 s) | Después (~1,5 s con encuentro abierto) |
|---|---|---|
| Mediana | 1,92 s | **0,82 s** |
| Media | 2,21 s | 0,75 s |
| p90 | 4,07 s | 1,28 s |
| Máximo | 4,88 s | **1,46 s** |
| Muestras por encima de 2 s | 15/30 | 0/30 |
| Respuesta propia del que acepta (mediana) | 94 ms | 101 ms |

- **Comportamiento comprobado:**
  - Sin encuentro: una consulta cada 5,01 s.
  - Con propuesta (`OFFERED`): una cada 1,55 s (de 1,31 a 1,68 s).
  - Con la red cortada (consulta abortada): vuelve a 5 s y muestra «Sin conexión».
  - Al reconectar: vuelve a 1,56 s y desaparece el aviso.
  - Nunca hay más de una consulta en curso.
  - La pausa en pestaña oculta y la sesión perdida (401) usan el mismo código de antes, sin cambios.
- **Pruebas focalizadas:** `MatchLiveStateTests`, `VerificationTests`, `ConnectReadWorkTests`, `EncounterProgressTests`, `FrontendBundleTests` y `PublicResourceTests` → **32 pruebas, 0 fallos**.
- **Carga estimada (no medida en Render):** solo consultan rápido las pantallas con un encuentro abierto. Ejemplo: 40 personas en 20 encuentros ≈ 27 consultas/s, frente a ≈ 8 consultas/s con 5 s. En local cada una cuesta ~6 ms; en Render Free (0,1 CPU, pool de 4) no está medido. Para la demo con dos teléfonos el impacto es despreciable. Antes de un evento con mucha gente, medir en Render (G3) o limitar el ritmo rápido a los pasos de espera.
- **Lo que esto no resuelve:**
  - El arranque en frío de Render Free (193 s) es una decisión de F1.
  - La red real añadirá ~0,4–0,8 s por consulta y recarga; en Render caliente se estima una mediana de unos 1,5–2 s. Es una estimación; medirlo requiere G3.
- **Cierre de F3** (15 nov, MEDICIÓN LOCAL con el mismo método; «antes» = F0 sobre `f5b5a9a`):

| Transición | Antes: mediana / máx (n) | Después: mediana / p90 / máx (n) |
|---|---|---|
| Aceptar | 1,92 s / 4,88 s (30) | 0,82 s / 1,28 s / 1,46 s (30) |
| Llegada | 2,27 s / 4,69 s (10) | **0,59 s** / 1,06 s / 1,32 s (20) |
| Verificar (código manual) | 3,00 s / 4,73 s (10) | **0,56 s** / 1,40 s / 1,59 s (20) |

- **Regresiones comprobadas con Playwright:**
  - Pestaña oculta: 0 consultas en 8 s; al volver, la primera sale en 3 ms.
  - Atrás (de Mi Ping al encuentro): sigue a ritmo rápido.
  - Sesión perdida (cookies borradas): aviso «Tu sesión ya no está disponible», 3 botones desactivados y 0 consultas después.
  - Ya comprobados antes: red cortada (vuelve a 5 s, «Sin conexión») y reconexión.
- **Evidencia:** `F3\despues-llegada-verificar.json` y `.playwright-mcp/arnes-llegada-verificar.js` (ignorado por Git).
- **F2 y F3 juntas:** `git merge-base --is-ancestor 6d4fcd1 feature/cierre-latencia` → sí, así que la rama candidata contiene las dos mejoras. Las pruebas focalizadas de ambas sobre el mismo árbol dan **90 pruebas, 0 fallos** (incluida `DemoImprovementTests`, 14). El JAR usado para medir F3 se construyó desde ese árbol. La suite completa sigue pendiente para el candidato de F7.
- **Estado:** Samuel aprobó F3 el 15 nov 2026 y autorizó un commit local. Sin push, PR, merge ni despliegue. DEC-072 sigue «propuesta» hasta que conste la aceptación de Julián.
- **Evidencia privada:** `PING-evidencia\cierre-final\F3\` (`antes-aceptar.json`, `despues-aceptar.json`, `arnes-aceptar.js`, `limpiar-cancelados.sql` y logs). Para repetir parejas, la limpieza borra solo encuentros cancelados de la base de prueba aislada.
