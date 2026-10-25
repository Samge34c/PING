# Continuidad Figma · Claude Code (avatares V3)

Fecha: 2026-10-25. Solo diseño: no hay cambios en la aplicación, el backend ni `main`.

## Repositorio

- Rama: `design/conectar-figma` (local, sin push). HEAD al empezar este registro: `07fbfb5`.
- Sin seguimiento hasta el commit de esta tarea: `docs/design-proposals/avatares-v2/` (paquete V2 extraído y fuentes V3).
- Carpeta compartida con Codex: no cambiar de rama ni tocar archivos ajenos.

## Figma

Archivo `WCrjKj5eGOZQ8wUaxsEqfc` («PING · Diseño móvil»). Página de trabajo: **PING · Avatares V3** (`62:262`). Las páginas 00–05 son propuestas anteriores y no se han modificado en este encargo.

Base de los enlaces: `https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING?node-id=`

| Sección | Frame | Nodo |
|---|---|---|
| V3 · 1 · Pantallas móviles (`73-17557`) | Personalizar · Cabeza · 390 | [72-4329](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING?node-id=72-4329) |
| | Personalizar · Pelaje · 390 (**aprobado**) | [72-4805](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING?node-id=72-4805) |
| | Personalizar · Cabeza · 390 · claro | [72-5629](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING?node-id=72-5629) |
| | Personalizar · Cabeza · 360×740 | [72-5153](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING?node-id=72-5153) |
| | Personalizar · Cabeza · 390 · texto ×1,3 | [72-6105](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING?node-id=72-6105) |
| | Mi Ping · 390 | [68-6438](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING?node-id=68-6438) |
| | Conectar · propuesta · 390 | [68-6568](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING?node-id=68-6568) |
| | Conectar · 360×740 · nombre largo | [73-17368](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING?node-id=73-17368) |
| | Lista y cabecera · 390 (32 y 44 px) | [68-6727](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING?node-id=68-6727) |
| V3 · 2 · Componentes (`73-17558`) | Avatar V3 (Animal × Recorte Editor/Mini/Primer plano) | [67-575](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING?node-id=67-575) |
| | Avatar V3 · Base | [66-444](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING?node-id=66-444) |
| | Accesorio · Cabeza / Audio / Cara / Cuello | `66-377`, `66-382`, `66-387`, `66-392` |
| | Accesorio trasero · Audio / Cabeza / Cuello | `66-397`, `66-426`, `66-443` |
| V3 · 3 · Paletas, ejemplos y reglas (`73-17559`) | Paletas y acentos · 16 ejemplos · Reglas | `73-4329`, `73-5400`, `73-8403` |
| V3 · 4 · Tamaños y fondos (`73-17560`) | Tamaños 96·64·48·32, oscuro y claro | `73-6208` |

Variables: «PING Avatares V3 · Pelaje» (modos Natural, Dorado, Lila, Perla, Cacao, Coral; papeles por animal y `fondo/disco`) y «PING Avatares V3 · Acento» (Violeta, Amarillo, Menta, Coral; principal/sombra/claro/detalle). Pantallas: colección «PING V2 · Color» (Oscuro/Claro) y estilos de texto `V2/*` (Manrope).

## Componentes utilizados

- Nuevos V3: los de la tabla. Las piezas son SVG del paquete importados como vectores editables (grupos conservados) con colores enlazados a variables mediante marcadores (`fuentes-v3/preparar_v3.py`).
- Reutilizados de V2 (página `38:262`, sin modificar): `V2 · Cabecera` (42:718), `V2 · Botón` (40:471), `V2 · Navegación`, `V2 · Insignia de estado`, `V2 · Gusto`, `V2 · En común`. Mi Ping, Conectar y Lista son copias de las pantallas V2·6 con el avatar sustituido.

## Terminado

- Paquete V2 extraído y verificado (SHA256SUMS 132/132); plantillas V3 equivalentes al original (24/24 bases, 480/480 renders de accesorios).
- Importación: prueba de una base y un accesorio; después bases y 80 piezas por lotes.
- Correcciones: contorno oscuro en gafas cuadradas; orejas del conejo por encima del sombrero bucket; piezas de cuello ya no se cortan (ilustración escalada dentro del disco); disco de fondo por pelaje para fondos oscuros; recortes Mini (32–44 px) y Primer plano (miniaturas de cabeza).
- Pantallas 390 y comprobaciones 360×740, tema claro, texto ×1,3 (ajuste de estado y cuadrícula a 3 columnas) y nombre largo.

## Decisiones aprobadas por Samuel

- 2026-10-25: aprueba composición, colores, tipografía y jerarquía de «V3 · Personalizar · Pelaje · 390» (72-4805). No se extiende a otros frames ni a la integración en la app (sigue requiriendo DECISION_LOG y aceptación de Julián según AGENTS.md).

## Reservado a Codex (no tocar)

- Corrección de las miniaturas de accesorios de «Cabeza» (cuadrícula de 72-4329, 72-5629, 72-5153, 72-6105) y los componentes compartidos que usan: `Avatar V3` (recorte Primer plano) y `Avatar V3 · Accesorio · Cabeza`. Codex trabaja en una página propia con duplicados; Claude no la edita.

## Pendiente del encargo original (independiente de lo reservado)

- Hecho el 2026-10-25: 12 capturas en `docs/design-proposals/avatares-v2/capturas/`, README en `docs/design-proposals/avatares-v2/`, nota en `docs/ENCUENTRO_CONFIGURABLE_LLEGADA.md` y commit local en esta rama (sin push).
- No queda trabajo independiente del encargo original. Falta la revisión humana del resto de frames y, tras la prueba de Codex, volver a capturar las pantallas de «Cabeza» si cambian sus miniaturas.

## Limitaciones comprobadas

- Sin prueba en teléfono real ni en navegador móvil: las comprobaciones son capturas de Figma.
- Al cambiar «Animal» en una instancia, Figma puede conservar la pieza del animal anterior; hay que volver a elegir piezas.
- Las piezas con parte trasera (auriculares, micrófono, vincha, pase) requieren elegir la misma pieza en la capa «Trasero · …».
- Las gafas de sol ocultan los ojos (propio del diseño). No se han revisado manualmente todas las combinaciones múltiples.
