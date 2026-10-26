# PING en Windows: abrir, probar y respaldar

## 1. Doble clic para empezar

Abre **`INICIAR-PING.cmd`**, en la raíz del proyecto. Es el archivo para Samuel: no hace falta abrir PowerShell, escribir comandos ni recordar la dirección. Funciona también desde un acceso directo o al lanzarlo desde otra carpeta. Abre el panel local con **Abrir PING**, dirección y QR del iPhone, estado de aplicación/MySQL/correo, recorrido breve, mensajes locales, configuración y registros.

No cambia de rama ni descarga otra copia. Reutiliza JDK 21, Maven Wrapper, Python 3.10+ y cliente MySQL ya instalados. `Start-Ping.ps1` delega en el mismo flujo; no hay dos sistemas de arranque. El doble clic usa CMD/Python, por lo que no ejecuta scripts PowerShell bloqueados ni cambia ExecutionPolicy.

### La primera vez

El asistente, en español, solo pide los datos que no encuentra en su configuración privada o en las variables del proceso:

- MySQL existente: detecta el servidor local cuando responde al protocolo; permite elegir otro. Solicita el nombre de la base y el usuario/contraseña con entrada oculta. **No crea ni cambia silenciosamente la base**. El arranque normal de PING aplica sus migraciones pendientes; respalda datos de uso antes de actualizar.
- Puerto: usa 8081 si está disponible y no hay otro configurado; se puede cambiar desde el panel. No mata al ocupante de un puerto.
- Red: reutiliza la interfaz elegida. Si solo existe una IPv4 activa, la identifica; si hay varias, exige elegir la compartida con el teléfono. No elige silenciosamente una entre varias.
- Correo: reutiliza SMTP configurado y comprueba conexión/TLS/autenticación sin enviar correo externo. Si falta, ofrece **buzón LOCAL de ensayo** o configuración real. El buzón local no entrega mensajes a Gmail/Outlook ni a otros servidores.

La configuración completa queda cifrada mediante **DPAPI del usuario actual**, con comprobación de cifrar/descifrar, en `%LOCALAPPDATA%\PING\launcher\<identificador-del-clon>\config.dpapi`. La carpeta nueva limita su ACL al usuario. No hay contraseñas en el repositorio, argumentos de Java ni registros de estas herramientas. Las variables de entorno de los procesos hijos contienen lo necesario mientras corren; usa un PC de confianza.

**No copies `config.dpapi` a otro usuario o PC:** DPAPI lo vincula a la cuenta/equipo. Desde **Configurar / retirar credenciales** puedes cambiar MySQL, red, puerto, correo o HTTPS, o retirar el archivo cifrado. Retirar credenciales no borra cuentas, datos ni mensajes. Si todavía no tienes panel, el modo de mantenimiento es `INICIAR-PING.cmd --configurar`; no es necesario en el uso diario.

No se crea/promueve una cuenta PING ni se adopta una identidad. El usuario MySQL es distinto de tu cuenta PING. Si la base es nueva y no tiene administración general, sigue el procedimiento único de [cuentas y primer general](ACCOUNTS_SETUP.md); el lanzador no inventa esa identidad.

### Después

Vuelve a abrir el mismo archivo. Comprueba SQL con `SELECT 1`, SMTP con conversación real y la pantalla pública de acceso con HTTP 200 e identidad PING. **No anuncia listo solo porque exista un proceso.** Si la instancia registrada responde y corresponde a la versión/configuración actuales, abre su panel sin iniciar otra.

La primera compilación prepara el proyecto actual con Maven `-Dmaven.test.skip=true clean package`, **sin ejecutar la suite Java**. Después reutiliza el JAR únicamente cuando coinciden SHA-256 de fuentes/recursos/POM/wrapper y del JAR. Cambiar o quitar fuentes/recursos exige reconstruir. Si cambia la IP del Wi-Fi, actualiza enlace/QR y `PING_PUBLIC_BASE_URL`; reinicia exclusivamente su PING registrado cuando la configuración cambia. No sirve silenciosamente un JAR anterior.

Si MySQL local está detenido y se identifica un único servicio, ofrece iniciarlo: Windows puede pedir permiso **solo para esa acción**. PING se ejecuta sin elevación. Nunca detiene MySQL compartido. No activa demo ni prepara participantes automáticamente.

### Detener y resolver errores

Abre **`DETENER-PING.cmd`** o usa el botón del panel. Solo alcanza procesos que coincidan con PID, instante de creación de Windows y ejecutable registrados. No mata todos los Java/Python, no confunde un PID reutilizado y conserva MySQL y los datos. Un buzón existente no iniciado por estas herramientas se conserva.

Si algo falla, la ventana muestra el motivo y una acción concreta, permite abrir registros o corregir configuración con C y permanece esperando. Registros privados junto a la configuración: `arranque.log`, `compilacion.log`, `aplicacion.log` y `buzon.log`. No publiques la carpeta privada: además de logs puede contener mensajes y enlaces personales. Una contraseña MySQL incorrecta ofrece corregirla sin reintroducir todos los datos. Puerto ocupado: cambiar puerto o cerrar explícitamente la otra aplicación; el lanzador no la mata.

## 2. Entrar desde el iPhone

1. PC e iPhone conectados al **mismo Wi-Fi efectivo**.
2. Abre `INICIAR-PING.cmd` en el PC y escanea su QR con la cámara del iPhone. El QR contiene únicamente el origen LAN real mostrado, nunca localhost, sesiones, credenciales ni enlaces de verificación.
3. En PING entra con tu cuenta o usa el código de un evento existente. En el panel, **Prueba breve** explica cómo obtenerlo desde Organización → evento → Entrada de participantes / Código y QR. No hay código universal ni evento inventado.

**Buzón local:** abre **Abrir mensajes de ensayo** en el panel del PC para consultar las verificaciones/recuperaciones solicitadas por ti. La lectura es solo loopback; no se publica el buzón en el Wi-Fi. Los enlaces usan el origen móvil configurado, pero siguen siendo privados. No los pongas en capturas. El helper anuncia por SMTP su carpeta real para que un buzón ya abierto se reutilice sin confundir un puerto ocupado con un fallo de inicio. Si es una versión antigua sin ese anuncio, la configuración pide su carpeta una sola vez.

### Cámara del iPhone: HTTPS local (desde el 25 de octubre de 2026)

La cámara dentro de PING («Escanear QR») requiere HTTPS con un certificado que el iPhone confíe. **INICIAR-PING lo prepara solo**: el panel muestra **Abrir PING por HTTPS**, su QR con `https://<IP del Wi-Fi>:<puerto>` y la tarjeta **Preparar el iPhone · una sola vez**.

**Una sola vez en cada iPhone** (mismo Wi-Fi que el PC):
1. Escanea con la app Cámara el QR **Preparar el iPhone** del panel (abre `http://<IP>:<puerto+1>/`) y pulsa **Descargar certificado PING** → *Permitir*.
2. Ajustes → **Perfil descargado** → Instalar. Si no aparece: General → VPN y gestión de dispositivos. iOS descarta el perfil a los 8 minutos si no se instala. [Apple: instalar un perfil](https://support.apple.com/102400).
3. Ajustes → General → Información → **Ajustes de confianza de certificados** → activa «PING CA local». Es obligatorio para certificados descargados de un sitio web. [Apple: confiar en un certificado instalado](https://support.apple.com/102390).
4. Escanea el QR de acceso HTTPS del panel. Safari debe abrir PING **sin aviso de seguridad**.
5. En PING pulsa **Escanear QR** → *Permitir*.

Antes de confiar puedes comparar la huella SHA-256 que muestra el panel con la del perfil en el iPhone (Más detalles). Ninguna herramienta de Windows puede instalar el perfil, confiar la CA ni aceptar el permiso de cámara en el teléfono.

**Si se denegó la cámara:** en Safari, con PING abierto, menú de la página (aA) → Ajustes del sitio web → Cámara → Preguntar o Permitir; recarga y vuelve a pulsar «Escanear QR». También: Ajustes → Apps → Safari → Cámara. El código manual sigue disponible siempre.

**Siguientes arranques:** se reutilizan la CA y el certificado. Si cambia la IP del Wi-Fi, se emite otro certificado de servidor con **la misma CA**: el iPhone no repite la preparación. Lo mismo ocurre cuando faltan menos de 30 días para la caducidad del certificado (397 días). Solo crear una CA nueva obliga a confiar otra vez. Eso ocurre si eliges *NUEVA* en Configurar → HTTPS o si retiras las credenciales, porque entonces se pierde la contraseña cifrada de la CA.

**Otras opciones** en Configurar → HTTPS:
- 2 · HTTP de ensayo con código manual, sin cámara.
- 3 · Un PKCS12 propio existente. Se conserva y se comprueba sin omitir certificados.

No se rebaja TLS silenciosamente.

**En este PC (opcional):** el navegador de Windows avisará en `https://<IP>` hasta que este usuario confíe en la misma CA. El panel ofrece **Confiar en este PC**, que añade solo el certificado público al almacén del usuario actual (`certutil -user`); Windows pide confirmar. No necesita administrador y no afecta al iPhone.

Si el teléfono no conecta, revisa redes de invitados/aislamiento, VPN y perfil de red. El panel ofrece **Permitir Wi-Fi privado**, que solicita UAC exclusivamente para una regla de Java/puerto, subred local y perfil privado. Con HTTPS local, la misma autorización incluye el Python del lanzador en el puerto de preparación, que entrega solo el certificado público; no eleva PING, no cambia el perfil de red ni abre el router. No se ejecuta esa acción automáticamente. [Alcance de reglas de Windows](https://learn.microsoft.com/en-us/troubleshoot/windows-server/networking/netsh-advfirewall-firewall-control-firewall-behavior). No se usaron túneles ni servicios externos.

### Diagnóstico y mantenimiento existentes

Se conservan `tools/windows/Test-Ping.ps1`, `Backup-Ping.ps1` y `Restore-PingBackup.ps1`. El panel evita necesitar el diagnóstico para cada arranque. Los respaldos siguen siendo privados y separados de Git; la configuración cifrada no sustituye un respaldo de datos.

## 3. Respaldar y comprobar recuperación

```powershell
.\tools\windows\Backup-Ping.ps1
```

Reutiliza o solicita la conexión como el arranque. Evita escrituras y cambios de estructura durante el respaldo; puedes detener TU PING explícitamente con DETENER-PING.cmd antes, manteniendo MySQL. El script no reinicia ni detiene servicios. Usa mysqldump binario de todas las tablas InnoDB con single-transaction, hex-blob, no-tablespaces y GTID_PURGED=OFF; incluye cuentas, saldos, Saga, canjes, planos, versiones y coordenadas. No redirige SQL mediante PowerShell5.1, que podría alterar su codificación.

Destino predeterminado: `%LOCALAPPDATA%\PING\backups`, en una subcarpeta NUEVA con fecha UTC y sufijo aleatorio. `-Destination` permite otra carpeta privada FUERA del repositorio, incluso al resolver enlaces. En Windows limita la ACL de la subcarpeta nueva al usuario actual, sin cambiar permisos globales ni de carpetas existentes. Si no puede protegerla, no genera el dump. No sobrescribe archivos anteriores. Captura TODAS las tablas presentes, también de un esquema PING anterior a cuentas/planos, para permitir respaldo antes de aplicar migraciones; restaura ese esquema real sin adelantar sus versiones.

Entrega tres archivos que debes conservar juntos y sin renombrar: `.sql`, `.sql.sha256`, `.sql.json`. Verifica código de salida, tamaño no vacío, SHA-256 y huellas de estructura/datos antes/después. El manifiesto solo guarda huellas/recuentos y metadatos técnicos, pero TODO el paquete es privado. Contraseña solo en memoria/entorno del proceso hijo, nunca en argumentos, archivos temporales o logs; no hay archivo de credenciales que limpiar. `MYSQL_PWD` puede ser visible para el propietario/administrador del proceso: usa un PC de confianza y no captures su entorno. El script limpia su copia al terminar y restaura las variables previas.

**Archivo no vacío + checksum no demuestran restaurabilidad.** Si la fuente cambió durante la captura, se conserva el SQL privado sin manifiesto válido y se pide repetir en otra carpeta cuando no haya escrituras. Un fallo de mysqldump deja un parcial privado, nunca un respaldo declarado válido. No publiques SQL, manifiestos privados, correos, cookies ni planos de uso.

Para el ensayo opcional, dentro de este mismo paso:

```powershell
.\tools\windows\Restore-PingBackup.ps1
```

Pide la ruta SQL, valida sus sidecars/checksum, propone una base `ping_restore_exclusive_...`, muestra origen/servidor/destino y exige escribir el nombre NUEVO exacto. Rechaza cualquier destino existente, incluso vacío, y jamás hace DROP DATABASE/clean ni importa sobre la base habitual. Necesita permisos MySQL para crear base, crear usuario y conceder permisos solo en esa base. Si no los tienes, falla sin promover cuentas ni cambiar permisos existentes.

Comprueba de nuevo el SHA-256 del mismo archivo abierto justo antes y después de importar, para detectar cambios durante el ensayo. Importa con una cuenta temporal limitada a la base nueva y a tu host de conexión, sin privilegios globales/FILE ni grant option. El cliente desactiva comandos de shell y local-infile durante el batch. Compara todas las tablas/definiciones, recuentos y SHA-256 de filas ordenadas, incluidos bytes de planos y estados/saldos. Retira el usuario temporal tanto al terminar como al fallar. No cambia DB_URL habitual. Conserva base de ensayo, respaldo y registro privado `restore_...json`; un error deja identificada una base NUEVA parcial, que no debe reutilizarse como destino. Si no consigue retirar el usuario, informa su nombre para retirada privilegiada explícita; no lo oculta como éxito.

El checksum detecta alteración accidental, no acredita autoría: importa únicamente tu paquete privado conocido. Si decides abrir PING contra el ensayo, hazlo en otra PowerShell, con una asignación explícita de DB_URL a ESA base y otro puerto. El script de restauración no hace ese cambio por ti. No borres versiones antiguas de planos que aún necesitan encuentros activos.

GitHub respalda **código, migraciones y evidencia ficticia**. MySQL respalda **datos de uso y planos**; actualizar la rama no recupera cuentas, imágenes ni saldos perdidos.

### Evidencia histórica de DEC-051 (anterior al doble clic)

En este entorno Linux: compilación Python,17 pruebas unitarias dirigidas; conexión MySQL8.4.11 con VERIFY_CA, respaldo binario real de35 tablas/260430bytes y restauración real en base exclusiva con todas las huellas coincidentes. Destino existente y checksum alterado rechazados; SQL de ensayo que intenta cambiar a otra base exclusiva denegado por el usuario restringido1044. Segundo restore con el lector final vuelve a coincidir en35 tablas; comando de shell en SQL rechazado sin crear su archivo de prueba. Origen y destino previo no recibieron el cambio de saldo intentado; blobs conservan cantidad/bytes y quedan0 cuentas temporales. Dumps/logs quedan fuera de Git. No se repitió la suite Java porque la aplicación no cambia.

**Aquella validación se hizo sin Windows ni PowerShell.** Los `.ps1` tienen revisión estática y UTF-8 BOM para Windows PowerShell5.1; no se presentan como ejecutados. Quedan por comprobar en tu PC: parser/host PowerShell5.1+, descubrimiento de Java/Python/clientes, prompt de red/puerto, ACL NTFS, ventana del buzón, Ctrl+C y acceso Wi-Fi del teléfono. Incluimos `tools/windows/Test-Scripts.ps1`, que usa el parser nativo sin ejecutar operaciones ni instalar Pester, para comprobar sintaxis cuando haya PowerShell. Si una política bloquea scripts, no la cambies automáticamente: usa la política autorizada de tu PC.


## Validación Windows del doble clic · 25 octubre 2026

Petición de Samuel: herramientas locales de arranque, sin cambios de producto, aplicación, ramas, commit/push/merge. Se conserva DEC-058 y la auditoría con las correcciones posteriores de Claude. El Issue de publicación de esta mejora queda por preparar cuando se autorice publicar; esta petición fija el alcance operativo.

- 37 pruebas dirigidas de herramientas Python en Windows: DPAPI real y alteración de cifrado, entrada oculta y reutilización, redes múltiples/IP cambiada, puertos ocupados, SMTP existente/cerrado, proceso con PID reutilizado, entrada CMD/ayuda desde un clon mínimo con espacios y acentos (la compilación completa se ejecutó en el clon original), caché de compilación y guardas Host/CSRF del panel. No se ejecutaron las 310 pruebas de aplicación.
- Ensayo nativo con MySQL nuevo y desechable, base `ping_launcher_exclusive`, contraseña sintética aleatoria solo en memoria/DPAPI y SMTP local: primer arranque desde otra carpeta, reapertura con el mismo PID, compilación inicial y posterior reutilización, error 1045 y recuperación con credencial válida, puerto ocupado sin matar al ocupante, buzón existente conservado y posterior arranque del buzón propio faltante, panel/QR/status/buzón HTTP 200 y detención de procesos propios. MySQL habitual no se usó ni se detuvo.
- Mensaje sintético enviado exclusivamente al SMTP loopback, almacenado en la carpeta anunciada por el helper y consultado en el panel; HTML del mensaje escapado. No se enviaron correos externos.
- El secreto sintético no aparece en salida, logs inspeccionados ni archivos fuente/herramientas del proyecto. Configuración cifrada comprobada en disco; proceso Java sin contraseña en argumentos.
- Captura real revisada: [panel de acceso](screenshots/windows-launcher/panel.png). La dirección de esa imagen pertenece al ensayo; no debe copiarse como dirección permanente. [Resultados sin secretos](screenshots/windows-launcher/verificacion.json), [correo local](screenshots/windows-launcher/correo-local-verificado.json).

Límites: falta probar desde el iPhone físico y su Wi-Fi/Safari, aceptar en persona los permisos que correspondan, preparar/confiar el certificado HTTPS inicial si se quiere cámara y probar correo real con configuración del propietario. No se modificó firewall en las pruebas ni se aceptó UAC automáticamente. El QR es acceso a PING; no certifica cámara ni conectividad entre dispositivos. Los procesos de ensayo se cerraron; sus archivos privados identificados permanecen fuera del repositorio. No se borraron árboles de carpetas para limpiar evidencias. El cierre final detectó y corrigió el watchdog/hijo de MySQL del banco de pruebas: se cerraron las cuatro instancias propias mediante SHUTDOWN tras verificar su datadir exacto; 3306/PID 8704 permanece intacto. [Cierre comprobado](screenshots/windows-launcher/cierre-mysql-ensayo.json).

La protección DPAPI utiliza el usuario/equipo actuales, no el modo máquina compartido: [documentación de Microsoft](https://learn.microsoft.com/en-us/windows/win32/seccrypto/example-c-program-using-cryptprotectdata).

## HTTPS local para la cámara del iPhone · 25 octubre 2026 (Claude)

Petición de Samuel: el escáner QR **dentro de PING** debe funcionar en Safari de iPhone por Wi-Fi. Son herramientas locales y no cambian reglas de producto. Sin commit, push, merge ni despliegue.

**Arquitectura y motivo.** Tomcat de Spring Boot sirve TLS directamente con un PKCS12 (`SERVER_SSL_*`, que el lanzador ya admitía). No hay proxy ni cabeceras reenviadas que confiar. `PING_PUBLIC_BASE_URL` pasa a ser el origen HTTPS, así que los enlaces de correo, el QR de entrada al evento y el origen permitido del escáner usan esa dirección.

Los certificados los genera **`keytool` del JDK 21 que PING ya exige**: no hay descargas ni dependencias nuevas, y se descartaron mkcert y un proxy por innecesarios.
- **CA local:** una por usuario de Windows y clon, RSA 3072, SHA-256, `CA:true, pathlen:0`, 10 años. Se crea **una sola vez**.
- **Certificado de servidor:** RSA 2048, SHA-256, SAN = **IP del Wi-Fi**, EKU serverAuth y 397 días, por debajo de los 825 que exige Apple ([requisitos de iOS](https://support.apple.com/103769)).
- **Renovación:** se reemite con la misma CA si cambia la IP, si cambia la CA o si faltan menos de 30 días.

Para que el iPhone descargue la CA **antes** de confiar en ella, el proceso de trabajo del lanzador sirve por HTTP una página fija, ligada a la IP del Wi-Fi en el puerto de PING+1. Solo contiene instrucciones y `PING-CA-local.cer` (certificado público). Cualquier otra ruta da 404 y tiene CSP estricta.

**Archivos:**
- Nuevos: `tools/windows/ping_tls.py`, `tools/windows/tests/test_https.py` y `tools/windows/tests/verify_https_windows.py`.
- Modificados:
  - `ping_launcher.py`: modo HTTPS, preparación de certificados al arrancar, verificación con la CA, página de preparación, regla de firewall y acción `trust-pc`.
  - `ping_panel.py`: estado HTTPS, QR de preparación y ayuda de cámara.
  - `ping_ops.py`: comando `trust-pc`.
  - `tests/verify_launcher_windows.py`: comprobación TLS con la CA.
  - `src/main/resources/static/js/qr-scanner.js`.
- Al validar se corrigió en `build()` un fallo previo: si el entorno define `NoDefaultCurrentDirectoryInExePath`, `cmd` no encontraba `mvnw.cmd`.

**Ubicación, sin secretos.** Todo está en `%LOCALAPPDATA%\PING\launcher\<id-del-clon>\https\`, con la ACL privada del usuario:
- `ca.p12` (clave de la CA);
- `server.p12` (clave del servidor);
- `ping-ca.cer` / `ping-ca.pem` (públicos);
- `server.pem`.

Las contraseñas de ambos almacenes se generan al azar, viven solo en `config.dpapi` y llegan a `keytool` por variables de entorno del proceso hijo, nunca por argumentos ni registros. Ni el panel ni la página de preparación sirven claves privadas.

**Escáner** (`qr-scanner.js`). Conserva:
- Arranque solo por acción del usuario.
- Cámara trasera preferida, `playsinline` y `muted`.
- `BarcodeDetector` o, si no existe (Safari), el ZXing local con SRI.
- Prevención de lecturas duplicadas.
- Liberación de la cámara al cancelar, salir de la pantalla o descargar la página.

Añade:
- Mensajes distintos para: falta de HTTPS o confianza, navegador sin cámara, permiso denegado (con la ruta de Safari), bloqueo de seguridad, cámara elegida desaparecida, lector no cargado, QR ilegible (aviso a los 12 s) y QR válido de otro flujo.
- Estado en `data-scan-state`.
- En la entrada al evento acepta el QR `http://` antiguo del mismo host, porque solo extrae el código y nunca navega.

El servidor sigue validando formato, propósito, evento, emisor, caducidad, consumo, límites, CSRF y autorización. La cámara no salta ninguna condición.

**Comprobado en este PC:**
- **42 pruebas Python** (`test_https`, `test_launcher`, `test_ops`).
- **Ensayo nativo** `verify_https_windows.py`: **57/57**, con MySQL nuevo y desechable, entrada por `INICIAR-PING.cmd`/`DETENER-PING.cmd` y Wi-Fi real 192.168.1.8.
  - **Certificados y TLS:** CA y certificado creados una vez. TLS 1.3 verificado con la CA; rechazado solo con el almacén de Windows y para otra IP. HTTP plano en el puerto HTTPS devuelve 400.
  - **Página de preparación:** entrega exactamente el `.cer`; las rutas privadas dan 404.
  - **Panel:** muestra el estado HTTPS verificado y los QR regenerados.
  - **Aplicación por HTTPS:** cookies `Secure`/`HttpOnly`, HSTS, redirección de login al origen HTTPS, páginas sin contenido mixto, formulario POST y enlace de entrada al evento en HTTPS. Sondeo `/me/state` y enlace del correo de verificación en HTTPS.
  - **Reutilización:** el segundo arranque mantiene la misma instancia, la CA y el certificado; tras pasar por HTTP y volver a HTTPS, misma CA. Sin secretos en salidas ni registros.
  - **Parada:** solo procesos propios y el puerto de preparación cerrado. El MySQL aislado se apagó tras comprobar su `datadir`.
  - [Resultados](screenshots/windows-launcher/https/verificacion-https.json).
- **Escáner en Chromium** con cámara simulada que mostraba los QR reales de la app, desde el origen seguro `http://127.0.0.1`:
  - En Wi-Fi por HTTP: aviso de HTTPS y entrada manual.
  - QR de evento: código leído, cámara liberada y envío.
  - Permiso denegado: mensaje, y reintento correcto tras concederlo.
  - Cancelar y salir de la pantalla: pistas terminadas y reapertura posible; una segunda pulsación no abre otra cámara.
  - QR del evento en la verificación del encuentro: «no corresponde», sin rellenar el campo.
  - Código de encuentro sustituido por renovación: leído y **rechazado por el servidor**.
  - Código vigente: verificado; reenviarlo no duplica.
  - Imagen sin QR: aviso de ilegible.
  - [Datos](screenshots/windows-launcher/https/escaner-chromium.json), [cancelar/salir](screenshots/windows-launcher/https/escaner-cancelar-salir.json).
- **Instalación real** de Samuel (base `ping_prueba_movil_20261017`, MySQL 3306, sin modificar datos): se detuvo con DETENER-PING para recompilar y se reinició en HTTPS local.
  - Origen `https://192.168.1.8:8081` verificado y preparación en `http://192.168.1.8:8082/`.
  - Ambos QR del panel, decodificados con ZXing, contienen exactamente esas direcciones.
  - Capturas: [panel](screenshots/windows-launcher/https/panel-https-escritorio.png), [preparación a 390](screenshots/windows-launcher/https/preparar-iphone-390.png) y [a 360](screenshots/windows-launcher/https/preparar-iphone-360.png).
  - La red «Crismar» figura como **Pública**. Ya existen reglas de entrada de Windows para ese Java de Adoptium y para el Python 3.14 del lanzador en el perfil público, así que no se cambió el firewall.

**Pendiente, solo en el iPhone:**
- Preparación única (descargar, instalar, confiar).
- Apertura de `https://192.168.1.8:8081` sin aviso.
- Permiso de cámara en Safari.
- Lectura real con la cámara física de un QR de evento y de un código de encuentro.
- Comportamiento al cambiar de app o bloquear la pantalla.

Una cámara simulada en Chromium no certifica Safari ni la cámara física. **El soporte en iPhone no se declara terminado hasta esa prueba.** Si el iPhone no llega a la IP: misma red, aislamiento de clientes en el router o VPN.

**Procedencia.** Toda la documentación citada es de Apple ([102400](https://support.apple.com/102400), [102390](https://support.apple.com/102390), [103769](https://support.apple.com/103769)). La herramienta `keytool` viene con el JDK 21 instalado.
