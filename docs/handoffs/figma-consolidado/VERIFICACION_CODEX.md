# Verificación independiente de Figma — Codex

Fecha: 2026-10-25. Archivo: [PING](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING?node-id=102-262), página `102:262`, sección de auditoría `109:10363`.

## Alcance y resultado

Inspección de solo lectura, con mediciones propias mediante `use_figma` y capturas actuales mediante `get_screenshot`. Se leyó el apartado «Auditoría final» y «Cierre» de `docs/handoffs/FIGMA_CONSOLIDADO.md` como conjunto de afirmaciones que comprobar, sin usar sus cifras como evidencia. No se modificaron nodos, componentes, variables, modos ni selección en Figma. Se usó navegación de contexto de página para leer; no se cambiaron propiedades del documento.

| Punto | Resultado | Alcance comprobado |
|---|---|---|
| 1. Deshacer, pestañas y colores | **PASA** en geometría y visibilidad de Deshacer | Las cinco copias cumplen las alturas y anchuras aplicables. Los colores de 360 quedan fuera del área visible inicial: su apariencia tras desplazar **no se pudo verificar**. |
| 2. Insignia | **PASA** | Texto íntegro en dos líneas, sin elipsis, confirmado en captura. |
| 3. Ninguno | **PASA** en ausencia de recorte interno | Etiqueta íntegra dentro de la tarjeta, confirmada en detalle. La acción fija la oculta en la captura inicial: **FALLA si se exige verla completa sin desplazar**. |
| 4. Valores antiguos | **PASA** para los valores antiguos medidos | Los cinco nodos conservan los defectos/valores indicados. No hay base histórica independiente para certificar que absolutamente ninguna otra propiedad cambió. |
| 5. Contraste | **PASA** | 175 segmentos calculados, ambos temas; ninguno bajo su umbral. Muestra de 32 mediciones abajo. |

«PASA» no certifica comportamiento de prototipo, accesibilidad de una aplicación implementada ni invariancia histórica completa.

## Método y trazabilidad

- Sesión MCP: `5fe94ea0-46c2-4234-932c-569ba37c6902`.
- Acceso confirmado a todos los IDs solicitados. Original V3 leído en página `62:262` («PING · Avatares V3»); el resto en `102:262`.
- Geometría: lectura de `width`, `height`, `absoluteBoundingBox`, `clipsContent`, `visible`, padres y hermanos. Medidas en px del diseño, no estimadas de una imagen.
- Texto: `characters`, `textAutoResize`, `textTruncation` y cajas de texto/contenedor, contrastados con capturas.
- Capturas completas actuales a resolución nativa: `103:20081` (390×844), `103:20165` (360×740), `103:20249` (390×844), `103:20333` (390×1180), `112:11776` (390×844), `103:29169` (360×1640) y `103:28464` (360×740).
- Detalles aislados capturados: tarjeta `103:28493`, su antes `109:10580`, insignia corregida `103:29181` y su antes `109:10493`. Un intento de capturar `103:20230` produjo solamente una imagen de 1×1: no se usa como evidencia visual de sus nombres.
- Las capturas se examinaron inline en la sesión. No se crearon archivos PNG, conforme a la restricción de crear únicamente este informe. No se incluyen URLs temporales de assets MCP.

## 1. Copias corregidas: áreas y límites

**PASA** para Deshacer en las cinco copias: área completa dentro de todos sus ancestros con recorte, texto completo y sin solape observado con controles en las capturas.

| Pantalla | ID área Deshacer | Área | Separación respecto a «Probando» | Seis pestañas: altura |
|---|---|---:|---|---|
| `103:20081` Cabeza 390 oscuro | `109:29723` | 78×44 | 10 px horizontales | 46 px cada una |
| `103:20165` Cabeza 360 oscuro | `109:29724` | 78×44 | Debajo, 6 px verticales | 46 px cada una |
| `103:20249` Cabeza 390 claro | `109:29725` | 78×44 | 10 px horizontales | 46 px cada una |
| `103:20333` Cabeza ×1,3 | `109:29726` | 97×44 | Debajo, 6 px verticales | 49 px cada una |
| `112:11776` Pelaje propuesta | `112:11845` | 78×44 | 10 px horizontales | 46 px cada una |

Método de separación: diferencia entre bordes de las cajas absolutas del área y del hermano «Probando». En las filas de dos líneas no se afirma una separación de 10 px: se midieron 6 px. Deshacer contiene una etiqueta de 62×16; a ×1,3, de 81×22. El área contenedora es la que cumple 44 px, no el texto por sí solo. La medición no demuestra que exista un evento interactivo implementado.

Pestañas medidas (Animal, Pelaje, Cabeza, Audio, Cara, Cuello):

| Tira | IDs de las seis pestañas | Alturas en orden |
|---|---|---|
| `103:20096` | `103:20097, 103:20099, 103:20101, 103:20103, 103:20105, 103:20107` | 46, 46, 46, 46, 46, 46 |
| `103:20180` | `103:20181, 103:20183, 103:20185, 103:20187, 103:20189, 103:20191` | 46, 46, 46, 46, 46, 46 |
| `103:20264` | `103:20265, 103:20267, 103:20269, 103:20271, 103:20273, 103:20275` | 46, 46, 46, 46, 46, 46 |
| `103:20348` | `103:20349, 103:20351, 103:20353, 103:20355, 103:20357, 103:20359` | 49, 49, 49, 49, 49, 49 |
| `112:11790` | `112:11791, 112:11793, 112:11795, 112:11797, 112:11799, 112:11801` | 46, 46, 46, 46, 46, 46 |

La uniformidad de altura no significa que todas estén visibles inicialmente: las tiras recortan horizontalmente. En Cabeza 390, Cara deja 35 px visibles; a 360 deja 5 px. Cuello queda fuera. En Pelaje, Cara deja 34 px. No se probó el gesto de desplazamiento. La afirmación adicional del handoff «72–84 px de ancho» no es universal: Cara mide 65 px, y en ×1,3 hay pestañas de hasta 96 px.

**PASA** para las anchuras de columnas Colores y contención local de nombres:

| Pantalla / Colores | IDs columnas Violeta, Amarillo, Menta, Coral | Anchuras | Alturas | Cajas de nombres: ancho×alto |
|---|---|---|---|---|
| `103:20081 / 103:20146` | `103:20147, 103:20151, 103:20155, 103:20159` | 48, 54, 48, 48 | 58, 59, 58, 58 | 46×20, 54×16, 42×20, 35×20 |
| `103:20165 / 103:20230` | `103:20231, 103:20235, 103:20239, 103:20243` | 48, 54, 48, 48 | 58, 59, 58, 58 | 46×20, 54×16, 42×20, 35×20 |
| `103:20249 / 103:20314` | `103:20315, 103:20319, 103:20323, 103:20327` | 48, 54, 48, 48 | 58, 59, 58, 58 | 46×20, 54×16, 42×20, 35×20 |
| `103:20333 / 103:20398` | `103:20399, 103:20403, 103:20407, 103:20411` | 59, 71, 54, 48 | 61, 65, 61, 61 | 59×23, 71×22, 54×23, 45×23 |

Todos los nombres tienen autoajuste WIDTH_AND_HEIGHT, truncamiento DISABLED y sus cajas caben dentro de sus columnas. Las capturas 390 oscuro, claro y ×1,3 muestran los cuatro nombres completos.

**Limitación 360:** `103:20230` tiene caja absoluta (568, 9117, 222, 59), pero `103:20167` «Contenido desplazable» acaba en y=9067. Por ello queda fuera del recorte inicial; no puedo certificar visualmente su aparición tras desplazar sin operar el prototipo. No se confunde esto con un recorte interno de la etiqueta.

En `112:11776` **no existe una tira llamada Colores**. Para no inventarla, esa parte literal es no aplicable. Sí se midieron sus seis opciones de pelaje: `112:11804, 112:11808, 112:11812, 112:11816, 112:11820, 112:11826`, todas de **114 px de ancho**, alturas 112/115 px; los nombres Natural, Dorado, Lila, Perla, Cacao y Coral aparecen íntegros en la captura.

## 2. Insignia de espera

**PASA.** Pantalla `103:29169`; insignia `103:29181`, **202×44**; texto `I103:29181;40:551`, **158×32**, autoajuste HEIGHT, truncamiento DISABLED. Contenido exacto: «Esperando a Valentina la del cosplay de dragón».

Caja de insignia: (2038, 9538, 202, 44). Texto: (2070, 9544, 158, 32), con 6 px de margen vertical por lado. La captura completa y el detalle muestran dos líneas y la palabra «dragón» íntegra, sin elipsis.

## 3. «Ninguno» en Identidad 360

**PASA para el recorte interno.** Pantalla `103:28464`; opción `103:28493` **69×133**; fila `103:28495` **55×32**; texto `103:28497` **55×16**, WIDTH_AND_HEIGHT, truncamiento DISABLED. El check `103:28496` mide 14×14 y está encima del nombre.

Texto absoluto (167, 9111,5, 55, 16), dentro de la opción (160, 9009, 69, 133): 7 px a cada lado. El detalle aislado muestra «Ninguno» completo.

**FALLA si el criterio incluye visibilidad inicial sin desplazar:** la acción fija `103:28510` ocupa (144, 9055, 360, 93) y cubre completamente la etiqueta, entre y=9111,5 y 9127,5. La captura completa confirma que solo asoman las partes superiores de las opciones. El detalle aislado excluye esa superposición y no debe usarse para afirmar visibilidad de la pantalla completa. No se verificó si un desplazamiento real permite descubrir toda la opción.

## 4. Preservación de valores antiguos

**PASA para los valores de referencia medidos hoy:**

| Nodo protegido | Evidencia propia actual |
|---|---|
| `103:23545` Pelaje aprobado | Deshacer `103:23552` **62×16**, sin contenedor táctil nuevo; pestaña activa Pelaje `103:23562` **73×40**. |
| `72:4805`, página `62:262` | Deshacer `72:4841` **62×16**; activa Pelaje `72:4906` **73×40**. Tira `72:4903`: alturas 46, 40, 46, 46, 46, 46. |
| `109:10379` antes Cabeza | Deshacer `109:10386` **62×16**; activa Cabeza `109:10399` **81×40**. Columnas `109:10445, 109:10449, 109:10453, 109:10457`: anchos **46, 54, 42, 36**; muestras 36×36 salvo seleccionada 41×41. |
| `109:10481` antes insignia | `109:10493` **202×28**; texto `I109:10493;40:551` **158×16**, autoajuste TRUNCATE, truncamiento ENDING. El detalle muestra «Esperando a Valentina l…». |
| `109:10551` antes Ninguno | Opción `109:10580` **69×117**, fila `109:10582` **73×16**, texto `109:10584` **55×16**, check a su izquierda. Fila excede 4 px el ancho de opción (2 px por lado); texto termina 2 px fuera del borde derecho. La captura aislada conserva el recorte. |

No se dispone de un snapshot anterior independiente, hash de árbol ni historial de versiones consultado. Por tanto **no verificado** que cada propiedad de estos subárboles haya permanecido sin cambios a lo largo del tiempo. Se comprueba su estado antiguo esperado en los atributos relevantes y que esta sesión no ejecutó escrituras.

## 5. Contraste por modo

**PASA.** Se recorrieron las siete pantallas corregidas enumeradas en el método: **175 segmentos de texto**, incluidos los que quedan fuera del viewport inicial. **0 bajo el umbral, 0 segmentos sin resolver**, mínimo **5,69846:1** (Deshacer en claro). No se extrapola este resultado a los 813 textos citados en el handoff ni a toda la página.

Procedimiento:
1. Leer segmentos con `getStyledTextSegments(['fills','fontSize','fontWeight'])`.
2. Para cada pintura vinculada, obtener variable con `getVariableByIdAsync` y resolver con `resolveForConsumer(nodo)`, incluyendo alias. Registrar `resolvedVariableModes`.
3. Resolver también las pinturas de los ancestros según el modo del consumidor correspondiente y componer las superficies desde fuera hacia dentro; componer la pintura del texto y su opacidad sobre ese fondo. No se encontraron casos no sólidos ni opacidades ancestrales distintas de 1 en los textos calculados.
4. Linealizar sRGB: c/12,92 para c≤0,04045; ((c+0,055)/1,055)^2,4 en otro caso. L=0,2126R+0,7152G+0,0722B. Relación=(Lmáx+0,05)/(Lmín+0,05).
5. Umbral 4,5:1; 3:1 para ≥24 px, o ≥18,6667 px y peso ≥700. Calcular con los floats originales; redondear solo al presentar.

Colección `VariableCollectionId:38:263` «PING V2 · Color»: **38:0 Oscuro**, **38:1 Claro**. La muestra siguiente usa 16 textos de `103:20081` y 16 de `103:20249`. Nombres de variables sin prefijo `v2/` para abreviar. Fondo identificado por la última superficie sólida pertinente. Todos los resultados de la tabla son **PASA**.

| Modo | ID texto | Texto | px/peso | Variable texto → variable fondo | Texto / fondo resueltos | Relación | Umbral |
|---|---|---|---|---|---|---:|---:|
| Oscuro | `I103:20082;42:709` | Mi Ping | 16/700 | texto/principal → fondo/lienzo | `#f7f4ef` / `#15121c` | 16.86:1 | 4.5:1 |
| Oscuro | `103:20087` | Probando: gorra amarilla · sin guardar | 13/700 | texto/principal → fondo/elevado | `#f7f4ef` / `#262131` | 14.24:1 | 4.5:1 |
| Oscuro | `103:20088` | Deshacer | 13/700 | interactivo → fondo/elevado | `#c2a6ff` / `#262131` | 7.58:1 | 4.5:1 |
| Oscuro | `103:20092` | Luna | 20/800 | texto/principal → fondo/elevado | `#f7f4ef` / `#262131` | 14.24:1 | 3:1 |
| Oscuro | `103:20094` | Al guardar se quitan los auriculares: no combinan con la gorra. | 14/500 | estado/aviso → estado/aviso-fondo | `#ffb547` / `#3a2a12` | 7.86:1 | 4.5:1 |
| Oscuro | `103:20098` | Animal | 14/500 | texto/principal → fondo/superficie | `#f7f4ef` / `#1d1926` | 15.70:1 | 4.5:1 |
| Oscuro | `103:20100` | Pelaje | 14/500 | texto/principal → fondo/superficie | `#f7f4ef` / `#1d1926` | 15.70:1 | 4.5:1 |
| Oscuro | `103:20102` | Cabeza | 13/700 | texto/sobre-uva → marca/uva | `#f7f4ef` / `#6a298f` | 8.23:1 | 4.5:1 |
| Oscuro | `103:20104` | Audio | 14/500 | texto/principal → fondo/superficie | `#f7f4ef` / `#1d1926` | 15.70:1 | 4.5:1 |
| Oscuro | `103:20115` | Ninguno | 13/700 | texto/principal → fondo/superficie | `#f7f4ef` / `#1d1926` | 15.70:1 | 4.5:1 |
| Oscuro | `103:20145` | Color de la gorra | 13/700 | texto/secundario → fondo/lienzo | `#bdb5ca` / `#15121c` | 9.36:1 | 4.5:1 |
| Oscuro | `103:20150` | Violeta | 14/500 | texto/secundario → fondo/lienzo | `#bdb5ca` / `#15121c` | 9.36:1 | 4.5:1 |
| Oscuro | `103:20154` | Amarillo | 13/700 | texto/principal → fondo/lienzo | `#f7f4ef` / `#15121c` | 16.86:1 | 4.5:1 |
| Oscuro | `103:20158` | Menta | 14/500 | texto/secundario → fondo/lienzo | `#bdb5ca` / `#15121c` | 9.36:1 | 4.5:1 |
| Oscuro | `103:20162` | Coral | 14/500 | texto/secundario → fondo/lienzo | `#bdb5ca` / `#15121c` | 9.36:1 | 4.5:1 |
| Oscuro | `I103:20164;40:404` | Guardar avatar | 16/800 | accion/texto → accion/fondo | `#0d0b12` / `#f8e836` | 15.45:1 | 4.5:1 |
| Claro | `I103:20250;42:709` | Mi Ping | 16/700 | texto/principal → fondo/lienzo | `#17141d` / `#f7f4ef` | 16.58:1 | 4.5:1 |
| Claro | `103:20255` | Probando: gorra amarilla · sin guardar | 13/700 | texto/principal → fondo/elevado | `#17141d` / `#eee8f3` | 15.14:1 | 4.5:1 |
| Claro | `103:20256` | Deshacer | 13/700 | interactivo → fondo/elevado | `#6e3fb8` / `#eee8f3` | 5.70:1 | 4.5:1 |
| Claro | `103:20260` | Luna | 20/800 | texto/principal → fondo/elevado | `#17141d` / `#eee8f3` | 15.14:1 | 3:1 |
| Claro | `103:20262` | Al guardar se quitan los auriculares: no combinan con la gorra. | 14/500 | estado/aviso → estado/aviso-fondo | `#8a4b00` / `#fff0d6` | 6.05:1 | 4.5:1 |
| Claro | `103:20266` | Animal | 14/500 | texto/principal → fondo/superficie | `#17141d` / `#ffffff` | 18.19:1 | 4.5:1 |
| Claro | `103:20268` | Pelaje | 14/500 | texto/principal → fondo/superficie | `#17141d` / `#ffffff` | 18.19:1 | 4.5:1 |
| Claro | `103:20270` | Cabeza | 13/700 | texto/sobre-uva → marca/uva | `#f7f4ef` / `#6a298f` | 8.23:1 | 4.5:1 |
| Claro | `103:20272` | Audio | 14/500 | texto/principal → fondo/superficie | `#17141d` / `#ffffff` | 18.19:1 | 4.5:1 |
| Claro | `103:20283` | Ninguno | 13/700 | texto/principal → fondo/superficie | `#17141d` / `#ffffff` | 18.19:1 | 4.5:1 |
| Claro | `103:20313` | Color de la gorra | 13/700 | texto/secundario → fondo/lienzo | `#5e566b` / `#f7f4ef` | 6.34:1 | 4.5:1 |
| Claro | `103:20318` | Violeta | 14/500 | texto/secundario → fondo/lienzo | `#5e566b` / `#f7f4ef` | 6.34:1 | 4.5:1 |
| Claro | `103:20322` | Amarillo | 13/700 | texto/principal → fondo/lienzo | `#17141d` / `#f7f4ef` | 16.58:1 | 4.5:1 |
| Claro | `103:20326` | Menta | 14/500 | texto/secundario → fondo/lienzo | `#5e566b` / `#f7f4ef` | 6.34:1 | 4.5:1 |
| Claro | `103:20330` | Coral | 14/500 | texto/secundario → fondo/lienzo | `#5e566b` / `#f7f4ef` | 6.34:1 | 4.5:1 |
| Claro | `I103:20332;40:404` | Guardar avatar | 16/800 | accion/texto → accion/fondo | `#0d0b12` / `#f8e836` | 15.45:1 | 4.5:1 |

## Límites, incidencias y cierre

- No hubo denegaciones de acceso al archivo ni nodos solicitados ausentes.
- Una primera lectura devolvió más de 48 000 bytes y fue rechazada por límite de salida; se repitió con campos acotados. Otra lectura falló al consultar opacity en SECTION; se corrigió el lector con comprobación de capacidad. Ninguno de esos scripts contenía operaciones de escritura.
- No se pudo examinar visualmente el bloque Colores de 360 tras desplazar ni demostrar que «Ninguno» quede accesible mediante desplazamiento. El estado inicial y sus límites sí se midieron.
- El contraste se calculó para las superficies ancestrales de los textos; no es un análisis del antialiasing de píxeles ni una prueba de visibilidad. No se probó teclado, lector de pantalla, teléfono, Safari, áreas seguras ni gestos.
- No se corrigió ningún defecto: el encargo es de solo lectura. La observación abierta más relevante es la superposición de la acción fija sobre las opciones de Identidad 360.
- Único archivo creado: `docs/handoffs/figma-consolidado/VERIFICACION_CODEX.md`. Sin cambios en aplicación/backend, sin ejecutar build, suite Java ni linter: no validarían estas medidas de Figma y podrían crear archivos fuera del alcance.
- Se aplicaron las pautas de `ping-efficient-work`, las instrucciones leídas de `figma-use` y `verification-before-completion`. No se iniciaron subagentes, no se instalaron herramientas y no se hizo commit, push, merge ni cambio de rama. Rama observada: `design/conectar-figma`; había cambios y archivos sin seguimiento previos, que no se tocaron.

