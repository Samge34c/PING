# Dirección visual común — piloto Conectar (Figma)

> Sustituida por [Festival editorial](../festival-editorial/README.md) (24 oct 2026). Sus frames se conservan en la página «00 · Referencia actual» del archivo Figma.

Estado: **propuesta para elegir, sin implementar.** Rama local `design/conectar-figma` desde `b98ca4f`; sin push, merge ni cambios en `main`, backend, datos, permisos o suscripciones. Integrar en la app exige decisión en `DECISION_LOG.md` y aceptación de Samuel y Julián (AGENTS.md).

Archivo Figma: [PING · Diseño móvil](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING-%C2%B7-Dise%C3%B1o-m%C3%B3vil), página «Page 1».

## Frames (todos examinados en captura)

| Frame | 390 | 360 |
|---|---|---|
| Base visual común (color, tipografía, espaciado, componentes, navegación, iconos, estados) | [7:2](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING-%C2%B7-Dise%C3%B1o-m%C3%B3vil?node-id=7-2) | — |
| A «Señal» · Conectar | [5:2](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING-%C2%B7-Dise%C3%B1o-m%C3%B3vil?node-id=5-2) | [5:105](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING-%C2%B7-Dise%C3%B1o-m%C3%B3vil?node-id=5-105) |
| A «Señal» · Mi Ping | [9:2](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING-%C2%B7-Dise%C3%B1o-m%C3%B3vil?node-id=9-2) | [9:390](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING-%C2%B7-Dise%C3%B1o-m%C3%B3vil?node-id=9-390) |
| A «Señal» · Beneficios | [9:104](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING-%C2%B7-Dise%C3%B1o-m%C3%B3vil?node-id=9-104) | [9:492](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING-%C2%B7-Dise%C3%B1o-m%C3%B3vil?node-id=9-492) |
| B «Cartel» · Conectar | [6:2](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING-%C2%B7-Dise%C3%B1o-m%C3%B3vil?node-id=6-2) | [6:101](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING-%C2%B7-Dise%C3%B1o-m%C3%B3vil?node-id=6-101) |
| B «Cartel» · Mi Ping | [9:201](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING-%C2%B7-Dise%C3%B1o-m%C3%B3vil?node-id=9-201) | [9:589](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING-%C2%B7-Dise%C3%B1o-m%C3%B3vil?node-id=9-589) |
| B «Cartel» · Beneficios | [9:298](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING-%C2%B7-Dise%C3%B1o-m%C3%B3vil?node-id=9-298) | [9:686](https://www.figma.com/design/WCrjKj5eGOZQ8wUaxsEqfc/PING-%C2%B7-Dise%C3%B1o-m%C3%B3vil?node-id=9-686) |

El frame `3:2` es la prueba de escritura anterior; se puede borrar.

**Referencia:** no había una instancia de la app en marcha (Docker/MySQL detenidos), así que la referencia de Conectar es `docs/screenshots/connect-event-entry/05-connect-mobile.png` junto con `templates/demo/connect.html` vigente. Para Mi Ping y Beneficios se usaron las evidencias A1/A6 de `design-audit`. Los datos son los del ensayo ficticio DEC-056: Luna encuentro móvil, 17 PTS, Videojuegos, 1 canje, 1 conexión anterior y el snack DEMO de 20 PTS. No se inventan estadísticas, personas disponibles ni proximidad.

## Inventario breve

- **Participante (prioridad):** home, `entry/*` (código, alias/avatar, intereses), `account/*` (acceso, registro, recuperación), Conectar `demo/connect`, Mi Ping `demo/dashboard`, propuesta/ACTIVE/resultado `demo/match` y `active-mission`, `encounter/*`, Beneficios y recibo `rewards/benefits` y `receipt`, Hub/mapa/plano `experience/*` y `floorplan/participant|viewer`, escáner `verification/scanner`, `errors/message`.
- **Organización (revisión operativa posterior):** `events/*`, `rewards/manage|delivery|form`, `floorplan/admin`, `interests/*`, `participants/*`, `admin/dashboard`.
- **Compartido:** `fragments/layout` (head, icon, theme, brand, nav, message, empty, footer), `fragments/navigation` (bottomNav, moreMenu), `static/img/icons.svg`, avatares preset y `brand.css` (2 913 líneas con prefijos por pantalla: `mp-`, `cn-`, `bf-`, `am-`, `pi-`…).

## Decisiones de la base (comunes a A y B)

- Se usan los tokens `--ping-*` existentes con una función fija por color: **una sola acción amarilla por pantalla**; morado para interacción y marca; plum `#21132F` para la superficie de acción; verde y rojo solo para estados.
- Manrope con escala corta. Mínimo 12 px; la etiqueta de navegación pasa de 10 a 11 px (F05).
- Áreas táctiles ≥ 44 px (F03, F04). Margen lateral de 16 px en todos los anchos.
- Componentes compartidos: botón principal, secundario y de enlace; chip de interés; etiquetas de estado (hecho, pendiente, error y neutro); campo con estado de error; navegación inferior; iconos de `icons.svg` sin librería nueva.
- Las diferencias entre direcciones se limitan a la «firma» de la cabecera de cada pantalla.

## Recomendación: B «Cartel»

- **Claridad:** en Conectar todo cabe en el primer viewport de 390 (838 px) y la acción queda dentro del pase, al alcance del pulgar. En A el radar ocupa unos 200 px antes del título.
- **Personalidad:** el bloque amarillo con titular de cartel prolonga el cupón actual de Beneficios y el lenguaje de festival del Hub. Es reconocible sin ilustración nueva.
- **Sin promesas falsas:** los anillos de A sugieren búsqueda por proximidad, algo que PING no hace.
- **Viabilidad:** hace falta un bloque compartido nuevo (`.ping-poster` + `.ping-pass`) y una variante de cabecera sobre amarillo. El amarillo y el negro funcionan igual en ambos temas, como la portada plum fija del Hub. A es más barata porque reutiliza `.mp-next`, pero cambia menos la percepción.
- **Riesgo y mitigación:** usar el cartel solo en las pantallas de primer nivel (Conectar, Mi Ping, Beneficios y quizá Home). Las pantallas operativas (ACTIVE, recibo, formularios) conservan su superficie para no saturar de amarillo.

## Configuración pendiente (no son problemas visuales)

Ningún diseño resuelve estos puntos:

- Cámara/QR en el teléfono: necesita HTTPS de confianza.
- Correo de verificación y recuperación: necesita SMTP real.
- Inicio de sesión con Google/Facebook: necesita los clientes del propietario.
- Prueba en teléfono físico y Safari.

## Orden de implementación (tras elegir y registrar la decisión)

1. **Base compartida:** tokens por función, botones, chip, etiquetas de estado, campo, navegación inferior (11 px, activo coherente, F05) y objetivos de 44 px. Sin cambiar rutas ni formularios.
2. **Piloto Conectar:** cartel y pase con todos sus estados (disponible, no disponible, evento en pausa o cerrado, búsqueda). Se verifica a 390/360 y en ambos temas.
3. **Mi Ping + Beneficios** con la misma firma; resolver el mensaje duplicado (F14) y el saldo, coste y faltantes sin repetirlos.
4. **Recorrido de encuentro:** propuesta, ACTIVE, resultado y recibo con la base común, sin cartel. Hub y ACTIVE se conservan salvo defectos (AGENTS.md).
5. **Entrada y cuenta:** home, código, alias/avatar, intereses, acceso y recuperación.
6. **Organización:** revisión operativa acotada (F10, F11, F15).
7. **Cierre:** recorrido integrado 390/360 y escritorio, suite pertinente una vez y capturas antes/después.

## Siguiente paso

Samuel y Julián eligen A o B (o B con ajustes). Después se registra la decisión DEC en `DECISION_LOG.md`, se abre el Issue y se empieza por el bloque 1.
