# Beneficios · Festival Social

Alcance visual autorizado por Samuel en DEC-034 (Issue #14), sin atribuir aprobación a Julián. Rama `feature/beneficios-mobile` desde `6110e484bf590008795a882198b8a6a53da7b7be`, que contiene Hub, ACTIVE y Mi Ping (PR #26). Entrega autorizada mediante commit/push y PR en borrador hacia `feature/mi-ping-mobile`; sin merge, force-push ni main.

## Implementación

- `/me/benefits`: cabecera compacta y acceso nativo temprano a Mis canjes con su recuento real. Cupón con saldo, coste y faltantes contiguos, acción y condiciones antes de la promoción. Flujo móvil primero; en escritorio Mis canjes aprovecha el lateral.
- Cupón amarillo, Manrope y variables de tema existentes. La pequeña perforación es geometría CSS original de esta tarea; no hay nuevas imágenes, fuentes, proveedores ni dependencias. Iconos locales de `static/img/icons.svg`, ya presentes en el repositorio. Sin portada ni animación decorativa.
- Coste y saldo vienen del modelo real; faltantes = máximo entre cero y coste menos saldo. El formulario conserva endpoint, CSRF, benefitId y requestKey. Saldo insuficiente y Saga pendiente bloquean la solicitud en la vista y conservan la protección del servicio.
- Mis canjes muestra solicitudes, coste guardado, fecha en Bogotá y estados Solicitado/Entregado/Incidencia. Se conserva el aviso de incidencia sin devolución automática. No se añaden controles de staff ni códigos de retiro.
- Promoción ficticia separada después de Mis canjes, sin formulario de canje ni gasto de puntos. Reutiliza el fragmento y desbloqueo originales, incluidos PING-GAMER y avisos de simulación, ausencia de descuento/vínculo comercial. Catálogo vacío conserva saldo y acceso al historial sin inventar disponibilidad futura.
- Solo plantilla y CSS de Beneficios. Backend, reglas, datos/migraciones, permisos, claves/idempotencia, admin de entrega, Hub, ACTIVE, Mi Ping y navegación compartida intactos.

## Capturas y revisión móvil

1. [Móvil390 — MySQL real, saldo existente](screenshots/benefits-mobile.png).
2. [Mis canjes móvil — datos aislados de prueba](screenshots/benefits-claims-mobile.png): los tres resultados se obtuvieron con servicios reales sobre H2 aislado; no son solicitudes de la demo MySQL.
3. [Escritorio1440 — MySQL real](screenshots/benefits-desktop.png).

La acción y la condición se entienden: Solicitar canje se habilita solo con saldo y sin Saga pendiente; cuando faltan puntos se ofrece Seguir conectando. Saldo/coste/faltantes ya no están separados por la portada. No hay decoración que desplace acciones; Mis canjes tiene acceso antes del catálogo. Se comprobó foco, ancla nativa y contenido final alcanzable sobre la navegación inferior, conservando su área segura. Amarillo/morado/negro y Manrope mantienen identidad Festival Social. El tema claro conserva contraste y superficies propias.

## Verificación actual y proporcional

- Evidencia fuera del checkout: `/workspace/work/ping-beneficios-20261016/` (0700). `browser-verification.json`: **9 vistas dirigidas**, sin matriz exhaustiva. Principal390 oscuro; 360 con alias largo/evento genérico;1440; claro para contraste/controles y solicitudes. Estados: saldos0/10/20, Saga pendiente y los tres resultados de entrega; promoción bloqueada/desbloqueada.
- Formularios reales en Spring/H2 aislado: doble clic → un POST; repetición de requestKey → una solicitud/un descuento; entrega repetida → ningún débito adicional. Saga pendiente rechaza el POST forzado y conserva saldo. Sin errores JS, overflow ni controles finales inaccesibles.
- Dos preguntas adicionales (`catalog-cost30.json`, `catalog-empty.json`): el catálogo cambia a30 sin alterar el coste20 de solicitudes anteriores; vacío conserva saldo10 y no ofrece solicitudes nuevas. Cambios solo en los fixtures H2, no en datos MySQL ni código del producto.
- MySQL: `mysql-verification.json`, móvil390 y escritorio1440 en lectura. Firma de estado, saldo y número de canjes iguales a la captura previa. No se hicieron canjes, confirmaciones ni ediciones de esa demo. Solo se recargó PING para la plantilla en caché; MySQL no se reinició.
- Java21/Maven Wrapper: `./mvnw -B -o -s /workspace/.ping-tools/settings.xml -Dmaven.repo.local=/workspace/.ping-tools/m2 -Dtest=RewardTests,EventExperienceTests verify`, con JAVA_HOME/MAVEN_USER_HOME existentes. **40 tests (12+28), cero fallos/errores/omitidos, BUILD SUCCESS y JAR generado**. No se repitió la suite de Mi Ping ni se presentan sus resultados como pruebas nuevas de Beneficios. Sin tests nuevos de clases CSS o estructura HTML.
- `git diff --check`, diff acotado y revisión de secretos/archivos de entrega antes del respaldo. `source-sha256.json` registra las fuentes verificadas.

Skills aplicadas mediante lectura manual: ping-efficient-work (reutilización y alcance), Redesign (jerarquía/cupón), Impeccable (modo operativo y revisión de capturas), verification-before-completion (evidencia actual). No fue necesaria una consulta UX ni depuración. Hooks de Impeccable siguen desactivados; no hubo instalaciones ni subagentes. Su detector mecánico dejó dos avisos: jerarquía plana por no resolver CSS externo (render real h1 28, h2 24/28, cuerpo14/16) y borde lateral antiguo en brand.css:1076 de otra pantalla. No se modificaron zonas ajenas para silenciarlos; esto no equivale a una auditoría automática completa.

Pendiente: revisión humana de las PR en borrador; Safari/dispositivos físicos y zoom nativo no ejecutados. No se declara certificación de accesibilidad. El respaldo de la tarea antigua sigue sin recuperarse.
