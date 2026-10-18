# Registro de decisiones — Ping

Fuente principal: [PREDEV v1.1](PING_PREDEV_v1.1.md), fechado el **12 de octubre de 2026**, y solicitud de preparación del usuario. Este registro resume decisiones conocidas; no constituye una aprobación nueva ni atribuye una aceptación individual no documentada.

## DEC-033 — Mi Ping operativo con prioridad móvil

- **Autoridad:** instrucción explícita de Samuel, 2026-10-16; no se atribuye aprobación a Julián. Issue #14 consultado abierto. Esta autorización habilita Mi Ping, antes excluido de DEC-031/032.
- **Base y aislamiento:** `d088e592dcccb6ede0b7b1ac97b7a80f6274348f` (incluye Hub, ACTIVE y pauta móvil respaldada), rama propia `feature/mi-ping-mobile`. No mezclar código de Mi Ping en la PR #25 de ACTIVE. Sin merge ni cambios en main; la petición original de commit/push se refería a AGENTS.md, ya respaldado en la rama anterior. Samuel autoriza posteriormente respaldar también Mi Ping y preparar PR en borrador hacia `feature/active-festival-social`, sin merge ni force-push.
- **Alcance:** únicamente Mi Ping `/me`, su plantilla y CSS acotado. Estado real y siguiente acción antes de intereses y pista propia opcional; reducir duplicados, sin portada grande. Conservar historial y destinos, saldo/alias reales, identidad Festival Social y navegación compartida.
- **Conservación:** ningún cambio de backend, permisos, rutas, CSRF, versión, matching, disponibilidad, aceptación/confirmación bilateral, Saga, puntos, persistencia, migraciones ni datos de la demo. La pista ajena y la palabra del encuentro no se trasladan a Mi Ping. Editor propio y actualización automática existentes intactos.
- **Verificación:** seguir la pauta proporcional de AGENTS.md y pruebas pertinentes actuales. Formularios con datos aislados; demo MySQL en lectura. Decisiones implementadas y evidencia en DESIGN.md y docs/MI_PING_MOBILE.md al entregar.

Las DEC-001 a DEC-014 se numeran conforme a la solicitud. La guía GitHub usa una numeración de ejemplo, no un registro alternativo. Para cada cambio futuro registrar ID, título, estado, fecha, decisión, motivo e impacto; documentar aceptación de ambos integrantes antes de cambiar el producto.

## DEC-001 — Conexiones 1 a 1

- **Estado:** ACORDADO EN CONSOLIDACIÓN DEL EQUIPO, según fuente adjunta; sin constancia individual adicional.
- **Fecha:** 2026-10-12 (fecha de la fuente).
- **Decisión:** Ping conecta inicialmente a dos personas dentro de un evento.
- **Motivo:** Facilitar una interacción breve entre dos asistentes.
- **Impacto:** No incluir grupos de tres o más.
- **Fuente:** PREDEV §§2, 12.

## DEC-002 — Afinidad por intereses

- **Estado:** ACORDADO EN CONSOLIDACIÓN DEL EQUIPO, según fuente adjunta; sin constancia individual adicional.
- **Fecha:** 2026-10-12 (fecha de la fuente).
- **Decisión:** Priorizar al candidato con más intereses en común.
- **Motivo:** La compatibilidad guía el encuentro.
- **Impacto:** Calcular afinidad antes de ordenar candidatos.
- **Fuente:** PREDEV §4.

## DEC-003 — Espera como desempate

- **Estado:** ACORDADO EN CONSOLIDACIÓN DEL EQUIPO, según fuente adjunta; sin constancia individual adicional.
- **Fecha:** 2026-10-12 (fecha de la fuente).
- **Decisión:** A igual afinidad, priorizar a quien más tiempo lleve esperando.
- **Motivo:** Dar prioridad de espera sin reemplazar afinidad.
- **Impacto:** Registrar espera y probar empates.
- **Fuente:** PREDEV §4.

## DEC-004 — Match alternativo voluntario

- **Estado:** ACORDADO EN CONSOLIDACIÓN DEL EQUIPO, según fuente adjunta; sin constancia individual adicional.
- **Fecha:** 2026-10-12 (fecha de la fuente).
- **Decisión:** Puede ofrecerse una alternativa mostrando intereses, con aceptación de ambos.
- **Motivo:** Ofrecer conexión sin obligarla cuando falta coincidencia suficiente.
- **Impacto:** Si no se acepta, puede ofrecerse actividad del anfitrión.
- **Fuente:** PREDEV §4.

## DEC-005 — Aceptación bilateral

- **Estado:** ACORDADO EN CONSOLIDACIÓN DEL EQUIPO, según fuente adjunta; sin constancia individual adicional.
- **Fecha:** 2026-10-12 (fecha de la fuente).
- **Decisión:** Ambas personas deben aceptar antes de activar la conexión.
- **Motivo:** La propuesta de conexión es voluntaria.
- **Impacto:** No mostrar encuentro ni activar por una sola aceptación.
- **Fuente:** PREDEV §§3, 7.

## DEC-006 — Punto público y palabra temporal

- **Estado:** ACORDADO EN CONSOLIDACIÓN DEL EQUIPO, según fuente adjunta; sin constancia individual adicional.
- **Fecha:** 2026-10-12 (fecha de la fuente).
- **Decisión:** Usar punto público y palabra/símbolo temporal en lugar de pistas físicas.
- **Motivo:** Reconocer la conexión sin ropa, altura, tatuajes ni GPS.
- **Impacto:** Reducir colisiones y usar catálogo neutral cuando no hay interés común.
- **Fuente:** PREDEV §5.

## DEC-007 — Actividades breves

- **Estado:** ACORDADO EN CONSOLIDACIÓN DEL EQUIPO, según fuente adjunta; sin constancia individual adicional.
- **Fecha:** 2026-10-12 (fecha de la fuente).
- **Decisión:** Después del match, ofrecer actividad breve de catálogo predefinido.
- **Motivo:** Ayudar a romper el hielo.
- **Impacto:** Sin generación IA ni plataforma general de actividades grupales.
- **Fuente:** PREDEV §§2, 6.

## DEC-008 — Recompensa equivalente del anfitrión

- **Estado:** ACORDADO EN CONSOLIDACIÓN DEL EQUIPO, según fuente adjunta; sin constancia individual adicional.
- **Fecha:** 2026-10-12 (fecha de la fuente).
- **Decisión:** La actividad alternativa del anfitrión puede tener recompensa equivalente.
- **Motivo:** No obligar a aceptar un match para acceder a la alternativa recompensada.
- **Impacto:** Precisar su validación antes de implementarla.
- **Fuente:** PREDEV §§4, 8.

## DEC-009 — Feedback opcional

- **Estado:** ACORDADO EN CONSOLIDACIÓN DEL EQUIPO, según fuente adjunta; sin constancia individual adicional.
- **Fecha:** 2026-10-12 (fecha de la fuente).
- **Decisión:** Feedback posterior opcional, sin puntos.
- **Motivo:** Recoger opinión sin convertirla en obligación.
- **Impacto:** Ofrecer útil/agradable, neutral o no me sirvió.
- **Fuente:** PREDEV §9.

## DEC-010 — Independencia de Tiked

- **Estado:** ACORDADO EN CONSOLIDACIÓN DEL EQUIPO, según fuente adjunta; sin constancia individual adicional.
- **Fecha:** 2026-10-12 (fecha de la fuente).
- **Decisión:** Ping funciona independientemente de Tiked en el MVP.
- **Motivo:** Demostrar el proyecto sin integración real con terceros.
- **Impacto:** No implementar Tiked ni Tiks.
- **Fuente:** PREDEV §13.

## DEC-011 — MVP web responsive

- **Estado:** ACORDADO EN CONSOLIDACIÓN DEL EQUIPO, según fuente adjunta; sin constancia individual adicional.
- **Fecha:** 2026-10-12 (fecha de la fuente).
- **Decisión:** El MVP será web responsive.
- **Motivo:** Preparar la interfaz web del proyecto académico.
- **Impacto:** MVC/Thymeleaf/Bootstrap; sin app móvil nativa.
- **Fuente:** PREDEV §§12, 14. El carácter responsive también está explicitado en la solicitud del usuario.

## DEC-012 — Monolito modular

- **Estado:** ACORDADO EN CONSOLIDACIÓN DEL EQUIPO, según fuente adjunta; sin constancia individual adicional.
- **Fecha:** 2026-10-12 (fecha de la fuente).
- **Decisión:** Una aplicación y una base de datos, organizadas en módulos.
- **Motivo:** Mantener el alcance técnico académico aprobado.
- **Impacto:** No introducir microservicios.
- **Fuente:** PREDEV §§14, 15.

## DEC-013 — Java y Spring Boot

- **Estado:** ACORDADO EN CONSOLIDACIÓN DEL EQUIPO, según fuente adjunta; sin constancia individual adicional.
- **Fecha:** 2026-10-12 (fecha de la fuente).
- **Decisión:** Java y Spring Boot son la tecnología principal.
- **Motivo:** Stack aprobado en la consolidación.
- **Impacto:** MVC, Thymeleaf, JPA/Hibernate, SQL y Maven; versiones pendientes.
- **Fuente:** PREDEV §14.

## DEC-014 — Evolución documentada

- **Estado:** ACORDADO EN CONSOLIDACIÓN DEL EQUIPO, según fuente adjunta; sin constancia individual adicional.
- **Fecha:** 2026-10-12 (fecha de la fuente).
- **Decisión:** El MVP puede evolucionar con decisiones aceptadas por Samuel y Julián y documentación previa.
- **Motivo:** Evitar cambios de alcance introducidos por una IA.
- **Impacto:** Actualizar registro y fuentes antes del código afectado.
- **Fuente:** PREDEV §17.

## Pendientes reales: no son decisiones aceptadas

No se detectó contradicción funcional entre la solicitud y PREDEV. La guía proponía asignar Issues; esta tarea pide no hacerlo sin saber quién los realizará, por lo que se dejan sin responsable.

| ID | Tema que requiere definición | Momento |
| --- | --- | --- |
| PEND-001 | RESUELTO por DEC-015: Java/Spring Boot/Maven/Wrapper/MySQL y coordenadas fijados | Fase 0 |
| PEND-002 | Qué constituye coincidencia «suficiente» y cuándo ofrecer match alternativo | Antes de fase 4 |
| PEND-003 | Empate simultáneo de afinidad y espera; duración de propuestas y tratamiento de rechazos | Antes de fases 4–5 |
| PEND-004 | Duración, unicidad y validación de palabra temporal | Antes de fase 6 |
| PEND-005 | Confirmación/validación de actividad del anfitrión y recompensa equivalente | Antes de fases 7–9 |
| PEND-006 | Cierre controlado: plazos, cancelación y posible ventana de canje | Antes de fases 1 y 12 |
| PEND-007 | Detalle de bloqueo/reporte, acceso administrativo e ingreso de participantes | Antes de módulos afectados |
| PEND-008 | Si se incluye canje opcional y cómo manejar reversión interna pendiente sin saldo visible negativo | Antes de fase 9 |
| PEND-009 | Verificar acceso de Julián, visibilidad y protección de main en GitHub | Antes del trabajo colaborativo |

### Observación inicial del entorno (antecedente de la preparación documental)

En la máquina auditada: OpenJDK **21.0.12.1**, Git **2.52.0**; `mvn` no disponible. No existe `pom.xml`, wrapper Maven ni aplicación. La versión instalada no fija compatibilidad del futuro proyecto. Java/Spring Boot/Maven/SQL permanecen **PENDIENTES DE FIJAR AL CREAR EL PROYECTO**. No se requiere instalar una base de datos para esta tarea documental.

## DEC-015 — Bootstrap técnico Spring Boot

- **Estado:** Elección técnica aplicada para PREDEV v1.1 / bootstrap académico, autorizada por la solicitud de fase 0. No atribuye una aprobación individual adicional a Samuel o Julián.
- **Fecha:** 2026-10-12 (fecha local del usuario).
- **Decisión:** Java 21 (línea LTS); Spring Boot 4.1.1 estable; Maven 3.9.16 mediante Wrapper oficial 3.3.4, tipo `only-script`; MySQL 8.4.11 LTS como referencia del servidor. Coordenadas `com.ping:ping:0.0.1-SNAPSHOT`, nombre PING, paquete `com.ping`, empaquetado JAR.
- **Motivo:** Java 21 instalado y compatible con el rango Java 17–26 documentado por Spring Boot 4.1.1; Maven 3.9.16 supera su mínimo 3.6.3. El Wrapper evita instalación global y MySQL concreta la alternativa SQL permitida por PREDEV, según instrucción del usuario.
- **Dependencias:** Web MVC (`spring-boot-starter-webmvc` en Boot 4), Thymeleaf, JPA, Validation, Connector/J en runtime y Boot Test solo para pruebas. Versiones gestionadas por el parent/BOM 4.1.1; Connector/J resuelto a 9.7.0. No se añaden Security, Lombok, bases alternativas ni infraestructura adicional.
- **Impacto:** Perfil `mysql` predeterminado con `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` sin valores versionados, DDL `none`; perfil técnico `bootstrap` excluye autoconfiguración JDBC/JPA para pruebas y arranque web sin SQL. No hay entidades, tablas ni lógica de negocio. El perfil MySQL se verificó contra un servidor temporal 8.4.11; cada desarrollador debe configurar su servidor y variables locales.
- **Integridad:** Distribución Maven contrastada con SHA-512 de Maven Central; se fija su SHA-256 en `maven-wrapper.properties`. Scripts oficiales generados por plugin Wrapper 3.3.4, sin binario wrapper versionado. No se deshabilitan TLS ni verificaciones.

### Fuentes oficiales efectivamente consultadas

Los portales start.spring.io, docs.spring.io, maven.apache.org, openjdk.org y dev.mysql.com devolvieron 403 del proxy. Se usaron sus repositorios oficiales y los artefactos publicados en Maven Central; no tutoriales.

- [Requisitos de Spring Boot 4.1.1 en su fuente oficial](https://github.com/spring-projects/spring-boot/blob/v4.1.1/documentation/spring-boot-docs/src/docs/antora/modules/ROOT/pages/system-requirements.adoc): Java 17–26 y Maven 3.6.3+.
- [Release oficial Boot 4.1.1](https://github.com/spring-projects/spring-boot/releases/tag/v4.1.1) y [BOM publicado](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.1/spring-boot-dependencies-4.1.1.pom).
- [OpenJDK 21 Updates](https://github.com/openjdk/jdk21u/blob/master/README.md): línea oficial de actualizaciones Java 21. El POM fija nivel 21, no un parche específico del proveedor.
- [JDK Eclipse Adoptium 21.0.12.1+1 oficial](https://github.com/adoptium/temurin21-binaries/releases/tag/jdk-21.0.12.1%2B1): JDK completo usado en Codex, descargado con SHA-256 oficial verificado. El runtime Debian inicial carecía de `ct.sym` y no admitía `--release 21`; se sustituyó para las verificaciones sin alterar la versión Java del POM.
- [Apache Maven 3.9.16](https://github.com/apache/maven/tree/maven-3.9.16) y [distribución con checksum oficial](https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/).
- [Apache Maven Wrapper 3.3.4](https://github.com/apache/maven-wrapper/tree/maven-wrapper-3.3.4) y [plugin publicado](https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-wrapper-plugin/3.3.4/).
- [MySQL 8.4.11: versión y madurez LTS en fuente oficial](https://github.com/mysql/mysql-server/blob/mysql-8.4.11/MYSQL_VERSION).
- [Connector/J 9.7.0 oficial](https://github.com/mysql/mysql-connector-j/blob/9.7.0/README), versionado por el BOM Spring Boot. Integración JDBC/JPA verificada contra MySQL 8.4.11 temporal, con TLS `VERIFY_CA`.

### Estado técnico actual

La observación de ausencia de Maven/proyecto anterior se conserva como antecedente. Ahora existe Wrapper y base Spring Boot. PEND-001 queda resuelto por esta elección técnica; PEND-002 a PEND-009 mantienen su carácter pendiente. PREDEV y DEC-001 a DEC-014 no cambian.

### Validación adicional de fase 0

Arranque real del JAR con perfil `mysql` contra MySQL 8.4.11: conexión JDBC establecida, EntityManagerFactory JPA inicializado y respuesta HTTP 404 esperada. Se verificaron cero tablas antes y después del arranque (`ddl-auto=none`). La conexión validó la CA del servidor mediante un almacén temporal; no se desactivó TLS. La imagen oficial usada fue `mysql@sha256:24989179f58856db763f418010c1169fecc944e3ec1ea7e921572d83eb070f70`.

Las credenciales aleatorias de la instancia de prueba no se imprimieron ni versionaron. Contenedor, volumen, procesos y archivos temporales se eliminaron al terminar. Docker fue una herramienta puntual de validación, no una dependencia del MVP. El workflow Bootstrap CI queda preparado para verificar el Wrapper en Ubuntu y Windows; la disponibilidad y resultados de Actions deben comprobarse en GitHub.

## DEC-016 — Base técnica y ciclo de vida de Eventos

- **Estado:** Aplicado por instrucción explícita del usuario para Issue #2; sin atribuir aprobación individual adicional.
- **Fecha:** 2026-10-13.
- **Decisión:** Entidad Event con estados DRAFT/OPEN/PAUSED/CLOSED. Solo DRAFT→OPEN, OPEN→PAUSED, PAUSED→OPEN, OPEN→CLOSED y PAUSED→CLOSED. CLOSED no se reabre. `canStartNewMatches()` solo devuelve true en OPEN.
- **Motivo:** Implementar las reglas definidas para una demo web persistente; no adelantar matching.
- **Impacto:** Migración V1 crea `events`; Flyway 12.4.0 y su módulo MySQL se gestionan por el BOM Boot 4.1.1. Hibernate valida el esquema, sin crear ni borrar tablas. Longitudes técnicas de formulario/SQL: nombre 120 y descripción 2000; control de versión para ediciones concurrentes. POST con token de formulario y escape Thymeleaf; sin autenticación completa.
- **Fuentes técnicas:** [Inicialización de datos oficial Spring Boot 4.1.1](https://github.com/spring-projects/spring-boot/blob/v4.1.1/documentation/spring-boot-docs/src/docs/antora/modules/how-to/pages/data-initialization.adoc) y BOM existente. Starter Flyway y `flyway-mysql` compatibles según esa fuente. H2 se añade solo en alcance `test`, para ejecutar migraciones y pruebas reproducibles sin servidor externo; no es una base productiva.

**PENDIENTE HUMANO — PEND-006:** plazos de cierre controlado, cancelaciones, canje y ventanas temporales siguen sin decidir. No se implementan. La gestión de estado no termina ni altera matches porque todavía no existe matching.

## DEC-017 — Demo mínima de Participantes

- **Estado:** Implementación técnica autorizada por la solicitud del usuario para Issue #3; no cierra PEND-007.
- **Fecha:** 2026-10-13.
- **Decisión:** Alias (límite técnico de 80 caracteres), vínculo con un evento y disponibilidad declarada. El formulario de demo cubre el camino documentado de registro en evento OPEN. Sin cuenta, email, contraseña, identificación física ni autenticación.
- **Motivo:** Mostrar Samuel y Julián dentro de la feria, con persistencia y disponibilidad modificable, sin inventar perfiles sociales.
- **Impacto:** V2 crea `participants` con FK a `events`. La disponibilidad se guarda por POST con token y control de versión; no crea matches ni sustituye `Event.canStartNewMatches()`.

**PENDIENTE HUMANO — PEND-007:** reglas definitivas de ingreso en otros estados, identidad/autenticación, acceso administrativo y bloqueo/reporte. El registro de demo en OPEN delimita el camino implementado, no decide políticas para PAUSED/CLOSED. No se impone unicidad de alias ni se atribuye identidad real a los nombres usados en la demostración.

## DEC-018 — Catálogo técnico de intereses para demostración

- **Estado:** Implementado por autorización explícita del usuario para Issue #4; no decide matching.
- **Fecha:** 2026-10-13.
- **Decisión:** Catálogo de intereses y selección múltiple por participante. V3 crea `interests` y `participant_interests`; V4 inserta los ocho ejemplos DEMO suministrados por el usuario, marcados con `demo=true` y visibles como datos de demostración.
- **Motivo:** Permitir mostrar Samuel y Julián con intereses guardados, sin construir matching ni crear datos que aparenten producción.
- **Impacto:** FK y clave compuesta evitan enlaces inválidos/duplicados. El formulario permite modificar o limpiar la selección; se valida el catálogo completo antes de cambiarla y se rechaza una versión obsoleta. No impone cantidad mínima para matching, afinidad suficiente ni políticas futuras. El límite de 80 caracteres y unicidad del nombre son restricciones técnicas del catálogo, no una lista definitiva del producto.

**PENDIENTE HUMANO:** PEND-002/PEND-003 y revisión de Samuel antes de Issue #5. No se implementa matching, participantes adicionales fuera del alcance definido, puntos ni feedback.

## DEC-019 — Flujo principal de demo autorizado

- **Estado:** Autorizado expresamente por la nueva solicitud del usuario (2026-10-13), sin atribuir aceptación individual adicional.
- **Decisión:** Rama única `feature/mvp-demo`, base `feature/intereses`, Issues #5–#11. Afinidad máxima (mínimo uno), espera e ID ascendente; alternativa con cero coincidencias y aceptación bilateral. Sin timeout. Sesión explícita académica, encuentro/palabra/actividad después de ambas aceptaciones, confirmación bilateral, +10 una sola vez y feedback opcional.
- **Detalle técnico reversible:** Ofertas también reservan a ambos participantes. Escrituras READ_COMMITTED, bloqueo transaccional por evento y reservas con PK por participante evitan ofertas concurrentes. Un rechazo/cancelación excluye esa pareja durante el ciclo actual de disponibilidad; volver a marcar no disponible/disponible inicia otro ciclo. Durante una reserva, primero hay que cancelar antes de cambiar disponibilidad. Completar reinicia la espera. Palabra persistida con sufijo ID base 36 único, válida solo durante ACTIVE; no expira automáticamente. Punto/actividad/intereses se conservan como instantánea. Activos pueden terminar tras pausa/cierre; nuevas ofertas/activaciones requieren OPEN.
- **Impacto:** PEND-002/PEND-003 y palabra de PEND-004 resueltos para esta demo. No se implementan autenticación definitiva, bloqueo/reporte general, actividad del anfitrión premiada, canje ni reglas de plazos de cierre. PR sin merge.

## DEC-020 — Mejoras visibles de demo autorizadas

- **Estado:** Autorizadas por la solicitud actual del usuario, 2026-10-13; sin atribuir aprobaciones individuales.
- **Decisión:** Mantener tema A predeterminado; comparar A/B/C/D únicamente en `/demo/themes`, mediante variables semánticas. Panel académico de métricas por evento; guía del siguiente paso e historial personal; errores de entrada comprensibles; preparación manual y reutilizable de un escenario demo sin borrar ni reiniciar datos. Sin cambios al algoritmo, recompensas, autenticación ni arquitectura. Continuar PR #20 sin merge.

## DEC-021 — Identidad visual oficial oscuro/claro

- **Estado:** Elección expresa del usuario, 2026-10-13, sin atribuir aprobaciones individuales.
- **Decisión:** B es oscuro oficial y predeterminado; D es claro oficial. Selector sencillo y preferencia local `ping-theme=dark|light`; A/C se retiran para simplificar. Variables centralizadas, superficies diferenciadas y logo original sin alterar. Sin cambios de matching ni persistencia SQL. Después se permite una mejora pequeña de UX del panel de demo.

## DEC-022 — Saga local de finalización académica

- **Estado:** Implementación solicitada expresamente por el usuario, 2026-10-13; sin atribuir aceptación individual adicional.
- **Decisión:** Orquestación local con pasos transaccionales independientes, registro durable y compensaciones inversas. Match permanece ACTIVE con finalización reservada hasta completar ambos créditos; solo después pasa a COMPLETED. Ante fallo se revierten los créditos de este intento y la última confirmación, sin borrar puntos previos ni liberar la reserva. Se conserva el monolito y una base; sin broker ni servicios distribuidos. Serialización del coordinador en el único proceso de aplicación, con bloqueo por evento en cada paso. Recuperación de pendientes al iniciar y mediante reintento. PR separada en `feature/saga`, base `feature/mvp-demo`, sin merge.

## DEC-023 — Cierre funcional del MVP académico

- **Estado:** Solicitud explícita del usuario, 2026-10-13; no atribuye aprobaciones individuales adicionales.
- **Decisión:** `feature/final-mvp` basada en Saga, Issues #12–#14: admin detallado, bloqueo direccional con exclusión simétrica de futuras propuestas, reporte sin sanciones automáticas, historial/UX y pruebas integrales. Reportes vinculados a interacciones terminadas/canceladas; comentario máximo 300 caracteres y uno por autor/match para reintentos. No modifica matches anteriores ni finalización Saga. Admin sigue siendo panel de demo sin autenticación definitiva. Canje opcional solo tras estabilizar el resto, sin gasto de créditos durante una Saga pendiente. Sin merge.

## DEC-024 — Canje opcional autorizado y acotado

- **Estado:** Aplicado tras validar admin/bloqueo/reporte/flujo en 91 tests y Chromium/MySQL, por autorización condicional del usuario.
- **Decisión:** Snack DEMO de 20 puntos; solicitud con clave idempotente, costo guardado y descuento transaccional único. REQUESTED → DELIVERED o INCIDENT, sin stock, pagos ni devolución automática. Una incidencia registra revisión de entrega; no vuelve a descontar. El bloqueo por evento y la prohibición de canjear mientras la finalización Saga esté pendiente protegen los créditos compensables.

## DEC-025 — Finalización de entrega

- **Estado:** Autorización explícita del usuario, 2026-10-14; sin atribuir aprobaciones individuales adicionales.
- **Decisión:** Correcciones de errores/recuperación de actualización automática, entrada/navegación de demo, presentación y guía IntelliJ vigente. Misma rama/PR #23; no cambia matching, Saga, recompensas ni esquema. No se reinician datos ni bloqueos de los ensayos.

## DEC-026 — Dirección visual App Social Dark

- **Estado:** Dirección visual explícita del usuario, 2026-10-14.
- **Decisión:** Sidebar desktop, navegación inferior móvil, paleta Work B/D, Manrope local y componentes de ondas/afinidad. Solo templates, CSS, recursos locales y JS de presentación; matching, Saga, puntos, services, repositorios y migraciones intactos. PR #23 sin merge.

## DEC-027 — Correcciones localizadas tras auditoría de entrega

- **Estado:** Autorizadas por la solicitud de auditoría del usuario, 2026-10-14; hallazgos mostrados antes de modificar.
- **Decisión:** Demo en localhost por defecto porque admin/identidad siguen siendo públicos; una sola instancia. Corregir alias visual al consultar perfiles ajenos, encabezado de alternativa y espaciado de misión activa. Sin autenticación nueva, cambios de matching/Saga/puntos/esquema ni merge. Se conservan evidencias anteriores y se valida regresión.

## DEC-028 — Escenario demo académico SOFA 2026

- **Estado:** Autorizado explícitamente por la solicitud del usuario, 2026-10-14; sin atribuir aprobación individual adicional.
- **Decisión:** Preparar SOFA 2026 (simulación académica, Corferias Bogotá, 8–12 de octubre), con intereses indicados para Samuel/Julián/Ana/Carlos y catálogo de encuentro SOFA por afinidad. Al preparar la antigua Feria Universitaria, actualizar el mismo evento y los intereses demo una sola vez; conservar estado, disponibilidad, puntos e historial. Los otros eventos mantienen su catálogo y punto configurado. Sin cambiar matching, Saga, puntos, seguridad, esquema ni dependencias; PR #23, sin merge.

## DEC-029 — Orientación y reconocimiento en la experiencia SOFA

- **Estado:** Segunda pasada autorizada explícitamente por el usuario, 2026-10-14; no atribuye aprobación individual adicional.
- **Decisión:** Añadir portada de evento, guía interna offline por zonas, enlace desde el encuentro y pistas preparadas de ropa/accesorios exclusivamente para participantes demo y solo durante ACTIVE. Las pistas complementan la palabra; no sustituyen su mecanismo ni introducen datos sensibles, GPS o perfiles. Promoción ficticia claramente marcada y badges derivados de conexiones, sin alterar saldos. Composición propia de Ping; fuentes públicas se registran sin atribuir derechos/licencias no comprobados. Sin cambiar matching, Saga, puntos, bloqueos, reportes, migraciones ni dependencias. Commits separados, PR #23, sin merge.

## DEC-030 — Cartel en vivo y pista personal opcional

- **Estado:** Solicitud explícita del usuario y prompt principal adjunto, 2026-10-14; no atribuye aceptación individual adicional.
- **Decisión:** Composición editorial continua en ACTIVE/Hub/Home/Mi Ping, objetos independientes en cards y navegación contextual. Única ampliación funcional: «Cómo reconocerme», pista privada opcional persistente de 120 caracteres Unicode, edición propia por sesión/CSRF/versión y lectura ajena exclusivamente ACTIVE con aceptación bilateral. Sin DTO público, backfill, pistas sensibles requeridas ni cambios de matching/ActivityCatalog/Saga/puntos/canje/seguridad. Migración nueva; V1–V11 intactas. Baseline e037ace: 133 tests aprobados y MySQL/Chromium real. Remoto pendiente de autenticación; no publicar sin verificarlo. Rama feature/final-mvp, PR23, sin merge.

## DEC-031 — Integración de Festival Social exclusivamente en Hub SOFA

- **Estado y autoridad:** instrucción y aprobación explícitas de **Samuel** del 2026-10-16; no se atribuye aprobación a Julián. La autorización actual sustituye la restricción de prototipo aislado para este Hub.
- **Asociación:** [Issue #14 — Preparar demo final](https://github.com/Samge34c/PING/issues/14), consultado mediante el conector (abierto); antecedente PR #23. Esta decisión acota el incremento visual; no declara completado el Issue.
- **Referencia real:** segunda iteración local `/workspace/work/sofa-hub-propuesta/`, HTML/CSS, ilustración con procedencia y capturas desktop/mobile-iteration2. No representa recuperación del respaldo anterior.
- **Alcance:** Hub, recursos locales necesarios, variante de navegación exclusiva, documentación y pruebas de sus estados. Portada con dos personas en convención; negro/morado/amarillo, zonas diferenciadas, siguiente acción antes de portada, nombre de la primera zona temprano en móvil.
- **Conservación:** sin cambios a reglas/permisos/migraciones/datos; demo, matching, Saga, aceptación y confirmación bilaterales, palabra/pista opcional, actividades, puntos, beneficios y mapa intactos. OFFERED presenta propuesta; ACTIVE destaca la zona derivada del punto por identificador, no pabellón. Nunca exponer la pista ajena en Hub.
- **Verificación:** controles del repositorio, EventExperienceTests/SofaDemoTests y Hub real a 1440/800/390/360 px, temas claro/oscuro y estados pertinentes. No usar resultados históricos como actuales.
- **Entrega autorizada:** commit y push solo a `reconstruction/festival-social` después de revisar diff/verificaciones; sin merge, main ni force-push. No incluir sesiones, secretos o datos privados.

## DEC-032 — Misión ACTIVE Festival Social en una rama independiente

- **Autoridad y fecha:** instrucción explícita de Samuel, 2026-10-16. No se atribuye aprobación a Julián.
- **Asociación:** Issue #14 (demo final), consultado abierto; las reglas de confirmación del Issue #9/PREDEV se conservan. PR #24 corresponde exclusivamente al Hub.
- **Base/entrega:** `55bb83162b01e3131732716c85796ff668ce145b`, rama nueva `feature/active-festival-social`; commit/push solo de esta tarea y PR en borrador hacia `reconstruction/festival-social`. Sin merge, force-push ni main.
- **Alcance visual:** pantalla del encuentro ACTIVE y ajustes necesarios de sus estados asociados. Escritorio: cabecera compacta, punto/mapa principal y palabra/pista/actividad lateral. Móvil: persona/estado, palabra, pista autorizada, lugar/mapa compacto, actividad y confirmación. Conservar componentes, recursos locales, temas, foco, lectura lógica y área segura. Sin portada nueva ni rediseño de otras pantallas.
- **Conservación funcional:** rutas/formularios, CSRF, sesión/pertenencia, palabra/punto asignados, pista opcional privada solo durante ACTIVE autorizado, actualización automática y protección de borradores, confirmación bilateral, Saga pendiente, reintentos e idempotencia. Sin cambiar backend, reglas, permisos, esquema ni datos de la demo.
- **Verificación:** ACTIVE mediante aceptación de ambos en dos sesiones; confirmaciones/puntos solo con datos aislados. Revisar pista presente/ausente, contenido largo, Gamer/otras zonas (IDs distintos en pabellón 17), mapa/retorno, estados adyacentes y 1440/800/390/360 claro/oscuro. Controles actuales del repositorio antes del respaldo; no atribuir las 150 pruebas históricas al nuevo trabajo.

## DEC-034 — Beneficios con prioridad móvil y composición de cupón

- **Autoridad:** autorización visual explícita de Samuel, 2026-10-16; sin atribuir aprobación a Julián. Issue #14 consultado abierto. Respaldo de Mi Ping autorizado y realizado antes de este incremento (PR #26).
- **Base/entrega:** `6110e484bf590008795a882198b8a6a53da7b7be`, rama propia `feature/beneficios-mobile`; tras verificar, commit/push y PR en borrador hacia `feature/mi-ping-mobile`. Conservar Hub y ACTIVE. Sin merge, force-push ni cambios en main.
- **Alcance:** presentación de `/me/benefits` y CSS local de la pantalla. Saldo, coste y puntos faltantes juntos; acceso temprano a Mis canjes; cupón para el beneficio y promoción ficticia claramente separada. Sin portada grande ni cambios en otras pantallas o navegación compartida.
- **Conservación:** backend, catálogo/costes/saldos, reglas y permisos existentes, bloqueo por Saga pendiente, persistencia/migraciones, CSRF, claves idempotentes, formularios y estados REQUESTED/DELIVERED/INCIDENT intactos. No inventar stock, caducidad, códigos de canje ni disponibilidad. La promoción sigue siendo ficticia y no consume puntos.
- **Verificación/continuidad:** pauta proporcional de AGENTS.md; revisar vistas y estados afectados con datos aislados para débitos/entregas y demo MySQL en lectura. Controles pertinentes al final; no repetir evidencia válida de Mi Ping. Decisiones en DESIGN.md y entrega en docs/BENEFICIOS_MOBILE.md.

## DEC-035 — Home Festival Social con entrada móvil directa

- **Autoridad:** instrucción explícita de Samuel, 2026-10-16; no se atribuye aprobación a Julián. Issue #14 consultado abierto. Implementación directa autorizada; no se requiere otra propuesta aislada.
- **Base/entrega:** `6711b216d463300000a72a062f3da79a6d0b8d28`, rama propia `feature/home-mobile`; commit/push y PR en borrador hacia `feature/beneficios-mobile`. Preservar avances posteriores que existan (árbol limpio al comenzar); sin merge, force-push ni main.
- **Alcance:** solo Home `/`, plantilla, CSS acotado y recursos/documentación necesarios. Mensaje, contexto académico y acción antes del arte a390 px; secuencia visual breve; adaptación a360 y escritorio. Reutilizar la ilustración social local y documentar su procedencia, sin generar otra imagen ni cambiar Hub.
- **Conservación:** backend/stack, datos, reglas, matching, puntos, permisos, navegación compartida y pantallas terminadas intactos. Entrada `/demo`, preparación `/demo/prepare`, Mi Ping `/me`, eventos `/events` y resolución del evento propio `/me/event` conservadas; ningún GET de Home debe preparar o modificar datos. No fijar un ID de SOFA ni mostrar actividad/participantes inventados. La referencia a SOFA identifica la demo académica, no el evento de toda sesión.
- **Verificación:** móvil390 principal,360 estrechez, un escritorio y tema alternativo; accesos y condiciones afectadas, teclado/barra inferior/recortes y controles pertinentes del repositorio. Datos aislados para preparar/seleccionar demo de prueba; MySQL existente solo en lectura. Hooks desactivados, sin instalaciones ni subagentes.

## DEC-036 — Cierre visual integrado móvil, correcciones acotadas

- **Autoridad:** instrucción explícita de Samuel, 2026-10-16; sin atribuir aprobación a Julián. Issue #14 consultado abierto. No se declara completa toda la aplicación ni el Issue.
- **Base/entrega:** `feature/home-mobile`, `4d434912583cda5c729a2d695199aa1acf121dfc`; contiene Hub, ACTIVE, Mi Ping, Beneficios y Home. Rama propia `fix/mobile-journey-review`, PR en borrador hacia Home; conservar PR #24–#28 y su cadena. Sin merge, force-push ni cambios en main.
- **Evidencia previa al código:** recorrido real a390 px en dos sesiones independientes, aplicación Spring/H2 aislada. Aceptación bilateral → ACTIVE → mapa/retorno → primera confirmación con saldo0 → segunda con saldo10 por persona → Beneficios con coste20/faltan10. MySQL y demo existente sin escrituras.
- **Defectos observados/alcance:** en `/demo`, participantes duplicados en botones y selector, icono/encabezados repetidos que retrasan el formulario; en COMPLETED, continuación hacia Mi Ping/Beneficios detrás del feedback opcional y cifras sobredimensionadas. Corregir solo esa jerarquía y densidad, conservando formularios, CSRF, destinos, información y comportamiento. No rehacer las cinco pantallas aprobadas ni navegación compartida.
- **Verificación proporcional:**390 principal,360/tema alternativo/escritorio para las dos superficies modificadas; controles y pruebas existentes pertinentes. Revisar capturas reales además del DOM. Sin nuevas instalaciones, auditoría general ni matriz de toda la app.

## DEC-037 — Entrada propia del participante con prioridad móvil

- **Autoridad:** instrucción explícita de Samuel, 2026-10-16; sin atribuir aprobación a Julián. Issue #14 consultado abierto. La prioridad general queda registrada una sola vez en AGENTS.md.
- **Base/entrega:** `fix/mobile-journey-review`, `e8246b1f47d67ab38de7caabb8ff6b7c0a4583c6`, local/remota coincidentes y árbol limpio; rama propia `feature/participant-mobile-entry`, PR en borrador hacia esa base, sin merge/force-push/main. Preservar cadena de PR anteriores.
- **Alcance autorizado:** comprobar y mejorar alta manual → intereses → Mi Ping → búsqueda/conexión; móvil390 primero,360 después y escritorio operativo, sin rediseño administrativo ni repasar las cinco pantallas. Demo académica diferenciada. Preservar backend/reglas/privacidad/confirmaciones/Saga/puntos/beneficios, CSRF y control de versión.
- **Limitación comprobada antes del código:** en Spring/H2 aislada, crear un alias nuevo y guardar intereses devuelve a `/participants/{id}`; `/me` redirige a `/demo` porque no existe vínculo de sesión. No hay edición de alias/evento después de crear. No basta un cambio de CSS para cumplir el recorrido independiente de perfiles predefinidos.
- **Propuesta mínima pendiente de decisión:** borrador alias/evento/intereses entre dos pasos, regreso para corregir sin crear duplicados, alta final mediante servicios actuales y vínculo a sesión existente → Mi Ping. Sin cuentas, contraseñas, tablas, permisos o reglas nuevas. Consultada a Samuel antes de implementar ese enlace. Mientras se decide, solo documentación y presentación del editor de intereses existente, con rutas/formularios intactos.

## DEC-038 — Enlace mínimo de alta autorizado

- **Autoridad:** autorización explícita de Samuel,16 de octubre de2026 (hora Bogotá); no atribuida a Julián. Issue14 abierto. Sustituye el pendiente de DEC-037.
- **Base:** `ea04d3def9440c9319f8ade8b0fa765dba3b114d`, local/remota iguales, árbol limpio; rama propia `feature/participant-onboarding`, PR en borrador hacia `feature/participant-mobile-entry`. Sin merge/main/force-push.
- **Alcance:** entrada normal separada de demo; borrador corregible alias/evento/intereses en sesión, validación/CSRF/revisión, creación+intereses mediante servicios existentes en una transacción, vínculo solo al ID devuelto por servidor. Serializar finalización por sesión y conservar recibo para reintentos sin duplicar. Sin cuentas, contraseñas, tablas, migraciones ni reglas nuevas.
- **Conservación:** disponibilidad inicialmente falsa; activación/búsqueda explícitas existentes. Administración/demo conservados; sin cambios matching, privacidad, aceptación bilateral, mapa, palabra/pista, Saga, puntos o beneficios. Revisión móvil390/360 y pruebas dirigidas aisladas, sin alterar MySQL.

## DEC-039 — Resultado visible tras confirmar desde celular

- **Autoridad:** instrucción explícita de Samuel,16 de octubre2026 (Bogotá); no atribuida a Julián. Issue14 consultado abierto. Base limpia local/remota `6afac1ea2771f2adebda57b1476576b39064c051`, rama propia `fix/participant-confirmation-feedback`, PR en borrador hacia `feature/participant-onboarding`. Sin merge/main/force-push.
- **Evidencia previa:** dos altas normales reales, disponibilidad explícita, propuesta descubierta por polling en5,17 s sin recargar, aceptación bilateral, misión idéntica, primera confirmación0/0 y segunda10/10 con reintentos sin duplicar. En una comprobación enfocada posterior, confirmar redirige a y0 y deja el resultado de espera a y1103, fuera de la vista390.
- **Corrección acotada:** fragmento en la redirección POST de confirmación al encabezado real de confirmación/resultado, focos nativos y mismo destino en COMPLETED. Reutilizar polling; no cambiar reglas/estado, Saga, CSRF, permisos, privacidad, recompensa, alta ni composición de las pantallas.390 principal y360 solo en el resultado modificado; pruebas de encuentro/flujo pertinentes con datos aislados, MySQL intacto.

## DEC-040 — Conectar y entrada por código/QR de evento

- **Autoridad y fecha:** instrucción explícita de Samuel, 16 de octubre de 2026 (Bogotá), Issue #33. Autoriza rutas, controladores, persistencia y migración mínimas; no se atribuye aprobación a Julián ni queda pendiente repetir esta autorización.
- **Base:** `fix/participant-confirmation-feedback` @ `8df0b7df2311282fce1f843beb97dcc7a1f01255`; rama propia `feature/connect-event-entry`, PR en borrador hacia esa base. Conservar cadena anterior, sin merge/main/force-push.
- **Decisión:** Mi Ping reúne identidad, intereses, saldo e historial. `/connect` presenta disponibilidad/búsqueda explícitas sin match y resuelve propuesta/ACTIVE; GET nunca crea conexiones ni activa disponibilidad. Evento identificado por código/QR antes de alias/intereses; conservar borrador, revisión, recibo e identidad propia del servidor.
- **Persistencia y QR:** código público único/estable por evento, nueva V13 sin editar V1–V12; completar eventos anteriores sin borrar datos. QR local con biblioteca mínima si no existe equivalente; base del enlace explícita/configurable, sin inferir Host, cookies o identidad de sesión. No sustituye la palabra privada ni autentica.
- **Conservación:** reglas de estado de evento/alta, demo/admin, privacidad, matching, aceptación y confirmación bilateral, Saga, puntos y beneficios. Fuera: varios puntos, actividades/recompensas/mapa configurables, «Ya llegué», cuentas o autenticación nueva.
- **Entrega:** pruebas nuevas de código/enlace/QR/migración y reuso de protecciones del alta; revisión móvil390/360 y navegación afectada, sin auditoría general. Escaneo físico se distingue de decodificación automatizada. Evidencia y Windows en `docs/CONECTAR_CODIGO_QR.md`.

## DEC-041 — Catálogos de encuentro por evento y llegada bilateral

- **Autoridad:** continuación explícita del segundo avance ya autorizado por el usuario, 16 de octubre de 2026 (Bogotá). Issue #35. No se atribuye aprobación a Julián ni se exige repetir autorización.
- **Base/entrega:** `feature/connect-event-entry` @ `c288f778d8d7cdf72ec3abb4306473aa756b352b`, obtenida del remoto y sin avances posteriores; rama propia `feature/event-encounter-arrival`, PR en borrador hacia esa base, sin main/merge/force-push.
- **Configuración:** listas opcionales por evento de puntos públicos (160 caracteres), palabras base (40) y actividades breves (500), una opción por línea, hasta 20 por lista. Valores vacíos conservan los catálogos actuales (incluido SOFA); cada lista puede sobrescribirse de forma independiente. Selección determinista del servidor por ID de match, palabra con sufijo único; instrucciones guardadas al activar, sin cambios retroactivos al editar. Sin mapa configurable ni cambios de recompensa.
- **Llegada:** nuevos encuentros activados después de V14 requieren «Ya llegué» de ambas personas antes de confirmar encuentro/actividad. Es propio, bilateral, idempotente y sin puntos; conserva ACTIVE y permite terminar aun en pausa/cierre. La actividad sigue visible para preparar el encuentro. La confirmación final sigue separada, bilateral y con Saga/+10 actuales.
- **Compatibilidad:** V14 aditiva; listas vacías y llegada no requerida por defecto para filas existentes. Encuentros ya activos, completados, confirmación parcial y Saga pendiente conservan sus condiciones y datos. Migraciones V1–V13 intactas. Propuestas previas activadas después de actualizar usan el nuevo flujo. Privacidad, CSRF, sesión, matching, bloqueos, feedback y beneficios conservados.
- **Verificación:** catálogos/validación/aislamiento y snapshot, migración con filas anteriores, llegada propia/ajena/reintentos/concurrencia, pausa/cierre y confirmación/recompensa/Saga. Móvil390 principal,360 y escritorio/tema alternativo en superficies afectadas, datos aislados.

## DEC-042 — Corrección de llegada y configuración estructurada (PR36)

Autorización explícita del usuario, 17 octubre2026 Bogotá. Sustituye DEC-041: «Ya llegué» siempre informativo, nunca requisito, incluso arrival_required=true. No inventar llegadas. Puntos con nombre/indicaciones/activo; actividades con título/instrucciones/intereses/puntos compatibles/activo; palabras por evento. Asignación entre combinaciones activas compatibles, afinidad antes de ID estable; configuración sin combinación válida mantiene propuesta recuperable. Snapshot al activar. Listas V14 conservadas e importables una vez sin borrarlas; antes de importación conservan catálogo anterior. V15 aditiva. PR36 sigue en rama propia; sin main/merge. Verificación MySQL exclusiva, móvil y protecciones existentes. Sin atribuir aceptación a Julián.

## DEC-043 — Puntos variables y recompensas de evento

Misma autorización: segundo bloque desde último respaldo del primero. Actividades de0 a100 puntos enteros (límite académico); snapshot del importe al activar. Anteriores conservan10. Saga usa importe guardado tanto para créditos como compensaciones, incluidos recibos anteriores. Beneficios por evento con nombre/descripción/coste1 a10000/lugar/indicaciones/activo; demo global anterior conservada. Canjes guardan copia de condiciones al solicitar y preservan idempotencia/entrega. Misma administración académica sin autenticación nueva ni exposición adicional. Rama/PR en borrador independientes y continuidad actualizada antes de cada respaldo.

## DEC-044 — Coherencia del recorrido integrado

Tercer bloque autorizado17 octubre2026, después de respaldar DEC-043 en bce34589cc346ab73d21fcd277b5f3c029e11f4d/PR37. Rama fix/event-journey-consistency hacia feature/event-points-rewards. Evidencia Chromium/MySQL del bloque2: snack demo precede la recompensa local, enlaces de búsqueda pasan por Mi Ping, llegada sin marcar se presenta como «En camino/Esperando». Corregir prioridad de beneficio local, enlaces directos a Conectar y llegada «Sin registrar» sin espera obligatoria; pedir indicaciones en nuevos puntos y dar orientación honesta en antiguos sin mapa. Sin rediseño, migración ni cambios a importes/Saga/permisos. Verificar código/QR, dos sesiones, llegada opcional, actividad, puntos y canje hasta entrega.

## DEC-045 — Categorías y selección móvil de intereses

Autorización explícita del usuario17 octubre2026. Base remota comprobada552906a80a88285c7eca7221bbd6b7dd4e3913b7, sin avances posteriores ni cambios locales; rama feature/interest-categories-mobile hacia fix/event-journey-consistency/PR38. Sin main/merge/force-push. Categorías globales administrables con nombre/orden/activo y asociación de intereses existentes; una categoría opcional por interés, sin categoría bajo «Otros intereses». Ocho intereses actuales se agrupan en Cultura y entretenimiento, Creatividad y tecnología, Vida activa y viajes; no se agregan gustos. V17 aditiva y sin cambiar IDs/relaciones/encuentros.

La categoría organiza y determina disponibilidad para NUEVAS selecciones; no cambia matching ni afinidad entre IDs distintos. Inactivas no aparecen como opciones nuevas. Intereses ya persistidos en una categoría inactiva se muestran en «Conservados»: pueden mantenerse o quitarse, pero no volver a añadirse después de guardar su retirada. Alta/borrador sin selección persistida no puede finalizar con una opción inactiva; muestra el borrador para quitarla, sin crear duplicados. Validación transaccional en servidor, no solo controles ocultos. Catálogo serializado con una fila de bloqueo para evitar desactivación concurrente durante una selección; mismo orden evento→catálogo, sin cambiar matching/disponibilidad del participante.

Misma administración académica, sin panel/cuentas nuevos. Alta y editor reutilizan selector accesible con categorías visibles inicialmente, filtro por grupo, resumen/contador y selección estable al filtrar. Búsqueda local solo cuando catálogo visible supere12 opciones; con8 no se añade un campo innecesario. Mensaje sin resultados, teclado y temas existentes. Conservar recibo/borrador/CSRF/versión. Revisar catálogo público, demo y configuración de actividades. Tres respaldos: modelo/migración, administración/selector y verificación final. No atribuir aceptación a Julián.


## DEC-046 — Plano configurable por evento

18 octubre2026: autorización explícita de Samuel para implementar y respaldar carga PNG/JPEG/WebP, colocación sobre puntos existentes y vista móvil desde Mi evento/encuentro, sobre21ff7c9/PR39, rama feature/event-floorplan-mobile. No se atribuye aprobación a Julián. Conserva reglas anteriores, SOFA, main y permisos académicos; sin GPS/personas/rutas/cuentas/mapas externos.

Persistencia elegida: versiones inmutables de imagen normalizada en MySQL, bytes separados de metadatos (sin cargarlos en listados/estados). Respaldo MySQL incluye planos; GitHub solo código y capturas de evento ficticio. UUID generados, sin filesystem/URLs/rutas de usuario. Límite entrada8MiB, lado6000 y16MP antes de decodificar; salida proporcional lado3200 y4MiB como máximo, PNG o JPEG de calidad0.90. Lector WebP puro Java TwelveMonkeys3.12.0 específico para el formato requerido; PNG/JPEG del JDK. Rechazar otros formatos, animaciones/errores, datos no decodificables y sobrelímites; reencodificar elimina metadatos y contenido anexado.

V18 nueva: versiones/bytes/posiciones por ID de punto, referencia actual en evento y snapshot nullable versión/punto/x/y en match al activar. Reemplazar exige referencia actual explícita y crea versión sin posiciones. Mover/quitar solo afecta nuevas activaciones; evento bloqueado serializa configuración/aceptación, revisión protege formularios obsoletos. No inferir coordenadas históricas por nombre. Los activos conservan bytes y coordenadas originales; sin eliminación de versiones en este bloque. Históricos anteriores siguen con texto y SOFA aplicable.

Organizador: imagen/preview/dimensiones, reemplazo explícito, seleccionar punto del mismo evento y tocar plano; ajuste numérico0–1 accesible y quitar marcador sin borrar punto. Participante: acceso compacto sin desplazar palabra/actividad/confirmación, plano correcto con un punto distinguido por forma/texto, centrar/zoom/reset/pan; nombres e indicaciones fuera de imagen. Sin marcador, advertencia útil sin ubicación inventada; genéricos nunca reciben SOFA por defecto. Cuatro unidades con continuidad, commit/push/SHA/PR borrador hacia PR39; ver documento existente.
