# Propuesta visual aislada · Identidad e Intereses (móvil primero)

**Es un prototipo de diseño, no la aplicación.** No está integrado en PING, no toca backend, seguridad, migraciones, permisos, reglas ni datos, y no se carga desde ninguna plantilla. Última revisión: 21 de octubre de 2026 (cierre tras revisión externa).

Base: `ci/development-validation` @ `f2923b98ff893c22138ef6552859e5a323ffa7c3` (PR #50 en borrador; ninguna rama del remoto la supera). Rama de trabajo: `claude/sweet-ritchie-ja97c7`. No se tocó `main`.

## Abrir y editar

```bash
# desde la raíz del repositorio
python3 -m http.server 8099
# abrir http://127.0.0.1:8099/docs/design-proposals/identidad-intereses-movil/index.html
```

Probado solo así (Chromium). Abrir `index.html` con `file://` puede bloquear la fuente en otros navegadores. Parámetros útiles: `?screen=identity|interests&theme=light&state=…&family=games&q=pop&alias=…&tools=1`.

| Archivo | Qué es |
|---|---|
| `index.html` | Las dos pantallas y el sprite con los 17 iconos locales usados (copiados de `static/img/icons.svg`) |
| `proposal.css` | Estilos. Tokens `--ping-*` copiados de `brand.css` (mismos nombres y valores) |
| `proposal.js` | Lógica nativa, sin dependencias. Los comentarios `CONTRATO` marcan lo que la app ya hace |
| `catalog-data.js` | Catálogo **real** exportado de la base de prueba (Flyway V22): 11 familias, 264 intereses |
| `HANDOFF_CODEX.md` | Especificación de integración |
| `screenshots/` | `app-real/` (antes), `prototipo/` (después), `comparacion/`, `estados/`, `textos-largos/` |

Recursos reutilizados de la app por ruta relativa: Manrope, los SVG de avatar y el favicon. No hay recursos nuevos, framework, CDN ni librería.

## Barra exclusiva del prototipo

Arriba de la página hay una barra plegada («PROTOTIPO · No es la app real · controles», 47 px con su borde) que **no existe en PING**: sirve para cambiar de pantalla, simular estados y reiniciar. Para que no distorsione la altura:

- `*-viewport-con-barra.png` y `*-completa-con-barra.png` la muestran tal cual y la **enmarcan en amarillo con una nota** al margen. Son el viewport literal de 390×844 o 360×740 *incluida* la barra.
- `*-viewport-sin-barra.png` oculta la barra: es lo que ocuparía la app en ese viewport. Las comparaciones con la app real y todas las medidas usan esta versión.

## Qué es real y qué no

| | Origen |
|---|---|
| `screenshots/app-real/*` | **Captura real** de PING: build local de f2923b9 con MySQL **8.0.46** (el proyecto apunta a 8.4.11), emulación móvil de Chromium, escenario demo SOFA, cuenta ficticia, alias «Mara Lume» |
| `screenshots/prototipo/*`, `estados/*`, `textos-largos/*` | **Prototipo** HTML/CSS/JS. No demuestra permisos, persistencia ni envío |
| Catálogo (familias, gustos, recuento) | Real, de la base de prueba. Los ids **no** son un contrato |
| «SOFA 2026 · Abierto», «Mara Lume» | Datos de ejemplo del escenario demo |
| Estados simulados | Ilustran contratos existentes; el copy de los errores es ilustrativo (marcado) |
| Textos largos | Se probaron sustituyendo nombres desde el navegador (interceptando `index.html` y `catalog-data.js`); el prototipo no incluye ese modo |

## Qué se cambió en este cierre

| # | Petición | Cambio | Resultado medido |
|---|---|---|---|
| 1 | Más margen para la acción en Identidad | Avatar de 96 → 76 px; ayuda más corta; título 28 → 26 px; el nombre del grupo de pestañas pasa a solo lectores de pantalla; opciones de animal de 76 → 60 px con nombre en una línea; sin controles fijos | Acción de 790 → **646** px |
| 2 | Compactar Intereses | Etiqueta de búsqueda solo para lectores de pantalla; mosaico con icono al pie y contador junto al recuento; la bandeja «Elegidos» pasa a **una sola línea** y no se muestra vacía; «Puedes continuar sin elegir.» se mueve junto a la acción | Mundos 861 → **780** px; con 2 elegidos 937 → 858 |
| 3 | «Gustos generales» a ancho completo | **Era una regla mía** (`data-wide` para la clave `legacy`) para evitar una casilla huérfana al emparejar el mosaico; no refleja ninguna prioridad de producto. Eliminada: las 11 familias tienen el mismo tamaño y el orden alfabético de la base. Con 11 (impar) la última queda sola; se acepta ese hueco antes que sugerir jerarquía | — |
| 4 | Textos largos | Ver abajo | 80/80 comprobaciones |

### Textos largos

Probado a 390×844 y 360×740 con: alias de 79 caracteres con espacios y de 80 sin espacios (el máximo del campo); evento de 137 caracteres («Convención Internacional de Cultura Pop, …»); familias de 101 y 79 caracteres, un subgrupo de 77, un gusto de 78 y otro de una palabra de 63 letras sin espacios.

- Defectos hallados **en la revisión visual** (los chequeos automáticos no los veían) y corregidos: «3 de 3 · Afinidad» se rompía en una columna (`Afi/nid/ad`); el texto de ayuda repetía el nombre completo del evento; los chips de varias líneas se deformaban como píldoras; el «+N más» podía quedar fuera de la fila de la bandeja.
- Ahora: la etiqueta del paso baja a su propia línea; el encabezado limita alias+evento a 3 líneas con puntos suspensivos (el nombre completo sigue en la primera pantalla); los nombres largos se parten (`overflow-wrap`) y los controles crecen sin recortarse; en la bandeja plegada el chip largo se abrevia con «…» y el desplegado muestra el nombre completo; «+N más» queda fijo al borde derecho.
- Automático: sin desplazamiento horizontal de página, nada fuera del ancho visible y controles ≥44 px en 4 casos × 6 estados.

## Pantallas elegidas y por qué (capturas reales)

Recorrido revisado: código de evento → acceso → alias/avatar → intereses. Elegidas **Identidad (alias + avatar)** e **Intereses**. Acceso/registro funciona y es corto; su único defecto observado se anota abajo y no se rediseña.

| # | Observado (real, 390 px) | Efecto | Cambio propuesto |
|---|---|---|---|
| 1 | Alias/avatar: el botón principal queda a 1311 px; vista previa de 150 px, cuatro acordeones y Sombrero/Accesorio/Color solo con texto | Hay que desplazar ~1,5 pantallas para continuar; las opciones no se ven | Gafete con avatar + alias juntos y pestañas Animal/Sombrero/Accesorio/Color con opciones **visuales** que muestran el resultado |
| 2 | Intereses: 11 familias son botones de texto iguales; «Elegidos» (vacío) y buscador van antes que las familias | El catálogo de 264 gustos no transmite estructura ni progreso | Mosaico de mundos con icono local y contador por familia; subgrupos como chips; bandeja «Elegidos» compacta |
| 3 | Dentro de las familias nuevas (p. ej. Videojuegos → Competitivo) todos los gustos repiten el mismo icono de corazón junto a una casilla vacía | Ruido repetido, sin identidad | Chips con marca circular y estado marcado además del color; sin icono repetido |
| 4 | El contador y la acción quedan lejos tras elegir | Poca retroalimentación cerca del toque | Bandeja con quitar-por-chip y contador vivo; la acción sigue en el flujo (DEC-045) |
| 5 | Tras escanear el QR, el acceso aparece como «Tu cuenta PING» sin «paso» ni evento destacado (caja gris fuera de paleta) | Se pierde el contexto del recorrido | **No incluido.** Pendiente para otra iteración |

## Medido (Chromium con emulación móvil; px CSS desde el borde superior; sin la barra del prototipo)

| Botón principal: parte superior (inferior) | Antes (app real) | Después (prototipo) | Margen del «después» bajo el pliegue |
|---|---|---|---|
| Identidad 390×844 | 1311 (1359) | **646** (698) | 146 px libres |
| Identidad 360×740 | 1310 (1358) | **646** (698) | 42 px libres |
| Intereses, mundos vacío 390×844 | 834 (882) | **780** (832) | 12 px libres (cabe, al borde) |
| Intereses, mundos vacío 360×740 | 890 (938) | **817** (869) | **Requiere desplazar** ~129 px |
| Intereses, familia de 8 gustos y 2 elegidos 390 / 360 | ≈945 / ≈966* | **741 / 793** | 390: cabe; 360: **requiere desplazar** |
| Intereses, mundos con 2 elegidos 390 / 360 | — | 858 / 875 | Requiere desplazar |

\* Medido en una ejecución anterior del mismo método; entre ejecuciones hubo ±3 px.

Lo que **no** se resolvió: en Intereses a 360×740 la acción sigue exigiendo desplazamiento en la vista de mundos (el mosaico es el contenido de la tarea). Está ~73 px más arriba que en la app real, no visible sin scroll. No se midieron efectos en usuarios.

## Cambios hechos tras leer completas las skills

`emil-design-eng` **no se invocó**: la herramienta `Skill` la reserva al usuario (`/emil-design-eng`). Se leyó el archivo a petición expresa y solo se contrastó su lista de comprobación con mi CSS, sin ejecutar su flujo.

| Antes | Después | Por qué |
|---|---|---|
| `pop-in` por *keyframes* en cada chip de la bandeja (se repetía en todos al reconstruirla) y en marca de chip y contador de familia | Solo el chip **nuevo** anima (160 ms, ease-out); sin animación en marca ni contador | emil-design-eng: no usar *keyframes* en elementos que se disparan rápido; no animar lo que se repite a menudo. DEC-048 ya pedía «sin animación de finalización nueva» |
| Chips, mosaico y opciones sin `touch-action` ni `user-select` | `touch-action: manipulation` y `user-select: none` solo en controles (no en `body`) | mobile-native §5 y §8 |
| Pie sin área segura | `padding-bottom` con `env(safe-area-inset-bottom)` | mobile-native §7: el prototipo declara `viewport-fit=cover`; **la app real hoy no**, así que sin esa meta no tendría efecto (ver HANDOFF) |
| Cifras de altura de una ejecución anterior | Re-medidas al cierre y con la misma emulación que la app real | verification-before-completion: evidencia fresca antes de afirmar |

## Verificado y no verificado (evidencia de este cierre)

- **Funcional, 28/28** (Chromium, 390 px): alias vacío (error, `aria-invalid`, foco), pestañas con flechas/Home/End y `tabindex` itinerante, radios nativos con lista cerrada, 264 casillas `interestIds`, contador singular/plural, búsqueda que conserva la selección, quitar desde la bandeja, volver sin perder el borrador, controles ≥44 px, sin errores de consola, movimiento reducido sin transiciones, 360 px sin desplazamiento horizontal.
- **Textos largos, 80/80.** Revisión visual de 390 y 360 (oscuro y claro) y escritorio 1440.
- **No verificado:** iPhone o Android reales (teclado, áreas seguras, tacto), Safari y Firefox, lector de pantalla, pruebas con personas, integración con Thymeleaf, rendimiento en dispositivos modestos, MySQL 8.4. No se ejecutó la suite Java (no cambia código).

## Skills: qué se leyó y qué se aplicó

- **Leídas completas:** `ping-efficient-work`, `redesign-existing-projects`, `mobile-native`, `emil-design-eng`, `verification-before-completion`. **Solo frontmatter o fragmentos:** `impeccable`, `ui-ux-pro-max`, `systematic-debugging` (no se usaron).
- **Aplicadas de verdad:** los cambios de la tabla anterior (mobile-native, criterios de emil-design-eng) y la regla de verificación fresca. `redesign-existing-projects` se usó de forma selectiva y subordinada a PRODUCT.md (no se cambió tipografía ni se evitó el morado). La revisión visual fue mía; **no** se ejecutó Impeccable.
- **Descubrimiento de las skills en Claude Code: pendiente de verificación definitiva** (ver `docs/SKILLS_PING.md`, con pasos exactos).

## Continuidad

- **Base:** f2923b98ff893c22138ef6552859e5a323ffa7c3. **Rama:** `claude/sweet-ritchie-ja97c7`. El SHA publicado se comprueba con `git ls-remote origin refs/heads/claude/sweet-ritchie-ja97c7`.
- **Producido:** esta carpeta; `CLAUDE.md`; `.claude/skills/` (8 punteros); apartado de `docs/SKILLS_PING.md`; `docs/REFERENCIAS_VISUALES_Y_SKILLS.md`.
- **Pendiente humano:** aceptación de Samuel y Julián, registro en `docs/DECISION_LOG.md` y un Issue antes de integrar (AGENTS.md); decisiones abiertas en `HANDOFF_CODEX.md` §6; verificación de descubrimiento de skills en sesión nueva.
- **Siguiente acción:** revisar las comparaciones y decidir qué se integra; solo entonces, que Codex siga `HANDOFF_CODEX.md` en una rama propia.
