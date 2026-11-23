# Validación presencial de actividades con QR (DEC-081: pedida por Samuel, pendiente de Julián)

Encargo de Samuel del 23 de noviembre de 2026. Rama `feature/validacion-qr-actividad` desde `0027df6` (`feature/organizacion-resultados`). Conserva Organización, Resultados, Actividades abiertas e Intereses. No se atribuye aprobación a Julián. Sin merge ni despliegue.

## Recorrido

1. **Organización** elige en la actividad «¿Cómo se comprueba?» → **Validación presencial con QR**. Configuración muestra «N con validación por QR».
2. **Participante**, con la llegada y la verificación hechas, pulsa **«Completar actividad»**. Eso registra su confirmación y la página baja al **QR de la actividad**. El QR muestra el código manual `ACTIVIDAD-…`, vence a los 20 minutos y se puede renovar. Sirve el QR de cualquiera de los dos.
3. **Personal u Organización del evento** (o administración general) abre **Organización → «Escanear QR de actividad»** (`/organizer/activity-scan`). Usa el escáner existente o escribe el código.
   - Antes de la segunda confirmación: «Falta que X pulse "Completar actividad"». No se acredita nada y el mismo código sigue sirviendo («Validar de nuevo este código»).
   - Con ambas confirmaciones: un escaneo registra la validación y arranca la Saga existente. Cada persona recibe los puntos configurados una sola vez.
   - Un escaneo repetido o simultáneo responde «ya estaba validada» y no suma puntos.

## Reglas en el servidor

| Regla | Dónde |
|---|---|
| Método congelado al activar el encuentro; los demás métodos y encuentros no cambian | `Match.qrValidation()`, `describeCheck` existente |
| La Saga espera hasta que exista la validación QR del encuentro | `MatchCompletionSagaSteps.awaitingReview` |
| Credencial `ACTIVITY` (`PING:ACTIVITY:…`) ligada al encuentro (`context_id`) y al evento (`event_id`), con caducidad y revocación | `CredentialService.activity` (reutiliza `issue`) |
| Rechazo de los QR de canje y de encuentro (con mensaje propio), de los códigos vencidos o sustituidos, de otro evento (403) y de quien participa en ese encuentro | `CredentialService.validateActivity` |
| Una validación por encuentro (clave primaria) y bloqueo del evento: los escaneos simultáneos no duplican | `activity_qr_validations`, `events.lock` |
| Acceso de Personal a la página de escaneo, igual que a la entrega de canjes | `AccessPolicy` |

**Migración `V27_1__activity_qr_validation.sql` (versión 27.1, aditiva):** amplía el CHECK de `purpose_credentials.purpose` con `ACTIVITY` y crea `activity_qr_validations`. Queda entre V27 y V28, de modo que **V28 sigue reservada a la regla de DEC-079** y se aplicará en orden.

## Verificación

- **`ActivityQrTests`** (6 pruebas):
  - escaneo antes de la segunda confirmación, con el mismo código reutilizado después, crédito único de 20 + 20 y repeticiones del personal y de Organización sin efecto;
  - dos escaneos simultáneos con los QR de ambos: crédito único (un `CREDITED` y un `ALREADY`), dos `GRANT` y una validación;
  - rechazos de: código vencido, código de canje (manual y `PING:REDEMPTION:`), QR de encuentro, personal de otro evento, cuenta sin rol y participante del encuentro con rol de personal;
  - método congelado en las dos direcciones;
  - actividad QR de 0 puntos;
  - recorrido HTTP completo, con la imagen PNG del QR y la página del personal.
- **Suite completa:** ver el commit.
- **MySQL 8.4.9 aislada** (127.0.0.1:3323, carpeta temporal ya borrada), con `DoorAttendanceUpgradeMySqlCheck` ampliada:
  - partiendo de V26 (el esquema del servicio), Flyway aplica V27 y V27.1 en 315 ms;
  - las 44 tablas y sus 715 filas quedan idénticas; los saldos, la Saga y el historial V1–V26 no cambian;
  - lo nuevo es solo `door_attendance`, la tabla vacía y el CHECK ampliado; el CHECK admite `ACTIVITY` y rechaza otros valores;
  - PING arranca con `validate`.
- **Navegador, 390 px y 360 px, datos ficticios** (`ResultsShowcase`, evento «Validación QR», Lina, Mario, `personal@ping.test` y `otro@ping.test`). **Cámara simulada:** se sustituyó `getUserMedia` por un lienzo que emite el PNG real del QR generado por PING. El escáner existente (ZXing) lo decodificó.
  - Lina completa la actividad y ve su QR. El personal lo escanea: «falta Mario».
  - Mario completa la actividad. El mismo QR escaneado acredita 20 + 20.
  - Dos repeticiones (con la cámara y con el código escrito) responden «ya estaba validada»; los saldos siguen en 20 y 20.
  - Un QR de canje real se rechaza en el escáner («no corresponde a esta acción») y, escrito a mano, en el servidor.
  - Una cuenta de otro evento recibe 403 sin ver nombres. Un código vencido se rechaza.
  - En Organización, el editor guarda el método.
  - Capturas en [`../screenshots/validacion-qr/`](../screenshots/validacion-qr/).
- **Límites:** cámara simulada (no un teléfono real) y sin pruebas en Render.

## Activarlo en un evento (por ejemplo USTA20262)

Organización → elegir el evento (**Cambiar evento**) → pestaña **Configuración** → fila **Actividades y puntos** → **Editar** → en la actividad, **Editar** → **¿Cómo se comprueba?** → **Validación presencial con QR** → (opcional) «Qué comprobará el personal antes de escanear» → **Guardar actividad**. Solo afecta a los encuentros que se activen después.

## Pendiente

- La aceptación de Julián para DEC-081. DEC-079 y DEC-080 también siguen pendientes, y desplegar esta rama los publica.
- Al desplegar se aplican V27 y V27.1 sobre la base de Render. Antes hay que confirmar un respaldo recuperable de Aiven.
