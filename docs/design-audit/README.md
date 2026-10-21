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
| A · Táctil, foco y navegación | **Implementado** | DIAGNOSTICO §7 |
| B · Comprobante de canje | **Implementado**; entregado/incidencia sin capturar | §8 |
| C · Identidad e Intereses (F02) | **Integrado en la app** (DEC-055: orden de Samuel; Julián sin aprobar) | §11 |
| Navegación coherente (F05, F13) | **Implementado** | §11 |
| Recuperación junto a la acción (F07) | **Implementado** | §11 |
| D · Estados de paso del encuentro (F08) | **Propuesto** (DEC-032, sin defecto) | §4 |
| E · Avatares coherentes (F09) | **Implementado** | §9 |
| F · Organización (F10, F11) | **Implementado en parte** | §10 |
| Sin bloque: F14–F18 | **Propuesto** | §4 |

Todos los cambios implementados son de **presentación e interacción** (CSS, JS y plantillas). Backend, datos, migraciones, rutas, formularios, CSRF y permisos no cambian. Evidencia: `evidencia/` (A, B, E, F, I, N, R, P).

## Verificación (resumen)

- Capturas reales de viewport a 390×844 y 360×740 (oscuro y claro según el bloque) y 1440 en escritorio; texto ampliado **simulado** ×1,5 y ×2 (no es el ajuste real de un teléfono).
- Mediciones propias: sin desbordes horizontales, sin fallos de contraste sobre fondos sólidos, sin controles bajo 44 px de alto en las pantallas de participante medidas; quedan excepciones puntuales (enlace «Plano» de 41 px de ancho en `/organizer`, «Mi cuenta» de 24 px en escritorio, pestañas de configuración de 43 px).
- Pruebas Java **solo las pertinentes** a cada bloque (`FinalMvpTests`, `DemoImprovementTests`, `PagePresentationTests`, `VerificationTests`, `RewardTests`, `ParticipantTests`, `EventEntryFlowTests`, `EventTests`, `InterestCategoryAdminTests`, `FloorplanTests`), todas verdes. **No se ejecutó la suite completa** ni se repitió sin cambios; no se añadieron pruebas de clases CSS.
- MySQL local 8.0.46 (el proyecto apunta a 8.4.11).

## Límites que siguen abiertos

Estados no alcanzados: «sin candidatos», consulta y entrega del canje por el organizador (recibos entregados o con incidencia), plano con imagen, envío de bloqueo o reporte, lector de QR con cámara, correos de verificación, «Enviando…/Está tardando» (solo revisados en el código). No se probó en iPhone ni Android reales (teclado, áreas seguras), ni con lectores de pantalla, ni con personas. Las 15 publicaciones sociales siguen sin poder abrirse; Apple HIG y productos de eventos no se inspeccionaron (REFERENCIAS.md). En la navegación inferior, a texto ×1,5 en 360 px se recortan con «…» «Conectar» y «Beneficios» (el nombre accesible completo se conserva).

## Preguntas materiales (para decidir; copiables a ChatGPT)

**Q1 · Máximo de intereses (pendiente independiente).** `ESPECIFICACION.md` pide conservar «el máximo actual», pero el código **no impone ninguno**; la integración de Intereses no introduce límite. *Decisión:* ¿existe un máximo que deba aplicarse (regla nueva, con DEC y pruebas) o se corrige la especificación? *Recomendación:* corregir el texto de la especificación y no inventar un número. *Si se aplaza:* el selector sigue sin límite y muestra el contador real.

**Q2 · Aceptación de Julián.** El alta (identidad e intereses) y la navegación cambian DEC-037/045; DEC-055 recoge solo la autorización de Samuel. *Recomendación:* revisión conjunta de `evidencia/I*` y `P*` antes del merge.

**Q3 · Avatares demo distintos** (dato, no presentación): los participantes demo comparten el gato violeta. Sigue fuera de alcance.

**Q4 · Navegación del organizador.** `/organizer` y la jerarquía de gustos no usan la barra inferior unificada. *Alternativa:* incluirlos (cambio pequeño) o dejarlos como pantallas de administración. *Recomendación:* incluir `/organizer` si el organizador opera desde móvil.

## Continuidad para el siguiente agente

1. Comprobar el remoto: `git ls-remote origin claude/sweet-ritchie-ja97c7` debe coincidir con el último SHA de la rama; base funcional `ci/development-validation` @ `f2923b9`.
2. Siguiente trabajo razonable: pruebas en **iPhone y Android reales** (teclado, áreas seguras; decidir si añadir `viewport-fit=cover`), encuentro ACTIVE con los nuevos avisos de recuperación, `/organizer` con la barra unificada (Q4), F14–F16.
3. Intereses: el inicio mide 941 px hasta el botón (antes 834). Si se quiere recortar, reducir el tamaño de los mosaicos o decidir un dock fijo (otra decisión; DEC-045 mantiene la acción tras el catálogo).
4. No editar `.claude/skills/` ni actualizar revisiones fijadas; el descubrimiento de skills en sesión nueva sigue **pendiente de verificar** (`docs/SKILLS_PING.md`).
5. Aprendizajes técnicos: en `layout.html` no definir elementos `<nav>` fuera del fragmento `nav` (el selector `layout :: nav` los reutiliza; las barras nuevas viven en `fragments/navigation.html`); los mapas `Map.of` del servidor no tienen orden estable.
