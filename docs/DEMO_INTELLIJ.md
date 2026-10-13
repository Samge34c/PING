# Demo de Ping en IntelliJ — 14 de octubre de 2026

## Preparación

1. Clonar `https://github.com/Samge34c/PING.git`. Seleccionar la rama **feature/intereses**: incluye Eventos, Participantes e Intereses sin requerir merges. Abrir la raíz que contiene `pom.xml` en IntelliJ.
2. Usar un **JDK 21 completo** como Project SDK y para Maven. Importar el proyecto con **Maven Wrapper**; no requiere Maven global.
3. Tener MySQL **8.4.11 LTS** disponible. En una instalación local propia, preparar una base vacía, por ejemplo `CREATE DATABASE ping CHARACTER SET utf8mb4;`. Usar un usuario local con permisos para migraciones y datos de esa base. No borrar una base existente ni copiar credenciales al repositorio.
4. En la configuración de ejecución de `com.ping.PingApplication`, configurar `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`. Formato de URL local: `jdbc:mysql://localhost:3306/ping`. Si el servidor exige TLS, configurar la confianza correspondiente; no desactivar verificación para resolver errores.
5. Argumento de ejecución: `--spring.profiles.active=mysql` (también es el perfil predeterminado). **No usar bootstrap para la demo funcional**: ese perfil excluye persistencia y los módulos.
6. Ejecutar `PingApplication` y esperar `Started PingApplication`. Flyway aplica V1–V4, crea el esquema y catálogo demo; Hibernate solo valida. Detener y volver a ejecutar no borra datos.

## Pruebas y ejecución por terminal en Windows

Desde la raíz, con JDK 21 configurado:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=mysql"
```

En Unix: `./mvnw clean verify` y `./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql`.

Las pruebas usan H2 solo en alcance de test, aplican las migraciones y no necesitan MySQL externo. La aplicación normal usa MySQL; no incorpora H2 como base de demo/productiva. Bootstrap CSS está incluido localmente y no requiere Internet al mostrar las páginas, una vez descargadas las dependencias Maven.

## Guion para mostrar al profesor

1. Abrir el navegador en la dirección local del puerto 8080, ruta `/`.
2. Mostrar el inicio de PING y entrar en **Eventos**.
3. Crear **Feria Universitaria 2026**, con descripción «Datos de demostración».
4. Abrir el evento. Mostrar detalle y estado **Abierto**. Opcionalmente pausar y reanudar; no cerrar todavía para poder registrar participantes en el camino de demo implementado.
5. Entrar en **Participantes** y pulsar **Registrar participante**.
6. Escribir **Samuel**, elegir la feria y marcar **Estoy disponible para conectar**. Registrar.
7. Pulsar **Elegir intereses**. Marcar **Videojuegos**, **Tecnología** y **Música**; guardar y mostrar su detalle.
8. Repetir para **Julián**, con la misma feria y sus intereses.
9. Mostrar el catálogo etiquetado como **Datos de demostración** y los participantes registrados.
10. Detener `PingApplication`, volver a ejecutar con la misma base y mostrar que evento, participantes e intereses permanecen almacenados.

## Rutas y clases principales

| Función | Ruta | Clase |
| --- | --- | --- |
| Inicio | `/` | `HomeController` |
| Eventos | `/events` | `EventController` / `EventService` |
| Crear evento | `/events/new` | `EventForm` |
| Detalle / editar | `/events/{id}` / `/events/{id}/edit` | `Event` |
| Participantes | `/participants` | `ParticipantController` / `ParticipantService` |
| Registro | `/participants/new` | `ParticipantForm` |
| Detalle | `/participants/{id}` | `ParticipantView` |
| Catálogo | `/interests` | `InterestController` / `InterestService` |
| Selección | `/participants/{id}/interests` | `InterestSelectionForm` |

## Alcance y pendientes humanos

Demo académica local sin autenticación. Los nombres y catálogo son de demostración. La disponibilidad no genera conexiones. No hay matching, puntos ni feedback. Se detiene el avance antes de Issue #5.

PEND-006 (cierre controlado/plazos/canje) y PEND-007 (identidad, ingreso fuera del camino OPEN, permisos/bloqueos) siguen abiertos. Los detalles de matching necesitan revisión humana. El JPG oficial debe adjuntarse como archivo para copiarlo a `static/images/ping-logo-official.jpg`; no se redibuja ni reemplaza el logo. Ver [BRAND_GUIDE](BRAND_GUIDE.md).

Si falla el arranque: comprobar JDK 21 completo, servidor/base MySQL y presencia de variables (sin imprimir contraseñas). Una base ajena no debe limpiarse para resolver un error Flyway; utilizar una base nueva o revisar su historial. No ejecutar `ddl-auto=create` ni `create-drop`.

## Evidencia de esta sesión

40 pruebas ejecutadas en `clean test` y `clean verify`: cero fallos, errores u omitidas. Navegador Chromium contra MySQL 8.4.11 real con TLS y CA verificada: feria creada/abierta/editada/pausada/reanudada; Samuel y Julián registrados y disponibles; tres intereses guardados por cada uno. Tras reiniciar el JAR se conservaron datos y selecciones. El cierre de otro evento fue comprobado por la interfaz. V1–V4 presentes en el historial Flyway; únicamente las cuatro tablas del módulo y su historial. El servidor/volumen de validación es temporal y se retira al concluir, no reemplaza el MySQL local del equipo.
