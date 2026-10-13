# Ping

Proyecto académico de **Samuel Riveros y Julián Caicedo**. Ping conecta a dos asistentes de un evento según sus intereses y facilita una interacción breve y voluntaria.

**Demo funcional en `feature/mvp-demo`, pendiente de merge:** eventos, participantes, intereses, matching, sesión demo, aceptación bilateral, encuentro/palabra/actividad, confirmación, +10 puntos y feedback opcional. [Guion y ejecución para mañana](docs/DEMO_INTELLIJ.md). Demo rápida: `/demo/prepare`; métricas por evento: `/admin`; comparación de paletas: `/demo/themes`.

## Flujo previsto

Evento → alias e intereses → disponibilidad → propuesta de match → aceptación de ambos → punto público y palabra temporal → actividad breve → confirmación de ambos → puntos → feedback opcional.

La afinidad por intereses determina la prioridad; el tiempo de espera desempata. El match alternativo también requiere aceptación bilateral.

## Stack aprobado

Java, Spring Boot, Spring MVC, Thymeleaf, Bootstrap y JavaScript básico; Spring Data JPA/Hibernate; MySQL o MariaDB; Maven e IntelliJ IDEA. Monolito modular web, una aplicación y una base de datos.

Bootstrap: **Java 21**, **Spring Boot 4.1.1**, **Maven 3.9.16** mediante Wrapper **3.3.4**, **MySQL 8.4.11 LTS** como referencia SQL. Las dependencias Spring y Connector/J 9.7.0 las gestiona el BOM de Spring Boot. Ver [DEC-015](docs/DECISION_LOG.md#dec-015--bootstrap-técnico-spring-boot).

## Organización

- [PREDEV v1.1](docs/PING_PREDEV_v1.1.md): fuente funcional principal.
- [Contexto para IA](docs/AI_CONTEXT.md) y [AGENTS.md](AGENTS.md): lectura para agentes.
- [Decisiones y pendientes](docs/DECISION_LOG.md).
- [Arquitectura](docs/ARCHITECTURE.md).
- [Backlog inicial](docs/BACKLOG_INICIAL.md).
- [Flujo GitHub](docs/WORKFLOW_GITHUB.md), [contribución](CONTRIBUTING.md) y plantillas en `.github/`.

## Contribuir

Leer las fuentes, seleccionar un Issue y crear una rama propia desde `main` actualizada. Entregar cambios pequeños mediante Pull Request para revisión del compañero. GitHub es la memoria compartida; los chats individuales no. Ver [CONTRIBUTING.md](CONTRIBUTING.md).

## Desarrollo local

Para la demo actual de `feature/mvp-demo`, seguir [DEMO_INTELLIJ.md](docs/DEMO_INTELLIJ.md). Las secciones siguientes conservan la preparación y verificación por incrementos; los resultados del bootstrap no describen el alcance funcional actual.

Requisitos: JDK **21** y acceso a Maven Central para la primera descarga. Maven global no es necesario: se incluye Wrapper oficial con checksum SHA-256. MySQL **8.4.11 LTS** es la referencia para persistencia; no se instala ni es obligatorio para probar el perfil técnico `bootstrap`.

Clonar `https://github.com/Samge34c/PING.git` y ejecutar los comandos desde la raíz. Hasta fusionar esta PR, seleccionar la rama `chore/bootstrap-spring-boot`.

### Windows (PowerShell)

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=bootstrap"
```

### Linux / macOS

```bash
./mvnw clean test
./mvnw clean package
./mvnw spring-boot:run -Dspring-boot.run.profiles=bootstrap
```

Alternativa tras empaquetar, igual en Windows/Unix:

```text
java -jar target/ping-0.0.1-SNAPSHOT.jar --spring.profiles.active=bootstrap
```

Estos comandos de Maven y JAR se verifican en Linux. El archivo oficial `mvnw.cmd` proporciona el equivalente Windows; Windows e IntelliJ no se ejecutaron en Codex. En redes con proxy, configurar el proxy en los ajustes Maven locales sin versionar credenciales; Codex utiliza ajustes fuera del checkout.

`bootstrap` inicia el servidor web con JDBC/JPA deshabilitados explícitamente. No verifica persistencia ni implementa pantallas: una petición a la raíz del puerto 8080 devuelve **404**, resultado esperado porque todavía no hay endpoints de negocio. Detener con Ctrl+C.

### MySQL y variables

El perfil predeterminado es `mysql`. Requiere servidor MySQL disponible, base creada externamente y permisos del usuario configurados fuera de Git. No hay entidades ni scripts de tablas. La aplicación utiliza `spring.jpa.hibernate.ddl-auto=none`, por lo que no crea ni elimina esquemas.

| Variable | Uso |
| --- | --- |
| `DB_URL` | URL JDBC, por ejemplo `jdbc:mysql://localhost:3306/ping` |
| `DB_USERNAME` | Usuario configurado localmente |
| `DB_PASSWORD` | Contraseña configurada localmente |

`.env.example` documenta nombres y deja valores vacíos. `.env` está ignorado y **Spring Boot no lo carga automáticamente**: establecer variables en el proceso o configuración de ejecución de IntelliJ. No subir valores reales. Para usar MySQL, configurar las tres variables de forma local y ejecutar:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=mysql"
```

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```

El perfil `mysql` se verificó en Codex contra un MySQL 8.4.11 temporal: conexión JDBC, inicialización JPA, respuesta HTTP y ausencia de tablas creadas. La conexión utilizó TLS `VERIFY_CA` con la CA del servidor en un almacén temporal. Esa instancia de validación y sus credenciales fueron retiradas; cada desarrollador debe configurar su servidor y variables locales. Docker solo se utilizó para esta comprobación, no es un requisito del proyecto. Ejecutar sin variables/servidor en el perfil `mysql` debe fallar; no equivale a una falla del perfil técnico.

### IntelliJ IDEA

1. Clonar el repositorio y abrir su carpeta raíz (contiene `pom.xml`).
2. Seleccionar JDK 21 como Project SDK y para el importador/runner Maven.
3. Permitir importación Maven y seleccionar **Maven Wrapper** como Maven home.
4. Crear/abrir la configuración de ejecución de `com.ping.PingApplication`.
5. Para verificación sin base, añadir argumento `--spring.profiles.active=bootstrap`. Para MySQL, usar perfil `mysql` y configurar `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` en las variables de esa configuración.
6. Ejecutar `PingApplication`. En `bootstrap`, esperar el inicio de Tomcat y el 404 de la raíz; no existe interfaz funcional aún.

Las carpetas `src/main/resources/static/` y `templates/` quedan listas para futuros Issues. Las pruebas actuales cargan contexto web y hacen una petición HTTP real sin persistencia; no prueban eventos ni matching.

### Verificación del bootstrap en Codex

`clean test` y `clean package` completados: 2 pruebas, 0 fallos, 0 errores, 0 omitidas en cada ejecución. Arranque Maven y JAR con `bootstrap` comprobados mediante petición HTTP real (404 JSON esperado). El arranque predeterminado sin variables MySQL falló, como se esperaba; el perfil `mysql` también arrancó contra MySQL 8.4.11 real con JDBC/JPA y TLS verificados; la base permaneció sin tablas. Windows e IntelliJ quedan pendientes de verificación local.

### Integración continua

[Bootstrap CI](.github/workflows/bootstrap.yml) ejecuta `clean verify` con JDK 21 y Maven Wrapper en Ubuntu y Windows, tanto en PR hacia `main` como al enviar cambios a `main` o a la rama de bootstrap. Usa acciones oficiales fijadas por SHA, permisos de lectura y verificación de la firma del JDK. No requiere MySQL ni secretos: las dos pruebas técnicas activan `bootstrap`.

El flujo está preparado; sus resultados deben comprobarse en los checks de la PR. La configuración no constituye por sí sola una ejecución aprobada ni verifica IntelliJ.

## Demo de Eventos (Issue #2)

La rama `feature/eventos` se apila sobre `chore/bootstrap-spring-boot` mientras #16 espera revisión. No requiere fusionar las PRs para probar la rama completa.

1. Seleccionar `feature/eventos` en IntelliJ y abrir `pom.xml` con JDK 21 y Maven Wrapper.
2. Preparar una base MySQL local vacía, por ejemplo `CREATE DATABASE ping CHARACTER SET utf8mb4;`, y un usuario local con permisos de migración/lectura/escritura. No borrar una base existente.
3. Configurar `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` en la configuración de `PingApplication`. Usar perfil `mysql` (predeterminado), no `bootstrap`, para la demo.
4. Ejecutar `PingApplication`. Flyway aplica V1; Hibernate solo valida el esquema.
5. Abrir la ruta `/`, entrar en Eventos, crear «Feria Universitaria 2026», abrirla, editarla, pausar, reanudar y cerrar. Un evento cerrado permanece almacenado y no puede reabrirse.

Rutas: `/`, `/events`, `/events/new`, `/events/{id}`, `/events/{id}/edit`. Las acciones de estado son POST desde los botones de detalle; no se cambian estados por GET. Formularios inválidos, evento inexistente y transiciones rechazadas tienen mensajes comprensibles.

Las pruebas usan H2 **solo para testing**, con migraciones Flyway y validación JPA. `mvnw.cmd clean verify` en Windows, `./mvnw clean verify` en Unix. No requieren MySQL externo. La validación de MySQL real se realiza adicionalmente.

Marca y pendiente del JPG original: [BRAND_GUIDE](docs/BRAND_GUIDE.md). Bootstrap 5.3.8 se incluye localmente con licencia MIT; la identidad se centraliza en `brand.css`. El módulo aún no tiene autenticación: esta demo se ejecuta localmente, con datos de demostración.

Validación Issue #2: `clean test` y `clean verify` con 25 pruebas aprobadas, cero fallos/errores/omitidas. En Chromium y MySQL 8.4.11 se comprobó crear/abrir/pausar/reanudar/editar/consultar, con V1 aplicada y TLS verificado. La vista móvil de 390 px no presentó desbordamiento horizontal. Cierre y rechazo de reapertura también están cubiertos por las pruebas de ciclo de vida. El archivo del logo oficial sigue pendiente de recepción.

## Demo de Participantes (Issue #3)

Rama apilada: `feature/participantes` sobre `feature/eventos`. Incluye Eventos y aplica V2 sin borrar los eventos existentes. Para probarla, seleccionar esa rama y ejecutar con las mismas variables MySQL del apartado anterior.

Rutas `/participants`, `/participants/new` y `/participants/{id}`. Abrir la feria, entrar en Participantes, registrar el alias «Samuel», elegir el evento y marcar disponibilidad. Repetir con «Julián». Desde el detalle se puede alternar disponibilidad. Son datos de demostración, no cuentas ni identidades verificadas.

El registro de demo implementa el camino de evento OPEN; las políticas de ingreso de otros estados y autenticación siguen en PEND-007. La disponibilidad declara intención, no genera matches. Las pruebas no requieren MySQL externo y cubren validación, pertenencia, persistencia, cambios de disponibilidad, edición obsoleta y formularios HTTP.

Validación Issue #3: `clean test` y `clean verify`, 32 pruebas aprobadas, sin fallos/errores/omitidas. Chromium registró Samuel y Julián en MySQL, alternó disponibilidad y verificó el listado. V2 conservó el evento de V1; se comprobó que el checkbox es accesible por su etiqueta y que el listado móvil no desborda la página.

## Antecedente Issue #4: Eventos → Participantes → Intereses

Seleccionar **`feature/intereses`** para disponer de los tres módulos antes del merge. La cadena de revisión es #16 → Eventos → Participantes → Intereses; cada PR tiene su base temporal anterior.

V3 crea el catálogo y la relación muchos-a-muchos; V4 inserta los ocho intereses **de demostración** suministrados por el usuario. No se crean eventos ni participantes de producción ni se borran datos al iniciar. Flyway aplica cada migración una vez y Hibernate valida el esquema.

Rutas nuevas: `/interests` (catálogo) y `/participants/{id}/interests` (selección múltiple). Desde el detalle del participante, pulsar «Elegir intereses», marcar Videojuegos, Tecnología y Música y guardar. Los intereses aparecen en su detalle. La selección puede modificarse o limpiarse; no se calcula matching.

Guion exacto y requisitos: [DEMO_INTELLIJ](docs/DEMO_INTELLIJ.md).

Validación Issue #4: `clean test` y `clean verify`, 40 pruebas aprobadas, cero fallos/errores/omitidas. Chromium y MySQL 8.4.11 con TLS VERIFY_CA comprobaron el catálogo, selección múltiple para Samuel y Julián, vistas móvil/escritorio y persistencia tras reiniciar la aplicación. Se verificó también el cierre de otro evento real. V1–V4 aplicadas sin borrar los datos previos. No se implementó Issue #5.
