# Auditoría independiente de D01–D14

25 octubre 2026. Auditor: Codex. Solo lectura de aplicación y Figma; datos sintéticos aislados. No constituye aprobación de producto.

## Veredicto

**No cerraría D01–D14 como totalmente conforme.** La identidad, los avatares, el personalizador, la llegada, Mi Ping, la pareja y el catálogo presentan la selección reconocible; el recorrido bilateral llegó a puntos y canje sin duplicación en lo comprobado. D08 conserva una composición diferente de la seleccionada. La entrega del canje devolvió 503 en el entorno H2 de esta auditoría; falta determinar si también ocurre en MySQL. Además, el seguimiento de un recibo `DELIVERED` contiene texto pendiente incorrecto, comprobado en la plantilla y pendiente de reproducción visual.

No encontré evidencia de pérdida de avatares ni de doble reducción al 84 % en las muestras examinadas. Esto no equivale a declarar ausencia de fallos.

## Versión y procedencia

- Rama: `feature/seleccion-implementacion`; HEAD: `49e9482da1c39217e63b318b4a7ab6fc0e5e639b`. Los cambios DEC-057/058 siguen sin commit y se conservaron.
- [Inventario inicial](auditoria-implementacion-d01-d14/version-inicial.json) y [final](auditoria-implementacion-d01-d14/version-final.json): 671 archivos, incluyendo fuentes, pruebas, recursos, POM y documentación de diseño seleccionada. **Cero diferencias de contenido entre ambos inventarios.** Huella de la lista inicial serializada: `95bfd7b000b2b4a1636944c2fa51a64544d83c00ddbb6a38265e848b53abcf13`. Los SHA-256 individuales identifican los cambios sin commit que HEAD no representa.
- JAR auditado: `target/ping-0.0.1-SNAPSHOT.jar`, SHA-256 `c04fce5d2428ecf0fb44a543406f793b3a0a5c5a1d2f24c3c42ac11583283867`. Sus 177 clases coinciden con `target/classes`; 489 recursos de producción coinciden con las fuentes, considerando la licencia empaquetada en la raíz del JAR. [Vínculo comprobado](auditoria-implementacion-d01-d14/vinculo-build-fuentes.json).
- No hubo puente `claude_ask` disponible en las herramientas de esta sesión. No se acordó una pausa; la doble lectura por hashes confirma estabilidad del conjunto revisado. No se atribuye a esta auditoría ningún cambio previo de Claude.

**Las 309 pruebas son evidencia de Claude, no una suite ejecutada por Codex.** Localicé la invocación final `./mvnw -Dspring.profiles.default=test clean verify` y su `verify2.log`: inicio registrado 20:40:44 UTC; final Maven 20:41:53 UTC / 15:41:53 Bogotá; 309 pruebas, cero fallos/errores/omitidas, `BUILD SUCCESS`, salida 0. Surefire conserva 37 clases, incluidas 40 pruebas de `AvatarV3Tests` y 16 de `VerificationTests`. Hubo una ejecución anterior con dos fallos: no es el resultado final. [Extracto y hash del log final](auditoria-implementacion-d01-d14/procedencia-claude-build.json), [recuento independiente de XML](auditoria-implementacion-d01-d14/procedencia-surefire.json).

Las fechas de fuentes Java son anteriores a la ejecución final y los recursos coinciden con el JAR. No existe un manifiesto de hashes de fuentes tomado al iniciar aquel build: la vinculación Java se apoya en esas fechas y las clases compiladas conservadas, no en una firma histórica de fuentes. No repetí Maven.

## Hallazgos prioritarios

### A01 · P1 en el entorno ensayado: entregar el canje devuelve 503

**Pantalla/estado:** Organización → Entregar un beneficio; canje propio del evento, solicitado por otra cuenta. **Comprobado en ejecución H2; atribución a producto/MySQL pendiente.**

1. Registrar dos participantes; completar aceptación, llegadas, verificación y confirmación.
2. Solicitar el beneficio de ensayo de 5 puntos y renovar su código.
3. En una tercera sesión con administración general, consultar el código vigente: aparece el beneficio correcto y «Pendiente de entrega».
4. Pulsar «Confirmar entrega».

**Observado:** POST `/organizer/redemptions/deliver` → 503, dos veces; mensaje «No pudimos completar la operación con los datos guardados». El recibo sigue `REQUESTED`, con saldo 5. La petición contiene un único `status=DELIVERED` y una credencial; no fue un formulario sin estado. **Esperado:** registrar entrega y mostrar el resultado, sin nuevo débito.

**Impacto:** el encargado no puede cerrar la entrega en este entorno. [Peticiones y respuestas sin credenciales](auditoria-implementacion-d01-d14/entrega-503.json). Código: `CredentialController.java:25`, `CredentialService.java:60`, `WebAdvice.storageUnavailable`. El manejador no conserva la excepción SQL en el log consultado. Una captura posterior del recibo pendiente **no demuestra entrega**: [estado conservado](auditoria-implementacion-d01-d14/18-recibo-pendiente-tras-503-360.png).

**Siguiente acción mínima:** reproducir exactamente consulta → entrega con credencial vigente y otra cuenta autorizada en MySQL aislada; obtener la excepción de `DataAccessException` y corregir solo su causa. No atribuirlo todavía a una regresión de DEC-058: ese servicio no fue modificado por su traslado visual. El intento de diagnóstico JFR del proceso aislado no pudo inicializar el grabador; no obtuvo una causa.

### A02 · P2: D08 está adaptada en estilo, pero conserva otra composición

**Pantalla/estado:** `/join/interests`, sin selección; después, edición de intereses → Videojuegos. **Discrepancia visual comprobada**, no fallo de persistencia ni incumplimiento de accesibilidad.

**Reproducción:** llegar a Gustos después de guardar identidad. La primera vista muestra once «Mundos» en dos columnas. Entrar en Videojuegos muestra «Todo» y familias como filtros sobre una lista alfabética. **Selección Figma `133:1703`:** categorías en una tira horizontal y grupos con título de familia y sus opciones; resumen y acción al pie. La diferencia se mantiene al navegar; no es solo la adaptación de 390 a 360 px.

**Impacto:** D08-1 no puede declararse trasladada íntegramente; cambia la forma de explorar y comparar grupos. El cierre de Claude reconoce esta sustitución en «Diferencias justificadas», pero documentarla no demuestra aceptación específica de la diferencia.

**Evidencia:** [Figma](auditoria-implementacion-d01-d14/figma-intereses.png), [alta real 390](auditoria-implementacion-d01-d14/05-intereses-390.png), [familias reales 360](auditoria-implementacion-d01-d14/23-intereses-familias-360-claro.png). `entry/interests.html:3`, `interests/selector.html:30`, `v2.css:280` y `interest-selector.js`.

**Corrección mínima recomendada:** resolver expresamente esta diferencia antes de cerrar D08: trasladar la jerarquía visual elegida reutilizando taxonomía, IDs y checkboxes actuales, o registrar la aceptación de la composición existente. Conservar búsqueda, selección retenida, familias, formulario y navegación funcional; no sustituir el catálogo ni fijar ejemplos de Figma.

### A03 · P2: el seguimiento conserva «Por entregar» después de entregar

**Pantalla/estado:** Recibo `DELIVERED`. **Comprobado por lectura de la condición; reproducción en navegador pendiente por A01.**

`rewards/receipt.html:28` marca el segundo paso como `done` para `DELIVERED`, pero el texto solo distingue `INCIDENT` del resto: produce «Por entregar» y «Acércate al punto de entrega con este código». La cabecera y el mensaje superior sí indican entrega registrada; el código ya no se muestra.

**Reproducción pendiente:** finalizar una entrega por el flujo autorizado y recargar el recibo. **Resultado deducido directamente de la plantilla:** mensajes incompatibles sobre si aún debe reclamarse. **Esperado:** seguimiento cerrado coherente, sin pedir presentar un código retirado. **Impacto:** confusión sobre una entrega ya finalizada.

**Corrección mínima:** texto específico de `DELIVERED` en ese paso o retirar su instrucción pendiente. No inventar una fecha de entrega, ni cambiar estados o cobros. Referencia de código suficiente para localizarlo; no se fabricó un recibo entregado para obtener una captura.

## Contraste D01–D14

«Conforme en lo comprobado» delimita las muestras ejecutadas; no certifica todos los estados.

| Decisión | Estado real | Evidencia | Discrepancia o pendiente |
|---|---|---|---|
| D01-1 | Conforme en lo comprobado | Tokens `v2.css`; Manrope cargada; capturas en ambos temas | Sin certificación completa de contraste ni texto de sistema real |
| D02-1 | Conforme en lo comprobado | Marca C en cabeceras; favicon y SVG de layout | No se evaluó registro legal de marca |
| D03-1 | Conforme en lo comprobado | Dos selecciones reales; guardar/recargar; cuatro animales; compatibilidad preset | Migración de una base histórica MySQL no ensayada |
| D04-1 | Conforme en lo comprobado | Pestañas de 46 px; opciones y colores ≥44 px; touch y teclado; rechazo/Deshacer | Teléfono físico y lector de pantalla pendientes |
| D05-1 | Conforme en lo comprobado | Figma `133:1331`; capas 111,375 / contenedor 132,594 = 84 %; cuatro variantes | Sin doble escala en muestras; conservar el encuadre |
| D06-1 | Conforme en lo comprobado | `133:1462`; Inicio editorial, código de evento y registro reales | Composición conservada con V2; créditos y enlaces adicionales funcionales |
| D07-1 | Conforme en lo comprobado | `133:1624`; alias largo y dos avatares reales en alta | Selector V3 sustituye el preset del ejemplo, según selección |
| D08-1 | Parcial | `133:1703`; capturas alta/familias | A02: composición diferente, reconocida por Claude |
| D09-1 | Conforme en lo comprobado | `133:1777`; Mi Ping con avatar, gustos, saldo y estado reales | Tarjetas de encuentro, reconocimiento e historial conservadas; adaptación razonable |
| D10-1 | Conforme en lo comprobado | `133:1838`; círculos iguales, espera bilateral, `connect-pair.js` DEC-057 | Recarga sin onda; no capturé la onda transitoria con éxito ni certifiqué fluidez física |
| D11-2 | Conforme en lo comprobado | ACTIVE real: palabra, punto, llegadas, verificación, confirmación y +10 | Sin plano configurado: comprobada alternativa textual, no plano con imagen |
| D12-1 | Conforme en lo comprobado | `133:2075`; fila del participante real, puntos y verificación cruzada | Caso histórico sin verificación confirmado en código y pruebas de Claude, no recreado como flujo propio |
| D13-1 | Parcial | `133:2145`; recibo real, saldo actual, caducidad y renovación | A01 bloquea entrega; A03 texto `DELIVERED`; expiración natural no ensayada |
| D14-1 | Conforme en lo comprobado | `133:2432`; cupón V2 con beneficio propio de 5 puntos y saldo real | Composición conservada; el beneficio demo adicional procede del catálogo existente, no de copiar Figma |

Leí la página de selección `133:262` con el servidor oficial de Figma y obtuve nueve referencias actuales de pantalla. Los identificadores consultados siguen existiendo. Figma conserva textos de situación previos («Solo en Figma»); no los traté como estado actual de código ni como nuevas aprobaciones. No modifiqué el lienzo. Consolidado, DEC-057 y auditoría previa se contrastaron mediante sus handoffs y evidencias existentes.

## Hallazgos anteriores y recorrido funcional

| Hallazgo anterior | Estado | Evidencia y límite |
|---|---|---|
| POST de avatar rechazado marcado como guardado | Corregido | POST incompatible → 400; «Probando · sin guardar», Deshacer habilitado; [estados](auditoria-implementacion-d01-d14/avatar-estados.json) |
| Deshacer compara contra borrador rechazado | Corregido | Volvió de Nutria/Coral/Gorro al Conejo/Dorado/Bucket persistido |
| Persistencia de animal, pelaje y accesorio/color | Conforme en lo comprobado | Guardé Nutria/Coral/Lazo/Menta y recargué; incompatibilidad retiró gorro al elegir auriculares, con aviso |
| Compatibilidad de cuentas antiguas | Corregido en el contrato comprobado | POST preset anterior en cuenta sintética: Panda/Corona/Gafas/Azul se conserva tras abrir el editor y recargar Mi Ping; propuesta V3 sin guardar y aviso explícito. [Evidencia](auditoria-implementacion-d01-d14/compatibilidad-legacy.json). No equivale a migración histórica MySQL |
| COMPLETED etiquetado automáticamente «Verificado» | Corregido en código; caso moderno ejecutado | `connections.html:17` exige ambas verificaciones. Mi encuentro verificado muestra esa etiqueta; `VerificationTests` final incluye los casos sin/con evidencia. No forcé un COMPLETED histórico |
| Recibo pierde «Saldo actual» | Descartado | Sigue mostrando «Tu saldo actual» y 5 puntos después del único débito |
| Caducidad genérica en lugar de `Issued.expiresAt` | Corregido | Campo y aviso usan `myCredential.expiresAt`, líneas 19/32; recibo real hasta 26/10 16:28. Código renovado, sin nuevo cobro |
| Giro anterior de 5 pasos/s | Corregido en configuración; fluidez física no verificable | Estilo real en espera: `24s steps(360)` y reversa: 15 pasos/s; movimiento reducido dejó cero animaciones; recarga ACTIVE sin celebración |
| Listeners 187→211 frente a seis adicionales | Aclarado, no es una medición nueva | +24 corresponde al contador global; seis al documento. Los JSON con/sin BFCache se refieren a ámbitos distintos; no sumarlos como fuga. No repetí la medición |
| Optimización y las 267 pruebas | Aclarado | Registro final DEC-057 de 19:23–19:25 UTC: 267 y salida 0 después del cambio a 15 pasos/s. Son evidencia histórica separada de las 309 finales |

**Recorrido propio:** registro y correo de verificación en SMTP local de descarte → código de evento → identidad/avatar → interés compartido → disponibilidad → propuesta → una aceptación (sin palabra ni indicaciones en texto o HTML) → segunda aceptación → ACTIVE en ambas sesiones → ambas llegadas → código inválido recuperable → verificación cruzada → primera confirmación (saldo 0) → segunda (saldo 10) → repetición del mismo POST (saldo 10) → historial → beneficio creado desde administración → solicitud y repetición del mismo requestKey (un canje, saldo 5) → renovación (saldo 5) → acceso al recibo desde la otra cuenta rechazado → consulta del encargado correcta → entrega 503. [Resumen sin credenciales](auditoria-implementacion-d01-d14/recorrido-resumen.json).

No se alteró la demo habitual. No se forzaron estados mediante SQL ni fixtures. Las dos cuentas entraron mediante formularios y verificación reales; la tercera utilizó el bootstrap existente de administración de una base vacía. La persistencia comprobada es tras recarga, no tras reinicio de la base en memoria.

## Capturas revisadas

Móvil primero. Los QR/códigos del recibo se ocultaron temporalmente **solo para capturar**, conservando su espacio; los huecos de esas imágenes no son defectos de la aplicación.

| 390 px | 360 px / tema alternativo |
|---|---|
| [Inicio editorial oscuro](auditoria-implementacion-d01-d14/01-inicio-390-oscuro.png) | [Avatar oscuro](auditoria-implementacion-d01-d14/16-avatar-360-oscuro.png) |
| [Identidad](auditoria-implementacion-d01-d14/04-identidad-borrador-390.png) | [Avatar claro](auditoria-implementacion-d01-d14/19-avatar-360-claro.png) |
| [Personalizador](auditoria-implementacion-d01-d14/07-avatar-cabeza-390.png) | [Final de scroll alcanzable](auditoria-implementacion-d01-d14/17-avatar-360-final-scroll.png) |
| [Propuesta con alias largo](auditoria-implementacion-d01-d14/09-propuesta-390.png) | [Familias de intereses](auditoria-implementacion-d01-d14/23-intereses-familias-360-claro.png) |
| [Mi Ping claro](auditoria-implementacion-d01-d14/20-mi-ping-390-claro.png) | [Recibo pendiente tras el fallo](auditoria-implementacion-d01-d14/18-recibo-pendiente-tras-503-360.png) |
| [Historial](auditoria-implementacion-d01-d14/13-historial-390.png), [beneficios](auditoria-implementacion-d01-d14/14-beneficios-390.png), [recibo](auditoria-implementacion-d01-d14/15-recibo-pendiente-390.png) | Cuatro animales: [gato](auditoria-implementacion-d01-d14/avatar-gato-390-claro.png), [conejo](auditoria-implementacion-d01-d14/avatar-conejo-390-claro.png), [nutria](auditoria-implementacion-d01-d14/avatar-nutria-390-claro.png), [panda rojo](auditoria-implementacion-d01-d14/avatar-panda-rojo-390-claro.png) |

Escritorio 1366×900: [administración oscura](auditoria-implementacion-d01-d14/30-admin-escritorio-oscuro.png), [clara](auditoria-implementacion-d01-d14/31-admin-escritorio-claro.png), [formulario de configuración](auditoria-implementacion-d01-d14/33-admin-formulario-escritorio-claro.png). `32-mi-ping-escritorio-claro.png` no se usa como prueba de Mi Ping: la sesión había caducado y muestra acceso; se conserva identificada como captura no válida para esa afirmación.

En las muestras móviles se entiende la acción principal; las opciones inferiores se alcanzan por scroll sin quedar permanentemente tapadas por Guardar o la navegación. Los animales mantienen orejas y accesorios reconocibles, sin deformación. La identidad Festival se conserva en llegada y cupones. La discrepancia principal de composición es A02. No exigí que todos los contenidos cupieran sin scroll.

## Comprobaciones y límites

- Entorno: JAR final existente, Java 21, Chrome con contextos separados, puerto local 18193, H2 2.4.240 en memoria y SMTP local 18194; demo desactivada. No había aplicación Java escuchando al iniciar. MySQL existente no se tocó. Perfil `integration-test` sin `LegacySecurityFixture`: `AuthSupport.legacyFixture()` exige perfil `test` y una clase de pruebas ausente del JAR. No se utilizó ese bypass.
- Táctil sintetizado por CDP: la tira de pestañas pasó de scrollLeft 0 a 154 y permitió seleccionar Audio, sin ensanchar la página. Teclado: flecha derecha seleccionó/focalizó Cara, con foco visible. [Touch](auditoria-implementacion-d01-d14/movil-interaccion.json), [teclado](auditoria-implementacion-d01-d14/teclado-pestanas.json). No son pruebas en teléfono físico.
- No observé recursos fallidos en la sesión de red registrada ni excepciones JS de aplicación. Una carga del editor legacy/V3 solicitó 32 recursos de avatar, 36.766 bytes decodificados, sin rutas duplicadas tras normalizar hashes. Es una muestra con caché, no un presupuesto universal de transferencia. [Recursos](auditoria-implementacion-d01-d14/avatares-recursos.json).
- SVG se renderiza mediante imágenes y rutas del catálogo cerrado; no encontré inserción de SVG aportado por usuario. Las capas conservan el 84 % una sola vez; no se redujeron más los animales.
- Se solicitó escala de texto de sistema 1,3 mediante CDP, pero no quedó demostrado un aumento efectivo: las medidas CSS de pestañas siguieron en 14 px/46 px. La captura `22-avatar-360-texto130.png` es un **intento de emulación**, no prueba de texto ampliado validado. Pendientes texto ampliado efectivo, lector de pantalla, Safari/iOS y teléfono físico. No se certifica accesibilidad ni contraste de toda la aplicación.
- Recibo vencido natural e incidencia no ejecutados; no se cambió el reloj ni se forzó la base. No hay prueba propia de entrega satisfactoria, recuperación tras caída real de red, reinicio/persistencia MySQL o plano con imagen. El estado histórico sin verificación tiene evidencia de código y pruebas existentes, no recorrido propio.
- Administración: lista y formulario revisados en escritorio, creación y edición usadas desde móvil. No se auditó toda administración ni se propuso rediseñarla.
- Skills: criterios de Impeccable y su referencia de auditoría, mobile-native, systematic-debugging y verification-before-completion; trabajo acotado con ping-efficient-work. Sin motor de Impeccable, hooks, instalaciones ni colección de skills. No fue necesaria una consulta especializada a ui-ux-pro-max ni rediseñar identidad con logo-design.

## Antes de cerrar la implementación

1. Resolver/reproducir A01 en MySQL aislada con excepción concreta; confirmar entrega y saldo sin duplicación.
2. Resolver explícitamente la discrepancia de D08 con la selección, conservando contratos e IDs actuales.
3. Corregir el texto `DELIVERED` de A03 y revisar un recibo entregado real.
4. Completar las verificaciones pendientes de texto ampliado efectivo y movimiento en dispositivo real antes de afirmar esas coberturas.

Esta auditoría no ejecuta correcciones, no modifica Figma y no autoriza integración. Solo añadió este informe y su carpeta de evidencia; fuentes y cambios previos conservados por hashes. Los procesos aislados de auditoría se cierran al terminar; no se dejan cuentas ni base de ensayo en MySQL.

---

## Resolución de hallazgos (Claude, 25 octubre 2026)

Añadido posterior. El texto anterior de Codex se conserva sin cambios como referencia. Los cambios siguen sin commit en `feature/seleccion-implementacion`; no hubo push, merge ni despliegue, y no se tocó la base MySQL habitual (3306). Las pruebas se hicieron en una instancia MySQL 8.4 privada (puerto 3399) con bases de ensayo creadas para la ocasión.

| Hallazgo | Estado | Resumen |
|---|---|---|
| A01 · Entrega 503 | **Observado originalmente por Codex; no reproducido después; causa desconocida; diagnóstico mejorado. No corregido** | La entrega funciona en H2 y en MySQL con el mismo recorrido; `WebAdvice.storageUnavailable` ahora registra la excepción con una referencia |
| A02 · D08 composición | **Confirmado y corregido** | Pestañas de mundo, grupos por familia con «k de n» y pie fijo «N elegidos en M familias · Revisar», según `133:1703` |
| A03 · Recibo `DELIVERED` | **Confirmado y corregido** | Reproducido por prueba y en navegador; etiqueta, seguimiento, instrucciones y pie responden al estado real |

### A01 · Entrega de beneficios

**Reproducción en H2 (modo MySQL, perfil de pruebas).** Prueba HTTP nueva con el recorrido del auditor: canje → generar código → renovar (`renew=true`) → consulta del encargado → dos POST de entrega. Resultado: 302 y 302, recibo `DELIVERED`, saldo sin segundo débito y una sola fila `DELIVER_CLAIM`. También se probó la forma de datos del auditor: avatar V3 guardado por formulario, beneficio de 5 puntos creado desde el formulario de administración, misma `requestKey` repetida y acceso ajeno al recibo (409). Resultado igual: 302/302, `DELIVERED`, saldo 37→32 y una auditoría.

**Mismo flujo en MySQL aislado.** Se repitió la misma clase de pruebas contra una base MySQL vacía, migrada por Flyway hasta V23. Las tres entregas quedaron `DELIVERED`, con una sola `DELIVER_CLAIM` por recibo, el código renovado consumido y el anterior revocado. La base se eliminó después.

**App real en el navegador (MySQL aislado).** Canje con doble envío (un canje, 10→5) → código → consulta y «Confirmar entrega» por la cuenta general, que no es dueña del participante → «Resultado registrado» → reintento del mismo POST (redirección) → recibo `DELIVERED`. Comprobado en la base: un canje, una `DELIVER_CLAIM`, credencial consumida. Sin errores JS. [Datos](../design-audit/evidencia/DEC-058/correcciones-auditoria/a01-a03-entrega-recibo.json).

**Descartado como causa.** H2 sin `MODE=MySQL` no llega a arrancar (V13 usa `MODIFY COLUMN`). H2 tiene alcance `test` y no está en el JAR; Codex no conserva la URL JDBC, el comando de arranque ni el log de su proceso (consultado en esta sesión). No hay evidencia para atribuir el 503 a H2, a la concurrencia ni a una regresión de DEC-058.

**Corrección aplicada.** `WebAdvice.storageUnavailable` descartaba la `DataAccessException` sin registrarla, y por eso el 503 quedó sin diagnóstico. Ahora la registra con método, ruta y una referencia de 8 caracteres, que también se muestra al usuario. El mensaje y el código 503 no cambian. Si el fallo reaparece, la causa SQL quedará en el log.

**Pendiente real.** Si vuelve a verse un 503 en la entrega, buscar en el log la línea «Error de datos de Ping … referencia X» y corregir esa causa concreta.

### A03 · Recibo entregado

**Reproducido.** La prueba nueva falló antes de la corrección: el recibo `DELIVERED` contenía «Por entregar» y «Acércate al punto de entrega».

**Corrección** (`rewards/receipt.html`):
- Paso 2 «Código comprobado · La persona encargada comprobó tu código.» cuando está `DELIVERED`.
- Paso 3 «Registrado por la persona encargada del evento.»
- Las indicaciones de recogida y el pie sobre «mostrar o renovar» solo aparecen mientras está `REQUESTED`.

Se conservan coste, saldo actual, ubicación, fecha de solicitud y número de recibo. La caducidad solo se mostraba con un código vigente y sigue igual. No se inventa una fecha de entrega. Capturas reales a [390 oscuro](../design-audit/evidencia/DEC-058/correcciones-auditoria/recibo-entregado-390-oscuro.png), [390 claro](../design-audit/evidencia/DEC-058/correcciones-auditoria/recibo-entregado-390-claro.png), [360 oscuro](../design-audit/evidencia/DEC-058/correcciones-auditoria/recibo-entregado-360-oscuro.png) y [360 claro](../design-audit/evidencia/DEC-058/correcciones-auditoria/recibo-entregado-360-claro.png). En las capturas de página completa, la barra inferior fija aparece a media altura; es un efecto de la captura.

### A02 · D08 Intereses

Diferencias concretas con `133:1703`, corregidas sin diseñar otra alternativa:
1. Los mundos eran una cuadrícula de dos columnas que había que abrir. Ahora son una tira horizontal de pestañas subrayadas, con icono y contador; el contador es amarillo en el mundo activo.
2. Dentro de un mundo había filtros «Todo/familia» sobre una lista alfabética. Ahora hay grupos con el título de la familia y «k de n» cuando hay elecciones.
3. El resumen superior y la bandeja fija se sustituyen por el pie fijo «N elegidos en M familias · Revisar» con «Entrar a {evento}». «Revisar» abre la bandeja de elegidos, donde se pueden quitar.
4. La etiqueta visible «Buscar un interés» pasa a estar solo disponible para lectores de pantalla.

**Se conserva:**
- Los checkboxes `interestIds` con su ID real como único estado: el script mueve las filas reales y no las copia.
- La taxonomía y las familias de DEC-045/056, y la búsqueda (devuelve la lista por categorías e indica «Tus elecciones se conservan»).
- El grupo de intereses retenidos y la lista completa sin JavaScript.
- El envío del formulario, el borrador y la vuelta a la identidad.

Se abre el primer mundo con elecciones o, si no hay ninguna, el primero del catálogo; el orden no está fijado a los ejemplos de Figma.

**Defecto encontrado al verificar.** `ui.js` sobrescribía el resumen del pie con su contador genérico en cada cambio. Ahora se aparta cuando el selector gestiona el formulario.

**Comprobado en el alta real** (`/join/interests`, base aislada) y en la edición:
- Los datos enviados coinciden con lo marcado (FormData = marcados) en cada paso: elegir, cambiar de mundo, volver, revisar, quitar desde la bandeja y buscar.
- Enter cambia de mundo, con contorno de foco visible.
- El pie es fijo en el alta, con la acción principal visible a 390.
- El envío lleva a `/me`; al reabrir la edición siguen las 2 elecciones guardadas.
- Sin desbordamiento horizontal a 390 ni a 360 y sin errores JS.

[Datos](../design-audit/evidencia/DEC-058/correcciones-auditoria/d08.json). Capturas: [inicial 390 oscuro](../design-audit/evidencia/DEC-058/correcciones-auditoria/d08-inicial-390-oscuro.png), [inicial 360 claro](../design-audit/evidencia/DEC-058/correcciones-auditoria/d08-inicial-360-claro.png), [eligiendo 390 oscuro](../design-audit/evidencia/DEC-058/correcciones-auditoria/d08-eligiendo-390-oscuro.png), [390 claro](../design-audit/evidencia/DEC-058/correcciones-auditoria/d08-eligiendo-390-claro.png), [360 oscuro](../design-audit/evidencia/DEC-058/correcciones-auditoria/d08-eligiendo-360-oscuro.png), [360 claro](../design-audit/evidencia/DEC-058/correcciones-auditoria/d08-eligiendo-360-claro.png), [Revisar](../design-audit/evidencia/DEC-058/correcciones-auditoria/d08-revisar-390-oscuro.png), [búsqueda](../design-audit/evidencia/DEC-058/correcciones-auditoria/d08-busqueda-390-oscuro.png), [edición 390](../design-audit/evidencia/DEC-058/correcciones-auditoria/d08-edicion-390-oscuro.png) y [edición 360 claro](../design-audit/evidencia/DEC-058/correcciones-auditoria/d08-edicion-360-claro.png).

**Diferencias que se mantienen.** En el alta, «← Tu identidad o evento» sigue al pie del formulario y no en la cabecera, porque es un envío que conserva el borrador. En la edición, el pie no es fijo para no chocar con la barra inferior de navegación.

### Pruebas y verificación final

- Prueba dirigida nueva: `VerificationTests.renewedReceiptCodeIsDeliveredOverHttpAndReceiptShowsClosedTracking` (A01 por HTTP con renovación, entrega idempotente y auditoría única; A03 recibo cerrado).
- La clase de sonda MySQL y sus bases se usaron solo para reproducir el fallo y se eliminaron. D08 no tiene prueba nueva, porque es composición; el estado se comprobó en el navegador.
- `./mvnw -Dspring.profiles.default=test clean verify` sobre la versión final: **310 pruebas, 0 fallos, 0 errores, BUILD SUCCESS**.

### Estado D01–D14 tras las correcciones

- **A02 (D08) y A03 (D13): corregidos**, con las evidencias enlazadas arriba.
- **A01: observado originalmente, no reproducido después, causa desconocida y diagnóstico mejorado.** No está corregido.
- **310 pruebas, 0 fallos, BUILD SUCCESS**: resultado de la versión final comprobada, después de todas las correcciones de esta sección. Las 309 y 267 anteriores corresponden a versiones previas.

No se declara el cierre completo de D01–D14. Pendientes reales: revisión humana de Samuel, texto ampliado efectivo, lector de pantalla y teléfono físico.

### Incidente: eliminación de `C:\c` durante las comprobaciones

**Qué ocurrió.** El 25 de octubre a las 23:29:22 UTC (18:29 en Bogotá), Claude ejecutó en Git Bash este comando:

`mkdir -p $S/dec058/auditoria && cp -r /c/c/Users/river/AppData/Local/Temp/claude/C--Users-river-PING-movil/<sesión>/scratchpad/dec058/auditoria/* $S/dec058/auditoria/ && rm -rf /c/c && ls …`

Antes de borrar, Claude solo listó la ruta profunda `…/dec058/auditoria/`, no el contenido de `C:\c`. El borrado eliminó la carpeta `C:\c` completa.

**Origen de la carpeta, comprobado en el registro de la sesión.** Dos ejecuciones de Node recibieron rutas `/c/Users/…` con `MSYS_NO_PATHCONV=1`; en ese caso Windows interpreta la ruta como `C:\c\Users\…`:
- `node flujo.mjs "$S/dec058/auditoria/flujo"` a las 23:27:11 UTC;
- `node auditoria.mjs "$S/dec058/auditoria/recibo"` a las 23:28 UTC (dos intentos; el segundo, a las 23:28:54).

Las ejecuciones anteriores con `MSYS_NO_PATHCONV` (20:19–20:38 UTC) cargaban `env.sh`, donde `S="C:/Users/…"`, y escribían en la ruta correcta. En ninguna otra transcripción de Claude (`~/.claude/projects`) ni sesión de Codex (`~/.codex/sessions`) aparece `C:\c`.

**Lo que se sabe que contenía**, y que no es un inventario completo:
- `…\scratchpad\dec058\auditoria\flujo\`: 8 capturas y `flujo.json`. Hay copia íntegra en el scratchpad de la sesión, hecha justo antes del borrado; coincide con lo que genera el script en esa ejecución.
- `…\scratchpad\dec058\auditoria\recibo\`: 5 capturas y `auditoria.json`. Hay copia íntegra; las capturas y los datos usados en este informe salen de ella.
- Perfiles temporales de Chrome de esas ejecuciones (`chrome-profile-flujo` de las 18:27 y `chrome-profile-auditoria`), deducidos porque en la ubicación correcta `chrome-profile-auditoria` no existe y `chrome-profile-flujo` no cambia desde las 15:26. No se copiaron. Eran desechables y solo tenían sesiones de la app local de ensayo.

**Mecanismo.** `rm -rf` de Git Bash borra directamente, sin pasar por la Papelera. Comprobado: de los 3.616 elementos de la Papelera, ninguno viene de `C:\c`; los tres borrados más recientes son de `Downloads` (24/10) y son ajenos a esta sesión.

**Lo que se desconoce.**
- Si `C:\c` existía antes de las 23:27 UTC o tenía otros archivos. No hay un listado previo de `C:\` en la sesión ni registros de otros programas que lo confirmen o lo descarten.
- Si hay instantáneas del volumen, porque la sesión no tiene privilegios de administrador para consultarlas.

**No se puede afirmar** que no se perdieron archivos ajenos. Solo consta que nada en los registros de Claude ni de Codex apunta a contenido previo.

**Opciones, sin aplicar** (no se instaló nada ni se restauró):
1. Aceptar el estado actual si nadie usaba `C:\c`.
2. Desde una consola de administrador, consultar en solo lectura el diario NTFS (`fsutil usn readjournal C: csv`, herramienta incluida en Windows) para ver qué nombres se borraron bajo `C:\c` en torno a las 18:29 de Bogotá.
3. Consultar las instantáneas o «Versiones anteriores» de `C:\`, también como administrador.

Desde este incidente no se ha hecho ninguna limpieza ni eliminación adicional.
