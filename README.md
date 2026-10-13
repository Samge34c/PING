# Ping

Proyecto académico de **Samuel Riveros y Julián Caicedo**. Ping conecta a dos asistentes de un evento según sus intereses y facilita una interacción breve y voluntaria.

**Estado: PREDEV v1.1 — preparación técnica / pre-implementación.** Este repositorio contiene documentación y colaboración; todavía no implementa la aplicación.

## Flujo previsto

Evento → alias e intereses → disponibilidad → propuesta de match → aceptación de ambos → punto público y palabra temporal → actividad breve → confirmación de ambos → puntos → feedback opcional.

La afinidad por intereses determina la prioridad; el tiempo de espera desempata. El match alternativo también requiere aceptación bilateral.

## Stack aprobado

Java, Spring Boot, Spring MVC, Thymeleaf, Bootstrap y JavaScript básico; Spring Data JPA/Hibernate; MySQL o MariaDB; Maven e IntelliJ IDEA. Monolito modular web, una aplicación y una base de datos.

Las versiones y la elección entre MySQL/MariaDB están **PENDIENTES DE FIJAR AL CREAR EL PROYECTO**. No hay instrucciones de ejecución todavía.

## Organización

- [PREDEV v1.1](docs/PING_PREDEV_v1.1.md): fuente funcional principal.
- [Contexto para IA](docs/AI_CONTEXT.md) y [AGENTS.md](AGENTS.md): lectura para agentes.
- [Decisiones y pendientes](docs/DECISION_LOG.md).
- [Arquitectura](docs/ARCHITECTURE.md).
- [Backlog inicial](docs/BACKLOG_INICIAL.md).
- [Flujo GitHub](docs/WORKFLOW_GITHUB.md), [contribución](CONTRIBUTING.md) y plantillas en `.github/`.

## Contribuir

Leer las fuentes, seleccionar un Issue y crear una rama propia desde `main` actualizada. Entregar cambios pequeños mediante Pull Request para revisión del compañero. GitHub es la memoria compartida; los chats individuales no. Ver [CONTRIBUTING.md](CONTRIBUTING.md).
