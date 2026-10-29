# Demostración pública a US$0 · Render Free + Aiven MySQL Free

Preparado el 25 de octubre de 2026 a petición de Samuel. **No hay commit, push ni despliegue.** Render solo despliega lo que está en GitHub. Mientras esta versión siga en la copia local, Render no puede construirla (ver «Qué falta para publicar»).

## Viabilidad comprobada

| Punto | Resultado | Cómo se comprobó |
|---|---|---|
| Dockerfile | Dockerfile Path `./Dockerfile`, Docker Build Context Directory `.` (la raíz del repositorio, un solo punto); JDK 21 para compilar y JRE 21 para ejecutar, usuario sin privilegios | Construido con Docker 29.8.1. Se corrigieron dos fallos: faltaba `unzip` (el wrapper descargaba el `.tar.gz` y su SHA-256 no coincidía) y los CRLF de las copias de Windows |
| Puerto dinámico | `deploy/render/entrypoint.sh` usa `SERVER_PORT=${PORT:-10000}`; perfil `render` con `server.address=0.0.0.0` | Contenedor escuchando en 10000 |
| HTTPS tras el proxy | `server.forward-headers-strategy=native` (solo confía `X-Forwarded-*` de redes privadas) y cookie `Secure` forzada | Con cabeceras de proxy: redirecciones a `https://<host>/login`, cookies `Secure`, HSTS, `X-Frame-Options: DENY`, sin `http://` en las páginas |
| 512 MB | Heap = 55 % de RAM, SerialGC, metaspace ≤160 MB, 24 hilos, 4 conexiones | `--memory=512m --cpus=0.1`: arranque 186 s (23 migraciones), 315 MiB al iniciar, **pico 360 MiB** en un recorrido completo de dos sesiones. Sin OOM ni funciones eliminadas |
| MySQL Aiven | MySQL 8.4 LTS; las 23 migraciones crean tablas con clave primaria (Aiven las exige por defecto) | Migraciones aplicadas en `mysql:8.4` |
| TLS a la base | El arranque **exige** la CA y conecta con `VERIFY_IDENTITY` (por defecto) o `VERIFY_CA`. Nunca `REQUIRED`/`DISABLED` | CA correcta: conecta. CA ajena: falla (PKIX). `VERIFY_IDENTITY` contra un certificado sin el nombre del host: falla (HostnameChecker). La app no arranca sin validar |
| Persistencia | Cuentas, planos (`MEDIUMBLOB`), códigos y recibos están en MySQL; el disco de Render no guarda nada | Revisión de código y migraciones |
| Exposición | Sin sesión: administración, entregas, recibos, QR personales, demo, `/actuator`, `/h2-console` → login HTTPS; `/me/state` → 401; errores sin trazas | Peticiones directas a la imagen |
| Correo | Render Free bloquea SMTP (25/465/587). Nuevo adaptador HTTPS para Brevo (`HttpApiMail`), activo solo si se configura | 4 pruebas contra un servidor local simulado. **Envío real NO probado** |

Límites del plan gratuito que afectan a la demo:
- **Render:** se suspende tras 15 minutos sin tráfico; el primer acceso muestra una pantalla de carga. A 0,1 CPU la app tarda ~3 minutos en arrancar la primera vez; los arranques en frío posteriores no se han medido. La sesión se pierde al suspenderse; las cuentas y los datos se conservan en MySQL.
- **Aiven:** puede apagar la base por inactividad, avisando antes. Se vuelve a encender desde su consola.

Fuentes:
- [Render Free](https://render.com/docs/free) y [web services](https://render.com/docs/web-services).
- [Aiven Free MySQL](https://aiven.io/docs/products/mysql/concepts/mysql-free-tier), [plan gratuito](https://aiven.io/docs/platform/concepts/free-plan) y [claves primarias](https://aiven.io/docs/products/mysql/howto/create-tables-without-primary-keys).

## US$0: lo que no hay que hacer

- **Render:** no añadas tarjeta ni cambies el plan `Free`. Sin método de pago, Render suspende en lugar de cobrar si se agotan el ancho de banda o los minutos de compilación.
- **Aiven:** no añadas método de pago. Hacerlo convierte el servicio gratuito en uno de pago.
- **Brevo:** el plan gratuito (300 correos al día) no necesita tarjeta.
- **Dominio:** no hace falta comprar uno; se usa `https://<nombre>.onrender.com`.

## 1. Aiven (base de datos)

1. Console → Create service → **MySQL** → plan **Free**. Elige la región disponible más cercana a la región del servicio en Render, para tener menos latencia.
2. En **Overview → Connection information** apunta Host, Port, User (`avnadmin`) y Database (`defaultdb`). La contraseña se copia directamente al panel de Render, sin pasarla por ningún otro sitio.
3. En **Overview → CA certificate** descarga `ca.pem`. Es el certificado público del proyecto; no es secreto.
4. No hay que crear tablas: Flyway las crea en el primer arranque. La base está vacía, sin datos personales.

## 2. Render (servicio ya creado con Docker y Free)

- **Settings → Build & Deploy:**
  - Repository: `Samge34c/PING`.
  - **Branch:** la rama publicada (ver abajo).
  - **Dockerfile Path:** `./Dockerfile`.
  - **Docker Build Context Directory:** `.` (un solo punto: la raíz del repositorio, donde está el Dockerfile).
  - **Secret Files:** Render los copia en la raíz del contexto de compilación; `.dockerignore` los excluye (`ca.pem`, `*.pem`, `*.p12`, `*.key`) y el Dockerfile solo copia rutas concretas. Comprobado: un `ca.pem` en la raíz no aparece en la imagen.
  - **Health Check Path:** `/login`.
  - **Auto-Deploy:** desactivado hasta validar.
- **Environment → Secret Files → Add:** nombre `ca.pem`, contenido el `ca.pem` de Aiven. Render lo monta en `/etc/secrets/ca.pem`.
- **Environment → Environment Variables:**

| Variable | Valor | Tipo |
|---|---|---|
| `DB_URL` | `jdbc:mysql://HOST:PUERTO/defaultdb`, sin `sslMode` ni otros parámetros | normal |
| `DB_USERNAME` | `avnadmin` | normal |
| `DB_PASSWORD` | contraseña de Aiven | **secreta**; pégala solo en este panel |
| `DB_CA_FILE` | `/etc/secrets/ca.pem` | normal |
| `PING_PUBLIC_BASE_URL` | `https://NOMBRE.onrender.com`, sin barra final | normal |
| `PING_BOOTSTRAP_EMAIL` | correo **nuevo** de la cuenta general | **solo en el primer despliegue** |
| `PING_BOOTSTRAP_PASSWORD` | 12–128 caracteres, generada en tu gestor de contraseñas | **secreta; solo en el primer despliegue** |
| `PING_MAIL_API_PROVIDER` | `brevo` | normal (cuando haya correo) |
| `PING_MAIL_API_KEY` | clave API de Brevo | **secreta** |
| `PING_MAIL_FROM` | remitente verificado en Brevo | normal |
| `PING_DEMO_ENABLED` | `true` solo si quieres el escenario SOFA de demostración (lo usa únicamente una cuenta general) | normal |
| `PING_OAUTH_ENABLED` | `true` para mostrar «Continuar con Google» (ver [GOOGLE_LOGIN.md](GOOGLE_LOGIN.md)) | normal (opcional) |
| `PING_GOOGLE_CLIENT_ID` | Client ID del cliente web de Google | normal (opcional) |
| `PING_GOOGLE_CLIENT_SECRET` | Client secret de Google | **secreta** (opcional) |

- **Valores opcionales:**
  - `DB_SSL_MODE`: no la definas. Se usa `VERIFY_IDENTITY` (CA del proyecto + nombre del servidor). Si el registro muestra un error de nombre (`HostnameChecker` o `No subject alternative`), **no rebajes la verificación**: sigue antes el diagnóstico de «Si falla la identidad TLS».
  - `SPRING_PROFILES_ACTIVE`: ya viene fijado en la imagen (`mysql,render`).
  - No definas `PORT`: lo pone Render.
- **Alternativa a la Secret File:** la variable `DB_CA_PEM` con el texto PEM completo.
- **Blueprint:** `render.yaml` describe el mismo servicio con los secretos como `sync: false`, de modo que Render los pide en el panel y no se guardan en Git.

### Si falla la identidad TLS (antes de tocar `DB_SSL_MODE`)

1. **Host:** el de `DB_URL` debe ser exactamente el **Host** de Aiven (Connection information), no una IP, un alias ni el de otro servicio.
2. **CA:** `ca.pem` debe ser el **CA certificate** del mismo proyecto de Aiven. Si se regeneró o se descargó de otro proyecto, vuelve a descargarlo y reemplaza la Secret File.
3. **Certificado del servidor:** desde un equipo con OpenSSL 3, `openssl s_client -starttls mysql -connect HOST:PUERTO -servername HOST -CAfile ca.pem -verify_hostname HOST` debe terminar en `Verify return code: 0 (ok)`. Si el código no es 0, lee el SAN del certificado que muestra la salida.
4. Solo si la CA valida pero el certificado de Aiven no incluye ese host, decide expresamente si aceptas `DB_SSL_MODE=VERIFY_CA` (sigue validando la CA). Registra el motivo y avisa a Aiven.

## 3. Primer despliegue y retirada del arranque

1. **Manual Deploy → Deploy latest commit.** En el registro deben aparecer `Successfully applied 23 migrations` y `Started PingApplication`. A 0,1 CPU tarda unos 3 minutos.
2. Abre `https://NOMBRE.onrender.com/login?next=/organizer` y entra con la cuenta general.
3. Quita `PING_BOOTSTRAP_EMAIL` y `PING_BOOTSTRAP_PASSWORD` de Render y vuelve a desplegar. El arranque inicial ya está consumido en esa base y no vuelve a crear ni a promover cuentas.
4. Para preparar los datos tienes dos caminos:
   - Crea el evento desde Organización.
   - O, con `PING_DEMO_ENABLED=true`, abre `/demo/prepare`, que crea el escenario SOFA con participantes ficticios.

   Los encargados de entregas se asignan en `/organizer/access`.

## 4. Correo real (Brevo) · estado: preparado, sin probar

1. Crea una cuenta gratuita en Brevo. En **Senders** añade y verifica un remitente (Brevo envía un correo de confirmación).
2. En **SMTP & API → API keys** crea una clave y pégala solo en `PING_MAIL_API_KEY` de Render.
3. Después del despliegue, registra una cuenta de prueba en `/register` y comprueba si llega el correo de verificación.

**Riesgo sin confirmar:** si el remitente es una dirección gratuita como gmail.com, sin dominio propio autenticado, Brevo puede reescribir el remitente o los proveedores pueden filtrarlo. No se promete la entrega hasta probarla. Si falla, el registro y la recuperación muestran «El correo todavía no está configurado» o «No pudimos enviar el correo». La demo sigue disponible con la cuenta general y la demo SOFA.

## Qué falta para publicar

1. **Commit y push** autorizados por Samuel. La versión completa (HEAD `49e9482` + 63 commits locales + 91 archivos sin commit) **no está en ninguna rama remota**. La rama remota más avanzada es `origin/fix/participant-confirmation-feedback` @ `8df0b7d` (17/10), anterior a DEC-057/058, HTTPS local y este despliegue, y **no tiene Dockerfile**. Propuesta: crear una rama nueva (por ejemplo `deploy/render-demo`) con todo lo actual, subirla y seleccionar en Render esa rama y su commit.
2. **Aceptación del registro DEC-059** (infraestructura de la demostración), según AGENTS.md.
3. **Paneles:** Aiven creado y CA cargada en Render, variables de Render, primer despliegue y retirada de `PING_BOOTSTRAP_*`.
4. **Correo:** cuenta de Brevo, remitente verificado y prueba real de entrega.
5. **Tras desplegar:** comprobar en Render que las redirecciones van a `https://NOMBRE.onrender.com` y que el inicio de sesión mantiene la sesión. Si el proxy de Render no llegara desde una red privada, Tomcat no confiaría en `X-Forwarded-*`. Las cookies seguirían siendo `Secure`, pero los límites por IP compartirían la del proxy, y habría que ajustar `server.tomcat.remoteip.internal-proxies`.
