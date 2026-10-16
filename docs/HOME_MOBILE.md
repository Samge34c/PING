# Home · Festival Social

Implementación directa autorizada por Samuel en DEC-035 (Issue #14), sin atribuir aprobación a Julián. Base `6711b216d463300000a72a062f3da79a6d0b8d28`, que incluye Hub, ACTIVE, Mi Ping y Beneficios. Rama `feature/home-mobile`; respaldo y PR en borrador hacia `feature/beneficios-mobile`, sin merge, force-push ni main.

## Qué cambió

- Mensaje explícito: PING conecta personas por intereses para encontrarse en el evento. Contexto SOFA como demo académica y aviso sin vínculo oficial, visibles en la entrada.
- A390 px, acción antes del arte. Sin sesión: Entrar a la demo → `/demo`, Explorar eventos → `/events`. Con sesión: Volver a Mi Ping → `/me`, Ir a mi evento → `/me/event`, que resuelve el evento real sin un ID fijado a SOFA. Selección de participante y exploración siguen accesibles con sesión.
- La escena social de convención sustituye al gráfico abstracto de iniciales. Es el WebP local existente del Hub, reutilizado sin modificar ni regenerar (SHA/procedencia en HOME_ART_PROVENANCE.json). Ambas personas y su saludo siguen visibles en el recorte compacto. Dimensiones1536×1024 reservadas, sin lazy loading, prioridad alta;319502 bytes ya optimizados y compartidos, sin descarga externa.
- Tres pasos con iconos locales explican intereses/disponibilidad, aceptación bilateral y encuentro/palabra/actividad/confirmación. Sin tarjetas repetidas ni cifras de actividad. +10 puntos por persona solo tras completar y confirmar ambos.
- Preparar demo permanece como acceso GET al formulario existente, separado del inicio como participante. Home nunca ejecuta la preparación; el POST y su CSRF existentes no cambian.
- Escritorio aprovecha el lateral para el arte. CSS acotado a `.home-festival-page` y `.hm-*`, Manrope y variables existentes de tema. Sin Java, JS, dependencias, reglas, permisos, datos/migraciones, navegación compartida ni cambios de otras pantallas.

## Capturas reales

1. [Home móvil390, MySQL](screenshots/home-mobile.png).
2. [Home escritorio1440, MySQL](screenshots/home-desktop.png).
3. [Home móvil con sesión, MySQL](screenshots/home-session-mobile.png).

El arte representa una escena ilustrada, no asistentes ni actividad real. Las capturas corresponden a Spring/Thymeleaf funcionando, no a un prototipo aislado.

## Revisión móvil y verificación

La pantalla y su entrada se entienden sin leer una portada extensa: el botón principal quedó alrededor de y288 a390 px, antes del arte (y433). No hay huecos decorativos ni texto superpuesto; a360 el título crece sin reducir la fuente y la acción sigue antes del arte y de la barra inferior. Ambos rostros/saludo conservan foco. El contenido final, preparación y foco siguen alcanzables sobre la navegación inferior y su área segura. La escena, Manrope, negro/morado/amarillo mantienen Festival Social; claro usa el acento morado para conservar legibilidad.

- `browser-verification.json`: **5 vistas dirigidas** con Spring/H2 real aislado:390 oscuro principal,360 estrechez,390 claro,1440 escritorio y390 con sesión. Accesos reales desde instancia vacía, preparación explícita mediante formulario POST, selección demo, Mi Ping y Hub propio. Escapar del menú Más devuelve el foco; sin overflow ni errores JS; imagen local realmente cargada (naturalWidth1536), dimensiones y orden acción/arte comprobados.
- `generic-event-verification.json`: una pregunta adicional de navegación. Con sesión de evento genérico, `/me/event` lleva a su detalle y no al Hub SOFA. No se amplió una matriz de estados de matching ajenos al cambio.
- `mysql-verification.json`: invitado móvil/escritorio y sesión en aplicación con MySQL; entrada/evento correctos y firma de estado de la demo conservada. Sin preparación, confirmaciones ni modificaciones de datos de esa demo. Solo se recargó PING por caché de plantilla; MySQL no se reinició.
- Java21/Maven Wrapper: `-Dtest=DemoImprovementTests,FinalMvpTests verify` con settings/caché existentes, **27 tests (13+14), cero fallos/errores/omitidos, BUILD SUCCESS/JAR**. Pruebas pertinentes de entrada/preparación/flujo, sin repetir suites de Beneficios ni añadir tests de clases CSS/texto decorativo/estructura HTML. Los resultados de incrementos anteriores no se presentan como ejecuciones nuevas de Home.
- Diff/check y revisión de secretos/alcance antes de publicar; fuentes de plantilla/CSS iguales a los recursos empaquetados verificados. Evidencia, hashes y logs en `/workspace/work/ping-home-20261016/` (0700), fuera de Git; no versionar sesiones, credenciales, logs o datos MySQL.

Skills aplicadas por lectura manual: ping-efficient-work (continuidad y alcance), Redesign (jerarquía móvil/arte/secuencia), Impeccable (revisión visual, sin flujo formal de propuesta/critique), Verification (evidencia antes de afirmar). No se necesitó consulta UX ni depuración. Hooks desactivados; sin instalaciones ni subagentes. El detector estático dejó tres avisos documentados: imagen sin src porque no interpreta `th:src` (el navegador carga el WebP real), jerarquía plana porque no resuelve CSS externo (h1 36/44, h2 22/24, cuerpo16) y borde lateral antiguo en brand.css:1076 de otra pantalla. No se alteraron zonas ajenas para silenciarlos ni se declara auditoría completa.

Pendiente: revisión humana de la PR en borrador; Safari/dispositivos físicos y zoom nativo no ejecutados. No se declara certificación de accesibilidad. El respaldo de la tarea antigua sigue sin recuperarse.
