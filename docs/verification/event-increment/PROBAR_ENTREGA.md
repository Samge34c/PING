# Probar la rama de entrega en local

Rama `feature/event-increment-delivery`, PR en borrador hacia `deploy/render-demo`. El commit de entrega contiene el mismo código, V24, recursos y pruebas del [manifiesto comprobado](source-manifest.json); después de las 369 pruebas solo se añadió documentación. La autorización posterior permite commit/push/PR y mantiene prohibidos merge, despliegue y acceso a Aiven/producción.

## Obtener esta versión

Usar un clon nuevo o un worktree propio; no cambiar ni limpiar la copia principal de Claude. PowerShell, Git, JDK 21, Maven Wrapper y Python 3 existentes:

```powershell
git clone --branch feature/event-increment-delivery --single-branch https://github.com/Samge34c/PING.git PING-review
Set-Location PING-review
git rev-parse HEAD
git ls-remote origin refs/heads/feature/event-increment-delivery
```

Ambos SHA deben coincidir con la cabecera del PR. Para fijar la revisión después de futuros cambios, usar el SHA de entrega indicado en el PR: `git switch --detach SHA_COMPLETO`. No usar la URL de Render como si ejecutara esta rama.

## Aplicación completa con H2 y correo de bucle local

No usar el perfil mysql/render, `.env` de producción ni credenciales existentes. Los argumentos siguientes fijan una base H2 **en memoria**, dirección 127.0.0.1 y transporte de correo local. La base y sus cuentas desaparecen al detener Java; no se modifica una base anterior. El perfil bootstrap no sirve para revisar el incremento completo porque desactiva persistencia.

Preparar clases y dependencias, sin repetir la suite:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress -DskipTests package dependency:build-classpath '-Dmdep.includeScope=test' '-Dmdep.outputFile=target/review-classpath.txt'
if ($LASTEXITCODE -ne 0) { throw 'No se pudo preparar la revisión' }
```

H2 es dependencia de prueba. El classpath de arranque añade sus dependencias pero **excluye `target/test-classes`**, para mantener cuentas, roles y permisos reales de la aplicación.

Terminal 1, desde el clon, mantener abierto:

```powershell
python tools/dev-mailbox.py --port 1041 --output "$env:TEMP\PING-review-delivery-mail"
```

Terminal 2, desde el mismo clon, elegir una contraseña local nueva de al menos 12 caracteres; no guardarla en archivos ni reutilizar una real:

```powershell
$env:PING_BOOTSTRAP_EMAIL = 'organizacion@review.test'
$env:PING_BOOTSTRAP_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Contraseña local nueva (12 o más caracteres)' -AsSecureString)).Password
$reviewClasspath = 'target/classes;' + (Get-Content -Raw target/review-classpath.txt).Trim()
try {
  & java '-cp' $reviewClasspath 'com.ping.PingApplication' `
    '--spring.profiles.active=review-delivery' `
    '--server.address=127.0.0.1' '--server.port=8121' `
    '--spring.datasource.url=jdbc:h2:mem:review-delivery;MODE=MySQL;DB_CLOSE_DELAY=-1' `
    '--spring.datasource.username=sa' '--spring.datasource.password=' `
    '--spring.datasource.hikari.max-lifetime=0' '--spring.datasource.hikari.idle-timeout=0' `
    '--spring.jpa.hibernate.ddl-auto=validate' '--spring.flyway.enabled=true' `
    '--ping.public-base-url=http://127.0.0.1:8121' `
    '--ping.demo.enabled=false' '--ping.oauth.enabled=false' `
    '--ping.mail.api.provider=none' '--ping.mail.from=local@review.test' `
    '--spring.mail.host=127.0.0.1' '--spring.mail.port=1041' `
    '--spring.mail.username=' '--spring.mail.password=' `
    '--spring.mail.properties.mail.smtp.starttls.enable=false' `
    '--spring.mail.properties.mail.smtp.starttls.required=false'
} finally {
  Remove-Item Env:PING_BOOTSTRAP_EMAIL, Env:PING_BOOTSTRAP_PASSWORD -ErrorAction SilentlyContinue
}
```

Las conexiones sin caducidad son exclusivamente para este ensayo H2: evitan el fallo de restricción/sesión cerrada documentado en el informe, sin cambiar la configuración del producto. Si los puertos están ocupados, elegir otros y actualizar puerto, URL pública y SMTP de forma coherente; no detener procesos ajenos.

## Recorrido de revisión

1. Abrir `http://127.0.0.1:8121/login`, entrar con la cuenta local de organización. Crear un evento, revisar `/events/ID/preparation` y abrirlo con las reglas existentes. Código/enlace, puntos/actividades, plano, beneficios y entrega siguen en sus editores actuales. En opciones publicar ayuda/contacto ficticios y activar o dejar desactivado el reto. Añadir agenda con su zona y punto del mismo evento.
2. En dos perfiles distintos del navegador, abrir el enlace de ese evento y crear dos cuentas de prueba nuevas. Sus mensajes se guardan como `.eml` en la carpeta temporal del terminal 1; abrirlos en un cliente local de correo para seguir el enlace a 127.0.0.1 y verificar con la contraseña elegida. No se envían mensajes fuera del equipo. Conservar la continuación al evento, alias, avatar y gustos.
3. En `/me`, revisar pista vacía, guardada, Editar/Cancelar y borrado guardando vacío. Límite de 120 puntos de código Unicode, revisión/CSRF y recuperación de borrador siguen vigentes. Propuesta y ACTIVE mantienen su CTA dominante. La otra persona solo ve la pista durante el ACTIVE autorizado.
4. Revisar `/me/help`, `/me/agenda`, `/me/challenges` y `/me/experience`. Descargar PNG y SVG; comprobar avatar, evento, conexiones y **saldo actual**, sin datos ajenos ni pistas/códigos. Cero conexiones es válido. Agenda y reto sin configurar no bloquean nada.
5. Completar el recorrido real con las dos cuentas: disponibilidad/búsqueda, aceptar ambos, llegar ambos, mostrar/verificar códigos cruzados, actividad y confirmar ambos. Revisar saldo/canje/entrega desde una cuenta autorizada y reintentos. Para alcanzar el reto hacen falta dos encuentros COMPLETED reales con personas distintas; no editar datos ni fabricar logros. Los tests ya cubren aislamiento, versiones y reintentos.
6. Examinar 390 px primero, 360 px después, ambos temas y escritorio 1280. [Capturas de referencia reales](../../screenshots/event-increment/README.md). Detener solo estos terminales con Ctrl+C. No publicar buzón, sesiones, base, contraseñas o salidas de `target/`.

## Verificación y límites

La ejecución local conservada fue `clean verify`, **369 pruebas, 0 fallos/errores/omitidas**; [log/hashes/evidencia](README.md). El commit no cambia el código respecto al manifiesto. Los checks de GitHub se muestran por separado en el PR y pertenecen a su SHA.

Si se cambia código o se necesita ejecutar de nuevo la verificación en otra máquina:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress '-Dspring.profiles.default=test' clean verify
python docs/performance/frontend/tools/bootstrap_subset.py --check
```

No activar Google real ni proveedores de pago para este recorrido. iPhone/Safari físico, clientes reales de correo/Google y medición autenticada en Render siguen pendientes externos. Esta guía no autoriza merge, despliegue ni cambios en producción.

El [manifiesto publicado](published-source-manifest.json) identifica los blobs Git de las mismas fuentes comprobadas; [revisión de publicación](publication-review.json). El manifiesto original conserva los bytes del worktree Windows: Git solo normaliza CRLF/LF en 640 archivos, sin cambios de comportamiento. Los binarios permanecen idénticos. Se ensayaron literalmente los argumentos de arranque de esta guía: login 200 y 24 migraciones en H2 aislada; procesos propios detenidos.
