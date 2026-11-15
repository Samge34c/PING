# Cierre final de PING · continuidad

Encargo de Samuel: `PING_CIERRE_FINAL_MEGA_PROMPT_V2.md` (fuera del repo, en Descargas). Cada fase tiene una rama local y una entrada aquí. Los logs y la evidencia privada quedan fuera del repo, en `C:\Users\river\PING-evidencia\cierre-final\`.

## Estado (15 de noviembre de 2026)

| Fase | Estado | Rama |
|---|---|---|
| F0 Diagnóstico | Hecho | — (solo lectura) |
| F1 Base de datos y alojamiento | Respaldo V26 **verificado**; cambio a Aiven Developer **pendiente (G2)** | — |
| F2 Gustos compactos en Mi Ping | Aprobada por Samuel; commit local | `feature/cierre-gustos` desde `f5b5a9a` |
| F3 Latencia A→B | En curso, en local | `feature/cierre-latencia` desde F2 |
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
