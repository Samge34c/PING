# Figma · Codex · Miniaturas V3

Fecha: 2026-10-25. Corrección visual autorizada por Samuel en una copia aislada. No implica aprobación de Julián ni autorización de integración en la aplicación.

## Alcance y estado

- Archivo: `WCrjKj5eGOZQ8wUaxsEqfc`.
- Página propia: **PING · Prueba Codex · Miniaturas** (`87:1967`).
- Rama conservada: `design/conectar-figma`; HEAD consultado al cierre: `49e9482`. Sin cambio de rama, commit, push o merge por Codex.
- No se modificaron aplicación, backend, MySQL ni los originales de Claude. No se ejecutaron pruebas Java.
- Trabajo cerrado en la copia. Samuel aprueba explícitamente el encuadre del «después» mostrado; conservarlo sin reducir más los animales. Esta aprobación no se extiende a integración en la aplicación ni a sobrescribir la página original.

## Cierre tras aprobación del encuadre · 2026-10-25

Instrucción explícita del usuario: «Apruebo el encuadre del después mostrado. Consérvalo y no reduzcas más los animales». Se conserva el factor **84 %** y la posición **20,48 / 20,48** dentro del master de 256 px. No se hicieron nuevas modificaciones en Figma.

Se reutilizaron y volvieron a examinar únicamente las capturas existentes `antes-despues.png`, `verificacion-pantallas.png` y `cuatro-animales.png`, junto con las medidas ya obtenidas de los componentes:

- **Accesorios pequeños:** el pin conserva su lectura de estrella; el lazo mantiene sus dos lóbulos; la banda se distingue de los gorros y las estrellas de la vincha siguen visibles. Sus nombres aportan identificación inequívoca. No se observó un fallo que exigiera ampliar el animal, reducirlo o alterar el accesorio.
- **390/360 y temas:** las opciones pequeñas del gato siguen diferenciándose en los dos anchos y ambos temas de las capturas existentes. Se conservan nombres e indicadores.
- **Otros animales:** la lámina de panda rojo, nutria, gato y conejo confirma que el mismo encuadre conserva las orejas y permite distinguir gorro, bucket y vincha sin saltos de escala. Las medidas previas de pin y lazo son equivalentes entre especies, con el desplazamiento original específico del conejo; se conserva la misma transformación uniforme.
- **Límite de la evidencia:** la captura corregida de los cuatro animales contiene cuatro opciones por especie; no se afirma una revisión visual de pin y lazo en todas las combinaciones. No se creó una matriz adicional ni se repitieron capturas o pruebas del producto.

Cierre: encuadre aprobado conservado, revisión acotada sin defectos nuevos observados y sin necesidad de ajustes adicionales. El resultado editable permanece en `87:2699`, dentro de la página aislada `87:1967`. Originales intactos; sin integración en aplicación, cambios de backend/MySQL, pruebas Java, commit, push, merge ni cambio de rama. No se modificaron los cambios ajenos presentes en el registro de skills.

## Skill de diseño aplicada: logo-design

Corrección de identificación solicitada por Samuel: la skill de diseño requerida es **`logo-design`**, no una skill de biblioteca ni una skill del servidor Figma. Las skills de Figma descritas más abajo son auxiliares de ejecución.

Se reutiliza **la instalación local compartida que usa Claude**, sin nueva descarga, adaptación, hooks ni colección adicional:

- Puntero de Claude: `.claude/skills/logo-design/SKILL.md`.
- Fuente única utilizable directamente por Codex: `.agents/skills/logo-design/SKILL.md`.
- Repositorio original: `https://github.com/kaankiziltug/logo-design-skill`.
- Versión **1.4.5**, commit **`734090317a2155c5bf1ae8bd4cc85c6848a0128f`**, licencia **MIT**, autoría kaankiziltug.
- Registro: `.agents/skill-installation.json`; SHA-256 comprobado: `65ec4bd58732e8cc11e244724e8666de78764b973f6d3b03b233904639cf47c6`.

Ante esta aclaración se hizo una nueva consulta específica `claude_ask` sobre la instalación, fuente y versión de `logo-design`, solo para obtener información. El puente agotó **60 segundos sin respuesta**; no se le atribuye una confirmación. Se consultaron el puntero y manifiesto locales y se compararon con el directorio de fuentes original usado por Claude: los **26 archivos de instrucciones/referencias/scripts/templates coinciden byte a byte**; `LICENSE` y `TRADEMARKS.md` están en la raíz de esas fuentes originales y coinciden con las copias de proyecto. Los **28 archivos registrados están presentes**; no faltan las **17 rutas de referencias extraídas de SKILL.md**. No se leyeron credenciales.

Se leyó **SKILL.md completo**. Para esta revisión se leyeron `references/critique.md`, `identity-system.md`, `testing-checklist.md`, `visual-techniques.md` y los principios pertinentes. El modo aplicado es crítica y refinamiento del encuadre existente: no es creación de logos, exploración de conceptos ni rediseño de personajes.

### Aplicación concreta a las miniaturas

| Criterio pertinente de logo-design | Revisión y decisión |
|---|---|
| Proporción y ausencia de deformación | Lectura posterior de las 16 muestras: factor 0,84 y relación de aspecto 1 en la ilustración. Se conserva la anatomía original. |
| Espacio negativo y supervivencia del recorte circular | Se conserva la corrección de zoom del 130 % al 84 %, con margen nominal del 8 %. Orejas y piezas altas/anchas permanecen dentro del disco. |
| Consistencia y tamaño aparente | Se conserva la misma escala entre accesorios del mismo animal. No se amplía el rostro para rellenar cada opción ni se igualan artificialmente las anatomías de distintas especies. |
| Coherencia del sistema de identidad | Se mantienen dibujos, paletas, fondos, tipografía Manrope, composición y controles aprobados. |
| Legibilidad a tamaño de uso y revisión en contexto | Se volvieron a examinar las capturas del antes/después y la lámina a 56 px: accesorios reconocibles, nombres e insignias legibles. Se conserva la validación móvil 390/360, claro/oscuro. |
| Fidelidad del color | Se conserva la corrección ya realizada de los modos explícitos de acento en las muestras, para evitar heredar el amarillo en todas las piezas. |

**Resultado de esta revisión:** lo realizado ya satisface los criterios pertinentes de `logo-design`; no se añadieron nuevas modificaciones al lienzo en esta aclaración. Los ajustes concretos del resultado son escala/margen del componente duplicado, reenlace de accesorios a masters copiados y corrección de modos de acento de la lámina. No se inventa una nueva modificación para atribuírsela a la revisión. Se conservan los originales.

La skill es accesible desde Codex con sus referencias, scripts y licencia; no requirió instalación. Sus scripts Python siguen sin ejecutarse porque `python` no está disponible en PATH. La evaluación se realizó sobre las capturas reales y la geometría nativa de Figma; no se afirma una auditoría SVG automática. La biblioteca opcional de logotipos de terceros continúa excluida y no es necesaria para este encargo.

## Skills auxiliares de Figma y evidencia histórica

En la comprobación inicial se emitió una consulta `claude_ask` mediante el puente local Tandem 0.9.1 para preguntar nombre, ruta, fuente y aplicación real de las skills. No se obtuvo una respuesta utilizable. La aclaración posterior sobre `logo-design` motivó la consulta específica descrita arriba. No se delegó la corrección. Los procesos de consulta recibieron `disableAllHooks: true`; no se instalaron ni activaron hooks. No se leyeron credenciales.

Se recurrió al handoff de Claude, los punteros de proyecto y los manifiestos locales pertinentes. Una extracción acotada de registros de invocación corroboró las skills efectivamente cargadas y utilizadas; no se copiaron conversaciones privadas al repositorio.

**Skill auxiliar de componentes: `figma-generate-library`.** No sustituye a `logo-design`.

- Procedencia: plugin oficial `figma@claude-plugins-official`, autor Figma; marketplace original `https://github.com/anthropics/claude-plugins-official`.
- Instalación de Claude: `C:/Users/river/.claude/plugins/cache/claude-plugins-official/figma/2.2.157/skills/figma-generate-library/`.
- Versión del plugin: **2.2.157**. Commit registrado en `installed_plugins.json`: **`f2789420e7e8b4e2465dfeb1e12e1fc6a31397e5`**.
- Evidencia de uso V3: llamada de Claude de 2026-10-25 05:07 UTC, «Página Avatares V3, variables de pelaje y acento, prueba de importación de una base y un accesorio», con `skillNames: figma-use,figma-generate-library`.
- Acceso de Codex: recurso oficial **`skill://figma/figma-generate-library/SKILL.md`** del servidor Figma. Se leyó y comparó con la instalación de Claude: contenido equivalente tras normalizar finales de línea y espacios exteriores.
- No se descargó ni redistribuyó el plugin; el manifiesto local no declara una licencia independiente de la skill. Se conserva la procedencia y se consume el recurso oficial.

Claude también cargó **`figma-use`** y **`figma-generate-design`** desde ese mismo plugin. Codex cargó ambas desde los recursos oficiales `skill://figma/<nombre>/SKILL.md`. El servidor incluye revisiones respecto a la copia 2.2.157: requisito de sesión y límites adicionales de lectura en `figma-use`, y una diferencia sobre generación de imágenes en `figma-generate-design` que no afecta este trabajo. No se afirma identidad byte a byte para esas dos skills.

**Compatibilidad comprobada, más allá de leer SKILL.md:**

- Referencias leídas: índice Plugin API, gotchas, componentes y trabajo con sistemas de diseño; referencia `component-creation.md` de la skill principal.
- El servidor no entregó el archivo de tipos `plugin-api-standalone.d.ts` ni el helper `scripts/validateCreation.js`. Se usaron exclusivamente las copias de la instalación **oficial** de Claude, no fuentes de terceros.
- Se consultaron las firmas de `rescale`, `clone`, límites de render y redimensionado en esos tipos.
- Se ejecutó el helper oficial **`validateCreation.js`** en `use_figma`, sin modificar el lienzo: **5 nodos comprobados, 0 fallos** (tres sets, pantalla final y lámina compacta).
- Se comprobaron lecturas, escritura del componente duplicado, intercambios de instancias, modos de variables, capturas y lectura posterior persistida mediante el servidor real.
- No fue necesaria instalación para Codex ni dependencias nuevas.

**Evidencia histórica de `logo-design`:** además de la identificación explícita de Samuel, la sesión de Claude demuestra su lectura para trabajo de identidad de PING. Esa evidencia histórica no demuestra por sí sola cada decisión del personalizador V3. Su uso en la revisión actual y su instalación exacta se documentan al inicio de este handoff.

Se leyeron sus instrucciones y referencias de coherencia, crítica y legibilidad. Solo se aprovecharon criterios de proporción, margen, consistencia y lectura a tamaño real. No se cambió el logo ni se redibujaron los animales. Sus scripts Python no se utilizaron: `python` no está disponible en PATH; la biblioteca opcional de logotipos tampoco está instalada, tal como ya documentaba el proyecto. No se instaló Python ni esa biblioteca para una edición nativa en Figma. Esta limitación no afecta a la ejecución auxiliar de Figma, cuyo helper de componentes sí se ejecutó.

## Causa y decisión concreta

El master original `Avatar V3`, variante `Recorte=Primer plano`, encerraba una ilustración de **332,8 × 332,8** en un disco de **256 × 256**, desplazada a **−38,4 / −17,6** y con `clipsContent` activo. El zoom del 130 % recortaba la ilustración.

Aplicando el criterio de componente reutilizable de `figma-generate-library`, se corrigió la **geometría del master duplicado**, en lugar de ajustar cada opción por separado. Las cuatro variantes Primer plano usan **215,04 × 215,04**, posición **20,48 / 20,48**: escala uniforme del **84 %** y margen nominal del **8 %**. La ilustración interior conserva `clipsContent=false`; no se añadieron máscaras. El disco exterior permanece intacto y ahora contiene la ilustración completa.

- A 56 px: área de ilustración 47,04 px y margen nominal 4,48 px.
- Misma escala entre todas las piezas y los cuatro animales; se preservan las proporciones originales de cada especie.
- Vectores, capas, variables de pelaje/acento, textos y estilos Manrope permanecen editables.
- No se alteraron los paths ni la anatomía. La ilustración existente consiste en cabeza y orejas; no se inventó cuerpo.
- Se duplicaron también los sets frontal y trasero de Cabeza. Se reenlazaron los overrides que Figma conservaba apuntando a originales, además de los enlaces del master copiado.
- Se conservaron el estado guardado/provisional, borde discontinuo, insignia, aviso de incompatibilidad, Deshacer y Guardar avatar.
- En la lámina se corrigieron modos explícitos heredados de acento para representar Violeta, Menta, Amarillo y Coral correctamente.

Al recibir la aclaración posterior sobre la skill, se conservaron todos los objetos. Se completó la comparación de procedencia/contenido y se ejecutó el helper oficial. La revisión confirmó la corrección actual; no hizo falta otro cambio visual.

## Resultado editable

Base: `https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING?node-id=`.

| Objeto | Nodo | Enlace |
|---|---|---|
| Antes / después visible | `89:3137` | [Comparación](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING?node-id=89-3137) |
| Pantalla final 390 oscuro | `87:2699` | [Editar resultado](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING?node-id=87-2699) |
| Antes conservado | `87:2615` | [Antes](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING?node-id=87-2615) |
| Copia de referencia Pelaje aprobada | `87:3157` | [Referencia](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING?node-id=87-3157) |
| Validación 360 y tema claro | `89:3144` | [Pantallas](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING?node-id=89-3144) |
| Cuatro animales, 16 muestras | `89:3152` | [Lámina compacta](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING?node-id=89-3152) |
| 390 texto ×1,3 | `87:4588` | [Texto ampliado](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING?node-id=87-4588) |

Componentes propios: `Avatar V3 · Codex` **87:1968** (12 variantes), Cabeza **87:2089** (32) y Cabeza trasera **87:2266** (32). Variantes Primer plano: panda **87:2049**, nutria **87:2059**, gato **87:2069**, conejo **87:2079**. Las bases y las familias Audio/Cara/Cuello siguen reutilizándose como dependencias originales de solo lectura; no se modificaron.

Pantallas adicionales: 360 oscuro **87:3214**, 390 claro **87:3672**, 360 claro **87:4130**.

## Verificación y capturas

Capturas reales obtenidas por MCP y examinadas por Codex:

1. [Antes y después](figma-codex-miniaturas/antes-despues.png).
2. [360 oscuro, 390 claro y 360 claro](figma-codex-miniaturas/verificacion-pantallas.png).
3. [Cuatro animales y accesorios](figma-codex-miniaturas/cuatro-animales.png).
4. [Texto ×1,3](figma-codex-miniaturas/texto-130.png).

La lámina incluye Panda rojo/Natural/Violeta, Nutria/Dorado/Menta, Gato/Cacao/Amarillo y Conejo/Perla/Coral; Ninguno, Gorro tejido, Sombrero bucket y Vincha de estrellas a 56 px. La pantalla completa incluye además gorra, banda, lazo y pin. Se comprobaron opciones altas/anchas, orejas, margen, proporción, nombres e indicadores. Los límites renderizados de los vectores en las 16 muestras quedan dentro del contenedor; el menor margen medido es aproximadamente **6,62 px**.

Lectura posterior del servidor:

- Cinco pantallas corregidas, cada una con ocho miniaturas al 84 %, vinculadas al master Codex.
- Todos los overrides frontal/trasero de esas miniaturas apuntan a los sets Codex.
- Los tres controles/avisos requeridos siguen presentes en las cinco pantallas.
- Comparación recursiva de la pantalla original `72:4329` con la referencia Antes: **idénticas** en geometría interna, textos, fuentes, fills, strokes, máscaras, clips, componentes y propiedades, excluyendo nombre/posición del frame raíz.
- El master original del gato mantiene 332,8 px y sus desplazamientos negativos; no recibió la corrección de la copia.
- Helper oficial de validación: **5 comprobaciones aprobadas, 0 fallos**.

Revisión móvil: se entiende la acción principal; las miniaturas ya no pierden ilustración; etiquetas e indicadores siguen legibles. Guardar avatar queda visible. En 360×740, los colores quedan más abajo dentro del contenido desplazable, como en el diseño de origen. Se conserva la composición y la identidad Festival Social, sin añadir decoración ni vacíos.

## Limitaciones reales

- Se revisó el lienzo y sus capturas, no un teléfono físico ni una implementación funcional. No se afirma que Guardar/Deshacer estén implementados o que las interacciones del prototipo hayan sido ensayadas.
- No se probó el producto completo ni todas las combinaciones de accesorios; la cobertura visual está acotada a la lámina y las pantallas afectadas.
- Figma puede conservar piezas de otro animal al cambiar variantes. La lámina fija explícitamente animal y pieza frontal/trasera; no se rediseñó la API general de variantes.
- Algunas piezas ocultan partes de orejas por su superposición original (p. ej. gorros); eso no es un recorte del contenedor y se conserva el dibujo existente.
- Los originales y la referencia aprobada permanecen en su página; el resultado editado solo está en la página Codex.

