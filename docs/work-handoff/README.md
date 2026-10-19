# PING · entrega Work → Codex

19 de octubre de 2026. Preparación de producto y recursos, **no implementación en PING**.

## Abrir y continuar

1. Descomprime este paquete y abre `prototipo.html`: funciona sin instalación ni internet. Es una propuesta interactiva, no una captura de PING ni una integración funcional.
2. En Codex, adjunta el ZIP completo y pega `PROMPT_CODEX.md`. Si no puedes adjuntarlo, usa el prompt y la especificación; informa de que los recursos todavía faltan. No afirmar que Codex recibió archivos que no puede leer.
3. Codex debe recuperar el último código de GitHub, comprobar la base indicada abajo y trabajar en ramas nuevas. No aplicar el prototipo como sustituto de los controladores existentes.

## Base comprobada

Repositorio `Samge34c/PING`. Rama `feature/mobile-interaction-polish`, PR #42. HEAD leído en la copia: `8c94cd7df452c7ababfa73687664712434c3f425`. Si aparece un avance posterior compatible, revisarlo antes de elegir la base; nunca retroceder automáticamente a este SHA.

## Qué se hace aquí y qué se hace en Codex

| Work: entregado en este paquete | Codex: implementación pendiente |
|---|---|
| Especificación de todas las solicitudes y decisiones propuestas | Servicios, controladores, persistencia y migraciones |
| Prototipo del editor progresivo, gustos, alias y accesos | Integración con Thymeleaf/CSS/JS existentes y datos reales |
| Catálogo jerárquico JSON con claves estables y ocho gustos históricos | Importación idempotente, administración y conservación de IDs |
| Animales y accesorios SVG originales, manifiesto de opciones | Persistencia de elecciones y validación de listas permitidas |
| Reglas de cuentas, permisos, llegada, verificación y canje | Spring Security, correo, OAuth y validación de códigos en servidor |
| Casos de aceptación y orden de trabajo | Pruebas pertinentes, migración MySQL y recorrido de dos sesiones |

El reparto es por capacidad, no un 50 % artificial de líneas de código. Aquí no se cambió la aplicación, la base de datos ni GitHub.

## Archivos

- `ESPECIFICACION.md`: alcance completo, decisiones propuestas, permisos y aceptación.
- `PROMPT_CODEX.md`: instrucción ejecutable para continuar sin rehacer esta preparación.
- `catalogo-intereses.json`: propuesta editorial; no es un ranking ni catálogo oficial de marcas.
- `avatares/manifest.json`, `avatares/animales/`, `avatares/sombreros/`, `avatares/accesorios/`: recursos originales locales por capas.
- `prototipo.html`: propuesta navegable sin backend; no genera QRs ni autentica usuarios.
- `build_assets.py`: fuente reproducible de datos y SVG; usa únicamente la biblioteca estándar de Python.
- `VALIDACION.json` y `SHA256SUMS.txt`: resultado de comprobación del paquete. No certifican implementación en PING.

## Revisión de esta entrega

El prototipo tiene lógica local para añadir/editar puntos y actividades, limitar cantidades, quitar palabras, buscar/seleccionar gustos, componer avatar y exportar un borrador. Solo esos recursos están escritos: no hay autenticación, cámara, QR real, permisos ni persistencia de PING implementados aquí.

Se comprobaron datos, referencias, SVG, sintaxis JavaScript y cobertura documental. **No se pudo hacer revisión visual ni interacción real en navegador en este entorno**: faltaba el ejecutable local y el navegador remoto no pudo acceder al archivo. No se declara validación a 390/360 ni ausencia de overflow observada. El diseño sí incluye esas adaptaciones CSS; Codex debe renderizarlo y comprobarlas antes de integrarlo.

Para revisar la propuesta a mano: añade dos puntos hasta ver 3/3; edita uno al llegar al máximo; añade palabras repetidas con mayúsculas y comprueba que no se dupliquen; entra en Gustos → Música → Pop, elige un artista y busca otro sin perderlo; cambia animal/sombrero; exporta el borrador. Estos son pasos propuestos, no una ejecución certificada.

Los nombres de juegos, plataformas y artistas identifican gustos de usuarios. No se incluyen logos, música, fotos ni ilustraciones de esas marcas o artistas. Recursos SVG originales de esta entrega: autorización para incorporarlos y modificarlos dentro de PING; sin material gráfico de Kahoot.
