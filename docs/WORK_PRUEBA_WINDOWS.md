# PING en Windows: diagnosticar, arrancar y respaldar

Bloque operativo DEC-051, rama **feature/windows-operations**, basada en d481cc1/PR47 e incluida toda la integración anterior. La [continuidad única](ENCUENTRO_CONFIGURABLE_LLEGADA.md) y la PR contienen el respaldo remoto actual. No hay que hacer merge ni cambiar main para probar.

El clon de Samuel está en `C:\Users\river\PING-movil`. **Los scripts resuelven la raíz desde su propia ubicación**, también si cambias de carpeta o el clon tiene espacios. Reutilizan JDK21, Maven Wrapper, Python3.10+ (también usado por el buzón) y clientes MySQL8.4 ya instalados. No instalan herramientas, paquetes ni skills. No cambian firewall, ExecutionPolicy, router, certificados o servicios ajenos.

Antes de actualizar, `git status --short`: conserva cambios versionados en su rama; no uses reset/clean ni borres `.codex/`, bundles u otros archivos. Para un clon `--single-branch`:

```powershell
cd C:\Users\river\PING-movil
git remote -v
git remote set-branches --add origin feature/windows-operations
git fetch origin refs/heads/feature/windows-operations:refs/remotes/origin/feature/windows-operations
git switch feature/windows-operations
git pull --ff-only origin feature/windows-operations
git rev-parse HEAD
git status --short
```

## 1. Diagnosticar

```powershell
.\tools\windows\Test-Ping.ps1
```

Consulta Java/JDK21, wrapper, Python, presencia/ausencia de configuración sin mostrar secretos, escuchas TCP, ruta pública local `/login`, IPv4 elegida/base pública, servicio MySQL si Windows lo expone y conexión SQL si ya están las variables/clientes. Propone consultar8081; si PING usa otro puerto: `Test-Ping.ps1 -Port 8080`. Puedes seleccionar IPv4 por número; no presupone que la primera interfaz sea Wi-Fi. `-LanIPv4` permite indicar explícitamente una de las IPv4 activas.

Es de lectura: no pide contraseñas ni cambia variables, procesos, servicios o configuraciones. Si faltan datos/clientes, informa qué no comprobó. Una respuesta200 local no demuestra acceso entre celulares, firewall, aislamiento de clientes del router, Safari ni permisos de cámara. Una escucha solo127.0.0.1 no admite el celular;0.0.0.0 incluye las IPv4 locales, pero no prueba la red del teléfono. Una base pública diferente puede corresponder a un proxy HTTPS real: el diagnóstico no la reemplaza.

## 2. Arrancar para el celular

```powershell
.\tools\windows\Start-Ping.ps1
```

El asistente propone **8081** o tu SERVER_PORT existente y permite elegir otro. Comprueba que esté libre, sin detener al ocupante, y exige elegir la interfaz compartida con el celular. Reutiliza `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`; solo solicita lo que falta, con contraseña oculta. La URL JDBC no debe incluir usuario/contraseña: se pasan por sus variables separadas. La cuenta MySQL es distinta de una cuenta PING. No existe contraseña universal y no se solicita por chat.

Busca `mysql.exe` en PATH o en la instalación habitual `Program Files\MySQL\MySQL Server 8.4\bin`. Si tu instalación está en otro lugar, puedes usar `-MySqlPath` y, en respaldo, `-MySqlDumpPath` con sus rutas existentes. No instala MySQL ni cambia su servicio. La configuración admite un servidor JDBC, puerto, base de letras/números/guion bajo y TLS; no implementa URLs multi-host/SRV. Respeta sslMode y las opciones anteriores useSSL/requireSSL; no rebaja TLS. Con VERIFY_CA/VERIFY_IDENTITY y truststore Java, indica `-SslCa` con el PEM del servidor existente para los clientes MySQL: un truststore Java no es un PEM. Su contraseña permanece oculta y no se pasa al cliente CLI.

Con MySQL comprobado, ofrece el **buzón local de ensayo existente**. Si aceptas, abre Python en otra ventana, solo127.0.0.1:1025, y muestra dónde leer los `.eml` privados: `PING-dev-mailbox` del perfil del usuario, fuera del clon. No envía a Internet. Si1025 está ocupado, exige confirmar que es TU buzón existente; no lo supone ni lo detiene. Solo cierra el buzón que él mismo inició. Si mantienes SMTP remoto, no desactiva su TLS ni inventa credenciales. Sin correo configurado no hay registro/recuperación por correo; las cuentas existentes pueden entrar. [Configuración de cuentas y primer general](ACCOUNTS_SETUP.md): el asistente no crea/promueve cuentas ni activa demo/proveedores.

Muestra la **dirección exacta** para PC y celulares de la misma red, configura temporalmente0.0.0.0/puerto/base LAN y ejecuta Maven Wrapper con perfil mysql. Espera `Started PingApplication`. Mantén la consola abierta; Ctrl+C termina tu ejecución. Si la base pública existente difiere, pide confirmación LAN antes de cambiarla temporalmente; `-UseLanHttp` expresa esa elección. Si tu configuración exige TLS/cookie Secure, se detiene en vez de degradarla. Restaura las variables previas al salir, incluso ante error; no guarda contraseñas en archivos.

**HTTP LAN sirve para navegación y códigos manuales; el escáner interno requiere HTTPS con certificado confiado por el teléfono.** Cambiar una variable a https no crea TLS. No desactives protecciones del navegador. Si no abre en el celular, ejecuta diagnóstico y comprueba en el teléfono la misma red efectiva, la IP actual y posible aislamiento del router. No se reutiliza la antigua IP172.20.10.4.

Recorrido breve: Organización→Código y QR proporciona el código de TU evento. Los datos ficticios del executor no vienen con Git. En dos celulares/sesiones: código→registro/verificación→alias/avatar→gustos→Mi Ping/Conectar→aceptación de ambos→punto/plano→ambas llegadas→códigos personales cruzados→confirmaciones→puntos→beneficio/recibo. La palabra compartida sirve para reconocerse, no como código personal ni de canje. Un encargado que sea otra cuenta autorizada valida y entrega el recibo.

## 3. Respaldar y comprobar recuperación

```powershell
.\tools\windows\Backup-Ping.ps1
```

Reutiliza o solicita la conexión como el arranque. Evita escrituras y cambios de estructura durante el respaldo; puedes detener TU PING explícitamente con Ctrl+C antes, manteniendo MySQL. El script no reinicia ni detiene servicios. Usa mysqldump binario de todas las tablas InnoDB con single-transaction, hex-blob, no-tablespaces y GTID_PURGED=OFF; incluye cuentas, saldos, Saga, canjes, planos, versiones y coordenadas. No redirige SQL mediante PowerShell5.1, que podría alterar su codificación.

Destino predeterminado: `%LOCALAPPDATA%\PING\backups`, en una subcarpeta NUEVA con fecha UTC y sufijo aleatorio. `-Destination` permite otra carpeta privada FUERA del repositorio, incluso al resolver enlaces. En Windows limita la ACL de la subcarpeta nueva al usuario actual, sin cambiar permisos globales ni de carpetas existentes. Si no puede protegerla, no genera el dump. No sobrescribe archivos anteriores.

Entrega tres archivos que debes conservar juntos y sin renombrar: `.sql`, `.sql.sha256`, `.sql.json`. Verifica código de salida, tamaño no vacío, SHA-256 y huellas de estructura/datos antes/después. El manifiesto solo guarda huellas/recuentos y metadatos técnicos, pero TODO el paquete es privado. Contraseña solo en memoria/entorno del proceso hijo, nunca en argumentos, archivos temporales o logs; no hay archivo de credenciales que limpiar. `MYSQL_PWD` puede ser visible para el propietario/administrador del proceso: usa un PC de confianza y no captures su entorno. El script limpia su copia al terminar y restaura las variables previas.

**Archivo no vacío + checksum no demuestran restaurabilidad.** Si la fuente cambió durante la captura, se conserva el SQL privado sin manifiesto válido y se pide repetir en otra carpeta cuando no haya escrituras. Un fallo de mysqldump deja un parcial privado, nunca un respaldo declarado válido. No publiques SQL, manifiestos privados, correos, cookies ni planos de uso.

Para el ensayo opcional, dentro de este mismo paso:

```powershell
.\tools\windows\Restore-PingBackup.ps1
```

Pide la ruta SQL, valida sus sidecars/checksum, propone una base `ping_restore_exclusive_...`, muestra origen/servidor/destino y exige escribir el nombre NUEVO exacto. Rechaza cualquier destino existente, incluso vacío, y jamás hace DROP DATABASE/clean ni importa sobre la base habitual. Necesita permisos MySQL para crear base, crear usuario y conceder permisos solo en esa base. Si no los tienes, falla sin promover cuentas ni cambiar permisos existentes.

Importa con una cuenta temporal limitada a la base nueva y a tu host de conexión, sin privilegios globales/FILE ni grant option. El cliente desactiva comandos de shell y local-infile durante el batch. Compara todas las tablas/definiciones, recuentos y SHA-256 de filas ordenadas, incluidos bytes de planos y estados/saldos. Retira el usuario temporal tanto al terminar como al fallar. No cambia DB_URL habitual. Conserva base de ensayo, respaldo y registro privado `restore_...json`; un error deja identificada una base NUEVA parcial, que no debe reutilizarse como destino. Si no consigue retirar el usuario, informa su nombre para retirada privilegiada explícita; no lo oculta como éxito.

El checksum detecta alteración accidental, no acredita autoría: importa únicamente tu paquete privado conocido. Si decides abrir PING contra el ensayo, hazlo en otra PowerShell, con una asignación explícita de DB_URL a ESA base y otro puerto. El script de restauración no hace ese cambio por ti. No borres versiones antiguas de planos que aún necesitan encuentros activos.

GitHub respalda **código, migraciones y evidencia ficticia**. MySQL respalda **datos de uso y planos**; actualizar la rama no recupera cuentas, imágenes ni saldos perdidos.

### Qué se comprobó y qué falta en tu PC

En este entorno Linux: compilación Python,13 pruebas unitarias dirigidas; conexión MySQL8.4.11 con VERIFY_CA, respaldo binario real de35 tablas/260430bytes y restauración real en base exclusiva con todas las huellas coincidentes. Destino existente y checksum alterado rechazados; SQL de ensayo que intenta cambiar a otra base exclusiva denegado por el usuario restringido1044. Dumps/logs quedan fuera de Git. No se repitió la suite Java porque la aplicación no cambia.

**No hay Windows ni PowerShell aquí.** Los `.ps1` tienen revisión estática y UTF-8 BOM para Windows PowerShell5.1; no se presentan como ejecutados. Quedan por comprobar en tu PC: parser/host PowerShell5.1+, descubrimiento de Java/Python/clientes, prompt de red/puerto, ACL NTFS, ventana del buzón, Ctrl+C y acceso Wi-Fi del teléfono. Incluimos `tools/windows/Test-Scripts.ps1`, que usa el parser nativo sin ejecutar operaciones ni instalar Pester, para comprobar sintaxis cuando haya PowerShell. Si una política bloquea scripts, no la cambies automáticamente: usa la política autorizada de tu PC.
