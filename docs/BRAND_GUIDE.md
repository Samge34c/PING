# Identidad visual oficial de Ping

Fuente: instrucciones e imagen oficial aportadas por el usuario el 13 de octubre de 2026. No se ha creado ni sustituido el logo.

## Logo oficial

Archivo original: `Gemini_Generated_Image_tun5w3tun5w3tun5.jpg`. Ubicación prevista de la copia sin modificar: `src/main/resources/static/img/ping-logo.jpg`.

**PENDIENTE HUMANO — archivo:** la imagen es visible en el chat, pero no hay un JPG accesible en los adjuntos del entorno. La ruta Windows del usuario no está disponible aquí. Hace falta adjuntar el JPG como archivo; copiarlo sin redibujar, recolorear ni sustituir el isotipo. Las vistas ya detectan su presencia y lo muestran en navbar, inicio y selección de participante demo. Hasta recibirlo muestran únicamente el nombre textual PING, sin logo alternativo ni enlace de imagen roto.

Conservar proporción, fondo y colores originales. No recortar ni estirar; usar `object-fit: contain`. Texto alternativo: «Ping — logo oficial».

## Colores y aplicación

Todos los colores se centralizan en [brand.css](../src/main/resources/static/css/brand.css), mediante variables CSS:

| Variable | Valor | Uso |
| --- | --- | --- |
| `--ping-black` | `#000000` | Fondo principal |
| `--ping-purple` | `#6A298F` | Base de marca; principal en claro |
| `--ping-purple-2` | `#872EA3` | Hover y bordes |
| `--ping-purple-dark` | `#512067` | Superficies y estados |
| `--ping-yellow` | `#F8E836` | Enlaces, foco y acentos |
| `--ping-yellow-light` | `#F8EE4F` | Resaltados y errores visibles |
| `--ping-text-light` | `#F5F5F5` | Texto sobre fondos oscuros |

Bootstrap 5.3.8 se conserva como CSS oficial local en `static/vendor/bootstrap/`, con su licencia MIT, para no depender de Internet durante la demo. Se comparó con la distribución oficial de `twbs/bootstrap` v5.3.8. Está personalizado desde este único CSS. Superficies y texto secundario se derivan de la paleta con `color-mix`; no dispersar valores de color por templates. Mantener blanco suave sobre morado para legibilidad. El amarillo es acento, no fondo dominante.

Usar labels visibles, foco de teclado amarillo, tablas con desplazamiento en móvil y mensajes comprensibles. No cambiar la paleta principal ni sustituir el logo sin autorización.

## Temas oficiales

B es **oscuro oficial y predeterminado**: grafito, violeta y dorado. D es **claro oficial**: fondos neutros, morado Ping y acento amarillo. La navbar ofrece únicamente Oscuro/Claro; `localStorage['ping-theme']` conserva `dark` o `light`. Sin preferencia válida se usa oscuro. `/demo/themes` muestra ambos oficiales; A/C se retiraron.

Variables de fondo, superficies, navbar, inputs, botones, texto, bordes y foco se centralizan en brand.css. En claro, palabras destacadas y enlaces usan morado para evitar amarillo ilegible. El logo original mantiene una superficie negra discreta en ambos temas, sin filtros ni recoloración. Se respeta movimiento reducido.

## Dirección App Social Dark

La dirección «Cartel en vivo» usa B oscuro `#101015` / superficies `#1B1822` y `#211D29`, morado `#BB9CFF` y amarillo `#F5DF65`; D claro `#F6F3EE`, superficies blancas y morado `#7444BA`. Cards de 10 px solo para objetos independientes; controles de 6 px; misión, pasos, saldo e historial usan tipografía y divisores. La paleta original del JPG se conserva. Sidebar de 224 px, navegación inferior móvil, SVG propios y Manrope variable local (licencia SIL OFL incluida en `static/fonts/OFL.txt`). Sin CDN. Las ondas son ilustraciones de conexión; no sustituyen el logo oficial.
