# Entrada del participante — DEC-037, avance visual y bloqueo de continuidad

Instrucción de Samuel, 2026-10-16, sin atribuir aprobación a Julián. Prioridad general registrada únicamente en AGENTS.md. Issue #14 consultado abierto; no se cierra. Base local/remota `e8246b1f47d67ab38de7caabb8ff6b7c0a4583c6`, `fix/mobile-journey-review`, árbol limpio al comenzar; rama propia `feature/participant-mobile-entry`. No se alteran las PR24–29 ni sus bases.

## Qué funciona actualmente y dónde se corta

Se recorrió el alta existente en PING/Spring/Thymeleaf real, instancia H2 aislada8082. MySQL8080 siguió funcionando, sin reinicio ni escrituras de esta tarea. La instancia de revisión anterior8082 estaba cerrada; se reutilizó su launcher, sin instalaciones. Un contexto separado preparó únicamente el evento de prueba; el participante nuevo nunca eligió una persona predefinida.

- GET `/participants/new`: alias, evento abierto y disponibilidad.
- POST `/participants`: crea alias nuevo **Visitante sin demo**, devuelve `/participants/5`, ficha de gestión.
- GET/POST `/participants/5/interests`: guarda selección válida, vuelve a la misma ficha.
- GET `/me` desde esa sesión: redirige a `/demo`, donde hay que seleccionar participante. El alta no establece la sesión utilizada por Mi Ping. La ficha enlaza ese selector.
- No existe edición de alias/evento después de crear: volver a crear para corregir puede duplicar participantes. Home, antes de este incremento, dirige al selector demo.

**Bloqueo:** CSS o enlaces de presentación no pueden vincular de forma fiable el alta a la sesión ni ofrecer corrección sin duplicar registros. El recorrido alta → intereses → Mi Ping independiente de perfiles demo **no está implementado** en este avance.

## Propuesta mínima pendiente de decisión

Se solicitó decisión explícita mediante pregunta en la sesión, atendiendo la instrucción de proponer el mínimo antes de ampliar el flujo:

1. Borrador de alias/evento/intereses entre dos pasos, con regreso para corregir y conservación de lo introducido.
2. Al finalizar, crear y guardar intereses mediante los servicios actuales; vincular al participante recién creado a la sesión que ya usa Mi Ping.
3. Continuar a Mi Ping con disponibilidad y siguiente acción reales, manteniendo la demo separada.

Sin cuentas, contraseñas, nuevas tablas, permisos o reglas de matching. Antes de implementar ese enlace se necesita la decisión pendiente de Samuel. No se considera la instalación de una skill ni una aprobación atribuida a Julián como autorización. No se ha escrito código Java del enlace ni cambiado contratos/rutas/controladores existentes.

## Trabajo independiente realizado

- Editor existente de intereses: encabezado compacto con alias/evento reales; opciones de dos columnas, cada label completamente pulsable, iconos locales y nombres del catálogo. Se retiraron descripciones genéricas que repetían información y alargaban la selección.
- Estado seleccionado visible mediante check, borde y superficie; sigue siendo checkbox nativo para teclado/tecnologías de asistencia. Sin mínimo de intereses inventado: la selección vacía continúa permitida por las reglas existentes.
- Retorno arriba y un botón principal Guardar intereses; mismos destinos, parámetros, CSRF y versión. El contador ya distingue «1 interés seleccionado» de plural.
- El ajuste visual no inicia sesión, cambia disponibilidad ni inventa una búsqueda. Home, Mi Ping, Hub, ACTIVE, Beneficios y administración no se rediseñaron.
- Ningún recurso artístico o dependencia nuevo. Manrope/iconos/variables de tema existentes; CSS acotado a la página del editor.

## Evidencia y límites

Capturas actuales de la aplicación real, no un prototipo:

- [390 oscuro, intereses](screenshots/participant-entry/interests-mobile.png).
- [360 claro, estrechez/tema alternativo](screenshots/participant-entry/interests-narrow-light.png).
- [1440 oscuro, adaptación](screenshots/participant-entry/interests-desktop.png).

Revisión390: título y acción claros; opciones táctiles, sin instrucciones por cada interés ni portada. Se corrigió el recorte inicial de la parte inferior del botón reduciendo altura interna de opciones sin bajar los objetivos táctiles. La acción queda completa sobre la barra inferior; a360 sigue alcanzable al desplazar. Festival Social se conserva con negro/morado/amarillo. No se declara revisada visualmente la totalidad de la aplicación.

- Java21/Maven Wrapper: `-Dtest=InterestTests verify` con settings/caché existentes, **8 pruebas,0 fallos/errores/omitidos, BUILD SUCCESS**. Incluyen guardado real, selección vacía, IDs inválidos, versión obsoleta y CSRF. Después solo se ajustaron altura/padding CSS y singular/plural del contador, verificados en navegador; no se repitió Java por esos cambios de presentación.
- Navegador:390 oscuro,360 claro y1440 oscuro solo en el editor; tap/teclado, contador/selección, overflow y acceso al botón. Sin submit de esos cambios visuales; persistencia cubierta por las8 pruebas actuales. No se repitió la auditoría de las cinco pantallas ni la suite completa histórica.
- Un intento del navegador agotó30 s esperando load. HTTP directo devolvió200 en página/CSS/JS; una reproducción instrumentada cargó en2,33 s, todos los recursos200 y sin solicitudes pendientes. No se reprodujo un fallo de PING ni se cambió código para ocultarlo. La vista8082 cerrada al inicio también se distinguió de un fallo del alta.
- Skills manuales: ping-efficient-work para continuidad/alcance; Redesign para composición del editor con el stack existente; Impeccable para revisión operativa de capturas, context/detector una vez y hooks desactivados; Verification para evidencia; Debugging ante errores concretos de ejecución. Sin flujo formal critique, subagentes ni instalaciones. Detector: un aviso de borde lateral antiguo en brand.css:1076, fuera del cambio; no se declara detector limpio.

Logs, scripts, JSON y capturas preliminares permanecen privados fuera de Git en `/workspace/work/ping-onboarding-20261016/` (0700). Las capturas publicadas contienen exclusivamente un alias ficticio de pruebas y catálogo demo. Sin sesiones, CSRF, secretos, datos MySQL o respaldos en el commit.

## Continuidad

Este avance se respalda en su rama y PR en borrador hacia `fix/mobile-journey-review`; no es una entrega funcional completa de onboarding. **Pendiente principal:** decidir el enlace mínimo descrito; después implementarlo y comprobar el recorrido real con alias propios/datos aislados hasta búsqueda/conexión. No añadir autenticación por cuenta propia. La pregunta de autorización sigue pendiente; el tiempo transcurrido no equivale a respuesta.

No se cambian main ni la cadena anterior, no merge ni force-push. El SHA local/remoto y enlace de PR se verifican al publicar. Safari/dispositivos físicos y rutas ajenas no se ensayaron; no hay certificación de accesibilidad. El respaldo de la tarea antigua sigue sin recuperarse.
