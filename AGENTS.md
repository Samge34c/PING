# Instrucciones para agentes de Ping

GitHub contiene la memoria compartida del proyecto. No dependas del contexto privado de chats anteriores.

## Antes de modificar código

1. Lee `docs/AI_CONTEXT.md`.
2. Lee `docs/PING_PREDEV_v1.1.md`.
3. Lee `docs/DECISION_LOG.md`.
4. Lee el Issue asociado. Si no existe, prepara el Issue y acuerda el alcance antes de programar.
5. Revisa el código existente del módulo y sus pruebas.

## Reglas obligatorias

- No inventes requisitos ni cambies el alcance silenciosamente.
- No trabajes directamente en `main`: una tarea = una rama propia y un Pull Request.
- No añadas tecnologías innecesarias ni introduzcas microservicios.
- No cambies arquitectura sin una decisión registrada.
- No asumas comportamientos no documentados; registra el pendiente y solicita decisión humana.
- Si hay contradicción, informa antes de programar.
- Mantén cambios pequeños y revisables; no modifiques archivos ajenos sin motivo.
- Nunca guardes tokens, contraseñas ni secretos en Git.
- No hagas merge sin autorización.
- No atribuyas aprobación a un integrante sin evidencia.

Si una petición contradice PING_PREDEV_v1.1.md, detente y solicita decisión humana.

Los cambios del producto requieren aceptación de Samuel y Julián, registro en `docs/DECISION_LOG.md` y actualización documental antes del código afectado. Consulta `CONTRIBUTING.md` y `docs/WORKFLOW_GITHUB.md`.

Consulta el registro de [skills de PING](docs/SKILLS_PING.md). Seleccionar skills según la tarea; no cargar todas por defecto.
