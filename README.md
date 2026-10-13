# Ping

Proyecto académico de **Samuel Riveros y Julián Caicedo**. Ping conecta a dos asistentes de un evento según sus intereses y facilita una interacción breve y voluntaria.

**Estado: PREDEV v1.1 — preparación técnica / pre-implementación.** El repositorio incorpora la base técnica Spring Boot; todavía no implementa funcionalidades de Ping.

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

El comando Maven de arranque es el mismo verificado con `bootstrap`; la conexión/arranque con MySQL real **no se ha validado** en Codex, donde no hay servidor ni credenciales. Ejecutar sin variables/servidor en el perfil `mysql` debe fallar; no equivale a una falla del perfil técnico.

### IntelliJ IDEA

1. Clonar el repositorio y abrir su carpeta raíz (contiene `pom.xml`).
2. Seleccionar JDK 21 como Project SDK y para el importador/runner Maven.
3. Permitir importación Maven y seleccionar **Maven Wrapper** como Maven home.
4. Crear/abrir la configuración de ejecución de `com.ping.PingApplication`.
5. Para verificación sin base, añadir argumento `--spring.profiles.active=bootstrap`. Para MySQL, usar perfil `mysql` y configurar `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` en las variables de esa configuración.
6. Ejecutar `PingApplication`. En `bootstrap`, esperar el inicio de Tomcat y el 404 de la raíz; no existe interfaz funcional aún.

Las carpetas `src/main/resources/static/` y `templates/` quedan listas para futuros Issues. Las pruebas actuales cargan contexto web y hacen una petición HTTP real sin persistencia; no prueban eventos ni matching.

### Verificación del bootstrap en Codex

`clean test` y `clean package` completados: 2 pruebas, 0 fallos, 0 errores, 0 omitidas en cada ejecución. Arranque Maven y JAR con `bootstrap` comprobados mediante petición HTTP real (404 JSON esperado). El arranque predeterminado sin variables MySQL falló, como se esperaba; no se verificó conexión SQL real. Windows e IntelliJ quedan pendientes de verificación local.
