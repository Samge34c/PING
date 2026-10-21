# Auditoría visual integral de PING — índice y continuidad

Rama `claude/sweet-ritchie-ja97c7` sobre `ci/development-validation` @ `f2923b9`. Sin merge, sin force-push, sin cambios en `main`. **Es una revisión de un agente con Chromium emulado, no una prueba con personas; no certifica accesibilidad ni producción.** Decisión registrada: DEC-054 (sin aprobación atribuida a Samuel ni a Julián).

## Qué hay aquí

| Documento | Contenido |
|---|---|
| [DIAGNOSTICO.md](DIAGNOSTICO.md) | Cobertura, revisión de primer uso, hallazgos F01–F18 con prioridad y tipo (defecto/mejora), mediciones y, por bloque, qué cambió, qué se comprobó y qué no |
| [REFERENCIAS.md](REFERENCIAS.md) | 10 referencias con fuente primaria, patrón aprovechado, qué se descartó y por qué; fuentes no accesibles |
| `evidencia/` | Hojas de contacto del recorrido real (01–17) y capturas antes/después de los bloques (A1–A6, B1–B2, E1, F1) |
| [../design-proposals/identidad-intereses-movil/](../design-proposals/identidad-intereses-movil/) | Prototipo aislado de Identidad e Intereses y `HANDOFF_CODEX.md` |

## Estado por bloque

| Bloque | Estado | Dónde |
|---|---|---|
| A · Táctil, foco y navegación (F03 F04 F06 F12; F05 parcial) | **Implementado y comprobado** | DIAGNOSTICO §7 |
| B · Comprobante de canje (F01) | **Implementado y comprobado**; estados entregado/incidencia sin capturar | §8 |
| C · Alta: identidad e intereses (F02) | **Propuesto**, no integrado: cambia la presentación de DEC-037/045 y exige aceptación de Samuel y Julián. Handoff para Codex listo | prototipo + HANDOFF_CODEX.md |
| D · Estados de paso del encuentro (F08) | **Propuesto**: pantalla aprobada (DEC-032), sin defecto que lo justifique | §4 |
| E · Avatares coherentes (F09) | **Implementado y comprobado** (Hub, lista, perfil) | §9 |
| F · Organización (F10, F11) | **Implementado en parte** | §10 |
| Sin bloque: F07 F13 F14–F18 | **Propuesto** | §4 |

Todos los cambios implementados son de **presentación**: CSS, una línea de `ui.js` (qué ítem de la navegación se marca en el comprobante) y plantillas (clases, orden de secciones, fragmento de avatar ya existente y textos). Rutas, formularios, campos, CSRF, controladores, datos y migraciones no se tocaron.

## Verificación (resumen)

- Capturas reales de viewport a 390×844 y 360×740 (oscuro y claro según el bloque) y 1440 en escritorio; texto ampliado **simulado** ×1,5 y ×2 (no es el ajuste real de un teléfono).
- Mediciones propias: sin desbordes horizontales, sin fallos de contraste sobre fondos sólidos, sin controles bajo 44 px de alto en las pantallas de participante medidas; quedan excepciones puntuales (enlace «Plano» de 41 px de ancho en `/organizer`, «Mi cuenta» de 24 px en escritorio, pestañas de configuración de 43 px).
- Pruebas Java **solo las pertinentes** a cada bloque (`FinalMvpTests`, `DemoImprovementTests`, `PagePresentationTests`, `VerificationTests`, `RewardTests`, `ParticipantTests`, `EventEntryFlowTests`, `EventTests`, `InterestCategoryAdminTests`, `FloorplanTests`), todas verdes. **No se ejecutó la suite completa** ni se repitió sin cambios; no se añadieron pruebas de clases CSS.
- MySQL local 8.0.46 (el proyecto apunta a 8.4.11).

## Límites que siguen abiertos

Estados no alcanzados: «sin candidatos», consulta y entrega del canje por el organizador (recibos entregados o con incidencia), plano con imagen, envío de bloqueo o reporte, lector de QR con cámara, correos de verificación, «Enviando…/Está tardando» (solo revisados en el código). No se probó en iPhone ni Android reales (teclado, áreas seguras), ni con lectores de pantalla, ni con personas. Las 15 publicaciones sociales siguen sin poder abrirse; Apple HIG y productos de eventos no se inspeccionaron (REFERENCIAS.md). En la navegación inferior, a texto ×1,5 en 360 px se recortan con «…» «Conectar» y «Beneficios» (el nombre accesible completo se conserva).

## Preguntas materiales (para decidir; copiables a ChatGPT)

**Q1 · Integrar Identidad + Intereses (bloque C) y el máximo de intereses**
- *Contexto y evidencia:* el botón para continuar queda a 1311 px en la identidad y el catálogo muestra 11 familias iguales (DIAGNOSTICO F02; prototipo: 646 px y mosaico con bandeja). `ESPECIFICACION.md` pide conservar «el máximo actual» de intereses, pero el código base **no impone ninguno**.
- *Decisión necesaria:* ¿se acepta (Samuel y Julián) cambiar la presentación de DEC-037/045 y se confirma que no hay máximo?
- *Alternativas:* (a) integrar el prototipo tal cual; (b) integrar solo la identidad compacta y dejar intereses como está; (c) no integrar.
- *Recomendación:* (b) como primer paso, porque no toca el catálogo y gana ~650 px; luego (a) con revisión sobre dispositivo.
- *Si se aplaza:* el alta sigue pidiendo ~1,5 pantallas de desplazamiento antes de continuar.

**Q2 · Avatares distintos por defecto para participantes demo.** Samuel, Julián y Carlos comparten el gato violeta, por lo que el avatar coherente (bloque E) no los distingue. Es un dato de la demo, no de presentación. *Recomendación:* diversificar los datos de la demo en una tarea aparte; si se aplaza, la identidad visual sigue siendo coherente pero uniforme.

**Q3 · Navegación inferior para visitantes y en el Hub.** Un visitante sin cuenta ve Mi Ping/Conectar/Beneficios (F13); el Hub tiene otra navegación (F05). *Alternativas:* ocultarla sin sesión; unificar Hub y resto; dejarla. *Recomendación:* ocultarla sin sesión (cambio pequeño y reversible) y decidir la unificación con Samuel y Julián. *Si se aplaza:* los visitantes siguen llegando a «Entrar» desde botones que parecen destinos propios.

## Continuidad para el siguiente agente

1. Comprobar el remoto: `git ls-remote origin claude/sweet-ritchie-ja97c7` debe coincidir con el último SHA de la rama; la base funcional es `ci/development-validation` @ `f2923b9`.
2. Para integrar el bloque C: seguir `docs/design-proposals/identidad-intereses-movil/HANDOFF_CODEX.md` **solo tras** Q1 y su aceptación.
3. Siguiente trabajo independiente razonable (si se decide): F13 (ocultar navegación sin sesión), F07 (mensaje de sesión perdida junto a la acción bloqueada), F14–F16.
4. No editar `.claude/skills/` ni actualizar revisiones fijadas; el descubrimiento de skills en una sesión nueva sigue **pendiente de verificar** (`docs/SKILLS_PING.md`).
