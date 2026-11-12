# Conectar como recorrido por estados reales · DEC-070

12 de noviembre de 2026. Rama local `feature/connect-state-cards` en `C:\Users\river\PING-integracion`, creada desde `8d3e18f` (DEC-069, sobre `00483fa`). No había commits remotos más nuevos (`git fetch`). `8d3e18f` es la base de código, no una afirmación sobre lo desplegado. Sin commit, push, merge ni despliegue.

## Regla

Hay una tarjeta operativa por pantalla. El servidor decide la fase desde la perspectiva de cada participante y la plantilla solo renderiza esa tarjeta. El contenido de pasos futuros **no está en la respuesta HTML**: no se oculta con CSS ni va en acordeones o pestañas.

| Fase (`EncounterProgress.phase`) | Cuándo | Qué se ve | Qué no está en el HTML |
|---|---|---|---|
| `arrive` · `go` / `wait` | Política 2 y falta alguna llegada | Lugar con sus indicaciones; «Ver croquis» si existe; palabra; pista de la otra persona; «Ya llegué», o «Esperando a …» | Actividad, puntos, QR, campos de código, confirmar |
| `verify` · `both` / `scan` / `show` | Ambos llegaron y falta alguna verificación | «Verifica a …» / «… ya te verificó. Escanea su QR» / «Muéstrale tu QR a …»; dos líneas de estado; vistas de la tarjeta: elegir, su código (escanear o escribir) y mi QR (solo a petición) | Actividad, puntos y confirmar. Mi QR solo con `?view=mine` y mientras la otra persona aún deba escanearlo |
| `activity` · `confirm` / `mark` / `submit` / `fix` / `wait` / `review` / `retry` | Igual que `MatchView.canConfirm()`, la condición que ya valida `Match.confirm` | Nombre, «+N puntos por persona al completarla», «Qué hacer», «Cómo se comprueba» (revisión del personal), «Qué mostrar al recoger el premio» (al entregar) y una zona de acción | Formularios de código |
| `finishing` | La Saga está pendiente | «Finalizando tu encuentro» + «Reintentar finalización» | — |

En la política 1 (encuentros anteriores) no se añaden la llegada ni los códigos: la tarjeta es Actividad desde el inicio. El lugar y la palabra quedan como contexto, y «Ya llegué (opcional, no da puntos)» se conserva.

## Archivos

- **Servidor (solo presentación):**
  - `EncounterProgress`: fase, subestado y textos.
  - `phase()` compartido con `MatchLiveState`: `/me/state` añade `step`, sin contenido privado.
  - `DemoController`: parámetro `view` (lista blanca `mine`/`theirs`, validado contra el subestado).
  - `CredentialController`, `ActivityReviewController` y `DemoController` redirigen a `#am-step`.
  - `MatchView`: `otherTopics()`, `directionsLead()` y `directionsMore()`.
  - `FloorplanViewController`: el croquis del encuentro no muestra la actividad antes de desbloquearla.
- **Plantillas:**
  - `demo/active-mission.html`, reescrita como tarjeta por fase.
  - `demo/match.html`: propuesta con máximo tres chips y «Ver todos (N)» / «Ver menos»; acción antes de la explicación; pista propia solo en Llegar y plegada.
  - `demo/connect.html`: chips.
  - `floorplan/participant.html`.
  - `eventextras/help.html`: nombres de botones.
- **JS:**
  - `encounter-step.js` (nuevo): vistas de Verificar y foco en la tarjeta tras una acción.
  - `live.js`: si cambia el paso, cierra la cámara, descarta el código escrito y recarga.
  - `qr-scanner.js`: escucha `ping:step-changed`.
- **CSS:** bloque DEC-070 en `cierre.css`, con tokens existentes.
- **Docs:** `DECISION_LOG.md` (DEC-070), `GUIA_USUARIO_PING.md`, este documento y `docs/screenshots/conectar-dec070/`.

Sin migraciones ni cambios en reglas de puntos, Saga, permisos, CSRF, caducidad, renovación o límite de intentos de los códigos.

## Pruebas

- **Suite completa sobre el código final:** `./mvnw -o -B test` → **398 pruebas, 0 fallos, BUILD SUCCESS**.
- **Pruebas nuevas:**
  - `VerificationTests.eachSessionSeesOnlyTheCardOfItsRealStep`: dos sesiones HTTP. Cubre:
    - qué contiene y qué no contiene el HTML en cada fase;
    - código inválido, caducado y renovado;
    - POST adelantado → 409;
    - croquis sin actividad;
    - una sola acción principal;
    - recarga;
    - abono único.
  - `EncounterProgressTests`: política 1, revisión del personal (enviar, corregir, en revisión, confirmar durante la revisión), al entregar y finalizando.
- **Pruebas ampliadas:**
  - `MatchLiveStateTests`: el `step` del estado en vivo coincide con el que renderiza la página.
  - `VerificationTests`: un encuentro cancelado informa su propio `step`.
- **Pruebas actualizadas al nuevo marcado,** sin quitar comprobaciones de privacidad ni de confirmación bilateral: `EventExperienceTests`, `EncounterConfigurationTests`, `MatchTests` (ancla `am-step`), `ActivityReviewTests`, `DeliveryEvidenceTests` y `FloorplanViewTests`.

### En la app: JAR final, MySQL 8.4 aislado (`ping_cards`), cuentas ficticias, dos o más sesiones de Chromium

| Caso | Resultado |
|---|---|
| Propuesta: A acepta y espera; B acepta | A ve «Esperando respuesta de Beto…»; su página pasa sola a Llegar. Sin lugar, actividad ni códigos en el HTML de la propuesta |
| Cero llegadas | Ambos: solo Llegar. HTML sin actividad, «+20 puntos», confirmar, formularios de código ni «Escanear su QR» |
| Solo A llega | A: «Esperando a Beto», sin «Ya llegué». B se actualiza solo a «Eva ya llegó.» y conserva «Ya llegué». Ninguno ve Verificar ni Actividad |
| Ambos llegan | Ambos en Verificar (`both`); A cambia sola de Llegar a Verificar |
| Código incorrecto | «El código no es válido…» en la misma tarjeta; sigue en Verificar, con el campo disponible |
| Sin cámara (Chromium sin dispositivo) | Al pulsar «Escanear su QR», el lector queda en «Solicitando acceso a la cámara…» y el campo manual sigue visible y usable. «Volver» regresa a elegir |
| Una verificación | A (verificada) pasa sola a «Beto ya te verificó. Escanea su QR», sin «Mostrar mi QR». B: «Muéstrale tu QR a Eva», sin escáner. La actividad no aparece en el HTML de ninguno |
| Ambas verificaciones | Ambos ven Actividad, con una sola acción principal; recargar mantiene la fase |
| A confirma | A: «Esperando a Beto», con «Tú · confirmado». B se actualiza sola a «Eva ya confirmó. Falta tu confirmación.» |
| B completa y repite el POST | «Conexión completada 🎉». El POST repetido no añade nada y no aparece botón de confirmar. Saldos de 40 (20 + 20). En la base, 2 `GRANT_*` por encuentro completado |
| Revisión del personal + política 1 | Actividad desde el inicio. Enviar, corregir (comentario en la tarjeta), reenviar, confirmar durante la revisión y aprobar → completado. Cada paso se actualiza solo en la otra sesión |
| Comprobar al entregar | «Qué mostrar al recoger el premio» con el nombre del método. Ambos marcan; el resultado indica qué mostrar al recoger |
| Cancelación con un código escrito en la otra sesión | La otra sesión cierra el formulario, descarta el código y pasa a «Otra afinidad te espera.». Ni el HTML ni el DOM conservan código, lector ni acciones |
| 390 / 360 / 1280 y tema claro | Sin desplazamiento horizontal. Título, instrucción y acción principal juntos a 390 y 360 en Llegar, Verificar, Actividad y Al entregar |

Durante la prueba en el navegador se encontraron y corrigieron estos defectos:
- «Aceptar conexión» quedaba fuera de la pantalla en la propuesta.
- El QR propio aparecía bajo los textos de estado.
- `th:if` se ignoraba junto a `th:replace` y duplicaba «Actualizar envío».
- Un encuentro cancelado no informaba `step`, así que se conservaba el código escrito.
- La acción de «Al entregar» y la corrección del personal quedaban bajo la barra inferior.

## Capturas

Están en `docs/screenshots/conectar-dec070/`. Los QR y los códigos personales están tapados; las cuentas y las palabras son ficticias.

| Pantalla | Archivos |
|---|---|
| Propuesta | `10-offer-390/360`, `11-offer-waiting-390` |
| Llegar | `12-arrive-390/360` |
| Esperando la llegada | `13-arrive-waiting-390/360`, `14-arrive-other-arrived-390` |
| Verificar | `15-verify-390/360`; error en `16-verify-wrong-390`; sin cámara en `17-scan-no-camera-390`; mi QR en `18-my-qr-390` |
| Una sola verificación, desde cada celular | `19-verify-one-A-390/360`, `19-verify-one-B-390/360` |
| Actividad | `20-activity-390/360` |
| Esperando la confirmación | `21-activity-waiting-390/360`, `22-activity-other-confirmed-390` |
| Resultado | `23-result-390/360` |
| Revisión del personal | `33-review-fix-390` |
| Al entregar | `36-delivery-activity-390/360`, `37-delivery-result-390` |
| Cancelación | `38-cancelled-390` |
| Escritorio y tema claro | `40-arrive-1280`, `41-arrive-light-390` |

## Figma

Archivo PING, página nueva «PING · Conectar por estados · DEC-070 (12 nov 2026)» (`215:262`). Contiene:
- cinco marcos móviles editables (Llegar, Espera de llegada, Verificar, Actividad y Espera final), con las variables PING V2 y los estilos de texto V2;
- componentes locales: botón principal, botón secundario y estado (Pendiente/Hecho);
- un prototipo que encadena las acciones principales.

La implementación se hizo antes que el diseño: los marcos reproducen lo implementado y probado. Los diseños aprobados no se tocaron.

## Pendiente comprobable

- **Cámara real:** no hubo prueba física con iPhone/Safari o Android ni escaneo de un QR en pantalla. En Chromium sin dispositivo, el lector queda en «Solicitando acceso…» y el modo manual funciona.
- **Lector de pantalla real:** sin probar. El foco va al título de la tarjeta tras cada acción y en cada cambio de paso; los estados de espera usan `role="status"`.
- **«Ver croquis» con plano subido:** el enlace a un plano subido (`/matches/{id}/plan`) solo se probó por HTTP; en el navegador se probó el enlace del mapa SOFA. Volver del croquis carga el paso real del servidor.
- **Fuera de alcance:** la pestaña Evento (`/events/{id}/hub`, a la que lleva `/me/event`) sigue mostrando «Tu actividad asignada» y «Pendientes +N» antes de verificar. Lo hace con `EncounterService.guide(…, active, …)`. Mi Ping no lo muestra: solo enseña la última conexión completada. Evento no se modificó, por la restricción de no tocar otros recorridos. Si se quiere la misma regla allí, hace falta decidirlo.
- Aceptación de DEC-070 por Samuel y Julián.
