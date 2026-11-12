# Integración del rediseño sobre la versión publicada · DEC-069

12 de noviembre de 2026. Rama local `feature/redesign-integrated-render` en la copia de trabajo aparte `C:\Users\river\PING-integracion`, creada desde `00483fa` (commit servido por Render). Sin commit, push, merge ni despliegue. La rama original `feature/redesign-admin-mi-ping-verification` y sus cambios sin confirmar quedan intactos en `C:\Users\river\PING`.

## Punto de partida

- `00483fa` **no** es ancestro de la rama del rediseño. Ocurre al revés: la base del rediseño (`8c94cd7`, 19 de octubre) es ancestro de `00483fa`, que tiene 71 commits más. La rama del rediseño no tenía commits propios: todo eran cambios locales sin confirmar sobre `8c94cd7`.
- **Códigos personales:** llegaron con `bd92bf4` («verify encounters and deliver purpose-bound receipts», V21), después de esa base. Por eso la copia local no los tenía, y DEC-049/DEC-050 de esa rama afirmaban que «no existe código de verificación del encuentro». Eso es falso en la versión publicada. Ahí, todo encuentro nuevo usa la política 2 (`JourneyPolicy`):
  1. Llegada de ambos.
  2. «Generar mi código personal».
  3. Escanear o escribir el código de la otra persona y «Verificar a la otra persona».
  4. Confirmación bilateral, o el método de la actividad.
- **Números de decisión:** la versión publicada ya usa DEC-049 (Work R01–R12) y DEC-050 (claridad móvil). Esta integración se registra como DEC-069.

## Qué se integró y qué no

| Mejora de la rama local | En la integración | Motivo |
|---|---|---|
| «Puntos entregados» desde el registro de la Saga | **Integrada** (`AdminService`, `CompletionStepRepository`, `DeliveredPointsMetricTests`) | La versión publicada conservaba `completadas × 20` |
| Guía por tareas | **Integrada sobre la guía existente**: `/me/help` reorganizada y `/organizer/help` nueva, más `docs/GUIA_USUARIO_PING.md` | Usa los botones reales: códigos personales, revisión del personal, comprobación al entregar, recibos y retiro |
| Mi Ping compacto | **Integrada**: la siguiente acción va antes del avatar y de la pista | La pista expandida dejaba «Ir a Conectar» a ~980 px |
| Navegación y vistas de administración por evento | No integrada | El tablero de Organización (DEC-066/067) ya resuelve evento, pendientes y acción, con permisos por evento |
| Lista de canjes con «Marcar entregado» | No integrada | Saltaría el código de recibo y la evidencia de DEC-068 |
| Máximo cuatro categorías y «Ver todas» | No integrada | El selector publicado usa pestañas por familia (una a la vez), búsqueda y barra fija |
| Encuentro por pasos de la rama local | No integrado | El publicado ya tiene «Ahora · paso N de 4» con códigos personales |

## Cálculo de «Puntos entregados», antes y después

| Datos | Publicado (`completadas × 20`) | Integrado (registro) | Saldo |
|---|---|---|---|
| MySQL aislado, «Feria ficticia Sur»: 1 encuentro de +20 con códigos personales | 20 | **40** | 40 |
| `DeliveredPointsMetricTests`: +15 y +40; un intento fallido revertido; confirmaciones repetidas; un canje entregado de 25 | 40 | **110** antes y después del canje | 110 → 85 |

## Pruebas

- **Arranque:** el JAR integrado arrancó sobre una base MySQL 8.4 nueva y aislada (`ping_integrado`, instancia propia en `127.0.0.1:3399`, sin tocar `MySQL84`).
  - Flyway aplicó 26 migraciones hasta **V26**, ninguna fallida.
  - Hibernate validó el esquema (`ddl-auto=validate`).
  - El JAR de `00483fa` también arrancó en otra base aislada (`ping_render`) para comparar.
- **Dos sesiones en Chromium (Playwright MCP), con cuentas ficticias verificadas por correo local:**
  - **Código del evento incorrecto:** aparece «No encontramos ese código…», se conserva lo escrito y el reintento entra.
  - **Código personal incorrecto:** aparece «El código no es válido para esta acción, caducó o fue sustituido. Pide uno nuevo y reintenta.». Sigue en el paso «2 Verificar», con el campo disponible y sin botón de confirmar.
  - **Código correcto:** avanza a «3 Actividad». A confirma y ve «Esperando a Nico» sin puntos. B confirma y ve «Conexión completada 🎉», con +20 a cada uno.
  - **Abono único:** un POST repetido no cambia el saldo. El registro tiene un solo `GRANT_FIRST` y un solo `GRANT_SECOND`, y los 2 códigos personales quedaron consumidos.
- **Resultados:** muestra 40.
- **Pantallas revisadas a 390, 360 y 1280 px:**
  - Mi Ping, `/me/help` y `/organizer/help`, sin desplazamiento horizontal.
  - La hoja de ayuda de Mi Ping abre la guía, sus enlaces internos funcionan y «Entendido» devuelve el foco a «Ayuda».
- **Suite completa sobre el resultado:** `./mvnw -o -B test` → **393 pruebas, 0 fallos, 0 errores, BUILD SUCCESS**.

## Pendiente

- **Sin probar:**
  - iPhone/Safari real;
  - la cámara (escaneo de QR personal y de recibo);
  - un lector de pantalla real;
  - Render/Aiven: no se desplegó nada.
- **Recorridos no repetidos en el navegador, porque no se tocaron:**
  - revisión del personal;
  - comprobación al entregar;
  - entrega de recibo;
  - retiro de participantes.

  Las pruebas existentes de la suite los cubren.
- **Aceptación de Samuel y Julián** (DEC-069 es una propuesta). Se aparta del orden de Mi Ping del diseño aprobado de DEC-058.
