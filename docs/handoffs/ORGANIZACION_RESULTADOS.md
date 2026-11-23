# Organización, Resultados y vistas por rol (DEC-079: aceptada por Samuel, pendiente de Julián)

Encargo de Samuel del 20 de noviembre de 2026 («PROMPT 1» y el PDF *PING_Tres_Propuestas_Administracion*): **A · Mesa de evento** para Organización, **B · Informe de participación** para Resultados y **C · Vistas por rol** para los accesos. No se atribuye aprobación a Julián.

- Rama local `feature/organizacion-resultados`, creada desde `b2b069f` (base de la revisión visual). No incluye las propuestas de avatares DEC-075 a DEC-078 de `feature/avatares-naturales`.
- Sin commit, push, merge ni despliegue. Nada se cambió en Render, Aiven ni datos publicados.
- Issue: no existe; borrador al final de este documento.

## Qué cambia

### Permisos (servidor)

| Ruta | Antes | Ahora |
|---|---|---|
| `/admin` y `/admin?eventId=` (Resultados) | `manage`: Organización **y Personal** del evento | Organización del evento o administración general. Personal: 403 aunque escriba la URL |
| `POST /events/{id}/attendance` (nuevo) | — | Organización del evento o administración general |
| `/organizer/events` (nuevo) | — | Solo administración general |
| `/organizer`, `/events/{id}/reviews`, `/organizer/redemptions` | Personal y Organización | Sin cambios |

El control está en `AccessPolicy` y además en `AdminController`. El resumen de cifras de Organización sigue la misma regla.

### Organización (A + C)

- **Cabecera del evento:** estado, «Cambiar evento» (enlaces, sin JavaScript), «Ver código y QR» y abrir/pausar/reanudar con las rutas existentes.
- **Pestañas:** Operación · Participantes · Resultados · Configuración. En celular forman una cuadrícula de 2 × 2.
- **Operación:**
  - «Validar actividades» y «Entregar recompensas», cada una con su contador y enlace.
  - Preparación resumida: solo lo que falta, con enlace a Configuración.
  - Resumen: registradas, conexiones completadas y canjes entregados.
- **Participantes:** buscar, retirar o readmitir (ruta existente). También enlaza a la lista y a los reportes del detalle de Resultados.
- **Configuración:** la lista de preparación con sus accesos directos, código y QR, total de puerta y equipo.
- **Personal:** evento, estado y dos acciones grandes (revisar tareas y entregar un canje). No ve pestañas, configuración ni resultados.
- **Administración general (`/organizer/events`):** tabla de eventos con «Abrir Organización» y «Permisos» (con el evento preseleccionado), «Nuevo evento», permisos por evento, resultados de todos los eventos y jerarquía de gustos.

### Resultados (B)

| Cifra | Definición |
|---|---|
| Personas registradas | Altas de participantes del evento en PING. Al lado: «disponibles ahora». |
| Personas con conexión completada | Integrantes distintos (por id, no por alias) de los encuentros `COMPLETED` del evento. Una sola consulta agrupada (`UNION ALL` + `COUNT(DISTINCT)`). Ana con Samuel y Ana con Julián: 3 personas, 2 conexiones. |
| Conexiones completadas | Encuentros `COMPLETED`, «de N propuestas» (ofrecidas + activas + completadas + canceladas). |
| Canjes entregados | Recibos `DELIVERED`, «de N canjes solicitados» (todos los recibos del evento). |
| Conversión de conexiones | Completadas ÷ propuestas (la definición que ya existía). |
| Participación sobre puerta | Personas con conexión completada ÷ total de puerta. Solo si el total es válido. |
| Puntos acreditados | Registro de la Saga (`GRANT` − `REVERSE`), antes de descontar canjes. No usa los saldos. |

Debajo aparecen los estados de las propuestas (barra simple con cifras), las opiniones voluntarias, los canjes pendientes e incidencias y el detalle existente (participantes, encuentros, reportes y canjes). `/admin` sin evento muestra una línea por evento con las cuatro cifras.

**Total de puerta.** La migración aditiva `V27` añade `events.door_attendance`, que admite nulos. Los eventos anteriores no se rellenan.

| Dato guardado | Qué se muestra |
|---|---|
| Ninguno | «Asistencia de puerta sin registrar». Sin porcentaje. |
| 0 | Sin porcentaje: no hay denominador. |
| Menor que las personas con conexión | «Requiere revisión». No se calcula el porcentaje; nunca se limita al 100 %. |
| Válido | Porcentaje, con el numerador y el denominador. |

### Revisión antes de acreditar (bloque 3)

- **Cómo distingue PING los puntos canjeables.** Hay un único saldo (`participants.points`). Todo punto positivo que acredita la Saga se puede canjear, así que el criterio es el método de comprobación de la actividad.
- **Actividades nuevas:**
  - «Comprobar al entregar el beneficio» (DEC-068) ya no se ofrece. `EncounterService.saveActivity` lo rechaza al crear una actividad o al cambiar otra a ese método.
  - Lo que necesita comprobación usa «Revisión del personal» (DEC-066). El recorrido sigue igual: el anfitrión aprueba o pide corrección, ambos confirman y la Saga acredita una sola vez. Una revisión pendiente o rechazada no suma ni altera los puntos previos.
- **Caso anterior:**
  - Una actividad que ya usaba «al entregar» lo conserva al editarla. Se muestra como «método anterior» y en Configuración se cuenta aparte.
  - Sus encuentros mantienen el método congelado.
  - Sus recibos siguen exigiendo la comprobación en el puesto de premios. Ahora llevan la etiqueta «Recibo con comprobación anterior».
- **Recibos sin comprobación pendiente:** el puesto de premios solo consulta el recibo y confirma la entrega. Una actividad «al entregar» anterior que siga activa todavía genera créditos y recibos que exigen la comprobación. La introducción de «Entregar un beneficio» ya no dice que «las actividades ya se revisaron», porque hoy no es cierto para «Confirmación de ambos».
- **Sin cambios:** Saga, saldos, datos guardados, fotos, IA y QR.
- **Regla pendiente de aprobación, no implementada:** «las actividades nuevas con puntos canjeables requieren revisión previa del personal; las de 0 puntos pueden cerrarse con confirmación de ambos».
  - *Situación actual:* una actividad nueva con puntos y «Confirmación de ambos» se acredita sin revisión. Medido en MySQL: 15 + 15 puntos y dos pasos `GRANT`.
  - El texto, lo que conserva y los cuatro puntos por decidir están en DEC-079. No se toca el código hasta que consten la aceptación de Samuel y la de Julián.
- **Regla aceptada por Samuel el 20 de noviembre de 2026, pendiente de Julián:** en eventos nuevos, toda actividad con puntos canjeables requiere revisión previa; el modo básico cierra sin puntos canjeables. El detalle está en DEC-079. Se implementa cuando conste la aceptación de Julián, junto con este PREDEV:

  > **Texto preparado para el PREDEV.** Se añadirá como sección final de `PING_PREDEV_v1.1.md` al registrar la aceptación de Julián; hoy no está aplicado.
  >
  > ## Evolución DEC-079 — Revisión antes de acreditar puntos canjeables
  >
  > Aceptación de Samuel (20 de noviembre de 2026) y de Julián (fecha de su registro).
  > - **Eventos nuevos:** creados después de activar la regla. Abrir uno después no lo convierte en nuevo.
  >   - Toda actividad con puntos (1–100) usa «Revisión del personal» (DEC-066): el personal aprueba o pide corrección, ambos confirman y la Saga acredita una vez.
  >   - El modo básico (sin actividades estructuradas) y las actividades de 0 puntos completan la conexión con confirmación bilateral. Cuentan en Resultados, pero no acreditan puntos.
  >   - En esos eventos sustituye a «conexión/actividad completada: +10 puntos» y a «+10, Saga y cierre controlado intactos» (DEC-041).
  > - **Eventos existentes:** conservan su política de puntos, también para las actividades que se añadan después.
  > - **Encuentros ya iniciados:** conservan su método, sus puntos y la Saga.
  > - **Actividades anteriores:** cambian de método solo por acción explícita del organizador, y solo para encuentros futuros.
  > - **En cualquier evento:** «Comprobar al entregar» (DEC-068) no se ofrece para actividades nuevas.
  > - **Sin cambios:** saldos, recibos, matching, aceptación y confirmación bilateral.

  **Plan de implementación (al registrarse la aceptación de Julián):**
  1. `V28`, aditiva: un indicador de política por evento. Falso para los eventos existentes y verdadero al crear uno nuevo. Abrir, pausar o reanudar no lo cambia.
  2. `EncounterService.saveActivity`: solo en eventos con el indicador, rechazar puntos > 0 sin «Revisión del personal» al crear una actividad o al cambiar su método o sus puntos. Los eventos existentes no cambian.
  3. Modo básico con el indicador: 0 puntos al activar. La conexión cuenta en Resultados igual que hoy. Los encuentros activos conservan lo guardado.
  4. Editor: ofrecer solo los métodos válidos y explicar el motivo.
  5. Verificación local, con suite y capturas de 390, 360 y escritorio:
     - **Evento nuevo:**
       - Una actividad de 0 puntos se completa: cuenta en Resultados sin acreditar.
       - Una actividad con puntos espera la aprobación y luego acredita una vez.
       - El modo básico cierra sin puntos.
     - **Evento existente:**
       - Al agregarle una actividad con puntos, conserva la política actual y se acredita con confirmación de ambos.
       - Un encuentro ya iniciado conserva su método.
       - «Comprobar al entregar» sigue rechazado.
- **Canjes no entregados:** sigue el comportamiento actual. Al retirar a una persona se anulan y su coste vuelve una vez (DEC-068). Fuera de ese caso, un canje sin entregar no se devuelve. La reversión general queda como propuesta separada.

## Verificación

- **Pruebas nuevas:**
  - `EventResultsTests`:
    - Persona repetida en dos conexiones y dos alias «Ana» distintos.
    - Proposiciones ofrecida, activa y cancelada.
    - Aislamiento entre eventos y evento vacío.
    - Total de puerta ausente, 0, inconsistente, válido y quitado.
    - Roles: personal, organización y administración general, en eventos propios y ajenos, por HTTP.
    - Regla de «al entregar».
  - `ActivityReviewTests.correctionKeepsEarlierBalanceAndRetryCreditsOnce`: revisión rechazada con saldo anterior de 40 y reintento con un único crédito (65; un solo intento de Saga).
- **Pruebas ajustadas:**
  - Textos renombrados en `OrganizerBoardTests`, `DemoImprovementTests`, `FinalMvpTests` y `RewardTests`.
  - En `DeliveryEvidenceTests`, la actividad «al entregar» se crea como dato anterior.
- **Suite completa:** 414 pruebas, 0 fallos.
- **Capturas:** Chromium con emulación móvil (390 y 360 px), escritorio (1366 px) y ambos temas, con datos ficticios en H2. Están en [`../screenshots/organizacion-resultados/`](../screenshots/organizacion-resultados/). Ninguna tiene desborde horizontal.
- **Segunda revisión de Codex (solo lectura, comprobación de MySQL y regla):**
  - Encontró y se corrigieron:
    - Las propiedades de prueba tenían la prioridad más baja: `validate` no se aplicaba y otra base podía imponerse.
    - Faltaban tipos completos, índices y restricciones en la comparación del esquema.
    - La serialización de filas era ambigua.
    - La salvaguarda de la URL era débil.
    - El conteo de `GRANT` no se comprobaba.
    - Había una contradicción en el texto de la regla.
    - El límite de los eventos sin catálogo estructurado estaba mal descrito.
    - Los recibos nuevos de una actividad «al entregar» que sigue activa aún exigen comprobación; se corrigió también el texto de «Entregar un beneficio».
  - Después de corregir, la comprobación se repitió completa sobre una base nueva.
- **Revisión de Codex (solo lectura):** detectó un bloqueo real, que se reprodujo leyendo `event-editor.js`. Al editar en la misma página una actividad «al entregar», la opción no existía en el formulario, ninguna quedaba marcada y el servidor recibía «Confirmación de ambos».
  - **Corrección:** la opción «método anterior» siempre está en el HTML, oculta, y el script la muestra solo al editar una actividad que ya la usa.
  - **Comprobación en el navegador:** oculta al abrir la página y al editar otra actividad; visible y marcada en la antigua; tras cambiar el título y guardar, sigue en `AT_DELIVERY`. Captura `15-actividad-anterior-en-edicion-390-oscuro`.
- **Actualización V26 → V27 en MySQL** (`DoorAttendanceUpgradeMySqlCheck`, manual y fuera de la suite): se usó una MySQL 8.4.9 nueva y aislada (127.0.0.1:3323, carpeta temporal). No se usó Aiven, producción, la MySQL del ensayo ni el servicio `MySQL84`.
  1. PING arranca con Flyway detenido en V26 y `validate`, y se rellena con datos ficticios mediante los servicios reales:
     - 4 encuentros completados por la Saga, uno de ellos con revisión del personal aprobada.
     - 1 propuesta cancelada, opiniones, 2 canjes (1 entregado) y una actividad «al entregar» anterior.
     - Eventos abierto, pausado y en borrador.
  2. Se toma una huella SHA-256 de cada fila de las 44 tablas, leyendo solo las columnas de V26 y con una serialización sin ambigüedades (nulos marcados, longitud de cada valor). También se registra el esquema: tablas, columnas con tipo completo, índices, restricciones y claves foráneas, 705 elementos en total.
  3. Flyway aplica lo pendiente, como en un despliegue: solo V27, en 77 a 83 ms según la ejecución.
  4. Resultado:
     - Esquema: la única diferencia es `events.door_attendance int NULL`.
     - PING arrancó con `ddl-auto=validate` en V26 y en V27. Se pasa por argumentos de línea de comandos para que `application.properties` (`none`) no lo anule, y la prueba lo comprueba.
     - Datos: las 715 filas son idénticas, igual que los saldos, el registro de la Saga (4 × `PREPARE_MATCH`, `GRANT_FIRST`, `GRANT_SECOND` y `CLOSE`) y el historial V1–V26. Ningún evento recibe total de puerta.
  5. PING vuelve a arrancar con `validate`. Resultados muestra el evento anterior como «sin registrar», y al guardar 70 calcula 10 %.

  Para repetirlo, con una base nueva y vacía cuyo nombre contenga `_exclusive`:
  `$env:PING_MYSQL_EXCLUSIVE_URL='jdbc:mysql://127.0.0.1:3323/ping_v27_exclusive?useSSL=false&allowPublicKeyRetrieval=true'; .\mvnw.cmd test "-Dtest=DoorAttendanceUpgradeMySqlCheck"`

  La comprobación se niega a correr si la URL no apunta a un único host local, si el nombre de la base no termina en `_exclusive` o si la base no está vacía. Se comprobó con una URL de varios hosts, que fue rechazada antes de conectarse.
- **Límites:**
  - Chromium emulado no sustituye a un teléfono real.
  - Los datos de V26 se sembraron con el código de esta rama. El código de Saga, saldos y canjes no cambia en ella, y las entidades se validaron contra el esquema V26.
  - No se probó con una copia de datos reales, por regla.

## Cómo abrir el candidato local

```powershell
Set-Location -LiteralPath "$env:USERPROFILE\PING-integracion"
git switch feature/organizacion-resultados
.\mvnw.cmd test "-Dtest=ResultsShowcase" "-Dping.showcase=true"
```

Abre <http://127.0.0.1:8096/login>. Todas las cuentas usan la contraseña `Ensayo-ficticio-2026`:

| Cuenta | Rol |
|---|---|
| `admin@ping.test` | Administración general |
| `organizacion@ping.test` | Organización de los tres eventos |
| `personal@ping.test` | Personal de «Bienvenida universitaria» |
| `participante@ping.test` | Participante «Quique», sin match (Mi Ping y «Actividades abiertas») |
| `gabi@ping.test` | Participante «Gabi», con un canje pendiente para que Personal lo entregue |

El servidor usa H2 en memoria y se detiene con Ctrl+C o a los 120 minutos (`-Dping.showcase.minutes=N`). No toca la MySQL del ensayo. `ResultsShowcase` no forma parte de la suite: solo se ejecuta con `-Dping.showcase=true`.

## Verificación del 23 de noviembre de 2026 (versión funcional local)

Sin commit, push, merge ni despliegue. Datos ficticios en H2 y MySQL 8.4.9 aislada (127.0.0.1:3323, carpeta temporal borrada al terminar).

- **A · Organización y Resultados.** No había otra versión del panel: `C:\Users\river\PING` (rama `feature/redesign-admin-mi-ping-verification`) contiene un rediseño anterior (DEC-049/050). En la demo: Organización y Administración general abren `/admin?eventId=1` (200). Personal recibe 403 en `/admin?eventId=1` y `/admin`, abre revisar tareas y canjes (200) y entregó el canje de Gabi («Entrega confirmada»).
- **B · Actividades y puntos.** La regla V28 de DEC-079 sigue sin implementar porque falta la aceptación de Julián. Prueba nueva `EventResultsTests.zeroPointActivityIsNotTheBasicModeAndBothCountAsConnections`: una actividad de 0 puntos cierra con confirmación de ambos, cuenta en Resultados y no acredita; el modo básico sigue dando +10; una actividad de 15 puntos con confirmación de ambos acredita sin revisión (dos `GRANT`). Ajuste de texto: un encuentro de 0 puntos muestra «Sin puntos» en vez de «+0 puntos» (Mi Ping, conexiones, encuentro y detalle de Resultados).
- **C · Actividades abiertas (DEC-080, pendiente de Julián).** Reutiliza la agenda: tarjeta en Mi Ping, fila con contador en Organización › Operación y nuevo nombre en sus páginas. Hora en formato de 12 horas («3:00 p. m.»). Prueba nueva `EventExtrasTests.organizerPublishesAnOpenActivityThatAParticipantSeesWithoutAMatch`. En la demo: Organización publicó «Taller de fotografía · 3:00 p. m. · Patio central» y Quique, sin match y con 0 puntos, la ve; gestionar le devuelve 403.
- **Revisión de Codex (solo lectura) y correcciones:** el total de puerta normalizaba antes de validar (`12.5` se guardaba como 125 y `...` borraba el total). Ahora solo acepta dígitos o miles bien agrupados (`1.200`), y las entradas inválidas no cambian lo guardado. Se añadieron casos HTTP. La pantalla del encuentro de 0 puntos aún decía «recibió +0 puntos»; ahora dice «Esta conexión no acredita puntos». También recordó que DEC-079 y DEC-080 siguen pendientes de Julián antes de cualquier merge.
- **Suite:** 416 pruebas, 0 fallos. **V26 → V27** repetida en MySQL aislada: 715 filas idénticas, 705 elementos de esquema comparados y solo `events.door_attendance` nueva.
- **Capturas:** `30`–`38` en [`../screenshots/organizacion-resultados/`](../screenshots/organizacion-resultados/), a 390 px, 360 px, en tema claro y en escritorio, sin desborde horizontal. Se usó Chromium de escritorio redimensionado, sin emulación táctil.

## Cifras: capacidad actual frente a ejemplo

- **Capacidad actual:** qué mide cada cifra y cómo se calcula (tabla de Resultados). Samuel puede enseñarlo como funcionalidad implementada y probada.
- **Ejemplos:** todos los números de las capturas y del candidato (19 registradas, 9 personas con conexión, 6 conexiones, 3 canjes, 13 %, 23 %…). Salen de datos ficticios sembrados para la demostración. No son resultados de un evento ni de un piloto.

## Borrador de Issue (no publicado)

**Título:** Organización por evento, Resultados explicados y vistas por rol (DEC-079)

**Alcance:**
- Resultados solo para la organización del evento y la administración general, comprobado en el servidor.
- Organización con Operación, Participantes, Resultados y Configuración.
- Vista de Personal con dos tareas.
- Administración general con eventos, permisos y catálogo.
- Resultados con personas únicas, total de puerta opcional (`V27`) y definiciones explícitas.
- «Comprobar al entregar» retirado para actividades nuevas, con el caso anterior identificado.

**Fuera de alcance:** reversión de canjes, encuesta y embudo histórico. Las actividades abiertas van aparte, en DEC-080.

**Aceptación:** Samuel, el 20 de noviembre de 2026. Pendiente: Julián.
