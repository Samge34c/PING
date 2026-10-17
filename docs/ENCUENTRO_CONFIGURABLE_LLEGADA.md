# Encuentro configurable y «Ya llegué» — DEC-041 / Issue #35

Segundo avance autorizado explícitamente por el usuario, 16 de octubre de 2026 (Bogotá). No se atribuye aprobación a Julián. Copia `/workspace/PING`, remoto `https://github.com/Samge34c/PING.git`. Al empezar: rama `work`, HEAD `97f9b43`, árbol limpio. Fetch de `feature/connect-event-entry` termina en `c288f778d8d7cdf72ec3abb4306473aa756b352b`; se comprobó que contiene ese commit y no había avances posteriores. Rama propia `feature/event-encounter-arrival`, PR en borrador hacia `feature/connect-event-entry`; conserva toda la cadena anterior. Sin merge, main ni force-push.

## Configurar y usar

En Crear/Editar evento hay tres listas opcionales: **puntos públicos**, **palabras base** y **actividades breves**. Una opción por línea, hasta 20 por lista; límites por opción160/40/500 caracteres. Se recortan espacios exteriores y opciones vacías/duplicadas. Entradas inválidas conservan el formulario y no escriben datos; versión anterior impide sobrescribir una edición concurrente.

Cada lista vacía conserva su catálogo actual de forma independiente: punto predeterminado/catálogo SOFA, palabra por interés o neutral y actividad vigente. No hay generación IA. El servidor selecciona por ID de match y guarda los valores al aceptar ambos. La palabra añade el ID en base36 como sufijo único, igual que antes. No se cambia un encuentro activo al editar el evento. Los puntos personalizados sin correspondencia exacta en el mapa SOFA se muestran como texto; no se inventan coordenadas, mapas ni marcadores.

En encuentros nuevos: aceptar ambos → palabra/punto/actividad → **Ya llegué** de cada persona → realizar actividad → confirmación final bilateral → Saga y +10 por persona. Llegar no da puntos, no confirma la actividad ni completa el match. Una llegada solo cambia la marca propia. Estado ajeno se actualiza con el polling existente, sin publicar palabra/pista/lugar en `/me/state`. Se conserva cancelación y finalización de activos en pausa/cierre; no se permite aceptar nuevas propuestas con evento detenido.

`POST /matches/{id}/arrive` usa participante de sesión y CSRF existentes; no acepta identidad del navegador. Reintentos no duplican nada y se serializan con el bloqueo de evento que ya protege acciones del match. Redirección con foco nativo a `#am-arrival-title`. Confirmación final conserva `#am-confirm-title`, Saga, compensación, recuperación y recompensa actuales.

## Datos anteriores

V14 es aditiva; V1–V13 no se modifican. Añade tres listas vacías a `events` y tres booleanos falsos a `ping_matches`: `arrival_required`, `first_arrived`, `second_arrived`. La activación nueva marca `arrival_required=true`. Los ya activos mantienen `false` y pueden terminar con la confirmación anterior, incluso si tenían una confirmación parcial o Saga pendiente. No se inventan llegadas históricas ni se modifican palabra/punto/actividad, saldos, códigos públicos, versiones o recibos Saga.

Prueba Flyway13→14 en H2 modo MySQL con evento anterior, dos participantes con saldos, un ACTIVE parcialmente confirmado y otro con finalización pendiente/primer crédito registrado; conserva valores y condiciones, y segunda migración aplica0 cambios. Prueba de servicio completa un ACTIVE anterior parcialmente confirmado sin exigir nuevas llegadas. **V14 no se ha ejecutado contra MySQL real en este entorno**; no hay una instancia/caché MySQL previa disponible. H2 no sustituye esa comprobación del motor objetivo. No se tocó ni reinició ninguna base demo.

## Verificación actual

- JDK21 completo aislado, Wrapper3.9.16: `./mvnw -B verify` (settings del proxy y repositorio Maven fuera de Git) → **175 pruebas,0 fallos/errores/omitidas**, BUILD SUCCESS y JAR. Incluye los9 casos nuevos y regresiones de matching, privacidad, alta/código/QR, confirmaciones, Saga, compensaciones, puntos, feedback y beneficios. Los fixtures anteriores añaden la llegada requerida; no se eliminan protecciones.
- Tras corregir únicamente el CSS del alias en llegada: `./mvnw -B -o -Dtest=EncounterConfigurationTests verify` → **8/8**, BUILD SUCCESS/JAR. No se repitió la suite completa tras ese ajuste de presentación.
- Chromium real, aplicación Spring/H2 efímera8083, evento configurado por formulario y dos altas normales independientes por código/intereses. Disponibilidad/búsqueda explícitas, aceptación bilateral, misma palabra/punto, una llegada sin confirmar ni premiar, llegada de ambos descubierta por polling, confirmaciones separadas y COMPLETED. Sin errores JavaScript. No se adoptaron perfiles demo ni se forzó el estado del recorrido.
- Móvil390 principal,360 claro y escritorio1440 oscuro. Revisión manual de capturas: estado/acción comprensibles, palabra y punto reconocibles, botón «Ya llegué» antes de la barra inferior, sin portada ni texto que retrase la acción; navegación conserva espacio final y no bloquea formularios. Alias inicialmente partido por reutilizar una grilla con icono: se corrigió con dos columnas solo en llegada. Festival Social/Manrope/morado/amarillo y tema alternativo conservados. Mapa/Hub anteriores cubiertos por pruebas, sin rediseño.
- `git diff --check` y revisión del diff/archivos nuevos. Skills locales leídas selectivamente: ping-efficient-work, systematic-debugging y verification-before-completion. PRODUCT/DESIGN y referencias visuales existentes consultadas; no se ejecutó el motor Impeccable ni se activaron hooks/subagentes.

Incidencias del entorno/harness, sin atribuirlas a PING: el primer fetch no alcanzó proxy8080; funcionó con acceso de red autorizado. Maven necesitó configuración explícita de proxy y caché fuera de Git. Java instalado anuncia21 pero carece de `lib/ct.sym`; `--release21` falla. Se descargó un JDK21 completo en `/workspace/tools` para compilar, sin bajar la versión del proyecto ni modificar Java del sistema. El primer harness Chromium leyó una URL antes de terminar la navegación; se corrigió la espera y se repitió el ensayo. No se presentan esos intentos como verificaciones exitosas.

Pendientes: MySQL8.4 real para V14, cámara/teléfono físico y Safari. No se habilita exposición pública ni autenticación nueva. Los logs, settings, harness y JDK quedan fuera de Git en `/workspace/work/ping-encounter/` y `/workspace/tools/`; ninguna sesión, token o base se incluye en la entrega.

## Capturas con datos ficticios

Primero móvil: [llegada390](screenshots/event-encounter-arrival/03-arrival-mobile.png), [una llegada](screenshots/event-encounter-arrival/04-one-arrived-mobile.png), [ambos llegaron](screenshots/event-encounter-arrival/05-both-arrived-mobile.png), [360 claro](screenshots/event-encounter-arrival/06-arrival-360-light.png), [primera confirmación](screenshots/event-encounter-arrival/08-first-confirmation-mobile.png), [completado](screenshots/event-encounter-arrival/09-completed-mobile.png). Después [escritorio1440](screenshots/event-encounter-arrival/07-active-desktop.png). [Editor390](screenshots/event-encounter-arrival/01-event-catalogs-mobile.png) y [ACTIVE completo390](screenshots/event-encounter-arrival/02-active-mobile.png).

## Continuidad

En otra copia con árbol limpio: fetch y switch a `feature/event-encounter-arrival`; conservar Java21/MySQL8.4 y variables privadas existentes. No cambiar main ni reiniciar datos. Flyway aplica V14 una vez al iniciar. Ensayar primero en base exclusiva: editar evento → catálogos → dos entradas normales → aceptación → una/dos llegadas → una/dos confirmaciones →10/10. Verificar un ACTIVE anterior sin llegada requerida y un evento SOFA sin listas personalizadas. No ejecutar preparación demo por comodidad ni modificar migraciones antiguas.
