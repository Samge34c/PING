# Handoff para Codex · Identidad e Intereses

Base verificada: `ci/development-validation` @ `f2923b98ff893c22138ef6552859e5a323ffa7c3`. Antes de empezar, comprobar el remoto; no partir de `main`. **No integrar sin** Issue, entrada en `docs/DECISION_LOG.md` y aceptación de Samuel y Julián (AGENTS.md). El prototipo no sustituye a los controladores ni a las plantillas.

## 1. Alcance

Rediseño solo de presentación de dos pantallas del alta (`/join` paso alias/avatar y `/join/interests`). **Sin** cambios de backend, permisos, migraciones, reglas, esquema de formularios ni datos. Hub, ACTIVE y las demás pantallas se conservan.

## 2. Archivos previstos

| Archivo | Cambio |
|---|---|
| `templates/entry/details.html` | Gafete (evento + avatar + alias), progreso, orden del botón |
| `templates/entry/avatar.html` (fragmento `picker`) | Acordeones → pestañas con opciones visuales. **Compartido** con `entry/avatar-edit.html` (`/me/avatar`): revisar ambas |
| `templates/entry/interests.html` | Cabecera compacta, progreso, bandeja «Elegidos» |
| `templates/interests/selector.html` | Fragmento **compartido** con `interests/form.html` (editor de intereses) y con `ui.js` (editor inline): el cambio debe seguir funcionando allí |
| `static/js/avatar.js`, `static/js/interest-selector.js` | Pestañas; contadores por familia, bandeja, cabecera de familia |
| `static/css/brand.css` | Sección nueva y acotada (propuesta: `.id-*` e `.in-*`), sin tocar `.pe-*`/`.pi-*` usados por otras pantallas |

Tests existentes que tocan estas pantallas y deben seguir pasando: `ParticipantEntryTests`, `EventEntryFlowTests`, `InterestTests`, `InterestCategoryAdminTests`, `PagePresentationTests`. Si solo cambian plantillas/CSS/JS, ejecutar esos y no la suite completa; AGENTS.md prohíbe tests de clases CSS o estructura exacta.

## 3. Contratos que no pueden cambiar (verificados en el código)

**Alias/avatar** (`POST /join`): campos ocultos `_csrf`, `draftKey`, `revision`, `eventId`; `alias` (`th:field`, obligatorio, `maxlength=80`, `autocomplete=nickname`, `aria-describedby="alias-help alias-error"`, `aria-invalid` según `#fields.hasErrors`, `th:errors`); radios `animal`, `hat`, `accessory`, `background` con valores de las listas permitidas (`avatarAnimals/Hats/Accessories/Colors`); botones `name=action` con `interests` (deshabilitado si `!entryEventOpen`) y `changeEvent` (`formnovalidate`). Avisos: `entryError` (`role=alert`, `tabindex=-1`, `data-error-summary`) y el de evento sin entradas. Etiquetas reales de estado: `EventStatus` = Borrador/Abierto/Pausado/Cerrado. Correo privado / alias público: conservar el texto.

**Intereses** (`POST /join/interests`): `_csrf`, `draftKey`, `revision`; casillas `name=interestIds` con el ID real; `action=complete|back`. Hooks del JS actual: `data-interest-editor`, `data-interest-selector`, `data-selection-summary`, `data-selected-count` (`role=status aria-live=polite`, texto singular/plural), `data-filter-controls`, `data-interest-filter` (oculto en modo taxonomía), `data-interest-search` (solo si `interestOptionCount > 12`), `data-interest-empty`, `data-clear-filter`, `data-taxonomy` con `data-node-key/-parent/-label`, `data-interest-name`, `data-interest-nodes`, `form.dataset.categoriesReady` y el evento `ping:interest-editor-ready`. Grupo `retained` («Conservados») y su texto condicional por `selectionContext`. Ocultar filas nunca debe desmarcarlas ni deshabilitarlas. Selecciones por ID explícito, sin heredar por ancestros.

## 4. Estructura de la propuesta (qué copiar)

- **Gafete:** banda ciruela fija (`#251333`) con evento y estado, avatar 96 px y alias en el mismo bloque; el texto de ayuda se mantiene.
- **Avatar:** `role=tablist` Animal/Sombrero/Accesorio/Color (cada pestaña muestra su valor actual), un panel visible. Las opciones de sombrero y accesorio se muestran **compuestas sobre el animal elegido** (mismas capas animal → accesorio → sombrero). Radios nativos dentro de cada panel; flechas/Home/End en la pestaña.
- **Intereses:** progreso de 3 segmentos con texto visible; bandeja «Elegidos» (3 chips + «+N más» que despliega todos, cada chip quita su selección); buscador; mosaico de 11 mundos (icono local, nombre, «N opciones», contador de elegidos en amarillo); vista de familia con botón «Mundos», subgrupos como chips con scroll horizontal y chips de gustos con marca circular.
- **Escritorio (≥900 px):** gafete a la izquierda y editor a la derecha; catálogo a la izquierda y bandeja + acción a la derecha, sin sticky que tape contenido.
- **Foco:** al abrir una familia, foco al título; al volver, al mosaico que la abrió; al cambiar de pantalla, al `h1`.

## 5. Recursos, estados y rendimiento

**Recursos:** solo locales y existentes: Manrope, SVG de avatares y `icons.svg` (ids `calendar`, `arrow`, `check`, `close`, `heart`, `game`, `music`, `film`, `spark`, `code`, `users`, `camera`, `ball`, `smile`, `bolt`). Mapeo familia→icono por clave de nodo (`games`, `music`, `screen`, `anime`, `tech`, `tabletop`, `creative`, `active`, `everyday`, `platforms`, `legacy`); clave desconocida → `heart`. Sin imágenes, CDN ni dependencias.

**Estados a cubrir:** identidad — normal, alias inválido, evento sin entradas, `entryError`; intereses — sin selección, con borrador, «Conservados», `entryError`, búsqueda sin resultados, catálogo vacío («puedes continuar sin elegir»).

**Rendimiento:** el prototipo compone ≈55 `<img>` de avatar (todos los paneles). En la app, componer las opciones de una pestaña **al activarla** (o con CSS) y no enviar capas que no se ven; los 15 SVG distintos se cachean. Movimiento: 120 ms, `scale(.97)`, `cubic-bezier(.23,1,.32,1)` (DEC-048), `prefers-reduced-motion` sin transiciones; hover solo con `(hover:hover) and (pointer:fine)`; campos ≥16 px; controles ≥44 px; sin `blur`.

## 6. Decisiones abiertas (humanas) y desviaciones

1. **Máximo de intereses.** `ESPECIFICACION.md` pide conservar «el máximo actual», pero el código base **no impone ninguno**. El prototipo no muestra límite. Confirmar si existe.
2. **Cambia DEC-037/DEC-045 (presentación):** filas de dos columnas → chips; sin icono repetido por interés. La acción sigue **en el flujo después del catálogo** (se respeta DEC-045); un dock fijo sería otra decisión.
3. **Colores de mosaico.** Usa ciruela `#251333`, morado de marca `#6A298F` y lila `#9B79D2`; ciruela y lila son tokens «solo Hub» según DESIGN.md. El amarillo queda reservado a la acción y a los contadores. Decidir si se amplía su uso.
4. **Buscador.** El prototipo oculta visualmente la etiqueta y usa `placeholder="Buscar un interés"` para ahorrar altura; conservar `<label>` accesible (`sr-only`) o volver a la etiqueta visible.
5. **Orden de familias:** hoy alfabético por nombre; el tono de cada mosaico es decorativo (`nth-child`), no semántico. Decidir si habrá orden editorial.
6. **Acceso tras QR:** la pantalla de acceso pierde el paso y el contexto del evento (problema 5 del README); no incluido.
7. Copy ilustrativo de errores: usar `th:errors` y `entryError` reales.

## 7. Comprobaciones antes de dar por buena la integración

Capturas reales a 390 (principal), 360 y 1440, oscuro (a fondo) y claro; los estados de §5; teclado (pestañas con flechas, chips con espacio, foco visible); movimiento reducido; envío real de `/join` y `/join/interests` conservando borrador y errores; selección conservada al buscar/volver; editor inline y `/me/avatar` sin regresión; **iPhone/Android reales** (teclado y áreas seguras), que este prototipo no verificó. Registrar en la continuidad única qué se comprobó y qué no.
