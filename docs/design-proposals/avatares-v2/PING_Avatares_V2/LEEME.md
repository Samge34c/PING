# PING · Avatares editables V2

## Qué cambió
- Se conservan las caras vectoriales de panda rojo, nutria y gato de V1.
- Conejo nuevo: mejillas redondeadas, ojos más próximos, nariz rosada pequeña, sonrisa cerrada y sin dientes salientes.
- 6 paletas para cada animal (24 bases): Natural, Dorado, Lila, Perla, Cacao y Coral.
- 16 accesorios en cuatro colores (Violeta, Amarillo, Menta, Coral), adaptados al conejo y a los otros animales.
- 16 combinaciones de ejemplo, configurador local y vistas de las piezas reales.

## Empezar
1. Extrae TODO el ZIP conservando sus carpetas.
2. Abre CONFIGURADOR.html con Chrome, Edge o Safari de escritorio. No necesita servidor ni conexión a Internet.
3. Elige animal, pelaje y accesorios. El color de accesorios se aplica conjuntamente en este configurador; en Figma puedes recolorear cada pieza por separado.
4. Descarga el SVG. Arrástralo a Figma para editar formas y rellenos; conserva el orden de grupos traseros y frontales.

## Carpetas
- 01-bases: 24 SVG transparentes sin accesorios.
- 02-accesorios: 64 SVG (16 accesorios × 4 animales), exportados en violeta. En catalogo.json y CONFIGURADOR.html están las cuatro opciones de color.
- 03-ejemplos: 16 combinaciones listas para importar.
- 04-vistas: láminas SVG/PNG renderizadas desde los vectores reales, incluidos todos los accesorios por especie y tamaños de lectura.
- 05-fuentes: generador Python y primitivas, para reconstrucción y ampliación.
- 06-referencia: referencia artística generada; no es la fuente vectorial ni una copia idéntica de los SVG.
- catalogo.json: cuerpos SVG, accesorios, colores, slots, ejemplos y reglas de superposición.
- PROMPT_CLAUDE_FIGMA.md: encargo listo para Claude Code.

## Accesorios
Audio: auriculares, auriculares con micrófono.
Cabeza: gorro, gorra, bucket, banda, lazo, pin estrella, vincha de estrellas.
Cara: gafas redondas, cuadradas, de sol.
Cuello: pañuelo, bufanda, corbatín, pase del evento.

Una pieza por slot. Los auriculares y los sombreros voluminosos se excluyen para evitar cruces; el configurador conserva tu última elección y retira la incompatible. El sistema ofrece mezclas de slots permitidas, pero no se ha revisado manualmente cada combinación múltiple posible. Todos los accesorios individuales por especie aparecen en las láminas.

## Editabilidad y alcance
Los personajes y accesorios SVG contienen formas y curvas nativas, sin imágenes incrustadas ni fuentes. Los grupos están nombrados. Las vistas sí contienen texto de presentación. Un SVG importado sigue necesitando convertirse a componentes de Figma; esto aún no está hecho ni verificado en Figma.

No hay integración en PING, animaciones, persistencia de elección ni cambios en reglas de producto. Estos recursos no son una identidad aprobada definitivamente. La referencia artística usa ImageGen y los SVG se construyen como interpretación vectorial simplificada. No atribuyas la referencia raster a un calco exacto.

## Reconstruir
Con Python 3: ejecuta `python 05-fuentes/build_v2.py`. El generador reconstruye los SVG, el catálogo y el configurador con la biblioteca estándar de Python. Los PNG incluidos son exportaciones de las láminas SVG hechas con Inkscape; pueden volver a exportarse con Figma o Inkscape. Al reconstruir no borra archivos ajenos; crea o actualiza los resultados con los mismos nombres dentro del paquete.
