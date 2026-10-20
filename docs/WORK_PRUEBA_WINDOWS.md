# Probar la entrega Work en Windows y móvil

Revisión móvil actual: **PR47, rama fix/mobile-journey-clarity**, basada en PR46 e incluida la cadena anterior. No hace falta merge para probarla. La continuidad y GitHub contienen el SHA remoto comprobado.

Base conservada en cuatro PR apiladas:43(editor/recursos),44(cuentas),45(conexión/recibos),46(integración final; comprobar enlace/SHA en la continuidad y GitHub). Todas son borradores. No se necesita merge para probar la última rama, que contiene los bloques anteriores. No borrar `.codex/`, bundles ni otros archivos locales.

## Recuperar la rama aunque el clon fuera single-branch

Desde la PowerShell del clon, revisar primero `git status --short`. Si hay cambios **versionados**, conservarlos antes de cambiar de rama; no usar reset/clean. Los archivos ajenos sin seguimiento no requieren borrado.

```powershell
cd C:\Users\river\PING
git remote -v
git remote set-branches --add origin fix/mobile-journey-clarity
git fetch origin refs/heads/fix/mobile-journey-clarity:refs/remotes/origin/fix/mobile-journey-clarity
git switch fix/mobile-journey-clarity
git pull --ff-only origin fix/mobile-journey-clarity
git rev-parse HEAD
git status --short
```

GitHub usa la conexión/autenticación Git instalada. No pegar tokens por chat. Si Git señala que sobrescribiría un archivo, conservarlo y resolver ese conflicto concreto; no borrar directorios para forzar el cambio.

## Antes de arrancar

JDK21 y MySQL8.4 deben estar disponibles. Usar la base MySQL que realmente contiene tus datos y respaldarla antes de aplicar V19–V22. No existe una contraseña MySQL o PING universal. La contraseña del usuario técnico MySQL no es una cuenta de participante.

La aplicación aplica migraciones nuevas automáticamente con `ddl-auto=validate`; no editar migraciones publicadas ni usar `ddl-auto=create`. Probar en una copia exclusiva si quieres conservar intactos los datos de uso.

Puedes introducir las variables locales sin sustituir texto dentro de comandos ni mostrar la contraseña:

```powershell
if (-not $env:DB_URL) { $env:DB_URL = Read-Host 'Pega tu URL JDBC MySQL existente, sin contraseña' }
if (-not $env:DB_USERNAME) { $env:DB_USERNAME = Read-Host 'Usuario técnico de MySQL' }
$pingDbCredential = Get-Credential -UserName $env:DB_USERNAME -Message 'Contraseña de MySQL: queda solo en esta PowerShell'
$env:DB_PASSWORD = $pingDbCredential.GetNetworkCredential().Password
Remove-Variable pingDbCredential
```

Conservar TLS de la conexión MySQL que corresponda a tu instalación. No se conoce desde este entorno la URL ni la contraseña de tu ordenador, y no se inventan valores.

Para la **primera cuenta general**, una vez, antes de arrancar:

```powershell
$env:PING_BOOTSTRAP_EMAIL = Read-Host 'Correo nuevo para la cuenta general de PING'
$pingGeneralCredential = Get-Credential -UserName $env:PING_BOOTSTRAP_EMAIL -Message 'Nueva contraseña PING: entre12 y128 caracteres'
$env:PING_BOOTSTRAP_PASSWORD = $pingGeneralCredential.GetNetworkCredential().Password
Remove-Variable pingGeneralCredential
```

Si auth_setup_state ya fue consumido, entrar con la cuenta general existente o usar recuperación de acceso; no reconfigurar otra contraseña esperando reemplazarla. Después del primer inicio correcto, retirar ambas variables bootstrap y reiniciar. Más detalle, SMTP/Google/Facebook y sus redirects en [ACCOUNTS_SETUP.md](ACCOUNTS_SETUP.md).

## Correo de ensayo controlado

En otra PowerShell, desde el clon, con Python ya instalado:

```powershell
py tools/dev-mailbox.py
```

El buzón escucha solo127.0.0.1:1025. Guarda mensajes `.eml` privados en `PING-dev-mailbox` del perfil del usuario, fuera del clon. Permite probar verificación/recuperación, pero **no envía correos a Internet**. Abrir los mensajes localmente; no adjuntarlos a Git ni al chat.

En la PowerShell de PING:

```powershell
$env:SPRING_MAIL_HOST = '127.0.0.1'
$env:SPRING_MAIL_PORT = '1025'
$env:PING_MAIL_FROM = 'pruebas@ping.test'
$env:PING_DEMO_ENABLED = 'false'
$env:PING_OAUTH_ENABLED = 'false'
$pingWifi = Get-NetIPConfiguration | Where-Object { $_.IPv4DefaultGateway -and $_.NetAdapter.Status -eq 'Up' -and $_.NetAdapter.InterfaceDescription -notmatch 'Hyper-V|WSL|Virtual' } | Select-Object -First 1
if (-not $pingWifi) { throw 'No encontramos una conexión con puerta de enlace. Conecta el PC a tu Wi-Fi y repite.' }
$pingLanIp = $pingWifi.IPv4Address.IPAddress | Select-Object -First 1
$env:PING_PUBLIC_BASE_URL = "http://${pingLanIp}:8080"
$env:SERVER_ADDRESS = '0.0.0.0'
Write-Host "Abre en el PC y los celulares de la misma red: $env:PING_PUBLIC_BASE_URL"
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--spring.mail.properties.mail.smtp.starttls.enable=false --spring.mail.properties.mail.smtp.starttls.required=false'
```

Estos dos overrides SMTP sin TLS son solo para el buzón loopback anterior; con SMTP remoto conservar STARTTLS y usar su configuración autorizada. Si hay varias conexiones, verificar que la IP mostrada es la de la red compartida. La IP antigua172.20.10.4 no se presupone vigente. Mantener la consola abierta. Si Windows solicita acceso, permitirlo únicamente en la red privada de ensayo; no abrir el puerto de MySQL ni exponer el buzón. El aislamiento de clientes de algunos puntos Wi-Fi puede impedir el acceso entre dispositivos.

**HTTP por IP LAN permite navegación y códigos manuales, no cámara web.** Para usar “Escanear QR” desde el teléfono se necesita un origen HTTPS con certificado confiado por ese teléfono y `PING_PUBLIC_BASE_URL` de ese origen. Cambiar solo una variable a https sin configurar TLS no crea HTTPS. No desactivar las protecciones del navegador. El lector externo de la cámara del sistema puede abrir un QR de entrada, pero eso no prueba el escáner interno del navegador. Google/Facebook requieren clientes reales y redirects del propietario; no se simula su configuración.

## Recorrido de aceptación

1. General: entrar por “Organizo un evento”, crear un **evento ficticio** propio y abrirlo. En “Editar configuración” añadir punto, palabras, actividad con37 puntos y lugar de entrega; crear recompensa de20 puntos. Probar guardar una sección con otra en borrador: esta se conserva. El catálogo anterior puede revisarse/reducirse/importarse explícitamente.
2. Opcional: cargar un plano esquemático ficticio, colocar el punto por toque o coordenadas y guardar. No subir planos privados a Git. Sin plano, nombre/indicaciones siguen funcionando.
3. En dos navegadores/perfiles o celulares: código del evento → registrar cuenta → verificar el correo recibido → alias/avatar → intereses. Elegir Lady Gaga no selecciona Pop ni sus otros artistas. Entrar a Mi Ping y comprobar avatar.
4. Activar disponibilidad, buscar, aceptar ambos. Antes de ambas llegadas no hay confirmación final. Cada persona marca su llegada, muestra su **propio** código/QR y verifica el que presenta la otra. La palabra compartida sirve para reconocerse, no como prueba.
5. Confirmar la actividad por ambos:37 puntos por persona una vez. Solicitar recompensa20: recibo pendiente, lugar congelado y saldo17. Renovar código no vuelve a descontar. Solo otro encargado autorizado del evento valida y confirma entrega.
6. Buscar otra vez: la pareja completada queda excluida en ambos órdenes. Reiniciar PING y volver a entrar: misma participación, saldo17 y recibo con su estado; las sesiones/códigos en claro no se prometen persistentes y se pueden renovar sin otro cobro.

## Qué respalda GitHub y qué debes respaldar aparte

GitHub conserva código, migraciones, catálogo, SVG y evidencia ficticia. **No conserva datos de uso.** MySQL conserva cuentas, selecciones, puntos, Saga, recibos, planos/bytes/versiones y coordenadas; copiar solo la rama no recupera estos datos. El respaldo de MySQL debe incluir blobs y todas las tablas, preferentemente con `mysqldump --single-transaction --hex-blob --no-tablespaces` y archivo de salida fuera del repositorio. Conservar acceso seguro al respaldo y comprobar restauración en una base exclusiva. No borrar versiones antiguas del plano que requieren encuentros activos.

No se incluyen contraseñas, cuentas sociales, correo privado, cookies, dumps ni planos privados en Git. Cerrar PING y el buzón con Ctrl+C al terminar; limpiar variables de contraseña cerrando esas PowerShell. No hacer merge ni modificar main para este ensayo.

## Qué revisar de la revisión móvil PR47

Con PING arrancado y la dirección LAN mostrada por los comandos anteriores, usa dos celulares o navegadores con sesiones separadas. En /organizer, el admin encuentra Código y QR, Editar configuración, Recompensas, Plano y Validar y entregar un canje. Comparte el código del evento que TÚ creaste: los datos ficticios del executor no se descargan con Git.

Introduce ese código en el celular. Debe mantenerse el nombre al crear cuenta, verificar el correo y entrar. Si verificas en otro navegador, vuelve a la pestaña original y pulsa Ya verifiqué mi correo · Entrar. Una cuenta con participación en otro evento debe continuar en el evento elegido. Alias y avatar son públicos en el evento; el correo no.

Elige gustos, busca otro y vuelve al alias: la selección debe seguir. Completa el recorrido de aceptación anterior; ACTIVE orienta qué falta de ti y de la otra persona. Generar mi código abre el panel personal: cada uno muestra el suyo e introduce el del otro. No usar la palabra compartida ni un código de canje para verificar el encuentro. Tras ambas confirmaciones, el recibo reúne coste, saldo actual, estado y entrega. Renovar no descuenta otra vez. El encargado debe ser otra cuenta autorizada para ese evento.
