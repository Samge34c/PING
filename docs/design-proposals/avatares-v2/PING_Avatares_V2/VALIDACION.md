# Comprobaciones y límites

- 113 SVG parseados como XML, sin imágenes raster incrustadas.
- 24 bases, 64 accesorios adaptados (16 diseños × 4 especies), 16 ejemplos y 9 láminas SVG.
- Nueve láminas exportadas con Inkscape y revisadas visualmente: caras, paletas, ejemplos claros/oscuros, accesorios por especie y tamaños.
- Se recolocó el sombrero bucket al detectar que tapaba parte de los ojos. Se ajustaron los sombreros y la banda del conejo.
- Panda rojo, nutria y gato naturales conservan exactamente el cuerpo vectorial de V1.
- Reconstrucción portable comprobada usando exclusivamente las fuentes incluidas, sin acceso al generador antiguo.
- Lógica del configurador ejercitada en DOM jsdom: 24 cambios de base/paleta, cuatro tonos, exclusión de accesorios incompatibles, reinicio, cambio de fondo y contenido exportado del SVG.
- El botón de exportación produjo un SVG con grupos y sin imágenes incrustadas en esa comprobación DOM. No se probó la descarga nativa en un navegador visual.

Pendientes: importación en Figma, comportamiento del configurador en navegador visual y teléfono físico/Safari, revisión de todas las combinaciones múltiples y aceptación del conejo por Samuel. Se intentó habilitar Chromium de ensayo, pero su descarga no fue válida; no se presenta como prueba de navegador superada. Los PNG de las láminas proceden de los SVG, no de capturas del configurador.

No se cambió PING ni se ejecutaron pruebas Java: este paquete contiene recursos de diseño y una herramienta local de selección.
