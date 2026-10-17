# Contexto compartido para IA — Ping

**Estado:** PREDEV v1.1 más decisiones registradas; MVP y demo SOFA en `feature/final-mvp` (PR #23), pendiente de revisión/merge. **Equipo:** Samuel Riveros y Julián Caicedo. **Objetivo:** MVP universitario funcional y defendible ante un profesor, sin operación comercial compleja.

## Qué es y cómo funciona

Ping conecta a dos personas dentro de un evento. Evento abierto → alias e intereses → disponibilidad → propuesta → aceptación bilateral → punto público + palabra/símbolo temporal + actividad → confirmación bilateral → puntos → feedback opcional → nueva búsqueda si sigue abierto.

La actividad ocurre después del match. No es una plataforma general de actividades grupales.

DEC-030 autoriza «Cartel en vivo» y «Cómo reconocerme»: prenda/accesorio opcional persistido en V12, edición de la sesión demo propia y lectura ajena solo en ACTIVE con ambas aceptaciones. Complementa la palabra; no la reemplaza. Sin pistas precargadas por alias, DTO público ni autenticación definitiva.

## Reglas esenciales

- Matching: mayor cantidad de intereses comunes; empate por mayor tiempo esperando. Respetar bloqueos, rechazos e indisponibilidad. Un solo match activo por persona.
- Match alternativo voluntario: mostrar intereses del candidato y preguntar a ambos; solo se conecta si ambos aceptan. Si no resulta, puede ofrecerse actividad del anfitrión.
- Palabra temporal: tras aceptación, inspirada en interés común o catálogo neutral del evento; reducir colisiones (ejemplo `PIXEL-27`). Sustituye pistas físicas y QR como mecanismo principal.
- Actividades breves de catálogo predefinido; sin IA generativa.
- Confirmación de ambos tras interacción; +10 puntos por conexión/actividad completada y recompensa equivalente para actividad del anfitrión. Saldo visible nunca negativo. Beneficio/canje básico opcional (snack de 20 puntos); entrega `ENTREGADO`, `NO_ENTREGADO`, `INCIDENCIA`.
- Feedback opcional 👍/😐/👎, sin puntos.
- Evento: `BORRADOR`, `ABIERTO`, `PAUSADO`, `CERRADO`. Pausa/cierre impiden nuevos matches; activos terminan controladamente.
- Match: `ESPERANDO`, `OFRECIDO`, `ACTIVO`, `COMPLETADO`, `CANCELADO`.

## Alcance

Dentro: eventos, participantes/alias, intereses, disponibilidad, matching, aceptación bilateral, alternativa voluntaria, punto/palabra, actividades, confirmación, puntos, feedback, cancelar, bloqueo/reporte básico, administración y estadísticas. Canje opcional para demo.

Fuera: GPS continuo, Bluetooth, pistas físicas, menores en el escenario inicial, chat, fotos, perfiles sociales persistentes, grupos de 3+, IA/ML para matching, pagos, sponsors, Tiked real, Tiks, app nativa, microservicios, RabbitMQ, Kubernetes y Redis obligatorio.

## Tecnología y autoridad

Java + Spring Boot; MVC/Thymeleaf/Bootstrap/JS básico; JPA/Hibernate; MySQL o MariaDB; Maven/IntelliJ/Git/GitHub. Monolito modular web, una aplicación y una base de datos. DEC-015 fija Java 21, Spring Boot 4.1.1, Maven 3.9.16/Wrapper 3.3.4 y MySQL 8.4.11 LTS para el bootstrap. Perfil técnico sin SQL: `bootstrap`; perfil predeterminado: `mysql`. `feature/mvp-demo`, basada en `feature/intereses`, implementa eventos, participantes, intereses y el flujo bilateral hasta puntos/feedback. DEC-019 registra la autorización del usuario para Issues #5–#11 y las reglas de demo. PR #20 sin merge. DEC-020 autoriza panel académico, escenario manual idempotente, guía/actualización de sesión y propuestas aisladas de paletas. DEC-021 fija B oscuro predeterminado y D claro oficial, con selector en navbar y preferencia local persistente; A/C retirados. Ejecución en DEMO_INTELLIJ.md.

Fuente funcional principal: [PING_PREDEV_v1.1.md](PING_PREDEV_v1.1.md). Leer también [DECISION_LOG.md](DECISION_LOG.md), [AGENTS.md](../AGENTS.md), Issue y código relacionado. GitHub es la memoria compartida; los chats no.

**Si una petición contradice PING_PREDEV_v1.1.md, detente y solicita decisión humana.**

No inventar comportamiento pendiente. Los cambios deben registrarse, aceptarse por Samuel y Julián y documentarse antes de modificar código. Los pendientes están en DECISION_LOG.

En `feature/saga`, DEC-022 implementa finalización con orquestación local durable, transacciones independientes y compensación; base `feature/mvp-demo`, PR separada. Ver SAGA.md para pasos, límites de proceso único, pruebas y CAP.

`feature/final-mvp` (DEC-023, Issues #12–#14) añade admin detallado, bloqueo simétrico en futuras propuestas, reporte sin sanciones y claridad del historial/flujo. Base temporal `feature/saga`, PR separada sin merge. DEC-024 incorpora canje DEMO de snack de 20 puntos, con clave idempotente y entrega/incidencia manual; no permite gastar durante finalización Saga pendiente.

En `reconstruction/festival-social`, DEC-031 registra la instrucción explícita de Samuel (2026-10-16) para integrar exclusivamente el Hub Festival Social aprobado, asociado al Issue #14. No se atribuye aprobación a Julián. Otras pantallas, reglas, permisos, migraciones y datos quedan fuera del cambio; el respaldo anterior sigue sin recuperarse.

DEC-032 registra la instrucción explícita de Samuel para reorganizar ACTIVE como misión Festival Social, en `feature/active-festival-social` desde `55bb831`, con PR separada hacia `reconstruction/festival-social`. PR #24/Hub no se modifica. Sin atribuir aprobación a Julián; backend, reglas, privacidad, persistencia y otras pantallas se conservan. Evidencia y continuidad: ACTIVE_FESTIVAL_SOCIAL.md.

DEC-033/Mi Ping quedó respaldado en `6110e48`, PR #26 hacia ACTIVE. DEC-034 autoriza ahora Beneficios móvil en `feature/beneficios-mobile` desde esa base y PR en borrador hacia Mi Ping: cupón con saldo/coste/faltantes juntos, Mis canjes temprano y promoción ficticia separada. Backend y reglas intactos; ver BENEFICIOS_MOBILE.md al entregar.

DEC-035 autoriza Home móvil desde `6711b21` (Beneficios/PR #27) en `feature/home-mobile`, con PR en borrador hacia Beneficios. Entrada funcional directa, arte social existente, demo explícitamente académica y navegación propia por sesión; sin cambiar backend ni pantallas terminadas. Ver HOME_MOBILE.md y HOME_ART_PROVENANCE.json.

DEC-036 cierra la revisión integrada a390 px desde Home (`4d43491`), con datos aislados y correcciones solo en entrada demo/continuación COMPLETED. Rama `fix/mobile-journey-review`, PR en borrador hacia `feature/home-mobile`; PR #24–#28 conservan sus bases. Ver REVISION_MOVIL_INTEGRADA.md para evidencia, resultados y siguiente prioridad; no declara toda la app terminada.

DEC-037: rama `feature/participant-mobile-entry` desde `e8246b1`; prioridad de producto actualizada en AGENTS.md y mejora visual del editor existente de intereses. Alta manual/sesión presenta un corte reproducido: crear/guardar intereses vuelve a ficha, `/me` sigue redirigiendo a demo. La propuesta mínima de borrador entre pasos + alta con servicios actuales + sesión existente fue consultada a Samuel y está pendiente; no implementarla sin decisión. Ver ENTRADA_PARTICIPANTE_MOVIL.md. No hay cuentas, autenticación, tablas ni reglas nuevas.

DEC-038 sustituye el bloqueo de DEC-037 por autorización explícita de Samuel: `feature/participant-onboarding` desde `ea04d3d`, PR en borrador hacia `feature/participant-mobile-entry`. Entrada `/join` normal → borrador corregible → creación/intereses transaccionales con servicios actuales → sesión propia/Mi Ping → disponibilidad/búsqueda explícitas. Recibo de sesión evita duplicación concurrente; ningún ID de participante del navegador autoriza adopción. Demo/admin y reglas sin cambios. Evidencia y límites en ALTA_PARTICIPANTE.md; ya no hay aprobación pendiente para este enlace.

DEC-039: dos altas normales verificadas de extremo a extremo desde `6afac1e`; polling descubre propuesta sin recarga y la recompensa bilateral/idempotencia funcionan. Único defecto reproducido: resultado de primera confirmación fuera de vista; `fix/participant-confirmation-feedback` dirige el POST al encabezado enfocable de confirmación/resultado, sin alterar reglas/composición. PR en borrador hacia `feature/participant-onboarding`; evidencia actual y límites en CONEXION_DOS_ALTAS.md. MySQL/demo conservados; ensayos efímeros aislados, no respaldo.

DEC-040/Issue #33: Samuel autoriza Conectar propio y alta evento→alias→intereses por código/QR local, con persistencia/migración mínimas. Base `8df0b7d`, rama `feature/connect-event-entry` y PR en borrador hacia `fix/participant-confirmation-feedback`. No atribuir aprobación a Julián; conservar reglas/demo y excluir evoluciones posteriores. Continuidad: CONECTAR_CODIGO_QR.md.
