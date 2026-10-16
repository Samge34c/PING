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

Escena social del Hub reutilizada sin alterar el archivo (ver HUB_ART_PROVENANCE.json), recorte compacto que conserva personas/manos, tamaño reservado y sin carga diferida. Secuencia enlazada con iconos locales e intereses → aceptación bilateral → encuentro/actividad/confirmación; sin tarjetas repetidas ni instrucciones duplicadas. Desktop amplía el arte al lateral del flujo móvil. Variables existentes, Manrope y Festival Social en ambos temas; sin navegación compartida nueva ni dependencias.
