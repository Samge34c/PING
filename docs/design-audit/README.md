# Revisión visual de PING — estado vigente y evidencia

**DEC-056,21 octubre2026.** Rama `fix/mobile-visual-finish` desde la base comprobada de Claude `3755c129cee95f6ece533b52a226c21969c6a52e`, sin avances posteriores al recuperar. [Issue51](https://github.com/Samge34c/PING/issues/51), [PR52 en borrador](https://github.com/Samge34c/PING/pull/52) hacia `claude/sweet-ritchie-ja97c7`. Autorización explícita de Samuel, sin atribuir aprobación a Julián. Sin merge, force-push ni cambios en main.

Es evidencia de un agente con Chromium táctil emulado y datos ficticios aislados, no pruebas con personas, teléfonos físicos, Safari, cámara o certificación de accesibilidad/producción. La auditoría original DEC-054 y la integración DEC-055 se conservan como historia en el diagnóstico.

## Documentos

| Documento | Contenido |
|---|---|
| [DIAGNOSTICO.md](DIAGNOSTICO.md) | Hallazgos y checkpoints históricos (§1–11); estado, mediciones y comprobaciones actuales DEC-056 (§12) |
| [REFERENCIAS.md](REFERENCIAS.md) | Fuentes consultadas por Claude y adaptación vigente; no se atribuyen nuevas consultas a esta tarea |
| [evidencia/DEC-056/](evidencia/DEC-056/) | Capturas individuales reales antes/después y de los estados comprobados; códigos personales/QR enmascarados |
| [Continuidad única](../ENCUENTRO_CONFIGURABLE_LLEGADA.md) | SHA remotos, servicios de ensayo, respaldos y siguiente acción |
| [Prototipo e instrucciones anteriores](../design-proposals/identidad-intereses-movil/) | Referencia histórica integrada en la app por DEC-055, no producto alternativo |

## Estado actual por bloque

| Bloque | Estado vigente | Evidencia / límite |
|---|---|---|
| Alta e identidad compacta | **Integrado y comprobado** | Alta real con avatar; formularios sin JS y retorno con borrador conservado. No teclado físico ni áreas seguras reales |
| Intereses compartidos | **Corregido y comprobado** | Botón831,45–879,45px a390, frente a944,39–992,39; directorio con iconos/contadores;3 y24 elegidos, expansión/retirada,23 IDs conservados al volver; inline y Conservados. Sin máximo nuevo |
| Avatar: etiquetas y pestañas | **Corregido y comprobado** | Amarillo y su marca juntos; pestañas redistribuyen con texto×2 en edición y alta compartidas. Datos/radios completos |
| Hub y navegación con texto ampliado | **Corregido y comprobado** | Alias/título/acciones cambian de distribución; caso largo sin palabras partidas (328px a360/×2), comparación final del Hub con mismo saldo17/estado en ambas versiones. Las etiquetas inferiores ya no se recortan con puntos suspensivos (DEC-055), no es una limitación actual |
| Recuperación en ACTIVE | **Corregido y comprobado** | Fallo de sondeo simulado conserva borrador y reintenta; cambio ajeno no recarga mientras se escribe; logout real/401 retira pista, códigos/QR y credencial escrita. Cámara real no comprobada |
| Comprobantes | **Comprobado** | Pendiente, entregado e incidencia por recorridos reales; lugar guardado visible. Sin ubicación: fixture histórica NUEVA, solo prueba de presentación, no tercer canje operativo |
| Plano con imagen | **Comprobado** | Imagen ficticia cargada por el organizador; marcador, zoom, gesto nativo emulado, resize360, centrado/restablecer y retorno. Persistencia leída tras reinicios de la app; sin ensayo nuevo de backup |
| Avatares coherentes / organización | **Conservado** | DEC-054/055; no nuevo rediseño de administración ni datos demo |
| F08 estados de pasos, F14–F16 | **Propuestas fuera de este bloque** | No se presentan como funciones implementadas; navegación de /organizer sigue independiente |

## Verificación y límites

- App real en base NUEVA `ping_visual_finish_exclusive`, MySQL **8.4.11**; no se modificó la demo existente. La auditoría histórica de Claude usó8.0.46 y sigue identificada así.
- 390×844 y360×740; ambos temas; escritorio1440; texto×2 simulado y nombres largos reales. Botones/controles afectados conservan áreas táctiles; el contenido puede requerir scroll.
-11 pruebas Java dirigidas correctas y sintaxis JS/diff revisados; suite completa mediante CI real de la PR, una vez por ejecución, sin repetirla localmente. Resultados y enlaces en diagnóstico/PR.
- Recorrido nuevo: dos cuentas verificadas por SMTP loopback, aceptación/llegadas/verificación/confirmación bilaterales,37 puntos cada una; canje20→saldo17, renovación sin nuevo débito, una entrega y una incidencia autorizadas.
- No correos externos, cámaras, teléfonos físicos, lector de pantalla, evaluación con personas, pruebas nuevas de bloqueo/reporte ni ensayo de teclado/ventanas Windows. Las15 publicaciones sociales y productos de referencia siguen sin inspección directa; no se reintentaron por rutina.

## Pendientes independientes

- **Máximo de intereses:** la especificación histórica dice «máximo actual», pero el código no impone ninguno. Se conserva ese comportamiento por orden de Samuel; no se inventó un límite. Aclarar la redacción en una decisión futura.
- **Revisión humana:** la autorización de Samuel para DEC-055/056 no acredita aceptación de Julián. Revisar la PR antes de cualquier merge.
- **Avatares demo distintos:** dato fuera de alcance, sin cambios.
- **Organización:** posible integración de /organizer en la barra compartida, propuesta pendiente; no bloquea este acabado.

## Continuidad

Respaldos visual/privacidad contrastados: `f15c867` y `366ff5b`; Cierre2fec949 con CI verde; HEAD añade el ajuste de ancho intrínseco del alias largo/evidencia final. Consultar el SHA completo y los checks de [PR52](https://github.com/Samge34c/PING/pull/52). Siguiente prioridad: revisión en teléfono físico de texto ampliado, teclado, navegación y cámara/HTTPS con configuración propia, conservando la revisión humana antes del merge. No editar skills ni activar hooks para retomar.
