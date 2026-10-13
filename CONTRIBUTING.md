# Contribuir a Ping

Samuel Riveros y Julián Caicedo comparten un repositorio y revisan los cambios del compañero. Antes de programar, leer [AGENTS.md](AGENTS.md), el [contexto IA](docs/AI_CONTEXT.md), [PREDEV](docs/PING_PREDEV_v1.1.md), [decisiones](docs/DECISION_LOG.md), el Issue y el módulo relacionado.

## Ramas y commits

Una tarea = una rama propia desde `main` actualizada:

| Prefijo | Uso |
| --- | --- |
| `feature/...` | Funcionalidad |
| `fix/...` | Corrección |
| `docs/...` | Documentación |
| `chore/...` | Preparación y herramientas |

Commits pequeños: `feat:`, `fix:`, `docs:`, `test:`, `refactor:` y `chore:`.

```text
feat: agrega selección de intereses
feat: implementa cálculo de afinidad
fix: evita matches simultáneos
docs: actualiza decisión de matching
test: agrega pruebas de MatchService
```

## Entrega

Issue → rama → trabajo → commit → push → Pull Request → revisión → merge autorizado.

Usar la plantilla de PR, vincular el Issue, explicar cómo se verificó y declarar cambios de base de datos y producto. No presentar pruebas no ejecutadas como aprobadas. Las pruebas técnicas del bootstrap se ejecutan con Maven Wrapper (`./mvnw clean test` en Unix, `.\mvnw.cmd clean test` en PowerShell), sin MySQL mediante perfil de prueba `bootstrap`; para documentación revisar enlaces, coherencia con PREDEV y `git diff --check`.

No subir secretos, archivos de IntelliJ ni salidas Maven. Revisar `git status` y el diff antes de cada commit. No programar simultáneamente el mismo módulo sin coordinación en el Issue. No asignar tareas a otro sin acuerdo.

## Cambios de producto

Registrar propuesta y pendiente en [DECISION_LOG](docs/DECISION_LOG.md). Samuel y Julián deben aceptar el cambio y actualizar PREDEV antes de modificar el módulo. No resolver contradicciones por suposición.

El procedimiento Git completo está en [WORKFLOW_GITHUB](docs/WORKFLOW_GITHUB.md).
