# Correos transaccionales de PING (DEC-062)

Preparado el 28 de octubre de 2026 a petición de Samuel. Rama `feature/transactional-emails` desde `deploy/render-demo` (`477c72a`), integrada mediante PR por autorización de Samuel. Sin despliegue automático ni envíos reales.

## Diseño (Figma)

- Página «PING · Correos transaccionales»: [148:262](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING?node-id=148-262).
- Plantilla compartida «Correo PING · Mensaje de cuenta», con variantes de 390 y 600 px: [148:324](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING?node-id=148-324).
- Los cuatro mensajes en [móvil 390](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING?node-id=149-289) y [escritorio 600](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING?node-id=149-421).
- Referencia del antes: [149:553](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING?node-id=149-553).

La página reutiliza piezas existentes del archivo:
- **Logo:** «Marca C · Horizontal» («P en común»).
- **Botón:** «V2 · Botón» en su variante Principal.
- **Tokens:** variables `PING V2 · Color` y `PING · Espacio`, con los mismos valores que el tema oscuro de la app.

No se modificó ninguna página anterior. Las fuentes de Figma son Inter, como referencia de la fuente del sistema; el correo no carga Manrope.

## Mensajes

| Tipo | Asunto | Botón | Destino | Caducidad |
|---|---|---|---|---|
| Verificación del registro | Verifica tu correo de PING | Verificar mi correo | `/account/verify` | 24 horas, un uso |
| Activación de cuenta pendiente | Activa tu cuenta de PING | Activar mi cuenta | `/account/reset` | 30 minutos, un uso |
| Recuperación de acceso | Recupera tu acceso a PING | Elegir nueva contraseña | `/account/reset` | 30 minutos, un uso |
| Cuenta ya existente | Ya tienes una cuenta de PING | Elegir nueva contraseña | `/account/reset` | 30 minutos, un uso |

Las caducidades salen de `AccountService.lifetime(...)`, la misma fuente que fija `expires_at`. No cambian destinos, tokens, uso único, continuación de sesión ni reglas de cuenta.

## Implementación

- **Contenido:** `auth/AccountMail.java` define el contenido de cada tipo y genera dos versiones: HTML (`templates/mail/account.html`) y texto con la URL completa.
- **Plantilla HTML:**
  - Tablas, estilos esenciales en línea y ancho máximo de 600 px.
  - Texto principal de 16 px y botón de 52 px de alto.
  - Fuentes del sistema y `color-scheme: dark`.
  - Sin JavaScript, formularios, CSS externo, SVG ni imágenes base64.
  - Los datos dinámicos se escapan.
- **Logo:** `static/img/email/ping-logo.png`, de 264×96 (3× de 88×32), rasterizado desde `docs/design-proposals/v2/marca/c-horizontal-oscuro.svg`. Se sirve sin sesión en `PING_PUBLIC_BASE_URL/img/email/ping-logo.png`, con `alt="PING"` y dimensiones declaradas.
- **Transporte:**
  - `PrivateMail` envía por SMTP como `multipart/alternative` (texto + HTML). Esto sirve para servidores SMTP y para el buzón local `tools/dev-mailbox.py`.
  - Con Brevo envía un mensaje que `HttpApiMail` transmite como `htmlContent` + `textContent`.
  - No hay campañas, listas ni parámetros de seguimiento en los enlaces.

## Comprobaciones

- **Automatizadas:**
  - `AccountMailTests`: asunto, botón y destino de cada tipo; la misma acción en HTML y texto; caducidad real; escape; marcado apto para correo.
  - `HttpApiMailTests`: `htmlContent` junto a `textContent`.
  - `AccountSecurityTests`: el correo SMTP es multipart con la misma URL en ambas partes, y el logo responde 200 `image/png` sin sesión.
- **Visual:** previsualización de la plantilla real en Chromium a 390, 360 y 600 px. El botón mide 52 px, el logo carga a 88×32, no hay desbordamiento horizontal, y con la imagen bloqueada se lee «PING».
- **Contraste (WCAG):** texto 8,4–15,7:1 y botón 15,5:1.

Capturas en [`screenshots/transactional-emails/`](screenshots/transactional-emails/):
- [antes, en texto plano](screenshots/transactional-emails/00-antes-recuperacion-texto-plano-390.png);
- [recuperación](screenshots/transactional-emails/01-recuperacion-390.png), [verificación](screenshots/transactional-emails/02-verificacion-390.png), [activación](screenshots/transactional-emails/03-activacion-390.png) y [cuenta existente](screenshots/transactional-emails/04-cuenta-existente-390.png) a 390 px;
- [verificación](screenshots/transactional-emails/05-verificacion-360.png) y [recuperación](screenshots/transactional-emails/06-recuperacion-360.png) a 360 px;
- [imágenes bloqueadas](screenshots/transactional-emails/07-recuperacion-imagenes-bloqueadas-390.png);
- [600 px](screenshots/transactional-emails/08-recuperacion-600.png).

**Límites:** son capturas de Chromium. No se ha comprobado en Gmail, Apple Mail ni Outlook, ni con su modo oscuro. Gmail puede alterar colores en modo oscuro y Outlook de escritorio ignora los bordes redondeados. No se ha enviado ningún correo real.

## Probar en el iPhone

1. Integrar y desplegar la rama (con autorización). El logo debe responder en `https://ping-b63o.onrender.com/img/email/ping-logo.png`.
2. Seguimiento de clics: PING no añade ninguno. La petición a Brevo solo lleva remitente, destinatario, asunto, `htmlContent` y `textContent`. Aun así, Brevo puede reescribir los enlaces transaccionales con su dominio de seguimiento: el botón redirige a PING, pero el enlace con token pasa antes por Brevo. Su API v3 no tiene una opción por mensaje para evitarlo, y según su personal desactivarlo es bajo solicitud para clientes Enterprise ([foro de Brevo](https://community.brevo.com/t/no-way-to-disable-by-option-tracking-in-transactional-e-mail/201/28)). En el iPhone, mantén pulsado el botón para ver a qué dominio apunta. Evitarlo del todo exigiría otro transporte (SMTP propio u otro proveedor); es una decisión pendiente, no un ajuste.
3. Con un correo propio, pide «Recuperar acceso» en PING y abre el mensaje en Mail o Gmail del iPhone. Comprueba logo, título, botón amarillo, caducidad y que «Elegir nueva contraseña» abre PING.
4. Repite con un registro nuevo (botón «Verificar mi correo»). No reenvíes ni compartas los mensajes: llevan enlaces de un solo uso.

## Revertir

Revertir el commit restaura el texto plano. No hay migraciones.
