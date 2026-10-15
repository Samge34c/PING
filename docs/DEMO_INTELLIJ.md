# Ejecutar y demostrar Ping en IntelliJ

## Preparación local

1. Seleccionar **`feature/final-mvp`** y actualizarla. Abrir en IntelliJ la raíz que contiene `pom.xml`.
2. Usar un **JDK 21 completo** como SDK del proyecto y de Maven; importar con Maven Wrapper.
3. Iniciar MySQL **8.4.11 LTS**. Usar una base propia con permiso para aplicar migraciones. Para ensayar desde cero sin tocar otros datos, crear una base nueva de demo, por ejemplo `CREATE DATABASE ping_demo_entrega CHARACTER SET utf8mb4;`.
4. En la configuración de ejecución de **`com.ping.PingApplication`**, definir las variables **`DB_URL`**, **`DB_USERNAME`**, **`DB_PASSWORD`**. Ejemplo local de URL: `jdbc:mysql://localhost:3306/ping_demo_entrega`. Usar las credenciales del MySQL local, sin guardarlas en Git. Spring no carga `.env` automáticamente. Si se configura TLS, configurar la confianza del certificado; no desactivar verificaciones.
5. En argumentos de programa, añadir **`--spring.profiles.active=mysql`** y ejecutar `PingApplication`. No usar `bootstrap` para la demo. Flyway aplica **V1–V12** pendientes y Hibernate valida el esquema; no borra datos existentes.
6. Abrir **http://127.0.0.1:8080/**. Mantener **una sola instancia de Ping** sobre esta base (el coordinador Saga es local). Si hay otra instancia de Ping, detenerla; si otra aplicación ocupa 8080, configurar `--server.port=8081` y usar ese puerto en ambos navegadores.

La demo escucha **solo en 127.0.0.1 por defecto**: identidad y administración son públicas, sin autenticación ni separación de roles. No exponerla a una red compartida. Samuel y Julián se demuestran en dos contextos del navegador del mismo PC.

Si se dispone del JPG oficial, copiarlo sin modificar a `src/main/resources/static/img/ping-logo.jpg` antes de ejecutar/compilar. Navbar, inicio y entrada a demo lo detectan; su ausencia no bloquea la aplicación.

## Recorrido de exposición

1. **Entrar a demo → Preparar demo rápida → Preparar escenario demo**. Se prepara SOFA 2026, simulación académica en Corferias · Bogotá (8–12 de octubre). Samuel: Videojuegos/Anime; Julián: Videojuegos/Tecnología; Ana: Anime/Fotografía; Carlos: Cine/Música. Si existía la antigua feria demo, se adapta el mismo evento y sus intereses una sola vez, sin reiniciar puntos, disponibilidad, matches, bloqueos ni estado. Repetir conserva los datos, sin duplicarlos.
2. **Seleccionar Samuel** en ventana normal; **Seleccionar Julián** en incógnito o segundo navegador. Entrar desde el selector. Dos pestañas normales comparten sesión: no sirven para representar dos personas.
3. En **Mi Ping**, marcar disponibles solo a Samuel y Julián. Samuel pulsa **Buscar conexión**: propuesta normal con Videojuegos compartido.
4. Ambos aceptan. Se muestran Zona Gamer · Pabellón 18, la misma palabra temporal `PIXEL-…` y una actividad gamer breve. La palabra usa un sufijo único, no un número fijo. Opcionalmente, cada sesión puede guardar su propia prenda o accesorio en **Cómo reconocerme**; solo su conexión podrá verlo mientras ACTIVE. No hace falta para buscar ni confirmar. **Abrir mapa** abre la guía local con la zona seleccionada y permite volver al encuentro.
5. Ambos pulsan **Confirmar encuentro y actividad**. La Saga finaliza: COMPLETADO y **+10 a cada uno**. Una sola confirmación todavía espera a la otra persona; reintentar no duplica puntos.
6. Guardar feedback opcional, volver a **Mi Ping** para historial y abrir **Administración** para resultados del evento. En la cabecera de Mi Ping, el enlace SOFA abre **Mi evento** (portada, estado, puntos y zonas). La promoción `PING-GAMER` de Beneficios es ficticia, no válida fuera de la demo y no consume puntos.
7. Si se quiere mostrar canje, completar otra conexión entre ambos: saldo 20 → **Beneficios → Solicitar canje** → admin registra entregado/incidencia. Reintentar la solicitud no duplica el descuento.
8. Para alternativa/rechazo, dejar Samuel/Julián no disponibles y usar Ana/Carlos. No comparten intereses. Tras rechazar, marcar no disponible/disponible inicia un nuevo ciclo antes de volver a buscar.
9. **Dejar bloqueo y cierre del evento para el final**: persisten. Bloqueo impide futuras propuestas entre la pareja; reportes no sancionan. `/demo/prepare` no deshace bloqueos, no reinicia puntos ni reabre eventos cerrados. Para un nuevo ensayo limpio, usar otra base propia de demo; no borrar una base existente.

Los estados se actualizan cada cinco segundos con la pestaña visible; hay actualización manual. Oscuro es predeterminado y Oscuro/Claro conserva la preferencia local. Admin e identidad son de demo académica, sin autenticación definitiva.

## Comprobación y alternativa de terminal

Con JDK y variables de MySQL configurados, Windows:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=mysql"
```

Unix: `./mvnw clean test`, `./mvnw clean verify`, `./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql`.
Las pruebas usan H2 únicamente en test y no requieren MySQL externo. CSS/Bootstrap están incluidos localmente: el flujo de demo no depende de CDN.

Si falla el arranque, comprobar JDK completo, MySQL iniciado, base existente, variables de la configuración IntelliJ y puerto libre. No usar `ddl-auto=create`, `create-drop` ni limpiar Flyway para solucionar un fallo.
