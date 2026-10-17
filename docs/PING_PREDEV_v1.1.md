# PING — PREDEV v1.1 CONSOLIDADO
**Proyecto académico — Samuel Riveros y Julián Caicedo**

**Fecha:** 12 de octubre de 2026

**Estado:** alcance académico consolidado para iniciar desarrollo

## 1. Objetivo del proyecto

Ping es una aplicación web para eventos presenciales que busca facilitar que dos asistentes se conozcan mediante intereses, una propuesta de conexión voluntaria y una actividad breve que ayude a romper el hielo.

El objetivo académico no es construir una plataforma comercial completa, sino demostrar un MVP coherente, funcional y defendible ante el profesor.

## 2. Idea central

> Ping conecta a dos personas dentro de un evento según sus intereses y les ayuda a iniciar una interacción mediante un punto de encuentro, una palabra temporal y una actividad breve.

La actividad no reemplaza al matching: ocurre después de que dos personas aceptan conectarse.

## 3. Flujo principal del MVP

1. El administrador crea y abre un evento.
2. El participante entra al evento.
3. El participante usa un alias y selecciona sus intereses.
4. El participante indica que está disponible para una conexión.
5. Ping busca candidatos compatibles.
6. Ping propone un match a dos personas.
7. Ambos deben aceptar.
8. Ping muestra un punto de encuentro público.
9. Ping genera una palabra temporal para reconocer/confirmar la conexión.
10. Ping muestra una actividad o pregunta breve relacionada con intereses comunes o con el evento.
11. Ambos confirman que realizaron la interacción.
12. Cada participante recibe puntos.
13. Se solicita feedback opcional.
14. El participante puede buscar otra conexión si el evento sigue abierto.

## 4. Regla de matching

La prioridad será la afinidad por intereses, no simplemente quién activó primero.

Regla académica:

1. Mayor cantidad de intereses en común = mayor compatibilidad.
2. Si dos candidatos tienen la misma compatibilidad, se prioriza a quien lleve más tiempo esperando.
3. Bloqueos, rechazos o indisponibilidad nunca se ignoran.
4. Una persona solo puede tener un match activo a la vez.

### Match alternativo

Si no existe una coincidencia suficiente, Ping puede mostrar:

> “No encontramos alguien con tus mismos gustos, pero hay una persona interesada en X, Y y Z. ¿Quieres intentar una conexión?”

La otra persona recibe una propuesta equivalente. El match solo existe si ambos aceptan.

Si tampoco existe una conexión alternativa aceptada, se puede ofrecer una actividad del anfitrión con una recompensa equivalente para no obligar a aceptar un match.

## 5. Punto de encuentro y palabra temporal

Se eliminan del MVP las pistas basadas en ropa, altura, tatuajes, cabello, accesorios o ubicación GPS.

Después de la aceptación bilateral, Ping muestra:

- un punto de encuentro público;
- una palabra o símbolo temporal;
- la actividad que realizarán.

Cuando existe un interés común, la palabra puede estar relacionada con ese interés, pero debe ser suficientemente específica para evitar confusión, por ejemplo `PIXEL-27`.

Cuando el match alternativo no tenga intereses en común, la palabra puede salir de un catálogo neutral relacionado con el evento.

La palabra temporal sustituye al QR como mecanismo principal del MVP académico.

## 6. Actividades

Las actividades son breves y sirven para evitar el silencio incómodo después del encuentro.

Ejemplos:

- “Descubran un videojuego que ambos conozcan.”
- “Encuentren una cosa en la que estén de acuerdo y otra en la que sean diferentes.”
- “Hablen durante dos minutos sobre el interés que tienen en común.”
- “Respondan juntos una pregunta asociada al evento.”

Para el MVP las actividades pueden salir de un catálogo previamente definido. No se necesita IA generativa.

## 7. Confirmación

La conexión requiere aceptación de ambas personas antes de activarse.

Después del encuentro, ambos participantes deben confirmar la interacción. La palabra temporal puede utilizarse como apoyo para reconocer/confirmar que pertenecen al mismo match.

No se necesita QR para el MVP académico.

## 8. Puntos y beneficios

Regla simple propuesta para la demo:

- conexión/actividad completada: +10 puntos;
- actividad alternativa del anfitrión: recompensa equivalente;
- el saldo visible nunca será negativo;
- el sistema puede registrar internamente una reversión pendiente si fuese necesario.

Se puede demostrar un beneficio sencillo, por ejemplo:

> Snack del evento — 20 puntos.

Para la entrega física, el staff podrá registrar estados básicos:

- ENTREGADO;
- NO_ENTREGADO;
- INCIDENCIA.

No se pretende resolver logística comercial real en el MVP académico.

## 9. Feedback

Después de completar una conexión, Ping ofrece feedback opcional:

- 👍 útil/agradable;
- 😐 neutral;
- 👎 no me sirvió.

El feedback no da puntos.

## 10. Estados principales

### Evento
- BORRADOR
- ABIERTO
- PAUSADO
- CERRADO

### Match
- ESPERANDO
- OFRECIDO
- ACTIVO
- COMPLETADO
- CANCELADO

### Reglas de pausa/cierre

**PAUSADO:** no se crean nuevos matches. Los matches activos pueden terminar de forma controlada.

**CERRADO:** no se crean nuevos matches. Se permite un cierre controlado de interacciones que ya estaban activas y, si se usa canje, puede existir una ventana final limitada.

## 11. Funciones que SÍ entran en el MVP

- administración básica de eventos;
- participantes y alias;
- catálogo de intereses;
- disponibilidad para matching;
- algoritmo de compatibilidad;
- propuesta y aceptación bilateral;
- match alternativo voluntario;
- punto de encuentro;
- palabra/símbolo temporal;
- catálogo de actividades;
- confirmación bilateral;
- puntos;
- beneficio/canje básico opcional para la demo;
- feedback;
- cancelar conexión;
- bloqueo/reporte básico;
- panel administrativo con métricas sencillas.

## 12. Funciones que NO entran ahora

- GPS continuo;
- Bluetooth;
- pistas de vestimenta o características físicas;
- menores de edad en el escenario inicial;
- chat;
- fotografías;
- perfiles sociales persistentes;
- grupos de 3 o más personas;
- inteligencia artificial/ML para matching;
- pagos;
- sponsors;
- integración real con Tiked;
- Tiks;
- aplicación móvil nativa;
- microservicios;
- RabbitMQ;
- Kubernetes;
- Redis obligatorio.

Pueden considerarse ampliaciones futuras, pero no forman parte del MVP v1.1.

## 13. Relación con Tiked

Ping será independiente para el MVP académico.

Una futura integración con Tiked podrá plantearse como ampliación si existe autorización y una interfaz disponible, pero no es necesaria para demostrar Ping ante el profesor.

## 14. Stack aprobado

### Lenguaje principal
Java.

### Backend
Spring Boot.

### Interfaz web
- Spring MVC;
- Thymeleaf;
- Bootstrap;
- JavaScript básico.

### Persistencia
- Spring Data JPA / Hibernate;
- MySQL o MariaDB.

### Herramientas
- Maven;
- IntelliJ IDEA;
- Git;
- GitHub.

### Arquitectura
Monolito modular web con una sola base de datos.

No se utilizarán microservicios para este MVP.

## 15. Organización técnica sugerida

```text
src/main/java/.../ping
├── config
├── controller
├── dto
├── model
├── repository
├── service
└── module
    ├── event
    ├── participant
    ├── interest
    ├── matching
    ├── activity
    ├── reward
    └── feedback
```

La estructura exacta puede evolucionar durante la implementación sin cambiar la arquitectura general.

## 16. Demo objetivo para el profesor

La demostración mínima exitosa será:

1. crear/abrir un evento;
2. ingresar varios participantes;
3. seleccionar intereses;
4. activar búsqueda;
5. calcular compatibilidad;
6. proponer un match;
7. aceptar desde ambos participantes;
8. mostrar punto + palabra temporal;
9. mostrar actividad;
10. confirmar desde ambos lados;
11. acreditar puntos;
12. registrar feedback;
13. mostrar estadísticas básicas al administrador.

## 17. Política de cambios

Esta versión congela el alcance necesario para comenzar a programar.

No significa que Ping quede así para siempre. Los cambios futuros deberán:

1. anotarse en un `DECISION_LOG.md`;
2. indicar qué regla o funcionalidad cambia;
3. ser aceptados por Samuel y Julián;
4. actualizar la documentación antes de cambiar módulos afectados.

Una IA no debe introducir por sí sola funciones fuera de alcance.

## 18. Estado después de la consolidación

**MVP académico:** listo para preparación técnica e inicio del desarrollo, sujeto a configurar repositorio, entorno Java/Spring y backlog.

El objetivo ahora es implementar una demo sólida, no resolver todavía operación comercial, escalabilidad empresarial o integración real con terceros.

## Evolución autorizada — DEC-040

Samuel autoriza el 16 de octubre de 2026 separar Conectar de Mi Ping y simplificar la entrada identificando primero el evento por código público o QR local. El QR solo enlaza al alta del evento y no reemplaza la palabra temporal ni autentica personas. Se permiten código único persistente, migración conservadora, enlace base configurable y cambios mínimos de rutas/controladores; se conservan borrador/alta transaccional y reglas existentes. No se implementan múltiples puntos, actividades/recompensas/mapa configurables ni «Ya llegué». Registro y límites: DEC-040/Issue #33. No se atribuye aprobación a Julián.

## Ampliación autorizada DEC-041 — Encuentro configurable y llegada

El evento puede configurar puntos públicos, palabras base y actividades breves, una opción por línea. Vacío mantiene el catálogo actual. Se asignan y guardan tras aceptación bilateral, sin modificar encuentros activos al editar. Nuevas activaciones requieren «Ya llegué» propio de ambos antes de la confirmación final; llegar no acredita puntos ni sustituye la actividad/confirmación bilateral. Los activos anteriores conservan sus condiciones; +10, Saga y cierre controlado intactos. Ver DEC-041/Issue #35.

## Correcciones autorizadas DEC-042/043 (17 octubre2026)

Llegada propia opcional/informativa: no condiciona confirmar ni otorga puntos, también en filas con arrival_required=true. Catálogo estructurado del evento: puntos activos con indicaciones y actividades activas con intereses y puntos compatibles; no asignar combinaciones inválidas. Conservar datos/listas y misiones activas. Actividades pueden configurar0–100 puntos enteros, guardados al activar; anteriores10. Saga conserva importe para crédito y compensación. Recompensas por evento con coste1–10000 e instrucciones de reclamación; condiciones de canjes anteriores conservadas. No cambia matching/aceptación/confirmación bilaterales ni añade autenticación.
