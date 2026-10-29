# Capturas reales del incremento

Datos ficticios en H2 local y Chrome; no son mockups ni capturas de producción. Las imágenes de Claude/Figma son referencias separadas. Código final y hashes en [evidencia](../../verification/event-increment/evidence.json).

## Móvil 390 px primero

- Pista [vacía con formulario](pista-vacia-formulario-390.png), [guardada](pista-final-guardada-390.png), [edición](pista-edicion-390.png), [error recuperable 400](pista-final-error-390.png), [versión obsoleta 409](pista-obsoleta-final-390.png), [prioridad de propuesta](pista-propuesta-390.png). Cancelar y borrado comprobados en los recorridos y las pruebas.
- [Preparación](preparacion-final-390.png), [ayuda con título y foco](ayuda-hoja-390.png), [tarjeta](tarjeta-final-390.png), [agenda Ahora/Terminada](agenda-estados-390.png), [reto alcanzado con dos conexiones reales](reto-final-390.png).
- [PNG descargado realmente](tarjeta-descargada.png), 1080×1350: cero conexiones reales de esa cuenta, sin logros inventados. El caso con dos conexiones/saldo15 se comprobó en el primer ensayo; no mezclar personas/fixtures.
- [Entrada editorial integrada](entrada-editorial-final-390.png).

## 360 px y tema alternativo

[Pista guardada](pista-final-guardada-360.png), [ACTIVE dominante](pista-active-360.png), [ayuda](ayuda-hoja-360.png), [ayuda del código](ayuda-codigo-360.png), [agenda con texto largo](agenda-estados-360.png). Texto ampliado al130% sin desbordamiento comprobado en el recorrido.

## Escritorio 1280 px

[Mi Ping](miping-final-1280.png), [preparación](preparacion-final-1280.png), [entrada editorial](entrada-editorial-final-1280.png).

Se conservan también capturas intermedias para trazabilidad. `pista-vacia-390`, `pista-guardada-360`, `preparacion-390`, `agenda-390` y `ayuda-390` preceden a las correcciones finales; no usarlas como evidencia de cierre visual. `pista-obsoleta-390.png` es intermedia y su viewport real era360px; queda sustituida por `pista-obsoleta-final-390.png`. `tarjeta-error-390.png` demuestra recuperación del error, antes del ajuste de separación de palabras. En una captura de página completa la barra fija aparece en la posición del viewport inicial; la alcanzabilidad se comprobó por desplazamiento e hit-test real, no por esa composición.
