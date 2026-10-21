# Auditoría visual integral — diagnóstico

Fecha: 21 de octubre de 2026. Base: rama `claude/sweet-ritchie-ja97c7` @ `dc44008` sobre `ci/development-validation` @ `f2923b9` (sin avances posteriores en el remoto). Build local de esa base con MySQL 8.0.46 (el proyecto apunta a 8.4.11), escenario demo SOFA y datos ficticios. **Es una revisión de primer uso hecha por un agente con Chromium emulado; no es una prueba con personas.**

Las imágenes de `evidencia/` son hojas de contacto de capturas reales (390×844, oscuro, salvo que se indique). Los estados se alcanzaron con recorridos reales (dos participantes demo y una cuenta general), no con maquetas.

## 1. Cobertura del recorrido

| Área | Estado de la revisión | Evidencia |
|---|---|---|
| Home (visitante y con sesión), claro/oscuro | Revisada | 01, 17 |
| Código de evento, acceso, registro, recuperación | Pantallas revisadas; el escáner de cámara y los correos de verificación **no** se ejercitaron | 01 |
| Alias y avatar; intereses y categorías (alta y edición) | Revisadas en el bloque anterior con capturas reales; edición `/participants/{id}/interests` y `/me/avatar` revisadas aquí | bloque anterior; 13 |
| Mi Ping, Conectar, disponibilidad | Revisadas | 02 |
| Búsqueda, propuesta, aceptación y espera | Revisadas. **«Sin candidatos» no se alcanzó**: la búsqueda de Ana encontró una propuesta con Samuel | 03 |
| Encuentro activo, llegadas, código personal y verificación cruzada (incluido código inválido) | Revisados; el QR de cámara no se ejercitó | 03–06 |
| Confirmación bilateral, finalización y puntos, feedback | Revisadas | 07 |
| Beneficios, solicitud, recibo y código de canje | Revisados | 08 |
| Entrega por el organizador | Pantalla «Entregar un beneficio» revisada; consulta y entrega **no** ejecutadas en esta pasada | 11 |
| Hub, mapa, plano (sin imagen cargada), perfil e historial | Revisados; plano **con** imagen no | 13 |
| Bloqueo y reporte | Formularios localizados (plegados en «Cuidemos este encuentro»); no se enviaron | 04 |
| Organización y configuración del evento | Pantallas revisadas; no se guardaron cambios | 09–11 |
| Errores, sesión caducada y sin conexión | 404, código inválido, **sin red al enviar** y **sesión perdida** capturados; «Enviando…» y «Está tardando» revisados solo por código (`ui.js`), sin captura fiable | 12 |
| Escritorio 1440 | Participante y organización, parcial | 14, 15 |
| Texto ampliado | Simulado (todos los textos ×1,5 y ×2 a 360/390); no es el ajuste real de un teléfono | 16 |

## 2. Revisión de primer uso

Qué entiende una persona sin conocer el producto (juicio del agente, no medición):

- **Qué hace PING:** claro en Home («Comparte gustos. Conecta en persona.», tres pasos y la ilustración).
- **Cómo entrar:** el código/QR del evento se explica en una frase; «El código identifica el evento, no a una persona» evita confundirlo con credenciales.
- **Qué es público:** el alias lo dice en el campo («Tu alias es público… tu correo sigue siendo privado»).
- **Siguiente paso y estado:** muy buena en el encuentro: una línea «Ahora: …» y estados «Ya llegó / Pendiente / Verificado».
- **Qué falta de la otra persona:** se nombra («Falta la llegada de Julián»).
- **Tres códigos distintos:** palabra compartida, código personal y código de canje se distinguen por texto («Es distinto de la palabra compartida y del código de canje»). Visualmente son tres cajas parecidas.
- **Confusión leve:** Mi Ping y Conectar repiten el mismo mensaje («Tu siguiente conexión, en Conectar» → «¿Con quién vas a conectar?»).

## 3. Qué ya funciona y se conserva

Identidad Festival Social reconocible y sobria; paneles morados de acción con botón amarillo único por pantalla; estados vacíos con siguiente acción; recuperación de errores (código inválido, sin red, sesión perdida con botón); **ningún desbordamiento horizontal a 360 px ni con texto ×2 en 7 pantallas**; **contraste suficiente** en 19 pantallas × 4 variantes (el único valor bajo es el punto amarillo del logotipo, exento por ser logotipo); todas las imágenes con `alt` y todos los campos con etiqueta; páginas de texto de 128–134 KB y CLS = 0 en la carga fría local (la ilustración de 312 KB solo en Home/Hub); reserva de `scroll-padding-bottom` para la navegación inferior (WCAG 2.4.11).

## 4. Hallazgos

Prioridad: **P0** impide actuar o produce un error serio; **P1** confusión importante o dificultad móvil; **P2** coherencia, identidad y calidad visual; **P3** acabado. **No encontré P0.** «Defecto» = observado y reproducible; «mejora» = criterio de diseño.

| ID | P | Pantalla · estado | Evidencia | Consecuencia | Cambio | Depende de | Tipo |
|---|---|---|---|---|---|---|---|
| F01 | **P1** | Recibo de canje, tras «Generar código» | 08; `receipt.html` tiene `<details>` sin `open` | En el mostrador hace falta otro toque para ver el código; el botón amarillo dominante es «Renovar código sin nuevo cobro» | Abrir el código al generarlo; composición de comprobante; renovar pasa a secundario; pestaña «Beneficios» activa | — | Defecto |
| F02 | **P1** | Alta: alias/avatar e intereses | Bloque anterior: botón a 1311 px, 11 familias iguales | Desplazar ~1,5 pantallas para continuar; el catálogo de 264 gustos no transmite estructura | Integrar la propuesta aislada | Aceptación de Samuel y Julián (cambia DEC-037/045); pregunta Q1 | Defecto + mejora |
| F03 | P2 | Casi todas: selector de tema | 36×36 px en 69 de 81 capturas | Bajo el estándar de 44 px del proyecto (cumple el mínimo AA de 24) | Botones de 44×44 | — | Defecto |
| F04 | P2 | Enlaces independientes: «← Mi Ping», «Actualizar estado», «Ver participante», «Editar categoría…» | 18–22 px de alto en 11, 9, 4 y 3 pantallas | Difíciles de tocar; por debajo de 24 px solo pasan WCAG 2.5.8 si se cumple la excepción de espaciado (no verificada) | Alto mínimo de 44 px con padding | — | Defecto |
| F05 | P2 | Navegación inferior | Etiquetas de 10 px en 69/81; con texto ×2 se pegan («ConectarEventoBeneficios»); recibo marca «Mi Ping» en vez de «Beneficios»; «Más» queda activo en Home; el Hub tiene otra navegación | Legibilidad y orientación | Etiqueta de 11 px con tope de crecimiento; activo coherente; resto como propuesta | Decidir si el Hub unifica navegación (Q3) | Defecto parcial |
| F06 | P2 | Tras cada envío: título o mensaje enfocado | 07, 12: recuadro blanco rectangular | Aspecto tosco; no usa el foco amarillo de la marca | Foco de marca con radio y desplazamiento | — | Defecto |
| F07 | P2 | Mensajes de recuperación | 12: «sesión perdida» al final, lejos del botón desactivado; «sin red» con recuadro dentro del panel | El motivo del bloqueo queda lejos de lo bloqueado | Mensaje junto a la acción; patrón de aviso | — | Propuesta (ver sección 6) |
| F08 | P2 | Encuentro activo: pasos 1·2·3 | 04, 05: los tres pasos con el mismo peso; estados como texto gris a la derecha | Para ver qué falta hay que leer todo | Estado visual por paso (hecho / actual / bloqueado) | Pantalla aprobada: DEC-032; solo con defecto observado | Mejora |
| F09 | P2 | Avatares incoherentes | 13, 09: inicial en círculo (Hub, lista y perfil) frente al retrato preset (Mi Ping, encuentro) | Mismo participante con dos identidades visuales | Usar el retrato preset donde hay dato | — | Defecto |
| F10 | P2 | Organización: detalle y listado | 09, 11: «Compartir código / QR», «Editar configuración» y «Entregar un canje» como enlaces de texto; `/organizer` sin menú lateral en escritorio; títulos de 3–4 líneas junto a un botón | La tarea principal del organizador (compartir el código) es la menos visible | Acción principal visible; título y botón apilados | — | Mejora |
| F11 | P2 | Organización: textos de desarrollo visibles | 10: «sin autenticación nueva», «Los planos se guardan en MySQL y requieren su propio respaldo», claves `games:accion-y-supervivencia` | Mensajes que no son para quien organiza | Copy orientado a la tarea | — | Defecto de texto |
| F12 | P2 | Texto ampliado | 16: «Benefici/os» a ×1,5; «Samu/el» a ×2 | Palabras partidas a la mitad | Cabeceras que envuelven por palabra | — | Defecto |
| F13 | P2 | Home para visitante | 01: navegación inferior de Mi Ping / Conectar / Beneficios | Lleva a «Entrar» (enlaces sin destino útil antes de tener cuenta) | Ocultarla sin sesión | Q3 | Defecto |
| F14 | P3 | Mi Ping ↔ Conectar | 02 | Mensaje duplicado | Reducir copy | — | Mejora |
| F15 | P3 | Plano admin | 10: «Choose File / No file chosen» | Idioma del navegador, no de la página | Control propio con etiqueta | — | Mejora |
| F16 | P3 | Pies y créditos | Textos de 9–11 px repetidos en todas las pantallas | Ruido | Un único pie compacto | — | Mejora |
| F17 | P3 | Escritorio | 14: Conectar en columna estrecha con gran aire lateral | Aprovechamiento | Composición lateral | — | Mejora |
| F18 | P3 | Participantes demo | 03: Samuel y Julián con el mismo avatar por defecto | No se distinguen en la propuesta | Q2 (dato, no presentación) | Q2 | Pregunta |

## 5. Mediciones

| Medida | Resultado |
|---|---|
| Contraste (texto, cálculo propio sobre fondos sólidos) | 0 fallos en 19 pantallas × (claro 390, oscuro 360, claro 360) salvo el punto del logotipo. No evalúa fondos con imagen o degradado |
| Desbordamiento horizontal | 0 en 57 mediciones; 0 con texto ×1,5 y ×2 |
| Texto < 11,5 px | Navegación (10 px) en 69/81; pie «Conexiones en tu evento» (10 px) en 23; créditos (9 px) en 10 |
| Áreas táctiles < 44 px | Tema 36×36 (69); logotipo 66×28 (79); «← Mi Ping» 60×18 (11); «Actualizar estado» 105×18 (9) |
| Etiquetas / `alt` | 0 campos sin etiqueta; 0 imágenes sin `alt` |
| Rendimiento (carga fría, sin throttling, caché desactivada) | Mi Ping 134 KB / 11 peticiones; Conectar 131 KB; Home y Hub 441 KB (312 KB de ilustración). CLS = 0. **No es comparable con las cifras de DEC-052** (otras condiciones de red y CPU) |

## 6. Estado de los bloques

Se actualiza al cerrar cada bloque (ver `README.md`).

| Bloque | Hallazgos | Estado |
|---|---|---|
| A · Táctil, foco y navegación | F03 F04 F05 F06 F12 | **Implementado** y comprobado en 8 pantallas con sesión (ver sección 7). F05 parcial; ver nota |
| B · Canje: comprobante | F01 (y F05 en recibo) | **Implementado** (sección 8) |
| C · Alta: identidad e intereses | F02 | **Integrado en la app** (DEC-055, §11); aceptación de Julián pendiente |
| D · Encuentro: estados de paso | F08 | Propuesto |
| E · Avatares coherentes | F09 | **Implementado** (sección 9) |
| F · Organización | F10 F11 | **Implementado en parte** (sección 10) |
| Pendientes sin bloque | F07 F13 → **implementados** (DEC-055, §11). F14 F15 F16 F17 F18 | Propuesto |

## 7. Bloque A — qué cambió y qué se comprobó

Implementado en `brand.css`, `ui.js` y 9 plantillas (solo clases; destinos, formularios y contratos intactos). Capturas antes/después: `evidencia/A1…A5`.

| Hallazgo | Cambio | Comprobado |
|---|---|---|
| F03 | Selector de tema de 44×44 px | Cabecera a 390 y 360 px sin desbordes (A1) |
| F04 | Clase `.link-44` en «← Mi Ping», «Actualizar estado», «Omitir», «Ver participante», «Editar categoría…», «← Mi evento», «← Volver al encuentro», «Beneficios», «← Mis canjes», «Volver al inicio»; logotipo y resumen de «Cuidemos este encuentro» a 44 px de alto | Medición en 8 pantallas con sesión a 390 px: 0 controles por debajo de 44 px (antes: 1–2 por pantalla). No cubre pantallas de organización ni todas las pantallas |
| F05 | Un solo estado activo en el comprobante (Beneficios); las etiquetas ya no se pisan | **Parcial.** Se probó subir la etiqueta a 11 px y se **descartó**: adelantaba el recorte de «Beneficios» a ×1,3 en 360 px. Se mantiene 10 px. A ×1,5 (360 px) se recortan «Conectar» y «Beneficios» con «…» y a ×2 casi todas: el icono y el nombre accesible completo se conservan, pero **el texto visible se recorta**. «Más» activo en Home, Hub con otra navegación y navegación visible para visitantes siguen sin resolver (Q3) |
| F06 | Foco de marca (amarillo en oscuro, morado en claro, radio 6 px) en elementos con `tabindex="-1"` | Con teclado: anillo visible en ambos temas (A4, A5). Sin teclado, el navegador decide cuándo mostrar el foco; no se forzó |
| F12 | Cabeceras de Mi Ping y de Beneficios que envuelven por palabra (en Beneficios el título heredaba `overflow-wrap:anywhere`; se acotó a `break-word`) | «Samuel» ya no se parte a ×2 y «Beneficios» tampoco a ×1,5 (A2, A6); el saldo o «Mis canjes» bajan a otra línea solo cuando no caben. A texto normal la pantalla es igual que antes (A1). Sin desbordes a ×1,5 y ×2 en 7 pantallas |

Pruebas: `FinalMvpTests`, `DemoImprovementTests` y `PagePresentationTests` (29, verdes). No se añadieron pruebas de clases CSS (AGENTS.md).

Rectificación durante el bloque: una primera versión partía las palabras de la navegación a ×2 («Con/ctar») y apilaba el saldo bajo el nombre también a tamaño normal; ambas se corrigieron antes de este checkpoint.

## 8. Bloque B — comprobante de canje

Plantilla `rewards/receipt.html` y bloque `.receipt-*` de `brand.css`. Mismos datos, rutas, campos del formulario (`_csrf`, `requestKey`, `renew`), `data-private-credential` (que `live.js` borra si la sesión se pierde) y texto de coste «ya se descontó una sola vez». Capturas: `evidencia/B1` (antes → después, 390 px) y `B2` (390, 360 y claro).

| Antes (F01) | Después |
|---|---|
| Tras «Generar código» el código y el QR seguían dentro de un `<details>` cerrado: otro toque en el mostrador; el botón amarillo dominante era «Renovar código sin nuevo cobro» | El código y el QR se ven al abrir la pantalla (se renderizan en la misma respuesta que los genera) dentro de una tarjeta «Muestra este código»; **renovar** pasa a botón secundario; «Generar» sigue siendo el botón principal cuando no hay código |
| Orden: saldos, aviso, lugar de entrega, instrucción, código | Orden: referencia y estado, código/acción, lugar de entrega, coste y saldo. Incidencia y entrega registrada suben junto al título |
| Estado y referencia en dos bloques | Referencia y estado en una fila |

Comprobado: 390 y 360 px oscuro y 390 claro sin desbordes, sin fallos de contraste (cálculo propio) ni controles bajo 44 px; el código se parte en dos líneas limpias a 390 y 360 tras ajustar el tamaño (una primera versión dejaba un carácter huérfano a 360 y se corrigió). Pruebas: `VerificationTests`, `RewardTests`, `FinalMvpTests`, `PagePresentationTests` (verdes).

**No comprobado:** recibos entregados o con incidencia (no se ejecutó la entrega por el organizador), recibo con ubicación guardada, escritorio, ni lectura real del QR con una cámara. La copia «Es para el encargado autorizado del evento. Válido 24 horas.» condensa los dos textos anteriores sin cambiar su sentido.

## 9. Bloque E — avatares coherentes (F09)

Tres plantillas (`experience/hub-fragments.html`, `participants/list.html`, `participants/detail.html`) usan ahora el fragmento existente `entry/avatar :: portrait(...)` con el avatar que ya viaja en `ParticipantView`; ningún controlador ni dato cambió. CSS: tres reglas de tamaño (`brand.css`, junto a `.h-avatar`). Captura: `evidencia/E1` (Hub, lista y perfil a 390 px, oscuro).

Comprobado: Hub a 390 y 1440 px, lista y perfil a 390 px sin desbordes, sin fallos de contraste (cálculo propio) y sin controles bajo 44 px; las tres plantillas pasan `ParticipantTests`, `FinalMvpTests`, `DemoImprovementTests`, `PagePresentationTests`. En el perfil, «← Volver a participantes», el evento y «Entrar con este participante» pasan a 44 px.

**Límites:** los tres participantes de la demo tienen el mismo avatar por defecto (gato violeta), así que la captura no muestra variedad; es un dato, no una decisión de presentación (Q2). Después se revisaron también 360 px en tema claro (Hub, lista y perfil: sin desbordes ni fallos de contraste). `demo/prepare.html` (organización) conserva la inicial.

## 10. Bloque F — organización (F10, F11)

Prioridad de producto: el participante; este bloque se limitó a la tarea principal del organizador y a textos de desarrollo. Plantillas `events/detail.html`, `interests/categories.html`, `floorplan/admin.html` y tres reglas CSS. Captura: `evidencia/F1` (detalle del evento a 390 px, antes → después).

| Cambio | Detalle |
|---|---|
| Compartir el código, primera tarea | «Entrada de participantes» sube justo bajo el título y las dos acciones; el código se muestra a 26 px en amarillo (antes 21 px, en blanco, tras ~1,5 pantallas). El enlace de texto «Compartir código / QR» se quitó porque la sección ya está visible; la ancla `#entry-share-title` se conserva |
| Acciones operativas | «Editar configuración» y «Entregar un canje» pasan de enlaces de texto a botones secundarios de 44 px |
| Toques | «← Mis eventos», «Ver resultados del evento», «Preparar demo rápida» y el resumen «Configuración del encuentro» a 44 px; medición a 390 px: 0 controles bajo 44 px en el detalle (antes 4) |
| F11 textos | Se retiró «Administración académica existente, sin autenticación nueva.» (categorías); el pie del plano deja de nombrar MySQL y GitHub; el aviso de enlace/QR sin dirección pública conserva el nombre de la variable (`PING_PUBLIC_BASE_URL`) porque es la acción técnica real, pero lo expresa como estado |

**No hecho / límites:** las claves de nodo (`games:accion-y-supervivencia`) siguen visibles en la jerarquía de gustos: son el identificador con el que se edita cada nodo, no se tocó. El plano de `/organizer` sin menú lateral en escritorio, el control de archivo en inglés (F15) y la tabla de configuración no se rediseñaron (sin dashboards ni embellecimiento de administración, AGENTS.md). Comprobado: detalle del evento a 390 y 1440 px sin desbordes ni fallos de contraste (cálculo propio). Pruebas: `EventEntryFlowTests`, `EventTests`, `InterestCategoryAdminTests`, `FloorplanTests`, `PagePresentationTests` (verdes). Tema claro a 390 px revisado en el detalle del evento (sin desbordes ni fallos de contraste); 360 px no.

## 11. Iteración DEC-055 — Identidad, navegación, recuperación e Intereses en la aplicación

Autorización de Samuel (DEC-055); sin aprobación atribuida a Julián. Todos los cambios son de presentación e interacción: plantillas, CSS y JS; ni backend, ni datos, ni migraciones. «Antes» = commit `3d66508` (tras el bloque anterior) servido junto a la versión nueva sobre la **misma base de datos**, mismo participante y mismo estado. Capturas: `evidencia/I*` (identidad), `N*` (navegación), `R1` (recuperación), `P*` (intereses).

| Bloque | Cambio | Comprobado |
|---|---|---|
| 1 · Identidad (`/join`, `/me/avatar`) | Gafete con evento y estado, avatar, alias y vista previa; progreso de 3 pasos; editor de avatar con pestañas (radios nativos del servidor); miniaturas de sombrero/accesorio compuestas solo al abrir su panel | Botón principal de 1309 → 670 px a 390 y 360 px (escritorio 1173 → 444). 9 imágenes de avatar al cargar. Corregir alias/evento conserva alias y avatar; alias vacío muestra el error del servidor junto al campo; edición en Mi Ping guarda; alta real hasta Mi Ping con avatar elegido; **sin JavaScript** el alta se completa (grupos apilados) |
| 2 · Navegación | Una barra inferior para Hub y resto (`fragments/navigation`): participante, organización y visitante (sin destinos personales: Inicio, Unirme a un evento, Entrar / Mi cuenta). Hub sin «Mapa» en la barra (sigue en su pantalla). Varias filas con texto ampliado sin recortar etiquetas; altura medida y reservada bajo el contenido; «Más» ya no se activa en Inicio | Etiquetas completas hasta ×2 a 360 px (2 filas, 123 px); el final de página no queda bajo la barra; menú «Más» en Hub y resto con foco, Escape e `inert`. La pantalla `/organizer` no usa esta barra |
| 3 · Recuperación | Aviso único arriba (sesión perdida: alerta y visible al desplazarse; sin conexión: estado de espera con «Reintentar ahora» sin recargar); botones bloqueados enlazados a su motivo; feedback de envío junto al botón pulsado | Sesión perdida y sondeo sin red simulados con rutas interceptadas en Conectar; envío sin red; se mantienen limpieza de credenciales privadas, bloqueo de doble envío y sondeo. **No** se reprodujo con un encuentro ACTIVE (mismo código, otra pantalla) |
| 4 · Intereses | Mundos en mosaico uniforme con icono local y contador; subgrupos como chips con «Todo»; gustos como chips con marca; bandeja de elegidos (quitar, «+N más»); búsqueda que conserva elecciones; «Conservados» visibles | Alta → Mi Ping con 3 elegidos tras buscar y volver; edición desde Mi Ping (3 → 2) guarda; editor en línea de `/interests`; 24 elegidos, 360 px y ×1,5/×2 sin desbordes; categoría inactiva muestra «Conservados»; alta completa sin JavaScript |

**Desviaciones respecto al prototipo y límites**
- Intereses: el inicio mide **941 px hasta el botón (antes 834)**; el prototipo anunciaba lo contrario. Se aceptó por pedido (iconos, contadores y mosaico uniforme); el mosaico no usa los tonos ciruela/lila «solo Hub» (no hay tokens nuevos). La cabecera no muestra el avatar: el modelo de ese paso no lo incluye y no se cambió backend.
- Los mapas de opciones de avatar del servidor no tienen orden estable (`Map.of`); la plantilla las ordena por clave. Es un defecto previo.
- «Conservados» (selecciones de categorías inactivas) quedan bajo el mosaico; antes se ocultaban en la vista de mundos.
- No se añadió `viewport-fit=cover`: las áreas seguras (muesca, indicador de inicio) **no se verificaron** en dispositivo.
- Hub con texto ×2: la cabecera propia del Hub aún desborda (previo, fuera de alcance).
- No probado: iPhone/Android reales, lectores de pantalla, ni personas.


## 12. DEC-056 — acabado móvil vigente (bloque visual)

Autorización explícita de Samuel, Issue #51; base remota3755c129cee95f6ece533b52a226c21969c6a52e, rama fix/mobile-visual-finish. Las secciones anteriores son evidencia histórica, no pendientes automáticos. Conservados los componentes de Claude y sus contratos, sin cambios Java/datos/migraciones.

**Intereses:** directorio de familias con los iconos locales existentes, nombres y contadores reales; separadores en lugar de cajas iguales. Se conserva el orden del catálogo, sin dar prioridad mediante colores nuevos. Menos altura en cabecera y filas de al menos68px, controles de elección/chips≥44px y botón48px. Bandeja: tres chips completos visibles, +N coincide con los restantes; expandir/quitar conserva los checkboxes como única fuente de estado.

Medición comparable a390×844, oscuro, alias Valentina/evento SOFA2026 ficticio,264 intereses,0 seleccionados, fuente local cargada, coordenadas de documento CSS (superior/inferior del botón):

| Referencia | Superior | Inferior |
|---|---:|---:|
| 3d66508, referencia anterior citada por Claude |837,25|885,25|
| 3755c12, base de esta tarea |944,39|992,39|
| DEC-056 |813,06|861,06|

La diferencia histórica107,14px coincide con941→834 publicados; en la primera entrada de las cuentas nuevas también se midió941,39/989,39. La repetición controlada quedó3px más abajo en ambas referencias, sin alterar la diferencia. DEC-056 recupera una altura comparable (24px por encima de la referencia antigua), sin fijar la acción al viewport. Se conservaron los fallos de la sonda inicial: recursos de referencias mezclados (comparación descartada), radio tapado por su label, búsqueda de guitarra inexistente y lectura parcial del HTML antes del script diferido; corregidos los instrumentos, sin debilitar la aplicación. Esperando categoriesReady, los23 IDs coinciden antes/después de volver al alias.

**Avatar:** reproducido el salto de marca en400px (columnas de110px), y Amarillo partido con pestañas solapadas a360px/texto×2. Distribución por ancho intrínseco y flex-wrap en el fragmento compartido: texto y marca juntos, pestañas con etiquetas completas; valores/radios/lógica intactos. A390 normal ya cabía en2 columnas; no se declara fallo normal en esa variante.

**Hub:** a texto×2, título de552px en312px disponibles (390) y493 en288 (360); alias recortado. Con autoaltura, redistribución de cabecera/acciones, alias sin ellipsis y título con salto por espacio:312/312 y288/288, alias completo, botones dentro del ancho. Composición normal y arte conservados. Misma misión ACTIVE, punto y saldos0 en ambas referencias de Hub.

**Comprobado:** Chromium táctil emulado390/360 oscuro y claro, escritorio1440, avatar/Hub×2; búsqueda, familias, subgrupos,3/24 selecciones, expandir/quitar,23 guardadas en borrador y retorno. Sin errores JS en la sonda final. Java dirigido EventEntryFlowTests/InterestCategoryTests/PagePresentationTests:11/0/0/0. Primer intento restringido falló por SocketException Operation not permitted al abrir Tomcat; mismo comando con permiso de sockets pasó, sin cambios de producto. Node --check y diff --check correctos. Evidencia individual en evidencia/DEC-056; sin datos personales ni credenciales.

**Pendiente de este bloque:** comprobaciones dirigidas de ACTIVE con recuperación, recibos y plano, fallback sin JS/edición, y cierre documental. No dispositivo físico/Safari/cámara ni certificación de accesibilidad. Servicios propios:8096 fuente actual,8097 referencia3755c12, SMTP loopback1026, MySQL8.4.11 conservado3308/base NUEVA ping_visual_finish_exclusive. No se reutilizó la demo existente. Skills versionadas manuales; launcher Impeccable no disponible, revisión directa con PRODUCT/DESIGN/polish/craft-floor; sin reinstalaciones/hooks/subagentes.


### DEC-056 — recuperación ACTIVE y privacidad

Comprobado en el encuentro1 de las cuentas nuevas: fallo de sondeo simulado con aborto de /me/state, aviso Sin conexión/Reintentar ahora, borrador de pista intacto al recuperar; llegada REAL ajena mientras se escribe muestra El encuentro cambió y no recarga ni pierde borrador. Cancelar explícitamente permite actualizar y registrar la otra llegada. Logout REAL POST /logout con CSRF en la sesión compartida produce401 en el polling: aviso persistente/entrada, pista ajena retirada, código/QR generado borrado y botones desactivados.

**Defecto reproducido y corregido:** el campo manual/escaneado de credencial quedaba visible tras401, aunque se limpiaban código/QR generados. live.js vacía value/defaultValue y su indicador dirty solo en sessionLost; no se cambia el borrador por un fallo de red ni se toca backend/escáner. La misma sonda con logout real falló antes (typedCredentialRemoved=false) y pasa después (true), comprobando también retirada de pista y recursos privados. Capturas sanitizadas active-sin-red-390 y active-sesion-perdida-390: los códigos generados se enmascaran al capturar, no se publican.

Además: alta completa SIN JavaScript con alias largo, animal/color elegidos, retorno con selección conservada; avatar guardado y edición por teclado con formularios nativos. Editor inline de /interests guarda1→2 y muestra2 al volver. Alias real de69 caracteres visible entero a360/texto×2 en Hub. Sin errores JS. No se afirma cámara real ni lector de pantalla. La primera sonda sin JS no logró estabilizar el clic de Guardar avatar; la operación se verificó por teclado, sin usar force ni declararlo prueba táctil.

Primer respaldo visual f15c867ac51dd2b2840b37c3db9920fda5d7bc33, remoto comprobado, [PR52 borrador](https://github.com/Samge34c/PING/pull/52). [CI real del bloque visual](https://github.com/Samge34c/PING/actions/runs/39659511985): Java y Windows correctos. El código de privacidad posterior necesita su nueva ejecución. Plano/recibos y sincronización final siguen en curso.
