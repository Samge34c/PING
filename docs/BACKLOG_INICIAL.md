# Backlog inicial — PREDEV v1.1

Backlog y estado de las propuestas de implementación; **ningún estado equivale a aprobación humana ni cierre manual de Issues**. Fuente: [PREDEV](PING_PREDEV_v1.1.md). Las fases 0–3 tienen implementación probada en PRs apiladas pendientes de revisión/merge; las fases 4–13 siguen sin implementación. Ninguna fase tiene responsable asignado. La secuencia indica dependencias; no estima fechas ni atribuye trabajo a Samuel o Julián.

Cada sección sirve como cuerpo preparado para su Issue GitHub, usando el título indicado. Antes de crear Issues remotos revisar duplicados. Registrar enlaces reales cuando existan; no inventar números. Los enlaces de las fases corresponden a Issues remotos verificados; todos permanecen abiertos.

La preparación documental inicial fue integrada; PR #16 contiene solo el bootstrap. #17 agrega Eventos, #18 Participantes y #19 Intereses, cada una sobre su base anterior. Matching (fase 4 / Issue #5) requiere nueva revisión humana y no se ha iniciado.

## Fase 0 — Bootstrap Spring Boot

- **Título del Issue:** Bootstrap Spring Boot.
- **Estado:** BASE TÉCNICA VERIFICADA; pendiente revisión y merge; sin responsable; Issue remoto abierto: https://github.com/Samge34c/PING/issues/1.
- **Objetivo:** Verificar versiones oficiales compatibles, elegir SQL y coordenadas; crear después un esqueleto sin negocio.
- **Dependencias:** PEND-001 resuelto por DEC-015; documentación revisada.
- **Resultado esperado:** Proyecto Spring Boot con MVC, Thymeleaf, JPA, driver SQL y Maven; configuración local sin secretos.
- **Criterio de terminado:** Versiones y fuentes documentadas; build Maven y arranque mínimo verificados en entorno limpio.
- **Entrega:** PR vinculada, pruebas pertinentes y documentación; revisión antes de merge.

## Fase 1 — Implementar modelo Evento

- **Título del Issue:** Implementar modelo Evento.
- **Estado:** IMPLEMENTADO Y PROBADO; pendiente revisión/merge; sin responsable; Issue abierto: https://github.com/Samge34c/PING/issues/2; PR apilada: https://github.com/Samge34c/PING/pull/17.
- **Objetivo:** Crear eventos y estados BORRADOR/ABIERTO/PAUSADO/CERRADO.
- **Dependencias:** Fase 0; PEND-006.
- **Resultado esperado:** Modelo y gestión básica del evento.
- **Criterio de terminado:** Estados válidos probados; pausa/cierre impiden nuevos matches y permiten finalización controlada según decisión.
- **Entrega:** PR vinculada, pruebas pertinentes y documentación; revisión antes de merge.

## Fase 2 — Implementar participantes

- **Título del Issue:** Implementar participantes.
- **Estado:** IMPLEMENTADO Y PROBADO; pendiente revisión/merge; sin responsable; Issue abierto: https://github.com/Samge34c/PING/issues/3; PR apilada: https://github.com/Samge34c/PING/pull/18.
- **Objetivo:** Ingreso, alias, pertenencia y disponibilidad.
- **Dependencias:** Fase 1; PEND-007.
- **Resultado esperado:** Participantes vinculados al evento.
- **Criterio de terminado:** Ingreso y disponibilidad verificados, sin datos físicos ni perfiles sociales persistentes.
- **Entrega:** PR vinculada, pruebas pertinentes y documentación; revisión antes de merge.

## Fase 3 — Implementar catálogo de intereses

- **Título del Issue:** Implementar catálogo de intereses.
- **Estado:** IMPLEMENTADO Y PROBADO; pendiente revisión/merge; sin responsable; Issue abierto: https://github.com/Samge34c/PING/issues/4; PR apilada: https://github.com/Samge34c/PING/pull/19.
- **Objetivo:** Catálogo y selección por participante.
- **Dependencias:** Fase 2.
- **Resultado esperado:** Intereses disponibles para afinidad.
- **Criterio de terminado:** Selección persistida y consultable; casos inválidos probados.
- **Entrega:** PR vinculada, pruebas pertinentes y documentación; revisión antes de merge.

## Fase 4 — Implementar matching por afinidad

- **Título del Issue:** Implementar matching por afinidad.
- **Estado:** PENDIENTE; sin responsable; Issue remoto: https://github.com/Samge34c/PING/issues/5.
- **Objetivo:** Ordenar por intereses comunes y espera; respetar disponibilidad, bloqueos y rechazos; ofrecer alternativa voluntaria.
- **Dependencias:** Fases 1–3; PEND-002 y PEND-003; detalle de bloqueos PEND-007.
- **Resultado esperado:** Propuestas compatibles sin más de un match activo por persona.
- **Criterio de terminado:** Pruebas de afinidad, empate, exclusiones, alternativa y concurrencia; no se activa conexión unilateral.
- **Entrega:** PR vinculada, pruebas pertinentes y documentación; revisión antes de merge.

## Fase 5 — Implementar aceptación bilateral

- **Título del Issue:** Implementar aceptación bilateral.
- **Estado:** PENDIENTE; sin responsable; Issue remoto: https://github.com/Samge34c/PING/issues/6.
- **Objetivo:** Registrar aceptación de ambos, rechazo y cancelación.
- **Dependencias:** Fase 4; PEND-003.
- **Resultado esperado:** OFRECIDO pasa a ACTIVO solo con ambos; cancelación disponible.
- **Criterio de terminado:** Pruebas de una/dos aceptaciones, rechazo, cancelación y solicitudes repetidas sin activaciones duplicadas.
- **Entrega:** PR vinculada, pruebas pertinentes y documentación; revisión antes de merge.

## Fase 6 — Implementar punto y palabra temporal

- **Título del Issue:** Implementar punto y palabra temporal.
- **Estado:** PENDIENTE; sin responsable; Issue remoto: https://github.com/Samge34c/PING/issues/7.
- **Objetivo:** Mostrar punto público y palabra/símbolo tras aceptación.
- **Dependencias:** Fase 5; PEND-004.
- **Resultado esperado:** Palabra por interés común o catálogo neutral, con riesgo de colisión reducido.
- **Criterio de terminado:** Solo match aceptado revela encuentro; validación, caducidad y colisiones probadas según decisión.
- **Entrega:** PR vinculada, pruebas pertinentes y documentación; revisión antes de merge.

## Fase 7 — Implementar catálogo de actividades

- **Título del Issue:** Implementar catálogo de actividades.
- **Estado:** PENDIENTE; sin responsable; Issue remoto: https://github.com/Samge34c/PING/issues/8.
- **Objetivo:** Ofrecer actividad breve tras match y alternativa del anfitrión cuando corresponda.
- **Dependencias:** Fase 6; PEND-005.
- **Resultado esperado:** Catálogo predefinido relacionado con interés o evento.
- **Criterio de terminado:** Flujo normal y alternativa verificables; actividad no reemplaza matching y no usa IA generativa.
- **Entrega:** PR vinculada, pruebas pertinentes y documentación; revisión antes de merge.

## Fase 8 — Implementar confirmación

- **Título del Issue:** Implementar confirmación.
- **Estado:** PENDIENTE; sin responsable; Issue remoto: https://github.com/Samge34c/PING/issues/9.
- **Objetivo:** Confirmación bilateral tras interacción.
- **Dependencias:** Fase 7; PEND-005.
- **Resultado esperado:** Match completado tras confirmación de ambos.
- **Criterio de terminado:** Una confirmación no completa; doble confirmación completa; reintentos no duplican la finalización.
- **Entrega:** PR vinculada, pruebas pertinentes y documentación; revisión antes de merge.

## Fase 9 — Implementar puntos

- **Título del Issue:** Implementar puntos.
- **Estado:** PENDIENTE; sin responsable; Issue remoto: https://github.com/Samge34c/PING/issues/10.
- **Objetivo:** Acreditar +10 por conexión/actividad completada y equivalente del anfitrión; canje básico solo si se decide.
- **Dependencias:** Fase 8; PEND-005 y PEND-008.
- **Resultado esperado:** Saldo visible no negativo; entrega opcional ENTREGADO/NO_ENTREGADO/INCIDENCIA.
- **Criterio de terminado:** Pruebas de acreditación única, alternativa y saldo; si hay canje, probar costo/entrega y reversión definida.
- **Entrega:** PR vinculada, pruebas pertinentes y documentación; revisión antes de merge.

## Fase 10 — Implementar feedback

- **Título del Issue:** Implementar feedback.
- **Estado:** PENDIENTE; sin responsable; Issue remoto: https://github.com/Samge34c/PING/issues/11.
- **Objetivo:** Opinión opcional posterior a completar interacción.
- **Dependencias:** Fase 8.
- **Resultado esperado:** Opciones 👍 útil/agradable, 😐 neutral, 👎 no me sirvió.
- **Criterio de terminado:** Omitir feedback permite continuar; registrarlo no cambia puntos.
- **Entrega:** PR vinculada, pruebas pertinentes y documentación; revisión antes de merge.

## Fase 11 — Implementar panel admin

- **Título del Issue:** Implementar panel admin.
- **Estado:** PENDIENTE; sin responsable; Issue remoto: https://github.com/Samge34c/PING/issues/12.
- **Objetivo:** Gestión y métricas sencillas; bloqueo/reporte básico.
- **Dependencias:** Fases 1–10; PEND-007.
- **Resultado esperado:** Panel administrativo y estadísticas del evento.
- **Criterio de terminado:** Datos consistentes con interacciones; acceso autorizado y bloqueos/reporte probados según decisión.
- **Entrega:** PR vinculada, pruebas pertinentes y documentación; revisión antes de merge.

## Fase 12 — Pruebas de flujo completo

- **Título del Issue:** Pruebas de flujo completo.
- **Estado:** PENDIENTE; sin responsable; Issue remoto: https://github.com/Samge34c/PING/issues/13.
- **Objetivo:** Validar flujo, alternativa, errores, cancelación y concurrencia.
- **Dependencias:** Fases 0–11; pendientes funcionales resueltos.
- **Resultado esperado:** Suite integral y evidencia de resultados reales.
- **Criterio de terminado:** Evento a puntos/feedback/admin probado; pausa/cierre, bloqueos y un match activo respetados; fallos documentados.
- **Entrega:** PR vinculada, pruebas pertinentes y documentación; revisión antes de merge.

## Fase 13 — Preparar demo final

- **Título del Issue:** Preparar demo final.
- **Estado:** PENDIENTE; sin responsable; Issue remoto: https://github.com/Samge34c/PING/issues/14.
- **Objetivo:** Preparar guion reproducible para profesor.
- **Dependencias:** Fase 12.
- **Resultado esperado:** Guion del PREDEV §16 y datos de demostración sin información personal real.
- **Criterio de terminado:** Demostración de extremo a extremo reproducible, limitaciones explícitas y comandos de ejecución documentados.
- **Entrega:** PR vinculada, pruebas pertinentes y documentación; revisión antes de merge.
