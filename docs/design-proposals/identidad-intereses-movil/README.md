# Propuesta visual aislada · Identidad e Intereses (móvil primero)

**Es un prototipo de diseño, no la aplicación.** No está integrado en PING, no toca backend, seguridad, migraciones, permisos, reglas ni datos, y no se carga desde ninguna plantilla. Fecha: 21 de octubre de 2026.

Base: `ci/development-validation` @ `f2923b98ff893c22138ef6552859e5a323ffa7c3` (PR #50 en borrador; ninguna rama del remoto la supera). Rama de trabajo: `claude/sweet-ritchie-ja97c7`. No se tocó `main`.

## Abrir y editar

```bash
# desde la raíz del repositorio
python3 -m http.server 8099
# abrir http://127.0.0.1:8099/docs/design-proposals/identidad-intereses-movil/index.html
```

Probado solo así (Chromium). Abrir `index.html` con `file://` puede bloquear la fuente en otros navegadores. La barra superior «PROTOTIPO» (plegada) permite cambiar de pantalla, simular estados y reiniciar. Parámetros útiles: `?screen=identity|interests&theme=light&state=…&family=games&q=pop&tools=1`.

| Archivo | Qué es |
|---|---|
| `index.html` | Las dos pantallas y el sprite con los 17 iconos locales usados (copiados de `static/img/icons.svg`) |
| `proposal.css` | Estilos. Tokens `--ping-*` copiados de `brand.css` (mismos nombres y valores) |
| `proposal.js` | Lógica nativa, sin dependencias. Comentarios `CONTRATO` marcan lo que la app ya hace |
| `catalog-data.js` | Catálogo **real** exportado de la base de prueba (Flyway V22): 11 familias, 264 intereses |
| `HANDOFF_CODEX.md` | Especificación de integración |
| `screenshots/` | `before/` = capturas reales de PING; `after/` y `estados/` = prototipo; dos composiciones antes/después |

Recursos reutilizados de la app por ruta relativa: Manrope, los SVG de avatar (`static/img/avatars/`) y el favicon. No hay recursos nuevos, framework, CDN ni librería.

## Qué es real y qué no

| | Origen |
|---|---|
| `screenshots/before/*` | **Captura real** de PING: build local de f2923b9 con MySQL **8.0.46** (el proyecto apunta a 8.4.11), escenario demo SOFA, cuenta ficticia, alias «Mara Lume» |
| `screenshots/after/*`, `estados/*` | **Prototipo** HTML/CSS/JS. No demuestra permisos, persistencia ni envío |
| Catálogo (nombres de intereses, familias, recuento) | Real, de la base de prueba. Los ids **no** son un contrato |
| «SOFA 2026 · Abierto», «Mara Lume» | Datos de ejemplo del escenario demo |
| Estados simulados | Ilustran contratos existentes; el copy de los errores es ilustrativo (marcado) |

## Pantallas elegidas y por qué (capturas reales)

Recorrido revisado: código de evento → acceso → alias/avatar → intereses. Elegidas **Identidad (alias + avatar)** e **Intereses**. Acceso/registro funciona y es corto; su único defecto observado se anota abajo y no se rediseña.

| # | Observado (real, 390 px) | Efecto | Cambio propuesto |
|---|---|---|---|
| 1 | Alias/avatar: el botón principal queda a 1311 px; vista previa de 150 px, cuatro acordeones y Sombrero/Accesorio/Color solo con texto | Hay que desplazar ~1,5 pantallas para continuar; las opciones no se ven | Gafete con avatar + alias juntos, pestañas Animal/Sombrero/Accesorio/Color con opciones **visuales** que muestran el resultado |
| 2 | Intereses: 11 familias son botones de texto iguales; «Elegidos» (vacío) y buscador van antes que las familias | El catálogo de 264 gustos no transmite estructura ni progreso | Mosaico de mundos con icono local y contador por familia; subgrupos como chips; bandeja «Elegidos» compacta |
| 3 | Intereses: dentro de las familias nuevas (p. ej. Videojuegos → Competitivo) todos los gustos repiten el mismo icono de corazón junto a una casilla vacía | Ruido repetido, sin identidad | Chips con marca circular y estado marcado además del color; sin icono repetido |
| 4 | El contador y la acción quedan lejos tras elegir (Elegidos arriba, botón abajo) | Poca retroalimentación cerca del toque | Bandeja con quitar-por-chip y contador vivo; acción sigue en el flujo (DEC-045) |
| 5 | Tras escanear el QR, el acceso aparece como «Tu cuenta PING» sin «paso» ni evento destacado (caja gris fuera de paleta) | Se pierde el contexto del recorrido | **No incluido.** Pendiente para otra iteración |

## Medido (Chromium, 390×844 y 360×740; px desde el borde superior; la barra del prototipo no se cuenta)

| Posición del botón principal | Antes (app real) | Después (prototipo) |
|---|---|---|
| Identidad 390 | 1311 | **790** (cabe en 844 px) |
| Identidad 360×740 | 1310 | 790 (queda bajo el pliegue) |
| Intereses, vista de mundos 390 | 837 | 861 (**+24, algo más abajo**) |
| Intereses, familia con 8 gustos y 2 elegidos 390 | 945 | 785 |
| Intereses, vista de mundos 360 | 893 | 879 |

La vista de mundos no mejora en este número: el mosaico ocupa más que los botones de texto. La mejora está en estructura, contexto y retroalimentación, no en el alto. No se miden efectos en usuarios.

## Verificado y no verificado

Con 28 aserciones automatizadas en Chromium: alias vacío (error, `aria-invalid`, foco), pestañas con flechas/Home/End y `tabindex` itinerante, radios nativos con lista cerrada, 264 casillas `interestIds`, contador singular/plural, búsqueda que conserva la selección, quitar desde la bandeja, volver sin perder el borrador, controles ≥44 px, sin errores de consola, movimiento reducido sin transiciones, 360 px sin desplazamiento horizontal. Revisión visual de 390/360/1440, oscuro y claro.

**No verificado:** iPhone o Android reales (teclado, áreas seguras, tacto), Safari y Firefox, lector de pantalla, pruebas con personas, integración con Thymeleaf, rendimiento en dispositivos modestos, MySQL 8.4. No se ejecutó la suite Java (no cambia código).

## Skills

Leídas y aplicadas: `ping-efficient-work`, `mobile-native` (campos ≥16 px, hover por capacidad, `touch-action`), `redesign-existing-projects` (de forma selectiva y subordinada a PRODUCT.md). Solo frontmatter: `emil-design-eng` (los valores de movimiento son los ya registrados en DEC-048), `verification-before-completion` (se aplicó el principio con las aserciones). **No ejecutadas ni usadas:** `impeccable` (su motor se descarga; la revisión de capturas fue mía), `ui-ux-pro-max`, `systematic-debugging`. Detalle: `docs/SKILLS_PING.md` y `docs/REFERENCIAS_VISUALES_Y_SKILLS.md`.

## Continuidad

- **Base:** f2923b98ff893c22138ef6552859e5a323ffa7c3. **Rama:** `claude/sweet-ritchie-ja97c7`.
- **Producido:** esta carpeta; `CLAUDE.md`; `.claude/skills/` (8 punteros); apartado nuevo en `docs/SKILLS_PING.md`; `docs/REFERENCIAS_VISUALES_Y_SKILLS.md`.
- **Guardado:** ver el estado de Git y de publicación al cierre de la sesión; un commit local no prueba respaldo remoto.
- **Pendiente humano:** aceptación de Samuel y Julián, registro en `docs/DECISION_LOG.md` y un Issue antes de integrar (AGENTS.md); decisiones abiertas en `HANDOFF_CODEX.md` §6.
- **Siguiente acción:** revisar las dos composiciones, decidir qué se integra y, solo entonces, que Codex siga `HANDOFF_CODEX.md` en una rama propia.
