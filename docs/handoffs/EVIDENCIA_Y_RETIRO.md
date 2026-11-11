# «Comprobar al entregar el beneficio» y retiro de participantes (DEC-068)

Solicitud de Samuel del 11 de noviembre de 2026 (sesión de Claude Code). No se atribuye aprobación a Julián. **Sin push, merge, despliegue, Render ni Aiven, y sin tocar datos reales.** Este es el documento único de continuidad de este cambio.

## Base y rama

- Worktree `C:\Users\river\PING-cierre`. Rama local nueva `feature/evidencia-retiro`, creada desde `f9172f5` (`feature/auditoria-recorridos`; el código venía de `2a6f431`). Una tarea, una rama (AGENTS.md).
- Commits separados: migración y dominio, pantallas, pruebas, documentación (`git log --oneline f9172f5..`).

## 1 · Marcar actividad como hecha y mostrar la foto al recoger el premio

**Organización** (`/events/{id}/encounters#activities`). Hay una tercera modalidad: «Comprobar al entregar el beneficio». Pide «Qué deben mostrar al recoger el premio»; la pregunta de la revisión del personal se oculta. Las modalidades anteriores no cambian. Como en DEC-066, cada encuentro conserva la modalidad con la que empezó.

**Participante**
- En el catálogo (Evento) y en la actividad asignada se explica qué hay que hacer, cuántos puntos da y que la foto se mostrará al recoger el premio. PING no pide subir ni guardar la foto.
- Una vez verificado el encuentro, la tarjeta «Ahora» muestra el botón «Marcar actividad como hecha». Es la **misma confirmación bilateral** que ya existía (`POST /matches/{id}/confirm`), así que no se pide dos veces lo mismo.
- Si solo una persona marcó, ve «Esperando a tu compañero» y la otra ve «Ana ya la marcó como hecha». Cuando marcan las dos, la Saga existente abona los puntos a cada una una sola vez, y la pantalla de la primera se actualiza sola.
- En Beneficios aparece «20 puntos se comprueban al entregar». Se puede pedir el canje sin aprobación previa. Si el canje usa esos puntos, el recibo dice «Lleva la evidencia de tu actividad», qué mostrar y dónde, con el punto de entrega real del recibo (el predeterminado del evento o el propio de la recompensa).
- Nunca se anuncia «Foto validada» ni «Premio entregado» antes de tiempo. «Comprobada el…» solo aparece después de que el personal la registra.

**Personal** (`/organizer/redemptions`)
- Al abrir el recibo se ve «Antes de entregar: comprueba la evidencia», con la actividad, qué deben mostrar y de quién es el encuentro. Escanear el código solo identifica el recibo. «Confirmar entrega» está desactivado y el servidor también la rechaza.
- «La vi y corresponde» registra quién validó y cuándo (`evidence_validations`). No suma puntos.
- «No la tienen o no corresponde» deja una indicación (`claim_evidence_notes`) que la persona ve en su recibo. El recibo sigue pendiente: no se entrega ni se cobra otra vez. La incidencia final que ya existía no cambia.
- Una validación cubre a las dos personas del encuentro. Si el compañero canjea después, el personal ve «Ya validada · fecha · quién validó» y su recibo dice «No necesitas mostrarla otra vez».
- No puede validar quien es parte del encuentro ni el personal de otro evento.

**Registro de puntos (sin segunda billetera).** El saldo sigue siendo `participants.points`. `evidence_credits` guarda, por encuentro y persona, cuánto de ese saldo vino de una actividad «al entregar» y cuánto queda sin gastar. Al canjear se usan, en este orden:
1. los puntos normales;
2. los puntos de encuentros ya validados (se enlazan solo para mostrar «ya validada»);
3. los puntos por comprobar, empezando por los más antiguos.

Solo la parte 3 bloquea la entrega. Un canje pagado solo con puntos ya comprobados sigue el flujo normal. El bloqueo del evento, `@Version` y la clave idempotente del canje evitan que un mismo saldo pague dos recibos. La Saga abona y compensa el registro en el mismo paso que los puntos (clave primaria encuentro + persona). Repetir la validación o la entrega no tiene efecto.

## 2 · Retirar participantes del evento

- En el tablero, la sección «Durante el evento» incluye «Participantes · Gestionar» (`/events/{id}/participants`). Tiene búsqueda por alias (los comodines se tratan como texto), estado, puntos y la acción «Retirar del evento» o «Readmitir».
- **Permisos.** La administración general gestiona cualquier evento. Una cuenta con rol de **organización** gestiona solo sus eventos. El rol de **personal** (nuevo, `event_administrators.admin_role`) solo revisa tareas y entrega canjes: no ve la preparación del evento ni los participantes, y el servidor le responde 403. Esto se comprueba en `AccessPolicy` y en `EventParticipantsService`. Todos los permisos anteriores quedan como organización. En «Permisos por evento» se elige el rol.
- La confirmación muestra a quién se retira y de qué evento, con las consecuencias, y pide un motivo que solo ve la organización. Se guardan autor, fecha y motivo (`participant_withdrawals` y `auth_audit`).
- Efectos del retiro:
  - la persona deja de aparecer en las búsquedas;
  - su propuesta o encuentro abierto se cancela como una cancelación normal, y su pareja ve «La organización del evento canceló este encuentro», sin el motivo;
  - sus canjes sin entregar se anulan (`CANCELLED`), se revocan sus códigos y el coste vuelve una vez a su saldo; así no hay entregas indebidas ni puntos reservados;
  - pierde el acceso a las páginas de participante y, si vuelve con el código o el QR, lee «La organización de ese evento te retiró…».

  Se conservan la cuenta, el historial, los puntos y las entregas hechas, y los demás eventos no cambian.
- «Readmitir» devuelve el acceso con la misma participación. La persona vuelve a marcar su disponibilidad.
- Si la persona está justo en la finalización de un encuentro, el retiro se rechaza con «Espera unos segundos».

## Migración V26 (aditiva)

Crea las tablas `evidence_credits`, `evidence_validations`, `claim_evidence`, `claim_evidence_notes` y `participant_withdrawals`. Añade las columnas `event_administrators.admin_role` (por defecto `ORGANIZER`), `participants.withdrawn` y `ping_matches.cancelled_by_organization`. Además amplía `chk_claim_status` con `CANCELLED` (`DROP CONSTRAINT`, MySQL 8.0.19 o posterior y H2).

**Prueba en MySQL 8.4 aislado** (instancia nueva en el scratchpad, `127.0.0.1:3319`, sin tocar el servicio en 3306). Se migró una base vacía hasta V25, se insertaron datos ficticios anteriores (permisos, participantes, encuentro completado con `STAFF_REVIEW`, recibos pedido y entregado) y se aplicó V26. Resultado: solo se ejecuta V26 y la repetición no hace nada. Los permisos pasan a organización; los saldos, encuentros y recibos no cambian; los recibos anteriores no exigen evidencia; se acepta `CANCELLED` y se rechaza un estado desconocido; las claves primarias impiden un segundo abono o una segunda validación. Después, la app arrancó sobre MySQL con `ddl-auto=validate` y se recorrieron los flujos.

## Comprobación en el entorno aislado (app real sobre MySQL, Chrome sin interfaz)

1. Ana y Beto verifican el encuentro y lo marcan como hecho. Cada uno recibe +20 una sola vez (un único intento de la Saga).
2. Ana pide la recompensa de 10 puntos sin revisión previa y obtiene recibo y código. El personal ve «Antes de entregar…». Al forzar la entrega, el servidor responde «Comprueba primero la evidencia…». La incidencia deja el recibo pendiente y Ana ve la indicación.
3. Tras «La vi y corresponde» y la entrega, a Ana le quedan 10 puntos. Repetir no duplica nada. Con Cami y Dani: el recibo de Dani, pedido después de la validación, dice «Evidencia ya comprobada» y el personal ve «Ya validada».
4. La organización retira a Cami: se cancela su encuentro con Ana, que ve el aviso sin motivo, se anula su canje y se devuelven los puntos. Cami no puede volver con el código. El personal y la organización de otro evento reciben 403. Tras la readmisión, Cami vuelve a entrar.

Además, a Cami (readmitida y con 10 puntos ya comprobados) le salió un canje de flujo normal y el siguiente quedó ligado a la evidencia, como se esperaba.

Se encontraron y corrigieron dos defectos:
- el `LIKE … escape '\'` funcionaba en H2 pero fallaba en MySQL (error 503); se cambió a `escape '!'` y se añadió una prueba con comodines;
- la pareja que canjeaba después de la validación no veía «ya validada».

## Pruebas

- Nuevas: `DeliveryEvidenceTests` (6) y `EventParticipantsTests` (3), en servicio y HTTP.
- Suite completa: ver «Resultados» al final.

## Revisión móvil (390 px principal, 360 px claro, escritorio)

- **¿Se entiende la pantalla y su acción principal?** Sí. En el encuentro, «Marcar actividad como hecha» está en la tarjeta «Ahora». En la entrega, la evidencia aparece antes del botón de entrega, que está desactivado y explica por qué. En el retiro se ve quién, de qué evento y qué pasará.
- **¿El espacio, el texto o la decoración desplazan lo importante?** El bloque de evidencia del recibo va justo después del código. En la entrega a 390, «La vi y corresponde» queda dentro de la primera pantalla. Se acortó la etiqueta porque antes se partía junto a la barra.
- **¿La navegación tapa contenido?** No se observó. No hay desbordes horizontales a 390, 360 ni 1366, ni errores de JavaScript.
- **¿Identidad y coherencia?** Se mantiene Festival Social (negro, morado y amarillo, Manrope) usando solo tokens existentes en `cierre.css`.
- **Pendiente visto, sin cambiar:** a 360 px el código manual del recibo se parte en dos líneas. Es comportamiento anterior.

Capturas en [`docs/screenshots/evidencia-retiro/`](../screenshots/evidencia-retiro/). `09` y `13` son anteriores al cambio de etiqueta del botón.

| Paso | Captura |
|---|---|
| Editor con la modalidad | `01-editor-modalidad-390` |
| Catálogo | `02-catalogo-390` |
| Marcar / compañero ya marcó / esperando | `03-marcar-hecha-390`, `03b-companero-ya-marco-390`, `04-esperando-companero-390` |
| Completado | `06-completado-390`, `06b-completado-360-claro` |
| Beneficios | `07-beneficios-390`, `07b-beneficios-360-claro` |
| Recibo con evidencia / indicación / ya comprobada | `08-…-390`, `08b-…-360-claro`, `09-recibo-indicacion-390`, `10-recibo-companero-ya-comprobada-390` |
| Entrega: comprobar, incidencia, validada, compañero, escritorio | `12b-…-390`, `12c-…-360-claro`, `13`, `14`, `15`, `16-entrega-escritorio` |
| Roles | `10-permisos-rol-390`, `11-tablero-personal-390`, `27-tablero-participantes-390` |
| Participantes, confirmar, lista, compañero, entrada bloqueada, historial | `20-…-390`, `20b-…-360-claro`, `21`, `22`, `23`, `24`, `25-historial-360-claro`, `26-participantes-escritorio` |

## Rutas para probar

| Rol | Ruta |
|---|---|
| Organización | `/organizer` → Preparar evento → Actividades → «Comprobar al entregar el beneficio» |
| Participante | `/events/{id}/plan` (catálogo) · `/matches/{id}` · `/me/benefits` · `/me/claims/{id}` |
| Personal | `/organizer/redemptions` → consultar el recibo → «La vi y corresponde» → «Confirmar entrega» |
| Organización | `/organizer` → Durante el evento → Participantes (`/events/{id}/participants`) |
| Administración general | `/organizer/access` (rol organización o personal) |

## Decisiones tomadas sin detenerse (registradas en DEC-068)

- El rol de personal se define como «solo revisar y entregar» (para el personal, la preparación del evento también queda cerrada). Los permisos existentes quedan como organización, así que no cambia nada para nadie hasta que se asigne un rol de personal.
- Al retirar a alguien, sus canjes pendientes se anulan con reembolso, en lugar de dejarlos bloqueados.
- Al readmitir, la persona no queda disponible automáticamente.

## Pendiente

- Aceptación de Samuel y Julián (DEC-068).
- Prueba física en el iPhone del flujo de entrega con una foto real de la galería.
- Integración: el SHA desplegado sigue sin verificar (ver AUDITORIA_RECORRIDOS_PING.md). Antes de publicar en Aiven hay que comprobar que la versión de MySQL es 8.0.19 o posterior (`DROP CONSTRAINT`).

## Entorno de verificación (fuera del repositorio)

Scratchpad de la sesión: MySQL 8.4 nuevo en 3319 (solo loopback, bases `ping_evidence_exclusive` y `ping_app_exclusive`, usuario local de prueba), app en 8097 desde `target/classes` y arnés CDP con perfiles por persona. Las cuentas y los datos son ficticios. Se puede borrar sin afectar a otras instancias.

## Resultados

- Dirigidas: `DeliveryEvidenceTests` 6/6 y `EventParticipantsTests` 3/3.
- Primera suite completa (`./mvnw -o test`), a mitad del trabajo: 392 pruebas, 0 fallos.
- **Verificación final `./mvnw -o clean verify`** sobre el código definitivo (después solo cambió documentación): **392 pruebas, 0 fallos, 0 errores, 0 omitidas, BUILD SUCCESS**, 11/11/2026 14:33 Bogotá.
- Migración en MySQL 8.4 aislado: 14/14 comprobaciones (V25 → V26 con datos ficticios anteriores).
