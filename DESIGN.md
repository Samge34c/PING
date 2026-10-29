# Hub SOFA — Festival Social

Implemented scope: only `/events/{id}/hub`, approved second composition (Samuel, DEC-031). Operate surface with an expressive event cover. Other routes keep their shared layout.

- Existing local Manrope and `--ping-*` theme tokens; Hub-only plum #251333, yellow #f8e956, purple #9b79d2. Fixed plum cover with light lettering works in both themes; operational areas follow the selected theme.
- Context and real next action before the cover. Split message/social illustration on desktop; compact sequence on mobile. Source and optimization: `docs/HUB_ART_PROVENANCE.json`. Image reserves 1536×1024 dimensions and a fixed crop; no lazy loading of visible art.
- Gamer yellow and larger; Cosplay purple poster, Communities compact, technology/music/cinema with specific original mini SVG art. On mobile the Gamer name comes first; the duplicate section map link is omitted because the contextual map action stays above the cover. Active meeting marker compares zone IDs derived from `experience.atPoint`, never pavilion numbers.
- Header/mobile navigation are exclusive fragments. Mobile scrollport reserves 68px plus safe area for its own bottom navigation. The Hub omits the shared main-entry animation. Controls keep at least 44px height and width; keyboard focus and reduced motion are explicit.
- Real alias, balance, event and match states; no fabricated activity. Academic/unofficial notices stay visible. No private recognition hints on Hub.

Editable sources: `templates/experience/hub.html`, `experience/hub-fragments.html`, scoped Festival Social section at end of `static/css/brand.css`. No new framework or dependency.

## Encuentro ACTIVE — DEC-032

Superficie operativa derivada del Hub, sin copiar su portada. Manrope y variables `--ping-*` existentes; palabra amarilla sobre morado, acción amarilla, estructura sin tarjetas repetidas ni ilustración nueva. La navegación compartida se conserva.

- Escritorio: persona/estado compactos; punto y plano interactivo a la izquierda; palabra, pista privada autorizada, actividad y confirmación a la derecha.
- Móvil/tablet: orden de lectura del DOM palabra/pista → lugar/plano compacto → actividad → confirmación. Texto sin truncar, controles ≥44 px, sin acción fija ni sticky que tape contenido. La barra inferior existente conserva su reserva de espacio y área segura.
- El SVG del recinto se reutiliza con marcadores enlazados al mapa real y zoom/centrado local. Los enlaces preservan zona y matchId (Música y Tecnología comparten pabellón pero no ID). Sin modificar el mapa externo ni sus rutas.
- Confirmación propia/ajena explícita; espera unilateral no equivale a puntos ni finalización. Saga pendiente conserva aviso y reintento. Formularios, live.js, pista/editor y permisos existentes intactos.

Fuentes editables: `templates/demo/match.html`, `templates/demo/active-mission.html`, `static/js/active-map.js` y sección CSS DEC-032. OFFERED/COMPLETED/CANCELLED mantienen su comportamiento y composición previa.

## Mi Ping — DEC-033

Pantalla operativa `/me`, diseñada a 390 px y comprobada después a 360 px; misma Manrope y paleta Festival Social que Hub/ACTIVE, sin ilustración ni portada nueva. Un solo panel morado para estado real y acción, botón amarillo, identidad/saldo compactos. Orden DOM: identidad → acción → intereses → editor privado propio opcional → historial. Escritorio aprovecha el espacio lateral para intereses y pista, conservando el orden de lectura.

Encuentro actual presentado una sola vez; historial desplegable con encuentros anteriores, detalles reales accesibles en sus enlaces. Pista ajena y palabra del encuentro ausentes de Mi Ping. Formularios, versión, CSRF, live.js y fragmento compartido de pista conservados. CSS limitado a `.mi-ping-page`/`.mp-*`, sin cambiar navegación compartida. Hooks Impeccable desactivados.

## Beneficios — DEC-034

Pantalla operativa diseñada a 390 px: cabecera compacta y enlace temprano a Mis canjes, cupón amarillo con saldo/coste/faltantes contiguos, acción y condiciones antes de cualquier promoción. La perforación geométrica pequeña identifica el cupón sin ilustración ni portada. Interiores siguen variables de tema; Manrope, amarillo, negro y morado conservan Festival Social.

Orden móvil: catálogo real por puntos → solicitudes y entrega → promoción ficticia, con avisos académicos. Desktop aprovecha el lateral para Mis canjes. Estados de saldo insuficiente, saldo suficiente y Saga pendiente explícitos; solicitud/entregado/incidencia muestran datos reales. El fragmento promocional compartido y las reglas de desbloqueo no se modifican. Formularios, CSRF, requestKey e idempotencia del backend intactos; CSS limitado a `.benefits-page` y `.bf-*`.

## Home — DEC-035

Entrada funcional diseñada desde390 px. Mensaje breve y acción antes del arte; contexto explícito de demo académica SOFA, sin confundirlo con el evento de una sesión. Invitado: entrada demo y explorar eventos. Con sesión: Mi Ping y resolución real de su evento mediante `/me/event`; selección demo, exploración y preparación siguen accesibles.

DEC-063 (propuesta, 29 oct 2026): Inicio usa la ilustración propia «Encuentro editorial» (HOME_ART_PROVENANCE.json); la del Hub no cambia. Se retira «Explorar eventos» (ruta solo de organización). La barra de visitante Inicio · Unirme · Entrar continúa en código, acceso y registro y se oculta mientras se escribe.

Escena social del Hub reutilizada sin alterar el archivo (ver HUB_ART_PROVENANCE.json), recorte compacto que conserva personas/manos, tamaño reservado y sin carga diferida. Secuencia enlazada con iconos locales e intereses → aceptación bilateral → encuentro/actividad/confirmación; sin tarjetas repetidas ni instrucciones duplicadas. Desktop amplía el arte al lateral del flujo móvil. Variables existentes, Manrope y Festival Social en ambos temas; sin navegación compartida nueva ni dependencias.

## Continuidad del recorrido móvil — DEC-036

Corrección operativa, sin rediseñar las cinco pantallas: entrada demo con un selector nativo, encabezado único y formulario temprano; ilustración antigua solo en escritorio con etiquetas separadas. COMPLETED agrupa recompensa/saldo y adelanta enlaces existentes de continuación antes del feedback opcional. CSS acotado a `.demo-entry-page` y `.completed-encounter-page`; ACTIVE/OFFERED/CANCELLED y navegación compartida conservados. Misma Manrope, variables y controles Festival Social; sin recursos nuevos. Evidencia y límites en docs/REVISION_MOVIL_INTEGRADA.md.

## Selección de intereses móvil — DEC-037 (parte visual)

Editor existente del participante: dos columnas de opciones táctiles completas, icono/nombre sin descripciones repetidas, selección por superficie/borde y check nativo, foco visible y contador singular/plural real. Título/contexto compactos, retorno arriba y un único botón principal. CSS `.participant-interests-page`/`.pi-*`, Manrope y variables existentes en ambos temas; controles≥44 px y reserva de navegación compartida. No se modifica administración ni el contrato del formulario (CSRF, versión e IDs). El enlace funcional de alta sigue pendiente de la decisión descrita en DEC-037; este editor no lo sustituye.

## Alta propia operativa — DEC-038

`/join` y `/join/interests`: flujo móvil390 con cabecera local compacta (marca/tema), alias/evento y opciones táctiles reutilizadas de DEC-037; sin portada nueva ni menú administrativo antes de entrar. Una acción principal por paso; corrección mediante POST conserva selección y borrador, con mensajes recuperables.360 claro conserva controles y acción; escritorio centra el mismo flujo sin nuevas columnas administrativas. Festival Social, Manrope e iconos locales existentes; ninguna ilustración nueva.

Home/nav de invitado llevan a entrada normal; demo permanece separada. En modo participante, etiquetas/salida y menú omiten preparación/admin como presentación, sin cambiar rutas ni permisos. Editor propio vuelve a Mi Ping. Sin rediseñar las otras pantallas ni la navegación del Hub.

## Resultado de confirmación visible — DEC-039

Después del POST de confirmación, fragmento nativo `#am-confirm-title` y encabezado con `tabindex=-1` en ACTIVE/COMPLETED. El resultado recibe foco y queda en vista sin añadir bloques, CSS o JavaScript, ni desplazar automáticamente a quien aún está leyendo la misión. El polling existente conserva los cambios de estado de la otra persona. Evidencia390/360 y límites en docs/CONEXION_DOS_ALTAS.md.

## Conectar y entrada identificada — DEC-040

Operación móvil390: código/QR → nombre del evento y alias → intereses. No hay selector repetido del evento; cambio explícito guarda alias/intereses, errores visibles sin perder borrador. Se reutilizan cabecera local, Manrope, controles táctiles y temas existentes; sin ilustraciones/portada nueva. Mi Ping concentra identidad/saldo/intereses/historial y un enlace de continuidad; Conectar concentra disponibilidad/búsqueda y resuelve propuesta/ACTIVE. Navegación compartida y del Hub apuntan a `/connect`; selección activa coherente. QR negro/blanco con quiet zone, código textual y enlace seleccionable/copiable accesibles en el detalle, sin nueva estética administrativa. Evidencia y configuración: docs/CONECTAR_CODIGO_QR.md.

## Llegada y configuración — DEC-041

ACTIVE conserva palabra/pista/lugar y añade llegada propia antes de actividad/confirmación; estado de ambas personas y una acción «Ya llegué» con foco nativo tras POST. Actividad visible desde aceptación; confirmación final disponible tras ambas llegadas en encuentros nuevos. Sin cambios a mapa/portada/Hub. Editor de evento reutiliza formulario y controles existentes con listas por línea y límites explícitos, sin dashboard nuevo.

## Corrección operativa DEC-042

Llegada opcional, confirmación siempre accesible tras aceptación bilateral; mensaje explícito de que no condiciona actividad ni premia. Indicaciones del punto y título de actividad guardados en la misión. Editor estructurado usa formularios/controles locales existentes y etiquetas claras de compatibilidad/estado, sin nueva portada o dashboard.

## Puntos y recompensas por evento — DEC-043

Misma composición móvil de Beneficios, saldo/coste/faltantes juntos, lugar e indicaciones explícitos y snapshot en Mis canjes. Recompensas propias identificadas como del evento; snack global conserva etiqueta DEMO. Editor operativo local sin portada nueva; importe de actividad visible en misión, resultado, historial y admin. Sin cambios en navegación compartida/Hub ni recursos externos.

## Recorrido integrado — DEC-044

Ajustes observados, sin rediseño: recompensa local primero, continuidad de búsqueda a Conectar y llegada informativa sin suponer «en camino». Confirmación nombra actividad y permanece independiente. Indicaciones útiles en puntos nuevos y orientación honesta en antiguos sin plano.390/360 y escritorio, mismos controles/temas/recursos. Evidencia final en documento único de continuidad.


## Categorías y selección compartida — DEC-045

Alta y edición reutilizan el mismo selector móvil: categorías visibles, dos columnas táctiles, resumen de nombres y contador completo, filtro nativo opcional sin pérdida de selección. Búsqueda local a partir de13 opciones; catálogo real de8 sin buscador innecesario. Acción en el flujo después del catálogo para no tapar contenido; sin ilustraciones nuevas, acordeón obligatorio ni cambios de navegación. «Otros intereses» conserva los no agrupados; «Conservados» explica las selecciones inactivas. Formularios nativos/CSRF/versión, foco por teclado y temas Festival Social intactos. Evidencia390/360/escritorio y semántica en el documento único ENCUENTRO_CONFIGURABLE_LLEGADA.md.


## Plano configurable — DEC-046, organizador

Superficie operativa en administración existente: seleccionar punto, tocar imagen y guardar; alternativa numérica0–1 con controles nativos. Preview real con nombre/dimensiones y errores recuperables; reemplazo en sección explícita que advierte recolocación. Visor compartido con zoom/centrar/reset y pan nativo táctil o mouse/teclado; un marcador seleccionado, ancla relativa a imagen y etiqueta1 fuera de los textos del plano. CSS/JS locales acotados, mismos tokens/Manrope/temas; sin portada ni dashboard nuevos. Plano de evidencia ficticio, no oficial SOFA.


## Plano del participante — DEC-046

Acceso compacto desde ACTIVE, sin incrustar otra imagen que desplace palabra/actividad/confirmación; vista operativa con nombre/indicaciones, marcador1 y retorno arriba/abajo. Mi evento usa el snapshot del ACTIVE propio; el mapa público general permite elegir puntos existentes. Sin imagen, texto útil y sin CTA de mapa ficticio; sin marcador, advertencia y centrar deshabilitado. Plano aportado conserva colores originales en ambos temas; controles/tipografía/retorno usan Festival Social.390 oscuro primero,360 claro y1440 examinados; zoom/pan/resize mantienen ancla. SOFA anterior sigue separado de los genéricos y de versiones nuevas.

## Interacción móvil — DEC-048

Misma composición Festival Social. Hover de los botones afectados depende de hover/fine; presión120ms, scale.97 y cubic-bezier(.23,1,.32,1), sin retrasar acciones. Etiquetas usan una marca local de presión que se cancela al desplazar; selección nativa, teclado sin transición y reduced-motion estático. Campos de texto táctiles16px, código18px conservado. Reload automático de misma fase recupera foco por ID sin forzar scroll; sin foco heredado al cambiar de fase ni nueva celebración del resultado. No se cambian plano, áreas seguras o navegación.

## Pareja de avatares en Conectar — DEC-057

Propuesta OFFERED (`/matches/{id}`): dos círculos del mismo diámetro desde una sola variable (`--pair-size: clamp(88px,27vw,112px)`), ilustración preset al 84 % con 8 % de margen, aro discontinuo de 1,5 px en `--ping-primary`, resplandor radial estático con `--ping-glow` y vínculo de 40 px con punto `--ping-accent`. Estados reales: `data-pair-state="offer"` (estático) o `"waiting"` (aceptaste y el evento está abierto: giro de 24 s en 360 pasos (15 por segundo), solo transform). ACTIVE añade un aro continuo al avatar existente; la onda de 0,9 s se reproduce una vez cuando la misma pestaña pasó de OFFERED a ACTIVE (`connect-pair.js`, sessionStorage por conexión). Sin consultas nuevas: el cambio de fase llega por `live.js`. Movimiento reducido: estático. Fallback: inicial del alias si falla una capa. Fuentes: `templates/demo/match.html`, `templates/demo/active-mission.html`, sección DEC-057 de `brand.css`, `static/js/connect-pair.js`.

## Selección final implementada — DEC-058

Sistema V2 en `static/css/v2.css` (mismos nombres `--ping-*`, cargado tras `brand.css`; adaptaciones de pantalla acotadas por clase de página: `.fe-*` llegada, `.id2-*` identidad, `.mp3-*` Mi Ping, `.cx3-*` conexiones anteriores, `.bf3-*` beneficios, `.rc3-*` recibo, `.av3-*` personalizador). Marca C como SVG en línea (`fragments/layout :: brand`). Avatares V3 por capas sobre 256×256 con encuadre al 84 % aplicado una sola vez en CSS; un avatar mostrado carga solo sus capas y el catálogo se compone en diferido únicamente en el editor. Cartel de inicio amarillo también en tema claro (identidad Festival). Detalle y diferencias con Figma: `docs/handoffs/FIGMA_CONSOLIDADO.md`, «Implementación DEC-058».
