# Flujo GitHub colaborativo — Samge34c/PING

Adaptado de `GUIA_GITHUB_COLABORATIVO_PING.md` aportada por el usuario. La numeración de decisiones de aquella guía era ilustrativa; el registro real está en [DECISION_LOG](DECISION_LOG.md).

**GitHub es la memoria compartida. Los chats individuales NO.** Samuel y Julián, cada uno con su GPT, deben leer las mismas fuentes, Issues y PRs.

## Acceso y main

El propietario debe comprobar la visibilidad del repositorio y el acceso del compañero; este documento no afirma que estén configurados. Cada integrante utiliza su cuenta y su conexión GitHub autorizada en su herramienta IA. La ruta de conexión depende de la interfaz disponible. No compartir sesiones, contraseñas ni tokens.

Configurar protección de `main` desde ajustes GitHub: exigir PR, preferiblemente una aprobación, impedir force push y eliminación. Su disponibilidad depende del plan/permisos; pendiente de verificar por el propietario. No se ha aplicado automáticamente.

## Ramas

| Rama | Propósito |
| --- | --- |
| `main` | Versión estable revisada |
| `feature/*` | Funcionalidad |
| `fix/*` | Corrección |
| `docs/*` | Documentación |
| `chore/*` | Preparación técnica |

Una tarea = una rama. No trabajar simultáneamente en la misma rama. Ejemplo: Samuel + GPT en `feature/matching`; Julián + GPT en `feature/eventos`, solo tras acordar las tareas en sus Issues. No asignar responsables automáticamente.

## Flujo diario

Issue → branch → trabajo → commit → push → Pull Request → revisión → merge.

```bash
git status
git switch main
git pull --ff-only origin main
git switch -c feature/nombre-tarea
# Trabajar y revisar el diff; añadir únicamente archivos de la tarea.
git add ruta/del/archivo
git commit -m "feat: descripción concreta"
git push -u origin feature/nombre-tarea
```

Crear PR hacia `main`, vincular el Issue y completar la plantilla. El compañero revisa y se integra únicamente con autorización. Después del merge:

```bash
git switch main
git pull --ff-only origin main
```

Si una rama existente necesita cambios recientes, actualizarla mediante merge de `origin/main` y resolver conflictos revisando ambas intenciones. No usar force push para ocultar conflictos. Guardar cambios propios antes de cambiar de rama.

## Coordinación entre personas e IA

1. Consultar [backlog](BACKLOG_INICIAL.md) e Issues existentes antes de crear duplicados.
2. Acordar en el Issue alcance, responsable y módulos afectados; cada quien confirma su tarea.
3. Leer `AGENTS.md`, `AI_CONTEXT.md`, PREDEV, decisiones, Issue y código relacionado antes de programar.
4. Registrar dudas y acuerdos en GitHub, no solo en chats. Si hay conflicto con PREDEV, detener el cambio afectado y pedir decisión humana.
5. Evitar dos agentes modificando el mismo módulo sin coordinación.
6. Mantener PRs pequeños y pruebas verificables; revisar documentación al integrar cambios.

Prompt para ambos GPT:

> Trabajarás en Samge34c/PING. Lee AGENTS.md, docs/AI_CONTEXT.md, docs/PING_PREDEV_v1.1.md, docs/DECISION_LOG.md y el Issue. No inventes requisitos ni amplíes el MVP. Trabaja en una rama propia y entrega PR. Si encuentras contradicciones, solicita decisión humana antes de modificar el comportamiento afectado.

## Cambios funcionales

Propuesta → decisión aceptada por Samuel y Julián → DECISION_LOG → PREDEV/contexto actualizado → implementación y pruebas. No asumir aceptación por silencio.
