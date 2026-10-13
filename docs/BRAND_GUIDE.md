# Identidad visual provisional de Ping

Fuente: instrucciones e imagen oficial aportadas por el usuario el 13 de octubre de 2026. No se ha creado ni sustituido el logo.

## Logo oficial

Archivo original: `Gemini_Generated_Image_tun5w3tun5w3tun5.jpg`. Ubicación prevista de la copia sin modificar: `src/main/resources/static/images/ping-logo-official.jpg`.

**PENDIENTE HUMANO — archivo:** la imagen es visible en el chat, pero no hay un JPG accesible en los adjuntos del entorno. La ruta Windows del usuario no está disponible aquí. Hace falta adjuntar el JPG como archivo; copiarlo sin redibujar, recolorear ni sustituir el isotipo. Las vistas ya detectan su presencia y lo muestran en navbar e inicio. Hasta recibirlo muestran únicamente el nombre textual PING, sin logo alternativo ni enlace de imagen roto.

Conservar proporción, fondo y colores originales. No recortar ni estirar; usar `object-fit: contain`. Texto alternativo: «Ping — logo oficial».

## Colores y aplicación

Todos los colores se centralizan en [brand.css](../src/main/resources/static/css/brand.css), mediante variables CSS:

| Variable | Valor | Uso |
| --- | --- | --- |
| `--ping-black` | `#000000` | Fondo principal |
| `--ping-purple` | `#6A298F` | Botones principales |
| `--ping-purple-2` | `#872EA3` | Hover y bordes |
| `--ping-purple-dark` | `#512067` | Superficies y estados |
| `--ping-yellow` | `#F8E836` | Enlaces, foco y acentos |
| `--ping-yellow-light` | `#F8EE4F` | Resaltados y errores visibles |
| `--ping-text-light` | `#F5F5F5` | Texto sobre fondos oscuros |

Bootstrap 5.3.8 se conserva como CSS oficial local en `static/vendor/bootstrap/`, con su licencia MIT, para no depender de Internet durante la demo. Se comparó con la distribución oficial de `twbs/bootstrap` v5.3.8. Está personalizado desde este único CSS. Superficies y texto secundario se derivan de la paleta con `color-mix`; no dispersar valores de color por templates. Mantener blanco suave sobre morado para legibilidad. El amarillo es acento, no fondo dominante.

Usar labels visibles, foco de teclado amarillo, tablas con desplazamiento en móvil y mensajes comprensibles. No cambiar la paleta principal ni sustituir el logo sin autorización.
