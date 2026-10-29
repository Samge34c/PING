# Incremento integrado del evento — 29 octubre 2026

**Entrega posterior autorizada:** rama `feature/event-increment-delivery`, commit/push y PR en borrador hacia `deploy/render-demo`; mismo código comprobado, con documentación posterior. [Instrucciones exactas de prueba local aislada](PROBAR_ENTREGA.md). El estado «sin commit» de la evidencia inferior corresponde al momento de ejecutar las pruebas; el SHA de entrega lo fija la cabecera del PR. No se autoriza merge ni despliegue.

Seis bloques implementados sobre `perf/closure-current`, HEAD/base `3a528cab9312cdb314ac40252bd7ba3782afd0b0`, con todos los cambios **locales, sin commit**. Conserva el cierre de rendimiento y la entrada editorial. Autorización explícita de Samuel; no se atribuye aprobación a Julián.

## Versión comprobada

`clean verify` terminó el **29/10/2026 a las 16:44:32 Bogotá**, código de salida 0, `BUILD SUCCESS`: **369 pruebas, 0 fallos, 0 errores, 0 omitidas**. Incluye pruebas existentes de Google, acceso continuo, correo HTML, revocación/permisos, matching, llegada, verificaciones, Saga, canje e idempotencia. No es una suma de ejecuciones anteriores.

- [Evidencia y suites](evidence.json), [manifiesto de fuentes](source-manifest.json).
- SHA-256 del manifiesto: `3506fce38eb845a6144f9202220dde05912a400d7c2918ed9296577c207d21ce`.
- JAR `target/ping-0.0.1-SNAPSHOT.jar`, SHA-256 `3b74f151872292ce1e0e404d325d2e5e6e30a3f0f4cfb12cccd8b70eaabeabbb`.
- Los **514 recursos y 200 clases** del JAR coinciden con el árbol final compilado. Bootstrap regenerado: **33 352 bytes**, `--check` correcto. Original y licencias conservados.
- Log completo local: `C:\Users\river\PING-movil\target\increment-evidence\final-verify.log`. Las capturas posteriores no cambiaron código ni motivaron otra suite.

## Comportamiento y rutas

| Bloque | Revisión con una participación propia / permisos existentes |
|---|---|
| Pista visible | `/me`: formulario vacío directo, resumen guardado, Editar/Guardar/Cancelar y borrado guardando vacío. Propuesta/ACTIVE siguen delante y dominantes. |
| Preparación | `/organizer` → evento → `/events/{id}/preparation`: estado y accesos a código, puntos, actividades/puntos, plano, beneficios/entrega y opciones reales. No crea requisitos de apertura. |
| Ayuda | Enlace voluntario en Mi Ping/encuentro, alternativa `/me/help`; ayuda de código en `/join/event`. Configuración en `/events/{id}/experience-settings`. |
| Tarjeta propia | `/me/experience`: PNG local 1080×1350 y SVG autónomo sin JS. Avatar V3/antiguo, alias/evento reales, conexiones COMPLETED y **saldo actual**, hasta tres gustos propios. |
| Agenda general | `/me/agenda`; edición `/events/{id}/agenda`. Zona IANA por actividad, orden real y punto existente. Independiente de la actividad del encuentro. |
| Reto voluntario | `/me/challenges`; activación en opciones del evento. Dos conexiones COMPLETED propias, sin puntos/recompensas adicionales ni ranking. |

Pista: límite vigente de 120 puntos de código Unicode, CSRF y revisión; valor persistido separado del borrador para Cancelar. El polling respeta escritura/guardado. La pista ajena sigue restringida al ACTIVE autorizado; no se exporta ni aparece en estado/historial/tarjeta/correo. La tarjeta usa una lista explícita de datos propios y no incluye otras personas, códigos, pistas, correo, recibos, QR, ubicaciones o IDs internos.

## Comprobaciones reales y capturas

[Índice móvil primero](../../screenshots/event-increment/README.md). Se examinaron 390 px, 360 px/tema alternativo y escritorio 1280; texto ampliado, foco, controles táctiles, navegación y altura adaptable. Se corrigieron relleno de pista, área de Editar, división de palabras del nombre, pictograma inexistente y foco inicial de ayuda. La acción principal se entiende y se alcanza; la pista precede a gustos sin desplazar la propuesta. La navegación conserva espacio al desplazar; se mantienen marca y avatares Festival Social. La hoja de ayuda empieza por su título y permite Escape/retorno al disparador. Sin animación nueva; se conserva el tratamiento existente de movimiento reducido.

El ensayo inicial completó dos encuentros reales en H2 con cuentas nuevas verificadas por correo MIME local: aceptaciones, ambas llegadas, códigos personales distintos, verificación bilateral, confirmaciones y reintentos. Canje de 5 y entrega por otra cuenta autorizada, sin débito doble. Tras la segunda conexión, el propietario tenía 2 completadas y saldo actual 15, sin recompensa del reto. La tarjeta con cero también se descargó realmente y es válida. Se comprobó fallo recuperable de generación PNG, formulario de pista sin JS, borrador tras 400/409, Cancelar y borrado; agenda con Ahora/Terminada/Próxima, texto largo, punto autorizado y ausencia de duplicados al reintentar.

Los JSON de recorridos conservan su estado original: `journey-initial` terminó en un selector auxiliar que eligió un botón oculto; sus comprobaciones previas son válidas. `remaining-initial` se detuvo al configurar la descarga automatizada. `downloads-review` corrigió el contexto y la ruta Windows del auxiliar y confirmó el archivo real. `states-before-h2-closure` validó formulario nativo y agenda antes de que una nueva búsqueda fallara en H2; `priority-stale-h2` registra ese fallo. No se presentan como recorridos íntegros exitosos. [Prioridad en base fresca](priority-fresh.json), [ajustes finales](post-visual.json) y [estado obsoleto final](state-detail.json) terminaron correctamente, sin excepciones JS. No se repitieron los tramos ya válidos.

El error H2 `23514 → 90098` se reprodujo fuera de PING con una tabla CHECK/IN al cerrar su primera conexión (auxiliar local `H2ConstraintProbe.java`). El ensayo llevaba horas abierto y había registrado pausas largas del equipo; la restricción retenía una sesión cerrada. Se repitió solo la prioridad pendiente en una base nueva con conexiones locales sin caducidad. No se cambió la migración antigua, matching ni configuración del producto para resolver este incidente del entorno.

## Persistencia, rendimiento y límites

`V24__event_experience_extras.sql` añade **dos tablas opcionales**, configuración complementaria y agenda. No altera filas/columnas anteriores ni requiere completar eventos antiguos. Fechas de agenda como milisegundos UTC y zona guardada por fila; rechaza horas inexistentes/ambiguas y puntos ajenos. Escritura bajo bloqueo del evento, revisión e idempotencia por petición. El reto es solo lectura sobre COMPLETED y saldo; no escribe progreso ni créditos. Se verificaron aislamiento de eventos y valores predeterminados sin configuración. Migraciones V1–V24 comprobadas en H2; no se ejecutó nada en producción/MySQL/Aiven.

El [antes/después de consultas](../../performance/closure/README.md) sigue referido a escenarios equivalentes del cierre de rendimiento: Conectar OFFERED 9→3, ACTIVE 10→3, sin encuentro 9→5; Beneficios 10→7; ACTIVE 20→14. La suite final incluye esas pruebas. No se extrapolan segundos de Render ni se compara una pantalla nueva con una anterior como si hicieran el mismo trabajo.

[Prueba reproducible de parches visuales](visual-patch-proof.json): ambos aplican en copias temporales de sus bases; proyección visual igual a los archivos actuales y las tres WebP idénticas. Excluye la ayuda y CSS del incremento. **No aplicar sobre este worktree**: la entrega es el código actual completo, no un parche histórico. El handoff original de Claude y su copia principal permanecen intactos.

Pendiente externo: iPhone/Safari físico (teclado, foco, descarga/guardar imagen y cámara), clientes reales de correo y consentimiento Google; medición autenticada en Render después de un despliegue que requiere otro encargo. Sin servicios de pago, publicación automática, producción, commit, push, merge ni despliegue. [Continuidad y siguiente paso para Claude](../../handoffs/INCREMENTO_EVENTO.md).
