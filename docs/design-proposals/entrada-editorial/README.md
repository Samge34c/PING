# Entrada editorial — propuesta aislada (DEC-063)

**Cierre actual:** código integrado con el incremento del evento, comprobado con369pruebas y capturas reales; [evidencia](../../verification/event-increment/README.md). Los parches/helper de esta propuesta documentan sus bases históricas y no incluyen la ayuda/CSS del incremento. No ejecutarlos sobre el árbol actual ni usarlos para sustituir archivos vigentes. [Prueba de aplicación en copias temporales](../../verification/event-increment/visual-patch-proof.json).

Prototipo y recursos de la mejora de primera impresión: arte de entrada, contraste de la cabecera de Inicio, navegación pública y continuidad Inicio / Unirme / Entrar. **Integrado localmente, sin commit,** sobre `perf/closure-current` (cierre de rendimiento de Codex). Estado y ruta de revisión: `docs/handoffs/CIERRE_RENDIMIENTO.md`, sección «Integración visual DEC-063».

Figma: página «PING · Entrada editorial · Propuesta (29 oct 2026)», `156:317`. Contiene copias sin cambios de `133:1462`, `133:1497` y `133:1572` (sección A), la propuesta móvil (B), escritorio y contraste (C) y el tablero de arte con recortes, alternativa y descarte (D). Los originales de «Selección final» no se tocaron.

## Qué cambia y por qué

| Hallazgo | Comprobado en producción (29 oct, `3a528ca`) | Propuesta |
|---|---|---|
| Arte | Escena de convención/cosplay; el apretón de manos no expresa un gusto común | «Encuentro editorial»: ella señala el pin de un dado en la bolsa de él; al fondo música, juego de mesa e ilustración. 358×220 en móvil, 1,35:1 en escritorio |
| A · «Explorar eventos» | `/events` → 302 `/login`; `AccessPolicy` lo limita a organización | Se retira de Inicio (la acción «Unirme a un evento» ya cubre la entrada por código). En la barra lateral, el visitante ve «Unirme a un evento» en lugar de «Eventos» |
| B · Contraste | `.topbar-note` `rgb(189,181,202)` sobre `#F8E836` = 1,56:1 (solo ≥1024 px). «PING /» de la miga, igual | `#0D0B12` (15,5:1) para nota, punto y miga, solo en la cabecera amarilla de Inicio |
| C · Navegación pública | Barra de visitante solo en Inicio; Unirme y Entrar la pierden. En Figma, «Cuenta · crear» seleccionada ya la tenía y el componente tiene variantes `Activo=Unirme al evento` / `Activo=Entrar` | Misma barra en `/join/event`, `/login` y `/register` con su pestaña activa; oculta (sin cambiar la altura) mientras un campo de texto tiene foco. Pasos 2–3 del alta e Intereses quedan como flujo enfocado, sin barra |

Sin cambios en autenticación, rutas, reglas, permisos, Saga, base de datos, proveedores, Hub ni ACTIVE.

## Archivos

- `fuentes/` — salidas del generador (PNG). `a` elegida; `b` descartada.
- `variantes/` — WebP servibles 768/1152/1400 (100 / 160 / 193 KB). Dimensiones, calidad y SHA-256 en `PROCEDENCIA.json`.
- `integracion/entrada-editorial.css` — único bloque CSS propuesto (se añade al final de `v2.css`).
- `integracion/entrada-editorial.patch` — diff de referencia contra `3a528ca` (aplica limpio ahí; no sobre bases anteriores).
- `tools/aplicar_entrada.py` — aplica la propuesta por anclas exactas sobre la base que toque y copia las variantes; si un ancla no aparece una sola vez, se detiene sin escribir. Probado sobre `3a528ca` y sobre `perf/integrated-mobile-response@b54aaae`.
- `tools/variantes.py` — regenera las variantes desde la fuente.
- `tools/prototipo.py` — genera `prototipo/actual-*.html` y `prototipo/propuesta-*.html` desde el HTML público de producción (`prototipo/vivo/`, CSRF sustituido) con el CSS local del repositorio.
- `capturas/` — evidencia del prototipo previo a integrar (WebP).
- `capturas-app/` — evidencia de la **app real integrada** (H2 y correo locales, evento ficticio): móvil 01–17, escritorio 20–21.
- `tools/revisar_aislado.py` y `tools/recorrido_revision.mjs` — arranque aislado en 127.0.0.1:8117 y recorrido que generó `capturas-app/` (datos privados en `target/visual-review/`, fuera de Git).

Ver el prototipo: desde la raíz del repo, `python -m http.server 8765` y abrir `http://localhost:8765/docs/design-proposals/entrada-editorial/prototipo/propuesta-inicio.html` (`?tema=claro` para el tema claro).

## Evidencia (emulación Chromium por DevTools, no hardware)

Móvil primero: `capturas/01`–`15`; escritorio: `16`–`17`. Mismo HTML, CSS, tema y tamaño en cada par actual/propuesta.

- 390×844: título, explicación, CTA (52 px, y=307) y acceso secundario antes del arte (y=423, 358×220). Sin desbordamiento horizontal a 300, 360 y 390.
- La barra de Unirme/Entrar/Crear marca su pestaña; con el campo de código enfocado queda `visibility:hidden`.
- Escritorio 1280: nota y miga en `rgb(13,11,18)`; arte en paralelo al texto.
- Imagen bloqueada: el hueco conserva su tamaño, el texto alternativo se lee y la acción no cambia.
- Foco por teclado visible (contorno amarillo sólido). Sin movimiento nuevo: nada que reducir.
- No se ha probado en iPhone/Safari ni con teclado real: emulación no reproduce teclado, áreas seguras ni Safari.
