# Rendimiento del frontend móvil

Base: `perf/render-response@2346585d0c047ed113686aa7fb15060ffe52f198` (PR #53). Rama: `perf/frontend-mobile-load`. Worktree: `C:\Users\river\PING-perf-frontend`. Sin merge ni despliegue.

## Alcance autorizado

Solicitud de Samuel del 25 de octubre de 2026: que PING cargue y responda con menos espera en celulares, conservando exactamente los diseños seleccionados (D01–D14, ambos temas, marca, tipografía), los avatares V3 y las funciones existentes. No es un rediseño. Archivos de este trabajo: templates, CSS, JavaScript, fuentes, imágenes y recursos públicos, `PublicResourceTests.java`, pruebas nuevas de recursos frontend y esta carpeta. Java, POM, Dockerfile, perfiles, workflows, migraciones y documentación central quedan fuera. No se atribuye aprobación a Julián.

En paralelo, Codex trabajó el backend (PR #55, `perf/backend-roundtrips`). Los dos conjuntos de cambios no tocan los mismos archivos.

## Método

- **Instancias:** dos instancias locales aisladas, una por versión, con H2 en memoria. Ninguna toca MySQL habitual, Aiven ni Render.
  - «Antes»: clases de la base copiadas antes de cualquier cambio.
  - «Después»: esta rama.
- **Datos:** en ambas, una cuenta general de arranque, el escenario SOFA de demostración y un encuentro ACTIVE entre los participantes ficticios 1 y 2. Se prepararon con los formularios reales y CSRF.
- **Medición:** Chrome 156 headless por CDP, con emulación móvil 390×844 a escala 3 y táctil. **Es una emulación, no Safari ni un iPhone físico.**
  - Perfil fijo de red: RTT 100 ms, 9 Mbit/s de bajada y 1,5 Mbit/s de subida. CPU ralentizada ×4.
  - Cada repetición abre un contexto nuevo, con la caché vacía, y carga dos veces: **fría** y **repetida**.
  - Mediana de 3 repeticiones.
  - Bytes transferidos = bytes de red tal como llegaron (CSS, JS, SVG y TTF van con gzip; HTML, JSON e imágenes WebP sin comprimir).
- **Tiempos:** son del navegador, con el servidor local. No incluyen DNS/TLS ni la latencia Render↔Aiven. El tiempo del servidor está en el primer byte (TTFB), de 6 a 20 ms en local.
- **Herramientas:** quedan fuera del repositorio (`C:\Users\river\ping-perf-frontend-evidence\tools`), junto con logs, capturas PNG originales y sesiones de la instancia local. No contienen credenciales de servicios reales.

## Problemas observados y por qué importaban

1. **Bootstrap completo en cada pantalla:** 232 KB (31 KB con gzip) que bloquean el renderizado de todas las páginas.
   - La cobertura de reglas de Chrome en 10 pantallas del participante usó el **5,5 %**. Brand usa el 35 % y v2 el 54 %.
   - El resto se descargaba, se analizaba y participaba en el cálculo de estilos con la CPU limitada de un teléfono.
2. **Fuente variable completa precargada:** 165 KB (68 KB con gzip) con griego, cirílico y vietnamita.
   - La interfaz en español solo necesita latín y algunos signos.
   - La precarga compite con el CSS crítico en la primera visita.
3. **Ilustración de Inicio a 1536 px para cualquier pantalla:** 312 KB, y es el elemento LCP.
   - A 390 px de ancho y escala 3 se muestran 1074 px de ancho.
   - A escala 2 bastan 716.
4. **Selector de intereses:** 226,5 KB de HTML **sin comprimir** (264 filas, ~860 B por fila).
   - Se sirve en `/participants/{id}/interests` y `/join/interests`, y también lo descarga entero el editor en línea de `/interests`.
   - El 14 % era espacio en blanco del template repetido en cada fila.

No se encontraron listeners duplicados, polling más frecuente, recursos de pestañas cargados antes de tiempo ni falta de caché inmutable en recursos con huella:
- PR #53 ya sirve `/css`, `/js`, `/img`, `/fonts` y `/vendor` con huella y `immutable`.
- Los envíos ya muestran estado y bloquean el doble envío (`ui.js`).
- El escáner y ZXing solo se cargan en su pantalla.

## Cambios

1. **`bootstrap.ping.min.css`:** subconjunto generado del Bootstrap 5.3.8 oficial local (`docs/performance/frontend/tools/bootstrap_subset.py`). El original se conserva para regenerarlo.
   - Conserva toda regla cuyo selector pueda coincidir con el marcado de PING: sus clases aparecen en templates, JS o Java, y las de `:not/:is/:where/:has` no se exigen.
   - También conserva las reglas sin clases (variables `:root`, `data-bs-theme`, reboot), las `@font-face`/`@keyframes` y la licencia MIT.
   - Tamaño: 232 KB → 33 KB, 7 KB con gzip.
   - `FrontendBundleTests` falla si algún template, JS o Java usa una clase de Bootstrap ausente del subconjunto. Se comprobó quitando `.form-label`: falla y nombra la clase. Si falla, se regenera; `--check` comprueba que el archivo esté al día.
2. **Manrope dividida por `unicode-range`** (`tools/manrope_split.py`, fontTools):
   - `Manrope-Variable-core.ttf` (89 KB, 37 KB con gzip): latín, latín extendido A, puntuación, flechas y signos que usa la interfaz. Es la que se precarga.
   - `Manrope-Variable-extra.ttf` contiene el resto de glifos. El navegador solo lo descarga cuando aparece un carácter fuera del núcleo. Comprobado: con «Жệ» se cargan ambas.
   - Mismo eje `wght`, mismas funciones OpenType y ningún glifo perdido. El original se conserva.
3. **Inicio con `srcset`:**
   - Variantes de 768 y 1152 px (WebP de calidad 85, `tools/hero_variants.py`) más el original de 1536 px.
   - `sizes` se calculó con el ancho real medido: 328 px a 360 de ancho, 358 px a 390 y hasta 566 px en escritorio, donde `cover` necesita ~629 px.
   - Un iPhone de escala 3 recibe la de 1152 px (207 KB). Las pantallas de escala 2 reciben la de 768 px (115 KB). Escritorio con escala 2 sigue recibiendo 1536 px.
   - El `width`/`height` intrínseco se mantiene, así que no hay saltos.
4. **Fila del selector de intereses sin espacio en blanco entre etiquetas.** Mismos elementos, IDs, atributos, `name`, CSRF y orden. La fila es `block` y la etiqueta `grid`, por lo que el espacio no se pintaba.
   - Tamaño: 226,5 KB → 199,6 KB por carga.

No se tocaron avatares V3, sus capas, el encuadre del 84 %, planos, escáner, cámara, polling, borradores ni JavaScript de comportamiento. Tampoco se añadieron service workers, cachés de páginas privadas ni dependencias.

## Mediciones comparables (mediana de 3, carga fría)

| Pantalla | Solicitudes | KB transferidos | FCP ms | LCP ms | load ms | TBT ms | Hilo principal ms |
|---|---|---|---|---|---|---|---|
| Login | 11 → 11 | 154 → 100 | 564 → 496 | 564 → 496 | 557 → 508 | 66 → 16 | 325 → 245 |
| Inicio | 11 → 11 | 475 → 312 | 628 → 512 | 720 → 612 | 686 → 585 | 108 → 0 | 389 → 275 |
| Avatar V3 | 16 → 41* | 191 → 164 | 752 → 540 | 952 → 744 | 703 → 612 | 176 → 0 | 507 → 549 |
| Intereses (catálogo + editor en línea) | 12 → 12 | 387 → 306 | 552 → 440 | 552 → 440 | 544 → 459 | 134 → 94 | 567 → 515 |
| Editar intereses | 11 → 11 | 371 → 291 | 1028 → 640 | 1028 → 640 | 1030 → 852 | 333 → 70 | 763 → 626 |
| Mi Ping | 13 → 13 | 173 → 118 | 756 → 548 | 756 → 548 | 685 → 581 | 163 → 2 | 480 → 372 |
| ACTIVE | 15 → 15 | 181 → 127 | 884 → 580 | 884 → 580 | 836 → 607 | 302 → 110 | 590 → 438 |
| Beneficios | 10 → 10 | 164 → 110 | 704 → 556 | 704 → 556 | 684 → 561 | 217 → 92 | 470 → 361 |

\* En el personalizador, las miniaturas de las pestañas son `loading="lazy"`. Cuántas entran antes de cerrar la ventana de medición varía entre ejecuciones (entre 16 y 41 en ambas versiones), así que **no es un efecto de estos cambios**.

TBT y el trabajo del hilo principal son indicativos: varían entre repeticiones.

**Carga repetida** (caché caliente):
- El HTML sigue siendo la única descarga en casi todas las pantallas.
- Los recursos con huella ya venían de caché antes de este trabajo.
- Diferencias observadas: Intereses 238 → 211 KB y Editar intereses 222 → 196 KB, por el marcado sin comprimir. Los tiempos repetidos quedan dentro de la variación.

Datos completos: [evidence.json](evidence.json).

## Conservación del diseño

- **Comparación píxel a píxel** (umbral 24/255 por canal) de capturas de página completa, antes y después.
  - **Pantallas principales:** Login, Inicio, Avatar, Intereses, Editar intereses, Mi Ping, ACTIVE y Beneficios. Cada una a 390 oscuro, 360 oscuro y 390 claro.
  - **Pantallas adicionales:** `/join`, registro, recuperación, error de login, Mi cuenta, Evento, Conexiones, Organización, Administración, Participantes, Eventos, demo, preparación demo, categorías, accesos, plano y verificación. Cada una a 390/360 oscuro, 390 claro y escritorio 1280 oscuro/claro.
- **Resultado:** 0 píxeles distintos salvo dos casos.
  - La ilustración de Inicio, al reescalarse, cambia el 0,06–0,07 % de los píxeles de la página. Comparación lado a lado: [inicio-ilustracion-antes-despues.webp](capturas/inicio-ilustracion-antes-despues.webp).
  - La hora «Actualizado» de Administración, que depende del momento de la captura.
- Capturas de muestra en [capturas/](capturas/), reducidas a WebP para el repositorio. Los PNG originales de la comparación quedan fuera del código.
  - Inicio 390 oscuro antes/después.
  - Editar intereses 390 oscuro.
  - ACTIVE 390 claro.
  - Login 360.
  - Administración en escritorio.
- **Comprobación funcional en Chrome (390):** 0 errores JS en 9 pantallas, con la fuente cargada. En el editor de intereses se probó marcar y desmarcar con el resumen actualizado, y la búsqueda «cine» devuelve «Cine» y «Cine colombiano» con su estado anunciado.

Foco, teclado y movimiento reducido no cambian:
- Las reglas de foco y `prefers-reduced-motion` de Bootstrap se conservan porque usan las mismas clases que el marcado.
- `brand.css` y `v2.css` no cambian salvo `@font-face`.
- Ningún JavaScript cambia.

## Pruebas

`./mvnw -Dspring.profiles.default=test clean verify` (mismo comando que CI):
- **324 pruebas, 0 fallos, 0 errores, 0 omitidas.** Son las 323 de la base más `FrontendBundleTests`. JAR generado.
- Mockito con `-javaagent` local, como en PR #53.
- No se cambiaron los scripts de Windows, así que no se repitió la suite Python.
- `PublicResourceTests` comprueba que `brand.css` referencia la misma huella que la precarga de la fuente núcleo.

## Límites y pendientes

- No se ha probado en Safari ni en un iPhone físico. No se ha desplegado en Render.
  - La mejora real en el teléfono depende de la red, de TLS y del despertar de Render Free, que este trabajo no cambia.
  - Comparar en el iPhone tras desplegar la versión integrada, con el mismo teléfono, red, cuenta y datos, y con el servicio despierto.
- El subconjunto de Bootstrap depende de que las clases aparezcan literalmente en el código. Hoy no hay clases de Bootstrap construidas por concatenación (se revisaron las concatenaciones de templates, JS y Java), y `FrontendBundleTests` vigila las literales.
- `docs/BRAND_GUIDE.md` describe Bootstrap 5.3.8 «oficial local». Este trabajo no edita documentación central: el integrador debería añadir que se sirve un subconjunto generado del mismo archivo.

## Propuestas que dependen de backend o configuración (no implementadas)

1. **HTML sin compresión:** Intereses y Editar intereses mueven ~200 KB de HTML. `application.properties` excluye HTML y JSON de la compresión por los secretos de sesión (BREACH).
   - Spring Security 6+ enmascara el token CSRF en cada respuesta (`XorCsrfTokenRequestAttributeHandler`), pero las páginas pueden contener otros datos personales.
   - Decidir si se comprime solo el HTML de páginas sin códigos ni pistas, o mantener la política. Es una decisión de seguridad para el integrador; no se cambió.
2. **Editor en línea de `/interests`:** descarga la página completa `/participants/{id}/interests` para extraer solo el formulario.
   - Supone un segundo viaje en serie y otro documento entero.
   - Un fragmento del formulario o renderizarlo directamente en `/interests` requiere un controlador: corresponde a backend.
3. **Formato WOFF2:** la fuente núcleo podría pasar a WOFF2 (~25–30 % menos que TTF con gzip). Requiere el codificador Brotli, que no está instalado aquí. WOFF2 ya va comprimido y no depende de la compresión del servidor. Queda como mejora opcional.
