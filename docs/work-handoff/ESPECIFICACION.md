# Segunda evolución funcional de PING

## Estado y autorización

Petición explícita de Samuel, 19 de octubre de 2026. Esta entrega prepara el trabajo para Codex; no declara funciones implementadas. La solicitud actual autoriza cuentas, roles y verificación obligatoria de llegada, y sustituye las exclusiones temporales anteriores. No atribuir aprobación a Julián. Codex debe registrar el cambio en DECISION_LOG y actualizar PRODUCT/AI_CONTEXT donde corresponda, sin mantener reglas contradictorias activas.

Diseñar primero a 390 px, comprobar 360 px; escritorio debe permitir operar administración con claridad. Mantener Festival Social, negro/morado/amarillo, claro/oscuro, stack, demo, SOFA, planos versionados, matching, Saga, puntos, canjes, privacidad, bloqueos y reportes. Los cambios funcionales solicitados son excepciones expresas a las restricciones anteriores; no son autorización para reescribir pantallas terminadas.

## Cobertura y aceptación por requisito

| ID | Solicitud | Resultado verificable |
|---|---|---|
| R01 | Editor con botones + Añadir | Cada botón abre un formulario inline de su tipo; editar/cancelar no pierde otros cambios; no hay tres textareas gigantes |
| R02 | Máximo 3 puntos, 10 actividades, 100 palabras | Contadores y límites por evento, en interfaz, servicios e importación; solicitudes simultáneas no exceden el límite |
| R03 | Categorías con desglose | Música → Pop → Lady Gaga/Michael Jackson y Juegos → familias → títulos; búsqueda, volver y selección persistente al filtrar |
| R04 | Verificar encuentro con palabra o QR | La otra persona presenta su desafío personal; validación servidor ligada al encuentro/participantes; palabra compartida original no basta |
| R05 | Escanear para entrar al evento | Un mismo componente de escaneo, con contexto de entrada, QR válido del evento y alternativa de código |
| R06 | Correo, Google y Facebook | Correo funcional y recuperable; proveedores reales cuando configurados; no presentar simulación como OAuth validado |
| R07 | Cuentas y roles separados | Participante, administrador de evento y general; accesos diferenciados y permisos en servidor limitados al evento autorizado |
| R08 | No repetir pareja exitosa en el evento | A/B o B/A ya completados quedan fuera de nuevas propuestas en ese evento, incluso con disponibilidad y sin bloqueo |
| R09 | Ambas llegadas antes de confirmar | Antes de ambas llegadas no aparece la confirmación final; POST directo también la rechaza; informar qué falta |
| R10 | Código/QR de canje y entrega | Solicitar crea recibo individual persistente; encargado autorizado valida y entrega una sola vez en el punto indicado |
| R11 | Punto de entrega de beneficios | Configuración del evento con nombre e indicaciones; el recibo conserva snapshot del lugar de entrega |
| R12 | Avatar preset personalizable | Alias con animal, sombrero y accesorio de catálogo; sin subida de imágenes ni URL arbitraria; persiste al volver a entrar |

## 1. Editor de evento

Orden móvil: título/estado → datos esenciales → puntos de encuentro → palabras → actividades → entrega de beneficios → recompensas → plano. No añadir portada decorativa. Un botón principal de guardar por formulario; no anidar formularios. Si se guardan secciones por separado, indicar claramente qué sección se guardó y qué sigue pendiente.

En escritorio: navegación de secciones a la izquierda, editor al centro y resumen compacto de cantidades/errores a la derecha. En móvil: secciones en flujo natural; sin panel lateral ni tabla ancha. El prototipo muestra una opción de formulario agregado; Codex puede conservar envíos por sección si evita pérdida de datos y comunica su alcance.

- **Puntos, 0/3:** filas con nombre, indicación breve, estado y Editar. `+ Añadir punto de encuentro` despliega nombre (máximo actual 160), indicaciones y enlace operativo al plano. Guardar/Cancelar locales. No exigir arrastrar: coordenadas numéricas/accesibles siguen disponibles.
- **Palabras, 0/100:** chips removibles; `+ Añadir palabras clave` abre input breve con Añadir. Opcional pegar varias separadas por coma o salto de línea, previsualizando válidas/duplicadas antes de confirmar. Máximo 40 caracteres por palabra. Normalizar espacios y comparar duplicados sin distinguir mayúsculas/acentos; preservar la forma visible elegida. Contar palabras únicas, no líneas vacías.
- **Actividades, 0/10:** título (máximo actual 120), instrucciones (máximo actual 500), puntos de recompensa actuales 0–100, puntos de encuentro compatibles e intereses opcionales. Una sola actividad activa debe tener al menos un punto activo compatible. No inventar compatibilidad ni desactivar una combinación sin avisar.
- **Entrega:** nombre, indicaciones, referencia al plano si existe. Propuesta mínima: un punto predeterminado por evento, sin imponer un máximo nuevo de puntos de entrega; cada recompensa puede conservar una ubicación específica existente. Lugar de entrega y punto de encuentro son conceptos distintos: la entrega no consume los tres cupos de encuentro.
- **Recompensas:** reutilizar administración existente: título, descripción, coste, ubicación de entrega efectiva. Al elegir predeterminado, mostrar de dónde viene. No sobrescribir snapshots de canjes antiguos.
- **Plano:** reutilizar carga/versión/posicionamiento; no almacenar archivos del plano en Git. Las imágenes y coordenadas antiguas siguen disponibles para encuentros activos.

**Interpretación propuesta de límites:** hasta 3 puntos y 10 actividades vigentes/configurables; filas archivadas conservadas por historial no consumen cupo ni se asignan. Si el producto usa inactivo en vez de archivado, distinguir desactivar temporalmente de retirar del catálogo. No permitir crear cien filas inactivas como atajo al límite. Hasta 100 palabras vigentes. Esta elección no puede destruir datos anteriores.

**Compatibilidad:** hoy coexisten listas V14 y catálogos estructurados V15. Cambiar solo la plantilla del formulario de evento dejaría los servicios sin límite: integrar EventForm/EncounterOptions, EncounterController/EncounterService y todos los caminos de importación. Importar una sola vez de forma idempotente, conservando IDs, configuraciones y snapshots; nuevos textos no duplican filas existentes. Si un evento antiguo supera el límite: conservarlo, señalar exceso y permitir reducción/edición sin aumentar cantidades; no truncar ni borrar automáticamente. Bloquear nuevas activaciones que no tengan configuración válida con mensaje recuperable, no un error 500.

Usar versión/CSRF y el bloqueo transaccional del evento ya existente al contar/crear. Error de validación conserva datos y foco en resumen. Sin JavaScript, un camino de formulario nativo sigue funcionando. No guardar un formulario incompleto por pulsar un botón de añadir.

## 2. Gustos con profundidad

Catálogo adjunto: nombres editoriales, no ranking de popularidad. Las asociaciones musicales son orientativas y admiten varios grupos; no son una clasificación exclusiva. Títulos de juegos son gustos, plataformas son intereses separados. No descargar logos/fotos por cada opción.

Móvil: buscador → selección resumida → familias → subgrupos → opciones. Por ejemplo, Música / Pop muestra el gusto Pop y los artistas relacionados. Seleccionar Lady Gaga **no selecciona automáticamente** Pop, Música ni otros artistas. Los ocho gustos históricos siguen elegibles con sus mismos IDs. Etiquetas de navegación no cuentan como selección.

`catalogo-intereses.json` separa nodos de presentación e intereses seleccionables mediante claves estables. Un interés puede verse en varios nodos, sin duplicar su ID ni su contador. Al importar, reutilizar por clave/alias histórico; no por posición de fila. Revisar conflictos de nombres antes de asignar IDs. El máximo de intereses del participante sigue siendo el actual del proyecto: mostrarlo y conservarlo, no sustituirlo por el tamaño del catálogo.

Mantener matching por intereses seleccionados explícitamente, no por coincidencia automática de ancestros. Género/grupo no convierte un artista en afinidad con todos los demás. Mantener selección al buscar/expandir/retroceder; explicar cuando se alcanza el máximo. Catálogo inactivo no admite nuevas elecciones; elecciones anteriores se conservan. No enviar todos los avatares ni cientos de botones ocultos en cada polling.

Administración: el general mantiene taxonomía global; administrador de evento configura actividades usando intereses existentes, sin editar taxonomía global. Una jerarquía de presentación puede mapear múltiples grupos a un interés; usar migración aditiva y relaciones explícitas, sin romper category_id histórico ni inventar coincidencias.

## 3. Cuentas, acceso y permisos

Entrada de participante: contexto del evento (si vino por QR/código) → Entrar o Crear cuenta → alias/avatar → intereses → Mi Ping/Conectar. Conservar evento y destino al regresar de OAuth; solo destinos internos permitidos. No pedir otra vez alias/intereses a quien ya participó en ese evento. Perfil de cuenta privado, alias público por evento.

Registro por correo: correo, contraseña, repetir contraseña y enlace a entrar. Acceso por correo: correo, contraseña, mostrar/ocultar y recuperar acceso. Usar autocompletado adecuado, permitir pegar/gestores, feedback explícito y no almacenar contraseñas en logs/borradores del navegador. No recoger nombre legal, fecha de nacimiento o teléfono sin necesidad actual.

Google y Facebook: usar Spring Security y flujos de proveedor reales; Google se presenta como “Continuar con Google”, no “Gmail”. Solicitar permisos mínimos; ausencia de correo válido no crea enlace inseguro. Identidad externa única por proveedor+subject. No fusionar cuentas automáticamente por coincidencia de correo no verificado. Vinculación de proveedores desde sesión autenticada con prueba de control. No aceptar fotos externas como avatar: sigue el catálogo preset.

Credenciales de proveedores, correo y URL HTTPS se configuran fuera del repositorio. Integración ausente: no dejar botones activos que fallen ni afirmar login social terminado. Documentar variables/redirect URI exactas sin secretos; probar rechazo/cancelación además del caso feliz. La configuración de aplicación, consentimiento o revisión del proveedor depende del propietario: no inventar aprobaciones. Correo de verificación/recuperación debe probarse con transporte controlado; recuperación de un solo uso y caducidad, respuestas que no revelen si la cuenta existe. Para desarrollo se permite un buzón local controlado, no imprimir enlaces privados en logs públicos. No abrir un servicio externo de pago por defecto.

| Operación | Participante | Admin de evento | Admin general |
|---|---|---|---|
| Perfil, avatar y participación propios | Sí | Como participante si participa | Como participante si participa |
| Buscar/aceptar/llegar/verificar/confirmar | Solo identidad propia | No puede hacerlo por otra persona | No puede hacerlo por otra persona |
| Editar evento, puntos, actividades, plano, recompensas | No | Solo eventos asignados | Sí |
| Validar y entregar canje | No | Solo evento asignado | Sí |
| Mantener catálogo global | No | No | Sí |
| Asignar/revocar admins de evento | No | No | Sí |
| Crear o promover admin general | No | No | Proceso restringido y auditado |

Accesos visuales separados: “Entrar a PING” para participantes y enlace discreto “Organizo un evento”. Este segundo acceso **no concede un rol**. El servidor determina roles y eventos autorizados. No permitir escoger ADMIN en registro ni confiar en eventId/participantId de formularios. Cada pantalla/acción y lectura privada debe autorizarse en servidor. Cerrar rutas administrativas públicas existentes, incluyendo edición de participantes, datos, métricas, recompensas y entrega; revisar las rutas de demo que permiten adoptar identidad y restringirlas al modo demo autorizado, nunca a cuentas reales por URL.

Arranque inicial del general: mecanismo de administración de una sola vez mediante configuración privada externa y deshabilitado después, sin contraseña fija ni endpoint público de promoción. Registrar acciones administrativas sin tokens/códigos/contraseñas. Logout con POST/CSRF, renovación de sesión al autenticar y expiración recuperable; conservar defensas existentes al integrar Spring Security.

Migración de sesiones antiguas: no asignar cuenta por alias ni por ID enviado por el navegador. Permitir vincular el participante realmente poseído por la sesión válida después de autenticación y confirmación explícita, de forma atómica. Un participante no se vincula a dos cuentas. Una cuenta puede participar en distintos eventos; mantener una identidad por cuenta/evento. No fusionar puntos ni borrar duplicados históricos automáticamente. Conservar pruebas/demo en perfil o aislamiento apropiado, sin bypass administrativo en modo normal.

## 4. Llegada, palabra/QR y confirmación

### Secuencia propuesta

Aceptación bilateral → ACTIVE con misión y punto → cada persona pulsa “Ya llegué” → cuando ambos llegaron se habilita verificar a la otra persona → ambas verificaciones válidas → confirmar actividad realizada → dos confirmaciones finales → Saga y recompensa.

Antes de ambas llegadas, ocultar la acción final y mostrar “Falta que [alias] marque su llegada” o “Marca tu llegada al punto”. Nunca fabricar la llegada ajena. La regla debe aplicarse también a POST directo; esconder botones no basta. Llegar o escanear no entrega puntos ni confirma automáticamente actividad. En la pantalla se puede leer la actividad antes de verificar; solo su confirmación queda bloqueada.

### Qué palabra se introduce

La palabra actual es compartida y se conoce antes del encuentro; no sirve como secreto de prueba. Conservarla para reconocerse. Propuesta: cada persona recibe **su propio código personal**, compuesto por palabra y sufijo aleatorio legible, y un QR equivalente. A muestra su código personal a B; B lo introduce/escanea y el servidor registra que B verificó a A. También a la inversa. A no puede leer el desafío privado de B por polling, HTML oculto o API.

Token QR opaco aleatorio, ligado al propósito `ENCOUNTER`, match, emisor y destinatario; almacenar representación segura, caducidad y consumo. Propuesta inicial revisable: validez 10 minutos, renovar invalida solo el desafío pendiente anterior; un recibo de verificación ya validado no caduca durante el ACTIVE. Código manual con entropía suficiente y límites de intentos por cuenta/encuentro además de IP, espera recuperable y mensaje claro; no escoger una palabra de las 100 sin sufijo como credencial. Cifras exactas de intentos/entropía deben fijarse y probarse al implementar, sin bloqueo permanente como castigo por errores.

Consumir únicamente mediante POST autenticado y CSRF. Reintentar la misma verificación por el mismo destinatario devuelve el mismo resultado; no crea comprobaciones extra. Código propio, otro match, persona ajena, expirado o propósito distinto se rechaza. Cancelar/completar invalida desafíos pendientes; snapshots de palabra/misión/recompensa permanecen. Los códigos no se ponen en URLs de recursos de terceros, logs, métricas ni títulos.

Esto dificulta el fraude de un clic. **No demuestra presencia física ni evita acuerdos entre dos cuentas que se envían códigos a distancia.** No añadir GPS, biometría ni selfies sin una solicitud nueva.

Compatibilidad: versionar política al activar el encuentro. ACTIVE antiguos ya iniciados no se dejan imposibles de finalizar ni se les cambian importes; informar de la política heredada. Los nuevos ACTIVE adoptan la nueva regla. Los COMPLETED y Saga iniciadas continúan idempotentes, sin reabrirse ni pagar otra vez. Si se exige aplicar la regla a un ACTIVE antiguo, debe existir migración explícita recuperable; nunca inventar llegadas/verificaciones pasadas.

## 5. Un escáner, tres propósitos

Contextos separados: unirse a evento (`EVENT_JOIN`), verificar encuentro (`ENCOUNTER`) y entregar beneficio (`REDEMPTION`). QR público del evento puede contener enlace conocido con código estable. Los otros contienen credenciales opacas sin información privada. Un escaneo no debe abrir cualquier URL: validar estructura, origen permitido y propósito antes de mostrar destino/confirmar.

Interfaz: “Escanear QR” y “Introducir código”. Pedir cámara solo tras toque, preferir trasera, permitir cancelar/cambiar cuando proceda; detener tracks al salir u ocultar pantalla. Estados: permiso denegado, cámara ocupada, no compatible, QR ilegible/incorrecto/expirado, sin red. No subir fotogramas a terceros. Si se permite elegir una foto para decodificar QR, procesarla localmente: esa función no habilita subir avatares.

La cámara web requiere contexto seguro: **HTTPS en el teléfono**. El HTTP de la IP LAN de Windows que hoy funciona para navegar no debe presentarse como válido para cámara. Mantener código manual operativo sin cámara. Si la API nativa de lectura de QR no existe, elegir y fijar un decoder local mantenido, revisar licencia/tamaño y cargarlo solo al abrir escáner; no introducir CDN, React ni SDK de seguimiento. ZXing Core del servidor ya genera QR, pero eso no demuestra que el navegador pueda leerlos.

## 6. Evitar pareja repetida

Excluir del matching todas las parejas que ya tengan una conexión COMPLETED en el mismo evento, sin importar el orden A/B, los bloqueos o disponibilidad. No excluir por esta nueva regla otros eventos ni encuentros fallidos; mantener las restricciones existentes de cancelación/bloqueo sin cambiarlas silenciosamente. No cambiar la regla actual de afinidad/alternativa.

Usar consulta persistente y coordinación transaccional al generar propuesta. El bloqueo de evento existente puede evitar carreras; si se añade clave canónica o registro de pareja exitosa, debe admitir historia previa y no bloquear por error una propuesta cancelada. Si ya hay duplicados históricos completados, no borrarlos ni retirar recompensas. Evitar volver a proponerlos desde ahora y mostrar un vacío útil: “Por ahora no hay nuevas personas disponibles”. Reabrir sesiones/cambiar de dispositivo no evade la exclusión de identidad persistente; cuentas nuevas y colusión siguen siendo limitaciones.

## 7. Canje, recibo y entrega

Conservar débito/idempotencia/Saga actuales. Solicitar canje genera un recibo persistente con estado, beneficio, coste, ubicación/indicaciones congeladas, código legible y QR. El QR autoriza únicamente la entrega de ese canje. El participante no puede entregárselo desde su cuenta.

Participante: saldo/coste/faltante juntos → solicitar → recibo “Pendiente de entrega” → “Ve a [punto]” → código/QR y acceso en Mis canjes. El coste se descuenta una sola vez según flujo actual; mostrar saldo real, no estimaciones. El canje pagado debe poder recuperarse tras caducidad del acceso; vincular identidad antes de crearlo en flujo normal. No vender recuperación de la sesión invitada antigua como cuenta recuperable.

Administrador autorizado: escanea/introduce código → ve evento, beneficio, cantidad y estado, sin correo ni datos innecesarios → confirma entrega en POST → transición atómica a Entregado. Dos dispositivos/POST repetidos no entregan ni cobran dos veces. Si un código de entrega expira, renovar sin nuevo débito, invalidando el anterior. Un usuario de otro evento no consulta ni entrega el recibo. Código desconocido, propósito incorrecto, cancelado o ya entregado muestra respuesta útil y no filtra detalles a no autorizados. No cambiar el flujo de devolución/cancelación que exista sin registrar la decisión.

## 8. Avatares

Ocho animales SVG originales, cuatro sombreros opcionales y tres accesorios opcionales, con paleta acotada. Claves, coordenadas y combinaciones en `avatares/manifest.json`. “Sin sombrero/accesorio” son opciones reales. Sin URL, archivo, cámara, foto del proveedor ni SVG introducido por el usuario. Validar enum/allowlist en servidor; no concatenar rutas recibidas.

Móvil: alias → avatar grande de vista previa → animales en cuadrícula → pestañas Animal/Sombrero/Accesorio/Color → continuar. Tamaño táctil mínimo cómodo de 44 px, estado seleccionado además del color y nombres accesibles. Cambiar avatar no cambia identidad ni saldo. Guardar avatar predeterminado compatible para participantes existentes. No asignar rostro humano ni rasgos sensibles.

## 9. Trabajo de Codex, puntos de control y pruebas

1. **Editor y recursos:** nuevo editor progresivo sobre servicios existentes, límites globales, entrega predeterminada, catálogo e integración de alias/avatar. No activar permisos nuevos de forma parcial.
2. **Cuentas y permisos:** autenticación, migración de sesión, autorización de todas las rutas, correo y adaptadores Google/Facebook. Sin secretos, documentar pendiente de configuración real. No publicar administración abierta como producto final.
3. **Conexión y canjes:** exclusión de pareja completada, llegada bilateral, desafío personal, componente scanner compartido y entrega autorizada. Persistir versión de política y conservar Saga/snapshots.
4. **Integración:** dos altas normales con evento propio → intereses/avatar → propuesta/aceptación → dos llegadas → verificación cruzada → dos confirmaciones → puntos exactos de la actividad → canje → entrega por admin del mismo evento → nueva búsqueda sin repetir pareja.

No ejecutar matrices combinatorias para ganar conteos. Pruebas dirigidas a límites/concurrencia/roles/CSRF/privacidad/no repetición/códigos/reintentos/Saga; después suite requerida una vez al cierre del bloque coherente, repitiendo solo lo afectado por cambios posteriores. Para diseño: 390 oscuro principal, 360 estrechez, claro y un escritorio; revisar capturas reales y estados afectados. Un mockup, HTTP 200 o emulación no valida OAuth, cámara física ni teléfono real.

Cada bloque funcional terminado: commit pequeño, push a rama propia, comprobar SHA remoto, PR en borrador hacia base correcta. No merge, no force push ni main. No subir credenciales, base MySQL, planos privados, códigos reales, cookies o logs. Nota breve de continuidad con HEAD, qué funciona, qué no y próximo comando; actualizarla al cerrar cada bloque. Si GitHub falla, conservar commit local y declarar que no hay respaldo remoto confirmado. Los datos MySQL necesitan respaldo aparte; Git no los recupera.

## Referencias técnicas verificadas el 19/10/2026

- MDN, cámara y contextos seguros: https://developer.mozilla.org/en-US/docs/Web/API/MediaDevices/getUserMedia
- Spring Security, OAuth Login y configuración de clientes: https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html
- Google, credenciales de OAuth: https://developers.google.com/identity/protocols/oauth2
- OWASP, autorización de transacciones (credenciales únicas, validación servidor, anti-replay e intentos): https://cheatsheetseries.owasp.org/cheatsheets/Transaction_Authorization_Cheat_Sheet.html

Las reglas de negocio y los valores propuestos de este documento son diseño de PING; esas fuentes no los certifican ni acreditan presencia física.
