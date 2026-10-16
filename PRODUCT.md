# PING

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Personas que asisten a un evento y quieren conectar en persona a partir de intereses compartidos. La demo académica usa SOFA como contexto y a Samuel, Julián, Ana y Carlos como participantes de demostración.

## Product Purpose

PING conecta personas por intereses en eventos como SOFA. Hace posible una interacción breve y voluntaria: propuesta, aceptación de ambos, encuentro en un punto público, actividad, confirmación bilateral y puntos.

## Operating Context

Aplicación web para escritorio y móvil, con temas claro y oscuro. Proyecto académico de Samuel Riveros y Julián Caicedo. La demo SOFA es una simulación académica, sin vínculo oficial acreditado con el evento. El entorno de desarrollo actual usa Java 21, Spring Boot, Spring MVC, Thymeleaf, Bootstrap, JavaScript y MySQL; una aplicación y una base de datos.

## Capabilities and Constraints

- Preservar backend, persistencia, migraciones y preparación reutilizable de la demo.
- Preservar matching normal y alternativo, aceptación bilateral, confirmación bilateral y Saga de finalización.
- Preservar mapa, punto de encuentro, palabra temporal, actividad y pista de reconocimiento opcional. La pista no sustituye la palabra y su lectura ajena se limita al encuentro ACTIVE autorizado.
- Preservar puntos, beneficios, canje, historial, bloqueos y reportes. No cambiar reglas, cantidades ni permisos como efecto de un cambio visual.
- Mantener el stack existente, los recursos locales y los temas claro/oscuro. No introducir tecnologías ni dependencias para resolver una preferencia visual.
- Respetar AGENTS.md y la documentación funcional del repositorio. Ninguna guía de diseño autoriza cambios de alcance o aprobaciones atribuidas al equipo.

## Brand Commitments

Dirección solicitada por el usuario: **Festival Social**, con energía de convención y de app social; negro, morado y amarillo. Debe funcionar en móvil y en temas claro/oscuro. El Hub integra la segunda composición aprobada; DESIGN.md registra sus decisiones. La implementación de otras pantallas sigue fuera de este incremento.

Evitar espacios vacíos sin propósito, exceso de texto, ilustraciones genéricas y tarjetas repetidas. El espacio debe ayudar a entender, decidir o actuar. No reemplazar contenidos reales con pruebas sociales, fotografías, personas o datos inventados.

## Product Principles

1. Conectar de manera voluntaria y con aceptación de ambos.
2. Dar prioridad a la siguiente acción y al estado real de la persona.
3. Mantener el encuentro, la palabra y la confirmación comprensibles durante el evento.
4. Expresar Festival Social sin alterar la lógica ni la información funcional.
5. Proponer cambios a partir de evidencia, distinguiendo problemas observados de preferencias visuales.

## Workflow Constraints

El usuario eligió composición primero para portadas y showcases, y trabajo directamente en código para pantallas operativas. El valor de herramienta para portadas se registra en `.impeccable/config.json` (`buildPath: comp`).

Alcance actual autorizado por **Samuel**, 2026-10-16: reorganizar visualmente Mi Ping `/me`, priorizando estado real, siguiente acción, intereses y pista propia opcional. Rama `feature/mi-ping-mobile` desde `d088e59` (Hub, ACTIVE y pauta móvil de AGENTS.md). Conservar backend y funcionalidad; sin nueva portada ni cambios de otras pantallas, reglas, permisos, persistencia o migraciones. Ver DEC-033; no se atribuye aprobación a Julián. Samuel autoriza posteriormente el respaldo de Mi Ping y su PR en borrador hacia la rama base de ACTIVE.

## Evidence on Hand

- Código de GitHub basado en `e2759ae672adc60235b2a3c492c48a370724d544`.
- Demo local con MySQL y migraciones V1–V12 aplicadas.
- Documentación funcional, guía de demo y recursos SOFA existentes en el repositorio.
- La identidad Festival Social se está reconstruyendo a partir de instrucciones actuales. El trabajo más reciente y `PING_RESCATE.zip` de la otra tarea **no se han recuperado**. No inferir su contenido a partir de esta copia.

## Open Decisions

Hub y ACTIVE integrados se conservan. Samuel autoriza Mi Ping en DEC-033; las ampliaciones funcionales y otras pantallas quedan fuera.
