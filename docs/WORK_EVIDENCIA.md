# Entrega Work — implementación y evidencia

Implementación del paquete conservado en [work-handoff](work-handoff/README.md), sobre8c94cd7/PR42. Se reutilizaron catálogo, SVG y estructura del prototipo; el prototipo no se presenta como producto ejecutable. Autorización de Samuel registrada en DEC-049, sin atribuir aprobación a Julián. La recuperación y los SHA/PR actuales se mantienen únicamente en [la continuidad](ENCUENTRO_CONFIGURABLE_LLEGADA.md).

## Capturas reales, móvil primero

Chromium con viewport390 y360, tacto emulado; escritorio1440 operativo. Son pantallas de PING conectado a MySQL8.4.11 exclusivo, con eventos, alias y plano **ficticios de prueba**. Sin credenciales personales ni QR privados en las imágenes. El plano no es oficial de SOFA. Las capturas muestran momentos distintos del recorrido: marcador original, sustitución sin recolocación y colocación posterior explícita.

- [Organizador: editar punto390](screenshots/work-integration/editor-punto-390.png), [editor360](screenshots/work-integration/editor-360.png), [escritorio](screenshots/work-integration/editor-desktop.png).
- [Organizador: colocación táctil390](screenshots/work-integration/organizador-plano-390.png), [participante: punto asignado390](screenshots/work-integration/participante-plano-390.png), [punto sin ubicar390](screenshots/work-integration/punto-sin-ubicar-390.png).
- [Avatar y saldo390](screenshots/work-integration/mi-ping-final-390.png), [360 claro](screenshots/work-integration/mi-ping-final-360.png).
- [Antes de llegadas390](screenshots/work-integration/active-sin-llegadas-390.png), [verificación bilateral390](screenshots/work-integration/verificacion-390.png), [360 claro](screenshots/work-integration/verificacion-360-claro.png), [recibo entregado390](screenshots/work-integration/recibo-entregado-final-390.png).
- [QR de entrada360](screenshots/work-integration/scanner-entrada-360.png), [origen HTTP y alternativa manual390](screenshots/work-integration/entrada-http-manual-390.png). El código de entrada visible es público y ficticio; no es una credencial personal.

## Cobertura

| Requisito | Implementación y comprobación |
|---|---|
| R01 editor | Formularios inline, editar/cancelar, chips y guardar por sección. POST nativo sin JS probado mediante teclado. Mejora progresiva reemplaza solo la sección guardada: guarda punto conservando actividad/palabras; guarda palabras conservando actividad; guarda datos conservando entrega. Versión/CSRF y errores mantienen borradores. Importación V14 explícita y reducción recuperable, sin ocultar listas antiguas al crear la primera fila. |
| R02 límites |3puntos,10actividades,100palabras únicas; servidor/importadores bajo bloqueo de evento, incluida concurrencia. Inactivos ocupan cupo; archivo explícito conserva historia. Excesos anteriores no se truncan y no pueden crecer. |
| R03 gustos |264intereses con claves estables y8IDs históricos,43grupos de presentación importados idempotentemente. Navegar no selecciona ancestros; Lady Gaga se guarda sola. Búsqueda/volver conservan selección. General mantiene jerarquía/asociaciones, con revisión, prevención de ciclos y acceso servidor; admin de evento no la cambia. Un mismo ID puede aparecer en varios grupos. |
| R04 desafío | Código individual100bits y QR256bits, solo hashes en MySQL,10min, emisor/destinatario/encuentro/propósito, renovación y consumo. Rechaza código propio/compartido/ajeno/caducado. Verificación cruzada real por QR+manual, sin secretos ajenos en HTML/polling. |
| R05 entrada QR | Mismo lector por propósito, QR real del evento leído a360px, envío explícito y contexto conservado hasta login. Decoder local fijado/lazy/SRI; no abre URLs externas ni sube fotogramas. |
| R06 cuentas | Registro y verificación por SMTP loopback reales, login y recuperación; reset por correo recibido, sesión previa401, replay rechazado y nueva entrada con saldo17/recibo conservados. Adaptadores Google/Facebook reales configurables, sin simular éxito ni mostrar botones sin configuración. Clientes/consentimiento real pendientes del propietario. |
| R07 roles | Cuenta/participación por evento, dueño propio, admin asignado y general. Bootstrap privado una vez, asignación/revocación auditada; rutas antiguas y demo cerradas en normal. CSRF, sesión rotada, recuperación y enlace legado con posesión+confirmación. JAR sin fixture; mysql,test no abre administración. |
| R08 parejas | COMPLETED excluido persistentemente en ambos órdenes/mismo evento. Nueva búsqueda por ambas personas y después de reiniciar no repite. Conserva historia y otras exclusiones. |
| R09 llegada | Nuevos ACTIVE política2: ambas llegadas, ambas verificaciones y ambas confirmaciones. POST directo no evita pasos. ACTIVE/Saga política1 anteriores conservan condiciones/importes, sin inventar datos históricos. |
| R10 recibos | Solicitud20 una vez, recibo persistente, código/QR por propósito, renovación sin cargo, otro encargado autorizado entrega de forma atómica. Self/otro evento rechazados, concurrencia/replay controlados; ruta antigua por ID no evita código. Recibo entregado y saldo17 sobreviven reinicio y recuperación. |
| R11 entrega | Nombre/indicaciones predeterminados por evento o específicos de recompensa; preview efectivo, validación y snapshot del recibo. No consume cupos de encuentro ni mueve recibos previos. Plano existente conserva versión y posición de ACTIVE frente a mover/sustituir; recursos anteriores se retienen. |
| R12 avatar | Animal/sombrero/accesorio/fondo,15SVG originales y listas permitidas servidor; editable y representado junto al alias en MiPing/propuesta/encuentro/historia. No fotos, upload, URL externa ni cambio de identidad/saldo. Persistencia tras login/recovery/reinicio. |

## Verificación ejecutada

- `./mvnw -B verify`:259pruebas,0fallos/errores/omitidas y JAR. Ocho pruebas dirigidas posteriores de integración/plantillas/recuperación pasan tras ajustar el panel legado y15de cuentas/integración tras el último ajuste de layout/etiquetas. Los cambios finales de lector/CSS/etiqueta se verificaron en navegador; no se repitió toda la suite Java sin cambios Java de producto.
- JAR final reempaquetado,133recursos comparados byte por byte con las fuentes; licencia META-INF en la raíz del JAR, sin clases/fixture de src/test. Arranque del JAR normal, Hibernate validate y Flyway sin migraciones pendientes. SHA del JAR no se usa como sustituto del SHA remoto del código.
- MySQL8.4.11 exclusivo: V1–V22; actualización V20→V21 fijada explícitamente y arranque actual→V22, rerun0 en base nueva vacía. Conserva ACTIVE parcialmente confirmado37, Saga22 con un paso abonado y recibo/lugar antiguo; recuperación/reintento sin otro crédito. No clean/drop/reset ni datos de demo tocados.
- Dos cuentas con correo verificado: entrada→alias/avatar→Lady Gaga explícita→disponibilidad→propuesta/aceptación→plano original→mover/sustituir sin trasladar ACTIVE→dos llegadas→QR+manual cruzados→confirmaciones37/37→recibo20→renovar→encargado entrega por QR→17/37→sin repetir en ambos órdenes.0erroresJS en esa pasada.
- En el lector JS fijado se reprodujo un QR que falla por muestreo de alineación en orientación0, incluso desde PNG; Java lo lee y el mismo JS lo lee girado90/180/270. Corrección: una orientación por fotograma sobre canvas local acotado a960px, con borrado al detener; no se modifica la dependencia ni se retienen/suben imágenes. Después leen EVENT_JOIN, ENCOUNTER y REDEMPTION con el decoder de producción y cámara simulada de Chromium. Pide cámara al tocar y detiene tracks. Permiso denegado recuperable, resultado nativo de propósito erróneo simulado rechazado sin navegar, cancelación y origen HTTP no seguro comprobados.
- Colocación táctil real en emulación y alternativa numérica; marcador alineado a0.6/0.4 al ampliar y redimensionar a360; quitar marcador conserva el punto y sus indicaciones. Falta de ubicación explícita sin aproximación inventada.
- Reinicio real al JAR con la misma base: ambas cuentas recuperan avatar,17/37, exclusión de pareja, PNG21614bytes y recibo entregado sin otro cargo. SHA256 del recurso de plano leído por ambas sesiones: `d6522f9b194601958810edb74284d3df26872cc5d5ecf79737f82ee59f04fbb4`.

Los fallos de harness se conservaron fuera de Git: selectores Thymeleaf/URL con query, ancla de marcador de tamaño cero frente a su distintivo visible, y click automático en contexto sin JS (comprobado después con teclado nativo). La intermitencia QR se aisló y corrigió como fallo real del detector, no se dio por resuelta solo cambiando el harness. No se publican logs, cookies, correos, códigos ni bases.

## Límites y puesta en marcha

Navegador Chromium táctil/cámara de vídeo simulados: no certifica teléfono físico, Safari ni cámara real. HTTPS confiado por el teléfono es necesario para cámara; HTTP por IP LAN conserva códigos manuales. SMTP de ensayo no entrega correo a Internet. Google/Facebook requieren clientes, secretos privados, redirects y consentimiento del propietario; no hubo flujo real con esos proveedores. Código/QR entre cuentas dificulta el clic unilateral, pero no prueba presencia física ni impide colusión.

[Windows, single-branch, Wi-Fi y recorrido](WORK_PRUEBA_WINDOWS.md). [Configuración de cuentas/SMTP/proveedores](ACCOUNTS_SETUP.md). GitHub respalda código; **cuentas, datos y planos viven en MySQL y necesitan su propio backup**, incluidos blobs, versiones y coordenadas. No requiere almacenamiento externo ni cuentas nuevas de mapas. No se modificó main ni se hizo merge/force-push.
