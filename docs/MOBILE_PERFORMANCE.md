# Rendimiento móvil — DEC-052 / PR49

Base comprobada `feature/windows-operations@77cacb8bb1c893abf49fef51e1de22faecb9bf86`; rama `perf/mobile-journey`, [PR49 en borrador](https://github.com/Samge34c/PING/pull/49) hacia esa base. Autorización de Samuel, sin cambio de reglas/composición ni aprobación atribuida a Julián. Sin main/merge/force-push. Respaldos y siguiente acción se mantienen únicamente en [continuidad](ENCUENTRO_CONFIGURABLE_LLEGADA.md).

## Tres mejoras y límites

1. **Recursos repetidos:** Spring Security enviaba `no-store` también para archivos públicos; cada pantalla repetía fuente, CSS, iconos y JS. Spring genera URLs con huella del contenido, transforma referencias CSS y preload, y permite caché pública de un año SOLO si el handler es de recursos públicos y la huella corresponde al archivo actual. Huella incorrecta devuelve404; URLs antiguas siguen disponibles sin caché larga. No hay caché compartida de HTML, estados, imágenes privadas, recibos, cuentas o permisos. Una nueva versión de un archivo obtiene otra URL; los enlaces antiguos sin huella conservan compatibilidad.
2. **Transferencia fría:** gzip para CSS, JS, SVG y TTF>=2KB. HTML/JSON/PNG quedan excluidos, incluidos formularios y códigos privados. Se verifica `Vary: Accept-Encoding` y que descomprimir produce los mismos bytes. No se cambia formato/calidad de planos ni se pone lazy loading al recurso principal.
3. **Estado:** `/me/state` construía historial completo/DTOs con snapshots e intereses para138bytes de estado. Ahora una consulta específica obtiene solo revisión, flags y estado vigente; no selecciona palabra, pista, alias ajeno, actividad, coordenadas o bytes de imagen. Conserva firma existente, ambas orientaciones, versión propia del código y revisión ajena de pista. Continúan dos consultas frescas de seguridad por petición; no hay caché de datos. La subconsulta sigue buscando el encuentro vigente: no se promete coste SQL constante con cualquier volumen de historial ni se añade un índice sin evidencia.

**Ya optimizado y conservado:** polling serial con5s después de cada respuesta, timeout12s y pausa oculta; decoder441121bytes cargado solo al abrir cámara cuando falta detector nativo, con rotaciones anteriores; planos separados de listados/estado y versiones con caché privada inmutable (conservada), avatares SVG locales y scripts por pantalla. No cambia el decoder ni los intervalos. El editor de intereses aún solicita su formulario privado completo; no se lo cachea ni se añade una cuarta mejora.

## Condiciones y cifras

Chromium real emulado táctil390×844, CPU4x,120ms de latencia, descarga200000B/s (1.6Mbps), subida93750B/s. MySQL8.4.11 real/TLS en base exclusiva de ensayo. Evento NUEVO ficticio9, dos pares de cuentas NUEVAS verificadas con SMTP loopback; sin perfiles/datos demo ni correo externo. Misma máquina, puertos8093 base/8094 comparación, mismo esquema22/datos/configuración. Los recorridos funcionales también comprobaron360 claro y escritorio1440 oscuro.

Frío: limpiar caché HTTP antes de navegar. Caliente: volver desde `about:blank` en el mismo contexto. Bytes de `Network.loadingFinished.encodedDataLength` incluyen transferencia codificada/cabeceras; respuestas de caché local tienen0. Se cuentan aparte las peticiones con transferencia, no los eventos del navegador que sirven caché. Una pareja por pantalla para bytes; intereses espera también el formulario obtenido por fetch. Se excluye de la tabla el plano de un encuentro ya completado porque esa URL ya no representa la vista ACTIVE. [CSV completo](evidence/mobile-performance.csv).

| Pantalla | Frío, bytes antes → después | Caliente, bytes antes → después | Peticiones con transferencia, caliente |
|---|---:|---:|---:|
| Entrada |556173 →143311|556084 →5407|10 →1|
| Acceso |547642 →138975|547553 →3791|10 →1|
| Intereses, incluido formulario |733969 →332566|733969 →195816|12 →2|
| Mi Ping |567041 →155222|567041 →13388|13 →1|
| Conectar |558734 →146708|558734 →9544|10 →1|
| Beneficios |555423 →147578|555423 →13221|9 →1|
| Recibo |551361 →142804|551361 →8447|9 →1|

El control intermedio con caché pero SIN gzip no mejora el frío (recibo551361→551804, el nombre con huella añade bytes), sí el caliente551361→8447. La mejora fría final procede de gzip, no de demorar un recurso necesario. El documento privado sigue viajando completo.

Acceso `/login`, tres pares adicionales en orden base/final y condiciones iguales, tiempo hasta `load` (sin los500ms de estabilización del CSV): frío base3250–3391ms, mediana3330; final1144–1751ms, mediana1545. Caliente base3125–3201ms, mediana3173; final254–489ms, mediana464. Son observaciones del executor con carga variable, no Core Web Vitals ni garantía de velocidad en un teléfono. No se extrapolan porcentajes de costes/tokens.

| Estado,5 peticiones por versión | Consultas antes → después | Filas entregadas antes → después | JSON |
|---|---:|---:|---:|
| Sin encuentro vigente, mismo participante/datos |6 →3|5 →3|53bytes|
| ACTIVE, misma configuración/dos participantes por encuentro |9 →3|8 →3|138bytes|

ACTIVE previo fue encuentro4; final medido en encuentro6 con historial previo, luego cancelado por formulario y otra sesión actualizada aNONE. No se falsearon estados históricos para reconstruir una medición. El conteo usa un proxy JDBC de ensayo por petición (incluye JPA/JdbcTemplate/seguridad), sin registrar parámetros ni SQL con datos; no se incorpora a producción. Filas significa resultados leídos por JDBC, no filas examinadas internamente por MySQL. Los tiempos locales del estado se solapan: se afirma menos trabajo, no una latencia garantizada.

## Comportamiento comprobado

- Dos pares completaron código→cuenta/verificación→alias/avatar→intereses→Mi Ping→disponibilidad→propuesta descubierta por polling→aceptaciones→ACTIVE→plano→ambas llegadas/códigos/verificaciones→confirmaciones37/37→beneficio20/recibo17. El segundo par renovó código sin otro cargo; QR de cada persona distinto y privado. Plano con zoom/centrar/resize360/vuelta conserva0.4/0.5. No se ejecutó entrega ni cámara de nuevo; no se cambió su código y las suites existentes las protegen.
- Pista ajena actualizada mediante endpoint privado, nunca por `/me/state`. Una petición en vuelo aun con eventos online repetidos;0 consultas durante6500ms con evento de visibilidad oculto EMULADO, recuperación en1000ms; fallo de red recupera aviso y consulta a5.02s. No se presenta esa emulación como prueba física de segundo plano/Safari. Logout real en otra pestaña produce401 y reentrada; otra sesión sigue200.
- Dos cuentas/eventos concurrentes, IDs ajenos en query ignorados, evento ajeno403, recibo ajeno409 genérico sin condiciones privadas. Estado de evento cambia inmediatamente y `auth_epoch` revocado produce401. HTML/estado/PNG de códigos conservan no-store y no gzip. Huella inválida404; CSS/fuente con misma URL, gzip y Vary correctos.
- Firmas del nuevo estado comparadas con HTML de ambos participantes, aceptación parcial, ACTIVE, llegadas, verificación, confirmación parcial, Saga pendiente, pausa, cancelación;24 filas históricas terminales y política1 conservadas. No se limita historial ni se cambian snapshots/importes/reglas.

## Verificación y evidencia

Skills versionadas aplicadas manualmente: ping-efficient-work, systematic-debugging y verification-before-completion; fuera del catálogo automático. No reinstalación, hooks ni subagentes; mobile-native no fue necesaria porque no cambia interacción. Pruebas dirigidas de estado3, recursos2 y privacidad/concurrencia1 pasaron. El primer `clean verify` offline no pudo resolver el plugin Maven clean3.5.0: se obtuvo del repositorio MavenCentral a través del proxy/TLS existente, sin dependencias de aplicación nuevas. La suite general inicial ejecutó267 pruebas y detectó6 aserciones de nombres de recurso antiguos; se adaptaron a `ResourceUrlProvider`, incluyendo comprobaciones negativas. Los errores anteriores al interceptor MVC conservan URLs sin huella operativas; sus pruebas admiten ambas rutas válidas. `clean verify` final con JDK21/Wrapper: **267 pruebas,0 fallos/errores/omitidas y JAR**. No se repitieron suites después de ese resultado; los cambios posteriores son documentación. JAR normal reiniciado sobre MySQL: Flyway22 válido/up-to-date, Hibernate validate; ambas cuentas recuperadas por login, saldo17/37, recibo20 y plano21614bytes persistentes, evento ajeno403/recibo ajeno409/anónimo401. CSS con huella=gzip+immutable, URL antigua operativa sin caché larga. Los134 recursos fuente son idénticos en el JAR (licencias en META-INF raíz incluidas), sin fixture ni probe de medición.

El harness tuvo errores de URL duplicada, orden de ambas llegadas antes de emitir códigos, enlace de retorno ambiguo, nombre de disponibilidad, panel de seguridad plegado, URL relativa de imagen y expectativa de caché no-store del plano (ya era privada/inmutable), y expectativa403/404 del recibo (el contrato real es409 genérico). Se corrigió el harness, no el producto para satisfacerlo. Una primera medición de intereses terminaba antes de su fetch; se sustituyó por la medición completa publicada. Fallos y salidas originales se conservaron fuera de Git.

Capturas del evento ficticio, examinadas sin auditoría visual nueva: [ACTIVE390](screenshots/mobile-performance/encuentro-390.png), [360 claro](screenshots/mobile-performance/encuentro-360-claro.png), [plano360](screenshots/mobile-performance/plano-360.png), [recibo390](screenshots/mobile-performance/recibo-390.png); después [escritorio1440](screenshots/mobile-performance/recibo-escritorio.png). Composición/tema/fuente/avatares se conservan, sin overflow horizontal; plano y retorno siguen accesibles sobre la navegación. Los códigos/QR personales se ocultaron antes de capturar el recibo. No teléfono físico, Safari ni prueba nueva de cámara/proveedores.

Logs, scripts de medición, perfiles de navegador, correos y credenciales están en `/workspace/work/ping-perf`, fuera de Git y no son respaldo. GitHub respalda código/CSV/capturas ficticias; MySQL sigue conteniendo planos y datos, con respaldo propio. Herramientas Windows se conservan.

## Comprobación de Samuel desde celular

Con la app detenida y sin cambios propios en archivos versionados, desde su clon actual (también `--single-branch`):

```powershell
git remote set-branches --add origin perf/mobile-journey
git fetch origin refs/heads/perf/mobile-journey:refs/remotes/origin/perf/mobile-journey
git switch perf/mobile-journey
git pull --ff-only origin perf/mobile-journey
.\tools\windows\Test-Ping.ps1
.\tools\windows\Start-Ping.ps1
```

Usar la URL LAN que muestra Start-Ping en dos celulares de la misma Wi-Fi y el código de su evento (los datos de ensayo no se descargan de GitHub). Entrar, elegir gustos, activar disponibilidad y aceptar la propuesta en ambos; abrir plano y volver, registrar ambas llegadas, verificar el código de la otra persona y confirmar. Comprobar puntos/recibo y renovar sin otro cargo. Cámara solo con HTTPS confiado; códigos manuales siguen operativos por HTTP. Volver a varias pantallas: los recursos públicos se reutilizan, pero saldos/estado/recibos continúan frescos. No cambiar contraseñas ni MySQL para esta optimización; sin migraciones nuevas.
