# Alta propia del participante — DEC-038

**Continuidad:** DEC-040 sustituye la selección inicial por código/QR → evento identificado → alias → intereses. El alta transaccional y protecciones descritas aquí se conservan. Ver [Conectar/código/QR](CONECTAR_CODIGO_QR.md) para el recorrido vigente.

Instrucción explícita de Samuel (16 de octubre2026, Bogotá), sin atribuir aprobación a Julián. Issue14 permanece abierto. Base limpia local/remota `ea04d3def9440c9319f8ade8b0fa765dba3b114d`; rama propia `feature/participant-onboarding`, PR en borrador hacia `feature/participant-mobile-entry` (PR30). Conserva PR24–30 y sus bases. Sustituye el pendiente anterior; no queda autorización pendiente para el enlace mínimo.

## Recorrido implementado

Home/entrada normal → `/join` (alias y evento abierto) → `/join/interests` → Mi Ping con alias/evento/intereses reales,0 puntos y no disponible. La persona activa disponibilidad y busca con los formularios existentes; si no hay candidatos se explica y puede buscar más tarde. No se crea una coincidencia ni se activa disponibilidad silenciosamente.

La demo académica tiene acceso separado `/demo`, desde donde siguen disponibles su selector y preparación. El alta normal no la prepara ni exige elegir perfiles predefinidos o visitar administración. Si no hay eventos abiertos se informa; corresponde al organizador abrir uno mediante las capacidades existentes.

## Tres fricciones corregidas

1. Entrada desde Home/nav y `/me` sin sesión lleva al alta propia. La demo conserva acceso explícito aparte; en modo participante se ocultan enlaces de preparación/admin del menú y se cambia la etiqueta de salida. Es presentación, sin permisos nuevos ni protección de rutas atribuida a esa ocultación.
2. Alias/evento/intereses se conservan al continuar/volver con POST. Se pueden corregir antes de crear; validaciones y eventos cerrados devuelven mensajes y mantienen borrador. Se crean participante e intereses al final en una única transacción, evitando perfiles parciales.
3. Editor de intereses propio retorna a Mi Ping, evitando otra ficha/selector de demo. Formularios administrativos y edición ajena existente conservan sus destinos y controles. Las dos pantallas nuevas reutilizan Festival Social y opciones táctiles, sin portada ni instrucciones técnicas.

## Sesión, versión y reintentos

- Borrador, revisión y recibo pertenecen a la sesión del servidor. Cada POST conserva CSRF y revisión; rechaza claves ajenas y cambios obsoletos sin sobrescribir datos guardados. No recibe un ID para autorizar adopción.
- Creación delegada a ParticipantService e InterestService, bajo transacción READ_COMMITTED; disponibilidad forzada a false en esta entrada y versión real del nuevo participante obtenida por servidor al guardar intereses. Sin cambiar reglas existentes, tablas ni migraciones.
- Finalización serializada por sesión. Solo después del commit se guarda el ID devuelto y se vincula a la sesión existente. Un POST repetido usa el recibo y vuelve a Mi Ping sin crear/descontar/activar nada. La salida borra el borrador/recibo; cambiar al selector demo también lo descarta.
- Borrador y recibo tienen alcance de sesión, no recuperación durable tras caducidad/reinicio. No se añadieron cuentas ni autenticación nueva; se conserva el modelo académico existente.

## Evidencia actual y proporcional

Aplicación Spring/Thymeleaf real con H2 efímera aislada8082. Dos eventos de prueba creados mediante EventService fuera del recorrido; ningún participante demo predefinido elegido. MySQL8080 siguió funcionando, sin escrituras ni reinicios de esta tarea.

Navegador390: Home→entrada; error de alias vacío con evento conservado; alias/intereses; regreso para corregir alias y cambiar evento con selección conservada; alta→Mi Ping; POST de finalización repetido conserva misma sesión/ID; disponibilidad y búsqueda explícitas sin candidatos; editar intereses vuelve directamente a Mi Ping.360 claro comprueba estrechez de los formularios y1440 una adaptación de intereses. Sin overflow ni errores JS. Menú normal sin preparación/admin; Escape devuelve foco.

Revisión visual: se entienden paso y acción; no hay ilustración/márgenes decorativos desplazándola; controles se leen y las opciones son pulsables. Las dos pantallas de entrada no montan una barra inferior de destinos todavía inaccesibles; Mi Ping conserva su barra y reserva existentes. Identidad Festival Social coherente, ambos temas comprobados. No se repitió auditoría de las cinco pantallas.

Pruebas dirigidas con Java21/Maven Wrapper y caché/settings existentes:

- ParticipantTests7, InterestTests8, DemoImprovementTests13: **28 aprobadas**, preservan administración/demo/edición y reglas existentes.
- ParticipantEntryTests6: **6 aprobadas**, corrección, búsqueda sin candidatos, doble envío concurrente/reintento, CSRF, clave ajena/ID manipulado, revisión obsoleta, cierre de evento recuperable y rollback del alta completa. La prueba de identidad se amplió y se volvió a ejecutar sola para comprobar que incluso una clave propia con ID ajeno crea la entrada propia.
- RecognitionHintTests7: **7 aprobadas**, privacidad de pista y controles existentes. **41 pruebas distintas aprobadas en grupos dirigidos**, no una ejecución histórica ni la suite completa.
- Comandos: `-Dtest=ParticipantEntryTests,ParticipantTests,InterestTests,DemoImprovementTests verify`; tras corregir en el test la comparación de redirecciones absolutas, repetir solo ParticipantEntryTests. Después, por ampliación de casos/etiquetas, `-Dtest=ParticipantEntryTests,RecognitionHintTests verify` y el caso de identidad ampliado individual. Los grupos válidos ajenos no se repitieron. Los comandos finales dieron BUILD SUCCESS/JAR.
- La primera prueba de navegador asumía un orden fijo del selector y se corrigió para usar el nombre real de la opción. La segunda alta recorrió todo; el contador pasó de1 a2 por esa alta intencionada y permaneció2 tras el reintento. En test concurrente aislado, dos solicitudes crean exactamente1 participante.
- Detector Impeccable una vez: cinco avisos sobre `th:src`/CSS externo de plantillas compartidas y borde lateral antiguo. Imágenes reales locales cargadas y jerarquía comprobada en capturas; no se declara detector limpio ni auditoría formal. Hooks desactivados. Skills manuales: ping-efficient-work, Redesign/Impeccable para composición operativa, Debugging para fallos concretos del harness, Verification para evidencia; sin instalaciones/subagentes ni consulta UX innecesaria.

## Capturas reales y continuidad

[Entrada390](screenshots/participant-onboarding/01-entry-mobile.png) · [Error recuperable](screenshots/participant-onboarding/02-recoverable-error.png) · [Intereses390](screenshots/participant-onboarding/03-interests-mobile.png) · [Mi Ping nuevo](screenshots/participant-onboarding/04-mi-ping-mobile.png) · [Búsqueda sin candidatos](screenshots/participant-onboarding/05-waiting-mobile.png) · [Escritorio](screenshots/participant-onboarding/06-interests-desktop.png).

No hay arte/dependencias nuevos: Manrope, iconos y componentes locales existentes; solo capturas de alias/eventos ficticios de pruebas. Logs/harness/JSON/cookies se mantienen fuera del repositorio en `/workspace/work/ping-entry-complete-20261016/` (0700); nunca versionar private-session.json. SHA remoto y PR se verifican al publicar. Sin merge, main o force-push.

Pendiente revisión humana de PR y comprobación en dispositivos físicos/Safari; no se afirma terminada toda PING ni accesibilidad certificada. La instancia8082 es efímera; no usarla como respaldo. MySQL y datos demo se conservaron. El ZIP de la tarea antigua no fue recuperado.
