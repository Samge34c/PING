Continúa el sistema de avatares de PING en Figma usando el paquete PING_Avatares_V2 que adjunto o que he extraído en el proyecto.

OBJETIVO
Importa y mejora estos iconos 2D editables. Samuel aprueba panda rojo, nutria y gato como dirección; conserva sus caras y su estilo. El conejo fue corregido: sin dientes salientes, mejillas suaves, ojos más próximos y sonrisa cerrada. Puedes refinar proporciones y uniones si detectas defectos; no vuelvas a personajes realistas, cabezas humanas ni criaturas deformes.

FIGMA
https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/
Lee el archivo actual y conserva las páginas anteriores. Trabaja en una nueva página «PING · Avatares V3». Usa la conexión de Figma ya configurada; no presupongas falta de permisos por una etiqueta de asiento.

ANTES DE EDITAR
Comprueba repositorio, rama y cambios. Lee AGENTS.md y el contexto de diseño necesario, LEEME.md, catalogo.json por secciones y las vistas del paquete. No cargues cientos de SVG a contexto. La imagen artística de referencia y los SVG son diferentes: los vectores son una interpretación simplificada, no un calco idéntico.

SKILLS
Carga figma-use y las habilidades que esta requiera antes de escribir. Reutiliza logo-design únicamente para coherencia de marca cuando proceda; los personajes no deben rehacer el logo. Aplica habilidades de ilustración, diseño móvil y verificación si están disponibles y son pertinentes. No instales un catálogo entero ni actives hooks.

IMPORTACIÓN EDITABLE
Importa SVG como vectores nativos, por ejemplo mediante figma.createNodeFromSvg cuando la herramienta disponible lo permita. No los conviertas en PNG ni uses una captura plana como supuesto componente editable. Conserva nombres y orden de capas: accesorios traseros → orejas → cabeza → manchas → detalles → ojos → hocico → accesorios frontales. Verifica una importación pequeña primero y revisa su captura antes de importar el resto. Procesa lotes manejables; evita respuestas enormes de la herramienta.

COMPONENTES Y COLORES
Hay cuatro animales, seis paletas por animal y dieciséis diseños de accesorio adaptados a cada especie. Los cuatro colores de accesorios están en catalogo.json y el configurador; los SVG sueltos de accesorios están exportados en violeta. Crea componentes de base y accesorios reutilizables, con propiedades claras para animal, pelaje y slots Cabeza/Audio/Cara/Cuello. Aplica estilos o variables a rellenos cuando aporte valor. No generes miles de variantes de todas las combinaciones: usa instancias, intercambio de componentes y propiedades.

AJUSTE VISUAL
Conserva el carácter 2D, curvas limpias y lectura pequeña. Revisa contornos, alineación óptica de ojos y hocico, separación de gafas, orejas con sombreros, contraste en ambos fondos y accesorios que tapan expresiones. Corrige con criterio; no reconstruyas todo por comodidad. Los anclajes del conejo son distintos y ya están contemplados en los accesorios por especie. Dos sombreros o dos tipos de gafas no se combinan; auriculares y sombreros voluminosos se excluyen. Usa las reglas del catálogo como punto de partida y documenta cualquier compatibilidad nueva que hayas revisado visualmente.

APLICACIÓN MÓVIL
Diseña la selección de personaje y personalización a 390×844, comprueba 360×740 y texto ampliado. Vista previa protagonista, controles claros y continuar alcanzable. Muestra también avatar en cabecera de Mi Ping y en tarjeta de encuentro. La persona elige su apariencia; los gustos no asignan automáticamente un animal ni personalidad. No se suben fotografías.

VERIFICACIÓN PROPORCIONAL
Revisa 32/48/64/96 px, temas claro y oscuro, todos los animales con accesorios representativos, y nombres largos. Si simplificas una versión de 32 px, conserva una variante detallada para el editor. No ejecutes la suite Java por una propuesta de Figma. No afirmes haber probado Figma o un teléfono si no lo hiciste.

ALCANCE Y CIERRE
Puedes mejorar y completar la propuesta de forma autónoma dentro de esta página. No modifiques backend, almacenamiento, registro, matching, puntos ni código de PING en esta tarea. Guarda las fuentes, exportaciones y una continuidad breve en una rama de diseño adecuada; no sobrescribas cambios ajenos ni hagas merge o push a main.
Entrega enlaces reales a los frames, captura móvil, componentes editables, correcciones concretas y límites de lo comprobado. Si falla una herramienta, trabaja en lo independiente y explica el bloqueo preciso. Empieza importando una base y un accesorio y continúa hasta dejar una propuesta revisable, no solo un plan.
