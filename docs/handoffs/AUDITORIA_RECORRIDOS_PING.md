# Auditoría de recorridos y facilidad de uso de PING

Encargo de Samuel del 8 de noviembre de 2026 (sesión de Claude Code), continuación del cierre DEC-066. No se atribuye aprobación a Julián. **Sin push, merge, despliegue, Render ni Aiven; producción solo lectura.** Documento único de continuidad de esta auditoría.

## Versión evaluada

| Qué | Versión | Cómo se comprobó |
|---|---|---|
| Web pública `ping-b63o.onrender.com` | **SHA desplegado no verificado.** Sus CSS/JS públicos coinciden con los de `7f20b49` | Huellas de `v2.css` (`3e535fc8…`, de los commits revisados solo coincide con `7f20b49`), `brand.css`, `ui.js`, `qr-scanner.js` antiguo y ausencia de `cierre.css`. Esto solo prueba que los recursos estáticos son iguales: el código Java, la configuración y las migraciones aplicadas no son observables desde fuera. Para afirmar el SHA hace falta el panel de Render o un endpoint de versión. |
| Rama de despliegue `origin/deploy/render-demo` | `3a528ca` | `git fetch`. No coincide con la web pública. |
| `origin/main` | `97f9b43` | Muy anterior; no representa el producto. |
| **Auditada (local)** | `4c0f0a6` (DEC-066, sin publicar) → `7b33430` → `b841a67` (ronda 3) → **`2a6f431`** (ronda 4) | Rama `feature/auditoria-recorridos` desde `feature/cierre-usabilidad`, worktree `C:\Users\river\PING-cierre`. MySQL 8.4 aislado (3317), buzón local (1027), app en 8091. |

## Herramientas

- **gstack** (`garrytan/gstack`, MIT, commit `b6155921`, `VERSION 1.98.0.0`): **no instalado**. Leídos `README.md`, `setup.ps1`, `setup` y `qa-only/SKILL.md` en esa revisión. Motivos: (1) **Smart App Control activado** en este Windows (`VerifiedAndReputablePolicyState=1`); gstack documenta que bloquea sus binarios compilados sin firmar (`browse`, base de `/qa-only`, `/qa` y `/design-review`) y las alternativas son WSL o desactivar Smart App Control, un cambio de seguridad global e irreversible sin reinstalar Windows; (2) requiere Bun (ausente); (3) el instalador escribe en `~/.claude/skills/gstack` y `~/.gstack`, sigue un canal con comprobación de actualizaciones y en Claude Code activa hooks de seguridad. Nada se instaló ni configuró. Se aplicó su método: primera pasada solo diagnóstico con evidencia, después corrección con regresión, después comprobación visual antes/después.
- **Navegador:** arnés propio por CDP sobre Chrome sin interfaz (perfiles separados por persona), el mismo del cierre DEC-066; fuera del repositorio.
- **axe-core 4.14.0** (npm, 5 oct 2026, MPL-2.0, sin dependencias, SHA-256 `20c09fe1…`): descargado en el scratchpad e inyectado por CDP; no se añadió al proyecto. Reglas WCAG 2.0/2.1 A y AA. El contraste queda «incompleto» en fondos con degradado: se revisó en capturas.
- Skills del proyecto: `ping-efficient-work`, `systematic-debugging` (fallos de compilación y de prueba). Figma: no hizo falta componer pantallas nuevas (los cambios son de texto, enlaces, posición pegajosa y tamaño); siguen vigentes los marcos de DEC-066 (`188:262`).

## Mapa de recorridos y cobertura real

| Recorrido | Rol | Cubierto en la app real | No cubierto |
|---|---|---|---|
| Inicio, unirse por código (vacío, erróneo, minúsculas), QR | Visitante | Sí (390; QR con cámara simulada en DEC-066) | Cámara física |
| Registro (errores), verificación por correo, acceso (contraseña errónea), recuperación, continuar al evento | Cuenta | Sí, con buzón local | Correo real; **Google**: botón visto en producción (solo lectura); flujo completo no probado (crearía una cuenta real) |
| Identidad, avatar, intereses (editar), Mi Ping, pista de reconocimiento | Participante | Sí (390, 360 claro, 320) | — |
| Búsqueda, propuesta, aceptación, llegada, espera, verificación, cancelar | Conexión | Sí | — |
| Actividades: catálogo, asignada, revisión, puntos | Participante / personal | Sí (DEC-066 y esta pasada) | — |
| Beneficios: catálogo, saldo, canje, recibo, entrega | Participante / personal | Sí | — |
| Organización: tablero, crear evento, editor (lugares, actividades, entrega), plano (carga y colocación), recompensas, revisión, entrega, permisos, agenda, ayuda y opciones, resultados | Organizador | Sí (390; tablero/editor/revisión también a 1366 claro) | — |
| Ayuda, agenda, reto, tarjeta, conexiones anteriores | Participante | Sí | — |
| Teclado (orden y foco visible), movimiento reducido, 320 px (equivalente a ampliación 200 %) | Todos | Muestreo: foco 3 px visible; sin animaciones con `reduce`; sin desbordes a 320 | Lector de pantalla real, ampliación de texto del sistema |

## Hallazgos priorizados (primera pasada sobre `4c0f0a6`)

Tipo: **F** fallo reproducido · **R** riesgo de comprensión inferido · **E** preferencia estética · **C** comprobación pendiente.

| Id | Tipo · prio | Tarea · rol · estado | Reproducción → observado | Problema | Propuesta | Estado · evidencia |
|---|---|---|---|---|---|---|
| A1 | F · P1 | Mi Ping · participante | Tocar el nombre del evento → `/events/1` → «Acceso restringido» | Enlace a una página de organización | Enlazar a la pestaña Evento | **Corregido** · `antes/01b`, `despues/01` |
| A2 | R · P1 | Encuentro · esperando a la otra persona | La única salida («Cancelar conexión») estaba en un desplegable cerrado al final: «Cuidemos este encuentro · Gestionar esta interacción» | No hay salida visible ante ausencia | Enlace «¿No llega o hay un problema? Ver opciones» en la tarjeta de espera, panel renombrado «¿Algún problema? Cancelar, bloquear o reportar», el enlace abre el panel | **Corregido** · `antes/02`, `despues/02`, `03` |
| A3 | R · P1 | Ayuda del encuentro | Paso 5: «Los puntos llegan cuando se completa la confirmación bilateral»; sin instrucción para salir | Instrucción que no coincide con la revisión del personal | Texto según reglas reales y apartado «Si algo no va bien» | **Corregido** · `antes/03`, `despues/03b` |
| A4 | R · P1 | Editor del evento · organizador | Pestaña «Puntos» y «Puntos compatibles» (lugares) junto a «Recompensa · 0–100 puntos» | Mezcla lugares con puntos canjeables | «Lugares», «Lugares de encuentro», «Lugares donde puede hacerse»; en el plano, «lugar» | **Corregido** · `antes/06`, `despues/06`, `07` |
| A5 | R · P1 | Agenda · organizador | «Añadir actividad» / «actividades publicadas» en la agenda | Mezcla agenda con actividades del encuentro | «Añadir a la agenda», «elementos en la agenda» | **Corregido** · `antes/08`, `despues/08` |
| A6 | R · P1 | Tablero · organizador con 2+ eventos | «Mis eventos» volvía al evento más reciente, no al que se estaba administrando | Se pierde el contexto | El tablero recuerda el evento (solo el id en la sesión, comprobando permiso en cada petición) | **Corregido** |
| A7 | R · P2 | Subpáginas de organización | Volver con cinco nombres distintos («Mi evento», «Gestionar evento», «Detalle del evento»…) | «¿Dónde estoy?» | «← Organización» al tablero del evento; el detalle ofrece «Preparar y operar este evento» | **Corregido** |
| A8 | F · P2 | Plano · organizador | El lugar archivado aparecía y contaba como «sin ubicar» | Historia mezclada con lo vigente | Excluir archivados | **Corregido** · `antes/07`, `despues/07` |
| A9 | F · P2 · a11y | Mi Ping | axe `label-content-name-mismatch` (2): avatar y saldo con `aria-label` distinto del texto visible (WCAG 2.5.3) | Voz/lectores | Nombre accesible con el texto visible | **Corregido** (axe sin violaciones) |
| A10 | E · P2 | Mi Ping | «Feria de Bienvenida U ·» con el separador colgado al partir la línea | Ruido | Evento y saldo en dos líneas | **Corregido** |
| A11 | R · P2 | Editar intereses | «Guardar intereses» quedaba bajo la barra inferior (no pegajoso, a diferencia de la entrada) | Elegir y no guardar | Barra pegajosa sobre la navegación | **Corregido** · `antes/04`, `despues/04` |
| A12 | R · P2 | Encuentro · panel de problemas | Bajo «Cancelar conexión» decía «…No borra el historial ni cancela esta conexión» (del bloqueo) | Parece contradictorio | Explicación propia para cancelar y para bloquear | **Corregido** · `despues/03` |
| A13 | R · P2 | Beneficios · participante | En móvil, «Solicitar canje» queda ~1,2 pantallas abajo (imagen + cupón) | Acción poco visible | Imagen 2:1 en ≤420 px (ronda 1); en la ronda 4 «Solicitar canje» sube justo después de «Te alcanza» sin cambiar la cabecera ni el coste del cupón D14 | **Corregido en la ronda 4** · `cobertura/antes/04`, `cobertura/despues/04` |
| A14 | R · P2 | Plano · organizador | Texto técnico «…requieren su propio respaldo; el repositorio solo conserva el código» | Jerga de desarrollo | Texto para organización | **Corregido** |
| A15 | R · P2 | Entrada | «1 de 3 · Tu evento» y después registro/verificación sin numerar, luego «Paso 2 de 3» | Pasos que no cuentan la cuenta | Decidir si la cuenta es un paso visible | **Abierto** (cambio del recorrido de entrada aprobado; se observa en la prueba con personas) |
| A16 | C | Google | Botón presente en producción | Flujo real no probado | Probar con una cuenta de prueba autorizada | **Pendiente** |
| A17 | C | Cámara | Lectura y alternancia probadas con cámaras simuladas | Safari iPhone/Android no certificados | Prueba física | **Pendiente** |
| A18 | R · P2 | Editor de lugares | Se pueden crear dos lugares con el mismo nombre (DEC-066 H10) | Ambigüedad | Aviso de nombre repetido | **Abierto** |

Sin defectos en esta pasada: códigos de evento erróneos/vacíos/minúsculas, registro con contraseñas distintas (conserva el correo), contraseña errónea (mensaje y enlace a crear cuenta), recuperación con respuesta común, vuelta de la participante a Mi Ping con «Volver al encuentro», permisos 403 para participantes y otros eventos, foco visible, movimiento reducido, sin desbordes a 320/360/390/1366, sin errores JS en los recorridos.

## Actividades, verificación y puntos (estado)

Implementado en DEC-066 y revisado aquí: verificación del encuentro (llegadas y códigos), validación de la actividad (confirmación de ambos o revisión del personal) y acreditación (Saga, una sola vez) están separadas en pantalla y en servidor. No hay botones «Completar» ni «Mostrar evidencia» sin operación; la foto no es obligatoria en ningún método. La ayuda ya refleja la revisión.

## Pruebas sobre la versión identificada

- Suite completa `./mvnw -o clean verify` sobre **`7b33430`**: **383 pruebas, 0 fallos, 0 errores, 0 omitidas, BUILD SUCCESS** (08/11/2026 20:23 Bogotá). Durante la corrección: un error de compilación (variable capturada en lambda) y una aserción de texto antiguo («Crea primero un punto» → «lugar»), ambos resueltos antes de la suite.
- Dirigidas: `FrontendBundleTests`, `EventExtrasTests`, `Floorplan*Tests`, `DemoImprovementTests`, `EventExperienceTests`, `OrganizerBoardTests`, `EventTests`, `ActivityReviewTests`, `MatchTests`.
- App real: cada corrección comprobada con su captura (`despues/`), axe sin violaciones en Mi Ping, enlace a opciones abre el panel (también por URL con `#connection-safety`).

## Capturas comparables

`docs/screenshots/auditoria-recorridos/antes/` (sobre `4c0f0a6`) y `despues/` (sobre `7b33430`), 390 px oscuro salvo `antes/07` (claro). Pares: 01/01b↔01 · 02↔02,03 · 03↔03b · 04↔04 · 05↔05 · 06↔06 · 07↔07 · 08↔08.

## Pendientes concretos

**iPhone (Safari) y Android, por HTTPS:** permiso de cámara (primera vez y denegado), trasera por defecto, «Usar cámara frontal», lectura de QR a 30–50 cm con luz de interior, volver desde otra app con la cámara cerrada, teclado sobre «Nota para el personal» y sobre el código manual, áreas seguras con la barra pegajosa de intereses, imagen de recompensa tomada con el móvil (orientación).

**Con personas:** guion y hoja en [auditoria-recorridos/PRUEBA_APRENDIZAJE.md](auditoria-recorridos/PRUEBA_APRENDIZAJE.md); opcional primer clic/cinco segundos en Lyssna Free (no creado).

**Decisiones:** A15 (contar la cuenta como paso), A18 y A20 (ver la ronda 4). A13 quedó corregido en la ronda 4.

## Cinco tareas para tu revisión final

1. En un celular, entra con una cuenta de participante y toca el nombre del evento en Mi Ping: debe abrir la pestaña Evento, no «Acceso restringido».
2. Acepta un encuentro, registra tu llegada y no hagas nada con la otra cuenta: comprueba que aparece «¿No llega o hay un problema? Ver opciones» y que abre directamente «Cancelar conexión».
3. Como organización, abre Configuración del evento: la pestaña dice «Lugares» y la actividad pide «Lugares donde puede hacerse»; la agenda dice «Añadir a la agenda».
4. Con dos eventos, abre uno desde el tablero, entra en su plano y pulsa «Mis eventos»: debe volver al mismo evento; el plano no ofrece lugares archivados.
5. Edita tus intereses en el celular: «Guardar intereses» debe verse sin desplazarte hasta el final.

## Siguiente acción para retomar

`cd C:\Users\river\PING-cierre && git status --short --branch && git log --oneline -4` (rama `feature/auditoria-recorridos`). Probar en el iPhone con `%LOCALAPPDATA%\PING\prueba-esta-version\PROBAR-ESTA-VERSION.cmd` y el guion [PRUEBA_IPHONE_10_MIN.md](auditoria-recorridos/PRUEBA_IPHONE_10_MIN.md). Decidir A15, A18 y A20 con Samuel. Con autorización, integrar sobre el SHA publicado (sin verificar: las huellas estáticas coinciden con `7f20b49`), ejecutar `./mvnw clean verify` y las pruebas físicas y con personas.

## Ronda 3 · Actividad asignada, envío, corrección, revisión, confirmaciones, puntos y canje

Relación en Git: historia lineal `7f20b49` → DEC-066 (`cb90ecb`…`4c0f0a6`) → `7b33430` (auditoría) → `fd7cc71` (docs) → **`b841a67`** (esta ronda). No se repitieron los recorridos ya verificados (entrada, cuenta, organización, plano, beneficios con imagen). gstack sigue sin instalar: Smart App Control no se desactivó ni se instaló WSL; se usó el arnés CDP existente.

**Recorridos comprobados en la app real (390 px, dos sesiones + personal, sin recargar salvo donde se indica):**

| Paso | Quién | Resultado observado |
|---|---|---|
| Llegada de la otra persona | Dani (esperando) | La tarjeta pasa sola a «Verifica el código de Elí» en ~4 s (sondeo cada 5 s). |
| Verificación cruzada | ambas | «Muestra tu código a…» → «Hagan la actividad y envíenla»; la otra persona se actualiza sola. |
| Envío con pregunta | Dani envía | Dani: «Confirma que hicieron la actividad». Elí (no envió) se actualiza sola y ve «Pendiente de revisión» y el texto enviado. |
| Corrección | personal | Pedir corrección sin comentario → error explicado; con comentario, ambas ven «Corrijan y vuelvan a enviar» con el motivo, sin recargar. |
| Reenvío por la otra persona | Elí | Vuelve a «Pendiente»; las dos confirman → «Esperando la revisión del personal». Tablero: «Revisar tareas · 1 pendiente». |
| Aprobación con ambas confirmaciones | personal | +15 a cada una una sola vez; la pantalla de Elí pasa sola a «Conexión completada». Evento: saldo 15, ganados 15, pendientes 0. |
| Envío **sin** pregunta | Ana | Campo «Nota para el personal (opcional)», no obligatorio; el personal ve «Sin nota: pidieron revisión en persona». La actividad ya activa conservó su pregunta al quitarla en el editor (snapshot). |
| Aprobación **sin** confirmaciones | personal | Mensaje con el resultado real (ver C3); al confirmar ambas, puntos una vez. |
| Cancelar un encuentro activo | Ana desde «¿Algún problema?» | Carolina se entera sola; texto corregido (C5). |
| Canje | (recorrido ya verificado en DEC-066 y ronda 2) | No repetido. |

**Corregido en `b841a67`** (antes/después en `docs/screenshots/auditoria-recorridos/flujo/`; `01`–`05` son de la versión anterior a `b841a67`):
- **C1** (R · P1) Quien no envió no sabía quién ni cuándo: el estado compartido dice «Enviado por ti / por Ana Lucía · 20:44». Evidencia `06`, prueba HTTP.
- **C2** (R · P1) Con la revisión pendiente había dos botones amarillos («Actualizar envío» y «Confirmar»): ahora reenviar es secundario y la acción principal es confirmar; tras una corrección el botón dice «Reenviar corregida». Antes `02`, después `06`.
- **C3** (R · P1) El mensaje al aprobar era condicional («si ambos ya confirmaron…»): ahora informa lo ocurrido: «Se acreditaron +15 puntos a cada persona, una sola vez» o «Confirmaciones: N de 2. Los puntos llegarán cuando ambas personas confirmen».
- **C4** (R · P2) La pantalla final no decía qué se completó: nombra la actividad y, si hubo revisión, que el personal la aprobó. Antes `05`, después `07`.
- **C5** (F · P2) Cancelar un encuentro ya aceptado decía «Propuesta cancelada»: ahora «Encuentro cancelado… conservas los que ya tenías». Después `08`.
- Descartado: «Saltar al contenido» visible en capturas de página completa es un artefacto de la captura (enlace fijo fuera de pantalla, `top:-80`), no un defecto.

**Pruebas:** suite completa `./mvnw -o clean verify` sobre el árbol de `b841a67`: **383 pruebas, 0 fallos, BUILD SUCCESS** (08/11/2026 20:49 Bogotá); `ActivityReviewTests` amplía la comprobación HTTP de «Enviado por ti / por Persona A».

**Pendientes físicos (iPhone Safari y Android, HTTPS):** permiso de cámara la primera vez y tras denegarlo; trasera por defecto y «Usar cámara frontal»; lectura del QR personal de la otra persona a 30–50 cm con luz de interior; cámara cerrada al cambiar de app y volver; teclado sobre «Nota para el personal» y sobre el código manual sin tapar el botón; barra pegajosa de intereses con el área segura inferior; sondeo de estado con la pantalla bloqueada y al desbloquear; foto de recompensa tomada con el móvil (orientación). Con personas: guion de `auditoria-recorridos/PRUEBA_APRENDIZAJE.md`, sin resultados todavía.

## Ronda 4 · Cobertura pendiente: entrada, perfil, canje y organización

Base: `926a6bf` (ronda 3, conservada) → **`2a6f431`** (correcciones de esta ronda) → prueba ampliada y documentación en el commit siguiente. No se repitió el recorrido de actividades: ninguna corrección toca encuentro, revisión ni Saga. No se intentó instalar gstack ni otra colección.

**Dónde se probó.** Instancia del **arranque guiado** (`INICIAR-PING.cmd` de esta copia) en `https://192.168.1.9:8095`, con su HTTPS local y una MySQL de ensayo propia (`127.0.0.1:3318`, fuera del repositorio). Es la misma que usarás desde el iPhone ([PRUEBA_IPHONE_10_MIN.md](auditoria-recorridos/PRUEBA_IPHONE_10_MIN.md)). El navegador de prueba es Chrome sin interfaz por CDP; solo acepta la clave pública de ese servidor (`--ignore-certificate-errors-spki-list`), así que la validación TLS de la app no se desactivó. Vista de 390 px en tema oscuro, más 360 px en tema claro y axe-core 4.14.0 en las pantallas modificadas. Las cuentas y los eventos son ficticios. La pasada automatizada usó su propio evento («Ensayo de cobertura»), de modo que «Feria de prueba PING» queda limpio para tu prueba.

**Qué evidencia se reutiliza.** Solo se reutiliza evidencia de rondas anteriores cuando las plantillas y el código de ese paso no cambiaron después (`git log` por archivo). Lo que solo se revisó en el código o con una prueba HTTP está marcado como tal; no cuenta como prueba en navegador.

### Cobertura

| Recorrido · paso | Comprobado | Corregido | Pendiente |
|---|---|---|---|
| Inicio → «Unirme a un evento» → código en minúsculas → «Entra a tu cuenta» con «Tu entrada a ‹evento›» | Navegador, ronda 4 · `cobertura/recorrido/01` | Inicio, paso 4: decía que los puntos llegan «al confirmar ambos»; ahora «Al completar la actividad del encuentro» (con revisión del personal también hace falta su aprobación) | QR del evento leído con la cámara física |
| Crear cuenta → «Revisa tu correo» → enlace (usa el origen HTTPS del Wi-Fi) → verificar con contraseña → identidad | Navegador, ronda 4 · `recorrido/02`, `03` | — | Correo real; Google |
| Errores de entrada (código vacío o erróneo, contraseña errónea, contraseñas distintas, recuperación) | Evidencia vigente de la ronda 1 (plantillas sin cambios desde `7b33430`) | — | — |
| Alias vacío → aviso nativo; alias y avatar (animal y gorra) → gustos | Navegador, ronda 4 | — | Cómo anuncia Safari/VoiceOver el aviso nativo |
| Gustos: elegir, contador («3 elegidos en 1 familia»), entrar a Mi Ping | Navegador, ronda 4 · `recorrido/04`, `05`. Editar gustos y barra pegajosa: ronda 1 vigente | — | Barra pegajosa con el área segura del iPhone |
| Pista de reconocimiento: guardar y editar | Navegador, ronda 4 | — | **A20** (abierto, ver abajo) |
| Editar avatar después de entrar | Ronda 1 vigente | — | — |
| Beneficios: saldo, coste, «Te alcanza · Quedarás con N», imagen propia y genérica | Navegador, ronda 4 · `cobertura/antes/04` → `despues/04`, `04b` (360 claro) | **A13**: «Solicitar canje» quedaba bajo la barra inferior en la primera pantalla (botón a 825 px en un visor de 844). Ahora va justo después de «Te alcanza» (674–726 px) y el lugar de entrega debajo. La línea «Saldo suficiente…» repetía «Te alcanza» y queda solo para lectores de pantalla | — |
| Solicitar canje → recibo (cobro único, código, caducidad, renovar sin cobro) | Navegador, ronda 4 | — | QR del recibo leído con la cámara física |
| Entrega: el personal de **otro** evento consulta un recibo | Navegador, ronda 4. Antes: página genérica «Acceso restringido» que invita a «Entrar con el código del evento». Después: `despues/07` | Ahora se explica en la misma página («Este recibo es de un evento que no administras…»), sin mostrar datos del recibo | — |
| Entrega: personal autorizado consulta (también en minúsculas) → confirmar → recibo «Entregado», sin cobro adicional | Navegador, ronda 4 · `recorrido/06` | «Resultado registrado» → «Entrega confirmada…» o «Incidencia registrada…» | — |
| Entrar como organización o personal sin participaciones | Navegador: antes `antes/03` (caía en «Únete a tu evento · 1 de 3»), después `despues/03`. Prueba HTTP en `OrganizerBoardTests` | Va a su tablero de Organización | — |
| Organización sin eventos (administración general) | Navegador, solo el antes (`antes/01`: sin título y «Pide a administración general…» a la propia administración general) | Título «Organización» y botón «Crear el primer evento»; el mensaje anterior solo lo ve quien no es general | Después: **solo revisión de código** (hace falta una base sin eventos) |
| Crear evento → detalle → «Preparar y operar este evento» → tablero | Navegador, ronda 4 | — | — |
| Tablero de un evento nuevo | Navegador, solo el antes (`antes/02`: «Lista anterior · Punto Ping del evento» e «Importar catálogo anterior · 0/3 · 0/10»). Después: prueba HTTP en `OrganizerBoardTests` | «Básico, ya funciona · … Añade tus lugares»; las herramientas de importación se ocultan si no hay nada que importar; «de3 puntos» → «de 3 lugares» | Después en navegador (los dos eventos ya tienen lugares) |
| Lugares y actividad con revisión del personal → «Preparar evento» se actualiza | Navegador, ronda 4 | — | A18 (nombres repetidos) sigue abierto |
| Recompensas: crear, imagen, sin imagen | Navegador, ronda 4. Antes, crear volvía a la lista sin confirmación y la imagen solo se descubría con «Editar recompensa e imagen» | Crear lleva a su sección de imagen con «Recompensa creada. Si quieres, añádele una imagen; es opcional» (`despues/06`); editar confirma en la lista; título «Nueva recompensa» o «Editar recompensa» | Foto hecha con el móvil |
| Permisos por evento | Navegador, ronda 4. Antes, al llegar desde «Feria de prueba PING» estaba preseleccionado otro evento | Preselecciona el evento que se administraba y confirma «Permiso asignado a X en «evento»». Un correo desconocido se explica en el mismo formulario y conserva lo escrito (`despues/05`). Antes iba al manejador general de errores (**revisión de código, no observado**) | — |
| Abrir evento y cambiar de evento (2 eventos) | Navegador, ronda 4 | — | — |
| Revisar tareas (personal) | Ronda 3 vigente (`b841a67`; sin cambios después) | — | — |
| Plano, agenda, ayuda, resultados | Rondas 1–2 vigentes (sin cambios después) | — | Plano con una foto real del recinto |
| Accesibilidad de lo modificado | axe: 0 violaciones en Inicio, Beneficios, tablero (organización y personal), Permisos, Entregar, Recompensas y editor. Sin desbordes a 390 ni a 360 | — | Lector de pantalla real |
| **iPhone por HTTPS** | Preparado y comprobado desde el PC: panel, QR, página de preparación (8096) responde 200 y TLS validado con la CA local | — | **Todo lo físico** (ver abajo) |

**A20 · abierto (R · P2).** La pista de reconocimiento acepta «llámame al 3001234567», aunque el texto pide no incluir teléfonos. Rechazarlo cambia una regla del producto y necesita decisión. Propuesta: rechazar en el servidor secuencias de siete dígitos o más y avisar en el mismo campo.

### Pruebas de la ronda 4

- Dirigidas, durante la corrección: 8 clases afectadas, 65 pruebas, 0 fallos.
- **Suite completa `./mvnw -o test` sobre `2a6f431`: 383 pruebas, 0 fallos, 0 errores, BUILD SUCCESS** (08/11/2026 22:25 Bogotá). Se usó `test` en lugar de `clean verify` porque el JAR de `target/` estaba en uso por la instancia del iPhone y Windows no permite sustituirlo.
- Después se amplió `OrganizerBoardTests` (evento nuevo sin «Lista anterior», editor sin importación vacía): 1 prueba, 0 fallos. No se repitió la suite porque el cambio es solo de esa prueba.
- La app reconstruida por el arranque guiado sirve `2a6f431`: la huella de fuentes coincide con `build.json` y `runtime.json`, y el hash del JAR es correcto. Cada corrección se comprobó en esa app. Las capturas están en `docs/screenshots/auditoria-recorridos/cobertura/{antes,despues,recorrido}` y no contienen contraseñas, enlaces con token ni códigos de recibo válidos: el recibo capturado ya está entregado y no muestra código.

### Pendiente físico y con personas

- **iPhone Safari y Android:**
  - instalar y confiar la CA de esta copia;
  - permiso de cámara (primera vez y tras denegarlo);
  - cámara trasera por defecto y «Usar cámara frontal»;
  - QR del evento, QR personal y QR del recibo leídos a 30–50 cm;
  - cámara cerrada al volver de otra app;
  - teclado sobre la nota para el personal y sobre el código manual;
  - áreas seguras;
  - estado con la pantalla bloqueada;
  - foto de recompensa hecha con el móvil.
- **Con personas:** el guion de 10 minutos ([PRUEBA_IPHONE_10_MIN.md](auditoria-recorridos/PRUEBA_IPHONE_10_MIN.md)) y el largo ([PRUEBA_APRENDIZAJE.md](auditoria-recorridos/PRUEBA_APRENDIZAJE.md)). Sin resultados todavía; no se inventan.
- **Decisiones:** A15 (contar la cuenta como paso), A18 (lugares con el mismo nombre) y A20 (teléfonos en la pista).
