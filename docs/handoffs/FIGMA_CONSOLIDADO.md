# Figma · Diseño consolidado de PING

Fecha: 2026-10-25. Solo diseño en Figma: sin cambios en la aplicación, el backend, los datos ni `main`. Rama `design/conectar-figma`, HEAD `49e9482` (sin commit de esta tarea).

## Página

**PING · Diseño consolidado · Revisión** — `102:262` — https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING?node-id=102-262

Base de enlaces: `https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING?node-id=`

| Sección | Nodo | Contenido |
|---|---|---|
| 00 · Guía de revisión | `102-278` | Versión de trabajo, leyenda de estados, Figma frente a la app, procedencia y piezas de la página |
| 01 · Sistema visual | `102-282` | Marca C (sola, 48–16 px en oscuro y claro, A·B·C al mismo tamaño), Fundamentos V2 y componentes V2 como instancias |
| 02 · Avatares y personalización | `102-286` | Pelajes y acentos, accesorios por slot, 16 combinaciones, reglas e incompatibilidades, lámina de miniaturas de Codex |
| 03 · Pantallas consolidadas | `102-290` | Recorrido A (llegar e identidad) y B (conectar, encuentro y recompensa) a 390; comprobaciones 360×740, tema claro, texto ×1,3 y escritorio |
| 04 · Antes / después | `102-294` | Mi Ping app→V3, Intereses app→V2, Identidad V1→V2, Personalizador V2→V3, Miniaturas V3→Codex |
| 05 · Pendientes y decisiones | `102-298` | Problemas abiertos, decisiones humanas, lo aprobado y límites |

Cada pantalla lleva encima una ficha «Revisión · Ficha» (`102:272`), fuera de la interfaz, con estado («Revisión · Estado», `102:271`: Aprobado, Propuesta, Pendiente o Referencia histórica), nombre y procedencia.

## Procedencia

Las pantallas son copias editables (`clone`) de los originales; los componentes son instancias de los maestros existentes. No se creó una segunda biblioteca.

- V1 «Festival editorial», páginas 01–04: Inicio, Unirme, Cuenta, Beneficios e Identidad V1. Ver `docs/design-proposals/festival-editorial/README.md`.
- V2, página 05: marca, fundamentos, componentes y pantallas. Ver `docs/design-proposals/v2/README.md`.
- Avatares V3 (Claude): ver [FIGMA_CLAUDE.md](FIGMA_CLAUDE.md).
- Corrección de miniaturas (Codex): ver [FIGMA_CODEX_MINIATURAS.md](FIGMA_CODEX_MINIATURAS.md). Se reutilizan sus pantallas y su componente `Avatar V3 · Codex` (`87:1968`) sin volver a escalar.
- Capturas de la app (solo en 04): dos imágenes históricas copiadas de «Antes / después» V1 (`29:2771`, `29:2773`), marcadas como no editables.

Variables y estilos: «PING V2 · Color» (Oscuro/Claro), «PING Avatares V3 · Pelaje» y «· Acento», «PING · Color» (V1) y estilos de texto `V2/*` en Manrope. Las superficies de documentación usan el modo Claro; cada copia conserva el modo de su origen.

## Cambios hechos al consolidar

- Página, secciones, fichas y textos de revisión nuevos. Las pantallas no se rediseñaron.
- Las copias recibieron el modo de color que heredaban en su página, para no cambiar de tema dentro del lienzo claro.
- Se quitaron 14 interacciones de prototipo de las copias que apuntaban a frames de otras páginas (llevaban a los originales); se conservaron 4 internas.
- Tabla de accesorios por slot montada con instancias del `Avatar V3` original (Gato, Natural, Violeta, recorte Editor).
- logo-design (kaankiziltug/logo-design-skill 1.4.5, `734090317a21`, registro `.agents/skill-installation.json`): según `references/presentation-delivery.md`, la marca se muestra sola, a tamaño pequeño, sobre fondo oscuro y claro, y comparada con A y B al mismo tamaño. La recomendación C sigue marcada como propuesta.

## Aprobaciones

- Aprobado (Samuel, 25 oct): «Personalizar · Pelaje · 390» (composición, color, tipografía y jerarquía) y encuadre de miniaturas al 84 % de Codex.
- Todo lo demás es propuesta o referencia histórica. No hay aprobación de Julián. Nada autoriza integrar sin DEC en `DECISION_LOG.md`.

## Verificación

- 12 693 nodos; 1 323 textos, todos en Manrope; 2 372 instancias y ninguna rota; 5 183 nodos con variables vinculadas.
- Imágenes: solo las dos capturas históricas y la ilustración de Inicio V1 («Arte · convención social»).
- Las cinco copias de Codex tienen 8 miniaturas enlazadas a `Avatar V3 · Codex`, Primer plano al 84 %, posición 8 %; las de referencia siguen al 130 %.
- Originales intactos: la página Codex conserva sus 8 nodos y sus 8 miniaturas al 84 %. La página Avatares V3 conserva sus 4 secciones. El maestro original de Primer plano sigue en 332,8 px.
- Capturas en [figma-consolidado/](figma-consolidado/).

## Auditoría final — sección 06 (`109-10363`)

Impeccable se aplicó leyendo `SKILL.md`, `reference/audit.md` y `reference/craft-floor.md`. No se ejecutó su motor ni `scripts/impeccable`, que CLAUDE.md prohíbe; los hooks siguen desactivados. Las mediciones se hicieron por script sobre las copias de la página, con las variables resueltas por modo.

- **Contraste:** 813 de 813 textos cumplen 4,5:1 (texto normal) o 3:1 (texto grande).
- **Recortes:** sin recortes accidentales. «Cara» asoma a propósito en la tira desplazable de pestañas, y bajo el pliegue de 360 el contenido se desplaza.
- **Navegación:** las 14 pantallas con barra marcan su propia pestaña.

Correcciones, solo en copias consolidadas. Cada una tiene su antes/después en 06:

1. **Personalizador · áreas táctiles** (`109-10368`). Afecta a Cabeza 390, 360, claro y ×1,3. «Deshacer» pasa de 62×16 a un área de 44 px; todas las pestañas miden 46 px; cada color es un destino de al menos 48×54. El encuadre aprobado no cambia.
2. **Insignia a ×1,3** (`109-10470`). En el Encuentro V2 a 360, el nombre largo pasa a una segunda línea en lugar de truncarse.
3. **«Ninguno» a 360** (`109-10540`). En la Identidad V2, el ✓ va sobre la etiqueta, que ya no se corta. Incluye un detalle ×2 de la opción en ambos lados.

No se corrigieron, a propósito:
- La copia aprobada de Pelaje: tiene el mismo problema táctil, pero cambiarla alteraría un frame aprobado. Queda pendiente de su aprobación.
- Las parejas de 04 y los «antes» de 06, que se conservan como evidencia.

Problemas anteriores revisados:
- **ACTIVE F08:** resuelto en el diseño V2.
- **Navegación F05:** coherente.
- **Texto ampliado F12:** corregido en la insignia.
- **Beneficios F01:** resuelto en Recibo V2. El catálogo de Beneficios sigue solo en V1.
- **Administración F10, F11 y F15:** no hay diseño en Figma y no se crearon pantallas nuevas.

### Cierre: Pelaje y límites de interacción

- **Pelaje 390, propuesta** (fila `112-11701`). Copia aprobada sin cambios (`112-11712`) junto a una copia nueva, marcada como Propuesta (`112-11776`), con «Deshacer» a 78×44 y todas las pestañas a 46 px. El frame aprobado de 03 (`103-23545`) y el original de la página V3 (`72-4805`) siguen intactos.
- **Límites medidos:**
  - «Deshacer»: 78×44 (97×44 a ×1,3), visible completo y dentro de la pantalla. Queda a 10 px del estado en la misma línea, o en la línea siguiente a 360 y ×1,3.
  - Pestañas: 46 px de alto (49 a ×1,3), entre 65 y 84 px de ancho (hasta 96 px a ×1,3) y 8 px de separación. Corregido tras la verificación de Codex, que midió «Cara» en 65 px.
  - La pestaña que asoma («Cara») deja 35 px tocables a 390 y 5 px a 360; el resto solo es tocable tras desplazar.

| Problema | Antes | Después | Captura |
|---|---|---|---|
| «Deshacer», pestaña activa y muestras de color pequeñas | 62×16; 40 px; 36×36 | 78×44; 46 px; ≥48×54 | `figma-consolidado/06-1-areas-tactiles-antes-despues.png` · Pelaje: `06-4-pelaje-aprobado-vs-propuesta.png` |
| Insignia truncada a ×1,3 | 1 línea de 16 px de alto, nombre cortado con «…» | 2 líneas (32 px), nombre completo; insignia de 202×44 | `figma-consolidado/06-2-insignia-texto-130-antes-despues.png` |
| «Ninguno» cortado a 360 | ✓ y nombre: 73 px en una opción de 69 px | 55×32, ✓ encima del nombre | `figma-consolidado/06-3-ninguno-360-antes-despues.png` |

**Criterios pendientes del 13/16:**
- Accesibilidad (−1): Pelaje aprobado conserva sus áreas pequeñas hasta que se apruebe la propuesta; no hay prueba de lector de pantalla ni de foco por teclado.
- Adaptación (−1): falta una señal de desplazamiento más clara en las pestañas y no hay versión ×1,3 de Pelaje.
- Integridad (−1): los pasos V1 sin versión V2 y la Identidad V2 con avatar V2.

**Pruebas pendientes en dispositivo real:**
- tocar «Deshacer» y las pestañas con el dedo;
- deslizar la tira de pestañas en iOS y Android;
- texto ampliado del sistema;
- lector de pantalla (VoiceOver y TalkBack);
- teclado y foco;
- Safari;
- áreas seguras.

Nada de esto se ha comprobado.

Puntuación adaptada: 13/16. El rendimiento no aplica a un lienzo de Figma. Los estados de aprobación no cambiaron: 5 fichas «Aprobado», las mismas que antes de la auditoría.

## Límites y siguiente paso

- Revisión sobre capturas de Figma; sin teléfono real, Safari ni lector de pantalla.
- Pasos solo en V1 (Inicio, Unirme, Cuenta, Beneficios, errores, plano, encuentro completado): no tienen versión V2.
- La Identidad V2 todavía muestra el avatar V2.
- `impeccable` y `redesign-existing-projects` son de invocación manual: no se ejecutaron sus flujos.
- **Siguiente paso:** revisión humana de la página. Si se adopta, hay que registrar la DEC y abrir un Issue antes de tocar código.

## Verificación independiente de Codex

Codex hizo una verificación de solo lectura en Figma mediante Tandem (`docs/handoffs/figma-consolidado/VERIFICACION_CODEX.md`). Confirmó las tres correcciones, los valores antiguos de los originales y el contraste: 175 segmentos, mínimo 5,70:1. También corrigió el rango de ancho de las pestañas. Solo creó su informe. Revisé sus medidas contra las mías: coinciden.

## 07 · Conectar · pareja de avatares (DEC-057)

Sección `120-12263` de esta página: https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING-%C2%B7-Dise%C3%B1o-m%C3%B3vil?node-id=120-12263. Es una **propuesta, sin aprobar**.

- **Frames:** «antes» V3 (`120-12269`), propuesta recibida (`120-12296`), esperando (`120-12505`), conexión confirmada (`120-12715`), 360 con nombre largo (`120-12928`), 390 en tema claro (`120-13138`) y especificación (`120-13348`).
- **Construcción:** copias de la pantalla V3 de Conectar con las instancias `Avatar V3` del original, aros y vínculo enlazados a variables V2 (`v2/interactivo`, `v2/marca/sol`) y resplandor radial.
- **Movimiento:** Figma muestra los estados estáticos y un fotograma de la onda; el movimiento solo funciona en la aplicación.

**Implementación en la app.** Rama `feature/connect-avatar-pair`, decisión DEC-057 y resumen en `DESIGN.md`. La app usa los avatares preset actuales; la geometría coincide con Figma. Evidencia en `docs/design-audit/evidencia/DEC-057/`.

**Entorno de las pruebas.**
- App real, con MySQL 8.4.9 privada y desechable (puerto 3399, base `ping_avatar_pair_exclusive`).
- Chrome sin interfaz controlado por CDP, con emulación móvil. No es un teléfono físico.
- Dos contextos aislados: Ana y la participante de demostración renombrada «María Fernanda de los Ángeles».

**Resultados comprobados.**
- Círculos iguales: 105,3 px a 390, 97,2 a 360 y 112 en escritorio. Sin desbordamiento horizontal, también a ×1,3.
- Imágenes dentro del círculo y sin animación.
- Movimiento reducido: sin animaciones.
- Recurso bloqueado: se muestra la inicial y se conserva el tamaño.
- Recarga en espera: mantiene el estado y no hay onda.
- Triple pulsación en «Aceptar»: un único POST.
- Aceptación bilateral: onda en ambas sesiones; en la de Ana llega por la recarga existente de `live.js`, 2,2 s después.
- Recarga y reentrada en ACTIVE: aro estable sin onda.
- Rechazo: la otra sesión pasa a «Cancelada» sin pareja ni animaciones.
- Botones visibles y sin nada encima a 390, 360 y escritorio.

**Rendimiento.** Mismo entorno y estado de espera, 60 s:
- Saltos de diseño (CLS) 0 y ninguna tarea larga, antes y después.
- Hilo principal: 0,066 s antes frente a 0,072 s después.
- Respuesta al toque: unos 10 ms en ambos casos.
- Listeners y nodos estables al entrar y salir cinco veces: 187/3121 antes y 211/3145 después, con 6 listeners más, uno por imagen.
- Composición, viz y GPU en 5 s: unos 4 ms antes frente a 181 ms después.

El giro continuo costaba unas 8 veces más en composición; se pasó a 120 pasos.

**Pruebas.** Pasan las 7 clases afectadas (51 pruebas) y `./mvnw -Dspring.profiles.default=test clean verify`: 267 pruebas, 0 fallos.

**Pendiente.**
- Teléfono físico, Safari iOS, texto ampliado del sistema real y lector de pantalla.
- Evento cerrado o en pausa con aceptación previa: la lógica está en la plantilla, sin prueba en navegador.
- La onda no se ve en una pestaña que no estaba en OFFERED, por ejemplo otro dispositivo; esa pestaña muestra directamente el aro estable.

### Entrega para revisión (DEC-057)

**Rutas de la app.**
- `/matches/{id}`: propuesta recibida, espera tras aceptar y, cuando aceptan los dos, la pantalla ACTIVE con el aro.
- `/connect`: lleva a la conexión en curso.

**Cómo llegar en local.**
1. Arrancar con `PING_DEMO_ENABLED=true` y entrar con una cuenta general.
2. `/demo/prepare` y después `/demo` para elegir participante.
3. Activar la disponibilidad en las dos sesiones y pulsar «Buscar conexión» en una de ellas.

**Evidencia.** Carpeta `docs/design-audit/evidencia/DEC-057/`, con la propuesta `/matches/3` de la base desechable.
- `rejilla-propuesta.png` y `rejilla-esperando.png`: 390 y 360, claro y oscuro, ×1,3 a 390 oscuro y a 360 claro, con «María Fernanda de los Ángeles». Las capturas individuales son `app-*.png`.
- Avatares actuales: los 15 recursos (8 animales, 4 sombreros, 3 accesorios) dibujados al 84 % dentro del círculo no pintan ningún píxel fuera de él. En las 12 capturas, cada capa está cargada, es cuadrada y queda dentro del círculo. El desborde horizontal es 0.
- `demo-1-espera-aceptacion-recarga.gif` y `grabacion-1-fotogramas.png`: espera → la otra sesión acepta → ACTIVE con onda → recarga sin onda. Se midió `celebraTrasCambio: true` y `celebraTrasRecarga: false`.
- `demo-2-espera-rechazo.gif`: espera → la otra sesión rechaza → «Cancelada» sin pareja. Las 3 animaciones que hay al cargar son entradas que la página ya tenía (`rise`) y terminan antes de 1,2 s.
- Movimiento reducido en espera: 0 animaciones. En 5 s, unos 4 ms de composición, viz y GPU, igual que sin el cambio. Hilo principal: 0,036 s en 30 s.
- Errores de consola o de red en toda la sesión de entrega: 0.

**Avatares.** La app usa los avatares preset actuales: 8 animales, 4 sombreros, 3 accesorios y 6 fondos. Figma usa los avatares V3 propuestos, que no están integrados. Solo comparten la geometría de la pareja.

### Aclaraciones del cierre (25 oct)

**Giro.** Comparación medida en una página aislada, con los mismos aros que la app y dos mediciones de 5 s por variante. Archivos: `comparacion-giro-fluido-vs-pasos.gif` y `giro-coste.json`.

| Variante | Composición, viz y GPU cada 5 s | Salto por paso |
|---|---|---|
| Fluida (60 fps) | ~1 020–1 155 ms | — |
| 5 pasos/s (versión anterior) | ~143–250 ms | 3° (~3 px) |
| 10 pasos/s | ~170–176 ms | 1,5° (~1,5 px) |
| **15 pasos/s (elegida)** | ~200–208 ms | 1° (~1 px) |
| Sin giro | ~1–8 ms | — |

Se eligió `steps(360)` en 24 s: a esta velocidad se ve continuo y cuesta una quinta parte del giro fluido. En la app, en espera: composición ~201 ms cada 5 s, CLS 0, sin tareas largas ni recursos fallidos, hilo principal 0,095 s en 60 s (`rendimiento-final-15-pasos.json`). Las capturas estáticas y las grabaciones anteriores son de la versión de 5 pasos/s; solo cambia la cadencia del giro.

**Listeners.** El «+24» de la tabla anterior es el contador global de Chrome (`JSEventListeners`), que suma todos los documentos del proceso, también los que la caché de atrás/adelante guarda al navegar. Medido con DevTools sobre el documento actual (`DOMDebugger.getEventListeners`):
- El documento tiene 28 listeners, de los cuales 6 son de la pareja: un `error` por imagen de avatar, para mostrar la inicial si falla. No cambia tras 5 entradas y salidas.
- Con la caché activada, el contador global sube a 211 y se estabiliza.
- Con la caché desactivada (`--disable-features=BackForwardCache`), el contador global se mantiene en 28 en las 5 reentradas.

Los 24 de diferencia son esos 6 por página multiplicados por las 4 copias que guarda la caché, no una acumulación. Archivos: `listeners-con-bfcache.json` y `listeners-sin-bfcache.json`.

**Pruebas sobre la versión final.** La optimización a 5 pasos/s ya estaba incluida cuando pasaron las 267 pruebas anteriores. El cambio a 15 pasos/s es posterior, así que se repitió todo sobre la versión final:
- la verificación completa (`clean verify`, 267 pruebas, 0 fallos);
- `node --check` del JS;
- en la app real: giro `steps(360)` en sentidos opuestos, onda al aceptar los dos (`true`) y ausencia de onda tras recargar (`false`).

**Arranque para revisar la pantalla** (PowerShell, en la rama `feature/connect-avatar-pair`):

```powershell
cd C:\Users\river\PING-movil
git branch --show-current          # debe mostrar feature/connect-avatar-pair
$env:DB_URL = "jdbc:mysql://127.0.0.1:3306/TU_BASE"   # base MySQL propia; sin usuario ni contraseña en la URL
$env:DB_USERNAME = "TU_USUARIO_MYSQL"
$env:DB_PASSWORD = "TU_CONTRASEÑA_MYSQL"
$env:PING_DEMO_ENABLED = "true"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8091"
```

- Espera a `Started PingApplication` y abre `http://127.0.0.1:8091`.
- La demo exige una cuenta general. Si la base es nueva, la primera vez añade `PING_BOOTSTRAP_EMAIL` y `PING_BOOTSTRAP_PASSWORD` (ver `docs/ACCOUNTS_SETUP.md`) y retíralos después.
- Usa una base de prueba: los pasos siguientes cambian disponibilidad y conexiones de los participantes de la demo.

**Pasos para reproducir la pantalla:**
1. Ventana A: `http://127.0.0.1:8091/login`, entra con la cuenta general.
2. A: abre `/demo/prepare` y envía el formulario («Escenario demo listo»).
3. A: abre `/demo`, elige «Ana» y entra.
4. Ventana B, en otro navegador o en una ventana privada para tener otra sesión: repite el paso 1 y elige «Carlos» en `/demo`.
5. Opcional, para probar accesorios: en cada ventana, `/me/avatar` y elige animal, sombrero y accesorio. En la evidencia, el alias largo «María Fernanda de los Ángeles» se puso con SQL en la base desechable; la demo no tiene editor de alias.
6. En A y en B: `/connect` y «Activar disponibilidad».
7. A: «Buscar conexión». Se abre `/matches/{id}` con la propuesta: dos círculos iguales y aros quietos.
8. A: «Aceptar conexión». Aparece «Esperando respuesta de Carlos…» y los aros giran.
9. B: `/connect`, que lleva a la misma `/matches/{id}`, y «Aceptar conexión». B pasa a ACTIVE con la onda; A pasa sola en 5 s como máximo, también con la onda.
10. Recarga A o B: aro estable, sin onda.
11. Salida por rechazo: repite los pasos 6 a 8 con una propuesta nueva y pulsa «Ahora no» en B. A pasa a «Cancelada» sin pareja.

**Pendiente:**
- teléfono físico, Safari de iOS, texto ampliado del sistema real y lector de pantalla;
- evento cerrado o en pausa después de aceptar, sin prueba en el navegador;
- una pestaña que no estaba viendo la propuesta, por ejemplo otro dispositivo, muestra el aro estable sin onda.

**Avatares.** La app usa los preset actuales; Figma, los V3 propuestos y no integrados. Todo sigue sin commit y como propuesta pendiente de revisión visual.

## Página de selección para implementación

**PING · Selección final para implementación** (`133:262`): https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING-%C2%B7-Dise%C3%B1o-m%C3%B3vil?node-id=133-262

- **Decisiones:** D01–D14, con identificadores estables, y opciones Dxx-1, Dxx-2… Se mantienen en ajustes posteriores.
- **Fichas:** copias editables de la página consolidada y de la sección 07, instancias de la marca, y dos capturas reales de la app identificadas (D10 y D11).
- **Recomendaciones:** marcadas pero sin seleccionar.
- **Etiquetas:** componente «Selección · Etiqueta» (`133:279`).
- **Alcance:** la sección 08 resume el alcance por decisión y separa las funciones nuevas: datos del avatar V3, «Retomar contacto» (sin diseño ni especificación) y administración (sin diseño).
- **Capturas:** `docs/handoffs/figma-seleccion/`.

No se han modificado aprobaciones ni código.

## Implementación DEC-058 (D01–D14)

Rama `feature/seleccion-implementacion`, sin commit. **Selección:** Samuel, 25 oct 2026. **Implementación:** Claude Code (este bloque). **Validación final:** pendiente de revisión humana. No se atribuye aprobación a Julián.

| Decisión | Estado | Dónde |
|---|---|---|
| D01-1 Sistema V2 | Implementado | `static/css/v2.css` (tokens claro/oscuro, botones, campos, foco) cargado tras `brand.css` |
| D02-1 Marca C | Implementado | `fragments/layout :: brand` (SVG en línea), `img/favicon.svg`, `favicon-32.png` |
| D03-1 Avatares V3 | Implementado | `AvatarV3`, `Avatar.v3`, `Participant` (V23), `img/avatars/v3/` |
| D04-1 Personalizador | Implementado | `entry/avatar.html`, `entry/avatar-edit.html`, `js/avatar.js` |
| D05-1 Miniaturas 84 % | Implementado | `v2.css` (`inset:8%`, 84 %), sin escalar de nuevo |
| D06-1 Llegada | Implementado | `home.html`, `entry/event.html`, `account/register.html`, `account/login.html` |
| D07-1 Identidad | Implementado | `entry/details.html` |
| D08-1 Intereses | Implementado (composición corregida tras la auditoría A02) | `entry/interests.html`, `interests/form.html`, `interests/selector.html`, `interest-selector.js`; mundos en pestañas, grupos por familia y pie con resumen; mismos checkboxes, taxonomía y búsqueda de DEC-045/056 |
| D09-1 Mi Ping V3 | Implementado | `demo/dashboard.html` |
| D10-1 Pareja | Conservado DEC-057, verificado con V3 | `demo/match.html`, `connect-pair.js` (15 pasos/s) |
| D11-2 Encuentro | Conservado | `demo/active-mission.html` sin cambios de este bloque |
| D12-1 Conexiones anteriores | Implementado | `demo/connections.html`, `GET /me/connections`, «Más» y barra lateral |
| D13-1 Recibo | Implementado | `rewards/receipt.html` (saldo actual, `Issued.expiresAt`) |
| D14-1 Beneficios | Implementado | `rewards/benefits.html` (estilos acotados a `.bf3-page`) |

**Datos y compatibilidad.** `V23__avatar_v3.sql` añade `avatar_style` (por defecto `preset`) y diez columnas V3. Los preset se muestran hasta guardar un V3; el editor explica si el animal no tiene equivalente (zorro, perro…) y propone sin guardar («Propuesta · sin guardar»). El formulario antiguo (sin `palette`) sigue aceptándose y conserva las columnas V3. Validación en servidor: catálogo cerrado e incompatibilidad cabeza voluminosa + audio (400 sin modificar lo guardado).

**Revisión de Codex incorporada.** (1) Confirmado y corregido: tras un 400 el editor presentaba el borrador como guardado; ahora `data-saved` lleva la selección persistida y Deshacer vuelve a ella. (2) Confirmado y corregido: «Verificado» solo si `verified && otherVerified`; si no, «Completada». (3) Aplicado: «Código válido hasta» y el aviso usan `Issued.expiresAt`; se conserva «Tu saldo actual». (4) D14 ya seguía el mapeo; estilos acotados a `.bf3-page`. Pruebas dirigidas añadidas para (1) y (2).

**Registro de tareas.** Codex `job-120ae8e2`: `AvatarV3Tests.java` (revisado; una aserción de redirección corregida). Codex: revisión `CODEX_APOYO_IMPLEMENTACION.md` (solo lectura). Claude: resto del código, capturas y verificaciones.

**Evidencia** en `docs/design-audit/evidencia/DEC-058/`: comparativas Figma | app por decisión, `demo-personalizador.gif`, `demo-recorrido-conexion.gif`, JSON de recorrido, accesibilidad y rendimiento.

**Diferencias justificadas respecto a Figma.** D08 adopta desde la auditoría A02 la composición de `133:1703` (pestañas de mundo, grupos por familia, pie con resumen); la búsqueda devuelve la lista por categorías y sin JavaScript se ve la lista completa. D09 mantiene la tarjeta de propuesta/encuentro, «Cómo reconocerme» y el acceso al historial. D13 no muestra «Saldo después» (no se guarda) ni fechas de entrega inventadas. D06 conserva el pie con créditos y los accesos «Explorar eventos», «Organizo un evento» y la demo. Avatares preset en Mi Ping conservan su forma cuadrada hasta guardar un V3.

**Pendiente.** Revisión humana; prueba en teléfono real/Safari y lector de pantalla; limpieza opcional de las reglas `.hm-*` ya sin uso en `brand.css`.

**Correcciones de la auditoría D01–D14 (25 oct 2026).** Resolución detallada en `AUDITORIA_IMPLEMENTACION_D01_D14.md` (sección añadida al final). A01 no se reproduce en H2 ni en MySQL; se registra la excepción del 503 para diagnosticarlo. A02 (D08) y A03 (recibo `DELIVERED`) quedan corregidos. Verificación posterior: 310 pruebas, 0 fallos, BUILD SUCCESS.

**Verificación de la versión final (25 oct 2026).** `./mvnw -Dspring.profiles.default=test clean verify`: 309 pruebas, 0 fallos, 0 errores, BUILD SUCCESS. Incluye las 40 de `AvatarV3Tests` y las dos nuevas de `VerificationTests` (etiqueta «Verificado»). Dos pruebas existentes (`DemoImprovementTests`, `FinalMvpTests`) se reorientaron de `/me` a `/me/connections` porque D12 traslada allí el historial, con las mismas comprobaciones (completadas, feedback, actividad y puntos). Recorrido con dos sesiones en MySQL local: propuesta, rechazo, aceptación bilateral con doble envío, ACTIVE, llegadas, verificación cruzada, confirmación (+10), historial, canje con doble envío (una solicitud y un débito) y recibo con código, sin errores JS.
