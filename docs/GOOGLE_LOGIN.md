# Acceso con Google

Preparado el 28 de octubre de 2026 a petición de Samuel. Rama `feature/google-login`, PR en borrador hacia `deploy/render-demo`. Sin merge ni despliegue. Amplía [ACCOUNTS_SETUP.md](ACCOUNTS_SETUP.md) (DEC-049); la propuesta técnica está en DEC-060 del [registro de decisiones](DECISION_LOG.md).

## Qué hace

La pantalla «Entra a tu cuenta» y la de «Crea tu cuenta» muestran «Continuar con Google» cuando está configurado.

- **Identidad ya vinculada:** entra en la misma cuenta PING, identificada por `google` y el `sub` estable de Google. No se usa el correo ni el nombre.
- **Persona nueva:** se crea una cuenta PING básica, sin administración, con el correo que Google marca como verificado (`email_verified=true`), sin pedir otra verificación de PING. La sesión queda iniciada y la persona continúa al evento elegido para completar alias, avatar e intereses. El nombre y la foto de Google no se guardan ni sobrescriben nada.
- **Mismo correo que una cuenta PING sin vincular (DEC-061):** no se fusionan. La identidad de Google queda guardada en el servidor, ligada a la sesión, durante 10 minutos. La página «Confirma que es tu cuenta» (`/login/link`) pide la contraseña de PING y ofrece «Vincular Google y continuar» o «Entrar sin vincular». Solo un POST con CSRF, la contraseña correcta de esa cuenta activa y la intención vigente completan la vinculación; un GET, un parámetro o un acceso con otra cuenta no lo hacen. Si la persona recupera el acceso desde ahí en el mismo navegador, vuelve a esa página y vincula sin repetir Google. Después continúa al evento. Mi cuenta sigue permitiendo vincular más tarde.
- **Evento por código o QR:** el evento elegido se conserva en la sesión durante la ida y la vuelta a Google. Nunca se recupera una participación de otro evento.
- **Cancelación, caducidad, error del proveedor o reintento del callback:** se vuelve a la pantalla de acceso con un mensaje propio y sin crear nada. Si se estaba vinculando desde Mi cuenta, la sesión local se conserva.
- **Sin configuración, o con configuración incompleta:** el botón no aparece y el acceso por correo sigue igual. PING arranca de todos modos y deja un aviso en el registro.

Seguridad, todo con Spring Security 7.1.1, sin bibliotecas nuevas:

- Flujo de código de autorización OIDC con `state` y `nonce`.
- Firma del ID token comprobada con las claves públicas de Google, junto con emisor, audiencia y caducidad.
- Cambio del ID de sesión al entrar.
- Retorno limitado a rutas internas.
- Alcances: `openid email profile`, nada más.
- No se guardan los tokens de acceso de Google.
- El secreto solo viaja del servidor a Google.

## Recorrido continuo (DEC-061)

Una única política (`AccessContinuation`) decide el destino después de cualquier acceso: contraseña, Google, confirmación de cuenta o enlace del correo.

- **Evento elegido por código o QR:** si ya hay participación en ese evento, se entra en ella; si no, se va al formulario del evento para completar solo lo que falta.
- **Sin evento elegido:** se entra en la participación más reciente; si no hay ninguna, se va a introducir el código del evento.
- **Nunca** se recupera la participación de otro evento. Los destinos son rutas internas permitidas.

Registro con correo:
- Tras crear la cuenta se muestra «Revisa tu correo», con «Reenviar enlace» y «Cambiar correo». El aviso dice «enlace», porque es lo que se envía.
- El enlace pide la contraseña del registro e **inicia la sesión en el navegador donde se abre**. Si se abre en otro dispositivo, el navegador que solo registró no queda autenticado.
- Registrar un correo que ya existe da la misma respuesta pública, no crea otra cuenta ni cambia la existente. El dueño recibe un enlace para entrar o elegir contraseña.
- Si el acceso falla, el correo escrito se conserva en la sesión (nunca la contraseña) y se ofrece «Créala con este correo». No se crea nada por un intento fallido.

Capturas reales de la revisión local (datos de ensayo, `docs/screenshots/continuous-access/`):
- Login fallido que ofrece crear la cuenta: [390 oscuro](screenshots/continuous-access/01-login-fallido-390-oscuro.png).
- «Revisa tu correo»: [390 oscuro](screenshots/continuous-access/02-revisa-correo-390-oscuro.png) y [360 claro](screenshots/continuous-access/03-revisa-correo-360-claro.png).
- Enlace del correo: [verificar](screenshots/continuous-access/04-verificar-correo-390-oscuro.png) y [continuación al evento](screenshots/continuous-access/05-continua-al-evento-390-oscuro.png).
- «Confirma que es tu cuenta»: [390 oscuro](screenshots/continuous-access/06-confirmar-cuenta-390-oscuro.png), [con error](screenshots/continuous-access/07-confirmar-cuenta-error-390-oscuro.png), [360 claro](screenshots/continuous-access/08-confirmar-cuenta-360-claro.png) y [escritorio](screenshots/continuous-access/09-confirmar-cuenta-1440-oscuro.png).

**Correo real:** registrar, reenviar y recuperar requieren el transporte configurado (Brevo en Render). Sin él, el registro con correo se muestra como no disponible y Google sigue funcionando. La entrega real no está probada.

## Cuentas sin verificar

Una cuenta registrada con correo y sin verificar no se activa con la contraseña de quien la registró, salvo que esa persona también controle el buzón. Si alguien vuelve a registrar el correo, pide «Recuperar acceso» o entra con un proveedor sin correo verificado, recibe un enlace de activación: elige una contraseña nueva, la anterior deja de funcionar, las sesiones previas caducan y los demás enlaces quedan anulados. El enlace de verificación del registro pide la contraseña elegida al registrarse.

## 1. Google Cloud (gratis, sin facturación)

1. En <https://console.cloud.google.com/>, crea un proyecto nuevo, por ejemplo `PING`. No actives facturación ni ninguna API de pago: el inicio de sesión no las necesita.
2. Ve a **Google Auth Platform → Get started** y completa:
   - **App name:** `PING`.
   - **User support email:** tu correo.
   - **Audience:** **External**.
   - **Contact information:** tu correo.
   - Acepta la política y pulsa **Create**.
3. En **Data Access → Add or remove scopes**, marca solo `openid`, `.../auth/userinfo.email` y `.../auth/userinfo.profile`. No añadas Gmail, Drive, contactos ni otros alcances.
4. En **Audience**, deja **Publishing status: Testing**. En **Test users → Add users**, añade las cuentas de Google que van a probar.
5. En **Clients → Create client**:
   - **Application type:** **Web application**. **Name:** `PING Render`.
   - **Authorized JavaScript origins:** déjalo **vacío**. El flujo es del servidor y no lo necesita.
   - **Authorized redirect URIs:** pega exactamente `https://ping-b63o.onrender.com/login/oauth2/code/google`
   - Pulsa **Create**. Copia el **Client ID** y el **Client secret**. Google puede mostrar el secreto solo en ese momento; si lo pierdes, crea otro secreto en el mismo cliente.
   - Si la consola rechaza la URI por el dominio, añade `ping-b63o.onrender.com` en **Branding → Authorized domains** y repite.
6. No subas logotipo. Hacerlo obliga a pasar la verificación de marca antes de mostrarlo.

## 2. Render

En **Environment → Environment Variables** del servicio:

| Variable | Valor | Tipo |
|---|---|---|
| `PING_OAUTH_ENABLED` | `true` | normal |
| `PING_GOOGLE_CLIENT_ID` | el Client ID (termina en `.apps.googleusercontent.com`) | normal |
| `PING_GOOGLE_CLIENT_SECRET` | el Client secret | **secreta**: pégala solo en este panel |
| `PING_PUBLIC_BASE_URL` | `https://ping-b63o.onrender.com` (ya existe; compruébala sin espacios) | normal |

La URI de retorno se construye con `PING_PUBLIC_BASE_URL` + `/login/oauth2/code/google`. Si esa variable cambia, cambia también la URI en Google.

Render solo despliega lo que está en GitHub y en la rama configurada en el servicio (lo publicado coincide con `deploy/render-demo`). Para probar, hay que integrar el PR en esa rama o apuntar el servicio a `feature/google-login`, con autorización expresa; publicar la rama sola no despliega.

## 3. Probar con usuarios de ensayo

1. En el iPhone (Safari), abre `https://ping-b63o.onrender.com/join?code=CODIGO` con el código de un evento abierto.
2. Pulsa **Continuar con Google**, elige una cuenta de prueba y acepta.
3. Deberías volver a PING con el evento arriba. Elige alias, avatar e intereses y llegarás a Mi Ping.
4. Cierra sesión y vuelve a entrar con Google: debe abrir la misma cuenta, con el mismo alias.
5. Para comprobar la cancelación, en la pantalla de Google pulsa **Cancelar**. Debe aparecer «Cancelaste el acceso con Google».
6. Para probar la coincidencia de correo, usa una cuenta de prueba cuyo correo ya tenga cuenta PING con contraseña. Debe pedir entrar con la contraseña y ofrecer «Vincular Google».

Una cuenta de Google que no esté en **Test users** recibe el bloqueo de Google (`403 access_denied`). Es lo esperado mientras la app está en Testing.

## 4. Acceso público

En **Audience**, pulsa **Publish app** (In production). Con solo `openid`, `email` y `profile` no hace falta verificar alcances sensibles. Sin verificación de marca, Google muestra el dominio (`ping-b63o.onrender.com`) en lugar del nombre «PING». Mostrar el nombre o un logo exige verificar la marca: un dominio verificado en Search Console y una política de privacidad publicada. Es opcional y no tiene coste.

## 5. Desactivar o revertir

- **Desactivar Google sin tocar el código:** en Render pon `PING_OAUTH_ENABLED=false` o borra el secreto, y guarda (Render redespliega). El botón desaparece y el correo sigue funcionando. Las cuentas creadas con Google se conservan. Esas personas pueden entrar con «Recuperar acceso», que les envía un enlace para fijar contraseña si el correo está configurado.
- **Retirar el acceso en Google:** en **Clients**, borra el secreto o el cliente.
- **Revertir el código:** son cambios sin migración de datos (usan las tablas de V20). Basta revertir el commit que los contenga. Los vínculos ya creados en `external_identities` quedan inertes.

## Archivos

- **Código:** `auth/AuthConfiguration.java`, `auth/LoginSuccess.java`, `auth/AccountService.java`, `auth/AccountController.java` y `auth/PrivateMail.java`.
- **Plantillas:** `account/context.html`, `account/login.html`, `account/register.html`, `account/profile.html` y `account/token.html`.
- **Estáticos:** `css/v2.css` y `js/account.js`.
- **Pruebas:** `OAuthLoginFlowTests`, `OAuthCancellationTests`, `OAuthIncompleteConfigTests` y dos pruebas nuevas en `AccountSecurityTests` (intruso de un correo sin verificar y registro legítimo).
- **Documentación:** esta guía, `render.yaml`, `ACCOUNTS_SETUP.md`, `DEPLOY_RENDER_AIVEN.md`, `DECISION_LOG.md` y la continuidad.
