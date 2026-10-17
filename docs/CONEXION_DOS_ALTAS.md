# Dos participantes nuevos: conexión real y resultado visible

Instrucción de Samuel,16 de octubre2026 (Bogotá), DEC-039/Issue14 abierto. Base local/remota limpia `6afac1ea2771f2adebda57b1476576b39064c051`; rama propia `fix/participant-confirmation-feedback`, PR en borrador hacia `feature/participant-onboarding`. Conservar cadena anterior; sin merge/main/force-push.

## Qué se comprobó y corrigió

Dos sesiones independientes, **Luna del evento** y **Nico del evento**, entraron por `/join`, eligieron el mismo evento/interés y activaron disponibilidad mediante sus botones. Evento de ensayo abierto por EventService en H2 efímera8082; ningún participante precargado, perfil demo elegido, estado de match forzado o escritura en MySQL8080.

El recorrido inicial funcionó: la segunda persona descubrió la propuesta en Mi Ping por el polling existente, sin recargar, en5,17 s. Cada una aceptó; ambas recibieron punto, palabra y actividad idénticos. Primera confirmación:0 puntos por persona; segunda:10 por persona; reintentos mantuvieron10/10.

**Defecto observado:** después de la primera confirmación, el POST redirigía al inicio del encuentro. A390, scrollY0 y mensaje «Tú ya confirmaste…» en y1103, fuera de la vista y de la barra inferior (y772). El estado se guardaba, pero visualmente parecía no cambiar.

**Corrección mínima:** el mismo POST redirige al encabezado real `#am-confirm-title`; el destino es enfocable con tabindex=-1 en ACTIVE y COMPLETED. Se ven la espera unilateral o la finalización y sus acciones. Sin texto que altere cuándo confirmar, sin bloques/CSS/JS nuevos, sin modificar polling, rutas de aceptación, reglas, permisos, privacidad, CSRF, Saga, puntos o mapas.

## Evidencia posterior al cambio

Recompilación y nueva ejecución aislada con dos altas normales nuevas; mismo recorrido real, sin perfiles demo ni recargas manuales para descubrir/actualizar estados:

- Propuesta descubierta automáticamente por la segunda persona en5,32 s; abrir propuesta y aceptar funciona. Tras primera aceptación, la otra pantalla explica que falta su aceptación; ambas pasan a ACTIVE automáticamente.
- Ambos leen `Zona Gamer · Pabellón 18`, palabra `PIXEL-1` y la misma actividad real del catálogo. No se generó contenido o matching con IA.
- Primera confirmación: saldo0/0, mensaje de espera y confirmación ajena correctos. A390, encabezado y89,67, final del mensaje y237,17, por encima de barra y772, con foco nativo; no se forzó scroll para esa captura. A360, mensaje hasta y258,17; resultado sigue visible.
- Segunda confirmación:10/10, COMPLETED visible para ambos por actualización existente. Repetir confirmación propia/ajena tres veces mantiene10/10. A360 el segundo POST enfoca el resultado; lectura adicional de esa zona en claro y un escritorio confirma foco, acciones y ausencia de overflow, sin mutaciones.
- MySQL8080 y demo persistente sin escrituras ni reinicios. Solo se recargó la instancia efímera de prueba para cargar el cambio Java; ninguna prueba altera encuentros ajenos.

Java21/Maven Wrapper, settings/caché existentes: `-Dtest=MatchTests,FinalMvpTests verify` → **34 pruebas actuales (20+14),0 fallos/errores/omitidos, BUILD SUCCESS/JAR**. Se conserva la prueba HTTP de dos sesiones y se añaden aserciones del destino/fragmento en primera y segunda confirmación; no se añaden tests de clases CSS ni estructura decorativa. No se repiten suites históricas de alta o auditorías visuales generales.

Skills selectivas por lectura manual: ping-efficient-work (continuidad/alcance), systematic-debugging (reproducción → redirección sin fragmento → corrección mínima) y verification-before-completion (pruebas/capturas antes de respaldar). No hizo falta una investigación visual ni consulta UX; hooks Impeccable conservados desactivados, sin instalaciones/subagentes.

## Capturas y continuidad

[Propuesta recibida automáticamente](screenshots/two-new-participants/01-proposal.png) · [Misión compartida](screenshots/two-new-participants/02-active.png) · [Confirmación propia visible](screenshots/two-new-participants/03-confirmed-waiting.png) · [Finalización bilateral](screenshots/two-new-participants/04-completed.png) · [Resultado360 claro](screenshots/two-new-participants/05-result-360-light.png) · [Escritorio, solo resultado](screenshots/two-new-participants/06-result-desktop.png) · [Antes: resultado fuera de vista](screenshots/two-new-participants/before-confirmation.png).

La actualización requiere la página abierta/visible; no se añadieron notificaciones externas. Chromium headless390 con prueba enfocada360 y lectura de resultado en escritorio; Safari/dispositivos físicos pendientes. No se declara terminada toda la app ni certificación de accesibilidad.

Logs, trazas, capturas adicionales y cookies aisladas quedan fuera de Git en `/workspace/work/ping-new-pair-20261016/` (0700). Nunca versionar private-*.json. Las capturas solo contienen alias ficticios del ensayo. Commit/SHA remoto y PR se verifican al publicar. El respaldo antiguo no fue recuperado.
