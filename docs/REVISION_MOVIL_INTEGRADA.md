# Revisión integrada móvil — DEC-036

Instrucción de Samuel, 2026-10-16; no se atribuye aprobación a Julián. Issue #14 permanece abierto. Base limpia `feature/home-mobile`, HEAD `4d434912583cda5c729a2d695199aa1acf121dfc`, en `/workspace/work/ping-verificacion-final-mvp`. Rama de correcciones `fix/mobile-journey-review`, dirigida a Home; sin merge ni cambios en main.

## Recorrido real y aislamiento

Aplicación Spring/Thymeleaf real, perfil `test`, H2 en memoria separado de MySQL, puerto loopback8082. Se reutilizaron el launcher y las herramientas existentes, con plantillas/estáticos del checkout, sin reiniciar los servicios MySQL/8080 ni instalar nada. Todas las preparaciones, disponibilidades y confirmaciones de esta revisión ocurrieron exclusivamente en esa base efímera; nunca en encuentros ajenos ni en la demo persistente.

A390×844, dos contextos Chromium independientes:

1. Home → formulario de preparación → POST explícito con CSRF → entrada demo.
2. Selección real de Samuel y Julián, Mi Ping y disponibilidad de ambos.
3. Búsqueda → OFFERED; primera aceptación mantiene espera; segunda activa el encuentro.
4. ACTIVE → mapa interactivo, zoom y retorno al mismo encuentro/punto/palabra.
5. Primera confirmación: ACTIVE, espera de la otra persona, saldo0.
6. Segunda confirmación: COMPLETED, saldo10 de cada persona. Beneficios muestra saldo10/coste20/faltan10 y canje deshabilitado; no se solicitó canje.
7. Desvíos de lectura: Hub del participante y editor de intereses propio. No se modificaron intereses ni pistas.

El recorrido completo se ejecutó sobre la base antes de las dos correcciones. Después se comprobaron de nuevo las superficies modificadas, POST/teclado de entrada, ambos destinos de continuación y saldo, sin repetir todo el ciclo ni otorgar otros puntos. Las protecciones de reintentos, feedback sin puntos y confirmación bilateral se ejecutaron además mediante MatchTests/FinalMvpTests.

## Problemas observados y cambios pequeños

- **Entrada:** botones de cuatro participantes repetían el selector nativo; icono y encabezados duplicados retrasaban el formulario. Se mantiene un único selector etiquetado, selección previa, CSRF, estado vacío y preparación/registro existentes. El botón quedó alrededor de y290 en390 px (antes y634), sin reducir controles. La ilustración antigua se conserva solo en escritorio; se separaron sus etiquetas que aparecían pegadas.
- **COMPLETED:** Mi Ping/Beneficios aparecían después del feedback opcional, badges y promoción. Se adelantaron los mismos enlaces, antes del feedback, y se agruparon +10 y saldo real. A390 ambos enlaces están alrededor de y398–446; a360 se apilan y siguen antes del feedback. No se condiciona la continuación a responder ni se cambian sus formularios.
- CSS exclusivo de entrada/COMPLETED, misma Manrope y variables Festival Social; sin arte, dependencias ni backend nuevos. OFFERED, ACTIVE y CANCELLED conservan lógica y navegación. Home, Hub, Mi Ping y Beneficios no se modificaron.

## Capturas reales

Todas son vistas de PING funcionando, no prototipos. Capturas de las pantallas sin cambios tomadas durante el recorrido inicial; entrada/COMPLETED después de las correcciones. El mapa se volvió a capturar en lectura con la transición terminada (el encuentro ya completado), sin fabricar ACTIVE ni repetir confirmaciones.

- [Home390](screenshots/integrated-mobile/01-home.png)
- [Entrada390 corregida](screenshots/integrated-mobile/02-entry.png)
- [Mi Ping390](screenshots/integrated-mobile/03-mi-ping.png)
- [Propuesta390](screenshots/integrated-mobile/04-offered.png)
- [ACTIVE390, aceptación real de ambos](screenshots/integrated-mobile/05-active.png)
- [Mapa390](screenshots/integrated-mobile/06-map.png)
- [COMPLETED390 corregido](screenshots/integrated-mobile/07-completed.png)
- [Beneficios390, saldo real10](screenshots/integrated-mobile/08-benefits.png)
- [COMPLETED1440](screenshots/integrated-mobile/09-completed-desktop.png)

## Verificación proporcional actual

- **390 principal:** recorrido completo descrito, sin overflow horizontal ni errores JS. Lectura visual: estado y siguiente acción claros; palabra/confirmación localizables; sin decoración añadida que desplace acciones. En las correcciones se conserva la identidad negro/morado/amarillo y controles legibles.
- **Solo las dos superficies modificadas:**360 oscuro para estrechez,390 claro para contraste/superficies/controles y1440 oscuro para adaptación. Entrada por teclado del selector al botón; enlaces de continuación reales; controles finales alcanzables por encima de la barra inferior al desplazar. Ninguna acción fija nueva. No se montó una matriz de toda la aplicación.
- **Maven/Java21:** `./mvnw -B -o -s /workspace/.ping-tools/settings.xml -Dmaven.repo.local=/workspace/.ping-tools/m2 -Dtest=DemoImprovementTests,FinalMvpTests,MatchTests verify`, usando JAVA_HOME y MAVEN_USER_HOME existentes. **47 pruebas, 0 fallos/errores/omitidos; BUILD SUCCESS**. Son13+14+20 actuales, no las suites históricas. El último ajuste fue separar dos líneas de texto en CSS de escritorio, verificado en navegador; no requirió repetir las pruebas Java sin cambios funcionales.
- **Herramienta de revisión:** dos errores del script auxiliar, no de PING: barra móvil oculta en escritorio devuelve bounding box nulo; selector de seguridad elegía un summary anidado dentro de un details cerrado. Se acotaron al elemento visible correcto y se completaron las comprobaciones restantes, sin convertir esas falsas alarmas en cambios de aplicación.
- **Impeccable:** lectura manual y revisión operativa de capturas, sin flujo formal de critique/subagentes. Context una vez, detector una vez, hooks desactivados. Cuatro avisos estáticos: `th:src` interpretado como imagen sin src, jerarquía plana por no resolver CSS externo, copy previo de OFFERED y borde lateral previo de aviso global. La jerarquía renderizada sí es28/16 px; no se alteraron otras pantallas para silenciar avisos ni se declara un detector limpio.
- **Skills:** ping-efficient-work para reutilización y alcance; Impeccable para jerarquía/capturas; Verification para evidencia; Debugging solo ante los errores concretos del script. No se necesitaron consulta UX ni rediseño estructural general. Lectura manual no equivale a descubrimiento automático.

Logs, scripts, capturas adicionales y JSON de verificación permanecen fuera de Git en `/workspace/work/ping-integrated-20261016/` (0700). No se versionan sesiones, claves, CSRF, datos MySQL ni logs privados. Las capturas versionadas contienen únicamente alias de fixtures de la demo aislada.

## Respaldo y cadena conservada

Al consultar el conector, PR24–28 abiertas, en borrador y sin merge; sus commits son ancestros de Home:

| PR | Pantalla / rama | Base |
|---|---|---|
| [24](https://github.com/Samge34c/PING/pull/24) | Hub / `reconstruction/festival-social` | `feature/final-mvp` |
| [25](https://github.com/Samge34c/PING/pull/25) | ACTIVE / `feature/active-festival-social` | `reconstruction/festival-social` |
| [26](https://github.com/Samge34c/PING/pull/26) | Mi Ping / `feature/mi-ping-mobile` | `feature/active-festival-social` |
| [27](https://github.com/Samge34c/PING/pull/27) | Beneficios / `feature/beneficios-mobile` | `feature/mi-ping-mobile` |
| [28](https://github.com/Samge34c/PING/pull/28) | Home / `feature/home-mobile` | `feature/beneficios-mobile` |

La nueva PR de correcciones apunta a `feature/home-mobile`, sin retargetear esas cinco PR. SHA del nuevo commit y verificación remota se registran en su PR/entrega después de publicar. No hay autorización de merge, main o force-push.

## Pendientes y continuidad

**Relevantes:** revisión humana de las PR; alta manual sin demo, alternativa voluntaria, errores/reintentos Saga y gestión de entrega de canjes no recibieron una revisión visual completa en esta pasada. No son fallos confirmados. Tampoco se verificaron Safari/dispositivos físicos ni zoom nativo. No se declara toda la app terminada ni accesibilidad certificada.

**Opcional:** simplificar en otra decisión la navegación compartida donde Mi Ping y Conectar llegan al mismo `/me` con sesión; no se altera ahora una navegación transversal aprobada por preferencia visual.

**Única siguiente prioridad propuesta:** revisar el alta manual → selección de intereses → llegada a Mi Ping desde celular, para cubrir la entrada sin depender de la demo. El editor se observó en lectura y su guardado sigue alcanzable, pero ese alta no se recorrió.

MySQL/8080 conserva datos y servicios anteriores. La vista efímera8082 contiene el encuentro aislado completado y puede cerrarse sin afectar MySQL. El respaldo de la tarea antigua no fue recuperado. No hay nuevos recursos artísticos ni procedencias externas que registrar.
