# Demo de Ping en IntelliJ — 14 de octubre de 2026

## Preparación

1. Clonar `https://github.com/Samge34c/PING.git` y seleccionar **feature/mvp-demo**, que contiene todo el flujo sin hacer merge. Abrir la raíz con `pom.xml`.
2. Elegir un **JDK 21 completo** como SDK del proyecto y de Maven; importar con Maven Wrapper.
3. Preparar MySQL **8.4.11 LTS** y una base local propia, por ejemplo `CREATE DATABASE ping CHARACTER SET utf8mb4;`, con usuario que pueda aplicar migraciones. Si ya tienes datos de Ping, conservar esa base.
4. En la configuración de `com.ping.PingApplication`, definir `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`. URL local: `jdbc:mysql://localhost:3306/ping`. Configurar confianza TLS cuando corresponda; no desactivar verificación ni versionar secretos. Spring no carga `.env` automáticamente.
5. Ejecutar con `--spring.profiles.active=mysql` (predeterminado). **No usar bootstrap para la demo**. Flyway aplica V1–V6; si V1–V4 ya existen, agrega V5/V6 sin borrar los datos. Hibernate valida el esquema.
6. El JPG visible en el chat no está disponible como archivo del entorno. Copiar el original sin modificar a `src/main/resources/static/img/ping-logo.jpg` antes de compilar/ejecutar para verlo en navbar, inicio y selección demo. Las vistas detectan su presencia; no hay logo generado ni imagen rota.

## Terminal

Windows, variables de base configuradas en el entorno de la terminal:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=mysql"
```

Unix: `./mvnw clean test`, `./mvnw clean verify`, `./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql`.
Las pruebas usan H2 únicamente en test y no requieren MySQL externo. Bootstrap CSS está incluido localmente.

## Guion exacto para el profesor

1. Iniciar `PingApplication` y abrir la aplicación local del puerto 8080, ruta `/`.
2. **Eventos → Crear evento**: «Feria Universitaria 2026», punto «Entrada principal». Guardar y **Abrir evento**.
3. **Participantes → Registrar participante**: Samuel en esa feria, inicialmente no disponible. **Elegir intereses**: Videojuegos y Tecnología; guardar.
4. Repetir para Julián en la misma feria con Videojuegos y Música.
5. En navegador normal, **Entrar como participante** (ruta `/demo`), seleccionar Samuel. En incógnito o segundo navegador, seleccionar Julián. Dos pestañas normales comparten sesión: no sirven para esta prueba.
6. En **Mi Ping** (`/me`), ambos pulsan **Marcar disponible**. Samuel pulsa **Buscar conexión**.
7. Samuel ve `OFFERED` con Julián e interés Videojuegos. Julián pulsa **Actualizar** en Mi Ping y abre su tarjeta de conexión. Todavía no se muestra el encuentro.
8. Samuel pulsa **Aceptar conexión**. Una aceptación conserva `OFFERED`. Julián pulsa **Actualizar estado** y **Aceptar conexión**. Samuel pulsa **Actualizar estado**.
9. Ambos ven `ACTIVE`, «Entrada principal», la misma palabra `PIXEL-…` y «Descubran un videojuego que ambos conozcan». El sufijo es el ID único del match en base 36; no se espera un número fijo.
10. Samuel pulsa **Confirmar encuentro y actividad**: permanece `ACTIVE`, saldo 0. Julián confirma. Samuel actualiza: ambos ven `COMPLETED`, cada saldo es 10.
11. Samuel deja **Útil / agradable**; Julián pulsa **Omitir**. No se suman puntos por feedback. Volver a **Mi Ping** muestra el saldo y la conexión completada.
12. Opcional: repetir búsqueda para sumar otros 10, rechazar una propuesta para mostrar cancelación o reiniciar la aplicación con la misma base para demostrar persistencia. Reintentar una confirmación completada no duplica puntos.

Las respuestas se actualizan automáticamente cada cinco segundos mientras la pestaña está visible; también están los enlaces **Actualizar / Actualizar estado**. No hay chat ni tiempo de expiración automático del match.

## Reglas de la demo

Matching solo en OPEN, mismo evento y disponibles; prioridad por más intereses comunes, mayor espera e ID ascendente. Cero coincidencias ofrece alternativa claramente etiquetada, también bilateral. Oferta y activo reservan ambos participantes. Antes de cambiar disponibilidad se debe cancelar/terminar la conexión. Un rechazo/cancelación evita repetir la pareja en el ciclo actual; marcar no disponible y disponible inicia otro ciclo. Activos pueden finalizar tras pausa/cierre; propuestas no pueden activarse hasta reabrir un evento pausado.

Punto, palabra, actividad e intereses se guardan como instantánea; palabra visible/válida durante ACTIVE. Cancelar no entrega puntos. COMPLETED acredita +10 a cada participante una vez. Feedback opcional y persistente, sin puntos. El selector de sesión es explícitamente académico: no acredita identidad ni protege administración. Autenticación definitiva, bloqueo/reporte general, actividad del anfitrión premiada y canje quedan pendientes.

Si falla el arranque, comprobar JDK completo, MySQL, variables y el historial Flyway. No limpiar una base ajena ni usar `ddl-auto=create` o `create-drop`.

## Verificación del incremento

`clean test` y `clean verify`: 68 pruebas aprobadas, cero fallos/errores/omitidas. MySQL 8.4.11 con TLS VERIFY_CA y Chromium: actualización real V1–V4→V6 preservando datos; dos sesiones completaron el flujo y feedback/omitir; acciones simultáneas no duplicaron reservas ni puntos. Segundo encuentro: 20 puntos por persona. Rechazo, alternativa de cero coincidencias, cancelación y móvil sin desbordamiento comprobados. Reiniciar el JAR conservó eventos, participantes, intereses, matches completados, saldos y feedback. La base temporal de validación se retira al terminar; para mañana se necesita la base local del equipo.

## Atajo de preparación y mejoras visibles

- `/demo/prepare`: preparación manual de feria + Samuel/Julián/Ana/Carlos; repetir conserva los datos. Elegir Samuel y Julián en sesiones separadas y marcar solo a ambos disponibles para el primer recorrido.
- `/admin`: métricas reales por evento, propuestas/activos/completados, puntos entregados y feedback; acceso académico sin autenticación definitiva.
- `/demo/themes`: B oscuro y D claro oficiales con contraste calculado. El selector de navbar conserva la elección en localStorage; oscuro es predeterminado.
- Mi Ping guía el siguiente paso, muestra historial personal y recibe cambios automáticamente cada cinco segundos mientras la pestaña está visible. Los enlaces de actualización manual siguen disponibles.
- V7 agrega únicamente el marcador del escenario manual. No genera participantes al iniciar ni reinicia saldos.

El panel permite elegir un evento concreto; «Actualizar métricas» conserva el filtro. Se puede volver a «Todos los eventos» sin alterar datos.
