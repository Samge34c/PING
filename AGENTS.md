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

## Prioridad móvil y verificación proporcional

Instrucción de Samuel, 2026-10-16: PING se usa principalmente desde celular durante el evento. Diseñar primero a **390 px**, resolver jerarquía, legibilidad, densidad, acciones al alcance y navegación; comprobar después **360 px**. Adaptar escritorio al flujo móvil aprovechando su espacio, sin partir de columnas de escritorio y apilarlas.

La prioridad de producto es el recorrido del participante desde celular, incluida la entrada sin perfiles demo predefinidos. Escritorio debe conservar navegación, formularios y tablas utilizables; corregir defectos concretos sin dedicar iteraciones a embellecer administración ni crear dashboards. Reutilizar capacidades existentes: el alta manual no autoriza cuentas, contraseñas o autenticación nueva. Ante un bloqueo funcional, proponer el cambio mínimo antes de ampliar el producto.

- En tareas visuales, concentrar el esfuerzo en implementar y examinar capturas reales. Revisar 390 px como referencia principal, 360 px para estrechez y un ancho de escritorio para regresiones. Revisar ambos temas; profundizar en el principal y comprobar contraste, superficies y controles en el alternativo.
- Validar los estados afectados. Ampliar solo por fallos, cambios funcionales o incertidumbre concreta; evitar matrices extensas y comprobaciones duplicadas sin una pregunta. Conservar controles obligatorios del repositorio y ejecutar la suite pertinente al finalizar, sin repetirla salvo modificaciones posteriores o fallos nuevos. No añadir tests de clases CSS, texto decorativo o estructura exacta del HTML; mantener protecciones de privacidad, confirmación bilateral, Saga e idempotencia cuando se vean afectadas.
- Antes de cerrar, examinar la captura móvil real y responder brevemente: ¿se entiende la pantalla y su acción principal?, ¿espacio vacío, texto o decoración desplazan lo importante?, ¿la navegación tapa contenido?, ¿tiene identidad propia y coherencia con Festival Social? Corregir los problemas observados; un conteo de pruebas no sustituye esta revisión.
- Mostrar primero la captura móvil y después escritorio; resumir verificaciones en pocas líneas. Usar skills selectivamente, sin auditorías generales, instalaciones ni subagentes por rutina.
- Conservar Hub y ACTIVE; no rehacerlos salvo un defecto concreto observado en una revisión móvil. Esta pauta de trabajo no amplía el alcance del producto ni autoriza cambios funcionales.
