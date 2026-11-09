# Auditoría de recorridos y facilidad de uso de PING

Encargo de Samuel del 8 de noviembre de 2026 (sesión de Claude Code), continuación del cierre DEC-066. No se atribuye aprobación a Julián. **Sin push, merge, despliegue, Render ni Aiven; producción solo lectura.** Documento único de continuidad de esta auditoría.

## Versión evaluada

| Qué | Versión | Cómo se comprobó |
|---|---|---|
| Web pública `ping-b63o.onrender.com` | recursos de **`7f20b49`** (`fix/evento-actividades-live`) | Huellas de `v2.css` (`3e535fc8…`, solo coincide con `7f20b49`), `brand.css`, `ui.js`, `qr-scanner.js` antiguo y ausencia de `cierre.css`. El código Java no es observable desde fuera. |
| Rama de despliegue `origin/deploy/render-demo` | `3a528ca` | `git fetch`. No coincide con la web pública. |
| `origin/main` | `97f9b43` | Muy anterior; no representa el producto. |
| **Auditada (local)** | `4c0f0a6` (DEC-066, sin publicar) → correcciones en **`7b33430`** | Rama `feature/auditoria-recorridos` desde `feature/cierre-usabilidad`, worktree `C:\Users\river\PING-cierre`. MySQL 8.4 aislado (3317), buzón local (1027), app en 8091. |

## Herramientas

- **gstack** (`garrytan/gstack`, MIT, commit `b6155921`, `VERSION 1.98.0.0`): **no instalado**. Leídos `README.md`, `setup.ps1`, `setup` y `qa-only/SKILL.md` en esa revisión. Motivos: (1) **Smart App Control activado** en este Windows (`VerifiedAndReputablePolicyState=1`); gstack documenta que bloquea sus binarios compilados sin firmar (`browse`, base de `/qa-only`, `/qa` y `/design-review`) y las alternativas son WSL o desactivar Smart App Control, un cambio de seguridad global e irreversible sin reinstalar Windows; (2) requiere Bun (ausente); (3) el instalador escribe en `~/.claude/skills/gstack` y `~/.gstack`, sigue un canal con comprobación de actualizaciones y en Claude Code activa hooks de seguridad. Nada se instaló ni configuró. Se aplicó su método: primera pasada solo diagnóstico con evidencia, después corrección con regresión, después comprobación visual antes/después.
- **Navegador:** arnés propio por CDP sobre Chrome sin interfaz (perfiles separados por persona), el mismo del cierre DEC-066; fuera del repositorio.
- **axe-core 4.14.0** (npm, 5 oct 2026, MPL-2.0, sin dependencias, SHA-256 `20c09fe1…`): descargado en el scratchpad e inyectado por CDP; no se añadió al proyecto. Reglas WCAG 2.0/2.1 A y AA. El contraste queda «incompleto» en fondos con degradado: se revisó en capturas.
- Skills del proyecto: `ping-efficient-work`, `systematic-debugging` (fallos de compilación y de prueba). Figma: no hizo falta componer pantallas nuevas (los cambios son de texto, enlaces, posición pegajosa y tamaño); siguen vigentes los marcos de DEC-066 (`188:262`).

## Mapa de recorridos y cobertura real

| Recorrido | Rol | Cubierto en la app real | No cubierto |
|---|---|---|---|
| Inicio, unirse por código (vacío, erróneo, minúsculas), QR | Visitante | Sí (390; QR con cámara simulada en DEC-066) | Cámara física |
| Registro (errores), verificación por correo, acceso (contraseña errónea), recuperación, continuar al evento | Cuenta | Sí, con buzón local | Correo real; **Google**: botón visto en producción (solo lectura); flujo completo no probado (crearía una cuenta real) |
| Identidad, avatar, intereses (editar), Mi Ping, pista de reconocimiento | Participante | Sí (390, 360 claro, 320) | — |
| Búsqueda, propuesta, aceptación, llegada, espera, verificación, cancelar | Conexión | Sí | — |
| Actividades: catálogo, asignada, revisión, puntos | Participante / personal | Sí (DEC-066 y esta pasada) | — |
| Beneficios: catálogo, saldo, canje, recibo, entrega | Participante / personal | Sí | — |
| Organización: tablero, crear evento, editor (lugares, actividades, entrega), plano (carga y colocación), recompensas, revisión, entrega, permisos, agenda, ayuda y opciones, resultados | Organizador | Sí (390; tablero/editor/revisión también a 1366 claro) | — |
| Ayuda, agenda, reto, tarjeta, conexiones anteriores | Participante | Sí | — |
| Teclado (orden y foco visible), movimiento reducido, 320 px (equivalente a ampliación 200 %) | Todos | Muestreo: foco 3 px visible; sin animaciones con `reduce`; sin desbordes a 320 | Lector de pantalla real, ampliación de texto del sistema |

## Hallazgos priorizados (primera pasada sobre `4c0f0a6`)

Tipo: **F** fallo reproducido · **R** riesgo de comprensión inferido · **E** preferencia estética · **C** comprobación pendiente.

| Id | Tipo · prio | Tarea · rol · estado | Reproducción → observado | Problema | Propuesta | Estado · evidencia |
|---|---|---|---|---|---|---|
| A1 | F · P1 | Mi Ping · participante | Tocar el nombre del evento → `/events/1` → «Acceso restringido» | Enlace a una página de organización | Enlazar a la pestaña Evento | **Corregido** · `antes/01b`, `despues/01` |
| A2 | R · P1 | Encuentro · esperando a la otra persona | La única salida («Cancelar conexión») estaba en un desplegable cerrado al final: «Cuidemos este encuentro · Gestionar esta interacción» | No hay salida visible ante ausencia | Enlace «¿No llega o hay un problema? Ver opciones» en la tarjeta de espera, panel renombrado «¿Algún problema? Cancelar, bloquear o reportar», el enlace abre el panel | **Corregido** · `antes/02`, `despues/02`, `03` |
| A3 | R · P1 | Ayuda del encuentro | Paso 5: «Los puntos llegan cuando se completa la confirmación bilateral»; sin instrucción para salir | Instrucción que no coincide con la revisión del personal | Texto según reglas reales y apartado «Si algo no va bien» | **Corregido** · `antes/03`, `despues/03b` |
| A4 | R · P1 | Editor del evento · organizador | Pestaña «Puntos» y «Puntos compatibles» (lugares) junto a «Recompensa · 0–100 puntos» | Mezcla lugares con puntos canjeables | «Lugares», «Lugares de encuentro», «Lugares donde puede hacerse»; en el plano, «lugar» | **Corregido** · `antes/06`, `despues/06`, `07` |
| A5 | R · P1 | Agenda · organizador | «Añadir actividad» / «actividades publicadas» en la agenda | Mezcla agenda con actividades del encuentro | «Añadir a la agenda», «elementos en la agenda» | **Corregido** · `antes/08`, `despues/08` |
| A6 | R · P1 | Tablero · organizador con 2+ eventos | «Mis eventos» volvía al evento más reciente, no al que se estaba administrando | Se pierde el contexto | El tablero recuerda el evento (solo el id en la sesión, comprobando permiso en cada petición) | **Corregido** |
| A7 | R · P2 | Subpáginas de organización | Volver con cinco nombres distintos («Mi evento», «Gestionar evento», «Detalle del evento»…) | «¿Dónde estoy?» | «← Organización» al tablero del evento; el detalle ofrece «Preparar y operar este evento» | **Corregido** |
| A8 | F · P2 | Plano · organizador | El lugar archivado aparecía y contaba como «sin ubicar» | Historia mezclada con lo vigente | Excluir archivados | **Corregido** · `antes/07`, `despues/07` |
| A9 | F · P2 · a11y | Mi Ping | axe `label-content-name-mismatch` (2): avatar y saldo con `aria-label` distinto del texto visible (WCAG 2.5.3) | Voz/lectores | Nombre accesible con el texto visible | **Corregido** (axe sin violaciones) |
| A10 | E · P2 | Mi Ping | «Feria de Bienvenida U ·» con el separador colgado al partir la línea | Ruido | Evento y saldo en dos líneas | **Corregido** |
| A11 | R · P2 | Editar intereses | «Guardar intereses» quedaba bajo la barra inferior (no pegajoso, a diferencia de la entrada) | Elegir y no guardar | Barra pegajosa sobre la navegación | **Corregido** · `antes/04`, `despues/04` |
| A12 | R · P2 | Encuentro · panel de problemas | Bajo «Cancelar conexión» decía «…No borra el historial ni cancela esta conexión» (del bloqueo) | Parece contradictorio | Explicación propia para cancelar y para bloquear | **Corregido** · `despues/03` |
| A13 | R · P2 | Beneficios · participante | En móvil, «Solicitar canje» queda ~1,2 pantallas abajo (imagen + cupón) | Acción poco visible | Imagen 2:1 en ≤420 px (mejora parcial); reordenar el cupón exige rehacer el diseño aprobado D14 → propuesta en Figma | **Parcial** · `antes/05`, `despues/05` |
| A14 | R · P2 | Plano · organizador | Texto técnico «…requieren su propio respaldo; el repositorio solo conserva el código» | Jerga de desarrollo | Texto para organización | **Corregido** |
| A15 | R · P2 | Entrada | «1 de 3 · Tu evento» y después registro/verificación sin numerar, luego «Paso 2 de 3» | Pasos que no cuentan la cuenta | Decidir si la cuenta es un paso visible | **Abierto** (cambio del recorrido de entrada aprobado; se observa en la prueba con personas) |
| A16 | C | Google | Botón presente en producción | Flujo real no probado | Probar con una cuenta de prueba autorizada | **Pendiente** |
| A17 | C | Cámara | Lectura y alternancia probadas con cámaras simuladas | Safari iPhone/Android no certificados | Prueba física | **Pendiente** |
| A18 | R · P2 | Editor de lugares | Se pueden crear dos lugares con el mismo nombre (DEC-066 H10) | Ambigüedad | Aviso de nombre repetido | **Abierto** |

Sin defectos en esta pasada: códigos de evento erróneos/vacíos/minúsculas, registro con contraseñas distintas (conserva el correo), contraseña errónea (mensaje y enlace a crear cuenta), recuperación con respuesta común, vuelta de la participante a Mi Ping con «Volver al encuentro», permisos 403 para participantes y otros eventos, foco visible, movimiento reducido, sin desbordes a 320/360/390/1366, sin errores JS en los recorridos.

## Actividades, verificación y puntos (estado)

Implementado en DEC-066 y revisado aquí: verificación del encuentro (llegadas y códigos), validación de la actividad (confirmación de ambos o revisión del personal) y acreditación (Saga, una sola vez) están separadas en pantalla y en servidor. No hay botones «Completar» ni «Mostrar evidencia» sin operación; la foto no es obligatoria en ningún método. La ayuda ya refleja la revisión.

## Pruebas sobre la versión identificada

- Suite completa `./mvnw -o clean verify` sobre **`7b33430`**: **383 pruebas, 0 fallos, 0 errores, 0 omitidas, BUILD SUCCESS** (08/11/2026 20:23 Bogotá). Durante la corrección: un error de compilación (variable capturada en lambda) y una aserción de texto antiguo («Crea primero un punto» → «lugar»), ambos resueltos antes de la suite.
- Dirigidas: `FrontendBundleTests`, `EventExtrasTests`, `Floorplan*Tests`, `DemoImprovementTests`, `EventExperienceTests`, `OrganizerBoardTests`, `EventTests`, `ActivityReviewTests`, `MatchTests`.
- App real: cada corrección comprobada con su captura (`despues/`), axe sin violaciones en Mi Ping, enlace a opciones abre el panel (también por URL con `#connection-safety`).

## Capturas comparables

`docs/screenshots/auditoria-recorridos/antes/` (sobre `4c0f0a6`) y `despues/` (sobre `7b33430`), 390 px oscuro salvo `antes/07` (claro). Pares: 01/01b↔01 · 02↔02,03 · 03↔03b · 04↔04 · 05↔05 · 06↔06 · 07↔07 · 08↔08.

## Pendientes concretos

**iPhone (Safari) y Android, por HTTPS:** permiso de cámara (primera vez y denegado), trasera por defecto, «Usar cámara frontal», lectura de QR a 30–50 cm con luz de interior, volver desde otra app con la cámara cerrada, teclado sobre «Nota para el personal» y sobre el código manual, áreas seguras con la barra pegajosa de intereses, imagen de recompensa tomada con el móvil (orientación).

**Con personas:** guion y hoja en [auditoria-recorridos/PRUEBA_APRENDIZAJE.md](auditoria-recorridos/PRUEBA_APRENDIZAJE.md); opcional primer clic/cinco segundos en Lyssna Free (no creado).

**Decisiones:** A13 (recolocar «Solicitar canje» en el cupón aprobado), A15 (contar la cuenta como paso), A18.

## Cinco tareas para tu revisión final

1. En un celular, entra con una cuenta de participante y toca el nombre del evento en Mi Ping: debe abrir la pestaña Evento, no «Acceso restringido».
2. Acepta un encuentro, registra tu llegada y no hagas nada con la otra cuenta: comprueba que aparece «¿No llega o hay un problema? Ver opciones» y que abre directamente «Cancelar conexión».
3. Como organización, abre Configuración del evento: la pestaña dice «Lugares» y la actividad pide «Lugares donde puede hacerse»; la agenda dice «Añadir a la agenda».
4. Con dos eventos, abre uno desde el tablero, entra en su plano y pulsa «Mis eventos»: debe volver al mismo evento; el plano no ofrece lugares archivados.
5. Edita tus intereses en el celular: «Guardar intereses» debe verse sin desplazarte hasta el final.

## Siguiente acción para retomar

`cd C:\Users\river\PING-cierre && git status --short --branch && git log --oneline -4` (rama `feature/auditoria-recorridos`). Revisar A13/A15/A18 con Samuel; con autorización, integrar sobre el SHA publicado (`7f20b49` según huellas), ejecutar `./mvnw clean verify` y las pruebas físicas y con personas.
